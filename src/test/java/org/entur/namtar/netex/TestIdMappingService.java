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
import java.util.Collection;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment= SpringBootTest.WebEnvironment.MOCK, classes = App.class)
public class TestIdMappingService {

    @Autowired
    DatedServiceJourneyService service;

    private LocalDateTime publicationTimestamp;
    private final String sourceFileName = "tmp.zip";

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

        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + ""+ service.getStorageService().findNextCreationNumber();
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
    public void testUpdateExistingServiceJourneyWithProvidedDatedServiceJourneyId() {

        String privateCode = "812";
        String lineRef = "NSB:Line:L1";
        String departureDate = "2018-01-01";
        String departureTime = "12:00";

        String datedServiceJourney = "NSB:DatedServiceJourney:123123-123123-123123";

        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + ""+ service.getStorageService().findNextCreationNumber();
        String serviceJourneyId_2 = "NSB:ServiceJourney-UNIQUE:"+service.getStorageService().findNextCreationNumber();

        DatedServiceJourney serviceJourney = new DatedServiceJourney(datedServiceJourney, serviceJourneyId, 0, privateCode, lineRef, departureDate, departureTime);
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney, publicationTimestamp, sourceFileName));

        // Add ServiceJourney that matches, but with different serviceJourneyId
        DatedServiceJourney serviceJourney2 = new DatedServiceJourney(datedServiceJourney, serviceJourneyId_2, 0, privateCode, lineRef, departureDate, departureTime);
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney2, publicationTimestamp, sourceFileName));

        DatedServiceJourney matches = service.findDatedServiceJourney(serviceJourneyId, "latest", departureDate);
        DatedServiceJourney matches_2 = service.findDatedServiceJourney(serviceJourneyId_2, "latest", departureDate);

        assertFalse(matches.equals(matches_2));
        assertTrue(matches.getDatedServiceJourneyId().equals(matches_2.getDatedServiceJourneyId()));

        assertEquals("Should have gotten the same Original id.", matches.getOriginalDatedServiceJourneyId(), matches_2.getOriginalDatedServiceJourneyId());
    }

    @Test
    public void testAddNewServiceJourney() {

        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + ""+ service.getStorageService().findNextCreationNumber();

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
    public void testAddNewServiceJourneyWithProvidedDatedServiceJourneyId() {

        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + ""+ service.getStorageService().findNextCreationNumber();

        final String datedServiceJourneyId = "NSB:DatedServiceJourney:"+getRandomId();
        DatedServiceJourney serviceJourney = new DatedServiceJourney(datedServiceJourneyId, serviceJourneyId, 0,  "123", "NSB:Line:L1", "2018-01-01", "12:00");

        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney, publicationTimestamp, sourceFileName));

        // Add ServiceJourney with same serviceJourneyId, that should not match
        DatedServiceJourney serviceJourney2 = new DatedServiceJourney(serviceJourneyId, 0, "123", "NSB:Line:L1", "2018-01-02", "12:00");
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney2, publicationTimestamp, sourceFileName));

        DatedServiceJourney matches = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-01");
        DatedServiceJourney matches_2 = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-02");

        assertNotNull(matches);
        assertNotNull(matches_2);

        assertFalse("Should have gotten different ids from different versions", matches.equals(matches_2));

        assertEquals(datedServiceJourneyId, matches.getDatedServiceJourneyId());
        assertEquals(datedServiceJourneyId, matches.getOriginalDatedServiceJourneyId());

        assertNotSame(datedServiceJourneyId, matches_2.getDatedServiceJourneyId());
    }


    @Test
    public void testUpdateServiceJourneyWithProvidedDatedServiceJourneyId() {

        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId();

        DatedServiceJourney serviceJourney = new DatedServiceJourney(serviceJourneyId, 0,  "234", "NSB:Line:L1", "2018-01-01", "12:00");

        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney, publicationTimestamp, sourceFileName));

        final String datedServiceJourneyId = "NSB:DatedServiceJourney:"+getRandomId();

        // Add ServiceJourney with provided datedServiceJourneyId,  new serviceJourneyId and same privateCode/date
        final String serviceJourneyId2 = "NSB:ServiceJourney:" + getRandomId();

        DatedServiceJourney serviceJourney2 = new DatedServiceJourney(datedServiceJourneyId, serviceJourneyId2, 0, "234", "NSB:Line:L1", "2018-01-01", "12:00");
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(serviceJourney2, publicationTimestamp, sourceFileName));

        DatedServiceJourney matches = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-01");
        DatedServiceJourney matches_2 = service.findDatedServiceJourney(serviceJourneyId2, "latest", "2018-01-01");

        assertNotNull(matches);
        assertNotNull(matches_2);

        assertFalse("Should have gotten different ids from different versions", matches.equals(matches_2));

        // Verify that new record has been created with same oDSJ as DSJ
        assertEquals(matches.getDatedServiceJourneyId(), matches.getOriginalDatedServiceJourneyId());

        // Verify that first DSJ does not have the provided DSJ-id
        assertNotSame(datedServiceJourneyId, matches.getOriginalDatedServiceJourneyId());

        // Verify that second DSJ has the provided DSJ-id
        assertEquals(datedServiceJourneyId, matches_2.getDatedServiceJourneyId());

        // Verify that the second DSJ has the same oDSJ as first
        assertEquals(matches.getOriginalDatedServiceJourneyId(), matches_2.getOriginalDatedServiceJourneyId());
    }

    @Test
    public void testReverseSearch() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + "" + service.getStorageService().findNextCreationNumber();
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, sourceFileName));
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-02", "12:00"), publicationTimestamp, sourceFileName));

        DatedServiceJourney expectedOldMatch = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-02");

        DatedServiceJourney serviceJourney = service.findServiceJourneyByDatedServiceJourney(expectedOldMatch.getDatedServiceJourneyId());

        assertNotNull(serviceJourney);
        assertEquals(serviceJourneyId, serviceJourney.getServiceJourneyId());
        assertEquals("2018-01-02", serviceJourney.getDepartureDate());
    }

    @Test
    public void testReverseSearchWithProvidedDatedServiceJourneyId() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        String serviceJourneyId = "NSB:ServiceJourney:" + getRandomId() + "" + service.getStorageService().findNextCreationNumber();

        final String datedServiceJourneyId = "NSB:DatedServiceJourney:111-222-333";

        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(datedServiceJourneyId, serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, sourceFileName));
        service.getStorageService().addDatedServiceJourney(service.createDatedServiceJourney(new DatedServiceJourney(datedServiceJourneyId, serviceJourneyId, 0,  "812", "NSB:Line:L1", "2018-01-02", "12:00"), publicationTimestamp, sourceFileName));

        DatedServiceJourney expectedOldMatch = service.findDatedServiceJourney(serviceJourneyId, "latest", "2018-01-02");

        assertEquals(datedServiceJourneyId, expectedOldMatch.getDatedServiceJourneyId());
        assertEquals(datedServiceJourneyId, expectedOldMatch.getOriginalDatedServiceJourneyId());

        DatedServiceJourney serviceJourney = service.findServiceJourneyByDatedServiceJourney(expectedOldMatch.getDatedServiceJourneyId());

        assertNotNull(serviceJourney);
        assertEquals(serviceJourneyId, serviceJourney.getServiceJourneyId());
        assertEquals("2018-01-02", serviceJourney.getDepartureDate());
    }


    @Test
    public void testReverseSearchForOriginalDatedServiceJourney() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        DatedServiceJourney datedServiceJourney_1 = service.createDatedServiceJourney(new DatedServiceJourney("NSB:ServiceJourney:" + getRandomId() + "1111", 0, "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, sourceFileName);
        service.getStorageService().addDatedServiceJourney(datedServiceJourney_1);

        String originalDatedServiceJourneyId = datedServiceJourney_1.getOriginalDatedServiceJourneyId();

        DatedServiceJourney datedServiceJourney_2 = service.createDatedServiceJourney(new DatedServiceJourney("NSB:ServiceJourney:" + getRandomId() + "2222", 0, "812", "NSB:Line:L1", "2018-01-01", "12:00"), publicationTimestamp, sourceFileName);
        service.getStorageService().addDatedServiceJourney(datedServiceJourney_2);


        assertEquals(datedServiceJourney_1.getOriginalDatedServiceJourneyId(), datedServiceJourney_2.getOriginalDatedServiceJourneyId());

        Collection<DatedServiceJourney> serviceJourneys = service.findServiceJourneysByOriginalDatedServiceJourney(originalDatedServiceJourneyId);

        assertNotNull(serviceJourneys);
        assertTrue(serviceJourneys.contains(datedServiceJourney_1));
        assertTrue(serviceJourneys.contains(datedServiceJourney_2));
    }

    private String getRandomId() {
        return "" + (int)(100000 * Math.random());
    }

    @Test
    public void testReverseSearchForOriginalDatedServiceJourneyWithProvidedDatedServiceJourneyId() {

        LocalDateTime publicationTimestamp = this.publicationTimestamp.minusDays(1);
        final String datedServiceJourneyId = "NSB:DatedServiceJourney:1234-1234-1234";

        final DatedServiceJourney datedServiceJourney = new DatedServiceJourney(datedServiceJourneyId, "NSB:ServiceJourney:" + getRandomId() + "1111", 0, "812", "NSB:Line:L1", "2018-01-01", "12:00");
        DatedServiceJourney datedServiceJourney_1 = service.createDatedServiceJourney(datedServiceJourney, publicationTimestamp, sourceFileName);
        service.getStorageService().addDatedServiceJourney(datedServiceJourney_1);

        String originalDatedServiceJourneyId = datedServiceJourney_1.getOriginalDatedServiceJourneyId();

        final DatedServiceJourney datedServiceJourney2 = new DatedServiceJourney(datedServiceJourneyId, "NSB:ServiceJourney:" + getRandomId() + "2222", 0, "812", "NSB:Line:L1", "2018-01-01", "12:00");
        DatedServiceJourney datedServiceJourney_2 = service.createDatedServiceJourney(datedServiceJourney2, publicationTimestamp, sourceFileName);
        service.getStorageService().addDatedServiceJourney(datedServiceJourney_2);


        assertEquals(datedServiceJourney_1.getOriginalDatedServiceJourneyId(), datedServiceJourney_2.getOriginalDatedServiceJourneyId());

        Collection<DatedServiceJourney> serviceJourneys = service.findServiceJourneysByOriginalDatedServiceJourney(originalDatedServiceJourneyId);

        assertNotNull(serviceJourneys);
        assertTrue(serviceJourneys.contains(datedServiceJourney_1));
        assertTrue(serviceJourneys.contains(datedServiceJourney_2));
    }

}
