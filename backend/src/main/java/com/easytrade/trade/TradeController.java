package com.easytrade.trade;

import com.easytrade.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;
    private final TradeRepository tradeRepository;

    public TradeController(TradeService tradeService, TradeRepository tradeRepository) {
        this.tradeService = tradeService;
        this.tradeRepository = tradeRepository;
    }

    @PostMapping("/buy")
    public TradeResponse buy(@AuthenticationPrincipal User user, @Valid @RequestBody TradeRequest request) {
        return TradeResponse.from(tradeService.buy(user, request.stockCode(), request.quantity()));
    }

    @PostMapping("/sell")
    public TradeResponse sell(@AuthenticationPrincipal User user, @Valid @RequestBody TradeRequest request) {
        return TradeResponse.from(tradeService.sell(user, request.stockCode(), request.quantity()));
    }

    @GetMapping
    public List<TradeResponse> history(@AuthenticationPrincipal User user) {
        return tradeRepository.findAllByUserOrderByTradedAtDesc(user).stream()
                .map(TradeResponse::from)
                .toList();
    }

    public record TradeRequest(
            @NotBlank String stockCode,
            @Min(1) int quantity
    ) {
    }

    public record TradeResponse(
            Long id,
            TradeType type,
            String stockCode,
            String stockName,
            int quantity,
            BigDecimal price,
            BigDecimal totalAmount,
            LocalDateTime tradedAt
    ) {
        public static TradeResponse from(Trade trade) {
            return new TradeResponse(
                    trade.getId(),
                    trade.getType(),
                    trade.getStockCode(),
                    trade.getStockName(),
                    trade.getQuantity(),
                    trade.getPrice(),
                    trade.getTotalAmount(),
                    trade.getTradedAt()
            );
        }
    }
}
