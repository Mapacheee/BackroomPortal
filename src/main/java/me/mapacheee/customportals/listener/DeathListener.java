package me.mapacheee.customportals.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.customportals.config.Messages;
import me.mapacheee.customportals.config.PluginConfig;
import me.mapacheee.customportals.service.PortalService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

@ListenerComponent
public class DeathListener implements Listener {

    private final PortalService portalService;
    private final Container<PluginConfig> config;
    private final Container<Messages> messages;

    @Inject
    public DeathListener(PortalService portalService, Container<PluginConfig> config,
                         Container<Messages> messages) {
        this.portalService = portalService;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        var player = event.getPlayer();
        var cfg = config.get();
        var backroomsWorld = player.getServer().getWorld(cfg.destinationWorld());

        if (backroomsWorld == null) return;
        if (!player.getWorld().equals(backroomsWorld)) return;

        var origin = portalService.getPlayerOrigin(player.getUniqueId());
        origin.ifPresent(location -> {
            player.setRespawnLocation(location, true);
        });
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        var player = event.getPlayer();
        var origin = portalService.getPlayerOrigin(player.getUniqueId());
        if (origin.isPresent()) {
            event.setRespawnLocation(origin.get());
            portalService.removePlayerOrigin(player.getUniqueId());
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().leftBackrooms()));
        }
    }
}
