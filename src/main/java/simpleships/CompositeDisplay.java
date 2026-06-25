package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 *
 * This provides the ability to manage a list of Block and Item Display
 * entities as a single component.  It will have an interaction as the
 * core and a single unique id and base location.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static simpleships.SimpleShipsPlugin.LOG;

/**
 * The root of a CompositeDisplay is the {@link spigot.org.bukkit.entity.Interaction}, all
 * other Display and ArmorStand entities are organized around this.
 *
 * One interesting note regarding the {@link spigot.org.bukkit.entity.Interaction} is that the location
 * always resets the yaw and pitch to 0, regardless of what is passed into it.
 *
 * Our CompositeDisplay needs to maintain the proper values for those in the default location so they
 * are being included in the persistent data of the parts (including the Interaction) so that when
 * reconsituting a ComponentDisplay from a discovered {@link spigot.org.bukkit.entity.Interaction} the
 * information is restored correctly.
 */
public class CompositeDisplay implements ICompositeDisplayHolder {
	final static double SEARCH_LIMIT_X = 5;
	final static double SEARCH_LIMIT_Y = 5;
	final static double SEARCH_LIMIT_Z = 5;
	
	final String uniqueId;
	final boolean persistent;

	boolean spawned = false;
	double interactionWidth;
	double interactionHeight;
	Location location;
	Interaction interaction;
	ArmorStandPart armorStandPart;

	List<DisplayPart> parts = new ArrayList<>();
	List<KeyData> keyData = new ArrayList<>();
	String compositeDisplayType;



	public CompositeDisplay(String compositeDisplayType, Location loc, boolean persistent, double interactionWidth, double interactionHeight) {
		this.uniqueId = UUID.randomUUID().toString();
		this.persistent = persistent;
		this.location = loc.clone();
		this.interactionWidth = interactionWidth;
		this.interactionHeight = interactionHeight;
		this.compositeDisplayType = compositeDisplayType;
	}

	private CompositeDisplay(String uniqueId, String compositeDisplayType, Location loc, Interaction interaction, List<Display> displays) {
		this.uniqueId = uniqueId;
		this.compositeDisplayType = compositeDisplayType;
		this.location = loc.clone();
		this.interactionWidth = interaction.getWidth();
		this.interactionHeight = interaction.getHeight();
		this.persistent = interaction.isPersistent();
		this.interaction = interaction;
		this.spawned = true;

		for(Display display : displays) {
			parts.add(new DisplayPart(display));
		}
	}

	public void showInfo() {
		if( interaction == null ) {
		LOG(0,"CompositeDisplay '%s': loc(%f,%f,%f) %f, inter(%f,%f,%f) %f  %s",
				compositeDisplayType,
				location.getX(), location.getY(), location.getZ(), location.getYaw(),
				0f, 0f, 0f, 0f, uniqueId);
		} else {
			Location l = interaction.getLocation();
			LOG(0,"CompositeDisplay '%s': loc(%f,%f,%f) %f, inter(%f,%f,%f) %f  %s",
					compositeDisplayType,
					location.getX(), location.getY(), location.getZ(), location.getYaw(),
					l.getX(), l.getY(), l.getZ(), l.getYaw(), uniqueId);
		}
	}


	public int getNumberOfComponents() {
		int count = 1;  //interaction

		count += (armorStandPart != null ? 1 : 0);
		count += parts.size();
		return count;
	}
	
	public String getId() {
		return this.uniqueId;
	}
	
	//distinct from the composite display id,
	//this allows locating the interaction entity
	//in the world if needed
	public String getInteractionId() {
		return this.interaction.getUniqueId().toString();
	}
	
	public Location getLocation() {
		return this.location;
	}
	public World getWorld() {
		return this.location.getWorld();
	}

	@Override
	public CompositeDisplay getCompositeDisplay() {
		return this;
	}
	
	public boolean isActive() {
		if( this.interaction == null )
			return false;

		Boolean active = this.interaction.getPersistentDataContainer().get(Constants.SS_CD_IS_ACTIVE_KEY, PersistentDataType.BOOLEAN);
		if( active == null )
			return true;
		return active;
	}

