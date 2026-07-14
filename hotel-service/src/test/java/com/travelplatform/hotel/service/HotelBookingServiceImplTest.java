package com.travelplatform.hotel.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.hotel.client.NotificationClient;
import com.travelplatform.hotel.dto.CreateBookingRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.HotelBooking;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.BookingStatus;
import com.travelplatform.hotel.enums.RoomType;
import com.travelplatform.hotel.exception.InvalidBookingDatesException;
import com.travelplatform.hotel.exception.RoomNotAvailableException;
import com.travelplatform.hotel.exception.UnauthorizedBookingActionException;
import com.travelplatform.hotel.repository.HotelBookingRepository;
import com.travelplatform.hotel.repository.RoomRepository;

@ExtendWith(MockitoExtension.class)
class HotelBookingServiceImplTest {

    @Mock private HotelBookingRepository bookingRepo;
    @Mock private RoomRepository roomRepo;
    @Mock private HotelService hotelService;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private HotelBookingServiceImpl bookingService;

    private UUID userId;
    private Hotel hotel;
    private Room room;
    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();

        hotel = new Hotel();
        hotel.setId(UUID.randomUUID());
        hotel.setActive(true);

        room = new Room();
        room.setId(UUID.randomUUID());
        room.setHotel(hotel);
        room.setActive(true);
        room.setRoomType(RoomType.DELUXE);
        room.setPricePerNight(1000.0);
        room.setTotalRooms(5);
        room.setAvailableRooms(5);

        request = new CreateBookingRequest();
        request.setHotelId(hotel.getId());
        request.setRoomId(room.getId());
        request.setCheckInDate(LocalDate.now().plusDays(1));
        request.setCheckOutDate(LocalDate.now().plusDays(4));
        request.setNumberOfGuests(2);
    }

    @Nested
    class BookHotel {

        @Test
        void createsBooking_confirmedImmediately_andDecrementsAvailability() {
            when(hotelService.getHotelById(hotel.getId())).thenReturn(hotel);
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
            when(roomRepo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
            when(bookingRepo.save(any(HotelBooking.class))).thenAnswer(i -> i.getArgument(0));

            HotelBooking booking = bookingService.bookHotel(request, userId, "traveler@example.com");

            assertEquals(BookingStatus.CONFIRMED, booking.getBookingStatus(),
                    "Hotel bookings auto-confirm immediately — no payment step yet");
            assertEquals(3000.0, booking.getTotalAmount(), "3 nights * 1000/night");
            assertEquals(4, room.getAvailableRooms(), "Availability decremented by one on booking");
        }

        @Test
        void throwsRoomNotAvailable_whenNoInventoryLeft() {
            room.setAvailableRooms(0);
            when(hotelService.getHotelById(hotel.getId())).thenReturn(hotel);
            when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

            assertThrows(RoomNotAvailableException.class, () -> bookingService.bookHotel(request, userId, "traveler@example.com"));
            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsInvalidBookingDates_whenCheckOutNotAfterCheckIn() {
            request.setCheckOutDate(request.getCheckInDate());

            assertThrows(InvalidBookingDatesException.class, () -> bookingService.bookHotel(request, userId, "traveler@example.com"));
            verifyNoInteractions(roomRepo, bookingRepo);
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void restoresAvailability_andMarksCancelled() {
            room.setAvailableRooms(4);
            HotelBooking booking = new HotelBooking();
            booking.setId(UUID.randomUUID());
            booking.setUserId(userId);
            booking.setRoom(room);
            booking.setBookingStatus(BookingStatus.CONFIRMED);

            when(bookingRepo.findById(booking.getId())).thenReturn(Optional.of(booking));
            when(roomRepo.save(any(Room.class))).thenAnswer(i -> i.getArgument(0));
            when(bookingRepo.save(any(HotelBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBooking(booking.getId(), userId, "traveler@example.com");

            assertEquals(BookingStatus.CANCELLED, booking.getBookingStatus());
            assertEquals(5, room.getAvailableRooms(), "Availability restored on cancel");
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotOwner() {
            HotelBooking booking = new HotelBooking();
            booking.setId(UUID.randomUUID());
            booking.setUserId(UUID.randomUUID()); // different user
            booking.setRoom(room);
            booking.setBookingStatus(BookingStatus.CONFIRMED);

            when(bookingRepo.findById(booking.getId())).thenReturn(Optional.of(booking));

            assertThrows(UnauthorizedBookingActionException.class,
                    () -> bookingService.cancelBooking(booking.getId(), userId, "traveler@example.com"));
            verify(roomRepo, never()).save(any());
        }
    }
}
