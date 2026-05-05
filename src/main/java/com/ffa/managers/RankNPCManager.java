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
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * RankNPCManager
 * A simple persistent witch NPC named "RANKS".
 * Behaves exactly like a mob with a name tag — won't despawn naturally.
 * Chunk loading is handled by Minecraft itself (persistent=true, removeWhenFarAway=false).
 * Right-clicking sends the player a clickable store link.
 */
public class RankNPCManager implements Listener {

    private final FFAPlugin plugin;
    private final NamespacedKey NPC_KEY;

    private File              dataFile;
    private FileConfiguration dataConfig;

    public RankNPCManager(FFAPlugin plugin) {
        this.plugin  = plugin;
        this.NPC_KEY = new NamespacedKey(plugin, "rank_npc");
        loadData();
    }

    // ── Spawn ───────────────────────────────────────────────────────

    public void spawnNPC(Location loc) {
        // Remove any existing tagged witches first
        removeExisting(loc.getWorld());

        Witch npc = (Witch) loc.getWorld().spawnEntity(loc, EntityType.WITCH);
        npc.setCustomName("§d§lRANKS");
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        // These two are the key — same effect as a name tag on a mob
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);
        npc.getPersistentDataContainer().set(NPC_KEY, PersistentDataType.BYTE, (byte) 1);

        saveData(loc);
        plugin.getLogger().info("RANKS NPC spawned.");
    }

    public void removeNPC() {
        if (!dataConfig.contains("world")) return;
        World world = Bukkit.getWorld(dataConfig.getString("world", "world"));
        if (world != null) removeExisting(world);
        clearData();
    }

    /** Called once on startup to tag-scan loaded entities. No respawn loop needed. */
    public void restoreNPC() {
        // Nothing to do — persistent=true means the mob survives restarts automatically.
        // It will be in the world's entity list when the chunk loads.
        // We just register our listener so right-clicks work.
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

    // ── Helpers ─────────────────────────────────────────────────────

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        return entity instanceof Witch &&
               entity.getPersistentDataContainer().has(NPC_KEY, PersistentDataType.BYTE);
    }

    private void removeExisting(World world) {
        if (world == null) return;
        world.getEntities().stream()
            .filter(this::isNPC)
            .forEach(Entity::remove);
    }

    // ── Persistence (just saves spawn location for /spawnranknpc re-use) ──

    private void saveData(Location loc) {
        dataConfig.set("world", loc.getWorld().getName());
        dataConfig.set("x",   loc.getX());
        dataConfig.set("y",   loc.getY());
        dataConfig.set("z",   loc.getZ());
        dataConfig.set("yaw", (double) loc.getYaw());
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
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }
}
