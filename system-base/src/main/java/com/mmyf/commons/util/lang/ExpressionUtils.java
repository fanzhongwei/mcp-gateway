package com.mmyf.commons.util.lang;

import java.util.Stack;

/**
 * package com.mmyf.commons.util <br/>
 * description: 表达式分析器 <br/>
 * Copyright 2019 mmyf, Inc. All rights reserved.
 *
 * @author Teddy
 * @date 2022/6/6
 */
public class ExpressionUtils {
    // 常量定义，表达式中出现的符号
    public static final String C_ADD = "+";

    public static final String C_MIN = "-";

    public static final String C_MUL = "*";

    public static final String C_DIV = "/";

    public static final String C_MOD = "%";

    public static final String C_EQU = "=";

    public static final String C_NOT_EQU = "<>";

    public static final String C_MORE_THAN = ">";

    public static final String C_LESS_THAN = "<";

    public static final String C_MORE_THAN_OR_EQU = ">=";

    public static final String C_LESS_THAN_OR_EQU = "<=";

    public static final String C_LEFT_PARE = "(";

    public static final String C_RIGHT_PARE = ")";

    public static final String C_AND = "&"; // 与符号

    public static final String C_OR = "|"; // 或符号

    public static final String C_BOUND_MARK = "#"; // 表达式开始和结束标记符号

    public static final String C_SPACE = " "; // 空格

    public static final String C_POINT = ".";

    public static final String C_QUOTES = "'";

    public static final String C_ERROR = "ERR"; // 结果错误

    public static final char C_CNUM_ZERO = '0';

    public static final char C_CNUM_NINE = '9';

    /**
     * 判断当前字符是否为运算符
     *
     * @param str
     *            要判断的串
     * @return boolean
     */
    private static boolean isOperator(String str) {
        if (str.equals(C_ADD) || str.equals(C_MIN) || str.equals(C_MUL)
                || str.equals(C_DIV) || str.equals(C_MOD) || str.equals(C_EQU)
                || str.equals(C_NOT_EQU) || str.equals(C_MORE_THAN)
                || str.equals(C_LESS_THAN) || str.equals(C_MORE_THAN_OR_EQU)
                || str.equals(C_LESS_THAN_OR_EQU) || str.equals(C_LEFT_PARE)
                || str.equals(C_RIGHT_PARE) || str.equals(C_AND)
                || str.equals(C_OR) || str.equals(C_BOUND_MARK))
            return true;
        return false;
    }

    /**
     * 判断当前字符是否为运算符（计算器应用）
     *
     * @param str
     *            要判断的串
     * @return true 是运算符;false 不是运算符
     */
    private static boolean isOperator_Cal(String str) {
        if (str.equals(C_ADD) || str.equals(C_MIN) || str.equals(C_MUL)
                || str.equals(C_DIV) || str.equals(C_MOD)
                || str.equals(C_LEFT_PARE) || str.equals(C_RIGHT_PARE)
                || str.equals(C_BOUND_MARK))
            return true;
        return false;
    }

