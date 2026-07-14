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

import com.travelplatform.packages.dto.CreatePackageRequest;
import com.travelplatform.packages.dto.DepartureRequest;
import com.travelplatform.packages.dto.ItineraryDayRequest;
import com.travelplatform.packages.dto.UpdatePackageRequest;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.exception.PackageDepartureNotFoundException;
import com.travelplatform.packages.exception.PackageNotFoundException;
import com.travelplatform.packages.repository.PackageDepartureRepository;
import com.travelplatform.packages.repository.TravelPackageRepository;

@ExtendWith(MockitoExtension.class)
class PackageServiceImplTest {

    @Mock private TravelPackageRepository packageRepo;
    @Mock private PackageDepartureRepository departureRepo;
    @InjectMocks private PackageServiceImpl packageService;

    private CreatePackageRequest createRequest;
    private UserRef creator;

    @BeforeEach
    void setUp() {
        creator = new UserRef(UUID.randomUUID(), "creator@example.com", "Creator Name");

        createRequest = new CreatePackageRequest();
        createRequest.setTitle("Goa Beach Getaway");
        createRequest.setDestinationId(UUID.randomUUID());
        createRequest.setDurationDays(4);
        createRequest.setDurationNights(3);
        createRequest.setPrice(15999.0);
        createRequest.setMaxPeople(20);
        createRequest.setInclusions(List.of("Hotel Stay", "Breakfast"));
        createRequest.setExclusions(List.of("Flights"));

        ItineraryDayRequest day1 = new ItineraryDayRequest();
        day1.setDayNumber(1);
        day1.setPlan("Arrival and check-in");
        createRequest.setItinerary(List.of(day1));
    }

    @Nested
    class CreatePackage {

        @Test
        void createsPackage_asDraft_untilExplicitlyPublished() {
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            TravelPackage pkg = packageService.createPackage(createRequest, creator);

            assertFalse(pkg.getActive(), "new packages start as drafts — must be explicitly published via updatePackage(active=true)");
            assertEquals(List.of("Hotel Stay", "Breakfast"), pkg.getInclusions());
            assertEquals(creator, pkg.getCreatedBy());
        }

        @Test
        void mapsItineraryDays_withBackReferenceToPackage() {
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            TravelPackage pkg = packageService.createPackage(createRequest, creator);

            assertEquals(1, pkg.getItinerary().size());
            assertEquals(1, pkg.getItinerary().get(0).getDayNumber());
            assertEquals(pkg, pkg.getItinerary().get(0).getTravelPackage());
        }

        @Test
        void defaultsListsToEmpty_whenNotProvided() {
            CreatePackageRequest minimal = new CreatePackageRequest();
            minimal.setTitle("Minimal Trip");
            minimal.setDestinationId(UUID.randomUUID());
            minimal.setDurationDays(2);
            minimal.setDurationNights(1);
            minimal.setPrice(5000.0);
            minimal.setMaxPeople(10);

            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            TravelPackage pkg = packageService.createPackage(minimal, creator);

            assertNotNull(pkg.getInclusions());
            assertTrue(pkg.getInclusions().isEmpty());
            assertNotNull(pkg.getItinerary());
            assertTrue(pkg.getItinerary().isEmpty());
        }

        @Test
        void createsInitialDepartures_whenProvided() {
            DepartureRequest dep = new DepartureRequest();
            dep.setStartDate(LocalDate.now().plusMonths(1));
            dep.setMaxPeople(15);
            createRequest.setDepartures(List.of(dep));

            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            TravelPackage pkg = packageService.createPackage(createRequest, creator);

            assertEquals(1, pkg.getDepartures().size());
            PackageDeparture created = pkg.getDepartures().get(0);
            assertEquals(15, created.getMaxPeople());
            assertEquals(15, created.getAvailableSlots(), "fully available on creation");
            assertTrue(created.getActive());
            assertEquals(pkg, created.getTravelPackage());
        }

        @Test
        void departureDefaultsMaxPeople_toPackagesMaxPeople_whenOmitted() {
            DepartureRequest dep = new DepartureRequest();
            dep.setStartDate(LocalDate.now().plusMonths(1));
            // maxPeople intentionally left null
            createRequest.setDepartures(List.of(dep));

            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            TravelPackage pkg = packageService.createPackage(createRequest, creator);

            assertEquals(20, pkg.getDepartures().get(0).getMaxPeople(), "falls back to the package's own maxPeople (20)");
        }
    }

    @Nested
    class UpdatePackage {

        private TravelPackage existing;
        private UUID packageId;

        @BeforeEach
        void setUp() {
            packageId = UUID.randomUUID();
            existing = new TravelPackage();
            existing.setId(packageId);
            existing.setMaxPeople(20);
            existing.setPrice(15999.0);
        }

        @Test
        void appliesOnlyNonNullFields() {
            when(packageRepo.findById(packageId)).thenReturn(Optional.of(existing));
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePackageRequest req = new UpdatePackageRequest();
            req.setPrice(17999.0);

            TravelPackage updated = packageService.updatePackage(packageId, req);

            assertEquals(17999.0, updated.getPrice());
            assertEquals(20, updated.getMaxPeople(), "maxPeople untouched when not provided");
        }

