package simpleships;
/*
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.joml.Vector3f;

import static simpleships.SimpleShipsPlugin.LOG;

public class RespawnHandle {
	Player player;
	Vector3f helmOffset;

	public RespawnHandle(Player player, Vector3f helmOffset) {
		this.player = player;
		this.helmOffset = helmOffset;
	}

	public void updateSpawnPoint(Location helmLoc, float shipYawAtAssemble, float finalYaw) {
		Vector3f rotated = UtilFuncs.rotateOffsetCardinal(helmOffset, shipYawAtAssemble, finalYaw, null);
		Location spawnLoc = helmLoc.clone().add(rotated.x, rotated.y, rotated.z);
		player.setRespawnLocation(spawnLoc,true);
		LOG(0,"Moving respawn point to (%d,%d,%d) for player %s", spawnLoc.getBlockX(), spawnLoc.getBlockY(), spawnLoc.getBlockZ(), player.getUniqueId().toString());
	}
}
