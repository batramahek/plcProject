//// LEXER MAIN
//package plc.project;
//
//import plc.project.lexer.LexException;
//import plc.project.lexer.Lexer;
//
//import java.util.Scanner;
//
//public final class Main {
//
//    public static void main(String[] args) {
//        var scanner = new Scanner(System.in);
//        while (true) {
//            var input = scanner.nextLine();
//            try {
//                var tokens = new Lexer(input).lex();
//                System.out.println(tokens);
//            } catch (LexException e) {
//                System.out.println(e.getMessage());
//            }
//        }
//    }
//
//}

//
//// PARSER MAIN
//package plc.project;
//
//import plc.project.lexer.Lexer;
//import plc.project.parser.Parser;
//
//import java.util.Scanner;
//
//public final class Main {
//
//    public static void main(String[] args) {
//        parser(); //edit for manual testing
//    }
//
//    private static void lexer() {
//        var scanner = new Scanner(System.in);
//        while (true) {
//            var input = scanner.nextLine();
//            try {
//                var tokens = new Lexer(input).lex();
//                System.out.println(tokens);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private static void parser() {
//        var scanner = new Scanner(System.in);
//        while (true) {
//            var input = new StringBuilder();
//            var next = scanner.nextLine();
//            while (!next.isEmpty()) {
//                input.append(next).append("\n");
//                next = scanner.nextLine();
//            }
//            try {
//                var tokens = new Lexer(input.toString()).lex();
//                var ast = new Parser(tokens).parseSource();
//                System.out.println(ast);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//}
//
//// EVALUATOR MAIN
//package plc.project;
//
//import plc.project.evaluator.Environment;
//import plc.project.evaluator.EvaluateException;
//import plc.project.evaluator.Evaluator;
//import plc.project.evaluator.RuntimeValue;
//import plc.project.evaluator.Scope;
//import plc.project.lexer.LexException;
//import plc.project.lexer.Lexer;
//import plc.project.lexer.Token;
//import plc.project.parser.Ast;
//import plc.project.parser.ParseException;
//import plc.project.parser.Parser;
//
//import java.util.List;
//import java.util.Scanner;
//import java.util.function.Supplier;
//import java.util.regex.Pattern;
//
//public final class Main {
//
//    public static void main(String[] args) {
//        repl(Main::evaluator); //edit for manual testing
//    }
//
//    private static void lexer(String input) throws LexException {
//        var tokens = new Lexer(input).lex();
//        if (tokens.isEmpty()) {
//            System.out.println(" - (empty)");
//        }
//        for (var token : tokens) {
//            System.out.println(" - " + token.type() + " `" + token.literal() + "`");
//        }
//    }
//
//    private static void parser(String input) throws LexException, ParseException {
//        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
//        System.out.println(ast);
//    }
//
//    private static final Evaluator EVALUATOR = new Evaluator(new Scope(Environment.scope())); //global to retain state changes
//
//    private static void evaluator(String input) throws LexException, ParseException, EvaluateException {
//        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
//        var value = EVALUATOR.visit(ast);
//        System.out.println(value.print());
//    }
//
//    private interface ReplBody {
//        void invoke(String input) throws LexException, ParseException, EvaluateException;
//    }
//
//    private static void repl(ReplBody body) {
//        while (true) {
//            var input = readInput();
//            try {
//                body.invoke(input);
//            } catch (LexException | EvaluateException | ParseException e) {
//                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
//            } catch (RuntimeException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    private static final Scanner SCANNER = new Scanner(System.in);
//
//    private static String readInput() {
//        var input = SCANNER.nextLine();
//        return input.isEmpty() ? readInputMultiline() : input;
//    }
//
//    private static String readInputMultiline() {
//        System.out.println("Multiline input - enter an empty line to submit:");
//        var builder = new StringBuilder();
//        while (true) {
//            System.out.print("> ");
//            var next = SCANNER.nextLine();
//            if (next.isEmpty()) {
//                break;
//            }
//            builder.append(next).append("\n");
//        }
//        return builder.toString();
//    }
//
//}


//
//
////// ANALYZER MAIN
package plc.project;

import plc.project.analyzer.AnalyzeException;
import plc.project.analyzer.Analyzer;
import plc.project.evaluator.Environment;
import plc.project.evaluator.EvaluateException;
import plc.project.evaluator.Evaluator;
import plc.project.evaluator.RuntimeValue;
import plc.project.evaluator.Scope;
import plc.project.lexer.LexException;
import plc.project.lexer.Lexer;
import plc.project.lexer.Token;
import plc.project.parser.Ast;
import plc.project.parser.ParseException;
import plc.project.parser.Parser;

