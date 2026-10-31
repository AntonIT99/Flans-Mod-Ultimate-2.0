#!/usr/bin/env python3
from __future__ import annotations

import argparse
import csv
import json
import re
import sys
import zipfile
from dataclasses import dataclass
from pathlib import Path
from typing import Dict, Iterable, List, Optional, Set, Tuple

PROJECT_ROOT = Path(__file__).resolve().parent.parent
ZIP_ROOT = PROJECT_ROOT / "run" / "flan"
SOURCE_ROOT = PROJECT_ROOT / "src"
CONFIG_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "config"
OUTPUT_CSV = PROJECT_ROOT / "missing_shortnames.csv"

FOLDER_TO_CATEGORY: Dict[str, str] = {
    "aaguns": "aagun",
    "armorFiles": "armor",
    "guns": "gun",
    "grenades": "grenade",
    "bullets": "bullet",
    "vehicles": "vehicle",
    "planes": "plane",
    "mechas": "mecha",
    "teams": "team",
}

CATEGORY_TO_JSON: Dict[str, str] = {
    "aagun": "aagun_categories.json",
    "armor": "armor_categories.json",
    "gun": "gun_categories.json",
    "grenade": "grenade_categories.json",
    "bullet": "bullet_categories.json",
    "vehicle": "vehicle_categories.json",
    "plane": "plane_categories.json",
    "mecha": "mecha_categories.json",
    "team": "team_categories.json",
}

SHORTNAME_RE = re.compile(r"^\s*Shortname\s+(.+?)\s*$", re.IGNORECASE)
NAME_RE = re.compile(r"^\s*Name\s+(.+?)\s*$", re.IGNORECASE)
LANG_FILE_SUFFIX = "assets/flansmod/lang/en_us.json"
LANG_KEY_PREFIX = "item.flansmod."


@dataclass(frozen=True)
class ShortnameOrigin:
    category: str
    shortname_lower: str
    full_name: str
    zip_path: str
    internal_txt_path: str


def iter_zip_files(root: Path) -> Iterable[Path]:
    if not root.exists():
        return []
    return sorted(p for p in root.rglob("*.zip") if p.is_file())


def iter_flans_content_dirs(source_root: Path) -> Iterable[Path]:
    if not source_root.exists():
        return []
    definition_dirs: List[Path] = []
    for source_folder in source_root.iterdir():
        if not source_folder.is_dir():
            continue
        flans_content_dir = source_folder / "resources" / "flans_content"
        if flans_content_dir.is_dir():
            definition_dirs.extend(
                candidate
                for candidate in flans_content_dir.rglob("definitions")
                if candidate.is_dir()
            )
    return sorted(definition_dirs)


def is_txt_in_category(internal_path: str) -> Optional[str]:
    normalized_path = internal_path.replace("\\", "/")
    if not normalized_path.lower().endswith(".txt"):
        return None
    parent_folders = {part.lower() for part in normalized_path.split("/")[:-1]}
    for folder, category in FOLDER_TO_CATEGORY.items():
        if folder.rstrip("/").lower() in parent_folders:
            return category
    return None


def extract_shortnames_from_text(text: str) -> List[str]:
    shortnames: List[str] = []
    for line in text.splitlines():
        match = SHORTNAME_RE.match(line)
        if match:
            shortnames.append(sanitize_shortname(match.group(1).strip()))
    return shortnames


def sanitize_shortname(value: str) -> str:
    return re.sub(r"[^a-z0-9._\-]", "_", value.lower())


def display_path(path: Path) -> str:
    try:
        return str(path.relative_to(PROJECT_ROOT))
    except ValueError:
        return str(path)


def extract_full_name_from_text(text: str) -> str:
    for line in text.splitlines():
        match = NAME_RE.match(line)
        if match:
            return match.group(1).strip()
    return ""


def decode_pack_text(raw: bytes) -> str:
    try:
        return raw.decode("utf-8-sig")
    except UnicodeDecodeError:
        return raw.decode("latin-1", errors="replace")


