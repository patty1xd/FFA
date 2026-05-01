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
    private final Map<UUID, Double> tierKills = new HashMap<>(); // double to support fractional progress
    private File dataFile;
    private FileConfiguration dataConfig;

    public static final int MAX_TIER = 4;

    public TierManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    public int getTier(UUID uuid) { return playerTier.getOrDefault(uuid, 1); }
    public int getTierKills(UUID uuid) { return (int) Math.floor(tierKills.getOrDefault(uuid, 0.0)); }
    public double getTierKillsExact(UUID uuid) { return tierKills.getOrDefault(uuid, 0.0); }

    public int getKillsNeeded(int tier) {
        return plugin.getConfig().getInt("tiers." + tier + ".kills-to-advance", 0);
    }

    public String getTierDisplay(int tier) {
        return plugin.getConfig().getString("tiers." + tier + ".display", "§7[T" + tier + "]")
            .replace("&", "§");
    }

    public void initPlayer(UUID uuid) {
        if (!playerTier.containsKey(uuid)) {
            playerTier.put(uuid, 1);
            tierKills.put(uuid, 0.0);
        }
    }

    public void addKill(UUID killerUUID, UUID victimUUID) {
        int killerTier = getTier(killerUUID);
        int victimTier = getTier(victimUUID);

        String restriction = plugin.getConfig().getString(
            "kill-restrictions." + killerTier + "." + victimTier, "progress:1");

        var killer = Bukkit.getPlayer(killerUUID);

        if (restriction.equalsIgnoreCase("none")) {
            if (killer != null) killer.sendMessage(
                plugin.getConfig().getString("messages.kill-no-progress",
                "§8[§6FFA§8] §7This kill gives no progress."));
            return;
        }

        if (restriction.toLowerCase().startsWith("tier:")) {
            // Instant tier change
            try {
                int tiers = Integer.parseInt(restriction.split(":")[1]);
                int newTier = Math.min(MAX_TIER, Math.max(1, killerTier + tiers));
                playerTier.put(killerUUID, newTier);
                tierKills.put(killerUUID, 0.0);
                if (killer != null) {
                    if (tiers > 0) {
                        killer.sendMessage("§8[§6FFA§8] §a§lTIER UP x" + tiers + "! §7You are now " + getTierDisplay(newTier) + "§7!");
                    } else {
                        killer.sendMessage("§8[§6FFA§8] §c§lTIER DOWN! §7You are now " + getTierDisplay(newTier) + "§7.");
                    }
                    plugin.getKitManager().upgradeKit(killer);
                    plugin.getBoardManager().updateNameTag(killer);
                }
                plugin.getBoardManager().updatePlayer(killer);
            } catch (Exception e) { e.printStackTrace(); }
            return;
        }

        // progress:X format (or legacy "full"/"reduced:X")
        double progress = 1.0;
        if (restriction.toLowerCase().startsWith("progress:")) {
            try { progress = Double.parseDouble(restriction.split(":")[1]); } catch (Exception e) { progress = 1.0; }
        } else if (restriction.toLowerCase().startsWith("reduced:")) {
            try { progress = Double.parseDouble(restriction.split(":")[1]); } catch (Exception e) { progress = 1.0; }
        } else if (restriction.equalsIgnoreCase("full")) {
            progress = 1.0;
        }

        if (killerTier >= MAX_TIER) return;

        double currentKills = getTierKillsExact(killerUUID) + progress;
        int needed = getKillsNeeded(killerTier);

        if (currentKills >= needed) {
            playerTier.put(killerUUID, killerTier + 1);
            tierKills.put(killerUUID, 0.0);
            if (killer != null) {
                String msg = plugin.getConfig().getString("messages.tier-up", "§aTIER UP! {tier}")
                    .replace("{tier}", getTierDisplay(killerTier + 1));
                killer.sendMessage(msg);
                plugin.getKitManager().upgradeKit(killer);
                plugin.getBoardManager().updateNameTag(killer);
            }
        } else {
            tierKills.put(killerUUID, currentKills);
            if (killer != null) {
                String msgKey = progress < 1.0 ? "kill-reduced-progress" : "kill-progress";
                String msg = plugin.getConfig().getString("messages." + msgKey,
                    "§e{kills}§7/§e{needed} §7kill progress!")
                    .replace("{kills}", String.valueOf(getTierKills(killerUUID)))
                    .replace("{needed}", String.valueOf(needed));
                killer.sendMessage(msg);
            }
        }
        plugin.getBoardManager().updatePlayer(killer);
    }

    public void onDeath(UUID victimUUID, int killerTier) {
        int victimTier = getTier(victimUUID);
        String consequence = plugin.getConfig().getString(
            "death-consequences." + victimTier + "." + killerTier, "lose-tier");

        var player = Bukkit.getPlayer(victimUUID);

        switch (consequence.toLowerCase()) {
            case "nothing" -> {
                if (player != null) player.sendMessage("§8[§6FFA§8] §7No tier penalty this death.");
            }
            case "lose-progress" -> {
                tierKills.put(victimUUID, 0.0);
                if (player != null) {
                    String msg = "§8[§6FFA§8] §cYou lost your kill progress!";
                    player.sendMessage(msg);
                }
            }
            default -> { // lose-tier
                if (victimTier <= 1) {
                    tierKills.put(victimUUID, 0.0);
                    if (player != null) plugin.getKitManager().giveKit(player);
                    return;
                }
                playerTier.put(victimUUID, victimTier - 1);
                tierKills.put(victimUUID, 0.0);
                if (player != null) {
                    String msg = plugin.getConfig().getString("messages.tier-down", "§cTIER DOWN! {tier}")
                        .replace("{tier}", getTierDisplay(victimTier - 1));
                    player.sendMessage(msg);
                    plugin.getKitManager().giveKit(player);
                    plugin.getBoardManager().updateNameTag(player);
                }
                plugin.getBoardManager().updatePlayer(player);
            }
        }
    }

    // Overload for environmental deaths (no killer)
    public void onDeath(UUID victimUUID) {
        onDeath(victimUUID, getTier(victimUUID)); // treat as same tier
    }

    public void setTier(UUID uuid, int tier) {
        playerTier.put(uuid, Math.min(MAX_TIER, Math.max(1, tier)));
        tierKills.put(uuid, 0.0);
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
        for (Map.Entry<UUID, Double> e : tierKills.entrySet())
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
                tierKills.put(UUID.fromString(k), dataConfig.getDouble("kills." + k));
    }
}
