package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class ParrotPerchHandle {
	ParrotPerch perch;
	Vector3f localOffset;
	float localYaw;
	Vector3i blockOffset;
	Vector3f intraBlockOffset;
	float shipYawAtAssemble;
	Location startingLoc;
	
	public ParrotPerchHandle(ParrotPerch perch, Location anchor, float shipYawAtAssemble) {
		this.perch = perch;
		this.shipYawAtAssemble = shipYawAtAssemble;
		startingLoc = perch.getPerchStandLocation().clone();
		Location standLoc = perch.getPerchStandLocation();
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
			
		this.localYaw = UtilFuncs.wrapDegrees(perch.getPerchStandLocation().getYaw() - shipYawAtAssemble);
	}
	
	public void move(Location anchor, float yaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(-yaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		Location loc = anchor.clone().add(worldOffset.x, worldOffset.y, worldOffset.z);

		loc.setYaw(UtilFuncs.wrapDegrees(yaw + localYaw));
		perch.move(loc);
	}

	void restore(Location anchor, float finalYaw) {
		Vector3i rotated = UtilFuncs.rotateOffsetCardinalInt(this.blockOffset, this.shipYawAtAssemble, finalYaw);
		Location blockLoc = anchor.clone().add(rotated.x, rotated.y, rotated.z);
		Block block = blockLoc.getBlock();
		Location loc = block.getLocation().clone().add(this.intraBlockOffset.x, this.intraBlockOffset.y, this.intraBlockOffset.z);
		loc.setYaw(UtilFuncs.wrapDegrees(finalYaw + localYaw));
		perch.move(loc);
	}
}
