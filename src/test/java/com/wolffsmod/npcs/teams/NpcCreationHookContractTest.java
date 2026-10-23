package com.wolffsmod.npcs.teams;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Checks production creation hooks without loading either client classes or the installed mod. */
class NpcCreationHookContractTest
{
    private static final Path CUSTOM_NPCS = Path.of("libs/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar");

    @Test
    void wandAddsExactlyOneEntityInItsUseOnMethod() throws IOException
    {
        assertEquals(1, calls("noppes/npcs/items/ItemNpcWand", Set.of("useOn", "m_6225_"), "(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;",
            "net/minecraft/world/level/Level", Set.of("addFreshEntity", "m_7967_"), "(Lnet/minecraft/world/entity/Entity;)Z"));
    }

    @Test
    void playerPacketSpawnsExactlyOneCloneThroughTheExpectedHelper() throws IOException
    {
        assertEquals(1, calls("noppes/npcs/packets/server/SPacketToolMobSpawner", Set.of("handle"), "()V", "noppes/npcs/packets/server/SPacketToolMobSpawner", Set.of("spawnClone"),
            "(Lnet/minecraft/nbt/CompoundTag;DDDLnet/minecraft/world/level/Level;)Lnet/minecraft/world/entity/Entity;"));
    }

    @Test
    void npcCreateCommandStillAddsAnEntityInTheExpectedCommandLambda() throws IOException
    {
        assertEquals(1, calls("noppes/npcs/command/CmdNPC", Set.of("lambda$register$11"), "(Lcom/mojang/brigadier/context/CommandContext;)I", "net/minecraft/world/level/Level",
            Set.of("addFreshEntity", "m_7967_"), "(Lnet/minecraft/world/entity/Entity;)Z"));
    }

    @Test
    void cloneSpawnAndGridStillShareTheExpectedCommandHelper() throws IOException
    {
        assertEquals(1, calls("noppes/npcs/command/CmdClone", Set.of("spawnClone"), "(Lcom/mojang/brigadier/context/CommandContext;Lnet/minecraft/core/BlockPos;Ljava/lang/String;)V",
            "net/minecraft/world/level/Level", Set.of("addFreshEntity", "m_7967_"), "(Lnet/minecraft/world/entity/Entity;)Z"));
    }

    private static int calls(String type, Set<String> methods, String descriptor, String owner, Set<String> names, String targetDescriptor) throws IOException
    {
        try (ZipFile jar = new ZipFile(CUSTOM_NPCS.toFile()))
        {
            ClassNode node = new ClassNode();
            new ClassReader(jar.getInputStream(jar.getEntry(type + ".class"))).accept(node, 0);
            var method = node.methods.stream().filter(candidate -> methods.contains(candidate.name) && candidate.desc.equals(descriptor)).findFirst().orElseThrow();
            int count = 0;
            for (var instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && names.contains(call.name) && call.desc.equals(targetDescriptor))
                    count++;
            return count;
        }
    }
}