def extract_localized_names(text: str, source: str) -> Dict[str, str]:
    try:
        data = json.loads(text)
    except Exception as error:
        print(f"[WARN] Could not parse localization JSON {source}: {error}", file=sys.stderr)
        return {}

    if not isinstance(data, dict):
        print(f"[WARN] Localization JSON is not an object: {source}", file=sys.stderr)
        return {}

    localized_names: Dict[str, str] = {}
    for key, value in data.items():
        if not isinstance(key, str) or not isinstance(value, str):
            continue
        key_lower = key.lower()
        if key_lower.startswith(LANG_KEY_PREFIX):
            localized_names[key_lower[len(LANG_KEY_PREFIX):]] = value
    return localized_names


def merge_localized_names(
    destination: Dict[str, str], incoming: Dict[str, str], source: str
) -> None:
    for shortname, localized_name in incoming.items():
        existing = destination.get(shortname)
        if existing is None:
            destination[shortname] = localized_name
        elif existing != localized_name:
            print(
                f"[WARN] Conflicting localization for {shortname!r} in {source}; "
                f"keeping {existing!r} instead of {localized_name!r}",
                file=sys.stderr,
            )


def read_zip_localized_names(zip_file: zipfile.ZipFile, zip_path: Path) -> Dict[str, str]:
    localized_names: Dict[str, str] = {}
    lang_infos = sorted(
        (
            info
            for info in zip_file.infolist()
            if not info.is_dir()
            and info.filename.replace("\\", "/").lower().endswith(LANG_FILE_SUFFIX)
        ),
        key=lambda info: (info.filename.replace("\\", "/").count("/"), info.filename.lower()),
    )
    for info in lang_infos:
        source = f"{zip_path}::{info.filename}"
        try:
            text = decode_pack_text(zip_file.read(info.filename))
        except Exception as error:
            print(f"[WARN] Could not read {source}: {error}", file=sys.stderr)
            continue
        merge_localized_names(localized_names, extract_localized_names(text, source), source)
    return localized_names


def find_resources_dir(definitions_dir: Path) -> Optional[Path]:
    for parent in definitions_dir.parents:
        if parent.name.lower() == "resources":
            return parent
    return None


def read_loose_localized_names(definitions_dir: Path) -> Dict[str, str]:
    resources_dir = find_resources_dir(definitions_dir)
    if resources_dir is None:
        resources_dir = definitions_dir

    lang_path = resources_dir / LANG_FILE_SUFFIX
    localized_names: Dict[str, str] = {}
    legacy_path = resources_dir / "assets/flansmod/lang/en_US.lang"
    if legacy_path.is_file():
        for line in decode_pack_text(legacy_path.read_bytes()).splitlines():
            if "=" not in line or line.lstrip().startswith("#"):
                continue
            key, value = line.split("=", 1)
            if key.startswith("item.") and key.endswith(".name"):
                shortname = key[len("item."):-len(".name")]
                if shortname.startswith("flansmod."):
                    shortname = shortname[len("flansmod."):]
                localized_names[sanitize_shortname(shortname)] = value
    if lang_path.is_file():
        localized_names.update(extract_localized_names(decode_pack_text(lang_path.read_bytes()), str(lang_path)))
    return localized_names


def read_zip_txt_shortnames(zip_path: Path) -> List[ShortnameOrigin]:
    results: List[ShortnameOrigin] = []
    try:
        with zipfile.ZipFile(zip_path, "r") as zip_file:
            localized_names = read_zip_localized_names(zip_file, zip_path)
            for info in zip_file.infolist():
                if info.is_dir():
                    continue

                category = is_txt_in_category(info.filename)
                if not category:
                    continue

                try:
                    raw = zip_file.read(info.filename)
                except Exception as error:
                    print(f"[WARN] Could not read {zip_path}::{info.filename}: {error}", file=sys.stderr)
                    continue

                text = decode_pack_text(raw)

                definition_name = extract_full_name_from_text(text)
                for shortname in extract_shortnames_from_text(text):
                    results.append(
                        ShortnameOrigin(
                            category=category,
                            shortname_lower=shortname,
                            full_name=localized_names.get(shortname, definition_name),
                            zip_path=display_path(zip_path),
                            internal_txt_path=info.filename.replace("\\", "/"),
                        )
                    )
    except Exception as error:
        print(f"[WARN] Failed to process zip {zip_path}: {error}", file=sys.stderr)

    return results


