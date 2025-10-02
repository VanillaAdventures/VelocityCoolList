package com.atikinbtw.velocitycoollist.database;

public enum DatabaseType {
    SQLITE("sqlite"),
    MYSQL("mysql");
    
    private final String name;
    
    DatabaseType(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public static DatabaseType fromString(String name) {
        for (DatabaseType type : values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return SQLITE; // по умолчанию SQLite
    }
}
