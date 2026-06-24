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
	ArmorStand armorStand;
	List<DisplayPart> parts = new ArrayList<>();
	List<KeyData> keyData = new ArrayList<>();
	String compositeDisplayType;



	public CompositeDisplay(String compositeDisplayType, Location loc, boolean persistent, double interactionWidth, double interactionHeight) {
		this.uniqueId = UUID.randomUUID().toString();
		this.persistent = persistent;
		this.location = loc.clone();
		this.location.setYaw(0);
		this.location.setPitch(0);
		this.interactionWidth = interactionWidth;
		this.interactionHeight = interactionHeight;
		this.compositeDisplayType = compositeDisplayType;
	}

	private CompositeDisplay(String uniqueId, String compositeDisplayType, Interaction interaction, List<Display> displays) {
		this.uniqueId = uniqueId;
		this.compositeDisplayType = compositeDisplayType;
		this.location = interaction.getLocation().clone();
		this.interactionWidth = interaction.getWidth();
		this.interactionHeight = interaction.getHeight();
		this.persistent = interaction.isPersistent();
		this.interaction = interaction;
		this.spawned = true;


		for(Display display : displays) {
			parts.add(new DisplayPart(display));
		}
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
	
	public String getInteractionId() {
		if( interaction == null )
			return null;

		return interaction.getUniqueId().toString();
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
		
		
		this.interaction = this.location.getWorld().spawn(this.location, Interaction.class, it-> {
				it.setPersistent(this.persistent);
				it.setGravity(false);
				it.setInteractionHeight((float)this.interactionHeight);
				it.setInteractionWidth((float)this.interactionWidth);
				it.setResponsive(true);
			});
		addKeys(this.interaction);

		for(DisplayPart part : parts ) {
			part.spawn(this.location, this.persistent);
			addKeys(part.getEntity());
		}

		if(armorStand != null )
			addKeys(armorStand);

		spawned = true;
		return this;
	}

	public CompositeDisplay addArmorStand(Location loc, boolean visible, boolean small) {
		this.armorStand = loc.getWorld().spawn(loc, ArmorStand.class, as -> {
				as.setPersistent(this.persistent);
				as.setGravity(false);
				as.setSmall(small);
				as.setMarker(false);
				as.setInvisible(!visible);
			});
		return this;
	}

	public CompositeDisplay mountPassenger(Entity entity) {
		if( armorStand != null && armorStand.isEmpty() ) {
			armorStand.addPassenger(entity);
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
		this.location.setYaw(0);
		this.location.setPitch(0);
		if( interaction != null )
			this.interaction.teleport(this.location);
		for(DisplayPart display : parts ) {
			display.moveTo(this.location);
		}
		if( armorStand != null )
			armorStand.teleport(this.location);
		
	}

	public void remove() {
		if( armorStand != null ) {
			armorStand.eject();
			armorStand.remove();
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
		World world = interaction.getWorld();

		// TODO for now all composite displays are kept withing a 1x1x1 bounding
		// region, a single block. that could change but we'll limit our search
		// for components
		BoundingBox box = new BoundingBox(center.getBlockX() - SEARCH_LIMIT_X, center.getBlockY() - SEARCH_LIMIT_Y, center.getBlockZ() - SEARCH_LIMIT_Z,
																			center.getBlockX() + SEARCH_LIMIT_X, center.getBlockY() + SEARCH_LIMIT_Y, center.getBlockZ() + SEARCH_LIMIT_Z);

		List<Display> foundDisplayEntities = new ArrayList<>();
		for(Entity entity : world.getNearbyEntities(box) ) {
			if( entity instanceof Display display) {
				if(isCompositeDisplayEntity(compositeDisplayType, display)) {
					String id = getCompositeDisplayUniqueId(display);
					if( uniqueId.equals(id)) {
						foundDisplayEntities.add(display);
					}
				}
			}
		}

		CompositeDisplay cd = new CompositeDisplay(uniqueId, compositeDisplayType, interaction, foundDisplayEntities);
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
			if(this.display != null)
				this.display.teleport(loc);
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
		}

		private void spawnBlock(Location location, boolean persistent) {
			Matrix4f xform = new Matrix4f().identity();
			xform.rotateX((float)Math.toRadians(rot.x));
			xform.rotateY((float)Math.toRadians(rot.y));
			xform.rotateZ((float)Math.toRadians(rot.z));
			xform.translate(pos);
			xform.scale(scale);

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
