package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.FlansModApi;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.gui.roles.GuiNpcGuard;
import noppes.npcs.roles.JobGuard;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Custom NPCs' Guard job page. Hides content-pack NPC models, Flan's Mod driveables and other non-mob entities from the
 * target lists, and keeps targets already in Current Targets out of Available Targets.
 */
@SuppressWarnings("UnresolvedMixinReference")
@Mixin(value = GuiNpcGuard.class, remap = false)
public abstract class GuiNpcGuardMixin
{
    /** Namespaces of Flan's Mod entities; their MISC types are machines and technical entities rather than mobs. */
    @Unique
    private static final Set<String> WOLFFSMODNPCS_FLAN_NAMESPACES = FlansModApi.getModNamespaces();

    @Shadow
    private JobGuard role;
    @Shadow
    private GuiCustomScrollNop scroll1;

    @Redirect(method = {"init", "m_7856_", "buttonEvent"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/client/EntityUtil;getAllEntities(Lnet/minecraft/world/level/Level;Z)Ljava/util/HashMap;"))
    private HashMap<String, ResourceLocation> wolffsmodnpcsGuardTargets(Level level, boolean withNpcs)
    {
        HashMap<String, ResourceLocation> entities = EntityUtil.getAllEntities(level, withNpcs);
        // Driveables, guns, seats and the like are living entities only to Minecraft; guards cannot target them. Content-pack
        // NPC models are never in a level as themselves: summoned, they become Custom NPCs, which guards do not target.
        entities.values().removeIf(id ->
        {
            EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
            return type == null || type instanceof FlanModelEntityType || WOLFFSMODNPCS_FLAN_NAMESPACES.contains(id.getNamespace()) && type.getCategory() == MobCategory.MISC;
        });
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
