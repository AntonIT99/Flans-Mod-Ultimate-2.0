package com.flansmodultimate.client.gui.centre;

import com.flansmodultimate.api.client.ICommandCentreView;
import com.flansmodultimate.common.teams.TeamsManager;
import com.flansmodultimate.common.teams.centre.TeamsSection;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * How the server runs Teams between and around rounds: voting for the next round, the rounds generator, the length of
 * each stage of the break, autobalance, the message of the day, experience, the ranked loadout pool and the lag
 * compensation of bullets.
 */
final class ServerPage extends TeamsPage
{
    /** The settings given in seconds, by the {@code /teams admin} command that sets each. */
    private static final List<String> TIMES = List.of(TeamsSection.VOTING_TIME, TeamsSection.SCORE_DISPLAY, TeamsSection.RANK_UPDATE, TeamsSection.AUTOBALANCE);
    private static final String NONE = "none";
    private static final String SETTING = "setting.";
    private static final String XP = "setting.xpmultiplier";
    private static final String SNAPSHOT_MIN = "setting.bltss_min";
    private static final String SNAPSHOT_DIVISOR = "setting.bltss_divisor";
    private static final int FIELD_WIDTH = 48;
    private static final int BUTTON_WIDTH = 40;

    private int columnWidth;

    ServerPage()
    {
        super("server", Items.COMPARATOR, 500);
    }

    @Override
    public void init(ICommandCentreView view)
    {
        CompoundTag settings = data().settings();
        columnWidth = (view.getWidth() - 6) / 2;
        int left = view.getLeft() + 6;
        int width = columnWidth - 12;
        int y = view.getTop() + 18;
        toggle(view, tr("setting.voting"), settings.getBoolean(TeamsSection.VOTING), left, y, width, tr("setting.voting.tip"), enabled -> run(view, "teams admin voting", enabled));
        y += ROW;
        toggle(view, tr("setting.generator"), settings.getBoolean(TeamsSection.GENERATOR), left, y, width, tr("setting.generator.tip"), enabled -> run(view, "teams admin roundsGenerator", enabled));
        for (String time : TIMES)
        {
            y += ROW;
            int least = TeamsSection.AUTOBALANCE.equals(time) ? 11 : 0;
            int x = left + width - FIELD_WIDTH - BUTTON_WIDTH - GAP;
            view.addField(time, x, y, FIELD_WIDTH, Integer.toString(settings.getInt(time)), tr("seconds"));
            button(view, tr("set"), x + FIELD_WIDTH + GAP, y, BUTTON_WIDTH, tr(SETTING + time.toLowerCase(Locale.ROOT) + ".tip"),
                () -> number(view, time, least, 86400, tr(SETTING + time.toLowerCase(Locale.ROOT))).ifPresent(seconds -> run(view, "teams admin", time, seconds)));
        }

        int right = view.getLeft() + columnWidth + 12;
        y = view.getTop() + 18;
        int motdWidth = width - BUTTON_WIDTH - GAP;
        view.addField(TeamsSection.MOTD, right, y + 10, motdWidth, settings.getString(TeamsSection.MOTD), tr("setting.motd"));
        button(view, tr("set"), right + motdWidth + GAP, y + 10, BUTTON_WIDTH, () ->
        {
            String motd = view.getField(TeamsSection.MOTD).strip();
            run(view, "teams admin motd", motd.isEmpty() ? TeamsManager.DEFAULT_MOTD : motd);
        });
        y += ROW + 10;
        int x = right + width - FIELD_WIDTH - BUTTON_WIDTH - GAP;
        view.addField(TeamsSection.XP, x, y, FIELD_WIDTH, Float.toString(settings.getFloat(TeamsSection.XP)), tr(XP));
        button(view, tr("set"), x + FIELD_WIDTH + GAP, y, BUTTON_WIDTH, tr("setting.xpmultiplier.tip"), () -> setExperience(view));
        y += ROW;
        List<String> pools = new ArrayList<>();
        pools.add(NONE);
        data().pools().forEach(pool -> pools.add(pool.id()));
        String pool = settings.getString(TeamsSection.POOL);
        choice(view, tr("setting.loadoutpool"), pools, pools.contains(pool) ? pool : NONE, id -> NONE.equals(id) ? tr("setting.loadoutpool.none") : poolName(id), right, y, width,
            id -> run(view, "teams admin loadoutpool", id)).setTooltip(Tooltip.create(tr("setting.loadoutpool.tip")));
        y += ROW + 10;
        int snapshotWidth = (width - BUTTON_WIDTH - 2 * GAP) / 2;
        view.addField(TeamsSection.SNAPSHOT_MIN, right, y + 10, snapshotWidth, Integer.toString(settings.getInt(TeamsSection.SNAPSHOT_MIN)), tr(SNAPSHOT_MIN))
            .setTooltip(Tooltip.create(tr(SNAPSHOT_MIN)));
        view.addField(TeamsSection.SNAPSHOT_DIVISOR, right + snapshotWidth + GAP, y + 10, snapshotWidth, Integer.toString(settings.getInt(TeamsSection.SNAPSHOT_DIVISOR)), tr(SNAPSHOT_DIVISOR))
            .setTooltip(Tooltip.create(tr(SNAPSHOT_DIVISOR)));
        button(view, tr("set"), right + width - BUTTON_WIDTH, y + 10, BUTTON_WIDTH, tr("setting.bltss.tip"), () ->
        {
            var minimum = number(view, TeamsSection.SNAPSHOT_MIN, 0, 100, tr(SNAPSHOT_MIN));
            var divisor = minimum.isPresent() ? number(view, TeamsSection.SNAPSHOT_DIVISOR, 0, 1000, tr(SNAPSHOT_DIVISOR)) : java.util.OptionalInt.empty();
            if (divisor.isPresent())
                run(view, "teams admin bltss", minimum.getAsInt(), divisor.getAsInt());
        });
    }

