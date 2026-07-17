package dev.civl.mc.transform.common;

import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.DIV;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.DIVEQ;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.EQUALS;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.LAND;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.LOR;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.MINUS;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.MINUSEQ;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.NEQ;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.PLUS;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.PLUSEQ;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.TIMES;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.TIMESEQ;
import static dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator.UNARYMINUS;
import static dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind.BOOL;
import static dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind.DOUBLE_COMPLEX;
import static dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind.FLOAT_COMPLEX;
import static dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind.LONG_DOUBLE_COMPLEX;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.conversion.IF.Conversion;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.declaration.DeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.CompoundLiteralNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FloatingConstantNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.type.AtomicTypeNode;
import dev.civl.abc.ast.node.IF.type.BasicTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.type.IF.ArithmeticType;
import dev.civl.abc.ast.type.IF.AtomicType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.ComplexFloatingValue;
import dev.civl.abc.ast.value.IF.RealFloatingValue;
import dev.civl.abc.ast.value.IF.ValueFactory.Answer;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SourceFile;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.mc.config.IF.CIVLConstants;
import dev.civl.mc.model.IF.CIVLInternalException;

/**
 * This class does the work of the complex number transformation. Note: it
 * assumes a side-effect-free program, i.e., the side effect free transformer
 * has already been run. It should not introduce any side-effect expressions.
 * Note the use of functions is not appropriate for complex operations since
 * these cannot occur in quantified formulas.
 */
public class ComplexWorker extends BaseWorker {

	private static String COMPLEX_H = "complex.h";

	private static String COMPLEX_CVL = "complex.cvl";

	private TypeFactory typeFactory;

	public ComplexWorker(String transformerName, ASTFactory astFactory) {
		super(transformerName, astFactory);
		typeFactory = astFactory.getTypeFactory();
	}

	/**
	 * Is the given type one of the 3 native C complex types: double _Complex, float
	 * _Complex, or long double _Complex? This includes qualified versions of those
	 * types and the atomic versions of them.
	 * 
	 * @param type the type, which may be null
	 * @return {@code} true iff {@code type} is one of the 3 native C complex types
	 */
	private boolean isComplex(Type type) {
		if (type == null)
			return false;
		switch (type.kind()) {
		case BASIC: {
			BasicTypeKind btk = ((StandardBasicType) type).getBasicTypeKind();
			return btk == DOUBLE_COMPLEX || btk == FLOAT_COMPLEX || btk == LONG_DOUBLE_COMPLEX;
		}
		case QUALIFIED:
			return isComplex(((QualifiedObjectType) type).getBaseType());
		case ATOMIC:
			return isComplex(((AtomicType) type).getBaseType());
		default:
			return false;
		}
	}

	/**
	 * Is the given type a boolean type? This corresponds to the C (or CIVL-C) type
	 * {@code Bool} as well as qualified versions of that type and the atomic
	 * version.
	 * 
	 * @param type any type
	 * @return {@code true} iff {@code type} is a boolean type
	 */
	private boolean isBool(Type type) {
		if (type == null)
			return false;
		switch (type.kind()) {
		case BASIC: {
			BasicTypeKind btk = ((StandardBasicType) type).getBasicTypeKind();
			return btk == BOOL;
		}
		case QUALIFIED:
			return isBool(((QualifiedObjectType) type).getBaseType());
		case ATOMIC:
			return isBool(((AtomicType) type).getBaseType());
		default:
			return false;
		}
	}

	/**
	 * Is the given type a real type? Note that includes integer types,
	 * enumerations, and (non-complex) floating-point types. It includes such types
	 * that are qualified, or atomic. In general this corresponds to the notion of
	 * "real domain" in the C Standard.
	 * 
	 * @param type any type
	 * @return {@code true} iff {@code type} is in the real domain
	 */
	private boolean isReal(Type type) {
		if (type == null)
			return false;
		if (type instanceof ArithmeticType)
			return ((ArithmeticType) type).inRealDomain();
		if (type instanceof QualifiedObjectType)
			return isReal(((QualifiedObjectType) type).getBaseType());
		if (type instanceof AtomicType)
			return isReal(((AtomicType) type).getBaseType());
		return false;
	}

	/**
	 * Returns the basic type kind of a complex type.
	 * 
	 * @param complexType one of the complex types
	 * @return the basic type kind of the given type
	 */
	private BasicTypeKind kind(Type complexType) {
		switch (complexType.kind()) {
		case BASIC:
			return ((StandardBasicType) complexType).getBasicTypeKind();
		case QUALIFIED:
			return kind(((QualifiedObjectType) complexType).getBaseType());
		case ATOMIC:
			return kind(((AtomicType) complexType).getBaseType());
		default:
			throw new RuntimeException("unreachable");
		}
	}

