package jtg.solver;

import jtg.MyEnum;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

class Z3SolverTest {

    @Test
    void sat_two_ints_add() throws Exception{
//        String result = Z3Solver.solve("!( 0 >= r0_len  ) && !( r0_ind0  <= 0  ) && !(  ( 0 ) + ( 1 )  >= r0_len  ) && !( r0_ind1  <= 0  ) && !(  (  ( 0 ) + ( 1 )  ) + ( 1 )  >= r0_len  ) && !( r0_ind2  <= 0  ) && !(  (  (  ( 0 ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  >= r0_len  ) && !( r0_ind3  <= 0  ) && !(  (  (  (  ( 0 ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  >= r0_len  ) && !( r0_ind4  <= 0  ) && (  (  (  (  (  ( 0 ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  ) + ( 1 )  >= r0_len  )");
//        System.out.println("result is");
//        System.out.println(result);
//        assertThat(result,containsString("a="));
//        assertThat(result,containsString("b="));
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            System.out.println(random.nextInt(2));
        }
    }
}