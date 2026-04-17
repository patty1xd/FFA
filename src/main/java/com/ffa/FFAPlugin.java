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

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        tierManager = new TierManager(this);
        npcManager = new NPCManager(this);
        kitManager = new KitManager(this);
        scoreboardManager = new ScoreboardManager(this);

        getServer().getPluginManager().registerEvents(new PlayerKillListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCInteractListener(this), this);
        getServer().getPluginManager().registerEvents(new NPCProtectListener(this), this);

        getCommand("spawnnpc").setExecutor(new SpawnNPCCommand(this));
        getCommand("removenpc").setExecutor(new RemoveNPCCommand(this));
        getCommand("tier").setExecutor(new TierCommand(this));
        getCommand("settier").setExecutor(new SetTierCommand(this));
        getCommand("resetplayer").setExecutor(new ResetPlayerCommand(this));

        npcManager.restoreNPC();
        scoreboardManager.startUpdater();

        getLogger().info("FFAKit enabled!");
    }

    @Override
    public void onDisable() {
        if (tierManager != null) tierManager.saveAll();
        if (npcManager != null) npcManager.removeNPC();
        getLogger().info("FFAKit disabled.");
    }

    public static FFAPlugin getInstance() { return instance; }
    public TierManager getTierManager() { return tierManager; }
    public NPCManager getNPCManager() { return npcManager; }
    public KitManager getKitManager() { return kitManager; }
    public ScoreboardManager getBoardManager() { return scoreboardManager; }
}
