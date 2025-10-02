package com.atikinbtw.velocitycoollist;

import com.atikinbtw.velocitycoollist.database.WhitelistRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Whitelist {
    private static Whitelist INSTANCE;
    private final VelocityCoolList plugin;
    private final Path whitelistPath;
    private WhitelistRepository repository;
    
    // Для обратной совместимости с существующим кодом
    @Getter
    private List<String> whitelist = new ArrayList<>();

    public Whitelist(VelocityCoolList plugin) {
        this.plugin = plugin;
        this.whitelistPath = Path.of(plugin.DATADIRECTORY + "/whitelist.json");
        this.repository = new WhitelistRepository(plugin);
        INSTANCE = this;
    }

    public static Whitelist getInstance() {
        if (INSTANCE == null)
            throw new IllegalStateException("Config has not been initialized");

        return INSTANCE;
    }

    public void reload() {
        // Обновляем локальный список для обратной совместимости
        this.whitelist = repository.getAllPlayers();
    }

    private void loadWhitelist() {
        Gson gson = new Gson();
        try {
            if (!Files.exists(whitelistPath)) {
                Files.createFile(whitelistPath);
                Files.writeString(whitelistPath, "[]");
            }

            try (Reader reader = new InputStreamReader(whitelistPath.toUri().toURL().openStream())) {
                TypeToken<List<String>> typeToken = new TypeToken<List<String>>() {};
                whitelist = gson.fromJson(reader, typeToken.getType());
            }
        } catch (IOException e) {
            VelocityCoolList.LOGGER.error("Error happened while loading whitelist: ", e);
        }
    }

    public boolean isWhitelistEmpty() {
        return repository.isEmpty();
    }

    public void clear() {
        if (repository.clear()) {
            this.whitelist.clear();
        }
    }

    public void removePlayer(String nickname) {
        if (repository.removePlayer(nickname)) {
            // Обновляем локальный список для обратной совместимости
            String lowercaseNickname = nickname.toLowerCase();
            this.whitelist.removeIf(player -> player.toLowerCase().equals(lowercaseNickname));
        }
    }

    public void addPlayer(String nickname) {
        if (repository.addPlayer(nickname)) {
            // Обновляем локальный список для обратной совместимости
            String lowercaseNickname = nickname.toLowerCase();
            boolean alreadyExists = this.whitelist.stream()
                    .anyMatch(player -> player.toLowerCase().equals(lowercaseNickname));
            
            if (!alreadyExists) {
                this.whitelist.add(nickname);
            }
        }
    }

    public boolean contains(String nickname) {
        return repository.contains(nickname);
    }

    public void saveFile() {
        // Метод оставлен для обратной совместимости, но теперь ничего не делает
        // так как сохранение происходит автоматически в базе данных
    }

    public void init() {
        VelocityCoolList.LOGGER.info("Loading whitelist...");

        if (!plugin.DATADIRECTORY.toFile().exists()) {
            try {
                plugin.DATADIRECTORY.toFile().mkdir();
            } catch (Exception e) {
                VelocityCoolList.LOGGER.error("Failed to create plugin data directory: ", e);
                return;
            }
        }

        // Инициализируем репозиторий базы данных
        repository.initialize();
        
        // Проверяем, нужна ли миграция с JSON
        if (Files.exists(whitelistPath)) {
            VelocityCoolList.LOGGER.info("Обнаружен старый whitelist.json, начинаем миграцию...");
            migrateFromJson();
        }
        
        // Обновляем локальный список для обратной совместимости
        this.whitelist = repository.getAllPlayers();
        VelocityCoolList.LOGGER.info("Whitelist загружен: {} игроков", whitelist.size());
    }
    
    private void migrateFromJson() {
        try {
            loadWhitelist(); // Загружаем старый JSON
            
            if (!whitelist.isEmpty()) {
                VelocityCoolList.LOGGER.info("Миграция {} игроков из JSON в базу данных...", whitelist.size());
                
                // Добавляем всех игроков в базу данных
                int migrated = 0;
                for (String player : whitelist) {
                    if (repository.addPlayer(player)) {
                        migrated++;
                    }
                }
                
                VelocityCoolList.LOGGER.info("Перенесено {} из {} игроков", migrated, whitelist.size());
                
                try {
                    // Создаем бэкап старого файла
                    Path backupPath = plugin.DATADIRECTORY.resolve("whitelist.json.backup");
                    Files.move(whitelistPath, backupPath);
                    VelocityCoolList.LOGGER.info("Миграция завершена успешно. Старый файл сохранен как whitelist.json.backup");
                } catch (IOException e) {
                    VelocityCoolList.LOGGER.error("Ошибка при создании бэкапа JSON файла: ", e);
                }
            }
        } catch (Exception e) {
            VelocityCoolList.LOGGER.error("Ошибка при миграции из JSON: ", e);
        }
    }
    
    public void shutdown() {
        if (repository != null) {
            repository.shutdown();
        }
    }
}