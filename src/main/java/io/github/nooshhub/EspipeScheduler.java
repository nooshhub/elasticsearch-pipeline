/*
 *  Copyright 2021-2022 the original author or authors.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *         https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.github.nooshhub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Espipe Scheduler is used to trigger synchronization task
 *
 * @author neals
 * @since 6/4/2022
 */
@Service
public class EspipeScheduler {

    private static final Logger logger = LoggerFactory.getLogger(EspipeSampleWorker.class);

    @Autowired
    private IndexConfigRegistry indexConfigRegistry;
    @Autowired
    private JdbcPipe jdbcPipe;

    @Scheduled(fixedRate = 5000)
    public void sync() {
        indexConfigRegistry.getIndexConfigs()
                .forEach(indexConfig -> {
                    jdbcPipe.sync(indexConfig);
                });

    }
}