	/**
	 * Constructs a new typedef name node corresponding to the given complex type.
	 * If the complex type has qualifiers or is atomic, that information is ignored.
	 * This method simply creates a typedef name node such as "$double_complex"
	 * without qualifiers.
	 * 
	 * @param source      the source to use for the new node
	 * @param complexType any complex type
	 * @return the new typedef name node
	 */
	private TypedefNameNode typedefName(Source source, Type complexType) {
		IdentifierNode idn;
		switch (kind(complexType)) {
		case DOUBLE_COMPLEX:
			idn = nodeFactory.newIdentifierNode(source, "$double_complex");
			break;
		case FLOAT_COMPLEX:
			idn = nodeFactory.newIdentifierNode(source, "$float_complex");
			break;
		case LONG_DOUBLE_COMPLEX:
			idn = nodeFactory.newIdentifierNode(source, "$ldouble_complex");
			break;
		default:
			throw new RuntimeException("unreachable");
		}
		TypedefNameNode result = nodeFactory.newTypedefNameNode(idn, null);
		result.setType(complexType);
		return result;
	}

	/**
	 * Given a type node for one of the complex types, returns a new type node for
	 * the corresponding CIVL complex type: one of the $*_complex types. Type
	 * qualifiers are preserved.
	 * 
	 * Note: a type node for a complex type must be one of the following: a
	 * {@link TypedefNameNode}, {@link BasicTypeNode}, or {@link AtomicTypeNode}.
	 * 
	 * @param complexTypeNode a basic type kind, one of *_COMPLEX
	 * @param source          source for the type node for the new node
	 * @return new typedef name node
	 */
	private TypeNode replacementTypeNode(TypeNode complexTypeNode) {
		Source source = complexTypeNode.getSource();
		TypedefNameNode typedefName = typedefName(source, complexTypeNode.getType());
		typedefName.setAtomicQualified(complexTypeNode.isAtomicQualified());
		typedefName.setConstQualified(complexTypeNode.isConstQualified());
		typedefName.setRestrictQualified(complexTypeNode.isRestrictQualified());
		typedefName.setVolatileQualified(complexTypeNode.isVolatileQualified());
		typedefName.setInputQualified(complexTypeNode.isInputQualified());
		typedefName.setOutputQualified(complexTypeNode.isOutputQualified());
		return typedefName;
	}

	/**
	 * Given a complex type, this method constructs a type node representing that
	 * type, preserving qualifiers and atomicity.
	 * 
	 * @param complexType any complex type
	 * @param source      the source to use for the new node
	 * @return a type node corresponding exactly to the given type
	 */
	private TypeNode complexTypeNode(Type complexType, Source source) {
		TypedefNameNode typedefName = typedefName(source, complexType);
		// Note: _Atomic(type) is a type specifier, represented by an AtomicType and an
		// AtomicTypeNode.
		// _Atomic ... is a type qualifier, represented by an AtomicType and an
		// arbitrary TypeNode with the atomic-qualified bit set.
		switch (complexType.kind()) {
		case BASIC:
			return typedefName;
		case QUALIFIED: {
			QualifiedObjectType qot = (QualifiedObjectType) complexType;
			typedefName.setAtomicQualified(false);
			typedefName.setConstQualified(qot.isConstQualified());
			typedefName.setRestrictQualified(qot.isRestrictQualified());
			typedefName.setVolatileQualified(qot.isVolatileQualified());
			typedefName.setInputQualified(qot.isInputQualified());
			typedefName.setOutputQualified(qot.isOutputQualified());
			return typedefName;
		}
		case ATOMIC:
			// choice: AtomicTypeNode, or just qualify the typedef name node.
			return nodeFactory.newAtomicTypeNode(source, typedefName);
		default:
			throw new RuntimeException("unreachable");
		}
	}

	/**
	 * Constructs a new node representing the real floating point number 0, with the
	 * real type corresponding to the given complex type: either float, double, or
	 * long double type.
	 * 
	 * @param complexType the complex type used to determine the real type
	 * @param source      source to be used for new node
	 * @return node representing real 0
	 */
	private ExpressionNode realZero(Type complexType, Source source) {
		String zeroString;
		BasicTypeKind kind = kind(complexType);
		if (kind == DOUBLE_COMPLEX) {
			zeroString = "0.0";
		} else if (kind == FLOAT_COMPLEX) {
			zeroString = "0.0f";
		} else if (kind == LONG_DOUBLE_COMPLEX) {
			zeroString = "0.0l";
		} else {
			throw new RuntimeException("unreachable");
		}
		try {
			return nodeFactory.newFloatingConstantNode(source, zeroString);
		} catch (SyntaxException e) {
			throw new CIVLInternalException("Syntax error parsing zero constant: " + zeroString, source);
		}
	}

