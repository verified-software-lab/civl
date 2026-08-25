package dev.civl.mc.dynamic.IF;

import java.util.BitSet;
import java.util.Iterator;
import java.util.List;

import dev.civl.mc.model.IF.CIVLInternalException;
import dev.civl.mc.model.IF.CIVLSource;
import dev.civl.mc.model.IF.CIVLTypeFactory;
import dev.civl.mc.state.IF.UnsatisfiablePathConditionException;
import dev.civl.mc.util.IF.Pair;
import dev.civl.sarl.IF.Reasoner;
import dev.civl.sarl.IF.expr.ArrayElementReference;
import dev.civl.sarl.IF.expr.BooleanExpression;
import dev.civl.sarl.IF.expr.NumericExpression;
import dev.civl.sarl.IF.expr.ReferenceExpression;
import dev.civl.sarl.IF.expr.SymbolicConstant;
import dev.civl.sarl.IF.expr.SymbolicExpression;
import dev.civl.sarl.IF.expr.SymbolicExpression.SymbolicOperator;
import dev.civl.sarl.IF.object.IntObject;
import dev.civl.sarl.IF.type.SymbolicCompleteArrayType;
import dev.civl.sarl.IF.type.SymbolicTupleType;
import dev.civl.sarl.IF.type.SymbolicType;

/**
 * A SymbolicUtility provides all the common operations of symbolic expressions.
 * 
 * @author Manchun Zheng, Ziqing Luo
 * 
 */
public interface SymbolicUtility {
	/**
	 * Constructs the string representation of an array of characters, which
	 * contains at least one '\0' character, starting from the given index and
	 * ending at the index of '\0' minus one. If the result is to be printed,
	 * then special characters like '\r', '\t', etc, will be replaced as the
	 * actual string representation "\\r", "\\t", etc.
	 * 
	 * For example, given an character array {'a', 'b', '\t', 'c', 'd', '\n',
	 * '\0'}, if starting index is 1, and is it not for print, then the result
	 * string will be "b\tcd\n"; if it is for print, then the result string is
	 * "b\\tcd\\n".
	 *
	 * Notions for pointer Value: <br>
	 * undefined: default initialized value/freed/ defined:<br>
	 * derefable: can be dereferenced, e.g., a, a+4 (a is int[5]) underefable:
	 * e.g., NULL, a+5 (a is int[5])<br>
	 * 
	 * @param source
	 *            The source code information for error report.
	 * @param charArray
	 *            The character array in the representation of symbolic sequence
	 * @param startIndex
	 *            The index in the character array where the string starts
	 * @param forPrint
	 *            Will the result be printed? If yes, then special characters
	 *            need to be handled specifically, e.g., '\n' becomes "\\n".
	 * @return the string representation from the given character array with
	 */
	StringBuffer charArrayToString(CIVLSource source,
			SymbolicExpression charArray, int startIndex, boolean forPrint);

	/**
	 * Checks if the object that the container pointer points to contains that
	 * of the element pointer.
	 * 
	 * Precondition: both pointers can be safely dereferenced.
	 * 
	 * @param container
	 *            The pointer that is expected to contain the object of the
	 *            element pointer.
	 * @param element
	 *            The element pointer.
	 * @return True iff the object that the first pointer points to contains
	 *         that of the send pointer.
	 */
	BooleanExpression contains(SymbolicExpression container,
			SymbolicExpression element);

	/**
	 * 
	 * Gets a Java concrete int from a symbolic expression
	 * 
	 * @param source
	 *            source code information for error report
	 * @param expression
	 *            a numeric expression expected to hold concrete int value
	 * @return the concrete int
	 */
	int extractInt(CIVLSource source, NumericExpression expression);

	/**
	 * Gets a concrete Java int from the field of a symbolic expression of tuple
	 * type or throws exception.
	 * 
	 * @param source
	 *            source code information for error report
	 * @param tuple
	 *            symbolic expression of tuple type
	 * @param fieldIndex
	 *            index of a field in that tuple
	 * @return the concrete int value of that field
	 * @throws CIVLInternalException
	 *             if a concrete integer value cannot be extracted
	 */
	int extractIntField(CIVLSource source, SymbolicExpression tuple,
			IntObject fieldIndex);

