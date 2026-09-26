"""Rename MCP searge identifiers (field_*, func_*, p_*) in decompiled Java sources.

Usage: py apply_mcp_mappings.py <mappings dir with fields/methods/params.csv> <source dir>

Rewrites every .java file under the source dir in place and prints how many
identifiers were renamed and which searge names had no mapping.
"""
import csv
import pathlib
import re
import sys

SEARGE = re.compile(r"\b(?:field_\d+_[A-Za-z]+_?|func_\d+_[A-Za-z]+_?|p_i?\d+_\d+_)\b")


def load(mappings: pathlib.Path) -> dict:
    names = {}
    for file_name in ("fields.csv", "methods.csv", "params.csv"):
        path = mappings / file_name
        if not path.is_file():
            continue
        with path.open(newline="", encoding="utf-8") as handle:
            for row in csv.DictReader(handle):
                searge = row.get("searge") or row.get("param")
                if searge and row.get("name") and row["name"] != searge:
                    names[searge] = row["name"]
    if not names:
        raise SystemExit(f"No mappings found in {mappings}")
    return names


def main() -> None:
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    names = load(pathlib.Path(sys.argv[1]))
    renamed = 0
    unmapped = set()

    def replace(match: re.Match) -> str:
        nonlocal renamed
        token = match.group(0)
        if token in names:
            renamed += 1
            return names[token]
        unmapped.add(token)
        return token

    for source in pathlib.Path(sys.argv[2]).rglob("*.java"):
        text = source.read_text(encoding="utf-8")
        mapped = SEARGE.sub(replace, text)
        if mapped != text:
            source.write_text(mapped, encoding="utf-8", newline="\n")

    print(f"Renamed {renamed} searge identifiers.")
    if unmapped:
        print(f"Unmapped ({len(unmapped)}): {', '.join(sorted(unmapped)[:50])}")


if __name__ == "__main__":
    main()
