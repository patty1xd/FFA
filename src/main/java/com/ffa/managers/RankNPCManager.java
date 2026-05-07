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
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Witch;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.io.File;
import java.io.IOException;

public class RankNPCManager implements Listener {

    private final FFAPlugin plugin;
    private static final String NPC_NAME = "§d§lRANKS";

    private Location savedLocation;
    private File dataFile;
    private FileConfiguration dataConfig;

    public RankNPCManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
    }

    public void spawnNPC(Location loc) {
        savedLocation = loc.clone();
        killAllRankNPCs();

        Witch npc = (Witch) loc.getWorld().spawnEntity(loc, EntityType.WITCH);
        npc.setCustomName(NPC_NAME);
        npc.setCustomNameVisible(true);
        npc.setAI(false);
        npc.setInvulnerable(true);
        npc.setSilent(true);
        npc.setPersistent(true);
        npc.setRemoveWhenFarAway(false);

        saveData(loc);

        // Check every 30 seconds — same as RTP NPC
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (savedLocation == null) return;
            boolean found = false;
            for (Entity e : savedLocation.getWorld().getEntities()) {
                if (e instanceof Witch && NPC_NAME.equals(e.getCustomName()) && e.isValid()) {
                    found = true;
                    break;
                }
            }
            if (!found) spawnNPC(savedLocation);
        }, 600L, 600L);
    }

    private void killAllRankNPCs() {
        for (World w : Bukkit.getWorlds()) {
            for (Entity e : w.getEntities()) {
                if (e instanceof Witch && NPC_NAME.equals(e.getCustomName())) {
                    e.remove();
                }
            }
        }
    }

    public void removeNPC() {
        killAllRankNPCs();
        savedLocation = null;
        clearData();
    }

    public void restoreNPC() {
        if (!dataConfig.contains("world")) return;
        World world = Bukkit.getWorld(dataConfig.getString("world", "world"));
        if (world == null) return;
        Location loc = new Location(world,
            dataConfig.getDouble("x"), dataConfig.getDouble("y"), dataConfig.getDouble("z"),
            (float) dataConfig.getDouble("yaw"), 0);
        Bukkit.getScheduler().runTaskLater(plugin, () -> spawnNPC(loc), 40L);
    }

    public boolean isNPC(Entity entity) {
        if (entity == null) return false;
        return entity instanceof Witch && NPC_NAME.equals(entity.getCustomName());
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

    // ── Persistence ──────────────────────────────────────────────────

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
