package net.fliuxx.aFKGuard.models;

import org.bukkit.Location;

public class PlayerActivity {

    private Location lastLocation;
    private float lastYaw;
    private float lastPitch;

    public PlayerActivity(Location initialLocation) {
        this.lastLocation = initialLocation.clone();
        this.lastYaw = initialLocation.getYaw();
        this.lastPitch = initialLocation.getPitch();
    }

    public boolean updateLocation(Location newLocation, String detectionMethod) {
        boolean significantMovement = false;

        if ("SIMPLE".equalsIgnoreCase(detectionMethod)) {
            significantMovement = !lastLocation.getWorld().equals(newLocation.getWorld()) ||
                    lastLocation.getX() != newLocation.getX() ||
                    lastLocation.getY() != newLocation.getY() ||
                    lastLocation.getZ() != newLocation.getZ();
        } else {
            double distanceSquared = lastLocation.distanceSquared(newLocation);
            float yawDiff = Math.abs(normalizeAngle(newLocation.getYaw() - lastYaw));
            float pitchDiff = Math.abs(normalizeAngle(newLocation.getPitch() - lastPitch));
            significantMovement = distanceSquared > 0.01 || yawDiff > 5 || pitchDiff > 5;
        }

        this.lastLocation = newLocation.clone();
        this.lastYaw = newLocation.getYaw();
        this.lastPitch = newLocation.getPitch();

        return significantMovement;
    }


    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }


    public Location getLastLocation() {
        return lastLocation.clone();
    }
}