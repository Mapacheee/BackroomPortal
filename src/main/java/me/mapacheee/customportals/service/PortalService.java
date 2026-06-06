package me.mapacheee.customportals.service;

import com.google.inject.Inject;
import com.thewinterframework.configurate.Container;

import me.mapacheee.customportals.config.Messages;
import me.mapacheee.customportals.config.PluginConfig;
import me.mapacheee.customportals.model.ActivePortal;
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

        var axisAndLocation = findFrameCenter(dropLocation, cfg.frameRadius(), frameMaterial);
        if (axisAndLocation.isEmpty()) return Optional.empty();

        var center = axisAndLocation.get().getKey();
        var axis = axisAndLocation.get().getValue();

        if (activePortal != null) {
            if (activePortal.center().distance(center) < 3) return Optional.empty();
            removePortal();
        }

        var portal = new ActivePortal(center, cfg.frameRadius(), axis);
        createPortalVisuals(portal);
        activePortal = portal;
        return Optional.of(portal);
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

    private Optional<Map.Entry<Location, ActivePortal.Axis>> findFrameCenter(
        Location dropLocation, int radius, Material frameMaterial
    ) {
        var world = dropLocation.getWorld();
        int cx = dropLocation.getBlockX();
        int cy = dropLocation.getBlockY();
        int cz = dropLocation.getBlockZ();

        var xyCenter = checkPlane(world, cx, cy, cz, radius, frameMaterial, true);
        if (xyCenter.isPresent()) {
            return Optional.of(new AbstractMap.SimpleEntry<>(xyCenter.get(), ActivePortal.Axis.X_Y));
        }

        var zyCenter = checkPlane(world, cx, cy, cz, radius, frameMaterial, false);
        return zyCenter.map(location -> new AbstractMap.SimpleEntry<>(location, ActivePortal.Axis.Z_Y));

    }

    private Optional<Location> checkPlane(World world, int cx, int cy, int cz, int radius,
                                           Material frameMaterial, boolean xyPlane) {
        var outlineOk = checkCircleOutline(world, cx, cy, cz, radius, frameMaterial, xyPlane);
        if (!outlineOk) return Optional.empty();

        var interiorOk = checkCircleInterior(world, cx, cy, cz, radius, xyPlane);
        if (!interiorOk) return Optional.empty();

        return Optional.of(new Location(world, cx + 0.5, cy + 0.5, cz + 0.5));
    }

    private boolean checkCircleOutline(World world, int cx, int cy, int cz, int radius,
                                       Material frameMaterial, boolean xyPlane) {
        double outerR = radius + 0.5;
        double innerR = radius - 0.5;

        for (int a = -radius - 1; a <= radius + 1; a++) {
            for (int b = -radius - 1; b <= radius + 1; b++) {
                double dist = Math.sqrt(a * a + b * b);
                if (dist < innerR || dist > outerR) continue;

                Block block;
                if (xyPlane) {
                    block = world.getBlockAt(cx + a, cy + b, cz);
                } else {
                    block = world.getBlockAt(cx, cy + b, cz + a);
                }
                if (block.getType() != frameMaterial) return false;
            }
        }
        return true;
    }

    private boolean checkCircleInterior(World world, int cx, int cy, int cz, int radius,
                                        boolean xyPlane) {
        double innerR = radius - 1.5;

        for (int a = -(radius - 1); a <= radius - 1; a++) {
            for (int b = -(radius - 1); b <= radius - 1; b++) {
                double dist = Math.sqrt(a * a + b * b);
                if (dist >= innerR) continue;

                Block block;
                if (xyPlane) {
                    block = world.getBlockAt(cx + a, cy + b, cz);
                } else {
                    block = world.getBlockAt(cx, cy + b, cz + a);
                }
                if (!block.getType().isAir()) return false;
            }
        }
        return true;
    }

    private void createPortalVisuals(ActivePortal portal) {
        var cfg = config.get();
        var portalMat = Material.matchMaterial(cfg.portalBlock());
        if (portalMat == null) portalMat = Material.NETHER_PORTAL;

        var center = portal.center();
        var world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int r = portal.radius();
        boolean xyPlane = portal.axis() == ActivePortal.Axis.X_Y;

        double fillR = r - 1.0;
        for (int a = -(r - 1); a <= r - 1; a++) {
            for (int b = -(r - 1); b <= r - 1; b++) {
                double dist = Math.sqrt(a * a + b * b);
                if (dist >= fillR) continue;

                Block block;
                if (xyPlane) {
                    block = world.getBlockAt(cx + a, cy + b, cz);
                } else {
                    block = world.getBlockAt(cx, cy + b, cz + a);
                }
                if (block.getType().isAir()) {
                    block.setType(portalMat);
                }
            }
        }

        startParticleEffect(portal);
    }

    private void startParticleEffect(ActivePortal portal) {
        var cfg = config.get();
        var particleName = cfg.particle().type();
        var count = cfg.particle().count();
        var speed = cfg.particle().speed();

        var particle = org.bukkit.Particle.valueOf(particleName);

        new BukkitRunnable() {
            final ActivePortal targetPortal = portal;
            double angle = 0;

            @Override
            public void run() {
                if (!targetPortal.equals(activePortal)) {
                    this.cancel();
                    return;
                }

                var center = targetPortal.center();
                var world = center.getWorld();
                int r = targetPortal.radius();
                double portalR = r * 0.7;
                boolean xyPlane = targetPortal.axis() == ActivePortal.Axis.X_Y;

                angle += 0.15;
                for (int i = 0; i < 3; i++) {
                    double a = angle + (i * 2 * Math.PI / 3);
                    double px = center.getX() + portalR * Math.cos(a);
                    double py = center.getY() + portalR * Math.sin(a * 0.7);
                    double pz;

                    if (xyPlane) {
                        pz = center.getZ();
                    } else {
                        pz = center.getZ() + portalR * Math.cos(a);
                        px = center.getX();
                    }

                    world.spawnParticle(particle, px, py, pz, count / 3, 0.3, 0.3, 0.3, speed);
                }
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    private void removePortalVisuals(ActivePortal portal) {
        var center = portal.center();
        var world = center.getWorld();
        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();
        int r = portal.radius();
        boolean xyPlane = portal.axis() == ActivePortal.Axis.X_Y;

        double fillR = r - 1.0;
        for (int a = -(r - 1); a <= r - 1; a++) {
            for (int b = -(r - 1); b <= r - 1; b++) {
                double dist = Math.sqrt(a * a + b * b);
                if (dist >= fillR) continue;

                Block block;
                if (xyPlane) {
                    block = world.getBlockAt(cx + a, cy + b, cz);
                } else {
                    block = world.getBlockAt(cx, cy + b, cz + a);
                }
                block.setType(Material.AIR);
            }
        }
    }

    public void reload() {
        removePortal();
        playerOrigins.clear();
    }
}
