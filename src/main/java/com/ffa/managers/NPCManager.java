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

public class NPCManager {

    private final FFAPlugin plugin;
    private Location savedLocation;
    private File npcFile;
    private FileConfiguration npcConfig;

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        savedLocation = loc.clone();
        killAllKitNPCs();

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

        saveNPCData(loc);

        // Check every 30 seconds — same as RTP NPC
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (savedLocation == null) return;
            String name = plugin.getConfig().getString("npc.name", "§6§lKit");
            boolean found = false;
            for (Entity e : savedLocation.getWorld().getEntities()) {
                if (e instanceof Breeze && name.equals(e.getCustomName()) && e.isValid()) {
                    found = true;
                    break;
                }
            }
            if (!found) spawnNPC(savedLocation);
        }, 600L, 600L);
    }

    private void killAllKitNPCs() {
        String name = plugin.getConfig().getString("npc.name", "§6§lKit");
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Breeze && name.equals(e.getCustomName())) {
                    e.remove();
                }
            }
        }
    }

    public void removeNPC() {
        killAllKitNPCs();
        savedLocation = null;
        clearNPCData();
    }

    public void restoreNPC() {
        if (!npcConfig.contains("world")) return;
        World world = Bukkit.getWorld(npcConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
            npcConfig.getDouble("x"), npcConfig.getDouble("y"), npcConfig.getDouble("z"),
            (float) npcConfig.getDouble("yaw"), 0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);
    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        String name = plugin.getConfig().getString("npc.name", "§6§lKit");
        return entity instanceof Breeze && name.equals(entity.getCustomName());
    }

    private void saveNPCData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x",   loc.getX());
        npcConfig.set("y",   loc.getY());
        npcConfig.set("z",   loc.getZ());
        npcConfig.set("yaw", (double) loc.getYaw());
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
}
