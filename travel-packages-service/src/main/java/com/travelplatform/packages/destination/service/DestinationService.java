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

    List<Destination> getByCategory(DestinationCategory category);

    /** Free-text search across destination names, listed only. */
    List<Destination> searchByKeyword(String keyword);

    /** Listed destinations with averageBudget <= the given ceiling. */
    List<Destination> searchByBudget(Double maxBudget);

    /** Listed destinations ordered by rating descending. */
    List<Destination> getPopularDestinations();

    Destination getDestinationById(UUID destinationId);
}
