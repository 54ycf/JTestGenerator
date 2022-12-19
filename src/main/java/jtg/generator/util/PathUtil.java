package jtg.generator.util;

import jtg.graphics.SootCFG;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.internal.*;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.util.*;
import java.util.stream.Collectors;

public class PathUtil {
    //一条Block转为一条Unit
    public static List<Unit> transferBp2Up(List<Block> blocks) {
        List<Unit> result = new ArrayList<>(); //每一条Block转换为unit
        for (Block block : blocks) {
            result.addAll(transferB2U(block));
        }
        return result;
    }

    public static List<Unit> transferB2U(Block block) {
        List<Unit> result = new ArrayList<>(); //每一个Block转换为unit
        Iterator<Unit> basicBlockIt = block.getBody().getUnits().iterator(block.getHead(), block.getTail());
        while (basicBlockIt.hasNext()) {
            Unit someUnit = (Unit) basicBlockIt.next();
            result.add(someUnit);
        }
        return result;
    }

    //处理调用方法，让他成为更大的cfg
    //JInvokeStmt如果是纯调用，最后就不处理返回值
    //JAssignStmt如果调用有返回值，要将返回值赋值给发起调用的位置
    public static void extendCFG(Body originBody) { //TODO 目前是只调用了一次
        //TODO 变量名重复的问题未解决
        UnitPatchingChain units = originBody.getUnits();
        List<Unit> unitList = new ArrayList<>(units);//循环要更改列表，不能用foreach
        for (int i = 0; i < unitList.size(); i++) { //当被调用的加入到里面时，不会增加list胀肚
            Unit unit = unitList.get(i);
//            System.out.println(unit + " 语句的类型是 " + unit.getClass().getSimpleName());

            //直接调用，返回值没有接收的变量，可能是returnVoid，也有可能return有值但是没有人接受
            if (unit instanceof JInvokeStmt) {
                JInvokeStmt jInvokeStmt = (JInvokeStmt) unit; //纯invoke语句
                InvokeExpr invokeExpr = jInvokeStmt.getInvokeExpr(); //invoke表达式 specialinvoke $r1.<jtg.Person: void <init>()>()
//                Body invokedBody = invokeExpr.getMethod().retrieveActiveBody();
                List<Unit> initAssigns = new ArrayList<>();
                //被调用者的body
                Body invokedBody = SootCFG.getMethodBody(invokeExpr.getMethod().getDeclaringClass().getName(), invokeExpr.getMethod().getSignature());
                Value caller = getCaller(jInvokeStmt.getInvokeExprBox().getValue());//是哪个主体调用的这个，方便重命名
                for (Local local : invokedBody.getLocals()) {
                    local.setName(caller + "X" + local.getName());
                    System.out.println(local.getName());
                }
                List<Value> args = invokeExpr.getArgs();/*调用的实参*/
                for (int j = 0; j < args.size(); j++)
                    initAssigns.add(new JAssignStmt(invokedBody.getParameterLocal(j), args.get(j)));//调用将实参赋值
                UnitPatchingChain invokedChain = invokedBody.getUnits(); //被调用函数的语句
                //将赋值的操作加到被调用语句的前面
                if (!initAssigns.isEmpty()) invokedChain.insertBefore(initAssigns, invokedChain.getFirst());
                //处理返回调度
                Iterator<Unit> invokedIt = invokedChain.snapshotIterator();
                while (invokedIt.hasNext()) {
                    Unit invokedUnit = invokedIt.next();
                    if (invokedUnit instanceof JReturnStmt || invokedUnit instanceof JReturnVoidStmt) {
                        //最关键，bug找了好久，要在函数调用最后加GOTO！！否则图是错的
                        JGotoStmt gotoStmt = new JGotoStmt(unitList.get(i + 1));
                        invokedChain.insertBefore(gotoStmt, invokedUnit);
                        invokedChain.remove(invokedUnit); //移除返回语句，否则图不对，直接终结了
                    }
                }
                units.insertOnEdge(invokedChain, unit, null); //将被调用的语句拼接到大图中
                units.remove(unit);//移除调用语句
            }


            //最后的返回值要保存
            if (unit instanceof JAssignStmt && ((JAssignStmt) unit).containsInvokeExpr()) {
                getCaller(((JAssignStmt) unit).getInvokeExprBox().getValue());
                InvokeExpr invokeExpr = ((JAssignStmt) unit).getInvokeExpr();  //r2.<cut.LogicStructure: int crazyFun(int,int)>(i9, i1)
                Value caller = ((JAssignStmt) unit).getLeftOp(); //赋值语句的左侧，用于存储返回值
                Body invokedBody = SootCFG.getMethodBody(invokeExpr.getMethod().getDeclaringClass().getName(), invokeExpr.getMethod().getSignature());
                List<Unit> initAssigns = new ArrayList<>();
                for (Local local : invokedBody.getLocals()) {
                    local.setName(caller + "X" + local.getName());
                    System.out.println(local.getName());
                }
                System.out.println(invokedBody);
                List<Value> args = invokeExpr.getArgs();
                for (int j = 0; j < args.size(); j++)
                    initAssigns.add(new JAssignStmt(args.get(j), invokedBody.getParameterLocal(j)));//调用将实参赋值
                UnitPatchingChain invokedChain = invokedBody.getUnits(); //被调用函数的语句
                //将赋值的操作加到被调用语句的前面
                if (!initAssigns.isEmpty()) invokedChain.insertBefore(initAssigns, invokedChain.getFirst());
                Iterator<Unit> invokedIt = invokedChain.snapshotIterator();
                while (invokedIt.hasNext()) {
                    Unit invokedUnit = invokedIt.next();
                    if (invokedUnit instanceof JReturnStmt) {
                        JReturnStmt retStmt = (JReturnStmt) invokedUnit;
                        Value retVal = retStmt.getOp();
                        //最后将返回值赋值给调用的地方
                        invokedChain.insertBefore(new JAssignStmt(caller, retVal), retStmt);
                        JGotoStmt gotoStmt = new JGotoStmt(unitList.get(i + 1));
                        invokedChain.insertBefore(gotoStmt, retStmt);
                        invokedChain.remove(retStmt); //移除返回语句
                    }
                }
                units.insertOnEdge(invokedChain, unit, null);
                units.remove(unit);//移除调用语句
            }
        }
    }

