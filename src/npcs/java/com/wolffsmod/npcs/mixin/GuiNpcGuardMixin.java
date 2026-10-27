package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelEntities;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.gui.roles.GuiNpcGuard;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Custom NPCs' Guard job page. Lists the content-pack NPC models (vehicles, planes, mechas, AA guns) as targets next to
 * the vanilla and modded mobs, and keeps targets already in Current Targets out of Available Targets.
 */
@SuppressWarnings({"FieldCanBeLocal", "UnresolvedMixinReference"})
@Mixin(value = GuiNpcGuard.class, remap = false)
public abstract class GuiNpcGuardMixin
{
    @Shadow
    private JobGuard role;
    @Shadow
    private GuiCustomScrollNop scroll1;

    @Redirect(method = {"init", "m_7856_", "buttonEvent"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/client/EntityUtil;getAllEntities(Lnet/minecraft/world/level/Level;Z)Ljava/util/HashMap;"))
    private HashMap<String, ResourceLocation> wolffsmodnpcsGuardTargets(Level level, boolean includeNpcs)
    {
        HashMap<String, ResourceLocation> entities = EntityUtil.getAllEntities(level, includeNpcs);
        for (FlanModelEntityType type : FlanModelEntities.getEntityTypes())
            entities.putIfAbsent(type.getDescriptionId(), BuiltInRegistries.ENTITY_TYPE.getKey(type));
        return entities;
    }

    @Redirect(method = {"init", "m_7856_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/shared/client/gui/components/GuiCustomScrollNop;setList(Ljava/util/List;)V"))
    private void wolffsmodnpcsHideCurrentTargets(GuiCustomScrollNop scroll, List<String> list)
    {
        if (scroll == scroll1)
        {
            list = new ArrayList<>(list);
            list.removeAll(role.targets);
        }
        scroll.setList(list);
    }
}
