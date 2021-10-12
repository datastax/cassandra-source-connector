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

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Implementation of {@link CommitLogTransfer} which deletes commit logs.
 */
public class ArchiveCommitLogTransfer implements CommitLogTransfer {

    ProducerConfig config;

    ArchiveCommitLogTransfer(ProducerConfig config) {
        this.config = config;
    }

    @Override
    public void onSuccessTransfer(Path file) {
        CommitLogUtil.moveCommitLog(file.toFile(), Paths.get(config.cdcWorkingDir, CommitLogReaderService.ARCHIVE_FOLDER));
    }

    @Override
    public void onErrorTransfer(Path file) {
        CommitLogUtil.moveCommitLog(file.toFile(), Paths.get(config.cdcWorkingDir, CommitLogReaderService.ERROR_FOLDER));
    }

    /**
     * Move CL on error to the cdc directory
     */
    @Override
    public void recycleErrorCommitLogFiles(Path cdcDir) {
        for(File file : CommitLogUtil.getCommitLogs(Paths.get(config.cdcWorkingDir, CommitLogReaderService.ERROR_FOLDER).toFile())) {
            CommitLogUtil.moveCommitLog(file, cdcDir);
        }
    }
}
