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

/**
 * Provides a ship component to allow carrying of living
 * entities, (not players)
 */
public class EntityPad {
	static public final float V_OFFSET = 0.80f;

	Interaction clickTarget;
	ArmorStand post;
	Display nest;
	Location padLoc;
	UUID padId;
	
	EntityPad(Location loc) {
		this.padLoc = loc.clone().add(0.5f,1.0f,0.5f);
		this.padLoc.setYaw(loc.getYaw());
		createEntityPad();
		setPadId(UUID.randomUUID());
	}
	EntityPad(Interaction inter, ArmorStand post, Display nest) {
		padLoc = nest.getLocation();  //it is not adjusted like armor stand is
		this.clickTarget = inter;
		this.post = post;
		this.nest = nest;
		String padIdStr = post.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
		if(padIdStr == null ) {
			setPadId(UUID.randomUUID());
		} else {
			setPadId(UUID.fromString(padIdStr));
		}
	}

	private void createEntityPad() {
		World world = padLoc.getWorld();
		Matrix4f transform = new Matrix4f().identity().translate(-0.5f, 0.0f, -0.5f).scale(1.0f, 1.0f, 1.0f);

		Quaternionf orient = new Quaternionf().rotateY((float)Math.toRadians(padLoc.getYaw()));

		post = world.spawn(padLoc.clone().add(0,-V_OFFSET,0), ArmorStand.class, as -> {
				as.setSmall(true);
				as.setInvisible(true);
				as.setMarker(false);
				as.setGravity(false);
				as.setPersistent(true);
			});
		post.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.ENTITY_PAD_ITEM_TYPE);
		Constants.markShipComponent(post);

