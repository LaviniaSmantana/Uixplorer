package com.licenta.v1.repositories;

import com.licenta.v1.models.UserArticle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserArticleRepo extends JpaRepository<UserArticle, Long> {
}
