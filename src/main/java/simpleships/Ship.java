package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Rotation;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Banner;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.block.Sign;
import org.bukkit.block.Skull;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Levelled;
import org.bukkit.block.data.MultipleFacing;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Light;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.BoundingBox;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

/**
 * The Ship handles all of the details around assembly,
 * creation of the display entities and movement.  The
 * Ship is auto assembled on when the player mounts the seat
 * and aligned and disassembled when the player dismounts.
 */
public class Ship {
	final UUID uniqueId;
	static final float ONE_64 = 1.0f/64.0f;
	static final float SHIP_VERTICAL_OFFSET = 0.45f;
	static final float HELM_VERTICAL_OFFSET = 0.45f;
	
	float shipYaw;
	float shipYawAtAssemble;
 
	ArmorStand helmAnchor;
	HelmSeat helmSeat;
	Player pilot;


	List<MaterializedBlock> shipBlocks = new ArrayList<>();
	List<MovingBlock> shipLights = new ArrayList<>();
	List<EntityPadHandle> entityPads = new ArrayList<>();
	List<PassengerSeatHandle> passengerSeats = new ArrayList<>();
	List<ItemFrameHandle> itemFrames = new ArrayList<>();
	List<ArmorStandHandle> armorStands = new ArrayList<>();

	boolean movingForward;
	boolean movingBackward;
	boolean autoMove;
	boolean turningLeft;
	boolean turningRight;

	boolean touchingWater;
	boolean canAssemble;

	BoundingBox shipBounds;

	public Ship(BlockFace facing, Location blockLocation) {
		this.uniqueId = UUID.randomUUID();
		SimpleShipsPlugin.log(0, "creating ship %s at (%f,%f,%f) => (%d,%d,%d)", uniqueId.toString(),
													blockLocation.getX(), blockLocation.getY(), blockLocation.getZ(),
													blockLocation.getBlockX(),blockLocation.getBlockY(),blockLocation.getBlockZ());
		this.shipYaw = UtilFuncs.wrapDegrees(UtilFuncs.getDegreesFromFace(facing));
		
		createHelmAnchor(blockLocation);
		
		//so we can identify armor stands that are helms
		Location helmLocation = helmAnchor.getLocation();
		helmAnchor.getPersistentDataContainer().set(Constants.ITEM_TYPE_KEY, PersistentDataType.STRING, Constants.SHIP_HELM_ITEM_TYPE);
		helmAnchor.getPersistentDataContainer().set(Constants.SHIP_HELM_ID_KEY, PersistentDataType.STRING, getUniqueIdStr());

		helmSeat = new HelmSeat(helmLocation.clone(), getUniqueIdStr());

	}

	public Ship(ArmorStand existingHelm) {
		String shipHelmId = existingHelm.getPersistentDataContainer().get(Constants.SHIP_HELM_ID_KEY, PersistentDataType.STRING);
		
		this.uniqueId = UUID.fromString(shipHelmId);
		this.shipYaw = UtilFuncs.getCardinalYaw(existingHelm.getLocation().getYaw());
		this.helmAnchor = existingHelm;
		helmSeat = HelmSeat.findForHelm(existingHelm.getLocation(), shipHelmId);
		if( helmSeat == null ) {
			helmSeat = new HelmSeat(existingHelm.getLocation(), shipHelmId);
		}
	}
																		

	public String getUniqueIdStr() {
		return uniqueId.toString();
	}
	
	public void removeHelm() {
		if( helmSeat != null )
			helmSeat.remove();
		helmSeat = null;
		if( helmAnchor != null )
			helmAnchor.remove();
	}

	public void forceUnmount() {
		SimpleShipsPlugin.log(0,"Force unmount called for %s", getUniqueIdStr());
		align();
		disassemble();
		pilot = null;
	}
	
	public void unmount(Player player) {
		if(player == null )
			return;
		if( pilot == null )
			return;
		
		if( player.equals(pilot) ) {
			SimpleShipsPlugin.log(0,"Unmounted player from %s", getUniqueIdStr());
			align();
			disassemble();
			pilot= null;
		}
	}

	public void mount(Player player) {
		if( pilot != null ) {
			SimpleShipsPlugin.log(0,player,"A player is already manning this vessel: %s", getUniqueIdStr());
			return;
		}
		helmAnchor.addPassenger(player);
		pilot = player;
		assemble();
		if(canAssemble)
			SimpleShipsPlugin.log(0,player, "Mounted player on %s", getUniqueIdStr());
	}

	public boolean isPlayerPilot(Player player) {
		if(pilot == null )
			return false;
		return pilot.equals(player);
	}
	public boolean isPiloted() {
		return pilot != null;
	}

	public void moveForward(boolean flag) {
		if( isPiloted() ) {
			movingForward = flag;
			if( flag ) {
				movingBackward = false;
				autoMove = false;
			}
		}
	}
	public void moveBackward(boolean flag) {
		if( isPiloted() && movingBackward != flag) {
			movingBackward = flag;
			autoMove = false;
		}
	}
	
	public void autoMove() {
		if( isPiloted() ) {
			autoMove = !autoMove;
		}
	}
	public void stopMoving() {
		movingForward = false;
		movingBackward = false;
		autoMove = false;
	}
	public void turnLeft(boolean flag) {
		if( isPiloted() )
			turningLeft = flag;
	}
	public void turnRight(boolean flag) {
		if( isPiloted() )
			turningRight = flag;
	}
	//align the ship to the nearest cardinal direction
	public void align() {
		Location loc = helmAnchor.getLocation().clone();
		this.shipYaw = UtilFuncs.getCardinalYaw(loc.getYaw());
		loc.setYaw(this.shipYaw);
		moveHelm(loc);
		doMoves();
	}

