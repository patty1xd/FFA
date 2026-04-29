package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;

public class KitManager {

    private final FFAPlugin plugin;

    public KitManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void giveKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String path = "tiers." + tier + ".kit.";

        // Armor
        player.getInventory().setHelmet(buildItem(path + "helmet"));
        player.getInventory().setChestplate(buildItem(path + "chestplate"));
        player.getInventory().setLeggings(buildItem(path + "leggings"));
        player.getInventory().setBoots(buildItem(path + "boots"));


        // Sword — slot 0
        player.getInventory().setItem(0, buildItem(path + "sword"));

        // Shield in offhand
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta shieldMeta = shield.getItemMeta();
        shieldMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
        shieldMeta.addEnchant(Enchantment.MENDING, 1, true);
        shield.setItemMeta(shieldMeta);
        player.getInventory().setItemInOffHand(shield);

        // Bow — slot 1
ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta bowMeta = bow.getItemMeta();
        bowMeta.addEnchant(Enchantment.POWER, tier, true);
        bowMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
        bowMeta.addEnchant(Enchantment.INFINITY, 1, true);
        bowMeta.addEnchant(Enchantment.FLAME, 1, true);
        bow.setItemMeta(bowMeta);
        player.getInventory().setItem(1, bow);

        // Pickaxe — slot 2
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta pickMeta = pick.getItemMeta();
        pickMeta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        pickMeta.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        pickMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
        pickMeta.addEnchant(Enchantment.MENDING, 1, true);
        pick.setItemMeta(pickMeta);
        player.getInventory().setItem(2, pick);

ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta axeMeta = axe.getItemMeta();
        axeMeta.addEnchant(Enchantment.SHARPNESS, tier, true);
        axeMeta.addEnchant(Enchantment.EFFICIENCY, 5, true);
        axeMeta.addEnchant(Enchantment.UNBREAKING, 3, true);
        axeMeta.addEnchant(Enchantment.MENDING, 1, true);
        axe.setItemMeta(axeMeta);
        player.getInventory().setItem(3, axe);


        // 128 golden apples (2 stacks) — slots 3-4
        player.getInventory().setItem(4, new ItemStack(Material.GOLDEN_APPLE, 64));
        player.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 64));

        // 64 cobwebs — slot 5
        player.getInventory().setItem(6, new ItemStack(Material.COBWEB, 64));

        // 3 fire resistance potions (8 min) — slot 6
        player.getInventory().setItem(7, buildPotion(Material.SPLASH_POTION, PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));
        player.getInventory().setItem(8, buildPotion(Material.SPLASH_POTION, PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));
        player.getInventory().setItem(9, buildPotion(Material.SPLASH_POTION, PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));

        // 3 speed potions (8 min) — slot 7
        player.getInventory().setItem(10, buildPotion(Material.SPLASH_POTION, PotionEffectType.SPEED, 1, 9600, 1));
        player.getInventory().setItem(11, buildPotion(Material.SPLASH_POTION, PotionEffectType.SPEED, 1, 9600, 1));
        player.getInventory().setItem(12, buildPotion(Material.SPLASH_POTION, PotionEffectType.SPEED, 1, 9600, 1));

        // 3 stacks of exp bottles — slots 8-10
        player.getInventory().setItem(13, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        player.getInventory().setItem(14, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        player.getInventory().setItem(15, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));

        // 3 water buckets — slots 11-13
        player.getInventory().setItem(16, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(17, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(18, new ItemStack(Material.WATER_BUCKET));

        // 64 warped logs — slot 14
        player.getInventory().setItem(19, new ItemStack(Material.WARPED_STEM, 64));

        // 64 arrows — slot 15
        player.getInventory().setItem(20, new ItemStack(Material.ARROW, 64));

        // 32 chorus fruit — slot 16
        player.getInventory().setItem(21, new ItemStack(Material.CHORUS_FRUIT, 32));

        // 19 strength II potions (1m30s = 1800 ticks) — slots 17-35
        for (int i = 22; i <= 35; i++) {
            player.getInventory().setItem(i, buildPotion(Material.SPLASH_POTION, PotionEffectType.STRENGTH, 2, 1800, 1));
        }

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §aKit updated! " + tierDisplay);
    }

    private ItemStack buildPotion(Material mat, PotionEffectType type, int amplifier, int duration, int amount) {
        ItemStack potion = new ItemStack(mat, amount);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier - 1), true);
        meta.setColor(type.getColor());
        potion.setItemMeta(meta);
        return potion;
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
                Enchantment ench = Enchantment.getByName(enchName);
                if (ench != null) meta.addEnchant(ench, enchantSection.getInt(enchName), true);
            }
        }
        item.setItemMeta(meta);
        return item;
    }
}