        @Test
        void maxPeopleUpdate_justSetsTheValue_noLongerShiftsAnyAvailableSlots() {
            when(packageRepo.findById(packageId)).thenReturn(Optional.of(existing));
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePackageRequest req = new UpdatePackageRequest();
            req.setMaxPeople(25);

            TravelPackage updated = packageService.updatePackage(packageId, req);

            assertEquals(25, updated.getMaxPeople(), "availability now lives per-departure — this is just the template default for new batches");
        }

        @Test
        void publishesPackage_whenActiveSetTrue() {
            existing.setActive(false);
            when(packageRepo.findById(packageId)).thenReturn(Optional.of(existing));
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            UpdatePackageRequest req = new UpdatePackageRequest();
            req.setActive(true);

            TravelPackage updated = packageService.updatePackage(packageId, req);

            assertTrue(updated.getActive());
        }

        @Test
        void throwsPackageNotFound_whenPackageDoesNotExist() {
            when(packageRepo.findById(packageId)).thenReturn(Optional.empty());

            assertThrows(PackageNotFoundException.class,
                    () -> packageService.updatePackage(packageId, new UpdatePackageRequest()));
        }
    }

    @Nested
    class DeletePackage {

        @Test
        void setsActiveFalse_ratherThanDeletingRow() {
            UUID packageId = UUID.randomUUID();
            TravelPackage existing = new TravelPackage();
            existing.setId(packageId);
            existing.setActive(true);

            when(packageRepo.findById(packageId)).thenReturn(Optional.of(existing));
            when(packageRepo.save(any(TravelPackage.class))).thenAnswer(i -> i.getArgument(0));

            packageService.deletePackage(packageId);

            assertFalse(existing.getActive());
            verify(packageRepo, never()).delete(any());
            verify(packageRepo, never()).deleteById(any());
        }
    }

    @Nested
    class Departures {

        private TravelPackage pkg;
        private UUID packageId;

        @BeforeEach
        void setUp() {
            packageId = UUID.randomUUID();
            pkg = new TravelPackage();
            pkg.setId(packageId);
            pkg.setMaxPeople(20);
        }

        @Test
        void addDeparture_defaultsMaxPeople_whenOmitted() {
            when(packageRepo.findById(packageId)).thenReturn(Optional.of(pkg));
            when(departureRepo.save(any(PackageDeparture.class))).thenAnswer(i -> i.getArgument(0));

            DepartureRequest req = new DepartureRequest();
            req.setStartDate(LocalDate.now().plusMonths(2));

            PackageDeparture created = packageService.addDeparture(packageId, req);

            assertEquals(20, created.getMaxPeople());
            assertEquals(20, created.getAvailableSlots());
            assertTrue(created.getActive());
        }

        @Test
        void updateDeparture_shiftsAvailableSlots_byMaxPeopleDelta() {
            UUID departureId = UUID.randomUUID();
            PackageDeparture existing = new PackageDeparture();
            existing.setId(departureId);
            existing.setTravelPackage(pkg);
            existing.setStartDate(LocalDate.now().plusMonths(1));
            existing.setMaxPeople(20);
            existing.setAvailableSlots(15); // 5 already booked

            when(departureRepo.findById(departureId)).thenReturn(Optional.of(existing));
            when(departureRepo.save(any(PackageDeparture.class))).thenAnswer(i -> i.getArgument(0));

            DepartureRequest req = new DepartureRequest();
            req.setMaxPeople(25); // +5 capacity

            PackageDeparture updated = packageService.updateDeparture(departureId, req);

            assertEquals(25, updated.getMaxPeople());
            assertEquals(20, updated.getAvailableSlots(), "5 already-booked stay booked; +5 new capacity becomes available");
        }

        @Test
        void cancelDeparture_setsActiveFalse() {
            UUID departureId = UUID.randomUUID();
            PackageDeparture existing = new PackageDeparture();
            existing.setId(departureId);
            existing.setActive(true);

            when(departureRepo.findById(departureId)).thenReturn(Optional.of(existing));
            when(departureRepo.save(any(PackageDeparture.class))).thenAnswer(i -> i.getArgument(0));

            packageService.cancelDeparture(departureId);

            assertFalse(existing.getActive());
        }

        @Test
        void getDepartureById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(departureRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(PackageDepartureNotFoundException.class, () -> packageService.getDepartureById(missingId));
        }
    }

    @Nested
    class Reads {

        @Test
        void getAllActivePackages_delegatesToRepository() {
            when(packageRepo.findByActiveTrue()).thenReturn(List.of(new TravelPackage()));
            assertEquals(1, packageService.getAllActivePackages().size());
        }

        @Test
        void findByDestination_delegatesToRepository() {
            UUID destId = UUID.randomUUID();
            when(packageRepo.findByDestinationIdAndActiveTrue(destId))
                    .thenReturn(List.of(new TravelPackage()));
            assertEquals(1, packageService.findByDestination(destId).size());
        }

        @Test
        void getPackageById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(packageRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(PackageNotFoundException.class, () -> packageService.getPackageById(missingId));
        }
    }
}
