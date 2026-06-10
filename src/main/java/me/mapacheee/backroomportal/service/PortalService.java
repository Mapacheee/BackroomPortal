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
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.BlockFace;
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
        activePortal = null;
    }

    public Optional<ActivePortal> createPortal(Location target, BlockFace facing) {
        if (activePortal != null) removePortal();

        int w = config.get().portalWidth();
        int h = config.get().portalHeight();
        boolean xyPlane = facing == BlockFace.NORTH || facing == BlockFace.SOUTH;

        int cx = target.getBlockX();
        int cy = target.getBlockY();
        int cz = target.getBlockZ();

        int aMin = cx - w / 2;
        int aMax = aMin + w - 1;
        int bMin = cy;
        int bMax = cy + h - 1;
        int fixed = xyPlane ? cz : cx;

        var world = target.getWorld();

        int centerX = xyPlane ? (aMin + aMax) / 2 : cx;
        int centerY = (bMin + bMax) / 2;
        int centerZ = xyPlane ? cz : (aMin + aMax) / 2;
        var center = new Location(world, centerX + 0.5, centerY + 0.5, centerZ + 0.5);

        var axis = xyPlane ? ActivePortal.Axis.X_Y : ActivePortal.Axis.Z_Y;
        var portal = new ActivePortal(center, aMin, aMax, bMin, bMax, fixed, axis);

        spawnCreationEffect(portal);
        startParticleEffect(portal);
        activePortal = portal;
        return Optional.of(portal);
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
                && x >= portal.minX() && x <= portal.maxX()
                && y >= portal.minY() && y <= portal.maxY();
        } else {
            return x == portal.z()
                && z >= portal.minX() && z <= portal.maxX()
                && y >= portal.minY() && y <= portal.maxY();
        }
    }

    private void spawnCreationEffect(ActivePortal portal) {
        var center = portal.center();
        var world = center.getWorld();

        var sound = Sound.sound(Key.key("entity.ender_dragon.growl"), Sound.Source.AMBIENT, 1.0f, 0.5f);
        world.playSound(sound);

        for (int i = 0; i < 3; i++) {
            new BukkitRunnable() {
                int ticks = 0;
                @Override
                public void run() {
                    if (ticks++ > 6 || activePortal == null) { this.cancel(); return; }
                    double radius = ticks * 0.8;
                    for (int a = 0; a < 360; a += 30) {
                        double rad = Math.toRadians(a + ticks * 20);
                        double x = center.getX() + radius * Math.cos(rad);
                        double z = center.getZ() + radius * Math.sin(rad);
                        world.spawnParticle(Particle.PORTAL, x, center.getY(), z, 2, 0, 0, 0, 0.1);
                        world.spawnParticle(Particle.REVERSE_PORTAL, x, center.getY() + 1, z, 1, 0, 0, 0, 0.05);
                    }
                }
            }.runTaskTimer(plugin, i * 5, 1);
        }
    }

    private void startParticleEffect(ActivePortal portal) {
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
                int w = targetPortal.maxX() - targetPortal.minX() + 1;
                int h = targetPortal.maxY() - targetPortal.minY() + 1;
                double halfW = w / 2.0;
                double halfH = h / 2.0;

                phase += 0.12;

                for (int ring = 0; ring < 3; ring++) {
                    double rPhase = phase + ring * 2.09;
                    double rScale = 0.3 + ring * 0.35;
                    double px, py, pz;
                    double a = Math.cos(rPhase) * halfW * rScale;
                    double b = Math.sin(rPhase * 0.7) * halfH * rScale + halfH * 0.2 * Math.sin(rPhase * 1.3);

                    if (xyPlane) {
                        px = cx + 0.5 + a;
                        py = cy + 0.5 + b;
                        pz = targetPortal.z() + 0.5;
                    } else {
                        px = targetPortal.z() + 0.5;
                        py = cy + 0.5 + b;
                        pz = cx + 0.5 + a;
                    }

                    world.spawnParticle(Particle.REVERSE_PORTAL, px, py, pz, 2, 0.15, 0.15, 0.15, 0.01);
                    world.spawnParticle(Particle.PORTAL, px, py, pz, 1, 0.1, 0.1, 0.1, 0.02);
                }

                for (int i = 0; i < 4; i++) {
                    double randX = Math.random() * w - halfW;
                    double randY = Math.random() * h - halfH;
                    double px, py, pz;
                    if (xyPlane) {
                        px = cx + 0.5 + randX;
                        py = cy + 0.5 + randY;
                        pz = targetPortal.z() + 0.5;
                    } else {
                        px = targetPortal.z() + 0.5;
                        py = cy + 0.5 + randY;
                        pz = cx + 0.5 + randX;
                    }
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, px, py, pz, 1, 0.05, 0.05, 0.05, 0.01);
                }

                if (phase % 1.5 < 0.1) {
                    var edgeSound = Sound.sound(Key.key("block.beacon.ambient"), Sound.Source.AMBIENT, 0.3f, 1.5f);
                    world.playSound(edgeSound, targetPortal.center().getX(),
                        targetPortal.center().getY(), targetPortal.center().getZ());
                }
            }
        }.runTaskTimer(plugin, 0, 1);
    }

    public void reload() {
        removePortal();
        playerOrigins.clear();
    }
}