	/**
	 * Is the operator one of the assignment operators that combines an arithmetic
	 * operation with assignment, possibly on complex numbers: +=, -=, *=, or /=.
	 * 
	 * @param op any Operator
	 * @return {@code true} iff {@code op} is one of the 4 operators above
	 */
	private boolean isAssignOp(Operator op) {
		return op == PLUSEQ || op == MINUSEQ || op == TIMESEQ || op == DIVEQ;
	}

	/**
	 * Is the operator one that performs an arithmetic operation that could possibly
	 * consume a complex type. This includes the assignment operators +=, -=, etc.,
	 * as well as the pure operators +,-, etc. It includes == and !=, and the unary
	 * minus operator as well.
	 * 
	 * @param op any Operator
	 * @return {@code true} iff {@code op} is an operator
	 */
	private boolean isArithmeticOp(Operator op) {
		return isAssignOp(op) || op == PLUS || op == MINUS || op == TIMES || op == DIV || op == EQUALS || op == NEQ
				|| op == UNARYMINUS;
	}

	/**
	 * Constructs new tree that applies the ".real" operator to the given argument.
	 * 
	 * Precondition: {@code complexNode} is unattached.
	 * 
	 * @param complexNode node for an expression of complex type
	 * @return expression representing the real part of {@code complexNode}.
	 */
	private ExpressionNode realPart(ExpressionNode complexNode) {
		if (complexNode.expressionKind() == ExpressionKind.COMPOUND_LITERAL) {
			CompoundLiteralNode cln = (CompoundLiteralNode) complexNode;
			CompoundInitializerNode cin = cln.getInitializerList();
			int n = cin.numChildren();
			assert n == 2;
			PairNode<DesignationNode, InitializerNode> pair0 = cin.getSequenceChild(0);
			if (pair0.getLeft() == null) {
				ExpressionNode result = (ExpressionNode) pair0.getRight();
				return result.copy();
			} else {
				// TODO: find the designation for "real"
			}
		}
		Source source = complexNode.getSource();
		ExpressionNode result = nodeFactory.newDotNode(source, complexNode,
				nodeFactory.newIdentifierNode(source, "real"));
		return result;
	}

	/**
	 * Constructs new tree that applies the ".imag" operator to the given argument.
	 * 
	 * Precondition: {@code complexNode} is unattached.
	 * 
	 * @param complexNode node for an expression of complex type
	 * @return expression representing the imaginary part of {@code complexNode}.
	 */
	private ExpressionNode imagPart(ExpressionNode complexNode) {
		if (complexNode.expressionKind() == ExpressionKind.COMPOUND_LITERAL) {
			CompoundLiteralNode cln = (CompoundLiteralNode) complexNode;
			CompoundInitializerNode cin = cln.getInitializerList();
			int n = cin.numChildren();
			assert n == 2;
			PairNode<DesignationNode, InitializerNode> pair0 = cin.getSequenceChild(0), pair1 = cin.getSequenceChild(1);
			if (pair0.getLeft() == null && pair1.getLeft() == null) {
				ExpressionNode result = (ExpressionNode) pair1.getRight();
				return result.copy();
			} else {
				// TODO: find the designation for "imag"
			}
		}
		Source source = complexNode.getSource();
		ExpressionNode result = nodeFactory.newDotNode(source, complexNode,
				nodeFactory.newIdentifierNode(source, "imag"));
		return result;
	}

	/**
	 * Makes a compound literal node representing the complex number specified by
	 * the given real and imaginary parts. Example:
	 * 
	 * <pre>
	 *  ($double_complex){ realPart, imagPart }
	 * </pre>
	 * 
	 * Preconditions: {@code realPart} and {@code imagPart} must be unattached
	 * 
	 * @param realPart    the node representing the real part of the complex number
	 * @param imagPart    the node representing the imaginary part of the complex
	 *                    number
	 * @param complexType the type of the new complex value
	 * @return a new compound literal node representing the complex number
	 */
	private ExpressionNode makeComplex(Source source, ExpressionNode realPart, ExpressionNode imagPart,
			Type complexType) {
		PairNode<DesignationNode, InitializerNode> realPair = nodeFactory.newPairNode(source, null, realPart),
				imagPair = nodeFactory.newPairNode(source, null, imagPart);
		TypeNode typeNode = complexTypeNode(complexType, source);
		CompoundInitializerNode cin = nodeFactory.newCompoundInitializerNode(source, Arrays.asList(realPair, imagPair));
		CompoundLiteralNode cln = nodeFactory.newCompoundLiteralNode(source, typeNode, cin);
		cln.setInitialType(complexType);
		return cln;
	}

