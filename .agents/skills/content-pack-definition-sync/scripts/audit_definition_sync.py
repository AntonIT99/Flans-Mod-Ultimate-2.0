#!/usr/bin/env python3
"""Audit category/definition drift without modifying content packs."""

from __future__ import annotations

import argparse
import json
import re
import sys
from collections import defaultdict
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path
from typing import Iterable

CATEGORY_BY_FOLDER = {
    "aaguns": "aagun",
    "armorfiles": "armor",
    "bullets": "bullet",
    "grenades": "grenade",
    "guns": "gun",
    "mechas": "mecha",
    "planes": "plane",
    "vehicles": "vehicle",
}

# These keys are consumed as collections or ordered lines. Mirroring a category
# value into a definition can make CategoryManager append it a second time.
REPEATABLE_KEYS = {
    "addround",
    "addroundforammo",
    "addtoammogroup",
    "ammo",
    "ammoexplosivemasstntg",
    "ammoexplosivemasstntkg",
    "ammomass",
    "ammomasskg",
    "ammomuzzlevelocity",
    "ammopenetrationat100m",
    "partarmormm",
    "removeammo",
    "useammogroup",
}


@dataclass(frozen=True)
class CategoryProperty:
    category: str
    key: str
    values: tuple[str, ...]
    mode: str


def sanitize_shortname(value: str) -> str:
    value = value.lower().replace(" ", "_")
    return re.sub(r"[^a-z0-9._-]", "_", value)


def json_values(value: object) -> tuple[str, ...]:
    raw = value if isinstance(value, list) else [value]
    result: list[str] = []
    for entry in raw:
        if isinstance(entry, bool):
            result.append(str(entry).lower())
        elif entry is None:
            result.append("")
        else:
            result.append(str(entry))
    return tuple(result)


def property_mode(category: dict[str, object], key: str) -> str:
    modes = category.get("propertyModes") or {}
    if not isinstance(modes, dict):
        return "append"
    value = next(
        (mode for mode_key, mode in modes.items() if str(mode_key).lower() == key.lower()),
        "append",
    )
    normalized = str(value).strip().lower()
    return normalized if normalized in {"replace", "ifabsent"} else "append"


def apply_property_modes(
    initial: Iterable[str], assignments: Iterable[CategoryProperty]
) -> tuple[str, ...]:
    effective = list(initial)
    for assignment in assignments:
        if assignment.mode == "replace":
            effective = list(assignment.values)
        elif assignment.mode == "ifabsent":
            if not effective:
                effective = list(assignment.values)
        else:
            effective.extend(assignment.values)
    return tuple(effective)


def canonical_lines(values: Iterable[str]) -> tuple[tuple[str, ...], ...]:
    return tuple(canonical_value(value) for value in values)


def strip_inline_comment(value: str) -> str:
    quote = ""
    depth = 0
    i = 0
    while i < len(value):
        char = value[i]
        if quote:
            if char == quote:
                quote = ""
        elif char in "\"'":
            quote = char
        elif char in "[({":
            depth += 1
        elif char in "]) }".replace(" ", ""):
            depth = max(0, depth - 1)
        elif char == "/" and i + 1 < len(value) and value[i + 1] == "/" and depth == 0:
            return value[:i].strip()
        i += 1
    return value.strip()


def canonical_token(token: str) -> str:
    lowered = token.lower()
    if lowered in {"true", "false"}:
        return lowered
    try:
        number = Decimal(token)
    except InvalidOperation:
        return token
    if not number.is_finite():
        return token
    return format(number.normalize(), "f")


def canonical_value(value: str) -> tuple[str, ...]:
    value = strip_inline_comment(value.strip())
    if value.startswith("="):
        value = value[1:].strip()
    return tuple(canonical_token(token) for token in value.split())


def parse_definition(path: Path) -> dict[str, list[str | None]]:
    raw = path.read_bytes()
    text = None
    for encoding in ("utf-8-sig", "utf-16", "cp1252"):
        try:
            text = raw.decode(encoding)
            break
        except UnicodeDecodeError:
            continue
    if text is None:
        raise UnicodeError("unsupported text encoding")

    fields: dict[str, list[str | None]] = defaultdict(list)
    for line in text.splitlines():
        if not line.strip() or line.lstrip().startswith("//"):
            continue
        split = line.strip().split(maxsplit=1)
        fields[split[0].lower()].append(split[1] if len(split) == 2 else None)
    return fields


