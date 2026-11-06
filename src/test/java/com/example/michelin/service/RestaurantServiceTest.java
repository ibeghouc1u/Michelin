package com.example.michelin.service;

import com.example.michelin.dto.RestaurantRequestDto;
import com.example.michelin.dto.RestaurantResponseDto;
import com.example.michelin.dto.RestaurantUpdateRequestDto;
import com.example.michelin.entity.EvaluationEntity;
import com.example.michelin.entity.RestaurantEntity;
import com.example.michelin.exception.ResourceNotFoundException;
import com.example.michelin.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestaurantServiceTest {

    @InjectMocks
    private RestaurantService restaurantService;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private S3Service s3Service;

    // Vérifie que la moyenne vaut -1 si pas d'évaluation
    @Test
    void should_return_average_minus_one_when_restaurant_has_no_evaluations() {
        RestaurantEntity restaurant = RestaurantEntity.builder()
                .id(1L)
                .name("Le Gourmet")
                .address("10 rue de Paris")
                .evaluations(new ArrayList<>())
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponseDto result = restaurantService.getRestaurantById(1L);

        assertNotNull(result);
        assertEquals(-1.0, result.getMoyenne());
    }

    // Calcul de la moyenne des évaluations
    @Test
    void should_calculate_correct_average_when_restaurant_has_evaluations() {
        EvaluationEntity eval1 = EvaluationEntity.builder().id(1L).note(1).build();
        EvaluationEntity eval2 = EvaluationEntity.builder().id(2L).note(3).build();

        RestaurantEntity restaurant = RestaurantEntity.builder()
                .id(1L)
                .name("Le Gourmet")
                .address("10 rue de Paris")
                .evaluations(List.of(eval1, eval2))
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(restaurant));

        RestaurantResponseDto result = restaurantService.getRestaurantById(1L);

        assertNotNull(result);
        assertEquals(2.0, result.getMoyenne());
        assertEquals(2, result.getEvaluations().size());
    }

    // Vérification du cas 404
    @Test
    void should_throw_ResourceNotFoundException_when_restaurant_not_found() {
        when(restaurantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> restaurantService.getRestaurantById(999L));
    }

    // Création d'un restaurant
    @Test
    void should_create_restaurant_successfully() {
        RestaurantRequestDto request = RestaurantRequestDto.builder()
                .name("Nouveau Restaurant")
                .address("1 Avenue des Champs-Elysées")
                .build();

        RestaurantEntity savedEntity = RestaurantEntity.builder()
                .id(10L)
                .name(request.getName())
                .address(request.getAddress())
                .evaluations(new ArrayList<>())
                .build();

        when(restaurantRepository.save(any(RestaurantEntity.class))).thenReturn(savedEntity);

        RestaurantResponseDto result = restaurantService.createRestaurant(request);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals("Nouveau Restaurant", result.getName());
        assertEquals(-1.0, result.getMoyenne());
    }

    // Mise à jour nom et adresse
    @Test
    void should_update_restaurant_name_and_address() {
        RestaurantEntity existing = RestaurantEntity.builder()
                .id(1L)
                .name("Ancien Nom")
                .address("Ancienne Adresse")
                .evaluations(new ArrayList<>())
                .build();

        RestaurantUpdateRequestDto updateDto = RestaurantUpdateRequestDto.builder()
                .name("Nouveau Nom")
                .address("Nouvelle Adresse")
                .build();

        when(restaurantRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(restaurantRepository.save(any(RestaurantEntity.class))).thenReturn(existing);

        RestaurantResponseDto result = restaurantService.updateRestaurant(1L, updateDto);

        assertNotNull(result);
        assertEquals("Nouveau Nom", existing.getName());
        assertEquals("Nouvelle Adresse", existing.getAddress());
    }
}
