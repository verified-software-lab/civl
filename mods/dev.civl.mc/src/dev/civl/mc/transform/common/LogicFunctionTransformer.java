package dev.civl.mc.transform.common;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.ASTNode.NodeKind;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.NodeFactory;
import dev.civl.abc.ast.node.IF.PairNode;
import dev.civl.abc.ast.node.IF.SequenceNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDeclarationNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode;
import dev.civl.abc.ast.node.IF.expression.ExpressionNode.ExpressionKind;
import dev.civl.abc.ast.node.IF.expression.FunctionCallNode;
import dev.civl.abc.ast.node.IF.expression.IdentifierExpressionNode;
import dev.civl.abc.ast.node.IF.expression.OperatorNode.Operator;
import dev.civl.abc.ast.node.IF.expression.QuantifiedExpressionNode;
import dev.civl.abc.ast.node.IF.type.ArrayTypeNode;
import dev.civl.abc.ast.node.IF.type.FunctionTypeNode;
import dev.civl.abc.ast.node.IF.type.PointerTypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode;
import dev.civl.abc.ast.node.IF.type.TypeNode.TypeNodeKind;
import dev.civl.abc.ast.type.IF.FunctionType;
import dev.civl.abc.ast.type.IF.StandardBasicType.BasicTypeKind;
import dev.civl.abc.ast.type.IF.Type.TypeKind;
import dev.civl.abc.token.IF.Source;
import dev.civl.abc.token.IF.SyntaxException;
import dev.civl.abc.token.IF.TokenFactory;
import dev.civl.abc.util.IF.Pair;
import dev.civl.mc.model.IF.CIVLUnimplementedFeatureException;

/**
 * <p>
 * A logic function transformer helps transforming 1) a logic function
 * definition to a form that can be easily evaluated in a stateless way. 2) a
 * logic function call expression to the form corresponds to the change of its
 * definition.
 * </p>
 * 
 * <p>
 * A logic function definition is suppose to be stateless. Objects referred by
 * the definition must be declared as the formal parameters, except for
 * pointers. Dereferencing pointers needs states. However, instead of passing
 * pointers to logic functions, passing array values which are referred by the
 * pointers can help make the logic function definition being stateless. This
 * idea brings "valid negative array element indices" in the definition since a
 * pointer can points to the middle of an array.
 * </p>
 * 
 * <p>
 * To get rid of "valid negative array element indices", this transformer
 * transforms a pointer <code>p</code> to a base address <code>q</code> and an
 * offset <code>oft</code> s.t. <code>q + offset = p</code> and
 * <code>q - 1</code> is invalid. Then any appearance of <code>p</code> in the
 * definition is replaced with <code>&q[oft]</code>. Except that for logic
 * function call expressions <code>f(..., e(p), ...)</code> will be transformed
 * to <code>f(..., e(&q[oft], 0, ...)</code>, where <code>e(p)</code> is an
 * expression of pointer type involving <code>p</code>.
 * </p>
 * 
 * <p>
 * For quantified expressions <code>Q</code> with a pointer type bound variable
 * <code>p</code>: <code>
 * Q := FORALL p. P(p) will be transformed to FORALL q. FORALL oft. P(&q[oft]).
 * Q := EXISTS p. P(p) will be transformed to EXISTS q. EXISTS oft. P(&q[oft]).
 * </code>
 * </p>
 * 
 * 
 * @author ziqing
 *
 */
public class LogicFunctionTransformer {

	/**
	 * Name prefix for the generated extra offset argument
	 */
	private static String offset_name_prefix = "_oft_";

	/**
	 * Name of the system function which maps a pointer p to another pointer q such
	 * that there exists a interger offset that q + offset = p. And q - 1 is
	 * invalid.
	 */
	private static String array_base_address_of = "$array_base_address_of";

	/**
	 * A reference to {@link NodeFactory}
	 */
	private NodeFactory nodeFactory;

	/**
	 * A reference to {@link TokenFactory}
	 */
	private TokenFactory tokenFactory;

	private class Pointer {
		final IdentifierNode baseAddr;
		final IdentifierNode offset;

		Pointer(IdentifierNode baseAddr) {
			this.baseAddr = baseAddr;
			this.offset = nodeFactory.newIdentifierNode(baseAddr.getSource(), offset_name_prefix + baseAddr.name());
			assert this.baseAddr != null && this.offset != null;
		}
	}

	public LogicFunctionTransformer(NodeFactory nodeFactory, TokenFactory tokenFactory) {
		this.nodeFactory = nodeFactory;
		this.tokenFactory = tokenFactory;
	}

