package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.teams.IPlayerClass;
import com.flansmodultimate.api.teams.ITeam;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import com.wolffsmod.npcs.teams.BotRoles;
import com.wolffsmod.npcs.teams.BotSettings;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * A picker over the classes of a team, each previewed in its class skin and loadout, with what it carries and the role
 * that suggests. Classes a setting names that the team does not offer are listed after its own, marked. Client side.
 */
abstract class BotClassPicker extends BotPickerScreen
{
    @Nullable
    private final ITeam team;
    private final String teamName;
    private final Map<String, IPlayerClass> classes = new LinkedHashMap<>();
    private final Map<String, BotSettings.ClassRole> suggestedRoles = new HashMap<>();
    private final Map<String, LivingEntity> previews = new HashMap<>();

    BotClassPicker(BotEditorScreen parent, String team, Component title, Component hint)
    {
        super(parent, title, hint);
        teamName = team;
        this.team = FlansModApi.getTeam(team).orElse(null);
        if (this.team != null)
            this.team.getClasses().forEach(playerClass -> classes.put(playerClass.getShortName(), playerClass));
    }

    /** @return the short names of the team's classes, in the team's order */
    protected final List<String> offered()
    {
        return List.copyOf(classes.keySet());
    }

    protected final boolean offers(String playerClass)
    {
        return classes.containsKey(playerClass);
    }

    /** @return the role a class's equipment suggests, as it gets unless its team sets one */
    protected final BotSettings.ClassRole suggestedRole(String playerClass)
    {
        return suggestedRoles.getOrDefault(playerClass, BotSettings.ClassRole.RIFLEMAN);
    }

    /** @return a choice for each class of the team, then one for each class named that the team does not offer */
    protected final List<Choice> classChoices(Collection<String> named)
    {
        List<Choice> choices = new ArrayList<>();
        classes.values().forEach(playerClass -> choices.add(classChoice(playerClass)));
        named.stream().filter(playerClass -> !classes.containsKey(playerClass)).distinct()
            .forEach(playerClass -> choices.add(new Choice(playerClass, Component.literal(playerClass), List.of(Component.translatable(PREFIX + "class.not_offered", teamName)), true)));
        return choices;
    }

    private Choice classChoice(IPlayerClass playerClass)
    {
        List<ItemStack> items = playerClass.createStartingItems();
        BotSettings.ClassRole role = BotRoles.suggested(items);
        suggestedRoles.put(playerClass.getShortName(), role);
        List<Component> details = new ArrayList<>();
        details.add(Component.literal(playerClass.getShortName()).withStyle(ChatFormatting.GRAY));
        details.add(Component.translatable(PREFIX + "class.role", roleName(role)));
        if (!items.isEmpty())
            details.add(Component.translatable(PREFIX + "class.carries", String.join(", ", items.stream().map(BotClassPicker::itemName).toList())));
        return new Choice(playerClass.getShortName(), playerClass.getNameComponent(), details, false);
    }

    private static String itemName(ItemStack item)
    {
        String name = item.getHoverName().getString();
        return item.getCount() > 1 ? item.getCount() + "× " + name : name;
    }

    /** @return a role's name as screens show it */
    static Component roleName(BotSettings.ClassRole role)
    {
        return Component.translatable(PREFIX + "role." + role.name().toLowerCase(Locale.ROOT));
    }

    /** @return what a role does, for its tooltip */
    static Component roleHelp(BotSettings.ClassRole role)
    {
        return Component.translatable(PREFIX + "role." + role.name().toLowerCase(Locale.ROOT) + ".tip");
    }

    @Nullable
    @Override
    protected LivingEntity preview(Choice choice)
    {
        IPlayerClass playerClass = classes.get(choice.id());
        if (playerClass == null || minecraft == null || minecraft.level == null)
            return null;
        return previews.computeIfAbsent(choice.id(), id -> NpcPreviews.ofClass(minecraft.level, new FlanSoldierSelection(team, playerClass, 0)));
    }

    @Override
    protected List<Component> summary()
    {
        return team == null ? List.of(Component.translatable(PREFIX + "class.team_missing", teamName).withStyle(ChatFormatting.GOLD)) : List.of();
    }
}
