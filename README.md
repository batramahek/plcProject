# PLC: Programming Language Compiler

This project implements a simplified compiler for a custom programming language, supporting the full pipeline from lexing to code generation. It includes a lexer, parser, semantic analyzer, evaluator, and generator.

## 🚀 Project Structure

```
src/
├── lexer/         # Tokenizer for input strings
├── parser/        # Recursive descent parser
├── analyzer/      # Type checking and semantic analysis
├── evaluator/     # AST evaluation for interpreting code
├── generator/     # Code generation from AST
└── main/          # Entry point and integration
```

## 🧠 Components

- **Lexer**: Converts raw source code into a list of tokens.
- **Parser**: Builds an Abstract Syntax Tree (AST) from tokens using recursive descent.
- **Analyzer**: Performs semantic analysis, including type checking and variable resolution.
- **Evaluator**: Interprets the AST and executes the program directly.
- **Generator**: Converts AST into valid Java source code.

## ✅ Features

- Strong error handling and clear exceptions (e.g., `ParseException`)
- Support for literals, conditionals, loops, and function definitions

## 🧪 Testing

Unit tests should be placed under `src/test/` and can be run using:


*This project was built for an academic course.*
