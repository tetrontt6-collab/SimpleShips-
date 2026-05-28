# Changelog

## 0.9.6-beta
Added enchanting tables and bookshelves to supported blocks.

## 0.9.5-beta
Corrected orientation issues with logs.

## 0.9.4-beta
Added checks for using death chest type plugins or data packs.  Assuming
the chest is placed at the location of the player's death, the re-assembly logic
now looks if there is a container at the location that the root block would be
placed and if that is the case, the root block is not restored and the container
is left untouched. 

## 0.9.3-beta
Implemented immediate restore of the ship on player death to
avoid ship loss.

## 0.9.2-beta
Updated the collision checks to ignore kelp and lily pads

## 0.9.1-beta

Initial public beta release.

### Features
- Sailing ships
- Passenger seats
- Entity pads
- Decorative support
- Item frame support
- Armor stand support
- Collision handling
- Ship persistence

### Notes
- Water-only ships currently supported
