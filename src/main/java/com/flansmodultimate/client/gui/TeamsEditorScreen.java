package com.flansmodultimate.client.gui;

import com.flansmodultimate.client.teams.EditorClientState;
import com.flansmodultimate.client.teams.TeamFlagTextures;
import com.flansmodultimate.common.teams.TeamFlagUrl;
import com.flansmodultimate.common.teams.editor.*;
import com.flansmodultimate.common.types.*;
import com.flansmodultimate.network.PacketHandler;
import com.flansmodultimate.network.server.teams.PacketEditorAction;
import com.flansmodultimate.platform.client.ClientPlatform;
import com.flansmodultimate.platform.entity.EntityPlatform;
import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/** Operator editor with draft-only inventory templates and explicit server commits. */
@SuppressWarnings("DataFlowIssue") // Screen initializes minecraft before init, callbacks and rendering.
public final class TeamsEditorScreen extends Screen
{
    private record Row(String key, String name, String pack, String status)
    {}

    private static final String VICTORY_FLAG = "victoryFlag";
    private static final int FLAG_PREVIEW_WIDTH = 72;
    private static final int FLAG_PREVIEW_HEIGHT = 48;

    private boolean team = true;
    private boolean detail;
    private boolean creating;
    private String key = "";
    private JsonObject initial = new JsonObject();
    private JsonObject draft = new JsonObject();
    private long editingRevision;
    private String query = "";
    private String packFilter = "";
    private int page;
    private int tab;
    private String message;
    private String selectedClass = "";
    private boolean previewClass;
    private ItemStack carried = ItemStack.EMPTY;
    private int left;
    private int top;
    private int panelWidth;
    private int panelHeight;
    private final Map<String, String> errors = new HashMap<>();
    private final Map<String, String> rawInputs = new HashMap<>();
    private final List<EditBox> fields = new ArrayList<>();
    private final Map<String, Button> membershipButtons = new HashMap<>();
    private final List<ColourSlider> colourSliders = new ArrayList<>();
    private Button saveButton;
    /** The flag field of the advanced team tab, while it is shown. */
    @Nullable
    private EditBox flagField;
    private RemotePlayer preview;
    private ResourceLocation previewSkin;

    public TeamsEditorScreen(String open, String key, String message)
    {
        super(tr("title"));
        this.message = message;
        if ("classes".equals(open))
            team = false;
        if ("team".equals(open) || "class".equals(open))
        {
            team = "team".equals(open);
            select(key);
        }
    }

    public static Component tr(String key, Object... values)
    {
        return Component.translatable("gui.flansmodultimate.editor." + key, values);
    }

    @Override
    protected void init()
    {
        panelWidth = Math.min(620, width - 12);
        panelHeight = Math.min(360, height - 12);
        left = (width - panelWidth) / 2;
        top = (height - panelHeight) / 2;
        fields.clear();
        flagField = null;
        membershipButtons.clear();
        colourSliders.clear();
        saveButton = null;
        if (detail)
            details();
        else
            listing();
    }

    private void rebuild()
    {
        clearWidgets();
        init();
    }

    private Button button(Component text, int x, int y, int w, Runnable action)
    {
        return addRenderableWidget(Button.builder(text, ignored -> action.run()).bounds(x, y, w, 20).build());
    }

    private void listing()
    {
        button(tr("teams"), left + 8, top + 25, 85, () -> switchKind(true));
        button(tr("classes"), left + 98, top + 25, 85, () -> switchKind(false));
        button(tr("new"), left + panelWidth - 88, top + 25, 80, () -> nameDialog(false));
        EditBox search = new EditBox(font, left + 8, top + 50, Math.max(100, panelWidth - 160), 20, tr("search"));
        search.setHint(tr("search"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            page = 0;
        });
        addRenderableWidget(search);
        button(Component.literal(packFilter.isEmpty() ? tr("all_packs").getString() : packFilter), left + panelWidth - 145, top + 50, 137, () ->
        {
            List<String> packs = new ArrayList<>(rows().stream().map(Row::pack).distinct().sorted().toList());
            packs.add(0, "");
            packFilter = packs.get((packs.indexOf(packFilter) + 1) % packs.size());
            page = 0;
            rebuild();
        });
        button(Component.literal("<"), left + 8, top + panelHeight - 30, 25, () ->
        {
            page = Math.max(0, page - 1);
            rebuild();
        });
        button(Component.literal(">"), left + 38, top + panelHeight - 30, 25, () ->
        {
            if ((page + 1) * rowCount() < filteredRows().size())
                page++;
            rebuild();
        });
        button(tr("close"), left + panelWidth - 88, top + panelHeight - 30, 80, this::onClose);
    }

    private void switchKind(boolean team)
    {
        this.team = team;
        page = 0;
        rebuild();
    }

