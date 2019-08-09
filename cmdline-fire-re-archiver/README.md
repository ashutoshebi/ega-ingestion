
# CMDLINE-FIRE-RE-ARCHIVER

A simple command-line application for re-encrypting and archiving files into Fire.

The GPG-encrypted input-file is first decrypted, 
then re-encrypted using Alexander's AES flavour (AesCtr256Ega),
finally it is stored in Fire.

# Running

The program needs two command-line arguments: an input file path
and a relative path on Fire (inside the staging area of Fire).  

An example run might look like this:  

```
java -jar cmdline-fire-re-archiver-1.0-SNAPSHOT.jar --filePath=/path/to/file-to-be-archived --pathOnFire=relative/path/inside/staging/area/on/fire
```
