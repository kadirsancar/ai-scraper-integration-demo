package com.kadir.aipage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Data
public class Product {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private String name;

    @Column (columnDefinition = "TEXT", nullable = false)
    private String url;

    private String platform;

    private LocalDateTime createdAt = LocalDateTime.now();
}
