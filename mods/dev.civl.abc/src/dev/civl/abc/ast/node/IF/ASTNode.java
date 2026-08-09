package dev.civl.abc.ast.node.IF;

import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;

import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.IF.ASTException;
import dev.civl.abc.ast.IF.ASTFactory;
import dev.civl.abc.ast.IF.DifferenceObject;
import dev.civl.abc.ast.entity.IF.Scope;
import dev.civl.abc.ast.node.IF.acsl.ContractNode;
import dev.civl.abc.ast.node.IF.acsl.TransformNode;
import dev.civl.abc.ast.node.IF.compound.ArrayDesignatorNode;
import dev.civl.abc.ast.node.IF.compound.DesignationNode;
import dev.civl.abc.ast.node.IF.compound.FieldDesignatorNode;
import dev.civl.abc.ast.node.IF.declaration.EnumeratorDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FieldDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.TypedefDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.GenericSelectionNode;
import dev.civl.abc.ast.node.IF.expression.ResultNode;
import dev.civl.abc.ast.node.IF.label.OrdinaryLabelNode;
import dev.civl.abc.ast.node.IF.label.SwitchLabelNode;
import dev.civl.abc.ast.node.IF.omp.OmpNode;
import dev.civl.abc.ast.node.IF.omp.OmpReductionNode;
import dev.civl.abc.ast.node.IF.statement.DeclarationListNode;
import dev.civl.abc.ast.node.IF.statement.StatementNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.token.IF.Source;

/**
 * <p>
 * Root of the AST node type hierarchy. All AST nodes implement this interface.
 * </p>
 * 
 * <p>
 * An AST node has some number <i>n</i> of <strong>children</strong>. Each child
 * is either an AST node or <code>null</code>. The children are arranged in an
 * ordered sequences and numbered from 0.
 * </p>
 * 
 * <p>
 * Every AST node has at most one parent. If a node <i>u</i> has a (non-
 * <code>null</code>) parent <i>v</i>, then it is guaranteed that <i>u</i> is a
 * child of <i>v</i>. This class is designed so that it is not possible to
 * violate these invariants.
 * </p>
 * 
 * <p>
 * Nodes are mutable objects. It is possible to modify various aspects of the
 * node, to remove a node from its parent, to add new children to a node, and so
 * on.
 * </p>
 * 
 * <p>
 * At any point in time, a node is either "owned" by an AST or it is "free". A
 * node can be owned by at most one AST. A node starts out as free, and becomes
 * owned by an AST when an AST is created and the node is reachable from the
 * root node used to create the new AST. The nodes of an AST can become free by
 * "releasing" the AST, which essentially dissolves the AST but leaves the nodes
 * intact. Modifications to the tree structure (the parent and child relations)
 * can only occur on free nodes, hence to modify an AST it is necessary to first
 * release it. After the modifications are complete, a new AST can be formed
 * from the modified nodes.
 * </p>
 * 
 * @see #child(int)
 * @see #numChildren()
 * @see #parent()
 * @see ASTFactory#newAST(ASTNode)
 * @see #getOwner()
 * @see AST#release()
 */
public interface ASTNode {

