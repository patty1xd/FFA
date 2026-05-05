package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
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

public class TrimManager {

    private final FFAPlugin plugin;

    private final Map<UUID, Map<String, TrimChoice>> trimChoices  = new HashMap<>();
    private final Map<UUID, ShieldDesign>            shieldDesigns = new HashMap<>();

    public TrimManager(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    public record TrimChoice(TrimPattern pattern, TrimMaterial material) {}
    public record ShieldDesign(PatternType patternType, DyeColor patternColor, DyeColor baseColor) {}

    // ── Apply trims ──────────────────────────────────────────────────

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

    // ── Strip trims ──────────────────────────────────────────────────

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

    public ItemStack stripShieldDesign(ItemStack item) {
        if (item == null || item.getType() != Material.SHIELD) return null;
        if (!(item.getItemMeta() instanceof BlockStateMeta meta)) return null;
        if (!(meta.getBlockState() instanceof Banner banner)) return null;
        if (banner.getPatterns().isEmpty() && banner.getBaseColor() == DyeColor.WHITE) return null;
        return new ItemStack(Material.SHIELD);
    }

    // ── Storage ──────────────────────────────────────────────────────

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

    public void setShieldDesign(UUID uuid, PatternType patternType, DyeColor patternColor, DyeColor baseColor) {
        shieldDesigns.put(uuid, new ShieldDesign(patternType, patternColor, baseColor));
    }

    public ShieldDesign getShieldDesign(UUID uuid) {
        return shieldDesigns.get(uuid);
    }

    // ── Persistence ──────────────────────────────────────────────────

    public Map<String, String> serializeTrimChoices(UUID uuid) {
        Map<String, String> result = new HashMap<>();
        Map<String, TrimChoice> choices = trimChoices.get(uuid);
        if (choices == null) return result;
        for (Map.Entry<String, TrimChoice> e : choices.entrySet()) {
            result.put(e.getKey(),
                e.getValue().pattern().getKey().getKey()
                + ":" + e.getValue().material().getKey().getKey());
        }
        return result;
    }

    public void deserializeTrimChoices(UUID uuid, Map<String, String> data) {
        if (data == null || data.isEmpty()) return;
        for (Map.Entry<String, String> e : data.entrySet()) {
            String[] parts = e.getValue().split(":");
            if (parts.length != 2) continue;
            TrimPattern  pattern  = findPattern(parts[0]);
            TrimMaterial material = findMaterial(parts[1]);
            if (pattern != null && material != null)
                setTrimChoice(uuid, e.getKey(), pattern, material);
        }
    }

    public String serializeShield(UUID uuid) {
        ShieldDesign d = shieldDesigns.get(uuid);
        if (d == null) return null;
        return d.patternType().getKey().getKey()
            + ":" + d.patternColor().name()
            + ":" + d.baseColor().name();
    }

    public void deserializeShield(UUID uuid, String data) {
        if (data == null || data.isBlank()) return;
        String[] parts = data.split(":");
        if (parts.length != 3) return;
        try {
            PatternType type = Registry.BANNER_PATTERN.get(NamespacedKey.minecraft(parts[0]));
            DyeColor    pc   = DyeColor.valueOf(parts[1]);
            DyeColor    bc   = DyeColor.valueOf(parts[2]);
            if (type != null) setShieldDesign(uuid, type, pc, bc);
        } catch (Exception ignored) {}
    }

    // ── Registry lookups ─────────────────────────────────────────────

    public static TrimPattern findPattern(String key) {
        return Registry.TRIM_PATTERN.get(NamespacedKey.minecraft(key.toLowerCase()));
    }

    public static TrimMaterial findMaterial(String key) {
        return Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft(key.toLowerCase()));
    }

    public static List<TrimPattern> allPatterns() {
        List<TrimPattern> list = new ArrayList<>();
        Registry.TRIM_PATTERN.forEach(list::add);
        return list;
    }

    public static List<TrimMaterial> allMaterials() {
        List<TrimMaterial> list = new ArrayList<>();
        Registry.TRIM_MATERIAL.forEach(list::add);
        return list;
    }

    public static List<PatternType> allBannerPatterns() {
        List<PatternType> list = new ArrayList<>();
        Registry.BANNER_PATTERN.forEach(list::add);
        return list;
    }
}
