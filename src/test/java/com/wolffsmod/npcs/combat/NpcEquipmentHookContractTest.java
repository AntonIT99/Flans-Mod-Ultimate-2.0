package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Checks the installed dependency's bytecode without loading Custom NPCs or client classes on the test/server path. */
class NpcEquipmentHookContractTest
{
    private static final Path CUSTOM_NPCS = Path.of("libs/CustomNPCs-Unofficial-NeoForge-1.21.1.20251230.jar");
    private static final String NPC = "noppes/npcs/entity/EntityNPCInterface";

    @Test
    void physicalReturnReplacesTheTickContainingBothAutomaticTeleports() throws IOException
    {
        ClassNode goal = read("noppes/npcs/ai/EntityAIReturn");
        assertEquals(2, calls(method(goal, "()V", "tick", "m_8037_"), NPC, "setPos", "(DDD)V"));
        assertDoesNotThrow(() -> method(goal, "(Z)V", "navigate"));
        assertDoesNotThrow(() -> method(goal, "()V", "start", "m_8056_"));
        assertDoesNotThrow(() -> method(goal, "()Z", "canUse", "m_8036_"));
        assertTrue(goal.fields.stream().anyMatch(field -> field.name.equals("totalTicks") && field.desc.equals("I")));
    }

    @Test
    void presentationHooksMatchInstalledEntityAndRenderers()
    {
        assertDoesNotThrow(() ->
        {
            method(read(NPC), "(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V", "m_7355_", "playStepSound");
            method(read(NPC), "()Lnet/minecraft/world/item/ItemStack;", "m_21205_", "getMainHandItem");
            ClassNode renderer = read("noppes/npcs/client/renderer/RenderNPCInterface");
            method(renderer, "(Lnoppes/npcs/entity/EntityNPCInterface;)V", "renderColor");
            method(renderer, "(Lnoppes/npcs/entity/EntityNPCInterface;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", "render");
            method(read("noppes/npcs/client/renderer/RenderCustomNpc"),
                "(Lnoppes/npcs/entity/EntityCustomNpc;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", "render");
        });
    }

    @Test
    void npcDamageHooksKeepTheExpectedProductionDescriptors() throws IOException
    {
        ClassNode npc = read(NPC);
        assertDoesNotThrow(() ->
        {
            method(npc, "(Lnet/minecraft/world/damagesource/DamageSource;F)F", "m_21161_", "getDamageAfterArmorAbsorb");
            method(npc, "(Lnet/minecraft/world/entity/Entity;)Z", "m_7327_", "doHurtTarget");
            method(npc, "(I)I", "m_7302_", "decreaseAirSupply");
            method(npc, "(FFLnet/minecraft/world/damagesource/DamageSource;)Z", "m_142535_", "causeFallDamage");
            method(npc, "()Z", "m_5825_", "fireImmune");
            method(npc, "()V", "updateTasks");
        });
    }

    @Test
    void partHealthHooksTheMethodThatTakesTheHealthAndTheSaveMethodsTheEntityDeclares() throws IOException
    {
        ClassNode npc = read(NPC);
        // The health a hit takes is handed to the part struck between the loss and the death check, which only an
        // injection into a method the entity itself declares can reach.
        MethodNode actuallyHurt = method(npc, "(Lnet/minecraft/world/damagesource/DamageSource;F)V", "m_6475_", "actuallyHurt");
        assertTrue(invokes(actuallyHurt, "net/minecraft/world/entity/PathfinderMob", "m_6475_", "(Lnet/minecraft/world/damagesource/DamageSource;F)V")
            || invokes(actuallyHurt, "net/minecraft/world/entity/PathfinderMob", "actuallyHurt", "(Lnet/minecraft/world/damagesource/DamageSource;F)V"));
        assertDoesNotThrow(() ->
        {
            method(npc, "(Lnet/minecraft/nbt/CompoundTag;)V", "m_7380_", "addAdditionalSaveData");
            method(npc, "(Lnet/minecraft/nbt/CompoundTag;)V", "m_7378_", "readAdditionalSaveData");
        });
    }

