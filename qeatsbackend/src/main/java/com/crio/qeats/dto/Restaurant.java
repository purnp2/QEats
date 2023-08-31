// IMPORTANT: This DTO class will be used even after we build the repository.
// Check the RestaurantRepositoryServiceImpl class where we have converted the RestaurantEntity ...
// ... to the Restaurant class using the ModelMapper class.
// This Restaurant class conversion is necessary despite we have defined GetRestaurantResponse class
// because we have defined this GetRestaurantResponse Class depended on the Restaurant class.

// import javax.inject.Provider;
// import org.modelmapper.ModelMapper;
// @Autowired
// private Provider<ModelMapper> modelMapperProvider;
// modelMapperProvider.get().map(r, Restaurant.class));
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.ArrayList;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// TODO: CRIO_TASK_MODULE_SERIALIZATION
// Implement Restaurant class.
// Complete the class such that it produces the following JSON during serialization.
// {
// "restaurantId": "10",
// "name": "A2B",
// "city": "Hsr Layout",
// "imageUrl": "www.google.com",
// "latitude": 20.027,
// "longitude": 30.0,
// "opensAt": "18:00",
// "closesAt": "23:00",
// "attributes": [
// "Tamil",
// "South Indian"
// ]
// }
// @Data
// @AllArgsConstructor
// public class Restaurant {
// @NotNull private int restaurantId;
// @NotNull private String name;
// private String city;
// private String imageUrl;
// @NotNull private double latitude;
// @NotNull private double longitude;
// @NotNull private LocalTime opensAt;
// @NotNull private LocalTime closesAt;
// private String[] attributes;
// }

// @Data
// @AllArgsConstructor
// @NoArgsConstructor
// @JsonIgnoreProperties(ignoreUnknown = true)
@JsonIgnoreProperties({"id"})
public class Restaurant {

    public Restaurant() {}

    // @JsonIgnore
    private String id;
    private String restaurantId;
    private String name;
    private String city;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String opensAt;
    private String closesAt;
    private List<String> attributes;

    //........Getter..Setter....Constructors........
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(String restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(String opensAt) {
        this.opensAt = opensAt;
    }

    public String getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(String closesAt) {
        this.closesAt = closesAt;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<String> attributes) {
        this.attributes = attributes;
    }

}

