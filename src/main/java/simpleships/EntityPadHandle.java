package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class EntityPadHandle {
	final EntityPad pad;
	final Vector3f localOffset;
	final Vector3i blockOffset;
	final Vector3f intraBlockOffset;
	final float shipYawAtAssemble;
	final float localYaw;
	
	public EntityPadHandle(EntityPad pad, Location anchor, float shipYawAtAssemble) {
		this.pad = pad;
		this.shipYawAtAssemble = shipYawAtAssemble;

		Location padLoc = pad.getLocation();

		int px = padLoc.getBlockX();
		int py = padLoc.getBlockY();
		int pz = padLoc.getBlockZ();

		this.blockOffset = new Vector3i(px - anchor.getBlockX(), py - anchor.getBlockY(), pz - anchor.getBlockZ());
		this.intraBlockOffset = new Vector3f((float)(padLoc.getX() - px),
																				 (float)(padLoc.getY() - py),
																				 (float)(padLoc.getZ() - pz));
		Vector3f worldOffset = new Vector3f((float)(padLoc.getX() - anchor.getX()),
																				(float)(padLoc.getY() - anchor.getY()),
																				(float)(padLoc.getZ() - anchor.getZ()));

		Quaternionf inverseShipRotation =
			new Quaternionf().rotateY((float)Math.toRadians(shipYawAtAssemble));
		
		this.localOffset = new Vector3f(worldOffset);
		inverseShipRotation.transform(this.localOffset);

		this.localYaw = UtilFuncs.wrapDegrees(pad.getLocation().getYaw() - shipYawAtAssemble);
		
	}
	
	public void move(Location anchor, float shipYaw) {
		Quaternionf rotation =
			new Quaternionf().rotateY((float)Math.toRadians(-shipYaw));
		
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		
		Location newLoc = anchor.clone().add(worldOffset.x,worldOffset.y,worldOffset.z);

		newLoc.setYaw(UtilFuncs.wrapDegrees(shipYaw + localYaw));
		pad.move(newLoc);
	}


	public void restore(Location anchor, float finalYaw) {
		Vector3i rotated = UtilFuncs.rotateOffsetCardinalInt(this.blockOffset,
																												 this.shipYawAtAssemble,
																												 finalYaw);

		Location blockLoc = anchor.clone().add(rotated.x, rotated.y, rotated.z);
		
		Location loc = blockLoc.getBlock().getLocation().clone().add(this.intraBlockOffset.x,
																																 this.intraBlockOffset.y,
																																 this.intraBlockOffset.z);
		
		loc.setYaw(UtilFuncs.wrapDegrees(finalYaw));
		pad.move(loc);
	}	

}
