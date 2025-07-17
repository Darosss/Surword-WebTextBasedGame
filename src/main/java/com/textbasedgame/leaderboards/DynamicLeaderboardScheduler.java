package com.textbasedgame.leaderboards;

import com.textbasedgame.settings.AppConfigManager;
import jakarta.annotation.PostConstruct;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Component
public class DynamicLeaderboardScheduler implements SchedulingConfigurer {

    private final AppConfigManager cfg;
    private final LeaderboardsService service;

    public DynamicLeaderboardScheduler(AppConfigManager cfg, LeaderboardsService service) {
        this.cfg = cfg;
        this.service = service;
    }

    @PostConstruct
    public void runImmediately() {
        service.updateLeaderboardScheduled();
    }
    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        registrar.addTriggerTask(
                service::updateLeaderboardScheduled,

                triggerContext -> {
                    long interval = cfg.getSystemConfig().getLeaderboardRefreshCooldownMs();
                    Instant last  = Optional.ofNullable(triggerContext.lastCompletion())
                            .orElse(Instant.now());
                    return Date.from(last.plusMillis(interval)).toInstant();
                }
        );
    }
}
