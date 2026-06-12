package com.easytrade.stock;

import com.easytrade.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockPriceProvider stockPriceProvider;
    private final KoreanStockCatalog koreanStockCatalog;

    public StockController(StockPriceProvider stockPriceProvider, KoreanStockCatalog koreanStockCatalog) {
        this.stockPriceProvider = stockPriceProvider;
        this.koreanStockCatalog = koreanStockCatalog;
    }

    @GetMapping("/search")
    public StockPrice search(@RequestParam String query) {
        if (query == null || query.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "검색어를 입력해주세요.");
        }

        return stockPriceProvider.search(query)
                .map(stock -> applyCatalogName(query, stock))
                .or(() -> koreanStockCatalog.search(query).stream()
                        .findFirst()
                        .flatMap(stock -> stockPriceProvider.findByCode(stock.code())
                                .map(price -> withName(price, stock.name()))))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "검색 결과가 없습니다."));
    }

    @GetMapping("/{code}")
    public StockPrice findByCode(@PathVariable String code) {
        return stockPriceProvider.findByCode(code)
                .map(stock -> applyCatalogName(code, stock))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."));
    }

    @GetMapping("/popular")
    public List<StockPrice> popularStocks() {
        return stockPriceProvider.popularStocks().stream()
                .map(stock -> applyCatalogName(stock.code(), stock))
                .toList();
    }

    @GetMapping("/market")
    public List<StockPrice> marketStocks(
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "60") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return koreanStockCatalog.search(query).stream()
                .limit(safeLimit)
                .map(stock -> stockPriceProvider.findByCode(stock.code())
                        .map(price -> withName(price, stock.name()))
                        .orElseGet(() -> new StockPrice(stock.code(), stock.name(), BigDecimal.ZERO, BigDecimal.ZERO)))
                .toList();
    }

    @GetMapping("/ranking")
    public List<StockPrice> ranking(
            @RequestParam(defaultValue = "rising") String type,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 100));
        return marketStocks(null, 100).stream()
                .sorted((left, right) -> compareByRankingType(type, left, right))
                .limit(safeLimit)
                .toList();
    }

    private int compareByRankingType(String type, StockPrice left, StockPrice right) {
        return switch (type) {
            case "falling" -> left.changeRate().compareTo(right.changeRate());
            case "price" -> right.price().compareTo(left.price());
            default -> right.changeRate().compareTo(left.changeRate());
        };
    }

    private StockPrice applyCatalogName(String query, StockPrice stock) {
        return koreanStockCatalog.search(query).stream()
                .filter(info -> info.code().equals(stock.code()))
                .findFirst()
                .map(info -> withName(stock, info.name()))
                .orElse(stock);
    }

    private StockPrice withName(StockPrice stock, String name) {
        return new StockPrice(stock.code(), name, stock.price(), stock.changeRate());
    }
}
