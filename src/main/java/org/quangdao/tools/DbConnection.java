package org.quangdao.tools;

import lombok.extern.log4j.Log4j2;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/*
@Log4j2
public class DbConnection {
    private final String inputTable;
    private final String inputMailColumn;
    private final String inputPassColumn;
    private final String outputTable;
    private final String outputMailColumn;
    private final String inputWhere;
    private final String dumpFile;
    private final long batchMax;

    private final Connection conn;
    private List<String> emails = new ArrayList<>();
    private final ScheduledExecutorService scheduledExecutorService;

    private final List<ScheduledFuture<?>> daemonTasks = new ArrayList<>();

    public DbConnection(long batchMax, long batchSeconds, ScheduledExecutorService scheduledExecutorService, String dumpFile, String addr, String dbType, String username, String password, String schema, String inputTable, String inputMailColumn, String inputPassColumn, String outputTable, String outputMailColumn) throws Exception {
        this.scheduledExecutorService = scheduledExecutorService;
        this.inputTable = inputTable;
        this.inputMailColumn = inputMailColumn;
        this.inputPassColumn = inputPassColumn;
        this.outputTable = outputTable;
        this.outputMailColumn = outputMailColumn;
        this.inputWhere = DbConnection.getWhereClause(dbType, inputMailColumn);
        this.dumpFile = dumpFile;
        this.batchMax = batchMax;

        String url = "jdbc:"+ DbConnection.getDriverName(dbType) +"://" + addr + "/" + schema;
        conn = DriverManager.getConnection(url, username, password);

        synchronized (daemonTasks) {
            daemonTasks.add(scheduledExecutorService.scheduleAtFixedRate(() -> {
                daemonTask();
                synchronized (daemonTasks) {
                    daemonTasks.removeAll(daemonTasks.stream().filter(ScheduledFuture::isDone).toList());
                }
            }, batchSeconds, batchSeconds, TimeUnit.SECONDS));
        }
   }

    ;
    List<MailPass> getEmail(long limit, long offset) throws SQLException {
        var stmt = conn.prepareStatement("SELECT `%s`, `%s` FROM `%s` WHERE %s LIMIT ? OFFSET ?".formatted(inputMailColumn, inputPassColumn, inputTable, inputWhere));
        stmt.setLong(1, limit);
        stmt.setLong(2, offset);
        log.info("SQL: "+stmt.toString());
        var rs = stmt.executeQuery();

        ArrayList<MailPass> ret = new ArrayList<>();
        while(rs.next())
            ret.add(new MailPass(rs.getString(1), rs.getString(2)));
        return ret;
    }

    public void addEmail(String email) {
        synchronized (this) {
            emails.add(email);
            if(emails.size() > batchMax)
                synchronized (daemonTasks) {
                    daemonTasks.add(scheduledExecutorService.schedule(this::daemonTask, 0, TimeUnit.MICROSECONDS));
                }
        }
    }

    private void daemonTask() {
        List<String> tmp;
        synchronized (this) {
            tmp = emails;
            emails = new ArrayList<>();
        }

        try {
            saveEmailBatch(tmp);
        } catch (Throwable t) {
            log.error("------------------ERROR----------------");
            log.error("------------------INSERT FAILED----------------");
            log.error(t.getMessage(), t);
            log.info("------------------SAVING TO DUMP FILE----------------");
            log.info("Saving %s records to file '%s'".formatted(tmp.size(), dumpFile));

            try(var writer = new FileWriter(dumpFile, true)) {
                for(var i : tmp)
                    writer.append(i).append('\n');
            } catch (Throwable throwable) {
                log.error("--------------------DUMP TO FILE FAILED-------------------");
                log.error(throwable.getMessage(), throwable);
                log.info("Save to log :<<<");
                log.info(tmp);
            }

        }
    }

    private void saveEmailBatch(List<String> emails) throws SQLException {
        if(emails.size() == 0) return;
        StringBuilder sb = new StringBuilder("INSERT INTO `%s`(`%s`) VALUES".formatted(outputTable, outputMailColumn));
        for(var i : emails)
            sb.append("('").append(i).append("'),");
        String sql = sb.substring(0, sb.length()-1);
        log.info("SQL: " + sql);
        log.info("Updating to table '%s', adding %s record".formatted(outputTable, emails.size()));
        var stmt = conn.createStatement();
        var rs = stmt.executeUpdate(sql);
        log.info("Update to table '%s' completed, total %s record".formatted(outputTable, rs));
    }

    static String getDriverName(String dbtype) throws Exception {
        String name = dbtype.toLowerCase();
        if(name.equals("mysqldb")) return "mysql";
        if(name.equals("oracledb")) return "oracle";
        if(name.equals("mssql")) return "sqlserver";
        if(name.equals("postgresdb")) return "postgres";
        if(name.equals("mariadb")) return "mysql";
        throw new Exception("CONFIG dbtype='%s' WRONG".formatted(dbtype));
    }

    static String getWhereClause(String dbtype, String emailCol) throws Exception {
        String name = dbtype.toLowerCase();
        if(name.equals("mysqldb")) return "%s LIKE '%%@gmail.com'".formatted(emailCol);
        if(name.equals("oracledb")) return "ends_with('%s', '@gmail.com')".formatted(emailCol);
        if(name.equals("mssql")) return "%s LIKE '%%@gmail.com'".formatted(emailCol);
        if(name.equals("postgresdb")) return "%s LIKE '%%@gmail.com'".formatted(emailCol);
        if(name.equals("mariadb")) return "%s LIKE '%%@gmail.com'".formatted(emailCol);
        throw new Exception("CONFIG dbtype='%s' WRONG".formatted(dbtype));
    }

    public void graceFullShutdown() {
        synchronized (daemonTasks) {
            for (var i : daemonTasks)
                i.cancel(false);
        }
        try {
            saveEmailBatch(emails);
        } catch (Throwable t) {
            log.error("------------------ERROR----------------");
            log.error("------------------INSERT FAILED----------------");
            log.error(t.getMessage(), t);
            log.info("------------------SAVING TO DUMP FILE----------------");
            log.info("Saving %s records to file '%s'".formatted(emails.size(), dumpFile));

            try (var writer = new FileWriter(dumpFile, true)) {
                for (var i : emails)
                    writer.append(i).append('\n');
            } catch (Throwable throwable) {
                log.error("--------------------DUMP TO FILE FAILED-------------------");
                log.error(throwable.getMessage(), throwable);
                log.info("Save to log :<<<");
                log.info(emails);
            }
        }
    }
}
*/