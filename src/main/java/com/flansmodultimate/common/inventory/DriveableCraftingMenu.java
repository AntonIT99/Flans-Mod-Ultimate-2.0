package com.flansmodultimate.common.inventory;

import com.flansmodultimate.FlansMod;
import com.flansmodultimate.common.driveables.DriveableData;
import com.flansmodultimate.common.item.PartItem;
import com.flansmodultimate.common.types.DriveableType;
import com.flansmodultimate.common.types.InfoType;
import com.flansmodultimate.common.types.PartType;
import com.flansmodultimate.util.InventoryHelper;
import com.flansmodultimate.util.ModUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-authoritative driveable workbench. The client sends only the selected
 * blueprint index; recipes, engine compatibility, consumption and output NBT are
 * all resolved again on the server.
 */
public final class DriveableCraftingMenu extends AbstractContainerMenu
{
    private static final double MAX_DISTANCE_SQUARED = 64D;
    public static final int CRAFT_BUTTON_BASE = 1_000;

    private final Inventory playerInventory;
    private final ContainerLevelAccess access;

    public DriveableCraftingMenu(int containerId, Inventory playerInventory, BlockPos workbenchPos)
    {
        super(FlansMod.driveableCraftingMenu.get(), containerId);
        this.playerInventory = playerInventory;
        access = ContainerLevelAccess.create(playerInventory.player.level(), workbenchPos);
    }

    public static List<DriveableType> getBlueprints()
    {
        return InfoType.getInfoTypes().values().stream()
            .filter(DriveableType.class::isInstance)
            .map(DriveableType.class::cast)
            .distinct()
            .sorted(Comparator.comparing(DriveableType::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(DriveableType::getShortName, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    @Override
    public boolean clickMenuButton(@NotNull Player player, int id)
    {
        int blueprintIndex = id - CRAFT_BUTTON_BASE;
        List<DriveableType> blueprints = getBlueprints();
        if (blueprintIndex < 0 || blueprintIndex >= blueprints.size() || !stillValid(player))
            return false;
        if (!player.level().isClientSide)
            craft(player, blueprints.get(blueprintIndex));
        return true;
    }

    public boolean canCraft(@NotNull DriveableType type)
    {
        if (ModUtils.getItem(type).isEmpty())
            return false;
        if (type.numEngines() <= 0)
            return playerInventory.player.getAbilities().instabuild
                || InventoryHelper.canConsumeAll(playerInventory, type.getDriveableRecipe());
        if (playerInventory.player.getAbilities().instabuild)
            return findBestEngine(playerInventory, type, true) != null;
        PartType engine = findBestEngine(playerInventory, type, false);
        return engine != null && InventoryHelper.canConsumeAll(playerInventory, getCompleteRecipe(type, engine));
    }

    @Nullable
    public PartType getBestEngine(@NotNull DriveableType type)
    {
        return findBestEngine(playerInventory, type, playerInventory.player.getAbilities().instabuild);
    }

    private static void craft(Player player, DriveableType type)
    {
        boolean creative = player.getAbilities().instabuild;
        PartType engine = findBestEngine(player.getInventory(), type, creative);
        if (engine == null && type.numEngines() > 0)
            return;

        ItemStack output = ModUtils.getItemStack(type).orElse(ItemStack.EMPTY);
        if (output.isEmpty())
            return;

        List<ItemStack> completeRecipe = getCompleteRecipe(type, engine);
        if (!InventoryHelper.tryConsumeAll(player.getInventory(), completeRecipe, creative))
            return;

        DriveableData data = new DriveableData(type);
        data.setEngineShortName(engine == null ? "" : engine.getShortName());
        data.copyToStack(output);

        if (!player.getInventory().add(output))
            player.drop(output, false);
        player.getInventory().setChanged();
    }

    private static List<ItemStack> getCompleteRecipe(DriveableType type, @Nullable PartType engine)
    {
        List<ItemStack> required = new ArrayList<>();
        type.getDriveableRecipe().stream().filter(stack -> stack != null && !stack.isEmpty()).map(ItemStack::copy).forEach(required::add);
        int engineCount = Math.max(0, type.numEngines());
        if (engineCount > 0 && engine != null)
            ModUtils.getItemStack(engine, engineCount).ifPresent(required::add);
        return required;
    }

    /**
     * Finds the fastest compatible engine after first reserving the normal recipe
     * ingredients. This prevents recipe entries and the engine requirement from
     * claiming the same item stack.
     */
    @Nullable
    private static PartType findBestEngine(Container inventory, DriveableType type, boolean creative)
    {
        SimpleContainer available = copyOf(inventory);
        if (!creative && !InventoryHelper.tryConsumeAll(available, type.getDriveableRecipe(), false))
            return null;

        int enginesNeeded = Math.max(0, type.numEngines());
        if (enginesNeeded == 0)
            return null;
        Map<PartType, Integer> counts = new IdentityHashMap<>();
        for (int slot = 0; slot < available.getContainerSize(); slot++)
        {
            ItemStack stack = available.getItem(slot);
            if (!(stack.getItem() instanceof PartItem partItem))
                continue;
            PartType part = partItem.getConfigType();
            if (part.getCategory() == PartType.Category.ENGINE && part.worksWith(type.getType()))
                counts.merge(part, stack.getCount(), Integer::sum);
        }

        Comparator<PartType> bestFirst = Comparator.comparingDouble(PartType::getEngineSpeed).reversed()
            .thenComparing(Comparator.comparingDouble(PartType::getEnginePower).reversed())
            .thenComparing(PartType::getShortName, String.CASE_INSENSITIVE_ORDER);
        PartType best = counts.entrySet().stream()
            .filter(entry -> creative || entry.getValue() >= enginesNeeded)
            .map(Map.Entry::getKey)
            .sorted(bestFirst)
            .findFirst()
            .orElse(null);
        if (best != null)
            return best;

        PartType fallback = PartType.getDefaultEngine(type.getType(), type.getContentPack(), type.getEngine());
        return creative && fallback != null && fallback.worksWith(type.getType()) ? fallback : null;
    }

    private static SimpleContainer copyOf(Container inventory)
    {
        SimpleContainer copy = new SimpleContainer(inventory.getContainerSize());
        for (int slot = 0; slot < inventory.getContainerSize(); slot++)
            copy.setItem(slot, inventory.getItem(slot).copy());
        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player)
    {
        return access.evaluate((level, pos) -> (level.getBlockState(pos).is(FlansMod.vehicleCraftingTable.get()) || level.getBlockState(pos).is(FlansMod.gunWorkbench.get()))
            && player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) <= MAX_DISTANCE_SQUARED, false);
    }

    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index)
    {
        return ItemStack.EMPTY;
    }
}
