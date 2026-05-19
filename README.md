# SimpleShips

<p align="center">
	<img src="screenshots/VikingLongShip.png" width="500">
</p>



> Smooth, survival-friendly sailing ships for modern Minecraft servers.

SimpleShips is a lightweight Paper plugin that allows players to build and sail their own ships using ordinary Minecraft blocks.

No client mods. No resource packs. No physics simulation.

Build a ship, place a helm, gather your crew, and set sail.

---

## Features

* Smooth ship movement using modern Display Entity APIs
* No client mods required
* Multiplayer friendly
* Survival-friendly recipes
* Passenger seats for other players
* Entity pads for animals and villagers
* Collision detection while sailing
* Reverse movement to help maneuver out of tight spaces
* Ship persistence across chunk loads and server restarts
* Lightweight implementation using supported Paper APIs only

### Supported Decorative Features

SimpleShips preserves many decorative elements while ships are materialized:

* Banners and their patterns
* Hanging banners
* Signs
* Hanging signs
* Player heads and skulls
* Item frames and glow item frames
* Armor stands
* Chests and double chests
* Barrels
* Furnaces
* Beds
* Campfires
* Light sources

Inventories and most block state information are restored automatically when ships disassemble.

---

## Philosophy

SimpleShips is intentionally *not* a physics simulation.

There are already fantastic projects in the Minecraft community focused on advanced engineering and physics-driven gameplay. SimpleShips takes a different approach:

* No buoyancy calculations
* No weight systems
* No power systems
* No rigid body physics
* No complicated setup

The goal is simple:

> Build a ship and sail it through your world.

SimpleShips focuses on atmosphere, exploration, creativity, and multiplayer adventures while remaining lightweight and easy to use.

---

## Requirements

* PaperMC 1.21.11+
* Java 21+

### Tested Versions

* Paper 1.21.11
* Paper 26.2.1

SimpleShips uses supported Paper APIs only and does not rely on NMS or internal server classes.

---

## Installation

1. Download the latest release
2. Drop the jar into your server's `plugins/` directory
3. Start or restart the server
4. Build a ship, place a helm and begin sailing

---

## Configuration

Currently the configuration file is limited to setting boundaries for assembled ships.

```yaml
debug: false

ship-size:
  max-ship-blocks:  500
  max-ship-x-width: 32
  max-ship-height:  16
  max-ship-z-width: 32
```

By default, ships are limited to 500 blocks and the dimensions listed above.

Using a value of `-1` for any of these settings removes the limit.

The `debug` parameter will cause extra messages to be generated in the server logs.

---

## Ship Components

There are various components available for use with the ship, the primary being the **Helm**.
To carry non-player living entities, there is the **Entity Pad** and to allow your companions to
ride along, there is the **Passenger Seat**.

When placing the **Helm**, it is necessary to be facing in the direction considered to be the front of the
ship as the direction the player is facing at the time of placement determines what direction is `forward`.

All components can be picked up with a `sneak-left click`.

To use the **Helm** or **Passenger Seat**, simply right-click and the player will mount. The moment a player mounts the
**Helm** the ship is assembled into Display entities and any entities or players standing on the ship will
fall through the ship, so make sure your companions and pets are seated before the captain takes the helm.

The **Entity Pads** are used by simply right clicking as well, but what they will do is scan the area around the pad for
any nearby living entity (excluding players, armor stands, and water mobs) and will mount that entity onto the pad. 

Unmounting a **Helm** or **Passenger Seat** is the standard unmount option.

To detach the entity from the **Entity Pad**, `sneak-right click` on the entity or the pad.

---

## Crafting Recipes


### Helm

Used to pilot ships.  When placing the helm, the player needs to be facing
in the direction considered the front of the ship as that will be considered
the direction of movement.

```text
[][ Spruce Trapdoor ][]
[][   Spruce Fence  ][]
[][    Iron Ingot   ][]
```

### Passenger Seat

Allows other players to ride aboard ships.

```text
[][  Yellow Carpet  ][]
[][ Mangrove Stairs ][]
[][    Iron Ingot   ][]
```

