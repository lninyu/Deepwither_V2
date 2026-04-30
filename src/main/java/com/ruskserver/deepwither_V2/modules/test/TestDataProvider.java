package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Component
public class TestDataProvider implements PlayerDataProvider<String> {

    public static final DataKey<String> KEY = new DataKey<>("test_data");

    @Override
    public DataKey<String> getKey() {
        return KEY;
    }

    @Override
    public String loadFromDb(UUID uuid, Connection conn) throws Exception {
        // テーブル作成（本来はStartableなどで1回だけやるべきだがテストなのでここで簡易的に）
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS test_player_data (uuid VARCHAR(36) PRIMARY KEY, value_data TEXT)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement(
                "SELECT value_data FROM test_player_data WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("value_data");
                }
            }
        }
        return "Default Data for " + uuid.toString().substring(0, 5); // 初期値
    }

    @Override
    public void saveToDb(UUID uuid, String data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO test_player_data (uuid, value_data) KEY(uuid) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, data);
            stmt.executeUpdate();
        }
    }
}