	/**
	 * Given an expression node of a real type, and a complex type, this method
	 * constructs a new tree representing the result of converting that real
	 * expression to the complex type. The given real expression will be removed if
	 * it is attached to some parent.
	 * 
	 * @param realExpr    any expression of real type (including an integer type,
	 *                    for example)
	 * @param complexType any complex type
	 * @return the expression representing the conversion of the real expression to
	 *         the complex type
	 */
	private ExpressionNode realToComplex(ExpressionNode realExpr, Type complexType) {
		// Result will look like: ($*_complex){ realExpr, 0 }
		// The int 0 will be converted to the appropriate real type.
		// Note: we already checked all the static type properties before getting to
		// this Transformer, so we can assume they are all good.
		Source source = realExpr.getSource();
		ExpressionNode zeroNode = nodeFactory.newIntConstantNode(source, 0);
		realExpr.remove();
		return makeComplex(source, realExpr, zeroNode, complexType);
	}

	/**
	 * Constructs a new type node for the real floating type corresponding to the
	 * given complex type. For example, given the complex type "double _Complex" (or
	 * any qualified form of that type), this method constructs a type node for the
	 * type "double" (with no qualifiers).
	 * 
	 * @param source      the source to use for the new typedef name node
	 * @param complexType any of the complex types (with or without qualifiers)
	 * @return a new type node for the corresponding real type
	 */
	private TypeNode realTypeNode(Source source, Type complexType) {
		switch (kind(complexType)) {
		case DOUBLE_COMPLEX:
			return nodeFactory.newBasicTypeNode(source, BasicTypeKind.DOUBLE);
		case FLOAT_COMPLEX:
			return nodeFactory.newBasicTypeNode(source, BasicTypeKind.FLOAT);
		case LONG_DOUBLE_COMPLEX:
			return nodeFactory.newBasicTypeNode(source, BasicTypeKind.LONG_DOUBLE);
		default:
			throw new RuntimeException("Illegal complex type: " + complexType);
		}
	}

	/**
	 * Transforms an expression of complex type to boolean. Pattern: {@code x} of
	 * float type transforms to {@code x.real != 0.0f || x.imag != 0.0f}. The given
	 * node will be removed if it is attached.
	 * 
	 * @param node        an expression of a complex type
	 * @param complexType the exact type of the given expression
	 * @return a new expression node representing the condition that the given
	 *         complex expression is non-zero
	 */
	private ExpressionNode complexToBool(ExpressionNode node, Type complexType) {
		Source source = node.getSource();
		node.remove();
		// need to duplicate the node for the imaginary part...
		ExpressionNode node2 = node.copy();
		OperatorNode neq1 = nodeFactory.newOperatorNode(source, NEQ, realPart(node),
				nodeFactory.newIntConstantNode(source, 0));
		OperatorNode neq2 = nodeFactory.newOperatorNode(source, NEQ, imagPart(node2),
				nodeFactory.newIntConstantNode(source, 0));
		OperatorNode or = nodeFactory.newOperatorNode(source, LOR, neq1, neq2);
		or.setInitialType(typeFactory.basicType(BOOL));
		return or;
	}

	/**
	 * Converts from one complex type to another. Given a node representing an
	 * expression of one complex type, this method constructs a node representing
	 * the result of converting that expression to another complex type.
	 * 
	 * Example: given an expression node of type "double _Complex", this method
	 * constructs the compound literal
	 * 
	 * <pre>
	 *  ($double_complex){ (double)node.real, (double)node.imag }
	 * </pre>
	 * 
	 * 
	 * @param node           a node representing any expression of any complex type
	 * @param oldComplexType the exactly type of the given expression
	 * @param newComplexType the new complex type for the expression
	 * @return a node representing the result of converting the given complex
	 *         expression to the new complex type (this may be the given node, if
	 *         the two types are equal)
	 */
	private ExpressionNode complexToComplex(ExpressionNode node, Type oldComplexType, Type newComplexType) {
		Source source = node.getSource();
		if (oldComplexType.equals(newComplexType))
			return node;
		node.remove();
		ExpressionNode node2 = node.copy();
		ExpressionNode realPart = realPart(node);
		ExpressionNode imagPart = imagPart(node2);
		ExpressionNode newRealPart = nodeFactory.newCastNode(source, realTypeNode(source, newComplexType), realPart);
		ExpressionNode newImagPart = nodeFactory.newCastNode(source, realTypeNode(source, newComplexType), imagPart);
		ExpressionNode result = makeComplex(source, newRealPart, newImagPart, newComplexType);
		result.setInitialType(newComplexType);
		return result;
	}

	/**
	 * Converts an expression of complex type to real type by dropping the imaginary
	 * component.
	 * 
	 * @param node     an expression of a complex type
	 * @param realType the real type
	 * @return result of converting to real type
	 */
	private ExpressionNode complexToReal(ExpressionNode node, Type realType) {
		node.remove();
		ExpressionNode result = realPart(node);
		result.setInitialType(realType);
		return result;
	}

