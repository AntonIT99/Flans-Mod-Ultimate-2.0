package com.flansmodultimate.mixin;

import com.flansmodultimate.apocalyse.ApocalypseDatapackSource;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoice;
import com.flansmodultimate.apocalyse.client.ApocalypseWorldChoiceScreen;
import com.flansmodultimate.util.FlansLog;
import com.mojang.datafixers.util.Pair;
import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.WorldDataConfiguration;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Makes the player decide whether a new world gets the Apocalypse dimension before it is created.
 *
 * <p>
 * Creating the world is held back until the player answers. The answer is applied through the
 * same data pack reload the Data Packs screen uses, so the world is validated and created exactly
 * as if the player had moved the Apocalypse pack there by hand.
 * </p>
 */
@SuppressWarnings("AddedMixinMembersNamePattern")
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenApocalypseMixin extends Screen
{
    @Shadow
    public abstract WorldCreationUiState getUiState();

    /** Set while re-entering {@code onCreate} after the player has answered. */
    @Unique
    private boolean flansmodultimateAnswered;

    /** The answer waiting for its data pack reload to finish, or null. */
    @Unique @Nullable
    private Boolean flansmodultimatePendingApocalypse;

    protected CreateWorldScreenApocalypseMixin(Component title)
    {
        super(title);
    }

    @Shadow
    private void onCreate()
    {}

    @Shadow
    @Nullable
    private Pair<Path, PackRepository> getDataPackSelectionSettings(WorldDataConfiguration configuration)
    {
        return null;
    }

    @Shadow
    private void tryApplyNewDataPacks(PackRepository repository, boolean shouldConfirm, Consumer<WorldDataConfiguration> callback)
    {}

    @Inject(method = "onCreate", at = @At("HEAD"), cancellable = true)
    private void flansmodultimateAskAboutApocalypse(CallbackInfo callback)
    {
        if (flansmodultimateAnswered)
        {
            flansmodultimateAnswered = false;
            return;
        }
        if (!ApocalypseWorldChoice.isOffered(getUiState().getSettings().dataConfiguration()))
            return;
        callback.cancel();
        Screen createScreen = this;
        if (minecraft != null)
            minecraft.setScreen(ApocalypseWorldChoiceScreen.forNewWorld(this::flansmodultimateApplyChoice, () -> Optional.ofNullable(minecraft).ifPresent(m -> m.setScreen(createScreen))));
    }

    /**
     * Once the data packs have reloaded with the player's answer, carries on creating the world.
     * The reload returns to this screen without initialising it again, so this waits for the
     * screen's first render after it is shown once more.
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void flansmodultimateResumeAfterReload(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo callback)
    {
        Boolean pending = flansmodultimatePendingApocalypse;
        if (pending == null)
            return;
        flansmodultimatePendingApocalypse = null;
        // A failed reload returns here with the old packs: the player is back on this screen
        // to try again, and nothing is created with a configuration they did not choose.
        // Created outside the screen tick, as a click on Create would be.
        if (BooleanUtils.isTrue(ApocalypseWorldChoice.isEnabled(getUiState().getSettings().dataConfiguration()) == pending) && minecraft != null)
            minecraft.tell(this::flansmodultimateCreateAnswered);
    }

    @Unique
    private void flansmodultimateApplyChoice(boolean withApocalypse)
    {
        Screen createScreen = this;
        WorldDataConfiguration configuration = getUiState().getSettings().dataConfiguration();
        if (ApocalypseWorldChoice.isEnabled(configuration) == withApocalypse)
        {
            if (minecraft != null)
                minecraft.setScreen(createScreen);
            flansmodultimateCreateAnswered();
            return;
        }

        Pair<Path, PackRepository> selection = getDataPackSelectionSettings(configuration);
        if (selection == null)
        {
            FlansLog.log.warn("Could not prepare the data packs to {} the Apocalypse dimension", withApocalypse ? "add" : "remove");
            if (minecraft != null)
                minecraft.setScreen(createScreen);
            return;
        }
        PackRepository repository = selection.getSecond();
        List<String> selected = new ArrayList<>(repository.getSelectedIds());
        selected.remove(ApocalypseDatapackSource.PACK_ID);
        if (withApocalypse)
            selected.add(ApocalypseDatapackSource.PACK_ID);
        repository.setSelected(selected);
        flansmodultimatePendingApocalypse = withApocalypse;
        tryApplyNewDataPacks(repository, false, ignored -> Optional.ofNullable(minecraft).ifPresent(m -> m.setScreen(createScreen)));
    }

    @Unique
    private void flansmodultimateCreateAnswered()
    {
        flansmodultimateAnswered = true;
        onCreate();
    }
}
