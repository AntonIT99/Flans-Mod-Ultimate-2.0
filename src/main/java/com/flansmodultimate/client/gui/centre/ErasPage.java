package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.client.eras.EraClientState;
import com.flansmodultimate.common.eras.EraCatalog;
import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.EraRestriction.BoundaryMode;
import com.flansmodultimate.common.eras.EraRestriction.MatchMode;
import com.flansmodultimate.common.eras.EraRestriction.RetirementPolicy;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.ServicePeriod;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Gives the server, a dimension, the round defaults or a round its era window: a boundary mode, one or two dates in the
 * content syntax and the policies, with a live count of the dated items the draft would block.
 */
final class ErasPage extends TeamsPage
{
    static final String ID = "flansmodultimate:eras";
    private static final String LOWER = "era_lower@";
    private static final String UPPER = "era_upper@";
    private static final String NONE = "—";
    private static String requestedTarget = "server";
    private String targetId = "server";
    private String loaded = "";
    @Nullable
    private BoundaryMode mode;
    private MatchMode match = MatchMode.SERVICE;
    private RetirementPolicy retirement = RetirementPolicy.ERA_END;
    private boolean undatedBlocked;
    private boolean ammo;
    private boolean despawn;
    private Component problem = Component.empty();
    private Component summary = Component.empty();
    private int infoTop;
    /** The bounds the summary was last worked out for, so typing refreshes it once per change. */
    private String summarised = "";

    ErasPage()
    {
        super("eras", Items.CLOCK, 420);
    }

    /** Opens the page on a target, as the timeline does. */
    static void open(ICommandCentreView view, String target)
    {
        requestedTarget = target;
        view.showPage(ID);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        List<EraTargets.Target> targets = EraTargets.all();
        if (!requestedTarget.isEmpty())
        {
            targetId = requestedTarget;
            requestedTarget = "";
        }
        EraTargets.Target target = EraTargets.byId(targets, targetId).orElse(targets.get(0));
        targetId = target.id();
        if (!loaded.equals(target.id() + "|" + target.restriction()))
            load(view, target);

        int left = view.getLeft();
        int width = view.getWidth();
        int half = (width - GAP) / 2;
        int y = view.getTop();
        choice(view, tr("eras.target"), targets.stream().map(EraTargets.Target::id).toList(), target.id(),
            id -> EraTargets.byId(targets, id).map(EraTargets.Target::name).orElse(Component.empty()), left, y, width, id ->
            {
                targetId = id;
                view.rebuild();
            });
        y += ROW;
        List<Optional<BoundaryMode>> modes = new ArrayList<>();
        modes.add(Optional.empty());
        Arrays.stream(BoundaryMode.values()).map(Optional::of).forEach(modes::add);
        choice(view, tr("eras.mode"), modes, Optional.ofNullable(mode), value -> Component.literal(value.map(BoundaryMode::getKey).orElse(NONE)), left, y, width, value ->
        {
            mode = value.orElse(null);
            view.rebuild();
        });
        y += ROW;
        EditBox lower = view.addField(LOWER + target.id(), left, y, half, view.getField(LOWER + target.id()), tr("eras.lower"));
        EditBox upper = view.addField(UPPER + target.id(), left + half + GAP, y, half, view.getField(UPPER + target.id()), tr("eras.upper"));
        lower.setEditable(mode != null && mode != BoundaryMode.UNTIL);
        upper.setEditable(mode == BoundaryMode.BETWEEN || mode == BoundaryMode.UNTIL);
        y += ROW;
        choice(view, tr("eras.match"), List.of(MatchMode.values()), match, value -> Component.literal(value.getKey()), left, y, half, value ->
        {
            match = value;
            refresh(view, target);
        });
        choice(view, tr("eras.retirement"), List.of(RetirementPolicy.values()), retirement, value -> Component.literal(value.getKey()), left + half + GAP, y, half, value ->
        {
            retirement = value;
            refresh(view, target);
        });
        y += ROW;
        int third = (width - 2 * GAP) / 3;
        toggle(view, tr("eras.undated"), undatedBlocked, left, y, third, tr("eras.undated.tip"), value ->
        {
            undatedBlocked = value;
            refresh(view, target);
        });
        toggle(view, tr("eras.ammo"), ammo, left + third + GAP, y, third, tr("eras.ammo.tip"), value ->
        {
            ammo = value;
            refresh(view, target);
        });
        if (target.world())
            toggle(view, tr("eras.despawn"), despawn, left + 2 * (third + GAP), y, third, tr("eras.despawn.tip"), value ->
            {
                despawn = value;
                refresh(view, target);
            });
        y += ROW;
        button(view, tr("eras.apply"), left, y, third, () -> apply(view, target));
        button(view, tr("eras.clear"), left + third + GAP, y, third, () -> run(view, target.clearCommand())).active = !target.restriction().isBlank();
        button(view, tr("eras.open_timeline"), left + 2 * (third + GAP), y, third, () -> TimelinePage.open(view, target.id()));
        infoTop = y + ROW + 4;
        refresh(view, target);
    }

