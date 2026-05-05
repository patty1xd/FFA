package com.ffa.gui;

import com.ffa.FFAPlugin;
import com.ffa.managers.TrimManager;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
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

import java.util.*;

public class TrimsGUI implements Listener {

    private final FFAPlugin plugin;

    private final Map<UUID, Integer> pageMap      = new HashMap<>();
    private final Map<UUID, Integer> patternPage  = new HashMap<>();
    private final Map<UUID, Integer> materialPage = new HashMap<>();
    private final Set<UUID>          awaitingName = new HashSet<>();

    // Draft state — committed only on SAVE
    private final Map<UUID, Map<String, TrimManager.TrimChoice>> draftTrims  = new HashMap<>();
    private final Map<UUID, TrimManager.ShieldDesign>            draftShield = new HashMap<>();
    private final Map<UUID, String> draftChatColor  = new HashMap<>();
    private final Map<UUID, String> draftSwordName  = new HashMap<>();
    private final Map<UUID, String> draftSwordColor = new HashMap<>();

    // Page IDs
    private static final int PAGE_MAIN       = 0;
    private static final int PAGE_HELMET     = 1;
    private static final int PAGE_CHESTPLATE = 2;
    private static final int PAGE_LEGGINGS   = 3;
    private static final int PAGE_BOOTS      = 4;
    private static final int PAGE_SHIELD     = 5;
    private static final int PAGE_CHAT_COLOR = 6;
    private static final int PAGE_SWORD_NAME = 7;

    // GUI title prefixes — used to detect our inventories
    private static final String TITLE_MAIN       = "§d§lDonor Customisation";
    private static final String TITLE_HELMET     = "§bHelmet Trim";
    private static final String TITLE_CHESTPLATE = "§bChestplate Trim";
    private static final String TITLE_LEGGINGS   = "§bLeggings Trim";
    private static final String TITLE_BOOTS      = "§bBoots Trim";
    private static final String TITLE_SHIELD     = "§dShield Design";
    private static final String TITLE_CHAT       = "§eChat Color";
    private static final String TITLE_SWORD      = "§6Sword Name";

    private static final List<TrimPattern>  ALL_PATTERNS;
    private static final List<TrimMaterial> ALL_MATERIALS;
    private static final List<PatternType>  ALL_BANNER_PATTERNS;

    static {
        List<TrimPattern> tp = new ArrayList<>();
        Registry.TRIM_PATTERN.forEach(tp::add);
        ALL_PATTERNS = Collections.unmodifiableList(tp);

        List<TrimMaterial> tm = new ArrayList<>();
        Registry.TRIM_MATERIAL.forEach(tm::add);
        ALL_MATERIALS = Collections.unmodifiableList(tm);

        List<PatternType> bp = new ArrayList<>();
        Registry.BANNER_PATTERN.forEach(bp::add);
        ALL_BANNER_PATTERNS = Collections.unmodifiableList(bp);
    }

    private static final List<ColorOption> CHAT_COLORS = List.of(
        new ColorOption("Green",        "§a", Material.LIME_WOOL),
        new ColorOption("Dark Green",   "§2", Material.GREEN_WOOL),
        new ColorOption("Aqua",         "§b", Material.CYAN_WOOL),
        new ColorOption("Dark Aqua",    "§3", Material.LIGHT_BLUE_WOOL),
        new ColorOption("Blue",         "§9", Material.BLUE_WOOL),
        new ColorOption("Light Purple", "§d", Material.MAGENTA_WOOL),
        new ColorOption("Dark Purple",  "§5", Material.PURPLE_WOOL),
        new ColorOption("Gold",         "§6", Material.ORANGE_WOOL),
        new ColorOption("Yellow",       "§e", Material.YELLOW_WOOL),
        new ColorOption("Red",          "§c", Material.RED_WOOL),
        new ColorOption("Dark Red",     "§4", Material.BROWN_WOOL),
        new ColorOption("White",        "§f", Material.WHITE_WOOL),
        new ColorOption("Gray",         "§7", Material.LIGHT_GRAY_WOOL),
        new ColorOption("Dark Gray",    "§8", Material.GRAY_WOOL),
        new ColorOption("Black",        "§0", Material.BLACK_WOOL)
    );

