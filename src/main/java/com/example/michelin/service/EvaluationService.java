package com.example.michelin.service;

import com.example.michelin.dto.EvaluationRequestDto;
import com.example.michelin.dto.EvaluationResponseDto;
import com.example.michelin.dto.ImageUploadResponseDto;
import com.example.michelin.entity.EvaluationEntity;
import com.example.michelin.entity.RestaurantEntity;
import com.example.michelin.exception.ResourceNotFoundException;
import com.example.michelin.repository.EvaluationRepository;
import com.example.michelin.repository.RestaurantRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final RestaurantRepository restaurantRepository;
    private final LuceneService luceneService;
    private final S3Service s3Service;

    public EvaluationService(EvaluationRepository evaluationRepository,
                             RestaurantRepository restaurantRepository,
                             LuceneService luceneService,
                             S3Service s3Service) {
        this.evaluationRepository = evaluationRepository;
        this.restaurantRepository = restaurantRepository;
        this.luceneService = luceneService;
        this.s3Service = s3Service;
    }

    public EvaluationResponseDto addEvaluation(Long restaurantId, EvaluationRequestDto dto, String authorId) {
        RestaurantEntity restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable avec l'id : " + restaurantId));

        EvaluationEntity entity = EvaluationEntity.builder()
                .reviewerName(dto.getReviewerName())
                .comment(dto.getComment())
                .note(dto.getNote())
                .authorId(authorId != null ? authorId : dto.getReviewerName())
                .photos(dto.getPhotos() != null ? dto.getPhotos() : new ArrayList<>())
                .restaurant(restaurant)
                .build();

        EvaluationEntity saved = evaluationRepository.save(entity);

        // Indexation du commentaire dans Lucene
        luceneService.indexComment(saved.getId(), saved.getComment());

        return mapToDto(saved);
    }

    // Seul l'auteur ou un admin peut supprimer
    public void deleteEvaluation(Long id, String currentUserId, boolean isAdmin) {
        EvaluationEntity evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable avec l'id : " + id));

        if (!isAdmin && !evaluation.getAuthorId().equalsIgnoreCase(currentUserId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer l'évaluation d'un autre utilisateur");
        }

        evaluationRepository.deleteById(id);
        luceneService.deleteComment(id);
    }

    // Mes évaluations
    public List<EvaluationResponseDto> getMyEvaluations(String authorId) {
        List<EvaluationEntity> entities = evaluationRepository.findByAuthorId(authorId);
        List<EvaluationResponseDto> dtos = new ArrayList<>();
        for (EvaluationEntity entity : entities) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    // Recherche plein texte Lucene
    public List<EvaluationResponseDto> searchEvaluations(String keyword) {
        List<Long> matchingIds = luceneService.searchComments(keyword);
        if (matchingIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<EvaluationEntity> entities = evaluationRepository.findAllById(matchingIds);
        List<EvaluationResponseDto> dtos = new ArrayList<>();
        for (EvaluationEntity entity : entities) {
            dtos.add(mapToDto(entity));
        }
        return dtos;
    }

    // Ajout d'une photo d'évaluation (réservé à l'auteur ou à l'admin)
    public ImageUploadResponseDto getEvaluationPhotoUploadUrl(Long id, String filename, String currentUserId, boolean isAdmin) {
        EvaluationEntity evaluation = evaluationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Évaluation introuvable avec l'id : " + id));

        if (!isAdmin && !evaluation.getAuthorId().equalsIgnoreCase(currentUserId)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à ajouter une photo sur l'évaluation d'un autre utilisateur");
        }

        String key = "eval-" + id + "-" + UUID.randomUUID() + (filename != null && !filename.isBlank() ? "-" + filename : ".jpg");
        String uploadUrl = s3Service.getUploadFileUrl(s3Service.getEvaluationBucket(), key);
        String finalUrl = s3Service.getPublicUrl(s3Service.getEvaluationBucket(), key);

        if (evaluation.getPhotos() == null) {
            evaluation.setPhotos(new ArrayList<>());
        }
        evaluation.getPhotos().add(finalUrl);
        evaluationRepository.save(evaluation);

        return new ImageUploadResponseDto(uploadUrl, finalUrl);
    }

    private EvaluationResponseDto mapToDto(EvaluationEntity entity) {
        return EvaluationResponseDto.builder()
                .id(entity.getId())
                .reviewerName(entity.getReviewerName())
                .comment(entity.getComment())
                .note(entity.getNote())
                .authorId(entity.getAuthorId())
                .photos(entity.getPhotos())
                .restaurantId(entity.getRestaurant() != null ? entity.getRestaurant().getId() : null)
                .build();
    }
}
