package Commands;

import Game.GameSession;
import Game.GameState;
import Services.GameManagerService;
import Services.LobbyService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.HeightMap;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LobbyCommand implements BasicCommand {

    private final LobbyService _lobbyService;
    private final GameManagerService _gameManagerService;

    public LobbyCommand(LobbyService lobbyService, GameManagerService gameManagerService) {
        _lobbyService = lobbyService;
        _gameManagerService = gameManagerService;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        commandSourceStack.getSender().sendMessage(
                Component.text("Použití: /lobby <číslo>").color(NamedTextColor.RED)
        );
    }

    public void executeWithNumber(CommandSourceStack commandSourceStack, int lobbyNumber) {
        if (!(commandSourceStack.getSender() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    Component.text("Tento příkaz může použít pouze hráč!").color(NamedTextColor.RED)
            );
            return;
        }

        World lobby = _lobbyService.getLobby("lobby" + lobbyNumber);

        if (lobby == null) {
            player.sendMessage(Component.text("Lobby #" + lobbyNumber + " neexistuje!").color(NamedTextColor.RED));
            return;
        }

        GameSession session = _gameManagerService.getSession(lobby);
        if (session != null && session.getState() != GameState.WAITING && session.getState() != GameState.ENDED) {
            player.sendMessage(Component.text("Hra v lobby #" + lobbyNumber + " už běží, nelze se připojit!").color(NamedTextColor.RED));
            return;
        }

        int x = (int) lobby.getSpawnLocation().getX();
        int z = (int) lobby.getSpawnLocation().getZ();
        Location spawn = lobby.getHighestBlockAt(x, z, HeightMap.MOTION_BLOCKING_NO_LEAVES)
                .getLocation().add(0, 1, 0);

        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(spawn);
        player.sendMessage(Component.text("Přesouvám do lobby #" + lobbyNumber + "...").color(NamedTextColor.GOLD));
    }
}