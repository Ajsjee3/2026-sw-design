package com.easytrade.trade;

import com.easytrade.common.ApiException;
import com.easytrade.holding.Holding;
import com.easytrade.holding.HoldingRepository;
import com.easytrade.stock.StockPrice;
import com.easytrade.stock.StockPriceProvider;
import com.easytrade.user.User;
import com.easytrade.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class TradeService {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final TradeRepository tradeRepository;
    private final StockPriceProvider stockPriceProvider;

    public TradeService(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            TradeRepository tradeRepository,
            StockPriceProvider stockPriceProvider
    ) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.tradeRepository = tradeRepository;
        this.stockPriceProvider = stockPriceProvider;
    }

    @Transactional
    public Trade buy(User loginUser, String stockCode, int quantity) {
        validateQuantity(quantity);

        User user = getUser(loginUser);
        StockPrice stock = getStock(stockCode);
        BigDecimal totalAmount = stock.price().multiply(BigDecimal.valueOf(quantity));

        if (user.getBalance().compareTo(totalAmount) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "잔액이 부족합니다.");
        }

        user.decreaseBalance(totalAmount);

        Holding holding = holdingRepository.findByUserAndStockCode(user, stock.code())
                .orElseGet(() -> new Holding(user, stock.code(), stock.name(), 0, BigDecimal.ZERO));

        holding.buy(quantity, stock.price());
        holdingRepository.save(holding);

        return tradeRepository.save(new Trade(user, TradeType.BUY, stock.code(), stock.name(), quantity, stock.price()));
    }

    @Transactional
    public Trade sell(User loginUser, String stockCode, int quantity) {
        validateQuantity(quantity);

        User user = getUser(loginUser);
        StockPrice stock = getStock(stockCode);
        Holding holding = holdingRepository.findByUserAndStockCode(user, stock.code())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "보유하지 않은 종목입니다."));

        if (holding.getQuantity() < quantity) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "보유 수량이 부족합니다.");
        }

        BigDecimal totalAmount = stock.price().multiply(BigDecimal.valueOf(quantity));
        user.increaseBalance(totalAmount);
        holding.sell(quantity);

        if (holding.getQuantity() == 0) {
            holdingRepository.delete(holding);
        }

        return tradeRepository.save(new Trade(user, TradeType.SELL, stock.code(), stock.name(), quantity, stock.price()));
    }

    private User getUser(User loginUser) {
        return userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "사용자 정보를 찾을 수 없습니다."));
    }

    private StockPrice getStock(String stockCode) {
        return stockPriceProvider.findByCode(stockCode)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "종목을 찾을 수 없습니다."));
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "수량은 1주 이상이어야 합니다.");
        }
    }
}
