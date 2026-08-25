package dev.civl.mc;

import static dev.civl.mc.TestConstants.NO_PRINTF;
import static dev.civl.mc.TestConstants.QUIET;
import static dev.civl.mc.TestConstants.VERIFY;
import static dev.civl.mc.TestConstants.errorBound;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.abc.err.IF.ABCException;
import dev.civl.mc.run.IF.UserInterface;

public class BackendTest {
	@SuppressWarnings("exports")
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File(new File("examples"), "backend");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods ************************** */

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	/* **************************** Test Methods *************************** */
	@Test
	public void printExpr() {
		assertTrue(ui.run(VERIFY, QUIET, filename("printExpr.cvl")));
	}

	@Test
	public void arrayWrite() {
		assertTrue(ui.run(VERIFY, QUIET, filename("arrayWrite.cvl")));
	}

	@Test
	public void showTrans() {
		assertTrue(ui.run(VERIFY, QUIET, filename("showTrans.cvl")));
	}

	@Test
	public void sizeOfTypes() {
		assertTrue(ui.run(VERIFY, QUIET, filename("sizeOfTypes.c")));
	}

	@Test
	public void symbolicConsts() {
		assertFalse(ui.run(VERIFY, QUIET, filename("symbolicConstants.cvl")));
	}

	@Test
	public void simplifyAbstractFunction() {
		assertTrue(ui.run(VERIFY, QUIET, filename("simplifyAbstractFunction2Concrete.cvl")));
	}

	@Test
	public void returnNull() throws ABCException {
		assertFalse(ui.run(VERIFY, errorBound(2), NO_PRINTF, QUIET, filename("returnNull.cvl")));
	}

	@Test
	public void quantified() {
		assertTrue(ui.run(VERIFY, QUIET, filename("quantified.cvl")));
	}

	@Test
	public void symbols() {
		assertTrue(ui.run(VERIFY, QUIET, filename("symbols.cvl")));
	}

	@Test
	public void binaryGuard() {
		assertTrue(ui.run(VERIFY, QUIET, filename("binaryGuard.cvl")));
	}

	@Test
	public void atomicBlocking() {
		assertFalse(ui.run(VERIFY, QUIET, filename("atomicExample.c")));
	}

	@Test
	public void atomicBlocking2() {
		assertFalse(ui.run(VERIFY, QUIET, filename("atomic2.c")));
	}

	@Test
	public void arrayLambda() {
		assertTrue(ui.run(VERIFY, QUIET, filename("arrayLambda.cvl")));
	}

	@Test
	public void sizeof() {
		ui.run(VERIFY, QUIET, filename("sizeof.cvl"));
	}

	@Test
	public void dynamicTypesCompatiableForAssignment() {
		assertTrue(ui.run(VERIFY, QUIET, filename("nonscalar_assignment.cvl")));
	}

	@Test
	public void dynamicTypesCompatiableForAssignment2() {
		assertTrue(ui.run(VERIFY, QUIET, filename("nonscalar_assignment2.cvl")));
	}

	@Test
	public void dynamicTypesCompatiableForAssignmentBad() {
		assertFalse(ui.run(VERIFY, QUIET, filename("nonscalar_assignment-bad.cvl")));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ui = null;
		rootDir = null;
	}
}
