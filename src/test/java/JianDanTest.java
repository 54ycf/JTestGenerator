import jtg.generator.*;
import jtg.generator.path.DDLPrimePathCal;
import org.junit.jupiter.api.Test;
import soot.Unit;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIfStmt;
import soot.jimple.internal.JReturnStmt;

import java.io.File;
import java.util.*;

public class JianDanTest {
    //扩展
    @Test
    public void testInsertBefore(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "multipleIf";
//        GeneralGenerator gg = new GeneralGenerator(clspath, clsName, methodName);
//        gg.generate();
    }

    //我的方法生成的
    @Test
    public void testCalPathConstraint(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "pat";
        StmtCovGenerator generator = new StmtCovGenerator(clspath, clsName, methodName);
        generator.generate();
    }

    @Test
    public void testRandomGen() throws Exception {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        for (int i = 1; i <= 8; i++) {
            String methodName = "test00" + i;
            RandomGenerator generator = new RandomGenerator(clspath, clsName, methodName);
            generator.generate();
        }
    }

    @Test
    public void testStmtCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        for (int i = 1; i <= 8; i++) {
            String methodName = "test00" + i;
            StmtCovGenerator generator = new StmtCovGenerator(clspath, clsName, methodName);
            generator.generate();
        }
    }

    @Test
    public void testPrimePathCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        for (int i = 1; i <= 8; i++) {
            String methodName = "test00" + i;
            PrimePathCovGenerator generator = new PrimePathCovGenerator(clspath, clsName, methodName);
            generator.generate();
        }
    }

    @Test
    public void testBranchCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        for (int i = 1; i <= 8; i++) {
            String methodName = "test00" + i;
            BranchCovGenerator generator = new BranchCovGenerator(clspath, clsName, methodName);
            generator.generate();
        }
    }
}
