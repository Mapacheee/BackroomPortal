package me.mapacheee.backroomportal.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;
import me.mapacheee.backroomportal.config.Messages;
import me.mapacheee.backroomportal.config.PluginConfig;
import me.mapacheee.backroomportal.model.ActivePortal;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.time.Duration;
import java.util.*;

public class PortalService {

    private static final int MAX_SEARCH = 10;

    private final Container<PluginConfig> config;
    private final Container<Messages> messages;
    private final Plugin plugin;
    private final Map<UUID, Location> playerOrigins = new HashMap<>();
    private ActivePortal activePortal;

    @Inject
    public PortalService(Container<PluginConfig> config, Container<Messages> messages, Plugin plugin) {
        this.config = config;
        this.messages = messages;
        this.plugin = plugin;
    }

    public Optional<ActivePortal> getActivePortal() {
        return Optional.ofNullable(activePortal);
    }

    public void removePortal() {
        if (activePortal == null) return;
        removePortalVisuals(activePortal);
        activePortal = null;
    }

    public Optional<ActivePortal> tryActivate(Location dropLocation) {
        var cfg = config.get();
        var frameMaterial = Material.matchMaterial(cfg.frameBlock());
        if (frameMaterial == null || !frameMaterial.isBlock()) return Optional.empty();

        var frameOpt = findRectangleFrame(dropLocation, frameMaterial);
        if (frameOpt.isEmpty()) return Optional.empty();

        var frame = frameOpt.get();

        if (activePortal != null) {
            if (framesOverlap(frame, activePortal)) return Optional.empty();
            removePortal();
        }

        createPortalVisuals(frame);
        activePortal = frame;
        return Optional.of(frame);
    }

    public boolean matchesActivationItems(Collection<org.bukkit.entity.Item> items) {
        var cfg = config.get();
        var required = cfg.activationItems();

        var grouped = new HashMap<Material, Integer>();
        for (var item : items) {
            var stack = item.getItemStack();
            grouped.merge(stack.getType(), stack.getAmount(), Integer::sum);
        }

        for (var req : required) {
            var mat = Material.matchMaterial(req.material());
            if (mat == null) return false;
            var count = grouped.getOrDefault(mat, 0);
            if (count < req.amount()) return false;
        }
        return true;
    }

    public void teleportToBackrooms(Player player) {
        var cfg = config.get();
        var world = plugin.getServer().getWorld(cfg.destinationWorld());
        if (world == null) {
            player.sendMessage(MiniMessage.miniMessage().deserialize(
                messages.get().worldNotAvailable()));
            return;
        }

        playerOrigins.put(player.getUniqueId(), player.getLocation());
        var msg = messages.get();

        player.sendMessage(MiniMessage.miniMessage().deserialize(msg.teleportingToBackrooms()));

        var sound = Sound.sound(Key.key(cfg.transition().sound()), Sound.Source.AMBIENT, 1.0f, 1.0f);
        player.playSound(sound);

        var fadeTicks = cfg.transition().fadeTicks();
        var title = Title.title(
            MiniMessage.miniMessage().deserialize(""),
            MiniMessage.miniMessage().deserialize(msg.teleportingToBackrooms()),
            Title.Times.times(Duration.ZERO, Duration.ofMillis(fadeTicks * 50L), Duration.ZERO)
        );
        player.showTitle(title);

        new BukkitRunnable() {
            @Override
            public void run() {
                var spawn = cfg.spawn();
                var destination = new Location(
                    world, spawn.x(), spawn.y(), spawn.z(), spawn.yaw(), spawn.pitch()
                );
                player.teleportAsync(destination).thenAccept(success -> {
                    if (success) {
                        player.sendMessage(MiniMessage.miniMessage().deserialize(msg.backroomsArrival()));
                        var arriveSound = Sound.sound(Key.key(cfg.transition().sound()), Sound.Source.AMBIENT, 0.5f, 0.5f);
                        player.playSound(arriveSound);
                    }
                });
            }
        }.runTaskLater(plugin, fadeTicks);
    }

