package org.example.homedatazip.listing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.apartment.entity.Apartment;
import org.example.homedatazip.common.BaseTimeEntity;
import org.example.homedatazip.user.entity.User;

@Entity
@Getter
public class Listing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne
    private Apartment apartment;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private Integer salePrice;
    private Integer deposit;
    private Integer monthlyRent;

    private String agentPhone;

    @Column(length = 2000)
    private String description;
}