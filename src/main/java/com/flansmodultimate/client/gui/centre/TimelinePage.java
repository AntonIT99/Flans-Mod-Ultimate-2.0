package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.commandcentre.ICommandCentreView;
import com.flansmodultimate.client.eras.EraClientState;
import com.flansmodultimate.common.command.FlansModCommandAliases;
import com.flansmodultimate.common.eras.EraCatalog;
import com.flansmodultimate.common.eras.EraEnforcement;
import com.flansmodultimate.common.eras.EraRestriction;
import com.flansmodultimate.common.eras.EraVerdicts;
import com.flansmodultimate.common.eras.ServiceDate;
import com.flansmodultimate.common.eras.ServicePeriod;
import com.flansmodultimate.common.types.EnumType;
import com.flansmodultimate.common.types.InfoType;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Shows the items by their service dates against the window of a chosen target, which can be moved by dragging its
 * brackets. Filters by kind and name, lists the undated items, and lets an operator set or clear an item's date
 * override.
 */
final class TimelinePage extends TeamsPage
{
    static final String ID = "flansmodultimate:timeline";
    private static final String SEARCH = "timeline_search";
    private static final String OVERRIDE = "timeline_override";
    private static final String ERA = FlansModCommandAliases.ROOT + " era ";
    private static final List<String> GROUPS = List.of("all", "guns", "vehicles", "planes", "mechas", "aa_guns", "grenades", "armor", "attachments", "tools", "ammo", "other");
    private static String requestedTarget = "";
    private final EraTimelineWidget timeline = new EraTimelineWidget(this::windowMoved, this::selected);
    private String targetId = "server";
    private String group = "all";
    private boolean undated;
    @Nullable
    private InfoType chosen;
    @Nullable
    private ICommandCentreView currentView;
    private int detailsTop;
    private int shownCount;
    private String searched = "";
    /** Set when a click chose another item; the page is rebuilt on the next tick, outside the click's dispatch. */
    private boolean rebuildPending;

    TimelinePage()
    {
        super("timeline", Items.COMPASS, 430);
    }

    /** Opens the page on a target, as the eras page does. */
    static void open(ICommandCentreView view, String target)
    {
        requestedTarget = target;
        view.showPage(ID);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        currentView = view;
        List<EraTargets.Target> targets = EraTargets.all();
        if (!requestedTarget.isEmpty())
        {
            targetId = requestedTarget;
            requestedTarget = "";
        }
        EraTargets.Target target = EraTargets.byId(targets, targetId).orElse(targets.get(0));
        targetId = target.id();
        int left = view.getLeft();
        int width = view.getWidth();
        int third = (width - 2 * GAP) / 3;
        int y = view.getTop();
        choice(view, tr("eras.target"), targets.stream().map(EraTargets.Target::id).toList(), target.id(),
            id -> EraTargets.byId(targets, id).map(EraTargets.Target::name).orElse(Component.empty()), left, y, 2 * third + GAP, id ->
            {
                targetId = id;
                view.rebuild();
            });
        button(view, tr("eras.edit"), left + 2 * (third + GAP), y, third, () -> ErasPage.open(view, targetId));
        y += ROW;
        choice(view, tr("timeline.group"), GROUPS, group, name -> tr("timeline.group." + name), left, y, third, name ->
        {
            group = name;
            view.rebuild();
        });
        view.addField(SEARCH, left + third + GAP, y, third, view.getField(SEARCH), tr("timeline.search"));
        toggle(view, tr("timeline.undated"), undated, left + 2 * (third + GAP), y, third / 2 - GAP / 2, null, value ->
        {
            undated = value;
            view.rebuild();
        });
        button(view, tr("timeline.reset"), left + 2 * (third + GAP) + third / 2 + GAP / 2, y, third - third / 2 - GAP / 2, () -> timeline.resetView());
        y += ROW;
        detailsTop = view.getTop() + view.getHeight() - 2 * ROW - 4;
        timeline.place(left, y, width, detailsTop - y - 4);
        view.addWidget(timeline);
        if (chosen != null)
        {
            view.addField(OVERRIDE, left, detailsTop + ROW, width - 2 * (60 + GAP), view.getField(OVERRIDE), tr("timeline.override"));
            String shortname = chosen.getShortName();
            button(view, tr("timeline.set"), left + width - 2 * (60 + GAP) + GAP, detailsTop + ROW, 60, () -> run(view, ERA + "date set " + shortname + " " + view.getField(OVERRIDE).strip()));
            button(view, tr("timeline.clear_override"), left + width - 60, detailsTop + ROW, 60, () -> run(view, ERA + "date clear " + shortname)).active = EraClientState.state().overrides()
                .containsKey(shortname.toLowerCase(Locale.ROOT));
        }
        searched = view.getField(SEARCH);
        update(target);
    }

