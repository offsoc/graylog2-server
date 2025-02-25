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
package org.graylog2.indexer;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableSet;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.IndexSetService;
import org.graylog2.indexer.indexset.events.IndexSetCreatedEvent;
import org.graylog2.indexer.indexset.events.IndexSetDeletedEvent;
import org.graylog2.indexer.indices.TooManyAliasesException;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;

@Singleton
public class MongoIndexSetRegistry implements IndexSetRegistry {
    private final IndexSetService indexSetService;
    private final MongoIndexSet.Factory mongoIndexSetFactory;

    static class IndexSetsCache {
        private final IndexSetService indexSetService;
        private final AtomicReference<Supplier<List<IndexSetConfig>>> indexSetConfigs;

        @Inject
        IndexSetsCache(IndexSetService indexSetService,
                       EventBus serverEventBus) {
            this.indexSetService = requireNonNull(indexSetService);
            this.indexSetConfigs = new AtomicReference<>(Suppliers.memoize(this.indexSetService::findAll));
            serverEventBus.register(this);
        }

        List<IndexSetConfig> get() {
            return Collections.unmodifiableList(indexSetConfigs.get().get());
        }

        @VisibleForTesting
        void invalidate() {
            this.indexSetConfigs.set(Suppliers.memoize(this.indexSetService::findAll));
        }

        @Subscribe
        void handleIndexSetCreation(IndexSetCreatedEvent indexSetCreatedEvent) {
            this.invalidate();
        }

        @Subscribe
        void handleIndexSetDeletion(IndexSetDeletedEvent indexSetDeletedEvent) {
            this.invalidate();
        }
    }

    private final IndexSetsCache indexSetsCache;

    @Inject
    public MongoIndexSetRegistry(IndexSetService indexSetService,
                                 MongoIndexSet.Factory mongoIndexSetFactory,
                                 IndexSetsCache indexSetsCache) {
        this.indexSetService = indexSetService;
        this.mongoIndexSetFactory = requireNonNull(mongoIndexSetFactory);
        this.indexSetsCache = indexSetsCache;
    }

    private Set<MongoIndexSet> findAllMongoIndexSets() {
        final List<IndexSetConfig> configs = this.indexSetsCache.get();
        final ImmutableSet.Builder<MongoIndexSet> mongoIndexSets = ImmutableSet.builder();
        for (IndexSetConfig config : configs) {
            final MongoIndexSet mongoIndexSet = mongoIndexSetFactory.create(config);
            mongoIndexSets.add(mongoIndexSet);
        }
        return mongoIndexSets.build();
    }

    @Override
    public Set<IndexSet> getAll() {
        return ImmutableSet.copyOf(findAllMongoIndexSets());
    }

    @Override
    public Optional<IndexSet> get(final String indexSetId) {
        return this.indexSetsCache.get()
                .stream()
                .filter(indexSet -> Objects.equals(indexSet.id(), indexSetId))
                .map(indexSetConfig -> (IndexSet) mongoIndexSetFactory.create(indexSetConfig))
                .findFirst();
    }

    @Override
    public Optional<IndexSet> getForIndex(String indexName) {
        return findAllMongoIndexSets()
                .stream()
                .filter(indexSet -> indexSet.isManagedIndex(indexName))
                .map(indexSet -> (IndexSet) indexSet)
                .findFirst();
    }

    @Override
    public Set<IndexSet> getForIndices(Collection<String> indices) {
        final Set<? extends IndexSet> indexSets = findAllMongoIndexSets();
        final ImmutableSet.Builder<IndexSet> resultBuilder = ImmutableSet.builder();
        for (IndexSet indexSet : indexSets) {
            for (String index : indices) {
                if (indexSet.isManagedIndex(index)) {
                    resultBuilder.add(indexSet);
                }
            }
        }

        return resultBuilder.build();
    }

    @Override
    public Set<IndexSet> getFromIndexConfig(Collection<IndexSetConfig> indexSetConfigs) {
        return indexSetConfigs.stream()
                .map(mongoIndexSetFactory::create)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public IndexSet getDefault() {
        return mongoIndexSetFactory.create(indexSetService.getDefault());
    }

    @Override
    public String[] getManagedIndices() {
        return findAllMongoIndexSets().stream()
                .flatMap(indexSet -> Stream.of(indexSet.getManagedIndices()))
                .toArray(String[]::new);
    }

    @Override
    public boolean isManagedIndex(String indexName) {
        return isManagedIndex(findAllMongoIndexSets(), indexName);
    }

    @Override
    public Map<String, Boolean> isManagedIndex(Collection<String> indices) {
        final Set<MongoIndexSet> indexSets = findAllMongoIndexSets();
        return indices.stream()
                .collect(Collectors.toMap(Function.identity(), index -> isManagedIndex(indexSets, index)));
    }

    private boolean isManagedIndex(Collection<? extends IndexSet> indexSets, String index) {
        return indexSets.stream()
                .anyMatch(indexSet -> indexSet.isManagedIndex(index));
    }

    private String[] doWithWritableIndices(Function<MongoIndexSet, String> fn) {
        return findAllMongoIndexSets().stream()
                .filter(indexSet -> indexSet.getConfig().isWritable())
                .map(fn)
                .toArray(String[]::new);
    }

    @Override
    public String[] getIndexWildcards() {
        return doWithWritableIndices(MongoIndexSet::getIndexWildcard);
    }

    @Override
    public String[] getWriteIndexAliases() {
        return doWithWritableIndices(MongoIndexSet::getWriteIndexAlias);
    }

    @Override
    public boolean isUp() {
        return findAllMongoIndexSets().stream()
                .filter(indexSet -> indexSet.getConfig().isWritable())
                .allMatch(MongoIndexSet::isUp);
    }

    @Override
    public boolean isCurrentWriteIndexAlias(String indexName) {
        return findAllMongoIndexSets().stream()
                .anyMatch(indexSet -> indexSet.isWriteIndexAlias(indexName));
    }

    @Override
    public boolean isCurrentWriteIndex(String indexName) throws TooManyAliasesException {
        return getForIndex(indexName)
                .map(indexSet -> Objects.equals(indexSet.getActiveWriteIndex(), indexName))
                .orElse(false);
    }

    @Nonnull
    @Override
    public Iterator<IndexSet> iterator() {
        return getAll().iterator();
    }
}
