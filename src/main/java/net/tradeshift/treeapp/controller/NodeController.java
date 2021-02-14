package net.tradeshift.treeapp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.tradeshift.treeapp.exceptions.CyclicalTreeStructureException;
import net.tradeshift.treeapp.exceptions.InvalidNodeException;
import net.tradeshift.treeapp.exceptions.MoveAttemptToSelfException;
import net.tradeshift.treeapp.exceptions.NodeAlreadyExistsException;
import net.tradeshift.treeapp.exceptions.RequiredFieldException;
import net.tradeshift.treeapp.model.Node;
import net.tradeshift.treeapp.service.NodeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/node")
@Slf4j
public class NodeController {

    private final NodeService nodeService;


    /**
     * Returns all children of the specified node.
     *
     * @param nodeId   node id
     * @param response stream of children list
     * @return stream of nodes
     */
    @GetMapping("/{id}/children")
    public ResponseEntity<StreamingResponseBody> getAllChildren(@PathVariable("id") String nodeId,
                                                                final HttpServletResponse response) {
        response.setContentType("application/json");
        StreamingResponseBody stream = out -> {
            try (OutputStream o = response.getOutputStream()) {
                nodeService.streamDescendantsById(nodeId, o);
            } catch (final RuntimeException e) {
                log.error("Exception while streaming data {}", e.getLocalizedMessage());
            }
        };
        return ResponseEntity.ok(stream);
    }

    @PostMapping("/")
    @ResponseStatus(HttpStatus.CREATED)
    public Node createNode(@RequestBody Node node) {
        // we don't allow blank node ID's
        if (node.getId().isBlank() || node.getParentId().isBlank() || node.getRootId().isBlank()) {
            throw new RequiredFieldException("node id, parentId and rootId ");
        }
        return nodeService.createNode(node);
    }

    @GetMapping("/{id}")
    public Node getNode(@PathVariable("id") String nodeId) {
        Node node = nodeService.findById(nodeId);
        if (node == null) {
            throw new InvalidNodeException(nodeId);
        }
        return node;
    }

    /**
     * changes the parent node of any node in the tree to a new parent
     *
     * @param nodeId      node
     * @param newParentId new parent
     */
    @PutMapping("/{id}/moveNode/{newParentId}")
    @ResponseStatus(HttpStatus.OK)
    public void moveNode(@PathVariable("id") String nodeId, @PathVariable("newParentId") String newParentId) {
        nodeService.moveNode(nodeId, newParentId);
    }

    @ExceptionHandler(CyclicalTreeStructureException.class)
    public final ResponseEntity<String> handleAllExceptions(CyclicalTreeStructureException ex) {
        return ResponseEntity.status(HttpStatus.LOOP_DETECTED).body("You may not move a node to one of its child");
    }

    @ExceptionHandler(InvalidNodeException.class)
    public final ResponseEntity<String> handleAllExceptions(InvalidNodeException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format("The specified node %s does not exist", e.getNodeId()));
    }

    @ExceptionHandler(MoveAttemptToSelfException.class)
    public final ResponseEntity<String> handleAllExceptions(MoveAttemptToSelfException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("You can not move a node to itself");
    }

    @ExceptionHandler(NodeAlreadyExistsException.class)
    public final ResponseEntity<String> handleAllExceptions(NodeAlreadyExistsException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body("A node with this ID already exists");
    }

    @ExceptionHandler(RequiredFieldException.class)
    public final ResponseEntity<String> handleAllExceptions(RequiredFieldException e) {
        return ResponseEntity.badRequest().body(String.format("%s is a required field", e.getFieldName()));
    }

}
