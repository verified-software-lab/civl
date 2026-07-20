/**
 * 
 */
package dev.civl.mc.model.IF.expression;

import java.util.Set;

import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.Sourceable;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * The parent of all expressions.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * 
 */
public interface Expression extends Sourceable {

	public enum ExpressionKind {
		/**
		 * A call to an abstract (mathematical) expression; instance of
		 * {@link AbstractFunctionCallExpression}.
		 */
		ABSTRACT_FUNCTION_CALL,
		/**
		 * A C address-of expression <code>&expr</code>, instance of
		 * {@link AddressOfExpression}.
		 */
		ADDRESS_OF,
		/**
		 * A C binary operator expression, e.g., <code>a+b</code>,
		 * <code>e/(d-5*c)</code>, instance of {@link BinaryExpression}.
		 */
		BINARY, BOOLEAN_LITERAL, BOUND_VARIABLE, CAST, CHAR_LITERAL,
		/**
		 * Conditional expression c ? a : b, instance of {@link ConditionalExpression}
		 */
		COND, DEREFERENCE, DOMAIN_GUARD, DOT, DYNAMIC_TYPE_OF, FUNCTION_IDENTIFIER, FUNCTION_GUARD, INITIAL_VALUE,
		INTEGER_LITERAL, MEMORY_UNIT, NULL_LITERAL, QUANTIFIER, REAL_LITERAL, REGULAR_RANGE, RESULT, SCOPEOF, SELF,
		SIZEOF_TYPE, SIZEOF_EXPRESSION, STRING_LITERAL, COMPOUND_LITERAL, SUBSCRIPT, SYSTEM_GUARD, UNARY,
		UNDEFINED_PROC, VARIABLE, HERE_OR_ROOT, PROC_NULL,
		/**
		 * instance of {@link FunctionCallExpression}
		 */
		FUNC_CALL, REC_DOMAIN_LITERAL, WILDCARD, NOTHING,
		/**
		 * an array lambda expression, instance of {@link ArrayLambdaExpression}
		 */
		ARRAY_LAMBDA, LAMBDA, EXTENDED_QUANTIFIER
	}

	/**
	 * @return The highest scope accessed by this expression. Null if no variables
	 *         accessed.
	 */
	Scope expressionScope();

	Scope lowestScope();

	/**
	 * 
	 * @return The type of this expression. For a primitive or variable, this is the
	 *         type of the primitive or variable. For a cast expression it is the
	 *         cast type. For operations it is the type of the operation result.
	 */
	CIVLType getExpressionType();

	/**
	 * Returns the kind of this expression
	 * 
	 * @return The expression kind
	 */
	ExpressionKind expressionKind();

	/**
	 * Calculate the existence of dereferences in this expression
	 */
	void calculateDerefs();

	/**
	 * return true iff the expression has at least one dereferences of a certain
	 * pointer variable
	 * 
	 * @return True of False
	 */
	boolean hasDerefs();

	/**
	 * Analyzes if variables accessed by this expression are purely local
	 * 
	 * @param funcScope The function scope of this expression
	 */
	void purelyLocalAnalysisOfVariables(Scope funcScope);

	/**
	 * @return True iff the expression accessed only purely-local variables
	 */
	boolean isPurelyLocal();

	/**
	 * Analyzes if this expression is purely local
	 */
	void purelyLocalAnalysis();

	/**
	 * Replace a certain conditional expression with a variable expression. Used
	 * when translating away conditional expressions with temporal variable
	 * 
	 * @param oldExpression The conditional expression
	 * @param newExpression The variable expression of the temporal variable for the
	 *                      conditional expression
	 */
	void replaceWith(ConditionalExpression oldExpression, VariableExpression newExpression);

	/**
	 * Attempt to create a expression by replacing a certain conditional expression
	 * with a new expression, used when translating away conditional expressions
	 * without introduction temporal variable
	 * 
	 * @param oldExpression The conditional expression
	 * @param newExpression The new expression
	 * @return Null if nothing is changed, otherwise the new expression
	 */
	Expression replaceWith(ConditionalExpression oldExpression, Expression newExpression);

	/**
	 * Compute the set of variables visible from a certain scope that appear in an
	 * address-of expression. e.g., <code>(&a + &b)</code> returns <code>{a}</code>
	 * if <code>a</code> is in visible from the given scope while <code>b</code>
	 * invisible from the given scope.
	 * 
	 * @param scope The scope to focus on.
	 * @return a nullable set of variables; null equivalents to an empty set
	 */
	Set<Variable> variableAddressedOf(Scope scope);

	/**
	 * Compute the set of variables that appear in an address-of expression. e.g.,
	 * <code>(&a + &b)</code> returns <code>{a, b}</code>.
	 * 
	 * @return a nullable set of variables; null equivalents to an empty set
	 */
	Set<Variable> variableAddressedOf();

	/**
	 * The immutable constant value of this expression. NULL if the expression is
	 * not a constant.
	 * 
	 * @return the constant value of this expression. NULL if the expression is not
	 *         a constant.
	 */
	SymbolicExpression constantValue();

	/**
	 * Checks if this expression has a constant value, i.e., constantValue() !=
	 * NULL.
	 * 
	 * @return true iff this expression has a constant value.
	 */
	boolean hasConstantValue();

	/**
	 * Calculates the constant value of this expression.
	 * 
	 * @param universe The symbolic universe to be used.
	 */
	void calculateConstantValue(SymbolicUniverse universe);

	/**
	 * checks if this expression contains the constant $here.
	 * 
	 * e.g.: s<$here would return true.
	 * 
	 * @return
	 */
	boolean containsHere();

	void setErrorFree(boolean value);

	/**
	 * if this expression is guaranteed to be error-free and thus doesn't need any
	 * error checking. e.g., &anon[0] which is used to translate array literals.
	 * 
	 * @return
	 */
	boolean isErrorFree();

	/**
	 * Computes the set of variables which occur freely in this expression. "Free"
	 * means not bound, i.e., quantified variables are not included.
	 * 
	 * @return the set of free variables occurring in the expression
	 */
	Set<Variable> freeVariables();
}
