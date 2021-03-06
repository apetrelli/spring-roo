package org.springframework.roo.addon.dbre.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.roo.support.util.StringUtils;

/**
 * Creates a {@link Database database} model from a live database using JDBC.
 * 
 * @author Alan Stewart
 * @since 1.1
 */
public class DatabaseIntrospector extends AbstractIntrospector {

    // Fields
    private final Set<Schema> schemas;
    private final boolean view;
    private final Set<String> includeTables;
    private final Set<String> excludeTables;

    public DatabaseIntrospector(final Connection connection,
            final Set<Schema> schemas, final boolean view,
            final Set<String> includeTables, final Set<String> excludeTables)
            throws SQLException {
        super(connection);
        this.schemas = schemas;
        this.view = view;
        this.includeTables = includeTables;
        this.excludeTables = excludeTables;
    }

    public Database createDatabase() throws SQLException {
        Set<Table> tables = new LinkedHashSet<Table>();
        for (Schema schema : schemas) {
            tables.addAll(getTables(schema));
        }
        return new Database(tables);
    }

    private Set<Table> getTables(final Schema schema) throws SQLException {
        Set<Table> tables = new LinkedHashSet<Table>();

        String[] types = view ? new String[] { TableType.TABLE.name(),
                TableType.VIEW.name() }
                : new String[] { TableType.TABLE.name() };
        ResultSet rs = databaseMetaData.getTables(null,
                getArtifact(schema.getName()), null, types);
        try {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");

                // Check for certain tables such as Oracle recycle bin tables,
                // and ignore
                if (ignoreTables(tableName)) {
                    continue;
                }

                if (hasIncludedTable(tableName) && !hasExcludedTable(tableName)) {
                    Table table = new Table(tableName, new Schema(
                            rs.getString("TABLE_SCHEM")));
                    table.setCatalog(rs.getString("TABLE_CAT"));
                    table.setDescription(rs.getString("REMARKS"));

                    readColumns(table);
                    readForeignKeys(table, false);
                    readForeignKeys(table, true);
                    readIndices(table);

                    for (String columnName : readPrimaryKeyNames(table)) {
                        Column column = table.findColumn(columnName);
                        if (column != null) {
                            column.setPrimaryKey(true);
                        }
                    }

                    tables.add(table);
                }
            }
        }
        finally {
            rs.close();
        }

