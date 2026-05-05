package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.DyeColor;

import java.util.*;

/**
 * TrimManager
 * Handles applying and stripping armor trims and shield designs.
 *
 * Each player can have independent trim choices per armor slot
 * and a separate shield banner pattern + color.
 *
 * Storage is kept in memory and persisted via RankManager's save.
 * The RankManager stores the raw string keys; this class converts them.
 */
public class TrimManager {

    private final FFAPlugin plugin;

    // UUID -> per-slot trim choices  (slot: "helmet","chestplate","leggings","boots")
    private final Map<UUID, Map<String, TrimChoice>> trimChoices = new HashMap<>();

    // UUID -> shield design
    private final Map<UUID, ShieldDesign> shieldDesigns = new HashMap<>();

    public TrimManager(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Data classes ────────────────────────────────────────────────

    public record TrimChoice(TrimPattern pattern, TrimMaterial material) {}

    public record ShieldDesign(PatternType patternType, DyeColor patternColor, DyeColor baseColor) {}

    // ── Apply trims to a living player's current armor ──────────────

    /**
     * Applies the stored trims onto the player's currently equipped armor.
     * Called after giveKit() so the base item already exists.
     * No-op if the player has no rank.
     */
    public void applyTrims(Player player) {
        if (!plugin.getRankManager().hasRank(player.getUniqueId())) return;
        UUID uuid = player.getUniqueId();

        applyTrimToSlot(player, uuid, "helmet",     player.getInventory().getHelmet());
        applyTrimToSlot(player, uuid, "chestplate", player.getInventory().getChestplate());
        applyTrimToSlot(player, uuid, "leggings",   player.getInventory().getLeggings());
        applyTrimToSlot(player, uuid, "boots",      player.getInventory().getBoots());

        applyShieldDesign(player, uuid);
    }

    private void applyTrimToSlot(Player player, UUID uuid, String slot, ItemStack item) {
        if (item == null) return;
        TrimChoice choice = getTrimChoice(uuid, slot);
        if (choice == null) return;
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return;
        meta.setTrim(new ArmorTrim(choice.material(), choice.pattern()));
        item.setItemMeta(meta);
        switch (slot) {
            case "helmet"     -> player.getInventory().setHelmet(item);
            case "chestplate" -> player.getInventory().setChestplate(item);
            case "leggings"   -> player.getInventory().setLeggings(item);
            case "boots"      -> player.getInventory().setBoots(item);
        }
    }

    private void applyShieldDesign(Player player, UUID uuid) {
        ShieldDesign design = shieldDesigns.get(uuid);
        if (design == null) return;
        ItemStack shield = player.getInventory().getItemInOffHand();
        if (shield == null || shield.getType() != Material.SHIELD) return;
        if (!(shield.getItemMeta() instanceof BlockStateMeta meta)) return;
        if (!(meta.getBlockState() instanceof Banner banner)) return;
        banner.setBaseColor(design.baseColor());
        banner.getPatterns().clear();
        banner.addPattern(new Pattern(design.patternColor(), design.patternType()));
        meta.setBlockState(banner);
        shield.setItemMeta(meta);
        player.getInventory().setItemInOffHand(shield);
    }

    // ── Strip trims from an ItemStack (for normalization) ───────────

    /**
     * Returns a copy of the item with all trims removed, or null if no trim was present.
     * Used by NormalizationManager when a non-rank-holder picks up trimmed armor.
     */
    public ItemStack stripTrims(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof ArmorMeta armorMeta)) return null;
        if (armorMeta.getTrim() == null) return null;
        armorMeta.setTrim(null);
        ItemStack stripped = item.clone();
        stripped.setItemMeta(armorMeta);
        return stripped;
    }

    /**
     * Returns a plain shield (no banner) if the given item is a shield with banner data.
     */
    public ItemStack stripShieldDesign(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return null;
        if (!(meta.getBlockState() instanceof Banner banner)) return null;
        if (banner.getPatterns().isEmpty() && banner.getBaseColor() == DyeColor.WHITE) return null;
        // Return plain shield
        return new ItemStack(Material.SHIELD);
    }

    // ── Trim choice storage ─────────────────────────────────────────

    public void setTrimChoice(UUID uuid, String slot, TrimPattern pattern, TrimMaterial material) {
        trimChoices.computeIfAbsent(uuid, k -> new HashMap<>())
                   .put(slot, new TrimChoice(pattern, material));
    }

    public TrimChoice getTrimChoice(UUID uuid, String slot) {
        Map<String, TrimChoice> map = trimChoices.get(uuid);
        return map == null ? null : map.get(slot);
    }

    public Map<String, TrimChoice> getAllTrimChoices(UUID uuid) {
        return trimChoices.getOrDefault(uuid, new HashMap<>());
    }

    // ── Shield design storage ───────────────────────────────────────

    public void setShieldDesign(UUID uuid, PatternType patternType, DyeColor patternColor, DyeColor baseColor) {
        shieldDesigns.put(uuid, new ShieldDesign(patternType, patternColor, baseColor));
    }

    public ShieldDesign getShieldDesign(UUID uuid) {
        return shieldDesigns.get(uuid);
    }

    // ── Persistence helpers (called by RankManager) ─────────────────

    /** Serialize trim choices to a string map for YAML storage. */
    public Map<String, String> serializeTrimChoices(UUID uuid) {
        Map<String, String> result = new HashMap<>();
        Map<String, TrimChoice> choices = trimChoices.get(uuid);
        if (choices == null) return result;
        for (Map.Entry<String, TrimChoice> e : choices.entrySet()) {
            result.put(e.getKey(), e.getValue().pattern().getKey().getKey()
                    + ":" + e.getValue().material().getKey().getKey());
        }
        return result;
    }

    /** Deserialize trim choices from YAML string map. */
    public void deserializeTrimChoices(UUID uuid, Map<String, String> data) {
        if (data == null || data.isEmpty()) return;
        for (Map.Entry<String, String> e : data.entrySet()) {
            String[] parts = e.getValue().split(":");
            if (parts.length != 2) continue;
            TrimPattern  pattern  = findPattern(parts[0]);
            TrimMaterial material = findMaterial(parts[1]);
            if (pattern != null && material != null) {
                setTrimChoice(uuid, e.getKey(), pattern, material);
            }
        }
    }

    /** Serialize shield design to a single string "patternType:patternColor:baseColor". */
    public String serializeShield(UUID uuid) {
        ShieldDesign d = shieldDesigns.get(uuid);
        if (d == null) return null;
        return d.patternType().getIdentifier() + ":" + d.patternColor().name() + ":" + d.baseColor().name();
    }

    /** Deserialize shield design from string. */
    public void deserializeShield(UUID uuid, String data) {
        if (data == null || data.isBlank()) return;
        String[] parts = data.split(":");
        if (parts.length != 3) return;
        try {
            PatternType type  = PatternType.getByIdentifier(parts[0]);
            DyeColor    pc    = DyeColor.valueOf(parts[1]);
            DyeColor    bc    = DyeColor.valueOf(parts[2]);
            if (type != null) setShieldDesign(uuid, type, pc, bc);
        } catch (Exception ignored) {}
    }

    // ── Registry lookup helpers ─────────────────────────────────────

    public static TrimPattern findPattern(String key) {
        for (TrimPattern p : TrimPattern.values()) {
            if (p.getKey().getKey().equalsIgnoreCase(key)) return p;
        }
        return null;
    }

    public static TrimMaterial findMaterial(String key) {
        for (TrimMaterial m : TrimMaterial.values()) {
            if (m.getKey().getKey().equalsIgnoreCase(key)) return m;
        }
        return null;
    }

    /** All available trim patterns for display in the GUI. */
    public static List<TrimPattern> allPatterns() {
        return List.copyOf(TrimPattern.values());
    }

    /** All available trim materials for display in the GUI. */
    public static List<TrimMaterial> allMaterials() {
        return List.copyOf(TrimMaterial.values());
    }
}
