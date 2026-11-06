package com.example.michelin.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String reviewerName;

    @Column(nullable = false, length = 255)
    private String comment;

    @Column(nullable = false)
    private int note;

    @Column(nullable = false)
    private String authorId;


    @ElementCollection
    @CollectionTable(name = "evaluation_photos", joinColumns = @JoinColumn(name = "evaluation_id"))
    @Column(name = "photo_url")
    @Builder.Default
    private List<String> photos = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id")
    private RestaurantEntity restaurant;
}