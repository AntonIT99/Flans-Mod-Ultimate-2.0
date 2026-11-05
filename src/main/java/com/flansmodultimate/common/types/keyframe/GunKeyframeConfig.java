package com.flansmodultimate.common.types.keyframe;

import com.flansmodultimate.common.types.TypeFile;
import com.flansmodultimate.content.IContentProvider;
import com.flansmodultimate.util.FileUtils;
import com.flansmodultimate.util.FlansLog;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static com.flansmodultimate.util.TypeReaderUtils.*;

/**
 * Keyframe animation settings of a gun, the Tyrants and Plebeians "GLTF" animation system. Each
 * {@code GLTFAnimation <name> <file>} line names an animation inside {@code assets/flansmod/animations/<file>.json};
 * the last such line is the default reload animation, and the other settings pick named animations for other actions.
 */
@Getter
@NoArgsConstructor
public class GunKeyframeConfig
{
    private static final String ANIMATIONS_FOLDER = "animations";

    private boolean enabled;
    private boolean rightArmAnimated;
    private boolean leftArmAnimated;
    private boolean loopReloadAnimation;
    @Nullable
    private String reloadAnimation;
    @Nullable
    private String tacticalReloadAnimation;
    @Nullable
    private String shootAnimation;
    @Nullable
    private String aimingShootAnimation;
    @Nullable
    private String startReloadAnimation;
    @Nullable
    private String loopReloadAnimationName;
    @Nullable
    private String endReloadAnimation;
    @Nullable
    private String actionAnimation;
    @Nullable
    private String switchAnimation;
    @Nullable
    private String secondarySwitchAnimation;
    @Nullable
    private String secondaryUnSwitchAnimation;
    @Nullable
    private String secondaryReloadAnimation;
    /** Animation name to the file it is read from, in definition order */
    private final Map<String, String> animationFiles = new LinkedHashMap<>();
    /** Loaded animations by name; filled on the client only */
    private final Map<String, KeyframeAnimation> animations = new HashMap<>();

    public void read(TypeFile file)
    {
        enabled = readValue("UseGLTFAnimation", enabled, file);
        rightArmAnimated = readValue("UseRightArmGLTFAnimation", rightArmAnimated, file);
        leftArmAnimated = readValue("UseLeftArmGLTFAnimation", leftArmAnimated, file);
        loopReloadAnimation = readValue("UseLoopReloadAnimation", loopReloadAnimation, file);

        readValuesInLines("GLTFAnimation", file, 2).ifPresent(lines -> lines.forEach(split ->
        {
            animationFiles.put(split[0], split[1]);
            reloadAnimation = split[0];
        }));

        tacticalReloadAnimation = readName("TacticalReloadGLTFAnimation", tacticalReloadAnimation, file);
        shootAnimation = readName("ShootAnimation", shootAnimation, file);
        aimingShootAnimation = readName("AimingShootAnimation", aimingShootAnimation, file);
        startReloadAnimation = readName("StartReloadAnimation", startReloadAnimation, file);
        loopReloadAnimationName = readName("LoopReloadAnimation", loopReloadAnimationName, file);
        endReloadAnimation = readName("EndReloadAnimation", endReloadAnimation, file);
        actionAnimation = readName("ActionAnimation", actionAnimation, file);
        switchAnimation = readName("SwitchAnimation", switchAnimation, file);
        secondarySwitchAnimation = readName("SecondarySwitchAnimation", secondarySwitchAnimation, file);
        secondaryUnSwitchAnimation = readName("SecondaryUnSwitchAnimation", secondaryUnSwitchAnimation, file);
        secondaryReloadAnimation = readName("SecondaryReloadAnimation", secondaryReloadAnimation, file);
    }

    /** Reads every animation file this gun names from its content pack. Missing files and animations are logged and skipped. */
    public void loadAnimations(IContentProvider contentPack, TypeFile file)
    {
        animations.clear();
        if (animationFiles.isEmpty())
            return;

        FileSystem fs = FileUtils.createFileSystem(contentPack);
        try
        {
            Map<String, Map<String, KeyframeAnimation>> filesRead = new HashMap<>();
            Path folder = contentPack.getAssetsPath(fs).resolve(ANIMATIONS_FOLDER);
            for (Map.Entry<String, String> entry : animationFiles.entrySet())
            {
                Map<String, KeyframeAnimation> fileAnimations = filesRead.computeIfAbsent(entry.getValue(), name -> readFile(folder, name, file));
                KeyframeAnimation animation = fileAnimations.get(entry.getKey());
                if (animation != null)
                    animations.put(entry.getKey(), animation);
                else if (!fileAnimations.isEmpty())
                    logError("Animation '" + entry.getKey() + "' not found in " + ANIMATIONS_FOLDER + "/" + entry.getValue() + ".json", file);
            }
        }
        finally
        {
            FileUtils.closeFileSystem(fs, contentPack);
        }
    }

    @Nullable
    public KeyframeAnimation getAnimation(@Nullable String name)
    {
        return name == null ? null : animations.get(name);
    }

    public boolean hasAnimation(@Nullable String name)
    {
        return getAnimation(name) != null;
    }

    private static Map<String, KeyframeAnimation> readFile(Path folder, String name, TypeFile file)
    {
        Optional<Path> path = resolve(folder, name + ".json");
        if (path.isEmpty())
        {
            logError("Animation file " + ANIMATIONS_FOLDER + "/" + name + ".json not found", file);
            return Map.of();
        }

        try (Reader reader = Files.newBufferedReader(path.get(), StandardCharsets.UTF_8))
        {
            return KeyframeAnimationReader.read(reader);
        }
        catch (IOException | RuntimeException e)
        {
            FlansLog.log.error("Could not read animation file {} for {}", path.get(), file, e);
            return Map.of();
        }
    }

    /** Exact name first; archives are case-sensitive, so fall back to a case-insensitive match. */
    private static Optional<Path> resolve(Path folder, String fileName)
    {
        Path exact = folder.resolve(fileName);
        if (Files.isRegularFile(exact))
            return Optional.of(exact);
        if (!Files.isDirectory(folder))
            return Optional.empty();

        String wanted = fileName.toLowerCase(Locale.ROOT);
        try (Stream<Path> files = Files.list(folder))
        {
            return files.filter(Files::isRegularFile).filter(candidate -> candidate.getFileName().toString().toLowerCase(Locale.ROOT).equals(wanted)).findFirst();
        }
        catch (IOException e)
        {
            return Optional.empty();
        }
    }

    @Nullable
    private static String readName(String key, @Nullable String defaultValue, TypeFile file)
    {
        String value = readValue(key, defaultValue, file);
        return StringUtils.isBlank(value) || value.equalsIgnoreCase("null") || value.equalsIgnoreCase("none") ? null : value;
    }
}
