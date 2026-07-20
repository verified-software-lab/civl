package dev.civl.abc.ast.node.IF.expression;

import dev.civl.abc.ast.conversion.IF.Conversion;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.acsl.NothingNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.type.IF.Type;

/**
 * <p>
 * A node representing any kind of C expression. This is the root of a type
 * hierarchy of expression nodes.
 * </p>
 * 
 * <p>
 * This interface extends {@link InitializerNode} because an expression can be
 * used as an initializer for a scalar variable.
 * </p>
 * 
 * <p>
 * This interface extends {@link SizeableNode} because an expression can be used
 * as an argument to the <code>sizeof</code> operator.
 * </p>
 * 
 * <p>
 * This interface extends {@link ForLoopInitializerNode} to indicate that an
 * expression can be used as the first clause of a "<code>for</code>" loop (as
 * can a variable declaration).
 * </p>
 * 
 * <p>
 * There are many different subtypes of this interface, for the different kinds
 * of expressions. An enumerated type {@link ExpressionKind} is provided to make
 * it easy to identify the subtype and, for example, <code>switch</code> on it.
 * </p>
 * 
 * <p>
 * Every expression has an initial type, and then some number (possibly 0) of
 * implicit conversions. Each conversion specifies an old type and a new type.
 * The conversions form a chain: the first conversion (conversion number 0) has
 * as its old type the initial type of the expression. The old type of
 * conversion n (for any n&ge;1) equals the new type of conversion n-1. The new
 * type of the last conversion is the final "converted type" of the expression.
 * </p>
 * 
 * @author siegel
 * 
 */
public interface ExpressionNode extends InitializerNode, SizeableNode, ForLoopInitializerNode {

	/**
	 * An enumerated type used to categorize the different kinds of expression
	 * nodes. Every expression node belongs to exactly one of these categories.
	 * 
	 * @author siegel
	 * 
	 */
	public enum ExpressionKind {
		/**
		 * An <code>_Alignof</code> expression; can be cast to {@link AlignOfNode}.
		 */
		ALIGNOF,
		/**
		 * An arrow (<code>e->f</code>) expression; can be cast to {@link ArrowNode}.
		 */
		ARROW,
		/**
		 * A cast expression, which has the form <code>(typeName)expr</code>; can be
		 * cast to {@link CastNode}.
		 */
		CAST,
		/**
		 * A compound literal node; can be cast to {@link CompoundLiteralNode}.
		 */
		COMPOUND_LITERAL,
		/**
		 * A constant node; can be cast to {@link ConstantNode}. Note there are many
		 * subtypes.
		 */
		CONSTANT,
		/**
		 * A C dot expression, which has the form <code>e.f</code>; can be cast to
		 * {@link DotNode}.
		 */
		DOT,
		/**
		 * A function call; can be cast to {@link FunctionCallNode}.
		 */
		FUNCTION_CALL,
		/**
		 * Generic selection expression; can be cast to {@link GenericSelectionNode}.
		 */
		GENERIC_SELECTION,
		/**
		 * An identifier used as an expression (e.g., a variable name or function name).
		 * Can be cast to {@link IdentifierExpressionNode}.
		 */
		IDENTIFIER_EXPRESSION,
		/**
		 * An array lambda expression. Can be cast to {@link ArrayLambdaNode}.
		 */
		ARRAY_LAMBDA, // TODO clean up ARRAY_LAMBDA using LAMBDA and
						// cast
		/**
		 * A lambda expression. Can be cast to {@link LambdaNode}.
		 */
		LAMBDA,
		/**
		 * An <code>$object_of</code> or <code>$region_of</code> expression, which is
		 * uded in contract. Can be cast to {@link ObjectOrRegionNode}.
		 */
		OBJECT_OR_REGION_OF,
		/**
		 * An operator expression: this includes a large set of expressions formed by
		 * using an operator represented by some C symbol, such as <code>=</code>
		 * (assignment), <code>+</code> (plus), and so on. It does not include any
		 * operators that form left-hand-side (lhs) expressions. Can be cast to
		 * {@link OperatorNode}.
		 */
		OPERATOR,
		/**
		 * A CIVL-C expression formed using a quantifier, such as the universal
		 * quantifier <code>$forall</code> or the existential quantifier
		 * <code>$exists</code>. Can be cast to {@link QuantifiedExpressionNode} .
		 */
		QUANTIFIED_EXPRESSION,
		/**
		 * A CIVL-C "regular range expression", which has the form <code>lo .. hi</code>
		 * or <code>lo .. hi # step</code>; can be cast to {@link RegularRangeNode}.
		 */
		REGULAR_RANGE,
		/**
		 * A CIVL-C remote reference expression, which refers to a variable in a
		 * different process. Can be cast to {@link RemoteExpressionNode}.
		 */
		REMOTE_REFERENCE,
		/**
		 * The CIVL-C <code>$result</code> built-in variable, which refers to the result
		 * returned by a function. Can be cast to {@link ResultNode}.
		 */
		RESULT,
		/**
		 * A CIVL-C <code>$scopeof(...)</code> expression; can be cast to
		 * {@link ScopeOfNode}.
		 */
		SCOPEOF,
		/**
		 * An expression built from the C <code>sizeof</code> operator; can be cast to
		 * {@link SizeofNode}.
		 */
		SIZEOF,
		/**
		 * A CIVL-C <code>$spawn</code> expression; can be cast to {@link SpawnNode}.
		 */
		SPAWN,
		/**
		 * A GNU C statement expression; can be cast to {@link StatementExpressionNode}
		 */
		STATEMENT_EXPRESSION,
		/**
		 * An ACSL-CIVL-C wildcard expression (<code>...</code>); can be cast to
		 * {@link WildcardNode}.
		 */
		WILDCARD,
		/**
		 * An ACSL nothing expression (<code>\nothing</code>); can be cast to
		 * {@link NothingNode}.
		 */
		NOTHING,
		/**
		 * An ACSL extended quantification expression (
		 * <code>\sum, \min, \max, \product, \numof</code>); can be cast to
		 * {@link ExtendedQuantifiedExpressionNode}.
		 */
		EXTENDED_QUANTIFIED
	}

