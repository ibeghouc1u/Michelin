package com.example.michelin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequestDto {

    @NotBlank(message = "Le nom de l'évaluateur est obligatoire")
    @Size(max = 50, message = "Le nom de l'évaluateur ne peut pas dépasser 50 caractères")
    private String reviewerName;

    @NotBlank(message = "Le commentaire est obligatoire")
    @Size(max = 255, message = "Le commentaire ne peut pas dépasser 255 caractères")
    private String comment;

    @Min(value = 0, message = "La note minimale est de 0 étoile")
    @Max(value = 3, message = "La note maximale est de 3 étoiles")
    private int note;

    @Builder.Default
    private List<String> photos = new ArrayList<>();
}
