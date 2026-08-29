package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

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
import com.flansmodultimate.common.entity.Mecha;
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
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.fml.LogicalSide;
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

import java.util.List;
import java.util.Objects;

/** Central client key router for guns, teams and server-authoritative driveables. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyInputHandler
{
    private static final String CATEGORY_GENERAL = "key.categories." + FlansMod.MOD_ID + ".general";
    private static final String CATEGORY_PLANES = "key.categories." + FlansMod.MOD_ID + ".planes";
    private static final String CATEGORY_VEHICLES = "key.categories." + FlansMod.MOD_ID + ".vehicles";
    private static final int INPUT_KEEPALIVE_TICKS = 5;
    private static final float AIM_CHANGE_EPSILON = 0.1F;
    private static final float FLIGHT_CONTROL_EPSILON = 0.005F;

    private static final KeyMapping reloadKey = key("reload", InputConstants.KEY_R, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping fireModeKey = key("fireMode", InputConstants.KEY_B, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping lookAtGunKey = key("lookAtGun", InputConstants.KEY_M, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping debugKey = new KeyMapping("key." + FlansMod.MOD_ID + ".debug", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, InputConstants.KEY_F10, CATEGORY_GENERAL);
    private static final KeyMapping teamsMenuKey = key("teams_menu", InputConstants.KEY_U, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping teamsScoresKey = key("teams_scores", InputConstants.KEY_I, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping teamsClassKey = key("teams_class", InputConstants.KEY_O, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);

    // Aircraft fly on their own binds rather than the vanilla movement keys, so
    // the flight axes can be rebound without changing how a player walks or
    // drives. Ground vehicles and mechas keep the vanilla keys.
    private static final KeyMapping pitchDownKey = key("driveable.pitch_down", InputConstants.KEY_W, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping pitchUpKey = key("driveable.pitch_up", InputConstants.KEY_S, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping yawLeftKey = key("driveable.yaw_left", InputConstants.KEY_A, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping yawRightKey = key("driveable.yaw_right", InputConstants.KEY_D, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping rollLeftKey = key("driveable.roll_left", InputConstants.KEY_Q, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping rollRightKey = key("driveable.roll_right", InputConstants.KEY_E, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping throttleUpKey = key("driveable.throttle_up", InputConstants.KEY_ADD, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping throttleDownKey = key("driveable.throttle_down", GLFW.GLFW_KEY_KP_SUBTRACT, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping controlModeKey = key("driveable.control_mode", InputConstants.KEY_C, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping gearKey = key("driveable.gear", InputConstants.KEY_G, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping modeKey = key("driveable.mode", InputConstants.KEY_J, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping flareKey = key("driveable.flare", InputConstants.KEY_F, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);

    private static final KeyMapping driveableInventoryKey = key("driveable.inventory", InputConstants.KEY_R, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);
    private static final KeyMapping primaryKey = key("driveable.primary", InputConstants.KEY_SPACE, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);
    private static final KeyMapping secondaryKey = key("driveable.secondary", InputConstants.KEY_V, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);
    private static final KeyMapping doorKey = key("driveable.door", InputConstants.KEY_K, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);

    /** Binds that claim their key while the player is at the controls of an aircraft. */
    private static final List<KeyMapping> AIRCRAFT_BINDS = List.of(pitchDownKey, pitchUpKey,
        yawLeftKey, yawRightKey, rollLeftKey, rollRightKey, throttleUpKey, throttleDownKey,
        controlModeKey, gearKey, modeKey, flareKey, driveableInventoryKey, primaryKey, secondaryKey, doorKey);
    /** Binds that claim their key while the player is at the controls of anything else. */
    private static final List<KeyMapping> GROUND_BINDS = List.of(driveableInventoryKey, primaryKey,
        secondaryKey, doorKey, modeKey, flareKey);

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

    private static KeyMapping key(String name, int keyCode, IKeyConflictContext context, String category)
    {
        return new KeyMapping("key." + FlansMod.MOD_ID + "." + name, context, InputConstants.Type.KEYSYM, keyCode, category);
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
        event.register(pitchDownKey);
        event.register(pitchUpKey);
        event.register(yawLeftKey);
        event.register(yawRightKey);
        event.register(rollLeftKey);
        event.register(rollRightKey);
        event.register(throttleUpKey);
        event.register(throttleDownKey);
        event.register(controlModeKey);
        event.register(gearKey);
        event.register(modeKey);
        event.register(flareKey);
        event.register(driveableInventoryKey);
        event.register(primaryKey);
        event.register(secondaryKey);
        event.register(doorKey);
    }

    /**
     * Claims keys shared with vanilla before {@code Minecraft.handleKeybinds}
     * gets to consume them, so rolling an aircraft with Q or E does not also
     * throw the held item or open the inventory. Only binds that are live for
     * the driveable being controlled take priority, so the same keys keep
     * working normally on foot and in a ground vehicle.
     */
    public static void claimConflictingVanillaKeys()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null)
            return;

        Driveable driveable = resolveDriveable(player);
        Entity mount = player.getVehicle();
        // Only the seat that actually steers loses the vanilla actions. A
        // passenger's Q and E do nothing to the aircraft, so they keep theirs.
        boolean driving = mount instanceof Seat seat ? seat.isDriverSeat() : mount instanceof Driveable;
        if (driveable == null || !driving)
            return;

        List<KeyMapping> claimed = driveable instanceof Plane ? AIRCRAFT_BINDS : GROUND_BINDS;
        for (KeyMapping vanilla : List.of(mc.options.keyInventory, mc.options.keyDrop,
            mc.options.keySwapOffhand, mc.options.keyAdvancements, mc.options.keySocialInteractions))
        {
            if (!vanilla.isUnbound() && isClaimedBy(vanilla, claimed))
            {
                // consumeClick drains one queued press at a time.
                while (vanilla.consumeClick()) { /* claimed by the driveable bind */ }
            }
        }
    }

    private static boolean isClaimedBy(KeyMapping vanilla, List<KeyMapping> claimed)
    {
        for (KeyMapping bind : claimed)
        {
            if (!bind.isUnbound() && vanilla.same(bind))
                return true;
        }
        return false;
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
        boolean aircraft = driveable instanceof Plane;
        if (acceptInput)
        {
            if (aircraft)
            {
                // The flight axes are their own binds. FORWARD and BACKWARD
                // still mean throttle to the server; only the keys behind them
                // moved, off the stick and onto the throttle hand.
                if (throttleUpKey.isDown()) mask |= DriveableInput.FORWARD;
                if (throttleDownKey.isDown()) mask |= DriveableInput.BACKWARD;
                if (yawLeftKey.isDown()) mask |= DriveableInput.LEFT;
                if (yawRightKey.isDown()) mask |= DriveableInput.RIGHT;
                if (pitchUpKey.isDown()) mask |= DriveableInput.ASCEND;
                if (pitchDownKey.isDown()) mask |= DriveableInput.DESCEND;
                if (rollLeftKey.isDown()) mask |= DriveableInput.ROLL_LEFT;
                if (rollRightKey.isDown()) mask |= DriveableInput.ROLL_RIGHT;
            }
            else
            {
                if (mc.options.keyUp.isDown()) mask |= DriveableInput.FORWARD;
                if (mc.options.keyDown.isDown()) mask |= DriveableInput.BACKWARD;
                if (mc.options.keyLeft.isDown()) mask |= DriveableInput.LEFT;
                if (mc.options.keyRight.isDown()) mask |= DriveableInput.RIGHT;
                if (mc.options.keyJump.isDown()) mask |= DriveableInput.ASCEND;
            }

            // Support both the classic V/B binds and the normal attack/use buttons.
            if (primaryKey.isDown() || mc.options.keyAttack.isDown()) mask |= DriveableInput.PRIMARY_FIRE;
            if (secondaryKey.isDown() || mc.options.keyUse.isDown()) mask |= DriveableInput.SECONDARY_FIRE;

            boolean sneaking = mc.options.keyShift.isDown();
            if (sneaking && !wasSneaking)
                edgeMask |= DriveableInput.EXIT;
            wasSneaking = sneaking;

            if (driveableInventoryKey.consumeClick())
                edgeMask |= DriveableInput.MENU;
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
            // A mecha consumes relative look into its torso. Send the resulting
            // world-space torso target so delayed retries remain idempotent.
            aimYaw = driveable instanceof Mecha
                ? Mth.wrapDegrees(driveable.getYaw() + seat.getRequestedAimYaw())
                : seat.getRequestedAimYaw();
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
        MouseInputHandler.resetFlightControls();
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
                    || (!mainHandGunItem.getGunItemHandler().hasEmptyAmmo(mainHandStack)
                    && offhandGunItem.getGunItemHandler().hasEmptyAmmo(offhandStack))))
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
