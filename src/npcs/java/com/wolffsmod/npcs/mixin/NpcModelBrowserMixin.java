package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.content.IContentPack;
import com.flansmodultimate.api.content.IContentType;
import com.flansmodultimate.api.content.PaintjobVariant;
import com.wolffsmod.npcs.client.FlanSoldierBrowser;
import com.wolffsmod.npcs.client.NpcModelNameScroll;
import com.wolffsmod.npcs.client.NpcPresetControls;
import com.wolffsmod.npcs.model.*;
import com.wolffsmod.npcs.network.NpcNetwork;
import com.wolffsmod.npcs.network.NpcPresetPacket;
import com.wolffsmod.npcs.soldier.FlanSoldierSelection;
import noppes.npcs.CustomEntities;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.common.util.NaturalOrderComparator;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.*;

/**
 * Groups ships separately, adds content-pack folders and restores parent viewports in the model picker while retaining Custom NPCs' model selection and save logic.
 * Shows full-name hover tooltips for shortened picker entries and hides the unsupported simple renderer for Flan model entities.
 * Also adds the Flan Soldier folder, which dresses the NPC as a class of a Flan's team, the toggle that applies that class's loadout on selection and the toggle that keeps the NPC's own skin.
 * Offers shared behaviour presets and Undo alongside the selected model.
 * Classes with several skins expose a variant folder with saved skin selection, like vehicle paintjobs.
 */
// Mixin merges this class into the target, so self-casts are valid.
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference", "DataFlowIssue"})
@Mixin(value = GuiCreationEntities.class, remap = false)
public abstract class NpcModelBrowserMixin
{
    @Unique
    private boolean wolffsmodnpcsPresetsRequested;

    @Shadow @SuppressWarnings("MismatchedCollectionQueryUpdate") // Shadowed target list, populated by the target.
    private List<EntityType<? extends Entity>> types;
    @Shadow
    private GuiCustomScrollNop scroll;

    @Unique
    private static final List<FlanModelFolder> WOLFFSMODNPCS_KINDS = List.of(FlanModelFolder.AA_GUN, FlanModelFolder.MECHA, FlanModelFolder.PLANE, FlanModelFolder.VEHICLE, FlanModelFolder.SHIP);
    @Unique
    private static final List<String> WOLFFSMODNPCS_LABELS = List.of("Flan AA Gun", "Flan Mecha", "Flan Plane", "Flan Vehicle", "Flan Ship");
    /** Folder rows at the top of the first level: the model kinds, then the soldier folder. */
    @Unique
    private static final int WOLFFSMODNPCS_TOP_FOLDERS = WOLFFSMODNPCS_KINDS.size() + 1;
    @Unique
    private FlanModelFolder wolffsmodnpcsKind;
    @Unique
    private IContentPack wolffsmodnpcsPack;
    @Unique
    private List<IContentPack> wolffsmodnpcsPacks = List.of();
    @Unique
    private List<EntityType<? extends Entity>> wolffsmodnpcsModels = List.of();
    @Unique
    private FlanModelEntityType wolffsmodnpcsVariantModel;
    @Unique
    private List<PaintjobVariant> wolffsmodnpcsPaintjobs = List.of();
    @Unique
    private Integer wolffsmodnpcsSelectionScrollY;
    @Unique
    private final Deque<Integer> wolffsmodnpcsParentScrollPositions = new ArrayDeque<>();
    /** The soldier folder while the picker is inside it. */
    @Unique
    private FlanSoldierBrowser wolffsmodnpcsSoldiers;

    @Redirect(method = {"init", "m_7856_"}, at = @At(value = "NEW", target = "noppes/npcs/shared/client/gui/components/GuiCustomScrollNop"))
    private GuiCustomScrollNop wolffsmodnpcsNameTooltips(Screen parent, int id)
    {
        return new NpcModelNameScroll(parent, id);
    }

