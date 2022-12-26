package jtg.generator.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CommonUtil {
    public static boolean leftIsSubList(List left, List right) {
        if (right.size() < left.size()) return false;
//        int lp = 0;
//        int rp = 0;
//        while (lp != left.size() && rp!=right.size()) {
//            if (right.get(rp).equals(left.get(lp))) {
//                ++rp;
//                ++lp;
//            }else {
//                ++rp;
//                lp = 0;
//            }
//        }
//        return lp == left.size();
        List rightCp = new ArrayList(right);
        rightCp.remove(0);
        return leftIsHeadOfRight(left, right) || leftIsSubList(left, rightCp);
    }

    public static boolean leftIsHeadOfRight(List left, List right) {
        if (left.size() > right.size()) return false;
        for (int i = 0; i < left.size(); i++) {
            if (!left.get(i).equals(right.get(i))) {
                return false;
            }
        }
        return true;
    }
}
