package com.amy.auth.repository;

import com.amy.auth.domain.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CountryRepository extends JpaRepository<Country, UUID> {
    Optional<Country> findByIsoCode(String isoCode);
}