    @Override
    public void tick(ICommandCentreView view)
    {
        String bounds = view.getField(LOWER + targetId) + "|" + view.getField(UPPER + targetId);
        if (bounds.equals(summarised))
            return;
        summarised = bounds;
        EraTargets.byId(EraTargets.all(), targetId).ifPresent(target -> refresh(view, target));
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int left = view.getLeft();
        int width = view.getWidth();
        int y = infoTop;
        label(view, graphics, summary, left, y, width, CentreStyle.ACCENT);
        y += 12;
        if (!problem.getString().isEmpty())
        {
            label(view, graphics, problem, left, y, width, CentreStyle.BAD);
            y += 12;
        }
        for (var line : view.getFont().split(tr("eras.help"), width))
        {
            if (y + view.getFont().lineHeight > view.getTop() + view.getHeight())
                break;
            graphics.drawString(view.getFont(), line, left, y, CentreStyle.MUTED, false);
            y += view.getFont().lineHeight;
        }
    }

    /** Takes the target's window into the editor, replacing a draft that no longer matches what the server holds. */
    private void load(ICommandCentreView view, EraTargets.Target target)
    {
        loaded = target.id() + "|" + target.restriction();
        EraRestriction restriction = target.parsed();
        mode = restriction == null ? null : restriction.mode();
        EraRestriction.Policies policies = restriction == null ? EraRestriction.Policies.DEFAULT : restriction.policies();
        match = policies.match();
        retirement = policies.retirement();
        undatedBlocked = policies.undated() == EraRestriction.UndatedPolicy.BLOCK;
        ammo = policies.ammo();
        despawn = policies.despawn();
        view.setField(LOWER + target.id(), restriction == null || restriction.lower() == null ? "" : restriction.lower().toString());
        view.setField(UPPER + target.id(), restriction == null || restriction.upper() == null || mode == BoundaryMode.EQUAL ? "" : restriction.upper().toString());
    }

    /** The draft as a restriction, or empty for none; {@link #problem} says why when it cannot be read. */
    private Optional<EraRestriction> draft(ICommandCentreView view, EraTargets.Target target)
    {
        problem = Component.empty();
        if (mode == null)
            return Optional.empty();
        String lower = view.getField(LOWER + target.id()).strip();
        String upper = view.getField(UPPER + target.id()).strip();
        StringBuilder text = new StringBuilder(mode.getKey());
        switch (mode)
        {
            case EQUAL, NOT_BEFORE -> text.append(' ').append(lower);
            case UNTIL -> text.append(' ').append(upper);
            case BETWEEN -> text.append(' ').append(lower).append(' ').append(upper);
        }
        text.append(" match=").append(match.getKey()).append(" retirement=").append(retirement.getKey()).append(" undated=").append(undatedBlocked ? "block" : "allow")
            .append(" ammo=").append(ammo ? "on" : "off");
        if (target.world())
            text.append(" despawn=").append(despawn ? "on" : "off");
        try
        {
            EraRestriction restriction = EraRestriction.parse(text.toString());
            Optional<String> invalid = restriction.validate(EraClientState.state().eras());
            if (invalid.isPresent())
            {
                problem = Component.literal(invalid.get());
                return Optional.empty();
            }
            return Optional.of(restriction);
        }
        catch (IllegalArgumentException exception)
        {
            problem = Component.literal(exception.getMessage());
            return Optional.empty();
        }
    }

    private void refresh(ICommandCentreView view, EraTargets.Target target)
    {
        Optional<EraRestriction> restriction = draft(view, target);
        summary = restriction.map(ErasPage::describe).orElse(mode == null ? tr("eras.none") : Component.empty());
    }

    private void apply(ICommandCentreView view, EraTargets.Target target)
    {
        Optional<EraRestriction> restriction = draft(view, target);
        if (mode == null)
            run(view, target.clearCommand());
        else if (restriction.isPresent())
            run(view, target.setCommand(restriction.get().toString()));
        else
            view.showMessage(problem, true);
    }

    /** How many dated items the restriction blocks, and how many items are undated. */
    static Component describe(EraRestriction restriction)
    {
        EraVerdicts verdicts = EraClientState.verdicts();
        EraCatalog eras = verdicts.getEras();
        int dated = 0;
        int blocked = 0;
        int undated = 0;
        for (InfoType type : InfoType.getInfoTypes().values().stream().distinct().toList())
        {
            ServicePeriod period = verdicts.periodOf(type);
            if (period == null)
                undated++;
            else
                dated++;
            if (!EraVerdicts.decide(restriction, type.getType(), period, eras) && period != null)
                blocked++;
        }
        return tr("eras.summary", blocked, dated, undated);
    }
}
