package dev.civl.abc.analysis.pointsTo;

import static dev.civl.abc.analysis.pointsTo.PointsToTestsUtils.exactContains;
import static dev.civl.abc.analysis.pointsTo.PointsToTestsUtils.findVariablesInFunction;
import static org.junit.Assert.assertTrue;

import java.io.File;
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

public class InterProceduralPointsToTest {

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
	 * int * p, * q;
	 *
	 * void f() {
	 * int a, b;
	 * p = &a;
	 * q = &b;
	 * }
	 *
	 * int main() {
	 * f();
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void globalsSeparate() throws ABCException {
		AST ast = getAST("globalsSeparate");
		Function f;
		Map<String, Variable> variables;
		Variable p, q;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");

		List<AssignExprIF> pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		List<AssignExprIF> qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});

		// System.out.println(pPts);
		assertTrue(exactContains(pPts, "a"));
		// System.out.println(qPts);
		assertTrue(exactContains(qPts, "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});

		// System.out.println(pPts);
		assertTrue(pPts.isEmpty());
		// System.out.println(qPts);
		assertTrue(qPts.isEmpty());
	}

	/**
	 * Test <code>
	 * int * p, * q;
	 *
	 * void f() {
	 * int a, b;
	 * p = &a;
	 * q = &a;
	 * }
	 *
	 * int main() {
	 * f();
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void globalsAlias() throws ABCException {
		AST ast = getAST("globalsAlias");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable p, q;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(pPts.isEmpty());
		assertTrue(qPts.isEmpty());
	}

	/**
	 * Test <code>
	 * int * p, * q;
	 *
	 * void f() {
	 * int a, b;
	 * p = &a;
	 * q = p;
	 * q = &b;
	 * }
	 *
	 * int main() {
	 * f();
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void globalsSubset() throws ABCException {
		AST ast = getAST("globalsSubset");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable q, p;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});

		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a", "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});

		assertTrue(pPts.isEmpty());
		assertTrue(qPts.isEmpty());

	}

	/**
	 * Test <code>
	 * void f(int * p, int * q) {
	 * // test separate here
	 * }
	 *
	 * int main() {
	 * int * p, int * q;
	 * int a, b;
	 * p = &a;
	 * q = &b;
	 * f(p, q);
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsSeparate() throws ABCException {
		AST ast = getAST("argsSeparate");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable q, p;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");

		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b"));
	}

	/**
	 * Test <code>
	 *
	 * void f(int * p, int * q) {
	 * // test alias here
	 * }
	 *
	 * int main() {
	 * int * p, int * q;
	 * int a, b;
	 * p = &a;
	 * q = &a;
	 * f(p, q);
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsAlias() throws ABCException {
		AST ast = getAST("argsAlias");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable q, p;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		q = variables.get("q");
		p = variables.get("p");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a"));
	}

	/**
	 * Test <code>
	 *
	 * void f(int * p, int * q) {
	 * // test subset here
	 * }
	 *
	 * int main() {
	 * int * p, int * q;
	 * int a, b;
	 *
	 * p = &a;
	 * q = p;
	 * q = &b;
	 * f(p, q);
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsSubset() throws ABCException {
		AST ast = getAST("argsSubset");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable q, p;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a", "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a", "b"));
	}

	/**
	 * Test <code>
	 * void f(int * p, int * q) {
	 * // test separate(p, q)
	 * }
	 *
	 * int main() {
	 * int * p, int * q, *r;
	 * int a, b, c;
	 *
	 * p = &a;
	 * q = &b;
	 * f(p, q);
	 * r = &c;
	 * f(p, r);
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsSeparate2() throws ABCException {
		AST ast = getAST("argsSeparate2");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable r, q, p;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b", "c"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo(f, new Entity[]{r});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b"));
		assertTrue(exactContains(rPts, "c"));
	}

	/**
	 * Test <code>
	 *
	 * void f(int * p, int * q) {
	 * // test alias here
	 * }
	 *
	 * int main() {
	 * int * p, int * q, *r;
	 * int a, b;
	 *
	 * p = &a;
	 * q = &b;
	 * f(p, q);
	 * f(q, p);
	 * }
	 *
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsAlias2() throws ABCException {
		AST ast = getAST("argsAlias2");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable p, q;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a", "b"));
		assertTrue(exactContains(qPts, "a", "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b"));
	}

	/**
	 * Test <code>
	 * int b;
	 *
	 * void f(int * p, int * q) {
	 * // test subset here
	 * f(p, &b);
	 * }
	 *
	 * int main() {
	 * int * p, int * q, * r;
	 * int a;
	 *
	 * p = &a;
	 * q = p;
	 * f(p, q);
	 * }
	 * </code>
	 *
	 * @throws ABCException
	 */
	@Test
	public void argsSubset2() throws ABCException {
		AST ast = getAST("argsSubset2");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable p, q;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a", "b"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo(f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo(f, new Entity[]{q});
		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "a"));
	}

	/**
	 * <code>
	 * int c;
	 *
	 * void f(int * p, int * q, int * r) {
	 * int a, b;
	 *
	 * p = &a;
	 * q = &b;
	 * r = &c;
	 * }
	 *
	 * int main() {
	 * int * p, int * q, * r;
	 *
	 * f(p, q, r);
	 * }
	 * </code>
	 */
	@Test
	public void scoping() throws ABCException {
		AST ast = getAST("scoping");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable p, q, r;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("f");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});

		assertTrue(exactContains(pPts, "a"));
		assertTrue(exactContains(qPts, "b"));
		assertTrue(exactContains(rPts, "c"));

		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});

		assertTrue(pPts.isEmpty());
		assertTrue(qPts.isEmpty());
		assertTrue(rPts.isEmpty());
	}

	/**
	 * <code>
	 * int c;
	 *
	 * int * f() {
	 * int * p = &c;
	 * return p;
	 * }
	 *
	 * int * g() {
	 *   int x;
	 *   int * p = &x;
	 *   return p;
	 * }
	 *
	 * int main() {
	 * int * p =f();
	 * int * q = &c;
	 * int * r = g();
	 * }
	 * </code>
	 */
	@Test
	public void returnVal() throws ABCException {
		AST ast = getAST("return");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable p, q, r;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});
		assertTrue(exactContains(pPts, "c"));
		assertTrue(exactContains(qPts, "c"));
		assertTrue(rPts.isEmpty());
	}

	/**
	 * <code>
	 * int c;
	 * int *g;
	 *
	 * int * f() {
	 * int ** p = &g;
	 * return *g;
	 * }
	 *
	 * int * g() {
	 *
	 * return &c;
	 * }
	 *
	 * int main() {
	 * g =&c;
	 * int * p = f(); // p->c
	 * int * q = &c; // q->c
	 * int * r = g(); // r->c
	 * }
	 * </code>
	 */
	@Test
	public void returnVal2() throws ABCException {
		AST ast = getAST("return2");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable p, q, r;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});
		assertTrue(exactContains(pPts, "c"));
		assertTrue(exactContains(qPts, "c"));
		assertTrue(exactContains(rPts, "c"));

	}

	/**
	 * Test <code>
	 * struct T {
	 * int x;
	 * int y;
	 * } t;
	 *
	 * void f(int ** p, int ** q) {
	 * *q = &t.y;
	 * f(q, p);
	 * }
	 *
	 * void g(int ** p, int ** q) {
	 * *p = &t.x;
	 * f(p, q);
	 * }
	 *
	 * int main() {
	 *   int *x, *y;
	 *
	 *   g(&x, &y);
	 * }
	 * </code> in main, both x and y can point to t.x and t.y
	 *
	 * @throws ABCException
	 */
	@Test
	public void recursive() throws ABCException {
		AST ast = getAST("recursive");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> xPts, yPts;
		Variable x, y;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		x = variables.get("x");
		y = variables.get("y");
		xPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{x});
		yPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{y});

		assertTrue(exactContains(xPts, "t.x", "t.y"));
		assertTrue(exactContains(yPts, "t.y"));
	}

	/**
	 * <code>
	 *  int *p, *q;
	 *  
	 *  int f(int *a) {
	 *    p = a + 1;
	 *    q = a + 2;
	 *  }
	 *  
	 *  int main() {
	 *    int a[10];
	 *    
	 *    f(a);
	 *  }
	 * </code>
	 * 
	 * @throws ABCException
	 */
	@Test
	public void pointerToArrayArgument() throws ABCException {
		AST ast = getAST("pointerToArrayArgument");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts;
		Variable p, q;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});

		assertTrue(exactContains(pPts, "a[1]"));
		assertTrue(exactContains(qPts, "a[2]"));
	}

	/**
	 * <code>
	 *  int *p, *q, *r;
	 *  
	 *  int f(int *a) {
	 *    p = a + 1;
	 *    q = a + 2;
	 *    r = a + *p;
	 *  }
	 *  
	 *  int main() {
	 *    int a[10];
	 *    
	 *    f(a + 1);
	 *  }
	 * </code>
	 * 
	 * @throws ABCException
	 */
	@Test
	public void pointerToArrayArgument2() throws ABCException {
		AST ast = getAST("pointerToArrayArgument2");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable p, q, r;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});

		assertTrue(exactContains(pPts, "a[2]"));
		assertTrue(exactContains(qPts, "a[3]"));
		assertTrue(exactContains(rPts, "a[*]", "a[3]", "a[2]", "a[1]"));
	}

	/**
	 * <code>
	 *  int *p, *q, *r;
	 *  
	 *  int f(int *a, int b[10]) {
	 *    p = a + 1;
	 *    q = b + 2;
	 *    r = b + *p;
	 *  }
	 *  
	 *  int main() {
	 *    int a[10][10];
	 *    int (*b)[10];
	 *    
	 *    b = a + 1;
	 *    f(a[1] + 1, b[2] + 1);
	 *  }
	 * </code>
	 * 
	 * @throws ABCException
	 */
	@Test
	public void pointerToArrayArgument3() throws ABCException {
		AST ast = getAST("pointerToArrayArgument3");
		Function f;
		Map<String, Variable> variables;
		List<AssignExprIF> pPts, qPts, rPts;
		Variable p, q, r;

		ptAnalyzer = SimplePointsToAnalysisIF.flowInsensePointsToAnalyzer(ast,
				ast.getASTFactory().getTypeFactory());
		f = (Function) ast.getInternalOrExternalEntity("main");
		variables = findVariablesInFunction(f);
		p = variables.get("p");
		q = variables.get("q");
		r = variables.get("r");
		pPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{p});
		qPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{q});
		rPts = ptAnalyzer.mayPointsTo((Function) f, new Entity[]{r});

		assertTrue(exactContains(pPts, "a[1][2]"));
		assertTrue(exactContains(qPts, "a[3][3]"));
		assertTrue(exactContains(rPts, "a[3][*]", "a[3][3]", "a[3][1]"));
	}
}
