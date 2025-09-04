package ru.gatling.__common.helpers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseHelper {
    private static DatabaseHelper instance;
    private final Connection connection;

    private DatabaseHelper(String url, String user, String password) {
        try {
            connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized DatabaseHelper getInstance(String url, String user, String password) {
        if (instance == null) {
            instance = new DatabaseHelper(url, user, password);
        }

        return instance;
    }

    public synchronized Connection getConnection() {
        return connection;
    }

    private void close() throws SQLException {
        connection.close();
        instance = null;
    }

    public static synchronized void closeInstance() {
        if (instance != null) {
            try {
                instance.close();
            } catch (SQLException e) {
                throw new RuntimeException();
            }
        }
    }
}
