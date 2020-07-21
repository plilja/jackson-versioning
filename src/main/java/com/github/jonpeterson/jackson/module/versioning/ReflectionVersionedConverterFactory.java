package com.github.jonpeterson.jackson.module.versioning;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ReflectionVersionedConverterFactory<V> implements VersionedConverterFactory<V> {
    private final Map<Class<? extends VersionConverter<V>>, VersionConverter<V>> map = new ConcurrentHashMap<>();

    @Override
    public VersionConverter<V> create(Class<? extends VersionConverter<V>> converterClass) {
        return map.computeIfAbsent(converterClass, this::createWithReflection);
    }

    private VersionConverter<V> createWithReflection(Class<? extends VersionConverter<V>> converterClass) {
        if (!converterClass.equals(VersionConverter.class)) {
            try {
                Constructor<? extends VersionConverter<V>> constructor = converterClass.getConstructor();
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("unable to create instance of converter '" + converterClass.getName() + "'", e);
            }
        } else {
            return null;
        }
    }
}
