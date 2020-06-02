package org.servantscode.person.rest;

import org.servantscode.commons.rest.AuthFilter;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ServiceConfiguration implements ServletContextListener {
    public void contextInitialized(ServletContextEvent arg0) {
        AuthFilter.registerPublicApi("GET", "person/maritalStatuses", false);
        AuthFilter.registerPublicApi("GET", "person/ethnicities", false);
        AuthFilter.registerPublicApi("GET", "person/languages", false);
        AuthFilter.registerPublicApi("GET", "person/religions", false);
        AuthFilter.registerPublicApi("GET", "person/specialNeeds", false);
        AuthFilter.registerPublicApi("GET", "person/phoneNumberTypes", false);
    }
}
