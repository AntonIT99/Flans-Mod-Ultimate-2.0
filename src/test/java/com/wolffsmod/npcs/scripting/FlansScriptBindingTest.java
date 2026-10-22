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
    private static final Path JAR = Path.of("libs/CustomNPCs-Unofficial-NeoForge-1.21.1.20251230.jar");

    /** The NeoForge build has no separate script executors: the container puts the shared bindings into its engine itself. */
    @Test
    void customNpcsPassesTheSharedBindingsToItsScriptEngine() throws IOException
    {
        ClassNode container = read("noppes/npcs/controllers/ScriptContainer");
        FieldNode bindings = container.fields.stream().filter(field -> field.name.equals("Data")).findFirst().orElseThrow();
        assertEquals(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, bindings.access);
        boolean usesBindings = false;
        boolean bindsEngine = false;
        MethodNode setEngine = container.methods.stream().filter(method -> method.name.equals("setEngine")).findFirst().orElseThrow();
        for (AbstractInsnNode instruction : setEngine.instructions)
        {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(container.name) && field.name.equals("Data"))
                usesBindings = true;
            if (instruction instanceof MethodInsnNode call && call.owner.equals("javax/script/ScriptEngine") && call.name.equals("put"))
                bindsEngine = true;
        }
        assertTrue(usesBindings && bindsEngine, "The registered flans global must enter each script engine");
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
