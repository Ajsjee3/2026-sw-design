package com.easytrade.stock;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Primary
@Component
public class FallbackStockPriceProvider implements StockPriceProvider {

    private final MockStockPriceProvider mockStockPriceProvider;
    private final Optional<KisStockPriceProvider> kisStockPriceProvider;

    public FallbackStockPriceProvider(
            MockStockPriceProvider mockStockPriceProvider,
            Optional<KisStockPriceProvider> kisStockPriceProvider
    ) {
        this.mockStockPriceProvider = mockStockPriceProvider;
        this.kisStockPriceProvider = kisStockPriceProvider;
    }

    @Override
    public Optional<StockPrice> findByCode(String code) {
        return kisStockPriceProvider
                .flatMap(provider -> provider.findByCode(code))
                .or(() -> mockStockPriceProvider.findByCode(code));
    }

    @Override
    public Optional<StockPrice> search(String query) {
        return kisStockPriceProvider
                .flatMap(provider -> provider.search(query))
                .or(() -> mockStockPriceProvider.search(query));
    }

    @Override
    public List<StockPrice> popularStocks() {
        return kisStockPriceProvider
                .map(KisStockPriceProvider::popularStocks)
                .filter(stocks -> !stocks.isEmpty())
                .orElseGet(mockStockPriceProvider::popularStocks);
    }
}
