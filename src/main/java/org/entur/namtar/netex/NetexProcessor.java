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
import org.rutebanken.netex.model.Common_VersionFrameStructure;
import org.rutebanken.netex.model.CompositeFrame;
import org.rutebanken.netex.model.DataManagedObjectStructure;
import org.rutebanken.netex.model.DatedServiceJourney;
import org.rutebanken.netex.model.DayType;
import org.rutebanken.netex.model.DayTypeAssignment;
import org.rutebanken.netex.model.DayTypes_RelStructure;
import org.rutebanken.netex.model.JourneyPattern;
import org.rutebanken.netex.model.JourneyPatternsInFrame_RelStructure;
import org.rutebanken.netex.model.JourneyRefStructure;
import org.rutebanken.netex.model.Journey_VersionStructure;
import org.rutebanken.netex.model.JourneysInFrame_RelStructure;
import org.rutebanken.netex.model.LinkSequence_VersionStructure;
import org.rutebanken.netex.model.OperatingDay;
import org.rutebanken.netex.model.OperatingPeriod;
import org.rutebanken.netex.model.PublicationDeliveryStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.RoutesInFrame_RelStructure;
import org.rutebanken.netex.model.ServiceAlterationEnumeration;
import org.rutebanken.netex.model.ServiceCalendarFrame;
import org.rutebanken.netex.model.ServiceFrame;
import org.rutebanken.netex.model.ServiceJourney;
import org.rutebanken.netex.model.TimetableFrame;
import org.rutebanken.netex.model.VersionFrameDefaultsStructure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class NetexProcessor {

    private static final Logger logger = LoggerFactory.getLogger(NetexProcessor.class);

    private static JAXBContext jaxbContext;
    private String timeZone;

    static DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE;
    static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    LocalDateTime publicationTimestamp;

    Map<String, JourneyPattern> journeyPatternsById;
    Map<String, Route> routesById;
    List<ServiceJourney> serviceJourneys;
    Map<String, String> serviceJourneyDateToDatedServiceJourney;

    Map<String, Set<DatedServiceJourney>> datedServiceJourneysByServiceJourney;

    Map<String, DayType> dayTypeById;
    Map<String, DayTypeAssignment> dayTypeAssignmentByDayTypeId;
    Map<String, OperatingDay> operatingDayByOperatingDayId;
    Map<String, OperatingPeriod> operatingPeriodById;
    Map<String, Boolean> dayTypeAvailable;

    ZipFile zipFile;

    static {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContext.newInstance(PublicationDeliveryStructure.class);
            } catch (JAXBException e) {
                logger.error("Could not initialize NetexProcessor", e);
            }
        }
    }

    public NetexProcessor(File file) throws IOException {
        zipFile = new ZipFile(file, ZipFile.OPEN_READ);
        journeyPatternsById = new HashMap<>();
        routesById = new HashMap<>();
        serviceJourneys = new ArrayList<>();
        serviceJourneyDateToDatedServiceJourney = new HashMap<>();
        datedServiceJourneysByServiceJourney = new HashMap<>();
        dayTypeById = new HashMap<>();
        dayTypeAssignmentByDayTypeId = new HashMap<>();
        operatingDayByOperatingDayId = new HashMap<>();
        operatingPeriodById = new HashMap<>();
        dayTypeAvailable = new HashMap<>();
    }

    private Unmarshaller createUnmarshaller() throws JAXBException {
        return jaxbContext.createUnmarshaller();
    }

    void loadFiles() {

        // Ensuring all "_[...]_shared_data.xml"-files are processed first
        zipFile.stream()
                .filter(entry -> entry.getName().startsWith("_"))
                .filter(entry -> entry.getName().endsWith(  ".xml"))
                .forEach(entry -> loadFile(entry, zipFile));

        zipFile.stream()
                .filter(entry -> !entry.getName().startsWith("_"))
                .filter(entry -> entry.getName().endsWith(  ".xml"))
                .forEach(entry -> loadFile(entry, zipFile));
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
                    String fileTimeZone = "GMT";
                    if (frameDefaults != null && frameDefaults.getDefaultLocale() != null
                            && frameDefaults.getDefaultLocale().getTimeZone() != null) {
                        fileTimeZone = frameDefaults.getDefaultLocale().getTimeZone();
                    }

                    setTimeZone(fileTimeZone);

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
            throw new RuntimeException("Caught exception when processing file '" + entry + "'", e);
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
                    .getVehicleJourneyOrDatedVehicleJourneyOrNormalDatedVehicleJourney();

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
                if (jStructure instanceof DatedServiceJourney) {
                    DatedServiceJourney dsj = (DatedServiceJourney) jStructure;
                    final String id = dsj.getId();
                    final ServiceAlterationEnumeration serviceAlteration = dsj.getServiceAlteration();


                    if (dsj.getJourneyRef() != null) {

                        final OperatingDay operatingDay = operatingDayByOperatingDayId.get(dsj.getOperatingDayRef().getRef());

                        final LocalDateTime date = operatingDay.getCalendarDate();//.format(dateFormatter);

                        final List<JAXBElement<? extends JourneyRefStructure>> serviceJourneyRefs = dsj.getJourneyRef();
                        for (JAXBElement<? extends JourneyRefStructure> serviceJourneyRef : serviceJourneyRefs) {
                            final JourneyRefStructure serviceJourneyRefValue = serviceJourneyRef.getValue();

                            final String serviceJourneyId = serviceJourneyRefValue.getRef();

                            serviceJourneyDateToDatedServiceJourney.put(getKey(date, serviceJourneyId), id);

                            final Set<DatedServiceJourney> datedServiceJourneys = datedServiceJourneysByServiceJourney.getOrDefault(serviceJourneyId, new HashSet<>());
                            datedServiceJourneys.add(dsj);
                            datedServiceJourneysByServiceJourney.put(serviceJourneyId, datedServiceJourneys);
                        }
                    }

                }
            }
        }
    }

    private String getKey(LocalDateTime date, String serviceJourneyId) {
        return date.format(dateFormatter) + ":" + serviceJourneyId;
    }


    public String getDatedServiceJourneyId(LocalDateTime departureDate, String serviceJourneyId) {
        return serviceJourneyDateToDatedServiceJourney.get(getKey(departureDate, serviceJourneyId));
    }

    // ServiceCalendar
    private void loadServiceCalendarFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceCalendarFrame) {
            ServiceCalendarFrame scf = (ServiceCalendarFrame) commonFrame.getValue();

            if (scf.getServiceCalendar() != null) {
                DayTypes_RelStructure dayTypes = scf.getServiceCalendar().getDayTypes();
                for (JAXBElement dt : dayTypes.getDayTypeRefOrDayType_()) {
                    loadDayType(dt);
                }
            }

            if (scf.getDayTypes() != null) {
                List<JAXBElement<? extends DataManagedObjectStructure>> dayTypes = scf.getDayTypes()
                        .getDayType_();
                for (JAXBElement dt : dayTypes) {
                    loadDayType(dt);
                }
            }

            if (scf.getDayTypeAssignments() != null) {
                List<DayTypeAssignment> dayTypeAssignments = scf.getDayTypeAssignments().getDayTypeAssignment();

                for (DayTypeAssignment dayTypeAssignment : dayTypeAssignments) {
                    String ref = dayTypeAssignment.getDayTypeRef().getValue().getRef();

                    dayTypeAssignmentByDayTypeId.put(ref, dayTypeAssignment);
                }
            }

            if (scf.getOperatingDays() != null) {
                final List<OperatingDay> operatingDays = scf.getOperatingDays().getOperatingDay();

                for (OperatingDay operatingDay : operatingDays) {
                    String id = operatingDay.getId();
                    if (operatingDay.getServiceCalendarRef() != null) {
                        id = operatingDay.getServiceCalendarRef().getRef();
                    }
                    operatingDayByOperatingDayId.put(id, operatingDay);
                }
            }
        }
    }

    private void loadDayType(JAXBElement dt) {
        if (dt.getValue() instanceof DayType) {
            DayType dayType = (DayType) dt.getValue();
            dayTypeById.put(dayType.getId(), dayType);
        }
    }

    private void loadServiceFrames(JAXBElement commonFrame) {
        if (commonFrame.getValue() instanceof ServiceFrame) {
            ServiceFrame sf = (ServiceFrame) commonFrame.getValue();

            //journeyPatterns
            JourneyPatternsInFrame_RelStructure journeyPatterns = sf.getJourneyPatterns();
            if (journeyPatterns != null) {
                List<JAXBElement<?>> journeyPatternOrJourneyPatternView = journeyPatterns
                        .getJourneyPattern_OrJourneyPatternView();
                for (JAXBElement pattern : journeyPatternOrJourneyPatternView) {
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
            RoutesInFrame_RelStructure routesInFrameRelStructure = sf.getRoutes();
            if (routesInFrameRelStructure != null) {
                List<JAXBElement<? extends LinkSequence_VersionStructure>> routesList = routesInFrameRelStructure.getRoute_();
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