def definition_type(path: Path) -> str | None:
    parts = list(path.parts)
    lowered = [part.lower() for part in parts]
    try:
        index = max(i for i, part in enumerate(lowered) if part == "definitions")
    except ValueError:
        return None
    if index + 1 >= len(parts):
        return None
    return CATEGORY_BY_FOLDER.get(lowered[index + 1])


def is_source_definition(path: Path) -> bool:
    lowered = [part.lower() for part in path.parts]
    return (
        "src" in lowered
        and "resources" in lowered
        and "flans_content" in lowered
        and "definitions" in lowered
    )


def iter_definitions(paths: Iterable[Path]) -> list[Path]:
    found: set[Path] = set()
    for path in paths:
        if path.is_file() and path.suffix.lower() == ".txt":
            candidates = [path]
        elif path.is_dir():
            candidates = path.rglob("*.txt")
        else:
            print(f"WARN missing scope: {path}", file=sys.stderr)
            continue
        for candidate in candidates:
            resolved = candidate.resolve()
            if is_source_definition(resolved) and definition_type(resolved):
                found.add(resolved)
    return sorted(found, key=lambda item: str(item).lower())


def load_categories(repo: Path) -> dict[str, dict[str, list[CategoryProperty]]]:
    result: dict[str, dict[str, list[CategoryProperty]]] = {}
    config = repo / "src" / "main" / "resources" / "config"
    for identifier in sorted(set(CATEGORY_BY_FOLDER.values())):
        path = config / f"{identifier}_categories.json"
        with path.open("r", encoding="utf-8") as handle:
            document = json.load(handle)
        membership: dict[str, list[CategoryProperty]] = defaultdict(list)
        for category_name, category in document.items():
            properties = category.get("properties") or {}
            exceptions = category.get("exceptions") or {}
            for item in category.get("items") or []:
                item_key = str(item).lower()
                for key, value in properties.items():
                    excluded = any(
                        exception_key.lower() == key.lower()
                        and item_key in {str(entry).lower() for entry in (exception_items or [])}
                        for exception_key, exception_items in exceptions.items()
                    )
                    if not excluded:
                        membership[item_key].append(
                            CategoryProperty(
                                category_name,
                                key,
                                json_values(value),
                                property_mode(category, key),
                            )
                        )
        result[identifier] = membership
    return result


def relative(path: Path, repo: Path) -> str:
    try:
        return str(path.relative_to(repo))
    except ValueError:
        return str(path)


