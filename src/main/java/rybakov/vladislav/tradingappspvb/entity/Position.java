package rybakov.vladislav.tradingappspvb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "positions",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"account_id", "seccode"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class Position {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seccode", nullable = false)
    private Instrument instrument;

    @Column(nullable = false)
    private Long quantity = 0L;

    @Column(nullable = false)
    private Long reservedQuantity = 0L;

    public Long getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}