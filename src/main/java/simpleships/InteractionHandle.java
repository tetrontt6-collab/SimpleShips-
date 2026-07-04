package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Interaction;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class InteractionHandle {
	Interaction inter;
	Vector3f localOffset;
	float localYaw;
	boolean originalGravity;
	Vector3i blockOffset;
	Vector3f intraBlockOffset;
	float shipYawAtAssemble;

	public InteractionHandle(Interaction inter, Location anchor, float shipYawAtAssemble) {
		this.inter = inter;
		this.shipYawAtAssemble = shipYawAtAssemble;
		Location interLoc = inter.getLocation();

		//this is needed to ensure the interactions restore at the correct offset
		//on the block to match where they started otherwise they drift
		int sx = interLoc.getBlockX();
		int sy = interLoc.getBlockY();
		int sz = interLoc.getBlockZ();
		this.blockOffset = new Vector3i(sx - anchor.getBlockX(), sy - anchor.getBlockY(), sz - anchor.getBlockZ());
		this.intraBlockOffset = new Vector3f((float)(interLoc.getX() - sx),
																				 (float)(interLoc.getY() - sy),
																				 (float)(interLoc.getZ() - sz));
			

			
		Vector3f worldOffset = new Vector3f((float)(interLoc.getX() - anchor.getX()),
																				(float)(interLoc.getY() - anchor.getY()),
																				(float)(interLoc.getZ() - anchor.getZ()));
		Quaternionf inverseShipRotation = new Quaternionf().rotateY((float)Math.toRadians(shipYawAtAssemble));
		this.localOffset = new Vector3f(worldOffset);
		inverseShipRotation.transform(this.localOffset);
			
		this.localYaw = UtilFuncs.wrapDegrees(inter.getLocation().getYaw() - shipYawAtAssemble);
		this.originalGravity = inter.hasGravity();
		inter.setGravity(false);
	}

	void move(Location anchor, float yaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(-yaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		Location loc = anchor.clone().add(worldOffset.x, worldOffset.y, worldOffset.z);

		loc.setYaw(UtilFuncs.wrapDegrees(yaw + localYaw));
		inter.teleport(loc);
	}

	void restore(Location anchor, float finalYaw) {
		Vector3i rotated = UtilFuncs.rotateOffsetCardinalInt(this.blockOffset, this.shipYawAtAssemble, finalYaw);
		Location blockLoc = anchor.clone().add(rotated.x, rotated.y, rotated.z);
		Block block = blockLoc.getBlock();
		Location loc = block.getLocation().clone().add(this.intraBlockOffset.x, this.intraBlockOffset.y, this.intraBlockOffset.z);
		loc.setYaw(UtilFuncs.wrapDegrees(finalYaw + localYaw));
		inter.teleport(loc);
		inter.setGravity(originalGravity);
	}
		
}
