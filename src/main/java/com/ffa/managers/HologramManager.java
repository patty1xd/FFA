package com.ffa.managers;

import com.ffa.FFAPlugin;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.TextDisplay;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class HologramManager {

    private final FFAPlugin plugin;
    private final Map<String, List<TextDisplay>> holograms = new HashMap<>(); // type -> display entities
    private File holoFile;
    private FileConfiguration holoConfig;

    // Line spacing between hologram lines
    private static final double LINE_SPACING = 0.28;

    public HologramManager(FFAPlugin plugin) {
        this.plugin = plugin;
        loadHoloData();
        // Update holograms every 30 seconds
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAll, 200L, 600L);
    }

    public void spawnHologram(String type, Location loc) {
        removeHologram(type);
        List<String> lines = buildLines(type);
        List<TextDisplay> displays = new ArrayList<>();
        double y = loc.getY() + (lines.size() * LINE_SPACING);

        for (String line : lines) {
            Location lineLoc = loc.clone();
            lineLoc.setY(y);
            TextDisplay display = (TextDisplay) loc.getWorld().spawnEntity(lineLoc, EntityType.TEXT_DISPLAY);
            display.text(LegacyComponentSerializer.legacySection().deserialize(line));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setDefaultBackground(false);
            displays.add(display);
            y -= LINE_SPACING;
        }

        holograms.put(type, displays);
        saveHoloData(type, loc);
    }

    public void removeHologram(String type) {
        List<TextDisplay> existing = holograms.remove(type);
        if (existing != null) existing.forEach(e -> { if (!e.isDead()) e.remove(); });
    }

    public void updateAll() {
        for (String type : new HashSet<>(holograms.keySet())) {
            List<TextDisplay> displays = holograms.get(type);
            if (displays == null || displays.isEmpty()) continue;
            List<String> lines = buildLines(type);
            for (int i = 0; i < Math.min(lines.size(), displays.size()); i++) {
                TextDisplay d = displays.get(i);
                if (d.isDead()) continue;
                d.text(LegacyComponentSerializer.legacySection().deserialize(lines.get(i)));
            }
        }
    }

    private List<String> buildLines(String type) {
        List<String> lines = new ArrayList<>();
        StatsManager stats = plugin.getStatsManager();

        switch (type) {
            case "kills" -> {
                lines.add("§6§l⚔ TOP KILLS ⚔");
                lines.add("§8▬▬▬▬▬▬▬▬▬▬▬");
                List<Map.Entry<UUID, Integer>> top = stats.getTopKills(10);
                int rank = 1;
                for (var entry : top) {
                    String medal = getRankPrefix(rank);
                    lines.add(medal + stats.getPlayerName(entry.getKey()) + " §7- §e" + entry.getValue());
                    rank++;
                }
                if (top.size() < 10) {
                    for (int i = top.size(); i < 10; i++) lines.add("§8" + (i + 1) + ". §7---");
                }
            }
            case "deaths" -> {
                lines.add("§c§l💀 TOP DEATHS 💀");
                lines.add("§8▬▬▬▬▬▬▬▬▬▬▬");
                List<Map.Entry<UUID, Integer>> top = stats.getTopDeaths(10);
                int rank = 1;
                for (var entry : top) {
                    String medal = getRankPrefix(rank);
                    lines.add(medal + stats.getPlayerName(entry.getKey()) + " §7- §c" + entry.getValue());
                    rank++;
                }
                if (top.size() < 10) {
                    for (int i = top.size(); i < 10; i++) lines.add("§8" + (i + 1) + ". §7---");
                }
            }
            case "kd" -> {
                lines.add("§a§l📊 TOP K/D 📊");
                lines.add("§8▬▬▬▬▬▬▬▬▬▬▬");
                List<Map.Entry<UUID, Double>> top = stats.getTopKD(10);
                int rank = 1;
                for (var entry : top) {
                    String medal = getRankPrefix(rank);
                    lines.add(medal + stats.getPlayerName(entry.getKey()) + " §7- §a" + entry.getValue());
                    rank++;
                }
                if (top.size() < 10) {
                    for (int i = top.size(); i < 10; i++) lines.add("§8" + (i + 1) + ". §7---");
                }
            }
        }
        return lines;
    }

    private String getRankPrefix(int rank) {
        return switch (rank) {
            case 1 -> "§6§l#1 §f";
            case 2 -> "§7§l#2 §f";
            case 3 -> "§c§l#3 §f";
            default -> "§8#" + rank + " §7";
        };
    }

    public boolean hasHologram(String type) { return holograms.containsKey(type); }

    private void saveHoloData(String type, Location loc) {
        holoConfig.set("holograms." + type + ".world", loc.getWorld().getName());
        holoConfig.set("holograms." + type + ".x", loc.getX());
        holoConfig.set("holograms." + type + ".y", loc.getY());
        holoConfig.set("holograms." + type + ".z", loc.getZ());
        try { holoConfig.save(holoFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void loadHoloData() {
        holoFile = new File(plugin.getDataFolder(), "holograms.yml");
        if (!holoFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { holoFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        holoConfig = YamlConfiguration.loadConfiguration(holoFile);

        if (!holoConfig.isConfigurationSection("holograms")) return;

        // Restore holograms on startup with a delay for world loading
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (String type : holoConfig.getConfigurationSection("holograms").getKeys(false)) {
                String worldName = holoConfig.getString("holograms." + type + ".world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                double x = holoConfig.getDouble("holograms." + type + ".x");
                double y = holoConfig.getDouble("holograms." + type + ".y");
                double z = holoConfig.getDouble("holograms." + type + ".z");
                spawnHologram(type, new Location(world, x, y, z));
            }
        }, 60L);
    }
}
