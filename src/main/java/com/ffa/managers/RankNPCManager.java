package com.ffa.managers;

import com.ffa.FFAPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Witch;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * RankNPCManager
 * Spawns a Witch NPC named "RANKS".
 * Right-clicking it sends the player a clickable chat link to the store URL
 * (configured in config.yml under rank-npc.store-url).
 */
public class RankNPCManager implements Listener {

    private final FFAPlugin    plugin;
    private final NamespacedKey NPC_KEY;

    private UUID      npcUUID;
    private Location  savedLocation;
    private File      npcFile;
    private FileConfiguration npcConfig;

    public RankNPCManager(FFAPlugin plugin) {
        this.plugin  = plugin;
        this.NPC_KEY = new NamespacedKey(plugin, "rank_npc");
        loadData();
    }

    // ── Spawn / Remove ──────────────────────────────────────────────

    public void spawnNPC(Location loc) {
        purgeTagged(loc.getWorld());
        savedLocation = loc.clone();

        Witch npc = (Witch) loc.getWorld().spawnEntity(loc, EntityType.WITCH);
        npc.setCustomName("§d§lRANKS");
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npc.getPersistentDataContainer().set(NPC_KEY, PersistentDataType.BYTE, (byte) 1);
        npcUUID = npc.getUniqueId();
        saveData(loc);
    }

    public void removeNPC() {
        if (savedLocation != null) purgeTagged(savedLocation.getWorld());
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
        clearData();
    }

    public void restoreNPC() {
        if (!npcConfig.contains("world")) return;
        World world = Bukkit.getWorld(npcConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
            npcConfig.getDouble("x"), npcConfig.getDouble("y"), npcConfig.getDouble("z"),
            (float) npcConfig.getDouble("yaw"), 0f);
        savedLocation = loc.clone();
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkAndRestore, 1200L, 1200L);
    }

    private void checkAndRestore() {
        if (savedLocation == null) return;
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null && !e.isDead() && e.isValid()) return;
        }
        if (savedLocation.getWorld() != null) {
            for (Entity entity : savedLocation.getWorld().getEntities()) {
                if (isNPC(entity)) { npcUUID = entity.getUniqueId(); return; }
            }
        }
        spawnNPC(savedLocation);
    }

    private void purgeTagged(World world) {
        if (world == null) return;
        for (Entity e : world.getEntities()) {
            if (e instanceof Witch w &&
                w.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE)) {
                w.remove();
            }
        }
        if (npcUUID != null) {
            Entity e = Bukkit.getEntity(npcUUID);
            if (e != null) e.remove();
            npcUUID = null;
        }
    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        if (npcUUID != null && entity.getUniqueId().equals(npcUUID)) return true;
        return entity instanceof Witch w &&
               w.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE);
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
        player.sendMessage(link);
        player.sendMessage("§8[§d§lRANKS§8] §7URL: §d" + url);
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (isNPC(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onTarget(EntityTargetEvent event) {
        if (isNPC(event.getEntity())) event.setCancelled(true);
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (savedLocation == null) return;
        if (!event.getWorld().equals(savedLocation.getWorld())) return;
        int cx = savedLocation.getBlockX() >> 4;
        int cz = savedLocation.getBlockZ() >> 4;
        if (event.getChunk().getX() != cx || event.getChunk().getZ() != cz) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (npcUUID != null) {
                Entity e = Bukkit.getEntity(npcUUID);
                if (e != null && !e.isDead() && e.isValid()) return;
            }
            spawnNPC(savedLocation);
        }, 10L);
    }

    // ── Persistence ─────────────────────────────────────────────────

    private void saveData(Location loc) {
        npcConfig.set("world", loc.getWorld().getName());
        npcConfig.set("x",   loc.getX());
        npcConfig.set("y",   loc.getY());
        npcConfig.set("z",   loc.getZ());
        npcConfig.set("yaw", (double) loc.getYaw());
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        npcFile = new File(plugin.getDataFolder(), "ranknpc.yml");
        if (!npcFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { npcFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        npcConfig = YamlConfiguration.loadConfiguration(npcFile);
    }

    private void clearData() {
        npcConfig.set("world", null);
        try { npcConfig.save(npcFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
