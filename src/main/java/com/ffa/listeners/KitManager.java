package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public class KitManager {

    private final FFAPlugin plugin;

    public KitManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void giveKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        player.getInventory().clear();

        // Protection level (tier 1 = 0, tier 2 = 1, etc.)
        int prot = tier - 1;
        // Sharpness level = tier
        int sharp = tier;

        // Helmet
        player.getInventory().setHelmet(buildArmor(Material.DIAMOND_HELMET, prot));
        // Chestplate
        player.getInventory().setChestplate(buildArmor(Material.DIAMOND_CHESTPLATE, prot));
        // Leggings
        player.getInventory().setLeggings(buildArmor(Material.DIAMOND_LEGGINGS, prot));
        // Boots
        player.getInventory().setBoots(buildArmor(Material.DIAMOND_BOOTS, prot));
        // Sword
        player.getInventory().addItem(buildSword(sharp));
        // Golden apples
        player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §aKit given! " + tierDisplay);
    }

    private ItemStack buildArmor(Material mat, int protLevel) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (protLevel > 0) meta.addEnchant(Enchantment.PROTECTION, protLevel, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSword(int sharpLevel) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.SHARPNESS, sharpLevel, true);
        meta.addEnchant(Enchantment.UNBREAKING, 10, true);
        meta.setUnbreakable(true);
        item.setItemMeta(meta);
        return item;
    }
}
