package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * RankManager
 * Tracks donor rank expiry, GUI save charges, chosen chat color,
 * and custom sword name + color for each player.
 *
 * Save charges by purchase duration:
 *   7  days ->  2 saves
 *   14 days ->  3 saves
 *   30 days ->  5 saves
 *   90 days -> 15 saves
 */
public class RankManager {

    private final FFAPlugin plugin;

    // UUID -> expiry epoch-ms (absent or 0 = no rank)
    private final Map<UUID, Long>    expiry     = new HashMap<>();
    // UUID -> saves remaining
    private final Map<UUID, Integer> savesLeft  = new HashMap<>();
    // UUID -> chat color code e.g. "§b"
    private final Map<UUID, String>  chatColor  = new HashMap<>();
    // UUID -> raw sword name text (no color prefix)
    private final Map<UUID, String>  swordName  = new HashMap<>();
    // UUID -> sword name color code e.g. "§6"
    private final Map<UUID, String>  swordColor = new HashMap<>();

    private File              dataFile;
    private FileConfiguration dataConfig;

    /** Duration (days) -> number of saves granted on purchase. */
    public static final Map<Integer, Integer> SAVES_FOR_DAYS = Map.of(
        7,  2,
        14, 3,
        30, 5,
        90, 15
    );

    public RankManager(FFAPlugin plugin) {
        this.plugin = plugin;
        load();
        // Auto-save every 5 min
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::save, 6000L, 6000L);
        // Expiry check every 60 s on main thread so we can call Bukkit API
        Bukkit.getScheduler().runTaskTimer(plugin, this::checkExpiry, 1200L, 1200L);
    }

    // ── Public API ─────────────────────────────────────────────────

    /** True if the player currently has an active rank. */
    public boolean hasRank(UUID uuid) {
        Long exp = expiry.get(uuid);
        if (exp == null || exp == 0L) return false;
        return System.currentTimeMillis() < exp;
    }

    public long getExpiry(UUID uuid) {
        return expiry.getOrDefault(uuid, 0L);
    }

    /**
     * Grant the rank for {@code days} days.
     * Stacks on any remaining time the player already has.
     * Sets tier to MAX (4) and re-gives kit immediately.
     */
    public void grantRank(UUID uuid, int days) {
        long now  = System.currentTimeMillis();
        long base = hasRank(uuid) ? expiry.get(uuid) : now;
        expiry.put(uuid, base + (long) days * 86_400_000L);

        int charges = SAVES_FOR_DAYS.getOrDefault(days, 2);
        savesLeft.put(uuid, getSavesLeft(uuid) + charges);

        plugin.getTierManager().setTier(uuid, TierManager.MAX_TIER);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            plugin.getKitManager().giveKit(player);
            player.sendMessage("§8[§6FFA§8] §d§lRANK ACTIVATED! §7You are now §dDonor §7for §e" + days + " §7day(s).");
            player.sendMessage("§8[§6FFA§8] §7You have §e" + getSavesLeft(uuid)
                    + " §7customisation save(s). Use §e/trims §7to set up your perks!");
        }
        save();
    }

    /**
     * Revokes the rank (called by expiry check or Tebex expiry command).
     * Strips chat color, sword name, and re-gives clean kit.
     */
    public void revokeRank(UUID uuid) {
        if (!hasRank(uuid)) return;
        expiry.put(uuid, 0L);
        chatColor.remove(uuid);
        swordName.remove(uuid);
        swordColor.remove(uuid);

        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            // Re-give kit without trims (TrimManager.applyTrims not called -> clean armor)
            plugin.getKitManager().giveKit(player);
            player.sendMessage("§8[§6FFA§8] §cYour donor rank has expired. Trims and perks removed.");
        }
        save();
    }

    public int getSavesLeft(UUID uuid) {
        return savesLeft.getOrDefault(uuid, 0);
    }

    /** Consume one save charge. Returns false if none remaining. */
    public boolean consumeSave(UUID uuid) {
        int left = getSavesLeft(uuid);
        if (left <= 0) return false;
        savesLeft.put(uuid, left - 1);
        save();
        return true;
    }

    // ── Chat color ──────────────────────────────────────────────────

    /** Returns the player's chosen chat color code, or empty string if no rank. */
    public String getChatColor(UUID uuid) {
        if (!hasRank(uuid)) return "";
        return chatColor.getOrDefault(uuid, "");
    }

    /** Store chosen chat color (saving triggered separately by GUI). */
    public void setChatColor(UUID uuid, String colorCode) {
        chatColor.put(uuid, colorCode);
    }

    // ── Sword name ──────────────────────────────────────────────────

    /** Returns the colored sword name e.g. "§6Slaughter Blade", or null if not set. */
    public String getColoredSwordName(UUID uuid) {
        if (!hasRank(uuid)) return null;
        String name  = swordName.get(uuid);
        String color = swordColor.getOrDefault(uuid, "§f");
        if (name == null || name.isBlank()) return null;
        return color + name;
    }

    public String getRawSwordName(UUID uuid)  { return swordName.getOrDefault(uuid, ""); }
    public String getSwordColor(UUID uuid)     { return swordColor.getOrDefault(uuid, "§f"); }

    /** Store sword name + color. Clamps name to 20 characters. */
    public void setSwordName(UUID uuid, String name, String colorCode) {
        if (name == null || name.isBlank()) {
            swordName.remove(uuid);
            swordColor.remove(uuid);
        } else {
            swordName.put(uuid, name.length() > 20 ? name.substring(0, 20) : name);
            swordColor.put(uuid, colorCode);
        }
    }

    // ── Expiry checking ─────────────────────────────────────────────

    private void checkExpiry() {
        long now = System.currentTimeMillis();
        List<UUID> toRevoke = new ArrayList<>();
        for (Map.Entry<UUID, Long> e : expiry.entrySet()) {
            if (e.getValue() > 0 && now >= e.getValue()) toRevoke.add(e.getKey());
        }
        toRevoke.forEach(this::revokeRank);
    }

    // ── Persistence ─────────────────────────────────────────────────

    public void save() {
        dataConfig.set("ranks", null);
        for (UUID uuid : expiry.keySet()) {
            String k = "ranks." + uuid;
            dataConfig.set(k + ".expiry",     expiry.getOrDefault(uuid, 0L));
            dataConfig.set(k + ".saves",      savesLeft.getOrDefault(uuid, 0));
            dataConfig.set(k + ".chatColor",  chatColor.get(uuid));
            dataConfig.set(k + ".swordName",  swordName.get(uuid));
            dataConfig.set(k + ".swordColor", swordColor.get(uuid));
        }
        try { dataConfig.save(dataFile); } catch (IOException e) { e.printStackTrace(); }
    }

    private void load() {
        dataFile = new File(plugin.getDataFolder(), "rankdata.yml");
        if (!dataFile.exists()) {
            plugin.getDataFolder().mkdirs();
            try { dataFile.createNewFile(); } catch (IOException e) { e.printStackTrace(); }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        if (!dataConfig.isConfigurationSection("ranks")) return;
        for (String key : dataConfig.getConfigurationSection("ranks").getKeys(false)) {
            UUID uuid = UUID.fromString(key);
            String p  = "ranks." + key;
            expiry.put(uuid,    dataConfig.getLong(p + ".expiry", 0L));
            savesLeft.put(uuid, dataConfig.getInt(p + ".saves", 0));
            String cc = dataConfig.getString(p + ".chatColor");  if (cc != null) chatColor.put(uuid, cc);
            String sn = dataConfig.getString(p + ".swordName");  if (sn != null) swordName.put(uuid, sn);
            String sc = dataConfig.getString(p + ".swordColor"); if (sc != null) swordColor.put(uuid, sc);
        }
    }
}
