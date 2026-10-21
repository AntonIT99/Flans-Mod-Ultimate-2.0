package com.wolffsmod.npcs.scripting;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Verifies the installed executor contract without loading Custom NPCs on the ordinary test/server classpath. */
class FlansScriptBindingTest
{
    private static final Path JAR = Path.of("libs/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar");

    @Test
    void customNpcsPassesTheSharedBindingsToItsScriptExecutor() throws IOException
    {
        ClassNode container = read("noppes/npcs/controllers/ScriptContainer");
        FieldNode bindings = container.fields.stream().filter(field -> field.name.equals("Data")).findFirst().orElseThrow();
        assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, bindings.access);
        boolean usesBindings = false;
        boolean initializes = false;
        MethodNode create = container.methods.stream().filter(method -> method.name.equals("getOrCreateExecutor")).findFirst().orElseThrow();
        for (AbstractInsnNode instruction : create.instructions)
        {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(container.name) && field.name.equals("Data"))
                usesBindings = true;
            if (instruction instanceof MethodInsnNode call && call.owner.equals("noppes/npcs/controllers/IScriptExecutor") && call.name.equals("initialize"))
                initializes = true;
        }
        assertTrue(usesBindings && initializes, "The registered flans global must be passed to each executor");
        ClassNode executor = read("noppes/npcs/controllers/Jsr223Executor");
        MethodNode initialize = executor.methods.stream().filter(method -> method.name.equals("initialize")).findFirst().orElseThrow();
        boolean bindsEngine = false;
        for (AbstractInsnNode instruction : initialize.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals("javax/script/ScriptEngine") && call.name.equals("put"))
                bindsEngine = true;
        assertTrue(bindsEngine, "Bindings must enter the script engine before it evaluates the script");
    }

    @Test
    void wrapperConversionsKeepTheInstalledUnmappedPublicSignatures() throws IOException
    {
        assertTrue(read("noppes/npcs/api/entity/IEntity").methods.stream().anyMatch(method -> method.name.equals("getMCEntity") && method.desc.equals("()Lnet/minecraft/world/entity/Entity;")));
        assertTrue(read("noppes/npcs/api/item/IItemStack").methods.stream().anyMatch(method -> method.name.equals("getMCItemStack") && method.desc.equals("()Lnet/minecraft/world/item/ItemStack;")));
    }

    private static ClassNode read(String name) throws IOException
    {
        try (ZipFile jar = new ZipFile(JAR.toFile()); var input = jar.getInputStream(jar.getEntry(name + ".class")))
        {
            ClassNode node = new ClassNode();
            new ClassReader(input).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return node;
        }
    }
}
