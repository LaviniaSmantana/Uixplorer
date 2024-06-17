package com.licenta.v1.repositories;

import com.licenta.v1.models.Badge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BadgeRepo extends JpaRepository<Badge, Long> {

    Optional<Badge> findById(Long id);

    @Query(value = "select * from badges where id = ?1", nativeQuery = true)
    Badge findBadgeById(Long id);
}
