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

    public static final int MAX_TIER = 5;

    public TierManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    public int getTier(UUID uuid) { return playerTier.getOrDefault(uuid, 1); }
    public int getTierKills(UUID uuid) { return tierKills.getOrDefault(uuid, 0); }

    public int getKillsNeeded(int tier) {
        return plugin.getConfig().getInt("tiers." + tier + ".kills-to-advance", 0);
    }

    public String getTierDisplay(int tier) {
        return plugin.getConfig().getString("tiers." + tier + ".display", "§7[T" + tier + "]");
    }

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
        int needed = getKillsNeeded(tier);
        if (kills >= needed) {
            playerTier.put(uuid, tier + 1);
            tierKills.put(uuid, 0);
            var player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String msg = plugin.getConfig().getString("messages.tier-up", "§aTIER UP! {tier}")
                    .replace("{tier}", getTierDisplay(tier + 1));
                player.sendMessage(msg);
                plugin.getKitManager().giveKit(player);
                plugin.getBoardManager().updateNameTag(player);
            }
        } else {
            tierKills.put(uuid, kills);
            var player = Bukkit.getPlayer(uuid);
            if (player != null) {
                String msg = plugin.getConfig().getString("messages.kill-progress", "§e{kills}§7/§e{needed}")
                    .replace("{kills}", String.valueOf(kills))
                    .replace("{needed}", String.valueOf(needed));
                player.sendMessage(msg);
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
            String msg = plugin.getConfig().getString("messages.tier-down", "§cTIER DOWN! {tier}")
                .replace("{tier}", getTierDisplay(tier - 1));
            player.sendMessage(msg);
            plugin.getKitManager().giveKit(player);
            plugin.getBoardManager().updateNameTag(player);
        }
        plugin.getBoardManager().updatePlayer(player);
    }

    public void setTier(UUID uuid, int tier) {
        playerTier.put(uuid, Math.min(MAX_TIER, Math.max(1, tier)));
        tierKills.put(uuid, 0);
        var player = Bukkit.getPlayer(uuid);
        plugin.getBoardManager().updatePlayer(player);
        plugin.getBoardManager().updateNameTag(player);
    }

    public void resetPlayer(UUID uuid) { setTier(uuid, 1); }

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
