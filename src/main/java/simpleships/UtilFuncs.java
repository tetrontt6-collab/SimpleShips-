package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.Rotation;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3i;

public class UtilFuncs {
	private UtilFuncs() {
	}


	static final public float rotationToDegrees(Rotation rot) {
		return switch(rot) {
			case CLOCKWISE -> -90f;
			case CLOCKWISE_135 -> -135f;
			case CLOCKWISE_45 -> -45f;
			case COUNTER_CLOCKWISE -> 90f;
			case COUNTER_CLOCKWISE_45 -> 45f;
			case FLIPPED -> -180f;
			case FLIPPED_45 -> -225f;
			case NONE -> 0;
			default -> 0;
		};
	}
	static final public Vector3f getWallOffset(float height, BlockFace face) {
		return switch(face) {
			case NORTH -> new Vector3f(0.0f, height, -1.0f);
			case SOUTH -> new Vector3f(0.0f, height,  1.0f);
			case EAST  -> new Vector3f(1.0f, height,  0.0f);
			case WEST  -> new Vector3f(-1.0f, height,  0.0f);
			default -> new Vector3f();
		};
	}

	//this returns the exact degrees for each face
	static final public float getYawDegrees(BlockFace face) {
		if( face != null ) {
			switch(face) {
				case SOUTH: return 0.0f;
				case SOUTH_SOUTH_WEST : return 22.5f;
				case SOUTH_WEST: return 45.0f;
				case WEST_SOUTH_WEST: return 67.5f;
				case WEST: return 90.0f;
				case WEST_NORTH_WEST: return 112.5f;
				case NORTH_WEST: return 135.0f;
				case NORTH_NORTH_WEST: return 157.5f;
				case NORTH: return 180.0f;
				case NORTH_NORTH_EAST: return 202.5f;
				case NORTH_EAST: return 225.0f;
				case EAST_NORTH_EAST: return 247.5f;
				case EAST: return 270.0f;
				case EAST_SOUTH_EAST: return 292.5f;
				case SOUTH_EAST: return 315.0f;
				case SOUTH_SOUTH_EAST: return 337.5f;
			}
		}
		return 0.0f;
	}

	static final public BlockFace getCardinalFaceFromYaw(float degrees) {
		float cardinalYaw = getCardinalYaw(degrees);
		
		if( cardinalYaw == 90 )
			return BlockFace.WEST;
		if( cardinalYaw == 180 )
			return BlockFace.NORTH;
		if( cardinalYaw == 270 )
			return BlockFace.EAST;
		
		return BlockFace.SOUTH;
	}

	//this returns the preferred cardinal degrees for each face
	static final public float getDegreesFromFace(BlockFace face) {
		if( face != null ) {
			switch(face) {
				case EAST:
				case EAST_NORTH_EAST:
				case EAST_SOUTH_EAST:
					return -90.0f;
				case NORTH:
				case NORTH_EAST:
				case NORTH_NORTH_EAST:
				case NORTH_NORTH_WEST:
				case NORTH_WEST:
					return 180.0f;
				case SOUTH:
				case SOUTH_SOUTH_EAST:
				case SOUTH_SOUTH_WEST:
					return 0;
				case SOUTH_WEST:
				case WEST:
				case WEST_NORTH_WEST:
				case WEST_SOUTH_WEST:
					return 90.0f;
				default:
					return 180.0f;
				
			}
		}
		return 180.0f;
	}

	static final public float getReverseYaw(float yaw) {
		float cyaw = getCardinalYaw(yaw);
		if(cyaw == 0 )
			return 180.0f;
		if( cyaw == 90.0f )
			return 270.0f;
		if(cyaw == 270.0f )
			return 90.0f;
		return 0.0f;
	}
	
	//Get the Minecraft cardinal points from the yaw.
	static final public float getCardinalYaw(float yaw) {
		float normalized = yaw % 360.0f;
		if( normalized < 0.0f ) {
			normalized += 360.0f;
		}
		if( normalized >= 315.0f || normalized < 45.0f) {
			//south
			return 0.0f;
		}
		if( normalized < 135.0f ) {
			//west
			return 90.0f;
		}
		if( normalized < 225.0f ) {
			//north
			return 180.0f;
		}
		return 270;
	}

	//ensure the angle is in the range -180 -> 180 which
	//is how Minecraft does its internal yaws
	static final public float wrapDegrees(float angle) {
		while( angle <= -180.0f) {
		 	angle += 360.0f;
		}
		while( angle > 180.0f) {
		 	angle -= 360.0f;
		}
		return angle;
	}

