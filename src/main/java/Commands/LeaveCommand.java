package Commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class LeaveCommand implements BasicCommand {

    @Override
    public void execute(CommandSourceStack commandSourceStack, String[] args) {
        if (!(commandSourceStack.getSender() instanceof Player player)) {
            commandSourceStack.getSender().sendMessage(
                    Component.text("Tento příkaz může použít pouze hráč!").color(NamedTextColor.RED)
            );
            return;
        }

        World defaultWorld = Bukkit.getWorlds().get(0);
        Location spawn = defaultWorld.getSpawnLocation();

        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        player.getInventory().clear();
        player.setGameMode(GameMode.ADVENTURE);
        player.teleport(spawn);
        player.sendMessage(Component.text("Opustil jsi hru.").color(NamedTextColor.GRAY));
    }
}
