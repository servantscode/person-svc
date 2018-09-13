package org.servantscode.person.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBAccess {
    protected Connection getConnection() {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not find database driver.");
        }

        try {
            return DriverManager.getConnection(
                    "jdbc:postgresql://postgres:5432/servantscode", "servant1",
                    "servant!IsH3r3");

        } catch (SQLException e) {
            throw new RuntimeException("Could not connect to database.");
        }
    }
}
