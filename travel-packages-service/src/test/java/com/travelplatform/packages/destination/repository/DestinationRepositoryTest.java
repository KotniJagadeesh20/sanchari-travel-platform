package com.travelplatform.packages.destination.repository;

import com.travelplatform.packages.destination.entity.Destination;
import com.travelplatform.packages.destination.enums.DestinationCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the real JPQL query (not a mock) — the whole point of this test
 * is proving `search()` actually ANDs its filters together instead of the
 * old controller behavior of applying only whichever one happened to be
 * set. Backed by an in-memory H2 instance (see pom.xml's test-scoped h2
 * dependency), no external DB needed.
 */
@DataJpaTest
class DestinationRepositoryTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private DestinationRepository destinationRepo;

    private Destination persist(String name, DestinationCategory category, double budget, Set<Integer> bestMonths, boolean active) {
        Destination d = new Destination();
        d.setName(name);
        d.setCountry("India");
        d.setCategory(category);
        d.setAverageBudget(budget);
        d.setBestMonths(bestMonths);
        d.setActive(active);
        d.setManualRating(0.0);
        return entityManager.persistAndFlush(d);
    }

    @Test
    void search_withNoFilters_returnsAllActiveDestinations() {
        persist("Goa", DestinationCategory.BEACH, 12000.0, Set.of(11, 12), true);
        persist("Manali", DestinationCategory.HILL_STATION, 20000.0, Set.of(5, 6), true);
        persist("Delisted Beach", DestinationCategory.BEACH, 5000.0, Set.of(), false);

        List<Destination> results = destinationRepo.search(null, null, null, null);

        assertThat(results).hasSize(2);
        assertThat(results).extracting(Destination::getName).containsExactlyInAnyOrder("Goa", "Manali");
    }

    @Test
    void search_combinesKeywordAndCategory_bothMustMatch() {
        persist("Goa Beach Resort Area", DestinationCategory.BEACH, 12000.0, Set.of(11), true);
        persist("Goa Hill View", DestinationCategory.HILL_STATION, 12000.0, Set.of(11), true);

        List<Destination> results = destinationRepo.search("Goa", DestinationCategory.BEACH, null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Goa Beach Resort Area");
    }

    @Test
    void search_combinesCategoryAndMaxBudget_bothMustMatch() {
        persist("Cheap Beach", DestinationCategory.BEACH, 8000.0, Set.of(), true);
        persist("Expensive Beach", DestinationCategory.BEACH, 50000.0, Set.of(), true);

        List<Destination> results = destinationRepo.search(null, DestinationCategory.BEACH, 15000.0, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Cheap Beach");
    }

    @Test
    void search_filtersByVisitMonth_usingMemberOfBestMonths() {
        persist("Winter Destination", DestinationCategory.BEACH, 12000.0, Set.of(11, 12, 1), true);
        persist("Summer Destination", DestinationCategory.HILL_STATION, 12000.0, Set.of(5, 6), true);

        List<Destination> results = destinationRepo.search(null, null, null, 12);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Winter Destination");
    }

    @Test
    void search_combinesAllFourFiltersTogether() {
        // Matches everything except the visitMonth
        persist("Almost Match", DestinationCategory.BEACH, 12000.0, Set.of(6), true);
        // Matches everything
        persist("Full Match", DestinationCategory.BEACH, 12000.0, Set.of(11, 12), true);

        List<Destination> results = destinationRepo.search("Match", DestinationCategory.BEACH, 15000.0, 11);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Full Match");
    }

    @Test
    void search_neverReturnsDelistedDestinations_regardlessOfFilters() {
        persist("Delisted Goa", DestinationCategory.BEACH, 12000.0, Set.of(), false);

        List<Destination> results = destinationRepo.search("Goa", null, null, null);

        assertThat(results).isEmpty();
    }
}