	public CompositeDisplay setActive(boolean f) {
		if( this.interaction == null ) {
			return this;
		}
		this.interaction.getPersistentDataContainer().set(Constants.SS_CD_IS_ACTIVE_KEY, PersistentDataType.BOOLEAN, f);
		return this;
	}
	public boolean isSpawned() {
		return this.spawned;
	}
	
	public CompositeDisplay spawn() {
		if( spawned )
			return this;
		
		if( this.interaction != null ) {
			remove();
		}
		
		this.interaction = this.location.getWorld().spawn(this.location.clone(), Interaction.class, it-> {
				it.setPersistent(this.persistent);
				it.setGravity(false);
				it.setInteractionHeight((float)this.interactionHeight);
				it.setInteractionWidth((float)this.interactionWidth);
				it.setResponsive(true);
			});
		addKeys(this.interaction);
		this.interaction.getPersistentDataContainer().set(Constants.SS_CD_YAW_VALUE, PersistentDataType.FLOAT, location.getYaw());
		this.interaction.getPersistentDataContainer().set(Constants.SS_CD_PITCH_VALUE, PersistentDataType.FLOAT, location.getPitch());

		for(DisplayPart part : parts ) {
			part.spawn(this.location.clone(), this.persistent);
			addKeys(part.getEntity());
		}

		if(armorStandPart != null ) {
			armorStandPart.spawn(this.location.clone(), this.persistent);
			addKeys(armorStandPart.getStand());
		}

		spawned = true;
		return this;
	}

	public CompositeDisplay addArmorStand(Vector3f offset, boolean visible, boolean small, float yaw) {
		if(this.armorStandPart != null)
			this.armorStandPart.remove();

//		LOG(0,"adding armor stand at (%f,%f,%f) %f to cd %s", offset.x, offset.y, offset.z, yaw, uniqueId);
		this.armorStandPart = new ArmorStandPart(offset, visible, small);
		return this;
	}

	public CompositeDisplay setArmorStand(ArmorStand stand) {
		if( this.armorStandPart != null )
			this.armorStandPart.remove();
		
		Location standLoc = stand.getLocation();
		Vector3f offset = new Vector3f((float)(standLoc.getX() - location.getX()),
																	 (float)(standLoc.getY() - location.getY()),
																	 (float)(standLoc.getZ() - location.getZ()));
//		LOG(0,"setting armor stand at (%f,%f,%f) %f to cd %s", offset.x, offset.y, offset.z, standLoc.getYaw(), uniqueId);
		this.armorStandPart = new ArmorStandPart(offset, stand);
		return this;
	}

	public boolean hasPassenger() {
		if( armorStandPart != null &&
				armorStandPart.getStand().getPassengers() != null &&
				armorStandPart.getStand().getPassengers().size() > 0 )
			return true;
		return false;
	}

	public Entity getPassenger() {
		if( hasPassenger() ) {
			return armorStandPart.getStand().getPassengers().get(0);
		}
		return null;
	}

	public CompositeDisplay mountPassenger(Entity entity) {
		if( armorStandPart != null && armorStandPart.getStand().isEmpty() ) {
//			LOG(0,"Mounting entity on composite display %s", uniqueId);
			entity.getPersistentDataContainer().set(Constants.SS_CD_INTERACTION_ID, PersistentDataType.STRING, getInteractionId());
			armorStandPart.getStand().addPassenger(entity);
		}
		return this;
	}

	public CompositeDisplay removePassengers() {
		if( armorStandPart != null && !armorStandPart.getStand().isEmpty()) {
			for(Entity entity : armorStandPart.getStand().getPassengers()) {
				entity.getPersistentDataContainer().remove(Constants.SS_CD_INTERACTION_ID);
			}
			armorStandPart.getStand().eject();
		}
		return this;
	}
			

	public CompositeDisplay addKey(NamespacedKey key, String val) {
		keyData.add(new KeyData(key, val));
		return this;
	}
	public CompositeDisplay addKey(NamespacedKey key, int val) {
		keyData.add(new KeyData(key, val));
		return this;
	}
	public CompositeDisplay addKey(NamespacedKey key, boolean val) {
		keyData.add(new KeyData(key, val));
		return this;
	}
	
