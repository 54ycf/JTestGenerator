package jtg.solver;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;

public class ExpressionEvaluator {

    public ExpressionEvaluator() {

    }

    private static List<ExpressionNode> parseExpression(String expression) {
        if (StringUtils.isEmpty(expression)) {
            return new ArrayList<ExpressionNode>();
        }

        List<ExpressionNode> listOperator = new ArrayList<ExpressionNode>(10);
        Stack<ExpressionNode> stackOperator = new Stack<ExpressionNode>();

        ExpressionParser expParser = new ExpressionParser(expression);
        ExpressionNode beforeExpNode = null; // 前一个节点
        ExpressionNode unitaryNode = null; // 一元操作符
        ExpressionNode expNode;

        while ((expNode = expParser.readNode()) != null) {
            if ((expNode.getType() == ExpressionNodeType.Numeric)
                    || (expNode.getType() == ExpressionNodeType.String)
                    || (expNode.getType() == ExpressionNodeType.Date)) {
                listOperator.add(expNode);
                continue;
            } else if (expNode.getType() == ExpressionNodeType.LParentheses) {
                // 左括号， 直接加入操作符栈
                stackOperator.push(expNode);
                continue;
            } else if (expNode.getType() == ExpressionNodeType.RParentheses) {
                // 右括号则在操作符栈中反向搜索，直到遇到匹配的左括号为止，将中间的操作符依次加到后缀表达式中。
                ExpressionNode lpNode = null;
                while (stackOperator.size() > 0) {
                    lpNode = stackOperator.pop();
                    if (lpNode.getType() == ExpressionNodeType.LParentheses) {
                        break;
                    }
                    listOperator.add(lpNode);
                }
                if (lpNode == null || lpNode.getType() != ExpressionNodeType.LParentheses) {
                    throw new ExpressionException(String.format("在表达式\"%s\"中没有与在位置(%s)上\")\"匹配的\"(%s)\"字符!", expParser.getExpression(), expParser.getPosition()));
                }
            } else {
                if (stackOperator.size() == 0) {
//                        // 第一个节点则判断此节点是否是一元操作符"+,-,!,("中的一个,否则其它都非法
//                        if (listOperator.size() == 0 && !(expNode.getType() == ExpressionNodeType.LParentheses
//                                || expNode.getType() == ExpressionNodeType.Not)) {
//                            // 后缀表达式没有任何数据则判断是否是一元操作数
//                            if (ExpressionNode.IsUnitaryNode(expNode.getType())) {
//                                unitaryNode = expNode;
//                            } else {
//                                // 丢失操作数
//                                throw new ExpressionException(String.format("表达式\"%s\"在位置(%s)上缺少操作数!",
//                                        expParser.getExpression(), expParser.getPosition()));
//                            }
//                        } else {
//                            // 直接压入操作符栈
//                            stackOperator.push(expNode);
//                        }
                    stackOperator.push(expNode);
                    continue;
                } else {
                    do {
                        // 取得上一次的操作符
                        beforeExpNode = stackOperator.peek();

                        // 如果前一个操作符优先级较高，则将前一个操作符加入后缀表达式中
                        if (beforeExpNode.getType() != ExpressionNodeType.LParentheses
                                && (beforeExpNode.getPri() - expNode.getPri()) >= 0) {
                            listOperator.add(stackOperator.pop());
                        } else {
                            break;
                        }

                    } while (stackOperator.size() > 0);

                    // 将操作符压入操作符栈
                    stackOperator.push(expNode);
                }
            }
        }
        // 清空堆栈
        while (stackOperator.size() > 0) {
            // 取得操作符
            beforeExpNode = stackOperator.pop();
            if (beforeExpNode.getType() == ExpressionNodeType.LParentheses) {
                throw new ExpressionException(String.format("表达式\"%s\"中括号不匹配,丢失右括号!", expParser.getExpression(),
                        expParser.getPosition()));
            }
            listOperator.add(beforeExpNode);
        }

        return listOperator;
    }

