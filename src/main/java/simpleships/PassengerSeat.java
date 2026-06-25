package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import static simpleships.SimpleShipsPlugin.LOG;

public class PassengerSeat {
	Location seatLocation;
	CompositeDisplay seat;
	BlockFace seatFacing;
	
	public PassengerSeat(BlockFace face, Location loc) {
		this.seatLocation = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 1, loc.getBlockZ() + 0.5);
		this.seatFacing = face.getOppositeFace();
		this.seatLocation.setYaw(UtilFuncs.getDegreesFromFace(seatFacing));
		this.seatLocation.setPitch(loc.getPitch());

		createPassengerSeat();
	}

	public PassengerSeat(CompositeDisplay cd) {
		this.seatLocation = cd.getLocation().clone();
		this.seat = cd;
	}

	private void createPassengerSeat() {
		seat = new CompositeDisplay(Constants.PASSENGER_SEAT_ITEM_TYPE, seatLocation, true, 1.0, 1.2)
			.addArmorStand(new Vector3f(0f, -.40f, 0f), false, true, UtilFuncs.getYawDegrees(seatFacing.getOppositeFace()))

			.addBlock(Material.MANGROVE_STAIRS, new Vector3f(-0.5f, 0.0f, -0.5f), new Vector3f(0, 0, 0), new Vector3f(1,1,1),
								(bd) -> {
									if( bd instanceof BlockDisplay blockDisplay ) {
										BlockData data = blockDisplay.getBlock();
										if( data instanceof Stairs stairs) {
											stairs.setHalf(Bisected.Half.BOTTOM);
											stairs.setShape(Stairs.Shape.STRAIGHT);
											blockDisplay.setBlock(stairs);
										}}})
			.addBlock(Material.YELLOW_CARPET, new Vector3f(-0.49f, 0.52f, -0.49f), new Vector3f(0,0,0), new Vector3f(0.98f, 0.05f, 0.98f))
			.addKey(Constants.ITEM_TYPE_KEY, Constants.PASSENGER_SEAT_ITEM_TYPE)
			.addKey(Constants.SHIP_COMPONENT_KEY, Constants.SHIP_COMPONENT_ITEM_TYPE)
			.addKey(Constants.SIMPLE_SHIPS_COMPONENT, true)
			.spawn();

	}

	public Player getPassenger() {
		Entity passenger = seat.getPassenger();
		if( passenger != null && (passenger instanceof Player player)) {
			return player;
		}
		return null;
	}
	
	public Location getLocation() {
		return this.seatLocation;
	}
	public void move(Location loc) {
		seatLocation = loc.clone();
		seat.moveTo(seatLocation);
	}

	void removeFromWorld() {
		seat.remove();
	}

}
