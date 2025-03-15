package org.pdks.entity;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;
import org.pdks.genel.model.PdksUtil;
import org.pdks.security.entity.User;

@Entity(name = Personel.TABLE_NAME)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { Personel.COLUMN_NAME_KGS_PERSONEL }) })
public class Personel extends BaseObject {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3135466261052396262L;

	static Logger logger = Logger.getLogger(Personel.class);

	public static final String TABLE_NAME = "PERSONEL";
	public static final String VIEW_YONETICI_KONTRATLI = "YONETICI_KONTRATLI_VIEW";
	public static final String VIEW_YONETICI_KONTRATLI_AYRILMIS = "YONETICI_KONTRATLI_AYRILMIS_VIEW";

	public static final String COLUMN_NAME_KGS_PERSONEL = "KGS_PERSONEL_ID";
	public static final String COLUMN_NAME_PDKS_SICIL_NO = "PDKS_SICIL_NO";
	public static final String COLUMN_NAME_SIRKET = "SIRKET_ID";
	public static final String COLUMN_NAME_TESIS = "TESIS_ID";
	public static final String COLUMN_NAME_YONETICI = "YONETICI_ID";
	public static final String COLUMN_NAME_ISE_BASLAMA_TARIHI = "ISE_BASLAMA_TARIHI";
	public static final String COLUMN_NAME_ISTEN_AYRILIS_TARIHI = "ISTEN_AYRILIS_TARIHI";
	public static final String COLUMN_NAME_GRUBA_GIRIS_TARIHI = "GRUBA_GIRIS_TARIHI";
	public static final String COLUMN_NAME_EK_SAHA1 = "EK_SAHA1_ID";
	public static final String COLUMN_NAME_EK_SAHA2 = "EK_SAHA2_ID";
	public static final String COLUMN_NAME_EK_SAHA3 = "EK_SAHA3_ID";
	public static final String COLUMN_NAME_EK_SAHA4 = "EK_SAHA4_ID";
	public static final String COLUMN_NAME_AD = "AD";
	public static final String COLUMN_NAME_SOYAD = "SOYAD";
	public static final String COLUMN_NAME_SUA_OLABILIR = "SUA_OLABILIR";
	public static final String COLUMN_NAME_FAZLA_MESAI_ODE = "FAZLA_MESAI_ODE";
	public static final String COLUMN_NAME_SANAL_PERSONEL = "SANAL_PERSONEL";
	public static final String COLUMN_NAME_FAZLA_MESAI_IZIN_KULLAN = "FAZLA_MESAI_IZIN_KULLAN";
	public static final String COLUMN_NAME_MAIL_CC_ID = "MAIL_CC_ID";
	public static final String COLUMN_NAME_MAIL_BCC_ID = "MAIL_BCC_ID";
	public static final String COLUMN_NAME_HAREKET_MAIL_ID = "MAIL_HAREKET_ID";
	public static final String COLUMN_NAME_IZIN_HAKEDIS_TARIHI = "IZIN_HAKEDIS_TARIHI";
	public static final String COLUMN_NAME_DOGUM_TARIHI = "DOGUM_TARIHI";
	public static final String COLUMN_NAME_SSK_CIKIS_TARIHI = "SSK_CIKIS_TARIHI";
	public static final String COLUMN_NAME_CALISMA_MODELI = "CALISMA_MODELI_ID";
	public static final String COLUMN_NAME_PERSONEL_TIPI = "PERSONEL_TIPI_ID";
	public static final String COLUMN_NAME_GOREV_TIPI = "GOREV_TIPI_ID";
	public static final String COLUMN_NAME_SABLON = "SABLON_ID";
	public static final String COLUMN_NAME_IZIN_KARTI_VAR = "IZIN_KARTI_VAR";

	public static final String COLUMN_NAME_MAIL_TAKIP = "MAIL_TAKIP";

	public static final String STATU_HEKIM = "2";

	public static final String STATU_KONTRATLI_HEKIM = "11";

	public static final String BOLUM_SUPERVISOR = "SUPERV";

	public static final String MASRAF_YERI_GENEL_DIREKTOR = "310000";

	public static String grubaGirisTarihiAlanAdi = COLUMN_NAME_ISE_BASLAMA_TARIHI;
	public static String altBolumGrupGoster = "ABG";
	// seam-gen attributes (you should probably edit these)
	private String ad = "", soyad = "", erpSicilNo = "", pdksSicilNo, sortAlanAdi = "";

