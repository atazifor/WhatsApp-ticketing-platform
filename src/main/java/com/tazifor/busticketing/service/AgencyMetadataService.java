package com.tazifor.busticketing.service;

import com.tazifor.busticketing.config.AgencyConfig;
import com.tazifor.busticketing.model.AgencyContact;
import com.tazifor.busticketing.repository.ScheduleDetailsLoader;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

//TODO use a proper caching mechanism later on
@Service
public class AgencyMetadataService {

    private final Map<String, AgencyConfig> configCache;
    private final Map<String, List<AgencyContact>> contactCache;

    public AgencyMetadataService(ScheduleDetailsLoader loader) {
        this.configCache = loader.getAgencyConfigs();
        this.contactCache = loader.getAgencyContacts();
    }

    public AgencyConfig getConfig(String agencyName) {
        return configCache.getOrDefault(agencyName, new AgencyConfig(agencyName, 4));
    }

    public Optional<AgencyContact> getContact(String agency, String city) {
        return contactCache.getOrDefault(agency, List.of()).stream()
            .filter(c -> c.city().equalsIgnoreCase(city))
            .findFirst();
    }

    public List<AgencyContact> getAllContacts(String agency) {
        return contactCache.getOrDefault(agency, List.of());
    }
}

