package simpleships;

import org.bukkit.Location;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;

public class ShipProjectileListener implements Listener {

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {

        Projectile projectile = event.getEntity();

        String shooterShipId = projectile
                .getPersistentDataContainer()
                .get(
                        Constants.SHIP_PROJECTILE_KEY,
                        PersistentDataType.STRING
                );

        if (shooterShipId == null) {
            return;
        }

        Location hitLoc;

        if (event.getHitEntity() != null) {
            hitLoc = event.getHitEntity().getLocation();
        } else if (event.getHitBlock() != null) {
            hitLoc = event.getHitBlock().getLocation();
        } else {
            return;
        }

        for (Ship ship : HelmListener.shipById.values()) {

            if (ship == null)
                continue;

            if (ship.isSunk())
                continue;

            if (ship.getShipBounds() == null)
                continue;

            // evita fuoco amico
            if (ship.getUniqueIdStr().equals(shooterShipId))
                continue;

            if (ship.getShipBounds().contains(hitLoc.toVector())) {

                ship.damage(100);

                projectile.remove();

                break;
            }
        }
    }
}