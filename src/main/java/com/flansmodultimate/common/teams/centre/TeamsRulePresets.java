package com.flansmodultimate.common.teams.centre;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.commandcentre.ISettingsPresets;
import com.flansmodultimate.common.teams.TeamsManager;

import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.*;

/** Global Teams configuration only. Built-in presets intentionally change the same subset as the legacy commands. */
final class TeamsRulePresets implements ISettingsPresets
{
    private static final String ARENA = "arena";
    private static final String WEAPON_DROPS = "weaponDrops";
    private static final String SMART = "smart";
    private static final String SEA_LEVEL = "seaLevel";
    private static final Map<String, BiConsumer<TeamsManager, Boolean>> SWITCHES = Map.ofEntries(Map.entry("restoreBlocks", TeamsManager::setRestoreBlocks),
        Map.entry("bodyPartDamageModifiers", TeamsManager::setBodyPartDamageModifiers), Map.entry("explosions", TeamsManager::setExplosionsBreakBlocks),
        Map.entry("parachutesCanBeHit", (manager, enabled) -> com.flansmodultimate.config.ModCommonConfig.setParachutesCanBeHit(enabled)),
        Map.entry("canBreakGlass", TeamsManager::setCanBreakGlass), Map.entry("canBreakGuns", TeamsManager::setCanBreakGuns), Map.entry("canBreakAAGuns", TeamsManager::setCanBreakAAGuns),
        Map.entry("vehiclesBreakBlocks", TeamsManager::setDriveablesBreakBlocks), Map.entry("survivalCanBreakVehicles", TeamsManager::setSurvivalCanBreakVehicles),
        Map.entry("survivalCanPlaceVehicles", TeamsManager::setSurvivalCanPlaceVehicles), Map.entry("forceAdventure", TeamsManager::setForceAdventureMode),
        Map.entry("pacifism", TeamsManager::setPacifism), Map.entry("bombs", TeamsManager::setBombsEnabled), Map.entry("shells", TeamsManager::setShellsEnabled),
        Map.entry("bullets", TeamsManager::setBulletsEnabled), Map.entry("armourDrops", TeamsManager::setArmourDrops), Map.entry("overrideHunger", TeamsManager::setOverrideHunger),
        Map.entry("regeneration", TeamsManager::setRegeneration), Map.entry("alliedSpawns", TeamsManager::setAlliedSpawnSharing), Map.entry("fuelNeeded", TeamsManager::setVehiclesNeedFuel),
        Map.entry("vehiclesCanZoom", TeamsManager::setVehiclesCanZoom), Map.entry("movieProp", TeamsManager::setMovieProp), Map.entry("bloodEffects", TeamsManager::setBloodEffects),
        Map.entry("corpses", TeamsManager::setCorpses));
    private static final Map<String, ObjIntConsumer<TeamsManager>> NUMBERS = Map.ofEntries(Map.entry("playerHealth", TeamsManager::setPlayerHealth),
        Map.entry("botHealth", TeamsManager::setBotHealth), Map.entry("spawnProtection", TeamsManager::setSpawnProtectionSeconds), Map.entry("mgLife", TeamsManager::setMgLife),
        Map.entry("planeLife", TeamsManager::setPlaneLife), Map.entry("vehicleLife", TeamsManager::setVehicleLife), Map.entry("mechaLife", TeamsManager::setMechaLife),
        Map.entry("aaLife", TeamsManager::setAaLife), Map.entry("playerRegenSeconds", TeamsManager::setPlayerRegenSeconds), Map.entry("botRegenSeconds", TeamsManager::setBotRegenSeconds),
        Map.entry("corpseLife", TeamsManager::setCorpseLife), Map.entry("bloodLife", TeamsManager::setBloodLife));
    private final Supplier<TeamsManager> managerSource;

    TeamsRulePresets()
    {
        this(() -> FlansMod.teamsManager);
    }

    TeamsRulePresets(Supplier<TeamsManager> managerSource)
    {
        this.managerSource = managerSource;
    }

    private static Path file(String name)
    {
        PresetFiles.checkName(name);
        return SettingsPresets.folder(SettingsPresets.TEAMS).resolve(".library").resolve(name + ".json");
    }

    @Override
    public Collection<String> names()
    {
        Set<String> names = new TreeSet<>(List.of(ARENA, "survival"));
        Path directory = file("unused").getParent();
        if (Files.isDirectory(directory))
        {
            try (var files = Files.list(directory))
            {
                files.map(path -> path.getFileName().toString()).filter(name -> name.matches("[a-z0-9_-]{1,32}\\.json")).map(name -> name.substring(0, name.length() - 5)).forEach(names::add);
            }
            catch (IOException e)
            {
                throw new UncheckedIOException(e);
            }
        }
        return List.copyOf(names);
    }

