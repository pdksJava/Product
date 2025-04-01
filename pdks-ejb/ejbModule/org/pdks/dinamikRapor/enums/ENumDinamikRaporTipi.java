/**
 * 
 */
package org.pdks.dinamikRapor.enums;

public enum ENumDinamikRaporTipi {

	VIEW(1), FUNCTION(2), STORE_PROCEDURE(3);

	private final Integer value;

	ENumDinamikRaporTipi(Integer v) {
		value = v;
	}

	public Integer value() {
		return value;
	}

	public static ENumDinamikRaporTipi fromValue(Integer v) {
		ENumDinamikRaporTipi deger = null;
		for (ENumDinamikRaporTipi c : ENumDinamikRaporTipi.values()) {
			if (c.value.equals(v)) {
				deger = c;
			}
		}
		return deger;
	}

}
