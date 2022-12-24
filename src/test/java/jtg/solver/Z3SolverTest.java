package jtg.solver;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class Z3SolverTest {

    @Test
    void sat_two_ints_add() throws Exception{
//        String result = Z3Solver.solve("a + b > 3 && a + b > 2");
//        System.out.println("result is");
//        System.out.println(result);
//        assertThat(result,containsString("a="));
//        assertThat(result,containsString("b="));

        String result = Z3Solver.solve("a < 0 && (a = 0 - 1 || a = 233)");
        System.out.println("result is");
        System.out.println(result);
    }
}