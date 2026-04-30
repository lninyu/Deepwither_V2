package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.database.player.PlayerData;
import com.ruskserver.deepwither_V2.core.database.player.PlayerDataRepository;
import com.ruskserver.deepwither_V2.core.di.annotations.Inject;
import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.UUID;
import java.util.logging.Logger;

@Service
public class TestService implements Startable, Stoppable {

    private final Logger logger;
    private final PlayerDataRepository repository;

    // @Inject is required when we have constructor parameters for Dependency Injection
    @Inject
    public TestService(JavaPlugin plugin, PlayerDataRepository repository) {
        this.logger = plugin.getLogger();
        this.repository = repository;
    }

    @Override
    public void start() {
        logger.info("[TestService] サービスが開始されました。DIコンテナは正常に機能しています！");
    }

    @Override
    public void stop() {
        logger.info("[TestService] サービスが停止されました。");
    }

    public void doSomething() {
        logger.info("[TestService] ビジネスロジックが実行されました。");
        
        // PlayerDataRepository test
        UUID testUuid = UUID.randomUUID();
        
        logger.info("[TestService] プレイヤーデータをロードします...");
        repository.get(testUuid).ifPresent(data -> {
            
            // データ取得（型安全！）
            String testValue = data.get(TestDataProvider.KEY);
            logger.info("[TestService] ロードされた初期値: " + testValue);
            
            // データの更新とDirtyフラグの設定
            String newValue = "Updated at " + System.currentTimeMillis();
            data.set(TestDataProvider.KEY, newValue);
            logger.info("[TestService] 値を更新しました: " + newValue);
            
            // 保存（DirtyなプロバイダーのみsaveToDbが呼ばれる）
            repository.save(testUuid, data);
            logger.info("[TestService] プレイヤーデータを保存しました！");
            
        });
    }
}
