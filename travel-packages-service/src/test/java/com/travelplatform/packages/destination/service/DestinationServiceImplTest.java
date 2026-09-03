package com.travelplatform.packages.destination.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.travelplatform.packages.destination.dto.ActivityRequest;
import com.travelplatform.packages.destination.dto.AttractionRequest;
import com.travelplatform.packages.destination.dto.CreateDestinationRequest;
import com.travelplatform.packages.destination.dto.UpdateDestinationRequest;
import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import com.travelplatform.packages.destination.exception.DestinationNotFoundException;
import com.travelplatform.packages.destination.repository.DestinationRepository;

@ExtendWith(MockitoExtension.class)
class DestinationServiceImplTest {

    @Mock private DestinationRepository destinationRepo;
    @InjectMocks private DestinationServiceImpl destinationService;

    private CreateDestinationRequest createRequest;

    @BeforeEach
    void setUp() {
        createRequest = new CreateDestinationRequest();
        createRequest.setName("Goa");
        createRequest.setState("Goa");
        createRequest.setCountry("India");
        createRequest.setCategory(DestinationCategory.BEACH);
        createRequest.setAverageBudget(12000.0);
        createRequest.setRecommendedDays(4);
        createRequest.setBestMonths(List.of(11, 12, 1, 2));

        AttractionRequest attraction = new AttractionRequest();
        attraction.setName("Baga Beach");
        attraction.setAttractionType("Beach");
        createRequest.setAttractions(List.of(attraction));

        ActivityRequest activity = new ActivityRequest();
        activity.setName("Scuba Diving");
        activity.setCategory("Water Sports");
        createRequest.setActivities(List.of(activity));
    }

    @Nested
    class CreateDestination {

        @Test
        void createsDestination_asActive_withZeroInitialRating() {
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            Destination destination = destinationService.createDestination(createRequest);

            assertTrue(destination.getActive());
            assertEquals(0.0, destination.getManualRating());
            assertEquals(DestinationCategory.BEACH, destination.getCategory());
            assertEquals(Set.of(11, 12, 1, 2), destination.getBestMonths());
        }

        @Test
        void mapsAttractionsAndActivities_withBackReferenceToDestination() {
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            Destination destination = destinationService.createDestination(createRequest);

            assertEquals(1, destination.getAttractions().size());
            assertEquals("Baga Beach", destination.getAttractions().get(0).getName());
            assertEquals(destination, destination.getAttractions().get(0).getDestination());

            assertEquals(1, destination.getActivities().size());
            assertEquals("Scuba Diving", destination.getActivities().get(0).getName());
            assertEquals(destination, destination.getActivities().get(0).getDestination());
        }

        @Test
        void defaultsListsToEmpty_whenNotProvided() {
            CreateDestinationRequest minimal = new CreateDestinationRequest();
            minimal.setName("Ooty");
            minimal.setCountry("India");
            minimal.setCategory(DestinationCategory.HILL_STATION);

            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            Destination destination = destinationService.createDestination(minimal);

            assertNotNull(destination.getImageUrls());
            assertTrue(destination.getImageUrls().isEmpty());
            assertTrue(destination.getAttractions().isEmpty());
            assertTrue(destination.getActivities().isEmpty());
        }
    }

    @Nested
    class UpdateDestination {

        private Destination existing;
        private UUID destinationId;

        @BeforeEach
        void setUp() {
            destinationId = UUID.randomUUID();
            existing = new Destination();
            existing.setId(destinationId);
            existing.setName("Goa");
            existing.setCategory(DestinationCategory.BEACH);
            existing.setAverageBudget(12000.0);
        }

        @Test
        void appliesOnlyNonNullFields() {
            when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(existing));
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            UpdateDestinationRequest req = new UpdateDestinationRequest();
            req.setAverageBudget(15000.0);

            Destination updated = destinationService.updateDestination(destinationId, req);

            assertEquals(15000.0, updated.getAverageBudget());
            assertEquals("Goa", updated.getName(), "Name untouched when not provided");
        }

