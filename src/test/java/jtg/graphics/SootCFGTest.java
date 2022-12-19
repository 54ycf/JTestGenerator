package jtg.graphics;

import org.junit.jupiter.api.Test;
import soot.toolkits.graph.UnitGraph;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class SootCFGTest {

    @Test
    void solo_if_cfg_success() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "soloIf";
        UnitGraph ug = SootCFG.getMethodCFG(clspath,clsName,methodName);
        assertNotNull(ug);
    }

    @Test
    void if_else_cfg_success() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "ifElse";
        UnitGraph ug = SootCFG.getMethodCFG(clspath,clsName,methodName);
        assertNotNull(ug);
    }

    @Test
    void multiple_if_cfg_success() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "multipleIf";
        UnitGraph ug = SootCFG.getMethodCFG(clspath,clsName,methodName);
        assertNotNull(ug);
    }
}