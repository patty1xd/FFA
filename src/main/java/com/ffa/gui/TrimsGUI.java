package com.ffa.gui;

import com.ffa.FFAPlugin;
import com.ffa.managers.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;

import java.util.*;

/**
 * TrimsGUI
 * --------
 * A multi-page chest inventory GUI with the following sections:
 *
 *  Page 0 — MAIN MENU
 *    Buttons: Helmet / Chestplate / Leggings / Boots / Shield / Chat Color / Sword Name / SAVE
 *
 *  Page 1-4 — ARMOR PIECE (helmet, chestplate, leggings, boots)
 *    Top row  : Trim Pattern picker  (paginated with prev/next)
 *    Mid row  : Trim Material picker
 *    Bottom   : Back button
 *
 *  Page 5 — SHIELD
 *    Pick base color, pick pattern type, pick pattern color
 *
 *  Page 6 — CHAT COLOR
 *    16 colored wool buttons for each ChatColor option
 *
 *  Page 7 — SWORD NAME
 *    Clicking opens a chat prompt for the player to type the name.
 *    A color row lets them pick the name color.
 *
 * The SAVE button on the main page consumes one charge and applies everything.
 */
public class TrimsGUI implements Listener {

    private final FFAPlugin plugin;

    // Tracks which page each open GUI is on
    private final Map<UUID, Integer>  pageMap      = new HashMap<>();
    // Tracks the pattern/material page offset for pagination
    private final Map<UUID, Integer>  patternPage  = new HashMap<>();
    private final Map<UUID, Integer>  materialPage = new HashMap<>();
    // Pending chat input for sword name
    private final Set<UUID>           awaitingName = new HashSet<>();
    // Pending chat input for sword name (armor slot context)
    private final Map<UUID, String>   editingSlot  = new HashMap<>();

    // Draft state (not committed until SAVE)
    private final Map<UUID, Map<String, TrimManager.TrimChoice>> draftTrims  = new HashMap<>();
    private final Map<UUID, TrimManager.ShieldDesign>            draftShield = new HashMap<>();
    private final Map<UUID, String>  draftChatColor  = new HashMap<>();
    private final Map<UUID, String>  draftSwordName  = new HashMap<>();
    private final Map<UUID, String>  draftSwordColor = new HashMap<>();

    // Page constants
    private static final int PAGE_MAIN        = 0;
    private static final int PAGE_HELMET      = 1;
    private static final int PAGE_CHESTPLATE  = 2;
    private static final int PAGE_LEGGINGS    = 3;
    private static final int PAGE_BOOTS       = 4;
    private static final int PAGE_SHIELD      = 5;
    private static final int PAGE_CHAT_COLOR  = 6;
    private static final int PAGE_SWORD_NAME  = 7;

    // All available trim patterns and materials
    private static final List<TrimPattern>  ALL_PATTERNS  = new ArrayList<>(TrimPattern.values());
    private static final List<TrimMaterial> ALL_MATERIALS = new ArrayList<>(TrimMaterial.values());

    // Chat colors available for selection
    private static final List<ColorOption> CHAT_COLORS = List.of(
        new ColorOption("§aGreen",        "§a", Material.LIME_WOOL),
        new ColorOption("§2Dark Green",   "§2", Material.GREEN_WOOL),
        new ColorOption("§bAqua",         "§b", Material.CYAN_WOOL),
        new ColorOption("§3Dark Aqua",    "§3", Material.LIGHT_BLUE_WOOL),
        new ColorOption("§9Blue",         "§9", Material.BLUE_WOOL),
        new ColorOption("§1Dark Blue",    "§1", Material.BLUE_WOOL),
        new ColorOption("§dLight Purple", "§d", Material.MAGENTA_WOOL),
        new ColorOption("§5Dark Purple",  "§5", Material.PURPLE_WOOL),
        new ColorOption("§6Gold",         "§6", Material.ORANGE_WOOL),
        new ColorOption("§eYellow",       "§e", Material.YELLOW_WOOL),
        new ColorOption("§cRed",          "§c", Material.RED_WOOL),
        new ColorOption("§4Dark Red",     "§4", Material.RED_WOOL),
        new ColorOption("§fWhite",        "§f", Material.WHITE_WOOL),
        new ColorOption("§7Gray",         "§7", Material.LIGHT_GRAY_WOOL),
        new ColorOption("§8Dark Gray",    "§8", Material.GRAY_WOOL),
        new ColorOption("§0Black",        "§0", Material.BLACK_WOOL)
    );

