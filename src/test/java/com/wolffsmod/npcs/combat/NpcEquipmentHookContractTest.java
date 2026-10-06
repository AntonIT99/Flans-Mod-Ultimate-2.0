package com.wolffsmod.npcs.combat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Checks the installed dependency's bytecode without loading Custom NPCs or client classes on the test/server path. */
class NpcEquipmentHookContractTest
{
    private static final Path CUSTOM_NPCS = Path.of("libs/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar");
    private static final String NPC = "noppes/npcs/entity/EntityNPCInterface";

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
        assertTrue(invokes(tick, "noppes/npcs/entity/data/DataMelee", "getRange", "()F"));
        MethodNode start = method(goal, "()Z", "m_8036_", "canUse");
        assertTrue(invokes(start, "noppes/npcs/entity/data/DataInventory", "getProjectile", "()Lnoppes/npcs/api/item/IItemStack;"));
    }

    @Test
    void clientAnimationAndResistanceCallbacksMatchTheInstalledEditor()
    {
        assertDoesNotThrow(() ->
        {
            method(read("noppes/npcs/client/model/animation/AnimationHandler"),
                "(Lnoppes/npcs/ModelData;Lnet/minecraft/client/model/HumanoidModel;Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", "animateBipedPost");
            ClassNode gui = read("noppes/npcs/client/gui/SubGuiNpcResistanceProperties");
            method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiSliderNop;)V", "mouseDragged");
            method(gui, "(Lnoppes/npcs/shared/client/gui/components/GuiSliderNop;)V", "mouseReleased");
            method(gui, "()V", "m_7856_", "init");
        });
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
