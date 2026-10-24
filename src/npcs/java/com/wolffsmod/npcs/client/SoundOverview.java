package com.wolffsmod.npcs.client;

import com.flansmodultimate.api.entity.EngineSound;
import com.flansmodultimate.api.entity.FlansEntityTypes;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.platform.CustomNpcsPlatform;
import com.wolffsmod.npcs.properties.*;
import lombok.RequiredArgsConstructor;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.shared.client.gui.components.*;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;

import java.util.Locale;
import java.util.Map;

/**
 * The Effective sounds tab of the Sounds page: each sound the NPC makes, the sound it plays with its current model,
 * equipment and switches, where that sound comes from, and a button to the page that sets it.
 *
 * <p>
 * Presentation only. A sound is set in one place, beside the other statistics of its source: the equipment pages for
 * the guns in hand, the model properties page for the model and the NPC's own sounds, and the Idle & movement
 * tab for vehicle and plane engines. The Weapon tab only switches whole sources on or off.
 * </p>
 */
final class SoundOverview
{
    private static final int OPEN_ID = 1700;
    private static final int VALUE_ID = 1720;
    private static final int SOURCE_ID = 1740;
    private static final int ROW_LABEL_ID = 1760;
    private static final int ROW_HEIGHT = 21;
    private static final int GREEN = 0x408040;
    private static final int RED = 0xA04040;
    private static final int GREY = 0xA0A0A0;
    private static final String PREFIX = "wolffsmodnpcs.sounds.";
    private static final String HELP_SUFFIX = ".help";

    private final GuiBasic gui;
    private final EntityNPCInterface npc;
    private Map<NpcTypeProperty, String> readouts = Map.of();

    /** The page that sets a sound, which the row's Open button goes to. */
    enum Target
    {
        MAIN_HAND, OFF_HAND, PROPERTIES, ENGINE
    }

    /** Where the sound a row shows comes from. */
    @RequiredArgsConstructor
    private enum Source
    {
        ITEM(GREEN), MODEL(GREEN), OWN(-1), OFF(RED), NONE(GREY);

        private final int color;

        private int color()
        {
            return color < 0 ? CustomNpcsPlatform.defaultTextColor() : color;
        }
    }

    /** The sounds the NPC makes whose value can be set, top to bottom. */
    private enum Row
    {
        FIRE_MAIN_HAND, FIRE_OFF_HAND, ENTITY_HIT, BLOCK_HIT, IDLE, MOVEMENT
    }

    /** The sound a row plays, or null for none, with where it comes from and where it is set. */
    private record Effective(@Nullable String sound, Source source, Target target)
    {}

    SoundOverview(GuiBasic gui, EntityNPCInterface npc)
    {
        this.gui = gui;
        this.npc = npc;
    }

