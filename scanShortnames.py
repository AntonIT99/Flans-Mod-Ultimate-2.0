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

ZIP_ROOT = Path("run/flan")
CONFIG_DIR = Path("src/main/resources/config")
OUTPUT_CSV = Path("missing_shortnames.csv")

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


@dataclass(frozen=True)
class ShortnameOrigin:
    category: str
    shortname_lower: str
    zip_path: str
    internal_txt_path: str


def iter_zip_files(root: Path) -> Iterable[Path]:
    if not root.exists():
        return []
    return sorted(p for p in root.rglob("*.zip") if p.is_file())


def is_txt_in_category(internal_path: str) -> Optional[str]:
    p = internal_path.replace("\\", "/")
    if not p.lower().endswith(".txt"):
        return None
    for folder, category in FOLDER_TO_CATEGORY.items():
        if p.startswith(folder.rstrip("/") + "/"):
            return category
    return None


def extract_shortnames_from_text(text: str) -> List[str]:
    out: List[str] = []
    for line in text.splitlines():
        m = SHORTNAME_RE.match(line)
        if m:
            out.append(m.group(1).strip().lower())
    return out


def read_zip_txt_shortnames(zip_path: Path) -> List[ShortnameOrigin]:
    results: List[ShortnameOrigin] = []
    try:
        with zipfile.ZipFile(zip_path, "r") as zf:
            for info in zf.infolist():
                if info.is_dir():
                    continue

                category = is_txt_in_category(info.filename)
                if not category:
                    continue

                try:
                    raw = zf.read(info.filename)
                except Exception as e:
                    print(f"[WARN] Could not read {zip_path}::{info.filename}: {e}", file=sys.stderr)
                    continue

                try:
                    text = raw.decode("utf-8")
                except UnicodeDecodeError:
                    text = raw.decode("latin-1", errors="replace")

                for sn in extract_shortnames_from_text(text):
                    results.append(
                        ShortnameOrigin(category, sn, str(zip_path), info.filename.replace("\\", "/"))
                    )
    except Exception as e:
        print(f"[WARN] Failed to process zip {zip_path}: {e}", file=sys.stderr)

    return results


def load_category_items(config_dir: Path, category: str) -> Set[str]:
    json_path = config_dir / CATEGORY_TO_JSON[category]
    if not json_path.exists():
        print(f"[WARN] Missing config JSON for category '{category}': {json_path}", file=sys.stderr)
        return set()

    try:
        data = json.loads(json_path.read_text(encoding="utf-8"))
    except Exception as e:
        print(f"[WARN] Could not parse JSON {json_path}: {e}", file=sys.stderr)
        return set()

    items: Set[str] = set()
    if isinstance(data, dict):
        for group_obj in data.values():
            if isinstance(group_obj, dict):
                for it in group_obj.get("items", []):
                    if isinstance(it, str):
                        items.add(it.lower())
    return items


def main() -> int:
    origins_by_category: Dict[str, List[ShortnameOrigin]] = {c: [] for c in CATEGORY_TO_JSON.keys()}

    for zp in iter_zip_files(ZIP_ROOT):
        for origin in read_zip_txt_shortnames(zp):
            origins_by_category[origin.category].append(origin)

    json_items_by_category = {
        cat: load_category_items(CONFIG_DIR, cat) for cat in CATEGORY_TO_JSON.keys()
    }

    missing_rows: List[Tuple[str, ShortnameOrigin]] = []
    for category, origins in origins_by_category.items():
        allowed_items = json_items_by_category.get(category, set())
        if not allowed_items:
            for origin in origins:
                missing_rows.append(("category_json_missing_or_empty", origin))
            continue

        for origin in origins:
            if origin.shortname_lower not in allowed_items:
                missing_rows.append(("not_found_in_any_items_list", origin))

    missing_rows.sort(key=lambda x: (x[1].category, x[1].shortname_lower))

    with OUTPUT_CSV.open("w", newline="", encoding="utf-8") as f:
        writer = csv.writer(f, delimiter=";", quoting=csv.QUOTE_MINIMAL)
        writer.writerow(["category", "shortname", "reason", "zip_path", "zip_internal_txt_path"])
        for reason, origin in missing_rows:
            writer.writerow([origin.category, origin.shortname_lower, reason, origin.zip_path, origin.internal_txt_path])

    print(f"[OK] Wrote output: {OUTPUT_CSV.resolve()}")
    print(f"[OK] Missing entries: {len(missing_rows)}")
    input("Press Enter to exit...")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
