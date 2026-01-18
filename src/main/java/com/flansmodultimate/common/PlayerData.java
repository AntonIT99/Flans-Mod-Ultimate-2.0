package com.flansmodultimate.common;

import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.guns.reload.GunReloader;
import com.flansmodultimate.common.guns.reload.PendingReload;
import com.flansmodultimate.common.raytracing.PlayerSnapshot;
import com.flansmodultimate.common.raytracing.RotatedAxes;
import com.flansmodultimate.common.teams.Team;
import com.flansmodultimate.common.types.GunType;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class PlayerData
{
    private static final Map<UUID, PlayerData> serverSideData = new HashMap<>();
    private static final Map<UUID, PlayerData> clientSideData = new HashMap<>();

    // Input related fields
    private boolean isShootKeyPressedRight;
    private boolean isPrevShootKeyPressedRight;
    private boolean isShootKeyPressedLeft;
    private boolean isPrevShootKeyPressedLeft;
    @Getter @Setter
    private boolean isSecondaryFunctionKeyPressed;

    /** Snapshots for bullet hit detection. Array size is set to number of snapshots required. When a new one is taken,
     * each snapshot is moved along one place and new one is added at the start, so that when the array fills up, the oldest one is lost */
    @Getter
    private PlayerSnapshot[] snapshots;

    // Gun-related fields
    /** The MG this player is using */
    @Getter @Setter
    private DeployedGun mountingGun;
    /** Tickers to stop shooting too fast */
    @Getter @Setter
    private float shootTimeRight;
    @Getter @Setter
    private float shootTimeLeft;
    /** Stops player shooting immediately after swapping weapons */
    private int shootClickDelay; //TODO: implement shootClick
    /** True if this player is shooting */
    private boolean isShootingRight;
    private boolean isShootingLeft;
    /** The speed of the minigun the player is using */
    @Getter @Setter
    private float minigunSpeed;
    /** Reloading booleans */
    private boolean isReloadingRight;
    private boolean isReloadingLeft;
    /** When remote explosives are thrown they are added to this list. When the player uses a remote, the first one from this list detonates */
    @Getter
    private final List<Grenade> remoteExplosives = new ArrayList<>(); //TODO: add Tools to detonate remote explosives
    /** Sound delay parameters */
    @Getter @Setter
    private int loopedSoundDelay;
    /** Melee weapon custom hit simulation */
    @Getter @Setter
    private int meleeProgress;
    @Getter @Setter
    private int meleeLength;
    /** When the player shoots a burst fire weapon, one shot is fired immediately and this counter keeps track of how many more should be fired */
    private int burstRoundsRemainingLeft = 0; //TODO: implement burst
    @Getter @Setter
    private int burstRoundsRemainingRight = 0;
    private PendingReload pendingReload;
    private boolean reloadedAfterRespawn; //TODO: implement
    @Getter
    private Vector3f[] lastMeleePositions;

    //TODO: implement Teams
    //Teams related fields
    /** Gametype variables */
    private int score, kills, deaths;
    /** Zombies variables */
    private int zombieScore;
    /** Gametype variable for Nerf */
    private boolean out;
    /** The player's vote for the next round from 1 ~ 5. 0 is not yet voted */
    private int vote;
    /** The team this player is currently on */
    @Getter
    private Team team;
    /** The team this player will switch to upon respawning */
    private Team newTeam;
    /** The class the player is currently using */
    //private PlayerClass playerClass;
    /** The class the player will switch to upon respawning */
    //private PlayerClass newPlayerClass;
    /** Keeps the player out of having to rechose their team each round */
    private boolean builder;
    /** e.e */
    private boolean playerMovedByAutobalancer;
    /** Save the player's skin here, to replace after having done a swap for a certain class override */
    @OnlyIn(Dist.CLIENT)
    private ResourceLocation skin;

    private PlayerData(UUID id)
    {
        snapshots = new PlayerSnapshot[PlayerSnapshot.NUM_PLAYER_SNAPSHOTS];
    }

    @NotNull
    public static PlayerData getInstance(@NotNull Player player)
    {
        return getInstance(player.getUUID(), player.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);
    }

    @NotNull
    public static PlayerData getInstance(UUID playerId)
    {
        return getInstance(playerId, LogicalSide.SERVER);
    }

    @NotNull
    public static PlayerData getInstance(@NotNull Player player, LogicalSide side)
    {
        return getInstance(player.getUUID(), side);
    }

    @NotNull
    public static PlayerData getInstance(UUID playerId, LogicalSide side)
    {
        if (side.isClient())
            return clientSideData.computeIfAbsent(playerId, PlayerData::new);
        else
            return serverSideData.computeIfAbsent(playerId, PlayerData::new);
    }

    public boolean isShootKeyPressed(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? isShootKeyPressedLeft : isShootKeyPressedRight;
    }

    public void setShootKeyPressed(InteractionHand hand, boolean value)
    {
        if (hand == InteractionHand.OFF_HAND)
            isShootKeyPressedLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            isShootKeyPressedRight = value;
    }

    public boolean isPrevShootKeyPressed(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? isPrevShootKeyPressedLeft : isPrevShootKeyPressedRight;
    }

    public void setPrevShootKeyPressed(InteractionHand hand, boolean value)
    {
        if (hand == InteractionHand.OFF_HAND)
            isPrevShootKeyPressedLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            isPrevShootKeyPressedRight = value;
    }

    public float getShootTime(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? shootTimeLeft : shootTimeRight;
    }

    public void setShootTime(InteractionHand hand, float value)
    {
        if (hand == InteractionHand.OFF_HAND)
            shootTimeLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            shootTimeRight = value;
    }

    public boolean isReloading(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? isReloadingLeft : isReloadingRight;
    }

    public void setReloading(InteractionHand hand, boolean value)
    {
        if (hand == InteractionHand.OFF_HAND)
            isReloadingLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            isReloadingRight = value;
    }

    public boolean isShooting(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? isShootingLeft : isShootingRight;
    }

    public void setShooting(InteractionHand hand, boolean value)
    {
        if (hand == InteractionHand.OFF_HAND)
            isShootingLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            isShootingRight = value;
    }

    public int getBurstRoundsRemaining(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? burstRoundsRemainingLeft : burstRoundsRemainingRight;
    }

    public void setBurstRoundsRemaining(InteractionHand hand, int value)
    {
        if (hand == InteractionHand.OFF_HAND)
            burstRoundsRemainingLeft = value;
        else if (hand == InteractionHand.MAIN_HAND)
            burstRoundsRemainingRight = value;
    }

    public void tick(Player player)
    {
        if (shootTimeRight > 0)
            shootTimeRight--;
        if (shootTimeRight == 0)
            isReloadingRight = false;

        if (shootTimeLeft > 0)
            shootTimeLeft--;
        if (shootTimeLeft == 0)
            isReloadingLeft = false;

        if (shootClickDelay > 0)
            shootClickDelay--;

        // Slow down minigun each tick
        minigunSpeed *= 0.9F;

        if (loopedSoundDelay > 0)
            loopedSoundDelay--;

        if (!player.level().isClientSide)
            GunReloader.handlePendingReload(player.level(), (ServerPlayer) player, this);

        //Move all snapshots along one place
        System.arraycopy(snapshots, 0, snapshots, 1, snapshots.length - 2 + 1);
        //Take new snapshot
        snapshots[0] = new PlayerSnapshot(player);
    }

    public Optional<PendingReload> getPendingReload()
    {
        return Optional.ofNullable(pendingReload);
    }

    public void clearPendingReload()
    {
        pendingReload = null;
    }

    public boolean queuePendingReload(PendingReload pr)
    {
        if (pendingReload != null)
            return false;

        pendingReload = pr;
        return true;
    }

    public void resetScore()
    {
        score = zombieScore = kills = deaths = 0;
        team = newTeam = null;
        //TODO: Uncomment for Teams
        //playerClass = newPlayerClass = null;*/
    }

    public void playerKilled()
    {
        mountingGun = null;
        isShootingRight = isShootingLeft = false;
        snapshots = new PlayerSnapshot[PlayerSnapshot.NUM_PLAYER_SNAPSHOTS];
    }

    public void doGunReload(InteractionHand hand, float reloadTime)
    {
        // Set player shoot delay to be the reload delay - Set both gun delays to avoid reloading two guns at once
        shootTimeRight = reloadTime;
        shootTimeLeft = reloadTime;
        setReloading(hand, true);
        setBurstRoundsRemaining(hand,0);
    }

    public void doMelee(Player player, int meleeTime, GunType type)
    {
        meleeLength = meleeTime;
        lastMeleePositions = new Vector3f[type.getMeleePath().size()];

        for(int k = 0; k < type.getMeleeDamagePoints().size(); k++)
        {
            Vector3f meleeDamagePoint = type.getMeleeDamagePoints().get(k);
            //Do a raytrace from the prev pos to the current pos and attack anything in the way
            Vector3f nextPos = type.getMeleePath().get(0);
            Vector3f nextAngles = type.getMeleePathAngles().get(0);
            RotatedAxes nextAxes = new RotatedAxes(-nextAngles.y, -nextAngles.z, nextAngles.x);

            Vector3f nextPosInPlayerCoords = new RotatedAxes(player.getYRot() + 90F, player.getXRot(), 0F).findLocalVectorGlobally(nextAxes.findLocalVectorGlobally(meleeDamagePoint));
            Vector3f.add(nextPos, nextPosInPlayerCoords, nextPosInPlayerCoords);

            lastMeleePositions[k] = new Vector3f(player.getX() + nextPosInPlayerCoords.x, player.getEyeY() + nextPosInPlayerCoords.y, player.getZ() + nextPosInPlayerCoords.z);
        }
    }

    //TODO: Events from PlayerHandler
}
