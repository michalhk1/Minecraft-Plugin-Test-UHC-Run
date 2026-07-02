package org.uHCRun;

import Listeners.GameListener;
import Services.*;
import org.bukkit.plugin.java.JavaPlugin;


public final class UHCRun extends JavaPlugin {


    @Override
    public void onEnable() {

        LobbyService lobbyService = new LobbyService();
        ArenaService arenaService = new ArenaService();
        ScoreboardService scoreboardService = new ScoreboardService();
        GameManagerService gameManagerService = new GameManagerService(this, arenaService, scoreboardService);
        CommandRegisterService commandRegisterService = new CommandRegisterService(getLifecycleManager(), gameManagerService);
        commandRegisterService.RegisterCommands(lobbyService, arenaService);
        RecipeManagerService recipeManagerService = new RecipeManagerService(this);
        recipeManagerService.registerRecipes();

        getServer().getPluginManager().registerEvents(new GameListener(gameManagerService), this);

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
