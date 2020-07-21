package com.github.jonpeterson.jackson.module.versioning;

public interface VersionedConverterFactory<V> {
    VersionConverter<V> create(Class<? extends VersionConverter<V>> converterClass);
}
