package com.github.jonpeterson.jackson.module.versioning;

class EnumVersionsDescription<V extends Enum<V>> implements VersionsDescription<V> {
    private final Class<V> enumClass;
    private final V currentVersion;

    EnumVersionsDescription(Class<V> enumClass) {
        this.enumClass = enumClass;
        currentVersion = enumClass.getEnumConstants()[enumClass.getEnumConstants().length - 1];
    }

    @Override
    public V getCurrentVersion() {
        return currentVersion;
    }

    @Override
    public V fromString(String value) {
        return V.valueOf(enumClass, value);
    }
}
