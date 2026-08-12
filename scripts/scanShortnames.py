#!/usr/bin/env python3
from __future__ import annotations

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
CONFIG_DIR = PROJECT_ROOT / "src" / "main" / "resources" / "config"
OUTPUT_CSV = PROJECT_ROOT / "missing_shortnames.csv"

FOLDER_TO_CATEGORY: Dict[str, str] = {
    "armorFiles": "armor",
    "guns": "gun",
    "grenades": "grenade",
    "bullets": "bullet",
}

CATEGORY_TO_JSON: Dict[str, str] = {
    "armor": "armor_categories.json",
    "gun": "gun_categories.json",
    "grenade": "grenade_categories.json",
    "bullet": "bullet_categories.json",
}

SHORTNAME_RE = re.compile(r"^\s*Shortname\s+(\S+)\s*$", re.IGNORECASE)
NAME_RE = re.compile(r"^\s*Name\s+(.+?)\s*$", re.IGNORECASE)


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


def is_txt_in_category(internal_path: str) -> Optional[str]:
    normalized_path = internal_path.replace("\\", "/")
    if not normalized_path.lower().endswith(".txt"):
        return None
    for folder, category in FOLDER_TO_CATEGORY.items():
        if normalized_path.startswith(folder.rstrip("/") + "/"):
            return category
    return None


def extract_shortnames_from_text(text: str) -> List[str]:
    shortnames: List[str] = []
    for line in text.splitlines():
        match = SHORTNAME_RE.match(line)
        if match:
            shortnames.append(match.group(1).strip().lower())
    return shortnames


def extract_full_name_from_text(text: str) -> str:
    for line in text.splitlines():
        match = NAME_RE.match(line)
        if match:
            return match.group(1).strip()
    return ""


def read_zip_txt_shortnames(zip_path: Path) -> List[ShortnameOrigin]:
    results: List[ShortnameOrigin] = []
    try:
        with zipfile.ZipFile(zip_path, "r") as zip_file:
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

                try:
                    text = raw.decode("utf-8")
                except UnicodeDecodeError:
                    text = raw.decode("latin-1", errors="replace")

                full_name = extract_full_name_from_text(text)
                for shortname in extract_shortnames_from_text(text):
                    results.append(
                        ShortnameOrigin(
                            category=category,
                            shortname_lower=shortname,
                            full_name=full_name,
                            zip_path=str(zip_path.relative_to(PROJECT_ROOT)),
                            internal_txt_path=info.filename.replace("\\", "/"),
                        )
                    )
    except Exception as error:
        print(f"[WARN] Failed to process zip {zip_path}: {error}", file=sys.stderr)

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
    origins_by_category: Dict[str, List[ShortnameOrigin]] = {
        category: [] for category in CATEGORY_TO_JSON
    }

    for zip_path in iter_zip_files(ZIP_ROOT):
        for origin in read_zip_txt_shortnames(zip_path):
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
    input("Press Enter to exit...")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
