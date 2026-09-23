package com.flansmodultimate.common.driveables;

import com.flansmodultimate.common.raytracing.RotatedAxes;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The articulated skeleton that 1.7.10 drove for planes with {@code Valkyrie true}
 * ({@code client.model.animation.AnimationController}). It is a fixed VF-1 style
 * hierarchy of 33 joints; {@code ModelPlane.valkyrie[i]} holds the geometry of joint
 * {@code i}, modelled relative to that joint. Each tick every joint eases its offset
 * and rotation towards the active pose. Fighter mode holds pose 0; GERWALK steps
 * through poses 1 to 4, advancing once every joint has reached the current one.
 * <p>
 * Positions are legacy model units (1/16 block, Y down as authored), rotations are
 * degrees. This is visual state only and is advanced on the client.
 * <p>
 * Deliberate differences from 1.7.10: the pose follows the synced wing flag, where
 * 1.7.10 only switched the pose on the pressing client, and a joint counts as settled
 * when it equals its target rather than when both vectors merely have the same
 * length, which could freeze a joint mid-transition. The unreachable fifth GERWALK
 * pose is omitted.
 */
public final class ValkyrieAnimation
{
    public static final int PART_COUNT = 33;
    public static final int LEFT_LEG_SHIN = 5;
    public static final int RIGHT_LEG_SHIN = 8;

    /** Exhaust nozzles at the feet, in the shins' model space. */
    private static final Vector3f LEFT_FOOT_EXHAUST = new Vector3f(151F, -25F, -24F);
    private static final Vector3f RIGHT_FOOT_EXHAUST = new Vector3f(151F, -25F, 24F);

    private static final int FIGHTER = 0;
    private static final int GERWALK = 1;
    private static final int LAST_GERWALK_STAGE = 4;

    private static final Pose[] POSES = createPoses();

    private final List<Part> parts = new ArrayList<>(PART_COUNT);
    private int state = -1;
    private int stage = 1;
    private int timeSinceSwitch;

    public ValkyrieAnimation()
    {
        // Joint positions and parents exactly as AnimationController.initAnim().
        addPart(11F, -34.5F, 0F, -1);      // 0 core
        addPart(6F, -34.5F, 0F, 0);        // 1 mid front
        addPart(-25F, -34.5F, 0F, 1);      // 2 nose
        addPart(-9F, -25F, -13.5F, 0);     // 3 left leg top
        addPart(6F, -23F, -21.5F, 3);      // 4 left leg mid
        addPart(34F, -21.5F, -21.5F, 4);   // 5 left leg shin
        addPart(-9F, -25F, 13.5F, 0);      // 6 right leg top
        addPart(6F, -23F, 21.5F, 6);       // 7 right leg mid
        addPart(34F, -21.5F, 21.5F, 7);    // 8 right leg shin
        addPart(6F, -33F, 0F, 0);          // 9 rear body
        addPart(43F, -38.5F, 0F, 9);       // 10 tail mid
        addPart(60F, -40F, -14.5F, 10);    // 11 fin left
        addPart(60F, -40F, 14.5F, 10);     // 12 fin right
        addPart(10F, -36.5F, -17F, 9);     // 13 left wing
        addPart(10F, -36.5F, 17F, 9);      // 14 right wing
        addPart(39F, -13F, -21.5F, 5);     // 15 left knee
        addPart(119F, -23F, -21.5F, 5);    // 16 left foot top
        addPart(119F, -23F, -21.5F, 5);    // 17 left foot bottom
        addPart(39F, -13F, 21.5F, 8);      // 18 right knee
        addPart(119F, -23F, 21.5F, 8);     // 19 right foot top
        addPart(119F, -23F, 21.5F, 8);     // 20 right foot bottom
        addPart(-17F, -33F, 0F, 2);        // 21 head
        addPart(-15F, -23F, 0F, 21);       // 22 head guns
        addPart(17F, -31F, -7F, 0);        // 23 left shoulder
        addPart(22F, -20.5F, -7F, 23);     // 24 left shoulder joint
        addPart(33F, -20.5F, -7F, 24);     // 25 left upper arm
        addPart(68F, -19.5F, -7F, 25);     // 26 left lower arm
        addPart(69F, -21.5F, -7F, 26);     // 27 left hand
        addPart(17F, -31F, 7F, 0);         // 28 right shoulder
        addPart(22F, -20.5F, 7F, 28);      // 29 right shoulder joint
        addPart(33F, -20.5F, 7F, 29);      // 30 right upper arm
        addPart(68F, -19.5F, 7F, 30);      // 31 right lower arm
        addPart(69F, -21.5F, 7F, 31);      // 32 right hand
    }

    private void addPart(float x, float y, float z, int parent)
    {
        Part part = new Part(parts.size(), new Vector3f(x, y, z), parent);
        parts.add(part);
        if (parent >= 0)
            parts.get(parent).children.add(part);
    }

    public Part getCore()
    {
        return parts.get(0);
    }

    public List<Part> getParts()
    {
        return Collections.unmodifiableList(parts);
    }

