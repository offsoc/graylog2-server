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
package org.graylog.plugins.views.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.mongojack.Id;
import org.mongojack.ObjectId;

public record SearchJobIdentifier(@JsonProperty("id")
                                  @ObjectId
                                  @Id
                                  String id,
                                  @JsonProperty(SEARCH_ID_FIELD) String searchId,
                                  @JsonProperty(OWNER_FIELD) String owner,
                                  @JsonProperty(NODE_ID_FIELD) String executingNodeId) {
    public static final String SEARCH_ID_FIELD = "search_id";
    public static final String OWNER_FIELD = "owner";
    public static final String NODE_ID_FIELD = "executing_node";
}

