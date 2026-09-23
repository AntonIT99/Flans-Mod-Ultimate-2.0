package com.flansmodultimate.common.driveables;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/** Definition and per-sight overrides for the five legacy optics HUD elements. */
@Getter
public final class OpticsHud
{
    public static final String[] ELEMENTS = {"Range", "Speed", "Traverse", "Compass", "Elevation"};
    boolean enabled;
    boolean edit;
    boolean requireRangeKey = true;
    boolean elevationIndicator;
    boolean overridePilotDefaults;
    int inheritSeat = -2;
    float maxRange = 5000F;
    int color = 0x55FF55;
    float scale = 1F;
    String compassMarker = "";
    int markerWidth = 18, markerHeight = 24, markerX, markerY = 10;
    final Element[] elements = {
        new Element(-120, 70, 80, 24), new Element(-120, 88, 96, 32),
        new Element(0, 70, 120, 48), new Element(0, -85, 220, 36), new Element(105, 0, 36, 120)
    };
    final Map<Integer, Integer> colors = new HashMap<>();
    final Map<Integer, Float> scales = new HashMap<>();
    final Map<Integer, String> markers = new HashMap<>();

    public int color(int sight) { return colors.getOrDefault(sight, color); }
    public float scale(int sight) { return scales.getOrDefault(sight, scale); }
    public String marker(int sight) { return markers.getOrDefault(sight, compassMarker); }

    @Getter
    public static final class Element
    {
        int x, y, width, height;
        float scale = 1F;
        String texture = "";
        final Map<Integer, int[]> positions = new HashMap<>();
        final Map<Integer, Float> scales = new HashMap<>();
        final Map<Integer, String> textures = new HashMap<>();

        Element(int x, int y, int width, int height)
        {
            this.x = x; this.y = y; this.width = width; this.height = height;
        }

        public int x(int sight) { return positions.containsKey(sight) ? positions.get(sight)[0] : x; }
        public int y(int sight) { return positions.containsKey(sight) ? positions.get(sight)[1] : y; }
        public float scale(int sight) { return scales.getOrDefault(sight, scale); }
        public String texture(int sight) { return textures.getOrDefault(sight, texture); }

        /** Session-local editor overrides; the editor exports definition lines for permanent changes. */
        public void move(int sight, int dx, int dy) { positions.put(sight, new int[]{x(sight) + dx, y(sight) + dy}); }
        public void resize(int sight, float delta) { scales.put(sight, Math.max(0.1F, scale(sight) + delta)); }
    }

    void read(String key, String[] values, int offset)
    {
        overridePilotDefaults = true;
        boolean perSight = key.endsWith("sight");
        int sight = perSight ? Math.max(0, Integer.parseInt(values[offset++]) - 1) : 0;
        String value = values[offset];
        switch (key)
        {
            case "hud" -> enabled = Boolean.parseBoolean(value);
            case "hudedit" -> edit = Boolean.parseBoolean(value);
            case "hudmaxrange" -> maxRange = VehicleOptics.positive(value, 1F);
            case "hudrequirerangekey" -> requireRangeKey = Boolean.parseBoolean(value);
            case "hudinherit" -> inheritSeat = value.equalsIgnoreCase("none") ? -1 : Integer.parseInt(value);
            case "hudscale" -> scale = VehicleOptics.positive(value, 0.1F);
            case "hudscalesight" -> scales.put(sight, VehicleOptics.positive(value, 0.1F));
            case "hudcolor" -> color = readColor(values, offset);
            case "hudcolorsight" -> colors.put(sight, readColor(values, offset));
            case "elevationindicator" -> elevationIndicator = Boolean.parseBoolean(value);
            case "compassmarker" -> compassMarker = value;
            case "compassmarkersight" -> markers.put(sight, value);
            case "compassmarkersize" -> { markerWidth = Math.max(1, Integer.parseInt(value)); markerHeight = Math.max(1, Integer.parseInt(values[offset + 1])); }
            case "compassmarkeroffset" -> { markerX = Integer.parseInt(value); markerY = Integer.parseInt(values[offset + 1]); }
            default -> {
                for (int i = 0; i < ELEMENTS.length; i++)
                {
                    if (!key.startsWith(ELEMENTS[i].toLowerCase(java.util.Locale.ROOT)))
                        continue;
                    Element element = elements[i];
                    if (key.contains("pos"))
                    {
                        int x = Integer.parseInt(value), y = Integer.parseInt(values[offset + 1]);
                        if (perSight) element.positions.put(sight, new int[]{x, y});
                        else { element.x = x; element.y = y; }
                    }
                    else if (key.contains("scale"))
                    {
                        float parsed = VehicleOptics.positive(value, 0.1F);
                        if (perSight) element.scales.put(sight, parsed); else element.scale = parsed;
                    }
                    else if (key.contains("texturesize"))
                    {
                        element.width = Math.max(1, Integer.parseInt(value));
                        element.height = Math.max(1, Integer.parseInt(values[offset + 1]));
                    }
                    else if (key.contains("texture"))
                    {
                        if (perSight) element.textures.put(sight, value); else element.texture = value;
                    }
                    return;
                }
            }
        }
    }

    private static int readColor(String[] values, int offset)
    {
        if (values.length < offset + 3)
            return Integer.decode(values[offset]) & 0xFFFFFF;
        int color = 0;
        for (int i = 0; i < 3; i++)
            color = color << 8 | Math.max(0, Math.min(255, Integer.parseInt(values[offset + i])));
        return color;
    }
}
