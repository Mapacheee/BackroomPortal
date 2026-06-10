package me.mapacheee.backroomportal.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("config")
public record PluginConfig(
    String catalystItem,
    int portalWidth,
    int portalHeight,
    String destinationWorld,
    SpawnLocation spawn,
    TransitionConfig transition
) {
    @ConfigSerializable
    public record SpawnLocation(double x, double y, double z, float yaw, float pitch) {}

    @ConfigSerializable
    public record TransitionConfig(int fadeTicks, String sound) {}
}
