package example;

import com.microsoft.z3.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

public class Z3API {

    @Test
    public void testZ3() {
        try {
            HashMap<String, String> cfg = new HashMap<String, String>();
            cfg.put("model", "true");
            Context ctx = new Context(cfg);
            Solver s = ctx.mkSolver();
            RealExpr a = ctx.mkRealConst("a");
            RealExpr c = ctx.mkRealConst("c");
            RealExpr d = ctx.mkRealConst("d");
            RealExpr e = ctx.mkRealConst("e");
            RealExpr x = ctx.mkRealConst("x");
            RealExpr y = ctx.mkRealConst("y");
            ArithExpr left = ctx.mkAdd(x, ctx.mkMul(y, c, e));
            ArithExpr right = ctx.mkAdd(d, a);
            BoolExpr equation = ctx.mkEq(left, right);
            s.add(equation);
            s.add(ctx.mkGt(a, ctx.mkReal(0)));
            Status result = s.check();
            if (result == Status.SATISFIABLE) {
                System.out.println("model for: x + y*c*e = d + a, a > 0");
                System.out.print(s.getModel());
                System.out.println(a.toString());
                Expr a_value = s.getModel().evaluate(a, false);
                System.out.println(a_value.toString());
            } else if (result == Status.UNSATISFIABLE)
                System.out.println("unsat");
            else
                System.out.println("unknow");

        } catch (Exception e) {
            System.out.println("z3 exception");
            e.printStackTrace();
        }
    }
}
