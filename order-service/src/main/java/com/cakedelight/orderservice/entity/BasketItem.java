package com.cakedelight.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "basket_items")
@Getter
@Setter
@NoArgsConstructor
public class BasketItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", nullable = false)
    private Basket basket;

    // References catalog-service's cakes.id by value only, no FK — same
    // cross-service-DB reasoning as rating-service's Rating.cakeId
    // (CLAUDE.md §10).
    @Column(name = "cake_id", nullable = false)
    private Long cakeId;

    // Captured from catalog-service at add-time (CLAUDE.md §5.2). Basket
    // display never needs a live catalog call, and checkout totals off these
    // snapshots rather than re-validating price/availability a second time —
    // a deliberate simplification for this capstone's scope, documented here
    // and in README rather than re-fetching on every checkout.
    @Column(name = "cake_name_snapshot", nullable = false, length = 100)
    private String cakeNameSnapshot;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(nullable = false)
    private Integer quantity;
}
