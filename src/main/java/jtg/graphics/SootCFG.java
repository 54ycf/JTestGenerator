package jtg.graphics;

import soot.*;
import soot.jimple.JimpleBody;
import soot.jimple.toolkits.ide.icfg.JimpleBasedInterproceduralCFG;
import soot.options.Options;
import soot.toolkits.graph.BlockGraph;
import soot.toolkits.graph.BriefBlockGraph;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.Collection;

public class SootCFG {


    public static UnitGraph getMethodCFG(String sourceDirectory,String clsName,String methodName){
        Body body = getMethodBody(sourceDirectory,clsName,methodName);
        UnitGraph ug = new ClassicCompleteUnitGraph(body);
        return ug;
    }

    //块图
    public static BlockGraph getSimpleCFG(String sourceDirectory, String clsName, String methodName){
        Body body = getMethodBody(sourceDirectory,clsName,methodName);
        BlockGraph bg = new BriefBlockGraph(body);
        return bg;
    }

//    public static void NANI(String sourceDirectory,String clsName,String methodName){
//        Options.v().set_whole_program(true);
//        Scene.v().loadNecessaryClasses();
//        PackManager.v().runPacks();
//        JimpleBasedInterproceduralCFG cfg = new JimpleBasedInterproceduralCFG();
//        Collection<Unit> callersOf = cfg.getCallersOf(sm);
//        for (Unit unit : callersOf) {
//            System.out.println("callersOf: " + unit);
//        }
//    }

    public static Body getMethodBody(String sourceDirectory,String clsName,String methodName){
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
//        Options.v().set_whole_program(true);  //TODO 自己加的
        Options.v().set_soot_classpath(sourceDirectory);
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
        SootClass mainClass = Scene.v().getSootClass(clsName);
        SootMethod sm = mainClass.getMethodByName(methodName);
        Body body = sm.retrieveActiveBody();
        return body;
    }

    //mine
    public static Body getMethodBody(String clsName,String signature){
        G.reset();
        Options.v().set_prepend_classpath(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_soot_classpath(System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes");
        SootClass sc = Scene.v().loadClassAndSupport(clsName);
        sc.setApplicationClass();
        Scene.v().loadNecessaryClasses();
        SootMethod sm = Scene.v().getMethod(signature);
        Body body = sm.retrieveActiveBody();
        return body;
    }
}
