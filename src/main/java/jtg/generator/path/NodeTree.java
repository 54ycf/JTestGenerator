package jtg.generator.path;

import soot.toolkits.graph.Block;

import java.util.ArrayList;
import java.util.List;

public class NodeTree {
    private Block node;
    private List<NodeTree> sons = new ArrayList<>();
    private NodeTree father;

    public NodeTree() {
        this.node = null;
        this.father = null;
    }

    public NodeTree(Block node) {
        this.node = node;
    }

    //检测这一条从根到叶子的节点是否包含这个Block节点
    public boolean fatherLinkNotContains(Block block) {
        return !node.equals(block) && (father==null || father.fatherLinkNotContains(block));
    }

    //检测another是否是this的子树
//    public boolean isSuperTreeOf(NodeTree another) {
//        if (this.equals(another))
//            return true;
//        for (NodeTree son : sons) {
//            if (son.equals(another))
//                return true;
//        }
//        return false;
//    }

    public Block getNode() {
        return node;
    }

    public void setNode(Block node) {
        this.node = node;
    }

    public List<NodeTree> getSons() {
        return sons;
    }

    public void setSons(List<NodeTree> sons) {
        this.sons = sons;
    }

    public NodeTree getFather() {
        return father;
    }

    public void setFather(NodeTree father) {
        this.father = father;
    }


//    @Override
//    public boolean equals(Object obj) { //广义等
//        if (!(obj instanceof NodeTree)) return false;
//        NodeTree another = (NodeTree) obj;
//        return this.node.equals(another.node)
//                && this.sons.equals(another.sons)
//                && (this.father==null && another.father==null || this.father.equals(another.father));
//    }
//
//    @Override
//    public String toString() {
//        return node.toString() + "  -> \n" + sons.toString();
//    }
}
