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
 *
 */

package org.entur.namtar.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import org.entur.namtar.repository.blobstore.BlobStoreRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;


@Service
public class BlobStoreService {

	private final Logger log = LoggerFactory.getLogger(BlobStoreService.class);

	@Autowired
	BlobStoreRepository repository;

	@Autowired
	Storage storage;

	@Value("${blobstore.gcs.container.name}")
	String containerName;

	@Value("${blobstore.gcs.subfolder}")
	String subFolder;

	@PostConstruct
	public void init() {
		repository.setStorage(storage);
		repository.setContainerName(containerName);
	}

	public Iterator<Blob> getAllBlobs() {
		log.info("Getting all files");

		long t1 = System.currentTimeMillis();
        Iterator<Blob> blobIterator = repository.listBlobs(subFolder);
        List<Blob> blobs = new ArrayList<>();
        blobIterator.forEachRemaining(blob -> blobs.add(blob));

        blobs.sort(Comparator.comparing(Blob::getUpdateTime));

        log.info("Got {} files in {} ms", blobs.size(), (System.currentTimeMillis()-t1));

        return blobs.iterator();
    }
}
