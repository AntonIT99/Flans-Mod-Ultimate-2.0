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
@Mixin(value = GuiCreationEntities.class, remap = false)
public abstract class NpcModelBrowserMixin
{
    @Shadow
    private List<EntityType<? extends Entity>> types;
    @Shadow
    private GuiCustomScrollNop scroll;

    @Unique
    private static final List<FlanModelKind> wolffsmodnpcs$kinds = List.of(FlanModelKind.AA_GUN, FlanModelKind.MECHA, FlanModelKind.PLANE, FlanModelKind.VEHICLE);
    @Unique
    private static final List<String> wolffsmodnpcs$labels = List.of("Flan AA Gun", "Flan Mecha", "Flan Plane", "Flan Vehicle");
    @Unique
    private FlanModelKind wolffsmodnpcs$kind;
    @Unique
    private IContentPack wolffsmodnpcs$pack;
    @Unique
    private List<IContentPack> wolffsmodnpcs$packs = List.of();
    @Unique
    private List<EntityType<? extends Entity>> wolffsmodnpcs$models = List.of();
    @Unique
    private FlanModelEntityType wolffsmodnpcs$variantModel;
    @Unique
    private List<PaintjobVariant> wolffsmodnpcs$paintjobs = List.of();
    @Unique @SuppressWarnings("java:S116") // Mixin fields use a module prefix to avoid collisions in the target class.
    private Integer wolffsmodnpcs$selectionScrollY;

    // Forge production uses SRG names; the development dependency uses Mojang names.
    @Inject(method = {"init", "m_7856_"}, at = @At("TAIL"))
    private void wolffsmodnpcs$folders(CallbackInfo callback)
    {
        List<String> labels = new ArrayList<>();
        List<EntityType<? extends Entity>> models = new ArrayList<>();
        if (wolffsmodnpcs$kind == null)
        {
            labels.addAll(wolffsmodnpcs$labels);
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
            if (wolffsmodnpcs$pack == null)
            {
                wolffsmodnpcs$packs = types.stream().filter(type -> type instanceof FlanModelEntityType model && model.getKind() == wolffsmodnpcs$kind)
                    .map(type -> ((FlanModelEntityType) type).getInfoType()).filter(Objects::nonNull).map(IContentType::getContentPack).distinct()
                    .sorted(Comparator.comparing(IContentPack::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(pack -> pack.getPath().toString())).toList();
                for (IContentPack pack : wolffsmodnpcs$packs)
                    labels.add(pack.getName());
            }
            else if (wolffsmodnpcs$variantModel != null)
            {
                for (PaintjobVariant job : wolffsmodnpcs$paintjobs)
                    labels.add(job.id() == 0 ? "Default" : job.name());
            }
            else
            {
                for (EntityType<? extends Entity> type : types)
                {
                    if (type instanceof FlanModelEntityType model && model.getKind() == wolffsmodnpcs$kind)
                    {
                        IContentType definition = model.getInfoType();
                        if (definition != null && Objects.equals(definition.getContentPack(), wolffsmodnpcs$pack))
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
        wolffsmodnpcs$models = models;
        // The scroll's equality check ignores ordering; clear first so the row indices stay aligned.
        scroll.setUnsortedList(new ArrayList<>());
        scroll.setUnsortedList(labels);
        GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
        int selected = models.indexOf(screen.entity == null ? CustomEntities.entityCustomNpc : screen.entity.getType());
        if (wolffsmodnpcs$variantModel != null && screen.entity instanceof FlanModelEntity model && model.getType() == wolffsmodnpcs$variantModel)
        {
            for (int i = 0; i < wolffsmodnpcs$paintjobs.size(); i++)
                if (wolffsmodnpcs$paintjobs.get(i).id() == model.getPaintjobId())
                    selected = i;
        }
        int offset = wolffsmodnpcs$kind == null ? wolffsmodnpcs$kinds.size() : 1;
        scroll.setSelectedIndex(selected < 0 ? -1 : selected + offset);
        wolffsmodnpcsRestoreSelectionScroll();
    }

    @Unique
    private void wolffsmodnpcsRestoreSelectionScroll()
    {
        if (wolffsmodnpcs$selectionScrollY != null)
        {
            ((NpcModelScrollAccessor) scroll).wolffsmodnpcs$setScrollY(wolffsmodnpcs$selectionScrollY);
            wolffsmodnpcs$selectionScrollY = null;
        }
    }

    @Inject(method = "scrollClicked", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcs$navigate(double mouseX, double mouseY, int button, GuiCustomScrollNop clicked, CallbackInfo callback)
    {
        int index = clicked.getSelectedIndex();
        if (index < 0)
        {
            callback.cancel();
            return;
        }
        boolean navigation = false;
        if (wolffsmodnpcs$kind == null && index < wolffsmodnpcs$kinds.size())
        {
            wolffsmodnpcs$kind = wolffsmodnpcs$kinds.get(index);
            navigation = true;
        }
        else if (wolffsmodnpcs$kind != null && index == 0)
        {
            if (wolffsmodnpcs$variantModel != null)
                wolffsmodnpcs$variantModel = null;
            else if (wolffsmodnpcs$pack != null)
                wolffsmodnpcs$pack = null;
            else
                wolffsmodnpcs$kind = null;
            navigation = true;
        }
        else if (wolffsmodnpcs$kind != null && wolffsmodnpcs$pack == null)
        {
            wolffsmodnpcs$pack = wolffsmodnpcs$packs.get(index - 1);
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
        int offset = wolffsmodnpcs$kind == null ? wolffsmodnpcs$kinds.size() : 1;
        if (wolffsmodnpcs$variantModel != null)
        {
            wolffsmodnpcs$selectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcs$getScrollY();
            PaintjobVariant job = wolffsmodnpcs$paintjobs.get(index - 1);
            GuiCreationScreenInterface screen = (GuiCreationScreenInterface) (Object) this;
            if (!Objects.equals(screen.playerdata.getEntityName(), ForgeRegistries.ENTITY_TYPES.getKey(wolffsmodnpcs$variantModel)))
                screen.playerdata.setEntity(ForgeRegistries.ENTITY_TYPES.getKey(wolffsmodnpcs$variantModel));
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
        EntityType<? extends Entity> selected = wolffsmodnpcs$models.get(index - offset);
        wolffsmodnpcs$selectionScrollY = ((NpcModelScrollAccessor) clicked).wolffsmodnpcs$getScrollY();
        if (selected instanceof FlanModelEntityType model && model.getInfoType() != null)
        {
            List<PaintjobVariant> jobs = model.getInfoType().getPaintjobVariants();
            if (jobs.stream().anyMatch(job -> job.id() != 0))
            {
                wolffsmodnpcs$variantModel = model;
                wolffsmodnpcs$paintjobs = jobs;
                wolffsmodnpcs$selectionScrollY = null;
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
