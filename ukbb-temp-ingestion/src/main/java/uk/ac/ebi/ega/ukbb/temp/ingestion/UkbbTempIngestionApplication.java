package uk.ac.ebi.ega.ukbb.temp.ingestion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class UkbbTempIngestionApplication {

    public static void main(String[] args) {
        SpringApplication.run(UkbbTempIngestionApplication.class, args);
    }

}
