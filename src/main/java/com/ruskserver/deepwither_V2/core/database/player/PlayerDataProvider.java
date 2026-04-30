package com.ruskserver.deepwither_V2.core.database.player;

import java.sql.Connection;
import java.util.UUID;

/**
 * プレイヤーデータの一部を提供するモジュール用インターフェース。
 * 各モジュールでこれを実装し、@Component を付与することで、起動時に自動収集されます。
 * @param <T> 提供するデータの型
 */
public interface PlayerDataProvider<T> {

    /**
     * このプロバイダーが担当するデータのキーを返します。
     */
    DataKey<T> getKey();

    /**
     * データベースから指定したプレイヤーのデータを読み込みます。
     * （データが存在しない場合は初期データを返します）
     */
    T loadFromDb(UUID uuid, Connection conn) throws Exception;

    /**
     * データベースへ指定したプレイヤーのデータを保存（または更新）します。
     * ※このメソッドは、対象の DataKey の DirtyFlag が立っている場合にのみ呼ばれます。
     */
    void saveToDb(UUID uuid, T data, Connection conn) throws Exception;
}
