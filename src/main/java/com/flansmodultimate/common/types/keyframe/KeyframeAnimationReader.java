package com.flansmodultimate.common.types.keyframe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads Bedrock-style animation files ({@code format_version}, {@code animations}, {@code bones} with time-keyed
 * {@code position} and {@code rotation} vectors). Keyframes may be plain vectors or {@code pre}/{@code post}
 * objects, and a channel may be a single static vector.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyframeAnimationReader
{
    private static final String ANIMATIONS = "animations";
    private static final String ANIMATION_LENGTH = "animation_length";
    private static final String BONES = "bones";

    /** @return every animation in the file, by name */
    public static Map<String, KeyframeAnimation> read(Reader reader)
    {
        JsonElement root = JsonParser.parseReader(reader);
        if (!root.isJsonObject() || !root.getAsJsonObject().has(ANIMATIONS) || !root.getAsJsonObject().get(ANIMATIONS).isJsonObject())
            throw new JsonParseException("Missing \"animations\" object");

        Map<String, KeyframeAnimation> animations = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().getAsJsonObject(ANIMATIONS).entrySet())
        {
            if (entry.getValue().isJsonObject())
                animations.put(entry.getKey(), readAnimation(entry.getValue().getAsJsonObject()));
        }
        return animations;
    }

    private static KeyframeAnimation readAnimation(JsonObject json)
    {
        float length = json.has(ANIMATION_LENGTH) && json.get(ANIMATION_LENGTH).isJsonPrimitive() ? json.get(ANIMATION_LENGTH).getAsFloat() : 0F;
        Map<String, KeyframeAnimation.KeyframeBone> bones = new HashMap<>();
        if (json.has(BONES) && json.get(BONES).isJsonObject())
        {
            for (Map.Entry<String, JsonElement> bone : json.getAsJsonObject(BONES).entrySet())
            {
                if (!bone.getValue().isJsonObject())
                    continue;
                JsonObject channels = bone.getValue().getAsJsonObject();
                bones.put(bone.getKey(), new KeyframeAnimation.KeyframeBone(readChannel(channels.get("position")), readChannel(channels.get("rotation"))));
            }
        }
        return new KeyframeAnimation(length, bones);
    }

    private static KeyframeChannel readChannel(@Nullable JsonElement json)
    {
        if (json == null || json.isJsonNull())
            return KeyframeChannel.EMPTY;

        float[] staticValue = readVector(json);
        if (staticValue != null)
            return new KeyframeChannel(new float[] {0F}, staticValue);
        if (!json.isJsonObject())
            return KeyframeChannel.EMPTY;

        List<Map.Entry<Float, float[]>> frames = new ArrayList<>();
        for (Map.Entry<String, JsonElement> frame : json.getAsJsonObject().entrySet())
        {
            float[] value = readVector(frame.getValue());
            if (value == null)
                continue;
            try
            {
                frames.add(Map.entry(Float.parseFloat(frame.getKey().trim()), value));
            }
            catch (NumberFormatException ignored)
            {
                // Not a keyframe time
            }
        }
        frames.sort(Comparator.comparing(Map.Entry::getKey));

        float[] times = new float[frames.size()];
        float[] values = new float[frames.size() * 3];
        for (int i = 0; i < frames.size(); i++)
        {
            times[i] = frames.get(i).getKey();
            System.arraycopy(frames.get(i).getValue(), 0, values, i * 3, 3);
        }
        return new KeyframeChannel(times, values);
    }

    /** A vector as {@code [x, y, z]}, or a keyframe object holding one under {@code post}, {@code pre} or {@code vector}. */
    @Nullable
    private static float[] readVector(JsonElement json)
    {
        if (json.isJsonArray())
        {
            JsonArray array = json.getAsJsonArray();
            if (array.size() < 3)
                return null;
            float[] vector = new float[3];
            for (int i = 0; i < 3; i++)
            {
                // Molang expressions are not supported
                if (!array.get(i).isJsonPrimitive() || !array.get(i).getAsJsonPrimitive().isNumber())
                    return null;
                vector[i] = array.get(i).getAsFloat();
            }
            return vector;
        }
        if (json.isJsonObject())
        {
            JsonObject object = json.getAsJsonObject();
            for (String key : new String[] {"post", "pre", "vector"})
            {
                if (object.has(key))
                    return readVector(object.get(key));
            }
        }
        return null;
    }
}
