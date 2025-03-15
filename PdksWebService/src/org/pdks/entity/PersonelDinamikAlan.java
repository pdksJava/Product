package org.pdks.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.pdks.genel.model.PdksUtil;

@Entity(name = PersonelDinamikAlan.TABLE_NAME)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { PersonelDinamikAlan.COLUMN_NAME_PERSONEL, PersonelDinamikAlan.COLUMN_NAME_ALAN }) })
public class PersonelDinamikAlan extends BasePDKSObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3881392269334061361L;

	public static final String TABLE_NAME = "PERSONEL_DINAMIK_ALAN";

	public static final String COLUMN_NAME_PERSONEL = "PERSONEL_ID";
	public static final String COLUMN_NAME_ALAN = "ALAN_ID";
	public static final String COLUMN_NAME_TANIM_DEGER = "TANIM_DEGER_ID";
	public static final String COLUMN_NAME_DURUM_SECIM = "DURUM_SECIM";
	public static final String COLUMN_NAME_SAYISAL_DEGER = "SAYISAL_DEGER";

	public PersonelDinamikAlan(Personel personel, Tanim alan) {
		super();
		this.personel = personel;
		this.alan = alan;
		if (alan != null) {
			if (alan.getTipi().equals(Tanim.TIPI_PERSONEL_DINAMIK_DURUM))
				this.durumSecim = alan.getErpKodu() != null && alan.getErpKodu().equals("1");
		}
	}

	public PersonelDinamikAlan() {
		super();

	}

	private Personel personel;

	private Tanim alan, tanimDeger;

	private Boolean durumSecim = Boolean.FALSE;

	private Double sayisalDeger;

	private Integer version = 0;

	@Column(name = "VERSION")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_PERSONEL, nullable = false)
	@Fetch(FetchMode.JOIN)
	public Personel getPersonel() {
		return personel;
	}

	public void setPersonel(Personel personel) {
		this.personel = personel;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_ALAN, nullable = false)
	@Fetch(FetchMode.JOIN)
	public Tanim getAlan() {
		return alan;
	}

	public void setAlan(Tanim alan) {
		this.alan = alan;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_TANIM_DEGER)
	@Fetch(FetchMode.JOIN)
	public Tanim getTanimDeger() {
		return tanimDeger;
	}

	public void setTanimDeger(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(tanimDeger, value));
		this.tanimDeger = value;
	}

	@Column(name = COLUMN_NAME_DURUM_SECIM)
	public Boolean getDurumSecim() {
		return durumSecim;
	}

	@Column(name = COLUMN_NAME_SAYISAL_DEGER)
	public Double getSayisalDeger() {
		return sayisalDeger;
	}

	public void setSayisalDeger(Double value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDoubleDegisti(sayisalDeger, value));
		this.sayisalDeger = value;
	}

	public void setDurumSecim(Boolean value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isBooleanDegisti(durumSecim, value));
		this.durumSecim = value;
	}

	@Transient
	public static String getKey(Personel personel, Tanim alan) {
		String key = (personel != null ? personel.getId() : "") + "_" + (alan != null ? alan.getId() : "");
		return key;
	}

	@Transient
	public String getKey() {
		String key = getKey(personel, alan);
		return key;
	}

	@Transient
	public boolean isDurumSecili() {
		return durumSecim != null && durumSecim.booleanValue();
	}

}
