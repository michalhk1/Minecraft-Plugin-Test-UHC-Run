package Services;

import org.bukkit.World;

import java.util.HashMap;
import java.util.Map;

public class ArenaService {
    private final Map<String, World> _arenas = new HashMap<>();

    public void addArena(String name, World world) {
        _arenas.put(name, world);
    }

    public void removeArena(String name) {
        _arenas.remove(name);
    }

    public World getArena(String name) {
        return _arenas.get(name);
    }

    public boolean arenaExists(String name) {
        return _arenas.containsKey(name);
    }

    public int arenaCount(){
        return _arenas.size();
    }
}
