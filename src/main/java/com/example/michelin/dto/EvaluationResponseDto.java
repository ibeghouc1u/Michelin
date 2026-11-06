package com.example.michelin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponseDto {
    private Long id;
    private String reviewerName;
    private String comment;
    private int note;
    private String authorId;
    private List<String> photos;
    private Long restaurantId;
}
