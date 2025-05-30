package org.pdks.security.action;

import java.io.File;
import java.io.Serializable;
import java.sql.Clob;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.security.Identity;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.pdks.dinamikRapor.entity.PdksDinamikRaporAlan;
import org.pdks.dinamikRapor.entity.PdksDinamikRaporParametre;
import org.pdks.dinamikRapor.entity.PdksDinamikRaporRole;
import org.pdks.entity.AccountPermission;
import org.pdks.entity.ArifeVardiyaDonem;
import org.pdks.entity.AylikPuantaj;
import org.pdks.entity.CalismaModeliGun;
import org.pdks.entity.CalismaModeliVardiya;
import org.pdks.entity.DepartmanMailGrubu;
import org.pdks.entity.IzinHakedisHakki;
import org.pdks.entity.IzinTipiBirlesikHaric;
import org.pdks.entity.IzinTipiMailAdres;
import org.pdks.entity.KatSayi;
import org.pdks.entity.LDAPDomain;
import org.pdks.entity.MailUser;
import org.pdks.entity.MenuIliski;
import org.pdks.entity.MenuItem;
import org.pdks.entity.Notice;
import org.pdks.entity.Parameter;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDenklestirme;
import org.pdks.entity.PersonelDinamikAlan;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelIzinDetay;
import org.pdks.entity.ServiceData;
import org.pdks.entity.Sirket;
import org.pdks.entity.SkinBean;
import org.pdks.entity.Tanim;
import org.pdks.entity.Tatil;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaGorev;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.VardiyaHafta;
import org.pdks.entity.VardiyaYemekIzin;
import org.pdks.entity.YemekKartsiz;
import org.pdks.enums.NoteTipi;
import org.pdks.erp.action.SapRfcManager;
import org.pdks.erp.entity.SAPSunucu;
import org.pdks.security.entity.KullaniciSession;
import org.pdks.security.entity.MenuItemConstant;
import org.pdks.security.entity.User;
import org.pdks.security.entity.UserDigerOrganizasyon;
import org.pdks.security.entity.UserRoles;
import org.pdks.session.ExcelUtil;
import org.pdks.session.LDAPUserManager;
import org.pdks.session.OrtakIslemler;
import org.pdks.session.PdksEntityController;
import org.pdks.session.PdksUtil;
import org.pdks.session.SSLImport;
import org.pdks.system.filter.RequestEncodingFilter;

import com.google.gson.Gson;
import com.pdks.mail.model.MailManager;

