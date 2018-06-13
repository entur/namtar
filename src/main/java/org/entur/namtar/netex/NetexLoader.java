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

import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.ServiceJourney;
import org.entur.namtar.repository.DatedServiceJourneyService;
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
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class NetexLoader {

    protected static Logger log = LoggerFactory.getLogger(NetexLoader.class);

    private final DatedServiceJourneyService datedServiceJourneyService;

    public NetexLoader(@Autowired DatedServiceJourneyService datedServiceJourneyServicey) {
        this.datedServiceJourneyService = datedServiceJourneyServicey;
    }

    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm");

    @Value("${namtar.default.netex.download.url}")
    private String defaultUrl;

    public void loadNetexFromUrl(String url) {


        try {
            String pathname = downloadFile(url);

            File file = new File(pathname);
            NetexProcessor processor = new NetexProcessor(file);

            long t1 = System.currentTimeMillis();
            processor.loadFiles();
            log.info("Loading file {} took {} ms", pathname, (System.currentTimeMillis()-t1));

            t1 = System.currentTimeMillis();
            int diffCounter = 0;
            List<DatedServiceJourney> changes = new ArrayList<>();
            for (org.rutebanken.netex.model.ServiceJourney serviceJourney : processor.serviceJourneyByPatternId.values()) {

                String serviceJourneyId = serviceJourney.getId();

                String version = serviceJourney.getVersion();

                JourneyPattern journeyPattern =  processor.journeyPatternsById.get(serviceJourney.getJourneyPatternRef().getValue().getRef());
                List<PointInLinkSequence_VersionedChildStructure> pointInJourneyPattern = journeyPattern.getPointsInSequence().getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern();
                PointInLinkSequence_VersionedChildStructure firstScheduledStopPoint = pointInJourneyPattern.get(0);

                String scheduledStopPointRef = ((StopPointInJourneyPattern) firstScheduledStopPoint).getScheduledStopPointRef().getValue().getRef();

//                String firstQuay = processor.quayIdByStopPointRef.get(scheduledStopPointRef);

                String lineRef = serviceJourney.getLineRef().getValue().getRef();

                String departureTime = serviceJourney.getPassingTimes().getTimetabledPassingTime().get(0).getDepartureTime().format(timeFormatter);

                DayTypeRefs_RelStructure dayTypes = serviceJourney.getDayTypes();
                for (JAXBElement<? extends DayTypeRefStructure> dayTypeRef : dayTypes.getDayTypeRef()) {

                    DayType dayType = processor.dayTypeById.get(dayTypeRef.getValue().getRef());

                    DayTypeAssignment dayTypeAssignment = processor.dayTypeAssignmentByDayTypeId.get(dayType.getId());

                    String departureDate = dayTypeAssignment.getDate().format(dateFormatter);

                    String privateCode = serviceJourney.getPrivateCode().getValue();

                    ServiceJourney currentServiceJourney = new ServiceJourney(serviceJourneyId, version, privateCode, lineRef, departureDate, departureTime);

//                    DatedServiceJourney current = serviceJourneyRepository.findByServiceJourneyIdAndDepartureDateAndVersion(serviceJourneyId, departureDate, version);
//                    if (current == null) {


                    boolean saved = datedServiceJourneyService.save(currentServiceJourney, processor.publicationTimestamp);

                    if (saved) {
                        diffCounter++;
                    }
                }
            }
            log.info("Added {} rows in {} ms", diffCounter, (System.currentTimeMillis()-t1));
            log.info(datedServiceJourneyService.toString());
            
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private String downloadFile(String url) throws IOException {
        if (url == null || url.isEmpty()) {
            log.info("Url not provided - using default");
            url = defaultUrl;
        }
        log.info("Downloading file from [{}]", url);
        long t1 = System.currentTimeMillis();
        // opens input stream from the HTTP connection
        InputStream inputStream = new URL(url).openStream();

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
        String absolutePath = f.getAbsolutePath();
        log.info("Downloading finished, saved to [{}] after {} ms", absolutePath, (System.currentTimeMillis()-t1));
        return absolutePath;
    }
}
