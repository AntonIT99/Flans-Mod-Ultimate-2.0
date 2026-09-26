package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.types.ArmorType;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ElytraItem;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Draws the CapeTexture of a worn armour piece on any humanoid, players and mobs alike. A player's
 * own cape is hidden meanwhile, see {@code CapeLayerMixin}, and so is their choice to hide capes.
 * <p>
 * The cape is posed like the vanilla player cape. Players swing it with the cloak position vanilla
 * keeps for them; other entities have none, so it is derived from their movement instead.
 */
public class ArmorCapeLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M>
{
    /** Pieces searched for a cape, the first one found wins. */
    private static final EquipmentSlot[] CAPE_SLOTS = { EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET };
    /** How far behind its owner the vanilla cloak position settles, in ticks of movement. */
    private static final double CLOAK_LAG = 3.0;
    /** Largest walking bob of a player, which scales the swing of the cape. */
    private static final float MAX_BOB = 0.1F;

    private static final Map<ArmorType, Optional<ResourceLocation>> VALIDATED = new ConcurrentHashMap<>();

    /** The vanilla player cloak, on the 64x32 sheet capes are authored on. */
    private final ModelPart cape;

    public ArmorCapeLayer(RenderLayerParent<T, M> parent)
    {
        super(parent);
        MeshDefinition mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cape", CubeListBuilder.create().texOffs(0, 0).addBox(-5F, 0F, -1F, 10F, 16F, 1F), PartPose.ZERO);
        cape = LayerDefinition.create(mesh, 64, 32).bakeRoot().getChild("cape");
    }

    /** The cape texture the armour of this entity enforces, or null when there is none or it does not exist. */
    @Nullable
    public static ResourceLocation getCape(LivingEntity entity)
    {
        for (EquipmentSlot slot : CAPE_SLOTS)
        {
            if (entity.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem && armorItem.getEquipmentSlot() == slot)
            {
                ArmorType armorType = armorItem.getConfigType();
                if (armorType.getCapeTexture() != null)
                {
                    ResourceLocation texture = VALIDATED.computeIfAbsent(armorType, ArmorCapeLayer::validate).orElse(null);
                    if (texture != null)
                        return texture;
                }
            }
        }
        return null;
    }

    /** Forgets which cape textures exist so reloaded resource packs are inspected again. */
    public static void clearValidationCache()
    {
        VALIDATED.clear();
    }

    private static Optional<ResourceLocation> validate(ArmorType armorType)
    {
        ResourceLocation texture = armorType.getCapeTexture();
        if (texture == null)
            return Optional.empty();
        if (Minecraft.getInstance().getResourceManager().getResource(texture).isEmpty())
        {
            FlansMod.log.warn("Ignoring CapeTexture '{}' of armor {}: texture {} was not found",
                armorType.getCapeTextureName(), armorType.getShortName(), texture);
            return Optional.empty();
        }
        return Optional.of(texture);
    }

    @Override
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, @NotNull T entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch)
    {
        if (entity.isInvisible() || entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ElytraItem)
            return;

        ResourceLocation texture = getCape(entity);
        if (texture == null)
            return;

        poseStack.pushPose();
        // Babies draw their body at half size, the cape follows it.
        if (getParentModel().young)
        {
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.translate(0F, 1.5F, 0F);
        }
        poseStack.translate(0F, 0F, 0.125F);

        // Where the cloak trails behind the entity, relative to its current position.
        double lagX;
        double lagY;
        double lagZ;
        float bob;
        float walk;
        if (entity instanceof Player player)
        {
            lagX = Mth.lerp(partialTicks, player.xCloakO, player.xCloak) - Mth.lerp(partialTicks, player.xo, player.getX());
            lagY = Mth.lerp(partialTicks, player.yCloakO, player.yCloak) - Mth.lerp(partialTicks, player.yo, player.getY());
            lagZ = Mth.lerp(partialTicks, player.zCloakO, player.zCloak) - Mth.lerp(partialTicks, player.zo, player.getZ());
            bob = Mth.lerp(partialTicks, player.oBob, player.bob);
            walk = Mth.lerp(partialTicks, player.walkDistO, player.walkDist) * 6F;
        }
        else
        {
            lagX = (entity.xo - entity.getX()) * CLOAK_LAG;
            lagY = (entity.yo - entity.getY()) * CLOAK_LAG;
            lagZ = (entity.zo - entity.getZ()) * CLOAK_LAG;
            // Swing in step with the legs, which is what walkDist does for players.
            bob = entity.walkAnimation.speed(partialTicks) * MAX_BOB;
            walk = entity.walkAnimation.position(partialTicks) * 0.6662F;
        }

        float bodyYaw = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        double backX = Mth.sin(bodyYaw * Mth.DEG_TO_RAD);
        double backZ = -Mth.cos(bodyYaw * Mth.DEG_TO_RAD);
        float lift = Mth.clamp((float) lagY * 10F, -6F, 32F);
        float trail = Mth.clamp((float) (lagX * backX + lagZ * backZ) * 100F, 0F, 150F);
        float sway = Mth.clamp((float) (lagX * backZ - lagZ * backX) * 100F, -20F, 20F);
        lift += Mth.sin(walk) * 32F * bob;
        if (entity.isCrouching())
            lift += 25F;

        poseStack.mulPose(Axis.XP.rotationDegrees(6F + trail / 2F + lift));
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway / 2F));
        poseStack.mulPose(Axis.YP.rotationDegrees(180F - sway / 2F));

        // Same offsets as the player cloak: pushed out over the chest piece, pulled in when crouching.
        boolean chestWorn = !entity.getItemBySlot(EquipmentSlot.CHEST).isEmpty();
        if (entity.isCrouching())
        {
            cape.z = chestWorn ? 0.3F : 1.4F;
            cape.y = chestWorn ? 0.8F : 1.85F;
        }
        else
        {
            cape.z = chestWorn ? -1.1F : 0F;
            cape.y = chestWorn ? -0.85F : 0F;
        }
        cape.render(poseStack, buffer.getBuffer(RenderType.entitySolid(texture)), packedLight, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
