/*
 *
 * Copyright 2019 EMBL - European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.ac.ebi.ega.ingestion.file.manager.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileJobNotFound;
import uk.ac.ebi.ega.ingestion.file.manager.models.EgaFile;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxFileJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.DownloadBoxJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.HistoricDownloadBoxFileJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.HistoricDownloadBoxJob;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.JobStatus;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxJobRepository;

import java.time.LocalDateTime;
import java.util.Collection;

public class DownloadBoxJobService implements IDownloadBoxJobService {

    private final Logger logger = LoggerFactory.getLogger(DownloadBoxJobService.class);

    private DownloadBoxJobRepository boxJobRepository;

    private DownloadBoxFileJobRepository boxFileJobRepository;

    private HistoricDownloadBoxJobRepository historicBoxJobRepository;

    private HistoricDownloadBoxFileJobRepository historicBoxFileJobRepository;

    private IDatasetService datasetService;

    private IMailingService mailingService;

    public DownloadBoxJobService(DownloadBoxJobRepository downloadBoxJobRepository,
                                 DownloadBoxFileJobRepository downloadBoxFileJobRepository,
                                 HistoricDownloadBoxJobRepository historicBoxJobRepository,
                                 HistoricDownloadBoxFileJobRepository historicBoxFileJobRepository,
                                 IDatasetService datasetService, IMailingService mailingService) {
        this.boxJobRepository = downloadBoxJobRepository;
        this.boxFileJobRepository = downloadBoxFileJobRepository;
        this.historicBoxJobRepository = historicBoxJobRepository;
        this.historicBoxFileJobRepository = historicBoxFileJobRepository;
        this.datasetService = datasetService;
        this.mailingService = mailingService;
    }

    @Override
    public DownloadBoxJob createJob(DownloadBoxJob downloadBoxJob) {
        final Collection<EgaFile> files = datasetService.getFiles(downloadBoxJob.getDatasetId());
        downloadBoxJob.setProcessedFiles(0);
        downloadBoxJob.setTotalFiles(files.size());
        final DownloadBoxJob savedDownloadBoxJob = boxJobRepository.save(downloadBoxJob);
        files.stream().forEach(egaFile ->
                boxFileJobRepository.save(new DownloadBoxFileJob(savedDownloadBoxJob, egaFile.getId(), egaFile.getPath())));
        return savedDownloadBoxJob;
    }

    @Override
    public synchronized void finishFileJob(long id) throws FileJobNotFound {
        DownloadBoxFileJob downloadBoxFileJob = boxFileJobRepository.findById(id).orElseThrow(FileJobNotFound::new);
        downloadBoxFileJob.finish(LocalDateTime.now(), LocalDateTime.now());
        boxFileJobRepository.save(downloadBoxFileJob);
        logger.info("DownloadBoxFileJob with id '{}' has finished.", downloadBoxFileJob.getId());

        updateDownloadBoxJob(downloadBoxFileJob.getDownloadBoxJob());
    }

    private void updateDownloadBoxJob(DownloadBoxJob downloadBoxJob) {
        long completed = updateProcessedCount(downloadBoxJob);
        if (completed == downloadBoxJob.getTotalFiles()) {
            moveJobToHistoric(downloadBoxJob);
            reportFinishedJob(downloadBoxJob);
        }
    }

    private long updateProcessedCount(DownloadBoxJob downloadBoxJob) {
        final long count = boxFileJobRepository.countByDownloadBoxJobAndAndStatus(downloadBoxJob, JobStatus.COMPLETED);
        downloadBoxJob.setProcessedFiles((int) count);
        boxJobRepository.save(downloadBoxJob);
        return count;
    }

    private void moveJobToHistoric(DownloadBoxJob downloadBoxJob) {
        HistoricDownloadBoxJob historicJob = historicBoxJobRepository.save(new HistoricDownloadBoxJob(downloadBoxJob));
        boxFileJobRepository.findAllByDownloadBoxJob(downloadBoxJob).forEach(downloadBoxFileJob -> {
            historicBoxFileJobRepository.save(new HistoricDownloadBoxFileJob(historicJob, downloadBoxFileJob));
            boxFileJobRepository.delete(downloadBoxFileJob);
        });
    }

    private void reportFinishedJob(DownloadBoxJob downloadBoxJob) {
        logger.info("DownloadBoxJob with id '{}' has finished.", downloadBoxJob.getId());
        mailingService.sendDownloadBoxFinishedMail(downloadBoxJob);
    }

}