	/**
	 * The different kind of AST nodes. Every AST node falls into one of the
	 * following categories.
	 * 
	 * @author siegel
	 * 
	 */
	public enum NodeKind {
		/**
		 * An array designator occurs in a compound initializer and specifies an index
		 * of an array. A node of this kind can be safely cast to
		 * {@link ArrayDesignatorNode}.
		 */
		ARRAY_DESIGNATOR,
		/**
		 * A node representing a CIVL-C collective assertion. Not yet implemented.
		 */
		COLLECTIVE,
		/**
		 * A node representing a contract item. A node of this kind can be safely cast
		 * to {@link ContractNode}.
		 */
		CONTRACT, DEPENDS_EVENT,
		/**
		 * A list of declarations; such a list can occur as an initializer in a
		 * <code>for</code> loop, for example. A node of this kind can be safely cast to
		 * {@link DeclarationListNode}.
		 */
		DECLARATION_LIST,
		/**
		 * A designation, which can occur in a compound initializer. A designation
		 * consists of a sequence of array or field designators to pinpoint a location
		 * within a compound structure. A node of this kind can be safely cast to
		 * {@link DesignationNode}.
		 */
		DESIGNATION,
		/**
		 * An enumerator declaration node represents the declaration of a single
		 * enumerator constant inside a complete <code>enum</code> definition. A node of
		 * this kind can be safely cast to {@link EnumeratorDeclarationNode}.
		 */
		ENUMERATOR_DECLARATION,
		/**
		 * A node representing an expression. A node of this kind can be safely cast to
		 * {@link ExpressionNode}.
		 */
		EXPRESSION,
		/**
		 * A single field declaration within a <code>struct</code> or <code>union</code>
		 * definition. A node of this kind can be safely cast to
		 * {@link FieldDeclarationNode}.
		 */
		FIELD_DECLARATION,
		/**
		 * A field designator can occur in a designation, which can occur in a compound
		 * initializer. It identifies a particular field in a structure or union. A node
		 * of this kind can be safely cast to {@link FieldDesignatorNode}.
		 */
		FIELD_DESIGNATOR,
		/**
		 * A function declaration which is not a function definition, i.e., which does
		 * not include the function body. A node of this kind can be safely cast to
		 * {@link FunctionDeclarationNode}.
		 */
		FUNCTION_DECLARATION,
		/**
		 * A function definition: this is the declaration of the function that includes
		 * the function body. A node of this kind may be safely cast to
		 * {@link FunctionDefinitionNode}.
		 */
		FUNCTION_DEFINITION,
		/**
		 * A generic association node. Represents an association (TypeNode,
		 * ExpressionNode) for use in a {@link GenericSelectionNode}. A node of this
		 * kind may be safely cast to {@link GenericAssociationNode}
		 */
		GENERIC_ASSOCIATION,
		/**
		 * An identifier node. Represents an occurrence of an identifier in the program.
		 * A node of this kind may be safely cast to {@link IdentifierNode}.
		 */
		IDENTIFIER,
		/**
		 * A node representing an OpenMP construct. May be safely cast to
		 * {@link OmpNode}.
		 */
		OMP_NODE,
		/**
		 * A node representing a reduction operator in an OpenMP <code>reduction</code>
		 * clause. May be safely cast to {@link OmpReductionNode}.
		 */
		OMP_REDUCTION_OPERATOR,
		/**
		 * A node representing a label in a labeled statement. (Does not include a
		 * <code>case</code> or <code>default</code> label.) May be safely cast to
		 * {@link OrdinaryLabelNode}.
		 */
		ORDINARY_LABEL,
		/**
		 * A pair node: a node of type {@link PairNode}<code>< S,T ></code> for some
		 * types <code>S</code> and <code>T</code> which are subtypes of
		 * {@link ASTNode}. Such a node has two children, one of type <code>S</code> and
		 * one of type <code>T</code>. A node of this kind can be safely cast to
		 * {@link PairNode}<code>< ?,? ></code>.
		 */
		PAIR,
		/**
		 * A pragma node, corresponding to a <code>#pragma</code> directive in the
		 * source code. May be safely cast to {@link PragmaNode}.
		 */
		PRAGMA,
		/**
		 * A "result"" node represents a use of the special variable
		 * <code>$result</code> in a post-condition in a CIVL-C procedure contract. It
		 * represents the value returned by the procedure. May be safely cast to
		 * {@link ResultNode}.
		 */
		RESULT,
		/**
		 * A CIVL-C scope-parameterized declaration. This is soon to be deprecated.
		 */
		SCOPE_PARAMETERIZED_DECLARATION,
		/**
		 * A node which implement the interface {@link SequenceNode} <code>< T ></code>.
		 * This is a node whose children all have type <code>T</code>, where
		 * <code>T</code> is a subtype of {@link ASTNode}.
		 */
		SEQUENCE,
		/**
		 * A node representing a statement. May be safely cast to {@link StatementNode}.
		 */
		STATEMENT,
		/**
		 * A node representing a C11 static assertion. This is a kind of assertion which
		 * can be checked at "compile time". A node of this kind may be safely cast to
		 * {@link StaticAssertionNode}.
		 */
		STATIC_ASSERTION,
		/**
		 * A switch label is either a label of the form <code>case CONSTANT:</code> or
		 * <code>default :</code> in a <code>switch</code> statement. A node of this
		 * kind may be safely cast to {@link SwitchLabelNode}.
		 */
		SWITCH_LABEL,
		/**
		 * A type node, representing any kind of type. A node of this kind may be safely
		 * cast to {@link TypeNode}.
		 */
		TYPE,
		/**
		 * A typedef node, representing a <code>typedef</code> construct in the program.
		 * A node of this kind may be safely cast to {@link TypedefDeclarationNode}.
		 */
		TYPEDEF,
		/**
		 * A variable declaration node. A node of this kind can be safely cast to
		 * {@link VariableDeclarationNode}.
		 */
		VARIABLE_DECLARATION
	};

