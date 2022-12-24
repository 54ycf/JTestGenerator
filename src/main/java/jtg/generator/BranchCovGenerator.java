package jtg.generator;

import jtg.generator.util.CommonUtil;
import jtg.generator.util.PathUtil;
import soot.Unit;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;

import java.util.*;

public class BranchCovGenerator extends GeneralGenerator{
    public BranchCovGenerator(String classPath, String className, String methodName) {
        super(classPath, className, methodName);
    }

    @Override
    void init() {
        initSet = new HashSet<>(calAllBranch());
        solvableSet = new HashSet<>();
        unsolvableSet = new HashSet<>(initSet);
        testData = new HashSet<>();
    }

    @Override
    Set<List<Unit>> calAllFullCandidate(Object o) {
        List<Unit> brachPath = (List)o;
        Unit headOfPrimePath = brachPath.get(0);
        Unit tailOfPrimePath = brachPath.get(brachPath.size()-1);
        Set<List<Unit>> backwardPaths = new HashSet<>();
        Set<List<Unit>> forwardPaths = new HashSet<>();
        for (Unit head : heads) {
            PathUtil pathUtil = new PathUtil();
            pathUtil.findPath(ug, headOfPrimePath, head, new ArrayList<>(), true, new HashMap<>());
            backwardPaths.addAll(pathUtil.getSearchPathResult());
        }
        for (Unit tail : tails) {
            PathUtil pathUtil = new PathUtil();
            pathUtil.findPath(ug, tailOfPrimePath, tail, new ArrayList<>(), false, new HashMap<>());
            forwardPaths.addAll(pathUtil.getSearchPathResult());
        }
        Set<List<Unit>> result = new HashSet<>();
        if (backwardPaths.isEmpty() || forwardPaths.isEmpty()) {
            return result; //没有完整路径，返回空
        }
        for (List<Unit> backwardPath : backwardPaths) {
            Collections.reverse(backwardPath); //这个地方注意，每次得复制一下
            for (List<Unit> forwardPath : forwardPaths) {
                List<Unit> backwardPathCopy = new ArrayList<>(backwardPath);
                List<Unit> forwardPathCopy = new ArrayList<>(forwardPath); //注意不能直接操作
                backwardPathCopy.remove(backwardPathCopy.size()-1); //首尾重合了
                backwardPathCopy.addAll(brachPath);
                forwardPathCopy.remove(0);
                backwardPathCopy.addAll(forwardPathCopy);//拼接到一起
                result.add(backwardPathCopy);
            }
        }
        return result;
    }

    @Override
    void checkCov(List<Unit> fullPath) {
        for (Object o : unsolvableSet) {
            List<Unit> branchPath = (List<Unit>) o;
            if (CommonUtil.leftIsSubList(branchPath, fullPath)) {
                solvableSet.add(branchPath);
            }
        }
        unsolvableSet.removeAll(solvableSet);
    }

    /**计算所有的分支
     * @return
     */
    private Set<List<Unit>> calAllBranch(){
        BlockGraph bg = new BriefBlockGraph(body);
        Set<List<Unit>> result = new HashSet<>();
        for (Block block : bg.getBlocks()) {
            for (Block suc : bg.getSuccsOf(block)) {
                List<Unit> head = PathUtil.transferB2U(block);
                head.addAll(PathUtil.transferB2U(suc));
                result.add(head);
            }

        }
        return result;
    }
}
