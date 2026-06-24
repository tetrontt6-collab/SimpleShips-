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
import org.bukkit.entity.Parrot;
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
public class ParrotPerch {
	static public final float V_OFFSET = 0.80f;
	ArmorStand perchAnchor;
	Display perchPost;
	Display perchCrossbar;
	Location perchLoc;
	UUID perchId;
	
	ParrotPerch(Location loc) {
		this.perchLoc = loc.clone().add(0.5f,1.0f,0.5f);
		this.perchLoc.setYaw(loc.getYaw());
		createParrotPerch();
		setPerchId(UUID.randomUUID());
	}
	ParrotPerch(ArmorStand anchor, Display perchPost, Display perchCrossbar) {
		perchLoc = anchor.getLocation();  //it is not adjusted like armor stand is
		this.perchAnchor = anchor;
		this.perchPost = perchPost;
		this.perchCrossbar = perchCrossbar;
		
		
		String perchIdStr = perchAnchor.getPersistentDataContainer().get(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING);
		if(perchIdStr == null ) {
			setPerchId(UUID.randomUUID());
		} else {
			setPerchId(UUID.fromString(perchIdStr));
		}
	}

	public Location getPerchStandLocation() {
		return perchAnchor.getLocation();
	}
	private void createParrotPerch() {
		World world = perchLoc.getWorld();

		LOG(0,"PerchLoc yaw: %f", perchLoc.getYaw());
		Location anchorLoc = perchLoc.clone();
		anchorLoc.setYaw(perchLoc.getYaw());
		perchAnchor = world.spawn(anchorLoc.add(0,-V_OFFSET,0), ArmorStand.class, as -> {
				as.setSmall(false);
				as.setInvisible(true);
				as.setMarker(false);
				as.setGravity(false);
				as.setPersistent(true);
			});
		perchAnchor.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PARROT_PERCH_ITEM_TYPE);
		Constants.markShipComponent(perchAnchor);

		Matrix4f postTransform = new Matrix4f()
			.identity()
			.translate(-4 * Constants.ONE_64, 0, 0)
			.scale(0.11f, 1.0f + (4f * Constants.ONE_64), 0.11f);
		BlockData perchPostData = Bukkit.createBlockData(Material.MANGROVE_LOG);
		Location perchPostLoc = perchLoc.clone();

