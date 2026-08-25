package dev.civl.sarl.IF.config;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import dev.civl.sarl.IF.SARLException;
import dev.civl.sarl.config.common.CommonSARLConfig;
import dev.civl.sarl.config.common.ConfigFactory;

/**
 * This is the public interface for managing SARL configuration objects and
 * files.
 * 
 * @author siegel
 *
 */
public class Configurations {

	/**
	 * <p>
	 * The single, global, default SARL configuration object. Initially
	 * <code>null</code>, it is used to cache the result returned by method
	 * {@link #getDefaultConfiguration()}.
	 * </p>
	 * 
	 * <p>
	 * The value is computed by first looking in the current working directory for a
	 * file named <code>.sarl</code>. If no file by that name is found, it looks in
	 * the user's home directory for <code>.sarl</code>. If that is not found, it
	 * looks in the current working directory for a file named
	 * <code>.sarl_default</code>. If that is not found, it looks in the home
	 * directory for <code>.sarl_default</code>.
	 * </p>
	 * 
	 * <p>
	 * If no configuration file is found, this variable will be <code>null</code>.
	 * If the file is found, but it has syntax errors, or there is some I/O problem
	 * when reading it, the application will terminate immediately with a non-0
	 * error code.
	 * </p>
	 */
	private static SARLConfig DEFAULT_CONFIG = null;

	/**
	 * Creates a new {@link SARLConfig} object based on the given list of prover
	 * information objects.
	 * 
	 * @param provers ordered collection of prover infos
	 * @return resulting {@link SARLConfig} object wrapping those provers
	 */
	public static SARLConfig newConfiguration(Collection<ProverInfo> provers) {
		return new CommonSARLConfig(provers);
	}

	/**
	 * <p>
	 * Parses the specified SARL configuration file.
	 * </p>
	 * 
	 * @param configFile a SARL configuration file
	 * @return the {@link SARLConfig} object resulting from parsing the file
	 * @throws SARLException if the file is not found, or an I/O error occurs, or if
	 *                       the configuration file does not conform to the syntax
	 *                       of SARL configuration files
	 */
	public static SARLConfig newConfiguration(File configFile) throws SARLException {
		try {
			return ConfigFactory.fromFile(configFile);
		} catch (IOException e) {
			throw new SARLException("I/O error reading SARL configuration file " + configFile + ": " + e.getMessage());
		}
	}

	/**
	 * <p>
	 * Looks for a SARL configuration file, and, if one is not found, creates one in
	 * the user's home directory. In the case where the file was not found, the new
	 * configuration file will be named <code>.sarl</code>, and it will be created
	 * by searching the user's <code>PATH</code> for appropriate theorem provers. In
	 * either case, the configuration file is parsed, and the resulting
	 * {@link SARLConfig} object is returned. The public static variable
	 * {@link #DEFAULT_CONFIG} will also be updated.
	 * </p>
	 * 
	 * <p>
	 * If the configuration file has syntax errors, or there is some I/O problem
	 * when reading it, the application will execute immediately with a non-0 error
	 * code.
	 * </p>
	 * 
	 * @return the {@link SARLConfig} object obtained by parsing the configuration
	 *         file
	 */
	public static SARLConfig getDefaultConfiguration() {
		if (DEFAULT_CONFIG != null)
			return DEFAULT_CONFIG;
		try {
			DEFAULT_CONFIG = ConfigFactory.findConfig();
			if (DEFAULT_CONFIG == null) {
				ConfigFactory.makeConfigFile();
				DEFAULT_CONFIG = ConfigFactory.findConfig();
			} else {
				ConfigFactory.checkConfig(DEFAULT_CONFIG);
			}
			if (DEFAULT_CONFIG.getNumProvers() == 0) {
				// TODO: Shouldn't have to hardcode what provers we support in
				// this warning message.
				System.err.println("Warning: using SARL with no theorem provers. "
						+ "Consider putting Z3, CVC4, CVC5, Alt-Ergo, or Vampire in your PATH.");
				System.err.flush();
			}
		} catch (Exception e) {
			System.err.println("SARL configuration error: " + e.getMessage());
			System.err.flush();
			System.exit(1);
		}
		return DEFAULT_CONFIG;
	}

	/**
	 * Makes a new SARL configuration file in the user's home directory. If there is
	 * already a file named <code>.sarl</code> in that directory, it is moved to
	 * <code>.sarl.old</code>. This method searches for theorem provers on the
	 * user's system in order to create the file. If anything goes wrong, it does
	 * not throw an exception, but terminates immediately with a non-0 exit code.
	 * 
	 */
	public static void makeConfigFile() {
		try {
			ConfigFactory.makeConfigFile();
		} catch (Exception e) {
			System.err.println("Unable to create SARL config file: " + e.getMessage());
			System.err.flush();
			System.exit(2);
		}
	}

}
