package org.servantscode.person.rest;

import org.servantscode.person.Person;

import java.util.List;

public class PersonQueryResponse {
    private int start;
    private int count;
    private int totalResults;
    private List<Person> results;

    public PersonQueryResponse() {}

    public PersonQueryResponse(int start, int count, int totalResults, List<Person> results) {
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

    public List<Person> getResults() { return results; }
    public void setResults(List<Person> results) { this.results = results; }
}
