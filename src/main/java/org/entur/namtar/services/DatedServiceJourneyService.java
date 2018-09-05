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

package org.entur.namtar.services;

import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.routes.api.DatedServiceJourneyParam;
import org.entur.namtar.routes.api.ServiceJourneyParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class DatedServiceJourneyService {

    protected static Logger logger = LoggerFactory.getLogger(DatedServiceJourneyService.class);


    private DataStorageService storageService;

    private String GENERATED_ID_PREFIX;

    private long nextId;


    public DatedServiceJourneyService(@Autowired DataStorageService storageService,
                                      @Value("${namtar.generated.id.prefix}") String idPrefix)
            throws IOException, InterruptedException {
        this.storageService = storageService;
        nextId = storageService.findNextCreationNumber();
        GENERATED_ID_PREFIX = idPrefix;
    }

    public DatedServiceJourney createDatedServiceJourney(DatedServiceJourney serviceJourney, LocalDateTime publicationTimestamp, String sourceFileName) {

        long t1 = System.currentTimeMillis();

        // TODO: Handle versions

        DatedServiceJourney alreadyProcessed = storageService.findByServiceJourneyIdAndDate(serviceJourney.getServiceJourneyId(), serviceJourney.getDepartureDate());
        long t2 = System.currentTimeMillis();

        if (alreadyProcessed != null) {
            return null;
        }

        // Check to see if departure with same privateCode already exists...
        DatedServiceJourney datedServiceJourney = storageService.findByPrivateCodeDepartureDate(serviceJourney.getPrivateCode(), serviceJourney.getDepartureDate());
        long t3 = System.currentTimeMillis();

        if (((t2-t1) > 10) & (t3-t2 > 10)) {
            logger.info("Check existing: serviceJourneyId: {} ms, privateCode: {} ms", (t2 - t1), (t3 - t2));
        }
        long creationNumber = nextId++;

        String datedServiceJourneyId = generateDatedServiceJourneyId(creationNumber);
        String originalDatedServiceJourney;
        if (datedServiceJourney != null) {
            // ...exists - set original Id
            originalDatedServiceJourney = datedServiceJourney.getOriginalDatedServiceJourneyId();
        } else {
            // ...does not exist - use current as original Id
            originalDatedServiceJourney = datedServiceJourneyId;
        }

        DatedServiceJourney storageDatedServiceJourney = new DatedServiceJourney();
        storageDatedServiceJourney.setCreatedDate(new Timestamp(System.currentTimeMillis()));
        storageDatedServiceJourney.setServiceJourneyId(serviceJourney.getServiceJourneyId());
        storageDatedServiceJourney.setDepartureDate(serviceJourney.getDepartureDate());
        storageDatedServiceJourney.setDepartureTime(serviceJourney.getDepartureTime());
        storageDatedServiceJourney.setPrivateCode(serviceJourney.getPrivateCode());
        storageDatedServiceJourney.setLineRef(serviceJourney.getLineRef());
        storageDatedServiceJourney.setVersion(serviceJourney.getVersion());
        storageDatedServiceJourney.setDatedServiceJourneyId(datedServiceJourneyId);
        storageDatedServiceJourney.setCreationNumber(creationNumber);
        storageDatedServiceJourney.setOriginalDatedServiceJourneyId(originalDatedServiceJourney);
        storageDatedServiceJourney.setPublicationTimestamp(publicationTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        storageDatedServiceJourney.setSourceFileName(sourceFileName);

        return storageDatedServiceJourney;
    }

    public DatedServiceJourney findServiceJourneyByDatedServiceJourney(String datedServiceJourneyId) {
        DatedServiceJourney datedServiceJourney = storageService.findByDatedServiceJourneyId(datedServiceJourneyId);
        return datedServiceJourney;
    }


    public List<DatedServiceJourney> findServiceJourneysByDatedServiceJourneys(DatedServiceJourneyParam... datedServiceJourneyParams) {

        List<DatedServiceJourney> result = new ArrayList<>();
        for (DatedServiceJourneyParam datedServiceJourneyParam : datedServiceJourneyParams) {
            DatedServiceJourney serviceJourney = storageService.findByDatedServiceJourneyId(datedServiceJourneyParam.datedServiceJourneyId);
            if (serviceJourney != null) {
                result.add(serviceJourney);
            }
        }

        return result;
    }

    public DatedServiceJourney findDatedServiceJourney(String serviceJourneyId, String version, String departureDate) {

        //TODO: Handle versions

        DatedServiceJourney datedServiceJourney = storageService.findByServiceJourneyIdAndDate(serviceJourneyId, departureDate);

        return datedServiceJourney;
    }

    public List<DatedServiceJourney> findDatedServiceJourneys(ServiceJourneyParam... serviceJourneyParams) {
        List<DatedServiceJourney> result = new ArrayList<>();
        for (ServiceJourneyParam serviceJourneyParam : serviceJourneyParams) {
            DatedServiceJourney datedServiceJourney = storageService.findByServiceJourneyIdAndDate(serviceJourneyParam.serviceJourneyId, serviceJourneyParam.departureDate);
            if (datedServiceJourney != null) {
                result.add(datedServiceJourney);
            }
        }

        return result;
    }

    private String generateDatedServiceJourneyId(long creationNumber) {
        return GENERATED_ID_PREFIX + creationNumber;
    }

    public DataStorageService getStorageService() {
        return storageService;
    }
}