import java.util.List;
import java.util.Scanner;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class Main {

    public static void main(String[] args) {
        repl(Main::analyzer); //edit for manual testing
    }

    private static void lexer(String input) throws LexException {
        var tokens = new Lexer(input).lex();
        if (tokens.isEmpty()) {
            System.out.println(" - (empty)");
        }
        for (var token : tokens) {
            System.out.println(" - " + token.type() + " `" + token.literal() + "`");
        }
    }

    private static void parser(String input) throws LexException, ParseException {
        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
        System.out.println(ast);
    }

    private static final Evaluator EVALUATOR = new Evaluator(new Scope(Environment.scope())); //global to retain state changes

    private static void evaluator(String input) throws LexException, ParseException, EvaluateException {
        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
        var value = EVALUATOR.visit(ast);
        System.out.println(value.print());
    }

    private static final Analyzer ANALYZER = new Analyzer(new plc.project.analyzer.Scope(plc.project.analyzer.Environment.scope()));

    private static void analyzer(String input) throws LexException, ParseException, EvaluateException, AnalyzeException {
        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
        var ir = ANALYZER.visit(ast); //Warning: exceptions may modify scope!
        System.out.println(ir);
        var value = EVALUATOR.visit(ast);
        System.out.println(value.print());
    }

    private interface ReplBody {
        void invoke(String input) throws LexException, ParseException, EvaluateException, AnalyzeException;
    }

    private static void repl(ReplBody body) {
        while (true) {
            var input = readInput();
            try {
                body.invoke(input);
            } catch (LexException | ParseException | AnalyzeException | EvaluateException e) {
                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
    }

    private static final Scanner SCANNER = new Scanner(System.in);

    private static String readInput() {
        var input = SCANNER.nextLine();
        return input.isEmpty() ? readInputMultiline() : input;
    }

    private static String readInputMultiline() {
        System.out.println("Multiline input - enter an empty line to submit:");
        var builder = new StringBuilder();
        while (true) {
            var next = SCANNER.nextLine();
            if (next.isEmpty()) {
                break;
            }
            builder.append(next).append("\n");
        }
        return builder.toString();
    }

}

//
////// GENERATOR MAIN
////package plc.project;
////
////import plc.project.analyzer.AnalyzeException;
////import plc.project.analyzer.Analyzer;
////import plc.project.analyzer.Ir;
////import plc.project.evaluator.Environment;
////import plc.project.evaluator.EvaluateException;
////import plc.project.evaluator.Evaluator;
////import plc.project.evaluator.RuntimeValue;
////import plc.project.evaluator.Scope;
////import plc.project.generator.Generator;
////import plc.project.lexer.LexException;
////import plc.project.lexer.Lexer;
////import plc.project.lexer.Token;
////import plc.project.parser.Ast;
////import plc.project.parser.ParseException;
////import plc.project.parser.Parser;
////
////import java.util.List;
////import java.util.Scanner;
////import java.util.function.Supplier;
////import java.util.regex.Pattern;
////
////public final class Main {
////
////    public static void main(String[] args) {
////        repl(Main::generator); //edit for manual testing
////    }
////
////    private static void lexer(String input) throws LexException {
////        var tokens = new Lexer(input).lex();
////        if (tokens.isEmpty()) {
////            System.out.println(" - (empty)");
////        }
////        for (var token : tokens) {
////            System.out.println(" - " + token.type() + " `" + token.literal() + "`");
////        }
////    }
////
////    private static void parser(String input) throws LexException, ParseException {
////        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
////        System.out.println(ast);
////    }
////
////    private static final Evaluator EVALUATOR = new Evaluator(new Scope(Environment.scope())); //global to retain state changes
////
////    private static void evaluator(String input) throws LexException, ParseException, EvaluateException {
////        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
////        var value = EVALUATOR.visit(ast);
////        System.out.println(value.print());
////    }
////
////    private static final Analyzer ANALYZER = new Analyzer(new plc.project.analyzer.Scope(plc.project.analyzer.Environment.scope()));
////
////    private static void analyzer(String input) throws LexException, ParseException, EvaluateException, AnalyzeException {
////        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
////        var ir = ANALYZER.visit(ast); //Warning: exceptions may modify scope!
////        System.out.println(ir);
////        var value = EVALUATOR.visit(ast);
////        System.out.println(value.print());
////    }
////
////    private static void generator(String input) throws LexException, ParseException, AnalyzeException {
////        var ast = new Parser(new Lexer(input).lex()).parseSource(); //edit for manual testing
////        var ir = ANALYZER.visit(ast); //Warning: exceptions may modify scope!
////        var source = new Generator().visit(ir).toString();
////        System.out.println(source);
////    }
////
////    private interface ReplBody {
////        void invoke(String input) throws LexException, ParseException, EvaluateException, AnalyzeException;
////    }
////
////    private static void repl(ReplBody body) {
////        while (true) {
////            var input = readInput();
////            try {
////                body.invoke(input);
////            } catch (LexException | ParseException | AnalyzeException | EvaluateException e) {
////                System.out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
////            } catch (RuntimeException e) {
////                e.printStackTrace();
////            }
////        }
////    }
////
////    private static final Scanner SCANNER = new Scanner(System.in);
////
////    public static String readInput() {
////        var input = SCANNER.nextLine();
////        return input.isEmpty() ? readInputMultiline() : input;
////    }
////
////    private static String readInputMultiline() {
////        System.out.println("Multiline input - enter an empty line to submit:");
////        var builder = new StringBuilder();
////        while (true) {
////            var next = SCANNER.nextLine();
////            if (next.isEmpty()) {
////                break;
////            }
////            builder.append(next).append("\n");
////        }
////        return builder.toString();
////    }
////
////}
