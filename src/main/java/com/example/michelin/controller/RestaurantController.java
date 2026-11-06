package com.example.michelin.controller;

import com.example.michelin.dto.ImageUploadResponseDto;
import com.example.michelin.dto.RestaurantRequestDto;
import com.example.michelin.dto.RestaurantResponseDto;
import com.example.michelin.dto.RestaurantUpdateRequestDto;
import com.example.michelin.service.RestaurantService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/restaurants")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }

    // Récupérer tous les restaurants (public)
    @GetMapping
    public List<RestaurantResponseDto> getAllRestaurants() {
        return restaurantService.getAllRestaurants();
    }

    // Récupérer un restaurant par son ID (public)
    @GetMapping("/{id}")
    public RestaurantResponseDto getRestaurantById(@PathVariable Long id) {
        return restaurantService.getRestaurantById(id);
    }

    // Créer un restaurant (réservé ADMIN)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantResponseDto createRestaurant(@Valid @RequestBody RestaurantRequestDto dto) {
        return restaurantService.createRestaurant(dto);
    }

    // Mettre à jour un restaurant (réservé ADMIN)
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public RestaurantResponseDto updateRestaurant(@PathVariable Long id,
                                                  @Valid @RequestBody RestaurantUpdateRequestDto dto) {
        return restaurantService.updateRestaurant(id, dto);
    }

    // Obtenir le lien d'upload S3 pour la photo du restaurant (réservé ADMIN)
    @PostMapping("/{id}/image-upload-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ImageUploadResponseDto getRestaurantImageUploadUrl(@PathVariable Long id,
                                                              @RequestParam(required = false) String filename) {
        return restaurantService.getRestaurantImageUploadUrl(id, filename);
    }
}
