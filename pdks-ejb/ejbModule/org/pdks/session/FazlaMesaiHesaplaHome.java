package org.pdks.session;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.AylikPuantaj;
import org.pdks.entity.CalismaModeli;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.Departman;
import org.pdks.entity.DepartmanDenklestirmeDonemi;
import org.pdks.entity.FazlaMesaiTalep;
import org.pdks.entity.HareketKGS;
import org.pdks.entity.IzinTipi;
import org.pdks.entity.Kapi;
import org.pdks.entity.KapiView;
import org.pdks.entity.PdksLog;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDenklestirme;
import org.pdks.entity.PersonelDenklestirmeBordro;
import org.pdks.entity.PersonelDenklestirmeBordroDetay;
import org.pdks.entity.PersonelDenklestirmeDinamikAlan;
import org.pdks.entity.PersonelDenklestirmeTasiyici;
import org.pdks.entity.PersonelFazlaMesai;
import org.pdks.entity.PersonelHareketIslem;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelView;
import org.pdks.entity.Sirket;
import org.pdks.entity.Tanim;
import org.pdks.entity.Tatil;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaGorev;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.VardiyaHafta;
import org.pdks.entity.VardiyaSaat;
import org.pdks.entity.YemekIzin;
import org.pdks.enums.BordroDetayTipi;
import org.pdks.security.action.StartupAction;
import org.pdks.security.action.UserHome;
import org.pdks.security.entity.MenuItemConstant;
import org.pdks.security.entity.User;

import com.pdks.webservice.MailFile;
import com.pdks.webservice.MailObject;
import com.pdks.webservice.MailStatu;

@Name("fazlaMesaiHesaplaHome")
public class FazlaMesaiHesaplaHome extends EntityHome<DepartmanDenklestirmeDonemi> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5201033120905302620L;
	static Logger logger = Logger.getLogger(FazlaMesaiHesaplaHome.class);

	public static String sayfaURL = "fazlaMesaiHesapla";

	@RequestParameter
	Long personelDenklestirmeId;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = false, create = true)
	User authenticatedUser;
	@In(required = false, create = true)
	OrtakIslemler ortakIslemler;
	@In(required = false, create = true)
	UserHome userHome;
	@In(required = false, create = true)
	FazlaMesaiOrtakIslemler fazlaMesaiOrtakIslemler;
	@In(required = false, create = true)
	StartupAction startupAction;
	@In(required = false, create = true)
	ComponentState componentState;
	@Out(scope = ScopeType.SESSION, required = false)
	String linkAdres;
	@In(scope = ScopeType.SESSION, required = false)
	String bordroAdres;
	@Out(scope = ScopeType.SESSION, required = false)
	VardiyaGun fazlaMesaiVardiyaGun;
	@In(scope = ScopeType.APPLICATION, required = false)
	List<String> izinliCalisilanGunler;

	@In(required = true, create = true)
	Renderer renderer;

	private List<PersonelDenklestirme> personelDenklestirmeList;

	private List<SelectItem> gorevYeriList, tesisList, altBolumList, planTanimsizBolumList, hataliPersoneller;

	private List<AylikPuantaj> aylikPuantajList;

	private List<Personel> tumBolumPersonelleri;

	private AylikPuantaj seciliAylikPuantaj;

	private List<DepartmanDenklestirmeDonemi> denklestirmeDonemiList;

	private List<PersonelDenklestirme> baslikDenklestirmeDonemiList;

	private HashMap<String, List<Tanim>> ekSahaListMap;

	private List saveGenelList;

	private VardiyaGun seciliVardiyaGun;

	private TreeMap<String, Boolean> baslikMap;

	private Sirket sirket;

	private DenklestirmeAy denklestirmeAy, gecenAy = null;

	private Boolean hataYok, fazlaMesaiIzinKullan = Boolean.FALSE, fazlaMesaiOde = Boolean.FALSE, fazlaMesaiTalepSil = Boolean.FALSE, yetkili = Boolean.FALSE, resmiTatilVar = Boolean.FALSE, haftaTatilVar = Boolean.FALSE, kaydetDurum = Boolean.FALSE;
	private Boolean sutIzniGoster = Boolean.FALSE, suaGoster, gebeGoster = Boolean.FALSE, partTimeGoster = Boolean.FALSE, onayla, hastaneSuperVisor = Boolean.FALSE, sirketIzinGirisDurum = Boolean.FALSE;
	private Boolean kesilenSureGoster = Boolean.FALSE, checkBoxDurum, yoneticiERP1Kontrol = Boolean.FALSE;
	private Boolean aksamGun = Boolean.FALSE, aksamSaat = Boolean.FALSE, hataliPuantajGoster = Boolean.FALSE, stajerSirket, departmanBolumAyni = Boolean.FALSE;
	private Boolean modelGoster = Boolean.FALSE, kullaniciPersonel = Boolean.FALSE, denklestirmeAyDurum = Boolean.FALSE, gecenAyDurum = Boolean.FALSE, izinGoster = Boolean.FALSE, yoneticiRolVarmi = Boolean.FALSE;
	private boolean adminRole, hareketIptalEt = false, ikRole, personelHareketDurum, personelFazlaMesaiDurum, vardiyaPlaniDurum, personelIzinGirisiDurum, fazlaMesaiTalepOnayliDurum = Boolean.FALSE;
	private Boolean izinCalismayanMailGonder = Boolean.FALSE, bakiyeSifirlaDurum = Boolean.FALSE, isAramaGoster = Boolean.FALSE, hatalariAyikla = Boolean.FALSE, kismiOdemeGoster = Boolean.FALSE, yasalFazlaCalismaAsanSaat = Boolean.FALSE;
	private boolean topluGuncelle = false, yarimYuvarla = true, istifaGoster = false, sadeceFazlaMesai = true, saatlikCalismaGoster = false, izinBordoroGoster = false, bordroPuantajEkranindaGoster = false, planOnayDurum, eksikCalismaGoster, eksikMaasGoster = false;
	private int ay, yil, maxYil, sonDonem, pageSize;
	private String manuelGirisGoster = "", kapiGirisSistemAdi = "", birdenFazlaKGSSirketSQL = "";

	private List<User> toList, ccList, bccList;

	private TreeMap<Long, List<FazlaMesaiTalep>> fmtMap;

	private List<FazlaMesaiTalep> fmtList;

	private List<SelectItem> aylar;

	private AylikPuantaj aylikPuantajDefault;

	private TreeMap<String, Tanim> ekSahaTanimMap;

	private String msgError, msgFazlaMesaiError, msgFazlaMesaiInfo, sanalPersonelAciklama, bolumAciklama, tmpAlan, linkBordroAdres, hataliSicilNo;
	private Double eksikSaatYuzde = null;
	private String sicilNo = "", sicilYeniNo = "", excelDosyaAdi, mailKonu, mailIcerik, msgwarnImg = "";
	private List<YemekIzin> yemekAraliklari;
	private CalismaModeli perCalismaModeli;
	private Long seciliEkSaha3Id, sirketId = null, departmanId, gorevTipiId, tesisId, seciliEkSaha4Id, planTanimsizBolumId;
	private Tanim gorevYeri, seciliBolum, seciliAltBolum, ekSaha4Tanim;
	private Double toplamFazlamMesai = 0D;
	private Double aksamCalismaSaati = null, aksamCalismaSaatiYuzde = null;
	private byte[] excelData;
	private List<Tanim> hareketIptalNedenList;
	private HareketKGS islemHareketKGS;
	private boolean mailGonder, tekSirket;
	private Boolean bakiyeGuncelle, ayrikHareketVar, fazlaMesaiOnayDurum = Boolean.FALSE;
	private Boolean gerceklesenMesaiKod = Boolean.FALSE, devredenBakiyeKod = Boolean.FALSE, normalCalismaSaatKod = Boolean.FALSE, haftaTatilCalismaSaatKod = Boolean.FALSE, resmiTatilCalismaSaatKod = Boolean.FALSE, izinSureSaatKod = Boolean.FALSE;
	private Boolean normalCalismaGunKod = Boolean.FALSE, haftaTatilCalismaGunKod = Boolean.FALSE, resmiTatilCalismaGunKod = Boolean.FALSE, izinSureGunKod = Boolean.FALSE, ucretliIzinGunKod = Boolean.FALSE, ucretsizIzinGunKod = Boolean.FALSE, hastalikIzinGunKod = Boolean.FALSE;
	private Boolean normalGunKod = Boolean.FALSE, haftaTatilGunKod = Boolean.FALSE, resmiTatilGunKod = Boolean.FALSE, artikGunKod = Boolean.FALSE, bordroToplamGunKod = Boolean.FALSE, devredenMesaiKod = Boolean.FALSE, ucretiOdenenKod = Boolean.FALSE;
	private List<SelectItem> pdksSirketList, departmanList;
	private Departman departman;
	private String adres, personelIzinGirisiStr, personelHareketStr, personelFazlaMesaiOrjStr, personelFazlaMesaiStr, vardiyaPlaniStr;
	private List<String> sabahVardiyalar, devamlilikPrimIzinTipleri;
	private Vardiya sabahVardiya;
	private Integer aksamVardiyaBasSaat, aksamVardiyaBitSaat, aksamVardiyaBasDakika, aksamVardiyaBitDakika;
	private List<YemekIzin> yemekList;
	private TreeMap<String, Tanim> fazlaMesaiMap;
	private List<Vardiya> izinTipiVardiyaList;
	private TreeMap<String, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap;
	private List<Tanim> denklestirmeDinamikAlanlar;
	private HashMap<Long, List<HareketKGS>> ciftBolumCalisanHareketMap = new HashMap<Long, List<HareketKGS>>();
	private HashMap<Long, Personel> ciftBolumCalisanMap = new HashMap<Long, Personel>();
	private List<HareketKGS> hareketler = new ArrayList<HareketKGS>();
	private Date bugun;
	private User userLogin;
	private Session session;

	@Override
	public Object getId() {
		if (personelDenklestirmeId == null) {
			return super.getId();
		} else {
			return personelDenklestirmeId;
		}
	}

	@Override
	@Begin(join = true)
	public void create() {
		super.create();
	}

	/**
	 * 
	 */
	private void sessionClear() {
		session.clear();
	}

	/**
	 * @param object
	 */
	@Transactional
	private void saveOrUpdate(Object object) {
		if (object != null)
			pdksEntityController.saveOrUpdate(session, entityManager, object);
	}

	/**
	 * 
	 */
	@Transactional
	private void sessionFlush() {
		session.flush();
	}

	/**
	 * 
	 */
	private void bordroAlanKapat() {
		gerceklesenMesaiKod = Boolean.TRUE;
		devredenBakiyeKod = Boolean.TRUE;
		devredenMesaiKod = Boolean.TRUE;
		ucretiOdenenKod = Boolean.TRUE;
		normalCalismaSaatKod = Boolean.FALSE;
		haftaTatilCalismaSaatKod = Boolean.FALSE;
		resmiTatilCalismaSaatKod = Boolean.FALSE;
		izinSureSaatKod = Boolean.FALSE;
		normalCalismaGunKod = Boolean.FALSE;
		haftaTatilCalismaGunKod = Boolean.FALSE;
		resmiTatilCalismaGunKod = Boolean.FALSE;
		izinSureGunKod = Boolean.FALSE;
		ucretliIzinGunKod = Boolean.FALSE;
		ucretsizIzinGunKod = Boolean.FALSE;
		hastalikIzinGunKod = Boolean.FALSE;
		normalGunKod = Boolean.FALSE;
		haftaTatilGunKod = Boolean.FALSE;
		resmiTatilGunKod = Boolean.FALSE;
		artikGunKod = Boolean.FALSE;
		bordroToplamGunKod = Boolean.FALSE;
		if (baslikMap == null)
			baslikMap = new TreeMap<String, Boolean>();
		else
			baslikMap.clear();
	}

	/**
	 * 
	 */
	public void instanceRefresh() {
		if (getInstance().getId() != null)
			session.refresh(getInstance());
	}

	/**
	 * 
	 */
	private void adminRoleDurum() {
		adminRole = userLogin.isAdmin() || userLogin.isSistemYoneticisi() || userLogin.isIKAdmin();
		ikRole = userLogin.isAdmin() || userLogin.isSistemYoneticisi() || userLogin.isIK() || userLogin.isIKDirektor();
	}

	/**
	 * @param vardiyaGun1
	 * @param vardiyaGun2
	 * @return
	 */
	public boolean isGunlerEsit(Date vardiyaGun1, Date vardiyaGun2) {
		boolean esit = false;
		if (vardiyaGun1 != null && vardiyaGun2 != null)
			esit = PdksUtil.tarihKarsilastirNumeric(vardiyaGun1, vardiyaGun2) == 0;
		return esit;
	}

	/**
	 * 
	 */
	public void aylariDoldur() {
		aylar = ortakIslemler.getSelectItemList("ay", authenticatedUser);
		ay = fazlaMesaiOrtakIslemler.aylariDoldur(yil, ay, aylar, session);
	}

	/**
	 * @return
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public String sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		boolean calistir = false;
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		aylikPuantajListClear();
		hataliPersoneller = null;
		userLogin = authenticatedUser;
		planTanimsizBolumList = null;
		componentState.setSeciliTab("tab1");

		tumBolumPersonelleri = null;
		bordroPuantajEkranindaGoster = false;
		saatlikCalismaGoster = false;
		izinBordoroGoster = false;
		linkBordroAdres = null;
		bordroAlanKapat();
		boolean ayniSayfa = userLogin.getCalistigiSayfa() != null && userLogin.getCalistigiSayfa().equals(sayfaURL);
		// if (!ayniSayfa)
		// userLogin.setCalistigiSayfa(sayfaURL);

		izinCalismayanMailGonder = Boolean.FALSE;
		adminRoleDurum();
		String inputPersonelNo = null;
		denklestirmeAy = null;
		fazlaMesaiVardiyaGun = null;
		bolumleriTemizle();
		boolean hareketDoldur = false;
		kismiOdemeGoster = Boolean.FALSE;
		try {
			eksikSaatYuzde = null;
			modelGoster = Boolean.FALSE;
			yasalFazlaCalismaAsanSaat = Boolean.FALSE;
			departmanBolumAyni = Boolean.FALSE;
			bakiyeGuncelle = null;
			stajerSirket = Boolean.FALSE;
			sutIzniGoster = Boolean.FALSE;
			gebeGoster = Boolean.FALSE;
			isAramaGoster = Boolean.FALSE;
			partTimeGoster = Boolean.FALSE;
			suaGoster = Boolean.FALSE;
			mailGonder = Boolean.FALSE;
			setSirket(null);
			sirketId = null;
			setTesisId(null);
			setTesisList(null);
			aylar = ortakIslemler.getAyListesi(Boolean.TRUE);
			seciliEkSaha3Id = null;
			seciliEkSaha4Id = null;
			Calendar cal = Calendar.getInstance();
			ay = cal.get(Calendar.MONTH) + 1;
			yil = cal.get(Calendar.YEAR);
			cal.add(Calendar.WEEK_OF_YEAR, 1);
			maxYil = cal.get(Calendar.YEAR);
			if (ortakIslemler.getCanliDurum() == false && authenticatedUser.isAdmin() && ay == 12)
				++maxYil;
			sonDonem = (maxYil * 100) + cal.get(Calendar.MONTH) + 1;
			setInstance(new DepartmanDenklestirmeDonemi());
			fillEkSahaTanim();
			if (userLogin.isSuperVisor() || userLogin.isProjeMuduru()) {
				setSirket(userLogin.getPdksPersonel().getSirket());
				bolumDoldur();
			}
			if (!adminRole) {
				if (departmanId == null && !userLogin.isYoneticiKontratli())
					setDepartmanId(userLogin.getDepartman().getId());
			}

			Departman pdksDepartman = null;
			if (!userLogin.isAdmin())
				pdksDepartman = userLogin.getDepartman();

			getInstance().setDepartman(pdksDepartman);

			hastaneSuperVisor = Boolean.FALSE;
			if (!(ikRole) && userLogin.getSuperVisorHemsirePersonelNoList() != null) {
				String superVisorHemsireSayfalari = ortakIslemler.getParameterKey("superVisorHemsireSayfalari");
				List<String> sayfalar = PdksUtil.hasStringValue(superVisorHemsireSayfalari) ? PdksUtil.getListByString(superVisorHemsireSayfalari, null) : null;
				hastaneSuperVisor = sayfalar != null && sayfalar.contains(sayfaURL);

			}

			setPersonelDenklestirmeList(new ArrayList<PersonelDenklestirme>());
			HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

			String ayStr = (String) req.getParameter("ay");
			String yilStr = (String) req.getParameter("yil");
			String linkAdresKey = (String) req.getParameter("linkAdresKey");

			String gorevYeriIdStr = null, sirketIdStr = null, tesisIdStr = null, altBolumIdStr = null;
			LinkedHashMap<String, Object> veriLastMap = null;
			if (linkAdresKey == null) {
				calistir = true;
				veriLastMap = ortakIslemler.getLastParameter(sayfaURL, session);
				if (veriLastMap != null && !veriLastMap.isEmpty()) {

					if (veriLastMap.containsKey("yil"))
						yilStr = (String) veriLastMap.get("yil");
					if (veriLastMap.containsKey("ay"))
						ayStr = (String) veriLastMap.get("ay");
					if (veriLastMap.containsKey("sirketId"))
						sirketIdStr = (String) veriLastMap.get("sirketId");
					if (veriLastMap.containsKey("tesisId"))
						tesisIdStr = (String) veriLastMap.get("tesisId");
					if (veriLastMap.containsKey("bolumId"))
						gorevYeriIdStr = (String) veriLastMap.get("bolumId");
					if (veriLastMap.containsKey("altBolumId"))
						altBolumIdStr = (String) veriLastMap.get("altBolumId");
					if (veriLastMap.containsKey("inputPersonelNo"))
						inputPersonelNo = (String) veriLastMap.get("inputPersonelNo");

					if (veriLastMap.containsKey("sadeceFazlaMesai"))
						sadeceFazlaMesai = (Boolean) veriLastMap.get("sadeceFazlaMesai");
					else
						sadeceFazlaMesai = Boolean.TRUE;
					if ((ikRole) && veriLastMap.containsKey("sicilNo"))
						setSicilNo((String) veriLastMap.get("sicilNo"));
					if (veriLastMap.containsKey("hataliPuantajGoster"))
						hataliPuantajGoster = new Boolean((String) veriLastMap.get("hataliPuantajGoster"));
					if (veriLastMap.containsKey("sayfaURL")) {
						String str = (String) veriLastMap.get("sayfaURL");
						if (str.equalsIgnoreCase(sayfaURL))
							linkBordroAdres = bordroAdres;
					}

				}
			}
			linkAdres = null;
			if (linkAdresKey != null || (ayStr != null && yilStr != null)) {
				if (linkAdresKey != null) {
					HashMap<String, String> veriMap = PdksUtil.getDecodeMapByBase64(linkAdresKey);
					if (veriMap.containsKey("yil"))
						yilStr = veriMap.get("yil");
					if (veriMap.containsKey("ay"))
						ayStr = veriMap.get("ay");
					if (veriMap.containsKey("sirketId"))
						sirketIdStr = veriMap.get("sirketId");
					if (veriMap.containsKey("inputPersonelNo"))
						inputPersonelNo = (String) veriMap.get("inputPersonelNo");
					if (veriMap.containsKey("linkBordroAdres")) {
						String str = veriMap.get("linkBordroAdres");
						linkBordroAdres = PdksUtil.getDecodeStringByBase64(str);
					}
					calistir = veriMap.containsKey("calistir");
					if (veriMap.containsKey("sadeceFazlaMesai"))
						sadeceFazlaMesai = new Boolean(veriMap.get("sadeceFazlaMesai"));
					else
						sadeceFazlaMesai = true;
					if (veriMap.containsKey("tesisId"))
						tesisIdStr = veriMap.get("tesisId");
					if (veriMap.containsKey("sicilNo"))
						setSicilNo(veriMap.get("sicilNo"));
					if (veriMap.containsKey("hataliPuantajGoster"))
						hataliPuantajGoster = new Boolean((String) veriMap.get("hataliPuantajGoster"));
					if (veriMap.containsKey("gorevYeriId"))
						gorevYeriIdStr = veriMap.get("gorevYeriId");
					if (veriMap.containsKey("altBolumId"))
						altBolumIdStr = veriMap.get("altBolumId");
					veriMap = null;
				} else if (veriLastMap == null || veriLastMap.isEmpty()) {
					altBolumIdStr = (String) req.getParameter("altBolumId");
					gorevYeriIdStr = (String) req.getParameter("gorevYeriId");
					tesisIdStr = (String) req.getParameter("tesisId");
					sirketIdStr = (String) req.getParameter("sirketId");
				}

				if (yilStr != null && ayStr != null) {
					yil = Integer.parseInt(yilStr);
					ay = Integer.parseInt(ayStr);
					sirket = null;
					if (gorevYeriIdStr != null)
						seciliEkSaha3Id = Long.parseLong(gorevYeriIdStr);
					if (altBolumIdStr != null)
						seciliEkSaha4Id = Long.parseLong(altBolumIdStr);
					if (tesisIdStr != null)
						tesisId = Long.parseLong(tesisIdStr);
					if (sirketIdStr != null) {
						sirketId = Long.parseLong(sirketIdStr);

					}
					if (sirketId != null && sirket == null) {
						sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
						if (ikRole && sirket != null) {
							departman = sirket.getDepartman();
							departmanId = departman.getId();
						}
					}
					fillSirketList();

					if (sirket != null) {
						long oncekiId = sirket.getId();
						if (sirket.isTesisDurumu())
							tesisDoldur(false);
						if (altBolumIdStr != null)
							seciliEkSaha4Id = Long.parseLong(altBolumIdStr);
						if (tesisId != null || sirket.isTesisDurumu() == false || seciliEkSaha3Id != null)
							bolumDoldur();
						if (altBolumIdStr != null)
							seciliEkSaha4Id = Long.parseLong(altBolumIdStr);
						if (sirket == null) {

							sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, oncekiId, Sirket.class, session);
							if (sirketId == null)
								sirketId = oncekiId;
						}

						departmanId = sirket.getDepartman().getId();
						setDepartman(sirket.getDepartman());
					}
					hareketDoldur = seciliEkSaha3Id != null || seciliEkSaha4Id != null;

				}

			}
			linkAdres = null;
			if (denklestirmeAy == null)
				setSeciliDenklestirmeAy();
			if (PdksUtil.hasStringValue(yilStr) == false && PdksUtil.hasStringValue(ayStr) == false) {
				yilAyDegisti(false);
			}

			if (hareketDoldur == false) {
				if (!userLogin.isAdmin() && !userLogin.isIK() && !userLogin.isYoneticiKontratli()) {
					sirket = userLogin.getPdksPersonel().getSirket();
					sirketId = sirket.getId();
				}

				setDepartman(departmanId != null ? (Departman) pdksEntityController.getSQLParamByFieldObject(Departman.TABLE_NAME, Departman.COLUMN_NAME_ID, departmanId, Departman.class, session) : null);

				if (tesisIdStr != null) {
					if (tesisList != null && !tesisList.isEmpty())
						setTesisId(Long.parseLong(tesisIdStr));
					else
						tesisIdStr = null;
				}

				if (departman != null && !departman.isAdminMi()) {

					if (sirket != null || sirketId != null) {
						if (sirket == null) {

							sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
						}
						gorevYeriList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);

					}
				} else if (sirketId != null) {
					tesisDoldur(false);
				} else {
					fillSirketList();
					if (sirketId != null) {
						tesisDoldur(false);
					}
				}

				if (tesisIdStr != null)
					setTesisId(Long.parseLong(tesisIdStr));
				bolumDoldur();
			} else if (veriLastMap == null || linkBordroAdres != null) {

				if (linkBordroAdres != null && PdksUtil.hasStringValue(sicilNo)) {
					inputPersonelNo = sicilNo;
					hataliPuantajGoster = true;
				}
				if (inputPersonelNo == null || !(hataliPuantajGoster != null && hataliPuantajGoster && PdksUtil.hasStringValue(sicilNo))) {
					if (calistir)
						fillPersonelDenklestirmeList(null);
				} else {
					hataliSicilNo = sicilNo;
					fillHataliPersonelDenklestirmeList();
				}

			}
			setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));

			if (hataliPuantajGoster != null && hataliPuantajGoster && PdksUtil.hasStringValue(sicilNo) && PdksUtil.hasStringValue(hataliSicilNo) == false)
				fillHataliPersonelleriGuncelle();

			if (denklestirmeAyDurum.equals(Boolean.FALSE))
				hataliPuantajGoster = denklestirmeAyDurum;
			if (!ayniSayfa)
				userLogin.setCalistigiSayfa("");

		} catch (Exception e) {
			e.printStackTrace();
		}
		aylariDoldur();
		kullaniciPersonel = ortakIslemler.getKullaniciPersonel(userLogin);
		if (kullaniciPersonel) {
			tesisList = null;
			setSicilNo(userLogin.getPdksPersonel().getPdksSicilNo());
		}

		return "";
	}

	private void aylikPuantajListClear() {
		if (userLogin == null)
			userLogin = authenticatedUser;
		if (aylikPuantajList != null)
			aylikPuantajList.clear();
		else
			aylikPuantajList = ortakIslemler.getSelectItemList("aylikPuantaj", userLogin);
	}

	/**
	 * 
	 */
	private void fillEkSahaTanim() {
		HashMap sonucMap = ortakIslemler.fillEkSahaTanim(session, Boolean.FALSE, null);
		setEkSahaListMap((HashMap<String, List<Tanim>>) sonucMap.get("ekSahaList"));
		setEkSahaTanimMap((TreeMap<String, Tanim>) sonucMap.get("ekSahaTanimMap"));
		bolumAciklama = (String) sonucMap.get("bolumAciklama");

	}

	/**
	 * 
	 */
	private void setSeciliDenklestirmeAy() {
		gecenAy = null;
		aylikPuantajList.clear();
		HashMap fields = new HashMap();
		if (denklestirmeAy == null && ay > 0) {

			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			if (denklestirmeAy != null) {

				if (denklestirmeAy.getFazlaMesaiMaxSure() == null)
					fazlaMesaiOrtakIslemler.setFazlaMesaiMaxSure(denklestirmeAy, session);
				fields.clear();
				StringBuffer sb = new StringBuffer();
				sb.append("select top 1 D." + PersonelDenklestirme.COLUMN_NAME_ID + " from " + PersonelDenklestirme.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where D." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = :d and D." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1");
				fields.put(PdksEntityController.MAP_KEY_SELECT, "id");
				fields.put("d", denklestirmeAy.getId());
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List idList = pdksEntityController.getObjectBySQLList(sb, fields, null);
				if (idList.isEmpty()) {
					denklestirmeAy = null;
					if (userLogin.getLogin())
						PdksUtil.addMessageWarn((ay > 0 ? yil + " " + (aylar.get(ay - 1).getLabel()) : "") + " döneme ait denkleştirme verisi tanımlanmamıştır!");
				}

				idList = null;
			} else if (userLogin.getLogin())
				PdksUtil.addMessageAvailableError((ay > 0 ? yil + " " + (aylar.get(ay - 1).getLabel()) : "") + " döneme ait çalışma planı tanımlanmamıştır!");
		}
		if (denklestirmeAy != null) {
			fields.clear();
			StringBuffer sb = new StringBuffer();
			sb.append("select D.* from " + DenklestirmeAy.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" where (D." + DenklestirmeAy.COLUMN_NAME_YIL + "*100)+ D." + DenklestirmeAy.COLUMN_NAME_AY + " <:s");
			fields.put("s", denklestirmeAy.getYil() * 100 + denklestirmeAy.getAy());
			sb.append(" order by (D." + DenklestirmeAy.COLUMN_NAME_YIL + "*100)+ D." + DenklestirmeAy.COLUMN_NAME_AY + " desc ");
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<DenklestirmeAy> list = pdksEntityController.getObjectBySQLList(sb, fields, DenklestirmeAy.class);
			if (!list.isEmpty())
				gecenAy = list.get(0);
		}
		gecenAyDurum = gecenAy != null && gecenAy.getDurum();
		// gecenAyDurum = gecenAy != null && fazlaMesaiOrtakIslemler.getDurum(gecenAy);
		setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
	}

	/**
	 * @param yilParametre
	 * @return
	 * @throws Exception
	 */
	public String yilAyDegisti(boolean yilParametre) throws Exception {
		denklestirmeAy = null;
		if (yilParametre) {
			aylariDoldur();
			if (aylar != null && aylar.size() == 1)
				ay = (Integer) aylar.get(0).getValue();
		}

		else {
			Integer seciliAy = ay;
			ay = 0;
			for (SelectItem st : aylar) {
				if (st.getValue().equals(seciliAy))
					ay = seciliAy;
			}
		}
		setSeciliDenklestirmeAy();
		if (denklestirmeAy != null) {
			departmanDegisti(false);
		}

		return "";
	}

	/**
	 * 
	 */
	private void fillDepartmanList() {
		planTanimsizBolumList = null;
		if (denklestirmeAy == null)
			setSeciliDenklestirmeAy();
		List<SelectItem> departmanListe = fazlaMesaiOrtakIslemler.getFazlaMesaiDepartmanList(denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
		if (!departmanListe.isEmpty()) {
			Long onceki = departmanId;
			if (departmanListe.size() == 1)
				departmanId = (Long) departmanListe.get(0).getValue();
			else if (onceki != null) {
				for (SelectItem st : departmanListe) {
					if (st.getValue().equals(onceki))
						departmanId = onceki;
				}
			}
		} else
			departmanId = null;
		setDepartmanList(departmanListe);
	}

	/**
	 * @param degisti
	 * @return
	 */
	public String departmanDegisti(boolean degisti) {
		if (degisti) {
			sirketId = null;
			seciliEkSaha3Id = null;
			seciliEkSaha4Id = null;
			if (tesisList != null)
				tesisList.clear();
			if (gorevYeriList != null)
				gorevYeriList.clear();

		}
		fillSirketList();
		if (!pdksSirketList.isEmpty()) {
			boolean bolumDoldurulmadi = true;
			if (sirketId != null || pdksSirketList.size() == 1) {
				Long tesisIdOnceki = tesisId;
				if (pdksSirketList.size() == 1)
					sirketId = (Long) pdksSirketList.get(0).getValue();
				try {
					tesisDoldur(false);
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (tesisList.size() == 1) {
					tesisId = (Long) tesisList.get(0).getValue();
					bolumDoldur();
					bolumDoldurulmadi = false;
				} else if (tesisIdOnceki != null && !tesisList.isEmpty()) {
					for (SelectItem si : tesisList) {
						Long id = (Long) si.getValue();
						if (id.equals(tesisIdOnceki))
							tesisId = tesisIdOnceki;
					}
					if (tesisId == null) {
						seciliEkSaha3Id = null;
						seciliEkSaha4Id = null;
					}

				}
			}
			if (bolumDoldurulmadi) {

				if (tesisId != null || seciliEkSaha3Id != null || (sirket != null && sirket.isTesisDurumu() == false))
					bolumDoldur();
				if (seciliEkSaha3Id != null && ekSaha4Tanim != null)
					altBolumDoldur();
			}

		}
		return "";
	}

	/**
	 * 
	 */
	private void fillSirketList() {
		if (adminRole)
			fillDepartmanList();
		planTanimsizBolumList = null;
		List<SelectItem> sirketler = null;
		tumBolumPersonelleri = null;
		eksikSaatYuzde = getDepartmanSaatlikIzin();
		bolumleriTemizle();

		try {
			if (denklestirmeAy == null)
				setSeciliDenklestirmeAy();
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		if (departmanId != null) {

			setDepartman((Departman) pdksEntityController.getSQLParamByFieldObject(Departman.TABLE_NAME, Departman.COLUMN_NAME_ID, departmanId, Departman.class, session));

		} else
			setDepartman(null);

		if (gorevYeriList != null)
			gorevYeriList.clear();
		ekSaha4Tanim = null;
		if (ikRole || userLogin.isYonetici()) {
			Long depId = departman != null ? departman.getId() : null;
			sirketler = fazlaMesaiOrtakIslemler.getFazlaMesaiSirketList(depId, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
			sirket = null;
			if (!sirketler.isEmpty()) {
				Long onceki = sirketId;
				if (sirketler.size() == 1) {
					sirketId = (Long) sirketler.get(0).getValue();
				} else if (onceki != null) {
					if (ikRole)
						sirketId = null;
					for (SelectItem st : sirketler) {
						if (st.getValue().equals(onceki))
							sirketId = onceki;
					}
				}
				if (sirketId != null) {

					sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
					ekSaha4Tanim = ortakIslemler.getEkSaha4(sirket, sirketId, session);
				}
			}
			setPdksSirketList(sirketler);
		} else {
			setSirket(userLogin.getPdksPersonel().getSirket());
		}

		aylikPuantajList.clear();
		setPersonelDenklestirmeList(new ArrayList<PersonelDenklestirme>());

	}

	/**
	 * @return
	 */
	private Double getDepartmanSaatlikIzin() {
		Double yuzde = null;
		String eksikSaatYuzdeStr = ortakIslemler.getParameterKey("eksikSaatYuzde");
		if (PdksUtil.hasStringValue(eksikSaatYuzdeStr) && !eksikSaatYuzdeStr.equals("0")) {
			try {
				yuzde = Double.parseDouble(eksikSaatYuzdeStr);
				if (yuzde <= 0.0d)
					yuzde = null;
			} catch (Exception e) {
				yuzde = null;
			}
		}
		if (yuzde == null && departmanId != null) {
			HashMap fields = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select * from " + IzinTipi.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" where " + IzinTipi.COLUMN_NAME_DURUM + " = 1 and " + IzinTipi.COLUMN_NAME_SAAT_GOSTERILECEK + " = 1 ");
			if (departmanId != null) {
				fields.put("d", departmanId);
				sb.append(" and " + IzinTipi.COLUMN_NAME_DEPARTMAN + " = :d");
			}
			sb.append(" and " + IzinTipi.COLUMN_NAME_GIRIS_TIPI + " <> :g ");
			fields.put("g", IzinTipi.GIRIS_TIPI_YOK);

			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<IzinTipi> izinTipiList = pdksEntityController.getObjectBySQLList(sb, fields, IzinTipi.class);
			if (!izinTipiList.isEmpty())
				yuzde = 100.0d;
		}
		return yuzde;
	}

	/**
	 * @param seciliBolumId
	 * @return
	 */
	public String bolumCalismaPlaniOlustur() {
		String str = "";

		if (planTanimsizBolumId != null) {
			String sayfa = VardiyaGunHome.sayfaURL;
			LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
			lastMap.put("yil", "" + yil);
			lastMap.put("ay", "" + ay);
			if (departmanId != null) {
				lastMap.put("departmanId", "" + departmanId);
				lastMap.put("departman", ortakIslemler.getSelectItemText(departmanId, departmanList));
			}

			if (sirketId != null) {
				lastMap.put("sirketId", "" + sirketId);
				lastMap.put("sirket", ortakIslemler.getSelectItemText(sirketId, pdksSirketList));
			}

			if (tesisId != null) {
				lastMap.put("tesisId", "" + tesisId);
				lastMap.put("tesis", ortakIslemler.getSelectItemText(tesisId, tesisList));
			}

			if (ekSaha4Tanim != null) {
				if (seciliEkSaha3Id != null) {
					lastMap.put("bolumId", "" + seciliEkSaha3Id);
					lastMap.put("bolum", ortakIslemler.getSelectItemText(seciliEkSaha3Id, gorevYeriList));
					lastMap.put("altBolumId", "" + planTanimsizBolumId);
					lastMap.put("altBolum", ortakIslemler.getSelectItemText(planTanimsizBolumId, altBolumList));
				}
			} else if (planTanimsizBolumId != null) {
				lastMap.put("bolumId", "" + planTanimsizBolumId);
				lastMap.put("bolum", ortakIslemler.getSelectItemText(planTanimsizBolumId, gorevYeriList));
			}

			lastMap.put("veriDoldur", "F");
			lastMap.put("plansiz", yil * 100 + ay);
			lastMap.put("sayfaURL", sayfa);
			try {
				ortakIslemler.saveLastParameter(lastMap, session);
			} catch (Exception e) {
			}
			Map<String, String> requestHeaderMap = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
			adres = requestHeaderMap.containsKey("host") ? requestHeaderMap.get("host") : "";
			linkAdres = getLinkAdresBilgi(sicilNo, false);
			if (ekSaha4Tanim == null)
				seciliEkSaha3Id = planTanimsizBolumId;
			else
				seciliEkSaha4Id = planTanimsizBolumId;
			saveLastParameter(null);

			str = MenuItemConstant.vardiyaPlani;
		} else {
			if (ekSaha4Tanim == null)
				PdksUtil.addMessageWarn("Plansız " + bolumAciklama + " seçiniz!");
			else
				PdksUtil.addMessageWarn("Plansız " + ekSaha4Tanim.getAciklama() + " seçiniz!");
		}

		return str;

	}

	/**
	 * @return
	 */
	public String fillPersonelSicilDenklestirmeList() {
		if (!PdksUtil.hasStringValue(sicilNo))
			aylikPuantajList.clear();
		else {
			sicilYeniNo = ortakIslemler.getSicilNo(sicilNo);
			if (sicilNo != null && !sicilYeniNo.equals(sicilNo))
				setSicilNo(sicilYeniNo);
			try {
				fillPersonelDenklestirmeList(null);
			} catch (Exception e) {
				logger.equals(e);
				e.printStackTrace();
			}

		}

		return "";
	}

	/**
	 * @param personel
	 * @return
	 */
	@Transactional
	public String fillBolumPersonelDenklestirmeList(Personel secPersonel) {

		if (secPersonel != null && secPersonel.getEkSaha3() != null) {
			setSicilNo(secPersonel.getPdksSicilNo());
			seciliEkSaha3Id = secPersonel.getEkSaha3().getId();
			if (ekSaha4Tanim != null && secPersonel.getEkSaha4() != null) {
				seciliEkSaha4Id = secPersonel.getEkSaha4().getId();
				altBolumDoldur();
			}
			fillPersonelDenklestirmeList(null);
		}

		return "";
	}

	public String fillHataliPersonelDenklestirmeList() {
		if (PdksUtil.hasStringValue(hataliSicilNo)) {
			setSicilNo(hataliSicilNo);
			fillPersonelDenklestirmeList(hataliSicilNo);
			fillHataliPersonelleriGuncelle();
		}
		return "";

	}

	/**
	 * 
	 */
	private void fillHataliPersonelleriGuncelle() {
		hataliPersoneller = null;
		setDenklestirmeAyDurum(denklestirmeAy != null && fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
		if (denklestirmeAyDurum && ortakIslemler.getParameterKey("hataliPersonelGuncelle").equals("1")) {
			DepartmanDenklestirmeDonemi denklestirmeDonemi = new DepartmanDenklestirmeDonemi();
			AylikPuantaj aylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, denklestirmeDonemi, session);
			List<Personel> donemPerList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(sirket, tesisId != null ? String.valueOf(tesisId) : null, seciliEkSaha3Id, seciliEkSaha4Id, denklestirmeAy != null ? aylikPuantajSablon : null, sadeceFazlaMesai, session);
			List<Long> perIdList = new ArrayList<Long>();
			for (Personel personel : donemPerList)
				perIdList.add(personel.getId());
			List<PersonelDenklestirme> list = getPdksPersonelDenklestirmeler(perIdList);
			perIdList = null;
			donemPerList = null;
			if (!list.isEmpty()) {
				hataliPersoneller = ortakIslemler.getSelectItemList("hataliPersonel", authenticatedUser);
				boolean kayitVar = false;
				if (hataliSicilNo == null)
					hataliSicilNo = "";
				List<String> perNoList = new ArrayList<String>();
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					PersonelDenklestirme personelDenklestirme = (PersonelDenklestirme) iterator.next();
					if (personelDenklestirme.getDurum()) {
						iterator.remove();
						continue;
					}
					Personel personel = personelDenklestirme.getPdksPersonel();
					String perNo = personel.getPdksSicilNo();
					perNoList.add(perNo);
					hataliPersoneller.add(new SelectItem(perNo, personel.getAdSoyad() + " [ " + perNo + " ]"));
					if (!kayitVar)
						kayitVar = perNo.equals(hataliSicilNo);
				}
				if (list.isEmpty() && PdksUtil.hasStringValue(bordroAdres) == false) {
					PdksUtil.addMessageInfo("Hatalı personel puantajı bulunmadı.");
					hataliSicilNo = "";
				} else if (!kayitVar) {
					if (perNoList.contains(sicilNo))
						hataliSicilNo = sicilNo;
					else if (!hataliPersoneller.isEmpty()) {
						hataliSicilNo = (String) hataliPersoneller.get(0).getValue();
						if (PdksUtil.hasStringValue(bordroAdres) == false)
							setSicilNo(hataliSicilNo);
					}
				}
				if (hataliPersoneller.isEmpty())
					hataliPersoneller = null;

				perNoList = null;
			}
			hataliPuantajGoster = !list.isEmpty();
			list = null;
		}

	}

	/**
	 * @return
	 */
	@Transactional
	public String fillPersonelDenklestirmeList(String inputPersonelNo) {
		componentState.setSeciliTab("tab1");
		aksamGun = Boolean.FALSE;
		aksamSaat = Boolean.FALSE;
		haftaTatilVar = Boolean.FALSE;
		mailGonder = !(ikRole);
		linkAdres = null;
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, userLogin);
		sessionClear();
		ciftBolumCalisanMap.clear();
		ciftBolumCalisanHareketMap.clear();
		yoneticiRolVarmi = ortakIslemler.yoneticiRolKontrol(session);
		// fillSirketList();

		personelDenklestirmeList.clear();
		ayrikHareketVar = false;

		denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
		denklestirmeAyDurum = fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy);
		if (denklestirmeAy != null) {
			try {
				if (denklestirmeAy.getFazlaMesaiMaxSure() == null)
					fazlaMesaiOrtakIslemler.setFazlaMesaiMaxSure(denklestirmeAy, session);
				DepartmanDenklestirmeDonemi denklestirmeDonemi = new DepartmanDenklestirmeDonemi();
				AylikPuantaj aylikPuantaj = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, denklestirmeDonemi, session);

				aylikPuantaj.setLoginUser(authenticatedUser);
				denklestirmeDonemi.setLoginUser(authenticatedUser);
				denklestirmeDonemi.setDenklestirmeAy(denklestirmeAy);
				setTopluGuncelle(false);
				fillPersonelDenklestirmeDevam(inputPersonelNo, aylikPuantaj, denklestirmeDonemi);
			} catch (Exception ee) {
				ortakIslemler.setExceptionLog(null, ee);
			}

		} else if (userLogin.getLogin())
			PdksUtil.addMessageWarn("İlgili döneme ait fazla mesai bulunamadı!");

		if (!(ikRole))
			departmanBolumAyni = false;
		tmpAlan = "";
		return "";
	}

	/**
	 * @return
	 */
	public String kaydetSec() {
		for (AylikPuantaj puantaj : aylikPuantajList) {
			PersonelDenklestirme personelDenklestirmeAylik = puantaj.getPersonelDenklestirme();
			if (puantaj.isDonemBitti() && personelDenklestirmeAylik.isOnaylandi() && personelDenklestirmeAylik.getDurum() && puantaj.isFazlaMesaiHesapla() && !personelDenklestirmeAylik.isErpAktarildi())
				puantaj.setKaydet(kaydetDurum);
			else
				puantaj.setKaydet(Boolean.FALSE);

		}
		return "";
	}

	/**
	 * @param aylikPuantajSablon
	 * @param denklestirmeDonemi
	 */
	@Transactional
	public List<AylikPuantaj> fillPersonelDenklestirmeDevam(String inputPersonelNo, AylikPuantaj aylikPuantajSablon, DepartmanDenklestirmeDonemi denklestirmeDonemi) {
		boolean kullaniciCalistir = authenticatedUser != null && userHome != null;
		aylikPuantajListClear();
		boolean sonHafta = false;
		User loginUser = aylikPuantajSablon.getLoginUser();
		if (loginUser == null && kullaniciCalistir)
			loginUser = authenticatedUser;
		if (userLogin == null)
			userLogin = loginUser;
		Personel per = loginUser.getPdksPersonel();
		Boolean mudurAltSeviye = ortakIslemler.getMudurAltSeviyeDurum(per, session);
		if (per != null)
			per.setMudurAltSeviye(mudurAltSeviye);
		denklestirmeDonemi.setDenklestirmeAy(denklestirmeAy);
		yoneticiERP1Kontrol = !ortakIslemler.getParameterKeyHasStringValue(("yoneticiERP1Kontrol"));
		msgwarnImg = "";
		bordroAlanKapat();
		eksikMaasGoster = false;
		if (kullaniciCalistir && loginUser.getId().equals(authenticatedUser.getId()))
			saveLastParameter(inputPersonelNo);
		boolean testDurum = PdksUtil.getTestDurum() && PdksUtil.getCanliSunucuDurum() == false;
		testDurum = false;
		Date basTarih = new Date();
		if (testDurum)
			logger.info("fillPersonelDenklestirmeDevam 0000 " + basTarih);
		String haftaTatilDurum = ortakIslemler.getParameterKey("haftaTatilDurum");
		seciliBolum = null;
		seciliAltBolum = null;
		kismiOdemeGoster = Boolean.FALSE;
		fazlaMesaiVardiyaGun = null;
		Tanim devamlilikPrimi = null;
		kesilenSureGoster = Boolean.FALSE;
		sanalPersonelAciklama = ortakIslemler.sanalPersonelAciklama();
		izinGoster = (loginUser.isAdmin() || loginUser.isSistemYoneticisi() || ortakIslemler.getParameterKeyHasStringValue(("izinPersonelOzetGoster")));
		sabahVardiya = null;
		departmanBolumAyni = Boolean.FALSE;
		aksamGun = Boolean.FALSE;
		aksamSaat = Boolean.FALSE;
		haftaTatilVar = Boolean.FALSE;
		fazlaMesaiIzinKullan = Boolean.FALSE;
		fazlaMesaiOde = Boolean.FALSE;
		sirketIzinGirisDurum = Boolean.FALSE;
		yemekList = null;
		bugun = new Date();
		fazlaMesaiOnayDurum = Boolean.FALSE;
		bordroPuantajEkranindaGoster = ortakIslemler.getParameterKey("bordroPuantajEkranindaGoster").equals("1");
		if (baslikMap == null)
			baslikMap = new TreeMap<String, Boolean>();
		else
			baslikMap.clear();
		if (fmtMap == null)
			fmtMap = new TreeMap<Long, List<FazlaMesaiTalep>>();
		else
			fmtMap.clear();
		if (saveGenelList == null)
			saveGenelList = new ArrayList();
		else
			saveGenelList.clear();

		if (kullaniciCalistir) {
			Map<String, String> map1 = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
			adres = map1 != null && map1.containsKey("host") ? map1.get("host") : "";
		}

		departmanBolumAyni = sirket != null && sirket.isTesisDurumu() == false;
		if (sicilNo != null)
			setSicilNo(sicilNo.trim());
		setHataYok(Boolean.FALSE);
		if (denklestirmeDinamikAlanlar == null)
			denklestirmeDinamikAlanlar = new ArrayList<Tanim>();
		else
			denklestirmeDinamikAlanlar.clear();
		sutIzniGoster = Boolean.FALSE;
		gebeGoster = Boolean.FALSE;
		isAramaGoster = Boolean.FALSE;
		yasalFazlaCalismaAsanSaat = Boolean.FALSE;
		partTimeGoster = Boolean.FALSE;
		suaGoster = Boolean.FALSE;
		aylikPuantajSablon.getVardiyalar();
		setAylikPuantajDefault(aylikPuantajSablon);

		kaydetDurum = Boolean.FALSE;
		String aksamBordroBasZamani = ortakIslemler.getParameterKey("aksamBordroBasZamani"), aksamBordroBitZamani = ortakIslemler.getParameterKey("aksamBordroBitZamani");
		Integer[] basZaman = ortakIslemler.getSaatDakika(aksamBordroBasZamani), bitZaman = ortakIslemler.getSaatDakika(aksamBordroBitZamani);
		aksamVardiyaBasSaat = basZaman[0];
		aksamVardiyaBasDakika = basZaman[1];
		aksamVardiyaBitSaat = bitZaman[0];
		aksamVardiyaBitDakika = bitZaman[1];

		try {
			seciliBolum = null;
			seciliAltBolum = null;
			setSeciliVardiyaGun(null);
			HashMap map = new HashMap();
			List<String> perList = new ArrayList<String>();
			sicilYeniNo = ortakIslemler.getSicilNo(sicilNo);
			if (sirketId != null && (sirket == null || sirket.getDepartman() == null))
				sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
			List<Personel> donemPerList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(sirket, tesisId != null ? String.valueOf(tesisId) : null, seciliEkSaha3Id, seciliEkSaha4Id, denklestirmeAy != null ? aylikPuantajSablon : null, sadeceFazlaMesai, session);
			if (testDurum)
				logger.info("fillPersonelDenklestirmeDevam 1000 " + basTarih);
			List<Long> perIdList = new ArrayList<Long>();
			for (Personel personel : donemPerList) {
				if (PdksUtil.hasStringValue(sicilNo) == false || ortakIslemler.isStringEqual(sicilYeniNo, personel.getPdksSicilNo())) {
					if (PdksUtil.hasStringValue(sicilNo) && personel.getPdksSicilNo().endsWith(sicilYeniNo))
						setSicilNo(personel.getPdksSicilNo());
					perIdList.add(personel.getId());
				}

			}
			if (loginUser.getDepartman().isAdminMi() == false && (loginUser.isSuperVisor() || loginUser.isProjeMuduru())) {
				sirket = loginUser.getPdksPersonel().getSirket();
			}
			if (sirketId != null && (ikRole)) {

				sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);

			}

			if (sirket != null)
				departmanBolumAyni = sirket.isTesisDurumu() == false;

			String searchKey = "sirket.id=";
			if (sirket == null)
				if (!loginUser.isIK() && !loginUser.isAdmin())
					sirket = loginUser.getPdksPersonel().getSirket();
			if (perList != null) {
				searchKey = "pdksSicilNo";
				if (perList.isEmpty())
					perList.add("YOKTUR");
			}

			List<PersonelDenklestirme> personelDenklestirmeler = null;
			if (!perIdList.isEmpty()) {
				personelDenklestirmeler = getPdksPersonelDenklestirmeler(perIdList);
				if (personelDenklestirmeler.isEmpty() && denklestirmeAyDurum && hataliPuantajGoster != null && hataliPuantajGoster) {
					hataliPuantajGoster = false;
					personelDenklestirmeler = getPdksPersonelDenklestirmeler(perIdList);
					if (!personelDenklestirmeler.isEmpty())
						PdksUtil.addMessageInfo("Hatalı personel puantajı bulunmadı.");
				}
			}

			else
				personelDenklestirmeler = new ArrayList<PersonelDenklestirme>();

			HashMap<Long, Personel> gorevliPersonelMap = new HashMap<Long, Personel>();
			if (seciliEkSaha3Id != null) {
				List<Long> gorevYerileri = new ArrayList<Long>();
				gorevYerileri.add(seciliEkSaha3Id);
				List<VardiyaGorev> gorevliler = departman == null || departman.isAdminMi() ? ortakIslemler.getVardiyaGorevYerleri(loginUser, aylikPuantajSablon.getIlkGun(), aylikPuantajSablon.getSonGun(), gorevYerileri, session) : new ArrayList<VardiyaGorev>();
				for (VardiyaGorev vardiyaGorev : gorevliler) {
					Personel personel = vardiyaGorev.getVardiyaGun().getPersonel();
					String perNo = personel.getPdksSicilNo();
					if (PdksUtil.hasStringValue(perNo) == false)
						continue;
					perNo = perNo.trim();
					if (!perList.contains(perNo) && (PdksUtil.hasStringValue(sicilNo) == false || sicilNo.equals(perNo)))
						gorevliPersonelMap.put(personel.getId(), personel);
				}

				if (!gorevliPersonelMap.isEmpty()) {
					List<PersonelDenklestirme> personelHelpDenklestirmeler = getPdksPersonelDenklestirmeler(new ArrayList(gorevliPersonelMap.keySet()));
					if (!personelHelpDenklestirmeler.isEmpty())
						personelDenklestirmeler.addAll(personelHelpDenklestirmeler);
				}

			}
			if (testDurum)
				logger.info("fillPersonelDenklestirmeDevam 2000 " + basTarih);

			HashMap<Long, PersonelDenklestirme> personelDenklestirmeMap = new HashMap<Long, PersonelDenklestirme>();
			TreeMap<Long, PersonelDenklestirme> personelDenklestirmeDonemMap = new TreeMap<Long, PersonelDenklestirme>();
			if (personelDenklestirmeler.isEmpty()) {
				perList.clear();
				if (userLogin.getLogin())
					PdksUtil.addMessageWarn("Çalışma planı kaydı bulunmadı!");

			}
			perList.clear();

			for (Iterator iterator = personelDenklestirmeler.iterator(); iterator.hasNext();) {
				PersonelDenklestirme personelDenklestirme = (PersonelDenklestirme) iterator.next();
				if (personelDenklestirme == null || personelDenklestirme.getPersonel() == null) {
					iterator.remove();
					continue;
				}
				personelDenklestirmeDonemMap.put(personelDenklestirme.getPersonelId(), personelDenklestirme);
				personelDenklestirme.setGuncellendi(personelDenklestirme.getId() == null);
				if (personelDenklestirme.isDenklestirmeDurum() || sadeceFazlaMesai == false) {
					personelDenklestirmeMap.put(personelDenklestirme.getPersonelId(), personelDenklestirme);
					perList.add(personelDenklestirme.getPersonel().getPdksSicilNo());
				} else
					iterator.remove();

			}
			Date sonCikisZamani = null;
			Date gunBas = PdksUtil.getDate(bugun);
			Calendar cal = Calendar.getInstance();
			if (seciliEkSaha3Id != null && PdksUtil.isSistemDestekVar()) {

				seciliBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha3Id, Tanim.class, session);

			}
			if (seciliEkSaha4Id != null && PdksUtil.isSistemDestekVar()) {
				seciliAltBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha4Id, Tanim.class, session);

			}
			if (!perList.isEmpty()) {
				if (sirket != null && denklestirmeAyDurum && personelIzinGirisiDurum) {
					map.clear();
					StringBuffer sb = new StringBuffer();
					sb.append("select * from " + IzinTipi.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
					sb.append(" where " + IzinTipi.COLUMN_NAME_DURUM + " = 1 and " + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + "  is null ");
					sb.append(" and " + IzinTipi.COLUMN_NAME_DEPARTMAN + " = :d and " + IzinTipi.COLUMN_NAME_GIRIS_TIPI + " <> :g ");
					map.put("d", sirket.getDepartman().getId());

					map.put("g", IzinTipi.GIRIS_TIPI_YOK);

					if (session != null)
						map.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<IzinTipi> izinTipiList = pdksEntityController.getObjectBySQLList(sb, map, IzinTipi.class);

					sirketIzinGirisDurum = !izinTipiList.isEmpty();
				}
				fazlaMesaiMap = ortakIslemler.getFazlaMesaiMap(session);
				if (kullaniciCalistir) {
					Map<String, String> requestHeaderMap = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
					adres = requestHeaderMap.containsKey("host") ? requestHeaderMap.get("host") : "";
				} else
					adres = "";

				sabahVardiyalar = null;
				String sabahVardiyaKisaAdlari = ortakIslemler.getParameterKey("sabahVardiyaKisaAdlari");
				if (PdksUtil.hasStringValue(sabahVardiyaKisaAdlari))
					sabahVardiyalar = PdksUtil.getListByString(sabahVardiyaKisaAdlari, null);
				else
					sabahVardiyalar = Arrays.asList(new String[] { "S", "Sİ", "SI" });
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 3000 " + basTarih);
				devamlilikPrimIzinTipleri = PdksUtil.getListByString(ortakIslemler.getParameterKey("devamlilikPrimIzinTipleri"), null);
				String gunduzVardiyaVar = ortakIslemler.getParameterKey("gunduzVardiyaVar");
				if (gunduzVardiyaVar.equals("1")) {
					sabahVardiya = ortakIslemler.getSabahVardiya(sabahVardiyalar, departmanId, session);
				} else
					sabahVardiya = null;
				setInstance(denklestirmeDonemi);
				map.clear();

				List<Personel> perListesi = pdksEntityController.getSQLParamByFieldList(Personel.TABLE_NAME, Personel.COLUMN_NAME_PDKS_SICIL_NO, perList, Personel.class, session);

				TreeMap<String, Tatil> tatilGunleriMap = ortakIslemler.getTatilGunleri(perListesi, ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBaslangicTarih(), -1), ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBitisTarih(), 1), session);
				boolean ayBitmedi = denklestirmeDonemi.getBitisTarih().getTime() >= PdksUtil.getDate(bugun).getTime();
				List<PersonelDenklestirmeTasiyici> list = null;
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 4000 " + PdksUtil.getCurrentTimeStampStr());
				try {
					denklestirmeDonemi.setPersonelDenklestirmeDonemMap(personelDenklestirmeDonemMap);
					denklestirmeDonemi.setDenklestirmeAyDurum(denklestirmeAyDurum);
					list = ortakIslemler.personelDenklestir(denklestirmeDonemi, tatilGunleriMap, searchKey, perList, Boolean.TRUE, Boolean.FALSE, ayBitmedi, session);
					if (list.isEmpty()) {
						sessionFlush();
						sessionClear();
						denklestirmeDonemi.setDurum(Boolean.FALSE);
						tatilGunleriMap = ortakIslemler.getTatilGunleri(perListesi, ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBaslangicTarih(), -1), ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBitisTarih(), 1), session);
						list = ortakIslemler.personelDenklestir(denklestirmeDonemi, tatilGunleriMap, searchKey, perList, Boolean.TRUE, Boolean.FALSE, ayBitmedi, session);

					}

				} catch (Exception ex) {
					list = new ArrayList<PersonelDenklestirmeTasiyici>();
					ortakIslemler.loggerErrorYaz(sayfaURL, ex);
				}
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 5000 " + PdksUtil.getCurrentTimeStampStr());
				if (!list.isEmpty()) {

					if (list.size() > 1)
						list = PdksUtil.sortObjectStringAlanList(list, "getAdSoyad", null);
				}

				boolean renk = Boolean.TRUE;
				aylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, denklestirmeDonemi, session);

				List<VardiyaHafta> vardiyaHaftaList = new ArrayList<VardiyaHafta>();
				fazlaMesaiOrtakIslemler.haftalikVardiyaOlustur(vardiyaHaftaList, aylikPuantajSablon, denklestirmeDonemi, tatilGunleriMap, null);
				if (denklestirmeAyDurum && vardiyaHaftaList != null && vardiyaHaftaList.size() > 4) {
					Date bugun = PdksUtil.getDate(new Date());
					String donem = String.valueOf(denklestirmeAy.getDonem());
					for (VardiyaHafta sonVardiyaHafta : vardiyaHaftaList) {
						if (bugun.after(sonVardiyaHafta.getBasTarih()) && bugun.before(sonVardiyaHafta.getBitTarih())) {
							if (sonVardiyaHafta.getVardiyaGunler() != null) {
								boolean haftaIci = false;
								for (VardiyaGun vg : sonVardiyaHafta.getVardiyaGunler()) {
									if (haftaIci) {
										if (!vg.getVardiyaDateStr().startsWith(donem))
											sonHafta = true;
									} else if (vg.getVardiyaDate().getTime() == bugun.getTime())
										haftaIci = true;
								}
							}
							if (sonHafta)
								break;
						}
					}
				}
				resmiTatilVar = Boolean.FALSE;
				haftaTatilVar = Boolean.FALSE;
				TreeMap<String, PersonelDenklestirmeTasiyici> perMap = new TreeMap<String, PersonelDenklestirmeTasiyici>();

				List<Long> kontratliPerIdList = new ArrayList<Long>();
				TreeMap<Long, Long> sirketIdMap = new TreeMap<Long, Long>(), sirketMap = new TreeMap<Long, Long>();
				for (PersonelDenklestirmeTasiyici personelDenklestirme : list) {
					Personel personel = personelDenklestirme.getPersonel();
					if (personel.getPdksYonetici() != null)
						sirketIdMap.put(personel.getPdksYonetici().getId(), personel.getId());
					if (personel.getSirket() != null)
						sirketMap.put(personel.getSirket().getId(), personel.getId());
					String key = (personel.getEkSaha3() != null ? personel.getEkSaha3().getKodu() : "");
					key += "_" + (personel.getBordroAltAlan() != null ? personel.getBordroAltAlan().getKodu() : "");
					key += "_" + personel.getPdksSicilNo() + "_" + personel.getId();
					perMap.put(key, personelDenklestirme);
				}

				list = null;
				list = new ArrayList<PersonelDenklestirmeTasiyici>(perMap.values());
				departmanBolumAyni = sirketMap.size() > 1;
				if (!list.isEmpty()) {
					kontratliPerIdList.clear();
					for (Iterator iterator = list.iterator(); iterator.hasNext();) {
						PersonelDenklestirmeTasiyici personelDenklestirme = (PersonelDenklestirmeTasiyici) iterator.next();
						Personel personel = personelDenklestirme.getPersonel();
						kontratliPerIdList.add(personel.getId());
						perMap.put(String.valueOf(personel.getId()), personelDenklestirme);
						iterator.remove();
					}
					List<Personel> personelList = ortakIslemler.getKontratliSiraliPersonel(kontratliPerIdList, session);
					if (sirketIdMap.size() > 0 && sirketIdMap.size() < personelList.size()) {
						List<Personel> perDigerList = new ArrayList<Personel>();
						for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
							Personel personel = (Personel) iterator.next();
							if (!sirketIdMap.containsKey(personel.getId())) {
								perDigerList.add(personel);
								iterator.remove();
							}
						}
						if (!perDigerList.isEmpty())
							personelList.addAll(perDigerList);
						perDigerList = null;
					}
					sirketIdMap = null;
					for (Personel personel : personelList)
						list.add(perMap.get(String.valueOf(personel.getId())));
					personelList = null;
				}
				sirketIdMap = null;
				kontratliPerIdList = null;
				perMap = null;
				boolean flush = Boolean.FALSE;
				List<String> gunList = new ArrayList<String>();
				for (Iterator iterator = aylikPuantajDefault.getAyinVardiyalari().iterator(); iterator.hasNext();) {
					VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
					gunList.add(vardiyaGun.getVardiyaDateStr());
				}
				personelIzinGirisiStr = ortakIslemler.getCalistiMenuAdi("personelIzinGirisi");
				personelHareketStr = ortakIslemler.getCalistiMenuAdi("personelHareket");
				personelFazlaMesaiOrjStr = ortakIslemler.getCalistiMenuAdi("personelFazlaMesai");
				vardiyaPlaniStr = ortakIslemler.getCalistiMenuAdi("vardiyaPlani");
				onayla = Boolean.FALSE;

				HashMap<String, Object> paramsMap = new HashMap<String, Object>();
				List saveList = new ArrayList();
				msgError = ortakIslemler.getParameterKey("msgErrorResim");
				if (!PdksUtil.hasStringValue(msgError))
					msgError = "msgerror.png";
				msgFazlaMesaiError = ortakIslemler.getParameterKey("msgFazlaMesaiErrorResim");
				if (!PdksUtil.hasStringValue(msgFazlaMesaiError))
					msgFazlaMesaiError = "msgerror.png";
				List<Long> vgIdList = new ArrayList<Long>();
				ayrikHareketVar = false;
				String str = ortakIslemler.getParameterKey("addManuelGirisCikisHareketler");
				boolean ayrikKontrol = false;
				if (PdksUtil.hasStringValue(sicilNo)) {
					ayrikKontrol = str.equals("A") || str.equals("1");
					if (!ayrikKontrol) {
						if (loginUser.isAdmin())
							ayrikKontrol = str.equalsIgnoreCase("I") || str.equalsIgnoreCase("S");
						else if (loginUser.isIK())
							ayrikKontrol = str.equalsIgnoreCase("I");

					}
				}
				boolean uyariHaftaTatilMesai = false;

				List<Long> denklestirmeIdList = new ArrayList<Long>();
				List<PersonelDenklestirmeTasiyici> haftaSonuList = new ArrayList<PersonelDenklestirmeTasiyici>();
				List<VardiyaGun> bosCalismaList = new ArrayList<VardiyaGun>();
				for (Iterator iterator1 = list.iterator(); iterator1.hasNext();) {
					PersonelDenklestirmeTasiyici denklestirmeTasiyici = (PersonelDenklestirmeTasiyici) iterator1.next();
					if (personelDenklestirmeMap.containsKey(denklestirmeTasiyici.getPersonel().getId())) {
						PersonelDenklestirme personelDenklestirme = personelDenklestirmeMap.get(denklestirmeTasiyici.getPersonel().getId());
						boolean hareketKaydiVardiyaBulsunmu = personelDenklestirme.getCalismaModeliAy().isHareketKaydiVardiyaBulsunmu();
						if (hareketKaydiVardiyaBulsunmu) {
							// if (haftaTatilDurum.equals("1"))
							haftaSonuList.add(denklestirmeTasiyici);
							TreeMap<String, VardiyaGun> vardiyaGunleriMap = denklestirmeTasiyici.getVardiyaGunleriMap();
							if (vardiyaGunleriMap != null) {
								for (String key : vardiyaGunleriMap.keySet()) {
									VardiyaGun vardiyaGun = vardiyaGunleriMap.get(key);
									if (vardiyaGun.isAyinGunu() && vardiyaGun.getVardiya() != null && vardiyaGun.getVardiya().isCalisma() && vardiyaGun.getVersion() < 0) {
										if (vardiyaGun.getIslemVardiya().getVardiyaBitZaman().before(bugun) && vardiyaGun.getCalismaSuresi() == 0.0d && vardiyaGun.isIzinli() == false && vardiyaGun.getHareketler() == null)
											bosCalismaList.add(vardiyaGun);
									}
								}
							}
						}

						denklestirmeIdList.add(personelDenklestirme.getId());
					}
				}
				if (!ortakIslemler.getParameterKey("bosCalismaOffGuncelle").equals("1"))
					bosCalismaList.clear();
				if (denklestirmeAyDurum && (bosCalismaList.size() + haftaSonuList.size()) > 0) {
					if (!haftaSonuList.isEmpty())
						haftaTatilVardiyaGuncelle(haftaSonuList);
					if (!bosCalismaList.isEmpty())
						bosCalismaOffGuncelle(bosCalismaList, haftaSonuList.isEmpty());
				}
				bosCalismaList = null;
				haftaSonuList = null;

				Date izinCalismayanMailSonGun = ortakIslemler.tariheGunEkleCikar(cal, aylikPuantajSablon.getSonGun(), -5);
				izinCalismayanMailGonder = bugun.after(izinCalismayanMailSonGun) || loginUser.isAdmin();
				List<AylikPuantaj> puantajDenklestirmeList = new ArrayList<AylikPuantaj>();
				aylikPuantajSablon.setGebeDurum(false);
				aylikPuantajSablon.setSuaDurum(false);
				aylikPuantajSablon.setIsAramaDurum(false);
				for (Iterator iterator1 = list.iterator(); iterator1.hasNext();) {
					PersonelDenklestirmeTasiyici denklestirmeTasiyici = (PersonelDenklestirmeTasiyici) iterator1.next();
					AylikPuantaj puantaj = (AylikPuantaj) aylikPuantajSablon.clone();
					puantaj.setPersonelDenklestirme(personelDenklestirmeMap.get(denklestirmeTasiyici.getPersonel().getId()));
					PersonelDenklestirme personelDenklestirme = puantaj.getPersonelDenklestirme();
					if (personelDenklestirme == null || !(personelDenklestirme.isDenklestirmeDurum() || sadeceFazlaMesai == false)) {
						iterator1.remove();
						continue;
					}
					puantaj.setPersonelDenklestirmeTasiyici(denklestirmeTasiyici);
					puantaj.setPdksPersonel(denklestirmeTasiyici.getPersonel());
					puantajDenklestirmeList.add(puantaj);
				}
				denklestirmeDinamikAlanlar = ortakIslemler.setDenklestirmeDinamikDurum(puantajDenklestirmeList, session);
				if (!denklestirmeDinamikAlanlar.isEmpty()) {
					for (Iterator iterator = denklestirmeDinamikAlanlar.iterator(); iterator.hasNext();) {
						Tanim tanim = (Tanim) iterator.next();
						if (tanim.getKodu().equals(PersonelDenklestirmeDinamikAlan.TIPI_DEVAMLILIK_PRIMI))
							devamlilikPrimi = tanim;

					}
				}
				if (devamlilikPrimi == null)
					devamlilikPrimi = denklestirmeMantiksalBilgiBul(PersonelDenklestirmeDinamikAlan.TIPI_DEVAMLILIK_PRIMI);

				String yoneticiPuantajKontrolStr = ortakIslemler.getParameterKey("yoneticiPuantajKontrol");
				boolean yoneticiKontrolEtme = loginUser.isAdmin() || loginUser.isSistemYoneticisi() || PdksUtil.hasStringValue(yoneticiPuantajKontrolStr) == false;
				if (!yoneticiKontrolEtme)
					yoneticiKontrolEtme = yoneticiRolVarmi;
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 6000 " + PdksUtil.getCurrentTimeStampStr());

				ortakIslemler.yoneticiPuantajKontrol(loginUser, puantajDenklestirmeList, Boolean.TRUE, session);
				boolean kayitVar = false;
				aksamCalismaSaati = null;
				aksamCalismaSaatiYuzde = null;
				try {
					if (ortakIslemler.getParameterKeyHasStringValue("aksamCalismaSaatiYuzde"))
						aksamCalismaSaatiYuzde = Double.parseDouble(ortakIslemler.getParameterKey("aksamCalismaSaatiYuzde"));

				} catch (Exception e) {
				}
				if (aksamCalismaSaatiYuzde != null && (aksamCalismaSaatiYuzde.doubleValue() < 0.0d || aksamCalismaSaatiYuzde.doubleValue() > 100.0d))
					aksamCalismaSaatiYuzde = null;
				try {
					if (ortakIslemler.getParameterKeyHasStringValue("aksamCalismaSaati"))
						aksamCalismaSaati = Double.parseDouble(ortakIslemler.getParameterKey("aksamCalismaSaati"));

				} catch (Exception e) {
				}
				if (aksamCalismaSaati == null)
					aksamCalismaSaati = 4.0d;
				HashMap<Long, Boolean> personelDurumMap = getPersonelDurumMap(aylikPuantajSablon, puantajDenklestirmeList);
				String denklesmeyenBakiyeDurum = denklestirmeAyDurum ? ortakIslemler.getParameterKey("denklesmeyenBakiyeDurum") : "";
				String izinCalismaUyariDurum = denklestirmeAyDurum ? ortakIslemler.getParameterKey("izinCalismaUyariDurum") : "";
				Date sonGun = ortakIslemler.tariheGunEkleCikar(cal, aylikPuantajSablon.getSonGun(), 1);
				if (kullaniciCalistir) {
					personelHareketDurum = userHome.hasPermission("personelHareket", "view");
					personelFazlaMesaiDurum = userHome.hasPermission("personelFazlaMesai", "view");
					vardiyaPlaniDurum = userHome.hasPermission("vardiyaPlani", "view");
					personelIzinGirisiDurum = userHome.hasPermission("personelIzinGirisi", "view");
				}
				boolean denklestirilmeyenDevredenVar = Boolean.FALSE;
				String donemStr = String.valueOf(denklestirmeAy.getYil() * 100 + denklestirmeAy.getAy());
				fazlaMesaiTalepOnayliDurum = Boolean.FALSE;
				if (personelFazlaMesaiDurum && denklestirmeAyDurum && ortakIslemler.getParameterKey("fazlaMesaiTalepDurum").equals("1")) {
					msgFazlaMesaiInfo = ortakIslemler.getParameterKey("fazlaMesaiTalepOnayli");
					fazlaMesaiTalepOnayliDurum = PdksUtil.hasStringValue(msgFazlaMesaiInfo);
				}
				LinkedHashMap<Long, PersonelIzin> izinMap = new LinkedHashMap<Long, PersonelIzin>();
				List<VardiyaGun> offIzinliGunler = new ArrayList<VardiyaGun>();
				kismiOdemeGoster = Boolean.FALSE;
				manuelGirisGoster = "";
				kapiGirisSistemAdi = "";
				String eksikCalismaGosterStr = ortakIslemler.getParameterKey("eksikCalismaGoster");
				eksikCalismaGoster = loginUser.isAdmin() || eksikCalismaGosterStr.equals("1") || (adminRole && eksikCalismaGosterStr.equalsIgnoreCase("ik"));
				if (ikRole || adminRole) {
					manuelGirisGoster = ortakIslemler.getParameterKey("manuelGirisGoster");
					if (PdksUtil.hasStringValue(manuelGirisGoster) == false && loginUser.isAdmin())
						manuelGirisGoster = "background-color: yellow;font-style: italic !important;";
					kapiGirisSistemAdi = !PdksUtil.hasStringValue(manuelGirisGoster) ? "" : ortakIslemler.getParameterKey("kapiGirisSistemAdi");
				}
				boolean yoneticiTanimli = !ortakIslemler.getParameterKeyHasStringValue(("yoneticiTanimsiz"));
				String idariVardiyaKisaAdi = ortakIslemler.getParameterKey("idariVardiyaKisaAdi");
				Vardiya normalCalismaVardiya = ortakIslemler.getNormalCalismaVardiya(idariVardiyaKisaAdi, session);
				List<Long> devamsizList = new ArrayList<Long>();
				if (devamlilikPrimi != null) {
					List<Long> idList = new ArrayList<Long>();
					for (Iterator iterator1 = puantajDenklestirmeList.iterator(); iterator1.hasNext();) {
						AylikPuantaj puantaj = (AylikPuantaj) iterator1.next();
						if (puantaj.getPersonelDenklestirme() != null) {
							if (puantaj.getDinamikAlanMap() != null && puantaj.getDinamikAlanMap().containsKey(devamlilikPrimi.getId())) {
								PersonelDenklestirme personelDenklestirme = puantaj.getPersonelDenklestirme();

								idList.add(personelDenklestirme.getPersonelId());

							}
						}
					}
					if (!idList.isEmpty()) {

						List<VardiyaGun> vgList = ortakIslemler.getPersonelEksikVardiyaCalismaList(idList, aylikPuantajSablon.getIlkGun(), aylikPuantajSablon.getSonGun(), session);
						if (vgList != null) {
							for (VardiyaGun vardiyaGun : vgList) {
								Long perId = vardiyaGun.getPersonel().getId();
								if (!devamsizList.contains(perId))
									devamsizList.add(perId);
							}
						}
					}

				}
				double fazlaMesaiMaxSure = ortakIslemler.getFazlaMesaiMaxSure(denklestirmeAy);
				boolean sirketFazlaMesaiOde = sirket.getFazlaMesaiOde() != null && sirket.getFazlaMesaiOde();
				Date yeniDonem = PdksUtil.tariheAyEkleCikar(PdksUtil.convertToJavaDate((yil * 100 + ay) + "01", "yyyyMMdd"), 1);
				boolean yoneticiZorunluDegil = ortakIslemler.getParameterKey("yoneticiZorunluDegil").equals("1");
				istifaGoster = false;
				aylikPuantajList.clear();
				List<HareketKGS> gecersizHareketler = new ArrayList<HareketKGS>();
				HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(null, session);
				KapiView manuelGiris = manuelKapiMap.get(Kapi.TIPI_KODU_GIRIS);
				KapiView manuelCikis = manuelKapiMap.get(Kapi.TIPI_KODU_CIKIS);
				for (Iterator iterator1 = puantajDenklestirmeList.iterator(); iterator1.hasNext();) {
					AylikPuantaj puantaj = (AylikPuantaj) iterator1.next();
					puantaj.setFazlaMesaiHesapla(true);
					puantaj.setYoneticiZorunlu(true);
					if (denklestirmeAyDurum == false || yoneticiZorunluDegil)
						puantaj.setYoneticiZorunlu(false);
					double negatifBakiyeDenkSaat = 0.0;
					offIzinliGunler.clear();
					puantaj.setEksikGunVar(false);
					PersonelDenklestirme personelDenklestirme = null;
					puantaj.setDonemBitti(Boolean.FALSE);
					puantaj.setAyrikHareketVar(false);
					puantaj.setFiiliHesapla(true);
					saveList.clear();
					Personel personel = puantaj.getPdksPersonel();
					perCalismaModeli = personel.getCalismaModeli();
					if (puantaj.getPersonelDenklestirme() != null && puantaj.getPersonelDenklestirme().getCalismaModeliAy() != null)
						perCalismaModeli = puantaj.getPersonelDenklestirme().getCalismaModeli();

					Boolean tarihGecti = Boolean.TRUE;
					Boolean gebemi = Boolean.FALSE, calisiyor = Boolean.FALSE;
					puantaj.setKaydet(Boolean.FALSE);

					puantaj.setCalisiyor(personel.isCalisiyorGun(yeniDonem));
					if (istifaGoster == false)
						istifaGoster = puantaj.isCalisiyor() == false;

					personelFazlaMesaiStr = personelFazlaMesaiOrjStr;
					puantaj.setSablonAylikPuantaj(aylikPuantajSablon);
					puantaj.setFazlaMesaiHesapla(Boolean.FALSE);
					CalismaModeli calismaModeli = puantaj.getCalismaModeli();
					puantaj.setTrClass(renk ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
					renk = !renk;
					Integer aksamVardiyaSayisi = 0;
					Double aksamVardiyaSaatSayisi = 0d, sabahAksamCikisSaatSayisi = 0d, haftaCalismaSuresi = 0d, resmiTatilSuresi = 0d, offSure = null;
					if (stajerSirket && denklestirmeAyDurum) {
						puantaj.planSureHesapla(tatilGunleriMap);
						offSure = 0.0D;
					}
					TreeMap<String, VardiyaGun> vardiyalar = new TreeMap<String, VardiyaGun>();
					Boolean fazlaMesaiHesapla = Boolean.FALSE;
					cal = Calendar.getInstance();
					puantaj.setHareketler(null);
					List<String> ayrikList = new ArrayList<String>();
					Date sonVardiyaBitZaman = null;
					boolean fazlaMesaiOnayla = false;
					int gunAdet = 0;
					boolean personelCalisiyor = false;
					if (puantaj.getVardiyalar() != null) {
						for (VardiyaGun vardiyaGun : puantaj.getVardiyalar()) {
							vardiyaGun.setAyinGunu(vardiyaGun.getVardiyaDateStr().startsWith(donemStr));
							if (vardiyaGun.isAyinGunu() == false || vardiyaGun.getVardiya() == null)
								continue;
							gunAdet++;
							if ((vardiyaGun.getHareketler() == null || vardiyaGun.getHareketler().isEmpty()) && (vardiyaGun.isIzinli() || vardiyaGun.getVardiya().isCalisma() == false))
								continue;
							puantaj.setSonGun(vardiyaGun.getVardiyaDate());
							Vardiya islemVardiya = vardiyaGun.getIslemVardiya();
							if (sonVardiyaBitZaman == null || islemVardiya.getVardiyaTelorans1BitZaman().after(sonVardiyaBitZaman))
								sonVardiyaBitZaman = islemVardiya.getVardiyaTelorans1BitZaman();
						}
						personelDenklestirme = puantaj.getPersonelDenklestirme();
						personelCalisiyor = personelDenklestirme.getPersonel().isCalisiyorGun(sonGun);
						planOnayDurum = denklestirmeAyDurum && (personelDenklestirme.isOnaylandi());
						if (personelDenklestirme.getDurum()) {
							if (sonVardiyaBitZaman != null)
								fazlaMesaiOnayla = bugun.after(sonVardiyaBitZaman);
						}
						negatifBakiyeDenkSaat = personelDenklestirme.getCalismaModeliAy() != null ? personelDenklestirme.getCalismaModeliAy().getNegatifBakiyeDenkSaat() : 0.0d;
						boolean ekle = (denklestirmeAyDurum || (bakiyeGuncelle != null && bakiyeGuncelle));
						fazlaMesaiHesapla = personelDenklestirme.isDenklestirmeDurum();

						boolean cumartesiCalisiyor = calismaModeli != null && calismaModeli.isHaftaTatilVar();
						HashMap<Long, List<VardiyaGun>> bosGunMap = new HashMap<Long, List<VardiyaGun>>();
						if (denklestirmeAyDurum && !haftaTatilDurum.equals("1")) {
							TreeMap<String, VardiyaGun> vgMap = new TreeMap<String, VardiyaGun>();
							for (Iterator iterator = puantaj.getVardiyalar().iterator(); iterator.hasNext();) {
								VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
								if (vardiyaGun.isAyinGunu() && vardiyaGun.getIzin() == null && vardiyaGun.getVardiya() != null && vardiyaGun.getVardiya().getId() != null)
									vgMap.put(vardiyaGun.getVardiyaDateStr(), vardiyaGun);
							}
							for (VardiyaHafta vardiyaHafta : puantaj.getVardiyaHaftaList()) {
								List<VardiyaGun> vardiyaGunList = new ArrayList<VardiyaGun>();
								VardiyaGun vardiyaTatil = null;
								for (VardiyaGun pVardiyaGun : vardiyaHafta.getVardiyaGunler()) {
									if (vgMap.containsKey(pVardiyaGun.getVardiyaDateStr())) {
										VardiyaGun vardiyaGun = vgMap.get(pVardiyaGun.getVardiyaDateStr());
										if (vardiyaGun.getVardiya().isHaftaTatil())
											vardiyaTatil = vardiyaGun;
										else if (vardiyaGun.getVersion() < 0) {
											if (vardiyaGun.getHareketler() == null || vardiyaGun.getHareketler().isEmpty())
												vardiyaGunList.add(vardiyaGun);
										}
									}
								}
								if (vardiyaTatil != null && !vardiyaGunList.isEmpty())
									bosGunMap.put(vardiyaTatil.getId(), vardiyaGunList);
								else
									vardiyaGunList = null;
							}
							vgMap = null;
						}

						for (Iterator iterator = puantaj.getVardiyalar().iterator(); iterator.hasNext();) {
							VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
							vardiyaGun.setAyinGunu(gunList.contains(vardiyaGun.getVardiyaDateStr()));
							if (!vardiyaGun.isAyinGunu()) {
								iterator.remove();
								continue;
							}
							if (vardiyaGun.getId() != null)
								vgIdList.add(vardiyaGun.getId());
							vardiyaGun.setStyle("");

							boolean saatEkle = false;
							vardiyaGun.addResmiTatilSure(vardiyaGun.getGecenAyResmiTatilSure());
							if (vardiyaGun.getPersonel().isCalisiyorGun(vardiyaGun.getVardiyaDate())) {
								if (vardiyaGun.getIslemVardiya().getVardiyaTelorans2BitZaman() != null)
									vardiyaGun.setZamanGelmedi(vardiyaGun.getSonrakiVardiyaGun() != null && !bugun.after(vardiyaGun.getIslemVardiya().getVardiyaTelorans2BitZaman()));
								else
									logger.debug("");
							}

							if (ekle && vardiyaGun.getHareketDurum() && vardiyaGun.getId() != null && vardiyaGun.getIslemVardiya() != null) {
								saatEkle = vardiyaGun.getVardiyaDate().before(gunBas);
								if (vardiyaGun.getIslemVardiya().isCalisma()) {
									if (!saatEkle)
										saatEkle = vardiyaGun.getTatil() != null || vardiyaGun.getIslemVardiya().getVardiyaBitZaman().before(bugun);

								}

							}

							if (offSure != null && vardiyaGun.getVardiya() != null && vardiyaGun.getIzin() == null && vardiyaGun.getVardiya().isOffGun()) {
								cal.setTime(vardiyaGun.getVardiyaDate());
								int haftaGunu = cal.get(Calendar.DAY_OF_WEEK);
								if (haftaGunu != Calendar.SATURDAY && haftaGunu != Calendar.SUNDAY)
									offSure += 9;

							}
							if (personel.getSskCikisTarihi().before(puantaj.getSonGun()) || puantaj.getSonGun().before(bugun)) {
								if (denklestirmeAyDurum && vardiyaGun.getVardiya() != null && vardiyaGun.isZamanGelmedi()) {
									// hataYok = Boolean.FALSE;
									puantaj.setDonemBitti(Boolean.FALSE);
								}
							}

							vardiyaGun.setLinkAdresler(null);
							vardiyaGun.setOnayli(Boolean.TRUE);
							vardiyaGun.setHataliDurum(Boolean.FALSE);
							vardiyaGun.setPersonel(puantaj.getPdksPersonel());
							vardiyaGun.setCalismaModeli(puantaj.getCalismaModeli());
							vardiyaGun.setFiiliHesapla(fazlaMesaiHesapla);

							if (vardiyaGun.getVardiya() != null && vardiyaGun.getVardiyaDate().getTime() >= puantaj.getIlkGun().getTime() && vardiyaGun.getVardiyaDate().getTime() <= puantaj.getSonGun().getTime()) {
								paramsMap.put("fazlaMesaiHesapla", fazlaMesaiHesapla);
								paramsMap.put("aksamVardiyaSayisi", aksamVardiyaSayisi);
								paramsMap.put("aksamVardiyaSaatSayisi", aksamVardiyaSaatSayisi);
								paramsMap.put("resmiTatilSuresi", resmiTatilSuresi);
								paramsMap.put("haftaCalismaSuresi", haftaCalismaSuresi);
								paramsMap.put("sabahAksamCikisSaatSayisi", sabahAksamCikisSaatSayisi);
								vardiyaGun.setFazlaMesaiTalepOnayliDurum(Boolean.FALSE);

								vardiyaGunKontrol(puantaj, vardiyaGun, paramsMap);
								if (vardiyaGun.getGecersizHareketler() != null) {
									gecersizHareketler.addAll(vardiyaGun.getGecersizHareketler());
									vardiyaGun.setGecersizHareketler(null);
								}

								if (izinCalismaUyariDurum.equals("1") && vardiyaGun.getIzin() != null) {
									PersonelIzin izin = vardiyaGun.getIzin();
									IzinTipi it = izin.getIzinTipi();
									if (vardiyaGun.isHareketHatali()) {
										if (ikRole && izin.getOrjIzin() != null) {
											Long izinId = izin.getOrjIzin().getId();
											PersonelIzin orjIzin = izinMap.containsKey(izinId) ? izinMap.get(izinId) : izin.getOrjIzin();
											if (!izinMap.containsKey(izinId)) {
												orjIzin.setCalisilanGunler(null);
												izinMap.put(izinId, orjIzin);
											}
											orjIzin.addCalisilanGunler(vardiyaGun);

										}
									} else if (vardiyaGun.getTatil() == null && vardiyaGun.getVardiya().isOffGun() && it.isRaporIzin() == false) {
										if (it.getTakvimGunumu() == null || it.getTakvimGunumu().equals(Boolean.FALSE)) {
											if (vardiyaGun.isHaftaIci() || cumartesiCalisiyor) {
												vardiyaGun.setStyle("color:red;");
												offIzinliGunler.add(vardiyaGun);
											}
										}

									}
								}
								if (vardiyaGun.isAyrikHareketVar()) {
									ayrikList.add(PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "d MMMMM EEEEEE"));
									VardiyaGun sonrakiVardiyaGun = vardiyaGun.getSonrakiVardiyaGun();
									if (sonrakiVardiyaGun != null && sonrakiVardiyaGun.isAyinGunu() == false && sonrakiVardiyaGun.isAyrikHareketVar())
										ayrikList.add(PdksUtil.convertToDateString(vardiyaGun.getSonrakiVardiyaGun().getVardiyaDate(), "d MMMMM EEEEEE"));
								}
								fazlaMesaiHesapla = (Boolean) paramsMap.get("fazlaMesaiHesapla");
								aksamVardiyaSayisi = (Integer) paramsMap.get("aksamVardiyaSayisi");
								resmiTatilSuresi = (Double) paramsMap.get("resmiTatilSuresi");
								aksamVardiyaSaatSayisi = (Double) paramsMap.get("aksamVardiyaSaatSayisi");
								sabahAksamCikisSaatSayisi = (Double) paramsMap.get("sabahAksamCikisSaatSayisi");
								haftaCalismaSuresi = (Double) paramsMap.get("haftaCalismaSuresi");
								paramsMap.clear();
							}
							Boolean hareketDurum = vardiyaGun.getDurum(), saveVardiyaGun = Boolean.FALSE;
							hareketDurum = vardiyaGun.getHareketDurum() && saatEkle;
							Vardiya vardiya = vardiyaGun.getVardiya();

							if (denklestirmeAyDurum && !bosGunMap.isEmpty() && vardiya != null && vardiya.getId() != null && vardiyaGun.isAyinGunu() && vardiya.isCalisma()) {
								if (vardiyaGun.getHareketler() != null && !vardiyaGun.getHareketDurum() && vardiyaGun.getOncekiVardiyaGun() != null) {
									VardiyaGun oncekiVardiyaGun = vardiyaGun.getOncekiVardiyaGun();
									if (oncekiVardiyaGun.isAyinGunu() && oncekiVardiyaGun.getVardiya() != null && oncekiVardiyaGun.isHaftaTatil() && !oncekiVardiyaGun.getHareketDurum()) {
										if (bosGunMap.containsKey(oncekiVardiyaGun.getId())) {
											List<VardiyaGun> bosGunList = bosGunMap.get(oncekiVardiyaGun.getId());
											Vardiya haftaVardiya = oncekiVardiyaGun.getVardiya();
											for (VardiyaGun vardiyaGun2 : bosGunList) {
												if (vardiyaGun2.getVardiyaDate().before(oncekiVardiyaGun.getVardiyaDate())) {
													Vardiya calismaVardiya = vardiyaGun2.getVardiya();
													oncekiVardiyaGun.setVardiya(calismaVardiya);
													oncekiVardiyaGun.setVersion(-1);
													vardiyaGun2.setVardiya(haftaVardiya);
													vardiyaGun2.setVersion(0);
													saveOrUpdate(oncekiVardiyaGun);
													saveOrUpdate(vardiyaGun2);
													flush = true;
													uyariHaftaTatilMesai = true;
													logger.debug(oncekiVardiyaGun.getVardiyaKeyStr() + " " + haftaVardiya.getAdi() + " " + vardiyaGun2.getVardiyaKeyStr() + " " + calismaVardiya.getAdi());
													break;
												}
											}

										}

									}

								}
							}
							if (vardiyaGun.getVardiyaDateStr().endsWith("0501"))
								logger.debug(vardiyaGun.getGecenAyResmiTatilSure());
							if (saatEkle) {
								// vardiyaGun.addCalismaSuresi(vardiyaGun.getGecenAyResmiTatilSure());

								VardiyaSaat vardiyaSaat = vardiyaGun.getVardiyaSaat();
								if (vardiyaSaat == null)
									vardiyaSaat = new VardiyaSaat();
								vardiyaSaat.setGuncellendi(false);
								double normalSure = 0.0d;
								if (vardiyaGun.getIslemVardiya() != null && vardiyaGun.getIzin() == null && vardiyaGun.getIslemVardiya().isCalisma())
									normalSure = vardiyaGun.getIslemVardiya().getNetCalismaSuresi();
								if (denklestirmeAyDurum) {
									if (hareketDurum.equals(Boolean.FALSE)) {
										vardiyaSaat.setResmiTatilSure(0.0d);
										vardiyaSaat.setAksamVardiyaSaatSayisi(0.0d);
									} else {
										vardiyaSaat.setResmiTatilSure(vardiyaGun.getResmiTatilSure());
										vardiyaSaat.setAksamVardiyaSaatSayisi(vardiyaGun.getAksamKatSayisi());
									}
									if (hareketDurum.equals(Boolean.TRUE) && vardiyaGun.isZamanGelmedi() == false && vardiyaGun.getHareketler() != null) {
										vardiyaSaat.setCalismaSuresi(vardiyaGun.getCalismaSuresi());
									} else {
										vardiyaSaat.setCalismaSuresi(0.0d);
									}
									if (vardiyaGun.getGecenAyResmiTatilSure() > 0.0d) {
										vardiyaSaat.setCalismaSuresi(vardiyaSaat.getCalismaSuresi() + vardiyaGun.getGecenAyResmiTatilSure());
									}
								}

								vardiyaSaat.setNormalSure(normalSure);
								if (vardiyaSaat.isGuncellendi()) {
									if (vardiyaSaat.getId() == null)
										vardiyaSaat.setNormalSure(normalSure);
									else
										vardiyaSaat.setGuncellemeTarihi(bugun);
									saveList.add(vardiyaSaat);
									if (vardiyaGun.getVardiyaSaat() == null) {
										vardiyaGun.setVardiyaSaat(vardiyaSaat);
										saveVardiyaGun = Boolean.TRUE;

									}
								} else if (vardiyaSaat.getId() == null)
									vardiyaGun.setVardiyaSaat(null);
							}
							if (denklestirmeAyDurum && vardiyaGun.getVardiya() != null && vardiyaGun.getId() != null && !vardiyaGun.getDurum().equals(hareketDurum)) {
								vardiyaGun.setDurum(hareketDurum);
								vardiyaGun.setGuncellemeTarihi(bugun);
								saveVardiyaGun = Boolean.TRUE;
							}
							if (saveVardiyaGun)
								saveList.add(vardiyaGun);
							vardiyalar.put(vardiyaGun.getVardiyaKeyStr(), vardiyaGun);

							Vardiya islemVardiya = vardiyaGun.getIslemVardiya();
							vardiyaGun.setTitleStr(null);
							if (islemVardiya != null) {
								boolean eksikCalismaDurum = false;
								if (vardiyaGun.getVardiya() != null) {
									Double netSure = vardiyaGun.getVardiya().getNetCalismaSuresi();
									if (vardiyaGun.getHareketDurum() && vardiyaGun.isIzinli() == false && netSure > 0.0d) {
										if ((calismaSuresi(vardiyaGun) * 100) / netSure < denklestirmeAy.getYemekMolasiYuzdesi()) {
											eksikCalismaDurum = denklestirmeAyDurum && eksikCalismaGoster;
											if (!vardiyaGun.isHataliDurum())
												vardiyaGun.setHataliDurum(eksikCalismaDurum);
										}
									}
								}
								if (loginUser.isAdmin()) {
									String titleStr = fazlaMesaiOrtakIslemler.getFazlaMesaiSaatleri(vardiyaGun, loginUser);
									if (eksikCalismaDurum)
										titleStr += "<br/>" + getEksikCalismaHTML(vardiyaGun);

									vardiyaGun.setTitleStr(titleStr);
									vardiyaGun.addLinkAdresler(titleStr);
								}
							}

							if (vardiyaGun.isZamanGelmedi() && vardiyaGun.getHareketler() != null) {
								for (Iterator iterator2 = vardiyaGun.getHareketler().iterator(); iterator2.hasNext();) {
									HareketKGS kgsHareket = (HareketKGS) iterator2.next();
									if (kgsHareket.isGecerliDegil())
										iterator2.remove();
								}
							}
						}
						if (!offIzinliGunler.isEmpty()) {
							Personel izinSahibi = puantaj.getPdksPersonel();
							String izinStr = izinSahibi.getPdksSicilNo() + " " + ortakIslemler.personelNoAciklama() + " " + izinSahibi.getAdSoyad() + " ait izinde ";
							String virgul = "";
							for (Iterator iterator = offIzinliGunler.iterator(); iterator.hasNext();) {
								VardiyaGun vGun = (VardiyaGun) iterator.next();
								izinStr += virgul + PdksUtil.convertToDateString(vGun.getVardiyaDate(), "d MMM EEEEE");
								virgul = ", ";
							}
							if (izinStr.indexOf(",") > 0) {
								int ind = izinStr.lastIndexOf(",");
								String str1 = izinStr.substring(0, ind), str2 = izinStr.substring(ind + 1);
								izinStr = str1 + " ve" + str2 + " günlerinde ";
							} else
								izinStr += " gününde ";
							izinStr = PdksUtil.replaceAllManuel(izinStr + " hafta içinde OFF planlanmıştır.", "  ", " ");
							if (userLogin.getLogin())
								PdksUtil.addMessageAvailableWarn(izinStr);
						}

						if (!saveList.isEmpty()) {
							for (Iterator iterator = saveList.iterator(); iterator.hasNext();) {
								Object object = (Object) iterator.next();
								saveOrUpdate(object);
							}
							sessionFlush();
						}
					}
					if (offSure != null)
						puantaj.setOffSure(offSure);
					puantaj.setResmiTatilToplami(0d);
					puantaj.setHaftaCalismaSuresi(0d);
					if (!fazlaMesaiHesapla)
						aksamVardiyaSayisi = 0;
					if (!fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_SAAT)) {
						aksamVardiyaSaatSayisi = 0.0d;
					}
					if (!fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_ADET)) {
						aksamVardiyaSayisi = 0;
					}
					puantaj.setAksamVardiyaSaatSayisi(aksamVardiyaSaatSayisi);
					puantaj.setAksamVardiyaSayisi(aksamVardiyaSayisi);
					puantaj.setFazlaMesaiHesapla(fazlaMesaiHesapla);
					if (fazlaMesaiHesapla && puantaj.isFazlaMesaiHesapla() == false && puantaj.getYonetici() == null && yoneticiTanimli == false) {
						puantaj.setYonetici(puantaj.getPdksPersonel());
						puantaj.setFazlaMesaiHesapla(fazlaMesaiHesapla);
						puantaj.setYonetici(null);
					}
					aylikPuantajSablon.setFazlaMesaiHesapla(fazlaMesaiHesapla);
					ortakIslemler.puantajHaftalikPlanOlustur(Boolean.TRUE, null, vardiyalar, aylikPuantajSablon, puantaj);
					personelDenklestirme = puantaj.getPersonelDenklestirme();
					if (personelDenklestirme == null)
						continue;

					personelDenklestirme.setGuncellendi(Boolean.FALSE);
					try {
						if (personelDenklestirme.isOnaylandi()) {
							// personelDenklestirme = ortakIslemler.aylikPlanSureHesapla(puantaj, !personelDenklestirme.isKapandi(), yemekAraliklari);
							yemekAraliklari = ortakIslemler.getYemekList(aylikPuantajDefault.getIlkGun(), aylikPuantajDefault.getSonGun(), session);
							if (personelDurumMap.containsKey(personelDenklestirme.getId()))
								puantaj.setFazlaMesaiIzinKontrol(Boolean.FALSE);
							puantaj.setLoginUser(loginUser);
							Boolean hesapla = puantaj.isFazlaMesaiHesapla();
							puantaj.setFazlaMesaiHesapla(true);

							personelDenklestirme = ortakIslemler.aylikPlanSureHesapla(manuelGiris, manuelCikis, true, normalCalismaVardiya, true, puantaj, !personelDenklestirme.isKapandi(loginUser), tatilGunleriMap, session);
							puantaj.setFazlaMesaiHesapla(hesapla);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (!fazlaMesaiIzinKullan)
						fazlaMesaiIzinKullan = personelDenklestirme.getFazlaMesaiIzinKullan() != null && personelDenklestirme.getFazlaMesaiIzinKullan();
					if (!fazlaMesaiOde && personelDenklestirme != null)
						fazlaMesaiOde = personelDenklestirme.getFazlaMesaiOde() != null && !personelDenklestirme.getFazlaMesaiOde().equals(sirketFazlaMesaiOde);

					double resmiTatilToplami = puantaj.getResmiTatilToplami();
					double kesilenSure = personelDenklestirme != null ? personelDenklestirme.getKesilenSure() : 0.0d;
					int izinsizGun = 0;
					ortakIslemler.setVardiyaYemekList(puantaj.getVardiyalar(), yemekAraliklari);
					double ucretiOdenenMesaiSure = 0.0d;
					boolean gunMaxCalismaOdenir = puantaj.getCalismaModeli().isFazlaMesaiVarMi() && personelDenklestirme.getCalismaModeliAy().isGunMaxCalismaOdenir() && personelDenklestirme.isFazlaMesaiIzinKullanacak() == false;
					for (Iterator iterator = puantaj.getVardiyalar().iterator(); iterator.hasNext();) {
						VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
						if (!vardiyaGun.isAyinGunu()) {
							iterator.remove();
						} else {
							if (!calisiyor)
								calisiyor = vardiyaGun.getVardiya() != null;
							if (!gebemi && vardiyaGun.getVardiya() != null)
								gebemi = vardiyaGun.getVardiya().isGebelikMi();
							if (calisiyor) {
								Double sure = vardiyaGun.getCalismaSuresi();
								if (gunMaxCalismaOdenir && vardiyaGun.isFcsDahil())
									ucretiOdenenMesaiSure += sure != null && sure.doubleValue() > fazlaMesaiMaxSure + (vardiyaGun.getHaftaCalismaSuresi() + vardiyaGun.getResmiTatilSure()) ? sure.doubleValue() - fazlaMesaiMaxSure
											- (vardiyaGun.getHaftaCalismaSuresi() + vardiyaGun.getResmiTatilSure()) : 0.0d;

								if (vardiyaGun.getIzin() == null)
									++izinsizGun;
								if (vardiyaGun.getHaftaCalismaSuresi() > 0) {
									if (!haftaTatilVar)
										haftaTatilVar = Boolean.TRUE;
								}
							}
							if (vardiyaGun.getGecenAyResmiTatilSure() > 0)
								vardiyaGun.addCalismaSuresi(vardiyaGun.getGecenAyResmiTatilSure());

							if (vardiyaGun.getResmiTatilSure() > 0) {
								if (!resmiTatilVar)
									resmiTatilVar = Boolean.TRUE;
								resmiTatilToplami += vardiyaGun.getResmiTatilSure();
								// logger.info(vardiyaGun.getVardiyaKeyStr() + " " + resmiTatilToplami + " " + vardiyaGun.getResmiTatilSure());
							}
							if (vardiyaGun.getCalisilmayanAksamSure() > 0)
								aksamVardiyaSaatSayisi += vardiyaGun.getCalisilmayanAksamSure();
						}
					}
					if (izinsizGun == 0 && puantaj.getFazlaMesaiSure() != 0.0d) {
						double devredenSure = 0.0d;
						if (personelDenklestirme.getPersonelDenklestirmeGecenAy() != null) {
							devredenSure = personelDenklestirme.getPersonelDenklestirmeGecenAy().getDevredenSure();
							puantaj.setEksikCalismaSure(personelDenklestirme.getPersonelDenklestirmeGecenAy().getEksikCalismaSure());
						}
						if (devredenSure < 0.0d) {
							puantaj.setDevredenSure(devredenSure);
							puantaj.setSaatToplami(0.0);
							puantaj.setPlanlananSure(0.0);
							puantaj.setUcretiOdenenMesaiSure(0.0d);
							puantaj.setFazlaMesaiSure(0.0d);
						}

					}
					if (denklestirmeAyDurum == false && !haftaTatilDurum.equals("1"))
						haftaCalismaSuresi = 0.0d;
					puantaj.setHaftaCalismaSuresi(haftaCalismaSuresi);
					puantaj.setUcretiOdenenMesaiSure(ucretiOdenenMesaiSure);
					if (!gebeGoster)
						gebeGoster = puantaj.isGebeDurum();
					if (!yasalFazlaCalismaAsanSaat && personelDenklestirme.getCalismaModeliAy().isGunMaxCalismaOdenir())
						yasalFazlaCalismaAsanSaat = calismaModeli.isFazlaMesaiVarMi() && ucretiOdenenMesaiSure > 0.0d;
					if (!sutIzniGoster)
						sutIzniGoster = personelDenklestirme != null && (personelDenklestirme.getSutIzniDurum() != null && personelDenklestirme.getSutIzniDurum());
					if (!isAramaGoster)
						isAramaGoster = personelDenklestirme != null && (personelDenklestirme.getIsAramaPersonelDonemselDurum() != null && personelDenklestirme.getIsAramaPersonelDonemselDurum().getIsAramaIzni());

					if (!partTimeGoster)
						partTimeGoster = personelDenklestirme != null && personelDenklestirme.isPartTimeDurumu();
					if (!suaGoster)
						suaGoster = personelDenklestirme != null && personelDenklestirme.isSuaDurumu();
					// if (/*personelDenklestirme.isErpAktarildi() ||*/ !personelDenklestirme.getDenklestirmeAy().isDurumu()) {
					if (personelDenklestirme.isErpAktarildi() || !denklestirmeAyDurum) {
						boolean buAyIstenAyrildi = false;

						if (!personelCalisiyor) {
							cal.setTime(personelDenklestirme.getPersonel().getSonCalismaTarihi());
							int ayrilmaYil = cal.get(Calendar.YEAR), ayrilmaAy = cal.get(Calendar.MONTH) + 1;
							buAyIstenAyrildi = denklestirmeAy.getAy() == ayrilmaAy && ayrilmaYil == denklestirmeAy.getYil();
						}
						if (personelDenklestirme.isKapandi(loginUser) && buAyIstenAyrildi == false) {
							double fazlaMesaiSure = puantaj.getFazlaMesaiSure();
							if (personelDenklestirme.isErpAktarildi() && fazlaMesaiSure != personelDenklestirme.getOdenecekSure())
								logger.debug(personelDenklestirme.getPersonel().getPdksSicilNo() + " " + fazlaMesaiSure);
							if (personelDenklestirme.isErpAktarildi() && personelDenklestirme.getOdenecekSure() > 0)
								puantaj.setFazlaMesaiSure(personelDenklestirme.getOdenecekSure());
							else {
								personelDenklestirme.setOdenenSure(fazlaMesaiSure);
							}

							if (bakiyeGuncelle == null || bakiyeGuncelle.equals(Boolean.FALSE) || puantaj.isFazlaMesaiHesapla() == false) {
								double devredenSure = 0.0;
								if (personelDenklestirme.getDevredenSure() != null) {
									devredenSure = PdksUtil.setSureDoubleTypeRounded(personelDenklestirme.getDevredenSure(), puantaj.getYarimYuvarla());
								}

								puantaj.setDevredenSure(devredenSure);
								puantaj.setKesilenSure(personelDenklestirme.getKesilenSure());
							} else if (denklestirmeAyDurum || (bakiyeGuncelle != null && bakiyeGuncelle)) {
								personelDenklestirme.setPlanlanSure(puantaj.getPlanlananSure());
								personelDenklestirme.setDevredenSure(puantaj.getDevredenSure());
								personelDenklestirme.setEksikCalismaSure(puantaj.getEksikCalismaSure());
								personelDenklestirme.setFazlaMesaiSure(puantaj.getAylikNetFazlaMesai());
								personelDenklestirme.setHaftaCalismaSuresi(puantaj.getHaftaCalismaSuresi());
								if (denklestirmeAyDurum) {
									personelDenklestirme.setHesaplananSure(puantaj.getSaatToplami());
									personelDenklestirme.setPlanlanSure(puantaj.getPlanlananSure());
								}

							}
							puantaj.setResmiTatilToplami(personelDenklestirme.getResmiTatilSure());

						} else if (denklestirmeAyDurum || (bakiyeGuncelle != null && bakiyeGuncelle)) {
							personelDenklestirme.setPlanlanSure(puantaj.getPlanlananSure());
							personelDenklestirme.setHesaplananSure(puantaj.getSaatToplami());
							personelDenklestirme.setFazlaMesaiSure(puantaj.getAylikNetFazlaMesai());
							personelDenklestirme.setDevredenSure(puantaj.getDevredenSure());
							personelDenklestirme.setEksikCalismaSure(puantaj.getEksikCalismaSure());
							personelDenklestirme.setResmiTatilSure(puantaj.getResmiTatilToplami());
							personelDenklestirme.setHaftaCalismaSuresi(puantaj.getHaftaCalismaSuresi());
							personelDenklestirme.setKesilenSure(puantaj.getKesilenSure());
							personelDenklestirme.setDurum(puantaj.isFazlaMesaiHesapla());
							personelDenklestirme.setOdenenSure(puantaj.getFazlaMesaiSure());

						}
						if (!denklestirmeAyDurum) {
							aksamVardiyaSayisi = personelDenklestirme.getAksamVardiyaSayisi().intValue();
							aksamVardiyaSaatSayisi = personelDenklestirme.getAksamVardiyaSaatSayisi();
							haftaCalismaSuresi = personelDenklestirme.getHaftaCalismaSuresi();
						}
					} else {
						personelDenklestirme.setDurum(puantaj.isFazlaMesaiHesapla());
						if (personelDenklestirme.getDurum()) {
							personelDenklestirme.setEksikCalismaSure(puantaj.getEksikCalismaSure());
							personelDenklestirme.setPlanlanSure(puantaj.getPlanlananSure());
							personelDenklestirme.setHesaplananSure(puantaj.getSaatToplami());
							personelDenklestirme.setFazlaMesaiSure(puantaj.getAylikNetFazlaMesai());
							if (denklestirmeAyDurum) {
								if (puantaj.getDevredenSure() > 0 && !personelCalisiyor) {
									double devredenSure = puantaj.getDevredenSure();
									puantaj.setFazlaMesaiSure(puantaj.getFazlaMesaiSure() + devredenSure);
									personelDenklestirme.setFazlaMesaiSure(personelDenklestirme.getFazlaMesaiSure() + devredenSure);
									puantaj.setDevredenSure(0.0d);
								}
							}

						} else {
							personelDenklestirme.setEksikCalismaSure(0d);
							personelDenklestirme.setPlanlanSure(0d);
							personelDenklestirme.setHesaplananSure(0d);
						}

						aksamVardiyaSaatSayisi += sabahAksamCikisSaatSayisi;
						if (!fazlaMesaiHesapla || !calisiyor) {
							puantaj.setFazlaMesaiSure(0d);
							puantaj.setDevredenSure(0d);
							puantaj.setEksikCalismaSure(0d);
							puantaj.setResmiTatilToplami(0d);
							puantaj.setHaftaCalismaSuresi(0d);
							if (denklestirmeAyDurum) {
								if (!personelDenklestirme.isKapandi(loginUser))
									personelDenklestirme.setDevredenSure(null);
								personelDenklestirme.setResmiTatilSure(0d);
								personelDenklestirme.setOdenenSure(0d);
								personelDenklestirme.setEksikCalismaSure(0d);

							}

						} else {
							if (denklestirmeAyDurum) {
								if (!puantaj.getPersonelDenklestirme().isOnaylandi()) {
									puantaj.setHaftaCalismaSuresi(0d);
									puantaj.setResmiTatilToplami(0d);
								}

								boolean partTime = stajerSirket || (personel.getPartTime() != null && personel.getPartTime().booleanValue());
								if ((personelDenklestirme.getFazlaMesaiOde().booleanValue() == false || puantaj.getFazlaMesaiSure() > 0) && calisiyor && partTime == false) {

								} else if (partTime) {
									puantaj.setFazlaMesaiSure(0.0d);
									puantaj.setDevredenSure(0D);
									personelDenklestirme.setOdenenSure(0D);
									personelDenklestirme.setResmiTatilSure(0D);

									personelDenklestirme.setDevredenSure(0D);
									personelDenklestirme.setAksamVardiyaSaatSayisi(0D);
									personelDenklestirme.setAksamVardiyaSayisi(0D);
								}

							}
							if (!hataYok)
								setHataYok(fazlaMesaiHesapla && tarihGecti);
						}
					}
					if (calisiyor) {
						boolean eksikCalismaVar = false;
						if (denklestirmeAyDurum && puantaj.isFazlaMesaiHesapla() && perCalismaModeli != null && hataliPuantajGoster) {
							eksikCalismaVar = puantaj.isEksikGunVar();

						}
						if (PdksUtil.hasStringValue(sicilNo) || denklestirmeAyDurum == false || hataliPuantajGoster == false || puantaj.isFazlaMesaiHesapla() == false || eksikCalismaVar)
							aylikPuantajList.add(puantaj);
					}

					else {
						iterator1.remove();
						continue;
					}

					if (denklestirmeAyDurum) {
						if (personelDenklestirme.getEgitimSuresiAksamGunSayisi() != null && (personelDenklestirme.getPersonel().getEgitimDonemi() == null || !personelDenklestirme.getPersonel().getEgitimDonemi())) {

							try {
								personelDenklestirme.setEgitimSuresiAksamGunSayisi(null);
								personelDenklestirme.setGuncellendi(Boolean.TRUE);
							} catch (Exception ee) {
								ee.printStackTrace();
							}

						}

						if (aksamVardiyaBasSaat == null || aksamVardiyaBitSaat == null) {
							aksamVardiyaSaatSayisi = 0d;
							aksamVardiyaSayisi = 0;

						} else if (personelDenklestirme.getEgitimSuresiAksamGunSayisi() != null && personelDenklestirme.getEgitimSuresiAksamGunSayisi() >= 0) {
							aksamVardiyaSaatSayisi = 0d;
							if (aksamVardiyaSayisi > personelDenklestirme.getEgitimSuresiAksamGunSayisi())
								aksamVardiyaSayisi = personelDenklestirme.getEgitimSuresiAksamGunSayisi();
						}
						if (!onayla)
							onayla = personel.getPdks() && !personelDenklestirme.isErpAktarildi() && puantaj.isDonemBitti() && puantaj.isFazlaMesaiHesapla() && personelDenklestirme.isOnaylandi();

					} else {
						if (!(ikRole))
							resmiTatilToplami = personelDenklestirme.getResmiTatilSure();
						else
							personelDenklestirme.setResmiTatilSure(resmiTatilToplami);
						aksamVardiyaSaatSayisi = personelDenklestirme.getAksamVardiyaSaatSayisi();
						aksamVardiyaSayisi = personelDenklestirme.getAksamVardiyaSayisi().intValue();
						haftaCalismaSuresi = personelDenklestirme.getHaftaCalismaSuresi();
					}
					if (denklestirmeAyDurum) {
						kesilenSure = 0;
						if (negatifBakiyeDenkSaat < 0.0d) {
							double normalCalisma = personelDenklestirme.getCalismaModeli().getHaftaIci();
							if (calisiyor == false) {
								if (puantaj.getDevredenSure() < 0) {
									kesilenSure = -puantaj.getDevredenSure();
									if (!denklestirmeAy.isKesintiYok()) {
										if (denklestirmeAy.isKesintiSaat()) {
											puantaj.setDevredenSure(kesilenSure + puantaj.getDevredenSure());
										} else if (denklestirmeAy.isKesintiGun()) {
											double kesilecekSaatSure = (new Double(-puantaj.getDevredenSure() / normalCalisma)).intValue() * normalCalisma;
											kesilenSure = kesilecekSaatSure / normalCalisma;
											puantaj.setDevredenSure(kesilecekSaatSure + puantaj.getDevredenSure());
										}
									}
								}
							} else if (puantaj.getDevredenSure() <= negatifBakiyeDenkSaat && puantaj.getGecenAyFazlaMesai(loginUser) < 0) {
								if (puantaj.getDevredenSure() < -normalCalisma) {
									double kesilecekSaatSure = (new Double(-puantaj.getDevredenSure() / normalCalisma)).intValue() * normalCalisma;
									if (!denklestirmeAy.isKesintiYok()) {
										if (denklestirmeAy.isKesintiSaat()) {
											kesilenSure = kesilecekSaatSure;
											puantaj.setDevredenSure(kesilecekSaatSure + puantaj.getDevredenSure());
										} else if (denklestirmeAy.isKesintiGun()) {
											kesilenSure = kesilecekSaatSure / normalCalisma;
											puantaj.setDevredenSure(kesilecekSaatSure + puantaj.getDevredenSure());
										}

									} else
										kesilenSure = kesilecekSaatSure;

								}

							}
						}
					}
					if (!kesilenSureGoster)
						kesilenSureGoster = kesilenSure > 0.0d;
					puantaj.setKesilenSure(kesilenSure);
					int yarimYuvarla = puantaj.getYarimYuvarla();
					puantaj.setResmiTatilToplami(PdksUtil.setSureDoubleTypeRounded(resmiTatilToplami, yarimYuvarla));
					if (denklestirmeAyDurum && puantaj.isFazlaMesaiHesapla() && personelDenklestirme.getPersonelDenklestirmeGecenAy() != null && personel.getIseGirisTarihi().before(aylikPuantajSablon.getIlkGun())) {
						PersonelDenklestirme personelDenklestirmeGecenAy = personelDenklestirme.getPersonelDenklestirmeGecenAy();
						if (personelDenklestirmeGecenAy != null) {
							CalismaModeli calismaModeliGecenAy = personelDenklestirmeGecenAy.getCalismaModeli();
							DenklestirmeAy denklestirmeAyGecen = personelDenklestirmeGecenAy.getDenklestirmeAy();
							if (!personelDenklestirmeGecenAy.isOnaylandi()) {
								puantaj.setFazlaMesaiHesapla(Boolean.FALSE);
								if (userLogin.getLogin())
									PdksUtil.addMessageAvailableError(denklestirmeAyGecen.getAyAdi() + " " + denklestirmeAyGecen.getYil() + " " + personel.getPdksSicilNo() + " - " + personel.getAdSoyad() + " " + " çalışma planı onaylanmadı!");
							} else if (!personelDenklestirmeGecenAy.getDurum() && calismaModeliGecenAy != null && calismaModeliGecenAy.isSaatlikOdeme()) {
								puantaj.setFazlaMesaiHesapla(Boolean.FALSE);
								if (userLogin.getLogin())
									PdksUtil.addMessageAvailableError(denklestirmeAyGecen.getAyAdi() + " " + denklestirmeAyGecen.getYil() + " " + personel.getPdksSicilNo() + " - " + personel.getAdSoyad() + " fazla mesaisi onaylanmadı!");
							}
						}

					}
					if (bakiyeGuncelle != null && bakiyeGuncelle && puantaj.isFazlaMesaiHesapla()) {
						personelDenklestirme.setDurum(puantaj.isFazlaMesaiHesapla());
						if (personelDenklestirme.isGuncellendi() && !loginUser.isAdmin()) {
							personelDenklestirme.setGuncellemeTarihi(new Date());
							personelDenklestirme.setGuncelleyenUser(loginUser);
						}
						personelDenklestirme.setGuncellendi(Boolean.TRUE);
					}
					boolean denklestirilmeyenPersonelDevredenVar = Boolean.FALSE;
					if (PdksUtil.hasStringValue(denklesmeyenBakiyeDurum) && personel.isCalisiyorGun(sonGun) && personelDenklestirme.getDurum() && personelDenklestirme.getDevredenSure() != null && personelDenklestirme.getDevredenSure().doubleValue() < 0.0d
							&& personelDenklestirme.getPersonelDenklestirmeGecenAy() != null) {
						PersonelDenklestirme personelDenklestirmeGecenAy = personelDenklestirme.getPersonelDenklestirmeGecenAy();
						if (personelDenklestirmeGecenAy.getDevredenSure() != null && personelDenklestirmeGecenAy.getDevredenSure().doubleValue() < 0.0d) {
							if (denklesmeyenBakiyeDurum.equalsIgnoreCase("U") || denklesmeyenBakiyeDurum.equalsIgnoreCase("B")) {
								denklestirilmeyenPersonelDevredenVar = Boolean.TRUE;
								denklestirilmeyenDevredenVar = Boolean.TRUE;
								if (denklesmeyenBakiyeDurum.equalsIgnoreCase("B")) {
									puantaj.setFazlaMesaiHesapla(Boolean.FALSE);
									personelDenklestirme.setGuncellendi(Boolean.TRUE);
								}
							}

						}

					}
					puantaj.setDenklestirilmeyenDevredenVar(denklestirilmeyenPersonelDevredenVar);
					if (personelDenklestirme.isGuncellendi()) {
						if ((bakiyeGuncelle != null && bakiyeGuncelle) || puantaj.isFazlaMesaiHesapla() != personelDenklestirme.getDurum() || (gecenAy != null && gecenAy.getDurum().equals(Boolean.FALSE))) {
							if (puantaj.isFazlaMesaiHesapla() != personelDenklestirme.getDurum())
								personelDenklestirme.setDurum(puantaj.isFazlaMesaiHesapla());
							saveOrUpdate(personelDenklestirme);
							flush = Boolean.TRUE;
						}
					}
					if (!fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_SAAT)) {
						aksamVardiyaSaatSayisi = 0.0d;
					}
					if (!fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_ADET)) {
						aksamVardiyaSayisi = 0;
					}
					puantaj.setAksamVardiyaSaatSayisi(aksamVardiyaSaatSayisi);
					puantaj.setAksamVardiyaSayisi(aksamVardiyaSayisi);
					puantaj.setHaftaCalismaSuresi(haftaCalismaSuresi);
					if (devamlilikPrimi != null) {

						if (puantaj.getDinamikAlanMap() != null) {
							PersonelDenklestirmeDinamikAlan denklestirmeDinamikAlan = puantaj.getDinamikAlanMap().get(devamlilikPrimi.getId());
							if (devamlilikPrimi.getDurum() && denklestirmeAyDurum && denklestirmeDinamikAlan != null) {
								try {
									if (devamsizList.contains(personel.getId()) || !personelDenklestirme.getDurum()) {
										if (denklestirmeDinamikAlan.getIslemDurum() == null || denklestirmeDinamikAlan.getIslemDurum().equals(Boolean.TRUE) || !personelDenklestirme.getDurum()) {
											denklestirmeDinamikAlan.setIslemDurum(Boolean.FALSE);
											saveOrUpdate(denklestirmeDinamikAlan);
											flush = true;
										}
									} else {
										boolean flushDurum = devamlilikPrimiHesapla(puantaj, denklestirmeDinamikAlan);
										if (!flush)
											flush = flushDurum;
									}

								} catch (Exception ed) {
									logger.error(ed);
									ed.printStackTrace();
								}

							}
						}

					}

					if (!aksamGun)
						aksamGun = puantaj.getAksamVardiyaSayisi() != 0;
					if (!aksamSaat)
						aksamSaat = puantaj.getAksamVardiyaSaatSayisi() != 0.0d;
					if (!haftaTatilVar)
						haftaTatilVar = puantaj.getHaftaCalismaSuresi() != 0.0d;
					if (!resmiTatilVar)
						resmiTatilVar = puantaj.getResmiTatilToplami() != 0.0d;
					if (!eksikMaasGoster)
						eksikMaasGoster = puantaj.getEksikCalismaSure() != 0.0d;
					if (gebemi)
						iterator1.remove();

					if (sonVardiyaBitZaman != null) {
						if (puantaj.isFazlaMesaiHesapla() && personelDenklestirme.getDurum()) {
							puantaj.setDonemBitti(bugun.after(sonVardiyaBitZaman));

						}
					} else if (fazlaMesaiOnayla || gunAdet > 0)
						puantaj.setDonemBitti(personel.getSskCikisTarihi().before(puantaj.getSonGun()) || puantaj.getSonGun().before(bugun));
					if (AylikPuantaj.getGebelikGuncelle() && denklestirmeAyDurum && puantaj.isFazlaMesaiHesapla() && puantaj.isGebeDurum() && puantaj.getCalisilanGunSayisi() > 0 && personelDenklestirme.getSutIzniSaatSayisi().doubleValue() == 0.0d) {
						puantaj.setFazlaMesaiHesapla(Boolean.FALSE);
						if (personelDenklestirme.getDurum()) {
							personelDenklestirme.setDurum(Boolean.FALSE);
							saveOrUpdate(personelDenklestirme);
							flush = true;
						}
						if (ikRole) {
							if (userLogin.getLogin())
								PdksUtil.addMessageAvailableWarn(personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " gebelik ÇGS girişi yapınız! ");
						}
					}
					if (denklestirmeAyDurum && yoneticiKontrolEtme == false) {
						if (personel.isSanalPersonelMi() == false && (puantaj.getYonetici() == null || puantaj.getYonetici().getId() == null)) {
							puantaj.setFazlaMesaiHesapla(false);
						}
					}

					if (denklestirmeAyDurum && puantaj.isFazlaMesaiHesapla() == false) {
						if (ayrikList.size() > 1) {
							if (!ayrikHareketVar)
								ayrikHareketVar = ayrikKontrol;
							if (!PdksUtil.getTestDurum()) {
								StringBuffer sb = new StringBuffer(personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " ");
								for (Iterator iterator = ayrikList.iterator(); iterator.hasNext();) {
									String string = (String) iterator.next();
									sb.append(string);
									if (iterator.hasNext()) {
										if (ayrikList.size() > 2)
											sb.append(", ");
										else
											sb.append(" ve ");
									}
								}
								if (userLogin.getLogin())
									PdksUtil.addMessageAvailableWarn(sb.toString() + (ayrikList.size() == 2 ? " arası" : "") + " giriş ve çıkış kayıtı vardır! ");
							}
						}
					} else
						puantaj.setAyrikHareketVar(false);
					if (denklestirmeAyDurum == false && personelDenklestirme != null) {
						boolean savePersonelDenklestirme = false;
						if (personelDenklestirme.getDurum()) {

							if (ikRole) {
								boolean odemeVar = personelDenklestirme.getOdenenSure() > 0.0d;
								if (personelDenklestirme.getHesaplananSure().equals(0.0D) && (odemeVar || puantaj.getSaatToplami() > 0.0d)) {
									personelDenklestirme.setHesaplananSure(puantaj.getSaatToplami());
									savePersonelDenklestirme = true;
								}
								if (personelDenklestirme.getPlanlanSure().equals(0.0D) && (odemeVar || puantaj.getPlanlananSure() > 0.0d)) {
									personelDenklestirme.setPlanlanSure(puantaj.getPlanlananSure());
									savePersonelDenklestirme = true;
								}
							}

						} else if (puantaj.isFazlaMesaiHesapla()) {
							personelDenklestirme.setDurum(Boolean.TRUE);
							savePersonelDenklestirme = true;
						}

						if (savePersonelDenklestirme) {
							saveOrUpdate(personelDenklestirme);
							flush = true;
						}
					}
					if (personelDenklestirme.getDurum() && personelDenklestirme.isOnaylandi()) {
						if (fazlaMesaiOnayDurum == false)
							fazlaMesaiOnayDurum = puantaj.isDonemBitti();
					}
					try {
						if (puantaj.isDonemBitti() && personelDenklestirme.isFazlaMesaiIzinKullanacak() && personelDenklestirme.getKismiOdemeSure() != null && personelDenklestirme.getKismiOdemeSure().doubleValue() > 0) {
							kismiOdemeGoster = ikRole;
							if (denklestirmeAyDurum && puantaj.getFazlaMesaiSure() == 0.0d) {
								if (userLogin.getLogin())
									PdksUtil.addMessageAvailableWarn(personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " Kısmi Ödenecek :" + loginUser.sayiFormatliGoster(personelDenklestirme.getKismiOdemeSure()) + " Devreden Süre : "
											+ loginUser.sayiFormatliGoster(personelDenklestirme.getDevredenSure()));
							}
						}
					} catch (Exception ex) {
						logger.error(ex);
						ex.printStackTrace();
					}

				}

				if (gecersizHareketler.isEmpty() == false && ortakIslemler.getParameterKey("mukerrerHareketIptal").equals("1")) {
					List<Long> idler = new ArrayList<Long>();
					for (HareketKGS hareketKGS : gecersizHareketler)
						idler.add(hareketKGS.getHareketTableId());
					List<PdksLog> logList = pdksEntityController.getSQLParamByFieldList(PdksLog.TABLE_NAME, PdksLog.COLUMN_NAME_ID, idler, PdksLog.class, session);
					Date guncellemeZamani = new Date();
					for (Iterator iterator = logList.iterator(); iterator.hasNext();) {
						PdksLog pdksLog = (PdksLog) iterator.next();
						pdksLog.setDurum(Boolean.FALSE);
						pdksLog.setGuncellemeZamani(guncellemeZamani);
						pdksEntityController.saveOrUpdate(session, entityManager, pdksLog);
					}
					flush = true;
					logList = null;
					idler = null;
				}
				gecersizHareketler = null;

				if (!(authenticatedUser.isAdmin() || authenticatedUser.isSistemYoneticisi()) && yasalFazlaCalismaAsanSaat)
					yasalFazlaCalismaAsanSaat = ortakIslemler.getParameterKey("yasalFazlaCalismaAsanSaat").equals("1");
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 7000 " + PdksUtil.getCurrentTimeStampStr());
				if (uyariHaftaTatilMesai) {
					if (userLogin.getLogin())
						PdksUtil.addMessageWarn("Hafta tatil günleri güncellendi, 'Fazla Mesai Getir' tekrar çalıştırın.");
				}

				if (denklestirilmeyenDevredenVar && userLogin.getLogin()) {
					if (denklesmeyenBakiyeDurum.equalsIgnoreCase("B"))
						PdksUtil.addMessageAvailableError("Geçen aydan devreden negatif bakiye denkleştirilemedi!");
					else
						PdksUtil.addMessageAvailableWarn("Geçen aydan devreden negatif bakiye denkleştirilemedi!");
				}
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 8000 " + PdksUtil.getCurrentTimeStampStr());

				if (denklestirmeAyDurum) {
					List<String> vGunList = ciftBolumCalisanKontrol(aylikPuantajList);
					if (!izinMap.isEmpty())
						try {
							if (loginUser.getLogin())
								izinCalismaUyariMesajiOlustur(vGunList, izinMap);
						} catch (Exception e) {
						}
				}

				izinMap = null;
				if (!vgIdList.isEmpty()) {
					map.clear();
					List<FazlaMesaiTalep> fList = pdksEntityController.getSQLParamByFieldList(FazlaMesaiTalep.TABLE_NAME, FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN, vgIdList, FazlaMesaiTalep.class, session);
					if (!fList.isEmpty()) {
						fList = PdksUtil.sortListByAlanAdi(fList, "baslangicZamani", true);
						for (Iterator iterator = fList.iterator(); iterator.hasNext();) {
							FazlaMesaiTalep fazlaMesaiTalep = (FazlaMesaiTalep) iterator.next();
							if (fazlaMesaiTalep.getOnayDurumu() == FazlaMesaiTalep.ONAY_DURUM_RED || fazlaMesaiTalep.getDurum() == null || fazlaMesaiTalep.getDurum().booleanValue() == false)
								iterator.remove();
							else {
								Long key = fazlaMesaiTalep.getVardiyaGun().getId();
								List<FazlaMesaiTalep> icListe = fmtMap.containsKey(key) ? fmtMap.get(key) : new ArrayList<FazlaMesaiTalep>();
								if (icListe.isEmpty())
									fmtMap.put(key, icListe);
								icListe.add(fazlaMesaiTalep);
							}
						}

					}
					fList = null;
				}
				if (testDurum)
					logger.info("fillPersonelDenklestirmeDevam 9000 " + PdksUtil.getCurrentTimeStampStr());

				paramsMap = null;
				if (!saveGenelList.isEmpty()) {
					for (Object obj : saveGenelList) {
						saveOrUpdate(obj);
					}
					flush = true;
				}
				if (aylikPuantajList.isEmpty() && userLogin.getLogin()) {
					if (kayitVar == false)
						PdksUtil.addMessageAvailableWarn("Fazla mesai kaydı bulunmadı! " + (seciliBolum != null ? " [ " + seciliBolum.getAciklama() + " ]" : ""));
					else if (hataliPuantajGoster)
						PdksUtil.addMessageAvailableWarn("Hatalı personel kaydı bulunmadı!");

				}

				else {
					ortakIslemler.sortAylikPuantajList(aylikPuantajList, true);
					msgwarnImg = ortakIslemler.getParameterKey("girisCikisResimYok");
					if (PdksUtil.hasStringValue(msgwarnImg) == false || msgwarnImg.indexOf(".") < 1)
						msgwarnImg = "msgwarn.png";
					if (flush)
						sessionFlush();
				}
			} else {
				if (fazlaMesaiMap == null)
					fazlaMesaiMap = new TreeMap<String, Tanim>();
				else
					fazlaMesaiMap.clear();
			}
			if (denklestirmeAyDurum && !hataYok) {
				setHataYok(sonCikisZamani != null && bugun.after(sonCikisZamani));
			}

		} catch (InvalidStateException e) {
			InvalidValue[] invalidValues = e.getInvalidValues();
			if (invalidValues != null) {
				for (InvalidValue invalidValue : invalidValues) {
					Object object = invalidValue.getBean();
					if (userLogin.getLogin().equals(Boolean.FALSE))
						continue;
					if (object != null && object instanceof VardiyaGun) {
						VardiyaGun vardiyaGun = (VardiyaGun) object;
						PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), PdksUtil.getDateFormat()) + " günü  alanı : " + invalidValue.getPropertyName() + " with message: " + invalidValue.getMessage());
					} else
						PdksUtil.addMessageAvailableWarn("Instance of bean class: " + invalidValue.getBeanClass().getSimpleName() + " has an invalid property: " + invalidValue.getPropertyName() + " with message: " + invalidValue.getMessage());
				}
			}
			logger.error(e);
			e.printStackTrace();

		} catch (Exception e3) {
			logger.error("Pdks hata in : \n");
			e3.printStackTrace();
			logger.error("Pdks hata out : " + e3.getMessage());

		} finally {

		}

		TreeMap<String, Object> ozetMap = izinGoster ? fazlaMesaiOrtakIslemler.getIzinOzetMap(loginUser, null, aylikPuantajList, izinGoster) : new TreeMap<String, Object>();
		izinTipiVardiyaList = ozetMap.containsKey("izinTipiVardiyaList") ? (List<Vardiya>) ozetMap.get("izinTipiVardiyaList") : new ArrayList<Vardiya>();
		izinTipiPersonelVardiyaMap = ozetMap.containsKey("izinTipiPersonelVardiyaMap") ? (TreeMap<String, TreeMap<String, List<VardiyaGun>>>) ozetMap.get("izinTipiPersonelVardiyaMap") : new TreeMap<String, TreeMap<String, List<VardiyaGun>>>();
		if (izinTipiVardiyaList != null && !izinTipiVardiyaList.isEmpty()) {
			fazlaMesaiOrtakIslemler.personelIzinAdetleriOlustur(aylikPuantajList, izinTipiVardiyaList, izinTipiPersonelVardiyaMap);
		}
		try {
			pageSize = Integer.parseInt(ortakIslemler.getParameterKey("puantajPageSize"));
		} catch (Exception e) {
			pageSize = 0;
		}
		if (pageSize < 20 || pageSize > aylikPuantajList.size())
			pageSize = aylikPuantajList.size() + 1;
		hatalariAyikla = false;
		if (testDurum)
			logger.info("fillPersonelDenklestirmeDevam 9100 " + PdksUtil.getCurrentTimeStampStr());

		if (denklestirmeAyDurum) {

			if (aylikPuantajList.size() > pageSize) {
				Date bitTarih = new Date();
				Date fark = new Date(bitTarih.getTime() - basTarih.getTime());
				String str = yil + " " + denklestirmeAy.getAyAdi() + " " + aylikPuantajList.size() + " adet " + (seciliBolum != null ? seciliBolum.getAciklama() + " personeli " : " personel ") + loginUser.getAdSoyad();
				if (userLogin.getLogin())
					logger.info(str + " --> " + PdksUtil.convertToDateString(basTarih, "HH:mm:ss") + " - " + PdksUtil.convertToDateString(bitTarih, "HH:mm:ss") + " : " + PdksUtil.convertToDateString(fark, "mm:ss"));
			}
		}

		if (denklestirmeAyDurum == false)
			fazlaMesaiOnayDurum = Boolean.FALSE;

		if (gecenAyDurum) {
			hataYok = false;
			if (userLogin.getLogin())
				PdksUtil.addMessageAvailableError(gecenAy.getAyAdi() + " " + gecenAy.getYil() + " dönemi açıktır!");
		} else if ((topluGuncelle || (kullaniciPersonel.equals(Boolean.FALSE) && ikRole)) && denklestirmeAyDurum && denklestirmeAy.getOtomatikOnayIKTarih() != null) {
			Calendar cal = Calendar.getInstance();
			Date otomatikOnayIKTarih = denklestirmeAy.getOtomatikOnayIKTarih();
			if (topluGuncelle && otomatikOnayIKTarih.before(cal.getTime()))
				otomatikOnayIKTarih = PdksUtil.getDate(PdksUtil.tariheGunEkleCikar(cal.getTime(), 1));
			cal.setTime(PdksUtil.getDate(cal.getTime()));
			cal.set(Calendar.YEAR, denklestirmeAy.getYil());
			cal.set(Calendar.MONTH, denklestirmeAy.getAy() - 1);
			cal.set(Calendar.DATE, 1);
			cal.add(Calendar.MONTH, 1);
			Date tarih = PdksUtil.getDate(cal.getTime());
			if (authenticatedUser.isIKAdmin()) {
				cal.add(Calendar.MONTH, 1);
				otomatikOnayIKTarih = PdksUtil.getDate(cal.getTime());
			}
			cal = Calendar.getInstance();
			Date toDay = cal.getTime();
			boolean baslangicDurum = false;
			if (denklestirmeAy.getOtomatikOnayIKBaslangicTarih() != null)
				baslangicDurum = toDay.after(denklestirmeAy.getOtomatikOnayIKBaslangicTarih());
			boolean tarihGeldi = (toDay.after(tarih) && toDay.before(otomatikOnayIKTarih)) || baslangicDurum || sonHafta;

			onayla = Boolean.FALSE;
			for (AylikPuantaj puantaj : aylikPuantajList) {
				PersonelDenklestirme pd = puantaj.getPersonelDenklestirme();
				boolean kaydet = pd.getDurum();
				if (kaydet) {
					Personel personel = pd.getPdksPersonel();
					if (tarihGeldi || (loginUser.isTestLogin() && toDay.after(personel.getSskCikisTarihi()))) {
						if (baslangicDurum) {
							puantaj.setSonGun(denklestirmeAy.getOtomatikOnayIKBaslangicTarih());
							puantaj.setDonemBitti(true);
							fazlaMesaiOnayDurum = true;
						}
						boolean eksikCalismaSureDegisti = PdksUtil.isDoubleDegisti(pd.getEksikCalismaSure(), puantaj.getEksikCalismaSure());
						boolean kesilenSureDegisti = PdksUtil.isDoubleDegisti(pd.getKesilenSure(), puantaj.getKesilenSure());
						boolean aksamVardiyaSaatSayisiDegisti = PdksUtil.isDoubleDegisti(pd.getAksamVardiyaSaatSayisi(), puantaj.getAksamVardiyaSaatSayisi());
						boolean aksamVardiyaSayisiDegisti = PdksUtil.isDoubleDegisti(pd.getAksamVardiyaSayisi(), (double) puantaj.getAksamVardiyaSayisi());
						boolean haftaCalismaSuresiDegisti = PdksUtil.isDoubleDegisti(pd.getHaftaCalismaSuresi(), puantaj.getHaftaCalismaSuresi());
						boolean resmiTatilSureDegisti = PdksUtil.isDoubleDegisti(pd.getResmiTatilSure(), puantaj.getResmiTatilToplami());
						boolean odenenSureDegisti = PdksUtil.isDoubleDegisti(pd.getOdenenSure(), puantaj.getFazlaMesaiSure());
						boolean devredenSureDegisti = PdksUtil.isDoubleDegisti(pd.getDevredenSure(), puantaj.getDevredenSure());
						kaydet = (!pd.isKapandi(loginUser) && (aksamVardiyaSaatSayisiDegisti || aksamVardiyaSayisiDegisti || devredenSureDegisti || eksikCalismaSureDegisti || haftaCalismaSuresiDegisti || resmiTatilSureDegisti || odenenSureDegisti || kesilenSureDegisti));
						puantaj.setKaydet(kaydet);
						if (puantaj.isKaydet())
							onayla = hataYok;
					}
				}
			}
			if (onayla) {
				mailGonder = Boolean.FALSE;
				try {
					if (!loginUser.isAdmin() || loginUser.getLogin().booleanValue() == false) {
						fazlaMesaiOnaylaDevam(aylikPuantajList, Boolean.TRUE, Boolean.TRUE);
					}
				} catch (Exception eo) {
					logger.error(eo);
					eo.printStackTrace();
				}

			}

		}

		if (denklestirmeAyDurum && aylikPuantajList != null && !aylikPuantajList.isEmpty()) {

			List<String> list = fazlaMesaiOrtakIslemler.getFazlaMesaiUyari(yil, ay, seciliEkSaha3Id, aylikPuantajList, session);
			for (String string : list)
				if (userLogin.getLogin())
					PdksUtil.addMessageAvailableWarn(string);

		}
		modelGoster = ortakIslemler.getModelGoster(denklestirmeAy, session);
		if (testDurum)
			logger.info("fillPersonelDenklestirmeDevam 9200 " + PdksUtil.getCurrentTimeStampStr());
		if (!modelGoster) {
			HashMap<Boolean, Long> sanalDurum = new HashMap<Boolean, Long>();
			if (aylikPuantajList != null) {
				try {
					for (AylikPuantaj ap : aylikPuantajList) {
						Personel personel = ap.getPdksPersonel();
						if (personel != null)
							sanalDurum.put(personel.getSanalPersonel(), personel.getId());
					}

				} catch (Exception e) {
					logger.error(e);
					e.printStackTrace();
				}

			}

			modelGoster = sanalDurum.size() > 1;
		}
		if (adminRole || denklestirmeAyDurum || (bakiyeGuncelle != null && bakiyeGuncelle)) {
			bordroVeriOlusturBasla(aylikPuantajList, loginUser);
		}
		saatlikCalismaGoster = false;
		izinBordoroGoster = false;
		if (bordroPuantajEkranindaGoster) {
			for (AylikPuantaj puantaj : aylikPuantajList) {
				if (saatlikCalismaGoster == false) {
					PersonelDenklestirme pd = puantaj.getPersonelDenklestirme();
					CalismaModeli cm = pd != null ? pd.getCalismaModeli() : null;
					saatlikCalismaGoster = cm != null && cm.isSaatlikOdeme();
				}
				if (izinBordoroGoster == false && puantaj.getDenklestirmeBordro() != null) {
					PersonelDenklestirmeBordro pdb = puantaj.getDenklestirmeBordro();
					izinBordoroGoster = pdb.getUcretliIzin().intValue() + pdb.getUcretsizIzin().intValue() + pdb.getRaporluIzin().intValue() > 0;
				}
				if (saatlikCalismaGoster && izinBordoroGoster)
					break;
			}
		}

		if (testDurum)
			logger.info("fillPersonelDenklestirmeDevam 9300 " + PdksUtil.getCurrentTimeStampStr());

		if (denklestirmeAyDurum) {
			boolean tekSicil = PdksUtil.hasStringValue(sicilNo);

			hataliPersoneller = ortakIslemler.getSelectItemList("hataliPersonel", authenticatedUser);
			if (tekSicil == false)
				if (hataliPersoneller != null)
					hataliPersoneller.clear();

			for (AylikPuantaj ap : aylikPuantajList) {
				PersonelDenklestirme pd = ap.getPersonelDenklestirme();
				if (pd.getDurum().equals(Boolean.FALSE)) {
					Personel personel = pd.getPdksPersonel();
					boolean ekle = true;
					if (tekSicil) {
						for (SelectItem st : hataliPersoneller) {
							if (st.getValue().equals(sicilNo))
								ekle = false;
						}
					}
					if (ekle)
						hataliPersoneller.add(new SelectItem(personel.getPdksSicilNo(), personel.getAdSoyad() + " [ " + personel.getPdksSicilNo() + " ]"));
				}
			}
			if (hataliPersoneller.isEmpty())
				hataliPersoneller = null;
		} else
			hataliPersoneller = null;

		linkAdres = null;
		if (denklestirmeAyDurum)
			linkAdres = getLinkAdresBilgi(inputPersonelNo, true);

		return aylikPuantajList;
	}

	/**
	 * @return
	 */
	public Boolean fazlaMesaiOnayDurumu() {
		boolean onayDurum = false;
		if (fazlaMesaiOnayDurum && kullaniciPersonel == false && hataYok && denklestirmeAyDurum)
			onayDurum = ikRole || PdksUtil.hasStringValue(sicilNo) == false || denklestirmeAy.getGuncelleIK();

		return onayDurum;

	}

	/**
	 * @param inputPersonelNo
	 * @return
	 */
	private String getLinkAdresBilgi(String inputPersonelNo, boolean calistir) {
		String strGizli = "yil=" + yil + "&ay=" + ay;
		if (!sadeceFazlaMesai)
			strGizli += "&sadeceFazlaMesai=" + sadeceFazlaMesai;
		strGizli += (hataliPuantajGoster != null && hataliPuantajGoster ? "&hataliPuantajGoster=" + hataliPuantajGoster : "");
		strGizli += (seciliEkSaha3Id != null ? "&gorevYeriId=" + seciliEkSaha3Id : "");
		strGizli += (seciliEkSaha4Id != null ? "&altBolumId=" + seciliEkSaha4Id : "");
		strGizli += (tesisId != null ? "&tesisId=" + tesisId : "");
		strGizli += (gorevTipiId != null ? "&gorevTipiId=" + gorevTipiId : "");
		strGizli += (sirket != null ? "&sirketId=" + sirket.getId() : "");
		strGizli += (PdksUtil.hasStringValue(sicilNo) ? "&sicilNo=" + sicilNo.trim() : "");
		if (PdksUtil.hasStringValue(inputPersonelNo))
			strGizli += "&inputPersonelNo=" + inputPersonelNo;
		if (calistir && aylikPuantajList != null && !aylikPuantajList.isEmpty())
			strGizli += "&calistir=" + new Date().getTime();
		if (linkBordroAdres != null)
			strGizli += "&linkBordroAdres=" + PdksUtil.getEncodeStringByBase64(linkBordroAdres);
		String deger = "<a href='http://" + adres + "/" + sayfaURL + "?linkAdresKey=" + PdksUtil.getEncodeStringByBase64(strGizli) + "'>" + ortakIslemler.getCalistiMenuAdi(sayfaURL) + " Ekranına Geri Dön</a>";
		return deger;
	}

	/**
	 * @param sessionInput
	 * @param entityManagerInput
	 * @param pdksEntityControllerInput
	 * @param ortakIslemlerInput
	 * @param fazlaMesaiOrtakIslemlerInput
	 */
	public void setInject(Session sessionInput, EntityManager entityManagerInput, PdksEntityController pdksEntityControllerInput, OrtakIslemler ortakIslemlerInput, FazlaMesaiOrtakIslemler fazlaMesaiOrtakIslemlerInput) {
		if (sessionInput != null && session == null)
			this.session = sessionInput;
		if (entityManagerInput != null && entityManager == null)
			this.entityManager = entityManagerInput;
		if (pdksEntityControllerInput != null && pdksEntityController == null)
			this.pdksEntityController = pdksEntityControllerInput;
		if (ortakIslemlerInput != null && ortakIslemler == null)
			this.ortakIslemler = ortakIslemlerInput;
		if (fazlaMesaiOrtakIslemlerInput != null && fazlaMesaiOrtakIslemler == null)
			this.fazlaMesaiOrtakIslemler = fazlaMesaiOrtakIslemlerInput;
	}

	/**
	 * @param puantajList
	 * @param loginUser
	 */
	private void bordroVeriOlusturBasla(List<AylikPuantaj> puantajList, User loginUser) {
		baslikMap.clear();
		if (sirket != null && ortakIslemler.getParameterKeyHasStringValue("bordroVeriOlustur")) {
			try {
				String str = ortakIslemler.getParameterKey("bordroVeriOlustur");
				int donem = yil * 100 + ay;
				if (donem >= Integer.parseInt(str))
					baslikMap = fazlaMesaiOrtakIslemler.bordroVeriOlustur(denklestirmeAyDurum || (bakiyeGuncelle != null && bakiyeGuncelle), puantajList, true, String.valueOf(donem), loginUser, session);

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
		boolean saatlikCalismaVar = ortakIslemler.getParameterKey("saatlikCalismaVar").equals("1");
		gerceklesenMesaiKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.gerceklesenMesaiKod());
		devredenBakiyeKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.devredenBakiyeKod());
		ucretiOdenenKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.ucretiOdenenKod());
		normalCalismaSaatKod = (saatlikCalismaVar && bordroPuantajEkranindaGoster) || baslikMap.containsKey(ortakIslemler.normalCalismaSaatKod());
		haftaTatilCalismaSaatKod = (saatlikCalismaVar && bordroPuantajEkranindaGoster) || (baslikMap.containsKey(ortakIslemler.haftaTatilCalismaSaatKod()));
		resmiTatilCalismaSaatKod = (saatlikCalismaVar && bordroPuantajEkranindaGoster) || (baslikMap.containsKey(ortakIslemler.resmiTatilCalismaSaatKod()));
		izinSureSaatKod = (saatlikCalismaVar && bordroPuantajEkranindaGoster) || (baslikMap.containsKey(ortakIslemler.izinSureSaatKod()));
		normalCalismaGunKod = bordroPuantajEkranindaGoster && baslikMap.containsKey(ortakIslemler.normalCalismaGunKod());
		haftaTatilCalismaGunKod = bordroPuantajEkranindaGoster && baslikMap.containsKey(ortakIslemler.haftaTatilCalismaGunKod());
		resmiTatilCalismaGunKod = bordroPuantajEkranindaGoster && baslikMap.containsKey(ortakIslemler.resmiTatilCalismaGunKod());
		izinSureGunKod = bordroPuantajEkranindaGoster && baslikMap.containsKey(ortakIslemler.izinSureGunKod());
		ucretliIzinGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.ucretliIzinGunKod());
		ucretsizIzinGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.ucretsizIzinGunKod());
		hastalikIzinGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.hastalikIzinGunKod());
		normalGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.normalGunKod());
		haftaTatilGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.haftaTatilGunKod());
		resmiTatilGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.resmiTatilGunKod());
		artikGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.artikGunKod());
		bordroToplamGunKod = bordroPuantajEkranindaGoster || baslikMap.containsKey(ortakIslemler.bordroToplamGunKod());
		devredenMesaiKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.devredenMesaiKod());
		if (!devredenMesaiKod || !devredenBakiyeKod) {
			for (AylikPuantaj ap : puantajList) {
				if (ap.getCalismaModeli().isSaatlikOdeme()) {
					double gecenAyFazlaMesai = ap.getGecenAyFazlaMesai(authenticatedUser);
					double devredenSure = ap.getDevredenSure();
					if (!devredenMesaiKod)
						devredenMesaiKod = gecenAyFazlaMesai != 0.0d;
					if (!!devredenBakiyeKod)
						devredenBakiyeKod = devredenSure != 0.0d;
				}
			}
		}
	}

	/**
	 * @param bosCalismaList
	 * @param uyariMesai
	 */
	private void bosCalismaOffGuncelle(List<VardiyaGun> bosCalismaList, boolean uyariMesai) {

		Vardiya offVardiya = ortakIslemler.getVardiyaOFF(session);

		if (offVardiya != null) {
			for (VardiyaGun vardiyaGun : bosCalismaList) {
				vardiyaGun.setVardiya(offVardiya);
				vardiyaGun.setVersion(0);
				saveOrUpdate(vardiyaGun);
			}
			sessionFlush();
			if (uyariMesai)
				if (userLogin.getLogin())
					PdksUtil.addMessageWarn(offVardiya.getAciklama() + " günler güncellendi, 'Fazla Mesai Getir' tekrar çalıştırın.");
		}

	}

	/**
	 * @param list
	 */
	private void haftaTatilVardiyaGuncelle(List<PersonelDenklestirmeTasiyici> list) {
		for (PersonelDenklestirmeTasiyici personelDenklestirmeTasiyici : list) {
			boolean flush = false;
			TreeMap<String, VardiyaGun> vgMap = new TreeMap<String, VardiyaGun>();
			TreeMap<Integer, List<VardiyaGun>> haftaMap = new TreeMap<Integer, List<VardiyaGun>>();
			int hafta = 0;
			TreeMap<String, Integer> vgHaftaMap = new TreeMap<String, Integer>();
			List<PersonelDenklestirmeTasiyici> personelDenklestirmeleri = personelDenklestirmeTasiyici.getPersonelDenklestirmeleri();
			Calendar cal1 = Calendar.getInstance();
			Date bugun = PdksUtil.getDate(cal1.getTime());
			for (PersonelDenklestirmeTasiyici personelDenklestirmeTasiyici2 : personelDenklestirmeleri) {
				++hafta;
				List<VardiyaGun> vardiyaGunList = personelDenklestirmeTasiyici2.getVardiyalar();
				if (vardiyaGunList == null)
					continue;
				for (VardiyaGun vardiyaGun : vardiyaGunList) {
					String key = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "yyyyMMdd");
					boolean haftaTatil = vardiyaGun.getVardiya() != null && vardiyaGun.getVardiya().isHaftaTatil();
					if (vardiyaGun.isAyinGunu() && vardiyaGun.getVardiya() != null && (haftaTatil || bugun.after(vardiyaGun.getVardiyaDate()))) {
						cal1.setTime(vardiyaGun.getVardiyaDate());
						if (haftaTatil || (vardiyaGun.getVardiya().isCalisma() && (vardiyaGun.getVersion() < 0 || vardiyaGun.getHareketler() == null || vardiyaGun.getHareketler().isEmpty()))) {
							if (haftaTatil)
								vgHaftaMap.put(key, hafta);
							else if (haftaTatil == false && (vardiyaGun.getHareketler() == null || vardiyaGun.getHareketler().isEmpty())) {
								List<VardiyaGun> vList = haftaMap.containsKey(hafta) ? haftaMap.get(hafta) : new ArrayList<VardiyaGun>();
								if (vList.isEmpty())
									haftaMap.put(hafta, vList);
								vList.add(vardiyaGun);
							}
							vgMap.put(key, vardiyaGun);
						}

					}

				}
			}
			if (!haftaMap.isEmpty()) {
				for (String key : vgHaftaMap.keySet()) {
					VardiyaGun vardiyaGun = vgMap.get(key);
					hafta = vgHaftaMap.get(key);
					if ((vardiyaGun.getVardiyaDate().after(bugun) || vardiyaGun.getHareketler() != null) && haftaMap.containsKey(hafta)) {
						List<VardiyaGun> vList = haftaMap.get(hafta);
						if (vList.size() == 1) {
							Vardiya vardiyaHafta = vardiyaGun.getVardiya();
							VardiyaGun vardiyaCalismaGun = vList.get(0);
							Vardiya vardiyaCalisma = vardiyaCalismaGun.getVardiya();
							logger.debug(vardiyaCalismaGun.getVardiyaKeyStr() + " " + key);
							vardiyaGun.setVardiya(vardiyaCalisma);
							vardiyaGun.setVersion(-1);
							vardiyaCalismaGun.setVardiya(vardiyaHafta);
							vardiyaCalismaGun.setVersion(0);
							saveOrUpdate(vardiyaGun);
							saveOrUpdate(vardiyaCalismaGun);
							flush = true;
						}
					}
				}
			}
			if (flush) {
				if (userLogin.getLogin())
					PdksUtil.addMessageWarn("Hafta tatilleri güncellendi, 'Fazla Mesai Getir' tekrar çalıştırın.");
				sessionFlush();
			}

		}
	}

	/**
	 * @param puantaj
	 * @return
	 */
	public boolean ciftBolumCalisan(AylikPuantaj puantaj) {
		boolean cal = false;
		if (puantaj != null && ciftBolumCalisanMap != null) {
			cal = ciftBolumCalisanMap.containsKey(puantaj.getPdksPersonel().getId());
		}
		return cal;
	}

	/**
	 * @param puantaj
	 * @return
	 */
	public String ciftAylikPuantajPersonelHTML(AylikPuantaj puantaj) {
		String str = "";
		if (puantaj != null) {
			Personel islemPersonel = ciftBolumCalisanMap.get(puantaj.getPdksPersonel().getId());
			if (islemPersonel != null)
				str = getPersonelAciklamaHTML(islemPersonel);
		}
		return str;
	}

	/**
	 * @param personel
	 * @return
	 */
	public String getPersonelAciklamaHTML(Personel personel) {
		String str = "";
		if (personel != null) {
			str = "<TABLE>";
			str = "<tr><td><b>" + ortakIslemler.sirketAciklama() + "</b></td><td><b> : </b>" + personel.getSirket().getAd() + "</td></tr>";
			str += "<tr><td><b>" + bolumAciklama + "</b></td><td><b> : </b>" + personel.getEkSaha3().getAciklama() + "</td></tr>";
			str += "<tr><td><b>" + ortakIslemler.personelNoAciklama() + "</b></td><td><b> : </b>" + personel.getPdksSicilNo() + "</td></tr>";
			str += "<tr><td><b>Adı Soyadı</b></td><td><b> : </b>" + personel.getAdSoyad() + "</td></tr>";
			str += "</TABLE>";
		}
		return str;
	}

	/**
	 * @return
	 */
	public String ciftBolumCalisanHareketGuncelle() {
		PersonelView personelView = null;
		Tanim ciftBolumCalisanKartNedenTanim = null;
		KapiView manuelGiris = null, manuelCikis = null;
		for (HareketKGS hareketKGS : hareketler) {
			if (hareketKGS.isCheckBoxDurum()) {
				HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(null, session);
				manuelGiris = manuelKapiMap.get(Kapi.TIPI_KODU_GIRIS);
				manuelCikis = manuelKapiMap.get(Kapi.TIPI_KODU_CIKIS);
				manuelKapiMap = null;
				ciftBolumCalisanKartNedenTanim = ciftBolumTanimGetir();
				break;
			}
		}
		String islemAciklama = "Seçili kayıt yok!";
		if (ciftBolumCalisanKartNedenTanim != null) {
			Long nedenId = ciftBolumCalisanKartNedenTanim.getId();
			Personel seciliPersonel = seciliAylikPuantaj.getPdksPersonel();
			Personel islemPersonel = ciftBolumCalisanMap.get(seciliPersonel.getId());
			personelView = new PersonelView();
			personelView.setId(islemPersonel.getPersonelKGS().getId());
			String seciliAciklama = seciliPersonel.getPdksSicilNo() + " - " + seciliPersonel.getAdSoyad() + " aktarım için iptal edildi.";
			islemAciklama = islemPersonel.getPdksSicilNo() + " - " + islemPersonel.getAdSoyad() + " hareket aktarımı yapıldı.";
			long pdksId = 0l;
			for (HareketKGS hareketKGS : hareketler) {
				if (hareketKGS.isCheckBoxDurum()) {
					KapiView kapiView = hareketKGS.getKapiView();
					if (kapiView != null) {
						if (kapiView.getKapi() != null && manuelCikis != null && manuelGiris != null) {
							Kapi kapi = kapiView.getKapi();
							if (kapi.isGirisKapi())
								kapiView = manuelGiris;
							else if (kapi.isCikisKapi())
								kapiView = manuelCikis;
						}
					}
					Long id = pdksEntityController.hareketEkle(kapiView, personelView, hareketKGS.getOrjinalZaman(), userLogin, nedenId, islemAciklama, session);
					if (id != null) {

						pdksEntityController.hareketSil(Long.parseLong(hareketKGS.getId().substring(1)), pdksId, userLogin, nedenId, seciliAciklama, hareketKGS.getKgsSirketId(), session);
					}
				}
			}
		}
		if (personelView != null) {
			sessionFlush();
			if (userLogin.getLogin())
				PdksUtil.addMessageInfo(islemAciklama);
			fillPersonelDenklestirmeList(null);
		} else if (userLogin.getLogin())
			PdksUtil.addMessageWarn(islemAciklama);
		return "";
	}

	/**
	 * @param puantaj
	 * @return
	 */
	public String ciftBolumCalisanSec() {
		for (HareketKGS hareketKGS : hareketler)
			hareketKGS.setCheckBoxDurum(checkBoxDurum);
		return "";
	}

	/**
	 * @param puantaj
	 * @return
	 */
	public String ciftBolumCalisanGetir(AylikPuantaj puantaj) {
		hareketler.clear();
		seciliAylikPuantaj = puantaj;
		tmpAlan = "";
		checkBoxDurum = !userLogin.isAdmin();
		if (puantaj != null && ciftBolumCalisanHareketMap != null && ciftBolumCalisanHareketMap.containsKey(puantaj.getPdksPersonel().getId())) {
			Personel personel = ciftBolumCalisanMap.get(puantaj.getPdksPersonel().getId());
			tmpAlan = getPersonelAciklamaHTML(personel);
			List<HareketKGS> list = ciftBolumCalisanHareketMap.get(puantaj.getPdksPersonel().getId());
			for (HareketKGS hareketKGS : list) {
				hareketKGS.setCheckBoxDurum(!userLogin.isAdmin());
				hareketler.add(hareketKGS);
			}
		}
		return "";
	}

	/**
	 * @param puantajList
	 */
	private List ciftBolumCalisanKontrol(List<AylikPuantaj> puantajList) {
		List<String> vGunList = new ArrayList<String>();
		String ciftBolumCalisanStr = ortakIslemler.getParameterKey("ciftBolumCalisan");
		if (ikRole && denklestirmeAy != null && seciliBolum != null && PdksUtil.hasStringValue(ciftBolumCalisanStr)) {
			List<String> ciftBolumList = PdksUtil.getListByString(ciftBolumCalisanStr, null);
			Tanim ciftBolumCalisanKartNedenTanim = null;
			if (ciftBolumList.contains(seciliBolum.getErpKodu())) {
				ciftBolumCalisanKartNedenTanim = ciftBolumTanimGetir();
				if (ciftBolumCalisanKartNedenTanim != null) {
					TreeMap<Long, AylikPuantaj> puantajMap = new TreeMap<Long, AylikPuantaj>();
					for (Iterator iterator = puantajList.iterator(); iterator.hasNext();) {
						AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
						puantajMap.put(aylikPuantaj.getPdksPersonel().getId(), aylikPuantaj);
					}
					StringBuffer sb = new StringBuffer();
					sb.append("SP_CIFT_PERSONEL");
					LinkedHashMap<String, Object> linkedHashMap = new LinkedHashMap<String, Object>();
					linkedHashMap.put("donemId", denklestirmeAy.getId());
					if (session != null)
						linkedHashMap.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<Object[]> alanList = null;
					try {
						alanList = pdksEntityController.execSPList(linkedHashMap, sb, null);
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (alanList != null) {
						HashMap<Long, Long> idMap = new HashMap<Long, Long>();
						for (Iterator iterator = alanList.iterator(); iterator.hasNext();) {
							Object[] objects = (Object[]) iterator.next();
							try {
								Long perId = ((BigDecimal) objects[0]).longValue(), perKGSNewId = ((BigDecimal) objects[1]).longValue(), perNewId = ((BigDecimal) objects[2]).longValue();
								if (puantajMap.containsKey(perId)) {
									AylikPuantaj aylikPuantaj = puantajMap.get(perId);
									TreeMap<String, List<HareketKGS>> hareketMap = new TreeMap<String, List<HareketKGS>>();
									for (VardiyaGun vardiyaGun : aylikPuantaj.getVardiyalar()) {
										if (vardiyaGun.getIzin() != null) {
											if (vardiyaGun.getHareketler() != null) {
												vGunList.add(vardiyaGun.getVardiyaKeyStr());
												for (HareketKGS hareketKGS : vardiyaGun.getHareketler()) {
													if (hareketKGS.getId() != null && (hareketKGS.getFazlaMesai() == null || hareketKGS.getFazlaMesai().doubleValue() == 0.0d) && hareketKGS.getId().startsWith(HareketKGS.GIRIS_ISLEM_YAPAN_SIRKET_KGS)) {
														String key = PdksUtil.convertToDateString(hareketKGS.getOrjinalZaman(), "yyyyMMdd");
														List<HareketKGS> list = hareketMap.containsKey(key) ? hareketMap.get(key) : new ArrayList<HareketKGS>();
														if (list.isEmpty())
															hareketMap.put(key, list);

														list.add(hareketKGS);
													}
												}
											}
										}
									}
									if (!hareketMap.isEmpty()) {
										List<Long> perIdList = new ArrayList<Long>();
										perIdList.add(perKGSNewId);
										List<HareketKGS> list = ortakIslemler.getHareketBilgileri(null, perIdList, aylikPuantajDefault.getIlkGun(), aylikPuantajDefault.getSonGun(), HareketKGS.class, session);
										for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
											HareketKGS hareketKGS = (HareketKGS) iterator2.next();
											if (hareketKGS.getId() != null && hareketKGS.getKapiView() != null && hareketKGS.getKapiView().getKapi().getPdks()) {
												String key = PdksUtil.convertToDateString(hareketKGS.getOrjinalZaman(), "yyyyMMdd");
												if (hareketMap.containsKey(key))
													hareketMap.remove(key);
											}
										}
									}
									if (!hareketMap.isEmpty()) {
										List<HareketKGS> list = new ArrayList<HareketKGS>();
										for (String key : hareketMap.keySet())
											list.addAll(hareketMap.get(key));

										idMap.put(perId, perNewId);
										ciftBolumCalisanHareketMap.put(perId, list);
									} else
										hareketMap = null;

								}

							} catch (Exception ex) {
								logger.error(ex);
							}

						}
						if (!idMap.isEmpty()) {
							List dataIdList = new ArrayList(idMap.values());

							TreeMap<Long, Personel> perMap = pdksEntityController.getSQLParamByFieldMap(Personel.TABLE_NAME, Personel.COLUMN_NAME_ID, dataIdList, Personel.class, "getId", false, session);

							for (Long key : idMap.keySet()) {
								Long kgID = idMap.get(key);
								if (perMap.containsKey(kgID))
									ciftBolumCalisanMap.put(key, perMap.get(kgID));
							}
						}
						idMap = null;
						alanList = null;
					}
				}
			}
			ciftBolumList = null;
		}
		return vGunList;
	}

	/**
	 * @return
	 */
	private Tanim ciftBolumTanimGetir() {
		Tanim tanim = null;
		String kodu = ortakIslemler.getParameterKey("ciftBolumCalisanKartNedenKodu");
		if (PdksUtil.hasStringValue(kodu) == false && adminRole)
			kodu = "cbc";
		if (PdksUtil.hasStringValue(kodu)) {

			tanim = (Tanim) ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_HAREKET_NEDEN, kodu, session);
			if (tanim != null && !tanim.getDurum())
				tanim = null;
		}
		return tanim;
	}

	/**
	 * 
	 */
	@Transactional
	private void saveLastParameter(String inputPersonelNo) {
		LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
		lastMap.put("yil", "" + yil);
		lastMap.put("ay", "" + ay);
		if (departmanId != null) {
			lastMap.put("departmanId", "" + departmanId);
			lastMap.put("departman", ortakIslemler.getSelectItemText(departmanId, departmanList));
		}
		if (sirketId != null) {
			lastMap.put("sirketId", "" + sirketId);
			lastMap.put("sirket", ortakIslemler.getSelectItemText(sirketId, pdksSirketList));
		}
		if (tesisId != null) {
			lastMap.put("tesisId", "" + tesisId);
			lastMap.put("tesis", ortakIslemler.getSelectItemText(tesisId, tesisList));
		}
		if (seciliEkSaha3Id != null) {
			lastMap.put("bolumId", "" + seciliEkSaha3Id);
			lastMap.put("bolum", ortakIslemler.getSelectItemText(seciliEkSaha3Id, gorevYeriList));
		}

		if (seciliEkSaha4Id != null) {
			lastMap.put("altBolumId", "" + seciliEkSaha4Id);
			lastMap.put("altBolum", ortakIslemler.getSelectItemText(seciliEkSaha4Id, altBolumList));
		}

		if (hataliPuantajGoster != null)
			lastMap.put("hataliPuantajGoster", "" + hataliPuantajGoster);
		if (inputPersonelNo != null)
			lastMap.put("inputPersonelNo", "" + inputPersonelNo);
		if (ikRole) {
			if (!sadeceFazlaMesai)
				lastMap.put("sadeceFazlaMesai", sadeceFazlaMesai);
			if (PdksUtil.hasStringValue(sicilNo))
				lastMap.put("sicilNo", sicilNo.trim());
		}
		lastMap.put("sayfaURL", sayfaURL);
		try {
			ortakIslemler.saveLastParameter(lastMap, session);
		} catch (Exception e) {
		}
	}

	/**
	 * @param aylikPuantaj
	 * @param denklestirmeDinamikAlan
	 * @return
	 */
	private boolean devamlilikPrimiHesapla(AylikPuantaj aylikPuantaj, PersonelDenklestirmeDinamikAlan denklestirmeDinamikAlan) {
		Boolean islemDurum = aylikPuantaj.getPersonelDenklestirme().getDurum(), flush = Boolean.FALSE;
		CalismaModeli cm = denklestirmeDinamikAlan.getPersonelDenklestirme().getCalismaModeli();
		boolean cumartesiCalisiyor = cm.isHaftaTatilVar();
		List<VardiyaGun> vardiyalar = aylikPuantaj.getVardiyalar();
		int adet = 0;
		for (VardiyaGun vardiyaGun : vardiyalar) {
			if (vardiyaGun.isAyinGunu()) {
				Vardiya vardiya = vardiyaGun.getVardiya();
				if (vardiyaGun.getId() == null || vardiya == null || vardiya.getId() == null) {
					adet = 0;
					break;
				}
				++adet;
				if (vardiyaGun.getIzin() != null) {
					IzinTipi izinTipi = vardiyaGun.getIzin().getIzinTipi();
					if (devamlilikPrimIzinTipleri.contains(izinTipi.getIzinTipiTanim().getErpKodu())) {
						adet = 0;
						break;
					}
					continue;
				}
				if (vardiya.isOffGun()) {
					int haftaninGunu = vardiyaGun.getHaftaninGunu();
					boolean cumartesi = haftaninGunu == Calendar.SATURDAY, pazar = haftaninGunu == Calendar.SUNDAY;
					if ((pazar == false && cumartesi == false) || (cumartesiCalisiyor)) {
						adet = 0;
						break;
					}
				}

			}
		}
		if (adet == 0)
			islemDurum = Boolean.FALSE;
		logger.debug(aylikPuantaj.getPersonelDenklestirme().getPersonel().getAdSoyad() + " " + islemDurum);
		if (!islemDurum.equals(denklestirmeDinamikAlan.getIslemDurum())) {
			denklestirmeDinamikAlan.setIslemDurum(islemDurum);
			saveOrUpdate(denklestirmeDinamikAlan);
			flush = Boolean.TRUE;
		}

		return flush;
	}

	/**
	 * @param kodu
	 * @return
	 */
	private Tanim denklestirmeMantiksalBilgiBul(String kodu) {

		Tanim tanim = (Tanim) ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_PERSONEL_DENKLESTIRME_DINAMIK_DURUM, kodu, session);

		return tanim;
	}

	/**
	 * @param vGunList
	 * @param izinMap
	 */
	private void izinCalismaUyariMesajiOlustur(List<String> vGunList, LinkedHashMap<Long, PersonelIzin> izinMap) {
		List<String> strList = new ArrayList<String>();
		try {
			Personel izinSahibiTEK = null;
			for (Long izinId : izinMap.keySet()) {
				PersonelIzin izin = izinMap.get(izinId);
				Personel izinSahibi = izin.getIzinSahibi();
				izinSahibiTEK = izinSahibi;
				String izinStr = izinSahibi.getPdksSicilNo() + " " + ortakIslemler.personelNoAciklama() + " " + izinSahibi.getAdSoyad() + " ait " + userLogin.dateTimeFormatla(izin.getBaslangicZamani()) + " - ";
				boolean tekGun = PdksUtil.tarihKarsilastirNumeric(izin.getBaslangicZamani(), izin.getBitisZamani()) == 0;
				if (!tekGun)
					izinStr += userLogin.dateTimeFormatla(izin.getBitisZamani()) + " tarihleri arasındaki ";
				else
					izinStr += userLogin.timeFormatla(izin.getBitisZamani()) + " tarihinde ";
				boolean devam = true;
				if (izin.getCalisilanGunler() != null && !izin.getCalisilanGunler().isEmpty()) {
					if (!tekGun) {
						String virgul = "";
						devam = false;
						for (Iterator iterator = izin.getCalisilanGunler().iterator(); iterator.hasNext();) {
							VardiyaGun vGun = (VardiyaGun) iterator.next();
							if (vGunList.contains(vGun.getVardiyaKeyStr())) {
								iterator.remove();
								continue;
							}
							devam = true;
							izinStr += virgul + PdksUtil.convertToDateString(vGun.getVardiyaDate(), "d MMMMM EEEEE");
							virgul = ", ";
						}
						if (!izin.getCalisilanGunler().isEmpty()) {
							if (izinStr.indexOf(",") > 0) {
								int ind = izinStr.lastIndexOf(",");
								String str1 = izinStr.substring(0, ind), str2 = izinStr.substring(ind + 1);
								izinStr = str1 + " ve" + str2 + " günlerinde ";
							} else
								izinStr += " gününde ";
						}
					}
					izinStr = PdksUtil.replaceAllManuel(izinStr + " " + izin.getIzinTipiAciklama() + " olmasına rağmen  hatalı giriş mevcuttur.", "  ", " ");
				}
				if (devam) {
					if (userLogin.getLogin())
						PdksUtil.addMessageAvailableWarn(izinStr);
					String str = izinStr + " ( Izin Id : " + izinId + " )";
					if (userLogin.getLogin())
						logger.info(userLogin.getAdSoyad() + " --> " + str + " " + izinSahibi.getAdSoyad());
					if (izinCalismayanMailGonder && !izinliCalisilanGunler.contains(str)) {
						strList.add(str);
						startupAction.addIzinliCalisilanGunlerList(str);
					}
				}

			}
			if (!strList.isEmpty()) {
				if (bolumAciklama == null)
					fillEkSahaTanim();
				MailObject mail = new MailObject();
				String konu = (strList.size() > 1 ? "" : izinSahibiTEK.getPdksSicilNo() + " " + ortakIslemler.personelNoAciklama() + " " + izinSahibiTEK.getAdSoyad() + " ait ");
				mail.setSubject(konu + (denklestirmeAy != null ? denklestirmeAy.getYil() + " - " + denklestirmeAy.getAyAdi() + " dönemi " : "") + "İzin gününde hatalı girişler");
				String uolStr = strList.size() > 1 ? "OL" : "UL";
				StringBuffer sb = new StringBuffer();
				sb.append("<p align=\"left\" style=\"width: 90%\">");
				sb.append("<TABLE style=\"width: 80%\">");
				if (sirketId != null)
					sb.append("<TR><TD  nowrap><B>" + ortakIslemler.sirketAciklama() + "</B></TD><TD  nowrap><B> : </B>" + PdksUtil.getSelectItemLabel(sirketId, pdksSirketList) + "</TD>");
				if (sirket != null && sirket.isTesisDurumu() && tesisId != null)
					sb.append("<TR><TD  nowrap><B>" + ortakIslemler.tesisAciklama() + "</B></TD><TD  nowrap><B> : </B>" + PdksUtil.getSelectItemLabel(tesisId, tesisList) + "</TD>");
				if (seciliEkSaha3Id != null)
					sb.append("<TR><TD  nowrap><B>" + bolumAciklama + "</B></TD><TD  nowrap><B> : </B>" + PdksUtil.getSelectItemLabel(seciliEkSaha3Id, gorevYeriList) + "</TD>");
				if (ekSaha4Tanim != null && seciliEkSaha4Id != null && seciliEkSaha4Id.longValue() > 0L)
					sb.append("<TR><TD  nowrap><B>" + ekSaha4Tanim.getAciklama() + "</B></TD><TD  nowrap><B> : </B>" + PdksUtil.getSelectItemLabel(seciliEkSaha4Id, altBolumList) + "</TD>");
				if (PdksUtil.hasStringValue(sicilNo))
					sb.append("<TR><TD  nowrap><B>" + ortakIslemler.personelNoAciklama() + "</B></TD><TD  nowrap><B> : </B>" + sicilNo.trim() + "</TD>");
				sb.append("</TABLE>");
				sb.append("<" + uolStr + ">");
				boolean renkUyari = true;
				for (String string : strList) {
					sb.append("<LI class=\"" + (renkUyari ? "odd" : "even") + "\" style=\"text-align: left;\">" + string + "</LI>");
					renkUyari = !renkUyari;
				}
				sb.append("</" + uolStr + ">");
				sb.append("</p>");
				User admin = userLogin.isAdmin() ? userLogin : ortakIslemler.getSistemAdminUser(session);
				mail.getToList().add(admin.getMailPersonel());
				mail.setBody(sb.toString());
				sb = null;
				ortakIslemler.mailSoapServisGonder(false, mail, null, null, session);
			}
		} catch (Exception e) {
		}
		strList = null;
	}

	/**
	 * @param per
	 * @param vardiya
	 * @return
	 */
	public Integer getVardiyaAdet(Personel per, Vardiya vardiya) {
		Integer adet = fazlaMesaiOrtakIslemler.getVardiyaAdet(izinTipiPersonelVardiyaMap, per, vardiya);
		return adet;
	}

	/**
	 * @param aylikPuantajSablon
	 * @param puantajDenklestirmeList
	 * @return
	 */
	private HashMap<Long, Boolean> getPersonelDurumMap(AylikPuantaj aylikPuantajSablon, List<AylikPuantaj> puantajDenklestirmeList) {
		HashMap<Long, Boolean> personelDurumMap = new HashMap<Long, Boolean>();
		String personelDurumKontrol = ortakIslemler.getParameterKey("personelDurumKontrol");
		if (PdksUtil.hasStringValue(personelDurumKontrol) || userLogin.isAdmin()) {
			List<Long> tempList = new ArrayList<Long>();
			for (Iterator iterator1 = puantajDenklestirmeList.iterator(); iterator1.hasNext();) {
				AylikPuantaj puantaj = (AylikPuantaj) iterator1.next();
				PersonelDenklestirme personelDenklestirme = puantaj.getPersonelDenklestirme();
				if (personelDenklestirme == null)
					continue;
				Personel personel = puantaj.getPdksPersonel();
				if (personelDenklestirme.getFazlaMesaiIzinKullan() != null && personelDenklestirme.getFazlaMesaiIzinKullan() && !personel.isCalisiyorGun(aylikPuantajSablon.getSonGun())) {
					tempList.add(personelDenklestirme.getId());
				}
			}
			if (!tempList.isEmpty()) {
				String fieldName = "p";
				HashMap fields = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select distinct PD.* from " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P1 " + PdksEntityController.getJoinLOCK() + " on P1." + Personel.COLUMN_NAME_ID + " = PD." + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
				sb.append(" inner join " + PersonelKGS.TABLE_NAME + " K1 " + PdksEntityController.getJoinLOCK() + " on K1." + PersonelKGS.COLUMN_NAME_ID + " = P1." + Personel.COLUMN_NAME_KGS_PERSONEL + " and COALESCE(K1.TC_KIMLIK_NO,'')<>'' ");
				sb.append(" inner join " + PersonelKGS.TABLE_NAME + " K2 " + PdksEntityController.getJoinLOCK() + " on K1.TC_KIMLIK_NO=K2.TC_KIMLIK_NO and K1." + PersonelKGS.COLUMN_NAME_ID + " <> K2." + PersonelKGS.COLUMN_NAME_ID);
				sb.append(" inner join " + Personel.TABLE_NAME + " P2 " + PdksEntityController.getJoinLOCK() + " on P2." + Personel.COLUMN_NAME_KGS_PERSONEL + " = K2." + PersonelKGS.COLUMN_NAME_ID + " and P2." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " > P1."
						+ Personel.COLUMN_NAME_SSK_CIKIS_TARIHI);
				sb.append(" inner join " + PersonelDenklestirme.TABLE_NAME + " PY " + PdksEntityController.getJoinLOCK() + " on PY.PERSONEL_ID = P2." + Personel.COLUMN_NAME_ID + " and PY." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = PD." + PersonelDenklestirme.COLUMN_NAME_DONEM
						+ " and PY.FAZLA_MESAI_IZIN_KULLAN = 1 ");
				sb.append(" where PD." + PersonelDenklestirme.COLUMN_NAME_ID + " :" + fieldName);
				fields.put(fieldName, tempList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				try {
					List<PersonelDenklestirme> denkList = pdksEntityController.getSQLParamList(tempList, sb, fieldName, fields, PersonelDenklestirme.class, session);
					for (PersonelDenklestirme personelDenklestirme : denkList) {
						personelDurumMap.put(personelDenklestirme.getId(), Boolean.FALSE);
					}

					denkList = null;
				} catch (Exception e) {
					logger.error(sb.toString());
				}
				fields = null;
			}
			tempList = null;
		}
		return personelDurumMap;
	}

	/**
	 * @param hareketler
	 */
	private void manuelHareketSil(List<HareketKGS> hareketler) {
		if (hareketler != null) {
			for (Iterator iterator = hareketler.iterator(); iterator.hasNext();) {
				HareketKGS hareketKGS = (HareketKGS) iterator.next();
				if (hareketKGS.getId() != null && hareketKGS.getId().startsWith(HareketKGS.AYRIK_HAREKET))
					iterator.remove();
			}
		}

	}

	/**
	 * @return
	 */
	public String ayrikKayitlariOlustur() {
		HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(null, session);
		Tanim neden = null;
		User sistemUser = null;
		if (PdksUtil.isSistemDestekVar()) {
			neden = ortakIslemler.getOtomatikKapGirisiNeden(session);
			if (neden != null)
				sistemUser = ortakIslemler.getSistemAdminUser(session);
		}
		List<AylikPuantaj> list = new ArrayList<AylikPuantaj>(aylikPuantajList);
		boolean devam = false;
		List<VardiyaGun> vardiyaList = new ArrayList<VardiyaGun>();
		for (AylikPuantaj aylikPuantaj : list) {
			if (aylikPuantaj.isAyrikHareketVar() == false)
				continue;

			vardiyaList.addAll(aylikPuantaj.getVardiyalar());
			int ayrikVar = 0;
			VardiyaGun sonrakiAyrikGun = null;
			for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
				VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
				if (vardiyaGun.isAyinGunu() && vardiyaGun.getVardiya() != null)
					sonrakiAyrikGun = vardiyaGun;
			}
			if (sonrakiAyrikGun != null && sonrakiAyrikGun.getSonrakiVardiyaGun() != null) {
				sonrakiAyrikGun = sonrakiAyrikGun.getSonrakiVardiyaGun();
				if (sonrakiAyrikGun.getVardiya() != null && sonrakiAyrikGun.isAyinGunu() == false && sonrakiAyrikGun.isAyrikHareketVar())
					vardiyaList.add(sonrakiAyrikGun);
			}
			for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
				VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
				boolean ayinGunu = vardiyaGun.isAyinGunu();
				if (!ayinGunu)
					ayinGunu = vardiyaGun.getOncekiVardiyaGun().isAyinGunu();
				if (ayinGunu && vardiyaGun.getId() != null) {
					if (vardiyaGun.isAyrikHareketVar()) {
						manuelHareketSil(vardiyaGun.getGirisHareketleri());
						manuelHareketSil(vardiyaGun.getCikisHareketleri());
						manuelHareketSil(vardiyaGun.getHareketler());
						++ayrikVar;
					} else if (ayrikVar == 0)
						iterator.remove();
				} else
					iterator.remove();

			}
			if (ayrikVar > 1) {
				try {
					LinkedHashMap<String, Object> addManuelGirisCikisHareketlerMap = new LinkedHashMap<String, Object>();
					addManuelGirisCikisHareketlerMap.put("manuelKapiMap", manuelKapiMap);
					addManuelGirisCikisHareketlerMap.put("neden", neden);
					addManuelGirisCikisHareketlerMap.put("sistemUser", sistemUser);
					addManuelGirisCikisHareketlerMap.put("vardiyalar", vardiyaList);
					addManuelGirisCikisHareketlerMap.put("hareketKaydet", true);
					addManuelGirisCikisHareketlerMap.put("oncekiVardiyaGun", null);
					ortakIslemler.addManuelGirisCikisHareketler(ortakIslemler.mapBosVeriSil(addManuelGirisCikisHareketlerMap, ""), session);
					devam = true;
				} catch (Exception e) {
					logger.error(e);
					e.printStackTrace();
				}

			}
			vardiyaList.clear();
		}
		vardiyaList = null;
		list = null;
		if (devam)
			fillPersonelDenklestirmeList(null);
		return "";
	}

	/**
	 * @param islemPuantaj
	 * @param vGun
	 * @param paramsMap
	 */
	private void vardiyaGunKontrol(AylikPuantaj islemPuantaj, VardiyaGun vGun, HashMap<String, Object> paramsMap) {
		boolean fazlaMesaiTalepVardiyaOnayliDurum = false;
		vGun.setAksamKatSayisi(0.0d);
		Date aksamVardiyaBitisZamani = null, aksamVardiyaBaslangicZamani = null;
		Vardiya vardiya = vGun.getVardiya();
		String key1 = vGun.getVardiyaDateStr(), vardiyaKey = vGun.getVardiyaKeyStr();
		if (denklestirmeAyDurum && vGun.getGecersizHareketler() != null) {
			if (vGun.getFazlaMesailer() != null) {
				for (Iterator iterator = vGun.getGecersizHareketler().iterator(); iterator.hasNext();) {
					HareketKGS hareketKGS = (HareketKGS) iterator.next();
					for (PersonelFazlaMesai pfm : vGun.getFazlaMesailer()) {
						if (pfm.getHareketId().equals(hareketKGS.getId())) {
							iterator.remove();
							break;
						}

					}
				}
				if (vGun.getGecersizHareketler().isEmpty())
					vGun.setGecersizHareketler(null);

			}
			logger.debug("");
		}

		if (islemPuantaj.isGebeDurum() == false && (vGun.isGebeMi() || vGun.isGebePersonelDonemselDurum())) {
			gebeGoster = true;
			islemPuantaj.setGebeDurum(true);
		}
		if (islemPuantaj.isSuaDurum() == false && (vGun.isSutIzniVar() || vGun.isSutIzniPersonelDonemselDurum())) {
			sutIzniGoster = true;
			islemPuantaj.setSutIzniDurumu(true);
		}
		if (vardiya.isIzin()) {
			izinGoster = true;
		}

		// boolean aksamVardiyasi = vardiya.isAksamVardiyasi();
		if (perCalismaModeli != null && perCalismaModeli.getGeceCalismaOdemeVar().equals(Boolean.TRUE)) {
			if (aksamVardiyaBitSaat != null && aksamVardiyaBitDakika != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(vGun.getVardiyaDate());
				cal.set(Calendar.HOUR_OF_DAY, aksamVardiyaBitSaat);
				cal.set(Calendar.MINUTE, aksamVardiyaBitDakika);
				if (vardiya.getBasDonem() > vardiya.getBitDonem() && vardiya.isCalisma())
					cal.add(Calendar.DATE, 1);
				aksamVardiyaBitisZamani = cal.getTime();
			}
			if (aksamVardiyaBasSaat != null && aksamVardiyaBasDakika != null) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(vGun.getVardiyaDate());
				cal.set(Calendar.HOUR_OF_DAY, aksamVardiyaBasSaat);
				cal.set(Calendar.MINUTE, aksamVardiyaBasDakika);
				if (vardiya.getBasDonem() < vardiya.getBitDonem() || vardiya.isCalisma() == false)
					cal.add(Calendar.DATE, -1);
				aksamVardiyaBaslangicZamani = cal.getTime();
			}
		}
		List<HareketKGS> girisHareketleri = null, cikisHareketleri = null, hareketler = null;
		vGun.setFazlaMesaiOnayla(null);
		if (vGun.getGecerliHareketler() != null || vGun.getTatil() != null)
			ortakIslemler.hareketleriDuzenle(vGun, vGun.getGecerliHareketler() == null ? vGun.getHareketler() : vGun.getGecerliHareketler(), Boolean.TRUE);
		else if (vGun.getHareketler() != null) {
			int adet = vGun.getHareketler().size();
			for (Iterator iterator = vGun.getHareketler().iterator(); iterator.hasNext();) {
				HareketKGS hareket = (HareketKGS) iterator.next();
				if (hareket.getOncekiGun() != null && hareket.getOncekiGun().booleanValue())
					iterator.remove();
			}
			if (adet != vGun.getHareketler().size())
				ortakIslemler.hareketleriDuzenle(vGun, vGun.getHareketler(), Boolean.TRUE);
		}

		boolean hareketsiz = vGun.getVardiya() != null && vGun.getHareketler() == null && vGun.getIzin() == null;
		hareketler = vGun.getHareketler();
		if (vGun.getGirisHareketleri() != null)
			girisHareketleri = new ArrayList(vGun.getGirisHareketleri());
		if (vGun.getCikisHareketleri() != null)
			cikisHareketleri = new ArrayList(vGun.getCikisHareketleri());
		boolean goster = false;
		List<String> idList = null;
		boolean ilkGunTatil = vGun.getTatil() != null && key1.endsWith("01");
		if (ilkGunTatil)
			idList = new ArrayList<String>();

		if (hareketler != null) {
			for (HareketKGS hareketKGS : hareketler) {
				String islemYapan = "";
				if (hareketKGS.getId() != null && hareketKGS.getId().startsWith(HareketKGS.SANAL_HAREKET))
					continue;

				if (idList != null)
					idList.add(hareketKGS.getId());
				if (hareketKGS.getKapiView() != null) {
					try {
						if (hareketKGS.isManuelGiris()) {
							String kapiAdi = hareketKGS.getKapiSirket() != null ? hareketKGS.getKapiSirket().getAciklama() : kapiGirisSistemAdi;
							islemYapan = hareketKGS.getIslem().getOnaylayanUser() == null && hareketKGS.getIslem() == null ? kapiAdi : (hareketKGS.getIslem().getOnaylayanUser() != null ? hareketKGS.getIslem().getOnaylayanUser().getAdSoyad() : "");

							if (!goster)
								goster = PdksUtil.hasStringValue(islemYapan);
						}

					} catch (Exception e) {
					}

				}
				hareketKGS.setIslemYapan(islemYapan);

			}
		}
		if (goster)
			vGun.setManuelGirisHTML(manuelGirisGoster);
		islemPuantaj.setAyrikHareketVar(true);
		if (islemPuantaj != null && hareketler != null && !hareketler.isEmpty()) {
			ArrayList<HareketKGS> tumHareketler = islemPuantaj.getHareketler() != null ? islemPuantaj.getHareketler() : new ArrayList<HareketKGS>();
			if (tumHareketler.isEmpty())
				islemPuantaj.setHareketler(tumHareketler);
			tumHareketler.addAll(hareketler);
		}

		// if (haret)

		boolean fazlaMesaiOnaylaDurum = vGun.isAyrikHareketVar() == false && girisHareketleri != null && cikisHareketleri != null && girisHareketleri.size() == cikisHareketleri.size();
		if (fazlaMesaiOnaylaDurum || (vGun.isAyrikHareketVar() && vGun.getVardiya().isCalisma())) {

			int gAdet = girisHareketleri != null ? girisHareketleri.size() : -1, cAdet = cikisHareketleri != null ? cikisHareketleri.size() : -2;
			if (girisHareketleri != null) {
				for (Iterator iterator = girisHareketleri.iterator(); iterator.hasNext();) {
					HareketKGS hareketKGS = (HareketKGS) iterator.next();
					if (hareketKGS.getId() == null || hareketKGS.getId().startsWith(HareketKGS.SANAL_HAREKET) || hareketKGS.getId().startsWith(HareketKGS.AYRIK_HAREKET) || (ilkGunTatil && !idList.contains(hareketKGS.getId())))
						iterator.remove();
				}
			}
			if (cikisHareketleri != null) {
				for (Iterator iterator = cikisHareketleri.iterator(); iterator.hasNext();) {
					HareketKGS hareketKGS = (HareketKGS) iterator.next();
					if (hareketKGS.getId() == null || hareketKGS.getId().startsWith(HareketKGS.SANAL_HAREKET) || hareketKGS.getId().startsWith(HareketKGS.AYRIK_HAREKET) || (ilkGunTatil && !idList.contains(hareketKGS.getId())))
						iterator.remove();

				}
			}

			vGun.setCikisHareketleri((ArrayList<HareketKGS>) cikisHareketleri);
			vGun.setGirisHareketleri((ArrayList<HareketKGS>) girisHareketleri);

			boolean cikis = false;
			int adet = 0;
			if (hareketler != null) {
				for (HareketKGS hareketKGS : hareketler) {
					try {
						if (cikis && hareketKGS.getId() != null && hareketKGS.getKapiView().getKapi().isCikisKapi())
							++adet;
					} catch (Exception e) {
					}
					cikis = !cikis;
				}
			}
			if (adet > 0 && adet == cAdet) {
				fazlaMesaiOnaylaDurum = true;
				vGun.setFazlaMesaiOnayla(true);
			}
			if (vGun.isAyrikHareketVar())
				vGun.setHareketHatali(gAdet != cAdet);

		}

		Boolean fazlaMesaiHesapla = (Boolean) paramsMap.get("fazlaMesaiHesapla");
		Integer aksamVardiyaSayisi = (Integer) paramsMap.get("aksamVardiyaSayisi");
		Double aksamVardiyaSaatSayisi = (Double) paramsMap.get("aksamVardiyaSaatSayisi");
		Double sabahAksamCikisSaatSayisi = (Double) paramsMap.get("sabahAksamCikisSaatSayisi");
		Double resmiTatilSuresi = (Double) paramsMap.get("resmiTatilSuresi");
		Double haftaCalismaSuresi = (Double) paramsMap.get("haftaCalismaSuresi");
		String izinERPUpdateStr = ortakIslemler.getParameterKey("izinERPUpdate");
		Vardiya islemVardiya = vGun.getIslemVardiya();
		Tatil tatil = vGun.getTatil() != null ? vGun.getTatil().getOrjTatil() : null;
		PersonelIzin personelIzin = vGun.getIzin();
		Boolean izinli = vGun.isIzinli();

		Personel personel = vGun.getPersonel();
		vGun.setAksamVardiyaSaatSayisi(0d);
		vGun.setLinkAdresler(null);
		String vardiyaPlanKey = vGun.getPlanKey();

		if (planOnayDurum == false)
			vardiyaPlaniGetir(vGun, vardiyaPlanKey);

		boolean izinDurum = false;
		if (hareketler == null && vGun.getIslemVardiya() != null) {
			vGun.setHataliDurum(vGun.getIzin() == null && vGun.getIslemVardiya().isCalisma());
			izinDurum = sirketIzinGirisDurum && vGun.getIslemVardiya().isCalisma() && vGun.isZamanGelmedi() == false && vGun.getIzin() == null;
		}
		if (vGun.isAyrikHareketVar()) {
			vGun.setHareketHatali(Boolean.TRUE);
			fazlaMesaiOnaylaDurum = false;
			logger.debug(key1 + " " + vGun.getHareketDurum());
			vGun.setFazlaMesaiOnayla(null);
		} else if (vGun.isZamanGelmedi() && islemVardiya != null && islemVardiya.isCalisma() && islemVardiya.getVardiyaBitZaman().after(islemVardiya.getVardiyaFazlaMesaiBitZaman())) {
			// fazlaMesaiHesapla = Boolean.FALSE;
			vGun.setHataliDurum(Boolean.TRUE);
			if (vardiyaPlaniDurum) {
				vardiyaPlaniGetir(vGun, vardiyaPlanKey);
			}
		} else if (vGun.getHareketDurum()) {
			try {

				if ((izinli || !vGun.getVardiya().isCalisma() || vGun.getVardiya().isHaftaTatil()) && vGun.getHareketler() != null && !vGun.getHareketler().isEmpty()) {
					PersonelFazlaMesai personelFazlaMesaiGiris = null, personelFazlaMesaiCikis = null;
					if (vGun.getFazlaMesailer() != null && vGun.getHareketler() != null) {
						for (PersonelFazlaMesai fm : vGun.getFazlaMesailer()) {
							if (fm.getDurum() && fm.isOnaylandi()) {
								for (HareketKGS hareketKGS : vGun.getHareketler()) {
									try {
										if (hareketKGS.getPersonelFazlaMesai() == null && hareketKGS.getId() != null && hareketKGS.getId().equals(fm.getHareketId())) {
											hareketKGS.setPersonelFazlaMesai(fm);
											break;
										}
									} catch (Exception e) {
									}
								}
							}
						}
					}

					if (vGun.getGirisHareketleri() != null && !vGun.getGirisHareketleri().isEmpty()) {
						int i = 0;
						for (HareketKGS hareketGiris : vGun.getGirisHareketleri()) {
							HareketKGS hareketCikis = null;
							try {
								hareketCikis = vGun.getCikisHareketleri().get(i++);
							} catch (Exception e) {
								// TODO: handle exception
							}
							if (hareketGiris.getId() == null || hareketCikis == null || hareketCikis.getId() == null)
								continue;
							personelFazlaMesaiGiris = hareketGiris.getPersonelFazlaMesai();
							personelFazlaMesaiCikis = hareketCikis.getPersonelFazlaMesai();
							if (personelFazlaMesaiGiris == null && personelFazlaMesaiCikis == null) {
								fazlaMesaiHesapla = Boolean.FALSE;
								vGun.setOnayli(Boolean.FALSE);
								break;
							}
						}

					}

					if (personelFazlaMesaiGiris == null && personelFazlaMesaiCikis == null) {
						vGun.setHareketHatali(Boolean.TRUE);
						kapiGirisGetir(vGun, vardiyaPlanKey);
						if (personelIzin != null && !izinERPUpdateStr.equals("1"))
							personelIzinGirisiEkle(vGun, personelIzin, null);

					}
					personelFazlaMesaiEkle(vGun, vardiyaPlanKey);

				} else if (vGun.getVardiya().isCalisma() && izinli == false) {
					if (!vGun.getVardiya().isIcapVardiyasi()) {
						double sure = vGun.getVardiya().getNetCalismaSuresi();
						boolean sureAz = eksikSaatYuzde != null && vGun.getCalismaSuresi() < (sure * eksikSaatYuzde / 100.0d);
						if (sureAz && tatil != null) {
							if (vGun.getCikisHareketleri() != null) {
								HareketKGS cikisHareket = vGun.getCikisHareketleri().get(vGun.getCikisHareketleri().size() - 1);
								sureAz = !(cikisHareket.getZaman().after(tatil.getBasTarih()) && cikisHareket.getZaman().before(tatil.getBitTarih()));
							}

						}
						if (sureAz) {

							vGun.setHataliDurum(eksikSaatYuzde != null);
							if (!izinERPUpdateStr.equals("1")) {
								String izinKey = "perId=" + personel.getId();
								personelIzinGirisiEkle(vGun, null, izinKey);
							}
							kapiGirisGetir(vGun, vardiyaPlanKey);
							if (vGun.getGirisHareketleri() != null) {
								HareketKGS girisHareket = vGun.getGirisHareketleri().get(0);
								HareketKGS cikisHareket = vGun.getCikisHareketleri().get(0);
								boolean hatali = Boolean.TRUE;
								if (vGun.getFazlaMesailer() != null) {
									for (PersonelFazlaMesai personelFazlaMesai : vGun.getFazlaMesailer()) {
										if (vGun.isAyinGunu() && islemVardiya.isCalisma()) {
											if (personelFazlaMesai.getBasZaman().after(islemVardiya.getVardiyaTelorans1BasZaman()))
												logger.debug(vardiyaKey + " " + personelFazlaMesai.getId() + " " + personelFazlaMesai.getBasZaman() + " " + personelFazlaMesai.getBitZaman());
										}
										if (personelFazlaMesai.getHareketId().equals(girisHareket.getId()) || personelFazlaMesai.getHareketId().equals(cikisHareket.getId())) {
											hatali = Boolean.FALSE;
										}
									}
								}

								if (hatali && girisHareket.getOrjinalZaman().getTime() < islemVardiya.getVardiyaTelorans1BasZaman().getTime()) {
									personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
									fazlaMesaiHesapla = Boolean.FALSE;
									vGun.setHareketHatali(Boolean.TRUE);
									vGun.setOnayli(Boolean.FALSE);
								}
							}
							if (vGun.getCikisHareketleri() != null) {
								HareketKGS cikisHareket = vGun.getCikisHareketleri().get(vGun.getCikisHareketleri().size() - 1);
								boolean hatali = Boolean.TRUE;
								if (vGun.getFazlaMesailer() != null) {
									for (PersonelFazlaMesai personelFazlaMesai : vGun.getFazlaMesailer()) {
										if (personelFazlaMesai.getHareketId().equals(cikisHareket.getId())) {
											hatali = Boolean.FALSE;
										}
									}
								}
								if (hatali && cikisHareket.getOrjinalZaman().getTime() > islemVardiya.getVardiyaTelorans2BitZaman().getTime()) {
									personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
									fazlaMesaiHesapla = Boolean.FALSE;
									vGun.setOnayli(Boolean.FALSE);
									vGun.setHareketHatali(Boolean.TRUE);
								}
							}
							if (vGun.getHareketler() == null || vGun.getHareketler().isEmpty())
								vardiyaPlaniGetir(vGun, vardiyaPlanKey);

						}
						try {
							if (vardiyaKey.endsWith("01"))
								logger.debug(key1 + " " + vGun.getId());
							girisHareketleri = vGun.getGirisHareketleri();
							cikisHareketleri = vGun.getCikisHareketleri();
							if (vGun.getFazlaMesailer() != null) {
								List<HareketKGS> list = new ArrayList<HareketKGS>();
								if (girisHareketleri != null && !girisHareketleri.isEmpty())
									list.addAll(girisHareketleri);
								if (cikisHareketleri != null && !cikisHareketleri.isEmpty())
									list.addAll(cikisHareketleri);
								if (!list.isEmpty()) {
									HashMap<String, PersonelFazlaMesai> mesaiMap = new HashMap<String, PersonelFazlaMesai>();
									for (PersonelFazlaMesai pfm : vGun.getFazlaMesailer())
										mesaiMap.put(pfm.getHareketId(), pfm);
									for (HareketKGS hareketKGS : list) {
										if (hareketKGS.getId() != null && mesaiMap.containsKey(hareketKGS.getId()) && hareketKGS.getPersonelFazlaMesai() == null)
											hareketKGS.setPersonelFazlaMesai(mesaiMap.get(hareketKGS.getId()));
									}
									mesaiMap = null;

								}

								personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
								list = null;

							}

							int girisAdet = girisHareketleri != null ? girisHareketleri.size() : -1, cikisAdet = cikisHareketleri != null ? cikisHareketleri.size() : -1;
							if (girisAdet > 1 && cikisAdet == girisAdet && vGun.isHareketHatali() == false) {
								for (int i = 0; i < girisAdet; i++) {
									HareketKGS girisHareket = girisHareketleri.get(i), cikisHareket = cikisHareketleri.get(i);
									Date girisZaman = girisHareket != null ? girisHareket.getOrjinalZaman() : null;
									Date cikisZaman = cikisHareket != null ? cikisHareket.getOrjinalZaman() : null;
									if ((girisZaman != null && girisZaman.before(islemVardiya.getVardiyaTelorans1BasZaman())) || (cikisZaman != null && cikisZaman.after(islemVardiya.getVardiyaTelorans2BitZaman()))) {
										if (girisHareket.getPersonelFazlaMesai() == null && cikisHareket.getPersonelFazlaMesai() == null) {
											vGun.setHareketHatali(Boolean.TRUE);
											if (vGun.getLinkAdresler() == null)
												kapiGirisGetir(vGun, vardiyaPlanKey);
											if (girisZaman != null && cikisZaman != null)
												personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
											vGun.setOnayli(Boolean.FALSE);
											fazlaMesaiHesapla = Boolean.FALSE;
											vGun.setHataliDurum(Boolean.TRUE);
										}
									}
								}
							} else if (girisAdet > 0 || cikisAdet > 0) {
								HareketKGS girisHareket = girisAdet > 0 ? girisHareketleri.get(0) : null;
								HareketKGS cikisHareket = cikisAdet > 0 ? cikisHareketleri.get(0) : null;
								Date girisZaman = girisHareket != null ? girisHareket.getOrjinalZaman() : null;
								Date cikisZaman = cikisHareket != null ? cikisHareket.getOrjinalZaman() : null;
								if (girisAdet > 0) {
									if (cikisHareket == null) {
										if (vGun.isZamanGelmedi())
											cikisHareket = new HareketKGS();
									}
									if (cikisHareket == null || (girisHareket.getPersonelFazlaMesai() == null && cikisHareket.getPersonelFazlaMesai() == null && girisZaman != null && girisZaman.before(islemVardiya.getVardiyaTelorans1BasZaman()))) {
										vGun.setHareketHatali(Boolean.TRUE);
										if (vGun.getLinkAdresler() == null)
											kapiGirisGetir(vGun, vardiyaPlanKey);

										if (girisZaman != null && cikisZaman != null && girisZaman.getTime() < islemVardiya.getVardiyaTelorans1BasZaman().getTime()) {
											personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
										}
										vGun.setOnayli(Boolean.FALSE);
										fazlaMesaiHesapla = Boolean.FALSE;
										vGun.setHataliDurum(Boolean.TRUE);
									}
								}
								if (cikisAdet > 0) {
									if (girisHareket == null)
										girisHareket = new HareketKGS();
									if (girisHareket.getPersonelFazlaMesai() == null && cikisHareket.getPersonelFazlaMesai() == null && cikisZaman != null
											&& (cikisZaman.getTime() > islemVardiya.getVardiyaTelorans2BitZaman().getTime() || cikisZaman.getTime() < islemVardiya.getVardiyaTelorans1BasZaman().getTime())) {
										vGun.setHareketHatali(Boolean.TRUE);
										if (vGun.getLinkAdresler() == null)
											kapiGirisGetir(vGun, vardiyaPlanKey);

										if (cikisZaman.getTime() > islemVardiya.getVardiyaTelorans2BitZaman().getTime()) {
											personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
										}
										vGun.setOnayli(Boolean.FALSE);
										fazlaMesaiHesapla = Boolean.FALSE;
									}
								}
							}
						} catch (Exception e1) {
							e1.printStackTrace();
							logger.error(vardiyaKey + " " + e1.getMessage());
						}

					} else {
						if (vGun.getGirisHareketleri() != null) {
							for (Iterator iterator2 = vGun.getGirisHareketleri().iterator(); iterator2.hasNext();) {
								HareketKGS girisHareket = (HareketKGS) iterator2.next();
								boolean hatali = Boolean.TRUE;
								if (vGun.getFazlaMesailer() != null) {
									for (PersonelFazlaMesai personelFazlaMesai : vGun.getFazlaMesailer()) {
										if (personelFazlaMesai.getHareketId().equals(girisHareket.getId())) {
											hatali = Boolean.FALSE;

										}
									}

								}
								if (hatali) {
									personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
									fazlaMesaiHesapla = Boolean.FALSE;
									vGun.setHareketHatali(Boolean.TRUE);
									vGun.setOnayli(Boolean.FALSE);
								}
							}
						}
					}
				}
				if (vGun.getVardiya() != null && vGun.getResmiTatilSure() > 0)
					resmiTatilSuresi += vGun.getResmiTatilSure();

				if (vGun.getIzin() == null && vGun.getVardiya() != null && vGun.getCalismaSuresi() > 0) {
					if (aksamVardiyaBaslangicZamani != null && aksamVardiyaBitisZamani != null) {
						boolean bayramAksamCalismaOde = ortakIslemler.getParameterKey("bayramAksamCalismaOde").equals("1");
						if (vGun.getGirisHareketleri() != null && vGun.getCikisHareketleri() != null && vGun.getGirisHareketleri().size() == vGun.getCikisHareketleri().size()) {
							double sure = 0;
							for (int i = 0; i < vGun.getGirisHareketleri().size(); i++) {
								Date cikisZaman = vGun.getCikisHareketleri().get(i).getZaman();
								Date girisZaman = vGun.getGirisHareketleri().get(i).getZaman();
								if (girisZaman == null || cikisZaman == null)
									continue;
								if (!(girisZaman.getTime() <= aksamVardiyaBitisZamani.getTime() && cikisZaman.getTime() >= aksamVardiyaBaslangicZamani.getTime()))
									continue;

								yemekList = vGun.getYemekList();
								if (girisZaman.before(aksamVardiyaBaslangicZamani))
									girisZaman = aksamVardiyaBaslangicZamani;
								if (cikisZaman.after(aksamVardiyaBitisZamani))
									cikisZaman = aksamVardiyaBitisZamani;
								if (girisZaman.before(cikisZaman)) {
									if (girisZaman.before(aksamVardiyaBaslangicZamani))
										girisZaman = aksamVardiyaBaslangicZamani;
									if (bayramAksamCalismaOde == false && vGun.getTatil() != null) {
										Tatil tatilSakli = vGun.getTatil();
										Tatil tatilAksam = tatilSakli;
										if (tatilSakli.getOrjTatil() != null && tatilSakli.getOrjTatil().isTekSefer())
											tatilAksam = (Tatil) tatilSakli.getOrjTatil().clone();
										Date tatilBas = tatilAksam.getBasTarih();
										Date tatilBit = tatilAksam.getBitTarih();
										if (tatilBit.getTime() >= girisZaman.getTime() && cikisZaman.getTime() > tatilBas.getTime()) {
											if (girisZaman.before(tatilBas))
												cikisZaman = tatilBas;
											else if (cikisZaman.after(tatilBit))
												girisZaman = tatilBit;
											else
												girisZaman = cikisZaman;
										}
									}
								}
								double calSure1 = girisZaman.getTime() < cikisZaman.getTime() ? PdksUtil.setSureDoubleTypeRounded(ortakIslemler.getSaatSure(girisZaman, cikisZaman, yemekList, vGun, session), vGun.getYarimYuvarla()) : 0.0d;

								sure += calSure1;
							}
							if (sure > 0 && vardiya.isCalisma())
								vGun.addAksamVardiyaSaatSayisi(sure);

							if (vGun.getFazlaMesailer() != null) {
								double sureMesai = 0;
								for (PersonelFazlaMesai fazlaMesai : vGun.getFazlaMesailer()) {
									if (fazlaMesai.isOnaylandi()) {
										if (bayramAksamCalismaOde == false && fazlaMesai.isBayram())
											continue;
										if (fazlaMesai.getBitZaman().getTime() > aksamVardiyaBaslangicZamani.getTime() && fazlaMesai.getBasZaman().getTime() < aksamVardiyaBitisZamani.getTime()) {
											Date girisZaman = fazlaMesai.getBasZaman(), cikisZaman = fazlaMesai.getBitZaman();
											if (girisZaman.before(aksamVardiyaBaslangicZamani))
												girisZaman = aksamVardiyaBaslangicZamani;
											if (cikisZaman.after(aksamVardiyaBitisZamani))
												cikisZaman = aksamVardiyaBitisZamani;
											double calSure1 = PdksUtil.setSureDoubleTypeRounded(ortakIslemler.getSaatSure(girisZaman, cikisZaman, yemekList, vGun, session), vGun.getYarimYuvarla());
											if (calSure1 > fazlaMesai.getFazlaMesaiSaati())
												calSure1 = fazlaMesai.getFazlaMesaiSaati();
											sureMesai += calSure1;
										}
									}
								}
								if (sureMesai > 0)
									vGun.addAksamVardiyaSaatSayisi(sureMesai);
							}
							if (vGun.getAksamVardiyaSaatSayisi() > 0.0d) {
								double aksamSure = vGun.getAksamVardiyaSaatSayisi();
								double netSure = vardiya.getNetCalismaSuresi();
								if (vardiya.isCalisma() == false) {
									netSure = 7.5d;
								}
								Double aksamCalismaMaxSaati = new Double(aksamCalismaSaati);
								if (aksamSure > 0.0d && aksamCalismaSaatiYuzde != null && vGun.getIslemVardiya().isCalisma()) {
									aksamCalismaMaxSaati = vGun.getIslemVardiya().getNetCalismaSuresi() * aksamCalismaSaatiYuzde.doubleValue() / 100.0d;
								}
								if (aksamCalismaMaxSaati != null && aksamSure >= aksamCalismaMaxSaati && fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_ADET)) {
									aksamVardiyaSayisi += 1;
									vGun.setAksamKatSayisi(1);
									if (vardiya.isCalisma() == false)
										logger.debug(vardiyaKey);

									logger.debug(aksamVardiyaSayisi + " " + vardiyaKey + " " + vGun.getVardiyaAciklama());
									if (aksamSure > netSure)
										aksamSure -= netSure;
									else
										aksamSure = 0;
								}
								if (fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_SAAT)) {
									vGun.setAksamKatSayisi(vGun.getAksamKatSayisi() - aksamSure);
									aksamVardiyaSaatSayisi += aksamSure;
								}

							}

						}
					}

				}
				try {
					haftaCalismaSuresi += vGun.getHaftaCalismaSuresi();

				} catch (Exception ee) {
					logger.error("Pdks hata in : \n");
					ee.printStackTrace();
					logger.error("Pdks hata out : " + ee.getMessage());
				}

			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());

				logger.info(vardiyaKey + " " + e.getMessage());
			}

		} else {

			if (personelHareketDurum)
				kapiGirisGetir(vGun, vardiyaPlanKey);

			if (personelIzin != null && !izinERPUpdateStr.equals("1"))
				personelIzinGirisiEkle(vGun, personelIzin, null);

			vardiyaPlaniGetir(vGun, vardiyaPlanKey);
			Boolean hataVar = Boolean.TRUE;
			if (vGun.getVardiya() != null && vGun.getVardiya().isIcapVardiyasi()) {
				hataVar = Boolean.FALSE;
				vGun.setHataliDurum(Boolean.FALSE);
			}
			if (hataVar && fazlaMesaiHesapla) {
				vGun.setOnayli(Boolean.FALSE);
				fazlaMesaiHesapla = Boolean.FALSE;
			}

		}
		if (fazlaMesaiOnaylaDurum && vGun.getIslemVardiya() != null && vGun.getHareketDurum().equals(Boolean.TRUE)) {
			try {
				if (key1.equals("20211111"))
					logger.debug(vardiyaKey + " " + vGun.getHareketDurum() + " " + vGun.isAyrikHareketVar());

				islemVardiya = vGun.getIslemVardiya();
				boolean calisma = islemVardiya.isCalisma() && vGun.getIzin() == null;
				Date fazlaMesaiBasZaman = calisma ? islemVardiya.getVardiyaTelorans1BasZaman() : null;
				Date fazlaMesaiBitZaman = calisma ? islemVardiya.getVardiyaTelorans2BitZaman() : null;
				if (girisHareketleri.size() == cikisHareketleri.size()) {
					TreeMap<String, PersonelFazlaMesai> pfmMap = new TreeMap<String, PersonelFazlaMesai>();
					if (vGun.getFazlaMesailer() != null) {
						for (PersonelFazlaMesai personelFazlaMesai : vGun.getFazlaMesailer()) {
							pfmMap.put(personelFazlaMesai.getHareketId(), personelFazlaMesai);
						}

					}
					for (int i = 0; i < girisHareketleri.size(); i++) {

						HareketKGS giris = girisHareketleri.get(i), cikis = cikisHareketleri.get(i);
						Date girisZaman = giris != null ? giris.getOrjinalZaman() : null;
						Date cikisZaman = cikis != null ? cikis.getOrjinalZaman() : null;
						if (calisma == false || (girisZaman != null && girisZaman.before(fazlaMesaiBasZaman)) || (cikisZaman != null && cikisZaman.after(fazlaMesaiBitZaman))) {
							String girisId = giris != null && giris.getId() != null ? giris.getId() : "";
							String cikisId = cikis != null && cikis.getId() != null ? cikis.getId() : "";
							if (!(pfmMap.containsKey(cikisId) || pfmMap.containsKey(girisId))) {
								if (giris != null && giris.getPersonelFazlaMesai() != null) {
									if (denklestirmeAyDurum) {
										PersonelFazlaMesai value = giris.getPersonelFazlaMesai();
										value.setDurum(Boolean.FALSE);
										saveGenelList.add(value);
									}

									giris.setPersonelFazlaMesai(null);
								}
								if (cikis != null && cikis.getPersonelFazlaMesai() != null) {
									if (denklestirmeAyDurum) {
										PersonelFazlaMesai value = cikis.getPersonelFazlaMesai();
										value.setDurum(Boolean.FALSE);
										saveGenelList.add(value);
									}
									cikis.setPersonelFazlaMesai(null);
								}
							}
							if (giris.getPersonelFazlaMesai() == null && cikis.getPersonelFazlaMesai() == null) {
								vGun.setFazlaMesaiOnayla(true);
								vGun.setHareketHatali(true);
								personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
								break;
							}
						}
					}
					pfmMap = null;
				}
			} catch (Exception ex1) {
				logger.error(ex1);
				ex1.printStackTrace();
			}

		}
		girisHareketleri = null;
		cikisHareketleri = null;
		if (vGun.getFazlaMesailer() != null) {
			for (PersonelFazlaMesai personelFazlaMesai : vGun.getFazlaMesailer()) {
				if (personelFazlaMesai.getDurum()) {
					if (personelFazlaMesai.isOnaylandi())
						vGun.setFazlaMesaiOnayla(false);
					personelFazlaMesaiEkle(vGun, vardiyaPlanKey);
					break;
				}
			}
		}
		if (hareketsiz || vGun.isHataliDurum() || fazlaMesaiOnaylaDurum) {
			kapiGirisGetir(vGun, vardiyaPlanKey);
			if (fazlaMesaiOnaylaDurum == false)
				vardiyaPlaniGetir(vGun, vardiyaPlanKey);
		}

		if (vGun.getHareketDurum() != null && vGun.getHareketDurum().booleanValue() == false) {
			if (islemPuantaj.isFiiliHesapla()) {
				islemPuantaj.setFiiliHesapla(false);
				fazlaMesaiHesapla = false;
				kapiGirisGetir(vGun, vardiyaPlanKey);
				vardiyaPlaniGetir(vGun, vardiyaPlanKey);
			} else if (vGun.isAyrikHareketVar()) {
				kapiGirisGetir(vGun, vardiyaPlanKey);
				vardiyaPlaniGetir(vGun, vardiyaPlanKey);
			}

		}
		if (izinDurum && !izinERPUpdateStr.equals("1")) {
			String izinKey = "perId=" + personel.getId() + "&tarih1=" + PdksUtil.convertToDateString(vGun.getVardiyaDate(), "yyyyMMdd") + "&tarih2=" + PdksUtil.convertToDateString(vGun.getVardiyaDate(), "yyyyMMdd");
			personelIzinGirisiEkle(vGun, null, izinKey);

		}
		if (key1.equals("20220202"))
			logger.debug(fazlaMesaiTalepOnayliDurum + " " + vardiyaKey);
		if (fazlaMesaiTalepOnayliDurum && vGun.getFazlaMesaiOnayla() != null) {
			fazlaMesaiTalepVardiyaOnayliDurum = vardiyaFazlaMesaiTalepOnayKontrol(vGun, islemVardiya);
		}
		if (denklestirmeAyDurum && vardiya != null && vardiya.getId() != null && vGun.isAyinGunu() && vardiya.isHaftaTatil() && vGun.isHareketHatali()) {
			logger.debug(vardiyaKey);
		}
		try {
			if (hataliPuantajGoster && vardiya != null && vGun.isAyinGunu() && vardiya.isCalisma() && vGun.getIzin() == null && vGun.getCalismaSuresi() == 0.0 && vGun.getVardiyaDate().before(bugun)) {
				islemPuantaj.setEksikGunVar(true);
				logger.debug(vGun.getVardiyaKeyStr());
			}
		} catch (Exception e) {

		}
		if (vGun.isGecmisHataliDurum()) {
			if (vGun.getOncekiVardiyaGun() != null) {
				Personel izinSahibi = vGun.getPdksPersonel();
				String hataStr = "";
				if (PdksUtil.hasStringValue(sicilNo) == false)
					hataStr += izinSahibi.getPdksSicilNo() + " " + ortakIslemler.personelNoAciklama() + " " + izinSahibi.getAdSoyad() + " ait  ";
				hataStr += PdksUtil.convertToDateString(vGun.getOncekiVardiyaGun().getVardiyaDate(), "d MMM yyyy EEEEE") + " gününde hatalı girişi vardır!";
				PdksUtil.addMessageAvailableError(hataStr);

			}
			// vGun.setHareketHatali(true);
			// vGun.setHataliDurum(false);
			// vGun.setOnayli(false);
			vGun.setDurum(false);
			islemPuantaj.setFiiliHesapla(false);
			fazlaMesaiHesapla = false;
		}

		paramsMap.put("fazlaMesaiHesapla", fazlaMesaiHesapla);
		paramsMap.put("aksamVardiyaSayisi", aksamVardiyaSayisi);
		paramsMap.put("aksamVardiyaSaatSayisi", aksamVardiyaSaatSayisi);
		paramsMap.put("sabahAksamCikisSaatSayisi", sabahAksamCikisSaatSayisi);
		paramsMap.put("haftaCalismaSuresi", haftaCalismaSuresi);
		paramsMap.put("resmiTatilSuresi", resmiTatilSuresi);
		vGun.setFazlaMesaiTalepOnayliDurum(fazlaMesaiTalepVardiyaOnayliDurum);

	}

	/**
	 * @param vGun
	 * @param personelIzin
	 * @param izinKey
	 */
	private void personelIzinGirisiEkle(VardiyaGun vGun, PersonelIzin personelIzin, String izinKey) {
		if (denklestirmeAyDurum && personelIzinGirisiDurum) {
			Personel personel = vGun.getPersonel();
			if (izinKey == null && personelIzin != null)
				izinKey = "perId=" + personel.getId() + "&tarih1=" + PdksUtil.convertToDateString(personelIzin.getBaslangicZamani(), "yyyyMMdd") + "&tarih2=" + PdksUtil.convertToDateString(personelIzin.getBitisZamani(), "yyyyMMdd");
			if (izinKey != null) {
				if (linkBordroAdres != null)
					izinKey += "linkBordroAdres=" + PdksUtil.getEncodeStringByBase64(linkBordroAdres);
				String link = "<a href='http://" + adres + "/personelIzinGirisi?izinKey=" + PdksUtil.getEncodeStringByBase64(izinKey) + "'>" + personelIzinGirisiStr + "</a>";
				vGun.addLinkAdresler(link);
			}
		}
	}

	/**
	 * @param vGun
	 * @param islemVardiya
	 * @return
	 */
	private boolean vardiyaFazlaMesaiTalepOnayKontrol(VardiyaGun vGun, Vardiya islemVardiya) {
		boolean onayliDurum = Boolean.FALSE;
		if (vGun.getFazlaMesaiTalepler() != null && !vGun.getFazlaMesaiTalepler().isEmpty()) {
			if (vGun.getHareketDurum().equals(Boolean.FALSE) && vGun.getHareketler() != null && !vGun.getHareketler().isEmpty()) {
				List<HareketKGS> girisList = vGun.getGirisHareketleri(), cikisList = vGun.getCikisHareketleri();
				boolean girisMesai = false, cikisMesai = false, normalCalisma = islemVardiya.isCalisma() && vGun.getIzin() == null;
				if (girisList != null && cikisList != null && girisList.size() == cikisList.size()) {
					List<Date> girisZamanList = new ArrayList<Date>(), cikisZamanList = new ArrayList<Date>();
					for (int i = 0; i < girisList.size(); i++) {
						Date giris = girisList.get(i).getOrjinalZaman(), cikis = cikisList.get(i).getOrjinalZaman();
						if (normalCalisma) {
							if (cikis.after(islemVardiya.getVardiyaTelorans2BitZaman())) {
								cikisMesai = true;
								if (giris.after(islemVardiya.getVardiyaTelorans2BitZaman()))
									girisZamanList.add(giris);
								else
									girisZamanList.add(islemVardiya.getVardiyaBitZaman());
								cikisZamanList.add(cikis);
							}
							if (giris.before(islemVardiya.getVardiyaTelorans1BasZaman())) {
								girisMesai = true;
								if (cikis.before(islemVardiya.getVardiyaTelorans1BasZaman()))
									cikisZamanList.add(cikis);
								else
									cikisZamanList.add(islemVardiya.getVardiyaBasZaman());
								girisZamanList.add(giris);
							}

						} else {
							girisZamanList.add(giris);
							cikisZamanList.add(cikis);
						}
					}
					int size = vGun.getFazlaMesaiTalepler().size();
					if (normalCalisma) {
						int adet = (girisMesai ? 1 : 0) + (cikisMesai ? 1 : 0);
						if (adet > size)
							size = adet;
					}
					if (size <= cikisZamanList.size()) {
						for (FazlaMesaiTalep fazlaMesaiTalep : vGun.getFazlaMesaiTalepler()) {
							if (fazlaMesaiTalep.isHatirlatmaMail()) {
								logger.debug(vGun.getVardiyaKeyStr());
								continue;
							}
							boolean girisTamam = !normalCalisma;
							Date tarih1 = fazlaMesaiTalep.getBaslangicZamani(), tarih2 = fazlaMesaiTalep.getBitisZamani();
							for (int i = 0; i < girisZamanList.size(); i++) {
								Date giris = girisZamanList.get(i), cikis = cikisZamanList.get(i);
								if (giris.getTime() <= tarih2.getTime() && tarih1.getTime() <= cikis.getTime()) {
									girisTamam = true;
									if (normalCalisma) {
										if (giris.before(islemVardiya.getVardiyaTelorans1BasZaman()))
											girisMesai = false;
										else if (cikis.after(islemVardiya.getVardiyaTelorans2BitZaman()))
											cikisMesai = false;

									}
								}

							}
							if (girisTamam)
								--size;

						}
						if (normalCalisma)
							onayliDurum = girisMesai == false && cikisMesai == false;
						else
							onayliDurum = size <= 0;
					}
				}

			}
		}
		return onayliDurum;
	}

	/**
	 * @param vardiyaGun
	 * @param vardiyaPlanKey
	 */
	private void kapiGirisGetir(VardiyaGun vardiyaGun, String vardiyaPlanKey) {
		if (denklestirmeAyDurum && planOnayDurum && personelHareketDurum) {
			String link = "<a href='http://" + adres + "/personelHareket?planKey=" + vardiyaPlanKey + "'>" + personelHareketStr + "</a>";
			vardiyaGun.addLinkAdresler(link);
		}
	}

	/**
	 * @param vardiyaGun
	 * @param vardiyaPlanKey
	 */
	private void vardiyaPlaniGetir(VardiyaGun vardiyaGun, String vardiyaPlanKey) {
		if (vardiyaPlaniDurum) {
			String link = "<a href='http://" + adres + "/vardiyaPlani?planKey=" + vardiyaPlanKey + "'>" + vardiyaPlaniStr + "</a>";
			vardiyaGun.addLinkAdresler(link);
		}
	}

	/**
	 * @param fazlaMesaiOnayla
	 * @param vardiyaGun
	 * @param vardiyaPlanKey
	 */
	private void personelFazlaMesaiEkle(VardiyaGun vardiyaGun, String vardiyaPlanKey) {
		if (denklestirmeAyDurum) {
			if (planOnayDurum) {
				if (vardiyaGun.getFazlaMesaiOnayla() != null && personelFazlaMesaiDurum) {
					String link = "<a href='http://" + adres + "/personelFazlaMesai?planKey=" + vardiyaPlanKey + "'>" + personelFazlaMesaiStr + "</a>";
					if (vardiyaGun.getFazlaMesaiOnayla())
						vardiyaGun.addLinkAdresler(link);
					logger.debug(vardiyaGun.getVardiyaKeyStr());
				} else
					kapiGirisGetir(vardiyaGun, vardiyaPlanKey);
			}
		}
		vardiyaPlaniGetir(vardiyaGun, vardiyaPlanKey);
	}

	/**
	 * @param perIdList
	 * @return
	 */
	private List<PersonelDenklestirme> getPdksPersonelDenklestirmeler(List<Long> perIdList) {
		List<PersonelDenklestirme> list = fazlaMesaiOrtakIslemler.getPdksPersonelDenklestirmeler(perIdList, denklestirmeAy, session);
		if (denklestirmeAy.getBakiyeSifirlaDurum())
			ortakIslemler.setBakiyeSifirlaDurum(list, session);

		boolean hgDurum = denklestirmeAyDurum && hataliPuantajGoster != null && hataliPuantajGoster && PdksUtil.hasStringValue(sicilNo) == false;
		List<PersonelDenklestirme> listOrj = new ArrayList<PersonelDenklestirme>();
		if (hgDurum)
			listOrj.addAll(list);
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			PersonelDenklestirme personelDenklestirme = (PersonelDenklestirme) iterator.next();
			if (sadeceFazlaMesai && !personelDenklestirme.isDenklestirmeDurum())
				iterator.remove();
			else if (hgDurum && personelDenklestirme.getDurum())
				iterator.remove();
		}
		if (list.isEmpty() && hgDurum && listOrj.isEmpty() == false)
			list.addAll(listOrj);
		listOrj = null;
		return list;
	}

	/**
	 * @return
	 */
	public String fazlaMesaiOnayKontrol() {
		onayla = Boolean.FALSE;
		for (AylikPuantaj puantaj : aylikPuantajList) {
			if (puantaj.isKaydet()) {
				onayla = Boolean.TRUE;
			}
		}
		seciliBolum = null;
		seciliAltBolum = null;
		if (!onayla) {
			if (userLogin.getLogin())
				PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "MMMMM yyyy") + " fazla mesai onayı yapacak personel seçiniz!");
		} else {

			if (seciliEkSaha3Id != null) {

				seciliBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha3Id, Tanim.class, session);

			}
			if (seciliEkSaha4Id != null) {

				seciliAltBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha4Id, Tanim.class, session);

			}
		}

		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String fazlaMesaiOnayla() {
		fazlaMesaiOnaylaDevam(aylikPuantajList, Boolean.TRUE, Boolean.FALSE);
		return "";
	}

	/**
	 * @param puantajList
	 * @param guncellendi
	 * @param manuelOnay
	 * @return
	 */
	private String fazlaMesaiOnaylaDevam(List<AylikPuantaj> puantajList, Boolean guncellendi, Boolean manuelOnay) {
		try {
			boolean onaylandi = Boolean.FALSE;
			TreeMap<Long, List<AylikPuantaj>> puantajMap = new TreeMap<Long, List<AylikPuantaj>>();
			LinkedHashMap<Long, Personel> yoneticiMap = new LinkedHashMap<Long, Personel>();
			boolean mailGonderildi = false;
			List<Long> sirketIdList = new ArrayList<Long>();
			for (AylikPuantaj puantajAylik : puantajList) {
				if (puantajAylik.isKaydet()) {
					PersonelDenklestirme personelDenklestirmeAy = puantajAylik.getPersonelDenklestirme();
					personelDenklestirmeAy.setGuncellendi(guncellendi);
					if (personelDenklestirmeAy != null && personelDenklestirmeAy.getDurum()) {
						Personel calisan = personelDenklestirmeAy.getPersonel();
						if (!personelDenklestirmeAy.isKapandi(userLogin)) {
							personelDenklestirmeAy.setAksamVardiyaSaatSayisi(puantajAylik.getAksamVardiyaSaatSayisi());
							personelDenklestirmeAy.setAksamVardiyaSayisi((double) puantajAylik.getAksamVardiyaSayisi());
							personelDenklestirmeAy.setDevredenSure(puantajAylik.getDevredenSure());
						}
						personelDenklestirmeAy.setFazlaMesaiSure(puantajAylik.getAylikNetFazlaMesai());
						personelDenklestirmeAy.setHaftaCalismaSuresi(puantajAylik.getHaftaCalismaSuresi());
						personelDenklestirmeAy.setResmiTatilSure(puantajAylik.getResmiTatilToplami());
						personelDenklestirmeAy.setOdenenSure(puantajAylik.getFazlaMesaiSure());
						personelDenklestirmeAy.setEksikCalismaSure(puantajAylik.getEksikCalismaSure());
						personelDenklestirmeAy.setKesilenSure(puantajAylik.getKesilenSure());
						if (personelDenklestirmeAy.isGuncellendi() && !userLogin.isAdmin()) {
							personelDenklestirmeAy.setGuncellemeTarihi(new Date());
							personelDenklestirmeAy.setGuncelleyenUser(userLogin);
						}
						if (guncellendi) {
							saveOrUpdate(personelDenklestirmeAy);
							puantajAylik.setKaydet(Boolean.FALSE);
						}
						if (mailGonder) {
							if (calisan != null && !sirketIdList.contains(calisan.getSirket().getId()))
								sirketIdList.add(calisan.getSirket().getId());
							puantajAylik.setSecili(Boolean.TRUE);
							Personel personel = null;
							if (ikRole)
								personel = puantajAylik.getYonetici();
							else
								personel = userLogin.getPdksPersonel();
							if (personel != null && personel.getId() != null) {
								List<AylikPuantaj> aylikPuantajlar = puantajMap.containsKey(personel.getId()) ? (List<AylikPuantaj>) puantajMap.get(personel.getId()) : new ArrayList<AylikPuantaj>();
								if (aylikPuantajlar.isEmpty()) {
									yoneticiMap.put(personel.getId(), personel);
									puantajMap.put(personel.getId(), aylikPuantajlar);
								}
								aylikPuantajlar.add(puantajAylik);
							}
						}
						puantajAylik.setKaydet(Boolean.FALSE);
						onaylandi = Boolean.TRUE;
					}
				}

			}
			tekSirket = sirketIdList.size() == 1;
			if (onaylandi) {
				kaydetDurum = false;
				sessionFlush();
			} else if (guncellendi) {
				if (!manuelOnay)
					PdksUtil.addMessageAvailableWarn("Kayıt seçiniz!");
			}

			if (mailGonder) {
				toList = ortakIslemler.IKKullanicilariBul(new ArrayList<User>(), userLogin.getPdksPersonel(), session);
				if (!toList.isEmpty() || !yoneticiMap.isEmpty()) {
					List<User> adminUserList = ortakIslemler.bccAdminAdres(session, null);
					Tanim bolum = null;
					if (seciliEkSaha3Id != null && seciliEkSaha3Id > 0) {

						bolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha3Id, Tanim.class, session);

					}
					boolean bolumYok = bolum == null;
					if (bccList == null)
						bccList = new ArrayList<User>();
					else
						bccList.clear();
					if (!adminUserList.isEmpty())
						bccList.addAll(adminUserList);
					if (ccList == null)
						ccList = new ArrayList<User>();
					if (userLogin.isAdmin())
						bccList.add(userLogin);
					String aciklama = null, veriAyrac = "_";
					LinkedHashMap<String, Object> veriMap = ortakIslemler.getListPersonelOzetVeriMap(aylikPuantajList, tesisId, " ");
					if (veriMap.containsKey("bolum"))
						bolum = (Tanim) veriMap.get("bolum");
					aciklama = veriMap.containsKey("aciklama") ? (String) veriMap.get("aciklama") : null;
					if (aciklama == null && bolum != null)
						aciklama = bolum.getAciklama();

					String gorevYeriAciklama = getExcelAciklama(veriMap);
					excelDosyaAdi = "fazlaMesai" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";
					mailKonu = PdksUtil.replaceAll(PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "MMMMM yyyy") + " " + (aciklama != null ? " " + PdksUtil.replaceAll(aciklama, veriAyrac, " ") : "") + " fazla mesai onayı", "  ", " ");
					mailIcerik = PdksUtil.replaceAll(PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "MMMMM yyyy") + " " + (aciklama != null ? " " + PdksUtil.replaceAll(aciklama, veriAyrac, " ") : "") + " fazla mesaileri " + userLogin.getAdSoyad() + " tarafından onaylanmıştır.", "  ",
							" ");
					for (Long yoneticiId : yoneticiMap.keySet()) {
						List<AylikPuantaj> list = puantajMap.get(yoneticiId);
						Personel personel = yoneticiMap.get(yoneticiId);
						List<Long> perIdList = new ArrayList<Long>();
						perIdList.add(personel.getId());
						boolean mudurVar = ortakIslemler.getParameterKey("yoneticiPuantajKontrol").equals("1");
						for (AylikPuantaj aylikPuantaj : list) {
							Personel mudur = mudurVar ? aylikPuantaj.getYonetici2() : aylikPuantaj.getPdksPersonel().getYonetici2();
							if (mudur != null && mudur.getId() != null) {
								if (!perIdList.contains(mudur.getId()))
									perIdList.add(mudur.getId());
							}
						}

						if (bolumYok)
							bolum = list.get(0).getPersonelDenklestirme().getPersonel().getEkSaha3();
						ccList.clear();

						try {

							if (!perIdList.isEmpty()) {
								TreeMap<Long, User> perUserMap = pdksEntityController.getSQLParamByFieldMap(User.TABLE_NAME, User.COLUMN_NAME_PERSONEL, perIdList, User.class, "getPersonelId", true, session);
								if (!perUserMap.isEmpty()) {
									List<User> kullanicilar = new ArrayList<User>(perUserMap.values());
									for (Iterator iterator = kullanicilar.iterator(); iterator.hasNext();) {
										User user = (User) iterator.next();
										if (!user.isDurum() || !user.getPdksPersonel().isCalisiyor())
											iterator.remove();

									}
									if (!kullanicilar.isEmpty()) {
										if (kullanicilar.size() > 1)
											kullanicilar = PdksUtil.sortObjectStringAlanList(kullanicilar, "getUsername", null);
										ccList.addAll(kullanicilar);
									}

									kullanicilar = null;
								}

								perUserMap = null;
							}
						} catch (Exception e) {
							logger.error(e);
							e.printStackTrace();
						}
						ByteArrayOutputStream baosDosya = fazlaMesaiExcelDevam(gorevYeriAciklama, list);
						excelData = baosDosya.toByteArray();
						boolean islemDurum = false;
						try {
							islemDurum = servisMesaiOnayMailGonder();
						} catch (Exception e) {
							islemDurum = false;
							logger.error(e);
							e.printStackTrace();
						}
						if (!mailGonderildi)
							mailGonderildi = islemDurum;
						perIdList = null;
						for (AylikPuantaj puantaj : list)
							puantaj.setSecili(Boolean.FALSE);

						list = null;
					}

				}
			}
			if (onaylandi && mailKonu != null) {
				if (!mailGonderildi)
					PdksUtil.addMessageAvailableWarn(mailKonu + " gerçekleşmiştir.");
				else
					PdksUtil.addMessageAvailableWarn(mailKonu + " gerçekleşti, mail bilgilendirmeleri yapılmıştır. ");
			}

			yoneticiMap = null;
			puantajMap = null;
		} catch (Exception e111) {
			logger.error(e111);
			e111.printStackTrace();
		}
		return "";
	}

	/**
	 * @return
	 */
	private boolean servisMesaiOnayMailGonder() {
		boolean islemDurum = false;
		MailObject mailObject = new MailObject();
		mailObject.setSubject(mailKonu);
		mailObject.setBody(mailIcerik);

		if (toList != null) {
			for (User user : toList)
				if (user.getPdksPersonel() == null || user.getPdksPersonel().isCalisiyor())
					mailObject.getToList().add(user.getMailPersonel());
		}
		if (ccList != null) {
			for (User user : ccList)
				if (user.getPdksPersonel() == null || user.getPdksPersonel().isCalisiyor())
					mailObject.getCcList().add(user.getMailPersonel());
		}

		if (bccList != null) {
			for (User user : bccList)
				if (user.getPdksPersonel() == null || user.getPdksPersonel().isCalisiyor())
					if (user.getPdksPersonel() == null || user.getPdksPersonel().isCalisiyor())
						mailObject.getBccList().add(user.getMailPersonel());
		}
		MailFile mailFile = new MailFile();
		mailFile.setIcerik(excelData);
		mailFile.setDisplayName(excelDosyaAdi);
		mailObject.getAttachmentFiles().add(mailFile);
		MailStatu mailStatu = null;
		try {
			if (userLogin.isAdmin()) {
				mailObject.getToList().clear();
				mailObject.getCcList().clear();
			}
			if (!userLogin.isAdmin())
				mailStatu = ortakIslemler.mailSoapServisGonder(true, mailObject, renderer, "/email/fazlaMesaiOnayMail.xhtml", session);
			else
				mailStatu = ortakIslemler.mailSoapServisGonder(true, mailObject, null, null, session);
		} catch (Exception e) {
			mailStatu = new MailStatu();
			if (e.getMessage() != null)
				mailStatu.setHataMesai(e.getMessage());
			else
				mailStatu.setHataMesai("Hata oluştu!");
		}
		islemDurum = mailStatu != null && mailStatu.getDurum();

		return islemDurum;
	}

	/**
	 * @return
	 */
	public String aylikVardiyaHareketExcel() {
		try {
			List<AylikPuantaj> list = new ArrayList<AylikPuantaj>();
			for (Iterator iter = aylikPuantajList.iterator(); iter.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
				if (aylikPuantaj.getHareketler() != null)
					list.add(aylikPuantaj);

			}
			if (!list.isEmpty()) {
				String gorevYeriAciklama = getExcelAciklama(null);
				ByteArrayOutputStream baosDosya = aylikVardiyaHareketExcelDevam(list);
				if (baosDosya != null) {
					String dosyaAdi = "AylikCalismaHareket_" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";
					PdksUtil.setExcelHttpServletResponse(baosDosya, dosyaAdi);
				}
			}
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}

		return "";
	}

	/**
	 * @param puantajList
	 * @return
	 */
	private ByteArrayOutputStream aylikVardiyaHareketExcelDevam(List<AylikPuantaj> puantajList) {

		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		try {
			aylikVardiyaHareketExcelOlustur(puantajList, wb);
			baos = new ByteArrayOutputStream();
			wb.write(baos);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			baos = null;
		}

		return baos;
	}

	/**
	 * @param puantajList
	 * @param wb
	 */
	private void aylikVardiyaHareketExcelOlustur(List<AylikPuantaj> puantajList, Workbook wb) {
		Sheet sheet = ExcelUtil.createSheet(wb, "Tüm Hareketler", Boolean.TRUE);
		CreationHelper helper = wb.getCreationHelper();
		ClientAnchor anchor = helper.createClientAnchor();
		Drawing drawing = sheet.createDrawingPatriarch();
		CellStyle header = ExcelUtil.getStyleHeader(wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddTimeStamp = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleOddDate = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb);
		XSSFCellStyle styleOddDateBold = ExcelUtil.setBoldweight(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);
		XSSFCellStyle styleOddKapiBold = ExcelUtil.setBoldweight(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenTimeStamp = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleEvenDate = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb);
		XSSFCellStyle styleEvenDateBold = ExcelUtil.setBoldweight(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);
		XSSFCellStyle styleEvenKapiBold = ExcelUtil.setBoldweight(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);
		int row = 0;
		int col = 0;
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.sirketAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adı Soyadı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Tarihi");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.vardiyaAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Kapi");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Zaman");
		if (ikRole)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Oluşturma Zamanı");
		boolean renk = true;
		for (Iterator iterator = puantajList.iterator(); iterator.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
			Personel personel = aylikPuantaj.getPdksPersonel();
			String sirket = "";
			try {
				sirket = personel.getSirket().getAd();
			} catch (Exception e1) {
				sirket = "" + ortakIslemler.sirketAciklama() + " tanımsız";
			}

			for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
				if (vg.getVardiya() == null)
					continue;
				CellStyle styleDateBold = null, style = null, styleCenter = null, styleDate = null, styleKapi = null, styleTimeStamp = null;
				if (renk) {
					styleDateBold = styleOddDateBold;
					styleDate = styleOddDate;
					style = styleOdd;
					styleCenter = styleOddCenter;
					styleKapi = styleOddKapiBold;
					styleTimeStamp = styleOddTimeStamp;
				} else {
					styleDateBold = styleEvenDateBold;
					styleDate = styleEvenDate;
					style = styleEven;
					styleCenter = styleEvenCenter;
					styleKapi = styleEvenKapiBold;
					styleTimeStamp = styleEvenTimeStamp;
				}

				String izinAciklama = null, izinKisaAciklama = null;
				if (vg.getIzin() != null) {
					izinAciklama = vg.getIzin().getIzinTipi().getIzinTipiTanim().getAciklama();
					izinKisaAciklama = vg.getIzin().getIzinTipi().getKisaAciklama();
				}

				List<HareketKGS> hareketList = vg.getHareketler();
				boolean sifirla = hareketList == null;
				if (sifirla) {
					hareketList = new ArrayList<HareketKGS>();
					hareketList.add(null);
				}
				Vardiya vardiya = vg.getVardiya();
				CellStyle dateStyle = styleDateBold;
				String vardiyaAdi = vardiya.isCalisma() ? userLogin.timeFormatla(vardiya.getBasZaman()) + ":" + userLogin.timeFormatla(vardiya.getBitZaman()) : vardiya.getVardiyaAdi();
				renk = !renk;
				for (Iterator iter = hareketList.iterator(); iter.hasNext();) {
					Object object = (Object) iter.next();
					HareketKGS hareket = object != null ? (HareketKGS) object : null;
					row++;
					col = 0;

					try {
						ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket);
						ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAdSoyad());
						ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSicilNo());
						ExcelUtil.getCell(sheet, row, col++, dateStyle).setCellValue(vg.getVardiyaDate());
						if (izinKisaAciklama == null)
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(vardiyaAdi);
						else {
							Cell izinCell = ExcelUtil.getCell(sheet, row, col++, style);
							ExcelUtil.baslikCell(izinCell, anchor, helper, drawing, izinKisaAciklama + "\n" + vardiyaAdi, izinAciklama);
						}
						CellStyle kapiStyle = vg.getHareketDurum() ? style : styleKapi;

						if (hareket != null) {
							ExcelUtil.getCell(sheet, row, col++, kapiStyle).setCellValue(hareket.getKapiView().getAciklama());
							ExcelUtil.getCell(sheet, row, col++, styleTimeStamp).setCellValue(hareket.getZaman());
							if (ikRole) {
								if (hareket.getOlusturmaZamani() != null) {
									Cell createCell = setCellDate(sheet, row, col++, styleTimeStamp, hareket.getOlusturmaZamani());
									if (hareket.getIslem() != null) {
										PersonelHareketIslem islem = hareket.getIslem();
										String title = "Ekleyen : " + islem.getOnaylayanUser().getAdSoyad();
										if (islem.getNeden() != null)
											title += "\nNeden : " + islem.getNeden().getAciklama();
										ExcelUtil.setCellComment(createCell, anchor, helper, drawing, title);
									}
								} else
									ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");

							}
						} else {
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						}

					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());
						logger.debug(row);

					}
					if (iter.hasNext()) {
						if (renk) {
							styleDateBold = styleOddDateBold;
							styleDate = styleOddDate;
							style = styleOdd;
							styleCenter = styleOddCenter;
							styleKapi = styleOddKapiBold;
							styleTimeStamp = styleOddTimeStamp;
						} else {
							styleDateBold = styleEvenDateBold;
							styleDate = styleEvenDate;
							style = styleEven;
							styleCenter = styleEvenCenter;
							styleKapi = styleEvenKapiBold;
							styleTimeStamp = styleEvenTimeStamp;
						}
						dateStyle = styleDate;
						renk = !renk;

					}

				}

				if (sifirla)
					hareketList = null;

			}

		}
		for (int i = 0; i < col; i++)
			sheet.autoSizeColumn(i);
	}

	/**
	 * @param veriMap
	 * @return
	 */
	private String getExcelAciklama(LinkedHashMap<String, Object> veriMap) {
		if (veriMap == null)
			veriMap = ortakIslemler.getListPersonelOzetVeriMap(aylikPuantajList, tesisId, " ");
		String gorevYeriAciklama = "";
		if (veriMap.containsKey("sirketGrup")) {
			Tanim sirketGrup = (Tanim) veriMap.get("sirketGrup");
			gorevYeriAciklama = sirketGrup.getAciklama() + "_";
		} else if (veriMap.containsKey("sirket")) {
			Sirket sirket = (Sirket) veriMap.get("sirket");
			gorevYeriAciklama = sirket.getAd() + "_";
		}

		if (seciliEkSaha3Id != null || tesisId != null || seciliEkSaha4Id != null) {
			Tanim ekSaha3 = null, ekSaha4 = null, tesis = null;
			if (tesisId != null) {
				if (veriMap.containsKey("tesis"))
					tesis = (Tanim) veriMap.get("tesis");
			}
			if (seciliEkSaha3Id != null) {
				if (veriMap.containsKey("bolum"))
					ekSaha3 = (Tanim) veriMap.get("bolum");
				if (veriMap.containsKey("altBolum"))
					ekSaha4 = (Tanim) veriMap.get("altBolum");
			}

			if (tesis != null)
				gorevYeriAciklama += tesis.getAciklama() + "_";
			if (ekSaha3 != null)
				gorevYeriAciklama += ekSaha3.getAciklama() + "_";
			if (ekSaha4 != null)
				gorevYeriAciklama += ekSaha4.getAciklama() + "_";
		}
		if (seciliEkSaha4Id != null && seciliEkSaha4Id.longValue() > 0L) {

			Tanim ekSaha4 = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, seciliEkSaha4Id, Tanim.class, session);
			if (ekSaha4 != null)
				gorevYeriAciklama += ekSaha4.getAciklama() + "_";
		}
		return gorevYeriAciklama;
	}

	/**
	 * @return
	 */
	public String fazlaMesaiExcel() {
		try {
			for (Iterator iter = aylikPuantajList.iterator(); iter.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
				aylikPuantaj.setSecili(Boolean.TRUE);
			}
			String gorevYeriAciklama = getExcelAciklama(null);
			ByteArrayOutputStream baosDosya = fazlaMesaiExcelDevam(gorevYeriAciklama, aylikPuantajList);
			if (baosDosya != null) {
				String dosyaAdi = "FazlaMesai_" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";
				PdksUtil.setExcelHttpServletResponse(baosDosya, dosyaAdi);
			}
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}

		return "";
	}

	/**
	 * @param gorevYeriAciklama
	 * @param list
	 * @return
	 */
	private ByteArrayOutputStream fazlaMesaiExcelDevam(String gorevYeriAciklama, List<AylikPuantaj> list) {
		if (!ortakIslemler.getParameterKey("sadeceFazlaMesaiGetir").equals("1"))
			sadeceFazlaMesai = true;
		boolean kimlikNoGoster = false;
		TreeMap<String, String> sirketMap = new TreeMap<String, String>();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
			Personel personel = aylikPuantaj.getPdksPersonel();
			if (!kimlikNoGoster && ikRole) {
				PersonelKGS personelKGS = personel.getPersonelKGS();
				if (personelKGS != null)
					kimlikNoGoster = PdksUtil.hasStringValue(personelKGS.getKimlikNo());

			}
			String tekSirketTesis = (personel.getSirket() != null ? personel.getSirket().getId() : "") + "_" + (personel.getTesis() != null ? personel.getTesis().getId() : "");
			String tekSirketTesisAdi = (personel.getSirket() != null ? personel.getSirket().getAd() : "") + " " + (personel.getTesis() != null ? personel.getTesis().getAciklama() : "");
			sirketMap.put(tekSirketTesis, tekSirketTesisAdi);
		}

		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "MMMMM yyyy") + " Fazla Mesai", Boolean.TRUE);
		CellStyle izinBaslik = ExcelUtil.getStyleHeader(wb);
		CellStyle styleTutarEven = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_TUTAR, wb);
		CellStyle styleTutarOdd = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_TUTAR, wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);

		CellStyle styleCenterEvenDay = ExcelUtil.getStyleDayEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleCenterOddDay = ExcelUtil.getStyleDayOdd(ExcelUtil.ALIGN_CENTER, wb);

		CellStyle styleDay = null, styleGenel = null, styleTutar = null, styleStrDay = null;
		CellStyle styleCenter = ExcelUtil.getStyleData(wb);
		CellStyle styleTatil = ExcelUtil.getStyleDataCenter(wb);

		CellStyle styleIstek = ExcelUtil.getStyleDataCenter(wb);
		CellStyle styleEgitim = ExcelUtil.getStyleDataCenter(wb);
		CellStyle styleOff = ExcelUtil.getStyleDataCenter(wb);
		ExcelUtil.setFontColor(styleOff, Color.WHITE);
		ExcelUtil.setFillForegroundColor(izinBaslik, 146, 208, 80);

		CellStyle styleIzin = ExcelUtil.getStyleDataCenter(wb);
		ExcelUtil.setFillForegroundColor(styleIzin, 146, 208, 80);

		CellStyle styleCalisma = ExcelUtil.getStyleDataCenter(wb);
		int row = 0, col = 0;
		XSSFCellStyle header = (XSSFCellStyle) ExcelUtil.getStyleHeader(9, wb);

		ExcelUtil.setFillForegroundColor(styleTatil, 255, 153, 204);

		ExcelUtil.setFillForegroundColor(styleIstek, 255, 255, 0);

		ExcelUtil.setFillForegroundColor(styleCalisma, 255, 255, 255);

		ExcelUtil.setFillForegroundColor(styleEgitim, 0, 0, 255);

		ExcelUtil.setFillForegroundColor(styleOff, 13, 12, 89);
		ExcelUtil.setFontColor(styleOff, 256, 256, 256);
		String aciklamaExcel = PdksUtil.replaceAll(gorevYeriAciklama + " " + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM  "), "_", " ");
		ExcelUtil.getCell(sheet, row, col, header).setCellValue(aciklamaExcel);
		for (int i = 0; i < 3; i++)
			ExcelUtil.getCell(sheet, row, col + i + 1, header).setCellValue("");

		try {
			sheet.addMergedRegion(ExcelUtil.getRegion((int) row, (int) 0, (int) row, (int) 4));
		} catch (Exception e) {
			e.printStackTrace();
		}
		col = 0;
		ExcelUtil.getCell(sheet, ++row, col, styleGenel).setCellValue("");
		ExcelUtil.getCell(sheet, ++row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adı Soyadı");
		if (kimlikNoGoster)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.kimlikNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.yoneticiAciklama());
		if (seciliEkSaha3Id != null)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(bolumAciklama);
		if (ekSaha4Tanim != null && seciliEkSaha4Id != null && seciliEkSaha4Id.longValue() > 0L)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ekSaha4Tanim.getAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.calismaModeliAciklama());

		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("FM Ödeme");
		if (fazlaMesaiIzinKullan)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.fmIzinKullanAciklama());

		Calendar cal = Calendar.getInstance();
		cal.setTime(aylikPuantajDefault.getIlkGun());
		CreationHelper helper = wb.getCreationHelper();
		ClientAnchor anchor = helper.createClientAnchor();
		Drawing drawing = sheet.createDrawingPatriarch();
		CellStyle headerVardiyaGun = ExcelUtil.getStyleHeader(9, wb);
		ExcelUtil.setFillForegroundColor(headerVardiyaGun, 99, 182, 153);

		CellStyle headerVardiyaTatilYarimGun = ExcelUtil.getStyleHeader(9, wb);
		ExcelUtil.setFontColor(headerVardiyaTatilYarimGun, 255, 255, 0);
		ExcelUtil.setFillForegroundColor(headerVardiyaTatilYarimGun, 144, 185, 63);

		CellStyle headerVardiyaTatilGun = ExcelUtil.getStyleHeader(9, wb);
		ExcelUtil.setFillForegroundColor(headerVardiyaTatilGun, 92, 127, 45);
		ExcelUtil.setFontColor(headerVardiyaTatilGun, 255, 255, 0);
		for (VardiyaGun vardiyaGun : aylikPuantajDefault.getVardiyalar()) {
			try {
				if (!vardiyaGun.isAyinGunu())
					continue;
				cal.setTime(vardiyaGun.getVardiyaDate());
				CellStyle headerVardiya = headerVardiyaGun;
				String title = null;
				if (vardiyaGun.getTatil() != null) {
					Tatil tatil = vardiyaGun.getTatil();
					title = tatil.getAd();
					headerVardiya = tatil.isYarimGunMu() ? headerVardiyaTatilYarimGun : headerVardiyaTatilGun;
				}
				Cell cell = ExcelUtil.getCell(sheet, row, col++, headerVardiya);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, cal.get(Calendar.DAY_OF_MONTH) + "\n " + userLogin.getTarihFormatla(cal.getTime(), "EEE"), title);

			} catch (Exception e) {
			}
		}

		Cell cell = ExcelUtil.getCell(sheet, row, col++, header);
		ExcelUtil.baslikCell(cell, anchor, helper, drawing, "TÇS", "Toplam Çalışma Saati: Çalışanın bu listedeki toplam çalışma saati");
		cell = ExcelUtil.getCell(sheet, row, col++, header);
		ExcelUtil.baslikCell(cell, anchor, helper, drawing, "ÇGS", "Çalışılması Gereken Saat: Çalışanın bu listede çalışması gereken saat");
		if (yasalFazlaCalismaAsanSaat) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.yasalFazlaCalismaAsanSaatKod(), "Yasal Çalışmayı Aşan Mesai : " + authenticatedUser.sayiFormatliGoster(denklestirmeAy.getFazlaMesaiMaxSure()) + " saati aşan çalışma toplam miktarı");
		}
		if (gerceklesenMesaiKod) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "GM", "Gerçekleşen Mesai : Çalışanın bu listedeki eksi/fazla çalışma saati");
		}

		if (devredenMesaiKod) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.devredenMesaiKod(), "Devreden Mesai: Çalisanin önceki listelerden devreden eksi/fazla mesaisi");

		}
		if (ucretiOdenenKod) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "ÜÖM", "Çalışanın bu listenin sonunda ücret olarak ödediğimiz fazla mesai saati");
		}
		if (kismiOdemeGoster) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "KÖM", "Çalışanın bu listenin sonunda ücret olarak kısmi ödediğimiz fazla mesai saati ");
		}

		if (resmiTatilVar || bordroPuantajEkranindaGoster) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "RÖM", "Çalışanın bu listenin sonunda ücret olarak ödediğimiz resmi tatil mesai saati");
		}
		if (haftaTatilVar) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_HAFTA_TATIL, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz hafta tatil mesai saati");
		}
		if (devredenBakiyeKod) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.devredenBakiyeKod(), "Bakiye: Çalışanın bu liste de dahil bugüne kadarki devreden eksi/fazla mesaisi");
		}
		if (kesilenSureGoster) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_KESINTI_SURE, "Kesilen Süre: Çalışanın bu liste de dahil bugüne kadarki denkleşmeyen eksi bakiyesi");
		}
		if (aksamGun) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_AKSAM_ADET, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz gece mesai gün");
		}
		if (aksamSaat) {
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_AKSAM_SAAT, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz gece mesai saati");
		}
		if (denklestirmeDinamikAlanlar != null && !denklestirmeDinamikAlanlar.isEmpty()) {
			for (Tanim alan : denklestirmeDinamikAlanlar) {
				ExcelUtil.getCell(sheet, row, col++, header).setCellValue(alan.getAciklama());

			}
		}
		CellStyle headerIzinTipi = (CellStyle) header.clone();
		ExcelUtil.setFillForegroundColor(headerIzinTipi, 255, 153, 204);
		if (bordroPuantajEkranindaGoster) {
			XSSFCellStyle headerSiyah = (XSSFCellStyle) ExcelUtil.getStyleHeader(wb);
			headerSiyah.getFont().setColor(ExcelUtil.getXSSFColor(255, 255, 255));
			XSSFCellStyle headerSaat = (XSSFCellStyle) headerSiyah.clone();
			XSSFCellStyle headerIzin = (XSSFCellStyle) headerSiyah.clone();
			XSSFCellStyle headerBGun = (XSSFCellStyle) headerSiyah.clone();
			XSSFCellStyle headerBTGun = (XSSFCellStyle) (XSSFCellStyle) headerSiyah.clone();
			ExcelUtil.setFillForegroundColor(headerSaat, 146, 208, 62);
			ExcelUtil.setFillForegroundColor(headerIzin, 255, 255, 255);
			ExcelUtil.setFillForegroundColor(headerBGun, 255, 255, 0);
			ExcelUtil.setFillForegroundColor(headerBTGun, 236, 125, 125);

			if (normalCalismaSaatKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerSaat);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.normalCalismaSaatKod(), "N.Çalışma Saat");
			}
			if (haftaTatilCalismaSaatKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerSaat);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.haftaTatilCalismaSaatKod(), "H.Tatil Saat");
			}
			if (resmiTatilCalismaSaatKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerSaat);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.resmiTatilCalismaSaatKod(), "R.Tatil Saat");
			}
			if (izinSureSaatKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerSaat);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.izinSureSaatKod(), "İzin Saat");
			}
			if (normalCalismaGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.normalCalismaGunKod(), "N.Çalışma Gün");
			}
			if (haftaTatilCalismaGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.haftaTatilCalismaGunKod(), "H.Tatil Gün");
			}
			if (resmiTatilCalismaGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.resmiTatilCalismaGunKod(), "R.Tatil Gün");
			}
			if (izinSureGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.izinSureGunKod(), "İzin Gün");
			}
			if (ucretliIzinGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerIzin);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.ucretliIzinGunKod(), "Ücretli İzin Gün");
			}
			if (ucretsizIzinGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerIzin);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.ucretsizIzinGunKod(), "Ücretsiz İzin Gün");
			}
			if (hastalikIzinGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerIzin);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.hastalikIzinGunKod(), "Hastalık İzin Gün");
			}
			if (normalGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerBGun);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.normalGunKod(), "Normal Gün");
			}
			if (haftaTatilGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerBGun);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.haftaTatilGunKod(), "H.Tatil Gün");
			}
			if (resmiTatilGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerBGun);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.resmiTatilGunKod(), "R.Tatil Gün");
			}
			if (artikGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerBGun);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.artikGunKod(), "Artık Gün");
			}
			if (bordroToplamGunKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerBTGun);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.bordroToplamGunKod(), "Toplam Gün");
			}
		}
		if (izinTipiVardiyaList != null) {
			for (Vardiya vardiya : izinTipiVardiyaList) {
				cell = ExcelUtil.getCell(sheet, row, col++, headerIzinTipi);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, vardiya.getKisaAdi(), vardiya.getAdi());

			}
		}
		boolean istifaGosterDurum = istifaGoster && (ikRole || mailGonder);
		if (istifaGosterDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Durum");
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();

			Personel personel = aylikPuantaj.getPdksPersonel();
			if (personel == null || PdksUtil.hasStringValue(personel.getSicilNo()) == false)
				continue;
			PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
			boolean denklestirmeVar = personelDenklestirme.isDenklestirmeDurum();
			if (denklestirmeVar) {
				if (!aylikPuantaj.isFazlaMesaiHesapla() || !aylikPuantaj.isSecili())
					continue;
			}
			CalismaModeli calismaModeli = aylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy() != null ? aylikPuantaj.getPersonelDenklestirme().getCalismaModeli() : null;
			if (calismaModeli == null)
				calismaModeli = personel.getCalismaModeli();
			PersonelKGS personelKGS = personel.getPersonelKGS();
			PersonelDenklestirme personelDenklestirmeGecenAy = personelDenklestirme != null ? personelDenklestirme.getPersonelDenklestirmeGecenAy() : null;
			row++;
			col = 0;

			try {
				boolean help = helpPersonel(aylikPuantaj.getPdksPersonel());
				try {
					if (row % 2 != 0) {
						styleCenter = styleOddCenter;
						styleStrDay = styleCenterOddDay;
						styleGenel = styleOdd;
						styleTutar = styleTutarOdd;
					} else {
						styleCenter = styleEvenCenter;
						styleStrDay = styleCenterEvenDay;
						styleGenel = styleEven;
						styleTutar = styleTutarEven;
					}
					ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSicilNo());
					Cell personelCell = ExcelUtil.getCell(sheet, row, col++, styleGenel);
					personelCell.setCellValue(personel.getAdSoyad());
					if (!sirketMap.isEmpty()) {
						Sirket personelSirket = personel.getSirket();
						String title = personelSirket.getAd() + (personel.getTesis() != null ? " - " + personel.getTesis().getAciklama() : "");
						ExcelUtil.setCellComment(personelCell, anchor, helper, drawing, title);
					}
					if (kimlikNoGoster) {
						String kimlikNo = "";
						if (personelKGS != null && PdksUtil.hasStringValue(personelKGS.getKimlikNo()))
							kimlikNo = personelKGS.getKimlikNo();
						ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(kimlikNo);
					}
					ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(aylikPuantaj.getYonetici() != null && aylikPuantaj.getYonetici().getId() != null ? aylikPuantaj.getYonetici().getAdSoyad() : "");
					if (seciliEkSaha3Id != null)
						ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "");
					if (ekSaha4Tanim != null && seciliEkSaha4Id != null && seciliEkSaha4Id.longValue() > 0L)
						ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(personel.getEkSaha4() != null ? personel.getEkSaha4().getAciklama() : "");

					String modelAciklama = "";
					if (calismaModeli != null)
						modelAciklama = calismaModeli.getAciklama();
					ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(modelAciklama);

					ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(userLogin.getYesNo(personelDenklestirme.getFazlaMesaiOde()));
					if (fazlaMesaiIzinKullan)
						ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(userLogin.getYesNo(personelDenklestirme.getFazlaMesaiIzinKullan()));

					List vardiyaList = aylikPuantaj.getAyinVardiyalari();

					for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
						VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
						String styleText = vardiyaGun.getAylikClassAdi(aylikPuantaj.getTrClass());
						styleDay = styleStrDay;
						if (styleText.equals(VardiyaGun.STYLE_CLASS_HAFTA_TATIL))
							styleDay = styleTatil;
						else if (styleText.equals(VardiyaGun.STYLE_CLASS_IZIN))
							styleDay = styleIzin;
						else if (styleText.equals(VardiyaGun.STYLE_CLASS_OZEL_ISTEK))
							styleDay = styleIstek;
						else if (styleText.equals(VardiyaGun.STYLE_CLASS_EGITIM))
							styleDay = styleEgitim;
						else if (styleText.equals(VardiyaGun.STYLE_CLASS_OFF))
							styleDay = styleOff;
						cell = ExcelUtil.getCell(sheet, row, col++, styleDay);
						String aciklama = !help || calisan(vardiyaGun) ? vardiyaGun.getFazlaMesaiOzelAciklama(Boolean.TRUE, userLogin.sayiFormatliGoster(vardiyaGun.getCalismaSuresi())) : "";
						String title = !help || calisan(vardiyaGun) ? vardiyaGun.getTitle() : null;
						if (title != null) {
							if (vardiyaGun.getVardiya() != null && (vardiyaGun.getCalismaSuresi() > 0 || (vardiyaGun.getVardiya().isCalisma() && styleGenel == styleCalisma)))
								title = vardiyaGun.getVardiya().getKisaAdi() + " --> " + title;
							ExcelUtil.setCellComment(cell, anchor, helper, drawing, title);
						}
						cell.setCellValue(aciklama);

					}

					setCell(sheet, row, col++, styleTutar, aylikPuantaj.getSaatToplami());
					Cell planlananCell = setCell(sheet, row, col++, styleTutar, aylikPuantaj.getPlanlananSure());
					if (aylikPuantaj.getCalismaModeliAy() != null && planlananCell != null && aylikPuantaj.getSutIzniDurum().equals(Boolean.FALSE)) {
						String title = aylikPuantaj.getCalismaModeli().getAciklama() + " : ";
						if (aylikPuantaj.getCalismaModeli().getToplamGunGuncelle().equals(Boolean.FALSE))
							title += authenticatedUser.sayiFormatliGoster(aylikPuantaj.getCalismaModeliAy().getSure());
						else
							title += authenticatedUser.sayiFormatliGoster(aylikPuantaj.getPersonelDenklestirme().getPlanlanSure());
						if (PdksUtil.hasStringValue(title))
							ExcelUtil.setCellComment(planlananCell, anchor, helper, drawing, title);
					}
					if (yasalFazlaCalismaAsanSaat) {
						if (aylikPuantaj.getUcretiOdenenMesaiSure() > 0)
							setCell(sheet, row, col++, styleTutar, aylikPuantaj.getUcretiOdenenMesaiSure());
						else
							ExcelUtil.getCell(sheet, row, col++, styleTutar).setCellValue("");
					}
					if (gerceklesenMesaiKod)
						setCell(sheet, row, col++, styleTutar, aylikPuantaj.getAylikNetFazlaMesai());
					if (devredenMesaiKod) {
						Double gecenAyFazlaMesai = aylikPuantaj.getGecenAyFazlaMesai(authenticatedUser);
						Cell gecenAyFazlaMesaiCell = setCell(sheet, row, col++, styleTutar, gecenAyFazlaMesai);
						if (gecenAyFazlaMesai != null && personelDenklestirmeGecenAy != null && gecenAyFazlaMesai.doubleValue() != 0.0d) {
							if (personelDenklestirmeGecenAy.getGuncelleyenUser() != null && personelDenklestirmeGecenAy.getGuncellemeTarihi() != null) {
								String title = "Onaylayan : " + personelDenklestirmeGecenAy.getGuncelleyenUser().getAdSoyad() + "\n";
								title += "Zaman : " + authenticatedUser.dateTimeFormatla(personelDenklestirmeGecenAy.getGuncellemeTarihi());
								ExcelUtil.setCellComment(gecenAyFazlaMesaiCell, anchor, helper, drawing, title);
							}
						}
					}
					boolean olustur = false;
					Comment commentGuncelleyen = null;
					if (ucretiOdenenKod) {
						if (denklestirmeVar && aylikPuantaj.isFazlaMesaiHesapla()) {
							Cell fazlaMesaiSureCell = setCell(sheet, row, col++, styleTutar, aylikPuantaj.getFazlaMesaiSure());
							if (aylikPuantaj.getFazlaMesaiSure() != 0.0d) {
								if (personelDenklestirme.getGuncelleyenUser() != null && personelDenklestirme.getGuncellemeTarihi() != null)
									commentGuncelleyen = fazlaMesaiOrtakIslemler.getCommentGuncelleyen(anchor, helper, drawing, personelDenklestirme);
								fazlaMesaiSureCell.setCellComment(commentGuncelleyen);
								olustur = true;
							}
						} else
							ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue("");
					}
					if (kismiOdemeGoster) {
						if (denklestirmeVar && personelDenklestirme.getKismiOdemeSure() != null && personelDenklestirme.getKismiOdemeSure().doubleValue() > 0.0d)
							setCell(sheet, row, col++, styleTutar, personelDenklestirme.getKismiOdemeSure());
						else
							ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue("");
					}
					if (resmiTatilVar || bordroPuantajEkranindaGoster)
						setCell(sheet, row, col++, styleTutar, denklestirmeVar == false ? 0L : aylikPuantaj.getResmiTatilToplami());
					if (haftaTatilVar)
						setCell(sheet, row, col++, styleTutar, denklestirmeVar == false ? 0L : aylikPuantaj.getHaftaCalismaSuresi());
					if (devredenBakiyeKod) {
						if (denklestirmeVar && aylikPuantaj.isFazlaMesaiHesapla()) {
							Cell devredenSureCell = setCell(sheet, row, col++, styleTutar, aylikPuantaj.getDevredenSure());
							if (aylikPuantaj.getDevredenSure() != null && aylikPuantaj.getDevredenSure().doubleValue() != 0.0d && commentGuncelleyen == null) {
								if (olustur)
									commentGuncelleyen = fazlaMesaiOrtakIslemler.getCommentGuncelleyen(anchor, helper, drawing, personelDenklestirme);
								if (commentGuncelleyen != null)
									devredenSureCell.setCellComment(commentGuncelleyen);
							}
						} else
							ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue("");
					}
					if (kesilenSureGoster) {
						if (aylikPuantaj.getKesilenSure() > 0.0d) {

							Cell kesilenSureCell = null;
							String title = denklestirmeAy.getKesintiAciklama();
							if (denklestirmeAy.isKesintiYok()) {
								kesilenSureCell = setCellStr(sheet, row, col++, styleGenel, "X");
								title = "Denkleşmeyen negatif bakiye var! [ " + userLogin.sayiFormatliGoster(aylikPuantaj.getDevredenSure()) + " ]";
							} else {
								kesilenSureCell = setCell(sheet, row, col++, styleTutar, aylikPuantaj.getKesilenSure());
							}
							ExcelUtil.setCellComment(kesilenSureCell, anchor, helper, drawing, title);
						} else
							ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue("");
					}
					if (aksamGun)
						setCell(sheet, row, col++, styleTutar, denklestirmeVar == false ? 0L : new Double(aylikPuantaj.getAksamVardiyaSayisi()));
					if (aksamSaat)
						setCell(sheet, row, col++, styleTutar, denklestirmeVar == false ? 0L : new Double(aylikPuantaj.getAksamVardiyaSaatSayisi()));

					if (denklestirmeDinamikAlanlar != null && !denklestirmeDinamikAlanlar.isEmpty()) {
						for (Tanim alan : denklestirmeDinamikAlanlar) {
							PersonelDenklestirmeDinamikAlan denklestirmeDinamikAlan = aylikPuantaj.getDinamikAlan(alan.getId());
							String alanStr = denklestirmeDinamikAlan == null ? "" : denklestirmeDinamikAlan.getPersonelDenklestirmeDinamikAlanStr(authenticatedUser);
							ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(alanStr);
						}
					}
					if (bordroPuantajEkranindaGoster) {
						PersonelDenklestirmeBordro denklestirmeBordro = aylikPuantaj.getDenklestirmeBordro();
						if (denklestirmeBordro == null) {
							denklestirmeBordro = new PersonelDenklestirmeBordro();
							denklestirmeBordro.setPersonelDenklestirme(aylikPuantaj.getPersonelDenklestirme());
						}

						boolean saatlikCalisma = calismaModeli.isSaatlikOdeme();
						if (denklestirmeBordro.getDetayMap() == null)
							denklestirmeBordro.setDetayMap(new HashMap<BordroDetayTipi, PersonelDenklestirmeBordroDetay>());
						if (normalCalismaSaatKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma ? denklestirmeBordro.getSaatNormal() : 0);
						if (haftaTatilCalismaSaatKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma ? denklestirmeBordro.getSaatHaftaTatil() : 0);
						if (resmiTatilCalismaSaatKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma ? denklestirmeBordro.getSaatResmiTatil() : 0);
						if (izinSureSaatKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma ? denklestirmeBordro.getSaatIzin() : 0);
						if (normalCalismaGunKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma == false ? denklestirmeBordro.getSaatNormal() : 0);
						if (haftaTatilCalismaGunKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma == false ? denklestirmeBordro.getSaatHaftaTatil() : 0);
						if (resmiTatilCalismaGunKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma == false ? denklestirmeBordro.getSaatResmiTatil() : 0);
						if (izinSureGunKod)
							setCell(sheet, row, col++, styleGenel, saatlikCalisma == false ? denklestirmeBordro.getSaatIzin() : 0);
						if (ucretliIzinGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getUcretliIzin().doubleValue());
						if (ucretsizIzinGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getUcretsizIzin().doubleValue());
						if (hastalikIzinGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getRaporluIzin().doubleValue());
						if (normalGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getNormalGunAdet());
						if (haftaTatilGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getHaftaTatilAdet());
						if (resmiTatilGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getResmiTatilAdet());
						if (artikGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getArtikAdet());
						if (bordroToplamGunKod)
							setCell(sheet, row, col++, styleGenel, denklestirmeBordro.getBordroToplamGunAdet());

					}
					if (izinTipiVardiyaList != null) {
						if (row % 2 != 0)
							styleGenel = styleTutarOdd;
						else {
							styleGenel = styleTutarEven;
						}
						for (Vardiya vardiya : izinTipiVardiyaList) {
							Integer adet = getVardiyaAdet(personel, vardiya);
							setCell(sheet, row, col++, styleGenel, new Double(adet != null ? adet : 0.0d));

						}
					}
					if (istifaGosterDurum)
						ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue(aylikPuantaj.isCalisiyor() ? "Çalışıyor" : "Ayrılmış");
					styleGenel = null;
				} catch (Exception e) {
					logger.error("Pdks hata in : \n");
					e.printStackTrace();
					logger.error("Pdks hata out : " + e.getMessage());
					logger.error(row);

				}
			} catch (Exception ex) {
				logger.error(ex);
				ex.printStackTrace();
			}

		}

		try {

			for (int i = 0; i <= col; i++)
				sheet.autoSizeColumn(i);
			if (ortakIslemler.getParameterKey("aylikVardiyaHareketExcelOlustur").equals("1"))
				aylikVardiyaHareketExcelOlustur(list, wb);
			baos = new ByteArrayOutputStream();
			wb.write(baos);
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			baos = null;
		}
		return baos;
	}

	/**
	 * @param sheet
	 * @param rowNo
	 * @param columnNo
	 * @param style
	 * @param deger
	 * @return
	 */
	public Cell setCell(Sheet sheet, int rowNo, int columnNo, CellStyle style, Double deger) {
		Cell cell = ExcelUtil.getCell(sheet, rowNo, columnNo, style);

		try {
			if (deger != 0.0d) {
				cell.setCellValue(userLogin.sayiFormatliGoster(deger));
			}

		} catch (Exception e) {
		}
		return cell;
	}

	/**
	 * @param sheet
	 * @param rowNo
	 * @param columnNo
	 * @param style
	 * @param str
	 * @return
	 */
	public Cell setCellStr(Sheet sheet, int rowNo, int columnNo, CellStyle style, String str) {
		Cell cell = ExcelUtil.getCell(sheet, rowNo, columnNo, style);
		cell.setCellValue(str != null ? str : "");
		return cell;
	}

	/**
	 * @param sheet
	 * @param rowNo
	 * @param columnNo
	 * @param style
	 * @param date
	 * @return
	 */
	public Cell setCellDate(Sheet sheet, int rowNo, int columnNo, CellStyle style, Date date) {
		Cell cell = ExcelUtil.getCell(sheet, rowNo, columnNo, style);

		try {
			if (date != null) {
				cell.setCellValue(date);
			} else
				cell.setCellValue("");

		} catch (Exception e) {
		}
		return cell;
	}

	/**
	 * @param vardiyaGun
	 * @return
	 */
	private boolean calisan(VardiyaGun vardiyaGun) {
		boolean calisan = vardiyaGun != null;
		if (calisan) {
			if (vardiyaGun.getVardiya() != null) {

				calisan = vardiyaGun.isKullaniciYetkili() || (vardiyaGun.getIzin() != null && !helpPersonel(vardiyaGun.getPersonel()));
			}
		}
		return calisan;
	}

	/**
	 * @param personel
	 * @return
	 */
	private boolean helpPersonel(Personel personel) {
		return false;

	}

	/**
	 * @param bolumDoldurDurum
	 * @throws Exception
	 */
	public void tesisDoldur(boolean bolumDoldurDurum) throws Exception {
		planTanimsizBolumList = null;
		sirket = null;
		tumBolumPersonelleri = null;
		fazlaMesaiBakiyeGuncelleAyarla();
		bolumleriTemizle();
		if (pdksSirketList == null || pdksSirketList.isEmpty())
			setTesisList(new ArrayList<SelectItem>());
		else {
			if (sirketId != null) {

				sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
				if (!sirket.isTesisDurumu())
					tesisId = null;
			}
			ekSaha4Tanim = ortakIslemler.getEkSaha4(sirket, sirketId, session);
			List<SelectItem> list = fazlaMesaiOrtakIslemler.getFazlaMesaiTesisList(sirket, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, true, session);
			setTesisList(list);
			Long onceki = tesisId;
			if (list != null && !list.isEmpty()) {
				if (list.size() == 1 || onceki == null)
					tesisId = (Long) list.get(0).getValue();
				else if (onceki != null) {
					tesisId = null;
					for (SelectItem st : list) {
						if (st.getValue().equals(onceki))
							tesisId = onceki;
					}
				}
			}
			if (!bolumDoldurDurum)
				if (sirket != null && sirket.isTesisDurumu() == false)
					bolumDoldurDurum = true;
			onceki = tesisId;

			if (tesisId != null || (bolumDoldurDurum)) {
				bolumDoldur();
				setTesisId(onceki);

			}
			if (denklestirmeAyDurum == false)
				hataliPuantajGoster = Boolean.FALSE;
		}
		aylikPuantajList.clear();
	}

	/**
	 * 
	 */
	private void fazlaMesaiBakiyeGuncelleAyarla() {
		bakiyeGuncelle = ortakIslemler.getParameterKey("fazlaMesaiBakiyeGuncelle").equals("1") ? Boolean.FALSE : null;
	}

	/**
	 * @return
	 */
	public String bolumDoldur() {
		planTanimsizBolumList = null;
		hataliPersoneller = null;
		fazlaMesaiVardiyaGun = null;
		linkAdres = null;
		stajerSirket = Boolean.FALSE;
		bolumleriTemizle();
		Long oncekiEkSaha3Id = null;
		tumBolumPersonelleri = null;
		if (pdksSirketList == null || pdksSirketList.isEmpty())
			setGorevYeriList(new ArrayList<SelectItem>());
		else {

			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			if (userLogin.getSuperVisorHemsirePersonelNoList() != null) {
				if (hastaneSuperVisor == null) {
					String calistigiSayfa = userLogin.getCalistigiSayfa();
					String superVisorHemsireSayfalari = ortakIslemler.getParameterKey("superVisorHemsireSayfalari");
					List<String> sayfalar = PdksUtil.hasStringValue(superVisorHemsireSayfalari) ? PdksUtil.getListByString(superVisorHemsireSayfalari, null) : null;
					hastaneSuperVisor = sayfalar != null && sayfalar.contains(calistigiSayfa);
				}

			} else
				hastaneSuperVisor = Boolean.FALSE;

			aylikPuantajList.clear();
			Sirket sirket = null;
			if (sirketId != null) {

				sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
			}
			setSirket(sirket);

			if (sirket != null) {
				setDepartman(sirket.getDepartman());
				if (departman.isAdminMi() && sirket.isTesisDurumu()) {
					gorevYeriList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, tesisId != null ? String.valueOf(tesisId) : null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
				} else {
					gorevYeriList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
				}
				if (gorevYeriList.size() == 1) {
					seciliEkSaha3Id = (Long) gorevYeriList.get(0).getValue();
					oncekiEkSaha3Id = seciliEkSaha3Id;
				} else {
					if (ikRole == false && adminRole == false) {
						String str = ortakIslemler.getParameterKey("fazlaMesaiTumBolumSayisi");
						if (PdksUtil.hasStringValue(str)) {
							long tumBolumSayisi = 0;
							try {
								tumBolumSayisi = Long.parseLong(str);
							} catch (Exception e) {
								tumBolumSayisi = 0;
							}
							if (tumBolumSayisi > 0) {
								DepartmanDenklestirmeDonemi denklestirmeDonemi = new DepartmanDenklestirmeDonemi();
								AylikPuantaj aylikPuantaj = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, denklestirmeDonemi, session);
								setDepartman(sirket.getDepartman());
								List<Personel> tumBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(sirket, departman.isAdminMi() && sirket.isTesisDurumu() && tesisId != null ? String.valueOf(tesisId) : null, null, null, denklestirmeAy != null ? aylikPuantaj : null, sadeceFazlaMesai,
										session);
								if (tumBolumList.size() <= tumBolumSayisi) {
									List<SelectItem> bolumlist = ortakIslemler.getSelectItemList("fmBolum", authenticatedUser);
									String aciklama = "";
									if (sirket.isTesisDurumu() && tesisId != null)
										aciklama = ortakIslemler.getSelectItemText(tesisId, tesisList);
									if (bolumAciklama == null)
										bolumAciklama = ortakIslemler.bolumAciklama();
									if (PdksUtil.hasStringValue(aciklama))
										aciklama += " " + bolumAciklama;
									else
										aciklama = bolumAciklama;
									bolumlist.add(new SelectItem(0L, "Tüm " + aciklama + " Hepsi"));
									bolumlist.addAll(gorevYeriList);
									gorevYeriList.clear();
									gorevYeriList.addAll(bolumlist);
									bolumlist = null;
								}
							}
						}
					}

					if (ortakIslemler.getParameterKey("tumBolumPersonelGetir").equals("1") && !(ikRole || adminRole)) {
						tumBolumPersonelleri = fazlaMesaiOrtakIslemler.getTumBolumPersonelListesi(sirket, denklestirmeAy, tesisId, sadeceFazlaMesai, session);
						if (tumBolumPersonelleri != null) {
							if (!tumBolumPersonelleri.isEmpty())
								componentState.setSeciliTab("tab2");
							else
								tumBolumPersonelleri = null;
						}
					}
					if (seciliEkSaha3Id != null) {
						for (SelectItem st : gorevYeriList) {
							if (st.getValue().equals(seciliEkSaha3Id))
								oncekiEkSaha3Id = seciliEkSaha3Id;

						}

					}
				}

			}
		}

		if (ekSaha4Tanim != null) {
			altBolumList = ortakIslemler.getSelectItemList("altBolum", authenticatedUser);
			if (oncekiEkSaha3Id != null)
				altBolumDoldur();
			else {
				seciliEkSaha4Id = null;
				altBolumList = null;
			}
		} else {

			if ((ikRole || adminRole || authenticatedUser.isDirektorSuperVisor()) & denklestirmeAyDurum && gorevYeriList != null) {
				if (departman.isAdminMi() && sirket.isTesisDurumu()) {
					planTanimsizBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiTanimsizBolumList(sirket, tesisId != null ? String.valueOf(tesisId) : null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, session);
					// planTanimsizBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, tesisId != null ? String.valueOf(tesisId) : null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, false, session);
				} else {
					// planTanimsizBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, false, session);
					planTanimsizBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiTanimsizBolumList(sirket, null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, session);
				}

				if (planTanimsizBolumList != null && planTanimsizBolumList.isEmpty())
					planTanimsizBolumList = null;

			}
			altBolumList = null;
			seciliEkSaha4Id = null;
			aylikPuantajList.clear();
			if (seciliEkSaha3Id != null)
				fillHataliPersonelleriGuncelle();
		}

		return "";
	}

	/**
	 * @return
	 */
	public String altBolumDoldur() {

		aylikPuantajList.clear();
		hataliPersoneller = null;
		if (ekSaha4Tanim != null) {
			boolean hepsiEkle = true;
			List<SelectItem> list = fazlaMesaiOrtakIslemler.getFazlaMesaiAltBolumList(sirket, tesisId != null ? String.valueOf(tesisId) : null, seciliEkSaha3Id, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
			altBolumList = ortakIslemler.getSelectItemList("altBolum", authenticatedUser);
			if (list.size() > 1) {
				List<Personel> donemPerList = null;
				try {
					donemPerList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(sirket, tesisId != null ? String.valueOf(tesisId) : null, seciliEkSaha3Id, null, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, sadeceFazlaMesai, session);
				} catch (Exception e) {
					logger.error(e);
				}
				hepsiEkle = donemPerList == null || donemPerList.size() < 100;
				if (hepsiEkle == false)
					altBolumList.add(new SelectItem(null, "Seçiniz"));

				donemPerList = null;
			}
			if (hepsiEkle)
				altBolumList.add(new SelectItem(-1L, "Hepsi"));
			if (list != null && !list.isEmpty()) {
				altBolumList.addAll(list);
				boolean eski = list.size() == 1;
				if (eski)
					seciliEkSaha4Id = (Long) altBolumList.get(0).getValue();
				else if (seciliEkSaha4Id != null) {
					for (SelectItem st : list) {
						if (st.getValue().equals(seciliEkSaha4Id))
							eski = true;
					}
				}

				if (!eski)
					seciliEkSaha4Id = -1L;
			}
			list = null;

			if ((ikRole || adminRole || authenticatedUser.isDirektorSuperVisor()) & denklestirmeAyDurum && altBolumList != null) {
				planTanimsizBolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiTanimsizAltBolumList(sirket, tesisId != null ? String.valueOf(tesisId) : null, seciliEkSaha3Id, denklestirmeAy != null ? new AylikPuantaj(denklestirmeAy) : null, session);

				if (planTanimsizBolumList != null && planTanimsizBolumList.isEmpty())
					planTanimsizBolumList = null;

			}

			aylikPuantajList.clear();

		} else {

			altBolumList = null;
			seciliEkSaha4Id = null;
		}
		if (seciliEkSaha4Id != null)
			fillHataliPersonelleriGuncelle();

		return "";
	}

	/**
	 * 
	 */
	private void bolumleriTemizle() {
		gorevYeriList = null;
		fazlaMesaiOnayDurum = Boolean.FALSE;
	}

	/**
	 * @param aylikPuantaj
	 * @param dateStr
	 */
	public String puantajVardiyaGoster(AylikPuantaj aylikPuantaj, String dateStr) {
		if (aylikPuantaj != null && dateStr != null) {
			VardiyaGun vg = new VardiyaGun();
			vg.setVardiyaDate(PdksUtil.convertToJavaDate(dateStr, "yyyyMMdd"));
			vardiyaGoster(aylikPuantaj.getVardiya(vg));
			vg = null;
		}
		return "";
	}

	public String hareketSilme() {
		islemHareketKGS.setIslem(null);
		setIslemHareketKGS(null);
		return "";
	}

	/**
	 * @param kgsHareket
	 * @return
	 */
	public String hareketSil() {
		if (islemHareketKGS != null) {
			PersonelHareketIslem islem = islemHareketKGS.getIslem();
			if (islem.getNeden() != null) {
				long kgsId = 0, pdksId = 0;
				String str = islemHareketKGS.getId();
				Long id = Long.parseLong(str.substring(1));
				if (str.startsWith(HareketKGS.GIRIS_ISLEM_YAPAN_SIRKET_KGS))
					kgsId = id;
				else
					pdksId = id;
				if (kgsId > 0 || pdksId > 0) {
					id = pdksEntityController.hareketSil(kgsId, pdksId, authenticatedUser, islem.getNeden().getId(), islem.getAciklama(), islemHareketKGS.getKgsSirketIdLong(), session);
					if (id > 0) {
						fillPersonelDenklestirmeList(null);
						hareketIptalEt = false;
					}

				}
			} else {
				PdksUtil.addMessageWarn("İptal nedeni seçiniz!");
			}

		}
		islemHareketKGS.setIslem(null);
		setIslemHareketKGS(null);
		return "";
	}

	/**
	 * @param kgsHareket
	 * @return
	 */
	public String guncelleSil(HareketKGS kgsHareket) {
		setIslemHareketKGS(kgsHareket);
		PersonelHareketIslem islem = new PersonelHareketIslem();
		kgsHareket.setIslem(islem);
		hareketIptalNedenList = ortakIslemler.getTanimList(Tanim.TIPI_HAREKET_NEDEN, session);
		return "";
	}

	/**
	 * @param vg
	 */
	public String vardiyaGoster(VardiyaGun vg) {
		hareketIptalEt = false;
		islemHareketKGS = null;
		hareketIptalNedenList = null;
		seciliAylikPuantaj = aylikPuantajList != null && aylikPuantajList.size() == 1 ? aylikPuantajList.get(0) : null;
		if (seciliAylikPuantaj == null && vg != null) {
			Long perId = vg.getPersonel().getId();
			for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
				if (aylikPuantaj.getPdksPersonel().getId().equals(perId)) {
					seciliAylikPuantaj = aylikPuantaj;
					break;
				}

			}
		}

		setSeciliVardiyaGun(vg);
		fazlaMesaiVardiyaGun = vg;
		toplamFazlamMesai = 0D;
		Long key = vg.getId();
		fmtList = key != null && fmtMap != null && fmtMap.containsKey(key) ? fmtMap.get(key) : null;
		if (vg.getIzin() == null && vg.getIzinler() != null) {
			for (Iterator iterator = vg.getIzinler().iterator(); iterator.hasNext();) {
				PersonelIzin personelIzin = (PersonelIzin) iterator.next();
				if (personelIzin.isGunlukOldu())
					iterator.remove();
			}
		}
		List<HareketKGS> orjinalHareketler = vg.getOrjinalHareketler();
		if (orjinalHareketler != null) {
			if (denklestirmeAyDurum)
				ortakIslemler.setUpdateKGSHareket(orjinalHareketler, session);
			int hareketAdet = vg.getOrjinalHareketler().size();
			if (denklestirmeAyDurum && hareketAdet > 2 && vg.getFazlaMesaiOnayla() == null && vg.getHareketDurum() == false) {
				if (aylikPuantajList.size() < 10 || hareketAdet == 3)
					hareketIptalEt = userHome.hasPermission("personelHareket", "view");

			}
			for (HareketKGS hareket : orjinalHareketler) {
				if (hareket.getPersonelFazlaMesai() != null && hareket.getPersonelFazlaMesai().isOnaylandi()) {
					if (hareket.getPersonelFazlaMesai().getFazlaMesaiSaati() != null)
						toplamFazlamMesai += hareket.getPersonelFazlaMesai().getFazlaMesaiSaati();
				}
			}
		}
		fazlaMesaiTalepSil = Boolean.FALSE;
		if (denklestirmeAyDurum && ikRole && vg.getHareketDurum() == false && vg.getFazlaMesaiOnayla() == null) {
			if (vg.getFazlaMesaiTalepler() != null) {
				for (FazlaMesaiTalep fazlaMesaiTalep : vg.getFazlaMesaiTalepler()) {
					String titleStr = null;
					for (HareketKGS hareketKGS : vg.getHareketler()) {
						if (hareketKGS.getIslem() != null) {
							String aciklama = hareketKGS.getIslem().getAciklama();
							if (aciklama != null && aciklama.indexOf(":" + fazlaMesaiTalep.getId()) >= 0) {
								titleStr = aciklama.trim();
								fazlaMesaiTalepSil = Boolean.TRUE;
								break;
							}
						}
					}
					fazlaMesaiTalep.setCheckBoxDurum(titleStr != null);
					fazlaMesaiTalep.setTitleStr(titleStr);

				}
			}
		}
		if (vg.getTitleStr() == null) {
			String titleStr = fazlaMesaiOrtakIslemler.getFazlaMesaiSaatleri(vg, seciliAylikPuantaj.getLoginUser());
			if (denklestirmeAyDurum && eksikCalismaGoster) {
				Double netSure = vg.getVardiya().getNetCalismaSuresi();
				if (vg.getHareketDurum() && vg.getVardiya() != null && vg.isIzinli() == false && netSure > 0.0d) {
					Double calSure = calismaSuresi(vg);
					if ((calSure * 100) / netSure < denklestirmeAy.getYemekMolasiYuzdesi()) {
						titleStr += "<br/>" + getEksikCalismaHTML(vg);

					}
				}
			}
			vg.setTitleStr(titleStr);

			vg.addLinkAdresler(titleStr);
		}
		return "";
	}

	/**
	 * @param vg
	 * @return
	 */
	private Double calismaSuresi(VardiyaGun vg) {
		Double sure = vg.getCalismaSuresi();
		if (sure == null)
			sure = 0.0d;
		else if (vg.getHaftaCalismaSuresi() > 0.0d)
			sure -= vg.getHaftaCalismaSuresi();
		return sure;
	}

	/**
	 * @return
	 */
	private String getEksikCalismaHTML(VardiyaGun vg) {
		String str1 = "";
		VardiyaSaat vs = vg.getVardiyaSaat();
		if (vg.getDurum() && vs != null && vs.getEksikCalisma()) {
			Double calSure = vs.getCalismaSuresi();
			String str = "";
			try {
				Double sure = new Double((vs.getNormalSure() - calSure));
				if (sure < 1.0d)
					str = PdksUtil.numericValueFormatStr(sure * 60.0d, null) + " dakika ";
				else
					str = PdksUtil.numericValueFormatStr(sure, null) + " saat ";

			} catch (Exception e) {
				str = null;
			}
			str1 = "<SPAN style=\"color: " + (calSure > 0.0d ? "black" : "red") + "; font-size: 12px; font-weight: bold;\">Eksik çalışma var!" + (str != null ? " ( " + str + " ) " : "") + "</SPAN><br/><br/>";
		}
		return str1;
	}

	/**
	 * @return
	 */
	public String fazlaMesaiOtomatikHareketSil() {
		boolean secili = false;
		if (seciliVardiyaGun.getFazlaMesaiTalepler() != null) {
			for (FazlaMesaiTalep fazlaMesaiTalep : seciliVardiyaGun.getFazlaMesaiTalepler()) {
				if (fazlaMesaiTalep.isCheckBoxDurum()) {
					secili = true;
					Long id = fazlaMesaiOrtakIslemler.fazlaMesaiOtomatikHareketSil(fazlaMesaiTalep.getId(), session);
					if (id != null && id.equals(fazlaMesaiTalep.getId()))
						fazlaMesaiTalepSil = Boolean.FALSE;
				}
			}
		}
		if (!secili)
			PdksUtil.addMessageAvailableWarn("İşlem yapılacak seçili kayıt yok!");
		else if (fazlaMesaiTalepSil == false) {
			try {
				fillPersonelDenklestirmeList(null);
				PdksUtil.addMessageAvailableInfo("Kayıtlar seçili kayıtlar silindi.");

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		}

		return "";
	}

	// Haftalık çalışma planlarından giriş çıkış hareketi, izinler ve fazla
	// mesailerden haftalık toplam çalışma durumu hesaplanır

	public List<PersonelDenklestirme> getPersonelDenklestirmeList() {
		return personelDenklestirmeList;
	}

	public void setPersonelDenklestirmeList(List<PersonelDenklestirme> personelDenklestirmeList) {
		this.personelDenklestirmeList = personelDenklestirmeList;
	}

	public List<DepartmanDenklestirmeDonemi> getDenklestirmeDonemiList() {
		return denklestirmeDonemiList;
	}

	public void setDenklestirmeDonemiList(List<DepartmanDenklestirmeDonemi> denklestirmeDonemiList) {
		this.denklestirmeDonemiList = denklestirmeDonemiList;
	}

	public Sirket getSirket() {
		return sirket;
	}

	public void setSirket(Sirket value) {
		this.sirket = value;
	}

	public List<PersonelDenklestirme> getBaslikDenklestirmeDonemiList() {
		return baslikDenklestirmeDonemiList;
	}

	public void setBaslikDenklestirmeDonemiList(List<PersonelDenklestirme> baslikDenklestirmeDonemiList) {
		this.baslikDenklestirmeDonemiList = baslikDenklestirmeDonemiList;
	}

	public Boolean getHataYok() {
		return hataYok;
	}

	public void setHataYok(Boolean hataYok) {
		this.hataYok = hataYok;
	}

	public String getSicilNo() {
		return sicilNo;
	}

	public void setSicilNo(String sicilNo) {
		this.sicilNo = sicilNo;
	}

	public List<YemekIzin> getYemekAraliklari() {
		return yemekAraliklari;
	}

	public void setYemekAraliklari(List<YemekIzin> yemekAraliklari) {
		this.yemekAraliklari = yemekAraliklari;
	}

	public int getYil() {
		return yil;
	}

	public void setYil(int yil) {
		this.yil = yil;
	}

	public int getAy() {
		return ay;
	}

	public void setAy(int ay) {
		this.ay = ay;
	}

	public List<SelectItem> getAylar() {
		return aylar;
	}

	public void setAylar(List<SelectItem> aylar) {
		this.aylar = aylar;
	}

	public int getMaxYil() {
		return maxYil;
	}

	public void setMaxYil(int maxYil) {
		this.maxYil = maxYil;
	}

	public Boolean getYetkili() {
		return yetkili;
	}

	public void setYetkili(Boolean yetkili) {
		this.yetkili = yetkili;
	}

	public void setAylikPuantajList(List<AylikPuantaj> aylikPuantajList) {
		this.aylikPuantajList = aylikPuantajList;
	}

	public List<AylikPuantaj> getAylikPuantajList() {
		return aylikPuantajList;
	}

	public AylikPuantaj getAylikPuantajDefault() {
		return aylikPuantajDefault;
	}

	public void setAylikPuantajDefault(AylikPuantaj aylikPuantajDefault) {
		this.aylikPuantajDefault = aylikPuantajDefault;
	}

	public DenklestirmeAy getDenklestirmeAy() {
		return denklestirmeAy;
	}

	public void setDenklestirmeAy(DenklestirmeAy denklestirmeAy) {
		this.denklestirmeAy = denklestirmeAy;
	}

	public List<SelectItem> getGorevYeriList() {
		return gorevYeriList;
	}

	public void setGorevYeriList(List<SelectItem> value) {
		this.gorevYeriList = value;
	}

	public Long getSeciliEkSaha3Id() {
		return seciliEkSaha3Id;
	}

	public void setSeciliEkSaha3Id(Long seciliEkSaha3Id) {
		this.seciliEkSaha3Id = seciliEkSaha3Id;
	}

	public Tanim getGorevYeri() {
		return gorevYeri;
	}

	public void setGorevYeri(Tanim gorevYeri) {
		this.gorevYeri = gorevYeri;
	}

	public TreeMap<String, Tanim> getEkSahaTanimMap() {
		return ekSahaTanimMap;
	}

	public void setEkSahaTanimMap(TreeMap<String, Tanim> ekSahaTanimMap) {
		this.ekSahaTanimMap = ekSahaTanimMap;
	}

	public Boolean getResmiTatilVar() {
		return resmiTatilVar;
	}

	public void setResmiTatilVar(Boolean resmiTatilVar) {
		this.resmiTatilVar = resmiTatilVar;
	}

	public Boolean getKaydetDurum() {
		return kaydetDurum;
	}

	public void setKaydetDurum(Boolean kaydetDurum) {
		this.kaydetDurum = kaydetDurum;
	}

	public Long getGorevTipiId() {
		return gorevTipiId;
	}

	public void setGorevTipiId(Long gorevTipiId) {
		this.gorevTipiId = gorevTipiId;
	}

	public Long getSirketId() {
		return sirketId;
	}

	public void setSirketId(Long sirketId) {
		this.sirketId = sirketId;
	}

	public Boolean getSutIzniGoster() {
		return sutIzniGoster;
	}

	public void setSutIzniGoster(Boolean sutIzniGoster) {
		this.sutIzniGoster = sutIzniGoster;
	}

	public byte[] getExcelData() {
		return excelData;
	}

	public void setExcelData(byte[] excelData) {
		this.excelData = excelData;
	}

	public String getExcelDosyaAdi() {
		return excelDosyaAdi;
	}

	public void setExcelDosyaAdi(String excelDosyaAdi) {
		this.excelDosyaAdi = excelDosyaAdi;
	}

	public String getMailKonu() {
		return mailKonu;
	}

	public void setMailKonu(String mailKonu) {
		this.mailKonu = mailKonu;
	}

	public String getMailIcerik() {
		return mailIcerik;
	}

	public void setMailIcerik(String mailIcerik) {
		this.mailIcerik = mailIcerik;
	}

	public List<User> getToList() {
		return toList;
	}

	public void setToList(List<User> toList) {
		this.toList = toList;
	}

	public List<User> getCcList() {
		return ccList;
	}

	public void setCcList(List<User> ccList) {
		this.ccList = ccList;
	}

	public List<User> getBccList() {
		return bccList;
	}

	public void setBccList(List<User> bccList) {
		this.bccList = bccList;
	}

	public boolean isMailGonder() {
		return mailGonder;
	}

	public void setMailGonder(boolean mailGonder) {
		this.mailGonder = mailGonder;
	}

	public Boolean getOnayla() {
		return onayla;
	}

	public void setOnayla(Boolean onayla) {
		this.onayla = onayla;
	}

	public Long getDepartmanId() {
		return departmanId;
	}

	public void setDepartmanId(Long departmanId) {
		this.departmanId = departmanId;
	}

	public List<SelectItem> getDepartmanList() {
		return departmanList;
	}

	public void setDepartmanList(List<SelectItem> departmanList) {
		this.departmanList = departmanList;
	}

	public Departman getDepartman() {
		return departman;
	}

	public void setDepartman(Departman value) {
		this.departman = value;
	}

	public Tanim getSeciliBolum() {
		return seciliBolum;
	}

	public void setSeciliBolum(Tanim seciliBolum) {
		this.seciliBolum = seciliBolum;
	}

	public Boolean getHastaneSuperVisor() {
		return hastaneSuperVisor;
	}

	public void setHastaneSuperVisor(Boolean hastaneSuperVisor) {
		this.hastaneSuperVisor = hastaneSuperVisor;
	}

	public Double getToplamFazlamMesai() {
		return toplamFazlamMesai;
	}

	public void setToplamFazlamMesai(Double toplamFazlamMesai) {
		this.toplamFazlamMesai = toplamFazlamMesai;
	}

	public Vardiya getSabahVardiya() {
		return sabahVardiya;
	}

	public void setSabahVardiya(Vardiya sabahVardiya) {
		this.sabahVardiya = sabahVardiya;
	}

	public Boolean getAksamGun() {
		return aksamGun;
	}

	public void setAksamGun(Boolean aksamGun) {
		this.aksamGun = aksamGun;
	}

	public Boolean getAksamSaat() {
		return aksamSaat;
	}

	public void setAksamSaat(Boolean aksamSaat) {
		this.aksamSaat = aksamSaat;
	}

	public String getAdres() {
		return adres;
	}

	public void setAdres(String adres) {
		this.adres = adres;
	}

	public String getPersonelIzinGirisiStr() {
		return personelIzinGirisiStr;
	}

	public void setPersonelIzinGirisiStr(String personelIzinGirisiStr) {
		this.personelIzinGirisiStr = personelIzinGirisiStr;
	}

	public String getPersonelHareketStr() {
		return personelHareketStr;
	}

	public void setPersonelHareketStr(String personelHareketStr) {
		this.personelHareketStr = personelHareketStr;
	}

	public String getPersonelFazlaMesaiOrjStr() {
		return personelFazlaMesaiOrjStr;
	}

	public void setPersonelFazlaMesaiOrjStr(String personelFazlaMesaiOrjStr) {
		this.personelFazlaMesaiOrjStr = personelFazlaMesaiOrjStr;
	}

	public String getPersonelFazlaMesaiStr() {
		return personelFazlaMesaiStr;
	}

	public void setPersonelFazlaMesaiStr(String personelFazlaMesaiStr) {
		this.personelFazlaMesaiStr = personelFazlaMesaiStr;
	}

	public List<String> getSabahVardiyalar() {
		return sabahVardiyalar;
	}

	public void setSabahVardiyalar(List<String> sabahVardiyalar) {
		this.sabahVardiyalar = sabahVardiyalar;
	}

	public String getVardiyaPlaniStr() {
		return vardiyaPlaniStr;
	}

	public void setVardiyaPlaniStr(String vardiyaPlaniStr) {
		this.vardiyaPlaniStr = vardiyaPlaniStr;
	}

	public Boolean getPartTimeGoster() {
		return partTimeGoster;
	}

	public void setPartTimeGoster(Boolean partTimeGoster) {
		this.partTimeGoster = partTimeGoster;
	}

	public Boolean getStajerSirket() {
		return stajerSirket;
	}

	public void setStajerSirket(Boolean stajerSirket) {
		this.stajerSirket = stajerSirket;
	}

	public Boolean getBakiyeGuncelle() {
		return bakiyeGuncelle;
	}

	public void setBakiyeGuncelle(Boolean bakiyeGuncelle) {
		this.bakiyeGuncelle = bakiyeGuncelle;
	}

	public List<SelectItem> getPdksSirketList() {
		return pdksSirketList;
	}

	public void setPdksSirketList(List<SelectItem> value) {
		this.pdksSirketList = value;
	}

	public Boolean getHaftaTatilVar() {
		return haftaTatilVar;
	}

	public void setHaftaTatilVar(Boolean haftaTatilVar) {
		this.haftaTatilVar = haftaTatilVar;
	}

	public List<SelectItem> getTesisList() {
		return tesisList;
	}

	public void setTesisList(List<SelectItem> value) {
		this.tesisList = value;
	}

	public Long getTesisId() {
		return tesisId;
	}

	public void setTesisId(Long tesisId) {
		this.tesisId = tesisId;
	}

	public Boolean getDepartmanBolumAyni() {
		return departmanBolumAyni;
	}

	public void setDepartmanBolumAyni(Boolean departmanBolumAyni) {
		this.departmanBolumAyni = departmanBolumAyni;
	}

	public List<YemekIzin> getYemekList() {
		return yemekList;
	}

	public void setYemekList(List<YemekIzin> yemekList) {
		this.yemekList = yemekList;
	}

	public boolean isTekSirket() {
		return tekSirket;
	}

	public void setTekSirket(boolean tekSirket) {
		this.tekSirket = tekSirket;
	}

	public Boolean getModelGoster() {
		return modelGoster;
	}

	public void setModelGoster(Boolean modelGoster) {
		this.modelGoster = modelGoster;
	}

	public String getMsgError() {
		return msgError;
	}

	public void setMsgError(String msgError) {
		this.msgError = msgError;
	}

	public String getMsgFazlaMesaiError() {
		return msgFazlaMesaiError;
	}

	public void setMsgFazlaMesaiError(String msgFazlaMesaiError) {
		this.msgFazlaMesaiError = msgFazlaMesaiError;
	}

	public TreeMap<String, Tanim> getFazlaMesaiMap() {
		return fazlaMesaiMap;
	}

	public void setFazlaMesaiMap(TreeMap<String, Tanim> fazlaMesaiMap) {
		this.fazlaMesaiMap = fazlaMesaiMap;
	}

	public Integer getAksamVardiyaBasSaat() {
		return aksamVardiyaBasSaat;
	}

	public void setAksamVardiyaBasSaat(Integer aksamVardiyaBasSaat) {
		this.aksamVardiyaBasSaat = aksamVardiyaBasSaat;
	}

	public Integer getAksamVardiyaBasDakika() {
		return aksamVardiyaBasDakika;
	}

	public void setAksamVardiyaBasDakika(Integer aksamVardiyaBasDakika) {
		this.aksamVardiyaBasDakika = aksamVardiyaBasDakika;
	}

	public Integer getAksamVardiyaBitDakika() {
		return aksamVardiyaBitDakika;
	}

	public void setAksamVardiyaBitDakika(Integer aksamVardiyaBitDakika) {
		this.aksamVardiyaBitDakika = aksamVardiyaBitDakika;
	}

	public TreeMap<Long, List<FazlaMesaiTalep>> getFmtMap() {
		return fmtMap;
	}

	public void setFmtMap(TreeMap<Long, List<FazlaMesaiTalep>> fmtMap) {
		this.fmtMap = fmtMap;
	}

	public List<FazlaMesaiTalep> getFmtList() {
		return fmtList;
	}

	public void setFmtList(List<FazlaMesaiTalep> fmtList) {
		this.fmtList = fmtList;
	}

	public Boolean getAyrikHareketVar() {
		return ayrikHareketVar;
	}

	public void setAyrikHareketVar(Boolean ayrikHareketVar) {
		this.ayrikHareketVar = ayrikHareketVar;
	}

	public String getSanalPersonelAciklama() {
		return sanalPersonelAciklama;
	}

	public void setSanalPersonelAciklama(String sanalPersonelAciklama) {
		this.sanalPersonelAciklama = sanalPersonelAciklama;
	}

	public Boolean getHataliPuantajGoster() {
		return hataliPuantajGoster;
	}

	public void setHataliPuantajGoster(Boolean hataliPuantajGoster) {
		this.hataliPuantajGoster = hataliPuantajGoster;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Boolean getFazlaMesaiIzinKullan() {
		return fazlaMesaiIzinKullan;
	}

	public void setFazlaMesaiIzinKullan(Boolean fazlaMesaiIzinKullan) {
		this.fazlaMesaiIzinKullan = fazlaMesaiIzinKullan;
	}

	public Boolean getSirketIzinGirisDurum() {
		return sirketIzinGirisDurum;
	}

	public void setSirketIzinGirisDurum(Boolean sirketIzinGirisDurum) {
		this.sirketIzinGirisDurum = sirketIzinGirisDurum;
	}

	public CalismaModeli getPerCalismaModeli() {
		return perCalismaModeli;
	}

	public void setPerCalismaModeli(CalismaModeli perCalismaModeli) {
		this.perCalismaModeli = perCalismaModeli;
	}

	public Boolean getKullaniciPersonel() {
		return kullaniciPersonel;
	}

	public void setKullaniciPersonel(Boolean kullaniciPersonel) {
		this.kullaniciPersonel = kullaniciPersonel;
	}

	public Boolean getDenklestirmeAyDurum() {
		return denklestirmeAyDurum;
	}

	public void setDenklestirmeAyDurum(Boolean denklestirmeAyDurum) {
		this.denklestirmeAyDurum = denklestirmeAyDurum;
	}

	public TreeMap<String, TreeMap<String, List<VardiyaGun>>> getIzinTipiPersonelVardiyaMap() {
		return izinTipiPersonelVardiyaMap;
	}

	public void setIzinTipiPersonelVardiyaMap(TreeMap<String, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap) {
		this.izinTipiPersonelVardiyaMap = izinTipiPersonelVardiyaMap;
	}

	public List<Vardiya> getIzinTipiVardiyaList() {
		return izinTipiVardiyaList;
	}

	public void setIzinTipiVardiyaList(List<Vardiya> izinTipiVardiyaList) {
		this.izinTipiVardiyaList = izinTipiVardiyaList;
	}

	public boolean isAdminRole() {
		return adminRole;
	}

	public void setAdminRole(boolean adminRole) {
		this.adminRole = adminRole;
	}

	public boolean isIkRole() {
		return ikRole;
	}

	public void setIkRole(boolean ikRole) {
		this.ikRole = ikRole;
	}

	public Boolean getFazlaMesaiOnayDurum() {
		return fazlaMesaiOnayDurum;
	}

	public void setFazlaMesaiOnayDurum(Boolean fazlaMesaiOnayDurum) {
		this.fazlaMesaiOnayDurum = fazlaMesaiOnayDurum;
	}

	public VardiyaGun getSeciliVardiyaGun() {
		return seciliVardiyaGun;
	}

	public void setSeciliVardiyaGun(VardiyaGun seciliVardiyaGun) {
		this.seciliVardiyaGun = seciliVardiyaGun;
	}

	public Boolean getYoneticiRolVarmi() {
		return yoneticiRolVarmi;
	}

	public void setYoneticiRolVarmi(Boolean yoneticiRolVarmi) {
		this.yoneticiRolVarmi = yoneticiRolVarmi;
	}

	public String getMsgFazlaMesaiInfo() {
		return msgFazlaMesaiInfo;
	}

	public void setMsgFazlaMesaiInfo(String msgFazlaMesaiInfo) {
		this.msgFazlaMesaiInfo = msgFazlaMesaiInfo;
	}

	public boolean isFazlaMesaiTalepOnayliDurum() {
		return fazlaMesaiTalepOnayliDurum;
	}

	public void setFazlaMesaiTalepOnayliDurum(boolean fazlaMesaiTalepOnayliDurum) {
		this.fazlaMesaiTalepOnayliDurum = fazlaMesaiTalepOnayliDurum;
	}

	public Boolean getKesilenSureGoster() {
		return kesilenSureGoster;
	}

	public void setKesilenSureGoster(Boolean kesilenSureGoster) {
		this.kesilenSureGoster = kesilenSureGoster;
	}

	public List<Tanim> getDenklestirmeDinamikAlanlar() {
		return denklestirmeDinamikAlanlar;
	}

	public void setDenklestirmeDinamikAlanlar(List<Tanim> denklestirmeDinamikAlanlar) {
		this.denklestirmeDinamikAlanlar = denklestirmeDinamikAlanlar;
	}

	public Boolean getGebeGoster() {
		return gebeGoster;
	}

	public void setGebeGoster(Boolean gebeGoster) {
		this.gebeGoster = gebeGoster;
	}

	public Boolean getHatalariAyikla() {
		return hatalariAyikla;
	}

	public void setHatalariAyikla(Boolean hatalariAyikla) {
		this.hatalariAyikla = hatalariAyikla;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public HashMap<String, List<Tanim>> getEkSahaListMap() {
		return ekSahaListMap;
	}

	public void setEkSahaListMap(HashMap<String, List<Tanim>> ekSahaListMap) {
		this.ekSahaListMap = ekSahaListMap;
	}

	public String getBolumAciklama() {
		return bolumAciklama;
	}

	public void setBolumAciklama(String bolumAciklama) {
		this.bolumAciklama = bolumAciklama;
	}

	public Boolean getKismiOdemeGoster() {
		return kismiOdemeGoster;
	}

	public void setKismiOdemeGoster(Boolean kismiOdemeGoster) {
		this.kismiOdemeGoster = kismiOdemeGoster;
	}

	public List<HareketKGS> getHareketler() {
		return hareketler;
	}

	public void setHareketler(List<HareketKGS> hareketler) {
		this.hareketler = hareketler;
	}

	public HashMap<Long, List<HareketKGS>> getCiftBolumCalisanHareketMap() {
		return ciftBolumCalisanHareketMap;
	}

	public void setCiftBolumCalisanHareketMap(HashMap<Long, List<HareketKGS>> ciftBolumCalisanHareketMap) {
		this.ciftBolumCalisanHareketMap = ciftBolumCalisanHareketMap;
	}

	public HashMap<Long, Personel> getCiftBolumCalisanMap() {
		return ciftBolumCalisanMap;
	}

	public void setCiftBolumCalisanMap(HashMap<Long, Personel> ciftBolumCalisanMap) {
		this.ciftBolumCalisanMap = ciftBolumCalisanMap;
	}

	public AylikPuantaj getSeciliAylikPuantaj() {
		return seciliAylikPuantaj;
	}

	public void setSeciliAylikPuantaj(AylikPuantaj seciliAylikPuantaj) {
		this.seciliAylikPuantaj = seciliAylikPuantaj;
	}

	public String getTmpAlan() {
		return tmpAlan;
	}

	public void setTmpAlan(String tmpAlan) {
		this.tmpAlan = tmpAlan;
	}

	public Boolean getCheckBoxDurum() {
		return checkBoxDurum;
	}

	public void setCheckBoxDurum(Boolean checkBoxDurum) {
		this.checkBoxDurum = checkBoxDurum;
	}

	public String getKapiGirisSistemAdi() {
		return kapiGirisSistemAdi;
	}

	public void setKapiGirisSistemAdi(String kapiGirisSistemAdi) {
		this.kapiGirisSistemAdi = kapiGirisSistemAdi;
	}

	public boolean isYarimYuvarla() {
		return yarimYuvarla;
	}

	public void setYarimYuvarla(boolean yarimYuvarla) {
		this.yarimYuvarla = yarimYuvarla;
	}

	public Boolean getFazlaMesaiTalepSil() {
		return fazlaMesaiTalepSil;
	}

	public void setFazlaMesaiTalepSil(Boolean fazlaMesaiTalepSil) {
		this.fazlaMesaiTalepSil = fazlaMesaiTalepSil;
	}

	public Tanim getEkSaha4Tanim() {
		return ekSaha4Tanim;
	}

	public void setEkSaha4Tanim(Tanim ekSaha4Tanim) {
		this.ekSaha4Tanim = ekSaha4Tanim;
	}

	public Long getSeciliEkSaha4Id() {
		return seciliEkSaha4Id;
	}

	public void setSeciliEkSaha4Id(Long seciliEkSaha4Id) {
		this.seciliEkSaha4Id = seciliEkSaha4Id;
	}

	public List<SelectItem> getAltBolumList() {
		return altBolumList;
	}

	public void setAltBolumList(List<SelectItem> altBolumList) {
		this.altBolumList = altBolumList;
	}

	public Tanim getSeciliAltBolum() {
		return seciliAltBolum;
	}

	public void setSeciliAltBolum(Tanim seciliAltBolum) {
		this.seciliAltBolum = seciliAltBolum;
	}

	public boolean isSadeceFazlaMesai() {
		return sadeceFazlaMesai;
	}

	public void setSadeceFazlaMesai(boolean sadeceFazlaMesai) {
		this.sadeceFazlaMesai = sadeceFazlaMesai;
	}

	public Boolean getGecenAyDurum() {
		return gecenAyDurum;
	}

	public void setGecenAyDurum(Boolean gecenAyDurum) {
		this.gecenAyDurum = gecenAyDurum;
	}

	public DenklestirmeAy getGecenAy() {
		return gecenAy;
	}

	public void setGecenAy(DenklestirmeAy gecenAy) {
		this.gecenAy = gecenAy;
	}

	public boolean isEksikMaasGoster() {
		return eksikMaasGoster;
	}

	public void setEksikMaasGoster(boolean eksikMaasGoster) {
		this.eksikMaasGoster = eksikMaasGoster;
	}

	public String getBirdenFazlaKGSSirketSQL() {
		return birdenFazlaKGSSirketSQL;
	}

	public void setBirdenFazlaKGSSirketSQL(String birdenFazlaKGSSirketSQL) {
		this.birdenFazlaKGSSirketSQL = birdenFazlaKGSSirketSQL;
	}

	public boolean isBordroPuantajEkranindaGoster() {
		return bordroPuantajEkranindaGoster;
	}

	public void setBordroPuantajEkranindaGoster(boolean bordroPuantajEkranindaGoster) {
		this.bordroPuantajEkranindaGoster = bordroPuantajEkranindaGoster;
	}

	public TreeMap<String, Boolean> getBaslikMap() {
		return baslikMap;
	}

	public void setBaslikMap(TreeMap<String, Boolean> baslikMap) {
		this.baslikMap = baslikMap;
	}

	public List getSaveGenelList() {
		return saveGenelList;
	}

	public void setSaveGenelList(List saveGenelList) {
		this.saveGenelList = saveGenelList;
	}

	public Boolean getIzinGoster() {
		return izinGoster;
	}

	public void setIzinGoster(Boolean izinGoster) {
		this.izinGoster = izinGoster;
	}

	public boolean isPersonelHareketDurum() {
		return personelHareketDurum;
	}

	public void setPersonelHareketDurum(boolean personelHareketDurum) {
		this.personelHareketDurum = personelHareketDurum;
	}

	public boolean isPersonelFazlaMesaiDurum() {
		return personelFazlaMesaiDurum;
	}

	public void setPersonelFazlaMesaiDurum(boolean personelFazlaMesaiDurum) {
		this.personelFazlaMesaiDurum = personelFazlaMesaiDurum;
	}

	public boolean isVardiyaPlaniDurum() {
		return vardiyaPlaniDurum;
	}

	public void setVardiyaPlaniDurum(boolean vardiyaPlaniDurum) {
		this.vardiyaPlaniDurum = vardiyaPlaniDurum;
	}

	public boolean isPersonelIzinGirisiDurum() {
		return personelIzinGirisiDurum;
	}

	public void setPersonelIzinGirisiDurum(boolean personelIzinGirisiDurum) {
		this.personelIzinGirisiDurum = personelIzinGirisiDurum;
	}

	public Boolean getIzinCalismayanMailGonder() {
		return izinCalismayanMailGonder;
	}

	public void setIzinCalismayanMailGonder(Boolean izinCalismayanMailGonder) {
		this.izinCalismayanMailGonder = izinCalismayanMailGonder;
	}

	public String getManuelGirisGoster() {
		return manuelGirisGoster;
	}

	public void setManuelGirisGoster(String manuelGirisGoster) {
		this.manuelGirisGoster = manuelGirisGoster;
	}

	public boolean isPlanOnayDurum() {
		return planOnayDurum;
	}

	public void setPlanOnayDurum(boolean planOnayDurum) {
		this.planOnayDurum = planOnayDurum;
	}

	public boolean isEksikCalismaGoster() {
		return eksikCalismaGoster;
	}

	public void setEksikCalismaGoster(boolean eksikCalismaGoster) {
		this.eksikCalismaGoster = eksikCalismaGoster;
	}

	public int getSonDonem() {
		return sonDonem;
	}

	public void setSonDonem(int sonDonem) {
		this.sonDonem = sonDonem;
	}

	public Double getEksikSaatYuzde() {
		return eksikSaatYuzde;
	}

	public void setEksikSaatYuzde(Double eksikSaatYuzde) {
		this.eksikSaatYuzde = eksikSaatYuzde;
	}

	public Double getAksamCalismaSaati() {
		return aksamCalismaSaati;
	}

	public void setAksamCalismaSaati(Double aksamCalismaSaati) {
		this.aksamCalismaSaati = aksamCalismaSaati;
	}

	public Double getAksamCalismaSaatiYuzde() {
		return aksamCalismaSaatiYuzde;
	}

	public void setAksamCalismaSaatiYuzde(Double aksamCalismaSaatiYuzde) {
		this.aksamCalismaSaatiYuzde = aksamCalismaSaatiYuzde;
	}

	public Boolean getGerceklesenMesaiKod() {
		return gerceklesenMesaiKod;
	}

	public void setGerceklesenMesaiKod(Boolean gerceklesenMesaiKod) {
		this.gerceklesenMesaiKod = gerceklesenMesaiKod;
	}

	public Boolean getDevredenBakiyeKod() {
		return devredenBakiyeKod;
	}

	public void setDevredenBakiyeKod(Boolean devredenBakiyeKod) {
		this.devredenBakiyeKod = devredenBakiyeKod;
	}

	public Boolean getNormalCalismaSaatKod() {
		return normalCalismaSaatKod;
	}

	public void setNormalCalismaSaatKod(Boolean normalCalismaSaatKod) {
		this.normalCalismaSaatKod = normalCalismaSaatKod;
	}

	public Boolean getHaftaTatilCalismaSaatKod() {
		return haftaTatilCalismaSaatKod;
	}

	public void setHaftaTatilCalismaSaatKod(Boolean haftaTatilCalismaSaatKod) {
		this.haftaTatilCalismaSaatKod = haftaTatilCalismaSaatKod;
	}

	public Boolean getResmiTatilCalismaSaatKod() {
		return resmiTatilCalismaSaatKod;
	}

	public void setResmiTatilCalismaSaatKod(Boolean resmiTatilCalismaSaatKod) {
		this.resmiTatilCalismaSaatKod = resmiTatilCalismaSaatKod;
	}

	public Boolean getIzinSureSaatKod() {
		return izinSureSaatKod;
	}

	public void setIzinSureSaatKod(Boolean izinSureSaatKod) {
		this.izinSureSaatKod = izinSureSaatKod;
	}

	public Boolean getNormalCalismaGunKod() {
		return normalCalismaGunKod;
	}

	public void setNormalCalismaGunKod(Boolean normalCalismaGunKod) {
		this.normalCalismaGunKod = normalCalismaGunKod;
	}

	public Boolean getHaftaTatilCalismaGunKod() {
		return haftaTatilCalismaGunKod;
	}

	public void setHaftaTatilCalismaGunKod(Boolean haftaTatilCalismaGunKod) {
		this.haftaTatilCalismaGunKod = haftaTatilCalismaGunKod;
	}

	public Boolean getResmiTatilCalismaGunKod() {
		return resmiTatilCalismaGunKod;
	}

	public void setResmiTatilCalismaGunKod(Boolean resmiTatilCalismaGunKod) {
		this.resmiTatilCalismaGunKod = resmiTatilCalismaGunKod;
	}

	public Boolean getIzinSureGunKod() {
		return izinSureGunKod;
	}

	public void setIzinSureGunKod(Boolean izinSureGunKod) {
		this.izinSureGunKod = izinSureGunKod;
	}

	public Boolean getUcretliIzinGunKod() {
		return ucretliIzinGunKod;
	}

	public void setUcretliIzinGunKod(Boolean ucretliIzinGunKod) {
		this.ucretliIzinGunKod = ucretliIzinGunKod;
	}

	public Boolean getUcretsizIzinGunKod() {
		return ucretsizIzinGunKod;
	}

	public void setUcretsizIzinGunKod(Boolean ucretsizIzinGunKod) {
		this.ucretsizIzinGunKod = ucretsizIzinGunKod;
	}

	public Boolean getHastalikIzinGunKod() {
		return hastalikIzinGunKod;
	}

	public void setHastalikIzinGunKod(Boolean hastalikIzinGunKod) {
		this.hastalikIzinGunKod = hastalikIzinGunKod;
	}

	public Boolean getNormalGunKod() {
		return normalGunKod;
	}

	public void setNormalGunKod(Boolean normalGunKod) {
		this.normalGunKod = normalGunKod;
	}

	public Boolean getHaftaTatilGunKod() {
		return haftaTatilGunKod;
	}

	public void setHaftaTatilGunKod(Boolean haftaTatilGunKod) {
		this.haftaTatilGunKod = haftaTatilGunKod;
	}

	public Boolean getResmiTatilGunKod() {
		return resmiTatilGunKod;
	}

	public void setResmiTatilGunKod(Boolean resmiTatilGunKod) {
		this.resmiTatilGunKod = resmiTatilGunKod;
	}

	public Boolean getArtikGunKod() {
		return artikGunKod;
	}

	public void setArtikGunKod(Boolean artikGunKod) {
		this.artikGunKod = artikGunKod;
	}

	public Boolean getBordroToplamGunKod() {
		return bordroToplamGunKod;
	}

	public void setBordroToplamGunKod(Boolean bordroToplamGunKod) {
		this.bordroToplamGunKod = bordroToplamGunKod;
	}

	public List<String> getDevamlilikPrimIzinTipleri() {
		return devamlilikPrimIzinTipleri;
	}

	public void setDevamlilikPrimIzinTipleri(List<String> devamlilikPrimIzinTipleri) {
		this.devamlilikPrimIzinTipleri = devamlilikPrimIzinTipleri;
	}

	public Integer getAksamVardiyaBitSaat() {
		return aksamVardiyaBitSaat;
	}

	public void setAksamVardiyaBitSaat(Integer aksamVardiyaBitSaat) {
		this.aksamVardiyaBitSaat = aksamVardiyaBitSaat;
	}

	/**
	 * @return the devredenMesaiKod
	 */
	public Boolean getDevredenMesaiKod() {
		return devredenMesaiKod;
	}

	/**
	 * @param devredenMesaiKod
	 *            the devredenMesaiKod to set
	 */
	public void setDevredenMesaiKod(Boolean devredenMesaiKod) {
		this.devredenMesaiKod = devredenMesaiKod;
	}

	/**
	 * @return the ucretiOdenenKod
	 */
	public Boolean getUcretiOdenenKod() {
		return ucretiOdenenKod;
	}

	/**
	 * @param ucretiOdenenKod
	 *            the ucretiOdenenKod to set
	 */
	public void setUcretiOdenenKod(Boolean ucretiOdenenKod) {
		this.ucretiOdenenKod = ucretiOdenenKod;
	}

	/**
	 * @return the yoneticiERP1Kontrol
	 */
	public Boolean getYoneticiERP1Kontrol() {
		return yoneticiERP1Kontrol;
	}

	/**
	 * @param yoneticiERP1Kontrol
	 *            the yoneticiERP1Kontrol to set
	 */
	public void setYoneticiERP1Kontrol(Boolean yoneticiERP1Kontrol) {
		this.yoneticiERP1Kontrol = yoneticiERP1Kontrol;
	}

	/**
	 * @return the linkBordroAdres
	 */
	public String getLinkBordroAdres() {
		return linkBordroAdres;
	}

	/**
	 * @param linkBordroAdres
	 *            the linkBordroAdres to set
	 */
	public void setLinkBordroAdres(String linkBordroAdres) {
		this.linkBordroAdres = linkBordroAdres;
	}

	/**
	 * @return the saatlikCalismaGoster
	 */
	public boolean isSaatlikCalismaGoster() {
		return saatlikCalismaGoster;
	}

	/**
	 * @param saatlikCalismaGoster
	 *            the saatlikCalismaGoster to set
	 */
	public void setSaatlikCalismaGoster(boolean saatlikCalismaGoster) {
		this.saatlikCalismaGoster = saatlikCalismaGoster;
	}

	/**
	 * @return the izinBordoroGoster
	 */
	public boolean isIzinBordoroGoster() {
		return izinBordoroGoster;
	}

	/**
	 * @param izinBordoroGoster
	 *            the izinBordoroGoster to set
	 */
	public void setIzinBordoroGoster(boolean izinBordoroGoster) {
		this.izinBordoroGoster = izinBordoroGoster;
	}

	/**
	 * @return the msgwarnImg
	 */
	public String getMsgwarnImg() {
		return msgwarnImg;
	}

	/**
	 * @param msgwarnImg
	 *            the msgwarnImg to set
	 */
	public void setMsgwarnImg(String msgwarnImg) {
		this.msgwarnImg = msgwarnImg;
	}

	public Boolean getYasalFazlaCalismaAsanSaat() {
		return yasalFazlaCalismaAsanSaat;
	}

	public void setYasalFazlaCalismaAsanSaat(Boolean yasalFazlaCalismaAsanSaat) {
		this.yasalFazlaCalismaAsanSaat = yasalFazlaCalismaAsanSaat;
	}

	public Boolean getFazlaMesaiOde() {
		return fazlaMesaiOde;
	}

	public void setFazlaMesaiOde(Boolean fazlaMesaiOde) {
		this.fazlaMesaiOde = fazlaMesaiOde;
	}

	public List<Personel> getTumBolumPersonelleri() {
		return tumBolumPersonelleri;
	}

	public void setTumBolumPersonelleri(List<Personel> tumBolumPersonelleri) {
		this.tumBolumPersonelleri = tumBolumPersonelleri;
	}

	public List<SelectItem> getPlanTanimsizBolumList() {
		return planTanimsizBolumList;
	}

	public void setPlanTanimsizBolumList(List<SelectItem> planTanimsizBolumList) {
		this.planTanimsizBolumList = planTanimsizBolumList;
	}

	public Long getPlanTanimsizBolumId() {
		return planTanimsizBolumId;
	}

	public void setPlanTanimsizBolumId(Long planTanimsizBolumId) {
		this.planTanimsizBolumId = planTanimsizBolumId;
	}

	public boolean isHareketIptalEt() {
		return hareketIptalEt;
	}

	public void setHareketIptalEt(boolean hareketIptalEt) {
		this.hareketIptalEt = hareketIptalEt;
	}

	public List<Tanim> getHareketIptalNedenList() {
		return hareketIptalNedenList;
	}

	public void setHareketIptalNedenList(List<Tanim> hareketIptalNedenList) {
		this.hareketIptalNedenList = hareketIptalNedenList;
	}

	public HareketKGS getIslemHareketKGS() {
		return islemHareketKGS;
	}

	public void setIslemHareketKGS(HareketKGS islemHareketKGS) {
		this.islemHareketKGS = islemHareketKGS;
	}

	public boolean isTopluGuncelle() {
		return topluGuncelle;
	}

	public void setTopluGuncelle(boolean topluGuncelle) {
		this.topluGuncelle = topluGuncelle;
	}

	public Boolean getSuaGoster() {
		return suaGoster;
	}

	public void setSuaGoster(Boolean suaGoster) {
		this.suaGoster = suaGoster;
	}

	public boolean isIstifaGoster() {
		return istifaGoster;
	}

	public void setIstifaGoster(boolean istifaGoster) {
		this.istifaGoster = istifaGoster;
	}

	public List<SelectItem> getHataliPersoneller() {
		return hataliPersoneller;
	}

	public void setHataliPersoneller(List<SelectItem> hataliPersoneller) {
		this.hataliPersoneller = hataliPersoneller;
	}

	public String getHataliSicilNo() {
		return hataliSicilNo;
	}

	public void setHataliSicilNo(String hataliSicilNo) {
		this.hataliSicilNo = hataliSicilNo;
	}

	public Boolean getIsAramaGoster() {
		return isAramaGoster;
	}

	public void setIsAramaGoster(Boolean isAramaGoster) {
		this.isAramaGoster = isAramaGoster;
	}

	public Boolean getBakiyeSifirlaDurum() {
		return bakiyeSifirlaDurum;
	}

	public void setBakiyeSifirlaDurum(Boolean bakiyeSifirlaDurum) {
		this.bakiyeSifirlaDurum = bakiyeSifirlaDurum;
	}

}
