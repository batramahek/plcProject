package plc.project.parser;

import org.checkerframework.checker.units.qual.A;
import plc.project.lexer.Token;

import javax.swing.*;
import java.awt.print.PrinterAbortException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

import static com.google.common.base.Preconditions.checkState;

/**
 * This style of parser is called <em>recursive descent</em>. Each rule in our
 * grammar has dedicated function, and references to other rules correspond to
 * calling that function. Recursive rules are therefore supported by actual
 * recursive calls, while operator precedence is encoded via the grammar.
 *
 * <p>The parser has a similar architecture to the lexer, just with
 * {@link Token}s instead of characters. As before, {@link TokenStream#peek} and
 * {@link TokenStream#match} help with traversing the token stream. Instead of
 * emitting tokens, you will instead need to extract the literal value via
 * {@link TokenStream#get} to be added to the relevant AST.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens)
    {
        this.tokens = new TokenStream(tokens);
    }

    public Ast.Source parseSource() throws ParseException {
        List<Ast.Stmt> statements = new ArrayList<>();
        while (tokens.has(0))
        {
            statements.add(parseStmt());
        }
        return new Ast.Source(statements);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    public Ast.Stmt parseStmt() throws ParseException {
        if (tokens.peek("LET"))
        {
            return parseLetStmt();
        }
        else if (tokens.peek("DEF"))
        {
            return parseDefStmt();
        }
        else if (tokens.peek("IF"))
        {
            return parseIfStmt();
        }
        else if (tokens.peek("FOR"))
        {
            return parseForStmt();
        }
        else if (tokens.peek("RETURN"))
        {
            return parseReturnStmt();
        }
        else
        {
            return parseExpressionOrAssignmentStmt();
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.Let parseLetStmt() throws ParseException {
//        if (!tokens.match("LET"))
//        {
//            throw new ParseException("Expected 'LET'");
//        }
//
//        if (!tokens.peek(Token.Type.IDENTIFIER))
//        {
//            throw new ParseException("Expected Identifier after 'LET'.");
//        }
//
//        String name = tokens.get(0).literal();
//        tokens.match(Token.Type.IDENTIFIER);
//
//        Optional<Ast.Expr> value = Optional.empty();
//
//        if (tokens.match("="))
//        {
//            value = Optional.of(parseExpr());
//        }
//        if (!tokens.match(";"))
//        {
//            throw new ParseException("expected ; at the end of stmt");
//        }
//
//        return new Ast.Stmt.Let(name, value);

        if (!tokens.match("LET")) {
            throw new ParseException("Expected 'LET'");
        }

        if (!tokens.peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected Identifier after 'LET'.");
        }

        String name = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER);

        Optional<String> type = Optional.empty();
        Optional<Ast.Expr> value = Optional.empty();

        if (tokens.match(":")) {
            if (!tokens.peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected type after ':'");
            }
            type = Optional.of(tokens.get(0).literal());
            tokens.match(Token.Type.IDENTIFIER);
        }

        if (tokens.match("=")) {
            value = Optional.of(parseExpr());
        }

        if (!tokens.match(";")) {
            throw new ParseException("expected ; at the end of stmt");
        }

        return new Ast.Stmt.Let(name, type, value);
    }


    private Ast.Stmt.Def parseDefStmt() throws ParseException
    {   if (!tokens.match("DEF"))
    {
        throw new ParseException("expected 'DEF'"); //TODO
    }

        if (!tokens.peek(Token.Type.IDENTIFIER))
        {
            throw new ParseException("Expected Identifier after 'DEF'.");
        }

        String funcName = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER);

        if (!tokens.match("("))
        {
            throw new ParseException("Expected '(' after function name");
        }

        List<String> parameters = new ArrayList<>();
        List<Optional<String>> parameterTypes = new ArrayList<>(); // To store parameter types

        if (!tokens.peek(")")) {
            while (true) {
                if (!tokens.peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected parameter name");
                }
                parameters.add(tokens.get(0).literal());
                tokens.match(Token.Type.IDENTIFIER);

                // Check if the parameter has a type
                Optional<String> paramType = Optional.empty();
                if (tokens.match(":")) {
                    if (!tokens.peek(Token.Type.IDENTIFIER)) {
                        throw new ParseException("Expected parameter type after ':'");
                    }
                    paramType = Optional.of(tokens.get(0).literal()); // Extract the type
                    tokens.match(Token.Type.IDENTIFIER);
                }
                parameterTypes.add(paramType);

                if (tokens.peek(")")) {
                    break; // Done parsing parameters
                } else if (tokens.peek(",")) {
                    tokens.match(",");
                    if (tokens.peek(")")) {
                        throw new ParseException("Trailing comma not allowed in parameter list");
                    }
                } else {
                    throw new ParseException("Expected ',' or ')' after parameter");
                }
            }
        }
        tokens.match(")");

        Optional<String> returnType = Optional.empty();

        if (tokens.match(":")) {
            if (!tokens.peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected return type after ':'");
            }
            returnType = Optional.of(tokens.get(0).literal());
            tokens.match(Token.Type.IDENTIFIER);
        }

        if (!tokens.match("DO"))
        {
            throw new ParseException("Expected 'DO' after function parameters");
        }

        List<Ast.Stmt> body = new ArrayList<>();
        while (!tokens.peek("END"))
        {
            body.add(parseStmt());
        }
        tokens.match("END");

        return new Ast.Stmt.Def(funcName, parameters, parameterTypes, returnType, body);
    }

    private Ast.Stmt.If parseIfStmt() throws ParseException
    {
        if (!tokens.match("IF")) {
            throw new ParseException("Expected 'IF'");
        }

        Ast.Expr condition = parseExpr();

        if (!tokens.match("DO"))
        {
            throw new ParseException("Expected 'DO' after condition");
        }

        List<Ast.Stmt> thenStatements = new ArrayList<>();
        while (!tokens.peek("END") && !tokens.peek("ELSE"))
        {
            thenStatements.add(parseStmt());
        }

        List<Ast.Stmt> elseStatements = new ArrayList<>();
        if (tokens.match("ELSE"))
        {
            while (!tokens.peek("END"))
            {
                elseStatements.add(parseStmt());
            }
        }

        if (!tokens.match("END"))
        {
            throw new ParseException("Expected 'END' after IF statement");
        }

        return new Ast.Stmt.If(condition, thenStatements, elseStatements);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.For parseForStmt() throws ParseException
    {
        if (!tokens.match("FOR"))
        {
            throw new ParseException("Expected 'FOR'");
        }

        if (!tokens.peek(Token.Type.IDENTIFIER))
        {
            throw new ParseException("Expected identifier after 'FOR'");
        }

        String identifier = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER);

        if (!tokens.match("IN"))
        {
            throw new ParseException("Expected 'IN' after identifier");
        }

        Ast.Expr iterable = parseExpr();

        if (!tokens.match("DO"))
        {
            throw new ParseException("Expected 'DO' after iterable expression");
        }

        List<Ast.Stmt> statements = new ArrayList<>();
        while (!tokens.peek("END"))
        {
            statements.add(parseStmt());
        }

        if (!tokens.match("END"))
        {
            throw new ParseException("Expected 'END' after FOR statement");
        }

        return new Ast.Stmt.For(identifier, iterable, statements);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt.Return parseReturnStmt() throws ParseException
    {
        if (!tokens.match("RETURN"))
        {
            throw new ParseException("Expected 'RETURN'");
        }

        Optional<Ast.Expr> value = Optional.empty();
        if (!tokens.peek(";"))
        {
            value = Optional.of(parseExpr());
        }

        if (!tokens.match(";"))
        {
            throw new ParseException("Expected ';' after RETURN statement");
        }

        return new Ast.Stmt.Return(value);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Stmt parseExpressionOrAssignmentStmt() throws ParseException
    {
        Ast.Expr expr = parseExpr();

        if (tokens.match("="))
        {
            Ast.Expr value = parseExpr();
            if (!tokens.match(";"))
            {
                throw new ParseException("Expected ';' after assignment");
            }
            return new Ast.Stmt.Assignment(expr, value);
        }
        else
        {
            if (!tokens.match(";"))
            {
                throw new ParseException("Expected ';' after expression");
            }
            return new Ast.Stmt.Expression(expr);
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    public Ast.Expr parseExpr() throws ParseException
    {
        return parseLogicalExpr();
    }

    private Ast.Expr parseLogicalExpr() throws ParseException
    {
        Ast.Expr left = null;
        Ast.Expr right = null;
        left = parseComparisonExpr();
        while (tokens.peek("&&") || tokens.peek("||") || tokens.peek("AND") || tokens.peek("OR"))
        {
            String operator = tokens.get(0).literal();
            tokens.match(Token.Type.IDENTIFIER);
            right = parseComparisonExpr();
//            if (!tokens.match("AND") && !tokens.match("OR"))
//            {
//                return new Ast.Expr.Binary(operator, left, right);
//            }
//            else
//            {
//                left = new Ast.Expr.Binary(operator, left, right);
//            }
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseComparisonExpr() throws ParseException
    {
//        Ast.Expr left = null;
//        Ast.Expr right = null;
//        left = parseAdditiveExpr();
//        while (tokens.peek("<") || tokens.peek( "<=") || tokens.peek(">") || tokens.peek( ">=") || tokens.peek("==") || tokens.peek( "!="))
//        {
//            String operator = tokens.get(0).literal();
//            tokens.match(Token.Type.OPERATOR);
//            right = parseAdditiveExpr();
//
//            if (!tokens.match("<") && !tokens.match("<=") || tokens.match(">") && !tokens.match(">=") || tokens.match("==") || tokens.match( "!="))
//            {
//                return new Ast.Expr.Binary(operator, left, right);
//            }
//            else
//            {
//                left = new Ast.Expr.Binary(operator, left, right);
//            }
//        }
//        return left;
//        //throw new UnsupportedOperationException("TODO"); //TODO

        Ast.Expr left = parseAdditiveExpr();
        while (tokens.peek("<") || tokens.peek("<=") || tokens.peek(">") || tokens.peek(">=") || tokens.peek("==") || tokens.peek("!="))
        {
            String operator = tokens.get(0).literal();
            tokens.match(Token.Type.OPERATOR);
            Ast.Expr right = parseAdditiveExpr();
            left = new Ast.Expr.Binary(operator, left, right);
        }
        return left;
    }

    private Ast.Expr parseAdditiveExpr() throws ParseException
    {
        Ast.Expr left = null;
        Ast.Expr right = null;
        left = parseMultiplicativeExpr();
        while (tokens.peek("+") || tokens.peek( "-")) {
            String operator = tokens.get(0).literal();
            tokens.match(Token.Type.OPERATOR);
            right = parseMultiplicativeExpr();

            if (!tokens.peek("+") && !tokens.peek("-"))
            {
                return new Ast.Expr.Binary(operator, left, right);
            }
            else
            {
                left = new Ast.Expr.Binary(operator, left, right);
            }
        }
        return left;
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseMultiplicativeExpr() throws ParseException
    {
        Ast.Expr left = parseSecondaryExpr();

        while (true) {
            String operator = null;

            if (tokens.peek("*")) {
                operator = "*";
                tokens.match(Token.Type.OPERATOR);
            } else if (tokens.peek("/")) {
                operator = "/";
                tokens.match(Token.Type.OPERATOR);
            } else {
                break; // Exit loop if no matching operators
            }

            Ast.Expr right = parseSecondaryExpr();
            left = new Ast.Expr.Binary(operator, left, right);
        }

        return left;
    }

    private Ast.Expr parseSecondaryExpr() throws ParseException
    {
        Ast.Expr left = parsePrimaryExpr();

        // If there's no dot, return the primary expression
        if (!tokens.match(".")) {
            return left;
        }

        // Handle method calls and property accesses
        while (true) {
            // Parse the identifier after the dot
            if (!tokens.peek(Token.Type.IDENTIFIER)) {
                throw new ParseException("Expected identifier after '.'");
            }
            String name = tokens.get(0).literal();
            tokens.match(Token.Type.IDENTIFIER);

            // Check if it's a method call (has parentheses)
            if (tokens.match("(")) {
                List<Ast.Expr> arguments = new ArrayList<>();
                while (!tokens.peek(")")) {
                    arguments.add(parseExpr());
                    if (tokens.peek(",")) {
                        tokens.match(",");
                        if (tokens.peek(")")) {
                            throw new ParseException("Trailing comma in function call arguments");
                        }
                    }
                    else if (tokens.peek(")")) {
                        break;
                    }
                    else
                    {
                        throw new ParseException("missing comma");
                    }

                }
                tokens.match(")"); // Match the closing parenthesis

                // Create a Method node with the receiver, name, and arguments
                left = new Ast.Expr.Method(left, name, arguments);
            } else {
                // If no parentheses, it's a property access
                left = new Ast.Expr.Property(left, name);
            }

            // If there's no more dots, return the result
            if (!tokens.match(".")) {
                return left;
            }
        }
        // throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parsePrimaryExpr() throws ParseException
    {
        // Handle literals: NIL, TRUE, FALSE
        if (tokens.match("NIL")) {
            return new Ast.Expr.Literal(null);
        } else if (tokens.match("TRUE")) {
            return new Ast.Expr.Literal(true);
        } else if (tokens.match("FALSE")) {
            return new Ast.Expr.Literal(false);
        }

        // Handle numeric literals: INTEGER, DECIMAL
        else if (tokens.match(Token.Type.INTEGER)) {
            try {
                Token token = tokens.get(-1);
                String intLiteral = token.literal();
                if (intLiteral.matches("\\d+[eE][-+]?\\d+")) {
                    return new Ast.Expr.Literal(new BigInteger(new BigDecimal(intLiteral).toBigIntegerExact().toString()));
                }
                BigInteger value = new BigInteger(intLiteral);
                return new Ast.Expr.Literal(value);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid integer literal: " + tokens.get(-1).literal());
            }
        } else if (tokens.match(Token.Type.DECIMAL)) {
            try {
                Token token = tokens.get(-1);
                String literal = token.literal();
                if (literal.matches("\\d+(\\.\\d+)?[eE][-+]?\\d+")) {
                    return new Ast.Expr.Literal(new BigDecimal(literal));
                }
                BigDecimal value = new BigDecimal(literal);
                return new Ast.Expr.Literal(value);
            } catch (NumberFormatException e) {
                throw new ParseException("Invalid decimal literal: " + tokens.get(-1).literal());
            }
        }

        // Handle CHARACTER literals
        else if (tokens.match(Token.Type.CHARACTER)) {
            String charLiteral = tokens.get(-1).literal();
            if (charLiteral.length() == 3 && charLiteral.startsWith("'") && charLiteral.endsWith("'")) {
                return new Ast.Expr.Literal(charLiteral.charAt(1));
            }
            //escapes
            else
            {
                String tmp = charLiteral.substring(1, charLiteral.length() - 1); // remove ''
                tmp = tmp.replace("\\b", "\b");
                tmp = tmp.replace("\\t", "\t");
                tmp = tmp.replace("\\f", "\f");
                tmp = tmp.replace("\\r", "\r");
                tmp = tmp.replace("\\n", "\n");
                tmp = tmp.replace("\\'", "\'");
                if (tmp.equals("'\\\""))
                {
                    tmp = "'\"'";
                }
                if (tmp.equals("'\\\\'"))
                {
                    tmp = "'\\''";
                }
                if (tmp.equals("'\\\''"))
                {
                    tmp = "'\''";
                }
                Character c = tmp.charAt(0);
                return new Ast.Expr.Literal(c);
                //throw new ParseException("Invalid character literal: " + charLiteral);
            }
        }
        else if (tokens.peek("OBJECT"))
        {
            return parseObjectExpr();
        }
        // Handle STRING literals
        else if (tokens.match(Token.Type.STRING)) {
            String strLiteral = tokens.get(-1).literal();
            strLiteral = strLiteral.substring(1, strLiteral.length() - 1) // Remove quotes
                    .replace("\\b", "\b")
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\\\", "\\")
                    .replace("\\'", "'");
            return new Ast.Expr.Literal(strLiteral);
        }


        // Handle grouped expressions: (expression)
        else if (tokens.match("(")) {
            Ast.Expr expr = parseExpr();
            if (!tokens.match(")")) {
                throw new ParseException("Expected ')' after grouped expression");
            }
            return new Ast.Expr.Group(expr);
        }

        // Handle identifiers (variables or function calls)
        else if (tokens.match(Token.Type.IDENTIFIER)) {
            String name = tokens.get(-1).literal();

            // Check if it's a function call
            if (tokens.match("(")) {
                List<Ast.Expr> arguments = new ArrayList<>();
                while (!tokens.peek(")")) {
                    arguments.add(parseExpr());
                    if (tokens.peek(",")) {
                        tokens.match(",");
                        if (tokens.peek(")")) {
                            throw new ParseException("Trailing comma in function call arguments");
                        }
                    }
                    else if (tokens.peek(")"))
                    {
                        break;
                    }
                    else
                    {
                        throw new ParseException("missing comma");
                    }
                }
                tokens.match(")"); // Match the closing parenthesis
                return new Ast.Expr.Function(name, arguments);
            }

            // Otherwise, it's a variable reference
            return new Ast.Expr.Variable(name);
        }

        // If none of the above, throw an exception
        throw new ParseException("Unexpected token: " + (tokens.has(0) ? tokens.get(0).literal() : "end of input"));



//        if (tokens.peek("NIL", Token.Type.INTEGER, Token.Type.STRING, "true", "false")) {
//            return parseLiteralExpr();
//        }
//        if (tokens.peek("(")) {
//            return parseGroupExpr();
//        }
//        if (tokens.peek(Token.Type.IDENTIFIER)) {
//            return parseVariableOrFunctionExpr();
//        }
//        throw new ParseException("Unexpected token");

        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.Literal parseLiteralExpr() throws ParseException
    {
        Token token = tokens.get(-1); // Get the current token
        tokens.match(token.literal()); // Consume the token

        switch (token.type()) {
            case IDENTIFIER:
                String literal = token.literal();
                if ("NIL".equals(literal)) {
                    return new Ast.Expr.Literal(null);
                } else if ("TRUE".equals(literal)) {
                    return new Ast.Expr.Literal(true);
                } else if ("FALSE".equals(literal)) {
                    return new Ast.Expr.Literal(false);
                } else {
                    throw new ParseException("Unexpected identifier: " + literal);
                }
            case INTEGER:
                try {
                    return new Ast.Expr.Literal(new BigInteger(token.literal()));
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid integer literal: " + token.literal());
                }
            case DECIMAL:
                try {
                    return new Ast.Expr.Literal(new BigDecimal(token.literal()));
                } catch (NumberFormatException e) {
                    throw new ParseException("Invalid decimal literal: " + token.literal());
                }
            case CHARACTER:
                String charLiteral = token.literal();
                if (charLiteral.length() == 3 && charLiteral.startsWith("'") && charLiteral.endsWith("'")) {
                    return new Ast.Expr.Literal(charLiteral.charAt(1)); // Extract the character
                } else {
                    throw new ParseException("Invalid character literal: " + charLiteral);
                }
            case STRING:
                String strLiteral = token.literal();
                strLiteral = strLiteral.substring(1, strLiteral.length() - 1) // Remove quotes
                        .replace("\\n", "\n")
                        .replace("\\t", "\t")
                        .replace("\\\"", "\"")
                        .replace("\\\\", "\\"); // Handle escape sequences
                return new Ast.Expr.Literal(strLiteral);
            default:
                throw new ParseException("Unexpected literal type: " + token.type());
        }
//        Token token = tokens.get(0);
//        tokens.match(token.type());
//        return new Ast.Expr.Literal(token.literal());
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.Group parseGroupExpr() throws ParseException
    {
        if (!tokens.match("("))
        {
            throw new ParseException("Expected '(' at start of grouped expression.");
        }
        Ast.Expr expr = parseExpr();  // Parse the inner expression
        if (!tokens.match(")"))
        {
            throw new ParseException("Expected ')' at end of grouped expression.");
        }
        return new Ast.Expr.Group(expr);
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr.ObjectExpr parseObjectExpr() throws ParseException
    {
        // Optional name of the object
        tokens.match("OBJECT");
        Optional<String> name = Optional.empty();
        if (tokens.peek(Token.Type.IDENTIFIER) && !tokens.get(0).literal().equals("DO")) {
            name = Optional.of(tokens.get(0).literal()); // Capture the object name
            tokens.match(Token.Type.IDENTIFIER); // Consume the identifier
        }

        // Debug print to check the next token

        // Match the "DO" token after the object name or directly after "OBJECT" if no name
        if (!tokens.peek("DO")) {
            throw new ParseException("Expected 'DO' after object name");
        }
        tokens.match("DO");
        // Lists for storing LET and DEF statements
        List<Ast.Stmt.Let> fields = new ArrayList<>();
        List<Ast.Stmt.Def> methods = new ArrayList<>();

        boolean seenMethod = false;

        // Parse through LET and DEF statements until "END" is encountered
        while (!tokens.peek("END")) {
            if (tokens.peek("LET")) {
                //tokens.peek("LET");  // Consume "LET"
                if (seenMethod)
                {
                    throw new ParseException("cannot have let after def in object");
                }
                fields.add(parseLetStmt());  // Parse the "LET" statement
            } else if (tokens.peek("DEF")) {
                //tokens.match("DEF");  // Consume "DEF"
                seenMethod = true;
                methods.add(parseDefStmt());  // Parse the "DEF" statement
            } else {
                throw new ParseException("Expected 'LET' or 'DEF' inside OBJECT");
            }
        }

        // Ensure the "END" token is present at the end of the object declaration
        if (!tokens.match("END")) {
            throw new ParseException("Expected 'END' after OBJECT");
        }

        // Return the parsed ObjectExpr, including the name (if any), fields, and methods
        return new Ast.Expr.ObjectExpr(name, fields, methods);

        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private Ast.Expr parseVariableOrFunctionExpr() throws ParseException
    {
        if (!tokens.peek(Token.Type.IDENTIFIER))
        {
            throw new ParseException("Expected an identifier.");
        }

        String name = tokens.get(0).literal();
        tokens.match(Token.Type.IDENTIFIER);

        if (tokens.match("("))
        {  // Function Call
            List<Ast.Expr> arguments = new ArrayList<>();
            while (!tokens.peek(")"))
            {
                arguments.add(parseExpr());
                if (!tokens.peek(")") && !tokens.match(","))
                {
                    throw new ParseException("Expected ',' or ')' in function call arguments.");
                }
            }
            tokens.match(")");
            return new Ast.Expr.Function(name, arguments);
        }
        else
        {  // Variable Reference
            return new Ast.Expr.Variable(name);
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at (index + offset).
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Returns the token at (index + offset).
         */
        public Token get(int offset) {
            checkState(has(offset));
            return tokens.get(index + offset);
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is either a {@link Token.Type}, matching tokens
         * of that type, or a {@link String}, matching tokens with that literal.
         * In effect, {@code new Token(Token.Type.IDENTIFIER, "literal")} is
         * matched by both {@code peek(Token.Type.IDENTIFIER)} and
         * {@code peek("literal")}.
         */
        public boolean peek(Object... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var token = tokens.get(index + offset);
                var pattern = patterns[offset];
                checkState(pattern instanceof Token.Type || pattern instanceof String, pattern);
                if (!token.type().equals(pattern) && !token.literal().equals(pattern)) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the token stream.
         */
        public boolean match(Object... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
            }
            return peek;
        }

    }

}
