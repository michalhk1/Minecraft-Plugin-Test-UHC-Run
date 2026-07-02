package Services;

import Game.GameSession;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Criteria;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class ScoreboardService {

    private static final String OBJECTIVE_NAME = "uhc_state";

    public Scoreboard createScoreboard(GameSession session) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();

        Objective objective = board.registerNewObjective(
                OBJECTIVE_NAME,
                Criteria.DUMMY,
                Component.text("UHC Run").color(NamedTextColor.GOLD)
        );
        objective.setDisplaySlot(org.bukkit.scoreboard.DisplaySlot.SIDEBAR);

        return board;
    }

    public void assignToPlayers(GameSession session) {
        Scoreboard board = session.getScoreboard();
        for (Player p : session.getOnlinePlayers()) {
            p.setScoreboard(board);
        }
    }

    /**
     * Aktualizuje scoreboard. remainingSeconds je nepovinný odpočet aktuální fáze
     * (mining countdown, grace period, ...) - pošli -1 pokud se nemá zobrazovat.
     */
    public void update(GameSession session, int remainingSeconds) {
        Scoreboard board = session.getScoreboard();
        if (board == null) return;

        Objective objective = board.getObjective(OBJECTIVE_NAME);
        if (objective == null) return;

        for (String entry : board.getEntries()) {
            board.resetScores(entry);
        }

        String stateText = switch (session.getState()) {
            case STARTING -> "Příprava";
            case MINING -> "Mining";
            case PVP -> "PvP";
            case ENDED -> "Konec";
            default -> "Čekání";
        };

        int line = 6;
        setLine(objective, "§7Fáze: §f" + stateText, line--);
        setLine(objective, "§7Hráči: §f" + session.getOnlinePlayers().size(), line--);

        if (remainingSeconds >= 0) {
            setLine(objective, "§7Čas: §f" + formatTime(remainingSeconds), line--);
        }

        if (session.getArenaWorld() != null) {
            double borderSize = session.getArenaWorld().getWorldBorder().getSize();
            setLine(objective, "§7Border: §f" + (int) borderSize, line--);
        }
    }

    public void update(GameSession session) {
        update(session, -1);
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void setLine(Objective objective, String text, int score) {
        objective.getScore(text).setScore(score);
    }
}