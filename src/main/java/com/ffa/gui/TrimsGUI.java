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

    private final Map<UUID, Integer> pageMap           = new HashMap<>();
    private final Map<UUID, Integer> patternPage       = new HashMap<>();
    private final Map<UUID, Integer> materialPage      = new HashMap<>();
    private final Map<UUID, Integer> shieldPatColorPage = new HashMap<>();
    private final Set<UUID>          awaitingName      = new HashSet<>();

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

    private static final String TITLE_MAIN       = "§d§lDonor Customisation";
    private static final String TITLE_HELMET     = "§bHelmet Trim";
    private static final String TITLE_CHESTPLATE = "§bChestplate Trim";
    private static final String TITLE_LEGGINGS   = "§bLeggings Trim";
    private static final String TITLE_BOOTS      = "§bBoots Trim";
    private static final String TITLE_SHIELD     = "§dShield Design";
    private static final String TITLE_CHAT       = "§eChat Color";
    private static final String TITLE_SWORD      = "§6Sword Name";

    // Slot tags — stored in item lore so we don't rely on display name matching
    private static final String TAG_PREFIX = "§0§0TAG:";

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

    private static final List<SwordStyle> SWORD_STYLES = List.of(
        new SwordStyle("§a§lBold Green",         "§a§l", Material.LIME_WOOL),
        new SwordStyle("§2§lBold Dark Green",    "§2§l", Material.GREEN_WOOL),
        new SwordStyle("§b§lBold Aqua",          "§b§l", Material.CYAN_WOOL),
        new SwordStyle("§3§lBold Dark Aqua",     "§3§l", Material.LIGHT_BLUE_WOOL),
        new SwordStyle("§9§lBold Blue",          "§9§l", Material.BLUE_WOOL),
        new SwordStyle("§d§lBold Pink",          "§d§l", Material.MAGENTA_WOOL),
        new SwordStyle("§5§lBold Purple",        "§5§l", Material.PURPLE_WOOL),
        new SwordStyle("§6§lBold Gold",          "§6§l", Material.GOLD_INGOT),
        new SwordStyle("§e§lBold Yellow",        "§e§l", Material.YELLOW_WOOL),
        new SwordStyle("§c§lBold Red",           "§c§l", Material.RED_WOOL),
        new SwordStyle("§4§lBold Dark Red",      "§4§l", Material.BROWN_WOOL),
        new SwordStyle("§f§lBold White",         "§f§l", Material.WHITE_WOOL),
        new SwordStyle("§b§oItalic Aqua",        "§b§o", Material.PRISMARINE_SHARD),
        new SwordStyle("§6§oItalic Gold",        "§6§o", Material.GOLD_NUGGET),
        new SwordStyle("§d§oItalic Pink",        "§d§o", Material.PINK_PETALS),
        new SwordStyle("§c§oItalic Red",         "§c§o", Material.POPPY),
        new SwordStyle("§a§l§oGreen Bold+Italic","§a§l§o", Material.FERN),
        new SwordStyle("§6§l§oGold Bold+Italic", "§6§l§o", Material.BLAZE_POWDER),
        new SwordStyle("§f§kObfuscated",         "§f§k", Material.ENDER_EYE),
        new SwordStyle("§c§l§kRed Obfuscated",   "§c§l§k", Material.NETHER_STAR)
    );

    private record ColorOption(String label, String code, Material icon) {}
    private record SwordStyle(String label, String code, Material icon) {}

    // Tags for action identification — avoids all display-name string matching issues
    private static final String TAG_BACK         = "BACK";
    private static final String TAG_SAVE         = "SAVE";
    private static final String TAG_PAT_PREV     = "PAT_PREV";
    private static final String TAG_PAT_NEXT     = "PAT_NEXT";
    private static final String TAG_MAT_PREV     = "MAT_PREV";
    private static final String TAG_MAT_NEXT     = "MAT_NEXT";
    private static final String TAG_BASE_PREV    = "BASE_PREV";
    private static final String TAG_BASE_NEXT    = "BASE_NEXT";
    private static final String TAG_PATCOL_SCROLL= "PATCOL_SCROLL";
    private static final String TAG_SET_NAME     = "SET_NAME";
    private static final String TAG_CLEAR_NAME   = "CLEAR_NAME";
    private static final String TAG_PAT          = "PAT:";   // PAT:index
    private static final String TAG_MAT          = "MAT:";   // MAT:index
    private static final String TAG_BASE         = "BASE:";  // BASE:dyeOrdinal
    private static final String TAG_BPAT         = "BPAT:";  // BPAT:index
    private static final String TAG_PATCOL       = "PATCOL:";// PATCOL:dyeOrdinal
    private static final String TAG_CHATCOL      = "CC:";    // CC:code
    private static final String TAG_STYLE        = "STYLE:"; // STYLE:index

    public TrimsGUI(FFAPlugin plugin) { this.plugin = plugin; }

    // ── Open ────────────────────────────────────────────────────────

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

    private Inventory buildMainMenu(Player player) {
        UUID uuid  = player.getUniqueId();
        int  saves = plugin.getRankManager().getSavesLeft(uuid);
        Inventory inv = Bukkit.createInventory(null, 54, TITLE_MAIN + " §8| §7" + saves + " save(s) left");
        fill(inv, Material.PURPLE_STAINED_GLASS_PANE);

        Map<String, TrimManager.TrimChoice> trims = draftTrims.getOrDefault(uuid, new HashMap<>());

        inv.setItem(10, makeTrimmedArmorItem(Material.DIAMOND_HELMET,     trims.get("helmet"),     "§bHelmet Trim",     TAG_PREFIX + "HELMET"));
        inv.setItem(19, makeTrimmedArmorItem(Material.DIAMOND_CHESTPLATE, trims.get("chestplate"), "§bChestplate Trim", TAG_PREFIX + "CHESTPLATE"));
        inv.setItem(28, makeTrimmedArmorItem(Material.DIAMOND_LEGGINGS,   trims.get("leggings"),   "§bLeggings Trim",   TAG_PREFIX + "LEGGINGS"));
        inv.setItem(37, makeTrimmedArmorItem(Material.DIAMOND_BOOTS,      trims.get("boots"),      "§bBoots Trim",      TAG_PREFIX + "BOOTS"));
        inv.setItem(13, makeTagged(Material.SHIELD, "§dShield Design", TAG_PREFIX + "SHIELD", "§7Click to customise"));

        String cc = draftChatColor.getOrDefault(uuid, "§7");
        inv.setItem(22, makeTagged(Material.NAME_TAG, "§eChat Color", TAG_PREFIX + "CHAT",
            "§7Current: " + cc + "Sample text§r", "§7Click to change"));

        String sn = draftSwordName.getOrDefault(uuid, "");
        String sc = draftSwordColor.getOrDefault(uuid, "§f");
        inv.setItem(31, makeTagged(Material.DIAMOND_SWORD, "§6Sword Name", TAG_PREFIX + "SWORD",
            "§7Current: " + sc + (sn.isBlank() ? "None" : sn), "§7Click to change"));

        if (saves > 0) {
            inv.setItem(49, makeTagged(Material.EMERALD, "§a§lSAVE", TAG_PREFIX + TAG_SAVE,
                "§7Applies all pending changes.",
                "§7Saves remaining: §e" + saves,
                "§cUses 1 save charge!"));
        } else {
            inv.setItem(49, makeItem(Material.BARRIER, "§c§lNO SAVES LEFT",
                "§7Purchase the rank again to get more."));
        }
        return inv;
    }

    private ItemStack makeTrimmedArmorItem(Material mat, TrimManager.TrimChoice choice, String title, String tag) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        if (meta instanceof ArmorMeta am && choice != null) {
            am.setTrim(new ArmorTrim(choice.material(), choice.pattern()));
        }
        meta.setDisplayName(title);
        List<String> lore = new ArrayList<>();
        if (choice != null) {
            lore.add("§7Pattern:  §e" + capitalize(choice.pattern().getKey().getKey()));
            lore.add("§7Material: §e" + capitalize(choice.material().getKey().getKey()));
            lore.add("§7Click to customise");
        } else {
            lore.add("§7No trim selected");
            lore.add("§7Click to customise");
        }
        lore.add(tag);
        meta.setLore(lore);
        item.setItemMeta(meta);
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

        // Preview at slot 4
        ItemStack previewItem = new ItemStack(icon);
        ItemMeta previewMeta = previewItem.getItemMeta();
        if (previewMeta != null) {
            if (previewMeta instanceof ArmorMeta am && current != null) {
                am.setTrim(new ArmorTrim(current.material(), current.pattern()));
            }
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
        int patStart = pPage * 7;
        // Use gold as fallback preview material
        TrimMaterial fallbackMat = findFirstMaterial();
        for (int i = 0; i < 7 && (patStart + i) < ALL_PATTERNS.size(); i++) {
            int globalIdx    = patStart + i;
            TrimPattern pat  = ALL_PATTERNS.get(globalIdx);
            boolean sel      = current != null && current.pattern().equals(pat);
            ItemStack pi     = new ItemStack(icon);
            ItemMeta  meta   = pi.getItemMeta();
            if (meta == null) continue;
            if (meta instanceof ArmorMeta am) {
                TrimMaterial previewMat = (current != null) ? current.material() : fallbackMat;
                if (previewMat != null) am.setTrim(new ArmorTrim(previewMat, pat));
            }
            meta.setDisplayName((sel ? "§a✔ " : "§7") + capitalize(pat.getKey().getKey()));
            meta.setLore(List.of("§7Click to select this pattern", TAG_PREFIX + TAG_PAT + globalIdx));
            pi.setItemMeta(meta);
            inv.setItem(9 + i, pi);
        }
        // Pattern nav: slot 16 = prev, slot 17 = next
        if (pPage > 0)
            inv.setItem(16, makeTagged(Material.ARROW, "§7← Prev Patterns", TAG_PREFIX + TAG_PAT_PREV,
                "§8Page " + pPage + " / " + ((ALL_PATTERNS.size() - 1) / 7)));
        if (patStart + 7 < ALL_PATTERNS.size())
            inv.setItem(17, makeTagged(Material.ARROW, "§7Next Patterns →", TAG_PREFIX + TAG_PAT_NEXT,
                "§8Page " + (pPage + 2) + " / " + ((ALL_PATTERNS.size() - 1) / 7 + 1)));

        // ── Materials: slots 27–33 (7 per page) ──
        int matStart = mPage * 7;
        for (int i = 0; i < 7 && (matStart + i) < ALL_MATERIALS.size(); i++) {
            int globalIdx      = matStart + i;
            TrimMaterial mat   = ALL_MATERIALS.get(globalIdx);
            boolean sel        = current != null && current.material().equals(mat);
            ItemStack mi       = makeTagged(trimMaterialIcon(mat),
                (sel ? "§a✔ " : "§e") + capitalize(mat.getKey().getKey()),
                TAG_PREFIX + TAG_MAT + globalIdx,
                "§7Click to select this material");
            inv.setItem(27 + i, mi);
        }
        // Material nav: slot 34 = prev, slot 35 = next
        if (mPage > 0)
            inv.setItem(34, makeTagged(Material.ARROW, "§7← Prev Materials", TAG_PREFIX + TAG_MAT_PREV,
                "§8Page " + mPage + " / " + ((ALL_MATERIALS.size() - 1) / 7)));
        if (matStart + 7 < ALL_MATERIALS.size())
            inv.setItem(35, makeTagged(Material.ARROW, "§7Next Materials →", TAG_PREFIX + TAG_MAT_NEXT,
                "§8Page " + (mPage + 2) + " / " + ((ALL_MATERIALS.size() - 1) / 7 + 1)));

        inv.setItem(45, makeTagged(Material.BARRIER, "§cBack to Menu", TAG_PREFIX + TAG_BACK));
        return inv;
    }

    private Inventory buildShieldPage(Player player) {
        UUID uuid = player.getUniqueId();
        TrimManager.ShieldDesign draft = draftShield.get(uuid);

        int bannerPatPage = patternPage.getOrDefault(uuid, 0);
        int baseColorPage = materialPage.getOrDefault(uuid, 0);
        int patColorPage  = shieldPatColorPage.getOrDefault(uuid, 0);

        Inventory inv = Bukkit.createInventory(null, 54, TITLE_SHIELD);
        fill(inv, Material.GRAY_STAINED_GLASS_PANE);

        int[] dividerSlots = {1,10,19,28,37,46, 7,16,25,34,43,52};
        for (int s : dividerSlots) inv.setItem(s, makeItem(Material.MAGENTA_STAINED_GLASS_PANE, "§0"));

        DyeColor    baseColor = draft != null ? draft.baseColor()    : DyeColor.WHITE;
        PatternType patType   = draft != null ? draft.patternType()  : ALL_BANNER_PATTERNS.get(0);
        DyeColor    patColor  = draft != null ? draft.patternColor() : DyeColor.BLACK;
        DyeColor[]  dyes      = DyeColor.values();

        // Header row
        inv.setItem(0, makeItem(dyeToWool(baseColor), "§fBase Color",
            "§7Selected: §e" + baseColor.name(), "§7← Scroll ↓"));
        inv.setItem(4, makeShieldPreview(patType, patColor, baseColor));
        inv.setItem(8, makeItem(dyeToWool(patColor), "§fPattern Color",
            "§7Selected: §e" + patColor.name(), "§7Scroll ↓ →"));

        // Left column: base colors (slots 9,18,27,36)
        int baseStart = baseColorPage * 4;
        for (int i = 0; i < 4 && (baseStart + i) < dyes.length; i++) {
            DyeColor dc = dyes[baseStart + i];
            boolean sel = dc == baseColor;
            inv.setItem(9 + i * 9, makeTagged(dyeToWool(dc),
                (sel ? "§a✔ " : "§7") + dc.name(),
                TAG_PREFIX + TAG_BASE + (baseStart + i),
                "§7Click to set base color"));
        }
        // Base scroll at slot 45 — also used for back, so only show scroll if needed
        boolean hasMoreBase = baseStart + 4 < dyes.length || baseColorPage > 0;
        if (hasMoreBase) {
            String scrollLabel = baseColorPage > 0 ? "§7↑ Prev Colors" : "§7↓ More Colors";
            String scrollTag   = baseColorPage > 0 ? TAG_PREFIX + TAG_BASE_PREV : TAG_PREFIX + TAG_BASE_NEXT;
            inv.setItem(46, makeTagged(Material.ARROW, scrollLabel, scrollTag));
        }

        // Center pattern grid: rows 1-4, cols 2-6 → slots 11-15,20-24,29-33,38-42
        int[] centerSlots = {11,12,13,14,15, 20,21,22,23,24, 29,30,31,32,33, 38,39,40,41,42};
        int patStart = bannerPatPage * centerSlots.length;
        for (int i = 0; i < centerSlots.length && (patStart + i) < ALL_BANNER_PATTERNS.size(); i++) {
            int globalIdx  = patStart + i;
            PatternType pt = ALL_BANNER_PATTERNS.get(globalIdx);
            boolean sel    = pt.equals(patType);
            inv.setItem(centerSlots[i], makeTagged(
                sel ? Material.FILLED_MAP : Material.MAP,
                (sel ? "§a✔ " : "§d") + formatBannerPatternName(pt),
                TAG_PREFIX + TAG_BPAT + globalIdx,
                "§7Click to select pattern"));
        }
        // Pattern nav row (row 5, center): slots 47,49,51
        if (bannerPatPage > 0)
            inv.setItem(47, makeTagged(Material.ARROW, "§7← Prev Patterns", TAG_PREFIX + TAG_PAT_PREV));
        inv.setItem(49, makeItem(Material.SHIELD, "§dCurrent Design",
            "§7Base: §f" + baseColor.name(),
            "§7Pattern: §f" + formatBannerPatternName(patType),
            "§7Color: §f" + patColor.name()));
        if (patStart + centerSlots.length < ALL_BANNER_PATTERNS.size())
            inv.setItem(51, makeTagged(Material.ARROW, "§7Next Patterns →", TAG_PREFIX + TAG_PAT_NEXT));

        // Back at slot 45
        inv.setItem(45, makeTagged(Material.BARRIER, "§cBack to Menu", TAG_PREFIX + TAG_BACK));

        // Right column: pattern colors (slots 17,26,35,44)
        int patColorStart = patColorPage * 4;
        for (int i = 0; i < 4 && (patColorStart + i) < dyes.length; i++) {
            DyeColor dc = dyes[patColorStart + i];
            boolean sel = dc == patColor;
            inv.setItem(17 + i * 9, makeTagged(dyeToWool(dc),
                (sel ? "§a✔ " : "§7") + dc.name(),
                TAG_PREFIX + TAG_PATCOL + (patColorStart + i),
                "§7Click to set pattern color"));
        }
        // Pat color scroll at slot 53
        if (patColorStart + 4 < dyes.length || patColorPage > 0) {
            String dir = patColorStart + 4 < dyes.length ? "§7↓ More" : "§7↑ Back";
            inv.setItem(53, makeTagged(Material.ARROW, dir, TAG_PREFIX + TAG_PATCOL_SCROLL));
        }

        return inv;
    }

    private ItemStack makeShieldPreview(PatternType patType, DyeColor patColor, DyeColor baseColor) {
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
            inv.setItem(10 + i, makeTagged(opt.icon(),
                (sel ? "§a✔ " : opt.code()) + opt.label(),
                TAG_PREFIX + TAG_CHATCOL + i,
                "§7Click to select this chat color"));
        }
        inv.setItem(31, makeTagged(Material.BARRIER, "§cBack to Menu", TAG_PREFIX + TAG_BACK));
        return inv;
    }

    private Inventory buildSwordNamePage(Player player) {
        UUID uuid       = player.getUniqueId();
        String curName  = draftSwordName.getOrDefault(uuid, "");
        String curStyle = draftSwordColor.getOrDefault(uuid, "§f");
        Inventory inv   = Bukkit.createInventory(null, 54, TITLE_SWORD);
        fill(inv, Material.ORANGE_STAINED_GLASS_PANE);

        inv.setItem(4, makeItem(Material.DIAMOND_SWORD, "§6Current Sword Name",
            "§7Name:  " + curStyle + (curName.isBlank() ? "None" : curName),
            "§7Style: §7" + curStyle + "preview§r",
            "§7Max 20 characters"));
        inv.setItem(11, makeTagged(Material.WRITABLE_BOOK, "§eSet Name", TAG_PREFIX + TAG_SET_NAME,
            "§7Click, then type your sword name in chat.",
            "§7Type §ccancel §7to abort."));
        inv.setItem(15, makeTagged(Material.BARRIER, "§cClear Name", TAG_PREFIX + TAG_CLEAR_NAME,
            "§7Removes the custom sword name."));

        for (int i = 0; i < SWORD_STYLES.size(); i++) {
            SwordStyle s   = SWORD_STYLES.get(i);
            boolean sel    = s.code().equals(curStyle);
            String preview = curName.isBlank() ? "Sword" : curName;
            inv.setItem(18 + i, makeTagged(s.icon(),
                (sel ? "§a✔ " : "") + s.label(),
                TAG_PREFIX + TAG_STYLE + i,
                "§7Preview: " + s.code() + preview,
                "§7Click to apply this style"));
        }

        inv.setItem(49, makeTagged(Material.BARRIER, "§cBack to Menu", TAG_PREFIX + TAG_BACK));
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

        String tag = getTag(clicked);
        if (tag == null) return;

        if (title.startsWith(TITLE_MAIN)) {
            handleMain(player, uuid, tag);
        } else if (title.equals(TITLE_HELMET)) {
            handleArmorPage(player, uuid, "helmet", tag);
        } else if (title.equals(TITLE_CHESTPLATE)) {
            handleArmorPage(player, uuid, "chestplate", tag);
        } else if (title.equals(TITLE_LEGGINGS)) {
            handleArmorPage(player, uuid, "leggings", tag);
        } else if (title.equals(TITLE_BOOTS)) {
            handleArmorPage(player, uuid, "boots", tag);
        } else if (title.equals(TITLE_SHIELD)) {
            handleShieldPage(player, uuid, tag);
        } else if (title.equals(TITLE_CHAT)) {
            handleChatColorPage(player, uuid, tag);
        } else if (title.equals(TITLE_SWORD)) {
            handleSwordNamePage(player, uuid, tag);
        }
    }

    // ── Tag extraction ────────────────────────────────────────────────

    /** Returns the tag value (everything after TAG_PREFIX) or null if none found. */
    private String getTag(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasLore()) return null;
        for (String line : meta.getLore()) {
            if (line != null && line.startsWith(TAG_PREFIX)) {
                return line.substring(TAG_PREFIX.length());
            }
        }
        return null;
    }

    // ── Handlers ─────────────────────────────────────────────────────

    private void handleMain(Player player, UUID uuid, String tag) {
        switch (tag) {
            case "HELMET"     -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_HELMET);     }
            case "CHESTPLATE" -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_CHESTPLATE); }
            case "LEGGINGS"   -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_LEGGINGS);   }
            case "BOOTS"      -> { patternPage.put(uuid,0); materialPage.put(uuid,0); openPage(player, PAGE_BOOTS);      }
            case "SHIELD"     -> { patternPage.put(uuid,0); materialPage.put(uuid,0); shieldPatColorPage.put(uuid,0); openPage(player, PAGE_SHIELD); }
            case "CHAT"       -> openPage(player, PAGE_CHAT_COLOR);
            case "SWORD"      -> openPage(player, PAGE_SWORD_NAME);
            case TAG_SAVE     -> handleSave(player, uuid);
        }
    }

    private void handleArmorPage(Player player, UUID uuid, String slot, String tag) {
        int targetPage = pageForSlot(slot);

        switch (tag) {
            case TAG_BACK     -> { patternPage.remove(uuid); materialPage.remove(uuid); openPage(player, PAGE_MAIN); return; }
            case TAG_PAT_PREV -> { patternPage.merge(uuid, -1, Integer::sum);  openPage(player, targetPage); return; }
            case TAG_PAT_NEXT -> { patternPage.merge(uuid,  1, Integer::sum);  openPage(player, targetPage); return; }
            case TAG_MAT_PREV -> { materialPage.merge(uuid, -1, Integer::sum); openPage(player, targetPage); return; }
            case TAG_MAT_NEXT -> { materialPage.merge(uuid,  1, Integer::sum); openPage(player, targetPage); return; }
        }

        if (tag.startsWith(TAG_PAT)) {
            int idx = parseIndex(tag, TAG_PAT);
            if (idx >= 0 && idx < ALL_PATTERNS.size()) {
                TrimPattern pat = ALL_PATTERNS.get(idx);
                TrimManager.TrimChoice cur = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimMaterial mat = (cur != null) ? cur.material() : findFirstMaterial();
                if (mat == null) mat = findFirstMaterial();
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, targetPage);
            }
            return;
        }

        if (tag.startsWith(TAG_MAT)) {
            int idx = parseIndex(tag, TAG_MAT);
            if (idx >= 0 && idx < ALL_MATERIALS.size()) {
                TrimMaterial mat = ALL_MATERIALS.get(idx);
                TrimManager.TrimChoice cur = draftTrims.getOrDefault(uuid, new HashMap<>()).get(slot);
                TrimPattern pat = (cur != null) ? cur.pattern() : findFirstPattern();
                if (pat == null) pat = findFirstPattern();
                draftTrims.computeIfAbsent(uuid, k -> new HashMap<>())
                          .put(slot, new TrimManager.TrimChoice(pat, mat));
                openPage(player, targetPage);
            }
        }
    }

    private void handleShieldPage(Player player, UUID uuid, String tag) {
        TrimManager.ShieldDesign cur = draftShield.get(uuid);
        DyeColor    baseColor = cur != null ? cur.baseColor()    : DyeColor.WHITE;
        PatternType patType   = cur != null ? cur.patternType()  : ALL_BANNER_PATTERNS.get(0);
        DyeColor    patColor  = cur != null ? cur.patternColor() : DyeColor.BLACK;
        DyeColor[]  dyes      = DyeColor.values();

        switch (tag) {
            case TAG_BACK        -> { patternPage.remove(uuid); materialPage.remove(uuid); shieldPatColorPage.remove(uuid); openPage(player, PAGE_MAIN); return; }
            case TAG_PAT_PREV    -> { patternPage.merge(uuid, -1, Integer::sum);      openPage(player, PAGE_SHIELD); return; }
            case TAG_PAT_NEXT    -> { patternPage.merge(uuid,  1, Integer::sum);      openPage(player, PAGE_SHIELD); return; }
            case TAG_BASE_PREV   -> { materialPage.merge(uuid, -1, Integer::sum);     openPage(player, PAGE_SHIELD); return; }
            case TAG_BASE_NEXT   -> { materialPage.merge(uuid,  1, Integer::sum);     openPage(player, PAGE_SHIELD); return; }
            case TAG_PATCOL_SCROLL -> {
                int pcp = shieldPatColorPage.getOrDefault(uuid, 0);
                int patColorStart = pcp * 4;
                if (patColorStart + 4 < dyes.length) shieldPatColorPage.merge(uuid, 1, Integer::sum);
                else                                  shieldPatColorPage.merge(uuid, -1, Integer::sum);
                openPage(player, PAGE_SHIELD);
                return;
            }
        }

        if (tag.startsWith(TAG_BASE)) {
            int idx = parseIndex(tag, TAG_BASE);
            if (idx >= 0 && idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, patColor, dyes[idx]));
                openPage(player, PAGE_SHIELD);
            }
            return;
        }

        if (tag.startsWith(TAG_BPAT)) {
            int idx = parseIndex(tag, TAG_BPAT);
            if (idx >= 0 && idx < ALL_BANNER_PATTERNS.size()) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(ALL_BANNER_PATTERNS.get(idx), patColor, baseColor));
                openPage(player, PAGE_SHIELD);
            }
            return;
        }

        if (tag.startsWith(TAG_PATCOL)) {
            int idx = parseIndex(tag, TAG_PATCOL);
            if (idx >= 0 && idx < dyes.length) {
                draftShield.put(uuid, new TrimManager.ShieldDesign(patType, dyes[idx], baseColor));
                openPage(player, PAGE_SHIELD);
            }
        }
    }

    private void handleChatColorPage(Player player, UUID uuid, String tag) {
        if (tag.equals(TAG_BACK)) { openPage(player, PAGE_MAIN); return; }
        if (tag.startsWith(TAG_CHATCOL)) {
            int idx = parseIndex(tag, TAG_CHATCOL);
            if (idx >= 0 && idx < CHAT_COLORS.size()) {
                ColorOption opt = CHAT_COLORS.get(idx);
                draftChatColor.put(uuid, opt.code());
                player.sendMessage("§8[§6FFA§8] §7Chat color preview: " + opt.code() + "This is what your messages will look like!");
                openPage(player, PAGE_CHAT_COLOR);
            }
        }
    }

    private void handleSwordNamePage(Player player, UUID uuid, String tag) {
        if (tag.equals(TAG_BACK))       { openPage(player, PAGE_MAIN); return; }
        if (tag.equals(TAG_CLEAR_NAME)) { draftSwordName.put(uuid, ""); openPage(player, PAGE_SWORD_NAME); return; }
        if (tag.equals(TAG_SET_NAME)) {
            player.closeInventory();
            awaitingName.add(uuid);
            player.sendMessage("§8[§6FFA§8] §eType your sword name in chat §7(max 20 chars). Type §ccancel §7to abort.");
            return;
        }
        if (tag.startsWith(TAG_STYLE)) {
            int idx = parseIndex(tag, TAG_STYLE);
            if (idx >= 0 && idx < SWORD_STYLES.size()) {
                draftSwordColor.put(uuid, SWORD_STYLES.get(idx).code());
                openPage(player, PAGE_SWORD_NAME);
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

    private int parseIndex(String tag, String prefix) {
        try { return Integer.parseInt(tag.substring(prefix.length())); }
        catch (NumberFormatException e) { return -1; }
    }

    private TrimMaterial findFirstMaterial() {
        return ALL_MATERIALS.isEmpty() ? null : ALL_MATERIALS.get(0);
    }

    private TrimPattern findFirstPattern() {
        return ALL_PATTERNS.isEmpty() ? null : ALL_PATTERNS.get(0);
    }

    private void fill(Inventory inv, Material mat) {
        ItemStack pane = makeItem(mat, "§0");
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, pane);
    }

    /** Creates an item with no tag in lore. */
    private ItemStack makeItem(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        if (lore.length > 0) meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    /** Creates an item with a tag embedded in lore (invisible to players due to §0§0 prefix color). */
    private ItemStack makeTagged(Material mat, String name, String tag, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta  meta = item.getItemMeta();
        if (meta == null) return item;
        meta.setDisplayName(name);
        List<String> loreList = new ArrayList<>(Arrays.asList(lore));
        loreList.add(tag); // tag line always last
        meta.setLore(loreList);
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
        if (mat == null) return Material.BARRIER;
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
