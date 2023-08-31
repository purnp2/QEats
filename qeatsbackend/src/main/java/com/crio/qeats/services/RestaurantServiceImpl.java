
/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import com.crio.qeats.configs.RedisConfiguration;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.globals.GlobalConstants;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import com.crio.qeats.utils.GeoLocation;
import com.crio.qeats.utils.GeoUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import ch.hsr.geohash.GeoHash;
import lombok.extern.log4j.Log4j2;
import redis.clients.jedis.Jedis;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@Log4j2
public class RestaurantServiceImpl implements RestaurantService {

  private final Double peakHoursServingRadiusInKms = 3.0;
  private final Double normalHoursServingRadiusInKms = 5.0;

  @Autowired
  private RestaurantRepositoryService restaurantRepositoryService;

  // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI - Implement findAllRestaurantsCloseby.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findAllRestaurantsCloseBy(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double userLat = getRestaurantsRequest.getLatitude();
    Double userLongt = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();
    Double radius = getAppicableRadius(currentTime);
    List<Restaurant> selectedRestaurantsCloseBy;

    // if (! searchFor.isEmpty()){
    // selectedRestaurantsCloseBy = restaurantRepositoryService
    // .findAllRestaurantsCloseBy(userLat, userLongt, currentTime, radius);
    // return new GetRestaurantsResponse(selectedRestaurantsCloseBy);
    // }else if (searchFor.trim().equals("")){
    // return new GetRestaurantsResponse(new ArrayList<>());
    // }else if (searchFor != null) {
    // return findRestaurantsBySearchQuery(getRestaurantsRequest, currentTime);
    // }

    selectedRestaurantsCloseBy = restaurantRepositoryService.findAllRestaurantsCloseBy(userLat,
        userLongt, currentTime, radius);
    return new GetRestaurantsResponse(selectedRestaurantsCloseBy);
  }


  // TODO: CRIO_TASK_MODULE_RESTAURANTSEARCH
  // Implement findRestaurantsBySearchQuery. The request object has the search string.
  // We have to combine results from multiple sources:
  // 1. Restaurants by name (exact and inexact)
  // 2. Restaurants by cuisines (also called attributes)
  // 3. Restaurants by food items it serves
  // 4. Restaurants by food item attributes (spicy, sweet, etc)
  // Remember, a restaurant must be present only once in the resulting list.
  // Check RestaurantService.java file for the interface contract.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQuery(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {

    Double userLat = getRestaurantsRequest.getLatitude();
    Double userLongt = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();
    Double radius = getAppicableRadius(currentTime);
    long startTime = System.currentTimeMillis();

    // PP: We will have multiple places for look at for the 'searchFor' parameter.
    List<Restaurant> restaurantsCloseByAndDifferentSeachQueries = new ArrayList<>();
    Set<Restaurant> uniqueRestaurantsSet = new HashSet<>();
    List<Restaurant> uniqueRestaurantsList = new ArrayList<>();

    // Looks like this if-condition is redundent. PP
    if (searchFor.isEmpty()) {
      return new GetRestaurantsResponse(restaurantsCloseByAndDifferentSeachQueries);
    }

    restaurantsCloseByAndDifferentSeachQueries.addAll(restaurantRepositoryService
        .findRestaurantsByName(userLat, userLongt, searchFor, currentTime, radius));
    restaurantsCloseByAndDifferentSeachQueries.addAll(restaurantRepositoryService
        .findRestaurantsByAttributes(userLat, userLongt, searchFor, currentTime, radius));
    System.out.println("Time consumed w/o mt [milli]: " + (System.currentTimeMillis() - startTime));

    for (Restaurant r : restaurantsCloseByAndDifferentSeachQueries) {
      if (!uniqueRestaurantsSet.contains(r)) {
        uniqueRestaurantsList.add(r);
        uniqueRestaurantsSet.add(r);
      }
    }

    return new GetRestaurantsResponse(uniqueRestaurantsList);
  }

  // Let's find under what radius we have to look the restaurants in, based on given time
  private Double getAppicableRadius(LocalTime currentTime) {
    Double radius;
    LocalTime t8AM = LocalTime.of(8, 00);
    LocalTime t10AM = LocalTime.of(10, 00);
    LocalTime t1PM = LocalTime.of(13, 00);
    LocalTime t2PM = LocalTime.of(14, 00);
    LocalTime t7PM = LocalTime.of(19, 00);
    LocalTime t9PM = LocalTime.of(21, 00);
    if ((!currentTime.isBefore(t8AM) && !currentTime.isAfter(t10AM))
        || (!currentTime.isBefore(t1PM) && !currentTime.isAfter(t2PM))
        || (!currentTime.isBefore(t7PM) && !currentTime.isAfter(t9PM))) {
      radius = peakHoursServingRadiusInKms;
    } else {
      radius = normalHoursServingRadiusInKms;
    }
    return radius;
  }


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  // Implement multi-threaded version of RestaurantSearch.
  // Implement variant of findRestaurantsBySearchQuery which is at least 1.5x time faster than
  // findRestaurantsBySearchQuery.
  @Override
  public GetRestaurantsResponse findRestaurantsBySearchQueryMt(
      GetRestaurantsRequest getRestaurantsRequest, LocalTime currentTime) {
    Double userLat = getRestaurantsRequest.getLatitude();
    Double userLongt = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();
    Double radius = getAppicableRadius(currentTime);
    long startTime = System.currentTimeMillis();

    // PP: We will have multiple places for look at for the 'searchFor' parameter.
    List<Restaurant> restaurantsCloseByAndDifferentSeachQueries = new ArrayList<>();
    Future<List<Restaurant>> futureGetRestaurantByNameList = restaurantRepositoryService
        .findRestaurantsByNameAsync(userLat, userLongt, searchFor, currentTime, radius);
    Future<List<Restaurant>> futureGetRestaurantByAttributesList = restaurantRepositoryService
        .findRestaurantsByAttributesAsync(userLat, userLongt, searchFor, currentTime, radius);

    try {
      while (true) {
        if (futureGetRestaurantByNameList.isDone()
            && futureGetRestaurantByAttributesList.isDone()) {
          restaurantsCloseByAndDifferentSeachQueries.addAll(futureGetRestaurantByNameList.get());
          restaurantsCloseByAndDifferentSeachQueries
              .addAll(futureGetRestaurantByAttributesList.get());
          System.out.println("Time consumed w/ mt [milli]: " + (System.currentTimeMillis() - startTime));
          break;
        }
      }
    } catch (InterruptedException | ExecutionException e) {
      e.printStackTrace();
      return new GetRestaurantsResponse(restaurantsCloseByAndDifferentSeachQueries);
    }

    Set<Restaurant> uniqueRestaurantsSet = new HashSet<>();
    List<Restaurant> uniqueRestaurantsList = new ArrayList<>();

    for (Restaurant r : restaurantsCloseByAndDifferentSeachQueries) {
      if (!uniqueRestaurantsSet.contains(r)) {
        uniqueRestaurantsList.add(r);
        uniqueRestaurantsSet.add(r);
      }
    }

    return new GetRestaurantsResponse(uniqueRestaurantsList);
  }

}

