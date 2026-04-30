# Deepwither_V2 データベース＆キャッシュ開発ガイド

このドキュメントでは、Deepwither_V2に導入されている「データベース層（H2 + HikariCP）」および「キャッシュ層（Caffeine）」を利用したデータ永続化と高速化の仕組みについて解説します。

## 1. アーキテクチャの概要

本プラグインでは、データの読み書き速度を最大化し、かつモジュール間の結合度を下げるため、強力な**「集約リポジトリパターン」**を採用しています。

* **H2 Database**: ローカルファイル（`plugins/Deepwither_V2/database/data.db`）に保存される軽量データベース。
* **HikariCP**: DB接続プールを管理し、非同期や多重アクセス時のパフォーマンスを最適化。
* **Caffeine Cache**: Paper標準の超高速インメモリキャッシュ。
* **PlayerData 集約レイヤー**: プレイヤーに紐づくすべてのデータ（レベル、所持金など）を1つの `PlayerData` オブジェクトに集約。各モジュールはプロバイダーを提供するだけで自動連携され、変更があったテーブル（DirtyFlag）だけがピンポイントで保存される極めて高パフォーマンスな設計です。

---

## 2. プレイヤーデータの作り方 (`PlayerDataProvider`)

例として、「プレイヤーのレベル」を保存するモジュールを作ってみましょう。
各モジュールは、リポジトリを自作するのではなく、**「データのキー」と「読み書きの方法」を定義した `PlayerDataProvider` を作成**します。

### ステップ 1: プロバイダークラスの作成

```java
package com.ruskserver.deepwither_V2.modules.level;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

@Component // 自動収集の対象にする
public class LevelDataProvider implements PlayerDataProvider<Integer> {

    // 1. このデータにアクセスするための型安全なキーを定義（型はInteger）
    public static final DataKey<Integer> KEY = new DataKey<>("player_level");

    @Override
    public DataKey<Integer> getKey() {
        return KEY;
    }

    // 2. データベースから読み込む処理
    @Override
    public Integer loadFromDb(UUID uuid, Connection conn) throws Exception {
        // テーブルがなければ作成（※本来はStartable等で1回だけ行うのが理想です）
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_levels (uuid VARCHAR(36) PRIMARY KEY, level INT)")) {
            stmt.execute();
        }

        try (PreparedStatement stmt = conn.prepareStatement("SELECT level FROM player_levels WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("level");
                }
            }
        }
        return 1; // データがない場合の初期値
    }

    // 3. データベースへ保存する処理
    @Override
    public void saveToDb(UUID uuid, Integer data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_levels (uuid, level) KEY(uuid) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, data);
            stmt.executeUpdate();
        }
    }
}
```

---

## 3. プレイヤーデータの使い方 (`PlayerDataRepository`)

作成したプロバイダーは起動時に自動で収集されます。
実際のサービス（ビジネスロジック）クラスからは、中央集約リポジトリである `PlayerDataRepository` を呼び出して使います。

```java
package com.ruskserver.deepwither_V2.modules.level;

import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import org.bukkit.entity.Player;

import java.util.UUID;

@Service
public class LevelService {

    private final PlayerDataRepository repository;

    @Inject
    public LevelService(PlayerDataRepository repository) {
        this.repository = repository;
    }

    public void levelUp(Player player) {
        UUID uuid = player.getUniqueId();
        
        // 1. プレイヤーの全データを取得（キャッシュになければ全プロバイダーから自動ロード）
        repository.get(uuid).ifPresent(data -> {
            
            // 2. DataKeyを使って型安全に値を取得
            int currentLevel = data.get(LevelDataProvider.KEY);
            int newLevel = currentLevel + 1;
            
            // 3. 値を更新（この瞬間、LevelデータのDirtyフラグが立つ）
            data.set(LevelDataProvider.KEY, newLevel);
            
            // 4. 保存（Dirtyフラグが立っているLevelDataProviderのsaveToDbだけが実行される！）
            repository.save(uuid, data);
            
            player.sendMessage("レベルが " + newLevel + " に上がりました！");
        });
    }
}
```

---

## 4. 一般データのリポジトリ作成 (`CachedRepository`)

プレイヤーに紐づかない全体データ（例: 町のデータ、グローバル経済、設定値など）については、これまで通りベースとなる `CachedRepository<K, V>` を直接継承して作成します。

```java
@Repository
public class TownRepository extends CachedRepository<String, TownData> {
    // ... loadFromDb や saveToDb などを実装
}
```

---

## 5. 開発時の重要なポイント

> [!TIP]
> **DirtyFlag による超最適化**
> `repository.save(uuid, data)` を呼んだ際、DIコンテナはすべてのプロバイダーをスキャンしますが、実際に `saveToDb()` が実行されるのは **`data.set()` で値が更新されたプロバイダーのみ** です。
> これにより、「レベルだけが上がったのに、所持金やスキルのテーブルまで無駄にUPDATEされる」といったパフォーマンス低下を完全に防ぎます。

> [!WARNING]
> **オブジェクト内部の変更に注意**
> `data.set(KEY, 10)` のように新しい値をセットした場合は自動でDirtyフラグが立ちますが、
> `data.get(KEY).addMoney(100)` のように**取得したオブジェクトの中身を直接書き換えた場合**は、フレームワーク側で変更を検知できません。
> この場合は、明示的に `data.markDirty(KEY)` を呼んで保存対象であることを知らせてください。

> [!WARNING]
> **H2のMERGE文の活用**
> データを保存・更新する `saveToDb()` では、H2特有の `MERGE INTO` 文を活用しています。
> `MERGE INTO table_name (keys...) KEY(primary_key) VALUES (values...)`
> これを使うことで、「データがなければINSERT、あればUPDATE」という処理（Upsert）を1行で安全に記述できます。
