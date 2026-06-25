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
import org.bukkit.entity.ArmorStand;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ArmorStandHandle {
	ArmorStand stand;
	Vector3f localOffset;
	float localYaw;
	boolean originalGravity;
	Vector3i blockOffset;
	Vector3f intraBlockOffset;
	float shipYawAtAssemble;
	

	public ArmorStandHandle(ArmorStand stand, Location anchor, float shipYawAtAssemble) {
		this.stand = stand;
		this.shipYawAtAssemble = shipYawAtAssemble;
		Location standLoc = stand.getLocation();

		//this is needed to ensure the armor stands restore at the correct offset
		//on the block to match where they started otherwise they drift
		int sx = standLoc.getBlockX();
		int sy = standLoc.getBlockY();
		int sz = standLoc.getBlockZ();
		this.blockOffset = new Vector3i(sx - anchor.getBlockX(), sy - anchor.getBlockY(), sz - anchor.getBlockZ());
		this.intraBlockOffset = new Vector3f((float)(standLoc.getX() - sx),
																				 (float)(standLoc.getY() - sy),
																				 (float)(standLoc.getZ() - sz));
			

			
		Vector3f worldOffset = new Vector3f((float)(standLoc.getX() - anchor.getX()),
																				(float)(standLoc.getY() - anchor.getY()),
																				(float)(standLoc.getZ() - anchor.getZ()));
		Quaternionf inverseShipRotation = new Quaternionf().rotateY((float)Math.toRadians(shipYawAtAssemble));
		this.localOffset = new Vector3f(worldOffset);
		inverseShipRotation.transform(this.localOffset);
			
		this.localYaw = UtilFuncs.wrapDegrees(stand.getLocation().getYaw() - shipYawAtAssemble);
		this.originalGravity = stand.hasGravity();
		stand.setGravity(false);
	}

	void move(Location anchor, float yaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(-yaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		Location loc = anchor.clone().add(worldOffset.x, worldOffset.y, worldOffset.z);

		loc.setYaw(UtilFuncs.wrapDegrees(yaw + localYaw));
		stand.teleport(loc);
	}

	void restore(Location anchor, float finalYaw) {
		Vector3i rotated = UtilFuncs.rotateOffsetCardinalInt(this.blockOffset, this.shipYawAtAssemble, finalYaw);
		Location blockLoc = anchor.clone().add(rotated.x, rotated.y, rotated.z);
		Block block = blockLoc.getBlock();
		Location loc = block.getLocation().clone().add(this.intraBlockOffset.x, this.intraBlockOffset.y, this.intraBlockOffset.z);
		loc.setYaw(UtilFuncs.wrapDegrees(finalYaw + localYaw));
		stand.teleport(loc);
		stand.setGravity(originalGravity);
	}
		
}
