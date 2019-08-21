package uk.ac.ebi.ega.fire.ingestion.service;

public class OldFireFile {

    private final Long fireId;

    private final Integer exitCode;

    private final String exitReason;

    public OldFireFile(final Long fireId,
                       final Integer exitCode,
                       final String exitReason) {
        this.fireId = fireId;
        this.exitCode = exitCode;
        this.exitReason = exitReason;
    }

    /**
     * @return fireId the value of the "ega-pro-filer.ega_ARCHIVE.archive.archive_id" column.
     */
    public Long getFireId() {
        return fireId;
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
                "fireId=" + fireId +
                ", exitCode=" + exitCode +
                ", exitReason='" + exitReason + '\'' +
                '}';
    }
}
