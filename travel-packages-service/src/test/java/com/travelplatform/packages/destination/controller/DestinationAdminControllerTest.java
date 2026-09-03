package com.travelplatform.packages.destination.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import com.travelplatform.packages.destination.exception.DestinationNotFoundException;
import com.travelplatform.packages.destination.service.DestinationService;
import com.travelplatform.packages.exception.GlobalExceptionHandler;

/**
 * Note: standalone MockMvc setup does not evaluate @PreAuthorize (no method-security
 * infrastructure registered), so these tests cover the controller's business logic
 * and response shape only — not ROLE_ADMIN enforcement itself, which is verified by
 * SecurityConfig's path matchers at the gateway-header level. Same caveat as
 * PackageAdminControllerTest.
 */
@ExtendWith(MockitoExtension.class)
class DestinationAdminControllerTest {

    @Mock private DestinationService destinationService;
    @InjectMocks private DestinationAdminController adminController;

    private MockMvc mockMvc;

    private static final String CREATE_JSON = """
            {"name":"Goa","state":"Goa","country":"India","category":"BEACH",
             "averageBudget":12000.0,"recommendedDays":4,
             "attractions":[{"name":"Baga Beach","attractionType":"Beach"}],
             "activities":[{"name":"Scuba Diving","category":"Water Sports"}]}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    private Destination buildDestination(UUID id) {
        Destination d = new Destination();
        d.setId(id);
        d.setName("Goa");
        d.setCountry("India");
        d.setCategory(DestinationCategory.BEACH);
        d.setActive(true);
        return d;
    }

    @Nested
    class GetAllDestinationsForAdmin {

        @Test
        void returns200_includingDelistedDestinations() throws Exception {
            Destination active = buildDestination(UUID.randomUUID());
            Destination delisted = buildDestination(UUID.randomUUID());
            delisted.setActive(false);
            when(destinationService.getAllDestinationsForAdmin()).thenReturn(java.util.List.of(active, delisted));

            mockMvc.perform(get("/destinations/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()", is(2)))
                    .andExpect(jsonPath("$[1].active", is(false)));
        }
    }

    @Nested
    class CreateDestination {

        @Test
        void returns201_withCreatedDestination() throws Exception {
            when(destinationService.createDestination(any())).thenReturn(buildDestination(UUID.randomUUID()));

            mockMvc.perform(post("/destinations/admin")
                    .contentType("application/json").content(CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name", is("Goa")));
        }

        @Test
        void returns400_whenNameMissing() throws Exception {
            String invalid = CREATE_JSON.replace("\"name\":\"Goa\",", "");

            mockMvc.perform(post("/destinations/admin")
                    .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.name").exists());

            verifyNoInteractions(destinationService);
        }

        @Test
        void returns400_whenCategoryMissing() throws Exception {
            String invalid = CREATE_JSON.replace("\"category\":\"BEACH\",", "");

            mockMvc.perform(post("/destinations/admin")
                    .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.category").exists());
        }
    }

    @Nested
    class UpdateDestination {

        @Test
        void returns200_withUpdatedDestination() throws Exception {
            UUID destinationId = UUID.randomUUID();
            Destination updated = buildDestination(destinationId);
            updated.setAverageBudget(15000.0);
            when(destinationService.updateDestination(eq(destinationId), any())).thenReturn(updated);

            mockMvc.perform(put("/destinations/admin/" + destinationId)
                    .contentType("application/json").content("{\"averageBudget\":15000.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.averageBudget", is(15000.0)));
        }

        @Test
        void returns404_whenDestinationNotFound() throws Exception {
            UUID destinationId = UUID.randomUUID();
            when(destinationService.updateDestination(eq(destinationId), any()))
                    .thenThrow(new DestinationNotFoundException(destinationId));

            mockMvc.perform(put("/destinations/admin/" + destinationId)
                    .contentType("application/json").content("{\"averageBudget\":15000.0}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeleteDestination {

        @Test
        void returns204_whenDelisted() throws Exception {
            UUID destinationId = UUID.randomUUID();

            mockMvc.perform(delete("/destinations/admin/" + destinationId))
                    .andExpect(status().isNoContent());

            verify(destinationService).deleteDestination(destinationId);
        }

        @Test
        void returns404_whenDestinationNotFound() throws Exception {
            UUID destinationId = UUID.randomUUID();
            doThrow(new DestinationNotFoundException(destinationId))
                    .when(destinationService).deleteDestination(destinationId);

            mockMvc.perform(delete("/destinations/admin/" + destinationId))
                    .andExpect(status().isNotFound());
        }
    }
}
