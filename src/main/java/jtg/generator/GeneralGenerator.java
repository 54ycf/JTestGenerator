package jtg.generator;

import jtg.generator.util.PathUtil;
import jtg.generator.util.StaticsUtil;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.Body;
import soot.Local;
import soot.Unit;
import soot.Value;
import soot.jimple.internal.*;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通用求解器
 * 先找到要覆盖的路径，语句覆盖就是覆盖每一个点，分支覆盖就是覆盖每一个跳转，基路径覆盖就是每一条基路径
 * 根据条件，进行上下蔓延，比如
 * 语句覆盖：拿出一个没有覆盖到的点，上蔓延n条到开始点，下蔓延m条到结束点，找到第一条能解的路径，并将所有路径上的点标注为已访问
 * 分支覆盖：拿出一个没有执行的跳转，找到跳转的起点，向上蔓延n条，找到跳转的目的地，向下蔓延m条，找到第一条能解的路径并将路径的跳转都标为已访问
 * 基路径覆盖：拿出一条未执行的基路径，找到起点，向上蔓延n条，找到基路径的最后一个点，蔓延到终点，知道条能解的路，并将所有能解的基路径标记未已访问
 * 如果蔓延了n*m条依旧无解，就把这个地方标记为暂时无解，统计覆盖率时表现出来
 */
public abstract class GeneralGenerator {
    protected String clsPath;
    protected String clsName;
    protected String mtdName;
    protected UnitGraph ug;
    protected Body body;
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

    Set<Object> initSet;
    Set<Object> solvableSet;
    Set<Object> unsolvableSet;
    Set<String> testData;
    public void generate() {
        PathUtil.extendCFG(body); //扩展调用的方法，成为完整的CFG
        ug = new ClassicCompleteUnitGraph(body); //更新这个图
//        for (Unit unit : body.getUnits()) {
//            System.out.println(unit.getClass().getSimpleName() + "---->"+unit);
//        }
        drawCFG("test_insertAfter", false);
        init();
        for (Object o : initSet) {
            if (solvableSet.contains(o)) continue; //如果这项前面已经覆盖了，就直接跳过不算了
            Set<List<Unit>> allFullPath = calAllFullCandidate(o);
            boolean solvable = false;
            List<Unit> successFullPath = new ArrayList<>();
            for (List<Unit> units : allFullPath) {
                try {
                    String constraint = calPathConstraint(units);
                    successFullPath = units;
                    System.out.println("success " + successFullPath );
                    solvable = true;
                    System.out.println("约束是" + constraint);
                    testData.add(solve(constraint));
                    break; //找到解，后面的备选路径就不用看了
                } catch (Exception e) {
                    continue; //这一条路径约束无解
                }
            }
            if (solvable) {
                solvableSet.add(o);
                checkOtherIfCov(successFullPath);
            }else {
                unsolvableSet.add(o);
            }
        }
        double covRate = solvableSet.size() / (double)initSet.size();
        System.out.println("覆盖率是 " + covRate);
        System.out.println(testData);
    }

    /**
     * 初始化初始集、不可解集、可解集
     */
    abstract void init();

    /**
     * 计算每一项向上向下拓展完全的所有备选路径
     * @param o
     * @return
     */
    abstract Set<List<Unit>> calAllFullCandidate(Object o);

    /**
     * 在计算出一条完整路径有解的时候，检测这个路径是否包含了其他的要求覆盖的集合
     * @param fullPath
     */
    abstract void checkOtherIfCov(List<Unit> fullPath);

