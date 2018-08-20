/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.entur.namtar.repository;

import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.ServiceJourney;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Repository
public class DatedServiceJourneyService {

    @Value("${namtar.generated.id.prefix}")
    private String GENERATED_ID_PREFIX;
    private int idCounter;

    // Reverse mapping
    Map<String, Set<ServiceJourney>> datedServiceJourney_serviceJourneyMap = new HashMap<>();

    Map<String, String> serviceJourney_privateCodeMap = new HashMap<>();

    Map<String, DatedServiceJourney> privateCode_datedServiceJourneyMap = new HashMap<>();

    Map<String, DatedServiceJourney> serviceJourney_datedServiceJourneyMap = new HashMap<>();

    public void save(ServiceJourney serviceJourney, LocalDateTime publicationTimestamp, String sourceFileName) {

        // Create serviceJourney-key
        String serviceJourneyKey = serviceJourney.getServiceJourneyId() + "_" + serviceJourney.getDepartureDate();

        // privateCode is unique per day
        String privateCodeKey = serviceJourney.getPrivateCode() + "_" + serviceJourney.getDepartureDate();

        String existingKey = serviceJourney_privateCodeMap.put(serviceJourneyKey, privateCodeKey);
        if (existingKey != null && existingKey.equals(privateCodeKey)) {
            // Already exists - no need to update
            return;
        }

        // Check to see if departure with same privateCode already exists...
        DatedServiceJourney datedServiceJourney = privateCode_datedServiceJourneyMap.get(privateCodeKey);

        String datedServiceJourneyId = generateDatedServiceJourneyId();
        String originalDatedServiceJourney;
        if (datedServiceJourney != null) {
            // ...exists - set original Id
            originalDatedServiceJourney = datedServiceJourney.getOriginalDatedServiceJourneyId();
        } else {
            // ...does not exist - use current as original Id
            originalDatedServiceJourney = datedServiceJourneyId;
        }

        DatedServiceJourney newDatedServiceJourney = new DatedServiceJourney(
                                                                datedServiceJourneyId,
                                                                originalDatedServiceJourney,
                                                                publicationTimestamp,
                                                                sourceFileName);

        // Save relation between serviceJourney and DatedServiceJourney
        serviceJourney_datedServiceJourneyMap.put(serviceJourneyKey, newDatedServiceJourney);

        if (!privateCode_datedServiceJourneyMap.containsKey(privateCodeKey)) {
            // Save privateCode with future originalId
            privateCode_datedServiceJourneyMap.put(privateCodeKey, newDatedServiceJourney);
        }

        //Add mapping for reverse lookup
        Set<ServiceJourney> serviceJourneys = datedServiceJourney_serviceJourneyMap.getOrDefault(datedServiceJourneyId, new HashSet<>());
        serviceJourneys.add(serviceJourney);
        datedServiceJourney_serviceJourneyMap.put(datedServiceJourneyId, serviceJourneys);
    }

    public Set<ServiceJourney> findServiceJourneysByDatedServiceJourney(String datedServiceJourneyId) {
        return datedServiceJourney_serviceJourneyMap.get(datedServiceJourneyId);
    }

    public DatedServiceJourney findDatedServiceJourneys(String serviceJourneyId, String version, String departureDate) {

        return serviceJourney_datedServiceJourneyMap.get(serviceJourneyId + "_" + departureDate);
    }

    private String generateDatedServiceJourneyId() {
        return GENERATED_ID_PREFIX + idCounter++;
    }

    @Override
    public String toString() {
        return "departures: " + serviceJourney_privateCodeMap.size() + ", privateCode_date: " + privateCode_datedServiceJourneyMap.size() + ", generated ids " + idCounter;
    }
}
