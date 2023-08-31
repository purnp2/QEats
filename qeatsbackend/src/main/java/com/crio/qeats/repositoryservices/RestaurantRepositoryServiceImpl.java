/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.repositoryservices;

import ch.hsr.geohash.GeoHash;
import redis.clients.jedis.Jedis;
import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.models.ItemEntity;
import com.crio.qeats.models.MenuEntity;
import com.crio.qeats.models.RestaurantEntity;
import com.crio.qeats.repositories.ItemRepository;
import com.crio.qeats.repositories.MenuRepository;
import com.crio.qeats.repositories.RestaurantRepository;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Provider;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;


@Service
@Primary
public class RestaurantRepositoryServiceImpl implements RestaurantRepositoryService {

  @Autowired
  private RedisConfiguration redisConfiguration;
  // PP
  @Autowired
  private RestaurantRepository restaurantRepository;

  @Autowired
  private MongoTemplate mongoTemplate;

  @Autowired
  private Provider<ModelMapper> modelMapperProvider;

  private boolean isOpenNow(LocalTime time, RestaurantEntity res) {
    LocalTime openingTime = LocalTime.parse(res.getOpensAt());
    LocalTime closingTime = LocalTime.parse(res.getClosesAt());

    return time.isAfter(openingTime) && time.isBefore(closingTime);
  }

  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objectives:
  // 1. Implement findAllRestaurantsCloseby.
  // 2. Remember to keep the precision of GeoHash in mind while using it as a key.
  // Check RestaurantRepositoryService.java file for the interface contract.
  public List<Restaurant> findAllRestaurantsCloseBy(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants = null;

    if (redisConfiguration.isCacheAvailable()) {
      restaurants =
          findAllRestaurantsCloseByFromCache(latitude, longitude, currentTime, servingRadiusInKms);
    } else {
      restaurants =
          findAllRestaurantsCloseByFromDB(latitude, longitude, currentTime, servingRadiusInKms);
    }
    // CHECKSTYLE:OFF
    // CHECKSTYLE:ON

    return restaurants;
  }


  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   */
  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose names have an exact or partial match with the search query.

