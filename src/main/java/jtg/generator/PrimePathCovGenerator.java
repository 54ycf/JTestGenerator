package jtg.generator;

import soot.Body;
import soot.Local;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.UnitGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
public class PrimePathCovGenerator {
    private UnitGraph ug;
    private Body body;
    private List<Local> jimpleVars;

    public PrimePathCovGenerator(Body body, UnitGraph ug) {
        this.body = body;
        this.ug = ug;
        jimpleVars = getJVars();
    }

    private List<Local> getJVars() {
        //Jimple自身增加的Locals，不是被测代码真正的变量
        ArrayList<Local> jimpleVars = new ArrayList<Local>();
        for (Local l : body.getLocals()) {
            if (l.toString().startsWith("$")) jimpleVars.add(l);
        }
        return jimpleVars;
    }

    public String calPathConstraint(List<Unit> path) {
        List<Local> jVars = getJVars();

        String pathConstraint = "";
        String expectedResult = "";

        HashMap<String, String> assignList = new HashMap<>();
        ArrayList<String> stepConditionsWithJimpleVars = new ArrayList<String>();
        ArrayList<String> stepConditions = new ArrayList<String>();

        for (Unit stmt : path) {

            if (stmt instanceof JAssignStmt) {
                assignList.put(((JAssignStmt) stmt).getLeftOp().toString(), ((JAssignStmt) stmt).getRightOp().toString());
                continue;
            }
            if (stmt instanceof JIfStmt) {

                String ifstms = ((JIfStmt) stmt).getCondition().toString();
                int nextUnitIndex = path.indexOf(stmt) + 1;
                Unit nextUnit = path.get(nextUnitIndex);

                //如果ifstmt的后继语句不是ifstmt中goto语句，说明ifstmt中的条件为假
                if (!((JIfStmt) stmt).getTarget().equals(nextUnit))
                    ifstms = "!( " + ifstms + " )";
                else
                    ifstms = "( " + ifstms + " )";
                stepConditionsWithJimpleVars.add(ifstms);
                continue;
            }
            if (stmt instanceof JReturnStmt) {
                expectedResult = stmt.toString().replace("return", "").trim();
            }
        }
        System.out.println("The step conditions with JimpleVars are: " + stepConditionsWithJimpleVars);

        //bug 没有考虑jVars为空的情况
        if (jVars.size() != 0) {
            for (String cond : stepConditionsWithJimpleVars) {
                //替换条件里的Jimple变量
                for (Local lv : jVars) {
                    if (cond.contains(lv.toString())) {
                        stepConditions.add(cond.replace(lv.toString(), assignList.get(lv.toString()).trim()));
                    }
                }
            }
        } else
            stepConditions = stepConditionsWithJimpleVars;

        if (stepConditions.isEmpty())
            return "";
        pathConstraint = stepConditions.get(0);
        int i = 1;
        while (i < stepConditions.size()) {
            pathConstraint = pathConstraint + " && " + stepConditions.get(i);
            i++;
        }
        //System.out.println("The path expression is: " + pathConstraint);
        return pathConstraint;
    }
}
