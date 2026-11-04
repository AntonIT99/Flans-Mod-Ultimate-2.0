package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.driveables.IDriveableType;
import com.wolffsmod.npcs.model.*;
import com.wolffsmod.npcs.teams.BotSettings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.*;

/** Edits vehicle allocations on a private copy, preserving separate allocations and spawner bindings. */
final class BotVehiclesPicker extends BotPickerScreen
{
    private enum Category
    {
        MECHAS, PLANES, SHIPS, VEHICLES, AA
    }

    private final BotEditorScreen editor;
    private final BotSettings.TeamForce force;
    private final List<BotSettings.VehicleEntry> entries = new ArrayList<>();
    private final Map<String, LivingEntity> previews = new HashMap<>();
    private final List<String> packs = new ArrayList<>();
    private Category category = Category.VEHICLES;
    /** Empty means all content packs. Filtering never changes the allocation list. */
    private String pack = "";

    BotVehiclesPicker(BotEditorScreen parent, String team, BotSettings.TeamForce force)
    {
        super(parent, Component.translatable(PREFIX + "vehicles.title", team), Component.translatable(PREFIX + "vehicles.hint"));
        editor = parent;
        this.force = force;
        entries.addAll(force.getVehicles());
        Set<String> included = new HashSet<>();
        entries.forEach(entry -> included.add(entry.type()));
        FlanModelEntities.getEntityTypes().stream().sorted(Comparator.comparing(type -> displayName(type).getString(), String.CASE_INSENSITIVE_ORDER)).filter(type -> included.add(type.getShortName()))
            .forEach(type -> entries.add(new BotSettings.VehicleEntry(type.getShortName(), 0, 0, 0, 1, null, "")));
        packs.add("");
        FlanModelEntities.getEntityTypes().stream().map(FlanModelEntityType::getInfoType).filter(Objects::nonNull).map(type -> type.getContentPack().getName()).distinct()
            .sorted(String.CASE_INSENSITIVE_ORDER).forEach(packs::add);
        rebuildChoices();
    }

    private static Component displayName(FlanModelEntityType type)
    {
        var definition = type.getInfoType();
        Component name = definition == null ? type.getDescription() : definition.getNameComponent();
        if (!name.getString().isBlank())
            return name;
        String fallback = definition == null ? "" : definition.getName();
        return Component.literal(fallback.isBlank() ? type.getShortName() : fallback);
    }

    private static Category categoryOf(FlanModelEntityType type)
    {
        if (type.getInfoType() instanceof IDriveableType driveable && driveable.isShip())
            return Category.SHIPS;
        return switch (type.getKind())
        {
            case MECHA -> Category.MECHAS;
            case PLANE -> Category.PLANES;
            case VEHICLE -> Category.VEHICLES;
            case AA_GUN -> Category.AA;
        };
    }

    @Override
    protected int headingHeight()
    {
        return BUTTON_HEIGHT + GAP;
    }

    @Override
    protected void addHeadings(int x, int y)
    {
        int half = (getListWidth() + getPreviewWidth()) / 2;
        addRenderableWidget(CycleButton.<Category>builder(value -> Component.translatable(PREFIX + "vehicles.category." + value.name().toLowerCase(Locale.ROOT))).withValues(Category.values())
            .withInitialValue(category).create(getLeft(), y, half, BUTTON_HEIGHT, Component.translatable(PREFIX + "vehicles.category"), (button, value) ->
            {
                category = value;
                rebuildChoices();
            }));
        addRenderableWidget(CycleButton.<String>builder(value -> value.isEmpty() ? Component.translatable(PREFIX + "vehicles.all_packs") : Component.literal(value)).withValues(packs)
            .withInitialValue(pack).create(getLeft() + half + GAP, y, half, BUTTON_HEIGHT, Component.translatable(PREFIX + "vehicles.pack"), (button, value) ->
            {
                pack = value;
                rebuildChoices();
            }));
    }

    private void rebuildChoices()
    {
        List<Choice> choices = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++)
        {
            BotSettings.VehicleEntry entry = entries.get(i);
            Optional<FlanModelEntityType> type = FlanModelNpcs.type(entry.type());
            String contentPack = type.map(FlanModelEntityType::getInfoType).map(value -> value.getContentPack().getName()).orElse("");
            if ((type.isPresent() && categoryOf(type.get()) != category) || (!pack.isEmpty() && !pack.equals(contentPack)))
                continue;
            List<Component> details = new ArrayList<>();
            details.add(Component.literal(entry.type()));
            details.add(Component.translatable(PREFIX + "vehicles.category." + category.name().toLowerCase(Locale.ROOT)));
            if (!contentPack.isEmpty())
                details.add(Component.literal(contentPack));
            if (type.isEmpty())
                details.add(Component.translatable(PREFIX + "vehicles.missing"));
            choices.add(new Choice(entry.type() + "#" + i, type.map(BotVehiclesPicker::displayName).orElse(Component.literal(entry.type())), details, type.isEmpty()));
        }
        setChoices(choices);
    }

    @Override
    protected int controlsWidth()
    {
        return STEPPER_WIDTH + GAP + 20;
    }

    @Override
    protected void addControls(Choice choice, int x, int y)
    {
        int index = Integer.parseInt(choice.id().substring(choice.id().lastIndexOf('#') + 1));
        BotSettings.VehicleEntry entry = entries.get(index);
        addStepper(x, y, entry.count(), Integer.MAX_VALUE, count ->
        {
            BotSettings.VehicleEntry current = entries.get(index);
            entries.set(index, new BotSettings.VehicleEntry(current.type(), count, current.budget(), current.respawnSeconds(), current.ticketCost(), current.spawner(), current.carry(),
                current.paintjobs(), current.aircraftRole()));
        }, Component.translatable(PREFIX + "vehicles.count"));
        addRowWidget(Button.builder(Component.literal("…"), button -> Minecraft.getInstance().setScreen(new BotVehicleSettingsScreen(this, editor.templates(), entries.get(index), updated ->
        {
            entries.set(index, updated);
            rebuildChoices();
        }))).bounds(x + STEPPER_WIDTH + GAP, y, 20, BUTTON_HEIGHT).build());
    }

    @Override
    protected boolean isChosen(Choice choice)
    {
        return entries.get(Integer.parseInt(choice.id().substring(choice.id().lastIndexOf('#') + 1))).count() > 0;
    }

    @Override
    protected LivingEntity preview(Choice choice)
    {
        BotSettings.VehicleEntry entry = entries.get(Integer.parseInt(choice.id().substring(choice.id().lastIndexOf('#') + 1)));
        String type = entry.type();
        if (minecraft == null || minecraft.level == null)
            return null;
        LivingEntity preview = previews.computeIfAbsent(type, key -> FlanModelNpcs.type(key).map(model -> new FlanModelEntity(model, minecraft.level)).orElse(null));
        if (preview instanceof FlanModelEntity model)
        {
            List<Integer> jobs = entry.paintjobs();
            model.setPaintjobId(jobs.isEmpty() ? 0 : jobs.get(0));
        }
        return preview;
    }

    @Override
    protected List<Component> summary()
    {
        return List
            .of(Component.translatable(PREFIX + "vehicles.total", entries.stream().filter(entry -> entry.count() > 0).count(), entries.stream().mapToLong(BotSettings.VehicleEntry::count).sum()));
    }

    @Override
    protected void apply()
    {
        force.getVehicles().clear();
        entries.stream().filter(entry -> entry.count() > 0).forEach(force.getVehicles()::add);
    }
}