    /**
     * Advances one tick. {@code fighterMode} is the plane's wing flag (1.7.10
     * {@code varWing}): set for fighter mode, clear for GERWALK.
     */
    public void tick(boolean fighterMode)
    {
        int requested = fighterMode ? FIGHTER : GERWALK;
        if (requested != state)
        {
            state = requested;
            stage = 1;
            timeSinceSwitch = 0;
        }
        Pose pose;
        if (state == FIGHTER)
        {
            pose = POSES[0];
            stage = 1;
            timeSinceSwitch = 0;
        }
        else
        {
            pose = POSES[Math.min(stage, LAST_GERWALK_STAGE)];
        }

        boolean settled = true;
        for (Part part : parts)
        {
            part.previousOffset.set(part.offset);
            part.previousRotation.set(part.rotation);
            Pose.Component target = pose.components[part.id];
            step(part.offset, target.position(), target.positionRate());
            step(part.rotation, target.rotation(), target.rotationRate());
            settled &= part.offset.equals(target.position()) && part.rotation.equals(target.rotation());
        }
        if (settled && timeSinceSwitch > 2)
            ++stage;
        ++timeSinceSwitch;
    }

    /** AnimationController.transformPart: move by {@code rate} per axis, snapping within half a step. */
    private static void step(Vector3f current, Vector3f target, float rate)
    {
        current.x = stepAxis(current.x, target.x, rate);
        current.y = stepAxis(current.y, target.y, rate);
        current.z = stepAxis(current.z, target.z, rate);
    }

    private static float stepAxis(float current, float target, float rate)
    {
        if (Math.abs(current - target) <= rate / 2F)
            return target;
        return current > target ? current - rate : current + rate;
    }

    /**
     * Where the left foot exhaust is relative to the plane origin, in the plane's
     * local axes and blocks, or {@code null} when the chain produced no finite point.
     */
    @Nullable
    public Vector3f leftFootExhaust()
    {
        return footExhaust(LEFT_FOOT_EXHAUST, parts.get(LEFT_LEG_SHIN));
    }

    @Nullable
    public Vector3f rightFootExhaust()
    {
        return footExhaust(RIGHT_FOOT_EXHAUST, parts.get(RIGHT_LEG_SHIN));
    }

    @Nullable
    private Vector3f footExhaust(Vector3f point, Part part)
    {
        Vector3f position = getFullPosition(point, part);
        if (!Float.isFinite(position.x) || !Float.isFinite(position.y) || !Float.isFinite(position.z))
            return null;
        // EntityPlane converted the skeleton result with (-x, -y, z) / 16.
        return new Vector3f(-position.x / 16F, -position.y / 16F, position.z / 16F);
    }

    /** AnimationController.getFullPosition: {@code point} on {@code part}, walked back up to the core. */
    Vector3f getFullPosition(Vector3f point, Part part)
    {
        List<Part> chain = new ArrayList<>();
        for (Part link = part; link != null; link = link.parent >= 0 ? parts.get(link.parent) : null)
            chain.add(link);

        Vector3f position = new Vector3f(getCore().position);
        RotatedAxes axes = new RotatedAxes(0F, 0F, 0F);
        for (int i = chain.size() - 1; i > 0; i--)
        {
            Part child = chain.get(i - 1);
            Vector3f childJoint = new Vector3f(child.position).add(child.offset.x, -child.offset.y, child.offset.z);
            position.add(positionOnPart(childJoint, chain.get(i), axes));
        }
        return position.add(positionOnPart(point, part, axes));
    }

    private static Vector3f positionOnPart(Vector3f point, Part part, RotatedAxes axes)
    {
        axes.rotateLocalRoll(-part.rotation.x);
        axes.rotateLocalYaw(part.rotation.y);
        axes.rotateLocalPitch(-part.rotation.z);
        return axes.findLocalVectorGlobally(new Vector3f(point).sub(part.position));
    }

    @Getter
    public static final class Part
    {
        private final int id;
        private final Vector3f position;
        private final int parent;
        private final Vector3f offset = new Vector3f();
        private final Vector3f rotation = new Vector3f();
        private final Vector3f previousOffset = new Vector3f();
        private final Vector3f previousRotation = new Vector3f();
        private final List<Part> children = new ArrayList<>();

        private Part(int id, Vector3f position, int parent)
        {
            this.id = id;
            this.position = position;
            this.parent = parent;
        }

        public Vector3f getOffset(float partialTick)
        {
            return new Vector3f(previousOffset).lerp(offset, partialTick);
        }

        public Vector3f getRotation(float partialTick)
        {
            return new Vector3f(previousRotation).lerp(rotation, partialTick);
        }

        public List<Part> getChildren()
        {
            return Collections.unmodifiableList(children);
        }
    }

    private record Pose(Component[] components)
    {
        private record Component(Vector3f position, Vector3f rotation, float positionRate, float rotationRate) {}
    }

