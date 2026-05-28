package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
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
 * A PassengerSeat allows for other players to ride on the ship.  The
 * seat is composed of an {@link:org.bukkit.entity.ArmorStand} that the player
 * will be mounted on, and two {@link:org.bukkit.entity.BlockDisplay} objects
 * that use a Mangrove stair and Yellow carpet to present as a seat.
 *
 * The player merely needs to right click the stair to mount.  Note that if the
 * player dismounts while the ship is materialized they will fall through the
 * ship
 */
public class PassengerSeat  {
	static public final float V_OFFSET = 0.0f;
	private Location seatLoc;
	private ArmorStand stand;
	private Display stair;
	private Display cushion;
	private UUID seatId;
	private UUID attachedEntityId;
	private float localSeatYaw;

	public PassengerSeat(BlockFace face, Location loc) {
		this.seatLoc = loc.clone().add(0.5f,1,0.5f);
		this.seatLoc.setYaw(UtilFuncs.getDegreesFromFace(face.getOppositeFace()));
		createSeat();
		setSeatId(UUID.randomUUID());
	}

	PassengerSeat(ArmorStand stand, Display stair, Display cushion) {
		seatLoc = stand.getLocation().clone(); 
		this.stand = stand;
		this.stair = stair;
		this.cushion = cushion;
		String stairIdStr = stand.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING);
		if(stairIdStr == null ) {
			setSeatId(UUID.randomUUID());
		} else {
			setSeatId(UUID.fromString(stairIdStr));
		}
	}

	public void setAssembleYaw(float shipYawAtAssemble) {
		localSeatYaw = UtilFuncs.wrapDegrees(seatLoc.getYaw() - shipYawAtAssemble);
	}
	public UUID getSeatId() {
		return this.seatId;
	}
	public String getSeatIdStr() {
		if( seatId != null )
			return seatId.toString();
		return null;
	}
	
	@Override
	public boolean equals(Object other) {
		if( other instanceof PassengerSeat ps) {
			return ps.seatId.equals(seatId);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return seatId.hashCode();
	}



	static public ItemStack createPassengerSeatItemStack() {
		ItemStack itemStack = ItemStack.of(Material.MANGROVE_STAIRS);
		ItemMeta meta = itemStack.getItemMeta();

		if( meta == null ) {
			return null;
		}

		meta.displayName(Component.text(Constants.getStringFor(Constants.PASSENGER_SEAT_ITEM_TYPE)));
		meta.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PASSENGER_SEAT_ITEM_TYPE);
		itemStack.setItemMeta(meta);
		return itemStack;
	}

	static public void giveSeatToPlayer(Player player) {
		ItemStack itemStack = createPassengerSeatItemStack();
		if( itemStack != null )
			player.getInventory().addItem(itemStack);
	}

	static public boolean isPassengerSeat(String type) {
		return Constants.PASSENGER_SEAT_ITEM_TYPE.equals(type);
	}
	static public boolean isPassengerSeat(ArmorStand stand) {
		if( stand == null )
			return false;
		if( Constants.PASSENGER_SEAT_ITEM_TYPE.equals(stand.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}
	static public boolean isPassengerSeat(BlockDisplay nest) {
		if( nest == null )
			return false;
		if( Constants.PASSENGER_SEAT_ITEM_TYPE.equals(nest.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}
	static public boolean isSeatCushion(BlockDisplay nest) {
		if( nest == null )
			return false;
		if( Constants.PASSENGER_SEAT_CUSHION_ITEM_TYPE.equals(nest.getPersistentDataContainer().get(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING)))
			return true;
		return false;
	}


	static public void onPlayerPlaceEntity(PlayerInteractEvent event, ItemStack itemStack) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();
		if( block == null )
			return;
		
		if( !BlockSupport.isBlockAllowed(block.getType())) {
			SimpleShipsPlugin.log(0, player, "Passenger seat must be placed on an allowed block");
			return;
		}
		if( BlockSupport.isLowerSlab(block)) {
			SimpleShipsPlugin.log(0, player, "Passenger seat can not be placed on a lower slab");
			return;
		}

		if(player.getGameMode() != GameMode.CREATIVE) {
			itemStack.subtract(1);
		}
		
		PassengerSeat passengerSeat = new PassengerSeat(player.getFacing(), block.getLocation());
		EntityManager.addPassengerSeat(passengerSeat);
	}

	//aready know we're a right click
	static public void onStandClicked(Player player, ArmorStand interaction, PlayerInteractAtEntityEvent event) {
		String postId = interaction.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING);
		if( postId == null ) {
			return;
		}
			
		PassengerSeat passengerSeat = EntityManager.getPassengerSeat(postId);
		if( passengerSeat == null)
			return;

		if( player.isSneaking()) {
			passengerSeat.detachEntity();
			player.sendMessage("Entity detached");
			return;
		}
		passengerSeat.attachEntity(player);
	}

	static public void onEntityHit(Player player, ArmorStand stand, EntityDamageByEntityEvent event) {
		String standId= stand.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING);
		if( standId == null )
			return;
		PassengerSeat passengerSeat = EntityManager.getPassengerSeat(standId);
		if( passengerSeat != null &&  player.isSneaking()) {
			passengerSeat.removeFromWorld();
			EntityManager.removePassengerSeat(passengerSeat);
			PassengerSeat.giveSeatToPlayer(player);
		}
		
	}

	private void detachEntity() {
		if( attachedEntityId == null) {
			return;
		}
		for(Entity passenger : stand.getPassengers()) {
			if(passenger.getUniqueId().equals(attachedEntityId)) {
				stand.removePassenger(passenger);
				break;
			}
		}
		attachedEntityId = null;
	}
	private void attachEntity(LivingEntity entity) {
		detachEntity();
		attachedEntityId = entity.getUniqueId();
		stand.addPassenger(entity);
		entity.setRotation(seatLoc.getYaw(),0);
	}
	

	public void move(Location loc) {
		double y = seatLoc.getY();
		seatLoc = loc.clone();
		seatLoc.setY(y);

		Location standLoc = loc.clone();
		standLoc.setY(stand.getLocation().getY());
		stand.teleport(standLoc);

		Location stairLoc = loc.clone();
		stairLoc.setY(stair.getLocation().getY());
		stair.teleport(stairLoc);

		Location cushionLoc = loc.clone();
		cushionLoc.setY(cushion.getLocation().getY());
		cushion.teleport(cushionLoc);
	}

	void removeFromWorld() {
		detachEntity();
		stand.remove();
		stair.remove();
		cushion.remove();
	}


	/*
	 * The ArmorStand, Stair and Cushion all have the PASSENGER_SEAT_ID_KEY value set and should
	 * all be the same.
	 */
	static public PassengerSeat findSeat(ArmorStand thePost) {
		Location center = thePost.getLocation();
		String id = thePost.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING);
		if( id == null ) {
			return null;
		}

		SimpleShipsPlugin.log(0,"Found passenger seat armor stand %s, looking for seat and cushion",id);
		BlockDisplay theStair = null;
		BlockDisplay theCushion = null;
		
		for(World world : Bukkit.getWorlds()) {
			for(BlockDisplay bd : world.getEntitiesByClass(BlockDisplay.class)) {
				String bdID = bd.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING);
//				SimpleShipsPlugin.log(0, "Found block %s, id %s", bd.getBlock().getMaterial(), bdID);
				if(bdID != null && id.equals(bdID)) {
					if(isPassengerSeat( bd)) {
						theStair = bd;
					}
					if(isSeatCushion(bd) ) {
						theCushion = bd;
					}
				}
				if(theStair != null && theCushion != null)
					break;
			}
			if(theStair != null && theCushion != null)
				break;
		}
		if( theStair != null && theCushion != null) {
			SimpleShipsPlugin.log(0,"Passenger seat %s being rehydrated", id);
			return new PassengerSeat(thePost, theStair, theCushion);
		}

		if( theStair == null ) {
			SimpleShipsPlugin.log(1, "Failed to find the stair for passenger seat %s", id);
		} else {
			SimpleShipsPlugin.log(1, "Removing orphaned stair for passenger seat %s", id);
			theStair.remove();
		}
		
		if( theCushion == null ) {
			SimpleShipsPlugin.log(1, "Failed to find the cushion for passenger seat %s", id);
		} else {
			SimpleShipsPlugin.log(1, "Removing orphaned cushion for passenger seat %s", id);
			theCushion.remove();
		}

		SimpleShipsPlugin.log(1, "Removing orphaned armor stand for passenger seat %s", id);
		thePost.remove();
		return null;
	}
	
	
	private void setSeatId(UUID uuid) {
		this.seatId = uuid;
		if( uuid == null )
			return;
		String id = uuid.toString();
		
		stand.getPersistentDataContainer().set(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING, id);
		stair.getPersistentDataContainer().set(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING, id);
		cushion.getPersistentDataContainer().set(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING, id);
	}

	public Location getSeatLocation(Location anchor, Vector3f localOffset, float shipYaw) {
		Quaternionf rotation = new Quaternionf().rotateY((float)Math.toRadians(shipYaw));
		Vector3f worldOffset = new Vector3f(localOffset);
		rotation.transform(worldOffset);
		
		Location seatLocation = anchor.clone().add(-worldOffset.x, worldOffset.y, worldOffset.z);
		seatLocation.setYaw(UtilFuncs.wrapDegrees(shipYaw + localSeatYaw));
		return seatLocation;
	}
	
		
	private void createSeat() {
		World world = seatLoc.getWorld();

		Matrix4f transform = new Matrix4f()
			.identity()
			.translate(-0.5f, 0.0f, -0.5f)
			.translate(0.5f, 0.0f, 0.5f)
			.rotateY((float)Math.toRadians(seatLoc.getYaw()))
			.translate(-0.5f, 0.0f, -0.5f);
							 

		Location standLoc = seatLoc.clone().add(0.0f, -0.40f, 0.0f);

		standLoc.setYaw(seatLoc.getYaw()); 
		stand = world.spawn(standLoc, ArmorStand.class, as -> {
				as.setSmall(true);
				as.setInvisible(true);
				as.setMarker(false);
				as.setGravity(false);
				as.setPersistent(true);
			});
		stand.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PASSENGER_SEAT_ITEM_TYPE);

		BlockData stairData = Bukkit.createBlockData(Material.MANGROVE_STAIRS);
		if( stairData instanceof Stairs stairs ) {
			stairs.setFacing(UtilFuncs.getCardinalFaceFromYaw(UtilFuncs.getReverseYaw(seatLoc.getYaw())));
			stairs.setHalf(Bisected.Half.BOTTOM);
			stairs.setShape(Stairs.Shape.STRAIGHT);
		}
		
		stair = world.spawn(seatLoc.clone(), BlockDisplay.class, id -> {
				id.setBlock(stairData);
		 		id.setPersistent(true);
		 		id.setGravity(false);
		 		id.setTransformationMatrix(new Matrix4f(transform));
		 		id.setInterpolationDuration(Constants.BD_LERP_DURATION);
		 		id.setInterpolationDelay(-1);
		 		id.setTeleportDuration(0);
		 	});
		stair.teleport(seatLoc);
		stair.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PASSENGER_SEAT_ITEM_TYPE);
		stair.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		Constants.markShipComponent(stair);

		Matrix4f cushionTransform = new Matrix4f()
			.identity()
			.translate(-0.5f, 0.52f, -0.5f)
			.scale(1.0f, 0.05f, 1.0f);
		
		BlockData cushionData = Bukkit.createBlockData(Material.YELLOW_CARPET);
		cushion = world.spawn(seatLoc.clone(), BlockDisplay.class, id -> {
				id.setBlock(cushionData);
		 		id.setPersistent(true);
		 		id.setGravity(false);
		 		id.setTransformationMatrix(new Matrix4f(cushionTransform));
		 		id.setInterpolationDuration(Constants.BD_LERP_DURATION);
		 		id.setInterpolationDelay(-1);
		 		id.setTeleportDuration(0);
			});
		cushion.teleport(seatLoc);
		cushion.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.PASSENGER_SEAT_CUSHION_ITEM_TYPE);
		cushion.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		Constants.markShipComponent(cushion);
				

	}
}
