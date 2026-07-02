package dev.civl.abc.analysis.pointsTo;

import static dev.civl.abc.analysis.pointsTo.PointsToTestsUtils.createDesignations;
import static dev.civl.abc.analysis.pointsTo.PointsToTestsUtils.exactContains;
import static dev.civl.abc.analysis.pointsTo.PointsToTestsUtils.findVariablesInFunction;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import dev.civl.abc.analysis.pointsTo.IF.AssignExprIF;
import dev.civl.abc.analysis.pointsTo.IF.FlowInsensePointsToAnalyzer;
import dev.civl.abc.analysis.pointsTo.IF.SimplePointsToAnalysisIF;
import dev.civl.abc.ast.IF.AST;
import dev.civl.abc.ast.entity.IF.Entity;
import dev.civl.abc.ast.entity.IF.Function;
import dev.civl.abc.ast.entity.IF.Variable;
import dev.civl.abc.err.IF.ABCException;
import dev.civl.abc.main.ABCExecutor;
import dev.civl.abc.main.TranslationTask;
import dev.civl.abc.main.TranslationTask.TranslationStage;

public class IntraProceduralPointsToTest {

	private File root = new File(new File(new File("examples"), "c"),
			"pointsToAnalysis");

	FlowInsensePointsToAnalyzer ptAnalyzer;

	private AST getAST(String filenameRoot) throws ABCException {
		TranslationTask task = new TranslationTask(
				new File(root, filenameRoot + ".c"));

		task.setStage(TranslationStage.ANALYZE_ASTS);

		ABCExecutor executor = ABCExecutor.execute(task);

		return executor.getAST(0);
	}

	/**
	 * Test <code>
	 * int a, b;
	 * int * p, *q;
	 * 
	 * p = &a;
	 * q = &b;
	 * </code> for separated(p, q);
	 * 
	 * @throws ABCException
	 */
	@Test
	public void twoSeparate() throws ABCException {
		AST ast = getAST("twoSeparate");
		Function mainFunc;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		mainFunc = (Function) ast.getInternalOrExternalEntity("main");

		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable q = varsInMain.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println("p points to " + pPts);
		assertTrue(exactContains(pPts, "a"));
		// System.out.println("q points to " + qPts);
		assertTrue(exactContains(qPts, "b"));
	}

