package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.*;
import com.flansmodultimate.api.ProjectileSources.Source;
import com.wolffsmod.npcs.client.RangedControlLocks.Page;
import com.wolffsmod.npcs.client.RangedControlLocks.Reason;
import com.wolffsmod.npcs.client.ReadOnlyTooltips.Widget;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import noppes.npcs.api.wrapper.ItemStackWrapper;
import noppes.npcs.client.gui.SubGuiNpcRangeProperties;
import noppes.npcs.client.gui.mainmenu.GuiNpcStats;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.GuiBasic;
import noppes.npcs.shared.client.gui.components.GuiLabel;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** Client-only presentation of inactive Custom NPCs defaults; it never changes saved statistics. */
public final class NpcRangedControls
{
    private static final int READ_ONLY_COLOR = 0xA0A0A0;
    private static final ProjectileSources CALLER_SOURCES = new ProjectileSources(Source.CALLER, Source.CALLER, Source.CALLER, false);
    private static final RangedControlLocks EMPTY = RangedControlLocks.create(Page.RANGED, false, CALLER_SOURCES, false);

    private NpcRangedControls()
    {}

    public static RangedControlLocks apply(GuiBasic gui)
    {
        Page page = gui instanceof SubGuiNpcRangeProperties ? Page.RANGED : Page.PROJECTILE;
        EntityNPCInterface npc = findNpc(gui);
        if (npc == null)
            return EMPTY;
        RangedControlLocks inherited = NpcTypeControls.apply(gui, npc);
        NpcWeaponOptions options = ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
        ItemStack ammunition = ItemStackWrapper.MCItem(npc.inventory.getProjectile());
        if (!NpcRangedAttack.usesFlanProjectile(options, ammunition))
            return inherited;
        List<ProjectileSources> sources = sources(npc, options, ammunition);
        ProjectileSources combined = RangedControlLocks.combine(sources);
        RangedControlLocks locks = RangedControlLocks.create(page, true, combined, options.enabled(FlansProjectiles.isGrenade(ammunition) ? Feature.THROW_SOUNDS : Feature.FIRE_SOUNDS));
        locks = RangedControlLocks.merge(locks, inherited);
        markControls(gui, locks);
        return locks;
    }

    @Nullable
    static EntityNPCInterface findNpc(GuiBasic gui)
    {
        if (gui instanceof GuiNPCInterface npcGui)
            return npcGui.npc;
        // GuiWrapper.getParent() returns the root (or itself before attachment), rather than the immediate parent.
        Screen parent = gui.getWrapper().parent;
        while (parent instanceof GuiBasic basic)
        {
            if (basic instanceof GuiNPCInterface npcGui)
                return npcGui.npc;
            parent = basic.getWrapper().parent;
        }
        return null;
    }

    private static List<ProjectileSources> sources(EntityNPCInterface npc, NpcWeaponOptions options, ItemStack ammunition)
    {
        FlanModelEntity model = npc instanceof EntityCustomNpc custom && custom.modelData.getEntity(npc) instanceof FlanModelEntity flan ? flan : null;
        IContentType definition = model == null ? null : model.getInfoType();
        boolean secondary = options.enabled(Feature.SECONDARY_BANK);
        List<WeaponMuzzle> muzzles = definition != null && options.enabled(Feature.MODEL_MUZZLES) ? FlansProjectiles.getMuzzles(definition, secondary, 0, false) : List.of();
        if (muzzles.isEmpty())
            muzzles = List.of(new WeaponMuzzle(Vec3.ZERO, Optional.empty(), List.of()));
        ItemStack held = npc.getMainHandItem();
        // Inspect every barrel so an alternating bank never locks a fallback still used by another mount.
        return muzzles.stream()
            .map(muzzle -> FlansProjectiles.getSources(ammunition, held, muzzle.weapon().orElse(null), definition, secondary, options.enabled(Feature.WEAPON_STATS)).orElse(CALLER_SOURCES)).toList();
    }

    static void markControls(GuiBasic gui, RangedControlLocks locks)
    {
        locks.textFields().forEach((id, reason) ->
        {
            var field = gui.getTextField(id);
            if (field != null)
            {
                field.setEditable(false);
                field.setTextColorUneditable(READ_ONLY_COLOR);
                field.setTooltip(ReadOnlyTooltips.create(gui, Widget.FIELD, id, reason));
            }
        });
        locks.buttons().forEach((id, reason) ->
        {
            var button = gui.getButton(id);
            if (button != null)
            {
                button.setEnabled(false);
                button.setTooltip(ReadOnlyTooltips.create(gui, Widget.BUTTON, id, reason));
            }
        });
        for (Map.Entry<Integer, Reason> entry : locks.labels().entrySet())
        {
            GuiLabel label = gui.getLabel(entry.getKey());
            if (label != null)
            {
                label.setColor(READ_ONLY_COLOR);
                label.setHeight(10);
                label.setTooltip(ReadOnlyTooltips.create(gui, Widget.LABEL, entry.getKey(), entry.getValue()));
            }
        }
        if (!(gui instanceof GuiNpcStats) && (!locks.textFields().isEmpty() || !locks.buttons().isEmpty()))
            gui.addLabel(new GuiLabel(1000, "wolffsmodnpcs.weapons.read_only.notice", gui.guiLeft + 5, gui.guiTop - 12, "wolffsmodnpcs.weapons.read_only.notice.help"));
    }
}
