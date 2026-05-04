package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Breeze;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class NPCManager implements Listener {

    private final FFAPlugin plugin;
    // Custom tag burned into the entity's NBT — survives restarts
    private final NamespacedKey NPC_KEY;

    private UUID npcUUID;
    private Location savedLocation;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        NPC_KEY = new NamespacedKey(plugin, "kit_npc");
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        // Purge every tagged breeze across the entire world before spawning
        purgeAllTaggedBreezes(loc.getWorld());
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
        // Burn our tag into the entity's persistent NBT
        npc.getPersistentDataContainer().set(NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        npcUUID = npc.getUniqueId();
        saveNPCData(loc);
    }

    /**
     * Scans every loaded chunk in the world and removes any breeze
     * that carries our NPC tag. This catches all duplicates left over
     * from previous sessions even if their UUIDs changed.
     */
    private void purgeAllTaggedBreezes(World world) {
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Breeze breeze) {
                if (breeze.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE)) {
                    breeze.remove();
                }
            }
        }
        // Also clear by UUID if tracked
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
    }

    public void removeNPC() {
        if (savedLocation != null) {
            purgeAllTaggedBreezes(savedLocation.getWorld());
        }
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
        clearNPCData();
    }

    public void restoreNPC() {
        if (!npcConfig.contains("world")) return;
        World world = Bukkit.getWorld(npcConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
            npcConfig.getDouble("x"), npcConfig.getDouble("y"), npcConfig.getDouble("z"),
            (float) npcConfig.getDouble("yaw"), 0);
        savedLocation = loc.clone();

        // Delay spawn so world is fully loaded, purge will handle any leftover duplicates
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);

        // Periodic check every 60 seconds — but only re-spawn if truly missing
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRestore, 1200L, 1200L);
    }

    private void checkAndRestore() {
        if (savedLocation == null) return;

        // First check by UUID
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null && !e.isDead() && e.isValid()) return; // still alive, do nothing
        }

        // Check if any tagged breeze already exists in the world (chunk may not be loaded yet)
        if (savedLocation.getWorld() != null) {
            for (Entity entity : savedLocation.getWorld().getEntities()) {
                if (entity instanceof Breeze breeze &&
                    breeze.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE)) {
                    // Already exists — sync our UUID and return
                    npcUUID = breeze.getUniqueId();
                    return;
                }
            }
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
        if (event.getChunk().getX() != npcChunkX || event.getChunk().getZ() != npcChunkZ) return;

        // The NPC's chunk just loaded — purge any stale saved copies, then restore one clean NPC
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // If our tracked NPC is already alive, nothing to do
            if (npcUUID != null) {
                Entity e = Bukkit.getEntity(npcUUID);
                if (e != null && !e.isDead() && e.isValid()) return;
            }
            // Otherwise purge duplicates and spawn fresh
            plugin.getLogger().info("NPC chunk loaded, restoring NPC...");
            spawnNPC(savedLocation);
        }, 10L);
    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        if (npcUUID != null && entity.getUniqueId().equals(npcUUID)) return true;
        // Fallback: check for our persistent tag
        if (entity instanceof Breeze breeze) {
            return breeze.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE);
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