    @Test
    void incomingDamageAndKnockbackHaveExactlyOneEditorResistanceRedirect() throws IOException
    {
        ClassNode npc = read(NPC);
        MethodNode hurt = method(npc, "(Lnet/minecraft/world/damagesource/DamageSource;F)Z", "m_6469_", "hurt");
        long resistanceCalls = 0;
        for (var instruction : hurt.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals("noppes/npcs/Resistances") && call.name.equals("applyResistance"))
                resistanceCalls++;
        assertEquals(1, resistanceCalls);
        MethodNode knockback = method(npc, "(DDD)V", "m_147240_", "knockback");
        long resistanceFields = 0;
        for (var instruction : knockback.instructions)
            if (instruction instanceof FieldInsnNode field && field.owner.equals("noppes/npcs/Resistances") && field.name.equals("knockback"))
                resistanceFields++;
        assertEquals(1, resistanceFields);
    }

    @Test
    void meleeGoalCallsItemReplaceableDelayReachAndProjectileGate() throws IOException
    {
        ClassNode goal = read("noppes/npcs/ai/EntityAIAttackTarget");
        MethodNode tick = method(goal, "()V", "m_8037_", "tick");
        assertTrue(invokes(tick, "noppes/npcs/entity/data/DataMelee", "getDelay", "()I"));
        assertTrue(invokes(tick, "noppes/npcs/entity/data/DataMelee", "getRange", "()I"));
        // Delay is chosen once before the one swing/hit, so alternating hands cannot create another cooldown channel.
        assertEquals(1, calls(tick, "noppes/npcs/entity/data/DataMelee", "getDelay", "()I"));
        assertEquals(1, calls(tick, NPC, "swing", "(Lnet/minecraft/world/InteractionHand;)V"));
        assertEquals(1, calls(tick, NPC, "doHurtTarget", "(Lnet/minecraft/world/entity/Entity;)Z"));
        MethodNode start = method(goal, "()Z", "m_8036_", "canUse");
        assertTrue(invokes(start, "noppes/npcs/entity/data/DataInventory", "getProjectile", "()Lnoppes/npcs/api/item/IItemStack;"));
    }

