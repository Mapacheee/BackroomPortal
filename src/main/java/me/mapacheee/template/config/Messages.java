package me.mapacheee.template.config;

import com.thewinterframework.configurate.config.Configurate;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;

@ConfigSerializable
@Configurate("messages")
public record Messages(
        String welcome) {
}