    // ── FIX 3: Richer sword style options ──────────────────────────────
    // Each entry: label (shown), code (applied), icon material
    // We store the full formatting code (color + decoration) in draftSwordColor
    private static final List<SwordStyle> SWORD_STYLES = List.of(
        new SwordStyle("§a§lBold Green",        "§a§l", Material.LIME_WOOL),
        new SwordStyle("§2§lBold Dark Green",   "§2§l", Material.GREEN_WOOL),
        new SwordStyle("§b§lBold Aqua",         "§b§l", Material.CYAN_WOOL),
        new SwordStyle("§3§lBold Dark Aqua",    "§3§l", Material.LIGHT_BLUE_WOOL),
        new SwordStyle("§9§lBold Blue",         "§9§l", Material.BLUE_WOOL),
        new SwordStyle("§d§lBold Pink",         "§d§l", Material.MAGENTA_WOOL),
        new SwordStyle("§5§lBold Purple",       "§5§l", Material.PURPLE_WOOL),
        new SwordStyle("§6§lBold Gold",         "§6§l", Material.GOLD_INGOT),
        new SwordStyle("§e§lBold Yellow",       "§e§l", Material.YELLOW_WOOL),
        new SwordStyle("§c§lBold Red",          "§c§l", Material.RED_WOOL),
        new SwordStyle("§4§lBold Dark Red",     "§4§l", Material.BROWN_WOOL),
        new SwordStyle("§f§lBold White",        "§f§l", Material.WHITE_WOOL),
        new SwordStyle("§b§oItalic Aqua",       "§b§o", Material.PRISMARINE_SHARD),
        new SwordStyle("§6§oItalic Gold",       "§6§o", Material.GOLD_NUGGET),
        new SwordStyle("§d§oItalic Pink",       "§d§o", Material.PINK_PETALS),
        new SwordStyle("§c§oItalic Red",        "§c§o", Material.POPPY),
        new SwordStyle("§a§l§oGold+Italic",     "§a§l§o", Material.FERN),
        new SwordStyle("§6§l§oGold Bold+Italic","§6§l§o", Material.BLAZE_POWDER),
        new SwordStyle("§f§kObfuscated",        "§f§k", Material.ENDER_EYE),
        new SwordStyle("§c§l§kRedObf",          "§c§l§k", Material.NETHER_STAR)
    );

    private record ColorOption(String label, String code, Material icon) {}
    private record SwordStyle(String label, String code, Material icon) {}

    public TrimsGUI(FFAPlugin plugin) { this.plugin = plugin; }

    // ── Open ─────────────────────────────────────────────────────────

    public void open(Player player) {
        UUID uuid = player.getUniqueId();
        if (!plugin.getRankManager().hasRank(uuid)) {
            player.sendMessage("§8[§6FFA§8] §cYou need the §dDonor §crank to use this!");
            return;
        }
        loadDraft(uuid);
        openPage(player, PAGE_MAIN);
    }

    private void loadDraft(UUID uuid) {
        draftTrims.put(uuid, new HashMap<>(plugin.getTrimManager().getAllTrimChoices(uuid)));
        TrimManager.ShieldDesign sd = plugin.getTrimManager().getShieldDesign(uuid);
        if (sd != null) draftShield.put(uuid, sd);
        String cc = plugin.getRankManager().getChatColor(uuid);
        if (cc != null && !cc.isBlank()) draftChatColor.put(uuid, cc);
        draftSwordName.put(uuid,  plugin.getRankManager().getRawSwordName(uuid));
        draftSwordColor.put(uuid, plugin.getRankManager().getSwordColor(uuid));
    }

    private void openPage(Player player, int page) {
        pageMap.put(player.getUniqueId(), page);
        Inventory inv = buildPage(player, page);
        if (inv != null) player.openInventory(inv);
    }

    // ── Page builders ────────────────────────────────────────────────

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

    // ── FIX 4: Main menu shows trimmed armor previews ─────────────────
    private Inventory buildMainMenu(Player player) {
        UUID uuid  = player.getUniqueId();
        int  saves = plugin.getRankManager().getSavesLeft(uuid);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN + " §8| §7" + saves + " save(s) left");

        fill(inv, Material.PURPLE_STAINED_GLASS_PANE);

        Map<String, TrimManager.TrimChoice> trims = draftTrims.getOrDefault(uuid, new HashMap<>());

        inv.setItem(10, makeTrimmedArmorItem(Material.DIAMOND_HELMET,     trims.get("helmet"),     "§bHelmet Trim"));
        inv.setItem(19, makeTrimmedArmorItem(Material.DIAMOND_CHESTPLATE, trims.get("chestplate"), "§bChestplate Trim"));
        inv.setItem(28, makeTrimmedArmorItem(Material.DIAMOND_LEGGINGS,   trims.get("leggings"),   "§bLeggings Trim"));
        inv.setItem(37, makeTrimmedArmorItem(Material.DIAMOND_BOOTS,      trims.get("boots"),      "§bBoots Trim"));
        inv.setItem(13, makeItem(Material.SHIELD, "§dShield Design", "§7Click to customise"));

        String cc = draftChatColor.getOrDefault(uuid, "§7");
        inv.setItem(22, makeItem(Material.NAME_TAG, "§eChat Color",
            "§7Current: " + cc + "Sample text§r", "§7Click to change"));

        String sn = draftSwordName.getOrDefault(uuid, "");
        String sc = draftSwordColor.getOrDefault(uuid, "§f");
        inv.setItem(31, makeItem(Material.DIAMOND_SWORD, "§6Sword Name",
            "§7Current: " + sc + (sn.isBlank() ? "None" : sn), "§7Click to change"));

