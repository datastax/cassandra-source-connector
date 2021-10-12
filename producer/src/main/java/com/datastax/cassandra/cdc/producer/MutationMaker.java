/**
 * Copyright DataStax, Inc 2021.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datastax.cassandra.cdc.producer;

import com.datastax.cassandra.cdc.producer.exceptions.CassandraConnectorTaskException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Responsible for generating ChangeRecord and/or TombstoneRecord for create/update/delete events, as well as EOF events.
 */
@Slf4j
public class MutationMaker<T> {
    ProducerConfig config;

    public MutationMaker(ProducerConfig config) {
        this.config = config;
    }

    public void insert(String cluster, UUID node, CommitLogPosition offsetPosition,
                       long tsMicro, RowData data, BlockingConsumer<Mutation<T>> consumer,
                       String md5Digest, T t) {
        createRecord(cluster, node, offsetPosition, tsMicro, data, consumer, md5Digest, t);
    }

    public void update(String cluster, UUID node, CommitLogPosition offsetPosition,
                       long tsMicro, RowData data, BlockingConsumer<Mutation<T>> consumer,
                       String md5Digest, T t) {
        createRecord(cluster, node, offsetPosition, tsMicro, data, consumer, md5Digest, t);
    }

    public void delete(String cluster, UUID node, CommitLogPosition offsetPosition,
                       long tsMicro, RowData data, BlockingConsumer<Mutation<T>> consumer,
                       String md5Digest, T t) {
        createRecord(cluster, node, offsetPosition, tsMicro,
                data, consumer, md5Digest, t);
    }

    private void createRecord(String cluster, UUID node, CommitLogPosition offsetPosition,
                              long tsMicro, RowData data, BlockingConsumer<Mutation<T>> consumer,
                              String md5Digest, T t) {
        // TODO: filter columns
        RowData filteredData = data;

        SourceInfo source = new SourceInfo(cluster, node);
        Mutation<T> record = new Mutation<T>(offsetPosition, source, filteredData, tsMicro, md5Digest, t);
        try {
            consumer.accept(record);
        }
        catch (InterruptedException e) {
            log.error("Interruption while enqueuing Change Event {}", record.toString());
            throw new CassandraConnectorTaskException("Enqueuing has been interrupted: ", e);
        }
    }
}