	/**
	 * Converts an expression from one type to another, where at least one of the
	 * two types is complex.
	 * 
	 * @param node    an expression
	 * @param oldType the type of the given expression
	 * @param newType the new type
	 * @return expression representing result of conversion from old type to new
	 *         type
	 */
	private ExpressionNode convert(ExpressionNode node, Type oldType, Type newType) {
		if (isComplex(oldType)) {
			if (isBool(newType))
				return complexToBool(node, oldType);
			else if (isReal(newType))
				return complexToReal(node, newType);
			else if (isComplex(newType))
				return complexToComplex(node, oldType, newType);
			else
				throw new CIVLInternalException("No conversion from " + oldType + " to " + newType, node.getSource());
		} else if (isComplex(newType)) { // non-complex -> complex
			return realToComplex(node, newType);
		}
		// conversion does not involve complex type: ignore
		return node;
	}

	/**
	 * Converts a literal node that has one of the original _Complex types to a
	 * struct literal using the new complex struct types.
	 * 
	 * @param fcn a node representing an imaginary constant, such as 1.0i
	 * @return a tree using the struct representation as ordered pair, e.g.,
	 *         ($double_complex){1.0, 0.0}.
	 */
	private ExpressionNode convertLiteral(FloatingConstantNode fcn) {
		assert fcn.isComplex();
		Type complexType = fcn.getInitialType();
		Source source = fcn.getSource();
		ComplexFloatingValue value = (ComplexFloatingValue) fcn.getConstantValue();
		RealFloatingValue realPart = value.getRealPart(), imagPart = value.getImaginaryPart();
		assert realPart.isZero() == Answer.YES;

		// The representation is used to print the second component of a struct,
		// so need to strip off the imaginary modifier.
		String representation = fcn.getStringRepresentation();
		int n = representation.length();
		String lower = representation.toLowerCase();
		if (lower.endsWith("i") || lower.endsWith("j"))
			representation = representation.substring(0, n - 1);
		else if (lower.endsWith("if") || lower.endsWith("il") || lower.endsWith("jf") || lower.endsWith("jl"))
			representation = representation.substring(0, n - 2) + representation.charAt(n - 1);

		FloatingConstantNode imagNode = nodeFactory.newFloatingConstantNode(source, representation, fcn.wholePart(),
				fcn.fractionPart(), fcn.exponent(), imagPart);
		ExpressionNode zeroNode = realZero(fcn.getConvertedType(), source);
		ExpressionNode result = makeComplex(source, zeroNode, imagNode, complexType);
		result.setInitialType(complexType);
		return result;
	}

	/**
	 * Real addition. Constructs new operator node with children x and y.
	 * 
	 * @param source the source to use for the new node
	 * @param x      expression of real type
	 * @param y      expression of real type
	 * @return expression representing sum of {@code x} and {@code y}
	 */
	private ExpressionNode plus(Source source, ExpressionNode x, ExpressionNode y) {
		return nodeFactory.newOperatorNode(source, PLUS, x, y);
	}

	/**
	 * Real subtraction. Constructs new operator node with children x and y.
	 * 
	 * @param source the source to use for the new node
	 * @param x      expression of real type
	 * @param y      expression of real type
	 * @return expression representing difference of {@code x} and {@code y}
	 */
	private ExpressionNode minus(Source source, ExpressionNode x, ExpressionNode y) {
		return nodeFactory.newOperatorNode(source, MINUS, x, y);
	}

	/**
	 * Real multiplication. Constructs new operator node with children x and y.
	 * 
	 * @param source the source to use for the new node
	 * @param x      expression of real type
	 * @param y      expression of real type
	 * @return expression representing product of {@code x} and {@code y}
	 */
	private ExpressionNode times(Source source, ExpressionNode x, ExpressionNode y) {
		return nodeFactory.newOperatorNode(source, TIMES, x, y);
	}

	/**
	 * Real division. Constructs new operator node with children x and y.
	 * 
	 * @param source the source to use for the new node
	 * @param x      expression of real type
	 * @param y      expression of real type
	 * @return expression representing quotient of {@code x} and {@code y}
	 */
	private ExpressionNode div(Source source, ExpressionNode x, ExpressionNode y) {
		return nodeFactory.newOperatorNode(source, DIV, x, y);
	}

