package dev.civl.mc.semantics.IF;

import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.statement.Statement;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.state.IF.MemoryUnit;
import dev.civl.mc.state.IF.State;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.SymbolicUniverse;
import dev.civl.sarl.IF.ValidityResult.ResultType;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * This class provides methods dealing with symbolic expressions and states,
 * which represent some common-used operations like obtaining a sub-array from a
 * given array, etc.
 * 
 * @author Manchun Zheng (zmanchun)
 * 
 */
public interface SymbolicAnalyzer {
	/**
	 * @param pointer
	 *            A pointer type symbolic expression.
	 * @return true iff the given pointer is concrete.
	 */
	static public boolean isConcretePointer(SymbolicExpression pointer) {
		return !(!pointer.operator().equals(SymbolicOperator.TUPLE)
				&& !pointer.operator().equals(SymbolicOperator.CONCRETE));
	}
	/**
	 * Given an array, a start index, and end index, returns the array which is
	 * the subsequence of the given array consisting of the elements in
	 * positions start index through end index minus one. The length of the new
	 * array is endIndex - startIndex. TODO move to libcivlc?
	 * 
	 * @param array
	 * @param startIndex
	 * @param endIndex
	 * @param assumption
	 * @param source
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	SymbolicExpression getSubArray(State state, int pid,
			SymbolicExpression array, NumericExpression startIndex,
			NumericExpression endIndex, CIVLSource source)
			throws UnsatisfiablePathConditionException;

	/**
	 * Computes the user-friendly and brief string representation of a state.
	 * Only the call stack is printed.
	 * 
	 * @param state
	 *            The state whose string representation is to be computed.
	 * @return The user-friendly string representation of a state.
	 */
	StringBuffer stateInformation(State state);

	/**
	 * Computes the user-friendly and complete string representation of a state.
	 * Everything including dyscopes, call stacks is printed.
	 * 
	 * @param state
	 * @return
	 */
	StringBuffer stateToString(State state);

	/**
	 * Computes the user-friendly and complete string representation of a state.
	 * Everything including dyscopes, call stacks is printed.
	 * 
	 * @param state
	 * @param lastSavedState
	 *            the last saved state that this state is generated from. -1 if
	 *            the there is no such last saved state.
	 * @param sequenceId
	 *            the place that this state have in the sequence of intermediate
	 *            states after executing the last saved state. -1 iff
	 *            lastSavedState is -1.
	 * @return
	 */
	StringBuffer stateToString(State state, int lastSavedState, int sequenceId);

	/**
	 * <p>
	 * Computes the user-friendly string representation of a symbolic
	 * expression.
	 * </p>
	 * 
	 * <p>
	 * If the given expression is a pointer, then its string representation is
	 * computed according to the object that it refers to:
	 * <ul>
	 * <li>a variable: <code>& variable &lt;dyscope name></code>; <br>
	 * e.g.,
	 * 
	 * <pre>
	 * int a = 9; int * p = &a;
	 * </pre>
	 * 
	 * The representation of <code>p</code> would be <code>&a&lt;d0></code>
	 * assuming that the name of the dynamic scope of <code>a</code> is
	 * <code>d0</code>.</li>
	 * <li>an element of an array: <code>&array&lt;dyscope name>[index]</code>;
	 * <br>
	 * e.g.,
	 * 
	 * <pre>
	 * int a[5]; int *p = &a[1];
	 * </pre>
	 * 
	 * The representation of <code>p</code> would be <code>&a&lt;d0>[1]</code>
	 * assuming that the name of the dynamic scope of <code>a</code> is
	 * <code>d0</code>.</li>
	 * <li>a field of a struct: <code>&struct&lt;dyscope name>.field</code>;<br>
	 * e.g.,
	 * 
	 * <pre>
	 * typedef struct {int x; int y;} A; A s; int*p = &s.y;
	 * </pre>
	 * 
	 * The representation of p would be <code>&a&lt;d0>.y</code> assuming that
	 * the name of the dynamic scope of <code>a</code> is <code>d0</code>.</li>
	 * 
	 * <li>a heap cell:
	 * <code>heapObject&lt;dyscope name, malloc ID, number of malloc call></code>
	 * .</li>
	 * </ul>
	 * </p>
	 * 
	 * @param source
	 *            The source code information related to the symbolic expression
	 *            for error report if any.
	 * @param state
	 *            The state that the symbolic expression belongs to.
	 * @param symbolicExpression
	 *            The symbolic expression whose string representation is to be
	 *            computed.
	 * @return The user-friendly string representation of a state.
	 */
	String symbolicExpressionToString(CIVLSource source, State state,
			CIVLType type, SymbolicExpression symbolicExpression);

