package com.tazifor.busticketing.model;

import java.util.List;

public record AgencySchedule(String agency,
                             String from,
                             String to,
                             String travelClass,
                             int price,
                             String time
) {
    public boolean hasClassIn(List<String> selectedClasses) {
        return selectedClasses.isEmpty() || selectedClasses.contains(travelClass);
    }

    public boolean isOperatedBy(List<String> selectedAgencies) {
        return selectedAgencies.isEmpty() || selectedAgencies.contains(agency);
    }

    public boolean departsAt(List<String> selectedTimes) {
        return selectedTimes.isEmpty() || selectedTimes.contains(time); // simple match for now
    }
}