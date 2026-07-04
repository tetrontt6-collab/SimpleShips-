package simpleships;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.block.data.Directional;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.SmallFireball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class CannonListener implements Listener {

    private final SimpleShipsPlugin plugin;

    public CannonListener(SimpleShipsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLeftClick(PlayerAnimationEvent event) {

        Player player = event.getPlayer();

        // 1. deve essere al timone
        Ship ship = plugin.getShipByPassenger(player.getUniqueId());
        if (ship == null) return;

        if (ship.getHelmAnchor() == null ||
            !ship.getHelmAnchor().getPassengers().contains(player)) {
            return;
        }

        // 2. solo capitano
        if (!player.getUniqueId().equals(ship.getCaptain())) return;

        // 3. sparo cannoni
        shootCannons(ship, player);
    }

    private void shootCannons(Ship ship, Player player) {

        Location base = ship.getHelmAnchor().getLocation();

        for (MaterializedBlock mb : ship.getShipBlocks()) {

            if (mb.data() == null)
                continue;

            if (mb.data().getMaterial() != Material.DISPENSER)
                continue;

            if (mb.data().getMaterial() != Material.DISPENSER)
                continue;

            boolean hasAmmo = false;

            ItemStack[] contents = mb.inventoryContents();

            if (contents != null) {
                for (ItemStack item : contents) {
                    if (item != null &&
                        item.getType() == Material.FIRE_CHARGE &&
                        item.getAmount() > 0) {

                        hasAmmo = true;
                        break;
                    }
                }
            }

            if (!hasAmmo)
                continue;

            Location spawn = ship.getHelmAnchor()
                    .getLocation()
                    .clone()
                    .add(mb.offset().x, mb.offset().y, mb.offset().z)
                    .add(0.5, 0.5, 0.5);

            Directional directional = (Directional) mb.data();

            Vector dir = directional.getFacing()
                    .getDirection()
                    .normalize();

            double yaw = Math.toRadians(ship.getShipYaw()); // Corretto: usa getYaw() anziché getShipYaw()

            double x = dir.getX();
            double z = dir.getZ();

            double rotatedX = x * Math.cos(yaw) - z * Math.sin(yaw);
            double rotatedZ = x * Math.sin(yaw) + z * Math.cos(yaw);

            dir.setX(rotatedX);
            dir.setZ(rotatedZ);

            dir.multiply(2.5);

            SmallFireball fireball = player.getWorld().spawn(
                    spawn,
                    SmallFireball.class
            );

            fireball.getPersistentDataContainer().set(
                    Constants.SHIP_PROJECTILE_KEY,
                    PersistentDataType.STRING,
                    ship.getUniqueIdStr()
            );

            fireball.setVelocity(dir);
            fireball.setIsIncendiary(false);
            fireball.setYield(0f);
        }
    }
}