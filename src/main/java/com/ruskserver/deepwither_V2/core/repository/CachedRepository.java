package com.ruskserver.deepwither_V2.core.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.ruskserver.deepwither_V2.core.database.DatabaseManager;

import java.util.Optional;

/**
 * データベースアクセスとCaffeineによるインメモリキャッシュを統合する抽象リポジトリクラス。
 * K = Key (e.g. UUID, String)
 * V = Value (e.g. PlayerData)
 */
public abstract class CachedRepository<K, V> {

    protected final DatabaseManager db;
    protected final Cache<K, V> cache;

    public CachedRepository(DatabaseManager db) {
        this.db = db;
        this.cache = buildCache();
    }

    /**
     * サブクラスでCaffeineキャッシュの設定（有効期限、最大サイズ等）を行います。
     * 例: return Caffeine.newBuilder().maximumSize(1000).expireAfterAccess(10, TimeUnit.MINUTES).build();
     */
    protected abstract Cache<K, V> buildCache();

    /**
     * データベースから直接読み込むロジックを実装します。
     * キャッシュミスした際に呼び出されます。
     */
    protected abstract Optional<V> loadFromDb(K key);

    /**
     * データベースへ保存・更新するロジックを実装します。
     */
    protected abstract void saveToDb(K key, V value);

    /**
     * キャッシュから値を取得します。存在しない場合はデータベースから読み込み、キャッシュに保存します。
     */
    public Optional<V> get(K key) {
        // Caffeine's get method provides an atomic compute-if-absent functionality
        V value = cache.get(key, k -> loadFromDb(k).orElse(null));
        return Optional.ofNullable(value);
    }

    /**
     * データをキャッシュに反映しつつ、非同期（または同期）でデータベースに保存します。
     */
    public void save(K key, V value) {
        cache.put(key, value);
        // デフォルトでは同期的に保存しますが、サブクラス側で BukkitScheduler を用いて
        // 非同期にオーバーライドすることも可能です。
        saveToDb(key, value);
    }

    /**
     * キャッシュからのみデータを削除します。データベースの削除ではありません。
     */
    public void invalidateCache(K key) {
        cache.invalidate(key);
    }
    
    /**
     * キャッシュとデータベースの両方から削除するロジックを実装します（オプション）。
     */
    public void delete(K key) {
        cache.invalidate(key);
        deleteFromDb(key);
    }
    
    /**
     * データベースから削除するロジックを実装します。
     */
    protected void deleteFromDb(K key) {
        // デフォルト実装は空。必要に応じてサブクラスでオーバーライド。
    }
}
