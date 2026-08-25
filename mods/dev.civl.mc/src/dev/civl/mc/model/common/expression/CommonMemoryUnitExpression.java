package dev.civl.mc.model.common.expression;

import java.util.Set;

import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.expression.Expression;
import dev.civl.mc.model.IF.expression.MemoryUnitExpression;
import dev.civl.mc.model.IF.expression.reference.MemoryUnitReference;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;

/**
 * This represents a memory unit of (part of) a variable in a certain (static)
 * scope.
 * 
 * @author Manchun Zheng (zmanchun)
 *
 */
public class CommonMemoryUnitExpression extends CommonExpression
		implements
			MemoryUnitExpression {

	private Variable variable;

	/**
	 * The type of the variable referred to by (scopeID, varID).
	 */
	private CIVLType objectType;

	/**
	 * The reference to the variable of this memory unit.
	 */
	private MemoryUnitReference reference;

	private boolean writable;

	/**
	 * shall this memory unit be dereferenced?
	 */
	private boolean deref = false;

	/**
	 * Creates a memory unit expression.
	 * 
	 * @param source
	 *            The source code information.
	 * @param scopeId
	 *            The static scope ID of the memory unit.
	 * @param varId
	 *            The variable ID that this memory unit refers to.
	 * @param ref
	 *            The reference that this memory unit holds for the variable.
	 */
	public CommonMemoryUnitExpression(CIVLSource source, Variable variable,
			CIVLType objType, MemoryUnitReference ref, boolean writable,
			boolean deref) {
		super(source, null, null, null);
		this.variable = variable;
		this.objectType = objType;
		this.reference = ref;
		this.writable = writable;
		this.deref = deref;
	}

	@Override
	public int scopeId() {
		return this.variable.scope().id();
	}

	@Override
	public int variableId() {
		return this.variable.vid();
	}

	@Override
	public MemoryUnitReference reference() {
		return this.reference;
	}

	@Override
	public ExpressionKind expressionKind() {
		return ExpressionKind.MEMORY_UNIT;
	}

	@Override
	public Set<Variable> variableAddressedOf(Scope scope) {
		return null;
	}

	@Override
	public Set<Variable> variableAddressedOf() {
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj instanceof MemoryUnitExpression) {
			MemoryUnitExpression that = (MemoryUnitExpression) obj;

			if (variable.equals(that.variable())
					&& reference.equals(that.reference()))
				return true;
		}
		return false;
	}

	@Override
	protected boolean expressionEquals(Expression expression) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public CIVLType objectType() {
		return this.objectType;
	}

	@Override
	public boolean writable() {
		return this.writable;
	}

	@Override
	public boolean deref() {
		return this.deref;
	}

	@Override
	public String toString() {
		String result;
		Scope scope = variable.scope();

		result = "(s" + scope.id() + ", "
				+ scope.getVariable(variable.vid()).name().name() + ", "
				+ reference + ")";
		if (this.writable)
			result = result + "[w]";
		return result;
	}

	@Override
	public Variable variable() {
		return this.variable;
	}

	@Override
	protected void addFreeVariables(Set<Variable> result) {
		assert !variable.isBound();
		result.add(variable);
	}
}
