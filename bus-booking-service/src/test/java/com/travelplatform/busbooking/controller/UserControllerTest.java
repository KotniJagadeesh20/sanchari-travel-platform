package com.travelplatform.busbooking.controller;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.travelplatform.busbooking.entity.Bookingdetails;
import com.travelplatform.busbooking.entity.Bus;
import com.travelplatform.busbooking.entity.UserAdmin;
import com.travelplatform.busbooking.exception.GlobalExceptionHandler;
import com.travelplatform.busbooking.exception.UnauthorizedBookingActionException;
import com.travelplatform.busbooking.repository.BusRepository;
import com.travelplatform.busbooking.service.BookingDetailsService;
import com.travelplatform.busbooking.service.BusService;
import com.travelplatform.busbooking.service.UserAdminService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private BusService busService;
    @Mock private BookingDetailsService bookingService;
    @Mock private BusRepository busRepository;
    @Mock private UserAdminService userAdminService;

    @InjectMocks private UserController userController;
    private MockMvc mockMvc;

    private static final String BOOKING_JSON =
        "{\"name\":\"Asha\",\"email\":\"asha@example.com\",\"phoneno\":\"9876543210\",\"age\":28}";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(new LocalValidatorFactoryBean())
                .build();
    }

    @Test
    void bookTicket_returns201_whenBusExists() throws Exception {
        UUID busId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        String email = "asha@example.com";

        Bus bus = new Bus();
        bus.setId(busId);
        bus.setPrice(800);

        when(busRepository.findById(busId)).thenReturn(Optional.of(bus));
        when(userAdminService.findOrCreate(eq(userId), eq(email)))
                .thenReturn(new UserAdmin(userId, email));

        Bookingdetails saved = new Bookingdetails();
        saved.setId(bookingId);
        when(bookingService.savebookingdetails(any())).thenReturn(saved);

        mockMvc.perform(post("/api/user/bookticket/" + busId)
                .header("X-Authenticated-Email", email)
                .header("X-Authenticated-User-Id", userId.toString())
                .contentType("application/json").content(BOOKING_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.bookingId", is(bookingId.toString())));
    }

    @Test
    void bookTicket_returns404_whenBusNotFound() throws Exception {
        UUID busId = UUID.randomUUID();
        when(busRepository.findById(busId)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/user/bookticket/" + busId)
                .header("X-Authenticated-Email", "asha@example.com")
                .header("X-Authenticated-User-Id", UUID.randomUUID().toString())
                .contentType("application/json").content(BOOKING_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));

        verify(bookingService, never()).savebookingdetails(any());
    }

    @Test
    void cancelBooking_returns200_whenCancelled() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(bookingService.cancelTickets(bookingId, userId)).thenReturn(true);

        mockMvc.perform(delete("/api/user/cancelbooking/" + bookingId)
                .header("X-Authenticated-User-Id", userId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }

    @Test
    void cancelBooking_returns404_whenNotFound() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(bookingService.cancelTickets(bookingId, userId)).thenReturn(false);

        mockMvc.perform(delete("/api/user/cancelbooking/" + bookingId)
                .header("X-Authenticated-User-Id", userId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    void cancelBooking_returns403_whenBookingBelongsToAnotherUser() throws Exception {
        UUID bookingId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(bookingService.cancelTickets(bookingId, userId))
                .thenThrow(new UnauthorizedBookingActionException("You are not authorized to cancel this booking."));

        mockMvc.perform(delete("/api/user/cancelbooking/" + bookingId)
                .header("X-Authenticated-User-Id", userId.toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)));
    }
}
