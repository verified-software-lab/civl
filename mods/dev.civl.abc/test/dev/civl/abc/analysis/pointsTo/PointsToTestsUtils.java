package dev.civl.abc.analysis.pointsTo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.civl.abc.analysis.pointsTo.IF.AssignExprIF;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Entity.EntityKind;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Scope;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.ast.node.IF.ASTNode;
import dev.civl.abc.ast.node.IF.IdentifierNode;
import dev.civl.abc.ast.node.IF.declaration.FunctionDefinitionNode;
import dev.civl.abc.ast.node.IF.declaration.VariableDeclarationNode;
import dev.civl.abc.ast.type.IF.Field;
import dev.civl.abc.ast.type.IF.StructureOrUnionType;
import dev.civl.abc.ast.type.IF.Type;
import dev.civl.abc.ast.type.IF.Type.TypeKind;

public class PointsToTestsUtils {

	static boolean exactContains(List<AssignExprIF> set, String... strings) {
		Set<String> strSet = new HashSet<>();

		for (String str : strings)
			strSet.add(str);
		for (AssignExprIF expr : set) {
			String exprStr = expr.toString();

			if (!strSet.remove(exprStr))
				return false;
		}
		return strSet.isEmpty();
	}

	// get variables visible in a function
	static Map<String, Variable> findVariablesInFunction(Function function) {
		FunctionDefinitionNode funcDefi = function.getDefinition();
		ASTNode body = funcDefi.getBody();
		ASTNode bodySibling = null;
		ASTNode tmp = body;
		Map<String, Variable> result = new HashMap<>();

		// add all formals
		for (VariableDeclarationNode formal : function.getDefinition()
				.getTypeNode().getParameters()) {
			result.put(formal.getName(), formal.getEntity());
		}
		// find sibling which will be the one stops DFS:
		while (tmp.parent() != null) {
			int childIdx = tmp.childIndex();

			if (tmp.parent().numChildren() > childIdx + 1) {
				bodySibling = tmp.parent().child(childIdx + 1);
				break;
			} else
				tmp = tmp.parent();
		}
		while (body.nextDFS() != bodySibling) {
			if (body instanceof IdentifierNode) {
				Entity entity = ((IdentifierNode) body).getEntity();

				if (entity.getEntityKind() == EntityKind.VARIABLE) {
					Variable var = (Variable) entity;

					result.put(var.getName(), var);
				}
			}
			body = body.nextDFS();
		}
		// visible entities from ancestor scopes:
		Scope parent = funcDefi.getScope();

		while (parent != null) {
			for (Entity entity : parent.getOrdinaryEntities())
				if (entity.getEntityKind() == EntityKind.VARIABLE) {
					Variable var = (Variable) entity;

					result.put(var.getName(), var);
				}
			parent = parent.getParentScope();
		}
		return result;
	}

	/**
	 * <p>
	 * Creates a list of {@link Entity}s representing a designation to a struct
	 * field which is identified by the given {@link Variable} "var" and a list
	 * of field names "n0, n1, n2, ...".
	 * </p>
	 * 
	 * <p>
	 * The returned designations denotes "var.n1.n2. ..." which must be a valid
	 * expression w.r.t the type of "var".
	 * </p>
	 * 
	 * @param var
	 * @param fieldNames
	 * @return
	 */
	// TODO: this can be re-written with AccessPathNode
	static Entity[] createDesignations(Variable var, String... fieldNames) {
		Type type = var.getType();
		List<Entity> designations = new LinkedList<>();
		int i = 0;

		designations.add(var);
		while (type.kind() == TypeKind.STRUCTURE_OR_UNION
				&& i < fieldNames.length) {
			StructureOrUnionType structType = (StructureOrUnionType) type;
			Field field = structType.getField(fieldNames[i++]);

			designations.add(field);
			type = field.getType();
		}

		Entity rets[] = new Entity[designations.size()];

		designations.toArray(rets);
		return rets;
	}
}