	private ExpressionNode arithmeticReplacement(OperatorNode opNode) {
		Operator op = opNode.getOperator();
		Source source = opNode.getSource();
		int numArgs = opNode.getNumberOfArguments();
		ExpressionNode[] args = new ExpressionNode[numArgs];
		Type type = opNode.getInitialType();
		ExpressionNode result;

		for (int i = 0; i < numArgs; i++) {
			ExpressionNode arg = opNode.getArgument(i);
			arg.remove();
			args[i] = arg;
		}
		switch (op) {
		case PLUS:
		case MINUS: {
			// x+y ==> {x.real+y.real, x.imag+y.imag};
			ExpressionNode x = args[0], y = args[1], xReal = realPart(x), xImag = imagPart(x.copy()),
					yReal = realPart(y), yImag = imagPart(y.copy());
			result = makeComplex(source, nodeFactory.newOperatorNode(source, op, xReal, yReal),
					nodeFactory.newOperatorNode(source, op, xImag, yImag), type);
			break;
		}
		case TIMES: {
			// x*y ==> {x.real*y.real - x.imag*y.imag, x.real*y.imag + x.imag*y.real}
			ExpressionNode x = args[0], y = args[1], xReal = realPart(x), xImag = imagPart(x.copy()),
					yReal = realPart(y), yImag = imagPart(y.copy());
			ExpressionNode newReal = minus(source, times(source, xReal, yReal), times(source, xImag, yImag)),
					newImag = plus(source, times(source, xReal.copy(), yImag.copy()),
							times(source, xImag.copy(), yReal.copy()));
			result = makeComplex(source, newReal, newImag, type);
			break;
		}
		case DIV: {
			// x/y: let a = x.real, b = x.imag, c = y.real, d = y.imag, r = c*c+d*d;
			// {(a*c+b*d)/r, (b*c-a*d)/r}
			ExpressionNode x = args[0], y = args[1], a = realPart(x), b = imagPart(x.copy()), c = realPart(y),
					d = imagPart(y.copy());
			ExpressionNode r = plus(source, times(y.getSource(), c, c.copy()), times(y.getSource(), d, d.copy()));
			ExpressionNode newReal = div(source, plus(source, times(source, a, c.copy()), times(source, b, d.copy())),
					r);
			ExpressionNode newImag = div(source,
					minus(source, times(source, b.copy(), c.copy()), times(source, a.copy(), d.copy())), r.copy());
			result = makeComplex(source, newReal, newImag, type);
			break;
		}
		case UNARYMINUS: {
			// -x : {-x.real, -x.imag}
			ExpressionNode x = args[0], xReal = realPart(x), xImag = imagPart(x.copy());
			ExpressionNode newReal = nodeFactory.newOperatorNode(source, UNARYMINUS, xReal),
					newImag = nodeFactory.newOperatorNode(source, UNARYMINUS, xImag);
			result = makeComplex(source, newReal, newImag, type);
			break;
		}
		case EQUALS: {
			// x==y : x.real==y.real && x.imag==y.imag
			ExpressionNode x = args[0], xReal = realPart(x), xImag = imagPart(x.copy());
			ExpressionNode y = args[1], yReal = realPart(y), yImag = imagPart(y.copy());
			result = nodeFactory.newOperatorNode(source, LAND,
					nodeFactory.newOperatorNode(source, EQUALS, xReal, yReal),
					nodeFactory.newOperatorNode(source, EQUALS, xImag, yImag));
			result.setInitialType(type);
			break;
		}
		case NEQ: {
			// x!=y : x.real!=y.real || x.imag!=y.imag
			ExpressionNode x = args[0], xReal = realPart(x), xImag = imagPart(x.copy());
			ExpressionNode y = args[1], yReal = realPart(y), yImag = imagPart(y.copy());
			result = nodeFactory.newOperatorNode(source, LOR, nodeFactory.newOperatorNode(source, NEQ, xReal, yReal),
					nodeFactory.newOperatorNode(source, NEQ, xImag, yImag));
			result.setInitialType(type);
			break;
		}
		default:
			throw new RuntimeException("unreachable");
		// Note: PLUSEQ, MINUSEQ, TIMESEQ, DIVEQ should have been removed by side-effect
		// remover.
		}
		return result;
	}

	/**
	 * Is the function called one of the complex functions that can be replaced by a
	 * pure expressions?
	 * 
	 * @param fcn any function call node
	 * @return {@code true} iff the the call is to a function in the complex library
	 *         and that functions is one of the ones that can be defined by a simple
	 *         expression
	 */
	private boolean isReplaceableCall(FunctionCallNode fcn) {
		ExpressionNode funcExpr = fcn.getFunction();
		if (funcExpr instanceof IdentifierExpressionNode) {
			IdentifierNode funcIdent = ((IdentifierExpressionNode) funcExpr).getIdentifier();
			Entity entity = funcIdent.getEntity();
			if (entity instanceof Function) {
				Function func = (Function) entity;
				DeclarationNode funcDecl = func.getDeclaration(0);
				Source funcSource = funcDecl.getSource();
				String funcFilename = funcSource.getFirstToken().getSourceFile().getName();
				if (COMPLEX_H.equals(funcFilename)) {
					switch (funcIdent.name()) {
					case "CMPLX":
					case "CMPLXF":
					case "CMPLXL":
					case "cabs":
					case "cabsf":
					case "cabsl":
					case "creal":
					case "crealf":
					case "creall":
					case "cimag":
					case "cimagf":
					case "cimagl":
					case "conj":
					case "conjf":
					case "conjl":
						return true;
					default:
					}
				}
			}
		}
		return false;
	}

