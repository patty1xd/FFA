package com.ffa.managers;

import com.ffa.FFAPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class RankNPCManager implements Listener {

    private final FFAPlugin plugin;
    private static final String NPC_NAME = "§d§lRANKS";

    private Location savedLocation;
    private UUID npcUUID;
    private boolean respawnScheduled = false;
    private File dataFile;
    private FileConfiguration dataConfig;

    public RankNPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        startWatchdog();
    }

    // ── Spawn / Remove ───────────────────────────────────────────────

    public void spawnNPC(Location loc) {
        savedLocation = loc.clone();
        removeExistingNPC();

        Witch npc = loc.getWorld().spawn(loc, Witch.class, entity -> {
            entity.setCustomName(NPC_NAME);
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
        saveData(loc);
    }

    public void removeNPC() {
        releaseChunkTicket();
        removeExistingNPC();
        savedLocation = null;
        clearData();
    }

    // ── Restore on startup ───────────────────────────────────────────

    public void restoreNPC() {
        if (!dataConfig.contains("world")) return;

        if (dataConfig.contains("uuid")) {
            try {
                UUID uuid = UUID.fromString(dataConfig.getString("uuid", ""));
                Entity entity = Bukkit.getEntity(uuid);
                if (entity instanceof Witch && entity.isValid()) {
                    npcUUID = uuid;
                    savedLocation = entity.getLocation().clone();
                    Witch witch = (Witch) entity;
                    witch.setAI(false);
                    witch.setGravity(false);
                    witch.setInvulnerable(true);
                    witch.setSilent(true);
                    witch.setPersistent(true);
                    witch.setRemoveWhenFarAway(false);
                    keepChunkLoaded(savedLocation);
                    return;
                }
            } catch (IllegalArgumentException ignored) {}
        }

        World world = Bukkit.getWorld(dataConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
                dataConfig.getDouble("x"), dataConfig.getDouble("y"), dataConfig.getDouble("z"),
                (float) dataConfig.getDouble("yaw"), 0);
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

    // ── Events ──────────────────────────────────────────────────────

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!isNPC(event.getRightClicked())) return;
        event.setCancelled(true);
        Player player = event.getPlayer();
        String url = plugin.getConfig().getString("rank-npc.store-url", "https://store.example.com");

        player.sendMessage("§8[§d§lRANKS§8] §7Click below to visit the store:");
        Component link = Component.text("  ➜ Click here to buy a rank!", NamedTextColor.LIGHT_PURPLE)
                .decorate(TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.openUrl(url));
    }

    // ── Internal helpers ─────────────────────────────────────────────

    private void startWatchdog() {
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
        // Safety net: kill any stray witch with the rank NPC name
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Witch && NPC_NAME.equals(e.getCustomName())) {
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

    private void saveData(Location loc) {
        dataConfig.set("uuid",  npcUUID != null ? npcUUID.toString() : null);
        dataConfig.set("world", loc.getWorld().getName());
        dataConfig.set("x",     loc.getX());
        dataConfig.set("y",     loc.getY());
        dataConfig.set("z",     loc.getZ());
        dataConfig.set("yaw",   (double) loc.getYaw());
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "ranknpc.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void clearData() {
        dataConfig.set("world", null);
        dataConfig.set("uuid",  null);
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