	/**
	 * Returns the index-th child node of this AST node. The children of a node are
	 * ordered and numbered starting from 0.
	 * 
	 * @param index an integer in the range [0,n-1], where n is the number of
	 *              children of this node, i.e., the value returned by
	 *              {@link #numChildren()}
	 * @return the index-th child of this node; note that this may be
	 *         <code>null</code>.
	 * @throws NoSuchElementException if index is less than 0 or greater than or
	 *                                equal to the number of children
	 */
	ASTNode child(int index) throws NoSuchElementException;

	/**
	 * <p>
	 * Returns the index of this node among the children of its parent.
	 * </p>
	 * 
	 * <p>
	 * The children of a node are ordered and numbered from 0 to <i>n</i>-1, where
	 * <i>n</i> is the number of children. Since an AST is a tree, every node has at
	 * most one parent. If this node has a parent, this method returns the index of
	 * this node in its parent's children list. If this node does not have a parent,
	 * this method returns -1.
	 * </p>
	 * 
	 * @return the index of this node it its parent's child list, or -1 if it has no
	 *         parent
	 */
	int childIndex();

	/**
	 * Returns the sequence of children of this node as an iterable object. Do not
	 * attempt to cast the iterable to another type and/or modify it; if you do, all
	 * bets are off. Use it only to iterate over the children.
	 * 
	 * @return the (ordered) sequence of children nodes of this node; may contain
	 *         <code>null</code> values
	 */
	Iterable<ASTNode> children();

	/**
	 * Constructs a deep copy of this AST node. The node and all of its descendants
	 * will be cloned. The cloning does not copy analysis or attribute information.
	 * 
	 * @return deep copy of this node
	 */
	ASTNode copy();

	/**
	 * Constructs a deep copy of this AST node, similar to {@link #copy()}, but also
	 * copies the type information on expression node, i.e., the initial type and
	 * the conversion sequence.
	 * 
	 * @return deep copy of this node with type information
	 */
	ASTNode copyWithTypes();

	/**
	 * Returns the attribute value associated to the given key, or <code>null</code>
	 * if no value has been set for that key. Note that attribute keys are generated
	 * using method {@link NodeFactory#newAttribute(String, Class)}.
	 * 
	 * @param key an attribute key
	 * @return the value associated to the key by this node, or <code>null</code>
	 */
	Object getAttribute(AttributeKey key);

	/**
	 * Returns the "owner" of this AST, i.e., the AST to which this node belongs.
	 * This can be <code>null</code>, in which case we say this node is "free".
	 * 
	 * @return the owner of this node or <code>null</code>
	 */
	AST getOwner();

	/**
	 * Gets the scope in which the syntactic element corresponding to this node
	 * occurs.
	 * 
	 * @return the scope
	 */
	Scope getScope();

	/**
	 * Returns the source object that locates the origin of this program construct
	 * in the original source code. This is used for reporting friendly messages to
	 * the user. The source element specifies a range of tokens in a token stream
	 * and can be used to display file name, line numbers, and character indexes to
	 * precisely target a source region.
	 * 
	 * @return source object for this node
	 */
	Source getSource();

