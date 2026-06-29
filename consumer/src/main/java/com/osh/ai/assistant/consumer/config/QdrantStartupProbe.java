package com.osh.ai.assistant.consumer.config;

import com.osh.ai.assistant.consumer.service.AlertService;
import io.qdrant.client.QdrantClient;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class QdrantStartupProbe {
    private static final String ALERT_KEY = "startup-qdrant-unreachable";

    private final QdrantClient qdrantClient;
    private final ExecutorService executorService;
    private final AlertService alertService;

    @Value("${spring.ai.vectorstore.qdrant.host:127.0.0.1}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${spring.ai.vectorstore.qdrant.collection-name:assistant_knowledge_lib}")
    private String collectionName;

    @Value("${ai-assistant.qdrant.probe.enabled:true}")
    private boolean probeEnabled;

    @Value("${ai-assistant.qdrant.probe.warn-after-seconds:10}")
    private long warnAfterSeconds;

    @Value("${ai-assistant.qdrant.probe.check-timeout-seconds:10}")
    private long checkTimeoutSeconds;

    @Value("${ai-assistant.qdrant.probe.retry-interval-seconds:10}")
    private long retryIntervalSeconds;

    private volatile boolean running;

    public QdrantStartupProbe(QdrantClient qdrantClient, ExecutorService executorService, AlertService alertService) {
        this.qdrantClient = qdrantClient;
        this.executorService = executorService;
        this.alertService = alertService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (!probeEnabled || running) {
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
        long warnAfterNanos = TimeUnit.SECONDS.toNanos(Math.max(0, warnAfterSeconds));
        Duration timeout = Duration.ofSeconds(Math.max(1, checkTimeoutSeconds));
        long sleepMillis = TimeUnit.SECONDS.toMillis(Math.max(1, retryIntervalSeconds));

        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                qdrantClient.healthCheckAsync(timeout).get(timeout.toMillis() + 1000L, TimeUnit.MILLISECONDS);
                long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startNanos);
                log.info("Qdrant connection is ready, host={}, port={}, collection={}, elapsed={}s",
                    qdrantHost, qdrantPort, collectionName, elapsedSeconds);
                alertService.notifyResolved(ALERT_KEY, "Qdrant 向量库连接已恢复",
                    "Qdrant 启动探针已恢复正常。\nHost: " + qdrantHost
                        + "\nPort: " + qdrantPort
                        + "\nCollection: " + collectionName
                        + "\n恢复耗时: " + elapsedSeconds + "s");
                return;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Qdrant startup probe interrupted, host={}, port={}", qdrantHost, qdrantPort);
                return;
            }
            catch (Exception e) {
                long elapsedNanos = System.nanoTime() - startNanos;
                if (elapsedNanos >= warnAfterNanos) {
                    long elapsedSeconds = TimeUnit.NANOSECONDS.toSeconds(elapsedNanos);
                    log.error("Qdrant is still unreachable after {}s, host={}, port={}, collection={}, nextRetry={}s",
                        elapsedSeconds,
                        qdrantHost,
                        qdrantPort,
                        collectionName,
                        retryIntervalSeconds,
                        e);
                    alertService.notifyOnce(ALERT_KEY, "Qdrant 向量库连接不可用",
                        "Qdrant 启动探针检测到连接不可用。\nHost: " + qdrantHost
                            + "\nPort: " + qdrantPort
                            + "\nCollection: " + collectionName
                            + "\n持续时长: " + elapsedSeconds + "s"
                            + "\n下次重试间隔: " + retryIntervalSeconds + "s"
                            + "\n异常: " + e.getClass().getSimpleName() + ": " + e.getMessage());
                }
            }

            sleepQuietly(sleepMillis);
        }
    }

    private void sleepQuietly(long sleepMillis) {
        try {
            Thread.sleep(sleepMillis);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