    @Override
    public CompoundTag read(String name)
    {
        if (builtIn(name))
            return builtInData(name.equals(ARENA));
        try
        {
            CompoundTag data = PresetFiles.decode(SettingsPresets.TEAMS, PresetFiles.read(file(name))).data();
            validate(data);
            return data;
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    private static CompoundTag builtInData(boolean arena)
    {
        CompoundTag data = new CompoundTag();
        for (String key : List.of("explosions", "vehiclesBreakBlocks", "fuelNeeded", "armourDrops", "canBreakGlass"))
            data.putBoolean(key, !arena);
        for (String key : List.of("bombs", "shells", "bullets", "canBreakGuns", "canBreakAAGuns"))
            data.putBoolean(key, true);
        data.putBoolean("forceAdventure", arena);
        data.putBoolean("overrideHunger", arena);
        if (!arena)
        {
            data.putBoolean("survivalCanBreakVehicles", true);
            data.putBoolean("survivalCanPlaceVehicles", true);
        }
        data.putString(WEAPON_DROPS, arena ? SMART : "on");
        TeamsSection.LIFETIMES.forEach(key -> data.putInt(key, arena ? 120 : 0));
        return data;
    }

    @Override
    public CompoundTag capture(String target)
    {
        CompoundTag data = TeamsSection.rules(managerSource.get());
        if (!data.contains(SEA_LEVEL))
            data.putString(SEA_LEVEL, "default");
        return data;
    }

    @Override
    public void validate(CompoundTag data)
    {
        if (data.isEmpty())
            throw new IllegalArgumentException("A rules preset cannot be empty");
        for (String key : data.getAllKeys())
        {
            if (!validRule(key, data.get(key)))
                throw new IllegalArgumentException("Unknown or invalid Teams rule: " + key);
        }
    }

    private static boolean validRule(String key, Tag value)
    {
        if (SWITCHES.containsKey(key))
            return value instanceof ByteTag number && (number.getAsByte() == 0 || number.getAsByte() == 1);
        if (NUMBERS.containsKey(key))
            return value instanceof IntTag number && number.getAsInt() >= 0 && number.getAsInt() <= maximum(key);
        if (key.equals(WEAPON_DROPS))
            return value instanceof StringTag && List.of("on", "off", SMART).contains(value.getAsString());
        if (key.equals(SEA_LEVEL))
            return value instanceof IntTag number && number.getAsInt() >= -2048 && number.getAsInt() <= 2048 || value instanceof StringTag && value.getAsString().equals("default");
        return false;
    }

    private static int maximum(String key)
    {
        if (key.endsWith("Health"))
            return TeamsManager.MAX_HEALTH_OVERRIDE;
        if (key.equals("spawnProtection"))
            return TeamsManager.MAX_SPAWN_PROTECTION_SECONDS;
        if (key.equals("bloodLife"))
            return TeamsManager.MAX_BLOOD_LIFE_SECONDS;
        return Integer.MAX_VALUE;
    }

    @Override
    public void save(String name, CompoundTag data, boolean replace) throws IOException
    {
        validate(data);
        if (builtIn(name))
            throw new IllegalArgumentException("Built-in presets cannot be overwritten");
        PresetFiles.write(file(name), PresetFiles.encode(SettingsPresets.TEAMS, name, data), replace);
    }

    @Override
    public void remove(String name) throws IOException
    {
        if (builtIn(name))
            throw new IllegalArgumentException("Built-in presets cannot be deleted");
        Files.delete(file(name));
    }

    @Override
    public boolean builtIn(String name)
    {
        return name.equals(ARENA) || name.equals("survival");
    }

    @Override
    public List<Component> preview(CompoundTag data, String target)
    {
        validate(data);
        CompoundTag current = capture(target);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.flansmodultimate.presets.rules_scope"));
        for (String key : new TreeSet<>(data.getAllKeys()))
        {
            if (!Objects.equals(current.get(key), data.get(key)))
                lines.add(Component.literal(key + ": " + current.get(key) + " → " + data.get(key)));
        }
        return lines;
    }

    @Override
    public void apply(CompoundTag data, String target)
    {
        validate(data);
        TeamsManager manager = managerSource.get();
        SWITCHES.forEach((key, setter) ->
        {
            if (data.contains(key))
                setter.accept(manager, data.getBoolean(key));
        });
        NUMBERS.forEach((key, setter) ->
        {
            if (data.contains(key))
                setter.accept(manager, data.getInt(key));
        });
        if (data.contains(WEAPON_DROPS))
            manager.setWeaponDrops(switch (data.getString(WEAPON_DROPS))
            {
                case "on" -> TeamsManager.EnumWeaponDrop.DROPS;
                case SMART -> TeamsManager.EnumWeaponDrop.SMART_DROPS;
                default -> TeamsManager.EnumWeaponDrop.NONE;
            });
        if (data.contains(SEA_LEVEL))
            manager.setSeaLevelOverride(data.contains(SEA_LEVEL, Tag.TAG_INT) ? data.getInt(SEA_LEVEL) : null);
        manager.saveSettings();
    }
}