    /**
     * 判断栈顶操作符和当前操作符的优先级
     *
     * @param optr1
     *            栈顶操作符
     * @param optr2
     *            当前操作符
     * @return 表示优先级的字符串
     */
    private static String precedeOperator(String optr1, String optr2) {
        String result = "ERR";

        if (optr2.equals(C_AND) || optr2.equals(C_OR)) {
            // 与或操作的优先级高于左括号
            if (optr1.equals(C_LEFT_PARE) || optr1.equals(C_BOUND_MARK)) {
                result = C_LESS_THAN;
            } else {
                result = C_MORE_THAN;
            }
        } else if (optr2.equals(C_EQU) || optr2.equals(C_NOT_EQU)
                || optr2.equals(C_MORE_THAN) || optr2.equals(C_LESS_THAN)
                || optr2.equals(C_MORE_THAN_OR_EQU)
                || optr2.equals(C_LESS_THAN_OR_EQU)) {
            // 等于,大于,小于,大于等于,小于等于操作符的优先级高于左括号和与或操作
            if (optr1.equals(C_LEFT_PARE) || optr1.equals(C_AND)
                    || optr1.equals(C_OR) || optr1.equals(C_BOUND_MARK)) {
                result = C_LESS_THAN;
            } else {
                result = C_MORE_THAN;
            }
        } else if (optr2.equals(C_ADD) || optr2.equals(C_MIN)) {
            // 加减操作的优先级比自身、乘除及右括号低
            if (optr1.equals(C_RIGHT_PARE) || optr1.equals(C_ADD)
                    || optr1.equals(C_MIN) || optr1.equals(C_MUL)
                    || optr1.equals(C_DIV)) {
                result = C_MORE_THAN;
            } else {
                result = C_LESS_THAN;
            }
        } else if (optr2.equals(C_MUL) || optr2.equals(C_DIV)
                || optr2.equals(C_MOD)) {
            // 乘除操作的优先级仅比自身及右括号低
            if (optr1.equals(C_MUL) || optr1.equals(C_DIV)
                    || optr1.equals(C_MOD) || optr1.equals(C_RIGHT_PARE)) {
                result = C_MORE_THAN;
            } else {
                result = C_LESS_THAN;
            }
        } else if (optr2.equals(C_LEFT_PARE)) {
            // 如果当前处理的是左括号，则说明站里不可能有右括号存在，否则报错
            // 左括号的优先级大于其它操作符
            if (optr1.equals(C_RIGHT_PARE)) {
                result = "ERR";
            } else {
                result = C_LESS_THAN;
            }
        } else if (optr2.equals(C_RIGHT_PARE)) {
            // 右括号和左括号匹配
            // 右括号的优先级小于其他操作符
            if (optr1.equals(C_LEFT_PARE)) {
                result = C_EQU;
            } else if (optr1.equals(C_BOUND_MARK)) {
                result = C_ERROR;
            } else {
                result = C_MORE_THAN;
            }
        } else if (optr2.equals(C_BOUND_MARK)) {
            if (optr1.equals(C_BOUND_MARK)) {
                result = C_EQU;
            } else if (optr1.equals(C_LEFT_PARE)) {
                result = C_ERROR;
            }
                // else if (optr1.equals(C_RIGHT_PARE)) {
                // result = C_LESS_THAN;
                // }
            else {
                result = C_MORE_THAN;
            }
        }

        return result;
    }

    /**
     * 根据运算符，对两操作数进行计算
     *
     * @param num1
     *            操作数1
     * @param optr
     *            运算符
     * @param num2
     *            操作数2
     * @return 运算结果
     */
    private static double calculateResult(double num1, String optr, double num2) {
        double result = 0;
        if (optr.equals(C_ADD)) {
            result = num1 + num2;
        } else if (optr.equals(C_MIN)) {
            result = num1 - num2;
        } else if (optr.equals(C_MUL)) {
            result = num1 * num2;
        } else if (optr.equals(C_DIV)) {
            result = num1 / num2;
        } else if (optr.equals(C_MOD)) {
            result = num1 % num2;
        } else if (optr.equals(C_EQU)) {
            if (num1 == num2) {
                result = 1;
            }
        } else if (optr.equals(C_NOT_EQU)) {
            if (num1 != num2) {
                result = 1;
            }
        } else if (optr.equals(C_MORE_THAN)) {
            if (num1 > num2) {
                result = 1;
            }
        } else if (optr.equals(C_LESS_THAN)) {
            if (num1 < num2) {
                result = 1;
            }
        } else if (optr.equals(C_MORE_THAN_OR_EQU)) {
            if (num1 >= num2) {
                result = 1;
            }
        } else if (optr.equals(C_LESS_THAN_OR_EQU)) {
            if (num1 <= num2) {
                result = 1;
            }
        } else if (optr.equals(C_AND)) {
            result = (int) num1 & (int) num2;
        } else if (optr.equals(C_OR)) {
            result = (int) num1 | (int) num2;
        }

        return result;
    }

    /**
     * 根据运算符，对两字符串进行比较
     *
     * @param str1
     *            字符串1
     * @param optr
     *            运算符
     * @param str2
     *            字符串2
     * @return 比较结果 1为相等；0为不等
     */
    private static double compareString(String str1, String optr, String str2) {
        double result = 0;

        if (optr.equals(C_EQU)) {
            if (str1.equals(str2))
                result = 1;
        } else if (optr.equals(C_NOT_EQU)) {
            if (!str1.equals(str2))
                result = 1;
        }

        return result;
    }

    /**
     * 将numTemp栈中的操作数每一位数字字符根据小数点的位置lastbit拼接成一个完整的操作数
     *
     * @param numTemp
     *            存放每个十进制位的数组
     * @param lastBit
     *            当前最末一位小数点的位置
     * @return 拼接之后完整的操作数
     */
    private static double todouble(Stack numTemp, double lastBit) {
        double result = 0;
        while (!numTemp.empty()) {
            result = result + ((Integer) numTemp.pop()).intValue() * lastBit;
            lastBit = lastBit * 10;
        }

        return result;
    }

