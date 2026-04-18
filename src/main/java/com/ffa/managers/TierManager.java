package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TierManager {

    private final FFAPlugin plugin;
    private final Map<UUID, Integer> playerTier = new HashMap<>();
    private final Map<UUID, Integer> tierKills = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    private static final int[] KILLS_TO_ADVANCE = {0, 2, 3, 4, 5, 0};
    public static final int MAX_TIER = 5;

    public TierManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    public int getTier(UUID uuid) { return playerTier.getOrDefault(uuid, 1); }
    public int getTierKills(UUID uuid) { return tierKills.getOrDefault(uuid, 0); }

    public void initPlayer(UUID uuid) {
        if (!playerTier.containsKey(uuid)) {
            playerTier.put(uuid, 1);
            tierKills.put(uuid, 0);
        }
    }

    public void addKill(UUID uuid) {
        int tier = getTier(uuid);
        if (tier >= MAX_TIER) return;
        int kills = getTierKills(uuid) + 1;
        int needed = KILLS_TO_ADVANCE[tier];
        if (kills >= needed) {
            playerTier.put(uuid, tier + 1);
            tierKills.put(uuid, 0);
            var player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage("§8[§6FFA§8] §a§lTIER UP! §7You are now " + getTierDisplay(tier + 1) + "§7!");
                plugin.getKitManager().giveKit(player);
            }
        } else {
            tierKills.put(uuid, kills);
            var player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage("§8[§6FFA§8] §e" + kills + "§7/§e" + needed + " §7kills to advance!");
            }
        }
        plugin.getBoardManager().updatePlayer(Bukkit.getPlayer(uuid));
    }

    public void onDeath(UUID uuid) {
        int tier = getTier(uuid);
        if (tier <= 1) {
            tierKills.put(uuid, 0);
            var player = Bukkit.getPlayer(uuid);
            if (player != null) plugin.getKitManager().giveKit(player);
            return;
        }
        playerTier.put(uuid, tier - 1);
        tierKills.put(uuid, 0);
        var player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.sendMessage("§8[§6FFA§8] §c§lTIER DOWN! §7You dropped to " + getTierDisplay(tier - 1) + "§7.");
            plugin.getKitManager().giveKit(player);
        }
        plugin.getBoardManager().updatePlayer(player);
    }

    public void setTier(UUID uuid, int tier) {
        playerTier.put(uuid, Math.min(MAX_TIER, Math.max(1, tier)));
        tierKills.put(uuid, 0);
        plugin.getBoardManager().updatePlayer(Bukkit.getPlayer(uuid));
    }

    public void resetPlayer(UUID uuid) { setTier(uuid, 1); }

    public String getTierDisplay(int tier) {
        return switch (tier) {
            case 1 -> "§7[T1]";
            case 2 -> "§a[T2]";
            case 3 -> "§b[T3]";
            case 4 -> "§d[T4]";
            case 5 -> "§6§l[T5★]";
            default -> "§7[T1]";
        };
    }

    public int getKillsNeeded(int tier) {
        if (tier >= MAX_TIER) return 0;
        return KILLS_TO_ADVANCE[tier];
    }

    public void saveAll() {
        dataConfig.set("tiers", null);
        dataConfig.set("kills", null);
        for (Map.Entry<UUID, Integer> e : playerTier.entrySet())
            dataConfig.set("tiers." + e.getKey(), e.getValue());
        for (Map.Entry<UUID, Integer> e : tierKills.entrySet())
            dataConfig.set("kills." + e.getKey(), e.getValue());
        try { dataConfig.save(dataFile); } catch (IOException ex) { ex.printStackTrace(); }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "playerdata.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.isConfigurationSection("tiers"))
            for (String k : dataConfig.getConfigurationSection("tiers").getKeys(false))
                playerTier.put(UUID.fromString(k), dataConfig.getInt("tiers." + k));
        if (dataConfig.isConfigurationSection("kills"))
            for (String k : dataConfig.getConfigurationSection("kills").getKeys(false))
                tierKills.put(UUID.fromString(k), dataConfig.getInt("kills." + k));
    }
}
