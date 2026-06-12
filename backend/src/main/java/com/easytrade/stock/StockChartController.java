package com.easytrade.stock;

import com.easytrade.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockChartController {

    private final StockPriceProvider stockPriceProvider;
    private final KoreanStockCatalog koreanStockCatalog;

    public StockChartController(StockPriceProvider stockPriceProvider, KoreanStockCatalog koreanStockCatalog) {
        this.stockPriceProvider = stockPriceProvider;
        this.koreanStockCatalog = koreanStockCatalog;
    }

    @GetMapping("/{code}/chart")
    public ChartResponse chart(@PathVariable String code, @RequestParam(defaultValue = "1M") String period) {
        StockPrice stock = stockPriceProvider.findByCode(code)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."));

        int points = switch (period) {
            case "1D" -> 7;
            case "1W" -> 10;
            case "3M" -> 16;
            default -> 12;
        };

        List<ChartPoint> chartPoints = new ArrayList<>();
        BigDecimal base = stock.price();
        for (int index = 0; index < points; index++) {
            int centered = index - (points / 2);
            BigDecimal wave = BigDecimal.valueOf(((index % 5) - 2) * 0.006);
            BigDecimal trend = stock.changeRate()
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(centered))
                    .divide(BigDecimal.valueOf(points), 6, RoundingMode.HALF_UP);
            BigDecimal multiplier = BigDecimal.ONE.add(wave).add(trend);
            BigDecimal price = base.multiply(multiplier).setScale(0, RoundingMode.HALF_UP);
            chartPoints.add(new ChartPoint(label(period, index + 1), price));
        }

        return new ChartResponse(stock.code(), catalogName(stock), period, chartPoints);
    }

    private String label(String period, int index) {
        return switch (period) {
            case "1D" -> index + "H";
            case "1W" -> "D" + index;
            case "3M" -> "W" + index;
            default -> "D" + index;
        };
    }

    private String catalogName(StockPrice stock) {
        return koreanStockCatalog.search(stock.code()).stream()
                .filter(info -> info.code().equals(stock.code()))
                .findFirst()
                .map(KoreanStockCatalog.StockInfo::name)
                .orElse(stock.name());
    }

    public record ChartResponse(String code, String name, String period, List<ChartPoint> points) {
    }

    public record ChartPoint(String label, BigDecimal price) {
    }
}
