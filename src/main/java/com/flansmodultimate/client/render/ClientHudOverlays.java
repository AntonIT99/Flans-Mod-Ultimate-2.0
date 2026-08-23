package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.ARGB;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ClientHudOverlays
{
    private static final Identifier GUI_ICONS_LOCATION = Identifier.fromNamespaceAndPath("minecraft", "textures/gui/icons.png");
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

    public static final GuiLayer SCOPE = (g, deltaTracker) -> {
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        Identifier scopeTexture = null;

        boolean hasScope = ModClient.getCurrentScope() != null && ModClient.getCurrentScope().hasZoomOverlay();
        boolean noScreen = Minecraft.getInstance().screen == null;
        boolean zoomedIn = ModClient.getZoomProgress() > 0.8F;

        if (hasScope && noScreen && zoomedIn)
            scopeTexture = ModClient.getCurrentScope().getZoomOverlay();

        if (scopeTexture != null)
            renderScopeOverlay(g, scopeTexture, sw, sh);
    };

    public static final GuiLayer ARMOR = (g, deltaTracker) -> {
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().options.getCameraType() != CameraType.FIRST_PERSON)
            return;

        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (!slot.isArmor())
                continue;
            if (player.getItemBySlot(slot).getItem() instanceof CustomArmorItem armorItem)
            {
                armorItem.getConfigType().getOverlay().ifPresent(overlayTexture ->
                    g.blit(RenderPipelines.GUI_TEXTURED, overlayTexture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh));
            }
        }

    };

    public static final GuiLayer HUD = (g, deltaTracker) -> {
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);
        renderAAGunHud(g, partialTick, sw);
        renderPlayerAmmo(g, sw, sh);
        renderDigitalAmmo(g, sw, sh);
        renderTeamInfo(g, sw, sh);
        renderKillMessages(g, sw, sh);
        renderVehicleDebug(g, sw, sh);
    };

    public static void renderAAGunHud(GuiGraphicsExtractor g, float partialTick, int sw)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.options.hideGui || !(player.getVehicle() instanceof AAGun aaGun))
            return;

        AAGunType type = aaGun.getConfigType();
        if (type == null)
            return;

        Font font = mc.font;
        g.text(font, Component.literal(type.getName()), LEGACY_HUD_LEFT, LEGACY_HUD_TOP, HUD_WHITE, false);
        int healthPercent = type.getHealth() <= 0
            ? 0
            : Mth.clamp(Math.round(aaGun.getHealth() * 100F / type.getHealth()), 0, 100);
        Component health = Component.translatable("hud.flansmodultimate.aa_gun.health", healthPercent);
        g.text(font, health, LEGACY_HUD_LEFT, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, healthColor(healthPercent), false);

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

        int rightX = Math.max(LEGACY_HUD_LEFT, sw - 2 - maxWidth(font, yawText, pitchText, reloadText, ammoHeading,
            currentAmmoName));

        g.text(font, yawText, rightX, LEGACY_HUD_TOP, HUD_WHITE, false);
        g.text(font, pitchText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, HUD_WHITE, false);
        g.text(font, reloadText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 2,
            aaGun.getReloadTimer() > 0 ? HUD_RED : HUD_GREEN, false);

        if (hasCurrentAmmo)
        {
            g.text(font, ammoHeading, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 3, HUD_WHITE, false);
            g.text(font, currentAmmoName, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 4, HUD_AMMO_GREEN, false);
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

    private static Component compassDirection(float yaw)
    {
        float wrappedYaw = Mth.wrapDegrees(yaw);
        if (wrappedYaw >= -45F && wrappedYaw < 45F)
            return Component.translatable("hud.flansmodultimate.driveable.compass.south");
        if (wrappedYaw >= 45F && wrappedYaw < 135F)
            return Component.translatable("hud.flansmodultimate.driveable.compass.west");
        if (wrappedYaw >= -135F && wrappedYaw < -45F)
            return Component.translatable("hud.flansmodultimate.driveable.compass.east");
        return Component.translatable("hud.flansmodultimate.driveable.compass.north");
    }

    public static final GuiLayer DAMAGE_ABSORPTION = (g, deltaTracker) -> {
        Minecraft minecraft = Minecraft.getInstance();
        if (!ModClientConfig.get().showArmorDamageAbsorptionBar || minecraft.options.hideGui
            || minecraft.gameMode == null || !minecraft.gameMode.canHurtPlayer())
            return;

        int sw = g.guiWidth();
        int sh = g.guiHeight();
        LocalPlayer player = minecraft.player;
        boolean vanillaArmorVisible = player != null && player.getArmorValue() > 0;
        int top = sh - (vanillaArmorVisible ? 59 : 49);
        renderDamageAbsorptionArmorBar(player, g, sw / 2 - 91, top);
    };

    //TODO: FMU Style hit marker
    /** Draw the hit marker at screen center with fade-out alpha. */
    public static void renderHitMarker(GuiGraphicsExtractor g, float partialTick, int sw, int sh)
    {
        if (ModClient.getHitMarkerTime() <= 0)
            return;

        float a = Math.max((ModClient.getHitMarkerTime() - 10.0f + partialTick) / 10.0f, 0.0f);

        int w = 9;
        int h = 9;
        int x = sw / 2 - 5;
        int y = sh / 2 - 5;

        g.blit(RenderPipelines.GUI_TEXTURED, FlansMod.hitmarkerTexture, x, y, 0, 0, w, h, 16, 16,
            ARGB.white(a));
    }

    /** Fullscreen scope/helmet overlay; mirrors your old quad (centered square using screen height). */
    public static void renderScopeOverlay(GuiGraphicsExtractor g, Identifier texture, int sw, int sh)
    {
        g.blit(RenderPipelines.GUI_TEXTURED, texture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh);

        //TODO: compare with renderArmorOverlay()
    }

    private static boolean renderDamageAbsorptionArmorBar(LocalPlayer player, GuiGraphicsExtractor g, int left, int top)
    {
        if (player == null)
            return false;

        double damageAbsorption = 0.0;
        double bulletAbsorption = 0.0;

        for (EquipmentSlot slot : new EquipmentSlot[] {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET })
        {
            ItemStack stack = player.getItemBySlot(slot);
            if (!(stack.getItem() instanceof CustomArmorItem armorItem))
                continue;

            ArmorType armorType = armorItem.getConfigType();
            damageAbsorption += armorType.getDefence();
            bulletAbsorption += armorType.getBulletDefence();
        }

        int absorptionPoints = toAbsorptionArmorPoints(Math.max(damageAbsorption, bulletAbsorption));
        if (absorptionPoints <= 0)
            return false;

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

        return true;
    }

    private static int toAbsorptionArmorPoints(double absorption)
    {
        return Mth.clamp((int) Math.round(absorption * 20.0), 0, 20);
    }

    private static void drawTintedArmorIcon(GuiGraphicsExtractor g, int x, int y, int u, float red, float green, float blue, float alpha)
    {
        g.blit(RenderPipelines.GUI_TEXTURED, GUI_ICONS_LOCATION, x, y, u, ARMOR_V,
            ARMOR_ICON_SIZE, ARMOR_ICON_SIZE, 256, 256, ARGB.colorFromFloat(alpha, red, green, blue));
    }

    public static void renderPlayerAmmo(GuiGraphicsExtractor g, int sw, int sh)
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
            g.text(font, modeText, modeX + 1, modeY + 1, 0x000000, false);
            g.text(font, modeText, modeX,     modeY,     modeColor, false);

            int xAccum = 0;

            List<ItemStack> bulletStacks = gunItem.getBulletItemStackList(stack, Minecraft.getInstance().level.registryAccess());

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

                g.item(sampleStack, iconX, iconY);

                String s = totalCount > 1 ? String.valueOf(totalCount) : "";

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                if (!s.isEmpty())
                {
                    g.text(font, s, textX + 1, textY + 1, 0x000000, false);
                    g.text(font, s, textX,     textY,     0xFFFFFF, false);
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

                g.item(bulletStack, iconX, iconY);
                g.itemDecorations(font, bulletStack, iconX, iconY);

                int stackCount = bulletStack.getCount();
                String s;
                if (stackCount > 1)
                    s = remaining + "/" + max + " x" + stackCount;
                else
                    s = remaining + "/" + max;

                int textX = (hand == InteractionHand.MAIN_HAND) ? sw / 2 + 112 + xAccum : sw / 2 - 160 - xAccum;
                int textY = sh - 13;
                g.text(font, s, textX + 1, textY + 1, 0x000000, false);
                g.text(font, s, textX,     textY,     0xFFFFFF, false);

                xAccum += 16 + font.width(s);
            }
        }
    }

    public static void renderDigitalAmmo(GuiGraphicsExtractor g, int sw, int sh)
    {
        if (!ModCommonConfig.get().enableDigitalAmmoSystem())
            return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null)
            return;

        int numTypes = Math.min(LocalBulletManager.getNumTypes(), BAR_X_OFFSETS.length);

        int bgX = 10;
        int bgY = sh - 50;

        g.blit(RenderPipelines.GUI_TEXTURED, FlansMod.ammoGuiTexture, bgX, bgY, 0, 30, 120, 12, TEXTURE_WIDTH, TEXTURE_HEIGHT);

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

                g.blit(RenderPipelines.GUI_TEXTURED, FlansMod.ammoGuiTexture, barX, barY, 2, 18, barWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }

    }

    //TODO: implement these methods
    public static void renderTeamInfo(GuiGraphicsExtractor g, int sw, int sh) {}
    public static void renderKillMessages(GuiGraphicsExtractor g, int sw, int sh) {}

    public static void renderVehicleDebug(GuiGraphicsExtractor g, int sw, int sh)
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
        g.text(font, Component.literal(driveable.getConfigType().getName()), LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;

        DriveableData data = driveable.getDriveableData();
        if (data != null)
        {
            DriveablePart core = data.getPart(EnumDriveablePart.CORE);
            if (core != null && core.getMaxHealth() > 0F)
            {
                int healthPercent = Mth.clamp(Math.round(core.getHealth() * 100F / core.getMaxHealth()), 0, 100);
                g.text(font, Component.translatable("hud.flansmodultimate.driveable.health", healthPercent),
                    LEGACY_HUD_LEFT, y, healthColor(healthPercent), false);
                y += LEGACY_HUD_LINE_HEIGHT;
            }

            float tankSize = driveable.getConfigType().getFuelTankSize();
            if (tankSize > 0F)
            {
                int fuelPercent = Mth.clamp(Math.round(driveable.getFuel() * 100F / tankSize), 0, 100);
                g.text(font, Component.translatable("hud.flansmodultimate.driveable.fuel", fuelPercent),
                    LEGACY_HUD_LEFT, y, healthColor(fuelPercent), false);
                y += LEGACY_HUD_LINE_HEIGHT;
            }
        }

        float maximumForwardThrottle = Math.max(0.0001F, driveable.getConfigType().getMaxThrottle());
        float maximumReverseThrottle = Math.max(0.0001F, driveable.getConfigType().getMaxNegativeThrottle());
        float throttleRatio = driveable.getThrottle() >= 0F
            ? driveable.getThrottle() / maximumForwardThrottle
            : driveable.getThrottle() / maximumReverseThrottle;
        int throttlePercent = Math.round(Mth.clamp(throttleRatio, -1F, 1F) * 100F);
        double speed = ModClientConfig.get().driveableSpeedUnit.convert(driveable.getDeltaMovement().length() * 20D);
        g.text(font, Component.translatable("hud.flansmodultimate.driveable.throttle", throttlePercent),
            LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;
        g.text(font, Component.translatable("hud.flansmodultimate.driveable.speed",
            String.format(Locale.ROOT, "%.1f", speed), ModClientConfig.get().driveableSpeedUnit.getSymbol()),
            LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;

        Component gear = Component.translatable(driveable.isGearDeployed()
            ? "hud.flansmodultimate.driveable.gear.down" : "hud.flansmodultimate.driveable.gear.up");
        Component door = Component.translatable(driveable.isDoorOpen()
            ? "hud.flansmodultimate.driveable.door.open" : "hud.flansmodultimate.driveable.door.closed");
        g.text(font, gear, LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;
        g.text(font, door, LEGACY_HUD_LEFT, y, HUD_WHITE, false);

        boolean isVehicle = driveable instanceof Vehicle;
        boolean isPlane = driveable instanceof Plane;
        float yaw = isVehicle ? driveable.getTurretYaw() : driveable.getYaw();
        float pitch = isVehicle ? -driveable.getTurretPitch() : -driveable.getPitch();
        Component yawText = Component.translatable("hud.flansmodultimate.driveable.yaw", Math.round(yaw));
        Component pitchText = Component.translatable("hud.flansmodultimate.driveable.pitch", Math.round(pitch));
        VehicleType vehicleType = isVehicle ? ((Vehicle) driveable).getVehicleType() : null;
        boolean primaryShellBank = driveable.getConfigType().weaponType(false) == EnumWeaponType.SHELL;
        boolean secondaryShellBank = driveable.getConfigType().weaponType(true) == EnumWeaponType.SHELL;
        boolean hasShellBank = primaryShellBank || secondaryShellBank;
        int shellReloadTicks = primaryShellBank ? driveable.getPrimaryReloadTicks()
            : secondaryShellBank ? driveable.getSecondaryReloadTicks() : 0;
        Component shellText = shellReloadTicks > 0
            ? Component.translatable("hud.flansmodultimate.aa_gun.reload_time", String.format(Locale.ROOT, "%.1f", shellReloadTicks / 20F))
            : Component.translatable("hud.flansmodultimate.aa_gun.ready");
        Component primaryAmmoName = driveable.getCurrentPrimaryAmmoName();
        Component secondaryAmmoName = driveable.getCurrentSecondaryAmmoName();
        Component currentAmmoName = !primaryAmmoName.getString().isEmpty() ? primaryAmmoName : secondaryAmmoName;
        boolean hasCurrentAmmo = !currentAmmoName.getString().isEmpty();
        Component ammoHeading = Component.translatable("hud.flansmodultimate.aa_gun.current_ammo");
        boolean hasSmoke = vehicleType != null && vehicleType.isHasFlare() && !vehicleType.getSmokers().isEmpty();
        Component smokeText = Component.translatable(driveable.isVarFlare()
            ? "hud.flansmodultimate.driveable.smoke.deploying"
            : driveable.isCountermeasureReloading()
                ? "hud.flansmodultimate.driveable.smoke.reloading"
                : "hud.flansmodultimate.driveable.smoke.ready");
        int smokeColor = driveable.isVarFlare() ? HUD_RED
            : driveable.isCountermeasureReloading() ? HUD_GOLD : HUD_GREEN;
        Component rollText = Component.translatable("hud.flansmodultimate.driveable.roll", Math.round(driveable.getRoll()));
        Component altitudeText = Component.translatable("hud.flansmodultimate.driveable.altitude",
            Math.round(driveable.getY() - driveable.level().getSeaLevel()));
        Component compassText = Component.translatable("hud.flansmodultimate.driveable.compass", compassDirection(driveable.getYaw()));
        int hudRightX = Math.max(LEGACY_HUD_LEFT,
            sw - 2 - maxWidth(font, yawText, pitchText, shellText, smokeText, ammoHeading, currentAmmoName,
                rollText, altitudeText, compassText));
        int rightLine = 0;
        g.text(font, yawText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        g.text(font, pitchText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        if (isPlane)
        {
            g.text(font, rollText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
            g.text(font, altitudeText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
            g.text(font, compassText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        }
        if (hasShellBank)
            g.text(font, shellText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++,
                shellReloadTicks > 0 ? HUD_RED : HUD_GREEN, false);
        if (hasSmoke)
            g.text(font, smokeText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, smokeColor, false);
        if (hasCurrentAmmo)
        {
            int ammoY = LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine;
            g.text(font, ammoHeading, hudRightX, ammoY, HUD_WHITE, false);
            g.text(font, currentAmmoName, hudRightX, ammoY + LEGACY_HUD_LINE_HEIGHT, HUD_AMMO_GREEN, false);
        }

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
        List<Component> debugLines = List.of(id, position, rotation, turret, input);
        int debugY = Math.max(LEGACY_HUD_TOP, sh - 2 - debugLines.size() * LEGACY_HUD_LINE_HEIGHT);
        for (Component line : debugLines)
        {
            g.text(font, line, rightX, debugY, HUD_GREEN, false);
            debugY += LEGACY_HUD_LINE_HEIGHT;
        }
    }
}
