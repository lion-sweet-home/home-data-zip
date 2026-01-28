package org.example.homedatazip.favorite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import org.example.homedatazip.listing.entity.Listing;
import org.example.homedatazip.user.entity.User;

@Entity
@Getter
@Table(
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "listing_id"})
        }
)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Listing listing;
}