    void init(boolean loaded, int top)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        properties.refresh(npc);
        readouts = properties.type().map(type -> NpcTypeReadouts.create(type, properties.inherited(), options(), properties.usesNativeProjectile())).orElse(Map.of());
        int x = gui.guiLeft + 8;
        for (Row row : Row.values())
        {
            int y = gui.guiTop + top + row.ordinal() * ROW_HEIGHT;
            String name = row.name().toLowerCase(Locale.ROOT);
            Effective effective = resolve(row);
            gui.addLabel(new GuiLabel(ROW_LABEL_ID + row.ordinal(), PREFIX + "row." + name, x, y + 5, PREFIX + "row." + name + HELP_SUFFIX));
            String sound = effective.sound() == null ? "-" : effective.sound();
            GuiLabel value = new GuiLabel(VALUE_ID + row.ordinal(), Component.literal(CombatPages.shorten(sound)), CustomNpcsPlatform.defaultTextColor(), x + 96, y + 5, 40, 0);
            value.setTooltip(Tooltip.create(Component.literal(sound)));
            gui.addLabel(value);
            String source = effective.source().name().toLowerCase(Locale.ROOT);
            // A sound switched off says which switch; any other source explains itself the same way on every row.
            String help = effective.source() == Source.OFF ? PREFIX + "off." + name : PREFIX + "source." + source + HELP_SUFFIX;
            GuiLabel from = new GuiLabel(SOURCE_ID + row.ordinal(), Component.translatable(PREFIX + "source." + source), effective.source().color(), x + 254, y + 5, 40, 0);
            from.setTooltip(Tooltip.create(Component.translatable(help)));
            gui.addLabel(from);
            String open = PREFIX + "open." + effective.target().name().toLowerCase(Locale.ROOT);
            GuiButtonNop button = new GuiButtonNop(gui, OPEN_ID + row.ordinal(), x + 336, y, 60, 20, new String[]{PREFIX + "open"}, 0);
            button.setEnabled(loaded);
            button.setTooltip(Tooltip.create(Component.translatable(open)));
            gui.addButton(button);
        }
    }

    boolean handles(int id)
    {
        return id >= OPEN_ID && id < OPEN_ID + Row.values().length;
    }

    /** @return the page that sets the sound of the row whose Open button this is */
    Target target(int id)
    {
        return resolve(Row.values()[id - OPEN_ID]).target();
    }

    private NpcWeaponOptions options()
    {
        return ((NpcWeaponSettings) npc.stats).wolffsmodnpcsWeaponOptions();
    }

    private Effective resolve(Row row)
    {
        return switch (row)
        {
            case FIRE_MAIN_HAND -> fire(EquipmentProperty.FIRING_SOUND, InteractionHand.MAIN_HAND);
            case FIRE_OFF_HAND -> NpcEquipment.offHandFires(npc) ? fire(EquipmentProperty.OFF_HAND_SOUND, InteractionHand.OFF_HAND) : new Effective(null, Source.NONE, Target.OFF_HAND);
            case ENTITY_HIT -> impact(EquipmentProperty.FIRING_ENTITY_HIT_SOUND);
            case BLOCK_HIT -> impact(EquipmentProperty.FIRING_BLOCK_HIT_SOUND);
            // A vehicle or plane sounds like its engine; any other NPC given a looped sound of its own plays that instead.
            case IDLE -> loops(false) ? engine(false) : modelOrOwn(NpcTypeProperty.IDLE_SOUND, Target.PROPERTIES);
            case MOVEMENT -> loops(true) ? engine(true) : modelOrOwn(NpcTypeProperty.STEP_SOUND, Target.PROPERTIES);
        };
    }

    /**
     * A gun in hand that inherits its firing sound plays its own, which Flan firing sounds can silence; otherwise the NPC
     * fires with its own firing sound, which the model may supply.
     */
    private Effective fire(EquipmentProperty statistic, InteractionHand hand)
    {
        Target page = hand == InteractionHand.MAIN_HAND ? Target.MAIN_HAND : Target.OFF_HAND;
        String item = EquipmentReadouts.value(npc, statistic);
        if (EquipmentValues.inUse(npc, statistic))
        {
            boolean silenced = !options().enabled(Feature.FIRE_SOUNDS) && NpcEquipment.flan(npc, hand).isPresent();
            return new Effective(item, silenced ? Source.OFF : Source.ITEM, page);
        }
        // Without a gun in the main hand, the NPC's own firing sound is best set beside the model's.
        return modelOrOwn(NpcTypeProperty.FIRING_SOUND, item == null && hand == InteractionHand.MAIN_HAND ? Target.PROPERTIES : page);
    }

    /** A projectile's hit sound is the main-hand weapon's, while the NPC fires it. */
    private Effective impact(EquipmentProperty statistic)
    {
        String item = EquipmentReadouts.value(npc, statistic);
        return EquipmentValues.inUse(npc, statistic) && item != null ? new Effective(item, Source.ITEM, Target.MAIN_HAND) : new Effective(null, Source.NONE, Target.MAIN_HAND);
    }

    /** The model's sound while it supplies one the NPC inherits, otherwise the NPC's own. */
    private Effective modelOrOwn(NpcTypeProperty property, Target ownPage)
    {
        NpcTypeProperties properties = NpcTypeProperties.of(npc);
        if (properties.supplied().contains(property) && !((NpcWeaponSettings) npc.stats).wolffsmodnpcsCombat().isCustom(property))
        {
            Tag value = properties.inherited().get(property);
            return new Effective(readouts.getOrDefault(property, value == null ? null : value.getAsString()), Source.MODEL, Target.PROPERTIES);
        }
        String own = properties.own(npc, property).getAsString();
        return new Effective(own.isBlank() ? I18n.get("wolffsmodnpcs.weapons.no_sound") : own, Source.OWN, ownPage);
    }

    private boolean loops(boolean moving)
    {
        return NpcPresentation.of(npc).getVehicleSounds().applies(npc, moving);
    }

    /** A vehicle engine sound: the vehicle's own while it uses its defaults, otherwise the one set on the Idle & movement tab. */
    private Effective engine(boolean moving)
    {
        NpcVehicleSounds sounds = NpcPresentation.of(npc).getVehicleSounds();
        NpcEngineSettings own = moving ? sounds.getMovement() : sounds.getIdle();
        String sound = sounds.isModelDefaults() ? vehicleSound(moving) : own.getSound();
        if (sound == null || sound.isBlank())
            return new Effective(null, Source.NONE, Target.ENGINE);
        if (!(moving ? sounds.isMovementEnabled() : sounds.isIdleEnabled()))
            return new Effective(sound, Source.OFF, Target.ENGINE);
        return new Effective(sound, sounds.isModelDefaults() ? Source.MODEL : Source.OWN, Target.ENGINE);
    }

    /** @return the selected vehicle or plane's own engine sound, or null without one */
    @Nullable
    private String vehicleSound(boolean moving)
    {
        return NpcPresentation.vehicleType(npc).flatMap(type -> moving ? FlansEntityTypes.getEngineSound(type) : FlansEntityTypes.getEngineIdleSound(type)).map(EngineSound::sound).orElse(null);
    }
}
