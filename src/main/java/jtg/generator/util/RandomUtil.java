package jtg.generator.util;

import java.util.Random;
import java.util.UUID;

public class RandomUtil {

    private static Random random = new Random();
    public static int randInt(){
        return random.nextInt();
    }

    public static int randInt(int bound) {
        return random.nextInt(bound);
    }
    public static double randDouble(){
        return random.nextDouble();
    }

    public static char randChar(){
        return (char) random.nextInt(65536);
    }

    public static boolean randBool(){
        return random.nextBoolean();
    }

    public static String randStr(){
        String s = UUID.randomUUID().toString();
        return s.substring(0,randInt(s.length()));
    }


}
