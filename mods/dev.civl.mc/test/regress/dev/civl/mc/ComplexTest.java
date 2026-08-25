package dev.civl.mc;

import static dev.civl.mc.TestConstants.QUIET;
import static dev.civl.mc.TestConstants.VERIFY;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import dev.civl.mc.run.IF.UserInterface;

public class ComplexTest {
	@SuppressWarnings("exports")
	@Rule
	public Timeout globalTimeout = Timeout.seconds(30);

	/* *************************** Static Fields *************************** */

	private static File rootDir = new File(new File("examples"), "complex");

	private static UserInterface ui = new UserInterface();

	/* *************************** Helper Methods ************************** */

	private void check(String name) {
		assertTrue(ui.run(VERIFY, QUIET, filename(name)));
	}

	private static String filename(String name) {
		return new File(rootDir, name).getPath();
	}

	/* **************************** Test Methods *************************** */

	@Test
	public void casts() {
		check("casts.cvl");
	}

	@Test
	public void complex_basics() {
		check("complex_basics.c");
	}

	@Test
	public void complex_funs() {
		check("complex_funs.c");
	}

	@Test
	public void complex1() {
		check("complex1.cvl");
	}

	@Test
	public void complex_ops() {
		check("complex_ops.c");
	}

	@Test
	public void unity() {
		check("unity.cvl");
	}

	@Test
	public void complex_quant() {
		check("complex_quant.cvl");
	}

	@Test
	public void mpi_complex() {
		assertTrue(ui.run(VERIFY, QUIET, "-input_mpi_nprocs=2", filename("mpi_complex.cvl")));
	}

	@Test
	public void fourth() {
		assertTrue(ui.run(VERIFY, QUIET, "-input_mpi_nprocs=4", filename("fourth.c")));
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		ui = null;
		rootDir = null;
	}
}
