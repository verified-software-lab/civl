package dev.civl.abc.ast.node.IF;

import java.util.List;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTFactory;
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
import dev.civl.abc.ast.node.IF.expression.RemoteOnExpressionNode;
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
import dev.civl.abc.ast.node.IF.statement.LabeledStatementNode;
import dev.civl.abc.ast.node.IF.statement.LoopNode;
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
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.TypeFactory;
import dev.civl.abc.ast.value.IF.FloatingValue;
import dev.civl.abc.ast.value.IF.Value;
import dev.civl.abc.ast.value.IF.ValueFactory;
import dev.civl.abc.config.IF.Configuration;
import dev.civl.abc.token.IF.CivlcToken;
import dev.civl.abc.token.IF.CivlcTokenSequence;
import dev.civl.abc.token.IF.CivlcTokenSource;
import dev.civl.abc.token.IF.ExecutionCharacter;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.StringLiteral;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;

/**
 * <p>
 * The factory used to construct the nodes of Abstract Syntax Trees.
 * </p>
 * 
 * <p>
 * The user constructs the nodes of an AST using the methods in this class.
 * These nodes have the structure of a tree, the root node being the node
 * representing the translation unit or program; the children of the root node
 * correspond to the "external definitions" of the unit. Once these have been
 * constructed, the {@link ASTFactory#newAST} method is invoked on the root node
 * to actually construct the AST object. This performs a number of analyses and
 * stores additional information about the AST. A number of errors can be
 * detected and reported at this stage. Among other things, this also computes
 * the abstract "type" of every variable, function, and expression. It also
 * computes the scope and linkage of all identifiers.
 * </p>
 * 
 * <p>
 * After the AST is created, the AST (and all of its nodes) become immutable.
 * Every node has an "owner" (originally <code>null</code>), which is set to the
 * new AST object at this time. If you want to modify the tree, you must first
 * invoke the {@link AST#release()} method, which frees the nodes from ownership
 * by the AST object, setting the "owner" fields again to <code>null</code>.
 * They can then be modified, and then {@link ASTFactory#newAST} called again to
 * re-analylze and re-build an AST. Alternatively, you can also clone the tree,
 * if you want to keep the old AST around for some reason.
 * </p>
 * 
 * <p>
 * Finally, one or more ASTs can be combined to form a complete "program" using
 * the newProgram method. This corresponds to "linking" in the usual compiler
 * sense. (Not yet implemented.)
 * </p>
 * 
 * @author siegel
 * 
 */
public interface NodeFactory {

	/**
	 * @param isSource if <code>true</code> then returns the {@link AttributeKey} of
	 *                 CIVL OpenMP depend source; otherwise depend target
	 * @return the AttributeKey which stores CIVL OpenMP dependency relationship
	 */
	AttributeKey getCivlOmpDependKey(Boolean isSource);

	/**
	 * @return the AttributeKey which stores the number of temporary variables in an
	 *         AST.
	 */
	AttributeKey getTempCountKey();

	/**
	 * Creates an attribute slot for all AST nodes. This is a mechanism for
	 * extending the functionality of nodes. This may be used to hang any kind of
	 * data onto nodes. Initially, the attribute value associated to the new key
	 * will be null in every node.
	 * 
	 * @param attributeName  a name for the new attribute, unique among all
	 *                       attribute names
	 * @param attributeClass the class to which attribute values of the new kind
	 *                       will belong
	 * @return a new attribute key which can be used to assign attribute values to
	 *         nodes
	 */
	AttributeKey newAttribute(String attributeName, Class<? extends Object> attributeClass);

	/**
	 * Creates a new sequence node, i.e., a node which has some finite ordered
	 * sequence of children belonging to a particular class.
	 * 
	 * @param source source information for the whole sequence
	 * @param name   a name to use when printing this sequence node
	 * @param nodes  a list of nodes that will form the children of the new sequence
	 *               node
	 * @return the new sequence node with the children set
	 */
	<T extends ASTNode> SequenceNode<T> newSequenceNode(Source source, String name, List<T> nodes);

	/**
	 * Creates a new ordered pair node, i.e., a node with exactly two children
	 * belonging to two specific classes.
	 * 
	 * @param node1 the first child node
	 * @param node2 the second child node
	 * @return the new pair node with the children set
	 */
	<S extends ASTNode, T extends ASTNode> PairNode<S, T> newPairNode(Source source, S node1, T node2);

	// Identifiers...

	/**
	 * Constructs and returns a new identifier node with given source object and
	 * name.
	 * 
	 * @param source source information for the identifier use
	 * @param name   the name of this identifier
	 * @return a new identifier node
	 */
	IdentifierNode newIdentifierNode(Source source, String name);

	// Type Nodes ...

	/**
	 * Returns a new type node for a basic type.
	 * 
	 * @param source source information for the occurrence of the basic type
	 * @param kind   the kind of the basic type
	 * @return the new basic type node
	 */
	BasicTypeNode newBasicTypeNode(Source source, BasicTypeKind kind);

	/**
	 * Returns a new void type node.
	 * 
	 * @param source source information for the occurrence of "void"
	 * @return the new void type node
	 */
	TypeNode newVoidTypeNode(Source source);

	/**
	 * Constructs and returns a new enumeration type node.
	 * 
	 * @param source      source information for the occurrence of the enumeration
	 *                    type
	 * @param tag         the enumeration tag, i.e., the name of the enumeration,
	 *                    the string that follows <code>enum</code>
	 * @param enumerators
	 * @return the new enumeration type node
	 */
	EnumerationTypeNode newEnumerationTypeNode(Source source, IdentifierNode tag,
			SequenceNode<EnumeratorDeclarationNode> enumerators);

	/**
	 * Constructs and returns a new array type node.
	 * 
	 * @param source      source information for the occurrence of the array type
	 * @param elementType the node representing the element type
	 * @param extent      the node representing the expression in square brackets,
	 *                    i.e., the array length or "extent"
	 * @return the new array type node
	 */
	ArrayTypeNode newArrayTypeNode(Source source, TypeNode elementType, ExpressionNode extent);

	/**
	 * Constructs and returns a new array type node.
	 * 
	 * @param source      source information for the occurrence of the array type
	 * @param elementType the node representing the element type
	 * @param extent      the node representing the expression in square brackets,
	 *                    i.e., the array length or "extent"
	 * @param startIndex  the node representing the expression in square brackets,
	 *                    i.e, the array starting index
	 * @return the new array type node
	 */
	ArrayTypeNode newArrayTypeNode(Source source, TypeNode elementType, ExpressionNode extent,
			ExpressionNode startIndex);

	/**
	 * Constructs and returns a new atomic type node.
	 * 
	 * @param source   the source information for the occurrence of the atomic type
	 * @param baseType the base type, i.e., the type modified by the "atomic"
	 * @return the new atomic type node
	 */
	AtomicTypeNode newAtomicTypeNode(Source source, TypeNode baseType);

	/**
	 * Constructs and returns a new pointer type node.
	 * 
	 * @param source         source information for the occurrence of the pointer
	 *                       type
	 * @param referencedType the type pointed to
	 * @return the new pointer type node
	 */
	PointerTypeNode newPointerTypeNode(Source source, TypeNode referencedType);

	/**
	 * Constructs and returns a new structure or union type node.
	 * 
	 * @param source         source information for the occurrence of the structure
	 *                       or union type node
	 * @param isStruct       <code>true</code> for a structure type,
	 *                       <code>false</code> for a union type
	 * @param tag            the tag of the structure or union, i.e., the string
	 *                       that follows <code>struct</code> or <code>union</code>.
	 *                       Maybe <code>null</code>.
	 * @param structDeclList the sequence of field declarations; may be
	 *                       <code>null</code>
	 * @return the new structure or union type node
	 */
	StructureOrUnionTypeNode newStructOrUnionTypeNode(Source source, boolean isStruct, IdentifierNode tag,
			SequenceNode<FieldDeclarationNode> structDeclList);

	/**
	 * Constructs and returns a new function type node.
	 * 
	 * @param source            source information for the occurrence of the
	 *                          function type
	 * @param returnType        the node representing the return type of the
	 *                          function type
	 * @param formals           the sequence of formal parameter declaration nodes
	 *                          for the function type. This parameter cannot be
	 *                          null. If there are no formals then it must be a
	 *                          sequence node with an empty list.
	 * @param hasIdentifierList <code>true</code> if the function is declared using
	 *                          an identifier list (i.e., without types associated
	 *                          to the parameters); <code>false</code> if the
	 *                          function is declared with a parameter declaration
	 *                          list
	 * @return the function type node
	 */
	FunctionTypeNode newFunctionTypeNode(Source source, TypeNode returnType,
			SequenceNode<VariableDeclarationNode> formals, boolean hasIdentifierList);

	/**
	 * Returns a new scope type node ("<code>$scope</code>"). This is a CIVL-C type.
	 * 
	 * @param source source information for the occurrence of <code>$scope</code>
	 * @return the new instance of scope type
	 */
	TypeNode newScopeTypeNode(Source source);

	/**
	 * Returns a new mem type node ("<code>$mem</code>"). This is a CIVL-C type.
	 * 
	 * @param source source information for the occurrence of <code>$mem</code>
	 * @return the new instance of mem type node
	 */
	TypeNode newMemTypeNode(Source source);

	/**
	 * Returns a new instance of a typedef name node. This is a use of a typedef
	 * name. The source is the same as that of the identifier name.
	 * 
	 * @param name the identifier node representing the use of the typedef name
	 * @return the new typedef name node wrapping the given identifier node
	 */
	TypedefNameNode newTypedefNameNode(IdentifierNode name);

	/**
	 * Returns a new instance of range type node; this is the CIVL-C type
	 * <code>$range</code>.
	 * 
	 * @param source source information for the occurrence of <code>$domain</code>
	 * @return the new range type node
	 */
	TypeNode newRangeTypeNode(Source source);

	/**
	 * Returns a new instance of domain type node, with no dimension specified; this
	 * is the CIVL-C type <code>$domain</code>.
	 * 
	 * @param source source information for the occurrence of <code>$domain</code>
	 * @return the new domain type node instance
	 */
	DomainTypeNode newDomainTypeNode(Source source);

