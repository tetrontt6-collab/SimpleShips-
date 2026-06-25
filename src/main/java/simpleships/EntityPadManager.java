package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * Responsible for handling details related to the entity pad
 */

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import static simpleships.SimpleShipsPlugin.LOG;

public class EntityPadManager implements Listener {
	final SimpleShipsPlugin plugin;

	public EntityPadManager(SimpleShipsPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler
	public void onPlayerPlace(PlayerInteractEvent event) {
		if( event.getAction() != Action.RIGHT_CLICK_BLOCK)
			return;
		ItemStack itemStack = event.getItem();
		if(itemStack == null ) {
			return;
		}
		ItemMeta meta = itemStack.getItemMeta();
		if(meta == null )
			return;

		String type = meta.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING);
		if(type == null || !Constants.ENTITY_PAD_ITEM_TYPE.equals(type))
			return;

		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if( block == null )
			return;

		if(!BlockSupport.isBlockAllowed(block.getType())) {
			LOG(1,player,"Entity pad must be placed on an allowed block");
			return;
		}
		if( BlockSupport.isLowerSlab(block)) {
			LOG(1,player,"Entity pad can not be placed on a lower slab");
			return;
		}
		event.setCancelled(true);

		if( player.getGameMode() != GameMode.CREATIVE) {
			itemStack.subtract(1);
		}

		EntityPad pad = new EntityPad(block.getLocation());
	}

	@EventHandler
	public void onEntityClicked(PlayerInteractAtEntityEvent event) {
		Entity entity = event.getRightClicked();
		Interaction inter = null;
		if( entity instanceof Interaction) {
			inter = (Interaction)event.getRightClicked();
		} else {
			String id = entity.getPersistentDataContainer().get(Constants.SS_CD_INTERACTION_ID, PersistentDataType.STRING);
			if( id == null )
				return;
			Entity byid = entity.getWorld().getEntity(UUID.fromString(id));
			if( byid == null )
				return;
			if( !(byid instanceof Interaction)) {
				return;
			}
			inter = (Interaction)byid;
		}
		
		CompositeDisplay cd = CompositeDisplay.reconstituteFromInteraction(Constants.ENTITY_PAD_ITEM_TYPE, inter);
		if( cd == null )
			return;
		
		event.setCancelled(true);

		Player player = event.getPlayer();
		if( player.isSneaking()) {
			cd.removePassengers();
			player.sendMessage("Entity detached");
			return;
		}

		if( cd.hasPassenger() ) {
			player.sendMessage("Entity pad already occupied");
			return;
		}
		
		LivingEntity nearest = EntityManager.findNearestLivingEntityNotInVehicle(player, cd.getLocation(), 3.0);
		if( nearest == null ) {
			player.sendMessage("No nearby entities found");
			return;
		}

		cd.mountPassenger(nearest);
		player.sendMessage(String.format("Attached %s to pad %s", nearest.getType().getName(), cd.getId()));
	}

	@EventHandler
	public void onEntityHit(EntityDamageByEntityEvent event) {
		
		if( !(event.getDamager() instanceof Player player))
			return;  //only care about players causing damage
		if( !(event.getEntity() instanceof Interaction inter )) {
			return;
		}
		CompositeDisplay cd = CompositeDisplay.reconstituteFromInteraction(Constants.ENTITY_PAD_ITEM_TYPE, inter);
		if( cd == null )
			return;

		event.setCancelled(true);
		if( player.isSneaking()) {
			if( cd.hasPassenger()) {
				player.sendMessage("Need to remove passenger before picking pad up");
				return;
			} else {
				cd.remove(); 
				if( player.getGameMode() != GameMode.CREATIVE) {
					givePadToPlayer(player);
				}
			}
		}
	}
		
	static public void givePadToPlayer(Player player) {
		ItemStack itemStack = createEntityPadItemStack();
		if( itemStack != null )
			player.getInventory().addItem(itemStack);
	}

	static public ItemStack createEntityPadItemStack() {
		ItemStack itemStack = ItemStack.of(Material.HORN_CORAL_FAN);
		ItemMeta meta = itemStack.getItemMeta();

		if( meta == null ) {
			return null;
		}

		meta.displayName(Component.text(Constants.getStringFor(Constants.ENTITY_PAD_ITEM_TYPE)));
		meta.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.ENTITY_PAD_ITEM_TYPE);
		itemStack.setItemMeta(meta);
		return itemStack;
	}
}
