package org.pdks.session;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TreeMap;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.security.Identity;
import org.pdks.entity.AramaSecenekleri;
import org.pdks.entity.AylikPuantaj;
import org.pdks.entity.CalismaModeli;
import org.pdks.entity.CalismaModeliAy;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.Departman;
import org.pdks.entity.DepartmanDenklestirmeDonemi;
import org.pdks.entity.HareketKGS;
import org.pdks.entity.IzinTipi;
import org.pdks.entity.Kapi;
import org.pdks.entity.KapiView;
import org.pdks.entity.Liste;
import org.pdks.entity.MenuItem;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDenklestirme;
import org.pdks.entity.PersonelDenklestirmeBordro;
import org.pdks.entity.PersonelDenklestirmeBordroDetay;
import org.pdks.entity.PersonelDenklestirmeDinamikAlan;
import org.pdks.entity.PersonelDenklestirmeTasiyici;
import org.pdks.entity.PersonelDinamikAlan;
import org.pdks.entity.PersonelDonemselDurum;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelView;
import org.pdks.entity.Sirket;
import org.pdks.entity.Tanim;
import org.pdks.entity.Tatil;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.VardiyaHafta;
import org.pdks.entity.VardiyaPlan;
import org.pdks.entity.VardiyaSaat;
import org.pdks.entity.YemekIzin;
import org.pdks.enums.BordroDetayTipi;
import org.pdks.erp.action.PdksNoSapController;
import org.pdks.erp.action.PdksSap3Controller;
import org.pdks.erp.action.PdksSapController;
import org.pdks.security.entity.User;

import com.pdks.mail.model.MailManager;

/**
 * @author Hasan Sayar
 * 
 */