	private PersonelKGS personelKGS;
	private VardiyaSablonu sablon;
	private Sirket sirket;
	private CalismaModeli calismaModeli;
	private Tanim gorevTipi, ekSaha1, ekSaha2, ekSaha3, ekSaha4, tesis, masrafYeri, ekSaha, cinsiyet, bordroAltAlan, personelTipi;
	private Boolean pdks = Boolean.FALSE, mailTakip = Boolean.FALSE, icapciOlabilir = Boolean.FALSE, ustYonetici = Boolean.FALSE, sutIzni = Boolean.FALSE;
	private Boolean suaOlabilir = Boolean.FALSE, fazlaMesaiIzinKullan = Boolean.FALSE, sanalPersonel = Boolean.FALSE, onaysizIzinKullanilir = Boolean.FALSE, egitimDonemi = Boolean.FALSE;
	private Boolean partTime = Boolean.FALSE, ikinciYoneticiIzinOnayla = Boolean.FALSE, fazlaMesaiOde = Boolean.FALSE, izinKartiVar = Boolean.FALSE, gebeMi = Boolean.FALSE, mudurAltSeviye;
	private Personel yoneticisi, asilYonetici1, asilYonetici2, pdksYonetici, tmpYonetici;
	private Date izinHakEdisTarihi, iseBaslamaTarihi, grubaGirisTarihi, istenAyrilisTarihi = PdksUtil.getSonSistemTarih(), sskCikisTarihi, dogumTarihi;
	private VardiyaSablonu workSablon;
	private PersonelIzin personelIzin;
	private PersonelView personelView;
	private MailGrubu mailGrubuCC, mailGrubuBCC, hareketMailGrubu;
	private String emailCC = "", emailBCC = "", hareketMail = "";
	private List<PersonelIzin> personelIzinList;
	private List<VardiyaGun> personelVardiyalari;
	private double fazlaMesaiIzin = 0, senelikIzin = 0;

	// private double mazeretIzin = 0, icEgitimIzin = 0, disEgitimIzin = 0;
	private HashMap<String, Double> izinBakiyeMap;
	private double toplamKalanIzin = 0;

	private User kullanici;
	private Integer version = 0;

	public Personel() {
		super();
		this.setPdks(true);
		this.setDegisti(false);
	}

