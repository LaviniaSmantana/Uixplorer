package com.licenta.v1.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.licenta.v1.models.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoordinatesService {

    private final RestTemplate restTemplate;

    public CoordinatesService() {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Uses the Nominatim API from OpenStreetMap to return the coordinates (latitude & longitude) of a country and state.
     * @param country The country of the location.
     * @param state The state from the country.
     * @return The latitude and longitude of that location.
     */
    public double[] getCoordinates(String country, String state) {

        String url = String.format("https://nominatim.openstreetmap.org/search?country=%s&state=%s&format=json&limit=1",
                country, state);
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);

        if (response != null && response.size() > 0) {
            double latitude = response.get(0).get("lat").asDouble();
            double longitude = response.get(0).get("lon").asDouble();
            return new double[]{latitude, longitude};
        }
        return null;
    }

    /**
     * Function that returns the distance between two users, calculated using the Manhattan Distance.
     * @param user1 Instance of the first user.
     * @param user2 Instance of the second user.
     * @return The distance between the users.
     */
    public double calculateManhattanDistance(AppUser user1, AppUser user2) {
        double lat1 = user1.getLatitude();
        double lon1 = user1.getLongitude();
        double lat2 = user2.getLatitude();
        double lon2 = user2.getLongitude();

        return Math.abs(lat1 - lat2) + Math.abs(lon1 - lon2);
    }

    /**
     * Function that returns the top 3 closest users to a specific user, based on the distance between them.
     * @param users List of all the users that are connected with the user.
     * @param currentUser Instance of a user.
     * @return A list of 3 Users that are the closest to our user instance.
     */
    public List<AppUser> getTop3ClosestUsers(List<AppUser> users, AppUser currentUser) {

        List<Long> friendIds = currentUser.getFriends().stream().map(AppUser::getId).toList();

        return users.stream()
                .filter(user -> !friendIds.contains(user.getId()))
                .filter(user -> !user.getId().equals(currentUser.getId()))
                .sorted(Comparator.comparingDouble(user -> calculateManhattanDistance(currentUser, user)))
                .limit(3)
                .collect(Collectors.toList());
    }
}
