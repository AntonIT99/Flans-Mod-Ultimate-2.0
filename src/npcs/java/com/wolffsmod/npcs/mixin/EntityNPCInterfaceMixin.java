package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.crew.CrewGunners;
import com.wolffsmod.npcs.crew.NpcCrew;
import com.wolffsmod.npcs.crew.NpcCrewHolder;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.drivers.NpcBoardingGoal;
import com.wolffsmod.npcs.drivers.NpcDriver;
import com.wolffsmod.npcs.drivers.NpcDrivers;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelRendering;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.*;
import com.wolffsmod.npcs.tactics.CustomNpcTacticGoal;
import com.wolffsmod.npcs.tactics.NpcTacticGoal;
import com.wolffsmod.npcs.tactics.NpcTacticState;
import com.wolffsmod.npcs.teams.BotObjectiveGoal;
import com.wolffsmod.npcs.teams.BotTag;
import noppes.npcs.Resistances;
import noppes.npcs.ai.EntityAIAttackTarget;
import noppes.npcs.api.item.IItemStack;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import noppes.npcs.entity.data.DataInventory;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * Every hook into the Custom NPCs entity base class.
 *
 * <ul>
 * <li>Equipment owns native combat; Custom NPCs' stored stats remain fallback/editor data, and stand in for each melee statistic
 * and each kind of armor protection the NPC keeps its own value for. An equipped ranged weapon is fired by a goal of its own,
 * which leaves the NPC's movement to a tactic of the mod's own, to Custom NPCs' tactical variant or else to an approach goal,
 * and it selects the ranged variant of Custom NPCs' tactic. The entity carries what its tactic remembers.</li>
 * <li>Recognized, enabled Flan ammunition replaces the ranged attack; every other projectile retains the original path.</li>
 * <li>Model, equipment supply and switch changes are resolved before the server runs NPC combat and movement.</li>
 * <li>Stored defaults and switches piggyback on Custom NPCs' existing server-to-client spawn/update data; old simple-render Flan models migrate to normal rendering.</li>
 * <li>Custom NPCs owns the live entity; detached model entities do not own tracking or culling.</li>
 * <li>Presentation settings control rendering, server-timed idle/movement clips and client engine pitch without changing combat;
 * a playing engine sound replaces the Custom NPCs step and idle sounds.</li>
 * <li>Protection: the NPC takes Flan damage as its model would, can be invulnerable or ignore weak hits, resists Flan damage by its kind and
 * resists knockback while it is a vehicle. Drawn as a driveable it is hit by the part: Flan rounds and blasts meet the model's part boxes,
 * the health a hit takes is handed to the part it struck, and the hit points of the parts are saved with the NPC.</li>
 * <li>Vanilla throwables in the projectile slot fly as the real thing; held Flan guns animate and sound when swung.
 * Optional melee dual wield alternates striking hands on one AI cooldown and sends item break feedback for the attacking hand.</li>
 * <li>Throw wind-ups synchronize a temporary displayed main-hand item and the player's throwing pose without moving inventory.</li>
 * <li>Armor potion effects can be disabled independently of protection. Flan armor invisibility hides the NPC's body on the client, keeping its armor drawn.</li>
 * <li>Vehicle simulation: the NPC carries the moving state of its content-pack model. A vehicle drives where its hull points and
 * slows down while the hull turns; its eyes sit at its gun, riders sit in its driver seat, and the client culls it by its whole model.
 * An aircraft below its attack altitude makes no attack, and an NPC without a hitbox is culled by the width it would have.</li>
 * <li>Senses and movement: line of sight reaches as far as NPCs may fight and can pass transparent blocks, a held target is kept
 * for a margin beyond aggro range, and terrain profiles set land and water speeds.</li>
 * <li>Seats: the NPC sits in a driveable seat as a player does, so it can man the seat gun as a gunner.</li>
 * <li>Crew: a vehicle NPC seats its riders in its model's seats, as many as it has, crews itself when it spawns or comes back, and
 * deals with its crew when destroyed or deleted. A crew member keeps its seat in synchronized data, holds its fire as driver or
 * gunner, breathes aboard and may be shielded, attackers aiming at the vehicle instead. A seat gun a crew member mans fires at the
 * vehicle's target.</li>
 * <li>Driving Flan's driveables: an NPC with the switch on walks to a free vehicle, mecha or aircraft nearby, before any other goal
 * moves it, gets in at the controls and drives or flies it where it would walk. It fights with the driveable's weapons, never with
 * its own.</li>
 * <li>Teams bots: a bot walks to its squad's task while it has nobody to fight, and its team and name tag rule reach clients with its spawn data.</li>
 * <li>Skins only clients know: a client puts the class skin or the model texture the server marked on its copy of the NPC whenever the server sends it.</li>
 * <li>Diagnostics: while a benchmark or the profiler measures, the whole tick of the NPC is timed.</li>
 * </ul>
 *
 * <p>
 * The NeoForge 1.21.1 Custom NPCs dependency uses Mojang names and the builder-based synced-data lifecycle.
 * </p>
 */