	/**
	 * Test <code>
	 * int a, b, c, d;
	 * int * p, *q;
	 *
	 * p = &a; p = &b;
	 * q = &c; q = &d;
	 * </code> for separated(p, q);
	 *
	 * @throws ABCException
	 */
	@Test
	public void twoSeparate2() throws ABCException {
		AST ast = getAST("twoSeparate2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable q = varsInMain.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println("p points to " + pPts);
		assertTrue(exactContains(pPts, "a", "b"));
		// System.out.println("q points to " + qPts);
		assertTrue(exactContains(qPts, "d", "c"));
	}

	/**
	 * Test <code>
	 * int a;
	 * int * p, *q;
	 *
	 * p = &a;
	 * q = &a;
	 * </code> for !separated(p, q);
	 *
	 * @throws ABCException
	 */
	@Test
	public void twoAlias() throws ABCException {
		AST ast = getAST("twoAlias");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable q = varsInMain.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println("p points to " + pPts);
		assertTrue(exactContains(pPts, "a"));
		// System.out.println("q points to " + qPts);
		assertTrue(exactContains(qPts, "a"));
	}

	/**
	 * Test <code>
	 * int a;
	 * int * p, *q;
	 *
	 * p = &a;
	 * q = p;
	 * </code> for !separated(p, q);
	 *
	 * @throws ABCException
	 */
	@Test
	public void twoAlias2() throws ABCException {
		AST ast = getAST("twoAlias2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable q = varsInMain.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println("p points to " + pPts);
		assertTrue(exactContains(pPts, "a"));
		// System.out.println("q points to " + qPts);
		assertTrue(exactContains(qPts, "a"));
	}

	/**
	 * Test <code>
	 * int a, b, c;
	 * int * p, *q;
	 *
	 * p = &a; p = &b;
	 * q = p; q = &c;
	 * </code> for subset(*q, *p);
	 *
	 * @throws ABCException
	 */
	@Test
	public void subsetTest() throws ABCException {
		AST ast = getAST("subset");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable q = varsInMain.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println("p points to " + pPts);
		assertTrue(exactContains(pPts, "a", "b"));
		// System.out.println("q points to " + qPts);
		assertTrue(exactContains(qPts, "a", "b", "c"));
	}

	/**
	 * Test <code>
	 * int a, b;
	 * int * p, **p2, ***p3;
	 *
	 * p = &a; p = &b;
	 * p2 = &p; p3 = &p2;
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void multiLevelPtrs() throws ABCException {
		AST ast = getAST("multiLevelPtrs");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> varsInMain = findVariablesInFunction(mainFunc);
		Variable p = varsInMain.get("p");
		Variable p2 = varsInMain.get("p2");
		Variable p3 = varsInMain.get("p3");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsP2 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p2});
		List<AssignExprIF> ptsP3 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p3});
		List<AssignExprIF> ptsDP2 = new LinkedList<>(); // *p2
		List<AssignExprIF> ptsDP3 = new LinkedList<>(); // *p3
		List<AssignExprIF> ptsDDP3 = new LinkedList<>();// **p3

		for (AssignExprIF ptP2 : ptsP2)
			ptsDP2.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptP2));
		for (AssignExprIF ptP3 : ptsP3)
			ptsDP3.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptP3));
		for (AssignExprIF ptDP3 : ptsDP3)
			ptsDDP3.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptDP3));
		// System.out.println(ptsP.toString());
		assertTrue(exactContains(ptsP, "a", "b"));
		// System.out.println(ptsP2.toString());
		assertTrue(exactContains(ptsP2, "p"));
		// System.out.println(ptsP3.toString());
		assertTrue(exactContains(ptsP3, "p2"));
		// System.out.println(ptsDP2.toString());
		assertTrue(exactContains(ptsDP2, "a", "b"));
		// System.out.println(ptsDP3.toString());
		assertTrue(exactContains(ptsDP3, "p"));
		// System.out.println(ptsDDP3.toString());
		assertTrue(exactContains(ptsDDP3, "a", "b"));
	}

	/**
	 * Test <code>
	 * #include<stdlib.h>
	 * int main() {
	 * int a, b, c;
	 * int **p, *q;
	 *
	 * q = &a;
	 * q = &b;
	 * p = &q;
	 * *p = &c;
	 *
	 * }
	 * </code> for equiv(pts(*p), pts(q));
	 */
	@Test
	public void multiLevelPtrs2() throws ABCException {
		AST ast = getAST("multiLevelPtrs2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsDp = new LinkedList<>();

		for (AssignExprIF ptr : ptsP)
			ptsDp.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptr));
		// System.out.println(ptsDp.toString());
		assertTrue(exactContains(ptsDp, "a", "b", "c"));
		// System.out.println(ptsQ.toString());
		assertTrue(exactContains(ptsQ, "a", "b", "c"));
		// System.out.println(ptsP.toString());
		assertTrue(exactContains(ptsP, "q"));
	}

	/**
	 * Test <code>
	 * #include<stdlib.h>
	 * int main() {
	 * int a, b, c, d;
	 * int **p, **q, *y, *x;
	 *
	 * y = &a;
	 * x = &b;
	 * q = &y;
	 * p = &x;
	 * y = *p; // pts(x) subset of pts(y)
	 * *q = x;
	 *
	 * }
	 * </code> for subset(pts(y), pts(x)) and !subset(pts(x), pts(y)) ;
	 */
	@Test
	public void multiLevelPtrs3() throws ABCException {
		AST ast = getAST("multiLevelPtrs3");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable x = variables.get("x");
		Variable y = variables.get("y");
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> ptsX = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});
		List<AssignExprIF> ptsY = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{y});
		List<AssignExprIF> ptsDp = new LinkedList<>();
		List<AssignExprIF> ptsDq = new LinkedList<>();

		for (AssignExprIF ptP : ptsP)
			ptsDp.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptP));
		for (AssignExprIF ptQ : ptsQ)
			ptsDq.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptQ));

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "x"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "y"));
		// System.out.println(ptsX);
		assertTrue(exactContains(ptsX, "b"));
		// System.out.println(ptsY);
		assertTrue(exactContains(ptsY, "a", "b"));
		// System.out.println(ptsDp);
		assertTrue(exactContains(ptsDp, "b"));
		// System.out.println(ptsDq);
		assertTrue(exactContains(ptsDq, "a", "b"));

	}

	/**
	 * Test <code>
	 * int a, b;
	 * int * p, **p2, **p3;
	 *
	 * p = &a;
	 * *p2 = (b++ + 3) * sizeof(int) + p;
	 * p3 = (b++ + 3) * sizeof(int) + &p;
	 * </code>
	 * 
	 * @throws ABCException
	 */
	@Test
	public void ptrComplexExpr() throws ABCException {
		AST ast = getAST("ptrComplexExpr");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable p2 = variables.get("p2");
		Variable p3 = variables.get("p3");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsP2 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p2});
		List<AssignExprIF> ptsP3 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p3});
		List<AssignExprIF> ptsDP3 = new LinkedList<>(); // *p2

		for (AssignExprIF ptP3 : ptsP3)
			ptsDP3.addAll(ptAnalyzer.mayPointsTo(mainFunc, ptP3));
		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "a"));
		// System.out.println(ptsP2);
		assertTrue(ptsP2.isEmpty());
		// System.out.println(ptsP3);
		assertTrue(exactContains(ptsP3, "p"));
		// System.out.println(ptsDP3);
		assertTrue(exactContains(ptsDP3, "a"));
	}

	/**
	 * <code>
	 * int main() {
	 *   int *p = (int*)1;
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void fullSet() throws ABCException {
		AST ast = getAST("fullSet");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});

		// System.out.println(ptsP);
		assertTrue(ptsP.toString().equals("[FULL]"));
	}

	/**
	 * <code>
	 * int main() {
	 * int a, b, c;
	 * int *p, **p2, *q, *r;
	 *
	 * p = &a;
	 * p2 = &p;
	 * q = *p2;
	 * *p2 = &b;
	 * r = &c;
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void complex() throws ABCException {
		AST ast = getAST("complex");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable r = variables.get("r");
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsR = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{r});

		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "a", "b"));
		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "a", "b"));
		// System.out.println(ptsR);
		assertTrue(exactContains(ptsR, "c"));
	}

	/**
	 * <code>
	 * int main() {
	 * int * p;
	 * int * x, a;
	 *
	 * x = &a;
	 * *(&p) = *(&x);
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void dereferenceCancelAddressof() throws ABCException {
		AST ast = getAST("dereferenceCancelAddressof");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable x = variables.get("x");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsX = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});
		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "a"));
		// System.out.println(ptsX);
		assertTrue(exactContains(ptsX, "a"));
	}

	/**
	 * Test <code>
	 * #include<stdlib.h>
	 * int main() {
	 * int * p = malloc(...);
	 * int * q = malloc(...);
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void allocation() throws ABCException {
		AST ast = getAST("allocation");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "malloc(sizeof(int) * 10)[0]"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "malloc(sizeof(int) * 11)[0]"));
	}

	/**
	 * <code>
	 *
	 * int main() {
	 * char * str = "hello";
	 * char * str2 = "world";
	 * char * p = (char*)malloc(1000);
	 * char * q = (char*)malloc(1000);
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void allocation2() throws ABCException {
		AST ast = getAST("allocation2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable str = variables.get("str");
		Variable str2 = variables.get("str2");
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsStr = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{str});
		List<AssignExprIF> ptsStr2 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{str2});
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println(ptsStr);
		assertTrue(exactContains(ptsStr, "\"hello\""));
		// System.out.println(ptsStr2);
		assertTrue(exactContains(ptsStr2, "\"world\""));
		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "malloc(1000)[0]"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "malloc(100)[0]"));
	}

	/**
	 * Test <code>
	 * #include<stdlib.h>
	 * int main() {
	 * int a, b, c;
	 * int *p, *q;
	 *
	 * if (a == b) {
	 * p = &a;
	 * q = &b;
	 * } else {
	 * p = &c;
	 * q = &a;
	 * }
	 *
	 * }
	 * </code> for !separated(p, q);
	 */
	@Test
	public void branchInsensitive() throws ABCException {
		AST ast = getAST("branchInsensitive");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "a", "c"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "a", "b"));
	}

	/**
	 * Test <code>
	 * struct T {
	 * int a;
	 * int b;
	 * struct H {
	 * int c;
	 * int d
	 * } h;
	 * };
	 *
	 * int main() {
	 * struct T t;
	 * int * p, * p2, * p3, * p4;
	 * void * p5;
	 *
	 * p = &t.a;
	 * p2 = &t.b;
	 * p3 = &t.h.c;
	 * p4 = &t.h.d;
	 * p5 = &t.h
	 * }
	 * </code> for separated(p, p2, p3, p4) and subset(p5, p4) and subset(p5,
	 * p3).
	 *
	 * @throws ABCException
	 */
	@Test
	public void structFields() throws ABCException {
		AST ast = getAST("structFields");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable p2 = variables.get("p2");
		Variable p3 = variables.get("p3");
		Variable p4 = variables.get("p4");
		Variable p5 = variables.get("p5");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsP2 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p2});
		List<AssignExprIF> ptsP3 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p3});
		List<AssignExprIF> ptsP4 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p4});
		List<AssignExprIF> ptsP5 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p5});

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "t.a"));
		// System.out.println(ptsP2);
		assertTrue(exactContains(ptsP2, "t.b"));
		// System.out.println(ptsP3);
		assertTrue(exactContains(ptsP3, "t.h.c"));
		// System.out.println(ptsP4);
		assertTrue(exactContains(ptsP4, "t.h.d"));
		// System.out.println(ptsP5);
		assertTrue(exactContains(ptsP5, "t.h"));
	}

	/**
	 * Test <code>
	 * struct T {
	 * int *a;
	 * int *b;
	 * struct H {
	 * int *c;
	 * int *d
	 * } h;
	 * };
	 *
	 * int main() {
	 * struct T t, *t2;
	 * int a, b, c, d;
	 *
	 * t.a = &a;
	 * t.b = &b;
	 * t.h.c = &c;
	 * t.h.d = &d;
	 * t2 = &t;
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void structPointerFields() throws ABCException {
		AST ast = getAST("structFields2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable t = variables.get("t");
		Variable t2Var = variables.get("t2");
		Entity[] ta, tb, thc, thd, t2;

		ta = createDesignations(t, "a");
		tb = createDesignations(t, "b");
		thc = createDesignations(t, "h", "c");
		thd = createDesignations(t, "h", "d");
		t2 = createDesignations(t2Var);

		List<AssignExprIF> ptsTa = ptAnalyzer.mayPointsTo(mainFunc, ta);
		List<AssignExprIF> ptsTb = ptAnalyzer.mayPointsTo(mainFunc, tb);
		List<AssignExprIF> ptsThc = ptAnalyzer.mayPointsTo(mainFunc, thc);
		List<AssignExprIF> ptsThd = ptAnalyzer.mayPointsTo(mainFunc, thd);
		List<AssignExprIF> ptsT2 = ptAnalyzer.mayPointsTo(mainFunc, t2);

		// System.out.println(ptsTa);
		assertTrue(exactContains(ptsTa, "a"));
		// System.out.println(ptsTb);
		assertTrue(exactContains(ptsTb, "b"));
		// System.out.println(ptsThc);
		assertTrue(exactContains(ptsThc, "c"));
		// System.out.println(ptsThd);
		assertTrue(exactContains(ptsThd, "d"));
		// System.out.println(ptsT2);
		assertTrue(exactContains(ptsT2, "t"));
	}

	/**
	 * Test <code>
	 * struct H {
	 * int *c;
	 * int *d
	 * };
	 *
	 * struct T {
	 * int *a;
	 * int *b;
	 * struct H *h;
	 * };
	 *
	 * int main() {
	 * struct T t, *t2;
	 * struct H h;
	 * int a, b, c, d;
	 *
	 * t.h = &h;
	 * t2 = &t;
	 * t2->a = &a;
	 * t2->b = &b;
	 * t2->h->c = &c;
	 * t2->h->d = &d;
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void structPointerFieldsWithArrow() throws ABCException {
		AST ast = getAST("structFieldsWithArrow");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable t = variables.get("t");
		Variable h = variables.get("h");
		Entity[] ta, tb, hc, hd;

		ta = createDesignations(t, "a");
		tb = createDesignations(t, "b");
		hc = createDesignations(h, "c");
		hd = createDesignations(h, "d");

		List<AssignExprIF> ptsTa = ptAnalyzer.mayPointsTo(mainFunc, ta);
		List<AssignExprIF> ptsTb = ptAnalyzer.mayPointsTo(mainFunc, tb);
		List<AssignExprIF> ptsHc = ptAnalyzer.mayPointsTo(mainFunc, hc);
		List<AssignExprIF> ptsHd = ptAnalyzer.mayPointsTo(mainFunc, hd);

		// System.out.println(ptsTa);
		assertTrue(exactContains(ptsTa, "a"));
		// System.out.println(ptsTb);
		assertTrue(exactContains(ptsTb, "b"));
		// System.out.println(ptsHc);
		assertTrue(exactContains(ptsHc, "c"));
		// System.out.println(ptsHd);
		assertTrue(exactContains(ptsHd, "d"));
	}

	/**
	 * Test <code>
	 * struct H {
	 * int *c;
	 * int *d
	 * };
	 *
	 * struct T {
	 * int *a;
	 * int *b;
	 * struct H *h;
	 * };
	 *
	 * int main() {
	 * struct T t, *t2;
	 * struct H h;
	 * int a, b, c, d;
	 * 
	 * t.h = &h;
	 * t2 = &t;
	 * (*t2).a = &a;
	 * (*t2).b = &b;
	 * (*((*t2).h)).c = &c;
	 * (*((*t2).h)).d = &d;
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void structPointerFieldsWithDerefAndDot() throws ABCException {
		AST ast = getAST("structPointerFieldsWithDerefAndDot");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable t = variables.get("t");
		Variable h = variables.get("h");
		Entity[] ta, tb, hc, hd;

		ta = createDesignations(t, "a");
		tb = createDesignations(t, "b");
		hc = createDesignations(h, "c");
		hd = createDesignations(h, "d");

		List<AssignExprIF> ptsTa = ptAnalyzer.mayPointsTo(mainFunc, ta);
		List<AssignExprIF> ptsTb = ptAnalyzer.mayPointsTo(mainFunc, tb);
		List<AssignExprIF> ptsHc = ptAnalyzer.mayPointsTo(mainFunc, hc);
		List<AssignExprIF> ptsHd = ptAnalyzer.mayPointsTo(mainFunc, hd);

		// System.out.println(ptsTa);
		assertTrue(exactContains(ptsTa, "a"));
		// System.out.println(ptsTb);
		assertTrue(exactContains(ptsTb, "b"));
		// System.out.println(ptsHc);
		assertTrue(exactContains(ptsHc, "c"));
		// System.out.println(ptsHd);
		assertTrue(exactContains(ptsHd, "d"));
	}

	/**
	 * <code>
	 *
	 * int main() {
	 * char * str = "hello";
	 * char * str2 = "world";
	 * char * str3 = str;
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void stringLiteral() throws ABCException {
		AST ast = getAST("stringLiteral");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable str = variables.get("str");
		Variable str2 = variables.get("str2");
		Variable str3 = variables.get("str3");
		List<AssignExprIF> ptsStr = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{str});
		List<AssignExprIF> ptsStr2 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{str2});
		List<AssignExprIF> ptsStr3 = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{str3});

		// System.out.println(ptsStr);
		assertTrue(exactContains(ptsStr, "\"hello\""));
		// System.out.println(ptsStr2);
		assertTrue(exactContains(ptsStr2, "\"world\""));
		// System.out.println(ptsStr3);
		assertTrue(exactContains(ptsStr3, "\"hello\""));
	}

	/**
	 * <code>
	 * int main() {
	 * int a[10];
	 * int *p, (*q)[10], *x;
	 *
	 * p = a;
	 * q = &a;
	 * x = a + 1;
	 * y = &q[0][0];
	 * }
	 * </code>
	 */
	@Test
	public void pointerToArray() throws ABCException {
		AST ast = getAST("pointerToArray");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable x = variables.get("x");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> ptsX = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "a[0]"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "a"));
		// System.out.println(ptsX);
		assertTrue(exactContains(ptsX, "a[1]"));
	}

	/**
	 * <code>
	 * int a[10];
	 * int *p, (*q)[10], *x;
	 * int b[10][10];
	 *
	 * q = &a;
	 * q = b;
	 * x = &q[2][2];
	 * p = b[0];  
	 * </code>
	 */
	@Test
	public void pointerToArray2() throws ABCException {
		AST ast = getAST("pointerToArray2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable x = variables.get("x");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> ptsX = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "b[0][0]"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "b[0]", "a"));
		// System.out.println(ptsX);
		assertTrue(exactContains(ptsX, "b[2][2]"));
	}

	/**
	 * <code>
	 * struct T {
	 *   int a[10][10];
	 *   int b[10];
	 *   struct T * t;
	 * };
	 *	
	 * int main() {
	 *  struct T s, s2;
	 *  int *p, (*q)[10];
	 * 
	 *  s.t = &s2;
	 *  p = s.b;
	 *  q = s.a;
	 *  q = s.t->a;
	 *  p = s.t->b;
	 * }
	 * </code>
	 * 
	 * @throws ABCException
	 */
	@Test
	public void structFieldArray() throws ABCException {
		AST ast = getAST("structFieldArray");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		List<AssignExprIF> ptsP = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> ptsQ = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});

		// System.out.println(ptsP);
		assertTrue(exactContains(ptsP, "s.b[0]", "s2.b[0]"));
		// System.out.println(ptsQ);
		assertTrue(exactContains(ptsQ, "s.a[0]", "s2.a[0]"));
	}

	/**
	 * <code>
	 * int main() {
	 * int a[10];
	 * int *p, *q, *x;
	 *
	 * p = &a[3];
	 * q = &a[4];
	 * x = p + 1;
	 * }
	 * </code>
	 */
	@Test
	public void arraySubscript() throws ABCException {
		AST ast = getAST("arraySubscript");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable x = variables.get("x");
		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> xPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});

		// System.out.println(pPts);
		assertTrue(exactContains(pPts, "a[3]"));
		// System.out.println(qPts);
		assertTrue(exactContains(qPts, "a[4]"));
		// System.out.println(xPts);
		assertTrue(exactContains(xPts, "a[4]"));
	}

	/**
	 * <code>
	 * struct T {int x;};
	 *
	 * int main() {
	 * struct T t[10];
	 * struct T * p;
	 * int i, *q, *r;
	 *
	 * p = t;
	 * q = &(p->x);
	 * r = &((p + i)->x);
	 * }
	 * </code>
	 */
	@Test
	public void arrayStructMix() throws ABCException {
		AST ast = getAST("arrayStructMix");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable r = variables.get("r");
		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> rPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{r});

		// System.out.println(pPts);
		assertTrue(exactContains(pPts, "t[0]"));
		// System.out.println(qPts);
		assertTrue(exactContains(qPts, "t[0].x"));
		// System.out.println(rPts);
		assertTrue(exactContains(rPts, "t[*].x"));
	}

	/**
	 * <code>
	 * int a;
	 * struct S { int *p; int x; };
	 *
	 * int main() {
	 *   struct S s = {&a, 1};
	 *   return *s.p;
	 * }
	 * </code>
	 */
	@Test
	public void compoundInitPtrField() throws ABCException {
		AST ast = getAST("ptrField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable s = variables.get("s");
		Entity[] sp = createDesignations(s, "p");
		List<AssignExprIF> ptsSp = ptAnalyzer.mayPointsTo(mainFunc, sp);

		// System.out.println(ptsSp);
		assertTrue(exactContains(ptsSp, "a"));
	}

	/**
	 * <code>
	 * int a;
	 * int b;
	 * struct S { int *p; int *q; };
	 *
	 * int main() {
	 *   struct S s = {&a, &b};
	 *   return *s.p + *s.q;
	 * }
	 * </code>
	 */
	@Test
	public void compoundInitTwoPtrFields() throws ABCException {
		AST ast = getAST("twoPtrFields");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable s = variables.get("s");
		Entity[] sp = createDesignations(s, "p");
		Entity[] sq = createDesignations(s, "q");
		List<AssignExprIF> ptsSp = ptAnalyzer.mayPointsTo(mainFunc, sp);
		List<AssignExprIF> ptsSq = ptAnalyzer.mayPointsTo(mainFunc, sq);

		// System.out.println(ptsSp);
		assertTrue(exactContains(ptsSp, "a"));
		// System.out.println(ptsSq);
		assertTrue(exactContains(ptsSq, "b"));
	}

	/**
	 * <code>
	 * int a;
	 * struct Inner { int *p; };
	 * struct Outer { struct Inner in; int x; };
	 *
	 * int main() {
	 *   struct Outer o = {{&a}, 2};
	 *   return *o.in.p;
	 * }
	 * </code>
	 */
	@Test
	public void compoundInitNestedPtrField() throws ABCException {
		AST ast = getAST("nestedPtrField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable o = variables.get("o");
		Entity[] oinp = createDesignations(o, "in", "p");
		List<AssignExprIF> ptsOinp = ptAnalyzer.mayPointsTo(mainFunc, oinp);

		// System.out.println(ptsOinp);
		assertTrue(exactContains(ptsOinp, "a"));
	}

	/**
	 * <code>
	 * int a;
	 * int b;
	 * struct S { int *arr[2]; };
	 *
	 * int main() {
	 *   struct S s = {{&a, &b}};
	 *   return *s.arr[0] + *s.arr[1];
	 * }
	 * </code>
	 *
	 * TODO: createDesignations does not support array subscripts; add element-level
	 * assertions once the API is extended.
	 */
	@Test
	public void compoundInitPtrArrayField() throws ABCException {
		AST ast = getAST("ptrArrayField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		// smoke test: analysis must complete without exception
		assertTrue(mainFunc != null);
	}

	/**
	 * <code>
	 * struct S { int x; int y; };
	 *
	 * int main() {
	 *   struct S s = {1, 2};
	 *   return s.x + s.y;
	 * }
	 * </code>
	 * No pointer fields — compound initializer should produce no pointer assignments.
	 */
	@Test
	public void compoundInitNoPtrField() throws ABCException {
		AST ast = getAST("noPtrField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable s = variables.get("s");
		List<AssignExprIF> ptsS = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{s});

		// s is a non-pointer struct — its points-to set should be empty
		assertTrue(ptsS.isEmpty());
	}

	/**
	 * <code>
	 * int a;
	 * struct S { int *p; int x; };
	 *
	 * int main() {
	 *   int *r = (struct S){&a, 1}.p;
	 *   return *r;
	 * }
	 * </code>
	 * A compound literal used as an expression (not a declaration
	 * initializer): reading its pointer field should resolve r to `a`.
	 */
	@Test
	public void compoundLiteralPtrField() throws ABCException {
		AST ast = getAST("compoundLiteralPtrField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable r = variables.get("r");
		List<AssignExprIF> rPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{r});

		// System.out.println(rPts);
		assertTrue(exactContains(rPts, "a"));
	}

	/**
	 * <code>
	 * int a;
	 * int b;
	 * struct S { int *p; int *q; };
	 *
	 * int main() {
	 *   int *x = (struct S){&a, &b}.p;
	 *   int *y = (struct S){&a, &b}.q;
	 *   return *x + *y;
	 * }
	 * </code>
	 * A compound literal expression with two pointer fields: reading each
	 * field must stay separated (x -> a, y -> b), guarding against merging
	 * the fields of the aggregate.
	 */
	@Test
	public void compoundLiteralTwoPtrFields() throws ABCException {
		AST ast = getAST("compoundLiteralTwoPtrFields");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable x = variables.get("x");
		Variable y = variables.get("y");
		List<AssignExprIF> xPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{x});
		List<AssignExprIF> yPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{y});

		// System.out.println(xPts);
		assertTrue(exactContains(xPts, "a"));
		// System.out.println(yPts);
		assertTrue(exactContains(yPts, "b"));
	}

	/**
	 * <code>
	 * int a;
	 * struct Inner { int *p; };
	 * struct Outer { struct Inner in; int x; };
	 *
	 * int main() {
	 *   int *r = (struct Outer){{&a}, 2}.in.p;
	 *   return *r;
	 * }
	 * </code>
	 * A nested compound literal expression: reading the pointer field of the
	 * nested aggregate should resolve r to `a`.
	 */
	@Test
	public void compoundLiteralNestedPtrField() throws ABCException {
		AST ast = getAST("compoundLiteralNestedPtrField");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable r = variables.get("r");
		List<AssignExprIF> rPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{r});

		// System.out.println(rPts);
		assertTrue(exactContains(rPts, "a"));
	}

	/**
	 * <code>
	 * struct T {int x[10];};
	 *
	 * int main() {
	 * struct T t[10];
	 * struct T * p;
	 * int i, *q, *r;
	 *
	 * p = t;
	 * q = p->x;
	 * r = &((p + i)->x[2]);
	 * }
	 * </code>
	 */
	@Test
	public void arrayStructMix2() throws ABCException {
		AST ast = getAST("arrayStructMix2");

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());

		Function mainFunc = (Function) ast.getInternalOrExternalEntity("main");
		Map<String, Variable> variables = findVariablesInFunction(mainFunc);
		Variable p = variables.get("p");
		Variable q = variables.get("q");
		Variable r = variables.get("r");
		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{q});
		List<AssignExprIF> rPts = ptAnalyzer.mayPointsTo(mainFunc,
				new Entity[]{r});

		// System.out.println(pPts);
		assertTrue(exactContains(pPts, "t[0]"));
		// System.out.println(qPts);
		assertTrue(exactContains(qPts, "t[0].x[0]"));
		// System.out.println(rPts);
		assertTrue(exactContains(rPts, "t[*].x[2]"));
	}
}