	static final public Vector3i rotateOffsetCardinalInt(Vector3i offset, float fromYaw, float toYaw) {
		float from = getCardinalYaw(fromYaw);
		float to = getCardinalYaw(toYaw);

		int steps = Math.round(wrapDegrees(to - from)/90.0f);
		steps = ((steps%4)+4)%4;
		int x = offset.x;
		int y = offset.y;
		int z = offset.z;

		for(int i = 0; i < steps; i++) {
			int nx = -z;
			int nz = x;
			x = nx;
			z = nz;
		}
		return new Vector3i(x,y,z);
	}
		

	//Basically this changes a given (x,y,z) block offset position to a new offset
	//accounting for a change in yaw
	static final public Vector3f rotateOffsetCardinal(Vector3f offset, float fromYaw, float toYaw, Vector3f out) {
		float from = getCardinalYaw(fromYaw);
		float to = getCardinalYaw(toYaw);

		if( to == from ) {
			if(out != null ) {
				out.set(offset.x, offset.y, offset.z);
				return out;
			} else {
				return new Vector3f(offset.x, offset.y, offset.z);
			}
		}

		int steps = Math.round(wrapDegrees(to - from) / 90.0f);
		steps = ((steps %4) + 4) %4;

		int rx = (int)offset.x;
		int rz = (int)offset.z;

		for(int i = 0; i < steps; i++) {
			int nextX = -rz;
			int nextZ = rx;
			rx = nextX;
			rz = nextZ;
		}
		if(out != null ) {
			out.set(rx, offset.y, rz);
			return out;
		} else {
			return new Vector3f(rx, offset.y, rz);
		}
	}

	//similar to the offset change, this determines the new BlockFace
	//it not be necessary to do this by steps
	// TODO see if this is simpler to just figure out the toFace from the toYaw
	static final public BlockFace getRotatedFace(BlockFace origFace, float fromYaw, float toYaw) {
		BlockFace toFace = origFace;
		float from = getCardinalYaw(fromYaw);
		float to = getCardinalYaw(toYaw);
		int steps = Math.round(wrapDegrees(to - from) / 90.0f);
		steps = ((steps %4) + 4) %4;

		//brute force go CW with the faces
		for(int i = 0; i < steps; i++) {
			toFace =
				toFace == BlockFace.NORTH ? BlockFace.EAST :
				toFace == BlockFace.EAST  ? BlockFace.SOUTH :
				toFace == BlockFace.SOUTH ? BlockFace.WEST:
				toFace == BlockFace.WEST  ? BlockFace.NORTH : toFace;
		}
		return toFace;
		
	}
	static final public BlockFace getRotatedItemFace(BlockFace origFace, float fromYaw, float toYaw) {
		BlockFace toFace = origFace;
		float from = fromYaw;
		float to = toYaw;
		int steps = Math.round(wrapDegrees(to - from) / 22.5f);
		steps = ((steps %16) + 16) %16;

		//brute force go CW with the faces
		for(int i = 0; i < steps; i++) {
			toFace =
				toFace == BlockFace.SOUTH ? BlockFace.SOUTH_SOUTH_WEST :
				toFace == BlockFace.SOUTH_SOUTH_WEST  ? BlockFace.SOUTH_WEST :
				toFace == BlockFace.SOUTH_WEST ? BlockFace.WEST_SOUTH_WEST:
				toFace == BlockFace.WEST_SOUTH_WEST  ? BlockFace.WEST : 
				toFace == BlockFace.WEST  ? BlockFace.WEST_NORTH_WEST : 
				toFace == BlockFace.WEST_NORTH_WEST  ? BlockFace.NORTH_WEST:
				toFace == BlockFace.NORTH_WEST  ? BlockFace.NORTH_NORTH_WEST:
				toFace == BlockFace.NORTH_NORTH_WEST  ? BlockFace.NORTH:
				toFace == BlockFace.NORTH  ? BlockFace.NORTH_NORTH_EAST:
				toFace == BlockFace.NORTH_NORTH_EAST  ? BlockFace.NORTH_EAST:
				toFace == BlockFace.NORTH_EAST  ? BlockFace.EAST_NORTH_EAST:
				toFace == BlockFace.EAST_NORTH_EAST  ? BlockFace.EAST:
				toFace == BlockFace.EAST  ? BlockFace.EAST_SOUTH_EAST:
				toFace == BlockFace.EAST_SOUTH_EAST  ? BlockFace.SOUTH_EAST:
				toFace == BlockFace.SOUTH_EAST  ? BlockFace.SOUTH_SOUTH_EAST : BlockFace.SOUTH;
		}
		return toFace;
		
	}

	static final public boolean isHorizontalFace(BlockFace face) {
		return
			face == BlockFace.NORTH ||
			face == BlockFace.SOUTH ||
			face == BlockFace.EAST ||
			face == BlockFace.WEST;
	}