	public void doUpdate() {
		boolean moved = false;
		boolean turned = false;
		Vector3f forward = new Vector3f(0,0,0);

		Location nextLocation = helmAnchor.getLocation().clone();
		float nextYaw = this.shipYaw;
		if( movingForward || movingBackward || autoMove ) {
			//only rotating around Y as that is all that is needed for ships that
			//operate on the water, later when flying we'll have to do addtional
			//rotations around X
			Quaternionf orient = new Quaternionf().rotateY((float)Math.toRadians(nextYaw));

			forward.z = 1;
			orient.transform(forward);

			//this applies speed, adjust as appropriate.  This value provides smooth
			//movement appears a little faster than a traditional boat.
			if( movingForward || autoMove )
				forward.mul(Constants.SHIP_SPEED);
			else if (movingBackward )
				forward.mul(-Constants.SHIP_REVERSE_SPEED);

			//Minecraft space means I needed to negate the X parameter to get it
			//right.
			nextLocation.add(-forward.x, forward.y, forward.z);  
			nextLocation.setYaw(this.shipYaw);
			moved = true;
		}
		
		if( turningRight ) {
			nextYaw = UtilFuncs.wrapDegrees(nextYaw + 5);
			nextLocation.setYaw(nextYaw);
			moved = true;
			turned = true;
		}

		if( turningLeft) {
			nextYaw = UtilFuncs.wrapDegrees (nextYaw - 5);
			nextLocation.setYaw(nextYaw);
			moved = true;
			turned = true;
		}
		

		if( moved || turned) {
			if(!movingBackward && wouldShipCollide(nextLocation)) {
				stopMoving();
			} else {
				this.shipYaw = nextYaw;
				moveHelm(nextLocation);
			}
			doMoves();
		}
	}

	private void doMoves() {
		
		Location loc = helmAnchor.getLocation().clone();
		helmSeat.teleport(loc);

		for(MaterializedBlock block : shipBlocks ) {
			Display display = block.display();
			if(display != null ) {
				display.teleport(loc);
			}
		}
		moveAllComponents(loc);
	 
		World world = loc.getWorld();
		for(MovingBlock ls : shipLights ) {
			ls.remove();
			ls.moveTo(loc);
			ls.render();
		}

		for(ArmorStandHandle ash : armorStands ) {
			ash.move(loc, shipYaw);
		}
	}

	private void assemble() {
		this.shipYawAtAssemble = this.shipYaw;
		this.touchingWater = false;
		this.canAssemble = true;
		Quaternionf orient = new Quaternionf().rotateY((float)Math.toRadians(this.shipYaw));

		ArrayDeque<Location> queue = new ArrayDeque<>();
		Set<Location> visited = new HashSet<>();
		List<Location> toRemove = new ArrayList<>();
		HashSet<Vector3i> blocksInShip = new HashSet<>();

		Location startLoc = helmAnchor.getLocation().clone();
		shipBounds = BoundingBox.of(startLoc.getBlock());
		
		queue.add(startLoc);
		visited.add(startLoc);
		while(!queue.isEmpty()) {
			Location blockLoc = queue.removeFirst();
			if(!addMaterializedBlock(blockLoc, startLoc, orient)) 
				continue;
			toRemove.add(blockLoc);
			blocksInShip.add(new Vector3i(blockLoc.getBlockX(), blockLoc.getBlockY(), blockLoc.getBlockZ()));
			shipBounds = shipBounds.union(blockLoc);
			
			for( int z = -1; z <= 1; z++) {
				for(int y = -1; y <= 1; y++) {
					for(int x = -1; x <= 1; x++) {
						Location nextBlock = blockLoc.clone().add(x, y, z);
						if( visited.contains(nextBlock))
							continue;
						visited.add(nextBlock);
						queue.addLast(nextBlock);
					}
				}
			}
		}


		Configuration cfg = SimpleShipsPlugin.configuration;
		if( cfg.maxBlocks != -1 && shipBlocks.size() > cfg.maxBlocks) {
			SimpleShipsPlugin.log(1,pilot,"Number of blocks %d exceeds max blocks allowed %d", shipBlocks.size(), cfg.maxBlocks);
			canAssemble = false;
		}

		if( cfg.maxXWidth != -1 && shipBounds.getWidthX() > cfg.maxXWidth ) {
			SimpleShipsPlugin.log(1,pilot,"X width %d of the ship exceeds max allowed %d", (int)shipBounds.getWidthX(), cfg.maxXWidth);
			canAssemble = false;
		}

		if( cfg.maxZWidth != -1 && shipBounds.getWidthZ() > cfg.maxZWidth ) {
			SimpleShipsPlugin.log(1,pilot,"Z width %d of the ship exceeds max allowed %d", (int)shipBounds.getWidthZ(), cfg.maxZWidth);
			canAssemble = false;
		}

		if( cfg.maxHeight != -1 && shipBounds.getHeight() > cfg.maxHeight ) {
			SimpleShipsPlugin.log(1,pilot,"Height %d of the ship exceeds max allowed %d", (int)shipBounds.getHeight(), cfg.maxHeight);
			canAssemble = false;
		}

		if(!touchingWater ) {
			SimpleShipsPlugin.log(1,pilot,"Ships must have a connection to water");
			canAssemble = false;
		}
		
		if(!canAssemble ) {
			for(MaterializedBlock mb : shipBlocks ) {
				if(mb.display() != null ) 
					mb.display().remove();
			}
			shipBlocks.clear();
			shipLights.clear();
			itemFrames.clear();
			armorStands.clear();
			helmAnchor.removePassenger(pilot);
			pilot = null;
			return;
		}

		findAllEntitiesInBounds(blocksInShip);
		SimpleShipsPlugin.log(0,"Found %d item frames, %d armor stands", itemFrames.size(), armorStands.size());
		createItemFrameDisplays();
		
		for(Location remove : toRemove) {
			Material mat = Material.AIR;
			if( remove.getBlock().getBlockData() instanceof Waterlogged waterlogged) {
				if( waterlogged.isWaterlogged() ) {
					mat = Material.WATER;
				}
			} else if( isBlockTouchingWater(remove)) {
				mat = Material.WATER;
			}
			remove.getBlock().setType(mat, false);
		}

		pilot.sendMessage("Ship assembled with " + shipBlocks.size() + " blocks");
		SimpleShipsPlugin.log(0, pilot, "Ship assembled with %d blocks bounds (%f,%f,%f) => (%f,%f,%f)   (%f,%f,%f)",
													shipBlocks.size(),
													shipBounds.getMinX(), shipBounds.getMinY(), shipBounds.getMinZ(),
													shipBounds.getMaxX(), shipBounds.getMaxY(), shipBounds.getMaxZ(),
													shipBounds.getWidthX(), shipBounds.getHeight(), shipBounds.getWidthZ()
													);
		doMoves();
	}

