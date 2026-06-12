package com.easytrade.trade;

import com.easytrade.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TradeType type;

    @Column(nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime tradedAt;

    protected Trade() {
    }

    public Trade(User user, TradeType type, String stockCode, String stockName, int quantity, BigDecimal price) {
        this.user = user;
        this.type = type;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.quantity = quantity;
        this.price = price;
        this.totalAmount = price.multiply(BigDecimal.valueOf(quantity));
        this.tradedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TradeType getType() {
        return type;
    }

    public String getStockCode() {
        return stockCode;
    }

    public String getStockName() {
        return stockName;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public LocalDateTime getTradedAt() {
        return tradedAt;
    }
}
