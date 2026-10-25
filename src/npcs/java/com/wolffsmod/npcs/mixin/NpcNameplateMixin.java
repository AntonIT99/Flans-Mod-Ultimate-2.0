package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.client.teams.FlansTeamsClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.wolffsmod.npcs.client.ClientServerSettings;
import com.wolffsmod.npcs.client.FlanModelBounds;
import com.wolffsmod.npcs.client.FlanNameplatePlacement;
import com.wolffsmod.npcs.diag.NpcServerSettings.Setting;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.properties.NpcPresentation;
import com.wolffsmod.npcs.teams.BotSettings;
import com.wolffsmod.npcs.teams.BotTag;
import noppes.npcs.client.renderer.RenderNPCInterface;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;

/**
 * Sets the name tag range from the server settings, raises Flan labels above their model, preserves tint with hurt flashes disabled, hides killed bodies without death rotation
 * and hides the name tags of Teams bots from players of the other sides when only allies should see them.
 */
@Mixin(value = RenderNPCInterface.class, remap = false)
@SuppressWarnings("DataFlowIssue") // Mixin merges this class into the target, so the self-cast is valid.
public abstract class NpcNameplateMixin
{
    @Inject(method = "renderColor", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsUnhurtTint(EntityNPCInterface npc, CallbackInfo callback)
    {
        if (!NpcPresentation.of(npc).isHurtFlash())
        {
            int tint = npc.display.getTint();
            RenderSystem.setShaderColor((tint >> 16 & 255) / 255F, (tint >> 8 & 255) / 255F, (tint & 255) / 255F, 1F);
            callback.cancel();
        }
    }

    @Inject(method = "render(Lnoppes/npcs/entity/EntityNPCInterface;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsHideKilled(EntityNPCInterface npc, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        if (!NpcPresentation.of(npc).isDeathRotation() && npc.stats.hideKilledBody && npc.isKilled())
            callback.cancel();
    }

    /** A Teams bot whose name tags only allies see shows none to a player of another side. */
    @Inject(method = "renderLivingLabel", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsAlliesOnlyLabel(EntityNPCInterface npc, PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo callback)
    {
        BotTag.readClient(npc).filter(bot -> bot.nameplates() == BotSettings.Nameplates.ALLIES).ifPresent(bot ->
        {
            boolean ally = FlansModApi.getTeam(bot.team()).map(FlansTeamsClient::isAllyOfLocalPlayer).orElse(false);
            if (FlansTeamsClient.isLocalPlayerPlaying() && !ally)
                callback.cancel();
        });
    }

    /** Custom NPCs hides name tags beyond a fixed squared distance of 512; the server settings of Wolff's Mod set the range instead. */
    @ModifyConstant(method = {"renderNameTag", "m_7649_"}, constant = @Constant(doubleValue = 512.0))
    private double wolffsmodnpcsNameTagRange(double original)
    {
        double range = ClientServerSettings.get(Setting.NAME_TAG_RANGE);
        return range * range;
    }

    @SuppressWarnings("unchecked")
    @ModifyVariable(method = "renderLivingLabel", at = @At("STORE"), name = "height")
    private float wolffsmodnpcsNameHeight(float original, EntityNPCInterface npc, PoseStack pose, MultiBufferSource buffers, int light)
    {
        if (!(npc instanceof EntityCustomNpc custom) || !(custom.modelData.getEntity(custom) instanceof FlanModelEntity model))
            return original;
        float scale = custom.display.getSize() / 5F;
        var renderer = (EntityRenderer<EntityNPCInterface>) (Object) this;
        double offset = renderer.getRenderOffset(npc, 0).y;
        return FlanNameplatePlacement.height(original, npc.getBbHeight(), FlanModelBounds.of(model), scale, offset);
    }
}