	/**
	 * <p>
	 * Given a pointer to a certain element of some array, returns the index of
	 * the element that the pointer points to.
	 * </p>
	 * 
	 * <p>
	 * Precondition: pointer must point to an element of some array.
	 * </p>
	 * 
	 * @param source
	 *            The source code information for error report.
	 * @param pointer
	 *            The pointer checks.
	 * @return The index of the element that the pointer points to.
	 */
	int getArrayIndex(CIVLSource source, SymbolicExpression pointer);

	/**
	 * Given a pointer value, returns the dynamic scope type component of that
	 * pointer value.
	 * 
	 * @param pointer
	 *            a pointer value
	 * @return dynamic scope type value which is a component of a pointer
	 */
	SymbolicExpression getScopeValue(SymbolicExpression pointer);

	/**
	 * Given a pointer value, returns the symbolic reference component of that
	 * value. The "symRef" refers to a sub-structure of the variable pointed to.
	 * 
	 * @param pointer
	 *            a pointer value
	 * @return the symRef component
	 */
	ReferenceExpression getSymRef(SymbolicExpression pointer);

	/**
	 * Returns the pointer value obtained by replacing the symRef component of
	 * the given pointer value with the given symRef.
	 * 
	 * @param pointer
	 *            a pointer value
	 * @param symRef
	 *            a symbolic reference expression
	 * @return the pointer obtained by modifying the given one by replacing its
	 *         symRef field with the given symRef
	 */
	SymbolicExpression setSymRef(SymbolicExpression pointer,
			ReferenceExpression symRef);

	/**
	 * Given a pointer value, returns the variable ID component of that value.
	 * 
	 * @param source
	 *            the source code information for error report.
	 * @param pointer
	 *            a pointer value
	 * @return the variable ID component of that value
	 */
	int getVariableId(CIVLSource source, SymbolicExpression pointer);

	/**
	 * <p>
	 * Returns the heap memory unit involved by the given pointer.
	 * </p>
	 * 
	 * @param pointer
	 *            A valid pointer that points to some part of a heap.
	 * @return A pointer to a heap object that is involved by the given pointer.
	 */
	SymbolicExpression heapMemUnit(SymbolicExpression pointer);

	/**
	 * Constructs an invalid heap object of a certain type. A heap object
	 * becomes invalid when it gets deallocated.
	 * 
	 * @param heapObjectType
	 *            The type of the heap object.
	 * @return The invalid heap object of the given type.
	 */
	SymbolicConstant invalidHeapObject(SymbolicType heapObjectType);

	/**
	 * Checks if the components (either a complete object or a sub-component of
	 * an object) that the two pointers point to have no intersection.
	 * 
	 * @param pointer1
	 *            The first pointer.
	 * @param pointer2
	 *            The second pointer.
	 * @return True iff there is no intersection between the components that the
	 *         given two pointers point to.
	 */
	boolean isDisjointWith(SymbolicExpression pointer1,
			SymbolicExpression pointer2);

	/**
	 * Checks if a heap is empty, i.e., either it is the SARL null expression or
	 * all heap objects it holds are marked as INVALID (already deallocated).
	 * 
	 * @param heapValue
	 *            The value of the heap to be checked.
	 * @return True iff the heap has null value or is empty.
	 */
	boolean isEmptyHeap(SymbolicExpression heapValue);

	/**
	 * Is the given pointer the result of some malloc/handle create operation?
	 * 
	 * @param source
	 *            The source code information for error report.
	 * @param pointer
	 *            The pointer to be checked.
	 * @return True iff the given pointer is the result of some malloc/handle
	 *         create operation.
	 */
	boolean isMallocPointer(CIVLSource source, SymbolicExpression pointer);

	/**
	 * Checks if a given value is initialized.
	 * 
	 * @param value
	 *            The value to be checked.
	 * @return True iff the value is already initialized.
	 */
	boolean isInitialized(SymbolicExpression value);

	/**
	 * Test if the given value is within the range, which is given by an
	 * inclusive lower bound, an exclusive upper bound and a step
	 * 
	 * @param value
	 *            a numeric value to be tested if it is in range.
	 * @param low
	 *            the inclusive lower bound of the range
	 * @param upper
	 *            the exclusive upper bound of the range
	 * @param step
	 *            the step of the range
	 * @return True iff the given value is within the index-th range of a
	 *         certain domain.
	 */
	BooleanExpression isInRange(NumericExpression value, NumericExpression low,
			NumericExpression upper, NumericExpression step);

