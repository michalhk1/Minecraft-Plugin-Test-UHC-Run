package Game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class GameSession {

    private final World _lobbyWorld;
    private final World _arenaWorld;
    private GameState _state = GameState.WAITING;

    private final List<Location> _platforms = new ArrayList<>();
    private final Set<UUID> _fallProtected = new HashSet<>();
    private final List<UUID> _players = new ArrayList<>();
    private Scoreboard _scoreboard;

    public GameSession(World lobbyWorld, World arenaWorld) {
        _lobbyWorld = lobbyWorld;
        _arenaWorld = arenaWorld;
    }

    public Scoreboard getScoreboard() {
        return _scoreboard;
    }

    public void setScoreboard(Scoreboard scoreboard) {
        _scoreboard = scoreboard;
    }

    public World getLobbyWorld() {
        return _lobbyWorld;
    }

    public World getArenaWorld() {
        return _arenaWorld;
    }

    public GameState getState() {
        return _state;
    }

    public void setState(GameState state) {
        _state = state;
    }

    public boolean isFrozen() {
        return _state == GameState.STARTING;
    }

    public List<Location> getPlatforms() {
        return _platforms;
    }

    public void setPlatforms(List<Location> platforms) {
        _platforms.clear();
        _platforms.addAll(platforms);
    }

    public void clearPlatforms() {
        _platforms.clear();
    }

    public void addPlayer(Player player) {
        _players.add(player.getUniqueId());
    }

    public void removePlayer(Player player) {
        _players.remove(player.getUniqueId());
    }

    public int getAlivePlayerCount() {
        return _players.size();
    }

    public List<UUID> getPlayerIds() {
        return _players;
    }

    public List<Player> getOnlinePlayers() {
        List<Player> result = new ArrayList<>();
        for (UUID id : _players) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) result.add(p);
        }
        return result;
    }

    public void protectFromFall(Player player) {
        _fallProtected.add(player.getUniqueId());
    }

    public boolean consumeFallProtection(Player player) {
        return _fallProtected.remove(player.getUniqueId());
    }
}