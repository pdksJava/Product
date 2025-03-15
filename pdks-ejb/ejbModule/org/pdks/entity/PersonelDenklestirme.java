package org.pdks.entity;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.apache.log4j.Logger;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.pdks.security.entity.User;
import org.pdks.session.PdksUtil;

@Entity(name = PersonelDenklestirme.TABLE_NAME)
@Table(uniqueConstraints = { @UniqueConstraint(columnNames = { PersonelDenklestirme.COLUMN_NAME_DONEM, PersonelDenklestirme.COLUMN_NAME_PERSONEL }) })
public class PersonelDenklestirme extends BaseObject {

	/**
	 * 
	 */
	private static final long serialVersionUID = 9060765954851374678L;
	static Logger logger = Logger.getLogger(PersonelDenklestirme.class);

	public static final String TABLE_NAME = "PERSONELDENKLESTIRME";
	public static final String COLUMN_NAME_DONEM = "DONEM_ID";
	public static final String COLUMN_NAME_PERSONEL = "PERSONEL_ID";
	public static final String COLUMN_NAME_DENKLESTIRME_DURUM = "DENKLESTIRME_DURUM";
	public static final String COLUMN_NAME_ONAYLANDI = "ONAYLANDI";
	public static final String COLUMN_NAME_ODENEN_SURE = "ODENEN_SURE";
	public static final String COLUMN_NAME_HAFTA_TATIL_SURE = "HAFTA_TATIL_SURE";
	public static final String COLUMN_NAME_RESMI_TATIL_SURE = "RESMI_TATIL_SURE";
	public static final String COLUMN_NAME_AKSAM_VARDIYA_GUN_ADET = "AKSAM_VARDIYA_ADET";
	public static final String COLUMN_NAME_AKSAM_VARDIYA_SAAT = "AKSAM_VARDIYA_SAAT";
	public static final String COLUMN_NAME_KESILEN_SURE = "KESILEN_SURE";
	public static final String COLUMN_NAME_SUT_IZNI_DURUM = "SUT_IZNI_DURUM";
	public static final String COLUMN_NAME_PART_TIME_DURUM = "PART_TIME";
	public static final String COLUMN_NAME_SUT_IZNI_SAAT = "SUT_IZNI_SAAT";
	public static final String COLUMN_NAME_EGITIM_SURESI_AKSAM_GUN_SAYISI = "EGITIM_SURESI_AKSAM_GUN";
	public static final String COLUMN_NAME_FAZLA_MESAI_ODE = "FAZLA_MESAI_ODE";
	public static final String COLUMN_NAME_FAZLA_MESAI_IZIN_KULLAN = "FAZLA_MESAI_IZIN_KULLAN";
	public static final String COLUMN_NAME_CALISMA_MODELI_AY = "CALISMA_MODELI_AY_ID";
	public static final String COLUMN_NAME_DEVREDEN_SURE = "DEVREDEN_SURE";
	public static final String COLUMN_NAME_KISMI_ODEME_SAAT = "KISMI_ODEME_SAAT";
	public static final String COLUMN_NAME_PERSONEL_NO = "PERSONEL_NO";
	public static final String COLUMN_NAME_EKSIK_CALISMA_SURE = "EKSIK_CALISMA_SURE";
	public static final String COLUMN_NAME_GECEN_AY_DENKLESTIRME = "GECEN_AY_DENKLESTIRME_ID";

	public static double calismaSaatiSua = 7.0d, calismaSaatiPartTime = 4.5d;

	public double isAramaIzniSaat;

	private Integer version = 0;

	private Personel personel;

	private CalismaModeliAy calismaModeliAy;

	private DenklestirmeAy denklestirmeAy;

	private String personelNo;

	private PersonelDenklestirme personelDenklestirmeGecenAy, personelDenklestirmeDB;

	private VardiyaGun izinVardiyaGun;

	// private PersonelDonemselDurum sutIzniPersonelDonemselDurum, gebePersonelDonemselDurum, isAramaPersonelDonemselDurum;

	private Double planlanSure = 0d, eksikCalismaSure = 0d, hesaplananSure = 0d, resmiTatilSure = 0d, haftaCalismaSuresi = 0d, fazlaMesaiSure = 0d, odenenSure = 0d;

