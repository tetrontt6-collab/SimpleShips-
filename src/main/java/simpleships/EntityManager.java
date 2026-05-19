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

/**
 * The EntityManager handles management of the ship components - seat, pad
 * except the helm which is managed directly by the {@link:Ship.class} and
 * {@link:HelmListener.class}.
 */
public class EntityManager implements Listener {
	static Set<EntityPad> entityPads = new HashSet<>();
	static Set<PassengerSeat> passengerSeats = new HashSet<>();
	final SimpleShipsPlugin plugin;
	final HelmListener helmListener;
	
	public EntityManager(SimpleShipsPlugin plugin, HelmListener helmListener) {
		this.plugin = plugin;
		this.helmListener = helmListener;
	}

	public void rehydrateEntities() {
		for(World world : Bukkit.getWorlds()) {
			for( Interaction inter : world.getEntitiesByClass(Interaction.class)) {
				if(EntityPad.isEntityPadInteraction(inter)) {
					EntityPad pad = EntityPad.findPad(inter);
					if( pad != null ) {
						entityPads.add(pad);
					} else {
						Location l = inter.getLocation();
						SimpleShipsPlugin.log(1,"Found orphaned entity pad at (%f,%f,%f)", l.getX(), l.getY(), l.getZ());
					}
				}
			}
			for(ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
				if(PassengerSeat.isPassengerSeat(stand)) {
					PassengerSeat seat = PassengerSeat.findSeat(stand);
					if( seat != null ) {
						passengerSeats.add(seat);
					} else {
						Location l = stand.getLocation();
						SimpleShipsPlugin.log(1,"Found orphaned passenger seat at (%f,%f,%f)", l.getX(), l.getY(), l.getZ());
					}
					
				}
			}
		}
	}
	
	static public LivingEntity findNearestLivingEntityNotInVehicle(Player player, Location center, double radius) {
		LivingEntity best = null;
		double bestDistance = radius * radius;

		for(Entity entity : center.getWorld().getNearbyEntities(center, radius, radius, radius)) {
			if(!(entity instanceof LivingEntity living)) {
				continue;
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

	//==============Entity Pads==================//
	static public void addEntityPad(EntityPad pad) {
		entityPads.add(pad);
	}
	static EntityPad getEntityPad(UUID uuid) {
		if( uuid != null)
			return getEntityPad(uuid.toString());
		return null;
	}
	static EntityPad getEntityPad(String id) {
		if(id == null)
			return null;
		for(EntityPad pad : entityPads) {
			if( id.equals(pad.getPadIdStr())) {
				return pad;
			}
		}
		return null;
	}
	static public void removeEntityPad(EntityPad pad) {
		if( pad != null )
			entityPads.remove(pad);
	}
	
	static public void removeEntityPad(String id) {
		if(id == null )
			return;
		EntityPad toRemove = getEntityPad(id);
		if( toRemove != null ) {
			toRemove.removeFromWorld();
			entityPads.remove(toRemove);
		}
	}

	//==============PassengerSeats Pads==================//
	static public void addPassengerSeat(PassengerSeat pad) {
		passengerSeats.add(pad);
	}
	static PassengerSeat getPassengerSeat(UUID uuid) {
		if( uuid != null)
			return getPassengerSeat(uuid.toString());
		return null;
	}
	static PassengerSeat getPassengerSeat(String id) {
		if(id == null)
			return null;
		for(PassengerSeat seat : passengerSeats) {
			if( id.equals(seat.getSeatIdStr())) {
				return seat;
			}
		}
		return null;
	}
	static public void removePassengerSeat(PassengerSeat pad) {
		if( pad != null )
			passengerSeats.remove(pad);
	}
	
	static public void removePassengerSeat(String id) {
		if(id == null )
			return;
		PassengerSeat toRemove = getPassengerSeat(id);
		if( toRemove != null ) {
			toRemove.removeFromWorld();
			passengerSeats.remove(toRemove);
		}
	}

	/*
	 * For detaching large entities from the Entity Pad, the Interaction
	 * can be blocked (like say a horse), so this provides an alternate path
	 * to detaching them.
	 */
	@EventHandler
	public void onMountedEntityClicked(PlayerInteractAtEntityEvent event) {
		if(!(event.getRightClicked() instanceof LivingEntity living)) {
			return;
		}

		if(living instanceof ArmorStand) {
			return;
		}

		Player player = event.getPlayer();
		if(!player.isSneaking()) {
			return;
		}

		if(!(living.getVehicle() instanceof ArmorStand stand)) {
			return;
		}

		if(!EntityPad.isEntityPadPost(stand)) {
			return;
		}

		EntityPad pad = getEntityPad(stand.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING));
		if( pad != null ) {
			pad.detachEntity();
			player.sendMessage("Entity detached");
		}
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
			} else if(PassengerSeat.isPassengerSeat(stand)) {
				event.setCancelled(true);
				PassengerSeat.onStandClicked(event.getPlayer(), stand, event);
			}
		}

		if(event.getRightClicked() instanceof Interaction interaction ) {
			if(EntityPad.isEntityPadInteraction(interaction)) {
				event.setCancelled(true);
				EntityPad.onInteractionClicked(event.getPlayer(), interaction, event);
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
				} else if(PassengerSeat.isPassengerSeat(stand)) {
					PassengerSeat.onEntityHit(player, stand, event);
					event.setCancelled(true);
				}
			}
			if( target instanceof Interaction inter) {
				if( EntityPad.isEntityPadInteraction(inter) ) {
					EntityPad.onEntityHit(player, inter, event);
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
		} else if( EntityPad.isEntityPad(type))  {
			event.setCancelled(true);
			EntityPad.onPlayerPlaceEntity(event, item);
		} else if( PassengerSeat.isPassengerSeat(type) ) {
			event.setCancelled(true);
			PassengerSeat.onPlayerPlaceEntity(event, item);
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		for(Entity entity : event.getChunk().getEntities()) {
			if( entity instanceof ArmorStand stand ) {
				if(HelmSeat.isHelmSeat(stand)) {
					helmListener.onChunkLoad(stand);
				} else if(PassengerSeat.isPassengerSeat(stand)) {
					PassengerSeat seat = PassengerSeat.findSeat(stand);
					if( seat != null ) {
						passengerSeats.add(seat);
					}
				}
			} else if( entity instanceof Interaction inter) {
				if(EntityPad.isEntityPadInteraction(inter)) {
					EntityPad pad = EntityPad.findPad(inter);
					if( pad != null ) {
						entityPads.add(pad);
					}
				}
			}
		}
	}

	public void flushAll() {
		for(EntityPad pad : entityPads ) {
			pad.removeFromWorld();
		}
		entityPads.clear();
		
		for(PassengerSeat seat : passengerSeats ) {
			seat.removeFromWorld();
		}
		passengerSeats.clear();
	}

	/**
	 * This will search for all Display, ArmorStand and Interactions
	 * that are tagged as a ship component and remove it from the world.
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
