package com.easytrade.holding;

import com.easytrade.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

@Entity
@Table(
        name = "holdings",
        uniqueConstraints = @UniqueConstraint(name = "uk_holding_user_stock", columnNames = {"user_id", "stock_code"})
)
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "stock_code", nullable = false, length = 20)
    private String stockCode;

    @Column(nullable = false, length = 100)
    private String stockName;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal averagePrice;

    protected Holding() {
    }

    public Holding(User user, String stockCode, String stockName, int quantity, BigDecimal averagePrice) {
        this.user = user;
        this.stockCode = stockCode;
        this.stockName = stockName;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
    }

    public Long getId() {
        return id;
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

    public BigDecimal getAveragePrice() {
        return averagePrice;
    }

    public void buy(int buyQuantity, BigDecimal buyPrice) {
        BigDecimal currentTotal = averagePrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal buyTotal = buyPrice.multiply(BigDecimal.valueOf(buyQuantity));
        int newQuantity = quantity + buyQuantity;

        this.averagePrice = currentTotal.add(buyTotal)
                .divide(BigDecimal.valueOf(newQuantity), 2, java.math.RoundingMode.HALF_UP);
        this.quantity = newQuantity;
    }

    public void sell(int sellQuantity) {
        this.quantity -= sellQuantity;
    }
}
