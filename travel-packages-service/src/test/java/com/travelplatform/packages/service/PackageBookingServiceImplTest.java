package com.travelplatform.packages.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.packages.client.NotificationClient;
import com.travelplatform.packages.dto.TravelerRequest;
import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.enums.PaymentStatus;
import com.travelplatform.packages.exception.*;
import com.travelplatform.packages.repository.PackageBookingRepository;
import com.travelplatform.packages.repository.PackageDepartureRepository;
import com.travelplatform.packages.repository.TravelPackageRepository;

@ExtendWith(MockitoExtension.class)
class PackageBookingServiceImplTest {

    @Mock private PackageBookingRepository bookingRepo;
    @Mock private PackageDepartureRepository departureRepo;
    @Mock private TravelPackageRepository packageRepo;
    @Mock private NotificationClient notificationClient;
    @InjectMocks private PackageBookingServiceImpl bookingService;

    private UserRef traveler;
    private TravelPackage pkg;
    private PackageDeparture departure;
    private UUID packageId;
    private UUID departureId;
    private List<TravelerRequest> twoTravelers;

    private TravelerRequest travelerReq(String name, int age) {
        TravelerRequest t = new TravelerRequest();
        t.setName(name);
        t.setAge(age);
        return t;
    }

    @BeforeEach
    void setUp() {
        traveler = new UserRef(UUID.randomUUID(), "asha@example.com", "Asha Rao");

        packageId = UUID.randomUUID();
        pkg = new TravelPackage();
        pkg.setId(packageId);
        pkg.setActive(true);
        pkg.setMaxPeople(20);
        pkg.setPrice(15999.0);

        departureId = UUID.randomUUID();
        departure = new PackageDeparture();
        departure.setId(departureId);
        departure.setTravelPackage(pkg);
        departure.setStartDate(LocalDate.now().plusMonths(1));
        departure.setMaxPeople(20);
        departure.setAvailableSlots(20);
        departure.setActive(true);

        twoTravelers = List.of(travelerReq("Asha Rao", 29), travelerReq("Kiran Rao", 31));
    }

    @Nested
    class BookPackage {

        @Test
        void createsBooking_confirmedImmediately_andDeductsSlotsFromTheDeparture() {
            when(departureRepo.findById(departureId)).thenReturn(Optional.of(departure));
            // Slot deduction is now one atomic conditional UPDATE, not read-check-write —
            // see PackageDepartureRepository.decrementAvailableSlots(). 1 = success.
            when(departureRepo.decrementAvailableSlots(departureId, 2)).thenReturn(1);
            when(bookingRepo.save(any(PackageBooking.class))).thenAnswer(i -> i.getArgument(0));

            PackageBooking booking = bookingService.bookPackage(departureId, twoTravelers, traveler);

            assertEquals(BookingStatus.CONFIRMED, booking.getStatus(), "Packages auto-confirm, unlike ride-share's PENDING flow");
            assertEquals(PaymentStatus.PENDING, booking.getPaymentStatus(), "no payment gateway integrated yet — honest PENDING, not PAID");
            assertEquals(2, booking.getTravelersCount());
            assertEquals(2, booking.getTravelers().size());
            assertEquals(31998.0, booking.getTotalAmount());
            verify(departureRepo).decrementAvailableSlots(departureId, 2);
        }