### Entity Pad

Allows animals and villagers to travel aboard ships.

```text
[][      Hay Bale   ][]
[][    Oak Fence    ][]
[][    Iron Ingot   ][]
```

---

## Controls

| Key    | Action                           |
| ------ | -------------------------------- |
| W      | Move forward                     |
| S      | Move backward                    |
| A / D  | Turn ship                        |
| Space  | Toggle auto-sail                 |
| Sprint | Align ship to cardinal direction |
| Sneak  | Dismount                         |


When auto-sail is active, the ship will move forward at a steady pace and can be turned.  To halt the ship,
simply press W/S or Space again.

---

## Building Ships

To create a ship:

1. Build using normal Minecraft blocks
2. Ensure at least part of the ship is touching water
3. Place a helm somewhere on the vessel
4. Right-click the helm to assemble and pilot the ship
5. Upon dismount, the ship will align to the nearest cardinal direction and rematerialize.

When assembled, the ship materializes into moving Display Entities while preserving the appearance of the original build.

---

## Collision Handling

SimpleShips performs collision detection while moving forward to help prevent ships from passing through terrain or structures.

Reverse movement intentionally ignores collision checks.

This behavior is a deliberate design choice intended to help players recover ships that become wedged in tight canals, docks, harbors, or other difficult navigation areas.

While it is technically possible to misuse reverse movement to force ships into invalid locations, SimpleShips prioritizes gameplay recovery and ease of use over strict movement enforcement.

Note that when the pilot dismounts, the ship rematerializes back into world blocks. Any intersecting blocks occupying the same space may be replaced during this process.

---
## Commands

| Command | Description |
|---|---|
| `/cleanup` | This will remove all ship component parts (helm/seat/pad) as an aid to cleaning up issues |
| `/helm` | Gives the player a ship helm |
| `/seat` | Gives the player a passenger seat |
| `/pad` | Gives the player an entity pad |
| `/flush` | Forces all active ships to rematerialize |

### Beta Note

At the moment, commands are intentionally unrestricted during the beta phase.

This means any player with access to commands can currently execute `/flush` and `/cleanup`, which forces all active ships to align and rematerialize and all ship components to be removed, respectively.

These commands exist primarily as recovery/debugging tools while the plugin continues to mature and may become permission-restricted in a future release.

---
## Notes

* Larger ships move more slowly than smaller ships
* Very large ships may occasionally show minor visual interpolation gaps during movement
* Reverse movement intentionally ignores collision checks to help ships escape tight spaces
* Ships currently operate on water only
* Decorative armor stands, player heads and item frames are supported

---

## Current Status

SimpleShips is currently in beta.  The project is actively developed and used in real survival gameplay.

The plugin is fully playable and actively used in survival gameplay, but there may still be occasional edge cases or issues during chunk loading, server restart recovery, or unusual builds.

Feedback, bug reports, and suggestions are welcome.

---

## Screenshots and Videos

<p align="center">
	<img src="screenshots/VikingLongShip.png" width="700">
</p>

<p align="center">
	<video src="videos/GoingAViking.mp4" controls width="800"></video>
</p>	

<p align="center">
	<em>
		Viking Longship design by Minecraft builder ThorHammerhand
	</em>
</p>	


---

## Inspiration

SimpleShips would not exist without the amazing work done over the years by the Minecraft modding and plugin communities.

Special appreciation goes to projects such as:

* Archimedes' Ships
* Davinci's Vessels
* Movecraft
* Valkyrien Skies
* Create
* Create Aeronautics

These projects helped inspire the idea that player-built ships could become living parts of a Minecraft world.

---

## License

SimpleShips is licensed under the MIT License.

See the [LICENSE](LICENSE) file for full details.

---

## Final Thoughts

SimpleShips began as a personal experiment exploring what modern Minecraft Display Entities could do.

It turned into something far more fun than expected.

If you build something cool with it, sail into adventure with friends, or simply enjoy watching a ship glide across the water at sunset, then the project has succeeded.

Fair winds and following seas.

