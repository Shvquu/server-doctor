package com.serverdoctor.core.update;

public record UpdateResult(Status status, String currentVersion, String latestVersion, String releaseUrl, String detail) {

    public enum Status { UP_TO_DATE, UPDATE_AVAILABLE, NO_RELEASES, ERROR }

    public boolean updateAvailable() {
        return status == Status.UPDATE_AVAILABLE;
    }

    static UpdateResult error(String current, String detail) {
        return new UpdateResult(Status.ERROR, current, null, null, detail);
    }

}
