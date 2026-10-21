package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.*;
import com.wolffsmod.npcs.combat.*;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.diag.NpcProfiler;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.model.FlanModelRendering;
import com.wolffsmod.npcs.model.NpcSkins;
import com.wolffsmod.npcs.properties.*;
import com.wolffsmod.npcs.sim.*;
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
 * <li>Vanilla throwables in the projectile slot fly as the real thing; held Flan guns animate and sound when swung.</li>
 * <li>Throw wind-ups synchronize a temporary displayed main-hand item and the player's throwing pose without moving inventory.</li>
 * <li>Flan armor that makes its wearer invisible makes the NPC invisible to the client: its body is not drawn, its armor is.</li>
 * <li>Vehicle simulation: the NPC carries the moving state of its content-pack model. A vehicle drives where its hull points and
 * slows down while the hull turns; its eyes sit at its gun, riders sit in its driver seat, and the client culls it by its whole model.
 * An aircraft below its attack altitude makes no attack, and an NPC without a hitbox is culled by the width it would have.</li>
 * <li>Senses and movement: line of sight reaches as far as NPCs may fight and can pass transparent blocks, a held target is kept
 * for a margin beyond aggro range, and terrain profiles set land and water speeds.</li>
 * <li>Seats: the NPC sits in a driveable seat as a player does, so it can man the seat gun as a gunner.</li>
 * <li>Teams bots: a bot walks to its squad's task while it has nobody to fight, and its team and name tag rule reach clients with its spawn data.</li>
 * <li>Skins only clients know: a client puts the class skin or the model texture the server marked on its copy of the NPC whenever the server sends it.</li>
 * <li>Diagnostics: while a benchmark or the profiler measures, the whole tick of the NPC is timed.</li>
 * </ul>
 *
 * <p>
 * Custom NPCs uses SRG names in production and Mojang names in the development dependency, hence the paired method names.
 * </p>
 */
