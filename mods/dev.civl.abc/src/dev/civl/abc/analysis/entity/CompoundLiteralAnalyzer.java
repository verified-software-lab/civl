package dev.civl.abc.analysis.entity;

import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.compound.ArrayDesignatorNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.compound.DesignatorNode;
import dev.civl.abc.ast.node.IF.compound.FieldDesignatorNode;
import dev.civl.abc.ast.node.IF.compound.LiteralObject;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.type.IF.ArrayType;
import dev.civl.abc.ast.type.IF.Field;
import dev.civl.abc.ast.type.IF.IntegerType;
import dev.civl.abc.ast.type.IF.ObjectType;
import dev.civl.abc.ast.type.IF.QualifiedObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.IntegerValue;
import dev.civl.abc.ast.value.IF.ValueFactory;
import dev.civl.abc.err.IF.ABCRuntimeException;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.UnsourcedException;

/**
 * An instance of this class is used to analyze compound literals.
 * 
 * Initialization, including of compound objects, is specified in C11 6.7.9.
 * Note in particular the following:
 * 
 * <pre>
 * 2. No initializer shall attempt to provide a value for an object
 *    not contained within the entity being initialized.
 * 3. The type of the entity to be initialized shall be an array
 *    of unknown size or a complete object type that is not a
 *    variable length array type.
 * 4. All the expressions in an initializer for an object that has
 *    static or thread storage duration shall be constant expressions
 *    or string literals.
 * 5. If the declaration of an identifier has block scope, and
 *    the identifier has external or internal linkage, the
 *    declaration shall have no initializer for the identifier.
 * 6. If a designator has the form
 *          [ constant-expression ]
 * 	  then the current object (defined below) shall have array
 *    type and the expression shall be an integer constant expression.
 *    If the array is of unknown size, any nonnegative value is valid.
 * 7. If a designator has the form
 *          . identifier
 *    then the current object (defined below) shall have structure
 *    or union type and the identifier shall be the name of a
 *    member of that type.
 * </pre>
 * 
 * The array extents must be either constants (e.g., [3]) or empty ([]). After
 * the first non-array type is reached from the root, they must all be
 * constants. Hence there is a prefix of array types which may or may not be
 * complete, followed by types which must be complete.
 */
public class CompoundLiteralAnalyzer {

	// ***************************** Fields *******************************

	/** The entity analyzer controlling this declaration analyzer. */
	private EntityAnalyzer entityAnalyzer;

	private NodeFactory nodeFactory;

	private TypeFactory typeFactory;

	private ValueFactory valueFactory;

	private IntegerType intType;

	// ************************** Constructors ****************************

	public CompoundLiteralAnalyzer(EntityAnalyzer entityAnalyzer) {
		this.entityAnalyzer = entityAnalyzer;
		this.nodeFactory = entityAnalyzer.nodeFactory;
		this.typeFactory = entityAnalyzer.typeFactory;
		this.valueFactory = entityAnalyzer.valueFactory;
		this.intType = (IntegerType) typeFactory.basicType(BasicTypeKind.INT);
	}

	// ************************* Exported Methods *************************

	/**
	 * Analyzes a compound initializer node, which is used to represent either a
	 * compound initializer or a compound literal expression. After performing
	 * the analysis, certain fields are set in the compound initializer node.
	 * 
	 * @param compoundInitNode
	 *                             the compound initializer node to analyze
	 * @param type
	 *                             the type of the expression represented by the
	 *                             compound initializer node, which was obtained
	 *                             by analyzing the type node associated to the
	 *                             compound initializer node
	 * @throws SyntaxException
	 */
	void processCompoundInitializer(CompoundInitializerNode compoundInitNode,
			ObjectType type) throws SyntaxException {
		LiteralTypeNode ltNode = makeTypeTree(type);
		CommonCompoundLiteralObject literalObject = interpret(compoundInitNode,
				ltNode);
		ObjectType completeType = extractType(ltNode);

		fill(literalObject);
		compoundInitNode.setLiteralObject(literalObject);
		compoundInitNode.setType(completeType);
	}

	// ************************* Private Methods **************************

	private SyntaxException error(String message, ASTNode node) {
		return entityAnalyzer.error(message, node);
	}

	private SyntaxException error(UnsourcedException e, ASTNode node) {
		return entityAnalyzer.error(e, node);
	}

