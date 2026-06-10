package me.mapacheee.backroomportal.model;

import org.bukkit.Location;

public record ActivePortal(
    Location center,
    int minX,
    int maxX,
    int minY,
    int maxY,
    int z,
    Axis axis
) {
    public enum Axis {
        X_Y,
        Z_Y
    }

    public int width() {
        return maxX - minX - 1;
    }

    public int height() {
        return maxY - minY - 1;
    }
}
