package com.wolffsarmormod.common;

import com.flansmod.common.vector.Vector3f;
import com.wolffsarmormod.ModUtils;
import com.wolffsarmormod.common.entity.DeployedGun;
import com.wolffsarmormod.common.entity.Grenade;
import com.wolffsarmormod.common.guns.QueuedReload;
import com.wolffsarmormod.common.raytracing.PlayerSnapshot;
import com.wolffsarmormod.common.raytracing.RotatedAxes;
import com.wolffsarmormod.common.types.GunType;
import lombok.Getter;
import lombok.Setter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import org.jetbrains.annotations.NotNull;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData
{
    public static final Map<UUID, PlayerData> serverSideData = new HashMap<>();
    public static final Map<UUID, PlayerData> clientSideData = new HashMap<>();

    /** Their UUID */
    private UUID playerId;

    //Movement related fields
    /** Roll variables */
    private float prevRotationRoll;
    private float rotationRoll;
    /** Snapshots for bullet hit detection. Array size is set to number of snapshots required. When a new one is taken,
     * each snapshot is moved along one place and new one is added at the start, so that when the array fills up, the oldest one is lost */
    @Getter
    private PlayerSnapshot[] snapshots;

    //Gun-related fields
    /** The slotID of the gun being used by the off-hand. 0 = no slot. 1 ~ 9 = hotbar slots */
    private int offHandGunSlot = 0;
    /** The off hand gun stack. For viewing other player's off hand weapons only (since you don't know what is in their inventory and hence just the ID is insufficient) */
    @OnlyIn(Dist.CLIENT)
    private ItemStack offHandGunStack;
    /** The MG this player is using */
    private DeployedGun mountingGun;
    /** Tickers to stop shooting too fast */
    @Getter @Setter
    private float shootTimeRight;
    @Getter @Setter
    private float shootTimeLeft;
    /** Stops player shooting immediately after swapping weapons */
    private int shootClickDelay;
    /** True if this player is shooting */
    @Getter @Setter
    private boolean isShootingRight;
    @Getter @Setter
    private boolean isShootingLeft;
    /** The speed of the minigun the player is using */
    @Getter @Setter
    private float minigunSpeed = 0F;
    /** Reloading booleans */
    @Getter @Setter
    private boolean reloadingRight;
    @Getter @Setter
    private boolean reloadingLeft;
    /** When remote explosives are thrown they are added to this list. When the player uses a remote, the first one from this list detonates */
    private ArrayList<Grenade> remoteExplosives = new ArrayList<>();
    /** Sound delay parameters */
    @Getter @Setter
    private int loopedSoundDelay;
    /** Sound delay parameters */
    private boolean shouldPlayCooldownSound;
    private boolean shouldPlayWarmupSound;
    /** Sound delay parameters */
    @Getter @Setter
    private boolean isSpinning;
    /** Melee weapon custom hit simulation */
    private int meleeProgress, meleeLength;
    /** When the player shoots a burst fire weapon, one shot is fired immediately and this counter keeps track of how many more should be fired */
    private int burstRoundsRemainingLeft = 0;
    @Getter @Setter
    private int burstRoundsRemainingRight = 0;

    private ItemStack gunToReload;
    private int reloadSlot;
    private QueuedReload queuedReload;

    private boolean isAmmoEmpty;
    private boolean reloadedAfterRespawn = false;

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
    //private Team team;
    /** The team this player will switch to upon respawning */
    //private Team newTeam;
    /** The class the player is currently using */
    //private PlayerClass playerClass;
    /** The class the player will switch to upon respawning */
    //private PlayerClass newPlayerClass;
    /** Keeps the player out of having to rechose their team each round */
    private boolean builder;
    /** e.e */
    private boolean playerMovedByAutobalancer = false;
    /** Save the player's skin here, to replace after having done a swap for a certain class override */
    @OnlyIn(Dist.CLIENT)
    private ResourceLocation skin;

    private PlayerData(UUID id)
    {
        playerId = id;
        snapshots = new PlayerSnapshot[PlayerSnapshot.numPlayerSnapshots];
    }

    public static PlayerData getInstance(@NotNull Player player)
    {
        return getInstance(player.getUUID(), player.level().isClientSide ? LogicalSide.CLIENT : LogicalSide.SERVER);
    }

    public static PlayerData getInstance(UUID playerId)
    {
        return getInstance(playerId, LogicalSide.SERVER);
    }

    public static PlayerData getInstance(@NotNull Player player, LogicalSide side)
    {
        return getInstance(player.getUUID(), side);
    }

    public static PlayerData getInstance(UUID playerId, LogicalSide side)
    {
        if (side.isClient())
            return clientSideData.computeIfAbsent(playerId, PlayerData::new);
        else
            return serverSideData.computeIfAbsent(playerId, PlayerData::new);
    }

    public void serverTick(Player player)
    {
        if(shootTimeRight > 0)
            shootTimeRight--;
        if(shootTimeRight == 0)
            reloadingRight = false;

        if(shootTimeLeft > 0)
            shootTimeLeft--;
        if(shootTimeLeft == 0)
            reloadingLeft = false;

        if(shootClickDelay > 0)
            shootClickDelay--;

        //Move all snapshots along one place
        System.arraycopy(snapshots, 0, snapshots, 1, snapshots.length - 2 + 1);
        //Take new snapshot
        snapshots[0] = new PlayerSnapshot(player);
    }

    @OnlyIn(Dist.CLIENT)
    public void clientTick(Player player)
    {
        serverTick(player);

        if (loopedSoundDelay > 0)
            loopedSoundDelay--;
    }

    public void resetScore()
    {
        score = zombieScore = kills = deaths = 0;
        //TODO: Uncomment for Teams
        /*team = newTeam = null;
        playerClass = newPlayerClass = null;*/
    }

    public void playerKilled()
    {
        mountingGun = null;
        isShootingRight = isShootingLeft = false;
        snapshots = new PlayerSnapshot[PlayerSnapshot.numPlayerSnapshots];
    }

    public float getShootTime(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? shootTimeLeft : shootTimeRight;
    }

    public void setShootTime(InteractionHand hand, float set)
    {
        if (hand == InteractionHand.OFF_HAND)
            shootTimeLeft = set;
        else
            shootTimeRight = set;
    }

    public int getBurstRoundsRemaining(InteractionHand hand)
    {
        return hand == InteractionHand.OFF_HAND ? burstRoundsRemainingLeft : burstRoundsRemainingRight;
    }

    public void setBurstRoundsRemaining(InteractionHand hand, int set)
    {
        if (hand == InteractionHand.OFF_HAND)
            burstRoundsRemainingLeft = set;
        else
            burstRoundsRemainingRight = set;
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

            if(!ModUtils.isThePlayer(player))
                nextPosInPlayerCoords.y += 1.6F;

            lastMeleePositions[k] = new Vector3f(player.getX() + nextPosInPlayerCoords.x, player.getY() + nextPosInPlayerCoords.y, player.getZ() + nextPosInPlayerCoords.z);
        }
    }

    //TODO: Events from PlayerHandler
}
