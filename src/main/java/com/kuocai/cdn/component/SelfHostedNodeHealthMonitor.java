package com.kuocai.cdn.component;

import com.kuocai.cdn.service.SelfHostedCdnService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@DependsOn("selfHostedCdnSchemaInitializer")
public class SelfHostedNodeHealthMonitor {
    private final SelfHostedCdnService selfHostedCdnService;
    private ScheduledExecutorService executor;
    private long nextTelemetryCleanupAt;

    public SelfHostedNodeHealthMonitor(SelfHostedCdnService selfHostedCdnService) {
        this.selfHostedCdnService = selfHostedCdnService;
    }

    @PostConstruct
    public void start() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "self-hosted-cdn-health");
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::reconcile, 30, 60, TimeUnit.SECONDS);
    }

    private void reconcile() {
        try {
            selfHostedCdnService.markStaleNodesOffline();
        } catch (Exception e) {
            log.warn("Self-hosted CDN node status reconciliation failed: {}", e.getMessage());
        }
        try {
            selfHostedCdnService.expireStaleCacheJobs();
        } catch (Exception e) {
            log.warn("Self-hosted CDN cache job reconciliation failed: {}", e.getMessage());
        }
        try {
            selfHostedCdnService.reconcileGroupDns();
        } catch (Exception e) {
            log.warn("Self-hosted CDN health reconciliation failed: {}", e.getMessage());
        }
        if (System.currentTimeMillis() >= nextTelemetryCleanupAt) {
            try {
                selfHostedCdnService.purgeNodeTelemetry();
                nextTelemetryCleanupAt = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1);
            } catch (Exception e) {
                log.warn("Self-hosted CDN telemetry cleanup failed: {}", e.getMessage());
            }
        }
    }

    @PreDestroy
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
