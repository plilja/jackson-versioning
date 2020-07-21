package com.github.jonpeterson.jackson.module.versioning;

/**
 * Describes the set of available versions.
 */
public interface VersionsDescription<T extends Comparable<T>> {
    T getCurrentVersion();

    T fromString(String value);
}
