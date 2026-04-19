package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;

public class KitManager {

    private final FFAPlugin plugin;

    public KitManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void giveKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String path = "tiers." + tier + ".kit.";

        player.getInventory().setHelmet(buildItem(path + "helmet"));
        player.getInventory().setChestplate(buildItem(path + "chestplate"));
        player.getInventory().setLeggings(buildItem(path + "leggings"));
        player.getInventory().setBoots(buildItem(path + "boots"));

        ItemStack sword = buildItem(path + "sword");
        boolean replaced = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType().name().endsWith("_SWORD")) {
                player.getInventory().setItem(i, sword);
                replaced = true;
                break;
            }
        }
        if (!replaced) player.getInventory().addItem(sword);

        List<?> items = plugin.getConfig().getList(path + "items");
        if (items != null) {
            for (Object obj : items) {
                if (obj instanceof Map<?,?> map) {
                    String matName = (String) map.get("material");
                    int amount = map.containsKey("amount") ? (int) map.get("amount") : 1;
                    try {
                        Material mat = Material.valueOf(matName);
                        player.getInventory().remove(mat);
                        player.getInventory().addItem(new ItemStack(mat, amount));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §aKit updated! " + tierDisplay);
    }

    private ItemStack buildItem(String path) {
        String matName = plugin.getConfig().getString(path + ".material", "DIAMOND_SWORD");
        Material mat;
        try { mat = Material.valueOf(matName); } catch (IllegalArgumentException e) { mat = Material.DIAMOND_SWORD; }
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        var enchantSection = plugin.getConfig().getConfigurationSection(path + ".enchants");
        if (enchantSection != null) {
            for (String enchName : enchantSection.getKeys(false)) {
                int level = enchantSection.getInt(enchName);
                Enchantment ench = Enchantment.getByName(enchName);
                if (ench != null) meta.addEnchant(ench, level, true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }
}
