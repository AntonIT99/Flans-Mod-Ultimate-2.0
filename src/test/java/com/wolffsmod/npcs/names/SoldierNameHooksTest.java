package com.wolffsmod.npcs.names;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.IOException;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/** Checks production hook descriptors without loading Custom NPCs or any client classes. */
class SoldierNameHooksTest
{
    @Test
    void generationAndPersistenceHooksMatchInstalledDependency() throws IOException
    {
        MethodNode fetch = method(read("nikedemos/markovnames/generators/MarkovGenerator"), "fetch", "(II)Ljava/lang/String;");
        assertNotEquals(0, fetch.access & Opcodes.ACC_STATIC);
        ClassNode display = read("noppes/npcs/entity/data/DataDisplay");
        method(display, "setMarkovGeneratorId", "(I)V");
        method(display, "save", "(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/nbt/CompoundTag;");
        method(display, "readToNBT", "(Lnet/minecraft/nbt/CompoundTag;)V");
        assertTrue(display.fields.stream().anyMatch(field -> field.name.equals("markovGeneratorId") && field.desc.equals("I")));
    }

    @Test
    void boundNameHooksMatchProductionMethodsAndTitleCallSites() throws IOException
    {
        ClassNode display = read("noppes/npcs/entity/data/DataDisplay");
        method(display, "setName", "(Ljava/lang/String;)V");
        method(display, "setTitle", "(Ljava/lang/String;)V");
        method(read("noppes/npcs/entity/EntityNPCInterface"), "m_7755_", "()Lnet/minecraft/network/chat/Component;");
        MethodNode label = method(read("noppes/npcs/client/renderer/RenderNPCInterface"), "renderLivingLabel",
            "(Lnoppes/npcs/entity/EntityNPCInterface;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V");
        int titleReads = 0;
        for (var instruction : label.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.equals("noppes/npcs/entity/data/DataDisplay") && call.name.equals("getTitle") && call.desc.equals("()Ljava/lang/String;"))
                titleReads++;
        assertEquals(2, titleReads);
    }

    @Test
    void bothNameMenusExposeTheExpectedTenSlotSelector() throws IOException
    {
        ClassNode client = read("noppes/npcs/client/gui/SubGuiNpcName");
        MethodNode init = client.methods.stream().filter(candidate -> candidate.name.equals("init") || candidate.name.equals("m_7856_")).findFirst().orElseThrow();
        assertFirstSelector(init, "noppes/npcs/shared/client/gui/components/GuiButtonBiDirectional", "<init>", "(Lnoppes/npcs/shared/client/gui/listeners/IGuiInterface;IIIII[Ljava/lang/String;I)V");
        MethodNode api = method(read("noppes/npcs/api/gui/DisplayMenu"), "openNameSubGui",
            "(Lnoppes/npcs/api/wrapper/gui/CustomGuiWrapper;Lnoppes/npcs/entity/data/DataDisplay;Lnoppes/npcs/api/entity/IPlayer;)V");
        assertFirstSelector(api, "noppes/npcs/api/wrapper/gui/CustomGuiButtonListWrapper", "setValues", "([Ljava/lang/String;)Lnoppes/npcs/api/wrapper/gui/CustomGuiButtonListWrapper;");
    }

    private static void assertFirstSelector(MethodNode method, String owner, String name, String descriptor)
    {
        boolean tenSlotArray = false;
        boolean romanLabel = false;
        for (var instruction : method.instructions)
        {
            if (instruction instanceof IntInsnNode number && number.operand == 10)
                tenSlotArray = true;
            if (instruction instanceof LdcInsnNode constant && "markov.roman.name".equals(constant.cst))
                romanLabel = true;
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name))
            {
                assertEquals(descriptor, call.desc);
                assertTrue(tenSlotArray);
                assertTrue(romanLabel);
                return;
            }
        }
        fail("Name selector hook missing");
    }

    private static MethodNode method(ClassNode node, String name, String descriptor)
    {
        return node.methods.stream().filter(candidate -> candidate.name.equals(name) && candidate.desc.equals(descriptor)).findFirst().orElseThrow();
    }

    private static ClassNode read(String name) throws IOException
    {
        try (ZipFile jar = new ZipFile("libs/CustomNPCs-1.20.1-GBPort-Unofficial-1.20.1.20260711.jar"))
        {
            try (var stream = jar.getInputStream(jar.getEntry(name + ".class")))
            {
                ClassNode node = new ClassNode();
                new ClassReader(stream).accept(node, 0);
                return node;
            }
        }
    }
}
