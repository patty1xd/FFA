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

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NPCManager {

    private final FFAPlugin plugin;
    private Breeze npcEntity;
    private UUID npcUUID;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        removeNPC();
        npcEntity = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npcEntity.setCustomName("§6§lKit");
        npcEntity.setCustomNameVisible(true);
        npcEntity.setAI(false);
        npcEntity.setInvulnerable(true);
        npcEntity.setSilent(true);
        npcEntity.setGravity(true);
        npcUUID = npcEntity.getUniqueId();
        saveNPCData(loc);
        plugin.getLogger().info("Kit NPC spawned at " + loc);
    }

    public void removeNPC() {
        if (npcEntity != null && !npcEntity.isDead()) {
            npcEntity.remove();
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
        float pitch = (float) npcConfig.getDouble("pitch");
        Location loc = new Location(world, x, y, z, yaw, pitch);
        // Small delay to let world load
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 20L);
    }

    public boolean isNPC(Entity entity) {
        if (npcUUID == null) return false;
        return entity.getUniqueId().equals(npcUUID);
    }

    private void saveNPCData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x", loc.getX());
        npcConfig.set("y", loc.getY());
        npcConfig.set("z", loc.getZ());
        npcConfig.set("yaw", loc.getYaw());
        npcConfig.set("pitch", loc.getPitch());
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
