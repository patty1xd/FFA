package com.ffa;

import com.ffa.commands.*;
import com.ffa.gui.TrimsGUI;
import com.ffa.listeners.*;
import com.ffa.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class FFAPlugin extends JavaPlugin {

    private static FFAPlugin instance;

    // Existing managers
    private TierManager          tierManager;
    private NPCManager           npcManager;
    private KitManager           kitManager;
    private ScoreboardManager    scoreboardManager;
    private NormalizationManager normalizationManager;
    private ChatManager          chatManager;
    private StatsManager         statsManager;
    private SpawnManager         spawnManager;
    private RandomTeleportManager rtpManager;
    private ArenaResetManager    arenaResetManager;

    // New rank managers
    private RankManager          rankManager;
    private TrimManager          trimManager;
    private KillEffectManager    killEffectManager;
    private RankNPCManager       rankNPCManager;

    // GUI
    private TrimsGUI             trimsGUI;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        // ── Existing managers ──────────────────────────────────────────
        tierManager          = new TierManager(this);
        npcManager           = new NPCManager(this);
        kitManager           = new KitManager(this);
        scoreboardManager    = new ScoreboardManager(this);
        normalizationManager = new NormalizationManager(this);
        chatManager          = new ChatManager(this);
        statsManager         = new StatsManager(this);
        spawnManager         = new SpawnManager(this);
        rtpManager           = new RandomTeleportManager(this);
        arenaResetManager    = new ArenaResetManager(this);

        // ── New rank managers (order matters: rank before trim/kit) ────
        rankManager       = new RankManager(this);
        trimManager       = new TrimManager(this);
        killEffectManager = new KillEffectManager(this);
        rankNPCManager    = new RankNPCManager(this);
        trimsGUI          = new TrimsGUI(this);

        // ── Register listeners ─────────────────────────────────────────
        getServer().getPluginManager().registerEvents(new PlayerKillListener(this),  this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this),  this);
        getServer().getPluginManager().registerEvents(new NPCInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCProtectListener(this),  this);
        getServer().getPluginManager().registerEvents(new ArenaProtectListener(this),this);
        getServer().getPluginManager().registerEvents(normalizationManager,           this);
        getServer().getPluginManager().registerEvents(spawnManager,                   this);
        getServer().getPluginManager().registerEvents(rankNPCManager,                 this);
        getServer().getPluginManager().registerEvents(trimsGUI,                       this);

        // ── Existing commands ──────────────────────────────────────────
        getCommand("spawnnpc").setExecutor(new SpawnNPCCommand(this));
        getCommand("removenpc").setExecutor(new RemoveNPCCommand(this));
        getCommand("tier").setExecutor(new TierCommand(this));
        getCommand("settier").setExecutor(new SetTierCommand(this));
        getCommand("resetplayer").setExecutor(new ResetPlayerCommand(this));
        getCommand("koalaffa").setExecutor((sender, cmd, label, args2) -> {
            if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args2.length > 0 && args2[0].equalsIgnoreCase("reload")) {
                reloadConfig(); sender.sendMessage("§aKoalaFFA reloaded!"); return true;
            }
            sender.sendMessage("§cUsage: /koalaffa reload"); return true;
        });
        getCommand("stats").setExecutor(new StatsCommand(this));

        MsgCommand msgCmd = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCmd);
        getCommand("tell").setExecutor(msgCmd);
        ReplyCommand replyCmd = new ReplyCommand(this);
        getCommand("reply").setExecutor(replyCmd);
        getCommand("r").setExecutor(replyCmd);

        SpawnCommand spawnCmd = new SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCmd);
        getCommand("setspawn").setExecutor(spawnCmd);

        ArenaCommand arenaCmd = new ArenaCommand(this);
        getCommand("setarena1").setExecutor(arenaCmd);
        getCommand("setarena2").setExecutor(arenaCmd);
        getCommand("spawnarnanpc").setExecutor(arenaCmd);
        getCommand("removearnanpc").setExecutor(arenaCmd);
        getCommand("savearena").setExecutor(arenaCmd);
        getCommand("resetarena").setExecutor(arenaCmd);

        // ── New rank commands ──────────────────────────────────────────
        RankCommand rankCmd = new RankCommand(this);
        getCommand("grantrank").setExecutor(rankCmd);
        getCommand("revokerank").setExecutor(rankCmd);

        TrimsCommand trimsCmd = new TrimsCommand(this, trimsGUI);
        getCommand("trims").setExecutor(trimsCmd);

        SpawnRankNPCCommand rankNPCCmd = new SpawnRankNPCCommand(this);
        getCommand("spawnranknpc").setExecutor(rankNPCCmd);
        getCommand("removeranknpc").setExecutor(rankNPCCmd);

        // ── Restore NPCs ───────────────────────────────────────────────
        npcManager.restoreNPC();
        rankNPCManager.restoreNPC();
        scoreboardManager.startUpdater();

        getLogger().info("KoalaFFA enabled!");
    }

    @Override
    public void onDisable() {
        if (tierManager    != null) tierManager.saveAll();
        if (statsManager   != null) statsManager.saveAll();
        if (npcManager     != null) npcManager.removeNPC();
        if (rankNPCManager != null) rankNPCManager.removeNPC();
        if (rtpManager     != null) rtpManager.removeNPC();
        if (arenaResetManager != null) arenaResetManager.stopResetTimer();
        if (rankManager    != null) rankManager.save();
        getLogger().info("KoalaFFA disabled.");
    }

    // ── Getters ────────────────────────────────────────────────────

    public static FFAPlugin getInstance()              { return instance; }
    public TierManager getTierManager()                { return tierManager; }
    public NPCManager getNPCManager()                  { return npcManager; }
    public KitManager getKitManager()                  { return kitManager; }
    public ScoreboardManager getBoardManager()         { return scoreboardManager; }
    public NormalizationManager getNormalizationManager() { return normalizationManager; }
    public ChatManager getChatManager()                { return chatManager; }
    public StatsManager getStatsManager()              { return statsManager; }
    public SpawnManager getSpawnManager()              { return spawnManager; }
    public RandomTeleportManager getRTPManager()       { return rtpManager; }
    public ArenaResetManager getArenaResetManager()    { return arenaResetManager; }
    public RankManager getRankManager()                { return rankManager; }
    public TrimManager getTrimManager()                { return trimManager; }
    public KillEffectManager getKillEffectManager()    { return killEffectManager; }
    public RankNPCManager getRankNPCManager()          { return rankNPCManager; }
    public TrimsGUI getTrimsGUI()                      { return trimsGUI; }
}
