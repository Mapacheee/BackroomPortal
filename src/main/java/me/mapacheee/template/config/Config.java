package me.mapacheee.template.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("config")
public record Config(
        boolean featureEnabled) {
}
