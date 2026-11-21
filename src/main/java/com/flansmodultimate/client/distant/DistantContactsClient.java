package com.flansmodultimate.client.distant;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.api.driveables.MountAim;
import com.flansmodultimate.api.parts.PartBodyPose;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.model.ModelPreloader;
import com.flansmodultimate.client.render.entity.DriveableImpostorCache;
import com.flansmodultimate.client.render.entity.DriveableRenderer;
import com.flansmodultimate.client.render.thermal.ThermalVision;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.parts.PartBodies;
import com.flansmodultimate.common.driveables.parts.PartBody;
import com.flansmodultimate.common.entity.Driveable;
import com.flansmodultimate.common.entity.Plane;
import com.flansmodultimate.common.entity.Vehicle;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PlaneType;
import com.flansmodultimate.common.types.VehicleType;
import com.flansmodultimate.network.client.driveable.PacketDistantContacts;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Driveables shown as simplified shapes because they are too far away to be drawn as entities: those the
 * client tracks but no longer draws, and those only the server reports, beyond the tracking range. An entity
 * that wears the part boxes of a driveable type, as an NPC drawn as one does, is shown the same way. Each is
 * drawn by the far-terrain renderer beyond the vanilla chunks and by {@link DistantBoxRenderer} within them.
 *
 * <p>
 * Planes and vehicles are drawn as impostors of their models once the atlas of {@link DriveableImpostorCache}
 * holds a view of them: by {@link DistantImpostorPass} into the far terrain, and by {@link DistantBoxRenderer}
 * within the vanilla chunks. Until then, and for mechas and the entities standing in for driveables, which the
 * atlas cannot picture in their pose, they are drawn as boxes. Render thread.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DistantContactsClient
{
    /** How driveables look in a thermal sight: their engines and hulls are the hottest things around. */
    static final int THERMAL_COLOR = 0xFFF2F2F2;
    /** Ticks a reported contact outlives the update that should have replaced it. */
    private static final int STALE_MARGIN_TICKS = 20;

    private record Reported(PacketDistantContacts.Contact contact, long receivedTick)
    {}

    private static final Map<Integer, Reported> reported = new HashMap<>();
    private static final Map<Integer, Proxy> proxies = new HashMap<>();
    private static int updateInterval = 5;
    private static long ticks;

    /** Replaces the reported contacts with a new update from the server. */
    public static void accept(PacketDistantContacts packet)
    {
        updateInterval = Math.max(1, packet.getUpdateInterval());
        reported.clear();
        if (!DistantHorizonsClient.contactsEnabled())
            return;
        for (PacketDistantContacts.Contact contact : packet.getContacts())
            reported.put(contact.entityId(), new Reported(contact, ticks));
    }

    static void tick(ClientLevel level, Vec3 camera, IDistantTerrain terrain, boolean enabled, double handoff)
    {
        ticks++;
        if (!enabled)
        {
            reset();
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Entity ownVehicle = minecraft.player == null ? null : minecraft.player.getRootVehicle();
        Set<Integer> live = new HashSet<>();

        // Tracked, but too far away to be drawn as entities
        for (Entity entity : level.entitiesForRendering())
        {
            if (entity == ownVehicle || !entity.isAlive() || entity.shouldRender(camera.x, camera.y, camera.z))
                continue;
            if (entity instanceof Driveable driveable)
            {
                if (driveable.isWreck() || driveable.getConfigType() == null)
                    continue;
                proxies.computeIfAbsent(driveable.getId(), Proxy::new).follow(driveable);
                live.add(driveable.getId());
                continue;
            }
            // An entity standing in for a driveable, as an NPC drawn as one, is shown by the part boxes it wears
            PartBody body = PartBodies.of(entity);
            if (body != null && body.getType() instanceof DriveableType wornType)
            {
                proxies.computeIfAbsent(entity.getId(), Proxy::new).follow(entity, body, wornType);
                live.add(entity.getId());
            }
        }

        // Only reported by the server; one the client tracks is drawn by the loop above or as itself
        long staleAfter = updateInterval * 2L + STALE_MARGIN_TICKS;
        for (Reported entry : reported.values())
        {
            int id = entry.contact().entityId();
            if (ticks - entry.receivedTick() > staleAfter || live.contains(id) || level.getEntity(id) != null)
                continue;
            if (!(InfoType.getInfoType(entry.contact().shortName()) instanceof DriveableType type))
                continue;
            proxies.computeIfAbsent(id, Proxy::new).follow(entry, type);
            live.add(id);
        }

        proxies.values().removeIf(proxy ->
        {
            if (live.contains(proxy.id))
                return false;
            proxy.close();
            return true;
        });

        boolean thermal = ThermalVision.active();
        boolean impostors = DistantHorizonsClient.impostorsEnabled();
        for (Proxy proxy : proxies.values())
            proxy.update(terrain, camera, handoff, thermal, impostors);
    }

    /**
     * Visits every shape that {@link DistantBoxRenderer} draws this tick. Those with an impostor are first offered
     * to {@code impostors}, when given, and passed to {@code boxes} only when it did not draw them.
     */
    static void forEachNear(@Nullable Predicate<IDistantImpostor> impostors, DistantBoxRenderer.ShapeConsumer boxes)
    {
        for (Proxy proxy : proxies.values())
        {
            if (proxy.near && (impostors == null || proxy.atlas == null || !impostors.test(proxy)))
                boxes.accept(proxy, proxy.boxes, DistantBoxStyle.SOLID);
        }
    }

    /** Visits every impostor drawn into the far terrain this tick, in place of its boxes there. */
    static void forEachFarImpostor(Consumer<IDistantImpostor> action)
    {
        for (Proxy proxy : proxies.values())
        {
            if (proxy.farImpostor && proxy.atlas != null)
                action.accept(proxy);
        }
    }

    public static void reset()
    {
        for (Proxy proxy : proxies.values())
            proxy.close();
        proxies.clear();
        reported.clear();
    }

    private static final class Proxy implements IDistantBoxGroup.Origin, IDistantImpostor
    {
        private final int id;
        private final Quaternionf rotationScratch = new Quaternionf();
        /** The tracked entity: a driveable, or the wearer of {@link #worn}. */
        @Nullable
        private Entity entity;
        /** The part body of a tracked entity that stands in for a driveable. */
        @Nullable
        private PartBody worn;
        /** The skin a stand-in was last seen with, kept for when only the server reports it. */
        @Nullable
        private ResourceLocation skin;
        /** The proxy's own body, posed from the reports of a stand-in the client does not track. */
        @Nullable
        private PartBody reportedBody;
        @Nullable
        private Reported report;
        @Nullable
        private DriveableType type;
        private List<DistantBox> boxes = List.of();
        @Nullable
        private IDistantBoxGroup group;
        /** Whether the mod draws this shape itself this tick, rather than the far-terrain renderer. */
        private boolean near;
        /** The atlas the shape is drawn from as an impostor, or null for boxes; asked for again every tick. */
        @Nullable
        private DriveableImpostorCache.DistantAtlas atlas;
        /** Blocks the impostor is pulled towards the camera; none for what floats, whose hull the water should hide. */
        private float pull;
        /** Whether the shape is drawn as an impostor into the far terrain this tick, rather than as boxes. */
        private boolean farImpostor;

        private Proxy(int id)
        {
            this.id = id;
        }

        private void follow(Driveable driveable)
        {
            entity = driveable;
            worn = null;
            skin = null;
            report = null;
            type = driveable.getConfigType();
        }

        private void follow(Entity wearer, PartBody body, DriveableType wornType)
        {
            entity = wearer;
            worn = body;
            skin = skinOf(wearer);
            report = null;
            type = wornType;
        }

        private void follow(Reported reported, DriveableType reportedType)
        {
            entity = null;
            worn = null;
            report = reported;
            type = reportedType;
        }

        /** The texture the entity's renderer draws it with, which for a stand-in is the skin of its driveable model. */
        @Nullable
        @SuppressWarnings({"unchecked", "rawtypes"}) // The dispatcher hands out the renderer registered for the entity's own type.
        private static ResourceLocation skinOf(Entity wearer)
        {
            EntityRenderer renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(wearer);
            return renderer.getTextureLocation(wearer);
        }

        @Override
        public Vec3 at(float partialTick)
        {
            if (entity != null)
                return entity.getPosition(partialTick);
            if (report == null)
                return Vec3.ZERO;

            // Carry on along the last reported velocity until the next update, but not indefinitely
            PacketDistantContacts.Contact contact = report.contact();
            double age = Math.min(ticks - report.receivedTick() + partialTick, updateInterval * 2D);
            return new Vec3(contact.x() + contact.velocityX() * age, contact.y() + contact.velocityY() * age, contact.z() + contact.velocityZ() * age);
        }

        /** Never null while visited as an impostor, which only happens while it holds an atlas. */
        @Override
        public DriveableImpostorCache.DistantAtlas atlas()
        {
            return atlas;
        }

        @Override
        public Quaternionf rotation(float partialTick, Quaternionf out)
        {
            float modelYaw;
            float pitch;
            float roll;
            if (entity instanceof Driveable driveable)
            {
                modelYaw = DriveableRenderer.modelYaw(driveable, Mth.rotLerp(partialTick, driveable.getPrevYaw(), driveable.getYaw()));
                pitch = Mth.rotLerp(partialTick, driveable.getPrevPitch(), driveable.getPitch());
                roll = Mth.rotLerp(partialTick, driveable.getPrevRoll(), driveable.getRoll());
            }
            else if (report != null)
            {
                // Only planes and vehicles are drawn as impostors, and both turn their models this way
                PacketDistantContacts.Contact contact = report.contact();
                modelYaw = 180F - contact.yaw();
                pitch = contact.pitch();
                roll = contact.roll();
            }
            else
                return out.identity();
            // As the driveable renderer turns the model it pictures
            return out.identity().rotateY(modelYaw * Mth.DEG_TO_RAD).rotateZ(pitch * Mth.DEG_TO_RAD).rotateX(roll * Mth.DEG_TO_RAD);
        }

        @Override
        public float pull()
        {
            return pull;
        }

        /**
         * The atlas a plane or vehicle is drawn from as an impostor, asking for the view the camera now has of it, or
         * null for boxes: until a view is captured, for mechas, and for the entities standing in for driveables, whose
         * pose the neutral model in the atlas does not show.
         */
        @Nullable
        private DriveableImpostorCache.DistantAtlas requestAtlas(Vec3 camera)
        {
            if (type == null || worn != null)
                return null;
            int paintjob;
            if (entity instanceof Driveable driveable && (driveable instanceof Plane || driveable instanceof Vehicle))
                paintjob = driveable.getPaintjobId();
            else if (entity == null && report != null && !report.contact().standIn() && (type instanceof PlaneType || type instanceof VehicleType))
                paintjob = report.contact().paintjobId();
            else
                return null;

            // A first sighting waits for its model's background load rather than stalling the tick
            if (ModelPreloader.isLoadPending(type) || !(ModelCache.getOrLoadTypeModel(type) instanceof ModelDriveable model))
                return null;
            DriveableImpostorCache.DistantAtlas requested = DriveableImpostorCache.requestDistant(model, type, DriveableRenderer.skin(type, paintjob), camera.subtract(at(1F)),
                rotation(1F, rotationScratch));
            if (requested != null)
                pull = type.isFloatOnWater() ? 0F : DriveableImpostorCache.distantRadius(requested);
            return requested;
        }

        private void update(IDistantTerrain terrain, Vec3 camera, double handoff, boolean thermal, boolean impostors)
        {
            atlas = null;
            farImpostor = false;
            if (type == null)
                return;

            refreshBoxes(type, thermal);
            near = at(1F).distanceTo(camera) < handoff;
            // Views are only worth capturing where an impostor can be drawn
            boolean farImpostors = terrain.drawsImpostors();
            if (impostors && (near || farImpostors))
                atlas = requestAtlas(camera);
            farImpostor = !near && farImpostors && atlas != null;
            if (near || farImpostor || !terrain.drawsBoxes())
            {
                if (group != null)
                    group.setActive(false);
                return;
            }
            showGroup(terrain, thermal);
        }

        /** Rebuilds the boxes from what is known of the driveable this tick. */
        private void refreshBoxes(DriveableType shown, boolean thermal)
        {
            if (entity instanceof Driveable driveable)
            {
                int color = tint(thermal, DistantTextureColors.of(shown, driveable.getPaintjobId()));
                boxes = DistantProxyShapes.forEntity(driveable, color);
            }
            else if (entity != null && worn != null)
            {
                int color = tint(thermal, DistantTextureColors.of(shown, skin));
                boxes = DistantProxyShapes.forBody(worn, worn.originOffset(), Set.of(), color);
            }
            else if (report != null && report.contact().standIn())
            {
                PacketDistantContacts.Contact contact = report.contact();
                int color = tint(thermal, DistantTextureColors.of(shown, skin));
                if (reportedBody == null || reportedBody.getType() != shown)
                    reportedBody = PartBody.of(shown);
                reportedBody.setPose(new PartBodyPose(Vec3.ZERO, contact.yaw(), contact.pitch(), contact.roll(), contact.scale(), MountAim.REST));
                boxes = DistantProxyShapes.forBody(reportedBody, Vec3.ZERO, destroyedParts(contact), color);
            }
            else if (report != null)
            {
                PacketDistantContacts.Contact contact = report.contact();
                int color = tint(thermal, DistantTextureColors.of(shown, contact.paintjobId()));
                boxes = DistantProxyShapes.forType(shown, contact.yaw(), contact.pitch(), contact.roll(), destroyedParts(contact), color);
            }
        }

        /** The colour of the boxes: the skin's, or a thermal sight's heat. */
        private static int tint(boolean thermal, int skinColor)
        {
            return thermal ? THERMAL_COLOR : skinColor;
        }

        /** Has the far terrain draw the boxes, making their group again after the terrain dropped it. */
        private void showGroup(IDistantTerrain terrain, boolean thermal)
        {
            if (group != null && !group.isValid())
            {
                group.close();
                group = null;
            }
            if (group == null)
            {
                group = terrain.createGroup("driveable", DistantBoxStyle.SOLID);
                if (group == null)
                    return;
                group.setOrigin(this);
            }
            group.setBoxes(boxes);
            group.setGlowing(thermal);
            group.setActive(true);
        }

        private static Set<EnumDriveablePart> destroyedParts(PacketDistantContacts.Contact contact)
        {
            if (contact.destroyedParts().length == 0)
                return Set.of();
            EnumDriveablePart[] parts = EnumDriveablePart.values();
            Set<EnumDriveablePart> destroyed = EnumSet.noneOf(EnumDriveablePart.class);
            for (int ordinal : contact.destroyedParts())
            {
                if (ordinal >= 0 && ordinal < parts.length)
                    destroyed.add(parts[ordinal]);
            }
            return destroyed;
        }

        private void close()
        {
            if (group != null)
                group.close();
            group = null;
            atlas = null;
            farImpostor = false;
        }
    }
}
