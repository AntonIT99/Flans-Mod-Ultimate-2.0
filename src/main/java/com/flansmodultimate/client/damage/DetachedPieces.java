package com.flansmodultimate.client.damage;

import com.flansmod.client.model.ModelDriveable;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.client.render.EnumRenderPass;
import com.flansmodultimate.client.render.gpu.GpuModelCache;
import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.visual.DriveableVisual;
import com.flansmodultimate.config.ModClientConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Pieces shot off driveables, drawn tumbling to the ground through the driveable's own model.
 *
 * <p>
 * Each frame a driveable or an NPC drawn as one is rendered, where its model was drawn is remembered relative to the
 * entity. A piece thrown off later starts from that pose, so it leaves exactly where it was drawn, turret turned and
 * all, then flies, spins, bounces and comes to rest on the blocks below, smoking while it burns, and finally settles
 * into the ground. Pieces are cosmetic: the server knows nothing of them.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DetachedPieces
{
    private static final double GRAVITY = 0.045D;
    private static final double AIR_DRAG = 0.985D;
    private static final double BOUNCE = 0.3D;
    private static final double GROUND_FRICTION = 0.6D;
    /** Ticks a piece burns for once it has been thrown off a burning machine. */
    private static final int BURN_TICKS = 160;
    /** Ticks over which a piece sinks into the ground at the end of its life. */
    private static final int SETTLE_TICKS = 40;
    private static final int MIN_LIFETIME = 300;
    private static final int LIFETIME_SPREAD = 120;

    /** How each entity's model was last drawn. */
    private static final Map<Entity, Capture> CAPTURES = new WeakHashMap<>();
    private static final List<Piece> PIECES = new ArrayList<>();

    /** Where a model was drawn, relative to the entity drawing it, and with what. */
    private static final class Capture
    {
        private final Matrix4f relative = new Matrix4f();
        private ModelDriveable model;
        private ModelDriveable.RenderState state;
        private DriveableVisual visual;
        private ResourceLocation texture;
        private boolean translucent;
        private boolean cull;
        private float red;
        private float green;
        private float blue;
        private Vec3 origin = Vec3.ZERO;
    }

    private static final class Piece
    {
        private final Capture capture = new Capture();
        private DriveableVisual visual;
        /** The point the piece spins around, relative to the capture's origin. */
        private Vector3f pivot;
        private Vec3 offset = Vec3.ZERO;
        private Vec3 prevOffset = Vec3.ZERO;
        private Vec3 velocity;
        private final Quaternionf rotation = new Quaternionf();
        private final Quaternionf prevRotation = new Quaternionf();
        private Vector3f spinAxis;
        private float spin;
        private int age;
        private int lifetime;
        private boolean burning;
        private boolean resting;
    }

    /**
     * Remembers how the entity's model was just drawn. Render thread, once per frame per drawn entity.
     *
     * @param entry
     *            the pose the entity was handed to its renderer with
     * @param drawn
     *            the pose the model was drawn with, after every transform and its scale
     * @param origin
     *            the entity's interpolated world position the entry pose stands for
     */
    // Mirrors what the renderer holds at the moment it draws: model, state, colour and both poses.
    @SuppressWarnings("java:S107")
    public static void capture(Entity entity, ModelDriveable model, ModelDriveable.RenderState state, DriveableVisual visual, ResourceLocation texture, boolean translucent, boolean cull, float red,
        float green, float blue, Matrix4f entry, Matrix4f drawn, Vec3 origin)
    {
        Capture capture = CAPTURES.computeIfAbsent(entity, ignored -> new Capture());
        capture.relative.set(entry).invert().mul(drawn);
        capture.model = model;
        capture.state = state;
        capture.visual = visual;
        capture.texture = texture;
        capture.translucent = translucent;
        capture.cull = cull;
        capture.red = red;
        capture.green = green;
        capture.blue = blue;
        capture.origin = origin;
    }

    /**
     * Throws the parts off the entity as one piece.
     *
     * @param pivot
     *            world point the piece spins around, usually the centre of its main part
     * @param velocity
     *            initial world velocity in blocks per tick
     * @param burning
     *            whether it leaves on fire and is drawn scorched
     * @return false when the entity was never drawn or pieces are turned off
     */
    public static boolean spawn(Entity entity, Set<EnumDriveablePart> parts, Vec3 pivot, Vec3 velocity, float spin, boolean burning)
    {
        Capture capture = CAPTURES.get(entity);
        int limit = maxPieces();
        if (capture == null || capture.model == null || limit <= 0 || parts.isEmpty())
            return false;
        while (PIECES.size() >= limit)
            PIECES.remove(0);
        RandomSource random = entity.level().random;
        Piece piece = new Piece();
        copy(capture, piece.capture);
        piece.visual = PieceVisuals.of(capture.visual, parts, burning);
        Vec3 relativePivot = pivot.subtract(capture.origin);
        piece.pivot = new Vector3f((float) relativePivot.x, (float) relativePivot.y, (float) relativePivot.z);
        piece.velocity = velocity;
        piece.spinAxis = new Vector3f(random.nextFloat() - 0.5F, random.nextFloat() - 0.5F, random.nextFloat() - 0.5F);
        if (piece.spinAxis.lengthSquared() < 1.0E-4F)
            piece.spinAxis.set(0F, 1F, 0F);
        piece.spinAxis.normalize();
        piece.spin = spin;
        piece.lifetime = MIN_LIFETIME + random.nextInt(LIFETIME_SPREAD);
        piece.burning = burning;
        PIECES.add(piece);
        return true;
    }

    private static void copy(Capture from, Capture to)
    {
        to.relative.set(from.relative);
        to.model = from.model;
        to.state = from.state;
        to.visual = from.visual;
        to.texture = from.texture;
        to.translucent = from.translucent;
        to.cull = from.cull;
        to.red = from.red;
        to.green = from.green;
        to.blue = from.blue;
        to.origin = from.origin;
    }

    private static int maxPieces()
    {
        ModClientConfig config = ModClientConfig.get();
        return config == null ? 24 : config.maxDetachedDriveablePieces;
    }

    /** Moves every piece on by a tick. Client tick. */
    public static void tick()
    {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
        {
            PIECES.clear();
            CAPTURES.clear();
            return;
        }
        Iterator<Piece> pieces = PIECES.iterator();
        while (pieces.hasNext())
        {
            Piece piece = pieces.next();
            if (++piece.age >= piece.lifetime)
                pieces.remove();
            else
                step(level, piece);
        }
    }

    private static void step(ClientLevel level, Piece piece)
    {
        piece.prevOffset = piece.offset;
        piece.prevRotation.set(piece.rotation);
        Vec3 centre = worldPivot(piece, piece.offset);
        if (piece.age > piece.lifetime - SETTLE_TICKS)
        {
            // Settles into the ground rather than vanishing.
            piece.offset = piece.offset.add(0D, -0.03D, 0D);
            return;
        }
        if (piece.burning && piece.age < BURN_TICKS)
            smoke(level, piece, centre);
        if (piece.resting)
            return;
        boolean water = level.getFluidState(BlockPos.containing(centre)).is(FluidTags.WATER);
        Vec3 velocity = piece.velocity.add(0D, water ? -GRAVITY * 0.2D : -GRAVITY, 0D).scale(water ? 0.8D : AIR_DRAG);
        Vec3 next = piece.offset.add(velocity);
        // A wall turns the piece back rather than letting it through.
        if (insideSolid(level, worldPivot(piece, new Vec3(next.x, piece.offset.y, next.z))) && !insideSolid(level, worldPivot(piece, piece.offset)))
        {
            next = new Vec3(piece.offset.x, next.y, piece.offset.z);
            velocity = new Vec3(-velocity.x * BOUNCE, velocity.y, -velocity.z * BOUNCE);
            piece.spin *= 0.7F;
        }
        double ground = groundBelow(level, worldPivot(piece, next));
        if (Double.isFinite(ground) && worldPivot(piece, next).y <= ground)
        {
            next = new Vec3(next.x, next.y + ground - worldPivot(piece, next).y, next.z);
            velocity = new Vec3(velocity.x * GROUND_FRICTION, -velocity.y * BOUNCE, velocity.z * GROUND_FRICTION);
            piece.spin *= 0.55F;
            if (Math.abs(velocity.y) < 0.06D && velocity.horizontalDistanceSqr() < 0.004D)
            {
                piece.resting = true;
                velocity = Vec3.ZERO;
                DamageParticles.puff(DamageParticles.DUST, worldPivot(piece, next), 0D, 0.01D, 0D, 0.8F, 30, false);
            }
        }
        piece.offset = next;
        piece.velocity = velocity;
        if (piece.spin != 0F)
            piece.rotation.premul(new Quaternionf().rotateAxis(piece.spin, piece.spinAxis));
    }

    private static void smoke(ClientLevel level, Piece piece, Vec3 centre)
    {
        RandomSource random = level.random;
        float fade = 1F - piece.age / (float) BURN_TICKS;
        if (random.nextFloat() < 0.6F * fade * DamageParticles.density())
            DamageParticles.puff(DamageParticles.BLACK_SMOKE, centre, 0D, 0.03D, 0D, 0.5F + 0.4F * fade, 50 + random.nextInt(30), false);
        if (random.nextFloat() < 0.4F * fade * DamageParticles.density())
            DamageParticles.puff(DamageParticles.FIRE, centre, 0D, 0.04D, 0D, 0.35F + 0.3F * fade, 12, false);
    }

    private static Vec3 worldPivot(Piece piece, Vec3 offset)
    {
        return piece.capture.origin.add(offset).add(piece.pivot.x, piece.pivot.y, piece.pivot.z);
    }

    /** @return whether the point lies inside a block's collision shape */
    private static boolean insideSolid(ClientLevel level, Vec3 point)
    {
        BlockPos pos = BlockPos.containing(point);
        VoxelShape shape = level.getBlockState(pos).getCollisionShape(level, pos);
        return !shape.isEmpty() && shape.bounds().move(pos).contains(point);
    }

    /** @return the top of the blocks under the point, within two blocks, or NaN when it is falling clear */
    static double groundBelow(ClientLevel level, Vec3 point)
    {
        BlockPos.MutableBlockPos cursor = BlockPos.containing(point).mutable();
        for (int i = 0; i < 3; i++)
        {
            BlockState state = level.getBlockState(cursor);
            VoxelShape shape = state.getCollisionShape(level, cursor);
            if (!shape.isEmpty())
                return cursor.getY() + shape.max(Direction.Axis.Y);
            cursor.move(0, -1, 0);
        }
        return Double.NaN;
    }

    /** Draws every piece. Render thread, after the entities. */
    public static void render(PoseStack poseStack, Camera camera, float partialTick)
    {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (PIECES.isEmpty() || level == null)
            return;
        MultiBufferSource.BufferSource buffers = minecraft.renderBuffers().bufferSource();
        Vec3 cameraPosition = camera.getPosition();
        Quaternionf rotation = new Quaternionf();
        for (Piece piece : PIECES)
        {
            Capture capture = piece.capture;
            Vec3 offset = piece.prevOffset.lerp(piece.offset, partialTick);
            piece.prevRotation.slerp(piece.rotation, partialTick, rotation);
            Vec3 at = capture.origin.add(offset);
            poseStack.pushPose();
            poseStack.translate(at.x - cameraPosition.x, at.y - cameraPosition.y, at.z - cameraPosition.z);
            poseStack.translate(piece.pivot.x, piece.pivot.y, piece.pivot.z);
            poseStack.mulPose(rotation);
            poseStack.translate(-piece.pivot.x, -piece.pivot.y, -piece.pivot.z);
            poseStack.last().pose().mul(capture.relative);
            poseStack.last().normal().mul(new Matrix3f(capture.relative).invert().transpose());
            int light = LevelRenderer.getLightColor(level, BlockPos.containing(worldPivot(piece, offset)));
            draw(capture, piece.visual, poseStack, buffers, light);
            poseStack.popPose();
        }
        buffers.endBatch();
    }

    private static void draw(Capture capture, DriveableVisual visual, PoseStack poseStack, MultiBufferSource.BufferSource buffers, int light)
    {
        for (EnumRenderPass pass : ModelCache.getRenderPasses(capture.model))
        {
            VertexConsumer consumer = GpuModelCache.begin(buffers, pass, capture.texture, capture.translucent, capture.cull, true);
            try
            {
                capture.model.render(visual, capture.state, poseStack, consumer, light, OverlayTexture.NO_OVERLAY, capture.red, capture.green, capture.blue, 1F, 1F, pass);
            }
            finally
            {
                GpuModelCache.end(consumer);
            }
        }
    }

    /** Forgets every piece, as when leaving a world. */
    public static void clear()
    {
        PIECES.clear();
        CAPTURES.clear();
    }

    /** @return a velocity throwing a piece up and away from the centre it was part of */
    static Vec3 throwVelocity(Vec3 from, Vec3 centre, Vec3 carried, double up, double out, RandomSource random)
    {
        Vec3 away = from.subtract(centre).multiply(1D, 0D, 1D);
        away = away.lengthSqr() > 1.0E-4D ? away.normalize() : new Vec3(random.nextDouble() - 0.5D, 0D, random.nextDouble() - 0.5D).normalize();
        double spread = 0.6D + random.nextDouble() * 0.8D;
        return carried.add(away.scale(out * spread)).add((random.nextDouble() - 0.5D) * out * 0.5D, up * (0.7D + random.nextDouble() * 0.6D), (random.nextDouble() - 0.5D) * out * 0.5D);
    }

    /** @return a random spin rate, in radians per tick */
    static float randomSpin(RandomSource random, float scale)
    {
        return (0.08F + random.nextFloat() * 0.25F) * scale * (random.nextBoolean() ? 1F : -1F);
    }

    /** Clamps a world-space point the piece is thrown from, so a piece never leaves from inside the ground. */
    static Vec3 aboveGround(ClientLevel level, Vec3 point)
    {
        double ground = groundBelow(level, point);
        return Double.isFinite(ground) && point.y < ground + 0.1D ? new Vec3(point.x, ground + 0.1D, point.z) : point;
    }

    /** @return the tilt the wreck of a mecha is drawn falling over with, in degrees */
    public static float mechaCollapseDegrees(float progress)
    {
        return 78F * Mth.clamp(progress, 0F, 1.05F);
    }
}
