package me.mapacheee.backroomportal.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.backroomportal.config.Messages;
import me.mapacheee.backroomportal.config.PluginConfig;
import me.mapacheee.backroomportal.service.PortalService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

@ListenerComponent
public class PortalListener implements Listener {

    private final PortalService portalService;
    private final Container<PluginConfig> config;
    private final Container<Messages> messages;

    @Inject
    public PortalListener(PortalService portalService, Container<PluginConfig> config,
                          Container<Messages> messages) {
        this.portalService = portalService;
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) return;

        var item = event.getItem();
        if (item == null) return;

        var cfg = config.get();
        var catalystMat = Material.matchMaterial(cfg.catalystItem());
        if (catalystMat == null || item.getType() != catalystMat) return;

        event.setCancelled(true);

        var player = event.getPlayer();
        var facing = player.getFacing();
        var clickedBlock = event.getClickedBlock();
        Location portalLocation;

        if (clickedBlock != null && clickedBlock.getType() == Material.CRYING_OBSIDIAN) {
            portalLocation = clickedBlock.getLocation().add(0.5, 1, 0.5);
        } else {
            var targetBlock = player.getTargetBlock(null, 5);
            if (targetBlock.getType().isSolid()) {
                portalLocation = targetBlock.getRelative(event.getBlockFace()).getLocation();
            } else {
                portalLocation = player.getLocation()
                    .add(player.getLocation().getDirection().multiply(3))
                    .toBlockLocation();
            }
        }

        portalService.createPortal(portalLocation, facing);

        player.sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().portalActivated()));
    }

    @EventHandler
    public void onLeftClickPortal(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        var portalOpt = portalService.getActivePortal();
        if (portalOpt.isEmpty()) return;

        var item = event.getItem();
        if (item == null) return;

        var cfg = config.get();
        var catalystMat = Material.matchMaterial(cfg.catalystItem());
        if (catalystMat == null || item.getType() != catalystMat) return;

        var player = event.getPlayer();
        var portal = portalOpt.get();
        var toPortal = portal.center().toVector().subtract(player.getEyeLocation().toVector());
        var direction = player.getEyeLocation().getDirection();
        double distance = toPortal.length();
        if (distance > 10) return;

        double dot = toPortal.normalize().dot(direction);
        if (dot < 0.85) return;

        portalService.removePortal();
        player.sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().portalRemoved()));
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        var player = event.getPlayer();
        var to = event.getTo();

        var portalOpt = portalService.getActivePortal();
        if (portalOpt.isEmpty()) return;

        var portal = portalOpt.get();
        if (portalService.isInsidePortal(to, portal)) {
            portalService.teleportToBackrooms(player);
        }
    }
}