	/**
	 * Some item's need specialized transformation matrices as they need to be
	 * both internally rotated and externally rotated, think like a moon around
	 * a planet.  The moon has a rotation/position relative to the planet, but then the moon
	 * also has rotation around its own axis.
	 */
	static final public Matrix4f createCustomTransformItem(Vector3f offset, BlockFace face, float shipYaw, float scaleX, Vector3f woff) {
		return createCustomTransformItem(offset, face, shipYaw, new Vector3f(scaleX, 1, 1), woff, null);
	}
	static public final Matrix4f createCustomTransformItem(Vector3f offset, BlockFace face, float shipYaw, Vector3f scaler, Vector3f woff) {
		return createCustomTransformItem(offset, face, shipYaw, scaler, woff, null);
	}
	static public final Matrix4f createCustomTransformItem(Vector3f offset, BlockFace face, float shipYaw, Vector3f scaler, Vector3f woff, Rotation itemRot) {
		float shipRadians = (float)Math.toRadians(shipYaw);
		float blockYawRadians = (float)Math.toRadians(-UtilFuncs.getYawDegrees(face));

		Quaternionf sr = new Quaternionf().rotateY(shipRadians);
		Matrix4f shipRotation = new Matrix4f().identity().rotate(sr);
		
		Matrix4f localTranslation = new Matrix4f().identity().translate(offset.x, offset.y + 0.5f - Ship.SHIP_VERTICAL_OFFSET, offset.z);

		Quaternionf byaw = new Quaternionf().rotateY(blockYawRadians);
		Matrix4f blockRotation = new Matrix4f().identity().rotate(byaw);

		if( face == BlockFace.UP || face == BlockFace.DOWN ) {
			Quaternionf itemRotX = new Quaternionf().rotateX((float)Math.toRadians(90));
			blockRotation.rotate(itemRotX);
		}

		if( itemRot != null ) {
			float rzD = UtilFuncs.rotationToDegrees(itemRot);
			float delta = 0;
			if( face == BlockFace.UP ) {
				delta = switch(itemRot) {
					case CLOCKWISE -> 90f;             //D
					case CLOCKWISE_135 -> 180f;        //D 
					case CLOCKWISE_45 -> 0f;           //D
					case COUNTER_CLOCKWISE -> 90;      //D   
					case COUNTER_CLOCKWISE_45 -> 180f;
					case FLIPPED -> 270f;              //D
					case FLIPPED_45 -> 360f;           //D
					case NONE -> -90;                  //D
				};
			} else if(face == BlockFace.DOWN ) {
				delta = 0;
			}
			rzD += delta;
			
			Quaternionf itemRotZ = new Quaternionf().rotateZ((float)Math.toRadians(rzD));
			blockRotation.rotate(itemRotZ);
		}

		Matrix4f scale = new Matrix4f().identity().scale(scaler);
		Matrix4f wallOffset = new Matrix4f().identity();

		if( woff != null )
			wallOffset = wallOffset.translate(woff);
		
		return new Matrix4f(shipRotation)
			.mul(localTranslation)
			.mul(wallOffset)
			.mul(blockRotation)
			.mul(scale);
	}
	
	/**
	 * Wall blocks and Multiblock structures like Chests and Beds have different requirements to get them
	 * looking right as BlockDisplays.
	 *
	 * For the double blocks structures to work as BlockDisplays only one of the blocks becomes a BlockDisplay
	 * then it is adjusted to appear to look like the full thing.  The standard rotation for normal
	 * blocks doesn't work due to the mechanics of the multi block so this routine handles that part.
	 *
	 * For Beds, simply creating a BlockDisplay of the HEAD part will render looking like a full bed.
	 *
	 * For Chests, creating the RIGHT side of a double block then scaling by 2.1 gives the appearance
	 * of a double chest.
	 *
	 * Wall block transformation are a sequencing.  Technically wall blocks are just small
	 * blocks located at the center of their placement, but then shifted toward the block the connect
	 * with, so for a complete rotation we need to have a series of transformations
	 *
	 *  shipRotation x localTranslation x center x offset x facingRotation x uncenter
	 */
	static public final Matrix4f createCustomTransform(Vector3f offset, BlockFace face, float shipYaw, float scaleX, Vector3f woff) {
		return createCustomTransform(offset, face, shipYaw, new Vector3f(scaleX,1,1), woff);
	}
	static public final Matrix4f createCustomTransform(Vector3f offset, BlockFace face, float shipYaw, Vector3f scaler, Vector3f woff) {
		float shipRadians = (float)Math.toRadians(shipYaw);
		float blockRadians = (float)Math.toRadians(-UtilFuncs.getYawDegrees(face));
		
		Quaternionf sr = new Quaternionf().rotateY(shipRadians);
		Matrix4f shipRotation = new Matrix4f().identity().rotate(sr);
		
		Matrix4f localTranslation = new Matrix4f().identity().translate(offset.x-0.5f, offset.y - Ship.SHIP_VERTICAL_OFFSET, offset.z-0.5f);

		Matrix4f center = new Matrix4f().identity().translate(0.5f, Ship.SHIP_VERTICAL_OFFSET, 0.5f);
		Matrix4f uncenter = new Matrix4f().identity().translate(-0.5f, -Ship.SHIP_VERTICAL_OFFSET, -0.5f);

		Quaternionf cr = new Quaternionf().rotateY(blockRadians);
		Matrix4f blockRotation = new Matrix4f().identity().rotate(cr);

		Matrix4f scale = new Matrix4f().identity().scale(scaler);
		Matrix4f wallOffset = new Matrix4f().identity();

		if( woff != null )
			wallOffset = wallOffset.translate(woff);
		
		return new Matrix4f(shipRotation)
			.mul(localTranslation)
			.mul(center)
			.mul(wallOffset)
			.mul(blockRotation)
			.mul(uncenter)
			.mul(scale);
	}


