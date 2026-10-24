package com.flansmodultimate.client.debug;

import com.flansmod.client.model.ModelGun;
import com.flansmod.common.vector.Vector3f;
import com.flansmodultimate.client.model.ModelCache;
import com.flansmodultimate.common.item.GunItem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/** Client-local, temporary previews of a held gun type's animAimOffset in model units. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AimOffsetDebugCommand
{
    private static final Map<String, Vector3f> overrides = new HashMap<>();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("flansdebug").then(Commands.literal("aimoffset").executes(context -> apply(context.getSource(), null, false))
            .then(Commands.literal("reset").executes(context -> apply(context.getSource(), null, true))).then(Commands.literal("clear").executes(context ->
            {
                overrides.clear();
                context.getSource().sendSuccess(() -> Component.literal("Cleared all Flan aim offset previews."), false);
                return 1;
            }))
            .then(Commands.argument("x", FloatArgumentType.floatArg())
                .then(Commands.argument("y", FloatArgumentType.floatArg()).then(Commands.argument("z", FloatArgumentType.floatArg()).executes(context -> apply(context.getSource(),
                    new Vector3f(FloatArgumentType.getFloat(context, "x"), FloatArgumentType.getFloat(context, "y"), FloatArgumentType.getFloat(context, "z")), false)))))));
        dispatcher.register(Commands.literal("flansmodultimate").then(Commands.literal("debug").redirect(dispatcher.getRoot().getChild("flansdebug"))));
    }

    public static Vector3f resolve(ModelGun model)
    {
        return overrides.getOrDefault(model.getType().getShortName(), model.getAimOffset());
    }

    private static int apply(CommandSourceStack source, @Nullable Vector3f offset, boolean reset)
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

        String shortName = gun.getConfigType().getShortName();
        if (reset)
            overrides.remove(shortName);
        else if (offset != null)
        {
            if (!Float.isFinite(offset.x) || !Float.isFinite(offset.y) || !Float.isFinite(offset.z))
            {
                source.sendFailure(Component.literal("Aim offset components must be finite numbers."));
                return 0;
            }
            overrides.put(shortName, offset);
        }

        Vector3f effective = resolve(model);
        String state = overrides.containsKey(shortName) ? "preview" : "pack/model";
        source.sendSuccess(() -> Component.literal(shortName + " (" + state + "): animAimOffset " + effective.x + " " + effective.y + " " + effective.z), false);
        return 1;
    }
}