	public CompositeDisplay addBlock(Material mat, Vector3f pos, Vector3f rot, Vector3f scale) {
		parts.add(new DisplayPart(mat, pos, rot, scale, true));
		return this;
	}
	public CompositeDisplay addBlock(Material mat, Vector3f pos, Vector3f rot, Vector3f scale, Consumer<Display> userFunc) {
		parts.add(new DisplayPart(mat, pos, rot, scale, true, userFunc));
		return this;
	}
	
	public CompositeDisplay addItem(Material mat, Vector3f pos, Vector3f rot, Vector3f scale) {
		parts.add(new DisplayPart(mat, pos, rot, scale, false));
		return this;
	}
	public CompositeDisplay addItem(Material mat, Vector3f pos, Vector3f rot, Vector3f scale, Consumer<Display> userFunc) {
		parts.add(new DisplayPart(mat, pos, rot, scale, false, userFunc));
		return this;
	}

	public void moveTo(Location loc) {
		this.location = loc.clone();
		this.location.setPitch(0);
		
		if( interaction != null ) {
			this.interaction.teleport(this.location.clone());
			
			this.interaction.getPersistentDataContainer().set(Constants.SS_CD_YAW_VALUE, PersistentDataType.FLOAT, location.getYaw());
			this.interaction.getPersistentDataContainer().set(Constants.SS_CD_PITCH_VALUE, PersistentDataType.FLOAT, location.getPitch());
		}
		for(DisplayPart display : parts ) {
			display.moveTo(this.location.clone());
		}

		if( armorStandPart != null )
			armorStandPart.moveTo(this.location.clone());
		
	}

	public void remove() {
		if( armorStandPart != null && armorStandPart.getStand() != null) {
			armorStandPart.getStand().eject();
			armorStandPart.getStand().remove();
		}
		
		if( interaction != null ) {
			this.interaction.remove();
			this.interaction = null;
		}
		if( parts != null && parts.size() != 0 ) {
			for(DisplayPart display : parts) {
				display.remove();
			}
		}
		parts.clear();
	}

	private void addKeys(Entity entity) {
		PersistentDataContainer pdc = entity.getPersistentDataContainer();
		pdc.set(Constants.SS_CD_ID_KEY, PersistentDataType.STRING, uniqueId);
		pdc.set(Constants.SS_CD_TYPE_KEY, PersistentDataType.STRING, compositeDisplayType);
		for(KeyData data : keyData) {
			data.set(pdc);
		}
			
	}

	/*
	 * This will try to reconstitute a composite display given an interaction.
	 */
	public static CompositeDisplay reconstituteFromInteraction(String compositeDisplayType, Interaction interaction) {
		if( interaction == null ) {
			return null;
		}
		
		if(!isCompositeDisplayEntity(compositeDisplayType, interaction)) {
			return null;
		}

		//ok this is part of a CD so lets find all block and item displays with the same id number
		String uniqueId = getCompositeDisplayUniqueId(interaction);
		if( uniqueId == null ) {
			return null;
		}


		Location center = interaction.getLocation();
		Float yaw = interaction.getPersistentDataContainer().get(Constants.SS_CD_YAW_VALUE, PersistentDataType.FLOAT);
		if( yaw == null ) {
			yaw = 0.0f;
		}

		Float pitch = interaction.getPersistentDataContainer().get(Constants.SS_CD_PITCH_VALUE, PersistentDataType.FLOAT);
		if( pitch == null ) {
			pitch = 0.0f;
		}
		
		center.setYaw(yaw);
		center.setPitch(pitch);

		World world = interaction.getWorld();

		

		// TODO for now all composite displays are kept withing a 1x1x1 bounding
		// region, a single block. that could change but we'll limit our search
		// for components
		BoundingBox box = new BoundingBox(center.getBlockX() - SEARCH_LIMIT_X, center.getBlockY() - SEARCH_LIMIT_Y, center.getBlockZ() - SEARCH_LIMIT_Z,
																			center.getBlockX() + SEARCH_LIMIT_X, center.getBlockY() + SEARCH_LIMIT_Y, center.getBlockZ() + SEARCH_LIMIT_Z);

		List<Display> foundDisplayEntities = new ArrayList<>();
		ArmorStand foundArmorStand = null;
		for(Entity entity : world.getNearbyEntities(box) ) {
			if( entity instanceof Display display) {
				if(isCompositeDisplayEntity(compositeDisplayType, display)) {
					String id = getCompositeDisplayUniqueId(display);
					if( uniqueId.equals(id)) {
						foundDisplayEntities.add(display);
					}
				}
			} else if(entity instanceof ArmorStand stand) {
				if( isCompositeDisplayEntity(compositeDisplayType, stand)) {
					String id = getCompositeDisplayUniqueId(stand);
					if( uniqueId.equals(id)) {
						foundArmorStand = stand;
					}
				}
			}
		}

		CompositeDisplay cd = new CompositeDisplay(uniqueId, compositeDisplayType, center, interaction, foundDisplayEntities);
		if( foundArmorStand != null )
			cd.setArmorStand(foundArmorStand);
		return cd;
	}


