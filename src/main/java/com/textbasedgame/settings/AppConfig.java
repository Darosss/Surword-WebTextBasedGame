package com.textbasedgame.settings;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

@Entity("app_configs")
public class AppConfig {
    @JsonSerialize(using = ToStringSerializer.class)
    @Id
    private ObjectId id;

    private SkirmishConfig skirmishConfig;
    private LootConfig lootConfig;
    private XpConfig xpConfig;
    private GoldConfig goldConfig;
    private MerchantConfig merchantConfig;
    private SystemConfig systemConfig;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public SkirmishConfig getSkirmishConfig() {
        return skirmishConfig;
    }

    public void setSkirmishConfig(SkirmishConfig skirmishConfig) {
        this.skirmishConfig = skirmishConfig;
    }

    public LootConfig getLootConfig() {
        return lootConfig;
    }

    public void setLootConfig(LootConfig lootConfig) {
        this.lootConfig = lootConfig;
    }

    public XpConfig getXpConfig() {
        return xpConfig;
    }

    public void setXpConfig(XpConfig xpConfig) {
        this.xpConfig = xpConfig;
    }

    public GoldConfig getGoldConfig() {
        return goldConfig;
    }

    public void setGoldConfig(GoldConfig goldConfig) {
        this.goldConfig = goldConfig;
    }


    public MerchantConfig getMerchantConfig() {
        return merchantConfig;
    }

    public void setMerchantConfig(MerchantConfig merchantConfig) {
        this.merchantConfig = merchantConfig;
    }

    public SystemConfig getSystemConfig() {
        return systemConfig;
    }

    public void setSystemConfig(SystemConfig systemConfig) {
        this.systemConfig = systemConfig;
    }

    public AppConfig copy() {
        AppConfig c = new AppConfig();
        c.id = this.id;
        c.skirmishConfig = this.skirmishConfig;
        c.lootConfig     = this.lootConfig;
        c.xpConfig       = this.xpConfig;
        c.goldConfig     = this.goldConfig;
        c.merchantConfig = this.merchantConfig;
        c.systemConfig = this.systemConfig;
        return c;
    }

    public static AppConfig defaults() {
        AppConfig cfg = new AppConfig();
        cfg.skirmishConfig = SkirmishConfig.defaults();
        cfg.lootConfig     = LootConfig.defaults();
        cfg.xpConfig       = XpConfig.defaults();
        cfg.goldConfig     = GoldConfig.defaults();
        cfg.merchantConfig = MerchantConfig.defaults();
        cfg.systemConfig = SystemConfig.defaults();
        return cfg;
    }
    @Override
    public String toString() {
        return "AppConfig{" +
                "id=" + id +
                ", skirmishConfig=" + skirmishConfig +
                ", lootConfig=" + lootConfig +
                ", xpConfig=" + xpConfig +
                ", goldConfig=" + goldConfig +
                ", merchantConfig=" + merchantConfig +
                ", systemConfig=" + systemConfig +
                '}';
    }
}
