package com.easytrade.holding;

import com.easytrade.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoldingRepository extends JpaRepository<Holding, Long> {

    List<Holding> findAllByUser(User user);

    Optional<Holding> findByUserAndStockCode(User user, String stockCode);
}
