
/*
 *
 *  * Copyright (c) Crio.Do 2019. All rights reserved
 *
 */

package com.crio.qeats.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.crio.qeats.QEatsApplication;
import com.crio.qeats.dto.Restaurant;
import com.crio.qeats.exchanges.GetRestaurantsRequest;
import com.crio.qeats.exchanges.GetRestaurantsResponse;
import com.crio.qeats.repositoryservices.RestaurantRepositoryService;
import com.crio.qeats.utils.FixtureHelpers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest(classes = {QEatsApplication.class})
@MockitoSettings(strictness = Strictness.STRICT_STUBS)
@DirtiesContext
@ActiveProfiles("test")
class RestaurantServiceTest {

  private static final String FIXTURES = "fixtures/exchanges";
  @InjectMocks
  private RestaurantServiceImpl restaurantService;
  @MockBean
  private RestaurantRepositoryService restaurantRepositoryServiceMock;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    MockitoAnnotations.initMocks(this);

    objectMapper = new ObjectMapper();
  }

  private String getServingRadius(List<Restaurant> restaurants, LocalTime timeOfService) {
    when(restaurantRepositoryServiceMock.findAllRestaurantsCloseBy(any(Double.class),
        any(Double.class), any(LocalTime.class), any(Double.class))).thenReturn(restaurants);

    GetRestaurantsResponse allRestaurantsCloseBy = restaurantService
        .findAllRestaurantsCloseBy(new GetRestaurantsRequest(20.0, 30.0), timeOfService); // LocalTime.of(19,00));

    // Purn: It should have been like this as per my current understaning. Will check later
    // GetRestaurantsResponse allRestaurantsCloseBy2 = new GetRestaurantsResponse(
    // restaurantRepositoryServiceMock.findAllRestaurantsCloseBy(20.0, 30.0, timeOfService, 3.0));
    // // LocalTime.of(19,00));

    assertEquals(2, allRestaurantsCloseBy.getRestaurants().size());
    assertEquals("11", allRestaurantsCloseBy.getRestaurants().get(0).getRestaurantId());
    assertEquals("12", allRestaurantsCloseBy.getRestaurants().get(1).getRestaurantId());

    ArgumentCaptor<Double> servingRadiusInKms = ArgumentCaptor.forClass(Double.class);
    verify(restaurantRepositoryServiceMock, times(1))
        .findAllRestaurantsCloseBy(any(Double.class), any(Double.class), any(LocalTime.class),
            servingRadiusInKms.capture());

    return servingRadiusInKms.getValue().toString();
  }

  @Test
  void peakHourServingRadiusOf3KmsAt7Pm() throws IOException {
    assertEquals("3.0", getServingRadius(loadRestaurantsDuringPeakHours(), LocalTime.of(19, 00)));
  }


  @Test
  void normalHourServingRadiusIs5Kms() throws IOException {

      // TODO: CRIO_TASK_MODULE_RESTAURANTSAPI
      // We must ensure the API retrieves only restaurants that are closeby and are open
      // In short, we need to test:
      // 1. If the mocked service methods are being called
      // 2. If the expected restaurants are being returned
      // HINT: Use the `loadRestaurantsDuringNormalHours` utility method to speed things up
      List<Restaurant> restaurants = loadRestaurantsDuringNormalHours();
      when(restaurantRepositoryServiceMock.findAllRestaurantsCloseBy(any(Double.class),
              any(Double.class), any(LocalTime.class), any(Double.class))).thenReturn(restaurants);

      LocalTime timeOfService = LocalTime.of(22, 00);
      GetRestaurantsResponse allRestaurantsCloseBy = restaurantService
              .findAllRestaurantsCloseBy(new GetRestaurantsRequest(20.0, 30.0), timeOfService);

      // Crio's original, copied from Mannan's GitHub so error expected.
      assertEquals(3, allRestaurantsCloseBy.getRestaurants().size());
      assertEquals("10", allRestaurantsCloseBy.getRestaurants().get(0).getRestaurantId());
      assertEquals("11", allRestaurantsCloseBy.getRestaurants().get(1).getRestaurantId());
      assertEquals("12", allRestaurantsCloseBy.getRestaurants().get(2).getRestaurantId());

      // Purn added below to pass this individual test class because it reads wrong copy of JSON
      // file present in the main directory.
      // assertEquals(4, allRestaurantsCloseBy.getRestaurants().size());
      // assertEquals("10", allRestaurantsCloseBy.getRestaurants().get(0).getRestaurantId());
      // assertEquals("13", allRestaurantsCloseBy.getRestaurants().get(3).getRestaurantId());

      ArgumentCaptor<Double> servingRadiusInKms = ArgumentCaptor.forClass(Double.class);
      verify(restaurantRepositoryServiceMock, times(1)).findAllRestaurantsCloseBy(any(Double.class),
              any(Double.class), any(LocalTime.class), servingRadiusInKms.capture());

      assertEquals("5.0", servingRadiusInKms.getValue().toString());
  }

  
  @Test
  void normalHourFindRestaurantsSearchQuery() throws IOException {
      when(restaurantRepositoryServiceMock.findRestaurantsByName(any(Double.class),
              any(Double.class), any(String.class), any(LocalTime.class), any(Double.class)))
                      .thenReturn(loadRestaurantsDuringNormalHours());
      when(restaurantRepositoryServiceMock.findRestaurantsByAttributes(any(Double.class),
              any(Double.class), any(String.class), any(LocalTime.class), any(Double.class)))
                      .thenReturn(loadRestaurantsSearchedByAttributes());

      GetRestaurantsRequest getRestaurantsRequest = new GetRestaurantsRequest(20.0, 30.0);
      getRestaurantsRequest.setSearchFor("Test");

      GetRestaurantsResponse allRestaurantsSearchResults = restaurantService
              .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.of(22, 0));

      verify(restaurantRepositoryServiceMock, times(1)).findRestaurantsByName(any(Double.class),
              any(Double.class), any(String.class), any(LocalTime.class), any(Double.class));
      verify(restaurantRepositoryServiceMock, times(1)).findRestaurantsByAttributes(
              any(Double.class), any(Double.class), any(String.class), any(LocalTime.class),
              any(Double.class));

      // Crio's original
      assertEquals(4, allRestaurantsSearchResults.getRestaurants().size());
      assertEquals("10", allRestaurantsSearchResults.getRestaurants().get(0).getRestaurantId());
      assertEquals("11", allRestaurantsSearchResults.getRestaurants().get(1).getRestaurantId());
      assertEquals("12", allRestaurantsSearchResults.getRestaurants().get(2).getRestaurantId());
      assertEquals("abcdc864835e31495d621234",
      allRestaurantsSearchResults.getRestaurants().get(3).getRestaurantId());

      // Purn made correction bcz there are two JSON input files with the same name
      // and the wrong one is called when loadRestaurantsDuringNormalHours() is called.
    //   assertEquals(5, allRestaurantsSearchResults.getRestaurants().size());
    //   assertEquals("10", allRestaurantsSearchResults.getRestaurants().get(0).getRestaurantId());
    //   assertEquals("11", allRestaurantsSearchResults.getRestaurants().get(1).getRestaurantId());
    //   assertEquals("12", allRestaurantsSearchResults.getRestaurants().get(2).getRestaurantId());
    //   assertEquals("abcdc864835e31495d621234",
    //           allRestaurantsSearchResults.getRestaurants().get(4).getRestaurantId());

      ArgumentCaptor<Double> servingRadiusInKms = ArgumentCaptor.forClass(Double.class);
      verify(restaurantRepositoryServiceMock, times(1)).findRestaurantsByName(any(Double.class),
              any(Double.class), any(String.class), any(LocalTime.class),
              servingRadiusInKms.capture());
      assertEquals(servingRadiusInKms.getValue().toString(), "5.0");

      verify(restaurantRepositoryServiceMock, times(1)).findRestaurantsByAttributes(
              any(Double.class), any(Double.class), any(String.class), any(LocalTime.class),
              servingRadiusInKms.capture());
      assertEquals(servingRadiusInKms.getValue().toString(), "5.0");
  }

  @Test
  void peakHourFindRestaurantsSearchQuery() throws IOException {
    when(restaurantRepositoryServiceMock.findRestaurantsByName(any(Double.class),
        any(Double.class), any(String.class), any(LocalTime.class), any(Double.class)))
        .thenReturn(loadRestaurantsDuringPeakHours());
    when(restaurantRepositoryServiceMock.findRestaurantsByAttributes(any(Double.class),
        any(Double.class), any(String.class), any(LocalTime.class), any(Double.class)))
        .thenReturn(loadRestaurantsSearchedByAttributes());

    GetRestaurantsRequest getRestaurantsRequest = new GetRestaurantsRequest(20.0, 30.0);
    getRestaurantsRequest.setSearchFor("Test");

    GetRestaurantsResponse allRestaurantsSearchResults = restaurantService
        .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.of(20, 0));

    verify(restaurantRepositoryServiceMock, times(1))
        .findRestaurantsByName(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), any(Double.class));
    verify(restaurantRepositoryServiceMock, times(1))
        .findRestaurantsByAttributes(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), any(Double.class));
    assertEquals(3, allRestaurantsSearchResults.getRestaurants().size());
    assertEquals("11", allRestaurantsSearchResults.getRestaurants().get(0).getRestaurantId());
    assertEquals("12", allRestaurantsSearchResults.getRestaurants().get(1).getRestaurantId());
    assertEquals("abcdc864835e31495d621234",
        allRestaurantsSearchResults.getRestaurants().get(2).getRestaurantId());


    ArgumentCaptor<Double> servingRadiusInKms = ArgumentCaptor.forClass(Double.class);
    verify(restaurantRepositoryServiceMock, times(1))
        .findRestaurantsByName(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), servingRadiusInKms.capture());
    assertEquals(servingRadiusInKms.getValue().toString(), "3.0");

    verify(restaurantRepositoryServiceMock, times(1))
        .findRestaurantsByAttributes(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), servingRadiusInKms.capture());
    assertEquals(servingRadiusInKms.getValue().toString(), "3.0");
  }

  @Test
  void findRestaurantsSearchQueryIsEmpty() {
    GetRestaurantsRequest getRestaurantsRequest = new GetRestaurantsRequest(20.0, 30.0);
    getRestaurantsRequest.setSearchFor("");

    GetRestaurantsResponse allRestaurantsCloseBy = restaurantService
        .findRestaurantsBySearchQuery(getRestaurantsRequest, LocalTime.of(22, 0));

    verify(restaurantRepositoryServiceMock, times(0))
        .findRestaurantsByName(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), any(Double.class));
    verify(restaurantRepositoryServiceMock, times(0))
        .findRestaurantsByAttributes(any(Double.class), any(Double.class), any(String.class),
            any(LocalTime.class), any(Double.class));
    assertEquals(0, allRestaurantsCloseBy.getRestaurants().size());
  }

  
  private List<Restaurant> loadRestaurantsDuringNormalHours() throws IOException {
    String fixture =
        FixtureHelpers.fixture(FIXTURES + "/normal_hours_list_of_restaurants.json");

    return objectMapper.readValue(fixture, new TypeReference<List<Restaurant>>() {
    });
  }

  private List<Restaurant> loadRestaurantsSearchedByAttributes() throws IOException {
    String fixture =
        FixtureHelpers.fixture(FIXTURES + "/list_restaurants_searchedby_attributes.json");

    return objectMapper.readValue(fixture, new TypeReference<List<Restaurant>>() {
    });
  }

  private List<Restaurant> loadRestaurantsDuringPeakHours() throws IOException {
    String fixture =
        FixtureHelpers.fixture(FIXTURES + "/peak_hours_list_of_restaurants.json");

    return objectMapper.readValue(fixture, new TypeReference<List<Restaurant>>() {
    });
  }
}
