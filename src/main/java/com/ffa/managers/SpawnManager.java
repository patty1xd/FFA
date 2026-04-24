package com.ffa.managers;

import com.ffa.FFAPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpawnManager implements Listener {

    private final FFAPlugin plugin;
    private Location spawnLocation;
    private final Map<UUID, Integer> pendingTeleports = new HashMap<>();
    private final Map<UUID, Location> teleportStartLocs = new HashMap<>();
    private File spawnFile;
    private FileConfiguration spawnConfig;

    public SpawnManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadSpawn();
    }

    public void setSpawn(Location loc) {
        this.spawnLocation = loc;
        saveSpawn();
    }

    public Location getSpawn() { return spawnLocation; }
    public boolean hasSpawn() { return spawnLocation != null; }

    // Auto respawn at spawn
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (spawnLocation != null) {
            event.setRespawnLocation(spawnLocation);
        }
    }

    // Auto respawn — skip death screen
    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        // Delay 1 tick so death registers properly
        Bukkit.getScheduler().runTaskLater(plugin, player::spawnAt, 1L);
    }

    // /spawn command logic
    public void handleSpawnCommand(Player player) {
        if (spawnLocation == null) {
            player.sendMessage("§cSpawn has not been set! Use §f/setspawn §cfirst.");
            return;
        }

        // Check if in combat via KoalaCombat
        if (isInCombat(player)) {
            String msg = plugin.getConfig().getString("spawn.in-combat-message",
                "&cYou cannot use /spawn while in combat!").replace("&", "§");
            player.sendMessage(msg);
            return;
        }

        // Cancel any existing pending teleport
        cancelTeleport(player.getUniqueId());

        int waitTime = plugin.getConfig().getInt("spawn.wait-time", 5);
        teleportStartLocs.put(player.getUniqueId(), player.getLocation().clone());

        String waitMsg = plugin.getConfig().getString("spawn.wait-message",
            "&eTeleporting to spawn in &f{time}s&e. Don't move!")
            .replace("{time}", String.valueOf(waitTime))
            .replace("&", "§");
        player.sendMessage(waitMsg);

        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingTeleports.remove(player.getUniqueId());
            teleportStartLocs.remove(player.getUniqueId());
            if (!player.isOnline()) return;
            player.teleport(spawnLocation);
            String doneMsg = plugin.getConfig().getString("spawn.teleported-message",
                "&aTeleported to spawn!").replace("&", "§");
            player.sendMessage(doneMsg);
        }, waitTime * 20L).getTaskId();

        pendingTeleports.put(player.getUniqueId(), taskId);
    }

    // Cancel teleport if player moves
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!pendingTeleports.containsKey(player.getUniqueId())) return;
        if (event.getTo() == null) return;
        Location from = event.getFrom();
        Location to = event.getTo();
        if (from.getBlockX() == to.getBlockX()
            && from.getBlockY() == to.getBlockY()
            && from.getBlockZ() == to.getBlockZ()) return;

        cancelTeleport(player.getUniqueId());
        String msg = plugin.getConfig().getString("spawn.cancelled-message",
            "&cTeleport cancelled!").replace("&", "§");
        player.sendMessage(msg);
    }

    public void cancelTeleport(UUID uuid) {
        Integer taskId = pendingTeleports.remove(uuid);
        if (taskId != null) Bukkit.getScheduler().cancelTask(taskId);
        teleportStartLocs.remove(uuid);
    }

    public boolean hasPendingTeleport(UUID uuid) {
        return pendingTeleports.containsKey(uuid);
    }

    private boolean isInCombat(Player player) {
        try {
            org.bukkit.plugin.Plugin combat = Bukkit.getPluginManager().getPlugin("KoalaCombat");
            if (combat == null) return false;
            Object manager = combat.getClass().getMethod("getCombatManager").invoke(combat);
            return (Boolean) manager.getClass().getMethod("isInCombat", UUID.class)
                .invoke(manager, player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    private void saveSpawn() {
        spawnConfig.set("world", spawnLocation.getWorld().getName());
        spawnConfig.set("x", spawnLocation.getX());
        spawnConfig.set("y", spawnLocation.getY());
        spawnConfig.set("z", spawnLocation.getZ());
        spawnConfig.set("yaw", spawnLocation.getYaw());
        spawnConfig.set("pitch", spawnLocation.getPitch());
        try { spawnConfig.save(spawnFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadSpawn() {
        spawnFile = new File(plugin.getDataFolder(), "spawn.yml");
        if (!spawnFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { spawnFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        spawnConfig = YamlConfiguration.loadConfiguration(spawnFile);
        if (!spawnConfig.contains("world")) return;
        World world = Bukkit.getWorld(spawnConfig.getString("world", "world"));
        if (world == null) return;
        spawnLocation = new Location(world,
            spawnConfig.getDouble("x"), spawnConfig.getDouble("y"), spawnConfig.getDouble("z"),
            (float) spawnConfig.getDouble("yaw"), (float) spawnConfig.getDouble("pitch"));
    }
}