@Name("fazlaMesaiOrtakIslemler")
public class FazlaMesaiOrtakIslemler implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4677070458220259401L;

	static Logger logger = Logger.getLogger(FazlaMesaiOrtakIslemler.class);

	public static final String PERSONEL_TANIM_SECIM_MUDUR_ALT_SEVIYE = "mudurAltSeviye";

	@In
	Identity identity;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	MailManager mailManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = false, create = true)
	User authenticatedUser;
	@In(required = true, create = true)
	PdksNoSapController pdksNoSapController;
	@In(required = true, create = true)
	PdksSapController pdksSapController;
	@In(required = true, create = true)
	PdksSap3Controller pdksSap3Controller;
	@In(required = false, create = true)
	HashMap<String, String> parameterMap;
	@In(required = false, create = true)
	OrtakIslemler ortakIslemler;
	@Out(required = false, scope = ScopeType.SESSION)
	User seciliYonetici;
	@In(scope = ScopeType.APPLICATION, required = false)
	HashMap<String, MenuItem> menuItemMap = new HashMap<String, MenuItem>();
	@In(required = false)
	FacesMessages facesMessages;

	/**
	 * @param vardiyaGunList
	 * @param sirket
	 * @param dm
	 * @param session
	 * @return
	 */
	@Transactional
	public void setDenklestirmeAySure(List<VardiyaGun> vardiyaGunList, Sirket sirket, DenklestirmeAy dm, Session session) {

		List<CalismaModeliAy> modelList = pdksEntityController.getSQLParamByAktifFieldList(CalismaModeliAy.TABLE_NAME, CalismaModeliAy.COLUMN_NAME_DONEM, dm.getId(), CalismaModeliAy.class, session);

		if (sirket != null && sirket.getDepartman() == null && sirket.getId() != null) {

			sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirket.getId(), Sirket.class, session);

		}
		Departman dep = sirket != null ? sirket.getDepartman() : null;
		LinkedHashMap<Long, CalismaModeliAy> modelMap = new LinkedHashMap<Long, CalismaModeliAy>();
		for (Iterator iterator = modelList.iterator(); iterator.hasNext();) {
			CalismaModeliAy cm = (CalismaModeliAy) iterator.next();
			if (dep != null && cm.getCalismaModeli().getDepartman() != null && !dep.getId().equals(cm.getCalismaModeli().getDepartman().getId()))
				iterator.remove();
			else
				modelMap.put(cm.getCalismaModeli().getId(), cm);
		}

		Departman departman = sirket.getDepartman();
		Double sutIzniHaftaIciDepartman = departman.getHaftaIciSutIzniSure();
		Double sutIzniCumartesiDepartman = departman.getCumartesiSutIzniSure();
		dm.setModeller(modelList);

		for (Iterator iterator = modelList.iterator(); iterator.hasNext();) {
			CalismaModeliAy calismaModeliAy = (CalismaModeliAy) iterator.next();
			boolean flush = false;
			CalismaModeli cm = calismaModeliAy.getCalismaModeli();
			Double sutIzniHaftaIci = cm.getHaftaIciSutIzniSure();
			Double sutIzniCumartesi = sutIzniHaftaIci != null && sutIzniHaftaIci.doubleValue() > 0.0d ? cm.getCumartesiSutIzniSure() : null;
			if (sutIzniHaftaIci == null || sutIzniHaftaIci <= 0.0d) {
				if (departman != null)
					sutIzniHaftaIci = sutIzniHaftaIciDepartman;
				if (sutIzniHaftaIci == null || sutIzniHaftaIci <= 0.0d)
					sutIzniHaftaIci = 7.5d;
			}
			if (sutIzniCumartesi == null || sutIzniCumartesi <= 0.0d) {
				sutIzniCumartesi = 0.0d;
				if (departman != null && departman.getCumartesiSutIzniSure() != null)
					sutIzniCumartesi = sutIzniCumartesiDepartman;

			}

			if (calismaModeliAy.getSure() == 0.0d || calismaModeliAy.getToplamIzinSure() == 0.0d || ((dm.getSure() == 0.0d || dm.getToplamIzinSure() == 0.0d) && cm.getHaftaIci() == 9.0d)) {
				double sure = 0.0d, toplamIzinSure = 0.0d;
				if (cm.isHaftaTatilSabitDegil() == false) {
					Calendar cal = Calendar.getInstance();
					for (VardiyaGun vg : vardiyaGunList) {
						if (vg.isAyinGunu()) {
							double gunSure = 0.0d;
							cal.setTime(vg.getVardiyaDate());
							int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
							if (vg.getTatil() == null) {
								gunSure = cm.getSaat(dayOfWeek);
								double sutIzinSure = cm.getSutIzinSaat(dayOfWeek);
								double izinSure = sutIzinSure > 7.5d ? 7.5d : sutIzinSure;
								toplamIzinSure += izinSure;
							} else if (vg.getTatil().isYarimGunMu()) {
								if (PdksUtil.tarihKarsilastirNumeric(vg.getVardiyaDate(), vg.getTatil().getBasTarih()) == 0) {
									if (vg.isHaftaIci() || cm.getSaat(dayOfWeek) > 0.0d) {
										gunSure += cm.getArife();
										logger.debug(vg.getVardiyaDateStr() + " " + sure);
										toplamIzinSure += cm.getArife();
									}

								}

							}
							if (gunSure > 0) {
								sure += gunSure;

							}

						}
					}
				}

				if (calismaModeliAy.getSure() == 0.0d || calismaModeliAy.getToplamIzinSure() == 0.0d) {
					if (cm.isIdariModelMi()) {
						dm.setSure(sure);
						dm.setToplamIzinSure(toplamIzinSure);
						pdksEntityController.saveOrUpdate(session, entityManager, dm);
					}
					if (calismaModeliAy.getSure() == 0.0d)
						calismaModeliAy.setSure(sure);
					if (calismaModeliAy.getToplamIzinSure() == 0.0d)
						calismaModeliAy.setToplamIzinSure(toplamIzinSure);
					pdksEntityController.saveOrUpdate(session, entityManager, calismaModeliAy);
					flush = true;
				}

			}
			if (flush)
				session.flush();
		}

	}

	/**
	 * @param dataMap
	 * @param session
	 */
	public void calismaPlaniDenklestir(LinkedHashMap<String, Object> dataMap, Session session) {
		List<AylikPuantaj> puantajList = dataMap.containsKey("aylikPuantajList") ? (List<AylikPuantaj>) dataMap.get("aylikPuantajList") : new ArrayList<AylikPuantaj>();
		KapiView manuelGiris = dataMap.containsKey("manuelGirisKapi") ? (KapiView) dataMap.get("manuelGirisKapi") : null;
		KapiView manuelCikis = dataMap.containsKey("manuelCikisKapi") ? (KapiView) dataMap.get("manuelCikisKapi") : null;
		if (manuelGiris == null && manuelCikis == null) {
			HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(null, session);
			manuelGiris = manuelKapiMap.get(Kapi.TIPI_KODU_GIRIS);
			manuelCikis = manuelKapiMap.get(Kapi.TIPI_KODU_CIKIS);
			manuelKapiMap = null;
		}
		Date basTarih = dataMap.containsKey("basTarih") ? (Date) dataMap.get("basTarih") : null;
		Date bitTarih = dataMap.containsKey("bitTarih") ? (Date) dataMap.get("bitTarih") : null;
		Vardiya normalCalismaVardiya = dataMap.containsKey("normalCalismaVardiya") ? (Vardiya) dataMap.get("normalCalismaVardiya") : null;
		// Boolean denklestirmeAyDurum = dataMap.containsKey("denklestirmeAyDurum") ? (Boolean) dataMap.get("denklestirmeAyDurum") : false;
		TreeMap<String, Tatil> tatilGunleriMap = dataMap.containsKey("tatilGunleriMap") ? (TreeMap<String, Tatil>) dataMap.get("tatilGunleriMap") : null;
		boolean tatilVar = tatilGunleriMap != null && !tatilGunleriMap.isEmpty();
		List<YemekIzin> yemekList = ortakIslemler.getYemekList(basTarih, bitTarih, session);
		LinkedHashMap<String, Object> dataDenkMap = new LinkedHashMap<String, Object>();
		HashMap<Long, Double> vardiyaNetCalismaSuresiMap = new HashMap<Long, Double>();
		Calendar cal = Calendar.getInstance();
		dataDenkMap.put("fiiliHesapla", Boolean.FALSE);
		dataDenkMap.put("updateSatus", Boolean.FALSE);
		dataDenkMap.put("yemekList", yemekList);
		dataDenkMap.put("tatilGunleriMap", tatilGunleriMap);
		dataDenkMap.put("girisView", manuelGiris);
		dataDenkMap.put("loginUser", authenticatedUser);
		dataDenkMap.put("sistemUser", ortakIslemler.getSistemAdminUser(session));
		dataDenkMap.put("vardiyaNetCalismaSuresiMap", vardiyaNetCalismaSuresiMap);
		TreeMap<String, VardiyaGun> vardiyaGunMap = new TreeMap<String, VardiyaGun>();
		boolean isAramaIzniOffDahil = ortakIslemler.getParameterKey("isAramaIzniOffDahil").equals("1");

		List<Long> perIdList = new ArrayList<Long>();

		HashMap<Long, List<PersonelDonemselDurum>> pddMap = new HashMap<Long, List<PersonelDonemselDurum>>();

		for (AylikPuantaj ap : puantajList) {

			Personel personel = ap.getPdksPersonel();
			if (personel.getIsAramaGunlukSaat() > 0.0d || personel.isGebelikSutIzinVar())
				perIdList.add(personel.getId());
			for (VardiyaGun vg : ap.getVardiyalar()) {
				if (vg.isGuncellendi())
					logger.debug(vg.getVardiyaDateStr());
				vardiyaGunMap.put(vg.getVardiyaKeyStr(), vg);
			}
		}
		if (!perIdList.isEmpty()) {
			List<PersonelDonemselDurum> list = pdksEntityController.getSQLParamByAktifFieldList(PersonelDonemselDurum.TABLE_NAME, PersonelDonemselDurum.COLUMN_NAME_PERSONEL, perIdList, PersonelDonemselDurum.class, session);
			for (PersonelDonemselDurum personelDonemselDurum : list) {
				Long perId = personelDonemselDurum.getPersonel().getId();
				List<PersonelDonemselDurum> pddList = pddMap.containsKey(perId) ? pddMap.get(perId) : new ArrayList<PersonelDonemselDurum>();
				if (pddList.isEmpty())
					pddMap.put(perId, pddList);
				pddList.add(personelDonemselDurum);
			}
			for (AylikPuantaj aylikPuantaj : puantajList) {
				Personel personel = aylikPuantaj.getPdksPersonel();
				if (pddMap.containsKey(personel.getId())) {
					PersonelDenklestirme pd = aylikPuantaj.getPersonelDenklestirme();
					List<PersonelDonemselDurum> pddList = pddMap.get(personel.getId());
					for (VardiyaGun vGun : aylikPuantaj.getVardiyalar()) {
						if (vGun.isAyinGunu() && vGun.getVardiya() != null) {
							for (PersonelDonemselDurum pdd : pddList) {
								boolean donemIci = vGun.getVardiyaDate().getTime() <= pdd.getBitTarih().getTime() && vGun.getVardiyaDate().getTime() >= pdd.getBasTarih().getTime();
								if (donemIci) {
									if (pdd.getIsAramaIzni()) {
										if (isAramaIzniOffDahil || vGun.getVardiya().isOff() == false) {
											if (pd.getIsAramaPersonelDonemselDurum() == null)
												pd.setIsAramaPersonelDonemselDurum(pdd);
											vGun.setIsAramaPersonelDonemselDurum(pdd);
										}
									} else {
										if (pdd.isSutIzni()) {
											if (pd.getSutIzniPersonelDonemselDurum() == null)
												pd.setSutIzniPersonelDonemselDurum(pdd);
											vGun.setSutIzniPersonelDonemselDurum(pdd);
										} else if (pdd.isGebe()) {
											if (pd.getGebePersonelDonemselDurum() == null)
												pd.setGebePersonelDonemselDurum(pdd);
											vGun.setGebePersonelDonemselDurum(pdd);
										}
										if (aylikPuantaj.isGebeDurum() == false && (vGun.isGebeMi() || vGun.isGebePersonelDonemselDurum())) {
											aylikPuantaj.setGebeDurum(true);
										}
										if (aylikPuantaj.isSuaDurum() == false && (vGun.isSutIzniVar() || vGun.isSutIzniPersonelDonemselDurum())) {

											aylikPuantaj.setSutIzniDurumu(true);
										}
									}
								}

							}

						}

					}
				}
			}
			list = null;
		}
		pddMap = null;
		perIdList = null;
		ortakIslemler.fazlaMesaiSaatiAyarla(vardiyaGunMap);
		boolean haftaTatilDurum = ortakIslemler.getParameterKey("haftaTatilDurum").equals("1");
		vardiyaGunMap = null;
		HashMap<Long, Double> netSureMap = new HashMap<Long, Double>();
		for (AylikPuantaj ap : puantajList) {
			PersonelDenklestirme pd = ap.getPersonelDenklestirme();
			Personel personel = pd.getPdksPersonel();
			logger.debug(personel.getPdksSicilNo() + " " + personel.getAdSoyad());
			PersonelDonemselDurum sutIzniPersonelDonemselDurum = pd != null ? pd.getSutIzniPersonelDonemselDurum() : null;
			PersonelDonemselDurum gebePersonelDonemselDurum = pd != null ? pd.getGebePersonelDonemselDurum() : null;
			PersonelView personelView = null;
			VardiyaGun sonVardiyaGun = null;
			for (VardiyaGun vg : ap.getVardiyalar()) {
				vg.setFiiliHesapla(Boolean.FALSE);
				if (vg.getVardiya() != null) {
					sonVardiyaGun = vg;
				}

			}
			boolean sutIzniDurum = sutIzniPersonelDonemselDurum != null && sutIzniPersonelDonemselDurum.isSutIzni();
			boolean gebeDurum = gebePersonelDonemselDurum != null && gebePersonelDonemselDurum.isGebe();
			for (VardiyaGun vg : ap.getVardiyalar()) {
				Vardiya vardiya = vg.getVardiya();
				if (vardiya == null)
					continue;
				if (sutIzniDurum) {
					boolean sutIzniDonemTamam = sutIzniPersonelDonemselDurum.getBasTarih().getTime() <= vg.getVardiyaDate().getTime() && sutIzniPersonelDonemselDurum.getBitTarih().getTime() >= vg.getVardiyaDate().getTime();
					vg.setSutIzniVar(sutIzniDonemTamam);
				}
				if (gebeDurum) {
					boolean gebeDonemTamam = gebePersonelDonemselDurum.getBasTarih().getTime() <= vg.getVardiyaDate().getTime() && gebePersonelDonemselDurum.getBitTarih().getTime() >= vg.getVardiyaDate().getTime();
					vg.setGebeMi(gebeDonemTamam);
				}

				if (vg.getIzin() != null || vardiya.isCalisma() == false)
					continue;

				if (!vardiyaNetCalismaSuresiMap.containsKey(vardiya.getId()))
					vardiyaNetCalismaSuresiMap.put(vardiya.getId(), vardiya.getNetCalismaSuresi());
				Vardiya islemVardiya = vg.getIslemVardiya();
				if (tatilVar) {
					if (vg.getTatil() == null)
						vg.setTatil(tatilGunleriMap.get(vg.getVardiyaDateStr()));
					vg.setFiiliHesapla(vg.getTatil() != null);
				}

				if (vg.isFiiliHesapla() == false && islemVardiya.getBasDonem() > islemVardiya.getBitDonem() && vg.getTatil() == null) {
					cal.setTime(vg.getVardiyaDate());
					int day = cal.get(Calendar.DATE), sonGun = cal.getActualMaximum(Calendar.DATE);
					if (day == sonGun)
						vg.setFiiliHesapla(true);
					else {
						if (vg.getSonrakiVardiyaGun() != null) {
							VardiyaGun sonrakiVardiyaGun = vg.getSonrakiVardiyaGun();
							if (tatilVar) {
								if (tatilGunleriMap.containsKey(sonrakiVardiyaGun.getVardiyaDateStr())) {
									vg.setTatil(tatilGunleriMap.get(sonrakiVardiyaGun.getVardiyaDateStr()));
									vg.setFiiliHesapla(vg.getTatil() != null);
								}
							}
							if (haftaTatilDurum && vg.isFiiliHesapla() == false)
								vg.setFiiliHesapla(sonrakiVardiyaGun.getVardiya().isHaftaTatil());
						}
					}

				}
				if (vg.isFiiliHesapla()) {
					if (personelView == null)
						personelView = ap.getPdksPersonel().getPersonelView();
					ortakIslemler.manuelHareketEkle(vg, new HareketKGS(personelView, manuelGiris, islemVardiya.getVardiyaBasZaman()), new HareketKGS(personelView, manuelCikis, islemVardiya.getVardiyaBitZaman()));
				} else if (islemVardiya.isCalisma()) {
					Double netSure = netSureMap.containsKey(islemVardiya.getId()) ? netSureMap.get(islemVardiya.getId()) : null;
					if (netSure == null) {
						netSure = islemVardiya.getNetCalismaSuresi();
						netSureMap.put(islemVardiya.getId(), netSure);
					}
					vg.addCalismaSuresi(netSure);
				}
			}
			try {

				for (VardiyaHafta vh : ap.getVardiyaHaftaList()) {
					PersonelDenklestirmeTasiyici dt = new PersonelDenklestirmeTasiyici(ap);
					dt.setSonVardiyaGun(sonVardiyaGun);
					dt.setVardiyalar(vh.getVardiyaGunler());
					dataDenkMap.put("personelDenklestirme", dt);

					ortakIslemler.personelVardiyaDenklestir(dataDenkMap, session);
					dt = null;

				}
				ap.setFazlaMesaiHesapla(false);
				ortakIslemler.aylikPlanSureHesapla(false, normalCalismaVardiya, true, ap, false, tatilGunleriMap, session);

			} catch (Exception exy) {
				logger.error(exy);
				exy.printStackTrace();
			}

		}
		netSureMap = null;

	}

	/**
	 * @param sirket
	 * @param denklestirmeAy
	 * @param tesisId
	 * @param durum
	 * @param session
	 * @return
	 */
	public List<Personel> getTumBolumPersonelListesi(Sirket sirket, DenklestirmeAy denklestirmeAy, Long tesisId, boolean durum, Session session) {
		DepartmanDenklestirmeDonemi denklestirmeDonemi = new DepartmanDenklestirmeDonemi();
		AylikPuantaj aylikPuantaj = getAylikPuantaj(denklestirmeAy.getAy(), denklestirmeAy.getYil(), denklestirmeDonemi, session);
		aylikPuantaj.setLoginUser(authenticatedUser);
		denklestirmeDonemi.setLoginUser(authenticatedUser);
		denklestirmeDonemi.setDenklestirmeAy(denklestirmeAy);
		List<Personel> tumBolumPersonelleri = getFazlaMesaiPersonelList(sirket, (tesisId != null ? String.valueOf(tesisId) : null), null, null, denklestirmeAy != null ? aylikPuantaj : null, durum, session);
		if (!tumBolumPersonelleri.isEmpty()) {
			List<AylikPuantaj> list = new ArrayList<AylikPuantaj>();
			for (Personel value : tumBolumPersonelleri) {
				AylikPuantaj puantaj = new AylikPuantaj();
				puantaj.setCalismaModeli(value.getCalismaModeli());
				puantaj.setPdksPersonel(value);
				list.add(puantaj);
			}
			tumBolumPersonelleri.clear();
			try {
				ortakIslemler.sortAylikPuantajList(list, true);
			} catch (Exception e) {
			}
			for (AylikPuantaj puantaj : list)
				tumBolumPersonelleri.add(puantaj.getPdksPersonel());
			list = null;
		}
		return tumBolumPersonelleri;
	}

	/**
	 * @param basTarih
	 * @param bitTarih
	 * @param session
	 * @return
	 */
	public TreeMap<String, DenklestirmeAy> getDenklestirmeAyMap(Date basTarih, Date bitTarih, Session session) {
		TreeMap<String, DenklestirmeAy> map = new TreeMap<String, DenklestirmeAy>();
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select D.* from " + DenklestirmeAy.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK());
		String str = " where";
		if (basTarih != null) {
			sb.append(str + " (D." + DenklestirmeAy.COLUMN_NAME_YIL + " * 100 + D." + DenklestirmeAy.COLUMN_NAME_AY + " >= :b1)");
			int b1 = Integer.parseInt(PdksUtil.convertToDateString(basTarih, "yyyyMM"));
			fields.put("b1", b1);
			str = " and ";
		}
		if (bitTarih != null) {
			sb.append(str + " (D." + DenklestirmeAy.COLUMN_NAME_YIL + " * 100 + D." + DenklestirmeAy.COLUMN_NAME_AY + " <= :b2)");
			int b2 = Integer.parseInt(PdksUtil.convertToDateString(bitTarih, "yyyyMM"));
			fields.put("b2", b2);
			str = " and ";
		}
		sb.append(" order by D." + DenklestirmeAy.COLUMN_NAME_AY);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<DenklestirmeAy> list = pdksEntityController.getObjectBySQLList(sb, fields, DenklestirmeAy.class);
		for (DenklestirmeAy denklestirmeAy : list) {
			String key = String.valueOf(denklestirmeAy.getYil() * 100 + denklestirmeAy.getAy());
			map.put(key, denklestirmeAy);
		}
		return map;
	}

	/**
	 * @param veriList
	 * @param session
	 * @return
	 */
	public List<AylikPuantaj> setPersonelDenklestirmeBordroVeri(List<PersonelDenklestirme> veriList, Session session) {
		List<AylikPuantaj> puantajList = new ArrayList<AylikPuantaj>();
		if (veriList != null && !veriList.isEmpty()) {
			for (PersonelDenklestirme pd : veriList) {
				AylikPuantaj aylikPuantaj = new AylikPuantaj();
				aylikPuantaj.setPersonelDenklestirmeData(pd);
				puantajList.add(aylikPuantaj);
			}
			setAylikPuantajBordroVeri(puantajList, session);
		}
		return puantajList;
	}

	/**
	 * @param puantajList
	 * @param session
	 */
	public void setAylikPuantajBordroVeri(List<AylikPuantaj> puantajList, Session session) {
		if (puantajList != null && !puantajList.isEmpty()) {
			HashMap<Long, AylikPuantaj> hashMap = new HashMap<Long, AylikPuantaj>();
			boolean renk = Boolean.TRUE;
			for (AylikPuantaj aylikPuantaj : puantajList) {
				aylikPuantaj.setTrClass(renk ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
				renk = !renk;
				hashMap.put(aylikPuantaj.getPersonelDenklestirme().getId(), aylikPuantaj);
			}
			List<Long> isList = new ArrayList<Long>(hashMap.keySet());
			String fieldName = "personelDenklestirme.id";
			HashMap fields = new HashMap();
			fields.put(fieldName, isList);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<PersonelDenklestirmeBordro> borDenklestirmeBordroList = ortakIslemler.getParamList(false, isList, fieldName, fields, PersonelDenklestirmeBordro.class, session);
			if (!borDenklestirmeBordroList.isEmpty()) {
				HashMap<Long, PersonelDenklestirmeBordro> idMap = new HashMap<Long, PersonelDenklestirmeBordro>();
				for (PersonelDenklestirmeBordro personelDenklestirmeBordro : borDenklestirmeBordroList) {
					PersonelDenklestirme pd = personelDenklestirmeBordro.getPersonelDenklestirme();
					if (hashMap.containsKey(pd.getId()))
						hashMap.get(pd.getId()).setDenklestirmeBordro(personelDenklestirmeBordro);
					idMap.put(personelDenklestirmeBordro.getId(), personelDenklestirmeBordro);
					personelDenklestirmeBordro.setDetayMap(new HashMap<BordroDetayTipi, PersonelDenklestirmeBordroDetay>());
				}
				fieldName = "personelDenklestirmeBordro.id";
				List idList = new ArrayList(idMap.keySet());
				fields.clear();
				fields.put(fieldName, idList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<PersonelDenklestirmeBordroDetay> list = ortakIslemler.getParamList(false, idList, fieldName, fields, PersonelDenklestirmeBordroDetay.class, session);
				for (PersonelDenklestirmeBordroDetay detay : list) {
					Long key = detay.getPersonelDenklestirmeBordro().getId();
					BordroDetayTipi bordroDetayTipi = BordroDetayTipi.fromValue(detay.getTipi());
					idMap.get(key).getDetayMap().put(bordroDetayTipi, detay);
				}
				idMap = null;
			}
			borDenklestirmeBordroList = null;
			hashMap = null;
		}

	}

	/**
	 * @param dm
	 * @param aramaSecenekleri
	 * @param hataliVeriGetir
	 * @param eksikCalisanVeriGetir
	 * @param session
	 * @return
	 */
	public List<AylikPuantaj> getBordoDenklestirmeList(DenklestirmeAy dm, AramaSecenekleri aramaSecenekleri, Boolean hataliVeriGetir, Boolean eksikCalisanVeriGetir, Session session) {
		int yil = dm.getYil(), ay = dm.getAy();
		Calendar cal = Calendar.getInstance();
		String sicilNo = aramaSecenekleri.getSicilNo();
		String str = ortakIslemler.getParameterKey("bordroVeriOlustur");
		boolean sicilDolu = PdksUtil.hasStringValue(sicilNo);
		List<AylikPuantaj> personelDenklestirmeList = ortakIslemler.getSelectItemList("aylikPuantaj", authenticatedUser);
		TreeMap<Long, AylikPuantaj> eksikCalismaMap = new TreeMap<Long, AylikPuantaj>();
		if (yil * 100 + ay >= Integer.parseInt(str)) {
			Long tesisId = aramaSecenekleri.getTesisId();
			Sirket sirket = aramaSecenekleri.getSirket();
			AylikPuantaj aylikPuantajSablon = new AylikPuantaj(dm);
			aylikPuantajSablon.setLoginUser(aramaSecenekleri.getLoginUser());
			List<Personel> donemPerList = getFazlaMesaiPersonelList(sirket, tesisId != null ? String.valueOf(tesisId) : null, null, null, dm != null ? aylikPuantajSablon : null, true, session);
			List<Long> perList = new ArrayList<Long>();

			for (Personel personel : donemPerList) {
				if (sicilDolu && !ortakIslemler.isStringEqual(sicilNo, personel.getPdksSicilNo()))
					continue;
				perList.add(personel.getId());
			}

			HashMap fields = new HashMap();
			StringBuffer sb = new StringBuffer();

			TreeMap<Long, PersonelDenklestirme> pdMap = new TreeMap<Long, PersonelDenklestirme>();
			boolean hataliDurum = false;
			TreeMap<Long, AylikPuantaj> aylikPuantajMap = new TreeMap<Long, AylikPuantaj>();
			TreeMap<Long, PersonelDenklestirmeBordro> idMap = new TreeMap<Long, PersonelDenklestirmeBordro>();
			TreeMap<Long, AylikPuantaj> hataliMap = new TreeMap<Long, AylikPuantaj>();
			if (!perList.isEmpty()) {
				String fieldName = "p";
				fields.clear();
				sb = new StringBuffer();
				sb.append("select V.* from " + PersonelDenklestirme.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where V." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + dm.getId() + " and V." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
				if (sicilDolu == false && (hataliVeriGetir == null || hataliVeriGetir == false)) {
					hataliDurum = true;
				}
				fields.put(fieldName, perList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<PersonelDenklestirme> pdlist = pdksEntityController.getSQLParamList(perList, sb, fieldName, fields, PersonelDenklestirme.class, session);
				perList.clear();
				for (PersonelDenklestirme pd : pdlist) {
					boolean ekle = true;

					if (hataliDurum) {
						ekle = false;
						if (pd.getDurum() && pd.isOnaylandi()) {
							CalismaModeli cm = pd.getCalismaModeli();
							ekle = cm.isFazlaMesaiGoruntulensinMi() || pd.isDenklestirme();
						}
					}
					if (ekle) {
						perList.add(pd.getId());
						pdMap.put(pd.getId(), pd);

						AylikPuantaj aylikPuantaj = new AylikPuantaj();
						if (hataliVeriGetir != null && hataliVeriGetir && pd.getDurum().equals(Boolean.FALSE) && pd.isOnaylandi())
							hataliMap.put(pd.getPdksPersonel().getId(), aylikPuantaj);

						aylikPuantaj.setPersonelDenklestirme(pd);
						aylikPuantaj.setPdksPersonel(pd.getPdksPersonel());
						aylikPuantaj.setEksikCalismaSure(null);
						aylikPuantajMap.put(pd.getId(), aylikPuantaj);
						personelDenklestirmeList.add(aylikPuantaj);
						if (pd.getDurum().equals(Boolean.TRUE) && (eksikCalisanVeriGetir != null && eksikCalisanVeriGetir)) {
							double normalSaat = 0.0d, planlananSaaat = 0.0d;
							CalismaModeli cm = pd.getCalismaModeli();
							if (cm != null) {
								try {
									normalSaat = pd.getHesaplananSure().doubleValue();
								} catch (Exception e) {
									normalSaat = 0.0d;
								}
								try {
									planlananSaaat = pd.getPlanlanSure().doubleValue() - cm.getHaftaIci();
								} catch (Exception e) {
									planlananSaaat = 0.0d;
								}
								if (pd.getPdksPersonel().getPdksSicilNo().equals("1385"))
									logger.debug(pd.getId());
								if (normalSaat <= planlananSaaat || cm.isAylikOdeme() || cm.isFazlaMesaiVarMi())
									eksikCalismaMap.put(pd.getId(), aylikPuantaj);
							}

						}
					}

				}
				if (!perList.isEmpty()) {
					List<PersonelDenklestirmeBordro> borDenklestirmeBordroList = pdksEntityController.getSQLParamByFieldList(PersonelDenklestirmeBordro.TABLE_NAME, PersonelDenklestirmeBordro.COLUMN_NAME_PERSONEL_DENKLESTIRME, perList, PersonelDenklestirmeBordro.class, session);
					for (PersonelDenklestirmeBordro personelDenklestirmeBordro : borDenklestirmeBordroList) {
						PersonelDenklestirme pd = personelDenklestirmeBordro.getPersonelDenklestirme();
						if (aylikPuantajMap.containsKey(pd.getId()))
							aylikPuantajMap.get(pd.getId()).setDenklestirmeBordro(personelDenklestirmeBordro);
						idMap.put(personelDenklestirmeBordro.getId(), personelDenklestirmeBordro);
						personelDenklestirmeBordro.setDetayMap(new HashMap<BordroDetayTipi, PersonelDenklestirmeBordroDetay>());
					}
				}

				pdlist = null;
				perList = null;
			}
			perList = null;
			if (!idMap.isEmpty()) {
				List idList = new ArrayList(idMap.keySet());
				List<PersonelDenklestirmeBordroDetay> list = pdksEntityController.getSQLParamByFieldList(PersonelDenklestirmeBordroDetay.TABLE_NAME, PersonelDenklestirmeBordroDetay.COLUMN_NAME_PERSONEL_DENKLESTIRME_BORDRO, idList, PersonelDenklestirmeBordroDetay.class, session);

				for (PersonelDenklestirmeBordroDetay detay : list) {
					Long key = detay.getPersonelDenklestirmeBordro().getId();
					BordroDetayTipi bordroDetayTipi = BordroDetayTipi.fromValue(detay.getTipi());
					idMap.get(key).getDetayMap().put(bordroDetayTipi, detay);
				}
				idMap = null;
			}
			String donem = String.valueOf(dm.getYil() * 100 + dm.getAy());
			if (!hataliMap.isEmpty()) {
				List<Long> perIdList = new ArrayList<Long>(hataliMap.keySet());
				String fieldName = "p";
				HashMap map = new HashMap();
				sb = new StringBuffer();
				sb.append("select distinct V.* from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = V." + VardiyaGun.COLUMN_NAME_PERSONEL);
				sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= P." + Personel.getIseGirisTarihiColumn());
				sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI);
				sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :basTarih and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + "< :bitTarih  ");
				sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <=CONVERT(DATE, GETDATE() ) and V." + VardiyaGun.COLUMN_NAME_DURUM + " = 0 ");
				sb.append(" and V." + VardiyaGun.COLUMN_NAME_PERSONEL + " :" + fieldName);
				sb.append(" order by V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI);
				Date basTarih = PdksUtil.getDateFromString((yil * 100 + ay) + "01");
				Date bitTarih = ortakIslemler.tariheAyEkleCikar(cal, basTarih, 1);
				map.put(fieldName, perIdList);
				map.put("basTarih", basTarih);
				map.put("bitTarih", bitTarih);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// List<VardiyaGun> vardiyaGunList = pdksEntityController.getObjectBySQLList(sb, map, VardiyaGun.class);
				List<VardiyaGun> vardiyaGunList = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, map, VardiyaGun.class, session);

				for (VardiyaGun vardiyaGun : vardiyaGunList) {
					String vardiyaDateStr = vardiyaGun.getVardiyaDateStr();
					if (vardiyaGun.getVardiyaSaatDB() == null && vardiyaGun.getVardiya().isCalisma() == false)
						continue;
					if (vardiyaDateStr.startsWith(donem)) {
						Personel personel = vardiyaGun.getPdksPersonel();
						Long id = personel.getId();
						AylikPuantaj aylikPuantaj = hataliMap.get(id);
						if (aylikPuantaj.getVardiyalar() == null)
							aylikPuantaj.setVardiyalar(new ArrayList<VardiyaGun>());
						aylikPuantaj.getVardiyalar().add(vardiyaGun);
					}
				}
			}
			if (!eksikCalismaMap.isEmpty()) {
				TreeMap<Long, AylikPuantaj> perDMap = new TreeMap<Long, AylikPuantaj>();
				for (Long key : eksikCalismaMap.keySet()) {
					AylikPuantaj aylikPuantaj = eksikCalismaMap.get(key);
					aylikPuantaj.setVardiyalar(new ArrayList<VardiyaGun>());
					perDMap.put(aylikPuantaj.getPdksPersonel().getId(), aylikPuantaj);
				}
				try {
					List<Long> perIdList = new ArrayList<Long>(perDMap.keySet());
					String fieldName = "p";
					HashMap map = new HashMap();
					sb = new StringBuffer();
					sb.append("select distinct VG.* from VARDIYA_GUN_SAAT_VIEW V " + PdksEntityController.getSelectLOCK() + " ");
					sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = V." + VardiyaGun.COLUMN_NAME_PERSONEL);
					sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= P." + Personel.getIseGirisTarihiColumn());
					sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI);
					sb.append(" inner join " + Vardiya.TABLE_NAME + " VA " + PdksEntityController.getJoinLOCK() + " on VA." + Vardiya.COLUMN_NAME_ID + " = V." + VardiyaGun.COLUMN_NAME_VARDIYA + " and VA.VARDIYATIPI=''");
					sb.append(" inner join " + VardiyaGun.TABLE_NAME + " VG " + PdksEntityController.getJoinLOCK() + " on VG." + VardiyaGun.COLUMN_NAME_ID + " = V.VARDIYA_GUN_ID ");
					sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :basTarih and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + "< :bitTarih  ");
					sb.append(" and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " < CONVERT(DATE, GETDATE() ) and V." + VardiyaSaat.COLUMN_NAME_CALISMA_SURESI + " = 0 ");
					sb.append(" and V." + VardiyaSaat.COLUMN_NAME_NORMAL_SURE + " > 0 and V." + VardiyaGun.COLUMN_NAME_PERSONEL + " :" + fieldName);
					Date basTarih = PdksUtil.getDateFromString((yil * 100 + ay) + "01");
					Date bitTarih = ortakIslemler.tariheAyEkleCikar(cal, basTarih, 1);
					map.put(fieldName, perIdList);
					map.put("basTarih", basTarih);
					map.put("bitTarih", bitTarih);
					if (session != null)
						map.put(PdksEntityController.MAP_KEY_SESSION, session);
					// List<VardiyaGun> vardiyaGunList = pdksEntityController.getObjectBySQLList(sb, map, VardiyaGun.class);
					List<VardiyaGun> vardiyaGunList = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, map, VardiyaGun.class, session);

					if (vardiyaGunList.size() > 1)
						vardiyaGunList = PdksUtil.sortListByAlanAdi(vardiyaGunList, "vardiyaDate", false);
					TreeMap<String, Tatil> tatilGunleriMap = ortakIslemler.getTatilGunleri(null, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -1), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 1), session);
					TreeMap<Long, Personel> perMap = new TreeMap<Long, Personel>();
					for (VardiyaGun vardiyaGun : vardiyaGunList) {
						String vardiyaDateStr = vardiyaGun.getVardiyaDateStr();
						if (vardiyaDateStr.startsWith(donem)) {
							if (tatilGunleriMap.containsKey(vardiyaDateStr))
								continue;
							vardiyaGun.setVardiyaZamani();
							Personel personel = vardiyaGun.getPdksPersonel();
							Long id = personel.getId();
							AylikPuantaj aylikPuantaj = perDMap.get(id);
							aylikPuantaj.getVardiyalar().add(vardiyaGun);
							if (!perMap.containsKey(id)) {
								perMap.put(id, personel);
								PersonelDenklestirme pd = aylikPuantaj.getPersonelDenklestirme();
								boolean hataYok = pd.getDurum().equals(Boolean.TRUE), donemBitti = true;
								if (hataYok) {
									CalismaModeli cm = eksikCalismaMap.containsKey(pd.getId()) && hataYok && hataliVeriGetir != null && hataliVeriGetir ? pd.getCalismaModeli() : null;
									Double eksikCalismaSure = null;
									if (cm != null && eksikCalisanVeriGetir != null && eksikCalisanVeriGetir) {
										double normalSaat = 0.0d, planlananSaaat = 0.0d;
										try {
											planlananSaaat = pd.getPlanlanSure().doubleValue() - cm.getHaftaIci();
										} catch (Exception e) {
											planlananSaaat = 0.0d;
										}
										try {
											if (cm.isSaatlikOdeme()) {
												PersonelDenklestirmeBordro pdb = aylikPuantaj.getDenklestirmeBordro();
												normalSaat = pdb != null ? pdb.getSaatNormal().doubleValue() : 0.0d;
											} else {
												normalSaat = pd.getHesaplananSure().doubleValue();
											}
										} catch (Exception e) {
											normalSaat = 0.0d;
										}
										try {
											eksikCalismaSure = normalSaat - (planlananSaaat + cm.getHaftaIci());
										} catch (Exception e) {
											hataYok = false;
										}
										donemBitti = hataYok;
									}

									aylikPuantaj.setEksikCalismaSure(eksikCalismaSure);
									aylikPuantaj.setDonemBitti(donemBitti);
								}
								logger.debug(personel.getPdksSicilNo() + " " + personel.getAdSoyad());

							}
						}

					}
					tatilGunleriMap = null;
					perMap = null;
					perDMap = null;
				} catch (Exception e) {
					logger.error(e + "\n" + sb.toString());
					e.printStackTrace();
				}

			}

		}
		if (!personelDenklestirmeList.isEmpty()) {
			List<AylikPuantaj> puantajList = new ArrayList<AylikPuantaj>(ortakIslemler.sortAylikPuantajList(personelDenklestirmeList, false));
			personelDenklestirmeList.clear();
			List<AylikPuantaj> aktifList = new ArrayList<AylikPuantaj>(), aktifEksikList = new ArrayList<AylikPuantaj>(), hataliGunList = new ArrayList<AylikPuantaj>();
			for (AylikPuantaj aylikPuantaj : puantajList) {
				PersonelDenklestirme pd = aylikPuantaj.getPersonelDenklestirme();
				boolean hataYok = pd.getDurum().equals(Boolean.TRUE);
				if (aylikPuantaj.getEksikCalismaSure() != null)
					aktifEksikList.add(aylikPuantaj);
				else if (hataYok)
					aktifList.add(aylikPuantaj);
				else {
					if (pd.isOnaylandi() == false)
						personelDenklestirmeList.add(aylikPuantaj);
					else
						hataliGunList.add(aylikPuantaj);
				}

			}
			if (!hataliGunList.isEmpty())
				personelDenklestirmeList.addAll(hataliGunList);
			if (!aktifEksikList.isEmpty())
				personelDenklestirmeList.addAll(aktifEksikList);
			if (!aktifList.isEmpty())
				personelDenklestirmeList.addAll(aktifList);
			puantajList = null;
			hataliGunList = null;
			aktifEksikList = null;
			aktifList = null;
		}
		return personelDenklestirmeList;
	}

	private void setExcelNumber(Sheet sheet, CellStyle tutarStyle, CellStyle numberStyle, int row, int col, Double tutar) {
		CellStyle style = tutar.doubleValue() - tutar.longValue() > 0.0d ? tutarStyle : numberStyle;
		ExcelUtil.getCell(sheet, row, col, style).setCellValue(tutar);
	}

	/**
	 * @param veriMap
	 * @param session
	 * @return
	 * @throws IOException
	 */
	public ByteArrayOutputStream denklestirmeExcelAktarDevam(HashMap<String, Object> veriMap, Session session) throws IOException {
		ByteArrayOutputStream baos = null;
		List<AylikPuantaj> personelDenklestirmeList = veriMap.containsKey("personelDenklestirmeList") ? (List<AylikPuantaj>) veriMap.get("personelDenklestirmeList") : new ArrayList<AylikPuantaj>();
		List<Tanim> personelDinamikAlanlar = veriMap.containsKey("personelDinamikAlanlar") ? (List<Tanim>) veriMap.get("personelDinamikAlanlar") : new ArrayList<Tanim>();
		TreeMap<String, PersonelDinamikAlan> personelDinamikAlanMap = veriMap.containsKey("personelDinamikAlanMap") ? (TreeMap<String, PersonelDinamikAlan>) veriMap.get("personelDinamikAlanMap") : new TreeMap<String, PersonelDinamikAlan>();
		Sirket sirket = veriMap.containsKey("sirket") ? (Sirket) veriMap.get("sirket") : null;
		User loginUser = veriMap.containsKey("loginUser") ? (User) veriMap.get("loginUser") : authenticatedUser;
		if (!personelDenklestirmeList.isEmpty()) {
			LinkedHashMap<String, String> baslikMap = veriMap.containsKey("baslikMap") ? (LinkedHashMap<String, String>) veriMap.get("baslikMap") : null;
			Tanim ekSaha4Tanim = veriMap.containsKey("ekSaha4Tanim") ? (Tanim) veriMap.get("ekSaha4Tanim") : null;
			DenklestirmeAy dm = veriMap.containsKey("denklestirmeAy") ? (DenklestirmeAy) veriMap.get("denklestirmeAy") : null;
			if (baslikMap == null) {
				baslikMap = new LinkedHashMap<String, String>();
				List<Tanim> bordroAlanlari = ortakIslemler.getTanimList(Tanim.TIPI_BORDRDO_ALANLARI, session);
				bordroAlanlari = PdksUtil.sortObjectStringAlanList(bordroAlanlari, "getErpKodu", null);
				for (Tanim tanim : bordroAlanlari) {
					baslikMap.put(tanim.getKodu(), tanim.getAciklama());
				}
				veriMap.put("baslikMap", baslikMap);
			}
			Workbook wb = new XSSFWorkbook();
			CellStyle tutarStyle = ExcelUtil.getCellStyleTutar(wb);
			CellStyle numberStyle = ExcelUtil.getCellStyleTutar(wb);
			CreationHelper helper = wb.getCreationHelper();
			ClientAnchor anchor = helper.createClientAnchor();
			boolean kimlikNoGoster = false;
			Boolean kartNoAciklamaGoster = null;
			String kartNoAciklama = ortakIslemler.getParameterKey("kartNoAciklama");
			if (PdksUtil.hasStringValue(kartNoAciklama))
				kartNoAciklamaGoster = false;
			if (kartNoAciklamaGoster == null)
				kartNoAciklamaGoster = false;
			List<AylikPuantaj> list = new ArrayList<AylikPuantaj>();
			Boolean aksamGun = Boolean.FALSE, aksamSaat = Boolean.FALSE, haftaCalisma = Boolean.FALSE, resmiTatilDurum = Boolean.FALSE, maasKesintiGoster = Boolean.FALSE, artikGunDurum = Boolean.FALSE, resmiTatilGunDurum = Boolean.FALSE;

			DenklestirmeBordroRaporuHome home = new DenklestirmeBordroRaporuHome();

			boolean haftaTatilBaslik = PdksUtil.hasStringValue(baslikMap.get(home.COL_HAFTA_TATIL_MESAI));
			boolean aksamGunBaslik = PdksUtil.hasStringValue(baslikMap.get(home.COL_AKSAM_GUN_MESAI));
			boolean aksamSaatBaslik = PdksUtil.hasStringValue(baslikMap.get(home.COL_AKSAM_SAAT_MESAI));
			boolean eksikCalismaBaslik = PdksUtil.hasStringValue(baslikMap.get(home.COL_EKSIK_CALISMA));
			for (AylikPuantaj aylikPuantaj : personelDenklestirmeList) {
				if (aylikPuantaj.getPersonelDenklestirme().getDurum().equals(Boolean.FALSE))
					continue;
				PersonelDenklestirmeBordro personelDenklestirmeBordro = aylikPuantaj.getDenklestirmeBordro();
				PersonelDenklestirme pd = aylikPuantaj.getPersonelDenklestirme();
				if (personelDenklestirmeBordro == null)
					personelDenklestirmeBordro = new PersonelDenklestirmeBordro();
				if (!artikGunDurum)
					artikGunDurum = personelDenklestirmeBordro.getArtikAdet() != null && personelDenklestirmeBordro.getArtikAdet().doubleValue() > 0.0d;
				if (!resmiTatilGunDurum)
					resmiTatilGunDurum = personelDenklestirmeBordro.getResmiTatilAdet() != null && personelDenklestirmeBordro.getResmiTatilAdet().doubleValue() > 0.0d;
				if (!artikGunDurum)
					artikGunDurum = personelDenklestirmeBordro.getArtikAdet() != null && personelDenklestirmeBordro.getArtikAdet().doubleValue() > 0.0d;
				if (!haftaCalisma && haftaTatilBaslik)
					haftaCalisma = pd.getHaftaCalismaSuresi() != null && pd.getHaftaCalismaSuresi().doubleValue() > 0.0d;
				if (!resmiTatilDurum)
					resmiTatilDurum = pd.getResmiTatilSure() != null && pd.getResmiTatilSure().doubleValue() > 0.0d;
				if (!aksamGun && aksamGunBaslik)
					aksamGun = pd.getAksamVardiyaSayisi() != null && pd.getAksamVardiyaSayisi().doubleValue() > 0.0d;
				if (!aksamSaat && aksamSaatBaslik)
					aksamSaat = pd.getAksamVardiyaSaatSayisi() != null && pd.getAksamVardiyaSaatSayisi().doubleValue() > 0.0d;
				if (!maasKesintiGoster && eksikCalismaBaslik)
					maasKesintiGoster = pd.getEksikCalismaSure() != null && pd.getEksikCalismaSure().doubleValue() > 0.0d;

				list.add(aylikPuantaj);
			}
			if (!list.isEmpty()) {

				boolean tesisGoster = false;
				for (AylikPuantaj aylikPuantaj : list) {

					Personel personel = aylikPuantaj.getPdksPersonel();
					PersonelKGS personelKGS = personel.getPersonelKGS();
					if (personelKGS != null) {
						if (kartNoAciklamaGoster != null && kartNoAciklamaGoster.booleanValue() == false) {
							kartNoAciklamaGoster = PdksUtil.hasStringValue(personelKGS.getKartNo());
							if (kartNoAciklamaGoster && kimlikNoGoster)
								break;
						}

						if (!kimlikNoGoster) {
							kimlikNoGoster = PdksUtil.hasStringValue(personelKGS.getKimlikNo());
							if (kimlikNoGoster && (kartNoAciklamaGoster == null || kartNoAciklamaGoster))
								break;
						}
					}
				}
				int yil = dm.getYil(), ay = dm.getAy();
				Calendar cal = Calendar.getInstance();
				Date tarih = PdksUtil.getDateFromString((yil * 100 + ay) + "01");
				Date ilkGun = ortakIslemler.tariheGunEkleCikar(cal, tarih, -1);
				Date sonGun = ortakIslemler.tariheAyEkleCikar(cal, tarih, 1);
				String ayAdi = dm.getAyAdi();
				Sheet sheet = ExcelUtil.createSheet(wb, PdksUtil.setTurkishStr(ayAdi + " " + dm.getYil()) + " Liste", Boolean.TRUE);
				Drawing drawing = sheet.createDrawingPatriarch();
				XSSFCellStyle headerSiyah = (XSSFCellStyle) ExcelUtil.getStyleHeader(wb);
				XSSFCellStyle header = (XSSFCellStyle) ExcelUtil.getStyleHeader(wb);
				CellStyle style = ExcelUtil.getStyleData(wb);
				CellStyle styleCenter = ExcelUtil.getStyleDataCenter(wb);

				headerSiyah.getFont().setColor(ExcelUtil.getXSSFColor(255, 255, 255));
				XSSFCellStyle headerSaat = (XSSFCellStyle) headerSiyah.clone();
				XSSFCellStyle headerIzin = (XSSFCellStyle) headerSiyah.clone();
				XSSFCellStyle headerBGun = (XSSFCellStyle) headerSiyah.clone();
				XSSFCellStyle headerBTGun = (XSSFCellStyle) (XSSFCellStyle) headerSiyah.clone();
				ExcelUtil.setFillForegroundColor(headerSaat, 146, 208, 62);
				ExcelUtil.setFillForegroundColor(headerIzin, 255, 255, 255);
				ExcelUtil.setFillForegroundColor(headerBGun, 255, 255, 0);
				ExcelUtil.setFillForegroundColor(headerBTGun, 236, 125, 125);
				DataFormat df = wb.createDataFormat();
				numberStyle.setDataFormat(df.getFormat("###"));
				int row = 0, col = 0;
				for (Iterator iterator = baslikMap.keySet().iterator(); iterator.hasNext();) {
					String kodu = (String) iterator.next();

					CellStyle baslikHeader = null;
					if (kodu.startsWith(home.COL_TESIS) && tesisGoster == false) {
						iterator.remove();
						continue;
					}

					if (kodu.startsWith(home.COL_KART_NO) && kartNoAciklamaGoster == false) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_KIMLIK_NO) && kimlikNoGoster == false) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_HAFTA_TATIL_MESAI) && haftaCalisma == false) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_AKSAM_SAAT_MESAI) && aksamSaat == false) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_EKSIK_CALISMA) && maasKesintiGoster == false) {
						iterator.remove();
						continue;
					}

					if (kodu.startsWith(home.COL_AKSAM_GUN_MESAI) && aksamGun == false) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_SIRKET) && (sirket == null || sirket.getSirketGrup() == null)) {
						iterator.remove();
						continue;
					}
					if (kodu.startsWith(home.COL_ALT_BOLUM) && ekSaha4Tanim == null) {
						iterator.remove();
						continue;
					}
					if (baslikHeader == null)
						baslikHeader = header;
					if (kodu.equals(home.COL_UCRETLI_IZIN) || kodu.equals(home.COL_RAPORLU_IZIN) || kodu.equals(home.COL_UCRETSIZ_IZIN) || kodu.equals(home.COL_YILLIK_IZIN))
						baslikHeader = headerIzin;
					else if (kodu.equals(home.COL_NORMAL_GUN_SAAT) || kodu.equals(home.COL_HAFTA_TATIL_SAAT) || kodu.equals(home.COL_RESMI_TATIL_SAAT) || kodu.equals(home.COL_IZIN_SAAT))
						baslikHeader = headerSaat;
					else if (kodu.equals(home.COL_NORMAL_GUN_ADET) || kodu.equals(home.COL_HAFTA_TATIL_ADET) || kodu.equals(home.COL_RESMI_TATIL_ADET) || kodu.equals(home.COL_ARTIK_ADET))
						baslikHeader = headerBGun;
					else if (kodu.equals(home.COL_TOPLAM_ADET))
						baslikHeader = headerBTGun;

					ExcelUtil.getCell(sheet, row, col++, baslikHeader).setCellValue(baslikMap.get(kodu));
					if (kodu.equals(home.COL_CALISMA_MODELI)) {
						for (Tanim alan : personelDinamikAlanlar) {
							ExcelUtil.getCell(sheet, row, col++, baslikHeader).setCellValue(alan.getAciklama());
						}
					}

				}
				PersonelDenklestirmeBordro denklestirmeBordroBos = new PersonelDenklestirmeBordro();
				for (AylikPuantaj ap : list) {
					Personel personel = ap.getPdksPersonel();
					PersonelDenklestirmeBordro denklestirmeBordro = ap.getDenklestirmeBordro();
					PersonelDenklestirme pd = ap.getPersonelDenklestirme();
					if (pd == null && denklestirmeBordro != null)
						pd = denklestirmeBordro.getPersonelDenklestirme();
					if (denklestirmeBordro == null)
						denklestirmeBordro = denklestirmeBordroBos;
					row++;
					col = 0;
					PersonelKGS personelKGS = personel.getPersonelKGS();
					StringBuffer sb = null;
					try {
						if (ap.getVardiyalar() != null && !ap.getVardiyalar().isEmpty()) {
							sb = new StringBuffer();
							if (pd.getDurum())
								sb.append("Kapı Giriş Hatalı veya Fazla Çalışması Onay Bekleyen Günler");
							else
								sb.append("Kapı Giriş Kayıtı ve İzin Girilmeyen Günler");
							String pattern = PdksUtil.getDateFormat();
							for (VardiyaGun vg : ap.getVardiyalar()) {
								Vardiya vardiya = vg.getVardiya();
								if (vardiya == null)
									continue;

								sb.append("\n" + PdksUtil.convertToDateString(vg.getVardiyaDate(), pattern) + " " + vardiya.getKisaAdi() + " [ " + vg.getVardiyaAdi() + " ] ");
							}
						}
					} catch (Exception e) {
						logger.equals(e);
					}

					for (String kodu : baslikMap.keySet()) {
						if (kodu.startsWith(home.COL_CALISMA_MODELI)) {
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(ap.getCalismaModeli().getAciklama());

							for (Tanim alan : personelDinamikAlanlar) {
								Tanim deger = ortakIslemler.getTanimDeger(personel, alan, personelDinamikAlanMap);
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(deger != null ? deger.getAciklama() : "");
							}

						} else if (kodu.equals(home.COL_SIRA))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(row);
						else if (kodu.equals(home.COL_ISE_BASLAMA_TARIHI))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(ilkGun.before(personel.getIseBaslamaTarihi()) ? loginUser.dateFormatla(personel.getIseBaslamaTarihi()) : "");
						else if (kodu.equals(home.COL_SSK_CIKIS_TARIHI))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.isCalisiyorGun(sonGun) ? "" : loginUser.dateFormatla(personel.getSskCikisTarihi()));
						else if (kodu.equals(home.COL_YIL))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(yil);
						else if (kodu.equals(home.COL_YIL))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(yil);
						else if (kodu.equals(home.COL_AY))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(ay);
						else if (kodu.equals(home.COL_AY_ADI))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(ayAdi);
						else if (kodu.equals(home.COL_DEVAMLILIK_PRIMI)) {
							String dp = "";
							Boolean dpDurum = denklestirmeBordro.getDevamlilikPrimi();
							if (dpDurum != null)
								dp = dpDurum ? "+" : "-";
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(dp);
						} else if (kodu.equals(home.COL_PERSONEL_NO)) {
							Cell perNoCell = ExcelUtil.getCell(sheet, row, col++, styleCenter);
							perNoCell.setCellValue(personel.getPdksSicilNo());
							if (sb != null) {
								String title = sb.toString();
								ExcelUtil.setCellComment(perNoCell, anchor, helper, drawing, title);
								sb = null;
							}

						}

						else if (kodu.equals(home.COL_AD_SOYAD))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAdSoyad());
						else if (kodu.equals(home.COL_AD))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAd());
						else if (kodu.equals(home.COL_SOYAD))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getSoyad());
						else if (kodu.equals(home.COL_KART_NO)) {
							String kartNo = "";
							if (personelKGS != null && PdksUtil.hasStringValue(personelKGS.getKartNo()))
								kartNo = personelKGS.getKartNo();
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(kartNo);
						} else if (kodu.equals(home.COL_KIMLIK_NO)) {
							String kimlikNo = "";
							if (personelKGS != null && PdksUtil.hasStringValue(personelKGS.getKimlikNo()))
								kimlikNo = personelKGS.getKimlikNo();
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(kimlikNo);
						} else if (kodu.startsWith(home.COL_SIRKET)) {
							if (personel.getSirket() != null) {
								if (kodu.startsWith(home.COL_SIRKET + "Kodu"))
									ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSirket().getErpKodu());
								else if (kodu.startsWith(home.COL_SIRKET))
									ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getSirket().getAd());
							} else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.startsWith(home.COL_TESIS)) {
							if (personel.getTesis() != null) {
								if (kodu.startsWith(home.COL_TESIS + "Kodu"))
									ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getTesis().getErpKodu());
								else if (kodu.startsWith(home.COL_TESIS))
									ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getTesis().getAciklama());
							} else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals(home.COL_BOLUM))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "");
						else if (kodu.equals(home.COL_ALT_BOLUM))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getEkSaha4() != null ? personel.getEkSaha4().getAciklama() : "");
						else if (kodu.equals(home.COL_NORMAL_GUN_ADET))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getNormalGunAdet() != null ? denklestirmeBordro.getNormalGunAdet() : 0.0d);
						else if (kodu.equals(home.COL_HAFTA_TATIL_ADET))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getHaftaTatilAdet() != null ? denklestirmeBordro.getHaftaTatilAdet() : 0.0d);
						else if (kodu.equals(home.COL_RESMI_TATIL_ADET))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getResmiTatilAdet() != null ? denklestirmeBordro.getResmiTatilAdet() : 0.0d);
						else if (kodu.equals(home.COL_ARTIK_ADET))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getArtikAdet() != null ? denklestirmeBordro.getArtikAdet() : 0);
						else if (kodu.equals(home.COL_TOPLAM_ADET))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getBordroToplamGunAdet());
						else if (kodu.equals(home.COL_NORMAL_GUN_SAAT))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getSaatNormal());
						else if (kodu.equals(home.COL_HAFTA_TATIL_SAAT))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getSaatHaftaTatil());
						else if (kodu.equals(home.COL_RESMI_TATIL_SAAT))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getSaatResmiTatil());
						else if (kodu.equals(home.COL_IZIN_SAAT))
							setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getSaatIzin());
						else if (kodu.equals(home.COL_UCRETLI_IZIN))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getUcretliIzin());
						else if (kodu.equals(home.COL_RAPORLU_IZIN))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getRaporluIzin());
						else if (kodu.equals(home.COL_UCRETSIZ_IZIN))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getUcretsizIzin());
						else if (kodu.equals(home.COL_YILLIK_IZIN))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getYillikIzin());
						else if (kodu.equals(home.COL_RESMI_TATIL_MESAI)) {
							if (denklestirmeBordro.getResmiTatilMesai() > 0)
								setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getResmiTatilMesai());
							else
								ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(0);
						} else if (kodu.equals(home.COL_UCRETI_ODENEN_MESAI)) {
							if (denklestirmeBordro.getUcretiOdenenMesai() > 0)
								setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getUcretiOdenenMesai());
							else
								ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(0);
						} else if (kodu.equals(home.COL_HAFTA_TATIL_MESAI)) {
							if (denklestirmeBordro.getHaftaTatilMesai() > 0)
								setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getHaftaTatilMesai());
							else
								ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(0);

						} else if (kodu.equals(home.COL_AKSAM_GUN_MESAI))
							ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(denklestirmeBordro.getAksamGunMesai());
						else if (kodu.equals(home.COL_AKSAM_SAAT_MESAI)) {
							if (denklestirmeBordro.getAksamSaatMesai() > 0)
								setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getAksamSaatMesai());
							else
								ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(0);

						} else if (kodu.equals(home.COL_EKSIK_CALISMA)) {
							if (denklestirmeBordro.getEksikCalismaSure() > 0)
								setExcelNumber(sheet, tutarStyle, numberStyle, row, col++, denklestirmeBordro.getEksikCalismaSure());
							else
								ExcelUtil.getCell(sheet, row, col++, numberStyle).setCellValue(0);

						}
					}

				}
				list = null;
				for (int i = 0; i < col; i++)
					sheet.autoSizeColumn(i);
				baos = new ByteArrayOutputStream();
				wb.write(baos);
			}
		} else
			PdksUtil.addMessageWarn("Aktarılacak hatasız veri yok!");

		return baos;

	}

	/**
	 * @param donemYil
	 * @param donemAy
	 * @param aylar
	 * @param session
	 * @return
	 */
	public int aylariDoldur(int donemYil, int donemAy, List<SelectItem> aylar, Session session) {
		donemAy = aylariDoldurDurum(donemYil, donemAy, aylar, true, session);

		return donemAy;

	}

	/**
	 * @param donemYil
	 * @param donemAy
	 * @param aylar
	 * @param donemDurum
	 * @param session
	 * @return
	 */
	public int aylariDoldurDurum(int donemYil, int donemAy, List<SelectItem> aylar, boolean donemDurum, Session session) {
		if (donemDurum == false)
			ortakIslemler.yilAyKontrol(donemYil, null, session);
		Calendar cal = Calendar.getInstance();
		int maxYil = cal.get(Calendar.YEAR);
		int sonDonem = (maxYil * 100) + cal.get(Calendar.MONTH) + 1;
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select distinct D.* from " + DenklestirmeAy.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" " + (donemDurum ? "inner" : "left") + " join " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getJoinLOCK() + " on PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = D." + DenklestirmeAy.COLUMN_NAME_ID);
		sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1 ");
		sb.append(" where D." + DenklestirmeAy.COLUMN_NAME_YIL + " = :y and D." + DenklestirmeAy.COLUMN_NAME_AY + " > 0 ");
		if (donemYil == maxYil) {
			sb.append(" and ((D." + DenklestirmeAy.COLUMN_NAME_YIL + "*100)+" + DenklestirmeAy.COLUMN_NAME_AY + ") <= :s");
			fields.put("s", sonDonem);
		}
		String ilkDonem = ortakIslemler.getParameterKey("ilkMaasDonemi");
		if (PdksUtil.hasStringValue(ilkDonem) == false) {
			String sistemBaslangicYili = ortakIslemler.getParameterKey("sistemBaslangicYili");
			if (PdksUtil.hasStringValue(sistemBaslangicYili))
				ilkDonem = sistemBaslangicYili + ilkDonem;
		}
		if (PdksUtil.hasStringValue(ilkDonem))
			sb.append(" and ((D." + DenklestirmeAy.COLUMN_NAME_YIL + "*100) + D." + DenklestirmeAy.COLUMN_NAME_AY + ")> " + ilkDonem);
		fields.put("y", donemYil);
		sb.append(" order by D." + DenklestirmeAy.COLUMN_NAME_AY);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<DenklestirmeAy> list = pdksEntityController.getObjectBySQLList(sb, fields, DenklestirmeAy.class);
		aylar.clear();
		int seciliAy = donemAy;
		donemAy = 0;
		for (DenklestirmeAy denklestirmeAy : list) {
			if (denklestirmeAy.getAy() == seciliAy)
				donemAy = seciliAy;
			aylar.add(new SelectItem(denklestirmeAy.getAy(), denklestirmeAy.getAyAdi()));
		}
		if (!aylar.isEmpty()) {
			if (donemAy == 0) {
				donemAy = (Integer) aylar.get(aylar.size() - 1).getValue();
			}
		} else
			PdksUtil.addMessageAvailableWarn(donemYil + " yılı aylarında çalışma planı oluşturulmamıştır!");

		return donemAy;
	}

	/**
	 * @param kaydet
	 * @param puantajList
	 * @param fazlaMesaiHesapla
	 * @param donemStr
	 * @param loginUser
	 * @param session
	 * @return
	 */
	@Transactional
	public TreeMap<String, Boolean> bordroVeriOlustur(boolean kaydet, List<AylikPuantaj> puantajList, Boolean fazlaMesaiHesapla, String donemStr, User loginUser, Session session) {
		TreeMap<String, Boolean> baslikMap = new TreeMap<String, Boolean>();
		String arifeGunuBordroYarim = ortakIslemler.getParameterKey("arifeGunuBordroYarim");
		boolean saatlikCalismaVar = ortakIslemler.getParameterKey("saatlikCalismaVar").equals("1");
		List<Tanim> list = pdksEntityController.getSQLParamByAktifFieldList(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_TIPI, Tanim.TIPI_IZIN_GRUPLARI, Tanim.class, session);

		if (list.isEmpty()) {
			BordroDetayTipi[] bordroTipileri = new BordroDetayTipi[] { BordroDetayTipi.UCRETLI_IZIN, BordroDetayTipi.UCRETSIZ_IZIN, BordroDetayTipi.RAPORLU_IZIN };
			for (BordroDetayTipi bordroTipi : bordroTipileri) {
				Tanim tanim = new Tanim();
				tanim.setTipi(Tanim.TIPI_IZIN_GRUPLARI);
				tanim.setAciklamatr(bordroTipi.name());
				tanim.setAciklamaen(bordroTipi.name());
				tanim.setKodu(bordroTipi.value());
				tanim.setErpKodu("izinGrup" + bordroTipi.value());
				pdksEntityController.saveOrUpdate(session, entityManager, tanim);

				list.add(tanim);
			}
			session.flush();
		}

		TreeMap<String, String> izinGrupMap = ortakIslemler.getIzinGrupMap(session);
		Calendar cal = Calendar.getInstance();
		// Date bugun = PdksUtil.getDate(cal.getTime());
		Date tarih = PdksUtil.convertToJavaDate(donemStr + "01", "yyyyMMdd");
		cal.setTime(tarih);
		int ayGunSayisi = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
		TreeMap<Long, AylikPuantaj> puantajMap = new TreeMap<Long, AylikPuantaj>();
		for (AylikPuantaj ap : puantajList) {
			PersonelDenklestirme personelDenklestirme = ap.getPersonelDenklestirme();
			if (personelDenklestirme.getId() != null)
				puantajMap.put(personelDenklestirme.getId(), ap);
		}
		if (!puantajMap.isEmpty()) {
			List dataIdList = new ArrayList(puantajMap.keySet());
			TreeMap<Long, PersonelDenklestirmeBordro> bordroMap = pdksEntityController
					.getSQLParamByFieldMap(PersonelDenklestirmeBordro.TABLE_NAME, PersonelDenklestirmeBordro.COLUMN_NAME_PERSONEL_DENKLESTIRME, dataIdList, PersonelDenklestirmeBordro.class, "getPersonelDenklestirmeId", false, session);

			TreeMap<String, PersonelDenklestirmeBordroDetay> bordroDetayMap = null;
			List<Long> idList = new ArrayList<Long>();
			if (!bordroMap.isEmpty()) {
				for (Long key : bordroMap.keySet())
					idList.add(bordroMap.get(key).getId());
				bordroDetayMap = pdksEntityController.getSQLParamByFieldMap(PersonelDenklestirmeBordroDetay.TABLE_NAME, PersonelDenklestirmeBordroDetay.COLUMN_NAME_PERSONEL_DENKLESTIRME_BORDRO, idList, PersonelDenklestirmeBordroDetay.class, "getDetayKey", false, session);

			} else
				bordroDetayMap = new TreeMap<String, PersonelDenklestirmeBordroDetay>();

			for (AylikPuantaj ap : puantajList) {
				PersonelDenklestirme personelDenklestirme = ap.getPersonelDenklestirme();
				if (!(personelDenklestirme.getDurum() || fazlaMesaiHesapla == false))
					continue;
				boolean flush = false;
				PersonelDenklestirmeDinamikAlan devamPrim = null;
				if (ap.getDinamikAlanMap() != null) {
					for (Object key : ap.getDinamikAlanMap().keySet()) {
						PersonelDenklestirmeDinamikAlan personelDenklestirmeDinamikAlan = ap.getDinamikAlanMap().get(key);
						if (personelDenklestirmeDinamikAlan.getDurum() && personelDenklestirmeDinamikAlan.getAlan().getKodu().equals(PersonelDenklestirmeDinamikAlan.TIPI_DEVAMLILIK_PRIMI)) {
							devamPrim = personelDenklestirmeDinamikAlan;
						}

					}

				}
				CalismaModeli calismaModeli = personelDenklestirme.getCalismaModeli();
				try {
					for (VardiyaHafta vardiyaHafta : ap.getVardiyaHaftaList()) {
						List<VardiyaGun> gunler = vardiyaHafta.getVardiyaGunler();
						if (gunler != null) {
							VardiyaGun sonGun = null, haftaTatilGun = null;
							for (VardiyaGun vardiyaGun : gunler) {
								if (vardiyaGun.getVardiya() != null) {
									sonGun = vardiyaGun;
									if (vardiyaGun.getVardiya().isHaftaTatil() && vardiyaGun.getCalismaSuresi() == 0.0)
										haftaTatilGun = vardiyaGun;
								}
							}
							if (haftaTatilGun != null && sonGun != null && sonGun.getIzin() != null && sonGun.getIzin().getIzinTipi().isTakvimGunuMu() == false) {
								if (sonGun.getCalismaSuresi() == 0.0d && !haftaTatilGun.getId().equals(sonGun.getId())) {
									//

								}

							}
						}

					}
				} catch (Exception ex) {
					logger.error(ex);
					ex.printStackTrace();
				}

				double normalGunAdet = 0.0d, haftaTatilAdet = 0.0d, resmiTatilAdet = 0.0d, artikAdet = 0.0d, izinGunAdet = 0.0d;
				double normalSaat = 0.0d, resmiTatilSaat = 0.0d, haftaTatilSaat = 0.0d, izinGunSaat = 0.0d;
				LinkedHashMap<BordroDetayTipi, Double> detayMap = new LinkedHashMap<BordroDetayTipi, Double>();
				boolean saatlikCalisma = calismaModeli.isSaatlikOdeme();
				Double gunlukKatsayi = null;
				for (VardiyaGun vardiyaGun : ap.getVardiyalar()) {
					if (vardiyaGun.isAyinGunu() && vardiyaGun.getVardiya() != null) {
						if (gunlukKatsayi == null) {
							gunlukKatsayi = vardiyaGun.getSaatCalisanGunlukKatsayisi();
							if (gunlukKatsayi == null || gunlukKatsayi.doubleValue() < 7.5d)
								gunlukKatsayi = gunlukKatsayi.doubleValue();
						}
						// boolean sonrakiIzinli = vardiyaGun.getSonrakiVardiyaGun() != null && vardiyaGun.getSonrakiVardiyaGun().getVardiya() != null && vardiyaGun.getSonrakiVardiyaGun().isIzinli();
						Vardiya vardiya = vardiyaGun.getVardiya();
						boolean haftaTatil = vardiya.isHaftaTatil();
						Tatil tatil = vardiyaGun.getTatil();
						boolean resmiTatil = tatil != null;
						double calismaGun = 1.0d;
						BordroDetayTipi izinBordroDetayTipi = null;
						if (vardiyaGun.isIzinli()) {

							// calisiyor = true;
							String izinKodu = null;
							double artiGun = 1.0d;
							// if (saatlikCalisma)
							// artiGun = vardiyaGun.getSaatCalisanIzinGunKatsayisi();
							IzinTipi senelikIzin = null;
							IzinTipi izinTipi = null;
							if (vardiyaGun.getIzin() != null) {
								PersonelIzin personelIzin = vardiyaGun.getIzin();
								if (personelIzin.getOrjIzin() != null)
									personelIzin = personelIzin.getOrjIzin();
								if (haftaTatil)
									logger.debug(vardiyaGun.getVardiyaDateStr());
								izinTipi = personelIzin.getIzinTipi();
								if (izinTipi != null) {

									izinBordroDetayTipi = ortakIslemler.getBordroDetayTipi(izinTipi, izinGrupMap);
									izinKodu = izinBordroDetayTipi != null ? izinBordroDetayTipi.value() : izinTipi.getIzinTipiTanim().getErpKodu();
									if (izinTipi.isUcretsizIzinTipi() || (izinBordroDetayTipi != null && izinBordroDetayTipi.equals(BordroDetayTipi.UCRETSIZ_IZIN))) {
										calismaGun = 0;
									} else if (saatlikCalisma) {
										if (resmiTatil == false) {
											Double sure = ortakIslemler.getVardiyaIzinSuresi(vardiyaGun.getSaatCalisanIzinGunKatsayisi(), vardiyaGun, personelDenklestirme, vardiyaGun.getVardiyaDate());
											if (sure > 0.0d)
												izinGunSaat += sure;
										}

									}
									if (izinTipi.isSenelikIzin())
										senelikIzin = izinTipi;

								}

								if (izinTipi.getTakvimGunumu() == false) {
									if (vardiyaGun.getVardiya().isOffGun()) {
										if (izinTipi.isOffDahilMi() == false || (izinTipi.isCumaCumartesiTekIzinSaysin() && personelIzin.getIzinSuresi() < 2.0d)) {
											artiGun = 0.0d;
											calismaGun = 1.0d;
										}

									} else if (haftaTatil) {
										if (izinTipi.isHTDahil() == false) {
											artiGun = 0.0d;
											calismaGun = 1.0d;
										}
									}
									if (izinTipi.getUcretli() && resmiTatil == false)
										izinGunAdet += artiGun;

								}

							} else
								izinKodu = vardiya.getStyleClass();

							if (izinKodu != null) {
								try {
									if (izinBordroDetayTipi == null)
										izinBordroDetayTipi = BordroDetayTipi.fromValue(izinKodu);
								} catch (Exception e) {

								}

								if (haftaTatil && izinBordroDetayTipi != null) {
									haftaTatil = (izinTipi != null && izinTipi.isHTDahil() == false) || izinBordroDetayTipi.value().equals(BordroDetayTipi.UCRETLI_IZIN.value());
									if (izinBordroDetayTipi.value().equals(BordroDetayTipi.RAPORLU_IZIN.value()) && vardiya.isHaftaTatil()) {
										artiGun = 0;
									} else

									if (haftaTatil == false)
										artiGun = 1;
								}
								if (artiGun > 0.0d) {
									String str = izinBordroDetayTipi != null ? izinBordroDetayTipi.value() : "";
									if (PdksUtil.hasStringValue(str))
										calismaGun = 0;
									if (resmiTatil == false || str.equals(BordroDetayTipi.RAPORLU_IZIN.value()))
										calismaGun = izinDetayYaz(detayMap, calismaGun, izinKodu, artiGun);
									else if (resmiTatil && str.equals(BordroDetayTipi.UCRETLI_IZIN.value()))
										calismaGun = 1;
									if (senelikIzin != null && resmiTatil == false)
										izinDetayYaz(detayMap, calismaGun, BordroDetayTipi.YILLIK_IZIN.value(), artiGun);

								}

							}

						}
						if (calismaGun > 0.0d) {
							if (resmiTatil && izinBordroDetayTipi == null) {
								if (!tatil.isYarimGunMu()) {
									resmiTatilAdet += calismaGun;
									calismaGun = 0;
								} else if (PdksUtil.hasStringValue(arifeGunuBordroYarim)) {
									calismaGun = 0.5d;
									resmiTatilAdet += calismaGun;
								}
							}
							if (!haftaTatil) {
								normalGunAdet += calismaGun;
								// logger.info(vardiyaGun.getVardiyaDateStr() + " " + normalGunAdet);
							} else {
								haftaTatilAdet += calismaGun;
								logger.debug(vardiyaGun.getVardiyaDateStr() + " " + normalGunAdet);
							}
						}
						if (vardiyaGun.isIzinli() == false) {
							if (resmiTatil) {
								if (!tatil.isYarimGunMu())
									resmiTatilSaat += vardiyaGun.getSaatCalisanResmiTatilKatsayisi();
								else {
									resmiTatilSaat += vardiyaGun.getSaatCalisanArifeTatilKatsayisi();
									normalSaat += vardiyaGun.getSaatCalisanArifeNormalKatsayisi();
								}
							} else if (vardiyaGun.isHaftaIci()) {
								normalSaat += vardiyaGun.getSaatCalisanNormalGunKatsayisi();
								logger.debug(vardiyaGun.getVardiyaDateStr() + " " + normalGunAdet);
							}

						}
						if (haftaTatil && calismaGun > 0)
							haftaTatilSaat += vardiyaGun.getSaatCalisanHaftaTatilKatsayisi();
					}

				}

				double toplamAdet = normalGunAdet + haftaTatilAdet + resmiTatilAdet + izinGunAdet;
				double toplamSaatAdet = saatlikCalisma ? normalSaat + haftaTatilSaat + resmiTatilSaat + izinGunSaat : 0;
				double normalCalisma = ap.getSaatToplami() > ap.getPlanlananSure() ? ap.getPlanlananSure() : ap.getSaatToplami();
				if (calismaModeli != null && calismaModeli.isSaatlikOdeme())
					normalCalisma += ap.getGecenAyFazlaMesai(authenticatedUser);

				if (!detayMap.isEmpty() || (saatlikCalisma == false && toplamAdet > 0) || (saatlikCalisma && toplamSaatAdet > 0)) {
					if (toplamAdet > 0) {
						if (ayGunSayisi == toplamAdet)
							normalGunAdet += 30 - ayGunSayisi;
						if (toplamAdet > 30) {
							artikAdet = 1;
						}
					}

					PersonelDenklestirmeBordro denklestirmeBordro = bordroMap.containsKey(personelDenklestirme.getId()) ? bordroMap.get(personelDenklestirme.getId()) : null;
					if (denklestirmeBordro == null) {
						denklestirmeBordro = new PersonelDenklestirmeBordro();
						denklestirmeBordro.setPersonelDenklestirme(personelDenklestirme);
					}
					Long denklestirmeBordroId = denklestirmeBordro.getId();

					denklestirmeBordro.setGuncellendi(denklestirmeBordro.getId() == null);
					if (!saatlikCalisma) {
						normalCalisma = 0.0d;
						resmiTatilSaat = 0.0d;
						haftaTatilSaat = 0.0d;
						izinGunSaat = 0.0d;
						normalSaat = 0.0d;
					} else {
						normalGunAdet = normalCalisma / gunlukKatsayi;
						resmiTatilAdet = resmiTatilSaat / gunlukKatsayi;
						haftaTatilAdet = haftaTatilSaat / gunlukKatsayi;
						artikAdet = 0;

					}
					if (izinGunAdet > 0) {
						detayMap.put(BordroDetayTipi.IZIN_GUN, izinGunAdet);
						// if (calismaModeli.isSaatlikOdeme() == false)
						// normalGunAdet -= izinGunAdet;
					}

					if (normalCalisma != 0)
						detayMap.put(BordroDetayTipi.SAAT_NORMAL, normalCalisma);
					if (resmiTatilSaat > 0)
						detayMap.put(BordroDetayTipi.SAAT_RESMI_TATIL, resmiTatilSaat);
					if (haftaTatilSaat > 0)
						detayMap.put(BordroDetayTipi.SAAT_HAFTA_TATIL, haftaTatilSaat);
					if (izinGunSaat > 0)
						detayMap.put(BordroDetayTipi.SAAT_IZIN, izinGunSaat);
					if (!kaydet)
						denklestirmeBordro = (PersonelDenklestirmeBordro) denklestirmeBordro.cloneEmpty();
					denklestirmeBordro.setNormalGunAdet(normalGunAdet);
					denklestirmeBordro.setHaftaTatilAdet(haftaTatilAdet);
					denklestirmeBordro.setResmiTatilAdet(resmiTatilAdet);
					denklestirmeBordro.setArtikAdet(artikAdet);
					if (kaydet) {
						if (denklestirmeBordro.isGuncellendi()) {
							pdksEntityController.saveOrUpdate(session, entityManager, denklestirmeBordro);
							flush = true;
						}

					}
					ap.setDenklestirmeBordro(denklestirmeBordro);

					HashMap<BordroDetayTipi, PersonelDenklestirmeBordroDetay> detayMap1 = new HashMap<BordroDetayTipi, PersonelDenklestirmeBordroDetay>();
					denklestirmeBordro.setDetayMap(detayMap1);
					if (kaydet || fazlaMesaiHesapla) {
						if (devamPrim != null) {
							detayMap.put(BordroDetayTipi.DEVAMSIZLIK_PRIMI, devamPrim.getIslemDurum() != null && devamPrim.getIslemDurum() ? 1.0d : 0.0d);
						}
						if (!detayMap.isEmpty()) {
							for (BordroDetayTipi bordroDetayTipi : detayMap.keySet()) {
								PersonelDenklestirmeBordroDetay bordroDetay = null;
								String detayKey = PersonelDenklestirmeBordroDetay.getDetayKey(denklestirmeBordro, bordroDetayTipi.value());
								if (bordroDetayMap.containsKey(detayKey)) {
									bordroDetay = bordroDetayMap.get(detayKey);
									bordroDetayMap.remove(detayKey);
								} else {
									bordroDetay = new PersonelDenklestirmeBordroDetay(denklestirmeBordro, bordroDetayTipi);
								}
								if (!kaydet)
									bordroDetay = (PersonelDenklestirmeBordroDetay) bordroDetay.cloneEmpty();
								detayMap1.put(bordroDetayTipi, bordroDetay);
								bordroDetay.setGuncellendi(bordroDetay.getId() == null);
								bordroDetay.setMiktar(detayMap.get(bordroDetayTipi));
								if (kaydet) {
									if (bordroDetay.isGuncellendi()) {
										pdksEntityController.saveOrUpdate(session, entityManager, bordroDetay);
										flush = true;
									}
								}
							}
							if (calismaModeli.isFazlaMesaiVarMi()) {
								if (ap.getGecenAyFazlaMesai(loginUser) != 0)
									baslikMap.put(ortakIslemler.devredenMesaiKod(), Boolean.TRUE);
								if (ap.getFazlaMesaiSure() > 0)
									baslikMap.put(ortakIslemler.ucretiOdenenKod(), Boolean.TRUE);
								if (ap.getDevredenSure() != 0)
									baslikMap.put(ortakIslemler.devredenBakiyeKod(), Boolean.TRUE);
								if (ap.getAylikNetFazlaMesai() != 0)
									baslikMap.put(ortakIslemler.gerceklesenMesaiKod(), Boolean.TRUE);
							}
						}
					} else if (denklestirmeBordroId != null) {
						for (String key : bordroDetayMap.keySet()) {
							PersonelDenklestirmeBordroDetay bordroDetay = bordroDetayMap.get(key);
							if (bordroDetay.getPersonelDenklestirmeBordro().getId().equals(denklestirmeBordroId)) {
								detayMap1.put(bordroDetay.getBordroDetayTipi(), bordroDetay);
							}
						}
					}

				}

				detayMap = null;
				if (flush)
					session.flush();
				if (saatlikCalismaVar) {
					String keyEk = saatlikCalisma ? "" : "G";
					baslikGuncelle(baslikMap, ortakIslemler.normalCalismaSaatKod() + keyEk, saatlikCalisma ? normalCalisma : normalSaat);
					baslikGuncelle(baslikMap, ortakIslemler.haftaTatilCalismaSaatKod() + keyEk, haftaTatilSaat);
					baslikGuncelle(baslikMap, ortakIslemler.resmiTatilCalismaSaatKod() + keyEk, resmiTatilSaat);
					baslikGuncelle(baslikMap, ortakIslemler.izinSureSaatKod() + keyEk, izinGunSaat);
					baslikGuncelle(baslikMap, ortakIslemler.izinSureGunAdetKod(), izinGunAdet);
				}
				if (ap.getDenklestirmeBordro() != null) {
					baslikGuncelle(baslikMap, ortakIslemler.ucretliIzinGunKod(), ap.getDenklestirmeBordro().getUcretliIzin().doubleValue());
					baslikGuncelle(baslikMap, ortakIslemler.ucretsizIzinGunKod(), ap.getDenklestirmeBordro().getUcretsizIzin().doubleValue());
					baslikGuncelle(baslikMap, ortakIslemler.hastalikIzinGunKod(), ap.getDenklestirmeBordro().getRaporluIzin().doubleValue());
					baslikGuncelle(baslikMap, ortakIslemler.normalGunKod(), ap.getDenklestirmeBordro().getNormalGunAdet());
					baslikGuncelle(baslikMap, ortakIslemler.haftaTatilGunKod(), ap.getDenklestirmeBordro().getHaftaTatilAdet());
					baslikGuncelle(baslikMap, ortakIslemler.artikGunKod(), ap.getDenklestirmeBordro().getArtikAdet());
					baslikGuncelle(baslikMap, ortakIslemler.resmiTatilGunKod(), ap.getDenklestirmeBordro().getResmiTatilAdet());
					baslikGuncelle(baslikMap, ortakIslemler.bordroToplamGunKod(), ap.getDenklestirmeBordro().getBordroToplamGunAdet());
				}

			}
			if (kaydet && !bordroDetayMap.isEmpty()) {
				for (String key : bordroDetayMap.keySet()) {
					session.delete(bordroDetayMap.get(key));
				}
				session.flush();
			}
			bordroDetayMap = null;
			bordroMap = null;
		}
		return baslikMap;
	}

	/**
	 * @param detayMap
	 * @param calismaGun
	 * @param izinKodu
	 * @param artiGun
	 * @return
	 */
	private double izinDetayYaz(LinkedHashMap<BordroDetayTipi, Double> detayMap, double calismaGun, String izinKodu, double artiGun) {
		BordroDetayTipi bordroTipi = null;
		try {
			if (PdksUtil.hasStringValue(izinKodu))
				bordroTipi = BordroDetayTipi.fromValue(izinKodu);
		} catch (Exception e) {
		}
		if (bordroTipi != null) {
			double miktar = (detayMap.containsKey(bordroTipi) ? detayMap.get(bordroTipi) : 0.0d) + artiGun;
			detayMap.put(bordroTipi, miktar);
			if (bordroTipi.equals(BordroDetayTipi.RAPORLU_IZIN) || bordroTipi.equals(BordroDetayTipi.UCRETSIZ_IZIN))
				calismaGun = 0;

		}
		return calismaGun;
	}

	/**
	 * @param baslikMap
	 * @param key
	 * @param deger
	 */
	public void baslikGuncelle(TreeMap<String, Boolean> baslikMap, String key, Double deger) {
		if (!baslikMap.containsKey(key) && deger != null && deger.doubleValue() != 0)
			baslikMap.put(key, Boolean.TRUE);
	}

	/**
	 * @param talepId
	 * @param session
	 */
	public Long fazlaMesaiOtomatikHareketSil(Long talepId, Session session) {
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		Long sonuc = null;
		try {
			if (talepId != null) {
				StringBuffer sp = new StringBuffer("SP_DELETE_FAZLA_MESAI_TALEP_HAREKET_DATA");
				veriMap.put("talepId", talepId);
				if (session != null)
					veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
				List list = pdksEntityController.execSPList(veriMap, sp, null);
				if (list != null && !list.isEmpty())
					sonuc = ((BigInteger) list.get(0)).longValue();
			}

		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		veriMap = null;
		return sonuc;
	}

	/**
	 * @param yil
	 * @param ay
	 * @param ardisik
	 * @param bolumId
	 * @param ardisikDurum
	 * @param idMap
	 * @param session
	 * @return
	 */
	public List<AylikPuantaj> getAylikFazlaMesaiKontrol(int yil, int ay, Integer ardisik, Long bolumId, boolean ardisikDurum, TreeMap<Long, Personel> idMap, Session session) {
		List<AylikPuantaj> aylikPuantajList = new ArrayList<AylikPuantaj>();
		StringBuffer sb = new StringBuffer();
		sb.append("select * from " + DenklestirmeAy.TABLE_NAME + " " + PdksEntityController.getSelectLOCK());
		sb.append(" where " + DenklestirmeAy.COLUMN_NAME_YIL + " = :y and " + DenklestirmeAy.COLUMN_NAME_AY + " <= :a");
		HashMap fields = new HashMap();
		fields.put("a", ay);
		fields.put("y", yil);
		fields.put(PdksEntityController.MAP_KEY_MAP, "getAy");
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		TreeMap<Integer, DenklestirmeAy> donemMap = pdksEntityController.getObjectBySQLMap(sb, fields, DenklestirmeAy.class, false);
		if (!donemMap.isEmpty()) {
			String maxCalismaSaatStr = ortakIslemler.getParameterKey("maxCalismaSaat");

			Double maxCalismaSaat = null;
			try {
				if (PdksUtil.hasStringValue(maxCalismaSaatStr))
					maxCalismaSaat = Double.parseDouble(maxCalismaSaatStr);
			} catch (Exception e) {
			}
			if (maxCalismaSaat == null || maxCalismaSaat.doubleValue() < 0.0d)
				maxCalismaSaatStr = "D." + DenklestirmeAy.COLUMN_NAME_GUN_MAX_CALISMA_SURESI;

			sb = new StringBuffer();
			sb.append("SP_GET_FAZLA_MESAI_KONTROL");
			LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
			linkedHashMap.put("perList", ortakIslemler.getListIdStr(new ArrayList(idMap.keySet())));
			linkedHashMap.put("yil", String.valueOf(yil));
			linkedHashMap.put("ay", String.valueOf(ay));
			linkedHashMap.put("ardisik", String.valueOf(ardisikDurum ? ardisik : 32));
			linkedHashMap.put("ardisikDurum", ardisikDurum ? 1 : 0);
			linkedHashMap.put("maxCalismaSaatStr", maxCalismaSaatStr);

			if (session != null)
				linkedHashMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Object[]> alanList = null;
			try {
				alanList = pdksEntityController.execSPList(linkedHashMap, sb, null);
				// if (session != null)
				// fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				// alanList = pdksEntityController.getObjectBySQLList(sb, fields, null);
			} catch (Exception e) {
				e.printStackTrace();
			}
			ardisikDurum = false;
			if (alanList != null && !alanList.isEmpty()) {
				TreeMap<String, Liste> map = new TreeMap<String, Liste>();
				for (Object[] objects : alanList) {
					if (objects != null) {
						Long id = ((BigDecimal) objects[0]).longValue();
						Integer donemAy = (Integer) objects[1];
						Long perId = ((BigDecimal) objects[3]).longValue();
						if (donemMap.containsKey(donemAy) && idMap.containsKey(perId)) {
							boolean ardisikDeger = ((Integer) objects[4]) > 0;
							Integer limit = (Integer) objects[5];
							Double fazlaMesai = (Double) objects[6];
							DenklestirmeAy da = donemMap.get(donemAy);
							AylikPuantaj aylikPuantaj = new AylikPuantaj(da);
							Personel personel = idMap.get(perId);
							aylikPuantaj.setPdksPersonel(personel);
							aylikPuantaj.setPersonelDenklestirme(new PersonelDenklestirme(personel, da, null));
							aylikPuantaj.getPersonelDenklestirme().setId(id);
							aylikPuantaj.setAyrikHareketVar(ardisikDeger);
							if (!ardisikDurum)
								ardisikDurum = ardisikDeger;
							aylikPuantaj.setFazlaMesaiSure(fazlaMesai);
							aylikPuantaj.setGunSayisi(limit);
							String key = (bolumId == null && personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "") + (personel.getYoneticisi() != null ? personel.getYoneticisi().getAdSoyad() : "") + personel.getAdSoyad() + "" + personel.getPdksSicilNo();
							List<AylikPuantaj> puantajList = null;
							Liste liste = null;

							if (!map.containsKey(key)) {
								puantajList = new ArrayList<AylikPuantaj>();
								liste = new Liste(key, puantajList);
								map.put(key, liste);
							} else {
								liste = map.get(key);
								puantajList = (List<AylikPuantaj>) liste.getValue();
							}
							puantajList.add(aylikPuantaj);

						}
					}
				}
				if (!map.isEmpty()) {
					List<Liste> puantajList = PdksUtil.sortObjectStringAlanList(new ArrayList(map.values()), "getId", null);
					for (Liste liste : puantajList) {
						List<AylikPuantaj> pl = (List<AylikPuantaj>) liste.getValue();
						aylikPuantajList.addAll(pl);
						pl = null;
					}
				}
				map = null;
			}
			if (!ardisikDurum)
				ardisik = null;

		}
		return aylikPuantajList;
	}

	/**
	 * @param puantajList
	 * @param izinTipiVardiyaList
	 * @param izinTipiPersonelVardiyaMap
	 */
	public void personelIzinAdetleriOlustur(List<AylikPuantaj> puantajList, List<Vardiya> izinTipiVardiyaList, TreeMap<String, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap) {
		if (puantajList != null) {
			for (AylikPuantaj aylikPuantaj : puantajList) {
				TreeMap<String, Integer> izinAdetMap = new TreeMap<String, Integer>();
				aylikPuantaj.setIzinAdetMap(izinAdetMap);
				Personel personel = aylikPuantaj.getPdksPersonel();
				List<Integer> izinAdetList = new ArrayList<Integer>();
				for (Vardiya vardiya : izinTipiVardiyaList) {
					Integer adet = getVardiyaAdet(izinTipiPersonelVardiyaMap, personel, vardiya);
					izinAdetMap.put(vardiya.getKisaAdi(), adet);
					izinAdetList.add(adet);
				}
				aylikPuantaj.setIzinAdetList(izinAdetList);
			}
		}
	}

	/**
	 * @param yil
	 * @param ay
	 * @param bolumId
	 * @param puantajList
	 * @param session
	 * @return
	 */
	public List<String> getFazlaMesaiUyari(int yil, int ay, Long bolumId, List<AylikPuantaj> puantajList, Session session) {
		List<String> list = new ArrayList<String>();
		try {
			User loginUser = authenticatedUser != null ? authenticatedUser : ortakIslemler.getSistemAdminUser(session);
			String maxToplamMesaiStr = ortakIslemler.getParameterKey("maxToplamMesai");
			if (PdksUtil.hasStringValue(maxToplamMesaiStr)) {
				Double maxToplamMesai = null;
				try {
					maxToplamMesai = Double.parseDouble(maxToplamMesaiStr);
				} catch (Exception e) {
					maxToplamMesai = null;
				}

				if (maxToplamMesai != null && maxToplamMesai.doubleValue() > 0.0d) {
					TreeMap<Long, Double> maxToplamMesaiMap = new TreeMap<Long, Double>();
					TreeMap<Long, AylikPuantaj> pIdList = new TreeMap<Long, AylikPuantaj>();
					TreeMap<Long, Personel> perIdMap = new TreeMap<Long, Personel>();
					for (AylikPuantaj ap : puantajList) {
						Personel per = ap.getPdksPersonel();
						if (per != null) {
							pIdList.put(per.getId(), ap);
							maxToplamMesaiMap.put(per.getId(), 0.0d);
							perIdMap.put(per.getId(), per);

						}
					}
					if (!perIdMap.isEmpty()) {
						List<AylikPuantaj> aylikPuantajList = getAylikFazlaMesaiKontrol(yil, ay, null, bolumId, false, perIdMap, session);
						for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
							if (aylikPuantaj != null && aylikPuantaj.getFazlaMesaiSure() > 0.0d) {
								Personel per = aylikPuantaj.getPdksPersonel();
								double maxToplamMesaiSaat = maxToplamMesaiMap.get(per.getId()) + aylikPuantaj.getFazlaMesaiSure();
								maxToplamMesaiMap.put(per.getId(), maxToplamMesaiSaat);
							}
						}
						for (AylikPuantaj ap : puantajList) {
							double mesai = maxToplamMesaiMap.get(ap.getPdksPersonel().getId());
							if (mesai >= maxToplamMesai) {
								Personel personel = ap.getPdksPersonel();
								String str = (puantajList.size() > 1 ? personel.getPdksSicilNo() + " " : "") + personel.getAdSoyad() + " toplam " + loginUser.sayiFormatliGoster(mesai) + " saat mesaisi oluşmuştur.";
								list.add(str);
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * @param entityManagerInput
	 * @param pdksEntityControllerInput
	 * @param ortakIslemlerInput
	 * @param loginUser
	 */
	public void setInject(EntityManager entityManagerInput, PdksEntityController pdksEntityControllerInput, OrtakIslemler ortakIslemlerInput, User loginUser) {
		if (entityManagerInput != null && entityManager == null)
			this.entityManager = entityManagerInput;
		if (pdksEntityControllerInput != null && pdksEntityController == null)
			this.pdksEntityController = pdksEntityControllerInput;
		if (ortakIslemlerInput != null && ortakIslemler == null)
			this.ortakIslemler = ortakIslemlerInput;
		if (loginUser != null && authenticatedUser == null)
			this.authenticatedUser = loginUser;
	}

	/**
	 * @param vardiyaGun
	 * @param loginUser
	 * @return
	 */
	public String getFazlaMesaiSaatleri(VardiyaGun vardiyaGun, User loginUser) {
		if (loginUser == null)
			loginUser = authenticatedUser;
		StringBuffer sb = new StringBuffer();
		String brStr = "<BR></BR>";
		if (vardiyaGun != null && vardiyaGun.getIslemVardiya() != null && vardiyaGun.getVersion() >= 0) {
			String pattern = PdksUtil.getDateTimeFormat();
			Vardiya islemVardiya = vardiyaGun.getIslemVardiya();
			List<String> list = new ArrayList<String>();
			try {
				if (islemVardiya.isCalisma()) {
					if (islemVardiya.getVardiyaTelorans1BasZaman() != null && islemVardiya.getVardiyaFazlaMesaiBasZaman() != null && islemVardiya.getVardiyaBasZaman().getTime() > vardiyaGun.getVardiyaDate().getTime()
							&& islemVardiya.getVardiyaTelorans1BasZaman().getTime() != islemVardiya.getVardiyaFazlaMesaiBasZaman().getTime())
						if (!PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBasZaman(), pattern).equals(PdksUtil.convertToDateString(islemVardiya.getVardiyaBasZaman(), pattern)))
							list.add(PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBasZaman(), pattern) + " - " + PdksUtil.convertToDateString(islemVardiya.getVardiyaBasZaman(), pattern));
					if (islemVardiya.getVardiyaTelorans2BitZaman() != null && islemVardiya.getVardiyaFazlaMesaiBitZaman() != null && islemVardiya.getVardiyaTelorans2BitZaman().getTime() != islemVardiya.getVardiyaFazlaMesaiBitZaman().getTime())
						if (!PdksUtil.convertToDateString(islemVardiya.getVardiyaBitZaman(), pattern).equals(PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBitZaman(), pattern)))
							list.add(PdksUtil.convertToDateString(islemVardiya.getVardiyaBitZaman(), pattern) + " - " + PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBitZaman(), pattern));
				} else if (islemVardiya.getVardiyaFazlaMesaiBasZaman() != null && islemVardiya.getVardiyaFazlaMesaiBitZaman() != null)
					if (!PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBasZaman(), pattern).equals(PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBitZaman(), pattern)))
						list.add(PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBasZaman(), pattern) + " - " + PdksUtil.convertToDateString(islemVardiya.getVardiyaFazlaMesaiBitZaman(), pattern));
			} catch (Exception e) {
				logger.error(loginUser.getAdSoyad() + " " + loginUser.getCalistigiSayfa() + " " + vardiyaGun.getVardiyaKeyStr() + " --> " + e);
			}
			Tatil tatil = vardiyaGun.getTatil();
			try {
				if (tatil != null && tatil.isYarimGunMu() && tatil.getVardiyaMap() != null && islemVardiya != null) {
					Vardiya vardiya = tatil.getVardiyaMap().get(islemVardiya.getId());
					if (vardiya != null)
						islemVardiya.setArifeBaslangicTarihi(vardiya.getArifeBaslangicTarihi());
				}
			} catch (Exception e) {

			}

			logger.debug("");
			String arife = "";
			if (!list.isEmpty()) {
				sb.append("<p align=\"left\">");

				if (loginUser != null && (loginUser.isAdmin() || loginUser.isIK() || loginUser.isSistemYoneticisi())) {
					if (islemVardiya.isCalisma()) {
						if (islemVardiya.getArifeBaslangicTarihi() != null)
							arife = "<B>Arife Tatil : </B>" + PdksUtil.convertToDateString(islemVardiya.getArifeBaslangicTarihi(), pattern) + brStr;
						sb.append("<B>Telorans Aralık : </B>" + PdksUtil.convertToDateString(islemVardiya.getVardiyaTelorans1BasZaman(), pattern) + " - " + PdksUtil.convertToDateString(islemVardiya.getVardiyaTelorans2BitZaman(), pattern) + brStr);

					}
					if (tatil != null) {
						VardiyaGun oncekiVardiyaGun = vardiyaGun.getOncekiVardiyaGun();
						if (oncekiVardiyaGun != null && oncekiVardiyaGun.isAyinGunu() == false && oncekiVardiyaGun.getVardiya() != null) {
							Vardiya tmpVardiya = vardiyaGun.getOncekiVardiyaGun().getIslemVardiya();
							if (tmpVardiya.getBasDonem() > tmpVardiya.getBitDonem()) {
								sb.append("<B>" + PdksUtil.convertToDateString(oncekiVardiyaGun.getVardiyaDate(), PdksUtil.getDateFormat()) + " Vardiya : </B>" + oncekiVardiyaGun.getVardiyaPlanAdi() + brStr);
							}
						}

					}
				}

				sb.append("<B>Fazla Çalışma Saat : </B>");
				if (list.size() > 1) {
					sb.append("<OL>");
					for (String string : list) {
						sb.append("<LI style=\"text-align: left;\">" + string + "</LI>");
					}
					sb.append("</OL>");
				} else
					sb.append(list.get(0));
				sb.append("</p>");
			}

			if (tatil != null && (loginUser.isIK() || loginUser.isSistemYoneticisi() || loginUser.isAdmin())) {
				if (PdksUtil.hasStringValue(arife)) {
					sb.append("<p align=\"left\">");
					sb.append(arife);
					sb.append("</p>");
				}
			}

			list = null;
		}
		String str = sb.toString();
		sb = null;
		return str;
	}

	/**
	 * @param denklestirmeAy
	 * @param user
	 * @return
	 */
	public boolean getDurum(DenklestirmeAy denklestirmeAy, User user) {
		boolean durum = false;
		if (denklestirmeAy != null) {
			if (user == null)
				user = authenticatedUser;
			boolean adminRole = user.isAdmin() || user.isSistemYoneticisi() || user.isIKAdmin();
			if (adminRole)
				durum = denklestirmeAy.getDurum() || denklestirmeAy.getGuncelleIK();
			else
				durum = denklestirmeAy.getDurum();
		}
		return durum;
	}

	/**
	 * @param denklestirmeAy
	 * @return
	 */
	public boolean getDurum(DenklestirmeAy denklestirmeAy) {
		boolean durum = getDurum(denklestirmeAy, authenticatedUser);
		return durum;
	}

	/**
	 * @param selectList
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiDepartmanList(AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		Long depId = null;
		if (loginUser.isTesisSuperVisor() || loginUser.isIK_Tesis() || loginUser.isIKSirket() || loginUser.isSirketSuperVisor() || loginUser.isProjeMuduru()) {
			Personel personel = loginUser.getPdksPersonel();
			depId = personel.getSirket().getDepartman().getId();
		}
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("departmanId", depId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", denklestirme);
		paramsMap.put("tipi", "D");
		paramsMap.put("fieldName", Departman.COLUMN_NAME_ID);
		List<Departman> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		// List<Departman> list = ortakIslemler.getFazlaMesaiList(loginUser, depId, null, null, null, null, aylikPuantaj, "D", denklestirme, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("departman", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Departman veri = (Departman) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		paramsMap = null;
		list = null;
		return selectList;
	}

	/**
	 * @param dm
	 * @param perList
	 * @param session
	 * @return
	 */
	public List<Personel> getFazlaMesaiPersonelList(DenklestirmeAy dm, List<Personel> perList, Session session) {
		List<Personel> list = null;
		if (dm != null && dm.getId() != null && perList != null && !perList.isEmpty()) {
			List<Long> idList = new ArrayList<Long>();
			for (Personel personel : perList)
				idList.add(personel.getId());
			String fieldName = "s";
			StringBuffer sb = new StringBuffer();
			sb.append("select distinct P.* from " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + CalismaModeliAy.TABLE_NAME + " CD " + PdksEntityController.getJoinLOCK() + " on CD." + CalismaModeliAy.COLUMN_NAME_ID + " = PD." + PersonelDenklestirme.COLUMN_NAME_CALISMA_MODELI_AY);
			sb.append(" inner join " + CalismaModeli.TABLE_NAME + " CM " + PdksEntityController.getJoinLOCK() + " on CM." + CalismaModeli.COLUMN_NAME_ID + " = CD." + CalismaModeliAy.COLUMN_NAME_CALISMA_MODELI);
			sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = " + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
			sb.append(" where PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + dm.getId());
			sb.append(" and (P.MAIL_TAKIP=1 or P.PDKS=1 or CM.FAZLA_CALISMA_GORUNTULENSIN=1)");
			sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
			HashMap fields = new HashMap();
			fields.put(fieldName, idList);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			try {
				// list = pdksEntityController.getObjectBySQLList(sb, fields, Personel.class);
				list = pdksEntityController.getSQLParamList(idList, sb, fieldName, fields, Personel.class, session);
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
			idList = null;
		}
		if (list == null)
			list = new ArrayList<Personel>();
		return list;

	}

	/**
	 * @param departmanId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiSirketList(Long departmanId, AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		Sirket sirketPersonel = null;
		if (loginUser.isTesisSuperVisor() || loginUser.isIK_Tesis() || loginUser.isIKSirket() || loginUser.isSirketSuperVisor() || loginUser.isProjeMuduru()) {
			if (loginUser.getYetkiliTesisler() == null || loginUser.getYetkiliTesisler().isEmpty()) {
				Personel personel = loginUser.getPdksPersonel();
				sirketPersonel = personel.getSirket();
				departmanId = sirketPersonel.getDepartman().getId();
			}

		}
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("departmanId", departmanId);
		paramsMap.put("sirket", sirketPersonel);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", denklestirme);
		paramsMap.put("tipi", "S");
		paramsMap.put("fieldName", Sirket.COLUMN_NAME_ID);
		List<Sirket> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		if (loginUser != null && loginUser.isIKSirket()) {
			if (loginUser.getYetkiliTesisler() == null || loginUser.getYetkiliTesisler().isEmpty()) {
				try {
					list = PdksUtil.sortObjectStringAlanList(list, "getAd", null);
					Sirket sirketUser = list != null && loginUser.getPdksPersonel() != null ? loginUser.getPdksPersonel().getSirket() : null;
					if (sirketUser != null) {
						for (Iterator iterator = list.iterator(); iterator.hasNext();) {
							Sirket sirket = (Sirket) iterator.next();
							if (!sirket.getId().equals(sirketUser.getId()))
								iterator.remove();
						}
					}
				} catch (Exception e) {

				}
			}
		}

		List<SelectItem> selectList = ortakIslemler.getSelectItemList("sirket", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAd", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Sirket veri = (Sirket) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAd()));
			}
		}
		list = null;
		paramsMap = null;
		return selectList;
	}

	/**
	 * @param sirket
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiTesisList(Sirket sirket, AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		List<Tanim> list = null;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		Long tesisId = null;
		if (sirket != null && (sirket.isTesisDurumu() || loginUser.isTesisSuperVisor() || loginUser.isIK_Tesis())) {
			if (loginUser.getYetkiliTesisler() == null || loginUser.getYetkiliTesisler().isEmpty()) {
				if (loginUser.isTesisSuperVisor() || loginUser.isIK_Tesis()) {
					Personel personel = loginUser.getPdksPersonel();
					if (personel.getTesis() != null)
						tesisId = personel.getTesis().getId();
				}
			}
			paramsMap.put("loginUser", loginUser);
			paramsMap.put("sirket", sirket);
			paramsMap.put("tesisId", tesisId != null ? String.valueOf(tesisId) : null);
			paramsMap.put("aylikPuantaj", aylikPuantaj);
			paramsMap.put("denklestirme", denklestirme);
			paramsMap.put("tipi", "T");
			paramsMap.put("fieldName", Tanim.COLUMN_NAME_ID);
			list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
			// list = ortakIslemler.getFazlaMesaiList(loginUser, null, sirket, tesisId, null, null, aylikPuantaj, "T", denklestirme, session);
		}
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("tesis", loginUser);
		if (list != null && !list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Tanim veri : list) {
				if (tesisId == null || tesisId.equals(veri.getId()))
					selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}

		}
		list = null;
		paramsMap = null;
		return selectList;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param aylikPuantaj
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiTanimsizBolumList(Sirket sirket, String tesisId, AylikPuantaj aylikPuantaj, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("sirket", sirket);
		paramsMap.put("tesisId", tesisId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", true);
		paramsMap.put("tipi", "B+");
		paramsMap.put("fieldName", Tanim.COLUMN_NAME_ID);
		List<Tanim> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("tanimsizBolum", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		list = null;
		paramsMap = null;
		return selectList;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiBolumList(Sirket sirket, String tesisId, AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("sirket", sirket);
		paramsMap.put("tesisId", tesisId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", denklestirme);
		paramsMap.put("tipi", "B");
		paramsMap.put("fieldName", Tanim.COLUMN_NAME_ID);
		List<Tanim> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("bolum", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		list = null;
		paramsMap = null;
		return selectList;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param bolumId
	 * @param aylikPuantaj
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiTanimsizAltBolumList(Sirket sirket, String tesisId, Long bolumId, AylikPuantaj aylikPuantaj, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("sirket", sirket);
		paramsMap.put("tesisId", tesisId);
		paramsMap.put("bolumId", bolumId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", true);
		paramsMap.put("tipi", "AB+");
		paramsMap.put("fieldName", Tanim.COLUMN_NAME_ID);
		List<Tanim> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("tanimsizAltBolum", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		list = null;
		paramsMap = null;
		return selectList;

	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param bolumId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiAltBolumList(Sirket sirket, String tesisId, Long bolumId, AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("sirket", sirket);
		paramsMap.put("tesisId", tesisId);
		paramsMap.put("bolumId", bolumId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", denklestirme);
		paramsMap.put("tipi", "AB");
		paramsMap.put("fieldName", Tanim.COLUMN_NAME_ID);
		List<Tanim> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("altBolumler", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		list = null;
		paramsMap = null;
		return selectList;
	}

	/**
	 * @param tarih
	 * @param list
	 * @param sirketId
	 * @param tesisId
	 * @param ekSaha3Id
	 * @param ekSaha4Id
	 * @param session
	 */
	public void setFazlaMesaiPersonel(Date tarih, List<String> list, Long sirketId, Long tesisId, Long ekSaha3Id, Long ekSaha4Id, Session session) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(tarih);

		DenklestirmeAy denklestirmeAyDonem = ortakIslemler.getSQLDenklestirmeAy(cal, session);
		Sirket sirket = null;
		if (sirketId != null) {

			sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);

		}
		List<Personel> donemPerList = getFazlaMesaiPersonelList(sirket, tesisId != null ? String.valueOf(tesisId) : null, ekSaha3Id, ekSaha4Id, denklestirmeAyDonem != null ? new AylikPuantaj(denklestirmeAyDonem) : null, false, session);
		List<String> searchSicilNoList = new ArrayList<String>(list);
		list.clear();
		if (donemPerList != null) {
			for (Personel personel : donemPerList) {
				if (searchSicilNoList.contains(personel.getPdksSicilNo()))
					if (personel.isCalisiyorGun(tarih))
						list.add(personel.getPdksSicilNo());
			}
		}
		searchSicilNoList = null;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param bolumId
	 * @param altBolumId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<Personel> getFazlaMesaiPersonelList(Sirket sirket, String tesisId, Long bolumId, Long altBolumId, AylikPuantaj aylikPuantaj, boolean denklestirme, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		LinkedHashMap<String, Object> paramsMap = new LinkedHashMap<String, Object>();
		paramsMap.put("loginUser", loginUser);
		paramsMap.put("sirket", sirket);
		paramsMap.put("tesisId", tesisId);
		paramsMap.put("bolumId", bolumId);
		paramsMap.put("altBolumId", altBolumId);
		paramsMap.put("aylikPuantaj", aylikPuantaj);
		paramsMap.put("denklestirme", denklestirme);
		paramsMap.put("tipi", "P");
		paramsMap.put("fieldName", Personel.COLUMN_NAME_ID);
		List<Personel> perList = ortakIslemler.getSelectItemList("personel", loginUser);
		List<Personel> list = ortakIslemler.getFazlaMesaiList(paramsMap, session);
		if (list != null) {
			if (!list.isEmpty())
				list = PdksUtil.sortObjectStringAlanList(list, "getAdSoyad", null);
			perList.addAll(list);
			list = null;
		}
		paramsMap = null;
		return perList;
	}

	/**
	 * @param departmanId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param fazlaMesaiTalepDurum
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiMudurSirketList(Long departmanId, AylikPuantaj aylikPuantaj, boolean denklestirme, boolean fazlaMesaiTalepDurum, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		List<Sirket> list = ortakIslemler.getFazlaMesaiMudurList(loginUser, departmanId, null, null, null, aylikPuantaj, "S", denklestirme, fazlaMesaiTalepDurum, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("mudurSirket", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAd", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Sirket veri = (Sirket) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAd()));
			}
		}
		return selectList;
	}

	/**
	 * @param sirket
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param fazlaMesaiTalepDurum
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiMudurTesisList(Sirket sirket, AylikPuantaj aylikPuantaj, boolean denklestirme, boolean fazlaMesaiTalepDurum, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		List<Tanim> list = ortakIslemler.getFazlaMesaiMudurList(loginUser, null, sirket, null, null, aylikPuantaj, "T", denklestirme, fazlaMesaiTalepDurum, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("mudurTesis", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}

		return selectList;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param fazlaMesaiTalepDurum
	 * @param session
	 * @return
	 */
	public List<SelectItem> getFazlaMesaiMudurBolumList(Sirket sirket, String tesisId, AylikPuantaj aylikPuantaj, boolean denklestirme, boolean fazlaMesaiTalepDurum, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		List<Tanim> list = ortakIslemler.getFazlaMesaiMudurList(loginUser, null, sirket, tesisId, null, aylikPuantaj, "B", denklestirme, fazlaMesaiTalepDurum, session);
		List<SelectItem> selectList = ortakIslemler.getSelectItemList("mudurBolum", loginUser);
		if (!list.isEmpty()) {
			list = PdksUtil.sortObjectStringAlanList(list, "getAciklama", null);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Tanim veri = (Tanim) iterator.next();
				selectList.add(new SelectItem(veri.getId(), veri.getAciklama()));
			}
		}
		return selectList;
	}

	/**
	 * @param sirket
	 * @param tesisId
	 * @param bolumId
	 * @param aylikPuantaj
	 * @param denklestirme
	 * @param fazlaMesaiTalepDurum
	 * @param session
	 * @return
	 */
	public List<Personel> getFazlaMesaiMudurPersonelList(Sirket sirket, String tesisId, Long bolumId, AylikPuantaj aylikPuantaj, boolean denklestirme, boolean fazlaMesaiTalepDurum, Session session) {
		User loginUser = aylikPuantaj != null && aylikPuantaj.getLoginUser() != null ? aylikPuantaj.getLoginUser() : null;
		if (loginUser == null)
			loginUser = authenticatedUser;
		List<Personel> list = ortakIslemler.getFazlaMesaiMudurList(loginUser, null, sirket, tesisId, bolumId, aylikPuantaj, "P", denklestirme, fazlaMesaiTalepDurum, session);
		if (!list.isEmpty())
			list = PdksUtil.sortObjectStringAlanList(list, "getAdSoyad", null);

		return list;
	}

	/**
	 * @param list
	 * @param yil
	 * @param ay
	 * @param session
	 * @return
	 */
	public List<Sirket> getDenklestirmeSirketList(List<Sirket> list, int yil, int ay, Session session) {
		HashMap fields = new HashMap();

		DenklestirmeAy denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);

		if (denklestirmeAy != null) {
			Calendar cal = Calendar.getInstance();

			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.MONTH, ay - 1);
			cal.set(Calendar.YEAR, yil);
			Date basTarih = PdksUtil.getDate(cal.getTime());
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DATE, -1);
			Date bitTarih = PdksUtil.getDate(cal.getTime());

			fields.clear();
			List<Long> idler = new ArrayList<Long>();
			for (Sirket sirket : list)
				if (!idler.contains(sirket.getId()))
					idler.add(sirket.getId());

			StringBuffer sb = new StringBuffer();
			String fieldName = "s";
			sb.append("select distinct S.* from " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = " + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
			sb.append(" and " + Personel.COLUMN_NAME_SIRKET + " :" + fieldName);
			if (bitTarih != null) {
				sb.append(" and P." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI + " <= :a1 ");
				fields.put("a1", bitTarih);
			}
			if (basTarih != null) {
				sb.append(" and P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= :a2 ");
				fields.put("a2", basTarih);
			}
			ortakIslemler.addIKSirketTesisKriterleri(fields, sb);
			sb.append(" where PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
			sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1 ");

			sb.append(" order by S." + Sirket.COLUMN_NAME_AD);
			fields.put(fieldName, idler);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			try {
				list = pdksEntityController.getSQLParamList(idler, sb, fieldName, fields, Sirket.class, session);
				idler = null;
			} catch (Exception ex) {
				logger.error(ex + "\n" + sb.toString());
			}

		}
		return list;
	}

	/**
	 * @param denklestirmeAy
	 * @param session
	 */
	@Transactional
	public void setFazlaMesaiMaxSure(DenklestirmeAy denklestirmeAy, Session session) {
		denklestirmeAy.setFazlaMesaiMaxSure(ortakIslemler.getFazlaMesaiMaxSure(null));
		pdksEntityController.saveOrUpdate(session, entityManager, denklestirmeAy);
		session.flush();

	}

	/**
	 * @param vardiyaHafta
	 * @param tarih
	 * @param amount
	 * @return
	 */
	public String setVardiyaGunHafta(VardiyaHafta vardiyaHafta, Date tarih, int amount) {
		VardiyaGun vardiyaGun = new VardiyaGun();
		Date vardiyaDate1 = tarih;
		if (amount > 0) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(tarih);
			cal.add(Calendar.DATE, amount);
			vardiyaDate1 = PdksUtil.getDate(cal.getTime());
		}

		logger.debug(PdksUtil.convertToDateString(tarih, "yyyyMMdd") + " " + amount + " " + PdksUtil.convertToDateString(vardiyaDate1, "yyyyMMdd"));
		vardiyaGun.setVardiyaDate(vardiyaDate1);
		String tarihStr = vardiyaGun.getVardiyaDateStr();
		vardiyaHafta.getVardiyaPlan().getVardiyaGunMap().put(tarihStr, vardiyaGun);
		switch (amount) {
		case 0:
			vardiyaHafta.setVardiyaGun1(vardiyaGun);
			break;
		case 1:
			vardiyaHafta.setVardiyaGun2(vardiyaGun);
			break;
		case 2:
			vardiyaHafta.setVardiyaGun3(vardiyaGun);
			break;
		case 3:
			vardiyaHafta.setVardiyaGun4(vardiyaGun);
			break;
		case 4:
			vardiyaHafta.setVardiyaGun5(vardiyaGun);
			break;
		case 5:
			vardiyaHafta.setVardiyaGun6(vardiyaGun);
			break;
		case 6:
			vardiyaHafta.setVardiyaGun7(vardiyaGun);
			break;

		default:
			break;
		}
		vardiyaHafta.getVardiyaGunler().add(vardiyaGun);
		return tarihStr;

	}

	/**
	 * @param vardiyaHaftaList
	 * @param aylikPuantaj
	 * @param donemi
	 * @param tatillerMap
	 * @param sablonVardiyalar
	 * @return
	 */
	public VardiyaPlan haftalikVardiyaOlustur(List<VardiyaHafta> vardiyaHaftaList, AylikPuantaj aylikPuantaj, DepartmanDenklestirmeDonemi donemi, TreeMap<String, Tatil> tatillerMap, List<VardiyaGun> sablonVardiyalar) {
		HashMap<String, Integer> gunHaftaMap = new HashMap<String, Integer>();
		if (vardiyaHaftaList == null)
			vardiyaHaftaList = new ArrayList<VardiyaHafta>();
		VardiyaPlan vardiyaPlanMaster = new VardiyaPlan();
		vardiyaPlanMaster.setDenklestirmeDonemi(donemi);
		vardiyaPlanMaster.setCheckBoxDurum(Boolean.FALSE);
		Date tarih1 = (Date) donemi.getBaslangicTarih().clone();
		int i = 0;
		String donem = String.valueOf(aylikPuantaj.getYil() * 100 + aylikPuantaj.getAy());
		List<String> gunler = new ArrayList<String>();
		if (sablonVardiyalar != null) {
			for (VardiyaGun vg : sablonVardiyalar)
				gunler.add(vg.getVardiyaDateStr());

		}
		aylikPuantaj.setVardiyaHaftaList(vardiyaHaftaList);
		boolean devam = Boolean.TRUE;
		Calendar cal = Calendar.getInstance();
		while (devam) {
			++i;
			VardiyaHafta vardiyaHafta = new VardiyaHafta();
			vardiyaHafta.setVardiyaPlan(vardiyaPlanMaster);
			vardiyaHafta.setHafta(i);
			for (int j = 0; j < 7; j++)
				gunHaftaMap.put(setVardiyaGunHafta(vardiyaHafta, tarih1, j), vardiyaHafta.getHafta());
			vardiyaHafta.setBasTarih(vardiyaHafta.getVardiyaGun1().getVardiyaDate());
			vardiyaHafta.setBitTarih(vardiyaHafta.getVardiyaGun7().getVardiyaDate());
			vardiyaHaftaList.add(vardiyaHafta);
			switch (i) {
			case 1:
				vardiyaPlanMaster.setVardiyaHafta1(vardiyaHafta);
				break;
			case 2:
				vardiyaPlanMaster.setVardiyaHafta2(vardiyaHafta);
				break;
			case 3:
				vardiyaPlanMaster.setVardiyaHafta3(vardiyaHafta);
				break;
			case 4:
				vardiyaPlanMaster.setVardiyaHafta4(vardiyaHafta);
				break;
			case 5:
				vardiyaPlanMaster.setVardiyaHafta5(vardiyaHafta);
				break;
			case 6:
				vardiyaPlanMaster.setVardiyaHafta6(vardiyaHafta);
				break;
			default:
				break;
			}
			for (Iterator iterator = vardiyaHafta.getVardiyaGunler().iterator(); iterator.hasNext();) {
				VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
				String vardiyaDateStr = vardiyaGun.getVardiyaDateStr();
				vardiyaGun.setAyinGunu(vardiyaDateStr.startsWith(donem));
				if (sablonVardiyalar != null && !gunler.contains(vardiyaDateStr)) {
					gunler.add(vardiyaDateStr);
					sablonVardiyalar.add(vardiyaGun);
				}
				if (tatillerMap != null && tatillerMap.containsKey(vardiyaDateStr))
					vardiyaGun.setTatil(tatillerMap.get(vardiyaDateStr));
				else if (vardiyaGun.getVardiya() != null && vardiyaGun.getVardiya().isCalisma()) {
					vardiyaGun.setVardiyaZamani();
					String key = PdksUtil.convertToDateString(vardiyaGun.getIslemVardiya().getVardiyaBitZaman(), "yyyyMMdd");
					if (tatillerMap.containsKey(key))
						vardiyaGun.setTatil(tatillerMap.get(key));
				}
			}

			tarih1 = ortakIslemler.tariheGunEkleCikar(cal, tarih1, 7);
			if (PdksUtil.tarihKarsilastirNumeric(tarih1, donemi.getBitisTarih()) == 1)
				devam = false;

		}
		gunler = null;
		vardiyaPlanMaster.getVardiyaHaftaList().addAll(vardiyaHaftaList);
		return vardiyaPlanMaster;
	}

	/**
	 * @param pdksDepartman
	 * @param yil
	 * @param ay
	 * @param session
	 * @return
	 */
	public List<SelectItem> sirketDoldur(Departman pdksDepartman, Integer yil, Integer ay, Session session) {
		Calendar cal = Calendar.getInstance();
		if (yil == null)
			yil = cal.get(Calendar.YEAR);
		if (ay == null)
			ay = cal.get(Calendar.MONTH) + 1;
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, ay - 1);
		cal.set(Calendar.YEAR, yil);
		Date basTarih = PdksUtil.getDate(cal.getTime());
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		Date bitTarih = PdksUtil.getDate(cal.getTime());
		HashMap fields = new HashMap();
		fields.put(PdksEntityController.MAP_KEY_MAP, "getId");
		fields.put(PdksEntityController.MAP_KEY_SELECT, "sirket");
		if (pdksDepartman != null)
			fields.put("sirket.departman.id=", pdksDepartman.getId());
		fields.put("iseBaslamaTarihi<=", bitTarih);
		fields.put("sskCikisTarihi>=", basTarih);

		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		TreeMap<Long, Sirket> sirketMap = pdksEntityController.getObjectByInnerObjectMapInLogic(fields, Personel.class, false);
		List<SelectItem> list = ortakIslemler.getSelectItemList("sirket", authenticatedUser);
		if (!sirketMap.isEmpty()) {
			List<Sirket> sirketList = PdksUtil.sortObjectStringAlanList(new ArrayList(sirketMap.values()), "getAd", null);
			for (Sirket sirket : sirketList) {
				if (sirket.getFazlaMesai())
					list.add(new SelectItem(sirket.getId(), sirket.getAd()));
			}
		}
		return list;
	}

	/**
	 * @param pdksDepartman
	 * @param sirket
	 * @param departmanId
	 * @param tesisId
	 * @param yil
	 * @param ay
	 * @param denklestirme
	 * @param session
	 * @return
	 */
	public List<SelectItem> bolumDoldur(Departman pdksDepartman, Sirket sirket, Long departmanId, Long tesisId, Integer yil, Integer ay, Boolean denklestirme, Session session) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		map.put("tip", "ekSaha3");
		map.put("sirket", sirket);
		map.put("pdksDepartman", pdksDepartman);
		map.put("departmanId", departmanId);
		if (tesisId != null)
			map.put("tesisId", tesisId);
		Calendar cal = Calendar.getInstance();
		if (yil == null)
			yil = cal.get(Calendar.YEAR);
		if (ay == null)
			ay = cal.get(Calendar.MONTH) + 1;
		cal.set(Calendar.DATE, 1);
		cal.set(Calendar.MONTH, ay - 1);
		cal.set(Calendar.YEAR, yil);
		Date basTarih = PdksUtil.getDate(cal.getTime());
		cal.add(Calendar.MONTH, 1);
		cal.add(Calendar.DATE, -1);
		Date bitTarih = PdksUtil.getDate(cal.getTime());
		map.put("basTarih", basTarih);
		map.put("bitTarih", bitTarih);

		if (denklestirme) {
			map.put("yil", yil);
			map.put("ay", ay);
		}
		List<SelectItem> list = bolumTesisMapDoldur(map, session);
		return list;
	}

	/**
	 * @param map
	 * @param session
	 * @return
	 */
	public List<SelectItem> bolumTesisMapDoldur(HashMap<String, Object> map, Session session) {
		User loginUser = authenticatedUser != null ? authenticatedUser : ortakIslemler.getSistemAdminUser(session);
		HashMap fields = new HashMap();
		List<SelectItem> gorevTipiList = null;
		Date basTarih = (Date) map.get("basTarih");
		Date bitTarih = (Date) map.get("bitTarih");
		String fieldAdi = (String) map.get("tip");
		Sirket sirket = (Sirket) map.get("sirket");
		Long tesisId = (Long) map.get("tesisId");
		Long departmanId = (Long) map.get("departmanId");
		Departman pdksDepartman = (Departman) map.get("pdksDepartman");
		Boolean denklestirme = (Boolean) map.get("denklestirme");
		if (fieldAdi == null)
			fieldAdi = "ekSaha3";
		fields.put(PdksEntityController.MAP_KEY_SELECT, fieldAdi);
		if (sirket != null) {
			if (sirket.isTesisDurumu()) {
				boolean tesisYetki = ortakIslemler.getParameterKey("tesisYetki").equals("1");
				if (tesisYetki && loginUser.getYetkiliTesisler() != null && !loginUser.getYetkiliTesisler().isEmpty()) {
					List<Long> idler = new ArrayList<Long>();
					for (Iterator iterator = loginUser.getYetkiliTesisler().iterator(); iterator.hasNext();) {
						Tanim tesis = (Tanim) iterator.next();
						idler.add(tesis.getId());
					}
					if (idler.size() == 1) {
						tesisId = idler.get(0);

					} else {
						fields.put("tesis.id", idler);
					}
					idler = null;
				}

				fields.put("sirket.id=", sirket.getId());
				if (tesisId != null && tesisId > 0L)
					fields.put("tesis.id=", tesisId);
			} else {
				if (fieldAdi == null || fieldAdi.equals("tesis"))
					fields.put("sirket.id=", 0L);
				else
					fields.put("sirket.id=", sirket.getId());
			}
		} else
			fields.put("sirket.id=", 0L);

		if (departmanId == null && loginUser.isDirektorSuperVisor())
			departmanId = loginUser.getPdksPersonel().getEkSaha1().getId();
		else {
			List<Long> tesisIdList = null;
			if (authenticatedUser.getYetkiliTesisler() != null && authenticatedUser.getYetkiliTesisler().isEmpty() == false) {
				tesisIdList = new ArrayList<Long>();
				for (Tanim tesis : authenticatedUser.getYetkiliTesisler())
					tesisIdList.add(tesis.getId());
				fields.put("tesis.id ", tesisIdList);
			}
			if (tesisIdList == null && loginUser.isIK_Tesis() && loginUser.getPdksPersonel().getTesis() != null)
				fields.put("tesis.id=", loginUser.getPdksPersonel().getTesis().getId());
		}

		if (departmanId != null)
			fields.put("ekSaha1.id=", departmanId);
		else if (loginUser.isYonetici() && !(loginUser.isIK() || loginUser.isAdmin()))
			fields.put("pdksSicilNo", ortakIslemler.getYetkiTumPersonelNoListesi(loginUser));
		fields.put("iseBaslamaTarihi<=", bitTarih);
		fields.put("sskCikisTarihi>=", basTarih);
		if (pdksDepartman != null && pdksDepartman.isAdminMi() && denklestirme != null && denklestirme)
			fields.put("pdks=", Boolean.TRUE);
		fields.put(fieldAdi + " <> ", null);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<Tanim> tanimlar = pdksEntityController.getObjectByInnerObjectListInLogic(fields, Personel.class);
		if (!tanimlar.isEmpty() && map.containsKey("yil") && map.containsKey("ay")) {
			fields.clear();

			DenklestirmeAy denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy((Integer) map.get("yil"), (Integer) map.get("ay"), session);
			if (denklestirmeAy != null) {
				fields.clear();
				List<Long> idler = new ArrayList<Long>();
				for (Tanim tanim : tanimlar)
					if (!idler.contains(tanim.getId()))
						idler.add(tanim.getId());

				StringBuffer sb = new StringBuffer();
				sb.append("select distinct T.* from " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = " + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
				if (bitTarih != null) {
					sb.append(" and P." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI + " <= :a1 ");
					fields.put("a1", bitTarih);
				}
				if (basTarih != null) {
					sb.append(" and P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= :a2 ");
					fields.put("a2", basTarih);
				}
				if (sirket != null)
					sb.append(" and P." + Personel.COLUMN_NAME_SIRKET + " = " + sirket.getId());
				if (tesisId != null && tesisId > 0L)
					sb.append(" and P." + Personel.COLUMN_NAME_TESIS + " = " + tesisId);
				sb.append(" inner join " + Tanim.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + Tanim.COLUMN_NAME_ID + " = " + (fieldAdi.equals("ekSaha3") ? Personel.COLUMN_NAME_EK_SAHA3 : Personel.COLUMN_NAME_TESIS));
				String fieldName = "t";
				sb.append(" and T." + Tanim.COLUMN_NAME_ID + " :" + fieldName);
				sb.append(" where PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
				sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1 ");
				fields.put(fieldName, idler);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				try {
					// tanimlar = pdksEntityController.getObjectBySQLList(sb, fields, Tanim.class);
					tanimlar = pdksEntityController.getSQLParamList(idler, sb, fieldName, fields, Tanim.class, session);
					idler = null;
				} catch (Exception ex) {
					logger.error(ex + "\n" + sb.toString());
				}

			}
		}
		if (loginUser.isYonetici() || loginUser.isDirektorSuperVisor() || loginUser.isAdmin() || loginUser.isIK() || tanimlar.size() > 50) {
			HashMap<Long, Tanim> tanimMap = new HashMap<Long, Tanim>();
			for (Tanim tanim : tanimlar)
				tanimMap.put(tanim.getId(), tanim);

			tanimlar = null;
			tanimlar = PdksUtil.sortObjectStringAlanList(new ArrayList<Tanim>(tanimMap.values()), "getAciklama", null);
			gorevTipiList = ortakIslemler.getTanimSelectItem("gorevTipi", tanimlar);
			tanimMap = null;

		}
		tanimlar = null;
		return gorevTipiList;

	}

	/**
	 * @param perIdList
	 * @return
	 */
	public List<PersonelDenklestirme> getPdksPersonelDenklestirmeler(List<Long> perIdList, DenklestirmeAy denklestirmeAy, Session session) {
		String fieldName = "p";
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select S.* from " + PersonelDenklestirme.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" where S." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId() + " and S." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
		fields.put(fieldName, perIdList);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<PersonelDenklestirme> list = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, fields, PersonelDenklestirme.class, session);

		fields = null;
		sb = null;
		return list;
	}

	/**
	 * @param ay
	 * @param yil
	 * @param denklestirmeDonemi
	 * @param session
	 * @return
	 */
	public AylikPuantaj getAylikPuantaj(int ay, int yil, DepartmanDenklestirmeDonemi denklestirmeDonemi, Session session) {
		AylikPuantaj aylikPuantaj = new AylikPuantaj();
		if (denklestirmeDonemi == null)
			denklestirmeDonemi = new DepartmanDenklestirmeDonemi();
		if (yil > 0) {

			aylikPuantaj.setAy(ay);
			aylikPuantaj.setYil(yil);

			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.DATE, 1);
			cal.set(Calendar.MONTH, aylikPuantaj.getAy() - 1);
			cal.set(Calendar.YEAR, aylikPuantaj.getYil());

			aylikPuantaj.setIlkGun(PdksUtil.getDate((Date) cal.getTime().clone()));
			int haftaGun = cal.get(Calendar.DAY_OF_WEEK);
			if (haftaGun != Calendar.MONDAY) {
				int cikar = haftaGun != Calendar.SUNDAY ? 2 - haftaGun : -6;
				cal.add(Calendar.DATE, cikar);
				denklestirmeDonemi.setBaslangicTarih(PdksUtil.getDate((Date) cal.getTime().clone()));
			} else
				denklestirmeDonemi.setBaslangicTarih(aylikPuantaj.getIlkGun());
			cal.setTime(aylikPuantaj.getIlkGun());
			cal.add(Calendar.MONTH, 1);
			cal.add(Calendar.DATE, -1);
			aylikPuantaj.setGunSayisi(cal.get(Calendar.DATE));
			haftaGun = cal.get(Calendar.DAY_OF_WEEK);
			aylikPuantaj.setSonGun(PdksUtil.getDate((Date) cal.getTime().clone()));
			if (haftaGun != Calendar.SUNDAY) {
				int ekle = 8 - haftaGun;
				cal.add(Calendar.DATE, ekle);
				denklestirmeDonemi.setBitisTarih(PdksUtil.getDate((Date) cal.getTime().clone()));
			} else
				denklestirmeDonemi.setBitisTarih(aylikPuantaj.getSonGun());
		} else {
			aylikPuantaj.setIlkGun(denklestirmeDonemi.getBaslangicTarih());
			aylikPuantaj.setSonGun(denklestirmeDonemi.getBitisTarih());
			List<VardiyaGun> list = new ArrayList<VardiyaGun>();
			aylikPuantaj.setVardiyalar(list);
			Calendar cal = Calendar.getInstance();
			cal.setTime(denklestirmeDonemi.getBaslangicTarih());
			while (cal.getTime().getTime() <= denklestirmeDonemi.getBitisTarih().getTime()) {
				Date vardiyaDate = (Date) cal.getTime().clone();
				VardiyaGun vardiyaGun = new VardiyaGun();
				vardiyaGun.setVardiyaDate(vardiyaDate);
				cal.add(Calendar.DATE, 1);
				vardiyaGun.setAyinGunu(Boolean.TRUE);
				list.add(vardiyaGun);
			}

		}

		TreeMap<String, Tatil> tatilGunleriMap = ortakIslemler.getTatilGunleri(null, denklestirmeDonemi.getBaslangicTarih(), denklestirmeDonemi.getBitisTarih(), session);
		List<VardiyaGun> vardiyaGunList = aylikPuantaj.getVardiyalar();
		for (Iterator iterator = vardiyaGunList.iterator(); iterator.hasNext();) {
			VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
			vardiyaGun.setTatil(tatilGunleriMap.containsKey(vardiyaGun.getVardiyaDateStr()) ? tatilGunleriMap.get(vardiyaGun.getVardiyaDateStr()) : null);
		}
		denklestirmeDonemi.setTatilGunleriMap(tatilGunleriMap);
		return aylikPuantaj;
	}

	/**
	 * @param departmanId
	 * @param sirketId
	 * @param yil
	 * @param ay
	 * @param fazlaMesai
	 * @param session
	 * @return
	 */
	public List<SelectItem> getBolumDepartmanSelectItems(Long departmanId, Long sirketId, Integer yil, Integer ay, Boolean fazlaMesai, Session session) {
		User loginUser = authenticatedUser != null ? authenticatedUser : ortakIslemler.getSistemAdminUser(session);
		List<SelectItem> bolumDepartmanlari = null;
		List<Tanim> bolumler = null;
		HashMap fields = new HashMap();
		if (departmanId != null) {
			Date bitTarih = null, basTarih = null;
			if (sirketId != null) {
				Calendar cal = Calendar.getInstance();
				if (yil == null)
					yil = cal.get(Calendar.YEAR);
				if (ay == null)
					ay = cal.get(Calendar.MONTH) + 1;
				cal.set(Calendar.DATE, 1);
				cal.set(Calendar.MONTH, ay - 1);
				cal.set(Calendar.YEAR, yil);
				basTarih = PdksUtil.getDate(cal.getTime());
				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.DATE, -1);
				bitTarih = PdksUtil.getDate(cal.getTime());

				fields.put(PdksEntityController.MAP_KEY_MAP, "getId");
				fields.put(PdksEntityController.MAP_KEY_SELECT, "ekSaha3");

				fields.put("sirket.id=", sirketId);
				if (loginUser.isDirektorSuperVisor())
					fields.put("ekSaha1.id=", loginUser.getPdksPersonel().getEkSaha1().getId());
				if (bitTarih != null)
					fields.put("iseBaslamaTarihi<=", bitTarih);
				if (basTarih != null)
					fields.put("sskCikisTarihi>=", basTarih);
				fields.put("ekSaha3<>", null);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				TreeMap<Long, Tanim> bolumMap = pdksEntityController.getObjectByInnerObjectMapInLogic(fields, Personel.class, true);
				if (!bolumMap.isEmpty())
					bolumler = new ArrayList<Tanim>(bolumMap.values());
				bolumMap = null;
			} else
				bolumler = ortakIslemler.getTanimList(Tanim.TIPI_BOLUM_DEPARTMAN + departmanId, session);
		}
		if (bolumler == null || bolumler.isEmpty())
			bolumDepartmanlari = ortakIslemler.getSelectItemList("bolumDepartman", authenticatedUser);
		else {
			if (fazlaMesai) {

				fields.clear();

				DenklestirmeAy denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
				if (denklestirmeAy != null) {
					fields.clear();
					List<Long> idler = new ArrayList<Long>();
					for (Tanim tanim : bolumler)
						if (!idler.contains(tanim.getId()))
							idler.add(tanim.getId());

					StringBuffer sb = new StringBuffer();
					sb.append("select distinct T.* from " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getSelectLOCK() + " ");
					sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = " + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
					if (sirketId != null)
						sb.append(" and P." + Personel.COLUMN_NAME_SIRKET + " = " + sirketId);

					String fieldName = "t";
					sb.append(" inner join " + Tanim.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + Tanim.COLUMN_NAME_ID + " = " + Personel.COLUMN_NAME_EK_SAHA3);
					sb.append(" and T." + Tanim.COLUMN_NAME_ID + " :" + fieldName);
					sb.append(" where PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
					sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1 ");
					fields.put(fieldName, idler);
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					try {
						// bolumler = pdksEntityController.getObjectBySQLList(sb, fields, Tanim.class);
						bolumler = pdksEntityController.getSQLParamList(idler, sb, fieldName, fields, Tanim.class, session);

						idler = null;
					} catch (Exception ex) {
						logger.error(ex + "\n" + sb.toString());
					}

				}

			}
			if (bolumler.size() > 1)
				bolumler = PdksUtil.sortObjectStringAlanList(bolumler, "getAciklama", null);
			bolumDepartmanlari = ortakIslemler.getTanimSelectItem("bolumDepartman", bolumler);
		}
		bolumler = null;
		return bolumDepartmanlari;
	}

	/**
	 * @param anchor
	 * @param helper
	 * @param drawing
	 * @param personelDenklestirme
	 * @return
	 */
	public Comment getCommentGuncelleyen(ClientAnchor anchor, CreationHelper helper, Drawing drawing, PersonelDenklestirme personelDenklestirme) {
		Comment commentGuncelleyen = null;
		if (personelDenklestirme.isOnaylandi() && personelDenklestirme.getGuncelleyenUser() != null) {
			User loginUser = authenticatedUser != null ? authenticatedUser : new User();
			String title = "Onaylayan : " + personelDenklestirme.getGuncelleyenUser().getAdSoyad() + "\n";
			title += "Zaman : " + loginUser.dateTimeFormatla(personelDenklestirme.getGuncellemeTarihi());
			commentGuncelleyen = ExcelUtil.getComment(anchor, helper, drawing, title);
		}

		return commentGuncelleyen;
	}

	/**
	 * @param aylikPuantaj
	 * @param gunData
	 * @return
	 */
	public String getVardiyaGun(AylikPuantaj aylikPuantaj, VardiyaGun gunData) {
		VardiyaGun gun = null;
		try {
			if (gunData != null && aylikPuantaj != null)
				gun = aylikPuantaj.getVardiyaGun(gunData);

		} catch (Exception e) {
			gun = null;
		}

		return gun != null ? "" : null;
	}

	/**
	 * @param map
	 * @param per
	 * @param vardiya
	 * @return
	 */
	public Integer getVardiyaAdet(TreeMap<String, TreeMap<String, List<VardiyaGun>>> map, Personel per, Vardiya vardiya) {
		Integer adet = null;
		if (vardiya != null && per != null && map != null) {
			if (per.getId() != null && map.containsKey(per.getId())) {
				TreeMap<String, List<VardiyaGun>> perMap = map.get(per.getId());
				if (vardiya.getKisaAdi() != null && perMap.containsKey(vardiya.getKisaAdi()))
					adet = perMap.get(vardiya.getKisaAdi()).size();
			}
		}
		return adet;
	}

	/**
	 * @param puantajList
	 */
	public void sortAylikPuantajPersonelBolum(List<AylikPuantaj> puantajList) {
		if (puantajList != null && !puantajList.isEmpty()) {
			List<AylikPuantaj> puantajYeniList = new ArrayList<AylikPuantaj>();
			TreeMap<Long, List<AylikPuantaj>> map = new TreeMap<Long, List<AylikPuantaj>>();
			TreeMap<Long, Tanim> tanimMap = new TreeMap<Long, Tanim>();
			for (AylikPuantaj aylikPuantaj : puantajList) {
				Personel personel = aylikPuantaj.getPdksPersonel();
				if (personel.getEkSaha3() != null) {
					Tanim tanim = personel.getEkSaha3();
					List<AylikPuantaj> list = map.containsKey(tanim.getId()) ? map.get(tanim.getId()) : new ArrayList<AylikPuantaj>();
					if (list.isEmpty()) {
						tanimMap.put(tanim.getId(), tanim);
						map.put(tanim.getId(), list);
					}
					list.add(aylikPuantaj);
				} else
					puantajYeniList.add(aylikPuantaj);
			}
			if (!tanimMap.isEmpty()) {
				List<Tanim> list = PdksUtil.sortObjectStringAlanList(new ArrayList(tanimMap.values()), "getAciklama", null);
				for (Tanim tanim : list) {
					puantajYeniList.addAll(map.get(tanim.getId()));
				}
			}
			puantajList.clear();
			puantajList.addAll(puantajYeniList);
			puantajYeniList = null;
			tanimMap = null;
			map = null;
		}

	}

	/**
	 * @param loginUser
	 * @param vgList
	 * @param puantajList
	 * @param izinGoster
	 * @return
	 */
	public TreeMap<String, Object> getIzinOzetMap(User loginUser, List<VardiyaGun> vgList, List<AylikPuantaj> puantajList, boolean izinGoster) {
		TreeMap<String, Object> map = new TreeMap<String, Object>();
		try {
			if (puantajList != null) {
				if (vgList == null)
					vgList = new ArrayList<VardiyaGun>();
				for (AylikPuantaj aylikPuantaj : puantajList) {
					for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
						Vardiya vardiya = vg.getVardiya();
						if (vg.isAyinGunu() && vardiya != null && vardiya.getId() != null && vg.isIzinli()) {
							boolean ekle = vardiya.isGecerliIzin() || (vg.getIzin() != null && !(vardiya.isHaftaTatil() || vardiya.isOff()));
							if (!ekle && vg.getIzin() != null) {
								IzinTipi izinTipi = vg.getIzin().getIzinTipi();
								ekle = izinTipi.isTakvimGunuMu();
								if (!ekle) {
									ekle = true;
									if (vardiya.isOff())
										ekle = izinTipi.isOffDahilMi();
									else if (vardiya.isHaftaTatil())
										ekle = izinTipi.isHTDahil();

								}
							}
							if (ekle) {
								vgList.add(vg);
							}

						}

					}
				}
			}
			String key = ortakIslemler.getParameterKey("izinPersonelOzetGoster");

			boolean devam = (loginUser.isAdmin() && izinGoster) || key.equals("1") || (loginUser.isIK() && PdksUtil.hasStringValue(key)), htToplamiGuncelle = false;
			if (vgList != null && (loginUser.isAdmin() || loginUser.isSistemYoneticisi() || devam)) {
				TreeMap<String, Vardiya> izinTipiVardiyaMap = new TreeMap<String, Vardiya>();
				TreeMap<Long, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap = new TreeMap<Long, TreeMap<String, List<VardiyaGun>>>();
				TreeMap<Long, Personel> izinTipiPersonelMap = new TreeMap<Long, Personel>();
				TreeMap<String, VardiyaGun> htMap = new TreeMap<String, VardiyaGun>();
				TreeMap<String, String> izinMap = new TreeMap<String, String>();
				for (VardiyaGun vg : vgList) {
					if (vg.getVardiya() != null && vg.getVardiya().getId() != null) {
						Vardiya vardiyaIzin = null;
						String dateStr = vg.getVardiyaKeyStr();
						if (vg.getIzin() == null && vg.getVardiya().isHaftaTatil()) {
							if (vg.getVardiyaSaat() == null || vg.getVardiyaSaat().getNormalSure() == 0.0d)
								htMap.put(dateStr, vg);
							continue;
						}
						if (vg.getIzin() != null) {
							IzinTipi izinTipi = vg.getIzin().getIzinTipi();
							if (!izinTipi.isTakvimGunuMu()) {
								Vardiya vardiya = vg.getVardiya();
								boolean hataliDurum = (vardiya.isHaftaTatil() && izinTipi.isHTDahil() == false) || (vardiya.isOffGun() && izinTipi.isOffDahilMi() == false);
								if (hataliDurum)
									continue;
							}
							vardiyaIzin = izinTipi.getIzinVardiya(izinTipi.isRaporIzin() == false ? Vardiya.TIPI_IZIN : Vardiya.TIPI_HASTALIK_RAPOR);
						} else if (vg.getVardiya().isGecerliIzin()) {
							htToplamiGuncelle = true;
							vardiyaIzin = vg.getVardiya();
						}

						else
							continue;
						String izinKodu = vardiyaIzin != null ? vardiyaIzin.getKisaAdi() : null;
						if (izinKodu == null)
							continue;
						if (vardiyaIzin.isHastalikRapor())
							izinMap.put(dateStr, izinKodu);
						if (vg.isAyinGunu() == false)
							continue;
						Long perNo = vg.getPersonel().getId();
						if (!izinTipiPersonelMap.containsKey(perNo))
							izinTipiPersonelMap.put(perNo, vg.getPersonel());
						if (!izinTipiVardiyaMap.containsKey(izinKodu))
							izinTipiVardiyaMap.put(izinKodu, vardiyaIzin);

						setVardiyaIzinListesi(vg, izinKodu, izinTipiPersonelVardiyaMap);

					}
				}
				if (htToplamiGuncelle) {
					for (String dateStr : htMap.keySet()) {
						VardiyaGun vg = htMap.get(dateStr);
						if (vg.getIzin() != null)
							continue;
						String oncekiStr = vg.getOncekiVardiya() != null ? vg.getOncekiVardiyaGun().getVardiyaKeyStr() : "";
						String sonrakiStr = vg.getSonrakiVardiyaGun() != null ? vg.getSonrakiVardiyaGun().getVardiyaKeyStr() : "";
						if ((izinMap.containsKey(sonrakiStr) && izinMap.containsKey(oncekiStr)) && !izinMap.containsKey(dateStr)) {
							String izinKoduOnceki = izinMap.get(oncekiStr), izinKoduSonraki = izinMap.get(sonrakiStr);
							logger.debug(dateStr + " " + izinKoduOnceki);
							if (izinKoduSonraki.equals(izinKoduOnceki))
								setVardiyaIzinListesi(vg, izinKoduOnceki, izinTipiPersonelVardiyaMap);
						}
					}
				}
				if (!izinTipiVardiyaMap.isEmpty()) {
					map.put("izinTipiVardiyaList", PdksUtil.sortListByAlanAdi(new ArrayList(izinTipiVardiyaMap.values()), "ekranSira", false));
					map.put("izinTipiPersonelMap", izinTipiPersonelMap);
					map.put("izinTipiPersonelVardiyaMap", izinTipiPersonelVardiyaMap);
				}

			}
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		return map;
	}

	/**
	 * @param vg
	 * @param izinKodu
	 * @param izinTipiPersonelVardiyaMap
	 */
	private void setVardiyaIzinListesi(VardiyaGun vg, String izinKodu, TreeMap<Long, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap) {
		Long perNo = vg.getPersonel().getId();
		TreeMap<String, List<VardiyaGun>> perMap = izinTipiPersonelVardiyaMap.containsKey(perNo) ? izinTipiPersonelVardiyaMap.get(perNo) : new TreeMap<String, List<VardiyaGun>>();
		if (perMap.isEmpty())
			izinTipiPersonelVardiyaMap.put(perNo, perMap);
		List<VardiyaGun> perVardiyaGunList = perMap.containsKey(izinKodu) ? perMap.get(izinKodu) : new ArrayList<VardiyaGun>();
		if (perVardiyaGunList.isEmpty())
			perMap.put(izinKodu, perVardiyaGunList);
		perVardiyaGunList.add(vg);
	}
}
