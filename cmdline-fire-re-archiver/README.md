
# CMDLINE-FIRE-ARCHIVER

A simple command-line application for archiving files into Fire.
The input files will not be re-encrypted; they will be stored
in Fire as they are now.

# Running

The program needs two command-line arguments: an input file path
and a relative path on Fire (inside the staging area of Fire).  

An example run might look like this:  

```
java -jar cmdline-fire-archiver-1.0-SNAPSHOT.jar --filePath=/path/to/file-to-be-archived --pathOnFire=relative/path/inside/staging/area/on/fire
```
