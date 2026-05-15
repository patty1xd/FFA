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
    private Location savedLocation;
    private UUID npcUUID;
    private boolean respawnScheduled = false;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
        startWatchdog();
    }

    // ── Spawn / Remove ───────────────────────────────────────────────

    public void spawnNPC(Location loc) {
        savedLocation = loc.clone();
        removeExistingNPC();

        String npcName = plugin.getConfig().getString("npc.name", "§6§lKit");

        Breeze npc = loc.getWorld().spawn(loc, Breeze.class, entity -> {
            entity.setCustomName(npcName);
            entity.setCustomNameVisible(true);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
        });

        npcUUID = npc.getUniqueId();
        keepChunkLoaded(loc);
        saveNPCData(loc);
    }

    public void removeNPC() {
        releaseChunkTicket();
        removeExistingNPC();
        savedLocation = null;
        clearNPCData();
    }

    // ── Restore on startup ───────────────────────────────────────────

    public void restoreNPC() {
        if (!npcConfig.contains("world")) return;

        // Try to reuse the still-loaded entity by UUID
        if (npcConfig.contains("uuid")) {
            try {
                UUID uuid = UUID.fromString(npcConfig.getString("uuid", ""));
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof Breeze && entity.isValid()) {
                    npcUUID = uuid;
                    savedLocation = entity.getLocation().clone();
                    // Re-apply all flags in case they got lost during world save/load
                    Breeze breeze = (Breeze) entity;
                    breeze.setAI(false);
                    breeze.setGravity(false);
                    breeze.setInvulnerable(true);
                    breeze.setSilent(true);
                    breeze.setPersistent(true);
                    breeze.setRemoveWhenFarAway(false);
                    keepChunkLoaded(savedLocation);
                    return;
                }
            } catch (IllegalArgumentException ignored) {}
        }

        World world = Bukkit.getWorld(npcConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
                npcConfig.getDouble("x"), npcConfig.getDouble("y"), npcConfig.getDouble("z"),
                (float) npcConfig.getDouble("yaw"), 0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);
    }

    // ── Death callback (called by NPCProtectListener) ────────────────

    public void onEntityDeath() {
        npcUUID = null;
        if (savedLocation != null && !respawnScheduled) {
            respawnScheduled = true;
            Location loc = savedLocation.clone();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                respawnScheduled = false;
                spawnNPC(loc);
            }, 40L);
        }
    }

    // ── Identification ───────────────────────────────────────────────

    public boolean isNPC(Entity entity) {
        if (entity == null || npcUUID == null) return false;
        return npcUUID.equals(entity.getUniqueId());
    }

    // ── Internal helpers ─────────────────────────────────────────────

    private void startWatchdog() {
        // Single timer started once — checks every 30 s if entity is still valid
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (savedLocation == null || npcUUID == null) return;
            Entity entity = Bukkit.getEntity(npcUUID);
            if (entity == null || !entity.isValid()) {
                spawnNPC(savedLocation);
            }
        }, 600L, 600L);
    }

    private void removeExistingNPC() {
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
        // Safety net: kill any stray entity matching our name
        String name = plugin.getConfig().getString("npc.name", "§6§lKit");
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Breeze && name.equals(e.getCustomName())) {
                    e.remove();
                }
            }
        }
    }

    private void keepChunkLoaded(Location loc) {
        loc.getWorld().addPluginChunkTicket(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, plugin);
    }

    private void releaseChunkTicket() {
        if (savedLocation != null) {
            savedLocation.getWorld().removePluginChunkTicket(
                    savedLocation.getBlockX() >> 4, savedLocation.getBlockZ() >> 4, plugin);
        }
    }

    // ── Persistence ──────────────────────────────────────────────────

    private void saveNPCData(Location loc) {
        npcConfig.set("uuid",  npcUUID != null ? npcUUID.toString() : null);
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x",     loc.getX());
        npcConfig.set("y",     loc.getY());
        npcConfig.set("z",     loc.getZ());
        npcConfig.set("yaw",   (double) loc.getYaw());
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void clearNPCData() {
        npcConfig.set("world", null);
        npcConfig.set("uuid",  null);
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
}