        @Test
        void replacesAttractions_whenProvided() {
            existing.getAttractions().add(new com.travelplatform.packages.destination.entity.Attraction());
            when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(existing));
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            AttractionRequest newAttraction = new AttractionRequest();
            newAttraction.setName("Calangute Beach");
            UpdateDestinationRequest req = new UpdateDestinationRequest();
            req.setAttractions(List.of(newAttraction));

            Destination updated = destinationService.updateDestination(destinationId, req);

            assertEquals(1, updated.getAttractions().size());
            assertEquals("Calangute Beach", updated.getAttractions().get(0).getName());
        }

        @Test
        void updatesManualRating_whenAdminSetsIt() {
            when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(existing));
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            UpdateDestinationRequest req = new UpdateDestinationRequest();
            req.setManualRating(4.7);

            Destination updated = destinationService.updateDestination(destinationId, req);

            assertEquals(4.7, updated.getManualRating(),
                    "manualRating is admin-set and must be updatable via UpdateDestinationRequest");
        }

        @Test
        void throwsDestinationNotFound_whenDestinationDoesNotExist() {
            when(destinationRepo.findById(destinationId)).thenReturn(Optional.empty());

            assertThrows(DestinationNotFoundException.class,
                    () -> destinationService.updateDestination(destinationId, new UpdateDestinationRequest()));
        }
    }

    @Nested
    class DeleteDestination {

        @Test
        void setsActiveFalse_ratherThanDeletingRow() {
            UUID destinationId = UUID.randomUUID();
            Destination existing = new Destination();
            existing.setId(destinationId);
            existing.setActive(true);

            when(destinationRepo.findById(destinationId)).thenReturn(Optional.of(existing));
            when(destinationRepo.save(any(Destination.class))).thenAnswer(i -> i.getArgument(0));

            destinationService.deleteDestination(destinationId);

            assertFalse(existing.getActive());
            verify(destinationRepo, never()).delete(any());
            verify(destinationRepo, never()).deleteById(any());
        }
    }

    @Nested
    class Reads {

        @Test
        void getAllActiveDestinations_delegatesToRepository() {
            when(destinationRepo.findByActiveTrue()).thenReturn(List.of(new Destination()));
            assertEquals(1, destinationService.getAllActiveDestinations().size());
        }

        @Test
        void getAllDestinationsForAdmin_returnsEveryDestinationRegardlessOfActiveFlag() {
            when(destinationRepo.findAll()).thenReturn(List.of(new Destination(), new Destination()));
            assertEquals(2, destinationService.getAllDestinationsForAdmin().size());
        }

        @Test
        void search_delegatesAllFiltersTogetherToTheRepositoryQuery() {
            when(destinationRepo.search("goa", DestinationCategory.BEACH, 15000.0, 11))
                    .thenReturn(List.of(new Destination()));

            List<Destination> results = destinationService.search("goa", DestinationCategory.BEACH, 15000.0, 11);

            assertEquals(1, results.size());
            verify(destinationRepo).search("goa", DestinationCategory.BEACH, 15000.0, 11);
        }

        @Test
        void search_treatsBlankKeywordAsNoKeywordFilter() {
            when(destinationRepo.search(null, DestinationCategory.BEACH, null, null))
                    .thenReturn(List.of(new Destination()));

            destinationService.search("   ", DestinationCategory.BEACH, null, null);

            verify(destinationRepo).search(null, DestinationCategory.BEACH, null, null);
        }

        @Test
        void search_allowsEveryFilterToBeOmitted() {
            when(destinationRepo.search(null, null, null, null)).thenReturn(List.of(new Destination()));

            List<Destination> results = destinationService.search(null, null, null, null);

            assertEquals(1, results.size());
        }

        @Test
        void getPopularDestinations_delegatesToRepository() {
            when(destinationRepo.findByActiveTrueOrderByManualRatingDesc()).thenReturn(List.of(new Destination()));
            assertEquals(1, destinationService.getPopularDestinations().size());
        }

        @Test
        void getDestinationById_throwsWhenNotFound() {
            UUID missingId = UUID.randomUUID();
            when(destinationRepo.findById(missingId)).thenReturn(Optional.empty());

            assertThrows(DestinationNotFoundException.class, () -> destinationService.getDestinationById(missingId));
        }
    }
}
