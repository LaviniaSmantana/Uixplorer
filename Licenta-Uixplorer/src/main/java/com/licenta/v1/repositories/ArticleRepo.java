package com.licenta.v1.repositories;

import com.licenta.v1.models.Article;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ArticleRepo extends JpaRepository<Article, Long> {

    @Query(value = "select * from articles where id = ?1", nativeQuery = true)
    Article findArticleById(Long id);

    @Query(value = "select * from articles where title = ?1", nativeQuery = true)
    Article findByTitle(String title);

    @Query("SELECT a FROM Article a WHERE a.title LIKE :title")
    List<Article> searchInTitle(@Param("title") String title);

    @Cacheable("articles")
    Page<Article> findAll(Pageable pageable);

}
