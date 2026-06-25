package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

import static simpleships.SimpleShipsPlugin.LOG;

public class PassengerSeatHandle {
	final PassengerSeat seat;
	final Vector3f localOffset;
	final Vector3i blockOffset;
	final Vector3f intraBlockOffset;
	final float shipYawAtAssemble;
	final float localYaw;
	Location spawnLocation;

	PassengerSeatHandle(PassengerSeat seat, Location anchor, float shipYawAtAssemble) {
		this.seat = seat;
		this.shipYawAtAssemble = shipYawAtAssemble;


		Location seatLoc = seat.getLocation();

		int px = seatLoc.getBlockX();
		int py = seatLoc.getBlockY();
		int pz = seatLoc.getBlockZ();

		this.blockOffset = new Vector3i(px - anchor.getBlockX(), py - anchor.getBlockY(), pz - anchor.getBlockZ());
		this.intraBlockOffset = new Vector3f((float)(seatLoc.getX() - px),
																				 (float)(seatLoc.getY() - py),
																				 (float)(seatLoc.getZ() - pz));
		Vector3f worldOffset = new Vector3f((float)(seatLoc.getX() - anchor.getX()),
																				(float)(seatLoc.getY() - anchor.getY()),
																				(float)(seatLoc.getZ() - anchor.getZ()));

		Quaternionf inverseShipRotation =
			new Quaternionf().rotateY((float)Math.toRadians(shipYawAtAssemble));
		
		this.localOffset = new Vector3f(worldOffset);
		inverseShipRotation.transform(this.localOffset);

		this.localYaw = UtilFuncs.wrapDegrees(seat.getLocation().getYaw() - shipYawAtAssemble);

		
		Player rider = seat.getPassenger();
		if( rider != null ) {
			Location loc = rider.getRespawnLocation();
			if( loc != null ) {
				spawnLocation = loc.clone();
				LOG(0,"Passenger %s respawns at (%d,%d,%d)", rider.getUniqueId().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
			}
		}
	}
	
	public void move(Location anchor, float shipYaw) {
		Quaternionf rotation =
			new Quaternionf().rotateY((float)Math.toRadians(-shipYaw));
		
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		
		Location newLoc = anchor.clone().add(worldOffset.x,worldOffset.y,worldOffset.z);

		newLoc.setYaw(UtilFuncs.wrapDegrees(shipYaw + localYaw));
		seat.move(newLoc);
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
		seat.move(loc);
	}	

}