    /**
     * 表达式预处理 去掉多余空格，加上结束标记
     *
     * @param expression
     *            表达式
     * @return 处理后的表达式
     */
    private static String prepareExpression(String expression) {
        StringBuffer bufExp = new StringBuffer(expression.trim());

        bufExp.append(C_BOUND_MARK);

        return bufExp.toString();
    }

    /**
     * 逻辑表达式计算
     *
     * @param expression
     *            表达式
     * @return true 结果为真; false 结果为假或表达式格式有误
     */
    public static boolean evaluateExpression(String expression) {

        expression = prepareExpression(expression); // 表达式预处理
        // int expLength = expression.length(); //取得表达式的长度

        Stack stackOptr = new Stack(); // 存放操作符
        Stack stackOpNum = new Stack(); // 存放操作数或运算结果
        Stack stackNumTemp = new Stack(); // 存放double型数的每一位数字
        StringBuffer sbufTemp1 = new StringBuffer();
        StringBuffer sbufTemp2 = new StringBuffer();

        double fLastBit = 1; // 当前double型数的小数点位置
        boolean isDouble = false; // 当前处理的数字是否为double型
        boolean isString = false; // 当前处理的是字符串
        boolean isFirstStr = true; // 当前处理的是第一个字符串
        boolean isQuoteIn = false; // 当前处理的是单引号内的字符

        String strCurrent = ""; // 当前处理的字符
        String strTop = ""; // 操作符栈栈顶字符

        int iTempSizeOptr = 0; // 操作符栈长度备份
        int iTempSizeNum = 0; // 操作数栈长度备份

        stackOptr.push(C_BOUND_MARK);
        strTop = C_BOUND_MARK;
        int i = 0;
        strCurrent = expression.substring(i, (i++) + 1);
        if (strCurrent.equals(C_ADD) || strCurrent.equals(C_MIN)) {
            stackOpNum.push(new Double(0));
        }

        while (!strCurrent.equals(C_BOUND_MARK) || !strTop.equals(C_BOUND_MARK)) {
            try {
                if (!isQuoteIn && isOperator(strCurrent)) {
                    //
                    // 字符串比较
                    //
                    if (!isFirstStr) {
                        double result = compareString(
                                removeSingleQuotes(sbufTemp1.toString()),
                                (String) stackOptr.pop(),
                                removeSingleQuotes(sbufTemp2.toString()));
                        stackOpNum.push(new Double(result));

                        sbufTemp1.delete(0, sbufTemp1.length());
                        sbufTemp2.delete(0, sbufTemp2.length());
                        isString = false;
                        isFirstStr = true;

                        strTop = (String) stackOptr
                                .elementAt(stackOptr.size() - 1);
                        continue;
                    }
                    //
                    // 拼接操作数
                    //
                    if (!stackNumTemp.empty()) {
                        stackOpNum.push(new Double(todouble(stackNumTemp,
                                fLastBit)));
                        fLastBit = 1;
                        isDouble = false;
                    }
                    //
                    // 括号的处理
                    //
                    if (strCurrent.equals(C_LEFT_PARE)) {
                        stackOptr.push(strCurrent);
                        strTop = strCurrent;
                        strCurrent = expression.substring(i, (i++) + 1);
                        if (strCurrent.equals(C_ADD)
                                || strCurrent.equals(C_MIN)) {
                            stackOpNum.push(new Double(0));
                        }
                        continue;
                    } else if (strCurrent.equals(C_LESS_THAN)
                            || strCurrent.equals(C_MORE_THAN)) {
                        // 大于等于和小于等于的处理
                        String strNext = expression.substring(i, i + 1);
                        if (strNext.equals(C_EQU)
                                || strNext.equals(C_MORE_THAN)) {
                            strCurrent = strCurrent + strNext;
                            i++;
                        }
                    }

                    if (strCurrent.equals(C_EQU)
                            || strCurrent.equals(C_NOT_EQU)) {
                        // 对字符串比较的处理,标记此后应该对第二个串进行存储
                        if (isString) {
                            isFirstStr = false;
                        }
                    }
                    //
                    // 算符优先级判断并处理
                    //
                    String sPrecede = precedeOperator(strTop, strCurrent);
                    if (sPrecede.equals(C_LESS_THAN)) {
                        // 栈顶元素优先权低，压栈并读取下一字符
                        stackOptr.push(strCurrent);
                        strCurrent = expression.substring(i, (i++) + 1);
                    } else if (sPrecede.equals(C_EQU)) {
                        // 栈顶是括号，或结束符
                        stackOptr.pop();
                        if (strCurrent.equals(C_BOUND_MARK)) {
                            continue;
                        }
                        strCurrent = expression.substring(i, (i++) + 1);
                    } else if (sPrecede.equals(C_MORE_THAN)) {
                        String strOptr = (String) stackOptr.pop();
                        double fOpNum2 = ((Double) stackOpNum.pop())
                                .doubleValue();
                        double fOpNum1 = ((Double) stackOpNum.pop())
                                .doubleValue();
                        stackOpNum.push(new Double(calculateResult(fOpNum1,
                                strOptr, fOpNum2)));
                    } else {
                        return false;
                    }
                } else if (!isString && strCurrent.charAt(0) >= C_CNUM_ZERO
                        && strCurrent.charAt(0) <= C_CNUM_NINE
                        || strCurrent.equals(C_POINT)) {
                    // 对数字进行处理
                    if (!strCurrent.equals(C_POINT)) {
                        if (isDouble) {
                            fLastBit = (new Double(fLastBit * 0.1))
                                    .doubleValue();
                        }
                        stackNumTemp.push(new Integer(strCurrent));
                    } else {
                        isDouble = true;
                    }
                    strCurrent = expression.substring(i, (i++) + 1);
                } else if (!isString && strCurrent.equals(C_SPACE)) {
                    strCurrent = expression.substring(i, (i++) + 1);
                } else {
                    // 对字符串进行读取
                    if (!isString) {
                        isString = true;
                    }
                    if (isString) {
                        // 处理单引号
                        if (C_QUOTES.equals(strCurrent)) {
                            if (isQuoteIn) {
                                isQuoteIn = false;
                            } else {
                                isQuoteIn = true;
                            }
                        }
                        if (isFirstStr) {
                            sbufTemp1.append(strCurrent);
                        } else {
                            sbufTemp2.append(strCurrent);
                        }
                    }
                    strCurrent = expression.substring(i, (i++) + 1);
                }

                // 重新记录将栈顶元素
                strTop = (String) stackOptr.elementAt(stackOptr.size() - 1);
                // 表达式错误处理，如果栈的空间两次都未发生变化，则出错
                if (strCurrent.equals(C_BOUND_MARK)) {
                    if (!(iTempSizeOptr == stackOptr.size() && iTempSizeNum == stackOpNum
                            .size())) {
                        iTempSizeOptr = stackOptr.size();
                        iTempSizeNum = stackOpNum.size();
                    } else
                        // if (!(((String) stackOptr.elementAt(stackOptr.size()
                        // - 1)).equals(C_BOUND_MARK)
                        // && stackOpNum.size() == 1))
                    {
                        return false;
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }

        // 拼接操作数，处理表达式只有一个操作数的情况
        if (!stackNumTemp.empty()) {
            stackOpNum.push(new Double(todouble(stackNumTemp, fLastBit)));
            fLastBit = 1;
            isDouble = false;
        }

        if (stackOptr.size() == 1
                && stackOpNum.size() == 1
                && ((String) stackOptr.elementAt(stackOptr.size() - 1))
                .equals(C_BOUND_MARK)) {
            if (((Double) stackOpNum.elementAt(stackOpNum.size() - 1))
                    .doubleValue() == 1) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * 计算器
     *
     * @param expression
     *            表达式
     * @return 计算结果
     */
    public static double calculator(String expression) {
        double result = 0;

        expression = prepareExpression(expression); // 表达式预处理

        Stack stackOptr = new Stack(); // 存放操作符
        Stack stackOpNum = new Stack(); // 存放操作数或运算结果
        Stack stackNumTemp = new Stack(); // 存放double型数的每一位数字

        double fLastBit = 1; // 当前double型数的小数点位置
        boolean isDouble = false; // 当前处理的数字是否为double型

        String strCurrent = ""; // 当前处理的字符
        String strTop = ""; // 操作符栈栈顶字符

        int iTempSizeOptr = 0; // 操作符栈长度备份
        int iTempSizeNum = 0; // 操作数栈长度备份

        stackOptr.push(C_BOUND_MARK);
        strTop = C_BOUND_MARK;
        int i = 0;
        strCurrent = expression.substring(i, (i++) + 1);
        if (strCurrent.equals(C_ADD) || strCurrent.equals(C_MIN)) {
            stackOpNum.push(new Double(0));
        }

        while (!strCurrent.equals(C_BOUND_MARK) || !strTop.equals(C_BOUND_MARK)) {
            try {
                if (isOperator_Cal(strCurrent)) {
                    //
                    // 拼接操作数
                    //
                    if (!stackNumTemp.empty()) {
                        stackOpNum.push(new Double(todouble(stackNumTemp,
                                fLastBit)));
                        fLastBit = 1;
                        isDouble = false;
                    }
                    //
                    // 括号的处理
                    //
                    if (strCurrent.equals(C_LEFT_PARE)) {
                        stackOptr.push(strCurrent);
                        strTop = strCurrent;
                        strCurrent = expression.substring(i, (i++) + 1);
                        if (strCurrent.equals(C_ADD)
                                || strCurrent.equals(C_MIN)) {
                            stackOpNum.push(new Double(0));
                        }
                        continue;
                    }

                    //
                    // 算符优先级判断并处理
                    //
                    String sPrecede = precedeOperator(strTop, strCurrent);
                    if (sPrecede.equals(C_LESS_THAN)) {
                        // 栈顶元素优先权低，压栈并读取下一字符
                        stackOptr.push(strCurrent);
                        strCurrent = expression.substring(i, (i++) + 1);
                    } else if (sPrecede.equals(C_EQU)) {
                        // 栈顶是括号，或结束符
                        stackOptr.pop();
                        if (strCurrent.equals(C_BOUND_MARK)) {
                            continue;
                        }
                        strCurrent = expression.substring(i, (i++) + 1);
                    } else if (sPrecede.equals(C_MORE_THAN)) {
                        String strOptr = (String) stackOptr.pop();
                        double fOpNum2 = ((Double) stackOpNum.pop())
                                .doubleValue();
                        double fOpNum1 = ((Double) stackOpNum.pop())
                                .doubleValue();
                        stackOpNum.push(new Double(calculateResult(fOpNum1,
                                strOptr, fOpNum2)));
                    } else {
                        return 0;
                    }
                } else if (strCurrent.charAt(0) >= C_CNUM_ZERO
                        && strCurrent.charAt(0) <= C_CNUM_NINE
                        || strCurrent.equals(C_POINT)) {
                    if (!strCurrent.equals(C_POINT)) {
                        if (isDouble) {
                            fLastBit = (new Double(fLastBit * 0.1))
                                    .doubleValue();
                        }
                        stackNumTemp.push(new Integer(strCurrent));
                    } else {
                        isDouble = true;
                    }
                    strCurrent = expression.substring(i, (i++) + 1);
                } else if (strCurrent.equals(C_SPACE)) {
                    strCurrent = expression.substring(i, (i++) + 1);
                } else {
                    return 0;
                }
                // 重新记录将栈顶元素
                strTop = (String) stackOptr.elementAt(stackOptr.size() - 1);

                // 表达式错误处理，如果栈的空间两次都未发生变化，则出错
                if (strCurrent.equals(C_BOUND_MARK)) {
                    if (!(iTempSizeOptr == stackOptr.size() && iTempSizeNum == stackOpNum
                            .size())) {
                        iTempSizeOptr = stackOptr.size();
                        iTempSizeNum = stackOpNum.size();
                    } else
                        // if (!(((String) stackOptr.elementAt(stackOptr.size()
                        // - 1)).equals(C_BOUND_MARK)
                        // && stackOpNum.size() == 1))
                    {
                        return 0;
                    }
                }
            } catch (Exception e) {
                return 0;
            }
        }

        // 拼接操作数，处理表达式只有一个操作数的情况
        if (!stackNumTemp.empty()) {
            stackOpNum.push(new Double(todouble(stackNumTemp, fLastBit)));
            fLastBit = 1;
            isDouble = false;
        }

        if (stackOptr.size() == 1 && stackOpNum.size() == 1) {
            result = ((Double) stackOpNum.elementAt(stackOpNum.size() - 1))
                    .doubleValue();
        } else {
            result = 0;
        }

        return result;
    }

    public static String calculators(String expression) {
        double re = calculator(expression);
        if (re - (long) re != 0) {
            return java.math.BigInteger.valueOf((long) re).toString();
        }

        return new java.math.BigDecimal(calculator(expression)).toString();
    }

    /**
     * 去掉字符串中的单引号
     *
     * @param str
     * @return
     */
    public static String removeSingleQuotes(String str) {
        str = str.trim();
        if (str.indexOf("'") != -1) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}
