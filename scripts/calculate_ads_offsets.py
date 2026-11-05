"""Calculate iron-sight animAimOffset candidates without loading Minecraft.

Run: python scripts/calculate_ads_offsets.py
Anchors are explicit points in a selected model part's local coordinates.
Results assume a fully equipped right-hand gun, full ADS, standing, no attachment,
recoil, bobbing, equipment transforms or debug previews. The selected view frame
must match the client. Existing aim-offset Y is ignored; X and Z are preserved.
Only literal model transforms are supported; ambiguous inputs fail explicitly.
"""

import argparse
import json
import math
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_ANCHORS = Path(__file__).with_name("ads_sight_anchors.json")
CENTRES = {"1.7.10": 0.0009249, "1.12.2": -0.005, "tap": 0.0009249}
NUMBER = r"[-+]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][-+]?\d+)?[FfDd]?"


def identity():
    return [[float(i == j) for j in range(4)] for i in range(4)]


def multiply(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def transform(matrix, point):
    vector = [*point, 1.0]
    return [sum(matrix[i][k] * vector[k] for k in range(4)) for i in range(3)]


def translation(x, y, z):
    matrix = identity()
    for i, value in enumerate((x, y, z)):
        matrix[i][3] = value
    return matrix


def scaling(value):
    matrix = identity()
    for i in range(3):
        matrix[i][i] = value
    return matrix


def rotation(axis, degrees):
    c, s = math.cos(math.radians(degrees)), math.sin(math.radians(degrees))
    rows = {
        "x": [[1, 0, 0], [0, c, -s], [0, s, c]],
        "y": [[c, 0, s], [0, 1, 0], [-s, 0, c]],
        "z": [[c, -s, 0], [s, c, 0], [0, 0, 1]],
    }
    matrix = identity()
    for i, row in enumerate(rows[axis]):
        matrix[i][:3] = row
    return matrix


def numbers(text):
    values = text.split(",")
    if not all(re.fullmatch(NUMBER, value.strip()) for value in values):
        raise ValueError(f"Expected numeric literals: {text}")
    return [float(value.strip().rstrip("FfDd")) for value in values]


def directives(text):
    result = {}
    relevant = {"scope", "model", "modelscale", "firstpersonframe", "ironsightoffset", "ironsightpitch", "animadseffectmultiplier", "animcrouchzoom", "animaimoffset"}
    for line in text.splitlines():
        words = line.split("//", 1)[0].split()
        if words:
            key = words[0].lower()
            if key not in relevant:
                continue
            if key in result:
                raise ValueError(f"Duplicate directive requires parser review: {words[0]}")
            result[key] = words[1:]
    return result


def model_point(source, anchor):
    """Resolve an explicit local anchor through the model's static placement."""
    source = re.sub(r"/\*.*?\*/|//[^\n]*", "", source, flags=re.S)
    part = re.escape(anchor["part"])
    angles = {}
    for axis in "xyz":
        assignments = re.findall(part + r"\.rotateAngle" + axis.upper() + r"\s*=\s*([^;]+);", source)
        if len(assignments) > 1:
            raise ValueError("Multiple anchor rotations require model review")
        angles[axis] = numbers(assignments[0])[0] if assignments else 0
    pivots = list(re.finditer(part + r"\.setRotationPoint\(([^)]+)\)", source))
    if len(pivots) > 1:
        raise ValueError("Multiple anchor pivots require model review")
    pivot = numbers(pivots[0][1]) if pivots else [0, 0, 0]
    local = list(anchor["point"])
    operations = list(re.finditer(r"\b(flipAll|translateAll)\(([^)]*)\)", source))
    for operation in operations:
        if pivots and operation.start() < pivots[0].end():
            raise ValueError("Whole-model transforms before anchor setup are unsupported")
        if operation[1] == "flipAll":
            # ModelGun.flip mirrors vertices and pivots, but does not negate angles.
            local = [local[0], -local[1], -local[2]]
            pivot = [pivot[0], -pivot[1], -pivot[2]]
        else:
            pivot = [a + b for a, b in zip(pivot, numbers(operation[2]))]
    # ModelRendererTurbo normal render order: Ry * Rz * Rx.
    matrix = identity()
    for axis in "yzx":
        matrix = multiply(matrix, rotation(axis, math.degrees(angles[axis])))
    return [a + b for a, b in zip(transform(matrix, local), pivot)]


def ads_matrix(view, authored, ads_multiplier=1.0):
    """Column-vector PoseStack order from vanilla and GunItemRenderer, at full ADS."""
    # ItemInHandRenderer.applyItemArmTransform; equipped progress and swing are zero.
    operations = [translation(0.56, -0.52, -0.72), rotation("y", 45), scaling(0.4)]
    a = ads_multiplier
    if view == "1.12.2":
        operations += [rotation("y", 45), rotation("z", -5 * a), translation(-1, 0.675 + 0.180 * a, -1 - 0.395 * a)]
    else:
        # LegacyHeldItemFrames.equippedItem; full ADS does not remove every rotation.
        operations += [translation(0, -0.3, 0), scaling(1.5), rotation("y", 50), rotation("z", 335), translation(-0.9375, -0.0625, 0),
                       rotation("z", 25 - 5 * a), rotation("y", -5)]
        if view == "tap":
            operations += [translation(0, -0.03 * a, 0), translation(0.15, 0.2 + 0.205 * a, -0.8 - 0.205 * a)]
        else:
            operations += [translation(0.15, 0.2 + 0.175 * a, -0.6 - 0.405 * a)]
    operations += [rotation("z", 4.5 * a), translation(0, -0.03 * a, 0), translation(0, 0, (CENTRES[view] - CENTRES[authored]) * a)]
    matrix = identity()
    for operation in operations:
        matrix = multiply(matrix, operation)
    return matrix


def solve(matrix, point, scale, sight_offset=(0, 0, 0), pitch=0, solve_z=True, existing=(0, 0, 0)):
    """Solve camera Y only, or the coupled X/Y system; keep aim-offset X zero."""
    unit = scale / 16
    geometry = multiply(translation(*sight_offset), multiply(rotation("z", pitch), scaling(unit)))
    fixed = translation(existing[0] * unit, 0, 0 if solve_z else existing[2] * unit)
    camera = transform(multiply(matrix, multiply(fixed, geometry)), point)
    a, b = matrix[0][1] * unit, matrix[0][2] * unit
    c, d = matrix[1][1] * unit, matrix[1][2] * unit
    determinant = a * d - b * c
    if abs(determinant) < 1e-12:
        raise ValueError("Cannot solve ADS Y/Z for this transform and model scale")
    if solve_z:
        y = (-camera[0] * d + b * camera[1]) / determinant
        z = (-a * camera[1] + camera[0] * c) / determinant
    else:
        if abs(c) < 1e-12:
            raise ValueError("Cannot solve vertical ADS placement using Y alone")
        y, z = -camera[1] / c, existing[2]
    final = multiply(matrix, multiply(translation(existing[0] * unit, y * unit, z * unit), geometry))
    projected = transform(final, point)
    if projected[2] >= 0:
        raise ValueError("Selected sight point is behind the camera")
    return [existing[0], y, z], camera, projected


def calculate(entry, view="1.7.10", root=ROOT, solve_z=False):
    definition = root / entry["definition"]
    data = directives(definition.read_text())
    if data.get("scope", ["None"])[0].lower() != "none":
        raise ValueError("Scope-overlay definitions are excluded from this calculator")
    for key in ("animadseffectmultiplier", "animcrouchzoom"):
        if key in data:
            raise ValueError(f"Definition override needs explicit support: {key}")
    model = data["model"][0].split(".")
    path = root / "src/officialpacks/java/com/flansmod/client/model" / Path(*model[:-1]) / ("Model" + model[-1] + ".java")
    source = path.read_text()
    point = model_point(source, entry["front"])
    scale = float(data.get("modelscale", ["1"])[0])
    authored = data.get("firstpersonframe", ["1.7.10"])[0].lower()
    if authored in ("tap", "labjac"):
        authored = "1.7.10"
    multipliers = re.findall(r"\badsEffectMultiplier\s*=\s*(" + NUMBER + r")\s*;", source)
    if len(multipliers) > 1:
        raise ValueError("Multiple ADS multipliers require model review")
    multiplier = numbers(multipliers[0])[0] if multipliers else 1
    if view == "tap" and re.search(r"\bcrouchZoom\s*=", source):
        raise ValueError("Tap crouchZoom translation needs explicit support")
    offset = [float(v.rstrip(",")) for v in data.get("ironsightoffset", ["0", "0", "0"])]
    pitch = float(data.get("ironsightpitch", ["0"])[0])
    matrix = ads_matrix(view, authored, multiplier)
    existing = [float(v.rstrip(",")) for v in data.get("animaimoffset", ["0", "0", "0"])]
    aim, before, after = solve(matrix, point, scale, offset, pitch, solve_z, existing)
    comparison = {}
    unit = scale / 16
    geometry = multiply(translation(*offset), multiply(rotation("z", pitch), scaling(unit)))
    for compared_view in ("1.7.10", "1.12.2"):
        compared_matrix = ads_matrix(compared_view, authored, multiplier)
        errors = {}
        for label, candidate in (("before", existing), ("after", aim)):
            placed = transform(multiply(compared_matrix, multiply(translation(*(v * unit for v in candidate)), geometry)), point)
            errors[label] = round(math.degrees(math.atan2(placed[1], -placed[2])), 6)
        comparison[compared_view] = errors
    return {"gun": definition.stem, "view_frame": view, "anchor": entry["front"], "placed_anchor": point,
            "animAimOffset": [round(v, 8) for v in aim], "camera_before": before, "camera_after": after,
            "vertical_error_degrees": comparison,
            "note": "Centers the front-sight point " + ("horizontally and vertically" if solve_z else "vertically, preserving authored aim-offset X and Z")
                    + "; does not align the rear sight or change pitch."}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--anchors", type=Path, default=DEFAULT_ANCHORS)
    parser.add_argument("--view-frame", choices=CENTRES, default="1.7.10")
    parser.add_argument("--solve-z", action="store_true", help="Also solve sideways centering; the default changes Y only")
    args = parser.parse_args()
    entries = json.loads(args.anchors.read_text())["guns"]
    print(json.dumps([calculate(entry, args.view_frame, solve_z=args.solve_z) for entry in entries], indent=2))


if __name__ == "__main__":
    main()
