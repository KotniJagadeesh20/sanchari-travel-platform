package com.travelplatform.packages.destination.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.travelplatform.packages.destination.entity.Activity;
import com.travelplatform.packages.destination.entity.Attraction;
import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import com.travelplatform.packages.destination.exception.DestinationNotFoundException;
import com.travelplatform.packages.destination.service.DestinationService;
import com.travelplatform.packages.exception.GlobalExceptionHandler;

@ExtendWith(MockitoExtension.class)
class DestinationControllerTest {

    @Mock private DestinationService destinationService;
    @InjectMocks private DestinationController destinationController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(destinationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Destination buildDestination(UUID id) {
        Destination d = new Destination();
        d.setId(id);
        d.setName("Goa");
        d.setState("Goa");
        d.setCountry("India");
        d.setCategory(DestinationCategory.BEACH);
        d.setAverageBudget(12000.0);
        d.setManualRating(4.5);
        d.setActive(true);
        return d;
    }

    @Nested
    class BrowseAndDetail {

        @Test
        void getAllDestinations_returns200_withListedDestinations() throws Exception {
            when(destinationService.getAllActiveDestinations()).thenReturn(List.of(buildDestination(UUID.randomUUID())));

            mockMvc.perform(get("/destinations"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name", is("Goa")));
        }

        @Test
        void getDestination_returns200_withAttractionsAndActivities() throws Exception {
            UUID destinationId = UUID.randomUUID();
            Destination destination = buildDestination(destinationId);

            Attraction attraction = new Attraction();
            attraction.setId(UUID.randomUUID());
            attraction.setName("Baga Beach");
            destination.getAttractions().add(attraction);

            Activity activity = new Activity();
            activity.setId(UUID.randomUUID());
            activity.setName("Scuba Diving");
            destination.getActivities().add(activity);

            when(destinationService.getDestinationById(destinationId)).thenReturn(destination);

            mockMvc.perform(get("/destinations/" + destinationId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.attractions[0].name", is("Baga Beach")))
                    .andExpect(jsonPath("$.activities[0].name", is("Scuba Diving")));
        }

        @Test
        void getDestination_returns404_whenNotFound() throws Exception {
            UUID destinationId = UUID.randomUUID();
            when(destinationService.getDestinationById(destinationId))
                    .thenThrow(new DestinationNotFoundException(destinationId));

            mockMvc.perform(get("/destinations/" + destinationId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class Search {

        @Test
        void search_passesAllProvidedFiltersTogetherToTheService() throws Exception {
            when(destinationService.search("goa", DestinationCategory.BEACH, 15000.0, 11))
                    .thenReturn(List.of(buildDestination(UUID.randomUUID())));

            mockMvc.perform(get("/destinations/search")
                    .param("keyword", "goa").param("category", "BEACH")
                    .param("maxBudget", "15000.0").param("visitMonth", "11"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].name", is("Goa")));

            verify(destinationService).search("goa", DestinationCategory.BEACH, 15000.0, 11);
        }

        @Test
        void search_byKeywordOnly() throws Exception {
            when(destinationService.search("goa", null, null, null))
                    .thenReturn(List.of(buildDestination(UUID.randomUUID())));

            mockMvc.perform(get("/destinations/search").param("keyword", "goa"))
                    .andExpect(status().isOk());

            verify(destinationService).search("goa", null, null, null);
        }

        @Test
        void getByCategory_pathVariant_returns200() throws Exception {
            when(destinationService.search(null, DestinationCategory.HILL_STATION, null, null))
                    .thenReturn(List.of(buildDestination(UUID.randomUUID())));

            mockMvc.perform(get("/destinations/category/HILL_STATION"))
                    .andExpect(status().isOk());
        }

        @Test
        void getPopularDestinations_returns200() throws Exception {
            when(destinationService.getPopularDestinations()).thenReturn(List.of(buildDestination(UUID.randomUUID())));

            mockMvc.perform(get("/destinations/popular"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].manualRating", is(4.5)));
        }
    }
}