        return tables;
    }

    private boolean ignoreTables(final String tableName) {
        boolean ignore = false;
        try {
            if ("Oracle".equalsIgnoreCase(databaseMetaData
                    .getDatabaseProductName()) && tableName.startsWith("BIN$")) {
                ignore = true;
            }
            if ("MySQL".equalsIgnoreCase(databaseMetaData
                    .getDatabaseProductName()) && tableName.equals("SEQUENCE")) {
                ignore = true;
            }
        }
        catch (SQLException ignored) {
        }
        return ignore;
    }

    private void readColumns(final Table table) throws SQLException {
        ResultSet rs = databaseMetaData.getColumns(table.getCatalog(), table
                .getSchema().getName(), table.getName(), null);
        try {
            while (rs.next()) {
                Column column = new Column(rs.getString("COLUMN_NAME"),
                        rs.getInt("DATA_TYPE"), rs.getString("TYPE_NAME"),
                        rs.getInt("COLUMN_SIZE"), rs.getInt("DECIMAL_DIGITS"));
                column.setDescription(rs.getString("REMARKS"));
                column.setDefaultValue(rs.getString("COLUMN_DEF"));
                column.setRequired("NO".equalsIgnoreCase(rs
                        .getString("IS_NULLABLE")));

                table.addColumn(column);
            }
        }
        finally {
            rs.close();
        }
    }

    private void readForeignKeys(final Table table, final boolean exported)
            throws SQLException {
        Map<String, ForeignKey> foreignKeys = new LinkedHashMap<String, ForeignKey>();

        ResultSet rs;
        if (exported) {
            rs = databaseMetaData.getExportedKeys(table.getCatalog(), table
                    .getSchema().getName(), table.getName());
        }
        else {
            rs = databaseMetaData.getImportedKeys(table.getCatalog(), table
                    .getSchema().getName(), table.getName());
        }

        try {
            while (rs.next()) {
                String name = rs.getString("FK_NAME");
                String foreignTableName = rs
                        .getString(exported ? "FKTABLE_NAME" : "PKTABLE_NAME");
                String key = name + "_" + foreignTableName;

                if (!hasExcludedTable(foreignTableName)) {
                    ForeignKey foreignKey = new ForeignKey(name,
                            foreignTableName);
                    foreignKey.setForeignSchemaName(StringUtils.defaultIfEmpty(
                            rs.getString(exported ? "FKTABLE_SCHEM"
                                    : "PKTABLE_SCHEM"),
                            DbreModelService.NO_SCHEMA_REQUIRED));
                    foreignKey.setOnUpdate(getCascadeAction(rs
                            .getShort("UPDATE_RULE")));
                    foreignKey.setOnDelete(getCascadeAction(rs
                            .getShort("DELETE_RULE")));
                    foreignKey.setExported(exported);

                    String localColumnName = rs
                            .getString(exported ? "PKCOLUMN_NAME"
                                    : "FKCOLUMN_NAME");
                    String foreignColumnName = rs
                            .getString(exported ? "FKCOLUMN_NAME"
                                    : "PKCOLUMN_NAME");
                    Reference reference = new Reference(localColumnName,
                            foreignColumnName);

                    if (foreignKeys.containsKey(key)) {
                        foreignKeys.get(key).addReference(reference);
                    }
                    else {
                        foreignKey.addReference(reference);
                        foreignKeys.put(key, foreignKey);
                    }
                }
            }
        }
        finally {
            rs.close();
        }

        for (ForeignKey foreignKey : foreignKeys.values()) {
            if (exported) {
                table.addExportedKey(foreignKey);
            }
            else {
                table.addImportedKey(foreignKey);
            }
        }
    }

    private CascadeAction getCascadeAction(final Short actionValue) {
        CascadeAction cascadeAction;
        switch (actionValue.intValue()) {
        case DatabaseMetaData.importedKeyCascade:
            cascadeAction = CascadeAction.CASCADE;
            break;
        case DatabaseMetaData.importedKeySetNull:
            cascadeAction = CascadeAction.SET_NULL;
            break;
        case DatabaseMetaData.importedKeySetDefault:
            cascadeAction = CascadeAction.SET_DEFAULT;
            break;
        case DatabaseMetaData.importedKeyRestrict:
            cascadeAction = CascadeAction.RESTRICT;
            break;
        case DatabaseMetaData.importedKeyNoAction:
            cascadeAction = CascadeAction.NONE;
            break;
        default:
            cascadeAction = CascadeAction.NONE;
        }
        return cascadeAction;
    }

    private boolean hasIncludedTable(final String tableName) {
        if (includeTables == null || includeTables.isEmpty()) {
            return true;
        }
        return hasTable(includeTables, tableName);
    }

    private boolean hasExcludedTable(final String tableName) {
        if (excludeTables == null || excludeTables.isEmpty()) {
            return false;
        }
        return hasTable(excludeTables, tableName);
    }

    private boolean hasTable(final Set<String> tables, final String tableName) {
        for (String table : tables) {
            String regex = table.replaceAll("\\*", ".*")
                    .replaceAll("\\?", ".?");
            Pattern pattern = Pattern.compile(regex);
            if (pattern.matcher(tableName).matches()) {
                return true;
            }
        }
        return false;
    }

    private void readIndices(final Table table) throws SQLException {
        Set<Index> indices = new LinkedHashSet<Index>();

        ResultSet rs;
        try {
            // Catching SQLException here due to Oracle throwing exception when
            // attempting to retrieve indices for deleted tables that exist in
            // Oracle's recycle bin
            rs = databaseMetaData.getIndexInfo(table.getCatalog(), table
                    .getSchema().getName(), table.getName(), false, false);
        }
        catch (SQLException e) {
            return;
        }

        if (rs != null) {
            try {
                while (rs.next()) {
                    Short type = rs.getShort("TYPE");
                    if (type == DatabaseMetaData.tableIndexStatistic) {
                        continue;
                    }

                    String indexName = rs.getString("INDEX_NAME");
                    Index index = findIndex(indexName, indices);
                    if (index == null) {
                        index = new Index(indexName);
                    }
                    else {
                        indices.remove(index);
                    }
                    index.setUnique(!rs.getBoolean("NON_UNIQUE"));

                    IndexColumn indexColumn = new IndexColumn(
                            rs.getString("COLUMN_NAME"));
                    index.addColumn(indexColumn);

                    indices.add(index);
                }
            }
            finally {
                rs.close();
            }
        }

        for (Index index : indices) {
            table.addIndex(index);
        }
    }

    private Index findIndex(final String name, final Set<Index> indices) {
        for (Index index : indices) {
            if (index.getName().equalsIgnoreCase(name)) {
                return index;
            }
        }
        return null;
    }

    private Set<String> readPrimaryKeyNames(final Table table)
            throws SQLException {
        Set<String> columnNames = new LinkedHashSet<String>();

        ResultSet rs = databaseMetaData.getPrimaryKeys(table.getCatalog(),
                table.getSchema().getName(), table.getName());
        try {
            while (rs.next()) {
                columnNames.add(rs.getString("COLUMN_NAME"));
            }
        }
        finally {
            rs.close();
        }

        return columnNames;
    }

    private String getArtifact(final String artifactName) throws SQLException {
        if (databaseMetaData.storesLowerCaseIdentifiers()) {
            return StringUtils.toLowerCase(artifactName);
        }
        else if (databaseMetaData.storesUpperCaseIdentifiers()) {
            return StringUtils.toUpperCase(artifactName);
        }
        else {
            return artifactName;
        }
    }
}
