package com.licenta.v1.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Entity @Data @AllArgsConstructor @RequiredArgsConstructor
@Table(name = "discussions")
public class Discussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String message;

    @OneToMany(mappedBy = "discussion", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Comment> commentsList = new ArrayList<Comment>();

    private String timePosted;
    private String userName;
    private String userImage;
}
