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
import java.util.concurrent.CompletableFuture;

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
        repository.refreshCache();
        // Обновляем кеш для обратной совместимости
        repository.getAllPlayers().thenAccept(players -> {
            this.whitelist = players;
        });
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
        repository.clear().thenAccept(success -> {
            if (success) {
                this.whitelist.clear();
            }
        });
    }

    public void removePlayer(String nickname) {
        repository.removePlayer(nickname).thenAccept(success -> {
            if (success) {
                // Обновляем локальный кеш для обратной совместимости
                String lowercaseNickname = nickname.toLowerCase();
                this.whitelist.removeIf(player -> player.toLowerCase().equals(lowercaseNickname));
            }
        });
    }

    public void addPlayer(String nickname) {
        repository.addPlayer(nickname).thenAccept(success -> {
            if (success) {
                // Обновляем локальный кеш для обратной совместимости
                String lowercaseNickname = nickname.toLowerCase();
                boolean alreadyExists = this.whitelist.stream()
                        .anyMatch(player -> player.toLowerCase().equals(lowercaseNickname));
                
                if (!alreadyExists) {
                    this.whitelist.add(nickname);
                }
            }
        });
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
        
        // Обновляем локальный кеш для обратной совместимости
        repository.getAllPlayers().thenAccept(players -> {
            this.whitelist = players;
            VelocityCoolList.LOGGER.info("Whitelist загружен: {} игроков", players.size());
        });
    }
    
    private void migrateFromJson() {
        try {
            loadWhitelist(); // Загружаем старый JSON
            
            if (!whitelist.isEmpty()) {
                VelocityCoolList.LOGGER.info("Миграция {} игроков из JSON в базу данных...", whitelist.size());
                
                // Добавляем всех игроков в базу данных
                @SuppressWarnings("unchecked")
                CompletableFuture<Boolean>[] futures = whitelist.stream()
                        .map(player -> repository.addPlayer(player))
                        .toArray(CompletableFuture[]::new);
                
                CompletableFuture.allOf(futures).thenRun(() -> {
                    try {
                        // Создаем бэкап старого файла
                        Path backupPath = plugin.DATADIRECTORY.resolve("whitelist.json.backup");
                        Files.move(whitelistPath, backupPath);
                        VelocityCoolList.LOGGER.info("Миграция завершена успешно. Старый файл сохранен как whitelist.json.backup");
                    } catch (IOException e) {
                        VelocityCoolList.LOGGER.error("Ошибка при создании бэкапа JSON файла: ", e);
                    }
                }).exceptionally(throwable -> {
                    VelocityCoolList.LOGGER.error("Ошибка при миграции whitelist: ", throwable);
                    return null;
                });
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