@Mixin(value = EntityNPCInterface.class, remap = false)
// Mixin inheritance follows the target entity; Minecraft owns entity identity/equality. Once merged, self-casts and
// instanceof checks on this are valid, and the overrides keep the target signatures.
@SuppressWarnings({"java:S110", "java:S2160", "DataFlowIssue", "NullableProblems", "ConstantValue", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
public abstract class EntityNPCInterfaceMixin extends PathfinderMob
    implements
        IEquipmentPolicy,
        IHeldItemPresentation,
        IFlanNpcDistance,
        IFlanSeatOccupant,
        ILivingVisualEffects,
        IEngineSoundSource,
        IFlanPartTarget,
        NpcVehicleStateHolder,
        NpcThrowPresentation,
        NpcEquipmentRangedGoal.Holder,
        NpcTacticState.Holder,
        NpcCrewHolder,
        NpcMeleeState.Holder,
        NpcDriver.Holder
{
    @Shadow
    private int taskCount;

    @Unique
    private static final String WOLFFSMODNPCS_TYPE_DEFAULTS = "WolffsModTypeDefaults";
    @Unique
    private static final String WOLFFSMODNPCS_DISTANCE_OPT_IN = "FlansDistanceOptIn";
    @Unique
    private static final EntityDataAccessor<Boolean> WOLFFSMODNPCS_DISTANCE_DATA = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<ItemStack> WOLFFSMODNPCS_THROW_DISPLAY = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.ITEM_STACK);
    @Unique
    private static final EntityDataAccessor<Integer> WOLFFSMODNPCS_CREW_SEAT = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.INT);
    @Unique
    private static final String WOLFFSMODNPCS_CREW_SEAT_KEY = "WolffsModCrewSeat";
    @Unique
    private static final String WOLFFSMODNPCS_CREWED_AS_KEY = "WolffsModCrewedAs";

    @Unique
    private NpcEquipmentRangedGoal wolffsmodnpcsItemGoal;
    @Unique
    private NpcTacticGoal wolffsmodnpcsTacticGoal;
    @Unique
    private NpcCombatApproachGoal wolffsmodnpcsApproachGoal;
    @Unique
    private BotObjectiveGoal wolffsmodnpcsObjectiveGoal;
    @Unique
    private NpcBoardingGoal wolffsmodnpcsBoardingGoal;
    @Unique
    private final NpcDriver wolffsmodnpcsDriver = new NpcDriver();
    @Unique
    private final NpcTacticState wolffsmodnpcsTacticState = new NpcTacticState();
    @Unique
    private int wolffsmodnpcsShieldCooldown;
    @Unique
    private int wolffsmodnpcsRound;
    @Unique
    private boolean wolffsmodnpcsLastDistanceManaged;

    @Unique
    private final NpcEngineAudio wolffsmodnpcsEngineAudio = new NpcEngineAudio();
    /** Created on first use: the entity's constructors already ask for its eye height. */
    @Unique
    private NpcVehicleState wolffsmodnpcsVehicle;
    @Unique
    private long wolffsmodnpcsTickStarted;
    /** The id the NPC had when it was last crewed as a vehicle. */
    @Unique
    private UUID wolffsmodnpcsCrewedAs;
    /** The seat guns the crew man, while the NPC is a vehicle. */
    @Unique
    private final CrewGunners wolffsmodnpcsGunners = new CrewGunners();
    @Unique
    private final NpcMeleeState wolffsmodnpcsMelee = new NpcMeleeState();

    protected EntityNPCInterfaceMixin(EntityType<? extends PathfinderMob> type, Level level)
    {
        super(type, level);
    }

    // Presentation

    @Override
    public ItemStack wolffsmodnpcsThrowDisplay()
    {
        return getEntityData().get(WOLFFSMODNPCS_THROW_DISPLAY);
    }

    @Override
    public void wolffsmodnpcsThrowDisplay(ItemStack stack)
    {
        if (!level().isClientSide)
        {
            ItemStack display = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            if (!ItemStack.matches(wolffsmodnpcsThrowDisplay(), display))
                getEntityData().set(WOLFFSMODNPCS_THROW_DISPLAY, display);
        }
    }

    @Override
    public boolean flansmodultimateChargingThrow(InteractionHand hand)
    {
        return hand == InteractionHand.MAIN_HAND && isAlive() && !((EntityNPCInterface) (Object) this).isKilled() && !wolffsmodnpcsThrowDisplay().isEmpty();
    }

    @Inject(method = "getMainHandItem", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsDisplayedThrow(CallbackInfoReturnable<ItemStack> callback)
    {
        // Only the client sees the substitute; server combat, saves and drops keep the original inventory.
        if (level().isClientSide && isAlive() && !((EntityNPCInterface) (Object) this).isKilled() && !wolffsmodnpcsThrowDisplay().isEmpty())
            callback.setReturnValue(wolffsmodnpcsThrowDisplay());
    }

    @Override
    public float flansmodultimateEnginePitch()
    {
        return NpcVehicleSounds.pitch((EntityNPCInterface) (Object) this);
    }

    @Override
    public boolean flansmodultimateHurtFlash()
    {
        return NpcPresentation.of((EntityNPCInterface) (Object) this).isHurtFlash();
    }

    @Override
    public boolean flansmodultimateDeathRotation()
    {
        return NpcPresentation.of((EntityNPCInterface) (Object) this).isDeathRotation();
    }

    @Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsEngineReplacesSteps(BlockPos pos, BlockState state, CallbackInfo callback)
    {
        if (NpcPresentation.of((EntityNPCInterface) (Object) this).getVehicleSounds().resolve((EntityNPCInterface) (Object) this, true).isPresent())
            callback.cancel();
    }

    /** A playing engine sound is the NPC's idle sound; the Custom NPCs idle clip would play over it. Its angry sound stays. */
    @Inject(method = "playAmbientSound", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsEngineReplacesIdle(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (npc.getTarget() == null && NpcPresentation.of(npc).getVehicleSounds().resolve(npc, NpcVehicleSounds.moving(npc)).isPresent())
            callback.cancel();
    }

    // Equipment

    @Override
    public boolean flansArmorEffects()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ITEM_ARMOR);
    }

    @Override
    public boolean flansmodultimateArmorPotionEffects()
    {
        return flansArmorEffects() && NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ARMOR_POTION_EFFECTS);
    }

    @Override
    public boolean flansArmorProtects(DamageSource source)
    {
        return NpcEquipment.armorResists((EntityNPCInterface) (Object) this, NpcDamageCategory.of(source));
    }

    @Override
    public boolean flansWeaponEffects()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ITEM_WEAPONS);
    }

    @Override
    public boolean flansArmorAnimations()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ARMOR_ANIMATIONS);
    }

    @Override
    public boolean flansWeaponAnimations()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.WEAPON_ANIMATIONS);
    }

    @Override
    public boolean flansmodultimateUsePlayerAimRules()
    {
        return true;
    }

    @Override
    public HeldItemSettings flansmodultimateHeldItemSettings(InteractionHand hand)
    {
        return NpcPresentation.of((EntityNPCInterface) (Object) this).hand(hand);
    }

    @Override
    public void flansmodultimateHeldItemSettings(InteractionHand hand, HeldItemSettings settings)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        NpcPresentation.of(npc).hand(hand, settings);
        if (!level().isClientSide)
            npc.updateClient = true;
    }

    @Override
    public boolean flansGunShotAnimations()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.GUN_SHOT_ANIMATION);
    }

    @Override
    public NpcEquipmentRangedGoal wolffsmodnpcsItemGoal()
    {
        return wolffsmodnpcsItemGoal;
    }

    @Override
    public NpcTacticState wolffsmodnpcsTacticState()
    {
        return wolffsmodnpcsTacticState;
    }

    /**
     * The firing goal claims no flag, so its priority does not matter. A tactic of the mod's own comes first, before the
     * tactical movement Custom NPCs registered. The approach goal takes the priority of Custom NPCs' attack goal: that
     * tactical movement then wins the NPC's movement, and melee, which only starts within melee range where the approach
     * ends, takes over from it there. Walking to a driveable to board comes before everything else that moves the NPC.
     */
    @Inject(method = "updateTasks", at = @At("TAIL"))
    private void wolffsmodnpcsItemAI(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (wolffsmodnpcsBoardingGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsBoardingGoal);
        wolffsmodnpcsBoardingGoal = new NpcBoardingGoal(npc);
        goalSelector.addGoal(0, wolffsmodnpcsBoardingGoal);
        if (wolffsmodnpcsItemGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsItemGoal);
        if (wolffsmodnpcsTacticGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsTacticGoal);
        if (wolffsmodnpcsObjectiveGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsObjectiveGoal);
        if (wolffsmodnpcsApproachGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsApproachGoal);
        wolffsmodnpcsItemGoal = new NpcEquipmentRangedGoal(npc);
        goalSelector.addGoal(1, wolffsmodnpcsItemGoal);
        wolffsmodnpcsTacticGoal = new NpcTacticGoal(npc);
        goalSelector.addGoal(1, wolffsmodnpcsTacticGoal);
        // Only a Teams bot uses it: it carries the bot to its squad's task while the bot has nobody to fight.
        wolffsmodnpcsObjectiveGoal = new BotObjectiveGoal(npc);
        goalSelector.addGoal(3, wolffsmodnpcsObjectiveGoal);
        wolffsmodnpcsApproachGoal = null;
        goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof EntityAIAttackTarget).mapToInt(WrappedGoal::getPriority).findFirst().ifPresent(priority ->
        {
            wolffsmodnpcsApproachGoal = new NpcCombatApproachGoal(npc);
            goalSelector.addGoal(priority, wolffsmodnpcsApproachGoal);
        });
    }

    /** Register the restored manoeuvre before normal attacks, retaining sprint/leap priority. */
    @Inject(method = "setResponse", at = @At(value = "FIELD", target = "Lnoppes/npcs/entity/EntityNPCInterface;inventory:Lnoppes/npcs/entity/data/DataInventory;", ordinal = 0))
    private void wolffsmodnpcsRestoreTactics(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // This build registers melee immediately before checking its projectile slot. Move it down one priority so
        // the restored manoeuvre wins movement; subsequent ranged/wander registrations use the incremented count.
        goalSelector.getAvailableGoals().stream().filter(goal -> goal.getGoal() instanceof EntityAIAttackTarget).findFirst().ifPresent(attack ->
        {
            goalSelector.removeGoal(attack.getGoal());
            goalSelector.addGoal(taskCount + 1, attack.getGoal());
        });
        goalSelector.addGoal(taskCount++, new CustomNpcTacticGoal(npc));
    }

    /** An equipped ranged weapon gets the ranged attack and the ranged form of the restored tactical movement. */
    @Redirect(method = "setResponse", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponChoosesResponse(DataInventory inventory)
    {
        return NpcEquipment.combatProjectile((EntityNPCInterface) (Object) this);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void wolffsmodnpcsEquipmentTick(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (level().isClientSide)
            return;
        if (npc.isKilled() || !npc.isAlive() || !NpcEquipment.enabled(npc, Feature.WEAPON_ANIMATIONS))
            NpcThrowPresentation.clear(npc);
        wolffsmodnpcsEngineAudio.tick(npc);
        wolffsmodnpcsUpdateArmorAttributes();
        NpcProtection.updateKnockback(npc);
        if (wolffsmodnpcsShieldCooldown > 0)
            wolffsmodnpcsShieldCooldown--;
        if (!flansWeaponEffects() || npc.getTarget() == null || !npc.getTarget().isAlive() || wolffsmodnpcsShieldCooldown > 0)
        {
            if (isUsingItem() && NpcEquipment.shield(getUseItem()))
                stopUsingItem();
        }
        else if (!isUsingItem())
        {
            if (NpcEquipment.shield(getOffhandItem()))
                startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
            else if (NpcEquipment.shield(getMainHandItem()))
                startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    @Unique
    private void wolffsmodnpcsUpdateArmorAttributes()
    {
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR)
                continue;
            ItemStack armor = getItemBySlot(slot);
            armor.forEachModifier(slot, (attribute, modifier) ->
            {
                var instance = getAttribute(attribute);
                if (instance == null)
                    return;
                if (!wolffsmodnpcsArmorAttribute(attribute))
                    instance.removeModifier(modifier.id());
                else if (!instance.hasModifier(modifier.id()))
                    instance.addTransientModifier(modifier);
            });
        }
    }

    /** Worn armor lends the NPC its attributes, except a knockback resistance the NPC keeps its own value for. */
    @Unique
    private boolean wolffsmodnpcsArmorAttribute(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (attribute.equals(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE))
            return NpcEquipment.inherits(npc, EquipmentProperty.ARMOR_KNOCKBACK);
        return flansArmorEffects();
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsItemMelee(Entity target, CallbackInfoReturnable<Boolean> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // An aircraft below its attack altitude attacks with nothing, whatever asks it to, nor does an NPC at the wheel.
        if (wolffsmodnpcsVehicleState().getFlight().holdsFire() || NpcDrivers.holdsFire(npc))
        {
            callback.setReturnValue(false);
            return;
        }
        // A held Flan gun is swung whether the item or the editor decides the damage.
        NpcItemAttacks.meleeFeedback(npc);
        if (NpcEquipment.itemMelee(npc))
            callback.setReturnValue(NpcItemAttacks.melee(npc, target));
    }

    @Override
    public NpcMeleeState wolffsmodnpcsMeleeState()
    {
        return wolffsmodnpcsMelee;
    }

    @Override
    public NpcDriver wolffsmodnpcsDriver()
    {
        return wolffsmodnpcsDriver;
    }

    @Override
    public void onEquippedItemBroken(net.minecraft.world.item.Item item, EquipmentSlot slot)
    {
        super.onEquippedItemBroken(item, wolffsmodnpcsMelee.breakSlot(slot));
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> callback)
    {
        if (NpcEquipment.armorResists((EntityNPCInterface) (Object) this, NpcDamageCategory.of(source)))
            callback.setReturnValue(super.getDamageAfterArmorAbsorb(source, amount));
    }

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorFireProtection(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_FIRE, EquippedArmorProperties::fireResistance))
            callback.setReturnValue(true);
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorAir(int air, CallbackInfoReturnable<Integer> callback)
    {
        // Crew breathe inside their vehicle, a diving submarine included.
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_BREATHING, EquippedArmorProperties::waterBreathing) || NpcCrew.vehicleOf(this).isPresent())
            callback.setReturnValue(air);
    }

    /**
     * Flan armor that makes its wearer invisible hides the NPC's body and leaves the armor drawn, as the effect does for a
     * player. Custom NPCs ignores that effect, so the armor is read directly; only on the client, which leaves what the
     * server makes of the NPC unchanged.
     */
    @Inject(method = "isInvisible", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorHidesBody(CallbackInfoReturnable<Boolean> callback)
    {
        if (level().isClientSide && !callback.getReturnValueZ() && NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::invisibility))
            callback.setReturnValue(true);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorFall(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_FALL, EquippedArmorProperties::negatesFallDamage))
            callback.setReturnValue(false);
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnoppes/npcs/Resistances;applyResistance(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float wolffsmodnpcsArmorNotEditor(Resistances resistances, DamageSource source, float amount)
    {
        return NpcEquipment.armorResists((EntityNPCInterface) (Object) this, NpcDamageCategory.of(source))
            ? amount
            : NpcProtection.resist((EntityNPCInterface) (Object) this, resistances, source, amount);
    }

    @Redirect(method = "knockback", at = @At(value = "FIELD", target = "Lnoppes/npcs/Resistances;knockback:F", opcode = Opcodes.GETFIELD))
    private float wolffsmodnpcsNativeKnockback(Resistances resistances)
    {
        return NpcEquipment.inherits((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_KNOCKBACK) ? 1F : resistances.knockback;
    }

    // LivingEntity's default equipment wear hooks are empty for ordinary mobs.
    @Override
    protected void hurtArmor(DamageSource source, float amount)
    {
        if (!flansArmorEffects() || amount <= 0F)
            return;
        int wear = Math.max(1, (int) (amount / 4F));
        for (EquipmentSlot slot : EquipmentSlot.values())
        {
            if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
            {
                ItemStack stack = getItemBySlot(slot);
                if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && stack.canBeHurtBy(source))
                    stack.hurtAndBreak(wear, this, slot);
            }
        }
    }

    @Override
    protected void hurtCurrentlyUsedShield(float amount)
    {
        if (amount >= 3F && flansWeaponEffects() && NpcEquipment.shield(getUseItem()))
        {
            getUseItem().hurtAndBreak(1 + (int) amount, this, getSlotForHand(getUsedItemHand()));
            if (getUseItem().isEmpty())
                stopUsingItem();
        }
    }

    @Override
    protected void blockUsingShield(LivingEntity attacker)
    {
        super.blockUsingShield(attacker);
        if (attacker.getMainHandItem().canDisableShield(getUseItem(), this, attacker))
        {
            wolffsmodnpcsShieldCooldown = 100;
            stopUsingItem();
            level().broadcastEntityEvent(this, (byte) 30);
        }
    }

    // Ranged attack

    @Inject(method = "performRangedAttack", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsFireFlanRound(LivingEntity target, float distanceFactor, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // The fallback ranged AI waits by itself; this stops scripts and anything else that pulls the trigger.
        // A gun whose turret has been shot off stays silent, and an NPC at the wheel fights with the driveable's weapons.
        if (wolffsmodnpcsVehicleState().getFlight().holdsFire() || wolffsmodnpcsVehicleState().getParts().effects().gunDisabled() || NpcDrivers.holdsFire(npc))
        {
            callback.cancel();
            return;
        }
        if (NpcEquipment.weaponAuthority(npc))
        {
            // Scripted ranged triggers use the same item action, never the editor projectile template.
            if (NpcEquipment.ranged(npc) && NpcItemAttacks.ranged(npc, target))
                NpcItemAttacks.launched(npc, target, NpcEquipment.offHandPrimary(npc) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
            callback.cancel();
            return;
        }
        if (NpcRangedAttack.fire(npc, target, distanceFactor == 1F, wolffsmodnpcsRound))
        {
            wolffsmodnpcsRound = wolffsmodnpcsRound >= Integer.MAX_VALUE / 10 - 1 ? 0 : wolffsmodnpcsRound + 1;
            callback.cancel();
            return;
        }
        if (NpcThrowables.fire(npc, target, distanceFactor == 1F))
            callback.cancel();
    }

    // Protection

    @Override
    public Optional<IContentType> flansmodultimateDamageModel()
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (!NpcEquipment.enabled(npc, Feature.VEHICLE_DAMAGE) || !(npc instanceof EntityCustomNpc custom) || !(custom.modelData.getEntity(npc) instanceof FlanModelEntity model))
            return Optional.empty();
        return Optional.ofNullable(model.getInfoType());
    }

    @Override
    public boolean flansmodultimateModelArmour()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.MODEL_ARMOUR);
    }

    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsIgnoreHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback)
    {
        if (!level().isClientSide && (NpcProtection.ignores((EntityNPCInterface) (Object) this, source, amount) || NpcCrew.shielded(this, source)))
            callback.setReturnValue(false);
    }

    @Override
    public Optional<IPartBody> flansmodultimatePartBody()
    {
        return wolffsmodnpcsVehicleState().getParts().body();
    }

    @Override
    public boolean flansmodultimateHurtPart(String part, DamageSource source, float amount)
    {
        return wolffsmodnpcsVehicleState().getParts().hurtPart((EntityNPCInterface) (Object) this, part, source, amount);
    }

    @Inject(method = "actuallyHurt", at = @At("HEAD"))
    private void wolffsmodnpcsBeforePartDamage(DamageSource source, float amount, CallbackInfo callback)
    {
        wolffsmodnpcsVehicleState().getParts().beforeDamage((EntityNPCInterface) (Object) this);
    }

    // The health lost is handed to the part that was struck before the NPC is checked for death.
    @Inject(method = "actuallyHurt", at = @At("RETURN"))
    private void wolffsmodnpcsAfterPartDamage(DamageSource source, float amount, CallbackInfo callback)
    {
        wolffsmodnpcsVehicleState().getParts().afterDamage((EntityNPCInterface) (Object) this, source);
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsSaveParts(CompoundTag tag, CallbackInfo callback)
    {
        CompoundTag parts = wolffsmodnpcsVehicleState().getParts().save();
        if (parts != null)
            tag.put(NpcParts.NBT_KEY, parts);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsReadParts(CompoundTag tag, CallbackInfo callback)
    {
        if (tag.contains(NpcParts.NBT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND))
            wolffsmodnpcsVehicleState().getParts().load(tag.getCompound(NpcParts.NBT_KEY));
    }

    // Type properties

    @Inject(method = "tick", at = @At("HEAD"))
    private void wolffsmodnpcsInheritType(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // Zero doubles as "not measuring": the clock never reads it in practice.
        wolffsmodnpcsTickStarted = NpcProfiler.server() && !npc.level().isClientSide ? System.nanoTime() : 0L;
        if (!npc.level().isClientSide)
        {
            NpcAmmunition.refillEmptySlots(npc);
            NpcTypeProperties.of(npc).refresh(npc);
        }
    }

    @Inject(method = "writeSpawnData()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void wolffsmodnpcsSyncTypeDefaults(CallbackInfoReturnable<CompoundTag> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        callback.getReturnValue().put(WOLFFSMODNPCS_TYPE_DEFAULTS, NpcTypeProperties.of(npc).syncDefaults(npc));
        BotTag.writeClient(npc, callback.getReturnValue());
    }

    @ModifyVariable(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("HEAD"), argsOnly = true)
    private CompoundTag wolffsmodnpcsMigrateSpawnRenderer(CompoundTag tag)
    {
        return FlanModelRendering.migrateNpc(tag, "ModelData");
    }

    @Inject(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void wolffsmodnpcsReadTypeDefaults(CompoundTag tag, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (!npc.level().isClientSide)
            return;
        if (tag.contains(WOLFFSMODNPCS_TYPE_DEFAULTS))
            NpcTypeProperties.of(npc).readSyncedDefaults(npc, tag.getCompound(WOLFFSMODNPCS_TYPE_DEFAULTS));
        BotTag.readClient(npc, tag);
        NpcSkins.resolveOnClient(npc);
    }

    // Vehicle simulation

    @Override
    public NpcVehicleState wolffsmodnpcsVehicleState()
    {
        if (wolffsmodnpcsVehicle == null)
            wolffsmodnpcsVehicle = new NpcVehicleState();
        return wolffsmodnpcsVehicle;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void wolffsmodnpcsVehicleTick(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        NpcVehicleState state = wolffsmodnpcsVehicleState();
        boolean server = !level().isClientSide;
        if (server)
        {
            // Before the model state, which reads the heading these leave the NPC with.
            state.getFlight().tick(npc);
            state.getSubmarine().tick(npc);
        }
        state.tick(npc);
        if (server)
        {
            NpcMobility.tick(npc);
            NpcPathDebug.tick(npc, state);
            NpcCrew.tick(npc);
            wolffsmodnpcsGunners.tick(npc);
            NpcDrivers.tick(npc);
        }
        if (wolffsmodnpcsTickStarted != 0L)
            NpcProfiler.npcTick(System.nanoTime() - wolffsmodnpcsTickStarted);
    }

    @ModifyVariable(method = "travel", at = @At("HEAD"), argsOnly = true)
    private Vec3 wolffsmodnpcsDrive(Vec3 travel)
    {
        if (level().isClientSide)
            return travel;
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        Vec3 driven = wolffsmodnpcsVehicleState().beforeTravel(npc, travel);
        // Broken tracks, wheels and engines slow the NPC down or stop it.
        float factor = NpcMobility.landFactor(npc) * wolffsmodnpcsVehicleState().getParts().effects().mobility();
        return factor == 1F ? driven : new Vec3(driven.x * factor, driven.y, driven.z * factor);
    }

    @Inject(method = "getWaterSlowDown", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsWaterSpeed(CallbackInfoReturnable<Float> callback)
    {
        callback.setReturnValue(NpcMobility.waterSlowDown((EntityNPCInterface) (Object) this, callback.getReturnValueF()));
    }

    /** Minecraft 1.21 takes the eye height from the entity's dimensions, which Custom NPCs builds in getDimensions. */
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsEyeHeight(Pose pose, CallbackInfoReturnable<EntityDimensions> callback)
    {
        EntityDimensions dimensions = callback.getReturnValue();
        wolffsmodnpcsVehicleState().eyeHeight((EntityNPCInterface) (Object) this, dimensions.height()).ifPresent(height -> callback.setReturnValue(dimensions.withEyeHeight(height)));
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move)
    {
        if (!hasPassenger(passenger) || !NpcCrew.positionRider((EntityNPCInterface) (Object) this, passenger, move))
            super.positionRider(passenger, move);
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        return wolffsmodnpcsVehicleState().cullingBox(npc).or(() -> NpcModels.unhitCullingBox(npc)).orElseGet(super::getBoundingBoxForCulling);
    }

    // Crew

    @Override
    protected boolean canAddPassenger(Entity passenger)
    {
        return NpcCrew.canCarry((EntityNPCInterface) (Object) this).orElseGet(() -> super.canAddPassenger(passenger));
    }

    /** Crew holding their fire take no target, and attackers aiming at shielded crew aim at their vehicle. */
    @ModifyVariable(method = "setTarget", at = @At("HEAD"), argsOnly = true)
    private LivingEntity wolffsmodnpcsCrewTarget(LivingEntity target)
    {
        return NpcCrew.target((EntityNPCInterface) (Object) this, target);
    }

    @Inject(method = "die", at = @At("HEAD"))
    private void wolffsmodnpcsCrewOnDeath(DamageSource source, CallbackInfo callback)
    {
        if (!level().isClientSide)
            NpcCrew.destroyed((EntityNPCInterface) (Object) this);
    }

    @Inject(method = "delete", at = @At("HEAD"))
    private void wolffsmodnpcsCrewOnDelete(CallbackInfo callback)
    {
        NpcCrew.deleted((EntityNPCInterface) (Object) this);
    }

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void wolffsmodnpcsDefineCrewSeat(SynchedEntityData.Builder builder, CallbackInfo callback)
    {
        builder.define(WOLFFSMODNPCS_CREW_SEAT, NpcCrew.NO_SEAT);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsReadCrew(CompoundTag tag, CallbackInfo callback)
    {
        getEntityData().set(WOLFFSMODNPCS_CREW_SEAT, tag.contains(WOLFFSMODNPCS_CREW_SEAT_KEY) ? tag.getInt(WOLFFSMODNPCS_CREW_SEAT_KEY) : NpcCrew.NO_SEAT);
        wolffsmodnpcsCrewedAs = tag.hasUUID(WOLFFSMODNPCS_CREWED_AS_KEY) ? tag.getUUID(WOLFFSMODNPCS_CREWED_AS_KEY) : null;
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsSaveCrew(CompoundTag tag, CallbackInfo callback)
    {
        if (wolffsmodnpcsCrewSeat() >= 0)
            tag.putInt(WOLFFSMODNPCS_CREW_SEAT_KEY, wolffsmodnpcsCrewSeat());
        if (wolffsmodnpcsCrewedAs != null)
            tag.putUUID(WOLFFSMODNPCS_CREWED_AS_KEY, wolffsmodnpcsCrewedAs);
    }

    @Override
    public int wolffsmodnpcsCrewSeat()
    {
        return getEntityData().get(WOLFFSMODNPCS_CREW_SEAT);
    }

    @Override
    public void wolffsmodnpcsCrewSeat(int seat)
    {
        getEntityData().set(WOLFFSMODNPCS_CREW_SEAT, Math.max(NpcCrew.NO_SEAT, seat));
    }

    @Override
    public UUID wolffsmodnpcsCrewedAs()
    {
        return wolffsmodnpcsCrewedAs;
    }

    @Override
    public void wolffsmodnpcsCrewedAs(UUID id)
    {
        wolffsmodnpcsCrewedAs = id;
    }

    // Senses

    @Override
    public boolean hasLineOfSight(Entity entity)
    {
        return NpcSight.sees((EntityNPCInterface) (Object) this, entity);
    }

    @ModifyVariable(method = "isInRange(Lnet/minecraft/world/entity/Entity;D)Z", at = @At("HEAD"), argsOnly = true)
    private double wolffsmodnpcsKeepTarget(double range, Entity entity)
    {
        return entity != null && entity == getTarget() ? range + NpcTargetSearch.margin(this) : range;
    }

    // Tracking and render distance

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void wolffsmodnpcsDefineDistance(SynchedEntityData.Builder builder, CallbackInfo callback)
    {
        builder.define(WOLFFSMODNPCS_DISTANCE_DATA, false);
        builder.define(WOLFFSMODNPCS_THROW_DISPLAY, ItemStack.EMPTY);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsReadDistance(CompoundTag tag, CallbackInfo callback)
    {
        getEntityData().set(WOLFFSMODNPCS_DISTANCE_DATA, tag.getBoolean(WOLFFSMODNPCS_DISTANCE_OPT_IN));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void wolffsmodnpcsSaveDistance(CompoundTag tag, CallbackInfo callback)
    {
        tag.putBoolean(WOLFFSMODNPCS_DISTANCE_OPT_IN, isFlanNpcDistanceOptIn());
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void wolffsmodnpcsChangedModel(CallbackInfo callback)
    {
        Entity npc = this;
        if (npc.level() instanceof ServerLevel level)
        {
            boolean managed = usesFlanNpcDistanceSettings();
            if (managed != wolffsmodnpcsLastDistanceManaged)
            {
                wolffsmodnpcsLastDistanceManaged = managed;
                FlansModApi.refreshEntityTracking(level);
            }
        }
    }

    // The (Object) cast is required: the mixin class itself is unrelated to EntityCustomNpc until merged.
    @SuppressWarnings("java:S1905")
    @Override
    public boolean usesFlanNpcDistanceSettings()
    {
        return isFlanNpcDistanceOptIn() || (Object) this instanceof EntityCustomNpc npc && npc.modelData.getEntity(npc) instanceof FlanModelEntity;
    }

    @Override
    public boolean isFlanNpcDistanceOptIn()
    {
        return getEntityData().get(WOLFFSMODNPCS_DISTANCE_DATA);
    }

    @Override
    public void setFlanNpcDistanceOptIn(boolean enabled)
    {
        Entity npc = this;
        if (!(npc.level() instanceof ServerLevel level) || !level.getServer().isSameThread())
            return;
        npc.getEntityData().set(WOLFFSMODNPCS_DISTANCE_DATA, enabled);
        FlansModApi.refreshEntityTracking(level);
    }
}
