package com.easytrade.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public User(String name, String email, String password, String nickname, BigDecimal balance) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.balance = balance;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void decreaseBalance(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }

    public void increaseBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }
}
