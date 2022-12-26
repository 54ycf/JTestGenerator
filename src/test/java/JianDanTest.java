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
    //基路径生成算法
//    @Test
//    public void testPrimePath(){
//        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
//        String clsName = "cut.LogicStructure";
//        String methodName = "pat";
//        DDLPrimePathCal cal = new DDLPrimePathCal(clspath, clsName, methodName);
//        Set<List<Unit>> lists = cal.generatePrimePathUnit();
//        int i = 0;
//        for (List<Unit> list : lists) {
//            System.out.println("***************prime path " + i++ + "**************");
//            for (Unit unit : list) {
//                System.out.println(unit);
//            }
//        }
//    }

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
        String methodName = "multipleIf";
//        GeneralGenerator gg = new GeneralGenerator(clspath, clsName, methodName);
//        gg.generate();
    }

    @Test
    public void testRandomGen() throws Exception {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "test007";
        RandomGenerator generator = new RandomGenerator(clspath, clsName, methodName);
        generator.generate();
    }

    @Test
    public void testStmtCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "test005";
        StmtCovGenerator generator = new StmtCovGenerator(clspath, clsName, methodName);
        generator.generate();
    }

    @Test
    public void testPrimePathCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "loopAll";
        PrimePathCovGenerator generator = new PrimePathCovGenerator(clspath, clsName, methodName);
        generator.generate();
    }

    @Test
    public void testBranchCov(){
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "loopAll";
        BranchCovGenerator generator = new BranchCovGenerator(clspath, clsName, methodName);
        generator.generate();
    }
}
