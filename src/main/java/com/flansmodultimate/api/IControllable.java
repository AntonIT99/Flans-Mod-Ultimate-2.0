package com.flansmodultimate.api;

import com.flansmodultimate.common.driveables.Seat;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public interface IControllable
{
	/**
	 * This is fired every tick.
	 *
	 * @param deltaX change in X of the mouse.
	 * @param deltaY change in Y of the mouse.
	 */
	void onMouseMoved(double deltaX, double deltaY);
	
	/**
	 * @param key the keycode of the key. see @link:KeyInputHandler
	 * @return boolean to indicate it this key was handled.
	 */
	boolean pressKey(int key, Player player, boolean isOnEvent);
	
	boolean serverHandleKeyPress(int key, Player player);
	
	void updateKeyHeldState(int key, boolean held);
	
	/**
	 * @return riddenByEntity
	 */
	Entity getControllingEntity();
	
	boolean isDead();
	
	/**
	 * @return The player's view roll
	 */
	float getPlayerRoll();
	
	float getPrevPlayerRoll();
	
	/**
	 * @return The player's 3rd person view distance
	 */
	float getCameraDistance();
	
	@OnlyIn(Dist.CLIENT)
    LivingEntity getCamera();

	Seat getSeat(LivingEntity living);
}
