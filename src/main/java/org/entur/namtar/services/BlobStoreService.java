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
import org.apache.camel.Exchange;
import org.entur.namtar.repository.BlobStoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Iterator;


@Service
public class BlobStoreService {

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

	public Iterator<Blob> getAllBlobs(String referential, Exchange exchange) {
		return repository.listBlobs(subFolder);
	}
}
