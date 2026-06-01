package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.joml.Vector3f;

public class EntityPadHandle {
	final EntityPad pad;
	final Vector3f offset;
	
	public EntityPadHandle(EntityPad pad, Vector3f offset) {
		this.pad = pad;
		this.offset = new Vector3f(offset);
	}
	
	public void move(Location loc) {
		pad.move(loc);
	}

}
