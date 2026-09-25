package com.flansmodultimate.client.gui;

import com.flansmodultimate.FlansMod;
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
        <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void register(MenuType<? extends M> type,
                                                                                          MenuScreens.ScreenConstructor<M, U> constructor);
    }

    public static void register(Registrar registrar)
    {
        registrar.register(FlansMod.gunWorkbenchMenu.get(), GunWorkbenchScreen::new);
        registrar.register(FlansMod.driveableCraftingMenu.get(), DriveableCraftingScreen::new);
        registrar.register(FlansMod.driveableInventoryMenu.get(), DriveableInventoryScreen::new);
        registrar.register(FlansMod.mechaInventoryMenu.get(), MechaInventoryScreen::new);
        registrar.register(FlansMod.paintjobTableMenu.get(), PaintjobTableScreen::new);
        registrar.register(FlansMod.armorBoxMenu.get(), ArmorBoxScreen::new);
        registrar.register(FlansMod.gunBoxMenu.get(), GunBoxScreen::new);
    }
}
