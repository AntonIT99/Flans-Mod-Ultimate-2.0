"""Generates the sound length index shipped with the built-in content packs.

Content packs in the flan folder get their index written while they are reprocessed at runtime.
The packs bundled in this repository are never reprocessed, so their index is generated here and
committed alongside the sound files.

The output must stay identical to what com.flansmodultimate.util.SoundLengthIndex writes, so the
parsing, the tick arithmetic and the JSON layout below mirror that class.

Usage:
    python scripts/buildSoundLengthIndexes.py            regenerate every index
    python scripts/buildSoundLengthIndexes.py --check    report outdated indexes without writing
"""

import argparse
import json
import re
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
ASSETS_GLOB = "src/*/resources/assets/flansmod"
SOUNDS_FOLDER_NAME = "sounds"
INDEX_FILE_NAME = "sound-lengths.json"

FORMAT_VERSION = 2
TICKS_PER_SECOND = 20
TICKS_UNKNOWN = 0

CAPTURE_PATTERN = b"OggS"
VORBIS_IDENTIFICATION = b"\x01vorbis"
PAGE_HEADER_SIZE = 27
MAX_PAGE_SIZE = PAGE_HEADER_SIZE + 255 + 255 * 255
HEAD_PROBE_SIZE = 512
OFFSET_GRANULE_POSITION = 6
OFFSET_SEGMENT_COUNT = 26
OFFSET_IDENTIFICATION_SAMPLE_RATE = 12
GRANULE_POSITION_NONE = -1


def read_sample_rate(head: bytes) -> int:
    """Reads the sample rate from the Vorbis identification header carried by the first page."""
    if not head.startswith(CAPTURE_PATTERN) or len(head) <= OFFSET_SEGMENT_COUNT:
        return 0

    segment_count = head[OFFSET_SEGMENT_COUNT]
    payload_start = PAGE_HEADER_SIZE + segment_count
    sample_rate_start = payload_start + OFFSET_IDENTIFICATION_SAMPLE_RATE
    if len(head) < sample_rate_start + 4:
        return 0

    if head[payload_start:payload_start + len(VORBIS_IDENTIFICATION)] != VORBIS_IDENTIFICATION:
        return 0

    return int.from_bytes(head[sample_rate_start:sample_rate_start + 4], "little")


def read_last_granule_position(tail: bytes) -> int:
    """Scans backwards for the last page that finishes a packet, whose granule position is the sample count."""
    for offset in range(len(tail) - PAGE_HEADER_SIZE, -1, -1):
        if tail[offset:offset + len(CAPTURE_PATTERN)] != CAPTURE_PATTERN:
            continue

        granule_start = offset + OFFSET_GRANULE_POSITION
        granule_position = int.from_bytes(tail[granule_start:granule_start + 8], "little", signed=True)
        if granule_position != GRANULE_POSITION_NONE:
            return granule_position

    return GRANULE_POSITION_NONE


def read_duration_ticks(file: Path) -> int:
    """Reads how long an Ogg Vorbis file plays for, in ticks, or TICKS_UNKNOWN when it cannot be measured."""
    file_size = file.stat().st_size
    if file_size < PAGE_HEADER_SIZE:
        return TICKS_UNKNOWN

    with file.open("rb") as stream:
        head = stream.read(HEAD_PROBE_SIZE)
        sample_rate = read_sample_rate(head)
        if sample_rate <= 0:
            return TICKS_UNKNOWN

        tail_size = min(file_size, MAX_PAGE_SIZE)
        stream.seek(file_size - tail_size)
        total_samples = read_last_granule_position(stream.read(tail_size))

    if total_samples <= 0:
        return TICKS_UNKNOWN

    # Integer division, matching Java. Truncating makes a repeated sound overlap itself
    # inaudibly rather than leave an audible gap.
    ticks = total_samples * TICKS_PER_SECOND // sample_rate
    return max(1, min(ticks, 2 ** 31 - 1))


def to_sound_event_key(relative_path: str) -> str:
    """Derives the sound event name a file is registered under, mirroring ResourceUtils.sanitize."""
    file_name = relative_path.rsplit("/", 1)[-1]
    if file_name.lower().endswith(".ogg"):
        file_name = file_name[:-len(".ogg")]

    return re.sub(r"[^a-z0-9._\-]", "_", file_name.lower().replace(" ", "_"))


def build_index(sounds_dir: Path) -> dict:
    """Builds the index document for one sounds folder, or an empty document when it holds no sounds."""
    sound_files = sorted(
        (path for path in sounds_dir.rglob("*") if path.is_file() and path.suffix.lower() == ".ogg"),
        key=lambda path: path.relative_to(sounds_dir).as_posix(),
    )

    entries = {}
    for sound_file in sound_files:
        relative_path = sound_file.relative_to(sounds_dir).as_posix()
        ticks = read_duration_ticks(sound_file)
        if ticks == TICKS_UNKNOWN:
            print(f"  WARNING: could not determine the length of {relative_path}")

        entries[relative_path] = {
            "key": to_sound_event_key(relative_path),
            "size": sound_file.stat().st_size,
            "ticks": ticks,
        }

    return {"version": FORMAT_VERSION, "sounds": entries} if entries else {}


def serialize(index: dict) -> str:
    return json.dumps(index, indent=2, ensure_ascii=False) + "\n"


def process(assets_dir: Path, check_only: bool) -> bool:
    """Writes or verifies one index. Returns True when the index on disk is already up to date."""
    sounds_dir = assets_dir / SOUNDS_FOLDER_NAME
    index_file = assets_dir / INDEX_FILE_NAME
    module = assets_dir.relative_to(PROJECT_ROOT).as_posix()

    if not sounds_dir.is_dir():
        return True

    print(f"{module}")
    index = build_index(sounds_dir)
    expected = serialize(index) if index else None
    actual = index_file.read_text(encoding="utf-8") if index_file.is_file() else None

    if expected == actual:
        print(f"  up to date ({len(index.get('sounds', {}))} sound(s))")
        return True

    if check_only:
        print(f"  OUTDATED: {index_file.relative_to(PROJECT_ROOT).as_posix()}")
        return False

    if expected is None:
        index_file.unlink()
        print("  removed (no sounds)")
    else:
        index_file.write_text(expected, encoding="utf-8")
        print(f"  wrote {len(index['sounds'])} sound(s) to {index_file.relative_to(PROJECT_ROOT).as_posix()}")

    return False


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--check", action="store_true",
                        help="report outdated indexes and exit non-zero instead of writing them")
    arguments = parser.parse_args()

    assets_dirs = sorted(PROJECT_ROOT.glob(ASSETS_GLOB))
    if not assets_dirs:
        print(f"No asset folders matched {ASSETS_GLOB} under {PROJECT_ROOT}")
        return 1

    outdated = [assets_dir for assets_dir in assets_dirs if not process(assets_dir, arguments.check)]

    if arguments.check and outdated:
        print(f"\n{len(outdated)} sound length index/indexes are outdated. "
              f"Run: python scripts/buildSoundLengthIndexes.py")
        return 1

    print("\nFinished!")
    return 0


if __name__ == "__main__":
    sys.exit(main())
