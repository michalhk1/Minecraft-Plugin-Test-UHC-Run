package Commands;

import Services.GameManagerService;
import Services.LobbyService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class LobbyStartCommand implements BasicCommand {

    private final GameManagerService _gameManagerService;
    private final LobbyService _lobbyService;

    public LobbyStartCommand(GameManagerService gameManagerService, LobbyService lobbyService) {
        _gameManagerService = gameManagerService;
        _lobbyService = lobbyService;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        if (!(commandSourceStack.getSender() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    Component.text("Tento příkaz může použít pouze hráč!").color(NamedTextColor.RED)
            );
            return;
        }
        if (!_lobbyService.lobbyExists(player.getWorld().getName())) {
            player.sendMessage(Component.text("Tento příkaz lze použít jen v lobby světě!").color(NamedTextColor.RED));
            return;
        }

        boolean started = _gameManagerService.startGame(player.getWorld());

        if (!started) {
            player.sendMessage(Component.text("Hra už běží nebo nejsou žádní hráči v lobby!").color(NamedTextColor.RED));
        }
    }
}
