import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.logging.Logger;
import me.venom.localbackup.BackupTask;
import me.venom.localbackup.BackupCommandTabCompleter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class VenomBackup extends JavaPlugin {

    private Logger log;
    private FileConfiguration config;
    private BackupTask backupTask;

    private Instant lastBackupTime;

    private long backupIntervalTicks;  // in ticks
    private long nextBackupTick;       // when next backup is scheduled


    @Override
    public void onEnable() {
        this.log = getLogger();

        saveDefaultConfig();
        config = getConfig();

        int delayMinutes = config.getInt("backup.delay_minutes", 30);
        backupIntervalTicks = 20L * 60 * delayMinutes;

        this.backupTask = new BackupTask(this);

        getServer().getScheduler().runTaskTimerAsynchronously(this, () -> {
            backupTask.run();
            lastBackupTime = Instant.now();
            nextBackupTick = getServer().getCurrentTick() + backupIntervalTicks;
        }, backupIntervalTicks, backupIntervalTicks);

        nextBackupTick = getServer().getCurrentTick() + backupIntervalTicks;

        getCommand("backup").setTabCompleter(new BackupCommandTabCompleter());

        log.info("venom-local-backup enabled. Backups every " + delayMinutes + " minutes.");
    }


    @Override
    public void onDisable() {
        log.info("Server shutting down. Performing final backup...");
        backupTask.run();
        lastBackupTime = Instant.now();
        log.info("Backup complete.");
    }

    public FileConfiguration getBackupConfig() {
        return config;
    }

    public void setLastBackupTime(Instant instant) {
        this.lastBackupTime = instant;
    }

    public Instant getLastBackupTime() {
        return lastBackupTime;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§cUsage: /backup <now|last|reload>");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "now" -> {
                sender.sendMessage("§aRunning backup...");
                getServer().getScheduler().runTaskAsynchronously(this, () -> {
                    backupTask.run();
                    lastBackupTime = Instant.now();
                    sender.sendMessage("§aBackup complete.");
                });
            }
            case "last" -> {
                if (lastBackupTime == null) {
                    sender.sendMessage("§eNo backup has been run yet.");
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault());
                    sender.sendMessage("§aLast backup: §f" + formatter.format(lastBackupTime));
                }
            }
            case "reload" -> {
                reloadConfig();
                config = getConfig();
                sender.sendMessage("§avenom-local-backup config reloaded.");
            }
            case "status" -> {
                sender.sendMessage("§e--- Backup Status ---");
            
                if (lastBackupTime == null) {
                    sender.sendMessage("§7Last backup: §cNever");
                } else {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                            .withZone(ZoneId.systemDefault());
                    sender.sendMessage("§7Last backup: §a" + formatter.format(lastBackupTime));
                }
            
                long ticks = getTicksUntilNextBackup();
                long seconds = ticks / 20;
                long minutes = seconds / 60;
                long remainSeconds = seconds % 60;
            
                sender.sendMessage("§7Next backup in: §b" + minutes + "m " + remainSeconds + "s");
            }
            default -> sender.sendMessage("§cUsage: /backup <now|last|reload>");
        }

        return true;
    }

    public long getTicksUntilNextBackup() {
        return Math.max(0, nextBackupTick - getServer().getCurrentTick());
    }
}