	static final public  BlockFace rotateFrameFace(BlockFace face, float fromYaw, float toYaw) {
		if(isHorizontalFace(face)) {
			return getRotatedFace(face, fromYaw, toYaw);
		}
		return face;
	}

	static final public Location getItemFrameSpawnLocation(Block attachedBlock, BlockFace facing) {
		Location base = attachedBlock.getLocation();
		return switch(facing) {
			case NORTH -> base.clone().add( 0.5, 0.0, -1.0);
			case SOUTH -> base.clone().add( 0.5, 0.0,  1.0);
			case EAST  -> base.clone().add( 1.0, 0.0,  0.5);
			case WEST  -> base.clone().add(-1.0, 0.0,  0.5);
			case UP    -> base.clone().add( 0.5, 1.0,  0.5);
			case DOWN  -> base.clone().add( 0.5, -1.0,  0.5);
			default -> base.clone().add(0.5, 0.0, 0.5);
		};
	}

	static final public Rotation rotateItemFrameRotation(Rotation original, BlockFace originalFacing, BlockFace restoredFacing, float fromYaw, float toYaw) {
		if( originalFacing != BlockFace.UP && originalFacing != BlockFace.DOWN)
			return original;

		int originalSteps = rotationToSteps(original);
		float deltaYaw = wrapDegrees(toYaw - fromYaw);
		int deltaSteps = Math.round(deltaYaw / 45.0f);

		if( originalFacing == BlockFace.DOWN) {
			deltaSteps = -deltaSteps;
		}
		int resultSteps = Math.floorMod(originalSteps + deltaSteps, 8);
		return stepsToRotation(resultSteps);
	}

	static public final int rotationToSteps(Rotation rot) {
		return switch(rot) {
			case NONE -> 0;
			case CLOCKWISE_45 -> 1;
			case CLOCKWISE -> 2;
			case CLOCKWISE_135 -> 3;
			case FLIPPED -> 4;
			case FLIPPED_45 -> 5;
			case COUNTER_CLOCKWISE -> 6;
			case COUNTER_CLOCKWISE_45 -> 7;
		};
	}

	static public Rotation stepsToRotation(int steps) {
		return switch (Math.floorMod(steps, 8)) {
			case 0 -> Rotation.NONE;
			case 1 -> Rotation.CLOCKWISE_45;
			case 2 -> Rotation.CLOCKWISE;
			case 3 -> Rotation.CLOCKWISE_135;
			case 4 -> Rotation.FLIPPED;
			case 5 -> Rotation.FLIPPED_45;
			case 6 -> Rotation.COUNTER_CLOCKWISE;
			case 7 -> Rotation.COUNTER_CLOCKWISE_45;
			default -> Rotation.NONE;
		};
	}

	static private final float EPSILON = 0.1f;
	static public final boolean isZeroOffset(Vector3f off) {
		return
			(float)Math.abs(off.x) < EPSILON &&
			(float)Math.abs(off.y) < EPSILON &&
			(float)Math.abs(off.z) < EPSILON;
	}

	static public final boolean isContainer(Block block) {
		BlockState state = block.getState();
		return (state instanceof Container);
	}

	static public final Axis rotateAxis(Axis original, float startYaw, float finalYaw) {
		if(original == Axis.Y ) {
			return Axis.Y;
		}

		float from = getCardinalYaw(startYaw);
		float to   = getCardinalYaw(finalYaw);

		int steps = Math.round(wrapDegrees(to - from) / 90.0f);
		steps = ((steps % 4) + 4) % 4;
		if((steps % 2) == 1) {
			return original == Axis.X ? Axis.Z : Axis.X;
		}
		return original;
	}
}

