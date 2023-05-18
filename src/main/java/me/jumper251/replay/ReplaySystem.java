package me.jumper251.replay;


import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import me.jumper251.replay.api.ReplayAPI;
import me.jumper251.replay.filesystem.ConfigManager;
import me.jumper251.replay.filesystem.saving.DatabaseReplaySaver;
import me.jumper251.replay.filesystem.saving.DefaultReplaySaver;
import me.jumper251.replay.filesystem.saving.ReplaySaver;
import me.jumper251.replay.replaysystem.Replay;
import me.jumper251.replay.replaysystem.utils.ReplayCleanup;
import me.jumper251.replay.utils.Metrics;
import me.jumper251.replay.utils.ReplayManager;
import me.jumper251.replay.utils.Updater;


public class ReplaySystem extends JavaPlugin {

	
	public static ReplaySystem instance;
	
	public static Updater updater;
	public static Metrics metrics;
	
	public final static String PREFIX = "§8[§3Replay§8] §r§7";

	
	@Override
	public void onDisable() {
		for (Replay replay : new HashMap<>(ReplayManager.activeReplays).values()) {
		    if (replay.isRecording() && replay.getRecorder().getData().getActions().size() > 0) {
				replay.getRecorder().stop(ConfigManager.SAVE_STOP);
				
			}
		}

	}
	
	@Override
	public void onEnable() {
		instance = this;
		
		Long start = System.currentTimeMillis();

		getLogger().info("Loading Replay v" + getDescription().getVersion() + " by " + getDescription().getAuthors().get(0));
		
		ConfigManager.loadConfigs();
		ReplayManager.register();
		
		ReplaySaver.register(ConfigManager.USE_DATABASE ? new DatabaseReplaySaver() : new DefaultReplaySaver());
		
		updater = new Updater();
		metrics = new Metrics(this, 2188);
		
		if (ConfigManager.CLEANUP_REPLAYS > 0) {
			ReplayCleanup.cleanupReplays();
		}
		
        getLogger().info("Finished (" + (System.currentTimeMillis() - start) + "ms)");
        if (ConfigManager.RECORD_STARTUP) {
            getLogger().info("Auto Record Time : " + ConfigManager.MAX_LENGTH + " sec");
            autoRecordTask();
        }
	}
	
	
	public static ReplaySystem getInstance() {
		return instance;
	}

    void autoRecordTask() {
        BukkitScheduler scheduler = getServer().getScheduler();
        scheduler.scheduleSyncRepeatingTask(this, () -> {
            if (Bukkit.getOnlinePlayers().size() > 0) {
                String formattedDateTime =
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd_HHmmss"));
                ReplayAPI.getInstance().recordReplay(formattedDateTime,
                        new ArrayList<Player>(Bukkit.getOnlinePlayers()));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        ReplayAPI.getInstance().stopReplay(formattedDateTime, true, true);
                    }
                }.runTaskLater(ReplaySystem.getInstance(), 20 * ConfigManager.MAX_LENGTH);
            }
        }, 0L, 20 * ConfigManager.MAX_LENGTH);
    }
}
