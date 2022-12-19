package jtg.solver;

import com.microsoft.z3.BoolExpr;
import com.microsoft.z3.Context;
import com.microsoft.z3.Expr;
import com.microsoft.z3.Solver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class Z3Solver {


       public static String solve(String str) throws Exception {
        Set<String> declareBools = new HashSet<>();
        Set<Expr> varList = new HashSet<>();
        ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();
        String asserts = expressionEvaluator.buildExpression(str, declareBools);
        HashMap<String, String> cfg = new HashMap<String, String>();
        cfg.put("model", "true");
        Context ctx = new Context(cfg);
        Solver s = ctx.mkSolver();
        StringBuilder exprs = new StringBuilder();
        for (String expr : declareBools){
            exprs.append(expr);
            String temp = expr.replaceAll("\\(declare-const ", "").replaceAll(" Real\\)","");
            varList.add(ctx.mkRealConst(temp));
        }
        BoolExpr boolExpr = ctx.parseSMTLIB2String(exprs.toString()+asserts,null,null,null,null)[0];
        s.add(boolExpr);
        StringBuilder res = new StringBuilder();
        try {
            String status = s.check().toString();
            if (status.equals("SATISFIABLE")) {
                for (Expr var : varList) {
                    res.append(var + "=" + s.getModel().eval(var, false) + " ");
                }
            } else {
                res.append("");//无解
            }

        }catch (Exception e){
            res.append(e);
        }
        //System.out.println(res.toString());
        return res.toString();
    }
}
