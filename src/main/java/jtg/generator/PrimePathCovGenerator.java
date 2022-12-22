package jtg.generator;

import jtg.generator.path.DDLPrimePathCal;
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
        DDLPrimePathCal primePathCal = new DDLPrimePathCal(clsPath, clsName, mtdName);
        initSet = new HashSet<>(primePathCal.generatePrimePathUnit()); //初始集合是所有的基路径
        solvableSet = new HashSet<>();
        unsolvableSet = new HashSet<>();
        testData = new HashSet<>();
    }

    @Override
    Set<List<Unit>> calAllFullCandidate(Object o) {
        return null;
    }

    @Override
    void checkOtherIfCov(List<Unit> fullPath) {

    }
}