def main() -> int:
    script = Path(__file__).resolve()
    default_repo = script.parents[4]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=default_repo, help="repository root")
    parser.add_argument(
        "--path",
        action="append",
        type=Path,
        dest="paths",
        help="file or directory to audit; repeatable",
    )
    args = parser.parse_args()

    repo = args.repo.resolve()
    scopes = args.paths or [repo / "src"]
    scopes = [(path if path.is_absolute() else repo / path).resolve() for path in scopes]
    definitions = iter_definitions(scopes)
    categories = load_categories(repo)

    counts: dict[str, int] = defaultdict(int)
    findings: list[str] = []
    for path in definitions:
        counts["definitions"] += 1
        identifier = definition_type(path)
        assert identifier is not None
        try:
            fields = parse_definition(path)
        except (OSError, UnicodeError) as error:
            counts["errors"] += 1
            findings.append(f"ERROR {relative(path, repo)} :: unreadable definition: {error}")
            continue

        short_values = fields.get("shortname", [])
        short_raw = next((value for value in reversed(short_values) if value), None)
        if not short_raw:
            counts["errors"] += 1
            findings.append(f"ERROR {relative(path, repo)} :: missing ShortName")
            continue
        shortname = sanitize_shortname(strip_inline_comment(short_raw).split()[0])
        properties = categories[identifier].get(shortname, [])
        if not properties:
            counts["uncategorized"] += 1
            findings.append(
                f"WARN  {relative(path, repo)} :: {shortname} has no {identifier} category"
            )
            continue

        by_key: dict[str, list[CategoryProperty]] = defaultdict(list)
        for prop in properties:
            by_key[prop.key.lower()].append(prop)

        for key_lower, assignments in sorted(by_key.items()):
            key = assignments[-1].key
            expected_lines = tuple(
                value for assignment in assignments for value in assignment.values
            )
            observed = tuple(value for value in fields.get(key_lower, []) if value is not None)
            if key_lower in REPEATABLE_KEYS or len(expected_lines) > 1:
                modes = ", ".join(
                    f"{assignment.category}:{assignment.mode}" for assignment in assignments
                )
                if any(assignment.mode == "replace" for assignment in assignments):
                    expected_fallback = apply_property_modes((), assignments)
                    if not observed:
                        counts["repeatable_sync_gaps"] += 1
                        findings.append(
                            f"GAP   {relative(path, repo)} :: {key} mode chain [{modes}] "
                            f"resolves to {len(expected_fallback)} fallback line(s); "
                            "definition is missing them"
                        )
                    elif canonical_lines(observed) != canonical_lines(expected_fallback):
                        counts["repeatable_sync_mismatches"] += 1
                        findings.append(
                            f"DIFF  {relative(path, repo)} :: {key} mode chain [{modes}] "
                            f"resolves to {expected_fallback!r}, definition has {observed!r}"
                        )
                elif all(assignment.mode == "ifabsent" for assignment in assignments):
                    if observed:
                        counts["repeatable_if_absent_preserved"] += 1
                    else:
                        expected_fallback = apply_property_modes((), assignments)
                        counts["repeatable_sync_gaps"] += 1
                        findings.append(
                            f"GAP   {relative(path, repo)} :: {key} mode chain [{modes}] "
                            f"provides {len(expected_fallback)} safe fallback line(s); "
                            "definition is missing them"
                        )
                elif observed:
                    counts["repeatable_overlaps"] += 1
                    findings.append(
                        f"INFO  {relative(path, repo)} :: {key} is repeatable; "
                        f"definition has {len(observed)} line(s), category mode chain [{modes}] "
                        f"contributes {len(expected_lines)} line(s)"
                    )
                else:
                    counts["repeatable_category_only"] += 1
                continue

            all_if_absent = all(assignment.mode == "ifabsent" for assignment in assignments)
            if all_if_absent and observed:
                counts["scalar_if_absent_preserved"] += 1
                continue

            effective_values = {
                canonical_value(assignment.values[-1])
                for assignment in assignments
                if assignment.values
            }
            if (
                all(assignment.mode == "append" for assignment in assignments)
                and len(effective_values) > 1
            ):
                counts["errors"] += 1
                sources = ", ".join(
                    f"{assignment.category}={assignment.values[-1]!r}"
                    for assignment in assignments
                )
                findings.append(
                    f"ERROR {relative(path, repo)} :: conflicting category values for {key}: {sources}"
                )
                continue

            resolved = apply_property_modes((), assignments)
            if not resolved:
                continue
            expected = resolved[-1]
            if not observed:
                counts["scalar_gaps"] += 1
                findings.append(
                    f"GAP   {relative(path, repo)} :: {key} expected {expected!r}, definition is missing it"
                )
            elif canonical_value(observed[-1]) != canonical_value(expected):
                counts["scalar_mismatches"] += 1
                findings.append(
                    f"DIFF  {relative(path, repo)} :: {key} expected {expected!r}, "
                    f"definition has {observed[-1]!r}"
                )

    for finding in findings:
        print(finding)
    print(
        "SUMMARY "
        + " ".join(
            f"{key}={counts[key]}"
            for key in (
                "definitions",
                "scalar_gaps",
                "scalar_mismatches",
                "repeatable_overlaps",
                "repeatable_category_only",
                "repeatable_sync_gaps",
                "repeatable_sync_mismatches",
                "repeatable_if_absent_preserved",
                "scalar_if_absent_preserved",
                "uncategorized",
                "errors",
            )
        )
    )
    return 1 if (
        counts["scalar_gaps"]
        or counts["scalar_mismatches"]
        or counts["repeatable_sync_gaps"]
        or counts["repeatable_sync_mismatches"]
        or counts["errors"]
    ) else 0


if __name__ == "__main__":
    raise SystemExit(main())
