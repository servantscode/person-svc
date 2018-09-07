package org.servantscode.person.rest;

import org.servantscode.person.Person;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

@Path("/person")
public class PersonSvc {

    @GET @Produces(MediaType.APPLICATION_JSON)
    public List<Person> getPeople() {
        ArrayList<Person> list = new ArrayList<>();
        list.add(new Person("James Smith"));
        return list;
    }
}
