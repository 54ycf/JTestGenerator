package jtg.generator;

import jtg.generator.path.DDLPrimePathCal;
import jtg.generator.util.CommonUtil;
import jtg.generator.util.PathUtil;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.*;

/**
 * 思路：
 * 从最长的基路径开始搞
 * 向前DFS到开始节点结束
 * 向后DFS到结束节点，得到完整路径，求解约束
 * 如果有解，完整路径+1，测试数据+1
 * 如果无解，继续DFS，找到的Path数量+1
 * 重复以上两步骤，直到Path数量==N，放弃，并告诉此基路径不可解
 *
 * 继续搞基路径，如果这条基路径在得到的路径里是某条完整路径子路径，则证明已经搞定，否则继续搞
 */
public class PrimePathCovGenerator extends GeneralGenerator{

    public PrimePathCovGenerator(String classPath, String className, String methodName) {
        super(classPath, className, methodName);
    }

    @Override
    void init() {
        DDLPrimePathCal primePathCal = new DDLPrimePathCal(body);
        initSet = new HashSet<>(primePathCal.generatePrimePathUnit()); //初始集合是所有的基路径
        solvableSet = new HashSet<>();
        unsolvableSet = new HashSet<>(initSet);
        testData = new HashSet<>();
    }

    @Override
    Set<List<Unit>> calAllFullCandidate(Object o) {
        List<Unit> primePath = (List)o;
        Unit headOfPrimePath = primePath.get(0);
        Unit tailOfPrimePath = primePath.get(primePath.size()-1);
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
                backwardPathCopy.addAll(primePath);
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
            List<Unit> primePath = (List<Unit>) o;
            if (CommonUtil.leftIsSubList(primePath, fullPath)) {
                solvableSet.add(primePath);
            }
        }
        unsolvableSet.removeAll(solvableSet);
    }
}