	private LiteralTypeNode makeTypeTree(ObjectType type) {
		switch (type.kind()) {
			case ARRAY : {
				LiteralTypeNode child = makeTypeTree(
						((ArrayType) type).getElementType());
				LiteralTypeNode result = new LiteralArrayTypeNode(
						(ArrayType) type, child);

				child.setParent(result);
				return result;
			}
			case STRUCTURE_OR_UNION : {
				StructureOrUnionType sut = (StructureOrUnionType) type;
				int numFields = sut.getNumFields();
				LiteralTypeNode[] children = new LiteralTypeNode[numFields];
				LiteralTypeNode result;

				for (int i = 0; i < numFields; i++) {
					Field field = sut.getField(i);
					ObjectType fieldType = field.getType();
					LiteralTypeNode child = makeTypeTree(fieldType);

					children[i] = child;
				}
				result = new LiteralStructOrUnionTypeNode(sut, children);
				for (int i = 0; i < numFields; i++)
					children[i].setParent(result);
				return result;
			}
			case QUALIFIED : {
				return makeTypeTree(((QualifiedObjectType) type).getBaseType());
			}
			default :
				return new LiteralScalarTypeNode(type);
		}
	}

	/**
	 * Extracts the complete type from the ltNode after it has been refined
	 * through the construction of the literal object.
	 * 
	 * As soon as you hit a non-array type, you can stop, because the fields of
	 * structs or unions have to be complete, except for the last "flexible
	 * member", but that can't be initialized.
	 * 
	 * @param ltNode
	 *                   the literal type node, which has been updated after
	 *                   processing the compound literal
	 * @return the complete Type specified by that node
	 */
	private ObjectType extractType(LiteralTypeNode ltNode) {
		if (ltNode instanceof LiteralArrayTypeNode) {
			LiteralTypeNode child = ((LiteralArrayTypeNode) ltNode)
					.getElementNode();
			ObjectType elementType = extractType(child);
			int length = ltNode.length();

			return typeFactory.arrayType(elementType,
					valueFactory.integerValue(intType, length));
		}
		return ltNode.getType();
	}

	/**
	 * Constructs an abstract Designation from an AST designation node.
	 * 
	 * @param desNode
	 *                    the designation node we are trying to analyze. This
	 *                    node wraps a sequence of DesignatorNode. Each node in
	 *                    the sequence is either an array designator node or a
	 *                    field designator node.
	 * @param ltNode
	 *                    abstract representation of the type of the fixed
	 *                    compound literal node of which the designation is a
	 *                    part. This information is needed to inform the
	 *                    analysis of the designation node.
	 * @return the abstract representation of the given designation node
	 * @throws SyntaxException
	 */
	private Designation processDesignation(DesignationNode desNode,
			LiteralTypeNode ltNode) throws SyntaxException {
		Designation result = new Designation(ltNode);
		Type curTy = ltNode.getType().ignoreQualifiersAtomic();

		for (DesignatorNode designatorNode : desNode) {
			if (designatorNode instanceof FieldDesignatorNode) {
				FieldDesignatorNode fdn = (FieldDesignatorNode) designatorNode;
				IdentifierNode fieldId = fdn.getField();
				String fieldName = fieldId.name();

				if (curTy.kind() != TypeKind.STRUCTURE_OR_UNION)
					throw error(
							"Field designator ." + fieldName
									+ " applied to non-structure/union type "
									+ curTy,
							fieldId);

				StructureOrUnionType suType = (StructureOrUnionType) curTy;
				Field[] navseq = suType.findDeepField(fieldName);

				if (navseq == null)
					throw error(
							"Structure or union type " + suType.getTag()
									+ " contains no field named " + fieldName,
							fieldId);
				fdn.setNavigationSequence(navseq);
				fieldId.setEntity(navseq[navseq.length - 1]);
				for (Field field : navseq)
					result.add(new Navigator(field.getMemberIndex(),
							designatorNode.getSource()));
				curTy = navseq[navseq.length - 1].getType().ignoreQualifiersAtomic();
			} else if (designatorNode instanceof ArrayDesignatorNode) {
				ExpressionNode indexExpr = ((ArrayDesignatorNode) designatorNode)
						.getIndex();

				if (curTy.kind() != TypeKind.ARRAY)
					throw error(
							"Array designator applied to non-array type "
									+ curTy,
							designatorNode);

				entityAnalyzer.expressionAnalyzer.processExpression(indexExpr);

				IntegerValue indexValue = (IntegerValue) nodeFactory
						.getConstantValue(indexExpr);
				int index = indexValue.getIntegerValue().intValue();

				result.add(new Navigator(index, designatorNode.getSource()));
				curTy = ((ArrayType) curTy).getElementType().ignoreQualifiersAtomic();
			} else
				throw new ABCRuntimeException(
						"Unreachable: unknown kind of designator node: "
								+ designatorNode);
		}
		return result;
	}

