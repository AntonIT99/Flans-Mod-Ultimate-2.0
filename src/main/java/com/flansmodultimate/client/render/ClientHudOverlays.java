package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHudOverlays
{
    private static final ResourceLocation GUI_ICONS_LOCATION = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");
    private static final int ARMOR_EMPTY_U = 16;
    private static final int ARMOR_HALF_U = 25;
    private static final int ARMOR_FULL_U = 34;
    private static final int ARMOR_V = 9;
    private static final int ARMOR_ICON_SIZE = 9;
    private static final float ABSORPTION_ICON_RED = 0.25F;
    private static final float ABSORPTION_ICON_GREEN = 0.95F;
    private static final float ABSORPTION_ICON_BLUE = 0.9F;
    private static final float ABSORPTION_EMPTY_RED = 0.06F;
    private static final float ABSORPTION_EMPTY_GREEN = 0.22F;
    private static final float ABSORPTION_EMPTY_BLUE = 0.22F;
    private static final int TEXTURE_WIDTH = 120;
    private static final int TEXTURE_HEIGHT = 64;
    private static final double BAR_MAX_WIDTH = 14.5;
    private static final int BAR_HEIGHT = 3;
    private static final int LEGACY_HUD_LEFT = 2;
    private static final int LEGACY_HUD_RIGHT = 172;
    private static final int LEGACY_HUD_TOP = 2;
    private static final int LEGACY_HUD_LINE_HEIGHT = 10;
    private static final int HUD_WHITE = 0xFFFFFF;
    private static final int HUD_GREEN = 0x00FF00;
    private static final int HUD_AMMO_GREEN = 0x24FF62;
    private static final int HUD_GOLD = 0xDAA520;
    private static final int HUD_RED = 0xFF0000;
    private static final double[] BAR_X_OFFSETS = {
        2.0, 19.0, 36.0, 53.0, 70.0, 87.0, 104.0
    };

    public static final IGuiOverlay SCOPE = (gui, g, partialTick, sw, sh) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        ResourceLocation scopeTexture = null;

        boolean hasScope = ModClient.getCurrentScope() != null && ModClient.getCurrentScope().hasZoomOverlay();
        boolean noScreen = Minecraft.getInstance().screen == null;
        boolean zoomedIn = ModClient.getZoomProgress() > 0.8F;

        if (hasScope && noScreen && zoomedIn)
            scopeTexture = ModClient.getCurrentScope().getZoomOverlay();

        if (scopeTexture != null)
            renderScopeOverlay(g, scopeTexture, sw, sh);
    };

    public static final IGuiOverlay ARMOR = (gui, g, partialTick, sw, sh) -> {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableCull();

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (!slot.isArmor())
                continue;
            if (player.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem)
            {
                armorItem.getConfigType().getOverlay().ifPresent(overlayTexture ->
                    g.blit(overlayTexture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh));
            }
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    };

    public static final IGuiOverlay HUD = (gui, g, partialTick, sw, sh) -> {
        renderAAGunHud(g, partialTick, sw);
        renderPlayerAmmo(g, sw, sh);
        renderDigitalAmmo(g, sw, sh);
        renderTeamInfo(g, sw, sh);
        renderKillMessages(g, sw, sh);
        renderVehicleDebug(g, sw, sh);
    };

    public static void renderAAGunHud(GuiGraphics g, float partialTick, int sw)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !(player.getVehicle() instanceof AAGun aaGun))
            return;

        AAGunType type = aaGun.getConfigType();
        if (type == null)
            return;

        Font font = mc.font;
        int healthPercent = type.getHealth() <= 0
            ? 0
            : Mth.clamp(Math.round(aaGun.getHealth() * 100F / type.getHealth()), 0, 100);
        Component health = Component.translatable("hud.flansmodultimate.aa_gun.health", healthPercent);
        g.drawString(font, health, LEGACY_HUD_LEFT, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, healthColor(healthPercent), false);

        float yaw = Mth.rotLerp(partialTick, aaGun.getPrevGunYaw(), aaGun.getGunYaw());
        float pitch = Mth.lerp(partialTick, aaGun.getPrevGunPitch(), aaGun.getGunPitch());
        Component yawText = Component.translatable("hud.flansmodultimate.aa_gun.yaw", Math.round(yaw));
        Component pitchText = Component.translatable("hud.flansmodultimate.aa_gun.gun_pitch", Math.round(-pitch));

        Component currentAmmoName = aaGun.getCurrentAmmoName();
        boolean hasCurrentAmmo = !currentAmmoName.getString().isEmpty();
        Component reloadText = aaGun.getReloadTimer() > 0
            ? Component.translatable("hud.flansmodultimate.aa_gun.reload_time", String.format(Locale.ROOT, "%.1f", aaGun.getReloadTimer() / 20F))
            : Component.translatable("hud.flansmodultimate.aa_gun.ready");
        Component ammoHeading = Component.translatable("hud.flansmodultimate.aa_gun.current_ammo");

        int rightX = Math.min(LEGACY_HUD_RIGHT, sw - 2 - maxWidth(font, yawText, pitchText, reloadText, ammoHeading,
            currentAmmoName));
        rightX = Math.max(LEGACY_HUD_LEFT, rightX);

        g.drawString(font, yawText, rightX, LEGACY_HUD_TOP, HUD_WHITE, false);
        g.drawString(font, pitchText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, HUD_WHITE, false);
        g.drawString(font, reloadText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 2,
            aaGun.getReloadTimer() > 0 ? HUD_RED : HUD_GREEN, false);

        if (hasCurrentAmmo)
        {
            g.drawString(font, ammoHeading, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 3, HUD_WHITE, false);
            g.drawString(font, currentAmmoName, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 4, HUD_AMMO_GREEN, false);
        }
    }

    private static int healthColor(int healthPercent)
    {
        if (healthPercent >= 75)
            return HUD_WHITE;
        if (healthPercent >= 50)
            return HUD_GREEN;
        if (healthPercent >= 25)
            return HUD_GOLD;
        return HUD_RED;
    }

    private static int maxWidth(Font font, Component... lines)
    {
        int width = 0;
        for (Component line : lines)
            width = Math.max(width, font.width(line));
        return width;
    }

    public static final IGuiOverlay DAMAGE_ABSORPTION = (gui, g, partialTick, sw, sh) -> {
        if (!ModClientConfig.get().showArmorDamageAbsorptionBar || gui.getMinecraft().options.hideGui || !gui.shouldDrawSurvivalElements())
            return;

        LocalPlayer player = gui.getMinecraft().player;
        boolean vanillaArmorVisible = player != null && player.getArmorValue() > 0;
        int top = sh - gui.leftHeight + (vanillaArmorVisible ? 0 : 10);
        if (renderDamageAbsorptionArmorBar(player, g, sw / 2 - 91, top) && vanillaArmorVisible)
            gui.leftHeight += 10;
    };

    //TODO: FMU Style hit marker
    /** Draw the hit marker at screen center with fade-out alpha. */
    public static void renderHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        if (ModClient.getHitMarkerTime() <= 0)
            return;

        float a = Math.max((ModClient.getHitMarkerTime() - 10.0f + partialTick) / 10.0f, 0.0f);

        int w = 9;
        int h = 9;
        int x = sw / 2 - 5;
        int y = sh / 2 - 5;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, a);
        g.blit(FlansMod.hitmarkerTexture, x, y, 0, 0, w, h, 16, 16);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    }

    /** Fullscreen scope/helmet overlay; mirrors your old quad (centered square using screen height). */
    public static void renderScopeOverlay(GuiGraphics g, ResourceLocation texture, int sw, int sh)
    {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(texture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh);

        //TODO: compare with renderArmorOverlay()
    }

    private static boolean renderDamageAbsorptionArmorBar(LocalPlayer player, GuiGraphics g, int left, int top)
    {
        if (player == null)
            return false;

        double damageAbsorption = 0.0;
        double bulletAbsorption = 0.0;

        for (ItemStack stack : player.getArmorSlots())
        {
            if (!(stack.getItem() instanceof CustomArmorItem armorItem))
                continue;

            ArmorType armorType = armorItem.getConfigType();
            damageAbsorption += armorType.getDefence();
            bulletAbsorption += armorType.getBulletDefence();
        }

        int absorptionPoints = toAbsorptionArmorPoints(Math.max(damageAbsorption, bulletAbsorption));
        if (absorptionPoints <= 0)
            return false;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (int i = 0; i < 10; i++)
        {
            int x = left + i * 8;
            drawTintedArmorIcon(g, x, top, ARMOR_EMPTY_U, ABSORPTION_EMPTY_RED, ABSORPTION_EMPTY_GREEN, ABSORPTION_EMPTY_BLUE, 0.65F);

            int point = i * 2 + 1;
            if (point < absorptionPoints)
                drawTintedArmorIcon(g, x, top, ARMOR_FULL_U, ABSORPTION_ICON_RED, ABSORPTION_ICON_GREEN, ABSORPTION_ICON_BLUE, 1.0F);
            else if (point == absorptionPoints)
                drawTintedArmorIcon(g, x, top, ARMOR_HALF_U, ABSORPTION_ICON_RED, ABSORPTION_ICON_GREEN, ABSORPTION_ICON_BLUE, 1.0F);
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        return true;
    }

    private static int toAbsorptionArmorPoints(double absorption)
    {
        return Mth.clamp((int) Math.round(absorption * 20.0), 0, 20);
    }

    private static void drawTintedArmorIcon(GuiGraphics g, int x, int y, int u, float red, float green, float blue, float alpha)
    {
        RenderSystem.setShaderColor(red, green, blue, alpha);
        g.blit(GUI_ICONS_LOCATION, x, y, u, ARMOR_V, ARMOR_ICON_SIZE, ARMOR_ICON_SIZE);
    }

    public static void renderPlayerAmmo(GuiGraphics g, int sw, int sh)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        Font font = mc.font;

        for (InteractionHand hand : InteractionHand.values())
        {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.isEmpty() || !(stack.getItem() instanceof GunItem gunItem))
                continue;

            GunType gunType = gunItem.getConfigType();

            EnumFireMode currentMode = gunType.getFireMode(stack);
            String modeText = currentMode.getDisplayName();
            int modeX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 : sw / 2 - 132 - font.width(modeText);
            int modeY = sh - 31;
            int modeColor = gunType.canSwitchFireMode(stack) ? 0xAAAAFF : 0x888888;
            g.drawString(font, modeText, modeX + 1, modeY + 1, 0x000000, false);
            g.drawString(font, modeText, modeX,     modeY,     modeColor, false);

            int xAccum = 0;

            List<ItemStack> bulletStacks = gunItem.getBulletItemStackList(stack);

            java.util.Map<ShootableItem, Integer> simpleAmmoTotals = new java.util.LinkedHashMap<>();
            java.util.Map<ShootableItem, ItemStack> simpleAmmoSamples = new java.util.LinkedHashMap<>();
            java.util.List<ItemStack> magazineAmmo = new java.util.ArrayList<>();

            for (ItemStack bulletStack : bulletStacks)
            {
                if (bulletStack == null || bulletStack.isEmpty())
                    continue;

                if (!(bulletStack.getItem() instanceof ShootableItem shootableItem))
                    continue;

                int roundsPerItem = shootableItem.getConfigType().getRoundsPerItem();
                if (roundsPerItem <= 1)
                {
                    simpleAmmoTotals.merge(shootableItem, bulletStack.getCount(), Integer::sum);
                    simpleAmmoSamples.putIfAbsent(shootableItem, bulletStack);
                }
                else
                {
                    magazineAmmo.add(bulletStack);
                }
            }

            for (java.util.Map.Entry<ShootableItem, Integer> entry : simpleAmmoTotals.entrySet())
            {
                ShootableItem shootableItem = entry.getKey();
                int totalCount = entry.getValue();
                ItemStack sampleStack = simpleAmmoSamples.get(shootableItem);

                int iconX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 + xAccum : sw / 2 - 176 - xAccum;
                int iconY = sh - 19;

                g.renderItem(sampleStack, iconX, iconY);

                String s = totalCount > 1 ? String.valueOf(totalCount) : "";

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                if (!s.isEmpty())
                {
                    g.drawString(font, s, textX + 1, textY + 1, 0x000000, false);
                    g.drawString(font, s, textX,     textY,     0xFFFFFF, false);
                }

                xAccum += 16 + font.width(s);
            }

            for (ItemStack bulletStack : magazineAmmo)
            {
                int max = ((ShootableItem) bulletStack.getItem()).getConfigType().getRoundsPerItem();
                int remaining = ShootableItem.getRoundsRemaining(bulletStack);

                if (remaining <= 0)
                    continue;

                int iconX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 96 + xAccum : sw / 2 - 176 - xAccum;
                int iconY = sh - 19;

                g.renderItem(bulletStack, iconX, iconY);
                g.renderItemDecorations(font, bulletStack, iconX, iconY);

                int stackCount = bulletStack.getCount();
                String s;
                if (stackCount > 1)
                    s = remaining + "/" + max + " x" + stackCount;
                else
                    s = remaining + "/" + max;

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                g.drawString(font, s, textX + 1, textY + 1, 0x000000, false);
                g.drawString(font, s, textX,     textY,     0xFFFFFF, false);

                xAccum += 16 + font.width(s);
            }
        }
    }

    public static void renderDigitalAmmo(GuiGraphics g, int sw, int sh)
    {
        if (!ModCommonConfig.get().enableDigitalAmmoSystem())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        int numTypes = Math.min(LocalBulletManager.getNumTypes(), BAR_X_OFFSETS.length);

        RenderSystem.setShaderTexture(0, FlansMod.ammoGuiTexture);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int bgX = 10;
        int bgY = sh - 50;

        g.blit(FlansMod.ammoGuiTexture, bgX, bgY, 0, 30, 120, 12, TEXTURE_WIDTH, TEXTURE_HEIGHT);

        for (int i = 0; i < numTypes; i++)
        {
            double amount = LocalBulletManager.getBullets(i + 1);
            double maxAmount = Math.max(1.0, ModCommonConfig.get().digitalAmmoMaxAmount());
            double percentage = Math.min(amount / maxAmount, 1.0);
            int barWidth = (int) (BAR_MAX_WIDTH * percentage);

            if (barWidth > 0)
            {
                int barX = (int) Math.round(bgX + BAR_X_OFFSETS[i]);
                int barY = bgY + 12;

                RenderSystem.setShaderTexture(0, FlansMod.ammoGuiTexture);
                g.blit(FlansMod.ammoGuiTexture, barX, barY, 2, 18, barWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }

        RenderSystem.disableBlend();
    }

    //TODO: implement these methods
    public static void renderTeamInfo(GuiGraphics g, int sw, int sh) {}
    public static void renderKillMessages(GuiGraphics g, int sw, int sh) {}

    public static void renderVehicleDebug(GuiGraphics g, int sw, int sh)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui)
            return;

        Driveable driveable = KeyInputHandler.resolveDriveable(player);
        if (driveable == null || driveable.getConfigType() == null)
            return;

        Font font = mc.font;
        int y = LEGACY_HUD_TOP;
        g.drawString(font, Component.literal(driveable.getConfigType().getName()), LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;

        DriveableData data = driveable.getDriveableData();
        if (data != null)
        {
            DriveablePart core = data.getPart(EnumDriveablePart.CORE);
            if (core != null && core.getMaxHealth() > 0F)
            {
                int healthPercent = Mth.clamp(Math.round(core.getHealth() * 100F / core.getMaxHealth()), 0, 100);
                g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.health", healthPercent),
                    LEGACY_HUD_LEFT, y, healthColor(healthPercent), false);
                y += LEGACY_HUD_LINE_HEIGHT;
            }

            float tankSize = driveable.getConfigType().getFuelTankSize();
            if (tankSize > 0F)
            {
                int fuelPercent = Mth.clamp(Math.round(driveable.getFuel() * 100F / tankSize), 0, 100);
                g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.fuel", fuelPercent),
                    LEGACY_HUD_LEFT, y, healthColor(fuelPercent), false);
                y += LEGACY_HUD_LINE_HEIGHT;
            }
        }

        int throttlePercent = Math.round(driveable.getThrottle() * 100F);
        double speed = driveable.getDeltaMovement().length() * 20D;
        g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.throttle", throttlePercent),
            LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;
        g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.speed",
            String.format(Locale.ROOT, "%.1f", speed)), LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;

        Component gear = Component.translatable(driveable.isGearDeployed()
            ? "hud.flansmodultimate.driveable.gear.down" : "hud.flansmodultimate.driveable.gear.up");
        Component door = Component.translatable(driveable.isDoorOpen()
            ? "hud.flansmodultimate.driveable.door.open" : "hud.flansmodultimate.driveable.door.closed");
        Component status = Component.translatable("hud.flansmodultimate.driveable.status", gear, door,
            driveable.getDriveableMode());
        g.drawString(font, status, LEGACY_HUD_LEFT, y, driveable.isVarFlare() ? HUD_GOLD : HUD_WHITE, false);

        if (!ModClient.isDebug())
            return;

        Component id = Component.literal("Driveable #" + driveable.getId() + " / " + driveable.getShortName());
        Component position = Component.literal(String.format(Locale.ROOT, "XYZ %.2f / %.2f / %.2f",
            driveable.getX(), driveable.getY(), driveable.getZ()));
        Component rotation = Component.literal(String.format(Locale.ROOT, "YPR %.1f / %.1f / %.1f",
            driveable.getYaw(), driveable.getPitch(), driveable.getRoll()));
        Component turret = Component.literal(String.format(Locale.ROOT, "Turret %.1f / %.1f",
            driveable.getTurretYaw(), driveable.getTurretPitch()));
        Component input = Component.literal(String.format(Locale.ROOT, "Input 0x%05X", driveable.getInputMask()));
        int rightX = Math.max(LEGACY_HUD_LEFT, sw - 2 - maxWidth(font, id, position, rotation, turret, input));
        int debugY = LEGACY_HUD_TOP;
        for (Component line : List.of(id, position, rotation, turret, input))
        {
            g.drawString(font, line, rightX, debugY, HUD_GREEN, false);
            debugY += LEGACY_HUD_LINE_HEIGHT;
        }
    }
}