    private List<Row> rows()
    {
        Map<String, Row> rows = new LinkedHashMap<>();
        Collection<? extends InfoType> originals = team ? Team.packValues() : PlayerClass.packValues();
        Collection<? extends InfoType> effective = team ? Team.values() : PlayerClass.values();
        originals.forEach(type -> rows.put(EditorCatalog.key(type), new Row(EditorCatalog.key(type), type.getName(), type.getContentPack().getName(), "original")));
        effective.forEach(type -> rows.put(EditorCatalog.key(type), new Row(EditorCatalog.key(type), type.getName(), type.getContentPack().getName(), "original")));
        JsonObject saved = team ? EditorClientState.document().teams() : EditorClientState.document().classes();
        saved.entrySet().forEach(entry ->
        {
            Row base = rows.get(entry.getKey());
            JsonObject patch = entry.getValue().getAsJsonObject();
            String status = entryStatus(base, entry.getKey(), patch);
            String name = base == null ? entry.getKey() : base.name();
            if (patch.has("name"))
                name = patch.get("name").getAsString();
            rows.put(entry.getKey(), new Row(entry.getKey(), name, base == null ? "Server editor" : base.pack(), status));
        });
        return rows.values().stream().sorted(Comparator.comparing(Row::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    private static String entryStatus(Row base, String key, JsonObject patch)
    {
        if (EditorCatalog.disabled(patch))
            return "disabled";
        if (key.startsWith("custom:"))
            return "custom";
        return base == null ? "unavailable" : "modified";
    }

    private JsonObject describe(InfoType type)
    {
        if (type == null)
            type = team ? new EditedTeam(key, null, new JsonObject()) : new EditedPlayerClass(key, null, new JsonObject());
        if (type instanceof Team value)
            return EditorDescriptions.team(value);
        return EditorDescriptions.playerClass((PlayerClass) type);
    }

    private List<Row> filteredRows()
    {
        String needle = query.toLowerCase(Locale.ROOT);
        return rows().stream().filter(row -> (packFilter.isEmpty() || packFilter.equals(row.pack())) && (row.name().toLowerCase(Locale.ROOT).contains(needle) || row.key().contains(needle))).toList();
    }

    private int rowCount()
    {
        return Math.max(1, (panelHeight - 120) / 23);
    }

    private void select(String key)
    {
        this.key = key;
        InfoType selected = (team ? Team.values().stream() : PlayerClass.values().stream()).filter(type -> EditorCatalog.key(type).equals(key)).findFirst().orElse(null);
        if (selected == null)
            selected = (team ? Team.packValues().stream() : PlayerClass.packValues().stream()).filter(type -> EditorCatalog.key(type).equals(key)).findFirst().orElse(null);
        draft = describe(selected);
        JsonObject saved = team ? EditorClientState.document().teams() : EditorClientState.document().classes();
        if (saved.has(key))
            EditorDescriptions.merge(draft, saved.getAsJsonObject(key));
        draft.addProperty("disabled", saved.has(key) && EditorCatalog.disabled(saved.getAsJsonObject(key)));
        initial = draft.deepCopy();
        editingRevision = EditorClientState.revision();
        creating = false;
        detail = true;
        tab = 0;
        page = 0;
        errors.clear();
        rawInputs.clear();
        preview = null;
    }

    private void details()
    {
        int tabWidth = Math.max(65, (panelWidth - 16) / 4);
        String[] tabs = {"basics", "equipment", team ? "classes" : "skins", "advanced"};
        for (int i = 0; i < tabs.length; i++)
        {
            int target = i;
            button(tr(tabs[i]), left + 8 + i * tabWidth, top + 27, tabWidth - 3, () ->
            {
                tab = target;
                previewClass = false;
                page = 0;
                query = "";
                rebuild();
            });
        }
        if (tab == 0)
            basics();
        else if (tab == 1)
            equipment();
        else if (tab == 2)
        {
            if (team)
                memberships();
            else
                skins();
        }
        else
            advanced();
        int y = top + panelHeight - 29;
        int w = (panelWidth - 30) / 5;
        saveButton = button(tr("save"), left + 6, y, w, this::save);
        button(tr("back"), left + 12 + w, y, w, this::back);
        button(tr("duplicate"), left + 18 + 2 * w, y, w, () -> nameDialog(true));
        button(tr("restore"), left + 24 + 3 * w, y, w, () -> confirm("restore"));
        button(tr("delete"), left + 30 + 4 * w, y, w - 6, () -> confirm("delete"));
    }

    private void basics()
    {
        field("name", 0, false);
        button(tr(draft.has("disabled") && draft.get("disabled").getAsBoolean() ? "disabled" : "enabled"), left + 8, top + 110, 145, () ->
        {
            draft.addProperty("disabled", !draft.has("disabled") || !draft.get("disabled").getAsBoolean());
            rebuild();
        });
        if (team)
        {
            field("colour", 1, false);
            for (int channel = 0; channel < 3; channel++)
            {
                ColourSlider slider = new ColourSlider(left + 8 + channel * (panelWidth - 24) / 3, top + 138, (panelWidth - 30) / 3, channel);
                colourSliders.add(slider);
                addRenderableWidget(slider);
            }
            button(Component.literal(draft.get("textColour").getAsString()), left + 164, top + 110, Math.max(95, panelWidth - 172), () ->
            {
                List<ChatFormatting> values = Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor).toList();
                ChatFormatting current = ChatFormatting.getByName(draft.get("textColour").getAsString());
                draft.addProperty("textColour", values.get((values.indexOf(current) + 1) % values.size()).getName());
                rebuild();
            });
        }
    }

    private void field(String name, int row, boolean number)
    {
        int x = left + Math.min(155, panelWidth / 2);
        EditBox field = new EditBox(font, x, top + 60 + row * 25, panelWidth - (x - left) - 10, 20, tr(name));
        field.setMaxLength(maxLength(name));
        field.setValue(rawInputs.getOrDefault(name, draft.has(name) ? draft.get(name).getAsString() : ""));
        if (VICTORY_FLAG.equals(name))
        {
            field.setHint(tr("victoryFlag_hint"));
            flagField = field;
        }
        field.setResponder(text ->
        {
            try
            {
                rawInputs.put(name, text);
                if (number)
                    draft.addProperty(name, Double.parseDouble(text));
                else
                    draft.addProperty(name, text);
                errors.remove(name);
                validate();
                if ("colour".equals(name) && text.matches("#[0-9a-fA-F]{6}"))
                    colourSliders.forEach(slider -> slider.sync(readColour(draft)));
            }
            catch (RuntimeException exception)
            {
                errors.put(name, exception.getMessage());
            }
        });
        fields.add(field);
        addRenderableWidget(field);
    }

    private static int maxLength(String field)
    {
        if ("name".equals(field))
            return 128;
        return VICTORY_FLAG.equals(field) ? TeamFlagUrl.MAX_LENGTH : 256;
    }

    private void advanced()
    {
        if (team)
        {
            field("victorySound", 0, false);
            field("defeatSound", 1, false);
            field(VICTORY_FLAG, 2, false);
            button(tr("roundsGenerator", draft.get("roundsGenerator").getAsBoolean()), left + 8, top + 140, panelWidth - 16, () ->
            {
                draft.addProperty("roundsGenerator", !draft.get("roundsGenerator").getAsBoolean());
                rebuild();
            });
            button(tr("victorySoundEnabled", draft.get("victorySoundEnabled").getAsBoolean()), left + 8, top + 165, panelWidth - 16, () ->
            {
                draft.addProperty("victorySoundEnabled", !draft.get("victorySoundEnabled").getAsBoolean());
                rebuild();
            });
        }
        else
        {
            field("unlockLevel", 0, true);
            field("requiredPrestigeLevel", 1, true);
            field("classLimit", 2, true);
            field("classLimitPercent", 3, true);
        }
    }

    @SuppressWarnings("java:S110") // Minecraft RemotePlayer has a fixed vanilla inheritance hierarchy.
    private void equipment()
    {
        button(tr("inventory"), left + 8, top + 55, 95, () -> picker(true));
        button(tr("catalogue"), left + 108, top + 55, 95, () -> picker(false));
        button(tr("capture"), left + 208, top + 55, Math.max(88, panelWidth - 216), this::capture);
        if (minecraft != null && minecraft.level != null && preview == null)
            preview = new RemotePlayer(minecraft.level, new GameProfile(UUID.randomUUID(), "Editor"))
            {
                @Override
                @NotNull
                public PlayerSkin getSkin()
                {
                    PlayerSkin skin = super.getSkin();
                    return previewSkin == null ? skin : new PlayerSkin(previewSkin, skin.textureUrl(), skin.capeTexture(), skin.elytraTexture(), skin.model(), skin.secure());
                }
            };
    }

    private void picker(boolean inventory)
    {
        minecraft.setScreen(new EditorItemPickerScreen(this, inventory, stack -> carried = stack));
    }

    private void capture()
    {
        previewClass = false;
        if (minecraft.player == null)
            return;
        JsonObject armour = draft.getAsJsonObject("armour");
        EditorDescriptions.ARMOUR.forEach(slot ->
        {
            ItemStack stack = minecraft.player.getItemBySlot(slot);
            armour.add(slot.getName(), stack.isEmpty() ? new JsonPrimitive("empty") : EditorStacks.save(stack));
        });
        if (!team)
        {
            JsonObject items = new JsonObject();
            for (int slot = 0; slot < 41; slot++)
            {
                if (slot >= 36 && slot != 40)
                    continue;
                ItemStack stack = minecraft.player.getInventory().getItem(slot);
                if (!stack.isEmpty())
                    items.add(Integer.toString(slot), EditorStacks.save(stack));
            }
            draft.add("items", items);
        }
    }

    private void memberships()
    {
        String needle = query.toLowerCase(Locale.ROOT);
        List<PlayerClass> available = PlayerClass.values().stream().filter(type -> membershipIndex(EditorCatalog.key(type)) != Integer.MAX_VALUE)
            .filter(type -> type.getName().toLowerCase(Locale.ROOT).contains(needle) || type.getShortName().contains(needle))
            .sorted(Comparator.<PlayerClass>comparingInt(type -> membershipIndex(EditorCatalog.key(type))).thenComparing(PlayerClass::getName, String.CASE_INSENSITIVE_ORDER)).toList();
        EditBox search = new EditBox(font, left + 8, top + 80, panelWidth - 116, 20, tr("search"));
        search.setHint(tr("search"));
        search.setValue(query);
        search.setResponder(text ->
        {
            query = text;
            page = 0;
            rebuild();
            children().stream().filter(EditBox.class::isInstance).findFirst().ifPresent(this::setFocused);
        });
        addRenderableWidget(search);
        button(tr("add_class"), left + panelWidth - 103, top + 80, 95, () ->
        {
            Set<String> assigned = new HashSet<>();
            draft.getAsJsonArray("classes").forEach(value -> assigned.add(value.getAsString()));
            minecraft.setScreen(new EditorClassPickerScreen(this, assigned, type ->
            {
                selectedClass = EditorCatalog.key(type);
                draft.getAsJsonArray("classes").add(selectedClass);
                query = "";
                page = 0;
            }));
        });
        int count = Math.max(1, (panelHeight - 170) / 23);
        for (int index = page * count; index < Math.min(available.size(), (page + 1) * count); index++)
        {
            PlayerClass type = available.get(index);
            String id = EditorCatalog.key(type);
            int rowY = top + 105 + index % count * 23;
            String label = (membershipIndex(id) + 1) + ". " + type.getName();
            int removeWidth = Math.max(90, font.width(tr("remove_class")) + 12);
            membershipButtons.put(id, button(Component.literal(label), left + 8, rowY, panelWidth - removeWidth - 20, () ->
            {
                selectedClass = id;
                rebuild();
            }));
            button(tr("remove_class"), left + panelWidth - removeWidth - 8, rowY, removeWidth, () ->
            {
                draft.getAsJsonArray("classes").remove(new JsonPrimitive(id));
                if (selectedClass.equals(id))
                    selectedClass = "";
                page = Math.min(page, Math.max(0, (available.size() - 2) / count));
                rebuild();
            });
        }
        button(Component.literal("<"), left + 8, top + 55, 25, () ->
        {
            page = Math.max(0, page - 1);
            rebuild();
        });
        button(Component.literal(">"), left + 38, top + 55, 25, () ->
        {
            if ((page + 1) * count < available.size())
                page++;
            rebuild();
        });
        button(tr("up"), left + 75, top + 55, 65, () -> moveClass(-1));
        button(tr("down"), left + 145, top + 55, 65, () -> moveClass(1));
        button(tr("preview"), left + 215, top + 55, panelWidth - 223, () ->
        {
            JsonArray classes = draft.getAsJsonArray("classes");
            if (!classes.isEmpty())
            {
                if (membershipIndex(selectedClass) == Integer.MAX_VALUE)
                    selectedClass = classes.get(0).getAsString();
                previewClass = true;
                tab = 1;
                rebuild();
            }
        });
    }

    private int membershipIndex(String id)
    {
        JsonArray classes = draft.getAsJsonArray("classes");
        for (int index = 0; index < classes.size(); index++)
            if (classes.get(index).getAsString().equals(id))
                return index;
        return Integer.MAX_VALUE;
    }

    private void moveClass(int direction)
    {
        JsonArray classes = draft.getAsJsonArray("classes");
        List<JsonElement> ordered = new ArrayList<>();
        classes.forEach(ordered::add);
        int index = ordered.indexOf(new JsonPrimitive(selectedClass));
        if (index < 0 || index + direction < 0 || index + direction >= ordered.size())
            return;
        Collections.swap(ordered, index, index + direction);
        JsonArray replacement = new JsonArray();
        ordered.forEach(replacement::add);
        draft.add("classes", replacement);
        rebuild();
    }

    private void skins()
    {
        button(tr("choose_skin"), left + 8, top + 55, panelWidth - 16, () -> minecraft.setScreen(new EditorSkinPickerScreen(this, texture ->
        {
            JsonArray skins = draft.getAsJsonArray("skins");
            JsonPrimitive value = new JsonPrimitive(texture.toString());
            if (!skins.contains(value))
                skins.add(value);
        })));
        JsonArray skins = draft.getAsJsonArray("skins");
        int count = Math.max(1, (panelHeight - 145) / 24);
        for (int index = page * count; index < Math.min(skins.size(), (page + 1) * count); index++)
        {
            int target = index;
            button(Component.literal("× " + skins.get(index).getAsString()), left + 8, top + 80 + index % count * 24, panelWidth - 16, () ->
            {
                skins.remove(target);
                rebuild();
            });
        }
        button(Component.literal("<"), left + 8, top + panelHeight - 70, 25, () ->
        {
            page = Math.max(0, page - 1);
            rebuild();
        });
        button(Component.literal(">"), left + 38, top + panelHeight - 70, 25, () ->
        {
            if ((page + 1) * count < skins.size())
                page++;
            rebuild();
        });
    }

    private boolean validate()
    {
        errors.remove("draft");
        JsonObject entries = new JsonObject();
        entries.add(key, draft);
        try
        {
            EditorSchema.validate(new TeamEditorStore.Document(team ? entries : new JsonObject(), team ? new JsonObject() : entries));
            return errors.isEmpty();
        }
        catch (RuntimeException exception)
        {
            errors.put("draft", exception.getMessage());
            return false;
        }
    }

    private void save()
    {
        if (!validate())
            return;
        JsonObject values = creating ? draft : EditorDescriptions.changes(initial, draft);
        if (values.toString().length() > 262144)
        {
            message = tr("too_large").getString();
            return;
        }
        PacketHandler.sendToServer(new PacketEditorAction(creating ? "create" : "save", team, key, values, editingRevision));
    }

    private void confirm(String operation)
    {
        minecraft.setScreen(new ConfirmScreen(accepted ->
        {
            if (accepted)
                PacketHandler.sendToServer(new PacketEditorAction(operation, team, key, new JsonObject(), editingRevision));
            minecraft.setScreen(this);
        }, tr("confirm_" + operation), Component.literal(key + " · " + references())));
    }

    private String references()
    {
        if (team)
            return tr("round_references").getString();
        return Team.values().stream().filter(candidate -> candidate.getClasses().stream().anyMatch(type -> EditorCatalog.key(type).equals(key))).map(Team::getName)
            .reduce((first, second) -> first + ", " + second).orElse(tr("no_teams").getString());
    }

    private void nameDialog(boolean duplicate)
    {
        JsonObject source = duplicate ? draft.deepCopy() : null;
        minecraft.setScreen(new Screen(tr(duplicate ? "duplicate" : "new"))
        {
            private EditBox id;
            private EditBox name;

            @Override
            protected void init()
            {
                int x = width / 2 - 130;
                id = new EditBox(font, x, height / 2 - 50, 260, 20, tr("id"));
                id.setHint(tr("id"));
                id.setMaxLength(100);
                addRenderableWidget(id);
                name = new EditBox(font, x, height / 2 - 20, 260, 20, tr("name"));
                name.setHint(tr("name"));
                name.setMaxLength(128);
                if (source != null)
                    name.setValue(source.get("name").getAsString());
                addRenderableWidget(name);
                addRenderableWidget(Button.builder(tr("create"), ignored ->
                {
                    String customKey = "custom:" + id.getValue().replaceFirst("^custom:", "").toLowerCase(Locale.ROOT);
                    JsonObject values = source == null ? new JsonObject() : source.deepCopy();
                    values.addProperty("name", name.getValue());
                    JsonObject entries = new JsonObject();
                    entries.add(customKey, values);
                    try
                    {
                        EditorSchema.validate(new TeamEditorStore.Document(team ? entries : new JsonObject(), team ? new JsonObject() : entries));
                        key = customKey;
                        draft = source == null ? describe(null) : values;
                        draft.addProperty("name", name.getValue());
                        initial = new JsonObject();
                        detail = true;
                        creating = true;
                        editingRevision = EditorClientState.revision();
                        tab = 0;
                        preview = null;
                        errors.clear();
                        rawInputs.clear();
                        minecraft.setScreen(TeamsEditorScreen.this);
                    }
                    catch (RuntimeException exception)
                    {
                        id.setTextColor(0xFF5555);
                    }
                }).bounds(x, height / 2 + 12, 125, 20).build());
                addRenderableWidget(Button.builder(tr("back"), ignored -> onClose()).bounds(x + 135, height / 2 + 12, 125, 20).build());
            }

            @Override
            public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
            {
                ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
                super.render(graphics, mouseX, mouseY, partialTick);
                graphics.drawCenteredString(font, title, width / 2, height / 2 - 72, 0xFFFFFF);
            }

            @Override
            public void onClose()
            {
                minecraft.setScreen(TeamsEditorScreen.this);
            }

            @Override
            public boolean isPauseScreen()
            {
                return false;
            }
        });
    }

    public void serverUpdate(String message)
    {
        if (!message.isEmpty())
            this.message = message;
        else if (detail && editingRevision != EditorClientState.revision())
            this.message = tr("stale").getString();
        if (!detail)
            rebuild();
    }

    private void back()
    {
        if (!initial.equals(draft))
            minecraft.setScreen(new ConfirmScreen(accepted ->
            {
                minecraft.setScreen(this);
                if (accepted)
                    leaveDetails();
            }, tr("discard"), tr("unsaved")));
        else
            leaveDetails();
    }

    private void leaveDetails()
    {
        detail = false;
        carried = ItemStack.EMPTY;
        query = "";
        page = 0;
        errors.clear();
        rawInputs.clear();
        rebuild();
    }

    @Override
    public void onClose()
    {
        if (detail)
            back();
        else
            super.onClose();
    }

    @Override
    public boolean isPauseScreen()
    {
        return false;
    }

    @Override
    public void tick()
    {
        if (saveButton != null && detail)
            saveButton.active = errors.isEmpty();
    }

    private record Cell(int x, int y, String slot, boolean armour, boolean source)
    {}

    private List<Cell> cells()
    {
        List<Cell> cells = new ArrayList<>();
        int y = top + 89;
        if (!team)
        {
            for (int slot = 0; slot < 36; slot++)
                cells.add(new Cell(left + 10 + slot % 9 * 18, y + (slot < 9 ? 3 : slot / 9 - 1) * 18, Integer.toString(slot), false, false));
            cells.add(new Cell(left + 154, y + 76, "40", false, false));
        }
        for (int i = 0; i < EditorDescriptions.ARMOUR.size(); i++)
            cells.add(new Cell(left + 10 + i * 26, y + 76, EditorDescriptions.ARMOUR.get(i).getName(), true, false));
        if (panelWidth >= 390)
            for (int slot = 0; slot < 36; slot++)
                cells.add(new Cell(left + 205 + slot % 9 * 18, y + (slot < 9 ? 3 : slot / 9 - 1) * 18, Integer.toString(slot), false, true));
        return cells;
    }

    private ItemStack stack(Cell cell)
    {
        if (cell.source())
            return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getInventory().getItem(Integer.parseInt(cell.slot()));
        JsonObject slots = draft.getAsJsonObject(cell.armour() ? "armour" : "items");
        JsonElement value = slots.get(cell.slot());
        return value != null && value.isJsonObject() ? EditorStacks.load(value.getAsJsonObject()) : ItemStack.EMPTY;
    }

    private static boolean contains(Cell cell, double x, double y)
    {
        return x >= cell.x() && x < cell.x() + 18 && y >= cell.y() && y < cell.y() + 18;
    }

    private void quickCopy()
    {
        previewClass = false;
        EquipmentSlot armour = preview == null ? EquipmentSlot.MAINHAND : EntityPlatform.equipmentSlotFor(preview, carried);
        if (EditorDescriptions.ARMOUR.contains(armour))
        {
            draft.getAsJsonObject("armour").add(armour.getName(), EditorStacks.save(carried));
            carried = ItemStack.EMPTY;
        }
        else if (!team)
        {
            JsonObject items = draft.getAsJsonObject("items");
            for (int slot = 0; slot < 36; slot++)
                if (!items.has(Integer.toString(slot)))
                {
                    items.add(Integer.toString(slot), EditorStacks.save(carried));
                    carried = ItemStack.EMPTY;
                    break;
                }
        }
    }

    private void place(Cell cell)
    {
        previewClass = false;
        draft.getAsJsonObject(cell.armour() ? "armour" : "items").add(cell.slot(), EditorStacks.save(carried));
        carried = ItemStack.EMPTY;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (!detail)
        {
            List<Row> rows = filteredRows();
            int index = page * rowCount() + ((int) mouseY - top - 78) / 23;
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 8 && mouseY >= top + 78 && mouseY < top + 78 + rowCount() * 23 && index < rows.size())
            {
                select(rows.get(index).key());
                rebuild();
                return true;
            }
        }
        else if (tab == 1)
        {
            for (Cell cell : cells())
            {
                if (!contains(cell, mouseX, mouseY))
                    continue;
                if (cell.source())
                {
                    carried = stack(cell).copy();
                    if (hasShiftDown() && !carried.isEmpty())
                        quickCopy();
                }
                else
                {
                    previewClass = false;
                    JsonObject slots = draft.getAsJsonObject(cell.armour() ? "armour" : "items");
                    if (button == 1)
                    {
                        if (cell.armour())
                        {
                            JsonElement value = slots.get(cell.slot());
                            slots.addProperty(cell.slot(), !team && value != null && value.isJsonPrimitive() && "empty".equals(value.getAsString()) ? "inherit" : "empty");
                        }
                        else
                            slots.remove(cell.slot());
                    }
                    else if (!carried.isEmpty())
                        place(cell);
                    else
                    {
                        carried = stack(cell).copy();
                        if (!carried.isEmpty())
                        {
                            if (cell.armour())
                                slots.addProperty(cell.slot(), "empty");
                            else
                                slots.remove(cell.slot());
                        }
                    }
                }
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        if (detail && tab == 1 && !carried.isEmpty())
            for (Cell cell : cells())
                if (!cell.source() && contains(cell, mouseX, mouseY))
                {
                    place(cell);
                    return true;
                }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double delta)
    {
        if (detail && tab == 1)
            for (Cell cell : cells())
                if (!cell.source() && contains(cell, mouseX, mouseY))
                {
                    ItemStack stack = stack(cell).copy();
                    if (!stack.isEmpty())
                    {
                        previewClass = false;
                        stack.setCount(Math.max(1, Math.min(stack.getMaxStackSize(), stack.getCount() + (delta > 0 ? 1 : -1))));
                        draft.getAsJsonObject(cell.armour() ? "armour" : "items").add(cell.slot(), EditorStacks.save(stack));
                    }
                    return true;
                }
        return super.mouseScrolled(mouseX, mouseY, scrollX, delta);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick)
    {
        ClientPlatform.renderBackground(this, graphics, mouseX, mouseY, partialTick);
        graphics.fill(left, top, left + panelWidth, top + panelHeight, 0xF01A202B);
        graphics.drawString(font, detail ? font.plainSubstrByWidth(draft.get("name").getAsString() + " · " + key, panelWidth - 16) : title.getString(), left + 8, top + 9, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
        if (!detail)
            renderList(graphics, mouseX, mouseY);
        else if (tab == 0 || tab == 3)
            renderFields(graphics);
        else if (tab == 1)
            renderEquipment(graphics, mouseX, mouseY);
        else if (tab == 2 && team)
        {
            Button selected = membershipButtons.get(selectedClass);
            if (selected != null)
                graphics.renderOutline(selected.getX() - 1, selected.getY() - 1, panelWidth - 14, selected.getHeight() + 2, 0xFFFFFF55);
        }
        String status = errors.isEmpty() ? message : errors.values().iterator().next();
        if (status != null && !status.isEmpty())
            graphics.drawString(font, font.plainSubstrByWidth(status, panelWidth - 16), left + 8, top + panelHeight - 42, errors.isEmpty() ? 0xFFDD77 : 0xFF6666);
        if (!carried.isEmpty())
        {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 400);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            graphics.pose().popPose();
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY)
    {
        List<Row> rows = filteredRows();
        for (int index = page * rowCount(); index < Math.min(rows.size(), (page + 1) * rowCount()); index++)
        {
            Row row = rows.get(index);
            int y = top + 78 + index % rowCount() * 23;
            graphics.fill(left + 8, y, left + panelWidth - 8, y + 21, mouseY >= y && mouseY < y + 21 ? 0xFF42516A : 0xFF2B3444);
            graphics.drawString(font, font.plainSubstrByWidth(row.name(), panelWidth - 145), left + 13, y + 2, 0xFFFFFF);
            graphics.drawString(font, font.plainSubstrByWidth(row.pack(), panelWidth - 145), left + 13, y + 12, 0xAABBDD);
            graphics.drawString(font, tr(row.status()), left + panelWidth - 112, y + 6, 0xFFDD77);
            if (mouseX >= left + 8 && mouseX < left + panelWidth - 8 && mouseY >= y && mouseY < y + 21)
                graphics.renderTooltip(font, Component.literal(row.key()), mouseX, mouseY);
        }
        graphics.drawString(font, Integer.toString(page + 1), left + 74, top + panelHeight - 24, 0xFFFFFF);
    }

    private void renderFields(GuiGraphics graphics)
    {
        String[] labels;
        if (tab == 0)
            labels = team ? new String[]{"name", "colour"} : new String[]{"name"};
        else
            labels = team ? new String[]{"victorySound", "defeatSound", VICTORY_FLAG} : new String[]{"unlockLevel", "requiredPrestigeLevel", "classLimit", "classLimitPercent"};
        for (int i = 0; i < labels.length; i++)
            graphics.drawString(font, tr(labels[i]), left + 8, top + 66 + i * 25, 0xCCDDEE);
        if (tab == 0 && team && draft.get("colour").getAsString().matches("#[0-9a-fA-F]{6}"))
        {
            int colour = Integer.parseInt(draft.get("colour").getAsString().substring(1), 16);
            graphics.fill(left + 8, top + 165, left + panelWidth - 8, top + 175, 0xFF000000 | colour);
        }
        if (tab == 0 && !team)
            graphics.drawString(font, font.plainSubstrByWidth(references(), panelWidth - 16), left + 8, top + 145, 0xAABBDD);
        if (tab == 3 && team)
            renderFlagPreview(graphics);
    }

    /**
     * The flag the draft names, below the advanced team settings. A web address is fetched only once
     * the field loses focus, so that typing it does not request every partial address.
     */
    private void renderFlagPreview(GuiGraphics graphics)
    {
        int y = top + 192;
        if (y + FLAG_PREVIEW_HEIGHT > top + panelHeight - 32 || flagField == null || flagField.isFocused() || errors.containsKey(VICTORY_FLAG))
            return;
        TeamFlagTextures.FlagTexture flag = TeamFlagTextures.preview(draft.has(VICTORY_FLAG) ? draft.get(VICTORY_FLAG).getAsString() : "");
        if (flag != null)
            TeamFlagTextures.draw(graphics, flag, left + 8, y, FLAG_PREVIEW_WIDTH, FLAG_PREVIEW_HEIGHT);
    }

    private void renderEquipment(GuiGraphics graphics, int mouseX, int mouseY)
    {
        PlayerClass previewedClass = team && previewClass ? EditorCatalog.resolveClass(selectedClass) : null;
        Component equipmentLabel = tr(team ? "uniform" : "kit");
        if (previewedClass != null)
            equipmentLabel = Component.literal(tr("preview").getString() + ": " + previewedClass.getName());
        graphics.drawString(font, font.plainSubstrByWidth(equipmentLabel.getString(), panelWidth >= 390 ? 190 : panelWidth - 20), left + 10, top + 79, 0xCCDDEE);
        if (panelWidth >= 390)
            graphics.drawString(font, tr("inventory"), left + 205, top + 79, 0xCCDDEE);
        for (Cell cell : cells())
        {
            graphics.fill(cell.x(), cell.y(), cell.x() + 18, cell.y() + 18, 0xFF677384);
            graphics.fill(cell.x() + 1, cell.y() + 1, cell.x() + 17, cell.y() + 17, 0xFF2A303B);
            ItemStack stack = stack(cell);
            if (!stack.isEmpty())
            {
                graphics.renderItem(stack, cell.x() + 1, cell.y() + 1);
                graphics.renderItemDecorations(font, stack, cell.x() + 1, cell.y() + 1);
                if (contains(cell, mouseX, mouseY) && carried.isEmpty())
                    graphics.renderTooltip(font, stack, mouseX, mouseY);
            }
            else if (cell.armour())
            {
                JsonElement value = draft.getAsJsonObject("armour").get(cell.slot());
                boolean inherit = value != null && value.isJsonPrimitive() && "inherit".equals(value.getAsString());
                graphics.drawString(font, inherit ? "T" : "×", cell.x() + 5, cell.y() + 5, 0xAAAAAA);
                if (contains(cell, mouseX, mouseY))
                    graphics.renderTooltip(font, tr(inherit ? "inherit_hint" : "empty_hint", cell.slot()), mouseX, mouseY);
            }
        }
        if ((panelWidth >= 470 || panelHeight >= 300) && preview != null)
        {
            JsonObject outfit = draft.getAsJsonObject("armour");
            // Team equipment edits preview the uniform itself. Class overrides apply only to an explicit class preview.
            PlayerClass chosen = previewedClass;
            Team uniform = team ? null : Team.values().stream().filter(owner -> owner.getClasses().stream().anyMatch(type -> EditorCatalog.key(type).equals(key))).findFirst().orElse(null);
            for (EquipmentSlot slot : EditorDescriptions.ARMOUR)
            {
                JsonElement value = outfit.get(slot.getName());
                ItemStack stack = value != null && value.isJsonObject() ? EditorStacks.load(value.getAsJsonObject()) : ItemStack.EMPTY;
                if (chosen != null)
                {
                    ItemStack piece = chosen.getArmour(slot);
                    if (!piece.isEmpty() || !chosen.usesTeamArmour(slot))
                        stack = piece;
                }
                else if (!team && uniform != null && value != null && value.isJsonPrimitive() && "inherit".equals(value.getAsString()))
                    stack = uniform.getArmour(slot);
                preview.setItemSlot(slot, stack);
            }
            JsonArray skins = draft.getAsJsonArray("skins");
            if (team)
                skins = chosen == null ? new JsonArray() : EditorDescriptions.playerClass(chosen).getAsJsonArray("skins");
            previewSkin = skins.isEmpty() ? null : com.flansmodultimate.client.render.layer.PlayerSkinOverrides.previewTexture(ResourceLocation.tryParse(skins.get(0).getAsString()));
            int previewY = panelWidth >= 470 ? top + 190 : top + panelHeight - 55;
            int scale = panelWidth >= 470 ? 40 : 28;
            ClientPlatform.renderEntityInInventory(graphics, left + panelWidth - 45, previewY, scale, left + panelWidth - 45 - mouseX, previewY - 40 - mouseY, preview);
        }
        if (panelHeight >= 270)
            graphics.drawString(font, tr("equipment_hint"), left + 8, top + 210, 0xAABBDD);
    }

    private final class ColourSlider extends AbstractSliderButton
    {
        private final int channel;

        private ColourSlider(int x, int y, int width, int channel)
        {
            super(x, y, width, 20, Component.empty(), (readColour(draft) >> (16 - channel * 8) & 255) / 255.0);
            this.channel = channel;
            updateMessage();
        }

        @Override
        protected void updateMessage()
        {
            setMessage(Component.literal(new String[]{"R", "G", "B"}[channel] + ": " + (int) Math.round(value * 255)));
        }

        @Override
        protected void applyValue()
        {
            int shift = 16 - channel * 8;
            int rgb = readColour(draft) & ~(255 << shift) | (int) Math.round(value * 255) << shift;
            draft.addProperty("colour", String.format("#%06X", rgb));
            if (fields.size() > 1)
                fields.get(1).setValue(draft.get("colour").getAsString());
        }

        private void sync(int rgb)
        {
            value = (rgb >> (16 - channel * 8) & 255) / 255.0;
            updateMessage();
        }
    }

    private static int readColour(JsonObject draft)
    {
        try
        {
            return Integer.parseInt(draft.get("colour").getAsString().replace("#", ""), 16);
        }
        catch (NumberFormatException ignored)
        {
            return 0;
        }
    }
}
