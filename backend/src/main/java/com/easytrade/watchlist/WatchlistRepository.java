package com.easytrade.watchlist;

import com.easytrade.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {

    List<WatchlistItem> findAllByUserOrderByIdDesc(User user);

    Optional<WatchlistItem> findByUserAndStockCode(User user, String stockCode);

    boolean existsByUserAndStockCode(User user, String stockCode);

    void deleteByUserAndStockCode(User user, String stockCode);
}
