package com.travelplatform.packages.destination.service;

import com.travelplatform.packages.destination.dto.CreateDestinationRequest;
import com.travelplatform.packages.destination.dto.UpdateDestinationRequest;
import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;

import java.util.List;
import java.util.UUID;

public interface DestinationService {

    /** Admin creates a new destination, including its attractions and activities. */
    Destination createDestination(CreateDestinationRequest request);

    /** Admin updates a destination. Only non-null fields applied; attractions/activities are fully replaced if provided. */
    Destination updateDestination(UUID destinationId, UpdateDestinationRequest request);

    /** Admin soft-delists a destination — sets active=false, preserves FK integrity for linked packages. */
    void deleteDestination(UUID destinationId);

    /** Public browse — only listed destinations. */
    List<Destination> getAllActiveDestinations();

    /** Admin view — every destination including delisted ones. Without this, a delisted destination can never be found again to re-activate it. */
    List<Destination> getAllDestinationsForAdmin();

    /**
     * Combined search — keyword/category/maxBudget/visitMonth are all
     * optional and, when more than one is given, applied together (AND), not
     * just whichever comes first. Pass null for any filter you don't want
     * applied. visitMonth is 1=January..12=December.
     */
    List<Destination> search(String keyword, DestinationCategory category, Double maxBudget, Integer visitMonth);

    /** Listed destinations ordered by rating descending. */
    List<Destination> getPopularDestinations();

    Destination getDestinationById(UUID destinationId);
}
