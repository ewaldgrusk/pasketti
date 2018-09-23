package pasketti.core;

import java.util.Objects;
import java.util.function.BinaryOperator;
import java.util.function.Predicate;


/**
 * A regular expression.
 *
 * @author Nora Gruner
 * @since 0.1.0
 */
public abstract class Regex<T> {

  private final boolean active;
  private final boolean empty;
  private final boolean matched;

  private Regex(boolean active, boolean empty, boolean matched) {
    this.active  = active;
    this.empty   = empty;
    this.matched = matched;
  }

  public final boolean isActive() {
    return active;
  }

  public final boolean isEmpty() {
    return empty;
  }

  public final boolean hasMatched() {
    return matched;
  }

  public final Regex<T> shift(boolean mark, T value) {
    return (active || mark) ? step(mark, value) : this;
  }

  public abstract Regex<T> step(boolean mark, T value);


  //----------------------------------------------------------------------------
  // Smart constructors --------------------------------------------------------
  //----------------------------------------------------------------------------

  private static final Regex<?> EPSILON = new Epsilon();

  /**
   * Returns a regular expression that matches the empty sequence.
   *
   * @param  <T>  the type of the symbols
   * @return      a regex matching the empty sequence
   */
  @SuppressWarnings("unchecked")
  public static <T> Regex<T> epsilon() {
    return (Regex<T>) EPSILON;
  }

  /**
   * Returns a regular expression that matches any symbol satisfying the
   * provided predicate.
   *
   * <p>For example, the regular expression {@code Regex.symbol(c -> c == 'A')}
   * matches the capital letter <em>A</em>.
   *
   * @param  <T>        the type of the symbols
   * @param  predicate  a predicate (may not be {@code null})
   * @return            a regex matching any symbol satisfying {@code predicate}
   */
  public static <T> Regex<T> symbol(Predicate<T> predicate) {
    Objects.requireNonNull(predicate, "predicate may not be null");

    return new Symbol<T>(predicate);
  }

  /**
   * Returns the result of reducing the specified array of elements with the
   * provided binary operator.
   *
   * <p><em>Note:</em> This helper method does not validate its arguments.
   *
   * @param  <U>       the type of the array elements
   * @param  operator  an associative function for combining two elements
   * @param  array     a non-empty array of elements
   * @return           the result of reducing {@code array}
   */
  private static <U> U reduce(BinaryOperator<U> operator, U[] array) {
    U result = array[0];

    for (int k = 1; k < array.length; k += 1) {
      result = operator.apply(result, array[k]);
    }

    return result;
  }

  /**
   * Checks that the specified array of elements does not contain any null
   * elements.
   *
   * <p><em>Note:</em> This helper method does not check that the array itself
   * is not {@code null}.
   *
   * @param  <U>    the type of the array elements
   * @param  array  an array of elements
   * @return        the {@code array} if it does not contain any null elements
   */
  private static <U> U[] requireNonNullElements(U[] array) {
    for (int k = 0; k < array.length; k += 1) {
      if (array[k] == null) {
        throw new NullPointerException(
            String.format("element at index %d may not be null", k)
        );
      }
    }

    return array;
  }

  /**
   * Returns a regular expression that matches any sequence matched by either of
   * the provided regular expressions.
   *
   * <p>The operation {@code choice} is also known as alternation. It is usually
   * denoted by a vertical bar, i.e. {@code <regex1>|<regex2>|...}.
   *
   * <p>Invoking this method with no arguments returns the same result as
   * calling {@link #epsilon()}.
   *
   * @param  <T>      the type of the symbols
   * @param  regexes  regular expressions (may not be {@code null})
   * @return          the alternation of {@code regexes}
   */
  @SafeVarargs public static <T> Regex<T> choice(Regex<T>... regexes) {
    Objects.requireNonNull(regexes, "regexes may not be null");

    if (regexes.length == 0) {
      return epsilon(); // Early exit.
    }

    return reduce(Choice<T>::new, requireNonNullElements(regexes));
  }

  /**
   * Returns the concatenation of the provided regular expressions.
   *
   * <p>The operation {@code concat} is usually expressed as simple
   * juxtaposition, i.e. {@code <regex1><regex2>...}.
   *
   * <p>Invoking this method with no arguments returns the same result as
   * calling {@link #epsilon()}.
   *
   * @param  <T>      the type of the symbols
   * @param  regexes  regular expressions (may not be {@code null})
   * @return          the concatenation of {@code regexes}
   */
  @SafeVarargs public static <T> Regex<T> concat(Regex<T>... regexes) {
    Objects.requireNonNull(regexes, "regexes may not be null");

    if (regexes.length == 0) {
      return epsilon(); // Early exit.
    }

    return reduce(Concat<T>::new, requireNonNullElements(regexes));
  }

