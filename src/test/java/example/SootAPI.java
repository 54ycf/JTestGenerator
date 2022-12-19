package example;

import org.junit.jupiter.api.Test;
import soot.*;
import soot.jimple.JimpleBody;
import soot.options.Options;
import soot.toolkits.graph.ClassicCompleteUnitGraph;
import soot.toolkits.graph.UnitGraph;

import java.io.File;
import java.util.List;

public class SootAPI {

    @Test
    public void JimpleDemo() {

        String sourceDirectory = System.getProperty("user.dir") + File.separator + "target" + File.separator + "test-classes";
        System.out.println(sourceDirectory);
        String clsName = "cut.LogicStructure";
        String methodName = "multipleIf";
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

        //print the jimpleBody
        System.out.println("--------------");
        System.out.println("JimpleBody:");
        System.out.println(body.toString());

        //print details of the locals
        System.out.println("--------------");
        System.out.println("Locals:");
        int clocals = 1;
        for (Local l : body.getLocals()) {
            System.out.println("(" + clocals + ") " + l.toString() + ": ");
            System.out.println("****Class : " + l.getClass().toString());
            System.out.println("****Name : " + l.getName());
            System.out.println("****Type : " + l.getType());
            System.out.println("****Number : " + l.getNumber());
            System.out.println("****UseBoxes : " + l.getUseBoxes());
            clocals++;
        }

        //print details of the jimple statement
        System.out.println("--------------");
        System.out.println("Units:");
        int cd = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + cd + ") " + u.toString() + ": ");
            System.out.println("****Class: " + u.getClass().toString());
            System.out.println("****JavaSourceStartColumnNumber: " + u.getJavaSourceStartColumnNumber());
            System.out.println("****JavaSourceStartLineNumber: " + u.getJavaSourceStartLineNumber());
            System.out.println("****Tags: " + u.getTags().toString());
            cd++;
        }

        // Print some information about method
        //System.out.println("Method Signature: " + sm.getSubSignature());
        System.out.println("Method Signature: " + sm.getSignature());
        System.out.println("--------------");
        System.out.println("Argument(s):");
        for (Local l : body.getParameterLocals()) {
            System.out.println(l.getName() + " : " + l.getType());
        }
        System.out.println("--------------");
        System.out.println("This: " + body.getThisLocal());
        System.out.println("--------------");
        System.out.println("Units:");
        int cu = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + cu + ") " + u.toString());
            cu++;
        }

        System.out.println("--------------");
        System.out.println("Units jumping to this unit:");
        int cdu = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + cdu + ") " + u.getBoxesPointingToThis());
            cdu++;
        }

        System.out.println("--------------");
        System.out.println("Units this unit is jumping to:");
        int csu = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + csu + ") " + u.getUnitBoxes());
            csu++;
        }

        System.out.println("--------------");
        System.out.println("values defined in this unit");
        int cdefine = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + cdefine + ") " + u.getDefBoxes());
            cdefine++;
        }

        System.out.println("--------------");
        System.out.println("values used in this unit");
        int c_used = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + c_used + ") " + u.getUseBoxes());
            c_used++;
        }

        System.out.println("--------------");
        System.out.println("values used and defined in this unit");
        int c_used_and_defined = 1;
        for (Unit u : body.getUnits()) {
            System.out.println("(" + c_used_and_defined + ") " + u.getUseAndDefBoxes());
            c_used_and_defined++;
        }

        //控制流图中的路径信息
        System.out.println("--------------------------------------------------------");
        System.out.println("--------------------------------------------------------");
        System.out.println("--------------------------------------------------------");
        UnitGraph ug = new ClassicCompleteUnitGraph(sm.retrieveActiveBody());
        System.out.println("heads:");
        int c_head = 1;
        for (Unit h : ug.getHeads()) {
            System.out.println("(" + c_head + ") " + h.toString());
            c_head++;
        }

        System.out.println("tails:");
        int c_tail = 1;
        for (Unit t : ug.getTails()) {
            System.out.println("(" + c_tail + ") " + t.toString());
            c_tail++;
        }

        System.out.println("Basic path between heads and tails:");
        for (Unit h : ug.getHeads()) {
            for (Unit t : ug.getTails()) {
                List<Unit> basicBlockPath = ug.getExtendedBasicBlockPathBetween(h,t);
                System.out.println(basicBlockPath.toString());
            }
        }
    }
}


