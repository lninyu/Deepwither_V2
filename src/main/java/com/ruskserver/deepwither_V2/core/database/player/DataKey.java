package com.ruskserver.deepwither_V2.core.database.player;

import java.util.Objects;

/**
 * プレイヤーデータの要素に型安全にアクセスするためのキー。
 * @param <T> このキーに紐づくデータの型
 */
public class DataKey<T> {
    private final String id;

    public DataKey(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataKey<?> dataKey = (DataKey<?>) o;
        return Objects.equals(id, dataKey.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
