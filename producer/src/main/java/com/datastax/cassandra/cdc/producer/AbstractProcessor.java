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

import java.io.IOException;

import lombok.extern.slf4j.Slf4j;

/**
 * An abstract processor designed to be a convenient superclass for all concrete processors for Cassandra
 * connector task. The class handles concurrency control for starting and stopping the processor.
 */
@Slf4j
public abstract class AbstractProcessor {

    private final String name;
    private final long delay;
    private boolean running;

    public AbstractProcessor(String name, long delayMillis) {
        this.name = name;
        this.delay = delayMillis;
        this.running = false;
    }

    /**
     * The actual work the processor is doing. This method will be executed in a while loop
     * until processor stops or encounters exception.
     */
    public abstract void process() throws InterruptedException, IOException;

    /**
     * Override initialize to initialize resources before starting the processor
     */
    public void initialize() throws Exception {
    }

    public boolean isRunning() {
        return running;
    }

    public void start() throws Exception {
        if (running) {
            log.warn("Ignoring start signal for {} because it is already started", name);
            return;
        }

        log.info("Started {}", name);
        running = true;
        while (isRunning()) {
            try {
                process();
                Thread.sleep(delay);
            } catch(Throwable t) {
                log.error("error:", t);
                throw t;
            }
        }
        log.info("Stopped {}", name);
    }

    public void stop() throws Exception {
        if (isRunning()) {
            log.info("Stopping {}", name);
            running = false;
        }
    }

    public String getName() {
        return name;
    }
}
