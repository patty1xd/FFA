package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Husk;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.UUID;

public class RandomTeleportManager {

    private final FFAPlugin plugin;
    private final Random random = new Random();
    private Location corner1;
    private Location corner2;
    private Husk npcEntity;
    private UUID npcUUID;
    private File dataFile;
    private FileConfiguration dataConfig;

    public RandomTeleportManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void setCorner1(Location loc) { this.corner1 = loc; saveData(); }
    public void setCorner2(Location loc) { this.corner2 = loc; saveData(); }
    public boolean hasArena() { return corner1 != null && corner2 != null; }
    public Location getCorner1() { return corner1; }
    public Location getCorner2() { return corner2; }

    public void teleportRandom(Player player) {
        if (!hasArena()) {
            player.sendMessage("§cArena not set! Use §f/setarena1 §cand §f/setarena2 §cfirst.");
            return;
        }
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        int maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        World world = corner1.getWorld();

        for (int attempt = 0; attempt < 20; attempt++) {
            int x = minX + random.nextInt(maxX - minX + 1);
            int z = minZ + random.nextInt(maxZ - minZ + 1);

            // Search from bottom up, find solid block with 2 air blocks above
            for (int y = minY; y <= maxY - 2; y++) {
                org.bukkit.block.Block block = world.getBlockAt(x, y, z);
                org.bukkit.block.Block above1 = world.getBlockAt(x, y + 1, z);
                org.bukkit.block.Block above2 = world.getBlockAt(x, y + 2, z);

                if (block.getType().isSolid()
                    && !block.getType().name().contains("FENCE")
                    && !block.getType().name().contains("DOOR")
                    && !above1.getType().isSolid()
                    && !above2.getType().isSolid()) {
                    Location tp = new Location(world, x + 0.5, y + 1, z + 0.5, random.nextFloat() * 360, 0);
                    player.teleport(tp);
                    return;
                }
            }
        }
        player.sendMessage("§cCould not find a safe spot! Try again.");
    }

    public void spawnNPC(Location loc) {
        removeNPC();
        npcEntity = (Husk) loc.getWorld().spawnEntity(loc, EntityType.HUSK);
        npcEntity.setCustomName("§e§lRTP");
        npcEntity.setCustomNameVisible(true);
        npcEntity.setAI(false);
        npcEntity.setInvulnerable(true);
        npcEntity.setSilent(true);
        npcEntity.setGravity(true);
        npcEntity.setPersistent(true);
        npcEntity.setRemoveWhenFarAway(false);
        npcUUID = npcEntity.getUniqueId();
        dataConfig.set("npc.world", loc.getWorld().getName());
        dataConfig.set("npc.x", loc.getX());
        dataConfig.set("npc.y", loc.getY());
        dataConfig.set("npc.z", loc.getZ());
        dataConfig.set("npc.yaw", (double) loc.getYaw());
        saveData();
    }

    public void removeNPC() {
        if (npcEntity != null && !npcEntity.isDead()) npcEntity.remove();
        npcEntity = null;
        npcUUID = null;
    }

    public boolean isNPC(org.bukkit.entity.Entity entity) {
        return npcUUID != null && entity.getUniqueId().equals(npcUUID);
    }

    private void saveData() {
        if (corner1 != null) {
            dataConfig.set("arena.world", corner1.getWorld().getName());
            dataConfig.set("arena.x1", corner1.getX());
            dataConfig.set("arena.y1", corner1.getY());
            dataConfig.set("arena.z1", corner1.getZ());
        }
        if (corner2 != null) {
            dataConfig.set("arena.x2", corner2.getX());
            dataConfig.set("arena.y2", corner2.getY());
            dataConfig.set("arena.z2", corner2.getZ());
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "arena.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.contains("arena.world")) {
            World world = Bukkit.getWorld(dataConfig.getString("arena.world", "world"));
            if (world != null) {
                corner1 = new Location(world, dataConfig.getDouble("arena.x1"), dataConfig.getDouble("arena.y1"), dataConfig.getDouble("arena.z1"));
                corner2 = new Location(world, dataConfig.getDouble("arena.x2"), dataConfig.getDouble("arena.y2"), dataConfig.getDouble("arena.z2"));
            }
        }
        if (dataConfig.contains("npc.world")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                World world = Bukkit.getWorld(dataConfig.getString("npc.world", "world"));
                if (world == null) return;
                Location loc = new Location(world, dataConfig.getDouble("npc.x"), dataConfig.getDouble("npc.y"), dataConfig.getDouble("npc.z"), (float) dataConfig.getDouble("npc.yaw"), 0);
                spawnNPC(loc);
            }, 20L);
        }
    }
}