	/**
	 * Given a replaceable function call node, this method constructs an expression
	 * tree which can replace that function call. This allows the call to be used in
	 * quantified formulas or wherever an expression is asked for.
	 * 
	 * @param fcn a function call node satisfying
	 *            {@link #isReplaceableCall(FunctionCallNode)}
	 * @return a new expression tree which computes the same thing as the function
	 */
	private ExpressionNode transformFunctionCall(FunctionCallNode fcn) {
		String funcName = ((IdentifierExpressionNode) fcn.getFunction()).getIdentifier().name();
		Type returnType = fcn.getInitialType();
		Source source = fcn.getSource();
		SequenceNode<ExpressionNode> arguments = fcn.getArguments();
		int numArgs = arguments.numChildren();
		ExpressionNode[] args = new ExpressionNode[numArgs];

		arguments.remove();
		for (int i = 0; i < numArgs; i++) {
			ExpressionNode arg = arguments.getSequenceChild(i);
			arg.remove();
			args[i] = arg;
		}
		switch (funcName) {
		case "CMPLX":
		case "CMPLXF":
		case "CMPLXL":
			return makeComplex(source, args[0], args[1], returnType);
		case "cabs":
		case "cabsf":
		case "cabsl": {
			// unfortunately sqrt and $pow cannot be used in quantified expressions
			// $pow(x.real*x.real + x.imag*x.imag, 0.5)
			ExpressionNode x = args[0], xReal = realPart(x), xImag = imagPart(x.copy()),
					s = plus(source, times(source, xReal, xReal.copy()), times(source, xImag, xImag.copy()));
			ExpressionNode half;
			try {
				half = nodeFactory.newFloatingConstantNode(source, "0.5");
			} catch (SyntaxException e) {
				throw new RuntimeException("unrechable");
			}
			ExpressionNode result = nodeFactory.newFunctionCallNode(source,
					nodeFactory.newIdentifierExpressionNode(source, nodeFactory.newIdentifierNode(source, "$pow")),
					Arrays.asList(s, half));
			result.setInitialType(returnType);
			return result;
		}
		case "creal":
		case "crealf":
		case "creall": {
			ExpressionNode result = realPart(args[0]);
			result.setInitialType(returnType);
			return result;
		}
		case "cimag":
		case "cimagf":
		case "cimagl": {
			ExpressionNode result = imagPart(args[0]);
			result.setInitialType(returnType);
			return result;
		}
		case "conj":
		case "conjf":
		case "conjl": {
			return makeComplex(source, realPart(args[0]),
					nodeFactory.newOperatorNode(source, UNARYMINUS, imagPart(args[0].copy())), returnType);
		}
		default:
			throw new RuntimeException("unreachable");
		}
	}

