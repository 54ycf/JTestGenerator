package cut;

import jtg.MyEnum;
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

    //测试循环，基本类型
    public int test001(int op) {
        int sum = 0;
        for (int i = 0; i < op; i++) {
            sum += i;
        }
        return sum;
    }

    //测试string
    public int test002(String str) {
        if (str.length() < 10) {
            return 1;
        }else if (str.length() < 20){
            return 0;
        }else {
            return -1;
        }
    }

    //测试数组
    public int test003(int[] arr) {
        int sum = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i] > 0) {
                sum += arr[i];
            }else {
                sum -= arr[i];
            }
        }
        return sum;
    }

    //测试枚举类
    public int test004(MyEnum myEnum) {
        if (myEnum== MyEnum.a) {
            return -1;
        } else if (myEnum== MyEnum.b) {
            return 1;
        } else {
            return 0;
        }
    }

    //测试普通类
    public int test005(Person person) {
        int temp = person.getAge();
        if (person.getAge() > 0) {
            return test001(temp);
        }else {
            return test001(-temp);
        }
    }

    //测试函数调用
    public int test006(boolean op) {
        if (op ) {
            op = false;
            return test001(1);
        }else {
            op = true;
            return test001(2);
        }
    }

    //复杂类型的数组
    public int test007(Person[] people) {
        int sum = 0;
        for (int i = 0; i < people.length; i++) {
            sum += people[i].getAge();
        }
        return sum;
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
        while (isPat == false  && iSub+patternLen-1<subjectLen) {
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

    public int charTest(String str) {
        if (str.equals("abc")) {
            return 1;
        }
        return 2;
    }
}