	static final public String getCompositeDisplayUniqueId(Entity entity) {
		return entity.getPersistentDataContainer().get(Constants.SS_CD_ID_KEY, PersistentDataType.STRING);
	}
	static final public boolean isCompositeDisplayEntity(String compositeDisplayType, Entity entity) {
		String cdType = entity.getPersistentDataContainer().get(Constants.SS_CD_TYPE_KEY, PersistentDataType.STRING);
		return compositeDisplayType.equals(cdType);
	}


	// ================ support classes ========================
	static class DisplayPart {
		private Display display;
		private Material mat;
		private Vector3f pos;
		private Vector3f rot;
		private Vector3f scale;
		private boolean isBlock;
		private Consumer<Display> userFunc;

		DisplayPart(Display display) {
			this.display = display;
			if( display instanceof BlockDisplay bd) {
				this.isBlock = true;
				this.mat = bd.getBlock().getMaterial();
			} else {
				this.mat = ((ItemDisplay)display).getItemStack().getType();
				this.isBlock = false;
			}
		}
		
		DisplayPart(Material mat, Vector3f pos, Vector3f rot, Vector3f scale, boolean isBlock) {
			this.mat = mat;
			this.pos = new Vector3f(pos);
			this.rot = new Vector3f(rot);
			this.scale = new Vector3f(scale);
			this.isBlock = isBlock;
		}

		DisplayPart(Material mat, Vector3f pos, Vector3f rot, Vector3f scale, boolean isBlock, Consumer<Display> userFunc) {
			this.mat = mat;
			this.pos = new Vector3f(pos);
			this.rot = new Vector3f(rot);
			this.scale = new Vector3f(scale);
			this.isBlock = isBlock;
			this.userFunc = userFunc;
		}
		
		Display getEntity() {
			return this.display;
		}

		void moveTo(Location loc) {
			if(this.display != null) {
				this.display.teleport(loc);
			}
		}
		void remove() {
			if(this.display != null )
				this.display.remove();
			this.display = null;
		}
		
		void spawn(Location location, boolean persistent) {
			if( this.display != null ) {
				this.display.remove();
				this.display = null;
			}
			
			if(isBlock )
				spawnBlock(location, persistent);
			else
				spawnItem(location, persistent);
//			LOG(0,"CP_DP: %s spawn at (%f,%f,%f) %f", mat.toString(), location.getX(), location.getY(),location.getZ(), location.getYaw());

		}

