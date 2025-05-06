package plc.project.analyzer;

import plc.project.parser.Ast;

import javax.swing.text.html.Option;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public final class Analyzer implements Ast.Visitor<Ir, AnalyzeException> {

    private Scope scope;

    public Analyzer(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Ir.Source visit(Ast.Source ast) throws AnalyzeException {
        var statements = new ArrayList<Ir.Stmt>();
        for (var statement : ast.statements()) {
            statements.add(visit(statement));
        }
        return new Ir.Source(statements);
    }

    private Ir.Stmt visit(Ast.Stmt ast) throws AnalyzeException {
        return (Ir.Stmt) visit((Ast) ast); //helper to cast visit(Ast.Stmt) to Ir.Stmt
    }

    @Override
    public Ir.Stmt.Let visit(Ast.Stmt.Let ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        if (scope.get(ast.name(), true).isPresent())
        {
            throw new AnalyzeException("variable " + ast.name()+" already defined");
        }

        Optional<Type> type = Optional.empty();
        if (ast.type().isPresent()){
            if (!Environment.TYPES.containsKey(ast.type().get()))
            {
                throw new AnalyzeException("type " + ast.type().get() + " is not defined");
            }
            type = Optional.of(Environment.TYPES.get(ast.type().get()));
        }

        Optional<Ir.Expr> value = ast.value().isPresent()
                ? Optional.of(visit(ast.value().get()))
                : Optional.empty();

        var variableType = type.or(() -> value.map(expr -> expr.type())).orElse(Type.ANY);
        if (value.isPresent())
        {
            requireSubtype(value.get().type(), variableType);
        }

        scope.define(ast.name(), variableType);
        return new Ir.Stmt.Let(ast.name(), variableType, value);
    }

    @Override
    public Ir.Stmt.Def visit(Ast.Stmt.Def ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        if (scope.get(ast.name(), true).isPresent())
        {
            throw new AnalyzeException("function " + ast.name()+" already defined");
        }
        Optional<Type> returnType = Optional.empty();
        if (ast.returnType().isPresent())
        {
            String returnTypeStr = ast.returnType().get();
            if (!Environment.TYPES.containsKey(returnTypeStr))
            {
                throw new AnalyzeException("return type " + returnTypeStr + " is not defined");
            }
            returnType = Optional.of(Environment.TYPES.get(returnTypeStr));
        }
        else
        {
            returnType = Optional.of(Type.ANY);
        }

//        List<Type> parameterTypes = new ArrayList<>();
//        for (var parameter: ast.parameters())
//        {
//            parameterTypes.add(Type.ANY);
//        }
//
//        Type functionType = new Type.Function(parameterTypes, returnType.get());
//        scope.define(ast.name(), functionType);
//
//        return new Ir.Stmt.Def(ast.name(), ast.parameters(), functionType, ast.body());
        List<Ir.Stmt.Def.Parameter> parameters = new ArrayList<>();
        List<String> paramNames = ast.parameters();
        List<Optional<String>> paramTypes = ast.parameterTypes();
        Set<String> seenParamNames = new HashSet<>();


        for (int i = 0; i < paramNames.size(); i++) {
            String paramName = paramNames.get(i);
            Optional<String> paramTypeStr = paramTypes.get(i);


            // Check for duplicate parameter names
            if (!seenParamNames.add(paramName)) {
                throw new AnalyzeException("duplicate parameter name: " + paramName);
            }

            // If a parameter type is specified, resolve it
            Type paramType = Type.ANY; // default to ANY if no type
            if (paramTypeStr.isPresent()) {
                String typeName = paramTypeStr.get();
                if (!Environment.TYPES.containsKey(typeName)) {
                    throw new AnalyzeException("parameter type " + typeName + " is not defined");
                }
                paramType = Environment.TYPES.get(typeName);
            }

            parameters.add(new Ir.Stmt.Def.Parameter(paramName, paramType));

            scope.define(paramName, paramType);
        }

        List<Ir.Stmt> body = new ArrayList<>();
        for (var stmt : ast.body()) {
            body.add(visit(stmt)); // visit each statement in the body
        }

        Type functionType = new Type.Function(parameters.stream().map(p -> p.type()).collect(Collectors.toList()), returnType.get());
        scope.define(ast.name(), functionType);

        return new Ir.Stmt.Def(ast.name(), parameters, returnType.get(), body);
    }

    @Override
    public Ir.Stmt.If visit(Ast.Stmt.If ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        try {
            var condition = visit(ast.condition());
            requireSubtype(condition.type(), Type.BOOLEAN);

            var thenScope = new Scope(scope);
            var thenStatements = new ArrayList<Ir.Stmt>();

            for (var stmt : ast.thenBody()) {
                var tempScope = scope;
                scope = thenScope;
                try {
                    thenStatements.add(visit(stmt));
                } finally {
                    scope = tempScope;
                }
            }

            var elseScope = new Scope(scope);
            var elseStatements = new ArrayList<Ir.Stmt>();

            for (var stmt : ast.elseBody()) {
                var tempScope = scope;
                scope = elseScope;
                try {
                    elseStatements.add(visit(stmt));
                } finally {
                    scope = tempScope;
                }
            }

            return new Ir.Stmt.If(condition, thenStatements, elseStatements);
        }
        catch (AnalyzeException e) {
            throw new AnalyzeException(e.getMessage());
        }
    }

    @Override
    public Ir.Stmt.For visit(Ast.Stmt.For ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        var expression = visit(ast.expression());
        if (!(expression.type() == Type.ITERABLE))
        {
            throw new AnalyzeException("type " + expression.type() + " is not iterable");
        }

        Scope originalScope = scope;
        scope = new Scope(scope);

        try {
            scope.define(ast.name(), Type.INTEGER);

            List<Ir.Stmt> body = new ArrayList<Ir.Stmt>();
            for (var stmt : ast.body()) {
                body.add(visit(stmt));
            }

            return new Ir.Stmt.For(ast.name(), Type.INTEGER, expression, body);
        }
        finally
        {
            scope = originalScope;
        }
    }

    @Override
    public Ir.Stmt.Return visit(Ast.Stmt.Return ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
//        //Check if we're inside a function or method by looking for $RETURNS
//        Type returnsType = scope.get("$RETURNS", false).orElseThrow(() -> new AnalyzeException("Return cannot be used outside of a function."));
//        Optional<Ir.Expr> returnValue = ast.value().isPresent()
//                ? Optional.of(visit(ast.value().get()))
//                : Optional.empty();
//
//
//        // Type check
//        if (returnValue.isPresent()) {
//            requireSubtype(returnValue.get().type(), returnsType);
//        } else {
//            requireSubtype(Type.NIL, returnsType);
//        }
//
//        if (returnValue.equals(Optional.empty())) {
//            throw new AnalyzeException("return value is empty");
//        }
//
//        return new Ir.Stmt.Return(returnValue);

        //throw new UnsupportedOperationException("TODO"); //TODO
        //Check if we're inside a function or method by looking for $RETURNS
        Optional<Type> returnsType = scope.get("$RETURNS", false);
        Optional<Ir.Expr> returnValue = ast.value().isPresent()
                ? Optional.of(visit(ast.value().get()))
                : Optional.empty();


        if (returnsType.isPresent()) {
            Type expectedType = returnsType.get();

            if (returnValue.isPresent()) {
                requireSubtype(returnValue.get().type(), expectedType);
            } else {
                requireSubtype(Type.NIL, expectedType);
            }
        }
        else {
            if (returnValue.isPresent()) {
                returnValue.get();
            } else {

            }
        }

        if (returnValue.equals(Optional.empty())) {
            throw new AnalyzeException("return value is empty");
        }
        return new Ir.Stmt.Return(returnValue);
    }

    @Override
    public Ir.Stmt.Expression visit(Ast.Stmt.Expression ast) throws AnalyzeException {
        var expression = visit(ast.expression());
        return new Ir.Stmt.Expression(expression);
    }

    @Override
    public Ir.Stmt.Assignment visit(Ast.Stmt.Assignment ast) throws AnalyzeException {

        //throw new UnsupportedOperationException("TODO"); //TODO
        if (ast.expression() instanceof Ast.Expr.Variable variable)
        {
            var ir = visit(variable);
            var type = scope.get(variable.name(), false).orElseThrow(() -> new AnalyzeException("variable " + variable.name() + " is not defined"));
            var value = visit(ast.value());
            requireSubtype(value.type(), ir.type());
            return new Ir.Stmt.Assignment.Variable(ir, value);
        }
        else if (ast.expression() instanceof Ast.Expr.Property property)
        {
            var irReceiver = visit(property.receiver());
            var objType = irReceiver.type();

            if (!(objType instanceof Type.Object objectType)) {
                throw new AnalyzeException("Receiver is not an object");
            }

            // Look up the field in the object’s scope
            Optional<Type> optionalPropertyType = objectType.scope().get(property.name(), false);
            if (optionalPropertyType.isEmpty()) {
                throw new AnalyzeException("property " + property.name() + " is not defined");
            }
            Type propertyType = optionalPropertyType.get();

            var irProperty = new Ir.Expr.Property(irReceiver, property.name(), propertyType);
            var value = visit(ast.value());
            requireSubtype(value.type(), propertyType);

            return new Ir.Stmt.Assignment.Property(irProperty, value);
        }

        throw new AnalyzeException("invalid assignment target");
    }

    private Ir.Expr visit(Ast.Expr ast) throws AnalyzeException {
        return (Ir.Expr) visit((Ast) ast); //helper to cast visit(Ast.Expr) to Ir.Expr
    }

    @Override
    public Ir.Expr.Literal visit(Ast.Expr.Literal ast) throws AnalyzeException {
        var type = switch (ast.value()) {
            case null -> Type.NIL;
            case Boolean _ -> Type.BOOLEAN;
            case BigInteger _ -> Type.INTEGER;
            case BigDecimal _ -> Type.DECIMAL;
            case String _ -> Type.STRING;
            //If the AST value isn't one of the above types, the Parser is
            //returning an incorrect AST - this is an implementation issue,
            //hence throw AssertionError rather than AnalyzeException.
            default -> throw new AssertionError(ast.value().getClass());
        };
        return new Ir.Expr.Literal(ast.value(), type);
    }

    @Override
    public Ir.Expr.Group visit(Ast.Expr.Group ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        return new Ir.Expr.Group(visit(ast.expression()));
    }

    @Override
    public Ir.Expr.Binary visit(Ast.Expr.Binary ast) throws AnalyzeException {
        var left = visit(ast.left());
        var right = visit(ast.right());

        switch (ast.operator())
        {
            case "+" ->
            {
                if (left.type().equals(Type.STRING) || right.type().equals(Type.STRING))
                    return new Ir.Expr.Binary(ast.operator(), left, right, Type.STRING);
                if (!(left.type().equals(Type.INTEGER) || left.type().equals(Type.DECIMAL)))
                {
                    throw new AnalyzeException("expected numeric value");
                }
                if (!right.type().equals(left.type()))
                {
                    throw new AnalyzeException("expected right type to be same as left");
                }

                Type resultType = (left.type().equals(Type.INTEGER) ? Type.INTEGER : Type.DECIMAL);
                return new Ir.Expr.Binary(ast.operator(), left, right, resultType);
            }
            case "-", "*", "/" -> {
                if (!left.type().equals(Type.INTEGER) && !left.type().equals(Type.DECIMAL))
                {
                    throw new AnalyzeException("expected numeric value");
                }
                if (!right.type().equals(left.type()))
                {
                    throw new AnalyzeException("expected right type to be same as left");
                }
                Type resultType = (left.type().equals(Type.INTEGER) ? Type.INTEGER : Type.DECIMAL);
                return new Ir.Expr.Binary(ast.operator(), left, right, resultType);
            }
            case "<", "<=", ">", ">=" -> {
                requireSubtype(left.type(), Type.COMPARABLE);
                requireSubtype(right.type(), Type.COMPARABLE);
                return new Ir.Expr.Binary(ast.operator(), left, right, Type.BOOLEAN);
            }
            case "==", "!=" -> {
                requireSubtype(left.type(), Type.EQUATABLE);
                requireSubtype(right.type(), Type.EQUATABLE);
                return new Ir.Expr.Binary(ast.operator(), left, right, Type.BOOLEAN);
            }
            case "AND", "OR" -> {
                requireSubtype(left.type(), Type.BOOLEAN);
                requireSubtype(right.type(), Type.BOOLEAN);
                return new Ir.Expr.Binary(ast.operator(), left, right, Type.BOOLEAN);
            }
            default -> throw new AnalyzeException("invalid binary operator");
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public Ir.Expr.Variable visit(Ast.Expr.Variable ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        var variable = scope.get(ast.name(), false).orElseThrow(() -> new AnalyzeException("undefined variable "));
        return new Ir.Expr.Variable(ast.name(), variable);
    }

    @Override
    public Ir.Expr.Property visit(Ast.Expr.Property ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        var receiver = visit(ast.receiver());
        if (!(receiver.type() instanceof Type.Object typeObj))
        {
            throw new AnalyzeException("receiver type must be object");
        }
        var property = typeObj.scope().get(ast.name(), false).orElseThrow(() -> new AnalyzeException("undefined property "));
        return new Ir.Expr.Property(receiver, ast.name(), property);
    }

    @Override
    public Ir.Expr.Function visit(Ast.Expr.Function ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        var function = scope.get(ast.name(), false).orElseThrow(() -> new AnalyzeException("undefined function"));
        if (!(function instanceof Type.Function func))
        {
            throw new AnalyzeException("undefined function");
        }

        // Check for missing arguments
        if (ast.arguments().size() != func.parameters().size()) {
            throw new AnalyzeException("argument size mismatch for function: " + ast.name());
        }

        List<Ir.Expr> args = new ArrayList<>();
        for (int i = 0; i < ast.arguments().size(); i++) {
            var arg = visit(ast.arguments().get(i));
            requireSubtype(arg.type(), ((Type.Function) function).parameters().get(i));
            args.add(arg);
        }

        return new Ir.Expr.Function(ast.name(), args, func.returns());
    }

    @Override
    public Ir.Expr.Method visit(Ast.Expr.Method ast) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        var receiver = visit(ast.receiver());
        if (!(receiver.type() instanceof Type.Object obj))
        {
            throw new AnalyzeException("receiver must be of type object");
        }
        var methodType = obj.scope().get(ast.name(), false).orElseThrow(() -> new AnalyzeException("undefined method "));

        if (!(methodType instanceof Type.Function functionType))
        {
            throw new AnalyzeException("method is not a function");
        }
        if (ast.arguments().size() != functionType.parameters().size())
        {
            throw new AnalyzeException("arguments size mismatch");
        }

        List<Ir.Expr> args = new ArrayList<>();
        for (int i = 0; i < ast.arguments().size(); i++) {
            var arg = visit(ast.arguments().get(i));
            requireSubtype(arg.type(), functionType.parameters().get(i));
            args.add(arg);
        }
        return new Ir.Expr.Method(receiver, ast.name(), args, functionType.returns());
    }

    @Override
    public Ir.Expr.ObjectExpr visit(Ast.Expr.ObjectExpr ast) throws AnalyzeException {
//        //throw new UnsupportedOperationException("TODO"); //TODO
//        if (Environment.TYPES.containsKey(ast.name()))
//        {
//            throw new AnalyzeException("Object cant be a type");
//        }
//
//        // Create new isolated scope for this object (no parent)
//        Scope objectScope = new Scope(null);  // Changed from new Scope(scope)
//        Type.Object objectType = new Type.Object(objectScope);
//
//        // Process fields
//        List<Ir.Stmt.Let> irFields = new ArrayList<>();
//        Set<String> declaredNames = new HashSet<>();
//
//        for (Ast.Stmt.Let field : ast.fields()) {
//            if (declaredNames.contains(field.name())) {
//                throw new AnalyzeException("Duplicate field name: " + field.name());
//            }
//            declaredNames.add(field.name());
//
//            Optional<Ir.Expr> value;
//            if (field.value().isPresent()) {
//                value = Optional.of(visit(field.value().get()));
//            } else {
//                value = Optional.empty();
//            }
//
//            Type fieldType;
//
//            if (field.type().isPresent()) {
//                fieldType = Environment.TYPES.get(field.type().get());
//                if (fieldType == null) {
//                    throw new AnalyzeException("Unknown field type: " + field.type().get());
//                }
//
//                if (value.isPresent()) {
//                    requireSubtype(value.get().type(), fieldType);
//                }
//
//            } else {
//                if (value.isPresent()) {
//                    fieldType = value.get().type();
//                } else {
//                    //throw new AnalyzeException("Field '" + field.name() + "' must have a type or an initial value");
//                fieldType = Type.ANY;
//                }
//            }
//
//            objectScope.define(field.name(), fieldType);
//            irFields.add(new Ir.Stmt.Let(field.name(), fieldType, value));
//        }
//        // Process methods
//        List<Ir.Stmt.Def> irMethods = new ArrayList<>();
//
//        for (Ast.Stmt.Def method : ast.methods()) {
//            if (declaredNames.contains(method.name())) {
//                throw new AnalyzeException("Duplicate method name: " + method.name());
//            }
//            declaredNames.add(method.name());
//
//            // Method scope inherits from object scope
//            Scope methodScope = new Scope(objectScope);
//            methodScope.define("this", objectType);
//
//            List<Ir.Stmt.Def.Parameter> parameters = new ArrayList<>();
//            for (int i = 0; i < method.parameters().size(); i++) {
//                String paramName = method.parameters().get(i);
//                Type paramType = Type.ANY;
//
//                if (i < method.parameterTypes().size() && method.parameterTypes().get(i).isPresent()) {
//                    String typeName = method.parameterTypes().get(i).get();
//                    paramType = Environment.TYPES.get(typeName);
//                    if (paramType == null) {
//                        throw new AnalyzeException("Unknown parameter type: " + typeName);
//                    }
//                }
//
//                methodScope.define(paramName, paramType);
//                parameters.add(new Ir.Stmt.Def.Parameter(paramName, paramType));
//            }
//
//            Type returnType = method.returnType().isPresent()
//                    ? Environment.TYPES.get(method.returnType().get())
//                    : Type.ANY;
//            if (returnType == null) {
//                throw new AnalyzeException("Unknown return type: " + method.returnType().get());
//            }
//
//            Scope originalScope = scope;
//            try {
//                scope = methodScope;
//                List<Ir.Stmt> body = new ArrayList<>();
//                for (Ast.Stmt stmt : method.body()) {
//                    body.add(visit(stmt));
//                }
//
//                Type.Function funcType = new Type.Function(
//                        parameters.stream().map(p -> p.type()).collect(Collectors.toList()),
//                        returnType
//                );
//
//                objectScope.define(method.name(), funcType);
//                irMethods.add(new Ir.Stmt.Def(method.name(), parameters, returnType, body));
//            } finally {
//                scope = originalScope;
//            }
//        }
//
//        return new Ir.Expr.ObjectExpr(ast.name(), irFields, irMethods, objectType);

        // Check if object name conflicts with existing type
        if (ast.name().isPresent() && Environment.TYPES.containsKey(ast.name().get())) {
            throw new AnalyzeException("Object name conflicts with existing type: " + ast.name().get());
        }

        // Create new isolated scope for this object
        Scope objectScope = new Scope(null);
        Type.Object objectType = new Type.Object(objectScope);

        // Process fields
        List<Ir.Stmt.Let> irFields = new ArrayList<>();
        Set<String> declaredNames = new HashSet<>();

        for (Ast.Stmt.Let field : ast.fields()) {
            // Check for duplicate field names
            if (declaredNames.contains(field.name())) {
                throw new AnalyzeException("Duplicate field name: " + field.name());
            }
            declaredNames.add(field.name());

            // Process field value if present
            Optional<Ir.Expr> value = Optional.empty();
            if (field.value().isPresent()) {
                try {
                    value = Optional.of(visit(field.value().get()));
                } catch (AnalyzeException e) {
                    throw new AnalyzeException("Error in field '" + field.name() + "' initialization: " + e.getMessage());
                }
            }

            // Determine field type
            Type fieldType;
            if (field.type().isPresent()) {
                // Use explicitly declared type
                fieldType = Environment.TYPES.get(field.type().get());
                if (fieldType == null) {
                    throw new AnalyzeException("Unknown field type: " + field.type().get());
                }

                // Verify value matches declared type
                if (value.isPresent()) {
                    requireSubtype(value.get().type(), fieldType);
                }
            } else {
                // Infer type from value or default to ANY
                fieldType = value.isPresent() ? value.get().type() : Type.ANY;
            }

            // Add to object scope and IR
            objectScope.define(field.name(), fieldType);
            irFields.add(new Ir.Stmt.Let(field.name(), fieldType, value));
        }

        // Process methods
        List<Ir.Stmt.Def> irMethods = new ArrayList<>();

        for (Ast.Stmt.Def method : ast.methods()) {
            // Check for duplicate method names
            if (declaredNames.contains(method.name())) {
                throw new AnalyzeException("Duplicate method name: " + method.name());
            }
            declaredNames.add(method.name());

            // Create method scope with access to object fields
            Scope methodScope = new Scope(objectScope);
            methodScope.define("this", objectType);

            // Process parameters
            List<Ir.Stmt.Def.Parameter> parameters = new ArrayList<>();
            for (int i = 0; i < method.parameters().size(); i++) {
                String paramName = method.parameters().get(i);
                Type paramType = Type.ANY; // Default to ANY if no type specified

                if (i < method.parameterTypes().size() && method.parameterTypes().get(i).isPresent()) {
                    String typeName = method.parameterTypes().get(i).get();
                    paramType = Environment.TYPES.get(typeName);
                    if (paramType == null) {
                        throw new AnalyzeException("Unknown parameter type: " + typeName);
                    }
                }

                methodScope.define(paramName, paramType);
                parameters.add(new Ir.Stmt.Def.Parameter(paramName, paramType));
            }

            // Determine return type
            Type returnType = Type.ANY; // Default to ANY if no return type specified
            if (method.returnType().isPresent()) {
                returnType = Environment.TYPES.get(method.returnType().get());
                if (returnType == null) {
                    throw new AnalyzeException("Unknown return type: " + method.returnType().get());
                }
            }

            // Process method body
            Scope originalScope = scope;
            try {
                scope = methodScope;
                List<Ir.Stmt> body = new ArrayList<>();
                for (Ast.Stmt stmt : method.body()) {
                    try {
                        body.add(visit(stmt));
                    } catch (AnalyzeException e) {
                        throw new AnalyzeException("Error in method '" + method.name() + "' body: " + e.getMessage());
                    }
                }

                // Create function type and add to object scope
                Type.Function funcType = new Type.Function(
                        parameters.stream().map(p -> p.type()).collect(Collectors.toList()),
                        returnType
                );
                objectScope.define(method.name(), funcType);
                irMethods.add(new Ir.Stmt.Def(method.name(), parameters, returnType, body));
            } finally {
                scope = originalScope;
            }
        }

        return new Ir.Expr.ObjectExpr(ast.name(), irFields, irMethods, objectType);
    }

    public static void requireSubtype(Type type, Type other) throws AnalyzeException {
        //throw new UnsupportedOperationException("TODO"); //TODO
        if (type.equals(other) || other == Type.ANY)
            return;

        if (type == Type.NIL || type == Type.COMPARABLE || type == Type.ITERABLE)
        {
            if (other == Type.EQUATABLE)
            {
                return;
            }
        }

        if (type == Type.BOOLEAN || type == Type.INTEGER || type == Type.DECIMAL || type == Type.STRING)
        {
            if (other == Type.COMPARABLE || other == Type.EQUATABLE)
                return;
        }

        throw new AnalyzeException("type " + type + " is not subtype of " + other);
    }
}
