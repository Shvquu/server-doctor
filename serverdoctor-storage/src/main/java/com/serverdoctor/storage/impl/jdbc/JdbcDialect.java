package com.serverdoctor.storage.impl.jdbc;

/**
 * Die einzigen Unterschiede zwischen den unterstützten SQL-Servern: Treiberklasse,
 * Auto-Increment-Primärschlüssel und Gleitkommatyp. INSERT-/SELECT-Statements sind
 * identisch (positionsbasierte {@code ?}-Parameter) und werden in den Repositories
 * wortgleich zur SQLite-Variante wiederverwendet.
 */
enum JdbcDialect {

    POSTGRES("org.postgresql.Driver", "BIGSERIAL PRIMARY KEY", "DOUBLE PRECISION", "serverdoctor-postgres"),
    MARIADB("org.mariadb.jdbc.Driver", "BIGINT AUTO_INCREMENT PRIMARY KEY", "DOUBLE", "serverdoctor-mariadb");

    final String driverClassName;
    final String serialPrimaryKey;
    final String doubleType;
    final String poolName;

    JdbcDialect(String driverClassName, String serialPrimaryKey, String doubleType, String poolName) {
        this.driverClassName = driverClassName;
        this.serialPrimaryKey = serialPrimaryKey;
        this.doubleType = doubleType;
        this.poolName = poolName;
    }
}