    /** Pose data from AnimationController.addDefaultPose() and addGERWALKPose() to addGERWALK4(). */
    private static Pose[] createPoses()
    {
        PoseBuilder fighter = new PoseBuilder()
            .rates(LEFT_LEG_SHIN, 2, 4).rates(RIGHT_LEG_SHIN, 2, 4)
            .rates(10, 2, 12).rates(11, 2, 4).rates(12, 2, 4)
            .set(13, 0, 0, 0, 0, 30, 0, 2, 2).set(14, 0, 0, 0, 0, -30, 0, 2, 2);
        for (int arm = 23; arm <= 32; arm++)
            fighter.rates(arm, 5, 5);

        PoseBuilder gerwalk = new PoseBuilder()
            .set(4, 5, 0, 0, -20, 0, -50, 2, 8)
            .set(5, 5, 0, 0, 0, 0, -100, 2, 16)
            .set(7, 5, 0, 0, 20, 0, -50, 2, 8)
            .set(8, 5, 0, 0, 0, 0, -100, 2, 16)
            .set(10, 0, 0, 0, 0, 0, 160, 2, 12)
            .set(11, 0, 0, 0, -100, 0, 0, 2, 14)
            .set(12, 0, 0, 0, 100, 0, 0, 2, 14)
            .set(15, 0, 0, 0, 0, 0, 90, 2, 25)
            .set(16, 0, 0, 0, 0, 0, 45, 2, 25)
            .set(17, 0, 0, 0, 0, 0, -45, 2, 25)
            .set(18, 0, 0, 0, 0, 0, 90, 2, 25)
            .set(19, 0, 0, 0, 0, 0, 45, 2, 25)
            .set(20, 0, 0, 0, 0, 0, -45, 2, 25);

        // GERWALK 2 is the settled stance that stages 3 and 4 build on.
        PoseBuilder gerwalk2 = gerwalk2();

        PoseBuilder gerwalk3 = gerwalk2()
            .set(23, 15, -25, 0, -90, 0, 0, 6, 12)
            .set(28, 15, -25, 0, 90, 0, 0, 6, 12);

        PoseBuilder gerwalk4 = gerwalk2()
            .set(23, 10, -25, -10, -90, 90, 0, 4, 6)
            .set(24, 0, 0, 0, 0, 0, -80, 2, 6)
            .set(25, 0, 0, 0, -90, 0, 0, 2, 6)
            .set(26, 0, 0, 0, 0, 0, 50, 2, 12)
            .set(27, 23, 0, 0, 180, 0, 0, 2, 20)
            .set(28, 10, -25, 10, 90, -90, 0, 4, 6)
            .set(29, 0, 0, 0, 0, 0, -80, 2, 6)
            .set(30, 0, 0, 0, 90, 0, 0, 2, 6)
            .set(31, 0, 0, 0, 0, 0, 50, 2, 12)
            .set(32, 23, 0, 0, 180, 0, 0, 2, 20);

        return new Pose[] {fighter.build(), gerwalk.build(), gerwalk2.build(), gerwalk3.build(), gerwalk4.build()};
    }

    private static PoseBuilder gerwalk2()
    {
        return new PoseBuilder()
            .set(4, 10, 0, 0, -20, 0, -35, 2, 3)
            .set(5, 5, 5, 0, 0, 0, -75, 2, 6)
            .set(7, 10, 0, 0, 20, 0, -35, 2, 3)
            .set(8, 5, 5, 0, 0, 0, -75, 2, 6)
            .set(10, 0, 0, 0, 0, 0, 160, 2, 12)
            .set(11, 0, 0, 0, -100, 0, 0, 2, 14)
            .set(12, 0, 0, 0, 100, 0, 0, 2, 14)
            .set(15, 0, 0, 0, 0, 0, 110, 2, 25)
            .set(16, 0, 0, 0, 0, 0, 60, 2, 2)
            .set(17, 0, 0, 0, 0, 0, -30, 2, 2)
            .set(18, 0, 0, 0, 0, 0, 110, 2, 25)
            .set(19, 0, 0, 0, 0, 0, 60, 2, 2)
            .set(20, 0, 0, 0, 0, 0, -30, 2, 2)
            .set(22, 0, 0, 0, 0, 0, 20, 2, 2)
            .set(23, 15, 0, 0, 0, 0, 0, 2, 2)
            .set(28, 15, 0, 0, 0, 0, 0, 2, 2);
    }

    /** Every joint defaults to rest with rates (2, 2), as in the legacy tables. */
    private static final class PoseBuilder
    {
        private final Pose.Component[] components = new Pose.Component[PART_COUNT];

        private PoseBuilder()
        {
            for (int i = 0; i < PART_COUNT; i++)
                components[i] = new Pose.Component(new Vector3f(), new Vector3f(), 2F, 2F);
        }

        private PoseBuilder set(int part, float x, float y, float z, float rotX, float rotY, float rotZ,
                                float positionRate, float rotationRate)
        {
            components[part] = new Pose.Component(new Vector3f(x, y, z), new Vector3f(rotX, rotY, rotZ),
                positionRate, rotationRate);
            return this;
        }

        private PoseBuilder rates(int part, float positionRate, float rotationRate)
        {
            Pose.Component component = components[part];
            components[part] = new Pose.Component(component.position(), component.rotation(), positionRate, rotationRate);
            return this;
        }

        private Pose build()
        {
            return new Pose(components.clone());
        }
    }
}