	/**
	 * Checks if the given value is within the index-th range of a certain
	 * rectangular domain.
	 * 
	 * @param value
	 *            The value to be tested if it is in range.
	 * @param range
	 *            The range.
	 * @return True iff the given value is within the index-th range of a
	 *         certain domain.
	 */
	BooleanExpression isInRange(SymbolicExpression value,
			SymbolicExpression range);

	/**
	 * Is this heap object invalid?
	 * 
	 * @param heapObject
	 *            The heap object.
	 * @return True iff the given heap object is invalid.
	 */
	boolean isInvalidHeapObject(SymbolicExpression heapObject);

	/**
	 * Checks if a given pointer is a NULL pointer.
	 * 
	 * @param pointer
	 *            The pointer to be checked.
	 * @return True iff the given pointer is NULL.
	 */
	boolean isNullPointer(SymbolicExpression pointer);

	/**
	 * Is the given pointer pointing to a memory space that is part of a heap?
	 * 
	 * @param pointer
	 *            The pointer to be tested.
	 * 
	 * @return True iff the pointer points to a certain part of some heap.
	 */
	boolean isPointerToHeap(SymbolicExpression pointer);

	/**
	 * <p>
	 * Returns true iff the given pointer is a <strong>pointer to a memory
	 * block</strong>, where a memory block is defined as a space in heap which
	 * is allocated by once execution of <code>$malloc</code>.
	 * </p>
	 * 
	 * 
	 * <p>
	 * FYI, given a pointer, {@link #isPointer2MemoryBlock(SymbolicExpression)}
	 * implies {@link #isPointerToHeap(SymbolicExpression)}, but not vice versa.
	 * </p>
	 * 
	 * @param pointer
	 *            A {@link SymbolicExpression} which represents a concrete
	 *            pointer.
	 * @return Returns true iff the given pointer is <strong>pointer to a memory
	 *         block</strong>.
	 */
	boolean isPointer2MemoryBlock(SymbolicExpression pointer);

	/**
	 * 
	 * Takes a pointer to some location inside a memory block <code>m</code>,
	 * returns the <strong>pointer to m</strong>, where a memory block is
	 * defined as a space in heap which is allocated by once execution of
	 * <code>$malloc</code>.
	 * 
	 * @param heapPointer
	 *            A pointer to some location inside a memory block.
	 *            {@link #isPointerToHeap(heapPointer)} must returns true.
	 * @return A pointer to the memory block
	 */
	SymbolicExpression getPointer2MemoryBlock(SymbolicExpression heapPointer);

	/**
	 * <p>
	 * Returns true iff the two pointers are pointing to the same memory block.
	 * A memory block is the space in memory heap that are allocated by once
	 * execution of a <code>$malloc</code>.
	 * </p>
	 * 
	 * @param ptr0
	 *            A concrete pointer which is an operand of pointer subtraction
	 *            operation.
	 * @param ptr1
	 *            A concrete pointer which is another operand of pointer
	 *            subtraction operation.
	 * @return True iff the two pointers are pointing to the same memory block.
	 */
	boolean arePoint2SameMemoryBlock(SymbolicExpression ptr0,
			SymbolicExpression ptr1);

	/**
	 * Checks if the given reference is valid for the symbolic type of a certain
	 * object value.
	 * 
	 * @param ref
	 *            The reference.
	 * @param objectValueType
	 *            The symbolic type of the value of the given object.
	 * @return True iff the given reference expression is applicable for the
	 *         given object value type.
	 */
	boolean isValidRefOf(ReferenceExpression ref, SymbolicType objectValueType);

	/**
	 * Makes a pointer value from the given dynamic scope ID, variable ID, and
	 * symbolic reference value.
	 * 
	 * @param dyscopeId
	 *            ID number of a dynamic scope
	 * @param varId
	 *            ID number of a variable within that scope
	 * @param symRef
	 *            a symbolic reference to a point within the variable
	 * @return a pointer value as specified by the 3 components
	 */
	SymbolicExpression makePointer(int dyscopeId, int varId,
			ReferenceExpression symRef);

