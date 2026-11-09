package com.wolffsmod.npcs.teams;

import lombok.Getter;
import lombok.Setter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** A squad's reusable ride. Entity passengers own their equipment and health; this stores only the assignment. */
@Getter
public final class SquadTransport
{
    public enum Phase
    {
        BOARDING, EMBARKED, UNLOADING
    }

    private static final String KEY_VEHICLE = "vehicle";
    private static final String KEY_PLAYER = "player";

    private final UUID vehicle;
    private final UUID player;
    /** Original entity ids, rather than slots: replacements never inherit a dead soldier's reservation. */
    private final Set<UUID> soldiers;
    @Setter
    private Phase phase = Phase.BOARDING;
    @Setter
    private long deadline;
    @Setter
    private Vec3 lastPosition;
    @Setter
    private long lastSeen;
    /** Rebuilt from live seats, never saved. */
    private final Map<UUID, Integer> reservations = new HashMap<>();

    public SquadTransport(UUID vehicle, UUID player, Collection<UUID> soldiers, Vec3 position, long now)
    {
        this.vehicle = vehicle;
        this.player = player;
        this.soldiers = new LinkedHashSet<>(soldiers);
        lastPosition = position;
        lastSeen = now;
        deadline = now + 60L * 20L;
    }

    /** Ends pickup without ejecting anyone into a moving vehicle's path. */
    public void unload()
    {
        phase = Phase.UNLOADING;
        reservations.clear();
    }

    CompoundTag save()
    {
        CompoundTag tag = new CompoundTag();
        tag.putUUID(KEY_VEHICLE, vehicle);
        tag.putUUID(KEY_PLAYER, player);
        CompoundTag members = new CompoundTag();
        soldiers.forEach(id -> members.putBoolean(id.toString(), true));
        tag.put("soldiers", members);
        tag.putString("phase", phase.name());
        tag.putLong("deadline", deadline);
        tag.putLong("last_seen", lastSeen);
        tag.putDouble("x", lastPosition.x);
        tag.putDouble("y", lastPosition.y);
        tag.putDouble("z", lastPosition.z);
        return tag;
    }

    static Optional<SquadTransport> load(CompoundTag tag)
    {
        if (!tag.hasUUID(KEY_VEHICLE) || !tag.hasUUID(KEY_PLAYER))
            return Optional.empty();
        Set<UUID> soldiers = new LinkedHashSet<>();
        for (String key : tag.getCompound("soldiers").getAllKeys())
        {
            try
            {
                soldiers.add(UUID.fromString(key));
            }
            catch (IllegalArgumentException ignored)
            {
                // An invalid member does not invalidate the remaining passengers.
            }
        }
        Vec3 position = new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"));
        if (soldiers.isEmpty() || !Double.isFinite(position.lengthSqr()))
            return Optional.empty();
        SquadTransport state = new SquadTransport(tag.getUUID(KEY_VEHICLE), tag.getUUID(KEY_PLAYER), soldiers, position, tag.getLong("last_seen"));
        state.phase = BotSettings.parse(Phase.class, tag.getString("phase"), Phase.UNLOADING);
        state.deadline = tag.getLong("deadline");
        return Optional.of(state);
    }
}
