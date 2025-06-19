package com.nourri.busticketing.repository;

import com.nourri.busticketing.model.TravelClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TravelClassRepository extends JpaRepository<TravelClass, UUID> {
    @Query("""
        SELECT tc FROM TravelClass tc
        WHERE LOWER(tc.name) = LOWER(:name)
        AND tc.agency.id = :agencyId
        """)
    Optional<TravelClass> findByNameAndAgency(
        @Param("name") String name,
        @Param("agencyId") UUID agencyId);

    // Alternative version using agency name
    @Query("""
        SELECT tc FROM TravelClass tc
        JOIN tc.agency a
        WHERE LOWER(tc.name) = LOWER(:className)
        AND LOWER(a.name) = LOWER(:agencyName)
        """)
    Optional<TravelClass> findByNameAndAgencyName(
        @Param("className") String className,
        @Param("agencyName") String agencyName);
}
