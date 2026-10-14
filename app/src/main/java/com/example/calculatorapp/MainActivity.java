package com.example.calculatorapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.activity.ComponentActivity;

import java.math.BigDecimal;
import java.math.MathContext;

public class MainActivity extends ComponentActivity {

    private TextView display;
    private String expression = "";
    private boolean resultShown = false;
    private boolean errorShown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        display = findViewById(R.id.display);

        int[] digits = {
                R.id.btn0, R.id.btn1, R.id.btn2,
                R.id.btn3, R.id.btn4, R.id.btn5,
                R.id.btn6, R.id.btn7, R.id.btn8,
                R.id.btn9
        };

        for (int i = 0; i < digits.length; i++) {
            final String digit = String.valueOf(i);
            findViewById(digits[i]).setOnClickListener(
                    v -> appendDigit(digit)
            );
        }

        bind(R.id.btnAdd, "+");
        bind(R.id.btnSubtract, "-");
        bind(R.id.btnMultiply, "×");
        bind(R.id.btnDivide, "÷");
        bind(R.id.btnOpenParen, "(");
        bind(R.id.btnCloseParen, ")");
        bind(R.id.btnDecimal, ".");

        findViewById(R.id.btnClear).setOnClickListener(v -> clear());
        findViewById(R.id.btnBackspace).setOnClickListener(
                v -> backspace()
        );
        findViewById(R.id.btnSign).setOnClickListener(
                v -> toggleSign()
        );
        findViewById(R.id.btnPercent).setOnClickListener(
                v -> percent()
        );
        findViewById(R.id.btnEquals).setOnClickListener(
                v -> calculate()
        );
    }

    private void bind(int id, String value) {
        findViewById(id).setOnClickListener(
                v -> append(value)
        );
    }

    private void appendDigit(String digit) {
        if (errorShown || resultShown) {
            expression = "";
            errorShown = false;
            resultShown = false;
        }

        if (expression.endsWith(")")) {
            expression += "×";
        }

        expression += digit;
        updateDisplay();
    }

    private void append(String value) {
        if (errorShown) clear();

        if (resultShown) {
            if (!isOperator(value)) expression = "";
            resultShown = false;
        }

        if (value.equals("(")) {
            if (!expression.isEmpty()) {
                char last = expression.charAt(
                        expression.length() - 1
                );
                if (Character.isDigit(last) ||
                        last == ')' || last == '%') {
                    expression += "×";
                }
            }
            expression += "(";

        } else if (value.equals(")")) {
            if (unclosedParentheses() > 0 &&
                    !expression.isEmpty()) {
                char last = expression.charAt(
                        expression.length() - 1
                );
                if (Character.isDigit(last) ||
                        last == ')' || last == '%') {
                    expression += ")";
                }
            }

        } else if (value.equals(".")) {
            String current = currentNumber();
            if (!current.contains(".")) {
                if (current.isEmpty()) {
                    if (expression.endsWith(")")) {
                        expression += "×";
                    }
                    expression += "0";
                }
                expression += ".";
            }

        } else if (isOperator(value)) {
            if (expression.isEmpty()) {
                if (value.equals("-")) expression = "-";
            } else {
                char last = expression.charAt(
                        expression.length() - 1
                );

                if (isOperator(String.valueOf(last))) {
                    expression = expression.substring(
                            0, expression.length() - 1
                    );
                }
                expression += value;
            }
        }

        updateDisplay();
    }

    private boolean isOperator(String s) {
        return s.equals("+") || s.equals("-") ||
                s.equals("×") || s.equals("÷");
    }

    private String currentNumber() {
        int i = expression.length() - 1;

        while (i >= 0) {
            char c = expression.charAt(i);
            if (!Character.isDigit(c) && c != '.') break;
            i--;
        }

        return expression.substring(i + 1);
    }

    private int unclosedParentheses() {
        int count = 0;

        for (char c : expression.toCharArray()) {
            if (c == '(') count++;
            if (c == ')') count--;
        }

        return count;
    }

    private void calculate() {
        if (expression.isEmpty()) return;

        try {
            ExpressionParser parser =
                    new ExpressionParser(expression);

            BigDecimal result = parser.parse();

            expression = result.stripTrailingZeros()
                    .toPlainString();

            resultShown = true;
            updateDisplay();

        } catch (Exception e) {
            display.setText("Error");
            errorShown = true;
        }
    }

    private void clear() {
        expression = "";
        resultShown = false;
        errorShown = false;
        updateDisplay();
    }

    private void backspace() {
        if (errorShown) {
            clear();
            return;
        }

        if (!expression.isEmpty()) {
            expression = expression.substring(
                    0, expression.length() - 1
            );
        }

        resultShown = false;
        updateDisplay();
    }

    private void toggleSign() {
        if (errorShown) return;

        // Negate the entire expression using parentheses.
        if (expression.isEmpty()) {
            expression = "-";
        } else {
            expression = "-(" + expression + ")";
        }

        resultShown = false;
        updateDisplay();
    }

    private void percent() {
        if (errorShown || expression.isEmpty()) return;

        char last = expression.charAt(expression.length() - 1);
        if (Character.isDigit(last) || last == ')') {
            expression += "%";
        }

        resultShown = false;
        updateDisplay();
    }

    private void updateDisplay() {
        display.setText(
                expression.isEmpty() ? "0" : expression
        );
    }

    // Recursive-descent expression parser
    private static class ExpressionParser {

        private final String input;
        private int pos = 0;

        ExpressionParser(String input) {
            this.input = input;
        }

        BigDecimal parse() {
            BigDecimal result = parseExpression();

            if (pos != input.length()) {
                throw new IllegalArgumentException();
            }

            return result;
        }

        // Addition and subtraction
        private BigDecimal parseExpression() {
            BigDecimal value = parseTerm();

            while (true) {
                if (match('+')) {
                    value = value.add(parseTerm());
                } else if (match('-') || match('−')) {
                    value = value.subtract(parseTerm());
                } else {
                    return value;
                }
            }
        }

        // Multiplication and division
        private BigDecimal parseTerm() {
            BigDecimal value = parseFactor();

            while (true) {
                if (match('×') || match('*')) {
                    value = value.multiply(
                            parseFactor(), MathContext.DECIMAL64
                    );
                } else if (match('÷') || match('/')) {
                    BigDecimal divisor = parseFactor();

                    if (divisor.compareTo(BigDecimal.ZERO) == 0) {
                        throw new ArithmeticException(
                                "Division by zero"
                        );
                    }

                    value = value.divide(
                            divisor, MathContext.DECIMAL64
                    );
                } else {
                    return value;
                }
            }
        }

        // Numbers, unary minus, parentheses, percentages
        private BigDecimal parseFactor() {
            BigDecimal value;

            if (match('+')) {
                return parseFactor();
            }

            if (match('-') || match('−')) {
                return parseFactor().negate();
            }

            if (match('(')) {
                value = parseExpression();
                if (!match(')')) {
                    throw new IllegalArgumentException(
                            "Missing closing parenthesis"
                    );
                }
            } else {
                int start = pos;

                while (pos < input.length()) {
                    char c = input.charAt(pos);
                    if (Character.isDigit(c) || c == '.') {
                        pos++;
                    } else {
                        break;
                    }
                }

                if (start == pos) {
                    throw new IllegalArgumentException(
                            "Expected number"
                    );
                }

                value = new BigDecimal(
                        input.substring(start, pos)
                );
            }

            while (match('%')) {
                value = value.movePointLeft(2);
            }

            return value;
        }

        private boolean match(char expected) {
            if (pos < input.length() &&
                    input.charAt(pos) == expected) {
                pos++;
                return true;
            }
            return false;
        }
    }
}
