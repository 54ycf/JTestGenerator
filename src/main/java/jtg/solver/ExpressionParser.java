package jtg.solver;

public class ExpressionParser {

    // 当前分析的表达式
    private String expression;

    // 当前读取的位置
    private int position;

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public ExpressionParser(String expression) {
        this.expression = expression;
        this.position = 0;
    }

    /**
     * 读取下一个表达式节点,如果读取失败则返回null
     *
     * @return
     */
    public ExpressionNode readNode() {
        ExpressionNode s = new ExpressionNode(null);
        // 空格的位置
        int whileSpacePos = -1;
        boolean flag = false;
        StringBuffer buffer = new StringBuffer(10);
        StringBuffer temp = new StringBuffer(10);
        while (this.position < this.expression.length()) {
            char c = this.expression.charAt(this.position);
            if (IsLetter(c)) {
                if (buffer.toString().length() > 0) {
                    break;
                }
                buffer.append(c);
                this.position++;
                while (this.position < this.expression.length() && (IsLetter(this.expression.charAt(this.position)) || IsDigit(this.expression.charAt(this.position)))) {
                    char b = this.expression.charAt(this.position);
                    buffer.append(b);
                    this.position++;
                }
                break;
            } else {
                if (s.IsWhileSpace(c)) {
                    if ((whileSpacePos >= 0) && ((this.position - whileSpacePos) > 1)) {
                        throw new ExpressionException(
                                String.format("表达式\"%s\"在位置(%s)上的字符非法!", this.getExpression(), this.getPosition()));
                    }
                    if (buffer.length() == 0) {
                        whileSpacePos = -1;
                    } else {
                        whileSpacePos = this.position;
                    }
                    this.position++;
                    if (this.expression.length() > this.position && buffer.toString().length() <= 0) {
                        continue;
                    } else {
                        break;
                    }

                }
                if (((buffer.length() == 0) || s.IsCongener(c, buffer.charAt(buffer.length() - 1))) && valiateTwo(buffer.toString())) {
                    this.position++;
                    buffer.append(c);
                } else {
                    break;
                }
                if (!s.needMoreOperator(c)) {
                    break;
                }
            }
        }
        if (buffer.length() == 0) {
            return null;
        }
        ExpressionNode node = new ExpressionNode(buffer.toString());
        if (node.getType() == ExpressionNodeType.Unknown) {
            throw new ExpressionException(String.format("表达式\"%s\"在位置%s上的字符\"%s\"非法!", this.getExpression(),
                    this.getPosition() - node.getValue().length(), node.getValue()));
        }
        return node;
    }

    private boolean IsLetter(char letter) {//注意C语言允许下划线也为标识符的一部分可以放在首部或其他地方
        if (letter >= 'a' && letter <= 'z' || letter >= 'A' && letter <= 'Z' || letter == '_') {
            return true;
        } else {
            return false;
        }
    }

    private boolean IsDigit(char digit) {
        if (digit >= '0' && digit <= '9') {
            return true;
        } else {
            return false;
        }
    }

    public static boolean valiateTwo(StringBuffer buffer, char c) {
        String value = buffer.append(c).toString();
        if ("&&".equals(value) || "||".equals(value) || "!||".equals(value)) {
            return false;
        } else {
            return true;
        }
    }

    public static boolean valiateTwo(String value) {
        if ("&&".equals(value) || "||".equals(value) || "!||".equals(value)) {
            return false;
        } else {
            return true;
        }
    }
}