package com.easytrade.portfolio;

import com.easytrade.holding.Holding;
import com.easytrade.holding.HoldingRepository;
import com.easytrade.stock.StockPrice;
import com.easytrade.stock.StockPriceProvider;
import com.easytrade.user.User;
import com.easytrade.user.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
public class PortfolioController {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final StockPriceProvider stockPriceProvider;

    public PortfolioController(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            StockPriceProvider stockPriceProvider
    ) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.stockPriceProvider = stockPriceProvider;
    }

    @GetMapping
    public PortfolioResponse portfolio(@AuthenticationPrincipal User loginUser) {
        User user = userRepository.findById(loginUser.getId()).orElseThrow();

        List<HoldingResponse> holdings = holdingRepository.findAllByUser(user).stream()
                .map(this::toResponse)
                .toList();

        BigDecimal stockValue = holdings.stream()
                .map(HoldingResponse::currentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PortfolioResponse(user.getBalance(), stockValue, user.getBalance().add(stockValue), holdings);
    }

    private HoldingResponse toResponse(Holding holding) {
        StockPrice stock = stockPriceProvider.findByCode(holding.getStockCode())
                .orElse(new StockPrice(holding.getStockCode(), holding.getStockName(), BigDecimal.ZERO, BigDecimal.ZERO));

        BigDecimal currentValue = stock.price().multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal purchaseValue = holding.getAveragePrice().multiply(BigDecimal.valueOf(holding.getQuantity()));
        BigDecimal profitLoss = currentValue.subtract(purchaseValue);
        BigDecimal profitRate = purchaseValue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : profitLoss.multiply(BigDecimal.valueOf(100)).divide(purchaseValue, 2, RoundingMode.HALF_UP);

        return new HoldingResponse(
                holding.getStockCode(),
                holding.getStockName(),
                holding.getQuantity(),
                holding.getAveragePrice(),
                stock.price(),
                currentValue,
                profitLoss,
                profitRate
        );
    }

    public record PortfolioResponse(
            BigDecimal cashBalance,
            BigDecimal stockValue,
            BigDecimal totalAsset,
            List<HoldingResponse> holdings
    ) {
    }

    public record HoldingResponse(
            String stockCode,
            String stockName,
            int quantity,
            BigDecimal averagePrice,
            BigDecimal currentPrice,
            BigDecimal currentValue,
            BigDecimal profitLoss,
            BigDecimal profitRate
    ) {
    }
}
