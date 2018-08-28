package org.entur.namtar.services;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.entur.namtar.model.DatedServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DataStorageService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String KIND_DATED_SERVICE_JOURNEYS = "namtarDatedServiceJourneys";

    private final Datastore datastore;
    private final KeyFactory keyFactory;

    Cache<String, DatedServiceJourney> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();

    Cache<String, String> sourceFileNameCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();
    private Date lastCacheUpdate;

    public DataStorageService(Datastore datastore) {
        this.datastore = datastore;
        this.keyFactory = datastore.newKeyFactory().setKind(KIND_DATED_SERVICE_JOURNEYS);

        //To support that subscriptions are changed from the console (or we get out of sync...)
//        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(() -> {
//            try {
//                populateCacheFromDatastore();
//                logger.debug("Reloads cache from datastore");
//            } catch (Exception e) {
//                logger.error("Got excetption while reloading cache from datastore", e);
//            }
//
//        }, 0, 1, TimeUnit.MINUTES);

        populateCacheFromDatastore();

    }

    public void populateCacheFromDatastore() {
        populateCacheFromDatastore(lastCacheUpdate);
    }

    private void populateCacheFromDatastore(Date date) {
        Query<Entity> query;

        String departureDate = getDepartureDateCacheLimit();
        lastCacheUpdate = new Date();

        if (date != null) {
            query = Query.newEntityQueryBuilder()
                    .setFilter(StructuredQuery.CompositeFilter.and(
                            StructuredQuery.PropertyFilter.gt("created", Timestamp.of(date)),
                            StructuredQuery.PropertyFilter.gt("departureDate", departureDate)
                            )
                    )
                    .setKind(KIND_DATED_SERVICE_JOURNEYS)
                    .build();
        } else {

            query = Query.newEntityQueryBuilder()
                    .setFilter(StructuredQuery.PropertyFilter.gt("departureDate", departureDate))
                    .setKind(KIND_DATED_SERVICE_JOURNEYS)
                    .build();
        }
        long queryStart = System.currentTimeMillis();
        QueryResults<Entity> results = datastore.run(query);

        long queryRun = System.currentTimeMillis();
        int counter = 0;
        while (results.hasNext()) {
            addToCache(convertDatedServiceJourney(results.next()));
            counter++;
        }

        logger.info("Cache populated with {} entities. Query: {} ms, fetch data: {} ms", counter, (queryRun-queryStart), (System.currentTimeMillis()-queryRun));


        ProjectionEntityQuery projectionEntityQuery =
                Query.newProjectionEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setDistinctOn("sourceFileName")
                        .build();

        QueryResults<ProjectionEntity> sourceFileResults = datastore.run(projectionEntityQuery);
        while (sourceFileResults.hasNext()) {
            addSourceFileToCache(sourceFileResults.next().getString("sourceFileName"));
        }

    }

    private String getDepartureDateCacheLimit() {
        return DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.now().minusDays(2));
    }

    private void addSourceFileToCache(String sourceFileName) {
        sourceFileNameCache.put(createCacheKey(sourceFileName), sourceFileName);
    }

    private void addToCache(DatedServiceJourney datedServiceJourney) {
        String[] cacheKeys = createCacheKeys(datedServiceJourney);
        for (String cacheKey : cacheKeys) {
            cache.put(cacheKey, datedServiceJourney);
        }
    }

    private String[] createCacheKeys(DatedServiceJourney datedServiceJourney) {
        return new String[] {
                createCacheKey(datedServiceJourney.getDatedServiceJourneyId()),
                createCacheKey(datedServiceJourney.getServiceJourneyId(), datedServiceJourney.getDepartureDate()),
                createCacheKey(datedServiceJourney.getPrivateCode(), datedServiceJourney.getDepartureDate())
        };
    }

    private static String createCacheKey(String... s) {
        return "" + Arrays.asList(s);
    }

    private Entity convertEntity(DatedServiceJourney s) {
        Key key = datastore.allocateId(keyFactory.newKey());

        Entity.Builder builder = Entity.newBuilder(key)
                .set("created", Timestamp.now())
                .set("serviceJourneyId", StringValue.newBuilder(s.getServiceJourneyId()).build())
                .set("departureDate", StringValue.newBuilder(s.getDepartureDate()).build())
                .set("departureTime", StringValue.newBuilder(s.getDepartureTime()).setExcludeFromIndexes(true).build())
                .set("privateCode", StringValue.newBuilder(s.getPrivateCode()).build())
                .set("lineRef", StringValue.newBuilder(s.getLineRef()).setExcludeFromIndexes(true).build())
                .set("version", LongValue.newBuilder(s.getVersion()).build())
                .set("datedServiceJourneyId",  StringValue.newBuilder(s.getDatedServiceJourneyId()).build())
                .set("originalDatedServiceJourneyId",  StringValue.newBuilder(s.getOriginalDatedServiceJourneyId()).setExcludeFromIndexes(true).build())
                .set("sourceFileName",  StringValue.newBuilder(s.getSourceFileName()).build())
                .set("publicationTimestamp",  StringValue.newBuilder(s.getPublicationTimestamp()).setExcludeFromIndexes(true).build())
                .set("creationNumber", LongValue.newBuilder(s.getDatedServiceJourneyCreationNumber()).build());
                ;
        return builder.build();
    }

    private DatedServiceJourney convertDatedServiceJourney(Entity entity) {
        DatedServiceJourney dsj = new DatedServiceJourney();
        dsj.setServiceJourneyId(entity.getString("serviceJourneyId"));
        dsj.setDepartureDate(entity.getString("departureDate"));
        dsj.setDepartureTime(entity.getString("departureTime"));
        dsj.setPrivateCode(entity.getString("privateCode"));
        dsj.setLineRef(entity.getString("lineRef"));
        dsj.setVersion((int) entity.getLong("version"));
        dsj.setDatedServiceJourneyId(entity.getString("datedServiceJourneyId"));
        dsj.setOriginalDatedServiceJourneyId(entity.getString("originalDatedServiceJourneyId"));
        dsj.setSourceFileName(entity.getString("sourceFileName"));
        dsj.setDatedServiceJourneyCreationNumber(entity.getLong("creationNumber"));
        dsj.setPublicationTimestamp(entity.getString("publicationTimestamp"));
        return dsj;
    }

    public void addDatedServiceJourney(DatedServiceJourney journey) {
        datastore.put(convertEntity(journey));
    }

    public boolean addDatedServiceJourneys(Collection<DatedServiceJourney> journeys) {

        boolean allSuccessful = true;
        List<FullEntity> entities = new ArrayList<>();
        for (DatedServiceJourney journey : journeys) {
            if (entities.size() >= 400) {
                addAsBatch(entities);
                entities.clear();
            }
            entities.add(convertEntity(journey));
            addToCache(journey);
        }

        if (!entities.isEmpty()) {
            addAsBatch(entities);
        }

        logger.info("Added {} ServiceJourneys to Datastore. Success: {}", journeys.size(), allSuccessful);

        return allSuccessful;
    }

    private void addAsBatch(List<FullEntity> entities) {
        long t1 = System.currentTimeMillis();
        Batch batch = datastore.newBatch();
        batch.add(entities.toArray(new FullEntity[entities.size()]));
        Batch.Response response = batch.submit();

        logger.info("Batch job with {} journeys took {} ms. Created {} keys", entities.size(),(System.currentTimeMillis()-t1), response.getGeneratedKeys().size());
    }