	/**
	 * Computes the CIVL type of the object referring to by the given pointer.
	 * 
	 * @param soruce
	 *            The source code information related to the symbolic expression
	 *            for error report if any.
	 * @param state
	 *            The state that the given pointer belongs to.
	 * @param pointer
	 *            The pointer the type of whose object is to be computed.
	 * @return The CIVL type of the object referring to by the given pointer.
	 */
	CIVLType civlTypeOfObjByPointer(CIVLSource soruce, State state,
			SymbolicExpression pointer);

	/**
	 * Computes the {@link SymbolicType} of the object referring to by the given
	 * pointer.
	 * 
	 * @param source
	 *            The source code information related to the symbolic expression
	 *            for error report if any.
	 * @param state
	 *            The state that the given pointer belongs to.
	 * @param pointer
	 *            The pointer the type of whose object is to be computed.
	 * @return The {@link SymbolicType} of the object referring to by the given
	 *         pointer.
	 */
	SymbolicType dynamicTypeOfObjByPointer(CIVLSource source, State state,
			SymbolicExpression pointer);

	/**
	 * pre-condition:
	 * <ol>
	 * <li>"arrayPtr" must point to an array</li>
	 * <li>"source" is the @{link CIVLSource} of the pointer expression</li>
	 * </ol>
	 * post-condition:
	 * <ol>
	 * <li>the returned {@link CIVLType} must not be an array type</li>
	 * <li>the returned object cannot be null</li>
	 * </ol>
	 * Get the type of the non-array element of an array by given a pointer to
	 * an array
	 * 
	 * @param array
	 * @return the type of the non-array element of an array
	 */
	CIVLType getArrayBaseType(State state, CIVLSource source,
			SymbolicExpression arrayPtr);

	/**
	 * <p>
	 * <b>Spec:</b> Returns a {@link ReferenceExpression} object which directly
	 * refer to the object which has the physical base type of the pointed array
	 * (or object) residing in memory.
	 * </p>
	 * Note: The "physical base type" means the base type of the a physical
	 * sequence of objects in memory space. For example: <code> 
	 * int ** a; 
	 * int b[2][2];
	 * int c;
	 * 
	 * a = (int **)malloc(..);
	 * </code> The "physical base type" of "a" is "int *" while both of "b" and
	 * of "c" is "int".
	 * 
	 * 
	 * @author Ziqing Luo
	 * @param state
	 *            The current state
	 * @param process
	 *            The information of the process
	 * @param source
	 *            The CIVL source of the pointer
	 * @return The Reference to the object that has physical base type
	 * @throws UnsatisfiablePathConditionException
	 */
	ReferenceExpression getLeafNodeReference(State state,
			SymbolicExpression pointer, CIVLSource source);

	/**
	 * Gets the symbolic universe used by this symbolic analyzer.
	 * 
	 * @return the symbolic universe
	 */
	SymbolicUniverse getUniverse();

	/**
	 * Compute a friendly string representation of an expression's evaluation.
	 * Eg, if the expression is a+b, then return 8+9 supposing a=8, b=9.
	 * 
	 * @return
	 * @throws UnsatisfiablePathConditionException
	 */
	Pair<State, String> expressionEvaluation(State state, int pid,
			Expression expression, boolean resultOnly)
			throws UnsatisfiablePathConditionException;

	StringBuffer statementEvaluation(State preState, State postState, int pid,
			Statement statement) throws UnsatisfiablePathConditionException;

	StringBuffer inputVariablesToStringBuffer(State state);

	Evaluator evaluator();

	SymbolicExpression pointerArithmetics(CIVLSource source, State state,
			boolean isSubtract, SymbolicExpression pointer,
			SymbolicExpression offset);

