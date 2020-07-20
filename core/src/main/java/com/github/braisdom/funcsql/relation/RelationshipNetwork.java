package com.github.braisdom.funcsql.relation;

import com.github.braisdom.funcsql.Database;
import com.github.braisdom.funcsql.SQLExecutor;
import com.github.braisdom.funcsql.SQLGenerator;
import com.github.braisdom.funcsql.Table;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RelationshipNetwork {

    private final Connection connection;
    private final Class baseClass;

    private final Map<Class, List> relationObjectsMap;

    public RelationshipNetwork(Connection connection, Class baseClass) {
        this.connection = connection;
        this.baseClass = baseClass;

        this.relationObjectsMap = new HashMap<>();
    }

    public void process(List rows, Relationship[] relationships) {
        Relationship sourceRelationship = getRelation(baseClass, relationships);

        catchObjects(baseClass, wrapObject(sourceRelationship, rows));
        setupAssociatedObjects(baseClass, sourceRelationship, relationships);
    }

    private void setupAssociatedObjects(Class baseClass, Relationship relationship, Relationship[] relationships) {
        List<RelationalFieldAccessor> baseObjects = getCachedObjects(baseClass);
        Map<Object, List<RelationalFieldAccessor>> groupedObjects = baseObjects.stream()
                .collect(Collectors.groupingBy(RelationalFieldAccessor::getRelationalValue));

        Class childClass = relationship.getRelatedClass();
        Relationship childRelationship = (Relationship) Arrays.stream(relationships)
                .filter(r -> r.getBaseClass().equals(childClass)).toArray()[0];
        if(childRelationship != null)
            setupAssociatedObjects(childClass, relationship, relationships);
    }

    protected List<RelationalFieldAccessor> wrapObject(Relationship relationship, List objects) {
        return (List<RelationalFieldAccessor>) objects.stream()
                .map(o -> new RelationalFieldAccessor(relationship, o))
                .collect(Collectors.toList());
    }

    protected List queryObjects(Class clazz, Relationship relationship, String associatedKey,
                                List associatedValues) throws SQLException {
        SQLExecutor sqlExecutor = Database.getSqlExecutor();
        String relationTableName = Table.getTableName(clazz);

        SQLGenerator sqlGenerator = Database.getSQLGenerator();

        String relationConditions = relationship.getRelationCondition() == null
                ? String.format(" %s IN (%s) ", associatedKey, associatedValues.toArray())
                : String.format(" %s IN (%s) AND (%s)", associatedKey, quote(associatedValues.toArray()),
                    relationship.getRelationCondition());
        String relationTableQuerySql = sqlGenerator.createQuerySQL(relationTableName, null, relationConditions);

        return sqlExecutor.query(connection, relationTableQuerySql, clazz);
    }

    protected boolean isObjectLoaded(Class clazz) {
        return this.relationObjectsMap.containsKey(clazz);
    }

    protected List getCachedObjects(Class clazz) {
        return this.relationObjectsMap.get(clazz);
    }

    protected void catchObjects(Class clazz, List objects) {
        this.relationObjectsMap.put(clazz, objects);
    }

    private Relationship getRelation(Class clazz, Relationship[] relationships) {
        Relationship[] filteredRelations = (Relationship[]) Arrays.stream(relationships)
                .filter(r -> r.getBaseClass().equals(clazz)).toArray();
        if(filteredRelations.length > 0)
            return filteredRelations[0];
        return null;
    }

    private String quote(Object... values) {
        StringBuilder sb = new StringBuilder();

        for (Object value : values) {
            if (value instanceof Integer || value instanceof Long ||
                    value instanceof Float || value instanceof Double)
                sb.append(String.valueOf(value));
            else
                sb.append(String.format("'%s'", String.valueOf(value)));
            sb.append(",");
        }
        sb.delete(sb.length() - 1, sb.length());
        return sb.toString();
    }

    private String encodeGroupKey(Class clazz, String fieldName) {
        return String.format("%s_%s", clazz.getName(), fieldName);
    }
}
