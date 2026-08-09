package dev.civl.mc.transform;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.abc.err.IF.ABCException;
import dev.civl.mc.TestConstants;
import dev.civl.mc.run.IF.UserInterface;

public class SideEffectsTest {
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File(new File("examples"), "sideEffects");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods *************************** */

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	/* **************************** Test Methods *************************** */

	@Test
	public void postIncr() throws ABCException {
		assertTrue(ui.run("verify ", TestConstants.QUIET, filename("postIncr.cvl")));
	}

	@Test
	public void forLoopIncrSE() throws ABCException {
		assertTrue(ui.run("verify ", TestConstants.QUIET, filename("forLoopIncretSE.c")));
	}

	@Test
	public void strictInitTest() throws ABCException {
		assertTrue(ui.run("verify ", TestConstants.QUIET, filename("structInitSideEffect.c")));
	}

	@Test
	public void quantifiedExpressionTest() throws ABCException {
		assertFalse(ui.run("verify ", TestConstants.QUIET, filename("quantifiedSideEffects.c")));
	}

	@Test
	public void structWithDiv() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("structWithDiv.cvl")));
	}

	@Test
	public void simpleShortCircuit() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("trivilShortCircuits.cvl")));
	}

	@Test
	public void complextShortCircuitExpression() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("complexShortCircuitExpressions.cvl")));
	}

	@Test
	public void complextShortCircuitLoopCondition() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("complexShortCircuitLoopConditions.cvl")));
	}

	@Test
	public void nestedShortCircuitLoopCondition() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("nestedShortCircuitLoopConditions.cvl")));
	}

	@Test
	public void errSideEffectInGuard() {
		assertFalse(ui.run("verify", TestConstants.QUIET, filename("errSideEffectsInGuards.cvl")));
	}

	// The following two tests require SARL's support of free of error
	// side-effects:
	@Ignore
	public void errSideEffectInQuantified() {
		assertFalse(ui.run("verify", TestConstants.QUIET, filename("errSideEffectsInQuantified.cvl")));
	}

	@Ignore
	public void errSideEffectInQuantifiedButOK() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("errSideEffectsInQuantifiedButOK.cvl")));
	}

	@Test
	public void compoundLiteral() {
		assertTrue(ui.run("verify", TestConstants.QUIET, filename("compoundLiteral.cvl")));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ui = null;
		rootDir = null;
	}
}
