package dev.civl.abc.ast.node.common;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.AttributeKey;
import dev.civl.abc.ast.node.IF.GenericAssociationNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.PragmaNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.StaticAssertionNode;
import dev.civl.abc.ast.node.IF.acsl.AllocationNode;
import dev.civl.abc.ast.node.IF.acsl.AnyactNode;
import dev.civl.abc.ast.node.IF.acsl.AssignsOrReadsNode;
import dev.civl.abc.ast.node.IF.acsl.AssumesNode;
import dev.civl.abc.ast.node.IF.acsl.BehaviorNode;
import dev.civl.abc.ast.node.IF.acsl.CallEventNode;
import dev.civl.abc.ast.node.IF.acsl.CompletenessNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode;
import dev.civl.abc.ast.node.IF.acsl.CompositeEventNode.EventOperator;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.DependsEventNode;
import dev.civl.abc.ast.node.IF.acsl.DependsNode;
import dev.civl.abc.ast.node.IF.acsl.EnsuresNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.acsl.ExtendedQuantifiedExpressionNode.ExtendedQuantifier;
import dev.civl.abc.ast.node.IF.acsl.FocusAssertTransformNode;
import dev.civl.abc.ast.node.IF.acsl.FocusLoopTransformNode;
import dev.civl.abc.ast.node.IF.acsl.FocusOrderedTransformNode;
import dev.civl.abc.ast.node.IF.acsl.GuardsNode;
import dev.civl.abc.ast.node.IF.acsl.InsertTransformNode;
import dev.civl.abc.ast.node.IF.acsl.InvariantNode;
import dev.civl.abc.ast.node.IF.acsl.MemoryEventNode;
import dev.civl.abc.ast.node.IF.acsl.MemoryEventNode.MemoryEventNodeKind;
import dev.civl.abc.ast.node.IF.acsl.NoactNode;
import dev.civl.abc.ast.node.IF.acsl.NothingNode;
import dev.civl.abc.ast.node.IF.acsl.ObjectOrRegionOfNode;
import dev.civl.abc.ast.node.IF.acsl.PredicateNode;
import dev.civl.abc.ast.node.IF.acsl.PureNode;
import dev.civl.abc.ast.node.IF.acsl.RequiresNode;
import dev.civl.abc.ast.node.IF.compound.ArrayDesignatorNode;
import dev.civl.abc.ast.node.IF.compound.CompoundInitializerNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.compound.DesignatorNode;
import dev.civl.abc.ast.node.IF.compound.FieldDesignatorNode;
import dev.civl.abc.ast.node.IF.declaration.AbstractFunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.InitializerNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.AlignOfNode;
import dev.civl.abc.ast.node.IF.expression.ArrayLambdaNode;
import dev.civl.abc.ast.node.IF.expression.ArrowNode;
import dev.civl.abc.ast.node.IF.expression.CastNode;
import dev.civl.abc.ast.node.IF.expression.CharacterConstantNode;
import dev.civl.abc.ast.node.IF.expression.CompoundLiteralNode;
import dev.civl.abc.ast.node.IF.expression.ConstantNode;
import dev.civl.abc.ast.node.IF.expression.DotNode;
import dev.civl.abc.ast.node.IF.expression.EnumerationConstantNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.FloatingConstantNode;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.GenericSelectionNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.IntegerConstantNode;
import dev.civl.abc.ast.node.IF.expression.LambdaNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode.Quantifier;
import dev.civl.abc.ast.node.IF.expression.RegularRangeNode;
import dev.civl.abc.ast.node.IF.expression.ScopeOfNode;
import dev.civl.abc.ast.node.IF.expression.SizeableNode;
import dev.civl.abc.ast.node.IF.expression.SizeofNode;
import dev.civl.abc.ast.node.IF.expression.SpawnNode;
import dev.civl.abc.ast.node.IF.expression.StatementExpressionNode;
import dev.civl.abc.ast.node.IF.expression.StringLiteralNode;
import dev.civl.abc.ast.node.IF.expression.WildcardNode;
import dev.civl.abc.ast.node.IF.label.LabelNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.omp.OmpAtomicNode.OmpAtomicClause;
import dev.civl.abc.ast.node.IF.omp.OmpDeclarativeNode;
import dev.civl.abc.ast.node.IF.omp.OmpEndNode;
import dev.civl.abc.ast.node.IF.omp.OmpEndNode.OmpEndType;
import dev.civl.abc.ast.node.IF.omp.OmpForNode;
import dev.civl.abc.ast.node.IF.omp.OmpFunctionReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpParallelNode;
import dev.civl.abc.ast.node.IF.omp.OmpReductionNode.OmpReductionOperator;
import dev.civl.abc.ast.node.IF.omp.OmpSimdNode;
import dev.civl.abc.ast.node.IF.omp.OmpSymbolReductionNode;
import dev.civl.abc.ast.node.IF.omp.OmpSyncNode;
import dev.civl.abc.ast.node.IF.omp.OmpSyncNode.OmpSyncNodeKind;
import dev.civl.abc.ast.node.IF.omp.OmpWorksharingNode;
import dev.civl.abc.ast.node.IF.omp.OmpWorksharingNode.OmpWorksharingNodeKind;
import dev.civl.abc.ast.node.IF.statement.AtomicNode;
import dev.civl.abc.ast.node.IF.statement.BlockItemNode;
import dev.civl.abc.ast.node.IF.statement.ChooseStatementNode;
import dev.civl.abc.ast.node.IF.statement.CivlForNode;
import dev.civl.abc.ast.node.IF.statement.CompoundStatementNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.ExpressionStatementNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopInitializerNode;
import dev.civl.abc.ast.node.IF.statement.ForLoopNode;
import dev.civl.abc.ast.node.IF.statement.GotoNode;
import dev.civl.abc.ast.node.IF.statement.IfNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode;
import dev.civl.abc.ast.node.IF.statement.JumpNode.JumpKind;
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode.LoopKind;
import dev.civl.abc.ast.node.IF.statement.ReturnNode;
import dev.civl.abc.ast.node.IF.statement.RunNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.statement.SwitchNode;
import dev.civl.abc.ast.node.IF.statement.WhenNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.AtomicTypeNode;
import dev.civl.abc.ast.node.IF.type.BasicTypeNode;
import dev.civl.abc.ast.node.IF.type.DomainTypeNode;
import dev.civl.abc.ast.node.IF.type.EnumerationTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.LambdaTypeNode;
import dev.civl.abc.ast.node.IF.type.PointerTypeNode;
import dev.civl.abc.ast.node.IF.type.StructureOrUnionTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypedefNameNode;
import dev.civl.abc.ast.node.IF.type.TypeofNode;
import dev.civl.abc.ast.node.common.acsl.CommonAllocationNode;
import dev.civl.abc.ast.node.common.acsl.CommonAnyactNode;
import dev.civl.abc.ast.node.common.acsl.CommonAssignsOrReadsNode;
import dev.civl.abc.ast.node.common.acsl.CommonAssumesNode;
import dev.civl.abc.ast.node.common.acsl.CommonBehaviorNode;
import dev.civl.abc.ast.node.common.acsl.CommonCallEventNode;
import dev.civl.abc.ast.node.common.acsl.CommonCompletenessNode;
import dev.civl.abc.ast.node.common.acsl.CommonCompositeEventNode;
import dev.civl.abc.ast.node.common.acsl.CommonDependsNode;
import dev.civl.abc.ast.node.common.acsl.CommonEnsuresNode;
import dev.civl.abc.ast.node.common.acsl.CommonExtendedQuantifiedExpressionNode;
import dev.civl.abc.ast.node.common.acsl.CommonFocusAssertTransformNode;
import dev.civl.abc.ast.node.common.acsl.CommonFocusLoopTransformNode;
import dev.civl.abc.ast.node.common.acsl.CommonFocusOrderedTransformNode;
import dev.civl.abc.ast.node.common.acsl.CommonGuardNode;
import dev.civl.abc.ast.node.common.acsl.CommonInsertTransformNode;
import dev.civl.abc.ast.node.common.acsl.CommonInvariantNode;
import dev.civl.abc.ast.node.common.acsl.CommonMemoryEventNode;
import dev.civl.abc.ast.node.common.acsl.CommonNoactNode;
import dev.civl.abc.ast.node.common.acsl.CommonNothingNode;
import dev.civl.abc.ast.node.common.acsl.CommonObjectOrRegionOfNode;
import dev.civl.abc.ast.node.common.acsl.CommonPredicateNode;
import dev.civl.abc.ast.node.common.acsl.CommonPureNode;
import dev.civl.abc.ast.node.common.acsl.CommonRequiresNode;
import dev.civl.abc.ast.node.common.compound.CommonArrayDesignatorNode;
import dev.civl.abc.ast.node.common.compound.CommonCompoundInitializerNode;
import dev.civl.abc.ast.node.common.compound.CommonDesignationNode;
import dev.civl.abc.ast.node.common.compound.CommonFieldDesignatorNode;
import dev.civl.abc.ast.node.common.declaration.CommonAbstractFunctionDefinitionNode;
import dev.civl.abc.ast.node.common.declaration.CommonEnumeratorDeclarationNode;
import dev.civl.abc.ast.node.common.declaration.CommonFieldDeclarationNode;
import dev.civl.abc.ast.node.common.declaration.CommonFunctionDeclarationNode;
import dev.civl.abc.ast.node.common.declaration.CommonFunctionDefinitionNode;
import dev.civl.abc.ast.node.common.declaration.CommonTypedefDeclarationNode;
import dev.civl.abc.ast.node.common.declaration.CommonVariableDeclarationNode;
import dev.civl.abc.ast.node.common.expression.CommonAlignOfNode;
import dev.civl.abc.ast.node.common.expression.CommonArrayLambdaNode;
import dev.civl.abc.ast.node.common.expression.CommonArrowNode;
import dev.civl.abc.ast.node.common.expression.CommonCastNode;
import dev.civl.abc.ast.node.common.expression.CommonCharacterConstantNode;
import dev.civl.abc.ast.node.common.expression.CommonCompoundLiteralNode;
import dev.civl.abc.ast.node.common.expression.CommonDotNode;
import dev.civl.abc.ast.node.common.expression.CommonEnumerationConstantNode;
import dev.civl.abc.ast.node.common.expression.CommonExpressionNode;
import dev.civl.abc.ast.node.common.expression.CommonFloatingConstantNode;
import dev.civl.abc.ast.node.common.expression.CommonFunctionCallNode;
import dev.civl.abc.ast.node.common.expression.CommonGenericSelectionNode;
import dev.civl.abc.ast.node.common.expression.CommonHereOrRootNode;
import dev.civl.abc.ast.node.common.expression.CommonIdentifierExpressionNode;
import dev.civl.abc.ast.node.common.expression.CommonIntegerConstantNode;
import dev.civl.abc.ast.node.common.expression.CommonLambdaNode;
import dev.civl.abc.ast.node.common.expression.CommonOperatorNode;
import dev.civl.abc.ast.node.common.expression.CommonProcnullNode;
import dev.civl.abc.ast.node.common.expression.CommonQuantifiedExpressionNode;
import dev.civl.abc.ast.node.common.expression.CommonRegularRangeNode;
import dev.civl.abc.ast.node.common.expression.CommonResultNode;
import dev.civl.abc.ast.node.common.expression.CommonScopeOfNode;
import dev.civl.abc.ast.node.common.expression.CommonSelfNode;
import dev.civl.abc.ast.node.common.expression.CommonSizeofNode;
import dev.civl.abc.ast.node.common.expression.CommonSpawnNode;
import dev.civl.abc.ast.node.common.expression.CommonStatementExpressionNode;
import dev.civl.abc.ast.node.common.expression.CommonStringLiteralNode;
import dev.civl.abc.ast.node.common.expression.CommonWildcardNode;
import dev.civl.abc.ast.node.common.label.CommonOrdinaryLabelNode;
import dev.civl.abc.ast.node.common.label.CommonSwitchLabelNode;
import dev.civl.abc.ast.node.common.omp.CommomOmpAtomicNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpDeclarativeNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpEndNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpForNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpFunctionReductionNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpParallelNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpSimdNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpSymbolReductionNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpSyncNode;
import dev.civl.abc.ast.node.common.omp.CommonOmpWorkshareNode;
import dev.civl.abc.ast.node.common.statement.CommonAtomicNode;
import dev.civl.abc.ast.node.common.statement.CommonChooseStatementNode;
import dev.civl.abc.ast.node.common.statement.CommonCivlForNode;
import dev.civl.abc.ast.node.common.statement.CommonCompoundStatementNode;
import dev.civl.abc.ast.node.common.statement.CommonDeclarationListNode;
import dev.civl.abc.ast.node.common.statement.CommonExpressionStatementNode;
import dev.civl.abc.ast.node.common.statement.CommonForLoopNode;
import dev.civl.abc.ast.node.common.statement.CommonGotoNode;
import dev.civl.abc.ast.node.common.statement.CommonIfNode;
import dev.civl.abc.ast.node.common.statement.CommonJumpNode;
import dev.civl.abc.ast.node.common.statement.CommonLabeledStatementNode;
import dev.civl.abc.ast.node.common.statement.CommonLoopNode;
import dev.civl.abc.ast.node.common.statement.CommonNullStatementNode;
import dev.civl.abc.ast.node.common.statement.CommonReturnNode;
import dev.civl.abc.ast.node.common.statement.CommonRunNode;
import dev.civl.abc.ast.node.common.statement.CommonSwitchNode;
import dev.civl.abc.ast.node.common.statement.CommonWhenNode;
import dev.civl.abc.ast.node.common.type.CommonArrayTypeNode;
import dev.civl.abc.ast.node.common.type.CommonAtomicTypeNode;
import dev.civl.abc.ast.node.common.type.CommonBasicTypeNode;
import dev.civl.abc.ast.node.common.type.CommonDomainTypeNode;
import dev.civl.abc.ast.node.common.type.CommonEnumerationTypeNode;
import dev.civl.abc.ast.node.common.type.CommonFunctionTypeNode;
import dev.civl.abc.ast.node.common.type.CommonMemTypeNode;
import dev.civl.abc.ast.node.common.type.CommonPointerTypeNode;
import dev.civl.abc.ast.node.common.type.CommonRangeTypeNode;
import dev.civl.abc.ast.node.common.type.CommonScopeTypeNode;
import dev.civl.abc.ast.node.common.type.CommonStructureOrUnionTypeNode;
import dev.civl.abc.ast.node.common.type.CommonTypedefNameNode;
import dev.civl.abc.ast.node.common.type.CommonTypeofNode;
import dev.civl.abc.ast.node.common.type.CommonVoidTypeNode;
import dev.civl.abc.ast.type.IF.IntegerType;
import dev.civl.abc.ast.type.IF.ObjectType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.StandardSignedIntegerType.SignedIntKind;
import dev.civl.abc.ast.type.IF.StandardUnsignedIntegerType;
import dev.civl.abc.ast.type.IF.StandardUnsignedIntegerType.UnsignedIntKind;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.CharacterValue;
import dev.civl.abc.ast.value.IF.FloatingValue;
import dev.civl.abc.ast.value.IF.IntegerValue;
import dev.civl.abc.ast.value.IF.StringValue;
import dev.civl.abc.ast.value.IF.Value;
import dev.civl.abc.ast.value.IF.ValueFactory;
import dev.civl.abc.config.IF.Configuration;
import dev.civl.abc.front.c.parse.CivlCParser;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcTokenSequence;
import dev.civl.abc.token.IF.ExecutionCharacter;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

