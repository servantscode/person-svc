package org.servantscode.person.db;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;

public class DBAccess {

    private static HikariDataSource source;

    static {
        try {
            //Ensure driver is loaded into local context.
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Database driver not present.", e);
        }

        source = new HikariDataSource();
        source.setJdbcUrl("jdbc:postgresql://postgres:5432/servantscode");
        source.setUsername("servant1");
        source.setPassword("servant!IsH3r3");
    }

    protected Connection getConnection() {
        try {
            return source.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database.", e);
        }
    }

    protected java.sql.Date convert(Date input) {
        if(input == null) return null;
        return new java.sql.Date(input.getTime());
    }
}