	LambdaTypeNode newLambdaTypeNode(Source source, TypeNode freeVariableType, TypeNode lambdaFunctionType);

	/**
	 * Returns a new instance of the domain type node with given integer dimension;
	 * this is the CIVL-C type <code>$domain(n)</code>, where <code>n</code> is the
	 * domain dimension. This is a subtype of <code>$domain</code>.
	 * 
	 * @param source    source information for the occurrence of
	 *                  <code>$domain</code>
	 * @param dimension the dimension of the domain, i.e., the arity of the tuples
	 *                  which comprise the domain
	 * @return the new domain type node instance
	 */
	DomainTypeNode newDomainTypeNode(Source source, ExpressionNode dimension);

	// Expressions...

	/**
	 * If the expression can be evaluated statically to yield a constant value, this
	 * method returns that value, else it returns null.
	 * 
	 * Every "constant expression" will yield a (non-null) value, but other
	 * expressions not strictly considered "constant expressions" may also yield
	 * non-null constant values. Hence if method isConstantExpression() returns
	 * true, this method should return a non-null value; if isConstantExpression()
	 * returns false, this method may or may not return a non-null value.
	 * 
	 * @param expression an expression node
	 * @return the constant value obtained by evaluating this expression, or null if
	 *         the expression cannot be evaluated
	 */
	Value getConstantValue(ExpressionNode expression) throws SyntaxException;

	/**
	 * If for some reason you know what the constant value of a node is supposed to
	 * be, tell it by invoking this method.
	 * 
	 * @param expression the expression node that has been determined to have a
	 *                   constant value
	 * @param value      the constant vale to associate to that expression node
	 */
	void setConstantValue(ExpressionNode expression, Value value);

	// Constant and literal expressions ...

	/**
	 * Returns a new character constant node. A character constant is a literal
	 * charcter in a program, something like <code>'a'</code>. C distinguishes
	 * between characters in the source code, and "execution characters" which are
	 * encoded in various ways by source code elements. Unicode characters can all
	 * be encoded using appropriate escape sequences.
	 * 
	 * @param source         the source information for the occurrence of the
	 *                       character constant
	 * @param representation the way the character literal actually appears in the
	 *                       program source code
	 * @param character      the execution character represented by the character
	 *                       constant
	 * @return the new character constant node
	 */
	CharacterConstantNode newCharacterConstantNode(Source source, String representation, ExecutionCharacter character);

	/**
	 * Constructs a new string literal node. A string literal occurs in the program
	 * source code as <code>"..."</code>.
	 * 
	 * @param source         the source information for the occurrence of the string
	 *                       literal. The string literal is usually a single token.
	 * @param representation the way the string literal actually appears in the
	 *                       program source code, with escape sequences intact
	 * @param literal        the string literal object obtained by interpreting the
	 *                       representation
	 * @return the new string literal node
	 */
	StringLiteralNode newStringLiteralNode(Source source, String representation, StringLiteral literal);

	/**
	 * Constructs a new integer constant node. An integer constant is an occurrence
	 * of a literal integer in the source, which encodes a concrete integer value.
	 * The C11 Standard specifies the format for integer constants, which includes
	 * various letter suffixes that can occur at the end of the constant, in Sec.
	 * 6.4.4.1. The integer constant value is constructed by interpreting the
	 * representation.
	 * 
	 * @param source         the source information for the integer constant
	 * @param representation the way the integer actually appears in the program
	 *                       source code
	 * @return the new integer constant node
	 * @throws SyntaxException if the representation does not conform to the format
	 *                         specified in the C11 Standard
	 */
	IntegerConstantNode newIntegerConstantNode(Source source, String representation) throws SyntaxException;

	/**
	 * A special case of {@link #newIntegerConstantNode(Source, String)} where the
	 * 2nd arg is an integer, and the type of this {@link IntegerConstantNode} is
	 * int.
	 * 
	 * @param source The source information for the integer constant
	 * @param value  The value of the integer constant.
	 * @return The new integer constant node
	 */
	IntegerConstantNode newIntConstantNode(Source source, int value);

	/**
	 * Constructs a new floating constant node. A floating constant is an occurrence
	 * of a literal floating point number in the source, which encodes a concrete
	 * floating point value. The C Standard specifies the format for floating
	 * constants, which includes various letter suffixes that can occur at the end
	 * of the constant, in Sec. 6.4.4.2.
	 */
	FloatingConstantNode newFloatingConstantNode(Source source, String representation, String wholePart,
			String fractionPart, String exponent, FloatingValue value);

	/**
	 * Constructs a new floating constant node. A floating constant is an occurrence
	 * of a literal floating point number in the source, which encodes a concrete
	 * floating point value. The C11 Standard specifies the format for floating
	 * constants, which includes various letter suffixes that can occur at the end
	 * of the constant, in Sec. 6.4.4.2. The floating constant value is constructed
	 * by interpreting the representation.
	 * 
	 * @param source         the source information for the floating constant
	 * @param representation the way the floating constant actually appears in the
	 *                       program source code
	 * @return the new floating constant node
	 * @throws SyntaxException if the representation does not conform to the format
	 *                         specified in the C11 Standard
	 */
	FloatingConstantNode newFloatingConstantNode(Source source, String representation) throws SyntaxException;

	/**
	 * Constructs a new enumeration constant node. This represents an occurrence of
	 * an enumeration constant, i.e., a use of a previously declared enumerator, in
	 * the program. This node just wraps an identifier node. The source is same as
	 * that of identifier.
	 * 
	 * @param name the identifier node which is the occurrence of the enumeration
	 *             constant
	 * @return the new enumeration constant node
	 */
	EnumerationConstantNode newEnumerationConstantNode(IdentifierNode name);

	/**
	 * Returns a new compound literal node. A compound literal is a C construct used
	 * to represent a literal array, structure, or union value. Compound literals
	 * are described in the C11 Standard in Secs. 6.5.2.5 and 6.7.9.
	 * 
	 * From Sec. 6.5.2, the syntax is:
	 * 
	 * <pre>
	 * ( type-name ) { initializer-list }
	 * ( type-name ) { initializer-list , }
	 * </pre>
	 * 
	 * and from Sec. 6.7.9:
	 * 
	 * <pre>
	 * initializer:
	 *   assignment-expression
	 *   { initializer-list } { initializer-list , }
	 * 
	 * initializer-list:
	 *   designationopt initializer
	 *   initializer-list , designationopt initializer
	 * 
	 * designation:
	 *   designator-list =
	 * 
	 * designator-list:
	 *   designator
	 *   designator-list designator
	 * 
	 * designator:
	 *   [ constant-expression ]
	 *   . identifier
	 * </pre>
	 * 
	 * 
	 * @param source          source information for the entire compound literal
	 *                        construct
	 * @param typeNode        node representing the type name portion of the
	 *                        compound literal
	 * @param initializerList node representing the initializer list portion of the
	 *                        compound literal
	 * @return the new compound literal node
	 */
	CompoundLiteralNode newCompoundLiteralNode(Source source, TypeNode typeNode,
			CompoundInitializerNode initializerList);

	/**
	 * Constructs a new node representing a CIVL-C boolean constant, "
	 * <code>$true</code>" or "<code>$false</code>".
	 * 
	 * @param source source information for the occurrence of the boolean constant
	 * @param value  <code>true</code> for <code>$true</code>, <code>false</code>
	 *               for <code>$false</code>
	 * @return the new boolean constant node
	 */
	ConstantNode newBooleanConstantNode(Source source, boolean value);

	/**
	 * Constructs a new node representing an occurrence of the CIVL-C "self" process
	 * constant, written "<code>$self</code>".
	 * 
	 * @param source source information for the occurrence of the constant
	 *               <code>$self</code>
	 * @return the new expression node representing the constant
	 */
	ExpressionNode newSelfNode(Source source);

	/**
	 * Constructs a new node representing an occurrence of the CIVL-C "null process"
	 * constant, written <code>$proc_null</code>.
	 * 
	 * @param source source information for the occurrence of the constant
	 *               <code>$proc_null</code>
	 * @return the new expression node representing the constant
	 */
	ExpressionNode newProcnullNode(Source source);

	/**
	 * Constructs a new node representing an occurrence of the CIVL-C expression
	 * "<code>$result</code>", used in function constracts to represent the result
	 * returned by the function.
	 * 
	 * @param source source information for the occurrence of the expression
	 *               <code>$result</code>
	 * @return the new expression node
	 */
	ExpressionNode newResultNode(Source source);

	// Other Expressions...

	/**
	 * Constructs a new identifier expression node. This is an expression node which
	 * just wraps an identifier. Identifiers can be used as expressions in various
	 * ways in C: a variable or the name of a function, for example.
	 * 
	 * The source is not necessarily the same as the identifier because you might
	 * want to include surrounding parentheses in the expression.
	 * 
	 * @param identifier the identifier node being wrapped
	 * @return the new identifier expression node
	 */
	IdentifierExpressionNode newIdentifierExpressionNode(Source source, IdentifierNode identifier);

	/**
	 * Constructs a new "align-of" node. This represents an occurrence of the C11
	 * construct <code>_Alignof(typename)</code>. See C11 Sec. 6.5.3.4. The value is
	 * considered an integer constant, i.e., it is known at compile-time.
	 * 
	 * @param source source information for the occurrence of the expression
	 * @param type   the type name portion of the expression
	 * @return the new align-of node
	 */
	AlignOfNode newAlignOfNode(Source source, TypeNode type);

	/**
	 * Constructs a new cast node. This represents an occurrence of a cast
	 * expression <code>(typename)argument</code>.
	 * 
	 * @param source   source information for the occurrence of the complete cast
	 *                 expression
	 * @param type     node representing the type name
	 * @param argument the argument part of the expression
	 * @return the new cast node
	 */
	CastNode newCastNode(Source source, TypeNode type, ExpressionNode argument);

