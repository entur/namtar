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
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingDayRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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
        log.info("Initializing NetexLoader");
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
        log.info("Initializing NetexLoader - done");
    }

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private boolean isLoadingData;

    public void loadNetexFromBlobStore(Iterator<Blob> blobIterator) throws IOException {
        if (!isLoadingData) {
            isLoadingData = true;
            log.info("Loading netex-files");
            try {
                int counter = 0;
                long t1 = System.currentTimeMillis();
                while (blobIterator.hasNext()) {
                    Blob next = blobIterator.next();
                    String name = next.getName();
                    String filename = name.substring(name.lastIndexOf('/') + 1);

                    if (!datedServiceJourneyService.getStorageService().isAlreadyProcessed(filename)) {

                        if (!filename.isEmpty()) {
                            counter++;

                            datedServiceJourneyService.getStorageService().setFileStatus(filename, false);

                            long download = System.currentTimeMillis();
                            String absolutePath = getFileFromInputStream(repository.getBlob(name), filename, true);
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
        datedServiceJourneyService.updateNextCreationNumber();

        long t1 = System.currentTimeMillis();
        processor.loadFiles();
        log.info("Reading file {} took {} ms", pathname, (System.currentTimeMillis()-t1));

        t1 = System.currentTimeMillis();
        int departureCounter = 0;
        int ignoreCounter = 0;
        for (org.rutebanken.netex.model.ServiceJourney serviceJourney : processor.serviceJourneys) {

            try {
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

                List<LocalDateTime> departureDates = new ArrayList<>();
                if (serviceJourney.getDayTypes() != null) {
                    DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
                    departureCounter += dayTypes.getDayTypeRef().size();
                    for (JAXBElement<? extends DayTypeRefStructure> dayTypeRef : dayTypes.getDayTypeRef()) {

                        DayType dayType = processor.dayTypeById.get(dayTypeRef.getValue().getRef());
                        DayTypeAssignment dayTypeAssignment = processor.dayTypeAssignmentByDayTypeId.get(dayType.getId());

                        if (dayTypeAssignment != null && dayTypeAssignment.getDate() != null) {
                            LocalDateTime departureDate = dayTypeAssignment.getDate();
                            departureDates.add(departureDate);
                        }
                    }
                } else {
                    final Set<org.rutebanken.netex.model.DatedServiceJourney> datedServiceJourneys = processor.datedServiceJourneysByServiceJourney.get(serviceJourneyId);
                    for (org.rutebanken.netex.model.DatedServiceJourney datedServiceJourney : datedServiceJourneys) {
                        final OperatingDayRefStructure operatingDayRef = datedServiceJourney.getOperatingDayRef();
                        final OperatingDay operatingDay = processor.operatingDayByOperatingDayId.get(operatingDayRef.getRef());
                        final LocalDateTime departureDate = operatingDay.getCalendarDate();
                        departureDates.add(departureDate);

                    }
                }

                for (LocalDateTime departureDateTime : departureDates) {
                    final String datedServiceJourneyId = processor.getDatedServiceJourneyId(departureDateTime, serviceJourneyId);

                    final String departureDate = departureDateTime.format(dateFormatter);

                    DatedServiceJourney currentServiceJourney = new DatedServiceJourney(datedServiceJourneyId, serviceJourneyId, version, privateCode, lineRef, departureDate, departureTime);


                    DatedServiceJourney datedServiceJourney = datedServiceJourneyService.createDatedServiceJourney(currentServiceJourney, processor.publicationTimestamp, sourceFileName);
                    if (datedServiceJourney != null) { // If null, it already exists and should not be added
                        datedServiceJourneyService.getStorageService().addDatedServiceJourney(datedServiceJourney);
                    } else {
                        ignoreCounter++;
                    }
                }
            } catch (NullPointerException npe) {
                log.warn("Caught NullPointerException for ServiceJourney {} from file {}, continuing", serviceJourney.getId(), sourceFileName);
            }
        }

        log.info("Added {} ServiceJourneys with {} departures in {} ms. {} already existed.", processor.serviceJourneys.size(), departureCounter, (System.currentTimeMillis()-t1), ignoreCounter);
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
            log.warn("Unable to find LineRef from ServiceJourney with id [{}]", serviceJourney.getId());
        }
        log.info("ServiceJourney was null in file {}", processor.zipFile.getName());
        return null;
    }

    private String getFileFromInputStream(InputStream inputStream, String fileName, boolean closeInputStreamWhenRead) throws IOException {
        File file = new File(tmpFileDirectory, fileName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        }
        // opens an output stream to createDatedServiceJourney into file
        try (FileOutputStream outputStream = new FileOutputStream(file)) {

            int bytesRead;
            byte[] buffer = new byte[2048];
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            if (closeInputStreamWhenRead) {
                inputStream.close();
            }
        }

        return file.getAbsolutePath();
    }
}
