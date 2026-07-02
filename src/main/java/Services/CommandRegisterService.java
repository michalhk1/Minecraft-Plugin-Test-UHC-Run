package Services;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.LifecycleEventManager;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.plugin.Plugin;
import Commands.LobbyCommand;
import Commands.LobbyCreateCommand;
import Commands.LobbyStartCommand;
import Commands.LeaveCommand;

public class CommandRegisterService {

    private LifecycleEventManager<Plugin> _lifecycleManager;
    public LobbyStartCommand lobbyStartCommand;

    private GameManagerService _gameManagerService;


    public CommandRegisterService(LifecycleEventManager<Plugin> lifecycleManager,GameManagerService gameManagerService){
        _lifecycleManager = lifecycleManager;
        _gameManagerService = gameManagerService;

    }

    public void RegisterCommands(LobbyService lobbyService, ArenaService arenaService){
        lobbyStartCommand = new LobbyStartCommand(_gameManagerService, lobbyService);

        _lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS,
                event -> {
                    final Commands commands = event.registrar();

                    commands.register(
                            Commands.literal("createlobby")
                                    .executes(ctx->{
                                new LobbyCreateCommand(lobbyService).execute(ctx.getSource(),new String[]{});
                                return Command.SINGLE_SUCCESS;
                            }).build()
                    );

                    commands.register(
                            Commands.literal("startlobby")
                                    .executes(ctx->{
                                        lobbyStartCommand.execute(ctx.getSource(),new String[]{});
                                        return Command.SINGLE_SUCCESS;
                                    }).build()
                    );

                    commands.register(
                            Commands.literal("lobby")
                                    .then(
                                            Commands.argument("číslo", IntegerArgumentType.integer(1))
                                                    .executes(ctx -> {
                                                        int number = IntegerArgumentType.getInteger(ctx, "číslo");
                                                        new LobbyCommand(lobbyService, _gameManagerService).executeWithNumber(ctx.getSource(), number);
                                                        return Command.SINGLE_SUCCESS;
                                                    })
                                    )
                                    .build()
                    );
                    commands.register("lobbystart", new LobbyStartCommand(_gameManagerService, lobbyService));
                    commands.register("leave", new LeaveCommand());
        });
    }
}
