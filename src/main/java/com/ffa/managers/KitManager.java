package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KitManager {

    private final FFAPlugin plugin;

    public KitManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void giveKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());

        // Replace armor
        player.getInventory().setHelmet(buildArmor(Material.DIAMOND_HELMET, tier));
        player.getInventory().setChestplate(buildArmor(Material.DIAMOND_CHESTPLATE, tier));
        player.getInventory().setLeggings(buildArmor(Material.DIAMOND_LEGGINGS, tier));
        player.getInventory().setBoots(buildArmor(Material.DIAMOND_BOOTS, tier));

        // Replace sword — find existing diamond sword and replace it, or add new
        ItemStack sword = buildSword(tier);
        boolean replaced = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.DIAMOND_SWORD) {
                player.getInventory().setItem(i, sword);
                replaced = true;
                break;
            }
        }
        if (!replaced) player.getInventory().addItem(sword);

        // Give golden apples only on first kit (tier 1)
        if (tier == 1) player.getInventory().addItem(new ItemStack(Material.GOLDEN_APPLE, 3));

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §aKit updated! " + tierDisplay);
    }

    private ItemStack buildArmor(Material mat, int tier) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        int prot = tier - 1; // tier 1 = 0, tier 2 = 1, etc.
        if (prot > 0) meta.addEnchant(Enchantment.PROTECTION, prot, true);
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack buildSword(int tier) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        meta.addEnchant(Enchantment.UNBREAKING, 3, true);
        meta.addEnchant(Enchantment.MENDING, 1, true);
        switch (tier) {
            case 1 -> meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            case 2 -> {
                meta.addEnchant(Enchantment.SHARPNESS, 2, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 1, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 1, true);
            }
            case 3 -> {
                meta.addEnchant(Enchantment.SHARPNESS, 3, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 2, true);
            }
            case 4 -> {
                meta.addEnchant(Enchantment.SHARPNESS, 4, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            }
            case 5 -> {
                meta.addEnchant(Enchantment.SHARPNESS, 5, true);
                meta.addEnchant(Enchantment.FIRE_ASPECT, 2, true);
                meta.addEnchant(Enchantment.SWEEPING_EDGE, 3, true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }
}
