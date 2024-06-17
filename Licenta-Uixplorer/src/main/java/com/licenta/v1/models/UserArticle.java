package com.licenta.v1.models;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Entity @Data @RequiredArgsConstructor @AllArgsConstructor
@Table(name = "user_article")
public class UserArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonBackReference
    private AppUser appuser;

    @ManyToOne
    @JoinColumn(name = "article_id")
    private Article article;

}
