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
import org.entur.namtar.repository.helpers.ServiceJourneyDetailedKey;
import org.entur.namtar.repository.helpers.ServiceJourneyKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Repository
public class DatedServiceJourneyService {

    @Value("${namtar.generated.id.prefix}")
    private String GENERATED_ID_PREFIX;

    Map<ServiceJourneyKey, Set<ServiceJourneyDetailedKey>> serviceJourneys = new HashMap<>();
    Map<ServiceJourneyDetailedKey, DatedServiceJourney> datedServiceJourneys = new HashMap<>();

    public boolean save(ServiceJourney serviceJourney, LocalDateTime publicationTimestamp, String sourceFileName) {

        ServiceJourneyKey serviceJourneyKey = createKey(serviceJourney);
        Set<ServiceJourneyDetailedKey> matchingServiceJourneys = serviceJourneys.getOrDefault(serviceJourneyKey, new HashSet<>());
        ServiceJourneyDetailedKey detailedKey = createDetailedKey(serviceJourney);
        boolean added = matchingServiceJourneys.add(detailedKey);
        if (added) {
            serviceJourneys.put(serviceJourneyKey, matchingServiceJourneys);
        }
        if (!datedServiceJourneys.containsKey(detailedKey)) {

            DatedServiceJourney datedServiceJourney = new DatedServiceJourney();
            datedServiceJourney.setDatedServiceJourneyId(generateDatedServiceJourneyId());
            datedServiceJourney.setPublicationTimestamp(publicationTimestamp);


            this.datedServiceJourneys.put(detailedKey, datedServiceJourney);
        }
        return added;
    }

    public List<DatedServiceJourney> findDatedServiceJourneys(String serviceJourneyId, String departureDate) {

        Set<ServiceJourneyDetailedKey> detailedKeySet = serviceJourneys.get(new ServiceJourneyKey(serviceJourneyId, departureDate));

        List<DatedServiceJourney> results = new ArrayList<>();
        if (detailedKeySet != null) {
            detailedKeySet.forEach(key -> results.add(datedServiceJourneys.get(key)));
            results.sort(Comparator.comparing(DatedServiceJourney::getPublicationTimestamp).reversed());
        }

        return results;
    }

    private ServiceJourneyKey createKey(ServiceJourney serviceJourney) {
        return new ServiceJourneyKey(serviceJourney.getServiceJourneyId(), serviceJourney.getDepartureDate());
    }
    private ServiceJourneyDetailedKey createDetailedKey(ServiceJourney serviceJourney) {
        return new ServiceJourneyDetailedKey(serviceJourney.getVersion(), serviceJourney.getPrivateCode(), serviceJourney.getLineRef(), serviceJourney.getDepartureDate());
    }

    private String generateDatedServiceJourneyId() {
        return GENERATED_ID_PREFIX + (datedServiceJourneys.size()+1);
    }

    @Override
    public String toString() {
        return "serviceJourneyCount: " + serviceJourneys.size() + ", datedServiceJourneyCount: " + datedServiceJourneys.size();
    }
}
