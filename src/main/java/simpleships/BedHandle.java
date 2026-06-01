package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.joml.Vector3i;

public class BedHandle {
	Vector3i bedLoc;
	Location endLoc;
	MaterializedBlock materializedBlock;
	
	public BedHandle(Vector3i bedLoc, MaterializedBlock mb) {
		this.bedLoc = bedLoc;
		this.materializedBlock = mb;
	}

	public void setEndLocation(Location endLoc) {
		this.endLoc = endLoc.clone();
	}
	public boolean startLocEquals(Location loc) {
		return bedLoc.x == loc.getBlockX() &&
			bedLoc.y == loc.getBlockY() &&
			bedLoc.z == loc.getBlockZ();
	}
}