    // Forge production uses SRG names; the development dependency uses Mojang names.
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsFolders(CallbackInfo callback)
    {
        List<String> labels = new ArrayList<>();
        List<EntityType<? extends Entity>> models = new ArrayList<>();
        if (wolffsmodnpcsSoldiers != null)
        {
            labels.add("..");
            labels.addAll(wolffsmodnpcsSoldiers.labels());
        }
        else if (wolffsmodnpcsKind == null)
            wolffsmodnpcsListTopLevel(labels, models);
        else
            wolffsmodnpcsListFolder(labels, models);
        wolffsmodnpcsModels = models;
        // The scroll's equality check ignores ordering; clear first so the row indices stay aligned.
        scroll.setUnsortedList(new ArrayList<>());
        scroll.setUnsortedList(labels);
        GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
        int selected = wolffsmodnpcsSelectedRow(screen, models);
        int offset = wolffsmodnpcsTopLevel() ? WOLFFSMODNPCS_TOP_FOLDERS : 1;
        scroll.setSelectedIndex(selected < 0 ? -1 : selected + offset);
        wolffsmodnpcsRestoreSelectionScroll();
        FlanSoldierBrowser.addLoadoutControls(screen, wolffsmodnpcsSoldiers != null);
        NpcPresetControls.addPicker(screen);
        if (!wolffsmodnpcsPresetsRequested)
        {
            wolffsmodnpcsPresetsRequested = true;
            NpcNetwork.sendToServer(NpcPresetPacket.request(screen.npc.getId()));
        }
        if (FlanModelRendering.manages(screen.playerdata))
        {
            screen.getButton(110).visible = false;
            screen.getButton(110).active = false;
            screen.getLabel(110).visible = false;
            screen.getLabel(110).enabled = false;
        }
    }

    /** The first level: the Flan folders, then every model that is not a Flan one. */
    @Unique
    private void wolffsmodnpcsListTopLevel(List<String> labels, List<EntityType<? extends Entity>> models)
    {
        labels.addAll(WOLFFSMODNPCS_LABELS);
        labels.add(FlanSoldierBrowser.LABEL);
        for (EntityType<? extends Entity> type : types)
        {
            if (!(type instanceof FlanModelEntityType))
            {
                labels.add(type.getDescriptionId());
                models.add(type);
            }
        }
    }