    @Override
    public void tick(ICommandCentreView view)
    {
        if (rebuildPending)
        {
            rebuildPending = false;
            view.rebuild();
            return;
        }
        if (view.getField(SEARCH).equals(searched))
            return;
        searched = view.getField(SEARCH);
        EraTargets.byId(EraTargets.all(), targetId).ifPresent(this::update);
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int left = view.getLeft();
        int width = view.getWidth();
        Component line;
        if (chosen == null)
            line = tr(undated ? "timeline.undated_count" : "timeline.help", shownCount);
        else
        {
            ServicePeriod period = EraClientState.periodOf(chosen);
            line = Component.translatable(chosen.getNameTranslationKey()).append(": ").append(period == null ? tr("timeline.no_dates") : Component.literal(EraEnforcement.describePeriod(period)));
        }
        label(view, graphics, line, left, detailsTop + 4, width, chosen == null ? CentreStyle.MUTED : CentreStyle.TEXT);
    }

    /** Recomputes what the timeline draws from the target's window, the filters and the client's dates. */
    private void update(EraTargets.Target target)
    {
        EraVerdicts verdicts = EraClientState.verdicts();
        EraCatalog eras = verdicts.getEras();
        EraRestriction window = target.parsed();
        if (window != null && window.validate(eras).isPresent())
            window = null;
        EraRestriction drawing = window == null ? EraRestriction.of(EraRestriction.BoundaryMode.NOT_BEFORE, ServiceDate.parse("1C"), null) : window;
        String search = searched.strip().toLowerCase(Locale.ROOT);
        List<EraTimelineWidget.Entry> entries = new ArrayList<>();
        for (InfoType type : InfoType.getInfoTypes().values().stream().distinct().toList())
        {
            if (!inGroup(type.getType()))
                continue;
            String name = Component.translatable(type.getNameTranslationKey()).getString();
            if (!search.isEmpty() && !name.toLowerCase(Locale.ROOT).contains(search) && !type.getShortName().contains(search))
                continue;
            ServicePeriod period = verdicts.periodOf(type);
            boolean dated = period != null && period.isResolvable(eras);
            if (undated != !dated)
                continue;
            boolean allowed = window == null || EraVerdicts.decide(window, type.getType(), period, eras);
            if (!dated)
            {
                entries.add(new EraTimelineWidget.Entry(type, name, ServiceDate.OPEN_LOW, ServiceDate.OPEN_LOW, false, false, allowed));
                continue;
            }
            int start = period.introduction().first(eras);
            int end = drawing.serviceEnd(period, eras);
            boolean open = period.retirement() == null && end >= ServiceDate.FUTURE - 1;
            entries.add(new EraTimelineWidget.Entry(type, name, start, end, period.introduction().approximate(), open, allowed));
        }
        entries.sort(Comparator.comparingInt(EraTimelineWidget.Entry::start).thenComparing(EraTimelineWidget.Entry::name));
        shownCount = entries.size();
        int lower = window == null ? ServiceDate.OPEN_LOW : window.lowerMonth(eras);
        int upper = window == null ? ServiceDate.OPEN_HIGH : window.upperMonth(eras);
        timeline.show(entries, eras, window != null, lower, upper, chosen);
    }

    private boolean inGroup(EnumType type)
    {
        String of = switch (type)
        {
            case GUN -> "guns";
            case VEHICLE -> "vehicles";
            case PLANE -> "planes";
            case MECHA, MECHA_ITEM -> "mechas";
            case AA_GUN -> "aa_guns";
            case GRENADE -> "grenades";
            case ARMOR -> "armor";
            case ATTACHMENT -> "attachments";
            case TOOL -> "tools";
            case BULLET -> "ammo";
            default -> "other";
        };
        return "all".equals(group) || group.equals(of);
    }

    /** A dragged bracket becomes the target's new window, its other bound and policies kept. */
    private void windowMoved(int lower, int upper)
    {
        ICommandCentreView view = currentView;
        if (view == null)
            return;
        EraTargets.byId(EraTargets.all(), targetId).ifPresent(target ->
        {
            EraRestriction current = target.parsed();
            if (current == null)
                return;
            EraRestriction.BoundaryMode mode = current.mode() == EraRestriction.BoundaryMode.EQUAL ? EraRestriction.BoundaryMode.BETWEEN : current.mode();
            ServiceDate from = mode == EraRestriction.BoundaryMode.UNTIL ? null : ServiceDate.parse(ServiceDate.formatMonth(lower));
            ServiceDate to = mode == EraRestriction.BoundaryMode.NOT_BEFORE ? null : ServiceDate.parse(ServiceDate.formatMonth(upper));
            EraRestriction moved = new EraRestriction(mode, from, to, current.policies());
            run(view, target.setCommand(moved.toString()));
        });
    }

    private void selected(EraTimelineWidget.Entry entry)
    {
        boolean changed = chosen != entry.type();
        chosen = entry.type();
        ICommandCentreView view = currentView;
        if (view != null && changed)
        {
            ServicePeriod override = EraClientState.state().overrides().get(entry.type().getShortName().toLowerCase(Locale.ROOT));
            ServicePeriod period = override != null ? override : EraClientState.periodOf(entry.type());
            view.setField(OVERRIDE, period == null ? "" : period.toString());
            rebuildPending = true;
        }
    }
}
