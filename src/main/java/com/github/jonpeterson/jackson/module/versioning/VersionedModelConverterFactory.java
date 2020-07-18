package com.github.jonpeterson.jackson.module.versioning;

public interface VersionedModelConverterFactory {
    VersionedModelConverter create(Class<? extends VersionedModelConverter> converterClass);
}
