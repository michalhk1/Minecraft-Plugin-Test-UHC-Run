package Services;

import Game.GameSession;
import Game.GameState;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class GameManagerService {

    private static final int MIN_PLAYERS_TO_AUTOSTART = 2;
    private static final int COUNTDOWN_SECONDS = 3;
    private static final int MINING_DURATION_SECONDS = 10;
    private static final int GRACE_PERIOD_SECONDS = 10;

    private static final double BORDER_START_SIZE = 1000;
    private static final double BORDER_END_SIZE = 50;
    private static final long BORDER_SHRINK_SECONDS = 60*10;
    private static final double BORDER_CENTER_X = 0;
    private static final double BORDER_CENTER_Z = 0;

    private static final double PVP_TELEPORT_HEIGHT = 250;
    private static final int ARENA_CLEANUP_DELAY_SECONDS = 30;

    private final Plugin _plugin;
    private final ArenaService _arenaService;
    private final ScoreboardService _scoreboardService;

    // klíč je lobby svět -> session (po startu obsahuje i arena svět)
    private final Map<World, GameSession> _sessions = new HashMap<>();
    private int _lastArenaNumber = 0;

    public GameManagerService(Plugin plugin, ArenaService arenaService, ScoreboardService scoreboardService) {
        _plugin = plugin;
        _arenaService = arenaService;
        _scoreboardService = scoreboardService;
    }

    public GameSession getSession(World world) {
        return _sessions.get(world);
    }

    public GameSession getSessionByArena(World arenaWorld) {
        for (GameSession session : _sessions.values()) {
            if (session.getArenaWorld() != null && session.getArenaWorld().equals(arenaWorld)) {
                return session;
            }
        }
        return null;
    }

    /**
     * Zavolej při přesunu hráče do lobby (PlayerChangedWorldEvent). Pokud je dost hráčů
     * a hra ještě neběží, autostartne.
     */
    public void onPlayerJoinedLobby(World lobbyWorld) {
        GameSession existing = _sessions.get(lobbyWorld);
        if (existing != null && existing.getState() != GameState.WAITING && existing.getState() != GameState.ENDED) {
            return;
        }

        int playerCount = lobbyWorld.getPlayers().size();
        if (playerCount >= MIN_PLAYERS_TO_AUTOSTART) {
            startGame(lobbyWorld);
        }
    }

    /**
     * Manuální/early start - zavolané z LobbyStartCommand, přebije autostart podmínku.
     */
    public boolean startGame(World lobbyWorld) {
        GameSession existing = _sessions.get(lobbyWorld);
        if (existing != null && existing.getState() != GameState.WAITING && existing.getState() != GameState.ENDED) {
            return false;
        }

        List<Player> players = new ArrayList<>(lobbyWorld.getPlayers());
        if (players.isEmpty()) return false;

        World arenaWorld = createArenaWorld();
        GameSession session = new GameSession(lobbyWorld, arenaWorld);
        _sessions.put(lobbyWorld, session);

        setupScoreboard(session);
        setupArena(session, players);
        return true;
    }

    private World createArenaWorld() {
        WorldCreator creator = new WorldCreator("arena" + _lastArenaNumber);
        _lastArenaNumber += 1;
        creator.type(WorldType.NORMAL);
        creator.generateStructures(true);
        World arena = creator.createWorld();
        _arenaService.addArena(arena.getName(), arena);
        arena.setDifficulty(Difficulty.PEACEFUL);
        arena.setGameRule(GameRules.SPAWN_MOBS, false);
        arena.setTime(6000);
        arena.setGameRule(GameRules.PVP, false);


        WorldBorder border = arena.getWorldBorder();
        border.setCenter(BORDER_CENTER_X, BORDER_CENTER_Z);
        border.setSize(BORDER_START_SIZE);

        return arena;
    }

    private void setupScoreboard(GameSession session) {
        session.setScoreboard(_scoreboardService.createScoreboard(session));
    }

    private void setupArena(GameSession session, List<Player> players) {
        World arenaWorld = session.getArenaWorld();

        List<Location> platforms = generatePlatforms(arenaWorld, players.size());
        session.setPlatforms(platforms);

        Collections.shuffle(players);

        session.setState(GameState.STARTING);

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            session.addPlayer(player);
            player.teleport(platforms.get(i));
            player.setGameMode(GameMode.SURVIVAL);
        }

        _scoreboardService.assignToPlayers(session);
        _scoreboardService.update(session);

        startCountdown(session);
    }

    private List<Location> generatePlatforms(World world, int playerCount) {
        List<Location> locations = new ArrayList<>();

        int columns = (int) Math.ceil(Math.sqrt(playerCount));
        int rows = (int) Math.ceil((double) playerCount / columns);

        int spacing = 80;
        int centerX = (int) BORDER_CENTER_X;
        int centerZ = (int) BORDER_CENTER_Z;
        int y = 150;

        int startX = centerX - ((columns - 1) * spacing) / 2;
        int startZ = centerZ - ((rows - 1) * spacing) / 2;

        for (int x = 0; x < columns; x++) {
            for (int z = 0; z < rows; z++) {
                if (locations.size() >= playerCount) return locations;

                Location loc = new Location(world, startX + x * spacing, y, startZ + z * spacing);
                createPlatform(loc);
                locations.add(loc.clone().add(0.5, 1, 0.5));
            }
        }

        return locations;
    }

    private void createPlatform(Location center) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                center.getBlock().getRelative(x, 0, z).setType(Material.GLASS);
            }
        }
    }

    private void removePlatforms(List<Location> platforms) {
        for (Location loc : platforms) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    loc.getBlock().getRelative(x, -1, z).setType(Material.AIR);
                }
            }
        }
        platforms.clear();
    }

    private void startCountdown(GameSession session) {
        World arenaWorld = session.getArenaWorld();
        AtomicInteger seconds = new AtomicInteger(COUNTDOWN_SECONDS);

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(_plugin, task -> {

            if (seconds.get() <= 0) {
                finishCountdown(session);
                task.cancel();
                return;
            }

            for (Player p : arenaWorld.getPlayers()) {
                p.sendActionBar(
                        Component.text("Start za ", NamedTextColor.YELLOW)
                                .append(Component.text(seconds.get(), NamedTextColor.RED))
                );
            }

            seconds.decrementAndGet();

        }, 1, 20);
    }

    private void finishCountdown(GameSession session) {
        removePlatforms(session.getPlatforms());

        for (Player p : session.getArenaWorld().getPlayers()) {
            session.protectFromFall(p);
            p.showTitle(Title.title(
                    Component.text("START!").color(NamedTextColor.GREEN),
                    Component.text("Můžete se hýbat").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(2000), Duration.ofMillis(500))
            ));
        }

        startMiningPhase(session);
    }

    private void startMiningPhase(GameSession session) {
        session.setState(GameState.MINING);
        session.getArenaWorld().setGameRule(GameRules.PVP, false);
        _scoreboardService.update(session, MINING_DURATION_SECONDS);

        for (Player p : session.getArenaWorld().getPlayers()) {
            p.showTitle(Title.title(
                    Component.text("Mining fáze!").color(NamedTextColor.GOLD),
                    Component.text("Sbírej zdroje, dokud to jde").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
            ));
        }

        AtomicInteger seconds = new AtomicInteger(MINING_DURATION_SECONDS);

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(_plugin, task -> {

            if (session.getState() != GameState.MINING) {
                task.cancel();
                return;
            }

            int current = seconds.get();

            if (current <= 0) {
                startGracePeriod(session);
                task.cancel();
                return;
            }

            if (current == 60 || current == 10) {
                for (Player p : session.getArenaWorld().getPlayers()) {
                    p.sendActionBar(
                            Component.text("PvP fáze za ", NamedTextColor.YELLOW)
                                    .append(Component.text(current + "s", NamedTextColor.RED))
                    );
                }
            }

            _scoreboardService.update(session, current);
            seconds.decrementAndGet();

        }, 1, 20);
    }


    private void startGracePeriod(GameSession session) {
        World arenaWorld = session.getArenaWorld();

        for (Player p : session.getOnlinePlayers()) {
            Location current = p.getLocation();
            Location airLocation = new Location(
                    arenaWorld,
                    current.getX(),
                    current.getY() + PVP_TELEPORT_HEIGHT,
                    current.getZ(),
                    current.getYaw(),
                    current.getPitch()
            );
            session.protectFromFall(p);
            p.teleport(airLocation);

            p.showTitle(Title.title(
                    Component.text("Grace period").color(NamedTextColor.AQUA),
                    Component.text("PvP začíná za " + GRACE_PERIOD_SECONDS + "s").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
            ));
        }

        AtomicInteger seconds = new AtomicInteger(GRACE_PERIOD_SECONDS);

        Bukkit.getGlobalRegionScheduler().runAtFixedRate(_plugin, task -> {

            int current = seconds.get();

            if (current <= 0) {
                startPvpPhase(session);
                task.cancel();
                return;
            }

            for (Player p : arenaWorld.getPlayers()) {
                p.sendActionBar(
                        Component.text("PvP za ", NamedTextColor.YELLOW)
                                .append(Component.text(current, NamedTextColor.RED))
                );
            }

            _scoreboardService.update(session, current);
            seconds.decrementAndGet();

        }, 1, 20);
    }

    private void startPvpPhase(GameSession session) {
        session.setState(GameState.PVP);
        World arenaWorld = session.getArenaWorld();
        arenaWorld.setGameRule(GameRules.PVP, true);
        _scoreboardService.update(session);

        for (Player p : arenaWorld.getPlayers()) {
            p.showTitle(Title.title(
                    Component.text("PvP fáze!").color(NamedTextColor.RED),
                    Component.text("Poslední hráč vítězí").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(1500), Duration.ofMillis(500))
            ));
        }

        startBorderShrink(session);
    }

    private void startBorderShrink(GameSession session) {
        World arenaWorld = session.getArenaWorld();
        WorldBorder border = arenaWorld.getWorldBorder();


        border.changeSize(BORDER_END_SIZE, BORDER_SHRINK_SECONDS);


        AtomicInteger elapsed = new AtomicInteger(0);
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(_plugin, task -> {

            if (session.getState() != GameState.PVP) {
                task.cancel();
                return;
            }

            _scoreboardService.update(session);

            elapsed.addAndGet(5);
            if (elapsed.get() >= BORDER_SHRINK_SECONDS) {
                task.cancel();
            }

        }, 100, 100);
    }

    public void onPlayerEliminated(GameSession session, Player eliminatedPlayer) {
        if (session.getState() != GameState.PVP && session.getState() != GameState.MINING) {
            return;
        }

        Component eliminationMessage = Component.text(eliminatedPlayer.getName() + " byl vyřazen!", NamedTextColor.RED);
        for (Player p : session.getOnlinePlayers()) {
            p.sendMessage(eliminationMessage);
        }

        if ((session.getState() == GameState.PVP || session.getState() == GameState.MINING) && session.getAlivePlayerCount() <= 1) {
            endGame(session);
        }
    }

    private void endGame(GameSession session) {
        session.setState(GameState.ENDED);

        List<Player> winners = session.getOnlinePlayers();
        Component winMessage = winners.isEmpty()
                ? Component.text("Hra skončila, nikdo nepřežil!", NamedTextColor.GRAY)
                : Component.text(winners.get(0).getName() + " vyhrává hru!", NamedTextColor.GOLD);

        Bukkit.broadcast(winMessage);

        for (Player p : winners) {
            p.showTitle(Title.title(
                    Component.text("Vítězství!").color(NamedTextColor.GOLD),
                    Component.text("Gratulujeme!").color(NamedTextColor.WHITE),
                    Title.Times.times(Duration.ZERO, Duration.ofMillis(3000), Duration.ofMillis(1000))
            ));
        }

        scheduleArenaCleanup(session);
    }

    private void scheduleArenaCleanup(GameSession session) {
        Bukkit.getGlobalRegionScheduler().runDelayed(_plugin, task -> {

            World arenaWorld = session.getArenaWorld();
            World lobbyWorld = session.getLobbyWorld();

            Location lobbySpawn = lobbyWorld.getHighestBlockAt(
                    (int) lobbyWorld.getSpawnLocation().getX(),
                    (int) lobbyWorld.getSpawnLocation().getZ(),
                    HeightMap.MOTION_BLOCKING_NO_LEAVES
            ).getLocation().add(0, 1, 0);

            for (Player p : new ArrayList<>(arenaWorld.getPlayers())) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                p.getInventory().clear();
                p.setGameMode(GameMode.ADVENTURE);
                p.teleport(lobbySpawn);
            }

            _sessions.remove(lobbyWorld);
            _arenaService.removeArena(arenaWorld.getName());
            var save = Bukkit.unloadWorld(arenaWorld, false);
            int i = 0;
            i = 1;

        }, ARENA_CLEANUP_DELAY_SECONDS);
    }
}