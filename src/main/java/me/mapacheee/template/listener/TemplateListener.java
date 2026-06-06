package me.mapacheee.template.listener;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.paper.listener.ListenerComponent;
import me.mapacheee.template.config.Config;
import me.mapacheee.template.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

@ListenerComponent
public class TemplateListener implements Listener {

    private final Container<Config> config;
    private final Container<Messages> messages;

    @Inject
    public TemplateListener(Container<Config> config, Container<Messages> messages) {
        this.config = config;
        this.messages = messages;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (config.get().featureEnabled()) {
            String message = messages.get().welcome();
            event.getPlayer().sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }
}
