package org.servantscode.person.rest;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

@ApplicationPath("rest")
public class PersonApplication extends ResourceConfig {
    public PersonApplication() {
        packages("org.servantscode.person.rest");
    }
}
