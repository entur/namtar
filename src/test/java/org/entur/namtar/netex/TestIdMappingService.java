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

package org.entur.namtar.netex;

import org.entur.namtar.App;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.ServiceJourney;
import org.entur.namtar.repository.DatedServiceJourneyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;
import java.util.Set;

import static junit.framework.TestCase.*;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK, classes = App.class)
public class TestIdMappingService {

    @Autowired
    DatedServiceJourneyService service;
    private LocalDateTime publicationTimestamp;

    @Before
    public void init() {
        publicationTimestamp = LocalDateTime.now();
    }

    @Test
    public void testUpdateExistingServiceJourney() {

        String privateCode = "812";
        String lineRef = "NSB:Line:L1";
        String departureDate = "2018-01-01";
        String departureTime = "12:00";

        ServiceJourney serviceJourney = new ServiceJourney("NSB:ServiceJourney:1", "0", privateCode, lineRef, departureDate, departureTime);
        service.save(serviceJourney, publicationTimestamp, null);

        // Add ServiceJourney that matches, but with different serviceJourneyId
        ServiceJourney serviceJourney2 = new ServiceJourney("NSB:ServiceJourney:444444", "0", privateCode, lineRef, departureDate, departureTime);
        service.save(serviceJourney2, publicationTimestamp, null);

        DatedServiceJourney matches = service.findDatedServiceJourneys("NSB:ServiceJourney:1", "latest", departureDate);
        DatedServiceJourney matches_2 = service.findDatedServiceJourneys("NSB:ServiceJourney:444444", "latest", departureDate);

        assertFalse(matches.equals(matches_2));
        assertFalse(matches.getDatedServiceJourneyId().equals(matches_2.getDatedServiceJourneyId()));

        assertEquals("Should have gotten the same Original id.", matches.getOriginalDatedServiceJourneyId(), matches_2.getOriginalDatedServiceJourneyId());
    }

    @Test
    public void testAddNewServiceJourney() {

        ServiceJourney serviceJourney = new ServiceJourney("NSB:ServiceJourney:2", "0",  "812", "NSB:Line:L1", "2018-01-01", "12:00");
        service.save(serviceJourney, publicationTimestamp, null);

        // Add ServiceJourney with same serviceJourneyId, that should not match
        ServiceJourney serviceJourney2 = new ServiceJourney("NSB:ServiceJourney:2", "0", "812", "NSB:Line:L1", "2018-01-02", "12:00");
        service.save(serviceJourney2, publicationTimestamp, null);


        DatedServiceJourney matches = service.findDatedServiceJourneys("NSB:ServiceJourney:2", "latest", "2018-01-01");
        DatedServiceJourney matches_2 = service.findDatedServiceJourneys("NSB:ServiceJourney:2", "latest", "2018-01-02");

        assertNotNull(matches);
        assertNotNull(matches_2);

        assertFalse("Should have gotten different ids from different versions", matches.equals(matches_2));
    }

//    @Test
//    public void testAddNewServiceJourneyVersion() {
//
//
//        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
//        service.save(new ServiceJourney("NSB:ServiceJourney:2", "0",  "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, null);
//        service.save(new ServiceJourney("NSB:ServiceJourney:2", "0",  "812", "NSB:Line:L1", "2018-01-02", "12:00"), publicationTimestamp, null);
//
//        List<DatedServiceJourney> expectedOldMatch = service.findDatedServiceJourneys("NSB:ServiceJourney:2", "latest", "2018-01-02");
//
//
//        // Add ServiceJourney with same serviceJourneyId, that should not match
//        service.save(new ServiceJourney("NSB:ServiceJourney:2", "1", "812", "NSB:Line:L1", "2018-01-02", "12:00"), this.publicationTimestamp, null);
//
//
//        DatedServiceJourney matches = service.findDatedServiceJourneys("NSB:ServiceJourney:2", "latest", "2018-01-01");
//        DatedServiceJourney matches_2 = service.findDatedServiceJourneys("NSB:ServiceJourney:2",  null, "2018-01-02");
//
//        assertFalse("Should have gotten different ids from different versions", matches.equals(matches_2));
//        assertTrue(expectedOldMatch.size() == 1);
//        assertTrue(matches.size() == 1);
//        assertTrue(matches_2.size() == 2);
//
//        // Previous version should also be returned
//        assertFalse(matches_2.contains(matches.get(0)));
//        assertTrue(matches_2.contains(expectedOldMatch.get(0)));
//    }
    
    @Test
    public void testReverseSearch() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        service.save(new ServiceJourney("NSB:ServiceJourney:2", "0",  "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, null);
        service.save(new ServiceJourney("NSB:ServiceJourney:2", "0",  "812", "NSB:Line:L1", "2018-01-02", "12:00"), publicationTimestamp, null);

        DatedServiceJourney expectedOldMatch = service.findDatedServiceJourneys("NSB:ServiceJourney:2", "latest", "2018-01-02");

        Set<ServiceJourney> serviceJourneys = service.findServiceJourneysByDatedServiceJourney(expectedOldMatch.getDatedServiceJourneyId());

        assertEquals(1, serviceJourneys.size());

        ServiceJourney serviceJourney = serviceJourneys.iterator().next();
        assertEquals("NSB:ServiceJourney:2", serviceJourney.getServiceJourneyId());
        assertEquals("2018-01-02", serviceJourney.getDepartureDate());
    }

}
