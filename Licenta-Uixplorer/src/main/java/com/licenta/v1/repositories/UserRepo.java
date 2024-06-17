package com.licenta.v1.repositories;

import com.licenta.v1.models.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepo extends JpaRepository<AppUser, Long> {

    @Query(value = "select * from users where email = ?1", nativeQuery = true)
    AppUser findUserByEmail(String email);

    @Query(value = "select * from users where email = ?1", nativeQuery = true)
    Optional<AppUser> findByEmail(String email);

    @Query(value = "select * from users where email = ?1 and password = ?2", nativeQuery = true)
    AppUser findByEmailAndPassword(String email, String password);

    @Query(value = "select id from users where email = ?1", nativeQuery = true)
    Long findIdByEmail(String email);

    @Query(value = "select password from users where email = ?1", nativeQuery = true)
    String findPasswordByEmail(String email);

    @Modifying @Transactional
    @Query(value = "update users set email = ?2 where email = ?1", nativeQuery = true)
    void updateEmail(String currentEmail, String newEmail);

    @Modifying @Transactional
    @Query(value = "update users set name = ?2 where email = ?1", nativeQuery = true)
    void updateName(String currentEmail, String newName);

    @Query(value = "UPDATE users SET password = ?2 WHERE email = ?1", nativeQuery = true)
    @Modifying
    @Transactional
    void updatePassword(String email, String newPassword);

    @Modifying @Transactional
    @Query(value = "update users set image = ?2 where email = ?1", nativeQuery = true)
    void updateImage(String currentEmail, String img);

    @Modifying @Transactional
    @Query(value = "update users set days = ?2 where email = ?1", nativeQuery = true)
    void updateDays(String currentEmail, int newDays);

    @Modifying @Transactional
    @Query(value = "update users set last_login_date = CURRENT_DATE where email = ?1", nativeQuery = true)
    void updateLoginDate(String currentEmail);

    @Modifying @Transactional
    @Query(value = "insert into user_article values (user_id, article_id) (?1, ?2)", nativeQuery = true)
    void finishReading(Long userId, Long articleId);
}
