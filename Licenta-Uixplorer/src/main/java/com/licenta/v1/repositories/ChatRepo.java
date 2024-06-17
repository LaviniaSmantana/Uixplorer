package com.licenta.v1.repositories;

import com.licenta.v1.models.AppUser;
import com.licenta.v1.models.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ChatRepo extends JpaRepository<Chat, Long> {

    @Query(value = "select * from chat where user_id = ?1 and friend_id = ?2", nativeQuery = true)
    Chat findByUserAndFriend(Long userId, Long friendId);
}
