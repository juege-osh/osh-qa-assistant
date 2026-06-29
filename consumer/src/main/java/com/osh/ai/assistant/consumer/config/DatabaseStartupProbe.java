package com.osh.ai.assistant.consumer.config;

import com.osh.ai.assistant.consumer.config.properties.DbProbeProperties;
import com.osh.ai.assistant.consumer.service.AlertService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class DatabaseStartupProbe {

    private static final String ALERT_KEY = "startup-db-unreachable";

    private final DataSource dataSource;
    private final ExecutorService executorService;
    private final DbProbeProperties dbProbeProperties;
    private final AlertService alertService;

    @Value("${spring.datasource.url:unknown}")
    private String datasourceUrl;

    @Value("${spring.datasource.username:unknown}")
    private String datasourceUsername;

    private volatile boolean running;

    public DatabaseStartupProbe(DataSource dataSource,
                                ExecutorService executorService,
                                DbProbeProperties dbProbeProperties,
                                AlertService alertService) {
        this.dataSource = dataSource;
        this.executorService = executorService;
        this.dbProbeProperties = dbProbeProperties;
        this.alertService = alertService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!dbProbeProperties.isEnabled() || running) {
            return;
        }
        running = true;
        executorService.submit(this::probeUntilConnected);
    }

    @PreDestroy
    public void stop() {
        running = false;
    }

    private void probeUntilConnected() {
        long startNanos = System.nanoTime();
        long warnAfterNanos = TimeUnit.SECONDS.toNanos(Math.max(0, dbProbeProperties.getWarnAfterSeconds()));
        long sleepMillis = TimeUnit.SECONDS.toMillis(Math.max(1, dbProbeProperties.getRetryIntervalSeconds()));

        while (running && !Thread.currentThread().isInterrupted()) {
            try (Connection connection = dataSource.getConnection()) {
                int timeoutSeconds = Math.max(1, dbProbeProperties.getCheckTimeoutSeconds());
                if (!connection.isValid(timeoutSeconds)) {
                    throw new IllegalStateException("database connection is invalid");
                }
                long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos);
                log.info("Database connection is ready, url={}, username={}, elapsed={}s",
                    datasourceUrl, datasourceUsername, elapsedSeconds);
                alertService.notifyResolved(ALERT_KEY, "数据库连接已恢复",
                    "数据库启动探针已恢复正常。\nURL: " + datasourceUrl + "\n用户名: " + datasourceUsername + "\n恢复耗时: " + elapsedSeconds + "s");
                return;
            } catch (Exception e) {
                long elapsedNanos = System.nanoTime() - startNanos;
                if (elapsedNanos >= warnAfterNanos) {
                    long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);
                    log.error("Database is still unreachable after {}s, url={}, nextRetry={}s",
                        elapsedSeconds, datasourceUrl, dbProbeProperties.getRetryIntervalSeconds(), e);
                    alertService.notifyOnce(ALERT_KEY, "数据库连接不可用",
                        "数据库启动探针检测到连接不可用。\nURL: " + datasourceUrl
                            + "\n用户名: " + datasourceUsername
                            + "\n持续时长: " + elapsedSeconds + "s"
                            + "\n下次重试间隔: " + dbProbeProperties.getRetryIntervalSeconds() + "s"
                            + "\n异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }
            sleepQuietly(sleepMillis);
        }
    }

    private void sleepQuietly(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