@Startup(depends = { "pdksEntityController" })
@Scope(ScopeType.APPLICATION)
@Name("startupAction")
public class StartupAction implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -6954515802074767473L;

	private static final String HELP_DESK_STATUS = "helpDeskStatus";
	static Logger logger = Logger.getLogger(StartupAction.class);

	@In(required = false)
	EntityManager entityManager;

	@In(required = false, create = true)
	UserHome userHome;

	@In(create = true)
	MenuItemConstant menuItemConstant;

	@In(required = false)
	Identity identity;

	@In(create = true)
	PdksEntityController pdksEntityController;

	@Out(scope = ScopeType.APPLICATION, required = false)
	Notice homePageNotice = new Notice();

	@Out(scope = ScopeType.APPLICATION, required = false)
	HashMap<String, MenuItem> menuItemMap = new HashMap<String, MenuItem>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	HashMap<String, Long> raporRoleMap = new HashMap<String, Long>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	List<Sirket> pdksSirketleri = new ArrayList<Sirket>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	Map<String, AccountPermission> accountPermissionMap = new HashMap<String, AccountPermission>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	List<MenuItem> menuItemList = new ArrayList<MenuItem>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	Map<String, MenuItem> topActiveMenuItemMap = new HashMap<String, MenuItem>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	HashMap<String, String> parameterMap = new HashMap<String, String>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	List<String> izinliCalisilanGunler = new ArrayList<String>();

	@Out(scope = ScopeType.APPLICATION, required = false)
	private String smtpHost, smtpUserName, smtpPassword, smtpSSLDurum, smtpTLSDurum;

	@Out(scope = ScopeType.APPLICATION, required = false)
	public byte[] projeHeaderImage;

	@Out(scope = ScopeType.APPLICATION, required = false)
	public byte[] projeFooterImage;

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeHeaderColor = "white";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeHeaderBackgroundColor = "blue";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeHeaderImageWidth = "480";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projePowerBy = "Hasan Sayar";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeFooterBackgroundColor = "white";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projePowerURL;

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeURL;

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeHeaderImageHeight = "83";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeFooterImageWidth = "480";

	@Out(scope = ScopeType.APPLICATION, required = false)
	public String projeFooterImageHeight = "83";

	private int smtpHostPort;

	public SkinBean skinBean = new SkinBean();

	public Notice getHomePageNotice() {
		return homePageNotice;
	}

	public void addIzinliCalisilanGunlerList(String str) {
		if (!izinliCalisilanGunler.contains(str))
			izinliCalisilanGunler.add(str);
	}

	public void setHomePageNotice(Notice homePageNotice) {
		this.homePageNotice = homePageNotice;
	}

	public Map<String, MenuItem> getTopActiveMenuItemMap() {
		return topActiveMenuItemMap;
	}

	public void setTopActiveMenuItemMap(Map<String, MenuItem> topActiveMenuItemMap) {
		this.topActiveMenuItemMap = topActiveMenuItemMap;
	}

	public List<MenuItem> getMenuItemList() {
		return menuItemList;
	}

	public void setMenuItemList(List<MenuItem> menuItemList) {
		this.menuItemList = menuItemList;
	}

	public void fillMenuItemList(Session session) {
		if (session == null)
			session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		fillAccountPermission(session, null);

		List<MenuItem> menuItemList = pdksEntityController.getSQLParamByFieldList(MenuItem.TABLE_NAME, null, null, MenuItem.class, session);
		if (menuItemList.size() > 1)
			menuItemList = PdksUtil.sortListByAlanAdi(menuItemList, "orderNo", false);
		HashMap<String, MenuItem> menuItemMapYeni = new HashMap<String, MenuItem>();
		for (MenuItem menuItem : menuItemList)
			menuItemMapYeni.put(menuItem.getName(), menuItem);

		setMenuItemMap(menuItemMapYeni);
		setMenuItemList(menuItemList);
		try {
			List<MenuItem> allTreeMenuItemList = pdksEntityController.getSQLParamByAktifFieldList(MenuItem.TABLE_NAME, MenuItem.COLUMN_NAME_TOP_MENU, Boolean.TRUE, MenuItem.class, session);
			if (allTreeMenuItemList.size() > 1)
				allTreeMenuItemList = PdksUtil.sortListByAlanAdi(allTreeMenuItemList, "orderNo", false);
			for (MenuItem menuItem : allTreeMenuItemList)
				topActiveMenuItemMap.put(menuItem.getName(), menuItem);

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			throw new FacesException(e.getMessage(), e);
		}
	}

	@Create
	public void startupMethodBasla() {
		// pdksUtil.setTimeZome();
		Session session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		startupMethod(session);
	}

	/**
	 * @param session
	 */
	@Transactional
	public void savePrepareAllTableID(Session session) {
		List<Class> list = new ArrayList<Class>();
		long toplamAdet = 0L;
		try {
			list.add(AccountPermission.class);
			list.add(ArifeVardiyaDonem.class);
			list.add(CalismaModeliGun.class);
			list.add(CalismaModeliVardiya.class);
			list.add(DepartmanMailGrubu.class);
			list.add(PdksDinamikRaporAlan.class);
			list.add(PdksDinamikRaporParametre.class);
			list.add(PdksDinamikRaporRole.class);
			list.add(IzinHakedisHakki.class);
			list.add(IzinTipiBirlesikHaric.class);
			list.add(IzinTipiMailAdres.class);
			list.add(KatSayi.class);
			list.add(MailUser.class);
			list.add(MenuIliski.class);
			list.add(Notice.class);
			list.add(Parameter.class);
			list.add(PersonelDinamikAlan.class);
			list.add(SAPSunucu.class);
			list.add(ServiceData.class);
			list.add(Tatil.class);
			list.add(UserDigerOrganizasyon.class);
			list.add(UserRoles.class);
			list.add(VardiyaGorev.class);
			list.add(VardiyaHafta.class);
			list.add(VardiyaYemekIzin.class);
			list.add(YemekKartsiz.class);
 			for (Class class1 : list) {
				long adet = pdksEntityController.savePrepareTableID(false, class1, entityManager, session);
				toplamAdet += adet;
				if (adet > 0)
					session.flush();
			}
		} catch (Exception e) {
			logger.error(e);
		}
		if (toplamAdet > 0)
			logger.info(toplamAdet + " adet kayıt id güncellendi.");

		list = null;
	}

	/**
	 * @param session
	 */
	public void startupMethod(Session session) {
		Calendar cal = Calendar.getInstance();
		logger.debug("startupMethod : " + cal.getTime());
		if (cal.get(Calendar.HOUR_OF_DAY) < 7)
			savePrepareAllTableID(session);
		viewRefresh(session);
		fillStartMethod(null, false, session);
	}

	/**
	 * @param session
	 */
	public void viewRefresh(Session session) {
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		try {
			veriMap.put("ekranYaz", 0);
			veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List hatalar = pdksEntityController.execSPList(veriMap, new StringBuffer("SP_PDKS_VIEW_REFRESH"), null);
			if (hatalar != null && !hatalar.isEmpty()) {
				Clob clobComment = (Clob) hatalar.get(0);
				String aciklama = PdksUtil.replaceAllManuel(PdksUtil.StringToByClob(clobComment), "|", "\n");
				if (PdksUtil.hasStringValue(aciklama))
					throw new Exception("\n" + aciklama);
			}

		} catch (Exception e) {
			logger.error(e);
		}
		veriMap = null;
	}

	/**
	 * @param user
	 * @param lockVar
	 * @param session
	 */
	public void fillStartMethod(User user, boolean lockVar, Session session) {
		if (session == null) {
			session = user != null ? user.getSessionSQL() : null;
			if (session == null)
				session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		}
		logger.info("Sistem verileri yukleniyor in " + PdksUtil.getCurrentTimeStampStr());
		fillParameter(session, lockVar);
		try {
			List<Notice> noticeList = pdksEntityController.getSQLParamByAktifFieldList(Notice.TABLE_NAME, Notice.COLUMN_NAME_ADI, NoteTipi.ANA_SAYFA.value(), Notice.class, session);
			Notice notice = null;
			if (noticeList != null && !noticeList.isEmpty()) {
				if (noticeList.size() == 1)
					notice = (Notice) noticeList.get(0).clone();
				else {
					notice = new Notice();
					StringBuffer aciklama = new StringBuffer();
					for (Iterator iterator = noticeList.iterator(); iterator.hasNext();) {
						Notice nt = (Notice) iterator.next();
						if (aciklama.length() > 0)
							aciklama.append("<br/>");
						if (nt.getValue() != null)
							aciklama.append(nt.getValue().trim());

					}
					notice.setValue(aciklama.toString());
					aciklama = null;
				}
			}
			noticeList = null;
			if (notice != null) {
				if (PdksUtil.hasStringValue(notice.getValue()) == false)
					notice = null;
			}

			setHomePageNotice(notice);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			logger.error(e);
		}

		List<SAPSunucu> sapSunucular = pdksEntityController.getSQLParamByFieldList(SAPSunucu.TABLE_NAME, SAPSunucu.COLUMN_NAME_DURUM, Boolean.TRUE, SAPSunucu.class, session);
		SapRfcManager.setSapSunucular(sapSunucular);

		fillAccountPermission(session, user);

		// Menu listesisini dolduruyoruz
		fillMenuItemList(session);
		logger.info("Sistem verileri yukleniyor out " + PdksUtil.getCurrentTimeStampStr());

	}

	/**
	 * @param session
	 */
	public void fillSirketList(Session session) {
		pdksSirketleri.clear();

		List<Sirket> list = null;
		try {

			list = pdksEntityController.getSQLParamByFieldList(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_DURUM, Boolean.TRUE, Sirket.class, session);

		} catch (Exception e) {
			logger.error("PDKS hata out : " + e.getMessage());
			list = new ArrayList<Sirket>();
		}
		if (list != null)
			pdksSirketleri.addAll(list);
		list = null;

	}

	/**
	 * @param session
	 */
	public void fillRaporRole(Session session) {
		raporRoleMap.clear();
		HashMap fields = new HashMap();
		List<PdksDinamikRaporRole> list = null;
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("select * from " + PdksDinamikRaporRole.TABLE_NAME + " " + PdksEntityController.getSelectLOCK());
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			list = pdksEntityController.getObjectBySQLList(sb, fields, PdksDinamikRaporRole.class);
			for (PdksDinamikRaporRole raporRole : list) {
				raporRoleMap.put(raporRole.getKey(), raporRole.getId());
			}
			list = null;
		} catch (Exception e) {
		}
	}

	public void fillParameter(Session session, boolean lockVar) {
		HashMap fields = new HashMap();
		List<Parameter> parameterList = null;
		try {
			StringBuffer sb = new StringBuffer();
			sb.append("select * from " + Parameter.TABLE_NAME + (lockVar ? " " + PdksEntityController.getSelectLOCK() : ""));
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			parameterList = pdksEntityController.getObjectBySQLList(sb, fields, Parameter.class);
		} catch (Exception e) {
			try {
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				parameterList = pdksEntityController.getObjectByInnerObjectList(fields, Parameter.class);
			} catch (Exception e2) {
				logger.error("PDKS hata out : " + e2.getMessage());
				e2.printStackTrace();
			}
		}
		parameterMap.clear();
		List<String> helpDeskList = new ArrayList<String>();
		HashMap<String, Parameter> pmMap = new HashMap<String, Parameter>();
		for (Parameter parameter : parameterList) {
			String key = parameter.getName().trim(), deger = parameter.getValue().trim();
			pmMap.put(key, parameter);
			if (parameter != null && parameter.getActive()) {
				parameterMap.put(key, deger);
				if (parameter.isHelpDeskMi())
					helpDeskList.add(key);
			}

		}
		String dateFormat = null;
		if (parameterMap.containsKey("dateFormat")) {
			String str = null;
			try {
				str = PdksUtil.convertToDateString(new Date(), parameterMap.get("dateFormat"));
				if (str != null && str.length() > 1)
					dateFormat = parameterMap.get("dateFormat");
			} catch (Exception e) {
			}
		}
		if (dateFormat == null)
			dateFormat = "dd/MM/yyyy";
		PdksUtil.setDateFormat(dateFormat);
		try {
			String gebelikGuncelle = "";
			if (parameterMap.containsKey("gebelikGuncelle"))
				gebelikGuncelle = parameterMap.get("gebelikGuncelle");
			AylikPuantaj.setGebelikGuncelle(gebelikGuncelle.equals("1"));
			String testSunucu = "srvglftest";
			if (parameterMap.containsKey("testSunucu"))
				testSunucu = parameterMap.get("testSunucu");
			PdksUtil.setTestSunucu(testSunucu);
			String canliSunucu = "srvglf";
			if (parameterMap.containsKey("canliSunucu"))
				canliSunucu = parameterMap.get("canliSunucu");
			PdksUtil.setCanliSunucu(canliSunucu);
			SSLImport.setServisURLList(null);
			if (parameterMap.containsKey("sslMail")) {
				String sslMail = parameterMap.get("sslMail");
				if (!sslMail.startsWith("https://"))
					sslMail = "https://" + sslMail;
				SSLImport.getCertificateInputStream(sslMail);
			}
			servisDurumKontrol(session, helpDeskList, pmMap);
			SSLImport.addCertToKeyStore(null, null, true);
		} catch (Exception e) {
		}

		if (parameterMap.containsKey("skin"))
			SkinBean.setSkinAdi(parameterMap.get("skin"));
		Integer yarimYuvarlaLast = null;
		if (parameterMap.containsKey("yarimYuvarlaLast")) {
			try {
				yarimYuvarlaLast = Integer.parseInt(parameterMap.get("yarimYuvarlaLast"));
			} catch (Exception e) {
				yarimYuvarlaLast = null;
			}
		}
		if (yarimYuvarlaLast == null || yarimYuvarlaLast < 1)
			yarimYuvarlaLast = 1;
		PdksUtil.setYarimYuvarlaLast(yarimYuvarlaLast);
		projeHeaderImage = null;
		if (parameterMap.containsKey("projeHeaderImageName")) {
			String projeHeaderImageName = parameterMap.get("projeHeaderImageName");
			File projeHeader = new File("/opt/pdks/" + projeHeaderImageName);
			if (projeHeader.exists()) {
				try {
					projeHeaderImage = PdksUtil.getFileByteArray(projeHeader);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		setExcelColor();
		if (parameterMap.containsKey("oddTableRenk")) {
			String deger = parameterMap.get("oddTableRenk");
			if (deger.length() > 3)
				MailManager.setOddRenk(deger);
		}
		if (parameterMap.containsKey("evenTableRenk")) {
			String deger = parameterMap.get("evenTableRenk");
			if (deger.length() > 3)
				MailManager.setEvenRenk(deger);
		}
		if (parameterMap.containsKey("headerTableRenk")) {
			String deger = (String) parameterMap.get("headerTableRenk");
			if (deger.length() > 3)
				MailManager.setHeaderRenk(deger);
		}
		String fontSize = "22px";
		boolean izinHakedisGuncelle = false;
		if (parameterMap.containsKey("izinHakedisGuncelle"))
			izinHakedisGuncelle = parameterMap.get("izinHakedisGuncelle").equals("1");
		PersonelIzinDetay.setIzinHakedisGuncelle(izinHakedisGuncelle);
		projeURL = null;
		if (parameterMap.containsKey("projeURL")) {
			projeURL = parameterMap.get("projeURL");
		}
		projePowerURL = null;
		projeFooterBackgroundColor = "white";
		if (parameterMap.containsKey("projePowerBy")) {
			projePowerBy = parameterMap.get("projePowerBy");
			if (parameterMap.containsKey("projePowerURL"))
				projePowerURL = parameterMap.get("projePowerURL");
			if (parameterMap.containsKey("projeFooterBackgroundColor"))
				projeFooterBackgroundColor = parameterMap.get("projeFooterBackgroundColor");

		}
		if (parameterMap.containsKey("projeHeaderRenk")) {
			String deger = parameterMap.get("projeHeaderRenk");
			LinkedHashMap<String, String> map = PdksUtil.parametreAyikla(deger);
			if (map.containsKey("background-color"))
				projeHeaderBackgroundColor = map.get("background-color");
			if (map.containsKey("color"))
				projeHeaderColor = map.get("color");
			if (map.containsKey("font-size"))
				fontSize = map.get("font-size");

		}
		projeHeaderColor += ";font-size:" + fontSize + ";";
		if (parameterMap.containsKey("vardiyaKontrolTarih")) {
			String dateStr = parameterMap.get("vardiyaKontrolTarih");
			Date vardiyaKontrolTarih = null;
			try {
				vardiyaKontrolTarih = PdksUtil.convertToJavaDate(dateStr, "yyyy-MM-dd");
			} catch (Exception e) {
				vardiyaKontrolTarih = null;
			}
			Vardiya.setVardiyaKontrolTarih(vardiyaKontrolTarih);
		}
		Vardiya.setVardiyaAySonuKontrolTarih(null);
		String grubaGirisTarihiAlanAdi = null;
		if (parameterMap.containsKey("grubaGirisTarihiAlanAdi"))
			grubaGirisTarihiAlanAdi = parameterMap.get("grubaGirisTarihiAlanAdi");
		if (grubaGirisTarihiAlanAdi == null || !(grubaGirisTarihiAlanAdi.trim().equalsIgnoreCase(Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI) || grubaGirisTarihiAlanAdi.trim().equalsIgnoreCase(Personel.COLUMN_NAME_GRUBA_GIRIS_TARIHI)))
			grubaGirisTarihiAlanAdi = Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI;
		Personel.setGrubaGirisTarihiAlanAdi(grubaGirisTarihiAlanAdi);
		if (parameterMap.containsKey("vardiyaAySonuKontrolTarih")) {
			String dateStr = parameterMap.get("vardiyaAySonuKontrolTarih");
			Date vardiyaAySonuKontrolTarih = null;
			try {
				vardiyaAySonuKontrolTarih = PdksUtil.convertToJavaDate(dateStr, "yyyy-MM-dd");
			} catch (Exception e) {
				vardiyaAySonuKontrolTarih = null;
			}
			Vardiya.setVardiyaAySonuKontrolTarih(vardiyaAySonuKontrolTarih);
		}
		boolean puantajSorguAltBolumGir = false;

		if (parameterMap.containsKey("puantajSorguAltBolumGir")) {
			String puantajSorguAltBolumGirStr = parameterMap.get("puantajSorguAltBolumGir");
			puantajSorguAltBolumGir = puantajSorguAltBolumGirStr != null && puantajSorguAltBolumGirStr.equals("1");
		}

		PdksUtil.setPuantajSorguAltBolumGir(puantajSorguAltBolumGir);

		boolean menuKapali = false;
		if (parameterMap.containsKey("menuKapali")) {
			String menuKapaliStr = parameterMap.get("menuKapali");
			menuKapali = PdksUtil.hasStringValue(menuKapaliStr);

		}
		UserHome.setMenuKapali(menuKapali);
		if (parameterMap.containsKey("vardiyaKontrolTarih3")) {
			String dateStr = parameterMap.get("vardiyaKontrolTarih3");
			Date vardiyaKontrolTarih3 = null;
			try {
				vardiyaKontrolTarih3 = PdksUtil.convertToJavaDate(dateStr, "yyyy-MM-dd");
			} catch (Exception e) {
				vardiyaKontrolTarih3 = null;
			}
			Vardiya.setVardiyaKontrolTarih3(vardiyaKontrolTarih3);

		}
		if (parameterMap.containsKey("vardiyaKontrolTarih2")) {
			String dateStr = parameterMap.get("vardiyaKontrolTarih2");
			Date vardiyaKontrolTarih2 = null;
			try {
				vardiyaKontrolTarih2 = PdksUtil.convertToJavaDate(dateStr, "yyyy-MM-dd");
			} catch (Exception e) {
				vardiyaKontrolTarih2 = null;
			}
			Vardiya.setVardiyaKontrolTarih2(vardiyaKontrolTarih2);
			int fazlaMesaiBasSaati = 2;
			if (parameterMap.containsKey("fazlaMesaiBasSaati")) {
				String str = parameterMap.get("fazlaMesaiBasSaati");
				Integer value = null;
				try {
					value = Integer.parseInt(str);
				} catch (Exception e) {
					value = null;
				}
				if (value == null || value < 0)
					value = 2;
				fazlaMesaiBasSaati = value;
			}
			Vardiya.setFazlaMesaiBasSaati(fazlaMesaiBasSaati);

		}
		int offFazlaMesaiBasDakika = -60, haftaTatiliFazlaMesaiBasDakika = -60;
		if (parameterMap.containsKey("offFazlaMesaiBasDakika")) {
			String str = parameterMap.get("offFazlaMesaiBasDakika");
			Integer value = null;
			try {
				value = Integer.parseInt(str);
			} catch (Exception e) {
				value = null;
			}
			if (value == null)
				value = -60;
			offFazlaMesaiBasDakika = value;
		}
		Vardiya.setIntOffFazlaMesaiBasDakika(offFazlaMesaiBasDakika);
		if (parameterMap.containsKey("haftaTatiliFazlaMesaiBasDakika")) {
			String str = parameterMap.get("haftaTatiliFazlaMesaiBasDakika");
			Integer value = null;
			try {
				value = Integer.parseInt(str);
			} catch (Exception e) {
				value = null;
			}
			if (value == null)
				value = -60;
			haftaTatiliFazlaMesaiBasDakika = value;
		}
		Vardiya.setIntHaftaTatiliFazlaMesaiBasDakika(haftaTatiliFazlaMesaiBasDakika);
		projeFooterImage = null;
		if (parameterMap.containsKey("projeFooterImageName")) {
			String imageName = parameterMap.get("projeFooterImageName");
			File projeImage = new File("/opt/pdks/" + imageName);
			if (projeImage.exists()) {
				try {
					projeFooterImage = PdksUtil.getFileByteArray(projeImage);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			if (parameterMap.containsKey("projeFooterImageSize")) {
				String deger = parameterMap.get("projeFooterImageSize");
				LinkedHashMap<String, String> map = PdksUtil.parametreAyikla(deger);
				if (map.containsKey("width"))
					projeFooterImageWidth = map.get("width");
				if (map.containsKey("height"))
					projeFooterImageHeight = map.get("height");
			}
		}

		if (parameterMap.containsKey("projeHeaderSize")) {
			String deger = parameterMap.get("projeHeaderSize");
			LinkedHashMap<String, String> map = PdksUtil.parametreAyikla(deger);
			if (map.containsKey("width"))
				projeHeaderImageWidth = map.get("width");
			if (map.containsKey("height"))
				projeHeaderImageHeight = map.get("height");
		}
		int sureYuvarlama = -1;
		if (parameterMap.containsKey("sureYuvarlamaKatsayi")) {
			String deger = parameterMap.get("sureYuvarlamaKatsayi");
			try {
				sureYuvarlama = Integer.parseInt(deger);
			} catch (Exception e) {
				sureYuvarlama = 0;
			}
		}
		if (sureYuvarlama <= 0)
			sureYuvarlama = 40;
		PdksUtil.setMESAI_YUVARLAMA_KATSAYI(sureYuvarlama);

		double eksikCalismaUyariYuzdesi = -1;
		if (parameterMap.containsKey("eksikCalismaUyariYuzdesi")) {
			String deger = parameterMap.get("eksikCalismaUyariYuzdesi");
			try {
				eksikCalismaUyariYuzdesi = Double.parseDouble(deger) / 100.0d;
			} catch (Exception e) {
				sureYuvarlama = -1;
			}
		}
		if (eksikCalismaUyariYuzdesi < 0.0d)
			eksikCalismaUyariYuzdesi = 0.85;
		else if (eksikCalismaUyariYuzdesi > 1.0d)
			eksikCalismaUyariYuzdesi = 1.00;
		AylikPuantaj.setEksikCalismaUyariYuzdesi(eksikCalismaUyariYuzdesi);

		SapRfcManager.setParameterMap(parameterMap);
		Integer sicilNoUzunluk = null;
		if (!parameterList.isEmpty())
			try {
				// Setting up LDAP attributes..
				PdksEntityController.setReadUnCommitted(parameterMap.containsKey("readUnCommitted") && parameterMap.get("readUnCommitted").equals("1"));
				PdksEntityController.setShowSQL(parameterMap.containsKey("showSql") && parameterMap.get("showSql").equals("1"));
				VardiyaGun.setHaftaTatilDurum(parameterMap.containsKey("haftaTatilDurum") && parameterMap.get("haftaTatilDurum").equals("1"));
				if (parameterMap.containsKey("sicilNoUzunluk")) {
					try {
						sicilNoUzunluk = Integer.parseInt(parameterMap.get("sicilNoUzunluk"));
						if (sicilNoUzunluk < 1)
							sicilNoUzunluk = null;
					} catch (Exception e) {
						sicilNoUzunluk = null;
					}
				}

				setSmtpHost(parameterMap.get("smtpHost"));
				setSmtpHostPort(parameterMap.containsKey("smtpHostPort") ? Integer.parseInt(parameterMap.get("smtpHostPort")) : 0);
				setSmtpUserName(parameterMap.containsKey("smtpUserName") ? parameterMap.get("smtpUserName").trim() : null);
				setSmtpPassword(parameterMap.containsKey("smtpUserName") && parameterMap.containsKey("smtpPassword") ? parameterMap.get("smtpPassword").trim() : null);
				setSmtpSSLDurum(parameterMap.containsKey("smtpSSLDurum") && parameterMap.containsKey("smtpSSLDurum") ? parameterMap.get("smtpSSLDurum").trim() : null);
				setSmtpTLSDurum(parameterMap.containsKey("smtpTLSDurum") && parameterMap.containsKey("smtpTLSDurum") ? parameterMap.get("smtpTLSDurum").trim() : null);
				LDAPUserManager.setLdapHost(parameterMap.get("ldapHost"));//
				LDAPUserManager.setLdapPort(parameterMap.get("ldapPort"));// "389"
				LDAPUserManager.setLdapAdminUsername(parameterMap.get("ldapAdminUsername"));//
				LDAPUserManager.setLdapAdminPassword(parameterMap.get("ldapAdminPassword"));//
				LDAPUserManager.setLdapDC(parameterMap.get("ldapDC"));//
				PersonelIzin.setFazlaMesaiSure(parameterMap.containsKey("fazlaMesaiSure") ? Integer.parseInt(parameterMap.get("fazlaMesaiSure")) : -6);
				PersonelIzin.setYillikIzinMaxBakiye(parameterMap.containsKey("yillikIzinMaxBakiye") ? Integer.parseInt(parameterMap.get("yillikIzinMaxBakiye")) : -6);
				PersonelIzin.setSuaIzinMaxBakiye(parameterMap.containsKey("suaIzinMaxBakiye") ? Integer.parseInt(parameterMap.get("suaIzinMaxBakiye")) : -30);
				PdksUtil.setSistemBaslangicYili(parameterMap.containsKey("sistemBaslangicYili") ? Integer.parseInt(parameterMap.get("sistemBaslangicYili")) : 2010);
				PdksUtil.setIzinHaftaAdet(parameterMap.containsKey("izinHaftaAdet") ? Integer.parseInt(parameterMap.get("izinHaftaAdet")) : -1);
				PdksUtil.setIzinOffAdet(parameterMap.containsKey("izinOffAdet") ? Integer.parseInt(parameterMap.get("izinOffAdet")) : -1);
				PdksUtil.setPlanOffAdet(parameterMap.containsKey("planOffAdet") ? Integer.parseInt(parameterMap.get("planOffAdet")) : -1);
				PdksUtil.setOdenenFazlaMesaiSaati(parameterMap.containsKey("odenenFazlaMesaiSaati") ? Double.parseDouble(parameterMap.get("odenenFazlaMesaiSaati")) : 0d);
				PersonelDenklestirme.setCalismaSaatiSua(parameterMap.containsKey("calismaSaatiSua") ? Double.parseDouble(parameterMap.get("calismaSaatiSua")) : 7.0d);
				PersonelDenklestirme.setCalismaSaatiPartTime(parameterMap.containsKey("calismaSaatiPartTime") ? Double.parseDouble(parameterMap.get("calismaSaatiPartTime")) : 6.0d);
				PdksUtil.setUrl(parameterMap.containsKey("uygulamaURL") ? parameterMap.get("uygulamaURL") : null);
				RequestEncodingFilter.setIpControl(parameterMap.containsKey("ipControl") && parameterMap.get("ipControl").equals("1"));
				Tanim.setMultiLanguage(parameterMap.containsKey("multiLanguage"));
				setLDAPUserList(session);
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
			}
		PdksUtil.setSicilNoUzunluk(sicilNoUzunluk);
		if (parameterMap.containsKey("serverTimeUpdateFromDB")) {
			if (PdksUtil.getTestSunucuDurum() || PdksUtil.getCanliSunucuDurum()) {
				try {
					List<String> strList = PdksUtil.getListByString(parameterMap.get("serverTimeUpdateFromDB"), "|");
					StringBuffer sb = new StringBuffer();
					sb.append(strList.get(0));
					fields.clear();
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					List list = pdksEntityController.getObjectBySQLList(sb, fields, null);
					if (!list.isEmpty()) {
						Timestamp tarih = (Timestamp) list.get(0);
						String replace = PdksUtil.convertToDateString(new Date(tarih.getTime()), "yyyy-MM-dd HH:mm:ss");
						String cmd = strList.size() == 2 ? PdksUtil.replaceAllManuel(strList.get(1), "$tarih", replace) : "date -s '" + replace + "'";
						List<String> cmdList = PdksUtil.executeCommand(cmd, true);
						for (String string : cmdList) {
							logger.info(string);
						}
					}
					list = null;
					strList = null;
				} catch (Exception e) {
				}

			}
		}
		fillSirketList(session);
		setHelpDeskParametre(session, pmMap);
		fillRaporRole(session);
		pmMap = null;
	}

	/**
	 * @param session
	 * @param pmMap
	 */
	@Transactional
	private void setHelpDeskParametre(Session session, HashMap<String, Parameter> pmMap) {
		OrtakIslemler ortakIslemler = new OrtakIslemler();
		try {
			ortakIslemler.setInject(entityManager, pdksEntityController);
			Parameter helpDeskStatus = pmMap.containsKey(HELP_DESK_STATUS) ? pmMap.get(HELP_DESK_STATUS) : null;
			if (helpDeskStatus == null) {
				helpDeskStatus = ortakIslemler.getParameter(session, HELP_DESK_STATUS);
				if (helpDeskStatus == null) {
					helpDeskStatus = new Parameter();
					helpDeskStatus.setName(HELP_DESK_STATUS);
				}
			}
			Date bugun = new Date();
			if (helpDeskStatus.getId() == null) {
				Date changeDate = null;
				try {
					changeDate = PdksUtil.convertToJavaDate(PdksUtil.getSistemBaslangicYili() + "0101", "yyyyMMdd");
				} catch (Exception e) {
				}
				HashMap<String, String> map = null;
				if (pmMap != null) {
					map = new HashMap<String, String>();
					for (String key : pmMap.keySet()) {
						Parameter parameter = pmMap.get(key);
						map.put(key, parameter.getValue());
					}
				}
				if (helpDeskStatus.getId() == null)
					helpDeskStatus.setChangeDate(changeDate);
				if (helpDeskStatus.getChangeUser() == null)
					helpDeskStatus.setChangeUser(ortakIslemler.getSistemAdminUserByParamMap(map, pdksEntityController, session));
				helpDeskStatus.setVersion(0);
				helpDeskStatus.setDescription("Sistem Desktek Durumu");
				helpDeskStatus.setName(HELP_DESK_STATUS);
				helpDeskStatus.setGuncelle(false);
				helpDeskStatus.setHelpDesk(Boolean.TRUE);
			}

			Boolean durum = PdksUtil.isSistemDestekVar();
			boolean degisti = helpDeskStatus.getId() == null || !helpDeskStatus.getActive().equals(durum);
			helpDeskStatus.setActive(durum);
			helpDeskStatus.setValue(durum ? "1" : "" + (helpDeskStatus.getId() != null ? -helpDeskStatus.getId() : 0));
			if (degisti) {
				if (helpDeskStatus.getId() != null)
					helpDeskStatus.setChangeDate(bugun);
				session.saveOrUpdate(helpDeskStatus);
				session.flush();
			}

		} catch (Exception ex) {
			logger.error(ex);
			ex.printStackTrace();
		}
		ortakIslemler = null;
	}

	private void setExcelColor() {
		HashMap<String, HashMap<String, Integer[]>> colorMap = ExcelUtil.getColorMap();
		colorMap.clear();
		if (parameterMap.containsKey("excelHeaderBackgroundColor")) {
			String colors = parameterMap.get("excelHeaderBackgroundColor");
			setExcelColorValue(ExcelUtil.HEADER_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelHeaderColor")) {
			String colors = parameterMap.get("excelHeaderColor");
			setExcelColorValue(ExcelUtil.HEADER_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelRowBackgroundColor")) {
			String colors = parameterMap.get("excelRowBackgroundColor");
			setExcelColorValue(ExcelUtil.ROW_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelRowColor")) {
			String colors = parameterMap.get("excelRowColor");
			setExcelColorValue(ExcelUtil.ROW_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelOddBackgroundColor")) {
			String colors = parameterMap.get("excelOddBackgroundColor");
			setExcelColorValue(ExcelUtil.ODD_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelOddColor")) {
			String colors = parameterMap.get("excelOddColor");
			setExcelColorValue(ExcelUtil.ODD_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelEvenBackgroundColor")) {
			String colors = parameterMap.get("excelEvenBackgroundColor");
			setExcelColorValue(ExcelUtil.EVEN_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelEvenColor")) {
			String colors = parameterMap.get("excelEvenColor");
			setExcelColorValue(ExcelUtil.EVEN_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelOddDayBackgroundColor")) {
			String colors = parameterMap.get("excelOddDayBackgroundColor");
			setExcelColorValue(ExcelUtil.ODD_DAY_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelOddDayColor")) {
			String colors = parameterMap.get("excelOddDayColor");
			setExcelColorValue(ExcelUtil.ODD_DAY_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelEvenDayBackgroundColor")) {
			String colors = parameterMap.get("excelEvenDayBackgroundColor");
			setExcelColorValue(ExcelUtil.EVEN_DAY_COLOR, ExcelUtil.BACKGROUND_COLOR, colors, colorMap);
		}
		if (parameterMap.containsKey("excelEvenDayColor")) {
			String colors = parameterMap.get("excelEvenDayColor");
			setExcelColorValue(ExcelUtil.EVEN_DAY_COLOR, ExcelUtil.COLOR, colors, colorMap);
		}
	}

	/**
	 * @param type
	 * @param color
	 * @param excelColor
	 * @param colorMap
	 */
	private void setExcelColorValue(String type, String color, String excelColor, HashMap<String, HashMap<String, Integer[]>> colorMap) {
		List<String> colorList = PdksUtil.getListStringTokenizer(excelColor, null);
		if (colorList.size() == 3) {
			Integer[] colors = new Integer[3];
			for (int i = 0; i < colors.length; i++) {
				String str = colorList.get(i);
				try {
					Integer deger = Integer.parseInt(str);
					if (deger >= 0)
						colors[i] = Integer.parseInt(str);
					else {
						colors = null;
						break;
					}
				} catch (Exception e) {
					colors = null;
					break;
				}

			}
			if (colors != null) {
				HashMap<String, Integer[]> map = colorMap.containsKey(type) ? colorMap.get(type) : new HashMap<String, Integer[]>();
				if (map.isEmpty())
					colorMap.put(type, map);
				map.put(color, colors);
			}
		}
	}

	/**
	 * @param session
	 * @param helpDeskList
	 * @param pmMap
	 */
	@Transactional
	private void servisDurumKontrol(Session session, List<String> helpDeskList, HashMap<String, Parameter> pmMap) {
		boolean sistemDestekVar = false;
		String helpDeskLastDateKey = "helpDeskLastDate";
		OrtakIslemler islemler = new OrtakIslemler();
		Gson gson = new Gson();
		if (PdksUtil.getCanliSunucuDurum()) {
			String helpDeskLastDateStr = islemler.getStringHelpDesk();
			if (helpDeskLastDateStr != null && pmMap.containsKey(helpDeskLastDateKey)) {
				Parameter parameter = pmMap.get(helpDeskLastDateKey);
				if (parameter.getValue() == null || !parameter.getValue().equals(helpDeskLastDateStr)) {
					parameterMap.put(helpDeskLastDateKey, helpDeskLastDateStr);
					parameter.setValue(helpDeskLastDateStr);
					pdksEntityController.saveOrUpdate(session, entityManager, parameter);
					session.flush();
				}
			}
		}
		Date helpDeskLastDate = null;
		String servisHelpDeskLastDateStr = null;
		String lisansMesaj = null, path = null;
		if (parameterMap.containsKey("helpDeskLisans") && parameterMap.containsKey("adminInput")) {
			String kodu = parameterMap.get("adminInput");
			String helpDeskLisansStr = parameterMap.get("helpDeskLisans");
			List<String> list = PdksUtil.getListByString(PdksUtil.getDecodeStringByBase64(helpDeskLisansStr), "|");
			if (list.size() == 2) {
				String lisansKey = null;
				for (String string : list) {
					if (string.startsWith("http")) {
						if (path == null)
							path = string;
					} else if (lisansKey == null)
						lisansKey = string;

				}
				if (lisansKey != null && path != null) {
					try {
						LinkedHashMap<String, String> headerMap = new LinkedHashMap<String, String>();
						headerMap.put("kodu", kodu);
						headerMap.put("lisansKey", lisansKey);
						String str = "";
						for (Iterator iterator = headerMap.keySet().iterator(); iterator.hasNext();) {
							String key = (String) iterator.next();
							str += key + " = " + headerMap.get(key);
							if (iterator.hasNext())
								str += "&";
						}
						String json = islemler.getJSONData(path + "?" + str, "POST", null, null, false);
						if (PdksUtil.hasStringValue(json)) {
							JSONParser jsonParser = new JSONParser();
							JSONObject jsonObject = (JSONObject) jsonParser.parse(json);
							if (jsonObject.containsKey("data")) {
								JSONObject jsonObjectData = (JSONObject) jsonObject.get("data");
								LinkedHashMap<String, Object> map = gson.fromJson(jsonObjectData.toJSONString(), LinkedHashMap.class);
								if (map != null && map.containsKey("helpDeskDate")) {
									try {
										servisHelpDeskLastDateStr = (String) map.get("helpDeskDate");
										lisansMesaj = lisansKey + " --> " + servisHelpDeskLastDateStr;
										helpDeskLastDate = OrtakIslemler.getHelpDeskLastDateFrom(servisHelpDeskLastDateStr);
									} catch (Exception ex) {
									}
								}
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}

		if (parameterMap.containsKey(helpDeskLastDateKey)) {
			String helpDeskLastDateStr = parameterMap.get(helpDeskLastDateKey);
			if (servisHelpDeskLastDateStr != null && !servisHelpDeskLastDateStr.equals(helpDeskLastDateStr)) {
				Parameter parameter = pmMap.get(helpDeskLastDateKey);
				if (parameter != null) {
					parameterMap.put(helpDeskLastDateKey, servisHelpDeskLastDateStr);
					parameter.setValue(servisHelpDeskLastDateStr);
					pdksEntityController.saveOrUpdate(session, entityManager, parameter);
					session.flush();
				}
			}
			if (helpDeskLastDate == null) {
				helpDeskLastDate = OrtakIslemler.getHelpDeskLastDateFrom(helpDeskLastDateStr);

			}
		}
		PdksUtil.setHelpDeskLastDate(helpDeskLastDate);
		if (helpDeskLastDate != null)
			sistemDestekVar = PdksUtil.tarihKarsilastirNumeric(new Date(), helpDeskLastDate) != 1;
		if (!sistemDestekVar) {
			for (Iterator iterator = helpDeskList.iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				parameterMap.remove(key);
			}
		} else if (lisansMesaj != null)
			logger.debug(lisansMesaj);
		helpDeskList = null;
		PdksUtil.setSistemDestekVar(sistemDestekVar);
		LinkedHashMap durumMap = new LinkedHashMap();
		durumMap.put("status", sistemDestekVar);
		String content = gson.toJson(durumMap);
		try {
			String dosyaAdi = "/opt/pdks/srvStatus.txt";
			PdksUtil.fileWrite(content, dosyaAdi);
			File file = new File(dosyaAdi);
			if (sistemDestekVar) {
				PdksUtil.fileWrite(content, dosyaAdi);
			} else if (file.exists())
				file.delete();
		} catch (Exception e) {
		}

	}

	/**
	 * @param session
	 */
	@Transactional
	public void setLDAPUserList(Session session) {
		List saveList = new ArrayList(), list = new ArrayList();
		if (session == null)
			session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		HashMap fields = new HashMap();
		fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List ldapUserList = pdksEntityController.getSQLParamByFieldList(LDAPDomain.TABLE_NAME, null, null, LDAPDomain.class, session);
		LDAPDomain ldapUser = LDAPUserManager.getDefaultLDAPUser();
		LDAPDomain ldapUserAna = null;
		if (!ldapUserList.isEmpty()) {
			if (ldapUserList.size() > 1)
				ldapUserList = PdksUtil.sortListByAlanAdi((ArrayList) ldapUserList, "sira", false);
			int sira = 1;
			for (Iterator iterator = ldapUserList.iterator(); iterator.hasNext();) {
				LDAPDomain lu = (LDAPDomain) iterator.next();
				if (ldapUser != null && lu.getLdapHost().equals(ldapUser.getLdapHost())) {
					if (lu.getSira() != ldapUser.getSira() || !lu.getDurum().booleanValue() || !lu.getLdapPort().equals(ldapUser.getLdapPort()) || !lu.getLdapDC().equals(ldapUser.getLdapDC()) || !lu.getLdapAdminPassword().equals(ldapUser.getLdapAdminPassword())) {
						lu.veriAktar(ldapUser.getLdapPort(), ldapUser.getLdapAdminUsername(), ldapUser.getLdapAdminPassword(), ldapUser.getLdapDC());
						saveList.add(lu);
					}
					ldapUserAna = lu;
				} else if (lu.getDurum().booleanValue()) {
					++sira;
					if (lu.getSira() != sira) {
						lu.setSira(sira);
						saveList.add(lu);
					}
					continue;
				}
				iterator.remove();
			}

		}
		if (ldapUserAna == null) {
			ldapUserAna = ldapUser;
			if (ldapUserAna != null)
				saveList.add(ldapUser);
		}
		if (ldapUserAna != null)
			list.add(ldapUserAna);
		if (!ldapUserList.isEmpty()) {
			list.addAll(ldapUserList);
			Collections.reverse(list);
		}

		if (!saveList.isEmpty()) {
			for (Iterator iterator = saveList.iterator(); iterator.hasNext();) {
				LDAPDomain lu = (LDAPDomain) iterator.next();
				if (lu.getLdapHost() != null)
					pdksEntityController.saveOrUpdate(session, entityManager, lu);
			}
			session.flush();
		}

		saveList = null;
		ldapUserList = null;
		fields = null;
		LDAPUserManager.setLdapUserList(list);
	}

	public void fillAccountPermission(Session session, User user) {
		if (session == null) {
			if (user != null)
				session = user.getSessionSQL();
			if (session == null)
				session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		}

		List<AccountPermission> permissionList = (ArrayList<AccountPermission>) pdksEntityController.getSQLParamByFieldList(AccountPermission.TABLE_NAME, AccountPermission.COLUMN_NAME_DURUM, Boolean.TRUE, AccountPermission.class, session);
		String key = "";
		accountPermissionMap = new HashMap<String, AccountPermission>();
		for (AccountPermission accountPermission : permissionList) {
			key = accountPermission.getAction() + "-" + accountPermission.getTarget() + "-" + accountPermission.getRecipient() + "-" + accountPermission.getDiscriminator();
			accountPermissionMap.put(key, accountPermission);
		}
		if (user != null) {
			List<AccountPermission> deletePermissionList = (ArrayList<AccountPermission>) pdksEntityController.getSQLParamByFieldList(AccountPermission.TABLE_NAME, AccountPermission.COLUMN_NAME_DURUM, Boolean.FALSE, AccountPermission.class, session);
			if (!deletePermissionList.isEmpty()) {
				for (Iterator iterator = deletePermissionList.iterator(); iterator.hasNext();) {
					AccountPermission accountPermission = (AccountPermission) iterator.next();
					entityManager.remove(accountPermission);
				}
				entityManager.flush();
			}
		}
		try {
			ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
			if (servletContext != null) {
				List<HttpSession> sessionList = new ArrayList(SessionListener.getSessionList(servletContext));
				if (sessionList != null) {
					Calendar cal = Calendar.getInstance();
					int zoneFark = cal.get(Calendar.ZONE_OFFSET);
					Date simdi = new Date();
					HashMap<String, HttpSession> map = new HashMap<String, HttpSession>();
					for (HttpSession httpSession : sessionList) {
						if (!map.containsKey(httpSession.getId())) {
							KullaniciSession kullaniciSession = new KullaniciSession(httpSession, simdi, zoneFark);
							if (kullaniciSession.getKullanici() != null) {
								User kullanici = kullaniciSession.getKullanici();
								if (kullanici.getMenuYetkiMap() != null)
									kullanici.getMenuYetkiMap().clear();
							}
							map.put(httpSession.getId(), httpSession);
						}
					}
				}
				sessionList = null;
			}
		} catch (Exception e) {
			// TODO: handle exception
		}

	}

	public HashMap<String, MenuItem> getMenuItemMap() {
		return menuItemMap;
	}

	public void setMenuItemMap(HashMap<String, MenuItem> menuItemMap) {
		this.menuItemMap = menuItemMap;
	}

	public String getMailSessionHostAdres() {
		String adres = (smtpHost != null ? smtpHost : "smtp.gmail.com");
		return adres;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public int getSmtpHostPort() {
		return smtpHostPort;
	}

	public void setSmtpHostPort(int smtpHostPort) {
		this.smtpHostPort = smtpHostPort;
	}

	public String getSmtpUserName() {
		return smtpUserName;
	}

	public void setSmtpUserName(String smtpUserName) {
		this.smtpUserName = smtpUserName;
	}

	public String getSmtpPassword() {
		return smtpPassword;
	}

	public void setSmtpPassword(String smtpPassword) {
		this.smtpPassword = smtpPassword;
	}

	public byte[] getProjeHeaderImage() {
		return projeHeaderImage;
	}

	public void setProjeHeaderImage(byte[] projeHeaderImage) {
		this.projeHeaderImage = projeHeaderImage;
	}

	public String getProjeHeaderColor() {
		return projeHeaderColor;
	}

	public void setProjeHeaderColor(String projeHeaderColor) {
		this.projeHeaderColor = projeHeaderColor;
	}

	public String getProjeHeaderBackgroundColor() {
		return projeHeaderBackgroundColor;
	}

	public void setProjeHeaderBackgroundColor(String projeHeaderBackgroundColor) {
		this.projeHeaderBackgroundColor = projeHeaderBackgroundColor;
	}

	public String getProjeHeaderImageWidth() {
		return projeHeaderImageWidth;
	}

	public void setProjeHeaderImageWidth(String projeHeaderImageWidth) {
		this.projeHeaderImageWidth = projeHeaderImageWidth;
	}

	public String getProjeHeaderImageHeight() {
		return projeHeaderImageHeight;
	}

	public void setProjeHeaderImageHeight(String projeHeaderImageHeight) {
		this.projeHeaderImageHeight = projeHeaderImageHeight;
	}

	public String getSmtpSSLDurum() {
		return smtpSSLDurum;
	}

	public void setSmtpSSLDurum(String smtpSSLDurum) {
		this.smtpSSLDurum = smtpSSLDurum;
	}

	public String getSmtpTLSDurum() {
		return smtpTLSDurum;
	}

	public void setSmtpTLSDurum(String smtpTLSDurum) {
		this.smtpTLSDurum = smtpTLSDurum;
	}

	public boolean getSmtpSSL() {
		boolean smtpSSL = Boolean.FALSE;
		try {
			smtpSSL = smtpSSLDurum != null && smtpSSLDurum.equals("1");
		} catch (Exception e) {
			smtpSSL = Boolean.FALSE;
		}
		return smtpSSL;
	}

	public boolean getSmtpTLS() {
		boolean smtpTLS = Boolean.FALSE;
		try {
			smtpTLS = smtpTLSDurum != null && smtpTLSDurum.equals("1");
		} catch (Exception e) {
			smtpTLS = Boolean.FALSE;
		}

		return smtpTLS;
	}

	public byte[] getProjeFooterImage() {
		return projeFooterImage;
	}

	public void setProjeFooterImage(byte[] projeFooterImage) {
		this.projeFooterImage = projeFooterImage;
	}

	public String getProjePowerBy() {
		return projePowerBy;
	}

	public void setProjePowerBy(String projePowerBy) {
		this.projePowerBy = projePowerBy;
	}

	public String getProjeFooterImageWidth() {
		return projeFooterImageWidth;
	}

	public void setProjeFooterImageWidth(String projeFooterImageWidth) {
		this.projeFooterImageWidth = projeFooterImageWidth;
	}

	public String getProjeFooterImageHeight() {
		return projeFooterImageHeight;
	}

	public void setProjeFooterImageHeight(String projeFooterImageHeight) {
		this.projeFooterImageHeight = projeFooterImageHeight;
	}

	public String getProjePowerURL() {
		return projePowerURL;
	}

	public void setProjePowerURL(String projePowerURL) {
		this.projePowerURL = projePowerURL;
	}

	public String getProjeFooterBackgroundColor() {
		return projeFooterBackgroundColor;
	}

	public void setProjeFooterBackgroundColor(String projeFooterBackgroundColor) {
		this.projeFooterBackgroundColor = projeFooterBackgroundColor;
	}

	public String getProjeURL() {
		return projeURL;
	}

	public void setProjeURL(String projeURL) {
		this.projeURL = projeURL;
	}
}
