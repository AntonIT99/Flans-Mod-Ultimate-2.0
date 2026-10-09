package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.IContentPack;
import com.flansmodultimate.api.IContentType;
import com.flansmodultimate.api.PaintjobVariant;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelEntityType;
import com.wolffsmod.npcs.model.FlanModelKind;
import net.minecraftforge.registries.ForgeRegistries;
import noppes.npcs.CustomEntities;
import noppes.npcs.client.gui.model.GuiCreationEntities;
import noppes.npcs.client.gui.model.GuiCreationScreenInterface;
import noppes.npcs.shared.client.gui.components.GuiCustomScrollNop;
import noppes.npcs.shared.common.util.NaturalOrderComparator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Adds folders to the model picker while retaining Custom NPCs' model selection and save logic. */
@SuppressWarnings({"AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
@Mixin(value = GuiCreationEntities.class, remap = false)
public abstract class NpcModelBrowserMixin
{
    @Shadow
    private List<EntityType<? extends Entity>> types;
    @Shadow
    private GuiCustomScrollNop scroll;

    @Unique
    private static final List<FlanModelKind> WOLFFSMODNPCS_KINDS = List.of(FlanModelKind.AA_GUN, FlanModelKind.MECHA, FlanModelKind.PLANE, FlanModelKind.VEHICLE);
    @Unique
    private static final List<String> WOLFFSMODNPCS_LABELS = List.of("Flan AA Gun", "Flan Mecha", "Flan Plane", "Flan Vehicle");
    @Unique
    private FlanModelKind wolffsmodnpcsKind;
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

    // Forge production uses SRG names; the development dependency uses Mojang names.
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcsFolders(CallbackInfo callback)
    {
        List<String> labels = new ArrayList<>();
        List<EntityType<? extends Entity>> models = new ArrayList<>();
        if (wolffsmodnpcsKind == null)
        {
            labels.addAll(WOLFFSMODNPCS_LABELS);
            for (EntityType<? extends Entity> type : types)
            {
                if (!(type instanceof FlanModelEntityType))
                {
                    labels.add(type.getDescriptionId());
                    models.add(type);
                }
            }
        }
        else
        {
            labels.add("..");
            if (wolffsmodnpcsPack == null)
            {
                wolffsmodnpcsPacks = types.stream().filter(type -> type instanceof FlanModelEntityType model && model.getKind() == wolffsmodnpcsKind)
                    .map(type -> ((FlanModelEntityType) type).getInfoType()).filter(Objects::nonNull).map(IContentType::getContentPack).distinct()
                    .sorted(Comparator.comparing(IContentPack::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(pack -> pack.getPath().toString())).toList();
                for (IContentPack pack : wolffsmodnpcsPacks)
                    labels.add(pack.getName());
            }
            else if (wolffsmodnpcsVariantModel != null)
            {
                for (PaintjobVariant job : wolffsmodnpcsPaintjobs)
                    labels.add(job.id() == 0 ? "Default" : job.name());
            }
            else
            {
                for (EntityType<? extends Entity> type : types)
                {
                    if (type instanceof FlanModelEntityType model && model.getKind() == wolffsmodnpcsKind)
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
        }
        wolffsmodnpcsModels = models;
        // The scroll's equality check ignores ordering; clear first so the row indices stay aligned.
        scroll.setUnsortedList(new ArrayList<>());
        scroll.setUnsortedList(labels);
        GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
        int selected = models.indexOf(screen.entity == null ? CustomEntities.entityCustomNpc : screen.entity.getType());
        if (wolffsmodnpcsVariantModel != null && screen.entity instanceof FlanModelEntity model && model.getType() == wolffsmodnpcsVariantModel)
        {
            for (int i = 0; i < wolffsmodnpcsPaintjobs.size(); i++)
                if (wolffsmodnpcsPaintjobs.get(i).id() == model.getPaintjobId())
                    selected = i;
        }
        int offset = wolffsmodnpcsKind == null ? WOLFFSMODNPCS_KINDS.size() : 1;
        scroll.setSelectedIndex(selected < 0 ? -1 : selected + offset);
        wolffsmodnpcsRestoreSelectionScroll();
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
        boolean navigation = false;
        if (wolffsmodnpcsKind == null && index < WOLFFSMODNPCS_KINDS.size())
        {
            wolffsmodnpcsKind = WOLFFSMODNPCS_KINDS.get(index);
            navigation = true;
        }
        else if (wolffsmodnpcsKind != null && index == 0)
        {
            if (wolffsmodnpcsVariantModel != null)
                wolffsmodnpcsVariantModel = null;
            else if (wolffsmodnpcsPack != null)
                wolffsmodnpcsPack = null;
            else
                wolffsmodnpcsKind = null;
            navigation = true;
        }
        else if (wolffsmodnpcsKind != null && wolffsmodnpcsPack == null)
        {
            wolffsmodnpcsPack = wolffsmodnpcsPacks.get(index - 1);
            navigation = true;
        }
        if (navigation)
        {
            // A search for a folder must not hide the contents of the next folder or its back row.
            clicked.clear();
            ((GuiCreationEntities) (Object) this).init();
            callback.cancel();
            return;
        }
        int offset = wolffsmodnpcsKind == null ? WOLFFSMODNPCS_KINDS.size() : 1;
        if (wolffsmodnpcsVariantModel != null)
        {
            wolffsmodnpcsSelectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY();
            PaintjobVariant job = wolffsmodnpcsPaintjobs.get(index - 1);
            GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
            if (!Objects.equals(screen.playerdata.getEntityName(), ForgeRegistries.ENTITY_TYPES.getKey(wolffsmodnpcsVariantModel)))
                screen.playerdata.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(wolffsmodnpcsVariantModel));
            else
                screen.playerdata.clearEntity();
            // Custom NPCs saves and synchronizes ExtraData with the selected model and presets.
            screen.playerdata.extra.putInt(FlanModelEntity.PAINTJOB_KEY, job.id());
            if (screen.playerdata.getEntity(screen.npc) instanceof FlanModelEntity model && model.getModelTexture() != null)
                screen.npc.display.setSkinTexture(model.getModelTexture().toString());
            ((GuiCreationEntities) (Object) this).init();
            callback.cancel();
            return;
        }
        EntityType<? extends Entity> selected = wolffsmodnpcsModels.get(index - offset);
        wolffsmodnpcsSelectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcsGetScrollY();
        if (selected instanceof FlanModelEntityType model && model.getInfoType() != null)
        {
            List<PaintjobVariant> jobs = model.getInfoType().getPaintjobVariants();
            if (jobs.stream().anyMatch(job -> job.id() != 0))
            {
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
}
