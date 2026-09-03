package com.travelplatform.hotel.service;

import com.travelplatform.hotel.client.NotificationClient;
import com.travelplatform.hotel.dto.CreateBookingRequest;
import com.travelplatform.hotel.entity.Hotel;
import com.travelplatform.hotel.entity.HotelBooking;
import com.travelplatform.hotel.entity.Room;
import com.travelplatform.hotel.enums.BookingStatus;
import com.travelplatform.hotel.enums.PaymentStatus;
import com.travelplatform.hotel.exception.BookingNotFoundException;
import com.travelplatform.hotel.exception.HotelNotFoundException;
import com.travelplatform.hotel.exception.InvalidBookingDatesException;
import com.travelplatform.hotel.exception.RoomNotAvailableException;
import com.travelplatform.hotel.exception.RoomNotFoundException;
import com.travelplatform.hotel.exception.UnauthorizedBookingActionException;
import com.travelplatform.hotel.repository.HotelBookingRepository;
import com.travelplatform.hotel.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class HotelBookingServiceImpl implements HotelBookingService {

    @Autowired private HotelBookingRepository bookingRepo;
    @Autowired private RoomRepository roomRepo;
    @Autowired private HotelService hotelService;
    @Autowired private NotificationClient notificationClient;

    @Override
    @Transactional
    public HotelBooking bookHotel(CreateBookingRequest request, UUID userId, String recipientEmail) {
        if (!request.getCheckOutDate().isAfter(request.getCheckInDate())) {
            throw new InvalidBookingDatesException("checkOutDate must be after checkInDate");
        }

        Hotel hotel = hotelService.getHotelById(request.getHotelId());
        if (!Boolean.TRUE.equals(hotel.getActive())) {
            throw new HotelNotFoundException(request.getHotelId());
        }

        Room room = roomRepo.findById(request.getRoomId())
                .orElseThrow(() -> new RoomNotFoundException(request.getRoomId()));
        if (!room.getHotel().getId().equals(hotel.getId()) || !Boolean.TRUE.equals(room.getActive())) {
            throw new RoomNotFoundException(request.getRoomId());
        }

        // Rule: a room type can only be booked while it has spare inventory.
        // See Room.availableRooms Javadoc for the "one pool per room type" caveat.
        if (room.getAvailableRooms() <= 0) {
            throw new RoomNotAvailableException(room.getId());
        }

        long nights = ChronoUnit.DAYS.between(request.getCheckInDate(), request.getCheckOutDate());

        room.setAvailableRooms(room.getAvailableRooms() - 1);
        roomRepo.save(room);

        HotelBooking booking = new HotelBooking();
        booking.setUserId(userId);
        booking.setHotel(hotel);
        booking.setRoom(room);
        booking.setCheckInDate(request.getCheckInDate());
        booking.setCheckOutDate(request.getCheckOutDate());
        booking.setNumberOfGuests(request.getNumberOfGuests());
        booking.setPricePerNight(room.getPricePerNight());
        booking.setTotalAmount(room.getPricePerNight() * nights);
        // Auto-confirm — no payment service yet (see design doc: introduce Payment
        // Service later and evolve this to start PENDING until payment succeeds).
        booking.setBookingStatus(BookingStatus.CONFIRMED);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setSpecialRequest(request.getSpecialRequest());

        HotelBooking saved = bookingRepo.save(booking);

        // Fire-and-forget — see NotificationClientImpl. Never blocks or fails the booking.
        notificationClient.notify(
                userId, recipientEmail, "BOOKING_CONFIRMED",
                "Hotel booking confirmed",
                String.format("Your booking at %s (%s) for %s to %s is confirmed.",
                        hotel.getName(), room.getRoomType(), request.getCheckInDate(), request.getCheckOutDate()),
                saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, UUID callerId, String recipientEmail) {
        HotelBooking booking = getBookingById(bookingId);

        if (!booking.getUserId().equals(callerId)) {
            throw new UnauthorizedBookingActionException(
                    "Only the user who made this booking can cancel it.");
        }

        if (booking.getBookingStatus() == BookingStatus.CANCELLED
                || booking.getBookingStatus() == BookingStatus.CHECKED_OUT) {
            return; // already terminal — cancelling again is a harmless no-op
        }

        Room room = booking.getRoom();
        room.setAvailableRooms(room.getAvailableRooms() + 1);
        roomRepo.save(room);

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        notificationClient.notify(
                callerId, recipientEmail, "BOOKING_CANCELLED",
                "Hotel booking cancelled",
                String.format("Your booking at %s has been cancelled.", booking.getHotel().getName()),
                booking.getId());
    }

    @Override
    public List<HotelBooking> getBookingsByUser(UUID userId) {
        return bookingRepo.findByUserIdOrderByBookingDateDesc(userId);
    }

    @Override
    public List<HotelBooking> getBookingsByHotel(UUID hotelId) {
        return bookingRepo.findByHotelIdOrderByBookingDateDesc(hotelId);
    }

    @Override
    public HotelBooking getBookingById(UUID bookingId) {
        return bookingRepo.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