    private static Component poolName(String id)
    {
        return Component.literal(data().pools().stream().filter(pool -> pool.id().equals(id)).map(TeamsData.Pool::name).findFirst().orElse(id));
    }

    private static void setExperience(ICommandCentreView view)
    {
        try
        {
            float multiplier = Float.parseFloat(view.getField(TeamsSection.XP).strip());
            if (multiplier >= 0F && multiplier <= 100F)
            {
                run(view, "teams admin xpmultiplier", multiplier);
                return;
            }
        }
        catch (NumberFormatException e)
        {
            // Told below, as a multiplier out of bounds is.
        }
        view.showMessage(tr("bad_number", tr(XP), 0, 100), true);
    }

    @Override
    public void render(ICommandCentreView view, GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        int left = view.getLeft();
        int top = view.getTop();
        view.drawPanel(graphics, left, top, columnWidth, view.getHeight(), tr("server.rotation"));
        int right = left + columnWidth + 6;
        int rightWidth = view.getWidth() - columnWidth - 6;
        view.drawPanel(graphics, right, top, rightWidth, view.getHeight(), tr("server.players"));
        int y = top + 18 + 2 * ROW;
        for (String time : TIMES)
        {
            label(view, graphics, tr(SETTING + time.toLowerCase(Locale.ROOT)), left + 6, y + 5, columnWidth - FIELD_WIDTH - BUTTON_WIDTH - 20, CentreStyle.TEXT);
            y += ROW;
        }
        int x = right + 6;
        label(view, graphics, tr("setting.motd"), x, top + 18, rightWidth - 12, CentreStyle.DIM);
        label(view, graphics, tr(XP), x, top + 18 + ROW + 15, rightWidth - FIELD_WIDTH - BUTTON_WIDTH - 20, CentreStyle.TEXT);
        label(view, graphics, tr("setting.bltss"), x, top + 18 + 3 * ROW + 20, rightWidth - 12, CentreStyle.DIM);
    }
}
