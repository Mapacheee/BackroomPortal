package me.mapacheee.customportals.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.customportals.config.Messages;
import me.mapacheee.customportals.service.PortalService;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class PortalCommand {

    private final PortalService portalService;
    private final ReloadServiceManager reloadServiceManager;
    private final Container<Messages> messages;

    @Inject
    public PortalCommand(PortalService portalService, ReloadServiceManager reloadServiceManager,
                         Container<Messages> messages) {
        this.portalService = portalService;
        this.reloadServiceManager = reloadServiceManager;
        this.messages = messages;
    }

    @Command("backrooms reload")
    @Permission("backrooms.admin")
    public void reload(Source source) {
        reloadServiceManager.reload();
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().reloaded()));
    }

    @Command("backrooms removeportal")
    @Permission("backrooms.admin")
    public void removePortal(Source source) {
        var portalOpt = portalService.getActivePortal();
        if (portalOpt.isEmpty()) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().noActivePortal()));
            return;
        }
        portalService.removePortal();
        source.source().sendMessage(MiniMessage.miniMessage().deserialize(
            messages.get().portalRemoved()));
    }

    @Command("backrooms leave")
    @Permission("backrooms.leave")
    public void leave(Source source) {
        if (!(source.source() instanceof Player player)) {
            source.source().sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().playersOnly()));
            return;
        }
        var origin = portalService.getPlayerOrigin(player.getUniqueId());
        if (origin.isEmpty()) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().notInBackrooms()));
            return;
        }
        var location = origin.get();
        portalService.removePlayerOrigin(player.getUniqueId());
        player.teleportAsync(location).thenAccept(success -> {
            if (success) {
                player.sendMessage(MiniMessage.miniMessage().deserialize(
                    messages.get().leftBackrooms()));
            }
        });
    }
}
