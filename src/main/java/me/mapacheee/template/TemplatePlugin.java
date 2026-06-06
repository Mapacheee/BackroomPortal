package me.mapacheee.template;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;

@WinterBootPlugin
public final class TemplatePlugin extends PaperWinterPlugin {

    public static TemplatePlugin get() {
        return TemplatePlugin.getPlugin(TemplatePlugin.class);
    }

    @Override
    public void onPluginLoad() {
        super.onPluginLoad();
        // External integrations builder (e.g. PacketEvents or PAPI)
    }

    @Override
    public void onPluginEnable() {
        super.onPluginEnable();
        // Initialize integrations
    }

    @Override
    public void onPluginDisable() {
        super.onPluginDisable();
        // Shutdown integrations
    }
}
