package cut;

import jtg.Person;

public class LogicStructure {

    public int sequence(int a, int b) {
        return a + b;
    }

    public boolean soloIf(int op) {
        if (op > 0)
            return true;
        return false;
    }

    public boolean ifElse(int op) {
        if (op > 18)
            return true;
        else
            return false;
    }

    public String multipleIf(int op, Person person) {
        if (op % 15 == 0)
            return "FizzBuzz";
        else if (op % 5 == 0)
            return "Buzz";
        else if (op % 3 == 0)
            return "Fizz";
        else
            return /*Integer.toString(op)*/"";
    }

    public int loopAll(int op) {
        int sum = 0;
        for (int i = 0; i < op; i++) {
            sum = sum + i;
        }
        return sum;
    }

    public int loopAllWhile(int op){
        int start = op;
        int res = 0;
        while (start > 0){
            res = res * start;
            start --;
        }
        res = res + 3;
        return res;
    }

    public int switchCase(int op1, int op2) {
        switch (op1) {
            case 1:
                op2 += 1;break;
            case 2:
                op2 += 2; break;
            default:
                op2 += -1; break;
        }
        op2 += 3;
        return op2;
    }

    public int pat(char[] subject, char[] pattern) {
        final int NotFound = -1;
        int iSub = 0;
        int rtnIndex = NotFound;
        boolean isPat = false;
        int subjectLen = subject.length;
        int patternLen = pattern.length;
        subjectLen = crazyFun(subjectLen,patternLen);
//        subjectLen = new Integer(patternLen);
        while (isPat == false ) {
            if (subject[iSub] == pattern[0]) {
                rtnIndex = iSub;
                isPat = true;
                for (int iPat = 0; iPat < patternLen; iPat++) {
                    if (subject[iSub + iPat] != pattern[iPat]) {
                        rtnIndex = NotFound;
                        isPat = false;
                        break;
                    }
                }
            }
            iSub++;
        }
        return rtnIndex;
    }

    public int crazyFun(int op, int oop) {
        System.out.println(oop);
//        crazyFunFun(op, oop);
        if (op > 0) {
            return op+9999;
        }else {
            return op-9999;
        }
    }

    public void crazyFunFun(int op, int oop) {
        System.out.println(oop);
        if (op > 0) {
            ++op;
            return;
        }else {
            --op;
            return;
        }
    }

}

