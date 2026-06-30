package dev.civl.abc.ast.type.IF;

import java.io.PrintStream;

/**
 * <p>
 * An instance of Type represents a C type. A type is a conceptual entity, not a
 * syntactic element. This class is the root of the type hierarchy for all types
 * used to represent C types.
 * </p>
 * 
 * <p>
 * There may also be additional types that are not part of C, but are part of
 * some extension to C (such as CIVL-C).
 * </p>
 * 
 * <p>
 * Summary of type hierarchy:
 * </p>
 * 
 * {@link Type}
 * <ul>
 * <li>{@link FunctionType}
 * <li>{@link ObjectType}
 * <ul>
 * <li>{@link UnqualifiedObjectType}
 * <ul>
 * <li>{@link ArithmeticType}</li>
 * <li>{@link ArrayType}</li>
 * <li>{@link AtomicType}</li>
 * <li>{@link StandardBasicType}</li>
 * <li>etc.</li>
 * </ul>
 * </li>
 * <li>{@link QualifiedObjectType}
 * <ul>
 * <li>uses one or more of <code>const</code>, <code>volatile</code>,
 * <code>restrict</code>, etc.</li>
 * </ul>
 * </li>
 * </ul>
 * </ul>
 * 
 * <p>
 * Note: {@link AtomicType} and {@link QualifiedObjectType} constructors take an
 * {@link UnqualifiedObjectType}.
 * </p>
 * 
 * 
 * <p>
 * Equivalence relations: there are 3 equivalence relations on the set of types.
 * From strongest to weakest, they are: equality, equivalence, and
 * compatibility. If T1 equals (according to the equals method) T2, then T1 is
 * also equivalent to T2. If T1 is equivalent to T2 then T1 is compatible with
 * T2.
 * <p>
 * 
 * <p>
 * The differences mainly involve tagged entities (structs, unions, enums). In
 * C, it is possible for two structs (for example) to have the same tag, fields,
 * and field types, but still represent two distinct types. This happens if they
 * are declared in two different scopes. Hence it is necessary to specify some
 * other "hidden" component of a struct type to determine when two are equal. We
 * call this the "key": this is any Object, whose "equals" method is used to
 * determine when two tagged types are equal. For any key, there can be at most
 * one struct type using that key. Hence two structs are equal if and only if
 * their keys are equal. Since there can be only one struct object with that
 * key, it follows that they are ==. This invariant is enforced by the type
 * factory.
 * </p>
 * 
 * <p>
 * Two structs which are not equal may still be "equivalent". This means they
 * have equal tags (equal as strings), and, they are either both complete or
 * both incomplete. If they are complete, they must have equivalent fields with
 * the same names and in the same order.
 * </p>
 * 
 * <p>
 * Two structs which are not equal may nevertheless be "compatible". An
 * incomplete struct with tag T is compatible with any struct with tag T. An
 * incomplete array type is compatible with a complete array type which has a
 * compatible element type.
 * </p>
 * 
 * @author siegel
 * 
 */
public interface Type {

	/**
	 * The different kinds of types.
	 */
	public static enum TypeKind {
		/**
		 * An array type; an instance of {@link ArrayType}
		 */
		ARRAY,
		/**
		 * An atomic type; an instance of {@link AtomicType}
		 */
		ATOMIC,
		/**
		 * A "standard basic type"; an instance of {@link StandardBasicType}
		 */
		BASIC,
		/**
		 * The CIVL-C domain type, <code>$domain</code> or
		 * <code>$domain(n)</code>.
		 */
		DOMAIN,
		/**
		 * An enumeration type; an instance of {@link EnumerationType}
		 */
		ENUMERATION,
		/**
		 * A function type; an instance of {@link FunctionType}
		 */
		FUNCTION,
		/**
		 * The CIVL-C heap type, represented by <code>$heap</code>
		 */
		HEAP,
		/**
		 * The CIVL-C $lambda type, represented by <code>$lambda_t</code>
		 */
		LAMBDA,
		/**
		 * The CIVL-C memory type, represented by <code>$memory</code>
		 */
		MEM,
		/**
		 * An integer type which is not a standard basic type. The C Standard
		 * allows a C implementation to provide additional integer types beyond
		 * those specified in the Standard.
		 */
		OTHER_INTEGER,
		/**
		 * A pointer type; an instance of {@link PointerType}
		 */
		POINTER,
		/**
		 * The CIVL-C process type, represented by <code>$proc</code>
		 */
		PROCESS,
		/**
		 * A qualified object type; an instance of {@link QualifiedObjectType}
		 */
		QUALIFIED,
		/**
		 * The CIVL-C range type, denoted <code>$range</code>
		 */
		RANGE,
		/**
		 * The CIVL-C set type. Expressions may have set type but one cannot declare a variable or a function has (or returns) a set type. 
		 */
		SET,
		/**
		 * The CIVL-C scope type, represented by <code>$scope</code>
		 */
		SCOPE,
		/**
		 * The CIVL-C state type, represented by <code>$state</code>
		 */
		STATE,
		/**
		 * A structure or union type; an instance of
		 * {@link StructureOrUnionType}
		 */
		STRUCTURE_OR_UNION,
		/**
		 * The <code>void</code> type, used to represent no type in places where
		 * a type is syntactically required.
		 */
		VOID
	};

