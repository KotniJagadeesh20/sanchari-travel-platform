package com.travelplatform.packages.service;

import com.travelplatform.packages.dto.CreatePackageRequest;
import com.travelplatform.packages.dto.DepartureRequest;
import com.travelplatform.packages.dto.ItineraryDayRequest;
import com.travelplatform.packages.dto.UpdatePackageRequest;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.PackageItinerary;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;
import com.travelplatform.packages.exception.PackageDepartureNotFoundException;
import com.travelplatform.packages.exception.PackageNotFoundException;
import com.travelplatform.packages.repository.PackageDepartureRepository;
import com.travelplatform.packages.repository.TravelPackageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PackageServiceImpl implements PackageService {

    @Autowired
    private TravelPackageRepository packageRepo;

    @Autowired
    private PackageDepartureRepository departureRepo;

    @Override
    @Transactional
    public TravelPackage createPackage(CreatePackageRequest request, UserRef creator) {
        TravelPackage pkg = new TravelPackage();
        pkg.setTitle(request.getTitle());
        pkg.setDescription(request.getDescription());
        pkg.setDestinationId(request.getDestinationId());
        pkg.setDurationDays(request.getDurationDays());
        pkg.setDurationNights(request.getDurationNights());
        pkg.setPrice(request.getPrice());
        pkg.setMaxPeople(request.getMaxPeople());
        pkg.setThumbnailImage(request.getThumbnailImage());
        pkg.setActive(false); // draft — partner must explicitly publish (updatePackage with active=true)
        pkg.setCreatedBy(creator);

        pkg.setInclusions(orEmpty(request.getInclusions()));
        pkg.setExclusions(orEmpty(request.getExclusions()));
        pkg.setPlacesCovered(orEmpty(request.getPlacesCovered()));
        pkg.setActivities(orEmpty(request.getActivities()));
        pkg.setImageUrls(orEmpty(request.getImageUrls()));

        pkg.setItinerary(toItineraryEntities(request.getItinerary(), pkg));

        TravelPackage saved = packageRepo.save(pkg);

        if (request.getDepartures() != null) {
            for (DepartureRequest d : request.getDepartures()) {
                saved.getDepartures().add(buildDeparture(d, saved));
            }
            saved = packageRepo.save(saved);
        }

        return saved;
    }

    @Override
    @Transactional
    public TravelPackage updatePackage(UUID packageId, UpdatePackageRequest request) {
        TravelPackage pkg = getPackageById(packageId);

        if (request.getTitle() != null) pkg.setTitle(request.getTitle());
        if (request.getDescription() != null) pkg.setDescription(request.getDescription());
        if (request.getDestinationId() != null) pkg.setDestinationId(request.getDestinationId());
        if (request.getDurationDays() != null) pkg.setDurationDays(request.getDurationDays());
        if (request.getDurationNights() != null) pkg.setDurationNights(request.getDurationNights());
        if (request.getPrice() != null) pkg.setPrice(request.getPrice());
        if (request.getMaxPeople() != null) pkg.setMaxPeople(request.getMaxPeople());
        if (request.getThumbnailImage() != null) pkg.setThumbnailImage(request.getThumbnailImage());
        if (request.getActive() != null) pkg.setActive(request.getActive());

        if (request.getInclusions() != null) pkg.setInclusions(request.getInclusions());
        if (request.getExclusions() != null) pkg.setExclusions(request.getExclusions());
        if (request.getPlacesCovered() != null) pkg.setPlacesCovered(request.getPlacesCovered());
        if (request.getActivities() != null) pkg.setActivities(request.getActivities());
        if (request.getImageUrls() != null) pkg.setImageUrls(request.getImageUrls());

        if (request.getItinerary() != null) {
            pkg.getItinerary().clear();
            pkg.getItinerary().addAll(toItineraryEntities(request.getItinerary(), pkg));
        }

        return packageRepo.save(pkg);
    }

    @Override
    @Transactional
    public void deletePackage(UUID packageId) {
        TravelPackage pkg = getPackageById(packageId);
        pkg.setActive(false); // soft delist — preserves booking history and FK integrity
        packageRepo.save(pkg);
    }

    @Override
    public List<TravelPackage> getAllActivePackages() {
        return packageRepo.findByActiveTrue();
    }

    @Override
    public List<TravelPackage> findByDestination(UUID destinationId) {
        return packageRepo.findByDestinationIdAndActiveTrue(destinationId);
    }

    @Override
    public List<TravelPackage> search(UUID destinationId, String keyword, Double maxBudget,
                                       Integer minDurationDays, Integer maxDurationDays) {
        return packageRepo.search(destinationId, blankToNull(keyword), maxBudget, minDurationDays, maxDurationDays);
    }

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Override
    public List<TravelPackage> getAllPackagesForAdmin() {
        return packageRepo.findAll();
    }

    @Override
    public List<TravelPackage> getPackagesByCreator(UUID creatorId) {
        return packageRepo.findByCreatedById(creatorId);
    }

    @Override
    public TravelPackage getPackageById(UUID packageId) {
        return packageRepo.findById(packageId).orElseThrow(() -> new PackageNotFoundException(packageId));
    }

    @Override
    @Transactional
    public PackageDeparture addDeparture(UUID packageId, DepartureRequest request) {
        TravelPackage pkg = getPackageById(packageId);
        PackageDeparture departure = buildDeparture(request, pkg);
        return departureRepo.save(departure);
    }

    @Override
    @Transactional
    public PackageDeparture updateDeparture(UUID departureId, DepartureRequest request) {
        PackageDeparture departure = getDepartureById(departureId);
        if (request.getStartDate() != null) departure.setStartDate(request.getStartDate());

        // maxPeople shifts availableSlots by the same delta, mirroring Ride.totalSeats update behavior —
        // already-booked travelers stay booked, only the net change in capacity affects availability.
        if (request.getMaxPeople() != null) {
            int delta = request.getMaxPeople() - departure.getMaxPeople();
            departure.setMaxPeople(request.getMaxPeople());
            departure.setAvailableSlots(Math.max(0, departure.getAvailableSlots() + delta));
        }

        return departureRepo.save(departure);
    }

    @Override
    @Transactional
    public void cancelDeparture(UUID departureId) {
        PackageDeparture departure = getDepartureById(departureId);
        departure.setActive(false);
        departureRepo.save(departure);
    }

    @Override
    public PackageDeparture getDepartureById(UUID departureId) {
        return departureRepo.findById(departureId).orElseThrow(() -> new PackageDepartureNotFoundException(departureId));
    }

    private PackageDeparture buildDeparture(DepartureRequest request, TravelPackage pkg) {
        PackageDeparture d = new PackageDeparture();
        d.setTravelPackage(pkg);
        d.setStartDate(request.getStartDate());
        int capacity = request.getMaxPeople() != null ? request.getMaxPeople() : pkg.getMaxPeople();
        d.setMaxPeople(capacity);
        d.setAvailableSlots(capacity);
        d.setActive(true);
        return d;
    }

    private List<String> orEmpty(List<String> list) {
        return list != null ? list : new ArrayList<>();
    }

    private List<PackageItinerary> toItineraryEntities(List<ItineraryDayRequest> days, TravelPackage pkg) {
        if (days == null) return new ArrayList<>();
        return days.stream().map(d -> {
            PackageItinerary day = new PackageItinerary();
            day.setDayNumber(d.getDayNumber());
            day.setPlan(d.getPlan());
            day.setTravelPackage(pkg);
            return day;
        }).collect(Collectors.toList());
    }
}
