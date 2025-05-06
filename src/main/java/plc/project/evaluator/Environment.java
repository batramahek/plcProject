package plc.project.evaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigInteger;

public final class Environment {

    public static Scope scope() {
        var scope = new Scope(null);
        scope.define("debug", new RuntimeValue.Function("debug", Environment::debug));
        scope.define("print", new RuntimeValue.Function("print", Environment::print));
        scope.define("log", new RuntimeValue.Function("log", Environment::log));
        scope.define("list", new RuntimeValue.Function("list", Environment::list));
        scope.define("range", new RuntimeValue.Function("range", Environment::range));
        scope.define("variable", new RuntimeValue.Primitive("variable"));
        scope.define("function", new RuntimeValue.Function("function", Environment::function));
        var object = new RuntimeValue.ObjectValue(Optional.of("Object"), new Scope(null));
        scope.define("object", object);
        object.scope().define("property", new RuntimeValue.Primitive("property"));
        object.scope().define("method", new RuntimeValue.Function("method", Environment::method));
        return scope;
    }

    /**
     * Prints the raw RuntimeValue.toString() result.
     */
    private static RuntimeValue debug(List<RuntimeValue> arguments) throws EvaluateException {
        if (arguments.size() != 1) {
            throw new EvaluateException("Expected debug to be called with 1 argument.");
        }
        System.out.println(arguments.getFirst());
        return new RuntimeValue.Primitive(null);
    }

    private static RuntimeValue print(List<RuntimeValue> arguments) throws EvaluateException {
        if (arguments.size() != 1) {
            throw new EvaluateException("Expected print to be called with 1 argument.");
        }
        System.out.println(arguments.getFirst().print());
        return new RuntimeValue.Primitive(null);
    }

    static RuntimeValue log(List<RuntimeValue> arguments) throws EvaluateException {
        if (arguments.size() != 1) {
            throw new EvaluateException("Expected log to be called with 1 argument.");
        }
        System.out.println("log: " + arguments.getFirst().print());
        return arguments.getFirst(); //size validated by print
    }

    private static RuntimeValue list(List<RuntimeValue> arguments) {
        return new RuntimeValue.Primitive(arguments);
    }

    private static RuntimeValue range(List<RuntimeValue> arguments) {
        if (arguments.size() != 2) {
            throw new UnsupportedOperationException("Range expects exactly two arguments.");
        }

        // Validate both arguments are Primitive and contain BigInteger values
        if (!(arguments.get(0) instanceof RuntimeValue.Primitive startPrimitive) ||
                !(arguments.get(1) instanceof RuntimeValue.Primitive endPrimitive)) {
            throw new UnsupportedOperationException("Range arguments must be Primitive types.");
        }

        if (!(startPrimitive.value() instanceof BigInteger start) ||
                !(endPrimitive.value() instanceof BigInteger end)) {
            throw new UnsupportedOperationException("Range arguments must be integers.");
        }

        if (start.compareTo(end) > 0) {
            throw new UnsupportedOperationException("Start of range must be less than or equal to end.");
        }

        List<RuntimeValue> values = new ArrayList<>();
        for (BigInteger i = start; i.compareTo(end) < 0; i = i.add(BigInteger.ONE)) {
            values.add(new RuntimeValue.Primitive(i));
        }

        // Wrap the list into a RuntimeValue.Primitive, just like the built-in list() does
        return new RuntimeValue.Primitive(values);
//        // throw new UnsupportedOperationException("TODO"); //TODO
    }

    private static RuntimeValue function(List<RuntimeValue> arguments) {
        return new RuntimeValue.Primitive(arguments);
    }

    private static RuntimeValue method(List<RuntimeValue> arguments) {
        return new RuntimeValue.Primitive(arguments.subList(1, arguments.size()));
    }

}
