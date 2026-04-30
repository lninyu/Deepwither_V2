package com.ruskserver.deepwither_V2.modules.test;

import com.ruskserver.deepwither_V2.core.di.annotations.Service;
import com.ruskserver.deepwither_V2.core.lifecycle.Startable;
import com.ruskserver.deepwither_V2.core.lifecycle.Stoppable;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

@Service
public class TestService implements Startable, Stoppable {

    private final Logger logger;

    // Default constructor is picked up if no @Inject is found,
    // but we can inject JavaPlugin here to get the logger.
    public TestService(JavaPlugin plugin) {
        this.logger = plugin.getLogger();
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
    }
}