@Mixin(value = EntityNPCInterface.class, remap = false)
// Mixin inheritance follows the target entity; Minecraft owns entity identity/equality. Once merged, self-casts and
// instanceof checks on this are valid, and the overrides keep the target signatures.
@SuppressWarnings({"java:S110", "java:S2160", "DataFlowIssue", "NullableProblems", "ConstantValue", "AddedMixinMembersNamePattern", "UnresolvedMixinReference"})
public abstract class EntityNPCInterfaceMixin extends PathfinderMob
    implements
        IEquipmentPolicy,
        IFlanNpcDistance,
        IFlanSeatOccupant,
        ILivingVisualEffects,
        IEngineSoundSource,
        IFlanPartTarget,
        NpcVehicleStateHolder,
        NpcThrowPresentation,
        NpcEquipmentRangedGoal.Holder,
        NpcTacticState.Holder
{
    @Unique
    private static final String WOLFFSMODNPCS_TYPE_DEFAULTS = "WolffsModTypeDefaults";
    @Unique
    private static final String WOLFFSMODNPCS_DISTANCE_OPT_IN = "FlansDistanceOptIn";
    @Unique
    private static final EntityDataAccessor<Boolean> WOLFFSMODNPCS_DISTANCE_DATA = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);
    @Unique
    private static final EntityDataAccessor<ItemStack> WOLFFSMODNPCS_THROW_DISPLAY = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.ITEM_STACK);

    @Unique
    private NpcEquipmentRangedGoal wolffsmodnpcsItemGoal;
    @Unique
    private NpcTacticGoal wolffsmodnpcsTacticGoal;
    @Unique
    private NpcCombatApproachGoal wolffsmodnpcsApproachGoal;
    @Unique
    private BotObjectiveGoal wolffsmodnpcsObjectiveGoal;
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

    @Inject(method = {"getMainHandItem", "m_21205_"}, at = @At("HEAD"), cancellable = true)
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

    @Inject(method = {"playStepSound", "m_7355_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsEngineReplacesSteps(BlockPos pos, BlockState state, CallbackInfo callback)
    {
        if (NpcPresentation.of((EntityNPCInterface) (Object) this).getVehicleSounds().resolve((EntityNPCInterface) (Object) this, true).isPresent())
            callback.cancel();
    }

    /** A playing engine sound is the NPC's idle sound; the Custom NPCs idle clip would play over it. Its angry sound stays. */
    @Inject(method = {"playAmbientSound", "m_8032_"}, at = @At("HEAD"), cancellable = true)
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
     * ends, takes over from it there.
     */
    @Inject(method = "updateTasks", at = @At("TAIL"))
    private void wolffsmodnpcsItemAI(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
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

    /** An equipped ranged weapon gets the ranged attack and the ranged variant of the NPC's tactic. */
    @Redirect(method = "setResponse", at = @At(value = "INVOKE", target = "Lnoppes/npcs/entity/data/DataInventory;getProjectile()Lnoppes/npcs/api/item/IItemStack;"))
    private IItemStack wolffsmodnpcsWeaponChoosesResponse(DataInventory inventory)
    {
        return NpcEquipment.combatProjectile((EntityNPCInterface) (Object) this);
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
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
            if (slot.getType() != EquipmentSlot.Type.ARMOR)
                continue;
            ItemStack armor = getItemBySlot(slot);
            armor.getAttributeModifiers(slot).forEach((attribute, modifier) ->
            {
                var instance = getAttribute(attribute);
                if (instance == null)
                    return;
                if (!wolffsmodnpcsArmorAttribute(attribute))
                    instance.removeModifier(modifier.getId());
                else if (!instance.hasModifier(modifier))
                    instance.addTransientModifier(modifier);
            });
        }
    }

    /** Worn armor lends the NPC its attributes, except a knockback resistance the NPC keeps its own value for. */
    @Unique
    private boolean wolffsmodnpcsArmorAttribute(net.minecraft.world.entity.ai.attributes.Attribute attribute)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (attribute == net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE)
            return NpcEquipment.inherits(npc, EquipmentProperty.ARMOR_KNOCKBACK);
        return flansArmorEffects();
    }

    @Inject(method = {"doHurtTarget", "m_7327_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsItemMelee(Entity target, CallbackInfoReturnable<Boolean> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // An aircraft below its attack altitude attacks with nothing, whatever asks it to.
        if (wolffsmodnpcsVehicleState().getFlight().holdsFire())
        {
            callback.setReturnValue(false);
            return;
        }
        // A held Flan gun is swung whether the item or the editor decides the damage.
        NpcItemAttacks.meleeFeedback(npc);
        if (NpcEquipment.itemMelee(npc))
            callback.setReturnValue(NpcItemAttacks.melee(npc, target));
    }

    @Inject(method = {"getDamageAfterArmorAbsorb", "m_21161_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> callback)
    {
        if (NpcEquipment.armorResists((EntityNPCInterface) (Object) this, NpcDamageCategory.of(source)))
            callback.setReturnValue(super.getDamageAfterArmorAbsorb(source, amount));
    }

    @Inject(method = {"fireImmune", "m_5825_"}, at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorFireProtection(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_FIRE, EquippedArmorProperties::fireResistance))
            callback.setReturnValue(true);
    }

    @Inject(method = {"decreaseAirSupply", "m_7302_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorAir(int air, CallbackInfoReturnable<Integer> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_BREATHING, EquippedArmorProperties::waterBreathing))
            callback.setReturnValue(air);
    }

    /**
     * Flan armor that makes its wearer invisible hides the NPC's body and leaves the armor drawn, as the effect does for a
     * player. Custom NPCs ignores that effect, so the armor is read directly; only on the client, which leaves what the
     * server makes of the NPC unchanged.
     */
    @Inject(method = {"isInvisible", "m_20145_"}, at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorHidesBody(CallbackInfoReturnable<Boolean> callback)
    {
        if (level().isClientSide && !callback.getReturnValueZ() && NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::invisibility))
            callback.setReturnValue(true);
    }

    @Inject(method = {"causeFallDamage", "m_142535_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorFall(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquipmentProperty.ARMOR_FALL, EquippedArmorProperties::negatesFallDamage))
            callback.setReturnValue(false);
    }

    @Redirect(method = {"hurt", "m_6469_"}, at = @At(value = "INVOKE", target = "Lnoppes/npcs/Resistances;applyResistance(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float wolffsmodnpcsArmorNotEditor(Resistances resistances, DamageSource source, float amount)
    {
        return NpcEquipment.armorResists((EntityNPCInterface) (Object) this, NpcDamageCategory.of(source))
            ? amount
            : NpcProtection.resist((EntityNPCInterface) (Object) this, resistances, source, amount);
    }

    @Redirect(method = {"knockback", "m_147240_"}, at = @At(value = "FIELD", target = "Lnoppes/npcs/Resistances;knockback:F", opcode = Opcodes.GETFIELD))
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
            if (slot.getType() == EquipmentSlot.Type.ARMOR)
            {
                ItemStack stack = getItemBySlot(slot);
                if (stack.getItem() instanceof net.minecraft.world.item.ArmorItem && (!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) || !stack.getItem().isFireResistant()))
                    stack.hurtAndBreak(wear, this, holder -> holder.broadcastBreakEvent(slot));
            }
        }
    }

    @Override
    protected void hurtCurrentlyUsedShield(float amount)
    {
        if (amount >= 3F && flansWeaponEffects() && NpcEquipment.shield(getUseItem()))
        {
            getUseItem().hurtAndBreak(1 + (int) amount, this, holder -> holder.broadcastBreakEvent(getUsedItemHand()));
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

    @Inject(method = {"performRangedAttack", "m_6504_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsFireFlanRound(LivingEntity target, float distanceFactor, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        // The fallback ranged AI waits by itself; this stops scripts and anything else that pulls the trigger.
        // A gun whose turret has been shot off stays silent.
        if (wolffsmodnpcsVehicleState().getFlight().holdsFire() || wolffsmodnpcsVehicleState().getParts().effects().gunDisabled())
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

    @Inject(method = {"hurt", "m_6469_"}, at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsIgnoreHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> callback)
    {
        if (!level().isClientSide && NpcProtection.ignores((EntityNPCInterface) (Object) this, source, amount))
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

    @Inject(method = {"actuallyHurt", "m_6475_"}, at = @At("HEAD"))
    private void wolffsmodnpcsBeforePartDamage(DamageSource source, float amount, CallbackInfo callback)
    {
        wolffsmodnpcsVehicleState().getParts().beforeDamage((EntityNPCInterface) (Object) this);
    }

    // The health lost is handed to the part that was struck before the NPC is checked for death.
    @Inject(method = {"actuallyHurt", "m_6475_"}, at = @At("RETURN"))
    private void wolffsmodnpcsAfterPartDamage(DamageSource source, float amount, CallbackInfo callback)
    {
        wolffsmodnpcsVehicleState().getParts().afterDamage((EntityNPCInterface) (Object) this, source);
    }

    @Inject(method = {"addAdditionalSaveData", "m_7380_"}, at = @At("TAIL"))
    private void wolffsmodnpcsSaveParts(CompoundTag tag, CallbackInfo callback)
    {
        CompoundTag parts = wolffsmodnpcsVehicleState().getParts().save();
        if (parts != null)
            tag.put(NpcParts.NBT_KEY, parts);
    }

    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("TAIL"))
    private void wolffsmodnpcsReadParts(CompoundTag tag, CallbackInfo callback)
    {
        if (tag.contains(NpcParts.NBT_KEY, net.minecraft.nbt.Tag.TAG_COMPOUND))
            wolffsmodnpcsVehicleState().getParts().load(tag.getCompound(NpcParts.NBT_KEY));
    }

    // Type properties

    @Inject(method = {"tick", "m_8119_"}, at = @At("HEAD"))
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

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
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
        }
        if (wolffsmodnpcsTickStarted != 0L)
            NpcProfiler.npcTick(System.nanoTime() - wolffsmodnpcsTickStarted);
    }

    @ModifyVariable(method = {"travel", "m_7023_"}, at = @At("HEAD"), argsOnly = true)
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

    @Inject(method = {"getWaterSlowDown", "m_6108_"}, at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsWaterSpeed(CallbackInfoReturnable<Float> callback)
    {
        callback.setReturnValue(NpcMobility.waterSlowDown((EntityNPCInterface) (Object) this, callback.getReturnValueF()));
    }

    @Override
    protected float getStandingEyeHeight(Pose pose, EntityDimensions dimensions)
    {
        float height = super.getStandingEyeHeight(pose, dimensions);
        return wolffsmodnpcsVehicleState().eyeHeight((EntityNPCInterface) (Object) this, dimensions.height).orElse(height);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move)
    {
        Optional<Vec3> seat = hasPassenger(passenger) ? wolffsmodnpcsVehicleState().riderOffset((EntityNPCInterface) (Object) this) : Optional.empty();
        if (seat.isPresent())
            move.accept(passenger, getX() + seat.get().x, getY() + seat.get().y + passenger.getMyRidingOffset(), getZ() + seat.get().z);
        else
            super.positionRider(passenger, move);
    }

    @Override
    public AABB getBoundingBoxForCulling()
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        return wolffsmodnpcsVehicleState().cullingBox(npc).or(() -> NpcModels.unhitCullingBox(npc)).orElseGet(super::getBoundingBoxForCulling);
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

    @Inject(method = {"defineSynchedData", "m_8097_"}, at = @At("TAIL"))
    private void wolffsmodnpcsDefineDistance(CallbackInfo callback)
    {
        getEntityData().define(WOLFFSMODNPCS_DISTANCE_DATA, false);
        getEntityData().define(WOLFFSMODNPCS_THROW_DISPLAY, ItemStack.EMPTY);
    }

    @Inject(method = {"readAdditionalSaveData", "m_7378_"}, at = @At("TAIL"))
    private void wolffsmodnpcsReadDistance(CompoundTag tag, CallbackInfo callback)
    {
        getEntityData().set(WOLFFSMODNPCS_DISTANCE_DATA, tag.getBoolean(WOLFFSMODNPCS_DISTANCE_OPT_IN));
    }

    @Inject(method = {"addAdditionalSaveData", "m_7380_"}, at = @At("TAIL"))
    private void wolffsmodnpcsSaveDistance(CompoundTag tag, CallbackInfo callback)
    {
        tag.putBoolean(WOLFFSMODNPCS_DISTANCE_OPT_IN, isFlanNpcDistanceOptIn());
    }

    @Inject(method = {"tick", "m_8119_"}, at = @At("TAIL"))
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
