package com.travelplatform.packages.dto;

import com.travelplatform.packages.entity.PackageBooking;
import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.enums.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Schema(description = "Booking details returned to clients")
public class PackageBookingResponse {

    private UUID id;
    private UUID packageId;
    private String packageTitle;
    private UUID departureId;
    private LocalDate departureStartDate;
    private UUID travelerId;
    private String travelerName;
    private String travelerEmail;
    private Integer travelersCount;
    private List<TravelerResponse> travelers;
    private Double totalAmount;
    private BookingStatus status;
    private PaymentStatus paymentStatus;
    private String cancellationReason;
    private LocalDateTime bookingTime;

    public static PackageBookingResponse from(PackageBooking booking) {
        PackageBookingResponse r = new PackageBookingResponse();
        r.id = booking.getId();
        r.packageId = booking.getTravelPackage().getId();
        r.packageTitle = booking.getTravelPackage().getTitle();
        r.departureId = booking.getDeparture().getId();
        r.departureStartDate = booking.getDeparture().getStartDate();
        r.travelerId = booking.getTraveler().getId();
        r.travelerName = booking.getTraveler().getName();
        r.travelerEmail = booking.getTraveler().getEmail();
        r.travelersCount = booking.getTravelersCount();
        r.travelers = booking.getTravelers().stream().map(TravelerResponse::from).collect(Collectors.toList());
        r.totalAmount = booking.getTotalAmount();
        r.status = booking.getStatus();
        r.paymentStatus = booking.getPaymentStatus();
        r.cancellationReason = booking.getCancellationReason();
        r.bookingTime = booking.getBookingTime();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getPackageId() { return packageId; }
    public String getPackageTitle() { return packageTitle; }
    public UUID getDepartureId() { return departureId; }
    public LocalDate getDepartureStartDate() { return departureStartDate; }
    public UUID getTravelerId() { return travelerId; }
    public String getTravelerName() { return travelerName; }
    public String getTravelerEmail() { return travelerEmail; }
    public Integer getTravelersCount() { return travelersCount; }
    public List<TravelerResponse> getTravelers() { return travelers; }
    public Double getTotalAmount() { return totalAmount; }
    public BookingStatus getStatus() { return status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public String getCancellationReason() { return cancellationReason; }
    public LocalDateTime getBookingTime() { return bookingTime; }
}