	private Double devredenSure, kesilenSure = 0d, calismaSuaSaati = calismaSaatiSua, kismiOdemeSure = 0d, aksamVardiyaSayisi = 0d, aksamVardiyaSaatSayisi = 0d, sutIzniSaatSayisi = 0d;

	private Integer egitimSuresiAksamGunSayisi;

	private Boolean suaDurum, fazlaMesaiIzinKullan = Boolean.FALSE, fazlaMesaiOde, sutIzniDurum, partTime, bakiyeSifirlaDurum = Boolean.FALSE;

	private boolean erpAktarildi = Boolean.FALSE, onaylandi = Boolean.FALSE, denklestirme;

	// private HashMap<PersonelDurumTipi, PersonelDonemselDurum> donemselDurumMap = new HashMap<PersonelDurumTipi, PersonelDonemselDurum>();
	private HashMap<PersonelDurumTipi, List<PersonelDonemselDurum>> donemselDurumlarMap = new HashMap<PersonelDurumTipi, List<PersonelDonemselDurum>>();

	private String mesaj;

	public PersonelDenklestirme() {
		super();
		this.setGuncellendi(null);
	}

	/**
	 * @param pdksPersonel
	 * @param da
	 * @param cmAy
	 */
	public PersonelDenklestirme(Personel pdksPersonel, DenklestirmeAy da, CalismaModeliAy cmAy) {
		super();
		this.setPersonel(pdksPersonel);
		if (da == null && cmAy != null)
			da = cmAy.getDenklestirmeAy();
		this.denklestirmeAy = da;
		this.calismaModeliAy = cmAy;
		if (cmAy != null) {
			CalismaModeli cm = cmAy.getCalismaModeli();
			this.onaylandi = cm.isIlkPlanOnaylidir() || cm.isFazlaMesaiVarMi() == false;
			if (cm.isFazlaMesaiVarMi() == false)
				this.fazlaMesaiOde = false;
		}
		if (da != null && pdksPersonel.getFazlaMesaiOde() == false && (da.getDenklestirmeDevret() == null || da.getDenklestirmeDevret().booleanValue())) {
			this.fazlaMesaiOde = false;
			this.fazlaMesaiIzinKullan = true;
		}

		this.setGuncellendi(Boolean.FALSE);

	}

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

	public void setPersonel(Personel value) {
		if (value != null && this.getId() == null) {
			if (value.isSuaOlur())
				this.setSuaDurum(value.getSuaOlabilir());
			if (value.isPartTimeCalisan())
				this.setPartTime(value.getPartTime());
			this.setDenklestirme(value.getPdks());
			this.setFazlaMesaiIzinKullan(value.getFazlaMesaiIzinKullan());
			this.setFazlaMesaiOde(value.getFazlaMesaiIzinKullan().equals(Boolean.FALSE) && value.getFazlaMesaiOde());

		}
		this.personel = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_CALISMA_MODELI_AY)
	@Fetch(FetchMode.JOIN)
	public CalismaModeliAy getCalismaModeliAy() {
		return calismaModeliAy;
	}

	public void setCalismaModeliAy(CalismaModeliAy value) {
		Long oldId = calismaModeliAy != null ? calismaModeliAy.getId() : 0L, newId = value != null ? value.getId() : 0L;
		if (!this.isGuncellendi())
			this.setGuncellendi(!oldId.equals(newId));
		this.calismaModeliAy = value;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_DONEM, nullable = false)
	@Fetch(FetchMode.JOIN)
	public DenklestirmeAy getDenklestirmeAy() {
		return denklestirmeAy;
	}

	public void setDenklestirmeAy(DenklestirmeAy denklestirmeAy) {
		this.denklestirmeAy = denklestirmeAy;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_GECEN_AY_DENKLESTIRME)
	@Fetch(FetchMode.JOIN)
	public PersonelDenklestirme getPersonelDenklestirmeGecenAy() {
		return personelDenklestirmeGecenAy;
	}

	public void setPersonelDenklestirmeGecenAy(PersonelDenklestirme personelDenklestirmeGecenAy) {
		this.personelDenklestirmeGecenAy = personelDenklestirmeGecenAy;
	}

	@Column(name = COLUMN_NAME_ODENEN_SURE)
	public Double getOdenenSure() {
		return odenenSure;
	}

