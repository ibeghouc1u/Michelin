package com.example.michelin.service;

import com.example.michelin.dto.EvaluationResponseDto;
import com.example.michelin.dto.ImageUploadResponseDto;
import com.example.michelin.dto.RestaurantRequestDto;
import com.example.michelin.dto.RestaurantResponseDto;
import com.example.michelin.dto.RestaurantUpdateRequestDto;
import com.example.michelin.entity.EvaluationEntity;
import com.example.michelin.entity.RestaurantEntity;
import com.example.michelin.exception.ResourceNotFoundException;
import com.example.michelin.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final S3Service s3Service;

    public RestaurantService(RestaurantRepository restaurantRepository, S3Service s3Service) {
        this.restaurantRepository = restaurantRepository;
        this.s3Service = s3Service;
    }

    public List<RestaurantResponseDto> getAllRestaurants() {
        List<RestaurantEntity> restaurants = restaurantRepository.findAll();
        List<RestaurantResponseDto> dtos = new ArrayList<>();

        for (RestaurantEntity restaurant : restaurants) {
            dtos.add(mapToResponseDto(restaurant));
        }

        return dtos;
    }

    public RestaurantResponseDto getRestaurantById(Long id) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable avec l'id : " + id));

        return mapToResponseDto(restaurant);
    }

    public RestaurantResponseDto createRestaurant(RestaurantRequestDto dto) {
        RestaurantEntity entity = RestaurantEntity.builder()
                .name(dto.getName())
                .address(dto.getAddress())
                .imagePath(dto.getImagePath())
                .evaluations(new ArrayList<>())
                .build();

        RestaurantEntity saved = restaurantRepository.save(entity);
        return mapToResponseDto(saved);
    }

    public RestaurantResponseDto updateRestaurant(Long id, RestaurantUpdateRequestDto dto) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable avec l'id : " + id));

        restaurant.setName(dto.getName());
        restaurant.setAddress(dto.getAddress());

        RestaurantEntity updated = restaurantRepository.save(restaurant);
        return mapToResponseDto(updated);
    }

    // Récupération de l'URL présignée pour l'image du restaurant
    public ImageUploadResponseDto getRestaurantImageUploadUrl(Long id, String filename) {
        RestaurantEntity restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant introuvable avec l'id : " + id));

        String key = "restaurant-" + id + "-" + UUID.randomUUID() + (filename != null && !filename.isBlank() ? "-" + filename : ".jpg");
        String uploadUrl = s3Service.getUploadFileUrl(s3Service.getRestaurantBucket(), key);
        String finalUrl = s3Service.getPublicUrl(s3Service.getRestaurantBucket(), key);

        restaurant.setImagePath(finalUrl);
        restaurantRepository.save(restaurant);

        return new ImageUploadResponseDto(uploadUrl, finalUrl);
    }

    // Si pas d'évaluation, la moyenne doit être de -1
    private double calculateAverage(List<EvaluationEntity> evaluations) {
        if (evaluations == null || evaluations.isEmpty()) {
            return -1.0;
        }

        double sum = 0.0;
        for (EvaluationEntity eval : evaluations) {
            sum += eval.getNote();
        }

        return sum / evaluations.size();
    }

    private RestaurantResponseDto mapToResponseDto(RestaurantEntity entity) {
        List<EvaluationResponseDto> evalDtos = new ArrayList<>();
        if (entity.getEvaluations() != null) {
            for (EvaluationEntity eval : entity.getEvaluations()) {
                evalDtos.add(mapEvaluationToDto(eval));
            }
        }

        return RestaurantResponseDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .imagePath(entity.getImagePath())
                .moyenne(calculateAverage(entity.getEvaluations()))
                .evaluations(evalDtos)
                .build();
    }

    private EvaluationResponseDto mapEvaluationToDto(EvaluationEntity evaluation) {
        return EvaluationResponseDto.builder()
                .id(evaluation.getId())
                .reviewerName(evaluation.getReviewerName())
                .comment(evaluation.getComment())
                .note(evaluation.getNote())
                .authorId(evaluation.getAuthorId())
                .photos(evaluation.getPhotos())
                .restaurantId(evaluation.getRestaurant() != null ? evaluation.getRestaurant().getId() : null)
                .build();
    }
}
