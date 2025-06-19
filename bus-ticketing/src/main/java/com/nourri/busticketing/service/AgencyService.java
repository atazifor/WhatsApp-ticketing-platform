package com.nourri.busticketing.service;

import com.nourri.busticketing.model.AgencyContact;
import com.nourri.busticketing.repository.AgencyContactRepository;
import com.nourri.busticketing.repository.AgencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AgencyService {
    private final static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AgencyService.class);
    private final AgencyRepository agencyRepository;
    private final AgencyContactRepository contactRepository;

    @Cacheable("maxTicketsPerAgency")
    public int getMaxTicketsPerBooking(String agencyName) {
        return agencyRepository.findByNameIgnoreCase(agencyName)
            .map(a -> Optional.ofNullable(a.getMaxTicketsPerBooking()).orElse(4))
            .orElse(4);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "agencyContacts", key = "#agencyName.toLowerCase() + '_' + #city.toLowerCase()")
    public Optional<AgencyContact> getContact(String agencyName, String city) {
        return contactRepository.findByAgencyNameAndCityIgnoreCase(agencyName, city);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "allContactsByAgency", key = "#agencyName.toLowerCase()")
    public List<AgencyContact> getAllContacts(String agencyName) {
        return contactRepository.findAllByAgencyNameIgnoreCase(agencyName);
    }

    @CacheEvict(cacheNames = {"maxTicketsPerAgency", "agencyContacts", "allContactsByAgency"}, allEntries = true)
    public void invalidateAll() {
        // intentionally empty — just triggers eviction
    }
}