  /**
   * Returns a regular expression that matches what zero or <em>one</em>
   * consecutive occurrences of the provided regular expression would match.
   *
   * <p>The operation {@code optional} is usually denoted by a question mark,
   * i.e. {@code <regex>?}.
   *
   * @param  <T>    the type of the symbols
   * @param  regex  a regular expression (may not be {@code null})
   * @return        the regular expression {@code <regex>?}
   */
  public static <T> Regex<T> optional(Regex<T> regex) {
    Objects.requireNonNull(regex, "regex may not be null");

    return new Choice<T>(regex, epsilon());
  }

  /**
   * Returns a regular expression that matches what zero or <em>more</em>
   * consecutive occurrences of the provided regular expression would match.
   *
   * <p>The operation {@code repetition} is also known as Kleene closure. It is
   * usually denoted by an asterisk, i.e. {@code <regex>*}.
   *
   * @param  <T>    the type of the symbols
   * @param  regex  a regular expression (may not be {@code null})
   * @return        the Kleene closure of {@code regex}
   */
  public static <T> Regex<T> repetition(Regex<T> regex) {
    Objects.requireNonNull(regex, "regex may not be null");

    return new Repetition<T>(regex);
  }


  //----------------------------------------------------------------------------
  // Static nested classes -----------------------------------------------------
  //----------------------------------------------------------------------------

  private static final class Epsilon<T> extends Regex<T> {

    private Epsilon() {
      super(false, true, false);
    }

    @Override public Regex<T> step(boolean mark, T value) {
      return this; // My job here is done.
    }
  }

  private static final class Symbol<T> extends Regex<T> {

    private final Predicate<T> predicate;

    private Symbol(Predicate<T> predicate, boolean active) {
      super(active, false, active);

      this.predicate = predicate;
    }

    private Symbol(Predicate<T> predicate) {
      this(predicate, false);
    }

    @Override public Regex<T> step(boolean mark, T value) {
      return new Symbol<T>(predicate, mark && predicate.test(value));
    }
  }

  private static final class Choice<T> extends Regex<T> {

    private final Regex<T> regex1;
    private final Regex<T> regex2;

    private Choice(Regex<T> regex1, Regex<T> regex2, boolean active) {
      super(
          active,
          regex1.empty || regex2.empty,
          active && (regex1.matched || regex2.matched)
      );

      this.regex1 = regex1;
      this.regex2 = regex2;
    }

    private Choice(Regex<T> regex1, Regex<T> regex2) {
      this(regex1, regex2, false);
    }

    @Override public Regex<T> step(boolean mark, T value) {
      Regex<T>
        r1 = regex1.shift(mark, value),
        r2 = regex2.shift(mark, value);

      return new Choice<T>(r1, r2, r1.active || r2.active);
    }
  }

  private static final class Concat<T> extends Regex<T> {

    private final Regex<T> regex1;
    private final Regex<T> regex2;

    private Concat(Regex<T> regex1, Regex<T> regex2, boolean active) {
      super(
          active,
          regex1.empty && regex2.empty,
          active && (regex2.matched || (regex1.matched && regex2.empty))
      );

      this.regex1 = regex1;
      this.regex2 = regex2;
    }

    private Concat(Regex<T> regex1, Regex<T> regex2) {
      this(regex1, regex2, false);
    }

    @Override public Regex<T> step(boolean mark, T value) {
      Regex<T>
        r1 = regex1.shift(mark, value),
        r2 = regex2.shift(regex1.matched || (mark && regex1.empty), value);

      return new Concat<T>(r1, r2, r1.active || r2.active);
    }
  }

  private static final class Repetition<T> extends Regex<T> {

    private final Regex<T> regex;

    private Repetition(Regex<T> regex, boolean active) {
      super(active, true, active && regex.matched);

      this.regex = regex;
    }

    private Repetition(Regex<T> regex) {
      this(regex, false);
    }

    @Override public Regex<T> step(boolean mark, T value) {
      Regex<T> r = regex.shift(mark || regex.matched, value);

      return new Repetition<T>(r, r.active);
    }
  }
}