	/**
	 * Constructs a new pointer by replacing the reference expression of a given
	 * pointer. For example, given a pointer &a[5] and a reference [0], the
	 * result will be &a[0]. The given pointer can refer to a heap object, in
	 * which case, the heap object reference is considered the old "variable"
	 * and will remain the same.
	 * 
	 * @param oldPointer
	 *            The old pointer whose reference expression will be changed.
	 * @param symRef
	 *            The new reference expression to be used for the new pointer.
	 * @return the new pointer which refers to the same variable/heap object as
	 *         the given pointer, but with a new reference expression.
	 */
	SymbolicExpression makePointer(SymbolicExpression oldPointer,
			ReferenceExpression symRef);

	/**
	 * * Constructs a pointer by combining a pointer to a component, either an
	 * object (a heap atomic object or a normal object) or a sub-component of an
	 * object, and a reference expression w.r.t that component.
	 * 
	 * @param componentPointer
	 *            a pointer to a component
	 * @param reference
	 *            a reference expression
	 * @return A new pointer by combining the component pointer and the given
	 *         reference w.r.t. that component.
	 */
	SymbolicExpression extendPointer(SymbolicExpression componentPointer,
			ReferenceExpression reference);

	/**
	 * Creates a new array of given length, using the given type as its element
	 * type, and each element having the given value.
	 * 
	 * @param context
	 *            The context of the operation, i.e., the path condition of the
	 *            current state.
	 * @param elementValueType
	 *            The type of the array element. Note necessarily the type of
	 *            <code>eleValue</code>.
	 * @param length
	 *            The length of the array.
	 * @param eleValue
	 *            The element value of the array.
	 * @return the new array of the given length, with each element initialized
	 *         with the given symbolic expression.
	 */
	SymbolicExpression newArray(BooleanExpression context,
			SymbolicType elementValueType, NumericExpression length,
			SymbolicExpression eleValue);

	/**
	 * Returns the NULL pointer of CIVL.
	 * 
	 * @return The NULL pointer of CIVL.
	 */
	SymbolicExpression nullPointer();

	/**
	 * Given a non-trivial pointer, i.e., a pointer to some location inside an
	 * object, returns the parent pointer. For example, a pointer to an array
	 * element returns the pointer to the array.
	 * 
	 * @param pointer
	 *            non-trivial pointer
	 * @return pointer to parent
	 */
	SymbolicExpression parentPointer(SymbolicExpression pointer);

	/**
	 * Computes the reference expression of a pointer. If the pointer is
	 * pointing to some part of the heap, then the reference expression is the
	 * reference expression w.r.t the corresponding heap atomic object;
	 * otherwise, it is the original reference expression of the pointer.
	 * 
	 * @param pointer
	 *            The pointer to whose reference is to be computed.
	 * @return The reference expression of the pointer w.r.t the object it
	 *         points to.
	 */
	ReferenceExpression referenceOfPointer(SymbolicExpression pointer);

	/**
	 * Computes the reference expression of a given heap pointer w.r.t the
	 * corresponding heap object.
	 * 
	 * @param heapPointer
	 *            The heap pointer.
	 * @return The reference expression of a given pointer w.r.t the
	 *         corresponding heap memory unit.
	 */
	ReferenceExpression referenceToHeapMemUnit(SymbolicExpression heapPointer);

	/**
	 * Returns an array element reference by giving the array reference and the
	 * coordinates to indexing the element.
	 * 
	 * This function makes the {@link ArrayElementReference} based on the given
	 * array reference. For example, for an array "int a[2][3][4]", giving the
	 * reference to "a[1]" and an indices array {1,2}, it returns the new
	 * reference to "a[1][1][2]".
	 * 
	 * @author Ziqing Luo
	 * @param arrayReference
	 *            An reference to an array
	 * @param newIndices
	 *            indexes for referencing the element
	 * @return the new arrayElementReference
	 */
	ReferenceExpression makeArrayElementReference(
			ReferenceExpression arrayReference, NumericExpression[] newIndices);

	/**
	 * Returns the undefined pointer of CIVL, which is an uninitialized pointer
	 * value. In CIVL, a pointer becomes undefined when the memory space it
	 * points to get deallocated.
	 * 
	 * @return The undefined pointer.
	 */
	SymbolicExpression undefinedPointer();

	/* ****************** Domain Operation section ********************* */