	/**
	 * Constructs a new function call node. A function call in C is an expression
	 * (with side effects) that has the form <code>f(arg0, arg1, ...)</code>.
	 * 
	 * @param source    source information for the occurrence of the entire function
	 *                  call expression
	 * @param function  the expression of function type which evaluates to the
	 *                  function being called. Typically this is just an identifier
	 *                  expression (naming the function), but it can be a function
	 *                  pointer or any expression evaluating to a function type or
	 *                  pointer to function type
	 * @param arguments the list of actual arguments to be evaluated and passed to
	 *                  the function in this function call
	 * @return the new function call node
	 */
	FunctionCallNode newFunctionCallNode(Source source, ExpressionNode function, List<ExpressionNode> arguments);

	/**
	 * Constructs a new kernel function call node. A kernel function call in Cuda-C
	 * is an expression (with side effects) that has the form
	 * <code>kernelF<<<cArg0, cArg1[, cArg2]>>>(arg0, arg1, ...)</code>. It
	 * represents the enqueueing of a kernel to execute on the Cuda device.
	 * 
	 * @param source           source information for the occurrence of the entire
	 *                         function call expression
	 * @param function         the expression of function type which evaluates to
	 *                         the function being called. Typically this is just an
	 *                         identifier expression (naming the function), but it
	 *                         can be a function pointer or any expression
	 *                         evaluating to a function type or pointer to function
	 *                         type
	 * @param contextArguments the list of arguments passed as the execution context
	 *                         (appearing between <<< and >>>) [It is only for CUDA
	 *                         programs]
	 * @param arguments        the list of actual arguments to be evaluated and
	 *                         passed to the function in this function call
	 * @return the new function call node
	 */
	FunctionCallNode newContextFunctionCallNode(Source source, ExpressionNode function,
			List<ExpressionNode> contextArguments, List<ExpressionNode> arguments);

	/**
	 * Constructs a new generic selection node, used to select an expression to
	 * evaluate at compile time based on the type of the controlling expression.
	 * 
	 * @param source                 source information for the occurrence of the
	 *                               entire generic selection expression
	 * @param controllingExpression  the (unevaluated) expression whose type
	 *                               determines which expression in the association
	 *                               list this generic selection expression
	 *                               evaluates to
	 * @param defaultExpression      the expression that this generic selection
	 *                               evaluates to if the type of the controlling
	 *                               expression is not compatible with any of the
	 *                               types in the association list. This may be
	 *                               null.
	 * @param genericAssociationList the list of (non-default) associations between
	 *                               a typenode and an expression node
	 * @return the new generic selection node
	 */
	GenericSelectionNode newGenericSelectionNode(Source source, ExpressionNode controllingExpression,
			ExpressionNode defaultExpression, SequenceNode<GenericAssociationNode> genericAssociationList);

	/**
	 * Constructs a new generic association node which acts as a binding between a
	 * type node and an expression for the purposes of a generic selection node
	 * 
	 * @param source               source information for the occurrence of this
	 *                             generic association node
	 * @param typeLabel            the type node which acts as a "key" for which a
	 *                             generic selection to determine whether to pick
	 *                             this association or not
	 * @param associatedExpression the expression associated with the type label
	 *                             that, if selected, will be evaluated
	 * @return the new generic association node
	 */
	GenericAssociationNode newGenericAssociationNode(Source source, TypeNode typeLabel,
			ExpressionNode associatedExpression);

	/**
	 * Constructs a new node for a "dot" expression, used in C for structure or
	 * union field navigation, as in <code>myStruct.field</code>.
	 * 
	 * @param source    source information for the occurrence of the entire dot
	 *                  expression
	 * @param structure the expression of either structure or union type
	 * @param fieldName an identifier which is the name of a field in the structure
	 *                  or union
	 * @return the new dot expression node
	 */
	DotNode newDotNode(Source source, ExpressionNode structure, IdentifierNode fieldName);

	/**
	 * Constructs a new node for an "arrow" expression, used in C for structure or
	 * union field navigation starting from a pointer, as in
	 * <code>myStructPtr->field</code>.
	 * 
	 * @param source           source information for the occurrence of the entire
	 *                         arrow expression
	 * @param structurePointer the expression which has type of the form
	 *                         pointer-to-structure or pointer-to-union
	 * @param fieldName        an identifier which is the name of a field in the
	 *                         structure or union
	 * @return the new arrow expression node
	 */
	ArrowNode newArrowNode(Source source, ExpressionNode structurePointer, IdentifierNode fieldName);

	/**
	 * <p>
	 * Constructs a new operator expression node using one of the standard operators
	 * provided in the enumerated type {@link Operator}.
	 * </p>
	 * 
	 * <p>
	 * Some operators are not included in the enumerated type, and instead have
	 * their own special class, because they either need to implement an interface
	 * that not all operator expressions should implement (e.g., because they are
	 * left-hand-side expressions) or because they need to implement some methods
	 * that do not apply to all operator expressions. Hence the operator enumerated
	 * type includes only those operators that can be treated in a single, generic
	 * way.
	 * </p>
	 * 
	 * @param source    source information for the occurrence of the entire operator
	 *                  expression, including the operator itself and its arguments
	 * @param operator  the operator
	 * @param arguments the ordered list of arguments to the operator. For binary
	 *                  operators, the left operand comes first, followed by the
	 *                  right operator
	 * @return the new operator expression node
	 */
	OperatorNode newOperatorNode(Source source, Operator operator, List<ExpressionNode> arguments);

