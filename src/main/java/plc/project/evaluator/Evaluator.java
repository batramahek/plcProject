package plc.project.evaluator;

import plc.project.parser.Ast;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

public final class Evaluator implements Ast.Visitor<RuntimeValue, EvaluateException> {

    private Scope scope;

    public Evaluator(Scope scope) {
        this.scope = scope;
    }

    @Override
    public RuntimeValue visit(Ast.Source ast) throws EvaluateException {
        RuntimeValue value = new RuntimeValue.Primitive(null);

        for (var stmt : ast.statements()) {
            try {
                value = visit(stmt);
            } catch (Return e) {
//                if (e.getMessage().startsWith("RETURN: ")) {
//                    return new RuntimeValue.Primitive(e.getMessage().substring(7));
//                }
//                    throw e;
//                }
                throw new EvaluateException("return outside of function");
            }
        }
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Let ast) throws EvaluateException {
        if (scope.get(ast.name(), true).isPresent()){
            throw new EvaluateException("Var already declared: " + ast.name());
        }
        RuntimeValue value;
        if (ast.value().isPresent()) {
            value = visit(ast.value().get());
        }
        else
        {
            value = new RuntimeValue.Primitive(null);
        }
        scope.define(ast.name(), value);
        return value;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Def ast) throws EvaluateException {
        if (scope.get(ast.name(), true).isPresent())
        {
            throw new EvaluateException("Function '" + ast.name() + "' is already defined.");
        }

        for (int i = 0; i < ast.parameters().size(); i++)
        {
            String param = ast.parameters().get(i);
            for (int j = i + 1; j < ast.parameters().size(); j++)
            {
                if (param.equals(ast.parameters().get(j)))
                {
                    throw new EvaluateException("Duplicate parameter name '" + param + "'.");
                }
            }
        }

        // current scope
        Scope definingScope = scope;

        // function definition
        RuntimeValue.Function.Definition definition = arguments ->
        {
            // verify argument count
            if (arguments.size() != ast.parameters().size())
            {
                throw new EvaluateException("Expected " + ast.parameters().size() + " arguments but got " + arguments.size() + ".");
            }

            // new function scope - child of defining scope
            Scope functionScope = new Scope(definingScope);
            try
            {
                for (int i = 0; i < ast.parameters().size(); i++)
                {
                    functionScope.define(ast.parameters().get(i), arguments.get(i));
                }

                // body statements
                Evaluator functionEvaluator = new Evaluator(functionScope);
                RuntimeValue result = null;

                for (Ast.Stmt stmt : ast.body())
                {
                    result = functionEvaluator.visit(stmt);

                    // handle return
                    if (result instanceof RuntimeValue.Primitive primitiveResult &&
                            primitiveResult.value() instanceof String returnValue &&
                            returnValue.startsWith("RETURN:"))
                    {
                        return new RuntimeValue.Primitive(returnValue.substring(7));
                    }
                }

            } catch (Return e)
            {
//                if (e.getMessage().startsWith("RETURN:"))
//                {
//                    return new RuntimeValue.Primitive(e.getMessage().substring(7));
//                }
//                throw e;
                return e.value;
            }

            return new RuntimeValue.Primitive(null); // return null if no return statement
        };

        // define the function in the current scope
        RuntimeValue function = new RuntimeValue.Function(ast.name(), definition);
        scope.define(ast.name(), function);

        return function;
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.If ast) throws EvaluateException {
        RuntimeValue condition = visit(ast.condition());
        boolean conditionVaL = requireType(visit(ast.condition()), Boolean.class);

        Scope originalScope = scope;

        try{
            this.scope = new Scope(originalScope);

            if (conditionVaL) {
                RuntimeValue result = new RuntimeValue.Primitive(null);
                for (Ast.Stmt stmt: ast.thenBody())
                {
                    result = visit(stmt);
                }
                return result;
            }
            else if (ast.elseBody() != null) {
                RuntimeValue result = new RuntimeValue.Primitive(null);
                for (Ast.Stmt stmt: ast.elseBody())
                {
                    result = visit(stmt);
                }
                return result;
            }
            else {
                return new RuntimeValue.Primitive(null);
            }
        }
        finally
        {
            this.scope = originalScope;
        }
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.For ast) throws EvaluateException {
        RuntimeValue iterableValue = visit(ast.expression());

        if (!(iterableValue instanceof RuntimeValue.Primitive)) {
            throw new EvaluateException("FOR expression must evaluate to an iterable.");
        }

        Object value = ((RuntimeValue.Primitive) iterableValue).value();
        if (!(value instanceof List<?>)) {
            throw new EvaluateException("FOR expression must evaluate to a list.");
        }

        List<?> elements = (List<?>) value;

        // iterate over the elements, each in a new scope
        Scope originalScope = scope; // save original scope
        try {
            for (Object element : elements) {
                // ensure the element is a RuntimeValue
                if (!(element instanceof RuntimeValue)) {
                    throw new EvaluateException("List elements must be of type RuntimeValue.");
                }

                // new scope for this iteration
                this.scope = new Scope(originalScope);

                // define the loop variable
                scope.define(ast.name(), (RuntimeValue) element);

                // body statements sequentially
                for (Ast.Stmt stmt : ast.body()) {
                    visit(stmt);
                }
            }
        } finally {
            // restore original scope after the loop ends
            this.scope = originalScope;
        }

        return new RuntimeValue.Primitive(null);
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Return ast) throws EvaluateException {
//        final RuntimeValue value;
//        if (ast.value().isPresent()) {
//            value = visit(ast.value().get());
//            //this.value = value;
//            System.out.println(value);
//        }
//        else
//        {
//            value = new RuntimeValue.Primitive(null);
//        }

        // throw an exception to return the evaluated value
        //throw new RuntimeException(value);
        throw new Return(visit(ast.value().orElseGet(() -> new Ast.Expr.Literal(null))));
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Expression ast) throws EvaluateException
    {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Stmt.Assignment ast) throws EvaluateException
    {
        // evaluate the left-hand side expression
        if (ast.expression() instanceof Ast.Expr.Variable variable) {
            String name = variable.name();
            Optional existingValue = scope.get(name, false);

            if (!existingValue.isPresent()) {
                throw new EvaluateException("Undefined variable: " + name);
            }

            RuntimeValue value = visit(ast.value());
            scope.set(name, value);
            return value; // return the assigned value
        }
        else if (ast.expression() instanceof Ast.Expr.Property property) {
            RuntimeValue receiver = visit(property.receiver());
            if (!(receiver instanceof RuntimeValue.ObjectValue object)) {
                throw new EvaluateException("Property assignment must be on an object.");
            }
            Optional<RuntimeValue> existingProperty = object.scope().get(property.name(), false);

            if (!existingProperty.isPresent()) {
                throw new EvaluateException("Undefined property: " + property.name());
            }

            RuntimeValue value = visit(ast.value());
            object.scope().set(property.name(), value);
            return value; // return the assigned value
        }
        else {
            throw new EvaluateException("Invalid assignment target.");
        }
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Literal ast) throws EvaluateException {
        return new RuntimeValue.Primitive(ast.value());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Group ast) throws EvaluateException {
        return visit(ast.expression());
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Binary ast) throws EvaluateException {
        // Evaluate the left and right operands of the binary expression
        RuntimeValue left = visit(ast.left());
        RuntimeValue right = visit(ast.right());

        // Check that both operands are primitive values (such as Integer or String)
        if (left instanceof RuntimeValue.Primitive leftValue && right instanceof RuntimeValue.Primitive rightValue) {

            String op = ast.operator();

            if (op.equals("AND")) {
                if (leftValue.value() instanceof Boolean && rightValue.value() instanceof Boolean) {
                    return new RuntimeValue.Primitive((Boolean) leftValue.value() && (Boolean) rightValue.value());
                } else {
                    throw new EvaluateException("Operands must be Booleans for 'AND' operator");
                }
            }
            else if (op.equals("OR")) {
                // evaluate left side of the ast
                if (leftValue.value() instanceof Boolean leftBoolean) {
                    if (op.equals("OR") && leftBoolean == true) {
                        return new RuntimeValue.Primitive(true);
                    } else if (op.equals("OR")) {
                        return new RuntimeValue.Primitive(leftBoolean || requireType(visit(ast.right()), Boolean.class));
                    }
                }
            }
//
//                if (leftValue.value() instanceof Boolean && leftValue.value() == Boolean.TRUE || rightValue.value() instanceof Boolean && rightValue.value() == Boolean.TRUE)
//                {
//                    return new RuntimeValue.Primitive(true);
//                }
//                else
//                {
//                    return new RuntimeValue.Primitive(false);
//                }
//
            else if (op.equals("+")) {
                if (leftValue.value() instanceof Integer && rightValue.value() instanceof Integer || leftValue.value() instanceof BigInteger && rightValue.value() instanceof BigInteger) {
                    return new RuntimeValue.Primitive(((BigInteger) leftValue.value()).add(((BigInteger) rightValue.value())));
                } else if (leftValue.value() instanceof BigDecimal && rightValue.value() instanceof BigDecimal) {
                    return new RuntimeValue.Primitive(((BigDecimal) leftValue.value()).add(((BigDecimal) rightValue.value())));
                } else if (leftValue.value() instanceof String)//|| rightValue.value() instanceof String) {
                {
                    return new RuntimeValue.Primitive(leftValue.value().toString() + rightValue.value());//.toString());
                }
                else if (rightValue.value() instanceof String)
                {
                    return new RuntimeValue.Primitive(leftValue.value() + rightValue.value().toString());
                }
                else {
                    throw new EvaluateException("Operands must be of type Integer, Decimal or String for '+' operator");
                }
            } else if (op.equals("-")) {
                if (leftValue.value() instanceof Integer && rightValue.value() instanceof Integer) {
                    return new RuntimeValue.Primitive(((Integer) leftValue.value()) - ((Integer) rightValue.value()));
                } else if (leftValue.value() instanceof Integer && rightValue.value() instanceof Integer || leftValue.value() instanceof BigInteger && rightValue.value() instanceof BigInteger) {
                    return new RuntimeValue.Primitive(((BigInteger) leftValue.value()).subtract(((BigInteger) rightValue.value())));
                } else if (leftValue.value() instanceof BigDecimal && rightValue.value() instanceof BigDecimal) {
                    return new RuntimeValue.Primitive(((BigDecimal) leftValue.value()).subtract(((BigDecimal) rightValue.value())));
                } else {
                    throw new EvaluateException("Operands must be integers or decimals for '-' operator");
                }
            } else if (op.equals("*")) {
                if (leftValue.value() instanceof Integer && rightValue.value() instanceof Integer) {
                    return new RuntimeValue.Primitive(((Integer) leftValue.value()) * ((Integer) rightValue.value()));
                }
                else if (leftValue.value() instanceof BigDecimal)
                {
                    BigDecimal leftDecimal = (BigDecimal) leftValue.value();
                    BigDecimal rightDecimal = (BigDecimal) rightValue.value();
                    BigDecimal result = leftDecimal.multiply(rightDecimal);
                    return new RuntimeValue.Primitive(result);
                }
                else {
                    throw new EvaluateException("Operands must be integers or decimals  for '*' operator");
                }
            } else if (op.equals("/")) {
                if ((leftValue.value() instanceof BigDecimal || leftValue.value() instanceof BigInteger) && left.getClass() == right.getClass())
                {
                    if ((rightValue.value() instanceof BigDecimal || rightValue.value() instanceof BigInteger) && visit(ast.right()).equals(BigDecimal.ZERO) || (rightValue.value() instanceof BigInteger && rightValue.equals(BigInteger.ZERO)))
                    {
                        throw new EvaluateException("dividing by 0");
                    }

                    if (leftValue.value() instanceof BigDecimal)
                    {
                        BigDecimal leftDecimal = (BigDecimal) leftValue.value();
                        BigDecimal rightDecimal = (BigDecimal) rightValue.value();
                        BigDecimal result = leftDecimal.divide(rightDecimal, RoundingMode.HALF_EVEN);
                        return new RuntimeValue.Primitive(result);
                    }
                    else if (leftValue.value() instanceof BigInteger && rightValue.value() instanceof BigInteger)
                    {
                        //return new RuntimeValue.Primitive(((Integer) leftValue.value()).divide(((Integer) rightValue.value()), RoundingMode.HALF_EVEN));
                        BigInteger leftInt = (BigInteger) leftValue.value();
                        BigInteger rightInt = (BigInteger) rightValue.value();
                        BigInteger result = leftInt.divide(rightInt);
                        return new RuntimeValue.Primitive(result);
                    }
                }
                else {
                    throw new EvaluateException("Operands must be integers or decimals for '/' operator");
                }
            }
            else if (op.equals("==") || op.equals("!=")) {
                switch (op) {
                    case "==":
                        if (visit(ast.left()).equals(visit(ast.right()))) {
                            return new RuntimeValue.Primitive(true);
                        } else {
                            return new RuntimeValue.Primitive(false);
                        }
                    case "!=":
                        if (visit(ast.left()).equals(visit(ast.right()))) {
                            return new RuntimeValue.Primitive(false);
                        } else {
                            return new RuntimeValue.Primitive(true);
                        }

                }
            }
            else if (op.equals(">") || op.equals("<") || op.equals(">=") || op.equals("<="))
            {
                Comparable lComp = requireType(leftValue, Comparable.class);
                Comparable rComp = requireType(rightValue, Comparable.class);

                if (lComp.getClass() == rComp.getClass()) {
                    switch (op) {
                        case "<":
                            return new RuntimeValue.Primitive((Boolean) (lComp.compareTo(rComp) < 0));
                        case "<=":
                            return new RuntimeValue.Primitive(lComp.compareTo(rComp) <= 0);
                        case ">":
                            return new RuntimeValue.Primitive(lComp.compareTo(rComp) > 0);
                        case ">=":
                            return new RuntimeValue.Primitive(lComp.compareTo(rComp) >= 0);

                        default:
                            throw new EvaluateException("Unknown binary operator: " + ast.operator());
                    }
                }
                else
                {
                    throw new EvaluateException("Operands must be of same type");
                }
            }
            else {
                throw new EvaluateException("Unknown operator: " + ast.operator());
            }
        }

        // If the operands are not of type RuntimeValue.Primitive, throw an exception
        throw new EvaluateException("Invalid operand types");
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Variable ast) throws EvaluateException {
        if (scope.get(ast.name(), false).isPresent())
        {
            return scope.get(ast.name(), false).get();
        }
        throw new EvaluateException("Var not found");
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Property ast) throws EvaluateException {

        RuntimeValue receiver = visit(ast.receiver());

        if (!(receiver instanceof RuntimeValue.ObjectValue))
        {
            throw new EvaluateException("Receiver must be object for properties");
        }

        RuntimeValue.ObjectValue object = (RuntimeValue.ObjectValue) receiver;
        Scope objScope = object.scope();
        Optional<RuntimeValue> property = objScope.get(ast.name(), true);

        if (!property.isPresent())
        {
            throw new EvaluateException("Property not found");
        }
        return property.get();
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Function ast) throws EvaluateException {
        if (!scope.get(ast.name(), false).isPresent())
            throw new EvaluateException("Function not defined");

        RuntimeValue funcVal = scope.get(ast.name(), false).get();

        if (!(funcVal instanceof RuntimeValue.Function))
        {
            throw new EvaluateException("Not a function");
        }

        int argCount = ast.arguments().size();
        RuntimeValue[] args = new RuntimeValue[argCount];

        for (int i = 0; i < argCount; i++)
        {
            args[i] = visit(ast.arguments().get(i));
        }

        RuntimeValue.Function func = (RuntimeValue.Function) funcVal;
        return func.definition().invoke(java.util.Arrays.asList(args));
    }