public class CommonNodeFactory implements NodeFactory {

	private int attributeCount = 0;

	private LiteralInterpreter literalInterpreter;

	private ValueFactory valueFactory;

	private TypeFactory typeFactory;

	private StandardUnsignedIntegerType booleanType;

	private ObjectType processType;

	private ObjectType scopeType;

	private Configuration configuration;

	private AttributeKey tempCountKey;

	private AttributeKey civlOmpDependSourceKey;

	private AttributeKey civlOmpDependTargetKey;

	public CommonNodeFactory(Configuration configuration, TypeFactory typeFactory, ValueFactory valueFactory) {
		this.literalInterpreter = new LiteralInterpreter(typeFactory, valueFactory);
		this.typeFactory = typeFactory;
		this.valueFactory = valueFactory;
		this.booleanType = typeFactory.unsignedIntegerType(UnsignedIntKind.BOOL);
		this.processType = typeFactory.processType();
		this.scopeType = typeFactory.scopeType();
		this.configuration = configuration;
		this.tempCountKey = newAttribute("tempCount", Integer.class);
		this.civlOmpDependSourceKey = newAttribute("dependSource", Set.class);
		this.civlOmpDependTargetKey = newAttribute("dependTarget", Set.class);
	}

	@Override
	public AttributeKey getCivlOmpDependKey(Boolean isSource) {
		return isSource ? civlOmpDependSourceKey : civlOmpDependTargetKey;
	}

