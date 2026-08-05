package com.travelplatform.packages.entity;

import com.travelplatform.packages.enums.BookingStatus;
import com.travelplatform.packages.enums.PaymentStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "package_booking")
public class PackageBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "departure_id", referencedColumnName = "id", nullable = false)
    private PackageDeparture departure;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private UserRef traveler;

    @Column(nullable = false)
    private Integer travelersCount;

    @Column(nullable = false)
    private Double totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    /**
     * Separate from `status` — no payment gateway is integrated anywhere in
     * this platform yet, so this starts PENDING at booking time (honest: no
     * money has actually moved) rather than defaulting to PAID. Exists ahead
     * of real payment integration so the field/API shape doesn't need to
     * change later.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus;

    @Column(nullable = false)
    private LocalDateTime bookingTime;

    /** Optional — set when either the traveler or an admin cancels, explaining why. */
    @Column(length = 500)
    private String cancellationReason;

    /** Real names/ages for the group — a partner needs these to arrange hotels/transport, not just a headcount. */
    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PackageTraveler> travelers = new ArrayList<>();

    public PackageBooking() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public PackageDeparture getDeparture() { return departure; }
    public void setDeparture(PackageDeparture departure) { this.departure = departure; }

    /** Convenience passthrough — keeps existing call sites (notifications, response mapping) working unchanged. */
    public TravelPackage getTravelPackage() { return departure != null ? departure.getTravelPackage() : null; }

    public UserRef getTraveler() { return traveler; }
    public void setTraveler(UserRef traveler) { this.traveler = traveler; }

    public Integer getTravelersCount() { return travelersCount; }
    public void setTravelersCount(Integer travelersCount) { this.travelersCount = travelersCount; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }

    public List<PackageTraveler> getTravelers() { return travelers; }
    public void setTravelers(List<PackageTraveler> travelers) { this.travelers = travelers; }
}
