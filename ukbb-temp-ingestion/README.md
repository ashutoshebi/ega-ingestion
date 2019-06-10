
# UKBB-TEMP-INGESTION

Project to re-encrypt and ingest UK Biobank files.

# Running

The program needs 3 command-line arguments: INPUT_FILE, INPUT_PASSWORD and OUTPUT_PASSWORD.  
An example run might look like this:  

```
mvn spring-boot:run -Dspring-boot.run.arguments="INPUT_FILE,INPUT_PASSWORD,OUTPUT_PASSWORD"
```

The INPUT_FILE is first decrypted using the AES-256-CBC algorithm and with the given INPUT_PASSWORD,
then it's re-encrypted with the AES-256-CTR algorithm and the given OUTPUT_PASSWORD.
Finally, the re-encrypted OUTPUT_FILE is stored in the Fire Data Object Store.