    @Test
    void clientAnimationAndResistanceCallbacksMatchTheInstalledEditor()
    {
        assertDoesNotThrow(() ->
        {
            method(read("noppes/npcs/client/model/animation/AnimationHandler"), "(Lnoppes/npcs/ModelData;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
                "animateBipedPost");
            ClassNode gui = read("noppes/npcs/client/gui/SubGuiNpcResistanceProperties");
            method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiSliderNop;)V", "mouseDragged");
            method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiSliderNop;)V", "mouseReleased");
            method(gui, "()V", "m_7856_", "init");
        });
    }

    @Test
    void rangedGoalReadsItsBurstItsIntervalAndItsPauseExactlyOnceEach() throws IOException
    {
        MethodNode tick = method(read("noppes/npcs/ai/EntityAIRangedAttack"), "()V", "m_8037_", "tick");
        // The reload cycle counts triggers on the interval read and the burst-end reload on the pause read.
        for (String getter : List.of("getBurst", "getBurstDelay", "getDelayRNG"))
            assertEquals(1, calls(tick, "noppes/npcs/entity/data/DataRanged", getter, "()I"), getter);
    }

    @Test
    void simulationHooksTargetMethodsTheEntityDeclaresAndOverrideOnlyOnesItDoesNot() throws IOException
    {
        ClassNode npc = read(NPC);
        assertDoesNotThrow(() ->
        {
            method(npc, "(Lnet/minecraft/world/phys/Vec3;)V", "m_7023_", "travel");
            method(npc, "()F", "m_6108_", "getWaterSlowDown");
            method(npc, "(Lnet/minecraft/world/entity/Entity;D)Z", "isInRange");
            // Minecraft 1.21 reads the eye height from the dimensions this method returns.
            method(npc, "(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", "getDimensions");
        });
        // The mixin adds these as overrides; a method of the same name in the target would silently win or clash.
        assertFalse(declares(npc, "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", "m_19956_", "positionRider"));
        assertFalse(declares(npc, "()Lnet/minecraft/world/phys/AABB;", "m_6921_", "getBoundingBoxForCulling"));
        assertFalse(declares(npc, "(Lnet/minecraft/world/entity/Entity;)Z", "m_142582_", "hasLineOfSight"));
        ClassNode custom = read("noppes/npcs/entity/EntityCustomNpc");
        // A worn model replaces the dimensions here, so the eye height is hooked in this method as well.
        assertTrue(declares(custom, "(Lnet/minecraft/world/entity/Pose;)Lnet/minecraft/world/entity/EntityDimensions;", "getDimensions"));
        assertFalse(declares(custom, "(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity$MoveFunction;)V", "m_19956_", "positionRider"));
        assertFalse(declares(custom, "()Lnet/minecraft/world/phys/AABB;", "m_6921_", "getBoundingBoxForCulling"));
        assertFalse(declares(custom, "(Lnet/minecraft/world/entity/Entity;)Z", "m_142582_", "hasLineOfSight"));
    }

    @Test
    void navigationAndTargetingHooksMatchTheInstalledGoals() throws IOException
    {
        ClassNode ranged = read("noppes/npcs/ai/EntityAIRangedAttack");
        assertTrue(ranged.fields.stream().anyMatch(field -> field.name.equals("moveTries") && field.desc.equals("I")));
        assertDoesNotThrow(
            () -> method(read("noppes/npcs/ai/NpcWalkNodeEvaluator"), "(Lnet/minecraft/world/entity/Mob;III)Lnet/minecraft/world/level/pathfinder/PathType;", "m_77567_", "getCachedBlockType"));
        ClassNode navigator = read("noppes/npcs/ai/NpcGroundPathNavigator");
        assertEquals("net/minecraft/world/entity/ai/navigation/GroundPathNavigation", navigator.superName);
        assertFalse(declares(navigator, "(Lnet/minecraft/world/entity/Entity;D)Z", "m_5624_", "moveTo"));
        assertFalse(declares(navigator, "(Ljava/util/Set;IZIF)Lnet/minecraft/world/level/pathfinder/Path;", "m_148222_", "createPath"));
        ClassNode goal = read("noppes/npcs/ai/target/NpcNearestAttackableTargetGoal");
        assertEquals("net/minecraft/world/entity/ai/goal/target/NearestAttackableTargetGoal", goal.superName);
        assertFalse(declares(goal, "()Z", "m_8036_", "canUse"));
        assertFalse(declares(goal, "()D", "m_7623_", "getFollowDistance"));
        // Its own check for giving a target up asks for the follow distance, which the mixin extends by the margin.
        MethodNode keep = method(goal, "()Z", "m_8045_", "canContinueToUse");
        assertTrue(invokes(keep, "noppes/npcs/ai/target/NpcNearestAttackableTargetGoal", "m_7623_", "()D")
            || invokes(keep, "noppes/npcs/ai/target/NpcNearestAttackableTargetGoal", "getFollowDistance", "()D"));
    }

    @Test
    void tacticHooksMatchTheInstalledGoalSetUpAndAvoidGoal() throws IOException
    {
        String projectile = "()Lnoppes/npcs/api/item/IItemStack;";
        // The NeoForge build has no tactical variants: the set-up checks the projectile once, for the ranged goal.
        MethodNode response = method(read(NPC), "()V", "setResponse");
        assertEquals(1, calls(response, "noppes/npcs/entity/data/DataInventory", "getProjectile", projectile));
        for (String tactic : List.of("EntityAIDodgeShoot", "EntityAIOrbitTarget", "EntityAIAmbushTarget", "EntityAIStalkTarget", "EntityAIZigZagTarget"))
            assertFalse(installed("noppes/npcs/ai/" + tactic), tactic);
        // The avoid-target goal is held back while a tactic of the mod's own runs, through the NPC field its mixin shadows.
        ClassNode avoid = read("noppes/npcs/ai/EntityAIAvoidTarget");
        assertTrue(hasNpcField(avoid, "npc"));
        assertEquals(0, calls(method(avoid, "()Z", "canUse"), "noppes/npcs/entity/data/DataInventory", "getProjectile", projectile));
        assertDoesNotThrow(() -> method(read("noppes/npcs/ai/EntityAIRangedAttack"), "()Z", "hasFired"));
    }

    @Test
    void customNpcsRangedGoalAdvancesAndHaltsOnceEachSoATacticCanTakeItsMovementOver() throws IOException
    {
        MethodNode tick = method(read("noppes/npcs/ai/EntityAIRangedAttack"), "()V", "m_8037_", "tick");
        String navigation = "net/minecraft/world/entity/ai/navigation/PathNavigation";
        assertEquals(1, calls(tick, navigation, "moveTo", "(Lnet/minecraft/world/entity/Entity;D)Z"));
        assertEquals(1, calls(tick, navigation, "stop", "()V"));
    }

    private static boolean hasNpcField(ClassNode goal, String name)
    {
        return goal.fields.stream().anyMatch(field -> field.name.equals(name) && field.desc.equals("L" + NPC + ";"));
    }

    @Test
    void onlyTheApproachGoalClaimsTheMovementOfAnNpcWithAnEquippedRangedWeapon() throws IOException
    {
        // Custom NPCs' tactics need both flags, which a firing goal holding either of them at its priority would keep from them.
        assertFalse(setsFlags(ownConstructor("NpcEquipmentRangedGoal")));
        assertTrue(setsFlags(ownConstructor("NpcCombatApproachGoal")));
    }

    private static long calls(MethodNode method, String owner, String name, String descriptor)
    {
        long calls = 0;
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor))
                calls++;
        return calls;
    }

    private static boolean setsFlags(MethodNode method)
    {
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && List.of("setFlags", "m_7021_").contains(call.name) && call.desc.equals("(Ljava/util/EnumSet;)V"))
                return true;
        return false;
    }

    /** Reads a class of this package as bytes, without loading it or the Minecraft and Custom NPCs classes it uses. */
    private static MethodNode ownConstructor(String simpleName) throws IOException
    {
        try (var input = NpcEquipmentHookContractTest.class.getResourceAsStream(simpleName + ".class"))
        {
            assertNotNull(input, simpleName);
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node.methods.stream().filter(method -> method.name.equals("<init>")).findFirst().orElseThrow(() -> new AssertionError("Missing constructor of " + simpleName));
        }
    }

    @Test
    void restoredNativeTacticsHaveStableDataEditorAndRegistrationHooks() throws IOException
    {
        ClassNode ai = read("noppes/npcs/entity/data/DataAI");
        method(ai, "(Lnet/minecraft/nbt/CompoundTag;)V", "readToNBT");
        method(ai, "(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;", "save");
        ClassNode gui = read("noppes/npcs/client/gui/mainmenu/GuiNpcAI");
        method(gui, "()V", "init");
        method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiButtonNop;)V", "buttonEvent");
        method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiTextFieldNop;)V", "unFocused");
        MethodNode response = method(read(NPC), "()V", "setResponse");
        int attack = -1;
        int inventory = -1;
        int inventories = 0;
        for (int index = 0; index < response.instructions.size(); index++)
        {
            var instruction = response.instructions.get(index);
            if (instruction instanceof TypeInsnNode type && type.desc.equals("noppes/npcs/ai/EntityAIAttackTarget"))
                attack = index;
            if (instruction instanceof FieldInsnNode field && field.owner.equals(NPC) && field.name.equals("inventory"))
            {
                inventory = index;
                inventories++;
            }
        }
        // This dependency adds melee before checking the slot: the restored goal has to lower its priority too.
        assertEquals(1, inventories);
        assertTrue(attack >= 0 && inventory > attack);
        assertTrue(read(NPC).fields.stream().anyMatch(field -> field.name.equals("taskCount") && field.desc.equals("I")));
    }

    private static boolean declares(ClassNode owner, String descriptor, String... names)
    {
        return owner.methods.stream().anyMatch(method -> List.of(names).contains(method.name) && method.desc.equals(descriptor));
    }

    private static boolean invokes(MethodNode method, String owner, String name, String descriptor)
    {
        for (var instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(descriptor))
                return true;
        return false;
    }

    private static MethodNode method(ClassNode owner, String descriptor, String... names)
    {
        return owner.methods.stream().filter(method -> List.of(names).contains(method.name) && method.desc.equals(descriptor)).findFirst()
            .orElseThrow(() -> new AssertionError("Missing equipment hook: " + owner.name + " " + List.of(names) + descriptor));
    }

    private static boolean installed(String name) throws IOException
    {
        try (ZipFile jar = new ZipFile(CUSTOM_NPCS.toFile()))
        {
            return jar.getEntry(name + ".class") != null;
        }
    }

    private static ClassNode read(String name) throws IOException
    {
        try (ZipFile jar = new ZipFile(CUSTOM_NPCS.toFile()); var input = jar.getInputStream(jar.getEntry(name + ".class")))
        {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
