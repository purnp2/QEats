/*
 *
 * * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.controller;

import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.services.RestaurantService;
import com.crio.qeats.utils.GeoLocation;
import java.time.LocalTime;
import java.util.List;
import javax.validation.Valid;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod; // PP
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

// TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
// Implement Controller using Spring annotations.
// Remember, annotations have various "targets". They can be class level, method level or others.

@RestController
// @Controller
// @ResponseBody
public class RestaurantController {
  public static final String RESTAURANT_API_ENDPOINT = "/qeats/v1";

  public static final String RESTAURANTS_API = "/restaurants";
  public static final String MENU_API = "/menu";
  public static final String CART_API = "/cart";
  public static final String CART_ITEM_API = "/cart/item";
  public static final String CART_CLEAR_API = "/cart/clear";
  public static final String POST_ORDER_API = "/order";
  public static final String GET_ORDERS_API = "/orders";

  @Autowired
  private RestaurantService restaurantService;



  // @GetMapping(RESTAURANTS_API)
  @GetMapping(RESTAURANT_API_ENDPOINT + RESTAURANTS_API)
  // @RequestMapping(value = RESTAURANTS_API, method = RequestMethod.GET)
  public ResponseEntity<GetRestaurantsResponse> getRestaurants(
      @Valid GetRestaurantsRequest getRestaurantsRequest) {

    // log.info("getRestaurants called with {}", getRestaurantsRequest);
    GetRestaurantsResponse getRestaurantsResponse;
    Double latitude = getRestaurantsRequest.getLatitude();
    Double longitude = getRestaurantsRequest.getLongitude();
    String searchFor = getRestaurantsRequest.getSearchFor();

    // PP: check if the latitude and longitute are acceptable
    GeoLocation geoLocation = new GeoLocation(latitude, longitude);
    if (!geoLocation.isValidGeoLocation()) {
      // throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
      return ResponseEntity.badRequest().body(null);
    }

    // CHECKSTYLE:OFF
    LocalTime clientTime = LocalTime.now();

    // TODO: PURN : Change time if too late
    /**This code-block is applicable only in the test environment. Must be commented out.
    */
    if ( (clientTime.isAfter(LocalTime.of(21, 00)) && clientTime.isBefore(LocalTime.of(24, 00))) ||
          (clientTime.isAfter(LocalTime.of(00, 00)) && clientTime.isBefore(LocalTime.of(10, 00))) ){
      clientTime = LocalTime.of(13, 00);
    }

    // getRestaurantsResponse =
    // restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, clientTime);

    if (searchFor != null && !searchFor.isEmpty()) {
      getRestaurantsResponse =
          restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, clientTime);
          //restaurantService.findRestaurantsBySearchQueryMt(getRestaurantsRequest, clientTime);
    } else {
      getRestaurantsResponse =
          restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, clientTime);
    }

    // if (searchFor == null) {
    //   getRestaurantsResponse =
    //       restaurantService.findAllRestaurantsCloseBy(getRestaurantsRequest, clientTime);
    // } else if (!searchFor.isEmpty()) {
    //   getRestaurantsResponse =
    //       restaurantService.findRestaurantsBySearchQuery(getRestaurantsRequest, clientTime);
    // } else {
    //   return ResponseEntity.badRequest().body(null);
    // }
    // log.info("getRestaurants returned {}", getRestaurantsResponse);
    // CHECKSTYLE:ON

    // Remove all non-alpha-numeric characters from the names of restaurants
  //   List<Restaurant> selectedRestaurantsCloseBy = getRestaurantsResponse.getRestaurants();
    if (getRestaurantsResponse != null && !getRestaurantsResponse.getRestaurants().isEmpty()) {
    for (Restaurant r : getRestaurantsResponse.getRestaurants()) {
      if (!r.getName().matches("^[a-zA-Z0-9]*$")) {
        r.setName(r.getName().replaceAll("[^a-zA-Z0-9]", "-"));
      }
    }
  }

    return ResponseEntity.ok().body(getRestaurantsResponse);
  }
  // http://localhost:8081/qeats/v1/restaurants


  // TODO: CRIO_TASK_MODULE_MULTITHREADING
  //  Improve the performance of this GetRestaurants API
  //  and keep the functionality same.
  // Get the list of open restaurants near the specified latitude/longitude & matching searchFor.
  // API URI: /qeats/v1/restaurants?latitude=21.93&longitude=23.0&searchFor=tamil
  // Method: GET
  // Query Params: latitude, longitude, searchFor(optional)
  // Success Output:
  // 1). If searchFor param is present, return restaurants as a list matching the following criteria
  //   1) open now
  //   2) is near the specified latitude and longitude
  //   3) searchFor matching(partially or fully):
  //      - restaurant name
  //      - or restaurant attribute
  //      - or item name
  //      - or item attribute (all matching is done ignoring case)
  //
  //   4) order the list by following the rules before returning
  //      1) Restaurant name
  //          - exact matches first
  //          - partial matches second
  //      2) Restaurant attributes
  //          - partial and full matches in any order
  //      3) Item name
  //          - exact matches first
  //          - partial matches second
  //      4) Item attributes
  //          - partial and full matches in any order
  //      Eg: For example, when user searches for "Udupi", "Udupi Bhavan" restaurant should
  //      come ahead of restaurants having "Udupi" in attribute.
  // 2). If searchFor param is absent,
  //     1) If there are restaurants near by return the list
  //     2) Else return empty list
  //
  // - For peak hours: 8AM-10AM, 1PM-2PM, 7PM-9PM
  //   - service radius is 3KMs.
  // - All other times
  //   - serving radius is 5KMs.
  // - If there are no restaurants, return empty list of restaurants.
  //
  //
  // HTTP Code: 200
  // {
  //  "restaurants": [
  //    {
  //      "restaurantId": "10",
  //      "name": "A2B",
  //      "city": "Hsr Layout",
  //      "imageUrl": "www.google.com",
  //      "latitude": 20.027,
  //      "longitude": 30.0,
  //      "opensAt": "18:00",
  //      "closesAt": "23:00",
  //      "attributes": [
  //        "Tamil",
  //        "South Indian"
  //      ]
  //    }
  //  ]
  // }
  //
  // Error Response:
  // HTTP Code: 4xx, if client side error.
  //          : 5xx, if server side error.
  // Eg:
  // curl -X GET "http://localhost:8081/qeats/v1/restaurants?latitude=28.4900591&longitude=77.536386&searchFor=tamil"


  // TIP(MODULE_MENUAPI): Model Implementation for getting menu given a restaurantId.
  // Get the Menu for the given restaurantId
  // API URI: /qeats/v1/menu?restaurantId=11
  // Method: GET
  // Query Params: restaurantId
  // Success Output:
  // 1). If restaurantId is present return Menu
  // 2). Otherwise respond with BadHttpRequest.
  //
  // HTTP Code: 200
  // {
  //  "menu": {
  //    "items": [
  //      {
  //        "attributes": [
  //          "South Indian"
  //        ],
  //        "id": "1",
  //        "imageUrl": "www.google.com",
  //        "itemId": "10",
  //        "name": "Idly",
  //        "price": 45
  //      }
  //    ],
  //    "restaurantId": "11"
  //  }
  // }
  // Error Response:
  // HTTP Code: 4xx, if client side error.
  //          : 5xx, if server side error.
  // Eg:
  // curl -X GET "http://localhost:8081/qeats/v1/menu?restaurantId=11"



}

