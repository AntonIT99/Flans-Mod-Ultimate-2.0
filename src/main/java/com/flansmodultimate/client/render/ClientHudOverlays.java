package com.flansmodultimate.client.render;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.digitalammo.LocalBulletManager;
import com.flansmodultimate.client.input.KeyInputHandler;
import com.flansmodultimate.client.teams.TeamsClientState;
import com.flansmodultimate.common.driveables.DriveableControlPhysics;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.driveables.DriveablePart;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.EnumWeaponType;
import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.guns.EnumFireMode;
import com.flansmodultimate.common.item.CustomArmorItem;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.item.ShootableItem;
import com.flansmodultimate.common.types.AAGunType;
import com.flansmodultimate.common.types.ArmorType;
import com.flansmodultimate.common.types.GunType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.config.EnumHitMarkerStyle;
import com.flansmodultimate.config.ModClientConfig;
import com.flansmodultimate.config.ModCommonConfig;
import com.flansmodultimate.network.client.PacketTeamsState;
import com.flansmodultimate.util.ModUtils;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import org.jetbrains.annotations.Nullable;

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
    private static final int LEGACY_HUD_TOP = 2;
    private static final int LEGACY_HUD_LINE_HEIGHT = 10;
    /** Extra clearance from the right screen edge so longer translated strings don't get clipped. */
    private static final int HUD_RIGHT_MARGIN = 12;
    private static final int HUD_WHITE = 0xFFFFFF;
    private static final int HUD_GREEN = 0x00FF00;
    private static final int HUD_AMMO_GREEN = 0x24FF62;
    private static final int HUD_GOLD = 0xDAA520;
    private static final int HUD_YELLOW = 0xFFD700;
    private static final int HUD_ORANGE = 0xFF8C00;
    private static final int HUD_RED = 0xFF0000;
    private static final int TEAM_INFO_HALF_WIDTH = 43;
    private static final int TEAM_INFO_HEIGHT = 27;
    private static final int TEAM_INFO_U = 85;
    private static final int TEAM_INFO_V = 0;
    private static final int TEAM_COLOUR_WIDTH = 24;
    private static final int TEAM_COLOUR_V = 98;
    private static final int TEAM_COLOUR_LEFT_U = 0;
    private static final int TEAM_COLOUR_RIGHT_U = 62;
    private static final int KILL_MESSAGE_LINE_HEIGHT = 16;
    private static final String KILL_MESSAGE_GAP = "     ";
    private static final String KILL_MESSAGE_HEADSHOT_GAP = "         ";
    private static final int HEADSHOT_SYMBOL_SIZE = 16;
    private static final int HEADSHOT_SYMBOL_SHEET = 64;
    private static final float HIT_MARKER_FADE_TICKS = 20F;
    private static final float WOUNDED_FLASH_FADE_TICKS = 20F;
    /** Fraction of a flashbang's duration spent fading back out. */
    private static final float FLASH_FADE_FRACTION = 0.4F;
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
        g.drawString(font, Component.literal(type.getName()), LEGACY_HUD_LEFT, LEGACY_HUD_TOP, HUD_WHITE, false);
        int healthPercent = type.getHealth() <= 0
            ? 0
            : Mth.clamp(Math.round(aaGun.getHealth() * 100F / type.getHealth()), 0, 100);
        Component health = Component.translatable("hud.flansmodultimate.aa_gun.health", healthPercent, aaGun.getHealth(), type.getHealth());
        g.drawString(font, health, LEGACY_HUD_LEFT, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, healthColor(healthPercent), false);

        float yaw = Mth.rotLerp(partialTick, aaGun.getPrevGunYaw(), aaGun.getGunYaw());
        float pitch = Mth.lerp(partialTick, aaGun.getPrevGunPitch(), aaGun.getGunPitch());
        Component yawText = Component.translatable("hud.flansmodultimate.aa_gun.yaw", Math.round(yaw));
        Component pitchText = Component.translatable("hud.flansmodultimate.aa_gun.gun_pitch", Math.round(-pitch));

        Component currentAmmoName = aaGun.getCurrentAmmoName();
        boolean hasCurrentAmmo = !currentAmmoName.getString().isEmpty();
        Component reloadText = !hasCurrentAmmo
            ? Component.translatable("hud.flansmodultimate.aa_gun.no_ammo")
            : aaGun.getReloadTimer() > 0
                ? Component.translatable("hud.flansmodultimate.aa_gun.reload_time", String.format(Locale.ROOT, "%.1f", aaGun.getReloadTimer() / 20F))
                : Component.translatable("hud.flansmodultimate.aa_gun.ready");
        int reloadColor = !hasCurrentAmmo || aaGun.getReloadTimer() > 0 ? HUD_RED : HUD_GREEN;
        Component ammoHeading = Component.translatable("hud.flansmodultimate.aa_gun.current_ammo");

        int rightX = Math.max(LEGACY_HUD_LEFT, sw - HUD_RIGHT_MARGIN - maxWidth(font, yawText, pitchText, reloadText, ammoHeading,
            currentAmmoName));

        g.drawString(font, yawText, rightX, LEGACY_HUD_TOP, HUD_WHITE, false);
        g.drawString(font, pitchText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT, HUD_WHITE, false);
        g.drawString(font, reloadText, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 2, reloadColor, false);

        if (hasCurrentAmmo)
        {
            g.drawString(font, ammoHeading, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 3, HUD_WHITE, false);
            g.drawString(font, currentAmmoName, rightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * 4, HUD_AMMO_GREEN, false);
        }
    }

    private static int healthColor(int healthPercent)
    {
        if (healthPercent >= 75)
            return HUD_GREEN;
        if (healthPercent >= 50)
            return HUD_YELLOW;
        if (healthPercent >= 25)
            return HUD_ORANGE;
        return HUD_RED;
    }

    private static int maxWidth(Font font, Component... lines)
    {
        int width = 0;
        for (Component line : lines)
            width = Math.max(width, font.width(line));
        return width;
    }

    private record OrdnanceLine(Component text, int color) {}

    /** Appends a Shell/Bomb/Missile readiness line for a weapon bank, if that bank fires ordnance. */
    private static void addOrdnanceLine(List<OrdnanceLine> lines, EnumWeaponType weapon, int reloadTicks, Component ammoName)
    {
        if (!EnumWeaponType.TAB_DRIVEABLES_TYPES.contains(weapon))
            return;

        String labelKey = "hud.flansmodultimate.driveable." + weapon.name().toLowerCase(Locale.ROOT);
        boolean hasAmmo = !ammoName.getString().isEmpty();
        if (!hasAmmo)
            lines.add(new OrdnanceLine(Component.translatable(labelKey + ".no_ammo"), HUD_RED));
        else if (reloadTicks > 0)
            lines.add(new OrdnanceLine(Component.translatable(labelKey + ".reload_time",
                String.format(Locale.ROOT, "%.1f", reloadTicks / 20F)), HUD_RED));
        else
            lines.add(new OrdnanceLine(Component.translatable(labelKey + ".ready"), HUD_GREEN));
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

    public static final IGuiOverlay DAMAGE_ABSORPTION = (gui, g, partialTick, sw, sh) -> {
        if (!ModClientConfig.get().showArmorDamageAbsorptionBar || gui.getMinecraft().options.hideGui || !gui.shouldDrawSurvivalElements())
            return;

        LocalPlayer player = gui.getMinecraft().player;
        boolean vanillaArmorVisible = player != null && player.getArmorValue() > 0;
        int top = sh - gui.leftHeight + (vanillaArmorVisible ? 0 : 10);
        if (renderDamageAbsorptionArmorBar(player, g, sw / 2 - 91, top) && vanillaArmorVisible)
            gui.leftHeight += 10;
    };

    /** Draw the hit marker in the style selected in the client config, with fade-out alpha. */
    public static void renderHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        if (ModClient.getHitMarkerTime() <= 0)
            return;

        if (ModClientConfig.get().hitMarkerStyle == EnumHitMarkerStyle.ULTIMATE)
            renderUltimateHitMarker(g, partialTick, sw, sh);
        else
            renderClassicHitMarker(g, partialTick, sw, sh);
    }

    /** Flan's Mod 1.12.2 style: small icon at screen center. */
    private static void renderClassicHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        float alpha = Math.max((ModClient.getHitMarkerTime() - 10F + partialTick) / 10F, 0F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, alpha);
        g.blit(FlansMod.TEXTURE_GUI_BASICHITMARKER, sw / 2 - 5, sh / 2 - 5, 0, 0, 9, 9, 16, 16);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    /**
     * Flan's Mod Ultimate 1.7.10 style: full screen overlay tinted by the kind of hit that was scored.
     * Red = no penetration, green = full damage, light blue = headshot, yellow = explosion.
     */
    private static void renderUltimateHitMarker(GuiGraphics g, float partialTick, int sw, int sh)
    {
        float alpha = Mth.clamp((ModClient.getHitMarkerTime() - partialTick) / HIT_MARKER_FADE_TICKS, 0F, 1F);

        float red = 1F;
        float green = 1F;
        float blue = 1F;

        if (ModClientConfig.get().fancyHitMarker)
        {
            if (ModClient.isHitMarkerExplosion())
            {
                red = 0.95F;
                green = 0.85F;
                blue = 0.3F;
            }
            else if (ModClient.isHitMarkerHeadshot())
            {
                red = 0F;
                green = 0.5F;
                blue = 1F;
            }
            else
            {
                // Two stage transition between red and green, to avoid going through yellow
                float penetration = Mth.clamp(ModClient.getHitMarkerPenAmount(), 0F, 1F);
                red = penetration < 0.5F ? 1F : 2F * (1F - penetration);
                green = penetration < 0.5F ? 2F * penetration : 1F;
                blue = 0F;
            }
        }

        renderFullScreenOverlay(g, hitMarkerTexture(), sw, sh, red, green, blue, alpha);
    }

    /** The HitTexture of a held gun overrides the built-in full screen hit marker. */
    private static ResourceLocation hitMarkerTexture()
    {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null)
        {
            for (GunItem gunItem : ModUtils.getGunItemsInHands(player))
            {
                ResourceLocation hitTexture = gunItem.getConfigType().getHitTexture();
                if (hitTexture != null)
                    return hitTexture;
            }
        }
        return ModClientConfig.get().hdHitMarker ? FlansMod.TEXTURE_GUI_FMUHITMARKERHD : FlansMod.TEXTURE_GUI_FMUHITMARKER;
    }

    /**
     * Blinding white overlay of a flashbang. It is held at full strength for most of
     * its time and then faded out, instead of the legacy hard cut back to normal.
     */
    public static final IGuiOverlay FLASH_BANG = (gui, g, partialTick, sw, sh) -> {
        if (!ModClient.isInFlash() || ModClient.getFlashTime() <= 0 || gui.getMinecraft().options.hideGui)
            return;

        float remaining = ModClient.getFlashTime() - partialTick;
        float fadeTicks = Math.max(1F, ModClient.getFlashDuration() * FLASH_FADE_FRACTION);
        float alpha = Mth.clamp(remaining / fadeTicks, 0F, 1F);
        renderFullScreenOverlay(g, FlansMod.TEXTURE_GUI_FLASH, sw, sh, 1F, 1F, 1F, alpha);
    };

    /** Flan's Mod Ultimate 1.7.10 style red flash shown while the player is wounded. */
    public static final IGuiOverlay WOUNDED_FLASH = (gui, g, partialTick, sw, sh) -> {
        if (!ModClientConfig.get().showFlashesWhenWounded || ModClient.getWoundedTime() <= 0 || gui.getMinecraft().options.hideGui)
            return;

        float alpha = Mth.clamp((ModClient.getWoundedTime() - partialTick) / WOUNDED_FLASH_FADE_TICKS, 0F, 1F);
        renderFullScreenOverlay(g, FlansMod.TEXTURE_GUI_BLOOD, sw, sh, 1F, 1F, 1F, alpha);
    };

    private static void renderFullScreenOverlay(GuiGraphics g, ResourceLocation texture, int sw, int sh, float red, float green, float blue, float alpha)
    {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        g.blit(texture, sw / 2 - 2 * sh, 0, 0, 0, 4 * sh, sh, 4 * sh, sh);
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
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

        RenderSystem.setShaderTexture(0, FlansMod.TEXTURE_GUI_AMMOGUI);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        int bgX = 10;
        int bgY = sh - 50;

        g.blit(FlansMod.TEXTURE_GUI_AMMOGUI, bgX, bgY, 0, 30, 120, 12, TEXTURE_WIDTH, TEXTURE_HEIGHT);

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

                RenderSystem.setShaderTexture(0, FlansMod.TEXTURE_GUI_AMMOGUI);
                g.blit(FlansMod.TEXTURE_GUI_AMMOGUI, barX, barY, 2, 18, barWidth, BAR_HEIGHT, TEXTURE_WIDTH, TEXTURE_HEIGHT);
            }
        }

        RenderSystem.disableBlend();
    }

    /**
     * Legacy Teams banner at the top of the screen: both team scores and colours, the map and
     * gametype names, the time left, and the local player's own score, kills and deaths.
     */
    public static void renderTeamInfo(GuiGraphics g, int sw, int sh)
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        PacketTeamsState state = TeamsClientState.get();
        if (player == null || mc.options.hideGui || state == null || !state.isEnabled() || state.getGameType().isBlank())
            return;

        PacketTeamsState.PlayerScore localScore = findPlayerScore(state, player.getGameProfile().getName());
        boolean teamGame = state.isSortedByTeam() && state.getTeamScores().size() == 2;
        if (localScore == null || (state.getTeamScores().isEmpty() && state.isSortedByTeam()))
            return;

        int centre = sw / 2;
        Font font = mc.font;

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        g.blit(FlansMod.TEXTURE_GUI_TEAMSSCORES, centre - TEAM_INFO_HALF_WIDTH, 0,
            TEAM_INFO_U, TEAM_INFO_V, TEAM_INFO_HALF_WIDTH * 2, TEAM_INFO_HEIGHT, 256, 256);

        if (teamGame)
        {
            PacketTeamsState.TeamScore left = state.getTeamScores().get(0);
            PacketTeamsState.TeamScore right = state.getTeamScores().get(1);
            drawTeamColourBlock(g, centre - TEAM_INFO_HALF_WIDTH, TEAM_COLOUR_LEFT_U, left.colour());
            drawTeamColourBlock(g, centre + TEAM_INFO_HALF_WIDTH - TEAM_COLOUR_WIDTH, TEAM_COLOUR_RIGHT_U, right.colour());
            RenderSystem.setShaderColor(1F, 1F, 1F, 1F);

            String leftScore = Integer.toString(left.score());
            String rightScore = Integer.toString(right.score());
            g.drawString(font, leftScore, centre - 36, 8, HUD_WHITE, true);
            g.drawString(font, rightScore, centre + 34 - font.width(rightScore), 8, HUD_WHITE, true);
        }

        g.drawString(font, state.getGameType(), centre + 47, 8, HUD_WHITE, true);
        g.drawString(font, state.getMapName(), centre - 48 - font.width(state.getMapName()), 8, HUD_WHITE, true);

        String timeLeft = timeText(state);
        g.drawString(font, timeLeft, centre - font.width(timeLeft) / 2, 30, HUD_WHITE, true);

        g.drawString(font, Integer.toString(localScore.score()), centre - 7, 1, HUD_WHITE, true);
        g.drawString(font, Integer.toString(localScore.kills()), centre - 7, 9, HUD_WHITE, true);
        g.drawString(font, Integer.toString(localScore.deaths()), centre - 7, 17, HUD_WHITE, true);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Remaining round time as m:ss, like the legacy banner. */
    private static String timeText(PacketTeamsState state)
    {
        int seconds = Math.max(0, state.getTimeLeftTicks() / 20);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }

    /** Tints one end of the banner with a team's colour. */
    private static void drawTeamColourBlock(GuiGraphics g, int x, int u, int colour)
    {
        RenderSystem.setShaderColor(((colour >> 16) & 0xFF) / 255F, ((colour >> 8) & 0xFF) / 255F, (colour & 0xFF) / 255F, 1F);
        g.blit(FlansMod.TEXTURE_GUI_TEAMSSCORES, x, 0, u, TEAM_COLOUR_V, TEAM_COLOUR_WIDTH, TEAM_INFO_HEIGHT, 256, 256);
    }

    @Nullable
    private static PacketTeamsState.PlayerScore findPlayerScore(PacketTeamsState state, String name)
    {
        return state.getTeamScores().stream()
            .flatMap(team -> team.players().stream())
            .filter(entry -> entry.name().equalsIgnoreCase(name))
            .findFirst().orElse(null);
    }

    /**
     * Kill feed in the bottom right corner: killer, weapon icon, an extra crosshair for a
     * headshot, and the victim, each line rising as newer kills come in.
     */
    public static void renderKillMessages(GuiGraphics g, int sw, int sh)
    {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || KillMessageFeed.getMessages().isEmpty())
            return;

        Font font = mc.font;
        for (KillMessageFeed.Entry message : KillMessageFeed.getMessages())
        {
            String killer = message.getKiller().getString();
            String victim = message.getVictim().getString();
            String gap = message.isHeadshot() ? KILL_MESSAGE_HEADSHOT_GAP : KILL_MESSAGE_GAP;
            int textY = sh - 32 - message.getLine() * KILL_MESSAGE_LINE_HEIGHT;
            int iconY = sh - 36 - message.getLine() * KILL_MESSAGE_LINE_HEIGHT;

            Component line = Component.empty().append(message.getKiller())
                .append(Component.literal(gap)).append(message.getVictim());
            g.drawString(font, line, sw - font.width(killer + gap + victim) - 6, textY, HUD_WHITE, true);

            if (!message.getWeapon().isEmpty())
                g.renderItem(message.getWeapon(), sw - font.width(gap + victim), iconY);

            if (message.isHeadshot())
            {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
                g.blit(FlansMod.TEXTURE_GUI_HEADSHOTSYMBOL, sw - font.width(KILL_MESSAGE_GAP + victim), iconY,
                    HEADSHOT_SYMBOL_SIZE, HEADSHOT_SYMBOL_SIZE, 0F, 0F,
                    HEADSHOT_SYMBOL_SHEET, HEADSHOT_SYMBOL_SHEET, HEADSHOT_SYMBOL_SHEET, HEADSHOT_SYMBOL_SHEET);
            }
        }
    }

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
                g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.health", healthPercent,
                    Math.round(core.getHealth()), Math.round(core.getMaxHealth())),
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

        int throttlePercent = DriveableControlPhysics.throttlePercent(driveable.getThrottle());
        double speed = ModClientConfig.get().driveableSpeedUnit.convert(driveable.getDeltaMovement().length() * 20D);
        g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.throttle", throttlePercent),
            LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;
        g.drawString(font, Component.translatable("hud.flansmodultimate.driveable.speed",
            String.format(Locale.ROOT, "%.1f", speed), ModClientConfig.get().driveableSpeedUnit.getSymbol()),
            LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;

        Component gear = Component.translatable(driveable.isGearDeployed()
            ? "hud.flansmodultimate.driveable.gear.down" : "hud.flansmodultimate.driveable.gear.up");
        Component door = Component.translatable(driveable.isDoorOpen()
            ? "hud.flansmodultimate.driveable.door.open" : "hud.flansmodultimate.driveable.door.closed");
        g.drawString(font, gear, LEGACY_HUD_LEFT, y, HUD_WHITE, false);
        y += LEGACY_HUD_LINE_HEIGHT;
        g.drawString(font, door, LEGACY_HUD_LEFT, y, HUD_WHITE, false);

        boolean isVehicle = driveable instanceof Vehicle;
        boolean isPlane = driveable instanceof Plane;
        // Only the driver's aim reaches the turret. A passenger gunner traverses
        // their own seat, so read the angles back from whichever seat the local
        // player is actually sitting in.
        Seat gunnerSeat = player.getVehicle() instanceof Seat seat && !seat.isDriverSeat() ? seat : null;
        float yaw = gunnerSeat != null ? gunnerSeat.getAimYaw()
            : isVehicle ? driveable.getTurretYaw() : driveable.getYaw();
        float pitch = gunnerSeat != null ? -gunnerSeat.getAimPitch()
            : isVehicle ? -driveable.getTurretPitch() : -driveable.getPitch();
        Component yawText = Component.translatable("hud.flansmodultimate.driveable.yaw", Math.round(yaw));
        Component pitchText = Component.translatable("hud.flansmodultimate.driveable.pitch", Math.round(pitch));
        VehicleType vehicleType = isVehicle ? ((Vehicle) driveable).getVehicleType() : null;
        Component primaryAmmoName = driveable.getCurrentPrimaryAmmoName();
        Component secondaryAmmoName = driveable.getCurrentSecondaryAmmoName();
        List<OrdnanceLine> ordnanceLines = new java.util.ArrayList<>();
        addOrdnanceLine(ordnanceLines, driveable.getConfigType().weaponType(false), driveable.getPrimaryReloadTicks(), primaryAmmoName);
        addOrdnanceLine(ordnanceLines, driveable.getConfigType().weaponType(true), driveable.getSecondaryReloadTicks(), secondaryAmmoName);
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
        int rightWidth = maxWidth(font, yawText, pitchText, smokeText, ammoHeading, currentAmmoName,
            rollText, altitudeText, compassText);
        for (OrdnanceLine line : ordnanceLines)
            rightWidth = Math.max(rightWidth, font.width(line.text()));
        int hudRightX = Math.max(LEGACY_HUD_LEFT, sw - HUD_RIGHT_MARGIN - rightWidth);
        int rightLine = 0;
        g.drawString(font, yawText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        g.drawString(font, pitchText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        if (isPlane)
        {
            g.drawString(font, rollText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
            g.drawString(font, altitudeText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
            g.drawString(font, compassText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, HUD_WHITE, false);
        }
        for (OrdnanceLine line : ordnanceLines)
            g.drawString(font, line.text(), hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, line.color(), false);
        if (hasSmoke)
            g.drawString(font, smokeText, hudRightX, LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine++, smokeColor, false);
        if (hasCurrentAmmo)
        {
            int ammoY = LEGACY_HUD_TOP + LEGACY_HUD_LINE_HEIGHT * rightLine;
            g.drawString(font, ammoHeading, hudRightX, ammoY, HUD_WHITE, false);
            g.drawString(font, currentAmmoName, hudRightX, ammoY + LEGACY_HUD_LINE_HEIGHT, HUD_AMMO_GREEN, false);
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
            g.drawString(font, line, rightX, debugY, HUD_GREEN, false);
            debugY += LEGACY_HUD_LINE_HEIGHT;
        }
    }
}
