package dev.civl.abc.ast.type.common;

import java.util.HashMap;
import java.util.Map;

import dev.civl.abc.ast.type.IF.AtomicType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.Type;

public abstract class CommonType implements Type {

	private TypeKind kind;

	private int id = -1;

	public CommonType(TypeKind kind) {
		this.kind = kind;
	}

	@Override
	public TypeKind kind() {
		return kind;
	}

	@Override
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	/**
	 * A "variably modified" (VM) type is a declarator type which in the nested
	 * sequence of declarators has a VLA type, or any type derived from a VM
	 * type. I.e.: a VLA is a VM; a pointer to a VM is a VM; a function
	 * returning a VM is a VM; an array with a VM element type is a VM.
	 * 
	 * Implement this in all concrete subclasses.
	 */
	@Override
	public abstract boolean isVariablyModified();

	protected final int objectCode() {
		return super.hashCode();
	}

	@Override
	public String toString() {
		return "Type[kind=" + kind + "]";
	}

	/**
	 * Method used to implement the equivalentTo and compatibleWith methods.
	 * Cycles are possible in the type relations: think, linked list. To prevent
	 * infinite recursion, it is necessary to keep track of the types seen so
	 * far in an equivalence or compatibility check.
	 * 
	 * @param other
	 *            the other type this one is being compared with
	 * @param equivalent
	 *            should we check for equivalence, or just compatibility?
	 * @param seen
	 *            map from types seen so far to their similar counterparts
	 * @return
	 */
	protected abstract boolean similar(Type other, boolean equivalent,
			Map<TypeKey, Type> seen);

	@Override
	public boolean equivalentTo(Type other) {
		if (this == other)
			return true;
		return similar(other, true, new HashMap<TypeKey, Type>());
	}

	@Override
	public boolean compatibleWith(Type other) {
		if (this == other)
			return true;
		return similar(other, false, new HashMap<TypeKey, Type>());
	}

	@Override
	public Type ignoreQualifiersAtomic() {
		Type type = this;
		if (type.kind() == TypeKind.QUALIFIED)
			type = ((QualifiedObjectType) type)
					.getBaseType();
		if (type.kind() == TypeKind.ATOMIC)
			type = ((AtomicType) type).getBaseType();
		return type;
	}
}
