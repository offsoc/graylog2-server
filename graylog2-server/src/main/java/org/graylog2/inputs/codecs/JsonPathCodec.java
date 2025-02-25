/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs.codecs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import com.jayway.jsonpath.JsonPath;
import jakarta.inject.Inject;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.inputs.annotations.Codec;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.inputs.codecs.AbstractCodec;
import org.graylog2.plugin.inputs.codecs.CodecAggregator;
import org.graylog2.plugin.inputs.failure.InputProcessingException;
import org.graylog2.plugin.journal.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Codec(name = "jsonpath", displayName = "JSON Path")
public class JsonPathCodec extends AbstractCodec {

    private static final Logger LOG = LoggerFactory.getLogger(JsonPathCodec.class);
    public static final String CK_PATH = "path";
    public static final String CK_SOURCE = "source";
    public static final String CK_FLATTEN = "enable_flattening";

    private final JsonPath jsonPath;
    private final boolean flatten;
    private final ObjectMapper objectMapper;
    private final MessageFactory messageFactory;

    @AssistedInject
    public JsonPathCodec(@Assisted Configuration configuration, ObjectMapper objectMapper, MessageFactory messageFactory) {
        super(configuration);
        this.messageFactory = messageFactory;
        final String pathString = configuration.getString(CK_PATH);
        jsonPath = pathString == null ? null : JsonPath.compile(pathString);
        flatten = configuration.getBoolean(CK_FLATTEN);
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<Message> decodeSafe(@Nonnull RawMessage rawMessage) {
        Map<String, Object> fields = new HashMap<>();
        if (flatten) {
            final String json = new String(rawMessage.getPayload(), charset);
            try {
                fields = flatten(json);
            } catch (JsonFlattenException e) {
                throw InputProcessingException.create(
                        "JSON contains type not supported by flatten method.", e, rawMessage, json);
            } catch (JsonProcessingException e) {
                throw InputProcessingException.create(
                        "Could not parse JSON.", e, rawMessage, json);
            }
        } else {
            if (jsonPath == null) {
                throw InputProcessingException.create(
                        "Field <%s> is empty for input with id <%s>".formatted(CK_PATH, rawMessage.getSourceNodes().get(0).inputId),
                        rawMessage);
            }
            final String json = new String(rawMessage.getPayload(), charset);
            fields = read(json);
        }

        final Message message = messageFactory.createMessage(buildShortMessage(fields),
                configuration.getString(CK_SOURCE),
                rawMessage.getTimestamp());
        message.addFields(fields);
        return Optional.of(message);
    }

    @VisibleForTesting
    protected Map<String, Object> read(String json) {
        final Object result = jsonPath.read(json);

        final Map<String, Object> fields = Maps.newHashMap();

        if (result instanceof Integer || result instanceof Double || result instanceof Long) {
            fields.put("result", result);
        } else if (result instanceof List) {
            final List list = (List) result;
            if (!list.isEmpty()) {
                fields.put("result", list.get(0).toString());
            }
        } else {
            // Now it's most likely a string or something we do not map.
            fields.put("result", result.toString());
        }
        return fields;
    }

    @VisibleForTesting
    protected String buildShortMessage(Map<String, Object> fields) {
        final StringBuilder shortMessage = new StringBuilder();
        shortMessage.append("JSON API poll result: ");
        if (!flatten) {
            shortMessage.append(jsonPath.getPath());
        }
        shortMessage.append(" -> ");

        if (fields.toString().length() > 50) {
            shortMessage.append(fields.toString().substring(0, 50)).append("[...]");
        } else {
            shortMessage.append(fields);
        }

        return shortMessage.toString();
    }

    public Map<String, Object> flatten(String json) throws JsonFlattenException, JsonProcessingException {
        return flatten("", objectMapper.readTree(json));
    }

    private Map<String, Object> flatten(String currentPath, JsonNode jsonNode) throws JsonFlattenException {
        if (jsonNode.isObject()) {
            ObjectNode objectNode = (ObjectNode) jsonNode;
            Iterator<Map.Entry<String, JsonNode>> iter = objectNode.fields();
            String pathPrefix = currentPath.isEmpty() ? "" : currentPath + ".";
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();

            while (iter.hasNext()) {
                Map.Entry<String, JsonNode> entry = iter.next();
                builder.putAll(flatten(pathPrefix + entry.getKey(), entry.getValue()));
            }
            return builder.build();
        } else if (jsonNode.isArray()) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
            for (int i = 0; i < arrayNode.size(); i++) {
                builder.putAll(flatten(currentPath + i, arrayNode.get(i)));
            }
            return builder.build();
        } else if (jsonNode.isTextual()) {
            TextNode textNode = (TextNode) jsonNode;
            return Map.of(currentPath, textNode.toString());
        } else if (jsonNode.isNumber()) {
            NumericNode numericNode = (NumericNode) jsonNode;
            return Map.of(currentPath, numericNode.numberValue());
        } else if (jsonNode.isBoolean()) {
            BooleanNode booleanNode = (BooleanNode) jsonNode;
            return Map.of(currentPath, booleanNode.asBoolean());
        } else {
            throw new JsonFlattenException("Warning: JSON contains type not supported by the flatten method. JsonNode: " + jsonNode);
        }
    }

    public static class JsonFlattenException extends Exception {
        public JsonFlattenException(String errorMessage) {
            super(errorMessage);
        }
    }

    @Nullable
    @Override
    public CodecAggregator getAggregator() {
        return null;
    }

    @FactoryClass
    public interface Factory extends AbstractCodec.Factory<JsonPathCodec> {
        @Override
        JsonPathCodec create(Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends AbstractCodec.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest r = super.getRequestedConfiguration();

            r.addField(new TextField(
                    CK_PATH,
                    "JSON path of data to extract",
                    "$.store.book[1].number_of_orders",
                    "Path to the value you want to extract from the JSON response. Take a look at the documentation for a more detailed explanation.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new TextField(
                    CK_SOURCE,
                    "Message source",
                    "yourapi",
                    "What to use as source field of the resulting message.",
                    ConfigurationField.Optional.NOT_OPTIONAL
            ));

            r.addField(new BooleanField(
                    CK_FLATTEN,
                    "Flatten JSON",
                    false,
                    "If set, the whole JSON will be flattened and returned as message fields."
            ));

            return r;
        }

        @Override
        public void overrideDefaultValues(@Nonnull ConfigurationRequest cr) {

        }
    }

    public static class Descriptor extends AbstractCodec.Descriptor {
        @Inject
        public Descriptor() {
            super(JsonPathCodec.class.getAnnotation(Codec.class).displayName());
        }
    }
}