	@Override
	public ValueFactory getValueFactory() {
		return valueFactory;
	}

	@Override
	public AttributeKey getTempCountKey() {
		return tempCountKey;
	}

	@Override
	public AttributeKey newAttribute(String attributeName, Class<? extends Object> attributeClass) {
		AttributeKey key = new CommonAttributeKey(attributeCount, attributeName, attributeClass);

		attributeCount++;
		return key;
	}

	@Override
	public <T extends ASTNode> SequenceNode<T> newSequenceNode(Source source, String name, List<T> nodes) {
		return new CommonSequenceNode<T>(source, name, nodes);
	}

	@Override
	public <S extends ASTNode, T extends ASTNode> PairNode<S, T> newPairNode(Source source, S node1, T node2) {
		return new CommonPairNode<S, T>(source, node1, node2);
	}

	@Override
	public IdentifierNode newIdentifierNode(Source source, String name) {
		return new CommonIdentifierNode(source, name);
	}

	@Override
	public BasicTypeNode newBasicTypeNode(Source source, BasicTypeKind kind) {
		return new CommonBasicTypeNode(source, kind);
	}

	@Override
	public TypeNode newVoidTypeNode(Source source) {
		return new CommonVoidTypeNode(source);
	}

	@Override
	public EnumerationTypeNode newEnumerationTypeNode(Source source, IdentifierNode tag,
			SequenceNode<EnumeratorDeclarationNode> enumerators) {
		return new CommonEnumerationTypeNode(source, tag, enumerators);
	}

