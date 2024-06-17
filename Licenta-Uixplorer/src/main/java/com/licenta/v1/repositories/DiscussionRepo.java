package com.licenta.v1.repositories;

import com.licenta.v1.models.Discussion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DiscussionRepo extends JpaRepository<Discussion, Long> {

    @Query(value = "select * from discussions where id = ?1", nativeQuery = true)
    Discussion findDiscussionById(Long id);
}
