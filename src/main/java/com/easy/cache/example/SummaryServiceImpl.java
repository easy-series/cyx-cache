package com.easy.cache.example;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

/**
 * 汇总服务实现类
 */
@Service
public class SummaryServiceImpl implements SummaryService {
    private int count = 0;

    @Override
    public BigDecimal summaryOfToday(long categoryId) {
        System.out.println("计算汇总数据: " + categoryId);
        // 模拟计算汇总数据
        count++;
        return new BigDecimal("100.00").add(new BigDecimal(count));
    }
} 