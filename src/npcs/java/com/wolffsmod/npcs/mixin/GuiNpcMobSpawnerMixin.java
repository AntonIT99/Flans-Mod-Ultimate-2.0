package com.wolffsmod.npcs.mixin;

import com.wolffsmod.npcs.model.FlanModelEntityType;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.client.EntityUtil;
import noppes.npcs.client.gui.GuiNpcMobSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.HashMap;

/** Hides generated Flan NPC model types from the mob cloner's Entities tab. */
@Mixin(value = GuiNpcMobSpawner.class, remap = false)
public abstract class GuiNpcMobSpawnerMixin
{
    @Redirect(method = "showEntities", at = @At(value = "INVOKE", target = "Lnoppes/npcs/client/EntityUtil;getAllEntities(Lnet/minecraft/world/level/Level;Z)Ljava/util/HashMap;"))
    private HashMap<String, ResourceLocation> wolffsmodnpcsClonableEntities(Level level, boolean includeNpcs)
    {
        HashMap<String, ResourceLocation> entities = EntityUtil.getAllEntities(level, includeNpcs);
        entities.values().removeIf(id -> ForgeRegistries.ENTITY_TYPES.getValue(id) instanceof FlanModelEntityType);
        return entities;
    }
}
