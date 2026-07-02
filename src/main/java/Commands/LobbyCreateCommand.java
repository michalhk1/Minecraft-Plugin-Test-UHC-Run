package Commands;

import Services.LobbyService;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;


public class LobbyCreateCommand implements BasicCommand {

    private final LobbyService _lobbyService;
    private int _lastLobbyNumber;

    public LobbyCreateCommand(LobbyService lobbyService) {
        _lobbyService = lobbyService;
    }

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {

        if(commandSourceStack.getSender() instanceof Player player){
            player.sendMessage(Component.text("Vytvářím lobby...").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
            lobbyWorldCreation(player);

            lobbyJoinBroadcastMessage();
        }
        else{
            commandSourceStack.getSender().sendMessage(Component.text("Tento příkaz může použít pouze hráč!").color(NamedTextColor.RED));
        }
    }

    private void lobbyJoinBroadcastMessage(){
        Component message = Component.text("Lobby je připraveno! ", NamedTextColor.YELLOW)
                .append(
                        Component.text("[Připojit se]", NamedTextColor.GREEN)
                                .decorate(TextDecoration.BOLD)
                                .clickEvent(ClickEvent.runCommand("/lobby "+ _lastLobbyNumber))
                                .hoverEvent(HoverEvent.showText(Component.text("Klikni pro vstup do lobby", NamedTextColor.GRAY)))
                );

        Bukkit.broadcast(message);
    }

    private void lobbyWorldCreation(Player player){
        _lastLobbyNumber = _lobbyService.lobbyCount()+1;
        WorldCreator creator = new WorldCreator("lobby"+_lastLobbyNumber);
        creator.type(WorldType.FLAT);
        creator.generateStructures(false);
        World lobby = creator.createWorld();
        _lobbyService.addLobby(lobby.getName(), lobby);

        lobby.setDifficulty(Difficulty.PEACEFUL);
        lobby.setGameRule(GameRules.ADVANCE_TIME, false);
        lobby.setGameRule(GameRules.ADVANCE_WEATHER, false);
        lobby.setGameRule(GameRules.SPAWN_MOBS, false);
        lobby.setGameRule(GameRules.KEEP_INVENTORY, true);
        player.setGameMode(GameMode.ADVENTURE);
        lobby.setTime(6000);
        lobby.getWorldBorder().setSize(50);
        Location spawn = new Location(lobby, 0, lobby.getHighestBlockYAt(0,0)+1, 0);
        player.sendMessage(Component.text("Přesouvám do lobby...").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
        player.teleport(spawn);
    }
}
