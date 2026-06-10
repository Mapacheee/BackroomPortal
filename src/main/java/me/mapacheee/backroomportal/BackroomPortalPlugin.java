package me.mapacheee.backroomportal;

import com.thewinterframework.paper.PaperWinterPlugin;
import com.thewinterframework.plugin.WinterBootPlugin;

@WinterBootPlugin
public final class BackroomPortalPlugin extends PaperWinterPlugin {
    public static BackroomPortalPlugin get() {
        return BackroomPortalPlugin.getPlugin(BackroomPortalPlugin.class);
    }
}
