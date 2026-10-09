package com.wolffsmod.npcs.mixin;

import com.flansmodultimate.api.EquippedArmorProperties;
import com.flansmodultimate.api.FlansModApi;
import com.flansmodultimate.api.IEquipmentPolicy;
import com.flansmodultimate.api.IFlanNpcDistance;
import com.wolffsmod.npcs.combat.NpcEquipment;
import com.wolffsmod.npcs.combat.NpcEquipmentRangedGoal;
import com.wolffsmod.npcs.combat.NpcItemAttacks;
import com.wolffsmod.npcs.combat.NpcRangedAttack;
import com.wolffsmod.npcs.combat.NpcWeaponOptions.Feature;
import com.wolffsmod.npcs.model.FlanModelEntity;
import com.wolffsmod.npcs.properties.NpcTypeProperties;
import noppes.npcs.Resistances;
import noppes.npcs.entity.EntityCustomNpc;
import noppes.npcs.entity.EntityNPCInterface;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Every hook into the Custom NPCs entity base class.
 *
 * <ul>
 * <li>Equipment owns native combat; Custom NPCs' stored stats remain fallback/editor data.</li>
 * <li>Recognized, enabled Flan ammunition replaces the ranged attack; every other projectile retains the original path.</li>
 * <li>Model and switch changes are resolved before the server runs NPC combat and movement.</li>
 * <li>Stored defaults and switches piggyback on Custom NPCs' existing server-to-client spawn/update data.</li>
 * <li>Custom NPCs owns the live entity; detached model entities do not own tracking or culling.</li>
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
public abstract class EntityNPCInterfaceMixin extends PathfinderMob implements IEquipmentPolicy, IFlanNpcDistance
{
    @Unique
    private static final String WOLFFSMODNPCS_TYPE_DEFAULTS = "WolffsModTypeDefaults";
    @Unique
    private static final String WOLFFSMODNPCS_DISTANCE_OPT_IN = "FlansDistanceOptIn";
    @Unique
    private static final EntityDataAccessor<Boolean> WOLFFSMODNPCS_DISTANCE_DATA = SynchedEntityData.defineId(EntityNPCInterface.class, EntityDataSerializers.BOOLEAN);

    @Unique
    private NpcEquipmentRangedGoal wolffsmodnpcsItemGoal;
    @Unique
    private int wolffsmodnpcsShieldCooldown;
    @Unique
    private int wolffsmodnpcsRound;
    @Unique
    private boolean wolffsmodnpcsLastDistanceManaged;

    protected EntityNPCInterfaceMixin(EntityType<? extends PathfinderMob> type, Level level)
    {
        super(type, level);
    }

    // Equipment

    @Override
    public boolean flansArmorEffects()
    {
        return NpcEquipment.enabled((EntityNPCInterface) (Object) this, Feature.ITEM_ARMOR);
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

    @Inject(method = "updateTasks", at = @At("TAIL"))
    private void wolffsmodnpcsItemAI(CallbackInfo callback)
    {
        if (wolffsmodnpcsItemGoal != null)
            goalSelector.removeGoal(wolffsmodnpcsItemGoal);
        wolffsmodnpcsItemGoal = new NpcEquipmentRangedGoal((EntityNPCInterface) (Object) this);
        goalSelector.addGoal(1, wolffsmodnpcsItemGoal);
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void wolffsmodnpcsEquipmentTick(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (level().isClientSide)
            return;
        wolffsmodnpcsUpdateArmorAttributes();
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
                if (!flansArmorEffects())
                    instance.removeModifier(modifier.id());
                else if (!instance.hasModifier(modifier.id()))
                    instance.addTransientModifier(modifier);
            });
        }
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsItemMelee(Entity target, CallbackInfoReturnable<Boolean> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (NpcEquipment.weaponAuthority(npc))
            callback.setReturnValue(NpcItemAttacks.melee(npc, target));
    }

