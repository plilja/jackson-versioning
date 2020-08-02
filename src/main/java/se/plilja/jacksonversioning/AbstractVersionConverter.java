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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractVersionConverter<V extends Comparable<V>> implements VersionConverter<V> {
    private final SortedMap<V, LinkedList<BiFunction<ObjectNode, JsonNodeFactory, ObjectNode>>> upConverters = new TreeMap<>();
    private final SortedMap<V, LinkedList<BiFunction<ObjectNode, JsonNodeFactory, ObjectNode>>> downConverters = new TreeMap<>((o1, o2) -> -o1.compareTo(o2));
    private final Class<?> targetClass;
    private final List<String> descriptions = new ArrayList<>();

    public AbstractVersionConverter(Class<?> targetClass) {
        this.targetClass = targetClass;
    }

    private void addConverter(
            V downVersion,
            V upVersion,
            BiFunction<ObjectNode, JsonNodeFactory, ObjectNode> downConverter,
            BiFunction<ObjectNode, JsonNodeFactory, ObjectNode> upConverter,
            String description) {
        upConverters.computeIfAbsent(downVersion, (key) -> new LinkedList<>()).add(upConverter);
        downConverters.computeIfAbsent(upVersion, (key) -> new LinkedList<>()).addFirst(downConverter);
        descriptions.add(description);
    }

    protected void attributeAdded(V downModelVersion, V upModelVersion, String attributeName, Function<ObjectNode, Object> valueProvider) {
        addConverter(
                downModelVersion,
                upModelVersion,
                (modelData, nodeFactory) -> {
                    modelData.remove(attributeName);
                    return modelData;
                },
                setFromValue(attributeName, valueProvider),
                String.format("Attribute %s was added to class %s", attributeName, targetClass.getSimpleName())
        );
    }

    protected void attributeRemoved(V downModelVersion, V upModelVersion, String attributeName, Function<ObjectNode, Object> valueProvider) {
        addConverter(
                downModelVersion,
                upModelVersion,
                setFromValue(attributeName, valueProvider),
                (modelData, nodeFactory) -> {
                    modelData.remove(attributeName);
                    return modelData;
                },
                String.format("Attribute %s was removed from class %s", attributeName, targetClass.getSimpleName())
        );
    }

    protected void attributeModified(V downModelVersion, V upModelVersion, String attributeName, BiFunction<ObjectNode, JsonNode, Object> valueDownModifier, BiFunction<ObjectNode, JsonNode, Object> valueUpModifier) {
        addConverter(
                downModelVersion,
                upModelVersion,
                setFromValue(attributeName, (modelData) -> {
                    JsonNode jsonNode = modelData.get(attributeName);
                    return valueDownModifier.apply(modelData, jsonNode);
                }),
                setFromValue(attributeName, (modelData) -> {
                    JsonNode jsonNode = modelData.get(attributeName);
                    return valueUpModifier.apply(modelData, jsonNode);
                }),
                String.format("Attribute %s was removed from class %s", attributeName, targetClass.getSimpleName())
        );
    }

    private BiFunction<ObjectNode, JsonNodeFactory, ObjectNode> setFromValue(String attributeName, Function<ObjectNode, Object> defaultValueSupplier) {
        return (ObjectNode modelData, JsonNodeFactory nodeFactory) -> {
            Object defaultValue = defaultValueSupplier.apply(modelData);
            JsonNode node = nodeFactory.nullNode();
            if (node == null) {
                node = nodeFactory.nullNode();
            } else if (defaultValue instanceof Boolean) {
                node = nodeFactory.booleanNode((boolean) defaultValue);
            } else if (defaultValue instanceof String) {
                node = nodeFactory.textNode((String) defaultValue);
            } else if (defaultValue instanceof BigDecimal) {
                node = nodeFactory.numberNode((BigDecimal) defaultValue);
            } else if (defaultValue instanceof BigInteger) {
                node = nodeFactory.numberNode((BigInteger) defaultValue);
            } else if (defaultValue instanceof Double) {
                node = nodeFactory.numberNode((Double) defaultValue);
            } else if (defaultValue instanceof Float) {
                node = nodeFactory.numberNode((Float) defaultValue);
            } else if (defaultValue instanceof Long) {
                node = nodeFactory.numberNode((Long) defaultValue);
            } else if (defaultValue instanceof Integer) {
                node = nodeFactory.numberNode((Integer) defaultValue);
            } else if (defaultValue instanceof Short) {
                node = nodeFactory.numberNode((Short) defaultValue);
            } else if (defaultValue instanceof Byte) {
                node = nodeFactory.numberNode((Byte) defaultValue);
            } else {
                node = nodeFactory.pojoNode(defaultValue);
            }
            modelData.set(attributeName, node);
            return modelData;
        };
    }

    protected void attributeRenamed(V downModelVersion, V upModelVersion, String oldAttributeName, String newAttributeName) {
        addConverter(
                downModelVersion,
                upModelVersion,
                (modelData, nodeFactory) -> {
                    JsonNode jsonNode = modelData.get(newAttributeName);
                    modelData.set(oldAttributeName, jsonNode);
                    modelData.remove(newAttributeName);
                    return modelData;
                },
                (modelData, nodeFactory) -> {
                    JsonNode jsonNode = modelData.get(oldAttributeName);
                    modelData.set(newAttributeName, jsonNode);
                    modelData.remove(oldAttributeName);
                    return modelData;
                },
                String.format("Attribute %s on class %s was renamed to %s", oldAttributeName, targetClass.getSimpleName(), newAttributeName)
        );
    }

    // TODO use for something or remove !!!
    public List<String> describe() {
        return Collections.unmodifiableList(descriptions);
    }

    @Override
    public void convertDown(ObjectNode modelData, V fromVersion, V toVersion, JsonNodeFactory nodeFactory) {
        for (List<BiFunction<ObjectNode, JsonNodeFactory, ObjectNode>> converters : downConverters.subMap(fromVersion, toVersion).values()) {
            for (BiFunction<ObjectNode, JsonNodeFactory, ObjectNode> converter : converters) {
                converter.apply(modelData, nodeFactory);
            }
        }
    }

    @Override
    public void convertUp(ObjectNode modelData, V fromVersion, V toVersion, JsonNodeFactory nodeFactory) {
        for (List<BiFunction<ObjectNode, JsonNodeFactory, ObjectNode>> converters : upConverters.subMap(fromVersion, toVersion).values()) {
            for (BiFunction<ObjectNode, JsonNodeFactory, ObjectNode> converter : converters) {
                converter.apply(modelData, nodeFactory);
            }
        }
    }
}