	@Override
	public ArrayTypeNode newArrayTypeNode(Source source, TypeNode elementType, ExpressionNode extent) {
		return new CommonArrayTypeNode(source, elementType, extent);
	}

	@Override
	public AtomicTypeNode newAtomicTypeNode(Source source, TypeNode baseType) {
		return new CommonAtomicTypeNode(source, baseType);
	}

	@Override
	public PointerTypeNode newPointerTypeNode(Source source, TypeNode referencedType) {
		return new CommonPointerTypeNode(source, referencedType);
	}

	@Override
	public StructureOrUnionTypeNode newStructOrUnionTypeNode(Source source, boolean isStruct, IdentifierNode tag,
			SequenceNode<FieldDeclarationNode> structDeclList) {
		return new CommonStructureOrUnionTypeNode(source, isStruct, tag, structDeclList);
	}

	@Override
	public FunctionTypeNode newFunctionTypeNode(Source source, TypeNode returnType,
			SequenceNode<VariableDeclarationNode> formals, boolean hasIdentifierList) {
		return new CommonFunctionTypeNode(source, returnType, formals, hasIdentifierList);
	}

	@Override
	public TypeNode newScopeTypeNode(Source source) {
		return new CommonScopeTypeNode(source);
	}

	@Override
	public TypeNode newRangeTypeNode(Source source) {
		return new CommonRangeTypeNode(source);
	}

	@Override
	public DomainTypeNode newDomainTypeNode(Source source) {
		return new CommonDomainTypeNode(source, null);
	}

	@Override
	public DomainTypeNode newDomainTypeNode(Source source, ExpressionNode dimension) {
		return new CommonDomainTypeNode(source, dimension);
	}

	@Override
	public TypedefNameNode newTypedefNameNode(IdentifierNode name) {
		return new CommonTypedefNameNode(name.getSource(), name);
	}

	@Override
	public CharacterConstantNode newCharacterConstantNode(Source source, String representation,
			ExecutionCharacter character) {
		CharacterValue constant = valueFactory.characterValue(character);

		return new CommonCharacterConstantNode(source, representation, constant);
	}

	@Override
	public StringLiteralNode newStringLiteralNode(Source source, String representation, StringLiteral literal) {
		StringValue stringValue = valueFactory.stringValue(literal);

		return new CommonStringLiteralNode(source, representation, stringValue);
	}

	@Override
	public IntegerConstantNode newIntegerConstantNode(Source source, String representation) throws SyntaxException {
		return literalInterpreter.integerConstant(source, representation);
	}

	@Override
	public IntegerConstantNode newIntConstantNode(Source source, int value) {
		IntegerType type = typeFactory.signedIntegerType(SignedIntKind.INT);
		IntegerValue intValue = valueFactory.integerValue(type, BigInteger.valueOf(value));

		return new CommonIntegerConstantNode(source, String.valueOf(value), intValue);
	}

	@Override
	public FloatingConstantNode newFloatingConstantNode(Source source, String representation, String wholePart,
			String fractionPart, String exponent, FloatingValue value) {
		return new CommonFloatingConstantNode(source, representation, wholePart, fractionPart, exponent, value);
	}

	@Override
	public FloatingConstantNode newFloatingConstantNode(Source source, String representation) throws SyntaxException {
		return literalInterpreter.floatingConstant(source, representation);
	}

	@Override
	public EnumerationConstantNode newEnumerationConstantNode(IdentifierNode name) {
		return new CommonEnumerationConstantNode(name.getSource(), name);
	}

	@Override
	public CompoundLiteralNode newCompoundLiteralNode(Source source, TypeNode typeNode,
			CompoundInitializerNode initializerList) {
		return new CommonCompoundLiteralNode(source, typeNode, initializerList);
	}

	@Override
	public IdentifierExpressionNode newIdentifierExpressionNode(Source source, IdentifierNode identifier) {
		return new CommonIdentifierExpressionNode(source, identifier);
	}

	@Override
	public AlignOfNode newAlignOfNode(Source source, TypeNode type) {
		return new CommonAlignOfNode(source, type);
	}

	@Override
	public CastNode newCastNode(Source source, TypeNode type, ExpressionNode argument) {
		return new CommonCastNode(source, type, argument);
	}

	@Override
	public FunctionCallNode newFunctionCallNode(Source source, ExpressionNode function,
			List<ExpressionNode> arguments) {
		SequenceNode<ExpressionNode> argumentSequenceNode = newSequenceNode(source, "ActualParameterList", arguments);

		return new CommonFunctionCallNode(source, function, null, argumentSequenceNode);
	}

	@Override
	public GenericSelectionNode newGenericSelectionNode(Source source, ExpressionNode controllingExpression,
			ExpressionNode defaultExpression, SequenceNode<GenericAssociationNode> genericAssociationList) {
		return new CommonGenericSelectionNode(source, controllingExpression, defaultExpression, genericAssociationList);
	}

	@Override
	public GenericAssociationNode newGenericAssociationNode(Source source, TypeNode typeLabel,
			ExpressionNode associatedExpression) {
		return new CommonGenericAssociationNode(source, typeLabel, associatedExpression);
	}

	@Override
	public FunctionCallNode newContextFunctionCallNode(Source source, ExpressionNode function,
			List<ExpressionNode> contextArguments, List<ExpressionNode> arguments) {
		SequenceNode<ExpressionNode> contextArgumentSequenceNode = newSequenceNode(source, "ActualContextParameterList",
				contextArguments);
		SequenceNode<ExpressionNode> argumentSequenceNode = newSequenceNode(source, "ActualParameterList", arguments);

		return new CommonFunctionCallNode(source, function, contextArgumentSequenceNode, argumentSequenceNode);
	}