    @Inject(method = "getDamageAfterArmorAbsorb", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsNativeArmor(DamageSource source, float amount, CallbackInfoReturnable<Float> callback)
    {
        if (flansArmorEffects())
            callback.setReturnValue(super.getDamageAfterArmorAbsorb(source, amount));
    }

    @Inject(method = "fireImmune", at = @At("RETURN"), cancellable = true)
    private void wolffsmodnpcsArmorFireProtection(CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::fireResistance))
            callback.setReturnValue(true);
    }

    @Inject(method = "decreaseAirSupply", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorAir(int air, CallbackInfoReturnable<Integer> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::waterBreathing))
            callback.setReturnValue(air);
    }

    @Inject(method = "causeFallDamage", at = @At("HEAD"), cancellable = true)
    private void wolffsmodnpcsArmorFall(float distance, float multiplier, DamageSource source, CallbackInfoReturnable<Boolean> callback)
    {
        if (NpcEquipment.armorProtects((EntityNPCInterface) (Object) this, EquippedArmorProperties::negatesFallDamage))
            callback.setReturnValue(false);
    }

    @Redirect(method = "hurt", at = @At(value = "INVOKE", target = "Lnoppes/npcs/Resistances;applyResistance(Lnet/minecraft/world/damagesource/DamageSource;F)F"))
    private float wolffsmodnpcsArmorNotEditor(Resistances resistances, DamageSource source, float amount)
    {
        return flansArmorEffects() ? amount : resistances.applyResistance(source, amount);
    }

    @Redirect(method = "knockback", at = @At(value = "FIELD", target = "Lnoppes/npcs/Resistances;knockback:F", opcode = Opcodes.GETFIELD))
    private float wolffsmodnpcsNativeKnockback(Resistances resistances)
    {
        return flansArmorEffects() ? 1F : resistances.knockback;
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
        if (NpcEquipment.weaponAuthority(npc))
        {
            // Scripted ranged triggers use the same item action, never the editor projectile template.
            if (NpcEquipment.ranged(npc) && NpcItemAttacks.ranged(npc, target))
                NpcItemAttacks.launched(npc, target);
            callback.cancel();
            return;
        }
        if (NpcRangedAttack.fire(npc, target, distanceFactor == 1F, wolffsmodnpcsRound))
        {
            wolffsmodnpcsRound = wolffsmodnpcsRound >= Integer.MAX_VALUE / 10 - 1 ? 0 : wolffsmodnpcsRound + 1;
            callback.cancel();
        }
    }

    // Type properties

    @Inject(method = "tick", at = @At("HEAD"))
    private void wolffsmodnpcsInheritType(CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (!npc.level().isClientSide)
            NpcTypeProperties.of(npc).refresh(npc);
    }

    @Inject(method = "writeSpawnData()Lnet/minecraft/nbt/CompoundTag;", at = @At("RETURN"))
    private void wolffsmodnpcsSyncTypeDefaults(CallbackInfoReturnable<CompoundTag> callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        callback.getReturnValue().put(WOLFFSMODNPCS_TYPE_DEFAULTS, NpcTypeProperties.of(npc).syncDefaults(npc));
    }

    @Inject(method = "readSpawnData(Lnet/minecraft/nbt/CompoundTag;)V", at = @At("TAIL"))
    private void wolffsmodnpcsReadTypeDefaults(CompoundTag tag, CallbackInfo callback)
    {
        EntityNPCInterface npc = (EntityNPCInterface) (Object) this;
        if (npc.level().isClientSide && tag.contains(WOLFFSMODNPCS_TYPE_DEFAULTS))
            NpcTypeProperties.of(npc).readSyncedDefaults(npc, tag.getCompound(WOLFFSMODNPCS_TYPE_DEFAULTS));
    }

    // Tracking and render distance

    @Inject(method = "defineSynchedData", at = @At("TAIL"))
    private void wolffsmodnpcsDefineDistance(SynchedEntityData.Builder builder, CallbackInfo callback)
    {
        builder.define(WOLFFSMODNPCS_DISTANCE_DATA, false);
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
