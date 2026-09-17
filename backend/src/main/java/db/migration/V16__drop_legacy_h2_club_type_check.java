package db.migration;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

/** V2's unnamed check gets an auto-generated name in H2; PostgreSQL's named check is removed in V15. */
public class V16__drop_legacy_h2_club_type_check extends BaseJavaMigration {
    @Override
    public void migrate(Context context) throws Exception {
        var connection = context.getConnection();
        if (!connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("H2")) return;
        var constraints = new ArrayList<String>();
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select constraint_name from information_schema.table_constraints "
                     + "where table_name = 'CLUBS' and constraint_type = 'CHECK'")) {
            while (result.next()) constraints.add(result.getString(1));
        }
        for (String constraint : constraints) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("alter table clubs drop constraint \"" + constraint.replace("\"", "\"\"") + "\"");
            }
        }
    }
}
