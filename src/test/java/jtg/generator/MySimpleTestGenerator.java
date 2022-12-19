package jtg.generator;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MySimpleTestGenerator {
    @Test
    void loop_all_correct() {
        String clspath = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        String clsName = "cut.LogicStructure";
        String methodName = "pat";
        SimpleGenerator sg = new SimpleGenerator(clspath, clsName, methodName);
        List<String> ts = sg.generate();
//        assertTrue(!ts.isEmpty());
    }
}