		perchPost = world.spawn(perchPostLoc, BlockDisplay.class, bd -> {
				bd.setBlock(perchPostData);
				bd.setPersistent(true);
				bd.setGravity(false);
				bd.setTransformationMatrix(postTransform);
		 		bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
		 		bd.setInterpolationDelay(-1);
		 		bd.setTeleportDuration(0);
			});
		perchPost.teleport(perchPostLoc);
		perchPost.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PARROT_PERCH_POST_ITEM_TYPE);
		perchPost.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		Constants.markShipComponent(perchPost);

		Matrix4f barTransform = new Matrix4f()
			.identity()
			.translate(-0.5f,1.075f,0.0f)
			.scale(1.0f, 0.1f, 0.1f);
		BlockData perchCrossbarData = Bukkit.createBlockData(Material.STRIPPED_MANGROVE_LOG);
		Location perchCrossbarLoc = perchLoc.clone();

		perchCrossbar = world.spawn(perchCrossbarLoc, BlockDisplay.class, bd -> {
				bd.setBlock(perchCrossbarData);
				bd.setPersistent(true);
				bd.setGravity(false);
				bd.setTransformationMatrix(barTransform);
		 		bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
		 		bd.setInterpolationDelay(-1);
		 		bd.setTeleportDuration(0);
			});
		perchCrossbar.teleport(perchCrossbarLoc);
		perchCrossbar.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PARROT_PERCH_CROSSBAR_ITEM_TYPE);
		perchCrossbar.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		Constants.markShipComponent(perchCrossbar);
		
	}

	private void setPerchId(UUID uuid) {
		this.perchId = uuid;
		if( uuid == null )
			return;
		String id = uuid.toString();
		
		perchAnchor.getPersistentDataContainer().set(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING, id);
		perchPost.getPersistentDataContainer().set(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING, id);
		perchCrossbar.getPersistentDataContainer().set(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING, id);
	}

	public UUID getPerchId() {
		return this.perchId;
	}
	public String getPerchIdStr() {
		if( perchId != null )
			return perchId.toString();
		return null;
	}

	static public ItemStack createParrotPerchItemStack() {
		ItemStack itemStack = ItemStack.of(Material.ARMOR_STAND);
		ItemMeta meta = itemStack.getItemMeta();

		if( meta == null ) {
			return null;
		}

		meta.displayName(Component.text(Constants.getStringFor(Constants.PARROT_PERCH_ITEM_TYPE)));
		meta.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PARROT_PERCH_ITEM_TYPE);
		itemStack.setItemMeta(meta);
		return itemStack;
	}
	static public void givePerchToPlayer(Player player) {
		ItemStack itemStack = createParrotPerchItemStack();
		if( itemStack != null )
			player.getInventory().addItem(itemStack);
	}

	static public boolean isParrotPerch(String type) {
		return Constants.PARROT_PERCH_ITEM_TYPE.equals(type);
	}
	static public boolean isParrotPerch(ArmorStand stand) {
		if( stand == null ) {
			return false;
		}
		if( Constants.PARROT_PERCH_ITEM_TYPE.equals(stand.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING))) {
			return true;
		}
		return false;
	}

	static public boolean isPerchPost(Display display) {
		if( display == null )
			return false;
		if( Constants.PARROT_PERCH_POST_ITEM_TYPE.equals(display.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}
	static public boolean isPerchCrossbar(Display display) {
		if( display == null )
			return false;
		if( Constants.PARROT_PERCH_CROSSBAR_ITEM_TYPE.equals(display.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}

	static public void onPlayerPlacePerch(PlayerInteractEvent event, ItemStack itemStack) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if( block == null )
			return;
		
		if( !BlockSupport.isBlockAllowed(block.getType())) {
			LOG(1, player, "Parrot perch must be placed on an allowed block");
			return;
		}
		if( BlockSupport.isLowerSlab(block)) {
			LOG(1, player, "Parrot perch can not be placed on a lower slab");
			return;
		}

		if(player.getGameMode() != GameMode.CREATIVE) {
			itemStack.subtract(1);
		}

		Location loc = block.getLocation().clone();
		float reversePlayerYaw = UtilFuncs.getClosestFacingYawDegrees(player.getYaw());
		LOG(0,"Placing perch, player yaw: %f, moved to %f", player.getYaw(), reversePlayerYaw);
		loc.setYaw(reversePlayerYaw);
		ParrotPerch perch = new ParrotPerch(loc);
		EntityManager.addParrotPerch(perch);
	}

	static public void onStandClicked(Player player, ArmorStand stand, PlayerInteractAtEntityEvent event) {
		String postId = stand.getPersistentDataContainer().get(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING);
		if( postId == null ) {
			return;
		}

		ParrotPerch perch = EntityManager.getParrotPerch(postId);
		if( perch == null )
			return;
		
		if( player.isSneaking()) {
			perch.detachParrot();
			player.sendMessage("Parrot detached");
			return;
		}

		Parrot parrot = EntityManager.findNearestParrotNotInVehicle(player, stand.getLocation(), 3.0);
		if( parrot == null) {
			player.sendMessage("No nearby parrot's found");
			return;
		}

		perch.attachParrot(parrot);
		player.sendMessage(String.format("Attached parrot to perch %s", postId));
	}
	
	static public void onPerchHit(Player player, ArmorStand stand, EntityDamageByEntityEvent event) {
		String standId= stand.getPersistentDataContainer().get(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING);
		LOG(0,"Perch hit");
		if( standId == null )
			return;
		ParrotPerch perch = EntityManager.getParrotPerch(standId);
		if( perch != null &&  player.isSneaking()) {
			perch.removeFromWorld();
			EntityManager.removeParrotPerch(perch);
			ParrotPerch.givePerchToPlayer(player);
		}
		
	}

	public void detachParrot() {
		for(Entity passenger : perchAnchor.getPassengers()) {
			perchAnchor.removePassenger(passenger);
		}
	}
	private void attachParrot(LivingEntity entity) {
		LOG(0,"Attached parrot %s to perch %s", entity.getType().getName(), perchId.toString());
		detachParrot();
		perchAnchor.addPassenger(entity);
	}
	

	public void move(Location loc) {
		perchLoc = loc.clone();
		perchAnchor.teleport(loc.clone());

		
		perchPost.teleport(loc.clone().add(0,V_OFFSET,0));
		perchCrossbar.teleport(loc.clone().add(0,V_OFFSET,0));
	}

	void removeFromWorld() {
		LOG(0,"Removing perch from world");
		detachParrot();
		perchPost.remove();
		perchAnchor.remove();
		perchCrossbar.remove();
	}



	@Override
	public boolean equals(Object other) {
		if( other instanceof ParrotPerch op) {
			return op.perchId.equals(perchId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return perchId.hashCode();
	}

	public Location getPerchLocation(Location anchor, Vector3f localOffset, float yawAtAssembly, float shipYaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(shipYaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		Location perchLocation = anchor.clone().add(-worldOffset.x, worldOffset.y, worldOffset.z);
		float changeInYaw = shipYaw - yawAtAssembly;

		float perchYaw = perchLocation.getYaw();
		perchYaw = UtilFuncs.wrapDegrees(perchYaw + changeInYaw);
		perchLocation.setYaw(perchYaw);
		return perchLocation;
	}

	static public ParrotPerch findParrotPerch(ArmorStand theStand) {
		Location center = theStand.getLocation();
		String id = theStand.getPersistentDataContainer().get(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING);
		if( id == null)
			return null;

		LOG(0,"Found parrot perch armor stand %s, looking for post and crossbar", id);
		BlockDisplay thePost = null;
		BlockDisplay theCrossbar = null;

		for(World world : Bukkit.getWorlds()) {
			for(BlockDisplay bd : world.getEntitiesByClass(BlockDisplay.class)) {
				String bdID = bd. getPersistentDataContainer().get(Constants.PARROT_PERCH_ID_KEY, PersistentDataType.STRING);
				if(bdID != null && id.equals(bdID)) {
					if( isPerchPost(bd)) {
						thePost = bd;
					}
					if(isPerchCrossbar(bd)) {
						theCrossbar = bd;
					}
					if( thePost != null && theCrossbar != null)
						break;
				}
			}
			if( thePost != null && theCrossbar != null)
				break;
		}

		if( thePost != null && theCrossbar != null) {
			LOG(0,"Parrot perch %s being rehydrated", id);
			return new ParrotPerch(theStand, thePost, theCrossbar);
		}

		if( thePost == null ) {
			LOG(1,"Failed to find the post for the parrot perch %s", id);
		} else {
			LOG(1,"Removing orphaned post for parrot perch %s", id);
		}

		//same for crossbar
		if( theCrossbar == null ) {
			LOG(1,"Failed to find the crossbar for the parrot perch %s", id);
		} else {
			LOG(1,"Removing orphaned crossbar for parrot perch %s", id);
		}
		
		
		LOG(1,"Removing orphaned armor stand for parrot perch %s", id);
		theStand.remove();
		return null;
	}
}
