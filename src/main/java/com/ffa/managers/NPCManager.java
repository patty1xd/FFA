package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
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
    private Breeze npcEntity;
    private UUID npcUUID;
    private Location savedLocation;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        removeNPC();
        savedLocation = loc.clone();
        String npcName = plugin.getConfig().getString("npc.name", "§6§lKit");
        npcEntity = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npcEntity.setCustomName(npcName);
        npcEntity.setCustomNameVisible(true);
        npcEntity.setAI(false);
        npcEntity.setInvulnerable(true);
        npcEntity.setSilent(true);
        npcEntity.setGravity(true);
        npcEntity.setPersistent(true);
        npcEntity.setRemoveWhenFarAway(false);
        npcUUID = npcEntity.getUniqueId();
        saveNPCData(loc);
    }

    // Called periodically to check if NPC is still alive
    public void checkAndRestoreNPC() {
        if (savedLocation == null) return;
        if (npcEntity == null || npcEntity.isDead() || !npcEntity.isValid()) {
            plugin.getLogger().info("Kit NPC missing, respawning...");
            spawnNPC(savedLocation);
        }
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (savedLocation == null) return;
        Chunk npcChunk = savedLocation.getChunk();
        if (event.getChunk().getX() == npcChunk.getX()
            && event.getChunk().getZ() == npcChunk.getZ()
            && event.getWorld().equals(savedLocation.getWorld())) {
            // Small delay so entities in chunk are loaded
            Bukkit.getScheduler().runTaskLater(plugin, this::checkAndRestoreNPC, 5L);
        }
    }

    public void removeNPC() {
        if (npcEntity != null && !npcEntity.isDead()) npcEntity.remove();
        // Also kill any stray Breeze at saved location
        if (savedLocation != null) {
            for (Entity e : savedLocation.getWorld().getNearbyEntities(savedLocation, 2, 2, 2)) {
                if (e instanceof Breeze && e.isCustomNameVisible()) e.remove();
            }
        }
        npcEntity = null;
        npcUUID = null;
        clearNPCData();
    }

    public void restoreNPC() {
        if (npcConfig == null) return;
        String worldName = npcConfig.getString("world");
        if (worldName == null) return;
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        double x = npcConfig.getDouble("x");
        double y = npcConfig.getDouble("y");
        double z = npcConfig.getDouble("z");
        float yaw = (float) npcConfig.getDouble("yaw");
        Location loc = new Location(world, x, y, z, yaw, 0);
        savedLocation = loc.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 20L);

        // Periodic check every 30 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRestoreNPC, 600L, 600L);
    }

    public boolean isNPC(org.bukkit.entity.Entity entity) {
        if (npcUUID != null && entity.getUniqueId().equals(npcUUID)) return true;
        // Fallback: check if it's a Breeze near saved location with custom name
        if (savedLocation != null && entity instanceof Breeze) {
            String name = plugin.getConfig().getString("npc.name", "§6§lKit");
            if (name.equals(entity.getCustomName()) && entity.getLocation().distance(savedLocation) < 3) return true;
        }
        return false;
    }

    private void saveNPCData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x", loc.getX());
        npcConfig.set("y", loc.getY());
        npcConfig.set("z", loc.getZ());
        npcConfig.set("yaw", loc.getYaw());
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void clearNPCData() {
        npcConfig.set("world", null);
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

    public Breeze getNPCEntity() { return npcEntity; }
}
