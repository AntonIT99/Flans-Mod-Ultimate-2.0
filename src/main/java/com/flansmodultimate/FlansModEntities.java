package com.flansmodultimate;

import com.flansmodultimate.common.entity.AAGun;
import com.flansmodultimate.common.entity.Bullet;
import com.flansmodultimate.common.entity.DeployedGun;
import com.flansmodultimate.common.entity.Flag;
import com.flansmodultimate.common.entity.Flagpole;
import com.flansmodultimate.common.entity.Grenade;
import com.flansmodultimate.common.entity.GunItemEntity;
import com.flansmodultimate.common.entity.Mecha;
import com.flansmodultimate.common.entity.Parachute;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Seat;
import com.flansmodultimate.common.entity.Shootable;
import com.flansmodultimate.common.entity.TeamItemEntity;
import com.flansmodultimate.common.entity.ThrownGun;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.entity.Wheel;
import com.flansmodultimate.config.ModCommonConfig;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModEntities
{
    /**
     * Seats and wheels snap to their parent on both sides every tick, so their own movement
     * packets carry nothing a client uses. Synced data such as seat aim is still sent as soon
     * as it changes, independently of this interval.
     */
    private static final int PROXY_UPDATE_INTERVAL = 20;
    public static final Supplier<? extends EntityType<Bullet>> bulletEntity = FlansModRegistries.entityRegistry.register("bullet", () -> EntityType.Builder.<Bullet>of(Bullet::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.bulletRegistrationTrackingRange())
        .updateInterval(20)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "bullet").toString())
    );
    public static final Supplier<? extends EntityType<Grenade>> grenadeEntity = FlansModRegistries.entityRegistry.register("grenade", () -> EntityType.Builder.<Grenade>of(Grenade::new, MobCategory.MISC)
        .sized(Shootable.DEFAULT_HITBOX_SIZE, Shootable.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.grenadeRegistrationTrackingRange())
        .updateInterval(20)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "grenade").toString())
    );
    public static final Supplier<? extends EntityType<ThrownGun>> thrownGunEntity = FlansModRegistries.entityRegistry.register("thrown_gun", () -> EntityType.Builder.<ThrownGun>of(ThrownGun::new, MobCategory.MISC)
        .sized(0.5F, 0.5F)
        .clientTrackingRange(4)
        .updateInterval(20)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "thrown_gun").toString())
    );
    public static final Supplier<? extends EntityType<DeployedGun>> deployedGunEntity = FlansModRegistries.entityRegistry.register("deployed_gun", () -> EntityType.Builder.<DeployedGun>of(DeployedGun::new, MobCategory.MISC)
        .sized(DeployedGun.DEFAULT_HITBOX_SIZE, DeployedGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.deployedGunRegistrationTrackingRange())
        .updateInterval(5)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "deployed_gun").toString())
    );
    public static final Supplier<? extends EntityType<GunItemEntity>> gunItemEntity = FlansModRegistries.entityRegistry.register("gun_item", () -> EntityType.Builder.<GunItemEntity>of(GunItemEntity::new, MobCategory.MISC)
        .sized(1F, 1F)
        .clientTrackingRange(16)
        .updateInterval(20)
        .build("gun_item")
    );
    public static final Supplier<? extends EntityType<TeamItemEntity>> teamItemEntity = FlansModRegistries.entityRegistry.register("team_item", () -> EntityType.Builder.<TeamItemEntity>of(TeamItemEntity::new, MobCategory.MISC)
        .sized(0.25F, 0.25F)
        .clientTrackingRange(6)
        .updateInterval(20)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "team_item").toString())
    );
    public static final Supplier<? extends EntityType<AAGun>> aaGunEntity = FlansModRegistries.entityRegistry.register("aa_gun", () -> EntityType.Builder.<AAGun>of(AAGun::new, MobCategory.MISC)
        .sized(AAGun.DEFAULT_HITBOX_SIZE, AAGun.DEFAULT_HITBOX_SIZE)
        .clientTrackingRange(ModCommonConfig.aaGunRegistrationTrackingRange())
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "aa_gun").toString())
    );
    public static final Supplier<? extends EntityType<Parachute>> parachuteEntity = FlansModRegistries.entityRegistry.register("parachute", () -> EntityType.Builder.<Parachute>of(Parachute::new, MobCategory.MISC)
        .sized(Parachute.DEFAULT_HITBOX_WIDTH, Parachute.DEFAULT_HITBOX_HEIGHT)
        .clientTrackingRange(64)
        .updateInterval(2)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "parachute").toString())
    );
    public static final Supplier<? extends EntityType<Plane>> planeEntity = FlansModRegistries.entityRegistry.register("plane", () -> EntityType.Builder.<Plane>of(Plane::new, MobCategory.MISC)
        .sized(3F, 2F)
        .clientTrackingRange(32)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "plane").toString())
    );
    public static final Supplier<? extends EntityType<Vehicle>> vehicleEntity = FlansModRegistries.entityRegistry.register("vehicle", () -> EntityType.Builder.<Vehicle>of(Vehicle::new, MobCategory.MISC)
        .sized(2.5F, 2F)
        .clientTrackingRange(32)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "vehicle").toString())
    );
    public static final Supplier<? extends EntityType<Mecha>> mechaEntity = FlansModRegistries.entityRegistry.register("mecha", () -> EntityType.Builder.<Mecha>of(Mecha::new, MobCategory.MISC)
        .sized(2F, 4F)
        .clientTrackingRange(32)
        .updateInterval(1)
        .setShouldReceiveVelocityUpdates(true)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "mecha").toString())
    );
    public static final Supplier<? extends EntityType<Seat>> seatEntity = FlansModRegistries.entityRegistry.register("driveable_seat", () -> EntityType.Builder.<Seat>of(Seat::new, MobCategory.MISC)
        .sized(0.6F, 0.6F)
        .clientTrackingRange(32)
        .updateInterval(PROXY_UPDATE_INTERVAL)
        .setShouldReceiveVelocityUpdates(false)
        .noSave()
        .noSummon()
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "driveable_seat").toString())
    );
    public static final Supplier<? extends EntityType<Wheel>> wheelEntity = FlansModRegistries.entityRegistry.register("driveable_wheel", () -> EntityType.Builder.<Wheel>of(Wheel::new, MobCategory.MISC)
        .sized(0.75F, 0.75F)
        .clientTrackingRange(32)
        .updateInterval(PROXY_UPDATE_INTERVAL)
        .setShouldReceiveVelocityUpdates(false)
        .noSave()
        .noSummon()
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "driveable_wheel").toString())
    );
    public static final Supplier<? extends EntityType<Flagpole>> flagpoleEntity = FlansModRegistries.entityRegistry.register("flagpole", () -> EntityType.Builder.<Flagpole>of(Flagpole::new, MobCategory.MISC)
        .sized(0.75F, 2.5F).clientTrackingRange(64).updateInterval(10)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "flagpole").toString()));
    public static final Supplier<? extends EntityType<Flag>> flagEntity = FlansModRegistries.entityRegistry.register("flag", () -> EntityType.Builder.<Flag>of(Flag::new, MobCategory.MISC)
        .sized(0.75F, 0.75F).clientTrackingRange(64).updateInterval(2)
        .build(ResourceLocation.fromNamespaceAndPath(FlansMod.MOD_ID, "flag").toString()));

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
