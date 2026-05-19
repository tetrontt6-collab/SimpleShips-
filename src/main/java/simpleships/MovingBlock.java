package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.joml.Vector3f;

/**
 * This allows for regular blocks, such as {@link:org.bukkit.block.data.type.Light} blocks
 * (or any other direct blocks) to be part of the ship and moved.  Unlike the {@link:org.bukkit.entity.BlockDisplay}
 * block entities, a normal block can not move in partial block increments, they must
 * go on block boundaries.
 *
 * The initial use of this was for the hidden light blocks that are created associated
 * with any light source on the ships.
 */
public class MovingBlock {
	Location location = null;
	Vector3f offset = new Vector3f();
	BlockData data;
	Material blockType;

	public MovingBlock(Location helm, Vector3f offset, BlockData data, Material blockType) {
		location = helm.clone();  
		this.offset.set(offset);
		this.data = data.clone();
		this.blockType = blockType;
		moveTo(helm);
	}
	void moveTo(Location l) {
		location.setX(l.getX() + offset.x);
		location.setY(l.getY() + offset.y);
		location.setZ(l.getZ() + offset.z);
	}
	void render() {
		location.getBlock().setBlockData(data);
	}

	void remove() {
		Block block = location.getBlock();
		if(block.getType() == this.blockType ) 
			location.getBlock().setType(Material.AIR);
	}

}