	/**
	 * Replaces C complex primitives with CIVL-C structure primitives in complex.cvh
	 * and complex.cvl.
	 * 
	 * typeNode: just replace type node
	 * 
	 * constantNode "3if" followed by conversion to double complex: first
	 * convertLiteral then apply conversions.
	 * 
	 * constantNode "1" converted to a complex type: apply conversions
	 * 
	 * operator node "a+b" followed by conversions: first arithmetic replacement,
	 * then apply conversions
	 * 
	 * cast node to or from complex: convert cast, then apply additional implicit
	 * conversions
	 * 
	 * Expression node: first translate to new node, then apply conversions.
	 * 
	 * @param node the root of the tree in which replacement will occur
	 * @return {@code true} iff any change was made to the tree
	 */
	private boolean process(ASTNode node) {
		boolean change = false;
		int numChildren = node.numChildren();

		for (int i = 0; i < numChildren; i++) {
			ASTNode child = node.child(i);
			if (child != null && process(child))
				change = true;
		}

		ASTNode parent = node.parent();
		int idx = node.childIndex();

		if (node instanceof TypeNode) {
			// for reasons I don't understand, a TypedefNameNode may
			// contain qualifiers but those are not present in its Type.
			Type type = ((TypeNode) node).getType();
			if (type != null && isComplex(type)) {
				node = replacementTypeNode((TypeNode) node);
				assert node != null;
				parent.setChild(idx, node);
				change = true;
			}
		} else if (node instanceof ExpressionNode) {
			// first, save the conversions:
			int numConversions = ((ExpressionNode) node).getNumConversions();
			Conversion[] conversions = new Conversion[numConversions];
			for (int i = 0; i < numConversions; i++)
				conversions[i] = ((ExpressionNode) node).getConversion(i);

			if (node instanceof OperatorNode) {
				OperatorNode opNode = (OperatorNode) node;
				if (isArithmeticOp(opNode.getOperator()) && isComplex(opNode.getArgument(0).getConvertedType())) {
					node = arithmeticReplacement(opNode);
					assert node != null;
					parent.setChild(idx, node);
					change = true;
				}
			} else if (node instanceof FloatingConstantNode) {
				FloatingConstantNode fcn = (FloatingConstantNode) node;
				if (fcn.isComplex()) {
					node = convertLiteral(fcn);
					assert node != null;
					parent.setChild(idx, node);
					change = true;
				}
			} else if (node instanceof CastNode) {
				ExpressionNode arg = ((CastNode) node).getArgument();
				Type oldType = arg.getConvertedType();
				Type newType = ((CastNode) node).getInitialType();
				ExpressionNode tmp = convert(arg, oldType, newType);
				if (tmp != arg) {
					node = tmp;
					parent.setChild(idx, node);
					change = true;
				}
			} else if (node instanceof FunctionCallNode) {
				if (isReplaceableCall((FunctionCallNode) node)) {
					node.remove();
					node = transformFunctionCall((FunctionCallNode) node);
					parent.setChild(idx, node);
					change = true;
				}
			}

			// now, apply the conversions:
			for (int i = 0; i < numConversions; i++) {
				Conversion cv = conversions[i];
				ExpressionNode tmp = convert((ExpressionNode) node, cv.getOldType(), cv.getNewType());
				if (tmp != node) {
					node = tmp;
					parent.setChild(idx, node);
					change = true;
				}
			}
		} else if (node instanceof IfNode || node instanceof LoopNode) {
			ExpressionNode cond = node instanceof IfNode ? ((IfNode) node).getCondition()
					: ((LoopNode) node).getCondition();
			int condIdx = cond.childIndex();
			Type type = cond.getType();
			if (isComplex(type)) {
				cond = complexToBool(cond, type);
				node.setChild(condIdx, cond);
				change = true;
			}
		}
		return change;
	}

	@Override
	protected AST transformCore(AST ast) throws SyntaxException {
		SequenceNode<BlockItemNode> root = ast.getRootNode();
		boolean needsTransform = false;

		for (ASTNode node = root; !needsTransform && node != null; node = node.nextDFS()) {
			if (node instanceof ExpressionNode) {
				ExpressionNode expr = (ExpressionNode) node;
				if (isComplex(expr.getInitialType())) {
					needsTransform = true;
					break;
				}
				if (!needsTransform) {
					int numConversions = expr.getNumConversions();
					for (int i = 0; !needsTransform && i < numConversions; i++) {
						if (isComplex(expr.getConversion(i).getNewType())) {
							needsTransform = true;
							break;
						}
					}
				}
			} else if (node instanceof TypeNode) {
				if (isComplex(((TypeNode) node).getType())) {
					needsTransform = true;
					break;
				}
			}
		}

		if (!needsTransform)
			return ast;

		boolean isWhole = ast.isWholeProgram();
		Collection<SourceFile> sourceFiles = ast.getSourceFiles();
		boolean hasComplexCvl = false;

		// remove all items from complex.h...
		ast.release();
		int nchildren = root.numChildren();
		for (int i = 0; i < nchildren; i++) {
			BlockItemNode node = root.getSequenceChild(i);
			Source source = node.getSource();
			String sourceName = source.getFirstToken().getSourceFile().getName();
			if (COMPLEX_H.equals(sourceName)) {
				root.removeChild(i);
			} else if (COMPLEX_CVL.equals(sourceName)) {
				hasComplexCvl = true;
			}
		}
		// TODO: this only sets the child to null. get rid of the null gaps?

		process(root);
		if (!hasComplexCvl) {
			// insert complex.cvl (which includes complex.cvh) at beginning:
			File file = new File(CIVLConstants.CIVL_LIB_SRC_PATH, COMPLEX_CVL);
			AST lib = this.parseSystemLibrary(file, EMPTY_MACRO_MAP);
			SequenceNode<BlockItemNode> libRoot = lib.getRootNode();
			lib.release();
			List<BlockItemNode> libNodes = new LinkedList<BlockItemNode>();
			for (BlockItemNode node : libRoot) {
				node.remove();
				libNodes.add(node);
			}
			root.insertChildren(0, libNodes);
		}
		ast = astFactory.newAST(root, sourceFiles, isWhole);
		return ast;
	}
}
