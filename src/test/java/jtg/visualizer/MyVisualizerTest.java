package jtg.visualizer;

import org.junit.jupiter.api.Test;
import soot.G;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;

public class MyVisualizerTest {
    @Test
    void displayLastMsg_cfgDot(){
        draw("crazyFun");
    }

    private void draw(String mtdName) {
        String sourceDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        System.out.println(sourceDirectory);

        String clsName = "cut.LogicStructure";
        String methodName = mtdName;
        //setupSoot
        System.out.println(sourceDirectory);
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();

        // Retrieve method's body
        SootClass mainClass = Scene.v().getSootClass(clsName);
        SootMethod sm = mainClass.getMethodByName(methodName);
        JimpleBody body = (JimpleBody) sm.retrieveActiveBody();

        UnitGraph ug = new ClassicCompleteUnitGraph(sm.retrieveActiveBody());
        Visualizer.printCFGDot(mtdName + "_cfg_label_with_index",ug,true);
        Visualizer.printCFGDot(mtdName +"_cfg_label_with_SootCode",ug,false);
    }
}
