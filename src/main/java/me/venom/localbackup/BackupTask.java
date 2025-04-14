package me.venom.localbackup;

import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class BackupTask implements Runnable {

    private final VenomBackup plugin;

    public BackupTask(VenomBackup plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        FileConfiguration config = plugin.getBackupConfig();
        String backupDir = config.getString("backup.destination", "backups");
        List<String> folders = config.getStringList("backup.folders");
        List<String> files = config.getStringList("backup.files");

        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        File backupBase = new File(plugin.getDataFolder().getParentFile().getParent(), backupDir + "/" + timestamp);

        if (!backupBase.exists()) backupBase.mkdirs();

        try {
            for (String folderPath : folders) {
                Path source = Paths.get(folderPath);
                Path dest = backupBase.toPath().resolve(source.getFileName());
                if (Files.exists(source)) {
                    copyFolder(source, dest);
                }
            }

            for (String filePath : files) {
                Path source = Paths.get(filePath);
                if (Files.exists(source)) {
                    Files.copy(source, backupBase.toPath().resolve(source.getFileName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            plugin.setLastBackupTime(Instant.now());

        } catch (IOException e) {
            plugin.getLogger().warning("Backup failed: " + e.getMessage());
        }
    }

    private void copyFolder(Path src, Path dest) throws IOException {
        Files.walk(src).forEach(source -> {
            try {
                Path destination = dest.resolve(src.relativize(source));
                if (Files.isDirectory(source)) {
                    if (!Files.exists(destination)) Files.createDirectory(destination);
                } else {
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Error copying folder: " + e.getMessage());
            }
        });
    }
}
