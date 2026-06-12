package com.easytrade.trade;

import com.easytrade.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {

    List<Trade> findAllByUserOrderByTradedAtDesc(User user);
}
