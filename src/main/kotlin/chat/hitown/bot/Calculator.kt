package chat.hitown.bot

import kotlin.math.*

/**
 * Calculator class that handles mathematical expressions and computations.
 */
class Calculator {
    companion object {
        private const val PI = Math.PI
        private const val E = Math.E
    }

    /**
     * Evaluates a mathematical expression and returns the result.
     * @param expression The mathematical expression to evaluate
     * @return The result of the evaluation
     * @throws IllegalArgumentException if the expression is invalid
     */
    fun evaluate(expression: String): Double {
        if (expression.isBlank()) {
            throw IllegalArgumentException("Expression cannot be empty")
        }
        try {
            return evaluateExpression(tokenize(expression))
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw IllegalArgumentException("Invalid expression: ${e.message}")
            }
        }
    }

    private fun tokenize(expression: String): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        val expr = expression.replace(" ", "").lowercase()
        
        if (expr.count { it == '(' } != expr.count { it == ')' }) {
            throw IllegalArgumentException("Mismatched parentheses")
        }

        while (i < expr.length) {
            when {
                expr[i].isDigit() || expr[i] == '.' -> {
                    var num = ""
                    var decimalPoints = 0
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        if (expr[i] == '.') decimalPoints++
                        if (decimalPoints > 1) throw IllegalArgumentException("Invalid number format: multiple decimal points")
                        num += expr[i]
                        i++
                    }
                    if (num.endsWith(".")) throw IllegalArgumentException("Invalid number format: ends with decimal point")
                    tokens.add(num)
                    i--
                }
                expr.substring(i).startsWith("pi") -> {
                    tokens.add(PI.toString())
                    i += 1
                }
                expr.substring(i).startsWith("e") && (i + 1 >= expr.length || !expr[i + 1].isLetter()) -> {
                    tokens.add(E.toString())
                }
                expr.substring(i).startsWith("sin") -> {
                    tokens.add("sin")
                    i += 2
                }
                expr.substring(i).startsWith("cos") -> {
                    tokens.add("cos")
                    i += 2
                }
                expr.substring(i).startsWith("tan") -> {
                    tokens.add("tan")
                    i += 2
                }
                expr.substring(i).startsWith("sqrt") -> {
                    tokens.add("sqrt")
                    i += 3
                }
                expr.substring(i).startsWith("log") -> {
                    tokens.add("log")
                    i += 2
                }
                expr.substring(i).startsWith("ln") -> {
                    tokens.add("ln")
                    i += 1
                }
                "+-*/^()".contains(expr[i]) -> {
                    tokens.add(expr[i].toString())
                }
                else -> {
                    if (!expr[i].isWhitespace()) {
                        throw IllegalArgumentException("Invalid character: ${expr[i]}")
                    }
                }
            }
            i++
        }
        return tokens
    }

    private fun evaluateExpression(tokens: List<String>): Double {
        val numbers = mutableListOf<Double>()
        val operators = mutableListOf<String>()

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.toDoubleOrNull() != null -> {
                    numbers.add(token.toDouble())
                }
                token == "(" -> {
                    operators.add(token)
                }
                token == ")" -> {
                    while (operators.isNotEmpty() && operators.last() != "(") {
                        applyOperator(numbers, operators)
                    }
                    if (operators.isNotEmpty() && operators.last() == "(") {
                        operators.removeAt(operators.lastIndex)
                    }
                    if (operators.isNotEmpty() && operators.last() in listOf("sin", "cos", "tan", "sqrt", "log", "ln")) {
                        applyFunction(numbers, operators.removeAt(operators.lastIndex))
                    }
                }
                token in listOf("sin", "cos", "tan", "sqrt", "log", "ln") -> {
                    operators.add(token)
                }
                token in listOf("+", "-", "*", "/", "^") -> {
                    while (operators.isNotEmpty() && operators.last() != "(" && 
                           getPrecedence(operators.last()) >= getPrecedence(token)) {
                        applyOperator(numbers, operators)
                    }
                    operators.add(token)
                }
            }
            i++
        }

        while (operators.isNotEmpty()) {
            applyOperator(numbers, operators)
        }

        return numbers.last()
    }

    private fun getPrecedence(operator: String): Int = when (operator) {
        "+", "-" -> 1
        "*", "/" -> 2
        "^" -> 3
        "sin", "cos", "tan", "sqrt", "log", "ln" -> 4
        else -> 0
    }

    private fun applyOperator(numbers: MutableList<Double>, operators: MutableList<String>) {
        val operator = operators.removeAt(operators.lastIndex)
        when (operator) {
            "+" -> {
                val b = numbers.removeAt(numbers.lastIndex)
                val a = numbers.removeAt(numbers.lastIndex)
                numbers.add(a + b)
            }
            "-" -> {
                val b = numbers.removeAt(numbers.lastIndex)
                val a = numbers.removeAt(numbers.lastIndex)
                numbers.add(a - b)
            }
            "*" -> {
                val b = numbers.removeAt(numbers.lastIndex)
                val a = numbers.removeAt(numbers.lastIndex)
                numbers.add(a * b)
            }
            "/" -> {
                val b = numbers.removeAt(numbers.lastIndex)
                if (b == 0.0) throw IllegalArgumentException("Division by zero")
                val a = numbers.removeAt(numbers.lastIndex)
                numbers.add(a / b)
            }
            "^" -> {
                val b = numbers.removeAt(numbers.lastIndex)
                val a = numbers.removeAt(numbers.lastIndex)
                numbers.add(a.pow(b))
            }
        }
    }

    private fun applyFunction(numbers: MutableList<Double>, function: String) {
        val x = numbers.removeAt(numbers.lastIndex)
        val result = when (function) {
            "sin" -> sin(x)
            "cos" -> cos(x)
            "tan" -> tan(x)
            "sqrt" -> {
                if (x < 0) throw IllegalArgumentException("Cannot calculate square root of negative number")
                sqrt(x)
            }
            "log" -> {
                if (x <= 0) throw IllegalArgumentException("Cannot calculate logarithm of non-positive number")
                log10(x)
            }
            "ln" -> {
                if (x <= 0) throw IllegalArgumentException("Cannot calculate natural logarithm of non-positive number")
                ln(x)
            }
            else -> throw IllegalArgumentException("Unknown function: $function")
        }
        numbers.add(result)
    }
} 