	/**
	 * Returns true if and only if the domain is more precisely a literal
	 * domain. A literal domain has the form of an array if integer tuples with
	 * fixed size (i.e., the domain's dimension).
	 * 
	 * @param domain
	 *            The symbolic expression of a domain object
	 * @return true iff the given domain is a literal domain.
	 */
	boolean isLiteralDomain(SymbolicExpression domain);

	/**
	 * Get the first integer tuple of a domain. Return null if domain is empty.
	 * 
	 * @param domValue
	 *            The domain object which will contribute a first element.
	 * @return the first integer tuple of a domain, null if the domain is empty
	 */
	List<SymbolicExpression> getDomainInit(SymbolicExpression domValue);

	/**
	 * Computes the size of a domain, that is the number of elements contained
	 * in the domain.
	 * 
	 * @param domain
	 *            The symbolic expression of domain.
	 * @return The number of elements contained in the domain.
	 */
	NumericExpression getDomainSize(SymbolicExpression domain);

	/**
	 * Get the type of elements of domain which are also elements of literal
	 * domain object. For an N dimensional domain, the element type should be an
	 * array of integers of length of N, i.e. <code>int [N]</code>.
	 * 
	 * @param domain
	 *            The symbolic expression of a domain object
	 * @return domain element type
	 * 
	 */
	SymbolicType getDomainElementType(SymbolicExpression domain);

	/**
	 * Return a iterator for a domain object. (This function can be applied on
	 * all domain types)
	 * 
	 * @param domain
	 *            The symbolic expression of the domain object.
	 * @return the iterator for iterating over the domain.
	 */
	Iterator<List<SymbolicExpression>> getDomainIterator(
			SymbolicExpression domain);

	/**
	 * Get the subsequence of the given element of the domain.
	 * 
	 * @param domValue
	 *            The symbolic expression of the rectangular domain field.
	 * @param currentTuple
	 *            The current integer tuple which is an element of the domain.
	 * @param concreteDim
	 *            The dimension of domain, which should be concrete.
	 * @return the next integer tuple in the domain
	 */
	List<SymbolicExpression> getNextInRectangularDomain(
			SymbolicExpression domValue, List<SymbolicExpression> currentTuple,
			int concreteDim);

	/**
	 * Check if the given domain element has a subsequence in the given
	 * rectangular domain. A rectangular domain is the cartesian product of a
	 * number of ranges.s
	 * 
	 * @param rectangularDomain
	 *            The rectangular domain union object.
	 * @param concreteDim
	 *            The number of the dimension of the domain
	 * @param domElement
	 *            The element of the domain
	 * @return true iff there is at least one subsequent element of the given
	 *         one in the given rectangular domain.
	 */
	boolean recDomainHasNext(SymbolicExpression rectangularDomain,
			int concreteDim, List<SymbolicExpression> domElement);

	/**
	 * Iterating a literal domain to match a given domain element. Returns the
	 * index of the element.
	 * 
	 * @param literalDomain
	 *            The symbolic expression of the literal domain union field
	 * @param literalDomElement
	 *            The given element will be matched
	 * @param dim
	 *            The dimension of the literal domain
	 * @return the index of the element in the domain or -1 which means the
	 *         given element is not a member of the domain.
	 */
	int literalDomainSearcher(SymbolicExpression literalDomain,
			List<SymbolicExpression> literalDomElement, int dim);

	/**
	 * Returns true if and only if the given domain is empty which means there
	 * is no elements in the domain.
	 * 
	 * @param domain
	 *            The symbolic expression of the domain object.
	 * @param dim
	 *            The concrete number of dimension of the domain. It's only
	 *            significant when the domain is a rectangular domain.
	 * @param source
	 *            The CIVL source of the statement involves this empty checking
	 *            operation.
	 * @return true iff the given domain is empty
	 */
	boolean isEmptyDomain(SymbolicExpression domain, int dim,
			CIVLSource source);

	/*
	 * ****************** End of Domain Operation section *********************
	 */
	/**
	 * 
	 * <p>
	 * <strong>pre-condition:</strong>
	 * <li>length(array_extents) > 0</li>
	 * </p>
	 * <p>
	 * Computing sizes of all slices of the given array. Here an array slice is
	 * a sub-array with a lower dimension of the given array.
	 * </p>
	 * 
	 * @param array_extents
	 *            Sizes of coordinates representing an array. e.g. {2,3,4}
	 *            stands for an array T a[2][3][4].
	 * @return Sizes of all array slices. e.g. input:{2,3,4} ==> output:{12, 4,
	 *         1}
	 * @throws UnsatisfiablePathConditionException
	 */
	NumericExpression[] arraySlicesSizes(NumericExpression[] array_extents);

