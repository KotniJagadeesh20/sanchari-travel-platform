package com.travelplatform.packages.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

import com.travelplatform.packages.dto.TravelerRequest;
import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.enums.PaymentStatus;
import com.travelplatform.packages.exception.GlobalExceptionHandler;
import com.travelplatform.packages.exception.InsufficientSlotsException;
import com.travelplatform.packages.exception.PackageNotBookableException;
import com.travelplatform.packages.exception.UnauthorizedBookingActionException;
import com.travelplatform.packages.service.PackageBookingService;
import com.travelplatform.packages.service.PackageService;
import com.travelplatform.packages.service.UserRefService;

@ExtendWith(MockitoExtension.class)
class PackageControllerTest {

    @Mock private PackageService packageService;
    @Mock private PackageBookingService bookingService;
    @Mock private UserRefService userRefService;
    @InjectMocks private PackageController packageController;

    private MockMvc mockMvc;

    private UUID travelerId;
    private String travelerEmail = "asha@example.com";
    private String travelerName = "Asha Rao";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(packageController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
        travelerId = UUID.randomUUID();
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

    @Nested
    class BrowseAndSearch {

        @Test
        void getAllPackages_returns200_withListedPackagesOnly() throws Exception {
            when(packageService.getAllActivePackages()).thenReturn(List.of(buildPackage(UUID.randomUUID())));

            mockMvc.perform(get("/packages"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title", is("Goa Beach Getaway")));
        }

        @Test
        void getPackagesByDestination_filtersByDestinationId() throws Exception {
            UUID destId = UUID.randomUUID();
            when(packageService.findByDestination(destId)).thenReturn(List.of(buildPackage(UUID.randomUUID())));

            mockMvc.perform(get("/packages/by-destination/" + destId))
                    .andExpect(status().isOk());
        }

        @Test
        void getPackage_returns200_withDepartures() throws Exception {
            UUID packageId = UUID.randomUUID();
            TravelPackage pkg = buildPackage(packageId);
            pkg.getDepartures().add(buildDeparture(UUID.randomUUID(), pkg));
            when(packageService.getPackageById(packageId)).thenReturn(pkg);

            mockMvc.perform(get("/packages/" + packageId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.departures[0].availableSlots", is(20)));
        }
    }

    @Nested
    class BookPackage {

        private static final String BOOK_JSON =
                "{\"travelers\":[{\"name\":\"Asha Rao\",\"age\":29},{\"name\":\"Kiran Rao\",\"age\":31}]}";

        private List<TravelerRequest> twoTravelers() {
            TravelerRequest t1 = new TravelerRequest(); t1.setName("Asha Rao"); t1.setAge(29);
            TravelerRequest t2 = new TravelerRequest(); t2.setName("Kiran Rao"); t2.setAge(31);
            return List.of(t1, t2);
        }

        @Test
        void returns201_withConfirmedBooking() throws Exception {
            UUID packageId = UUID.randomUUID();
            UUID departureId = UUID.randomUUID();
            TravelPackage pkg = buildPackage(packageId);
            PackageDeparture departure = buildDeparture(departureId, pkg);
            UserRef traveler = new UserRef(travelerId, travelerEmail, travelerName);

            when(userRefService.findOrCreate(travelerId, travelerEmail, travelerName)).thenReturn(traveler);

            PackageBooking booking = new PackageBooking();
            booking.setId(UUID.randomUUID());
            booking.setDeparture(departure);
            booking.setTraveler(traveler);
            booking.setTravelersCount(2);
            booking.setTotalAmount(31998.0);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.bookPackage(eq(departureId), any(), eq(traveler))).thenReturn(booking);

            mockMvc.perform(post("/packages/departures/" + departureId + "/book")
                    .header("X-Authenticated-Email", travelerEmail)
                    .header("X-Authenticated-Name", travelerName)
                    .header("X-Authenticated-User-Id", travelerId.toString())
                    .contentType("application/json").content(BOOK_JSON))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status", is("CONFIRMED")))
                    .andExpect(jsonPath("$.paymentStatus", is("PENDING")))
                    .andExpect(jsonPath("$.travelersCount", is(2)));
        }

        @Test
        void returns400_whenInsufficientSlots() throws Exception {
            UUID departureId = UUID.randomUUID();
            UserRef traveler = new UserRef(travelerId, travelerEmail, travelerName);
            when(userRefService.findOrCreate(travelerId, travelerEmail, travelerName)).thenReturn(traveler);
            when(bookingService.bookPackage(eq(departureId), any(), eq(traveler)))
                    .thenThrow(new InsufficientSlotsException(2, 1));

            mockMvc.perform(post("/packages/departures/" + departureId + "/book")
                    .header("X-Authenticated-Email", travelerEmail)
                    .header("X-Authenticated-Name", travelerName)
                    .header("X-Authenticated-User-Id", travelerId.toString())
                    .contentType("application/json").content(BOOK_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message", containsString("available")));
        }

        @Test
        void returns400_whenPackageDelisted() throws Exception {
            UUID departureId = UUID.randomUUID();
            UserRef traveler = new UserRef(travelerId, travelerEmail, travelerName);
            when(userRefService.findOrCreate(travelerId, travelerEmail, travelerName)).thenReturn(traveler);
            when(bookingService.bookPackage(eq(departureId), any(), eq(traveler)))
                    .thenThrow(new PackageNotBookableException(UUID.randomUUID()));

            mockMvc.perform(post("/packages/departures/" + departureId + "/book")
                    .header("X-Authenticated-Email", travelerEmail)
                    .header("X-Authenticated-Name", travelerName)
                    .header("X-Authenticated-User-Id", travelerId.toString())
                    .contentType("application/json").content(BOOK_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void returns400_whenTravelersMissing() throws Exception {
            UUID departureId = UUID.randomUUID();

            mockMvc.perform(post("/packages/departures/" + departureId + "/book")
                    .header("X-Authenticated-Email", travelerEmail)
                    .header("X-Authenticated-Name", travelerName)
                    .header("X-Authenticated-User-Id", travelerId.toString())
                    .contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(bookingService);
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void returns204_whenCancelledByOwner() throws Exception {
            UUID bookingId = UUID.randomUUID();

            mockMvc.perform(delete("/packages/bookings/" + bookingId)
                    .header("X-Authenticated-User-Id", travelerId.toString()))
                    .andExpect(status().isNoContent());

            verify(bookingService).cancelBooking(bookingId, travelerId, null);
        }

        @Test
        void returns204_withReason_whenProvided() throws Exception {
            UUID bookingId = UUID.randomUUID();

            mockMvc.perform(delete("/packages/bookings/" + bookingId)
                    .param("reason", "Change of plans")
                    .header("X-Authenticated-User-Id", travelerId.toString()))
                    .andExpect(status().isNoContent());

            verify(bookingService).cancelBooking(bookingId, travelerId, "Change of plans");
        }

        @Test
        void returns403_whenCallerIsNotTraveler() throws Exception {
            UUID bookingId = UUID.randomUUID();
            UUID strangerId = UUID.randomUUID();
            doThrow(new UnauthorizedBookingActionException("Only the traveler who made this booking can cancel it."))
                    .when(bookingService).cancelBooking(eq(bookingId), eq(strangerId), isNull());

            mockMvc.perform(delete("/packages/bookings/" + bookingId)
                    .header("X-Authenticated-User-Id", strangerId.toString()))
                    .andExpect(status().isForbidden());
        }
    }

    @Nested
    class MyBookings {

        @Test
        void returns200_withTravelersOwnBookings() throws Exception {
            TravelPackage pkg = buildPackage(UUID.randomUUID());
            PackageDeparture departure = buildDeparture(UUID.randomUUID(), pkg);
            UserRef traveler = new UserRef(travelerId, travelerEmail, travelerName);

            PackageBooking booking = new PackageBooking();
            booking.setId(UUID.randomUUID());
            booking.setDeparture(departure);
            booking.setTraveler(traveler);
            booking.setTravelersCount(2);
            booking.setTotalAmount(31998.0);
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setBookingTime(java.time.LocalDateTime.now());

            when(bookingService.getBookingsByTraveler(travelerId)).thenReturn(List.of(booking));

            mockMvc.perform(get("/packages/bookings")
                    .header("X-Authenticated-User-Id", travelerId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status", is("CONFIRMED")));
        }
    }
}
