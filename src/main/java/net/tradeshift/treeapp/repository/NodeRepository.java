package net.tradeshift.treeapp.repository;

import lombok.RequiredArgsConstructor;
import net.tradeshift.treeapp.model.Node;
import net.tradeshift.treeapp.util.JsonResultSetExtractor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.io.OutputStream;

@Repository
@RequiredArgsConstructor
public class NodeRepository {

    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * @param id: Node id to find
     * @return a single Node object based on its id;
     */
    public Node findById(String id) {
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", id);
        var sql = "SELECT id, n.parent as parentId, n.root as rootId, d.depth as height " +
                "FROM nodes n " +
                "LEFT JOIN children d ON d.ancestor = n.root AND d.descendant = :nodeId " +
                "WHERE id = :nodeId";
        try {
            return namedParameterJdbcTemplate.queryForObject(sql, parameterSource,
                    (resultSet, rowNumber) -> new Node(resultSet.getString("id"),
                            resultSet.getString("parentId"), resultSet.getString("rootId"),
                            resultSet.getInt("height")));
        } catch (EmptyResultDataAccessException e) {
            return null;
        }

    }

    /**
     * Streams all children of a given node to the provided OutputStream in JSON format
     *
     * @param id           node id
     * @param outputStream stream of children
     */
    public void streamChildrenById(String id, OutputStream outputStream) {
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", id);
        var sql = "SELECT c.descendant as \"id\", c.parent as \"parentId\", c.root as \"rootId\", " +
                "d.depth as height " +
                "FROM " +
                "children c " +
                "LEFT JOIN children d ON d.ancestor = c.root AND d.descendant = c.descendant " +
                "WHERE c.ancestor = :nodeId AND c.descendant != :nodeId;";
        namedParameterJdbcTemplate.query(sql, parameterSource, new JsonResultSetExtractor(outputStream));
    }

    /**
     * adds a single entry to the closure (children) table that links the node to itself as one of its descendants
     *
     * @param node to enter in children table
     */
    public void createChildrenTableEntry(Node node) {
        var sql = "INSERT INTO children(ancestor, descendant, depth, parent, root) " +
                "VALUES(:nodeId, :nodeId, 0, :parentId, :rootId)";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.getId())
                .addValue("parentId", node.getParentId())
                .addValue("rootId", node.getRootId());
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    public void updateChildrenTableEntry(Node node) {
        var sql = "UPDATE children " +
                "SET parent = :parentId, " +
                "root = :rootId " +
                "WHERE ancestor = :nodeId AND descendant = :nodeId AND depth = 0";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.getId())
                .addValue("parentId", node.getParentId())
                .addValue("rootId", node.getRootId());
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * add a row that links the parent to the node being moved or inserted, as well as each of its descendants as
     *
     * @param nodeId   child node id
     * @param parentId parent node id
     */
    public void addNodeToParentUpdate(String nodeId, String parentId) {
        var sql = "INSERT into children(ancestor, descendant, parent, root, depth) " +
                "SELECT p.ancestor, c.descendant, c.parent, p.root, p.depth+c.depth+1 " +
                "FROM children p, children c " +
                "WHERE p.descendant=:parentId and c.ancestor=:childId";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", nodeId);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * references the closure table to check for an entry that links the parent node to the child node
     *
     * @param parentId parent
     * @param childId  child
     * @return decision that if parent child relationship exists
     */
    public boolean isDescendantOf(String parentId, String childId) {
        var sql = "SELECT CASE WHEN EXISTS (" +
                "SELECT ancestor " +
                "FROM children " +
                "WHERE ancestor = :parentId AND descendant = :childId" +
                ") " +
                "THEN TRUE " +
                "ELSE FALSE " +
                "END as exists";

        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", childId);
        return namedParameterJdbcTemplate.queryForObject(sql, parameterSource, (resultSet, rowNumber) -> resultSet.getBoolean("exists"));
    }

    /**
     * This is the reverse of addNodeToParentUpdate; we delete all parent-descendant relationships for all parents
     * of the node and the nodes in its subtree.
     *
     * @param nodeId
     * @param parentId
     * @throws RuntimeException
     */
    public void removeNodeFromParentUpdate(String nodeId, String parentId) throws RuntimeException {
        var sql = "DELETE FROM children " +
                "USING " +
                "(SELECT link.ancestor AS ancestor, link.descendant AS descendant, link.depth AS depth, " +
                " link.parent as parent, link.root as root " +
                " FROM children p, children link, children c " +
                " WHERE p.ancestor = link.ancestor AND c.descendant = link.descendant " +
                "   AND p.descendant=:parentId AND c.ancestor=:childId) l " +
                "WHERE children.ancestor = l.ancestor AND children.descendant = l.descendant " +
                "AND children.depth = l.depth " +
                "AND children.parent = l.parent " +
                "AND children.root = l.root";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", parentId)
                .addValue("childId", nodeId);
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

    /**
     * creates a single entry in the main nodes table; this is called initially whenever a new node is created
     *
     * @param node
     * @return
     * @throws RuntimeException
     */
    public Node createNodesTableEntry(Node node) throws RuntimeException {
        var sql = "INSERT INTO nodes(id, parent, root) " +
                "VALUES (:nodeId, :parentId, :rootNodeId);";
        var parameterSource = new MapSqlParameterSource()
                .addValue("nodeId", node.getId())
                .addValue("parentId", node.getParentId())
                .addValue("rootNodeId", node.getRootId());
        KeyHolder holder = new GeneratedKeyHolder();
        namedParameterJdbcTemplate.update(sql, parameterSource, holder);
        //var resultNode = new Node();
        var newId = (String) holder.getKeys().getOrDefault("id", "-1");

        return this.findById(newId);
    }

    /**
     * updates the main nodes table with a new node and its parent relationship; this is called whenever a node is
     * moved to a new parent
     *
     * @param node
     * @throws RuntimeException
     */
    public void updateNodesTableEntry(Node node) throws RuntimeException {
        var sql = "UPDATE nodes  " +
                "SET parent = :parentId, " +
                "root = :rootId " +
                "WHERE id = :nodeId";
        var parameterSource = new MapSqlParameterSource()
                .addValue("parentId", node.getParentId())
                .addValue("nodeId", node.getId())
                .addValue("rootId", node.getRootId());
        namedParameterJdbcTemplate.update(sql, parameterSource);
    }

}
