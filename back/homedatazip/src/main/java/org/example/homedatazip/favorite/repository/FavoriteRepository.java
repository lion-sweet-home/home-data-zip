package org.example.homedatazip.favorite.repository;

import org.example.homedatazip.favorite.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    /** 이미 관심 등록 여부 확인 */
    boolean existsByUserIdAndListingId(Long userId, Long listingId);

    /** 관심 해제 시 조회 */
    Optional<Favorite> findByUserIdAndListingId(Long userId, Long listingId);

    /** 내 관심 매물 목록 (관심 등록 시점 최신순) */
    @Query("SELECT f FROM Favorite f JOIN FETCH f.listing l JOIN FETCH l.apartment WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<Favorite> findByUserIdOrderByCreatedAtDescWithListingAndApartment(@Param("userId") Long userId);
}