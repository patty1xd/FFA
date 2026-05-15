package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
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

    private Location npcSavedLocation;
    private UUID npcUUID;
    private boolean respawnScheduled = false;

    private File dataFile;
    private FileConfiguration dataConfig;

    private static final String NPC_NAME = "§e§lRTP";

    public RandomTeleportManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        startWatchdog();
    }

    // ── Arena corners ────────────────────────────────────────────────

    public void setCorner1(Location loc) { this.corner1 = loc; saveData(); }
    public void setCorner2(Location loc) { this.corner2 = loc; saveData(); }
    public boolean hasArena()            { return corner1 != null && corner2 != null; }
    public Location getCorner1()         { return corner1; }
    public Location getCorner2()         { return corner2; }

    // ── Teleport ─────────────────────────────────────────────────────

    public void teleportRandom(Player player) {
        if (!hasArena()) { player.sendMessage("§cArena not set!"); return; }

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
            for (int y = minY; y <= maxY - 2; y++) {
                if (world.getBlockAt(x, y, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 1, z).getType().isSolid()
                        && !world.getBlockAt(x, y + 2, z).getType().isSolid()) {
                    player.teleport(new Location(world, x + 0.5, y + 1, z + 0.5,
                            random.nextFloat() * 360, 0));
                    return;
                }
            }
        }
        player.sendMessage("§cCould not find a safe spot! Try again.");
    }

    // ── NPC Spawn / Remove ───────────────────────────────────────────

    public void spawnNPC(Location loc) {
        npcSavedLocation = loc.clone();
        removeExistingNPC();

        Husk npc = loc.getWorld().spawn(loc, Husk.class, entity -> {
            entity.setCustomName(NPC_NAME);
            entity.setCustomNameVisible(true);
            entity.setAI(false);
            entity.setGravity(false);
            entity.setInvulnerable(true);
            entity.setSilent(true);
            entity.setPersistent(true);
            entity.setRemoveWhenFarAway(false);
            entity.setBaby(false);
        });

        npcUUID = npc.getUniqueId();
        keepChunkLoaded(loc);

        dataConfig.set("npc.uuid",  npcUUID.toString());
        dataConfig.set("npc.world", loc.getWorld().getName());
        dataConfig.set("npc.x",     loc.getX());
        dataConfig.set("npc.y",     loc.getY());
        dataConfig.set("npc.z",     loc.getZ());
        dataConfig.set("npc.yaw",   (double) loc.getYaw());
        saveData();
    }

    public void removeNPC() {
        releaseChunkTicket();
        removeExistingNPC();
        npcSavedLocation = null;
    }

    // ── Death callback (called by NPCProtectListener) ────────────────

    public void onEntityDeath() {
        npcUUID = null;
        if (npcSavedLocation != null && !respawnScheduled) {
            respawnScheduled = true;
            Location loc = npcSavedLocation.clone();
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
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (npcSavedLocation == null || npcUUID == null) return;
            Entity entity = Bukkit.getEntity(npcUUID);
            if (entity == null || !entity.isValid()) {
                spawnNPC(npcSavedLocation);
            }
        }, 600L, 600L);
    }

    private void removeExistingNPC() {
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
        // Safety net: kill any stray husk with the RTP name
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Husk && NPC_NAME.equals(e.getCustomName())) {
                    e.remove();
                }
            }
        }
    }

    private void keepChunkLoaded(Location loc) {
        loc.getWorld().addPluginChunkTicket(loc.getBlockX() >> 4, loc.getBlockZ() >> 4, plugin);
    }

    private void releaseChunkTicket() {
        if (npcSavedLocation != null) {
            npcSavedLocation.getWorld().removePluginChunkTicket(
                    npcSavedLocation.getBlockX() >> 4, npcSavedLocation.getBlockZ() >> 4, plugin);
        }
    }

    // ── Persistence ──────────────────────────────────────────────────

    private void saveData() {
        if (corner1 != null) {
            dataConfig.set("arena.world", corner1.getWorld().getName());
            dataConfig.set("arena.x1",    corner1.getX());
            dataConfig.set("arena.y1",    corner1.getY());
            dataConfig.set("arena.z1",    corner1.getZ());
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
                corner1 = new Location(world,
                        dataConfig.getDouble("arena.x1"), dataConfig.getDouble("arena.y1"), dataConfig.getDouble("arena.z1"));
                corner2 = new Location(world,
                        dataConfig.getDouble("arena.x2"), dataConfig.getDouble("arena.y2"), dataConfig.getDouble("arena.z2"));
            }
        }

        if (dataConfig.contains("npc.world")) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                // Try UUID first
                if (dataConfig.contains("npc.uuid")) {
                    try {
                        UUID uuid = UUID.fromString(dataConfig.getString("npc.uuid", ""));
                        Entity entity = Bukkit.getEntity(uuid);
                        if (entity instanceof Husk && entity.isValid()) {
                            npcUUID = uuid;
                            npcSavedLocation = entity.getLocation().clone();
                            Husk husk = (Husk) entity;
                            husk.setAI(false);
                            husk.setGravity(false);
                            husk.setInvulnerable(true);
                            husk.setSilent(true);
                            husk.setPersistent(true);
                            husk.setRemoveWhenFarAway(false);
                            keepChunkLoaded(npcSavedLocation);
                            return;
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
                // Fall back to coordinates
                World world = Bukkit.getWorld(dataConfig.getString("npc.world", "world"));
                if (world == null) return;
                Location loc = new Location(world,
                        dataConfig.getDouble("npc.x"), dataConfig.getDouble("npc.y"), dataConfig.getDouble("npc.z"),
                        (float) dataConfig.getDouble("npc.yaw"), 0);
                spawnNPC(loc);
            }, 40L);
        }
    }
}
