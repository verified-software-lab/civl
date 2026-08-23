/**
 * 
 */
package dev.civl.mc.model.common.variable;

import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.Identifier;
import dev.civl.mc.model.IF.Scope;
import dev.civl.mc.model.IF.type.CIVLType;
import dev.civl.mc.model.IF.variable.Variable;
import dev.civl.mc.model.common.CommonSourceable;
import dev.civl.sarl.IF.expr.SymbolicExpression;

/**
 * A variable.
 * 
 * @author Timothy K. Zirkel (zirkel)
 * @author Timothy J. McClory (tmcclory)
 * 
 */
public class CommonVariable extends CommonSourceable implements Variable {

	private CIVLType type;
	private Identifier name;
	private boolean isConst;
	private boolean isInput;
	private boolean isOutput;
	private boolean isBound;
	private int vid;
	private Scope scope;
	private int hashCode;
	private boolean purelyLocal = true;
	private boolean isStatic = false;
	private boolean hasPointerRef = false;
	private SymbolicExpression constantValue = null;
	private boolean isParameter = false;

	/**
	 * A variable.
	 * 
	 * @param type The type of the variable.
	 * @param name The name of this variable.
	 * @param vid  The index of this variable in its scope.
	 */
	public CommonVariable(CIVLSource source, CIVLType type, Identifier name, int vid, boolean isParameter) {
		super(source);
		this.type = type;
		this.name = name;
		this.vid = vid;
		this.isParameter = isParameter;
		computeHashCode();
	}

	/**
	 * @return The index of this variable in its scope.
	 */
	public int vid() {
		return vid;
	}

	/**
	 * @return The type of this variable.
	 */
	public CIVLType type() {
		return type;
	}

	/**
	 * @return Whether this variable is a const.
	 */
	public boolean isConst() {
		return isConst;
	}

	/**
	 * @return Whether this variable is an input.
	 */
	public boolean isInput() {
		return isInput;
	}

	/**
	 * @return Whether this variable is an output.
	 */
	public boolean isOutput() {
		return isOutput;
	}

	/**
	 * @param type The type of this variable.
	 */
	public void setType(CIVLType type) {
		this.type = type;
		computeHashCode();
	}

	/**
	 * @param isConst Whether this variable is a const.
	 */
	public void setConst(boolean isConst) {
		this.isConst = isConst;
		computeHashCode();
	}

	/**
	 * @param value Whether this variable is an input.
	 */
	public void setIsInput(boolean value) {
		this.isInput = value;
	}

	/**
	 * @param value Whether this variable is an output.
	 */
	public void setIsOutput(boolean value) {
		this.isOutput = value;
	}

	/**
	 * @return The name of this variable.
	 */
	public Identifier name() {
		return name;
	}

	/**
	 * @param name The name of this variable.
	 */
	public void setName(Identifier name) {
		this.name = name;
	}

	/**
	 * @param scope The scope to which this variable belongs.
	 */
	public void setScope(Scope scope) {
		this.scope = scope;
	}

	/**
	 * @return The scope of this variable.
	 */
	public Scope scope() {
		return scope;
	}

	@Override
	public String toString() {
		String result = "";

		if (isConst) {
			result += "const ";
		}
		if (isInput) {
			result += "$input ";
		}
		if (isOutput) {
			result += "$output ";
		}
		result += name;
		if (this.constantValue != null) {
			result += " (=" + this.constantValue + ")";
		}
		result += " : " + type;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	private void computeHashCode() {
		final int prime = 31;
		int result = 1;
		// result = prime * result + ((extent == null) ? 0 : extent.hashCode());
		result = prime * result + (isConst ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		hashCode = prime * result + ((type == null) ? 0 : type.hashCode());
	}

	@Override
	public boolean purelyLocal() {
		return this.purelyLocal;
	}

	@Override
	public void setPurelyLocal(boolean pl) {
		// a constant or an input variable is always considered as purely local
		if (this.isConst() || this.isInput())
			this.purelyLocal = true;
		else {
			if (this.type.isPointerType())// || this.type.isHandleType())
				this.purelyLocal = false;
			else
				this.purelyLocal = pl;
		}
	}

	@Override
	public boolean isBound() {
		return isBound;
	}

	@Override
	public void setIsBound(boolean value) {
		this.isBound = value;
	}

	@Override
	public void setVid(int vid) {
		this.vid = vid;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Variable) {
			Variable other = (Variable) obj;

			if (scope.id() == other.scope().id() && vid == other.vid())
				return true;
		}
		return false;
	}

	@Override
	public boolean isStatic() {
		return this.isStatic;
	}

	@Override
	public void setStatic(boolean value) {
		this.isStatic = value;
	}

	@Override
	public boolean hasPointerRef() {
		return // !this.type.isHandleType() &&
		this.hasPointerRef;
	}

	@Override
	public void setPointerRef(boolean value) {
		this.hasPointerRef = value;
	}

	@Override
	public SymbolicExpression constantValue() {
		return this.constantValue;
	}

	@Override
	public boolean isParameter() {
		return this.isParameter;
	}

	@Override
	public void setConstantValue(SymbolicExpression value) {
		this.constantValue = value;
	}
}