	/**
	 * ID number unique within the AST to which this node belongs, or -1 if the node
	 * is free (not owned by an AST).
	 * 
	 * @return if the node is owned by an AST, the node ID number, which is unique
	 *         among all nodes in this AST; otherwise -1
	 */
	int id();

	/**
	 * <p>
	 * Removes all children that do not satisfy the predicate and applies this
	 * method recursively to the remaining children.
	 * </p>
	 * 
	 * <p>
	 * "Removing a node" is interpreted as follows: if <i>u</i> is an instance of
	 * {@link SequenceNode}, and a child of <i>u</i> does not satisfy the predicate,
	 * then the child is removed and all subsequent elements of the sequence are
	 * shifted down to remove the gap. If <i>u</i> is not an instance of
	 * {@link SequenceNode} and the child does not satisfy the predicate then the
	 * child is replaced by <code>null</code>.
	 * </p>
	 * 
	 * @param keep a node predicate specifying which nodes to keep
	 */
	void keepOnly(NodePredicate keep);

	/**
	 * Returns the node kind: this is an element of the enumerated type
	 * {@link NodeKind}, which is used to categorize the different kinds of nodes.
	 * 
	 * @return The node kind
	 */
	NodeKind nodeKind();

	/**
	 * Returns the number of child nodes of this AST node. This includes children
	 * which are <code>null</code>!
	 * 
	 * @return the number of child nodes of this node
	 */
	int numChildren();

	/**
	 * Returns the parent of this node, or <code>null</code> if this node has no
	 * parent.
	 * 
	 * @return parent node or <code>null</code>
	 */
	ASTNode parent();

	/**
	 * Prints a long-form textual representation of this node. The form usually
	 * involves multiple lines.
	 * 
	 * @param prefix        a string which should be prepended to every line of
	 *                      output; typically something like "| | " which is used to
	 *                      control indentation
	 * @param out           stream to which to print
	 * @param includeSource should the source information be included in the
	 *                      print-out? This incorporates the file name, line
	 *                      number(s), and start and end character indexes for the
	 *                      source code corresponding to this node
	 */
	void print(String prefix, PrintStream out, boolean includeSource);

	/**
	 * <p>
	 * Removes the child at given <code>index</code> from this node.
	 * </p>
	 * 
	 * <p>
	 * The index must be in the range [0,n-1], where n is the value returned by
	 * {@link #numChildren()} in the pre-state (i.e., before this method is
	 * invoked). If there is no child at the given index (i.e., child is
	 * <code>null</code>), this is a no-op.
	 * </p>
	 * 
	 * <p>
	 * If the removed child is non-<code>null</code>, its parent is set to
	 * <code>null</code>.
	 * </p>
	 * 
	 * @param index nonnegative integer in the range [0,n-1], where n is the number
	 *              of children before executing this method
	 * @return the child that was removed (may be <code>null</code>)
	 * @throws ASTException if this node is not free, or <code>index</code> is not
	 *                      in the range [0,n-1]
	 */
	ASTNode removeChild(int index);

	/**
	 * Removes this node from its parent. If the parent of this node is already
	 * <code>null</code>, this is a no-op.
	 */
	void remove();

	/**
	 * Sets the attribute value associated to the given key. This method also checks
	 * that the value belongs to the correct class. Note that attribute keys are
	 * generated by calling method {@link NodeFactory#newAttribute(String, Class)}.
	 * 
	 * @param key   the attribute key
	 * @param value the attribute value
	 */
	void setAttribute(AttributeKey key, Object value);