    private record ColorOption(String label, String code, Material icon) {}

    public TrimsGUI(FFAPlugin plugin) {
        this.plugin = plugin;
    }

    // ── Open entry point ────────────────────────────────────────────

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getRankManager().hasRank(uuid)) {
            player.sendMessage("§8[§6FFA§8] §cYou need the §dDonor §crank to use this!");
            return;
        }
        // Load current saved choices as draft starting point
        loadDraft(uuid);
        openPage(player, PAGE_MAIN);
    }

    private void loadDraft(UUID uuid) {
        // Copy saved trim choices into draft
        Map<String, TrimManager.TrimChoice> saved = plugin.getTrimManager().getAllTrimChoices(uuid);
        draftTrims.put(uuid, new HashMap<>(saved));

        TrimManager.ShieldDesign sd = plugin.getTrimManager().getShieldDesign(uuid);
        if (sd != null) draftShield.put(uuid, sd);

        String cc = plugin.getRankManager().getChatColor(uuid);
        if (cc != null && !cc.isBlank()) draftChatColor.put(uuid, cc);

        draftSwordName.put(uuid, plugin.getRankManager().getRawSwordName(uuid));
        draftSwordColor.put(uuid, plugin.getRankManager().getSwordColor(uuid));
    }

    private void openPage(Player player, int page) {
        UUID uuid = player.getUniqueId();
        pageMap.put(uuid, page);
        Inventory inv = buildPage(player, page);
        if (inv != null) player.openInventory(inv);
    }

    // ── Page builders ───────────────────────────────────────────────

    private Inventory buildPage(Player player, int page) {
        return switch (page) {
            case PAGE_MAIN       -> buildMainMenu(player);
            case PAGE_HELMET     -> buildArmorPage(player, "helmet",     Material.DIAMOND_HELMET);
            case PAGE_CHESTPLATE -> buildArmorPage(player, "chestplate", Material.DIAMOND_CHESTPLATE);
            case PAGE_LEGGINGS   -> buildArmorPage(player, "leggings",   Material.DIAMOND_LEGGINGS);
            case PAGE_BOOTS      -> buildArmorPage(player, "boots",      Material.DIAMOND_BOOTS);
            case PAGE_SHIELD     -> buildShieldPage(player);
            case PAGE_CHAT_COLOR -> buildChatColorPage(player);
            case PAGE_SWORD_NAME -> buildSwordNamePage(player);
            default              -> null;
        };
    }

    private Inventory buildMainMenu(Player player) {
        UUID uuid    = player.getUniqueId();
        int  saves   = plugin.getRankManager().getSavesLeft(uuid);
        Inventory inv = Bukkit.createInventory(null, 54,
            "§d§lDonor Customisation §8| §7" + saves + " save(s) left");

        // Glass pane border
        ItemStack pane = makeItem(Material.PURPLE_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        // Armor pieces
        inv.setItem(10, makeItem(Material.DIAMOND_HELMET,     "§bHelmet Trim",     "§7Click to customise"));
        inv.setItem(19, makeItem(Material.DIAMOND_CHESTPLATE, "§bChestplate Trim", "§7Click to customise"));
        inv.setItem(28, makeItem(Material.DIAMOND_LEGGINGS,   "§bLeggings Trim",   "§7Click to customise"));
        inv.setItem(37, makeItem(Material.DIAMOND_BOOTS,      "§bBoots Trim",      "§7Click to customise"));

        // Shield
        inv.setItem(13, makeItem(Material.SHIELD, "§dShield Design", "§7Click to customise"));

        // Chat color
        String currentChatColor = draftChatColor.getOrDefault(uuid, "§7None");
        inv.setItem(22, makeItem(Material.NAME_TAG, "§eChat Color",
            "§7Current: " + currentChatColor + "Sample§r", "§7Click to change"));

        // Sword name
        String swordN = draftSwordName.getOrDefault(uuid, "§7None");
        String swordC = draftSwordColor.getOrDefault(uuid, "§f");
        inv.setItem(31, makeItem(Material.DIAMOND_SWORD, "§6Sword Name",
            "§7Current: " + swordC + swordN, "§7Click to change"));

        // Save button
        if (saves > 0) {
            inv.setItem(49, makeItem(Material.EMERALD, "§a§lSAVE",
                "§7This will apply all changes.",
                "§7Saves remaining: §e" + saves,
                "§cThis uses 1 save charge!"));
        } else {
            inv.setItem(49, makeItem(Material.BARRIER, "§c§lNO SAVES LEFT",
                "§7You have no save charges remaining.",
                "§7Purchase the rank again to get more."));
        }

        return inv;
    }

    private Inventory buildArmorPage(Player player, String slot, Material icon) {
        UUID uuid = player.getUniqueId();
        int  pPage = patternPage.getOrDefault(uuid, 0);
        int  mPage = materialPage.getOrDefault(uuid, 0);

        Inventory inv = Bukkit.createInventory(null, 54,
            "§b" + capitalize(slot) + " Trim");

        ItemStack pane = makeItem(Material.CYAN_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        // Current selection display
        TrimManager.TrimChoice current = draftTrims
            .getOrDefault(uuid, new HashMap<>()).get(slot);
        String patName  = current != null ? current.pattern().getKey().getKey()  : "§7None";
        String matName  = current != null ? current.material().getKey().getKey() : "§7None";
        inv.setItem(4, makeItem(icon, "§b" + capitalize(slot),
            "§7Pattern:  §e" + patName,
            "§7Material: §e" + matName));

        // ── Pattern row (slots 9-17) ──
        int patStart = pPage * 7;
        for (int i = 0; i < 7 && (patStart + i) < ALL_PATTERNS.size(); i++) {
            TrimPattern pat = ALL_PATTERNS.get(patStart + i);
            ItemStack pi = makeTrimPatternItem(pat, icon, slot, uuid, current);
            inv.setItem(9 + i, pi);
        }

        // Pattern prev/next
        if (pPage > 0)
            inv.setItem(9 + 7,  makeItem(Material.ARROW, "§7← Previous Patterns"));
        if (patStart + 7 < ALL_PATTERNS.size())
            inv.setItem(9 + 8, makeItem(Material.ARROW, "§7Next Patterns →"));

        // ── Material row (slots 27-35) ──
        int matStart = mPage * 7;
        for (int i = 0; i < 7 && (matStart + i) < ALL_MATERIALS.size(); i++) {
            TrimMaterial mat = ALL_MATERIALS.get(matStart + i);
            ItemStack mi = makeTrimMaterialItem(mat, slot, uuid, current);
            inv.setItem(27 + i, mi);
        }

        if (mPage > 0)
            inv.setItem(27 + 7, makeItem(Material.ARROW, "§7← Previous Materials"));
        if (matStart + 7 < ALL_MATERIALS.size())
            inv.setItem(27 + 8, makeItem(Material.ARROW, "§7Next Materials →"));

        // Back
        inv.setItem(45, makeItem(Material.BARRIER, "§cBack"));

        return inv;
    }

    private ItemStack makeTrimPatternItem(TrimPattern pat, Material armorIcon,
                                          String slot, UUID uuid,
                                          TrimManager.TrimChoice current) {
        boolean selected = current != null && current.pattern().equals(pat);
        String name = (selected ? "§a✔ " : "§7") + capitalize(pat.getKey().getKey());
        ItemStack item = new ItemStack(armorIcon);
        // Apply trim with a default material for preview
        if (item.getItemMeta() instanceof ArmorMeta meta) {
            TrimMaterial previewMat = (current != null) ? current.material() : TrimMaterial.GOLD;
            meta.setTrim(new ArmorTrim(previewMat, pat));
            meta.setDisplayName(name);
            meta.setLore(List.of("§7Click to select this pattern"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack makeTrimMaterialItem(TrimMaterial mat, String slot,
                                           UUID uuid, TrimManager.TrimChoice current) {
        boolean selected = current != null && current.material().equals(mat);
        String name = (selected ? "§a✔ " : "§e") + capitalize(mat.getKey().getKey());
        // Use a visible ingredient item for material preview
        Material icon = trimMaterialIcon(mat);
        return makeItem(icon, name, "§7Click to select this material");
    }

    private Inventory buildShieldPage(Player player) {
        UUID uuid = player.getUniqueId();
        TrimManager.ShieldDesign draft = draftShield.get(uuid);

        Inventory inv = Bukkit.createInventory(null, 54, "§dShield Design");
        ItemStack pane = makeItem(Material.MAGENTA_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 54; i++) inv.setItem(i, pane);

        // Current summary
        String baseC = draft != null ? draft.baseColor().name()    : "WHITE";
        String patT  = draft != null ? draft.patternType().getIdentifier() : "None";
        String patC  = draft != null ? draft.patternColor().name() : "WHITE";
        inv.setItem(4, makeItem(Material.SHIELD, "§dShield Design",
            "§7Base: §f"    + baseC,
            "§7Pattern: §f" + patT,
            "§7Pattern Color: §f" + patC));

        // Base colors (row 1, slots 9-24)
        DyeColor[] dyes = DyeColor.values();
        for (int i = 0; i < Math.min(16, dyes.length); i++) {
            DyeColor dye = dyes[i];
            boolean sel  = draft != null && draft.baseColor().equals(dye);
            inv.setItem(9 + i, makeItem(dyeToWool(dye),
                (sel ? "§a✔ " : "§7") + "Base: " + dye.name(), "§7Click to set base color"));
        }

        // Pattern types (row 3, slots 27-33) — show a subset of common patterns
        List<PatternType> patterns = commonPatterns();
        for (int i = 0; i < Math.min(9, patterns.size()); i++) {
            PatternType pt = patterns.get(i);
            boolean sel    = draft != null && draft.patternType().equals(pt);
            inv.setItem(27 + i, makeItem(Material.PAPER,
                (sel ? "§a✔ " : "§d") + pt.getIdentifier(), "§7Click to set pattern type"));
        }

        // Pattern colors (row 4, slots 36-51)
        for (int i = 0; i < Math.min(16, dyes.length); i++) {
            DyeColor dye = dyes[i];
            boolean sel  = draft != null && draft.patternColor().equals(dye);
            inv.setItem(36 + i, makeItem(dyeToWool(dye),
                (sel ? "§a✔ " : "§7") + "Pattern Color: " + dye.name(), "§7Click to set pattern color"));
        }

        inv.setItem(45, makeItem(Material.BARRIER, "§cBack"));
        return inv;
    }

    private Inventory buildChatColorPage(Player player) {
        UUID uuid = player.getUniqueId();
        String current = draftChatColor.getOrDefault(uuid, "");

        Inventory inv = Bukkit.createInventory(null, 36, "§eChat Color");
        ItemStack pane = makeItem(Material.YELLOW_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        for (int i = 0; i < CHAT_COLORS.size(); i++) {
            ColorOption opt = CHAT_COLORS.get(i);
            boolean sel     = opt.code().equals(current);
            inv.setItem(10 + i, makeItem(opt.icon(),
                (sel ? "§a✔ " : "") + opt.label(),
                "§7Click to select " + opt.label() + " §7chat color"));
        }

        inv.setItem(31, makeItem(Material.BARRIER, "§cBack"));
        return inv;
    }

    private Inventory buildSwordNamePage(Player player) {
        UUID uuid = player.getUniqueId();
        String currentName  = draftSwordName.getOrDefault(uuid, "");
        String currentColor = draftSwordColor.getOrDefault(uuid, "§f");

        Inventory inv = Bukkit.createInventory(null, 36, "§6Sword Name");
        ItemStack pane = makeItem(Material.ORANGE_STAINED_GLASS_PANE, "§0");
        for (int i = 0; i < 36; i++) inv.setItem(i, pane);

        // Current name display
        inv.setItem(4, makeItem(Material.DIAMOND_SWORD, "§6Current Sword Name",
            "§7Name: " + currentColor + currentName,
            "§7(Max 20 characters)"));

        // Set name button
        inv.setItem(11, makeItem(Material.WRITABLE_BOOK, "§eSet Name",
            "§7Click then type your sword name in chat."));

        // Clear name button
        inv.setItem(15, makeItem(Material.BARRIER, "§cClear Name",
            "§7Removes the custom sword name."));

        // Color picker row
        for (int i = 0; i < CHAT_COLORS.size(); i++) {
            ColorOption opt = CHAT_COLORS.get(i);
            boolean sel     = opt.code().equals(currentColor);
            inv.setItem(18 + i, makeItem(opt.icon(),
                (sel ? "§a✔ " : "") + opt.label(),
                "§7Click to select name color"));
        }

        inv.setItem(31, makeItem(Material.BARRIER, "§cBack"));
        return inv;
    }

    // ── Click handler ───────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        if (!pageMap.containsKey(uuid)) return;
        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        String name = clicked.hasItemMeta() && clicked.getItemMeta().hasDisplayName()
            ? clicked.getItemMeta().getDisplayName() : "";

        int page = pageMap.get(uuid);

        switch (page) {
            case PAGE_MAIN       -> handleMain(player, uuid, name);
            case PAGE_HELMET     -> handleArmorPage(player, uuid, "helmet",     name, clicked, event.getSlot());
            case PAGE_CHESTPLATE -> handleArmorPage(player, uuid, "chestplate", name, clicked, event.getSlot());
            case PAGE_LEGGINGS   -> handleArmorPage(player, uuid, "leggings",   name, clicked, event.getSlot());
            case PAGE_BOOTS      -> handleArmorPage(player, uuid, "boots",      name, clicked, event.getSlot());
            case PAGE_SHIELD     -> handleShieldPage(player, uuid, name, event.getSlot());
            case PAGE_CHAT_COLOR -> handleChatColorPage(player, uuid, name);
            case PAGE_SWORD_NAME -> handleSwordNamePage(player, uuid, name);
        }
    }

    private void handleMain(Player player, UUID uuid, String name) {
        String clean = stripColor(name);
        switch (clean) {
            case "Helmet Trim"     -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_HELMET);     }
            case "Chestplate Trim" -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_CHESTPLATE); }
            case "Leggings Trim"   -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_LEGGINGS);   }
            case "Boots Trim"      -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_BOOTS);      }
            case "Shield Design"   -> openPage(player, PAGE_SHIELD);
            case "Chat Color"      -> openPage(player, PAGE_CHAT_COLOR);
            case "Sword Name"      -> openPage(player, PAGE_SWORD_NAME);
            case "SAVE"            -> handleSave(player, uuid);
        }
    }

    private void handleArmorPage(Player player, UUID uuid, String slot,
                                  String name, ItemStack clicked, int slotIndex) {
        String clean = stripColor(name);
        if (clean.equals("Back")) { patternPage.remove(uuid); materialPage.remove(uuid); openPage(player, PAGE_MAIN); return; }
        if (clean.equals("← Previous Patterns")) { patternPage.merge(uuid, -1, Integer::sum); openPage(player, pageMap.get(uuid)); return; }
        if (clean.equals("Next Patterns →"))     { patternPage.merge(uuid,  1, Integer::sum); openPage(player, pageMap.get(uuid)); return; }
        if (clean.equals("← Previous Materials")) { materialPage.merge(uuid, -1, Integer::sum); openPage(player, pageMap.get(uuid)); return; }
        if (clean.equals("Next Materials →"))      { materialPage.merge(uuid,  1, Integer::sum); openPage(player, pageMap.get(uuid)); return; }

        // Check if it's a pattern selection (slots 9-15)
        if (slotIndex >= 9 && slotIndex <= 15) {
            int idx = patternPage.getOrDefault(uuid, 0) * 7 + (slotIndex - 9);
            if (idx < ALL_PATTERNS.size()) {
                TrimPattern pat  = ALL_PATTERNS.get(idx);
                // Keep existing material or default
                TrimManager.TrimChoice current = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimMaterial mat = current != null ? current.material() : TrimMaterial.GOLD;
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, pageMap.get(uuid));
                return;
            }
        }

        // Material selection (slots 27-33)
        if (slotIndex >= 27 && slotIndex <= 33) {
            int idx = materialPage.getOrDefault(uuid, 0) * 7 + (slotIndex - 27);
            if (idx < ALL_MATERIALS.size()) {
                TrimMaterial mat = ALL_MATERIALS.get(idx);
                TrimManager.TrimChoice current = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimPattern pat  = current != null ? current.pattern() : TrimPattern.SENTRY;
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, pageMap.get(uuid));
                return;
            }
        }
    }

    private void handleShieldPage(Player player, UUID uuid, String name, int slotIndex) {
        String clean = stripColor(name);
        if (clean.equals("Back")) { openPage(player, PAGE_MAIN); return; }

        TrimManager.ShieldDesign current = draftShield.get(uuid);
        DyeColor baseColor    = current != null ? current.baseColor()    : DyeColor.WHITE;
        PatternType patType   = current != null ? current.patternType()  : PatternType.BASE;
        DyeColor patColor     = current != null ? current.patternColor() : DyeColor.BLACK;

        // Base color row: slots 9-24
        if (slotIndex >= 9 && slotIndex <= 24) {
            DyeColor[] dyes = DyeColor.values();
            int idx = slotIndex - 9;
            if (idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, patColor, dyes[idx]));
                openPage(player, PAGE_SHIELD); return;
            }
        }

        // Pattern type row: slots 27-35
        if (slotIndex >= 27 && slotIndex <= 35) {
            List<PatternType> patterns = commonPatterns();
            int idx = slotIndex - 27;
            if (idx < patterns.size()) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patterns.get(idx), patColor, baseColor));
                openPage(player, PAGE_SHIELD); return;
            }
        }

        // Pattern color row: slots 36-51
        if (slotIndex >= 36 && slotIndex <= 51) {
            DyeColor[] dyes = DyeColor.values();
            int idx = slotIndex - 36;
            if (idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, dyes[idx], baseColor));
                openPage(player, PAGE_SHIELD);
            }
        }
    }

    private void handleChatColorPage(Player player, UUID uuid, String name) {
        String clean = stripColor(name);
        if (clean.equals("Back")) { openPage(player, PAGE_MAIN); return; }
        for (ColorOption opt : CHAT_COLORS) {
            if (stripColor(opt.label()).equals(clean)) {
                draftChatColor.put(uuid, opt.code());
                player.sendMessage("§8[§6FFA§8] §7Chat color preview: " + opt.code() + "This is how your messages will look!");
                openPage(player, PAGE_CHAT_COLOR);
                return;
            }
        }
    }

    private void handleSwordNamePage(Player player, UUID uuid, String name) {
        String clean = stripColor(name);
        if (clean.equals("Back"))       { openPage(player, PAGE_MAIN); return; }
        if (clean.equals("Clear Name")) {
            draftSwordName.put(uuid, "");
            openPage(player, PAGE_SWORD_NAME);
            return;
        }
        if (clean.equals("Set Name")) {
            player.closeInventory();
            awaitingName.add(uuid);
            player.sendMessage("§8[§6FFA§8] §eType your sword name in chat §7(max 20 chars). Type §ccancel §7to abort.");
            return;
        }
        // Color selection
        for (ColorOption opt : CHAT_COLORS) {
            if (stripColor(opt.label()).equals(clean)) {
                draftSwordColor.put(uuid, opt.code());
                openPage(player, PAGE_SWORD_NAME);
                return;
            }
        }
    }

    // ── Chat input for sword name ───────────────────────────────────

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!awaitingName.contains(uuid)) return;
        event.setCancelled(true);
        awaitingName.remove(uuid);
        String input = event.getMessage();
        Player player = event.getPlayer();
        if (input.equalsIgnoreCase("cancel")) {
            Bukkit.getScheduler().runTask(plugin, () -> openPage(player, PAGE_SWORD_NAME));
            return;
        }
        String trimmed = input.length() > 20 ? input.substring(0, 20) : input;
        draftSwordName.put(uuid, trimmed);
        player.sendMessage("§8[§6FFA§8] §7Sword name set to: "
            + draftSwordColor.getOrDefault(uuid, "§f") + trimmed);
        Bukkit.getScheduler().runTask(plugin, () -> openPage(player, PAGE_SWORD_NAME));
    }

    // ── Save handler ────────────────────────────────────────────────

    private void handleSave(Player player, UUID uuid) {
        if (plugin.getRankManager().getSavesLeft(uuid) <= 0) {
            player.sendMessage("§8[§6FFA§8] §cNo save charges left!");
            return;
        }
        if (!plugin.getRankManager().consumeSave(uuid)) {
            player.sendMessage("§8[§6FFA§8] §cFailed to consume save charge.");
            return;
        }

        // Commit draft trims
        Map<String, TrimManager.TrimChoice> trims = draftTrims.getOrDefault(uuid, new HashMap<>());
        for (Map.Entry<String, TrimManager.TrimChoice> e : trims.entrySet()) {
            plugin.getTrimManager().setTrimChoice(uuid, e.getKey(), e.getValue().pattern(), e.getValue().material());
        }

        // Commit shield design
        TrimManager.ShieldDesign shield = draftShield.get(uuid);
        if (shield != null) {
            plugin.getTrimManager().setShieldDesign(uuid, shield.patternType(), shield.patternColor(), shield.baseColor());
        }

        // Commit chat color
        String cc = draftChatColor.get(uuid);
        if (cc != null) plugin.getRankManager().setChatColor(uuid, cc);

        // Commit sword name
        String sn = draftSwordName.getOrDefault(uuid, "");
        String sc = draftSwordColor.getOrDefault(uuid, "§f");
        plugin.getRankManager().setSwordName(uuid, sn, sc);

        // Apply trims to currently worn armor immediately
        plugin.getTrimManager().applyTrims(player);

        // Apply custom sword name to sword in hand/slot 0
        plugin.getKitManager().applyCustomSwordName(player);

        // Save rank data
        plugin.getRankManager().save();

        player.sendMessage("§8[§6FFA§8] §a§lSaved! §7Your customisations have been applied.");
        player.sendMessage("§8[§6FFA§8] §7Saves remaining: §e" + plugin.getRankManager().getSavesLeft(uuid));

        // Refresh main menu
        openPage(player, PAGE_MAIN);
    }

    // ── Inventory close cleanup ─────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        pageMap.remove(uuid);
        patternPage.remove(uuid);
        materialPage.remove(uuid);
        editingSlot.remove(uuid);
        // Note: draft data cleared on next open(). awaitingName intentionally kept.
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta  = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replaceAll("✔ ", "");
    }

    private Material dyeToWool(DyeColor dye) {
        return switch (dye) {
            case WHITE      -> Material.WHITE_WOOL;
            case ORANGE     -> Material.ORANGE_WOOL;
            case MAGENTA    -> Material.MAGENTA_WOOL;
            case LIGHT_BLUE -> Material.LIGHT_BLUE_WOOL;
            case YELLOW     -> Material.YELLOW_WOOL;
            case LIME       -> Material.LIME_WOOL;
            case PINK       -> Material.PINK_WOOL;
            case GRAY       -> Material.GRAY_WOOL;
            case LIGHT_GRAY -> Material.LIGHT_GRAY_WOOL;
            case CYAN       -> Material.CYAN_WOOL;
            case PURPLE     -> Material.PURPLE_WOOL;
            case BLUE       -> Material.BLUE_WOOL;
            case BROWN      -> Material.BROWN_WOOL;
            case GREEN      -> Material.GREEN_WOOL;
            case RED        -> Material.RED_WOOL;
            case BLACK      -> Material.BLACK_WOOL;
        };
    }

    private Material trimMaterialIcon(TrimMaterial mat) {
        String key = mat.getKey().getKey().toUpperCase();
        return switch (key) {
            case "GOLD"       -> Material.GOLD_INGOT;
            case "SILVER", "IRON" -> Material.IRON_INGOT;
            case "DIAMOND"    -> Material.DIAMOND;
            case "NETHERITE"  -> Material.NETHERITE_INGOT;
            case "REDSTONE"   -> Material.REDSTONE;
            case "COPPER"     -> Material.COPPER_INGOT;
            case "AMETHYST"   -> Material.AMETHYST_SHARD;
            case "EMERALD"    -> Material.EMERALD;
            case "LAPIS"      -> Material.LAPIS_LAZULI;
            case "QUARTZ"     -> Material.QUARTZ;
            default           -> Material.GOLD_INGOT;
        };
    }

    private List<PatternType> commonPatterns() {
        return List.of(
            PatternType.BASE, PatternType.STRIPE_BOTTOM, PatternType.STRIPE_TOP,
            PatternType.STRIPE_LEFT, PatternType.STRIPE_RIGHT, PatternType.STRIPE_CENTER,
            PatternType.STRIPE_MIDDLE, PatternType.STRIPE_DOWNRIGHT, PatternType.STRIPE_DOWNLEFT,
            PatternType.CROSS, PatternType.STRAIGHT_CROSS, PatternType.DIAGONAL_UP,
            PatternType.DIAGONAL_DOWN, PatternType.HALF_VERTICAL, PatternType.HALF_HORIZONTAL,
            PatternType.SKULL, PatternType.CREEPER, PatternType.FLOWER, PatternType.BRICKS
        );
    }


}
