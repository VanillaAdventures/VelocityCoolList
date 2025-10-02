package com.atikinbtw.velocitycoollist;

import com.moandjiezana.toml.Toml;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class Migration {
    private final Config config;
    private final VelocityCoolList plugin;
    private final YamlFile configFile;

    public Migration(VelocityCoolList plugin, YamlFile config) {
        this.config = Config.getInstance();
        this.plugin = plugin;
        this.configFile = config;
    }

    public void migrateIfNeeded() {
        if (Path.of(plugin.DATADIRECTORY + "/config.toml").toFile().exists()) {
            migrateOldTomlConfig();
            return;
        }

        switch (config.getInt("config_version")) {
            case 1 -> firstToSecondVerMigration();
            case 2 -> secondToThirdVerMigration();
            case 3 -> {
            }
            default -> VelocityCoolList.LOGGER.error("Unknown config version: {}", config.getInt("config_version"));
        }
    }

    private void firstToSecondVerMigration() {
        plugin.scheduleTask(() -> {
            boolean enabled;
            String prefix;
            boolean enableClearCommand;

            enabled = config.getBoolean("enabled");
            prefix = config.getString("prefix");
            enableClearCommand = config.getBoolean("enable_clear_command");

            try (InputStream resource = VelocityCoolList.class.getResourceAsStream("/config.yml")) {
                Files.copy(resource, Path.of(configFile.getFilePath()), StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                VelocityCoolList.LOGGER.error("Error happened while creating config.yml: ", e);
                return;
            }

            config.reload();

            configFile.set("enabled", enabled);
            configFile.set("prefix", prefix);
            configFile.set("enable_clear_command", enableClearCommand);

            config.saveConfigFile();

            config.reload();

            VelocityCoolList.LOGGER.info("Migration completed!");
        });
    }

    public void migrateOldTomlConfig() {
        plugin.scheduleTask(() -> {
            final Path oldConfigPath = Path.of(plugin.DATADIRECTORY + "/config.toml");

            Toml toml;
            try {
                toml = new Toml().read(oldConfigPath.toFile());
            } catch (IllegalStateException e) {
                VelocityCoolList.LOGGER.error("The old config is broken, can't migrate automatically");
                return;
            }

            if (toml.getBoolean("enabled") == null || toml.getString("message") == null || toml.getString("prefix") == null) {
                VelocityCoolList.LOGGER.error("The old config is broken, can't migrate automatically");
                return;
            }

            configFile.set("enabled", toml.getBoolean("enabled"));
            config.setMessage("kick_message", toml.getString("message"));
            configFile.set("prefix", toml.getString("prefix"));

            config.saveConfigFile();
            config.saveMessages();

            oldConfigPath.toFile().delete();

            config.reload();

            VelocityCoolList.LOGGER.info("Migration completed!");
        });
    }
    
    private void secondToThirdVerMigration() {
        plugin.scheduleTask(() -> {
            VelocityCoolList.LOGGER.info("Миграция конфигурации с версии 2 на версию 3...");
            
            // Добавляем настройки базы данных
            configFile.set("database.type", "sqlite");
            configFile.set("database.mysql.host", "localhost");
            configFile.set("database.mysql.port", 3306);
            configFile.set("database.mysql.database", "velocitycoollist");
            configFile.set("database.mysql.username", "root");
            configFile.set("database.mysql.password", "password");
            
            // Обновляем версию конфигурации
            configFile.set("config_version", 3);
            
            config.saveConfigFile();
            config.reload();
            
            VelocityCoolList.LOGGER.info("Миграция конфигурации завершена!");
        });
    }
}