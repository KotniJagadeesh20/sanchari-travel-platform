package com.travelplatform.rideshare.repository;

import com.travelplatform.rideshare.entity.Ride;
import com.travelplatform.rideshare.entity.UserRef;
import com.travelplatform.rideshare.enums.RideStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real JPQL query (not a mock), since the whole point of this
 * fix is query-level case/whitespace handling that a mocked repository can't
 * verify. Backed by an in-memory H2 instance (see pom.xml's test-scoped h2
 * dependency) — no external DB needed.
 */
@DataJpaTest
class RideRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private RideRepository rideRepository;

    private Ride persistScheduledRide(String source, String destination, LocalDate date) {
        UserRef driver = new UserRef();
        driver.setId(UUID.randomUUID());
        driver.setEmail("driver@example.com");
        entityManager.persist(driver);

        Ride ride = new Ride();
        ride.setSource(source);
        ride.setDestination(destination);
        ride.setTravelDate(date);
        ride.setDepartureTime(LocalTime.of(8, 0));
        ride.setTotalSeats(4);
        ride.setAvailableSeats(4);
        ride.setPricePerSeat(500.0);
        ride.setStatus(RideStatus.SCHEDULED);
        ride.setDriver(driver);
        return entityManager.persistAndFlush(ride);
    }

    @Test
    void findBySourceAndDestination_matchesRegardlessOfCase() {
        LocalDate date = LocalDate.now().plusDays(1);
        persistScheduledRide("Hyderabad", "Vijayawada", date);

        List<Ride> results = rideRepository.findBySourceAndDestinationAndTravelDateAndStatus(
                "hyderabad", "VIJAYAWADA", date, RideStatus.SCHEDULED);

        assertThat(results).hasSize(1);
    }

    @Test
    void findBySourceAndDestination_matchesDespiteSurroundingWhitespace() {
        LocalDate date = LocalDate.now().plusDays(1);
        persistScheduledRide("Hyderabad", "Vijayawada", date);

        List<Ride> results = rideRepository.findBySourceAndDestinationAndTravelDateAndStatus(
                "  Hyderabad  ", " Vijayawada", date, RideStatus.SCHEDULED);

        assertThat(results).hasSize(1);
    }

    @Test
    void findBySourceAndDestination_stillExcludesNonScheduledStatus() {
        LocalDate date = LocalDate.now().plusDays(1);
        Ride ride = persistScheduledRide("Hyderabad", "Vijayawada", date);
        ride.setStatus(RideStatus.CANCELLED);
        entityManager.persistAndFlush(ride);

        List<Ride> results = rideRepository.findBySourceAndDestinationAndTravelDateAndStatus(
                "Hyderabad", "Vijayawada", date, RideStatus.SCHEDULED);

        assertThat(results).isEmpty();
    }

    @Test
    void findBySourceAndDestination_doesNotMatchDifferentCities() {
        LocalDate date = LocalDate.now().plusDays(1);
        persistScheduledRide("Hyderabad", "Vijayawada", date);

        List<Ride> results = rideRepository.findBySourceAndDestinationAndTravelDateAndStatus(
                "Hyderabad", "Bangalore", date, RideStatus.SCHEDULED);

        assertThat(results).isEmpty();
    }
}
