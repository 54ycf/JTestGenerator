package jtg.generator;

import jtg.exception.ConstraintSolveException;
import jtg.generator.util.PathUtil;
import jtg.generator.util.StaticsUtil;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.*;
import soot.dava.internal.javaRep.DLengthExpr;
import soot.jimple.ArrayRef;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimpleGenerator {

    private String clsPath;
    private String clsName;
    private String mtdName;
    private UnitGraph ug;
    private Body body;


    public SimpleGenerator(String className, String methodName) {
        String defaultClsPath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "classes";
        new SimpleGenerator(defaultClsPath, className, methodName);
    }

    public SimpleGenerator(String classPath, String className, String methodName) {
        clsPath = classPath;
        clsName = className;
        mtdName = methodName;
        ug = SootCFG.getMethodCFG(clsPath, clsName, mtdName);
        body = SootCFG.getMethodBody(clsPath, clsName, mtdName);
    }

    public void drawCFG(String graphName, boolean indexLabel) {
        Visualizer.printCFGDot(graphName, ug, indexLabel);
    }


    private List<Local> getJVars() {
        //Jimple自身增加的Locals，不是被测代码真正的变量
        ArrayList<Local> jimpleVars = new ArrayList<Local>();
        for (Local l : body.getLocals()) {
            if (l.toString().startsWith("$")) jimpleVars.add(l);
        }
        return jimpleVars;
    }

    private List<Local> getParameter() {
        ArrayList<Local> paras = new ArrayList<Local>();
        for (Local para : body.getParameterLocals()) {
            paras.add(para);
        }
        return paras;
    }

    public List<String> generate() {

        List<Unit> path = null;
        ArrayList<String> testSet = null;
        String pathConstraint = "";

        System.out.println("============================================================================");
        System.out.println("Generating test case inputs for method: " + clsName + "." + mtdName + "()");
        System.out.println("============================================================================");
        System.out.println("老body\n" + body);
        PathUtil.extendCFG(body);
        System.out.println("新body\n" + body);
        ug = new ClassicCompleteUnitGraph(body); //扩展后的图
        Visualizer.printCFGDot("test_new_invoke", ug, false);
        System.out.println("#######################################################");
        try {
            testSet = new ArrayList<String>();
            for (Unit h : ug.getHeads())
                for (Unit t : ug.getTails()) {
                    path = ug.getExtendedBasicBlockPathBetween(h, t);
                    pathConstraint = calPathConstraint2(path);
                    //如果路径约束为空字符串，表示路径约束为恒真
                    if (pathConstraint.isEmpty()) testSet.add(randomTC(body.getParameterLocals()));
                    System.out.println("The corresponding path constraint is: " + pathConstraint);
                    if (!pathConstraint.isEmpty())
                        testSet.add(solve(pathConstraint));
                }
        } catch (Exception e) {
            System.err.println("Error in generating test cases: ");
            System.err.println(e.toString());
        }
        if (!testSet.isEmpty()) {
            System.out.println("");
            System.out.println("The generated test case inputs:");
            int count = 1;
            for (String tc : testSet) {
                System.out.println("( " + count + " ) " + tc.toString());
                count++;
            }
        }
        return testSet;
    }


    HashMap<String, String> assignList = new HashMap<>(); //用于变量替换，在路径遍历途中，变量的具体值在不断变化
    //TODO TODO !!!!变量重名，快处理
    public String calPathConstraint2(List<Unit> path) throws Exception{
        String pathConstraint = "";
        String expectedResult = "";

        ArrayList<String> stepConditions = new ArrayList<>(); //Jimple变量是字节码中为三地址表示增添的变量，去掉它

        for (Unit stmt : path) {
            //赋值语句
            if (stmt instanceof JAssignStmt) { //TODO 不支持new对象，数组lengthof，左侧右侧都有可能数组
                JAssignStmt assignStmt = (JAssignStmt) stmt;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                String leftOpStr = leftOp.toString();
                String rightOpStr = rightOp.toString();

                System.out.println("左边： " + leftOpStr);
                System.out.println("右边： " + rightOpStr);
                //先预处理右边的符号，lengthof、new、数组引用
                rightOpStr = preHandleRightVal(leftOp, rightOp);
                //再预处理左侧表达式
                leftOpStr = preHandleLeftVal(leftOp);
                System.out.println("左边处理： " + leftOpStr);
                System.out.println("右边处理： " + rightOpStr);


                System.out.println(leftOpStr + "  <-->  " + rightOpStr);
                assignList.put(leftOpStr, rightOpStr);
                continue;
            }
            //if判断
            if (stmt instanceof JIfStmt) {
                String ifstmt = ((JIfStmt) stmt).getCondition().toString();
//                for (Map.Entry<String, String> entry : assignList.entrySet()) {
//                    System.out.println("替换前：" + ifstmt);
//                    ifstmt = ifstmt.replace(entry.getKey(), entry.getValue()); //条件化简，这个替换方式不对！
//                    System.out.println("替换后：" + ifstmt);
//                }
                String[] split = ifstmt.split("\\s+"); //换一种化简方式
                String ifstmtStr= "";
                for (String s : split) {
                    if (assignList.containsKey(s)) {
                        s = assignList.get(s);
                    }
                    ifstmtStr = ifstmtStr + s + " ";
                }
                ifstmt = ifstmtStr;
                System.out.println("替换条件后" + ifstmtStr);
                int nextUnitIndex = path.indexOf(stmt) + 1;
                Unit nextUnit = path.get(nextUnitIndex);
                //如果ifstmt的后继语句不是ifstmt中goto语句，说明ifstmt中的条件为假
                if (!((JIfStmt) stmt).getTarget().equals(nextUnit))
                    ifstmt = "!( " + ifstmt + " )";
                else
                    ifstmt = "( " + ifstmt + " )";
                stepConditions.add(ifstmt);
                continue;
            }
            if (stmt instanceof JReturnStmt) {
                expectedResult = stmt.toString().replace("return", "").trim();
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
        System.out.println("路径是：" + path);
        for (String stepCondition : stepConditions) {
            System.out.println("condition: " + stepCondition);
        }
        //System.out.println("The path expression is: " + pathConstraint);
        return pathConstraint;
    }



    /**
     * 该方法主要使用正则表达式来判断字符串中是否包含字母
     * @return 返回是否包含
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
            String baseStr = jInstanceFieldRef.getBase().toString();
            String[] cutBaseStr = baseStr.split("X");
            if (assignList.containsKey(cutBaseStr[0])) {
                System.out.println(cutBaseStr[0] + "左边头头");
                cutBaseStr[0] = assignList.get(cutBaseStr[0]) + "_";
            }
            baseStr = cutBaseStr[0] + cutBaseStr[1];
            System.out.println("field的baseStr修改为" + baseStr);
            return baseStr + "_" + jInstanceFieldRef.getField().getName();
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
            if (assignList.containsKey(op.toString())) {
                return assignList.get(op) + "_len";
            }
            return op + "_len";
        } else if (rightOp instanceof JArrayRef) { //数组引用
            JArrayRef jArrayRef = (JArrayRef) rightOp;
            String arrExpr = handleArrInd(jArrayRef);
            if (assignList.containsKey(arrExpr)) {
                arrExpr = assignList.get(arrExpr);
            }
            return arrExpr;
        } else if (rightOp instanceof JNewArrayExpr) {
            JNewArrayExpr jNewArrayExpr = (JNewArrayExpr) rightOp;
            String rightOpStr = "newArr_" + jNewArrayExpr.getBaseType().toString() +  StaticsUtil.genMark(); //TODO 这个baseType可能是包含.，这个符号不支持z3，需要修改
            String leftOpStr = leftOp.toString();
            if (assignList.containsKey(leftOpStr)) {
                leftOpStr = assignList.get(leftOpStr);
            }
            assignList.put(leftOpStr + "_len", jNewArrayExpr.getSize().toString());//新建数组，初始化长度要用到
            return rightOpStr;
        } else if (rightOp instanceof JNewExpr) { //new 对象
            JNewExpr jNewExpr = (JNewExpr) rightOp;
            String rightOpStr = "newObj_" + jNewExpr.getBaseType()+ StaticsUtil.genMark();
            rightOpStr = rightOpStr.replace(".", "_"); //jtg.Person改成jtg_Person
            return rightOpStr;
        } else if (rightOp instanceof JInstanceFieldRef) {
            JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
            String baseStr = jInstanceFieldRef.getBase().toString();
            String[] cutBaseStr = baseStr.split("X");
            if (assignList.containsKey(cutBaseStr[0])) {
                System.out.println(cutBaseStr[0] + "头头");
                cutBaseStr[0] = assignList.get(cutBaseStr[0]) + "_";
            }
            baseStr = cutBaseStr[0] + cutBaseStr[1];
            System.out.println("field的baseStr修改为" + baseStr);
            String fullStr =  baseStr + "_" + jInstanceFieldRef.getField().getName();
            if (assignList.containsKey(fullStr)) {
                fullStr = assignList.get(fullStr);
            }
            return fullStr;
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
            if (assignList.containsKey(rightOpStr)) {
                return assignList.get(rightOpStr);
            }
            return rightOpStr;
        }else {
            return rightOp.toString();
        }
    }
    private String extendFullObjName(Value value){
        if (value.toString().contains("X")) {
            String[] cutBaseStr = value.toString().split("X");
            cutBaseStr[0] = assignList.get(cutBaseStr[0]) + "_";
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
        if (assignList.containsKey(baseStr)) {
            baseStr = assignList.get(baseStr);
        }
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
        for (String stepCondition : stepConditions) {
            System.out.println("condition: " + stepCondition);
        }
        //System.out.println("The path expression is: " + pathConstraint);
        return pathConstraint;
    }

    public String solve(String pathConstraint) throws Exception {
        return Z3Solver.solve(pathConstraint);
    }

    public String randomTC(List<Local> parameters) {

        String varName;
        String varValue = "";
        String testinput = "";

        for (Local para : parameters) {
            varName = para.getName();
            if ("int".equals(para.getType().toString())) {
                varValue = String.valueOf((int) (Math.random() * 10));
            }
            if ("String".equals(para.getType().toString())) {
                varValue = "abc";
            }
            //其它的基本类型没写
            testinput = testinput + " " + varName + "=" + varValue;
        }
        return testinput;
    }

}
