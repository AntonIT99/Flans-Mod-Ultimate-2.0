package com.flansmodultimate.client.input;

import org.lwjgl.glfw.GLFW;

import com.flansmod.client.model.GunAnimations;
import com.flansmod.client.model.ModelDriveable;
import com.flansmod.client.model.ModelVehicle;
import com.flansmodultimate.FlansMod;
import com.flansmodultimate.api.IControllable;
import com.flansmodultimate.client.ModClient;
import com.flansmodultimate.client.gui.GunAmmoSelectScreen;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.PlayerData;
import com.flansmodultimate.common.driveables.DriveableInput;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.PacketDriveableInput;
import com.flansmodultimate.network.server.PacketGunFireMode;
import com.flansmodultimate.network.server.PacketGunReload;
import com.flansmodultimate.network.server.PacketGunSecondaryMode;
import com.flansmodultimate.network.server.PacketGunVariableZoom;
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
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
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
    private static final String CATEGORY_DRIVEABLES = "key.categories." + FlansMod.MOD_ID + ".driveables";
    private static final String CATEGORY_VEHICLES = "key.categories." + FlansMod.MOD_ID + ".vehicles";
    private static final String CATEGORY_PLANES = "key.categories." + FlansMod.MOD_ID + ".planes";
    private static final int INPUT_KEEPALIVE_TICKS = 5;
    private static final float AIM_CHANGE_EPSILON = 0.1F;
    private static final float FLIGHT_CONTROL_EPSILON = 0.005F;

    // On foot: guns, teams and debugging.
    private static final KeyMapping reloadKey = key("reload", InputConstants.KEY_R, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping fireModeKey = key("fire_mode", InputConstants.KEY_B, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping lookAtGunKey = key("look_at_gun", InputConstants.KEY_M, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping preferredAmmoKey = key("preferred_ammo", InputConstants.KEY_P, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping secondaryModeKey = key("secondary_mode", InputConstants.KEY_K, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping increaseZoomKey = key("increase_zoom", InputConstants.KEY_UP, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping decreaseZoomKey = key("decrease_zoom", InputConstants.KEY_DOWN, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping debugKey = new KeyMapping("key." + FlansMod.MOD_ID + ".debug", KeyConflictContext.UNIVERSAL, InputConstants.Type.KEYSYM, InputConstants.KEY_F10, CATEGORY_GENERAL);
    private static final KeyMapping teamsMenuKey = key("teams_menu", InputConstants.KEY_U, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping teamsScoresKey = key("teams_scores", InputConstants.KEY_I, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);
    private static final KeyMapping teamsClassKey = key("teams_class", InputConstants.KEY_O, KeyConflictContext.IN_GAME, CATEGORY_GENERAL);

    // Every driveable: planes, ground vehicles and mechas alike.
    private static final KeyMapping driveableInventoryKey = key("driveable.inventory", InputConstants.KEY_R, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping primaryKey = mouseKey("driveable.primary", GLFW.GLFW_MOUSE_BUTTON_LEFT, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping primaryAlternativeKey = key("driveable.primary_alternative", InputConstants.UNKNOWN.getValue(), EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping secondaryKey = mouseKey("driveable.secondary", GLFW.GLFW_MOUSE_BUTTON_RIGHT, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping secondaryAlternativeKey = key("driveable.secondary_alternative", InputConstants.UNKNOWN.getValue(), EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping changeSeatKey = key("driveable.change_seat", InputConstants.KEY_NUMPAD0, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping doorKey = key("driveable.door", InputConstants.KEY_K, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);
    private static final KeyMapping flareKey = key("driveable.flare", InputConstants.KEY_X, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);

    // Ground vehicles and mechas. These default to the vanilla movement keys so
    // driving is unchanged, but they are explicit binds so the controls screen
    // shows what a tank or a mecha actually answers to.
    private static final KeyMapping driveForwardKey = key("vehicle.forward", InputConstants.KEY_W, EnumKeyConflictContext.GROUND_DRIVEABLE, CATEGORY_VEHICLES);
    private static final KeyMapping driveBackwardKey = key("vehicle.backward", InputConstants.KEY_S, EnumKeyConflictContext.GROUND_DRIVEABLE, CATEGORY_VEHICLES);
    private static final KeyMapping steerLeftKey = key("vehicle.left", InputConstants.KEY_A, EnumKeyConflictContext.GROUND_DRIVEABLE, CATEGORY_VEHICLES);
    private static final KeyMapping steerRightKey = key("vehicle.right", InputConstants.KEY_D, EnumKeyConflictContext.GROUND_DRIVEABLE, CATEGORY_VEHICLES);
    private static final KeyMapping brakeKey = key("vehicle.brake", InputConstants.KEY_SPACE, EnumKeyConflictContext.GROUND_DRIVEABLE, CATEGORY_VEHICLES);
    private static final KeyMapping decreaseVehicleThrottleKey = key("vehicle.throttle_decrease", InputConstants.KEY_Q, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);
    private static final KeyMapping increaseVehicleThrottleKey = key("vehicle.throttle_increase", InputConstants.KEY_E, EnumKeyConflictContext.VEHICLE, CATEGORY_VEHICLES);

    // Planes and helicopters. Aircraft fly on their own binds rather than the
    // vanilla movement keys, so the flight axes can be rebound without changing
    // how a player walks or drives.
    private static final KeyMapping pitchDownKey = key("plane.pitch_down", InputConstants.KEY_W, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping pitchUpKey = key("plane.pitch_up", InputConstants.KEY_S, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping yawLeftKey = key("plane.yaw_left", InputConstants.KEY_Q, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping yawRightKey = key("plane.yaw_right", InputConstants.KEY_E, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping rollLeftKey = key("plane.roll_left", InputConstants.KEY_A, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping rollRightKey = key("plane.roll_right", InputConstants.KEY_D, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping throttleUpKey = key("plane.throttle_up", InputConstants.KEY_SPACE, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping throttleDownKey = key("plane.throttle_down", InputConstants.KEY_LCONTROL, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping controlModeKey = key("plane.control_mode", InputConstants.KEY_C, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping gearKey = key("plane.gear", InputConstants.KEY_G, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping modeKey = key("plane.mode", InputConstants.KEY_J, EnumKeyConflictContext.PLANE, CATEGORY_PLANES);
    private static final KeyMapping driveablePlayerInventoryKey = key("driveable.player_inventory", InputConstants.KEY_Z, EnumKeyConflictContext.DRIVEABLE, CATEGORY_DRIVEABLES);

    /**
     * General binds that checkKeys only reads on foot, behind the gun branch.
     * Mounting a driveable takes the other branch, so these can share a key with
     * a driveable bind without either losing anything.
     */
    private static final List<KeyMapping> ON_FOOT_BINDS = List.of(reloadKey, fireModeKey, lookAtGunKey,
        preferredAmmoKey, secondaryModeKey, increaseZoomKey, decreaseZoomKey);

    private static final List<KeyMapping> DRIVEABLE_BINDS = List.of(driveableInventoryKey, driveablePlayerInventoryKey,
        primaryKey, primaryAlternativeKey, secondaryKey, secondaryAlternativeKey, changeSeatKey, doorKey, flareKey);
    /** Binds that claim their key while the player is at the controls of an aircraft. */
    private static final List<KeyMapping> AIRCRAFT_BINDS = List.of(pitchDownKey, pitchUpKey,
        yawLeftKey, yawRightKey, rollLeftKey, rollRightKey, throttleUpKey, throttleDownKey,
        controlModeKey, gearKey, modeKey,
        driveableInventoryKey, primaryKey, primaryAlternativeKey, secondaryKey, secondaryAlternativeKey,
        changeSeatKey, doorKey, flareKey, driveablePlayerInventoryKey);
    /** Binds that claim their key while the player is at the controls of anything else. */
    private static final List<KeyMapping> GROUND_BINDS = List.of(driveForwardKey, driveBackwardKey,
        steerLeftKey, steerRightKey, brakeKey, driveableInventoryKey, primaryKey, primaryAlternativeKey,
        secondaryKey, secondaryAlternativeKey, changeSeatKey, doorKey, flareKey, driveablePlayerInventoryKey);
    /** Ground binds plus the persistent throttle lever that only vehicles answer. */
    private static final List<KeyMapping> VEHICLE_BINDS = List.of(driveForwardKey, driveBackwardKey,
        steerLeftKey, steerRightKey, brakeKey, decreaseVehicleThrottleKey, increaseVehicleThrottleKey,
        driveableInventoryKey, primaryKey, primaryAlternativeKey, secondaryKey, secondaryAlternativeKey,
        changeSeatKey, doorKey, flareKey, driveablePlayerInventoryKey);
    /**
     * Binds read with consumeClick. Forge gates isDown on the conflict context
     * but not consumeClick, so a press made outside the context stays queued and
     * would fire the moment the player mounts. These are drained instead.
     */
    private static final List<KeyMapping> CLICK_BINDS = List.of(driveableInventoryKey, changeSeatKey, doorKey,
        flareKey, controlModeKey, gearKey, modeKey, driveablePlayerInventoryKey);

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
    private static List<KeyMapping> claimableVanillaActions;

    private static KeyMapping key(String name, int keyCode, IKeyConflictContext context, String category)
    {
        return new KeyMapping("key." + FlansMod.MOD_ID + "." + name, context, InputConstants.Type.KEYSYM, keyCode, category);
    }

    private static KeyMapping mouseKey(String name, int button, IKeyConflictContext context, String category)
    {
        return new KeyMapping("key." + FlansMod.MOD_ID + "." + name, context, InputConstants.Type.MOUSE, button, category);
    }

    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(reloadKey);
        event.register(fireModeKey);
        event.register(lookAtGunKey);
        event.register(preferredAmmoKey);
        event.register(secondaryModeKey);
        event.register(increaseZoomKey);
        event.register(decreaseZoomKey);
        event.register(debugKey);
        event.register(teamsMenuKey);
        event.register(teamsScoresKey);
        event.register(teamsClassKey);
        event.register(driveableInventoryKey);
        event.register(primaryKey);
        event.register(primaryAlternativeKey);
        event.register(secondaryKey);
        event.register(secondaryAlternativeKey);
        event.register(changeSeatKey);
        event.register(doorKey);
        event.register(flareKey);
        event.register(driveForwardKey);
        event.register(driveBackwardKey);
        event.register(steerLeftKey);
        event.register(steerRightKey);
        event.register(brakeKey);
        event.register(decreaseVehicleThrottleKey);
        event.register(increaseVehicleThrottleKey);
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
        event.register(driveablePlayerInventoryKey);
    }

    /**
     * Gives the player's own inventory a way back while controlling any
     * driveable, since its controls take over the vanilla key. Mirrors what
     * {@code Minecraft.handleKeybinds} does, because that is exactly the
     * handling claimConflictingVanillaKeys suppresses. The other claimed
     * actions get no stand-in: none of them is worth a key while driving.
     */
    private static void handleDriveableShortcuts(Minecraft mc, LocalPlayer player)
    {
        while (driveablePlayerInventoryKey.consumeClick())
        {
            if (mc.gameMode != null && mc.gameMode.isServerControlledInventory())
            {
                player.sendOpenInventory();
            }
            else
            {
                mc.getTutorial().onOpenInventory();
                mc.setScreen(new InventoryScreen(player));
            }
        }
    }

    /** True for a bind that this router stops reading as soon as the player mounts. */
    public static boolean isOnFootOnly(KeyMapping mapping)
    {
        return ON_FOOT_BINDS.contains(mapping);
    }

    /**
     * Drops presses queued by binds that are not live for the driveable the
     * player is on right now, so tapping a plane key on foot does not fire the
     * moment they climb in.
     */
    private static void discardInactiveClicks()
    {
        for (KeyMapping bind : CLICK_BINDS)
        {
            if (!bind.isConflictContextAndModifierActive())
            {
                while (bind.consumeClick()) { /* never applied, so never queued */ }
            }
        }
    }

    /**
     * Claims keys shared with vanilla before {@code Minecraft.handleKeybinds}
     * gets to consume them. Drop and player inventory are always reserved while
     * driving, freeing Q and E for every driveable control scheme; the other
     * actions are claimed only when a live driveable bind uses their key.
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

        List<KeyMapping> claimed = driveable instanceof Plane ? AIRCRAFT_BINDS
            : driveable instanceof Vehicle ? VEHICLE_BINDS : GROUND_BINDS;
        for (KeyMapping vanilla : claimableVanillaActions())
        {
            boolean alwaysReserved = vanilla == mc.options.keyInventory || vanilla == mc.options.keyDrop;
            if (vanilla.isUnbound() || !alwaysReserved && !isClaimedBy(vanilla, claimed))
                continue;
            // consumeClick drains one queued press at a time; the hotbar
            // activators are read as a held key instead, so clear that too.
            while (vanilla.consumeClick()) { /* claimed by the driveable bind */ }
            vanilla.setDown(false);
        }
    }

    /**
     * Vanilla actions that a live driveable bind takes over. Everything here is
     * switched off while the player is at the controls, which is also why the
     * controls screen has nothing left to warn about for these keys.
     */
    private static List<KeyMapping> claimableVanillaActions()
    {
        if (claimableVanillaActions == null)
        {
            Options options = Minecraft.getInstance().options;
            claimableVanillaActions = List.of(options.keyInventory, options.keyDrop,
                options.keySwapOffhand, options.keyAdvancements, options.keySocialInteractions,
                options.keySaveHotbarActivator, options.keyLoadHotbarActivator);
        }
        return claimableVanillaActions;
    }

    /** True for a vanilla action a driveable bind is allowed to take over. */
    public static boolean isClaimableVanillaAction(KeyMapping mapping)
    {
        return claimableVanillaActions().contains(mapping);
    }

    private static boolean isClaimedBy(KeyMapping vanilla, List<KeyMapping> claimed)
    {
        for (KeyMapping bind : claimed)
        {
            // Compare the bound key directly. KeyMapping.same answers a
            // different question (should the options screen call this a
            // conflict) and is filtered by KeyMappingConflictMixin.
            if (!bind.isUnbound() && vanilla.getKey().equals(bind.getKey()))
                return true;
        }
        return false;
    }

    public static void checkKeys()
    {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        boolean noScreen = mc.screen == null;

        discardInactiveClicks();
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
                if (preferredAmmoKey.consumeClick())
                {
                    doOpenPreferredAmmoScreen();
                    return;
                }
                if (secondaryModeKey.consumeClick())
                {
                    doToggleSecondaryMode();
                    return;
                }
                if (increaseZoomKey.consumeClick())
                {
                    doChangeVariableZoom(true);
                    return;
                }
                if (decreaseZoomKey.consumeClick())
                {
                    doChangeVariableZoom(false);
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
            handleDriveableShortcuts(mc, player);
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
                // A vehicle reads these as throttle, steering and brake; a mecha
                // reads the same flags as walking, strafing and jumping.
                if (driveForwardKey.isDown()) mask |= DriveableInput.FORWARD;
                if (driveBackwardKey.isDown()) mask |= DriveableInput.BACKWARD;
                if (steerLeftKey.isDown()) mask |= DriveableInput.LEFT;
                if (steerRightKey.isDown()) mask |= DriveableInput.RIGHT;
                if (brakeKey.isDown()) mask |= DriveableInput.ASCEND;
                if (driveable instanceof Vehicle)
                {
                    if (increaseVehicleThrottleKey.isDown()) mask |= DriveableInput.THROTTLE_INCREASE;
                    if (decreaseVehicleThrottleKey.isDown()) mask |= DriveableInput.THROTTLE_DECREASE;
                }
            }

            if (primaryKey.isDown() || primaryAlternativeKey.isDown())
                mask |= DriveableInput.PRIMARY_FIRE;
            if (secondaryKey.isDown() || secondaryAlternativeKey.isDown())
                mask |= DriveableInput.SECONDARY_FIRE;

            boolean sneaking = mc.options.keyShift.isDown();
            if (sneaking && !wasSneaking)
                edgeMask |= DriveableInput.EXIT;
            wasSneaking = sneaking;

            if (driveableInventoryKey.consumeClick())
                edgeMask |= DriveableInput.MENU;
            if (changeSeatKey.consumeClick()) edgeMask |= DriveableInput.CHANGE_SEAT;
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

    private static void doOpenPreferredAmmoScreen()
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        InteractionHand hand = findGunHand(player, (gunItem, stack) -> !gunItem.getConfigType().getAmmoTypes().isEmpty());
        if (hand != null && PlayerData.getInstance(player, LogicalSide.CLIENT).getShootTime(hand) <= 0F)
            Minecraft.getInstance().setScreen(new GunAmmoSelectScreen(hand));
    }

    private static void doToggleSecondaryMode()
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        InteractionHand hand = findGunHand(player,
            (gunItem, stack) -> gunItem.getConfigType().canToggleSecondaryFire(stack));
        if (hand != null && PlayerData.getInstance(player, LogicalSide.CLIENT).getShootTime(hand) <= 0F)
            PacketHandler.sendToServer(new PacketGunSecondaryMode(hand));
    }

    private static void doChangeVariableZoom(boolean increase)
    {
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof GunItem gunItem && gunItem.hasVariableZoom(stack))
            PacketHandler.sendToServer(new PacketGunVariableZoom(InteractionHand.MAIN_HAND, increase));
    }

    @Nullable
    private static InteractionHand findGunHand(Player player, java.util.function.BiPredicate<GunItem, ItemStack> predicate)
    {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof GunItem gunItem && predicate.test(gunItem, main))
            return InteractionHand.MAIN_HAND;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof GunItem gunItem && predicate.test(gunItem, off))
            return InteractionHand.OFF_HAND;
        return null;
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
