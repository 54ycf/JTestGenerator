package jtg.generator;

import jtg.generator.util.PathUtil;
import jtg.generator.util.StaticsUtil;
import jtg.graphics.SootCFG;
import jtg.solver.Z3Solver;
import jtg.visualizer.Visualizer;
import soot.*;
import soot.jimple.StaticFieldRef;
import soot.jimple.internal.*;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.lang.reflect.Field;
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
    protected List<Unit> heads;
    protected List<Unit> tails;
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
    Map<String, Type> typeMap;
    Set<Unit> caller = new HashSet<>();

    public void generate() {
        drawCFG(mtdName + "", false);
        PathUtil.extendCFG(body, caller); //扩展调用的方法，成为完整的CFG
        PathUtil.extendCFG(body, caller);
        ug = new ClassicCompleteUnitGraph(body); //更新这个图
        drawCFG(mtdName + "_extend", false);
        init();
        heads = ug.getHeads();
        tails = ug.getTails();
        typeMap = new HashMap<>();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof JIdentityStmt) { //只要开头的Identity，记录下类型
                JIdentityStmt jIdentityStmt = (JIdentityStmt) unit;
                String leftOpStr = jIdentityStmt.getLeftOp().toString();
                Type type = jIdentityStmt.getRightOp().getType();
                System.out.println(leftOpStr + "  " + type);
                typeMap.put(leftOpStr, type);
            } else {
                break;
            }
        }
        for (Object o : initSet) {
            if (solvableSet.contains(o)) continue; //如果这项前面已经覆盖了，就直接跳过不算了
            Set<List<Unit>> allFullPath = calAllFullCandidate(o);
            boolean solvable = false;
            List<Unit> successFullPath = new ArrayList<>();
            for (List<Unit> units : allFullPath) {
                try {
                    String constraint = calPathConstraint(units);
                    System.out.println("约束：" + constraint);
                    constraint = addTypeConstraint(constraint); //根据类型加入新约束
                    System.out.println("新约束：" + constraint);
                    String solve = solve(constraint);
                    if (solve.contains("error")) { //无解
                        System.out.println("无解：" + units);
                        System.out.println();
                        continue;
                    }
                    successFullPath = units;
                    System.out.println("success ");
                    for (Unit unit : successFullPath) {
                        System.out.println(unit);
                    }
                    solvable = true;
                    System.out.println("解是 " + solve);
                    System.out.println();
                    testData.add(solve);
                    break; //找到解，后面的备选路径就不用看了
                } catch (Exception e) {
                    System.out.println("异常 ");
                    e.printStackTrace();
                    continue; //这一条路径约束无解
                }
            }
            if (solvable) {
                checkCov(successFullPath);
            }
        }
        double covRate = solvableSet.size() / (double) initSet.size();
        System.out.println("覆盖率是 " + covRate);
        System.out.println(testData);
    }

    /**
     * 初始化初始集、不可解集、可解集
     */
    abstract void init();

    /**
     * 计算每一项向上向下拓展完全的所有备选路径
     *
     * @param o
     * @return
     */
    abstract Set<List<Unit>> calAllFullCandidate(Object o);

    /**
     * 在计算出一条完整路径有解的时候，检测这个路径包含了哪些要求覆盖的集合
     *
     * @param fullPath
     */
    abstract void checkCov(List<Unit> fullPath);

    /******************************路径约束求解***********************************/
    HashMap<String, String> assignList; //用于变量替换，在路径遍历途中，变量的具体值在不断变化

    public String calPathConstraint(List<Unit> path) throws Exception {
        assignList = new HashMap<>();
        String pathConstraint = "";
        String expectedResult = "";
        ArrayList<String> stepConditions = new ArrayList<>(); //Jimple变量是字节码中为三地址表示增添的变量，去掉它
        for (int i=0; i<path.size(); ++i) {
            Unit stmt = path.get(i);
            if (caller.contains(stmt)) continue; //调用语句不处理
            //赋值语句
            if (stmt instanceof JAssignStmt) {
                JAssignStmt assignStmt = (JAssignStmt) stmt;
                Value leftOp = assignStmt.getLeftOp();
                Value rightOp = assignStmt.getRightOp();
                //先预处理右边的符号，lengthof、new、数组引用
                String rightOpStr = preHandleRightVal(leftOp, rightOp);
                //再预处理左侧表达式
                String leftOpStr = preHandleLeftVal(leftOp);
//                System.out.println(leftOpStr + "  <-->  " + rightOpStr);
                assignList.put(leftOpStr, rightOpStr);
            }
            //if判断
            if (stmt instanceof JIfStmt) {
                String ifstmt = ((JIfStmt) stmt).getCondition().toString();
                String[] split = ifstmt.split("\\s+"); //换一种化简方式
                String ifstmtStr = "";
                for (String s : split) {
                    s = Optional.ofNullable(assignList.get(s)).orElse(s);
                    ifstmtStr = ifstmtStr + s + " ";
                }
                ifstmt = ifstmtStr;
//                System.out.println("替换条件后" + ifstmtStr);
                int nextUnitIndex = i + 1;
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
     *
     * @return 返回是否包含字母，包含则证明有位置变量，目前水平解不出来
     */
    private boolean judgeContainsStr(String cardNum) {
        String regex = ".*[a-zA-Z]+.*";
        Matcher m = Pattern.compile(regex).matcher(cardNum);
        return m.matches();
    }

    private String preHandleLeftVal(Value leftOp) throws Exception {
        if (leftOp instanceof JArrayRef) {
            JArrayRef jArrayRef = (JArrayRef) leftOp;
            return handleArrInd(jArrayRef);
        } else if (leftOp instanceof JInstanceFieldRef) {
            JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) leftOp;
            return extendFullObjName(jInstanceFieldRef.getBase()) + "_" + jInstanceFieldRef.getField().getName();
        } else if (leftOp instanceof JimpleLocal) {
            return leftOp.toString();
        } else {
            return leftOp.toString();
        }
    }

    private String preHandleRightVal(Value leftOp, Value rightOp) throws Exception {
        if (rightOp instanceof JLengthExpr) { //数组长度表达式
            JLengthExpr jLengthExpr = (JLengthExpr) rightOp;
            Value op = jLengthExpr.getOp(); //lengthof op
            String str = Optional.ofNullable(assignList.get(op.toString())).orElse(op.toString()) + "_len";
            typeMap.put(str, IntType.v()); //长度类型是整型
            return str;
        } else if (rightOp instanceof JArrayRef) { //数组引用
            JArrayRef jArrayRef = (JArrayRef) rightOp;
            String arrExpr = handleArrInd(jArrayRef);
            return Optional.ofNullable(assignList.get(arrExpr)).orElse(arrExpr);
        } else if (rightOp instanceof JNewArrayExpr) {
            JNewArrayExpr jNewArrayExpr = (JNewArrayExpr) rightOp;
            String rightOpStr = "newArr_" + jNewArrayExpr.getBaseType().toString() + StaticsUtil.genMark(); //TODO 这个baseType可能是包含.，这个符号不支持z3，需要修改
            String leftOpStr = leftOp.toString();
            leftOpStr = Optional.ofNullable(assignList.get(leftOpStr)).orElse(leftOpStr);
            assignList.put(leftOpStr + "_len", jNewArrayExpr.getSize().toString());//新建数组，初始化长度要用到
            return rightOpStr;
        } else if (rightOp instanceof JNewExpr) { //new 对象
            JNewExpr jNewExpr = (JNewExpr) rightOp;
            String rightOpStr = "newObj_" + jNewExpr.getBaseType() + StaticsUtil.genMark();
            rightOpStr = rightOpStr.replace(".", ""); //jtg.Person改成(jtg_Person) jtgPerson
            return rightOpStr;
        } else if (rightOp instanceof JInstanceFieldRef) {
            JInstanceFieldRef jInstanceFieldRef = (JInstanceFieldRef) rightOp;
            String fullStr = extendFullObjName(jInstanceFieldRef.getBase()) + "_" + jInstanceFieldRef.getField().getName();
            fullStr = Optional.ofNullable(assignList.get(fullStr)).orElse(fullStr);
            typeMap.put(fullStr, rightOp.getType());
            return fullStr;
        } else if (rightOp instanceof AbstractJimpleFloatBinopExpr) {
            //最后正式在处理变量的具体表示，如：r0 = r1 + r2
            StringBuilder detailRightOp = new StringBuilder();
            String[] split = rightOp.toString().split("\\s+");
            split[0] = " ( " + Optional.ofNullable(assignList.get(split[0])).orElse(split[0]) + " ) ";
            split[2] = " ( " + Optional.ofNullable(assignList.get(split[2])).orElse(split[2]) + " ) ";
            for (String s : split) {
//                if (assignList.containsKey(s)) {
//                    s = " ( " + assignList.get(s) + " ) "; //每次put加括号也可能有问题，比如对象.属性，这个就变成了(对象).属性
//                }
                detailRightOp.append(s); //将右侧的语句化简成最简单
            }
            return detailRightOp.toString();
        } else if (rightOp instanceof JimpleLocal) {
            String rightOpStr = extendFullObjName(rightOp);
            return Optional.ofNullable(assignList.get(rightOpStr)).orElse(rightOpStr);
        } else if (rightOp instanceof JNegExpr) {
            JNegExpr jNegExpr = (JNegExpr) rightOp;
            String opStr = jNegExpr.getOp().toString();
            opStr = Optional.ofNullable(assignList.get(opStr)).orElse(opStr);
            return " ( 0 - " + opStr + " ) ";
        } else if (rightOp instanceof StaticFieldRef) {
            StaticFieldRef staticFieldRef = (StaticFieldRef) rightOp;
            String rightOpStr = rightOp.toString();
            Class<?> cls = Class.forName(staticFieldRef.getType().toString());
            if (cls.isEnum()) {
                Field field = cls.getDeclaredField(staticFieldRef.getField().getName());
                Enum anEnum = (Enum) field.get(null);
                int ordinal = anEnum.ordinal();
                rightOpStr = String.valueOf(ordinal);
            }
            return rightOpStr;
        } else if (rightOp instanceof JInstanceOfExpr) {
            JInstanceOfExpr jInstanceOfExpr = (JInstanceOfExpr) rightOp;
            System.out.println(jInstanceOfExpr.getOp().getType());
            System.out.println(jInstanceOfExpr.getCheckType());
            return rightOp.toString();
        } else {
            return rightOp.toString();
        }
    }

    /**
     * 将$r1Xi0扩展成newObj_jtg_Person123_i0 （出错误）
     *
     * @param value
     * @return
     */
    private String extendFullObjName(Value value) {
        String valStr = value.toString();
        valStr = Optional.ofNullable(assignList.get(valStr)).orElse(valStr);
//        if (valStr.contains("X")) {
//            String[] cutBaseStr = value.toString().split("X");
//            cutBaseStr[0] = Optional.ofNullable(assignList.get(cutBaseStr[0])).orElse(cutBaseStr[0]) + "_";
//            return cutBaseStr[0] + cutBaseStr[1];
//        }
        return valStr;
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
        } else { //r0[3]这样的常量
            rightOpStr = baseStr + "_ind" + indStr;
        }
        return rightOpStr;
    }

    public String solve(String pathConstraint) throws Exception {
        return Z3Solver.solve(pathConstraint);
    }

    private String addTypeConstraint(String s) throws ClassNotFoundException {
        String constraint = new String(s);
        if (s.equals("")) s = "1 == 1";
        for (Map.Entry<String, Type> stringTypeEntry : typeMap.entrySet()) {
            String val = stringTypeEntry.getKey();
            Type type = stringTypeEntry.getValue();
            if (constraint.contains(val)) {
                if (type instanceof IntType) {
                    s = s + " && ( " + val + " % 1 = 0 )";
                } else if (type instanceof CharType) {
                    s = s + " && ( " + val + " % 1 = 0 ) && ( " + val + " >= 0 ) && ( " + val + " <= 65535 )";
                } else if (type instanceof BooleanType) {
                    s = s + " && ( " + val + " = 0 || " + val + " = 1 )"; //0 true
                } else if (type instanceof ArrayType) {
                    ArrayType arrayType = (ArrayType) type;
                    Type elementType = arrayType.getElementType();
                } else if (type instanceof RefType) {
                    Class<?> clz = Class.forName(type.toString());
                    if (clz.isEnum()) {
                        Class<Enum> clazz = (Class<Enum>) clz;
                        int ordNum = clazz.getEnumConstants().length;
                        s = s + " && ( " + val + " % 1 = 0 ) && ( " + val + " >= 0 ) && ( " + val + " < " + ordNum + " )";
                    }
                }
//                System.out.println(type.getClass().getSimpleName() + " haha ");
            }
        }
        return s;
    }
}
