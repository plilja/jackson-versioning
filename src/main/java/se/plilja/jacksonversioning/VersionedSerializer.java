/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jon Peterson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package se.plilja.jacksonversioning;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.ResolvableSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

class VersionedSerializer<T, V extends Comparable<V>> extends StdSerializer<T> implements ResolvableSerializer {
    private final StdSerializer<T> delegate;
    private final VersionedConverterRepository<V> versionedConverterRepository;
    private final JsonVersioned jsonVersioned;
    private final VersionsDescription<V> versionsDescription;
    private final VersionResolutionStrategy<V> versionResolutionStrategy;

    VersionedSerializer(
            StdSerializer<T> delegate,
            VersionedConverterRepository<V> versionedConverterRepository,
            JsonVersioned jsonVersioned,
            VersionsDescription<V> versionsDescription,
            VersionResolutionStrategy<V> versionResolutionStrategy) {
        super(delegate.handledType());

        this.delegate = delegate;
        this.versionedConverterRepository = versionedConverterRepository;
        this.jsonVersioned = jsonVersioned;
        this.versionsDescription = versionsDescription;
        this.versionResolutionStrategy = versionResolutionStrategy;
    }

    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException {
        if (delegate instanceof ResolvableSerializer)
            ((ResolvableSerializer) delegate).resolve(provider);
    }

    @Override
    public void serialize(T value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        doSerialize(value, generator, provider, null);
    }

    @Override
    public void serializeWithType(T value, JsonGenerator generator, SerializerProvider provider, TypeSerializer typeSerializer) throws IOException {
        doSerialize(value, generator, provider, typeSerializer);
    }

    private void doSerialize(T value, JsonGenerator generator, SerializerProvider provider, TypeSerializer typeSerializer) throws IOException {
        // serialize the value into a byte array buffer then parse it back out into a JsonNode tree
        // TODO: find a better way to convert the value into a tree
        JsonFactory factory = generator.getCodec().getFactory();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(4096);
        JsonGenerator bufferGenerator = factory.createGenerator(buffer);
        try {
            if (typeSerializer != null)
                delegate.serializeWithType(value, bufferGenerator, provider, typeSerializer);
            else
                delegate.serialize(value, bufferGenerator, provider);
        } finally {
            bufferGenerator.close();
        }

        ObjectNode modelData = factory.createParser(buffer.toByteArray()).readValueAsTree();

        V targetVersion = versionResolutionStrategy.getSerializeToVersion(modelData);
        if (targetVersion == null) {
            targetVersion = versionsDescription.getCurrentVersion();
        }

        VersionConverter<V> converter = versionedConverterRepository.get((Class) jsonVersioned.converterClass());
        if (converter != null && targetVersion.compareTo(versionsDescription.getCurrentVersion()) < 0) {
            converter.convertDown(modelData, versionsDescription.getCurrentVersion(), targetVersion, JsonNodeFactory.instance);
        }

        // write node
        generator.writeTree(modelData);
    }
}