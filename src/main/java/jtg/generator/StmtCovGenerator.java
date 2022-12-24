package jtg.generator;

import jtg.generator.util.PathUtil;
import soot.Unit;

import java.util.*;
import java.util.stream.Collectors;

public class StmtCovGenerator extends GeneralGenerator{
    public StmtCovGenerator(String classPath, String className, String methodName) {
        super(classPath, className, methodName);
    }

    @Override
    void init() {
        initSet = new HashSet<>(body.getUnits());
        solvableSet = new HashSet<>();
        unsolvableSet = new HashSet<>(initSet);
        testData = new HashSet<>();
    }

    @Override
    Set<List<Unit>> calAllFullCandidate(Object o) {
        Unit stmtUnit = (Unit) o;
        Set<List<Unit>> backwardPaths = new HashSet<>();
        Set<List<Unit>> forwardPaths = new HashSet<>();
        for (Unit head : heads) {
            PathUtil pathUtil = new PathUtil();
            pathUtil.findPath(ug, stmtUnit, head, new ArrayList<>(), true, new HashMap<>());
            backwardPaths.addAll(pathUtil.getSearchPathResult());
        }
        for (Unit tail : tails) {
            PathUtil pathUtil = new PathUtil();
            pathUtil.findPath(ug, stmtUnit, tail, new ArrayList<>(), false, new HashMap<>());
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
                backwardPathCopy.remove(backwardPathCopy.size()-1); //首尾重合了
                backwardPathCopy.addAll(forwardPath);
                result.add(backwardPathCopy);
            }
        }
        return result;
    }

    @Override
    void checkCov(List<Unit> fullPath) {
        solvableSet.addAll(fullPath); //这条路径覆盖的所有语句都加进去
        fullPath.forEach(unsolvableSet::remove); //在不可解集中的也减去
    }

}
