package teamproject;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class ConnectionManager {

    private HikariDataSource dataSource;
    private Properties dbProperties;

    public ConnectionManager() {
        dbProperties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.err.println("Error finding application.properties");
                throw new RuntimeException("application.properties not found in classpath.");
            }
            dbProperties.load(input);
        }
        catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error loading application.properties", e);
        }
    }

    /**
     * 데이터베이스 연결 풀을 초기화하고 ConnectionManager 내부에 설정
     * 애플리케이션 시작 시 한 번만 호출
     *
     * @throws SQLException 풀 초기화 중 오류 발생 시
     */
    public void connect() throws SQLException {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("Database connection pool is already initialized.");
            return;
        }

        System.out.println("Attempting to initialize database connection pool...");
        try {
            HikariConfig config = getHikariConfig();

            dataSource = new HikariDataSource(config);
            System.out.println("Database connection pool initialized successfully.");

            // 초기 연결 테스트
            try (Connection testConn = dataSource.getConnection()) {
                System.out.println("Initial connection test successful!");
            }

        } catch (SQLException e) {
            System.err.println("!!! CRITICAL ERROR: Failed to initialize database connection pool !!!");
            e.printStackTrace();
            throw e; // 풀 초기화 실패 시 애플리케이션 시작하지 않기
        }
    }

    private HikariConfig getHikariConfig() {
        HikariConfig config = new HikariConfig();

        // properties 파일에서 값 가져오기
        config.setJdbcUrl(dbProperties.getProperty("db.url"));
        config.setUsername(dbProperties.getProperty("db.username"));
        config.setPassword(dbProperties.getProperty("db.password"));

        config.addDataSourceProperty("cachePrepStmts", dbProperties.getProperty("hikari.cachePrepStmts", "true"));
        config.addDataSourceProperty("prepStmtCacheSize", dbProperties.getProperty("hikari.prepStmtCacheSize", "250"));
        config.addDataSourceProperty("prepStmtCacheSqlLimit", dbProperties.getProperty("hikari.prepStmtCacheSqlLimit", "2048"));
        config.setMaximumPoolSize(Integer.parseInt(dbProperties.getProperty("hikari.maximumPoolSize", "10")));
        config.setMinimumIdle(Integer.parseInt(dbProperties.getProperty("hikari.minimumIdle", "5")));
        config.setIdleTimeout(Long.parseLong(dbProperties.getProperty("hikari.idleTimeout", "60000")));
        config.setMaxLifetime(Long.parseLong(dbProperties.getProperty("hikari.maxLifetime", "1800000")));
        config.setConnectionTimeout(Long.parseLong(dbProperties.getProperty("hikari.connectionTimeout", "30000")));
        config.setLeakDetectionThreshold(Long.parseLong(dbProperties.getProperty("hikari.leakDetectionThreshold", "2000")));

        return config;
    }

    /**
     * 커넥션 풀에서 Connection 객체를 빌려온다
     * Connection 사용 후 반드시 close()하여 풀에 반납해야 한다
     *
     * @return Connection 객체
     * @throws SQLException 연결을 빌려오는 중 발생한 오류
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            System.err.println("Connection pool is not initialized or is closed. Call connect() first.");
            throw new SQLException("Connection pool not available.");
        }

        return dataSource.getConnection();
    }

    /**
     * 커넥션 풀을 종료
     * 애플리케이션 종료 시 호출해야 한다
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            System.out.println("Closing database connection pool...");
            dataSource.close();
            System.out.println("Database connection pool closed successfully.");
        } else {
            System.out.println("Database connection pool is already closed or not initialized.");
        }
    }
}