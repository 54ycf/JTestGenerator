package jtg.visualizer;

import soot.*;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JInvokeStmt;
import soot.jimple.internal.JVirtualInvokeExpr;
import soot.toolkits.graph.UnitGraph;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public class Visualizer {

    private static PrintStream ps;

    /**
     * filename: 输出dot的文件名
     * unitGraph: Soot生成的控制流图
     * indexLabel: dot中显示节点序号(true)，还是Soot代码(false)
     **/

    public static void printCFGDot(String filename, UnitGraph unitGraph, boolean indexLabel) {
        try {
            ps = new PrintStream(new FileOutputStream(filename));

            ps.println("digraph G {");
            int index = 0;
            for (Unit unit : unitGraph) {
                index += 1;
                String aid = getID(unit);
                String label = indexLabel ? String.valueOf(index) : getLabelFromUnit(unit);
                ps.println("\t" + aid + "[label=\"" + label + "\"" + "]");
            }

            for (Unit unit : unitGraph) {
                for (Unit suc : unitGraph.getSuccsOf(unit)) {
                    String aid = getID(unit);
                    String bid = getID(suc);
                    ps.println("\t" + aid + "->" + bid + ";");
                }
            }
            ps.println("}");
            ps.close();
            Dot.showDot(filename);
        } catch (IOException exx) {
            TGConsole.out.println("printDOT failed: " + exx.toString());
        }
    }

    //有缺陷还未调试
    private static String getLabelFromUnit(Unit unit) {
        StringBuilder label;
        label = new StringBuilder(unit.toString());
//        System.out.println("The original label: " + label.toString());
        if (unit instanceof JAssignStmt) {
            JAssignStmt jAssignStmt = (JAssignStmt) unit;
            if (jAssignStmt.containsInvokeExpr() && jAssignStmt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
                label = new StringBuilder(jAssignStmt.getLeftOp().toString() + "=");
                JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) jAssignStmt.getInvokeExpr();
                label.append(jVirtualInvokeExpr.getBase()).append(".").append(jVirtualInvokeExpr.getMethod().getName());
                if (jVirtualInvokeExpr.getArgCount() > 0) {
                    label.append("(");
                    for (Value v : jVirtualInvokeExpr.getArgs()) {
                        label.append(v.toString()).append(",");
                    }
                    label = new StringBuilder(label.substring(0, label.length() - 1));
                    label.append(")");
                }
            }
        }
        if (unit instanceof JInvokeStmt) {
            JInvokeStmt jInvokeStmt = (JInvokeStmt) unit;
            if (jInvokeStmt.containsInvokeExpr() && jInvokeStmt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
                JVirtualInvokeExpr jVirtualInvokeExpr = (JVirtualInvokeExpr) jInvokeStmt.getInvokeExpr();
                label = new StringBuilder(jVirtualInvokeExpr.getBase() + "." + jVirtualInvokeExpr.getMethod().getName());
                if (jVirtualInvokeExpr.getArgCount() > 0) {
                    label.append("(");
                    for (Value v : jVirtualInvokeExpr.getArgs()) {
                        label.append(v.toString()).append(",");
                    }
                    label = new StringBuilder(label.substring(0, label.length() - 1));
                    label.append(")");
                }
            }
        }
        label = new StringBuilder(label.toString()
                .replace("java.lang.", "")
                .replace("java.io.", "")
                .replace("\"", ""));//bug:如果sootcode中包含""，dot生成图像出错
//        System.out.println("The filtered label: " + label.toString());
        return label.toString();
    }

    private static String getID(Object a) {
//      String id = Integer.toString(System.identityHashCode(a));
        String id = Integer.toString(a.hashCode());
        StringBuilder newId = new StringBuilder();
        for (int i = 0; i < id.length(); i++) {
            newId.append((char) (97 + id.codePointAt(i) - "0".codePointAt(0)));
        }
        return newId.toString();
    }
}
