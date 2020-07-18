package com.github.jonpeterson.jackson.module.versioning;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class ReflectionVersionedModelConverterFactory implements VersionedModelConverterFactory {
    private final Map<Class<? extends VersionedModelConverter>, VersionedModelConverter> map = new ConcurrentHashMap<>();

    @Override
    public VersionedModelConverter create(Class<? extends VersionedModelConverter> converterClass) {
        return map.computeIfAbsent(converterClass, this::createWithReflection);
    }

    private VersionedModelConverter createWithReflection(Class<? extends VersionedModelConverter> converterClass) {
        if (converterClass != VersionedModelConverter.class) {
            try {
                Constructor<? extends VersionedModelConverter> constructor = converterClass.getConstructor();
                return constructor.newInstance();
            } catch (Exception e) {
                throw new RuntimeException("unable to create instance of converter '" + converterClass.getName() + "'", e);
            }
        } else {
            return null;
        }
    }
}
