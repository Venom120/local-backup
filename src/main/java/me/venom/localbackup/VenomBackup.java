package me.venom.localbackup;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

public class VenomBackup extends JavaPlugin {

    private Logger log;
    private FileConfiguration config;
    private BackupTask backupTask;

    @Override
    public void onEnable() {
        this.log = getLogger();

        // Save default config.yml if not present
        saveDefaultConfig();
        config = getConfig();

        int delayMinutes = config.getInt("backup.delay_minutes", 30);
        long delayTicks = 20L * 60 * delayMinutes;

        this.backupTask = new BackupTask(this);
        getServer().getScheduler().runTaskTimerAsynchronously(this, backupTask, delayTicks, delayTicks);

        log.info("venom-local-backup enabled. Backups every " + delayMinutes + " minutes.");
    }

    @Override
    public void onDisable() {
        log.info("Server shutting down. Performing final backup...");
        backupTask.run();
        log.info("Backup complete.");
    }

    public FileConfiguration getBackupConfig() {
        return config;
    }
}
