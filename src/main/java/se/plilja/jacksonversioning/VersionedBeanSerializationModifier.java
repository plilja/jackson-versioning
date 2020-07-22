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

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

class VersionedBeanSerializationModifier<V extends Comparable<V>> extends BeanSerializerModifier {

    private final VersionedConverterRepository<V> versionedConverterRepository;
    private final VersionsDescription<V> versionsDescription;
    private final VersionResolutionStrategy<V> versionResolutionStrategy;

    VersionedBeanSerializationModifier(VersionedConverterRepository<V> versionedConverterRepository, VersionsDescription<V> versionsDescription, VersionResolutionStrategy<V> versionResolutionStrategy) {
        this.versionedConverterRepository = versionedConverterRepository;
        this.versionsDescription = versionsDescription;
        this.versionResolutionStrategy = versionResolutionStrategy;
    }

    private <T> VersionedSerializer<T, V> createVersionedSerializer(
            StdSerializer<T> serializer,
            JsonVersioned jsonVersioned) {
        return new VersionedSerializer<>(
                serializer,
                versionedConverterRepository,
                jsonVersioned,
                versionsDescription,
                versionResolutionStrategy);
    }

    @Override
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDescription, JsonSerializer<?> serializer) {
        if (serializer instanceof StdSerializer) {
            JsonVersioned jsonVersioned = beanDescription.getClassAnnotations().get(JsonVersioned.class);
            if (jsonVersioned != null)
                return createVersionedSerializer(
                        (StdSerializer) serializer,
                        jsonVersioned
                );
        }

        return serializer;
    }
}
