package rybakov.vladislav.tradingappspvb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import rybakov.vladislav.tradingappspvb.enums.OrderSide;
import rybakov.vladislav.tradingappspvb.enums.OrderStatus;
import rybakov.vladislav.tradingappspvb.time.TimePoints;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccode", nullable = false)
    private Instrument instrument;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private OrderSide side;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal price;

    @Column(nullable = false)
    private Long quantity;

    @Column(nullable = false)
    private Long filledQuantity = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status = OrderStatus.NEW;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @PrePersist
    private void normalizeTimestampsOnCreate() {
        createdAt = TimePoints.micros(createdAt);
        updatedAt = TimePoints.micros(updatedAt);
        priorityAt = TimePoints.micros(priorityAt);
    }

    @PreUpdate
    private void normalizeUpdatedAt() {
        // Truncate only. Assigning Instant.now() here would run at flush,
        // after the response DTO has already been built.
        updatedAt = TimePoints.micros(updatedAt);
    }

    public Long getRemainingQuantity() {
        return quantity - filledQuantity;
    }

    @Column(nullable = false)
    private Instant priorityAt = Instant.now();
}