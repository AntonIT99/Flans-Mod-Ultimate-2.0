package com.flansmodultimate.client.damage;

import com.flansmodultimate.common.driveables.EnumDriveablePart;
import com.flansmodultimate.common.driveables.visual.DriveableVisual;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Stand-ins that draw a single piece of a driveable through its own model.
 *
 * <p>
 * Every part a model draws is gated by whether the visual still draws it, turret, wings, wheels and limbs alike, with
 * each part's own pivots and animation. A stand-in that answers yes only for the parts of a piece therefore draws
 * exactly that piece, however the model is built, and passes everything else through to the visual it stands in for.
 * It takes on every visual interface its original has, so a plane's piece still reads as a plane to the model.
 * </p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class PieceVisuals
{
    /** The scorched colour a piece blown off a burning machine is drawn in. */
    private static final float BURNT_TINT = 0.45F;

    static DriveableVisual of(DriveableVisual original, Set<EnumDriveablePart> parts, boolean burnt)
    {
        List<Class<?>> interfaces = ClassUtils.getAllInterfaces(original.getClass()).stream().filter(DriveableVisual.class::isAssignableFrom).toList();
        EnumSet<EnumDriveablePart> drawn = parts.isEmpty() ? EnumSet.noneOf(EnumDriveablePart.class) : EnumSet.copyOf(parts);
        InvocationHandler handler = new Handler(original, drawn, burnt);
        return (DriveableVisual) Proxy.newProxyInstance(PieceVisuals.class.getClassLoader(), interfaces.toArray(new Class<?>[0]), handler);
    }

    private record Handler(DriveableVisual original, Set<EnumDriveablePart> parts, boolean burnt) implements InvocationHandler
    {
        // Every other call goes through to the visual the stand-in stands in for.
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            String name = method.getName();
            if (args != null && args.length == 1 && (args[0] == null || args[0] instanceof EnumDriveablePart))
            {
                if ("isPartDrawn".equals(name) || "isPartIntact".equals(name))
                    return args[0] != null && parts.contains(args[0]);
                if ("getPartTint".equals(name))
                    return burnt ? BURNT_TINT : original.getPartTint((EnumDriveablePart) args[0]);
            }
            if (method.getDeclaringClass() == Object.class)
                return switch (name)
                {
                    case "equals" -> args != null && proxy == args[0];
                    case "hashCode" -> System.identityHashCode(proxy);
                    default -> "PieceVisual" + parts;
                };
            try
            {
                return method.invoke(original, args);
            }
            catch (InvocationTargetException ex)
            {
                throw ex.getCause();
            }
        }
    }
}
