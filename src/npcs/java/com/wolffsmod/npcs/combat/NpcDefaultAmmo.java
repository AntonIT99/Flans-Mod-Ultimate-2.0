package com.wolffsmod.npcs.combat;

import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.driveables.IAAGunType;
import com.flansmodultimate.api.driveables.IDriveableType;
import com.flansmodultimate.api.driveables.WeaponMuzzle;
import com.flansmodultimate.api.equipment.FlansEquipment;
import com.flansmodultimate.api.projectiles.FlansProjectiles;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.sim.NpcModels;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import org.jetbrains.annotations.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The rounds an NPC falls back on while it carries none its weapon takes: every round the weapon, or the selected bank of
 * the model it wears, is issued with by default, rather than only the first of them. A tank that carries nothing can
 * therefore still choose between its armour-piercing and its high-explosive shell, as {@link NpcWeaponChoice} scores them
 * against the target.
 *
 * <p>
 * The fallback is a convenience of infinite ammunition: an NPC that uses its ammunition up has to carry what it fires, and
 * an owner who wants an NPC held to exactly the rounds it was given turns {@link Feature#DEFAULT_AMMO} off. Either side
 * after content loading, so that the editor can list what an NPC would fall back on.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NpcDefaultAmmo
{
    private static final String KEY = "wolffsmodnpcs.ammunition.";

    /** @return whether the NPC may fall back on default rounds at all: its ammunition is infinite and the fallback is on */
    public static boolean available(EntityNPCInterface npc)
    {
        return !NpcEquipment.enabled(npc, Feature.CONSUME_AMMO) && NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO);
    }

    /** @return the rounds a gun definition, such as one a model mounts, falls back on; empty when the NPC may not fall back */
    public static List<ItemStack> forGun(EntityNPCInterface npc, IContentType gun)
    {
        return available(npc) ? FlansEquipment.createDefaultAmmunitionChoices(gun) : List.of();
    }

    /** @return the rounds a held gun falls back on, those it would also accept from an inventory */
    public static List<ItemStack> forWeapon(EntityNPCInterface npc, ItemStack weapon)
    {
        if (!available(npc))
            return List.of();
        return FlansEquipment.createDefaultAmmunitionChoices(weapon).stream().filter(ammunition -> FlansEquipment.acceptsAmmunition(weapon, ammunition)).toList();
    }

    /** @return the rounds the selected bank of an AA gun or driveable model falls back on */
    public static List<ItemStack> forBank(EntityNPCInterface npc, IContentType model, boolean secondary)
    {
        return available(npc) ? FlansEquipment.createDefaultAmmunitionChoices(model, secondary) : List.of();
    }

    /**
     * What the editor lists under the fallback switch: a line per weapon of the NPC naming the rounds it falls back on, so
     * that an owner can see what an NPC with an empty inventory fires before deciding to hold it to its own supplies. The
     * rounds are read from the content definitions alone, so the list is the same on either side.
     *
     * @return a line per weapon, or a single line saying why there is nothing to list
     */
    public static List<Component> lines(EntityNPCInterface npc)
    {
        if (NpcEquipment.enabled(npc, Feature.CONSUME_AMMO))
            return List.of(Component.translatable(KEY + "consumed"));
        if (!NpcEquipment.enabled(npc, Feature.DEFAULT_AMMO))
            return List.of(Component.translatable(KEY + "fallback_off"));
        Map<Component, List<ItemStack>> weapons = new LinkedHashMap<>();
        IContentType model = modelWeapon(npc);
        if (model != null)
            modelWeapons(npc, model, weapons);
        else
            weapons.put(Component.translatable(KEY + "held_gun"), forWeapon(npc, npc.getMainHandItem()));
        List<Component> lines = new ArrayList<>();
        weapons.forEach((weapon, rounds) ->
        {
            if (!rounds.isEmpty())
                lines.add(Component.translatable(KEY + "line", weapon, names(rounds)));
        });
        return lines.isEmpty() ? List.of(Component.translatable(KEY + "none")) : List.copyOf(lines);
    }

    /** The banks of a model, then each gun it mounts, named as the rows above them are. */
    private static void modelWeapons(EntityNPCInterface npc, IContentType model, Map<Component, List<ItemStack>> weapons)
    {
        if (model instanceof IAAGunType)
        {
            weapons.put(Component.translatable(KEY + "aa_gun"), forBank(npc, model, false));
            return;
        }
        for (boolean secondary : new boolean[]{false, true})
        {
            List<WeaponMuzzle> muzzles = FlansProjectiles.getMuzzles(model, secondary, 0, false);
            if (muzzles.isEmpty())
                continue;
            weapons.put(Component.translatable(KEY + (secondary ? "secondary_bank" : "primary_bank")), forBank(npc, model, secondary));
            for (WeaponMuzzle muzzle : muzzles)
                muzzle.weapon().ifPresent(gun -> weapons.put(gun.getNameComponent(), forGun(npc, gun)));
        }
    }

    /** @return the AA gun or driveable whose model the NPC wears, or null */
    @Nullable
    private static IContentType modelWeapon(EntityNPCInterface npc)
    {
        FlanModelEntity model = NpcModels.flan(npc);
        IContentType definition = model == null ? null : model.getInfoType();
        return definition instanceof IDriveableType || definition instanceof IAAGunType ? definition : null;
    }

    /** @return the rounds' names, separated by commas, as one component the client translates */
    private static Component names(List<ItemStack> rounds)
    {
        MutableComponent names = Component.empty();
        for (int round = 0; round < rounds.size(); round++)
        {
            if (round > 0)
                names.append(", ");
            names.append(rounds.get(round).getHoverName());
        }
        return names;
    }
}