	/**
	 * Transforms a logic function definition to a form that is easily for back-end
	 * to evaluate it in a stateless way. see {@link LogicFunctionTransformer}.
	 * 
	 * @param type       the function type of the logic function
	 * @param expression A logic function definition
	 * @return transformed definition
	 * @throws SyntaxException
	 */
	public void transformDefinition(FunctionDeclarationNode logicFunctionDecl) throws SyntaxException {
		if (!logicFunctionDecl.isLogicFunction())
			return;

		// System.out.println(
		// "Transform " + logicFunctionDecl.prettyRepresentation());

		FunctionTypeNode typeNode = (FunctionTypeNode) logicFunctionDecl.getTypeNode();
		List<VariableDeclarationNode> newParams = new LinkedList<>();
		List<Pointer> pointerParams = new LinkedList<>();
		Source newParamSource = null;

		for (VariableDeclarationNode formal : typeNode.getParameters()) {
			if (formal.getTypeNode().getType().kind() == TypeKind.POINTER) {
				Pointer pointerParam = new Pointer(formal.getIdentifier());
				supportedFormalType(formal, formal.getTypeNode());
				newParams.add(formal.copy());
				newParams.add(nodeFactory.newVariableDeclarationNode(formal.getSource(), pointerParam.offset.copy(),
						nodeFactory.newBasicTypeNode(formal.getSource(), BasicTypeKind.INT)));
				pointerParams.add(pointerParam);
			} else
				newParams.add(formal.copy());
			newParamSource = newParamSource == null ? formal.getSource()
					: tokenFactory.join(newParamSource, formal.getSource());
		}
		newParamSource = newParamSource == null ? logicFunctionDecl.getSource() : newParamSource;

		Stack<Pointer[]> pointersStack = new Stack<>();
		Pointer[] pointerArgs = new Pointer[pointerParams.size()];

		pointerParams.toArray(pointerArgs);
		pointersStack.push(pointerArgs);
		if (logicFunctionDecl.isDefinition()) {
			ExpressionNode definition = ((FunctionDefinitionNode) logicFunctionDecl).getLogicDefinition();

			definition = tranformExpression(definition, pointersStack);

			// System.out.println(" ==> " + definition.prettyRepresentation());
		}
		typeNode.getParameters().remove();
		typeNode.setParameters(nodeFactory.newSequenceNode(newParamSource, "logic function params", newParams));
		// System.out.println(" ==> " +
		// logicFunctionDecl.prettyRepresentation());
	}

	private ExpressionNode tranformExpression(ExpressionNode definition, Stack<Pointer[]> pointersStack)
			throws SyntaxException {
		ASTNode node = definition;
		ASTNode parent = node.parent();
		int childIdx = node.childIndex();
		List<Pair<ASTNode, ASTNode>> replacements = new LinkedList<>();

		node.remove();
		do {
			if (node.nodeKind() == NodeKind.EXPRESSION) {
				ExpressionNode expr = (ExpressionNode) node;

				switch (expr.expressionKind()) {
				case IDENTIFIER_EXPRESSION:
					expr = transformIdentifierExpressionWorker((IdentifierExpressionNode) expr, pointersStack);
					replacements.add(new Pair<>(node, expr));
					break;
				case FUNCTION_CALL:
					transformFuncCallExpressionWorker((FunctionCallNode) expr, pointersStack);
					// changing children of FunctionCallNode, no need for
					// replacing
					break;
				case QUANTIFIED_EXPRESSION:
					expr = transformQuantifiedExpressionWorker((QuantifiedExpressionNode) expr, pointersStack);
					replacements.add(new Pair<>(node, expr));
					break;
				default:
				}
			}
		} while ((node = node.nextDFS()) != null);
		parent.setChild(childIdx, definition);
		// do replacements at one time:
		for (Pair<ASTNode, ASTNode> replace : replacements) {
			ASTNode replaceParent = replace.left.parent();
			int replaceChildIdx = replace.left.childIndex();

			replace.left.remove();
			replaceParent.setChild(replaceChildIdx, replace.right);
		}
		return definition;
	}

	/**
	 * transforms <code>p</code> to <code>&q[offset}</code>,
	 * <code>q + offset = p</code>.
	 * 
	 * @return new expression node can be used to replace the given identifier
	 *         expression.
	 */
	private ExpressionNode transformIdentifierExpressionWorker(IdentifierExpressionNode identifierExpr,
			Stack<Pointer[]> pointersStack) {
		Pointer matched = match(identifierExpr.getIdentifier(), pointersStack);

		if (matched == null)
			return identifierExpr;
		else {
			// p -> &q[oft]: //TODO: think about what type of pointer is correct
			Source source = identifierExpr.getSource();
			ExpressionNode transformed = nodeFactory.newOperatorNode(source, Operator.SUBSCRIPT,
					nodeFactory.newIdentifierExpressionNode(source, matched.baseAddr.copy()),
					nodeFactory.newIdentifierExpressionNode(source, matched.offset.copy()));

			return nodeFactory.newOperatorNode(source, Operator.ADDRESSOF, transformed);
		}
	}

