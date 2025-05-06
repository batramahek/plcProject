package plc.project.lexer;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through a combination of {@link #lex()}, which repeatedly
 * calls {@link #lexToken()} and skips over whitespace/comments, and
 * {@link #lexToken()}, which determines the type of the next token and
 * delegates to the corresponding lex method.
 *
 * <p>Additionally, {@link CharStream} manages the lexer state and contains
 * {@link CharStream#peek} and {@link CharStream#match}. These are helpful
 * utilities for working with character state and building tokens.
 */
public final class Lexer {

    private final CharStream chars;

    public Lexer(String input)
    {
        chars = new CharStream(input);
    }

    public List<Token> lex() throws LexException
    {
        var tokens = new ArrayList<Token>();

        // start looping - while there are characters in the input
        while (chars.has(0)) {
            // Skip over whitespace
            if (chars.peek("[\\s]") || (chars.peek("[\b\n\r\t]+"))) {
                chars.consume();
                chars.skip(); // consume and skip the whitespace
                continue;
            }

            if (chars.peek("/", "/")) {
                lexComment();
            }
            // attempt to lex a valid token
            else {
                try {
                    tokens.add(lexToken());
                } catch (Exception e) {
                    // throw LexException for invalid characters or sequences at positions
                    throw new plc.project.lexer.LexException("Invalid token at position " + chars.index);
                }
            }
        }

        return tokens; // return the list of tokens
    }

    private void lexComment()
    {
        // match the starting //
        if (chars.peek("/", "/")) {
            chars.match("/", "/");

            // while there are no whitespace or the chars has characters left
            while (!chars.peek("[\\n\\r]") && (chars.has(0)))
            {
                // consume and skip
                chars.consume();
                chars.skip();
            }

            if (chars.peek("[\\n\\r]")) {
                chars.consume();
            }

            chars.skip();
        }
        else
        {
            // throw exception if no starting //
            throw new UnsupportedOperationException("invalid comment");
        }
    }

    private Token lexToken()
    {
        // lex identifier
        if (chars.peek("[A-Za-z_]"))
            return lexIdentifier();
            // lex number
        else if (chars.peek("[+\\-]", "[0-9]") || chars.peek("[0-9]"))
            return lexNumber();
            // lex character
        else if (chars.peek("\'"))
            return lexCharacter();
            // lex string
        else if (chars.peek("\""))
            return lexString();
            // lex operator
        else
            return lexOperator();
    }

    private Token lexIdentifier()
    {
        // [A-Za-z_] [A-Za-z0-9_-]*
        // match the starting char
        chars.match("[A-Za-z_]");

        // continue matching until there are characters that match [A-Za-z0-9_-]*
        while (chars.match("[A-Za-z0-9_-]*"));
        String literal = chars.emit();
        return new Token(Token.Type.IDENTIFIER, literal);
    }

    private Token lexNumber() {
        // optionally match a sign
        if (chars.peek("[+\\-]")) {
            chars.match("[+\\-]");
        }

        // Match one or more digs for int
        while (chars.peek("\\d")) {
            chars.match("\\d");
        }

        boolean isDec = false; // Flag to indicate if a decimal number

        // Check if decimal is followed by at least one dig
        if (chars.peek("[.]", "\\d")) {
            isDec = true;
            chars.match("[.]");
            while (chars.peek("\\d")) {
                chars.match("\\d");
            }
        }

        // check for an exponent part followed by dig
        if (chars.peek("[e]") && chars.peek("[e]", "\\d")) {
            chars.match("[e]");
            // optionally match a sign for exponent.
            if (chars.peek("[+\\-]")) {
                chars.match("[+\\-]");
            }
            // must be at least one digit after the exponent
            if (!chars.peek("\\d"))
                throw new UnsupportedOperationException("no dig after exp");

            while (chars.peek("\\d")) {
                chars.match("\\d");
            }
        }

        String literal = chars.emit();
        if (isDec)
            return new Token(Token.Type.DECIMAL, literal);
        else
            return new Token(Token.Type.INTEGER, literal);
    }

    private Token lexCharacter()
    {
        // match the starting single quote
        if (chars.peek("\'"))
            chars.match("\'");
        else
            throw new UnsupportedOperationException("missing opening single quote");

        //catch the escape
        if (chars.peek("\\\\"))
            lexEscape();
        else if (chars.peek("[^\'\\n\\r\\\\]"))
            chars.match("[^\'\\n\\r\\\\]");
        else
            throw new UnsupportedOperationException("invalid char");

        // match the ending single quote
        if (chars.peek("\'"))
        {
            chars.match("\'");
            String literal = chars.emit();
            return new Token(Token.Type.CHARACTER, literal);
        }
        else
            throw new UnsupportedOperationException("missing closing single quote");
    }

    private Token lexString()
    {
        // match starting double quote
        chars.match("\"");
        while (chars.has(0) && !chars.peek("\""))
        {
            // match escape
            if (chars.peek("\\\\"))
            {
                lexEscape();
            }
            else if (chars.peek("[^\"\n\r\\\\]"))
            {
                chars.match("[^\"\n\r\\\\]");
            }
            else
            {
                throw new UnsupportedOperationException("invalid string");
            }
        }

        // match last double quote
        if (chars.peek("\""))
        {
            chars.match("\"");
            String literal = chars.emit();
            return new Token(Token.Type.STRING, literal);
        }
        else
        {
            throw new UnsupportedOperationException("Unterminated string");
        }
    }

    private void lexEscape()
    {
        // match escape
        if (chars.match("\\\\") || chars.match("\\"))
        {
            if (!chars.match("[bnrt\"'\\\\]"))
            {
                throw new UnsupportedOperationException("invalid escape");
            }
        }
        //throw new UnsupportedOperationException("TODO"); //TODO
    }

    public Token lexOperator()
    {
        // match symbols and .
        if (chars.peek("[<>!=]", "="))
            chars.match("[<>!=]", "=");
        else if (chars.peek("[<>!*/+=\\-]"))
            chars.match("[<>!*/+=\\-]");
        else if (chars.peek(".") && (!chars.peek("\\\\")))
            chars.match(".");
        else
            throw new UnsupportedOperationException("invalid operator");

        String literal = chars.emit();
        return new Token(Token.Type.OPERATOR, literal);
    }

    /**
     * A helper class for maintaining the state of the character stream (input)
     * and methods for building up token literals.
     */
    private static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        /**
         * Returns true if the next characters match their corresponding
         * pattern. Each pattern is a regex matching only ONE character!
         */
        public boolean peek(String... patterns) {
            if (!has(patterns.length - 1)) {
                return false;
            }
            for (int offset = 0; offset < patterns.length; offset++) {
                var character = input.charAt(index + offset);
                if (!String.valueOf(character).matches(patterns[offset])) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Equivalent to peek, but also advances the character stream.
         */
        public boolean match(String... patterns) {
            var peek = peek(patterns);
            if (peek) {
                index += patterns.length;
                length += patterns.length;
            }
            return peek;
        }

        /**
         * Returns the literal built by all characters matched since the last
         * call to emit(); also resetting the length for subsequent tokens.
         */
        public String emit() {
            var literal = input.substring(index - length, index);
            length = 0;
            return literal;
        }
        /**
         * this method consumes the next char in the input -- advances the current position by one char.
         * also increments the current token length
         */
        private void consume() {
            index++; // move current position by one forward
            length ++; // increment the token length by one to include consumed char
        }
        /**
         * resets the length of the current token length to 0 and discards any chars that were previously consumed.
         * and accumulated for the current token.
         */
        private void skip()
        {
            length = 0; // reset token length
        }

    }

}
