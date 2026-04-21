package com.ffa;

import com.ffa.commands.*;
import com.ffa.listeners.*;
import com.ffa.managers.*;
import org.bukkit.plugin.java.JavaPlugin;

public class FFAPlugin extends JavaPlugin {

    private static FFAPlugin instance;
    private TierManager tierManager;
    private NPCManager npcManager;
    private KitManager kitManager;
    private ScoreboardManager scoreboardManager;
    private NormalizationManager normalizationManager;
    private ChatManager chatManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        tierManager = new TierManager(this);
        npcManager = new NPCManager(this);
        kitManager = new KitManager(this);
        scoreboardManager = new ScoreboardManager(this);
        normalizationManager = new NormalizationManager(this);
        chatManager = new ChatManager(this);

        getServer().getPluginManager().registerEvents(new PlayerKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCProtectListener(this), this);
        getServer().getPluginManager().registerEvents(normalizationManager, this);

        getCommand("spawnnpc").setExecutor(new SpawnNPCCommand(this));
        getCommand("removenpc").setExecutor(new RemoveNPCCommand(this));
        getCommand("tier").setExecutor(new TierCommand(this));
        getCommand("settier").setExecutor(new SetTierCommand(this));
        getCommand("resetplayer").setExecutor(new ResetPlayerCommand(this));
        getCommand("koalaffa").setExecutor((sender, cmd, label, args) -> {
            if (!sender.hasPermission("ffa.admin")) { sender.sendMessage("§cNo permission."); return true; }
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig(); sender.sendMessage("§aKoalaFFA config reloaded!"); return true;
            }
            sender.sendMessage("§cUsage: /koalaffa reload"); return true;
        });

        MsgCommand msgCmd = new MsgCommand(this);
        getCommand("msg").setExecutor(msgCmd);
        getCommand("tell").setExecutor(msgCmd);
        ReplyCommand replyCmd = new ReplyCommand(this);
        getCommand("reply").setExecutor(replyCmd);
        getCommand("r").setExecutor(replyCmd);

        npcManager.restoreNPC();
        scoreboardManager.startUpdater();

        getLogger().info("KoalaFFA enabled!");
    }

    @Override
    public void onDisable() {
        if (tierManager != null) tierManager.saveAll();
        if (npcManager != null) npcManager.removeNPC();
        getLogger().info("KoalaFFA disabled.");
    }

    public static FFAPlugin getInstance() { return instance; }
    public TierManager getTierManager() { return tierManager; }
    public NPCManager getNPCManager() { return npcManager; }
    public KitManager getKitManager() { return kitManager; }
    public ScoreboardManager getBoardManager() { return scoreboardManager; }
    public NormalizationManager getNormalizationManager() { return normalizationManager; }
    public ChatManager getChatManager() { return chatManager; }
}
