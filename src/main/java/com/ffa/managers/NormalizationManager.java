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
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;
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
        // Periodic check every N ticks
        int interval = plugin.getConfig().getInt("normalization-check-interval", 40);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) normalizePlayer(p, false);
        }, interval, interval);
    }

    // Triggered on item pickup
    @EventHandler
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Bukkit.getScheduler().runTaskLater(plugin, () -> normalizePlayer(player, true), 1L);
    }

    public void normalizePlayer(Player player, boolean notify) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        boolean changed = false;

        // Check armor slots
        ItemStack[] armor = player.getInventory().getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            if (armor[i] == null) continue;
            if (isKoalaArmor(armor[i])) {
                ItemStack normalized = normalizeArmor(armor[i], tier, i);
                if (normalized != null) { armor[i] = normalized; changed = true; }
            }
        }
        if (changed) player.getInventory().setArmorContents(armor);

        // Check main inventory for armor and swords
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item == null) continue;
            if (isKoalaArmor(item)) {
                ItemStack normalized = normalizeArmorByMaterial(item, tier);
                if (normalized != null) { player.getInventory().setItem(i, normalized); changed = true; }
            } else if (isKoalaSword(item)) {
                ItemStack normalized = normalizeSword(item, tier);
                if (normalized != null) { player.getInventory().setItem(i, normalized); changed = true; }
            }
        }

        // Check sword in hand
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (isKoalaSword(hand)) {
            ItemStack normalized = normalizeSword(hand, tier);
            if (normalized != null) { player.getInventory().setItemInMainHand(normalized); changed = true; }
        }

        if (changed && notify) {
            String msg = plugin.getConfig().getString("messages.item-normalized", "§7Your gear was adjusted to your tier.");
            player.sendMessage(msg);
        }
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
        // slot: 0=boots, 1=leggings, 2=chestplate, 3=helmet
        String slotName = switch (slot) {
            case 0 -> "boots";
            case 1 -> "leggings";
            case 2 -> "chestplate";
            case 3 -> "helmet";
            default -> null;
        };
        if (slotName == null) return null;
        return buildNormalizedArmor(slotName, tier);
    }

    private ItemStack normalizeArmorByMaterial(ItemStack item, int tier) {
        String slotName = null;
        String matName = item.getType().name().toLowerCase();
        if (matName.contains("helmet")) slotName = "helmet";
        else if (matName.contains("chestplate")) slotName = "chestplate";
        else if (matName.contains("leggings")) slotName = "leggings";
        else if (matName.contains("boots")) slotName = "boots";
        if (slotName == null) return null;

        // Check if already correct tier
        String expectedMat = plugin.getConfig().getString("tiers." + tier + ".kit." + slotName + ".material", "DIAMOND_HELMET");
        if (item.getType() == Material.valueOf(expectedMat) && hasCorrectEnchants(item, tier, slotName)) return null;

        return buildNormalizedArmor(slotName, tier);
    }

    private ItemStack normalizeSword(ItemStack item, int tier) {
        String expectedMat = plugin.getConfig().getString("tiers." + tier + ".kit.sword.material", "DIAMOND_SWORD");
        if (item.getType() == Material.valueOf(expectedMat) && hasCorrectEnchants(item, tier, "sword")) return null;
        return buildNormalizedSword(tier);
    }

    private ItemStack buildNormalizedArmor(String slotName, int tier) {
        String path = "tiers." + tier + ".kit." + slotName;
        String matName = plugin.getConfig().getString(path + ".material", "DIAMOND_HELMET");
        Material mat;
        try { mat = Material.valueOf(matName); } catch (Exception e) { return null; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
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
        ItemMeta meta = item.getItemMeta();
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

    private boolean hasCorrectEnchants(ItemStack item, int tier, String slotName) {
        var enchSection = plugin.getConfig().getConfigurationSection("tiers." + tier + ".kit." + slotName + ".enchants");
        if (enchSection == null) return true;
        for (String enchName : enchSection.getKeys(false)) {
            Enchantment ench = Enchantment.getByName(enchName);
            if (ench == null) continue;
            int expected = enchSection.getInt(enchName);
            int actual = item.getEnchantmentLevel(ench);
            if (actual != expected) return false;
        }
        return true;
    }
}
