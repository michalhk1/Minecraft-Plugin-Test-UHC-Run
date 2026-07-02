package Listeners;

import Game.GameSession;
import Game.GameState;
import Services.GameManagerService;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class GameListener implements Listener {

    private final GameManagerService _gameManagerService;
    private final Map<UUID, Location> _deathLocations = new HashMap<>();

    public GameListener(GameManagerService gameManagerService) {
        _gameManagerService = gameManagerService;
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent e) {
        _gameManagerService.onPlayerJoinedLobby(e.getPlayer().getWorld());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {
        GameSession session = _gameManagerService.getSessionByArena(e.getPlayer().getWorld());
        if (session == null || !session.isFrozen()) return;

        var from = e.getFrom();
        var to = e.getTo();
        if (from.getX() != to.getX() || from.getZ() != to.getZ() || from.getY() != to.getY()) {
            e.setTo(from);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        GameSession session = _gameManagerService.getSessionByArena(e.getPlayer().getWorld());
        if (session != null && session.isFrozen()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        GameSession session = _gameManagerService.getSessionByArena(e.getPlayer().getWorld());
        if (session != null && session.isFrozen()) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent e) {
        if (e.getCause() != EntityDamageEvent.DamageCause.FALL) return;
        if (!(e.getEntity() instanceof Player player)) return;

        GameSession session = _gameManagerService.getSessionByArena(player.getWorld());
        if (session == null) return;

        if (session.consumeFallProtection(player)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getPlayer();
        GameSession session = _gameManagerService.getSessionByArena(player.getWorld());
        if (session == null) return;

        session.removePlayer(player);
        _deathLocations.put(player.getUniqueId(), player.getLocation());
        _gameManagerService.onPlayerEliminated(session, player);
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        Player player = e.getPlayer();
        Location deathLocation = _deathLocations.remove(player.getUniqueId());
        if (deathLocation == null) return;

        e.setRespawnLocation(deathLocation);
        player.setGameMode(GameMode.SPECTATOR);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        _deathLocations.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onRegainHealth(EntityRegainHealthEvent e) {
        if (e.getRegainReason() != EntityRegainHealthEvent.RegainReason.REGEN) return;
        if (!(e.getEntity() instanceof Player player)) return;

        GameSession session = _gameManagerService.getSessionByArena(player.getWorld());
        if (session != null) {
            e.setCancelled(true);
        }
    }
}
