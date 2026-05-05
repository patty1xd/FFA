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

import java.util.List;

public class KitManager {

    private final FFAPlugin plugin;

    public KitManager(FFAPlugin plugin) { this.plugin = plugin; }

    /**
     * Full kit — given on first join, death, or NPC right-click.
     * Clears inventory and gives everything.
     * After building armor, applies donor trims if the player has an active rank.
     */
    public void giveKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String path = "tiers." + tier + ".kit.";

        player.getInventory().clear();

        // Armor
        player.getInventory().setHelmet(buildItem(path + "helmet"));
        player.getInventory().setChestplate(buildItem(path + "chestplate"));
        player.getInventory().setLeggings(buildItem(path + "leggings"));
        player.getInventory().setBoots(buildItem(path + "boots"));

        // Sword slot 0
        ItemStack sword = buildItem(path + "sword");
        player.getInventory().setItem(0, sword);

        // Shield offhand
        player.getInventory().setItemInOffHand(buildShield());

        // Bow
        player.getInventory().setItem(1, buildBow(tier));

        // Axe
        player.getInventory().setItem(2, buildAxe(tier));

        // Pickaxe
        player.getInventory().setItem(3, buildPickaxe());

        // 128 golden apples
        player.getInventory().setItem(4, new ItemStack(Material.GOLDEN_APPLE, 64));
        player.getInventory().setItem(5, new ItemStack(Material.GOLDEN_APPLE, 64));

        // 64 cobwebs
        player.getInventory().setItem(6, new ItemStack(Material.COBWEB, 64));

        // 3 fire resistance (8 min)
        player.getInventory().setItem(7,  buildPotion(PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));
        player.getInventory().setItem(8,  buildPotion(PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));
        player.getInventory().setItem(9,  buildPotion(PotionEffectType.FIRE_RESISTANCE, 1, 9600, 1));

        // 3 speed (8 min)
        player.getInventory().setItem(10, buildPotion(PotionEffectType.SPEED, 1, 9600, 1));
        player.getInventory().setItem(11, buildPotion(PotionEffectType.SPEED, 1, 9600, 1));
        player.getInventory().setItem(12, buildPotion(PotionEffectType.SPEED, 1, 9600, 1));

        // 3 stacks exp bottles
        player.getInventory().setItem(13, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        player.getInventory().setItem(14, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));
        player.getInventory().setItem(15, new ItemStack(Material.EXPERIENCE_BOTTLE, 64));

        // 3 water buckets
        player.getInventory().setItem(16, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(17, new ItemStack(Material.WATER_BUCKET));
        player.getInventory().setItem(18, new ItemStack(Material.WATER_BUCKET));

        // 64 warped logs
        player.getInventory().setItem(19, new ItemStack(Material.WARPED_STEM, 64));

        // 64 arrows
        player.getInventory().setItem(20, new ItemStack(Material.ARROW, 64));

        // 32 chorus fruit
        player.getInventory().setItem(21, new ItemStack(Material.CHORUS_FRUIT, 32));

        // 3 strength potions
        for (int i = 22; i <= 24; i++) {
            player.getInventory().setItem(i, buildPotion(PotionEffectType.STRENGTH, 1, 9600, 1));
        }

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §aKit given! " + tierDisplay);

        // Apply donor trims if rank active (runs after inventory is fully set)
        if (plugin.getRankManager().hasRank(player.getUniqueId())) {
            plugin.getTrimManager().applyTrims(player);
            applyCustomSwordName(player);
        }
    }