    private static Value getCaller(Value jInvokeValue) {
        if (jInvokeValue instanceof JSpecialInvokeExpr) {
            JSpecialInvokeExpr jSpecialInvokeExpr = (JSpecialInvokeExpr) jInvokeValue;
            return jSpecialInvokeExpr.getBase();
        } else if (jInvokeValue instanceof JVirtualInvokeExpr) {
            JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) jInvokeValue;
            return jVirtualInvokeExpr.getBase();
        }
        return null; //TODO 目前只处理了两种invoke
    }


    private Set<List<Unit>> result = new HashSet<>();

    public Set<List<Unit>> getSearchPathResult() {
        return result;
    }

    //向上或向下扩招到目的点
    public void findPath(UnitGraph ug, Unit startU, Unit targetU, List<Unit> path, boolean back/*back true forward false*/) {
        int threshold;
        List<Unit> nextUnits;
        if (back) {
            threshold = StaticsUtil.MOST_BACKWARD_FIND;
            nextUnits = ug.getPredsOf(startU);
        } else {
            threshold = StaticsUtil.MOST_FORWARD_FIND;
            nextUnits = ug.getSuccsOf(startU);
        }
        if (result.size() >= threshold) //已经找到一定数量了，不用再找了
            return;
        path.add(startU);
        //找到了目标
        if (startU.equals(targetU)) {
            result.add(path);
            return;
        }
        for (Unit next : nextUnits) {
            ArrayList<Unit> pathClone = new ArrayList<>(path); //克隆
            findPath(ug, next, targetU, pathClone, back);
        }
    }
}
