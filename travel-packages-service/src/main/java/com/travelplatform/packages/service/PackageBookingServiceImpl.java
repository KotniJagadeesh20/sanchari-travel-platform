package com.travelplatform.packages.service;

import com.travelplatform.packages.client.NotificationClient;
import com.travelplatform.packages.dto.TravelerRequest;
import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.PackageTraveler;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.enums.PaymentStatus;
import com.travelplatform.packages.exception.*;
import com.travelplatform.packages.repository.PackageBookingRepository;
import com.travelplatform.packages.repository.PackageDepartureRepository;
import com.travelplatform.packages.repository.TravelPackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PackageBookingServiceImpl implements PackageBookingService {

    @Autowired
    private PackageBookingRepository bookingRepo;

    @Autowired
    private PackageDepartureRepository departureRepo;

    @Autowired
    private TravelPackageRepository packageRepo;

    @Autowired
    private NotificationClient notificationClient;

    @Override
    @Transactional
    public PackageBooking bookPackage(UUID departureId, List<TravelerRequest> travelers, UserRef traveler) {
        PackageDeparture departure = departureRepo.findById(departureId)
                .orElseThrow(() -> new PackageDepartureNotFoundException(departureId));
        TravelPackage pkg = departure.getTravelPackage();

        // Rule: delisted packages, or a departure batch that's been individually cancelled, cannot be booked.
        if (!Boolean.TRUE.equals(pkg.getActive()) || !Boolean.TRUE.equals(departure.getActive())) {
            throw new PackageNotBookableException(pkg.getId());
        }

        int travelersCount = travelers.size();

        // Rule: requested travelers must not exceed this departure's own availability.
        if (travelersCount > departure.getAvailableSlots()) {
            throw new InsufficientSlotsException(travelersCount, departure.getAvailableSlots());
        }

        // Auto-confirm: unlike ride-share's approval flow, package bookings
        // confirm immediately, so slots are deducted right away.
        departure.setAvailableSlots(departure.getAvailableSlots() - travelersCount);
        departureRepo.save(departure);

        PackageBooking booking = new PackageBooking();
        booking.setDeparture(departure);
        booking.setTraveler(traveler);
        booking.setTravelersCount(travelersCount);
        booking.setTotalAmount(travelersCount * pkg.getPrice());
        booking.setStatus(BookingStatus.CONFIRMED);
        // No payment gateway integrated anywhere in this platform yet — PENDING is
        // honest (no money has actually moved), not a placeholder for PAID.
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());
        booking.setTravelers(travelers.stream()
                .map(t -> new PackageTraveler(t.getName(), t.getAge()))
                .peek(pt -> pt.setBooking(booking))
                .collect(Collectors.toList()));

        PackageBooking saved = bookingRepo.save(booking);

        notificationClient.notify(
                traveler.getId(), traveler.getEmail(), "BOOKING_CONFIRMED",
                "Package booking confirmed",
                String.format("Your booking (%s) for %s departing %s (%d traveler(s)) is confirmed.",
                        saved.getId(), pkg.getTitle(), departure.getStartDate(), travelersCount),
                saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public void cancelBooking(UUID bookingId, UUID callerId, String reason) {
        PackageBooking booking = getBookingById(bookingId);

        if (!booking.getTraveler().getId().equals(callerId)) {
            throw new UnauthorizedBookingActionException(
                    "Only the traveler who made this booking can cancel it.");
        }

        doCancel(booking, reason);

        UserRef traveler = booking.getTraveler();
        notificationClient.notify(
                traveler.getId(), traveler.getEmail(), "BOOKING_CANCELLED",
                "Package booking cancelled",
                String.format("Your booking (%s) for %s has been cancelled.",
                        booking.getId(), booking.getTravelPackage().getTitle()),
                booking.getId());
    }

    @Override
    @Transactional
    public void cancelBookingAsAdmin(UUID bookingId, String reason) {
        PackageBooking booking = getBookingById(bookingId);
        doCancel(booking, reason);

        UserRef traveler = booking.getTraveler();
        notificationClient.notify(
                traveler.getId(), traveler.getEmail(), "BOOKING_CANCELLED_BY_OPERATOR",
                "Your package booking was cancelled by the operator",
                String.format("Your booking (%s) for %s has been cancelled by the operator.%s",
                        booking.getId(), booking.getTravelPackage().getTitle(),
                        reason != null ? " Reason: " + reason : ""),
                booking.getId());
    }

    private void doCancel(PackageBooking booking, String reason) {
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            PackageDeparture departure = booking.getDeparture();
            departure.setAvailableSlots(departure.getAvailableSlots() + booking.getTravelersCount());
            departureRepo.save(departure);
        }

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancellationReason(reason);
        bookingRepo.save(booking);
    }

    @Override
    public List<PackageBooking> getBookingsByTraveler(UUID travelerId) {
        return bookingRepo.findByTravelerId(travelerId);
    }

    @Override
    public List<PackageBooking> getBookingsByPackage(UUID packageId) {
        // Existence check so a bad packageId returns 404 rather than an empty list silently.
        if (!packageRepo.existsById(packageId)) {
            throw new PackageNotFoundException(packageId);
        }
        return bookingRepo.findByDeparture_TravelPackage_Id(packageId);
    }

    @Override
    public PackageBooking getBookingById(UUID bookingId) {
        return bookingRepo.findById(bookingId).orElseThrow(() -> new BookingNotFoundException(bookingId));
    }
}