	/**
	 * <p>
	 * Is this type "compatible" with the given type? See C11 Sec. 6.2.7 for the
	 * definition of "compatible".
	 * </p>
	 * 
	 * <p>
	 * Special handling for tags that begin with "$anon"--all of these are
	 * treated as identical. These are the names given to anonymous entities by
	 * ABC for convenience.
	 * </p>
	 * 
	 * @param type
	 *            the type to compare with this one for compatibility
	 * @return true iff the two types are compatible
	 */
	boolean compatibleWith(Type type);

	/**
	 * <p>
	 * Is this type "equivalent to" the given type. Two equivalent types should
	 * be interchangeable in any situation. Note that an incomplete struct type
	 * (for example) will never be equivalent to a complete struct type, though
	 * they may be compatible.
	 * </p>
	 * 
	 * <p>
	 * Special handling for tags that begin with "$anon"--all of these are
	 * treated as identical. These are the names given to anonymous entities by
	 * ABC for convenience.
	 * </p>
	 * 
	 * @param type
	 *            any type
	 * @return true iff the two types are equivalent.
	 */
	boolean equivalentTo(Type type);

	/**
	 * The types created by the type factories are given unique id numbers. This
	 * method returns the id number of this type.
	 * 
	 * @return the id number of this type
	 */
	int getId();

	/**
	 * C11 6.2.4(21):
	 * 
	 * "Arithmetic types and pointer types are collectively called scalar types."
	 * 
	 * @return true iff type is scalar
	 */
	boolean isScalar();

	/**
	 * Is this type a "VM" type (variable modified type)? This is defined in the
	 * C11 Standard Sec. 6.7.6:
	 * 
	 * <blockquote> If, in the nested sequence of declarators in a full
	 * declarator, there is a declarator specifying a variable length array
	 * type, the type specified by the full declarator is said to be variably
	 * modified. Furthermore, any type derived by declarator type derivation
	 * from a variably modified type is itself variably modified. </blockquote>
	 * 
	 * The definition of "variable length array type" is given in Sec. 6.7.6.2:
	 * 
	 * <blockquote> If the size is not present, the array type is an incomplete
	 * type. If the size is * instead of being an expression, the array type is
	 * a variable length array type of unspecified size, which can only be used
	 * in declarations or type names with function prototype scope;143) such
	 * arrays are nonetheless complete types. If the size is an integer constant
	 * expression and the element type has a known constant size, the array type
	 * is not a variable length array type; otherwise, the array type is a
	 * variable length array type. (Variable length arrays are a conditional
	 * feature that implementations need not support; see
	 * 6.10.8.3.) </blockquote>
	 * 
	 * @return true iff this type is a VM type
	 */
	boolean isVariablyModified();

	/**
	 * The kind of type this is. See definition of the enumerated type
	 * {@link TypeKind}. These kinds partition the set of all types.
	 * 
	 * @return the kind of this type
	 */
	TypeKind kind();

	/**
	 * Prints the type in a tree-formatted style. The prefix string is prepended
	 * to each line of output other than the first. Output for structure or
	 * union types may leave out the fields by setting abbrv to true.
	 * 
	 * @param prefix
	 *            string to preprend to lines after first
	 * @param out
	 *            PrintStream to which output should be sent
	 * @param abbrv
	 *            if true, abbreviate representations of structure or union
	 *            types by leaving out their fields
	 */
	void print(String prefix, PrintStream out, boolean abbrv);

	/**
	 * 
	 * @return the {@link Type} after ignoring qualifiers, including
	 *         {@link AtomicType}
	 */
	Type ignoreQualifiersAtomic();
}