	/**
	 * Adds a conversion to the sequence of conversions for this expression. The
	 * added conversion must satisfy the following, else an IllegalArgumentException
	 * will be thrown: (1) if this is the first conversion (index 0) to be added,
	 * the old type of the conversion must equal the initial type; (2) if this is
	 * not the first conversion (index > 0) to be added, the old type of the
	 * conversion must equal the newType of the previous conversion.
	 * 
	 * @param conversion the conversion to add to the conversion chain for this
	 *                   expression
	 */
	void addConversion(Conversion conversion);

	@Override
	ExpressionNode copy();

	/**
	 * Returns the kind of this expression. Every expression belongs to exactly one
	 * kind.
	 * 
	 * @return the kind of this expression
	 */
	ExpressionKind expressionKind();

	/**
	 * Returns the index-th conversion in the chain of types for this expression.
	 * 
	 * @param index an integer in the range [0,numTypes-1]
	 * @return the index-th conversion associated to this expression
	 */
	Conversion getConversion(int index);

	/**
	 * Returns the final converted type of the expression. This is the type the
	 * expression has after going through all conversions in its conversion sequence
	 * (if any). If there are no conversions, it is the same as the initial type.
	 * Otherwise, it is the newType of the last conversion.
	 * 
	 * @return the final converted type of the expression
	 */
	Type getConvertedType();

	/**
	 * Returns the initial type of the expression. This is the type the expression
	 * has independent of any context in which the expression occurs.
	 * 
	 * @return initial type of expression
	 */
	Type getInitialType();

	/**
	 * Returns the number of conversions in the chain leading from the initial type
	 * of the expression to the final converted type.
	 * 
	 * The type of an expression may go through a number of type conversions before
	 * arriving at its final "converted type". These conversions depend upon the
	 * context in which the expression occurs. The sequence of conversions leading
	 * from the initial type to the final converted type form a chain in which the
	 * "newType" of conversion i equals the "oldType" of conversion i+1 for each i.
	 * The oldType of conversion 0 is the original type of the expression; the
	 * newType of the last conversion is the converted type of the expression.
	 * 
	 * This method returns the total number of conversions in that chain. The method
	 * will return a nonnegative integer. If there are no conversions, this method
	 * returns 0 and the initial and final types are equal.
	 * 
	 * @return the number of type conversions between the original type and the
	 *         converted type
	 */
	int getNumConversions();

	/**
	 * Is this expression a "constant expression" in the sense of the C11 Standard?
	 * 
	 * @return true iff this expression is a constant expression
	 */
	boolean isConstantExpression();

	/** Removes all conversions from this node */
	void removeConversions();

	/**
	 * Sets the initial type of the expression. This must be set before any
	 * conversions are added.
	 * 
	 * @param type the type that will be the initial type of this expression
	 */
	void setInitialType(Type type);

	/**
	 * checks if this expression is an lvalue expression.
	 * 
	 */
	boolean isLvalue();
}
