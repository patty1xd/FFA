package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NPCManager implements Listener {

    private final FFAPlugin plugin;
    private UUID npcUUID;
    private Location savedLocation;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        // Kill any existing NPC first
        removeNPC();
        savedLocation = loc.clone();

        String npcName = plugin.getConfig().getString("npc.name", "§6§lKit");
        Breeze npc = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npc.setCustomName(npcName);
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setGravity(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npcUUID = npc.getUniqueId();
        saveNPCData(loc);
    }

    // Kill all breeze near saved location and clear UUID
    public void removeNPC() {
        if (savedLocation != null) {
            for (Entity e : savedLocation.getWorld().getNearbyEntities(savedLocation, 3, 3, 3)) {
                if (e instanceof Breeze b && b.getCustomName() != null
                    && b.getCustomName().equals(plugin.getConfig().getString("npc.name", "§6§lKit"))) {
                    e.remove();
                }
            }
        }
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null && !e.isDead()) e.remove();
        }
        npcUUID = null;
    }

    public void restoreNPC() {
        if (!npcConfig.contains("world")) return;
        World world = Bukkit.getWorld(npcConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
            npcConfig.getDouble("x"), npcConfig.getDouble("y"), npcConfig.getDouble("z"),
            (float) npcConfig.getDouble("yaw"), 0);
        savedLocation = loc.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);
        // Periodic check every 60 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRestore, 1200L, 1200L);
    }

    private void checkAndRestore() {
        if (savedLocation == null) return;
        // Check if NPC entity is still valid
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null && !e.isDead() && e.isValid()) return; // still alive
        }
        plugin.getLogger().info("Kit NPC missing, respawning...");
        spawnNPC(savedLocation);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (savedLocation == null) return;
        if (!event.getWorld().equals(savedLocation.getWorld())) return;
        int npcChunkX = savedLocation.getBlockX() >> 4;
        int npcChunkZ = savedLocation.getBlockZ() >> 4;
        if (event.getChunk().getX() == npcChunkX && event.getChunk().getZ() == npcChunkZ) {
            Bukkit.getScheduler().runTaskLater(plugin, this::checkAndRestore, 10L);
        }
    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        if (npcUUID != null && entity.getUniqueId().equals(npcUUID)) return true;
        // Fallback name check
        if (entity instanceof Breeze && entity.getCustomName() != null) {
            String name = plugin.getConfig().getString("npc.name", "§6§lKit");
            return entity.getCustomName().equals(name);
        }
        return false;
    }

    private void saveNPCData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x", loc.getX());
        npcConfig.set("y", loc.getY());
        npcConfig.set("z", loc.getZ());
        npcConfig.set("yaw", (double) loc.getYaw());
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadNPCData() {
        npcFile = new File(plugin.getDataFolder(), "npc.yml");
        if (!npcFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { npcFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    private void clearNPCData() {
        npcConfig.set("world", null);
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
