package plc.project.generator;

import plc.project.analyzer.Ir;
import plc.project.analyzer.Type;

import java.math.BigDecimal;
import java.math.BigInteger;

public class Generator implements Ir.Visitor<StringBuilder, RuntimeException> {

    private final StringBuilder builder = new StringBuilder();
    private int indent = 0;

    private void newline(int indent) {
        builder.append("\n");
        builder.append("    ".repeat(indent));
    }

    @Override
    public StringBuilder visit(Ir.Source ir) {
        builder.append(Environment.imports()).append("\n\n");
        builder.append("public final class Main {").append("\n\n");
        builder.append(Environment.definitions()).append("\n");
        //Java doesn't allow for nested functions, but we will pretend it does.
        //To support simple programs involving functions, we will "hoist" any
        //variable/function declaration at the start of the program to allow
        //these functions to be used as valid Java.
        indent = 1;
        boolean main = false;
        for (var statement : ir.statements()) {
            newline(indent);
            if (!main) {
                if (statement instanceof Ir.Stmt.Let || statement instanceof Ir.Stmt.Def) {
                    builder.append("static ");
                } else {
                    builder.append("public static void main(String[] args) {");
                    main = true;
                    indent = 2;
                    newline(indent);
                }
            }
            visit(statement);
        }
        if (main) {
            builder.append("\n").append("    }");
        }
        indent = 0;
        builder.append("\n\n").append("}");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Let ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO

        if (ir.value().isPresent()) {
            if (ir.type() instanceof Type.Object)
            {
                builder.append("var ").append(ir.name()).append(" = ");
                visit(ir.value().get()); //.append(";\n");
            }
            else {
                builder.append(ir.type().jvmName()).append(" ").append(ir.name()).append(" = ");
                visit(ir.value().get());
            }
        }
        else
        {
            builder.append(ir.type().jvmName()).append(" ").append(ir.name());
        }

        builder.append(';');
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Def ir)
    {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append(ir.returns().jvmName()).append(" ").append(ir.name()).append("(");
//        builder.append(ir.parameters().stream()
//                .map(p -> p.type() + " " + p.name())
//                .reduce((a, b) -> a + ", " + b).orElse(""));

        for(int i = 0; i < ir.parameters().size(); i++)
        {
            Ir.Stmt.Def.Parameter param = ir.parameters().get(i);
            builder.append(param.type().jvmName()).append(" ").append(param.name());
            if (i < ir.parameters().size() - 1) {
                builder.append(", ");
            }
        }

        builder.append(") {");
        indent++;
        for (Ir.Stmt stmt : ir.body()) {
            newline(indent);
            visit(stmt);
        }
        indent--;
        newline(indent);
        builder.append("}");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.If ir) {

        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append("if (");
        visit(ir.condition());
        builder.append(") {");
        //newline(indent++);

        indent++;
        for (int i = 0; i < ir.thenBody().size(); i++)
        {
//            if (i == 0)
//            {
//                newline(indent);
//            }
            newline(indent);
            visit(ir.thenBody().get(i));
        }
        newline(--indent);
        builder.append("}");


        if (!ir.elseBody().isEmpty()) {
            builder.append(" else {");
            indent++;
            //newline(indent);
            for (int i = 0; i < ir.elseBody().size(); i++) {
//                if (i == 0)
//                {
//                    newline(indent);
//                }
                newline(indent);
                visit(ir.elseBody().get(i));
            }
            newline(--indent);
            builder.append("}");
        }
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.For ir)
    {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append("for (");
        builder.append(ir.type().jvmName()).append(" ").append(ir.name()).append(" : ");
        visit(ir.expression());
        builder.append(") {");
        indent++;
        for (Ir.Stmt stmt : ir.body())
        {
            newline(indent);
            visit(stmt);
        }
        indent--;
        newline(indent);
        builder.append("}");

        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Return ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append("return ");

        if(ir.value().isPresent())
        {
            visit(ir.value().get());
        }
        else
        {
            builder.append("null");
        }
        builder.append(";");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Expression ir) {
        visit(ir.expression());
        builder.append(";");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Assignment.Variable ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append(ir.variable().name()).append(" = ");
        visit(ir.value());
        builder.append(";");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Stmt.Assignment.Property ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        visit(ir.property());
        builder.append(" = ");
        visit(ir.value());
        builder.append(";");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Expr.Literal ir) {
        var literal = switch (ir.value()) {
            case null -> "null";
            case Boolean b -> b.toString();
            case BigInteger i -> "new BigInteger(\"" + i + "\")";
            case BigDecimal d -> "new BigDecimal(\"" + d + "\")";
            case String s -> "\"" + s + "\""; //TODO: Escape characters?
            //If the IR value isn't one of the above types, the Parser/Analyzer
            //is returning an incorrect IR - this is an implementation issue,
            //hence throw AssertionError rather than a "standard" exception.
            default -> throw new AssertionError(ir.value().getClass());
        };
        builder.append(literal);
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Expr.Group ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append("(");
        visit(ir.expression());
        builder.append(")");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Expr.Binary ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        if (ir.operator().equals("+"))
        {
            if (ir.type().equals(Type.INTEGER) || ir.type().equals(Type.DECIMAL))
            {
                builder.append("(");
                visit(ir.left());
                builder.append(").add(");
                visit(ir.right());
                builder.append(")");
                return builder;
            }
            else if (ir.type().equals(Type.STRING))
            {
                visit(ir.left());
                builder.append(" + ");
                visit(ir.right());
                return builder;
            }
        }
        else if (ir.operator().equals("-"))
        {
            builder.append("(");
            visit(ir.left());
            builder.append(").subtract(");
            visit(ir.right());
            builder.append(")");
            return builder;
        }
        else if (ir.operator().equals("*"))
        {
            builder.append("(");
            visit(ir.left());
            builder.append(").multiply(");
            visit(ir.right());
            builder.append(")");
            return builder;
        }
        else if (ir.operator().equals("/"))
        {
            if (ir.type().equals(Type.INTEGER))
            {
                builder.append("(");
                visit(ir.left());
                builder.append(").divide(");
                visit(ir.right());
                builder.append(")");
                return builder;
            }
            else if (ir.type().equals(Type.DECIMAL))
            {
                builder.append("(");
                visit(ir.left());
                builder.append(").divide(");
                visit(ir.right());
                builder.append(", RoundingMode.HALF_EVEN)");
                return builder;
            }
        }
        else if (ir.operator().equals("<") || ir.operator().equals("<=") || ir.operator().equals(">") || ir.operator().equals(">="))
        {
            builder.append("(");
            visit(ir.left());
            builder.append(").compareTo(");
            visit(ir.right());
            builder.append(") ").append(ir.operator()).append(" 0");
            return builder;
        }
        else if (ir.operator().equals("=="))
        {
            builder.append("Objects.equals(");
            visit(ir.left());
            builder.append(", ");
            visit(ir.right());
            builder.append(")");
            return builder;
        }
        else if (ir.operator().equals("!="))
        {
            builder.append("!Objects.equals(");
            visit(ir.left());
            builder.append(", ");
            visit(ir.right());
            builder.append(")");
            return builder;
        }
        else if (ir.operator().equals("AND"))
        {
            if (ir.left() instanceof Ir.Expr.Binary leftBinary && leftBinary.operator().equals("||")) {
                builder.append("(");
                visit(ir.left());
                builder.append(")");
            } else if (!(ir.left() instanceof Ir.Expr.Literal || ir.left() instanceof Ir.Expr.Variable || ir.left() instanceof Ir.Expr.Property)) {
                builder.append("(");
                visit(ir.left());
                builder.append(")");
            } else {
                visit(ir.left());
            }
            builder.append(" && ");
            visit(ir.right());
            return builder;
        }
        else if (ir.operator().equals("OR"))
        {
            visit(ir.left());
            builder.append(" || ");
            visit(ir.right());
            return builder;
        }
        throw new UnsupportedOperationException("TODO"); //TODO
    }

