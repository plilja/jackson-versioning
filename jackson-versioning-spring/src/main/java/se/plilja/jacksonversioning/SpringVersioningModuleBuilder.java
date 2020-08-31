/*
 * The MIT License
 * Copyright © 2020 Patrik Lilja
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.plilja.jacksonversioning;

import org.springframework.context.ApplicationContext;

import java.util.Objects;

public final class SpringVersioningModuleBuilder {
    private SpringVersioningModuleBuilder() {
        // should not be instantiated
    }

    private static class InnerBuilder<V extends Comparable<V>> implements SpringVersioningModuleBuilder.WithVersionResolutionStrategy, SpringVersioningModuleBuilder.WithConverterRepository, SpringVersioningModuleBuilder.ReadyForBuilding {
        private final VersionsDescription<V> versionsDescription;
        private VersionedConverterRepository<V> versionedConverterRepository = null;
        private VersionResolutionStrategy<V> versionResolutionStrategy = null;

        private InnerBuilder(VersionsDescription<V> versionsDescription) {
            this.versionsDescription = versionsDescription;
        }

        @Override
        public WithConverterRepository withVersionDeterminedByRequestParameter(String parameterName) {
            versionResolutionStrategy = new RequestParameterVersionResolutionStrategy<>(parameterName, versionsDescription);
            return this;
        }

        @Override
        public WithConverterRepository withVersionDeterminedByRequestHeader(String headerName) {
            versionResolutionStrategy = new RequestHeaderVersionResolutionStrategy<>(headerName, versionsDescription);
            return this;
        }

        @Override
        public ReadyForBuilding withConvertersFromApplicationContext(ApplicationContext applicationContext) {
            versionedConverterRepository = new ApplicationContextConverterRepository<>(applicationContext);
            return this;
        }

        @Override
        public ReadyForBuilding withConvertersCreatedByReflection() {
            versionedConverterRepository = new ReflectionVersionedConverterRepository<>();
            return this;
        }

        @Override
        public VersioningModule build() {
            Objects.requireNonNull(versionsDescription, "VersionsDescription must be set before building");
            Objects.requireNonNull(versionedConverterRepository, "VersionsConverterRepository must be set before building");
            Objects.requireNonNull(versionResolutionStrategy, "VersionResolutionStrategy must be set before building");
            return new VersioningModule(versionsDescription, versionedConverterRepository, versionResolutionStrategy);
        }
    }

    public static <V extends Enum<V>> WithVersionResolutionStrategy withEnumVersions(Class<V> versionsEnum) {
        return new InnerBuilder<>(new EnumVersionsDescription<V>(versionsEnum));
    }

    public static <V extends Comparable<V>> WithVersionResolutionStrategy withCustomVersions(VersionsDescription<V> versionsDescription) {
        return new InnerBuilder<>(versionsDescription);
    }

    public interface WithVersionResolutionStrategy {
        WithConverterRepository withVersionDeterminedByRequestParameter(String parameterName);

        WithConverterRepository withVersionDeterminedByRequestHeader(String headerName);
    }

    public interface WithConverterRepository {
        ReadyForBuilding withConvertersFromApplicationContext(ApplicationContext applicationContext);

        ReadyForBuilding withConvertersCreatedByReflection();
    }

    public interface ReadyForBuilding {
        VersioningModule build();
    }
}

