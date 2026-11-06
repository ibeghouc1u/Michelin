package com.example.michelin.service;

import com.example.michelin.dto.EvaluationRequestDto;
import com.example.michelin.dto.EvaluationResponseDto;
import com.example.michelin.entity.EvaluationEntity;
import com.example.michelin.entity.RestaurantEntity;
import com.example.michelin.exception.ResourceNotFoundException;
import com.example.michelin.repository.EvaluationRepository;
import com.example.michelin.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EvaluationServiceTest {

    @InjectMocks
    private EvaluationService evaluationService;

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private LuceneService luceneService;

    @Mock
    private S3Service s3Service;

    // Ajout d'une évaluation
    @Test
    void should_add_evaluation_to_existing_restaurant() {
        RestaurantEntity restaurant = RestaurantEntity.builder().id(1L).name("Le Ritz").build();

        EvaluationRequestDto request = EvaluationRequestDto.builder()
                .reviewerName("Lucien")
                .comment("Excellent service !")
                .note(3)
                .build();

        EvaluationEntity savedEntity = EvaluationEntity.builder()
                .id(100L)
                .reviewerName("Lucien")
                .comment("Excellent service !")
                .note(3)
                .authorId("Lucien")
                .restaurant(restaurant)
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));
        when(evaluationRepository.save(any(EvaluationEntity.class))).thenReturn(savedEntity);

        EvaluationResponseDto result = evaluationService.addEvaluation(1L, request, "Lucien");

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Lucien", result.getReviewerName());
        assertEquals(3, result.getNote());
        verify(luceneService, times(1)).indexComment(100L, "Excellent service !");
    }

    // Erreur si restaurant inconnu
    @Test
    void should_throw_ResourceNotFoundException_when_adding_evaluation_to_unknown_restaurant() {
        EvaluationRequestDto request = EvaluationRequestDto.builder()
                .reviewerName("Lucien")
                .comment("Comment")
                .note(2)
                .build();

        when(restaurantRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> evaluationService.addEvaluation(404L, request, "Lucien"));
        verify(evaluationRepository, never()).save(any());
    }

    // Suppression par l'auteur
    @Test
    void should_delete_evaluation_when_user_is_author() {
        EvaluationEntity evaluation = EvaluationEntity.builder()
                .id(10L)
                .authorId("lucien")
                .build();

        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(evaluation));

        assertDoesNotThrow(() -> evaluationService.deleteEvaluation(10L, "lucien", false));

        verify(evaluationRepository, times(1)).deleteById(10L);
        verify(luceneService, times(1)).deleteComment(10L);
    }

    // Erreur si suppression par un autre utilisateur non-admin
    @Test
    void should_throw_AccessDeniedException_when_deleting_other_user_evaluation() {
        EvaluationEntity evaluation = EvaluationEntity.builder()
                .id(10L)
                .authorId("lucien")
                .build();

        when(evaluationRepository.findById(10L)).thenReturn(Optional.of(evaluation));

        assertThrows(AccessDeniedException.class,
                () -> evaluationService.deleteEvaluation(10L, "autre_utilisateur", false));

        verify(evaluationRepository, never()).deleteById(any());
    }

    // Récupération de mes évaluations
    @Test
    void should_return_my_evaluations_only() {
        EvaluationEntity eval = EvaluationEntity.builder()
                .id(1L)
                .authorId("lucien")
                .reviewerName("Lucien")
                .comment("Top")
                .note(3)
                .build();

        when(evaluationRepository.findByAuthorId("lucien")).thenReturn(List.of(eval));

        List<EvaluationResponseDto> results = evaluationService.getMyEvaluations("lucien");

        assertEquals(1, results.size());
        assertEquals("Lucien", results.get(0).getReviewerName());
    }
}
