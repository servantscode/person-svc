package org.servantscode.person.rest;

import org.servantscode.person.Family;

import java.util.List;

public class FamilyQueryResponse {
    private int start;
    private int count;
    private int totalResults;
    private List<Family> results;

    public FamilyQueryResponse() {}

    public FamilyQueryResponse(int start, int count, int totalResults, List<Family> results) {
        this.start = start;
        this.count = count;
        this.totalResults = totalResults;
        this.results = results;
    }

    // ----- Accesssors -----
    public int getStart() { return start; }
    public void setStart(int start) { this.start = start; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }

    public List<Family> getResults() { return results; }
    public void setResults(List<Family> results) { this.results = results; }
}