		private void spawnBlock(Location location, boolean persistent) {
			Matrix4f xform = new Matrix4f().identity();
			xform.rotateX((float)Math.toRadians(rot.x));
			xform.rotateY((float)Math.toRadians(rot.y));
			xform.rotateZ((float)Math.toRadians(rot.z));
			xform.translate(pos);
			xform.scale(scale);

			// LOG(0,"spawnBlock: P(%f,%f,%f), R(%f,%f,%f), S(%f,%f,%f)",
			// 		pos.x, pos.y, pos.z,
			// 		rot.x, rot.y, rot.z,
			// 		scale.x, scale.y, scale.z);

			BlockData data = Bukkit.createBlockData(mat);

			this.display = location.getWorld().spawn(location, BlockDisplay.class, bd-> {
					bd.setPersistent(persistent);
					bd.setGravity(false);
					bd.setBlock(data);
					bd.setInterpolationDelay(0);
					bd.setInterpolationDuration(2);
					bd.setTeleportDuration(2);
					bd.setTransformationMatrix(xform);
				});

			if( userFunc != null ) {
				userFunc.accept(this.display);
			}
			
		}
		private void spawnItem(Location location, boolean persistent) {
			Matrix4f xform = new Matrix4f().identity();
			xform.translate(pos);
			xform.rotateX((float)Math.toRadians(rot.x));
			xform.rotateY((float)Math.toRadians(rot.y));
			xform.rotateZ((float)Math.toRadians(rot.z));
			xform.scale(scale);

			ItemStack item = ItemStack.of(mat);

			this.display = location.getWorld().spawn(location, ItemDisplay.class, bd-> {
					bd.setPersistent(persistent);
					bd.setGravity(false);
					bd.setItemStack(item);
					bd.setInterpolationDelay(0);
					bd.setInterpolationDuration(2);
					bd.setTeleportDuration(2);
					bd.setTransformationMatrix(xform);
				});
			if( userFunc != null ) {
				userFunc.accept(this.display);
			}
		}
	}

	static class ArmorStandPart {
		private Vector3f offset;
		private boolean visible;
		private boolean small;
		private ArmorStand stand;
		
		ArmorStandPart(Vector3f offset, boolean visible, boolean small) {
			this.offset = new Vector3f(offset);
			this.visible = visible;
			this.small = small;
//			LOG(0,"Armor stand created at offset (%f,%f,%f)", offset.x, offset.y, offset.z);
		}
		ArmorStandPart(Vector3f offset, ArmorStand stand) {
			this.offset = new Vector3f(offset);
			this.visible = stand.isVisible();
			this.small = stand.isSmall();
			this.stand = stand;
//			LOG(0,"Armor stand added at offset (%f,%f,%f)", offset.x, offset.y, offset.z);
		}

		public ArmorStand getStand() {
			return this.stand;
		}

		public void spawn(Location loc, boolean persistent) {
			remove();
			Location standLoc = loc.clone().add(offset.x, offset.y, offset.z);
			//		LOG(0,"Armor stand spawned at offset (%f,%f,%f) %f", standLoc.getX(), standLoc.getY(), standLoc.getZ(), standLoc.getYaw());
			this.stand = loc.getWorld().spawn(standLoc, ArmorStand.class, as -> {
				as.setPersistent(persistent);
				as.setGravity(false);
				as.setSmall(this.small);
				as.setMarker(false);
				as.setInvisible(!this.visible);
			});
		}

		public void remove() {
			if( this.stand != null )
				this.stand.remove();
			this.stand = null;
		}

		public void moveTo(Location loc) {
			if( this.stand != null ) {
				Location standLoc = loc.clone().add(offset.x, offset.y, offset.z);
				this.stand.teleport(standLoc);
			}
		}
	}
		
		

	static class KeyData {
		private NamespacedKey key;
		private Object value;

		KeyData(NamespacedKey key, Object value) {
			this.key = key;
			this.value = value;
		}

		public void set(PersistentDataContainer pdc) {
			if( value instanceof String strVal) {
				pdc.set(key, PersistentDataType.STRING, strVal);
			} else if( value instanceof Integer intVal) {
				pdc.set(key, PersistentDataType.INTEGER, intVal);
			} else if( value instanceof Boolean boolVal) {
				pdc.set(key, PersistentDataType.BOOLEAN, boolVal);
			} else {
				throw new RuntimeException("Value of type " + value.getClass().getName() + " not supported");
			}
		}
	}
}
