package uk.ac.ebi.ega.fire.ingestion.service;

public class OldFireFile {

    private final Long fileId;

    private final Integer exitCode;

    private final String exitReason;

    public OldFireFile(final Long fileId,
                       final Integer exitCode,
                       final String exitReason) {
        this.fileId = fileId;
        this.exitCode = exitCode;
        this.exitReason = exitReason;
    }

    public Long getFileId() {
        return fileId;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getExitReason() {
        return exitReason;
    }

    @Override
    public String toString() {
        return "OldFireFile{" +
                "fileId=" + fileId +
                ", exitCode=" + exitCode +
                ", exitReason='" + exitReason + '\'' +
                '}';
    }
}
