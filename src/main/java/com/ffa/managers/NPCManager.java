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

public class NPCManager implements Listener {

    private final FFAPlugin plugin;
    private Location savedLocation;
    private File npcFile;
    private FileConfiguration npcConfig;
    private static final String NPC_NAME = "§6§lKit";

    public NPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadNPCData();
    }

    public void spawnNPC(Location loc) {
        savedLocation = loc.clone();
        killAllKitNPCs();

        Breeze npc = (Breeze) loc.getWorld().spawnEntity(loc, EntityType.BREEZE);
        npc.setCustomName(NPC_NAME);
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setGravity(false);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);

        saveNPCData(loc);
        plugin.getLogger().info("Kit NPC spawned at " + loc);
    }

   // Check every 30 seconds

        Bukkit.getScheduler().runTaskTimer(plugin, () -> {

            if (npcSavedLocation == null) return;

            boolean found = false;

            for (Entity e : npcSavedLocation.getWorld().getEntities()) {

                if (e instanceof Breeze && NPC_NAME.equals(e.getCustomName()) && e.isValid()) {

                    found = true;

                    break;

                }

            }

            if (!found) spawnNPC(npcSavedLocation);

        }, 600L, 600L);

    }

    private void killAllNPCs() {

        for (World w : Bukkit.getWorlds()) {

            for (Entity e : w.getEntities()) {

                if (e instanceof Husk && NPC_NAME.equals(e.getCustomName())) {

                    e.remove();

                }

            }

        }

    }

    public void removeNPC() {

        killAllNPCs();

        npcSavedLocation = null;

    }

    public boolean isNPC(Entity entity) {

        if (entity == null) return false;

        return entity instanceof Husk && NPC_NAME.equals(entity.getCustomName());

    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        return entity instanceof Breeze && NPC_NAME.equals(entity.getCustomName());
    }

    private void saveNPCData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x", loc.getX());
        npcConfig.set("y", loc.getY());
        npcConfig.set("z", loc.getZ());
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
