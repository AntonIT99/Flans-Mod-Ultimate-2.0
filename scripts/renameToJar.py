from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent.parent
FLAN_DIR = PROJECT_ROOT / "run" / "flan"


def main() -> None:
    if not FLAN_DIR.is_dir():
        print(f'"{FLAN_DIR}" does not exist.')
    else:
        for zip_path in FLAN_DIR.glob("*.zip"):
            jar_path = zip_path.with_suffix(".jar")
            zip_path.rename(jar_path)
            print(f"Renamed: {zip_path.name} -> {jar_path.name}")

    print("Done.")
    input("Press Enter to exit...")


if __name__ == "__main__":
    main()
