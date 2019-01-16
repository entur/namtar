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

import org.apache.commons.io.IOUtils;
import org.rutebanken.netex.model.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class NetexProcessor {

    private static JAXBContext jaxbContext;
    private String timeZone;

    LocalDateTime publicationTimestamp;

    Map<String, JourneyPattern> journeyPatternsById;
    Map<String, Route> routesById;
    List<ServiceJourney> serviceJourneys;
    Map<String, DayType> dayTypeById;
    Map<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
    Map<String, OperatingPeriod> operatingPeriodById;
    Map<String, Boolean> dayTypeAvailable;

    ZipFile zipFile;

    static {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
    }

    public NetexProcessor(File file) throws IOException {
        zipFile = new ZipFile(file, ZipFile.OPEN_READ);
        journeyPatternsById = new HashMap<>();
        routesById = new HashMap<>();
        serviceJourneys = new ArrayList<>();
        dayTypeById = new HashMap<>();
        dayTypeAssignmentByDayTypeId = new HashMap<>();
        operatingPeriodById = new HashMap<>();
        dayTypeAvailable = new HashMap<>();
    }



    private Unmarshaller createUnmarshaller() throws JAXBException {
        return jaxbContext.createUnmarshaller();
    }

    void loadFiles() {
        zipFile.stream().forEach(entry -> loadFile(entry, zipFile));
    }

    private byte[] entryAsBytes(ZipFile zipFile, ZipEntry entry) {
        try {
            return IOUtils.toByteArray(zipFile.getInputStream(entry));
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void loadFile(ZipEntry entry, ZipFile zipFile) {
        try {
            byte[] bytesArray = entryAsBytes(zipFile, entry);

            PublicationDeliveryStructure value = parseXmlDoc(bytesArray);
            List<JAXBElement<? extends Common_VersionFrameStructure>> compositeFrameOrCommonFrames = value
                    .getDataObjects().getCompositeFrameOrCommonFrame();

            publicationTimestamp = value.getPublicationTimestamp();

            for (JAXBElement frame : compositeFrameOrCommonFrames) {

                if (frame.getValue() instanceof CompositeFrame) {
                    CompositeFrame cf = (CompositeFrame) frame.getValue();
                    VersionFrameDefaultsStructure frameDefaults = cf.getFrameDefaults();
                    String timeZone = "GMT";
                    if (frameDefaults != null && frameDefaults.getDefaultLocale() != null
                            && frameDefaults.getDefaultLocale().getTimeZone() != null) {
                        timeZone = frameDefaults.getDefaultLocale().getTimeZone();
                    }

                    setTimeZone(timeZone);

                    List<JAXBElement<? extends Common_VersionFrameStructure>> commonFrames = cf
                            .getFrames().getCommonFrame();
                    for (JAXBElement commonFrame : commonFrames) {
                        loadServiceFrames(commonFrame);
                        loadRoutes(commonFrame);
                        loadServiceCalendarFrames(commonFrame);
                        loadTimeTableFrames(commonFrame);
                    }
                }
            }
        } catch (JAXBException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private PublicationDeliveryStructure parseXmlDoc(byte[] bytesArray) throws JAXBException {
        JAXBElement<PublicationDeliveryStructure> root;
        ByteArrayInputStream stream = new ByteArrayInputStream(bytesArray);

        root = (JAXBElement<PublicationDeliveryStructure>) createUnmarshaller().unmarshal(stream);

        return root.getValue();
    }

    // ServiceJourneys
    private void loadTimeTableFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof TimetableFrame) {
            TimetableFrame timetableFrame = (TimetableFrame) commonFrame.getValue();

            JourneysInFrame_RelStructure vehicleJourneys = timetableFrame.getVehicleJourneys();
            List<Journey_VersionStructure> datedServiceJourneyOrDeadRunOrServiceJourney = vehicleJourneys
                    .getDatedServiceJourneyOrDeadRunOrServiceJourney();
            for (Journey_VersionStructure jStructure : datedServiceJourneyOrDeadRunOrServiceJourney) {
                if (jStructure instanceof ServiceJourney) {
                    ServiceJourney sj = (ServiceJourney) jStructure;
                    String journeyPatternId = sj.getJourneyPatternRef().getValue().getRef();

                    JourneyPattern journeyPattern = journeyPatternsById.get(journeyPatternId);

                    if (journeyPattern != null) {
                        if (journeyPattern.getPointsInSequence().
                                getPointInJourneyPatternOrStopPointInJourneyPatternOrTimingPointInJourneyPattern()
                                .size() == sj.getPassingTimes().getTimetabledPassingTime().size()) {
                            serviceJourneys.add(sj);
                        }
                    }
                }
            }
        }
    }

    // ServiceCalendar
    private void loadServiceCalendarFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame) {
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();

            if (scf.getServiceCalendar() != null) {
                DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
                for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        dayTypeById.put(dayType.getId(), dayType);
                    }
                }
            }

            if (scf.getDayTypes() != null) {
                List<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes()
                        .getDayType_();
                for (JAXBElement dt : dayTypes) {
                    if (dt.getValue() instanceof DayType) {
                        DayType dayType = (DayType) dt.getValue();
                        dayTypeById.put(dayType.getId(), dayType);
                    }
                }
            }

            List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments().getDayTypeAssignment();

            for (DayTypeAssignment dayTypeAssignment : dayTypeAssignments) {
                String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();

                dayTypeAssignmentByDayTypeId.put(ref, dayTypeAssignment);
            }
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if (journeyPatterns != null) {
                List<JAXBElement<?>> journeyPattern_orJourneyPatternView = journeyPatterns
                        .getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPattern_orJourneyPatternView) {
                    if (pattern.getValue() instanceof JourneyPattern) {
                        JourneyPattern journeyPattern = (JourneyPattern) pattern.getValue();
                        journeyPatternsById.put(journeyPattern.getId(), journeyPattern);
                    }
                }

            }
        }
    }

    private void loadRoutes(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //Routes
            RoutesInFrame_RelStructure routesInFrame_relStructure = sf.getRoutes();
            if (routesInFrame_relStructure != null) {
                List<JAXBElement<? extends LinkSequence_VersionStructure>> routesList = routesInFrame_relStructure.getRoute_();
                for (JAXBElement element : routesList) {
                    if (element.getValue() instanceof Route) {
                        Route route = (Route) element.getValue();
                        routesById.put(route.getId(), route);
                    }
                }

            }
        }
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getTimeZone() {
        return timeZone;
    }
}
