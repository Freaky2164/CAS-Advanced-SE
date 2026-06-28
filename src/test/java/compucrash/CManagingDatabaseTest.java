package compucrash;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CManagingDatabaseTest {

    @Test
    void getMainFrame_usesResultSetNext_insteadOfFirst() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);

        when(connection.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        TestManagingDatabase managingDatabase = new TestManagingDatabase(connection);
        CPropertyManager.setUser("test-user");

        managingDatabase.getMainFrame();

        verify(resultSet).next();
        verify(resultSet, never()).first();
    }

    @Test
    void getMainPanels_usesSchemaCompatiblePanelProjection() {
        CapturingManagingDatabase managingDatabase = new CapturingManagingDatabase();
        CPropertyManager.setUser("test-user");

        managingDatabase.getMainPanels();

        assertTrue(managingDatabase.lastSql.contains("test-user"));
    }

    private static class TestManagingDatabase extends CManagingDatabase {
        TestManagingDatabase(Connection connection) {
            this.conn = connection;
        }

        @Override
        void connect(Properties p) {
            // not needed for tests
        }

        @Override
        CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent) {
            return null;
        }

        @Override
        CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent) {
            return null;
        }

        @Override
        public Object getInit(String init) {
            return null;
        }
    }

    private static class CapturingManagingDatabase extends CManagingDatabase {
        private String lastSql;

        @Override
        void connect(Properties p) {
            // not needed for tests
        }

        @Override
        CListDataManagingDatabase createCListDataManagingDatabase(CListDataObject parent) {
            return null;
        }

        @Override
        CInfoDataManagingDatabase createCInfoDataManagingDatabase(CInfoDataObject parent) {
            return null;
        }

        @Override
        public Object getInit(String init) {
            return null;
        }

        @Override
        public Statement getStatement() {
            return null;
        }

        @Override
        protected ResultSet getPreparedResultSet(String sqlString, Object... parameters) {
            this.lastSql = sqlString;
            return null;
        }
    }
}
