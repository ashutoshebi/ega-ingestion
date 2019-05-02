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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.ega.ingestion.file.manager.controller.exceptions.FileJobNotFound;
import uk.ac.ebi.ega.ingestion.file.manager.message.DownloadBoxFileProcess;
import uk.ac.ebi.ega.ingestion.file.manager.models.EgaFile;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.entities.*;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.DownloadBoxJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxFileJobRepository;
import uk.ac.ebi.ega.ingestion.file.manager.persistence.repository.HistoricDownloadBoxJobRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class DownloadBoxJobServiceTest {

    @Mock
    private DownloadBoxJobRepository boxJobRepository;

    @Mock
    private DownloadBoxFileJobRepository boxFileJobRepository;

    @Mock
    private HistoricDownloadBoxJobRepository historicBoxJobRepository;

    @Mock
    private HistoricDownloadBoxFileJobRepository historicBoxFileJobRepository;

    @Mock
    private DatasetService datasetService;

    @Mock
    private IMailingService mailingService;

    @Mock
    private KafkaTemplate<String, DownloadBoxFileProcess> kafkaTemplate;

    @InjectMocks
    private DownloadBoxJobService downloadBoxJobService;

    private static final String EMPTY_STRING = "";

    /**
     * To test createJob executes successfully, and return saved DownloadBoxJob instance.
     */
    @Test
    public void createJob_DownloadBoxJobGiven_ShouldReturnSavedDownloadBoxJob() {

        final EgaFile egaFile = new EgaFile(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        final List<EgaFile> egaFiles = Arrays.asList(egaFile);
        final DownloadBox downloadBox = new DownloadBox();
        downloadBox.setPath(EMPTY_STRING);

        final DownloadBoxAssignation downloadBoxAssignation = new DownloadBoxAssignation();
        downloadBoxAssignation.setDownloadBox(downloadBox);

        final DownloadBoxJob expectedDownloadBoxJob = new DownloadBoxJob();
        expectedDownloadBoxJob.setAssignedDownloadBox(downloadBoxAssignation);
        expectedDownloadBoxJob.setDatasetId(EMPTY_STRING);

        when(datasetService.getFiles(anyString())).thenReturn(egaFiles);
        when(boxJobRepository.save(any(DownloadBoxJob.class))).thenReturn(expectedDownloadBoxJob);
        when(boxFileJobRepository.save(any(DownloadBoxFileJob.class))).thenReturn(null);
        when(kafkaTemplate.send(anyString(), anyString(), any(DownloadBoxFileProcess.class))).thenReturn(null);

        final DownloadBoxJob argumentDownloadBoxJob = new DownloadBoxJob();
        argumentDownloadBoxJob.setDatasetId(EMPTY_STRING);

        final DownloadBoxJob receivedDownloadBoxJob = downloadBoxJobService.createJob(argumentDownloadBoxJob);

        assertTrue(expectedDownloadBoxJob == receivedDownloadBoxJob);
    }

    /**
     * To test finishFileJob executes successfully, without any error/exceptions when Id given.
     */
    @Test
    public void finishFileJob_IdGiven_ShouldExecuteSuccessfully() throws FileJobNotFound {

        final DownloadBox downloadBox = new DownloadBox();
        final DownloadBoxAssignation downloadBoxAssignation = new DownloadBoxAssignation();
        downloadBoxAssignation.setDownloadBox(downloadBox);

        final DownloadBoxJob downloadBoxJob = new DownloadBoxJob();
        downloadBoxJob.setId(Long.valueOf(0));
        downloadBoxJob.setAssignedDownloadBox(downloadBoxAssignation);
        downloadBoxJob.setDatasetId(EMPTY_STRING);

        final EgaFile egaFile = new EgaFile(EMPTY_STRING, EMPTY_STRING, EMPTY_STRING);
        final DownloadBoxFileJob downloadBoxFileJob = new DownloadBoxFileJob(downloadBoxJob, egaFile);
        final HistoricDownloadBoxJob historicDownloadBoxJob = new HistoricDownloadBoxJob(downloadBoxJob);

        when(boxFileJobRepository.findById(any(Long.class))).thenReturn(Optional.ofNullable(downloadBoxFileJob));
        when(boxFileJobRepository.save(any(DownloadBoxFileJob.class))).thenReturn(null);
        when(historicBoxJobRepository.save(any(HistoricDownloadBoxJob.class))).thenReturn(historicDownloadBoxJob);

        final List<DownloadBoxFileJob> downloadBoxFileJobs = Arrays.asList(downloadBoxFileJob);
        when(boxFileJobRepository.findAllByDownloadBoxJob(any(DownloadBoxJob.class))).thenReturn(downloadBoxFileJobs.stream());

        when(historicBoxFileJobRepository.save(any(HistoricDownloadBoxFileJob.class))).thenReturn(null);
        doNothing().when(boxFileJobRepository).delete(any(DownloadBoxFileJob.class));
        doNothing().when(boxJobRepository).delete(any(DownloadBoxJob.class));
        doNothing().when(mailingService).sendDownloadBoxFinishedMail(any(DownloadBoxJob.class));

        downloadBoxJobService.finishFileJob(Long.valueOf(0));
    }

    /***** Negative Scenarios Test Cases *****/

    /**
     * To test if wrong Id passed should throw an exception.
     *
     * @throws FileJobNotFound if wrong Id passed
     */
    @Test(expected = FileJobNotFound.class)
    public void finishFileJob_IdWrongGiven_ShouldThrowFileJobNotFoundException() throws FileJobNotFound {

        when(boxFileJobRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        downloadBoxJobService.finishFileJob(Long.valueOf(0));
    }
}
