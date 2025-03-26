package com.caoyixin.cache.serialization;

import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序列化工厂，负责创建和管理各种序列化器
 */
public class SerializationFactory {

    private static final Map<String, KeyConvertorProvider> KEY_CONVERTOR_PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, ValueEncoderProvider> VALUE_ENCODER_PROVIDERS = new ConcurrentHashMap<>();
    private static final Map<String, ValueDecoderProvider> VALUE_DECODER_PROVIDERS = new ConcurrentHashMap<>();

    static {
        // 使用SPI机制加载所有提供者
        loadProviders();

        // 注册默认提供者
        registerDefaultProviders();
    }

    /**
     * 获取键转换器
     *
     * @param type 转换器类型
     * @param <K>  键类型
     * @return 键转换器
     */
    @SuppressWarnings("unchecked")
    public static <K> KeyConvertor<K> getKeyConvertor(String type) {
        KeyConvertorProvider provider = KEY_CONVERTOR_PROVIDERS.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的键转换器类型: " + type);
        }
        return (KeyConvertor<K>) provider.create();
    }

    /**
     * 获取值编码器
     *
     * @param type 编码器类型
     * @param <V>  值类型
     * @return 值编码器
     */
    @SuppressWarnings("unchecked")
    public static <V> ValueEncoder<V> getValueEncoder(String type) {
        ValueEncoderProvider provider = VALUE_ENCODER_PROVIDERS.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的值编码器类型: " + type);
        }
        return (ValueEncoder<V>) provider.create();
    }

    /**
     * 获取值解码器
     *
     * @param type 解码器类型
     * @param <V>  值类型
     * @return 值解码器
     */
    @SuppressWarnings("unchecked")
    public static <V> ValueDecoder<V> getValueDecoder(String type) {
        ValueDecoderProvider provider = VALUE_DECODER_PROVIDERS.get(type);
        if (provider == null) {
            throw new IllegalArgumentException("不支持的值解码器类型: " + type);
        }
        return (ValueDecoder<V>) provider.create();
    }

    /**
     * 注册键转换器提供者
     *
     * @param type     类型
     * @param provider 提供者
     */
    public static void registerKeyConvertorProvider(String type, KeyConvertorProvider provider) {
        KEY_CONVERTOR_PROVIDERS.put(type, provider);
    }

    /**
     * 注册值编码器提供者
     *
     * @param type     类型
     * @param provider 提供者
     */
    public static void registerValueEncoderProvider(String type, ValueEncoderProvider provider) {
        VALUE_ENCODER_PROVIDERS.put(type, provider);
    }

    /**
     * 注册值解码器提供者
     *
     * @param type     类型
     * @param provider 提供者
     */
    public static void registerValueDecoderProvider(String type, ValueDecoderProvider provider) {
        VALUE_DECODER_PROVIDERS.put(type, provider);
    }

    /**
     * 使用SPI机制加载所有提供者
     */
    private static void loadProviders() {
        ServiceLoader.load(KeyConvertorProvider.class)
                .forEach(provider -> KEY_CONVERTOR_PROVIDERS.put(provider.getType(), provider));

        ServiceLoader.load(ValueEncoderProvider.class)
                .forEach(provider -> VALUE_ENCODER_PROVIDERS.put(provider.getType(), provider));

        ServiceLoader.load(ValueDecoderProvider.class)
                .forEach(provider -> VALUE_DECODER_PROVIDERS.put(provider.getType(), provider));
    }

    /**
     * 注册默认提供者
     */
    private static void registerDefaultProviders() {
        // 字符串键转换器
        registerKeyConvertorProvider("string", new KeyConvertorProvider() {
            @Override
            public KeyConvertor<?> create() {
                return new StringKeyConvertor();
            }
        });

        // Fastjson键转换器
        registerKeyConvertorProvider("fastjson", new KeyConvertorProvider() {
            @Override
            public KeyConvertor<?> create() {
                return new FastjsonKeyConvertor();
            }
        });

        // Jackson键转换器
        registerKeyConvertorProvider("jackson", new KeyConvertorProvider() {
            @Override
            public KeyConvertor<?> create() {
                return new JacksonKeyConvertor();
            }
        });

        // Java序列化
        registerValueEncoderProvider("java", new ValueEncoderProvider() {
            @Override
            public ValueEncoder<?> create() {
                return new JavaValueEncoder();
            }
        });

        registerValueDecoderProvider("java", new ValueDecoderProvider() {
            @Override
            public ValueDecoder<?> create() {
                return new JavaValueDecoder();
            }
        });

        // Jackson序列化
        registerValueEncoderProvider("jackson", new ValueEncoderProvider() {
            @Override
            public ValueEncoder<?> create() {
                return new Jackson2ValueEncoder();
            }
        });

        registerValueDecoderProvider("jackson", new ValueDecoderProvider() {
            @Override
            public ValueDecoder<?> create() {
                return new Jackson2ValueDecoder();
            }
        });
    }

    /**
     * 键转换器提供者接口
     */
    public interface KeyConvertorProvider {
        /**
         * 获取类型
         *
         * @return 类型
         */
        default String getType() {
            return "default";
        }

        /**
         * 创建键转换器
         *
         * @return 键转换器
         */
        KeyConvertor<?> create();
    }

    /**
     * 值编码器提供者接口
     */
    public interface ValueEncoderProvider {
        /**
         * 获取类型
         *
         * @return 类型
         */
        default String getType() {
            return "default";
        }

        /**
         * 创建值编码器
         *
         * @return 值编码器
         */
        ValueEncoder<?> create();
    }

    /**
     * 值解码器提供者接口
     */
    public interface ValueDecoderProvider {
        /**
         * 获取类型
         *
         * @return 类型
         */
        default String getType() {
            return "default";
        }

        /**
         * 创建值解码器
         *
         * @return 值解码器
         */
        ValueDecoder<?> create();
    }
}