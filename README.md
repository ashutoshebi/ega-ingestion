# ega-ingestion

## Requirements

1. Java 8 (OpenJDK)
1. Maven
1. SAMLtools
1. Credentials

### Installation of SAMtools

```
wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2
tar xjvf samtools-1.9.tar.bz2 && cd samtools-1.9
make && sudo make install
```

## Credentials

We don't store credentials in our `application.properties` files.
Credentials are instead stored in our `~/.m2/settings.xml` file,
for example, like this:

```$xml
<profile>
  <id>localPostgresDatabase</id>
  <activation>
      <activeByDefault>true</activeByDefault>
  </activation>
  <properties>
      <ega.ukbb.temp.ingestion.datasource.re-encrypt.url>jdbc:postgresql://localhost:5432/...</ega.ukbb.temp.ingestion.datasource.re-encrypt.url>
      <ega.ukbb.temp.ingestion.datasource.re-encrypt.username>...</ega.ukbb.temp.ingestion.datasource.re-encrypt.username>
      <ega.ukbb.temp.ingestion.datasource.re-encrypt.password>...</ega.ukbb.temp.ingestion.datasource.re-encrypt.password>
  </properties>
</profile>
```

Most of the secret values (for the properties above) can be found in the `credentials.txt` file in the Vault.


## Compile

`mvn clean test install`

## Run

Currently, there are 4 "sub-projects":
*file-manager, file-discovery, file-encryption-processor, file-re-encryption-processor*.  
An example of running one of them:

```
cd file-re-encryption-processor
mvn spring-boot:run
```
