package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */


import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static simpleships.SimpleShipsPlugin.LOG;

/**
 * Provides a ship component to allow carrying of living
 * entities, (not players)
 */
public class EntityPad {

	Location padLocation;
	CompositeDisplay pad;
	
	public EntityPad(Location loc) {
		this.padLocation = new Location(loc.getWorld(), loc.getBlockX() + 0.5, loc.getBlockY() + 1, loc.getBlockZ() + 0.5);
		this.padLocation.setYaw(loc.getYaw());
		this.padLocation.setPitch(loc.getPitch());

		LOG(0, "new entity pad at (%f,%f,%f)", padLocation.getX(), padLocation.getY(), padLocation.getZ());
		createEntityPad();
	}

	public EntityPad(CompositeDisplay cd) {
		this.padLocation = cd.getLocation().clone();
		this.pad = cd;
		LOG(0,"Creating entity pad from composite display, found %d components at (%f,%f,%f)",
				cd.getNumberOfComponents(),
				padLocation.getX(), padLocation.getY(), padLocation.getZ());
		
	}

	private void createEntityPad() {
		pad = new CompositeDisplay(Constants.ENTITY_PAD_ITEM_TYPE, padLocation, true, 1.0, 1.2)
			.addArmorStand(new Vector3f(0f, -.80f, 0f), false, true, padLocation.getYaw())
			.addBlock(Material.HORN_CORAL_FAN, new Vector3f(-0.5f, 0.0f, -0.5f), new Vector3f(0, 0, 0), new Vector3f(1,1,1))
			.addKey(Constants.ITEM_TYPE_KEY, Constants.ENTITY_PAD_ITEM_TYPE)
			.addKey(Constants.SHIP_COMPONENT_KEY, Constants.SHIP_COMPONENT_ITEM_TYPE)
			.addKey(Constants.SIMPLE_SHIPS_COMPONENT, true)
			.spawn();
	}

	public Location getLocation() {
		return this.padLocation;
	}
	public void move(Location loc) {
		padLocation = loc.clone();
		pad.moveTo(padLocation);
	}

	void removeFromWorld() {
		pad.remove();
	}
}
