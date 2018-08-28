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

import com.google.gson.JsonObject;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.ServiceJourney;
import org.entur.namtar.netex.NetexLoader;
import org.entur.namtar.services.DataStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class DatedServiceJourneyService {

    protected static Logger logger = LoggerFactory.getLogger(NetexLoader.class);


    private final DataStorageService storageService;

    @Value("${namtar.generated.id.prefix}")
    private String GENERATED_ID_PREFIX;

    private long nextId;


    public DatedServiceJourneyService(@Autowired DataStorageService storageService) throws IOException, InterruptedException {
        this.storageService = storageService;
        nextId = storageService.findNextCreationNumber();
    }

    public DatedServiceJourney createDatedServiceJourney(ServiceJourney serviceJourney, LocalDateTime publicationTimestamp, String sourceFileName) {

        long t1 = System.currentTimeMillis();

        // TODO: Handle versions

        DatedServiceJourney alreadyProcessed = storageService.findByServiceJourneyIdAndDate(serviceJourney.getServiceJourneyId(), serviceJourney.getDepartureDate());
        long t2 = System.currentTimeMillis()-t1;

        if (alreadyProcessed != null) {
            return null;
        }

        // Check to see if departure with same privateCode already exists...
        DatedServiceJourney datedServiceJourney = storageService.findByPrivateCodeDepartureDate(serviceJourney.getPrivateCode(), serviceJourney.getDepartureDate());

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
        storageDatedServiceJourney.setServiceJourneyId(serviceJourney.getServiceJourneyId());
        storageDatedServiceJourney.setDepartureDate(serviceJourney.getDepartureDate());
        storageDatedServiceJourney.setDepartureTime(serviceJourney.getDepartureTime());
        storageDatedServiceJourney.setPrivateCode(serviceJourney.getPrivateCode());
        storageDatedServiceJourney.setLineRef(serviceJourney.getLineRef());
        storageDatedServiceJourney.setVersion(serviceJourney.getVersion());
        storageDatedServiceJourney.setDatedServiceJourneyId(datedServiceJourneyId);
        storageDatedServiceJourney.setDatedServiceJourneyCreationNumber(creationNumber);
        storageDatedServiceJourney.setOriginalDatedServiceJourneyId(originalDatedServiceJourney);
        storageDatedServiceJourney.setPublicationTimestamp(publicationTimestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        storageDatedServiceJourney.setSourceFileName(sourceFileName);

        return storageDatedServiceJourney;
    }

    public DatedServiceJourney findServiceJourneysByDatedServiceJourney(String datedServiceJourneyId) {
        DatedServiceJourney datedServiceJourney = storageService.findByDatedServiceJourneyId(datedServiceJourneyId);
        return datedServiceJourney;
    }

    public DatedServiceJourney findDatedServiceJourneys(String serviceJourneyId, String version, String departureDate) {

        //TODO: Handle versions

        DatedServiceJourney datedServiceJourney = storageService.findByServiceJourneyIdAndDate(serviceJourneyId, departureDate);

        return datedServiceJourney;
    }

    private String generateDatedServiceJourneyId(long creationNumber) {
        return GENERATED_ID_PREFIX + creationNumber;
    }

    public DataStorageService getStorageService() {
        return storageService;
    }


    public String getServiceJourneyAsJson(DatedServiceJourney datedServiceJourney) {
        if (datedServiceJourney == null) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("datedServiceJourneyId", datedServiceJourney.getDatedServiceJourneyId());
        jsonObject.addProperty("originalDatedServiceJourneyId", datedServiceJourney.getOriginalDatedServiceJourneyId());
        jsonObject.addProperty("publicationTimestamp", datedServiceJourney.getPublicationTimestamp());
        jsonObject.addProperty("sourceFileName", datedServiceJourney.getSourceFileName());
        return jsonObject.toString();
    }

    public String getDatedServiceJourneyAsJson(DatedServiceJourney datedServiceJourney) {
        if (datedServiceJourney == null) {
            return null;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("serviceJourneyId", datedServiceJourney.getServiceJourneyId());
        jsonObject.addProperty("departureDate", datedServiceJourney.getDepartureDate());
        jsonObject.addProperty("privateCode", datedServiceJourney.getPrivateCode());
        jsonObject.addProperty("version", datedServiceJourney.getVersion());
        return jsonObject.toString();
    }
}
