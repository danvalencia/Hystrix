/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.hystrix.contrib.codahalemetricspublisher;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserMetrics;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.functions.Func0;

/**
 * Implementation of {@link HystrixMetricsPublisherCollapser} using Coda Hale Metrics (https://github.com/codahale/metrics)
 */
public class HystrixCodaHaleMetricsPublisherCollapser implements HystrixMetricsPublisherCollapser {
    private final HystrixCollapserKey key;
    private final HystrixCollapserMetrics metrics;
    private final HystrixCollapserProperties properties;
    private final MetricRegistry metricRegistry;
    private final String metricType;

    static final Logger logger = LoggerFactory.getLogger(HystrixCodaHaleMetricsPublisherCollapser.class);

    public HystrixCodaHaleMetricsPublisherCollapser(HystrixCollapserKey collapserKey, HystrixCollapserMetrics metrics, HystrixCollapserProperties properties, MetricRegistry metricRegistry) {
        this.key = collapserKey;
        this.metrics = metrics;
        this.properties = properties;
        this.metricRegistry = metricRegistry;
        this.metricType = key.name();
    }

    /**
     * An implementation note.  If there's a version mismatch between hystrix-core and hystrix-codahale-metrics-publisher,
     * the code below may reference a HystrixRollingNumberEvent that does not exist in hystrix-core.  If this happens,
     * a j.l.NoSuchFieldError occurs.  Since this data is not being generated by hystrix-core, it's safe to count it as 0
     * and we should log an error to get users to update their dependency set.
     */
    @Override
    public void initialize() {
        // allow monitor to know exactly at what point in time these stats are for so they can be plotted accurately
        metricRegistry.register(createMetricName("currentTime"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return System.currentTimeMillis();
            }
        });

        // cumulative counts
        safelyCreateCumulativeCountForEvent("countRequestsBatched", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSER_REQUEST_BATCHED;
            }
        });
        safelyCreateCumulativeCountForEvent("countBatches", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSER_BATCH;
            }
        });
        safelyCreateCumulativeCountForEvent("countResponsesFromCache", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
            }
        });

        // rolling counts
        safelyCreateRollingCountForEvent("rollingRequestsBatched", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSER_REQUEST_BATCHED;
            }
        });
        safelyCreateRollingCountForEvent("rollingBatches", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.COLLAPSER_BATCH;
            }
        });
        safelyCreateRollingCountForEvent("rollingCountResponsesFromCache", new Func0<HystrixRollingNumberEvent>() {

            @Override
            public HystrixRollingNumberEvent call() {
                return HystrixRollingNumberEvent.RESPONSE_FROM_CACHE;
            }
        });

        // batch size metrics
        metricRegistry.register(createMetricName("batchSize_mean"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizeMean();
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_25"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(25);
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_50"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(50);
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_75"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(75);
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_90"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(90);
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_99"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(99);
            }
        });
        metricRegistry.register(createMetricName("batchSize_percentile_995"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getBatchSizePercentile(99.5);
            }
        });

        // shard size metrics
        metricRegistry.register(createMetricName("shardSize_mean"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizeMean();
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_25"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(25);
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_50"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(50);
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_75"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(75);
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_90"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(90);
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_99"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(99);
            }
        });
        metricRegistry.register(createMetricName("shardSize_percentile_995"), new Gauge<Integer>() {
            @Override
            public Integer getValue() {
                return metrics.getShardSizePercentile(99.5);
            }
        });

        // properties (so the values can be inspected and monitored)
        metricRegistry.register(createMetricName("propertyValue_rollingStatisticalWindowInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.metricsRollingStatisticalWindowInMilliseconds().get();
            }
        });

        metricRegistry.register(createMetricName("propertyValue_requestCacheEnabled"), new Gauge<Boolean>() {
            @Override
            public Boolean getValue() {
                return properties.requestCacheEnabled().get();
            }
        });

        metricRegistry.register(createMetricName("propertyValue_maxRequestsInBatch"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.maxRequestsInBatch().get();
            }
        });

        metricRegistry.register(createMetricName("propertyValue_timerDelayInMilliseconds"), new Gauge<Number>() {
            @Override
            public Number getValue() {
                return properties.timerDelayInMilliseconds().get();
            }
        });
    }

    protected String createMetricName(String name) {
        return MetricRegistry.name("", metricType, name);
    }

    protected void createCumulativeCountForEvent(final String name, final HystrixRollingNumberEvent event) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return metrics.getCumulativeCount(event);
            }
        });
    }

    protected void safelyCreateCumulativeCountForEvent(final String name, final Func0<HystrixRollingNumberEvent> eventThunk) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return metrics.getCumulativeCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    logger.error("While publishing CodaHale metrics, error looking up eventType for : " + name + ".  Please check that all Hystrix versions are the same!");
                    return 0L;
                }
            }
        });
    }

    protected void createRollingCountForEvent(final String name, final HystrixRollingNumberEvent event) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return metrics.getRollingCount(event);
            }
        });
    }

    protected void safelyCreateRollingCountForEvent(final String name, final Func0<HystrixRollingNumberEvent> eventThunk) {
        metricRegistry.register(createMetricName(name), new Gauge<Long>() {
            @Override
            public Long getValue() {
                try {
                    return metrics.getRollingCount(eventThunk.call());
                } catch (NoSuchFieldError error) {
                    logger.error("While publishing CodaHale metrics, error looking up eventType for : " + name + ".  Please check that all Hystrix versions are the same!");
                    return 0L;
                }
            }
        });
    }
}