	private boolean wouldShipCollide(Location nextAnchor) {
		World world = nextAnchor.getWorld();
		if(world == null )
			return true;

		Vector3f rotated = new Vector3f();
		for(MaterializedBlock mb : shipBlocks) {
			if(mb.data() == null) {
				continue;
			}

			rotated = UtilFuncs.rotateOffsetCardinal(mb.offset(), shipYawAtAssemble, shipYaw, rotated);
			int x = nextAnchor.getBlockX() + Math.round(rotated.x);
			int y = nextAnchor.getBlockY() + Math.round(rotated.y);
			int z = nextAnchor.getBlockZ() + Math.round(rotated.z);

			Block block = world.getBlockAt(x,y,z);
			if( !BlockSupport.canPassThru(block) )
				return true;
		}
		return false;
	}

	private void disassemble() {
		Location helmLoc = helmAnchor.getLocation().clone();
		World world = helmLoc.getWorld();

		removeShipLights();
		
		//we ensure the final direction is aligned to a cardinal direciton
		//for facing purposes.  that is N/S/E/W only
		float finalYaw = UtilFuncs.getCardinalYaw(helmLoc.getYaw());
		this.shipYaw = finalYaw;

		boolean isRotated = finalYaw != shipYawAtAssemble;
		
		helmLoc.setYaw(this.shipYaw);

		List<BlockAndState> restoredBlocks = new ArrayList<>();
		Vector3f rotated = new Vector3f();
		for(MaterializedBlock mb : shipBlocks ) {
			if(mb.display() != null ) 
				mb.display().remove();

			//some extra blocks provided for display purposes only
			if(mb.data() == null ) {
				continue;
			}

			//this is a support function that calculates the changes in x/y/z offsets that will need to be
			//applied to account for the ship orientation change
			rotated = UtilFuncs.rotateOffsetCardinal(mb.offset(), shipYawAtAssemble, finalYaw, rotated);
			
			Location bl = helmLoc.clone().add(rotated.x, rotated.y, rotated.z);
			Block block = bl.getBlock();
			BlockData data = mb.data().clone();
			BlockState state = mb.state();
			boolean isItem = (mb.display() instanceof ItemDisplay);
			if( isRotated ) {
				if( data instanceof Directional dir) {
					BlockFace origDir = dir.getFacing();
					BlockFace newDir = UtilFuncs.getRotatedFace(origDir, shipYawAtAssemble, finalYaw);
					dir.setFacing(newDir);
				}
				else if( data instanceof Rotatable rot) {
					BlockFace origDir = rot.getRotation();
					BlockFace newDir = null;
					newDir = UtilFuncs.getRotatedItemFace(origDir, shipYawAtAssemble, finalYaw);
							
					rot.setRotation(newDir);
				}
				if( data instanceof MultipleFacing mf) {
					rotateMultipleFacing(mf, shipYawAtAssemble, finalYaw);
				}
				
			}
			block.setBlockData(data, true);
			block.getState().update(true, true);
			restoredBlocks.add(new BlockAndState(block,state,mb.inventoryContents()));
		}

		for(BlockAndState restoredBlock : restoredBlocks) {
			Location rloc = restoredBlock.block().getLocation();
			Block block = rloc.getBlock();
			Material type = block.getType();
			BlockData refresh = block.getBlockData();
			block.setType(Material.AIR,false);
			block.getState().update(true, true);
				
			block.setBlockData(refresh, true);
			block.getState().update(true, true);

			if( restoredBlock.state() != null ) {
				BlockState state = restoredBlock.state();
				if(state instanceof Sign signState ) {
					restoreSignState(signState, block);
				}
				if( state instanceof Skull skull) {
					restoreHeadState(skull, block);
				}
				if( state instanceof Banner banner) {
					restoreBannerState(banner, block);
				}
			}

		}

		//rerunning the restore to handle inventory because double chests
		//need to be setup correctly before we do this
		for(BlockAndState restoredBlock : restoredBlocks) {
			BlockState state = restoredBlock.block().getState();
			if( state instanceof Container container ) {
				Block block = restoredBlock.block();
				boolean isChest = false;
				boolean isDoubleChest = false;
				boolean isLeftChest = false;
				if( block.getBlockData() instanceof Chest chest) {
				 	isChest = true;
				 	isDoubleChest = chest.getType() != Chest.Type.SINGLE;
				 	isLeftChest  = chest.getType() == Chest.Type.LEFT;
				}
				
				ItemStack[] ic = restoredBlock.inventoryContents();
				try {
					if( ic != null ) {
						if(isDoubleChest )
							container.getInventory().setContents(BlockSupport.cloneContents(ic));
						else
							container.getSnapshotInventory().setContents(BlockSupport.cloneContents(ic));
					}
				} catch(Exception ex) {
				}
				container.update(true,false);
			}
		}

		//ok now restoring any item frames we collected previously
		
		for(ItemFrameHandle ifh : itemFrames) {
			Vector3i rotatedOffset = UtilFuncs.rotateOffsetCardinalInt(ifh.attachedBlockOffset, shipYawAtAssemble, finalYaw);
			Block attachedBlock = helmLoc.clone().add(rotatedOffset.x, rotatedOffset.y, rotatedOffset.z).getBlock();
			
			BlockFace restoredFacing = ifh.facing;

			if(isRotated) {
				restoredFacing = UtilFuncs.rotateFrameFace(ifh.facing, shipYawAtAssemble, finalYaw);
			}

			float deltaY = 0;
			if(ifh.facing == BlockFace.UP || ifh.facing == BlockFace.DOWN ) {
				deltaY = 0;
			} else {
				deltaY = 1.0f;
			}

			Location spawnLoc = UtilFuncs.getItemFrameSpawnLocation(attachedBlock, restoredFacing); //attachedBlock.getRelative(restoredFacing).getLocation().clone().add(0.5,deltaY,0.5f);

			ItemFrame frame = null;
			if( ifh.isGlowItemFrame)
				frame = attachedBlock.getWorld().spawn(spawnLoc, GlowItemFrame.class);
			else
				frame = attachedBlock.getWorld().spawn(spawnLoc, ItemFrame.class);
			frame.setFacingDirection(restoredFacing, true);
			frame.setItem(ifh.itemStack == null ? ItemStack.empty() : ifh.itemStack.clone());
			frame.setRotation(UtilFuncs.rotateItemFrameRotation(ifh.rotation, ifh.facing, restoredFacing, shipYawAtAssemble, finalYaw));
			frame.setFixed(ifh.fixed);
			frame.setVisible(ifh.visible);
		}

		for(ArmorStandHandle ash : armorStands) {
			ash.restore(helmLoc, finalYaw);
		}
		
	
		Block block = helmAnchor.getLocation().getBlock();
		
		helmLoc = block.getLocation().clone();
		helmLoc.add(0.5f, HELM_VERTICAL_OFFSET, 0.5f);
		helmLoc.setYaw(this.shipYaw);

		moveHelm(helmLoc);
		if( pilot != null )
			helmAnchor.removePassenger(pilot);
		
		stopMoving();
		moveAllComponents(helmLoc);

		//remove the captured blocks.  this means that next
		//mount could pick up more blocks, but that is a
		//chosen tradeoff to avoid complexity
		shipBlocks.clear();
		shipLights.clear();
		entityPads.clear();
		passengerSeats.clear();
		itemFrames.clear();
	}

