package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class NormalizationManager implements Listener {

    private final FFAPlugin plugin;

    private static final Set<Material> ARMOR_MATERIALS = Set.of(
        Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS,
        Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE, Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS,
        Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS,
        Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS,
        Material.GOLDEN_HELMET, Material.GOLDEN_CHESTPLATE, Material.GOLDEN_LEGGINGS, Material.GOLDEN_BOOTS,
        Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS
    );

    private static final Set<Material> SWORD_MATERIALS = Set.of(
        Material.DIAMOND_SWORD, Material.NETHERITE_SWORD, Material.IRON_SWORD,
        Material.STONE_SWORD, Material.GOLDEN_SWORD, Material.WOODEN_SWORD
    );

    public NormalizationManager(FFAPlugin plugin) {
        this.plugin = plugin;
        int interval = plugin.getConfig().getInt("normalization-check-interval", 40);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) normalizePlayer(p, false);
        }, interval, interval);
    }

    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> normalizePlayer(player, true), 1L);
    }

    public void normalizePlayer(Player player, boolean notify) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        boolean isRankHolder = plugin.getRankManager().hasRank(player.getUniqueId());
        boolean changed = false;

        // ── Armor slots ──────────────────────────────────────────────────────
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null) continue;

            // If this player is NOT a rank holder and the armor has a trim, strip it
            if (!isRankHolder && hasTrim(armor[i])) {
                ItemStack stripped = plugin.getTrimManager().stripTrims(armor[i]);
                if (stripped != null) { armor[i] = stripped; changed = true; }
            }

            // Material normalization (existing logic)
            if (isKoalaArmor(armor[i])) {
                ItemStack normalized = normalizeArmor(armor[i], tier, i);
                if (normalized != null) { armor[i] = normalized; changed = true; }
            }
        }
        if (changed) player.getInventory().setArmorContents(armor);

        // ── Main inventory ────────────────────────────────────────────────────
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;

            // Strip trims from armor in inventory if not rank holder
            if (!isRankHolder && ARMOR_MATERIALS.contains(item.getType()) && hasTrim(item)) {
                ItemStack stripped = plugin.getTrimManager().stripTrims(item);
                if (stripped != null) { player.getInventory().setItem(i, stripped); changed = true; continue; }
            }

            // Strip donor sword name from swords if not the owner
            if (SWORD_MATERIALS.contains(item.getType())) {
                ItemStack strippedSword = plugin.getKitManager().stripSwordName(item);
                if (strippedSword != null && !isRankHolder) {
                    // Replace with their own kit sword
                    ItemStack ownSword = plugin.getKitManager().buildItem("tiers." + tier + ".kit.sword");
                    player.getInventory().setItem(i, ownSword);
                    changed = true;
                    continue;
                }
            }

            // Material normalization
            if (isKoalaArmor(item)) {
                ItemStack normalized = normalizeArmorByMaterial(item, tier);
                if (normalized != null) { player.getInventory().setItem(i, normalized); changed = true; }
            } else if (isKoalaSword(item)) {
                ItemStack normalized = normalizeSword(item, tier);
                if (normalized != null) { player.getInventory().setItem(i, normalized); changed = true; }
            }
        }

        // ── Sword in hand ─────────────────────────────────────────────────────
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isKoalaSword(hand)) {
            // Strip donor name if not rank holder
            if (!isRankHolder) {
                ItemStack strippedSword = plugin.getKitManager().stripSwordName(hand);
                if (strippedSword != null) {
                    ItemStack ownSword = plugin.getKitManager().buildItem("tiers." + tier + ".kit.sword");
                    player.getInventory().setItemInMainHand(ownSword);
                    changed = true;
                }
            }
            ItemStack normalized = normalizeSword(hand, tier);
            if (normalized != null) { player.getInventory().setItemInMainHand(normalized); changed = true; }
        }

        // ── Offhand shield ────────────────────────────────────────────────────
        ItemStack offhand = player.getInventory().getItemInOffHand();
        if (!isRankHolder && offhand != null && offhand.getType() == Material.SHIELD) {
            ItemStack strippedShield = plugin.getTrimManager().stripShieldDesign(offhand);
            if (strippedShield != null) { player.getInventory().setItemInOffHand(strippedShield); changed = true; }
        }

        if (changed && notify) {
            String msg = plugin.getConfig().getString("messages.item-normalized",
                "§7Your gear was adjusted to your tier.");
            player.sendMessage(msg);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean hasTrim(ItemStack item) {
        if (item == null) return false;
        if (!(item.getItemMeta() instanceof ArmorMeta meta)) return false;
        return meta.getTrim() != null;
    }

    private boolean isKoalaArmor(ItemStack item) {
        if (item == null) return false;
        return ARMOR_MATERIALS.contains(item.getType());
    }

    private boolean isKoalaSword(ItemStack item) {
        if (item == null) return false;
        return SWORD_MATERIALS.contains(item.getType());
    }

    private ItemStack normalizeArmor(ItemStack item, int tier, int slot) {
        String slotName = switch (slot) {
            case 0 -> "boots";
            case 1 -> "leggings";
            case 2 -> "chestplate";
            case 3 -> "helmet";
            default -> null;
        };
        if (slotName == null) return null;
        String expectedMat = plugin.getConfig().getString(
            "tiers." + tier + ".kit." + slotName + ".material", "DIAMOND_HELMET");
        if (item.getType() == Material.valueOf(expectedMat)) return null;
        return buildNormalizedArmor(slotName, tier);
    }

    private ItemStack normalizeArmorByMaterial(ItemStack item, int tier) {
        String slotName = null;
        String matName = item.getType().name().toLowerCase();
        if (matName.contains("helmet"))      slotName = "helmet";
        else if (matName.contains("chestplate")) slotName = "chestplate";
        else if (matName.contains("leggings"))   slotName = "leggings";
        else if (matName.contains("boots"))      slotName = "boots";
        if (slotName == null) return null;
        String expectedMat = plugin.getConfig().getString(
            "tiers." + tier + ".kit." + slotName + ".material", "DIAMOND_HELMET");
        if (item.getType() == Material.valueOf(expectedMat)) return null;
        return buildNormalizedArmor(slotName, tier);
    }

    private ItemStack normalizeSword(ItemStack item, int tier) {
        String expectedMat = plugin.getConfig().getString(
            "tiers." + tier + ".kit.sword.material", "DIAMOND_SWORD");
        if (item.getType() == Material.valueOf(expectedMat)) return null;
        return buildNormalizedSword(tier);
    }

    private ItemStack buildNormalizedArmor(String slotName, int tier) {
        String path = "tiers." + tier + ".kit." + slotName;
        String matName = plugin.getConfig().getString(path + ".material", "DIAMOND_HELMET");
        Material mat;
        try { mat = Material.valueOf(matName); } catch (Exception e) { return null; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        var enchSection = plugin.getConfig().getConfigurationSection(path + ".enchants");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(enchName);
                if (ench != null) meta.addEnchant(ench, enchSection.getInt(enchName), true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildNormalizedSword(int tier) {
        String path = "tiers." + tier + ".kit.sword";
        String matName = plugin.getConfig().getString(path + ".material", "DIAMOND_SWORD");
        Material mat;
        try { mat = Material.valueOf(matName); } catch (Exception e) { return null; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        var enchSection = plugin.getConfig().getConfigurationSection(path + ".enchants");
        if (enchSection != null) {
            for (String enchName : enchSection.getKeys(false)) {
                Enchantment ench = Enchantment.getByName(enchName);
                if (ench != null) meta.addEnchant(ench, enchSection.getInt(enchName), true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }
}