	private CommonCompoundLiteralObject interpret(
			CompoundInitializerNode compoundInitNode, LiteralTypeNode ltNode)
			throws SyntaxException {
		CommonCompoundLiteralObject result = new CommonCompoundLiteralObject(
				ltNode, compoundInitNode);
		Designation position = new Designation(ltNode);

		for (PairNode<DesignationNode, InitializerNode> pair : compoundInitNode) {
			DesignationNode desNode = pair.getLeft();
			InitializerNode initNode = pair.getRight();
			LiteralObject subLiteral;
			LiteralTypeNode subType;

			if (desNode != null) {
				position = processDesignation(desNode, ltNode);
			} else {
				if (position.length() == 0)
					position.add(new Navigator(0, initNode.getSource()));
				else
					position.increment(ltNode);
			}
			if (initNode instanceof CompoundInitializerNode) {
				CompoundInitializerNode ciNode = (CompoundInitializerNode) initNode;
				
				subType = position.getDesignatedType();
				subLiteral = interpret(ciNode, subType);
				ciNode.setType(extractType(subType));
			} else {
				ExpressionNode expr = (ExpressionNode) initNode;

				entityAnalyzer.expressionAnalyzer.processExpression(expr);
				subType = null;
				for (int i = 0; i < 2; i++) { // make at most 2 attempts:
					try {
						/*
						 * if an exception gets thrown, the current object
						 * cannot be assigned by the initializer, which means
						 * either there is an error or it is the sub-object that
						 * should be initialized. Hence, call "descendToType"
						 * and try again.
						 */
						subType = position.getDesignatedType();
						entityAnalyzer.expressionAnalyzer
								.processAssignment(subType.getType(), expr);
					} catch (UnsourcedException e) {
						if (i == 0) { // i == 0 means looking at sub-obj
							position.descendToType((ObjectType) expr.getType(),
									initNode.getSource());
							continue;
						} else
							throw error(e, expr);
					}
					break;
				}
				assert subType != null;
				subLiteral = new CommonScalarLiteralObject(subType, expr);
			}
			result.set(compoundInitNode.getSource(), position, subLiteral);
		}
		return result;
	}

	/**
	 * Fills in missing spaces with 0s. Needs to create fake expression nodes
	 * for the 0s. Creates them with source the entire surrounding compound
	 * initializer.
	 * 
	 * @param object
	 *                   compound literal object that has already been processed
	 * @throws SyntaxException
	 */
	private void fill(CommonCompoundLiteralObject object)
			throws SyntaxException {
		// for proper sourcing, need a node for each compound
		// literal object, or source
		LiteralTypeNode ltNode = object.getTypeNode();
		int length = ltNode.length();
		ASTNode sourceNode = object.getSourceNode();
		Source source = sourceNode.getSource();
		ObjectType type = ltNode.getType();
		boolean isUnion = type.kind() == TypeKind.STRUCTURE_OR_UNION
				&& ((StructureOrUnionType) type).isUnion();
		boolean hasNonNullMember = false;

		for (int i = 0; (!hasNonNullMember) && i < length; i++)
			if (object.get(i) != null)
				hasNonNullMember = true;
		for (int i = 0; i < length; i++) {
			LiteralObject member = object.get(i);

			// if all members of a union are null then the first
			// member should be 0ed and filled
			if (member == null && (!isUnion || (i == 0 && !hasNonNullMember))) {
				// what is the type of this member supposed to be?
				LiteralTypeNode child = ltNode.getChild(i);

				if (child instanceof LiteralScalarTypeNode) {
					ExpressionNode fakeNode = nodeFactory
							.newIntegerConstantNode(source, "0");

					try {
						entityAnalyzer.expressionAnalyzer
								.processAssignment(child.getType(), fakeNode);
					} catch (UnsourcedException e) {
						throw error(e, sourceNode);
					}
					member = new CommonScalarLiteralObject(child, fakeNode);
				} else {
					member = new CommonCompoundLiteralObject(child, sourceNode);
				}
				object.setElement(source, i, member);
			}
			if (member != null
					&& member instanceof CommonCompoundLiteralObject) {
				fill((CommonCompoundLiteralObject) member);
			}
		}
	}
}
