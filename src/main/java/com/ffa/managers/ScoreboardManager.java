package com.ffa.managers;

import com.ffa.FFAPlugin;
import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager implements Listener {

    private final FFAPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private int animTick = 0;

    // Base text that gets the moving highlight. The "✦" stars stay constant.
    private static final String LOGO = "KoalaFFA Beta";

    public ScoreboardManager(FFAPlugin plugin) { this.plugin = plugin; }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Prevent unbounded growth of the per-UUID scoreboard cache.
        boards.remove(event.getPlayer().getUniqueId());
    }

    public void startUpdater() {
        int interval = plugin.getConfig().getInt("scoreboard-update-interval", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
        }, interval, interval);

        // Separate, faster loop just for the animated TAB header (every 4 ticks ≈ 5 fps).
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            animTick++;
            for (Player p : Bukkit.getOnlinePlayers()) updateTabHeaderFooter(p);
        }, 4L, 4L);
    }

    /** Sends the animated "KoalaFFA Beta" header with a /discord line, plus the online-count footer. */
    public void updateTabHeaderFooter(Player player) {
        if (player == null) return;

        String header = animatedLogo() + "\n§b/discord";
        String footer = plugin.getConfig().getString("tab.footer", "§7Players online: §e{online}")
            .replace("{online}", String.valueOf(Bukkit.getOnlinePlayers().size()))
            .replace("&", "§");

        player.sendPlayerListHeaderAndFooter(
            LegacyComponentSerializer.legacySection().deserialize(header),
            LegacyComponentSerializer.legacySection().deserialize(footer)
        );
    }

    /** Builds one animation frame: a white highlight sweeping across the purple logo. */
    private String animatedLogo() {
        int hi = animTick % LOGO.length();
        StringBuilder sb = new StringBuilder("§5§l✦ ");
        for (int i = 0; i < LOGO.length(); i++) {
            char c = LOGO.charAt(i);
            if (c == ' ') { sb.append(' '); continue; }
            sb.append(i == hi ? "§f§l" : "§5§l").append(c);
        }
        sb.append(" §5§l✦");
        return sb.toString();
    }

    public void updatePlayer(Player player) {
        if (player == null) return;
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
            k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("ffa");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("ffa", Criteria.DUMMY, "§5§l✦ KoalaFFA §7Beta");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);
        // Hide the red score numbers on the right side of the sidebar.
        obj.numberFormat(NumberFormat.blank());

        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(player.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        int ping = player.getPing();
        String pingColor = ping < 50 ? "§a" : ping < 100 ? "§e" : ping < 200 ? "§6" : "§c";

        int line = 9;
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬", line--);
        setLine(obj, "§7Tier: " + tierDisplay, line--);
        if (tier < TierManager.MAX_TIER) {
            setLine(obj, "§7Kill Progress: §e" + kills + "§7/§e" + needed, line--);
        } else {
            setLine(obj, "§4§l☠ MAX TIER ☠", line--);
        }
        setLine(obj, "§7Ping: " + pingColor + ping + "ms", line--);
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬ ", line--);
        setLine(obj, "§e/stats", line--);
        setLine(obj, "§b/discord", line--);
        setLine(obj, "§fplay.tierster.xyz", line--);

        player.setScoreboard(board);

        updateNameTag(player);
    }

    public void updateNameTag(Player player) {
        if (player == null) return;
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);
        int ping = player.getPing();
        String pingColor = ping < 50 ? "§a" : ping < 100 ? "§e" : ping < 200 ? "§6" : "§c";

        // Get LP prefix — empty string if none
        String lpPrefix = plugin.getChatManager().getLPPrefix(player);
        String prefixPart = lpPrefix.isEmpty() ? "" : lpPrefix + " ";

        // TAB format from config
        String tabFormat = plugin.getConfig().getString("tab.player-format", "{lp_prefix}{tier} §f{player} §8| {ping}ms")
            .replace("{lp_prefix}", prefixPart)
            .replace("{tier}", tierDisplay)
            .replace("{player}", player.getName())
            .replace("{ping}", pingColor + ping)
            .replace("&", "§");
        player.setPlayerListName(tabFormat);

        // Nametag above head
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            Scoreboard board = viewer.getScoreboard();
            if (board == Bukkit.getScoreboardManager().getMainScoreboard()) continue;
            String teamKey = "ffa_" + player.getName().substring(0, Math.min(12, player.getName().length()));
            Team team = board.getTeam(teamKey);
            if (team == null) team = board.registerNewTeam(teamKey);
            team.setPrefix((prefixPart + tierDisplay + " ").replace("&", "§"));
            team.addEntry(player.getName());
        }
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
