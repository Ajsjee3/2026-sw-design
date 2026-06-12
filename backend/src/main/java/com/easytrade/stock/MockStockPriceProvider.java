package com.easytrade.stock;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Component
public class MockStockPriceProvider implements StockPriceProvider {

    private final List<StockPrice> stocks = List.of(
            new StockPrice("005930", "Samsung Electronics", BigDecimal.valueOf(71000), BigDecimal.valueOf(1.5)),
            new StockPrice("000660", "SK hynix", BigDecimal.valueOf(128000), BigDecimal.valueOf(-0.8)),
            new StockPrice("035420", "NAVER", BigDecimal.valueOf(195000), BigDecimal.valueOf(2.3)),
            new StockPrice("051910", "LG Chem", BigDecimal.valueOf(412000), BigDecimal.valueOf(0.5)),
            new StockPrice("035720", "Kakao", BigDecimal.valueOf(48500), BigDecimal.valueOf(-1.2)),
            new StockPrice("005380", "Hyundai Motor", BigDecimal.valueOf(245000), BigDecimal.valueOf(1.8)),
            new StockPrice("068270", "Celltrion", BigDecimal.valueOf(175000), BigDecimal.valueOf(3.2)),
            new StockPrice("006400", "Samsung SDI", BigDecimal.valueOf(387000), BigDecimal.valueOf(-0.3))
    );

    @Override
    public Optional<StockPrice> findByCode(String code) {
        return stocks.stream()
                .filter(stock -> stock.code().equals(code))
                .findFirst();
    }

    @Override
    public Optional<StockPrice> search(String query) {
        String normalizedQuery = query.toLowerCase();
        return stocks.stream()
                .filter(stock -> stock.code().equals(query) || stock.name().toLowerCase().contains(normalizedQuery))
                .findFirst();
    }

    @Override
    public List<StockPrice> popularStocks() {
        return stocks.stream().limit(3).toList();
    }
}
