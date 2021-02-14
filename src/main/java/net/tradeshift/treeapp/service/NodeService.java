package net.tradeshift.treeapp.service;

import lombok.RequiredArgsConstructor;
import net.tradeshift.treeapp.exceptions.CyclicalTreeStructureException;
import net.tradeshift.treeapp.exceptions.InvalidNodeException;
import net.tradeshift.treeapp.exceptions.MoveAttemptToSelfException;
import net.tradeshift.treeapp.exceptions.NodeAlreadyExistsException;
import net.tradeshift.treeapp.model.Node;
import net.tradeshift.treeapp.repository.NodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;

@Service
@RequiredArgsConstructor
public class NodeService {

    private final NodeRepository nodeRepository;

    @Transactional
    public Node createNode(Node node) {
        // first, ensure that a node with this id does not already exist
        Node existingNode = nodeRepository.findById(node.getId());
        if (existingNode != null) {
            throw new NodeAlreadyExistsException();
        }
        // now, ensure that the parent node and root node of this node exist
        Node parentNode = nodeRepository.findById(node.getParentId());
        if (parentNode == null) {
            throw new InvalidNodeException(node.getParentId());
        }
        Node rootNode = nodeRepository.findById(node.getRootId());
        if (rootNode == null || (!parentNode.getRootId().equals(rootNode.getId()))) {
            throw new InvalidNodeException(node.getRootId());
        }
        Node resultNode = nodeRepository.createNodesTableEntry(node);
        nodeRepository.createChildrenTableEntry(resultNode);
        nodeRepository.addNodeToParentUpdate(resultNode.getId(), resultNode.getParentId());
        // in order to calculate height, we re-load the Node from the repo
        return nodeRepository.findById(resultNode.getId());
    }

    /**
     * changes the parent of any node to any other valid node
     *
     * @param nodeId      node
     * @param newParentId new parent
     */
    @Transactional
    public void moveNode(String nodeId, String newParentId) {
        // first, make sure we are moving a valid node to another valid node
        // moving a node to itself is not allowed
        if (nodeId.equals(newParentId)) {
            throw new MoveAttemptToSelfException();
        }
        // moving a node to one of its descendants is not allowed
        if (nodeRepository.isDescendantOf(nodeId, newParentId)) {
            throw new CyclicalTreeStructureException();
        }
        Node node = nodeRepository.findById(nodeId);
        if (node == null) {
            throw new InvalidNodeException(nodeId);
        }
        Node newParent = nodeRepository.findById(newParentId);
        if (newParent == null) {
            throw new InvalidNodeException(newParentId);
        }
        String oldParentId = node.getParentId();
        node.setParentId(newParentId);

        //first, update the closure table to remove the parent-descendant relationships for all affected nodes
        nodeRepository.removeNodeFromParentUpdate(node.getId(), oldParentId);
        //then, update the main nodes table with the new parent
        nodeRepository.updateNodesTableEntry(node);
        //now, update the one record in the children table that references the node as its own descendant
        nodeRepository.updateChildrenTableEntry(node);
        //lastly, update the closure table with new parent-descendant entries for the moved node
        nodeRepository.addNodeToParentUpdate(nodeId, newParentId);
    }

    public Node findById(String id) {
        return nodeRepository.findById(id);
    }

    public void streamDescendantsById(String id, OutputStream outputStream) {
        nodeRepository.streamChildrenById(id, outputStream);
    }

    public boolean isDescendantOf(String parentId, String childId) {
        return nodeRepository.isDescendantOf(parentId, childId);
    }

}
