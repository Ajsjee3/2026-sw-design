package com.easytrade.stock;

import java.util.List;
import java.util.Optional;

public interface StockPriceProvider {

    Optional<StockPrice> findByCode(String code);

    Optional<StockPrice> search(String query);

    List<StockPrice> popularStocks();
}
