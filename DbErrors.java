package com.hms.util;

import java.sql.SQLException;

/** Turns raw Oracle / JDBC errors into messages that say what to fix. */
public final class DbErrors {

    private DbErrors() { }

    public static String explain(SQLException e) {
        // errors raised by DBUtil itself (driver missing, config missing)
        if (e.getSQLState() != null && e.getSQLState().startsWith("HMS")) {
            return "Database setup problem: " + e.getMessage();
        }
        int code = e.getErrorCode();
        String raw = e.getMessage() == null ? "" : e.getMessage().trim();
        String ora = code > 0 ? String.format("ORA-%05d", code) : "JDBC";
        String hint;
        switch (code) {
            case 1017:
                hint = "Oracle rejected the user name or password in db.properties. Use the same login that works in SQL Developer, then restart Tomcat.";
                break;
            case 28000:
                hint = "The Oracle account is locked (usually after repeated wrong passwords). In SQL Developer run: ALTER USER <db.user> ACCOUNT UNLOCK; then fix db.password.";
                break;
            case 28001:
                hint = "The Oracle password has expired. In SQL Developer run: ALTER USER <db.user> IDENTIFIED BY <new password>; and update db.password.";
                break;
            case 942:
                hint = "The HMS tables are not in the schema this app connects to. Restart Tomcat so the app can create them, or run database/hospital_oracle.sql in the SAME connection (service + user) that db.properties uses.";
                break;
            case 12514: case 12505: case 12154:
                hint = "The listener does not know that service/SID. Fix db.url (XE, XEPDB1 or SID style) to match your SQL Developer connection.";
                break;
            case 12541: case 17002:
                hint = "Cannot reach the Oracle listener. Start the Windows services OracleServiceXE and OracleXETNSListener and check the host/port in db.url.";
                break;
            case 1950: case 1536:
                hint = "This Oracle user has no space/privilege to create tables. Use SYSTEM or grant: GRANT CONNECT, RESOURCE, UNLIMITED TABLESPACE TO <db.user>;";
                break;
            default:
                hint = "";
                break;
        }
        return "Database problem (" + ora + "): " + (hint.isEmpty() ? raw : hint + "  [" + raw + "]");
    }
}
