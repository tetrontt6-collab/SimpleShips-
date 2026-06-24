package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import static simpleships.SimpleShipsPlugin.LOG;

public class PassengerSeatHandle {
	final PassengerSeat seat;
	final Vector3f offset;
	Location spawnLocation;

	PassengerSeatHandle(PassengerSeat seat, Vector3f offset) {
		this.seat = seat;
		this.offset = new Vector3f(offset);
		
		Player rider = seat.getPassenger();
		if( rider != null ) {
			Location loc = rider.getRespawnLocation();
			if( loc != null ) {
				spawnLocation = loc.clone();
				LOG(0,"Passenger %s respawns at (%d,%d,%d)", rider.getUniqueId().toString(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
			}
		}
	}
	
	void move(Location loc) {
		seat.move(loc);
	}

}
