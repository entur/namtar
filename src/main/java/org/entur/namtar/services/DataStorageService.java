/*
 *
 *  * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 *  * the European Commission - subsequent versions of the EUPL (the "Licence");
 *  * You may not use this work except in compliance with the Licence.
 *  * You may obtain a copy of the Licence at:
 *  *
 *  *  https://joinup.ec.europa.eu/software/page/eupl
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the Licence is distributed on an "AS IS" basis,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the Licence for the specific language governing permissions and
 *  * limitations under the Licence.
 *
 */

package org.entur.namtar.services;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.entur.namtar.model.DatedServiceJourney;
import org.entur.namtar.model.SourceFile;
import org.entur.namtar.repository.persistence.StorageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DataStorageService {
    Logger logger = LoggerFactory.getLogger(this.getClass());

    private final StorageRepository repository;

    Cache<String, DatedServiceJourney> cache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .recordStats()
            .build();

    Cache<String, String> sourceFileNameCache = CacheBuilder.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .build();

    private Date lastCacheUpdate;
    private int searchCount;

    public DataStorageService(StorageRepository repository) {
        this.repository = repository;

        populateCache(lastCacheUpdate);
    }

    private void populateCache(Date date) {

        lastCacheUpdate = new Date();
        long queryStart = System.currentTimeMillis();


        Collection<DatedServiceJourney> results = repository.getAllFutureDatedServiceJourneys(date, -2);
        long queryRun = System.currentTimeMillis();

        // Add to cache
        results.forEach(datedServiceJourney -> addToCache(datedServiceJourney));

        logger.info("Cache populated with {} entities. Query: {} ms, fetch data: {} ms", results.size(), (queryRun-queryStart), (System.currentTimeMillis()-queryRun));


        List<String> sourceFileResults = repository.getAllDistinctSourceFileNames();

        sourceFileResults.forEach(s -> addSourceFileToCache(s));

    }

    private void addSourceFileToCache(String sourceFileName) {
        if (sourceFileName != null) {
            sourceFileNameCache.put(createCacheKey(sourceFileName), sourceFileName);
        }
    }

    private DatedServiceJourney addToCache(DatedServiceJourney datedServiceJourney) {
        if (datedServiceJourney != null) {
            String[] cacheKeys = createCacheKeys(datedServiceJourney);
            for (String cacheKey : cacheKeys) {
                cache.put(cacheKey, datedServiceJourney);
            }
        }
        return datedServiceJourney;
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


    public void addDatedServiceJourney(DatedServiceJourney journey) {
        repository.save(journey);
        addToCache(journey);
    }

    protected DatedServiceJourney findInCache(String... keys) {
        if (searchCount++ % 1000 ==  0) {
            logger.info("Searches: {}, Cache stats: {}", searchCount, cache.stats());
        }
        return cache.getIfPresent(createCacheKey(keys));
    }

    public DatedServiceJourney findByServiceJourneyIdAndDate(String serviceJourneyId, String departureDate) {
        DatedServiceJourney cachedValue = findInCache(serviceJourneyId, departureDate);
        if (cachedValue != null) {
            return cachedValue;
        }

        return addToCache(repository.findByServiceJourneyIdAndDate(serviceJourneyId, departureDate));
    }



    public DatedServiceJourney findByDatedServiceJourneyId(String datedServiceJourneyId) {
        DatedServiceJourney cachedValue = findInCache(datedServiceJourneyId);
        if (cachedValue != null) {
            return cachedValue;
        }
        return addToCache(repository.findByDatedServiceJourneyId(datedServiceJourneyId));
    }

    public DatedServiceJourney findByPrivateCodeDepartureDate(String privateCode, String departureDate) {
        DatedServiceJourney cachedValue = findInCache(privateCode, departureDate);
        if (cachedValue != null) {
            return cachedValue;
        }
        return addToCache(repository.findByPrivateCodeDepartureDate(privateCode, departureDate));
    }

    public boolean isAlreadyProcessed(String name) {
        String key = createCacheKey(name);
        if (sourceFileNameCache.getIfPresent(key) != null) {
            return true;
        }
        SourceFile alreadyProcessed = repository.findSourceFileByName(name);
        if (alreadyProcessed != null && alreadyProcessed.isProcessed()) {
            addSourceFileToCache(name);
            return true;
        }
        return false;
    }


    public void setFileStatus(String filename, boolean isProcessed) {
        SourceFile alreadyProcessed = repository.findSourceFileByName(filename);
        if (alreadyProcessed == null) {
            alreadyProcessed = new SourceFile(filename, isProcessed);
        }
        alreadyProcessed.setProcessed(isProcessed);
        repository.save(alreadyProcessed);

        sourceFileNameCache.put(filename, filename);
    }

    public long findNextCreationNumber() {
        return repository.findNextCreationNumber();
    }

}
