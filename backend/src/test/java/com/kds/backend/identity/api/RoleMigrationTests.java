package com.kds.backend.identity.api;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import java.sql.DriverManager;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class RoleMigrationTests {
    @Test void upgradesExistingCreatorAndMemberAssignments() throws Exception {
        String url = "jdbc:h2:mem:role_upgrade_" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("2").load().migrate();
        UUID club = UUID.randomUUID();
        try (var connection = DriverManager.getConnection(url, "sa", "")) {
            try (var statement = connection.prepareStatement("insert into clubs(id,name,club_type,created_at) values (?, 'Legacy', 'INVESTMENT_CLUB', CURRENT_TIMESTAMP)")) {
                statement.setObject(1, club); statement.executeUpdate();
            }
            for (boolean admin : new boolean[]{true, false}) {
                UUID user = UUID.randomUUID();
                try (var statement = connection.prepareStatement("insert into users(id,email,password_hash,created_at,updated_at) values (?, ?, 'hash', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")) {
                    statement.setObject(1, user); statement.setString(2, user + "@example.test"); statement.executeUpdate();
                }
                try (var statement = connection.prepareStatement("insert into club_memberships(id,club_id,user_id,administrator,created_at) values (?, ?, ?, ?, CURRENT_TIMESTAMP)")) {
                    statement.setObject(1, UUID.randomUUID()); statement.setObject(2, club); statement.setObject(3, user); statement.setBoolean(4, admin); statement.executeUpdate();
                }
            }
            Flyway.configure().dataSource(url, "sa", "").load().migrate();
            try (var statement = connection.prepareStatement("select role_code from club_memberships where club_id = ? order by role_code")) {
                statement.setObject(1, club);
                try (var result = statement.executeQuery()) {
                    assertTrue(result.next()); assertEquals("ADMINISTRATOR", result.getString(1));
                    assertTrue(result.next()); assertEquals("MEMBER", result.getString(1)); assertFalse(result.next());
                }
            }
        }
    }
}