	/**
	 * <p>
	 * Given a pointer to an element of an array object, returns a pointer to
	 * the whole array object.
	 * </p>
	 * 
	 * @param arrayPtr
	 *            A pointer refers to an array object.
	 * @return a pointer to the whole array object which is referred by the
	 *         given pointer.
	 */
	SymbolicExpression arrayRootPtr(SymbolicExpression arrayPtr);

	/**
	 * <p>
	 * Given a pointer to an array element, returns the indices of the element.
	 * Array element indices as heap structure are ignored.
	 * </p>
	 * 
	 * @param pointerToArrayElement
	 *            A concrete pointer to an array element.
	 * @return an array of indices with the given reference. The <b>order</b> of
	 *         indices from left to right is same as the lexical subscript
	 *         order.
	 */
	NumericExpression[] extractArrayIndicesFrom(
			SymbolicExpression pointerToArrayElement);

	/**
	 * <p>
	 * <strong>Pre-condition:</strong> For all the descendant types of the array
	 * type, if it is an array type, it must be complete.
	 * </p>
	 * <p>
	 * Computes extent of each dimension of a given array. Returns an Java Array
	 * of extents which are in the same order as the array being declared.<br>
	 * For example, giving an array "int a[2][3][4]" returns {2, 3, 4}.
	 * </p>
	 * 
	 * @param arrayType
	 *            The type of the target array.
	 * @return The Java Array contains array extents information.
	 */
	NumericExpression[] arrayDimensionExtents(
			SymbolicCompleteArrayType arrayType);

	/**
	 * This function does an arithmetic integer division, returns the quotient
	 * and remainder
	 * 
	 * @param dividend
	 *            The {@link NumericExpression} of the quotient, must be a
	 *            Integer number
	 * @param denominator
	 *            The {@link NumericExpression} of the denominator, must be a
	 *            Integer number
	 * @return A {@link Pair} of quotient (left) and remainder (right)
	 */
	Pair<NumericExpression, NumericExpression> arithmeticIntDivide(
			NumericExpression dividend, NumericExpression denominator);

	/**
	 * returns the symbolic type of dynamic type which is used to model
	 * user-defined types. In CIVL, each user-defined type (e.g., a struct) has
	 * a unique ID and is represented as a symbolic expression of the type
	 * "dynamic type".
	 * 
	 * @return the symbolic type of dynamic type
	 */
	SymbolicTupleType dynamicType();

	/**
	 * Is the given domain a rectangular domain?
	 * 
	 * @param domain
	 * @return true iff the given domain is a rectangular domain.
	 */
	boolean isRectangularDomain(SymbolicExpression domain);

	/**
	 * Is the given range a regular range?
	 * 
	 * @param range
	 * @return true iff the given range is a regular range.
	 */
	boolean isRegularRange(SymbolicExpression range);

	/**
	 * Returns the given index-th range of a rectangular domain.
	 * 
	 * Precondition: domain is a rectangular domain.
	 * 
	 * @param domain
	 * @param index
	 * @return the index-th range of the given rectangular domain.
	 */
	SymbolicExpression getRangeOfRectangularDomain(SymbolicExpression domain,
			int index);

	/**
	 * Returns the upper bound of the given range.
	 * 
	 * Precondition: range is a regular range
	 * 
	 * @param range
	 * @return the upper bound of the given range.
	 */
	NumericExpression getHighOfRegularRange(SymbolicExpression range);

	/**
	 * Returns the lower bound of the given range.
	 * 
	 * Precondition: range is a regular range
	 * 
	 * @param range
	 * @return the lower bound of the given range.
	 * 
	 */
	NumericExpression getLowOfRegularRange(SymbolicExpression range);

	/**
	 * Returns the step of the given range
	 * 
	 * Precondition: range is a regular range
	 * 
	 * @param range
	 * @return the step of the given range
	 */
	NumericExpression getStepOfRegularRange(SymbolicExpression range);

