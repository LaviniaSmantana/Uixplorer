package com.licenta.v1.models;

import jakarta.persistence.*;
import lombok.Data;

@Data @Entity @Table(name = "articles")
public class Article {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String mainImg;
    @Column(columnDefinition = "TEXT")
    private String introText;
    private String imgOne;
    @Column(columnDefinition = "TEXT")
    private String textOne;
    private String imgTwo;
    @Column(columnDefinition = "TEXT")
    private String textTwo;
    private String imgThree;
    @Column(columnDefinition = "TEXT")
    private String textThree;
    private String source;

}