    @Override
    public StringBuilder visit(Ir.Expr.Variable ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append(ir.name());
        return builder;

    }

    @Override
    public StringBuilder visit(Ir.Expr.Property ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        visit(ir.receiver());
        builder.append(".").append(ir.name());

        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Expr.Function ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append(ir.name()).append("(");
        for (int i = 0; i < ir.arguments().size(); i++) {
            if (i > 0) builder.append(", ");
            visit(ir.arguments().get(i));
        }
        builder.append(")");
        return builder;
    }

    @Override
    public StringBuilder visit(Ir.Expr.Method ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        visit(ir.receiver());
        builder.append(".").append(ir.name());
        builder.append("(");
        for (int i = 0; i < ir.arguments().size(); i++) {
            if (i > 0) builder.append(", ");
            visit(ir.arguments().get(i));
        }
        builder.append(")");
        return builder;

    }

    @Override
    public StringBuilder visit(Ir.Expr.ObjectExpr ir) {
        //throw new UnsupportedOperationException("TODO"); //TODO
        builder.append("new Object() {");
        indent++;

        for (Ir.Stmt.Let field : ir.fields()) {
            newline(indent);
            visit(field);
        }

        for (Ir.Stmt.Def method : ir.methods()) {
            newline(indent);
            visit(method);
        }

        indent--;
        newline(indent);
        builder.append("}");
        return builder;
    }

}
