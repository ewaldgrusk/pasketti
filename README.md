# Pasketti

Composable regular expressions for Java. This library implements some of the
ideas presented in the paper [A Play on Regular Expressions][1] by Sebastian
Fischer, Frank Huch, and Thomas Wilke.

[1]: https://sebfisch.github.io/haskell-regexp/regexp-play.pdf

## Usage

### Regex Combinators

  * `Regex.epsilon()` returns a regular expression that matches the empty
    sequence.

  * `Regex.symbol(predicate)` returns a regular expression that matches any
    symbol satisfying `predicate`. For example, the regular expression
    `Regex.symbol(c -> c == 'A')` matches the capital letter *A*.

  * `Regex.choice(regex1, regex2, ...)` returns a regular expression that
    matches any sequence matched by either of the provided regular expressions.
    This operation is also known as alternation. It is usually denoted by a
    vertical bar, i.e. `<regex1>|<regex2>|...`.

  * `Regex.concat(regex1, regex2, ...)` returns the concatenation of the
    provided regular expressions. This operation is usually expressed as simple
    juxtaposition, i.e. `<regex1><regex2>...`.

  * `Regex.optional(regex)` returns a regular expression that matches what zero
    or *one* consecutive occurrences of `regex` would match. This operation is
    usually denoted by a question mark, i.e. `<regex>?`.

  * `Regex.repetition(regex)` returns a regular expression that matches what
    zero or *more* consecutive occurrences of `regex` would match. This
    operation is also known as Kleene closure. It is usually denoted by an
    asterisk, i.e. `<regex>*`.

### A Simple Example

The regular expression `identifier` matches any legal Java identifier.

```java
Regex<Character>
  identifierStart = Regex.symbol(Character::isJavaIdentifierStart),
  identifierPart  = Regex.symbol(Character::isJavaIdentifierPart),
  identifier      = Regex.concat(
      identifierStart, Regex.repetition(identifierPart)
  );
```

## License

Copyright (c) 2018 Nora Gruner

Licensed under the [MIT License](LICENSE).
