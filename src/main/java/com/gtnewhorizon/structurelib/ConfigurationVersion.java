package com.gtnewhorizon.structurelib;

import java.util.Arrays;
import java.util.Objects;

import javax.annotation.Nullable;

import net.minecraftforge.common.config.Configuration;

enum ConfigurationVersion {

    V1 {

        @Override
        protected void step(Configuration c) {}
    };

    private static final ConfigurationVersion[] VALUES = values();

    private final String versionMarker;

    ConfigurationVersion() {
        this.versionMarker = name();
    }

    public static void migrateToLatest(Configuration c) {
        for (int i = identify(c).ordinal() + 1; i < VALUES.length; i++) {
            VALUES[i].step(c);
        }
    }

    public static ConfigurationVersion latest() {
        return VALUES[VALUES.length - 1];
    }

    public static ConfigurationVersion identify(Configuration c) {
        return Arrays.stream(VALUES).filter(v -> Objects.equals(c.getLoadedConfigVersion(), v.getVersionMarker()))
                .findFirst().orElse(V1);
    }

    @Nullable
    public String getVersionMarker() {
        return versionMarker;
    }

    protected abstract void step(Configuration c);
}
