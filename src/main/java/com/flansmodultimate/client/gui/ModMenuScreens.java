package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansModMenus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** The screen of each Flan's Mod menu. Client-only. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ModMenuScreens
{
    /** The loader's screen registration: {@code MenuScreens::register} or the registration event's {@code register}. */
    @FunctionalInterface
    public interface Registrar
    {
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type, MenuScreens.ScreenConstructor<M, U> constructor);
    }

    public static void register(Registrar registrar)
    {
        registrar.register(FlansModMenus.gunWorkbenchMenu.get(), GunWorkbenchScreen::new);
        registrar.register(FlansModMenus.driveableCraftingMenu.get(), DriveableCraftingScreen::new);
        registrar.register(FlansModMenus.driveableInventoryMenu.get(), DriveableInventoryScreen::new);
        registrar.register(FlansModMenus.mechaInventoryMenu.get(), MechaInventoryScreen::new);
        registrar.register(FlansModMenus.paintjobTableMenu.get(), PaintjobTableScreen::new);
        registrar.register(FlansModMenus.armorBoxMenu.get(), ArmorBoxScreen::new);
        registrar.register(FlansModMenus.gunBoxMenu.get(), GunBoxScreen::new);
    }
}
