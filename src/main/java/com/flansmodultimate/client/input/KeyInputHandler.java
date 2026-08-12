package com.flansmodultimate.client.input;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelVehicle;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketDriveableInput;
import com.flansmodultimate.network.server.PacketGunFireMode;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketRequestDebug;
import com.flansmodultimate.network.server.PacketTeamsAction;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.fml.LogicalSide;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Central client key router for guns, teams and server-authoritative driveables. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyInputHandler
{
    private static final String CATEGORY = "key.categories." + FlansMod.MOD_ID;
    private static final int INPUT_KEEPALIVE_TICKS = 5;
    private static final float AIM_CHANGE_EPSILON = 0.1F;
    private static final float FLIGHT_CONTROL_EPSILON = 0.005F;

    private static final KeyMapping reloadKey = key("reload", InputConstants.KEY_R);
    private static final KeyMapping fireModeKey = key("fireMode", InputConstants.KEY_B);
    private static final KeyMapping lookAtGunKey = key("lookAtGun", InputConstants.KEY_M);
    private static final KeyMapping debugKey = new KeyMapping("key." + FlansMod.MOD_ID + ".debug", KeyConflictContext.UNIVERSAL,
        InputConstants.Type.KEYSYM, InputConstants.KEY_F10, CATEGORY);
    private static final KeyMapping teamsMenuKey = key("teams_menu", InputConstants.KEY_G);
    private static final KeyMapping teamsScoresKey = key("teams_scores", InputConstants.KEY_H);
    private static final KeyMapping teamsClassKey = key("teams_class", InputConstants.KEY_O);

    private static final KeyMapping descendKey = key("driveable.descend", InputConstants.KEY_LCONTROL);
    private static final KeyMapping driveableInventoryKey = key("driveable.inventory", InputConstants.KEY_R);
    private static final KeyMapping primaryKey = key("driveable.primary", InputConstants.KEY_V);
    private static final KeyMapping secondaryKey = key("driveable.secondary", InputConstants.KEY_B);
    private static final KeyMapping controlModeKey = key("driveable.control_mode", InputConstants.KEY_C);
    private static final KeyMapping rollLeftKey = key("driveable.roll_left", InputConstants.KEY_Z);
    private static final KeyMapping rollRightKey = key("driveable.roll_right", InputConstants.KEY_X);
    private static final KeyMapping gearKey = key("driveable.gear", InputConstants.KEY_L);
    private static final KeyMapping doorKey = key("driveable.door", InputConstants.KEY_K);
    private static final KeyMapping modeKey = key("driveable.mode", InputConstants.KEY_J);
    private static final KeyMapping flareKey = key("driveable.flare", InputConstants.KEY_N);

    private static final int[] LEGACY_KEYS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 18};

    private static int lastControlEntityId = -1;
    private static int lastInputMask;
    private static int inputSequence;
    private static int ticksSinceInputPacket = INPUT_KEEPALIVE_TICKS;
    private static float lastAimYaw;
    private static float lastAimPitch;
    private static float lastFlightPitch;
    private static float lastFlightRoll;
    private static boolean lastMouseControl;
    private static boolean wasSneaking;
    private static boolean inventoryActionQueued;

    private static KeyMapping key(String name, int keyCode)
    {
        return new KeyMapping("key." + FlansMod.MOD_ID + "." + name, KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM, keyCode, CATEGORY);
    }

    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(reloadKey);
        event.register(fireModeKey);
        event.register(lookAtGunKey);
        event.register(debugKey);
        event.register(teamsMenuKey);
        event.register(teamsScoresKey);
        event.register(teamsClassKey);
        event.register(descendKey);
        event.register(driveableInventoryKey);
        event.register(primaryKey);
        event.register(secondaryKey);
        event.register(controlModeKey);
        event.register(rollLeftKey);
        event.register(rollRightKey);
        event.register(gearKey);
        event.register(doorKey);
        event.register(modeKey);
        event.register(flareKey);
    }

    public static void checkKeys()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean noScreen = mc.screen == null;

        if (player != null && resolveControllable(player) != null)
        {
            updateDriveableControls(mc, player, noScreen);
        }
        else
        {
            resetDriveableInputState();
            if (isGunContext() && noScreen)
            {
                if (reloadKey.consumeClick())
                {
                    doReload();
                    return;
                }
                if (fireModeKey.consumeClick())
                {
                    doSwitchFireMode();
                    return;
                }
                if (lookAtGunKey.consumeClick())
                    doLookAtGun();
            }
        }

        if (noScreen && debugKey.consumeClick())
        {
            if (ModClient.isDebug())
                ModClient.setDebug(false);
            else
                PacketHandler.sendToServer(new PacketRequestDebug());
        }

        if (noScreen && teamsMenuKey.consumeClick())
            PacketHandler.sendToServer(PacketTeamsAction.openTeamMenu());
        if (noScreen && teamsScoresKey.consumeClick())
            PacketHandler.sendToServer(PacketTeamsAction.openScoreboard());
        if (noScreen && teamsClassKey.consumeClick())
            PacketHandler.sendToServer(PacketTeamsAction.openClassMenu());
    }

    private static void updateDriveableControls(Minecraft mc, LocalPlayer player, boolean acceptInput)
    {
        Entity mount = player.getVehicle();
        IControllable controllable = resolveControllable(player);
        Driveable driveable = resolveDriveable(player);
        if (mount == null || controllable == null || driveable == null)
        {
            resetDriveableInputState();
            return;
        }

        ModClient.showDriveableTutorial(player, driveableInventoryKey.getTranslatedKeyMessage(),
            mc.options.keyShift.getTranslatedKeyMessage(), controlModeKey.getTranslatedKeyMessage());

        int mask = 0;
        int edgeMask = 0;
        if (acceptInput)
        {
            if (mc.options.keyUp.isDown()) mask |= DriveableInput.FORWARD;
            if (mc.options.keyDown.isDown()) mask |= DriveableInput.BACKWARD;
            if (mc.options.keyLeft.isDown()) mask |= DriveableInput.LEFT;
            if (mc.options.keyRight.isDown()) mask |= DriveableInput.RIGHT;
            if (mc.options.keyJump.isDown()) mask |= DriveableInput.ASCEND;
            if (descendKey.isDown()) mask |= DriveableInput.DESCEND;
            if (rollLeftKey.isDown()) mask |= DriveableInput.ROLL_LEFT;
            if (rollRightKey.isDown()) mask |= DriveableInput.ROLL_RIGHT;

            // Support both the classic V/B binds and the normal attack/use buttons.
            if (primaryKey.isDown() || mc.options.keyAttack.isDown()) mask |= DriveableInput.PRIMARY_FIRE;
            if (secondaryKey.isDown() || mc.options.keyUse.isDown()) mask |= DriveableInput.SECONDARY_FIRE;

            boolean sneaking = mc.options.keyShift.isDown();
            if (sneaking && !wasSneaking)
                edgeMask |= DriveableInput.EXIT;
            wasSneaking = sneaking;

            if (driveableInventoryKey.consumeClick() || inventoryActionQueued)
                edgeMask |= DriveableInput.MENU;
            inventoryActionQueued = false;
            if (gearKey.consumeClick()) edgeMask |= DriveableInput.TOGGLE_GEAR;
            if (doorKey.consumeClick()) edgeMask |= DriveableInput.TOGGLE_DOOR;
            if (modeKey.consumeClick()) edgeMask |= DriveableInput.TOGGLE_MODE;
            if (flareKey.consumeClick()) edgeMask |= DriveableInput.FLARE;
            if (controlModeKey.consumeClick() && ModClient.tryToggleDriveableControlMode(player, driveable))
                edgeMask |= DriveableInput.CONTROL_MODE;
        }
        else
        {
            wasSneaking = false;
        }

        mask |= edgeMask;
        updateCompatibilityState(controllable, mask, edgeMask, player);

        float aimYaw;
        float aimPitch;
        if (mount instanceof Seat seat)
        {
            aimYaw = seat.getRequestedAimYaw();
            aimPitch = seat.getRequestedAimPitch();
        }
        else
        {
            aimYaw = Mth.wrapDegrees(player.getYRot() - driveable.getYaw());
            aimPitch = player.getXRot();
        }

        boolean mouseControl = driveable instanceof Plane && ModClient.isMouseControlEnabled();
        float flightPitch = mouseControl ? MouseInputHandler.getFlightPitchControl() : 0F;
        float flightRoll = mouseControl ? MouseInputHandler.getFlightRollControl() : 0F;

        boolean changedMount = driveable.getId() != lastControlEntityId;
        boolean changedMask = mask != lastInputMask;
        boolean changedAim = Math.abs(Mth.wrapDegrees(aimYaw - lastAimYaw)) >= AIM_CHANGE_EPSILON
            || Math.abs(aimPitch - lastAimPitch) >= AIM_CHANGE_EPSILON;
        boolean pendingAim = mount instanceof Seat seat && seat.isAimRequestPending(AIM_CHANGE_EPSILON);
        boolean changedFlightControl = Math.abs(flightPitch - lastFlightPitch) >= FLIGHT_CONTROL_EPSILON
            || Math.abs(flightRoll - lastFlightRoll) >= FLIGHT_CONTROL_EPSILON || mouseControl != lastMouseControl;
        boolean keepAlive = ++ticksSinceInputPacket >= INPUT_KEEPALIVE_TICKS;

        if (changedMount || changedMask || changedAim || pendingAim || changedFlightControl || keepAlive)
        {
            Vec3 barrelPitchPivot = getActiveModelAimPivot(driveable, mount);
            if (mount instanceof Seat seat && !seat.isDriverSeat())
                driveable.setModelPassengerGunAimPivot(seat.getSeatIndex(), barrelPitchPivot);
            else
                driveable.setModelBarrelPitchPivot(barrelPitchPivot);
            PacketDriveableInput packet = mount instanceof Seat seat
                ? new PacketDriveableInput(seat, mask, aimYaw, aimPitch, flightPitch, flightRoll,
                    mouseControl, barrelPitchPivot, ++inputSequence)
                : new PacketDriveableInput(driveable, mask, aimYaw, aimPitch, flightPitch, flightRoll,
                    mouseControl, barrelPitchPivot, ++inputSequence);
            PacketHandler.sendToServer(packet);
            lastControlEntityId = driveable.getId();
            lastInputMask = mask;
            lastAimYaw = aimYaw;
            lastAimPitch = aimPitch;
            lastFlightPitch = flightPitch;
            lastFlightRoll = flightRoll;
            lastMouseControl = mouseControl;
            ticksSinceInputPacket = 0;
        }
    }

    @Nullable
    private static Vec3 getVehicleBarrelPitchPivot(Driveable driveable)
    {
        return driveable.getConfigType() != null
            && ModelCache.getOrLoadTypeModel(driveable.getConfigType()) instanceof ModelVehicle model
            ? model.getPrimaryBarrelPitchPivot() : null;
    }

    @Nullable
    private static Vec3 getActiveModelAimPivot(Driveable driveable, Entity mount)
    {
        if (!(mount instanceof Seat seat) || seat.isDriverSeat())
            return getVehicleBarrelPitchPivot(driveable);
        if (seat.getSeatInfo() == null || driveable.getConfigType() == null
            || !(ModelCache.getOrLoadTypeModel(driveable.getConfigType()) instanceof ModelDriveable model))
            return null;
        return model.getRegisteredGunAimPivot(seat.getSeatInfo().getGunName());
    }

    private static void updateCompatibilityState(IControllable controllable, int mask, int edgeMask, Player player)
    {
        for (int legacyKey : LEGACY_KEYS)
        {
            int flag = DriveableInput.forLegacyKey(legacyKey);
            boolean held = flag != 0 && (mask & flag) != 0;
            controllable.updateKeyHeldState(legacyKey, held);
            if (flag != 0 && (edgeMask & flag) != 0)
                controllable.pressKey(legacyKey, player, true);
        }
    }

    private static void resetDriveableInputState()
    {
        lastControlEntityId = -1;
        lastInputMask = 0;
        lastAimYaw = 0F;
        lastAimPitch = 0F;
        lastFlightPitch = 0F;
        lastFlightRoll = 0F;
        lastMouseControl = false;
        ticksSinceInputPacket = INPUT_KEEPALIVE_TICKS;
        wasSneaking = false;
        inventoryActionQueued = false;
        MouseInputHandler.resetFlightControls();
    }

    /** Called when vanilla tries to open the player inventory while mounted. */
    public static void queueDriveableInventoryAction()
    {
        inventoryActionQueued = true;
    }

    @Nullable
    public static IControllable resolveControllable(Player player)
    {
        if (player == null)
            return null;
        Entity mount = player.getVehicle();
        if (mount instanceof IControllable controllable)
            return controllable;
        if (mount instanceof Seat seat)
            return seat.getDriveable();
        return null;
    }

    @Nullable
    public static Driveable resolveDriveable(Player player)
    {
        if (player == null)
            return null;
        Entity mount = player.getVehicle();
        if (mount instanceof Driveable driveable)
            return driveable;
        if (mount instanceof Seat seat)
            return seat.getDriveable();
        return null;
    }

    private static void doSwitchFireMode()
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        if (canSwitchFireMode(player.getMainHandItem()))
        {
            PacketHandler.sendToServer(new PacketGunFireMode(InteractionHand.MAIN_HAND));
            return;
        }
        if (canSwitchFireMode(player.getOffhandItem()))
            PacketHandler.sendToServer(new PacketGunFireMode(InteractionHand.OFF_HAND));
    }

    private static boolean canSwitchFireMode(ItemStack stack)
    {
        return stack.getItem() instanceof GunItem gunItem && gunItem.getConfigType().canSwitchFireMode(stack);
    }

    private static boolean isGunContext()
    {
        Player player = Minecraft.getInstance().player;
        return player != null && (player.getMainHandItem().getItem() instanceof GunItem
            || player.getOffhandItem().getItem() instanceof GunItem);
    }

    private static void doReload()
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        PlayerData data = PlayerData.getInstance(player, LogicalSide.CLIENT);
        ItemStack mainHandStack = player.getMainHandItem();
        ItemStack offhandStack = player.getOffhandItem();

        if (data.getShootTimeRight() <= 0.0F && data.getShootTimeLeft() <= 0.0F)
        {
            if (mainHandStack.getItem() instanceof GunItem gunItem && !(offhandStack.getItem() instanceof GunItem))
            {
                if (gunItem.getGunItemHandler().canReload(player.getInventory()))
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.MAIN_HAND));
            }
            else if (offhandStack.getItem() instanceof GunItem gunItem && !(mainHandStack.getItem() instanceof GunItem))
            {
                if (gunItem.getGunItemHandler().canReload(player.getInventory()))
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.OFF_HAND));
            }
            else if (mainHandStack.getItem() instanceof GunItem mainHandGunItem && offhandStack.getItem() instanceof GunItem offhandGunItem)
            {
                if (offhandGunItem.getGunItemHandler().canReload(player.getInventory())
                    && (!mainHandGunItem.getGunItemHandler().canReload(player.getInventory())
                    || (!mainHandGunItem.getGunItemHandler().hasEmptyAmmo(mainHandStack, player.level().registryAccess())
                    && offhandGunItem.getGunItemHandler().hasEmptyAmmo(offhandStack, player.level().registryAccess()))))
                {
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.OFF_HAND));
                }
                else if (mainHandGunItem.getGunItemHandler().canReload(player.getInventory()))
                {
                    PacketHandler.sendToServer(new PacketGunReload(InteractionHand.MAIN_HAND));
                }
            }
        }
    }

    private static void doLookAtGun()
    {
        Player player = Minecraft.getInstance().player;
        ModClient.getGunAnimations(player, InteractionHand.MAIN_HAND).setLookAt(GunAnimations.EnumLookAtState.TILT1);
        ModClient.getGunAnimations(player, InteractionHand.OFF_HAND).setLookAt(GunAnimations.EnumLookAtState.TILT1);
    }
}