	/**
	 * transforms <code>f(..., p, ...)</code> where p is a pointer to
	 * <code>f(..., &q[offset], 0, ...)</code>
	 */
	private void transformFuncCallExpressionWorker(FunctionCallNode callNode, Stack<Pointer[]> pointersStack)
			throws SyntaxException {
		if (callNode.getFunction().expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			return;

		List<ExpressionNode> newArgs = new LinkedList<>();
		SequenceNode<ExpressionNode> oldArgs = callNode.getArguments();

		for (ExpressionNode arg : callNode.getArguments()) {
			if (arg.getType().kind() == TypeKind.POINTER) {
				arg = tranformExpression(arg, pointersStack);
				newArgs.add(arg.copy());
				newArgs.add(nodeFactory.newIntegerConstantNode(arg.getSource(), "0"));
			} else {
				arg.remove();
				newArgs.add(arg);
			}
		}
		oldArgs.remove();
		callNode.setArguments(nodeFactory.newSequenceNode(oldArgs.getSource(), "logic-func-args", newArgs));
		return;
	}

	/**
	 * transforms <code>Quantifier *p. P(p)</code> to
	 * <code>Quantifier *q, a. P(&q[a])</code>
	 * 
	 * @return new expression node can be used to replace the given quantified
	 *         expression.
	 */
	private ExpressionNode transformQuantifiedExpressionWorker(QuantifiedExpressionNode quantNode,
			Stack<Pointer[]> pointersStack) throws SyntaxException {
		List<Pointer> pointers = new LinkedList<>();

		for (PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode> bvs : quantNode.boundVariableList()) {
			for (VariableDeclarationNode bv : bvs.getLeft()) {
				// TODO: for now, assuming pointer type bound variables are
				// never appear in restrictions:
				if (bv.getTypeNode().getType().kind() == TypeKind.POINTER) {
					Pointer pointer = new Pointer(bv.getIdentifier());

					pointers.add(pointer);
				}
			}
		}

		Pointer pointersArray[] = new Pointer[pointers.size()];
		ExpressionNode pred;

		if (pointersArray.length <= 0)
			return quantNode;
		pointers.toArray(pointersArray);
		pointersStack.push(pointersArray);
		pred = tranformExpression(quantNode.expression(), pointersStack);
		pointersStack.pop();
		pred = nodeFactory.newQuantifiedExpressionNode(quantNode.getSource(), quantNode.quantifier(),
				quantNode.boundVariableList().copy(), quantNode.restriction().copy(), pred.copy());

		List<VariableDeclarationNode> offsets_bv = new LinkedList<>();
		SequenceNode<PairNode<SequenceNode<VariableDeclarationNode>, ExpressionNode>> offsets;
		Source offsetsSource = null;

		for (Pointer ptr : pointersArray) {
			offsets_bv.add(nodeFactory.newVariableDeclarationNode(ptr.offset.getSource(), ptr.offset.copy(),
					nodeFactory.newBasicTypeNode(ptr.offset.getSource(), BasicTypeKind.INT)));
			offsetsSource = offsetsSource != null ? tokenFactory.join(offsetsSource, ptr.offset.getSource())
					: ptr.offset.getSource();
		}
		assert offsetsSource != null;
		offsets = nodeFactory.newSequenceNode(offsetsSource, "bounded-offset-sequence",
				Arrays.asList(nodeFactory.newPairNode(offsetsSource,
						nodeFactory.newSequenceNode(offsetsSource, "bounded-offsets", offsets_bv), null)));
		return nodeFactory.newQuantifiedExpressionNode(pred.getSource(), quantNode.quantifier(), offsets, null, pred);
	}

	/**
	 * 
	 * @return true iff the given node is an identifier expression of one of the
	 *         pointer type arguments (or bound variable).
	 */
	private Pointer match(ASTNode node, Stack<Pointer[]> pointersStack) {
		if (node instanceof IdentifierNode) {
			IdentifierNode idNode = (IdentifierNode) node;
			for (Pointer[] ptrs : pointersStack)
				for (Pointer ptr : ptrs)
					if (ptr.baseAddr.name().equals(idNode.name()))
						return ptr;
		}
		return null;
	}

	/**
	 * <p>
	 * Transforms a logic function call, which is NOT in any logic function
	 * definition, to a form that corresponds to the change of its definition. see
	 * {@link #transformDefinition(FunctionType, ExpressionNode)}.
	 * </p>
	 * <p>
	 * A logic function call with pointer-type actual paramter <code>p</code>
	 * <code>f(..., p, ...)</code> will be transformed to
	 * <code>f(..., $array_base_address_of(p),  p - $array_base_address_of(p), ...)</code>
	 * .
	 * </p>
	 * 
	 * @param expression a function call expression to a logic function
	 * @return transformed logic function call
	 */
	public void transformCall(FunctionCallNode expression) {
		ExpressionNode function = expression.getFunction();

		if (function.expressionKind() != ExpressionKind.IDENTIFIER_EXPRESSION)
			return;

		IdentifierExpressionNode funcIdent = (IdentifierExpressionNode) function;
		Entity entity = funcIdent.getIdentifier().getEntity();
		Function funcEntity;

		if (entity == null || entity.getEntityKind() != EntityKind.FUNCTION)
			return;
		funcEntity = (Function) entity;
		if (!funcEntity.isLogic())
			return;

		// System.out.println("Transform: " +
		// expression.prettyRepresentation());
		// transform arguments:
		SequenceNode<ExpressionNode> args = expression.getArguments();
		List<ExpressionNode> newArgs = new LinkedList<>();
		int idx = 0;
		FunctionDeclarationNode logicFuncDecl = (FunctionDeclarationNode) funcEntity.getFirstDeclaration();
		FunctionTypeNode funcType = (FunctionTypeNode) logicFuncDecl.getTypeNode();

		for (ExpressionNode arg : args) {
			if (arg.getType().kind() == TypeKind.POINTER) {
				newArgs.add(arrayBaseAddressOf(arg.copy()));
				newArgs.add(
						offsetToArrayBase(funcType.getParameters().getSequenceChild(idx).getTypeNode(), arg.copy()));
				idx += 2;
			} else {
				arg.remove();
				newArgs.add(arg);
				idx++;
			}
		}
		args.remove();
		expression.setArguments(nodeFactory.newSequenceNode(args.getSource(), "logic-function arguments", newArgs));
		// System.out.println(" ==> " + expression.prettyRepresentation());
	}

	/**
	 * transforms <code>p</code> to <code>$array_base_address_of(p)</code>
	 */
	private ExpressionNode arrayBaseAddressOf(ExpressionNode pointer) {
		Source source = pointer.getSource();

		return nodeFactory.newFunctionCallNode(source, nodeFactory.newIdentifierExpressionNode(source,
				nodeFactory.newIdentifierNode(source, array_base_address_of)), Arrays.asList(pointer.copy()));
	}

	/**
	 * generating <code>p - (int *)$array_base_address_of(p)</code>
	 */
	private ExpressionNode offsetToArrayBase(TypeNode pointerTypeNode, ExpressionNode pointer) {
		Source source = pointer.getSource();
		return nodeFactory.newOperatorNode(source, Operator.MINUS, pointer.copy(),
				nodeFactory.newCastNode(pointer.getSource(), pointerTypeNode.copy(), arrayBaseAddressOf(pointer)));
	}

	/**
	 *
	 * @return true iff the given type node of the formal parameter represents a
	 *         non-pointer scalar type or a pointer to non-pointer scalar type
	 */
	private void supportedFormalType(ASTNode formal, TypeNode typeNode) {
		if (typeNode.kind() == TypeNodeKind.BASIC && typeNode.kind() != TypeNodeKind.POINTER)
			return;
		if (typeNode.kind() == TypeNodeKind.POINTER) {
			TypeNode referredType = ((PointerTypeNode) typeNode).referencedType();

			if (noPointerIn(referredType))
				return;
		}
		throw new CIVLUnimplementedFeatureException("A formal parameter of logic function has non-scalar type,"
				+ " pointer to pointer type or pointer to non-scalar type.", formal.getSource());
	}

	/**
	 * @return true iff the given type node contains no sub-type which is a pointer
	 *         type. e.g. if the given type node represents an array of int type,
	 *         there is NO sub-type in it is a pointer type; if the given type node
	 *         represents an array of pointer to int, then there IS sub-type in it
	 *         is a pointer type.
	 */
	private boolean noPointerIn(TypeNode typeNode) {
		if (typeNode.kind() == TypeNodeKind.POINTER)
			return false;
		if (typeNode.kind() == TypeNodeKind.BASIC)
			return true;
		if (typeNode.kind() == TypeNodeKind.ARRAY) {
			return noPointerIn(((ArrayTypeNode) typeNode).getElementType());
		}
		return false;
	}
}
