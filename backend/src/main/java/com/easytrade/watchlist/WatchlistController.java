package com.easytrade.watchlist;

import com.easytrade.common.ApiException;
import com.easytrade.stock.KoreanStockCatalog;
import com.easytrade.stock.StockPrice;
import com.easytrade.stock.StockPriceProvider;
import com.easytrade.user.User;
import com.easytrade.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
public class WatchlistController {

    private final UserRepository userRepository;
    private final WatchlistRepository watchlistRepository;
    private final StockPriceProvider stockPriceProvider;
    private final KoreanStockCatalog koreanStockCatalog;

    public WatchlistController(
            UserRepository userRepository,
            WatchlistRepository watchlistRepository,
            StockPriceProvider stockPriceProvider,
            KoreanStockCatalog koreanStockCatalog
    ) {
        this.userRepository = userRepository;
        this.watchlistRepository = watchlistRepository;
        this.stockPriceProvider = stockPriceProvider;
        this.koreanStockCatalog = koreanStockCatalog;
    }

    @GetMapping
    public List<WatchlistResponse> watchlist(@AuthenticationPrincipal User loginUser) {
        User user = getUser(loginUser);
        return watchlistRepository.findAllByUserOrderByIdDesc(user).stream()
                .map(this::toResponse)
                .toList();
    }

    @PostMapping("/{stockCode}")
    @Transactional
    public WatchlistResponse add(@AuthenticationPrincipal User loginUser, @PathVariable String stockCode) {
        User user = getUser(loginUser);
        StockPrice stock = stockPriceProvider.findByCode(stockCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."));

        WatchlistItem item = watchlistRepository.findByUserAndStockCode(user, stock.code())
                .orElseGet(() -> watchlistRepository.save(new WatchlistItem(user, stock.code(), catalogName(stock))));

        return toResponse(item);
    }

    @DeleteMapping("/{stockCode}")
    @Transactional
    public void delete(@AuthenticationPrincipal User loginUser, @PathVariable String stockCode) {
        User user = getUser(loginUser);
        watchlistRepository.deleteByUserAndStockCode(user, stockCode);
    }

    private User getUser(User loginUser) {
        return userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));
    }

    private WatchlistResponse toResponse(WatchlistItem item) {
        StockPrice stock = stockPriceProvider.findByCode(item.getStockCode())
                .orElse(new StockPrice(item.getStockCode(), item.getStockName(), BigDecimal.ZERO, BigDecimal.ZERO));

        return new WatchlistResponse(stock.code(), catalogName(stock, item.getStockName()), stock.price(), stock.changeRate());
    }

    private String catalogName(StockPrice stock) {
        return catalogName(stock, stock.name());
    }

    private String catalogName(StockPrice stock, String fallback) {
        return koreanStockCatalog.search(stock.code()).stream()
                .filter(info -> info.code().equals(stock.code()))
                .findFirst()
                .map(KoreanStockCatalog.StockInfo::name)
                .orElse(fallback);
    }

    public record WatchlistResponse(
            String code,
            String name,
            BigDecimal price,
            BigDecimal changeRate
    ) {
    }
}
