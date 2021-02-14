package net.tradeshift.treeapp.model;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class Node {
    private String id;
    private String parentId;
    private String rootId;
    private int height;
}
