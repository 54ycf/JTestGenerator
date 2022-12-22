package jtg.generator.path;

import jtg.generator.util.CommonUtil;
import jtg.generator.util.PathUtil;
import jtg.graphics.SootCFG;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;

import java.util.*;

public class DDLPrimePathCal {
    private String clsPath;
    private String clsName;
    private String mtdName;
    private BlockGraph bg;
    public DDLPrimePathCal(String classPath, String className, String methodName) {
        this.clsPath = classPath;
        this.clsName = className;
        this.mtdName = methodName;
        bg = SootCFG.getSimpleCFG(clsPath, clsName, mtdName);
    }

    public Set<List<Unit>> generatePrimePathUnit(){
        Set<List<Unit>> result = new HashSet<>();
        List<List<Block>> primePathBlock = generatePrimePathBlock();
        for (List<Block> blocks : primePathBlock) {
            result.add(PathUtil.transferBp2Up(blocks));
        }
        return result;
    }

    public List<List<Block>> generatePrimePathBlock(){
        System.out.println("========================Blocks=====================");
        System.out.println(bg);
        System.out.println("===================================================");
        System.out.println();
        List<NodeTree> nodeTrees = new ArrayList<>(); //存放了所有的节点树
        for (Block start : bg) {
            if (bg.getTails().contains(start)) //终结点不搞
                continue;
            NodeTree nodeTree = new NodeTree(start); //每个节点都生成一个节点树
            for (Block suc : bg.getSuccsOf(start)){
                generateNodeTree(suc, nodeTree, start);
            }
            nodeTrees.add(nodeTree);
        }
        List<List<Block>> primePathBlocks = new ArrayList<>();
        for (NodeTree nodeTree : nodeTrees) {
            dfsNodeTree(nodeTree, new ArrayList<>(), primePathBlocks);
        }

        for (int i = 0; i < primePathBlocks.size(); i++) {
            List<Block> path = primePathBlocks.get(i);
            System.out.println("路径" + i);
            path.forEach(item -> System.out.print(item.getIndexInMethod()+1+ "  "));
            System.out.println();
        }
        return primePathBlocks;
    }


    private void generateNodeTree(Block start, NodeTree fatherTree, Block rootNode){
        if (fatherTree.fatherLinkNotContains(start)) {
            NodeTree sonTree = new NodeTree(start);
            sonTree.setFather(fatherTree);
            fatherTree.getSons().add(sonTree);
            for (Block suc : bg.getSuccsOf(start)) {
                generateNodeTree(suc, sonTree, rootNode);
            }
        } else if (start.equals(rootNode)) { //最多只能和根节点重复，重复之后就不能继续往下走了
            NodeTree sonTree = new NodeTree(start);
            sonTree.setFather(fatherTree);
            fatherTree.getSons().add(sonTree);
        }
    }

    private void dfsNodeTree(NodeTree root, List<Block> onePath, List<List<Block>> result) {
        onePath.add(root.getNode());
        if (root.getSons().isEmpty()) {//递归到叶子
            int i;
            for (i = 0; i < result.size(); i++) {            //检测这个新的路径是否是已经存在的某一条子路径
                List<Block> blocks = result.get(i);
                if (CommonUtil.leftIsSubList(onePath, blocks)) {
                    System.out.println("这条路是原来的一条子路，没有加入");
                    System.out.println("老路是：");
                    blocks.forEach(item -> System.out.print(item.getIndexInMethod()+1+ "  "));
                    System.out.println();
                    System.out.println("新路是");
                    onePath.forEach(item -> System.out.print(item.getIndexInMethod()+1+ "  "));                    System.out.println();
                    break;
                }
                if (CommonUtil.leftIsSubList(blocks, onePath)) {            //检测里面某一条是否是这一条新的路径的子路径
                    System.out.println("老路是新路的子路径，删除老路");
                    System.out.println("老路是：");
                    blocks.forEach(item -> System.out.print(item.getIndexInMethod()+1+ "  "));
                    System.out.println();
                    System.out.println("新路是");
                    onePath.forEach(item -> System.out.print(item.getIndexInMethod()+1+ "  "));
                    System.out.println();
                    result.remove(blocks);
                    i = result.size();
                    break;
                }
            }
            if (i == result.size())
                result.add(onePath);
            return;
        }
        for (NodeTree son : root.getSons()) {
            List<Block> onePathClone = new ArrayList<>(onePath);
            dfsNodeTree(son, onePathClone, result);
        }
    }
}