    /******************************路径约束求解***********************************/
    HashMap<String, String> assignList = new HashMap<>(); //用于变量替换，在路径遍历途中，变量的具体值在不断变化
    public String calPathConstraint(List<Unit> path) throws Exception{
        String pathConstraint = "";
        String expectedResult = "";
        ArrayList<String> stepConditions = new ArrayList<>(); //Jimple变量是字节码中为三地址表示增添的变量，去掉它
        for (Unit stmt : path) {
            //赋值语句
            if (stmt instanceof JAssignStmt) {
                JAssignStmt assignStmt = (JAssignStmt) stmt;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                String leftOpStr = leftOp.toString();
                String rightOpStr = rightOp.toString();
                //先预处理右边的符号，lengthof、new、数组引用
                rightOpStr = preHandleRightVal(leftOp, rightOp);
                //再预处理左侧表达式
                leftOpStr = preHandleLeftVal(leftOp);
//                System.out.println(leftOpStr + "  <-->  " + rightOpStr);
                assignList.put(leftOpStr, rightOpStr);
            }
            //if判断
            if (stmt instanceof JIfStmt) {
                String ifstmt = ((JIfStmt) stmt).getCondition().toString();
                String[] split = ifstmt.split("\\s+"); //换一种化简方式
                String ifstmtStr= "";
                for (String s : split) {
                    s = Optional.ofNullable(assignList.get(s)).orElse(s);
                    ifstmtStr = ifstmtStr + s + " ";
                }
                ifstmt = ifstmtStr;
//                System.out.println("替换条件后" + ifstmtStr);
                int nextUnitIndex = path.indexOf(stmt) + 1;
                Unit nextUnit = path.get(nextUnitIndex);
                //如果ifstmt的后继语句不是ifstmt中goto语句，说明ifstmt中的条件为假
                if (!((JIfStmt) stmt).getTarget().equals(nextUnit))
                    ifstmt = "!( " + ifstmt + " )";
                else
                    ifstmt = "( " + ifstmt + " )";
                stepConditions.add(ifstmt);
            }
        }

        //尾部
        if (stepConditions.isEmpty())
            return "";  //没有约束条件
        pathConstraint = stepConditions.get(0);
        int i = 1;
        while (i < stepConditions.size()) {
            pathConstraint = pathConstraint + " && " + stepConditions.get(i);
            i++;
        }
        return pathConstraint;
    }
    /**
     * 该方法主要使用正则表达式来判断字符串中是否包含字母
     * @return 返回是否包含字母，包含则证明有位置变量，目前水平解不出来
     */
    private boolean judgeContainsStr(String cardNum) {
        String regex=".*[a-zA-Z]+.*";
        Matcher m= Pattern.compile(regex).matcher(cardNum);
        return m.matches();
    }
    private String preHandleLeftVal(Value leftOp) throws Exception {
        if (leftOp instanceof JArrayRef) {
            JArrayRef jArrayRef = (JArrayRef) leftOp;
            return handleArrInd(jArrayRef);
        }else if (leftOp instanceof JInstanceFieldRef) {
            JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) leftOp;
            return extendFullObjName(jInstanceFieldRef.getBase()) + "_" + jInstanceFieldRef.getField().getName();
        }else if(leftOp instanceof JimpleLocal){
            return extendFullObjName(leftOp);
        }else {
            return leftOp.toString();
        }
    }
    private String preHandleRightVal(Value leftOp, Value rightOp) throws Exception{
        if (rightOp instanceof JLengthExpr) { //数组长度表达式
            JLengthExpr jLengthExpr = (JLengthExpr) rightOp;
            Value op = jLengthExpr.getOp(); //lengthof op
            return Optional.ofNullable(assignList.get(op.toString())).orElse(op.toString()) + "_len";
        } else if (rightOp instanceof JArrayRef) { //数组引用
            JArrayRef jArrayRef = (JArrayRef) rightOp;
            String arrExpr = handleArrInd(jArrayRef);
            return Optional.ofNullable(assignList.get(arrExpr)).orElse(arrExpr);
        } else if (rightOp instanceof JNewArrayExpr) {
            JNewArrayExpr jNewArrayExpr = (JNewArrayExpr) rightOp;
            String rightOpStr = "newArr_" + jNewArrayExpr.getBaseType().toString() +  StaticsUtil.genMark(); //TODO 这个baseType可能是包含.，这个符号不支持z3，需要修改
            String leftOpStr = leftOp.toString();
            leftOpStr = Optional.ofNullable(assignList.get(leftOpStr)).orElse(leftOpStr);
            assignList.put(leftOpStr + "_len", jNewArrayExpr.getSize().toString());//新建数组，初始化长度要用到
            return rightOpStr;
        } else if (rightOp instanceof JNewExpr) { //new 对象
            JNewExpr jNewExpr = (JNewExpr) rightOp;
            String rightOpStr = "newObj_" + jNewExpr.getBaseType()+ StaticsUtil.genMark();
            rightOpStr = rightOpStr.replace(".", "_"); //jtg.Person改成jtg_Person
            return rightOpStr;
        } else if (rightOp instanceof JInstanceFieldRef) {
            JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
            String fullStr= extendFullObjName(jInstanceFieldRef.getBase()) + "_" + jInstanceFieldRef.getField().getName();
            return Optional.ofNullable(assignList.get(fullStr)).orElse(fullStr);
        }else if(rightOp instanceof AbstractJimpleFloatBinopExpr){
            //最后正式在处理变量的具体表示，如：r0 = r1 + r2
            StringBuilder detailRightOp = new StringBuilder();
            for (String s : rightOp.toString().split("\\s+")) {
                if (assignList.containsKey(s)) {
                    s = " ( " + assignList.get(s) + " ) "; //每次put加括号也可能有问题，比如对象.属性，这个就变成了(对象).属性
                }
                detailRightOp.append(s); //将右侧的语句化简成最简单
            }
            return detailRightOp.toString();
        }else if(rightOp instanceof JimpleLocal){
            String rightOpStr = extendFullObjName(rightOp);
            return Optional.ofNullable(assignList.get(rightOpStr)).orElse(rightOpStr);
        }else {
            return rightOp.toString();
        }
    }
    /**
     * 将$r1Xi0扩展成newObj_jtg_Person123_i0
     * @param value
     * @return
     */
    private String extendFullObjName(Value value){
        if (value.toString().contains("X")) {
            String[] cutBaseStr = value.toString().split("X");
            cutBaseStr[0] = Optional.ofNullable(assignList.get(cutBaseStr[0])).orElse(cutBaseStr[0]) + "_";
            return cutBaseStr[0] + cutBaseStr[1];
        }
        return value.toString();
    }
    /**
     * 处理数组变量的符号
     * @param jArrayRef
     * @return
     * @throws Exception
     */
    private String handleArrInd(JArrayRef jArrayRef) throws Exception {
        String baseStr = jArrayRef.getBase().toString();
        baseStr = Optional.ofNullable(assignList.get(baseStr)).orElse(baseStr);
        String indStr = jArrayRef.getIndex().toString();
        String rightOpStr = "";
        if (assignList.containsKey(indStr)) { // 像r0[b0]这样b0是变量的索引，要归结到常量。如果b0是变量，就一定出现过赋值
            String indExp = assignList.get(indStr); //索引是由确定常量赋值的，否则无法解出来
            if (judgeContainsStr(indExp)) { //index包含未确定的变量，暂时无法求解
                throw new Exception("抱歉，暂不支持未确定索引的数组处理");
            }
            String solveRes = solve(indExp + " - a == 0"); //最后会得到a=?这样的字符串表达式，问号就是索引值
            rightOpStr = baseStr + "_ind" + solveRes.substring(2);
        }else { //r0[3]这样的常量
            rightOpStr = baseStr + "_ind" + indStr;
        }
        return rightOpStr;
    }

    public String solve(String pathConstraint) throws Exception {
        return Z3Solver.solve(pathConstraint);
    }
}
