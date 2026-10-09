package com.flansmodultimate;

import com.flansmodultimate.common.inventory.ArmorBoxMenu;
import com.flansmodultimate.common.inventory.DriveableCraftingMenu;
import com.flansmodultimate.common.inventory.DriveableInventoryMenu;
import com.flansmodultimate.common.inventory.GunBoxMenu;
import com.flansmodultimate.common.inventory.GunWorkbenchMenu;
import com.flansmodultimate.common.inventory.MechaInventoryMenu;
import com.flansmodultimate.common.inventory.PaintjobTableMenu;
import com.flansmodultimate.platform.menu.MenuPlatform;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import net.minecraft.world.inventory.MenuType;

import java.util.function.Supplier;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class FlansModMenus
{
    public static final Supplier<? extends MenuType<GunWorkbenchMenu>> gunWorkbenchMenu = FlansModRegistries.menuRegistry.register("gunworkbench_menu",
        () -> MenuPlatform.menuType((windowId, inv, buf) -> new GunWorkbenchMenu(windowId, inv, buf.readBlockPos())));
    public static final Supplier<? extends MenuType<DriveableCraftingMenu>> driveableCraftingMenu = FlansModRegistries.menuRegistry.register("driveable_crafting_menu",
        () -> MenuPlatform.menuType((windowId, inv, buf) -> new DriveableCraftingMenu(windowId, inv, buf.readBlockPos())));
    public static final Supplier<? extends MenuType<DriveableInventoryMenu>> driveableInventoryMenu = FlansModRegistries.menuRegistry.register("driveable_inventory_menu",
        () -> MenuPlatform.menuType(DriveableInventoryMenu::createFromNetwork));
    public static final Supplier<? extends MenuType<MechaInventoryMenu>> mechaInventoryMenu = FlansModRegistries.menuRegistry.register("mecha_inventory_menu",
        () -> MenuPlatform.menuType(MechaInventoryMenu::createFromNetwork));
    public static final Supplier<? extends MenuType<PaintjobTableMenu>> paintjobTableMenu = FlansModRegistries.menuRegistry.register("paintjob_table_menu",
        () -> MenuPlatform.menuType(PaintjobTableMenu::createFromNetwork));
    public static final Supplier<? extends MenuType<ArmorBoxMenu>> armorBoxMenu = FlansModRegistries.menuRegistry.register("armorbox_menu",
        () -> MenuPlatform.menuType(ArmorBoxMenu::createFromNetwork));
    public static final Supplier<? extends MenuType<GunBoxMenu>> gunBoxMenu = FlansModRegistries.menuRegistry.register("gunbox_menu", () -> MenuPlatform.menuType(GunBoxMenu::createFromNetwork));

    /** Forces supplier registration without resolving any registry entries. */
    static void initialize()
    {
        // no-op
    }
}
