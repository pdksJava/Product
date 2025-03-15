package org.pdks.kgs.model;

import java.io.Serializable;

public class CihazPersonel implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5454911434851152996L;

	private Long id;

	private String adi;

	private String soyadi;

	private String personelNo;

	private String kimlikNo;

	private Integer durum = Durum.AKTIF.value();

	public CihazPersonel() {
		super();
	}

	public CihazPersonel(Long id, String adi, String soyadi, String personelNo) {
		super();
		this.id = id;
		this.adi = adi;
		this.soyadi = soyadi;
		this.personelNo = personelNo;

	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAdi() {
		return adi;
	}

	public void setAdi(String adi) {
		this.adi = adi;
	}

	public String getSoyadi() {
		return soyadi;
	}

	public void setSoyadi(String soyadi) {
		this.soyadi = soyadi;
	}

	public String getPersonelNo() {
		return personelNo;
	}

	public void setPersonelNo(String personelNo) {
		this.personelNo = personelNo;
	}

	public String getKimlikNo() {
		return kimlikNo;
	}

	public void setKimlikNo(String kimlikNo) {
		this.kimlikNo = kimlikNo;
	}

	public Integer getDurum() {
		return durum;
	}

	public void setDurum(Integer durum) {
		this.durum = durum;
	}

}