    @Override
    public RuntimeValue visit(Ast.Expr.Method ast) throws EvaluateException {
        // receiver
        RuntimeValue receiver = visit(ast.receiver());

        if (!(receiver instanceof RuntimeValue.ObjectValue)) {
            throw new EvaluateException("Method call requires object receiver.");
        }

        RuntimeValue.ObjectValue object = (RuntimeValue.ObjectValue) receiver;

        // get method from the object's scope
        Optional<RuntimeValue> methodOpt = object.scope().get(ast.name(), false);
        if (!methodOpt.isPresent()) {
            throw new EvaluateException("Method '" + ast.name() + "' not found.");
        }

        RuntimeValue methodValue = methodOpt.get();
        if (!(methodValue instanceof RuntimeValue.Function)) {
            throw new EvaluateException("Property '" + ast.name() + " - not a method.");
        }

        RuntimeValue.Function method = (RuntimeValue.Function) methodValue;

        // evaluate arguments
        List<RuntimeValue> arguments = new java.util.ArrayList<>();
        arguments.add(receiver); // Add receiver as first argument (this)
        for (Ast.Expr arg : ast.arguments()) {
            arguments.add(visit(arg));
        }

        return method.definition().invoke(arguments);
    }

    @Override
    public RuntimeValue visit(Ast.Expr.ObjectExpr ast) throws EvaluateException {
        // Create a new scope for the object (child of current scope)
        Scope objectScope = new Scope(scope);

        // Define fields in the object's scope
        for (Ast.Stmt.Let field : ast.fields()) {
            RuntimeValue value;
            try {
                value = field.value().isPresent()
                        ? visit(field.value().get())
                        : new RuntimeValue.Primitive(null);
            } catch (EvaluateException e) {
                throw new EvaluateException("Error initializing field '" + field.name() + "': " + e.getMessage());
            }
            objectScope.define(field.name(), value);
        }

        // Define methods in the object's scope
        for (Ast.Stmt.Def method : ast.methods()) {
            // Capture the object scope for static binding
            Scope methodDefiningScope = objectScope;

            RuntimeValue.Function function = new RuntimeValue.Function(method.name(), arguments -> {
                // Create method execution scope (child of object scope)
                Scope methodScope = new Scope(methodDefiningScope);

                try {
                    // bind 'this' reference to the object
                    if (!arguments.isEmpty()) {
                        methodScope.define("this", arguments.get(0));
                    }

                    // bind method parameters
                    for (int i = 1; i < arguments.size(); i++) {
                        if (i - 1 < method.parameters().size()) {
                            RuntimeValue argument = arguments.get(i);

                            // unwrap if it's a Primitive to avoid double wrapping -- something not working
                            if (argument instanceof RuntimeValue.Primitive primitiveArgument) {
                                methodScope.define(method.parameters().get(i - 1), new RuntimeValue.Primitive(primitiveArgument.value()));
                            } else {
                                methodScope.define(method.parameters().get(i - 1), argument);
                            }
                        }
                    }

                    // Execute method body
                    RuntimeValue result = new RuntimeValue.Primitive(null);
                    Evaluator methodEvaluator = new Evaluator(methodScope);
                    for (Ast.Stmt stmt : method.body()) {
                        result = methodEvaluator.visit(stmt);

                        // Handle return statements
                        if (result instanceof RuntimeValue.Primitive &&
                                result.print().startsWith("RETURN:")) {
                            return new RuntimeValue.Primitive(result.print().substring(7));
                        }
                    }
                    return result;
                } catch (Return e) {
//                    if (e.getMessage().startsWith("RETURN:")) {
//                        return new RuntimeValue.Primitive(e.getMessage().substring(7));
//                    }
//                    throw e;
                    return e.value;
                }
            });

            objectScope.define(method.name(), function);
        }
        return new RuntimeValue.ObjectValue(ast.name(), objectScope);
    }

    /**
     * Helper function for extracting RuntimeValues of specific types. If the
     * type is subclass of {@link RuntimeValue} the check applies to the value
     * itself, otherwise the value is expected to be a {@link RuntimeValue.Primitive}
     * and the check applies to the primitive value.
     */
    private static <T> T requireType(RuntimeValue value, Class<T> type) throws EvaluateException {
        //To be discussed in lecture 3/5.
        if (RuntimeValue.class.isAssignableFrom(type)) {
            if (!type.isInstance(value)) {
                throw new EvaluateException("Expected value to be of type " + type + ", received " + value.getClass() + ".");
            }
            return (T) value;
        } else {
            var primitive = requireType(value, RuntimeValue.Primitive.class);
            if (!type.isInstance(primitive.value())) {
                var received = primitive.value() != null ? primitive.value().getClass() : null;
                throw new EvaluateException("Expected value to be of type " + type + ", received " + received + ".");
            }
            return (T) primitive.value();
        }
    }

    private class Return extends RuntimeException {

        private final RuntimeValue value;

        private Return(RuntimeValue value) {
            this.value = value;
        }
    }
}
