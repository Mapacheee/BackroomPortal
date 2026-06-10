package me.mapacheee.customportals.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import java.util.List;

@ConfigSerializable
@Configurate("config")
public record PluginConfig(
    String frameBlock,
    int minFrameWidth,
    int minFrameHeight,
    List<ActivationItem> activationItems,
    String portalBlock,
    String destinationWorld,
    SpawnLocation spawn,
    ParticleConfig particle,
    TransitionConfig transition
) {
    @ConfigSerializable
    public record ActivationItem(String material, int amount) {}

    @ConfigSerializable
    public record SpawnLocation(double x, double y, double z, float yaw, float pitch) {}

    @ConfigSerializable
    public record ParticleConfig(String type, int count, double speed) {}

    @ConfigSerializable
    public record TransitionConfig(int fadeTicks, String sound) {}
}