	public void setOdenenSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.odenenSure == null || this.odenenSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.odenenSure = value;
	}

	@Column(name = COLUMN_NAME_PERSONEL_NO, insertable = false, updatable = false)
	public String getPersonelNo() {
		return personelNo;
	}

	public void setPersonelNo(String personelNo) {
		this.personelNo = personelNo;
	}

	@Column(name = "SURE")
	public Double getHesaplananSure() {
		return hesaplananSure;
	}

	public void setHesaplananSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.hesaplananSure == null || this.hesaplananSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.hesaplananSure = value;
	}

	@Column(name = COLUMN_NAME_HAFTA_TATIL_SURE)
	public Double getHaftaCalismaSuresi() {
		return haftaCalismaSuresi;
	}

	public void setHaftaCalismaSuresi(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.haftaCalismaSuresi == null || this.haftaCalismaSuresi.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.haftaCalismaSuresi = value;
	}

	@Column(name = COLUMN_NAME_RESMI_TATIL_SURE)
	public Double getResmiTatilSure() {
		return resmiTatilSure;
	}

	public void setResmiTatilSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.resmiTatilSure == null || this.resmiTatilSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.resmiTatilSure = value;
	}

	@Column(name = "FAZLA_MESAI_SURE")
	public Double getFazlaMesaiSure() {
		return fazlaMesaiSure;
	}

	public void setFazlaMesaiSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.fazlaMesaiSure == null || this.fazlaMesaiSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.fazlaMesaiSure = value;
	}

	@Column(name = "SUA_DURUM")
	public Boolean getSuaDurum() {
		return suaDurum;
	}

	public void setSuaDurum(Boolean value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.suaDurum == null || !this.suaDurum.equals(value));
			if (guncellendi)
				logger.debug(value);
		}

		this.suaDurum = value;
	}

	@Column(name = "PLANLANAN_SURE")
	public Double getPlanlanSure() {
		return planlanSure;
	}

	public void setPlanlanSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.planlanSure == null || this.planlanSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.planlanSure = value;
	}

	@Column(name = COLUMN_NAME_DEVREDEN_SURE)
	public Double getDevredenSure() {
		return devredenSure;
	}

	public void setDevredenSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.devredenSure == null || this.devredenSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.devredenSure = value;
	}

	@Column(name = COLUMN_NAME_KISMI_ODEME_SAAT)
	public Double getKismiOdemeSure() {
		return kismiOdemeSure;
	}

	public void setKismiOdemeSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.kismiOdemeSure == null || this.kismiOdemeSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.kismiOdemeSure = value;
	}

	@Column(name = "ERP_AKTARILDI")
	public boolean isErpAktarildi() {
		return erpAktarildi;
	}

	public void setErpAktarildi(boolean erpAktarildi) {
		this.erpAktarildi = erpAktarildi;
	}

	@Column(name = COLUMN_NAME_FAZLA_MESAI_IZIN_KULLAN)
	public Boolean getFazlaMesaiIzinKullan() {
		return fazlaMesaiIzinKullan;
	}

	public void setFazlaMesaiIzinKullan(Boolean value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.fazlaMesaiIzinKullan == null || !this.fazlaMesaiIzinKullan.equals(value));
			if (guncellendi)
				logger.debug(value);
		}

		this.fazlaMesaiIzinKullan = value;
	}

	@Transient
	public Boolean isFazlaMesaiIzinKullanacak() {
		return fazlaMesaiIzinKullan != null && fazlaMesaiIzinKullan.booleanValue();
	}

	@Column(name = COLUMN_NAME_FAZLA_MESAI_ODE)
	public Boolean getFazlaMesaiOde() {
		return fazlaMesaiOde;
	}

	public void setFazlaMesaiOde(Boolean value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.fazlaMesaiOde == null || !this.fazlaMesaiOde.equals(value));
			if (guncellendi)
				logger.debug(value);
		}

		this.fazlaMesaiOde = value;
	}

	@Column(name = COLUMN_NAME_ONAYLANDI)
	public boolean isOnaylandi() {
		return onaylandi;
	}

	public void setOnaylandi(boolean onaylandi) {
		this.onaylandi = onaylandi;
	}

	@Column(name = COLUMN_NAME_DENKLESTIRME_DURUM)
	public boolean isDenklestirme() {
		return denklestirme;
	}

	public void setDenklestirme(boolean denklestirme) {
		this.denklestirme = denklestirme;
	}

	@Column(name = COLUMN_NAME_AKSAM_VARDIYA_GUN_ADET)
	public Double getAksamVardiyaSayisi() {
		return aksamVardiyaSayisi;
	}

	public void setAksamVardiyaSayisi(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.aksamVardiyaSayisi == null || this.aksamVardiyaSayisi.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.aksamVardiyaSayisi = value;
	}

	@Column(name = COLUMN_NAME_EKSIK_CALISMA_SURE)
	public Double getEksikCalismaSure() {
		return eksikCalismaSure;
	}

	public void setEksikCalismaSure(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.eksikCalismaSure == null || this.eksikCalismaSure.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.eksikCalismaSure = value;
	}

	@Column(name = COLUMN_NAME_AKSAM_VARDIYA_SAAT)
	public Double getAksamVardiyaSaatSayisi() {
		return aksamVardiyaSaatSayisi;
	}

	public void setAksamVardiyaSaatSayisi(Double value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.aksamVardiyaSaatSayisi == null || this.aksamVardiyaSaatSayisi.doubleValue() != value.doubleValue());
			if (guncellendi)
				logger.debug(value);
		}

		this.aksamVardiyaSaatSayisi = value;
	}

	@Column(name = COLUMN_NAME_PART_TIME_DURUM)
	public Boolean getPartTime() {
		return partTime;
	}

	public void setPartTime(Boolean value) {
		if (guncellendi != null && !guncellendi && value != null) {
			this.setGuncellendi(this.partTime == null || this.partTime.booleanValue() != value.booleanValue());
			if (guncellendi)
				logger.debug(value);
		}
		this.partTime = value;
	}

	@Column(name = COLUMN_NAME_SUT_IZNI_DURUM)
	public Boolean getSutIzniDurum() {
		return sutIzniDurum;
	}

	public void setSutIzniDurum(Boolean sutIzniDurum) {
		this.sutIzniDurum = sutIzniDurum;
	}

	@Column(name = COLUMN_NAME_SUT_IZNI_SAAT)
	public Double getSutIzniSaatSayisi() {
		return sutIzniSaatSayisi;
	}

	public void setSutIzniSaatSayisi(Double sutIzniSaatSayisi) {
		this.sutIzniSaatSayisi = sutIzniSaatSayisi;
	}

	@Column(name = COLUMN_NAME_EGITIM_SURESI_AKSAM_GUN_SAYISI)
	public Integer getEgitimSuresiAksamGunSayisi() {
		return egitimSuresiAksamGunSayisi;
	}

	public void setEgitimSuresiAksamGunSayisi(Integer egitimSuresiAksamGunSayisi) {
		this.egitimSuresiAksamGunSayisi = egitimSuresiAksamGunSayisi;
	}

	@Column(name = COLUMN_NAME_KESILEN_SURE)
	public Double getKesilenSure() {
		return kesilenSure;
	}

	public void setKesilenSure(Double kesilenSure) {
		this.kesilenSure = kesilenSure;
	}

	@Transient
	public Long getPersonelId() {
		return personel != null ? personel.getId() : 0L;
	}

	@Transient
	public double getKalanSure() {
		double kalanSure = devredenSure != null ? devredenSure.doubleValue() : 0d;
		return kalanSure;
	}

	/**
	 * @param cm
	 * @param gebePersonelDonemselDurum
	 * @param vardiyalar
	 * @return
	 */
	private double getPlananSureHesapla(CalismaModeli cm, PersonelDonemselDurum gebePersonelDonemselDurum, List<VardiyaGun> vardiyalar) {
		double sure = 0.0d, gun = cm.getHaftaIci();
		Double sutIzniSabitSaat = null;
		if (cm.getSutIzniSabitSaat() != null && cm.getSutIzniSabitSaat().doubleValue() > 0.0d)
			sutIzniSabitSaat = cm.getSutIzniSabitSaat();
		for (VardiyaGun vg : vardiyalar) {
			double sureGunlukSut = sutIzniSabitSaat == null ? cm.getSutIzinSaat(PdksUtil.getDateField(vg.getVardiyaDate(), Calendar.DAY_OF_WEEK)) : sutIzniSabitSaat;
			Tatil tatil = vg.getTatil();
			Vardiya vardiya = vg.getVardiya();
			double gunPlanSure = gebePersonelDonemselDurum == null ? gun : cm.getSaat(PdksUtil.getDateField(vg.getVardiyaDate(), Calendar.DAY_OF_WEEK)), sutIzniSure = 0.0d;
			if (vg.isSutIzniVar()) {
				sutIzniSure = gunPlanSure <= 9.0d ? sureGunlukSut : 7.5d;
				gunPlanSure = sutIzniSure;
				logger.debug(vg.getVardiyaDateStr() + " Sut İzni " + gunPlanSure + " ");
			} else if (vg.isGebePersonelDonemselDurum()) {
				sutIzniSure = gunPlanSure > 7.5d ? 7.5d : gunPlanSure;
				gunPlanSure = sutIzniSure;
				logger.debug(vg.getVardiyaDateStr() + " Gebe " + gunPlanSure + " ");
			} else if (isSuaDurumu()) {
				sutIzniSure = gunPlanSure > 7.5d ? 7.5d : gunPlanSure;
				gunPlanSure = AylikPuantaj.getGunlukCalismaSuresi();
				logger.debug(vg.getVardiyaDateStr() + " Şua " + gunPlanSure + " ");
			}
			if (vg.isAyinGunu() && vardiya != null && vardiya.getId() != null) {
				boolean hesapla = !(vg.isIzinli() || vardiya.isHaftaTatil() || tatil != null || (isSuaDurumu() && vardiya.isOff()));
				if (!hesapla) {
					gunPlanSure = 0;
					if (vardiya.isHaftaTatil() == false && tatil != null && tatil.isYarimGunMu() && vg.isIzinli() == false) {
						gunPlanSure = cm.getArife();
						if (sutIzniSure > 0.0d && gunPlanSure > sutIzniSure)
							gunPlanSure = sutIzniSure;
						hesapla = true;
					}
				}
				if (hesapla) {
					sure += gunPlanSure;
					logger.debug(vg.getVardiyaDateStr() + " " + gunPlanSure + " " + sure);
				}
			}
		}
		return sure;
	}

	/**
	 * @param izinSure
	 * @return
	 */
	@Transient
	public Double getMaksimumSure(double izinSure, double arifeToplamSure, List<VardiyaGun> vardiyalar) {
		CalismaModeli cm = calismaModeliAy != null ? calismaModeliAy.getCalismaModeli() : personel.getCalismaModeli();
		PersonelDonemselDurum gebePersonelDonemselDurum = getGebePersonelDonemselDurum(), sutIzniPersonelDonemselDurum = getSutIzniPersonelDonemselDurum();
		isAramaIzniSaat = 0.0d;
		if (donemselDurumlarMap.containsKey(PersonelDurumTipi.IS_ARAMA_IZNI) && personel != null && personel.getIsAramaGunlukSaat() > 0.0d)
			isAramaIzniSaat = personel.getIsAramaGunlukSaat();
		double aylikSure = calismaModeliAy != null ? calismaModeliAy.getSure() : denklestirmeAy.getSure();

		double aylikSutSure = calismaModeliAy != null && calismaModeliAy.getToplamIzinSure() > 0.0d ? calismaModeliAy.getToplamIzinSure() : denklestirmeAy.getToplamIzinSure();
		if (calismaModeliAy != null && cm.getToplamGunGuncelle() && sutIzniSaatSayisi > 0)
			aylikSure = sutIzniSaatSayisi;
		else if (cm.isHaftaTatilSabitDegil() || sutIzniPersonelDonemselDurum != null || gebePersonelDonemselDurum != null) {
			aylikSure = getPlananSureHesapla(cm, gebePersonelDonemselDurum, vardiyalar);
			if (sutIzniPersonelDonemselDurum != null)
				aylikSutSure = aylikSure;
		} else {
			if (izinSure > 0.0d && aylikSure >= izinSure)
				aylikSure -= izinSure;
		}
		Double gunlukCalismaSuresi = calismaModeliAy != null ? AylikPuantaj.getGunlukCalismaSuresi() : null;
		if (isSuaDurumu() || (cm != null && cm.isSua())) {
			aylikSure = aylikSureHesapla(aylikSure - arifeToplamSure, calismaSuaSaati, gunlukCalismaSuresi) + arifeToplamSure;
		} else if (isPartTimeDurumu()) {
			aylikSure = aylikSureHesapla(aylikSure - arifeToplamSure, calismaSaatiPartTime, gunlukCalismaSuresi) + arifeToplamSure;
		}

		double sutIzniSure = sutIzniSaatSayisi != null && sutIzniSaatSayisi.doubleValue() > 0.0d && sutIzniSaatSayisi.doubleValue() != aylikSutSure ? sutIzniSaatSayisi.doubleValue() : aylikSutSure;
		double maxSure = sutIzniDurum == null || sutIzniDurum.equals(Boolean.FALSE) || planlanSure == 0 ? aylikSure : sutIzniSure;
		if (isAramaIzniSaat > 0.0d) {
			double isAramaSure = getIsAramaSure(vardiyalar);
			maxSure -= isAramaSure;
		}
		return maxSure;
	}

	/**
	 * @param vardiyalar
	 * @return
	 */
	private double getIsAramaSure(List<VardiyaGun> vardiyalar) {
		double isAramaSure = 0.0d;
		for (VardiyaGun vg : vardiyalar) {
			Vardiya vardiya = vg.getVardiya();
			if (vg.isAyinGunu() && vardiya != null && vardiya.getId() != null) {
				// if (vg.getVardiyaDateStr().endsWith("1209"))
				// logger.debug("");
				if (vg.getIsAramaIzmiPersonelDonemselDurum()) {
					double sure = 0.0d;
					Tatil tatil = vg.getTatil();
					boolean hesapla = !(vg.isIzinli() || vardiya.isHaftaTatil() || tatil != null);
					if (hesapla)
						sure = isAramaIzniSaat;
					isAramaSure += sure;
				}

			}
		}
		return isAramaSure;
	}

	/**
	 * @param sure
	 * @param katsayi
	 * @param gunlukCalismaSuresi
	 * @return
	 */
	public static double aylikSureHesapla(double sure, double katsayi, Double gunlukCalismaSuresi) {
		if (gunlukCalismaSuresi == null)
			gunlukCalismaSuresi = AylikPuantaj.getGunlukCalismaSuresi();
		int gun = new BigDecimal(sure / AylikPuantaj.getGunlukCalismaSuresi()).intValue();
		double artiSaat = (sure - gunlukCalismaSuresi * gun);
		if (artiSaat > katsayi)
			artiSaat = katsayi;
		double hesaplananSure = (katsayi * gun) + artiSaat;
		return hesaplananSure;
	}

	/**
	 * @param sure
	 * @param gunlukSaat
	 * @return
	 */
	public static double aylikSureArtikHesapla(double sure, double gunlukSaat) {
		int gunAdet = new Double(sure / AylikPuantaj.getGunlukCalismaSuresi()).intValue();
		double kalanSure = sure - (gunAdet * AylikPuantaj.getGunlukCalismaSuresi());
		double hesaplananSure = (gunAdet * gunlukSaat) + (kalanSure > gunlukSaat ? gunlukSaat : kalanSure);
		return hesaplananSure;
	}

	@Transient
	public Double getOdenecekSure() {
		Double odenecekSure = odenenSure;
		return odenecekSure;
	}

	@Transient
	public String getMesaj() {
		return mesaj;
	}

	@Transient
	public boolean isKapandi() {
		boolean kapandi = (erpAktarildi) || (denklestirmeAy == null || !denklestirmeAy.getDurum());
		return kapandi;
	}

	public void setMesaj(String mesaj) {
		this.mesaj = mesaj;
	}

	@Transient
	public boolean isSuaDurumu() {
		boolean sd = suaDurum != null && suaDurum.booleanValue();
		if (!sd && calismaModeliAy != null)
			sd = calismaModeliAy.getCalismaModeli() != null && calismaModeliAy.getCalismaModeli().isSua();
		return sd;
	}

	@Transient
	public boolean isPartTimeDurumu() {
		return partTime != null && partTime.booleanValue();
	}

	@Transient
	public String getSicilNo() {
		return personel != null ? personel.getPdksSicilNo() : "";
	}

	@Transient
	public boolean isOtomatikFazlaCalismaOnaylansinmi() {
		return calismaModeliAy != null && calismaModeliAy.isOtomatikFazlaCalismaOnaylansinmi();
	}

	@Transient
	public PersonelDenklestirme getPersonelDenklestirmeDB() {
		return personelDenklestirmeDB;
	}

	public void setPersonelDenklestirmeDB(PersonelDenklestirme personelDenklestirmeDB) {
		this.personelDenklestirmeDB = personelDenklestirmeDB;
	}

	@Transient
	public String getSortKey() {
		String key = "";
		if (personel != null) {
			if (personel.getPdksYonetici() != null)
				key = personel.getPdksYonetici().getAdSoyad();
			if (personel.getEkSaha3() != null)
				key += "_" + personel.getEkSaha3().getAciklamatr();

			key += "_" + personel.getPdksSicilNo();
		}
		return key;
	}

	@Transient
	public String getKontratliSortKey() {
		String str = personel != null ? personel.getKontratliSortKey() : (id != null ? id.toString() : "");
		return str;
	}

	@Transient
	public boolean isSutIzniVar() {
		return sutIzniDurum != null && sutIzniDurum.booleanValue();
	}

	@Transient
	public boolean isKapandi(User user) {
		boolean kapandi = (erpAktarildi) || (denklestirmeAy == null || !denklestirmeAy.isDurum(user));
		return kapandi;
	}

	@Transient
	public boolean isDenklestirmeDurum() {
		boolean denkDurum = denklestirme;
		if (!denkDurum) {
			CalismaModeli calismaModeli = getCalismaModeli();
			denkDurum = calismaModeli != null && calismaModeli.isHareketKaydiVardiyaBulsunmu();
		}
		return denkDurum;
	}

	@Transient
	public Personel getPdksPersonel() {
		return personel;
	}

	@Transient
	public CalismaModeli getCalismaModeli() {
		CalismaModeli calismaModeli = null;
		if (calismaModeliAy != null)
			calismaModeli = calismaModeliAy.getCalismaModeli();
		else if (personel != null)
			calismaModeli = personel.getCalismaModeli();

		return calismaModeli;
	}

	@Transient
	public Double getCalismaSuaSaati() {
		return calismaSuaSaati;
	}

	public void setCalismaSuaSaati(Double calismaSuaSaati) {
		this.calismaSuaSaati = calismaSuaSaati;
	}

	public void entityRefresh() {
		if (this.getBaseObject() != null) {
			PersonelDenklestirme pd = (PersonelDenklestirme) this.getBaseObject();
			this.fazlaMesaiIzinKullan = pd.getFazlaMesaiIzinKullan();
			this.fazlaMesaiOde = pd.getFazlaMesaiOde();
			this.suaDurum = pd.getSuaDurum();
			this.sutIzniDurum = pd.getSutIzniDurum();
			this.sutIzniSaatSayisi = pd.getSutIzniSaatSayisi();
			this.calismaModeliAy = pd.getCalismaModeliAy();
			this.denklestirme = pd.isDenklestirme();
			this.kismiOdemeSure = pd.getKismiOdemeSure();
			this.egitimSuresiAksamGunSayisi = pd.getEgitimSuresiAksamGunSayisi();
			this.partTime = pd.getPartTime();
		}
	}

	@Transient
	public VardiyaGun getIzinVardiyaGun() {
		return izinVardiyaGun;
	}

	public void setIzinVardiyaGun(VardiyaGun izinVardiyaGun) {
		this.izinVardiyaGun = izinVardiyaGun;
	}

	@Transient
	public PersonelDonemselDurum getPersonelDonemselDurum(PersonelDurumTipi key) {
		PersonelDonemselDurum personelDonemselDurum = null;
		if (key != null && donemselDurumlarMap.containsKey(key))
			personelDonemselDurum = donemselDurumlarMap.get(key).get(0);
		return personelDonemselDurum;
	}

	@Transient
	public PersonelDonemselDurum getSutIzniPersonelDonemselDurum() {
		PersonelDonemselDurum sutIzniPersonelDonemselDurum = getPersonelDonemselDurum(PersonelDurumTipi.SUT_IZNI);
		return sutIzniPersonelDonemselDurum;
	}

	@Transient
	public PersonelDonemselDurum getGebePersonelDonemselDurum() {
		PersonelDonemselDurum gebePersonelDonemselDurum = getPersonelDonemselDurum(PersonelDurumTipi.GEBE);
		return gebePersonelDonemselDurum;
	}

	@Transient
	public PersonelDonemselDurum getIsAramaPersonelDonemselDurum() {
		PersonelDonemselDurum isAramaPersonelDonemselDurum = getPersonelDonemselDurum(PersonelDurumTipi.IS_ARAMA_IZNI);
		return isAramaPersonelDonemselDurum;
	}

	public void setSutIzniPersonelDonemselDurum(PersonelDonemselDurum value) {
		addPersonelDonemselDurum(PersonelDurumTipi.SUT_IZNI, value);
	}

	public void setGebePersonelDonemselDurum(PersonelDonemselDurum value) {
		addPersonelDonemselDurum(PersonelDurumTipi.GEBE, value);
	}

	public void setIsAramaPersonelDonemselDurum(PersonelDonemselDurum value) {
		addPersonelDonemselDurum(PersonelDurumTipi.IS_ARAMA_IZNI, value);
	}

	/**
	 * @param key
	 * @param value
	 */
	public void addPersonelDonemselDurum(PersonelDurumTipi key, PersonelDonemselDurum value) {
		if (key != null) {
			if (value != null) {
				List<PersonelDonemselDurum> list = donemselDurumlarMap.containsKey(key) ? donemselDurumlarMap.get(key) : new ArrayList<PersonelDonemselDurum>();
				if (list.isEmpty())
					donemselDurumlarMap.put(key, list);
				list.add(value);
			} else {
				if (donemselDurumlarMap.containsKey(key))
					donemselDurumlarMap.remove(key);
			}
		}
	}

	@Transient
	public String getGebeDurumAciklama() {
		String str = getDurumAciklama(PersonelDurumTipi.GEBE);
		return str;
	}

	@Transient
	public String getSutIzniDurumAciklama() {
		String str = getDurumAciklama(PersonelDurumTipi.SUT_IZNI);
		return str;
	}

	@Transient
	public String getIsAramaIzniDurumAciklama() {
		String str = getDurumAciklama(PersonelDurumTipi.IS_ARAMA_IZNI);
		return str;
	}

	@Transient
	public String getDurumAciklama(PersonelDurumTipi key) {
		StringBuffer sb = new StringBuffer();
		PersonelDonemselDurum personelDonemselDurum = null;
		if (key != null && donemselDurumlarMap.containsKey(key)) {
			String pattern = PdksUtil.getDateFormat();
			List<PersonelDonemselDurum> list = donemselDurumlarMap.get(key);
			for (PersonelDonemselDurum pdd : list) {
				personelDonemselDurum = pdd;
				if (sb.length() > 0)
					sb.append(", ");
				sb.append(PdksUtil.convertToDateString(pdd.getBasTarih(), pattern) + " - " + PdksUtil.convertToDateString(pdd.getBitTarih(), pattern));
			}
		}
		String str = "";
		if (personelDonemselDurum != null) {
			str = sb.toString();
			str = "<B>" + personelDonemselDurum.getPersonelDurumTipiAciklama() + " " + (str.indexOf(",") > 0 ? "Dönemleri" : "Dönemi") + " : </B>" + str;
		}
		sb = null;
		return str;
	}

	@Transient
	public HashMap<PersonelDurumTipi, List<PersonelDonemselDurum>> getDonemselDurumlarMap() {
		return donemselDurumlarMap;
	}

	public void setDonemselDurumlarMap(HashMap<PersonelDurumTipi, List<PersonelDonemselDurum>> donemselDurumlarMap) {
		this.donemselDurumlarMap = donemselDurumlarMap;
	}

	public static double getCalismaSaatiSua() {
		return calismaSaatiSua;
	}

	public static void setCalismaSaatiSua(double calismaSaatiSua) {
		PersonelDenklestirme.calismaSaatiSua = calismaSaatiSua;
	}

	public static double getCalismaSaatiPartTime() {
		return calismaSaatiPartTime;
	}

	public static void setCalismaSaatiPartTime(double calismaSaatiPartTime) {
		PersonelDenklestirme.calismaSaatiPartTime = calismaSaatiPartTime;
	}

	@Transient
	public double getIsAramaIzniSaat() {
		return isAramaIzniSaat;
	}

	public void setIsAramaIzniSaat(double isAramaIzniSaat) {
		this.isAramaIzniSaat = isAramaIzniSaat;
	}

	@Transient
	public Boolean getBakiyeSifirlaDurum() {
		return bakiyeSifirlaDurum;
	}

	public void setBakiyeSifirlaDurum(Boolean bakiyeSifirlaDurum) {
		this.bakiyeSifirlaDurum = bakiyeSifirlaDurum;
	}

}
