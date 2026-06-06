package me.mapacheee.customportals.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.customportals.config.Messages;
import me.mapacheee.customportals.model.ActivePortal;
import me.mapacheee.customportals.service.PortalService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Collection;
import java.util.ArrayList;

@ListenerComponent
public class PortalListener implements Listener {

    private final PortalService portalService;
    private final Container<Messages> messages;

    @Inject
    public PortalListener(PortalService portalService, Container<Messages> messages) {
        this.portalService = portalService;
        this.messages = messages;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        var item = event.getItemDrop();
        var location = item.getLocation();
        var world = location.getWorld();

        var nearbyEntities = world.getNearbyEntities(location, 2, 2, 2);
        var nearbyItems = new ArrayList<Item>();
        for (var entity : nearbyEntities) {
            if (entity instanceof Item itemEntity) {
                nearbyItems.add(itemEntity);
            }
        }
        nearbyItems.add(item);

        if (!portalService.matchesActivationItems(nearbyItems)) return;

        var portalOpt = portalService.tryActivate(location);
        if (portalOpt.isEmpty()) {
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().invalidFrame()));
            return;
        }

        nearbyItems.forEach(Item::remove);
        event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().portalActivated()));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var to = event.getTo();

        var portalOpt = portalService.getActivePortal();
        if (portalOpt.isEmpty()) return;

        var portal = portalOpt.get();
        var portalCenter = portal.center();
        double distance = to.distance(portalCenter);
        int radius = portal.radius();

        if (distance < radius * 0.5) {
            portalService.teleportToBackrooms(player);
        }
    }
}