    public Optional<Location> getPlayerOrigin(UUID playerId) {
        return Optional.ofNullable(playerOrigins.get(playerId));
    }

    public void removePlayerOrigin(UUID playerId) {
        playerOrigins.remove(playerId);
    }

    public boolean isInsidePortal(Location location, ActivePortal portal) {
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();

        if (portal.axis() == ActivePortal.Axis.X_Y) {
            return z == portal.z()
                && x > portal.minX() && x < portal.maxX()
                && y >= portal.minY() && y <= portal.maxY();
        } else {
            return x == portal.z()
                && z > portal.minX() && z < portal.maxX()
                && y >= portal.minY() && y <= portal.maxY();
        }
    }

    private Optional<ActivePortal> findRectangleFrame(Location dropLocation, Material frameMaterial) {
        var cfg = config.get();
        int cx = dropLocation.getBlockX();
        int cy = dropLocation.getBlockY();
        int cz = dropLocation.getBlockZ();
        var world = dropLocation.getWorld();

        var xyFrame = findFrameInPlane(world, cx, cy, cz, frameMaterial, true,
            cfg.minFrameWidth(), cfg.minFrameHeight());
        if (xyFrame.isPresent()) return xyFrame;

        return findFrameInPlane(world, cx, cy, cz, frameMaterial, false,
            cfg.minFrameWidth(), cfg.minFrameHeight());
    }

    private Optional<ActivePortal> findFrameInPlane(World world, int cx, int cy, int cz,
                                                     Material frameMaterial, boolean xyPlane,
                                                     int minWidth, int minHeight) {
        int fx = cx, fy = cy, fz = cz;

        int aMin, aMax, bMin, bMax;
        Block searchBlock;

        aMin = cx;
        for (int x = cx; x >= cx - MAX_SEARCH; x--) {
            searchBlock = xyPlane ? world.getBlockAt(x, cy, cz) : world.getBlockAt(cx, cy, x);
            if (searchBlock.getType() == frameMaterial) { aMin = x; break; }
        }

        aMax = cx;
        for (int x = cx; x <= cx + MAX_SEARCH; x++) {
            searchBlock = xyPlane ? world.getBlockAt(x, cy, cz) : world.getBlockAt(cx, cy, x);
            if (searchBlock.getType() == frameMaterial) { aMax = x; break; }
        }

        bMin = cy;
        for (int y = cy; y >= cy - MAX_SEARCH; y--) {
            searchBlock = world.getBlockAt(xyPlane ? aMin : fx, y, xyPlane ? fz : aMin);
            if (searchBlock.getType() == frameMaterial) { bMin = y; break; }
        }

        bMax = cy;
        for (int y = cy; y <= cy + MAX_SEARCH; y++) {
            searchBlock = world.getBlockAt(xyPlane ? aMin : fx, y, xyPlane ? fz : aMin);
            if (searchBlock.getType() == frameMaterial) { bMax = y; break; }
        }

        int interiorWidth = aMax - aMin - 1;
        int interiorHeight = bMax - bMin - 1;
        if (interiorWidth < minWidth || interiorHeight < minHeight) return Optional.empty();

        var valid = validateFrame(world, aMin, aMax, bMin, bMax,
            xyPlane ? cz : cx, frameMaterial, xyPlane);
        if (!valid) return Optional.empty();

        int centerX = xyPlane ? (aMin + aMax) / 2 : fx;
        int centerY = (bMin + bMax) / 2;
        int centerZ = xyPlane ? fz : (aMin + aMax) / 2;
        var center = new Location(world, centerX + 0.5, centerY + 0.5, centerZ + 0.5);

        var axis = xyPlane ? ActivePortal.Axis.X_Y : ActivePortal.Axis.Z_Y;
        return Optional.of(new ActivePortal(center, aMin, aMax, bMin, bMax,
            xyPlane ? cz : cx, axis));
    }

