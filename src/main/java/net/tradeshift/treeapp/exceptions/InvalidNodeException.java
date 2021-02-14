package net.tradeshift.treeapp.exceptions;

public class InvalidNodeException extends RuntimeException {
    private final String nodeId;

    public InvalidNodeException(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeId() {
        return nodeId;
    }
}
