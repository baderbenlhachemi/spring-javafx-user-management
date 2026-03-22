package com.badereddine.client.model;

/**
 * Data class representing batch import result
 */
public class BatchImportResult {
    private int totalRecords;
    private int successfulImports;
    private int failedImports;

    public BatchImportResult() {
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessfulImports() {
        return successfulImports;
    }

    public void setSuccessfulImports(int successfulImports) {
        this.successfulImports = successfulImports;
    }

    public int getFailedImports() {
        return failedImports;
    }

    public void setFailedImports(int failedImports) {
        this.failedImports = failedImports;
    }

    public double getSuccessRate() {
        if (totalRecords == 0) return 0;
        return (double) successfulImports / totalRecords * 100;
    }
}
