package me.mapacheee.template.command;

import com.google.inject.Inject;
import com.thewinterframework.command.CommandComponent;
import com.thewinterframework.configurate.Container;
import com.thewinterframework.service.ReloadServiceManager;
import me.mapacheee.template.config.Config;
import me.mapacheee.template.config.Messages;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;

@CommandComponent
public class TemplateCommand {

    private final ReloadServiceManager reloadServiceManager;
    private final Container<Config> config;
    private final Container<Messages> messages;

    @Inject
    public TemplateCommand(Container<Config> config, Container<Messages> messages,
            ReloadServiceManager reloadServiceManager) {
        this.config = config;
        this.messages = messages;
        this.reloadServiceManager = reloadServiceManager;
    }

    @Command("template hello")
    @Permission("template.hello")
    public void hello(Source source) {
        if (source.source() instanceof Player player) {
            String message = messages.get().welcome();
            player.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    @Command("template reload")
    @Permission("template.admin")
    public void reload(Source source) {
        reloadServiceManager.reload();
        source.source().sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloaded!"));
    }
}