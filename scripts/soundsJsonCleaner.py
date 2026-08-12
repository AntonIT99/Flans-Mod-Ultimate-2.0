import json
from collections import Counter
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
INPUT_JSON = PROJECT_ROOT / "sounds.json"
OUTPUT_JSON = PROJECT_ROOT / "sounds_cleaned.json"


def main() -> None:
    with INPUT_JSON.open("r", encoding="utf-8") as input_file:
        data = json.load(input_file)

    key_counts = Counter(data.keys())
    cleaned_data = {key: value for key, value in data.items() if key_counts[key] == 1}

    with OUTPUT_JSON.open("w", encoding="utf-8") as output_file:
        json.dump(cleaned_data, output_file, indent=4)

    print(f"Finished! Cleaned sounds saved to '{OUTPUT_JSON}'")


if __name__ == "__main__":
    main()
