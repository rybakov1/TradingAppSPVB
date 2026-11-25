package rybakov.vladislav.tradingappspvb.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import rybakov.vladislav.tradingappspvb.enums.Currency;
import rybakov.vladislav.tradingappspvb.enums.InstrumentType;

import java.math.BigDecimal;

@Entity
@Table(name = "instruments")
@Getter
@Setter
@NoArgsConstructor
public class Instrument {

    @Id
    @Column(length = 32)
    private String seccode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private InstrumentType type;

    @Column(nullable = false)
    private Long lotSize;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal minStep;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Currency currency;
}