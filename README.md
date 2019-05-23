# ega-ingestion

## Requirements

1. Java 8 (OpenJDK)
1. Maven
1. SAMLtools
1. credentials? (TODO bjuhasz)

### Installation of SAMtools

```
wget https://github.com/samtools/samtools/releases/download/1.9/samtools-1.9.tar.bz2
tar xjvf samtools-1.9.tar.bz2 && cd samtools-1.9
make && sudo make install
```

## Credentials

TODO bjuhasz

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
