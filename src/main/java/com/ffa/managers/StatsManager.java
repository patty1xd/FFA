package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class StatsManager {

    private final FFAPlugin plugin;
    private final Map<UUID, Integer> kills = new HashMap<>();
    private final Map<UUID, Integer> deaths = new HashMap<>();
    private final Map<UUID, Integer> currentStreak = new HashMap<>();
    private final Map<UUID, Integer> bestStreak = new HashMap<>();
    private final Map<UUID, String> playerNames = new HashMap<>();
    private File dataFile;
    private FileConfiguration dataConfig;

    public StatsManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadData();
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAll, 6000L, 6000L);
    }

    public void initPlayer(UUID uuid, String name) {
        playerNames.put(uuid, name);
        kills.putIfAbsent(uuid, 0);
        deaths.putIfAbsent(uuid, 0);
        currentStreak.putIfAbsent(uuid, 0);
        bestStreak.putIfAbsent(uuid, 0);
    }

    public void addKill(UUID uuid) {
        kills.merge(uuid, 1, Integer::sum);
        int streak = currentStreak.merge(uuid, 1, Integer::sum);
        if (streak > bestStreak.getOrDefault(uuid, 0)) {
            bestStreak.put(uuid, streak);
        }
        checkStreak(uuid, streak);
    }

    public void addDeath(UUID uuid) {
        deaths.merge(uuid, 1, Integer::sum);
        currentStreak.put(uuid, 0);
    }

    private void checkStreak(UUID uuid, int streak) {
        List<Integer> milestones = plugin.getConfig().getIntegerList("streaks.milestones");
        if (milestones.isEmpty()) milestones = Arrays.asList(5, 10, 15, 20);
        if (!milestones.contains(streak)) return;

        String playerName = playerNames.getOrDefault(uuid, "Unknown");
        String format = plugin.getConfig().getString("streaks.message",
            "&6⚡ &e{player} &6is on a &c{streak} kill streak&6!")
            .replace("{player}", playerName)
            .replace("{streak}", String.valueOf(streak))
            .replace("&", "§");
        Bukkit.broadcastMessage(format);
    }

    public int getKills(UUID uuid) { return kills.getOrDefault(uuid, 0); }
    public int getDeaths(UUID uuid) { return deaths.getOrDefault(uuid, 0); }
    public int getCurrentStreak(UUID uuid) { return currentStreak.getOrDefault(uuid, 0); }
    public int getBestStreak(UUID uuid) { return bestStreak.getOrDefault(uuid, 0); }
    public String getPlayerName(UUID uuid) { return playerNames.getOrDefault(uuid, "Unknown"); }

    public double getKD(UUID uuid) {
        int d = getDeaths(uuid);
        if (d == 0) return getKills(uuid);
        return Math.round((getKills(uuid) / (double) d) * 100.0) / 100.0;
    }

    public List<Map.Entry<UUID, Integer>> getTopKills(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(kills.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<UUID, Integer>> getTopDeaths(int limit) {
        List<Map.Entry<UUID, Integer>> list = new ArrayList<>(deaths.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        return list.subList(0, Math.min(limit, list.size()));
    }

    public List<Map.Entry<UUID, Double>> getTopKD(int limit) {
        List<Map.Entry<UUID, Double>> list = new ArrayList<>();
        for (UUID uuid : kills.keySet()) {
            if (getKills(uuid) < 5) continue; // min kills to appear on KD board
            list.add(Map.entry(uuid, getKD(uuid)));
        }
        list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        return list.subList(0, Math.min(limit, list.size()));
    }

    public void saveAll() {
        dataConfig.set("kills", null);
        dataConfig.set("deaths", null);
        dataConfig.set("streaks", null);
        dataConfig.set("names", null);
        for (UUID uuid : kills.keySet()) {
            String k = uuid.toString();
            dataConfig.set("kills." + k, kills.get(uuid));
            dataConfig.set("deaths." + k, deaths.getOrDefault(uuid, 0));
            dataConfig.set("streaks." + k, bestStreak.getOrDefault(uuid, 0));
            dataConfig.set("names." + k, playerNames.get(uuid));
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadData() {
        dataFile = new File(plugin.getDataFolder(), "stats.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (dataConfig.isConfigurationSection("kills"))
            for (String k : dataConfig.getConfigurationSection("kills").getKeys(false)) {
                UUID uuid = UUID.fromString(k);
                kills.put(uuid, dataConfig.getInt("kills." + k));
                deaths.put(uuid, dataConfig.getInt("deaths." + k, 0));
                bestStreak.put(uuid, dataConfig.getInt("streaks." + k, 0));
                currentStreak.put(uuid, 0);
                playerNames.put(uuid, dataConfig.getString("names." + k, "Unknown"));
            }
    }
}