    /**
     * Upgrade kit — called on tier up.
     * Only updates armor, sword, bow, axe. Preserves other inventory items.
     * Re-applies donor trims after updating armor.
     */
    public void upgradeKit(Player player) {
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String path = "tiers." + tier + ".kit.";

        // Update armor slots
        player.getInventory().setHelmet(buildItem(path + "helmet"));
        player.getInventory().setChestplate(buildItem(path + "chestplate"));
        player.getInventory().setLeggings(buildItem(path + "leggings"));
        player.getInventory().setBoots(buildItem(path + "boots"));

        // Update sword
        ItemStack newSword = buildItem(path + "sword");
        boolean swordReplaced = false;
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && (item.getType() == Material.DIAMOND_SWORD
                    || item.getType() == Material.NETHERITE_SWORD)) {
                player.getInventory().setItem(i, newSword);
                swordReplaced = true;
                break;
            }
        }
        if (!swordReplaced) player.getInventory().setItem(0, newSword);

        // Update bow
        ItemStack newBow = buildBow(tier);
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.BOW) {
                player.getInventory().setItem(i, newBow);
                break;
            }
        }

        // Update axe
        ItemStack newAxe = buildAxe(tier);
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.getType() == Material.DIAMOND_AXE) {
                player.getInventory().setItem(i, newAxe);
                break;
            }
        }

        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        player.sendMessage("§8[§6FFA§8] §a§lTIER UP! §7Your gear has been upgraded to " + tierDisplay + "§7!");

        // Re-apply donor trims after armor upgrade
        if (plugin.getRankManager().hasRank(player.getUniqueId())) {
            plugin.getTrimManager().applyTrims(player);
            applyCustomSwordName(player);
        }
    }

    /**
     * Applies the player's custom sword name + color to whichever sword
     * is currently in their inventory (prefers slot 0, then searches).
     * Called after giveKit/upgradeKit for rank holders.
     */
    public void applyCustomSwordName(Player player) {
        String coloredName = plugin.getRankManager().getColoredSwordName(player.getUniqueId());
        if (coloredName == null || coloredName.isBlank()) return;

        // Try slot 0 first, then search whole inventory
        ItemStack sword = player.getInventory().getItem(0);
        int swordSlot   = 0;
        if (sword == null || (sword.getType() != Material.DIAMOND_SWORD
                && sword.getType() != Material.NETHERITE_SWORD)) {
            sword = null;
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && (item.getType() == Material.DIAMOND_SWORD
                        || item.getType() == Material.NETHERITE_SWORD)) {
                    sword = item;
                    swordSlot = i;
                    break;
                }
            }
        }
        if (sword == null) return;

        ItemMeta meta = sword.getItemMeta();
        meta.setDisplayName(coloredName);
        // Store a marker so normalization can detect named swords
        meta.setLore(List.of("§8[Donor Sword]"));
        sword.setItemMeta(meta);
        player.getInventory().setItem(swordSlot, sword);
    }

    /**
     * Strips donor name/lore from a sword ItemStack.
     * Returns the stripped copy, or null if it wasn't a donor-named sword.
     */
    public ItemStack stripSwordName(ItemStack item) {
        if (item == null) return null;
        if (item.getType() != Material.DIAMOND_SWORD && item.getType() != Material.NETHERITE_SWORD)
            return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return null;
        if (meta.getLore() == null || !meta.getLore().contains("§8[Donor Sword]")) return null;
        // It's a donor-named sword — return clean copy
        ItemStack clean = item.clone();
        ItemMeta cleanMeta = clean.getItemMeta();
        cleanMeta.setDisplayName(null);
        cleanMeta.setLore(null);
        clean.setItemMeta(cleanMeta);
        return clean;
    }

    // ── Builders ──────────────────────────────────────────────────

    private ItemStack buildShield() {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta m = shield.getItemMeta();
        m.addEnchant(Enchantment.UNBREAKING, 3, true);
        m.addEnchant(Enchantment.MENDING, 1, true);
        shield.setItemMeta(m);
        return shield;
    }

    private ItemStack buildBow(int tier) {
        ItemStack bow = new ItemStack(Material.BOW);
        ItemMeta m = bow.getItemMeta();
        m.addEnchant(Enchantment.POWER, tier, true);
        m.addEnchant(Enchantment.UNBREAKING, 3, true);
        m.addEnchant(Enchantment.INFINITY, 1, true);
        m.addEnchant(Enchantment.FLAME, 1, true);
        bow.setItemMeta(m);
        return bow;
    }

    private ItemStack buildAxe(int tier) {
        ItemStack axe = new ItemStack(Material.DIAMOND_AXE);
        ItemMeta m = axe.getItemMeta();
        m.addEnchant(Enchantment.SHARPNESS, tier + 2, true);
        m.addEnchant(Enchantment.EFFICIENCY, 5, true);
        m.addEnchant(Enchantment.UNBREAKING, 3, true);
        m.addEnchant(Enchantment.MENDING, 1, true);
        axe.setItemMeta(m);
        return axe;
    }

    private ItemStack buildPickaxe() {
        ItemStack pick = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta m = pick.getItemMeta();
        m.addEnchant(Enchantment.EFFICIENCY, 5, true);
        m.addEnchant(Enchantment.SILK_TOUCH, 1, true);
        m.addEnchant(Enchantment.UNBREAKING, 3, true);
        m.addEnchant(Enchantment.MENDING, 1, true);
        pick.setItemMeta(m);
        return pick;
    }

    private ItemStack buildPotion(PotionEffectType type, int amplifier, int duration, int amount) {
        ItemStack potion = new ItemStack(Material.SPLASH_POTION, amount);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        meta.addCustomEffect(new PotionEffect(type, duration, amplifier - 1), true);
        if (type.getColor() != null) meta.setColor(type.getColor());
        potion.setItemMeta(meta);
        return potion;
    }

    public ItemStack buildItem(String path) {
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