	@Override
	public DotNode newDotNode(Source source, ExpressionNode structure, IdentifierNode fieldName) {
		return new CommonDotNode(source, structure, fieldName);
	}

	@Override
	public ArrowNode newArrowNode(Source source, ExpressionNode structurePointer, IdentifierNode fieldName) {
		return new CommonArrowNode(source, structurePointer, fieldName);
	}

	@Override
	public OperatorNode newOperatorNode(Source source, Operator operator, List<ExpressionNode> arguments) {
		return new CommonOperatorNode(source, operator, arguments);
	}

	@Override
	public SizeofNode newSizeofNode(Source source, SizeableNode argument) {
		return new CommonSizeofNode(source, argument);
	}

	@Override
	public ScopeOfNode newScopeOfNode(Source source, ExpressionNode argument) {
		return new CommonScopeOfNode(source, argument);
	}

	@Override
	public VariableDeclarationNode newVariableDeclarationNode(Source source, IdentifierNode name, TypeNode type) {
		return new CommonVariableDeclarationNode(source, name, type);
	}

	@Override
	public VariableDeclarationNode newVariableDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			InitializerNode initializer) {
		return new CommonVariableDeclarationNode(source, name, type, initializer);
	}

	@Override
	public FunctionDeclarationNode newFunctionDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			SequenceNode<ContractNode> contract) {
		return new CommonFunctionDeclarationNode(source, name, type, contract);
	}

	@Override
	public EnumeratorDeclarationNode newEnumeratorDeclarationNode(Source source, IdentifierNode name,
			ExpressionNode value) {
		return new CommonEnumeratorDeclarationNode(source, name, value);
	}

	@Override
	public FieldDeclarationNode newFieldDeclarationNode(Source source, IdentifierNode name, TypeNode type) {
		return new CommonFieldDeclarationNode(source, name, type);
	}

	@Override
	public FieldDeclarationNode newFieldDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			ExpressionNode bitFieldWidth) {
		return new CommonFieldDeclarationNode(source, name, type, bitFieldWidth);
	}

	@Override
	public OrdinaryLabelNode newStandardLabelDeclarationNode(Source source, IdentifierNode name,
			StatementNode statement) {
		CommonOrdinaryLabelNode label = new CommonOrdinaryLabelNode(source, name);

		label.setStatement(statement);
		return label;
	}

	@Override
	public SwitchLabelNode newCaseLabelDeclarationNode(Source source, ExpressionNode constantExpression,
			StatementNode statement) {
		CommonSwitchLabelNode label = new CommonSwitchLabelNode(source, constantExpression);

		label.setStatement(statement);
		return label;
	}

	@Override
	public SwitchLabelNode newDefaultLabelDeclarationNode(Source source, StatementNode statement) {
		CommonSwitchLabelNode label = new CommonSwitchLabelNode(source);

		label.setStatement(statement);
		return label;
	}

	@Override
	public TypedefDeclarationNode newTypedefDeclarationNode(Source source, IdentifierNode name, TypeNode type) {
		return new CommonTypedefDeclarationNode(source, name, type);
	}

	@Override
	public CompoundInitializerNode newCompoundInitializerNode(Source source,
			List<PairNode<DesignationNode, InitializerNode>> initList) {
		return new CommonCompoundInitializerNode(source, initList);
	}

	@Override
	public DesignationNode newDesignationNode(Source source, List<DesignatorNode> designators) {
		return new CommonDesignationNode(source, designators);
	}

	@Override
	public FieldDesignatorNode newFieldDesignatorNode(Source source, IdentifierNode name) {
		return new CommonFieldDesignatorNode(source, name);
	}

	@Override
	public ArrayDesignatorNode newArrayDesignatorNode(Source source, ExpressionNode index) {
		return new CommonArrayDesignatorNode(source, index);
	}

	@Override
	public CompoundStatementNode newCompoundStatementNode(Source source, List<BlockItemNode> items) {
		return new CommonCompoundStatementNode(source, items);
	}

	@Override
	public ExpressionStatementNode newExpressionStatementNode(ExpressionNode expression) {
		return new CommonExpressionStatementNode(expression.getSource(), expression);
	}

	@Override
	public StatementNode newNullStatementNode(Source source) {
		return new CommonNullStatementNode(source);
	}

	@Override
	public ForLoopNode newForLoopNode(Source source, ForLoopInitializerNode initializer, ExpressionNode condition,
			ExpressionNode incrementer, StatementNode body, SequenceNode<ContractNode> contracts) {
		return new CommonForLoopNode(source, condition, body, initializer, incrementer, contracts);
	}

	@Override
	public DeclarationListNode newForLoopInitializerNode(Source source, List<VariableDeclarationNode> declarations) {
		return new CommonDeclarationListNode(source, declarations);
	}

	@Override
	public LoopNode newWhileLoopNode(Source source, ExpressionNode condition, StatementNode body,
			SequenceNode<ContractNode> contracts) {
		return new CommonLoopNode(source, LoopKind.WHILE, condition, body, contracts);
	}

	@Override
	public LoopNode newDoLoopNode(Source source, ExpressionNode condition, StatementNode body,
			SequenceNode<ContractNode> contracts) {
		return new CommonLoopNode(source, LoopKind.DO_WHILE, condition, body, contracts);
	}

	@Override
	public GotoNode newGotoNode(Source source, IdentifierNode label) {
		return new CommonGotoNode(source, label);
	}

	@Override
	public IfNode newIfNode(Source source, ExpressionNode condition, StatementNode trueBranch) {
		return new CommonIfNode(source, condition, trueBranch);
	}

	@Override
	public IfNode newIfNode(Source source, ExpressionNode condition, StatementNode trueBranch,
			StatementNode falseBranch) {
		return new CommonIfNode(source, condition, trueBranch, falseBranch);
	}

	@Override
	public JumpNode newContinueNode(Source source) {
		return new CommonJumpNode(source, JumpKind.CONTINUE);
	}

	@Override
	public JumpNode newBreakNode(Source source) {
		return new CommonJumpNode(source, JumpKind.BREAK);
	}

	@Override
	public ReturnNode newReturnNode(Source source, ExpressionNode argument) {
		return new CommonReturnNode(source, argument);
	}

	@Override
	public LabeledStatementNode newLabeledStatementNode(Source source, LabelNode label, StatementNode statement) {
		return new CommonLabeledStatementNode(source, label, statement);
	}

	@Override
	public SwitchNode newSwitchNode(Source source, ExpressionNode condition, StatementNode body) {
		CommonSwitchNode switchNode = new CommonSwitchNode(source, condition, body);

		return switchNode;
	}

	@Override
	public CivlForNode newCivlForNode(Source source, boolean isParallel, DeclarationListNode variables,
			ExpressionNode domain, StatementNode body, SequenceNode<ContractNode> loopContracts) {
		return new CommonCivlForNode(source, isParallel, variables, domain, body, loopContracts);
	}

	@Override
	public StaticAssertionNode newStaticAssertionNode(Source source, ExpressionNode expression,
			StringLiteralNode message) {
		return new CommonStaticAssertionNode(source, expression, message);
	}

	@Override
	public PragmaNode newPragmaNode(Source source, IdentifierNode identifier, CivlcTokenSequence producer,
			CivlcToken newlineToken) {
		newlineToken.setType(CivlCParser.EOF);
		return new CommonPragmaNode(source, identifier, producer, newlineToken);
	}

	@Override
	public FunctionDefinitionNode newFunctionDefinitionNode(Source source, IdentifierNode name, FunctionTypeNode type,
			SequenceNode<ContractNode> contract, CompoundStatementNode body) {
		return new CommonFunctionDefinitionNode(source, name, type, contract, body);
	}

	@Override
	public AbstractFunctionDefinitionNode newAbstractFunctionDefinitionNode(Source source, IdentifierNode name,
			TypeNode type, SequenceNode<ContractNode> contract, int continuity,
			SequenceNode<PairNode<ExpressionNode, ExpressionNode>> intervals, StringLiteralNode attr) {
		return new CommonAbstractFunctionDefinitionNode(source, name, type, contract, continuity, intervals, attr);
	}

	@Override
	public SequenceNode<BlockItemNode> newTranslationUnitNode(Source source, List<BlockItemNode> definitions) {
		return newSequenceNode(source, "TranslationUnit", definitions);
	}

	@Override
	public SequenceNode<BlockItemNode> newProgramNode(Source source, List<BlockItemNode> definitions) {
		return newSequenceNode(source, "Program", definitions);
	}

	@Override
	public Value getConstantValue(ExpressionNode expression) throws SyntaxException {
		CommonExpressionNode commonNode = (CommonExpressionNode) expression;

		if (commonNode.constantComputed()) {
			return commonNode.getConstantValue();
		} else {
			Value value = valueFactory.evaluate(expression);

			commonNode.setConstantValue(value);
			return value;
		}
	}

	@Override
	public SpawnNode newSpawnNode(Source source, FunctionCallNode callNode) {
		return new CommonSpawnNode(source, callNode);
	}

	@Override
	public ScopeOfNode newScopeOfNode(Source source, IdentifierExpressionNode variableExpression) {
		return new CommonScopeOfNode(source, variableExpression);
	}

	@Override
	public WhenNode newWhenNode(Source source, ExpressionNode guard, StatementNode body) {
		return new CommonWhenNode(source, guard, body);
	}

	@Override
	public ChooseStatementNode newChooseStatementNode(Source source, List<StatementNode> statements) {
		return new CommonChooseStatementNode(source, statements);
	}

	@Override
	public ConstantNode newBooleanConstantNode(Source source, boolean value) {
		IntegerValue theValue;
		String representation;

		if (value) {
			representation = "\\true";
			theValue = valueFactory.integerValue(booleanType, 1);
		} else {
			representation = "\\false";
			theValue = valueFactory.integerValue(booleanType, 0);
		}
		return new CommonIntegerConstantNode(source, representation, theValue);
	}

	@Override
	public ExpressionNode newSelfNode(Source source) {
		ExpressionNode result = new CommonSelfNode(source, processType);

		result.setInitialType(processType);
		return result;
	}

	@Override
	public ExpressionNode newHereNode(Source source) {
		ExpressionNode result = new CommonHereOrRootNode(source, "$here", scopeType);

		result.setInitialType(scopeType);
		return result;
	}

	@Override
	public ExpressionNode newRootNode(Source source) {
		ExpressionNode result = new CommonHereOrRootNode(source, "$root", scopeType);

		result.setInitialType(scopeType);
		return result;
	}

	@Override
	public ExpressionNode newResultNode(Source source) {
		return new CommonResultNode(source);
	}

	@Override
	public RequiresNode newRequiresNode(Source source, ExpressionNode expression) {
		return new CommonRequiresNode(source, expression);
	}

	@Override
	public EnsuresNode newEnsuresNode(Source source, ExpressionNode expression) {
		return new CommonEnsuresNode(source, expression);
	}

	@Override
	public void setConstantValue(ExpressionNode expression, Value value) {
		((CommonExpressionNode) expression).setConstantValue(value);
	}

	@Override
	public AtomicNode newAtomicStatementNode(Source statementSource, StatementNode body) {
		return new CommonAtomicNode(statementSource, body);
	}

	@Override
	public ExpressionNode newProcnullNode(Source source) {
		ExpressionNode result = new CommonProcnullNode(source, processType);

		result.setInitialType(processType);
		return result;
	}

	/* *************************** OpenMP Section ************************** */

	@Override
	public OmpParallelNode newOmpParallelNode(Source source, StatementNode statement) {
		return new CommonOmpParallelNode(source, statement);
	}

	@Override
	public OmpForNode newOmpForNode(Source source, StatementNode statement) {
		return new CommonOmpForNode(source, statement);
	}

	@Override
	public OmpSyncNode newOmpMasterNode(Source source, StatementNode statement) {
		return new CommonOmpSyncNode(source, OmpSyncNodeKind.MASTER, statement);
	}

	@Override
	public OmpSyncNode newOmpAtomicNode(Source source, StatementNode statement, OmpAtomicClause clause,
			boolean seqConsistent) {
		return new CommomOmpAtomicNode(source, statement, clause, seqConsistent);
	}

	@Override
	public OmpSyncNode newOmpBarrierNode(Source source) {
		return new CommonOmpSyncNode(source, OmpSyncNodeKind.BARRIER, null);
	}

	@Override
	public OmpWorksharingNode newOmpSectionsNode(Source source, StatementNode statement) {
		return new CommonOmpWorkshareNode(source, OmpWorksharingNodeKind.SECTIONS, statement);
	}

	@Override
	public OmpWorksharingNode newOmpSectionNode(Source source, StatementNode statement) {
		return new CommonOmpWorkshareNode(source, OmpWorksharingNodeKind.SECTION, statement);
	}

	@Override
	public OmpDeclarativeNode newOmpThreadprivateNode(Source source, SequenceNode<IdentifierExpressionNode> variables) {
		return new CommonOmpDeclarativeNode(source, variables);
	}

	@Override
	public OmpSymbolReductionNode newOmpSymbolReductionNode(Source source, OmpReductionOperator operator,
			SequenceNode<IdentifierExpressionNode> variables) {
		return new CommonOmpSymbolReductionNode(source, operator, variables);
	}

	@Override
	public OmpSyncNode newOmpCriticalNode(Source source, IdentifierNode name, StatementNode statement) {
		OmpSyncNode criticalNode = new CommonOmpSyncNode(source, OmpSyncNodeKind.CRITICAL, statement);

		criticalNode.setCriticalName(name);
		return criticalNode;
	}

	@Override
	public OmpSyncNode newOmpFlushNode(Source source, SequenceNode<IdentifierExpressionNode> variables) {
		OmpSyncNode flushNode = new CommonOmpSyncNode(source, OmpSyncNodeKind.FLUSH, null);

		flushNode.setFlushedList(variables);
		return flushNode;
	}

	@Override
	public OmpSyncNode newOmpFlushNode(Source source) {
		return new CommonOmpSyncNode(source, OmpSyncNodeKind.FLUSH, null);
	}

	@Override
	public OmpSyncNode newOmpOrederedNode(Source source, StatementNode statement) {
		return new CommonOmpSyncNode(source, OmpSyncNodeKind.ORDERED, statement);
	}

	@Override
	public OmpWorksharingNode newOmpSingleNode(Source source, StatementNode statement) {
		return new CommonOmpWorkshareNode(source, OmpWorksharingNodeKind.SINGLE, statement);
	}

	@Override
	public OmpSimdNode newOmpSimdNode(Source source, StatementNode statement) {
		return new CommonOmpSimdNode(source, statement);
	}

	@Override
	public OmpFunctionReductionNode newOmpFunctionReductionNode(Source source, IdentifierExpressionNode function,
			SequenceNode<IdentifierExpressionNode> variables) {
		return new CommonOmpFunctionReductionNode(source, function, variables);
	}

	@Override
	public OmpWorksharingNode newWorksharingNode(Source source, OmpWorksharingNodeKind kind) {
		return new CommonOmpWorkshareNode(source, kind, null);
	}

	@Override
	public OmpEndNode newOmpFortranEndNode(Source source, OmpEndType endType) {
		return new CommonOmpEndNode(source, endType);
	}

	@Override
	public RegularRangeNode newRegularRangeNode(Source source, ExpressionNode low, ExpressionNode high) {
		return new CommonRegularRangeNode(source, low, high);
	}

	@Override
	public RegularRangeNode newRegularRangeNode(Source source, ExpressionNode low, ExpressionNode high,
			ExpressionNode step) {
		return new CommonRegularRangeNode(source, low, high, step);
	}

	@Override
	public OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode argument) {
		return new CommonOperatorNode(source, operator, Arrays.asList(argument));
	}

	@Override
	public OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode arg0, ExpressionNode arg1) {
		return new CommonOperatorNode(source, operator, Arrays.asList(arg0, arg1));
	}

	@Override
	public OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode arg0, ExpressionNode arg1,
			ExpressionNode arg2) {
		return new CommonOperatorNode(source, operator, Arrays.asList(arg0, arg1, arg2));
	}

	@Override
	public DependsNode newDependsNode(Source source, ExpressionNode condition,
			SequenceNode<DependsEventNode> eventList) {
		return new CommonDependsNode(source, eventList);
	}

	@Override
	public GuardsNode newGuardNode(Source source, ExpressionNode expression) {
		return new CommonGuardNode(source, expression);
	}

	@Override
	public AssignsOrReadsNode newAssignsNode(Source source, SequenceNode<ExpressionNode> expressionList) {
		return new CommonAssignsOrReadsNode(source, true, expressionList);
	}

	@Override
	public AssignsOrReadsNode newReadsNode(Source source, SequenceNode<ExpressionNode> expressionList) {
		return new CommonAssignsOrReadsNode(source, false, expressionList);
	}

	@Override
	public WildcardNode newWildcardNode(Source source) {
		return new CommonWildcardNode(source);
	}

	@Override
	public Configuration configuration() {
		return this.configuration;
	}

	@Override
	public StatementExpressionNode newStatementExpressionNode(Source source, CompoundStatementNode statement) {
		return new CommonStatementExpressionNode(source, statement);
	}

	@Override
	public TypeofNode newTypeofNode(Source source, ExpressionNode expression) {
		return new CommonTypeofNode(source, expression);
	}

	@Override
	public MemoryEventNode newMemoryEventNode(Source source, MemoryEventNodeKind kind,
			SequenceNode<ExpressionNode> memoryList) {
		return new CommonMemoryEventNode(source, kind, memoryList);
	}

	@Override
	public CompositeEventNode newOperatorEventNode(Source source, EventOperator op, DependsEventNode left,
			DependsEventNode right) {
		return new CommonCompositeEventNode(source, op, left, right);
	}

	@Override
	public NothingNode newNothingNode(Source source) {
		return new CommonNothingNode(source);
	}

	@Override
	public BehaviorNode newBehaviorNode(Source source, IdentifierNode name, SequenceNode<ContractNode> body) {
		return new CommonBehaviorNode(source, name, body);
	}

	@Override
	public CompletenessNode newCompletenessNode(Source source, boolean isComplete,
			SequenceNode<IdentifierNode> idList) {
		return new CommonCompletenessNode(source, isComplete, idList);
	}

	@Override
	public AssumesNode newAssumesNode(Source source, ExpressionNode predicate) {
		return new CommonAssumesNode(source, predicate);
	}

	@Override
	public NoactNode newNoactNode(Source source) {
		return new CommonNoactNode(source);
	}

	@Override
	public AnyactNode newAnyactNode(Source source) {
		return new CommonAnyactNode(source);
	}

	@Override
	public CallEventNode newCallEventNode(Source source, IdentifierExpressionNode function,
			SequenceNode<ExpressionNode> args) {
		return new CommonCallEventNode(source, function, args);
	}

	@Override
	public PureNode newPureNode(Source source) {
		return new CommonPureNode(source);
	}

	@Override
	public TypeFactory typeFactory() {
		return typeFactory;
	}

	@Override
	public InvariantNode newInvariantNode(Source source, boolean isLoopInvariant, ExpressionNode expression) {
		return new CommonInvariantNode(source, isLoopInvariant, expression);
	}

	@Override
	public ArrayTypeNode newArrayTypeNode(Source source, TypeNode elementType, ExpressionNode extent,
			ExpressionNode startIndex) {
		return new CommonArrayTypeNode(source, elementType, extent, startIndex);
	}

	@Override
	public QuantifiedExpressionNode newQuantifiedExpressionNode(Source source, Quantifier quantifier,
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationList,
			ExpressionNode restriction, ExpressionNode expression) {
		return new CommonQuantifiedExpressionNode(source, quantifier, boundVariableDeclarationList, restriction,
				expression);
	}

	@Override
	public ArrayLambdaNode newArrayLambdaNode(Source source, TypeNode type,
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationList,
			ExpressionNode restriction, ExpressionNode expression) {
		return new CommonArrayLambdaNode(source, type, boundVariableDeclarationList, restriction, expression);
	}

	@Override
	public ArrayLambdaNode newArrayLambdaNode(Source source, TypeNode type,
			List<VariableDeclarationNode> boundVariableDeclarationList, ExpressionNode restriction,
			ExpressionNode expression) {
		List<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> variableList = new LinkedList<>();

		variableList.add(newPairNode(source,
				newSequenceNode(source, "bound variable sub-list", boundVariableDeclarationList), null));
		return new CommonArrayLambdaNode(source, type, newSequenceNode(source, "bound variable list", variableList),
				restriction, expression);
	}

	@Override
	public LambdaNode newLambdaNode(Source source, VariableDeclarationNode freeVariableDeclaration,
			ExpressionNode expression) {
		return new CommonLambdaNode(source, freeVariableDeclaration, expression);
	}

	@Override
	public LambdaNode newLambdaNode(Source source, VariableDeclarationNode boundVariableDeclaration,
			ExpressionNode restriction, ExpressionNode expression) {
		return new CommonLambdaNode(source, boundVariableDeclaration, restriction, expression);
	}

	@Override
	public RunNode newRunNode(Source source, StatementNode statement) {
		return new CommonRunNode(source, statement);
	}

	@Override
	public ObjectOrRegionOfNode newObjectofNode(Source source, ExpressionNode operand) {
		return new CommonObjectOrRegionOfNode(source, true, operand);
	}

	@Override
	public ObjectOrRegionOfNode newRegionofNode(Source source, ExpressionNode operand) {
		return new CommonObjectOrRegionOfNode(source, false, operand);
	}

	@Override
	public AllocationNode newAllocationNode(Source source, boolean isAllocates,
			SequenceNode<ExpressionNode> memoryList) {
		return new CommonAllocationNode(source, isAllocates, memoryList);
	}

	@Override
	public TypeNode newMemTypeNode(Source source) {
		return new CommonMemTypeNode(source);
	}

	@Override
	public ExtendedQuantifiedExpressionNode newExtendedQuantifiedExpressionNode(Source source, ExtendedQuantifier quant,
			ExpressionNode lo, ExpressionNode hi, ExpressionNode function) {
		return new CommonExtendedQuantifiedExpressionNode(source, quant, lo, hi, function);
	}

	@Override
	public LambdaTypeNode newLambdaTypeNode(Source source, TypeNode freeVariableType, TypeNode lambdaFunctionType) {
		return new CommonLambdaTypeNode(source, freeVariableType, lambdaFunctionType);
	}

	@Override
	public PredicateNode newPredicateNode(Source source, IdentifierNode identifier,
			SequenceNode<VariableDeclarationNode> parameters, ExpressionNode body) {
		TypeNode boolType = newBasicTypeNode(source, BasicTypeKind.BOOL);
		FunctionTypeNode predicateTypeNode = newFunctionTypeNode(source, boolType, parameters.copy(), false);
		CompoundStatementNode wrappedBody = newCompoundStatementNode(source,
				Arrays.asList(newReturnNode(source, body)));

		return new CommonPredicateNode(source, predicateTypeNode, identifier, wrappedBody);
	}

	@Override
	public FocusLoopTransformNode newFocusLoopNode(Source source, TokenFactory tokenFactory, String focusTag,
			SequenceNode<ExpressionNode> tagWindow, SequenceNode<ExpressionNode> memoryList) {
		return new CommonFocusLoopTransformNode(source, this, tokenFactory, focusTag, tagWindow, memoryList);
	}

	@Override
	public FocusOrderedTransformNode newFocusOrderedNode(Source source, TokenFactory tokenFactory, String focusTag,
			OperatorNode operator, RegularRangeNode range, ExpressionNode expr) {
		return new CommonFocusOrderedTransformNode(source, this, tokenFactory, focusTag, operator, range, expr);
	}

	@Override
	public FocusAssertTransformNode newFocusAssertNode(Source source, TokenFactory tokenFactory,
			List<String> focusTags) {
		return new CommonFocusAssertTransformNode(source, this, tokenFactory, focusTags);
	}

	@Override
	public InsertTransformNode newInsertTransformNode(Source source, List<BlockItemNode> nodesToInsert,
			boolean insertAfter) {
		return new CommonInsertTransformNode(source, nodesToInsert, insertAfter);
	}
}
