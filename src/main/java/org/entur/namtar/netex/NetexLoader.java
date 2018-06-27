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
import org.entur.namtar.model.ServiceJourney;
import org.entur.namtar.repository.BlobStoreRepository;
import org.entur.namtar.repository.DatedServiceJourneyService;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypeRefStructure;
import org.rutebanken.netex.model.DayTypeRefs_RelStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.xml.bind.JAXBElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Service
public class NetexLoader {

    protected static Logger log = LoggerFactory.getLogger(NetexLoader.class);

    private final DatedServiceJourneyService datedServiceJourneyService;
    private final BlobStoreRepository repository;

    public NetexLoader(@Autowired DatedServiceJourneyService datedServiceJourneyServicey, @Autowired BlobStoreRepository repository) {
        this.datedServiceJourneyService = datedServiceJourneyServicey;
        this.repository = repository;
    }

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");

    private static final Set<String> alreadyProcessedBlobName = new HashSet<>();
    private static boolean isLoadingData;

    public void loadNetexFromBlobStore(Iterator<Blob> blobIterator) throws IOException {
        if (!isLoadingData) {
            isLoadingData = true;
            try {
                while (blobIterator.hasNext()) {
                    Blob next = blobIterator.next();
                    String name = next.getName();
                    if (!alreadyProcessedBlobName.contains(name)) {
                        alreadyProcessedBlobName.add(name);
                        log.info("Loading netex-file {}", name);
                        String absolutePath = getFileFromInputStream(repository.getBlob(name));
                        String filename = name.substring(name.lastIndexOf("/") + 1);
                        processNetexFile(absolutePath, filename);
                    }
                }
            } finally {
                isLoadingData = false;
            }
        }
    }

    private void processNetexFile(String pathname, String sourceFileName) throws IOException {
        File file = new File(pathname);
        if (file.length() == 0) {
            return;
        }
        NetexProcessor processor = new NetexProcessor(file);

        long t1 = System.currentTimeMillis();
        processor.loadFiles();
        log.info("Loading file {} took {} ms", pathname, (System.currentTimeMillis()-t1));

        t1 = System.currentTimeMillis();
        int diffCounter = 0;
        for (org.rutebanken.netex.model.ServiceJourney serviceJourney : processor.serviceJourneyByPatternId.values()) {

            String serviceJourneyId = serviceJourney.getId();
            String version = serviceJourney.getVersion();
            String lineRef = serviceJourney.getLineRef().getValue().getRef();
            String departureTime = serviceJourney.getPassingTimes().getTimetabledPassingTime().get(0).getDepartureTime().format(timeFormatter);

            DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
            for (JAXBElement<? extends DayTypeRefStructure> dayTypeRef : dayTypes.getDayTypeRef()) {

                DayType dayType = processor.dayTypeById.get(dayTypeRef.getValue().getRef());
                DayTypeAssignment dayTypeAssignment = processor.dayTypeAssignmentByDayTypeId.get(dayType.getId());
                String departureDate = dayTypeAssignment.getDate().format(dateFormatter);
                String privateCode = serviceJourney.getPrivateCode().getValue();

                ServiceJourney currentServiceJourney = new ServiceJourney(serviceJourneyId, version, privateCode, lineRef, departureDate, departureTime);

                if (datedServiceJourneyService.save(currentServiceJourney, processor.publicationTimestamp, sourceFileName)) {
                    diffCounter++;
                }
            }
        }
        log.info("Added {} rows in {} ms", diffCounter, (System.currentTimeMillis()-t1));
        log.info(datedServiceJourneyService.toString());
    }

    private String getFileFromInputStream(InputStream inputStream) throws IOException {
        File f = File.createTempFile("netex", ".zip");

        // opens an output stream to save into file
        FileOutputStream outputStream = new FileOutputStream(f);

        int bytesRead = -1;
        byte[] buffer = new byte[2048];
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }

        outputStream.close();
        inputStream.close();
        return f.getAbsolutePath();
    }
}