    private boolean validateFrame(World world, int aMin, int aMax, int bMin, int bMax,
                                  int fixed, Material frameMaterial, boolean xyPlane) {
        for (int a = aMin; a <= aMax; a++) {
            for (int b : new int[]{bMin, bMax}) {
                Block block = xyPlane ? world.getBlockAt(a, b, fixed) : world.getBlockAt(fixed, b, a);
                if (block.getType() != frameMaterial) return false;
            }
        }

        for (int b = bMin + 1; b < bMax; b++) {
            for (int a : new int[]{aMin, aMax}) {
                Block block = xyPlane ? world.getBlockAt(a, b, fixed) : world.getBlockAt(fixed, b, a);
                if (block.getType() != frameMaterial) return false;
            }
        }

        for (int a = aMin + 1; a < aMax; a++) {
            for (int b = bMin + 1; b < bMax; b++) {
                Block block = xyPlane ? world.getBlockAt(a, b, fixed) : world.getBlockAt(fixed, b, a);
                if (!block.getType().isAir()) return false;
            }
        }

        return true;
    }

    private boolean framesOverlap(ActivePortal a, ActivePortal b) {
        return a.center().getWorld().equals(b.center().getWorld())
            && a.center().distance(b.center()) < 5;
    }

    private void createPortalVisuals(ActivePortal portal) {
        var cfg = config.get();
        var portalMat = Material.matchMaterial(cfg.portalBlock());
        if (portalMat == null) portalMat = Material.NETHER_PORTAL;

        var world = portal.center().getWorld();
        boolean xyPlane = portal.axis() == ActivePortal.Axis.X_Y;

        for (int a = portal.minX() + 1; a < portal.maxX(); a++) {
            for (int b = portal.minY() + 1; b < portal.maxY(); b++) {
                Block block = xyPlane
                    ? world.getBlockAt(a, b, portal.z())
                    : world.getBlockAt(portal.z(), b, a);
                if (block.getType().isAir()) {
                    block.setType(portalMat);
                }
            }
        }

        startParticleEffect(portal);
    }

    private void startParticleEffect(ActivePortal portal) {
        var cfg = config.get();
        var particle = org.bukkit.Particle.valueOf(cfg.particle().type());
        var count = cfg.particle().count();

        new BukkitRunnable() {
            final ActivePortal targetPortal = portal;
            double phase = 0;

            @Override
            public void run() {
                if (!targetPortal.equals(activePortal)) {
                    this.cancel();
                    return;
                }

                var world = targetPortal.center().getWorld();
                boolean xyPlane = targetPortal.axis() == ActivePortal.Axis.X_Y;
                int cx = (targetPortal.minX() + targetPortal.maxX()) / 2;
                int cy = (targetPortal.minY() + targetPortal.maxY()) / 2;
                int halfW = targetPortal.width() / 2;
                int halfH = targetPortal.height() / 2;

                phase += 0.15;
                for (int i = 0; i < 3; i++) {
                    double angle = phase + (i * 2 * Math.PI / 3);
                    int a = (int) (Math.cos(angle) * halfW);
                    int b = (int) (Math.sin(angle) * halfH + halfH * 0.5 * Math.sin(angle * 0.5));
                    double px, py, pz;

                    if (xyPlane) {
                        px = cx + a + 0.5;
                        py = cy + b + 0.5;
                        pz = targetPortal.z() + 0.5;
                    } else {
                        px = targetPortal.z() + 0.5;
                        py = cy + b + 0.5;
                        pz = cx + a + 0.5;
                    }

                    world.spawnParticle(particle, px, py, pz, count / 3, 0.3, 0.3, 0.3, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void removePortalVisuals(ActivePortal portal) {
        var world = portal.center().getWorld();
        boolean xyPlane = portal.axis() == ActivePortal.Axis.X_Y;

        for (int a = portal.minX() + 1; a < portal.maxX(); a++) {
            for (int b = portal.minY() + 1; b < portal.maxY(); b++) {
                Block block = xyPlane
                    ? world.getBlockAt(a, b, portal.z())
                    : world.getBlockAt(portal.z(), b, a);
                block.setType(Material.AIR);
            }
        }
    }

    public void reload() {
        removePortal();
        playerOrigins.clear();
    }
}
