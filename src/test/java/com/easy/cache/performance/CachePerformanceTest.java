package com.easy.cache.performance;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.easy.cache.core.Cache;
import com.easy.cache.core.CacheManager;
import com.easy.cache.core.LocalCache;
import com.easy.cache.support.JdkSerializer;
import com.easy.cache.support.JsonSerializer;

/**
 * 缓存性能测试类
 * 
 * 注意：此测试类需要JMH依赖，可能需要较长时间运行
 */
@State(Scope.Benchmark)
public class CachePerformanceTest {
    
    private Cache<String, TestData> localCache;
    private Cache<String, TestData> bloomFilterCache;
    private JdkSerializer jdkSerializer;
    private JsonSerializer jsonSerializer;
    
    @BeforeEach
    public void setUp() {
        CacheManager cacheManager = CacheManager.getInstance();
        localCache = new LocalCache<>("perf-test-cache");
        bloomFilterCache = cacheManager.wrapWithBloomFilter(localCache, 10000, 0.01);
        jdkSerializer = new JdkSerializer();
        jsonSerializer = new JsonSerializer();
        
        // 预热缓存
        for (int i = 0; i < 1000; i++) {
            TestData data = new TestData(i, "name-" + i);
            localCache.put("key-" + i, data);
        }
    }
    
    @Test
    @Tag("performance")
    public void launchBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(CachePerformanceTest.class.getSimpleName())
                .forks(1)
                .warmupIterations(3)
                .measurementIterations(5)
                .build();
        
        new Runner(opt).run();
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public TestData testLocalCacheGet() {
        return localCache.get("key-500");
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public TestData testBloomFilterCacheGet() {
        return bloomFilterCache.get("key-500");
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public byte[] testJdkSerialization() {
        TestData data = new TestData(1, "test-data");
        return jdkSerializer.serialize(data);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public byte[] testJsonSerialization() {
        TestData data = new TestData(1, "test-data");
        return jsonSerializer.serialize(data);
    }
    
    /**
     * 测试数据类
     */
    static class TestData {
        private int id;
        private String name;
        
        public TestData(int id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
    }
} 