	/**
	 * <p>
	 * Sets the child node at the given index. This node (i.e., <code>this</code>)
	 * must be free (not owned by an AST) when this method is called.
	 * </p>
	 * 
	 * <p>
	 * The child may be <code>null</code> or non-<code>null</code>. A non-
	 * <code>null</code> child must have a <code>null</code> parent, i.e., it must
	 * not be the child of another node.
	 * </p>
	 * 
	 * <p>
	 * If there is already a non-<code>null</code> child of this node in position
	 * <code>index</code>, the old child is removed, i.e., its parent is set to
	 * <code>null</code>.
	 * </p>
	 * 
	 * <p>
	 * The caller is responsible for ensuring that the child is of the appropriate
	 * kind and type.
	 * </p>
	 * 
	 * <p>
	 * The index can be any nonnegative integer. The list of children will be
	 * expanded as necessary with <code>null</code> values in order to incorporate
	 * the index.
	 * </p>
	 * 
	 * <p>
	 * To illustrate how this method could be used, consider the case of swapping
	 * two nodes. Supposed <code>u1</code> and <code>u2</code> are nodes,
	 * <code>u1</code> has a non-<code>null</code> child in position
	 * <code>i1</code>, and <code>u2</code> has a non-<code>null</code> child in
	 * position <code>i2</code>, and we wish to swap the two children. This could be
	 * accomplished with the following code:
	 * 
	 * <pre>
	 * ASTNode v1 = u1.removeChild(i1), v2 = u2.removeChild(i2);
	 * u1.setChild(i1, v2);
	 * u2.setChild(i2, v1);
	 * </pre>
	 * 
	 * </p>
	 * 
	 * @param index any nonnegative integer
	 * @param child a node (or null) to be made the index-th child of this node
	 * @return the old child in position <code>index</code> (may be
	 *         <code>null</code>)
	 * @throws ASTException if any of the following hold: (1) this node is not free,
	 *                      (2) <code>index</code> is negative, or (3)
	 *                      <code>child</code> is not <code>null</code> and has a
	 *                      non-<code>null</code> parent.
	 */
	ASTNode setChild(int index, ASTNode child);

	/**
	 * Sets the ID number of this node.
	 * 
	 * @param id the ID number
	 */
	void setId(int id);

	/**
	 * Sets the owner of this node to the given AST.
	 * 
	 * @param owner the AST to make the owner of this node
	 */
	void setOwner(AST owner);

	/**
	 * Sets the scope of this syntactic element.
	 * 
	 * @param scope the scope
	 */
	void setScope(Scope scope);

	void addTransformAnnotation(TransformNode transform);

	void removeTransformAnnotation(int index);

	void addAllTransformAnnotations(List<TransformNode> transforms);

	List<TransformNode> transformAnnotations();

	/**
	 * Is the given AST node equivalent to me?
	 * 
	 * @param that The given AST node to be compared.
	 * @return True iff this AST node is equivalent with that AST node.
	 */
	boolean equiv(ASTNode that);

	/**
	 * Returns the first difference between this AST node and that node.
	 * 
	 * @param that The given AST node to be compared.
	 * @return The difference of this AST node and that node, null if both nodes are
	 *         equivalent.
	 */
	DifferenceObject diff(ASTNode that);

	/**
	 * Returns a short textual representation of this node only.
	 * 
	 * @return short textual representation of this node
	 */
	String toString();

	/**
	 * Finds next non-null node in AST in DFS order.
	 * 
	 * @return next non-null node in DFS order or null if there is none
	 */
	ASTNode nextDFS();

	/**
	 * Pretty-prints this AST node (and its descendants) in a form that should be
	 * similar to the actual programming language.
	 * 
	 * @param out stream to which output should be sent
	 */
	void prettyPrint(PrintStream out);

	/**
	 * Returns the pretty representation of this AST node (and its descendants) in a
	 * form that should be similar to the actual programming language.
	 * 
	 * 
	 * @return the pretty representation of this AST node (and its descendants) in a
	 *         form that should be similar to the actual programming language.
	 */
	StringBuffer prettyRepresentation();

	/**
	 * Returns the pretty representation of this AST node (and its descendants) in a
	 * form that should be similar to the actual programming language.
	 * 
	 * @param maxLength the maximal length of the string representation of this
	 *                  node; -1 if the length is unlimited
	 * @return the pretty representation of this AST node (and its descendants) in a
	 *         form that should be similar to the actual programming language.
	 */
	StringBuffer prettyRepresentation(int maxLength);
}