    /**
     * 对逆波兰表达式进行计算
     *
     * @param nodes
     * @return
     */
    private static Object CalcExpression(List<ExpressionNode> nodes, Set<String> declareBools) throws Exception {
        String expressionZ3 = null;
        if (nodes == null || nodes.size() == 0) {
            return null;
        }

        if (nodes.size() == 1) {
            ExpressionNode expressionNode = nodes.get(0);
            String declareConst = "(declare-const " + expressionNode.getValue() + " Bool)";
            declareBools.add(declareConst);
            String express = "(= " + expressionNode.getValue() + " true)";
            expressionNode.setValue(express);
        }

        if (nodes.size() > 1) {
            int index = 0;
            // 储存数据
            ArrayList values = new ArrayList();
            while (index < nodes.size()) {
                ExpressionNode node = nodes.get(index);
                switch (node.getType()) {
                    // 如果是数字，则将值存入 values 中
                    case Numeric:
                    case Date:
                        values.add(node.getNumeric());
                        index++;
                        break;
                    case String:
                        values.add(node.getValue());
                        index++;
                        break;
                    default:
                        // 二元表达式，需要二个参数， 如果是Not的话，则只要一个参数
                        int paramCount = 2;
                        if (node.getType() == ExpressionNodeType.Not) {
                            paramCount = 1;
                        }
                        // 计算操作数的值
                        if (values.size() < paramCount) {
                            throw new ExpressionException("缺少操作数");
                        }
                        // 传入参数
                        Object[] data = new Object[paramCount];
                        for (int i = 0; i < paramCount; i++) {
                            data[i] = values.get(index - paramCount + i);
                        }
                        // 将计算结果再存入当前节点
                        node.setNumeric(calculate(node.getType(), data, declareBools));
                        node.setType(ExpressionNodeType.Numeric);
                        // 将操作数节点删除
                        for (int i = 0; i < paramCount; i++) {
                            nodes.remove(index - i - 1);
                            values.remove(index - i - 1);
                        }
                        index -= paramCount;
                        break;
                }

            }
        }

        if (nodes.size() != 1) {
            throw new ExpressionException("缺少操作符或操作数");
        }
        switch (nodes.get(0).getType()) {
            case Numeric:
                return nodes.get(0).getNumeric();

            case String:
            case Date:
                return nodes.get(0).getNumeric().toString().replace("\"", "");
        }
        throw new ExpressionException("缺少操作数");
    }

    private static Object calculate(ExpressionNodeType nodeType, Object[] data, Set<String> declareBools) throws Exception {
        Object obj1 = data[0];
        Object obj2 = null;
        if (nodeType != ExpressionNodeType.Not) {
            obj2 = data[1];
        }
        switch (nodeType) {
            case Plus:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(+ " + obj1.toString() + " " + obj2.toString() + ")";
            case Subtract:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(- " + obj1.toString() + " " + obj2.toString() + ")";
            case MultiPly:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(* " + obj1.toString() + " " + obj2.toString() + ")";
            case Divide:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(/ " + obj1.toString() + " " + obj2.toString() + ")";
            case Power:
                return "(^ " + obj1.toString() + " " + obj2.toString() + ")";
            case Mod:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(mod " + obj1.toString() + " " + obj2.toString() + ")";
                //Bug:return "(% " + obj1.toString() + " " + obj2.toString() + ")";
            case BitwiseAnd:
                return "(& " + obj1.toString() + " " + obj2.toString() + ")";
            case BitwiseOr:
                return "(| " + obj1.toString() + " " + obj2.toString() + ")";
            case And:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Bool)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Bool)";
                    declareBools.add(defineExpress);
                }
                return "(and " + obj1.toString() + " " + obj2.toString() + ")";
            case Or:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Bool)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Bool)";
                    declareBools.add(defineExpress);
                }
                return "(or " + obj1.toString() + " " + obj2.toString() + ")";
            case Xor:
                return "(xor " + obj1.toString() + " " + obj2.toString() + ")";
            case Not:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Bool)";
                    declareBools.add(defineExpress);
                }
                return "(not " + obj1.toString() + ")";
            case Equal:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(= " + obj1.toString() + " " + obj2.toString() + ")";
            case Unequal:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(not (= " + obj1.toString() + " " + obj2.toString() + "))";
            case GT:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(> " + obj1.toString() + " " + obj2.toString() + ")";
            case LT:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(< " + obj1.toString() + " " + obj2.toString() + ")";
            case GTOrEqual:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(>= " + obj1.toString() + " " + obj2.toString() + ")";
            case LTOrEqual:
                if (isVariableString(obj1.toString())) {
                    String defineExpress = "(declare-const " + obj1.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                if (isVariableString(obj2.toString())) {
                    String defineExpress = "(declare-const " + obj2.toString() + " Real)";
                    declareBools.add(defineExpress);
                }
                return "(<= " + obj1.toString() + " " + obj2.toString() + ")";
            case LShift:
                return "(<< " + obj1.toString() + " " + obj2.toString() + ")";
            case RShift:
                return "(>> " + obj1.toString() + " " + obj2.toString() + ")";
        }
        return null;
    }

    private static boolean isVariableString(String param) {
        char[] chars = param.toCharArray();
        if ((chars[0] >= 'a' && chars[0] <= 'z') || (chars[0] >= 'A' && chars[0] <= 'Z') || chars[0] == '_') {
            for (char ch : chars) {
                if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch == '_' || (ch >= '0' && ch <= '9')) {
                    continue;
                } else {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    private static boolean isDigital(String param) {
        try {
            Float f = new Float(param);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static Object eval(String expression, Set<String> declareBools) throws Exception {
        List<ExpressionNode> expressionNodes = parseExpression(expression);
        return CalcExpression(expressionNodes, declareBools);
    }

    public static String buildExpression(String expression, Set<String> declareBools) throws Exception {
        Object express = eval(expression, declareBools);
        if (express != null) {
            return "(assert " + express.toString() + ")";
        }
        return null;
    }
}
