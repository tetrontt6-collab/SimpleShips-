package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * The ItemFrameHandle is used to capture the details of
 * an ItemFrame or GlowingItemFrame that is part of a ship
 */
class ItemFrameHandle{
	Location loc;
	Vector3i attachedBlockOffset;
	Vector3f frameOffset;
	BlockFace facing;
	BlockFace attachedFace;
	boolean isGlowItemFrame;
		
	ItemStack itemStack;
	boolean fixed;
	boolean visible;
	Rotation rotation;

	ItemFrameHandle(Location startLoc, Location loc, Vector3f offset, ItemFrame frame) {
		this.loc = loc.clone();
		this.frameOffset = new Vector3f(offset);
		this.itemStack = frame.getItem().clone();
		this.fixed = frame.isFixed();
		this.visible = frame.isVisible();
		this.rotation = frame.getRotation();
		this.attachedFace = frame.getAttachedFace();
		this.facing = frame.getFacing();
		this.isGlowItemFrame = (frame instanceof GlowItemFrame);

		Block attachedBlock = frame.getLocation().getBlock().getRelative(frame.getAttachedFace());
		this.attachedBlockOffset = new Vector3i(attachedBlock.getX() - startLoc.getBlockX(),
																						attachedBlock.getY() - startLoc.getBlockY(),
																						attachedBlock.getZ() - startLoc.getBlockZ());
	}
	
}
