package at.hoenisch.lp_solver;

import net.sf.javailp.*;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * User: Philipp Hoenisch
 * Date: 4/3/14
 */
public class ExampleServiceTest {


    /**
     * Min 5 x + 10 y
     * Subject to
     * 3 x + 1 y >= 8
     * 4y >= 4
     * 2x <= 2
     */
    @Test
    public void test1() {

        Problem problem = new Problem();

        Linear linear = new Linear();
        linear.add(5, "x");
        linear.add(10, "y");

        problem.setObjective(linear, OptType.MIN);

        linear = new Linear();
        linear.add(3, "x");
        linear.add(1, "y");

        problem.add(linear, ">=", 8);

        linear = new Linear();
        linear.add(4, "y");

        problem.add(linear, ">=", 4);

        linear = new Linear();
        linear.add(2, "x");

        problem.add(linear, "<=", 2);


        LPSolverService lPSolverService = new LPSolverService();
        Result result = lPSolverService.solveProblem(problem);
        Number objective = result.getObjective();
        Number x = result.get("x");
        Number y = result.get("y");
        assertThat(objective.doubleValue(), is(55.0));
        assertThat(x.doubleValue(), is(1.0));
        assertThat(y.doubleValue(), is(5.0));
    }
}
