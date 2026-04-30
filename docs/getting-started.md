# Deepwither_V2 開発ガイド: 新規モジュールの作り方

このドキュメントでは、Deepwither_V2の「独自のDIコンテナ」および「モジュラーモノリスアーキテクチャ」を用いた新規機能（モジュール）の作成手順を詳細に解説します。

## 1. アーキテクチャの基本概念

Deepwither_V2では、巨大な外部フレームワークに依存せず、Minecraftプラグインに最適化された軽量な独自のDI（依存性注入）コンテナを採用しています。
各機能は完全に独立した「モジュール」として分割され、疎結合な設計を保ちます。

* **DIコンテナ**: クラス間の依存関係（どのクラスがどのクラスを必要とするか）を自動で解決し、インスタンスを生成・管理・注入するシステムです。
* **シングルトン**: コンテナが管理するインスタンスは、サーバー稼働中に1つだけ生成され、すべてのクラスで共有されます。

## 2. モジュールのディレクトリ構成

新しい機能を作る際は、`com.ruskserver.deepwither_V2.modules` の直下に機能名のパッケージを作成し、その中にすべての関連クラスを配置します。

```text
com.ruskserver.deepwither_V2.modules.your_feature/
 ├── YourService.java   (ビジネスロジック・データ管理)
 ├── YourListener.java  (Bukkitイベントの購読)
 └── YourCommand.java   (プレイヤーからのコマンド受付)
```

## 3. アノテーションの種類

DIコンテナにクラスを認識させるため、クラス宣言の上に以下のいずれかのアノテーションを付与します。

* `@Service`: メインのロジックや状態を保持するサービスクラスに使用。
* `@Component`: イベントリスナーなど、汎用的なコンポーネントに使用。
* `@Repository`: データベースなどの永続化処理を行うクラスに使用。
* `@Command`: コマンドクラス専用。引数として名前やエイリアスを指定します。
* `@Ignore`: スキャン対象から意図的に除外したいクラス（書きかけの機能など）に使用します。

## 4. ステップバイステップ：新規機能の作り方

ここでは、「プレイヤーがブロックを壊した回数をカウントする機能」を例に解説します。

### ステップ 1: サービスの作成
ビジネスロジックを担当するクラスを作成します。プラグイン起動時・終了時に処理が必要な場合は `Startable`, `Stoppable` インターフェースを実装します。

```java
package com.ruskserver.deepwither_V2.modules.blockcounter;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;

@Service // DIコンテナの管理対象にする
public class BlockCounterService implements Startable, Stoppable {

    private int totalBlocksBroken = 0;

    @Override
    public void start() {
        // プラグイン起動時の処理（データのロードなど）
    }

    @Override
    public void stop() {
        // プラグイン終了時の処理（データの保存など）
    }

    public void incrementCount() {
        totalBlocksBroken++;
    }

    public int getCount() {
        return totalBlocksBroken;
    }
}
```

### ステップ 2: イベントリスナーの作成
Bukkitのイベントを検知するリスナーを作成します。**リスナーを手動で登録する必要はありません。** `@Component` がついていれば自動で登録されます。
他のクラスの機能（Serviceなど）を使いたい場合は、**コンストラクタ**経由で注入（DI）してもらいます。

```java
package com.ruskserver.deepwither_V2.modules.blockcounter;

import com.ruskserver.deepwither_V2.core.di.annotations.Component;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

@Component // 自動的にListenerとして登録されます
public class BlockBreakListener implements Listener {

    private final BlockCounterService counterService;

    @Inject // コンストラクタインジェクションを指定
    public BlockBreakListener(BlockCounterService counterService) {
        this.counterService = counterService;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        // イベント発生時にServiceのメソッドを呼び出す
        counterService.incrementCount();
    }
}
```

### ステップ 3: コマンドの作成
プレイヤーが実行できるコマンドを作成します。Paperの最新APIに準拠するため、`BasicCommand` インターフェースを実装します。**`paper-plugin.yml` への追記は不要です。**

```java
package com.ruskserver.deepwither_V2.modules.blockcounter;

import com.ruskserver.deepwither_V2.core.di.annotations.Command;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

// コマンド名とエイリアスをここで定義します
@Command(name = "blockcount", aliases = {"bc", "blocks"}, description = "壊したブロック数を確認")
public class BlockCountCommand implements BasicCommand {

    private final BlockCounterService counterService;

    @Inject
    public BlockCountCommand(BlockCounterService counterService) {
        this.counterService = counterService;
    }

    @Override
    public void execute(@NotNull CommandSourceStack stack, @NotNull String[] args) {
        if (stack.getSender() instanceof Player) {
            Player player = (Player) stack.getSender();
            player.sendMessage("§aサーバー全体で破壊されたブロック数: " + counterService.getCount());
        }
    }
}
```

## 5. 重要なルールと注意点

> [!WARNING]
> **1. フィールドインジェクションの禁止**
> 依存関係の注入は必ず**コンストラクタ**で行ってください。本プロジェクトでは設計の明確化と安全性の確保のため、フィールドインジェクション（フィールドに直接 `@Inject` を付けること）はサポートしていません。

> [!WARNING]
> **2. 循環参照の回避**
> クラスAがクラスBを要求し、クラスBがクラスAを要求するような状態（循環参照）になると、DIコンテナがインスタンスを生成できず起動時に `CircularDependencyException` エラーでクラッシュします。エラーログには `ServiceA -> ServiceB -> ServiceA` のように正確な経路（トレース）が出力されるので、これを見て共通の処理を別のクラス（Service C等）に切り出すなど、設計をリファクタリングしてください。

> [!NOTE]
> **JavaPlugin インスタンスの取得について**
> プラグインのメインインスタンス（`JavaPlugin`）や `Logger` が必要な場合は、コンストラクタの引数に `JavaPlugin` クラス（または `Deepwither_V2` クラス）を含めることで、DIコンテナから自動的に注入されます。

## 6. 開発用デバッグ機能

DIコンテナには開発をサポートする強力なデバッグ機能が搭載されています（開発環境では `DIContainer.setDebugMode(true)` が設定されています）。

### 起動時の依存関係ツリー出力
プラグインが起動した際、コンソールにすべてのコンポーネントの依存関係がツリー状にログ出力されます。「どのクラスがどの順序でロードされ、何に依存しているか」が一目で分かります。

```text
[Deepwither_V2] === [DI Tree] Registered Components ===
[Deepwither_V2] ├── TestCommand
[Deepwither_V2] │   └── TestService
[Deepwither_V2] │       └── Deepwither_V2
[Deepwither_V2] └── TestListener
[Deepwither_V2]     └── TestService
[Deepwither_V2]         └── Deepwither_V2
[Deepwither_V2] =======================================
```

### スキャンの詳細ログとエラー出力
デバッグモードがONの場合、「どのクラスが検出されたか」「なぜクラスのロードに失敗したか（欠損しているライブラリなど）」がコンソールに出力されるため、「クラスを作ったのに認識されない」といったトラブルシューティングが容易になります。
