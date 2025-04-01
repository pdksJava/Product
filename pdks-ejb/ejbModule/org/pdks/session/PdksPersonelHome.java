package org.pdks.session;

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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.FacesException;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.mail.internet.InternetAddress;
import javax.persistence.EntityManager;
import javax.ws.rs.HttpMethod;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.AramaSecenekleri;
import org.pdks.entity.CalismaModeli;
import org.pdks.entity.Departman;
import org.pdks.entity.Dosya;
import org.pdks.entity.IzinTipi;
import org.pdks.entity.KapiSirket;
import org.pdks.entity.Liste;
import org.pdks.entity.MailGrubu;
import org.pdks.entity.Notice;
import org.pdks.entity.PdksPersonelView;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDinamikAlan;
import org.pdks.entity.PersonelDonemselDurum;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelIzinDetay;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelView;
import org.pdks.entity.Sirket;
import org.pdks.entity.Tanim;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaSablonu;
import org.pdks.enums.NoteTipi;
import org.pdks.enums.OrganizasyonTipi;
import org.pdks.enums.PersonelDurumTipi;
import org.pdks.erp.entity.PersonelERPDB;
import org.pdks.quartz.PersonelERPGuncelleme;
import org.pdks.security.entity.DefaultPasswordGenerator;
import org.pdks.security.entity.Role;
import org.pdks.security.entity.User;
import org.pdks.security.entity.UserDigerOrganizasyon;
import org.pdks.security.entity.UserMenuItemTime;
import org.pdks.security.entity.UserRoles;
import org.richfaces.component.UITree;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.TreeNode;
import org.richfaces.model.TreeNodeImpl;
import org.richfaces.model.UploadItem;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.internal.LinkedTreeMap;
import com.pdks.webservice.MailObject;
import com.pdks.webservice.MailPersonel;
import com.pdks.webservice.MailStatu;
import com.pdks.webservice.PdksSoapVeriAktar;
import com.pdks.webservice.PersonelERP;

