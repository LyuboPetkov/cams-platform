package cams.util;

import org.springframework.stereotype.Component;

// Substring-based location matching shared by MatchingService and
// CandidacyService - not geocoding.
@Component
public class LocationEligibility {

    // "Sofia, Bulgaria" -> "sofia". Null/blank means "no constraint", not "exclude".
    public String extractCity(String location) {
        if (location == null || location.isBlank()) {
            return null;
        }

        String city = location.split(",", 2)[0].trim().toLowerCase();
        return city.isBlank() ? null : city;
    }

    // Eligible if the listing is remote or its location text contains the
    // candidate's city; missing data on either side means "no constraint".
    public boolean isEligible(String candidateLocation, String listingLocation, Boolean listingRemote) {
        if (Boolean.TRUE.equals(listingRemote)) {
            return true;
        }

        String candidateCity = extractCity(candidateLocation);
        if (candidateCity == null || listingLocation == null || listingLocation.isBlank()) {
            return true;
        }

        return listingLocation.toLowerCase().contains(candidateCity);
    }
}
