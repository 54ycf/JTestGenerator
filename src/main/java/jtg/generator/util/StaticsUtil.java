package jtg.generator.util;

public class StaticsUtil {
    public static int MOST_RECUR = 0;
    public static int MOST_BACKWARD_FIND = 10;
    public static int MOST_FORWARD_FIND = 10;

    private static int RAND = 0;

    public static int genMark(){
        return RAND++;
    }
}
