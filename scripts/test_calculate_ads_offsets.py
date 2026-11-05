"""Run with python -m unittest discover -s scripts -p test_calculate_ads_offsets.py."""

import json
import unittest

import calculate_ads_offsets as ads


class AdsOffsetTest(unittest.TestCase):
    def test_solves_coupled_axes_at_several_scales(self):
        matrix = ads.multiply(ads.translation(0.3, -0.4, -2), ads.rotation("y", 90))
        for scale in (0.35, 0.6, 1.3):
            with self.subTest(scale=scale):
                aim, _, final = ads.solve(matrix, (10, 5, 2), scale)
                self.assertAlmostEqual(-0.3 * 16 / scale - 2, aim[2])
                self.assertAlmostEqual(0.4 * 16 / scale - 5, aim[1])
                self.assertAlmostEqual(0, final[0])
                self.assertAlmostEqual(0, final[1])

    def test_whole_model_transform_order(self):
        pivot = "gunModel[0].setRotationPoint(1F, -4F, -0.5F);"
        anchor = {"part": "gunModel[0]", "point": [0.5, -2, 0.5]}
        self.assertEqual([1.5, 9, 0], ads.model_point(pivot + "flipAll(); translateAll(0F, 3F, 0F);", anchor))
        self.assertEqual([1.5, 3, 0], ads.model_point(pivot + "translateAll(0F, 3F, 0F); flipAll();", anchor))

    def test_rejects_nonliteral_transforms(self):
        anchor = {"part": "gunModel[0]", "point": [0, 0, 0]}
        source = "gunModel[0].setRotationPoint(0F, 0F, 0F);"
        for extra in ("gunModel[0].rotateAngleY = angle;", "translateAll(x, 0F, 0F);"):
            with self.subTest(extra=extra), self.assertRaises(ValueError):
                ads.model_point(source + extra, anchor)

    def test_mirroring_precedes_part_rotation_and_does_not_negate_angles(self):
        source = "gunModel[0].setRotationPoint(1F, -4F, -0.5F); gunModel[0].rotateAngleZ = 1.5707963267948966F; flipAll(); translateAll(0F, 3F, 0F);"
        point = ads.model_point(source, {"part": "gunModel[0]", "point": [0.5, -2, 0.5]})
        for actual, expected in zip(point, (-1, 7.5, 0)):
            self.assertAlmostEqual(expected, actual)

    def test_implicit_zero_pivot_is_supported(self):
        point = ads.model_point("flipAll();", {"part": "gunModel[0]", "point": [1, -2, 3]})
        self.assertEqual([1, 2, -3], point)

    def test_aiming_candidates_reproject_to_crosshair_in_each_view(self):
        entries = json.loads(ads.DEFAULT_ANCHORS.read_text())["guns"]
        for frame in ads.CENTRES:
            for entry in entries:
                with self.subTest(frame=frame, definition=entry["definition"]):
                    result = ads.calculate(entry, frame, solve_z=True)
                    self.assertAlmostEqual(0, result["camera_after"][0], places=12)
                    self.assertAlmostEqual(0, result["camera_after"][1], places=12)
                    self.assertLess(result["camera_after"][2], 0)

    def test_default_candidates_preserve_zero_z_and_center_vertically(self):
        entries = json.loads(ads.DEFAULT_ANCHORS.read_text())["guns"]
        for entry in entries:
            result = ads.calculate(entry)
            self.assertEqual(0, result["animAimOffset"][2])
            self.assertAlmostEqual(0, result["camera_after"][1], places=12)

    def test_y_only_solution_preserves_nonzero_x_and_z_and_ignores_previous_y(self):
        matrix = ads.ads_matrix("1.7.10", "1.7.10")
        first, _, camera = ads.solve(matrix, (30, 8, 0), 0.6, solve_z=False, existing=(2, 99, -0.3))
        second, _, _ = ads.solve(matrix, (30, 8, 0), 0.6, solve_z=False, existing=(2, -99, -0.3))
        self.assertEqual(first, second)
        self.assertEqual(2, first[0])
        self.assertEqual(-0.3, first[2])
        self.assertAlmostEqual(0, camera[1], places=12)

    def test_every_official_gun_definition_is_covered_or_explicitly_excluded(self):
        manifest = json.loads(ads.DEFAULT_ANCHORS.read_text())
        covered = [entry["definition"] for entry in manifest["guns"] + manifest["excluded"]]
        definitions = ads.ROOT.glob("src/officialpacks/resources/flans_content/*/definitions/guns/**/*.txt")
        expected = {path.relative_to(ads.ROOT).as_posix() for path in definitions if "model" in ads.directives(path.read_text())}
        self.assertEqual(expected, set(covered))
        self.assertEqual(len(covered), len(set(covered)))


if __name__ == "__main__":
    unittest.main()
