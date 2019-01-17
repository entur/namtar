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

import com.google.cloud.storage.Blob;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.repository.blobstore.BlobStoreRepository;
import org.entur.namtar.services.DatedServiceJourneyService;
import org.rutebanken.netex.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;

@Service
public class NetexLoader {

    private final Logger log = LoggerFactory.getLogger(NetexLoader.class);

    private final DatedServiceJourneyService datedServiceJourneyService;
    private final BlobStoreRepository repository;

    private final File tmpFileDirectory;

    private Instant lastSuccessfulDataLoaded;

    public NetexLoader(@Autowired DatedServiceJourneyService datedServiceJourneyService,
                       @Autowired BlobStoreRepository repository,
                       @Value("${namtar.tempfile.directory:/tmp}") String tmpFileDirectoryPath) {
        this.datedServiceJourneyService = datedServiceJourneyService;
        this.repository = repository;
        tmpFileDirectory = new File(tmpFileDirectoryPath);
        if (!tmpFileDirectory.exists()) {
            boolean created = tmpFileDirectory.mkdirs();
            log.info("Created tmp-directory with path {}, success: {}", tmpFileDirectoryPath, created);
        } else {
            log.info("Using tmp-directory with path {}, already existed.", tmpFileDirectoryPath);
        }
        lastSuccessfulDataLoaded = Instant.now();
    }

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;///ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private static boolean isLoadingData;

    public void loadNetexFromBlobStore(Iterator<Blob> blobIterator) throws IOException {
        if (!isLoadingData) {
            isLoadingData = true;
            try {
                int counter = 0;
                long t1 = System.currentTimeMillis();
                while (blobIterator.hasNext()) {
                    Blob next = blobIterator.next();
                    String name = next.getName();
                    String filename = name.substring(name.lastIndexOf("/") + 1);

                    if (!datedServiceJourneyService.getStorageService().isAlreadyProcessed(filename)) {

                        if (!filename.isEmpty()) {
                            counter++;

                            datedServiceJourneyService.getStorageService().setFileStatus(filename, false);

                            long download = System.currentTimeMillis();
                            String absolutePath = getFileFromInputStream(repository.getBlob(name), filename);
                            long process = System.currentTimeMillis();
                            processNetexFile(absolutePath, filename);
                            long done = System.currentTimeMillis();

                            datedServiceJourneyService.getStorageService().setFileStatus(filename, true);

                            log.info("{} read - download {} ms, process {} ms", name, (process - download), (done - process));
                            lastSuccessfulDataLoaded = Instant.now();
                        }
                    }
                    blobIterator.remove();
                }
                log.info("Loaded {} netex-files in {} ms", counter, ( System.currentTimeMillis()-t1 ));
            } finally {
                isLoadingData = false;
            }
        } else {
            log.info("Already loading data - ignoring until finished");
        }
    }

    public Instant getLastSuccessfulDataLoaded() {
        return lastSuccessfulDataLoaded;
    }

    private void processNetexFile(String pathname, String sourceFileName) throws IOException {
        File file = new File(pathname);
        if (file.length() == 0) {
            return;
        }
        NetexProcessor processor = new NetexProcessor(file);

        long t1 = System.currentTimeMillis();
        processor.loadFiles();
        log.info("Reading file {} took {} ms", pathname, (System.currentTimeMillis()-t1));

        t1 = System.currentTimeMillis();
        int departureCounter = 0;
        int ignoreCounter = 0;
        for (org.rutebanken.netex.model.ServiceJourney serviceJourney : processor.serviceJourneys) {

            String serviceJourneyId = serviceJourney.getId();
            Integer version = Integer.parseInt(serviceJourney.getVersion());

            String lineRef = resolveLineRef(processor, serviceJourney);

            String departureTime = serviceJourney.getPassingTimes().getTimetabledPassingTime().get(0).getDepartureTime().format(timeFormatter);

            // TODO: Future support for more operators should rely on requirement and usage of ExternalVehicleJourneyRef - not PrivateCode
            // e.g. serviceJourney.getExternalVehicleJourneyRef().getRef();

            if (serviceJourney.getPrivateCode() == null) {
                continue;
            }

            String privateCode = serviceJourney.getPrivateCode().getValue();

            DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
            departureCounter += dayTypes.getDayTypeRef().size();
            for (JAXBElement<? extends DayTypeRefStructure> dayTypeRef : dayTypes.getDayTypeRef()) {

                DayType dayType = processor.dayTypeById.get(dayTypeRef.getValue().getRef());
                DayTypeAssignment dayTypeAssignment = processor.dayTypeAssignmentByDayTypeId.get(dayType.getId());

                String departureDate = dayTypeAssignment.getDate().format(dateFormatter);

                DatedServiceJourney currentServiceJourney = new DatedServiceJourney(serviceJourneyId, version, privateCode, lineRef, departureDate, departureTime);

                DatedServiceJourney datedServiceJourney = datedServiceJourneyService.createDatedServiceJourney(currentServiceJourney, processor.publicationTimestamp, sourceFileName);
                if (datedServiceJourney != null) { // If null, it already exists and should not be added
                    datedServiceJourneyService.getStorageService().addDatedServiceJourney(datedServiceJourney);
                } else {
                    ignoreCounter++;
                }
            }
        }

//        datedServiceJourneyService.getStorageService().addDatedServiceJourneys(datedServiceJourneys);

        log.info("Added {} ServiceJourneys with {} departures in {} ms. {} already existed.", processor.serviceJourneys.size(), departureCounter, (System.currentTimeMillis()-t1), ignoreCounter);
        log.info(datedServiceJourneyService.toString());
    }

    private String resolveLineRef(NetexProcessor processor, ServiceJourney serviceJourney) {

        if (serviceJourney != null) {
            if (serviceJourney.getJourneyPatternRef() != null) {
                String journeyPatternRef = serviceJourney.getJourneyPatternRef().getValue().getRef();

                JourneyPattern journeyPattern = processor.journeyPatternsById.get(journeyPatternRef);
                String routeRefValue = journeyPattern.getRouteRef().getRef();

                Route route = processor.routesById.get(routeRefValue);
                JAXBElement<? extends LineRefStructure> lineRef = route.getLineRef();
                return lineRef.getValue().getRef();
            }

        }
        log.warn("Unable to find LineRef from ServiceJourney with id [{}]", serviceJourney.getId());
        return null;
    }

    private String getFileFromInputStream(InputStream inputStream, String fileName) throws IOException {
        File file = new File(tmpFileDirectory, fileName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        // opens an output stream to createDatedServiceJourney into file
        FileOutputStream outputStream = new FileOutputStream(file);

        int bytesRead;
        byte[] buffer = new byte[2048];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        return file.getAbsolutePath();
    }
}
