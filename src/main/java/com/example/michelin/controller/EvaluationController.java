package com.example.michelin.controller;

import com.example.michelin.dto.EvaluationRequestDto;
import com.example.michelin.dto.EvaluationResponseDto;
import com.example.michelin.dto.ImageUploadResponseDto;
import com.example.michelin.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EvaluationController {

    private final EvaluationService evaluationService;

    public EvaluationController(EvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    // Ajouter un avis
    @PostMapping("/restaurants/{restaurantId}/evaluations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public EvaluationResponseDto addEvaluation(@PathVariable Long restaurantId,
            @Valid @RequestBody EvaluationRequestDto dto,
            Authentication principal) {
        String authorId = (principal != null) ? principal.getName() : dto.getReviewerName();
        return evaluationService.addEvaluation(restaurantId, dto, authorId);
    }

    // Supprimer un avis (l'auteur ou un admin)
    @DeleteMapping("/evaluations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("isAuthenticated()")
    public void deleteEvaluation(@PathVariable Long id, Authentication principal) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String currentUserId = principal != null ? principal.getName() : "";

        evaluationService.deleteEvaluation(id, currentUserId, isAdmin);
    }

    // Mes évaluations
    @GetMapping("/evaluations/me")
    @PreAuthorize("isAuthenticated()")
    public List<EvaluationResponseDto> getMyEvaluations(Authentication principal) {
        String authorId = principal != null ? principal.getName() : "";
        return evaluationService.getMyEvaluations(authorId);
    }

    // Recherche par mot-clé
    @GetMapping("/evaluations/search")
    public List<EvaluationResponseDto> searchEvaluations(@RequestParam String keyword) {
        return evaluationService.searchEvaluations(keyword);
    }

    // Obtenir le lien d'upload S3 pour une photo
    @PostMapping("/evaluations/{id}/photo-upload-url")
    @PreAuthorize("isAuthenticated()")
    public ImageUploadResponseDto getEvaluationPhotoUploadUrl(@PathVariable Long id,
            @RequestParam(required = false) String filename,
            Authentication principal) {
        String currentUserId = principal != null ? principal.getName() : "";
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        return evaluationService.getEvaluationPhotoUploadUrl(id, filename, currentUserId, isAdmin);
    }
}