@Name("pdksPersonelHome")
public class PdksPersonelHome extends EntityHome<Personel> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8547356626329930627L;
	/**
	 * 
	 */
	static Logger logger = Logger.getLogger(PdksPersonelHome.class);
	@RequestParameter
	Long kapiId;

	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = false, create = true)
	User authenticatedUser;
	@In(required = true, create = true)
	OrtakIslemler ortakIslemler;
	@In(required = false, create = true)
	HashMap<String, String> parameterMap;

	@In(create = true)
	Renderer renderer;

	public static String sayfaURL = "personelTanimlama";

	public static final String MAIL_CC = MailGrubu.TIPI_CC;
	public static final String MAIL_BCC = MailGrubu.TIPI_BCC;
	public static final String MAIL_HAREKET = MailGrubu.TIPI_HAREKET;
	private String iconLeaf = "/img/plus.gif";
	private TreeNode<PersonelView> rootNodeForAllPersonelView;
	private Dosya personelDosya = new Dosya();
	private HashMap<String, List<Tanim>> ekSahaListMap;
	private HashMap<Long, List<Tanim>> dinamikPersonelAciklamaMap;
	private TreeMap<String, Tanim> ekSahaTanimMap;
	private List<Tanim> bolumDepartmanlari, gorevDepartmanlari, personelTipleri;
	private List<Tanim> dinamikDurumList, dinamikSayisalList, dinamikTanimList;
	private List<PersonelDinamikAlan> dinamikPersonelDurumList, dinamikPersonelSayisalList, dinamikPersonelTanimList;

	private PersonelDinamikAlan personelDinamikAlan;
	private HashMap<String, Boolean> personelDurumMap = new HashMap<String, Boolean>();
	private PersonelDonemselDurum donemselDurum;
	private List<SelectItem> personelDurumTipiList;
	private List<Vardiya> calismaModeliVardiyaList;
	private List<PersonelDonemselDurum> donemselDurumList;
	private TreeMap<String, PersonelDinamikAlan> personelDinamikMap;
	private PersonelView personelView;
	private List<Departman> departmanTanimList = new ArrayList<Departman>(), departmanKullaniciList = new ArrayList<Departman>();
	private List<Tanim> unvanTanimList = new ArrayList<Tanim>(), departmanPDKSTanimList = new ArrayList<Tanim>(), cinsiyetList, masrafYeriList, tesisList, yoneticiVardiyaTipiList, vardiyaGirisTipiTanimList = new ArrayList<Tanim>();
	private List<PersonelERP> personelERPList;
	private List<VardiyaSablonu> sablonlar = new ArrayList<VardiyaSablonu>();
	private List<PersonelKGS> personelKGSList = new ArrayList<PersonelKGS>();
	private List<PersonelView> tanimsizPersonelList;
	private List<Personel> pdksPersonelList = new ArrayList<Personel>(), yoneticiList, yonetici2List, ikinciYoneticiHataliList;
	private List<Sirket> sirketList = new ArrayList<Sirket>();
	private List<UserMenuItemTime> menuItemTimeList;
	private List<PersonelView> personelList = new ArrayList<PersonelView>();
	private List<Liste> dosyaTanimList = new ArrayList<Liste>();
	private HashMap<Long, List<String>> gebeIcapSuaDurumMap = new HashMap<Long, List<String>>();

	private List<CalismaModeli> calismaModeliList = new ArrayList<CalismaModeli>();

	private List<PersonelERP> personelERPReturnList;

	private List<Role> distinctRoleList = new ArrayList<Role>();
	private List<Tanim> distinctTesisList = new ArrayList<Tanim>(), distinctBolumList = new ArrayList<Tanim>();
	private List<Long> gebeSutIzniDurumList = new ArrayList<Long>();

	private List<String> ccAdresList = null, bccAdresList = null, hareketAdresList = null;
	private String adi, soyadi, sicilNo, yoneticiTipi, ccAdres, adresTipi, sanalPersonelAciklama, ePosta, denemeMesaj;
	private String bolumAciklama, departmanAciklama, kartNoAciklama, altBolumAciklama, sirketKodu = "";
	private User user, eskiKullanici;
	private Date tarih;
	private Tanim bosDepartman, parentDepartman, parentBordroTanim;
	private String oldUserName, bosDepartmanKodu;

	private PersonelIzin bakiyeIzin;
	private Double bakiyeIzinSuresi;
	private Sirket oldSirket;
	private Personel asilYonetici1;
	private String hataMesaj = "", personelERPGuncelleme = "";
	private Boolean updateValue, yeniPersonelGuncelle, pdks, servisCalisti = Boolean.FALSE, fazlaMesaiIzinKullan = Boolean.FALSE, gebeMi = Boolean.FALSE, bolumYetki = Boolean.FALSE, tesisYetki = Boolean.FALSE, istenAyrilmaGoster = Boolean.FALSE;
	private Boolean sutIzni = Boolean.FALSE, kimlikNoGoster = Boolean.FALSE, kullaniciPersonel = Boolean.FALSE, sanalPersonel = Boolean.FALSE, icapDurum = Boolean.FALSE, yoneticiRolVarmi = Boolean.FALSE;
	private Boolean ustYonetici = Boolean.FALSE, fazlaMesaiOde = Boolean.FALSE, suaOlabilir = Boolean.FALSE, izinKartiVardir = Boolean.FALSE, egitimDonemi = Boolean.FALSE, partTimeDurum = Boolean.FALSE, tesisDurum = Boolean.FALSE;
	private Boolean emailCCDurum = Boolean.FALSE, emailBCCDurum = Boolean.FALSE, taseronKulaniciTanimla = Boolean.FALSE, manuelTanimla = Boolean.FALSE, ikinciYoneticiManuelTanimla = Boolean.FALSE;
	private Boolean onaysizIzinKullanilir = Boolean.FALSE, departmanGoster = Boolean.FALSE, kartNoGoster = Boolean.FALSE, ikinciYoneticiIzinOnayla = Boolean.FALSE, izinGirisiVar = Boolean.FALSE, dosyaGuncellemeYetki = Boolean.FALSE;
	private Boolean ekSaha1Disable, ekSaha2Disable, ekSaha4Disable, transferAciklamaCiftKontrol, bakiyeIzinGoster = Boolean.FALSE, gebeSecim = Boolean.FALSE, personelTipiGoster = Boolean.FALSE;
	public Boolean disableAdviseNodeOpened, organizasyonSemasiGoster = Boolean.FALSE, bakiyeTakipEdiliyor = Boolean.FALSE;
	private TreeMap<Long, PersonelKGS> personelKGSMap;
	private int COL_SICIL_NO, COL_ADI, COL_SOYADI, COL_SIRKET_KODU, COL_SIRKET_ADI, COL_TESIS_KODU, COL_TESIS_ADI, COL_GOREV_KODU, COL_GOREVI, COL_BOLUM_KODU, COL_BOLUM_ADI;
	private int COL_ISE_BASLAMA_TARIHI, COL_KIDEM_TARIHI, COL_GRUBA_GIRIS_TARIHI, COL_ISTEN_AYRILMA_TARIHI, COL_DOGUM_TARIHI, COL_CINSIYET_KODU, COL_CINSIYET, COL_YONETICI_KODU, COL_YONETICI2_KODU;
	private int COL_DEPARTMAN_KODU, COL_DEPARTMAN_ADI, COL_MASRAF_YERI_KODU, COL_MASRAF_YERI_ADI, COL_BORDRO_ALT_ALAN_KODU, COL_BORDRO_ALT_ALAN_ADI, COL_BORDRO_SANAL_PERSONEL;
	private int COL_PERSONEL_TIPI, COL_PERSONEL_TIPI_ADI;

	private Session session;

	@Override
	public Object getId() {
		if (kapiId == null) {
			return super.getId();
		} else {
			return kapiId;
		}
	}

	@Override
	@Begin(join = true)
	public void create() {
		super.create();
	}

	/**
	 * @param id
	 * @return
	 */
	public boolean gebeSutIzniDurum(Long id) {
		boolean gebeSutIzniDurum = false;
		if (id != null && gebeSutIzniDurumList != null)
			gebeSutIzniDurum = gebeSutIzniDurumList.contains(id);
		return gebeSutIzniDurum;
	}

	/**
	 * @param tipi
	 * @return
	 */
	public String gebeSutIzniTarihDegisti() {
		if (donemselDurum.getId() == null && donemselDurum.isSutIzni()) {
			if (donemselDurum.getBasTarih() == null) {
				if (donemselDurum.getBitTarih() != null)
					donemselDurum.setBasTarih(PdksUtil.tariheGunEkleCikar(PdksUtil.tariheAyEkleCikar(donemselDurum.getBitTarih(), -12), -1));
			} else if (donemselDurum.getBitTarih() == null) {
				if (donemselDurum.getBasTarih() != null)
					donemselDurum.setBitTarih(PdksUtil.tariheGunEkleCikar(PdksUtil.tariheAyEkleCikar(donemselDurum.getBasTarih(), 12), -1));
			}
		}
		return "";
	}

	/**
	 * @param personel
	 * @return
	 */
	public String gebeSutIzniSecimi(Personel personel) {
		donemselDurum = new PersonelDonemselDurum();
		donemselDurum.setPersonel(personel);
		donemselDurumList = null;
		personelDurumTipiList = ortakIslemler.getSelectItemList("personelDurumTipi", authenticatedUser);
		if (personel != null) {
			session.clear();
			donemselDurumList = pdksEntityController.getSQLParamByFieldList(PersonelDonemselDurum.TABLE_NAME, PersonelDonemselDurum.COLUMN_NAME_PERSONEL, personel.getId(), PersonelDonemselDurum.class, session);

			if (!donemselDurumList.isEmpty()) {
				Date bugun = PdksUtil.getDate(new Date());
				donemselDurumList = PdksUtil.sortListByAlanAdi(donemselDurumList, "basTarih", Boolean.FALSE);
				boolean flush = false;
				List<PersonelDonemselDurum> pasifList = new ArrayList<PersonelDonemselDurum>();
				for (Iterator iterator = donemselDurumList.iterator(); iterator.hasNext();) {
					PersonelDonemselDurum personelDonemselDurum = (PersonelDonemselDurum) iterator.next();
					if (personelDonemselDurum.getDurum()) {
						if (personelDonemselDurum.getBasTarih().getTime() <= bugun.getTime() && personelDonemselDurum.getBitTarih().getTime() >= bugun.getTime()) {
							if (personelDonemselDurum.isGebe()) {
								if (personel.isGebelikMuayeneIzniKullan() == false || personel.isSutIzniKullan()) {
									personel.setGebeMi(false);
									personel.setSutIzni(false);
									personel.setGuncellemeTarihi(personelDonemselDurum.getSonIslemTarihi());
									personel.setGuncelleyenUser(personelDonemselDurum.getSonIslemYapan());
									flush = true;
								}
							} else if (personelDonemselDurum.isSutIzni()) {
								if (personel.isGebelikMuayeneIzniKullan() || personel.isSutIzniKullan() == false) {
									personel.setGebeMi(false);
									personel.setSutIzni(false);
									personel.setGuncellemeTarihi(personelDonemselDurum.getSonIslemTarihi());
									personel.setGuncelleyenUser(personelDonemselDurum.getSonIslemYapan());
									flush = true;
								}
							}
						}
					} else {
						pasifList.add(personelDonemselDurum);
						iterator.remove();
					}

				}
				if (!pasifList.isEmpty())
					donemselDurumList.addAll(pasifList);
				pasifList = null;
				if (flush) {
					pdksEntityController.saveOrUpdate(session, entityManager, personel);
					session.flush();
				}

			}
			List<Integer> list = new ArrayList<Integer>();
			if (personel.getSirket().isGebelikSutIzinVar()) {
				if (personel.getCinsiyetBayan()) {
					if (secGebe(personel))
						list.add(PersonelDurumTipi.GEBE.value());
					list.add(PersonelDurumTipi.SUT_IZNI.value());
				}
			}
			if (getIsAramaIzinDurum(personel))
				list.add(PersonelDurumTipi.IS_ARAMA_IZNI.value());
			for (Integer tipi : list)
				personelDurumTipiList.add(new SelectItem(tipi, PersonelDonemselDurum.getPersonelDurumTipiAciklama(tipi)));
			list = null;
			if (personelDurumTipiList.size() == 1)
				donemselDurum.setPersonelDurumTipiId((Integer) personelDurumTipiList.get(0).getValue());
		}
		setInstance(personel);
		return "";

	}

	/**
	 * @param value
	 * @return
	 */
	public String setPersonelDonemselDurum(PersonelDonemselDurum value) {
		if (value == null) {
			Personel personel = getInstance();
			value = new PersonelDonemselDurum();
			value.setPersonel(personel);
			if (personelDurumTipiList.size() == 1)
				donemselDurum.setPersonelDurumTipiId((Integer) personelDurumTipiList.get(0).getValue());
		}
		donemselDurum = value;
		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String savePersonelDonemselDurum() {
		List<PersonelDonemselDurum> list = null;
		boolean devam = true;
		if (donemselDurum.getPersonelDurumTipiId() == null) {
			PdksUtil.addMessageWarn("Tipi seçiniz!!");
			devam = false;
		}
		if (donemselDurum.getBasTarih() == null) {
			PdksUtil.addMessageWarn("Başlangıç tarihi giriniz!");
			devam = false;
		} else if (donemselDurum.getBitTarih() == null) {
			PdksUtil.addMessageWarn("Bitiş tarihi giriniz!");
			devam = false;
		} else if (donemselDurum.getBasTarih().after(donemselDurum.getBitTarih())) {
			PdksUtil.addMessageWarn("Başlangıç tarihi bitişi tarihinden büyük olamaz!");
			devam = false;
		}

		if (devam) {
			if (donemselDurum.getDurum()) {
				HashMap fields = new HashMap();
				fields.put("personel.id=", donemselDurum.getPersonel().getId());
				fields.put("bitTarih>=", donemselDurum.getBasTarih());
				fields.put("basTarih<=", donemselDurum.getBitTarih());
				if (donemselDurum.getId() != null)
					fields.put("id<>", donemselDurum.getId());
				if (getIsAramaIzinDurum(donemselDurum.getPersonel())) {
					if (donemselDurum.getIsAramaIzni())
						fields.put("personelDurumTipiId=", donemselDurum.getPersonelDurumTipiId());
					else
						fields.put("personelDurumTipiId<>", donemselDurum.getPersonelDurumTipiId());
				}
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				list = pdksEntityController.getObjectByInnerObjectListInLogic(fields, PersonelDonemselDurum.class);
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					PersonelDonemselDurum personelDonemselDurum = (PersonelDonemselDurum) iterator.next();
					if (personelDonemselDurum.getDurum().equals(Boolean.FALSE))
						iterator.remove();
				}
			}
			if (list == null || list.isEmpty()) {
				if (donemselDurum.getId() == null) {
					donemselDurum.setOlusturmaTarihi(new Date());
					donemselDurum.setOlusturanUser(authenticatedUser);
				} else {
					donemselDurum.setGuncellemeTarihi(new Date());
					donemselDurum.setGuncelleyenUser(authenticatedUser);
				}
				pdksEntityController.saveOrUpdate(session, entityManager, donemselDurum);
				session.flush();
				gebeSutIzniSecimi(donemselDurum.getPersonel());
			} else {
				PersonelDonemselDurum personelDonemselDurum = list.get(0);
				PdksUtil.addMessageWarn(authenticatedUser.dateFormatla(personelDonemselDurum.getBasTarih()) + " - " + authenticatedUser.dateFormatla(personelDonemselDurum.getBitTarih()) + " " + personelDonemselDurum.getPersonelDurumTipiAciklama() + " bilgisi girilmiştir!");
			}
			list = null;
		}

		return "";

	}

	/**
	 * @param alan
	 * @return
	 */
	public String durumGebeSutIzni(String alan) {
		Personel per = getInstance();
		if (per.getCinsiyetBayan()) {
			Boolean durumu = false;
			if (alan.equalsIgnoreCase("G"))
				durumu = per.getGebeMi();
			else if (alan.equalsIgnoreCase("S"))
				durumu = per.getSutIzni();
			if (durumu != null && durumu) {
				if (alan.equalsIgnoreCase("G"))
					per.setSutIzni(Boolean.FALSE);
				else if (alan.equalsIgnoreCase("S"))
					per.setGebeMi(Boolean.FALSE);
			}
		}
		return "";
	}

	/**
	 * @param alan
	 * @param pdksPersonel
	 * @return
	 */
	public PersonelDinamikAlan getPersonelDinamikAlan(Tanim alan, Personel pdksPersonel) {
		PersonelDinamikAlan dinamikAlan = null;
		if (personelDinamikMap != null && alan != null && pdksPersonel != null) {
			String key = PersonelDinamikAlan.getKey(pdksPersonel, alan);
			if (personelDinamikMap.containsKey(key))
				dinamikAlan = personelDinamikMap.get(key);
		}
		if (dinamikAlan == null)
			dinamikAlan = new PersonelDinamikAlan(pdksPersonel, alan);
		return dinamikAlan;
	}

	/**
	 * @return
	 */
	public String cinsiyetDegisti() {
		return "";
	}

	/**
	 * @return
	 */
	public String adresGuncelle() {
		Personel personel = getInstance();
		if (adresTipi != null) {
			if (adresTipi.equals(MAIL_CC)) {
				String sb = ortakIslemler.adresDuzelt(ccAdresList);
				personel.setEmailCC(sb);
			} else if (adresTipi.equals(MAIL_BCC)) {
				String sb = ortakIslemler.adresDuzelt(bccAdresList);
				personel.setEmailBCC(sb);
			} else if (adresTipi.equals(MAIL_HAREKET)) {
				String sb = ortakIslemler.adresDuzelt(hareketAdresList);
				personel.setHareketMail(sb);
			}
		}

		ccAdres = "";
		return "";

	}

	/**
	 * @return
	 */
	public String personelTipiDegisti() {
		Personel personel = getInstance();
		fillCalismaModeli(personel);
		if (personel.getPersonelTipi() != null) {
			Long id = personel.getPersonelTipi().getId();
			for (Iterator iterator = calismaModeliList.iterator(); iterator.hasNext();) {
				CalismaModeli cm = (CalismaModeli) iterator.next();
				if (cm.getPersonelTipi() != null && cm.getPersonelTipi().getId().longValue() != id.longValue())
					iterator.remove();
			}
		}
		if (calismaModeliList.size() == 1) {
			personel.setCalismaModeli(calismaModeliList.get(0));
			modelDegisti();
		}

		return "";
	}

	/**
	 * @return
	 */
	public String sablonDegisti() {
		Personel personel = getInstance();
		if (personel.getSablon() != null) {
			VardiyaSablonu sablon = personel.getSablon();
			if (sablon.getCalismaModeli() != null) {
				personel.setCalismaModeli(calismaModeliList.get(0));
				fillCalismaModeliVardiyaList();
			}

		}
		return "";
	}

	/**
	 * @return
	 */
	public String modelDegisti() {
		Personel personel = getInstance();
		if (personel.getCalismaModeli() != null) {
			CalismaModeli calismaModeli = personel.getCalismaModeli();
			if (calismaModeli.getBagliVardiyaSablonu() != null)
				personel.setSablon(calismaModeli.getBagliVardiyaSablonu());
		}
		fillCalismaModeliVardiyaList();
		return "";
	}

	/**
	 * @param adres
	 * @return
	 */
	public String adresSil(String adres) {
		if (adresTipi.equals(MAIL_CC)) {
			if (ccAdresList.contains(adres))
				ccAdresList.remove(adres);

		} else if (adresTipi.equals(MAIL_BCC)) {
			if (bccAdresList.contains(adres))
				bccAdresList.remove(adres);

		} else if (adresTipi.equals(MAIL_HAREKET)) {
			if (hareketAdresList.contains(adres))
				hareketAdresList.remove(adres);

		}
		return "";

	}

	/**
	 * @return
	 */
	public String adresEkle() {
		String eMail = PdksUtil.setTurkishStr(ccAdres.trim()).toLowerCase(Locale.ENGLISH);
		List<String> adresler = null;
		if (adresTipi != null) {
			if (adresTipi.equals(MAIL_CC)) {
				if (ccAdresList == null)
					ccAdresList = new ArrayList<String>();
				adresler = ccAdresList;
			}

			else if (adresTipi.equals(MAIL_BCC)) {
				if (bccAdresList == null)
					bccAdresList = new ArrayList<String>();
				adresler = bccAdresList;
			} else if (adresTipi.equals(MAIL_HAREKET)) {
				if (hareketAdresList == null)
					hareketAdresList = new ArrayList<String>();
				adresler = hareketAdresList;
			}
			try {
				if (eMail.indexOf("@") < 1)
					throw new Exception(eMail);
				InternetAddress internetAddress = new InternetAddress(eMail);
				eMail = internetAddress.getAddress();
				List<User> userList = ortakIslemler.getMailUser(eMail, false);
				if (!userList.isEmpty()) {
					if (userList.size() == 1) {
						User user = userList.get(0);
						if (user.getYetkiliPersonelNoList().size() > 1 && user.getShortUsername() != null) {
							List<User> groups = ortakIslemler.getMailUser(eMail, true);
							PdksUtil.addMessageError(user.getShortUsername() + " Grubu");
							StringBuffer sb = new StringBuffer("");
							int sira = 0;
							for (User user2 : groups)
								sb.append((sira++ > 0 ? ", " : "") + user2.getFullName() + " - " + user2.getEmail());
							PdksUtil.addMessageAvailableInfo(sb.toString());
						}
					}
				}

				if (adresler.contains(eMail))
					PdksUtil.addMessageError(ccAdres + " listede var!");
				else {
					adresler.add(eMail);
					ccAdres = "";
				}
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
				PdksUtil.addMessageError(ccAdres + " hatalı adres!");
			}
		}

		return "";

	}

	/**
	 * @param adres
	 * @param xAdresTipi
	 * @return
	 */
	public String adresAyarla(String adres, String xAdresTipi) {
		ccAdres = "";
		setAdresTipi(xAdresTipi);
		if (!PdksUtil.hasStringValue(adres)) {
			if (xAdresTipi.equals(MAIL_CC))
				adres = getInstance().getEmailCC();
			else if (xAdresTipi.equals(MAIL_BCC))
				adres = getInstance().getEmailBCC();
			else if (xAdresTipi.equals(MAIL_HAREKET))
				adres = getInstance().getHareketMail();
		}
		List<String> adresler = new ArrayList<String>();
		if (adres != null && adres.indexOf("@") > 0) {
			String separator = PdksUtil.SEPARATOR_MAIL;
			if (adres.indexOf(separator) < 0 && adres.indexOf(",") > 0)
				separator = ",";
			if (adres.indexOf(separator) > 0)
				adresler.addAll(Arrays.asList(adres.split(separator)));
			else
				adresler.add(adres);
		}
		if (xAdresTipi.equals(MAIL_CC))
			ccAdresList = adresler;
		else if (xAdresTipi.equals(MAIL_BCC))
			bccAdresList = adresler;
		else if (xAdresTipi.equals(MAIL_HAREKET))
			hareketAdresList = adresler;
		return "";

	}

	/**
	 * 
	 */
	public void fillPdksCalismaModeliList() {
		try {
			Personel pdksPersonel = getInstance();
			if (calismaModeliList != null)
				calismaModeliList.clear();
			else
				calismaModeliList = new ArrayList<CalismaModeli>();
			if (pdksPersonel.getSirket() != null) {
				fillCalismaModeli(pdksPersonel);
				if (calismaModeliList.size() == 1) {
					pdksPersonel.setCalismaModeli(calismaModeliList.get(0));
					modelDegisti();
				}

			} else if (pdksPersonel.getId() == null) {
				pdksPersonel.setSablon(null);
				pdksPersonel.setCalismaModeli(null);
				modelDegisti();
			}

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			logger.error("fillPdksCalismaModeliList Hata : " + e.getMessage());
		}

	}

	/**
	 * @param pdksPersonel
	 */
	private void fillCalismaModeli(Personel pdksPersonel) {
		Long pdksDepartmanId = pdksPersonel.getSirket() != null ? pdksPersonel.getSirket().getDepartman().getId() : null;
		calismaModeliList = ortakIslemler.getCalismaModeliList(pdksPersonel.getSirket(), pdksDepartmanId, true, session);
		// calismaModeliList = pdksEntityController.getSQLParamByFieldList(CalismaModeli.TABLE_NAME, CalismaModeli.COLUMN_NAME_DURUM, 1, CalismaModeli.class, session);
		for (Iterator iterator = calismaModeliList.iterator(); iterator.hasNext();) {
			CalismaModeli cm = (CalismaModeli) iterator.next();
			Departman departman = cm.getSirket() != null ? cm.getSirket().getDepartman() : cm.getDepartman();
			if (departman != null && pdksDepartmanId != null && !pdksDepartmanId.equals(departman.getId())) {
				iterator.remove();
				continue;
			}
			if (pdksPersonel.getGebeMi().equals(Boolean.FALSE) && pdksPersonel.getSutIzni().equals(Boolean.FALSE)) {
				if (cm.getToplamGunGuncelle().equals(Boolean.TRUE)) {
					iterator.remove();
					continue;
				}

			}
			if (pdksPersonel.getCalismaModeli() == null && cm.isIdariModelMi())
				pdksPersonel.setCalismaModeli(cm);
		}
		calismaModeliList = PdksUtil.sortObjectStringAlanList(calismaModeliList, "getAciklama", null);
		if (pdksPersonel.isCalisiyor() == false && pdksPersonel.getCalismaModeli() != null && pdksPersonel.getCalismaModeli().getDurum().equals(Boolean.FALSE))
			calismaModeliList.add(pdksPersonel.getCalismaModeli());
	}

	/**
	 * 
	 */
	private void fillCalismaModeliVardiyaList() {
		Personel pdksPersonel = getInstance();
		if (pdksPersonel.getCalismaModeli() != null)
			calismaModeliVardiyaList = ortakIslemler.fillCalismaModeliVardiyaList(pdksPersonel.getCalismaModeli(), session);
		if (calismaModeliVardiyaList != null && calismaModeliVardiyaList.isEmpty())
			calismaModeliVardiyaList = null;
	}

	/**
	 * 
	 */
	public void fillPdksVardiyaSablonList() {
		List<VardiyaSablonu> sablonList = null;
		try {
			Personel pdksPersonel = getInstance();
			Departman pdksDepartman = pdksPersonel.getSirket() != null ? pdksPersonel.getSirket().getDepartman() : null;
			sablonlar.clear();

			Long departmanId = pdksDepartman != null ? pdksDepartman.getId() : null;
			sablonList = ortakIslemler.getVardiyaSablonuList(pdksPersonel.getSirket(), departmanId, session);
			for (Iterator iterator = sablonList.iterator(); iterator.hasNext();) {
				VardiyaSablonu vardiyaSablonu = (VardiyaSablonu) iterator.next();
				Departman departman = vardiyaSablonu.getSirket() != null ? vardiyaSablonu.getSirket().getDepartman() : vardiyaSablonu.getDepartman();
				if (departman != null && pdksDepartman != null && !pdksDepartman.getId().equals(departman.getId()))
					iterator.remove();

			}
			if (!sablonList.isEmpty()) {
				sablonlar.addAll(sablonList);
				boolean degistir = pdksPersonel.getId() == null;
				if (!degistir) {
					VardiyaSablonu sablon = pdksPersonel.getSablon();
					degistir = true;
					for (VardiyaSablonu vardiyaSablonu : sablonList) {
						if (sablon.getId().equals(vardiyaSablonu.getId()))
							degistir = false;
					}
				}
				if (degistir) {
					VardiyaSablonu sablon = null;
					if (sablonlar.size() == 1) {
						sablon = sablonlar.get(0);
					} else if (pdksPersonel.getSablon() == null) {
						for (VardiyaSablonu vardiyaSablonu : sablonList) {
							if (vardiyaSablonu.getBeyazYakaDefault() != null && vardiyaSablonu.getBeyazYakaDefault())
								sablon = vardiyaSablonu;

						}
					}
					if (sablon != null) {
						pdksPersonel.setSablon(sablon);
						sablonDegisti();
					}
				}

			}

			if (pdksPersonel.isCalisiyor() == false && pdksPersonel.getSablon() != null && pdksPersonel.getSablon().getDurum().equals(Boolean.FALSE))
				sablonList.add(pdksPersonel.getSablon());

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			logger.error("fillPdksVardiyaSablonList Hata : " + e.getMessage());
		}
		sablonList = null;

	}

	/**
	 * @param pdksPersonel
	 */
	private void savePersonel(Personel pdksPersonel) {
		if (pdksPersonel.getId() != null) {
			pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);
			session.flush();
		}
	}

	/**
	 * 
	 */
	public void ldapKontrol() {
		setHataMesaj("");
		Personel pdksPersonel = getInstance();
		User user = ortakIslemler.digerKullanici(pdksPersonel.getKullanici(), oldUserName, session);
		if (user != null) {
			setHataMesaj(user.getUsername() + " " + user.getAdSoyad() + " ait kullanÄ±cÄ±dÄ±r");
			// pdksUtil.mesajYaz(user.getUsername() + " " + user.getAdSoyad() +
			// " ait kullanÄ±cÄ±dÄ±r");
			pdksPersonel.getKullanici().setUsername(oldUserName);

		} else if (pdksPersonel.getKullanici() != null && pdksPersonel.getSirket() != null && pdksPersonel.getSirket().isLdap()) {
			user = pdksPersonel.getKullanici();
			user.setPdksPersonel(pdksPersonel);
			String email = user.getEmailFromUserName();
			if (email != null)
				user.setEmail(email);

		}

	}

	/**
	 * @return
	 */
	@Transactional
	public String detaysizSave() {

		Personel pdksPersonel = getInstance();
		String ok = "";
		if (pdksPersonel.getId() != null) {
			pdksPersonel.setGuncelleyenUser(authenticatedUser);
			pdksPersonel.setGuncellemeTarihi(new Date());
		} else {
			if (PdksUtil.hasStringValue(pdksPersonel.getAd()))
				pdksPersonel.setAd(pdksPersonel.getAd().toUpperCase(Constants.TR_LOCALE));
			if (PdksUtil.hasStringValue(pdksPersonel.getSoyad()))
				pdksPersonel.setSoyad(pdksPersonel.getSoyad().toUpperCase(Constants.TR_LOCALE));
			pdksPersonel.setOlusturanUser(authenticatedUser);
		}

		pdksPersonel.setPdksSicilNo(pdksPersonel.getPersonelKGS().getSicilNo());
		session.save(pdksPersonel);
		session.flush();
		session.refresh(personelView);
		return ok;

	}

	/**
	 * 
	 */
	public void istenAyrilmaTarihiUzat() {
		getInstance().setIstenAyrilisTarihi(PdksUtil.getSonSistemTarih());
	}

	/**
	 * @return
	 */
	@Transactional
	public String saveKullanici() {
		Personel pdksPersonel = getInstance();
		User kullaniciYeni = pdksPersonel.getKullanici();
		if ((kullaniciYeni == null || kullaniciYeni.getId() == null) && eskiKullanici != null && pdksPersonel.getId() != null) {
			eskiKullanici.setPdksPersonel(pdksPersonel);
			eskiKullanici.setDurum(pdksPersonel.isCalisiyor());
			pdksEntityController.saveOrUpdate(session, entityManager, eskiKullanici);
			if (eskiKullanici.isDurum()) {
				pdksPersonel.setGuncellemeTarihi(new Date());
				pdksPersonel.setGuncelleyenUser(authenticatedUser);
				pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);
			}
			session.flush();
			fillPersonelKGSList();
		}
		return "";
	}

	/**
	 * @param personelView
	 * @return
	 */
	@Transactional
	public String personelDegistir(PersonelView personelView) {
		if (personelView != null) {
			try {
				Personel personel = personelView.getPdksPersonel();
				if (personel != null && personelKGSMap.containsKey(personel.getPersonelKGS().getId())) {
					PersonelKGS personelKGS = personelKGSMap.get(personel.getPersonelKGS().getId());
					personel.setPersonelKGS(personelKGS);
					if (!authenticatedUser.isAdmin()) {
						personel.setGuncellemeTarihi(new Date());
						personel.setGuncelleyenUser(authenticatedUser);
					}
					pdksEntityController.saveOrUpdate(session, entityManager, personel);
					session.flush();
					fillPersonelKGSList();
				}
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String save() {
		hataMesaj = "";
		Personel pdksPersonel = getInstance();
		Sirket sirket = pdksPersonel.getSirket();
		boolean izinERPUpdate = ortakIslemler.getParameterKeyHasStringValue("bakiyeIzinGoster");
		if (pdksPersonel.getSanalPersonel() != null && pdksPersonel.getSanalPersonel() && pdksPersonel.getIseBaslamaTarihi() != null) {
			if (pdksPersonel.getIzinHakEdisTarihi() == null)
				pdksPersonel.setIzinHakEdisTarihi(pdksPersonel.getIseBaslamaTarihi());
			if (pdksPersonel.getGrubaGirisTarihi() == null)
				pdksPersonel.setGrubaGirisTarihi(pdksPersonel.getIseBaslamaTarihi());
		}
		if (pdksPersonel.getId() == null && sirket != null) {
			if (sirket.isErp() == false) {
				if (pdksPersonel.getIseBaslamaTarihi() == null)
					pdksPersonel.setIseBaslamaTarihi(pdksPersonel.getGrubaGirisTarihi() == null ? pdksPersonel.getIzinHakEdisTarihi() : pdksPersonel.getGrubaGirisTarihi());
				if (pdksPersonel.getGrubaGirisTarihi() == null)
					pdksPersonel.setGrubaGirisTarihi(pdksPersonel.getIzinHakEdisTarihi());
			} else {
				if (sirket.isPdksMi() && pdksPersonel.getTesis() == null && tesisList != null && tesisList.size() == 1)
					pdksPersonel.setTesis(tesisList.get(0));
			}
		}

		ArrayList<String> mesajList = new ArrayList<String>();
		if (authenticatedUser.isAdmin() && pdksPersonel.getGrubaGirisTarihi() != null && PdksUtil.tarihKarsilastirNumeric(pdksPersonel.getGrubaGirisTarihi(), pdksPersonel.getIseBaslamaTarihi()) == 1)
			mesajList.add("İşe Giriş Tarihi Grubu Giriş Tarihinden önce olamaz");
		if (PdksUtil.tarihKarsilastirNumeric(pdksPersonel.getIseBaslamaTarihi(), pdksPersonel.getSonCalismaTarihi()) == 1)
			mesajList.add("İşe Giriş Tarihi İşten Ayrılma Tarihinden önce olamaz");
		if (izinGirisiVar && PdksUtil.tarihKarsilastirNumeric(pdksPersonel.getDogumTarihi(), pdksPersonel.getIzinHakEdisTarihi()) == 1)
			mesajList.add("Doğum Tarihi " + ortakIslemler.kidemBasTarihiAciklama() + "den önce olamaz");
		StringBuffer kullaniciRolMesaj = pdksPersonel.getKullanici() != null ? kullaniciRolKontrolMesaji(pdksPersonel.getKullanici(), true) : null;
		if (kullaniciRolMesaj != null)
			mesajList.add(kullaniciRolMesaj.toString());
		kullaniciRolMesaj = null;

		User kullanici = pdksPersonel.getKullanici();
		if (kullanici != null)
			PdksUtil.setUserYetki(kullanici);
		List<Role> yeniRoller = kullanici != null && kullanici.getYetkiliRollerim() != null ? new ArrayList<Role>(kullanici.getYetkiliRollerim()) : new ArrayList<Role>();
		List<Tanim> yeniTesisler = new ArrayList<Tanim>(), yeniBolumler = new ArrayList<Tanim>();
		if (kullanici != null && bolumYetki && (kullanici.isIK() || kullanici.isDirektorSuperVisor()) == false && kullanici.getYetkiliBolumler() != null)
			yeniBolumler.addAll(kullanici.getYetkiliBolumler());
		User user = null;
		String ok = "";

		try {
			boolean yeni = pdksPersonel.getId() == null;
			boolean kullaniciYaz = PdksUtil.hasStringValue(kullanici.getUsername()) && PdksUtil.hasStringValue(kullanici.getEmail());
			if (kullaniciYaz) {
				if (PdksUtil.isValidEMail(kullanici.getEmail()))
					user = ortakIslemler.digerKullanici(kullanici, getOldUserName(), session);
				else
					mesajList.add("Geçersiz e-posta adresi --> " + kullanici.getEmail());
			} else if (kullanici.getYetkiliRollerim() != null && kullanici.getYetkiliRollerim().isEmpty()) {
				mesajList.add("Kullanıcı bilgilerini girmeden önce role kayıt olamaz!");
			}

			if (user == null) {
				if (!yeni) {
					pdksPersonel.setGuncelleyenUser(authenticatedUser);
					pdksPersonel.setGuncellemeTarihi(new Date());
				} else {
					if (pdksPersonel.getIseBaslamaTarihi() == null)
						pdksPersonel.setIseBaslamaTarihi(pdksPersonel.getIzinHakEdisTarihi());
					if (pdksPersonel.getGrubaGirisTarihi() == null)
						pdksPersonel.setGrubaGirisTarihi(pdksPersonel.getIzinHakEdisTarihi());
					pdksPersonel.setOlusturanUser(authenticatedUser);
					if (sirket != null && !sirket.isErp()) {
						if (PdksUtil.hasStringValue(pdksPersonel.getAd()))
							pdksPersonel.setAd(pdksPersonel.getAd().toUpperCase(Constants.TR_LOCALE));
						if (PdksUtil.hasStringValue(pdksPersonel.getSoyad()))
							pdksPersonel.setSoyad(pdksPersonel.getSoyad().toUpperCase(Constants.TR_LOCALE));
					}
				}

				if (pdksPersonel.getId() == null || authenticatedUser.isAdmin())
					pdksPersonel.setPdksSicilNo(pdksPersonel.getPersonelKGS().getSicilNo());
				boolean personelERPGuncellemeDurum = personelERPGuncelleme != null && personelERPGuncelleme.equalsIgnoreCase("M");
				if (asilYonetici1 != null && sirket != null) {
					if (!sirket.isErp()) {
						pdksPersonel.setAsilYonetici1(asilYonetici1);
					} else {
						if (asilYonetici1 == null && pdksPersonel.getAsilYonetici1() != null)
							asilYonetici1 = pdksPersonel.getAsilYonetici1();
					}
					if (personelERPGuncellemeDurum)
						pdksPersonel.setAsilYonetici1(asilYonetici1);
					pdksPersonel.setYoneticisi(asilYonetici1);

				} else if (pdksPersonel.getAsilYonetici1() != null) {
					pdksPersonel.setYoneticisi(pdksPersonel.getAsilYonetici1());
					// pdksPersonel.setAsilYonetici1(null);
				} else if (sirket != null && !sirket.isErp()) {
					if (pdksPersonel.getYoneticisi() != null && pdksPersonel.getYoneticisi().getSirket().isErp())
						pdksPersonel.setAsilYonetici2(pdksPersonel.getYoneticisi());
					else
						pdksPersonel.setAsilYonetici2(null);
				}

				if (pdksPersonel.getSonCalismaTarihi() == null)
					pdksPersonel.setIstenAyrilisTarihi(PdksUtil.getSonSistemTarih());
				try {
					if (pdksPersonel.getIcapciOlabilir() && !sirket.getDepartman().getIcapciOlabilir())
						pdksPersonel.setIcapciOlabilir(Boolean.FALSE);
					if (kullaniciYaz) {
						ortakIslemler.setUserRoller(kullanici, session);
						if (kullanici.isProjeMuduru() && pdksPersonel.getYoneticisi() == null) {
							pdksPersonel.setYoneticisi(pdksPersonel);
							pdksPersonel.setAsilYonetici1(pdksPersonel);
						}
					}
					if (!pdksPersonel.getCinsiyetBayan()) {
						pdksPersonel.setGebeMi(Boolean.FALSE);
						pdksPersonel.setSutIzni(Boolean.FALSE);
					}
					if (pdksPersonel.getEmailCC() != null) {
						String emailCC = ortakIslemler.getPersonelCCMail(pdksPersonel);
						if (emailCC != null)
							pdksPersonel.setEmailCC(emailCC);
					}
					if (pdksPersonel.getEmailBCC() != null) {
						String emailBCC = ortakIslemler.getPersonelBCCMail(pdksPersonel);
						if (emailBCC != null)
							pdksPersonel.setEmailBCC(emailBCC);
					}
					if (pdksPersonel.getHareketMail() != null) {
						String hareketMail = ortakIslemler.getPersonelHareketMail(pdksPersonel);
						if (hareketMail != null)
							pdksPersonel.setHareketMail(hareketMail);
					}
					if (mesajList.isEmpty()) {

						ortakIslemler.personelKaydet(pdksPersonel, session);
						if (secGebe(pdksPersonel).booleanValue() == false)
							pdksPersonel.setGebeMi(Boolean.FALSE);
						if (secSutIzni(pdksPersonel).booleanValue() == false)
							pdksPersonel.setSutIzni(Boolean.FALSE);
						if (secSua(pdksPersonel).booleanValue() == false)
							pdksPersonel.setSuaOlabilir(Boolean.FALSE);
						if (secIcap(pdksPersonel).booleanValue() == false)
							pdksPersonel.setIcapciOlabilir(Boolean.FALSE);
						if (secFMI(pdksPersonel).booleanValue() == false)
							pdksPersonel.setFazlaMesaiIzinKullan(Boolean.FALSE);
						if (pdksPersonel.getFazlaMesaiIzinKullan())
							pdksPersonel.setFazlaMesaiOde(Boolean.FALSE);
						if (pdksPersonel.getCalismaModeli() != null && pdksPersonel.getCalismaModeli().isFazlaMesaiVarMi() == false) {
							pdksPersonel.setFazlaMesaiOde(false);
							pdksPersonel.setFazlaMesaiIzinKullan(false);
						}
						pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);
						if (mesajList.isEmpty()) {
							try {
								for (PersonelDinamikAlan pda : dinamikPersonelDurumList) {
									if (pda.getId() != null || pda.isDurumSecili()) {
										pda.setPersonel(pdksPersonel);
										pdksEntityController.saveOrUpdate(session, entityManager, pda);
									}
								}
								for (PersonelDinamikAlan pda : dinamikPersonelTanimList) {
									if (pda.getId() != null || pda.getTanimDeger() != null) {
										pda.setPersonel(pdksPersonel);
										pdksEntityController.saveOrUpdate(session, entityManager, pda);
									}
								}
								for (PersonelDinamikAlan pda : dinamikPersonelSayisalList) {
									if (pda.getId() != null || pda.getSayisalDeger() != null) {
										pda.setPersonel(pdksPersonel);
										pdksEntityController.saveOrUpdate(session, entityManager, pda);
									}
								}
							} catch (Exception epda) {
							}

						}

					}
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());

				}

				try {

					if (mesajList.isEmpty() && kullaniciYaz) {
						if (pdksPersonel.getGrubaGirisTarihi() == null || !sirket.getDepartman().isAdminMi())
							pdksPersonel.setGrubaGirisTarihi(pdksPersonel.getIseBaslamaTarihi());
						kullanici.setPdksPersonel(pdksPersonel);
						if (kullanici.isYeniSifre() || kullanici.getId() == null || getOldSirket().isLdap() != sirket.isLdap()) {
							if (!sirket.isLdap() || kullanici.isYeniSifre())
								kullaniciSifreOlustur(kullanici);
							else
								kullanici.setPasswordHash("");
						}
						ortakIslemler.setUserRoller(kullanici, session);
						if (kullanici.getYetkiliRollerim() != null && kullanici.getYetkiliRollerim().isEmpty())
							kullanici = ortakIslemler.personelPdksRolAta(kullanici, Boolean.FALSE, session);
						pdksEntityController.saveOrUpdate(session, entityManager, kullanici);
						HashMap<Long, UserRoles> roller = new HashMap<Long, UserRoles>();

						List<UserRoles> yetkiliRoller = pdksEntityController.getSQLParamByFieldList(UserRoles.TABLE_NAME, UserRoles.COLUMN_NAME_USER, kullanici.getId(), UserRoles.class, session);
						if (yetkiliRoller != null) {
							for (Iterator iterator = yetkiliRoller.iterator(); iterator.hasNext();) {
								UserRoles userRoles = (UserRoles) iterator.next();
								boolean ekle = authenticatedUser.isAdmin() || (userRoles.getRole() != null && userRoles.getRole().isAdminRoleMu() == false);
								if (ekle) {
									if (authenticatedUser.isIK_Tesis() || authenticatedUser.isIKSirket())
										ekle = userRoles.getRole().isIK() == false;
								}
								if (ekle)
									roller.put(userRoles.getRole().getId(), userRoles);
							}
						}

						List<UserDigerOrganizasyon> yetkiliUserDigerOrganizasyonlar = pdksEntityController.getSQLParamByFieldList(UserDigerOrganizasyon.TABLE_NAME, UserDigerOrganizasyon.COLUMN_NAME_USER, kullanici.getId(), UserDigerOrganizasyon.class, session);
						HashMap<Long, UserDigerOrganizasyon> tesisler = new HashMap<Long, UserDigerOrganizasyon>(), bolumler = new HashMap<Long, UserDigerOrganizasyon>();

						for (UserDigerOrganizasyon userDigerOrganizasyon : yetkiliUserDigerOrganizasyonlar) {
							if (userDigerOrganizasyon.isTesis())
								tesisler.put(userDigerOrganizasyon.getOrganizasyon().getId(), userDigerOrganizasyon);
							else if (userDigerOrganizasyon.isBolum())
								bolumler.put(userDigerOrganizasyon.getOrganizasyon().getId(), userDigerOrganizasyon);
						}
						if (pdksPersonel.getTesis() != null && !tesisler.containsKey(pdksPersonel.getTesis().getId()))
							tesisler.put(pdksPersonel.getTesis().getId(), new UserDigerOrganizasyon(kullanici, OrganizasyonTipi.TESIS, pdksPersonel.getTesis()));
						if (yeniRoller != null) {
							for (Iterator iterator = yeniRoller.iterator(); iterator.hasNext();) {
								Role role = (Role) iterator.next();
								if (roller.containsKey(role.getId())) {
									iterator.remove();
									roller.remove(role.getId());
								} else {
									UserRoles userRoles = new UserRoles();
									userRoles.setRole(role);
									userRoles.setUser(kullanici);
									pdksEntityController.saveOrUpdate(session, entityManager, userRoles);
								}

							}
							yeniRoller = null;
						}
						if (yeniTesisler != null) {
							if (kullanici.getYetkiliTesisler() != null && kullanici.getYetkiliTesisler().isEmpty() == false)
								yeniTesisler.addAll(kullanici.getYetkiliTesisler());
							if (tesisGoster() == false)
								yeniTesisler.clear();
							for (Iterator iterator = yeniTesisler.iterator(); iterator.hasNext();) {
								Tanim tesis = (Tanim) iterator.next();
								if (tesisler.containsKey(tesis.getId())) {
									iterator.remove();
									tesisler.remove(tesis.getId());
								} else {
									UserDigerOrganizasyon userTesis = new UserDigerOrganizasyon(kullanici, OrganizasyonTipi.TESIS, tesis);
									pdksEntityController.saveOrUpdate(session, entityManager, userTesis);
								}

							}

							yeniTesisler = null;
						}
						if (yeniBolumler != null) {
							for (Iterator iterator = yeniBolumler.iterator(); iterator.hasNext();) {
								Tanim bolum = (Tanim) iterator.next();
								if (bolumler.containsKey(bolum.getId())) {
									iterator.remove();
									bolumler.remove(bolum.getId());
								} else {
									UserDigerOrganizasyon userBolum = new UserDigerOrganizasyon(kullanici, OrganizasyonTipi.BOLUM, bolum);
									pdksEntityController.saveOrUpdate(session, entityManager, userBolum);
								}

							}
							yeniBolumler = null;
						}
						if (tesisler != null && !tesisler.isEmpty()) {
							List<UserDigerOrganizasyon> list = new ArrayList<UserDigerOrganizasyon>(tesisler.values());
							for (Iterator iterator = list.iterator(); iterator.hasNext();) {
								UserDigerOrganizasyon userTesis = (UserDigerOrganizasyon) iterator.next();
								pdksEntityController.deleteObject(session, entityManager, userTesis);

							}
						}
						if (bolumler != null && !bolumler.isEmpty()) {
							List<UserDigerOrganizasyon> list = new ArrayList<UserDigerOrganizasyon>(bolumler.values());
							for (Iterator iterator = list.iterator(); iterator.hasNext();) {
								UserDigerOrganizasyon userTesis = (UserDigerOrganizasyon) iterator.next();
								pdksEntityController.deleteObject(session, entityManager, userTesis);

							}
						}
						if (roller != null && !roller.isEmpty()) {
							yetkiliRoller = new ArrayList<UserRoles>(roller.values());
							for (Iterator iterator = yetkiliRoller.iterator(); iterator.hasNext();) {
								UserRoles userRoles = (UserRoles) iterator.next();
								pdksEntityController.deleteObject(session, entityManager, userRoles);

							}
						}
						tesisler = null;
						roller = null;

					}
					if (mesajList.isEmpty()) {
						if (izinERPUpdate) {
							if (bakiyeIzin != null) {
								boolean izinGuncelle = false;
								if (bakiyeIzin.getId() != null) {
									if (bakiyeIzinSuresi.doubleValue() != bakiyeIzin.getIzinSuresi().doubleValue()) {
										List<PersonelIzinDetay> list = pdksEntityController.getSQLParamByFieldList(PersonelIzinDetay.TABLE_NAME, PersonelIzinDetay.COLUMN_NAME_HAKEDIS_IZIN, bakiyeIzin.getId(), PersonelIzinDetay.class, session);
										for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
											PersonelIzinDetay personelIzinDetay = (PersonelIzinDetay) iterator2.next();
											PersonelIzin personelIzin = personelIzinDetay.getPersonelIzin();
											if (personelIzin.getIzinDurumu() == PersonelIzin.IZIN_DURUMU_REDEDILDI || personelIzin.getIzinDurumu() == PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL)
												iterator2.remove();

										}
										bakiyeIzin.setIzinDurumu(!list.isEmpty() || bakiyeIzinSuresi != 0.0d ? PersonelIzin.IZIN_DURUMU_ONAYLANDI : PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL);
										bakiyeIzin.setIzinSuresi(bakiyeIzinSuresi);
										bakiyeIzin.setGuncelleyenUser(authenticatedUser);
										bakiyeIzin.setGuncellemeTarihi(new Date());
										izinGuncelle = true;
									}

								} else if (bakiyeIzinSuresi != null && bakiyeIzinSuresi.doubleValue() != 0.0d) {
									Calendar cal = Calendar.getInstance();
									cal.setTime(bakiyeIzin.getBaslangicZamani());
									int yil = cal.get(Calendar.YEAR);
									cal.setTime(pdksPersonel.getIzinHakEdisTarihi());
									cal.set(Calendar.YEAR, yil);
									Date bitisZamani = cal.getTime();
									bakiyeIzin.setBitisZamani(bitisZamani);
									bakiyeIzin.setAciklama("Devir Bakiye");
									bakiyeIzin.setIzinSuresi(bakiyeIzinSuresi);
									bakiyeIzin.setOlusturanUser(authenticatedUser);
									bakiyeIzin.setOlusturmaTarihi(new Date());
									bakiyeIzin.setIzinDurumu(PersonelIzin.IZIN_DURUMU_ONAYLANDI);
									bakiyeIzin.setHesapTipi(PersonelIzin.HESAP_TIPI_GUN);
									izinGuncelle = true;

								}
								if (izinGuncelle)
									pdksEntityController.saveOrUpdate(session, entityManager, bakiyeIzin);

							}
						}

						saveIkinciYoneticiOlmazList();

						session.flush();
						if (tesisYetki && kullanici.getId() != null && authenticatedUser.getId() != null) {
							authenticatedUser.setYetkiliTesisler(null);
							ortakIslemler.setUserTesisler(authenticatedUser, session);
						}
						try {
							session.refresh(personelView);
						} catch (Exception e) {
							logger.error("PDKS hata in : \n");
							e.printStackTrace();
							logger.error("PDKS hata out : " + e.getMessage());
						}
						// if (ms == null || ms.getDurum())
						fillPersonelKGSList();

					}

					ok = "persisted";
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());

					PdksUtil.addMessageError(e.getMessage());
				}

			} else
				mesajList.add(user.getUsername() + " " + user.getPdksPersonel().getPdksSicilNo() + " sicil numaralı kullanıcısı " + user.getAdSoyad() + " personel'de tanımlıdır!");
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			mesajList.add("Hata : " + e.getMessage());
		}
		if (!mesajList.isEmpty()) {
			ok = "";
			hataMesaj = "";
			for (String mesaj : mesajList) {
				hataMesaj += mesaj + "\n";
				PdksUtil.addMessageWarn(mesaj);
			}

		}
		return ok;

	}

	/**
	 * @param kullanici
	 */
	private void kullaniciSifreOlustur(User kullanici) {
		String encodePassword = DefaultPasswordGenerator.generate(8);
		if (kullanici.getId() == null)
			encodePassword = "123456";
		kullanici.setNewPassword(encodePassword);
		setUser(kullanici);
		MailStatu ms = null;
		try {
			MailObject mailObject = new MailObject();
			mailObject.setSubject("Şifre Gönderme");
			String body = "<p>Sayın " + kullanici.getMailPersonel().getAdiSoyadi() + ",</p><p>Kullanıcı şifreniz aşağıda belirtildiği şekilde " + (kullanici.getId() == null ? "tanımlanmıştır" : "değiştirilmiştir.") + " İlk kullanımda değiştirmenizi öneririz.</p>";
			body += "<p>Kullanıcı Adı : " + kullanici.getUsername() + " --->   Şifre   : " + encodePassword + "</p>";
			body += "<p>Saygılarımla,</p>";
			Map<String, String> map = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
			String donusAdres = map.containsKey("host") ? map.get("host") : "";
			if (PdksUtil.hasStringValue(donusAdres))
				body += "<p><a href=\"http://" + donusAdres + "/sifreDegistirme\">" + ortakIslemler.getParameterKey("fromName") + " uygulamasına girmek için buraya tıklayınız.</a></p>";

			mailObject.setBody(body);
			mailObject.getToList().add(kullanici.getMailPersonel());
			boolean durum = false;
			String mesaj = "";

			try {
				ms = ortakIslemler.mailSoapServisGonder(false, mailObject, renderer, "/mail.xhtml", session);
				if (ms != null) {
					durum = ms.getDurum();
					if (!durum)
						mesaj = ms.getHataMesai();
					if (mesaj == null)
						mesaj = "Hata oluştu!";
				}
			} catch (Exception e) {

			}

			if (durum)
				PdksUtil.addMessageInfo("Mesaj Gönderildi.");
			else {
				if (ms == null || !ms.getDurum().booleanValue())
					mesaj += " ---> " + (kullanici.getId() != null ? "Yeni " : "") + " Şifre : " + encodePassword;
				PdksUtil.addMessageWarn("Mesaj Gönderilemedi." + mesaj);
			}

		} catch (Exception e) {
			logger.info(kullanici.getUsername() + " --> " + encodePassword);
		}

		kullanici.setPasswordHash(PdksUtil.encodePassword(encodePassword));

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String erpVeriGuncelle() throws Exception {
		Personel personel = getInstance();
		if (personelERPGuncelleme != null && personelERPGuncelleme.equalsIgnoreCase("E"))
			erpVeriGuncelle(null, personel, Boolean.FALSE);
		else if (ortakIslemler.getParameterKeyHasStringValue(ortakIslemler.getParametrePersonelERPTableView())) {
			List<String> perNoList = new ArrayList<String>();
			perNoList.add(personel.getPdksSicilNo());
			List<PersonelERP> updateList = ortakIslemler.personelERPDBGuncelle(false, perNoList, session);
			if (updateList != null && updateList.isEmpty()) {
				Personel personel2 = (Personel) pdksEntityController.getSQLParamByFieldObject(Personel.TABLE_NAME, Personel.COLUMN_NAME_PDKS_SICIL_NO, personel.getPdksSicilNo(), Personel.class, session);

				if (personel2 != null)
					setInstance(personel2);
			}
		}

		return "";
	}

	/**
	 * @param personelView
	 * @throws Exception
	 */
	public void kayitERPGuncelle(PersonelView personelView) throws Exception {
		Personel personel = personelView.getPdksPersonel();
		if (personelERPGuncelleme != null && personelERPGuncelleme.equalsIgnoreCase("E"))
			erpVeriGuncelle(personelView, personel, Boolean.TRUE);
		else if (ortakIslemler.getParameterKeyHasStringValue(ortakIslemler.getParametrePersonelERPTableView())) {
			String sicilNo = personel != null && personel.getId() != null ? personel.getPdksSicilNo() : personelView.getSicilNo();
			if (sicilNo != null && PdksUtil.hasStringValue(sicilNo)) {
				List<String> list = new ArrayList<String>();
				list.add(sicilNo);
				List<PersonelERP> updateList = ortakIslemler.personelERPDBGuncelle(false, list, session);
				if (updateList != null) {
					if (updateList.isEmpty()) {
						fillPersonelKGSList();
						PdksUtil.addMessageInfo(sicilNo + " " + personelView.getAdSoyad() + " güncellendi");
					} else if (updateList.size() == 1) {
						PersonelERP personelERP = updateList.get(0);
						if (!personelERP.getHataList().isEmpty()) {
							for (String mesaj : personelERP.getHataList()) {
								PdksUtil.addMessageAvailableWarn(sicilNo + " " + personelView.getAdSoyad() + " --> " + mesaj);

							}
						}
					}
				}
			}
		}
	}

	/**
	 * @param personelView
	 * @param personel
	 * @param update
	 */
	private void erpVeriGuncelle(PersonelView personelView, Personel personel, Boolean update) {

		if (personel != null && personel.getSirket().isErp()) {
			try {
				boolean guncellendi = ortakIslemler.sapVeriGuncelle(session, authenticatedUser, null, null, personel, null, update, Boolean.FALSE, Boolean.TRUE);
				if (guncellendi) {
					if (update) {
						session.flush();

						PdksUtil.addMessageInfo(personel.getAdSoyad() + " SAP'den verileri güncellendi.");
						if (personelView != null)
							session.refresh(personelView);
					}
				} else
					PdksUtil.addMessageWarn(personel.getAdSoyad() + " SAP'den verileri güncellenmedi!");

			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				PdksUtil.addMessageError(personel.getAdSoyad() + " SAP'den verileri güncellenemedi!" + e.getMessage());
			}
		}
	}

	/**
	 * @return
	 */
	public String denemeMesajHazirla() {
		Personel personel = getInstance();
		if (ePosta == null || ePosta.indexOf("@") < 2)
			ePosta = personel.getKullanici().getEmail();
		return "";

	}

	/**
	 * @return
	 */
	public String denemeMesajGonder() {
		try {
			Personel personel = getInstance();
			if (denemeMesaj.indexOf("\t") > 0)
				denemeMesaj = PdksUtil.replaceAll(denemeMesaj, "\t", "");
			if (denemeMesaj.indexOf("\n") > 0)
				denemeMesaj = PdksUtil.replaceAll(denemeMesaj, "\n", "<br/>");
			MailStatu mailStatu = null;
			if (PdksUtil.isValidEMail(ePosta)) {
				Notice uyariNot = ortakIslemler.getNotice(NoteTipi.MAIL_CEVAPLAMAMA.value(), Boolean.TRUE, session);
				MailObject mailObject = new MailObject();
				mailObject.setSubject("Test mail");
				if (uyariNot != null)
					denemeMesaj += uyariNot.getValue();
				mailObject.setBody(denemeMesaj);
				MailPersonel mailUser = new MailPersonel();
				mailUser.setEPosta(ePosta);
				mailUser.setAdiSoyadi(personel.getAdSoyad());
				mailObject.getToList().add(mailUser);
				if (!ePosta.equals(authenticatedUser.getEmail()))
					mailObject.getBccList().add(authenticatedUser.getMailPersonel());
				try {
					mailStatu = ortakIslemler.mailSoapServisGonder(false, mailObject, renderer, "/email/testKullaniciMail.xhtml", session);

				} catch (Exception e) {
					mailStatu = new MailStatu();
					if (e.getMessage() != null)
						mailStatu.setHataMesai(e.getMessage());
					else
						mailStatu.setHataMesai("Hata oluştu!");
				}

				if (mailStatu != null) {
					if (mailStatu.getDurum())
						PdksUtil.addMessageInfo(ePosta + " test mesajı gönderilmiştir.");
					else
						PdksUtil.addMessageAvailableWarn(mailStatu.getHataMesai());
				}
			} else
				PdksUtil.addMessageWarn(ePosta + " geçersiz email!");
			// ortakIslemler.mailGonder(renderer, "/email/testKullaniciMail.xhtml");
			// PdksUtil.addMessageInfo(ePosta + " test mesajı gönderilmiştir.");
		} catch (Exception e) {
			PdksUtil.addMessageError(ePosta + " " + e);
			logger.error("Hata  --> " + ePosta);
		}
		if (denemeMesaj.indexOf("<br/>") > 0)
			denemeMesaj = PdksUtil.replaceAll(denemeMesaj, "<br/>", "\n");
		return "";

	}

	/**
	 * @param pdksPersonel
	 */
	private void izinGirisDurum(Personel pdksPersonel) {
		izinGirisiVar = Boolean.FALSE;
		if (pdksPersonel.getSirket() != null) {

			HashMap map = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select * from " + IzinTipi.TABLE_NAME + " " + PdksEntityController.getSelectLOCK());
			sb.append(" where " + IzinTipi.COLUMN_NAME_DURUM + " = 1 and " + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + " is null ");
			sb.append(" and " + IzinTipi.COLUMN_NAME_DEPARTMAN + " = :d and " + IzinTipi.COLUMN_NAME_GIRIS_TIPI + " <> :g ");
			map.put("d", pdksPersonel.getSirket().getDepartman().getId());
			map.put("g", IzinTipi.GIRIS_TIPI_YOK);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<IzinTipi> list = pdksEntityController.getObjectBySQLList(sb, map, IzinTipi.class);

			izinGirisiVar = !list.isEmpty();
			list = null;
		}
	}

	/**
	 * @param personel
	 * @return
	 */
	private List<String> getGebeIcapSuaDurumList(Personel personel) {
		List<String> list = null;
		if (personel != null && gebeIcapSuaDurumMap != null && !gebeIcapSuaDurumMap.isEmpty()) {
			if (gebeIcapSuaDurumMap.containsKey(-1))
				list = gebeIcapSuaDurumMap.get(-1);
			if (personel.getSirket() != null) {
				Long key = personel.getSirket().getDepartman().getId();
				if (gebeIcapSuaDurumMap.containsKey(key)) {
					if (list == null)
						list = new ArrayList<String>();
					list.addAll(gebeIcapSuaDurumMap.get(key));

				}
			}

		}
		return list;
	}

	/**
	 * @param personel
	 * @return
	 */
	public Boolean secSutIzni(Personel personel) {
		if (personel == null)
			personel = getInstance();
		Sirket sirket = personel.getSirket();
		boolean secim = personel.isSutIzniKullan() || (personel.getCinsiyetBayan() && sirket != null && sirket.isGebelikSutIzinVar());
		return secim;
	}

	/**
	 * @param personel
	 * @return
	 */
	public Boolean secGebe(Personel personel) {
		if (personel == null)
			personel = getInstance();
		Sirket sirket = personel.getSirket();
		List<String> list = sirket != null && sirket.isGebelikSutIzinVar() ? getGebeIcapSuaDurumList(personel) : null;
		boolean secim = personel.getGebeMi() || (personel.getCinsiyetBayan() && list != null && list.contains(Vardiya.GEBE_KEY));
		list = null;
		return secim;
	}

	/**
	 * @param personel
	 * @return
	 */
	public Boolean secSua(Personel personel) {
		if (personel == null)
			personel = getInstance();
		Sirket sirket = personel.getSirket();
		List<String> list = sirket != null && sirket.getSuaOlabilir() ? getGebeIcapSuaDurumList(personel) : null;
		boolean secim = personel.isSuaOlur() || (list != null && list.contains(Vardiya.SUA_KEY));
		list = null;
		return secim;
	}

	/**
	 * @param personel
	 * @return
	 */
	public Boolean secFMI(Personel personel) {
		if (personel == null)
			personel = getInstance();
		Sirket sirket = personel.getSirket();
		List<String> list = sirket != null && sirket.getFazlaMesaiIzinKullan() ? getGebeIcapSuaDurumList(personel) : null;
		boolean secim = personel.getFazlaMesaiIzinKullan() || (list != null && list.contains(Vardiya.FMI_KEY));
		list = null;
		return secim;
	}

	/**
	 * @param personel
	 * @return
	 */
	public Boolean secIcap(Personel personel) {
		if (personel == null)
			personel = getInstance();
		Sirket sirket = personel.getSirket();
		List<String> gebeIcapSuaDurumList = sirket != null ? getGebeIcapSuaDurumList(personel) : null;
		boolean secim = personel.getIcapciOlabilir() || (gebeIcapSuaDurumList != null && gebeIcapSuaDurumList.contains(Vardiya.ICAP_KEY));
		gebeIcapSuaDurumList = null;
		return secim;
	}

	private void gebeSuaIcapGuncelle() {
		if (gebeIcapSuaDurumMap == null)
			gebeIcapSuaDurumMap = new HashMap<Long, List<String>>();
		else
			gebeIcapSuaDurumMap.clear();

		StringBuffer sb = new StringBuffer();
		HashMap fields = new HashMap();
		sb.append("select distinct COALESCE(" + Vardiya.COLUMN_NAME_DEPARTMAN + ",-1) " + Vardiya.COLUMN_NAME_DEPARTMAN + ", CASE WHEN " + Vardiya.COLUMN_NAME_GEBELIK + "=1 THEN '" + Vardiya.GEBE_KEY + "' ");
		sb.append("	WHEN " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " = :fm1 THEN '" + Vardiya.FMI_KEY + "' ");
		sb.append("	WHEN " + Vardiya.COLUMN_NAME_SUA + " = 1 THEN '" + Vardiya.SUA_KEY + "' ");
		sb.append("	WHEN " + Vardiya.COLUMN_NAME_ICAP + " = 1 THEN '" + Vardiya.ICAP_KEY + "' END SONUC from " + Vardiya.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" where P." + PersonelKGS.COLUMN_NAME_DURUM + " = 1 ");
		sb.append(" and (" + Vardiya.COLUMN_NAME_GEBELIK + " + " + Vardiya.COLUMN_NAME_SUA + " + " + Vardiya.COLUMN_NAME_ICAP + " = 1 or " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " = :fm2 ) ");
		fields.put("fm1", Vardiya.TIPI_FMI);
		fields.put("fm2", Vardiya.TIPI_FMI);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		try {
			List<Object[]> veriler = pdksEntityController.getObjectBySQLList(sb, fields, null);
			for (Iterator iterator = veriler.iterator(); iterator.hasNext();) {
				Object[] objects = (Object[]) iterator.next();
				Long key = ((BigDecimal) objects[0]).longValue();
				String tip = (String) objects[1];
				List<String> list = gebeIcapSuaDurumMap.containsKey(key) ? gebeIcapSuaDurumMap.get(key) : new ArrayList<String>();
				if (list.isEmpty())
					gebeIcapSuaDurumMap.put(key, list);
				list.add(tip);
			}
			veriler = null;
		} catch (Exception e) {

			logger.error(e + "\n" + sb.toString());

		}

	}

	/**
	 * @param personelView
	 * @param pdksDurum
	 */
	public void kayitGuncelle(PersonelView personelView, boolean pdksDurum) {

		bakiyeIzinGoster = Boolean.FALSE;
		parentBordroTanim = null;
		izinGirisiVar = Boolean.FALSE;
		HashMap fields = new HashMap();
		denemeMesaj = "Deneme mesajı gönderilmiştir.";
		ePosta = "";
		eskiKullanici = null;
		kartNoAciklama = ortakIslemler.getParameterKey("kartNoAciklama");
		String tumPersonelDenklestirme = ortakIslemler.getParameterKey("tumPersonelDenklestirme");
		Departman pdksDepartman = null;
		ikinciYoneticiManuelTanimla = Boolean.FALSE;
		bosDepartman = null;
		bosDepartmanKodu = ortakIslemler.getParameterKey("bosDepartmanKodu");
		Personel pdksPersonel = personelView.getPdksPersonel();
		if (pdksPersonel == null) {
			pdksPersonel = new Personel();
			if (!tumPersonelDenklestirme.equals(""))
				pdksPersonel.setPdks(tumPersonelDenklestirme.equals("1"));
			personelView.setPdksPersonel(pdksPersonel);
		}

		if (PdksUtil.hasStringValue(bosDepartmanKodu)) {
			if (parentDepartman == null) {
				parentDepartman = ortakIslemler.getSQLTanimAktifByTipKodu(Tanim.TIPI_PERSONEL_EK_SAHA, "ekSaha1", session);
			}
			if (parentDepartman != null) {
				fields.clear();
				StringBuffer sb = new StringBuffer();
				sb.append("select TOP 1 P.* from " + Tanim.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where P." + Tanim.COLUMN_NAME_TIPI + " = :t and P." + Tanim.COLUMN_NAME_KODU + " = :k ");
				sb.append(" and P." + Tanim.COLUMN_NAME_PARENT_ID + " = :p and P." + Tanim.COLUMN_NAME_DURUM + " = 0");
				fields.put("t", Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA);
				fields.put("k", bosDepartmanKodu);
				fields.put("p", parentDepartman.getId());
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<Tanim> list = pdksEntityController.getObjectBySQLList(sb, fields, Tanim.class);
				bosDepartman = list != null && !list.isEmpty() ? list.get(0) : null;

			}

		}

		try {
			if (pdksPersonel != null && !pdksPersonel.getSirket().getDepartman().isAdminMi()) {
				pdksDepartman = pdksPersonel.getSirket().getDepartman();
			}
			if (pdksDepartman == null && authenticatedUser.isIK() && !authenticatedUser.isIKAdmin())
				pdksDepartman = authenticatedUser.getDepartman();
			bolumDepartmanlari = ortakIslemler.getBolumDepartmanlari(pdksDepartman, session);
			gorevDepartmanlari = ortakIslemler.getGorevDepartmanlari(pdksDepartman, session);
		} catch (Exception e) {

		}

		asilYonetici1 = null;
		if (pdksPersonel != null && pdksPersonel.getAsilYonetici1() != null) {
			if (pdksPersonel.getSirket().isErp()) {
				if (pdksPersonel.getYoneticisi() != null && !pdksPersonel.getYoneticisi().getId().equals(pdksPersonel.getAsilYonetici1().getId()))
					asilYonetici1 = pdksPersonel.getYoneticisi();
			} else
				asilYonetici1 = pdksPersonel.getYoneticisi();
		}
		setHataMesaj("");
		setPersonelView(personelView);

		setInstance(pdksPersonel);
		userMenuList(personelView.getKullanici());

		Sirket sirket = pdksPersonel != null && pdksPersonel.getId() != null ? pdksPersonel.getSirket() : null;
		User kullaniciPer = personelView.getKullanici();
		onaysizIzinKullanilir = Boolean.FALSE;
		ikinciYoneticiIzinOnayla = Boolean.FALSE;
		if (sirket != null && pdksPersonel.isCalisiyor() && (kullaniciPer == null || kullaniciPer.getId() == null)) {
			LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
			veriMap.put("personelId", pdksPersonel.getId());
			StringBuffer sb = new StringBuffer("SP_FIND_OLD_USER");
			if (session != null)
				veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<User> kullanicilar = null;
			try {
				kullanicilar = pdksEntityController.execSPList(veriMap, sb, User.class);
			} catch (Exception e1) {
			}
			if (kullanicilar != null) {
				for (Iterator iterator = kullanicilar.iterator(); iterator.hasNext();) {
					User user = (User) iterator.next();
					if (!user.getPdksPersonel().isCalisiyor()) {
						eskiKullanici = user;
						break;
					}
				}
				kullanicilar = null;
			}
		}

		// List<IzinTipi> izinTipiList = getOnaysizIzinDurum(sirket);
		TreeMap<String, Boolean> map1 = getOnaysizIzinDurumMap(sirket);
		onaysizIzinKullanilir = map1.containsKey("onaysizIzinDurum");
		ikinciYoneticiIzinOnayla = map1.containsKey("ikinciYoneticiIzinOnaySec");

		if (sirket != null) {
			if (sirket.getDepartman().isAdminMi() == false)
				ikinciYoneticiManuelTanimla = ikinciYoneticiIzinOnayla || sirket.getDepartman().isFazlaMesaiTalepGirer();
			else if (!ortakIslemler.getParameterKey("yonetici2ERPKontrol").equals("1"))
				ikinciYoneticiManuelTanimla = ikinciYoneticiIzinOnayla || sirket.getDepartman().isFazlaMesaiTalepGirer();

		}

		setOldSirket(sirket);
		setOldUserName(personelView.getKullanici() != null ? personelView.getKullanici().getUsername() : null);

		PersonelKGS personelKGS = personelView.getPersonelKGS();
		if (pdksPersonel != null && sirket != null && !pdksPersonel.getSirket().isErp())
			try {
				fillPdksPersonelList();
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();

			}
		User kullanici = null;
		if (pdksPersonel.getPersonelKGS() == null) {
			kullanici = new User();
			if (!authenticatedUser.isAdmin())
				kullanici.setDepartman(authenticatedUser.getDepartman());
			kullanici.setDurum(Boolean.FALSE);
			pdksPersonel.setPersonelKGS(personelKGS);
			pdksPersonel.setAd(personelKGS.getAd());
			pdksPersonel.setSoyad(personelKGS.getSoyad());
			pdksPersonel.setDurum(personelKGS.getDurum());
			pdksPersonel.setDogumTarihi(personelKGS.getDogumTarihi());
			pdksPersonel.setIseBaslamaTarihi(personelKGS.getIseBaslamaTarihi());
			pdksPersonel.setGrubaGirisTarihi(personelKGS.getIseBaslamaTarihi());
			pdksPersonel.setIzinHakEdisTarihi(personelKGS.getIseBaslamaTarihi());
		} else {

			if (personelView.getKullanici() == null) {
				kullanici = new User();
				if (!authenticatedUser.isAdmin())
					kullanici.setDepartman(authenticatedUser.getDepartman());
				kullanici.setDurum(Boolean.FALSE);
				kullanici.setYeniSifre(Boolean.FALSE);
			} else {
				kullanici = personelView.getKullanici();
			}

		}
		if (kullanici != null) {
			if (kullanici.getId() != null)
				PdksUtil.setUserYetki(kullanici);
			pdksPersonel.setKullanici(kullanici);
		}
		if (pdksPersonel.getCalismaModeli() == null && calismaModeliList.size() == 1) {
			pdksPersonel.setCalismaModeli(calismaModeliList.get(0));
			savePersonel(pdksPersonel);
		}
		if (pdksPersonel.getId() == null) {
			pdksPersonel.setAd(pdksPersonel.getAd().toUpperCase(Constants.TR_LOCALE));
			pdksPersonel.setSoyad(pdksPersonel.getSoyad().toUpperCase(Constants.TR_LOCALE));
			pdksPersonel.setPdksSicilNo(personelView.getSicilNo());
			pdksPersonel.setPdks(Boolean.TRUE);
			pdksPersonel.setMailTakip(Boolean.TRUE);
		}
		if (pdksPersonel.getKullanici() != null) {
			pdksPersonel.getKullanici().setYeniSifre(Boolean.FALSE);
		}
		String emailCC = ortakIslemler.getAktifMailAdress(pdksPersonel.getEmailCC(), session);
		String emailBCC = ortakIslemler.getAktifMailAdress(pdksPersonel.getEmailBCC(), session);
		String hareketMail = ortakIslemler.getAktifMailAdress(pdksPersonel.getHareketMail(), session);
		if (PdksUtil.hasStringValue(emailCC)) {
			pdksPersonel.setEmailCC(emailCC);
			adresAyarla(null, MAIL_CC);
			String yeniAdres = ortakIslemler.adresDuzelt(ccAdresList);
			if (!yeniAdres.equals(emailCC)) {
				pdksPersonel.setEmailCC(yeniAdres);
			}
		}
		if (PdksUtil.hasStringValue(emailBCC)) {
			pdksPersonel.setEmailBCC(emailBCC);
			adresAyarla(null, MAIL_BCC);
			String yeniAdres = ortakIslemler.adresDuzelt(bccAdresList);
			if (!yeniAdres.equals(emailBCC)) {
				pdksPersonel.setEmailBCC(yeniAdres);
			}
		}
		if (PdksUtil.hasStringValue(hareketMail)) {
			pdksPersonel.setHareketMail(hareketMail);
			adresAyarla(null, MAIL_HAREKET);
			String yeniAdres = ortakIslemler.adresDuzelt(hareketAdresList);
			if (!yeniAdres.equals(hareketMail)) {
				pdksPersonel.setHareketMail(yeniAdres);
			}
		}
		fillPersonelTablolar(pdksDurum);

		if (pdksPersonel.getKullanici() != null && pdksPersonel.getKullanici().getId() == null && departmanKullaniciList != null && departmanKullaniciList.size() == 1)
			pdksPersonel.getKullanici().setDepartman(departmanTanimList.get(0));
		tesisYetki = ortakIslemler.getParameterKey("tesisYetki").equals("1");
		if (tesisYetki && authenticatedUser.isIK_Tesis())
			tesisYetki = authenticatedUser.getYetkiliTesisler() != null && authenticatedUser.getYetkiliTesisler().isEmpty() == false;
		bolumYetki = ortakIslemler.getParameterKey("bolumYetki").equals("1");
		fillDistinctRoleList();
		if (tesisYetki)
			fillDistinctTesisList();
		if (bolumYetki)
			fillDistinctBolumList();
		if (pdksPersonel.getKullanici() != null)
			PdksUtil.setUserYetki(pdksPersonel.getKullanici());

		dinamikPersonelDurumList.clear();
		dinamikPersonelSayisalList.clear();
		dinamikPersonelTanimList.clear();
		dinamikPersonelAciklamaMap.clear();
		if (pdksPersonel.getId() != null)
			getPersonelDinamikMap(pdksPersonel.getId());
		else
			personelDinamikMap.clear();
		for (Tanim tanim : dinamikDurumList) {
			PersonelDinamikAlan pda = getPersonelDinamikAlan(tanim, pdksPersonel);
			if (pda.getAlan().getKodu().equals("ikinciYoneticiOlmaz")) {
				getIkinciYoneticiOlmazList(pda);
			}
			dinamikPersonelDurumList.add(pda);
		}

		for (Tanim tanim : dinamikSayisalList)
			dinamikPersonelSayisalList.add(getPersonelDinamikAlan(tanim, pdksPersonel));
		for (Tanim tanim : dinamikTanimList)
			dinamikPersonelTanimList.add(getPersonelDinamikAlan(tanim, pdksPersonel));
		if (!dinamikTanimList.isEmpty()) {
			List<Long> idList = new ArrayList<Long>();
			for (Tanim tanim : dinamikTanimList) {
				idList.add(tanim.getId());

			}
			HashMap map = new HashMap();

			String fieldName = "p";
			StringBuffer sb = new StringBuffer();
			sb.append("select * from " + Tanim.TABLE_NAME + " " + PdksEntityController.getSelectLOCK());
			sb.append(" where " + Tanim.COLUMN_NAME_DURUM + " = 1 and " + Tanim.COLUMN_NAME_TIPI + " = :t ");
			sb.append(" and " + Tanim.COLUMN_NAME_PARENT_ID + "  :" + fieldName);
			map.put(fieldName, idList);
			map.put("t", Tanim.TIPI_PERSONEL_DINAMIK_LISTE_TANIM);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Tanim> tanimList = pdksEntityController.getSQLParamList(idList, sb, fieldName, map, Tanim.class, session);
			if (!tanimList.isEmpty())
				tanimList = PdksUtil.sortObjectStringAlanList(tanimList, "getErpKodu", null);
			for (Tanim tanim : tanimList) {
				Long key = tanim.getParentTanim().getId();
				List<Tanim> list = dinamikPersonelAciklamaMap.containsKey(key) ? dinamikPersonelAciklamaMap.get(key) : new ArrayList<Tanim>();
				if (list.isEmpty())
					dinamikPersonelAciklamaMap.put(key, list);
				list.add(tanim);
			}
		}
		if (pdksPersonel.getId() == null) {
			gebeSecim = false;
			if (sirketList.isEmpty())
				PdksUtil.addMessageAvailableWarn("İşlem yapılacak ERP olamayan " + ortakIslemler.sirketAciklama() + " yoktur!");
			else if (sirketList.size() == 1) {
				pdksPersonel.setSirket(sirketList.get(0));
				fillPdksPersonelList();
				if (pdksPersonel.getSirket().getPdks()) {
					pdksPersonel.setPdks(pdksPersonel.getSirket().getPdks());
					if (pdksPersonel.getSirket().getFazlaMesaiOde())
						pdksPersonel.setFazlaMesaiOde(pdksPersonel.getSirket().getFazlaMesaiOde());
				}

			}
		}

		else {
			cinsiyetDegisti();
			if (pdksPersonel.getSirket().isErp() == false) {
				erpSirketleriAyikla(sirketList);
			} else
				parentBordroTanim = ortakIslemler.getEkSaha4(pdksPersonel.getSirket(), null, session);

		}
		PersonelIzin izin = null;
		try {
			bakiyeDurumKontrolEt(pdksPersonel);

			if (bakiyeIzinGoster) {
				Departman departman = pdksPersonel.getSirket() != null ? pdksPersonel.getSirket().getDepartman() : null;
				if (pdksPersonel.getId() != null)
					izin = ortakIslemler.getPersonelBakiyeIzin(0, authenticatedUser, pdksPersonel, session);
				if (izin == null)
					bakiyeIzinGoster = getBakiyeDeparmanIzinDurum(departman);

			}
			setBakiyeIzinSuresi(izin != null ? izin.getIzinSuresi() : null);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			izin = null;
		}
		fillCalismaModeliVardiyaList();
		setBakiyeIzin(izin);
		izinGirisDurum(pdksPersonel);
		ekSahaDisable();
		if (pdksPersonel.getId() != null) {
			ortakIslemler.addObjectList(pdksPersonel.getSablon(), sablonlar, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getCalismaModeli(), calismaModeliList, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getCinsiyet(), cinsiyetList, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getTesis(), tesisList, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getEkSaha3(), bolumDepartmanlari, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getSirket(), sirketList, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getGorevTipi(), gorevDepartmanlari, Boolean.FALSE);
			ortakIslemler.addObjectList(pdksPersonel.getPersonelTipi(), personelTipleri, Boolean.FALSE);
			for (String key : ekSahaListMap.keySet()) {
				List<Tanim> ekSahaList = ekSahaListMap.get(key);
				Tanim ekSaha = null;
				if (key.endsWith("1"))
					ekSaha = pdksPersonel.getEkSaha1();
				else if (key.endsWith("2"))
					ekSaha = pdksPersonel.getEkSaha2();
				else if (key.endsWith("3"))
					ekSaha = pdksPersonel.getEkSaha3();
				else if (key.endsWith("4"))
					ekSaha = pdksPersonel.getEkSaha4();
				if (ekSaha != null && ekSaha.getDurum().equals(Boolean.FALSE))
					ortakIslemler.addObjectList(ekSaha, ekSahaList, Boolean.FALSE);

			}
		}
	}

	/**
	 * @param pdksPersonel
	 * @return
	 */
	public String bakiyeDurumKontrolEt(Personel pdksPersonel) {
		String bakiyeIzinSoDonemStr = ortakIslemler.getParameterKey("bakiyeIzinSonDonem");
		try {
			bakiyeIzinGoster = pdksPersonel.getIzinKartiVar() && PdksUtil.hasStringValue(bakiyeIzinSoDonemStr);
			if (bakiyeIzinGoster) {
				long donemBitis = Long.parseLong(bakiyeIzinSoDonemStr);
				bakiyeIzinGoster = pdksPersonel.getIzinHakEdisTarihi() == null || Long.parseLong(PdksUtil.convertToDateString(pdksPersonel.getIzinHakEdisTarihi(), "yyyyMM")) < donemBitis;
			}
		} catch (Exception e) {
			bakiyeIzinGoster = false;
		}

		return "";
	}

	/**
	 * 
	 */
	private void saveIkinciYoneticiOlmazList() {
		if (ikinciYoneticiHataliList != null) {
			Date guncellemeTarihi = new Date();
			for (Personel personel2 : ikinciYoneticiHataliList) {
				if (personel2.isCheckBoxDurum()) {
					personel2.setAsilYonetici2(personel2.getYoneticisi());
					personel2.setGuncellemeTarihi(guncellemeTarihi);
					personel2.setGuncelleyenUser(authenticatedUser);
					session.saveOrUpdate(personel2);
				}
			}
		}
	}

	/**
	 * @param personelDinamikAlan
	 * @return
	 */
	public String getIkinciYoneticiOlmazList(PersonelDinamikAlan personelDinamikAlan) {
		ikinciYoneticiHataliList = null;
		if (personelDinamikAlan.getPersonel().getId() != null && personelDinamikAlan.getDurumSecim() != null) {
			Long perId = personelDinamikAlan.getPersonel().getId();
			boolean durum = personelDinamikAlan.getDurumSecim() != null && personelDinamikAlan.getDurumSecim();
			if (durum)
				ikinciYoneticiHataliList = ortakIslemler.getIkinciYoneticiOlmazList(perId, personelDinamikAlan.getAlan().getKodu(), session);
			else {
				ikinciYoneticiHataliList = ortakIslemler.getIkinciYoneticiOlmazList(0L, personelDinamikAlan.getAlan().getKodu(), session);
				for (Iterator iterator = ikinciYoneticiHataliList.iterator(); iterator.hasNext();) {
					Personel personel = (Personel) iterator.next();
					if (!personel.getId().equals(perId) && !(personel.getYoneticisi() != null && personel.getYoneticisi().getId().equals(perId)))
						iterator.remove();

				}
			}
			for (Personel p2 : ikinciYoneticiHataliList) {
				p2.setCheckBoxDurum(Boolean.TRUE);
			}
		}
		if (ikinciYoneticiHataliList == null)
			ikinciYoneticiHataliList = new ArrayList<Personel>();

		return "";
	}

	/**
	 * @return
	 */
	public String durumIK() {
		Personel seciliPersonel = getInstance();
		if (seciliPersonel != null) {
			User seciliKullanici = seciliPersonel.getKullanici();
			if (seciliKullanici != null) {
				if (seciliKullanici.getDepartman() != null) {
					StringBuffer sb = kullaniciRolKontrolMesaji(seciliKullanici, false);
					if (sb != null)
						sb = null;

				}
				PdksUtil.setUserYetki(seciliKullanici);
			}
		}
		return "";
	}

	/**
	 * @param seciliKullanici
	 * @param kayit
	 * @return
	 */
	private StringBuffer kullaniciRolKontrolMesaji(User seciliKullanici, boolean kayit) {
		StringBuffer sb = null;
		if (seciliKullanici != null && seciliKullanici.getYetkiliRollerim() != null) {
			sb = new StringBuffer();
			for (Iterator iterator = seciliKullanici.getYetkiliRollerim().iterator(); iterator.hasNext();) {
				Role role = (Role) iterator.next();
				if (role.getDepartman() != null && !role.getDepartman().getId().equals(seciliKullanici.getDepartman().getId())) {
					if (sb.length() > 0)
						sb.append(", ");
					sb.append("\"" + role.getAciklama() + "\"");
					distinctRoleList.add(role);
					iterator.remove();
				}
			}
			if (sb.length() > 0) {
				String hataliRol = sb.toString();
				sb = new StringBuffer(hataliRol + " " + seciliKullanici.getDepartman().getAciklama() + " ait rol" + (hataliRol.indexOf(",") > 0 ? "ler" : "") + " değildir!");
				if (!kayit)
					PdksUtil.addMessageError(sb.toString());
			} else
				sb = null;

		}
		return sb;
	}

	/**
	 * @param pdksDurum
	 */
	private void fillPersonelTablolar(boolean pdksDurum) {
		fillGorevTipiTanimList();
		Personel pdksPersonel = getInstance();
		if (pdksDurum) {
			fillCinsiyetList();
			fillYoneticiVardiyaTipiTanimList();
			fillMasrafYeriTanimList();
			fillVardiyaGirisTipiList();
			fillTesisTanimList();
			fillDepartmanTanimList();
			fillDepartmanPDKSTanimList();
			fillEkSahaTanim();
			fillYoneticiList();
			fillPersonelTipList();
			if (pdksPersonel.getPersonelTipi() == null)
				fillPdksCalismaModeliList();
			else
				personelTipiDegisti();
			fillPdksVardiyaSablonList();
		}
		List<Sirket> list = ortakIslemler.fillSirketList(session, pdksDurum, Boolean.TRUE);
		if (!personelERPGuncelleme.equalsIgnoreCase("M") && (pdksPersonel == null || pdksPersonel.getId() == null))
			erpSirketleriAyikla(list);

		if (pdksPersonel != null && pdksPersonel.getId() != null && !personelERPGuncelleme.equalsIgnoreCase("M")) {
			Sirket sirketPersonel = pdksPersonel.getSirket();
			boolean ekle = true;
			if (sirketPersonel != null && authenticatedUser.isAdmin() == false) {
				Long departmanId = sirketPersonel.getDepartman().getId();
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Sirket sirket = (Sirket) iterator.next();
					if (!sirket.getDepartman().getId().equals(departmanId))
						iterator.remove();
				}
			}

			for (Sirket sirket : list) {
				if (sirket.getId().equals(sirketPersonel.getId()))
					ekle = false;
			}
			if (ekle) {
				list.add(sirketPersonel);
				list = PdksUtil.sortObjectStringAlanList(list, "getAd", null);
			}
		}
		if (pdksPersonel.getSirket() != null && pdksPersonel.isCalisiyor() == false && pdksPersonel.getSirket().getDurum().equals(Boolean.FALSE))
			list.add(pdksPersonel.getSirket());
		setSirketList(list);
	}

	/**
	 * @param list
	 */
	private void erpSirketleriAyikla(List<Sirket> list) {
		if (personelERPGuncelleme.equalsIgnoreCase("T")) {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Sirket sirket = (Sirket) iterator.next();
				if (sirket.isErp())
					iterator.remove();
			}
		}
	}

	public boolean tesisGoster() {
		boolean goster = false;
		if (tesisYetki) {
			Personel personel = getInstance();
			if (personel.getKullanici() != null) {
				List<Role> rolList = personel.getKullanici().getYetkiliRollerim();
				if (rolList != null && rolList.isEmpty() == false) {
					List<String> rolNameList = null;
					String tesisYetkiliRoller = ortakIslemler.getParameterKey("tesisYetkiliRoller");

					if (PdksUtil.hasStringValue(tesisYetkiliRoller))
						rolNameList = PdksUtil.getListStringTokenizer(tesisYetkiliRoller, null);
					else
						rolNameList = Arrays.asList(new String[] { Role.TIPI_IK, Role.TIPI_IK_SIRKET, Role.TIPI_IK_Tesis, Role.TIPI_TESIS_SUPER_VISOR });
					for (Role role : rolList) {
						if (rolNameList.contains(role.getRolename())) {
							goster = true;
							break;
						}

					}
				}
			}
		}
		return goster;

	}

	/**
	 * 
	 */
	public void fillYoneticiList() {
		setYoneticiList(null);
		if (authenticatedUser.isAdmin() || (authenticatedUser.isIK() && authenticatedUser.getDepartman().isAdminMi())) {
			List<Long> idList = new ArrayList<Long>();
			if (authenticatedUser.getTumPersoneller() != null)
				for (Personel per : authenticatedUser.getTumPersoneller()) {
					idList.add(per.getId());
				}

			List<Personel> list = null;

			try {

				Date bugun = PdksUtil.getDate(Calendar.getInstance().getTime());
				LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
				map.put("tarih", PdksUtil.convertToDateString(bugun, "yyyyMMdd"));
				map.put("df", "112");
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				StringBuffer sp = new StringBuffer("SP_GET_YONETICI");
				list = pdksEntityController.execSPList(map, sp, Personel.class);

				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Personel personel = (Personel) iterator.next();
					if (!idList.contains(personel.getId()))
						iterator.remove();
				}
				idList = null;
				Personel pdksPersonel = personelView != null ? personelView.getPdksPersonel() : null, yoneticisi = null, asilYonetici1 = null, asilYonetici2 = null;
				if (pdksPersonel != null) {
					yoneticisi = pdksPersonel.getYoneticisi();
					asilYonetici1 = pdksPersonel.getAsilYonetici1();
					asilYonetici2 = pdksPersonel.getAsilYonetici2();
				}
				Boolean yoneticiEkle = yoneticisi != null, yonetici1Ekle = asilYonetici1 != null, yonetici2Ekle = asilYonetici2 != null;
				if (yoneticiEkle || yonetici1Ekle || yonetici2Ekle) {
					for (Personel yonetici : list) {
						try {
							if (yoneticisi != null && yonetici.getId().equals(yoneticisi.getId())) {
								yoneticiEkle = Boolean.FALSE;
							}
							if (asilYonetici1 != null && yonetici.getId().equals(asilYonetici1.getId())) {
								yonetici1Ekle = Boolean.FALSE;
							}
							if (asilYonetici2 != null && yonetici.getId().equals(asilYonetici2.getId())) {
								yonetici2Ekle = Boolean.FALSE;
							}
						} catch (Exception e) {

						}

					}
					List<Personel> ekler = new ArrayList<Personel>();
					if (yoneticiEkle)
						ekler.add(yoneticisi);
					if (yonetici1Ekle)
						ekler.add(asilYonetici1);
					if (yonetici2Ekle)
						ekler.add(asilYonetici2);
					if (!ekler.isEmpty()) {
						for (Personel personel : ekler) {
							if (personel != null) {
								boolean ekle = true;
								for (Iterator iterator = list.iterator(); iterator.hasNext();) {
									Personel yoneticiPersonel = (Personel) iterator.next();
									if (yoneticiPersonel.getId().equals(personel.getId())) {
										ekle = false;
										break;
									}
								}
								if (ekle)
									list.add(personel);
							}
						}
					}
					ekler = null;

				}
				if (list.size() > 1)
					list = PdksUtil.sortObjectStringAlanList(list, "getAdSoyad", null);
				yonetici2List = new ArrayList<Personel>();
				setYoneticiList(list);
				if (!list.isEmpty()) {
					yonetici2List.addAll(list);
					ikinciYoneticiOlmazKontrolu();
				}

			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());

			}
		}
	}

	/**
	 * 
	 */
	private void ikinciYoneticiOlmazKontrolu() {
		List<Long> yIdList = null;
		HashMap fields = new HashMap();
		if (ikinciYoneticiIzinOnayla) {
			fields.put(PdksEntityController.MAP_KEY_SELECT, "pdksPersonel.id");
			fields.put("pdksPersonel.ikinciYoneticiIzinOnayla=", Boolean.FALSE);
			fields.put("durum=", Boolean.TRUE);
			fields.put("pdksPersonel.durum=", Boolean.TRUE);
			fields.put("pdksPersonel.sskCikisTarihi>=", PdksUtil.getDate(new Date()));
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			yIdList = pdksEntityController.getObjectByInnerObjectListInLogic(fields, User.class);
		} else
			yIdList = new ArrayList<Long>();

		Tanim ikinciYoneticiOlmaz = (Tanim) ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_PERSONEL_DINAMIK_DURUM, Tanim.IKINCI_YONETICI_ONAYLAMAZ, session);
		if (yIdList.size() > 0 || (ikinciYoneticiOlmaz != null && ikinciYoneticiOlmaz.getDurum())) {
			if (ikinciYoneticiOlmaz != null && ikinciYoneticiOlmaz.getDurum()) {
				List<Long> idList = new ArrayList<Long>();
				for (Personel personel : yonetici2List)
					if (!yIdList.contains(personel.getId()))
						idList.add(personel.getId());
				fields.clear();
				String fieldName = "p";
				StringBuffer sb = new StringBuffer();
				sb.append(" select D." + PersonelDinamikAlan.COLUMN_NAME_PERSONEL + " from " + PersonelDinamikAlan.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where D." + PersonelDinamikAlan.COLUMN_NAME_PERSONEL + " :" + fieldName + " and " + PersonelDinamikAlan.COLUMN_NAME_ALAN + " = " + ikinciYoneticiOlmaz.getId());
				sb.append(" and " + PersonelDinamikAlan.COLUMN_NAME_DURUM_SECIM + " = 1 ");
				fields.put(fieldName, idList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				// List<BigDecimal> list2 = pdksEntityController.getObjectBySQLList(sb, fields, null);
				List<BigDecimal> list2 = pdksEntityController.getSQLParamList(idList, sb, fieldName, fields, null, session);

				for (BigDecimal bigDecimal : list2) {
					yIdList.add(bigDecimal.longValue());
				}
			}
			for (Iterator iterator = yonetici2List.iterator(); iterator.hasNext();) {
				Personel personel = (Personel) iterator.next();
				if (yIdList.contains(personel.getId()))
					iterator.remove();

			}
		}
	}

	/**
	 * @return
	 */
	public String fillOrganizasyonAgaciList() {
		fillPersonelList();
		fillPersonelViewTree();
		return "";
	}

	@Transactional
	public String deletePersonelData() {
		Personel personel = getInstance();
		List<Object[]> list = null;
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("personelNo", personel.getPdksSicilNo());
		if (session != null)
			map.put(PdksEntityController.MAP_KEY_SESSION, session);
		StringBuffer sp = new StringBuffer("SP_PERSONEL_NO_DELETE");
		try {
			list = pdksEntityController.execSPList(map, sp, null);
		} catch (Exception e) {
		}
		if (list == null)
			list = new ArrayList();
		else if (!list.isEmpty()) {
			Object[] objects = list.get(0);
			Object perId = objects[0];
			if (perId != null) {
				PdksUtil.addMessageInfo(personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " personel bilgileri silinmiştir.");
				session.clear();
				fillPersonelKGSList();
			} else
				PdksUtil.addMessageWarn(personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " personel bilgileri silinemez!");
		}
		return "";
	}

	/**
	 * 
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void fillPersonelList() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, "personelListesi");
		personelDurumMap.clear();
		bakiyeTakipEdiliyor = ortakIslemler.getBakiyeTakipEdiliyor(session);
		sanalPersonelAciklama = ortakIslemler.sanalPersonelAciklama();
		yoneticiRolVarmi = ortakIslemler.yoneticiRolKontrol(session);
		fillEkSahaTanim();

		HashMap parametreMap = new HashMap();
		ArrayList<Personel> tumPersoneller = new ArrayList<Personel>(authenticatedUser.getTumPersoneller());
		Date bugun = PdksUtil.buGun();
		if (authenticatedUser.isYoneticiKontratli())
			ortakIslemler.digerPersoneller(tumPersoneller, null, bugun, bugun, session);

		List<Long> pList = new ArrayList<Long>();
		StringBuffer sb = new StringBuffer();
		sb.append("select V.* from " + PdksPersonelView.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = V." + PdksPersonelView.COLUMN_NAME_PERSONEL);
		ortakIslemler.addIKSirketTesisKriterleri(parametreMap, sb);
		if (!authenticatedUser.isAdmin()) {
			sb.append(" and P." + Personel.COLUMN_NAME_DURUM + " = 1 and P." + Personel.COLUMN_NAME_ID + " :d");
			for (Personel personel : tumPersoneller) {
				pList.add(personel.getId());
			}
			parametreMap.put("d", pList);

		} else
			sb.append(" where V." + PdksPersonelView.COLUMN_NAME_DURUM + " = 1 ");

		if (session != null)
			parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<PersonelView> list = ortakIslemler.getPersonelViewList(pdksEntityController.getObjectBySQLList(sb, parametreMap, PdksPersonelView.class));
		tumPersoneller = null;
		pList = null;
		TreeMap<String, Boolean> map = mantiksalAlanlariDoldur(list);
		for (String key : map.keySet())
			personelDurumMap.put(key, map.get(key));
		if (authenticatedUser.isAdmin())
			for (PersonelView personelView : list) {
				try {
					Personel pdksPersonel = personelView.getPdksPersonel();
					if (pdksPersonel.getEmailCC() == null && pdksPersonel.getEmailBCC() == null)
						continue;
					boolean degisti = Boolean.FALSE;
					if (pdksPersonel.getEmailCC() != null) {
						String emailCC = ortakIslemler.getPersonelCCMail(pdksPersonel);
						if (emailCC != null) {
							degisti = !pdksPersonel.getEmailCC().trim().equals(emailCC.trim());
							pdksPersonel.setEmailCC(emailCC);
						}
					}
					if (pdksPersonel.getEmailBCC() != null) {
						String emailBCC = ortakIslemler.getPersonelBCCMail(pdksPersonel);
						if (emailBCC != null) {
							if (!degisti)
								degisti = !pdksPersonel.getEmailBCC().trim().equals(emailBCC.trim());
							pdksPersonel.setEmailBCC(emailBCC);
						}
					}
					if (pdksPersonel.getHareketMail() != null) {
						String hareketMail = ortakIslemler.getPersonelHareketMail(pdksPersonel);
						if (hareketMail != null) {
							if (!degisti)
								degisti = !pdksPersonel.getHareketMail().trim().equals(hareketMail.trim());
							pdksPersonel.setHareketMail(hareketMail);
						}
					}

					if (degisti) {
						pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);

						session.flush();
						session.refresh(personelView);

					}
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					logger.info(personelView.getSicilNo());
				}

			}
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 1);
		Date tarih = PdksUtil.getDate(cal.getTime());
		List<PersonelView> list2 = new ArrayList<PersonelView>();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			PersonelView personelView = (PersonelView) iterator.next();
			if (personelView.getPdksPersonel() == null || personelView.getPdksPersonel().isCalisiyorGun(tarih) == false) {
				list2.add(personelView);
				iterator.remove();

			}

		}
		if (!list2.isEmpty())
			list.addAll(list2);
		list2 = null;
		setPersonelList(list);
		rootNodeForAllPersonelView = null;
	}

	/**
	 * @param event
	 */
	public void setDisableAdviseNodeOpened(org.richfaces.event.NodeExpandedEvent event) {
		this.disableAdviseNodeOpened = true;
	}

	/**
	 * 
	 */
	private void fillPersonelViewTree() {
		try {
			rootNodeForAllPersonelView = new TreeNodeImpl<PersonelView>();
			if (disableAdviseNodeOpened == null)
				disableAdviseNodeOpened = !personelList.isEmpty();
			List<PersonelView> list = PdksUtil.sortObjectStringAlanList(personelList, "getAdSoyad", null);
			HashMap<String, LinkedHashMap<Long, PersonelView>> sirketMap = new HashMap<String, LinkedHashMap<Long, PersonelView>>();
			HashMap<Long, PersonelView> personelDataMap = new HashMap<Long, PersonelView>();
			for (PersonelView personelView : list) {
				personelView.setAltPersoneller(null);
				personelView.setUstPersonelView(null);
				if (personelView.getPdksPersonel() != null)
					personelDataMap.put(personelView.getPdksPersonelId(), personelView);

			}
			String yoneticiSirketBazli = ortakIslemler.getParameterKey("yoneticiSirketBazli");
			HashMap<String, String> aciklamaMap = new HashMap<String, String>();
			for (PersonelView personelView : list) {
				if (personelView.getPdksPersonel() != null) {
					Personel personel = personelView.getPdksPersonel();
					Sirket sirket = personel.getSirket();
					if (sirket.getPdks().booleanValue() == false || sirket.getDepartman().isAdminMi() == false)
						continue;
					String key = null;
					if (yoneticiSirketBazli.equals("") && sirket.getSirketGrup() == null) {
						key = "S" + sirket.getId();
						if (!aciklamaMap.containsKey(key))
							aciklamaMap.put(key, sirket.getAd());
					} else {
						key = sirket.getSirketGrup() != null ? "G" + sirket.getSirketGrup().getId() : "Y";
						if (!aciklamaMap.containsKey(key))
							aciklamaMap.put(key, sirket.getSirketGrup() != null ? sirket.getSirketGrup().getAciklama() : yoneticiSirketBazli);
					}
					LinkedHashMap<Long, PersonelView> perMap = sirketMap.containsKey(key) ? sirketMap.get(key) : new LinkedHashMap<Long, PersonelView>();
					if (personelView.getSicilNo().equals("0214") || personelView.getSicilNo().equals("0584"))
						logger.debug("");
					Personel yonetici = personel.getYoneticisi();
					Long yoneticiId = yonetici != null ? personel.getYoneticisi().getId() : personel.getId();
					if (yonetici != null && yonetici.getYoneticisi() != null && personel.getId().equals(yonetici.getYoneticisi().getId()))
						yoneticiId = personel.getId();
					if (personelDataMap.containsKey(yoneticiId)) {
						PersonelView personelYonetici = personelDataMap.get(yoneticiId);
						if (!personel.getId().equals(yoneticiId)) {
							personelView.setUstPersonelView(personelYonetici);
							if (personelView.getOrganizasyonDurum())
								personelView.getYoneticiDurumKontrol();
							personelYonetici.addAltPersonel(personelView);
						}
						if (perMap.isEmpty())
							sirketMap.put(key, perMap);
						perMap.put(yoneticiId, personelYonetici);
					}
				}
			}

			list = null;
			if (!sirketMap.isEmpty()) {
				for (String key : sirketMap.keySet()) {
					LinkedHashMap<Long, PersonelView> perMap = sirketMap.get(key);
					LinkedHashMap<Long, PersonelView> perDataMap = new LinkedHashMap<Long, PersonelView>();
					PersonelView yoneticiSirketView = new PersonelView();
					Personel yoneticiPer = new Personel();
					yoneticiPer.setPdksSicilNo("Yönetici Tanımsız");
					yoneticiSirketView.setPdksPersonel(yoneticiPer);
					yoneticiSirketView.setId(-new Date().getTime());
					PersonelView personelSirketView = new PersonelView();
					Personel sirketPer = new Personel();
					sirketPer.setPdksSicilNo(aciklamaMap.get(key));
					personelSirketView.setPdksPersonel(sirketPer);

					TreeNodeImpl<PersonelView> sirketImpl = new TreeNodeImpl<PersonelView>();
					TreeNodeImpl<PersonelView> yoneticiYokNode = new TreeNodeImpl<PersonelView>();
					yoneticiYokNode.setData(yoneticiSirketView);
					rootNodeForAllPersonelView.addChild(key, sirketImpl);
					sirketImpl.setData(personelSirketView);
					List<Long> yoneticiList = new ArrayList<Long>(perMap.keySet());
					HashMap<Long, TreeNode<PersonelView>> nodeMap = new HashMap<Long, TreeNode<PersonelView>>();
					for (Iterator iterator = yoneticiList.iterator(); iterator.hasNext();) {
						Long yoneticiId = (Long) iterator.next();
						PersonelView personelYonetici = personelDataMap.get(yoneticiId);
						perMap.remove(yoneticiId);
						if (personelYonetici.getOrganizasyonDurum() == false) {
							iterator.remove();
							continue;
						}
						TreeNode<PersonelView> nodeImpl = new TreeNodeImpl<PersonelView>();
						nodeImpl.setData(personelYonetici);
						List list2 = personelYonetici.getAltPersoneller();
						if (!list2.isEmpty())
							loadTree(nodeImpl, personelYonetici, nodeMap, perDataMap);
						perDataMap.put(yoneticiId, personelYonetici);
						if (personelYonetici.getUstPersonelView() == null) {
							nodeMap.put(yoneticiId, nodeImpl);
						}
					}
					int sayac = 0;
					while (!yoneticiList.isEmpty() && sayac < 200) {
						sayac++;
						for (Iterator iterator = yoneticiList.iterator(); iterator.hasNext();) {
							Long id = (Long) iterator.next();
							PersonelView personelYonetici = personelDataMap.get(id);
							TreeNode<PersonelView> nodeYonetici = nodeMap.get(personelYonetici.getPdksPersonelId());
							if (nodeYonetici == null) {
								iterator.remove();
								continue;
							}
							if (nodeYonetici.getParent() == null) {
								if (personelYonetici.getUstPersonelView() != null) {
									Long ustYoneticiId = personelYonetici.getUstPersonelView().getPdksPersonelId();
									TreeNode<PersonelView> nodeUstYonetici = nodeMap.get(ustYoneticiId);
									if (nodeUstYonetici != null)
										nodeUstYonetici.addChild(id, nodeYonetici);

								} else {
									if (personelYonetici.getPdksPersonel().getYoneticisi() == null) {
										if (yoneticiYokNode.getParent() == null)
											sirketImpl.addChild(yoneticiYokNode.getData().getId(), yoneticiYokNode);

										yoneticiYokNode.addChild(id, nodeYonetici);
									} else if (sayac > 3)
										sirketImpl.addChild(id, nodeYonetici);

								}

							}
							if (nodeYonetici != null && nodeYonetici.getParent() != null) {
								iterator.remove();
								continue;
							}
						}
					}
					sortTree(sirketImpl);

				}
			}
			personelDataMap = null;
			sirketMap = null;
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

	}

	/**
	 * @param parentNode
	 */
	private void sortTree(TreeNode<PersonelView> parentNode) {
		List<Liste> list = new ArrayList<Liste>();
		Iterator<Map.Entry<java.lang.Object, TreeNode<PersonelView>>> iterable = parentNode.getChildren();
		while (iterable.hasNext()) {
			Map.Entry<Object, TreeNode<PersonelView>> map = (Map.Entry<Object, TreeNode<PersonelView>>) iterable.next();
			TreeNode<PersonelView> child = map.getValue();
			Object key = map.getKey();
			Liste liste = new Liste(key, child);
			PersonelView personelView = child.getData();
			Personel personel = personelView != null ? personelView.getPdksPersonel() : null;
			String alan = null;
			try {
				alan = personel != null && personel.getId() != null ? personel.getBolumOzelAciklama() + "_" + (personel.getCalismaModeli() != null ? personel.getCalismaModeli().getAciklama() : "") + "_" + personel.getAdSoyad() + "_" + personel.getPdksSicilNo() : personelView.getAdSoyad();
			} catch (Exception e) {
				alan = personelView.getAdSoyad();
			}
			liste.setSelected((personelView.getPdksPersonelId() != null ? (personelView.getAltPersoneller().size() > 0 ? "0" : "1") + "_" + alan : "") + "_" + personelView.getId());
			list.add(liste);
			if (!personelView.getAltPersoneller().isEmpty())
				sortTree(child);
		}
		if (list.size() > 1) {
			for (Liste liste : list)
				parentNode.removeChild(liste.getId());
			list = PdksUtil.sortObjectStringAlanList(list, "getSelected", null);
			for (Liste liste : list) {
				TreeNode<PersonelView> child = (TreeNode<PersonelView>) liste.getValue();
				parentNode.addChild(liste.getId(), child);
			}

		}
		list = null;
	}

	/**
	 * @param rootNode
	 * @param yonetici
	 * @param nodeMap
	 */
	private void loadTree(TreeNode<PersonelView> parentNode, PersonelView yonetici, HashMap<Long, TreeNode<PersonelView>> nodeMap, LinkedHashMap<Long, PersonelView> perDataMap) {
		try {
			if (!perDataMap.containsKey(yonetici.getPdksPersonelId())) {

				perDataMap.put(yonetici.getPdksPersonelId(), yonetici);
				logger.debug(yonetici.getSicilNo() + " " + yonetici.getAdSoyad() + " yönetici ");
				List<PersonelView> altPersoneller = yonetici.getAltPersoneller();
				nodeMap.put(yonetici.getPdksPersonelId(), parentNode);
				if (parentNode.getParent() == null && yonetici.getUstPersonelView() != null) {
					Long id = yonetici.getUstPersonelView().getPdksPersonelId();
					if (nodeMap.containsKey(id))
						parentNode.setParent(nodeMap.get(id));
				}
				for (Iterator iterator = altPersoneller.iterator(); iterator.hasNext();) {
					PersonelView personelView = (PersonelView) iterator.next();
					if (!personelView.getPdksPersonelId().equals(yonetici.getPdksPersonelId())) {
						List<PersonelView> list = personelView.getAltPersoneller();
						if (!list.isEmpty() || personelView.getPdksPersonel().isCalisiyor()) {
							if (yonetici.getSicilNo().equals("1097"))
								logger.debug(personelView.getSicilNo() + " " + personelView.getAdSoyad());
							TreeNode<PersonelView> nodeImpl = new TreeNodeImpl<PersonelView>();// yeni bir top menu node tanimlanir
							parentNode.addChild(personelView.getPdksPersonelId(), nodeImpl);// yeni node root a eklenir
							nodeImpl.setData(personelView);// yeni node a menu bileseni eklenir.

							if (!list.isEmpty())
								loadTree(nodeImpl, personelView, nodeMap, perDataMap);
						} else
							iterator.remove();

					} else
						iterator.remove();

				}
			}

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			throw new FacesException(e.getMessage(), e);
		}
	}

	/**
	 * 
	 */
	private void fillEkSahaTanim() {
		HashMap sonucMap = ortakIslemler.fillEkSahaTanim(session, Boolean.TRUE, null);
		setEkSahaListMap((HashMap<String, List<Tanim>>) sonucMap.get("ekSahaList"));
		setEkSahaTanimMap((TreeMap<String, Tanim>) sonucMap.get("ekSahaTanimMap"));
		setSirketList((List<Sirket>) sonucMap.get("sirketList"));
		bolumAciklama = (String) sonucMap.get("bolumAciklama");
		departmanAciklama = (String) sonucMap.get("departmanAciklama");
		altBolumAciklama = (String) sonucMap.get("altBolumAciklama");
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String yeniPersonelleriGuncelle() throws Exception {
		List<PersonelView> list = null;
		try {
			list = ortakIslemler.yeniPersonelleriOlustur(null, session);
			session.clear();
			if (list != null && !list.isEmpty())
				fillPersonelTablolar(true);
		} catch (Exception ex) {
			ortakIslemler.loggerErrorYaz(null, ex);
		}
		setTanimsizPersonelList(list);
		yeniPersonelOlustur(true);
		return "";
	}

	/**
	 * @return
	 */
	public String fillPersonelKGSList() {
		personelDurumMap.clear();
		if (gebeSutIzniDurumList == null)
			gebeSutIzniDurumList = new ArrayList<Long>();
		else
			gebeSutIzniDurumList.clear();
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		session.clear();
		yoneticiRolVarmi = ortakIslemler.yoneticiRolKontrol(session);
		manuelTanimla = Boolean.FALSE;
		eskiKullanici = null;
		List<PersonelView> list = null;
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select V.* from " + PersonelKGS.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" left join " + Personel.TABLE_NAME + " Y " + PdksEntityController.getJoinLOCK() + " on Y." + Personel.COLUMN_NAME_KGS_PERSONEL + " = V." + PersonelKGS.COLUMN_NAME_ID);
		String str = " where ";
		if (PdksUtil.hasStringValue(adi)) {
			fields.put("ad1", "%" + adi.trim() + "%");
			fields.put("ad2", "%" + adi.trim() + "%");
			sb.append(str + " (V." + PersonelKGS.COLUMN_NAME_AD + " like :ad1 or Y." + Personel.COLUMN_NAME_AD + " like :ad2 )");
			str = " and ";
		}
		if (PdksUtil.hasStringValue(soyadi)) {
			fields.put("soyad1", "%" + soyadi.trim() + "%");
			fields.put("soyad2", "%" + soyadi.trim() + "%");
			sb.append(str + " (V." + PersonelKGS.COLUMN_NAME_SOYAD + " like :soyad1 or Y." + Personel.COLUMN_NAME_SOYAD + " like :soyad2 )");
			str = " and ";
		}
		if (PdksUtil.hasStringValue(sicilNo)) {
			sicilNo = ortakIslemler.getSicilNo(sicilNo);
			String eqStr = "=";
			if (PdksUtil.getSicilNoUzunluk() != null) {
				fields.put("sicilNo1", sicilNo.trim());
				fields.put("sicilNo2", sicilNo.trim());

			} else {
				eqStr = "like";
				Long sayi = null;
				try {
					sayi = Long.parseLong(sicilNo);
				} catch (Exception e) {
				}
				if (sayi != null && sayi.longValue() > 0) {
					fields.put("sicilNo1", "%" + sicilNo.trim());
					fields.put("sicilNo2", "%" + sicilNo.trim());
				} else {
					fields.put("sicilNo1", sicilNo.trim() + "%");
					fields.put("sicilNo2", sicilNo.trim() + "%");
				}
			}
			sb.append(str + " (V." + PersonelKGS.COLUMN_NAME_SICIL_NO + " " + eqStr + " :sicilNo1 or Y." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " " + eqStr + " :sicilNo2 )");
			str = " and ";
		}

		boolean bos = fields.isEmpty();
		Date bugun = PdksUtil.getDate(new Date());
		if (bos)
			sb.append(str + " V." + PersonelKGS.COLUMN_NAME_DURUM + " = 1 and V." + PersonelKGS.COLUMN_NAME_PERSONEL_ID + " is null");
		List<Long> tesisIdList = null;
		if (authenticatedUser.getYetkiliTesisler() != null && authenticatedUser.getYetkiliTesisler().isEmpty() == false) {
			tesisIdList = new ArrayList<Long>();
			for (Tanim tesis : authenticatedUser.getYetkiliTesisler())
				tesisIdList.add(tesis.getId());

		}
		if (authenticatedUser.isIK_Tesis() && authenticatedUser.getPdksPersonel().getTesis() != null) {
			tesisIdList = new ArrayList<Long>();
			tesisIdList.add(authenticatedUser.getPdksPersonel().getTesis().getId());
		}

		boolean eksahaGoster = Boolean.FALSE;
		try {
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			list = ortakIslemler.getPersonelViewByPersonelKGSList(pdksEntityController.getObjectBySQLList(sb, fields, PersonelKGS.class));
			if (bos) {
				bugun = PdksUtil.getDate(new Date());
				for (Iterator<PersonelView> iterator = list.iterator(); iterator.hasNext();) {
					PersonelView personelView = iterator.next();
					PersonelKGS personelKGS = personelView.getPersonelKGS();
					if (personelKGS != null) {
						if (personelKGS.getKapiSirket() == null || personelKGS.getKapiSirket().getBitTarih().before(bugun))
							iterator.remove();
					}
				}
			}
			if (!authenticatedUser.isAdmin() && !authenticatedUser.isIKAdmin() && authenticatedUser.getDepartman() != null) {
				for (Iterator<PersonelView> iterator = list.iterator(); iterator.hasNext();) {
					PersonelView personelView = iterator.next();
					Personel pdksPersonel = personelView.getPdksPersonel();
					PersonelKGS personelKGS = personelView.getPersonelKGS();
					if (pdksPersonel == null) {
						KapiSirket kapiSirket = personelKGS.getKapiSirket();
						if (kapiSirket != null && (!kapiSirket.getDurum() || kapiSirket.getBitTarih().before(bugun))) {
							iterator.remove();
							continue;

						}
					}
					if (pdksPersonel == null && !personelKGS.getDurum()) {
						iterator.remove();
					} else {
						if (tesisIdList != null) {
							if (pdksPersonel.getTesis() == null || tesisIdList.contains(pdksPersonel.getTesis().getId()) == false) {
								iterator.remove();
								continue;
							}
						}

						Sirket sirket = pdksPersonel != null ? pdksPersonel.getSirket() : null;
						if (pdksPersonel != null && sirket != null && !sirket.getDepartman().getId().equals(authenticatedUser.getDepartman().getId()))
							iterator.remove();
						else if (!personelView.getDurum() && pdksPersonel == null)
							iterator.remove();

					}

				}

			}
			if (!list.isEmpty()) {
				Sirket sirketUser = authenticatedUser.isIKSirket() && authenticatedUser.getPdksPersonel() != null ? authenticatedUser.getPdksPersonel().getSirket() : null;
				if (authenticatedUser.getYetkiliTesisler() != null && authenticatedUser.getYetkiliTesisler().isEmpty() == false)
					sirketUser = null;
				if (authenticatedUser.isAdmin() || authenticatedUser.isIKAdmin() || sirketUser != null) {
					for (Iterator<PersonelView> iterator = list.iterator(); iterator.hasNext();) {
						PersonelView personelView = iterator.next();
						Personel pdksPersonel = personelView.getPdksPersonel();
						PersonelKGS personelKGS = personelView.getPersonelKGS();
						if (pdksPersonel == null) {
							if (!personelKGS.getDurum()) {
								iterator.remove();
								continue;
							} else {
								KapiSirket kapiSirket = personelKGS.getKapiSirket();
								if (kapiSirket != null && (!kapiSirket.getDurum() || kapiSirket.getBitTarih().before(bugun))) {
									iterator.remove();
									continue;
								}
							}

						}
						if (pdksPersonel == null && !personelView.getPersonelKGS().getDurum()) {
							iterator.remove();
						} else {
							Personel ppdksPersonel = personelView.getPdksPersonel();
							if (tesisIdList != null) {
								if (ppdksPersonel.getTesis() == null || tesisIdList.contains(ppdksPersonel.getTesis().getId()) == false) {
									iterator.remove();
									continue;
								}
							}

							Sirket sirket = ppdksPersonel != null ? ppdksPersonel.getSirket() : null;
							if (sirketUser != null && sirket != null && !sirket.getId().equals(sirketUser.getId())) {
								iterator.remove();
								continue;
							}
							if (!eksahaGoster && sirket != null)
								eksahaGoster = sirket.getDepartman().isAdminMi();
						}
					}
				}
				if (eksahaGoster)
					fillEkSahaTanim();

				list = PdksUtil.sortObjectStringAlanList(null, list, "getAdSoyad", null);
				TreeMap<String, Boolean> map = mantiksalAlanlariDoldur(list);
				for (String key : map.keySet())
					personelDurumMap.put(key, map.get(key));

				boolean personelTanimSorgula = false;
				if (personelKGSMap == null)
					personelKGSMap = new TreeMap<Long, PersonelKGS>();
				else
					personelKGSMap.clear();
				HashMap<Long, PersonelKGS> idMap = new HashMap<Long, PersonelKGS>();

				for (PersonelView personelView : list) {
					boolean personelTanimli = false;
					Personel pdksPersonel = personelView.getPdksPersonel();
					if (pdksPersonel != null && pdksPersonel.getSirket() != null) {
						if (pdksPersonel.getSirket().isGebelikSutIzinVar()) {
							if (pdksPersonel.getCinsiyetBayan())
								gebeSutIzniDurumList.add(pdksPersonel.getId());
						}
						PersonelKGS personelKGS = pdksPersonel.getPersonelKGS();
						if (personelKGS.getKapiSirket() != null && personelKGS.getKapiSirket().getDurum().equals(Boolean.FALSE))
							idMap.put(personelKGS.getId(), personelKGS);
						personelTanimli = pdksPersonel.getSirket().getId() != null;

					}

					if (!personelTanimSorgula)
						personelTanimSorgula = !personelTanimli;

				}
				if (!idMap.isEmpty()) {
					try {
						String birdenFazlaKGSSirketSQL = ortakIslemler.getBirdenFazlaKGSSirketSQL(null, null, session);
						TreeMap<Long, Long> iliskiMap = new TreeMap<Long, Long>();
						List pList = new ArrayList(idMap.keySet());
						String fieldName = "p";
						fields.clear();
						sb = new StringBuffer();
						sb.append("select P." + PersonelKGS.COLUMN_NAME_ID + ", K." + PersonelKGS.COLUMN_NAME_ID + " as REF from " + PersonelKGS.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
						sb.append(" inner join " + PersonelKGS.TABLE_NAME + " K " + PdksEntityController.getJoinLOCK() + " on " + birdenFazlaKGSSirketSQL);
						sb.append(" where P." + PersonelKGS.COLUMN_NAME_ID + " :" + fieldName + " and P." + PersonelKGS.COLUMN_NAME_SICIL_NO + " <>''");
						fields.put(fieldName, pList);
						if (session != null)
							fields.put(PdksEntityController.MAP_KEY_SESSION, session);
						// List<Object[]> perList = pdksEntityController.getObjectBySQLList(sb, fields, null);
						List<Object[]> perList = pdksEntityController.getSQLParamList(pList, sb, fieldName, fields, null, session);
						for (Object[] objects : perList) {
							BigDecimal refId = (BigDecimal) objects[1], id = (BigDecimal) objects[0];
							if (refId.longValue() != id.longValue())
								iliskiMap.put(refId.longValue(), id.longValue());
						}
						if (!iliskiMap.isEmpty()) {
							List idList = new ArrayList<Long>(iliskiMap.keySet());

							List<PersonelKGS> personelKGSList = pdksEntityController.getSQLParamByFieldList(PersonelKGS.TABLE_NAME, PersonelKGS.COLUMN_NAME_ID, idList, PersonelKGS.class, session);

							for (PersonelKGS personelKGS : personelKGSList) {
								if (personelKGS.getKapiSirket().getDurum()) {
									Long id = iliskiMap.get(personelKGS.getId());
									PersonelKGS personelKGS2 = id != null ? idMap.get(id) : null;
									if (personelKGS2 != null && !personelKGS.getKapiSirket().getId().equals(personelKGS2.getKapiSirket().getId()) && personelKGS2.getAdSoyad().equals(personelKGS.getAdSoyad()))
										personelKGSMap.put(id, personelKGS);
								}

							}
						}
						iliskiMap = null;
						sb = new StringBuffer();

					} catch (Exception ex) {
						ex.printStackTrace();
					}

				}
				idMap = null;
				if (personelTanimSorgula) {
					List<Sirket> templist = ortakIslemler.fillSirketList(session, Boolean.TRUE, Boolean.FALSE);
					for (Sirket sirket : templist) {
						if (sirket.getErpDurum().equals(Boolean.FALSE))
							manuelTanimla = Boolean.TRUE;
					}
				}

			}

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}
		if (!ortakIslemler.getParameterKeyHasStringValue("gebeSutIzniDurum"))
			gebeSutIzniDurumList.clear();
		dosyaGuncelleDurum();
		if (!list.isEmpty()) {
			if (ortakIslemler.getParameterKeyHasStringValue(ortakIslemler.getParametrePersonelERPTableView()) || personelERPGuncelleme.equalsIgnoreCase("E")) {
				TreeMap<String, PersonelView> viewMap = new TreeMap<String, PersonelView>();
				for (PersonelView personelView : list) {
					if (personelView.isERPGuncelle() == false) {
						viewMap.put(personelView.getSicilNo(), personelView);
					}
				}
				if (!viewMap.isEmpty()) {
					String tableName = ortakIslemler.getParameterKey(ortakIslemler.getParametrePersonelERPTableView());
					List<PersonelERPDB> erpList = pdksEntityController.getSQLParamByFieldList(tableName, PersonelERPDB.COLUMN_NAME_PERSONEL_NO, new ArrayList(viewMap.keySet()), PersonelERPDB.class, session);
					for (PersonelERPDB personelERPDB : erpList) {
						viewMap.get(personelERPDB.getPersonelNo()).setYeniPersonel(Boolean.TRUE);
					}
					erpList = null;
				}
				viewMap = null;
			}
		}
		tanimsizPersonelList = ortakIslemler.getSelectItemList("personel", authenticatedUser);
		if (list != null && !list.isEmpty())
			tanimsizPersonelList.addAll(list);

		List<String> yeniList = yeniPersonelOlustur(false);
		if (yeniList != null && !tanimsizPersonelList.isEmpty()) {
			List<PersonelView> eskiList = new ArrayList<PersonelView>();
			for (Iterator iterator = tanimsizPersonelList.iterator(); iterator.hasNext();) {
				PersonelView pw = (PersonelView) iterator.next();
				pw.setYeniPersonel(yeniList.contains(pw.getSicilNo()));
				if (pw.isYeniPersonelMi() == false) {
					eskiList.add(pw);
					iterator.remove();
				}
			}
			if (!eskiList.isEmpty())
				tanimsizPersonelList.addAll(eskiList);
			eskiList = null;
		}

		if (tanimsizPersonelList.isEmpty()) {
			gebeIcapSuaDurumMap = new HashMap<Long, List<String>>();
		} else {
			gebeSuaIcapGuncelle();
			List<PersonelView> eskiList = new ArrayList<PersonelView>(), calismayanList = new ArrayList<PersonelView>();
			for (Iterator iterator = tanimsizPersonelList.iterator(); iterator.hasNext();) {
				PersonelView pw = (PersonelView) iterator.next();
				boolean sil = true;
				if (pw.getPdksPersonel() != null && pw.getPdksPersonel().getId() != null) {
					if (pw.getPdksPersonel().isCalisiyor() == false)
						eskiList.add(pw);
					else
						sil = false;
				} else
					calismayanList.add(pw);
				if (sil)
					iterator.remove();
			}
			if (!calismayanList.isEmpty())
				tanimsizPersonelList.addAll(calismayanList);
			if (!eskiList.isEmpty())
				tanimsizPersonelList.addAll(eskiList);
			eskiList = null;
			calismayanList = null;
		}

		return "";
	}

	/**
	 * @param personel
	 * @return
	 */
	public boolean getIsAramaIzinDurum(Personel personel) {
		double isAramaIzniSaat = 0.0d;
		if (personel != null && personel.getIsAramaGunlukSaat() > 0.0d)
			isAramaIzniSaat = personel.getIsAramaGunlukSaat();
		boolean isAramaIzni = isAramaIzniSaat > 0.0d;
		return isAramaIzni;
	}

	/**
	 * @param list
	 */
	private TreeMap<String, Boolean> mantiksalAlanlariDoldur(List<PersonelView> list) {
		TreeMap<String, Boolean> map = ortakIslemler.mantiksalAlanlariDoldur(list, bakiyeTakipEdiliyor);
		fazlaMesaiIzinKullan = map.containsKey("fazlaMesaiIzinKullan");
		istenAyrilmaGoster = map.containsKey("istenAyrilmaGoster");
		fazlaMesaiOde = map.containsKey("fazlaMesaiOde");
		sanalPersonel = map.containsKey("sanalPersonel");
		kullaniciPersonel = map.containsKey("kullaniciPersonel");
		izinKartiVardir = map.containsKey("izinKartiVardir");
		gebeMi = map.containsKey("gebeMi");
		sutIzni = map.containsKey("sutIzni");
		kimlikNoGoster = map.containsKey("kimlikNoGoster");
		ustYonetici = map.containsKey("ustYonetici");
		icapDurum = map.containsKey("icapDurum");
		partTimeDurum = map.containsKey("partTimeDurum");
		tesisDurum = map.containsKey("tesisDurum");
		egitimDonemi = map.containsKey("egitimDonemi");
		suaOlabilir = map.containsKey("suaOlabilir");
		emailCCDurum = map.containsKey("emailCCDurum");
		emailBCCDurum = map.containsKey("emailBCCDurum");
		departmanGoster = map.containsKey("departmanGoster");
		personelTipiGoster = map.containsKey("personelTipiGoster");
		kartNoGoster = map.containsKey("kartNoGoster");
		if (bakiyeTakipEdiliyor)
			map.put("bakiyeTakipEdiliyor", bakiyeTakipEdiliyor);
		try {
			if (dinamikPersonelDurumList == null)
				dinamikPersonelDurumList = new ArrayList<PersonelDinamikAlan>();
			if (dinamikPersonelSayisalList == null)
				dinamikPersonelSayisalList = new ArrayList<PersonelDinamikAlan>();
			if (dinamikPersonelTanimList == null)
				dinamikPersonelTanimList = new ArrayList<PersonelDinamikAlan>();

			if (dinamikPersonelAciklamaMap == null)
				dinamikPersonelAciklamaMap = new HashMap<Long, List<Tanim>>();
			if (dinamikDurumList == null)
				dinamikDurumList = new ArrayList<Tanim>();
			else
				dinamikDurumList.clear();
			if (dinamikSayisalList == null)
				dinamikSayisalList = new ArrayList<Tanim>();
			else
				dinamikSayisalList.clear();

			if (personelDinamikMap == null)
				personelDinamikMap = new TreeMap<String, PersonelDinamikAlan>();
			else
				personelDinamikMap.clear();
			if (list != null && !list.isEmpty()) {
				List<Long> perIdList = new ArrayList<Long>();
				for (PersonelView personelView : list) {
					if (personelView.getPdksPersonel() != null && personelView.getPdksPersonel().getId() != null)
						perIdList.add(personelView.getPdksPersonel().getId());
				}
				if (!perIdList.isEmpty())
					getPersonelDinamikMap(perIdList);

				if (!personelDinamikMap.isEmpty()) {
					dinamikTanimList = ortakIslemler.getPersonelTanimList(Tanim.TIPI_PERSONEL_DINAMIK_TANIM, session);
					dinamikDurumList = ortakIslemler.getPersonelTanimList(Tanim.TIPI_PERSONEL_DINAMIK_DURUM, session);
					dinamikSayisalList = ortakIslemler.getPersonelTanimList(Tanim.TIPI_PERSONEL_DINAMIK_SAYISAL, session);
				} else {
					if (dinamikTanimList == null)
						dinamikTanimList = new ArrayList<Tanim>();
					else
						dinamikTanimList.clear();
					if (dinamikDurumList == null)
						dinamikDurumList = new ArrayList<Tanim>();
					else
						dinamikDurumList.clear();
					if (dinamikSayisalList == null)
						dinamikSayisalList = new ArrayList<Tanim>();
					else
						dinamikSayisalList.clear();
				}

			}
		} catch (Exception ex) {
			logger.error(ex);
			ex.printStackTrace();
		}
		return map;
	}

	/**
	 * @param object
	 */
	private void getPersonelDinamikMap(Object object) {
		personelDinamikMap.clear();
		List<PersonelDinamikAlan> personelDinamikAlanList = pdksEntityController.getSQLParamByFieldList(PersonelDinamikAlan.TABLE_NAME, PersonelDinamikAlan.COLUMN_NAME_PERSONEL, object, PersonelDinamikAlan.class, session);
		for (PersonelDinamikAlan personelDinamikAlan : personelDinamikAlanList)
			personelDinamikMap.put(personelDinamikAlan.getKey(), personelDinamikAlan);

	}

	/**
	 * 
	 */
	public void fillPdksPersonelList() {
		fillEkSahaTanim();
		Date bugun = PdksUtil.getDate(Calendar.getInstance().getTime());
		Personel pdksPersonel = getInstance();
		parentBordroTanim = null;
		if (pdksPersonel.getSirket().isErp())
			parentBordroTanim = ortakIslemler.getEkSaha4(pdksPersonel.getSirket(), null, session);

		izinGirisDurum(pdksPersonel);
		Departman departman = null;
		Sirket sirket = pdksPersonel.getSirket();
		try {
			departman = pdksPersonel != null && sirket != null ? sirket.getDepartman() : null;
		} catch (Exception e) {
			departman = null;
		}
		bolumDepartmanlari = departman != null && !departman.isAdminMi() ? ortakIslemler.getBolumDepartmanlari(departman, session) : null;
		gorevDepartmanlari = departman != null && !departman.isAdminMi() ? ortakIslemler.getGorevDepartmanlari(departman, session) : null;
		if (departman != null)
			bakiyeIzinGoster = getBakiyeDeparmanIzinDurum(departman);

		if (pdksPersonel.getId() == null && sirket != null) {
			if (sirket.getPdks()) {
				pdksPersonel.setPdks(sirket.getPdks());
				if (sirket.getFazlaMesaiOde())
					pdksPersonel.setFazlaMesaiOde(sirket.getFazlaMesaiOde());
			} else {
				pdksPersonel.setPdks(Boolean.FALSE);
				pdksPersonel.setFazlaMesaiOde(Boolean.FALSE);
			}
		}
		List<Personel> list = ortakIslemler.getTaseronYoneticiler(session);
		TreeMap<String, Boolean> map1 = getOnaysizIzinDurumMap(sirket);
		onaysizIzinKullanilir = map1.containsKey("onaysizIzinDurum");
		ikinciYoneticiIzinOnayla = map1.containsKey("ikinciYoneticiIzinOnaySec");
		if (pdksPersonel.getId() == null && sirket.isErp() == false)
			pdksPersonel.setOnaysizIzinKullanilir(map1.containsKey("onaysizIzinDurum"));
		try {

			HashMap parametreMap = new HashMap();
			parametreMap.put(PdksEntityController.MAP_KEY_SELECT, "user.pdksPersonel");
			parametreMap.put("user.pdksPersonel.sskCikisTarihi>=", bugun);
			parametreMap.put("user.pdksPersonel.iseBaslamaTarihi<=", bugun);
			parametreMap.put("user.pdksPersonel.durum=", Boolean.TRUE);
			parametreMap.put("user.durum=", Boolean.TRUE);
			if (pdksPersonel.getId() != null)
				parametreMap.put("user.pdksPersonel.id<>", pdksPersonel.getId());
			parametreMap.put("role.rolename=", Role.TIPI_YONETICI_KONTRATLI);
			if (session != null)
				parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Personel> yoneticiDigerlist = pdksEntityController.getObjectByInnerObjectListInLogic(parametreMap, UserRoles.class);
			if (!yoneticiDigerlist.isEmpty()) {
				for (Iterator iterator = yoneticiDigerlist.iterator(); iterator.hasNext();) {
					Personel personel = (Personel) iterator.next();
					boolean ekle = Boolean.TRUE;
					for (Personel pdksPersonel2 : list) {
						if (pdksPersonel2.getId().equals(personel.getId())) {
							ekle = Boolean.FALSE;
							break;
						}

					}
					if (!ekle)
						iterator.remove();

				}
				if (!yoneticiDigerlist.isEmpty())
					list.addAll(yoneticiDigerlist);
			}
			if (list.size() > 1)
				list = (ArrayList<Personel>) PdksUtil.sortObjectStringAlanList(null, list, "getAdSoyad", null);

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}

		setPdksPersonelList(list);
		fillPdksVardiyaSablonList();
		fillPdksCalismaModeliList();
		cinsiyetDegisti();
		ekSahaDisable();
	}

	/**
	 * @param personelDepartman
	 * @return
	 */
	private boolean getBakiyeDeparmanIzinDurum(Departman personelDepartman) {
		boolean durum = ortakIslemler.getParameterKeyHasStringValue("bakiyeIzinGoster");
		if (durum && personelDepartman != null) {
			HashMap parametreMap = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select TOP 1 U.* from " + IzinTipi.TABLE_NAME + " U " + PdksEntityController.getSelectLOCK() + " ");
			sb.append("  inner join " + Tanim.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + Tanim.COLUMN_NAME_ID + " = U." + IzinTipi.COLUMN_NAME_IZIN_TIPI);
			sb.append(" and T." + Tanim.COLUMN_NAME_KODU + " = :k");
			sb.append(" where U." + IzinTipi.COLUMN_NAME_DEPARTMAN + " = :d and U." + IzinTipi.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" and U." + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + " is null");
			parametreMap.put("d", personelDepartman.getId());
			parametreMap.put("k", IzinTipi.YILLIK_UCRETLI_IZIN);
			if (session != null)
				parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<IzinTipi> list = pdksEntityController.getObjectBySQLList(sb, parametreMap, IzinTipi.class);
			IzinTipi tipi = list != null && !list.isEmpty() ? list.get(0) : null;

			durum = tipi != null && !tipi.getPersonelGirisTipi().equals(IzinTipi.GIRIS_TIPI_YOK);
		}
		return durum;
	}

	/**
	 * @param sirket
	 * @return
	 */
	private TreeMap<String, Boolean> getOnaysizIzinDurumMap(Sirket sirket) {
		TreeMap<String, Boolean> dataMap = new TreeMap<String, Boolean>();
		boolean onaysizIzinSec = false, onaysizIzinDurum = false, ikinciYoneticiIzinOnayla = false;
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		StringBuffer sb = new StringBuffer("SP_SIRKET_IZIN_ONAY_BILGI");
		try {
			long sirketId = sirket != null ? sirket.getIdLong() : 0L;
			map.put("sirketId", sirketId);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Object[]> list = pdksEntityController.execSPList(map, sb, null);
			if (list.size() == 1) {
				Object[] dizi = list.get(0);
				onaysizIzinSec = ((Integer) dizi[1]) > 0;
				ikinciYoneticiIzinOnayla = ((Integer) dizi[2]) > 0;
				onaysizIzinDurum = ((Integer) dizi[3]) == 1;
			}

		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		if (onaysizIzinSec)
			dataMap.put("onaysizIzinSec", onaysizIzinSec);
		if (onaysizIzinDurum)
			dataMap.put("onaysizIzinDurum", onaysizIzinDurum);
		if (ikinciYoneticiIzinOnayla)
			dataMap.put("ikinciYoneticiIzinOnaySec", ikinciYoneticiIzinOnayla);
		return dataMap;

	}

	/**
	 * @param sirket
	 */
	protected List<IzinTipi> getOnaysizIzinDurum(Sirket sirket) {
		onaysizIzinKullanilir = Boolean.FALSE;
		HashMap fields = new HashMap();
		if (sirket != null)
			fields.put("departman.id=", sirket.getDepartman().getId());
		fields.put("personelGirisTipi<>", IzinTipi.GIRIS_TIPI_YOK);
		fields.put("bakiyeIzinTipi=", null);
		fields.put("durum=", Boolean.TRUE);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<IzinTipi> izinTipiList = pdksEntityController.getObjectByInnerObjectListInLogic(fields, IzinTipi.class);
		for (IzinTipi izinTipi : izinTipiList) {
			if (izinTipi.getOnaylayanTipi().equals(IzinTipi.ONAYLAYAN_TIPI_YOK))
				onaysizIzinKullanilir = Boolean.TRUE;

		}

		return izinTipiList;
	}

	/**
	 * @param user
	 * @return
	 */
	private String userMenuList(User user) {
		menuItemTimeList = null;
		if (user != null && user.getId() != null) {
			List<UserMenuItemTime> list = pdksEntityController.getSQLParamByFieldList(UserMenuItemTime.TABLE_NAME, UserMenuItemTime.COLUMN_NAME_USER, user.getId(), UserMenuItemTime.class, session);
			if (!list.isEmpty())
				menuItemTimeList = PdksUtil.sortListByAlanAdi(list, "lastTime", Boolean.TRUE);
		}

		return "";
	}

	/**
	 * 
	 */
	private void fillMasrafYeriTanimList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_ERP_MASRAF_YERI, session);
		setMasrafYeriList(tanimList);
	}

	/**
	 * 
	 */
	private void fillVardiyaGirisTipiList() {
		List<Tanim> girisTipiList = ortakIslemler.getTanimList(Tanim.TIPI_GIRIS_TIPI, session);
		setVardiyaGirisTipiTanimList(girisTipiList);
	}

	/**
	 * 
	 */
	private void fillYoneticiVardiyaTipiTanimList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_YONETICI_VARDIYA, session);
		setYoneticiVardiyaTipiList(tanimList);
	}

	/**
	 * 
	 */
	private void fillGorevTipiTanimList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_GOREV_TIPI, session);
		setUnvanTanimList(tanimList);
	}

	/**
	 * 
	 */
	private void fillTesisTanimList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_TESIS, session);
		setTesisList(tanimList);
	}

	/**
	 * 
	 */
	public void fillDepartmanTanimList() {
		List<Departman> list = ortakIslemler.fillDepartmanTanimList(session);
		departmanKullaniciList = new ArrayList<Departman>(list);
		taseronKulaniciTanimla = ortakIslemler.getParameterKey("taseronKulaniciTanimla").equals("1");
		if (departmanKullaniciList.size() > 1 && !taseronKulaniciTanimla) {
			for (Iterator iterator = departmanKullaniciList.iterator(); iterator.hasNext();) {
				Departman departman = (Departman) iterator.next();
				if (!departman.getAdmin())
					iterator.remove();
			}
		}
		setDepartmanTanimList(list);
	}

	/**
	 * 
	 */
	private void fillCinsiyetList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_CINSIYET, session);
		setCinsiyetList(tanimList);
	}

	/**
	 * 
	 */
	private void fillPersonelTipList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_PERSONEL_TIPI, session);
		setPersonelTipleri(tanimList);
	}

	/**
	 * 
	 */
	private void fillDepartmanPDKSTanimList() {
		List<Tanim> tanimList = ortakIslemler.getTanimList(Tanim.TIPI_PDKS_DEPARTMAN, session);
		setDepartmanPDKSTanimList(tanimList);
	}

	/**
	 * 
	 */
	public void instanceRefresh() {
		Personel pdksPersonel = getInstance();
		if (pdksPersonel != null && pdksPersonel.getId() != null) {
			try {
				if (pdksPersonel.getKullanici() != null && pdksPersonel.getKullanici().getId() != null)
					session.refresh(pdksPersonel.getKullanici());
				session.refresh(pdksPersonel);
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());

			}
		} else {
			personelView.setPdksPersonel(null);
			personelView.setKullanici(null);
		}

	}

	/**
	 * 
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		tanimsizPersonelList = ortakIslemler.getSelectItemList("personel", authenticatedUser);
		fazlaMesaiIzinKullan = Boolean.FALSE;
		yeniPersonelGuncelle = Boolean.FALSE;
		bakiyeIzinGoster = Boolean.FALSE;
		fazlaMesaiOde = Boolean.FALSE;
		sanalPersonel = Boolean.FALSE;
		kullaniciPersonel = Boolean.FALSE;
		sutIzni = Boolean.FALSE;
		gebeMi = Boolean.FALSE;
		ustYonetici = Boolean.FALSE;
		icapDurum = Boolean.FALSE;
		partTimeDurum = Boolean.FALSE;
		tesisDurum = Boolean.FALSE;
		egitimDonemi = Boolean.FALSE;
		suaOlabilir = Boolean.FALSE;
		emailCCDurum = Boolean.FALSE;
		emailBCCDurum = Boolean.FALSE;
		tesisYetki = Boolean.FALSE;
		bolumYetki = Boolean.FALSE;
		ikinciYoneticiManuelTanimla = Boolean.FALSE;
		eskiKullanici = null;
		if (dinamikDurumList == null)
			dinamikDurumList = new ArrayList<Tanim>();
		else
			dinamikDurumList.clear();
		if (dinamikSayisalList == null)
			dinamikSayisalList = new ArrayList<Tanim>();
		else
			dinamikSayisalList.clear();

		if (personelERPList == null)
			personelERPList = new ArrayList<PersonelERP>();
		else
			personelERPList.clear();

		personelERPGuncelleme = ortakIslemler.getParameterKey("personelERPOku");
		sanalPersonelAciklama = ortakIslemler.sanalPersonelAciklama();
		setPdks(Boolean.TRUE);

		setAdi("");
		setSoyadi("");
		setSicilNo("");
		setHataMesaj("");
		personelDosya.setDosyaIcerik(null);
		setTanimsizPersonelList(new ArrayList<PersonelView>());
		Personel personel = Personel.newpdksPersonel();
		PersonelIzin izin = new PersonelIzin();
		izin.setIzinSuresi(0D);
		personel.setPersonelIzin(izin);
		boolean tableERPOku = ortakIslemler.getParameterKeyHasStringValue(ortakIslemler.getParametrePersonelERPTableView());
		updateValue = false;
		if ((authenticatedUser.isIK() || authenticatedUser.isAdmin() || authenticatedUser.isSistemYoneticisi()))
			updateValue = tableERPOku || ortakIslemler.getParameterKeyHasStringValue(PersonelERPGuncelleme.PARAMETER_KEY + "Update");
		dosyaGuncelleDurum();
		yeniPersonelOlustur(true);

	}

	/**
	 * 
	 */
	private void organizasyonDurumSorgula() {
		String organizasyonSemasiGosterStr = ortakIslemler.getParameterKey("organizasyonSemasiGoster");
		if (PdksUtil.hasStringValue(organizasyonSemasiGosterStr))
			organizasyonSemasiGoster = (authenticatedUser.isIK() && organizasyonSemasiGosterStr.equalsIgnoreCase("IK")) || organizasyonSemasiGosterStr.equals("1");
		else
			organizasyonSemasiGoster = false;

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String personelERPDBGuncelle() throws Exception {
		try {
			ortakIslemler.personelERPDBGuncelle(true, null, session);
			yeniPersonelOlustur(true);
		} catch (Exception ex) {
			try {
				ortakIslemler.loggerErrorYaz(authenticatedUser.getCalistigiSayfa(), ex);
			} catch (Exception e) {
				PdksUtil.addMessageWarn(e.getLocalizedMessage());
			}

		}

		return "";
	}

	/**
	 * @param organizasyonDurum
	 * @return
	 */
	public List<String> yeniPersonelOlustur(boolean organizasyonDurum) {
		yeniPersonelGuncelle = Boolean.FALSE;
		String key = ortakIslemler.getParametrePersonelERPTableView();
		List<String> list = null;
		if (authenticatedUser.isAdmin() && ortakIslemler.getParameterKeyHasStringValue(key)) {
			StringBuffer sb = new StringBuffer();
			sb.append(" select PS." + PersonelKGS.COLUMN_NAME_SICIL_NO + " from " + PersonelERPDB.VIEW_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + PersonelKGS.TABLE_NAME + " PS " + PdksEntityController.getJoinLOCK() + " on PS." + PersonelKGS.COLUMN_NAME_SICIL_NO + " = D." + PersonelERPDB.COLUMN_NAME_PERSONEL_NO);
			sb.append(" inner join " + KapiSirket.TABLE_NAME + " K " + PdksEntityController.getJoinLOCK() + " on K." + KapiSirket.COLUMN_NAME_ID + " = PS." + PersonelKGS.COLUMN_NAME_KGS_SIRKET + " and PS." + PersonelKGS.COLUMN_NAME_DURUM + " = 1");
			sb.append(" and K." + KapiSirket.COLUMN_NAME_DURUM + " = 1 and K." + KapiSirket.COLUMN_NAME_BIT_TARIH + " > GETDATE()");
			sb.append(" left join " + Sirket.TABLE_NAME + " S " + PdksEntityController.getJoinLOCK() + " on S." + Sirket.COLUMN_NAME_ERP_KODU + " = D." + PersonelERPDB.COLUMN_NAME_SIRKET_KODU);
			sb.append(" left join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_KGS_PERSONEL + " = PS." + PersonelKGS.COLUMN_NAME_ID);
			sb.append(" where P." + Personel.COLUMN_NAME_ID + " is null and COALESCE(S." + Sirket.COLUMN_NAME_DURUM + ",1) = 1 ");
			sb.append(" and PS." + PersonelKGS.COLUMN_NAME_SICIL_NO + " not in ( select " + Personel.COLUMN_NAME_PDKS_SICIL_NO + " from " + Personel.TABLE_NAME + ")");
			HashMap fields = new HashMap();
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			list = pdksEntityController.getObjectBySQLList(sb, fields, null);
			yeniPersonelGuncelle = !list.isEmpty();
			if (organizasyonDurum || yeniPersonelGuncelle == false)
				list = null;
		}

		organizasyonDurumSorgula();
		return list;
	}

	/**
	 * 
	 */
	private void dosyaGuncelleDurum() {
		dosyaGuncellemeYetki = ortakIslemler.getTestDurum() && (authenticatedUser.isAdmin() || authenticatedUser.isSistemYoneticisi() || authenticatedUser.isIK());
		if (dosyaGuncellemeYetki == false) {
			String dosyaGuncellemeYetkiStr = ortakIslemler.getParameterKey("dosyaGuncellemeYetki").trim();
			dosyaGuncellemeYetki = dosyaGuncellemeYetkiStr.equals("1");
		}
	}

	/**
	 * 
	 */
	public String personelDosyaYaz() throws Exception {

		TreeMap<String, PersonelERP> perMap = new TreeMap<String, PersonelERP>();
		for (PersonelERP personelERP : personelERPList) {
			perMap.put(personelERP.getPersonelNo(), personelERP);
			personelERP.setYazildi(null);
		}
		servisCalisti = Boolean.FALSE;
		try {
			personelERPReturnList = null;
			PdksSoapVeriAktar service = ortakIslemler.getPdksSoapVeriAktar();
			personelERPReturnList = service.savePersoneller(personelERPList);
			if (personelERPReturnList != null) {
				List<PersonelERP> personelERPHatasizList = new ArrayList<PersonelERP>();
				for (Iterator iterator = personelERPReturnList.iterator(); iterator.hasNext();) {
					PersonelERP returnERP = (PersonelERP) iterator.next();
					if (perMap.containsKey(returnERP.getPersonelNo())) {
						PersonelERP personelERP = perMap.get(returnERP.getPersonelNo());
						personelERP.setYazildi(returnERP.getYazildi());
						if (returnERP.getYazildi()) {
							personelERPHatasizList.add(returnERP);
							iterator.remove();
						} else {
							personelERP.getHataList().addAll(returnERP.getHataList());
						}
					} else
						returnERP.getHataList().add("İşlem yapılmadı!");
				}
				if (!personelERPReturnList.isEmpty()) {
					personelERPList.clear();
					PdksUtil.addMessageWarn("Hata oluştu!");
				} else
					PdksUtil.addMessageInfo("Güncelleme  başarılı tamamlandı.");

				personelERPHatasizList = null;
			}

			servisCalisti = Boolean.TRUE;
		} catch (Exception e) {
			personelERPReturnList = null;
			logger.error(e);
		}

		return "";
	}

	/**
	 * 
	 */
	public String personelDosyaRestYaz() throws Exception {
		servisCalisti = Boolean.FALSE;
		String adres = ortakIslemler.getParameterKey("pdksWebService");
		if (!PdksUtil.hasStringValue(adres))
			adres = "http://localhost:9080/PdksWebService";

		String url = adres + "/rest/services/savePersoneller";
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		TreeMap<String, PersonelERP> perMap = new TreeMap<String, PersonelERP>();
		for (PersonelERP personelERP : personelERPList) {
			perMap.put(personelERP.getPersonelNo(), personelERP);
			personelERP.setYazildi(null);
		}
		String gsonStr = gson.toJson(personelERPList);
		String strGson = null;
		try {
			strGson = ortakIslemler.getJSONData(url, HttpMethod.POST, gsonStr, null, true);
		} catch (Exception e) {
			strGson = null;
			e.printStackTrace();
		}

		if (strGson != null) {
			servisCalisti = Boolean.TRUE;
			List<LinkedTreeMap<String, Object>> list = gson.fromJson(strGson, List.class);
			if (list != null)
				for (LinkedTreeMap<String, Object> linkedHashMap : list) {
					gsonStr = gson.toJson(linkedHashMap);
					PersonelERP returnERP = gson.fromJson(gsonStr, PersonelERP.class);
					if (perMap.containsKey(returnERP.getPersonelNo())) {
						PersonelERP personelERP = perMap.get(returnERP.getPersonelNo());
						personelERP.setYazildi(returnERP.getYazildi());
						personelERP.getHataList().addAll(returnERP.getHataList());
					}
				}
		}
		logger.info(strGson);

		// String strGson1 = ortakIslemler.getURLJSONData(true, url, HttpMethod.POST, null, true, null);

		return "";
	}

	/**
	 * @param alanAdi
	 * @param alanMap
	 * @return
	 */
	private Integer getAlanColNo(String alanAdi, HashMap<String, Integer> alanMap) {
		int colNo = alanMap.containsKey(alanAdi) ? alanMap.get(alanAdi) : -1;
		if (!alanAdi.equals("SICIL_NO") && COL_SICIL_NO < 0)
			colNo = -1;
		return colNo;
	}

	/**
	 * @param deger
	 * @param map
	 */
	private void setKodAciklama(String deger, LinkedHashMap<String, String> map) {
		if (map == null)
			map = new LinkedHashMap<String, String>();
		else
			map.clear();
		int yer = deger.indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA);
		if (yer > 0) {
			String kod = deger.substring(0, yer).trim();
			String aciklama = deger.substring(yer + 1).trim();
			if (PdksUtil.hasStringValue(kod))
				map.put("kod", kod);
			if (PdksUtil.hasStringValue(aciklama))
				map.put("aciklama", aciklama);
		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String personelDosyaOkuBasla() throws Exception {
		session.clear();
		servisBilgileriOlustur();
		return "";

	}

	/**
	 * 
	 */
	private void servisBilgileriOlustur() {
		List<Tanim> personelTransferList = ortakIslemler.getTanimList(Tanim.TIPI_ERP_PERSONEL_ALAN, session);
		dosyaTanimList.clear();
		HashMap<Long, Liste> map = new HashMap<Long, Liste>();
		long bas = -1, son = -1;
		for (Tanim tanim : personelTransferList) {
			long sira = -1;
			try {
				if (PdksUtil.hasStringValue(tanim.getErpKodu())) {
					sira = Long.parseLong(tanim.getErpKodu().trim());
					if (sira > son)
						son = sira;
					if (bas < 0 || sira < bas)
						bas = sira;

				}

			} catch (Exception e) {
				sira = -1;
			}
			if (sira >= 0L && !map.containsKey(sira)) {
				Liste liste = new Liste(sira, tanim);
				map.put(sira, liste);
			}

		}
		if (!map.isEmpty()) {
			for (long i = 0; i <= son; i++) {
				Liste liste = map.get(i);
				if (liste == null) {
					Tanim tanim = new Tanim();
					tanim.setAciklamatr((i + 1) + ". sutün boş ");
					liste = new Liste(i, tanim);

				}

				dosyaTanimList.add(liste);
			}
		} else
			PdksUtil.addMessageAvailableWarn("Dosya transfer alanları tanımsız!");
		map = null;
	}

	/**
	 * @param anaMap
	 * @param veriTip
	 * @param key
	 * @param deger
	 */
	private void kontrolDosyaYaz(LinkedHashMap<String, TreeMap<String, LinkedHashMap<String, Liste>>> anaMap, String veriTip, String key, String deger) {
		if (PdksUtil.hasStringValue(veriTip)) {
			if (PdksUtil.hasStringValue(key) && deger != null) {
				veriTip = veriTip.trim();

				deger = deger.trim();
				TreeMap<String, LinkedHashMap<String, Liste>> map = anaMap.containsKey(veriTip) ? anaMap.get(veriTip) : new TreeMap<String, LinkedHashMap<String, Liste>>();
				if (map.isEmpty())
					anaMap.put(veriTip, map);
				if (transferAciklamaCiftKontrol) {
					String key1 = (sirketKodu != null ? sirketKodu.trim() + PdksUtil.SEPARATOR_KOD_ACIKLAMA : "") + deger.trim();
					LinkedHashMap<String, Liste> mapsDeger = map.containsKey(key1) ? map.get(key1) : new LinkedHashMap<String, Liste>();
					if (mapsDeger.isEmpty())
						map.put(key1, mapsDeger);
					if (!mapsDeger.containsKey(key))
						mapsDeger.put(key, new Liste(key, deger));
				}
				String key2 = (sirketKodu != null ? sirketKodu.trim() + PdksUtil.SEPARATOR_KOD_ACIKLAMA : "") + key.trim();
				LinkedHashMap<String, Liste> mapsKey = map.containsKey(key2) ? map.get(key2) : new LinkedHashMap<String, Liste>();
				if (mapsKey.isEmpty())
					map.put(key2, mapsKey);
				if (!mapsKey.containsKey(deger))
					mapsKey.put(deger, new Liste(key, deger));

			}
		}

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String personelDosyaOku() throws Exception {
		servisCalisti = Boolean.FALSE;
		if (personelERPList == null)
			personelERPList = new ArrayList<PersonelERP>();
		else
			personelERPList.clear();
		Workbook wb = ortakIslemler.getWorkbook(personelDosya);
		try {
			if (wb != null) {
				setServisAlanlar();
				Sheet sheet = wb.getSheetAt(0);
				fillEkSahaTanim();
				LinkedHashMap<String, TreeMap<String, LinkedHashMap<String, Liste>>> anaMap = new LinkedHashMap<String, TreeMap<String, LinkedHashMap<String, Liste>>>();
				HashMap<String, String> referansMap = new HashMap<String, String>();
				String REFERANS_SIRKET = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_SIRKET_KODU);
				if (REFERANS_SIRKET == null)
					REFERANS_SIRKET = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_SIRKET_ADI);
				String REFERANS_TESIS = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_TESIS_KODU);
				if (REFERANS_TESIS == null)
					REFERANS_TESIS = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_TESIS_ADI);
				String REFERANS_CINSIYET = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_CINSIYET_KODU);
				if (REFERANS_CINSIYET == null)
					REFERANS_CINSIYET = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_CINSIYET);
				String REFERANS_BOLUM = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BOLUM_KODU);
				if (REFERANS_BOLUM == null)
					REFERANS_BOLUM = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BOLUM_ADI);
				String REFERANS_DEPARTMAN = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_DEPARTMAN_KODU);
				if (REFERANS_DEPARTMAN == null)
					REFERANS_DEPARTMAN = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_DEPARTMAN_ADI);
				String REFERANS_GOREV = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_GOREV_KODU);
				if (REFERANS_GOREV == null)
					REFERANS_GOREV = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_GOREVI);
				String REFERANS_BORDRO_ALT_ALAN = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BORDRO_ALT_ALAN_KODU);
				if (REFERANS_BORDRO_ALT_ALAN == null)
					REFERANS_BORDRO_ALT_ALAN = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BORDRO_ALT_ALAN_ADI);
				String REFERANS_MASRAF_YERI = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_MASRAF_YERI_KODU);
				if (REFERANS_MASRAF_YERI == null)
					REFERANS_MASRAF_YERI = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_MASRAF_YERI_ADI);
				if (REFERANS_SIRKET != null)
					referansMap.put(REFERANS_SIRKET, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_SIRKET_ADI));
				if (REFERANS_TESIS != null)
					referansMap.put(REFERANS_TESIS, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_TESIS_ADI));
				if (REFERANS_CINSIYET != null)
					referansMap.put(REFERANS_CINSIYET, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_CINSIYET));
				if (REFERANS_BOLUM != null)
					referansMap.put(REFERANS_BOLUM, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BOLUM_ADI));
				if (REFERANS_GOREV != null)
					referansMap.put(REFERANS_GOREV, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_GOREVI));
				if (REFERANS_DEPARTMAN != null)
					referansMap.put(REFERANS_DEPARTMAN, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_DEPARTMAN_ADI));
				if (REFERANS_BORDRO_ALT_ALAN != null)
					referansMap.put(REFERANS_BORDRO_ALT_ALAN, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_BORDRO_ALT_ALAN_ADI));
				if (REFERANS_MASRAF_YERI != null)
					referansMap.put(REFERANS_MASRAF_YERI, ExcelUtil.getSheetStringValueTry(sheet, 0, COL_MASRAF_YERI_ADI));
				String REFERANS_PERSONEL_TIPI = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_PERSONEL_TIPI);
				if (REFERANS_PERSONEL_TIPI == null)
					REFERANS_PERSONEL_TIPI = ExcelUtil.getSheetStringValueTry(sheet, 0, COL_PERSONEL_TIPI_ADI);

				String pattern = "dd.MM.yyyy", patternServis = "yyyy-MM-dd";
				String perSicilNo = null;
				List<String> perNoList = new ArrayList<String>();
				String sicilNoUzunlukStr = ortakIslemler.getParameterKey("sicilNoUzunluk");
				int maxTextLength = 0;
				try {
					if (PdksUtil.hasStringValue(sicilNoUzunlukStr))
						maxTextLength = Integer.parseInt(sicilNoUzunlukStr);
				} catch (Exception e) {
					maxTextLength = 0;
				}
				LinkedHashMap<String, PersonelERP> perMap = new LinkedHashMap<String, PersonelERP>();
				LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
				transferAciklamaCiftKontrol = ortakIslemler.getParameterKey("transferAciklamaCiftKontrol").equals("1");
				try {
					for (int row = 1; COL_SICIL_NO >= 0 && row <= sheet.getLastRowNum(); row++) {
						try {
							perSicilNo = ExcelUtil.getSheetStringValueTry(sheet, row, COL_SICIL_NO);
							logger.debug(row + " " + perSicilNo);
							if (!PdksUtil.hasStringValue(perSicilNo))
								break;
							if (maxTextLength > 0 && perSicilNo.trim().length() < maxTextLength)
								perSicilNo = PdksUtil.textBaslangicinaKarakterEkle(perSicilNo.trim(), '0', maxTextLength);
							if (perNoList.contains(perSicilNo))
								continue;
						} catch (Exception e) {

						}
						sirketKodu = null;
						PersonelERP personelERP = new PersonelERP();

						personelERP.setPersonelNo(perSicilNo);
						if (COL_ADI >= 0)
							personelERP.setAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_ADI));
						if (COL_SOYADI >= 0)
							personelERP.setSoyadi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_SOYADI));
						if (COL_SIRKET_ADI >= 0) {
							personelERP.setSirketAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_SIRKET_ADI));
							if (COL_SIRKET_KODU >= 0) {
								personelERP.setSirketKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_SIRKET_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_SIRKET, personelERP.getSirketKodu(), personelERP.getSirketAdi());
							}

							if (personelERP.getSirketKodu() == null && personelERP.getSirketAdi() != null && personelERP.getSirketAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getSirketAdi(), map);
								personelERP.setSirketKodu(map.get("kod"));
								personelERP.setSirketAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_SIRKET, personelERP.getSirketKodu(), personelERP.getSirketAdi());
							}
						}
						if (COL_TESIS_ADI >= 0) {
							personelERP.setTesisAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_TESIS_ADI));
							sirketKodu = personelERP.getSirketKodu();
							if (COL_TESIS_KODU >= 0) {
								personelERP.setTesisKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_TESIS_KODU));
								String tesisKodu = personelERP.getTesisKodu();
								kontrolDosyaYaz(anaMap, REFERANS_TESIS, tesisKodu, personelERP.getTesisAdi());
							}

							if (personelERP.getTesisKodu() == null && personelERP.getTesisAdi() != null && personelERP.getTesisAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getTesisAdi(), map);
								personelERP.setTesisKodu(map.get("kod"));
								String tesisKodu = personelERP.getTesisKodu();
								personelERP.setTesisAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_TESIS, tesisKodu, personelERP.getTesisAdi());
							}
						}
						sirketKodu = null;
						if (COL_DEPARTMAN_ADI >= 0) {
							personelERP.setDepartmanAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_DEPARTMAN_ADI));
							if (COL_DEPARTMAN_KODU >= 0) {
								personelERP.setDepartmanKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_DEPARTMAN_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_DEPARTMAN, personelERP.getDepartmanKodu(), personelERP.getDepartmanAdi());
							}

							if (personelERP.getDepartmanKodu() == null && personelERP.getDepartmanAdi() != null && personelERP.getDepartmanAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getDepartmanAdi(), map);
								personelERP.setDepartmanKodu(map.get("kod"));
								personelERP.setDepartmanAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_DEPARTMAN, personelERP.getDepartmanKodu(), personelERP.getDepartmanAdi());
							}
						}
						if (COL_BOLUM_ADI >= 0) {
							personelERP.setBolumAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_BOLUM_ADI));
							if (COL_BOLUM_KODU >= 0) {
								personelERP.setBolumKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_BOLUM_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_BOLUM, personelERP.getBolumKodu(), personelERP.getBolumAdi());
							}

							if (personelERP.getBolumKodu() == null && personelERP.getBolumAdi() != null && personelERP.getBolumAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getBolumAdi(), map);
								personelERP.setBolumKodu(map.get("kod"));
								personelERP.setBolumAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_BOLUM, personelERP.getBolumKodu(), personelERP.getBolumAdi());
							}
						}

						sirketKodu = personelERP.getTesisKodu();
						if (COL_BORDRO_ALT_ALAN_ADI >= 0) {
							personelERP.setBordroAltAlanAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_BORDRO_ALT_ALAN_ADI));
							if (COL_BORDRO_ALT_ALAN_KODU >= 0) {
								personelERP.setBordroAltAlanKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_BORDRO_ALT_ALAN_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_BORDRO_ALT_ALAN, personelERP.getBordroAltAlanKodu(), personelERP.getBordroAltAlanAdi());
							}
							if (personelERP.getBordroAltAlanKodu() == null && personelERP.getBordroAltAlanAdi() != null && personelERP.getBordroAltAlanAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getBordroAltAlanAdi(), map);
								personelERP.setBordroAltAlanKodu(map.get("kod"));
								personelERP.setBordroAltAlanAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_BORDRO_ALT_ALAN, personelERP.getBordroAltAlanKodu(), personelERP.getBordroAltAlanAdi());
							}
						}

						if (COL_MASRAF_YERI_ADI >= 0) {
							personelERP.setMasrafYeriAdi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_MASRAF_YERI_ADI));
							if (COL_MASRAF_YERI_KODU >= 0) {
								personelERP.setMasrafYeriKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_MASRAF_YERI_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_MASRAF_YERI, personelERP.getMasrafYeriKodu(), personelERP.getMasrafYeriAdi());
							}
							if (personelERP.getMasrafYeriKodu() == null && personelERP.getMasrafYeriAdi() != null && personelERP.getMasrafYeriAdi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getMasrafYeriAdi(), map);
								personelERP.setMasrafYeriKodu(map.get("kod"));
								personelERP.setMasrafYeriAdi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_MASRAF_YERI, personelERP.getMasrafYeriKodu(), personelERP.getMasrafYeriAdi());
							}
						}

						if (COL_PERSONEL_TIPI_ADI >= 0) {
							personelERP.setPersonelTipi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_PERSONEL_TIPI_ADI));
							if (COL_PERSONEL_TIPI >= 0) {
								personelERP.setPersonelTipiKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_PERSONEL_TIPI));
								kontrolDosyaYaz(anaMap, REFERANS_PERSONEL_TIPI, personelERP.getPersonelTipiKodu(), personelERP.getPersonelTipi());
							}
							if (personelERP.getPersonelTipiKodu() == null && personelERP.getPersonelTipi() != null && personelERP.getPersonelTipi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getPersonelTipi(), map);
								personelERP.setPersonelTipiKodu(map.get("kod"));
								personelERP.setPersonelTipi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_PERSONEL_TIPI, personelERP.getPersonelTipiKodu(), personelERP.getPersonelTipi());
							}
						}

						sirketKodu = null;
						if (COL_GOREVI >= 0) {
							personelERP.setGorevi(ExcelUtil.getSheetStringValueTry(sheet, row, COL_GOREVI));
							if (COL_GOREV_KODU >= 0) {
								personelERP.setGorevKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_GOREV_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_GOREV, personelERP.getGorevKodu(), personelERP.getGorevi());
							}

							if (personelERP.getGorevKodu() == null && personelERP.getGorevi() != null && personelERP.getGorevi().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getGorevi(), map);
								personelERP.setGorevKodu(map.get("kod"));
								personelERP.setGorevi(map.get("aciklama"));
								kontrolDosyaYaz(anaMap, REFERANS_GOREV, personelERP.getGorevKodu(), personelERP.getGorevi());
							}
						}
						if (COL_CINSIYET >= 0) {
							personelERP.setCinsiyeti(ExcelUtil.getSheetStringValueTry(sheet, row, COL_CINSIYET));
							if (COL_CINSIYET_KODU >= 0) {
								personelERP.setCinsiyetKodu(ExcelUtil.getSheetStringValueTry(sheet, row, COL_CINSIYET_KODU));
								kontrolDosyaYaz(anaMap, REFERANS_CINSIYET, personelERP.getCinsiyetKodu(), personelERP.getCinsiyeti());
							}

							if (personelERP.getCinsiyetKodu() == null && personelERP.getCinsiyeti() != null && personelERP.getCinsiyeti().indexOf(PdksUtil.SEPARATOR_KOD_ACIKLAMA) > 0) {
								setKodAciklama(personelERP.getCinsiyeti(), map);
								personelERP.setCinsiyetKodu(map.get("kod"));
								personelERP.setCinsiyeti(map.get("aciklama"));

							}
						}
						if (COL_BORDRO_SANAL_PERSONEL >= 0) {
							String sanalPersonel = ExcelUtil.getSheetStringValueTry(sheet, row, COL_BORDRO_SANAL_PERSONEL);
							if (sanalPersonel != null)
								try {
									personelERP.setSanalPersonel(new Boolean(sanalPersonel));
								} catch (Exception e) {
								}
						}

						String yoneticiPerNo = null, yonetici2PerNo = null;
						if (COL_YONETICI_KODU >= 0)
							yoneticiPerNo = ExcelUtil.getSheetStringValueTry(sheet, row, COL_YONETICI_KODU);
						if (PdksUtil.hasStringValue(yoneticiPerNo) && yoneticiPerNo.trim().length() < maxTextLength)
							yoneticiPerNo = PdksUtil.textBaslangicinaKarakterEkle(yoneticiPerNo.trim(), '0', maxTextLength);
						personelERP.setYoneticiPerNo(yoneticiPerNo);

						if (COL_YONETICI2_KODU >= 0)
							yonetici2PerNo = ExcelUtil.getSheetStringValueTry(sheet, row, COL_YONETICI2_KODU);
						if (PdksUtil.hasStringValue(yonetici2PerNo) && yonetici2PerNo.trim().length() < maxTextLength)
							yonetici2PerNo = PdksUtil.textBaslangicinaKarakterEkle(yonetici2PerNo.trim(), '0', maxTextLength);
						personelERP.setYonetici2PerNo(yonetici2PerNo);
						Date izinHakEdisTarihi = null, iseBaslamaTarihi = null, istenAyrilmaTarihi = null, dogumTarihi = null, grubaGirisTarihi = null;
						try {
							try {
								if (COL_KIDEM_TARIHI >= 0)
									izinHakEdisTarihi = ExcelUtil.getSheetDateValueTry(sheet, row, COL_KIDEM_TARIHI, pattern);
							} catch (Exception e) {
								PdksUtil.addMessageWarn(perSicilNo + " kıdem tarihinde sorun var!");

							}
							try {
								if (COL_GRUBA_GIRIS_TARIHI >= 0)
									grubaGirisTarihi = ExcelUtil.getSheetDateValueTry(sheet, row, COL_GRUBA_GIRIS_TARIHI, pattern);
							} catch (Exception e) {
								PdksUtil.addMessageWarn(perSicilNo + " gruba giriş tarihinde sorun var!");

							}

							try {
								if (COL_ISE_BASLAMA_TARIHI >= 0)
									iseBaslamaTarihi = ExcelUtil.getSheetDateValueTry(sheet, row, COL_ISE_BASLAMA_TARIHI, pattern);
							} catch (Exception e) {
								PdksUtil.addMessageWarn(perSicilNo + " işe giriş tarihinde sorun var!");

							}

							try {
								if (COL_ISTEN_AYRILMA_TARIHI >= 0)
									istenAyrilmaTarihi = ExcelUtil.getSheetDateValueTry(sheet, row, COL_ISTEN_AYRILMA_TARIHI, pattern);
							} catch (Exception e) {
								PdksUtil.addMessageWarn(perSicilNo + " işten ayrılma tarihinde sorun var!");

							}
							try {
								if (COL_DOGUM_TARIHI >= 0)
									dogumTarihi = ExcelUtil.getSheetDateValueTry(sheet, row, COL_DOGUM_TARIHI, pattern);
							} catch (Exception e) {
								PdksUtil.addMessageWarn(perSicilNo + " doğum tarihinde  sorun var!");

							}
							personelERP.setIstenAyrilmaTarihi(istenAyrilmaTarihi != null ? PdksUtil.convertToDateString(istenAyrilmaTarihi, patternServis) : null);
							personelERP.setIseGirisTarihi(iseBaslamaTarihi != null ? PdksUtil.convertToDateString(iseBaslamaTarihi, patternServis) : null);
							personelERP.setGrubaGirisTarihi(grubaGirisTarihi != null ? PdksUtil.convertToDateString(grubaGirisTarihi, patternServis) : null);
							personelERP.setDogumTarihi(dogumTarihi != null ? PdksUtil.convertToDateString(dogumTarihi, patternServis) : null);
							personelERP.setKidemTarihi(izinHakEdisTarihi != null ? PdksUtil.convertToDateString(izinHakEdisTarihi, patternServis) : null);
						} catch (Exception ex) {
							ex.printStackTrace();
						}

						perNoList.add(perSicilNo);
						perMap.put(perSicilNo, personelERP);
					}
				} catch (Exception exx) {
					logger.error(exx);
					exx.printStackTrace();
				}

				if (!perNoList.isEmpty()) {
					boolean listeDolu = true;
					for (String veriTip : anaMap.keySet()) {
						if (referansMap.containsKey(veriTip)) {
							StringBuffer sb = new StringBuffer();
							String aciklama = referansMap.get(veriTip);
							TreeMap<String, LinkedHashMap<String, Liste>> veriMap = anaMap.get(veriTip);
							for (String key : veriMap.keySet()) {
								LinkedHashMap<String, Liste> maps = veriMap.get(key);
								if (maps.size() > 1) {
									if (sb.length() > 0)
										sb.append(", ");
									List<Liste> listeler = new ArrayList<Liste>(maps.values());
									Liste liste1 = listeler.get(0), liste2 = listeler.get(1);
									if (liste1.getId().equals(liste2.getId())) {
										listeler = PdksUtil.sortObjectStringAlanList(listeler, "getValue", null);
										sb.append(veriTip + " = \"" + liste1.getId() + "\" : [ ");
										for (Iterator iterator = listeler.iterator(); iterator.hasNext();) {
											Liste liste = (Liste) iterator.next();
											sb.append("\"" + liste.getValue() + "\"");
											if (iterator.hasNext())
												sb.append(", ");
										}
									} else {
										listeler = PdksUtil.sortObjectStringAlanList(listeler, "getId", null);
										sb.append(veriTip + " = \"" + liste1.getValue() + "\" : [ ");
										for (Iterator iterator = listeler.iterator(); iterator.hasNext();) {
											Liste liste = (Liste) iterator.next();
											sb.append("\"" + liste.getId() + "\"");
											if (iterator.hasNext())
												sb.append(", ");
										}
									}
									sb.append(" ] " + maps.size() + " adet ");
								}
							}
							if (sb.length() > 0) {
								PdksUtil.addMessageAvailableWarn(sb.toString() + " farklı \"" + aciklama + "\"  tanımlanmıştır.");

								listeDolu = false;
							}
							sb = null;
						}
					}
					if (listeDolu) {
						String fieldName = "sicilNo";
						List dataIdList = new ArrayList(perMap.keySet());
						session.clear();
						HashMap fields = new HashMap();
						fields.put(fieldName, dataIdList);
						if (session != null)
							fields.put(PdksEntityController.MAP_KEY_SESSION, session);
						TreeMap<String, PersonelKGS> personelKGSMap = ortakIslemler.getParamTreeMap(Boolean.FALSE, "getSicilNo", false, dataIdList, fieldName, fields, PersonelKGS.class, session);
						for (String sicilNo : perNoList) {
							if (personelKGSMap.containsKey(sicilNo))
								personelERPList.add(perMap.get(sicilNo));
						}
					}
				}

			}

		} catch (Exception e) {

		}

		return "";
	}

	public Boolean adviseNodeOpened(UITree tree) {
		// if (disableAdviseNodeOpened)
		// return Boolean.FALSE;
		// Object rowKey = tree.getRowKey();
		// TreeNode<PersonelView> selectedNode = tree.getModelTreeNode(rowKey);
		// PersonelView personelView = (PersonelView) selectedNode.getData();
		// boolean currentlyNodeSelected = personelView.getId() == null || personelView.getId() > 0L;
		// if (currentlyNodeSelected) {
		// return Boolean.TRUE;
		// }
		// return Boolean.FALSE;
		return disableAdviseNodeOpened == null || disableAdviseNodeOpened == false;
	}

	/**
	 * @return
	 */
	private HashMap<String, Integer> setServisAlanlar() {
		HashMap<String, Integer> alanMap = new HashMap<String, Integer>();
		if (authenticatedUser.isAdmin() || dosyaTanimList.isEmpty())
			servisBilgileriOlustur();
		String yonetici2ERPKontrolStr = ortakIslemler.getParameterKey("yonetici2ERPKontrol");
		boolean yonetici2ERPKontrol = yonetici2ERPKontrolStr.equals("1");
		for (Iterator iterator = dosyaTanimList.iterator(); iterator.hasNext();) {
			Liste liste = (Liste) iterator.next();
			try {
				Tanim tanim = (Tanim) liste.getValue();
				if (PdksUtil.hasStringValue(tanim.getErpKodu()))
					alanMap.put(tanim.getKodu(), Integer.parseInt(tanim.getErpKodu().trim()));
			} catch (Exception e) {

			}

		}
		COL_SICIL_NO = getAlanColNo("SICIL_NO", alanMap);
		COL_ADI = getAlanColNo("ADI", alanMap);
		COL_SOYADI = getAlanColNo("SOYADI", alanMap);
		COL_SIRKET_KODU = getAlanColNo("SIRKET_KODU", alanMap);
		COL_SIRKET_ADI = getAlanColNo("SIRKET_ADI", alanMap);
		COL_TESIS_KODU = getAlanColNo("TESIS_KODU", alanMap);
		COL_TESIS_ADI = getAlanColNo("TESIS_ADI", alanMap);
		COL_GOREV_KODU = getAlanColNo("GOREV_KODU", alanMap);
		COL_GOREVI = getAlanColNo("GOREVI", alanMap);
		COL_BOLUM_KODU = getAlanColNo("BOLUM_KODU", alanMap);
		COL_BOLUM_ADI = getAlanColNo("BOLUM_ADI", alanMap);
		COL_ISE_BASLAMA_TARIHI = getAlanColNo("ISE_BASLAMA_TARIHI", alanMap);
		COL_KIDEM_TARIHI = getAlanColNo("KIDEM_TARIHI", alanMap);
		COL_GRUBA_GIRIS_TARIHI = getAlanColNo("GRUBA_GIRIS_TARIHI", alanMap);
		COL_ISTEN_AYRILMA_TARIHI = getAlanColNo("ISTEN_AYRILMA_TARIHI", alanMap);
		COL_DOGUM_TARIHI = getAlanColNo("DOGUM_TARIHI", alanMap);
		COL_CINSIYET_KODU = getAlanColNo("CINSIYET_KODU", alanMap);
		COL_CINSIYET = getAlanColNo("CINSIYET", alanMap);
		COL_YONETICI_KODU = getAlanColNo("YONETICI_KODU", alanMap);
		COL_YONETICI2_KODU = yonetici2ERPKontrol ? getAlanColNo("YONETICI2_KODU", alanMap) : -1;
		COL_DEPARTMAN_KODU = getAlanColNo("DEPARTMAN_KODU", alanMap);
		COL_DEPARTMAN_ADI = getAlanColNo("DEPARTMAN_ADI", alanMap);
		COL_MASRAF_YERI_KODU = getAlanColNo("MASRAF_YERI_KODU", alanMap);
		COL_MASRAF_YERI_ADI = getAlanColNo("MASRAF_YERI_ADI", alanMap);
		COL_BORDRO_ALT_ALAN_KODU = getAlanColNo("BORDRO_ALT_ALAN_KODU", alanMap);
		COL_BORDRO_ALT_ALAN_ADI = getAlanColNo("BORDRO_ALT_ALAN_ADI", alanMap);
		COL_BORDRO_SANAL_PERSONEL = getAlanColNo("SANAL_PERSONEL", alanMap);
		COL_PERSONEL_TIPI = getAlanColNo("PERSONEL_TIPI", alanMap);
		COL_PERSONEL_TIPI_ADI = getAlanColNo("PERSONEL_TIPI_ADI", alanMap);

		return alanMap;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String personelDosyaSifirla() throws Exception {
		servisCalisti = Boolean.FALSE;
		personelERPList.clear();
		personelDosya.setDosyaIcerik(null);
		if (personelERPReturnList == null)
			personelERPReturnList = new ArrayList<PersonelERP>();
		else
			personelERPReturnList.clear();
		return "";
	}

	/**
	 * @param event
	 * @return
	 * @throws Exception
	 */
	public String listenerPersonelDosya(UploadEvent event) throws Exception {
		servisCalisti = Boolean.FALSE;
		UploadItem item = event.getUploadItem();
		PdksUtil.getDosya(item, personelDosya);
		if (personelERPList == null)
			personelERPList = new ArrayList<PersonelERP>();
		else
			personelERPList.clear();
		return "";

	}

	/**
	 * 
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void detaysizSayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		session.setFlushMode(FlushMode.MANUAL);
		session.clear();
		setPdks(Boolean.FALSE);
		setTanimsizPersonelList(new ArrayList<PersonelView>());
		fillGorevTipiTanimList();
	}

	/**
	 * 
	 */
	public void fillDistinctRoleList() {
		Personel personel = getInstance();
		List<String> yoneticiOlmayanRoller = getPersonelYoneticiolmayanRoller(personel);
		List<Role> allRoles = ortakIslemler.yetkiRolleriGetir(session);
		if (authenticatedUser.isAdmin() == false && authenticatedUser.isSistemYoneticisi() == false) {
			for (Iterator iterator = allRoles.iterator(); iterator.hasNext();) {
				Role role = (Role) iterator.next();
				if (role.getStatus().booleanValue() == false)
					iterator.remove();
				else if ((authenticatedUser.isIK_Tesis() || authenticatedUser.isIKSirket()) && role.isIK())
					iterator.remove();
				else if (authenticatedUser.isAdmin() == false && role.isAdminRoleMu())
					iterator.remove();
				else if (role.getDepartman() != null && !departmanKullaniciList.contains(role.getDepartman()))
					iterator.remove();
				else if (yoneticiOlmayanRoller != null && role.isAdminRoleMu() == false) {
					String rolAdi = role.getRolename();
					if (!yoneticiOlmayanRoller.contains(rolAdi))
						iterator.remove();
				}
			}
		}
		if (allRoles.size() > 1)
			allRoles = PdksUtil.sortObjectStringAlanList(allRoles, "getAciklama", null);

		if (personel != null) {
			User seciliKullanici = personel.getKullanici();
			seciliKullanici.setYetkiliRollerim(null);
			ortakIslemler.setUserRoller(seciliKullanici, session);
			if (seciliKullanici.getYetkiliRollerim() != null) {
				for (Iterator iterator = seciliKullanici.getYetkiliRollerim().iterator(); iterator.hasNext();) {
					Role role = (Role) iterator.next();
					if (authenticatedUser.isAdmin() == false && role.isAdminRoleMu()) {
						iterator.remove();
						continue;
					}
					if (allRoles.contains(role))
						allRoles.remove(role);
				}
			}
		}
		if (bosDepartman != null && personel.getId() != null && personel.getSirket().isErp()) {
			if (personel.getEkSaha1() == null || personel.getEkSaha1().getKodu().equals(bosDepartmanKodu)) {
				for (Iterator iterator = allRoles.iterator(); iterator.hasNext();) {
					Role role = (Role) iterator.next();
					if (role.getRolename().equals(Role.TIPI_DIREKTOR_SUPER_VISOR)) {
						iterator.remove();
					}
				}
			}
		}
		yoneticiOlmayanRoller = null;
		setDistinctRoleList(allRoles);
	}

	/**
	 * @param personel
	 * @return
	 */
	private List<String> getPersonelYoneticiolmayanRoller(Personel personel) {
		boolean yoneticiPersonelEngelleDurum = !ortakIslemler.getParameterKey("yoneticiPersonelEngelleDurum").equals("1");
		List<String> yoneticiOlmayanRoller = null;
		if (yoneticiPersonelEngelleDurum && personel != null && personel.getId() != null) {
			yoneticiOlmayanRoller = Arrays.asList(new String[] { Role.TIPI_PERSONEL, Role.TIPI_IK, Role.TIPI_IK_Tesis, Role.TIPI_IK_DIREKTOR, Role.TIPI_GENEL_MUDUR });
			String yoneticiRolleriHaric = ortakIslemler.getParameterKey("yoneticiRolleriHaric");
			if (PdksUtil.hasStringValue(yoneticiRolleriHaric))
				yoneticiOlmayanRoller = PdksUtil.getListByString(yoneticiRolleriHaric, null);
			Date bugun = PdksUtil.getDate(Calendar.getInstance().getTime());
			HashMap parametreMap = new HashMap();
			parametreMap.put("yoneticisi.id=", personel.getId());
			parametreMap.put("iseBaslamaTarihi<=", bugun);
			parametreMap.put("sskCikisTarihi>=", bugun);
			if (session != null)
				parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Personel> list = pdksEntityController.getObjectByInnerObjectListInLogic(parametreMap, Personel.class);
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Personel per = (Personel) iterator.next();
				if (per.getDurum().equals(Boolean.FALSE) || per.getId().equals(personel.getId()) || per.isCalisiyorGun(bugun) == false)
					iterator.remove();

			}

			if (!list.isEmpty())
				yoneticiOlmayanRoller = null;
			list = null;
		}
		return yoneticiOlmayanRoller;
	}

	/**
	 * 
	 */
	public void fillDistinctTesisList() {
		List<Tanim> allTesis = ortakIslemler.getTesisDurumu() ? ortakIslemler.getTanimList(Tanim.TIPI_TESIS, session) : new ArrayList<Tanim>();
		if (authenticatedUser.getYetkiliTesisler() != null && authenticatedUser.getYetkiliTesisler().isEmpty() == false) {
			for (Iterator iterator = allTesis.iterator(); iterator.hasNext();) {
				Tanim tanim = (Tanim) iterator.next();
				boolean sil = true;
				for (Tanim tesis : authenticatedUser.getYetkiliTesisler()) {
					if (tesis.getId().equals(tanim.getId())) {
						sil = false;
						break;
					}
				}
				if (sil)
					iterator.remove();
			}
		} else if (authenticatedUser.isIKSirket()) {
			AramaSecenekleri as = new AramaSecenekleri();
			Date bugun = PdksUtil.getDate(new Date());
			as.setSirket(authenticatedUser.getPdksPersonel().getSirket());
			as.setSirketId(authenticatedUser.getPdksPersonel().getSirket().getId());
			ortakIslemler.setAramaSecenekTesisData(as, bugun, bugun, false, session);
			for (Iterator iterator = allTesis.iterator(); iterator.hasNext();) {
				Tanim tanim = (Tanim) iterator.next();
				boolean sil = true;
				for (SelectItem tesis : as.getTesisList()) {
					if (tesis.getValue().equals(tanim.getId())) {
						sil = false;
						break;
					}
				}
				if (sil)
					iterator.remove();
			}
		}
		Personel seciliPersonel = getInstance();
		if (seciliPersonel != null) {
			User seciliKullanici = seciliPersonel.getKullanici();
			seciliKullanici.setYetkiliTesisler(null);
			ortakIslemler.setUserTesisler(seciliKullanici, session);
			if (seciliKullanici.getYetkiliTesisler() != null) {
				Tanim perTesis = seciliPersonel.getTesis();
				for (Iterator iterator = allTesis.iterator(); iterator.hasNext();) {
					Tanim tesis = (Tanim) iterator.next();
					if (seciliKullanici.getYetkiliTesisler().contains(tesis) || (perTesis != null && perTesis.getId().equals(tesis.getId())))
						iterator.remove();
				}
				if (allTesis.size() > 1)
					allTesis = PdksUtil.sortTanimList(null, allTesis);
			}

		}
		setDistinctTesisList(allTesis);
	}

	/**
	 * 
	 */
	public void fillDistinctBolumList() {
		Personel seciliPersonel = getInstance();
		List<Tanim> bolumList = null;
		if (seciliPersonel != null && seciliPersonel.getSirket() != null) {
			User seciliKullanici = seciliPersonel.getKullanici();
			if (seciliKullanici != null) {
				Sirket sirket = seciliPersonel.getSirket();
				StringBuffer sb = new StringBuffer();
				HashMap fields = new HashMap();
				sb.append("select distinct T.* from " + Personel.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + Tanim.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + Tanim.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_EK_SAHA3);
				if (sirket.getSirketGrup() == null) {
					sb.append(" where P." + Personel.COLUMN_NAME_SIRKET + " = " + sirket.getId() + " and");
				} else {
					sb.append(" inner join " + Sirket.TABLE_NAME + " S " + PdksEntityController.getJoinLOCK() + " on S." + Sirket.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_SIRKET);
					sb.append(" and S." + Sirket.COLUMN_NAME_SIRKET_GRUP + " = " + sirket.getSirketGrup().getId());
					sb.append(" where");
				}
				sb.append(" P." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI + " <= :t1 and P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= :t2");
				Date bugun = PdksUtil.getDate(new Date());
				fields.put("t1", bugun);
				fields.put("t2", bugun);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				bolumList = pdksEntityController.getObjectBySQLList(sb, fields, Tanim.class);
				seciliKullanici.setYetkiliBolumler(null);
				ortakIslemler.setUserBolumler(seciliKullanici, session);

				if (bolumList != null) {
					Tanim perBolum = seciliPersonel.getEkSaha3();
					for (Iterator iterator = bolumList.iterator(); iterator.hasNext();) {
						Tanim bolum = (Tanim) iterator.next();
						if (bolum.getKodu().equals("YOK") || (perBolum != null && perBolum.getId().equals(bolum.getId())) || seciliKullanici.getYetkiliBolumler().contains(bolum))
							iterator.remove();
					}
					if (bolumList.size() > 1)
						bolumList = PdksUtil.sortTanimList(null, bolumList);

				}
			}

		}

		setDistinctBolumList(bolumList);
	}

	/**
	 * @return
	 */
	public String excelServiceAktar() {
		try {
			ByteArrayOutputStream baosDosya = excelServiceAktarDevam(personelList);
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "personelWebServisListesi.xlsx");

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}

		return "";
	}

	/**
	 * @param personelList
	 * @param tanimMap
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private ByteArrayOutputStream excelServiceAktarDevam(List<PersonelView> list) throws Exception {

		Tanim parentBordroTanim = (Tanim) ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_GENEL_TANIM, Tanim.TIPI_BORDRO_ALT_BIRIMI, session);

		String bordroAltAlanStr = "";
		if (parentBordroTanim != null && PdksUtil.hasStringValue(parentBordroTanim.getErpKodu()))
			bordroAltAlanStr = parentBordroTanim.getErpKodu().trim().toLowerCase(PdksUtil.TR_LOCALE);
		HashMap<String, Integer> alanMap = setServisAlanlar();
		List<PersonelView> personelList = new ArrayList<PersonelView>(list);
		for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
			PersonelView personelView = (PersonelView) iterator.next();
			PersonelKGS personelKGS = personelView.getPersonelKGS();
			Sirket sirket = personelView.getPdksPersonel().getSirket();
			try {
				if (personelKGS.getDurum().equals(Boolean.FALSE) || sirket.isErp() == false || sirket.isPdksMi() == false)
					iterator.remove();
			} catch (Exception e) {
			}

		}

		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, "Personel WebService Listesi", false);
		CellStyle header = ExcelUtil.getStyleHeader(wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddDate = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenDate = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb);

		int row = 0;
		int col = 0;
		for (Iterator iterator = dosyaTanimList.iterator(); iterator.hasNext();) {
			Liste liste = (Liste) iterator.next();
			Tanim tanim = (Tanim) liste.getValue();
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(tanim.getAciklama());
		}
		boolean renk = true;
		for (Iterator iter = personelList.iterator(); iter.hasNext();) {
			PersonelView personelView = (PersonelView) iter.next();
			CellStyle style = null, styleCenter = null, styleDate = null;
			if (renk) {
				styleDate = styleOddDate;
				style = styleOdd;
				styleCenter = styleOddCenter;
			} else {
				styleDate = styleEvenDate;
				style = styleEven;
				styleCenter = styleEvenCenter;
			}
			renk = !renk;
			row++;
			col = 0;
			Personel personel = personelView.getPdksPersonel();
			try {
				Sirket sirket = personel.getSirket();
				Tanim cinsiyet = personel.getCinsiyet(), gorev = personel.getGorevTipi(), bolum = personel.getEkSaha3(), personelTipi = personel.getPersonelTipi();
				Tanim masrafYeri = personel.getMasrafYeri(), bordroAltAlani = personel.getBordroAltAlan(), departman = personel.getEkSaha1(), tesis = personel.getTesis();

				if (bordroAltAlanStr.startsWith("eksaha2"))
					bordroAltAlani = personel.getEkSaha2();
				else if (bordroAltAlanStr.startsWith("eksaha4"))
					bordroAltAlani = personel.getEkSaha4();
				for (int i = 0; i < dosyaTanimList.size(); i++) {
					Liste liste = dosyaTanimList.get(i);
					Tanim tanim = (Tanim) liste.getValue();
					String kodu = tanim.getKodu();
					if (alanMap.containsKey(kodu)) {
						if (kodu.equals("SICIL_NO"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSicilNo());
						else if (kodu.equals("ADI"))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAd());
						else if (kodu.equals("SOYADI"))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getSoyad());
						else if (kodu.equals("SIRKET_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(sirket.getErpKodu() != null ? sirket.getErpKodu() : "");
						else if (kodu.equals("SIRKET_ADI")) {
							if (COL_SIRKET_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket.getAd());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue((sirket.getErpKodu() != null ? sirket.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA : "") + sirket.getAd());
						} else if (kodu.equals("TESIS_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(tesis != null ? tesis.getErpKodu() : "");
						else if (kodu.equals("TESIS_ADI")) {
							if (COL_TESIS_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(tesis != null ? tesis.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(tesis != null ? tesis.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + tesis.getAciklama() : "");
						} else if (kodu.equals("GOREV_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(gorev != null ? gorev.getErpKodu() : "");
						else if (kodu.equals("GOREVI")) {
							if (COL_GOREV_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(gorev != null ? gorev.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(gorev != null ? gorev.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + gorev.getAciklama() : "");
						} else if (kodu.equals("BOLUM_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(bolum != null ? bolum.getErpKodu() : "");
						else if (kodu.equals("BOLUM_ADI")) {
							if (COL_BOLUM_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(bolum != null ? bolum.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(bolum != null ? bolum.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + bolum.getAciklama() : "");
						} else if (kodu.equals("CINSIYET_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(cinsiyet != null ? cinsiyet.getErpKodu() : "");
						else if (kodu.equals("CINSIYET")) {
							if (COL_CINSIYET_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(cinsiyet != null ? cinsiyet.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(cinsiyet != null ? cinsiyet.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + cinsiyet.getAciklama() : "");
						} else if (kodu.equals("BORDRO_ALT_ALAN_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(bordroAltAlani != null ? bordroAltAlani.getErpKodu() : "");
						else if (kodu.equals("BORDRO_ALT_ALAN_ADI")) {
							if (COL_BORDRO_ALT_ALAN_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(bordroAltAlani != null ? bordroAltAlani.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(bordroAltAlani != null ? bordroAltAlani.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + bordroAltAlani.getAciklama() : "");
						} else if (kodu.equals("DEPARTMAN_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(departman != null ? departman.getErpKodu() : "");
						else if (kodu.equals("DEPARTMAN_ADI")) {
							if (COL_DEPARTMAN_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(departman != null ? departman.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(departman != null ? departman.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + departman.getAciklama() : "");
						} else if (kodu.equals("MASRAF_YERI_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(masrafYeri != null ? masrafYeri.getErpKodu() : "");
						else if (kodu.equals("MASRAF_YERI_ADI")) {
							if (COL_MASRAF_YERI_KODU >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(masrafYeri != null ? masrafYeri.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(masrafYeri != null ? masrafYeri.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + masrafYeri.getAciklama() : "");
						} else if (kodu.equals("YONETICI_KODU"))
							ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getYoneticisi() != null ? personel.getYoneticisi().getPdksSicilNo() : "");
						else if (kodu.equals("SANAL_PERSONEL"))
							ExcelUtil.getCell(sheet, row, col++, style).setCellValue(String.valueOf(personel.isSanalPersonelMi()));
						else if (kodu.equals("ISE_BASLAMA_TARIHI")) {
							if (personel.getIseBaslamaTarihi() != null)
								ExcelUtil.getCell(sheet, row, col++, styleDate).setCellValue(personel.getIseBaslamaTarihi());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals("KIDEM_TARIHI")) {
							if (personel.getIzinHakEdisTarihi() != null)
								ExcelUtil.getCell(sheet, row, col++, styleDate).setCellValue(personel.getIzinHakEdisTarihi());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals("ISTEN_AYRILMA_TARIHI")) {
							if (personel.getIstenAyrilisTarihi() != null)
								ExcelUtil.getCell(sheet, row, col++, styleDate).setCellValue(personel.getIstenAyrilisTarihi());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals("DOGUM_TARIHI")) {
							if (personel.getDogumTarihi() != null)
								ExcelUtil.getCell(sheet, row, col++, styleDate).setCellValue(personel.getDogumTarihi());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals("GRUBA_GIRIS_TARIHI")) {
							if (personel.getGrubaGirisTarihi() != null)
								ExcelUtil.getCell(sheet, row, col++, styleDate).setCellValue(personel.getGrubaGirisTarihi());
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
						} else if (kodu.equals("PERSONEL_TIPI_ADI")) {
							if (COL_PERSONEL_TIPI >= 0)
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personelTipi != null ? personelTipi.getAciklama() : "");
							else
								ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personelTipi != null ? personelTipi.getErpKodu() + PdksUtil.SEPARATOR_KOD_ACIKLAMA + personelTipi.getAciklama() : "");
						} else
							logger.debug(kodu);
					} else
						ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
				}
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
				logger.info(row + " " + personel.getPdksSicilNo());

			}
		}
		try {

			for (int i = 0; i < dosyaTanimList.size(); i++)
				sheet.autoSizeColumn(i);
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
	 * 
	 */
	private void ekSahaDisable() {
		ekSaha1Disable = false;
		ekSaha2Disable = false;
		ekSaha4Disable = false;
		Personel personel = getInstance();
		boolean admin = authenticatedUser.isAdmin();
		if (admin == false && !personelERPGuncelleme.equals("1") && !personelERPGuncelleme.equals("M")) {
			ekSaha2Disable = true;
			ekSaha4Disable = true;
			if (personel != null && personel.getSirket() != null && personel.getSirket().getDepartman().isAdminMi()) {

				Tanim parentBordroTanim = (Tanim) ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_GENEL_TANIM, Tanim.TIPI_BORDRO_ALT_BIRIMI, session);
				String parentBordroTanimKodu = Tanim.TIPI_BORDRO_ALT_BIRIMI;
				String parentBordroTanimKoduStr = "";
				if (parentBordroTanim != null) {
					parentBordroTanimKoduStr = parentBordroTanim.getErpKodu();
					if (parentBordroTanimKoduStr == null)
						parentBordroTanimKoduStr = "";
					else {
						parentBordroTanimKodu = parentBordroTanimKoduStr.trim();
					}

				}
				parentBordroTanimKoduStr = parentBordroTanimKodu.toLowerCase(Constants.TR_LOCALE);
				ekSaha1Disable = !admin;
				if (admin == false) {
					if (PdksUtil.hasStringValue(parentBordroTanimKoduStr)) {
						if (!parentBordroTanimKoduStr.equals("eksaha2"))
							ekSaha2Disable = false;
						if (!parentBordroTanimKoduStr.equals("eksaha4"))
							ekSaha4Disable = false;
					}
				}

			}

		}
	}

	/**
	 * @param list
	 * @return
	 */
	public String excelAktar(List<PersonelView> list) {
		try {
			if (list == null)
				list = personelList;
			ByteArrayOutputStream baosDosya = ortakIslemler.personelExcelDevam(Boolean.TRUE, list, ekSahaTanimMap, authenticatedUser, personelDinamikMap, bakiyeTakipEdiliyor, session);
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "personelListesi.xlsx");

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}

		return "";
	}

	public List<Role> getDistinctRoleList() {
		return distinctRoleList;
	}

	public void setDistinctRoleList(List<Role> distinctRoleList) {
		this.distinctRoleList = distinctRoleList;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public Date getTarih() {
		return tarih;
	}

	public void setTarih(Date tarih) {
		this.tarih = tarih;
	}

	public Date getBugun() {
		return PdksUtil.buGun();
	}

	public List<VardiyaSablonu> getSablonlar() {
		return sablonlar;
	}

	public void setSablonlar(List<VardiyaSablonu> sablonlar) {
		this.sablonlar = sablonlar;
	}

	public List<Sirket> getSirketList() {
		return sirketList;
	}

	public void setSirketList(List<Sirket> sirketList) {
		this.sirketList = sirketList;
	}

	public List<PersonelKGS> getpersonelKGSList() {
		return personelKGSList;
	}

	public void setpersonelKGSList(List<PersonelKGS> personelKGSList) {
		this.personelKGSList = personelKGSList;
	}

	public List<PersonelView> getTanimsizPersonelList() {
		return tanimsizPersonelList;
	}

	public void setTanimsizPersonelList(List<PersonelView> tanimsizPersonelList) {
		this.tanimsizPersonelList = tanimsizPersonelList;
	}

	public List<Personel> getPdksPersonelList() {
		return pdksPersonelList;
	}

	public void setPdksPersonelList(List<Personel> pdksPersonelList) {
		this.pdksPersonelList = pdksPersonelList;
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

	public String getSicilNo() {
		return sicilNo;
	}

	public void setSicilNo(String sicilNo) {
		this.sicilNo = sicilNo;
	}

	public List<Departman> getDepartmanTanimList() {
		return departmanTanimList;
	}

	public void setDepartmanTanimList(List<Departman> departmanTanimList) {
		this.departmanTanimList = departmanTanimList;
	}

	public List<Tanim> getDepartmanPDKSTanimList() {
		return departmanPDKSTanimList;
	}

	public void setDepartmanPDKSTanimList(List<Tanim> departmanPDKSTanimList) {
		this.departmanPDKSTanimList = departmanPDKSTanimList;
	}

	public PersonelIzin getBakiyeIzin() {
		return bakiyeIzin;
	}

	public void setBakiyeIzin(PersonelIzin bakiyeIzin) {
		this.bakiyeIzin = bakiyeIzin;
	}

	public PersonelView getPersonelView() {
		return personelView;
	}

	public void setPersonelView(PersonelView personelView) {
		this.personelView = personelView;
	}

	public String getOldUserName() {
		return oldUserName;
	}

	public void setOldUserName(String oldUserName) {
		this.oldUserName = oldUserName;
	}

	public Sirket getOldSirket() {
		return oldSirket;
	}

	public void setOldSirket(Sirket oldSirket) {
		this.oldSirket = oldSirket;
	}

	public String getHataMesaj() {
		return hataMesaj;
	}

	public void setHataMesaj(String hataMesaj) {
		this.hataMesaj = hataMesaj;
	}

	public List<PersonelView> getPersonelList() {
		return personelList;
	}

	public void setPersonelList(List<PersonelView> personelList) {
		this.personelList = personelList;
	}

	public Boolean getPdks() {
		return pdks;
	}

	public void setPdks(Boolean pdks) {
		this.pdks = pdks;
	}

	public List<Tanim> getCinsiyetList() {
		return cinsiyetList;
	}

	public void setCinsiyetList(List<Tanim> cinsiyetList) {
		this.cinsiyetList = cinsiyetList;
	}

	public Personel getAsilYonetici1() {
		return asilYonetici1;
	}

	public void setAsilYonetici1(Personel asilYonetici1) {
		this.asilYonetici1 = asilYonetici1;
	}

	public List<Personel> getYoneticiList() {
		return yoneticiList;
	}

	public void setYoneticiList(List<Personel> yoneticiList) {
		this.yoneticiList = yoneticiList;
	}

	public String getYoneticiTipi() {
		return yoneticiTipi;
	}

	public void setYoneticiTipi(String yoneticiTipi) {
		this.yoneticiTipi = yoneticiTipi;
	}

	public HashMap<String, List<Tanim>> getEkSahaListMap() {
		return ekSahaListMap;
	}

	public void setEkSahaListMap(HashMap<String, List<Tanim>> ekSahaListMap) {
		this.ekSahaListMap = ekSahaListMap;
	}

	public TreeMap<String, Tanim> getEkSahaTanimMap() {
		return ekSahaTanimMap;
	}

	public void setEkSahaTanimMap(TreeMap<String, Tanim> ekSahaTanimMap) {
		this.ekSahaTanimMap = ekSahaTanimMap;
	}

	public Double getBakiyeIzinSuresi() {
		return bakiyeIzinSuresi;
	}

	public void setBakiyeIzinSuresi(Double bakiyeIzinSuresi) {
		this.bakiyeIzinSuresi = bakiyeIzinSuresi;
	}

	public List<Tanim> getMasrafYeriList() {
		return masrafYeriList;
	}

	public void setMasrafYeriList(List<Tanim> masrafYeriList) {
		this.masrafYeriList = masrafYeriList;
	}

	public List<String> getCcAdresList() {
		return ccAdresList;
	}

	public void setCcAdresList(List<String> ccAdresList) {
		this.ccAdresList = ccAdresList;
	}

	public String getCcAdres() {
		return ccAdres;
	}

	public void setCcAdres(String ccAdres) {
		this.ccAdres = ccAdres;
	}

	public String getAdresTipi() {
		return adresTipi;
	}

	public void setAdresTipi(String adresTipi) {
		this.adresTipi = adresTipi;
	}

	public List<String> getBccAdresList() {
		return bccAdresList;
	}

	public void setBccAdresList(List<String> bccAdresList) {
		this.bccAdresList = bccAdresList;
	}

	public List<Tanim> getYoneticiVardiyaTipiList() {
		return yoneticiVardiyaTipiList;
	}

	public void setYoneticiVardiyaTipiList(List<Tanim> yoneticiVardiyaTipiList) {
		this.yoneticiVardiyaTipiList = yoneticiVardiyaTipiList;
	}

	public List<Tanim> getVardiyaGirisTipiTanimList() {
		return vardiyaGirisTipiTanimList;
	}

	public void setVardiyaGirisTipiTanimList(List<Tanim> vardiyaGirisTipiTanimList) {
		this.vardiyaGirisTipiTanimList = vardiyaGirisTipiTanimList;
	}

	public List<Tanim> getBolumDepartmanlari() {
		return bolumDepartmanlari;
	}

	public void setBolumDepartmanlari(List<Tanim> bolumDepartmanlari) {
		this.bolumDepartmanlari = bolumDepartmanlari;
	}

	public List<Tanim> getUnvanTanimList() {
		return unvanTanimList;
	}

	public void setUnvanTanimList(List<Tanim> unvanTanimList) {
		this.unvanTanimList = unvanTanimList;
	}

	public List<String> getHareketAdresList() {
		return hareketAdresList;
	}

	public void setHareketAdresList(List<String> hareketAdresList) {
		this.hareketAdresList = hareketAdresList;
	}

	public List<PersonelKGS> getPersonelKGSList() {
		return personelKGSList;
	}

	public void setPersonelKGSList(List<PersonelKGS> personelKGSList) {
		this.personelKGSList = personelKGSList;
	}

	public Dosya getPersonelDosya() {
		return personelDosya;
	}

	public void setPersonelDosya(Dosya personelDosya) {
		this.personelDosya = personelDosya;
	}

	public List<PersonelERP> getPersonelERPList() {
		return personelERPList;
	}

	public void setPersonelERPList(List<PersonelERP> personelERPList) {
		this.personelERPList = personelERPList;
	}

	public String getPersonelERPGuncelleme() {
		return personelERPGuncelleme;
	}

	public void setPersonelERPGuncelleme(String personelERPGuncelleme) {
		this.personelERPGuncelleme = personelERPGuncelleme;
	}

	public Boolean getServisCalisti() {
		return servisCalisti;
	}

	public void setServisCalisti(Boolean servisCalisti) {
		this.servisCalisti = servisCalisti;
	}

	public List<Tanim> getTesisList() {
		return tesisList;
	}

	public void setTesisList(List<Tanim> tesisList) {
		this.tesisList = tesisList;
	}

	public int getCOL_SICIL_NO() {
		return COL_SICIL_NO;
	}

	public void setCOL_SICIL_NO(int cOL_SICIL_NO) {
		COL_SICIL_NO = cOL_SICIL_NO;
	}

	public int getCOL_ADI() {
		return COL_ADI;
	}

	public void setCOL_ADI(int cOL_ADI) {
		COL_ADI = cOL_ADI;
	}

	public int getCOL_SOYADI() {
		return COL_SOYADI;
	}

	public void setCOL_SOYADI(int cOL_SOYADI) {
		COL_SOYADI = cOL_SOYADI;
	}

	public int getCOL_SIRKET_KODU() {
		return COL_SIRKET_KODU;
	}

	public void setCOL_SIRKET_KODU(int cOL_SIRKET_KODU) {
		COL_SIRKET_KODU = cOL_SIRKET_KODU;
	}

	public int getCOL_SIRKET_ADI() {
		return COL_SIRKET_ADI;
	}

	public void setCOL_SIRKET_ADI(int cOL_SIRKET_ADI) {
		COL_SIRKET_ADI = cOL_SIRKET_ADI;
	}

	public int getCOL_TESIS_KODU() {
		return COL_TESIS_KODU;
	}

	public void setCOL_TESIS_KODU(int cOL_TESIS_KODU) {
		COL_TESIS_KODU = cOL_TESIS_KODU;
	}

	public int getCOL_TESIS_ADI() {
		return COL_TESIS_ADI;
	}

	public void setCOL_TESIS_ADI(int cOL_TESIS_ADI) {
		COL_TESIS_ADI = cOL_TESIS_ADI;
	}

	public int getCOL_GOREV_KODU() {
		return COL_GOREV_KODU;
	}

	public void setCOL_GOREV_KODU(int cOL_GOREV_KODU) {
		COL_GOREV_KODU = cOL_GOREV_KODU;
	}

	public int getCOL_GOREVI() {
		return COL_GOREVI;
	}

	public void setCOL_GOREVI(int cOL_GOREVI) {
		COL_GOREVI = cOL_GOREVI;
	}

	public int getCOL_BOLUM_KODU() {
		return COL_BOLUM_KODU;
	}

	public void setCOL_BOLUM_KODU(int cOL_BOLUM_KODU) {
		COL_BOLUM_KODU = cOL_BOLUM_KODU;
	}

	public int getCOL_BOLUM_ADI() {
		return COL_BOLUM_ADI;
	}

	public void setCOL_BOLUM_ADI(int cOL_BOLUM_ADI) {
		COL_BOLUM_ADI = cOL_BOLUM_ADI;
	}

	public int getCOL_ISE_BASLAMA_TARIHI() {
		return COL_ISE_BASLAMA_TARIHI;
	}

	public void setCOL_ISE_BASLAMA_TARIHI(int cOL_ISE_BASLAMA_TARIHI) {
		COL_ISE_BASLAMA_TARIHI = cOL_ISE_BASLAMA_TARIHI;
	}

	public int getCOL_KIDEM_TARIHI() {
		return COL_KIDEM_TARIHI;
	}

	public void setCOL_KIDEM_TARIHI(int cOL_KIDEM_TARIHI) {
		COL_KIDEM_TARIHI = cOL_KIDEM_TARIHI;
	}

	public int getCOL_ISTEN_AYRILMA_TARIHI() {
		return COL_ISTEN_AYRILMA_TARIHI;
	}

	public void setCOL_ISTEN_AYRILMA_TARIHI(int cOL_ISTEN_AYRILMA_TARIHI) {
		COL_ISTEN_AYRILMA_TARIHI = cOL_ISTEN_AYRILMA_TARIHI;
	}

	public int getCOL_DOGUM_TARIHI() {
		return COL_DOGUM_TARIHI;
	}

	public void setCOL_DOGUM_TARIHI(int cOL_DOGUM_TARIHI) {
		COL_DOGUM_TARIHI = cOL_DOGUM_TARIHI;
	}

	public int getCOL_CINSIYET_KODU() {
		return COL_CINSIYET_KODU;
	}

	public void setCOL_CINSIYET_KODU(int cOL_CINSIYET_KODU) {
		COL_CINSIYET_KODU = cOL_CINSIYET_KODU;
	}

	public int getCOL_CINSIYET() {
		return COL_CINSIYET;
	}

	public void setCOL_CINSIYET(int cOL_CINSIYET) {
		COL_CINSIYET = cOL_CINSIYET;
	}

	public int getCOL_YONETICI_KODU() {
		return COL_YONETICI_KODU;
	}

	public void setCOL_YONETICI_KODU(int cOL_YONETICI_KODU) {
		COL_YONETICI_KODU = cOL_YONETICI_KODU;
	}

	public int getCOL_DEPARTMAN_KODU() {
		return COL_DEPARTMAN_KODU;
	}

	public void setCOL_DEPARTMAN_KODU(int cOL_DEPARTMAN_KODU) {
		COL_DEPARTMAN_KODU = cOL_DEPARTMAN_KODU;
	}

	public int getCOL_DEPARTMAN_ADI() {
		return COL_DEPARTMAN_ADI;
	}

	public void setCOL_DEPARTMAN_ADI(int cOL_DEPARTMAN_ADI) {
		COL_DEPARTMAN_ADI = cOL_DEPARTMAN_ADI;
	}

	public int getCOL_MASRAF_YERI_KODU() {
		return COL_MASRAF_YERI_KODU;
	}

	public void setCOL_MASRAF_YERI_KODU(int cOL_MASRAF_YERI_KODU) {
		COL_MASRAF_YERI_KODU = cOL_MASRAF_YERI_KODU;
	}

	public int getCOL_MASRAF_YERI_ADI() {
		return COL_MASRAF_YERI_ADI;
	}

	public void setCOL_MASRAF_YERI_ADI(int cOL_MASRAF_YERI_ADI) {
		COL_MASRAF_YERI_ADI = cOL_MASRAF_YERI_ADI;
	}

	public int getCOL_BORDRO_ALT_ALAN_KODU() {
		return COL_BORDRO_ALT_ALAN_KODU;
	}

	public void setCOL_BORDRO_ALT_ALAN_KODU(int cOL_BORDRO_ALT_ALAN_KODU) {
		COL_BORDRO_ALT_ALAN_KODU = cOL_BORDRO_ALT_ALAN_KODU;
	}

	public int getCOL_BORDRO_ALT_ALAN_ADI() {
		return COL_BORDRO_ALT_ALAN_ADI;
	}

	public void setCOL_BORDRO_ALT_ALAN_ADI(int cOL_BORDRO_ALT_ALAN_ADI) {
		COL_BORDRO_ALT_ALAN_ADI = cOL_BORDRO_ALT_ALAN_ADI;
	}

	public int getCOL_BORDRO_SANAL_PERSONEL() {
		return COL_BORDRO_SANAL_PERSONEL;
	}

	public void setCOL_BORDRO_SANAL_PERSONEL(int cOL_BORDRO_SANAL_PERSONEL) {
		COL_BORDRO_SANAL_PERSONEL = cOL_BORDRO_SANAL_PERSONEL;
	}

	public List<CalismaModeli> getCalismaModeliList() {
		return calismaModeliList;
	}

	public void setCalismaModeliList(List<CalismaModeli> calismaModeliList) {
		this.calismaModeliList = calismaModeliList;
	}

	public String getSanalPersonelAciklama() {
		return sanalPersonelAciklama;
	}

	public void setSanalPersonelAciklama(String sanalPersonelAciklama) {
		this.sanalPersonelAciklama = sanalPersonelAciklama;
	}

	public List<Tanim> getGorevDepartmanlari() {
		return gorevDepartmanlari;
	}

	public void setGorevDepartmanlari(List<Tanim> gorevDepartmanlari) {
		this.gorevDepartmanlari = gorevDepartmanlari;
	}

	public Boolean getFazlaMesaiIzinKullan() {
		return fazlaMesaiIzinKullan;
	}

	public void setFazlaMesaiIzinKullan(Boolean fazlaMesaiIzinKullan) {
		this.fazlaMesaiIzinKullan = fazlaMesaiIzinKullan;
	}

	public Boolean getFazlaMesaiOde() {
		return fazlaMesaiOde;
	}

	public void setFazlaMesaiOde(Boolean fazlaMesaiOde) {
		this.fazlaMesaiOde = fazlaMesaiOde;
	}

	public Boolean getSanalPersonel() {
		return sanalPersonel;
	}

	public void setSanalPersonel(Boolean sanalPersonel) {
		this.sanalPersonel = sanalPersonel;
	}

	public Boolean getKullaniciPersonel() {
		return kullaniciPersonel;
	}

	public void setKullaniciPersonel(Boolean kullaniciPersonel) {
		this.kullaniciPersonel = kullaniciPersonel;
	}

	public Boolean getGebeMi() {
		return gebeMi;
	}

	public void setGebeMi(Boolean gebeMi) {
		this.gebeMi = gebeMi;
	}

	public Boolean getSutIzni() {
		return sutIzni;
	}

	public void setSutIzni(Boolean sutIzni) {
		this.sutIzni = sutIzni;
	}

	public Boolean getIcapDurum() {
		return icapDurum;
	}

	public void setIcapDurum(Boolean icapDurum) {
		this.icapDurum = icapDurum;
	}

	public Boolean getUstYonetici() {
		return ustYonetici;
	}

	public void setUstYonetici(Boolean ustYonetici) {
		this.ustYonetici = ustYonetici;
	}

	public Boolean getPartTimeDurum() {
		return partTimeDurum;
	}

	public void setPartTimeDurum(Boolean partTimeDurum) {
		this.partTimeDurum = partTimeDurum;
	}

	public Boolean getEgitimDonemi() {
		return egitimDonemi;
	}

	public void setEgitimDonemi(Boolean egitimDonemi) {
		this.egitimDonemi = egitimDonemi;
	}

	public Boolean getSuaOlabilir() {
		return suaOlabilir;
	}

	public void setSuaOlabilir(Boolean suaOlabilir) {
		this.suaOlabilir = suaOlabilir;
	}

	public Boolean getEmailCCDurum() {
		return emailCCDurum;
	}

	public void setEmailCCDurum(Boolean emailCCDurum) {
		this.emailCCDurum = emailCCDurum;
	}

	public Boolean getEmailBCCDurum() {
		return emailBCCDurum;
	}

	public void setEmailBCCDurum(Boolean emailBCCDurum) {
		this.emailBCCDurum = emailBCCDurum;
	}

	public List<UserMenuItemTime> getMenuItemTimeList() {
		return menuItemTimeList;
	}

	public void setMenuItemTimeList(List<UserMenuItemTime> menuItemTimeList) {
		this.menuItemTimeList = menuItemTimeList;
	}

	public String getePosta() {
		return ePosta;
	}

	public void setePosta(String ePosta) {
		this.ePosta = ePosta;
	}

	public String getDenemeMesaj() {
		return denemeMesaj;
	}

	public void setDenemeMesaj(String denemeMesaj) {
		this.denemeMesaj = denemeMesaj;
	}

	public String getKartNoAciklama() {
		return kartNoAciklama;
	}

	public void setKartNoAciklama(String kartNoAciklama) {
		this.kartNoAciklama = kartNoAciklama;
	}

	public List<Liste> getDosyaTanimList() {
		return dosyaTanimList;
	}

	public void setDosyaTanimList(List<Liste> dosyaTanimList) {
		this.dosyaTanimList = dosyaTanimList;
	}

	public List<Tanim> getDistinctTesisList() {
		return distinctTesisList;
	}

	public void setDistinctTesisList(List<Tanim> distinctTesisList) {
		this.distinctTesisList = distinctTesisList;
	}

	public Boolean getTesisYetki() {
		return tesisYetki;
	}

	public void setTesisYetki(Boolean tesisYetki) {
		this.tesisYetki = tesisYetki;
	}

	public User getEskiKullanici() {
		return eskiKullanici;
	}

	public void setEskiKullanici(User eskiKullanici) {
		this.eskiKullanici = eskiKullanici;
	}

	public List<Departman> getDepartmanKullaniciList() {
		return departmanKullaniciList;
	}

	public List<PersonelERP> getPersonelERPReturnList() {
		return personelERPReturnList;
	}

	public void setPersonelERPReturnList(List<PersonelERP> personelERPReturnList) {
		this.personelERPReturnList = personelERPReturnList;
	}

	public void setDepartmanKullaniciList(List<Departman> departmanKullaniciList) {
		this.departmanKullaniciList = departmanKullaniciList;
	}

	public Boolean getTaseronKulaniciTanimla() {
		return taseronKulaniciTanimla;
	}

	public void setTaseronKulaniciTanimla(Boolean taseronKulaniciTanimla) {
		this.taseronKulaniciTanimla = taseronKulaniciTanimla;
	}

	public Boolean getManuelTanimla() {
		return manuelTanimla;
	}

	public void setManuelTanimla(Boolean manuelTanimla) {
		this.manuelTanimla = manuelTanimla;
	}

	public int getCOL_GRUBA_GIRIS_TARIHI() {
		return COL_GRUBA_GIRIS_TARIHI;
	}

	public void setCOL_GRUBA_GIRIS_TARIHI(int cOL_GRUBA_GIRIS_TARIHI) {
		COL_GRUBA_GIRIS_TARIHI = cOL_GRUBA_GIRIS_TARIHI;
	}

	public List<Tanim> getDinamikDurumList() {
		return dinamikDurumList;
	}

	public void setDinamikDurumList(List<Tanim> dinamikDurumList) {
		this.dinamikDurumList = dinamikDurumList;
	}

	public List<Tanim> getDinamikSayisalList() {
		return dinamikSayisalList;
	}

	public void setDinamikSayisalList(List<Tanim> dinamikSayisalList) {
		this.dinamikSayisalList = dinamikSayisalList;
	}

	public TreeMap<String, PersonelDinamikAlan> getPersonelDinamikMap() {
		return personelDinamikMap;
	}

	public void setPersonelDinamikMap(TreeMap<String, PersonelDinamikAlan> personelDinamikMap) {
		this.personelDinamikMap = personelDinamikMap;
	}

	public PersonelDinamikAlan getPersonelDinamikAlan() {
		return personelDinamikAlan;
	}

	public void setPersonelDinamikAlan(PersonelDinamikAlan personelDinamikAlan) {
		this.personelDinamikAlan = personelDinamikAlan;
	}

	public List<PersonelDinamikAlan> getDinamikPersonelDurumList() {
		return dinamikPersonelDurumList;
	}

	public void setDinamikPersonelDurumList(List<PersonelDinamikAlan> dinamikPersonelDurumList) {
		this.dinamikPersonelDurumList = dinamikPersonelDurumList;
	}

	public List<PersonelDinamikAlan> getDinamikPersonelSayisalList() {
		return dinamikPersonelSayisalList;
	}

	public void setDinamikPersonelSayisalList(List<PersonelDinamikAlan> dinamikPersonelSayisalList) {
		this.dinamikPersonelSayisalList = dinamikPersonelSayisalList;
	}

	public HashMap<Long, List<Tanim>> getDinamikPersonelAciklamaMap() {
		return dinamikPersonelAciklamaMap;
	}

	public void setDinamikPersonelAciklamaMap(HashMap<Long, List<Tanim>> dinamikPersonelAciklamaMap) {
		this.dinamikPersonelAciklamaMap = dinamikPersonelAciklamaMap;
	}

	public int getCOL_YONETICI2_KODU() {
		return COL_YONETICI2_KODU;
	}

	public void setCOL_YONETICI2_KODU(int cOL_YONETICI2_KODU) {
		COL_YONETICI2_KODU = cOL_YONETICI2_KODU;
	}

	public Boolean getIkinciYoneticiManuelTanimla() {
		return ikinciYoneticiManuelTanimla;
	}

	public void setIkinciYoneticiManuelTanimla(Boolean ikinciYoneticiManuelTanimla) {
		this.ikinciYoneticiManuelTanimla = ikinciYoneticiManuelTanimla;
	}

	public Boolean getOnaysizIzinKullanilir() {
		return onaysizIzinKullanilir;
	}

	public void setOnaysizIzinKullanilir(Boolean onaysizIzinKullanilir) {
		this.onaysizIzinKullanilir = onaysizIzinKullanilir;
	}

	public Boolean getIkinciYoneticiIzinOnayla() {
		return ikinciYoneticiIzinOnayla;
	}

	public void setIkinciYoneticiIzinOnayla(Boolean ikinciYoneticiIzinOnayla) {
		this.ikinciYoneticiIzinOnayla = ikinciYoneticiIzinOnayla;
	}

	public HashMap<String, Boolean> getPersonelDurumMap() {
		return personelDurumMap;
	}

	public void setPersonelDurumMap(HashMap<String, Boolean> personelDurumMap) {
		this.personelDurumMap = personelDurumMap;
	}

	public Boolean getIzinGirisiVar() {
		return izinGirisiVar;
	}

	public void setIzinGirisiVar(Boolean izinGirisiVar) {
		this.izinGirisiVar = izinGirisiVar;
	}

	public Boolean getDosyaGuncellemeYetki() {
		return dosyaGuncellemeYetki;
	}

	public void setDosyaGuncellemeYetki(Boolean dosyaGuncellemeYetki) {
		this.dosyaGuncellemeYetki = dosyaGuncellemeYetki;
	}

	public Boolean getYoneticiRolVarmi() {
		return yoneticiRolVarmi;
	}

	public void setYoneticiRolVarmi(Boolean yoneticiRolVarmi) {
		this.yoneticiRolVarmi = yoneticiRolVarmi;
	}

	public Tanim getBosDepartman() {
		return bosDepartman;
	}

	public void setBosDepartman(Tanim bosDepartman) {
		this.bosDepartman = bosDepartman;
	}

	public Tanim getParentDepartman() {
		return parentDepartman;
	}

	public void setParentDepartman(Tanim parentDepartman) {
		this.parentDepartman = parentDepartman;
	}

	public List<Personel> getYonetici2List() {
		return yonetici2List;
	}

	public void setYonetici2List(List<Personel> yonetici2List) {
		this.yonetici2List = yonetici2List;
	}

	public Boolean getDepartmanGoster() {
		return departmanGoster;
	}

	public void setDepartmanGoster(Boolean departmanGoster) {
		this.departmanGoster = departmanGoster;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getBolumAciklama() {
		return bolumAciklama;
	}

	public void setBolumAciklama(String bolumAciklama) {
		this.bolumAciklama = bolumAciklama;
	}

	public Tanim getParentBordroTanim() {
		return parentBordroTanim;
	}

	public void setParentBordroTanim(Tanim parentBordroTanim) {
		this.parentBordroTanim = parentBordroTanim;
	}

	public Boolean getKimlikNoGoster() {
		return kimlikNoGoster;
	}

	public void setKimlikNoGoster(Boolean kimlikNoGoster) {
		this.kimlikNoGoster = kimlikNoGoster;
	}

	public String getDepartmanAciklama() {
		return departmanAciklama;
	}

	public void setDepartmanAciklama(String departmanAciklama) {
		this.departmanAciklama = departmanAciklama;
	}

	public String getAltBolumAciklama() {
		return altBolumAciklama;
	}

	public void setAltBolumAciklama(String altBolumAciklama) {
		this.altBolumAciklama = altBolumAciklama;
	}

	public List<Personel> getIkinciYoneticiHataliList() {
		return ikinciYoneticiHataliList;
	}

	public void setIkinciYoneticiHataliList(List<Personel> ikinciYoneticiHataliList) {
		this.ikinciYoneticiHataliList = ikinciYoneticiHataliList;
	}

	public Boolean getIstenAyrilmaGoster() {
		return istenAyrilmaGoster;
	}

	public void setIstenAyrilmaGoster(Boolean istenAyrilmaGoster) {
		this.istenAyrilmaGoster = istenAyrilmaGoster;
	}

	public Boolean getTesisDurum() {
		return tesisDurum;
	}

	public void setTesisDurum(Boolean tesisDurum) {
		this.tesisDurum = tesisDurum;
	}

	public Boolean getEkSaha1Disable() {
		return ekSaha1Disable;
	}

	public void setEkSaha1Disable(Boolean ekSaha1Disable) {
		this.ekSaha1Disable = ekSaha1Disable;
	}

	public Boolean getEkSaha2Disable() {
		return ekSaha2Disable;
	}

	public void setEkSaha2Disable(Boolean ekSaha2Disable) {
		this.ekSaha2Disable = ekSaha2Disable;
	}

	public Boolean getEkSaha4Disable() {
		return ekSaha4Disable;
	}

	public void setEkSaha4Disable(Boolean ekSaha4Disable) {
		this.ekSaha4Disable = ekSaha4Disable;
	}

	public Boolean getKartNoGoster() {
		return kartNoGoster;
	}

	public void setKartNoGoster(Boolean kartNoGoster) {
		this.kartNoGoster = kartNoGoster;
	}

	public Boolean getTransferAciklamaCiftKontrol() {
		return transferAciklamaCiftKontrol;
	}

	public void setTransferAciklamaCiftKontrol(Boolean transferAciklamaCiftKontrol) {
		this.transferAciklamaCiftKontrol = transferAciklamaCiftKontrol;
	}

	public TreeMap<Long, PersonelKGS> getPersonelKGSMap() {
		return personelKGSMap;
	}

	public void setPersonelKGSMap(TreeMap<Long, PersonelKGS> personelKGSMap) {
		this.personelKGSMap = personelKGSMap;
	}

	public Boolean getBakiyeIzinGoster() {
		return bakiyeIzinGoster;
	}

	public void setBakiyeIzinGoster(Boolean bakiyeIzinGoster) {
		this.bakiyeIzinGoster = bakiyeIzinGoster;
	}

	public Boolean getGebeSecim() {
		return gebeSecim;
	}

	public void setGebeSecim(Boolean gebeSecim) {
		this.gebeSecim = gebeSecim;
	}

	public List<Tanim> getPersonelTipleri() {
		return personelTipleri;
	}

	public void setPersonelTipleri(List<Tanim> personelTipleri) {
		this.personelTipleri = personelTipleri;
	}

	public Boolean getPersonelTipiGoster() {
		return personelTipiGoster;
	}

	public void setPersonelTipiGoster(Boolean personelTipiGoster) {
		this.personelTipiGoster = personelTipiGoster;
	}

	public int getCOL_PERSONEL_TIPI() {
		return COL_PERSONEL_TIPI;
	}

	public void setCOL_PERSONEL_TIPI(int cOL_PERSONEL_TIPI) {
		COL_PERSONEL_TIPI = cOL_PERSONEL_TIPI;
	}

	public int getCOL_PERSONEL_TIPI_ADI() {
		return COL_PERSONEL_TIPI_ADI;
	}

	public void setCOL_PERSONEL_TIPI_ADI(int cOL_PERSONEL_TIPI_ADI) {
		COL_PERSONEL_TIPI_ADI = cOL_PERSONEL_TIPI_ADI;
	}

	public List<Long> getGebeSutIzniDurumList() {
		return gebeSutIzniDurumList;
	}

	public void setGebeSutIzniDurumList(List<Long> gebeSutIzniDurumList) {
		this.gebeSutIzniDurumList = gebeSutIzniDurumList;
	}

	public PersonelDonemselDurum getDonemselDurum() {
		return donemselDurum;
	}

	public void setDonemselDurum(PersonelDonemselDurum donemselDurum) {
		this.donemselDurum = donemselDurum;
	}

	public List<PersonelDonemselDurum> getDonemselDurumList() {
		return donemselDurumList;
	}

	public void setDonemselDurumList(List<PersonelDonemselDurum> donemselDurumList) {
		this.donemselDurumList = donemselDurumList;
	}

	public List<SelectItem> getPersonelDurumTipiList() {
		return personelDurumTipiList;
	}

	public void setPersonelDurumTipiList(List<SelectItem> personelDurumTipiList) {
		this.personelDurumTipiList = personelDurumTipiList;
	}

	public Boolean getUpdateValue() {
		return updateValue;
	}

	public void setUpdateValue(Boolean updateValue) {
		this.updateValue = updateValue;
	}

	public Boolean getYeniPersonelGuncelle() {
		return yeniPersonelGuncelle;
	}

	public void setYeniPersonelGuncelle(Boolean yeniPersonelGuncelle) {
		this.yeniPersonelGuncelle = yeniPersonelGuncelle;
	}

	public void getYoneticiler(String tipi) {
		setYoneticiTipi(tipi);
	}

	public void setYoneticisi(Personel seciliYonetici) {

	}

	public String getIconLeaf() {
		return iconLeaf;
	}

	public void setIconLeaf(String iconLeaf) {
		this.iconLeaf = iconLeaf;
	}

	public Boolean getDisableAdviseNodeOpened() {
		return disableAdviseNodeOpened;
	}

	public void setDisableAdviseNodeOpened(Boolean value) {
		this.disableAdviseNodeOpened = value;
	}

	public TreeNode<PersonelView> getRootNodeForAllPersonelView() {

		return rootNodeForAllPersonelView;
	}

	public void setRootNodeForAllPersonelView(TreeNode<PersonelView> rootNodeForAllPersonelView) {
		this.rootNodeForAllPersonelView = rootNodeForAllPersonelView;
	}

	public Boolean getOrganizasyonSemasiGoster() {
		return organizasyonSemasiGoster;
	}

	public void setOrganizasyonSemasiGoster(Boolean organizasyonSemasiGoster) {
		this.organizasyonSemasiGoster = organizasyonSemasiGoster;
	}

	public List<Vardiya> getCalismaModeliVardiyaList() {
		return calismaModeliVardiyaList;
	}

	public void setCalismaModeliVardiyaList(List<Vardiya> calismaModeliVardiyaList) {
		this.calismaModeliVardiyaList = calismaModeliVardiyaList;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		PdksPersonelHome.sayfaURL = sayfaURL;
	}

	public Boolean getIzinKartiVardir() {
		return izinKartiVardir;
	}

	public void setIzinKartiVardir(Boolean izinKartiVardir) {
		this.izinKartiVardir = izinKartiVardir;
	}

	public List<Tanim> getDinamikTanimList() {
		return dinamikTanimList;
	}

	public void setDinamikTanimList(List<Tanim> dinamikTanimList) {
		this.dinamikTanimList = dinamikTanimList;
	}

	public List<PersonelDinamikAlan> getDinamikPersonelTanimList() {
		return dinamikPersonelTanimList;
	}

	public void setDinamikPersonelTanimList(List<PersonelDinamikAlan> dinamikPersonelTanimList) {
		this.dinamikPersonelTanimList = dinamikPersonelTanimList;
	}

	public Boolean getBakiyeTakipEdiliyor() {
		return bakiyeTakipEdiliyor;
	}

	public void setBakiyeTakipEdiliyor(Boolean bakiyeTakipEdiliyor) {
		this.bakiyeTakipEdiliyor = bakiyeTakipEdiliyor;
	}

	public Boolean getBolumYetki() {
		return bolumYetki;
	}

	public void setBolumYetki(Boolean bolumYetki) {
		this.bolumYetki = bolumYetki;
	}

	public List<Tanim> getDistinctBolumList() {
		return distinctBolumList;
	}

	public void setDistinctBolumList(List<Tanim> distinctBolumList) {
		this.distinctBolumList = distinctBolumList;
	}

}
