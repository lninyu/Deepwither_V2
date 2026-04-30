# 1ファイル完結型モジュールの作り方 (Single-File Module Example)

Deepwither_V2のDIコンテナとプレイヤーデータ集約レイヤーを使うと、通常なら複数のファイルに分割しなければならない機能（データ定義、プロバイダー、サービス、リスナー、コマンド）を **1つの `.java` ファイルにまとめる** ことが可能です。

これは小規模〜中規模の機能をサクッと追加したい場合に非常に便利です。Javaでは1つのファイルに `public` なクラスは1つしか置けませんが、アクセス修飾子を省略した（パッケージプライベートな）クラスであれば、いくつでもトップレベルクラスとして定義でき、DIコンテナはそれらも漏れなくスキャンしてくれます。

## サンプルコード: CoinModule.java

以下は、「プレイヤーがブロックを壊すと確率でコインを獲得し、コマンドで自分のコインを確認できる」という完全な機能を1ファイルで実装した例です。
これを `src/main/java/com/ruskserver/deepwither_V2/modules/coin/CoinModule.java` にコピー＆ペーストするだけで、即座に機能がサーバーに組み込まれます。

```java
package com.ruskserver.deepwither_V2.modules.coin;

import com.ruskserver.deepwither_V2.core.database.player.DataKey;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataProvider;
import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 1. データ定義 (POJO)
 * プレイヤーのコインデータを保持する単純なクラス
 */
class CoinData {
    int coins = 0;
}

/**
 * 2. プロバイダー (@Component)
 * DIコンテナに自動収集され、データベースとのやり取りを担当
 */
@Component
class CoinDataProvider implements PlayerDataProvider<CoinData> {
    public static final DataKey<CoinData> KEY = new DataKey<>("coin_data");

    @Override
    public DataKey<CoinData> getKey() { return KEY; }

    @Override
    public CoinData loadFromDb(UUID uuid, Connection conn) throws Exception {
        // 本来はStartableなどでテーブル作成を分離推奨
        try (PreparedStatement stmt = conn.prepareStatement(
                "CREATE TABLE IF NOT EXISTS player_coins (uuid VARCHAR(36) PRIMARY KEY, coins INT)")) {
            stmt.execute();
        }

        CoinData data = new CoinData();
        try (PreparedStatement stmt = conn.prepareStatement("SELECT coins FROM player_coins WHERE uuid = ?")) {
            stmt.setString(1, uuid.toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    data.coins = rs.getInt("coins");
                }
            }
        }
        return data;
    }

    @Override
    public void saveToDb(UUID uuid, CoinData data, Connection conn) throws Exception {
        try (PreparedStatement stmt = conn.prepareStatement(
                "MERGE INTO player_coins (uuid, coins) KEY(uuid) VALUES (?, ?)")) {
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, data.coins);
            stmt.executeUpdate();
        }
    }
}

/**
 * 3. サービス (@Service)
 * コインを増やすなどのメインロジックを担当するクラス
 */
@Service
class CoinService {
    private final PlayerDataRepository repository;

    @Inject
    public CoinService(PlayerDataRepository repository) {
        this.repository = repository;
    }

    public void addCoins(Player player, int amount) {
        repository.get(player.getUniqueId()).ifPresent(data -> {
            CoinData coinData = data.get(CoinDataProvider.KEY);
            coinData.coins += amount;
            
            // オブジェクトの内部プロパティを直接書き換えたため、明示的にDirtyフラグを立てる
            data.markDirty(CoinDataProvider.KEY);
            repository.save(player.getUniqueId(), data);
            
            player.sendMessage("§e" + amount + " コイン獲得しました！ (合計: " + coinData.coins + "コイン)");
        });
    }
}

/**
 * 4. リスナー (@Component)
 * ブロック破壊イベントを監視し、Serviceを呼び出す
 */
@Component
class CoinListener implements Listener {
    private final CoinService coinService;

    @Inject
    public CoinListener(CoinService coinService) {
        this.coinService = coinService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // 10%の確率でコインを1枚獲得
        if (ThreadLocalRandom.current().nextDouble() < 0.10) {
            coinService.addCoins(event.getPlayer(), 1);
        }
    }
}

/**
 * 5. コマンド (@Command)
 * プレイヤーが /coins と打った時の処理
 */
@Command(name = "coins", description = "自分の所持コインを確認します")
class CoinCommand implements BasicCommand {
    private final PlayerDataRepository repository;

    @Inject
    public CoinCommand(PlayerDataRepository repository) {
        this.repository = repository;
    }

    @Override
    public void execute(CommandSourceStack stack, String[] args) {
        if (stack.getSender() instanceof Player player) {
            repository.get(player.getUniqueId()).ifPresent(data -> {
                int currentCoins = data.get(CoinDataProvider.KEY).coins;
                player.sendMessage("§e現在の所持コイン: " + currentCoins + "枚");
            });
        }
    }
}

/**
 * パブリッククラス (空でOK)
 * Javaのファイル名制約を満たすためのダミークラス。
 * モジュールの初期化処理などがあれば Startable を実装してここに書くのもアリです。
 */
public class CoinModule {
}
```

## このアプローチのメリット
1. **圧倒的なスピード**: 新しい機能を思いついたとき、ファイルをポンポン行ったり来たりせず、上から下へ一気に書き上げることができます。
2. **スキャン対象**: Javaのコンパイラは `class CoinService` のようなパッケージプライベートクラスも独立した `.class` ファイルとして出力するため、`ClassScanner` はこれらを問題なく検知し、DIコンテナに登録します。
3. **リファクタリングが容易**: コードが長くなってきたら、IDEの機能を使って簡単に別ファイルへクラスを抽出（Extract Class）できます。小規模なうちは1ファイルで、育ってきたら分離するというアジャイルな開発が可能です。
