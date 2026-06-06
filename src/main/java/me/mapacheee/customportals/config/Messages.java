package me.mapacheee.customportals.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("messages")
public record Messages(
    String portalActivated,
    String portalAlreadyActive,
    String invalidFrame,
    String teleportingToBackrooms,
    String backroomsArrival,
    String notInBackrooms,
    String leftBackrooms,
    String reloaded,
    String noActivePortal,
    String portalRemoved,
    String playersOnly,
    String worldNotAvailable
) {}
