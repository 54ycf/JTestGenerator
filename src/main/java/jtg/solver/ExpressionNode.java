package jtg.solver;

import org.apache.commons.lang3.StringUtils;

public class ExpressionNode {

        private String value;

        private ExpressionNodeType type;

        private int pri;

        private ExpressionNode unitaryNode;

        private Object numeric;

        public ExpressionNode(String value) {
            this.value = value;
            this.type = parseNodeType(value);
            this.pri = getNodeTypePRI(this.type);
            this.numeric = null;
        }

        public Object getNumeric() {
            if (this.numeric == null) {

                if ((this.type == ExpressionNodeType.String) || (this.type == ExpressionNodeType.Date)) {
                    return this.value;
                }

                if (this.type != ExpressionNodeType.Numeric) {
                    return 0;
                }
                Double num = new Double(this.value);
                if (this.unitaryNode != null && this.unitaryNode.type == ExpressionNodeType.Subtract) {
                    num = 0 - num;
                }
                this.numeric = num;
            }
            return numeric;
        }

        public void setNumeric(Object numeric) {
            this.numeric = numeric;
            this.value = this.numeric.toString();
        }

        public void setUnitaryNode(ExpressionNode unitaryNode) {
            this.unitaryNode = unitaryNode;
        }

        private ExpressionNodeType parseNodeType(String value) {
            if (StringUtils.isEmpty(value)) {
                return ExpressionNodeType.Unknown;
            }
            switch (value) {
                case "+":
                    return ExpressionNodeType.Plus;
                case "-":
                    return ExpressionNodeType.Subtract;
                case "*":
                    return ExpressionNodeType.MultiPly;
                case "/":
                    return ExpressionNodeType.Divide;
                case "%":
                    return ExpressionNodeType.Mod;
                case "^":
                    return ExpressionNodeType.Power;
                case "(":
                    return ExpressionNodeType.LParentheses;
                case ")":
                    return ExpressionNodeType.RParentheses;
                case "&":
                    return ExpressionNodeType.BitwiseAnd;
                case "|":
                    return ExpressionNodeType.BitwiseOr;
                case "&&":
                    return ExpressionNodeType.And;
                case "||":
                    return ExpressionNodeType.Or;
                case "!||":
                    return ExpressionNodeType.Xor;
                case "!":
                    return ExpressionNodeType.Not;
                case "==":
                case "=":
                    return ExpressionNodeType.Equal;
                case "!=":
                case "<>":
                case "≠":
                    return ExpressionNodeType.Unequal;
                case ">":
                    return ExpressionNodeType.GT;
                case "<":
                    return ExpressionNodeType.LT;
                case ">=":
                case "≥":
                    return ExpressionNodeType.GTOrEqual;
                case "<=":
                case "≤":
                    return ExpressionNodeType.LTOrEqual;
                case "<<":
                    return ExpressionNodeType.LShift;
                case ">>":
                    return ExpressionNodeType.RShift;
                case "@":
                    return ExpressionNodeType.Like;
                case "!@":
                    return ExpressionNodeType.NotLike;
                case "!!$":
                    return ExpressionNodeType.StartWith;
                case "!!@":
                    return ExpressionNodeType.EndWith;

            }
            if (isNumerics(value)) {
                return ExpressionNodeType.Numeric;
            }
            if (isDatetime(value)) {
                return ExpressionNodeType.Date;
            }
            if (valiateString(value)) {
                return ExpressionNodeType.String;
            }
            return ExpressionNodeType.Unknown;
        }

        private boolean valiateString(String value) {
            char[] arr = value.toCharArray();
            for (char c : arr) {
                if (!(c == 95 || (c >= 65 && c <= 90) || (c >= 97 && c <= 122) || c >= 48 && c <= 57)) {
                    return false;
                }
            }
            return true;
        }

        private int getNodeTypePRI(ExpressionNodeType nodeType) {
            switch (nodeType) {
                case LParentheses:
                case RParentheses:
                    return 9;
                // 逻辑非是一元操作符,所以其优先级较高
                case Not:
                    return 8;
                case Mod:
                    return 7;
                case MultiPly:
                case Divide:
                case Power:
                    return 6;
                case Plus:
                case Subtract:
                    return 5;
                case LShift:
                case RShift:
                    return 4;
                case BitwiseAnd:
                case BitwiseOr:
                    return 3;
                case Equal:
                case Unequal:
                case GT:
                case LT:
                case GTOrEqual:
                case LTOrEqual:
                case Like:
                case NotLike:
                case StartWith:
                case EndWith:
                    return 2;
                case And:
                case Or:
                case Xor:
                    return 1;
                default:
                    return 0;
            }

        }

        public boolean isNumerics(String op) {
            return op.matches("^[\\+\\-]?(0|[1-9]\\d*|[1-9]\\d*\\.\\d+|0\\.\\d+)");
        }

        public static boolean isDatetime(String op) {
            op = op.replace("\"", "").trim();
            return op.matches("\\d{4}\\-\\d{2}\\-\\d{2}(\\s\\d{2}\\:\\d{2}\\:\\d{2})?");
        }

        public boolean needMoreOperator(char c) {
            switch (c) {
                case '&':
                case '|':
                case '=':
                case '!':
                case '>':
                case '<':
                case '.': // 小数点
                    return true;
            }
            // //数字则需要更多
            return Character.isDigit(c);
        }

        public boolean IsCongener(char c1, char c2) {
            if ((c1 == '(') || (c2 == '(')) {
                return false;
            }
            if ((c1 == ')') || (c2 == ')')) {
                return false;
            }
            if ((c1 == '"') || (c2 == '"')) {
                return false;
            }
            if (Character.isDigit(c1) || (c1 == '.')) {
                // c1为数字,则c2也为数字
                return (Character.isDigit(c2) || (c2 == '.'));
            }
            return (!Character.isDigit(c2) && (c2 != '.'));
        }

        public boolean IsWhileSpace(char c) {
            return c == ' ' || c == '\t';
        }


        public static boolean IsUnitaryNode(ExpressionNodeType nodeType) {
            return (nodeType == ExpressionNodeType.Plus || nodeType == ExpressionNodeType.Subtract);
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public ExpressionNodeType getType() {
            return type;
        }

        public void setType(ExpressionNodeType type) {
            this.type = type;
        }

        public int getPri() {
            return pri;
        }

        public void setPri(int pri) {
            this.pri = pri;
        }

        public ExpressionNode getUnitaryNode() {
            return unitaryNode;
        }
    }