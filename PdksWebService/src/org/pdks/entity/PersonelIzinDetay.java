package org.pdks.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

//@Entity
@Entity(name = PersonelIzinDetay.TABLE_NAME)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { PersonelIzinDetay.COLUMN_NAME_IZIN, PersonelIzinDetay.COLUMN_NAME_HAKEDIS_IZIN }) })
public class PersonelIzinDetay extends BasePDKSObject implements Serializable, Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7487009656840444082L;
	static Logger logger = Logger.getLogger(PersonelIzinDetay.class);
	public static final String TABLE_NAME = "PERSONELIZINDETAY";

	public static final String COLUMN_NAME_IZIN = "IZIN_ID";
	public static final String COLUMN_NAME_HAKEDIS_IZIN = "HAKEDIS_IZIN_ID";

	private PersonelIzin hakEdisIzin;

	private PersonelIzin personelIzin;

	// private double izinMiktari;

	public PersonelIzinDetay() {
		super();

	}

	public PersonelIzinDetay(PersonelIzin hakEdisIzin, PersonelIzin personelIzin, double izinMiktari) {
		super();
		this.hakEdisIzin = hakEdisIzin;
		this.personelIzin = personelIzin;
		// this.izinMiktari = izinMiktari;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = PersonelIzinDetay.COLUMN_NAME_HAKEDIS_IZIN, nullable = false)
	@Fetch(FetchMode.JOIN)
	public PersonelIzin getHakEdisIzin() {
		return hakEdisIzin;
	}

	public void setHakEdisIzin(PersonelIzin hakEdisIzin) {
		this.hakEdisIzin = hakEdisIzin;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = PersonelIzinDetay.COLUMN_NAME_IZIN, nullable = false)
	@Fetch(FetchMode.JOIN)
	public PersonelIzin getPersonelIzin() {
		return personelIzin;
	}

	public void setPersonelIzin(PersonelIzin personelIzin) {
		this.personelIzin = personelIzin;
	}

	// @Column(name = "IZIN_MIKTARI", precision = 2, scale = 2)
	// public double getIzinMiktari() {
	// return izinMiktari;
	// }
	//
	// public void setIzinMiktari(double izinMiktari) {
	// this.izinMiktari = izinMiktari;
	// }

	@Transient
	public double getIzinMiktari() {
		return personelIzin != null && personelIzin.getIzinSuresi() != null ? personelIzin.getIzinSuresi().doubleValue() : 0.0d;
	}

	@Transient
	public static String getHakEdisIzinKeyStr(PersonelIzin hakEdisIzinGelen, PersonelIzin personelIzinGelen) {
		return (hakEdisIzinGelen != null ? hakEdisIzinGelen.getId() : 0L) + "_" + (personelIzinGelen != null ? personelIzinGelen.getId() : 0L);
	}

	@Transient
	public Long getPersonelIzinId() {
		return personelIzin != null ? personelIzin.getId() : 0L;
	}

	@Transient
	public String getHakEdisIzinKey() {
		return getHakEdisIzinKeyStr(hakEdisIzin, personelIzin);
	}

	@Transient
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			// bu class cloneable oldugu icin buraya girilmemeli...
			throw new InternalError();
		}
	}

}