	/**
	 * Convenience method for constructing new unary operator node; equivalent to
	 * invoking {@link #newOperatorNode(Source, Operator, ExpressionNode)} on the
	 * singleton list containing <code>argument</code>.
	 * 
	 * @param source   source information for the occurrence of the entire operator
	 *                 expression, including the operator itself and its arguments
	 * @param operator the unary operator
	 * @param argument the sole argument to the operator
	 * @return the new operator expression node
	 */
	OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode argument);

	/**
	 * Convenience method for constructing new binary operator node; equivalent to
	 * invoking {@link #newOperatorNode(Source, Operator, ExpressionNode)} on the
	 * list consisting of <code>arg0</code> and <code>arg1</code>.
	 * 
	 * @param source   source information for the occurrence of the entire operator
	 *                 expression, including the operator itself and its arguments
	 * @param operator the binary operator
	 * @param arg0     the first argument to the binary operator
	 * @param arg1     the second argument to the binary operator
	 * @return the new operator expression node
	 */
	OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode arg0, ExpressionNode arg1);

	/**
	 * Convenience method for constructing new ternary operator node; equivalent to
	 * invoking {@link #newOperatorNode(Source, Operator, ExpressionNode)} on the
	 * list consisting of <code>arg0</code>, <code>arg1</code>, and
	 * <code>arg2</code>.
	 * 
	 * @param source   source information for the occurrence of the entire operator
	 *                 expression, including the operator itself and its arguments
	 * @param operator the ternary operator
	 * @param arg0     the first argument to the ternary operator
	 * @param arg1     the second argument to the ternary operator
	 * @param arg2     the third argument to the ternary operator
	 * @return the new operator expression node
	 */
	OperatorNode newOperatorNode(Source source, Operator operator, ExpressionNode arg0, ExpressionNode arg1,
			ExpressionNode arg2);

	/**
	 * Constructs a new <code>sizeof</code> expression. This takes one argument,
	 * which can be either a type name or an expression.
	 * 
	 * @param source   source information for the occurrence of the entire
	 *                 <code>sizeof</code> expression, including the argument
	 * @param argument the argument to the <code>sizeof</code> operator
	 * @return the new expression node
	 */
	SizeofNode newSizeofNode(Source source, SizeableNode argument);

	/**
	 * Constructs a new CIVL-C spawn expression. A spawn expression has the form
	 * <code>$spawn</code> followed by a function call expression. Hence a spawn
	 * node has one argument, which is a function call node.
	 * 
	 * @param source   source information for the occurrence of the entire
	 *                 <code>$spawn</code> expression, including the entire function
	 *                 call
	 * @param callNode the function call node
	 * @return the new spawn expression node
	 */
	SpawnNode newSpawnNode(Source source, FunctionCallNode callNode);

	/**
	 * Constructs a remote expression node, representing an expression of the form
	 * <code>proc_expr@x</code>. This refers to a variable in the process
	 * <code>p</code> referenced by the expression <code>proc_expr</code>. The
	 * static variable <code>x</code> can be determined statically now. Later it
	 * will be evaluated in a dynamic state in <code>p</code>'s context.
	 * 
	 * @param source source information for the entire remove expression, including
	 *               both arguments
	 * @param left   the left argument, which is an expression of integer type.
	 * @param right  the right argument, which is a foreign expression that will
	 *               evaluates on the process represented by the left expression.
	 */
	RemoteOnExpressionNode newRemoteOnExpressionNode(Source source, ExpressionNode left, ExpressionNode right);

	/**
	 * Constructs a new CIVL-C <code>$scopeof</code> expression node. This is an
	 * expression which takes one argument, which is a variable expression. It
	 * returns a reference to the dynamic scope (a value of type
	 * <code>$scope</code>) containing the memory unit identified by that variable.
	 * 
	 * @param source             source information for the entire
	 *                           <code>$scopeof</code> expression
	 * @param variableExpression the variable argument
	 * @return the new <code>$scopeof</code> expression
	 */
	ScopeOfNode newScopeOfNode(Source source, IdentifierExpressionNode variableExpression);

	/**
	 * Constructs a new quantified expression.
	 * 
	 * @param source                       The source code information for the
	 *                                     entire expression
	 * @param quantifier                   The quantifier, one of (1)
	 *                                     {@link Quantifier#EXISTS}, the standard
	 *                                     existential quantifier, (2)
	 *                                     {@link Quantifier#FORALL} , the standard
	 *                                     universal quantifier, or (3)
	 *                                     {@link Quantifier#UNIFORM}, the CIVL-C
	 *                                     quantifier representing a uniform
	 *                                     universal condition
	 * @param boundVariableDeclarationList The list of bound variable declarations.
	 * @param restriction                  A boolean-valued expression that holds
	 *                                     true when the quantified variables is in
	 *                                     the domain
	 * @param expression                   The quantified expression.
	 * @return The new quantified expression with the given children.
	 */
	QuantifiedExpressionNode newQuantifiedExpressionNode(Source source, Quantifier quantifier,
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationList,
			ExpressionNode restriction, ExpressionNode expression);

	/**
	 * Constructs a new array lambda expression.
	 * 
	 * @param source                       The source code information for the
	 *                                     entire expression
	 * @param type                         the type of this array lambda
	 * @param boundVariableDeclarationList The list of bound variable declarations.
	 * @param restriction                  A boolean-valued expression that holds
	 *                                     true when the quantified variables is in
	 *                                     the domain
	 * @param expression                   The body-expression
	 * @return The new array lambda expression with the given children.
	 */
	ArrayLambdaNode newArrayLambdaNode(Source source, TypeNode type,
			SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> boundVariableDeclarationList,
			ExpressionNode restriction, ExpressionNode expression);

	/**
	 * Constructs a new array lambda expression.
	 * 
	 * @param source                       The source code information for the
	 *                                     entire expression
	 * @param type                         the type of this array lambda
	 * @param boundVariableDeclarationList The list of bound variable declarations.
	 * @param restriction                  A boolean-valued expression that holds
	 *                                     true when the quantified variables is in
	 *                                     the domain
	 * @param expression                   The body-expression
	 * @return The new array lambda expression with the given children.
	 */
	ArrayLambdaNode newArrayLambdaNode(Source source, TypeNode type,
			List<VariableDeclarationNode> boundVariableDeclarationList, ExpressionNode restriction,
			ExpressionNode expression);

	/**
	 * Constructs a new lambda expression with a free (no restriction on the bound
	 * variable) variable.
	 * 
	 * @param source                   The source code information for the entire
	 *                                 expression
	 * @param boundVariableDeclaration The bound variable declaration.
	 * @param expression               The body-expression
	 * @return The new array lambda expression with the given children.
	 */
	LambdaNode newLambdaNode(Source source, VariableDeclarationNode boundVariableDeclaration,
			ExpressionNode expression);

	/**
	 * Constructs a new lambda expression with restriction on the bound variable.
	 * 
	 * @param source                   The source code information for the entire
	 *                                 expression
	 * @param boundVariableDeclaration The bound variable declaration.
	 * @param restriction              A boolean restriction on the bound variable
	 * @param expression               The body-expression
	 * @return The new array lambda expression with the given children.
	 */
	LambdaNode newLambdaNode(Source source, VariableDeclarationNode boundVariableDeclaration,
			ExpressionNode restriction, ExpressionNode expression);

	/**
	 * 
	 * @param source     The source code elements.
	 * @param quantifier The quantifier. One of {EXISTS, FORALL, UNIFORM}.
	 * @param variable   The quantified variable.
	 * @param lower      Integer-valued expression for the lower bound on the
	 *                   quantified variable.
	 * @param upper      Integer-valued expression for the upper bound on the
	 *                   quantified variable.
	 * @param expression The quantified expression.
	 * @return The new quantified expression with the given children.
	 */
	// QuantifiedExpressionNode newQuantifiedExpressionNode(Source source,
	// Quantifier quantifier, VariableDeclarationNode variable,
	// ExpressionNode lower, ExpressionNode upper,
	// ExpressionNode expression);

	/**
	 * <p>
	 * Constructs a new CIVL-C regular range expression, which has the form
	 * <code>lo .. hi</code>, where <code>lo</code> and <code>hi</code> are integer
	 * expressions. A range expression represents an (ordered) set of integers. This
	 * expression represents the set of integers that are greater than or equal to
	 * <code>lo</code> and less than or equal to <code>hi</code>. The order is from
	 * lowest to highest.
	 * </p>
	 * 
	 * <p>
	 * See
	 * {@link #newRegularRangeNode(Source, ExpressionNode, ExpressionNode, ExpressionNode)}
	 * for the more general expression which permits a "step" to be specified. The
	 * expression returned by this method is equivalent to using a step of 1.
	 * </p>
	 * 
	 * @param source source information for the entire expression
	 * @param low    the lower bound of the range (inclusive)
	 * @param high   the upper bound of the range (inclusive)
	 * @return the new range expression
	 */
	RegularRangeNode newRegularRangeNode(Source source, ExpressionNode low, ExpressionNode high);

	/**
	 * <p>
	 * Constructs a new CIVL-C regular range expression, which has the form
	 * <code>lo .. hi # step</code>, where <code>lo</code>, <code>hi</code>, and
	 * <code>step</code> are all integer expressions.
	 * </p>
	 * 
	 * <p>
	 * A range expression represents an (ordered) set of integers. If
	 * <code>step</code> is positive, this expression represents the set of integers
	 * <code>lo</code>, <code>lo+step</code>, <code>lo+2*step</code>, and so on, up
	 * to and possibly including <code>hi</code>. That is also the order.
	 * </p>
	 * 
	 * <p>
	 * If <code>step</code> is negative, this represents the ordered set of integers
	 * <code>hi</code>, <code>hi+step</code>, <code>hi+2*step</code>, and so on,
	 * down to and possibly including <code>lo</code>. That is also the order.
	 * </p>
	 * 
	 * @param source source information for the entire expression
	 * @param low    the lower bound of the range (inclusive)
	 * @param high   the upper bound of the range (inclusive)
	 * @param step   the step, i.e., the (positive or negative) distance between two
	 *               consecutive elements in the range
	 * @return the new range expression
	 */
	RegularRangeNode newRegularRangeNode(Source source, ExpressionNode low, ExpressionNode high, ExpressionNode step);

	// Declarations...

	/**
	 * Creates a new declaration of an "object" variable with no initializer.
	 * 
	 * @param source the source information for the variable declaration
	 * @param name   the identifier node corresponding to the name of the variable
	 *               in its declaration
	 * @param type   the node corresponding to the type in the declaration
	 * @return the new variable declaration node with the given chidren
	 */
	VariableDeclarationNode newVariableDeclarationNode(Source source, IdentifierNode name, TypeNode type);

	/**
	 * Creates a new declaration for an "object" variable with an initializer.
	 * 
	 * @param name        identifier being declared
	 * @param type        the type
	 * @param initializer optional initializer (for variables only) or null
	 * @return a new declaration for an "ordinary identifier"
	 */
	VariableDeclarationNode newVariableDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			InitializerNode initializer);

	/**
	 * Creates a new function declaration with no body (so it is not a function
	 * "definition").
	 * 
	 * @param source   source information for this declaration
	 * @param name     the identifier node for the name of this function
	 * @param type     node representing the type of the function
	 * @param contract sequence of contract elements or <code>null</code>
	 * @return the new function declaration node formed from given children
	 */
	FunctionDeclarationNode newFunctionDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			SequenceNode<ContractNode> contract);

	/**
	 * Creates new declaration of an enumerator, which is an element inside of a C
	 * <code>enum</code> definition. An enumerator declaration always contains an
	 * identifier, and may or may not contain an optional integer value.
	 * 
	 * @param source source information for the entire enumerator declaration,
	 *               including the value node if present
	 * @param name   the identifier which is the name of the enumerator
	 * @param value  the (optional) value to be assigned to this enumerator; if
	 *               absent, use <code>null</code>
	 * @return the new enumerator declaration
	 */
	EnumeratorDeclarationNode newEnumeratorDeclarationNode(Source source, IdentifierNode name, ExpressionNode value);

	/**
	 * Consructs a new field declaration node. A field declaration occurs inside a
	 * <code>struct</code> or <code>union</code> definition in C. This declaration
	 * is similar to an ordinary variable declaration.
	 * 
	 * @param source source information for the entire field declaration
	 * @param name   the identifier which is the name of the field being declared
	 * @param type   the type of the field
	 * @return the new field declaration node
	 * 
	 * @see {@link #newFieldDeclarationNode(Source, IdentifierNode, TypeNode, ExpressionNode)
	 *      for the more general method which also permits a "bit width" argument,
	 *      an optional C construct
	 */
	FieldDeclarationNode newFieldDeclarationNode(Source source, IdentifierNode name, TypeNode type);

	/**
	 * Consructs a new field declaration node which also includes a "bit width"
	 * argument. A field declaration occurs inside a <code>struct</code> or
	 * <code>union</code> definition in C. This declaration is similar to an
	 * ordinary variable declaration, but may include a bit width parameter.
	 * 
	 * @param source        source information for the entire field declaration
	 * @param name          the identifier which is the name of the field being
	 *                      declared
	 * @param type          the type of the field
	 * @param bitFieldWidth the constant expression of integer type which specifies
	 *                      the number of bits in the field
	 * @return the new field declaration node
	 */
	FieldDeclarationNode newFieldDeclarationNode(Source source, IdentifierNode name, TypeNode type,
			ExpressionNode bitFieldWidth);

	/**
	 * <p>
	 * Creates a new node representing a standard C label. An ordinary label is an
	 * identifier preceding a colon then a statement. It can be used as the target
	 * of a <code>goto</code> statement.
	 * </p>
	 * 
	 * <p>
	 * A label declaration node has one child: an identifier node which is the name
	 * of the label. Note in particular that the statement (following the colon) is
	 * <strong>not</strong> a child of the label declaration node. The declaration
	 * node does contain a reference to that statement, but it is not a child, since
	 * both the label declaration and the statement will be children of a
	 * {@link LabeledStatementNode}. If the statement were a child of the label
	 * declaration node, the AST would not be a tree.
	 * </p>
	 * 
	 * <p>
	 * The standard protocol for constructing a labled statement is as follows:
	 * first, construct the ordinary statement <code>S</code>. Then construct the
	 * label declaration node <code>L</code> using this method, using <code>S</code>
	 * as the <code>statement</code> argument. Finally, create a new
	 * {@link LabeledStatementNode} using method
	 * {@link #newLabeledStatementNode(Source, LabelNode, StatementNode)} with
	 * arguments <code>L</code> and <code>S</code>.
	 * </p>
	 * 
	 * @param source    source information for the label only (not the statement
	 *                  that follows)
	 * @param name      the name of the label
	 * @param statement the statement that follows the label and colon
	 * @return the new label declaration node
	 */
	OrdinaryLabelNode newStandardLabelDeclarationNode(Source source, IdentifierNode name, StatementNode statement);

	/**
	 * Constructs a new case-labeled declaration node. This node represents a C
	 * construct of the form <code>case expr :</code> which precedes a statement
	 * inside a <code>switch</code> statement body.
	 * 
	 * @param source             source information spanning the <code>case</code>
	 *                           and <code>expr</code> tokens
	 * @param constantExpression the expression <code>expr</code> following
	 *                           <code>case</code>; must be a constant expression
	 *                           whose type is consistent with that of the argument
	 *                           to <code>switch</code>
	 * @param statement          the statement following the colon; that statement
	 *                           is <strong>not</strong> made a child of this node
	 * @return the new case-labeled declaration node
	 */
	SwitchLabelNode newCaseLabelDeclarationNode(Source source, ExpressionNode constantExpression,
			StatementNode statement);

	/**
	 * Constructs a new node representing the occurence of a <code>default :</code>
	 * label inside of a <code>switch</code> statement body.
	 * 
	 * @param source    the source information spanning the <code>default</code>
	 *                  token
	 * @param statement the statement following the colon; this is
	 *                  <strong>not</strong> made a child of the new switch label
	 *                  node
	 * @return the new switch label node
	 */
	SwitchLabelNode newDefaultLabelDeclarationNode(Source source, StatementNode statement);

	/**
	 * Constructs a new <code>typedef</code> declaration node. If the typedef was
	 * scope parameterized, the type argument will be a ScopeParameterizedTypeNode.
	 * 
	 * @param source source code reference
	 * @param name   the name of the typedef as an IdentifierNode
	 * @param type   the type node being bound to the identifier (this may be scope
	 *               parameterized)
	 * @return a new typedef declaration node
	 */
	TypedefDeclarationNode newTypedefDeclarationNode(Source source, IdentifierNode name, TypeNode type);

	/**
	 * <p>
	 * Constructs new compound initializer node. A compound initializer in C is used
	 * to initialize an array or structure. It occurs inside curly braces. It
	 * consists of a list of designation-initializer pairs; each designation
	 * represents a "point" inside the structure or array; the initializer specifies
	 * a value to assign to that point. The definition is recursive, since the
	 * initializer in a pair may be a compound initializer.
	 * </p>
	 * 
	 * <p>
	 * A designation in pair may be <code>null</code>. In this case the "point" is
	 * obtained by rules specified in the C Standard, essentially by increment one
	 * past the last point.
	 * </p>
	 * 
	 * @param source   source information spanning the entire compound initializer,
	 *                 from the opening curly brace to the closing curly brace
	 * @param initList the list of designation-initializer pairs.
	 * @return the new compound initializer nodes
	 */
	CompoundInitializerNode newCompoundInitializerNode(Source source,
			List<PairNode<DesignationNode, InitializerNode>> initList);

	/**
	 * Creates a new designation node, which can be used as part of a compound
	 * initializer. A designation consists of a sequence of
	 * <strong>designators</strong>. The designator sequence describes how to
	 * navigate to a particular point inside a complex structure or array.
	 * 
	 * @param source      source information spanning the entire designation
	 * @param designators the sequence of designators
	 * @return the new designation node
	 */
	DesignationNode newDesignationNode(Source source, List<DesignatorNode> designators);

	/**
	 * Constructs a new field designator node. A field designator is a designator
	 * (used as part of a designation, which is part of a compound initializer)
	 * which represents navigation to a particular field in a structure or union. It
	 * essentially wraps a field name.
	 * 
	 * @param source source information spanning this field designator
	 * @param name   the identifier which is the name of the field
	 * @return the new field designator node
	 */
	FieldDesignatorNode newFieldDesignatorNode(Source source, IdentifierNode name);

	/**
	 * Constructs a new array designator node. An array designator is a designator
	 * (used as part of a designation, which is part of a compound initializer)
	 * which represents navigation to a particular element of an array. It
	 * essentially wraps an integer expression, which specifies an index.
	 * 
	 * @param source source information spanning the array designator
	 * @param index  the integer expression specifying an index into the array
	 * @return the new array designator node
	 */
	ArrayDesignatorNode newArrayDesignatorNode(Source source, ExpressionNode index);

	// Statements...

	/**
	 * Constructs a new compound statement node. This is designated in C as
	 * <code>{ s1; s2; ...}</code>, where each <code>s</code>i is a block item
	 * (e.g., a statement, a declaration, or any other instance of
	 * {@link BlockItermNode}.
	 * 
	 * @param source source information encompassing the entire compound statement,
	 *               from the opening curly brace to the closing curly brace
	 * @param items  the list of block items comprising the compound statement
	 * @return the new compound statement node
	 */
	CompoundStatementNode newCompoundStatementNode(Source source, List<BlockItemNode> items);

	/**
	 * Constructs a new expression statement node. This is a statement which wraps
	 * an expression. The source is the same as that of the expression.
	 * 
	 * @param expression the expression node
	 * @return the new expression statement node wrapping that expression
	 */
	ExpressionStatementNode newExpressionStatementNode(ExpressionNode expression);

	/**
	 * Constructs a new node representing a C "null" statement, also known as a
	 * "no-op" statement, and written as just a semicolon.
	 * 
	 * @param source source specification encompassing the single semicolon
	 *               character
	 * @return the new null statement node
	 */
	StatementNode newNullStatementNode(Source source);

	/**
	 * Constructs a new <code>for</code> loop node.
	 * 
	 * @param source      source information for the entire loop construct
	 *                    (including body)
	 * @param initializer the initializer part of the <code>for</code> loop, an
	 *                    {@link Expression} or another instance of
	 *                    {@link ForLoopInitializerNode}, such as one produced from
	 *                    a list of delcarations; may be <code>null</code>
	 * @param condition   the condition part of the <code>for</code> loop
	 * @param incrementer the incrementer part of the <code>for</code> loop
	 * @param body        the body of the <code>for</code> loop
	 * @param invariant   loop invariant: may be <code>null</code>
	 * @return the new <code>for</code> loop node
	 */
	ForLoopNode newForLoopNode(Source source, ForLoopInitializerNode initializer, ExpressionNode condition,
			ExpressionNode incrementer, StatementNode body, SequenceNode<ContractNode> contracts);

	/**
	 * Construcs a new declaration list node, which is comprised of a sequence of
	 * variable declarations. Such a node can be used as the initializer part of a
	 * <code>for</code> loop, or as the variable list part of a CIVL-C
	 * <code>$for</code> or <code>$parfor</code> statement.
	 * 
	 * @param source       source specification encompassing the entire list of
	 *                     declarations
	 * @param declarations list of variable declarations
	 * @return the new declaration list node
	 */
	DeclarationListNode newForLoopInitializerNode(Source source, List<VariableDeclarationNode> declarations);

	/**
	 * Constructs a new node representing a <code>while</code> loop. This is
	 * represented <code>while (cond) body</code> in C, but may also have an
	 * optional CIVL-C loop invariant.
	 * 
	 * @param source    source specification spanning the entire loop and all its
	 *                  components, including the invariant (if present) and the
	 *                  body
	 * @param condition the boolean expression which determines whether control
	 *                  stays in the loop
	 * @param body      the loop body, a statement
	 * @param invariant a boolean expression which is a loop invariant; may be
	 *                  <code>null</code>
	 * @return the new <code>while</code> loop node
	 */
	LoopNode newWhileLoopNode(Source source, ExpressionNode condition, StatementNode body,
			SequenceNode<ContractNode> contracts);

	/**
	 * Constructs a new node representing a <code>do...while</code> loop. This is
	 * represented in C as <code>do body while (cond)</code>. This kind of loop
	 * guarantees that the body will be executed at least once, as the condition is
	 * checked after each execution of the body, rather than before. Otherwise it is
	 * the same as a <code>while</code> loop.
	 * 
	 * @see #newWhileLoopNode(Source, ExpressionNode, StatementNode, ExpressionNode)
	 * @param source    source specification spanning the entire loop, including the
	 *                  body, the invariant, and the condition
	 * @param condition the boolean condition which determines whether control
	 *                  should return to the top of the loop body
	 * @param body      the loop body
	 * @param invariant an optional boolean expression loop invariant which may be
	 *                  associated to this node; may be <code>null</code>
	 * @return the new <code>do</code> loop node
	 */
	LoopNode newDoLoopNode(Source source, ExpressionNode condition, StatementNode body,
			SequenceNode<ContractNode> contracts);

	/**
	 * Constructs a new node representing a <code>goto</code> statement.
	 * 
	 * @param source source specification spanning the whole <code>goto</code>
	 *               statement, including the label
	 * @param label  identifier which is the name of the label to which control
	 *               should "go"
	 * @return the new <code>goto</code> node
	 */
	GotoNode newGotoNode(Source source, IdentifierNode label);

	/**
	 * Creates new <code>if</code> statement node when there is no false ("else")
	 * branch.
	 * 
	 * @param source     source specification spanning the entire statement,
	 *                   including the entire "true" branch
	 * @param condition  the condition expression
	 * @param trueBranch the body of the <code>if</code> statement
	 * @return the new <code>if</code> statement node formed from given children
	 */
	IfNode newIfNode(Source source, ExpressionNode condition, StatementNode trueBranch);

	/**
	 * Creates a new <code>if</code> statement node. False branch may be null if
	 * there is no "else" clause.
	 * 
	 * @param source      source specification spanning the entire statement,
	 *                    including both branches in their entirety
	 * @param condition   the branch condition
	 * @param trueBranch  the statement for the "true" branch
	 * @param falseBranch the statement for the "false" branch
	 * @return the new <code>if</code> statement node
	 */
	IfNode newIfNode(Source source, ExpressionNode condition, StatementNode trueBranch, StatementNode falseBranch);

	/**
	 * Creates a new node representing the C <code>continue</code> statement, used
	 * in a loop body to direct control to the next loop iteration.
	 * 
	 * @param source source specification for the <code>continue</code> token
	 * @return the new new <code>continue</code> node
	 */
	JumpNode newContinueNode(Source source);

	/**
	 * Creates a new node representing the C <code>break</code> statement, used in a
	 * loop or <code>switch</code> body to direct control to the location just after
	 * the loop or <code>switch</code> construct.
	 * 
	 * @param source source specification for the <code>break</code> token
	 * @return the new new <code>break</code> node
	 */
	JumpNode newBreakNode(Source source);

	/**
	 * Creates a new <code>return</code> statement node. Argument may be
	 * <code>null</code>.
	 * 
	 * @param argument the expression being returned or <code>null</code> if there
	 *                 is none
	 * @return the new <code>return</code> statement node
	 */
	ReturnNode newReturnNode(Source source, ExpressionNode argument);

	/**
	 * Constructs new node representing a labeled statement. The label and the
	 * statement being labeled must be constructed first, then used as arguments to
	 * this method.
	 * 
	 * @param source    source specification spanning the entire labeled statement,
	 *                  including the label and the statement being labeled
	 * @param label     the node representing the label
	 * @param statement the statement to be labeled
	 * @return the new labeled statement node
	 * 
	 * @see #newStandardLabelDeclarationNode(Source, IdentifierNode, StatementNode)
	 * @see #newCaseLabelDeclarationNode(Source, ExpressionNode, StatementNode)
	 * @see #newDefaultLabelDeclarationNode(Source, StatementNode)
	 */
	LabeledStatementNode newLabeledStatementNode(Source source, LabelNode label, StatementNode statement);

	/**
	 * Constructs a new node representing a C <code>switch</code> statement. The
	 * <code>switch</code> body must be created first, and this body includes all
	 * the case-labeled statements (and <code>break</code> statements) required. The
	 * <code>condition</code> must have a type appropriate for a <code>swich</code>
	 * statement, such as an integer type.
	 * 
	 * @param source    source specification spanning the entire <code>switch</code>
	 *                  statement, including the entire body
	 * @param condition the expression that is evaluated to determine which case to
	 *                  switch to
	 * @param body      the body of the <code>switch</code> statement
	 * @return the new <code>switch</code> statement node
	 */
	SwitchNode newSwitchNode(Source source, ExpressionNode condition, StatementNode body);

	/**
	 * Creates a new instance of the CIVL <code>$for</code> or <code>$parfor</code>
	 * node.
	 * 
	 * @param source       source information for the entire loop construct
	 *                     (including body)
	 * @param isParallel   if <code>true</code> create a <code>$parfor</code>
	 *                     statement, else create a <code>$for</code> statement
	 * @param variables    the list of loop variables or variable decls
	 * @param domain       the expression of domain type defining the iteration
	 *                     domain; the dimension of the domain must equal the number
	 *                     of loop variables
	 * @param body         the body of the loop statement
	 * @param loopContract optional loop contracts node
	 * @return the new node
	 */
	CivlForNode newCivlForNode(Source source, boolean isParallel, DeclarationListNode variables, ExpressionNode domain,
			StatementNode body, SequenceNode<ContractNode> loopContract);

	/**
	 * Creates a new node representing a CIVL-C <code>$assume</code> statement. This
	 * statement adds an assumption to the current context during execution or
	 * verification of the CIVL model.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$assume</code> statement, including the expression
	 *                   argument
	 * @param expression a boolean expression which is to be evaluated and added to
	 *                   the current context
	 * @return the new <code>$assume</code> statement node
	 */
	// AssumeNode newAssumeNode(Source source, ExpressionNode expression);

	/**
	 * Creates a new node representing a CIVL-C <code>$assert</code> statement.
	 * 
	 * @param source      source specification spanning the entire
	 *                    <code>$assert</code> statement, including the expression
	 *                    argument
	 * @param expression  a boolean expression which is to be evaluated and expected
	 *                    to be true
	 * @param explanation the list of expressions for explaining the assertion if it
	 *                    is violated.
	 * @return the new <code>$assert</code> statement node
	 */
	// AssertNode newAssertNode(Source source, ExpressionNode expression,
	// SequenceNode<ExpressionNode> explanation);

	/**
	 * Creates a new node representing a CIVL-C <code>$when</code> node, used to
	 * represent a guarded command. Such a statement blocks until the guard is
	 * <code>true</code>. Note that only the first atomic sub-statement of the
	 * statement body is guaranteed to execute atomically with the evaluation to
	 * <code>true</code> of the guard.
	 * 
	 * @param source source specification spanning the entire <code>$when</code>
	 *               statement, including the guard and the entire body
	 * @param guard  a boolean expression which determines when the statement is
	 *               enabled
	 * @param body   a statement that may be executed once the guard holds
	 * @return the new <code>$when</code> statement node
	 */
	WhenNode newWhenNode(Source source, ExpressionNode guard, StatementNode body);

	/**
	 * Constructs a new node representing a CIVL-C <code>$choose</code> statement.
	 * This statement has the form
	 * 
	 * <pre>
	 * $choose {
	 *   $when (g1) s1
	 *   $when (g2) s2
	 *   ...
	 * }
	 * </pre>
	 * 
	 * The statement is used to specify a nondeterministic choice. Whenever control
	 * is at the <code>$choose</code> location, the guards <code>g1</code>,
	 * <code>g2</code>, etc., are evaluated. Each that evaluates to true specifies
	 * one enabled transition. If none are enabled, the entire statement blocks. The
	 * order of these clauses is irrelevant.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$choose</code> statement, including the entire body
	 * @param statements the guarded commands which form the clauses of the
	 *                   <code>$choose</code> statement
	 * @return the new <code>$choose</code> statement node
	 */
	ChooseStatementNode newChooseStatementNode(Source source, List<StatementNode> statements);

	// misc. nodes ...

	/**
	 * Creates a new C11 static assertion node. A static assertion is an assertion
	 * which is checked at compile time. The syntax is
	 * 
	 * <pre>
	 * _Static_assert(expr, message)
	 * </pre>
	 * 
	 * @param source     source specification spanning the entire static assertion
	 *                   statement
	 * @param expression the integer constant expression which, if it evaluates to
	 *                   0, yields an assertion violation. Note that the boolean
	 *                   type <code>_Bool</code> is an integer type in C, so a value
	 *                   of this type may be used.
	 * @param message    the message to be printed if the assertion is violated
	 * @return the new static assertion node
	 */
	StaticAssertionNode newStaticAssertionNode(Source source, ExpressionNode expression, StringLiteralNode message);

	/**
	 * Constructs a new pragma node, representing a C <code>#pragma</code>
	 * directive. The pragma is left uninterpreted in this representation. The first
	 * token following the <code>#pragma</code> must be an identifier, this
	 * identifier has a special role as it specifies a pragma domain, such as
	 * <code>omp</code> (for OpenMP). The remainder of the pragma is represented as
	 * a sequence of raw tokens. These can be parsed interpreted at a later stage of
	 * processing. Finally, every pragma must be terminated by the first newline
	 * character encountered, and the token for that newline is also specified.
	 * 
	 * @param source       source specification spanning the entire pragma line,
	 *                     from the <code>#pragma</code> up to and including the
	 *                     <code>newline</code>.
	 * 
	 * @param identifier   the first token after the <code>#pragma</code> token
	 *                     specifying the pragma domain (e.g., <code>omp</code>)
	 * @param producer     a producer for producing new {@link CivlcTokenSource}
	 *                     objects which are essentially iterators over the tokens
	 *                     comprising the body, i.e., the sequence of tokens
	 *                     comprising the rest of the pragma body after the
	 *                     identifier, and not including the newline
	 * @param newlineToken the newlinen token at the end of the pragma
	 * @return the new pragma node
	 */
	PragmaNode newPragmaNode(Source source, IdentifierNode identifier, CivlcTokenSequence producer,
			CivlcToken newlineToken);

	/**
	 * Constructs a new node representing a CIVL-C <code>$requires</code> contract
	 * clause. This is used to specify a pre-condition for a function. It is
	 * currently not used.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$requires</code> clause, including the entire
	 *                   expression
	 * @param expression the boolean expression which specifies a pre-condition
	 * @return the new <code>$requires</code> clause node
	 */
	RequiresNode newRequiresNode(Source source, ExpressionNode expression);

	/**
	 * Constructs a new node representing a CIVL-C <code>$ensures</code> contract
	 * clause. This is used to specify a post-condition for a function. It is
	 * currently not used.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$ensures</code> clause, including the entire
	 *                   expression
	 * @param expression the boolean expression which specifies a post-condition
	 * @return the new <code>$ensures</code> clause node
	 */
	EnsuresNode newEnsuresNode(Source source, ExpressionNode expression);

	/**
	 * Constructs a new node representing a CIVL-C <code>$depends</code> contract
	 * clause. This is used to specify the dependency relationship between processes
	 * for a function.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$depends</code> clause, including the entire
	 *                   expression
	 * @param expression the boolean expression which specifies a condition of
	 *                   dependency
	 * @return the new <code>$depends</code> clause node
	 */
	DependsNode newDependsNode(Source source, ExpressionNode condition, SequenceNode<DependsEventNode> eventList);

	/**
	 * Constructs a new node representing a CIVL-C <code>$guard</code> contract
	 * clause. This is used to specify the guard of a function.
	 * 
	 * @param source     source specification spanning the entire
	 *                   <code>$guard</code> clause, including the entire expression
	 * @param expression the boolean expression which specifies the the guard
	 * @return the new <code>$guard</code> clause node
	 */
	GuardsNode newGuardNode(Source source, ExpressionNode expression);

	/**
	 * Constructs a new node representing an ACSL <code>assigns</code> contract
	 * clause.
	 * 
	 * @param source         source specification spanning the entire
	 *                       <code>$assigns</code> clause, including the entire
	 *                       expression
	 * @param expressionList the expression list which specifies the memory units
	 *                       associated with the <code>assigns</code> clause
	 * @return the new <code>assigns</code> clause node
	 */
	AssignsOrReadsNode newAssignsNode(Source source, SequenceNode<ExpressionNode> expressionList);

	/**
	 * Constructs a new node representing an ACSL <code>reads</code> contract
	 * clause.
	 * 
	 * @param source         source specification spanning the entire
	 *                       <code>reads</code> clause, including the entire
	 *                       expression
	 * @param expressionList the expression list which specifies the memory units
	 *                       associated with the <code>reads</code> clause
	 * @return the new <code>reads</code> clause node
	 */
	AssignsOrReadsNode newReadsNode(Source source, SequenceNode<ExpressionNode> expressionList);

	/**
	 * Constructs a new "pure" node representing the CIVL-extended ACSL "pure;"
	 * clause which may occur as the first line of a function contract, indicating
	 * the function is a pure function (side-effect-free and the output is a
	 * deterministic function of the input).
	 * 
	 * @param source source specifying the clause
	 * @return the new contract node for "pure"
	 */
	PureNode newPureNode(Source source);

	// external definitions...

	/**
	 * Constructs a new node representing a function definition, i.e., a function
	 * declaration with body.
	 * 
	 * @param source   source specification spanning the entire function definition,
	 *                 including the entire function body
	 * @param name     the identifier which is the name of the function being
	 *                 defined
	 * @param type     the type of the function; note that a function type comprises
	 *                 a return type and some sequence of input types
	 * @param contract the (optional) function contract; may be <code>null</code>
	 * @param body     the function body
	 * @return the new function definition node
	 */
	FunctionDefinitionNode newFunctionDefinitionNode(Source source, IdentifierNode name, FunctionTypeNode type,
			SequenceNode<ContractNode> contract, CompoundStatementNode body);

	/**
	 * Creates a new CIVL abstract function definition. An abstract function is an
	 * unspecified mathematical function. In particular, if x1=y1 and ... and xn=yn
	 * then f(x1,...,xn)=f(y1,...,yn). In other words, the value "returned" by f is
	 * a deterministic function of its inputs. In cannot depend on any other
	 * variables values in the program, or other parts of the state. As a special
	 * case, note that if n=0, then f() is essentially a constant. In fact, the case
	 * n=0 is not allowed: if you want a constant, create instead an input variable
	 * (<code>$input</code>).
	 * 
	 * @param source     The source information for the abstract function
	 *                   definition.
	 * @param name       The name of the abstract function.
	 * @param type       The function type with the appropriate parameters and
	 *                   return type.
	 * @param contract   Any code contract associated with the function.
	 * @param continuity The number of derivatives that may be taken; this applies
	 *                   to real valued functions of real variables only
	 * @param intervals  sequence of intervals whose Cartesian product specifies the
	 *                   domain on which this function is differentiable to the
	 *                   specified degree
	 * @param attr       nullable. The optional string literal representing the
	 *                   attribute attached to this abstract function.
	 * @return An abstract function definition with the specified properties.
	 */
	AbstractFunctionDefinitionNode newAbstractFunctionDefinitionNode(Source source, IdentifierNode name, TypeNode type,
			SequenceNode<ContractNode> contract, int continuity,
			SequenceNode<PairNode<ExpressionNode, ExpressionNode>> intervals, StringLiteralNode attr);

	/**
	 * Creates a new node representing an entire translation unit. The children of
	 * this node will be external definitions.
	 * 
	 * @param source      source specification spanning the entire translation unit,
	 *                    which is typically the entire token sequence emanating
	 *                    from the preprocessor
	 * @param definitions the list of external definitions which form the children
	 *                    of the translation unit
	 * @return the new translation unit node
	 */
	SequenceNode<BlockItemNode> newTranslationUnitNode(Source source, List<BlockItemNode> definitions);

	/**
	 * Creates a new node representing an entire program. The children of this node
	 * will be external definitions.
	 * 
	 * @param source      source specification for the whole program; typically a
	 *                    "fake" source
	 * @param definitions the list of external definitions which form the children
	 *                    of the new node; typically obtained by concatenating those
	 *                    from the translation units, perhaps after some
	 *                    modifications
	 * @return the new program node
	 */
	SequenceNode<BlockItemNode> newProgramNode(Source source, List<BlockItemNode> definitions);

	/**
	 * Returns the value factory associated to this node factory. The value factory
	 * is used to reason about constants and constant expressions in a program.
	 * 
	 * @return the value factory
	 */
	ValueFactory getValueFactory();

	/**
	 * Creates a new CIVL-C <code>$atomic</code> statement node.
	 * 
	 * @param statementSource source specification spanning the entire statement,
	 *                        including the body
	 * @param body            The body statement node of the atomic node
	 * @return The new atomic statement node
	 */
	AtomicNode newAtomicStatementNode(Source statementSource, StatementNode body);

	/**
	 * Creates a new constant expression node representing <code>$here</code>.
	 * 
	 * @param source The source code element of the new node
	 * @return a new constant expression node representing <code>$here</code>
	 */
	ExpressionNode newHereNode(Source source);

	/**
	 * Creates a new constant expression node representing <code>$root</code>.
	 * 
	 * @param source The source code element of the new node
	 * @return a new constant expression node representing <code>$root</code>
	 */
	ExpressionNode newRootNode(Source source);

	/**
	 * Creates a new expression node representing <code>$scopeof(expr)</code>.
	 * 
	 * @param source   The source code element of the new node.
	 * @param argument The argument of the <code>$scopeof(expr)</code> expression.
	 * @return a new constant expression node representing <code>$here</code>
	 */
	ScopeOfNode newScopeOfNode(Source source, ExpressionNode argument);

	/**
	 * Creates a new OpenMP parallel node, representing
	 * <code>#pragma omp parallel...</code>. The clauses of the node can be updated
	 * by calling the corresponding setters, e.g, setStatementNode(),
	 * setPrivateList(), etc.
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the parallel construct.
	 * @return The new OpenMP parallel statement node created.
	 */
	OmpParallelNode newOmpParallelNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP for node, representing <code>#pragma omp for...</code>.
	 * The clauses of the node can be updated by calling the corresponding setters,
	 * e.g, setStatementNode(), setPrivateList(), etc.
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the parallel construct.
	 * @return The new OpenMP for node created.
	 */
	OmpForNode newOmpForNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP master node, representing
	 * <code>#pragma omp master...</code>. A master node has exactly one child node,
	 * i.e., the statement node corresponding to the block affected by the master
	 * construct. The syntax of the master construct is:<br>
	 * <code>#pragma omp master new-line <br>
	 * structured-block</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the master construct.
	 * @return The new OpenMP master node created.
	 */
	OmpSyncNode newOmpMasterNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP atomic node, representing
	 * <code>#pragma omp atomic...</code>. An atomic node has exactly one child
	 * node, i.e., the statement node corresponding to the block affected by the
	 * atomic construct. The syntax of the atomic construct is:<br>
	 * <code>#pragma omp atomic new-line <br>
	 * structured-block</code>
	 * 
	 * @param source        The source code element of the new node.
	 * @param statement     The statement node of the master construct.
	 * @param clause        the atomic clause associated with this atomic construct,
	 *                      either "read", "write", "update" or "capture". Absent of
	 *                      clause means "update"
	 * @param seqConsistent true if this atomic construct is sequentially consistent
	 * @return The new OpenMP atomic node created.
	 */
	OmpSyncNode newOmpAtomicNode(Source source, StatementNode statement, OmpAtomicClause clause, boolean seqConsistent);

	/**
	 * Creates a new OpenMP critical node, representing
	 * <code>#pragma omp critical...</code>. A critical node has at most two
	 * children the name of the critical section and the statement node
	 * corresponding to the block affected by the critical construct. The syntax of
	 * the critical construct is:<br>
	 * <code>#pragma omp critical [(name)] new-line <br>
	 * structured-block</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param name      The name of the critical section.
	 * @param statement The statement node of the critical construct.
	 * @return The new OpenMP critical node created.
	 */
	OmpSyncNode newOmpCriticalNode(Source source, IdentifierNode name, StatementNode statement);

	/**
	 * Creates a new OpenMP barrier node, representing
	 * <code>#pragma omp barrier...</code>. A barrier node has NO child node. The
	 * syntax of the barrier construct is:<br>
	 * <code>#pragma omp barrier new-line</code>
	 * 
	 * @param source The source code element of the new node.
	 * @return The new OpenMP barrier node created.
	 */
	OmpSyncNode newOmpBarrierNode(Source source);

	/**
	 * Creates a new OpenMP flush node, representing
	 * <code>#pragma omp flush...</code>. A flush node has at most one child node:
	 * the list of variables of the flush operation. The syntax of the flush
	 * construct is:<br>
	 * <code>#pragma omp flush [(list)] new-line</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param variables The list of variables of the flush operation.
	 * @return The new OpenMP flush node created.
	 */
	OmpSyncNode newOmpFlushNode(Source source, SequenceNode<IdentifierExpressionNode> variables);

	/**
	 * Creates a new OpenMP flush node with no variable list
	 * 
	 * @param source The source code element of the new node.
	 * @return The new OpenMP flush node created.
	 */
	OmpSyncNode newOmpFlushNode(Source source);

	/**
	 * Creates a new OpenMP ordered node, representing
	 * <code>#pragma omp ordered...</code>. An ordered node has exactly one child
	 * node, i.e., the statement node corresponding to the block affected by the
	 * ordered construct. The syntax of the ordered construct is:<br>
	 * <code>#pragma omp ordered new-line<br>
	 * structured-block</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the ordered construct.
	 * @return The new OpenMP ordered node created.
	 */
	OmpSyncNode newOmpOrederedNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP sections node, representing
	 * <code>#pragma omp sections...</code>. The clauses of the node can be updated
	 * by calling the corresponding setters, e.g, setStatementNode(),
	 * setPrivateList(), etc.
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the ordered construct.
	 * @return The new OpenMP sections statement node created.
	 */
	OmpWorksharingNode newOmpSectionsNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP section node, representing
	 * <code>#pragma omp section...</code>. A section node has exactly one child
	 * node, i.e., the statement node corresponding to the block affected by the
	 * section construct. The syntax of the section construct is:<br>
	 * <code>#pragma omp section new-line <br>
	 * structured-block</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the section construct.
	 * @return The new OpenMP section node created.
	 */
	OmpWorksharingNode newOmpSectionNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP single node, representing
	 * <code>#pragma omp single...</code>. The syntax of the single construct is as
	 * follows:<br>
	 * <code>#pragma omp single [clause[[,] clause] ...] new-line<br>
	 * structured-block</code><br>
	 * where clause is one of the following:<br>
	 * <code>private(list) </code><br>
	 * <code>firstprivate(list)  </code><br>
	 * <code>copyprivate(list)</code><br>
	 * <code>nowait</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the section construct.
	 * @return The new OpenMP single node created.
	 */
	OmpWorksharingNode newOmpSingleNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP simd node, representing
	 * <code>#pragma omp simd...</code>. The syntax of the simd directive is as
	 * follows:<br>
	 * <code>#pragma omp simd [clause[[,] clause] ...] new-line<br>
	 * structured-block</code><br>
	 * where clause is one of the following:<br>
	 * <code>safelen</code><br>
	 * <code>simdlen</code><br>
	 * <code>private(list) </code><br>
	 * <code>firstprivate(list)  </code><br>
	 * <code>copyprivate(list)</code><br>
	 * <code>nowait</code>
	 * 
	 * @param source    The source code element of the new node.
	 * @param statement The statement node of the simd construct.
	 * @return The new OpenMP simd node created.
	 */
	OmpSimdNode newOmpSimdNode(Source source, StatementNode statement);

	/**
	 * Creates a new OpenMP threadprivate node.
	 * 
	 * @param source    The source code element of the new node.
	 * @param variables The list of variables declared by the clause.
	 * @return The new OpenMP threadprivate node created.
	 */
	OmpDeclarativeNode newOmpThreadprivateNode(Source source, SequenceNode<IdentifierExpressionNode> variables);

	/**
	 * <strong>FORTRAN ONLY</strong> <br>
	 * Creates a new OpenMP END node with its type, which indicates what previous
	 * OpenMP executable struct should end.
	 * 
	 * @param source  The source code element of the new node.
	 * @param endType The end type of the new END node.
	 * @return The new OpenMP END node created.
	 */
	OmpEndNode newOmpFortranEndNode(Source source, OmpEndType endType);

	/**
	 * Creates a new OpenMP reduction node with a standard operator.
	 * 
	 * @param source    The source code element of the new node.
	 * @param operator  The {@link OmpReductionOperator} of the reduction node.
	 * @param variables The variables of the reduction clause.
	 * @return The new OpenMP reduction node.
	 */
	OmpSymbolReductionNode newOmpSymbolReductionNode(Source source, OmpReductionOperator operator,
			SequenceNode<IdentifierExpressionNode> variables);

	/**
	 * Creates a new OpenMP reduction node with an identifier operator (i.e.,
	 * function names).
	 * 
	 * @param source    The source code element of the new node.
	 * @param function  The name of the function of the reduction node.
	 * @param variables The variables of the reduction clause.
	 * @return The new OpenMP reduction node.
	 */
	OmpFunctionReductionNode newOmpFunctionReductionNode(Source source, IdentifierExpressionNode function,
			SequenceNode<IdentifierExpressionNode> variables);

	/**
	 * Creates a new OpenMP worksharing node with a specific kind. The kind could
	 * be:
	 * <ul>
	 * <li>SECTIONS</li>
	 * <li>SINGLE</li>
	 * <li>SECTION</li>
	 * <li>FOR</li>
	 * </ul>
	 * 
	 * @param source The source code element of the new node.
	 * @param kind   The kind of the worksharing node, either FOR, SECTIONS, SECTION
	 *               or SINGLE.
	 * @return The new OpenMP worksharing node.
	 */
	OmpWorksharingNode newWorksharingNode(Source source, OmpWorksharingNodeKind kind);

	/**
	 * gets the configuration associated with this translation task.
	 * 
	 * @return the configuration associated with this translation task.
	 */
	Configuration configuration();

	/**
	 * creates a new wildcard (<code>...</code>) node.
	 * 
	 * @param source
	 * @return
	 */
	WildcardNode newWildcardNode(Source source);

	/**
	 * creates a new statement expression node (GNU C extension).
	 * 
	 * @param source    the source of the node
	 * @param statement the statement enclosed by the expression excluding the
	 *                  expression at end
	 * @return the new statement expression node (GNU C extension)
	 */
	StatementExpressionNode newStatementExpressionNode(Source source, CompoundStatementNode statement);

	/**
	 * creates a new typeof node (GNU C extension)
	 * 
	 * @param source
	 * @param expression
	 * @return
	 */
	TypeofNode newTypeofNode(Source source, ExpressionNode expression);

	/**
	 * creates a new memory event node, which could be either <code>\read</code> ,
	 * <code>write</code> or <code>reach</code>.
	 * 
	 * @param source
	 * @param kind
	 * @param memoryList
	 * @return
	 */
	MemoryEventNode newMemoryEventNode(Source source, MemoryEventNodeKind kind,
			SequenceNode<ExpressionNode> memoryList);

	/**
	 * creates a new composite event node, which is composed by two events node and
	 * an operator.
	 * 
	 * @param source
	 * @param op
	 * @param left
	 * @param right
	 * @return
	 */
	CompositeEventNode newOperatorEventNode(Source source, EventOperator op, DependsEventNode left,
			DependsEventNode right);

	/**
	 * creates a <code>\nothing</code> node which represents an empty set of memory
	 * units.
	 * 
	 * @param source
	 * @return
	 */
	NothingNode newNothingNode(Source source);

	/**
	 * creates a behavior node. (ACSL contract)
	 * 
	 * @param source
	 * @param name
	 * @param body
	 * @return
	 */
	BehaviorNode newBehaviorNode(Source source, IdentifierNode name, SequenceNode<ContractNode> body);

	/**
	 * creates a completeness clause node, which could be <code>complete</code> or
	 * <code>disjoint</code>
	 * 
	 * @param source
	 * @param isComplete true if to create a complete clause node, otherwise, a
	 *                   disjoint clause node
	 * @param idList
	 * @return
	 */
	CompletenessNode newCompletenessNode(Source source, boolean isComplete, SequenceNode<IdentifierNode> idList);

	/**
	 * Creates a new <code>assumes</code> clause node
	 * 
	 * @param source
	 * @param predicate
	 * @return
	 */
	AssumesNode newAssumesNode(Source source, ExpressionNode predicate);

	/**
	 * Creates a new <code>invariant</code> clause node
	 * 
	 * @param source
	 * @param expression
	 * @return
	 */
	InvariantNode newInvariantNode(Source source, boolean isLoopInvariant, ExpressionNode expression);

	/**
	 * Creates a new <code>\noact</code> event node
	 * 
	 * @param source
	 * @return
	 */
	NoactNode newNoactNode(Source source);

	/**
	 * Creates a new <code>\anyact</code> event node
	 * 
	 * @param source
	 * @return
	 */
	AnyactNode newAnyactNode(Source source);

	/**
	 * Creates a new <code>\call</code> event node
	 * 
	 * @param source
	 * @param function
	 * @param args
	 * @return
	 */
	CallEventNode newCallEventNode(Source source, IdentifierExpressionNode function, SequenceNode<ExpressionNode> args);

	/**
	 * Returns a reference to a {@link TypeFactory}
	 * 
	 * @return
	 */
	TypeFactory typeFactory();

	/**
	 * Create a new {@link RunNode}
	 * 
	 * @param source    The {@link Source} attached to the $run statement
	 * @param statement The statement attached with the $run statement
	 * @return A new {@link RunNode}
	 */
	RunNode newRunNode(Source source, StatementNode statement);

	/**
	 * Creates a new <code>$object_of</code> node
	 * 
	 * @param source  the source of the <code>$object_of</code> node
	 * @param operand the operand of the <code>$object_of</code> node, which shall
	 *                have pointer type
	 * @return the new <code>$object_of</code> node
	 */
	ObjectOrRegionOfNode newObjectofNode(Source source, ExpressionNode operand);

	/**
	 * Creates a new <code>$region_of</code> node
	 * 
	 * @param source  the source of the <code>$region_of</code> node
	 * @param operand the operand of the <code>$region_of</code> node, which shall
	 *                have pointer type
	 * @return the new <code>$region_of</code> node
	 */
	ObjectOrRegionOfNode newRegionofNode(Source source, ExpressionNode operand);

	/**
	 * Creates a new allocation node, which represents either an
	 * <code>allocates</code> or <code>frees</code> clause.
	 * 
	 * @param source      the source of the allocation clause
	 * @param isAllocates true if this is an <code>allocates</code> clause,
	 *                    otherwise, a <code>frees</code> clause
	 * @param memoryList  the list of memory units of the allocation clause
	 * @return the new allocation node
	 */
	AllocationNode newAllocationNode(Source source, boolean isAllocates, SequenceNode<ExpressionNode> memoryList);

	/**
	 * Creates a new extended quantified expression node.
	 * 
	 * @param source   the source of the node
	 * @param quant    the extended quantifier, which could be one of
	 *                 <code>\sum, \max, \min, \numof, \product</code>.
	 * @param lo       the lower bound argument
	 * @param hi       the upper bound argument
	 * @param function the function argument
	 * @return the new extended quantified expression node.
	 */
	ExtendedQuantifiedExpressionNode newExtendedQuantifiedExpressionNode(Source source, ExtendedQuantifier quant,
			ExpressionNode lo, ExpressionNode hi, ExpressionNode function);

	/**
	 * Creates a new {@link PredicateNode} for ACSL predicates
	 * 
	 * @param source     the source of the new node
	 * @param identifier the {@link IdentifierNode} of the predicate identifier
	 * @param parameters parameters used in the predicate definition
	 * @param body       the predicate body expression
	 * @return
	 */
	PredicateNode newPredicateNode(Source source, IdentifierNode identifier,
			SequenceNode<VariableDeclarationNode> parameters, ExpressionNode body);

	FocusLoopTransformNode newFocusLoopNode(Source source, TokenFactory tokenFactory, String focusTag,
			SequenceNode<ExpressionNode> tagWindow, SequenceNode<ExpressionNode> memoryList);

	FocusOrderedTransformNode newFocusOrderedNode(Source source, TokenFactory tokenFactory, String focusTag,
			OperatorNode operator, RegularRangeNode range, ExpressionNode expr);

	FocusAssertTransformNode newFocusAssertNode(Source source, TokenFactory tokenFactory, List<String> focusTags);

	InsertTransformNode newInsertTransformNode(Source source, List<BlockItemNode> nodesToInsert, boolean insertAfter);
}
