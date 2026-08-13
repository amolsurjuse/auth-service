package com.electrahub.identity.repository;

import com.electrahub.identity.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {
    /**
     * Retrieves find by iso code for `CountryRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param isoCode input consumed by findByIsoCode.
     * @return result produced by findByIsoCode.
     */
    Optional<Country> findByIsoCode(String isoCode);
    /**
     * Retrieves find by iso code and enabled true for `CountryRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @param isoCode input consumed by findByIsoCodeAndEnabledTrue.
     * @return result produced by findByIsoCodeAndEnabledTrue.
     */
    Optional<Country> findByIsoCodeAndEnabledTrue(String isoCode);
    /**
     * Retrieves find by enabled true order by name asc for `CountryRepository`.
     *
     * <p>Detailed behavior: follows the current implementation path and
     * enforces component-specific rules in `com.electrahub.identity.repository`.
     * @return result produced by findByEnabledTrueOrderByNameAsc.
     */
    List<Country> findByEnabledTrueOrderByNameAsc();
}
