package com.ffa.managers;

import com.ffa.FFAPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final FFAPlugin plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();

    public ScoreboardManager(FFAPlugin plugin) { this.plugin = plugin; }

    public void startUpdater() {
        int interval = plugin.getConfig().getInt("scoreboard-update-interval", 20);
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) updatePlayer(p);
        }, interval, interval);
    }

    public void updatePlayer(Player player) {
        if (player == null) return;
        Scoreboard board = boards.computeIfAbsent(player.getUniqueId(),
            k -> Bukkit.getScoreboardManager().getNewScoreboard());

        Objective obj = board.getObjective("ffa");
        if (obj != null) obj.unregister();
        obj = board.registerNewObjective("ffa", Criteria.DUMMY, "§6§l✦ TIERSTERMC");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        int kills = plugin.getTierManager().getTierKills(player.getUniqueId());
        int needed = plugin.getTierManager().getKillsNeeded(tier);
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        int line = 8;
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬", line--);
        setLine(obj, "§7Tier: " + tierDisplay, line--);
        if (tier < TierManager.MAX_TIER) {
            setLine(obj, "§7Progress: §e" + kills + "§7/§e" + needed, line--);
        } else {
            setLine(obj, "§6§l★ MAX TIER ★", line--);
        }
        setLine(obj, "§8▬▬▬▬▬▬▬▬▬▬▬▬", line--);
        setLine(obj, "§btierstermc.ungsp.foo", line--);

        player.setScoreboard(board);
    }

    public void updateNameTag(Player player) {
        if (player == null) return;
        int tier = plugin.getTierManager().getTier(player.getUniqueId());
        String tierDisplay = plugin.getTierManager().getTierDisplay(tier);

        for (Player p : Bukkit.getOnlinePlayers()) {
            Scoreboard board = p.getScoreboard();
            if (board == null) continue;
            String teamName = "ffa_" + player.getName().substring(0, Math.min(12, player.getName().length()));
            Team team = board.getTeam(teamName);
            if (team == null) team = board.registerNewTeam(teamName);
            team.setPrefix(tierDisplay + " ");
            team.addEntry(player.getName());
        }

        player.setPlayerListName(tierDisplay + " §f" + player.getName());
    }

    private void setLine(Objective obj, String text, int score) {
        obj.getScore(text).setScore(score);
    }
}
