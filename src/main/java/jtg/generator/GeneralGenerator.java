package jtg.generator;

import jtg.generator.util.PathUtil;
import jtg.graphics.SootCFG;
import jtg.visualizer.Visualizer;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.*;

/**
 * 通用求解器
 * 先找到要覆盖的路径，语句覆盖就是覆盖每一个点，分支覆盖就是覆盖每一个跳转，基路径覆盖就是每一条基路径
 * 根据条件，进行上下蔓延，比如
 * 语句覆盖：拿出一个没有覆盖到的点，上蔓延n条到开始点，下蔓延m条到结束点，找到第一条能解的路径，并将所有路径上的点标注为已访问
 * 分支覆盖：拿出一个没有执行的跳转，找到跳转的起点，向上蔓延n条，找到跳转的目的地，向下蔓延m条，找到第一条能解的路径并将路径的跳转都标为已访问
 * 基路径覆盖：拿出一条未执行的基路径，找到起点，向上蔓延n条，找到基路径的最后一个点，蔓延到终点，知道条能解的路，并将所有能解的基路径标记未已访问
 * 如果蔓延了n*m条依旧无解，就把这个地方标记为暂时无解，统计覆盖率时表现出来
 */
public class GeneralGenerator {
    private String clsPath;
    private String clsName;
    private String mtdName;
    private UnitGraph ug;
    private Body body;
    private List<Local> jimpleVars;
    private List<Local> paras;
    private List<Local> localVars;
    public GeneralGenerator(String classPath, String className, String methodName) {
        clsPath = classPath;
        clsName = className;
        mtdName = methodName;
        ug = SootCFG.getMethodCFG(clsPath, clsName, mtdName);
        body = SootCFG.getMethodBody(clsPath, clsName, mtdName);
        getVars();
    }

    public void drawCFG(String graphName, boolean indexLabel) {
        System.out.println("=======================start To draw======================");
        Visualizer.printCFGDot(graphName, ug, indexLabel);
    }

    private void getVars() {
        jimpleVars = new ArrayList<>();
        paras = new ArrayList<>();
        localVars = new ArrayList<>();
        //Jimple自身增加的Locals，不是被测代码真正的变量
        for (Local l : body.getLocals()) {
            if (l.toString().startsWith("$")) jimpleVars.add(l);
        }
        //参数
        for (Local para : body.getParameterLocals()) {
            paras.add(para);
        }
        //剩下的就是局部变量
        for (Local l : body.getLocals()) {
            if (!paras.contains(l) && !jimpleVars.contains(l)) {
                localVars.add(l);
            }
        }
    }

    public void generate(){
        System.out.println("pathUtil.extendCFG*****************************************************");
        PathUtil.extendCFG(body); //扩展调用的方法，成为完整的CFG
        ug = new ClassicCompleteUnitGraph(body); //更新这个图
        System.out.println("+++++++++++++++++++++++update__+++++++++++++++++++++++");
        for (Unit unit : body.getUnits()) {
            System.out.println(unit.getClass().getSimpleName() + "---->"+unit);
        }
        drawCFG("test_insertAfter", false);
    }

    //路径约束求解


}
