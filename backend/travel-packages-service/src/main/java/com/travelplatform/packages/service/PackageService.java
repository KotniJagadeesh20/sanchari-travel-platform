package com.travelplatform.packages.service;

import com.travelplatform.packages.dto.CreatePackageRequest;
import com.travelplatform.packages.dto.DepartureRequest;
import com.travelplatform.packages.dto.UpdatePackageRequest;
import com.travelplatform.packages.entity.PackageDeparture;
import com.travelplatform.packages.entity.TravelPackage;
import com.travelplatform.packages.entity.UserRef;

import java.util.List;
import java.util.UUID;

public interface PackageService {

    /** Admin creates a new package as a draft (active=false) — must be explicitly published via updatePackage(active=true). */
    TravelPackage createPackage(CreatePackageRequest request, UserRef creator);

    /** Admin updates a package's own fields. Departure capacity/dates are managed separately — see addDeparture etc. */
    TravelPackage updatePackage(UUID packageId, UpdatePackageRequest request);

    /** Admin soft-deletes (delists) a package — sets active=false, keeps booking history. */
    void deletePackage(UUID packageId);

    /** Public browse — only active (published) packages. */
    List<TravelPackage> getAllActivePackages();

    /** Find all active packages linked to a specific destination UUID. */
    List<TravelPackage> findByDestination(UUID destinationId);

    /** Admin view — every package including delisted/draft ones. */
    List<TravelPackage> getAllPackagesForAdmin();

    /** Admin view scoped to packages a specific user created — ahead of full ROLE_PARTNER ownership enforcement. */
    List<TravelPackage> getPackagesByCreator(UUID creatorId);

    TravelPackage getPackageById(UUID packageId);

    /** Adds a new bookable departure batch to an existing package. maxPeople defaults to the package's maxPeople if not given. */
    PackageDeparture addDeparture(UUID packageId, DepartureRequest request);

    /** Edits a departure's date/capacity. Changing maxPeople shifts availableSlots by the same delta (already-booked travelers stay booked). */
    PackageDeparture updateDeparture(UUID departureId, DepartureRequest request);

    /** Soft-cancels one specific departure batch (active=false) without affecting the package template or its other departures. */
    void cancelDeparture(UUID departureId);

    PackageDeparture getDepartureById(UUID departureId);
}
