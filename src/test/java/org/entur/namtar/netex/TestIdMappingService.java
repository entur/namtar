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
import org.entur.namtar.services.DatedServiceJourneyService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.LocalDateTime;

import static junit.framework.TestCase.*;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK, classes = App.class)
public class TestIdMappingService {

    @Autowired
    DatedServiceJourneyService service;

    private LocalDateTime publicationTimestamp;
    private String sourceFileName = "tmp.zip";

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

        String serviceJourneyId = "NSB:ServiceJourney:"+ service.getStorageService().findNextCreationNumber();
        String serviceJourneyId_2 = "NSB:ServiceJourney-UNIQUE:"+service.getStorageService().findNextCreationNumber();

        DatedServiceJourney serviceJourney = new DatedServiceJourney(serviceJourneyId, 0, privateCode, lineRef, departureDate, departureTime);
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney, publicationTimestamp, sourceFileName));

        // Add ServiceJourney that matches, but with different serviceJourneyId
        DatedServiceJourney serviceJourney2 = new DatedServiceJourney(serviceJourneyId_2, 0, privateCode, lineRef, departureDate, departureTime);
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney2, publicationTimestamp, sourceFileName));

        DatedServiceJourney matches = service.findDatedServiceJourney(serviceJourneyId, "latest", departureDate);
        DatedServiceJourney matches_2 = service.findDatedServiceJourney(serviceJourneyId_2, "latest", departureDate);

        assertFalse(matches.equals(matches_2));
        assertFalse(matches.getDatedServiceJourneyId().equals(matches_2.getDatedServiceJourneyId()));

        assertEquals("Should have gotten the same Original id.", matches.getOriginalDatedServiceJourneyId(), matches_2.getOriginalDatedServiceJourneyId());
    }

    @Test
    public void testAddNewServiceJourney() {

        String serviceJourneyId = "NSB:ServiceJourney:"+ service.getStorageService().findNextCreationNumber();

        DatedServiceJourney serviceJourney = new DatedServiceJourney(serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-01", "12:00");
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney, publicationTimestamp, sourceFileName));

        // Add ServiceJourney with same serviceJourneyId, that should not match
        DatedServiceJourney serviceJourney2 = new DatedServiceJourney(serviceJourneyId, 0, "812", "NSB:Line:L1", "2018-01-02", "12:00");
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney2, publicationTimestamp, sourceFileName));

        DatedServiceJourney matches = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-01");
        DatedServiceJourney matches_2 = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-02");

        assertNotNull(matches);
        assertNotNull(matches_2);

        assertFalse("Should have gotten different ids from different versions", matches.equals(matches_2));
    }

    @Test
    public void testReverseSearch() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        String serviceJourneyId = "NSB:ServiceJourney:" + service.getStorageService().findNextCreationNumber();
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, sourceFileName));
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-02", "12:00"), publicationTimestamp, sourceFileName));

        DatedServiceJourney expectedOldMatch = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-02");

        DatedServiceJourney serviceJourney = service.findServiceJourneyByDatedServiceJourney(expectedOldMatch.getDatedServiceJourneyId());

        assertNotNull(serviceJourney);
        assertEquals(serviceJourneyId, serviceJourney.getServiceJourneyId());
        assertEquals("2018-01-02", serviceJourney.getDepartureDate());
    }

}
