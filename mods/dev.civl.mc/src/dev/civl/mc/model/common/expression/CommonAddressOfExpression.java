package dev.civl.mc.model.common.expression;

import java.util.HashSet;
import java.util.Set;

import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.expression.AddressOfExpression;
import dev.civl.mc.model.IF.expression.ConditionalExpression;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.LHSExpression;
import dev.civl.mc.model.IF.expression.VariableExpression;
import dev.civl.mc.model.IF.type.CIVLPointerType;
import dev.civl.mc.model.IF.type.CIVLSetType;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;

public class CommonAddressOfExpression extends CommonExpression implements AddressOfExpression {

	/* ************************** Private Fields *************************** */

	/**
	 * The operand of the address-off operator (<code> & </code>).
	 */
	private LHSExpression operand;

	/**
	 * Is this expression evaluating the offset of a field of some struct, which has
	 * the form <code>&(((T*)0)->f)</code>?
	 */
	private boolean isOffset = false;

	private CIVLType type4Offset = null;

	private int fieldIndex = -1;

	/* **************************** Constructor **************************** */

	/**
	 * Creates a new instance of AddressOfExpression.
	 * 
	 * @param source  The source code information of the expression.
	 * @param type    The type of the expression, which is always a (set-of) pointer
	 *                type.
	 * @param operand The operand of the address-of operator (<code>&</code>).
	 */
	public CommonAddressOfExpression(CIVLSource source, CIVLType type, LHSExpression operand) {
		super(source, operand.expressionScope(), operand.lowestScope(), type);
		assert type.isPointerType() || (type.isSetType() && ((CIVLSetType) type).elementType().isPointerType());
		this.operand = operand;
	}

	/* ****************** Methods from AddressOfExpression ***************** */

	@Override
	public LHSExpression operand() {
		return operand;
	}

	/* ********************** Methods from Expression ********************** */

	@Override
	public ExpressionKind expressionKind() {
		return ExpressionKind.ADDRESS_OF;
	}

	@Override
	public void calculateDerefs() {
		this.operand.calculateDerefs();
		this.hasDerefs = this.operand.hasDerefs();
	}

	@Override
	public void purelyLocalAnalysisOfVariables(Scope funcScope) {
		this.operand.setPurelyLocal(false);
	}

	@Override
	public void purelyLocalAnalysis() {
		if (this.hasDerefs) {
			this.purelyLocal = false;
			return;
		}
		this.operand.purelyLocalAnalysis();
		this.purelyLocal = this.operand.isPurelyLocal();
	}

	@Override
	public void replaceWith(ConditionalExpression oldExpression, VariableExpression newExpression) {
		if (operand == oldExpression) {
			operand = newExpression;
			return;
		}
		operand.replaceWith(oldExpression, newExpression);
	}

	@Override
	public Expression replaceWith(ConditionalExpression oldExpression, Expression newExpression) {
		Expression newOperand = operand.replaceWith(oldExpression, newExpression);
		CommonAddressOfExpression result = null;

		if (newOperand != null) {
			result = new CommonAddressOfExpression(this.getSource(), (CIVLPointerType) this.expressionType,
					(LHSExpression) newOperand);
		}
		return result;
	}

	@Override
	public Set<Variable> variableAddressedOf(Scope scope) {
		Set<Variable> variableSet = new HashSet<>();
		Variable variableWritten = operand.variableWritten(scope);

		if (variableWritten != null)
			variableSet.add(variableWritten);
		return variableSet;
	}

	@Override
	public Set<Variable> variableAddressedOf() {
		Set<Variable> variableSet = new HashSet<>();
		Variable variableWritten = operand.variableWritten();

		if (variableWritten != null)
			variableSet.add(variableWritten);
		return variableSet;
	}

	@Override
	protected void addFreeVariables(Set<Variable> result) {
		((CommonExpression) operand).addFreeVariables(result);
	}

	/* ************************ Methods from Object ************************ */

	@Override
	public String toString() {
		return "&(" + operand + ")";
	}

	@Override
	protected boolean expressionEquals(Expression expression) {
		AddressOfExpression that = (AddressOfExpression) expression;

		return this.operand.equals(that.operand());
	}

	@Override
	public boolean containsHere() {
		return this.operand.containsHere();
	}

	@Override
	public void setErrorFree(boolean value) {
		super.setErrorFree(value);
		this.operand.setErrorFree(value);
	}

	@Override
	public boolean isFieldOffset() {
		return this.isOffset;
	}

	@Override
	public void setFieldOffset(boolean value) {
		this.isOffset = value;
	}

	@Override
	public void setTypeForOffset(CIVLType type) {
		this.type4Offset = type;
	}

	@Override
	public void setFieldIndex(int index) {
		this.fieldIndex = index;
	}

	@Override
	public CIVLType getTypeForOffset() {
		return this.type4Offset;
	}

	@Override
	public int getFieldIndex() {
		return this.fieldIndex;
	}
}