def read_loose_txt_shortnames(flans_content_dir: Path) -> List[ShortnameOrigin]:
    results: List[ShortnameOrigin] = []
    localized_names = read_loose_localized_names(flans_content_dir)
    for txt_path in sorted(flans_content_dir.rglob("*.txt")):
        if not txt_path.is_file():
            continue

        relative_path = txt_path.relative_to(flans_content_dir).as_posix()
        category = is_txt_in_category(relative_path)
        if not category:
            continue

        try:
            text = decode_pack_text(txt_path.read_bytes())
        except Exception as error:
            print(f"[WARN] Could not read {txt_path}: {error}", file=sys.stderr)
            continue

        definition_name = extract_full_name_from_text(text)
        for shortname in extract_shortnames_from_text(text):
            results.append(
                ShortnameOrigin(
                    category=category,
                    shortname_lower=shortname,
                    full_name=localized_names.get(shortname, definition_name),
                    zip_path=display_path(flans_content_dir),
                    internal_txt_path=relative_path,
                )
            )

    return results


def load_category_items(config_dir: Path, category: str) -> Set[str]:
    json_path = config_dir / CATEGORY_TO_JSON[category]
    if not json_path.exists():
        print(f"[WARN] Missing config JSON for category '{category}': {json_path}", file=sys.stderr)
        return set()

    try:
        data = json.loads(json_path.read_text(encoding="utf-8"))
    except Exception as error:
        print(f"[WARN] Could not parse JSON {json_path}: {error}", file=sys.stderr)
        return set()

    items: Set[str] = set()
    if isinstance(data, dict):
        for group in data.values():
            if isinstance(group, dict):
                for item in group.get("items", []):
                    if isinstance(item, str):
                        items.add(item.lower())
    return items


def main() -> int:
    parser = argparse.ArgumentParser(description="Find content-pack short names missing from built-in categories.")
    parser.add_argument("--external", type=Path, action="append", default=[], metavar="PACK", help="Also scan an unpacked pack folder or ZIP (repeatable).")
    parser.add_argument("--no-pause", action="store_true", help="Exit without waiting for Enter.")
    args = parser.parse_args()
    for path in args.external:
        if not path.is_dir() and not (path.is_file() and path.suffix.lower() == ".zip"):
            parser.error(f"External pack is not a directory or ZIP: {path}")
    origins_by_category: Dict[str, List[ShortnameOrigin]] = {
        category: [] for category in CATEGORY_TO_JSON
    }

    for zip_path in iter_zip_files(ZIP_ROOT):
        for origin in read_zip_txt_shortnames(zip_path):
            origins_by_category[origin.category].append(origin)

    for flans_content_dir in iter_flans_content_dirs(SOURCE_ROOT):
        for origin in read_loose_txt_shortnames(flans_content_dir):
            origins_by_category[origin.category].append(origin)

    for path in args.external:
        path = path.resolve()
        origins = read_loose_txt_shortnames(path) if path.is_dir() else read_zip_txt_shortnames(path)
        for origin in origins:
            origins_by_category[origin.category].append(origin)

    json_items_by_category = {
        category: load_category_items(CONFIG_DIR, category)
        for category in CATEGORY_TO_JSON
    }

    missing_rows: List[Tuple[str, ShortnameOrigin]] = []
    for category, origins in origins_by_category.items():
        allowed_items = json_items_by_category.get(category, set())
        if not allowed_items:
            missing_rows.extend(("category_json_missing_or_empty", origin) for origin in origins)
            continue

        for origin in origins:
            if origin.shortname_lower not in allowed_items:
                missing_rows.append(("not_found_in_any_items_list", origin))

    missing_rows.sort(key=lambda row: (row[1].category, row[1].shortname_lower))

    with OUTPUT_CSV.open("w", newline="", encoding="utf-8") as output_file:
        writer = csv.writer(output_file, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        writer.writerow(["category", "shortname", "full_name", "zip_path", "zip_internal_txt_path"])
        for _reason, origin in missing_rows:
            writer.writerow([
                origin.category,
                origin.shortname_lower,
                origin.full_name,
                origin.zip_path,
                origin.internal_txt_path,
            ])

    print(f"[OK] Wrote output: {OUTPUT_CSV}")
    print(f"[OK] Missing entries: {len(missing_rows)}")
    if not args.no_pause and sys.stdin.isatty():
        try:
            input("Press Enter to exit...")
        except EOFError:
            pass
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
