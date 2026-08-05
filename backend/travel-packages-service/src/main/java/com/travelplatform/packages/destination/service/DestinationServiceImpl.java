package com.travelplatform.packages.destination.service;

import com.travelplatform.packages.destination.dto.ActivityRequest;
import com.travelplatform.packages.destination.dto.AttractionRequest;
import com.travelplatform.packages.destination.dto.CreateDestinationRequest;
import com.travelplatform.packages.destination.dto.UpdateDestinationRequest;
import com.travelplatform.packages.destination.entity.Activity;
import com.travelplatform.packages.destination.entity.Attraction;
import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import com.travelplatform.packages.destination.exception.DestinationNotFoundException;
import com.travelplatform.packages.destination.repository.DestinationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DestinationServiceImpl implements DestinationService {

    @Autowired
    private DestinationRepository destinationRepo;

    @Override
    @Transactional
    public Destination createDestination(CreateDestinationRequest request) {
        Destination destination = new Destination();
        destination.setName(request.getName());
        destination.setState(request.getState());
        destination.setCountry(request.getCountry());
        destination.setDescription(request.getDescription());
        destination.setBestTimeToVisit(request.getBestTimeToVisit());
        destination.setAverageBudget(request.getAverageBudget());
        destination.setRecommendedDays(request.getRecommendedDays());
        destination.setCategory(request.getCategory());
        destination.setActive(true);
        destination.setManualRating(request.getManualRating() != null ? request.getManualRating() : 0.0);
        destination.setImageUrls(orEmpty(request.getImageUrls()));

        destination.setAttractions(toAttractionEntities(request.getAttractions(), destination));
        destination.setActivities(toActivityEntities(request.getActivities(), destination));

        return destinationRepo.save(destination);
    }

    @Override
    @Transactional
    public Destination updateDestination(UUID destinationId, UpdateDestinationRequest request) {
        Destination destination = getDestinationById(destinationId);

        if (request.getName() != null) destination.setName(request.getName());
        if (request.getState() != null) destination.setState(request.getState());
        if (request.getCountry() != null) destination.setCountry(request.getCountry());
        if (request.getDescription() != null) destination.setDescription(request.getDescription());
        if (request.getBestTimeToVisit() != null) destination.setBestTimeToVisit(request.getBestTimeToVisit());
        if (request.getAverageBudget() != null) destination.setAverageBudget(request.getAverageBudget());
        if (request.getRecommendedDays() != null) destination.setRecommendedDays(request.getRecommendedDays());
        if (request.getCategory() != null) destination.setCategory(request.getCategory());
        if (request.getActive() != null) destination.setActive(request.getActive());
        if (request.getManualRating() != null) destination.setManualRating(request.getManualRating());
        if (request.getImageUrls() != null) destination.setImageUrls(request.getImageUrls());

        if (request.getAttractions() != null) {
            destination.getAttractions().clear();
            destination.getAttractions().addAll(toAttractionEntities(request.getAttractions(), destination));
        }
        if (request.getActivities() != null) {
            destination.getActivities().clear();
            destination.getActivities().addAll(toActivityEntities(request.getActivities(), destination));
        }

        return destinationRepo.save(destination);
    }

    @Override
    @Transactional
    public void deleteDestination(UUID destinationId) {
        Destination destination = getDestinationById(destinationId);
        destination.setActive(false); // soft delist — same pattern as TravelPackage.deletePackage()
        destinationRepo.save(destination);
    }

    @Override
    public List<Destination> getAllActiveDestinations() {
        return destinationRepo.findByActiveTrue();
    }

    @Override
    public List<Destination> getByCategory(DestinationCategory category) {
        return destinationRepo.findByCategoryAndActiveTrue(category);
    }

    @Override
    public List<Destination> searchByKeyword(String keyword) {
        return destinationRepo.findByNameContainingIgnoreCaseAndActiveTrue(keyword);
    }

    @Override
    public List<Destination> searchByBudget(Double maxBudget) {
        return destinationRepo.findByAverageBudgetLessThanEqualAndActiveTrue(maxBudget);
    }

    @Override
    public List<Destination> getPopularDestinations() {
        return destinationRepo.findByActiveTrueOrderByManualRatingDesc();
    }

    @Override
    public Destination getDestinationById(UUID destinationId) {
        return destinationRepo.findById(destinationId)
                .orElseThrow(() -> new DestinationNotFoundException(destinationId));
    }

    private List<String> orEmpty(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }

    private List<Attraction> toAttractionEntities(List<AttractionRequest> requests, Destination destination) {
        if (requests == null) return new ArrayList<>();
        return requests.stream().map(r -> {
            Attraction a = new Attraction();
            a.setName(r.getName());
            a.setDescription(r.getDescription());
            a.setAttractionType(r.getAttractionType());
            a.setDestination(destination);
            return a;
        }).collect(Collectors.toList());
    }

    private List<Activity> toActivityEntities(List<ActivityRequest> requests, Destination destination) {
        if (requests == null) return new ArrayList<>();
        return requests.stream().map(r -> {
            Activity a = new Activity();
            a.setName(r.getName());
            a.setCategory(r.getCategory());
            a.setDestination(destination);
            return a;
        }).collect(Collectors.toList());
    }
}
