package me.mapacheee.customportals;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;

@WinterBootPlugin
public final class CustomPortalsPlugin extends PaperWinterPlugin {
    public static CustomPortalsPlugin get() {
        return CustomPortalsPlugin.getPlugin(CustomPortalsPlugin.class);
    }
}
