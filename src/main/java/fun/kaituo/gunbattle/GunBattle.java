package fun.kaituo.gunbattle;


import fun.kaituo.gameutils.GameUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;


public class GunBattle extends JavaPlugin {
    private GameUtils gameUtils;
    List<Player> players = new ArrayList<>();

    public static GunBattleGame getGameInstance() {
        return GunBattleGame.getInstance();
    }

    public void onEnable() {
        gameUtils = (GameUtils) Bukkit.getPluginManager().getPlugin("GameUtils");
        gameUtils.registerGame(getGameInstance());
    }

    public void onDisable() {
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
        for (Player p: Bukkit.getOnlinePlayers()) {
            if (gameUtils.getPlayerGame(p) == getGameInstance()) {
                Bukkit.dispatchCommand(p, "join Lobby");
            }
        }
        gameUtils.unregisterGame(getGameInstance());
    }
}