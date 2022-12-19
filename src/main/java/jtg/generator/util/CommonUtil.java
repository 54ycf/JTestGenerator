package jtg.generator.util;

import java.util.List;

public class CommonUtil {
    public static boolean leftIsSubList(List left, List right) {
        if (right.size() < left.size()) return false;
        int lp = 0;
        int rp = 0;
        while (lp != left.size() && rp!=right.size()) {
            if (right.get(rp).equals(left.get(lp))) {
                ++rp;
                ++lp;
            }else {
                ++rp;
                lp = 0;
            }
        }
        return lp == left.size();
    }
}
