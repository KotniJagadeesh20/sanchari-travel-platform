package com.travelplatform.packages.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
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
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.exception.GlobalExceptionHandler;
import com.travelplatform.packages.exception.PackageDepartureNotFoundException;
import com.travelplatform.packages.exception.PackageNotFoundException;
import com.travelplatform.packages.service.PackageBookingService;
import com.travelplatform.packages.service.PackageService;
import com.travelplatform.packages.service.UserRefService;

/**
 * Note: standalone MockMvc setup does not evaluate @PreAuthorize (no method-security
 * infrastructure registered), so these tests cover the controller's business logic
 * and response shape only — not the ROLE_ADMIN enforcement itself, which is a
 * Spring Security concern verified by SecurityConfig's path matchers at the
 * gateway-header level.
 */
@ExtendWith(MockitoExtension.class)
class PackageAdminControllerTest {

    @Mock private PackageService packageService;
    @Mock private PackageBookingService bookingService;
    @Mock private UserRefService userRefService;
    @InjectMocks private PackageAdminController adminController;

    private MockMvc mockMvc;

    private static final String CREATE_JSON = """
            {"title":"Goa Beach Getaway","destinationId":"3fa85f64-5717-4562-b3fc-2c963f66afa6","durationDays":4,
             "durationNights":3,"price":15999.0,"maxPeople":20,
             "inclusions":["Hotel Stay","Breakfast"],
             "itinerary":[{"dayNumber":1,"plan":"Arrival and check-in"}]}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    private TravelPackage buildPackage(UUID id) {
        TravelPackage pkg = new TravelPackage();
        pkg.setId(id);
        pkg.setTitle("Goa Beach Getaway");
        pkg.setDestinationId(UUID.randomUUID());
        pkg.setDurationDays(4);
        pkg.setDurationNights(3);
        pkg.setPrice(15999.0);
        pkg.setMaxPeople(20);
        pkg.setActive(true);
        return pkg;
    }

    @Nested
    class CreatePackage {

        @Test
        void returns201_withCreatedPackage() throws Exception {
            when(userRefService.findOrCreate(any(), any(), any())).thenReturn(new UserRef(UUID.randomUUID(), "creator@example.com", "Creator Name"));
            when(packageService.createPackage(any(), any())).thenReturn(buildPackage(UUID.randomUUID()));

            mockMvc.perform(post("/packages/admin")
                    .header("X-Authenticated-Email", "creator@example.com")
                    .header("X-Authenticated-Name", "Creator Name")
                    .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                    .contentType("application/json").content(CREATE_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title", is("Goa Beach Getaway")))
                    .andExpect(jsonPath("$.maxPeople", is(20)));
        }

        @Test
        void returns400_whenTitleMissing() throws Exception {
            String invalid = CREATE_JSON.replace("\"title\":\"Goa Beach Getaway\",", "");

            mockMvc.perform(post("/packages/admin")
                    .header("X-Authenticated-Email", "creator@example.com")
                    .header("X-Authenticated-Name", "Creator Name")
                    .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                    .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.title").exists());

            verifyNoInteractions(packageService);
        }

        @Test
        void returns400_whenPriceNotPositive() throws Exception {
            String invalid = CREATE_JSON.replace("\"price\":15999.0,", "\"price\":-100.0,");

            mockMvc.perform(post("/packages/admin")
                    .header("X-Authenticated-Email", "creator@example.com")
                    .header("X-Authenticated-Name", "Creator Name")
                    .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                    .contentType("application/json").content(invalid))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.price").exists());
        }
    }

    @Nested
    class UpdatePackage {

        @Test
        void returns200_withUpdatedPackage() throws Exception {
            UUID packageId = UUID.randomUUID();
            TravelPackage updated = buildPackage(packageId);
            updated.setPrice(17999.0);
            when(packageService.updatePackage(eq(packageId), any())).thenReturn(updated);

            mockMvc.perform(put("/packages/admin/" + packageId)
                    .contentType("application/json").content("{\"price\":17999.0}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.price", is(17999.0)));
        }

        @Test
        void returns404_whenPackageNotFound() throws Exception {
            UUID packageId = UUID.randomUUID();
            when(packageService.updatePackage(eq(packageId), any()))
                    .thenThrow(new PackageNotFoundException(packageId));

            mockMvc.perform(put("/packages/admin/" + packageId)
                    .contentType("application/json").content("{\"price\":17999.0}"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class DeletePackage {

        @Test
        void returns204_whenDelisted() throws Exception {
            UUID packageId = UUID.randomUUID();

            mockMvc.perform(delete("/packages/admin/" + packageId))
                    .andExpect(status().isNoContent());

            verify(packageService).deletePackage(packageId);
        }

        @Test
        void returns404_whenPackageNotFound() throws Exception {
            UUID packageId = UUID.randomUUID();
            doThrow(new PackageNotFoundException(packageId)).when(packageService).deletePackage(packageId);

            mockMvc.perform(delete("/packages/admin/" + packageId))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class AdminViews {

        @Test
        void getAllPackages_includesDelistedOnes() throws Exception {
            TravelPackage delisted = buildPackage(UUID.randomUUID());
            delisted.setActive(false);
            when(packageService.getAllPackagesForAdmin()).thenReturn(List.of(delisted));

            mockMvc.perform(get("/packages/admin"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].active", is(false)));
        }

        @Test
        void getBookingsForPackage_returns200_withBookings() throws Exception {
            UUID packageId = UUID.randomUUID();
            TravelPackage pkg = buildPackage(packageId);
            PackageDeparture departure = new PackageDeparture();
            departure.setId(UUID.randomUUID());
            departure.setTravelPackage(pkg);
            departure.setStartDate(LocalDate.now().plusMonths(1));
            departure.setMaxPeople(20);
            departure.setAvailableSlots(18);
            departure.setActive(true);
            UserRef traveler = new UserRef(UUID.randomUUID(), "asha@example.com", "Asha Rao");

            PackageBooking booking = new PackageBooking();
            booking.setId(UUID.randomUUID());
            booking.setDeparture(departure);
            booking.setTraveler(traveler);
            booking.setTravelersCount(2);
            booking.setTotalAmount(31998.0);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.getBookingsByPackage(packageId)).thenReturn(List.of(booking));

            mockMvc.perform(get("/packages/admin/" + packageId + "/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].travelersCount", is(2)));
        }

        @Test
        void getBookingsForPackage_returns404_whenPackageNotFound() throws Exception {
            UUID packageId = UUID.randomUUID();
            when(bookingService.getBookingsByPackage(packageId))
                    .thenThrow(new PackageNotFoundException(packageId));

            mockMvc.perform(get("/packages/admin/" + packageId + "/bookings"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void getMyPackages_scopesToCallersCreatedPackages() throws Exception {
            UUID creatorId = UUID.randomUUID();
            when(packageService.getPackagesByCreator(creatorId)).thenReturn(List.of(buildPackage(UUID.randomUUID())));

            mockMvc.perform(get("/packages/admin/mine")
                    .header("X-Authenticated-User-Id", creatorId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title", is("Goa Beach Getaway")));

            verify(packageService).getPackagesByCreator(creatorId);
        }
    }

    @Nested
    class Departures {

        private PackageDeparture buildDeparture(UUID id, TravelPackage pkg) {
            PackageDeparture d = new PackageDeparture();
            d.setId(id);
            d.setTravelPackage(pkg);
            d.setStartDate(LocalDate.now().plusMonths(1));
            d.setMaxPeople(20);
            d.setAvailableSlots(20);
            d.setActive(true);
            return d;
        }

        @Test
        void addDeparture_returns201() throws Exception {
            UUID packageId = UUID.randomUUID();
            TravelPackage pkg = buildPackage(packageId);
            when(packageService.addDeparture(eq(packageId), any())).thenReturn(buildDeparture(UUID.randomUUID(), pkg));

            mockMvc.perform(post("/packages/admin/" + packageId + "/departures")
                    .contentType("application/json")
                    .content("{\"startDate\":\"" + LocalDate.now().plusMonths(1) + "\",\"maxPeople\":20}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.availableSlots", is(20)))
                    .andExpect(jsonPath("$.active", is(true)));
        }

        @Test
        void addDeparture_returns404_whenPackageNotFound() throws Exception {
            UUID packageId = UUID.randomUUID();
            when(packageService.addDeparture(eq(packageId), any())).thenThrow(new PackageNotFoundException(packageId));

            mockMvc.perform(post("/packages/admin/" + packageId + "/departures")
                    .contentType("application/json")
                    .content("{\"startDate\":\"" + LocalDate.now().plusMonths(1) + "\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void updateDeparture_returns200() throws Exception {
            UUID departureId = UUID.randomUUID();
            TravelPackage pkg = buildPackage(UUID.randomUUID());
            PackageDeparture updated = buildDeparture(departureId, pkg);
            updated.setMaxPeople(25);
            updated.setAvailableSlots(25);
            when(packageService.updateDeparture(eq(departureId), any())).thenReturn(updated);

            mockMvc.perform(put("/packages/admin/departures/" + departureId)
                    .contentType("application/json")
                    .content("{\"maxPeople\":25}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.maxPeople", is(25)));
        }

        @Test
        void updateDeparture_returns404_whenDepartureNotFound() throws Exception {
            UUID departureId = UUID.randomUUID();
            when(packageService.updateDeparture(eq(departureId), any()))
                    .thenThrow(new PackageDepartureNotFoundException(departureId));

            mockMvc.perform(put("/packages/admin/departures/" + departureId)
                    .contentType("application/json")
                    .content("{\"maxPeople\":25}"))
                    .andExpect(status().isNotFound());
        }

        @Test
        void cancelDeparture_returns204() throws Exception {
            UUID departureId = UUID.randomUUID();

            mockMvc.perform(delete("/packages/admin/departures/" + departureId))
                    .andExpect(status().isNoContent());

            verify(packageService).cancelDeparture(departureId);
        }
    }

    @Nested
    class AdminCancelBooking {

        @Test
        void returns204_withReason() throws Exception {
            UUID bookingId = UUID.randomUUID();

            mockMvc.perform(post("/packages/admin/bookings/" + bookingId + "/cancel")
                    .param("reason", "Trip called off due to weather"))
                    .andExpect(status().isNoContent());

            verify(bookingService).cancelBookingAsAdmin(bookingId, "Trip called off due to weather");
        }

        @Test
        void returns204_withoutReason() throws Exception {
            UUID bookingId = UUID.randomUUID();

            mockMvc.perform(post("/packages/admin/bookings/" + bookingId + "/cancel"))
                    .andExpect(status().isNoContent());

            verify(bookingService).cancelBookingAsAdmin(bookingId, null);
        }
    }
}
