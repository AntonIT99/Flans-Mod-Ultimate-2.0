package com.flansmodultimate.common.types;

import com.flansmod.common.vector.Vector3f;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.flansmodultimate.util.TypeReaderUtils.*;

@Getter
@NoArgsConstructor
public class MechaType extends DriveableType
{
    public record LegNode(int rotation, float lowerBound, float upperBound, int speed, int legPart) {}

    protected float turnLeftModifier = 1F;
    protected float turnRightModifier = 1F;
    protected float moveSpeed = 1F;
    protected boolean squashMobs;
    protected int stepHeight;
    protected float jumpHeight = -99F;
    protected float jumpVelocity = 1F;
    protected float rotateSpeed = 10F;
    protected Vector3f leftArmOrigin = new Vector3f();
    protected Vector3f rightArmOrigin = new Vector3f();
    protected float armLength = 1F;
    protected float legLength = 1F;
    protected float heldItemScale = 1F;
    protected float height = 3F;
    protected float width = 2F;
    protected float chassisHeight = 1F;
    protected float reach = 10F;
    protected boolean damageBlocksFromFalling = true;
    protected float blockDamageFromFalling = 1F;
    protected boolean takeFallDamage = true;
    protected float fallDamageMultiplier = 1F;
    protected float legSwingLimit = 2F;
    protected boolean limitHeadTurn;
    protected float limitHeadTurnValue = 90F;
    protected float legSwingTime = 5F;
    protected float upperArmLimit = 90F;
    protected float lowerArmLimit = 90F;
    protected Vector3f leftHandModifier = new Vector3f();
    protected Vector3f rightHandModifier = new Vector3f();
    protected final List<LegNode> legNodes = new ArrayList<>();
    protected float legAnimSpeed;
    protected String stompSound = StringUtils.EMPTY;
    protected int stompSoundLength;
    protected float stompRangeLower;
    protected float stompRangeUpper;
    protected boolean restrictInventoryInput;
    protected boolean allowMechaToolsInRestrictedInv = true;

    @Override
    protected void read(TypeFile file)
    {
        super.read(file);
        turnLeftModifier = readValue("TurnLeftSpeed", turnLeftModifier, file);
        turnRightModifier = readValue("TurnRightSpeed", turnRightModifier, file);
        moveSpeed = readValue("MoveSpeed", moveSpeed, file);
        squashMobs = readValue("SquashMobs", squashMobs, file);
        stepHeight = Math.max(0, readValue("StepHeight", stepHeight, file));
        jumpHeight = readValue("JumpHeight", jumpHeight, file);
        jumpVelocity = jumpHeight == -99F ? 1F : (float) Math.sqrt(Math.abs(9.81F * (jumpHeight + 0.2F) / 200F));
        rotateSpeed = readValue("RotateSpeed", rotateSpeed, file);
        stompSound = readSound("StompSound", stompSound, file);
        stompSoundLength = Math.max(0, readSoundLength("StompSoundLength", stompSoundLength, file));
        stompRangeLower = readValue("StompRangeLower", stompRangeLower, file);
        stompRangeUpper = readValue("StompRangeUpper", stompRangeUpper, file);
        leftArmOrigin = modelVector("LeftArmOrigin", leftArmOrigin, file);
        rightArmOrigin = modelVector("RightArmOrigin", rightArmOrigin, file);
        armLength = Math.max(0F, readValue("ArmLength", armLength * 16F, file) / 16F);
        legLength = Math.max(0F, readValue("LegLength", legLength * 16F, file) / 16F);
        heldItemScale = Math.max(0F, readValue("HeldItemScale", heldItemScale, file));
        height = Math.max(0.1F, readValue("Height", height * 16F, file) / 16F);
        width = Math.max(0.1F, readValue("Width", width * 16F, file) / 16F);
        chassisHeight = Math.max(0F, (float) Math.floor(readValue("ChassisHeight", chassisHeight * 16F, file)) / 16F);
        fallDamageMultiplier = Math.max(0F, readValue("FallDamageMultiplier", fallDamageMultiplier, file));
        blockDamageFromFalling = Math.max(0F, readValue("BlockDamageFromFalling", blockDamageFromFalling, file));
        reach = Math.max(0F, readValue("Reach", reach, file));
        takeFallDamage = readValue("TakeFallDamage", takeFallDamage, file);
        damageBlocksFromFalling = readValue("DamageBlocksFromFalling", damageBlocksFromFalling, file);
        legSwingLimit = Math.max(0F, readValue("LegSwingLimit", legSwingLimit, file));
        readValues("LimitHeadTurn", file, 2).ifPresent(values -> {
            try
            {
                limitHeadTurn = "1".equals(values[0]) || Boolean.parseBoolean(values[0]);
                limitHeadTurnValue = Float.parseFloat(values[1]);
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse LimitHeadTurn", file, ex);
            }
        });
        legSwingTime = Math.max(0.01F, readValue("LegSwingTime", legSwingTime, file));
        upperArmLimit = readValue("UpperArmLimit", upperArmLimit, file);
        lowerArmLimit = readValue("LowerArmLimit", lowerArmLimit, file);
        leftHandModifier = modelVector("LeftHandModifier", leftHandModifier, file);
        rightHandModifier = modelVector("RightHandModifier", rightHandModifier, file);
        for (String[] values : readValuesInLines("LegNode", file).orElse(List.of()))
        {
            if (values == null || values.length < 5)
                continue;
            try
            {
                legNodes.add(new LegNode(Integer.parseInt(values[0]), Float.parseFloat(values[1]), Float.parseFloat(values[2]),
                    Integer.parseInt(values[3]), Integer.parseInt(values[4])));
            }
            catch (RuntimeException ex)
            {
                logError("Could not parse LegNode", file, ex);
            }
        }
        legAnimSpeed = readValue("LegAnimSpeed", legAnimSpeed, file);
        restrictInventoryInput = readValue("RestrictInventoryInput", restrictInventoryInput, file);
        allowMechaToolsInRestrictedInv = readValue("AllowMechaToolsInRestrictedInv", allowMechaToolsInRestrictedInv, file);
    }

    public float getLeftHandModifierX() { return leftHandModifier.x; }
    public float getLeftHandModifierY() { return leftHandModifier.y; }
    public float getLeftHandModifierZ() { return leftHandModifier.z; }
    public float getRightHandModifierX() { return rightHandModifier.x; }
    public float getRightHandModifierY() { return rightHandModifier.y; }
    public float getRightHandModifierZ() { return rightHandModifier.z; }

    private static Vector3f modelVector(String key, Vector3f fallback, TypeFile file)
    {
        Vector3f vector = readVector(key, null, file);
        return vector == null ? fallback : vector.scale(1F / 16F);
    }
}
