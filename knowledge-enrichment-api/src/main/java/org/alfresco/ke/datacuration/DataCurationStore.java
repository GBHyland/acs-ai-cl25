package org.alfresco.ke.datacuration;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store to cache mapping between jobId and presigned getUrl.
 * Used to track pending data curation jobs across requests.
 */
@Component
public class DataCurationStore {

    private final Map<String, String> byJob = new ConcurrentHashMap<>();

    /**
     * Stores the presigned getUrl for a given job ID.
     *
     * @param jobId  the job identifier
     * @param getUrl the presigned S3 URL for result retrieval
     */
    public void save(String jobId, String getUrl) {
        byJob.put(jobId, getUrl);
    }

    /**
     * Retrieves the presigned URL for a given job ID, if present.
     *
     * @param jobId the job identifier
     * @return the stored getUrl or {@code null} if not found
     */
    public String find(String jobId) {
        return byJob.get(jobId);
    }

    /**
     * Removes a job entry from the store.
     *
     * @param jobId the job identifier
     */
    public void remove(String jobId) {
        byJob.remove(jobId);
    }
}
