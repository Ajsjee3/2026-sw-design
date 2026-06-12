package com.easytrade.stock;

import java.math.BigDecimal;

public record StockPrice(
        String code,
        String name,
        BigDecimal price,
        BigDecimal changeRate
) {
}
