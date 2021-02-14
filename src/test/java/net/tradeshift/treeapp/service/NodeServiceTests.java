package net.tradeshift.treeapp.service;

import net.tradeshift.treeapp.exceptions.CyclicalTreeStructureException;
import net.tradeshift.treeapp.exceptions.InvalidNodeException;
import net.tradeshift.treeapp.exceptions.MoveAttemptToSelfException;
import net.tradeshift.treeapp.model.Node;
import net.tradeshift.treeapp.repository.NodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;


@ExtendWith(SpringExtension.class)
public class NodeServiceTests {

    private static final String BASIC_TREE = "[" +
            "{\"id\":\"c\",\"parentId\":\"a\",\"rootId\":\"root\",\"height\":2}," +
            "{\"id\":\"d\",\"parentId\":\"a\",\"rootId\":\"root\",\"height\":2}," +
            "{\"id\":\"e\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
            "{\"id\":\"f\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
            "{\"id\":\"g\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}" +
            "]";
    NodeService nodeService;
    Node rootNode;
    // define nodes
    Node aNode;
    Node bNode;
    Node cNode;

    @Mock
    private NodeRepository repository;

    @BeforeEach
    void setUp() {
        nodeService = new NodeService(repository);
        rootNode = new Node("root", null, "root", 0);
        aNode = new Node("a", "root", "root", 1);
        bNode = new Node("b", "root", "root", 1);
        cNode = new Node("c", "a", "root", 2);
    }

    @Test
    public void getDescendants() throws IOException {
        // mock repository response
        when(repository.findById("a")).thenReturn(null, aNode);
        when(repository.findById("root")).thenReturn(rootNode);
        when(repository.createNodesTableEntry(aNode)).thenReturn(aNode);
        doNothing().when(repository).createChildrenTableEntry(any(Node.class));
        doNothing().when(repository).addNodeToParentUpdate(anyString(), anyString());

        nodeService.createNode(aNode);

        /*
         *  consider created nodes by mocking response
         *  response of repository is mocked for further data
         */

        var os = new ByteArrayOutputStream();
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            ByteArrayOutputStream output_value = invocation.getArgument(1);
            output_value.write(BASIC_TREE.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(repository).streamChildrenById("a", os);

        nodeService.streamDescendantsById("a", os);
        os.close();
        String expectedOutput = "[" +
                "{\"id\":\"c\",\"parentId\":\"a\",\"rootId\":\"root\",\"height\":2}," +
                "{\"id\":\"d\",\"parentId\":\"a\",\"rootId\":\"root\",\"height\":2}," +
                "{\"id\":\"e\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
                "{\"id\":\"f\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
                "{\"id\":\"g\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}" +
                "]";

        assertEquals(expectedOutput, os.toString());
    }

    @Test
    public void moveNode() throws IOException {
        /* consider nodes present in db, mocking response */

        // call to /api/v1/moveNode/c/b  -  moves node c (and its subtree) from parent a to to parent b
        when(repository.isDescendantOf("c", "b")).thenReturn(false);
        when(repository.findById(anyString())).thenReturn(cNode, bNode);
        doNothing().when(repository).removeNodeFromParentUpdate(anyString(), anyString());
        doNothing().when(repository).updateNodesTableEntry(any(Node.class));
        doNothing().when(repository).updateChildrenTableEntry(any(Node.class));
        doNothing().when(repository).addNodeToParentUpdate(anyString(), anyString());

        // b is the new parent of C
        nodeService.moveNode("c", "b");

        // first check the previous parent to make sure it no longer has the moved subtree
        var osTwo = new ByteArrayOutputStream();
        String expectedOutputTwo = "[{\"id\":\"d\",\"parentId\":\"a\",\"rootId\":\"root\",\"height\":2}]";

        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            ByteArrayOutputStream output_value = invocation.getArgument(1);
            output_value.write(expectedOutputTwo.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(repository).streamChildrenById("a", osTwo);

        nodeService.streamDescendantsById("a", osTwo);
        osTwo.close();

        assertEquals(expectedOutputTwo, osTwo.toString());

        // now check the new parent to make sure it has the moved subtree
        var osThree = new ByteArrayOutputStream();
        String expectedOutputThree = "[" +
                "{\"id\":\"c\",\"parentId\":\"b\",\"rootId\":\"root\",\"height\":2}," +
                "{\"id\":\"e\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
                "{\"id\":\"f\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}," +
                "{\"id\":\"g\",\"parentId\":\"c\",\"rootId\":\"root\",\"height\":3}" +
                "]";
        doAnswer(invocation -> {
            String id = invocation.getArgument(0);
            ByteArrayOutputStream output_value = invocation.getArgument(1);
            output_value.write(expectedOutputThree.getBytes(StandardCharsets.UTF_8));
            return null;
        }).when(repository).streamChildrenById("b", osThree);
        nodeService.streamDescendantsById("b", osThree);
        osThree.close();
        assertEquals(expectedOutputThree, osThree.toString());
    }

    @Test
    public void createNode() {
        when(repository.findById("a")).thenReturn(null, aNode, aNode);
        when(repository.findById("root")).thenReturn(rootNode);
        when(repository.createNodesTableEntry(aNode)).thenReturn(aNode);
        doNothing().when(repository).createChildrenTableEntry(any(Node.class));
        doNothing().when(repository).addNodeToParentUpdate(anyString(), anyString());
        nodeService.createNode(aNode);
        Node result = nodeService.findById("a");
        assertEquals("a", result.getId());
    }

    @Test
    public void findById() {
        when(repository.findById("c")).thenReturn(cNode);
        Node n = nodeService.findById("c");
        assertNotNull(n);
        assertEquals("c", n.getId());
        assertEquals("a", n.getParentId());
        assertEquals(2, n.getHeight());
        assertEquals("root", n.getRootId());

    }

    @Test
    public void isDescendantOf() {
        when(repository.isDescendantOf(anyString(), anyString())).thenReturn(true, false);
        assertTrue(nodeService.isDescendantOf("root", "c"));
        assertFalse(nodeService.isDescendantOf("b", "c"));
    }

    @Test
    public void noCycleAllowed() {
        // mock repository so c is child of a
        when(repository.isDescendantOf("a", "c")).thenReturn(true);
        assertThrows(CyclicalTreeStructureException.class, () -> nodeService.moveNode("a", "c"));
    }

    @Test
    public void invalidNodeMove() {
        // mock repository so node b does not exists
        when(repository.isDescendantOf("a", "b")).thenReturn(false);
        when(repository.findById("a")).thenReturn(aNode, null);
        when(repository.findById("b")).thenReturn(null, bNode);
        assertThrows(InvalidNodeException.class, () -> nodeService.moveNode("a", "b"), "new Parent is not present");
        assertThrows(InvalidNodeException.class, () -> nodeService.moveNode("a", "b"), "Node itself isn't present");
    }

    @Test
    public void invalidNodeMoveToSelf() {
        assertThrows(MoveAttemptToSelfException.class, () -> nodeService.moveNode("a", "a"));
    }


    @Test
    public void createWithInvalidRootParent() {
        when(repository.findById("root")).thenReturn(null, null);
        when(repository.findById("az")).thenReturn(null, null);
        when(repository.findById("ax")).thenReturn(rootNode);
        // invalid parent in mock
        assertThrows(InvalidNodeException.class, () -> nodeService.createNode(Node.builder().id("az").parentId("root").rootId("ax").build()));
        // invalid root in mock
        assertThrows(InvalidNodeException.class, () -> nodeService.createNode(Node.builder().id("az").parentId("ax").rootId("root").build()));
    }
}
