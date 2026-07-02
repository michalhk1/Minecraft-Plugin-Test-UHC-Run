package Services;

import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class LobbyService {
    private final Map<String, World> _lobbies = new HashMap<>();

    public void addLobby(String name, World world) {
        _lobbies.put(name, world);
    }

    public World getLobby(String name) {
        return _lobbies.get(name);
    }

    public boolean lobbyExists(String name) {
        return _lobbies.containsKey(name);
    }

    public int lobbyCount(){
        return _lobbies.size();
    }
}