	private void moveAllComponents(Location loc) {
		for(EntityPadHandle eph : entityPads) {
			Location componentLocation = eph.pad.getPadLocation(loc, eph.offset, this.shipYaw);
			eph.move(componentLocation);
		}
		for(PassengerSeatHandle psh : passengerSeats) {
			Location componentLocation = psh.seat.getSeatLocation(loc, psh.offset, this.shipYaw);
			psh.move(componentLocation);
		}
	}
	
	private boolean addMaterializedBlock(Location blockLoc, Location startLoc, Quaternionf orient) {
		World world = blockLoc.getWorld();
		Block block = blockLoc.getBlock();
		BlockState state = block.getState();
		Material type = block.getType();
		Vector3f offset = new Vector3f(blockLoc.getBlockX() - startLoc.getBlockX(),
																	 blockLoc.getBlockY() - startLoc.getBlockY(),
																	 blockLoc.getBlockZ() - startLoc.getBlockZ());
		

		if( Material.ITEM_FRAME.equals(type)) {
			SimpleShipsPlugin.log(0,"Item frame is the block");
		}
		
		if(!BlockSupport.isBlockAllowed(type)) {
			if( type == Material.WATER ) {
				touchingWater = true;
			}
			return false;
		}
		if( Material.ITEM_FRAME.equals(type)) {
			SimpleShipsPlugin.log(0,"Found an item frame");
		}

		ItemStack[] inventoryContents = null;
		BlockData displayData = block.getBlockData().clone();

		if( block.getState() instanceof Container container) {
			inventoryContents = BlockSupport.cloneContents(container.getInventory().getContents());
			SimpleShipsPlugin.log(0,"Found a container");
		}
		
		if( displayData instanceof Chest chest ) {
			SimpleShipsPlugin.log(0,"Found a chest");
			if( chest.getType() == Chest.Type.SINGLE ) {
				addDoubleBlockDisplay(world, block, displayData, startLoc, offset, 1.0f, inventoryContents);
			}
			else if( chest.getType() == Chest.Type.RIGHT ) {
				addDoubleBlockDisplay(world, block, displayData, startLoc, offset, 2.1f, inventoryContents);
			}
			else {
				//no display block for left side of chest but do need to restore it
				shipBlocks.add(new MaterializedBlock(null, displayData, state, offset, inventoryContents));
			}
			return true;
		}
		if( displayData instanceof Bed bed) {
			if( bed.getPart() == Bed.Part.FOOT ) {
				shipBlocks.add(new MaterializedBlock(null, displayData, null, offset, inventoryContents));
			} else {
				addDoubleBlockDisplay(world, block, displayData, startLoc, offset, 1.0f, inventoryContents);
			}
			return true;
		}
		
		if( Tag.ALL_SIGNS.isTagged(type)) {
			//ALL_HANGING_SIGNS doesn't compile though it is in the docs
			//if(Tag.ALL_HANGING_SIGNS.isTaggedType(type))
			if(Tag.CEILING_HANGING_SIGNS.isTagged(type) || Tag.WALL_HANGING_SIGNS.isTagged(type))
				addHangingSignDisplay(world, block, displayData, startLoc, offset, orient);
			else
				addSignDisplay(world, block, displayData, startLoc, offset);
		 	return true;
		}
		
		if( Tag.BANNERS.isTagged(type)) {
		 	addBannerDisplay(world, block, displayData, startLoc, offset);
		 	return true;
		}
		if( BlockSupport.isHead(type)) {
			addPlayerHeadDisplay(world, block, displayData, startLoc, offset, orient);
			return true;
		}

		if( Material.ITEM_FRAME.equals(type)) {
			SimpleShipsPlugin.log(0,"Item frame is handled default");
		}

		BlockDisplay display = null;
		Matrix4f trans = new Matrix4f().identity().rotate(orient).translate(offset.x-0.5f, offset.y - SHIP_VERTICAL_OFFSET, offset.z - 0.5f);
		display = world.spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
				bd.setBlock(displayData);
				bd.setPersistent(false);
				bd.setTransformationMatrix(trans);
				bd.setGravity(false);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
			});
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state, offset, inventoryContents);
		shipBlocks.add(mb);

		int lightLevel = displayData.getLightEmission();
		if(lightLevel > 0 ) {
			Matrix4f lightTrans = new Matrix4f().identity().translate(offset.x, offset.y, offset.z);
			
			BlockData lightData = Bukkit.createBlockData(Material.LIGHT);
			if( lightData instanceof Light light ) {
				if( lightData instanceof Levelled levelled ) {
					levelled.setLevel(lightLevel);
				}
			}
			MovingBlock ls = new MovingBlock(helmAnchor.getLocation(), offset, lightData, Material.LIGHT);
			shipLights.add(ls);
		}
		return true;
	}
	/*
	 * Create item display for ItemFrames, ArmorStands, Paintings
	 */
	private void createItemFrameDisplays() {
		//item frames
		for(ItemFrameHandle ifh : itemFrames) {
			MaterializedBlock mb = null;
			if(ifh.attachedFace == BlockFace.UP || ifh.attachedFace == BlockFace.DOWN ) {
			 	createUpDownItemFrame(ifh);
			} else {
				createCardinalItemFrame(ifh);
			}
		}
	}

	private void createUpDownItemFrame(ItemFrameHandle ifh) {
		Vector3f scale = new Vector3f(48 * ONE_64, 48 * ONE_64, 5 * ONE_64); //0.75f, 0.05f, 0.75f);
		boolean isUp = ifh.attachedFace.getOppositeFace() == BlockFace.UP;
		Vector3f attachOffset = null;
		MaterializedBlock mb = null;


		if(ifh.visible) {
			//frame
			attachOffset = new Vector3f(-24 * ONE_64, 0, -24 * ONE_64);

			Matrix4f frameXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset);
		
			Display theFrame = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
					bd.setBlock(Bukkit.createBlockData(Material.BIRCH_PLANKS));
					bd.setPersistent(false);
					bd.setGravity(false);
					bd.setTransformationMatrix(frameXform);
					bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
					bd.setInterpolationDelay(-1);
					bd.setTeleportDuration(0);
				});
			theFrame.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
			mb = new MaterializedBlock(theFrame, null, null, ifh.frameOffset, null);
			shipBlocks.add(mb);

			///background
			scale = new Vector3f(40 * ONE_64, 40 * ONE_64, 5 * ONE_64);    //0.06125
			attachOffset = new Vector3f(-20 * ONE_64, isUp ? ONE_64/4.0f : ONE_64/-4.0f, -20 * ONE_64);
			Matrix4f backgroundXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset);
		
			Display theBackground = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
					bd.setBlock(Bukkit.createBlockData(Material.TERRACOTTA));
					bd.setPersistent(false);
					bd.setGravity(false);
					bd.setTransformationMatrix(backgroundXform);
					bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
					bd.setInterpolationDelay(-1);
					bd.setTeleportDuration(0);
				});
			theBackground.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
			mb = new MaterializedBlock(theBackground, null, null, ifh.frameOffset, null);
			shipBlocks.add(mb);

		}		

		if( ifh.itemStack == null || ifh.itemStack.getType().isAir()) {
			return;
		}

		scale = new Vector3f(34*ONE_64,34*ONE_64, 34*ONE_64);
		attachOffset = new Vector3f(0, isUp ? ONE_64 : -5 * ONE_64, 0);
		Matrix4f itemXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset, ifh.rotation);
		Display theContents = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), ItemDisplay.class, bd -> {
				bd.setItemStack(ifh.itemStack.clone());
				bd.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(itemXform);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		theContents.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		mb = new MaterializedBlock(theContents, null, null, ifh.frameOffset, null);
		shipBlocks.add(mb);
		
	}

	private void createCardinalItemFrame(ItemFrameHandle ifh) {
		Vector3f scale = null;
		Vector3f wallOffset = null;
		Vector3f attachOffset = null;
		MaterializedBlock mb = null;
		
		wallOffset = UtilFuncs.getWallOffset(0.0f, ifh.attachedFace.getOppositeFace());
			
		if(ifh.visible == true ) {
			//render the fake frame
			scale = new Vector3f(48 * ONE_64, 48 * ONE_64, 4 * ONE_64); //0.05f);
				
			attachOffset = new Vector3f(wallOffset.x == 0 ? wallOffset.z * (-24 * ONE_64) : wallOffset.x > 0 ? (-3 * ONE_64) : (3 * ONE_64),   //-0.025f : 0.025f,
																	wallOffset.y - (27 * ONE_64),
																	wallOffset.z == 0 ? wallOffset.x * (24 * ONE_64) : wallOffset.z > 0 ? (-3 * ONE_64) : (3 * ONE_64));   //-0.025f : 0.025f);
			Matrix4f frameXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset);
				
			Display theFrame = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
					bd.setBlock(Bukkit.createBlockData(Material.BIRCH_PLANKS));
					bd.setPersistent(false);
					bd.setGravity(false);
					bd.setTransformationMatrix(frameXform);
					bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
					bd.setInterpolationDelay(-1);
					bd.setTeleportDuration(0);
				});
			theFrame.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
			mb = new MaterializedBlock(theFrame, null, null, ifh.frameOffset, null);
			shipBlocks.add(mb);
				
			
			scale = new Vector3f(40 * ONE_64, 40 * ONE_64, 4 * ONE_64);
			attachOffset = new Vector3f(wallOffset.x == 0 ? wallOffset.z * (-20 * ONE_64) : wallOffset.x > 0 ? (-2.95f * ONE_64) : (2.95f * ONE_64),
																	wallOffset.y - (23 * ONE_64),
																	wallOffset.z == 0 ? wallOffset.x * (20 * ONE_64) : wallOffset.z > 0 ? (-2.95f * ONE_64) : (2.95f * ONE_64));
			Matrix4f backgroundXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset);
				
			Display theBackground = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
					bd.setBlock(Bukkit.createBlockData(Material.TERRACOTTA));
			 		bd.setPersistent(false);
			 		bd.setGravity(false);
			 		bd.setTransformationMatrix(backgroundXform);
			 		bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
			 		bd.setInterpolationDelay(-1);
			 		bd.setTeleportDuration(0);
			 	});
			theBackground.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
			mb = new MaterializedBlock(theBackground, null, null, ifh.frameOffset, null);
			shipBlocks.add(mb);
			
		}
			 
		if( ifh.itemStack == null || ifh.itemStack.getType().isAir()) {
			return;
		}

		scale = new Vector3f(34*ONE_64,34*ONE_64, 34*ONE_64);
		attachOffset = new Vector3f(wallOffset.x == 0 ? 0 : wallOffset.x > 0 ? (2 * ONE_64) : (-2 * ONE_64),
																wallOffset.y - (2.0f * ONE_64),
																wallOffset.z == 0 ? 0 : wallOffset.z > 0 ? (2 * ONE_64) : (-2 * ONE_64));
		Matrix4f itemXform = UtilFuncs.createCustomTransformItem(ifh.frameOffset, ifh.attachedFace.getOppositeFace(), shipYaw, scale, attachOffset, ifh.rotation);
		Display theContents = ifh.loc.getWorld().spawn(helmAnchor.getLocation().clone(), ItemDisplay.class, bd -> {
				bd.setItemStack(ifh.itemStack.clone());
				bd.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(itemXform);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		theContents.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		mb = new MaterializedBlock(theContents, null, null, ifh.frameOffset, null);
		shipBlocks.add(mb);

	}


	/*
	 * Signs have 4 states - Hanging, Wall Hanging, Wall and Standing.
	 *
	 * Wall signs need to know the block they are attached to
	 * but need a custom display model because the default for a sign
	 * has the stick.  Wall signs are Directional indicating the block
	 * the are connected to.
	 *
	 * Standing signs and hanging signs use the Rotatable interface to determine
	 * their face for rotation purposes.
	 *
	 * Wall and Wall Hanging use Directional.
	 */
	private void addHangingSignDisplay(World world, Block block, BlockData displayData, Location startLoc, Vector3f offset, Quaternionf orient) {
		BlockFace facing = null;  
		Material mat = block.getType();

		Display display = null;
		Matrix4f rotation = null;
		BlockState state = block.getState();

		if( Tag.WALL_HANGING_SIGNS.isTagged(mat)) {
			facing = ((Directional)displayData).getFacing();
			
			BlockFace connectedFace = facing;
			rotation = UtilFuncs.createCustomTransformItem(offset, connectedFace, shipYaw, 1, new Vector3f(0,0,0));
			rotation.scale(1.125f, 1.125f, 1.125f);
		} else {
			facing = ((Rotatable)displayData).getRotation();
			rotation = UtilFuncs.createCustomTransformItem(offset, facing, shipYaw, 1, null);			
			rotation.scale(1.125f, 1.125f, 1.125f);
		}

		Matrix4f trans = new Matrix4f(rotation);
		float facingYaw = UtilFuncs.getYawDegrees(facing);
		Collection<ItemStack> items = block.getDrops();
		ItemStack itemStack = items != null && items.size() > 0 ? items.iterator().next().clone() : ItemStack.of(mat);
		display = world.spawn(helmAnchor.getLocation().clone(), ItemDisplay.class, bd -> {
				bd.setItemStack(itemStack);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(trans);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		display.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state==null?null:state.copy(), offset, null);
		shipBlocks.add(mb);
	}

	private void addSignDisplay(World world, Block block, BlockData originalDisplayData, Location startLoc, Vector3f offset) {
		BlockFace facing = null;  //only wall banners are Directional, standing are Rotatable
		Material mat = block.getType();

		BlockData displayData = originalDisplayData;
		BlockDisplay display = null;
		BlockState state = block.getState();
		
		Matrix4f rotation = null;

		boolean asBlockDisplay = false;

	
		if( Tag.WALL_SIGNS.isTagged(mat) ) {
			facing = ((Directional)displayData).getFacing();
			BlockFace connectedFace = facing.getOppositeFace();
			Block connectedTo = block.getRelative(connectedFace);
			rotation = UtilFuncs.createCustomTransform(offset, facing, shipYaw, 1, new Vector3f(0, 0.325f, 0));
			Material plankType = BlockSupport.getPlankForSign(mat);
			displayData = plankType.createBlockData();
			originalDisplayData.copyTo(displayData);
			rotation = rotation.scale(1.0f, 0.5f, .10f);
		} else {
			facing = ((Rotatable)displayData).getRotation();
			rotation = UtilFuncs.createCustomTransform(offset, facing, shipYaw, 1, null);
		}

		Matrix4f trans = new Matrix4f(rotation);
		display = world.spawn(helmAnchor.getLocation().clone(),BlockDisplay.class);
		display.setBlock(displayData);
		display.setPersistent(false);
		display.setGravity(false);
		display.setInterpolationDuration(Constants.BD_LERP_DURATION);
		display.setInterpolationDelay(-1);
		display.setTeleportDuration(0);
		display.setTransformationMatrix(rotation);

		display.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state==null?null:state.copy(), offset, null);
		shipBlocks.add(mb);
	}

	private void addPlayerHeadDisplay(World world, Block block, BlockData displayData, Location startLoc, Vector3f offset, Quaternionf orient) {
		BlockFace facing = null;  
		Material mat = block.getType();

		Display display = null;
		Matrix4f rotation = null;
		BlockState state = block.getState();

		if(BlockSupport.isWallHead(mat) ) {
			facing = ((Directional)displayData).getFacing();
			
			BlockFace connectedFace = facing.getOppositeFace();
			Block connectedTo = block.getRelative(connectedFace);
			Vector3f woff = UtilFuncs.getWallOffset(0.25f, connectedFace);
			woff.mul(new Vector3f(0.25f, 1.0f, 0.25f));
			rotation = UtilFuncs.createCustomTransformItem(offset, connectedFace, shipYaw, 1, woff);
		} else {
			facing = ((Rotatable)displayData).getRotation();
			rotation = UtilFuncs.createCustomTransformItem(offset, facing, shipYaw, 1, null);			
		}

		Matrix4f trans = new Matrix4f(rotation);
		float facingYaw = UtilFuncs.getYawDegrees(facing);
		Collection<ItemStack> items = block.getDrops();
		ItemStack itemStack = items != null && items.size() > 0 ? items.iterator().next().clone() : ItemStack.of(mat);
		display = world.spawn(helmAnchor.getLocation().clone(), ItemDisplay.class, bd -> {
				bd.setItemStack(itemStack);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(trans);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		display.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state==null?null:state.copy(), offset, null);
		shipBlocks.add(mb);
	}

	/**
	 * Banners have 2 states - wall and standing.  As with other similar blocks,
	 * the wall version uses the Directional interface to identify the block they
	 * are connected to.  The standing version uses the rotational interface
	 * to determine the orientation.
	 *
	 * The wall versions have a specific height location depending on type of block.
	 */
	private void addBannerDisplay(World world, Block block, BlockData displayData, Location startLoc, Vector3f offset) {
		BlockFace facing = null;  //only wall banners are Directional
		Material mat = block.getType();

		Display display = null;
		Matrix4f rotation = null;

		BlockState state = block.getState();

		
		if(BlockSupport.isWallBanner(mat) ) {
			facing = ((Directional)displayData).getFacing();
			BlockFace connectedFace = facing.getOppositeFace();
			Block connectedTo = block.getRelative(connectedFace);
			Vector3f woff = UtilFuncs.getWallOffset(-1.0f, connectedFace);
			woff = woff.mul(new Vector3f(0.4375f, 1.0f, 0.4375f));
			rotation = UtilFuncs.createCustomTransformItem(offset, connectedFace, shipYaw, 1, woff);
		} else {
			facing = ((Rotatable)displayData).getRotation();
			rotation = UtilFuncs.createCustomTransformItem(offset, facing.getOppositeFace(), shipYaw, 1, null);			
		}

		Collection<ItemStack> items = block.getDrops();
		ItemStack itemStack = items != null && items.size() > 0 ? items.iterator().next().clone() : ItemStack.of(mat);
		
		Matrix4f trans = new Matrix4f(rotation);
		float facingYaw = UtilFuncs.getYawDegrees(facing);
		display = world.spawn(helmAnchor.getLocation().clone(), ItemDisplay.class, bd -> {
				bd.setItemStack(itemStack);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(trans);
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		display.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state==null?null:state.copy(), offset, null);
		shipBlocks.add(mb);
	}

	/**
	 * Multi structure blocks like beds and chests require custom transformation due
	 * to the fact that as a BlockDisplay only one side of the double block is rendered.
	 * For beds, that only requires the head but the BlockDisplay will look like a full bed.
	 * For chests, the standard BlockDisplay would look like a single chest, so to get
	 * the double appearance we scale that one chest by 2 in the X direction.  That
	 * looks close enough
	 */
	private void addDoubleBlockDisplay(World world, Block block, BlockData displayData, Location startLoc, Vector3f offset, float scale, ItemStack[] inventoryContents) {
		BlockFace facing = ((Directional)displayData).getFacing();
		BlockDisplay display = null;
		BlockState state = block.getState();

		float facingYaw = UtilFuncs.getYawDegrees(facing);
		display = world.spawn(helmAnchor.getLocation().clone(), BlockDisplay.class, bd -> {
				bd.setBlock(displayData);
				bd.setPersistent(false);
				bd.setGravity(false);
				bd.setTransformationMatrix(UtilFuncs.createCustomTransform(offset, facing, shipYaw, scale, null));
				bd.setInterpolationDuration(Constants.BD_LERP_DURATION);
				bd.setInterpolationDelay(-1);
				bd.setTeleportDuration(0);
			});
		display.setTeleportDuration(Constants.BD_TELEPORT_DURATION);
		MaterializedBlock mb = new MaterializedBlock(display, block.getBlockData(), state==null?null:state.copy(), offset, inventoryContents);
		shipBlocks.add(mb);
	}


	//For the helmAnchor we're using an armor stand as it
	//will allow the player to be a passenger.
	//After some trial and error I determined the
	//correct offset to place the helmAnchor so when the player
	//is mounted he is sitting on the seat.
	private void createHelmAnchor(Location blockLocation) {
		Quaternionf orient = new Quaternionf().rotateY((float)Math.toRadians(blockLocation.getYaw()));

		World world = blockLocation.getWorld();
		Location helmLocation = new Location(world,
																				 blockLocation.getX() + 0.5f,
																				 blockLocation.getY() + HELM_VERTICAL_OFFSET,
																				 blockLocation.getZ() + 0.5f);

		helmLocation.setYaw(this.shipYaw);
		helmAnchor = world.spawn(helmLocation, ArmorStand.class, as -> {
				as.setSmall(true);
				as.setInvisible(true);
				as.setMarker(false);
				as.setGravity(false);
				as.setPersistent(true);
			});

	}

	private void rotateMultipleFacing(MultipleFacing mf, float fromYaw, float toYaw) {
		Set<BlockFace> originalFaces = new HashSet<>();
		for(BlockFace face : mf.getAllowedFaces()) {
			if(mf.hasFace(face)) {
				originalFaces.add(face);
			}
		}

		for(BlockFace face : mf.getAllowedFaces()) {
			if(UtilFuncs.isHorizontalFace(face)) {
				mf.setFace(face, false);
			}
		}

		for(BlockFace face : originalFaces ) {
			if(UtilFuncs.isHorizontalFace(face) ) {
				BlockFace rotated = UtilFuncs.getRotatedFace(face, fromYaw, toYaw);
				if(mf.getAllowedFaces().contains(rotated) ) {
					mf.setFace(rotated, true);
				}
			} else {
				mf.setFace(face, true);
			}
		}
	}



	private void restoreBannerState(Banner origState, Block block) {
		if( block.getState() instanceof Banner restored) {
			if( origState.getBaseColor() != null )
				restored.setBaseColor(origState.getBaseColor());
			int num = origState.numberOfPatterns();
			if( num > 0 ) {
				restored.setPatterns(origState.getPatterns());
			}
			restored.update(true, false);
		}
	}
	private void restoreHeadState(Skull origState, Block block) {
		if( block.getState() instanceof Skull restored ) {
			if( origState.getNoteBlockSound() != null )
				restored.setNoteBlockSound(origState.getNoteBlockSound());
			if( origState.getOwnerProfile() != null ) {
				restored.setOwnerProfile(origState.getOwnerProfile());
			}
			restored.update(true, false);
		}
	}
	private void restoreSignState(Sign origState, Block block) {
		if( block.getState() instanceof Sign restored) {
			SignSide rfront = restored.getSide(Side.FRONT);
			SignSide rback = restored.getSide(Side.BACK);

			SignSide ofront = origState.getSide(Side.FRONT);
			SignSide oback  = origState.getSide(Side.BACK);

			for(int i = 0; i < 4; i++) {
				rfront.setLine(i, ofront.getLine(i));
				rback.setLine(i, oback.getLine(i));
			}
			rfront.setGlowingText(ofront.isGlowingText());
			rback.setGlowingText(oback.isGlowingText());

			rfront.setColor(ofront.getColor());
			rback.setColor(oback.getColor());
			restored.setWaxed(origState.isWaxed());
			restored.update(true, false);
		}
		
	}

	private void moveHelm(Location loc) {
		helmAnchor.teleport(loc);
		helmSeat.teleport(loc);
	}

	private boolean isBlockTouchingWater(Location loc) {
		//only on the cardinals, not up or down
		Location west = loc.clone().add(-1,0,0);
		Location east = loc.clone().add(1,0,0);
		Location north = loc.clone().add(0,0,-1);
		Location south = loc.clone().add(0,0,1);
		if( Material.WATER == west.getBlock().getType() ||
				Material.WATER == east.getBlock().getType() ||
				Material.WATER == north.getBlock().getType() ||
				Material.WATER == south.getBlock().getType())
			return true;
		return false;
	}


	private boolean isHelmStand(ArmorStand stand) {
		return stand.getUniqueId().equals(helmAnchor.getUniqueId());
	}
	private void captureEntityPad(Location standLoc, ArmorStand stand) {
		SimpleShipsPlugin.log(0,"Found enitity pad at (%f,%f,%f)", standLoc.getX(), standLoc.getY(), standLoc.getZ());
		EntityPad pad = EntityManager.getEntityPad(stand.getPersistentDataContainer().get(Constants.ENTITY_PAD_ID_KEY, PersistentDataType.STRING));
		if( pad != null ) {
			Vector3f worldOffset = new Vector3f((float)-(standLoc.getX() - helmAnchor.getX()),
																					(float)((standLoc.getY() + EntityPad.V_OFFSET) - helmAnchor.getY()),
																					(float)(standLoc.getZ() - helmAnchor.getZ()));
			Quaternionf inverseShipRotation = new Quaternionf().rotateY((float)Math.toRadians(-shipYawAtAssemble));
			Vector3f localOffset = new Vector3f(worldOffset);
			inverseShipRotation.transform(localOffset);
			entityPads.add(new EntityPadHandle(pad, localOffset));
		}
	}
	private void capturePassengerSeat(Location standLoc, ArmorStand stand) {
		SimpleShipsPlugin.log(0,"Found passenger seat at (%f,%f,%f)", standLoc.getX(), standLoc.getY(), standLoc.getZ());
		PassengerSeat pseat = EntityManager.getPassengerSeat(stand.getPersistentDataContainer().get(Constants.PASSENGER_SEAT_ID_KEY, PersistentDataType.STRING));
		if( pseat != null ) {
			Vector3f worldOffset = new Vector3f((float)-(standLoc.getX() - helmAnchor.getX()),
																					(float)((standLoc.getY() + PassengerSeat.V_OFFSET) - helmAnchor.getY()),
																					(float)(standLoc.getZ() - helmAnchor.getZ()));
			Quaternionf inverseShipRotation = new Quaternionf().rotateY((float)Math.toRadians(-shipYawAtAssemble));
			Vector3f localOffset = new Vector3f(worldOffset);
			inverseShipRotation.transform(localOffset);
			pseat.setAssembleYaw(shipYawAtAssemble);
			passengerSeats.add(new PassengerSeatHandle(pseat, localOffset));
		}
	}
	private void findAllEntitiesInBounds(HashSet<Vector3i> blocksInShip) {
		Location anchor = helmAnchor.getLocation();
		BoundingBox largerBox = shipBounds.clone().expand(1.0);
		Vector3i blockLoc = new Vector3i();
		Quaternionf inverseShipRotation = new Quaternionf().rotateY((float)Math.toRadians(-shipYawAtAssemble));
		
		for(Entity entity : anchor.getWorld().getNearbyEntities(largerBox)) {
			Location eloc = entity.getLocation().clone();
			if(!largerBox.contains(eloc.getX(), eloc.getY(), eloc.getZ()))
				continue;
			
			Vector3f worldOffset = new Vector3f((float)(eloc.getX() - anchor.getX()),
																		 (float)(eloc.getY() - anchor.getY()),
																		 (float)(eloc.getZ() - anchor.getZ()));
			if( (entity instanceof BlockDisplay) ||
					(entity instanceof ItemDisplay)) {
				//the materialized blocks already exist so they all get found.  for the
				//ship parts we monitor for ArmorStands to identify them as they all
				//have an armor stand
				continue;
			}
			if( entity instanceof ArmorStand stand) {
				SimpleShipsPlugin.log(0,"Entity is an armor stand");
				if(isHelmStand(stand) )
					continue;
				if(EntityPad.isEntityPadPost(stand)) {
					captureEntityPad(eloc,stand);
					continue;
				}
				if(PassengerSeat.isPassengerSeat(stand)) {
					capturePassengerSeat(eloc, stand);
					continue;
				}
				armorStands.add(new ArmorStandHandle(stand, anchor, shipYawAtAssemble));
			} else 	if(entity instanceof ItemFrame frame) {

				BlockFace attachedFace = frame.getAttachedFace();
				Block attachedBlock = frame.getLocation().getBlock().getRelative(attachedFace);
				Location attachedLoc = attachedBlock.getLocation();
				blockLoc.set(attachedLoc.getBlockX(), attachedLoc.getBlockY(), attachedLoc.getBlockZ());
															
				
				if(BlockSupport.isBlockAllowed(attachedBlock) && blocksInShip.contains(blockLoc)) {
					itemFrames.add(new ItemFrameHandle(anchor, eloc, worldOffset, frame));
					frame.remove();
				}
			}
		}
	}
	
	private void removeShipLights() {
		for(MovingBlock ls : shipLights ) {
			ls.remove();
		}
	}

	
	record BlockAndState(Block block, BlockState state, ItemStack[] inventoryContents) { }



	


	
	
	class EntityPadHandle {
		final EntityPad pad;
		final Vector3f offset;

		EntityPadHandle(EntityPad pad, Vector3f offset) {
			this.pad = pad;
			this.offset = new Vector3f(offset);
		}

		void move(Location loc) {
			pad.move(loc);
		}
	}

	class PassengerSeatHandle {
		final PassengerSeat seat;
		final Vector3f offset;

		PassengerSeatHandle(PassengerSeat seat, Vector3f offset) {
			this.seat = seat;
			this.offset = new Vector3f(offset);
		}

		void move(Location loc) {
			seat.move(loc);
		}
	}
	
}