        if (saves > 0) {
            inv.setItem(49, makeItem(Material.EMERALD, "§a§lSAVE",
                "§7Applies all pending changes.",
                "§7Saves remaining: §e" + saves,
                "§cUses 1 save charge!"));
        } else {
            inv.setItem(49, makeItem(Material.BARRIER, "§c§lNO SAVES LEFT",
                "§7Purchase the rank again to get more."));
        }
        return inv;
    }

    /**
     * Creates a trimmed armor item for the main menu preview.
     * If no trim is selected, returns a plain item with lore.
     */
    private ItemStack makeTrimmedArmorItem(Material mat, TrimManager.TrimChoice choice, String title) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof ArmorMeta am) {
            if (choice != null) {
                am.setTrim(new ArmorTrim(choice.material(), choice.pattern()));
                am.setDisplayName(title);
                am.setLore(List.of(
                    "§7Pattern:  §e" + capitalize(choice.pattern().getKey().getKey()),
                    "§7Material: §e" + capitalize(choice.material().getKey().getKey()),
                    "§7Click to customise"
                ));
            } else {
                am.setDisplayName(title);
                am.setLore(List.of("§7No trim selected", "§7Click to customise"));
            }
            item.setItemMeta(am);
        }
        return item;
    }

    private Inventory buildArmorPage(Player player, String slot, Material icon) {
        UUID uuid  = player.getUniqueId();
        int  pPage = patternPage.getOrDefault(uuid, 0);
        int  mPage = materialPage.getOrDefault(uuid, 0);

        String title = switch (slot) {
            case "helmet"     -> TITLE_HELMET;
            case "chestplate" -> TITLE_CHESTPLATE;
            case "leggings"   -> TITLE_LEGGINGS;
            default           -> TITLE_BOOTS;
        };
        Inventory inv = Bukkit.createInventory(null, 54, title);
        fill(inv, Material.CYAN_STAINED_GLASS_PANE);

        TrimManager.TrimChoice current = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
        String patName = current != null ? current.pattern().getKey().getKey()  : "None";
        String matName = current != null ? current.material().getKey().getKey() : "None";

        // Preview item in slot 4 with actual trim applied
        ItemStack previewItem = new ItemStack(icon);
        ItemMeta previewMeta = previewItem.getItemMeta();
        if (previewMeta instanceof ArmorMeta am && current != null) {
            am.setTrim(new ArmorTrim(current.material(), current.pattern()));
        }
        if (previewMeta != null) {
            previewMeta.setDisplayName("§b" + capitalize(slot));
            previewMeta.setLore(List.of(
                "§7Pattern:  §e" + patName,
                "§7Material: §e" + matName,
                "§7Pick a pattern (top rows) or material (bottom rows)"
            ));
            previewItem.setItemMeta(previewMeta);
        }
        inv.setItem(4, previewItem);

        // ── Patterns: slots 9–15 (7 per page) ──
        // FIX 1: patStart is correctly used for both display AND index reference
        int patStart = pPage * 7;
        for (int i = 0; i < 7 && (patStart + i) < ALL_PATTERNS.size(); i++) {
            TrimPattern pat = ALL_PATTERNS.get(patStart + i);
            boolean sel     = current != null && current.pattern().equals(pat);
            ItemStack pi    = new ItemStack(icon);
            ItemMeta  meta  = pi.getItemMeta();
            if (meta instanceof ArmorMeta am) {
                TrimMaterial previewMat = current != null ? current.material()
                    : Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft("gold"));
                if (previewMat != null) am.setTrim(new ArmorTrim(previewMat, pat));
                am.setDisplayName((sel ? "§a✔ " : "§7") + capitalize(pat.getKey().getKey()));
                am.setLore(List.of("§7Click to select this pattern"));
                pi.setItemMeta(am);
            }
            inv.setItem(9 + i, pi);
        }
        // Pattern nav: 16 = prev, 17 = next
        if (pPage > 0)
            inv.setItem(16, makeItem(Material.ARROW, "§7← Prev Patterns",
                "§8Page " + pPage + " / " + ((ALL_PATTERNS.size() - 1) / 7)));
        if (patStart + 7 < ALL_PATTERNS.size())
            inv.setItem(17, makeItem(Material.ARROW, "§7Next Patterns →",
                "§8Page " + (pPage + 2) + " / " + ((ALL_PATTERNS.size() - 1) / 7 + 1)));

        // ── Materials: slots 27–33 (7 per page) ──
        int matStart = mPage * 7;
        for (int i = 0; i < 7 && (matStart + i) < ALL_MATERIALS.size(); i++) {
            TrimMaterial mat = ALL_MATERIALS.get(matStart + i);
            boolean sel      = current != null && current.material().equals(mat);
            inv.setItem(27 + i, makeItem(trimMaterialIcon(mat),
                (sel ? "§a✔ " : "§e") + capitalize(mat.getKey().getKey()),
                "§7Click to select this material"));
        }
        // Material nav: 34 = prev, 35 = next
        if (mPage > 0)
            inv.setItem(34, makeItem(Material.ARROW, "§7← Prev Materials",
                "§8Page " + mPage + " / " + ((ALL_MATERIALS.size() - 1) / 7)));
        if (matStart + 7 < ALL_MATERIALS.size())
            inv.setItem(35, makeItem(Material.ARROW, "§7Next Materials →",
                "§8Page " + (mPage + 2) + " / " + ((ALL_MATERIALS.size() - 1) / 7 + 1)));

        // Back: slot 45
        inv.setItem(45, makeItem(Material.BARRIER, "§cBack to Menu"));
        return inv;
    }

    // ── FIX 2: Shield page redesigned like a loom ─────────────────────
    // Layout (54 slots):
    //   Col 0-1 (slots 0,9,18,27,36,45 etc.) = base color palette (left column)
    //   Col 2-6 center area = pattern type grid
    //   Col 7-8 (slots 7,16,25,34,43,52 etc.) = pattern color palette (right column)
    //   Row 0 top = labels/preview
    //
    // Simplified to a clean 54-slot loom-style layout:
    //   Slots 0-8   = top row: [base label][...][preview shield][...][pat color label]
    //   Slots 9-17  = base colors column (left 3: 9,10,11) | patterns center (12-14) | pat colors right (15,16,17)
    //   Etc.
    //
    // Actual clean layout:
    //   Left column  (slots 0,9,18,27,36,45)       = base color swatches (6 visible + scroll)
    //   Center area  (slots 2-6, 11-15, 20-24, ...) = pattern type buttons (5 wide, 5 tall = 25)
    //   Right column (slots 8,17,26,35,44,53)       = pattern color swatches
    //   Slot 1 = "Base Color" label, slot 7 = "Pat Color" label
    //   Slot 4 (top center) = preview / current selection info
    //
    // We'll use a 54-slot inv, loom-inspired:
    //   Col 0 (slots 0,9,18,27,36,45)   = base color choices (up to 6, paged)
    //   Col 1 = divider glass
    //   Cols 2-6 (slots 2-6, 11-15, etc.) = pattern types (5 cols × 5 rows = 25 patterns, paged)
    //   Col 7 = divider glass
    //   Col 8 (slots 8,17,26,35,44,53) = pattern color choices (up to 6, paged)
    //   Row 0: base label, div, [pattern nav prev][page info][pattern nav next], div, patcolor label
    //   Row 5: base scroll, div, [back][preview shield][save/ok], div, patcolor scroll
    private Inventory buildShieldPage(Player player) {
        UUID uuid = player.getUniqueId();
        TrimManager.ShieldDesign draft = draftShield.get(uuid);

        // Page state for shield (reuse patternPage for banner pattern page, materialPage for color pages)
        int bannerPatPage = patternPage.getOrDefault(uuid, 0);
        int baseColorPage = materialPage.getOrDefault(uuid, 0);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SHIELD);

        // Fill everything with dark glass first
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);

        // Divider columns (col 1 and col 7) with magenta glass
        int[] dividerSlots = {1,10,19,28,37,46, 7,16,25,34,43,52};
        for (int s : dividerSlots) inv.setItem(s, makeItem(Material.MAGENTA_STAINED_GLASS_PANE, "§0"));

        // Current selections
        DyeColor    baseColor = draft != null ? draft.baseColor()    : DyeColor.WHITE;
        PatternType patType   = draft != null ? draft.patternType()  : ALL_BANNER_PATTERNS.get(0);
        DyeColor    patColor  = draft != null ? draft.patternColor() : DyeColor.BLACK;
        DyeColor[]  dyes      = DyeColor.values();

        // ── Header row ──
        // Slot 0: Base color label
        inv.setItem(0, makeItem(dyeToWool(baseColor), "§fBase Color",
            "§7Selected: §e" + baseColor.name(), "§7← Scroll ↓"));
        // Slot 4: Preview shield (uses BlockStateMeta banner)
        inv.setItem(4, makeShieldPreview(draft, patType, patColor, baseColor));
        // Slot 8: Pattern color label
        inv.setItem(8, makeItem(dyeToWool(patColor), "§fPattern Color",
            "§7Selected: §e" + patColor.name(), "§7Scroll ↓ →"));

        // ── Left column: base colors (col 0, rows 1-4 = slots 9,18,27,36) ──
        int baseStart = baseColorPage * 4;
        for (int i = 0; i < 4 && (baseStart + i) < dyes.length; i++) {
            DyeColor dc = dyes[baseStart + i];
            boolean sel = dc == baseColor;
            inv.setItem(9 + i * 9, makeItem(dyeToWool(dc),
                (sel ? "§a✔ " : "§7") + dc.name(),
                "§7Click to set base color"));
        }
        // Base color nav: slot 45 = prev/next
        if (baseColorPage > 0)
            inv.setItem(45, makeItem(Material.ARROW, "§7↑ More Colors"));
        else if (baseStart + 4 < dyes.length)
            inv.setItem(45, makeItem(Material.ARROW, "§7↓ More Colors"));

        // ── Center area: pattern types (cols 2-6, rows 1-4 = 20 slots per page) ──
        // Slots: row1=11-15, row2=20-24, row3=29-33, row4=38-42
        int[] centerSlots = {11,12,13,14,15, 20,21,22,23,24, 29,30,31,32,33, 38,39,40,41,42};
        int patStart = bannerPatPage * centerSlots.length;
        for (int i = 0; i < centerSlots.length && (patStart + i) < ALL_BANNER_PATTERNS.size(); i++) {
            PatternType pt = ALL_BANNER_PATTERNS.get(patStart + i);
            boolean sel    = pt.equals(patType);
            inv.setItem(centerSlots[i], makeItem(
                sel ? Material.FILLED_MAP : Material.MAP,
                (sel ? "§a✔ " : "§d") + formatBannerPatternName(pt),
                "§7Click to select pattern"));
        }
        // Pattern nav row (row 5, center): slots 47,48,49,50,51
        if (bannerPatPage > 0)
            inv.setItem(47, makeItem(Material.ARROW, "§7← Prev Patterns"));
        inv.setItem(49, makeItem(Material.SHIELD, "§dCurrent Design",
            "§7Base: §f" + baseColor.name(),
            "§7Pattern: §f" + formatBannerPatternName(patType),
            "§7Color: §f" + patColor.name()));
        if (patStart + centerSlots.length < ALL_BANNER_PATTERNS.size())
            inv.setItem(51, makeItem(Material.ARROW, "§7Next Patterns →"));

        // Back button
        inv.setItem(45, makeItem(Material.BARRIER, "§cBack to Menu"));

        // ── Right column: pattern colors (col 8, rows 1-4 = slots 17,26,35,44) ──
        // We just show 4 dye colors per "page" — but there are 16, so we need a separate page tracker.
        // Reuse patternPage for banner patterns; use a shield-specific key by storing in materialPage offset.
        // Actually let's just show all 16 in two halves: top 8 rows 1-2, bottom 8 rows 3-4 — no, col 8 only has 4 rows.
        // Show first 4 dyes in rows 1-4 right column, with scroll at row 5.
        // We'll use a hidden "colorPage" via a separate map not currently used — let's add shieldColorPage logic
        // using the patternPage map is taken, materialPage is taken for baseColorPage...
        // Simple: split into two groups with slot 53 as toggle.
        // For now use materialPage for base scroll and a fixed offset for right col.
        // Right col shows dyes[0-3] always in rows 1-4 and dyes[4-7] etc if scrolled.
        // We'll track right-col separately - but we've used both maps. 
        // Pragmatic solution: right column shows 4 at a time, we'll use the HIGH bits of materialPage.
        // materialPage low nibble = baseColorPage, high nibble = patColorPage. But that's hacky.
        // Cleanest: just store patColorPage in a new map. Let's add one.
        // --> See shieldPatColorPage map added below the field declarations.
        int patColorPage = shieldPatColorPage.getOrDefault(uuid, 0);
        int patColorStart = patColorPage * 4;
        for (int i = 0; i < 4 && (patColorStart + i) < dyes.length; i++) {
            DyeColor dc = dyes[patColorStart + i];
            boolean sel = dc == patColor;
            inv.setItem(17 + i * 9, makeItem(dyeToWool(dc),
                (sel ? "§a✔ " : "§7") + dc.name(),
                "§7Click to set pattern color"));
        }
        // Pat color nav: slot 53
        if (patColorPage > 0 || patColorStart + 4 < dyes.length)
            inv.setItem(53, makeItem(Material.ARROW,
                patColorStart + 4 < dyes.length ? "§7↓ More" : "§7↑ Back"));

        return inv;
    }

    /** Builds a banner/shield preview item showing the current draft design. */
    private ItemStack makeShieldPreview(TrimManager.ShieldDesign draft, PatternType patType, DyeColor patColor, DyeColor baseColor) {
        ItemStack shield = new ItemStack(Material.SHIELD);
        ItemMeta meta = shield.getItemMeta();
        if (meta instanceof BlockStateMeta bsm) {
            Banner banner = (Banner) bsm.getBlockState();
            banner.setBaseColor(baseColor);
            banner.getPatterns().clear();
            banner.addPattern(new Pattern(patColor, patType));
            bsm.setBlockState(banner);
            bsm.setDisplayName("§dShield Preview");
            bsm.setLore(List.of(
                "§7Base:    §f" + baseColor.name(),
                "§7Pattern: §f" + formatBannerPatternName(patType),
                "§7Color:   §f" + patColor.name()
            ));
            shield.setItemMeta(bsm);
        }
        return shield;
    }

    private String formatBannerPatternName(PatternType pt) {
        return capitalize(pt.getKey().getKey().replace("_", " "));
    }

    private Inventory buildChatColorPage(Player player) {
        UUID uuid    = player.getUniqueId();
        String current = draftChatColor.getOrDefault(uuid, "");
        Inventory inv  = Bukkit.createInventory(null, 36, TITLE_CHAT);
        fill(inv, Material.YELLOW_STAINED_GLASS_PANE);
        for (int i = 0; i < CHAT_COLORS.size(); i++) {
            ColorOption opt = CHAT_COLORS.get(i);
            boolean sel     = opt.code().equals(current);
            inv.setItem(10 + i, makeItem(opt.icon(),
                (sel ? "§a✔ " : opt.code()) + opt.label(),
                "§7Click to select this chat color"));
        }
        inv.setItem(31, makeItem(Material.BARRIER, "§cBack to Menu"));
        return inv;
    }

    // ── FIX 3: Sword name page uses SWORD_STYLES ──────────────────────
    private Inventory buildSwordNamePage(Player player) {
        UUID uuid        = player.getUniqueId();
        String curName   = draftSwordName.getOrDefault(uuid, "");
        String curStyle  = draftSwordColor.getOrDefault(uuid, "§f");
        Inventory inv    = Bukkit.createInventory(null, 54, TITLE_SWORD);
        fill(inv, Material.ORANGE_STAINED_GLASS_PANE);

        inv.setItem(4, makeItem(Material.DIAMOND_SWORD, "§6Current Sword Name",
            "§7Name:  " + curStyle + (curName.isBlank() ? "None" : curName),
            "§7Style: §7" + curStyle + "preview§r",
            "§7Max 20 characters"));
        inv.setItem(11, makeItem(Material.WRITABLE_BOOK, "§eSet Name",
            "§7Click, then type your sword name in chat.",
            "§7Type §ccancel §7to abort."));
        inv.setItem(15, makeItem(Material.BARRIER, "§cClear Name",
            "§7Removes the custom sword name."));

        // Sword style grid: slots 18-37 (20 styles)
        for (int i = 0; i < SWORD_STYLES.size(); i++) {
            SwordStyle s = SWORD_STYLES.get(i);
            boolean sel  = s.code().equals(curStyle);
            String preview = curName.isBlank() ? "Sword" : curName;
            inv.setItem(18 + i, makeItem(s.icon(),
                (sel ? "§a✔ " : "") + s.label(),
                "§7Preview: " + s.code() + preview,
                "§7Click to apply this style"));
        }

        inv.setItem(49, makeItem(Material.BARRIER, "§cBack to Menu"));
        return inv;
    }

    // ── Click handler ────────────────────────────────────────────────

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String title = event.getView().getTitle();
        boolean isOurs = title.startsWith(TITLE_MAIN)
            || title.equals(TITLE_HELMET) || title.equals(TITLE_CHESTPLATE)
            || title.equals(TITLE_LEGGINGS) || title.equals(TITLE_BOOTS)
            || title.equals(TITLE_SHIELD)   || title.equals(TITLE_CHAT)
            || title.equals(TITLE_SWORD);
        if (!isOurs) return;

        event.setCancelled(true);

        if (event.getClickedInventory() == null) return;
        if (!event.getClickedInventory().equals(event.getView().getTopInventory())) return;

        UUID uuid = player.getUniqueId();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        if (title.startsWith(TITLE_MAIN)) {
            handleMain(player, uuid, clicked);
        } else if (title.equals(TITLE_HELMET)) {
            handleArmorPage(player, uuid, "helmet", clicked, event.getSlot());
        } else if (title.equals(TITLE_CHESTPLATE)) {
            handleArmorPage(player, uuid, "chestplate", clicked, event.getSlot());
        } else if (title.equals(TITLE_LEGGINGS)) {
            handleArmorPage(player, uuid, "leggings", clicked, event.getSlot());
        } else if (title.equals(TITLE_BOOTS)) {
            handleArmorPage(player, uuid, "boots", clicked, event.getSlot());
        } else if (title.equals(TITLE_SHIELD)) {
            handleShieldPage(player, uuid, clicked, event.getSlot());
        } else if (title.equals(TITLE_CHAT)) {
            handleChatColorPage(player, uuid, clicked);
        } else if (title.equals(TITLE_SWORD)) {
            handleSwordNamePage(player, uuid, clicked);
        }
    }

    // ── Handlers ─────────────────────────────────────────────────────

    private void handleMain(Player player, UUID uuid, ItemStack clicked) {
        String name = stripColor(displayName(clicked));
        switch (name) {
            case "Helmet Trim"     -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_HELMET);     }
            case "Chestplate Trim" -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_CHESTPLATE); }
            case "Leggings Trim"   -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_LEGGINGS);   }
            case "Boots Trim"      -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_BOOTS);      }
            case "Shield Design"   -> { patternPage.put(uuid,0); materialPage.put(uuid,0); shieldPatColorPage.put(uuid,0); openPage(player, PAGE_SHIELD); }
            case "Chat Color"      -> openPage(player, PAGE_CHAT_COLOR);
            case "Sword Name"      -> openPage(player, PAGE_SWORD_NAME);
            case "SAVE"            -> handleSave(player, uuid);
        }
    }

    private void handleArmorPage(Player player, UUID uuid, String slot, ItemStack clicked, int slotIndex) {
        String name = stripColor(displayName(clicked));

        if (name.equals("Back to Menu")) {
            patternPage.remove(uuid); materialPage.remove(uuid);
            openPage(player, PAGE_MAIN); return;
        }
        if (name.equals("← Prev Patterns")) { patternPage.merge(uuid, -1, Integer::sum); openPage(player, pageForSlot(slot)); return; }
        if (name.equals("Next Patterns →"))  { patternPage.merge(uuid,  1, Integer::sum); openPage(player, pageForSlot(slot)); return; }
        if (name.equals("← Prev Materials")) { materialPage.merge(uuid, -1, Integer::sum); openPage(player, pageForSlot(slot)); return; }
        if (name.equals("Next Materials →"))  { materialPage.merge(uuid,  1, Integer::sum); openPage(player, pageForSlot(slot)); return; }

        // ── FIX 1: Pattern selection — use pPage offset in index calculation ──
        if (slotIndex >= 9 && slotIndex <= 15) {
            int pPage = patternPage.getOrDefault(uuid, 0);
            int idx   = pPage * 7 + (slotIndex - 9);   // <-- was missing pPage * 7
            if (idx < ALL_PATTERNS.size()) {
                TrimPattern pat = ALL_PATTERNS.get(idx);
                TrimManager.TrimChoice cur = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimMaterial mat = cur != null ? cur.material()
                    : Registry.TRIM_MATERIAL.get(NamespacedKey.minecraft("gold"));
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, pageForSlot(slot));
            }
            return;
        }

        // ── FIX 1: Material selection — use mPage offset in index calculation ──
        if (slotIndex >= 27 && slotIndex <= 33) {
            int mPage = materialPage.getOrDefault(uuid, 0);
            int idx   = mPage * 7 + (slotIndex - 27);  // <-- was missing mPage * 7
            if (idx < ALL_MATERIALS.size()) {
                TrimMaterial mat = ALL_MATERIALS.get(idx);
                TrimManager.TrimChoice cur = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimPattern pat = cur != null ? cur.pattern()
                    : Registry.TRIM_PATTERN.get(NamespacedKey.minecraft("sentry"));
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, pageForSlot(slot));
            }
        }
    }

    // New field needed for shield right-column paging — add near the top of the class:
    private final Map<UUID, Integer> shieldPatColorPage = new HashMap<>();

    private void handleShieldPage(Player player, UUID uuid, ItemStack clicked, int slotIndex) {
        String name = stripColor(displayName(clicked));
        if (name.equals("Back to Menu")) {
            patternPage.remove(uuid); materialPage.remove(uuid); shieldPatColorPage.remove(uuid);
            openPage(player, PAGE_MAIN); return;
        }

        TrimManager.ShieldDesign cur = draftShield.get(uuid);
        DyeColor    baseColor = cur != null ? cur.baseColor()    : DyeColor.WHITE;
        PatternType patType   = cur != null ? cur.patternType()  : ALL_BANNER_PATTERNS.get(0);
        DyeColor    patColor  = cur != null ? cur.patternColor() : DyeColor.BLACK;
        DyeColor[]  dyes      = DyeColor.values();

        int bannerPatPage  = patternPage.getOrDefault(uuid, 0);
        int baseColorPage  = materialPage.getOrDefault(uuid, 0);
        int patColorPage   = shieldPatColorPage.getOrDefault(uuid, 0);

        // Left column: base color swatches (col 0 = slots 9, 18, 27, 36)
        if (slotIndex == 9 || slotIndex == 18 || slotIndex == 27 || slotIndex == 36) {
            int row = (slotIndex / 9) - 1; // row 1-4 → index 0-3
            int idx = baseColorPage * 4 + row;
            if (idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, patColor, dyes[idx]));
                openPage(player, PAGE_SHIELD);
            }
            return;
        }
        // Left column scroll (slot 45)
        if (slotIndex == 45) {
            if (name.contains("↑")) materialPage.merge(uuid, -1, Integer::sum);
            else                    materialPage.merge(uuid,  1, Integer::sum);
            openPage(player, PAGE_SHIELD);
            return;
        }

        // Center area pattern nav
        if (name.equals("← Prev Patterns")) { patternPage.merge(uuid, -1, Integer::sum); openPage(player, PAGE_SHIELD); return; }
        if (name.equals("Next Patterns →"))  { patternPage.merge(uuid,  1, Integer::sum); openPage(player, PAGE_SHIELD); return; }

        // Center pattern slots: rows 1-4, cols 2-6
        int[] centerSlots = {11,12,13,14,15, 20,21,22,23,24, 29,30,31,32,33, 38,39,40,41,42};
        for (int i = 0; i < centerSlots.length; i++) {
            if (slotIndex == centerSlots[i]) {
                int idx = bannerPatPage * centerSlots.length + i;
                if (idx < ALL_BANNER_PATTERNS.size()) {
                    draftShield.put(uuid, new TrimManager.ShieldDesign(ALL_BANNER_PATTERNS.get(idx), patColor, baseColor));
                    openPage(player, PAGE_SHIELD);
                }
                return;
            }
        }

        // Right column: pattern color swatches (col 8 = slots 17, 26, 35, 44)
        if (slotIndex == 17 || slotIndex == 26 || slotIndex == 35 || slotIndex == 44) {
            int row = (slotIndex - 17) / 9; // 0-3
            int idx = patColorPage * 4 + row;
            if (idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, dyes[idx], baseColor));
                openPage(player, PAGE_SHIELD);
            }
            return;
        }
        // Right column scroll (slot 53)
        if (slotIndex == 53) {
            if (name.contains("↑")) shieldPatColorPage.merge(uuid, -1, Integer::sum);
            else                    shieldPatColorPage.merge(uuid,  1, Integer::sum);
            openPage(player, PAGE_SHIELD);
        }
    }

    private void handleChatColorPage(Player player, UUID uuid, ItemStack clicked) {
        String name = stripColor(displayName(clicked));
        if (name.equals("Back to Menu")) { openPage(player, PAGE_MAIN); return; }
        for (ColorOption opt : CHAT_COLORS) {
            if (opt.label().equals(name)) {
                draftChatColor.put(uuid, opt.code());
                player.sendMessage("§8[§6FFA§8] §7Chat color preview: " + opt.code() + "This is what your messages will look like!");
                openPage(player, PAGE_CHAT_COLOR);
                return;
            }
        }
    }

    // ── FIX 3: Sword name handler uses SWORD_STYLES ───────────────────
    private void handleSwordNamePage(Player player, UUID uuid, ItemStack clicked) {
        String name = stripColor(displayName(clicked));
        switch (name) {
            case "Back to Menu" -> { openPage(player, PAGE_MAIN); return; }
            case "Clear Name"   -> { draftSwordName.put(uuid, ""); openPage(player, PAGE_SWORD_NAME); return; }
            case "Set Name"     -> {
                player.closeInventory();
                awaitingName.add(uuid);
                player.sendMessage("§8[§6FFA§8] §eType your sword name in chat §7(max 20 chars). Type §ccancel §7to abort.");
                return;
            }
        }
        // Style selection — strip the checkmark and match label
        for (SwordStyle s : SWORD_STYLES) {
            String cleanLabel = stripColor(s.label());
            if (cleanLabel.equals(name)) {
                draftSwordColor.put(uuid, s.code());
                openPage(player, PAGE_SWORD_NAME);
                return;
            }
        }
    }

    // ── Chat input for sword name ─────────────────────────────────────

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();
        if (!awaitingName.contains(uuid)) return;
        event.setCancelled(true);
        awaitingName.remove(uuid);
        Player player = event.getPlayer();
        String input  = event.getMessage();
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

    // ── Save ──────────────────────────────────────────────────────────

    private void handleSave(Player player, UUID uuid) {
        if (plugin.getRankManager().getSavesLeft(uuid) <= 0) {
            player.sendMessage("§8[§6FFA§8] §cNo save charges left!");
            return;
        }
        if (!plugin.getRankManager().consumeSave(uuid)) return;

        Map<String, TrimManager.TrimChoice> trims = draftTrims.getOrDefault(uuid, new HashMap<>());
        for (Map.Entry<String, TrimManager.TrimChoice> e : trims.entrySet())
            plugin.getTrimManager().setTrimChoice(uuid, e.getKey(), e.getValue().pattern(), e.getValue().material());

        TrimManager.ShieldDesign shield = draftShield.get(uuid);
        if (shield != null)
            plugin.getTrimManager().setShieldDesign(uuid, shield.patternType(), shield.patternColor(), shield.baseColor());

        String cc = draftChatColor.get(uuid);
        if (cc != null) plugin.getRankManager().setChatColor(uuid, cc);

        plugin.getRankManager().setSwordName(uuid,
            draftSwordName.getOrDefault(uuid, ""),
            draftSwordColor.getOrDefault(uuid, "§f"));

        plugin.getTrimManager().applyTrims(player);
        plugin.getKitManager().applyCustomSwordName(player);
        plugin.getRankManager().save();

        player.sendMessage("§8[§6FFA§8] §a§lSaved! §7Saves remaining: §e"
            + plugin.getRankManager().getSavesLeft(uuid));
        openPage(player, PAGE_MAIN);
    }

    // ── Close cleanup ─────────────────────────────────────────────────

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        pageMap.remove(uuid);
        patternPage.remove(uuid);
        materialPage.remove(uuid);
        shieldPatColorPage.remove(uuid);
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private int pageForSlot(String slot) {
        return switch (slot) {
            case "helmet"     -> PAGE_HELMET;
            case "chestplate" -> PAGE_CHESTPLATE;
            case "leggings"   -> PAGE_LEGGINGS;
            default           -> PAGE_BOOTS;
        };
    }

    private String displayName(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        ItemMeta meta = item.getItemMeta();
        return meta.hasDisplayName() ? meta.getDisplayName() : "";
    }

    private String stripColor(String s) {
        if (s == null) return "";
        return s.replaceAll("§.", "").replace("✔ ", "").trim();
    }

    private void fill(Inventory inv, Material mat) {
        ItemStack pane = makeItem(mat, "§0");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).replace("_", " ");
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
        return switch (mat.getKey().getKey().toUpperCase()) {
            case "GOLD"      -> Material.GOLD_INGOT;
            case "IRON"      -> Material.IRON_INGOT;
            case "DIAMOND"   -> Material.DIAMOND;
            case "NETHERITE" -> Material.NETHERITE_INGOT;
            case "REDSTONE"  -> Material.REDSTONE;
            case "COPPER"    -> Material.COPPER_INGOT;
            case "AMETHYST"  -> Material.AMETHYST_SHARD;
            case "EMERALD"   -> Material.EMERALD;
            case "LAPIS"     -> Material.LAPIS_LAZULI;
            case "QUARTZ"    -> Material.QUARTZ;
            case "RESIN"     -> Material.RESIN_BRICK;
            default          -> Material.RESIN_BRICK;
        };
    }
}