  @Override
  public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants =
        findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);
    if (searchString == null || searchString.isEmpty()) {
      return restaurants;
    } else
      return restaurants.stream()
          .filter(r -> r.getName().toLowerCase().contains(searchString.toLowerCase()))
          .collect(Collectors.toList());
  }

  // @Override
  // public List<Restaurant> findRestaurantsByName(Double latitude, Double longitude,
  // String searchString, LocalTime currentTime, Double servingRadiusInKms) {

  // Optional<List<RestaurantEntity>> optRestaurantsEntities =
  // restaurantRepository.findRestaurantsByNameExact(searchString);
  // List<RestaurantEntity> restaurantsEntities = optRestaurantsEntities.get();
  // if (restaurantsEntities == null) return null;
  // List<Restaurant> restaurants = new ArrayList<>();
  // for (RestaurantEntity r : restaurantsEntities) {
  // if (isRestaurantCloseByAndOpen(r, currentTime, latitude, longitude, servingRadiusInKms)) {
  // restaurants.add(modelMapperProvider.get().map(r, Restaurant.class));
  // }
  // }

  // return restaurants;
  // }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants whose attributes (cuisines) intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {

    List<Restaurant> restaurants =
        findAllRestaurantsCloseBy(latitude, longitude, currentTime, servingRadiusInKms);

    if (searchString == null)
      return restaurants;
    return restaurants.stream()
        .filter(r -> r.getAttributes().stream().anyMatch(a -> a.equalsIgnoreCase(searchString)))
        .collect(Collectors.toList());
  }



  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose names form a complete or partial match
  // with the search query.

  @Override
  public List<Restaurant> findRestaurantsByItemName(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    return findRestaurantsByAttributes(latitude, longitude, searchString, currentTime,
        servingRadiusInKms);
  }

  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Objective:
  // Find restaurants which serve food items whose attributes intersect with the search query.
  @Override
  public List<Restaurant> findRestaurantsByItemAttributes(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    return findRestaurantsByAttributes(latitude, longitude, searchString, currentTime,
        servingRadiusInKms);
  }


  // TODO: CRIO_TASK_MODULE_NOSQL
  // Objective:
  // 1. Check if a restaurant is nearby and open. If so, it is a candidate to be returned.
  // NOTE: How far exactly is "nearby"?

  /**
   * Utility method to check if a restaurant is within the serving radius at a given time.
   * 
   * @return boolean True if restaurant falls within serving radius and is open, false otherwise
   */
  private boolean isRestaurantCloseByAndOpen(RestaurantEntity restaurantEntity,
      LocalTime currentTime, Double latitude, Double longitude, Double servingRadiusInKms) {
    if (isOpenNow(currentTime, restaurantEntity)) {
      return GeoUtils.findDistanceInKm(latitude, longitude, restaurantEntity.getLatitude(),
          restaurantEntity.getLongitude()) < servingRadiusInKms;
    }

    return false;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromCache(Double userLat, Double userLongt,
      LocalTime currentTime, Double radius) {

    List<Restaurant> restaurantList = new ArrayList<>();
    GeoHash geoHash = GeoHash.withCharacterPrecision(userLat, userLongt, 7);

    try (Jedis jedis = redisConfiguration.getJedisPool().getResource()) {
      String jsonStringFromCache = jedis.get(geoHash.toBase32());
      if (jsonStringFromCache == null) {
        String createdJsonString = "";
        try {
          restaurantList = findAllRestaurantsCloseByFromDB(userLat, userLongt, currentTime, radius);
          createdJsonString = new ObjectMapper().writeValueAsString(restaurantList);
        } catch (JsonProcessingException e) {
          e.printStackTrace();
        }
        jedis.setex(geoHash.toBase32(), GlobalConstants.REDIS_ENTRY_EXPIRY_IN_SECONDS,
            createdJsonString);
      } else {
        try {
          restaurantList = new ObjectMapper().readValue(jsonStringFromCache,
              new TypeReference<List<Restaurant>>() {});
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

    return restaurantList;
  }

  private List<Restaurant> findAllRestaurantsCloseByFromDB(Double latitude, Double longitude,
      LocalTime currentTime, Double servingRadiusInKms) {

    List<RestaurantEntity> restaurantsEntities = restaurantRepository.findAll();
    List<Restaurant> restaurants = new ArrayList<>();
    for (RestaurantEntity r : restaurantsEntities) {
      if (isRestaurantCloseByAndOpen(r, currentTime, latitude, longitude, servingRadiusInKms)) {
        restaurants.add(modelMapperProvider.get().map(r, Restaurant.class));
      }
    }

    return restaurants;
  }

  // MULTITHREADING
  @Override
  public Future<List<Restaurant>> findRestaurantsByNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList =
        findRestaurantsByName(latitude, longitude, searchString, currentTime, servingRadiusInKms);
    return new AsyncResult<>(restaurantList);

  }

  public Future<List<Restaurant>> findRestaurantsByAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = findRestaurantsByAttributes(latitude, longitude, searchString,
        currentTime, servingRadiusInKms);

    return new AsyncResult<>(restaurantList);
  }


  public Future<List<Restaurant>> findRestaurantsByItemNameAsync(Double latitude, Double longitude,
      String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = findRestaurantsByItemName(latitude, longitude, searchString,
        currentTime, servingRadiusInKms);

    return new AsyncResult<>(restaurantList);
  }


  public Future<List<Restaurant>> findRestaurantsByItemAttributesAsync(Double latitude,
      Double longitude, String searchString, LocalTime currentTime, Double servingRadiusInKms) {
    List<Restaurant> restaurantList = findRestaurantsByItemAttributes(latitude, longitude,
        searchString, currentTime, servingRadiusInKms);

    return new AsyncResult<>(restaurantList);
  }

}

