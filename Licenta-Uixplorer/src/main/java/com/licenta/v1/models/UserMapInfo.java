package com.licenta.v1.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data @AllArgsConstructor @RequiredArgsConstructor
public class UserMapInfo {

    private Long id;
    private String name;
    private Double latitude;
    private Double longitude;
}
