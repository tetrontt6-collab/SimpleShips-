
package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Parrot;
import org.bukkit.entity.Player;
import org.bukkit.entity.WaterMob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static simpleships.SimpleShipsPlugin.LOG;

/**
 * The EntityManager handles management of the ship components - seat, pad
 * except the helm which is managed directly by the {@link:Ship.class} and
 * {@link:HelmListener.class}.
 */
public class EntityManager implements Listener {
	final SimpleShipsPlugin plugin;
	final HelmListener helmListener;
	
	public EntityManager(SimpleShipsPlugin plugin, HelmListener helmListener) {
		this.plugin = plugin;
		this.helmListener = helmListener;
	}

	protected void checkForShipEntity(Entity entity) {
		String id = entity.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING);
		Location entityLocation = entity.getLocation();
			
		if( entity instanceof ArmorStand stand ) {
			if(HelmSeat.isHelmSeat(stand)) {
				helmListener.onChunkLoad(stand);
			}
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for(Entity entity : event.getChunk().getEntities()) {
			checkForShipEntity(entity);
		}
	}
	
	public void rehydrateEntities() {
		for(World world : Bukkit.getWorlds()) {
			for( Interaction inter : world.getEntitiesByClass(Interaction.class)) {
				checkForShipEntity(inter);
			}
			for(ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
				checkForShipEntity(stand);
			}
		}
	}

	static public Parrot findNearestParrotNotInVehicle(Player player, Location center, double radius) {
		LivingEntity entity = findNearestLivingEntityNotInVehicle(player, center, radius, true);
		if( entity != null && (entity instanceof Parrot parrot))
			return parrot;
		return null;
	}
	
	static public LivingEntity findNearestLivingEntityNotInVehicle(Player player, Location center, double radius) {
		return findNearestLivingEntityNotInVehicle(player, center, radius, false);
	}
	static public LivingEntity findNearestLivingEntityNotInVehicle(Player player, Location center, double radius, boolean onlyParrots) {
		LivingEntity best = null;
		double bestDistance = radius * radius;

		for(Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
			if(!(entity instanceof LivingEntity living)) {
				continue;
			}
			if( onlyParrots) {
				if(!(entity instanceof Parrot parrot)) {
					continue;
				}
			}
			if( living instanceof WaterMob) {
				continue;
			}
			if( living instanceof ArmorStand) {
				continue;
			}
			if( living instanceof Player )
				continue;

			if( living.isInsideVehicle())
				continue;

			double dist = living.getLocation().distanceSquared(center);
			if( dist < bestDistance ) {
				bestDistance = dist;
				best = living;
			}
		}
		return best;
	}


	
	/*
	 * This method handles the player interacting with a placed
	 * control entity
	 */
	@EventHandler
	public void onEntityClicked(PlayerInteractAtEntityEvent event) {
		if(event.getRightClicked() instanceof ArmorStand stand) {
			if( HelmSeat.isHelmSeat(stand)) {
				event.setCancelled(true);
				HelmListener.onEntityClicked(event.getPlayer(), stand, event);
				return;
			}
		}
	}

	/*
	 * This method manages the breaking of a control entity.
	 */
	@EventHandler
	public void doEntityHit(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		Entity target = event.getEntity();

		if( damager instanceof Player player ) {
			if(target instanceof ArmorStand stand ) {
				if( HelmSeat.isHelmSeat(stand) ) {
					HelmListener.onEntityHit(player, stand, event);
					event.setCancelled(true);
				}
			}
		}
	}

	/**
	 * This method handls the placement of the custom
	 * items that make up the different control entities
	 */
	@EventHandler
	public void onPlayerPlaceEntity(PlayerInteractEvent event) {
		if( event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		ItemStack item = event.getItem();
		if(item == null ) {
			return;
		}
		ItemMeta meta = item.getItemMeta();
		if(meta == null )
			return;
		
		String type = meta.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING);
		if(HelmSeat.isHelmSeat(type) ) {
			event.setCancelled(true);
			HelmListener.onPlayerPlaceEntity(event, item);
		}
	}


	public void flushAll(World world) {
		for(Entity entity : world.getEntities()) {
			if( Constants.SHIP_COMPONENT_ITEM_TYPE.equals(entity.getPersistentDataContainer().get(Constants.SHIP_COMPONENT_KEY, PersistentDataType.STRING))) {
				entity.remove();
			}	else {
				Boolean bool = entity.getPersistentDataContainer().get(Constants.SIMPLE_SHIPS_COMPONENT, PersistentDataType.BOOLEAN);
				if( bool != null && bool )
					entity.remove();
			}
		}

	}

	/**
	 * This will search for all Display, ArmorStand and Interactions
	 * that are tagged as a ship component and remove it from all worlds.
	 *
	 * Useful for clearing components without doing bulk kills on other
	 * entities.
	 */
	public void removeAllComponents() {
		for(World world : Bukkit.getWorlds()) {
			for( Interaction inter : world.getEntitiesByClass(Interaction.class)) {
				if(inter.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING) != null ) {
					inter.remove();
				}
			}
			for(ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
				if( stand.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING) != null ) {
					stand.remove();
				}
			}
			for(BlockDisplay bd : world.getEntitiesByClass(BlockDisplay.class)) {
				if( bd.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING) != null ) {
					bd.remove();
				}
			}
			for(ItemDisplay id : world.getEntitiesByClass(ItemDisplay.class)) {
				if( id.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING) != null ) {
					id.remove();
				}
			}
		}
	}
}