//    public void addSourceFileName(String sourceFileName) {
//        if (!isAlreadyProcessed(sourceFileName)) {
//            Key key = datastore.allocateId(sourceFileKeyFactory.newKey());
//            Entity task = Entity.newBuilder(key)
//                    .set("sourceFileName", sourceFileName)
//                    .set("processed", Timestamp.now())
//                    .build();
//            //No need for a transaction when adding
//            datastore.put(task);
//            addSourceFileToCache(sourceFileName);
//        }
//    }

    public DatedServiceJourney findByServiceJourneyIdAndDate(String serviceJourneyId, String departureDate) {
        String key = createCacheKey(serviceJourneyId, departureDate);
        DatedServiceJourney cachedValue = cache.getIfPresent(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setFilter(
                                StructuredQuery.CompositeFilter.and(
                                        StructuredQuery.PropertyFilter.eq("serviceJourneyId", serviceJourneyId.replaceAll(":", "\\:")),
                                        StructuredQuery.PropertyFilter.eq("departureDate", departureDate))
                        )
                        .setLimit(1)
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);

        if (queryResults.hasNext()) {
            DatedServiceJourney datedServiceJourney = convertDatedServiceJourney(queryResults.next());
            addToCache(datedServiceJourney);
            return datedServiceJourney;
        }

        return null;
    }

    public void deleteBySourceFileName(String sourceFileName) {

        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setFilter(StructuredQuery.PropertyFilter.eq("sourceFileName", sourceFileName))
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);

        List<Key> keysToDelete = new ArrayList<>();
        queryResults.forEachRemaining(entity -> keysToDelete.add(entity.getKey()));

        datastore.delete(keysToDelete.toArray(new Key[keysToDelete.size()]));
    }

    public DatedServiceJourney findByDatedServiceJourneyId(String datedServiceJourneyId) {
        String key = createCacheKey(datedServiceJourneyId);
        DatedServiceJourney cachedValue = cache.getIfPresent(key);
        if (cachedValue != null) {
            return cachedValue;
        }
        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setFilter(StructuredQuery.PropertyFilter.eq("datedServiceJourneyId", datedServiceJourneyId.replaceAll(":", "\\:")))
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);

        if (queryResults.hasNext()) {
            DatedServiceJourney datedServiceJourney = convertDatedServiceJourney(queryResults.next());
            addToCache(datedServiceJourney);
            return datedServiceJourney;
        }

        return null;
    }

    public DatedServiceJourney findByPrivateCodeDepartureDate(String privateCode, String departureDate) {
        String key = createCacheKey(privateCode, departureDate);
        DatedServiceJourney cachedValue = cache.getIfPresent(key);
        if (cachedValue != null) {
            return cachedValue;
        }
        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setFilter(
                                StructuredQuery.CompositeFilter.and(
                                        StructuredQuery.PropertyFilter.eq("privateCode", privateCode),
                                        StructuredQuery.PropertyFilter.eq("departureDate", departureDate))
                        )
                        .setLimit(1)
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);

        if (queryResults.hasNext()) {
            DatedServiceJourney datedServiceJourney = convertDatedServiceJourney(queryResults.next());
            addToCache(datedServiceJourney);
            return datedServiceJourney;
        }

        return null;
    }

    public boolean isAlreadyProcessed(String name) {
        String key = createCacheKey(name);
        if (sourceFileNameCache.getIfPresent(key) != null) {
            return true;
        }
        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .setFilter(StructuredQuery.PropertyFilter.eq("sourceFileName", name))
                        .setLimit(1)
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);

        if (queryResults.hasNext()) {
            sourceFileNameCache.put(key, name);
            return true;
        }
        return false;
    }

    public long findNextCreationNumber() {
        EntityQuery query =
                Query.newEntityQueryBuilder()
                        .setKind(KIND_DATED_SERVICE_JOURNEYS)
                        .addOrderBy(StructuredQuery.OrderBy.desc("creationNumber"))
                        .setLimit(1)
                        .build();

        QueryResults<Entity> queryResults = datastore.run(query);
        if (queryResults.hasNext()) {
            DatedServiceJourney datedServiceJourney = convertDatedServiceJourney(queryResults.next());
            // if 10 objects are created, the next should be numbered 11
            return datedServiceJourney.getDatedServiceJourneyCreationNumber() + 1;
        }

        // No objects created - 0 is the first
        return 0;
    }
}
