# Staging ingestion service
This service is a continuous consumer of a kafka topic that will read messages of type `IngestionEvent` as defined in
 the commons library.
 
## What does it do

Once a message is received the service will perform the following actions:

- Check that the files referred to by the message `plainMd5, encryptedMd5` exist.
    - If the files have been modified then END. 
    - If the files are missing then SKIPPED. No error reported as it is not possible to receive messages
     that have already been processed. 
- Read both md5 files and normalized the md5 values so that they contain only uppercase characters.
- Check the file `pgpFile`.
    - If the file doesn't exist in `user staging path` but it exists within the `stagingPath` this means that while the process completed
     it is possible that the acknowledgement signal was not sent. We will generate a `newFileEvent` and send this
      to the appropriate topic, SUCCESS.
    - If the file exists and has been modified SKIPPED, the user will have modified the file since the emission of the event.
    - If the file exists and has not been modified, then the file will be moved into `stagingPath` with a name that
     will be a composition of the process key and a timestamp. We will generate a `newFileEvent` and sent to the
      appropriate topic, SUCCESS.
      
Once the process has finished, an acknowledgement message is sent to the Kafka queue. Depending on how the previous
 operation ended the following operations will be done: 
 
- SUCCESS
    - Delete md5 files.
- SKIPPED
    - No operations are performed.
- ERROR
    - log output error.

## Configuration properties

- `staging.ingestion.internal.area` path where the files will be stored.
- `spring.kafka.bootstrap-servers` url to the kafka server.
- `staging.ingestion.instance` name of the service instance.
- `staging.ingestion.groupid` name of the consumer group.
- `staging.ingestion.queue` name of the queue to consume `IngestionEvent` messages.
- `new.file.queue` name of the queue to send `newFileEvent` when the process has been successful. 
    
## TODO and improvements
- [ ] Send errors to dead letter queue for further analysis

