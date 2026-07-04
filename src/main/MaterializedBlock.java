package simpleships;
/*
 * SimpleShips
 * Copyright (c) 2026, Jere McDevitt
 *
 * Licensed under the MIT License.
 * See LICENSE file in the project root for full license information.
 */

import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

/**
 * The MaterializedBlock record is used to record the information
 * about a block that is part of the ship so that it can be restored
 * when the ship is dematerialized.
 *
 * In some cases a block that is materialized is actually made up of multiple
 * {@link:org.bukkit.entity.BlockDisplay.class} and/or {@link:org.bukkit.entity.ItemDisplay.class} objects
 * to simulate the appearance.  In those cases, the {@code:data} argument may be null, indicating
 * the MaterializedBlock is solely for display purposes and has nothing to restore.
 */
public record MaterializedBlock(Display display, BlockData data, BlockState state, Vector3f offset, ItemStack[] inventoryContents) {}