    /** Inside a model folder: its content packs, a pack's models, or a model's paintjobs. */
    @Unique
    private void wolffsmodnpcsListFolder(List<String> labels, List<EntityType<? extends Entity>> models)
    {
        labels.add("..");
        if (wolffsmodnpcsPack == null)
        {
            wolffsmodnpcsPacks = types.stream().filter(type -> type instanceof FlanModelEntityType model && FlanModelFolder.of(model.getInfoType()) == wolffsmodnpcsKind)
                .map(type -> ((FlanModelEntityType) type).getInfoType()).filter(Objects::nonNull).map(IContentType::getContentPack).distinct()
                .sorted(Comparator.comparing(IContentPack::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(pack -> pack.getPath().toString())).toList();
            for (IContentPack pack : wolffsmodnpcsPacks)
            {
                String name = pack.getName();
                labels.add(name.regionMatches(true, Math.max(0, name.length() - 4), ".zip", 0, 4) ? name.substring(0, name.length() - 4) : name);
            }
        }
        else if (wolffsmodnpcsVariantModel != null)
        {
            for (PaintjobVariant job : wolffsmodnpcsPaintjobs)
                labels.add(job.id() == 0 ? "Default" : job.name());
        }
        else
            wolffsmodnpcsListPackModels(labels, models);
    }

    @Unique
    private void wolffsmodnpcsListPackModels(List<String> labels, List<EntityType<? extends Entity>> models)
    {
        for (EntityType<? extends Entity> type : types)
        {
            if (type instanceof FlanModelEntityType model && FlanModelFolder.of(model.getInfoType()) == wolffsmodnpcsKind)
            {
                IContentType definition = model.getInfoType();
                if (definition != null && Objects.equals(definition.getContentPack(), wolffsmodnpcsPack))
                {
                    models.add(type);
                }
            }
        }
        NaturalOrderComparator names = new NaturalOrderComparator();
        models.sort(Comparator.comparing((EntityType<? extends Entity> type) -> I18n.get(type.getDescriptionId()).toLowerCase(Locale.ROOT), names)
            .thenComparing(type -> ((FlanModelEntityType) type).getShortName()));
        for (EntityType<? extends Entity> type : models)
            labels.add(type.getDescriptionId());
    }

    /** @return the row of the NPC's current model, paintjob or class in the listed level, before its folder rows, or -1 */
    @Unique
    private int wolffsmodnpcsSelectedRow(GuiCreationScreenInterface screen, List<EntityType<? extends Entity>> models)
    {
        if (wolffsmodnpcsSoldiers != null)
            return wolffsmodnpcsSoldiers.selectedRow(FlanSoldierSelection.read(screen.playerdata.extra).orElse(null));
        int selected = models.indexOf(screen.entity == null ? CustomEntities.entityCustomNpc : screen.entity.getType());
        if (wolffsmodnpcsVariantModel != null && screen.entity instanceof FlanModelEntity model && model.getType() == wolffsmodnpcsVariantModel)
        {
            for (int i = 0; i < wolffsmodnpcsPaintjobs.size(); i++)
                if (wolffsmodnpcsPaintjobs.get(i).id() == model.getPaintjobId())
                    selected = i;
        }
        return selected;
    }

    @Unique
    private boolean wolffsmodnpcsTopLevel()
    {
        return wolffsmodnpcsKind == null && wolffsmodnpcsSoldiers == null;
    }

    @Unique
    private void wolffsmodnpcsRestoreSelectionScroll()
    {
        if (wolffsmodnpcsSelectionScrollY != null)
        {
            ((NpcModelScrollAccessor) scroll).wolffsmodnpcsSetScrollY(wolffsmodnpcsSelectionScrollY);
            wolffsmodnpcsSelectionScrollY = null;
        }
    }

    @Inject(method = "scrollClicked", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNavigate(double mouseX, double mouseY, int button, GuiCustomScrollNop clicked, CallbackInfo callback)
    {
        int index = clicked.getSelectedIndex();
        if (index < 0)
        {
            callback.cancel();
            return;
        }
        if (wolffsmodnpcsSoldiers != null || wolffsmodnpcsKind == null && index == WOLFFSMODNPCS_KINDS.size())
        {
            wolffsmodnpcsNavigateSoldiers(index, clicked);
            callback.cancel();
            return;
        }
        if (wolffsmodnpcsChangeFolder(index, clicked))
        {
            // A search for a folder must not hide the contents of the next folder or its back row.
            clicked.clear();
            ((GuiCreationEntities) (Object) this).init();
            callback.cancel();
            return;
        }
        if (wolffsmodnpcsVariantModel != null)
        {
            wolffsmodnpcsSelectPaintjob(index, clicked);
            callback.cancel();
            return;
        }
        wolffsmodnpcsSelectModel(index, clicked);
    }

    /** @return whether the click entered a model folder or a pack, or went back one level */
    @Unique
    private boolean wolffsmodnpcsChangeFolder(int index, GuiCustomScrollNop clicked)
    {
        if (wolffsmodnpcsKind == null && index < WOLFFSMODNPCS_KINDS.size())
        {
            wolffsmodnpcsParentScrollPositions.push(((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY());
            wolffsmodnpcsKind = WOLFFSMODNPCS_KINDS.get(index);
            return true;
        }
        if (wolffsmodnpcsKind != null && index == 0)
        {
            wolffsmodnpcsSelectionScrollY = wolffsmodnpcsParentScrollPositions.pop();
            if (wolffsmodnpcsVariantModel != null)
                wolffsmodnpcsVariantModel = null;
            else if (wolffsmodnpcsPack != null)
                wolffsmodnpcsPack = null;
            else
                wolffsmodnpcsKind = null;
            return true;
        }
        if (wolffsmodnpcsKind != null && wolffsmodnpcsPack == null)
        {
            wolffsmodnpcsParentScrollPositions.push(((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY());
            wolffsmodnpcsPack = wolffsmodnpcsPacks.get(index - 1);
            return true;
        }
        return false;
    }

    @Unique
    private void wolffsmodnpcsSelectPaintjob(int index, GuiCustomScrollNop clicked)
    {
        wolffsmodnpcsSelectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY();
        PaintjobVariant job = wolffsmodnpcsPaintjobs.get(index - 1);
        GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
        if (!Objects.equals(screen.playerdata.getEntityName(), BuiltInRegistries.ENTITY_TYPE.getKey(wolffsmodnpcsVariantModel)))
            screen.playerdata.setEntity(BuiltInRegistries.ENTITY_TYPE.getKey(wolffsmodnpcsVariantModel));
        else
            screen.playerdata.clearEntity();
        // Custom NPCs saves and synchronizes ExtraData with the selected model and presets.
        screen.playerdata.extra.putInt(FlanModelEntity.PAINTJOB_KEY, job.id());
        if (screen.playerdata.getEntity(screen.npc) instanceof FlanModelEntity model && model.getModelTexture() != null)
            screen.npc.display.setSkinTexture(model.getModelTexture().toString());
        ((GuiCreationEntities) (Object) this).init();
    }

    /** Hands a model row to Custom NPCs' own selection handler, which runs after this hook returns. */
    @Unique
    private void wolffsmodnpcsSelectModel(int index, GuiCustomScrollNop clicked)
    {
        int offset = wolffsmodnpcsKind == null ? WOLFFSMODNPCS_TOP_FOLDERS : 1;
        EntityType<? extends Entity> selected = wolffsmodnpcsModels.get(index - offset);
        wolffsmodnpcsSelectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY();
        if (selected instanceof FlanModelEntityType model && model.getInfoType() != null)
        {
            List<PaintjobVariant> jobs = model.getInfoType().getPaintjobVariants();
            if (jobs.stream().anyMatch(job -> job.id() != 0))
            {
                wolffsmodnpcsParentScrollPositions.push(wolffsmodnpcsSelectionScrollY);
                wolffsmodnpcsVariantModel = model;
                wolffsmodnpcsPaintjobs = jobs;
                wolffsmodnpcsSelectionScrollY = null;
                clicked.clear();
            }
        }
        // Restore the original index space just for Custom NPCs' selection handler. It updates
        // the preview, skin and saved entity id, then calls init(), which restores the folders.
        clicked.setUnsortedList(new ArrayList<>());
        clicked.setUnsortedList(types.stream().map(EntityType::getDescriptionId).toList());
        clicked.setSelectedIndex(types.indexOf(selected));
    }

    /** Handles a click on the soldier folder row or inside the folder: packs, their teams, then a team's classes. */
    @Unique
    private void wolffsmodnpcsNavigateSoldiers(int index, GuiCustomScrollNop clicked)
    {
        int scrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY();
        if (wolffsmodnpcsSoldiers == null)
        {
            wolffsmodnpcsParentScrollPositions.push(scrollY);
            wolffsmodnpcsSoldiers = new FlanSoldierBrowser();
            clicked.clear();
        }
        else if (index == 0)
        {
            wolffsmodnpcsSelectionScrollY = wolffsmodnpcsParentScrollPositions.pop();
            if (!wolffsmodnpcsSoldiers.back())
                wolffsmodnpcsSoldiers = null;
            clicked.clear();
        }
        else
        {
            boolean wasBrowsingSkins = wolffsmodnpcsSoldiers.browsingSkins();
            Optional<FlanSoldierSelection> selection = wolffsmodnpcsSoldiers.open(index - 1);
            boolean enteredSkins = !wasBrowsingSkins && wolffsmodnpcsSoldiers.browsingSkins();
            if (enteredSkins)
            {
                wolffsmodnpcsParentScrollPositions.push(scrollY);
                clicked.clear();
            }
            if (selection.isPresent())
            {
                // The equipment changes too only while the loadout toggle is on.
                wolffsmodnpcsSelectionScrollY = enteredSkins ? null : scrollY;
                FlanSoldierBrowser.choose((GuiCreationScreenInterface) (Object) this, selection.get());
            }
            else
            {
                wolffsmodnpcsParentScrollPositions.push(scrollY);
                clicked.clear();
            }
        }
        ((GuiCreationEntities) (Object) this).init();
    }
}