	/**
	 * Is this an defined pointer? A pointer is defined if one of the following
	 * holds:
	 * <ul>
	 * <li>it can be dereferenced (derefable pointer), e.g., <code>&a</code>,
	 * <code>&b[0]</code> where <code>a</code> is a scalar variable and
	 * <code>b</code> is an array of length 5.</li>
	 * <li>it is the NULL pointer.</li>
	 * <li>it points to the end of an array, e.g., <code>&b[5]</code> where
	 * <code>b</code> is an array of length 5.</li>
	 * </ul>
	 * For the latter two cases, the pointer is called underefable pointer.
	 * 
	 * @param state
	 *            The current state
	 * @param pointer
	 *            The pointer.
	 * @param civlSource
	 *            The source related with the pointer
	 * @return True iff the given pointer is defined.
	 * @throws CIVLUnimplementedFeatureException
	 *             If the given pointer is a non-concrete one.
	 */
	Pair<BooleanExpression, ResultType> isDefinedPointer(State state,
			SymbolicExpression pointer, CIVLSource civlSource);

	/**
	 * Is this a derefable pointer? In other words, check if the pointer can be
	 * dereferenced safely. Examples of derefable pointers: <code>&a</code>,
	 * <code>&b[0]</code> where <code>a</code> is a scalar variable and
	 * <code>b</code> is an array of length 5. Examples of underefable pointers:
	 * NULL, <code>&b[5]</code> where <code>b</code> is an array of length 5.
	 * 
	 * @param state
	 * @param pointer
	 * @param source
	 *            The source related with the pointer, used to annotate any
	 *            theorem-prover queries generated while checking; may be
	 *            <code>null</code>.
	 * @return
	 */
	Pair<BooleanExpression, ResultType> isDerefablePointer(State state,
			SymbolicExpression pointer, CIVLSource source);

	/**
	 * Pretty representation of a path condition, which is broken into lines if
	 * it is in CNF.
	 * 
	 * @param state
	 * @param pc
	 * @return
	 */
	StringBuffer pathconditionToString(CIVLSource source, State state,
			String prefix, BooleanExpression pc);

	/**
	 * Pretty representation of a memory unit.
	 * 
	 * @param state
	 * @param mu
	 * @return
	 */
	StringBuffer memoryUnitToString(State state, MemoryUnit mu);

	/**
	 * <p>
	 * Check if the dynamic types of the left-hand side (lhs) and right-hand
	 * side (rhs) expression are compatible for assignment operation.
	 * </p>
	 * 
	 * <p>
	 * If the type of lhs or rhs is a numeric/boolean/char/uninterpreted type,
	 * their dynamic types must be exactly the same.
	 * </p>
	 * 
	 * <p>
	 * If the type of lhs or rhs is non-scalar type, the following rules will be
	 * recursively applied to check their compatibility:
	 * <ul>
	 * <li>IF lhs has a complete array-of-T0 type "t0", rhs must have a complete
	 * array-of-T1 type "t1". T0 and T1 must be compatible for assignment. The
	 * extent of "t0" must equal to the extent of "t1".</li>
	 * <li>IF lhs has an incomplete array-of-T type, rhs must have array-of-T
	 * type.</li>
	 * <li>IF lhs has a tuple type, rhs must have a tuple type as well. The
	 * tuple types of lhs and rhs must have same amount of component types. Each
	 * pair of component types in the tuple types of the lhs and rhs must be
	 * compatiable</li>
	 * <li>IF lhs has a union type, rhs must have a union type as well. The
	 * union types of lhs and rhs must have same amount of component types. Each
	 * pair of component types in the union types of the lhs and rhs must be
	 * compatiable</li>
	 * </ul>
	 * </p>
	 * 
	 * @param lhsType
	 *            The dynamic type of the left-hand side expression
	 * @param rhsType
	 *            The dynamic type of the right-hand side expression
	 * @return true iff the given two dynamic types are compatible for an
	 *         assignment operation
	 */
	boolean areDynamicTypesCompatiableForAssign(SymbolicType lhsType,
			SymbolicType rhsType);
}