	@Column(name = "VERSION")
	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_KGS_PERSONEL, nullable = false)
	@Fetch(FetchMode.JOIN)
	public PersonelKGS getPersonelKGS() {
		return personelKGS;
	}

	public void setPersonelKGS(PersonelKGS value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(personelKGS != null ? personelKGS.getId() : null, value != null ? value.getId() : null));
		this.personelKGS = value;
	}

	@Column(name = COLUMN_NAME_AD)
	public String getAd() {
		return ad;
	}

	public void setAd(String value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isStrDegisti(ad, value));
		value = PdksUtil.convertUTF8(value);
		this.ad = value;
	}

	@Column(name = COLUMN_NAME_SOYAD)
	public String getSoyad() {

		return soyad;
	}

	@Column(name = COLUMN_NAME_PDKS_SICIL_NO, length = 15)
	public String getPdksSicilNo() {
		return pdksSicilNo;
	}

	public void setPdksSicilNo(String value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isStrDegisti(pdksSicilNo, value));
		this.pdksSicilNo = value;
	}

	public void setSoyad(String value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isStrDegisti(soyad, value));
		value = PdksUtil.convertUTF8(value);
		this.soyad = value;
	}

	@Transient
	public String getAdSoyad() {
		String adSoyad = PdksUtil.getAdSoyad(ad, soyad);
		return adSoyad;
	}

	@Transient
	public String getSicilNo() {
		String kSicilNo = pdksSicilNo;
		if (PdksUtil.hasStringValue(kSicilNo) == false)
			kSicilNo = personelKGS != null ? personelKGS.getSicilNo() : "";
		String sicilNo = !PdksUtil.hasStringValue(erpSicilNo) ? kSicilNo : erpSicilNo;
		return sicilNo.trim();
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_SIRKET, nullable = false)
	@Fetch(FetchMode.JOIN)
	public Sirket getSirket() {
		return sirket;
	}

	public void setSirket(Sirket value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(sirket != null ? sirket.getId() : null, value != null ? value.getId() : null));
		this.sirket = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "CALISMA_MODELI_ID")
	@Fetch(FetchMode.JOIN)
	public CalismaModeli getCalismaModeli() {
		return calismaModeli;
	}

	public void setCalismaModeli(CalismaModeli value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(calismaModeli != null ? calismaModeli.getId() : null, value != null ? value.getId() : null));
		this.calismaModeli = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = COLUMN_NAME_GOREV_TIPI)
	@Fetch(FetchMode.JOIN)
	public Tanim getGorevTipi() {
		return gorevTipi;
	}

	public void setGorevTipi(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(gorevTipi, value));
		this.gorevTipi = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "EK_SAHA1_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getEkSaha1() {
		return ekSaha1;
	}

	public void setEkSaha1(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(ekSaha1, value));
		this.ekSaha1 = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "EK_SAHA2_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getEkSaha2() {
		return ekSaha2;
	}

	public void setEkSaha2(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(ekSaha2, value));
		this.ekSaha2 = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "EK_SAHA3_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getEkSaha3() {
		return ekSaha3;
	}

	public void setEkSaha3(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(ekSaha3, value));
		this.ekSaha3 = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "EK_SAHA4_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getEkSaha4() {
		return ekSaha4;
	}

	public void setEkSaha4(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(ekSaha4, value));
		this.ekSaha4 = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "BORDRO_ALT_BIRIMI")
	@Fetch(FetchMode.JOIN)
	public Tanim getBordroAltAlan() {
		return bordroAltAlan;
	}

	public void setBordroAltAlan(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(bordroAltAlan, value));
		this.bordroAltAlan = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = COLUMN_NAME_PERSONEL_TIPI)
	@Fetch(FetchMode.JOIN)
	public Tanim getPersonelTipi() {
		return personelTipi;
	}

	public void setPersonelTipi(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(personelTipi, value));
		this.personelTipi = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "CINSIYET_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getCinsiyet() {
		return cinsiyet;
	}

	public void setCinsiyet(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(cinsiyet, value));
		this.cinsiyet = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = COLUMN_NAME_TESIS)
	@Fetch(FetchMode.JOIN)
	public Tanim getTesis() {
		return tesis;
	}

	public void setTesis(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(tesis, value));
		this.tesis = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = "MASRAF_YERI_ID")
	@Fetch(FetchMode.JOIN)
	public Tanim getMasrafYeri() {
		return masrafYeri;
	}

	public void setMasrafYeri(Tanim value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isTanimDegisti(masrafYeri, value));
		this.masrafYeri = value;
	}

	@Column(name = COLUMN_NAME_MAIL_TAKIP)
	public Boolean getMailTakip() {
		return mailTakip;
	}

	public void setMailTakip(Boolean value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isBooleanDegisti(mailTakip, value));
		this.mailTakip = value;
	}

	@Column(name = "GEBE_MI")
	public Boolean getGebeMi() {
		return gebeMi;
	}

	public void setGebeMi(Boolean gebeMi) {
		this.gebeMi = gebeMi;
	}

	@Column(name = "SUT_IZNI")
	public Boolean getSutIzni() {
		return sutIzni;
	}

	public void setSutIzni(Boolean sutIzni) {
		this.sutIzni = sutIzni;
	}

	@ManyToOne(cascade = CascadeType.REFRESH, fetch = FetchType.LAZY)
	@JoinColumn(name = COLUMN_NAME_SABLON)
	@Fetch(FetchMode.JOIN)
	public VardiyaSablonu getSablon() {
		return sablon;
	}

	public void setSablon(VardiyaSablonu value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(sablon != null ? sablon.getId() : null, value != null ? value.getId() : null));
		this.sablon = value;
	}

	@Column(name = "PDKS")
	public Boolean getPdks() {
		return pdks;
	}

	public void setPdks(Boolean value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isBooleanDegisti(pdks, value));
		this.pdks = value;
	}

	@Column(name = "UST_YONETICIMI")
	public Boolean getUstYonetici() {
		return ustYonetici;
	}

	public void setUstYonetici(Boolean ustYonetici) {
		this.ustYonetici = ustYonetici;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_YONETICI)
	@Fetch(FetchMode.JOIN)
	public Personel getYoneticisi() {
		return yoneticisi;
	}

	public void setYoneticisi(Personel value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(yoneticisi != null ? yoneticisi.getId() : null, value != null ? value.getId() : null));
		this.yoneticisi = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "ASIL_YONETICI1_ID")
	@Fetch(FetchMode.JOIN)
	public Personel getAsilYonetici1() {
		return asilYonetici1;
	}

	public void setAsilYonetici1(Personel value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(asilYonetici1 != null ? asilYonetici1.getId() : null, value != null ? value.getId() : null));
		this.asilYonetici1 = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = "ASIL_YONETICI2_ID")
	@Fetch(FetchMode.JOIN)
	public Personel getAsilYonetici2() {
		return asilYonetici2;
	}

	public void setAsilYonetici2(Personel value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isLongDegisti(asilYonetici2 != null ? asilYonetici2.getId() : null, value != null ? value.getId() : null));
		this.asilYonetici2 = value;
	}

	@Transient
	public String getErpSicilNo() {
		return erpSicilNo;
	}

	public void setErpSicilNo(String erpSicilNo) {
		this.erpSicilNo = erpSicilNo;
	}

	@Transient
	public User getKullanici() {
		return kullanici;
	}

	public void setKullanici(User kullanici) {
		this.kullanici = kullanici;
	}

	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_ISE_BASLAMA_TARIHI)
	public Date getIseBaslamaTarihi() {
		return iseBaslamaTarihi;
	}

	public void setIseBaslamaTarihi(Date value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDateDegisti(iseBaslamaTarihi, value));
		this.iseBaslamaTarihi = value;
	}

	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_GRUBA_GIRIS_TARIHI)
	public Date getGrubaGirisTarihi() {
		return grubaGirisTarihi;
	}

	public void setGrubaGirisTarihi(Date value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDateDegisti(grubaGirisTarihi, value));
		this.grubaGirisTarihi = value;
	}

	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_ISTEN_AYRILIS_TARIHI)
	public Date getIstenAyrilisTarihi() {
		return istenAyrilisTarihi;
	}

	public void setIstenAyrilisTarihi(Date value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDateDegisti(istenAyrilisTarihi, value));
		this.istenAyrilisTarihi = value;
	}

	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_DOGUM_TARIHI)
	public Date getDogumTarihi() {
		return dogumTarihi;
	}

	public void setDogumTarihi(Date value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDateDegisti(dogumTarihi, value));
		this.dogumTarihi = value;
	}

	@Generated(value = GenerationTime.ALWAYS)
	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_SSK_CIKIS_TARIHI, insertable = false, updatable = false)
	public Date getSskCikisTarihi() {
		return sskCikisTarihi;
	}

	public void setSskCikisTarihi(Date sskCikisTarihi) {
		this.sskCikisTarihi = sskCikisTarihi;
	}

	@Temporal(value = TemporalType.DATE)
	@Column(name = COLUMN_NAME_IZIN_HAKEDIS_TARIHI)
	public Date getIzinHakEdisTarihi() {
		return izinHakEdisTarihi;
	}

	public void setIzinHakEdisTarihi(Date value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isDateDegisti(izinHakEdisTarihi, value));
		this.izinHakEdisTarihi = value;
	}

	@Column(name = "ICAP_OLABILIR")
	public Boolean getIcapciOlabilir() {
		return icapciOlabilir != null && icapciOlabilir;
	}

	public void setIcapciOlabilir(Boolean icapciOlabilir) {
		this.icapciOlabilir = icapciOlabilir;
	}

	@Column(name = "ONAYSIZ_IZIN_KULLANIR")
	public Boolean getOnaysizIzinKullanilir() {
		return onaysizIzinKullanilir;
	}

	public void setOnaysizIzinKullanilir(Boolean onaysizIzinKullanilir) {
		this.onaysizIzinKullanilir = onaysizIzinKullanilir;
	}

	@Column(name = "PART_TIME")
	public Boolean getPartTime() {
		return partTime;
	}

	public void setPartTime(Boolean partTime) {
		this.partTime = partTime;
	}

	@Column(name = "EGITIM_DONEMI")
	public Boolean getEgitimDonemi() {
		return egitimDonemi;
	}

	public void setEgitimDonemi(Boolean egitimDonemi) {
		this.egitimDonemi = egitimDonemi;
	}

	@Column(name = COLUMN_NAME_SANAL_PERSONEL)
	public Boolean getSanalPersonel() {
		return sanalPersonel;
	}

	public void setSanalPersonel(Boolean value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isBooleanDegisti(sanalPersonel, value));
		this.sanalPersonel = value;
	}

	@Transient
	public boolean isSanalPersonelMi() {
		return sanalPersonel != null && sanalPersonel;
	}

	@Column(name = COLUMN_NAME_FAZLA_MESAI_IZIN_KULLAN)
	public Boolean getFazlaMesaiIzinKullan() {
		return fazlaMesaiIzinKullan != null && fazlaMesaiIzinKullan;
	}

	public void setFazlaMesaiIzinKullan(Boolean fazlaMesaiIzinKullan) {
		this.fazlaMesaiIzinKullan = fazlaMesaiIzinKullan;
	}

	@Column(name = COLUMN_NAME_SUA_OLABILIR)
	public Boolean getSuaOlabilir() {
		return suaOlabilir;
	}

	public void setSuaOlabilir(Boolean suaOlabilir) {
		this.suaOlabilir = suaOlabilir;
	}

	@Column(name = "IKINCI_YONETICI_IZIN_ONAYLA")
	public Boolean getIkinciYoneticiIzinOnayla() {
		return ikinciYoneticiIzinOnayla;
	}

	public void setIkinciYoneticiIzinOnayla(Boolean ikinciYoneticiIzinOnayla) {
		this.ikinciYoneticiIzinOnayla = ikinciYoneticiIzinOnayla;
	}

	@Column(name = COLUMN_NAME_FAZLA_MESAI_ODE)
	public Boolean getFazlaMesaiOde() {
		return fazlaMesaiOde;
	}

	public void setFazlaMesaiOde(Boolean value) {
		if (this.isDegisti() == false)
			this.setDegisti(PdksUtil.isBooleanDegisti(fazlaMesaiOde, value));
		this.fazlaMesaiOde = value;
	}

	@Column(name = COLUMN_NAME_IZIN_KARTI_VAR)
	public Boolean getIzinKartiVar() {
		return izinKartiVar;
	}

	public void setIzinKartiVar(Boolean izinKartiVar) {
		this.izinKartiVar = izinKartiVar;
	}

	@OneToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_MAIL_CC_ID)
	@Fetch(FetchMode.JOIN)
	public MailGrubu getMailGrubuCC() {
		return mailGrubuCC;
	}

	public void setMailGrubuCC(MailGrubu value) {
		this.emailCC = value != null ? value.getEmail() : null;
		this.mailGrubuCC = value;
	}

	@OneToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_MAIL_BCC_ID)
	@Fetch(FetchMode.JOIN)
	public MailGrubu getMailGrubuBCC() {
		return mailGrubuBCC;
	}

	public void setMailGrubuBCC(MailGrubu value) {
		this.emailBCC = value != null ? value.getEmail() : null;
		this.mailGrubuBCC = value;
	}

	@OneToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_HAREKET_MAIL_ID)
	@Fetch(FetchMode.JOIN)
	public MailGrubu getHareketMailGrubu() {
		return hareketMailGrubu;
	}

	public void setHareketMailGrubu(MailGrubu value) {
		this.hareketMail = value != null ? value.getEmail() : null;
		this.hareketMailGrubu = value;
	}

	@Transient
	public VardiyaSablonu getWorkSablon() {
		return workSablon;
	}

	public void setWorkSablon(VardiyaSablonu workSablon) {
		this.workSablon = workSablon;
	}

	@Transient
	public PersonelIzin getPersonelIzin() {
		return personelIzin;
	}

	public void setPersonelIzin(PersonelIzin personelIzin) {
		this.personelIzin = personelIzin;
	}

	@Transient
	public boolean isCalisiyorGun(Date bugun) {
		boolean calisiyor = Boolean.FALSE;
		if (iseBaslamaTarihi != null && istenAyrilisTarihi != null) {
			if (bugun == null)
				bugun = PdksUtil.getDate(new Date());
			try {
				Date cikisTarihi = getSskCikisTarihi();
				if (cikisTarihi == null)
					cikisTarihi = this.getSonCalismaTarihi();
				calisiyor = iseBaslamaTarihi != null && bugun.getTime() >= iseBaslamaTarihi.getTime() && bugun.getTime() <= cikisTarihi.getTime();
			} catch (Exception e) {
				calisiyor = false;
			}
		}
		return calisiyor && durum;
	}

	@Transient
	public boolean isCalisiyor() {
		return isCalisiyorGun(Calendar.getInstance().getTime());
	}

	@Transient
	public boolean isSutIzniKullan() {
		return sutIzni != null && sutIzni.booleanValue();
	}

	@Transient
	public boolean isGebelikMuayeneIzniKullan() {
		return gebeMi != null && gebeMi.booleanValue();
	}

	@Transient
	public List<PersonelIzin> getPersonelIzinList() {
		return personelIzinList;
	}

	public void setPersonelIzinList(List<PersonelIzin> personelIzinList) {
		this.personelIzinList = personelIzinList;
	}

	@Transient
	public double getToplamKalanIzin() {
		return toplamKalanIzin;
	}

	public void setToplamKalanIzin(double toplamKalanIzin) {
		this.toplamKalanIzin = toplamKalanIzin;
	}

	@Transient
	public String getAdSoyadKisa() {
		String adiSoyadi = getAdSoyad();
		if (adiSoyadi.length() > 11) {
			adiSoyadi = adiSoyadi.substring(0, 11) + "..";
		}
		return adiSoyadi;
	}

	@Transient
	public List<VardiyaGun> getPersonelVardiyalari() {
		return personelVardiyalari;
	}

	public void setPersonelVardiyalari(List<VardiyaGun> personelVardiyalari) {
		this.personelVardiyalari = personelVardiyalari;
	}

	@Transient
	protected HashMap<String, Double> getIzinBakiyeMap() {
		return izinBakiyeMap;
	}

	public void setIzinBakiyeMap(HashMap<String, Double> izinBakiyeMap) {
		this.izinBakiyeMap = izinBakiyeMap;
	}

	@Transient
	public boolean getIzinBakiyeMapKey(String key) {
		boolean izinvar = izinBakiyeMap != null && izinBakiyeMap.containsKey(key);

		return izinvar;
	}

	public void putIzinBakiyeMap(String key, Double value) {
		if (izinBakiyeMap == null)
			izinBakiyeMap = new HashMap<String, Double>();
		izinBakiyeMap.put(key, value);
	}

	@Transient
	public double getSenelikIzin() {
		return senelikIzin;
	}

	public void setSenelikIzin(double senelikIzin) {
		this.senelikIzin = senelikIzin;
	}

	@Transient
	public double getFazlaMesaiIzin() {
		return fazlaMesaiIzin;
	}

	public void setFazlaMesaiIzin(double fazlaMesaiIzin) {
		this.fazlaMesaiIzin = fazlaMesaiIzin;
	}

	@Transient
	public boolean isSuaOlur() {
		return suaOlabilir != null && suaOlabilir;
	}

	@Transient
	public boolean isPartTimeCalisan() {
		return partTime != null && partTime;
	}

	@Transient
	public boolean isOnaysizIzinKullanir() {
		return onaysizIzinKullanilir != null && onaysizIzinKullanilir;
	}

	@Transient
	public Personel getYonetici2() {
		Personel pdksPersonel = asilYonetici2;
		if (pdksPersonel != null && !pdksPersonel.isCalisiyor())
			pdksPersonel = null;
		if (pdksPersonel == null) {
			if (getPdksYonetici() != null) {
				pdksPersonel = getPdksYonetici();
				if (pdksPersonel != null)
					pdksPersonel = pdksPersonel.getPdksYonetici();
			}
		}
		if (pdksPersonel != null && !pdksPersonel.isCalisiyor())
			pdksPersonel = null;
		return pdksPersonel;
	}

	@Transient
	public Personel getPdksYonetici() {
		if (pdksYonetici == null) {
			pdksYonetici = yoneticisi;
			if (pdksYonetici != null && !pdksYonetici.isCalisiyor() && asilYonetici1 != null && asilYonetici1.isCalisiyor())
				pdksYonetici = asilYonetici1;
		}
		return pdksYonetici;
	}

	public void setPdksYonetici(Personel pdksYonetici) {
		this.pdksYonetici = pdksYonetici;
	}

	@Transient
	public void setYoneticisiAta(Personel pdksPersonel) {
		if (getAsilYonetici1() != null && getAsilYonetici1().getId() != null) {
			if (pdksPersonel != null) {
				boolean yoneticiGuncelle = getYoneticisi() == null || getYoneticisi().getId() == null || getAsilYonetici1().getId().equals(getYoneticisi().getId());
				setAsilYonetici1(pdksPersonel);
				if (yoneticiGuncelle)
					setYoneticisi(pdksPersonel);
			}
		} else {
			setAsilYonetici1(pdksPersonel);
			setYoneticisi(pdksPersonel);
		}

	}

	@Transient
	public boolean isHekim() {
		String gorevTipiKodu = gorevTipi != null ? gorevTipi.getKodu() : null;
		boolean durum = gorevTipiKodu != null && (gorevTipiKodu.equals(STATU_HEKIM) || gorevTipiKodu.equals(STATU_KONTRATLI_HEKIM));
		return durum;

	}

	@Transient
	public String getPersonelExtraAciklama() {
		StringBuffer sb = new StringBuffer();

		String str = sb.toString();
		sb = null;
		return str;
	}

	@Transient
	public boolean isHastaneSuperVisor() {
		boolean durum = ekSaha3 != null && ekSaha3.getKodu().equals(BOLUM_SUPERVISOR);
		return durum;

	}

	@Transient
	public String getSortAlanAdi() {
		return sortAlanAdi;
	}

	public void setSortAlanAdi(String sortAlanAdi) {
		this.sortAlanAdi = sortAlanAdi;
	}

	@Transient
	public Tanim getEkSaha() {
		return ekSaha;
	}

	public void setEkSaha(Tanim ekSaha) {
		this.ekSaha = ekSaha;
	}

	@Transient
	public Boolean isIkinciYoneticiIzinOnaylasin() {
		return ikinciYoneticiIzinOnayla == null || ikinciYoneticiIzinOnayla.booleanValue();
	}

	@Transient
	public boolean isGenelDirektor() {
		return masrafYeri != null && String.valueOf(masrafYeri.getKoduLong()).equals(MASRAF_YERI_GENEL_DIREKTOR.trim());
	}

	@Transient
	public boolean isPersonelGebeMi() {
		return gebeMi != null && gebeMi.booleanValue();
	}

	@Transient
	public Tanim getPlanGrup2() {
		return bordroAltAlan != null ? bordroAltAlan : gorevTipi;
	}

	@Transient
	public String getKontratliSortKey() {
		String str = this.getAdSoyad() + "_" + this.getPdksSicilNo() + "_" + this.getId();
		return str;
	}

	@Transient
	public Boolean getCinsiyetBay() {
		Boolean cinsiyetDurum = cinsiyet != null && cinsiyet.getKodu().equalsIgnoreCase("e");
		return cinsiyetDurum;
	}

	@Transient
	public Boolean getCinsiyetBayan() {
		Boolean cinsiyetDurum = cinsiyet != null && cinsiyet.getKodu().equalsIgnoreCase("k");
		return cinsiyetDurum;
	}

	@Transient
	public boolean isGebelikSutIzinVar() {
		boolean gebelikDurum = getCinsiyetBayan();
		if (gebelikDurum && sirket != null)
			gebelikDurum = sirket.isGebelikSutIzinVar();
		return gebelikDurum;
	}

	@Transient
	public Personel getAktifAsilYonetici2() {
		Personel personel = asilYonetici2;
		try {
			if (personel != null && !personel.isCalisiyor())
				personel = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return personel;
	}

	@Transient
	public Date getSonCalismaTarihi() {
		Date sonCalismaTarihi = getSskCikisTarihi();
		if (sonCalismaTarihi == null) {
			sonCalismaTarihi = istenAyrilisTarihi;
			if (sirket != null && (sirket.getIstenAyrilmaTarihindeCalisiyor() == null || sirket.getIstenAyrilmaTarihindeCalisiyor().equals(Boolean.FALSE))) {
				if (istenAyrilisTarihi != null && PdksUtil.tarihKarsilastirNumeric(istenAyrilisTarihi, PdksUtil.getSonSistemTarih()) != 0)
					sonCalismaTarihi = PdksUtil.tariheGunEkleCikar(istenAyrilisTarihi, -1);
			}

		}

		return sonCalismaTarihi;
	}

	@Transient
	public static Personel newpdksPersonel() {
		Personel pdksPersonel = new Personel();
		pdksPersonel.setPdks(Boolean.TRUE);
		pdksPersonel.setMailTakip(Boolean.TRUE);
		return pdksPersonel;
	}

	@Transient
	public String getEmailCC() {
		return emailCC;
	}

	public void setEmailCC(String value) {
		this.emailCC = value;
		if (mailGrubuCC != null)
			mailGrubuCC.setGuncellendi(Boolean.FALSE);
		if (value != null && value.indexOf("@") > 1) {
			if (mailGrubuCC == null)
				mailGrubuCC = new MailGrubu(MailGrubu.TIPI_CC, value.trim());
			else
				mailGrubuCC.setEmail(value.trim());
			mailGrubuCC.setGuncellendi(Boolean.TRUE);
		}
	}

	@Transient
	public String getEmailBCC() {
		return emailBCC;
	}

	public void setEmailBCC(String value) {
		this.emailBCC = value;
		if (mailGrubuBCC != null)
			mailGrubuBCC.setGuncellendi(Boolean.FALSE);
		if (value != null && value.indexOf("@") > 1) {
			if (mailGrubuBCC == null)
				mailGrubuBCC = new MailGrubu(MailGrubu.TIPI_BCC, value.trim());
			else
				mailGrubuBCC.setEmail(value.trim());
			mailGrubuBCC.setGuncellendi(Boolean.TRUE);
		}

	}

	@Transient
	public String getHareketMail() {
		return hareketMail;
	}

	public void setHareketMail(String value) {
		this.hareketMail = value;
		if (hareketMailGrubu != null)
			hareketMailGrubu.setGuncellendi(Boolean.FALSE);
		if (value != null && value.indexOf("@") > 1) {
			if (hareketMailGrubu == null)
				hareketMailGrubu = new MailGrubu(MailGrubu.TIPI_HAREKET, value.trim());
			else
				hareketMailGrubu.setEmail(value.trim());
			hareketMailGrubu.setGuncellendi(Boolean.TRUE);
		}
	}

	@Transient
	public List<String> getEMailCCList() {
		if (mailGrubuCC != null && (!PdksUtil.hasStringValue(emailCC)))
			emailCC = mailGrubuCC.getEmail();
		List<String> mailList = PdksUtil.getListFromString(emailCC, null);
		return mailList;
	}

	@Transient
	public List<String> getEMailHareketList() {
		if (hareketMailGrubu != null && (!PdksUtil.hasStringValue(hareketMail)))
			hareketMail = hareketMailGrubu.getEmail();
		List<String> mailList = PdksUtil.getListFromString(hareketMail, null);
		return mailList;
	}

	@Transient
	public List<String> getEMailBCCList() {
		if (mailGrubuBCC != null && (!PdksUtil.hasStringValue(emailBCC)))
			emailBCC = mailGrubuBCC.getEmail();
		List<String> mailList = PdksUtil.getListFromString(emailBCC, null);
		return mailList;
	}

	@Transient
	public Date getIseGirisTarihi() {
		Date tarih = !grubaGirisTarihiAlanAdi.equals(COLUMN_NAME_ISE_BASLAMA_TARIHI) && grubaGirisTarihi != null && iseBaslamaTarihi != null && grubaGirisTarihi.before(iseBaslamaTarihi) ? grubaGirisTarihi : iseBaslamaTarihi;
		return tarih;
	}

	@Transient
	private String getBolumOzelAciklamaGetir(boolean ozel) {
		String bolumAciklama = "";
		if (ekSaha3 != null) {
			bolumAciklama = ekSaha3.getAciklama();
			if (ekSaha4 != null) {
				if (ozel == false || (ekSaha3.getKodu() != null && ekSaha3.getKodu().endsWith(altBolumGrupGoster)))
					bolumAciklama += " / " + ekSaha4.getAciklama();
			}
		}
		return bolumAciklama;
	}

	@Transient
	public String getBolumOzelAciklama() {
		String bolumOzelAciklama = getBolumOzelAciklamaGetir(true);
		return bolumOzelAciklama;
	}

	@Transient
	public String getBolumAciklama() {
		String bolumAciklama = getBolumOzelAciklamaGetir(false);
		return bolumAciklama;
	}

	@Transient
	public Personel getPdksPersonel() {
		return this;
	}

	@Transient
	public PersonelView getPersonelView() {
		if (personelView == null) {
			personelView = new PersonelView();
			personelView.setPdksPersonel(this);
			personelView.setPersonelKGS(personelKGS);
			personelView.setId(personelKGS.getId());
			personelView.setAdi(this.getAd());
			personelView.setSoyadi(this.getSoyad());
			personelView.setKgsSicilNo(this.getPdksSicilNo());
		}
		return personelView;
	}

	public static String getIseGirisTarihiColumn() {
		String str = PdksUtil.hasStringValue(grubaGirisTarihiAlanAdi) ? grubaGirisTarihiAlanAdi : COLUMN_NAME_ISE_BASLAMA_TARIHI;
		return str;
	}

	public static String getGrubaGirisTarihiAlanAdi() {
		String grubaGirisTarihiAlanAdiStr = grubaGirisTarihiAlanAdi != null ? grubaGirisTarihiAlanAdi.trim() : COLUMN_NAME_ISE_BASLAMA_TARIHI;
		return grubaGirisTarihiAlanAdiStr;
	}

	public static void setGrubaGirisTarihiAlanAdi(String grubaGirisTarihiAlanAdi) {
		Personel.grubaGirisTarihiAlanAdi = grubaGirisTarihiAlanAdi;
	}

	@Transient
	public static String getAltBolumGrupGoster() {
		return altBolumGrupGoster;
	}

	public static void setAltBolumGrupGoster(String altBolumGrupGoster) {
		Personel.altBolumGrupGoster = altBolumGrupGoster;
	}

	@Transient
	public Boolean getMudurAltSeviye() {
		return mudurAltSeviye;
	}

	@Transient
	public boolean isIzinKartiVardir() {
		return sirket.isIzinGirer() || izinKartiVar != null && izinKartiVar;
	}

	public void setMudurAltSeviye(Boolean mudurAltSeviye) {
		this.mudurAltSeviye = mudurAltSeviye;
	}

	@Transient
	public Personel getTmpYonetici() {
		return tmpYonetici;
	}

	public void setTmpYonetici(Personel tmpYonetici) {
		this.tmpYonetici = tmpYonetici;
	}

	@Transient
	public boolean isGenelMudur() {
		boolean gm = gorevTipi != null && gorevTipi.getKodu() != null && gorevTipi.getKodu().equalsIgnoreCase(Tanim.GOREV_TIPI_GENEL_MUDUR);
		return gm;
	}

	public void entityRefresh() {

	}

}
