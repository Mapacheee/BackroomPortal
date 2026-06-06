package me.mapacheee.customportals.model;

import org.bukkit.Location;

public record ActivePortal(
    Location center,
    int radius,
    Axis axis
) {
    public enum Axis {
        X_Y,
        Z_Y
    }
}
