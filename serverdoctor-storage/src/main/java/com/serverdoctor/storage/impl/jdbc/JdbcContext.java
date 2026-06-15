package com.serverdoctor.storage.impl.jdbc;

import javax.sql.DataSource;

/** Hält die gepoolte DataSource plus das aktive SQL-Dialekt. */
final class JdbcContext {

    final DataSource dataSource;
    final JdbcDialect dialect;

    JdbcContext(DataSource dataSource, JdbcDialect dialect) {
        this.dataSource = dataSource;
        this.dialect = dialect;
    }

}