	/**
	 * Returns the dimension of the given domain.
	 * 
	 * @param domain
	 * @return the dimension of the given domain.
	 */
	NumericExpression getDimensionOf(SymbolicExpression domain);

	/**
	 * Returns the sub-clauses of a CNF clause. For example, if the given CNF
	 * clause is <code> X==3 && Y<5 && (a[k]>2 || k<0) </code>, then the result
	 * is an array of clauses:<code>{ X==3, Y<5, a[k]>2 || k<0}</code>
	 * 
	 * @param cnfClause
	 * @return the sub-clauses of a CNF clause
	 */
	BooleanExpression[] getConjunctiveClauses(BooleanExpression cnfClause);

	/**
	 * returns the abstract guard of a function call
	 * 
	 * @param function
	 * @param arguments
	 * @return
	 */
	SymbolicExpression getAbstractGuardOfFunctionCall(String library,
			String function, SymbolicExpression[] argumentValues);

	/**
	 * Apply the reverse of a given uninterpreted function. If the argument is
	 * <code>f(X)</code> and the given function is f, then returns
	 * <code>X</code>. If the argument is <code>f(X,Y,Z)</code>, then returns
	 * NULL. If the given function doesn't match the argument's function name,
	 * returns NULL. If the argument's operator isn't APPLY, then returns NULL.
	 * 
	 * @param originalFunction
	 * @param argument
	 * @return
	 */
	SymbolicExpression applyReverseFunction(String originalFunction,
			SymbolicExpression argument);

	/**
	 * <p>
	 * pre-condition : The parameter range is either a range or an integer
	 * </p>
	 * Translate a range to a concrete {@link BitSet}. The given range can be a
	 * regular range or a simple integer which can be seen as a singleton range.
	 * 
	 * @param range
	 * @return
	 */
	BitSet range2BitSet(SymbolicExpression range, Reasoner reasoner);

	/**
	 * <p>
	 * The parameter "pointer" must be a returned value of
	 * {@link #heapMemUnit(SymbolicExpression)}
	 * </p>
	 * <p>
	 * Given a pointer p to a memory heap, returns the malloc ID of the memory
	 * heap.
	 * </p>
	 * 
	 * @param pointer
	 * @return
	 */
	IntObject getMallocID(SymbolicExpression pointer);

	/**
	 * Convert an array type symbolic expression with ARRAY operator to a
	 * concrete Java array.
	 * 
	 * @param array
	 *            the symbolic expression with ARRAY operator
	 * @return the converted Java array
	 */
	SymbolicExpression[] symbolicArrayToConcreteArray(SymbolicExpression array);

	/**
	 * <p>
	 * Tests if the given symbolic expression is a concrete pointer value. A
	 * symbolic expression is a concrete pointer value iff the expression has
	 * {@link CIVLTypeFactory#pointerSymbolicType()} and the expression's
	 * operator is {@link SymbolicOperator#TUPLE} and the arguments of the Tuple
	 * are respectively instances of:
	 * <code>IntObject, SymbolicExpression (of Scope type), ReferenceExpression</code>
	 * </p>
	 * 
	 * @param pointer
	 *            a symbolic expression that will be tested if it is a concrete
	 *            pointer value
	 * @return true iff the given symbolic expression satisfies the description
	 *         above
	 */
	boolean isConcretePointer(SymbolicExpression pointer);

	/**
	 * <p>
	 * Creating a bound variable of the given "type", which is an instance of
	 * {@link SymbolicConstant}, whose name has no conflict with any other
	 * symbolic constants in the given set of "expressions".
	 * </p>
	 * 
	 * @param type
	 *            the type of the returning bound variable
	 * 
	 * @param expressions
	 *            a set of expressions in the scope of the returning bound
	 *            variable
	 * @return a bound variable of the given "type" whose name has no conflict
	 *         with any other symbolic constants in the given set of
	 *         "expressions".
	 */
	SymbolicConstant freshBoundVariableFor(SymbolicType type,
			SymbolicExpression... expressions);

	List<SymbolicConstant> freshBoundVariablesFor(int num, SymbolicType type,
			SymbolicExpression... expressions);

	ValueSetUtility getValueSetUtility();

	SymbolicExpression makeFunctionPointer(int dyscopeID, int fid);

	SymbolicExpression nullFunctionPointer();
}