		BlockData nestBlockData = Bukkit.createBlockData(Material.HORN_CORAL_FAN);
		nest = world.spawn(padLoc.clone(), BlockDisplay.class, id -> {
				id.setBlock(nestBlockData);
				id.setPersistent(true);
				id.setGravity(false);
				id.setTransformationMatrix(new Matrix4f(transform));
				id.setInterpolationDuration(Constants.BD_LERP_DURATION);
				id.setInterpolationDelay(-1);
				id.setTeleportDuration(0);
			});
		nest.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.ENTITY_PAD_ITEM_TYPE);
		nest.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		Constants.markShipComponent(nest);

		clickTarget = world.spawn(padLoc.clone().add(0.0f, 0.0f, 0.0f), Interaction.class, ic -> {
				ic.setInteractionWidth(1.0f);
				ic.setInteractionHeight(1.2f);
				ic.setResponsive(true);
				ic.setPersistent(true);
			});
		clickTarget.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.ENTITY_PAD_ITEM_TYPE);
		Constants.markShipComponent(clickTarget);
	}

	private void setPadId(UUID uuid) {
		this.padId = uuid;
		if( uuid == null )
			return;
		String id = uuid.toString();
		
		post.getPersistentDataContainer().set(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING, id);
		nest.getPersistentDataContainer().set(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING, id);
		clickTarget.getPersistentDataContainer().set(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING, id);
	}

	public UUID getPadId() {
		return this.padId;
	}
	public String getPadIdStr() {
		if( padId != null )
			return padId.toString();
		return null;
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
	static public void givePadToPlayer(Player player) {
		ItemStack itemStack = createEntityPadItemStack();
		if( itemStack != null )
			player.getInventory().addItem(itemStack);
	}

	static public boolean isEntityPad(String type) {
		return Constants.ENTITY_PAD_ITEM_TYPE.equals(type);
	}
	static public boolean isEntityPadPost(ArmorStand stand) {
		if( stand == null )
			return false;
		if( Constants.ENTITY_PAD_ITEM_TYPE.equals(stand.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}
	static public boolean isEntityPadInteraction(Interaction interaction) {
		if( interaction == null )
			return false;
		if( Constants.ENTITY_PAD_ITEM_TYPE.equals(interaction.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}
	static public boolean isEntityPadNest(BlockDisplay nest) {
		if( nest == null )
			return false;
		if( Constants.ENTITY_PAD_ITEM_TYPE.equals(nest.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}

	static public void onPlayerPlaceEntity(PlayerInteractEvent event, ItemStack itemStack) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if( block == null )
			return;
		
		if( !BlockSupport.isBlockAllowed(block.getType())) {
			SimpleShipsPlugin.log(1, player, "Entity pad must be placed on an allowed block");
			return;
		}
		if( BlockSupport.isLowerSlab(block)) {
			SimpleShipsPlugin.log(1, player, "Entity pad can not be placed on a lower slab");
			return;
		}

		if(player.getGameMode() != GameMode.CREATIVE) {
			itemStack.subtract(1);
		}
		
		EntityPad pad = new EntityPad(block.getLocation());
		EntityManager.addEntityPad(pad);
	}

	//aready know we're a right click
	static public void onInteractionClicked(Player player, Interaction interaction, PlayerInteractAtEntityEvent event) {
		String postId = interaction.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
		if( postId == null ) {
			return;
		}
			
		EntityPad pad = EntityManager.getEntityPad(postId);
		if( pad == null)
			return;

		if( player.isSneaking()) {
			pad.detachEntity();
			player.sendMessage("Entity detached");
			return;
		}

		LivingEntity nearest = EntityManager.findNearestLivingEntityNotInVehicle(player, interaction.getLocation(), 3.0);
		if( nearest == null) {
			player.sendMessage("No nearby entity found");
			return;
		}
		pad.attachEntity(nearest);
		player.sendMessage(String.format("Attached %s to pad %s", nearest.getType().getName(), postId));
	}

	static public void onEntityHit(Player player, Interaction inter, EntityDamageByEntityEvent event) {
		String standId= inter.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
		if( standId == null )
			return;
		EntityPad pad = EntityManager.getEntityPad(standId);
		if( pad != null &&  player.isSneaking()) {
			pad.removeFromWorld();
			EntityManager.removeEntityPad(pad);
			EntityPad.givePadToPlayer(player);
		}
		
	}

	public void detachEntity() {
		for(Entity passenger : post.getPassengers()) {
			post.removePassenger(passenger);
		}
	}
	private void attachEntity(LivingEntity entity) {
		SimpleShipsPlugin.log(0,"Attached entity %s to pad %s", entity.getType().getName(), padId.toString());
		detachEntity();
		post.addPassenger(entity);
	}
	

	public void move(Location loc) {
		padLoc = loc.clone();
		post.teleport(loc.clone().add(0,-V_OFFSET,0));
		nest.teleport(loc.clone());
		clickTarget.teleport(loc.clone().add(0.0f, 0.0f, 0.0f));
	}

	void removeFromWorld() {
		detachEntity();
		post.remove();
		nest.remove();
		clickTarget.remove();
	}


	static public EntityPad findPad(Interaction inter) {
		Location center = inter.getLocation();
		String id = inter.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
		if( id == null ) {
			return null;
		}
		SimpleShipsPlugin.log(0,"Found enitity pad interaction %s, looking for nest and post", id);
		
		ArmorStand thePost = null;
		BlockDisplay theNest = null;
		for(World world : Bukkit.getWorlds()) {
			for(ArmorStand stand : world.getEntitiesByClass(ArmorStand.class)) {
				if( isEntityPadPost (stand) ) {
					String standId = stand.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
					if( id.equals(standId)) {
						thePost = stand;
						break;
					}
				}
			}

			for(BlockDisplay nest : world.getEntitiesByClass(BlockDisplay.class)) {
				if(isEntityPadNest( nest)) {
					String nestId = nest.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING);
					if( id.equals(nestId)) {
						theNest = nest;
						break;
					}
				}
			}
			if(thePost != null && theNest != null )
				break;
		}
		if( thePost != null && theNest != null ) {
			return new EntityPad(inter, thePost, theNest);
		}

		if( thePost == null ) {
			SimpleShipsPlugin.log(1,"Failed to find post for entity pad %s", id);
		} else {
			SimpleShipsPlugin.log(1,"Removing orphaned post for entity pad %s", id);
			thePost.remove();
		}
		if( theNest == null ) {
			SimpleShipsPlugin.log(1, "Failed to find nest for entity pad %s", id);
		} else {
			SimpleShipsPlugin.log(1,"Removing orphaned nest for entity pad %s", id);
			theNest.remove();
		}
		SimpleShipsPlugin.log(1, "Removing orphaned interaction for entity pad %s", id);
		inter.remove();
		
		return null;
	}


	@Override
	public boolean equals(Object other) {
		if( other instanceof EntityPad op) {
			return op.padId.equals(padId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return padId.hashCode();
	}

	public Location getPadLocation(Location anchor, Vector3f localOffset, float shipYaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(shipYaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		Location padLocation = anchor.clone().add(-worldOffset.x, worldOffset.y, worldOffset.z);
		padLocation.setYaw(shipYaw);
		return padLocation;
	}
}
