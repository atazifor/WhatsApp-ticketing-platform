package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.model.AgencyContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AgencyContactRepository extends JpaRepository<AgencyContact, UUID> {

    @Query("SELECT ac FROM AgencyContact ac JOIN ac.agency a WHERE LOWER(a.name) = LOWER(:agencyName)")
    List<AgencyContact> findAllByAgencyNameIgnoreCase(@Param("agencyName") String agencyName);

    // Also fix your single-result query
    @Query("SELECT ac FROM AgencyContact ac JOIN ac.agency a " +
        "WHERE LOWER(a.name) = LOWER(:agencyName) " +
        "AND LOWER(ac.city) = LOWER(:city)")
    Optional<AgencyContact> findByAgencyNameAndCityIgnoreCase(
        @Param("agencyName") String agencyName,
        @Param("city") String city);
}
