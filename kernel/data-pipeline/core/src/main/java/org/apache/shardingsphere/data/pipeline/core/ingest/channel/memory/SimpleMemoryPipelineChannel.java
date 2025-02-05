/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.data.pipeline.core.ingest.channel.memory;

import lombok.SneakyThrows;
import org.apache.shardingsphere.data.pipeline.api.ingest.channel.AckCallback;
import org.apache.shardingsphere.data.pipeline.api.ingest.channel.PipelineChannel;
import org.apache.shardingsphere.data.pipeline.api.ingest.record.Record;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Simple memory pipeline channel.
 */
public final class SimpleMemoryPipelineChannel implements PipelineChannel {
    
    private final BlockingQueue<Record> queue;
    
    private final AckCallback ackCallback;
    
    public SimpleMemoryPipelineChannel(final int blockQueueSize, final AckCallback ackCallback) {
        this.queue = new ArrayBlockingQueue<>(blockQueueSize);
        this.ackCallback = ackCallback;
    }
    
    @SneakyThrows(InterruptedException.class)
    @Override
    public void pushRecord(final Record dataRecord) {
        queue.put(dataRecord);
    }
    
    @SneakyThrows(InterruptedException.class)
    // TODO thread-safe?
    @Override
    public List<Record> fetchRecords(final int batchSize, final int timeout, final TimeUnit timeUnit) {
        List<Record> result = new ArrayList<>(batchSize);
        long start = System.currentTimeMillis();
        while (batchSize > queue.size()) {
            if (timeUnit.toMillis(timeout) <= System.currentTimeMillis() - start) {
                break;
            }
            TimeUnit.MILLISECONDS.sleep(100L);
        }
        queue.drainTo(result, batchSize);
        return result;
    }
    
    @Override
    public void ack(final List<Record> records) {
        ackCallback.onAck(records);
    }
    
    @Override
    public void close() {
        queue.clear();
    }
}
