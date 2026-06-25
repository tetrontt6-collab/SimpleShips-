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

public class ParrotPerch {
	Location perchLocation;
	CompositeDisplay perch;
	float yaw;
	
	public ParrotPerch(float playerYaw, Location loc) {
		this.perchLocation = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 1, loc.getBlockZ() + 0.5);
		this.yaw = UtilFuncs.getClosestFacingYawDegrees(playerYaw + 180.0f);
		this.perchLocation.setYaw(this.yaw);
		this.perchLocation.setPitch(loc.getPitch());

		createParrotPerch();
	}

	public ParrotPerch(CompositeDisplay cd) {
		this.perchLocation = cd.getLocation().clone();
		this.perch = cd;
	}

	private void createParrotPerch() {
		perch = new CompositeDisplay(Constants.PARROT_PERCH_ITEM_TYPE, perchLocation, true, 1.0, 1.2)
			.addArmorStand(new Vector3f(0f, 0.10f, 0f), false, true, yaw)

			.addBlock(Material.MANGROVE_LOG, new Vector3f(-0.05f, 0f, -0.05f), new Vector3f(0, 0, 0), new Vector3f(0.1f, 1.0625f, 0.1f))
			.addBlock(Material.STRIPPED_MANGROVE_LOG, new Vector3f(-0.5f, 1.05f, -0.05f), new Vector3f(0,0,0), new Vector3f(1.0f, 0.1f, 0.1f))
			.addKey(Constants.ITEM_TYPE_KEY, Constants.PARROT_PERCH_ITEM_TYPE)
			.addKey(Constants.SHIP_COMPONENT_KEY, Constants.SHIP_COMPONENT_ITEM_TYPE)
			.addKey(Constants.SIMPLE_SHIPS_COMPONENT, true)
			.spawn();
	}

	public Location getLocation() {
		return this.perchLocation;
	}
	public void move(Location loc) {
		perchLocation = loc.clone();
		perch.moveTo(perchLocation);
	}

	void removeFromWorld() {
		perch.remove();
	}

}
