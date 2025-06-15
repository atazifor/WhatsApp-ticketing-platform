package com.tazifor.busticketing.repository;

import com.tazifor.busticketing.config.AgencyConfig;
import com.tazifor.busticketing.model.AgencyContact;
import com.tazifor.busticketing.model.ScheduleDetails;
import com.tazifor.busticketing.model.SeatingInfo;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduleDetailsLoaderTest {
    static ScheduleDetailsLoader loader;

    @BeforeAll
    static void setUp() throws Exception {
        loader = new ScheduleDetailsLoader();
        InputStream is = new ClassPathResource("data/seed_data.json").getInputStream();
        loader.loadFromStream(is);
    }

    @Test
    void it_should_load_schedule_details_correctly() {
        List<ScheduleDetails> all = loader.getAllSchedules();
        assertThat(all).isNotEmpty();

        ScheduleDetails first = all.get(0);
        assertThat(first.schedule().agency()).isNotBlank();
        assertThat(first.schedule().travelClass()).isNotBlank();

        SeatingInfo info = first.seatingInfo();
        assertThat(info.availableInClass(first.schedule().travelClass())).isGreaterThanOrEqualTo(0);
    }

    @Test
    void it_should_load_agency_configs_correctly() {
        Map<String, AgencyConfig> configs = loader.getAgencyConfigs();
        assertThat(configs).isNotEmpty();

        AgencyConfig config = configs.values().iterator().next();
        assertThat(config.maxTicketsPerBooking()).isGreaterThan(0);
        assertThat(config.agencyName()).isNotBlank();
    }

    @Test
    void it_should_load_agency_contacts_correctly() {
        Map<String, List<AgencyContact>> contacts = loader.getAgencyContacts();
        assertThat(contacts).isNotEmpty();
        AgencyContact contact = contacts.values().iterator().next().get(0);
        assertThat(contact.address()).isNotBlank();
    }
}