        @Test
        void throwsInsufficientSlots_whenRequestExceedsDepartureAvailability() {
            when(departureRepo.findById(departureId)).thenReturn(Optional.of(departure));
            // 0 = the atomic conditional UPDATE's WHERE clause didn't match (not enough slots).
            when(departureRepo.decrementAvailableSlots(departureId, 2)).thenReturn(0);

            assertThrows(InsufficientSlotsException.class,
                    () -> bookingService.bookPackage(departureId, twoTravelers, traveler));

            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsPackageNotBookable_whenPackageIsDelisted() {
            pkg.setActive(false);
            when(departureRepo.findById(departureId)).thenReturn(Optional.of(departure));

            assertThrows(PackageNotBookableException.class,
                    () -> bookingService.bookPackage(departureId, twoTravelers, traveler));

            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsPackageNotBookable_whenThisSpecificDepartureIsCancelled() {
            departure.setActive(false); // package itself still active — only this batch was cancelled
            when(departureRepo.findById(departureId)).thenReturn(Optional.of(departure));

            assertThrows(PackageNotBookableException.class,
                    () -> bookingService.bookPackage(departureId, twoTravelers, traveler));

            verify(bookingRepo, never()).save(any());
        }

        @Test
        void throwsDepartureNotFound_whenDepartureDoesNotExist() {
            when(departureRepo.findById(departureId)).thenReturn(Optional.empty());

            assertThrows(PackageDepartureNotFoundException.class,
                    () -> bookingService.bookPackage(departureId, twoTravelers, traveler));
        }
    }

    @Nested
    class CancelBooking {

        private PackageBooking booking;
        private UUID bookingId;

        @BeforeEach
        void setUp() {
            bookingId = UUID.randomUUID();
            booking = new PackageBooking();
            booking.setId(bookingId);
            booking.setDeparture(departure);
            booking.setTraveler(traveler);
            booking.setTravelersCount(2);
        }

        @Test
        void cancelsConfirmedBooking_andReturnsSlotsToTheDeparture() {
            booking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepo.save(any(PackageBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBooking(bookingId, traveler.getId(), "Change of plans");

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            assertEquals("Change of plans", booking.getCancellationReason());
            // Slot release is now one atomic UPDATE — see
            // PackageDepartureRepository.incrementAvailableSlots().
            verify(departureRepo).incrementAvailableSlots(departureId, 2);
        }

        @Test
        void throwsUnauthorized_whenCallerIsNotTheTraveler() {
            booking.setStatus(BookingStatus.CONFIRMED);
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));

            UUID strangerId = UUID.randomUUID();
            assertThrows(UnauthorizedBookingActionException.class,
                    () -> bookingService.cancelBooking(bookingId, strangerId, null));

            verify(bookingRepo, never()).save(any());
            verify(departureRepo, never()).incrementAvailableSlots(any(), anyInt());
        }

        @Test
        void throwsBookingNotFound_whenBookingDoesNotExist() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class,
                    () -> bookingService.cancelBooking(bookingId, traveler.getId(), null));
        }
    }

    @Nested
    class CancelBookingAsAdmin {

        private PackageBooking booking;
        private UUID bookingId;

        @BeforeEach
        void setUp() {
            bookingId = UUID.randomUUID();
            booking = new PackageBooking();
            booking.setId(bookingId);
            booking.setDeparture(departure);
            booking.setTraveler(traveler);
            booking.setTravelersCount(2);
            booking.setStatus(BookingStatus.CONFIRMED);
        }

        @Test
        void cancelsBooking_withNoOwnershipCheck_andReturnsSlots() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepo.save(any(PackageBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBookingAsAdmin(bookingId, "Trip called off due to weather");

            assertEquals(BookingStatus.CANCELLED, booking.getStatus());
            assertEquals("Trip called off due to weather", booking.getCancellationReason());
            verify(departureRepo).incrementAvailableSlots(departureId, 2);
        }

        @Test
        void notifiesTraveler_ofOperatorCancellation() {
            when(bookingRepo.findById(bookingId)).thenReturn(Optional.of(booking));
            when(bookingRepo.save(any(PackageBooking.class))).thenAnswer(i -> i.getArgument(0));

            bookingService.cancelBookingAsAdmin(bookingId, "Low bookings");

            verify(notificationClient).notify(
                    eq(traveler.getId()), eq(traveler.getEmail()), eq("BOOKING_CANCELLED_BY_OPERATOR"),
                    any(), any(), eq(bookingId));
        }
    }

    @Nested
    class Reads {

        @Test
        void getBookingsByTraveler_delegatesToRepository() {
            when(bookingRepo.findByTravelerId(traveler.getId())).thenReturn(List.of(new PackageBooking()));
            assertEquals(1, bookingService.getBookingsByTraveler(traveler.getId()).size());
        }

        @Test
        void getBookingsByPackage_returnsBookings_whenPackageExists() {
            when(packageRepo.existsById(packageId)).thenReturn(true);
            when(bookingRepo.findByDeparture_TravelPackage_Id(packageId)).thenReturn(List.of(new PackageBooking()));

            assertEquals(1, bookingService.getBookingsByPackage(packageId).size());
        }

        @Test
        void getBookingsByPackage_throwsPackageNotFound_whenPackageMissing() {
            when(packageRepo.existsById(packageId)).thenReturn(false);

            assertThrows(PackageNotFoundException.class,
                    () -> bookingService.getBookingsByPackage(packageId));

            verify(bookingRepo, never()).findByDeparture_TravelPackage_Id(any());
        }

        @Test
        void getBookingById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(bookingRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(BookingNotFoundException.class, () -> bookingService.getBookingById(missingId));
        }
    }
}
