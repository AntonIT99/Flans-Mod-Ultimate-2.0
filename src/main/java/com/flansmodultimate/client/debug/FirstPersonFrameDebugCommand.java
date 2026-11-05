package com.flansmodultimate.client.debug;

import com.flansmod.client.model.ModelGun;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.GunItem;
import com.flansmodultimate.common.types.EnumGunFirstPersonFrame;
import com.flansmodultimate.common.types.GunType;
import com.mojang.brigadier.CommandDispatcher;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/** Client-local previews of the held gun type's authored FirstPersonFrame property. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FirstPersonFrameDebugCommand
{
    private static final Map<GunType, EnumGunFirstPersonFrame> overrides = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansdebug").then(Commands.literal("authoredFrame").executes(context -> apply(context.getSource(), null, false))
            .then(Commands.literal("1.7.10").executes(context -> apply(context.getSource(), EnumGunFirstPersonFrame.FLANS_1710, false)))
            .then(Commands.literal("1.12.2").executes(context -> apply(context.getSource(), EnumGunFirstPersonFrame.FLANS_1122, false)))
            .then(Commands.literal("reset").executes(context -> apply(context.getSource(), null, true)))
            .then(Commands.literal("clear").executes(context ->
            {
                overrides.clear();
                context.getSource().sendSuccess(() -> Component.literal("Cleared all Flan FirstPersonFrame property previews."), false);
                return 1;
            }))));
    }

    public static EnumGunFirstPersonFrame resolve(ModelGun model)
    {
        GunType type = model.getType();
        EnumGunFirstPersonFrame configured = type.getFirstPersonFrame();
        return overrides.getOrDefault(type, configured != null ? configured : EnumGunFirstPersonFrame.FLANS_1710);
    }

    private static int apply(CommandSourceStack source, @Nullable EnumGunFirstPersonFrame selected, boolean reset)
    {
        var player = Minecraft.getInstance().player;
        if (player == null)
            return 0;
        var stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof GunItem))
            stack = player.getOffhandItem();
        if (!(stack.getItem() instanceof GunItem gun) || !(ModelCache.getOrLoadTypeModel(gun.getConfigType()) instanceof ModelGun model))
        {
            source.sendFailure(Component.literal("Hold a Flan gun with a loaded gun model in either hand."));
            return 0;
        }

        GunType type = model.getType();
        if (reset)
            overrides.remove(type);
        else if (selected != null)
            overrides.put(type, selected);

        String state = overrides.containsKey(type) ? "preview" : "pack/default";
        source.sendSuccess(() -> Component.literal(type.getShortName() + " (" + state + "): FirstPersonFrame " + resolve(model).getDisplayName()), false);
        return 1;
    }
}
