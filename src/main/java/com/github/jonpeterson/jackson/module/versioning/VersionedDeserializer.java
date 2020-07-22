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
package com.github.jonpeterson.jackson.module.versioning;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;

import java.io.IOException;

class VersionedDeserializer<T, V extends Comparable<V>> extends StdDeserializer<T> implements ResolvableDeserializer {
    private final StdDeserializer<T> delegate;
    private final JsonVersioned jsonVersioned;
    private final VersionedConverterRepository<V> versionedConverterRepository;
    private final VersionsDescription<V> versionsDescription;
    private final VersionResolutionStrategy<V> versionResolutionStrategy;

    VersionedDeserializer(
            StdDeserializer<T> delegate,
            VersionedConverterRepository<V> versionedConverterRepository,
            JsonVersioned jsonVersioned,
            VersionsDescription<V> versionsDescription,
            VersionResolutionStrategy<V> versionResolutionStrategy) {
        super(delegate.getValueType());
        this.delegate = delegate;
        this.jsonVersioned = jsonVersioned;
        this.versionedConverterRepository = versionedConverterRepository;
        this.versionsDescription = versionsDescription;
        this.versionResolutionStrategy = versionResolutionStrategy;
    }

    @Override
    public void resolve(DeserializationContext context) throws JsonMappingException {
        if (delegate instanceof ResolvableDeserializer)
            ((ResolvableDeserializer) delegate).resolve(context);
    }

    @Override
    public T deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = parser.readValueAsTree();

        if (!(jsonNode instanceof ObjectNode))
            throw context.mappingException("value must be a JSON object");

        ObjectNode modelData = (ObjectNode) jsonNode;

        V version = versionResolutionStrategy.getDeserializeToVersion(modelData);

        if (version == null) {
            version = versionsDescription.getCurrentVersion();
        }

        // convert the model if converter specified and model needs converting
        VersionConverter<V> converter = versionedConverterRepository.get((Class) jsonVersioned.converterClass());
        if (converter != null && version.compareTo(versionsDescription.getCurrentVersion()) < 0) {
            converter.convertUp(modelData, version, versionsDescription.getCurrentVersion(), context.getNodeFactory());
        }

        JsonParser postInterceptionParser = new TreeTraversingParser(modelData, parser.getCodec());
        postInterceptionParser.nextToken();
        return delegate.deserialize(postInterceptionParser, context);
    }
}