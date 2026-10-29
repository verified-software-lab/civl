package dev.civl.abc.ast.value.IF;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface RealFloatingValue extends FloatingValue {

	int getRadix();

	double getDoubleValue();

	/**
	 * Returns the exact value of this constant as a {@link BigDecimal}, computed
	 * directly from the integer parts rather than through a {@code double}, so
	 * that no precision is lost.
	 *
	 * @return the exact value of this floating point constant
	 */
	BigDecimal getBigDecimalValue();

	BigInteger getWholePartValue();

	BigInteger getFractionPartValue();

	int getFractionLength();

	BigInteger getExponentValue();

}
