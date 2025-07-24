package com.textbasedgame.settings;

import dev.morphia.Datastore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class AppConfigManager {
    @Autowired
    private final Datastore datastore;
    private AppConfig currentConfig;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Autowired
    public AppConfigManager(Datastore datastore) {
        this.datastore = datastore;
    }


    @PostConstruct                // runs right after bean construction
    private void init() {
        loadFromDbOrDefaults();
    }

    private void loadFromDbOrDefaults() {
        lock.writeLock().lock();
        try {
            currentConfig = datastore.find(AppConfig.class).first();
            if (currentConfig == null) {
                currentConfig = AppConfig.defaults();
                datastore.save(currentConfig);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }


    public AppConfig getConfig() {
        if (currentConfig == null) {
            loadFromDbOrDefaults();
        }
        lock.readLock().lock();
        try {
            return currentConfig.copy();
        } finally {
            lock.readLock().unlock();
        }
    }

    public void updateConfig(AppConfig newConfig) {
        lock.writeLock().lock();
        try {
            AppConfig dbConfigs = datastore.find(AppConfig.class).first();
            if (dbConfigs != null) {
                dbConfigs.setSystemConfig(newConfig.getSystemConfig());
                dbConfigs.setGoldConfig(newConfig.getGoldConfig());
                dbConfigs.setLootConfig(newConfig.getLootConfig());
                dbConfigs.setMerchantConfig(newConfig.getMerchantConfig());
                dbConfigs.setSkirmishConfig(newConfig.getSkirmishConfig());
                dbConfigs.setXpConfig(newConfig.getXpConfig());

                datastore.save(dbConfigs);
                currentConfig = dbConfigs.copy();

            } else {
                AppConfig dbConfig = datastore.save(newConfig);
                currentConfig = dbConfig.copy();
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    //TODO: make usage of update[...opts?] idk

    public SkirmishConfig getSkirmishOpts() {
        return getConfig().getSkirmishConfig();
    }

    public void updateSkirmishOpts(SkirmishConfig opts) {
        AppConfig cfg = getConfig();
        cfg.setSkirmishConfig(opts);
        updateConfig(cfg);
    }

    public LootConfig getLootConfig() {
        return getConfig().getLootConfig();
    }

    public void updateLootConfig(LootConfig config) {
        AppConfig cfg = getConfig();
        cfg.setLootConfig(config);
        updateConfig(cfg);
    }


    public GoldConfig getGoldConfigs() {
        return getConfig().getGoldConfig();
    }

    public void updateGoldConfig(GoldConfig config) {
        AppConfig cfg = getConfig();
        cfg.setGoldConfig(config);
        updateConfig(cfg);
    }

    public XpConfig getXpConfig() {
        return getConfig().getXpConfig();
    }

    public void updateXpConfig(XpConfig config) {
        AppConfig cfg = getConfig();
        cfg.setXpConfig(config);
        updateConfig(cfg);
    }

    public MerchantConfig getMerchantConfig() {
        return getConfig().getMerchantConfig();
    }

    public void updateMerchantConfig(MerchantConfig config) {
        AppConfig cfg = getConfig();
        cfg.setMerchantConfig(config);
        updateConfig(cfg);
    }
    public SystemConfig getSystemConfig() {
        return getConfig().getSystemConfig();
    }

    public void updateSystemConfig(SystemConfig config) {
        AppConfig cfg = getConfig();
        cfg.setSystemConfig(config);
        updateConfig(cfg);
    }

}
