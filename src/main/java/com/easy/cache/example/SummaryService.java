package com.easy.cache.example;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import com.easy.cache.annotation.CachePenetrationProtect;
import com.easy.cache.annotation.CacheRefresh;
import com.easy.cache.annotation.Cached;
import com.easy.cache.core.QuickConfig.CacheType;

/**
 * 汇总服务接口
 */
public interface SummaryService {
    /**
     * 获取今日汇总数据
     * 
     * @param categoryId 分类ID
     * @return 汇总数据
     */
    @Cached(expire = 3600, cacheType = CacheType.REMOTE)
    @CacheRefresh(refresh = 1, stopRefreshAfterLastAccess = 3600, timeUnit = TimeUnit.SECONDS)
    @CachePenetrationProtect
    BigDecimal summaryOfToday(long categoryId);
} 