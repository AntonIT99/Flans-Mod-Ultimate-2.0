package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.client.NpcRemoteReceiver;
import com.wolffsmod.npcs.network.NpcRemotePacket;
import noppes.npcs.client.gui.GuiNpcRemoteEditor;
import noppes.npcs.client.gui.util.GuiNPCInterface;
import noppes.npcs.packets.Packets;
import noppes.npcs.packets.server.SPacketRemoteNpcsGet;
import noppes.npcs.shared.client.gui.components.GuiButtonNop;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.*;

/** Adds radius and faction filters, refresh, and exact nearest-first ordering to the wand's air-click browser. */
// The target's superclass provides its GUI controls; shadows are populated by the target or its packet handler.
@SuppressWarnings({"java:S110", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcRemoteEditor.class, remap = false)
public abstract class GuiNpcRemoteEditorMixin extends GuiNPCInterface implements NpcRemoteReceiver
{
    @Unique
    private static final int[] WOLFFSMODNPCS_RADII = {0, 16, 32, 64, 128, 256, 512};
    @Shadow
    private GuiCustomScrollNop scroll;
    @Shadow
    private Map<String, Integer> data;
    @Unique
    private NpcRemotePacket wolffsmodnpcsSnapshot = new NpcRemotePacket(List.of());
    @Unique
    private int wolffsmodnpcsRadius;
    @Unique
    private Integer wolffsmodnpcsFaction;

    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsControls(CallbackInfo callback)
    {
        wolffsmodnpcsAddButton(100, 156, wolffsmodnpcsRadiusLabel(), I18n.get("wolffsmodnpcs.remote.radius.help"));
        wolffsmodnpcsAddButton(101, 178, wolffsmodnpcsFactionLabel(), wolffsmodnpcsFactionLabel());
        wolffsmodnpcsAddButton(102, 200, I18n.get("wolffsmodnpcs.remote.refresh"), I18n.get("wolffsmodnpcs.remote.refresh.help"));
        wolffsmodnpcsApply();
    }

    @Unique
    private void wolffsmodnpcsAddButton(int id, int y, String label, String tooltip)
    {
        GuiButtonNop button = new GuiButtonNop(this, id, guiLeft + 170, guiTop + y, 82, 20, label);
        button.setTooltip(Tooltip.create(Component.literal(tooltip)));
        addButton(button);
    }

    @Inject(method = "buttonEvent", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsFilter(GuiButtonNop button, CallbackInfo callback)
    {
        if (button.id == 100)
        {
            wolffsmodnpcsRadius = (wolffsmodnpcsRadius + 1) % WOLFFSMODNPCS_RADII.length;
            button.setDisplayText(wolffsmodnpcsRadiusLabel());
        }
        else if (button.id == 101)
        {
            List<Integer> factions = wolffsmodnpcsSnapshot.entries().stream()
                .sorted(Comparator.comparing(NpcRemotePacket.Entry::faction, String.CASE_INSENSITIVE_ORDER).thenComparingInt(NpcRemotePacket.Entry::factionId)).map(NpcRemotePacket.Entry::factionId)
                .distinct().toList();
            int next = wolffsmodnpcsFaction == null ? 0 : factions.indexOf(wolffsmodnpcsFaction) + 1;
            wolffsmodnpcsFaction = next < factions.size() ? factions.get(next) : null;
            wolffsmodnpcsUpdateFactionButton();
        }
        else if (button.id == 102)
            Packets.sendServer(new SPacketRemoteNpcsGet());
        else
            return;
        wolffsmodnpcsApply();
        callback.cancel();
    }

    @Override
    public void wolffsmodnpcsReceive(NpcRemotePacket packet)
    {
        wolffsmodnpcsSnapshot = packet;
        if (packet.entries().stream().noneMatch(entry -> Objects.equals(entry.factionId(), wolffsmodnpcsFaction)))
            wolffsmodnpcsFaction = null;
        wolffsmodnpcsUpdateFactionButton();
        wolffsmodnpcsApply();
    }

    @Unique
    private void wolffsmodnpcsUpdateFactionButton()
    {
        GuiButtonNop button = getButton(101);
        if (button != null)
        {
            button.setDisplayText(wolffsmodnpcsFactionLabel());
            button.setTooltip(Tooltip.create(Component.literal(wolffsmodnpcsFactionLabel())));
        }
    }

    @Unique
    private void wolffsmodnpcsApply()
    {
        if (scroll == null)
            return;
        Integer selected = data.get(scroll.getSelected());
        // Retain the original meaning of Reset All: all loaded NPCs in this dimension.
        data = new HashMap<>();
        for (NpcRemotePacket.Entry entry : wolffsmodnpcsSnapshot.entries())
            data.put(wolffsmodnpcsLabel(entry), entry.entityId());
        List<NpcRemotePacket.Entry> entries = wolffsmodnpcsSnapshot.filtered(WOLFFSMODNPCS_RADII[wolffsmodnpcsRadius], wolffsmodnpcsFaction);
        List<String> labels = new ArrayList<>();
        String selection = null;
        for (NpcRemotePacket.Entry entry : entries)
        {
            String label = wolffsmodnpcsLabel(entry);
            labels.add(label);
            if (selected != null && entry.entityId() == selected)
                selection = label;
        }
        // Custom NPCs' equality check ignores order. Empty the list first so reordering is always applied.
        scroll.setUnsortedList(new ArrayList<>());
        scroll.setUnsortedList(labels);
        scroll.setSelected(selection);
    }

    @Unique
    private String wolffsmodnpcsRadiusLabel()
    {
        int radius = WOLFFSMODNPCS_RADII[wolffsmodnpcsRadius];
        return I18n.get("wolffsmodnpcs.remote.radius", radius == 0 ? I18n.get("wolffsmodnpcs.remote.all") : radius);
    }

    @Unique
    private String wolffsmodnpcsFactionLabel()
    {
        String faction = wolffsmodnpcsSnapshot.entries().stream().filter(entry -> Objects.equals(entry.factionId(), wolffsmodnpcsFaction)).map(NpcRemotePacket.Entry::faction).findFirst()
            .orElse(I18n.get("wolffsmodnpcs.remote.all"));
        return I18n.get("wolffsmodnpcs.remote.faction", faction);
    }

    @Unique
    private static String wolffsmodnpcsLabel(NpcRemotePacket.Entry entry)
    {
        return String.format(Locale.ROOT, "%.1f : %s [#%d]", Math.sqrt(entry.distanceSquared()), entry.name(), entry.entityId());
    }
}
