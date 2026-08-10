package com.cakedelight.catalogservice.mapper;

import com.cakedelight.catalogservice.dto.request.CreateCakeRequest;
import com.cakedelight.catalogservice.dto.request.UpdateCakeRequest;
import com.cakedelight.catalogservice.dto.response.CakeResponse;
import com.cakedelight.catalogservice.entity.Cake;
import org.springframework.stereotype.Component;

// Manual mapping, not MapStruct — this project's other services (auth-service)
// hand-map too, and a handful of fields here doesn't earn the extra build
// step/annotation processor (coding-guidelines §1 lists mapper/ as the right
// package for this regardless of mapping strategy).
@Component
public class CakeMapper {

    public Cake toEntity(CreateCakeRequest request) {
        Cake cake = new Cake();
        cake.setName(request.name());
        cake.setDescription(request.description());
        cake.setCategory(request.category());
        cake.setPrice(request.price());
        cake.setAvailable(request.available() != null ? request.available() : true);
        cake.setImageUrl(request.imageUrl());
        return cake;
    }

    public void updateEntity(Cake cake, UpdateCakeRequest request) {
        cake.setName(request.name());
        cake.setDescription(request.description());
        cake.setCategory(request.category());
        cake.setPrice(request.price());
        cake.setAvailable(request.available());
        cake.setImageUrl(request.imageUrl());
    }

    public CakeResponse toResponse(Cake cake) {
        return new CakeResponse(
                cake.getId(),
                cake.getName(),
                cake.getDescription(),
                cake.getCategory(),
                cake.getPrice(),
                cake.getAvailable(),
                cake.getImageUrl(),
                cake.getCreatedAt()
        );
    }
}
