package org.pdks.session;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Timestamp;
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
import java.util.StringTokenizer;
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
import org.apache.poi.ss.usermodel.RichTextString;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.validator.InvalidStateException;
import org.hibernate.validator.InvalidValue;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.AramaSecenekleri;
import org.pdks.entity.AylikPuantaj;
import org.pdks.entity.BolumKat;
import org.pdks.entity.CalismaModeli;
import org.pdks.entity.CalismaModeliAy;
import org.pdks.entity.CalismaModeliVardiya;
import org.pdks.entity.CalismaPlanKilit;
import org.pdks.entity.CalismaPlanKilitTalep;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.Departman;
import org.pdks.entity.DepartmanDenklestirmeDonemi;
import org.pdks.entity.DepartmanMailGrubu;
import org.pdks.entity.Dosya;
import org.pdks.entity.FazlaMesaiTalep;
import org.pdks.entity.HareketKGS;
import org.pdks.entity.IzinTipi;
import org.pdks.entity.Kapi;
import org.pdks.entity.KapiKGS;
import org.pdks.entity.KapiView;
import org.pdks.entity.Liste;
import org.pdks.entity.Notice;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDenklestirme;
import org.pdks.entity.PersonelDenklestirmeBordro;
import org.pdks.entity.PersonelDenklestirmeBordroDetay;
import org.pdks.entity.PersonelDenklestirmeDinamikAlan;
import org.pdks.entity.PersonelDenklestirmeTasiyici;
import org.pdks.entity.PersonelDonemselDurum;
import org.pdks.entity.PersonelFazlaMesai;
import org.pdks.entity.PersonelHareket;
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
import org.pdks.entity.VardiyaPlan;
import org.pdks.entity.VardiyaSablonu;
import org.pdks.entity.YemekIzin;
import org.pdks.enums.BordroDetayTipi;
import org.pdks.enums.NoteTipi;
import org.pdks.enums.PersonelDurumTipi;
import org.pdks.security.action.UserHome;
import org.pdks.security.entity.MenuItemConstant;
import org.pdks.security.entity.User;
import org.primefaces.event.FileUploadEvent;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import com.pdks.webservice.MailFile;
import com.pdks.webservice.MailObject;
import com.pdks.webservice.MailStatu;

@Name("vardiyaGunHome")
public class VardiyaGunHome extends EntityHome<VardiyaPlan> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5067953117682032644L;
	static Logger logger = Logger.getLogger(VardiyaGunHome.class);

	@RequestParameter
	Long pdksVardiyaGunId;
	@In(required = false, create = true)
	ComponentState componentState;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = false, create = true)
	User authenticatedUser;
	@In(required = false, create = true)
	UserHome userHome;
	@In(required = false)
	FacesMessages facesMessages;
	@In(create = true, required = true)
	OrtakIslemler ortakIslemler;
	@In(required = false, create = true)
	HashMap parameterMap;
	@In(required = true, create = true)
	Renderer renderer;
	@In(required = false, create = true)
	FazlaMesaiOrtakIslemler fazlaMesaiOrtakIslemler;
	@In(create = true, required = true)
	PersonelIzinGirisiHome personelIzinGirisiHome;
	@In(required = false, create = true)
	String linkAdres;
	@In(scope = ScopeType.SESSION, required = false)
	String bordroAdres;

	public static String sayfaURL = "vardiyaPlani";

	private TreeMap<String, Tanim> fazlaMesaiMap;

	private Integer aksamVardiyaBasSaat, aksamVardiyaBitSaat, aksamVardiyaBasDakika, aksamVardiyaBitDakika;

	private List<VardiyaPlan> vardiyaPlanList = new ArrayList<VardiyaPlan>();

	private HashMap<Long, List<Personel>> gorevPersonelMap;

	private List<PersonelDenklestirmeDinamikAlan> personelDenklestirmeDinamikAlanList;

	private HashMap<Long, Vardiya> vardiyaDbMap;

	private List<SelectItem> fazlaMesaiTalepDurumList, fazlaMesaiDurumList;

	private Integer fazlaMesaiDurum;

	private List<Personel> tumBolumPersonelleri;

	private TreeMap<String, Boolean> baslikMap;

	private List<Tanim> dinamikAlanlar = null;

	private List<VardiyaGun> vardiyaGunList = new ArrayList<VardiyaGun>(), aylikVardiyaOzetList;

	private List<Vardiya> vardiyaList = new ArrayList<Vardiya>();

	private PersonelDonemselDurum personelGebeDurum, personelSutIzniDurum;

	protected List<Vardiya> vardiyaBolumList = new ArrayList<Vardiya>();

	private FazlaMesaiTalep fazlaMesaiTalep, islemFazlaMesaiTalep;

	private HashMap<String, List<Tanim>> ekSahaListMap;

	private CalismaPlanKilit calismaPlanKilit = null;

	private List<CalismaPlanKilit> kilitliPlanList = null;

	private List<Vardiya> calismaModeliVardiyaList;

	private boolean fileImport = Boolean.FALSE, fazlaMesaiTalepVar = Boolean.FALSE, fazlaMesaiOde, fazlaMesaiIzinKullan, modelGoster = Boolean.FALSE, gebeGoster = Boolean.FALSE;

	private Boolean manuelHareketEkle, vardiyaFazlaMesaiTalepGoster = Boolean.FALSE, bakiyeSifirlaDurum = Boolean.FALSE, isAramaGoster = Boolean.FALSE, yoneticiERP1Kontrol = Boolean.FALSE, bordroPuantajEkranindaGoster = Boolean.FALSE;

	private boolean adminRole, ikRole, gorevYeriGirisDurum, kartBasmayanPersonel, fazlaMesaiTarihGuncelle = Boolean.FALSE, offIzinGuncelle = Boolean.FALSE, gebeSutIzniGuncelle = Boolean.FALSE;

	private Dosya vardiyaPlanDosya = new Dosya();

	private HashMap<Double, ArrayList<Vardiya>> vardiyaMap = new HashMap<Double, ArrayList<Vardiya>>();

	private List<VardiyaSablonu> sablonList = new ArrayList<VardiyaSablonu>();

	private List<BolumKat> bolumKatlari;

	private List<HareketKGS> hareketPdksList = null;

	private TreeMap<String, Tatil> tatilGunleriMap, tatilMap;
	private AylikPuantaj defaultAylikPuantajSablon;

	private List<CalismaModeliAy> modelList;

	private List<FazlaMesaiTalep> aylikFazlaMesaiTalepler;

	private List<Vardiya> calismaOlmayanVardiyaList;

	private boolean vardiyaVar = Boolean.FALSE, seciliDurum, mailGonder, mesaiOnayla, haftaTatilMesaiDurum = Boolean.FALSE, vardiyaGuncelle = Boolean.FALSE, hastaneSuperVisor = Boolean.FALSE;
	private boolean fazlaMesaiIzinRaporuDurum, onayDurum = Boolean.FALSE, partTimeGoster = Boolean.FALSE, sutIzniGoster = Boolean.FALSE, planGirisi, sablonGuncelle, veriGuncellendi;
	private boolean plansiz = false;
	private ArrayList<Date> islemGunleri;

	private AylikPuantaj aylikPuantajDonem;

	private List<FazlaMesaiTalep> fazlaMesaiTalepler;

	private KapiView manuelGiris = null, manuelCikis = null;

	private Date basTarih, bitTarih;

	private Vardiya normalCalismaVardiya;

	private String basTarihStr, bitTarihStr, sanalPersonelAciklama, bolumAciklama, altBolumAciklama, tesisAciklama, linkBordroAdres;

	private List<SelectItem> gunler;

	private VardiyaGun seciliVardiyaGun;

	private ByteArrayOutputStream baosDosya;

	private List<Long> gorevYerileri;

	private VardiyaHafta vardiyaHafta1, vardiyaHafta2;

	private User loginUser = null;

	private String sicilNo = "", dosyaAdi, donusAdres;

	private Boolean denklestirmeHesapla = Boolean.FALSE, gunSec = Boolean.FALSE, gorevli = false, ozelIstek = Boolean.FALSE, islemYapiliyor = Boolean.FALSE, departmanBolumAyni = Boolean.FALSE;

	private Boolean resmiTatilVar = Boolean.FALSE, eksikMaasGoster = Boolean.FALSE, aksamGunVar = Boolean.FALSE, aksamSaatVar = Boolean.FALSE, haftaTatilVar = Boolean.FALSE;

	private Boolean topluFazlaCalismaTalep = Boolean.FALSE, denklestirmeAyDurum = Boolean.FALSE, fazlaMesaiTalepDurum = Boolean.FALSE, aylikHareketKaydiVardiyaBul = Boolean.FALSE;

	private Boolean gerceklesenMesaiKod = Boolean.FALSE, devredenBakiyeKod = Boolean.FALSE, normalCalismaSaatKod = Boolean.FALSE, haftaTatilCalismaSaatKod = Boolean.FALSE, resmiTatilCalismaSaatKod = Boolean.FALSE, izinSureSaatKod = Boolean.FALSE;

	private Boolean normalCalismaGunKod = Boolean.FALSE, haftaTatilCalismaGunKod = Boolean.FALSE, resmiTatilCalismaGunKod = Boolean.FALSE, izinSureGunKod = Boolean.FALSE, ucretliIzinGunKod = Boolean.FALSE, ucretsizIzinGunKod = Boolean.FALSE, hastalikIzinGunKod = Boolean.FALSE;

	private Boolean normalGunKod = Boolean.FALSE, haftaTatilGunKod = Boolean.FALSE, resmiTatilGunKod = Boolean.FALSE, artikGunKod = Boolean.FALSE, bordroToplamGunKod = Boolean.FALSE, devredenMesaiKod = Boolean.FALSE, ucretiOdenenKod = Boolean.FALSE;

	private DepartmanDenklestirmeDonemi departmanDenklestirmeDonemi;

	private Tanim gorevYeri, seciliBolum, seciliAltBolum;

	private TreeMap<String, Tanim> ekSahaTanimMap;

	private AylikPuantaj aylikPuantajDefault, personelAylikPuantaj;

	private List<AylikPuantaj> aylikPuantajList, aylikPuantajDosyaList, aylikPuantajMesaiTalepList;

	private DenklestirmeAy denklestirmeAy, denklestirmeOncekiAy, denklestirmeSonrakiAy;

	private PersonelDenklestirme personelDenklestirme;

	private List<SelectItem> ozelDurumList, redNedenleri;

	private Long redNedeniId, mesaiNedenId, seciliEkSaha4Id, gorevTipiId;

	private List<Tanim> gorevYeriTanimList, mesaiIptalNedenTanimList;

	private HashMap<String, Personel> gorevliPersonelMap;

	private double haftaTatilSaat = 0d;

	private List<User> toList, ccList, bccList;

	private File ekliDosya;

	private String mailKonu, mailIcerik;

	private int ay, yil, maxYil, talepOnayDurum, sonDonem;

	private List<SelectItem> aylar, mesaiNedenTanimList;

	private Date haftaTatili;

	private Boolean kaydet, degisti, kullaniciPersonel = false;

	private Departman departman;

	private Tanim planDepartman, ekSaha4Tanim;

	private VardiyaPlan vardiyaPlan;

	private TreeMap<String, VardiyaGun> vardiyalarMap = new TreeMap<String, VardiyaGun>();
	private TreeMap<String, Vardiya> kayitliVardiyalarMap = new TreeMap<String, Vardiya>();
	private TreeMap<Long, List<FazlaMesaiTalep>> vardiyaFazlaMesaiMap = new TreeMap<Long, List<FazlaMesaiTalep>>();

	private boolean stajerSirket;
	private boolean manuelVardiyaIzinGir = false;

	private byte[] mailData;
	private List<Vardiya> izinTipiVardiyaList;
	private TreeMap<String, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap;
	private AramaSecenekleri aramaSecenekleri = null;
	private Notice uyariNot;
	private Session session, sessionHareket;

	@Override
	public Object getId() {
		if (pdksVardiyaGunId == null) {
			return super.getId();
		} else {
			return pdksVardiyaGunId;
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
	 * @param tanim
	 * @return
	 */
	public List<Personel> getGorevPersonelList(Object object) {
		List<Personel> list = null;
		Tanim tanim = null;
		Long key = null;
		if (object != null && object instanceof Tanim) {
			tanim = (Tanim) object;
			key = tanim != null && tanim.getId() != null ? tanim.getId() : null;
		}
		if (gorevPersonelMap != null && key != null && gorevPersonelMap.containsKey(key))
			list = gorevPersonelMap.get(key);

		return list;
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
	 * @param user
	 */
	private void adminRoleDurum(User user) {
		if (user == null)
			user = authenticatedUser;
		adminRole = user != null && (user.isAdmin() || user.isSistemYoneticisi() || user.isIKAdmin());
		ikRole = user != null && (user.isAdmin() || user.isSistemYoneticisi() || user.isIK());
		fazlaMesaiTalepDurum = Boolean.FALSE;
		aylikPuantajListClear();
		if (fazlaMesaiTalepler != null)
			fazlaMesaiTalepler.clear();
		else
			fazlaMesaiTalepler = new ArrayList<FazlaMesaiTalep>();
		if (aylikVardiyaOzetList != null)
			aylikVardiyaOzetList.clear();
		else
			aylikVardiyaOzetList = new ArrayList<VardiyaGun>();
		if (gorevPersonelMap != null)
			gorevPersonelMap.clear();
		else
			gorevPersonelMap = new HashMap<Long, List<Personel>>();

		gorevYeriGirisDurum = false;
	}

	/**
	 * 
	 */
	private void fillEkSahaTanim() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		if (aramaSecenekleri == null)
			aramaSecenekleri = new AramaSecenekleri();
		ortakIslemler.fillEkSahaTanimAramaSecenekAta(session, Boolean.FALSE, Boolean.TRUE, aramaSecenekleri);
		HashMap sonucMap = ortakIslemler.fillEkSahaTanim(session, Boolean.FALSE, null);
		setEkSahaListMap((HashMap<String, List<Tanim>>) sonucMap.get("ekSahaList"));
		setEkSahaTanimMap((TreeMap<String, Tanim>) sonucMap.get("ekSahaTanimMap"));
		bolumAciklama = (String) sonucMap.get("bolumAciklama");
		altBolumAciklama = (String) sonucMap.get("altBolumAciklama");
		tesisAciklama = (String) sonucMap.get("tesisAciklama");
	}

	/**
	 * @return
	 */
	public String kayitGuncelle() {
		setVardiyaVar(Boolean.TRUE);
		return "";
	}

	/**
	 * @param event
	 * @throws Exception
	 */
	public void listener(UploadEvent event) throws Exception {
		if (event != null && event.getUploadItem() != null) {
			UploadItem uploadItem = event.getUploadItem();
			vardiyaPlanDosya.setDosyaAdi(uploadItem.getFileName());
			vardiyaPlanDosya.setDosyaIcerik(uploadItem.getData());
		}

	}

	/**
	 * @return
	 */
	private Vardiya getNormalCalismaVardiya() {
		if (normalCalismaVardiya == null) {
			String idariVardiyaKisaAdi = ortakIslemler.getParameterKey("idariVardiyaKisaAdi");
			normalCalismaVardiya = ortakIslemler.getNormalCalismaVardiya(idariVardiyaKisaAdi, session);
		}
		return normalCalismaVardiya;
	}

	/**
	 * 
	 */
	/**
	 * @param cm
	 * @return
	 */
	public String fillCalismaModeliVardiyaList(CalismaModeli cm) {
		if (cm != null && ikRole)
			calismaModeliVardiyaList = ortakIslemler.fillCalismaModeliVardiyaList(cm, session);
		if (calismaModeliVardiyaList != null && calismaModeliVardiyaList.isEmpty())
			calismaModeliVardiyaList = null;
		return "";
	}

	/**
	 * 
	 */
	public void onUpload() {
		vardiyaPlanDosya.setDosyaIcerik(null);
	}

	/**
	 * @param event
	 */
	public void handleFileUpload(FileUploadEvent event) {
		org.primefaces.model.UploadedFile fileUploaded = event.getFile();
		if (fileUploaded != null) {
			vardiyaPlanDosya.setDosyaAdi(fileUploaded.getFileName());
			vardiyaPlanDosya.setDosyaIcerik(fileUploaded.getContents());
		}

	}

	/**
	 * @return
	 */
	public String modelDegisti() {
		// personelAylikPuantaj
		ArrayList<Vardiya> vardiyalar = fillAylikVardiyaList(personelAylikPuantaj, personelDenklestirme);
		personelDenklestirme.setGuncellendi(true);
		fillCalismaModeliVardiyaList(personelDenklestirme.getCalismaModeliAy() != null ? personelDenklestirme.getCalismaModeliAy().getCalismaModeli() : null);
		for (VardiyaGun pdksVardiyaGun : personelAylikPuantaj.getVardiyalar()) {
			if (pdksVardiyaGun.getVardiya() != null) {
				pdksVardiyaGun.setVardiyalar(pdksVardiyaGun.getIzin() == null ? vardiyalar : null);
				pdksVardiyaGun.setIslemVardiya(null);
				pdksVardiyaGun.setIslendi(false);
			}
		}

		return "";
	}

	/**
	 * @param vg
	 * @return
	 */
	public String getVardiyaFazlaMesailer(VardiyaGun vg) {
		fazlaMesaiTalepler.clear();
		seciliVardiyaGun = vg;
		if (planGirisi && vg != null && vg.getId() != null && vardiyaFazlaMesaiMap != null) {
			List<FazlaMesaiTalep> list = vardiyaFazlaMesaiMap.containsKey(vg.getId()) ? vardiyaFazlaMesaiMap.get(vg.getId()) : new ArrayList<FazlaMesaiTalep>();
			fazlaMesaiTalepler.addAll(list);

		}
		return "";
	}

	/**
	 * @param vg
	 * @return
	 */
	public boolean isVardiyaFazlaMesailer(VardiyaGun vg) {
		boolean fmtVar = false;
		if (vg != null && vg.getId() != null && vardiyaFazlaMesaiMap != null) {
			fmtVar = vardiyaFazlaMesaiMap.containsKey(vg.getId());
		}
		return fmtVar;
	}

	/**
	 * @param vg
	 * @return
	 */
	public String mesaiGuncelle(VardiyaGun vg) {
		seciliVardiyaGun = vg;
		hareketPdksList = null;
		mesaiNedenId = null;
		boolean kullaniciYetkili = denklestirmeAyDurum;
		vg.setKullaniciYetkili(kullaniciYetkili);
		setFazlaMesaiTalep(null);
		if (kullaniciYetkili == false) {
			PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(vg.getVardiyaDate(), "dd MMMMM EEEEE") + " günü vardiyasını güncelleyiniz!");
		} else {
			fazlaMesaiTarihGuncelle = Boolean.FALSE;
			if (vg != null) {
				if (vg.getIslemVardiya() != null)
					fazlaMesaiTarihGuncelle = PdksUtil.tarihKarsilastirNumeric(vg.getIslemVardiya().getVardiyaFazlaMesaiBasZaman(), vg.getIslemVardiya().getVardiyaFazlaMesaiBitZaman()) != 0;
				List<FazlaMesaiTalep> fazlaMesaiTalepler = pdksEntityController.getSQLParamByFieldList(FazlaMesaiTalep.TABLE_NAME, FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN, seciliVardiyaGun.getId(), FazlaMesaiTalep.class, session);
				if (fazlaMesaiTalepler.size() > 1)
					fazlaMesaiTalepler = PdksUtil.sortListByAlanAdi(fazlaMesaiTalepler, "id", Boolean.TRUE);
				vg.setFazlaMesaiTalepler(fazlaMesaiTalepler.isEmpty() ? null : fazlaMesaiTalepler);
				if (denklestirmeAyDurum)
					mesaiEkle(vg);

			}
		}
		aylikPuantajMesaiTalepList = null;
		return "";
	}

	/**
	 * @return
	 */
	public String mesaiSureHesapla() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(fazlaMesaiTalep.getBaslangicZamani());
		cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBasSaat());
		cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBasDakika());
		Date baslangicZamani = cal.getTime();
		cal.setTime(fazlaMesaiTalep.getBitisZamani());
		cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBitSaat());
		cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBitDakika());
		Date bitisZamani = cal.getTime();
		fazlaMesaiTalep.setBaslangicZamani(baslangicZamani);
		fazlaMesaiTalep.setBitisZamani(bitisZamani);
		if (bitisZamani.after(baslangicZamani)) {
			String talepGirisCikisHareketEkleStr = ortakIslemler.getParameterKey("talepGirisCikisHareketEkle");
			boolean kendiGirisYapiyor = false;
			if (talepGirisCikisHareketEkleStr.equals("1")) {
				Personel personel = null;
				Personel girisYapan = authenticatedUser.getPdksPersonel();
				if (aylikPuantajMesaiTalepList == null) {
					if (seciliVardiyaGun != null)
						personel = seciliVardiyaGun.getPersonel();
				} else
					for (AylikPuantaj aylikPuantaj : aylikPuantajMesaiTalepList) {
						Personel pdksPersonel = aylikPuantaj.getPdksPersonel();
						if (pdksPersonel != null && pdksPersonel.getId() != null && girisYapan.getId().equals(pdksPersonel.getId())) {
							personel = pdksPersonel;
							break;
						}
					}
				if (personel != null)
					kendiGirisYapiyor = personel.getId().equals(girisYapan.getId());
			}
			int adet = 0;
			if (kendiGirisYapiyor == false)
				hareketPdksList = null;
			else {
				if (hareketPdksList != null) {
					for (HareketKGS hareketKGS : hareketPdksList) {
						Date zaman = hareketKGS.getOrjinalZaman();
						if (zaman != null && baslangicZamani.getTime() <= zaman.getTime() && bitisZamani.getTime() >= zaman.getTime())
							++adet;
					}
				}
			}
			int yarimYuvarla = seciliVardiyaGun != null ? seciliVardiyaGun.getYarimYuvarla() : PdksUtil.getYarimYuvarlaLast();
			Vardiya islemVardiya = seciliVardiyaGun.getIslemVardiya();
			if (islemVardiya != null && islemVardiya.isCalisma()) {
				if (bitisZamani.getTime() >= islemVardiya.getVardiyaTelorans1BasZaman().getTime() && bitisZamani.getTime() <= islemVardiya.getVardiyaTelorans2BasZaman().getTime())
					bitisZamani = islemVardiya.getVardiyaBasZaman();
				if (bitisZamani.getTime() >= islemVardiya.getVardiyaTelorans1BitZaman().getTime() && bitisZamani.getTime() <= islemVardiya.getVardiyaTelorans2BitZaman().getTime())
					bitisZamani = islemVardiya.getVardiyaBitZaman();
				if (baslangicZamani.getTime() >= islemVardiya.getVardiyaTelorans1BasZaman().getTime() && baslangicZamani.getTime() <= islemVardiya.getVardiyaTelorans2BasZaman().getTime())
					baslangicZamani = islemVardiya.getVardiyaBasZaman();
				if (baslangicZamani.getTime() >= islemVardiya.getVardiyaTelorans1BitZaman().getTime() && baslangicZamani.getTime() <= islemVardiya.getVardiyaTelorans2BitZaman().getTime())
					baslangicZamani = islemVardiya.getVardiyaBitZaman();
			}
			double toplamSure = PdksUtil.setSureDoubleTypeRounded(PdksUtil.getSaatFarki(bitisZamani, baslangicZamani).doubleValue(), yarimYuvarla);
			double farkSure = 0;

			if (islemVardiya != null && islemVardiya.isCalisma() && islemVardiya.getVardiyaFazlaMesaiBasZaman().getTime() <= baslangicZamani.getTime() && islemVardiya.getVardiyaFazlaMesaiBitZaman().getTime() >= bitisZamani.getTime()) {
				if (bitisZamani.getTime() < islemVardiya.getVardiyaTelorans2BitZaman().getTime() && baslangicZamani.getTime() > islemVardiya.getVardiyaTelorans1BasZaman().getTime()) {
					farkSure = toplamSure;
				} else if (bitisZamani.getTime() < islemVardiya.getVardiyaTelorans1BitZaman().getTime() && bitisZamani.getTime() > islemVardiya.getVardiyaTelorans1BasZaman().getTime()) {
					Date bit1 = bitisZamani;
					Date bas1 = islemVardiya.getVardiyaBasZaman();
					farkSure = PdksUtil.setSureDoubleTypeRounded(PdksUtil.getSaatFarki(bit1, bas1).doubleValue(), yarimYuvarla);
					if (farkSure < 0.0d)
						farkSure = 0.0d;
				} else if (baslangicZamani.getTime() > islemVardiya.getVardiyaTelorans1BasZaman().getTime() && baslangicZamani.getTime() < islemVardiya.getVardiyaTelorans2BitZaman().getTime()) {
					Date bit1 = islemVardiya.getVardiyaBitZaman();
					Date bas1 = baslangicZamani;
					farkSure = PdksUtil.setSureDoubleTypeRounded(PdksUtil.getSaatFarki(bit1, bas1).doubleValue(), yarimYuvarla);
					if (farkSure < 0.0d)
						farkSure = 0.0d;
				} else if (baslangicZamani.getTime() < islemVardiya.getVardiyaTelorans1BasZaman().getTime() && bitisZamani.getTime() > islemVardiya.getVardiyaTelorans2BitZaman().getTime()) {
					Date bit1 = islemVardiya.getVardiyaBitZaman();
					Date bas1 = islemVardiya.getVardiyaBasZaman();
					farkSure = PdksUtil.setSureDoubleTypeRounded(PdksUtil.getSaatFarki(bit1, bas1).doubleValue(), yarimYuvarla);
					if (farkSure < 0.0d)
						farkSure = 0.0d;
				}
			}
			fazlaMesaiTalep.setMesaiSuresi(toplamSure - farkSure);
			if (kendiGirisYapiyor) {
				if (adet < 2) {
					if (manuelHareketEkle == null || !manuelHareketEkle)
						manuelHareketEkle = fazlaMesaiTalep.getMesaiSuresi().doubleValue() > 0.0d;
				} else
					manuelHareketEkle = false;

			} else
				manuelHareketEkle = null;

		} else
			fazlaMesaiTalep.setMesaiSuresi(0.0d);
		if (manuelHareketEkle != null && manuelHareketEkle) {
			Date simdi = new Date();
			manuelHareketEkle = bitisZamani.getTime() < simdi.getTime();
		}
		return "";
	}

	/**
	 * @return
	 */
	public String mesaiOnayDurumDegisti() {
		for (FazlaMesaiTalep fmt : aylikFazlaMesaiTalepler) {
			fmt.setCheckBoxDurum(islemYapiliyor && fmt.isIslemYapildi());
		}
		return "";

	}

	/**
	 * @param yilDegisti
	 * @return
	 * @throws Exception
	 */
	public String mesaiDonemDegisti(boolean yilDegisti) throws Exception {
		Date basTarih = null, bitTarih = null;
		StringBuffer sb = new StringBuffer();
		HashMap fields = new HashMap();
		List idler = null;
		Calendar cal = Calendar.getInstance();
		if (yilDegisti) {
			basTarih = PdksUtil.convertToJavaDate(((yil * 100) + 1) + "01", "yyyyMMdd");
			bitTarih = PdksUtil.convertToJavaDate(((yil * 100) + 12) + "31", "yyyyMMdd");
			sb.append("select distinct MONTH(" + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + ") from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " FT " + PdksEntityController.getJoinLOCK() + " on FT." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V." + VardiyaGun.COLUMN_NAME_ID);
			sb.append(" and FT." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :ta1 and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= :ta2");
			sb.append(" order by 1");
			fields.put("ta1", basTarih);
			fields.put("ta2", bitTarih);
			idler = pdksEntityController.getObjectBySQLList(sb, fields, null);
			int seciliAy = ay;
			aylar.clear();
			ay = 0;
			for (Iterator iterator = idler.iterator(); iterator.hasNext();) {
				Integer object = (Integer) iterator.next();
				if (object.intValue() == seciliAy)
					ay = seciliAy;
				cal.set(Calendar.MONTH, object.intValue() - 1);
				cal.set(Calendar.YEAR, yil);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				String ayAdi = PdksUtil.convertToDateString(cal.getTime(), "MMMM");
				aylar.add(new SelectItem(object.intValue(), ayAdi));
			}
		}

		gunler = ortakIslemler.getSelectItemList("gun", authenticatedUser);

		if (ay == 0 && !aylar.isEmpty())
			ay = (Integer) aylar.get(aylar.size() - 1).getValue();
		basTarihStr = "";
		bitTarihStr = "";
		if (ay > 0) {
			basTarih = PdksUtil.convertToJavaDate(((yil * 100) + ay) + "01", "yyyyMMdd");
			cal.setTime(basTarih);
			int sonGun = cal.getActualMaximum(Calendar.DATE);
			cal.set(Calendar.DATE, sonGun);
			bitTarih = cal.getTime();
			sb = new StringBuffer();
			fields.clear();
			sb.append("select distinct " + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " FT " + PdksEntityController.getJoinLOCK() + " on FT." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V." + VardiyaGun.COLUMN_NAME_ID);
			sb.append(" and FT." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :ta1 and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= :ta2");
			sb.append(" order by V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI);
			fields.put("ta1", basTarih);
			fields.put("ta2", bitTarih);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);

			try {
				idler = pdksEntityController.getObjectBySQLList(sb, fields, null);
			} catch (Exception e) {
				logger.equals(e);
				e.printStackTrace();
			}
			for (Iterator iterator = idler.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				String str = "";
				if (object instanceof Timestamp)
					str = PdksUtil.convertToDateString(new Date(((Timestamp) object).getTime()), PdksUtil.getDateFormat());
				else if (object instanceof Date)
					str = PdksUtil.convertToDateString((Date) object, PdksUtil.getDateFormat());

				if (PdksUtil.hasStringValue(str))
					gunler.add(new SelectItem(str, str));
			}
			if (!idler.isEmpty()) {
				basTarihStr = gunler.get(0).getLabel();
				bitTarihStr = gunler.get(gunler.size() - 1).getLabel();
			}
		}
		aylikFazlaMesaiTalepler.clear();
		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public String mesaiOnay() throws Exception {
		boolean guncelle = false;
		for (FazlaMesaiTalep fmt : aylikFazlaMesaiTalepler) {
			if (fmt.isCheckBoxDurum()) {
				fmt.setGuncelleyenUser(authenticatedUser);
				fmt.setOnayDurumu(FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI);
				fmt.setGuncellemeTarihi(new Date());
				saveOrUpdate(fmt);
				sessionFlush();
				setIslemFazlaMesaiTalep(fmt);
				mesaiMudurOnayCevabi(true);
				fmt.setCheckBoxDurum(false);
				guncelle = true;
			}

		}
		if (guncelle)
			fillAylikMesaiTalepList();
		else
			PdksUtil.addMessageAvailableWarn("Seçili kayıt yok!");
		return "";
	}

	/**
	 * @param fmt
	 * @return
	 */
	public String mesaiIptalSec(FazlaMesaiTalep fmt) {
		setIslemFazlaMesaiTalep(fmt);
		mesaiIptalNedenTanimList = ortakIslemler.getTanimList(Tanim.TIPI_FAZLA_MESAI_IPTAL_NEDEN, session);
		return "";

	}

	/**
	 * @return
	 */
	@Transactional
	public String mesaiIptal() {
		islemFazlaMesaiTalep.setDurum(Boolean.FALSE);
		saveOrUpdate(islemFazlaMesaiTalep);
		sessionFlush();

		if (islemFazlaMesaiTalep.getOnayDurumu() == FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI)
			mesaiMudurOnayCevabi(false);
		String talepGirisCikisHareketEkleStr = ortakIslemler.getParameterKey("talepGirisCikisHareketEkle");
		if (talepGirisCikisHareketEkleStr.equals("1"))
			talepGirisCikisHareketEkle();
		islemFazlaMesaiTalep = null;
		if (seciliVardiyaGun != null)
			mesaiGuncelle(seciliVardiyaGun);
		else
			try {
				if (authenticatedUser.getCalistigiSayfa() != null && authenticatedUser.getCalistigiSayfa().equals("fazlaMesaiTalep"))
					fillFazlaMesaiTalepList();
				else
					fillAylikMesaiTalepList();
			} catch (Exception e) {

			}

		return "";
	}

	/**
	 * @param durum
	 * @return
	 */
	public String getOnayId(String durum) {
		String id = ortakIslemler.getEncodeStringByBase64("&fmtId=" + islemFazlaMesaiTalep.getId() + "&durum=" + durum);
		return id;

	}

	/**
	 * @param basTarih
	 * @param bitTarih
	 * @return
	 */
	private String getTarihArasiBitisZamanString(Date basTarih, Date bitTarih) {
		String str = "";
		if (basTarih != null && bitTarih != null && !basTarih.after(bitTarih)) {
			String pattern = PdksUtil.getDateTimeFormat();
			if (PdksUtil.tarihKarsilastirNumeric(basTarih, bitTarih) == 0) {
				pattern = PdksUtil.getSaatFormat();
			}
			str = " - " + PdksUtil.convertToDateString(bitTarih, pattern);
		}

		return str;
	}

	/**
	 * @param fmt
	 * @return
	 */
	public String mesaiMailHatirlatma(FazlaMesaiTalep fmt) {
		setIslemFazlaMesaiTalep(fmt);
		Map<String, String> map = null;
		try {
			map = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();

		} catch (Exception e) {
		}
		setDonusAdres(map.containsKey("host") ? map.get("host") : "");
		boolean test = donusAdres.indexOf("localhost:") >= 0;
		if (toList == null)
			toList = new ArrayList<User>();
		else
			toList.clear();
		if (!test)
			toList.add(fmt.getGuncelleyenUser());
		else
			toList.add(authenticatedUser);
		if (bccList == null)
			bccList = new ArrayList<User>();
		else
			bccList.clear();
		String servisMailAdresleri = ortakIslemler.getParameterKey("bccAdres");
		if (servisMailAdresleri != null && servisMailAdresleri.indexOf("@") > 1) {
			List<String> mailList = PdksUtil.getListFromString(servisMailAdresleri, null);
			for (String mail : mailList) {
				User user = new User();
				user.setEmail(mail);
				bccList.add(user);
			}
		}
		VardiyaGun vg = fmt.getVardiyaGun();
		Personel personel = vg.getPersonel();
		Sirket sirket = personel.getSirket();
		Tanim tesis = sirket.isTesisDurumu() ? personel.getTesis() : null;
		Tanim bolum = personel.getEkSaha3();
		Tanim altBolum = getPersonelAltBolumu(personel);
		if (tesis != null && tesisAciklama == null && tesis.getParentTanim() == null)
			fillEkSahaTanim();
		String tesisAciklamasi = tesis != null ? getAciklamasi(tesis, tesisAciklama) : "";
		String bolumAciklamasi = getAciklamasi(bolum, bolumAciklama);
		String altBolumAciklamasi = altBolum != null ? getAciklamasi(altBolum, altBolumAciklama) : null;
		String ortakAciklama = PdksUtil.replaceAll(sirket.getAd() + " " + ortakIslemler.sirketAciklama().toLowerCase(PdksUtil.TR_LOCALE) + "i " + (tesis != null ? tesis.getAciklama() + " " + tesisAciklamasi + " " : "") + " " + (bolum != null ? bolum.getAciklama() + " " + bolumAciklamasi + " " : "")
				+ " " + (altBolum != null ? altBolum.getAciklama() + " " + altBolumAciklamasi + " " : "") + " çalışanı " + personel.getAdSoyad(), "  ", " ");
		mailKonu = PdksUtil.replaceAll(ortakAciklama + " fazla mesai talep ", "  ", " ");
		StringBuffer sb = new StringBuffer();
		sb.append("<p>Sayın " + fmt.getGuncelleyenUser().getAdSoyad() + ",</p>");
		sb.append("<p>" + ortakAciklama + " " + authenticatedUser.dateTimeFormatla(fmt.getBaslangicZamani()) + getTarihArasiBitisZamanString(fmt.getBaslangicZamani(), fmt.getBitisZamani()) + " arası " + authenticatedUser.sayiFormatliGoster(fmt.getMesaiSuresi()) + " saat ");
		sb.append((fmt.getMesaiNeden() != null ? "<b>\"" + fmt.getMesaiNeden().getAciklama() + (PdksUtil.hasStringValue(fmt.getAciklama()) ? " ( Açıklama : " + fmt.getAciklama().trim() + " ) " : "") + "\"</b> nedeniyle " : "") + " fazla mesai yapacaktır." + "</p>");
		mailIcerik = PdksUtil.replaceAll(sb.toString(), "  ", " ");
		try {
			MailStatu mailSatu = null;
			try {
				MailObject mail = new MailObject();
				mail.setSubject(mailKonu);
				StringBuffer body = new StringBuffer(mailIcerik);
				body.append("<p><TABLE style=\"width: 270px;\"><TR>");
				body.append("<td width=\"90px\"><a style=\"font-size: 16px;\" href=\"http://" + donusAdres + "/mesaiTalepLinkOnay?id=" + getOnayId(String.valueOf(FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI)) + "\"><b>Onay</b></a></td>");
				body.append("<td width=\"90px\"><a style=\"font-size: 16px;\" href=\"http://" + donusAdres + "/mesaiTalepLinkOnay?id=" + getOnayId(String.valueOf(FazlaMesaiTalep.ONAY_DURUM_RED)) + "\"><b>Red</b></a></td>");
				body.append("</TR></TABLE></p>");
				mail.setBody(body.toString());
				body = null;
				ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
				mailSatu = ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/fazlaMesaiTalepMail.xhtml", session);
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				PdksUtil.addMessageError(e.getMessage());
			}
			if (mailSatu != null && mailSatu.getDurum())
				PdksUtil.addMessageAvailableInfo(personel.getAdSoyad() + " " + authenticatedUser.getTarihFormatla(vg.getVardiyaDate(), PdksUtil.getDateFormat()) + " günü " + authenticatedUser.sayiFormatliGoster(fmt.getMesaiSuresi()) + " saat  fazla mesai talep mesajı "
						+ fmt.getGuncelleyenUser().getAdSoyad() + " gönderildi.");
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * @param tanim
	 * @param ozelAciklama
	 * @return
	 */
	private String getAciklamasi(Tanim tanim, String ozelAciklama) {
		String aciklama = null;
		if (tanim != null && tanim.getParentTanim() != null)
			aciklama = tanim.getParentTanim().getAciklama();
		if (ozelAciklama != null && PdksUtil.hasStringValue(aciklama) == false)
			aciklama = ozelAciklama;
		if (aciklama != null)
			aciklama = aciklama.trim().toLowerCase(PdksUtil.TR_LOCALE);
		return aciklama;
	}

	/**
	 * @param personel
	 * @return
	 */
	private Tanim getPersonelAltBolumu(Personel personel) {
		Tanim bolumu = null, altBolum = personel.getEkSaha4();
		boolean altBolumVar = false;
		try {
			bolumu = (Tanim) personel.getEkSaha3().clone();
		} catch (Exception e) {
		}
		if (PdksUtil.isPuantajSorguAltBolumGir() && altBolum != null) {
			if (!bolumu.getAciklama().equals(altBolum.getAciklama()))
				altBolumVar = true;
		}
		bolumu = null;
		if (altBolumVar) {
			bolumu = altBolum;
		}
		return bolumu;
	}

	/**
	 * @param onayDurum
	 * @return
	 */
	public String mesaiMudurOnayCevabi(boolean onayDurum) {
		uyariNot = ortakIslemler.getNotice(NoteTipi.MAIL_CEVAPLAMAMA.value(), Boolean.TRUE, session);

		FazlaMesaiTalep fmt = islemFazlaMesaiTalep;
		if (toList == null)
			toList = new ArrayList<User>();
		else
			toList.clear();
		toList.add(fmt.getOlusturanUser());
		if (ccList == null)
			ccList = new ArrayList<User>();
		else
			ccList.clear();
		if (onayDurum)
			if (ortakIslemler.getParameterKey("fazlaMesaiMudurOnayCevabiIK").equals("1"))
				ortakIslemler.IKKullanicilariBul(ccList, fmt.getVardiyaGun().getPersonel(), session);

		if (bccList == null)
			bccList = new ArrayList<User>();
		else
			bccList.clear();
		String servisMailAdresleri = ortakIslemler.getParameterKey("bccAdres");
		if (servisMailAdresleri != null && servisMailAdresleri.indexOf("@") > 1) {
			List<String> mailList = PdksUtil.getListFromString(servisMailAdresleri, null);
			for (String mail : mailList) {
				User user = new User();
				user.setEmail(mail);
				bccList.add(user);
			}

		}

		VardiyaGun vg = fmt.getVardiyaGun();
		Personel personel = vg.getPersonel();
		Sirket sirket = personel.getSirket();
		Tanim tesis = sirket.isTesisDurumu() ? personel.getTesis() : null;
		Tanim bolum = personel.getEkSaha3();
		if (tesis != null && tesisAciklama == null && tesis.getParentTanim() == null)
			fillEkSahaTanim();
		Tanim altBolum = getPersonelAltBolumu(personel);
		String tesisAciklamasi = tesis != null ? getAciklamasi(tesis, tesisAciklama) : "";
		String bolumAciklamasi = getAciklamasi(bolum, bolumAciklama);
		String altBolumAciklamasi = altBolum != null ? getAciklamasi(altBolum, altBolumAciklama) : null;
		String ortakAciklama = PdksUtil.replaceAll(sirket.getAd() + " " + ortakIslemler.sirketAciklama().toLowerCase(PdksUtil.TR_LOCALE) + "i " + (tesis != null ? tesis.getAciklama() + " " + tesisAciklamasi + " " : "") + " " + (bolum != null ? bolum.getAciklama() + " " + bolumAciklamasi + " " : "")
				+ " " + (altBolum != null ? altBolum.getAciklama() + " " + altBolumAciklamasi + " " : "") + " çalışanı " + personel.getAdSoyad(), "  ", " ");
		mailKonu = PdksUtil.replaceAll(ortakAciklama + " Fazla Mesai Talep " + fmt.getOnayDurumAciklama(), "  ", " ");
		StringBuffer sb = new StringBuffer();
		sb.append("<p>Sayın " + fmt.getOlusturanUser().getAdSoyad() + ",</p>");
		sb.append("<p>" + ortakAciklama + " " + fmt.getOlusturanUser().dateTimeFormatla(fmt.getBaslangicZamani()) + getTarihArasiBitisZamanString(fmt.getBaslangicZamani(), fmt.getBitisZamani()) + " arası " + fmt.getOlusturanUser().sayiFormatliGoster(fmt.getMesaiSuresi()) + " saat ");
		sb.append((fmt.getMesaiNeden() != null ? "<b>\"" + fmt.getMesaiNeden().getAciklama() + (PdksUtil.hasStringValue(fmt.getAciklama()) ? " ( Açıklama : " + fmt.getAciklama().trim() + " ) " : "") + "\"</b>" : " olması sebebiyle ") + " fazla mesai talebi "
				+ (onayDurum ? "onaylandı" : (fmt.getRedNedeni() != null ? "<b>\"" + fmt.getRedNedeni().getAciklama() + "\"</b> nedeniyle " : "") + (fmt.getDurum() ? " rededildi" : " iptal edildi")) + ".</p>");
		if (PdksUtil.hasStringValue(fmt.getIptalAciklama())) {
			sb.append("<p><b>İptal açıklama : " + "</b>" + fmt.getIptalAciklama().trim() + "</p>");
		}
		if (uyariNot != null)
			sb.append(uyariNot.getValue());
		mailIcerik = PdksUtil.replaceAll(sb.toString(), "  ", " ");

		MailStatu mailSatu = null;
		try {
			MailObject mail = new MailObject();
			ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
			ortakIslemler.addMailPersonelUserList(ccList, mail.getCcList());
			mail.setSubject(mailKonu);
			mail.setBody(mailIcerik);
			if (!mail.getToList().isEmpty() || !mail.getCcList().isEmpty())
				mailSatu = ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/fazlaMesaiTalepCevapMail.xhtml", session);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			PdksUtil.addMessageError(e.getMessage());
		}
		if (mailSatu != null && mailSatu.getDurum())
			PdksUtil.addMessageAvailableInfo(fmt.getOlusturanUser().getAdSoyad() + " fazla mesai talep cevabı gönderildi.");

		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String mesaiRedIslemi() {

		Tanim redNedeni = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, redNedeniId, Tanim.class, session);
		islemFazlaMesaiTalep.setRedNedeni(redNedeni);
		islemFazlaMesaiTalep.setOnayDurumu(FazlaMesaiTalep.ONAY_DURUM_RED);
		islemFazlaMesaiTalep.setGuncellemeTarihi(new Date());
		saveOrUpdate(islemFazlaMesaiTalep);
		sessionFlush();
		mesaiMudurOnayCevabi(false);
		mesaiIptalNedenTanimList = null;
		return "";
	}

	/**
	 * @param topluGuncelle
	 * @return
	 */
	@Transactional
	public String mesaiKaydet(boolean topluGuncelle) {
		Vardiya islemVardiya = seciliVardiyaGun.getIslemVardiya();
		if (islemVardiya != null) {
			boolean devam = true;
			String anaMesaj = seciliVardiyaGun.getPersonel().getPdksSicilNo() + " " + seciliVardiyaGun.getPersonel().getAdSoyad();
			Tanim mesaiNeden = ortakIslemler.getTanimById(mesaiNedenId, session);
			fazlaMesaiTalep.setMesaiNeden(mesaiNeden);
			if (fazlaMesaiTalep.getMesaiNeden() == null) {
				PdksUtil.addMessageAvailableWarn("Mesai nedeni seçiniz!");
				devam = false;
			} else {
				Calendar cal = Calendar.getInstance();
				cal.setTime(fazlaMesaiTalep.getBaslangicZamani());
				cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBasSaat());
				cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBasDakika());
				Date baslangicZamani = cal.getTime();
				cal.setTime(fazlaMesaiTalep.getBitisZamani());
				cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBitSaat());
				cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBitDakika());
				Date bitisZamani = cal.getTime();
				fazlaMesaiTalep.setBaslangicZamani(baslangicZamani);
				fazlaMesaiTalep.setBitisZamani(bitisZamani);
				if (seciliVardiyaGun.getVersion() >= 0) {
					if (islemVardiya.isCalisma()) {
						boolean o1 = islemVardiya.getVardiyaFazlaMesaiBasZaman() == null || islemVardiya.getVardiyaFazlaMesaiBasZaman().getTime() <= baslangicZamani.getTime();
						boolean o2 = islemVardiya.getVardiyaBasZaman().getTime() >= bitisZamani.getTime();
						boolean oncekiVardiyaSinirda = o1 && o2;
						boolean s1 = islemVardiya.getVardiyaFazlaMesaiBitZaman() == null || islemVardiya.getVardiyaFazlaMesaiBitZaman().getTime() > bitisZamani.getTime();
						boolean s2 = baslangicZamani.getTime() >= islemVardiya.getVardiyaBitZaman().getTime();
						boolean sonrakiVardiyaSinirda = s1 && s2;
						if (!oncekiVardiyaSinirda && !sonrakiVardiyaSinirda) {
							PdksUtil.addMessageAvailableWarn(anaMesaj + " mesai başlangıç ve bitiş zamanı hatalıdır");
							devam = false;
						}
					} else if (baslangicZamani.before(islemVardiya.getVardiyaFazlaMesaiBasZaman()) || bitisZamani.after(islemVardiya.getVardiyaFazlaMesaiBitZaman())) {
						PdksUtil.addMessageAvailableWarn(anaMesaj + " mesai başlangıç ve bitiş zamanı hatalıdır");
						devam = false;
					}
				}
			}
			if (authenticatedUser.isAdmin())
				devam = false;
			if (devam) {
				HashMap fields = new HashMap();
				if (fazlaMesaiTalep.getId() != null)
					fields.put("id<>", fazlaMesaiTalep.getId());
				fields.put("vardiyaGun.id=", seciliVardiyaGun.getId());
				fields.put("baslangicZamani<", fazlaMesaiTalep.getBitisZamani());
				fields.put("bitisZamani>", fazlaMesaiTalep.getBaslangicZamani());
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<FazlaMesaiTalep> list = pdksEntityController.getObjectByInnerObjectListInLogic(fields, FazlaMesaiTalep.class);
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					FazlaMesaiTalep fazlaMesaiTalep = (FazlaMesaiTalep) iterator.next();
					if (fazlaMesaiTalep.getOnayDurumu() == FazlaMesaiTalep.ONAY_DURUM_RED || fazlaMesaiTalep.getDurum().equals(Boolean.FALSE))
						iterator.remove();

				}
				if (list.isEmpty()) {
					String str = anaMesaj + " ait " + authenticatedUser.dateTimeFormatla(fazlaMesaiTalep.getBaslangicZamani()) + getTarihArasiBitisZamanString(fazlaMesaiTalep.getBaslangicZamani(), fazlaMesaiTalep.getBitisZamani()) + " arası "
							+ authenticatedUser.sayiFormatliGoster(fazlaMesaiTalep.getMesaiSuresi()) + " saat mesai ";
					if (fazlaMesaiTalep.getOlusturanUser().getId().equals(fazlaMesaiTalep.getGuncelleyenUser().getId())) {
						fazlaMesaiTalep.setOnayDurumu(FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI);
						fazlaMesaiTalep.setGuncellemeTarihi(new Date());
					}
					saveOrUpdate(fazlaMesaiTalep);
					sessionFlush();
					mesaiMailHatirlatma(fazlaMesaiTalep);
					if (manuelHareketEkle != null && manuelHareketEkle)
						talepGirisCikisHareketEkle();
					PdksUtil.addMessageAvailableInfo(str + fazlaMesaiTalep.getGuncelleyenUser().getAdSoyad() + " tarafından onaylanacaktır.");
					if (!topluGuncelle)
						mesaiGuncelle(seciliVardiyaGun);
					else
						fazlaMesaiTalep = null;
				} else {
					FazlaMesaiTalep mesaiTalep = list.get(0);
					String str = authenticatedUser.dateTimeFormatla(mesaiTalep.getBaslangicZamani()) + getTarihArasiBitisZamanString(mesaiTalep.getBaslangicZamani(), mesaiTalep.getBitisZamani()) + " arası " + authenticatedUser.sayiFormatliGoster(mesaiTalep.getMesaiSuresi()) + " saat mesai ";
					if (mesaiTalep.getOnayDurumu() == FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI)
						PdksUtil.addMessageWarn(str + " daha önce " + mesaiTalep.getGuncelleyenUser().getAdSoyad() + " tarafından onaylanmıştır.");
					else
						PdksUtil.addMessageWarn(str + " daha önce " + mesaiTalep.getGuncelleyenUser().getAdSoyad() + " onaylanması beklenmektedir!");
				}

			}
		}

		return "";
	}

	/**
	 * @param vg
	 * @return
	 */
	public String mesaiEkle(VardiyaGun vg) {
		FazlaMesaiTalep fmt = null;
		Personel mudur = personelAylikPuantaj.getYonetici2();
		if (vg != null && mudur != null) {
			User mudurKullanici = null;
			if (mudur.getId() != null) {
				if (mudur.isCalisiyor()) {

					mudurKullanici = (User) pdksEntityController.getSQLParamByFieldObject(User.TABLE_NAME, User.COLUMN_NAME_PERSONEL, mudur.getId(), User.class, session);

					if (mudurKullanici != null && !mudurKullanici.isDurum())
						mudurKullanici = null;

					if (mudurKullanici == null)
						PdksUtil.addMessageAvailableWarn(mudur.getAdSoyad() + " personelin aktif PDKS kullanıcı yoktur!");
				}
			}
			if (mudurKullanici != null) {
				fmt = new FazlaMesaiTalep(vg);
				fmt.setOlusturanUser(authenticatedUser);
				fmt.setGuncelleyenUser(mudurKullanici);
				mesaiNedenTanimList = ortakIslemler.getTanimSelectItem("mesaiNeden", ortakIslemler.getTanimList(Tanim.TIPI_FAZLA_MESAI_NEDEN, session));
				if (seciliVardiyaGun.getVardiya().isCalisma()) {
					Vardiya islemVardiya = seciliVardiyaGun.getIslemVardiya();
					fmt.setBaslangicZamani(islemVardiya.getVardiyaBitZaman());
					fmt.setBitisZamani(islemVardiya.getVardiyaBitZaman());
					Calendar cal = Calendar.getInstance();
					cal.setTime(fmt.getBaslangicZamani());
					fmt.setBasSaat(cal.get(Calendar.HOUR_OF_DAY));
					fmt.setBasDakika(cal.get(Calendar.MINUTE));
					cal.setTime(fmt.getBitisZamani());
					fmt.setBitSaat(cal.get(Calendar.HOUR_OF_DAY));
					fmt.setBitDakika(cal.get(Calendar.MINUTE));
				}
			}
		}

		setFazlaMesaiTalep(fmt);
		manuelHareketEkle = null;
		if (seciliVardiyaGun != null && seciliVardiyaGun.getIslemVardiya() != null) {
			Personel girisYapan = authenticatedUser.getPdksPersonel(), personel = vg.getPersonel();
			if (personel != null && girisYapan.getId().equals(personel.getId())) {
				Vardiya islemVardiya = seciliVardiyaGun.getIslemVardiya();
				Date basTarih = islemVardiya.getVardiyaFazlaMesaiBasZaman();
				Date bitTarih = islemVardiya.getVardiyaFazlaMesaiBitZaman();
				hareketPdksList = getPdksHareketler(girisYapan.getPersonelKGS().getId(), basTarih, bitTarih);
				manuelHareketEkle = hareketPdksList.isEmpty();
			}
		}
		return "";
	}

	/**
	 * @param tarih
	 * @return
	 */
	public String bitTarihGuncelle(Date tarih) {
		Calendar cal = Calendar.getInstance();
		if (Calendar.MONDAY == PdksUtil.getDateField(tarih, Calendar.DAY_OF_WEEK))
			setBitTarih(ortakIslemler.tariheGunEkleCikar(cal, tarih, 13));
		else
			setBitTarih(null);
		return "";
	}

	/**
	 * 
	 */
	public void vardiyaSablonuEkle() {
		this.clearInstance();
		setVardiyaList(new ArrayList<Vardiya>());
		setVardiyaVar(Boolean.FALSE);
	}

	/**
	 * @param seciliVardiyaGun
	 * @return
	 */
	public String vardiyaDegistir(VardiyaGun seciliVardiyaGun) {
		if (personelAylikPuantaj != null)
			personelAylikPuantaj.setVardiyaDegisti(Boolean.TRUE);
		vardiyalarMap.put(seciliVardiyaGun.getVardiyaDateStr(), seciliVardiyaGun);
		seciliVardiyaGun.setIslemVardiya(null);
		seciliVardiyaGun.setGuncellendi(Boolean.TRUE);
		seciliVardiyaGun.setHareketHatali(Boolean.FALSE);
		seciliVardiyaGun.setBasSaat(null);
		seciliVardiyaGun.setBasDakika(null);
		seciliVardiyaGun.setBitSaat(null);
		seciliVardiyaGun.setBitDakika(null);
		Personel personel = personelAylikPuantaj.getPdksPersonel();
		PersonelDenklestirme seciliPersonelDenklestirme = personelAylikPuantaj.getPersonelDenklestirme();
		boolean sutIzin = personel.isSutIzniKullan() || seciliPersonelDenklestirme.isSutIzniVar();
		if (!sutIzin && AylikPuantaj.getGebelikGuncelle()) {
			if (seciliVardiyaGun.getVardiya().isGebelikMi() || seciliVardiyaGun.isGebeMi())
				gebeSutIzniGuncelle = true;
			else {
				gebeSutIzniGuncelle = false;
				for (VardiyaGun pdksVardiyaGun : personelAylikPuantaj.getVardiyalar()) {
					if (!gebeSutIzniGuncelle && pdksVardiyaGun.isAyinGunu() && pdksVardiyaGun.getVardiya() != null)
						gebeSutIzniGuncelle = pdksVardiyaGun.getIzin() == null && pdksVardiyaGun.getVardiya().getGebelik();
				}
			}
		}
		return "";
	}

	/**
	 * @param aylikPuantaj
	 * @param pdksVardiyaGun
	 * @return
	 */
	public String gorevYeriDegistir(AylikPuantaj aylikPuantaj, VardiyaGun pdksVardiyaGun) {
		personelAylikPuantaj = aylikPuantaj;
		seciliVardiyaGun = pdksVardiyaGun;

		return "";
	}

	/**
	 * @param pdksVardiyaGun
	 * @return
	 */
	public String gorevYeriDegistir(VardiyaGun pdksVardiyaGun) {
		seciliVardiyaGun = pdksVardiyaGun;

		return "";
	}

	/**
	 * @param vardiyaHafta
	 */
	public void sablonDegistir(VardiyaHafta vardiyaHafta) {

	}

	/**
	 * @param durum
	 * @param list
	 * @return
	 */
	public boolean veriGoster(String durum, List list) {
		boolean goster = false;

		if (PdksUtil.hasStringValue(durum)) {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				if (durum.equals("suaDurum")) {
					if (aylikPuantaj.getPersonelDenklestirme() != null && aylikPuantaj.getPersonelDenklestirme().getSuaDurum() != null) {
						goster = true;
						break;
					}

				} else if (durum.equals("aylikFazlaMesai")) {
					if (aylikPuantaj.getAylikNetFazlaMesai() != 0d) {
						goster = true;
						break;
					}

				} else if (durum.equals("gecenAyFazlaMesai")) {
					if (aylikPuantaj.getGecenAyFazlaMesai().doubleValue() != 0d) {
						goster = true;
						break;
					}
				} else if (durum.equals("gecenDonem")) {
					if (aylikPuantaj.getPersonelDenklestirmeGecenAylik() != null && aylikPuantaj.getPersonelDenklestirmeGecenAylik().getId() != null && aylikPuantaj.getPersonelDenklestirmeGecenAylik().getHesaplananSure() != 0) {
						goster = true;
						break;
					}
				} else if (durum.equals("fiiliDonem")) {
					if (aylikPuantaj.getPersonelDenklestirme() != null && aylikPuantaj.getPersonelDenklestirme().getId() != null && aylikPuantaj.getPersonelDenklestirme().getHesaplananSure() != 0) {
						goster = true;
						break;
					}
				}

			}
		}
		return goster;
	}

	/**
	 * @param vardiyaGun
	 * @param vardiyalar
	 * @param tatilVardiyalar
	 * @return
	 */
	public VardiyaGun vardiyaListeDoldur(VardiyaGun vardiyaGun, ArrayList<Vardiya> vardiyalar, ArrayList<Vardiya> tatilVardiyalar) {
		if (vardiyaGun.getVardiya() != null) {
			if (vardiyaGun.getTatil() == null || vardiyaGun.getTatil().isYarimGunMu())
				vardiyaGun.setVardiyalar(vardiyalar);
			else
				vardiyaGun.setVardiyalar(tatilVardiyalar);
			try {
				Departman departman = vardiyaGun.getPersonel().getSirket().getDepartman();
				if (departman != null) {
					ArrayList<Vardiya> perVardiyalar = vardiyaGun.getVardiyalar();
					for (Iterator iterator = perVardiyalar.iterator(); iterator.hasNext();) {
						Vardiya pdksVardiya = (Vardiya) iterator.next();
						if (!pdksVardiya.getDurum() || (pdksVardiya.getDepartman() != null && !pdksVardiya.getDepartman().getId().equals(departman.getId())))
							iterator.remove();

					}
				}

			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
				logger.error("vardiyaListeDoldur : " + e.getMessage());
			}

		}
		return vardiyaGun;
	}

	/**
	 * @param plan
	 * @param hafta
	 */
	private void haftaListesiOlustur(VardiyaPlan plan, VardiyaHafta hafta) {
		ArrayList<Vardiya> tatilVardiyaList = fillVardiyaDoldurList(hafta.getVardiyaSablonu(), plan.getPersonel());
		ArrayList<Vardiya> vardiyaList = new ArrayList<Vardiya>();
		for (Vardiya vardiya : tatilVardiyaList)

			vardiyaList.add(vardiya);
		hafta.setVardiyalar(vardiyaList);
		hafta.getVardiyaGunler().clear();
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun1(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun2(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun3(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun4(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun5(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun6(), vardiyaList, tatilVardiyaList));
		hafta.getVardiyaGunler().add(vardiyaListeDoldur(hafta.getVardiyaGun7(), vardiyaList, tatilVardiyaList));
	}

	/**
	 * @param plan
	 * @return
	 */
	@Transactional
	public String guncelle(VardiyaPlan plan) {
		vardiyaGuncelle = ikRole || ortakIslemler.getParameterKeyHasStringValue("vardiyaGuncelle");
		setVardiyaPlan(plan);
		fillVardiyaSablonList(plan.getPersonel());
		fillVardiyaList();

		if (plan.getVardiyaHafta1().isCheckBoxDurum())
			haftaListesiOlustur(plan, plan.getVardiyaHafta1());
		if (plan.getVardiyaHafta2().isCheckBoxDurum())
			haftaListesiOlustur(plan, plan.getVardiyaHafta2());

		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun1(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun2(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun3(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun4(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun5(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun6(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta1().getVardiyaGun7(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun1(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun2(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun3(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun4(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun5(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun6(), Boolean.FALSE);
		vardiyaSaatDakika(plan.getVardiyaHafta2().getVardiyaGun7(), Boolean.FALSE);
		setVardiyaPlan(plan);
		setDegisti(Boolean.FALSE);

		return "";

	}

	/**
	 * @return
	 */
	public String degerDegisti() {
		setDegisti(Boolean.TRUE);
		return "";
	}

	/**
	 * @param vardiyaGun
	 * @param degistir
	 */
	public void vardiyaSaatDakika(VardiyaGun vardiyaGun, boolean degistir) {
		if (vardiyaGun.getVardiya() != null && vardiyaGun.getVardiya().isCalisma()) {
			try {
				if (degistir || vardiyaGun.getBasSaat() == null)
					vardiyaGun.setBasSaat((int) vardiyaGun.getVardiya().getBasSaat());
				if (degistir || vardiyaGun.getBasDakika() == null)
					vardiyaGun.setBasDakika((int) vardiyaGun.getVardiya().getBasDakika());
				if (degistir || vardiyaGun.getBitSaat() == null)
					vardiyaGun.setBitSaat((int) vardiyaGun.getVardiya().getBitSaat());
				if (degistir || vardiyaGun.getBitDakika() == null)
					vardiyaGun.setBitDakika((int) vardiyaGun.getVardiya().getBitDakika());
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());

			}

		}
		setDegisti(Boolean.TRUE);
	}

	/**
	 * @param vardiyaGun
	 * @return
	 */
	public String vardiyaSec(VardiyaGun vardiyaGun) {
		setSeciliVardiyaGun(vardiyaGun);
		return "";
	}

	/**
	 * @param hafta
	 */
	@Transactional
	public void sablonVardiyalarUygula(VardiyaHafta hafta) {
		if (hafta.isCheckBoxDurum()) {
			setDegisti(Boolean.TRUE);
			VardiyaSablonu vardiyaSablonu = hafta.getVardiyaSablonu();
			boolean vardiyaDegistir = Boolean.TRUE;
			ArrayList<Vardiya> tatilVardiyaList = null;
			try {
				tatilVardiyaList = fillVardiyaDoldurList(vardiyaSablonu, vardiyaPlan.getPersonel());

			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
			}
			ArrayList<Vardiya> vardiyaList = new ArrayList<Vardiya>();
			for (Vardiya vardiya : tatilVardiyaList)
				vardiyaList.add(vardiya);

			hafta.setVardiyalar(vardiyaList);
			hafta.getVardiyaGunler().clear();
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun1(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya1() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun2(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya2() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun3(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya3() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun4(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya4() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun5(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya5() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun6(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya6() : null));
			hafta.getVardiyaGunler().add(vardiyaListeDegistir(hafta.getVardiyaGun7(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya7() : null));
		}
	}

	/**
	 * @param personelDenklestirme
	 * @param calismaModeliVardiyaOzelMap
	 * @param vardiyaMap
	 * @param plan
	 * @param mesaj
	 * @param excelAktar
	 * @return
	 */
	@Transactional
	public boolean vardiyaPlanKontrol(PersonelDenklestirme personelDenklestirme, HashMap<Long, HashMap<String, Boolean>> calismaModeliVardiyaOzelMap, TreeMap<Long, Vardiya> vardiyaMap, VardiyaPlan plan, String mesaj, boolean excelAktar) {
		boolean yaz = Boolean.TRUE;
		boolean haftaTatil = Boolean.FALSE;
		Calendar cal = Calendar.getInstance();
		Date ilkGun = aylikPuantajDefault.getIlkGun(), iseBaslamaTarihi = null, istenAyrilmaTarihi = null;
		Date sonGun = ortakIslemler.tariheAyEkleCikar(cal, ilkGun, 1);
		boolean admin = ikRole;
		StringBuffer sb = new StringBuffer();
		if (vardiyaMap != null && vardiyaMap.isEmpty())
			vardiyaMap = null;
		boolean ikMesaj = false;
		TreeMap<String, VardiyaGun> vardiyaGunMap = new TreeMap<String, VardiyaGun>();
		List<VardiyaGun> vardiyaGunHareketOnaysizList = new ArrayList<VardiyaGun>();
		Date basGun = null;
		Personel personel = plan.getPdksPersonel();
		if (personelDenklestirme != null && personel == null)
			personel = personelDenklestirme.getPdksPersonel();
		if (plan != null && plan.getVardiyaHaftaList() != null) {
			for (VardiyaHafta vardiyaHafta : plan.getVardiyaHaftaList()) {
				if (vardiyaHafta.getVardiyaGunler() != null) {
					for (VardiyaGun vg : vardiyaHafta.getVardiyaGunler()) {
						if (vg.getVardiya() != null) {
							if (personel == null)
								personel = vg.getPdksPersonel();
							if (basGun == null || basGun.before(vg.getVardiyaDate()))
								basGun = vg.getVardiyaDate();
							vardiyaGunMap.put(vg.getVardiyaDateStr(), vg);
						}

					}
				}

			}
		}
		StringBuffer sbCalismaModeliUyumsuz = new StringBuffer();
		CalismaModeli cm = personelDenklestirme != null && personelDenklestirme.getPersonel() != null ? personelDenklestirme.getPersonel().getCalismaModeli() : null;
		if (personelDenklestirme.getCalismaModeliAy() != null)
			cm = personelDenklestirme.getCalismaModeli();
		String donem = String.valueOf(yil * 100 + ay);
		Double kisaDonemSaat = null;
		try {
			String str = ortakIslemler.getParameterKey("kisaDonemSaat");
			if (PdksUtil.hasStringValue(str))
				kisaDonemSaat = Double.valueOf(Double.parseDouble(str));
			if (kisaDonemSaat.doubleValue() <= 0.0D)
				kisaDonemSaat = null;
		} catch (Exception e) {
			kisaDonemSaat = null;
		}
		Integer geceVardiyaAdetMaxSaat = null;
		try {
			String str = ortakIslemler.getParameterKey("geceVardiyaAdetMaxSaat");
			if (PdksUtil.hasStringValue(str))
				geceVardiyaAdetMaxSaat = Integer.valueOf(Integer.parseInt(str));
			if (geceVardiyaAdetMaxSaat.intValue() <= 0)
				geceVardiyaAdetMaxSaat = null;
		} catch (Exception e) {
			geceVardiyaAdetMaxSaat = null;
		}
		Integer haftaTatilCalismaGunAdetMaxSaat = null;
		try {
			String str = ortakIslemler.getParameterKey("haftaTatilCalismaGunAdetMaxSaat");
			if (PdksUtil.hasStringValue(str))
				haftaTatilCalismaGunAdetMaxSaat = Integer.valueOf(Integer.parseInt(str));
			if (haftaTatilCalismaGunAdetMaxSaat.intValue() <= 0)
				haftaTatilCalismaGunAdetMaxSaat = null;
		} catch (Exception e) {
			haftaTatilCalismaGunAdetMaxSaat = null;
		}
		VardiyaGun oncekiVardiyaGunKontrol = null;
		int geceVardiyaAdetSaat = 0;
		Integer haftaTatilCalismaGunAdetSaat = null;
		String haftaTatilCalismaGunAdetMaxSaatStr = null;
		String geceVardiyaAdetMaxSaatStr = null;
		String calismaPlanDenetimTarihStr = this.ortakIslemler.getParameterKey("calismaPlanDenetimTarih");
		Date calismaPlanDenetimTarih = null;
		try {
			if (PdksUtil.hasStringValue(calismaPlanDenetimTarihStr))
				calismaPlanDenetimTarih = PdksUtil.getDateFromString(calismaPlanDenetimTarihStr);
		} catch (Exception localException1) {
		}
		if (personel != null && haftaTatilCalismaGunAdetMaxSaat != null && calismaPlanDenetimTarih != null && calismaPlanDenetimTarih.before(ilkGun)) {
			Date basTarih = ortakIslemler.tariheGunEkleCikar(cal, ilkGun, -7), bitTarih = ortakIslemler.tariheGunEkleCikar(cal, basGun, -1);
			if (bitTarih.getTime() >= basTarih.getTime()) {
				List<Long> personeller = new ArrayList<Long>();
				personeller.add(personel.getId());
				List<VardiyaGun> list = ortakIslemler.getAllPersonelIdVardiyalar(personeller, basTarih, bitTarih, false, session);
				for (VardiyaGun vg : list) {
					vardiyaGunMap.put(vg.getVardiyaDateStr(), vg);
				}
			}
		}
		VardiyaGun oncekiVardiyaGun = null;
		StringBuffer izinSonrasiOffDurum = new StringBuffer(), haftaTatilSb = new StringBuffer(), cakisanVardiyaSb = new StringBuffer();
		Vardiya vardiyaGunOnceki = null;
		for (Iterator<String> iterator = vardiyaGunMap.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			VardiyaGun vardiyaGun = vardiyaGunMap.get(key);
			boolean hataliDurum = false;

			String str = vardiyaGun.getVardiyaDateStr();
			vardiyaGun.setAyinGunu(str.startsWith(donem));
			if (vardiyaGun.getVardiya() != null) {
				StringBuffer manuelGirisHTML = new StringBuffer();
				Vardiya vardiya = vardiyaGun.getVardiya(), islemVardiya = vardiyaGun.getIslemVardiya();
				if (vardiyaGun.getIzin() == null && vardiyaGunOnceki != null && islemVardiya != null && islemVardiya.isCalisma() && vardiyaGunOnceki.getVardiyaBitZaman().after(islemVardiya.getVardiyaBasZaman())) {
					if (cakisanVardiyaSb.length() > 0)
						cakisanVardiyaSb.append(", ");
					String mesajStr = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), PdksUtil.getDateFormat() + " EEEEE") + " başlangıç saati önceki gün bitiş saati";
					cakisanVardiyaSb.append(mesajStr);
					manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr + " çakışmaktadır.");
					yaz = Boolean.FALSE;
					hataliDurum = true;
				}
				if (vardiya.isCalisma() == false && vardiyaGun.isAyinGunu() && oncekiVardiyaGun != null && oncekiVardiyaGun.getIzin() != null) {
					IzinTipi izinTipi = oncekiVardiyaGun.getIzin().getIzinTipi();
					if (izinTipi.isBaslamaZamaniCalismadir() && (vardiyaGun.getTatil() == null || vardiyaGun.getTatil().isYarimGunMu())) {
						cal.setTime(vardiyaGun.getVardiyaDate());
						if (vardiya.isHaftaTatil() == false && (izinTipi.isCumaCumartesiTekIzinSaysin() == false || cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)) {
							yaz = false;
							if (izinSonrasiOffDurum.length() > 0)
								izinSonrasiOffDurum.append(", ");
							String mesajStr = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), PdksUtil.getDateFormat() + " EEEEE") + " günü " + vardiya.getAdi();
							izinSonrasiOffDurum.append(mesajStr);
							manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
						}

					}
				}
				boolean calismaPlanDenetimTarihKontrol = (calismaPlanDenetimTarih != null) && (vardiyaGun.getVardiyaDate().after(calismaPlanDenetimTarih));
				if (haftaTatilCalismaGunAdetMaxSaat != null) {
					if (vardiya.isHaftaTatil()) {
						if ((haftaTatilCalismaGunAdetSaat != null) && (haftaTatilCalismaGunAdetSaat.intValue() > haftaTatilCalismaGunAdetMaxSaat.intValue())) {
							yaz = ikRole;
							if (!ikMesaj)
								ikMesaj = ikRole;
							String mesajStr = haftaTatilCalismaGunAdetMaxSaat + " günden fazla ardışık çalışma olamaz! ";
							haftaTatilCalismaGunAdetMaxSaatStr = mesajStr;
							manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
							haftaTatilCalismaGunAdetMaxSaat = null;
						}
						if (calismaPlanDenetimTarihKontrol)
							haftaTatilCalismaGunAdetSaat = Integer.valueOf(0);
					} else if ((vardiya.isCalisma()) && (haftaTatilCalismaGunAdetSaat != null)) {
						haftaTatilCalismaGunAdetSaat = Integer.valueOf(haftaTatilCalismaGunAdetSaat.intValue() + 1);
					}
				}
				if (geceVardiyaAdetMaxSaat != null) {
					if ((!vardiya.isAksamVardiyasi()) || (vardiyaGun.getIzin() != null)) {
						if (vardiya.isCalisma()) {
							if ((geceVardiyaAdetSaat > 0) && (geceVardiyaAdetSaat > geceVardiyaAdetMaxSaat.intValue())) {
								yaz = ikRole;
								if (!ikMesaj)
									ikMesaj = ikRole;
								String mesajStr = geceVardiyaAdetMaxSaat + " günden fazla ardışık akşam çalışma olamaz! ";
								geceVardiyaAdetMaxSaatStr = mesajStr;
								manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
								geceVardiyaAdetMaxSaat = null;
							}
							geceVardiyaAdetSaat = 0;
						}
					} else if (calismaPlanDenetimTarihKontrol) {
						if (vardiya.isAksamVardiyasi())
							geceVardiyaAdetSaat++;
						logger.debug(vardiyaGun.getVardiyaDateStr() + " " + vardiya.getKisaAdi() + " " + geceVardiyaAdetSaat);
					}

				}
				if ((kisaDonemSaat != null) && (vardiya.isCalisma()) && (vardiyaGun.getIzin() == null)) {
					if ((key.startsWith(donem)) && (oncekiVardiyaGunKontrol != null)) {
						Date basSaat = oncekiVardiyaGunKontrol.getIslemVardiya().getVardiyaBitZaman();
						Date bitSaat = vardiyaGun.getIslemVardiya().getVardiyaBasZaman();
						double toplamSure = PdksUtil.setSureDoubleTypeRounded(Double.valueOf(PdksUtil.getSaatFarki(bitSaat, basSaat).doubleValue()), vardiyaGun.getYarimYuvarla());
						if (toplamSure < kisaDonemSaat.doubleValue() && calismaPlanDenetimTarihKontrol) {
							yaz = ikRole;
							if (!ikMesaj)
								ikMesaj = ikRole;
							String mesajStr = PdksUtil.convertToDateString(bitSaat, "d MMMMMM EEEEE HH:ss") + " kısa çalışma olamaz! ";
							sb.append(mesajStr);
							manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
						}
					}
					if (calismaPlanDenetimTarihKontrol)
						oncekiVardiyaGunKontrol = vardiyaGun;

				}
				if (vardiya.isCalisma() && vardiyaMap != null && vardiyaGun.isAyinGunu()) {
					if (vardiya.getGenel()) {
						if (cm.isOrtakVardiyadir() == false && vardiyaMap != null && !vardiyaMap.containsKey(vardiya.getId())) {
							if (sbCalismaModeliUyumsuz.length() > 0)
								sbCalismaModeliUyumsuz.append(", ");
							String mesajStr = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "d MMMMMM ") + " " + vardiya.getAciklama() + (admin ? " [ " + vardiya.getKisaAciklama() + " ] " : "");
							sbCalismaModeliUyumsuz.append(mesajStr);
							manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);

						}
					} else if (calismaModeliVardiyaOzelMap != null && calismaModeliVardiyaOzelMap.containsKey(cm.getId())) {
						HashMap<String, Boolean> map = calismaModeliVardiyaOzelMap.get(cm.getId());
						boolean ozel = false;
						if (vardiya.isGebelikMi() || vardiyaGun.isGebeMi())
							ozel = map.containsKey(Vardiya.GEBE_KEY) && map.get(Vardiya.GEBE_KEY);
						if (vardiya.isSuaMi())
							ozel = map.containsKey(Vardiya.SUA_KEY) && map.get(Vardiya.SUA_KEY);
						if (vardiya.isIcapVardiyasi())
							ozel = map.containsKey(Vardiya.ICAP_KEY) && map.get(Vardiya.ICAP_KEY);
						if (ozel && !vardiyaMap.containsKey(vardiya.getId())) {
							if (sbCalismaModeliUyumsuz.length() > 0)
								sbCalismaModeliUyumsuz.append(", ");
							String mesajStr = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "d MMMMMM ") + " " + vardiya.getAciklama() + (admin ? " [ " + vardiya.getKisaAciklama() + " ] " : "");
							sbCalismaModeliUyumsuz.append(mesajStr);
							manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
							yaz = Boolean.FALSE;
						}
					}
				}
				if (iseBaslamaTarihi == null)
					iseBaslamaTarihi = vardiyaGun.getPersonel().getIseGirisTarihi();
				if (istenAyrilmaTarihi == null)
					istenAyrilmaTarihi = vardiyaGun.getPersonel().getSonCalismaTarihi();
				if (vardiyaGun.getVardiya().isHaftaTatil()) {
					if (haftaTatil) {
						if (authenticatedUser.isIK() == false)
							yaz = Boolean.FALSE;
						String mesajStr = "Arka arkaya hafta tatili olamaz! [ " + authenticatedUser.dateFormatla(vardiyaGun.getVardiyaDate()) + " ] ";
						sb.append(mesajStr);
						manuelGirisHTML.append((manuelGirisHTML.length() > 0 ? "<br></br>" : "") + mesajStr);
					}
					haftaTatil = Boolean.TRUE;
				} else
					haftaTatil = Boolean.FALSE;
				vardiyaGunOnceki = vardiya.isCalisma() ? islemVardiya : null;
				if (manuelGirisHTML.length() > 0)
					vardiyaGun.setManuelGirisHTML(manuelGirisHTML.toString());
				manuelGirisHTML = null;
			} else {
				vardiyaGunOnceki = null;
				geceVardiyaAdetSaat = 0;
				haftaTatil = Boolean.FALSE;
			}
			vardiyaGun.setHataliDurum(hataliDurum);
			oncekiVardiyaGun = vardiyaGun;
		}
		if (cakisanVardiyaSb.length() > 0) {
			sb.append(cakisanVardiyaSb.toString() + " çakışmaktadır!");
			yaz = false;
		}

		if (geceVardiyaAdetMaxSaatStr != null)
			sb.append(geceVardiyaAdetMaxSaatStr);
		if (haftaTatilCalismaGunAdetMaxSaatStr != null)
			sb.append(haftaTatilCalismaGunAdetMaxSaatStr);
		if (izinSonrasiOffDurum.length() > 0)
			sb.append(izinSonrasiOffDurum.toString() + " olamaz!");
		izinSonrasiOffDurum = null;
		if (yaz) {
			List<VardiyaHafta> vardiyaHaftaList = plan.getVardiyaHaftaList();
			if (vardiyaHaftaList == null) {
				vardiyaHaftaList = new ArrayList<VardiyaHafta>();
				for (int i = 1; i < 3; i++) {
					VardiyaHafta vardiyaHafta = i == 1 ? plan.getVardiyaHafta1() : plan.getVardiyaHafta2();
					vardiyaHaftaList.add(vardiyaHafta);
				}
			}
			List<VardiyaGun> haftaTatilAyList = new ArrayList<VardiyaGun>(), haftaTatilSonrakiAyList = new ArrayList<VardiyaGun>(), haftaTatilOncekiAyList = new ArrayList<VardiyaGun>();

			for (int i = 1; i <= vardiyaHaftaList.size(); i++) {
				VardiyaHafta vardiyaHafta = vardiyaHaftaList.get(i - 1);
				if (iseBaslamaTarihi == null && vardiyaHafta.getPersonel() != null)
					iseBaslamaTarihi = vardiyaHafta.getPersonel().getIseGirisTarihi();
				if (istenAyrilmaTarihi == null && vardiyaHafta.getPersonel() != null)
					istenAyrilmaTarihi = vardiyaHafta.getPersonel().getSonCalismaTarihi();

				String haftaStr = PdksUtil.convertToDateString(vardiyaHafta.getBasTarih(), PdksUtil.getDateFormat()) + " - " + PdksUtil.convertToDateString(vardiyaHafta.getBitTarih(), PdksUtil.getDateFormat()) + " tarihleri arası";
				boolean parcaliBitHafta = false, parcaliBasHafta = false;
				haftaTatilSonrakiAyList.clear();
				haftaTatilAyList.clear();
				haftaTatilOncekiAyList.clear();
				if (excelAktar) {
					parcaliBasHafta = denklestirmeOncekiAy != null && ilkGun.getTime() > vardiyaHafta.getBasTarih().getTime();
					parcaliBitHafta = denklestirmeSonrakiAy != null && sonGun.getTime() <= vardiyaHafta.getBitTarih().getTime();
				}
				ArrayList<Vardiya> list = new ArrayList<Vardiya>();
				List<VardiyaGun> vardiyaGunler = vardiyaHafta.getVardiyaGunler();
				if (vardiyaGunler != null && vardiyaGunler.size() == 7) {
					for (VardiyaGun pdksVardiyaGun : vardiyaGunler) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, pdksVardiyaGun, excelAktar));
						if (getVardiyaDurum(pdksVardiyaGun, i))
							yaz = Boolean.FALSE;
					}
				} else {
					if (vardiyaHafta.getVardiyaGun1().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun1(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun1(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun2().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun2(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun2(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun3().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun3(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun3(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun4().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun4(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun4(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun5().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun5(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun5(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun6().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun6(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun6(), i))
							yaz = Boolean.FALSE;
					}
					if (vardiyaHafta.getVardiyaGun7().getVardiya() != null) {
						vardiyaGunHareketOnaysizList.add(addHaftaVardiya(list, vardiyaHafta.getVardiyaGun7(), excelAktar));
						if (getVardiyaDurum(vardiyaHafta.getVardiyaGun7(), i))
							yaz = Boolean.FALSE;
					}
				}
				int tatilAdet = 0, offAdet = 0, gunSayisi = 0;
				if (iseBaslamaTarihi != null && vardiyaHafta.getBasTarih().getTime() < iseBaslamaTarihi.getTime() && vardiyaHafta.getBitTarih().getTime() >= iseBaslamaTarihi.getTime()) {
					Double dakikaFarki = PdksUtil.getDakikaFarki(iseBaslamaTarihi, vardiyaHafta.getBasTarih());
					gunSayisi = new Double(dakikaFarki.doubleValue() / 1440d).intValue();
				}
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Vardiya vardiya = (Vardiya) iterator.next();
					Date vardiyaDate = vardiya.getVardiyaTarih();
					if (vardiyaDate == null)
						vardiyaDate = ortakIslemler.tariheGunEkleCikar(cal, vardiyaHafta.getBasTarih(), gunSayisi);
					++gunSayisi;

					if (vardiya.isCalisma() && vardiya.getGenel() && vardiyaMap != null && !vardiyaMap.containsKey(vardiya.getId())) {
						String key = PdksUtil.convertToDateString(vardiya.getVardiyaTarih(), "yyyyMMdd");
						VardiyaGun vardiyaGun = vardiyaGunMap.get(key);
						if (vardiyaGun == null || vardiyaGun.isAyinGunu()) {
							if (sbCalismaModeliUyumsuz.length() > 0)
								sbCalismaModeliUyumsuz.append(", ");
							sbCalismaModeliUyumsuz.append(PdksUtil.convertToDateString(vardiya.getVardiyaTarih(), "d MMMMMM ") + " " + vardiya.getAciklama() + (admin ? " [ " + vardiya.getKisaAciklama() + " ] " : ""));
							yaz = Boolean.FALSE;
						}
					}
					if (vardiyaDate.before(iseBaslamaTarihi) || vardiyaDate.after(istenAyrilmaTarihi)) {
						iterator.remove();
						continue;
					}

					if (vardiya.isHaftaTatil()) {
						if (parcaliBasHafta) {
							String key = PdksUtil.convertToDateString(vardiyaDate, "yyyyMMdd");
							if (plan.getVardiyaGunMap().containsKey(key)) {
								VardiyaGun vardiyaGun = plan.getVardiyaGunMap().get(key);
								if (vardiyaGun.getVardiya() != null && vardiyaGun.getPersonel() != null) {

									if (vardiyaGun.getVardiya().isHaftaTatil() && vardiyaGun.getPersonel().isCalisiyorGun(vardiyaDate)) {
										if (vardiyaDate.before(ilkGun))
											haftaTatilOncekiAyList.add(vardiyaGun);
										else
											haftaTatilAyList.add(vardiyaGun);
									}

								}
							}

						} else if (parcaliBitHafta) {
							String key = PdksUtil.convertToDateString(vardiyaDate, "yyyyMMdd");
							if (plan.getVardiyaGunMap().containsKey(key)) {
								VardiyaGun vardiyaGun = plan.getVardiyaGunMap().get(key);

								if (vardiyaGun.getPersonel() != null && vardiyaGun.getVardiya().isHaftaTatil() && vardiyaGun.getPersonel().isCalisiyorGun(vardiyaDate)) {
									if (vardiyaDate.before(sonGun))
										haftaTatilAyList.add(vardiyaGun);
									else
										haftaTatilSonrakiAyList.add(vardiyaGun);
								}

							}

						}
						++tatilAdet;
					}

					if (PdksUtil.getPlanOffAdet() > 0 && vardiya.isOffGun())
						++offAdet;

				}

				if (tatilAdet == 0 && list.size() == 7) {
					sb.append(haftaStr + " tatil tanımlanmamıştır! ");
					haftaTatilSb.append(haftaStr + " tatil tanımlanmamıştır! ");
					yaz = Boolean.FALSE;
				}
				boolean partTime = Boolean.FALSE;
				if (plan.getPersonel() != null && plan.getPersonel().getPartTime() != null)
					partTime = plan.getPersonel().getPartTime();
				if (!partTime && offAdet > 0 && offAdet > PdksUtil.getPlanOffAdet()) {
					sb.append(haftaStr + " en fazla " + (PdksUtil.getPlanOffAdet()) + " off gün tanımlanmalıdır! ");
					yaz = Boolean.FALSE;
				} else if (tatilAdet > 1) {
					if (haftaTatilAyList.size() == 1 && haftaTatilOncekiAyList.size() == 1) {
						VardiyaGun vardiyaGunBuAy = haftaTatilAyList.get(0), vardiyaGunOncekiAy = haftaTatilOncekiAyList.get(0);
						vardiyaGunOncekiAy.setVardiya(vardiyaGunBuAy.getEskiVardiya());
						vardiyaGunOncekiAy.setGuncelleyenUser(authenticatedUser);
						vardiyaGunOncekiAy.setGuncellemeTarihi(new Date());
						saveOrUpdate(vardiyaGunOncekiAy);
					} else if (haftaTatilAyList.size() == 1 && haftaTatilSonrakiAyList.size() == 1) {
						VardiyaGun vardiyaGunBuAy = haftaTatilAyList.get(0), vardiyaGunSonrakiAy = haftaTatilSonrakiAyList.get(0);
						vardiyaGunSonrakiAy.setVardiya(vardiyaGunBuAy.getEskiVardiya());
						vardiyaGunSonrakiAy.setGuncelleyenUser(authenticatedUser);
						vardiyaGunSonrakiAy.setGuncellemeTarihi(new Date());
						vardiyaGunSonrakiAy.setDurum(Boolean.FALSE);
						saveOrUpdate(vardiyaGunSonrakiAy);
					} else {
						haftaTatilSb.append(haftaStr + " en fazla bir tatil günü tanımlanmalıdır! ");
						sb.append(haftaStr + " en fazla bir tatil günü tanımlanmalıdır! ");
						yaz = Boolean.FALSE;
					}
				}
			}

		}
		boolean flush = false;
		if (haftaTatilSb.length() == 0) {
			if (!yaz || (ikMesaj && sb.length() > 0)) {
				if (sb.length() > 0)
					PdksUtil.addMessageAvailableWarn(mesaj + sb.toString());
				if (sbCalismaModeliUyumsuz.length() > 0) {
					String str = sbCalismaModeliUyumsuz.toString();
					PdksUtil.addMessageAvailableWarn(mesaj + str + " " + (cm != null ? cm.getAciklama() + " vardiyalarına " : ortakIslemler.calismaModeliAciklama() + "ne") + " uymayan hatalı " + (str.indexOf(",") < 0 ? "vardiyadır!" : "vardiyalardır"));
				}
				if (personelDenklestirme != null) {
					if (ikMesaj)
						personelDenklestirme.setGuncelleyenUser(authenticatedUser);
					personelDenklestirme.setOnaylandi(yaz);
					personelDenklestirme.setDurum(Boolean.FALSE);
					personelDenklestirme.setGuncellemeTarihi(new Date());
					saveOrUpdate(personelDenklestirme);

				}
			} else {
				for (Iterator iterator = vardiyaGunHareketOnaysizList.iterator(); iterator.hasNext();) {
					VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
					if (vardiyaGun.getId() != null && vardiyaGun.isAyinGunu() && vardiyaGun.getVersion() < 0) {
						vardiyaGun.setVersion(0);
						saveOrUpdate(vardiyaGun);
						flush = true;
					}

				}
			}
			if (flush)
				sessionFlush();
		} else {
			yaz = false;
			PdksUtil.addMessageAvailableWarn(mesaj + sb.toString());
		}

		vardiyaGunMap = null;
		sb = null;
		sbCalismaModeliUyumsuz = null;

		return yaz;

	}

	/**
	 * @param list
	 * @param vardiyaGun
	 * @param excelAktar
	 */
	private VardiyaGun addHaftaVardiya(ArrayList<Vardiya> list, VardiyaGun vardiyaGun, boolean excelAktar) {
		try {
			if (vardiyaGun != null) {
				if (vardiyaGun.getVardiya() != null) {
					Vardiya vardiya = (Vardiya) vardiyaGun.getVardiya().clone();
					vardiya.setVardiyaTarih(vardiyaGun.getVardiyaDate());
					if (excelAktar == false && vardiya != null && vardiya.isOffGun() && vardiyaGun.getTatil() != null) {
						// vardiya = null;
						logger.debug(vardiyaGun.getVardiyaDateStr());
					}

					if (vardiya != null)
						list.add(vardiya);
				} else
					logger.debug(vardiyaGun.getVardiyaDateStr());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return vardiyaGun;
	}

	/**
	 * @param gorevYeriAciklama
	 * @param puantajList
	 * @param sonucGoster
	 * @return
	 */
	public ByteArrayOutputStream aylikVardiyaExcelDevam(String gorevYeriAciklama, List<AylikPuantaj> puantajList, Boolean sonucGoster) {
		List<Boolean> onayDurumList = new ArrayList<Boolean>();
		for (AylikPuantaj aylikPuantaj : puantajList) {
			if (!onayDurumList.contains(aylikPuantaj.isOnayDurum()))
				onayDurumList.add(aylikPuantaj.isOnayDurum());
		}
		ByteArrayOutputStream baos = null;
		String aciklamaExcel = PdksUtil.replaceAll(gorevYeriAciklama + " " + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM  "), "_", " ");
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "MMMMM yyyy") + " Çalışma Planı", Boolean.TRUE);

		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleTutarEvenDay = ExcelUtil.setAlignment(ExcelUtil.getStyleDayEven(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleTutarOddDay = ExcelUtil.setAlignment(ExcelUtil.getStyleDayOdd(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleTutarDay = null, styleGenelCenter = null, styleGenel = null, styleGenelLeft = null;
		XSSFCellStyle styleTatil = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);

		XSSFCellStyle styleIstek = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		XSSFCellStyle styleEgitim = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		XSSFCellStyle styleOff = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		ExcelUtil.setFontColor(styleOff, Color.WHITE);
		XSSFCellStyle style = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		XSSFCellStyle styleIzin = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		XSSFCellStyle header = (XSSFCellStyle) ExcelUtil.getStyleHeader(9, wb);

		XSSFCellStyle styleCalisma = (XSSFCellStyle) ExcelUtil.getStyleDataCenter(wb);
		int row = 0, col = 0;

		ExcelUtil.setFillForegroundColor(styleTatil, 255, 153, 204);
		ExcelUtil.setFillForegroundColor(styleIstek, 255, 255, 0);
		ExcelUtil.setFillForegroundColor(styleIzin, 146, 208, 80);
		ExcelUtil.setFillForegroundColor(styleCalisma, 255, 255, 255);

		ExcelUtil.setFillForegroundColor(styleEgitim, 0, 0, 255);

		ExcelUtil.setFillForegroundColor(styleOff, 13, 12, 89);
		styleOff.getFont().setColor(ExcelUtil.getXSSFColor(256, 256, 256));
		ExcelUtil.getCell(sheet, row, col, header).setCellValue(aciklamaExcel);
		for (int i = 0; i < 3; i++)
			ExcelUtil.getCell(sheet, row, col + i + 1, style).setCellValue("");

		try {
			sheet.addMergedRegion(ExcelUtil.getRegion((int) row, (int) 0, (int) row, (int) 4));
		} catch (Exception e) {
			e.printStackTrace();
		}
		col = 0;
		ExcelUtil.getCell(sheet, ++row, col, style).setCellValue("");
		ExcelUtil.getCell(sheet, ++row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adı Soyadı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.yoneticiAciklama());

		Calendar cal = Calendar.getInstance();
		cal.setTime(aylikPuantajDefault.getIlkGun());
		CreationHelper helper = wb.getCreationHelper();
		Drawing drawing = sheet.createDrawingPatriarch();
		ClientAnchor anchor = helper.createClientAnchor();
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
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, cal.get(Calendar.DAY_OF_MONTH) + "\n " + authenticatedUser.getTarihFormatla(cal.getTime(), "EEE"), title);

			} catch (Exception e) {
			}
		}
		if (sonucGoster) {
			Cell cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "TÇS", "Toplam Çalışma Saati: Çalışanın bu listedeki toplam çalışma saati");
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "ÇGS", "Çalışılması Gereken Saat: Çalışanın bu listede çalışması gereken saat");
			cell = ExcelUtil.getCell(sheet, row, col++, header);
			ExcelUtil.baslikCell(cell, anchor, helper, drawing, "GM", "Gerçekleşen Mesai : Çalışanın bu listedeki eksi/fazla çalışma saati");
			if (devredenMesaiKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.devredenMesaiKod(), "Devreden Mesai: Çalisanin önceki listelerden devreden eksi/fazla mesaisi");

			}
			if (ucretiOdenenKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, "ÜÖM", "Çalışanın bu listenin sonunda ücret olarak ödediğimiz fazla mesai saati");
			}
			if (eksikMaasGoster) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, "NORMC", ortakIslemler.eksikCalismaAciklama() + " : Çalışanın bu listenin sonunda ücretinden kesilecek saati");
			}
			if (resmiTatilVar || bordroPuantajEkranindaGoster) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, "RÖM", "Çalışanın bu listenin sonunda ücret olarak ödediğimiz resmi tatil mesai saati");
			}
			if (haftaTatilVar) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_HAFTA_TATIL, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz hafta tatil mesai saati");
			}
			if (aksamGunVar) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_AKSAM_ADET, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz gece mesai gün");
			}
			if (aksamSaatVar) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, AylikPuantaj.MESAI_TIPI_AKSAM_SAAT, "Çalışanın bu listenin sonunda ücret olarak ödediğimiz gece mesai saati");
			}
			if (devredenBakiyeKod) {
				cell = ExcelUtil.getCell(sheet, row, col++, header);
				ExcelUtil.baslikCell(cell, anchor, helper, drawing, ortakIslemler.devredenBakiyeKod(), "Bakiye: Çalışanın bu liste de dahil bugüne kadarki devreden eksi/fazla mesaisi");
			}
			if (modelGoster)
				ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.calismaModeliAciklama());
			CellStyle headerIzinTipi = (XSSFCellStyle) header.clone();
			ExcelUtil.setFillForegroundColor(headerIzinTipi, 255, 153, 204);
			if (dinamikAlanlar != null && !dinamikAlanlar.isEmpty()) {
				for (Tanim alan : dinamikAlanlar) {
					ExcelUtil.getCell(sheet, row, col++, header).setCellValue(alan.getAciklama());

				}
			}
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

		}
		int sayac = 0;
		TreeMap<Long, CalismaModeli> modelMap = new TreeMap<Long, CalismaModeli>();
		boolean yoneticiRol = adminRole == false && ikRole == false;
		for (Iterator iter = puantajList.iterator(); iter.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();

			Personel personel = aylikPuantaj.getPdksPersonel();
			if (!aylikPuantaj.isSecili() || personel == null || PdksUtil.hasStringValue(personel.getSicilNo()) == false)

				continue;
			row++;
			CalismaModeli calismaModeli = aylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy() != null ? aylikPuantaj.getPersonelDenklestirme().getCalismaModeli() : null;
			if (calismaModeli == null)
				calismaModeli = personel.getCalismaModeli();
			boolean help = helpPersonel(aylikPuantaj.getPdksPersonel());
			++sayac;
			if (yoneticiRol == false && calismaModeli != null && calismaModeli.getDurum()) {
				if (!modelMap.containsKey(calismaModeli.getId()))
					modelMap.put(calismaModeli.getId(), calismaModeli);
			}
			try {
				if (row % 2 != 0) {
					styleTutarDay = styleTutarOddDay;
					styleGenel = styleOdd;
					styleGenelLeft = styleOdd;
					styleGenelCenter = styleOddCenter;
				} else {
					styleTutarDay = styleTutarEvenDay;
					styleGenel = styleEven;
					styleGenelLeft = styleEven;
					styleGenelCenter = styleEvenCenter;
				}

				boolean koyuRenkli = onayDurumList.size() == 2 && aylikPuantaj.isOnayDurum();
				if (koyuRenkli) {
					ExcelUtil.setFontNormalBold(wb, styleGenel);
					ExcelUtil.setFontNormalBold(wb, styleGenelCenter);
				}
				col = 0;
				ExcelUtil.getCell(sheet, row, col++, styleGenelCenter).setCellValue(personel.getSicilNo());
				Cell personelCell = ExcelUtil.getCell(sheet, row, col++, styleGenelLeft);
				personelCell.setCellValue(personel.getAdSoyad());
				String titlePersonel = null;
				if (koyuRenkli) {
					PersonelDenklestirme denklestirme = aylikPuantaj.getPersonelDenklestirme();
					titlePersonel = authenticatedUser.getAdSoyad() + " planı " + authenticatedUser.dateTimeFormatla(denklestirme.getGuncellemeTarihi()) + " onaylandı.";
				}
				if (titlePersonel != null) {
					Comment comment1 = drawing.createCellComment(anchor);
					RichTextString str1 = helper.createRichTextString(titlePersonel);
					comment1.setString(str1);
					personelCell.setCellComment(comment1);
				}
				ExcelUtil.getCell(sheet, row, col++, styleGenelLeft).setCellValue(aylikPuantaj.getYonetici() != null && aylikPuantaj.getYonetici().getId() != null ? aylikPuantaj.getYonetici().getAdSoyad() : "");

				List<VardiyaGun> vardiyaList = aylikPuantaj.getAyinVardiyalari();
				for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
					VardiyaGun pdksVardiyaGun = (VardiyaGun) iterator.next();
					String styleText = pdksVardiyaGun.getAylikClassAdi(aylikPuantaj.getTrClass());
					styleGenel = styleTutarDay;
					if (styleText.equals(VardiyaGun.STYLE_CLASS_HAFTA_TATIL))
						styleGenel = styleTatil;
					else if (styleText.equals(VardiyaGun.STYLE_CLASS_IZIN))
						styleGenel = styleIzin;
					else if (styleText.equals(VardiyaGun.STYLE_CLASS_OZEL_ISTEK))
						styleGenel = styleIstek;
					else if (styleText.equals(VardiyaGun.STYLE_CLASS_EGITIM))
						styleGenel = styleEgitim;
					else if (styleText.equals(VardiyaGun.STYLE_CLASS_OFF))
						styleGenel = styleOff;
					Cell cell = ExcelUtil.getCell(sheet, row, col++, styleGenel);
					String aciklama = !help || calisan(pdksVardiyaGun) ? pdksVardiyaGun.getOzelAciklama(Boolean.TRUE) : "";
					if (aciklama.equals(".") && pdksVardiyaGun.getVardiya() != null && pdksVardiyaGun.getVardiya().isCalisma() == false) {
						aciklama = pdksVardiyaGun.getVardiya().getKisaAdi();
					}

					String title = !help || calisan(pdksVardiyaGun) ? pdksVardiyaGun.getTitle() : null;
					if (title != null) {
						Comment comment1 = drawing.createCellComment(anchor);
						RichTextString str1 = helper.createRichTextString(title);
						comment1.setString(str1);
						cell.setCellComment(comment1);

					}
					cell.setCellValue(aciklama);

				}
				if (sonucGoster && !help) {
					if (row % 2 != 0)
						styleGenel = styleOdd;
					else {
						styleGenel = styleEven;
					}
					setCell(sheet, row, col++, styleGenel, aylikPuantaj.getSaatToplami());
					Cell planlananCell = setCell(sheet, row, col++, styleGenel, aylikPuantaj.getPlanlananSure());

					if (aylikPuantaj.getCalismaModeliAy() != null && planlananCell != null && aylikPuantaj.getSutIzniDurum().equals(Boolean.FALSE)) {
						Comment comment1 = drawing.createCellComment(anchor);
						String title = aylikPuantaj.getCalismaModeli().getAciklama() + " : ";
						if (aylikPuantaj.getCalismaModeli().getToplamGunGuncelle().equals(Boolean.FALSE))
							title += authenticatedUser.sayiFormatliGoster(aylikPuantaj.getCalismaModeliAy().getSure());
						else
							title += authenticatedUser.sayiFormatliGoster(aylikPuantaj.getPersonelDenklestirme().getPlanlanSure());
						RichTextString str1 = helper.createRichTextString(title);
						comment1.setString(str1);
						planlananCell.setCellComment(comment1);
					}
					setCell(sheet, row, col++, styleGenel, aylikPuantaj.getAylikNetFazlaMesai());
					if (devredenMesaiKod)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getGecenAyFazlaMesai());
					if (ucretiOdenenKod)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getFazlaMesaiSure());
					if (eksikMaasGoster)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getEksikCalismaSure());

					if (resmiTatilVar || bordroPuantajEkranindaGoster)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getResmiTatilToplami());
					if (haftaTatilVar)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getHaftaCalismaSuresi());
					if (aksamGunVar)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getAksamVardiyaSayisi());
					if (aksamSaatVar)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getAksamVardiyaSaatSayisi());
					if (devredenBakiyeKod)
						setCell(sheet, row, col++, styleGenel, aylikPuantaj.getDevredenSure());
					if (modelGoster) {
						String modelAciklama = "";
						if (calismaModeli != null)
							modelAciklama = calismaModeli.getAciklama();
						ExcelUtil.getCell(sheet, row, col++, styleGenelLeft).setCellValue(modelAciklama);
					}
					if (dinamikAlanlar != null && !dinamikAlanlar.isEmpty()) {
						for (Tanim alan : dinamikAlanlar) {
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
						for (Vardiya vardiya : izinTipiVardiyaList) {
							Integer adet = getVardiyaAdet(personel, vardiya);
							if (adet != null)
								setCell(sheet, row, col++, styleGenel, adet);
							else
								ExcelUtil.getCell(sheet, row, col++, styleGenel).setCellValue("");

						}
					}
				}
				styleGenel = null;
				styleGenelCenter = null;
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
				logger.error(row);

			}
		}
		if (sonucGoster && sayac > 1) {
			row += 2;
			col = 0;
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
			ExcelUtil.getCell(sheet, row, col++, styleGenelLeft).setCellValue("");
			ExcelUtil.getCell(sheet, row, col++, styleGenelLeft).setCellValue("");
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
					ExcelUtil.baslikCell(cell, anchor, helper, drawing, cal.get(Calendar.DAY_OF_MONTH) + "\n " + authenticatedUser.getTarihFormatla(cal.getTime(), "EEE"), title);

				} catch (Exception e) {
				}
			}
			col = 0;
			boolean renk = Boolean.TRUE;
			for (VardiyaGun vardiyaGun : aylikVardiyaOzetList) {
				Vardiya vardiya = vardiyaGun.getVardiya();
				row++;
				if (renk) {
					styleGenelLeft = styleOdd;
					styleGenelCenter = styleOddCenter;
					styleTutarDay = styleTutarOddDay;

				} else {
					styleGenelLeft = styleEven;
					styleGenelCenter = styleEvenCenter;
					styleTutarDay = styleTutarEvenDay;

				}
				renk = !renk;
				String bolumAdi = "";
				Personel personel = vardiyaGun.getPersonel();
				StringBuffer sb = null;
				if (personel != null) {
					List<Personel> list = getGorevPersonelList(personel.getPlanGrup2());
					if (list != null) {
						sb = new StringBuffer();
						for (Iterator iterator = list.iterator(); iterator.hasNext();) {
							Personel personel2 = (Personel) iterator.next();
							sb.append(personel2.getPdksSicilNo() + " " + personel2.getAdSoyad());
							if (iterator.hasNext())
								sb.append("\n");
						}
					}
					bolumAdi = personel.getPlanGrup2() != null ? personel.getPlanGrup2().getAciklama() : "Tanımsız";
				}

				col = 0;
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
				Cell cellGorev = ExcelUtil.getCell(sheet, row, col++, styleGenelLeft);
				cellGorev.setCellValue(bolumAdi);
				if (sb != null)
					ExcelUtil.setCellComment(cellGorev, anchor, helper, drawing, sb.toString());

				Cell cellBaslik = ExcelUtil.getCell(sheet, row, col++, styleGenelCenter);
				String title = vardiya.getVardiyaAciklama();
				if (title != null) {
					Comment comment1 = drawing.createCellComment(anchor);
					RichTextString str1 = helper.createRichTextString(title);
					comment1.setString(str1);
					cellBaslik.setCellComment(comment1);

				}
				cellBaslik.setCellValue(vardiya.getKisaAciklama());

				for (Integer ay : vardiya.getGunlukList()) {
					Cell cell = ExcelUtil.getCell(sheet, row, col++, styleTutarDay);
					cell.setCellValue(ay != 0 ? ay.toString() : "");
				}
			}
		}
		try {
			for (int i = 0; i < col; i++)
				sheet.autoSizeColumn(i);
			if (yoneticiRol) {
				AylikPuantaj aylikPuantajSablon = new AylikPuantaj(denklestirmeAy);
				aylikPuantajSablon.setLoginUser(authenticatedUser);
				List<Personel> donemPerList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(null, null, null, null, denklestirmeAy != null ? aylikPuantajSablon : null, true, session);
				if (donemPerList.isEmpty() == false) {
					ArrayList<Long> idler = (ArrayList<Long>) ortakIslemler.getBaseObjectIdList(donemPerList);
					String fieldName = "p";
					HashMap fields = new HashMap();
					StringBuffer sb = new StringBuffer();
					sb.append("select distinct CM.* from " + PersonelDenklestirme.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK());
					sb.append(" inner join " + CalismaModeliAy.TABLE_NAME + " CMA " + PdksEntityController.getJoinLOCK() + " on CMA." + CalismaModeliAy.COLUMN_NAME_ID + " = S." + PersonelDenklestirme.COLUMN_NAME_CALISMA_MODELI_AY);
					sb.append(" inner join " + CalismaModeli.TABLE_NAME + " CM " + PdksEntityController.getJoinLOCK() + " on CM." + CalismaModeliAy.COLUMN_NAME_ID + " = CMA." + CalismaModeliAy.COLUMN_NAME_CALISMA_MODELI);
					sb.append(" where S." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId() + " and S." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
					fields.put(fieldName, idler);
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<CalismaModeli> list = pdksEntityController.getSQLParamList(idler, sb, fieldName, fields, CalismaModeli.class, session);
					for (CalismaModeli calismaModeli : list) {
						if (calismaModeli != null && calismaModeli.getDurum()) {
							if (!modelMap.containsKey(calismaModeli.getId()))
								modelMap.put(calismaModeli.getId(), calismaModeli);
						}
					}
					idler = null;
					list = null;
				}
				donemPerList = null;
			}
			if (!modelMap.isEmpty()) {
				styleTutarEvenDay = ExcelUtil.setAlignment(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
				styleTutarOddDay = ExcelUtil.setAlignment(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);

				HashMap<Long, List<Vardiya>> hashMap = new HashMap<Long, List<Vardiya>>();
				List<CalismaModeliVardiya> calismaModeliVardiyaList = pdksEntityController.getSQLParamByAktifFieldList(CalismaModeliVardiya.TABLE_NAME, CalismaModeliVardiya.COLUMN_NAME_CALISMA_MODELI, new ArrayList(modelMap.keySet()), CalismaModeliVardiya.class, session);
				for (CalismaModeliVardiya calismaModeliVardiya : calismaModeliVardiyaList) {
					Vardiya vardiya = calismaModeliVardiya.getVardiya();
					if (vardiya.isCalisma() && vardiya.getDurum()) {
						Long cmId = calismaModeliVardiya.getCalismaModeli().getId();
						List<Vardiya> vardiyaList = hashMap.containsKey(cmId) ? hashMap.get(cmId) : new ArrayList<Vardiya>();
						if (vardiyaList.isEmpty())
							hashMap.put(cmId, vardiyaList);
						vardiyaList.add(vardiya);
					}
				}
				List<CalismaModeli> cmList = new ArrayList<CalismaModeli>(modelMap.values());
				if (cmList.size() > 1)
					cmList = PdksUtil.sortObjectStringAlanList(cmList, "getAciklama", null);
				modelMap = null;
				for (CalismaModeli calismaModeli : cmList) {
					Long cmId = calismaModeli.getId();
					if (!hashMap.containsKey(cmId))
						continue;
					Sheet sheetModel = ExcelUtil.createSheet(wb, calismaModeli.getAciklama(), Boolean.TRUE);
					List<Vardiya> vardiyaList = hashMap.get(cmId);
					row = 0;
					col = 0;
					ExcelUtil.getCell(sheetModel, row, col, header).setCellValue(calismaModeli.getAciklama() + " " + ortakIslemler.calismaModeliAciklama() + " Vardiyaları");
					int adet = authenticatedUser.isAdmin() ? 5 : 4;
					for (int i = 0; i < adet; i++)
						ExcelUtil.getCell(sheetModel, row, col + i + 1, header).setCellValue("");

					try {
						sheetModel.addMergedRegion(ExcelUtil.getRegion((int) row, (int) 0, (int) row, (int) adet));
					} catch (Exception e) {
						e.printStackTrace();
					}

					if (vardiyaList.size() > 1)
						vardiyaList = PdksUtil.sortObjectStringAlanList(vardiyaList, "getAdi", null);
					++row;
					col = 0;
					if (authenticatedUser.isAdmin())
						ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue(ortakIslemler.vardiyaAciklama() + " Id");
					ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue("Saat Aralığı");
					ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue("Kısa Adı");
					ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue("Toplam Çalışma (Saat)");
					ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue("Mola (Dakika)");
					ExcelUtil.getCell(sheetModel, row, col++, header).setCellValue("Net Çalışma (Saat)");
					boolean renk = Boolean.TRUE;
					for (Vardiya vardiya : vardiyaList) {
						++row;
						col = 0;
						if (renk) {
							styleGenelLeft = styleOdd;
							styleGenelCenter = styleOddCenter;
							styleTutarDay = styleTutarOddDay;
						} else {
							styleGenelLeft = styleEven;
							styleGenelCenter = styleEvenCenter;
							styleTutarDay = styleTutarEvenDay;
						}
						renk = !renk;
						if (authenticatedUser.isAdmin())
							ExcelUtil.getCell(sheetModel, row, col++, styleGenelCenter).setCellValue(vardiya.getId());
						ExcelUtil.getCell(sheetModel, row, col++, styleGenelCenter).setCellValue(vardiya.getAdi());
						ExcelUtil.getCell(sheetModel, row, col++, styleGenelCenter).setCellValue(vardiya.getKisaAdi());
						double netSure = vardiya.getNetCalismaSuresi();
						double toplamSure = netSure + (vardiya.getYemekSuresi() / 60.d);
						ExcelUtil.getCell(sheetModel, row, col++, styleTutarDay).setCellValue(toplamSure);
						if (toplamSure > netSure)
							ExcelUtil.getCell(sheetModel, row, col++, styleGenelCenter).setCellValue(vardiya.getYemekSuresi().longValue());
						else
							ExcelUtil.getCell(sheetModel, row, col++, styleGenelCenter).setCellValue("");
						ExcelUtil.getCell(sheetModel, row, col++, styleTutarDay).setCellValue(netSure);
					}
					for (int i = 0; i < col; i++)
						sheetModel.autoSizeColumn(i);
				}
				cmList = null;
			}
			baos = new ByteArrayOutputStream();
			wb.write(baos);
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			baos = null;
		}
		onayDurumList = null;
		return baos;

	}

	/**
	 * @param sheet
	 * @param rowNo
	 * @param columnNo
	 * @param style
	 * @param deger
	 */
	public Cell setCell(Sheet sheet, int rowNo, int columnNo, CellStyle style, Double deger) {
		Cell cell = ExcelUtil.getCell(sheet, rowNo, columnNo, style);

		try {
			if (deger != 0.0d) {
				cell.setCellValue(authenticatedUser.sayiFormatliGoster(deger));
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
	 * @param deger
	 */
	public Cell setCell(Sheet sheet, int rowNo, int columnNo, CellStyle style, Integer deger) {
		Cell cell = ExcelUtil.getCell(sheet, rowNo, columnNo, style);

		try {
			if (deger != 0.0d) {
				cell.setCellValue(deger);
			}

		} catch (Exception e) {
		}
		return cell;
	}

	/**
	 * @return
	 */
	public String aylikVardiyaExcel() {
		try {
			for (Iterator iter = aylikPuantajList.iterator(); iter.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
				aylikPuantaj.setSecili(Boolean.TRUE);
			}
			String gorevYeriAciklama = getExcelAciklama();
			baosDosya = aylikVardiyaExcelDevam(gorevYeriAciklama, aylikPuantajList, Boolean.TRUE);
			if (baosDosya != null) {
				dosyaAdi = "AylıkÇalışmaPlanı_" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";
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
	 * @return
	 */
	private String getExcelAciklama() {

		LinkedHashMap<String, Object> veriMap = ortakIslemler.getListPersonelOzetVeriMap(aylikPuantajList, aramaSecenekleri.getTesisId(), " ");
		String gorevYeriAciklama = "";
		if (veriMap.containsKey("sirketGrup")) {
			Tanim sirketGrup = (Tanim) veriMap.get("sirketGrup");
			gorevYeriAciklama = sirketGrup.getAciklama() + "_";
		} else if (veriMap.containsKey("sirket")) {
			Sirket sirket = (Sirket) veriMap.get("sirket");
			gorevYeriAciklama = sirket.getAd() + "_";
		}

		if (aramaSecenekleri.getEkSaha3Id() != null || aramaSecenekleri.getTesisId() != null || aramaSecenekleri.getEkSaha4Id() != null) {
			Tanim ekSaha3 = null, ekSaha4 = null, tesis = null;
			if (aramaSecenekleri.getTesisId() != null) {
				if (veriMap.containsKey("tesis"))
					tesis = (Tanim) veriMap.get("tesis");
			}
			if (aramaSecenekleri.getEkSaha3Id() != null) {
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
	public String aylikRefresh() {
		if (!islemYapiliyor) {
			try {
				islemYapiliyor = Boolean.TRUE;
				if (personelAylikPuantaj.isGorevYeriSec()) {
					islemYapiliyor = Boolean.FALSE;
					if (personelAylikPuantaj != null && (!vardiyalarMap.isEmpty() || personelDenklestirme.isGuncellendi()) && personelAylikPuantaj.isKaydet())
						fillAylikVardiyaPlanList();

				} else if (personelAylikPuantaj != null && (!vardiyalarMap.isEmpty() || personelDenklestirme.isGuncellendi()) && personelAylikPuantaj.isKaydet()) {
					if (personelAylikPuantaj.getPersonelDenklestirme() != null && personelDenklestirme.isGuncellendi()) {
						personelDenklestirme.entityRefresh();
						// PersonelDenklestirme pd = (PersonelDenklestirme) ortakIslemler.objectRefresh("id", personelDenklestirme.getId(), PersonelDenklestirme.class, session);
						// personelAylikPuantaj.setPersonelDenklestirme(pd);
					}

					if (sablonGuncelle) {
						for (VardiyaHafta pdksVardiyaHafta : personelAylikPuantaj.getVardiyaHaftaList()) {
							try {
								if (pdksVardiyaHafta == null || pdksVardiyaHafta.getId() == null || !pdksVardiyaHafta.isGuncellendi())
									continue;
								pdksVardiyaHafta.entityRefresh();
							} catch (Exception e) {

							}
						}
					}

					for (Iterator iterator = vardiyalarMap.keySet().iterator(); iterator.hasNext();) {
						String key = (String) iterator.next();
						VardiyaGun pdksVardiyaGun = vardiyalarMap.get(key);
						if (pdksVardiyaGun.getId() != null && pdksVardiyaGun.isGuncellendi()) {
							Boolean refresh = Boolean.TRUE;
							try {
								Vardiya seciliVardiya = pdksVardiyaGun.getVardiya();
								Long oldId = pdksVardiyaGun.getEskiVardiya() != null && pdksVardiyaGun.getEskiVardiya().getId() != null ? pdksVardiyaGun.getEskiVardiya().getId() : 0L, newId = seciliVardiya != null && seciliVardiya.getId() != null ? seciliVardiya.getId() : 0l;
								refresh = PdksUtil.isLongDegisti(oldId, newId);
								if (refresh) {
									pdksVardiyaGun.setVardiya(vardiyaDbMap.get(oldId));
									session.refresh(pdksVardiyaGun);
								}

							} catch (Exception e1) {
							}

							// if (refresh)
							// entityManager.refresh(pdksVardiyaGun);
							if (pdksVardiyaGun.getVardiyaGorev().getId() != null)
								session.refresh(pdksVardiyaGun.getVardiyaGorev());
							else
								pdksVardiyaGun.getVardiyaGorev().setYeniGorevYeri(null);
						}

					}
					vardiyaDbMap = null;

					if (!helpPersonel(personelAylikPuantaj.getPdksPersonel())) {
						calismaPlaniDenklestir(departmanDenklestirmeDonemi, null, personelAylikPuantaj);

					}
				}
			} catch (Exception ee) {
				logger.info(ee.getMessage());
				ee.printStackTrace();
			} finally {
				islemYapiliyor = Boolean.FALSE;
			}
		}

		return "";
	}

	public String setPersonelDenklestirmeDinamikAlan(PersonelDenklestirmeDinamikAlan pda) {
		if (pda != null)
			pda.setGuncellendi(true);
		personelDenklestirme.setGuncellendi(true);
		return "";
	}

	/**
	 * @param aylikPuantaj
	 * @param tipi
	 * @return
	 */
	public String aylikPuantajSec(AylikPuantaj aylikPuantaj, String tipi) {
		kartBasmayanPersonel = false;
		if (personelDenklestirmeDinamikAlanList == null)
			personelDenklestirmeDinamikAlanList = new ArrayList<PersonelDenklestirmeDinamikAlan>();
		else
			personelDenklestirmeDinamikAlanList.clear();
		personelGebeDurum = null;
		personelSutIzniDurum = null;
		Personel personel = aylikPuantaj.getPdksPersonel();
		if (tipi.equalsIgnoreCase("P")) {
			Sirket sirket = personel.getSirket();
			HashMap fields = new HashMap();
			if (denklestirmeAyDurum && ikRole) {
				int adet = 25;
				if (!PdksUtil.hasStringValue(sicilNo))
					adet = aylikPuantajList.size();

				StringBuffer sb = new StringBuffer();
				sb.append(" with DATA as ( ");
				sb.append("	select CMA." + CalismaModeliAy.COLUMN_NAME_ID + ", COUNT (*) as ADET from " + PersonelDenklestirme.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
				sb.append("	inner join " + CalismaModeliAy.TABLE_NAME + " CMA " + PdksEntityController.getJoinLOCK() + " on CMA." + CalismaModeliAy.COLUMN_NAME_ID + " = D." + PersonelDenklestirme.COLUMN_NAME_CALISMA_MODELI_AY);
				sb.append("	where D." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
				sb.append("	group by CMA." + CalismaModeliAy.COLUMN_NAME_ID);
				sb.append("	), ");
				sb.append(" VERI as ( ");
				sb.append(" select CMA." + CalismaModeliAy.COLUMN_NAME_ID + ", CASE WHEN COALESCE(ADET,0) >= " + adet + "  THEN ADET ELSE 0 END ADET from " + CalismaModeliAy.TABLE_NAME + " CMA " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" left join DATA D " + PdksEntityController.getJoinLOCK() + " on D." + CalismaModeliAy.COLUMN_NAME_ID + " = CMA." + CalismaModeliAy.COLUMN_NAME_ID);
				sb.append("	where CMA." + CalismaModeliAy.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
				sb.append(" ) ");
				sb.append("	select CMA.* from VERI V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append("	inner join " + CalismaModeliAy.TABLE_NAME + " CMA " + PdksEntityController.getJoinLOCK() + " on V." + CalismaModeliAy.COLUMN_NAME_ID + " = CMA." + CalismaModeliAy.COLUMN_NAME_ID);
				sb.append("	inner join " + CalismaModeli.TABLE_NAME + " CM " + PdksEntityController.getJoinLOCK() + " on CM." + CalismaModeli.COLUMN_NAME_ID + " = CMA." + CalismaModeliAy.COLUMN_NAME_CALISMA_MODELI);
				sb.append("	AND ( CM." + CalismaModeli.COLUMN_NAME_DEPARTMAN + " is null or CM." + CalismaModeli.COLUMN_NAME_DEPARTMAN + " = " + sirket.getDepartman().getId() + " )");
				sb.append("	AND ( CM." + CalismaModeli.COLUMN_NAME_SIRKET + " is null or CM." + CalismaModeli.COLUMN_NAME_SIRKET + " = " + sirket.getId() + " )");
				sb.append("	order by V.ADET desc, CM." + CalismaModeli.COLUMN_NAME_ACIKLAMA);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				modelList = pdksEntityController.getObjectBySQLList(sb, fields, CalismaModeliAy.class);
			} else
				modelList = new ArrayList<CalismaModeliAy>();
			if (aylikPuantaj.getPersonelDenklestirme() != null)
				ortakIslemler.addObjectList(aylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy(), modelList, null);
			List<Long> idList = new ArrayList<Long>();
			for (Iterator iterator = modelList.iterator(); iterator.hasNext();) {
				CalismaModeliAy cma = (CalismaModeliAy) iterator.next();
				CalismaModeli cm = cma.getCalismaModeli();
				if (cma.getDurum().booleanValue() == false && cm.getDurum().booleanValue() == false)
					iterator.remove();
				else if (cma.getId() == null || idList.contains(cma.getId()) || !cma.getDenklestirmeAy().getId().equals(denklestirmeAy.getId())) {
					iterator.remove();
				} else
					idList.add(cma.getId());

			}
			personelSutIzniDurum = null;
			personelGebeDurum = null;
			if (personel.isGebelikSutIzinVar()) {
				fields.clear();
				Date bitTarih = null, basTarih = null;
				for (VardiyaHafta vardiyaHafta : aylikPuantajDefault.getVardiyaHaftaList()) {
					if (basTarih == null || basTarih.after(vardiyaHafta.getBasTarih()))
						basTarih = vardiyaHafta.getBasTarih();
					if (bitTarih == null || bitTarih.before(vardiyaHafta.getBitTarih()))
						bitTarih = vardiyaHafta.getBitTarih();
				}
				List<PersonelDonemselDurum> list = pdksEntityController.getSQLParamByAktifFieldList(PersonelDonemselDurum.TABLE_NAME, PersonelDonemselDurum.COLUMN_NAME_PERSONEL, personel.getId(), PersonelDonemselDurum.class, session);
				personelSutIzniDurum = aylikPuantaj.getPersonelDenklestirme() != null ? aylikPuantaj.getPersonelDenklestirme().getSutIzniPersonelDonemselDurum() : null;
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					PersonelDonemselDurum pdd = (PersonelDonemselDurum) iterator.next();
					boolean donemIci = basTarih.getTime() <= pdd.getBitTarih().getTime() && bitTarih.getTime() >= pdd.getBasTarih().getTime();
					if (pdd.isGebe()) {
						if (donemIci)
							personelGebeDurum = pdd;
						if (personelGebeDurum == null) {
							personelGebeDurum = new PersonelDonemselDurum();
							personelGebeDurum.setBasTarih(PdksUtil.tariheGunEkleCikar(basTarih, -36));
							personelGebeDurum.setBitTarih(PdksUtil.tariheGunEkleCikar(basTarih, -1));
							personelGebeDurum.setPersonel(aylikPuantaj.getPdksPersonel());
							personelGebeDurum.setPersonelDurumTipiId(PersonelDurumTipi.GEBE.value());
						}
					}

				}
				list = null;
			}
		}
		aylikPuantajMesaiTalepList = null;
		setPersonelAylikPuantaj(aylikPuantaj);
		manuelHareketEkle = null;
		hareketPdksList = null;
		gebeSutIzniGuncelle = false;
		fazlaMesaiTalep = null;
		kayitliVardiyalarMap.clear();
		vardiyalarMap.clear();
		boolean kaydet = !authenticatedUser.isAdmin() && denklestirmeAyDurum;

		aylikPuantaj.setKaydet(kaydet);

		ozelDurumList = ortakIslemler.getSelectItemList("ozelDurum", authenticatedUser);

		ozelDurumList.add(new SelectItem(VardiyaGorev.OZEL_ISTEK_YOK, ""));
		ozelDurumList.add(new SelectItem(VardiyaGorev.OZEL_ISTEK_PERSONEL, "Özel İstek"));
		if (authenticatedUser.getDepartman().isAdminMi()) {
			ozelDurumList.add(new SelectItem(VardiyaGorev.OZEL_ISTEK_EGITIM, "Eğitim"));
		}
		ozelDurumList.add(new SelectItem(VardiyaGorev.OZEL_RAPOR_IZNI, "Rapor"));
		if (aylikPuantaj.getSablonAylikPuantaj().getSonGun().getTime() < personel.getSonCalismaTarihi().getTime())
			ozelDurumList.add(new SelectItem(VardiyaGorev.OZEL_ISTIFA, "İstifa"));

		personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();

		if (personelDenklestirme.getId() != null)
			session.refresh(personelDenklestirme);
		personelDenklestirme.setGuncellendi(Boolean.FALSE);
		personelDenklestirme.clone();
		try {
			if (tipi.equals("P")) {
				StringBuffer sb = new StringBuffer();
				sb.append("SP_GET_PERS_DENK_DINAMIK_ALAN");
				LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
				fields.put("pdId", personelDenklestirme.getId().toString());
				fields.put("durum", 0);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List list = pdksEntityController.execSPList(fields, sb, null);
				List<Long> idList = new ArrayList<Long>();
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Object[] object = (Object[]) iterator.next();
					if (object != null) {
						if (object[0] != null)
							idList.add(((BigDecimal) object[0]).longValue());

					}
				}
				TreeMap<Long, PersonelDenklestirmeDinamikAlan> map = new TreeMap<Long, PersonelDenklestirmeDinamikAlan>();
				if (!idList.isEmpty()) {
					List<PersonelDenklestirmeDinamikAlan> pdList = pdksEntityController.getSQLParamByFieldList(PersonelDenklestirmeDinamikAlan.TABLE_NAME, PersonelDenklestirmeDinamikAlan.COLUMN_NAME_ID, idList, PersonelDenklestirmeDinamikAlan.class, session);
					for (Iterator iterator = pdList.iterator(); iterator.hasNext();) {
						PersonelDenklestirmeDinamikAlan pdda = (PersonelDenklestirmeDinamikAlan) iterator.next();
						if (denklestirmeAy.getBakiyeSifirlaDurum().booleanValue() == false) {
							if (pdda.getAlan().getKodu().equals(PersonelDenklestirmeDinamikAlan.TIPI_BAKIYE_SIFIRLA))
								iterator.remove();
							continue;
						}
						map.put(pdda.getAlan().getId(), pdda);
						pdda.setGuncellendi(Boolean.FALSE);

					}
					pdList = null;

				}
				if (!dinamikAlanlar.isEmpty()) {
					for (Tanim tanim : dinamikAlanlar) {
						Long key = tanim.getId();
						PersonelDenklestirmeDinamikAlan pdda = map.containsKey(key) ? map.get(key) : new PersonelDenklestirmeDinamikAlan(personelDenklestirme, tanim);
						if (pdda.getId() == null) {
							pdda.setDurum(Boolean.TRUE);
							pdda.setIslemDurum(Boolean.FALSE);
						}
						pdda.setGuncellendi(Boolean.FALSE);
						personelDenklestirmeDinamikAlanList.add(pdda);
					}
				}
				map = null;
				list = null;
				idList = null;
				ortakIslemler.vardiyaCalismaModeliGuncelle(aylikPuantaj.getVardiyalar(), session);
			}
		} catch (Exception ed) {
			logger.error(ed);
			ed.printStackTrace();
		}

		aylikPuantaj.setGorevYeriSec(Boolean.FALSE);
		setVardiyaList(fillAllVardiyaList());
		VardiyaGun oncekiVardiya = null;
		TreeMap<String, VardiyaGun> vm = new TreeMap<String, VardiyaGun>();
		if (personel.isSutIzniKullan() || personelDenklestirme.isSutIzniVar())
			gebeSutIzniGuncelle = true;
		VardiyaGun vGun = null, vgAy = null, vgIlkAy = null;
		if (aylikPuantaj.getPersonelDenklestirme() != null)
			aylikPuantaj.getPersonelDenklestirme().setGuncellendi(Boolean.FALSE);
		vardiyaDbMap = new HashMap<Long, Vardiya>();
		for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
			pdksVardiyaGun.saklaVardiya();
			pdksVardiyaGun.setGuncellendi(Boolean.FALSE);
			if (pdksVardiyaGun.getVardiya() != null) {
				if (!vardiyaDbMap.containsKey(pdksVardiyaGun.getVardiya().getId()))
					vardiyaDbMap.put(pdksVardiyaGun.getVardiya().getId(), pdksVardiyaGun.getVardiya());
			}
			if (oncekiVardiya != null) {
				if (pdksVardiyaGun.getVardiya() != null) {
					oncekiVardiya.setSonrakiVardiyaGun(pdksVardiyaGun);
				}

			}
			if (!gebeSutIzniGuncelle && pdksVardiyaGun.isAyinGunu() && pdksVardiyaGun.getVardiya() != null && AylikPuantaj.getGebelikGuncelle())
				gebeSutIzniGuncelle = pdksVardiyaGun.getIzin() == null && pdksVardiyaGun.getVardiya().getGebelik();
			setVardiyalarToMap(pdksVardiyaGun, vm, aylikPuantaj);
			pdksVardiyaGun.setSonrakiVardiya(null);
			pdksVardiyaGun.setOncekiVardiyaGun(oncekiVardiya);
			if (pdksVardiyaGun.isAyinGunu())
				vgAy = pdksVardiyaGun;
			if (vGun == null) {
				if (pdksVardiyaGun.isAyinGunu())
					vgIlkAy = pdksVardiyaGun;
			}
			vGun = pdksVardiyaGun;
		}
		Calendar cal = Calendar.getInstance();
		if (vgIlkAy != null) {
			HashMap fields = new HashMap();
			fields.put("personel.id=", vGun.getPersonel().getId());
			fields.put("vardiyaDate>=", ortakIslemler.tariheGunEkleCikar(cal, vgIlkAy.getVardiyaDate(), -7));
			fields.put("vardiyaDate<=", ortakIslemler.tariheGunEkleCikar(cal, vgIlkAy.getVardiyaDate(), -1));
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<VardiyaGun> digerVardiyaGunList = pdksEntityController.getObjectByInnerObjectListInLogic(fields, VardiyaGun.class);
			for (VardiyaGun digerVardiyaGun : digerVardiyaGunList) {
				setVardiyalarToMap(digerVardiyaGun, vm, aylikPuantaj);
			}
		}
		if (vgAy != null && vGun.getId() != null && vGun.getId().equals(vgAy.getId())) {
			HashMap fields = new HashMap();
			fields.put("personel.id=", vGun.getPersonel().getId());
			fields.put("vardiyaDate>=", ortakIslemler.tariheGunEkleCikar(cal, vgAy.getVardiyaDate(), 1));
			fields.put("vardiyaDate<=", ortakIslemler.tariheGunEkleCikar(cal, vgAy.getVardiyaDate(), 7));
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<VardiyaGun> digerVardiyaGunList = pdksEntityController.getObjectByInnerObjectListInLogic(fields, VardiyaGun.class);
			for (VardiyaGun digerVardiyaGun : digerVardiyaGunList) {
				setVardiyalarToMap(digerVardiyaGun, vm, aylikPuantaj);
			}
		}
		planYetkilendir(aylikPuantaj);
		sablonGuncelle = authenticatedUser == null && denklestirmeAyDurum && personelAylikPuantaj.isKaydet() && aylikPuantaj.isKaydet();
		if (sablonGuncelle) {
			String sablonGuncelleStr = ortakIslemler.getParameterKey("sablonGuncellemeyenDepartmanlar");
			if (PdksUtil.hasStringValue(sablonGuncelleStr)) {
				List<String> depList = Arrays.asList(sablonGuncelleStr.split(","));
				sablonGuncelle = !depList.contains(personel.getSirket().getDepartman().getId().toString());
			}
			// && !personel.getSirket().getDepartman().isAdminMi();
		}

		if (sablonGuncelle) {
			sablonGuncelle = Boolean.FALSE;
			if (aylikPuantaj.getVardiyaHaftaList() != null) {
				for (VardiyaHafta pdksVardiyaHafta : aylikPuantaj.getVardiyaHaftaList()) {
					if (pdksVardiyaHafta == null)
						continue;
					pdksVardiyaHafta.setGuncellendi(Boolean.FALSE);
					Boolean durum = null;
					for (VardiyaGun vardiyaGun : pdksVardiyaHafta.getVardiyaGunler()) {
						if (vardiyaGun.isAylikGirisYap() && vardiyaGun.getIzin() == null && vardiyaGun.isKullaniciYetkili()) {
							if (durum == null)
								durum = Boolean.TRUE;
							if (vardiyaGun == null || vardiyaGun.getVardiya() == null)
								durum = Boolean.FALSE;
							else
								kayitliVardiyalarMap.put(vardiyaGun.getVardiyaDateStr(), vardiyaGun.getVardiya());
						}

					}
					pdksVardiyaHafta.setDurum(durum != null && durum);
					if (!sablonGuncelle)
						sablonGuncelle = pdksVardiyaHafta.getDurum();

				}
				if (sablonGuncelle)
					fillVardiyaSablonList(personel);
			}

		}

		vardiyalarSec(aylikPuantaj);
		fazlaMesaiTalep = null;
		if (tipi.equals("M")) {
			seciliVardiyaGun = null;
			TreeMap<Long, VardiyaGun> varMap = new TreeMap<Long, VardiyaGun>();
			try {
				for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
					vg.setFazlaMesaiTalepler(null);
					if (vg.getId() != null && vg.isAyinGunu())
						varMap.put(vg.getId(), vg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (!varMap.isEmpty()) {
				List idList = new ArrayList(varMap.keySet());
				String fieldName = "vardiyaGun.id";
				HashMap fields = new HashMap();
				fields.put(fieldName, idList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<FazlaMesaiTalep> fazlaMesaiTalepler = ortakIslemler.getParamList(false, idList, fieldName, fields, FazlaMesaiTalep.class, session);
				if (fazlaMesaiTalepler.size() > 1)
					fazlaMesaiTalepler = PdksUtil.sortListByAlanAdi(fazlaMesaiTalepler, "id", Boolean.TRUE);
				if (!fazlaMesaiTalepler.isEmpty()) {
					for (FazlaMesaiTalep fmt : fazlaMesaiTalepler) {
						VardiyaGun vg = vm.get(fmt.getVardiyaGun().getVardiyaKeyStr());
						if (vg.getFazlaMesaiTalepler() == null)
							vg.setFazlaMesaiTalepler(new ArrayList<FazlaMesaiTalep>());
						vg.getFazlaMesaiTalepler().add(fmt);
					}
				}
			}

		}
		fazlaMesaiDurum = null;
		if (tipi.equals("M")) {
			if (!vm.isEmpty())
				ortakIslemler.fazlaMesaiSaatiAyarla(vm);
			if (denklestirmeAyDurum) {
				List<AylikPuantaj> puantajList = new ArrayList<AylikPuantaj>();
				puantajList.add(aylikPuantaj);
				List<String> list = fazlaMesaiOrtakIslemler.getFazlaMesaiUyari(yil, ay, aramaSecenekleri.getEkSaha3Id(), puantajList, session);
				for (String string : list)
					PdksUtil.addMessageAvailableWarn(string);
			}

		} else if (tipi.equals("P")) {
			fillCalismaModeliVardiyaList(personelAylikPuantaj.getCalismaModeli());
			if (calismaPlanKilit != null && calismaPlanKilit.getKilitDurum() && personelAylikPuantaj.getPersonelDenklestirme().getOlusturmaTarihi().before(calismaPlanKilit.getGuncellemeTarihi()))
				personelAylikPuantaj.setKaydet(false);
			if (denklestirmeAyDurum) {
				if (ortakIslemler.getParameterKey("fazlaMesaiIzinKullan").equals("1")) {
					Sirket sirket = personel.getSirket();
					if (sirket.getFazlaMesaiIzinKullan() == null || sirket.getFazlaMesaiIzinKullan().booleanValue() == false) {
						if (personelDenklestirme.isFazlaMesaiIzinKullanacak())
							fazlaMesaiDurum = 3;
						else
							fazlaMesaiDurum = personelDenklestirme.getFazlaMesaiOde() == null || personelDenklestirme.getFazlaMesaiOde().booleanValue() == false ? 1 : 2;
						fazlaMesaiDurumList = ortakIslemler.getSelectItemList("fazlaMesaiDurum", authenticatedUser);
						fazlaMesaiDurumList.add(new SelectItem(1, "Fazla Mesai Denkleştir"));
						fazlaMesaiDurumList.add(new SelectItem(2, "Fazla Mesai Öde"));
						fazlaMesaiDurumList.add(new SelectItem(3, ortakIslemler.fmIzinKullanAciklama()));
					}
				}
				if (personel.getSablon() != null) {
					VardiyaSablonu vardiyaSablonu = personel.getSablon();
					List<Vardiya> vardiyaList = vardiyaSablonu.getVardiyaList();
					boolean izinVardiyaVar = false;
					for (Vardiya vardiya : vardiyaList) {
						if (vardiya.isIzinVardiya())
							izinVardiyaVar = true;
					}
					vardiyaList = null;
					if (izinVardiyaVar) {
						for (VardiyaGun vg : personelAylikPuantaj.getVardiyalar()) {
							Vardiya vardiya = vg.getVardiya();
							if (vg.isAyinGunu() && vardiya != null) {
								if (vardiya.isCalisma()) {
									kartBasmayanPersonel = true;
									break;
								}

							}

						}
					}
				}

			}
		}

		return "";
	}

	/**
	 * @param vg
	 * @param vm
	 * @param puantaj
	 */
	private void setVardiyalarToMap(VardiyaGun vg, TreeMap<String, VardiyaGun> vm, AylikPuantaj puantaj) {
		vg.setIslemVardiya(null);
		vg.setIslendi(Boolean.FALSE);
		vg.setVardiyaZamani();
		vg.setSonrakiVardiya(null);
		vg.setHareketHatali(Boolean.FALSE);
		vg.saklaVardiya();
		vg.setGuncellendi(Boolean.FALSE);
		PersonelIzin izin = vg.getIzin();
		String vardiyaKey = vg.getVardiyaKey();
		if (izin != null) {
			boolean izinGirisYok = izin.getIzinTipi().getPersonelGirisTipi().equals(IzinTipi.GIRIS_TIPI_YOK);
			CalismaModeli cm = null;
			try {
				cm = offIzinGuncelle && izinGirisYok && puantaj != null ? puantaj.getPersonelDenklestirme().getCalismaModeli() : null;
			} catch (Exception e) {
				cm = null;
			}

			boolean izinGuncelleme = cm == null;
			if (ikRole == false || denklestirmeAyDurum == false || vg.getTatil() != null || vg.getVardiya().isOffGun() == false)
				izinGuncelleme = true;
			else if (cm != null) {
				// int haftaGun = vg.getHaftaninGunu();
				boolean cumartesiCalisiyor = cm.isHaftaTatilVar();
				if (vg.isHaftaIci() && cumartesiCalisiyor == false)
					izinGuncelleme = !offIzinGuncelle;
			}
			if (izinGuncelleme)
				vg.setIzin(izin);
		}
		vm.put(vardiyaKey, vg);

	}

	/**
	 * @param pdksVardiyaHafta
	 * @return
	 */
	public String aylikSablonGuncelle(VardiyaHafta pdksVardiyaHafta) {
		VardiyaSablonu vardiyaSablonu = pdksVardiyaHafta.getVardiyaSablonu();
		pdksVardiyaHafta.setGuncellendi(Boolean.TRUE);
		if (vardiyaSablonu != null) {
			logger.debug(pdksVardiyaHafta.getHafta() + " " + vardiyaSablonu.getAdi());
			vardiyaSablonu.vardiyaBul();
			List<Vardiya> sablonVardiyaList = vardiyaSablonu.getVardiyaList();
			int index = 0;
			for (VardiyaGun vardiyaGun : pdksVardiyaHafta.getVardiyaGunler()) {
				if (vardiyaGun.getVardiya() != null && vardiyaGun.isAylikGirisYap() && vardiyaGun.getIzin() == null && vardiyaGun.isKullaniciYetkili()) {
					Vardiya sablonVardiya = sablonVardiyaList.get(index);
					if (!sablonVardiya.getId().equals(vardiyaGun.getVardiya().getId())) {
						vardiyaGun.setVardiya(sablonVardiya);
						vardiyaDegistir(vardiyaGun);
					}
				}
				++index;
			}
		}
		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public String aylikKaydet() throws Exception {
		if (!islemYapiliyor) {
			islemYapiliyor = Boolean.TRUE;
			boolean flush = false;
			for (Iterator iterator = personelDenklestirmeDinamikAlanList.iterator(); iterator.hasNext();) {
				PersonelDenklestirmeDinamikAlan pda = (PersonelDenklestirmeDinamikAlan) iterator.next();
				if (pda.getAlan().getKodu().equals(PersonelDenklestirmeDinamikAlan.TIPI_BAKIYE_SIFIRLA)) {
					personelDenklestirme.setBakiyeSifirlaDurum(pda.getIslemDurum() != null && pda.getIslemDurum());
				}
				if (pda.isGuncellendi()) {
					if (pda.getId() != null || pda.getIslemDurum() || pda.getSayisalDeger() != null) {

						flush = true;
					}
				} else
					iterator.remove();

			}
			if (personelAylikPuantaj.getVardiyalar() != null && (!vardiyalarMap.isEmpty() || personelDenklestirme.isGuncellendi()) || flush)
				try {
					if (aylikVardiyaKontrolKaydet(Boolean.TRUE))
						vardiyalarMap.clear();
				} catch (Exception e) {
					e.printStackTrace();
					throw e;
				}

			islemYapiliyor = Boolean.FALSE;
		}
		aylikHareketKaydiVardiyaBul = Boolean.FALSE;
		if (denklestirmeAyDurum)
			aylikHareketKaydiVardiyalariBul();

		return "";
	}

	/**
	 * 
	 */
	private void aylikVardiyaOzetOlustur() {
		if (aylikVardiyaOzetList == null)
			aylikVardiyaOzetList = new ArrayList<VardiyaGun>();
		else
			aylikVardiyaOzetList.clear();
		if (gorevPersonelMap != null)
			gorevPersonelMap.clear();
		else
			gorevPersonelMap = new HashMap<Long, List<Personel>>();
		AylikPuantaj aylikPuantajToplam = new AylikPuantaj();
		TreeMap<String, VardiyaGun> vardiyaMap = new TreeMap<String, VardiyaGun>();
		VardiyaGun toplamVardiyaGun = new VardiyaGun();
		Vardiya toplamVardiya = new Vardiya();
		toplamVardiya.setKisaAdi("Toplam");
		toplamVardiya.setId(0L);
		toplamVardiyaGun.setVardiya(toplamVardiya);
		boolean gorevAciklama = aylikPuantajList != null && aylikPuantajList.size() > 1;
		for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
			if (gorevAciklama) {
				Personel personel = aylikPuantaj.getPdksPersonel();
				Tanim gorevTipi = personel != null ? personel.getGorevTipi() : null;
				if (gorevTipi != null) {
					List<Personel> list = gorevPersonelMap.containsKey(gorevTipi.getId()) ? gorevPersonelMap.get(gorevTipi.getId()) : new ArrayList<Personel>();
					if (list.isEmpty())
						gorevPersonelMap.put(gorevTipi.getId(), list);
					list.add(personel);
				}
			}
			aylikPuantaj.setDenklestirmeAy(denklestirmeAy);
			aylikPuantaj.setOnayDurum(aylikPuantaj.getPersonelDenklestirme() == null || aylikPuantaj.getPersonelDenklestirme().isOnaylandi() == false);
			puantajYetkilendir(vardiyaMap, aylikPuantaj, aylikPuantajToplam, toplamVardiyaGun);

		}
		if (!vardiyaMap.isEmpty()) {
			if (vardiyaMap.containsKey("0"))

				vardiyaMap.remove("0");

			aylikVardiyaOzetList.addAll(new ArrayList(vardiyaMap.values()));
			if (aylikVardiyaOzetList.size() > 1)
				aylikVardiyaOzetList.add(toplamVardiyaGun);
		}

	}

	/**
	 * @param vardiyaIdList
	 * @param tableName
	 * @param columnName
	 * @param tableClass
	 * @return
	 */
	private List getVardiyaTable(List<Long> vardiyaIdList, String tableName, String columnName, Class tableClass) {
		List list = null;
		try {
			if (vardiyaIdList != null && !vardiyaIdList.isEmpty()) {
				String fieldName = "v";
				HashMap map = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select P.* from " + tableName + " P " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where P." + columnName + " :" + fieldName);
				map.put(fieldName, vardiyaIdList);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// list = pdksEntityController.getObjectBySQLList(sb, map, tableClass);
				list = pdksEntityController.getSQLParamList(vardiyaIdList, sb, fieldName, map, tableClass, session);

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	/**
	 * 
	 */
	@Transactional
	private void savePlanLastParameter() {
		LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
		lastMap.put("yil", "" + yil);
		lastMap.put("ay", "" + ay);
		if (aramaSecenekleri.getDepartmanId() != null) {
			lastMap.put("departmanId", "" + aramaSecenekleri.getDepartmanId());
			lastMap.put("departman", ortakIslemler.getSelectItemText(aramaSecenekleri.getDepartmanId(), aramaSecenekleri.getDepartmanIdList()));
		}
		if (aramaSecenekleri.getSirketId() != null) {
			lastMap.put("sirketId", "" + aramaSecenekleri.getSirketId());
			lastMap.put("sirket", ortakIslemler.getSelectItemText(aramaSecenekleri.getSirketId(), aramaSecenekleri.getSirketIdList()));
		}
		if (aramaSecenekleri.getTesisId() != null) {
			lastMap.put("tesisId", "" + aramaSecenekleri.getTesisId());
			lastMap.put("tesis", ortakIslemler.getSelectItemText(aramaSecenekleri.getTesisId(), aramaSecenekleri.getTesisList()));
		}

		if (aramaSecenekleri.getEkSaha3Id() != null) {
			lastMap.put("bolumId", "" + aramaSecenekleri.getEkSaha3Id());
			lastMap.put("bolum", ortakIslemler.getSelectItemText(aramaSecenekleri.getEkSaha3Id(), aramaSecenekleri.getGorevYeriList()));
		}
		if (aramaSecenekleri.getEkSaha4Id() != null) {
			lastMap.put("altBolumId", "" + aramaSecenekleri.getEkSaha4Id());
			lastMap.put("altBolum", ortakIslemler.getSelectItemText(aramaSecenekleri.getEkSaha4Id(), aramaSecenekleri.getAltBolumIdList()));
		}
		if ((ikRole) && PdksUtil.hasStringValue(sicilNo))
			lastMap.put("sicilNo", sicilNo.trim());
		try {
			ortakIslemler.saveLastParameter(lastMap, session);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	/**
	 * @return
	 */
	public String gunlukListeleriOlustur() {
		List<Integer> list = new ArrayList<Integer>();
		for (VardiyaGun pdksVardiyaGun : aylikPuantajDefault.getAyinVardiyalari()) {
			if (pdksVardiyaGun.isCheckBoxDurum()) {
				int index = PdksUtil.getDateField(pdksVardiyaGun.getVardiyaDate(), Calendar.DATE) - 1;
				list.add(index);
			}
		}
		if (list.size() == 1) {
			int index = list.get(0);
			AylikPuantaj aylikPuantajToplam = new AylikPuantaj();
			for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
				puantajYetkilendir(null, aylikPuantaj, aylikPuantajToplam, null);
				List<VardiyaGun> gunler = aylikPuantaj.getAyinVardiyalari();
				VardiyaGun pdksVardiyaGun = gunler.get(index);
				pdksVardiyaGun.setGuncellendi(Boolean.FALSE);
				pdksVardiyaGun.saklaVardiya();
				aylikPuantaj.setVardiyaGun(pdksVardiyaGun);
			}

		} else if (list.size() > 1)
			PdksUtil.addMessageWarn("Bir'den fazla gün seçemezsiniz!");
		else
			PdksUtil.addMessageWarn("Bir gün seçiniz!");
		return "";
	}

	/**
	 * @return
	 */
	public String gunlukListeKaydet() {

		return "";
	}

	/**
	 * @param vardiyaMap
	 * @param aylikPuantaj
	 * @param aylikPuantajToplam
	 * @param toplamVardiyaGun
	 */
	private void puantajYetkilendir(TreeMap<String, VardiyaGun> vardiyaMap, AylikPuantaj aylikPuantaj, AylikPuantaj aylikPuantajToplam, VardiyaGun toplamVardiyaGun) {
		if (toplamVardiyaGun == null) {
			toplamVardiyaGun = new VardiyaGun();

			Vardiya toplamVardiya = new Vardiya();
			toplamVardiya.setKisaAdi("Toplam");
			toplamVardiya.setId(0L);
			toplamVardiyaGun.setVardiya(toplamVardiya);

		}
		Double yemekMolasiYuzdesi = ortakIslemler.getYemekMolasiYuzdesi(aylikPuantaj.getDenklestirmeAy(), session);
		String aksamBordroAltBirim = ortakIslemler.getParameterKey("sapAksamBordroAltBirim");
		String aksamBordroVardiyaKontrol = ortakIslemler.getParameterKey("aksamBordroVardiyaKontrol");
		List<String> aksamBordroAltBirimleri = Arrays.asList(aksamBordroAltBirim.split(","));
		boolean haftaTatilDurum = ortakIslemler.getParameterKey("haftaTatilDurum").equals("1");
		double aksamVardiyaSaatSayisi = 0d, haftaCalismaSuresi = 0d;
		int aksamVardiyaSayisi = 0;
		VardiyaGun vardiyaGun = null;
		boolean bayramAksamCalismaOde = ortakIslemler.getParameterKey("bayramAksamCalismaOde").equals("1");
		String donem = String.valueOf(yil * 100 + ay);

		for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
			if (vardiyaGun != null) {
				String tarih = pdksVardiyaGun.getVardiyaDateStr();
				pdksVardiyaGun.setDonemStr(donem);
				pdksVardiyaGun.setAyinGunu(tarih.startsWith(donem));
				Vardiya sonrakVardiya = null;
				if (pdksVardiyaGun.getVardiya() != null)
					sonrakVardiya = (Vardiya) pdksVardiyaGun.getVardiya().clone();
				else
					sonrakVardiya = null;
				vardiyaGun.setSonrakiVardiya(sonrakVardiya);
			}
			vardiyaGun = pdksVardiyaGun;

		}
		gorevli = helpPersonel(aylikPuantaj.getPdksPersonel());

		List<YemekIzin> yemekler = null;
		Date aksamVardiyaBaslangicZamani = null, aksamVardiyaBitisZamani = null;
		String keyDonem = String.valueOf(yil * 100 + ay);
		Personel personel = aylikPuantaj.getPdksPersonel();
		CalismaModeli calismaModeli = null;
		try {
			calismaModeli = aylikPuantaj.getPersonelDenklestirme().getCalismaModeli();
		} catch (Exception e) {
			calismaModeli = aylikPuantaj.getCalismaModeli();
		}
		Calendar cal = Calendar.getInstance();
		for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
			pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(keyDonem));
			if (pdksVardiyaGun != null)
				pdksVardiyaGun.setGorevliPersonelMap(gorevliPersonelMap);
			pdksVardiyaGun.setVardiyalar(null);
			aksamVardiyaBaslangicZamani = null;
			aksamVardiyaBitisZamani = null;
			Vardiya vardiya = pdksVardiyaGun.getVardiya();
			if (vardiya != null && vardiya.isAksamVardiyasi()) {
				if (aksamVardiyaBitSaat != null && aksamVardiyaBitDakika != null) {

					cal.setTime(pdksVardiyaGun.getVardiyaDate());
					cal.set(Calendar.HOUR_OF_DAY, aksamVardiyaBitSaat);
					cal.set(Calendar.MINUTE, aksamVardiyaBitDakika);
					if (vardiya.getBasDonem() > vardiya.getBitDonem())
						cal.add(Calendar.DATE, 1);
					aksamVardiyaBitisZamani = cal.getTime();
				}
				if (aksamVardiyaBasSaat != null && aksamVardiyaBasDakika != null) {

					cal.setTime(pdksVardiyaGun.getVardiyaDate());
					cal.set(Calendar.HOUR_OF_DAY, aksamVardiyaBasSaat);
					cal.set(Calendar.MINUTE, aksamVardiyaBasDakika);
					if (vardiya.getBasDonem() < vardiya.getBitDonem())
						cal.add(Calendar.DATE, -1);
					aksamVardiyaBaslangicZamani = cal.getTime();
				}
			}
			boolean kullaniciYetkili = Boolean.FALSE;
			boolean donemAcik = Boolean.FALSE;

			if (pdksVardiyaGun.getVardiya() != null) {

				if (pdksVardiyaGun.getVardiya() != null && pdksVardiyaGun.getIzin() == null) {
					Tanim yeniGoreviYeri = pdksVardiyaGun.getVardiyaGorev().getYeniGorevYeri();
					if (!gorevli)
						kullaniciYetkili = yeniGoreviYeri == null || gorevYerileri.isEmpty();
					else
						kullaniciYetkili = yeniGoreviYeri != null && gorevYerileri.contains(yeniGoreviYeri.getId());
				}

				if (pdksVardiyaGun.isAyinGunu()) {
					double haftaSuresi = 0d;
					if (kullaniciYetkili)
						if (vardiyaMap != null && pdksVardiyaGun.getVardiya().isCalisma() && !pdksVardiyaGun.isRaporIzni()) {
							int index = PdksUtil.getDateField(pdksVardiyaGun.getVardiyaDate(), Calendar.DATE) - 1;
							Vardiya islemVardiya = pdksVardiyaGun.getIslemVardiya();
							if (!gorevli && islemVardiya.getBasDonem() >= islemVardiya.getBitDonem()) {
								if (pdksVardiyaGun.getSonrakiVardiyaGun() != null) {
									try {
										if (haftaTatilDurum && pdksVardiyaGun.getSonrakiVardiya() != null && pdksVardiyaGun.getSonrakiVardiya().isHaftaTatil()) {
											double calismaToplamSuresi = islemVardiya.getNetCalismaSuresi();
											Date haftaTatil = ortakIslemler.tariheGunEkleCikar(cal, pdksVardiyaGun.getVardiyaDate(), 1);
											String haftaGunStr = PdksUtil.convertToDateString(haftaTatil, "yyyyMMdd").substring(6);
											double aksamSure = ortakIslemler.getSaatSure(islemVardiya.getVardiyaBasZaman(), haftaTatil, yemekler, pdksVardiyaGun, session);
											haftaSuresi = ortakIslemler.getSaatSure(haftaTatil, islemVardiya.getVardiyaBitZaman(), yemekler, pdksVardiyaGun, session);
											if (calismaToplamSuresi > haftaSuresi + aksamSure) {
												double fark = calismaToplamSuresi - (haftaSuresi + aksamSure);
												if (haftaSuresi > aksamSure)
													haftaSuresi += fark;
												else
													aksamSure += fark;
											}
											pdksVardiyaGun.setCalismaSuresi(aksamSure);
											if (haftaGunStr.equals("01")) {
												haftaSuresi = 0;
											}
											// if (islemVardiya.isAksamVardiyasi())
											// aksamVardiyaSaatSayisi += pdksVardiyaGun.getCalismaSuresi();
										}
									} catch (Exception eh) {
										logger.error(pdksVardiyaGun.getVardiyaKeyStr() + "\n" + eh);
										eh.printStackTrace();
									}

								}
							}
							vardiyaGunEkle(vardiyaMap, aylikPuantaj, pdksVardiyaGun, index);
							aylikPuantajToplam.setSablonAylikPuantaj(aylikPuantaj.getSablonAylikPuantaj());
							if (aylikPuantajToplam != null)
								vardiyaGunEkle(vardiyaMap, aylikPuantajToplam, toplamVardiyaGun, index);

						}
					if (calismaModeli != null && calismaModeli.isFazlaMesaiVarMi())
						pdksVardiyaGun.setHaftaCalismaSuresi(haftaSuresi);
					haftaCalismaSuresi += haftaSuresi;

				} else if (!helpPersonel(aylikPuantaj.getPdksPersonel())) {
					if (pdksVardiyaGun.getVardiyaDate().before(aylikPuantajDefault.getIlkGun())) {
						if (aylikPuantaj.getPersonelDenklestirmeGecenAy() != null)
							donemAcik = !aylikPuantaj.getPersonelDenklestirmeGecenAy().isErpAktarildi();
						else
							donemAcik = Boolean.TRUE;
						if (donemAcik && aylikPuantajDefault.getDenklestirmeGecenAy() != null)
							donemAcik = aylikPuantajDefault.getDenklestirmeGecenAy().getDurum();
					} else if (pdksVardiyaGun.getVardiyaDate().after(aylikPuantajDefault.getSonGun())) {
						if (aylikPuantaj.getPersonelDenklestirmeGelecekAy() != null)
							donemAcik = !aylikPuantaj.getPersonelDenklestirmeGelecekAy().isErpAktarildi();
						else
							donemAcik = Boolean.TRUE;
						if (donemAcik && aylikPuantajDefault.getDenklestirmeGelecekAy() != null)
							donemAcik = aylikPuantajDefault.getDenklestirmeGelecekAy().getDurum();
					}

				}
			}

			pdksVardiyaGun.setKullaniciYetkili(kullaniciYetkili);
			pdksVardiyaGun.setDonemAcik(donemAcik);
			List<YemekIzin> yemekList = pdksVardiyaGun.getYemekList();

			if (pdksVardiyaGun.isAyinGunu() && aksamVardiyaBaslangicZamani != null && aksamVardiyaBitisZamani != null && pdksVardiyaGun.getIslemVardiya() != null && pdksVardiyaGun.getIslemVardiya().isAksamVardiyasi()) {
				Date cikisZaman = pdksVardiyaGun.getIslemVardiya().getVardiyaBitZaman();
				Date girisZaman = pdksVardiyaGun.getIslemVardiya().getVardiyaBasZaman();
				if (girisZaman == null || cikisZaman == null)
					continue;
				if (!(girisZaman.getTime() <= aksamVardiyaBitisZamani.getTime() && cikisZaman.getTime() >= aksamVardiyaBaslangicZamani.getTime()))
					continue;

				if (girisZaman.before(aksamVardiyaBaslangicZamani))
					girisZaman = aksamVardiyaBaslangicZamani;
				if (cikisZaman.after(aksamVardiyaBitisZamani))
					cikisZaman = aksamVardiyaBitisZamani;
				if (girisZaman.before(cikisZaman)) {
					if (girisZaman.before(aksamVardiyaBaslangicZamani))
						girisZaman = aksamVardiyaBaslangicZamani;
					if (bayramAksamCalismaOde == false) {
						Tatil tatilSakli = null;
						if (pdksVardiyaGun.getIslemVardiya() != null) {
							String key = PdksUtil.convertToDateString(pdksVardiyaGun.getVardiyaDate(), "yyyyMMdd");
							if (tatilMap.containsKey(key))
								tatilSakli = tatilMap.get(key);
							else {
								key = PdksUtil.convertToDateString(pdksVardiyaGun.getIslemVardiya().getVardiyaBitZaman(), "yyyyMMdd");
								if (tatilMap.containsKey(key))
									tatilSakli = tatilMap.get(key);
							}
						}

						if (tatilSakli != null) {
							Tatil tatilAksam = tatilSakli;
							if (tatilAksam.getOrjTatil().isTekSefer())
								tatilAksam = (Tatil) tatilAksam.getOrjTatil().clone();
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
				}

				double sure = girisZaman.getTime() < cikisZaman.getTime() ? PdksUtil.setSureDoubleTypeRounded(ortakIslemler.getSaatSure(girisZaman, cikisZaman, yemekList, pdksVardiyaGun, session), pdksVardiyaGun.getYarimYuvarla()) : 0.0d;

				if (sure > 0) {

					double netSure = vardiya.getNetCalismaSuresi();
					if (sure > vardiya.getNetCalismaSuresi() * yemekMolasiYuzdesi && fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_ADET)) {
						aksamVardiyaSayisi += 1;
						if (sure > netSure)
							sure -= netSure;
						else
							sure = 0;
					}
					if (sure > 0)
						aksamVardiyaSaatSayisi += sure;

					if (fazlaMesaiMap.containsKey(AylikPuantaj.MESAI_TIPI_AKSAM_SAAT)) {
						pdksVardiyaGun.addAksamVardiyaSaatSayisi(sure);
						aksamVardiyaSaatSayisi += pdksVardiyaGun.getAksamVardiyaSaatSayisi();
					}

				}

			}
		}
		aylikPuantaj.setVardiyaDegisti(Boolean.FALSE);
		aylikPuantaj.setVardiyaOlustu(Boolean.TRUE);
		aylikPuantaj.setVardiyaDegisti(Boolean.FALSE);
		if (aylikPuantaj.getPersonelDenklestirme().getDenklestirmeAy().getDurum() && aylikPuantaj.getPersonelDenklestirme().getEgitimSuresiAksamGunSayisi() != null
				&& (aylikPuantaj.getPersonelDenklestirme().getPersonel().getEgitimDonemi() == null || !aylikPuantaj.getPersonelDenklestirme().getPersonel().getEgitimDonemi())) {
			try {
				aylikPuantaj.getPersonelDenklestirme().setEgitimSuresiAksamGunSayisi(null);

			} catch (Exception ee) {
				ee.printStackTrace();
			}

		}

		try {
			personel = aylikPuantaj != null ? aylikPuantaj.getPdksPersonel() : null;
			if (personel != null && (aksamBordroVardiyaKontrol.equals("1")) && (personel.getBordroAltAlan() == null || !aksamBordroAltBirimleri.contains(personel.getBordroAltAlan().getKodu()))) {
				aksamVardiyaSaatSayisi = 0d;

			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (haftaCalismaSuresi > 0) {
			calismaPlaniDenklestir(departmanDenklestirmeDonemi, null, aylikPuantaj);

		}

		aylikPuantaj.setAksamVardiyaSaatSayisi(aksamVardiyaSaatSayisi);
		aylikPuantaj.setAksamVardiyaSayisi(aksamVardiyaSayisi);

	}

	/**
	 * @param list
	 * @param pdksVardiyaGun
	 * @param vardiyaMap
	 */
	private void setVardiyaGunleri(List<Vardiya> list, VardiyaGun pdksVardiyaGun, TreeMap<Long, Vardiya> vardiyaMap) {
		Vardiya vardiya = pdksVardiyaGun.getVardiya();
		if (vardiya != null && vardiya.isFMI() && pdksVardiyaGun.isKullaniciYetkili()) {
			pdksVardiyaGun.setKullaniciYetkili(fazlaMesaiIzinRaporuDurum);
		}
		if (list == null || ((vardiya != null && vardiyaMap.containsKey(vardiya.getId())) && pdksVardiyaGun.getVardiya().getDurum() && pdksVardiyaGun.getVardiya().isFMI() == false))
			pdksVardiyaGun.setVardiyalar((ArrayList<Vardiya>) list);
		else {
			ArrayList<Vardiya> value = new ArrayList<Vardiya>(list);
			if (pdksVardiyaGun.isAyinGunu() == false && vardiya != null && vardiya.getDurum()) {
				value.add(vardiya);
			} else
				pdksVardiyaGun.setKontrolVardiyalar(value);
		}

	}

	/**
	 * @param vardiyaMap
	 * @param aylikPuantaj
	 * @param pdksVardiyaGun
	 * @param index
	 */
	private void vardiyaGunEkle(TreeMap<String, VardiyaGun> vardiyaMap, AylikPuantaj aylikPuantaj, VardiyaGun pdksVardiyaGun, int index) {
		List<Integer> gunlukList = null;
		Vardiya pdksVardiya = null;
		try {
			pdksVardiya = pdksVardiyaGun.getVardiya();
			String key = "";
			Personel personel = pdksVardiyaGun.getPersonel();
			if (personel != null)
				key += (personel.getPlanGrup2() != null ? personel.getPlanGrup2().getId().toString() : "00") + "_";
			key += pdksVardiya.getId();
			if (!vardiyaMap.containsKey(key)) {
				if (personel != null) {
					pdksVardiyaGun = (VardiyaGun) pdksVardiyaGun.clone();
					pdksVardiya = (Vardiya) pdksVardiya.clone();
					pdksVardiyaGun.setVardiya(pdksVardiya);
				}
				vardiyaMap.put(key, pdksVardiyaGun);
				gunlukList = new ArrayList<Integer>();
				pdksVardiya.setGunlukList(gunlukList);
				for (int i = 0; i < aylikPuantaj.getSablonAylikPuantaj().getGunSayisi(); i++)
					gunlukList.add(0);

			} else
				gunlukList = vardiyaMap.get(key).getVardiya().getGunlukList();
			if (gunlukList != null && index >= 0 && gunlukList.size() > index) {
				int deger = gunlukList.get(index);
				gunlukList.set(index, ++deger);
			}

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			logger.info(index + " hata " + pdksVardiya.getId());
		}

	}

	/**
	 * @param aylik
	 * @throws Exception
	 */
	@Transactional
	private boolean aylikVardiyaKontrolKaydet(Boolean aylik) throws Exception {
		boolean flush = Boolean.FALSE;
		boolean tekrarOku = Boolean.FALSE;
		boolean baskaKayitVar = Boolean.FALSE;
		String haftaTatilDurum = ortakIslemler.getParameterKey("haftaTatilDurum");
		haftaTatilMesaiDurum = haftaTatilDurum.equals("1");
		VardiyaPlan plan = personelAylikPuantaj.getVardiyaPlan();
		Personel personel = personelAylikPuantaj.getPdksPersonel();
		boolean pdGuncellendi = personelDenklestirme.isGuncellendi() && !personelDenklestirme.isKapandi();
		gorevli = helpPersonel(personel);
		if (plan.getVardiyaGunMap() == null)
			plan.setVardiyaGunMap(new TreeMap<String, VardiyaGun>());
		else
			plan.getVardiyaGunMap().clear();
		boolean durum = !vardiyalarMap.isEmpty();
		if (!helpPersonel(personelAylikPuantaj.getPdksPersonel()))
			personelAylikPuantaj.setFazlaMesaiHesapla(false);

		TreeMap<Long, VardiyaGun> mesaiMap = new TreeMap<Long, VardiyaGun>();
		boolean vardiyaGuncellendi = false;
		for (VardiyaGun pdksVardiyaGun : personelAylikPuantaj.getVardiyalar()) {
			if (pdksVardiyaGun.isGuncellendi() || pdksVardiyaGun.getId() == null) {
				try {
					pdksVardiyaGun.setHaftaTatilDigerSure(0.0d);
					pdksVardiyaGun.setHaftaCalismaSuresi(0.0d);
					pdksVardiyaGun.setCalismaSuresi(0.0d);
					pdksVardiyaGun.setResmiTatilSure(0.0d);
					Long newVardiyaId = pdksVardiyaGun.getVardiya() != null ? pdksVardiyaGun.getVardiya().getId() : 0L;
					Long eskiVardiyaId = pdksVardiyaGun.getEskiVardiya() != null ? pdksVardiyaGun.getEskiVardiya().getId() : 0L;
					pdksVardiyaGun.setGuncellendi(PdksUtil.isLongDegisti(newVardiyaId, eskiVardiyaId));
					if (!vardiyaGuncellendi)
						vardiyaGuncellendi = pdksVardiyaGun.isGuncellendi();
					if (pdksVardiyaGun.getId() == null && pdksVardiyaGun.getVardiya() != null) {
						if (pdksVardiyaGun.isGuncellendi()) {
							tekrarOku = true;
							saveOrUpdate(pdksVardiyaGun);
						}
					}
					if (pdksVardiyaGun.getId() != null && pdksVardiyaGun.isGuncellendi()) {
						pdGuncellendi = true;
						pdksVardiyaGun.setGuncelleyenUser(authenticatedUser);
						pdksVardiyaGun.setGuncellemeTarihi(new Date());
						pdksVardiyaGun.setDurum(Boolean.FALSE);
						if (pdksVardiyaGun.getVersion() < 0)
							pdksVardiyaGun.setVersion(0);
						mesaiMap.put(pdksVardiyaGun.getId(), pdksVardiyaGun);
						tekrarOku = Boolean.TRUE;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

				durum = Boolean.TRUE;
			}
			plan.getVardiyaGunMap().put(pdksVardiyaGun.getVardiyaDateStr(), pdksVardiyaGun);

		}

		if (durum || pdGuncellendi) {

			for (VardiyaHafta pdksVardiyaHafta : plan.getVardiyaHaftaList())
				pdksVardiyaHafta.setCheckBoxDurum(Boolean.TRUE);
			if (vardiyaGuncellendi && !vardiyalarMap.isEmpty())
				durum = vardiyaPlanKontrol(personelDenklestirme, null, null, plan, "", false) || ortakIslemler.getParameterKey("calismaPlanKaydetme").equals("1") == false;
			if (durum) {
				if (personelDenklestirme.isGuncellendi())
					flush = Boolean.TRUE;

				for (VardiyaGun pdksVardiyaGun : personelAylikPuantaj.getVardiyalar()) {
					if (pdksVardiyaGun.getVardiya() != null && pdksVardiyaGun.getId() != null) {
						VardiyaGorev pdksVardiyaGorev = pdksVardiyaGun.getVardiyaGorev();
						if (pdksVardiyaGun.isGuncellendi()) {
							if (pdksVardiyaGun.getVardiya().isCalisma())
								vardiyaSaatDakika(pdksVardiyaGun, false);
							else {
								pdksVardiyaGun.setBasSaat(null);
								pdksVardiyaGun.setBasDakika(null);
								pdksVardiyaGun.setBitSaat(null);
								pdksVardiyaGun.setBitDakika(null);
							}
							logger.debug("Plan " + pdksVardiyaGun.getVardiyaKeyStr());
							try {
								if (pdksVardiyaGun.getId() != null) {
									Long newVardiyaId = pdksVardiyaGun.getVardiya() != null ? pdksVardiyaGun.getVardiya().getId() : null;
									Long eskiVardiyaId = pdksVardiyaGun.getEskiVardiya() != null ? pdksVardiyaGun.getEskiVardiya().getId() : null;
									if (PdksUtil.isLongDegisti(newVardiyaId, eskiVardiyaId)) {
										pdGuncellendi = true;
										pdksVardiyaGun.setGuncelleyenUser(authenticatedUser);
										pdksVardiyaGun.setGuncellemeTarihi(new Date());
										pdksVardiyaGun.setDurum(Boolean.FALSE);
										tekrarOku = Boolean.TRUE;
									}
								}
							} catch (Exception e) {
							}

							if (pdksVardiyaGun.getId() == null || mesaiMap.containsKey(pdksVardiyaGun.getId())) {
								pdksVardiyaGun.setDurum(Boolean.FALSE);
								if (pdksVardiyaGun.getId() == null)
									tekrarOku = true;
								saveOrUpdate(pdksVardiyaGun);

								if (!pdGuncellendi)
									pdGuncellendi = pdksVardiyaGun.isAyinGunu();
								if (pdGuncellendi)
									personelDenklestirme.setOnaylandi(Boolean.FALSE);
							}
							flush = Boolean.TRUE;
							if (!pdksVardiyaGun.getVardiya().isCalisma())
								pdksVardiyaGorev.setYeniGorevYeri(null);
							if (gorevli) {
								if (pdksVardiyaGorev.getYeniGorevYeri() == null) {
									pdksVardiyaGorev.setBolumKat(null);
									tekrarOku = Boolean.TRUE;
								} else if (!gorevYerileri.contains(pdksVardiyaGorev.getYeniGorevYeri().getId())) {
									pdksVardiyaGorev.setBolumKat(null);
									tekrarOku = Boolean.TRUE;
								}

							}

							if (pdksVardiyaGorev.isShiftGorevli() || !pdksVardiyaGorev.isOzelDurumYok() || pdksVardiyaGorev.getYeniGorevYeri() != null || pdksVardiyaGorev.getBolumKat() != null) {
								if (pdksVardiyaGun.getId() == null)
									tekrarOku = true;
								if (gorevYeriGirisDurum)
									saveOrUpdate(pdksVardiyaGorev);
								logger.debug("Gorev " + pdksVardiyaGun.getVardiyaKeyStr());
								flush = Boolean.TRUE;
							}

							else if (pdksVardiyaGorev.getId() != null) {

								pdksEntityController.deleteObject(session, entityManager, pdksVardiyaGorev);

								pdksVardiyaGorev.setId(null);
								flush = Boolean.TRUE;
							}
							if (pdksVardiyaGun.isAyinGunu()) {
								personelDenklestirme.setGuncellendi(Boolean.TRUE);

							}

							flush = Boolean.TRUE;
						} else if (gorevli && pdksVardiyaGorev.getYeniGorevYeri() != null && !baskaKayitVar) {
							Tanim yeniGorevYeri = pdksVardiyaGorev.getYeniGorevYeri();
							baskaKayitVar = gorevYerileri.contains(yeniGorevYeri.getId());
						}

					}
				}
				if (!mesaiMap.isEmpty()) {
					List idList = new ArrayList(mesaiMap.keySet());
					String fieldName = "vardiyaGun.id";
					HashMap fields = new HashMap();
					fields.put(fieldName, idList);
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<PersonelFazlaMesai> list = ortakIslemler.getParamList(false, idList, fieldName, fields, PersonelFazlaMesai.class, session);
					for (PersonelFazlaMesai fazlaMesai : list) {
						if (fazlaMesai.isOnaylandi()) {
							fazlaMesai.setFazlaMesaiSaati(0.0d);
							fazlaMesai.setOnayDurum(PersonelFazlaMesai.DURUM_ONAYLANMADI);
							fazlaMesai.setGuncelleyenUser(authenticatedUser);
							fazlaMesai.setGuncellemeTarihi(new Date());
							saveOrUpdate(fazlaMesai);
							flush = Boolean.TRUE;
						}
					}
				}

			} else {
				for (VardiyaGun pdksVardiyaGun : personelAylikPuantaj.getVardiyalar()) {
					if (pdksVardiyaGun.getVardiya() != null && pdksVardiyaGun.isGuncellendi()) {
						vardiyalarMap.put(pdksVardiyaGun.getVardiyaDateStr(), pdksVardiyaGun);
						// session.refresh(pdksVardiyaGun);
						flush = false;
					}

				}
			}
		} else {
			flush = personelDenklestirme.isGuncellendi();
		}
		if (pdGuncellendi) {
			for (Iterator iterator = personelDenklestirmeDinamikAlanList.iterator(); iterator.hasNext();) {
				PersonelDenklestirmeDinamikAlan pda = (PersonelDenklestirmeDinamikAlan) iterator.next();
				if (pda.isGuncellendi()) {
					if (pda.getId() != null || pda.getIslemDurum()) {
						saveOrUpdate(pda);
						flush = true;
					}
				} else
					iterator.remove();

			}
		}

		if (!flush && vardiyaGuncellendi == false)
			flush = pdGuncellendi;
		if (flush) {
			if (pdGuncellendi) {
				boolean sutIzin = personel.isSutIzniKullan() || personelDenklestirme.isSutIzniVar();
				if (!sutIzin && gebeSutIzniGuncelle == false && personelDenklestirme.getCalismaModeli().isUpdateCGS() == false) {
					if (personelDenklestirme.getSutIzniSaatSayisi() != null && personelDenklestirme.getSutIzniSaatSayisi().doubleValue() > 0.0d)
						personelDenklestirme.setSutIzniSaatSayisi(0.0d);
				}

				if (fazlaMesaiDurum != null) {
					personelDenklestirme.setFazlaMesaiOde(fazlaMesaiDurum == 2);
					personelDenklestirme.setFazlaMesaiIzinKullan(fazlaMesaiDurum == 3);
				} else if (personelDenklestirme.getFazlaMesaiIzinKullan())
					personelDenklestirme.setFazlaMesaiOde(Boolean.FALSE);
				personelDenklestirme.setDurum(Boolean.FALSE);
				personelAylikPuantaj.setPersonelDenklestirme(personelDenklestirme);
				savePersonelDenklestirme(personelDenklestirme);
				if (personelDenklestirme.getCalismaModeliAy() != null && personelDenklestirme.getCalismaModeliAy().getDurum().booleanValue() == false) {
					CalismaModeliAy cma = personelDenklestirme.getCalismaModeliAy();
					cma.setDurum(Boolean.TRUE);
					saveOrUpdate(cma);
					fazlaMesaiOrtakIslemler.setDenklestirmeAySure(defaultAylikPuantajSablon.getVardiyalar(), aramaSecenekleri.getSirket(), denklestirmeAy, session);
				}
				logger.debug("Denklestirme " + personelDenklestirme.getPersonel().getPdksSicilNo());
				basliklariGuncelle(null);
				tekrarOku = true;

			}
			logger.debug("Veri tabanına kayıt ediliyor");
			suaKontrol(aylikPuantajList);
			try {
				personelAylikPuantaj.setOnayDurum(true);
				sessionFlush();
			} catch (InvalidStateException e) {
				flush = Boolean.FALSE;
				InvalidValue[] invalidValues = e.getInvalidValues();
				if (invalidValues != null) {
					for (InvalidValue invalidValue : invalidValues) {
						Object object = invalidValue.getBean();
						if (object != null && object instanceof VardiyaGun) {
							VardiyaGun pdksVardiyaGun = (VardiyaGun) object;
							PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(pdksVardiyaGun.getVardiyaDate(), PdksUtil.getDateFormat()) + " günü  alanı : " + invalidValue.getPropertyName() + " with message: " + invalidValue.getMessage());
						} else
							PdksUtil.addMessageAvailableWarn("Instance of bean class: " + invalidValue.getBeanClass().getSimpleName() + " has an invalid property: " + invalidValue.getPropertyName() + " with message: " + invalidValue.getMessage());
					}
				}
				logger.error(e);
				e.printStackTrace();

			} catch (Exception e) {
				PdksUtil.addMessageAvailableWarn(e.getMessage());
				flush = Boolean.FALSE;
				logger.error(e);
				e.printStackTrace();

			}
			if (flush) {
				if (dinamikAlanlar != null && !dinamikAlanlar.isEmpty()) {
					HashMap<Long, AylikPuantaj> pdIdMap = new HashMap<Long, AylikPuantaj>();
					pdIdMap.put(personelAylikPuantaj.getPersonelDenklestirme().getId(), personelAylikPuantaj);
					ortakIslemler.dinamikAlanlariDoldur(pdIdMap, session);
				}
				if (calismaPlanKilit != null)
					calismaPlanKilitKontrol();
				logger.debug("Plan kayıt edildi");
				for (VardiyaGun pdksVardiyaGun : aylikPuantajDefault.getVardiyalar()) {
					if (pdksVardiyaGun.isAyinGunu()) {
						pdksVardiyaGun.setCheckBoxDurum(Boolean.FALSE);
					}
				}
				islemYapiliyor = false;
				if (tekrarOku) {
					int index = -1;
					if (aylikPuantajList.size() > 1) {
						String orjSicilNo = new String(sicilNo);
						List<AylikPuantaj> aylikPuantajOrjinalList = new ArrayList<AylikPuantaj>(aylikPuantajList);
						for (int i = 0; i < aylikPuantajOrjinalList.size(); i++) {
							AylikPuantaj ap = aylikPuantajOrjinalList.get(i);
							if (ap.getPersonelDenklestirme().getId().equals(personelAylikPuantaj.getPersonelDenklestirme().getId())) {
								index = i;
								break;
							}
						}
						if (index >= 0) {
							session.flush();
							ortakIslemler.aylikPlanSureHesapla(false, normalCalismaVardiya, true, personelAylikPuantaj, false, tatilGunleriMap, session);
							aylikVardiyaOzetOlustur();

							sicilNo = orjSicilNo;
						}
						aylikPuantajOrjinalList = null;
					}
					if (index < 0)
						fillStartAylikVardiyaPlanList();

				}

			}

		}
		logger.debug("İşlem bitti.");
		return flush;

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
	 * @return
	 * @throws Exception
	 */
	private boolean getDenklestirmeDurum() {
		boolean denklestirme = true;
		if (denklestirmeAy != null)
			denklestirme = !denklestirmeAyDurum;
		return denklestirme;
	}

	/**
	 * @throws Exception
	 */
	private void fillDepartmanList() throws Exception {
		if (denklestirmeAy == null)
			setSeciliDenklestirmeAy();

		List<SelectItem> departmanListe = fazlaMesaiOrtakIslemler.getFazlaMesaiDepartmanList(denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
		fazlaMesaiListeGuncelle(null, "D", departmanListe);
		if (departmanListe.size() > 0) {
			Long depId = null;
			if (aramaSecenekleri.getDepartmanId() == null || departmanListe.size() == 1)
				depId = (Long) departmanListe.get(0).getValue();
			if (depId != null) {

				Departman departman = (Departman) pdksEntityController.getSQLParamByFieldObject(Departman.TABLE_NAME, Departman.COLUMN_NAME_ID, depId, Departman.class, session);

				aramaSecenekleri.setDepartmanId(departman.getId());
				aramaSecenekleri.setDepartman(departman);
			}

		}

		aramaSecenekleri.setDepartmanIdList(departmanListe);
	}

	/**
	 * @param dm
	 * @return
	 */
	private void setAylikPuantajDonem(DenklestirmeAy dm) {
		AylikPuantaj apd = null;
		try {
			if (dm != null && dm.getYil() == yil && dm.getAy() == ay) {
				if (aylikPuantajDonem == null)
					apd = new AylikPuantaj(dm);
				else {
					apd = aylikPuantajDonem;
					apd.setDenklestirmeAy(dm);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		if (apd == null)
			apd = new AylikPuantaj();
		if (loginUser == null) {
			if (authenticatedUser != null) {
				loginUser = authenticatedUser;
			} else {
				loginUser = ortakIslemler.getSistemAdminUser(session);
				loginUser.setAdmin(Boolean.TRUE);
			}
			loginUser.setLogin(authenticatedUser != null);

		}

		apd.setLoginUser(loginUser);
		setAylikPuantajDonem(apd);
	}

	/**
	 * @param vardiyaPlan
	 * @param base
	 * @param vardiya
	 * @param personel
	 * @return
	 */
	public VardiyaGun vardiyaGunKopyala(VardiyaPlan vardiyaPlan, VardiyaGun base, Vardiya vardiya, Personel personel) {
		VardiyaGun vardiyaGun = (VardiyaGun) base.clone();
		vardiyaGun.setPersonel(personel);
		vardiyaGun.setVardiya(vardiya);
		vardiyaPlan.getVardiyaGunMap().put(vardiyaGun.getVardiyaDateStr(), vardiyaGun);
		return vardiyaGun;
	}

	/**
	 * @param vardiyaPlan
	 * @param baseVardiya
	 * @param personel
	 * @return
	 */
	public VardiyaHafta haftaKopyala(VardiyaPlan vardiyaPlan, VardiyaHafta baseVardiya, Personel personel) {
		VardiyaHafta dataHafta = new VardiyaHafta();
		VardiyaSablonu sablonu = personel.getSablon();
		dataHafta.setPersonel(personel);
		dataHafta.setVardiyaSablonu(sablonu);
		dataHafta.setVardiyaPlan(vardiyaPlan);
		dataHafta.setVardiyaSablonu(new VardiyaSablonu());
		dataHafta.setBasTarih(baseVardiya.getBasTarih());
		dataHafta.setBitTarih(baseVardiya.getBitTarih());
		dataHafta.setHafta(baseVardiya.getHafta());
		dataHafta.setVardiyaGun1(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun1(), sablonu.getVardiya1(), personel));
		dataHafta.setVardiyaGun2(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun2(), sablonu.getVardiya2(), personel));
		dataHafta.setVardiyaGun3(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun3(), sablonu.getVardiya3(), personel));
		dataHafta.setVardiyaGun4(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun4(), sablonu.getVardiya4(), personel));
		dataHafta.setVardiyaGun5(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun5(), sablonu.getVardiya5(), personel));
		dataHafta.setVardiyaGun6(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun6(), sablonu.getVardiya6(), personel));
		dataHafta.setVardiyaGun7(vardiyaGunKopyala(vardiyaPlan, baseVardiya.getVardiyaGun7(), sablonu.getVardiya7(), personel));
		return dataHafta;

	}

	/**
	 * @param personelId
	 * @param izinler
	 * @param list
	 */
	public void setIzin(long personelId, List<PersonelIzin> izinler, List<VardiyaGun> vardiyaList) {
		List<VardiyaGun> vList = new ArrayList<VardiyaGun>();
		for (VardiyaGun pGun : vardiyaList) {
			if (pGun.getPersonel().getId().longValue() == personelId) {
				pGun.setIzinler(null);
				pGun.setIzin(null);
				vList.add(pGun);
			}
		}
		if (izinler != null) {
			List<PersonelIzin> izinList = new ArrayList<PersonelIzin>();
			for (Iterator<PersonelIzin> iterator = izinler.iterator(); iterator.hasNext();) {
				PersonelIzin personelIzin = (PersonelIzin) iterator.next();
				if (personelIzin.getIzinSahibi().getId().longValue() == personelId) {
					izinList.add(personelIzin);
				}
			}
			if (!izinList.isEmpty())
				ortakIslemler.vardiyaIzinleriGuncelle(izinList, vList);
			izinList = null;
		}

		vList = null;
	}

	/**
	 * @return
	 */
	public String izinGirisi() {
		personelIzinGirisiHome.setIzinliSahibi(vardiyaPlan.getPersonel());
		personelIzinGirisiHome.setBasDate(basTarih);
		personelIzinGirisiHome.setBitDate(bitTarih);
		StringBuilder donusDeger = new StringBuilder(MenuItemConstant.personelIzinGirisi);
		donusDeger.append("?personelId=" + vardiyaPlan.getPersonel().getId());
		donusDeger.append("&bastarih=" + PdksUtil.convertToDateString(basTarih, "yyyyMMdd"));
		String str = donusDeger.toString();
		donusDeger = null;
		return str;
	}

	/**
	 * @param pdksVardiyaGun
	 * @return
	 */
	public String getClass(VardiyaGun pdksVardiyaGun) {
		String classAdi = "haftaBaslik";
		try {
			if (pdksVardiyaGun != null) {
				String dateStr = PdksUtil.convertToDateString(pdksVardiyaGun.getVardiyaDate(), "yyyyMMdd");
				Tatil tatil = !getTatilMap().containsKey(dateStr) ? null : getTatilMap().get(dateStr);
				if (tatil != null)
					classAdi = !tatil.isYarimGunMu() ? "bayram" : "arife";
				pdksVardiyaGun.setTatil(tatil);
			}

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			classAdi = "";
		}
		return classAdi;
	}

	/**
	 * 
	 */
	public void aylariDoldur() {
		int buYil = PdksUtil.getDateField(new Date(), Calendar.YEAR);
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select distinct D.* from " + DenklestirmeAy.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK() + " ");
		if (fazlaMesaiTalepDurum == false) {
			if (buYil > yil) {
				sb.append(" inner join " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksEntityController.getJoinLOCK() + " on PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = D." + DenklestirmeAy.COLUMN_NAME_ID);
				sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DENKLESTIRME_DURUM + " = 1 ");
			}
		} else {
			sb.append(" inner join " + VardiyaGun.TABLE_NAME + " VG " + PdksEntityController.getJoinLOCK() + " on YEAR(VG." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + ")=" + yil);
			sb.append(" and MONTH(VG." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + ")=D." + DenklestirmeAy.COLUMN_NAME_AY);
			sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " FT " + PdksEntityController.getJoinLOCK() + " on FT." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = VG." + VardiyaGun.COLUMN_NAME_ID);
			sb.append(" and FT." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1");
		}
		sb.append(" where D." + DenklestirmeAy.COLUMN_NAME_YIL + " = :y and D." + DenklestirmeAy.COLUMN_NAME_AY + " > 0 ");
		if (yil == maxYil) {
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

		fields.put("y", yil);
		sb.append(" order by D." + DenklestirmeAy.COLUMN_NAME_AY);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<DenklestirmeAy> list = pdksEntityController.getObjectBySQLList(sb, fields, DenklestirmeAy.class);

		aylar = ortakIslemler.getSelectItemList("ay", authenticatedUser);
		int seciliAy = ay;
		ay = 0;
		for (DenklestirmeAy denklestirmeAy : list) {
			if (denklestirmeAy.getAy() == seciliAy)
				ay = seciliAy;
			aylar.add(new SelectItem(denklestirmeAy.getAy(), denklestirmeAy.getAyAdi()));
		}

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
		} else {
			Integer seciliAy = ay;
			ay = 0;
			for (SelectItem st : aylar) {
				if (st.getValue().equals(seciliAy))
					ay = seciliAy;
			}
		}
		setSeciliDenklestirmeAy();
		if (denklestirmeAy != null) {
			if (denklestirmeAy.getFazlaMesaiMaxSure() == null)
				fazlaMesaiOrtakIslemler.setFazlaMesaiMaxSure(denklestirmeAy, session);
			aramaSecenekleri.setTesisList(null);
			aramaSecenekleri.setGorevYeriList(null);
			Long sirketOnceki = aramaSecenekleri.getSirketId(), tesisIdOnceki = aramaSecenekleri.getTesisId(), bolumIdOnceki = aramaSecenekleri.getEkSaha3Id();
			fillSirketList();
			List<SelectItem> sirketIdList = aramaSecenekleri.getSirketIdList();
			if (sirketIdList != null && !sirketIdList.isEmpty()) {
				boolean bolumDoldurulmadi = true;
				if (sirketOnceki != null)
					aramaSecenekleri.setSirketId(null);
				if (sirketIdList.size() == 1)
					aramaSecenekleri.setSirketId((Long) sirketIdList.get(0).getValue());
				else if (sirketOnceki != null) {
					for (SelectItem selectItem : sirketIdList) {
						if (selectItem.getValue().equals(sirketOnceki))
							aramaSecenekleri.setSirketId(sirketOnceki);
					}
				}
				if (aramaSecenekleri.getSirketId() != null) {
					try {
						tesisDoldur(false);
					} catch (Exception e) {
						e.printStackTrace();
					}
					List<SelectItem> tesisList = aramaSecenekleri.getTesisList();
					if (tesisIdOnceki != null)
						aramaSecenekleri.setTesisId(null);
					if (tesisList != null && !tesisList.isEmpty()) {
						if (tesisList.size() == 1) {
							aramaSecenekleri.setTesisId((Long) tesisList.get(0).getValue());

						} else if (tesisIdOnceki != null) {
							for (SelectItem selectItem : tesisList) {
								if (selectItem.getValue().equals(tesisIdOnceki))
									aramaSecenekleri.setTesisId(tesisIdOnceki);
							}
						}
						bolumDoldur();
						bolumDoldurulmadi = false;
					}
					if (bolumIdOnceki != null)
						aramaSecenekleri.setEkSaha3Id(null);
					if (bolumDoldurulmadi)
						if (aramaSecenekleri.getTesisId() != null || bolumIdOnceki != null) {
							bolumDoldur();
						}
					List<SelectItem> bolumList = aramaSecenekleri.getGorevYeriList();
					if (bolumList != null && bolumIdOnceki != null) {
						for (SelectItem selectItem : bolumList) {
							if (selectItem.getValue().equals(bolumIdOnceki))
								aramaSecenekleri.setEkSaha3Id(bolumIdOnceki);
						}
					}

				}
			}
		}

		return "";
	}

	/**
	 * 
	 */
	private void setSeciliDenklestirmeAy() {
		aylikPuantajListClear();

		denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);

		if (denklestirmeAy == null) {
			if (ay > 0) {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.MONTH, ay - 1);
				cal.set(Calendar.YEAR, yil);
				cal.set(Calendar.DAY_OF_MONTH, 1);
				String ayAdi = PdksUtil.convertToDateString(cal.getTime(), "MMMM");
				if (fazlaMesaiTalepDurum == false)
					PdksUtil.addMessageAvailableError(yil + " " + ayAdi + " döneme ait çalışma planı tanımlanmamıştır!");
				else
					PdksUtil.addMessageAvailableError(yil + " " + ayAdi + " döneme ait fazla mesai talep tanımlanmamıştır!");
			}
		} else
			setAylikPuantajDonem(denklestirmeAy);
		setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));

	}

	/**
	 * @param tesisDoldur
	 */
	public void fillSirketDoldur(boolean tesisDoldur) {
		if (aylikPuantajList != null) {
			aylikPuantajList.clear();
			aylikVardiyaOzetList.clear();
		}
		if (tesisDoldur) {
			if (denklestirmeAy == null || denklestirmeAy.getAy() != ay || denklestirmeAy.getYil() != yil) {
				if (denklestirmeAy == null || denklestirmeAy.getYil() != yil) {
					aylariDoldur();
					if (aylar != null && aylar.size() == 1) {
						ay = (Integer) aylar.get(0).getValue();
					}
				}

			}

		}
		aramaSecenekleri.setGorevYeriList(null);
		aramaSecenekleri.setTesisList(null);
		listeleriTemizle();
		setSeciliDenklestirmeAy();
		List<SelectItem> sirketler = null;
		Sirket sirket = null;
		if (denklestirmeAy != null) {
			if (aramaSecenekleri.getDepartmanId() != null) {

				departman = (Departman) pdksEntityController.getSQLParamByFieldObject(Departman.TABLE_NAME, Departman.COLUMN_NAME_ID, aramaSecenekleri.getDepartmanId(), Departman.class, session);

				sirketler = fazlaMesaiOrtakIslemler.getFazlaMesaiMudurSirketList(adminRole ? aramaSecenekleri.getDepartmanId() : null, denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), fazlaMesaiTalepDurum, session);
			}
			if (sirketler == null)
				sirketler = ortakIslemler.getSelectItemList("sirket", authenticatedUser);

			if (!sirketler.isEmpty()) {
				Long onceki = aramaSecenekleri.getSirketId();
				if (sirketler.size() == 1) {
					aramaSecenekleri.setSirketId((Long) sirketler.get(0).getValue());
				} else if (onceki != null) {
					for (SelectItem st : sirketler) {
						if (st.getValue().equals(onceki))
							aramaSecenekleri.setSirketId(onceki);
					}
				}
				if (aramaSecenekleri.getSirketId() != null) {
					HashMap map = new HashMap();
					map.put("id", aramaSecenekleri.getSirketId());
					if (session != null)
						map.put(PdksEntityController.MAP_KEY_SESSION, session);
					sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

					if (tesisDoldur)
						fillTesisDoldur(tesisDoldur);
				}

			}
		}
		aramaSecenekleri.setSirket(sirket);
		aramaSecenekleri.setSirketIdList(sirketler);

	}

	/**
	 * @param bolumDoldur
	 */
	public void fillTesisDoldur(boolean bolumDoldur) {
		departman = aramaSecenekleri.getSirket() != null ? aramaSecenekleri.getSirket().getDepartman() : authenticatedUser.getDepartman();
		listeleriTemizle();
		aramaSecenekleri.setGorevYeriList(null);

		Sirket sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);
		aramaSecenekleri.setSirket(sirket);

		List<SelectItem> list = fazlaMesaiOrtakIslemler.getFazlaMesaiMudurTesisList(aramaSecenekleri.getSirket(), denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), fazlaMesaiTalepDurum, session);
		if (list != null && !list.isEmpty()) {
			Long oncekiId = aramaSecenekleri.getTesisId();
			if (list.size() == 1)
				aramaSecenekleri.setTesisId((Long) list.get(0).getValue());
			else if (oncekiId != null) {
				for (SelectItem st : list) {
					if (st.getValue().equals(oncekiId))
						aramaSecenekleri.setTesisId(oncekiId);
				}
			}
		}
		aramaSecenekleri.setTesisList(list);
		if (bolumDoldur) {
			if (aramaSecenekleri.getTesisId() != null)
				fillBolumDoldur();
		}

	}

	/**
	 * 
	 */
	public void fillBolumDoldur() {
		Sirket sirket = aramaSecenekleri.getSirket();
		List<SelectItem> list = null;
		String tesisId = null;
		if (departman.isAdminMi() && sirket.isTesisDurumu())
			tesisId = aramaSecenekleri.getTesisId() != null ? String.valueOf(aramaSecenekleri.getTesisId()) : null;

		list = fazlaMesaiOrtakIslemler.getFazlaMesaiMudurBolumList(sirket, tesisId, denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), fazlaMesaiTalepDurum, session);
		if (list != null && !list.isEmpty()) {
			Long oncekiId = aramaSecenekleri.getEkSaha3Id();
			if (list.size() == 1)
				aramaSecenekleri.setEkSaha3Id((Long) list.get(0).getValue());
			else if (oncekiId != null) {
				for (SelectItem st : list) {
					if (st.getValue().equals(oncekiId))
						aramaSecenekleri.setEkSaha3Id(oncekiId);
				}
			}
		}
		aramaSecenekleri.setGorevYeriList(list);
	}

	/**
	 * @throws Exception
	 */
	public void fillSirketList() throws Exception {
		tumBolumPersonelleri = null;
		fazlaMesaiTalepler.clear();
		ekSaha4Tanim = null;
		if (adminRole)
			fillDepartmanList();
		listeleriTemizle();
		setDepartman(null);
		aramaSecenekleri.setGorevYeriList(null);
		if (aramaSecenekleri.getDepartmanId() != null) {
			List<SelectItem> sirketler = fazlaMesaiOrtakIslemler.getFazlaMesaiSirketList(adminRole ? aramaSecenekleri.getDepartmanId() : null, denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
			Sirket sirket = null;
			fazlaMesaiListeGuncelle(null, "S", sirketler);
			if (!sirketler.isEmpty()) {
				Long onceki = aramaSecenekleri.getSirketId();
				if (sirketler.size() == 1) {
					onceki = aramaSecenekleri.getSirketId();
					aramaSecenekleri.setSirketId((Long) sirketler.get(0).getValue());
				} else if (onceki != null) {
					for (SelectItem st : sirketler) {
						if (st.getValue().equals(onceki))
							aramaSecenekleri.setSirketId(onceki);
					}
				}
				if (aramaSecenekleri.getSirketId() != null) {

					sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

					ekSaha4Tanim = ortakIslemler.getEkSaha4(sirket, null, session);

				}

				aramaSecenekleri.setSirketIdList(sirketler);

			}
			aramaSecenekleri.setSirket(sirket);
			if (sirket != null && sirketler.size() == 1) {
				if (sirket.isTesisDurumu())
					fillTesisDoldur(true);
				else
					bolumDoldur();
			}
		}

		gorevTipiId = null;
	}

	/**
	 * 
	 */
	private void listeleriTemizle() {
		aylikPuantajListClear();
		aylikVardiyaOzetList.clear();
		fazlaMesaiTalepler.clear();
	}

	/**
	 * @return
	 */
	public String gunlerSecildi() {
		for (VardiyaGun pdksVardiyaGun : aylikPuantajDefault.getVardiyalar())
			pdksVardiyaGun.setCheckBoxDurum(gunSec);
		return "";

	}

	/**
	 * @param aylikPuantaj
	 */
	private void planYetkilendir(AylikPuantaj aylikPuantaj) {
		if (aylikPuantaj != null) {
			Personel personel = aylikPuantaj.getPdksPersonel();
			gorevli = helpPersonel(personel);

			if (!aylikPuantaj.isGorevYeriSec() && aramaSecenekleri.getEkSaha3Id() != null && aramaSecenekleri.getEkSaha3Id() > 0) {
				// HashMap map1 = new HashMap();
				// map1.put("durum", Boolean.TRUE);
				// map1.put("bolum.id", aramaSecenekleri.getEkSaha3Id());
				// if (session != null)
				// map1.put(PdksEntityController.MAP_KEY_SESSION, session);
				bolumKatlari = pdksEntityController.getSQLParamByAktifFieldList(BolumKat.TABLE_NAME, BolumKat.COLUMN_NAME_BOLUM, aramaSecenekleri.getEkSaha3Id(), BolumKat.class, session);

				if (bolumKatlari.size() > 1)
					bolumKatlari = PdksUtil.sortObjectStringAlanList(bolumKatlari, "getAciklama", null);
				else
					bolumKatlari.clear();

			} else
				bolumKatlari = new ArrayList<BolumKat>();
			AylikPuantaj aylikPuantajToplam = new AylikPuantaj();
			try {
				puantajYetkilendir(null, aylikPuantaj, aylikPuantajToplam, null);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

	}

	/**
	 * @param aylikPuantaj
	 * @return
	 */
	private void vardiyalarSec(AylikPuantaj aylikPuantaj) {
		if (aylikPuantaj != null) {
			ArrayList<Vardiya> vardiyalar = fillAylikVardiyaList(aylikPuantaj, null);
			TreeMap<Long, Vardiya> vardiyaMap = new TreeMap<Long, Vardiya>(), vardiyaGebeOzelMap = new TreeMap<Long, Vardiya>();
			for (Iterator iterator = vardiyalar.iterator(); iterator.hasNext();) {
				Vardiya vardiya = (Vardiya) iterator.next();
				if (ikRole == false && (vardiya.isFMI() || vardiya.isIcapVardiyasi())) {
					iterator.remove();
					continue;
				}
				if (personelGebeDurum != null) {
					if (!vardiya.isGebelikMi())
						vardiyaGebeOzelMap.put(vardiya.getId(), vardiya);
				}
				vardiyaMap.put(vardiya.getId(), vardiya);
			}
			for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
				if (pdksVardiyaGun.getVardiya() != null) {
					List<Vardiya> list = pdksVardiyaGun.getIzin() == null || (pdksVardiyaGun.getVardiya().isFMI() && fazlaMesaiIzinRaporuDurum) ? vardiyalar : null;
					TreeMap<Long, Vardiya> vardiyaDataMap = vardiyaMap;
					if (personelGebeDurum != null) {
						if (pdksVardiyaGun.getVardiyaDate().getTime() < personelGebeDurum.getBasTarih().getTime() || pdksVardiyaGun.getVardiyaDate().getTime() > personelGebeDurum.getBitTarih().getTime()) {
							vardiyaDataMap = vardiyaGebeOzelMap;
							list = new ArrayList<Vardiya>(vardiyaGebeOzelMap.values());
						}

					}
					setVardiyaGunleri(list, pdksVardiyaGun, vardiyaDataMap);
					pdksVardiyaGun.setIslemVardiya(null);
					pdksVardiyaGun.setIslendi(false);
				}
			}
			vardiyaMap = null;
			vardiyaGebeOzelMap = null;
		}
	}

	/**
	 * @param aylikPuantaj
	 * @return
	 */
	public String aylikGorevYeriDegistir(AylikPuantaj aylikPuantaj) {

		vardiyalarMap.clear();
		if (aylikPuantaj != null) {
			String mesaj = "";
			boolean gorevYeriSec = Boolean.FALSE;
			HashMap<String, VardiyaGun> map = new HashMap<String, VardiyaGun>();
			for (VardiyaGun pdksVardiyaGun : aylikPuantajDefault.getAyinVardiyalari()) {
				if (pdksVardiyaGun.isCheckBoxDurum()) {
					String gun = pdksVardiyaGun.getVardiyaDateStr();
					map.put(gun, pdksVardiyaGun);
				}
			}
			if (!map.isEmpty()) {
				setVardiyaList(fillAllVardiyaList());
				setPersonelAylikPuantaj(aylikPuantaj);
				planYetkilendir(aylikPuantaj);

				for (VardiyaGun perVardiyaGun : aylikPuantaj.getVardiyalar()) {
					String gun = perVardiyaGun.getVardiyaDateStr();
					perVardiyaGun.setGuncellendi(Boolean.FALSE);
					perVardiyaGun.saklaVardiya();
					if (perVardiyaGun.isAyinGunu() && perVardiyaGun.getVardiya() != null && perVardiyaGun.getIzin() == null) {
						if (perVardiyaGun.isKullaniciYetkili()) {
							perVardiyaGun.setKullaniciYetkili(map.containsKey(gun));
							if (perVardiyaGun.isKullaniciYetkili())
								gorevYeriSec = Boolean.TRUE;
						}

					} else
						perVardiyaGun.setKullaniciYetkili(false);
				}

				if (gorevYeriSec) {
					Tanim parent = ortakIslemler.getSQLTanimByTipKodu(Tanim.TIPI_PERSONEL_EK_SAHA, "ekSaha3", session);
					if (parent != null)
						gorevYeriTanimList = pdksEntityController.getSQLParamByFieldList(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_PARENT_ID, parent.getId(), Tanim.class, session);
					else
						gorevYeriTanimList = new ArrayList<Tanim>();
					if (aramaSecenekleri.getGorevYeriList() != null && !gorevli) {
						List<Long> idler = new ArrayList<Long>();
						if ((authenticatedUser.isYonetici() || authenticatedUser.isYoneticiKontratli()) && !authenticatedUser.isDirektorSuperVisor()) {
							for (Iterator iterator2 = aramaSecenekleri.getGorevYeriList().iterator(); iterator2.hasNext();) {
								SelectItem item = (SelectItem) iterator2.next();
								idler.add((Long) item.getValue());
							}
						}
						if (aramaSecenekleri.getEkSaha3Id() != null && idler.isEmpty())
							idler.add(aramaSecenekleri.getEkSaha3Id());
						if (idler.isEmpty())
							gorevYeriTanimList.clear();
						else {
							Long gorevYeriTanimId = authenticatedUser.isIK() ? aramaSecenekleri.getEkSaha3Id() : null;
							for (Iterator iterator = gorevYeriTanimList.iterator(); iterator.hasNext();) {
								Tanim tanim = (Tanim) iterator.next();
								if (!tanim.getDurum() || (gorevYeriTanimId == null && idler.contains(tanim.getId())) || (gorevYeriTanimId != null && gorevYeriTanimId.equals(tanim.getId())))
									iterator.remove();

							}
						}

						idler = null;
					}
					if (gorevYeriTanimList.size() > 1)
						gorevYeriTanimList = PdksUtil.sortObjectStringAlanList(gorevYeriTanimList, "getAciklama", null);
					aylikPuantaj.setGorevYeriSec(Boolean.TRUE);
					aylikPuantaj.setKaydet(Boolean.TRUE);
					personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
					personelDenklestirme.setGuncellendi(Boolean.FALSE);
				} else
					mesaj = "Görev yeri değişecek çalışılan gün seçiniz!";
				map = null;
			} else
				mesaj = "Görev yeri değişecek gün seçiniz!";
			map = null;
			if (PdksUtil.hasStringValue(mesaj))
				PdksUtil.addMessageWarn(mesaj);
			if (aylikPuantaj != null)
				aylikPuantaj.setGorevYeriSec(gorevYeriSec);
			vardiyalarSec(aylikPuantaj);
		}
		setPersonelAylikPuantaj(aylikPuantaj);
		return "";
	}

	/**
	 * @return
	 */
	public String onayDurumDegisti() {

		for (Iterator iter = aylikPuantajList.iterator(); iter.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
			if (calismaPlanKilit == null)
				aylikPuantaj.setOnayDurum(onayDurum);
			else
				aylikPuantaj.setOnayDurum(true);

		}
		return "";

	}

	/**
	 * @param kilit
	 * @return
	 */
	public String planKilitTalepKontrol(CalismaPlanKilit kilit) {
		CalismaPlanKilitTalep talep = kilit.getTalep();
		if (talep.getId() != null)
			PdksUtil.addMessageWarn(talep.getAciklama() + " açıklaması nedeniyle talep açılmıştır, onay beklemektedir.");
		return "";
	}

	/**
	 * @param kilit
	 * @return
	 */
	@Transactional
	public String planKilitTalep(CalismaPlanKilit kilit) {
		CalismaPlanKilitTalep talep = kilit.getTalep();
		if (talep.getAciklama() != null && talep.getAciklama().length() > 2) {
			// todo
			talep.setOlusturanUser(authenticatedUser);
			talep.setOlusturmaTarihi(new Date());
			saveOrUpdate(talep);
			List<User> ikList = new ArrayList<User>();
			ortakIslemler.IKKullanicilariBul(ikList, null, session);
			if (ccList == null)
				ccList = new ArrayList<User>();
			else
				ccList.clear();
			if (toList == null)
				toList = new ArrayList<User>();
			Map<String, String> map = null;
			try {
				map = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();

			} catch (Exception e) {
			}
			setDonusAdres(map.containsKey("host") ? map.get("host") : "");
			for (User ikUser : ikList) {
				toList.clear();
				toList.add(ikUser);
				MailObject mail = new MailObject();
				String key = "userId=" + ikUser.getId() + "&talepId=" + talep.getId();
				String mailKonu = authenticatedUser.getAdSoyad() + " çalışma plan güncelleme talep ";
				StringBuffer sb = new StringBuffer();
				sb.append(denklestirmeAy.getAyAdi() + " " + yil + " " + PdksUtil.getSelectItemLabel(aramaSecenekleri.getEkSaha3Id(), aramaSecenekleri.getGorevYeriList()) + " " + bolumAciklama + "  çalışma planı güncelleme talebi cevabını verir misiniz. ");
				sb.append("<p><TABLE style=\"width: 270px;\"><TR>");
				sb.append("<td width=\"90px\"><a style=\"font-size: 16px;\" href=\"http://" + donusAdres + "/planGuncellemeTalepLinkOnay?id=" + ortakIslemler.getEncodeStringByBase64(key + "&durum=1") + "\"><b>Onay</b></a></td>");
				sb.append("<td width=\"90px\"><a style=\"font-size: 16px;\" href=\"http://" + donusAdres + "/planGuncellemeTalepLinkOnay?id=" + ortakIslemler.getEncodeStringByBase64(key + "&durum=0") + "\"><b>Red</b></a></td>");
				sb.append("</TR></TABLE></p>");
				mailIcerik = sb.toString();
				ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
				mail.setSubject(mailKonu);
				mail.setBody(mailIcerik);
				if (!mail.getToList().isEmpty() || !mail.getCcList().isEmpty())
					try {
						ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/fazlaMesaiTalepCevapMail.xhtml", session);
					} catch (Exception e) {

					}
			}
			sessionFlush();

		}
		return "";
	}

	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	@Transactional
	public void sayfaCalismaPlanKilitTalepAction() throws Exception {
		if (session == null)
			session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		session.setFlushMode(FlushMode.MANUAL);
		HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String id = (String) req.getParameter("id");
		if (id != null) {
			String decodeStr = OrtakIslemler.getDecodeStringByBase64(id);
			StringTokenizer st = new StringTokenizer(decodeStr, "&");
			HashMap<String, String> param = new HashMap<String, String>();
			String userIdStr = null, talepIdStr = null, durumStr = null;
			while (st.hasMoreTokens()) {
				String tk = st.nextToken();
				String[] parStrings = tk.split("=");
				param.put(parStrings[0], parStrings[1]);
			}
			if (param.size() == 3) {
				if (param.containsKey("userId"))
					userIdStr = param.get("userId");
				else
					PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme onaylayan bilgisi bulunamadı");
				if (param.containsKey("talepId"))
					talepIdStr = param.get("talepId");
				else
					PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme talep bilgisi bulunamadı");
				if (param.containsKey("durum"))
					durumStr = param.get("durum");
				else
					PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme durum bilgisi  bulunamadı");

			}
			if (userIdStr != null && talepIdStr != null && durumStr != null) {
				HashMap fields = new HashMap();
				fields.put("id", Long.parseLong(talepIdStr));
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				CalismaPlanKilitTalep cpkt = (CalismaPlanKilitTalep) pdksEntityController.getSQLParamByFieldObject(CalismaPlanKilitTalep.TABLE_NAME, CalismaPlanKilitTalep.COLUMN_NAME_ID, Long.parseLong(talepIdStr), CalismaPlanKilitTalep.class, session);

				if (cpkt != null) {
					denklestirmeAy = cpkt.getCalismaPlanKilit().getDenklestirmeAy();
					fillEkSahaTanim();
					setCalismaPlanKilit(cpkt.getCalismaPlanKilit());
					fields.clear();

					User guncelleyenUser = (User) pdksEntityController.getSQLParamByFieldObject(User.TABLE_NAME, User.COLUMN_NAME_ID, Long.parseLong(userIdStr), User.class, session);

					if (cpkt.getOnayDurum() == null) {
						boolean flush = true;
						cpkt.setOnayDurum(durumStr.equals("1"));
						cpkt.setGuncellemeTarihi(new Date());
						cpkt.setGuncelleyenUser(guncelleyenUser);
						saveOrUpdate(cpkt);
						if (cpkt.getOnayDurum()) {
							CalismaPlanKilit kilit = cpkt.getCalismaPlanKilit();
							if (kilit.getKilitDurum()) {
								kilit.setKilitDurum(Boolean.FALSE);
								kilit.setGuncellemeTarihi(new Date());
								kilit.setGuncelleyenUser(authenticatedUser);
								saveOrUpdate(kilit);
								sessionFlush();
								flush = false;
								try {
									calismaPlanSorumluMailGonder(kilit);
								} catch (Exception e) {
									logger.equals(e);
									e.printStackTrace();
								}
							}

						}
						PdksUtil.addMessageAvailableInfo(cpkt.getOnayDurum() ? "Onaylandı" : "Onaylanmadı");
						if (flush)
							sessionFlush();
					} else {
						if (cpkt.getGuncelleyenUser() == null || guncelleyenUser == null || !guncelleyenUser.getId().equals(cpkt.getGuncelleyenUser().getId()))
							PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme talep bilgisini onaylanmıştır!");
						else
							PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme talep bilgisini onayladınız.");
					}

				} else
					PdksUtil.addMessageAvailableWarn("Çalışma plan güncelleme talep bilgisi bulunamadı");

			}
		}
	}

	/**
	 * @param kilit
	 * @return
	 */
	@Transactional
	public String planKilitAc(CalismaPlanKilit kilit) {
		if (kilit != null && kilitliPlanList != null) {
			for (Iterator iterator = kilitliPlanList.iterator(); iterator.hasNext();) {
				CalismaPlanKilit cpk = (CalismaPlanKilit) iterator.next();
				if (cpk.getId() != null && cpk.getId().equals(kilit.getId())) {
					kilit.setKilitDurum(Boolean.FALSE);
					kilit.setGuncellemeTarihi(new Date());
					kilit.setGuncelleyenUser(authenticatedUser);
					saveOrUpdate(kilit);
					sessionFlush();
					try {
						calismaPlanSorumluMailGonder(kilit);
					} catch (Exception e) {
						logger.equals(e);
						e.printStackTrace();
					}

					iterator.remove();
					break;
				}
			}
		}

		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String planOnayla() {
		boolean devam = Boolean.FALSE;
		HashMap<Long, User> yoneticiMap = new HashMap<Long, User>();
		HashMap<Long, Boolean> yoneticiOnayMap = new HashMap<Long, Boolean>();
		HashMap<Long, List<AylikPuantaj>> yoneticiPuantajMap = new HashMap<Long, List<AylikPuantaj>>();
		List<AylikPuantaj> puantajList = new ArrayList<AylikPuantaj>();
		List<Long> perIdList = new ArrayList<Long>();
		String aciklama = null, veriAyrac = "_";
		boolean mesajYaz = true;
		String genelMudurERPKoduStr = ortakIslemler.getParameterKey("genelMudurERPKodu");
		boolean tumOnaylanPlanEkle = ortakIslemler.getParameterKey("tumOnaylanPlanEkle").equals("1");
		boolean yoneticiTanimsiz = ortakIslemler.getParameterKey("yoneticiTanimsiz").equals("1");
		List<String> genelMudurERPKodlari = !PdksUtil.hasStringValue(genelMudurERPKoduStr) ? new ArrayList<String>() : PdksUtil.getListByString(genelMudurERPKoduStr, null);
		List<Long> eskiOnayList = new ArrayList<Long>();
		TreeMap<Long, TreeMap<Long, Vardiya>> calismaModeliMap = getCalismaModeliMap(aylikPuantajList, null);
		boolean onayliVar = false;
		boolean usetYonetici = authenticatedUser.getPdksPersonel().getUstYonetici();
		for (Iterator iter = aylikPuantajList.iterator(); iter.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iter.next();
			aylikPuantaj.setSecili(false);
			long yoneticiPersonelId = 0;
			TreeMap<Long, Vardiya> vardiyaMap = null;
			if (aylikPuantaj.getCalismaModeli() != null) {
				Long id = aylikPuantaj.getCalismaModeli().getId();
				if (calismaModeliMap.containsKey(id))
					vardiyaMap = calismaModeliMap.get(id);
			}
			Personel personel = aylikPuantaj.getPdksPersonel();
			if (denklestirmeAy.isDurumu() && !(authenticatedUser.isAdmin() || authenticatedUser.isSistemYoneticisi())) {
				if (personel.isSanalPersonelMi() == false && (yoneticiTanimsiz == false && (aylikPuantaj.getYonetici() == null || aylikPuantaj.getYonetici().getId() == null))) {
					aylikPuantaj.setOnayDurum(usetYonetici || false);
				}
			}
			boolean onayDurum = aylikPuantaj.isOnayDurum();
			VardiyaPlan pdksVardiyaPlan = aylikPuantaj.getVardiyaPlan();
			boolean asilOnay = false;
			PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
			if (onayDurum) {
				mesajYaz = false;
				onayDurum = vardiyaPlanKontrol(personelDenklestirme, null, vardiyaMap, pdksVardiyaPlan, personel.getSicilNo() + " " + personel.getAdSoyad() + " ", false);
				if (!onayliVar)
					onayliVar = onayDurum;
				asilOnay = true;
			} else if (tumOnaylanPlanEkle) {
				onayDurum = personelDenklestirme != null && personelDenklestirme.isOnaylandi();
				eskiOnayList.add(personel.getId());
			}

			if (onayDurum) {
				User userYonetici = null;
				if (personel.getPdksYonetici() != null) {
					yoneticiPersonelId = personel.getPdksYonetici().getId();
					userYonetici = yoneticiMap.containsKey(yoneticiPersonelId) ? yoneticiMap.get(yoneticiPersonelId) : null;
					if (userYonetici == null) {

						userYonetici = (User) pdksEntityController.getSQLParamByFieldObject(User.TABLE_NAME, User.COLUMN_NAME_PERSONEL, yoneticiPersonelId, User.class, session);

					}
					if (userYonetici != null) {
						if (asilOnay || !yoneticiOnayMap.containsKey(yoneticiPersonelId))
							yoneticiOnayMap.put(yoneticiPersonelId, asilOnay);
						List<AylikPuantaj> list = yoneticiPuantajMap.containsKey(yoneticiPersonelId) ? (List<AylikPuantaj>) yoneticiPuantajMap.get(yoneticiPersonelId) : new ArrayList<AylikPuantaj>();
						if (list.isEmpty()) {
							yoneticiMap.put(yoneticiPersonelId, userYonetici);
							yoneticiPuantajMap.put(yoneticiPersonelId, list);
						}
						list.add(aylikPuantaj);
					}
				}
				if (yoneticiPersonelId == 0) {
					List<AylikPuantaj> list = yoneticiPuantajMap.containsKey(yoneticiPersonelId) ? (List<AylikPuantaj>) yoneticiPuantajMap.get(yoneticiPersonelId) : new ArrayList<AylikPuantaj>();
					if (list.isEmpty())
						yoneticiPuantajMap.put(yoneticiPersonelId, list);
					list.add(aylikPuantaj);
				}

				if (!devam)
					devam = Boolean.TRUE;
				puantajList.add(aylikPuantaj);
				if (aylikPuantaj.isOnayDurum())
					perIdList.add(personel.getId());

			}

		}
		if (!devam) {
			if (mesajYaz)
				PdksUtil.addMessageAvailableWarn("Onaylanacak plan bilgisi bulunamadı!");
		} else {

			Boolean onayDurum = Boolean.FALSE;
			String calismaPlanOnayMailAdres = ortakIslemler.getParameterKey("calismaPlanOnayMail");
			Map<String, String> mapReq = null;
			try {

				mapReq = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap();
				String webAdres = mapReq.containsKey("host") ? mapReq.get("host") : "";
				if (webAdres.equals("localhost:8080"))
					calismaPlanOnayMailAdres = "";
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());

			}
			if (departman != null && !departman.isAdminMi()) {
				for (AylikPuantaj aylikPuantaj : puantajList) {
					if (aylikPuantaj.isOnayDurum()) {
						Boolean durum = saveOnay(session, aylikPuantaj);
						aylikPuantaj.setOnayDurum(false);
						if (durum)
							onayDurum = Boolean.TRUE;
					}
				}
			} else {
				HashMap fields = new HashMap();

				TreeMap<Long, User> perUserMap = pdksEntityController.getSQLParamByFieldMap(User.TABLE_NAME, User.COLUMN_NAME_PERSONEL, perIdList, User.class, "getPersonelId", false, session);

				Tanim bolum = null;
				if (aramaSecenekleri.getEkSaha3Id() != null) {

					bolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, aramaSecenekleri.getEkSaha3Id(), Tanim.class, session);

				}
				if (toList == null)
					toList = new ArrayList<User>();
				else
					toList.clear();
				if (ccList == null)
					ccList = new ArrayList<User>();
				else
					ccList.clear();
				if (bccList == null)
					bccList = new ArrayList<User>();
				else
					bccList.clear();

				LinkedHashMap<String, Object> veriMap = ortakIslemler.getListPersonelOzetVeriMap(puantajList, aramaSecenekleri.getTesisId(), veriAyrac);
				if (veriMap.containsKey("bolum"))
					bolum = (Tanim) veriMap.get("bolum");
				aciklama = veriMap.containsKey("aciklama") ? (String) veriMap.get("aciklama") : null;
				if (aciklama == null && bolum != null)
					aciklama = bolum.getAciklama();
				dosyaAdi = "AylıkÇalışmaPlanı_" + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + (bolum != null ? "_" + bolum.getAciklama() : "") + ".xlsx";
				mailKonu = PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM") + " ayı" + (aciklama != null ? " " + PdksUtil.replaceAll(aciklama, veriAyrac, " ") : "") + " çalışma planı";
				for (AylikPuantaj aylikPuantaj : puantajList) {
					Personel personel = aylikPuantaj.getPdksPersonel();
					toList.clear();
					if (eskiOnayList.contains(personel.getId()) || perUserMap.size() < 2 || !perUserMap.containsKey(personel.getId())) {
						Boolean durum = saveOnay(session, aylikPuantaj);
						aylikPuantaj.setSecili(!durum);
						if (durum)
							onayDurum = Boolean.TRUE;
						continue;
					}
					mailIcerik = "<P>Sayın " + personel.getAdSoyad() + ",</P><br/> ";
					mailIcerik += "<P>" + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM  ") + " ayı" + (aciklama != null ? " " + PdksUtil.replaceAll(aciklama, veriAyrac, " ") : "") + " çalışma planınız ekteki dosyadır.</p><br/><br/>";
					toList.add(perUserMap.get(personel.getId()));
					aylikPuantaj.setSecili(Boolean.TRUE);
					String gorevYeriAciklama = getExcelAciklama();
					baosDosya = aylikVardiyaExcelDevam(gorevYeriAciklama, puantajList, Boolean.FALSE);
					mailData = baosDosya.toByteArray();

					try {
						if (PdksUtil.hasStringValue(calismaPlanOnayMailAdres) && mailData != null) {
							MailObject mail = new MailObject();
							mail.setSubject(mailKonu);
							mail.setBody(mailIcerik);
							ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
							ortakIslemler.addMailPersonelUserList(ccList, mail.getCcList());
							ortakIslemler.addMailPersonelUserList(bccList, mail.getBccList());
							if (!PdksUtil.hasStringValue(dosyaAdi))
								dosyaAdi = "AylıkÇalışmaPlanı_" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";

							MailFile mf = new MailFile();
							mf.setDisplayName(dosyaAdi);
							mf.setIcerik(mailData);
							mail.getAttachmentFiles().add(mf);
							ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/" + calismaPlanOnayMailAdres, session);
						}

						Boolean durum = saveOnay(session, aylikPuantaj);
						if (durum)
							onayDurum = Boolean.TRUE;
						aylikPuantaj.setSecili(!durum);
					} catch (Exception e) {
						logger.error("Pdks hata in : \n");
						e.printStackTrace();
						logger.error("Pdks hata out : " + e.getMessage());
						logger.error("hata /email/" + calismaPlanOnayMailAdres + " " + e.getMessage());
						PdksUtil.addMessageError("Mesaj gönderilmemiştir. " + e.getMessage());

					}
					toList.clear();

				}
				if (planDepartman != null) {
					HashMap map = new HashMap();

					StringBuffer sb = new StringBuffer();
					fields.clear();
					sb.append("select TOP 1 T.* from " + Tanim.TABLE_NAME + " T " + PdksEntityController.getSelectLOCK() + " ");
					sb.append(" inner join " + Tanim.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Tanim.COLUMN_NAME_ID + " = T." + Tanim.COLUMN_NAME_PARENT_ID);
					sb.append(" and P." + Tanim.COLUMN_NAME_TIPI + " = :pt and P." + Tanim.COLUMN_NAME_KODU + " = :pk ");
					sb.append(" where T." + Tanim.COLUMN_NAME_KODU + " = :k ");
					fields.put("pt", Tanim.TIPI_PERSONEL_EK_SAHA);
					fields.put("pk", "ekSaha3");
					fields.put("k", Personel.BOLUM_SUPERVISOR);
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<Tanim> list = pdksEntityController.getObjectBySQLList(sb, fields, Tanim.class);
					Tanim superVisor = list != null && !list.isEmpty() ? list.get(0) : null;

					if (superVisor != null) {
						map.clear();
						map.put("pdksPersonel.ekSaha1.id=", planDepartman.getId());
						map.put("pdksPersonel.iseBaslamaTarihi<=", aylikPuantajDefault.getSonGun());
						map.put("pdksPersonel.sskCikisTarihi>=", aylikPuantajDefault.getIlkGun());
						map.put("durum=", Boolean.TRUE);
						map.put("pdksPersonel.durum=", Boolean.TRUE);
						map.put("pdksPersonel.ekSaha3.id=", superVisor.getId());
						if (session != null)
							map.put(PdksEntityController.MAP_KEY_SESSION, session);
						List<User> superVisorUserList = null;
						try {
							superVisorUserList = pdksEntityController.getObjectByInnerObjectListInLogic(map, User.class);
						} catch (Exception e) {
							logger.error("Pdks hata in : \n");
							e.printStackTrace();
							logger.error("Pdks hata out : " + e.getMessage());
							logger.debug(e.getMessage());
							superVisorUserList = new ArrayList<User>();
						}
						if (!superVisorUserList.isEmpty())
							ccList.addAll(superVisorUserList);
					}
					HashMap parametreMap = new HashMap();

					parametreMap.put("departman.id", planDepartman.getId());
					if (session != null)
						parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
					List<DepartmanMailGrubu> departmanMailGrubuList = pdksEntityController.getObjectByInnerObjectList(parametreMap, DepartmanMailGrubu.class);
					for (DepartmanMailGrubu departmanMailGrubu : departmanMailGrubuList) {
						User user = new User();
						user.setEmail(departmanMailGrubu.getMailAdress());
						ccList.add(user);
					}
				}
			}
			String servisMailAdresleri = ortakIslemler.getParameterKey("servisMailAdresleri");
			if (PdksUtil.hasStringValue(servisMailAdresleri)) {
				List<String> mailList = PdksUtil.getListFromString(servisMailAdresleri, null);
				for (String mail : mailList) {
					User user = new User();
					user.setEmail(mail);
					bccList.add(user);
				}

			}

			for (Iterator iterator = yoneticiPuantajMap.keySet().iterator(); iterator.hasNext();) {
				Long yoneticiPersonelId = (Long) iterator.next();
				if (!yoneticiOnayMap.containsKey(yoneticiPersonelId) || !yoneticiOnayMap.get(yoneticiPersonelId))
					continue;
				mailIcerik = "";
				if (toList == null)
					toList = new ArrayList<User>();
				else
					toList.clear();
				if (yoneticiMap.containsKey(yoneticiPersonelId)) {
					User yonetici = yoneticiMap.get(yoneticiPersonelId);
					Personel personel = yonetici.getPdksPersonel();
					if (personel.getGorevTipi() == null || !genelMudurERPKodlari.contains(personel.getGorevTipi().getErpKodu())) {
						mailIcerik += "<P>Sayın " + personel.getAdSoyad() + ",</P><br/> ";
						toList.add(yonetici);
					} else
						continue;
				}
				if ((toList != null && !toList.isEmpty()) || (ccList != null && !ccList.isEmpty()) || (bccList != null && !bccList.isEmpty())) {
					mailIcerik += "<P>" + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM") + " ayı" + (aciklama != null ? " " + PdksUtil.replaceAll(aciklama, veriAyrac, " ") : "") + " çalışma planı ekteki dosyadır.</p><br/><br/>";
					puantajList = yoneticiPuantajMap.get(yoneticiPersonelId);
					for (AylikPuantaj aylikPuantaj : puantajList) {
						aylikPuantaj.setOnayDurum(!eskiOnayList.contains(aylikPuantaj.getPdksPersonel().getId()));
						aylikPuantaj.setSecili(Boolean.TRUE);
					}

					mailData = null;
					if (PdksUtil.hasStringValue(calismaPlanOnayMailAdres)) {
						String gorevYeriAciklama = getExcelAciklama();
						baosDosya = aylikVardiyaExcelDevam(gorevYeriAciklama, puantajList, Boolean.FALSE);
						mailData = baosDosya.toByteArray();
					}
					try {
						if (mailData != null) {
							MailObject mail = new MailObject();
							mail.setSubject(mailKonu);
							mail.setBody(mailIcerik);
							ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
							ortakIslemler.addMailPersonelUserList(ccList, mail.getCcList());
							ortakIslemler.addMailPersonelUserList(bccList, mail.getBccList());
							MailFile mf = new MailFile();
							mf.setDisplayName(dosyaAdi);
							mf.setIcerik(mailData);
							mail.getAttachmentFiles().add(mf);
							ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/" + calismaPlanOnayMailAdres, session);
						}

					} catch (Exception e) {
						logger.error("Pdks hata in : \n");
						e.printStackTrace();
						logger.error("Pdks hata out : " + e.getMessage());
						logger.error("hata /email/" + calismaPlanOnayMailAdres + " " + e.getMessage());
						PdksUtil.addMessageError("Mesaj gönderilmemiştir. " + e.getMessage());

					}
					for (AylikPuantaj aylikPuantaj : puantajList)
						aylikPuantaj.setSecili(Boolean.FALSE);
				}
			}

			for (AylikPuantaj aylikPuantaj : puantajList) {
				aylikPuantaj.setOnayDurum(false);
			}
			try {
				sessionFlush();
			} catch (Exception es) {
				ortakIslemler.setExceptionLog(null, es);
			}

			if (onayDurum) {
				if (calismaPlanKilit != null) {
					boolean kilitle = true;
					for (AylikPuantaj ap : aylikPuantajList) {
						if (ap.getPersonelDenklestirme().isOnaylandi() == false) {
							kilitle = false;
							break;
						}
					}
					if (kilitle)
						calismaPlanKilit.setKilitDurum(Boolean.TRUE);
					if (calismaPlanKilit.getGuncellemeTarihi() == null)
						calismaPlanKilit.setTalepKontrol(ortakIslemler.getParameterKey("calismaPlanKilitTalep").equals("1"));
					calismaPlanKilit.setGuncellemeTarihi(new Date());
					calismaPlanKilit.setGuncelleyenUser(authenticatedUser);
					saveOrUpdate(calismaPlanKilit);
					sessionFlush();

				}
				aylikPuantajOlusturuluyor();
				puantajList = aylikPuantajList;
				suaKontrol(puantajList);
				if (onayliVar)
					PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyy MMMMMM  ") + " ayı çalışma onaylandı.");
			}

		}
		return "";

	}

	/**
	 * @param tempList
	 * @param calismaModeliVardiyaOzelMap
	 * @return
	 */
	private TreeMap<Long, TreeMap<Long, Vardiya>> getCalismaModeliMap(List<AylikPuantaj> tempList, HashMap<Long, HashMap<String, Boolean>> calismaModeliVardiyaOzelMap) {
		TreeMap<Long, TreeMap<Long, Vardiya>> calismaModeliMap = new TreeMap<Long, TreeMap<Long, Vardiya>>();
		if (calismaModeliVardiyaOzelMap == null)
			calismaModeliVardiyaOzelMap = new HashMap<Long, HashMap<String, Boolean>>();
		if (ortakIslemler.getParameterKey("vardiyaCalismaModeliKontrol").equals("1")) {
			List<Long> cmIdList = new ArrayList<Long>();
			for (Iterator iterator = tempList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				if (aylikPuantaj.getCalismaModeli() != null) {
					Long id = aylikPuantaj.getCalismaModeli().getId();
					if (id != null && !cmIdList.contains(id))
						cmIdList.add(id);
				}

			}
			if (!cmIdList.isEmpty()) {
				HashMap parametreMap = new HashMap();
				parametreMap.put("calismaModeli.id", cmIdList);
				if (session != null)
					parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
				List<CalismaModeliVardiya> list = pdksEntityController.getSQLParamByFieldList(CalismaModeliVardiya.TABLE_NAME, CalismaModeliVardiya.COLUMN_NAME_CALISMA_MODELI, cmIdList, CalismaModeliVardiya.class, session);

				for (CalismaModeliVardiya calismaModeliVardiya : list) {
					Long cmId = calismaModeliVardiya.getCalismaModeli().getId();
					Vardiya vardiya = calismaModeliVardiya.getVardiya();
					if (vardiya.getDurum()) {
						TreeMap<Long, Vardiya> map1 = calismaModeliMap.containsKey(cmId) ? calismaModeliMap.get(cmId) : new TreeMap<Long, Vardiya>();
						if (map1.isEmpty()) {

							calismaModeliMap.put(cmId, map1);
						}
						if (vardiya.getGenel().equals(Boolean.FALSE)) {
							HashMap<String, Boolean> map = calismaModeliVardiyaOzelMap.containsKey(cmId) ? calismaModeliVardiyaOzelMap.get(cmId) : new HashMap<String, Boolean>();
							if (map.isEmpty()) {
								map.put(Vardiya.GEBE_KEY, Boolean.FALSE);
								map.put(Vardiya.SUA_KEY, Boolean.FALSE);
								map.put(Vardiya.ICAP_KEY, Boolean.FALSE);
								calismaModeliVardiyaOzelMap.put(cmId, map);
							}
							if (vardiya.isGebelikMi())
								map.put(Vardiya.GEBE_KEY, Boolean.TRUE);
							if (vardiya.isSuaMi())
								map.put(Vardiya.SUA_KEY, Boolean.TRUE);
							if (vardiya.isIcapVardiyasi())
								map.put(Vardiya.ICAP_KEY, Boolean.TRUE);
						}

						map1.put(vardiya.getId(), vardiya);
					}
				}

			}
		}
		return calismaModeliMap;
	}

	/**
	 * 
	 */
	@Transactional
	protected void talepGirisCikisHareketEkle() {
		if (islemFazlaMesaiTalep != null && islemFazlaMesaiTalep.getId() != null) {
			Vardiya islemVardiya = seciliVardiyaGun != null ? seciliVardiyaGun.getIslemVardiya() : null;
			PersonelKGS personelKGS = null;
			try {
				Personel personel = islemFazlaMesaiTalep.getVardiyaGun().getPersonel(), girisYapan = islemFazlaMesaiTalep.getOlusturanUser() == null ? authenticatedUser.getPdksPersonel() : islemFazlaMesaiTalep.getOlusturanUser().getPdksPersonel();
				personelKGS = personel.getPersonelKGS();
				if (personel.getId().equals(girisYapan.getId())) {
					if (manuelGiris == null) {
						fillKapiPDKSList();
						if (manuelGiris == null || manuelCikis == null) {
							if (manuelGiris == null)
								manuelGiris = new KapiView();
							if (manuelCikis == null)
								manuelCikis = new KapiView();
						}
					}
					if (manuelGiris.getId() != null && manuelGiris.getId() != null) {
						Long personelKGSId = personelKGS.getId();
						Calendar cal = Calendar.getInstance();
						Long nedenId = islemFazlaMesaiTalep.getMesaiNeden() != null ? islemFazlaMesaiTalep.getMesaiNeden().getId() : null;
						Tanim fazlaMesaiSistemOnayDurum = ortakIslemler.getOtomatikKapGirisiNeden(session);
						if (fazlaMesaiSistemOnayDurum != null)
							nedenId = fazlaMesaiSistemOnayDurum.getId();
						Date tarih1 = islemFazlaMesaiTalep.getBaslangicZamani(), tarih2 = islemFazlaMesaiTalep.getBitisZamani();
						String referans = "TRef:" + islemFazlaMesaiTalep.getId();
						HashMap fields = new HashMap();
						fields.put("islem.islemTipi<>", "D");
						fields.put("personel.id=", personelKGSId);
						fields.put("zaman>=", ortakIslemler.tariheGunEkleCikar(cal, tarih1, -1));
						fields.put("zaman<=", ortakIslemler.tariheGunEkleCikar(cal, tarih2, 1));
						fields.put("islem.aciklama like ", "%" + referans + "%");
						if (session != null)
							fields.put(PdksEntityController.MAP_KEY_SESSION, session);
						List<PersonelHareket> hareketList = pdksEntityController.getObjectByInnerObjectListInLogic(fields, PersonelHareket.class);
						PersonelHareket girisHareket = null, cikisHareket = null;
						if (islemFazlaMesaiTalep.getOnayDurumu() != FazlaMesaiTalep.ONAY_DURUM_RED && islemFazlaMesaiTalep.getDurum()) {
							boolean flush = false;
							for (PersonelHareket personelHareket : hareketList) {
								if (personelHareket.getKapiView().getKapi().isGirisKapi())
									girisHareket = personelHareket;
								else if (personelHareket.getKapiView().getKapi().isCikisKapi())
									cikisHareket = personelHareket;

							}
							Date basTarih = null, bitTarih = null;
							if (seciliVardiyaGun != null && seciliVardiyaGun.getIslemVardiya() != null) {
								basTarih = islemVardiya.getVardiyaFazlaMesaiBasZaman();
								bitTarih = islemVardiya.getVardiyaFazlaMesaiBitZaman();
							}
							if (hareketPdksList == null) {
								if (basTarih == null)
									basTarih = PdksUtil.addTarih(tarih1, Calendar.MINUTE, -15);
								if (bitTarih == null)
									bitTarih = PdksUtil.addTarih(tarih2, Calendar.MINUTE, 15);

								hareketPdksList = getPdksHareketler(personelKGSId, basTarih, bitTarih);
							}
							int doluGirisAdet = 0, doluCikisAdet = 0;
							Integer ardisikAdet = null;
							if (!hareketPdksList.isEmpty()) {
								List<HareketKGS> hareketKGSList = new ArrayList<HareketKGS>(hareketPdksList);
								List<HareketKGS> hareketKGSDeleteList = new ArrayList<HareketKGS>();
								if (islemVardiya != null) {
									seciliVardiyaGun.setHareketler(null);
									seciliVardiyaGun.setCikisHareketleri(null);
									seciliVardiyaGun.setGecersizHareketler(null);
									seciliVardiyaGun.setGirisHareketleri(null);
									HareketKGS girisYeniHareket = new HareketKGS();
									girisYeniHareket.setKapiView(manuelGiris);
									girisYeniHareket.setOrjinalZaman(tarih1);
									girisYeniHareket.setZaman(tarih1);
									hareketKGSList.add(girisYeniHareket);
									HareketKGS cikisYeniHareket = new HareketKGS();
									cikisYeniHareket.setKapiView(manuelCikis);
									cikisYeniHareket.setOrjinalZaman(tarih2);
									cikisYeniHareket.setZaman(tarih2);
									hareketKGSList.add(cikisYeniHareket);
									hareketKGSList = PdksUtil.sortListByAlanAdi(hareketKGSList, "zaman", Boolean.FALSE);
									HareketKGS hareketOncekiKGS = null, hareketSonrakiKGS = null;
									Date bas1 = islemVardiya.getVardiyaTelorans1BasZaman(), bas2 = islemVardiya.getVardiyaTelorans2BasZaman();
									Date bit1 = islemVardiya.getVardiyaTelorans1BitZaman(), bit2 = islemVardiya.getVardiyaTelorans2BitZaman();
									int kayitAdet = hareketKGSList.size();
									for (int i = 0; i < kayitAdet; i++) {
										HareketKGS hareketKGS = hareketKGSList.get(i);
										Date zaman = hareketKGS.getOrjinalZaman(), zamanOnceki = hareketOncekiKGS != null ? hareketOncekiKGS.getOrjinalZaman() : null;
										Kapi kapi = hareketKGS.getKapiView().getKapi();
										if (hareketKGS.getId() == null) {
											hareketSonrakiKGS = null;
											if (kapi.isGirisKapi()) {
												doluGirisAdet = 0;
												ardisikAdet = 0;
												for (int j = i + 1; j < kayitAdet; j++) {
													HareketKGS hareketSonraki = hareketKGSList.get(j);
													Kapi kapiSonraki = hareketSonraki.getKapiView().getKapi();
													boolean sil = false;
													if (hareketSonraki.getId() != null) {
														++doluGirisAdet;
														Date zamanSonraki = hareketSonraki.getOrjinalZaman();
														if (bas1.getTime() <= zaman.getTime() && bas2.getTime() >= zamanSonraki.getTime()) {
															girisHareket = new PersonelHareket();
															hareketKGSDeleteList.add(hareketSonraki);
														} else if (bit1.getTime() <= zaman.getTime() && bit2.getTime() >= zamanSonraki.getTime()) {
															sil = kapiSonraki.isCikisKapi();
															if (sil) {
																girisHareket = new PersonelHareket();
															}
															hareketKGSDeleteList.add(hareketSonraki);

														}

													} else {
														// if (i == 0)
														// i = kayitAdet;
														break;
													}

												}
												if (hareketKGSDeleteList.isEmpty() && hareketOncekiKGS != null && doluGirisAdet < 2) {
													HareketKGS hareketOnceki = hareketOncekiKGS;
													if (bas2.getTime() >= zaman.getTime() && bas1.getTime() >= zamanOnceki.getTime()) {
														girisHareket = new PersonelHareket();
														// hareketKGS = hareketOnceki;
													} else if (bit2.getTime() >= zaman.getTime() && bit1.getTime() <= zamanOnceki.getTime()) {
														girisHareket = new PersonelHareket();
														hareketKGSDeleteList.add(hareketOnceki);
													} else if (bas2.getTime() >= zamanOnceki.getTime() && bas1.getTime() <= zamanOnceki.getTime()) {
														if (bit2.getTime() >= zaman.getTime() && bit1.getTime() <= zaman.getTime())
															girisHareket = new PersonelHareket();
													}
													if (girisHareket != null)
														break;
												}

											} else if (kapi.isCikisKapi()) {
												doluCikisAdet = 0;
												for (int j = i + 1; j < kayitAdet; j++) {
													HareketKGS hareketSonraki = hareketKGSList.get(j);
													if (hareketSonraki.getId() != null) {
														++doluCikisAdet;
														Kapi kapiSonraki = hareketSonraki.getKapiView().getKapi();
														hareketSonrakiKGS = hareketSonraki;
														Date zamanSonraki = hareketSonraki.getOrjinalZaman();
														if (bas1.getTime() <= zaman.getTime() && bas2.getTime() >= zamanSonraki.getTime()) {
															if (kapiSonraki.isGirisKapi() && doluCikisAdet == 1) {
																cikisHareket = new PersonelHareket();
																hareketKGSDeleteList.add(hareketSonraki);
															}

														} else if (bit1.getTime() <= zaman.getTime() && bit2.getTime() >= zamanSonraki.getTime()) {
															cikisHareket = new PersonelHareket();
															hareketKGSDeleteList.add(hareketSonraki);
														}
													}
												}
												if (doluCikisAdet > 0) {
													if (hareketOncekiKGS != null) {
														if (bas2.getTime() >= zaman.getTime() && bas1.getTime() <= zamanOnceki.getTime()) {
															cikisHareket = new PersonelHareket();
															hareketKGSDeleteList.add(hareketOncekiKGS);
														} else if (bit2.getTime() >= zaman.getTime() && bit1.getTime() <= zamanOnceki.getTime()) {
															cikisHareket = new PersonelHareket();
															hareketKGSDeleteList.add(hareketOncekiKGS);
														} else if (bas2.getTime() >= zamanOnceki.getTime() && bas1.getTime() <= zamanOnceki.getTime()) {
															if (bit2.getTime() >= zaman.getTime() && bit1.getTime() <= zaman.getTime())
																cikisHareket = new PersonelHareket();
														}
													} else if (hareketSonrakiKGS != null && bas2.getTime() >= zaman.getTime() && bas1.getTime() <= zaman.getTime()) {
														Date zamanSonraki = hareketSonrakiKGS.getOrjinalZaman();
														if (bit2.getTime() >= zamanSonraki.getTime() && bit1.getTime() <= zamanSonraki.getTime())
															cikisHareket = new PersonelHareket();
													}
												}
												break;
											}
										} else {
											if (ardisikAdet != null)
												++ardisikAdet;
											hareketOncekiKGS = hareketKGS;
										}

									}
								}
								if (!hareketKGSDeleteList.isEmpty() && (girisHareket == null || cikisHareket == null)) {
									for (HareketKGS hareketKGS3 : hareketKGSDeleteList) {

										long kgsId = 0, pdksId = 0;
										String str = hareketKGS3.getId();
										Long id = Long.parseLong(str.substring(1));
										if (str.startsWith(HareketKGS.GIRIS_ISLEM_YAPAN_SIRKET_KGS))
											kgsId = id;
										else
											pdksId = id;
										if (kgsId + pdksId > 0) {
											User sistemUser = ortakIslemler.getSistemAdminUser(session);
											if (sistemUser == null)
												sistemUser = authenticatedUser;
											String aciklama = PdksUtil.hasStringValue(islemFazlaMesaiTalep.getAciklama()) ? islemFazlaMesaiTalep.getAciklama().trim() : "";
											pdksEntityController.hareketSil(kgsId, pdksId, sistemUser, nedenId, aciklama + (referans != null ? " " + referans.trim() : ""), hareketKGS3.getKgsSirketId(), session);
											flush = true;
										}
									}

								}
							}

							String aciklama = referans + " " + (PdksUtil.hasStringValue(islemFazlaMesaiTalep.getAciklama()) ? islemFazlaMesaiTalep.getAciklama().trim() : "") + " hareket";
							User guncelleyen = ortakIslemler.getSistemAdminUser(session);
							if (!authenticatedUser.isAdmin() && userHome.hasPermission("personelHareket", "view"))
								guncelleyen = authenticatedUser;
							if (girisHareket == null && doluGirisAdet < 2) {
								if (nedenId != null) {
									hareketEkleReturn(guncelleyen, manuelGiris, personelKGS, tarih1, nedenId, aciklama + " giriş");
									flush = true;
								}

							}
							if (cikisHareket == null && (ardisikAdet == null || ardisikAdet < 2)) {
								if (nedenId != null) {
									hareketEkleReturn(guncelleyen, manuelCikis, personelKGS, tarih2, nedenId, aciklama + " çıkış");
									flush = true;
								}

							}
							if (flush)
								sessionFlush();
						} else if (!hareketList.isEmpty()) {
							Long sonuc = fazlaMesaiOrtakIslemler.fazlaMesaiOtomatikHareketSil(islemFazlaMesaiTalep.getId(), session);
							if (sonuc != null && sonuc.equals(islemFazlaMesaiTalep.getId()))
								sessionFlush();
						}

					}
				}
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
			hareketPdksList = null;
			if (aylikPuantajMesaiTalepList == null && islemVardiya != null && personelKGS != null)
				hareketPdksList = getPdksHareketler(personelKGS.getId(), islemVardiya.getVardiyaFazlaMesaiBasZaman(), islemVardiya.getVardiyaFazlaMesaiBitZaman());

		}
	}

	/**
	 * @param personelKGSId
	 * @param tarih1
	 * @param tarih2
	 * @return
	 */
	private List<HareketKGS> getPdksHareketler(Long personelKGSId, Date tarih1, Date tarih2) {
		if (sessionHareket == null)
			sessionHareket = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		else
			sessionHareket.clear();
		List<Long> kgsPerIdler = new ArrayList<Long>();
		kgsPerIdler.add(personelKGSId);
		HashMap<Long, ArrayList<HareketKGS>> personelHareketMap = ortakIslemler.fillPersonelKGSHareketMap(kgsPerIdler, tarih1, tarih2, sessionHareket);
		List<HareketKGS> hareketPdksList = personelHareketMap.containsKey(personelKGSId) ? personelHareketMap.get(personelKGSId) : new ArrayList<HareketKGS>();
		for (Iterator iterator = hareketPdksList.iterator(); iterator.hasNext();) {
			HareketKGS hareketKGS = (HareketKGS) iterator.next();
			try {
				KapiKGS kapiKGS = hareketKGS.getKapiKGS();
				if (kapiKGS.getKapi() == null || !kapiKGS.getKapi().getPdks())
					iterator.remove();
			} catch (Exception e) {
			}
		}
		personelHareketMap = null;
		kgsPerIdler = null;
		return hareketPdksList;
	}

	/**
	 * 
	 */
	private void fillKapiPDKSList() {
		List<KapiKGS> kapiKGSList = ortakIslemler.fillKapiKGSList(session);
		List<KapiView> list = new ArrayList<KapiView>();
		for (KapiKGS kapiKGS : kapiKGSList)
			list.add(kapiKGS.getKapiView());
		HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(list, session);
		manuelGiris = manuelKapiMap.get(Kapi.TIPI_KODU_GIRIS);
		manuelCikis = manuelKapiMap.get(Kapi.TIPI_KODU_CIKIS);
		manuelKapiMap = null;
	}

	/**
	 * @param guncelleyen
	 * @param kapi
	 * @param personelKGS
	 * @param zaman
	 * @param nedenId
	 * @param aciklama
	 * @return
	 */
	private Long hareketEkleReturn(User guncelleyen, KapiView kapi, PersonelKGS personelKGS, Date zaman, Long nedenId, String aciklama) {
		Long id = null;
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		try {
			StringBuffer sp = new StringBuffer("SP_HAREKET_EKLE_RETURN");
			veriMap.put("kapi", kapi.getId());
			veriMap.put("personelKGS", personelKGS.getId());
			veriMap.put("zaman", zaman);
			veriMap.put("guncelleyen", authenticatedUser.getId());
			veriMap.put("nedenId", nedenId);
			veriMap.put("aciklama", aciklama);
			if (session != null)
				veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List list = pdksEntityController.execSPList(veriMap, sp, null);
			if (list != null && !list.isEmpty())
				id = ((BigDecimal) list.get(0)).longValue();
			list = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		veriMap = null;
		return id;

	}

	/**
	 * @param session
	 * @param aylikPuantaj
	 * @return
	 */

	private Boolean saveOnay(Session session, AylikPuantaj aylikPuantaj) {
		Personel personel = aylikPuantaj.getPdksPersonel();
		PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
		if (personelDenklestirme.isOnaylandi() == false) {
			personelDenklestirme.setOnaylandi(Boolean.TRUE);
			if (personelDenklestirme.getSutIzniDurum() == null || personelDenklestirme.getSutIzniDurum().equals(Boolean.FALSE))
				personelDenklestirme.setSutIzniDurum(personel.getSutIzni() != null && personel.getSutIzni());
			if (!personelDenklestirme.isKapandi())
				personelDenklestirme.setDevredenSure(null);
			personelDenklestirme.setGuncellemeTarihi(new Date());
			saveOrUpdate(personelDenklestirme);
		}
		aylikPuantaj.setOnayDurum(Boolean.FALSE);
		Boolean durum = Boolean.TRUE;
		return durum;
	}

	/**
	 * @return
	 */
	public String mesaiTopluKaydet() {
		List<String> mesajlar = new ArrayList<String>();
		HashMap<Long, AylikPuantaj> puantajMap = new HashMap<Long, AylikPuantaj>();
		TreeMap<Long, User> userMap = new TreeMap<Long, User>();
		Tanim mesaiNeden = ortakIslemler.getTanimById(mesaiNedenId, session);
		fazlaMesaiTalep.setMesaiNeden(mesaiNeden);
		if (fazlaMesaiTalep.getMesaiNeden() == null) {
			mesajlar.add("Mesai nedeni seçiniz!");

		} else {
			Calendar cal = Calendar.getInstance();
			cal.setTime(fazlaMesaiTalep.getBaslangicZamani());
			cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBasSaat());
			cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBasDakika());
			Date baslangicZamani = cal.getTime();
			cal.setTime(fazlaMesaiTalep.getBitisZamani());
			cal.set(Calendar.HOUR_OF_DAY, fazlaMesaiTalep.getBitSaat());
			cal.set(Calendar.MINUTE, fazlaMesaiTalep.getBitDakika());
			Date bitisZamani = cal.getTime();
			fazlaMesaiTalep.setBaslangicZamani(baslangicZamani);
			fazlaMesaiTalep.setBitisZamani(bitisZamani);
			HashMap<Long, VardiyaGun> devamMap = new HashMap<Long, VardiyaGun>();
			List<Long> perIdList = new ArrayList<Long>();
			for (AylikPuantaj aylikPuantaj : aylikPuantajMesaiTalepList) {
				VardiyaGun vardiyaGunPer = aylikPuantaj.getVardiyaGun();
				if (aylikPuantaj.getYonetici2() != null && !perIdList.contains(aylikPuantaj.getYonetici2().getId()))
					perIdList.add(aylikPuantaj.getYonetici2().getId());
				puantajMap.put(vardiyaGunPer.getId(), aylikPuantaj);
			}

			if (!perIdList.isEmpty()) {
				String fieldName = "v";
				HashMap map = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select U.* from " + Personel.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + User.TABLE_NAME + " U " + PdksEntityController.getJoinLOCK() + " on U." + User.COLUMN_NAME_PERSONEL + " = P." + Personel.COLUMN_NAME_ID + " and U." + User.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" where P." + Personel.COLUMN_NAME_ID + " :" + fieldName);
				map.put(fieldName, perIdList);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// List<User> userList = pdksEntityController.getObjectBySQLList(sb, map, User.class);
				List<User> userList = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, map, User.class, session);
				for (User user : userList) {
					userMap.put(user.getPersonelId(), user);
				}
			}
			for (Iterator iterator = aylikPuantajMesaiTalepList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				VardiyaGun vardiyaGunPer = aylikPuantaj.getVardiyaGun();
				String anaMesaj = aylikPuantaj.getPdksPersonel().getPdksSicilNo() + " " + aylikPuantaj.getPdksPersonel().getAdSoyad();
				Vardiya islemVardiya = vardiyaGunPer.getIslemVardiya();
				VardiyaGun sonrakiVardiyaGun = vardiyaGunPer.getSonrakiVardiyaGun(), oncekiVardiyaGun = vardiyaGunPer.getOncekiVardiyaGun();
				Vardiya sonrakiVardiya = null, oncekiVardiya = null;
				if (oncekiVardiyaGun != null) {
					oncekiVardiyaGun.setIslemVardiya(null);
					oncekiVardiyaGun.setVardiyaZamani();
					oncekiVardiya = oncekiVardiyaGun.getIslemVardiya();
				}
				if (sonrakiVardiyaGun != null) {
					sonrakiVardiyaGun.setIslemVardiya(null);
					sonrakiVardiyaGun.setVardiyaZamani();
					sonrakiVardiya = sonrakiVardiyaGun.getIslemVardiya();
				}
				boolean devam = true;
				if (islemVardiya.isCalisma()) {
					boolean disVardiSinirda = (oncekiVardiya == null || oncekiVardiya.getVardiyaBitZaman().getTime() < fazlaMesaiTalep.getBaslangicZamani().getTime()) && (sonrakiVardiya == null || sonrakiVardiya.getVardiyaBasZaman().getTime() > fazlaMesaiTalep.getBitisZamani().getTime());
					boolean icVardiSinirda = fazlaMesaiTalep.getBitisZamani().getTime() <= islemVardiya.getVardiyaBasZaman().getTime() || fazlaMesaiTalep.getBaslangicZamani().getTime() >= islemVardiya.getVardiyaBitZaman().getTime();
					if (!disVardiSinirda) {
						mesajlar.add(anaMesaj + " mesai başlangıç ve bitiş zamanı hatalıdır");
						devam = false;
					} else if (!icVardiSinirda) {
						mesajlar.add(anaMesaj + " mesai başlangıç ve bitiş zamanı hatalıdır");
						devam = false;
					}
				} else if (PdksUtil.tarihKarsilastirNumeric(fazlaMesaiTalep.getBaslangicZamani(), seciliVardiyaGun.getVardiyaDate()) != 0 || PdksUtil.tarihKarsilastirNumeric(fazlaMesaiTalep.getBitisZamani(), seciliVardiyaGun.getVardiyaDate()) != 0) {
					mesajlar.add(anaMesaj + " mesai başlangıç ve bitiş zamanı hatalıdır");
					devam = false;
				}
				if (devam)
					devamMap.put(vardiyaGunPer.getId(), vardiyaGunPer);
				else
					puantajMap.remove(vardiyaGunPer.getId());
			}
			if (!devamMap.isEmpty()) {
				List idList = new ArrayList(devamMap.keySet());
				String fieldName = "id";
				HashMap map = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select F.*," + FazlaMesaiTalep.COLUMN_NAME_ONAY_DURUMU + " from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " F " + PdksEntityController.getJoinLOCK() + " on F." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V." + VardiyaGun.COLUMN_NAME_ID + " and F." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" and F." + FazlaMesaiTalep.COLUMN_NAME_BASLANGIC_ZAMANI + " <= :t2 and F." + FazlaMesaiTalep.COLUMN_NAME_BITIS_ZAMANI + " >= :t1 ");
				sb.append(" where F." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " :" + fieldName);
				map.put(fieldName, idList);
				map.put("t1", fazlaMesaiTalep.getBaslangicZamani());
				map.put("t2", fazlaMesaiTalep.getBitisZamani());
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// List<FazlaMesaiTalep> mesaiList = pdksEntityController.getObjectBySQLList(sb, map, FazlaMesaiTalep.class);
				List<FazlaMesaiTalep> mesaiList = pdksEntityController.getSQLParamList(idList, sb, fieldName, map, FazlaMesaiTalep.class, session);
				String patternTarih = PdksUtil.getDateFormat(), saatPattern = PdksUtil.getSaatFormat();
				for (FazlaMesaiTalep fazlaMesaiTalep : mesaiList) {
					Personel personel = fazlaMesaiTalep.getVardiyaGun().getPersonel();
					boolean esit = PdksUtil.tarihKarsilastirNumeric(fazlaMesaiTalep.getBaslangicZamani(), fazlaMesaiTalep.getBitisZamani()) == 0;
					String pattern = (esit ? "" : patternTarih + " ") + saatPattern;
					String anaMesaj = personel.getPdksSicilNo() + " " + personel.getAdSoyad() + " için oluşturulan " + PdksUtil.convertToDateString(fazlaMesaiTalep.getBaslangicZamani(), patternTarih + " " + saatPattern) + " - "
							+ PdksUtil.convertToDateString(fazlaMesaiTalep.getBitisZamani(), pattern);
					switch (fazlaMesaiTalep.getOnayDurumu()) {
					case FazlaMesaiTalep.ONAY_DURUM_ISLEM_YAPILMADI:
						PdksUtil.addMessageAvailableInfo(anaMesaj + " ait fazla mesai talep onayda beklemektedir.");
						break;
					case FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI:
						mesajlar.add(anaMesaj + " ait fazla mesai talep onaylandı!");
						break;
					case FazlaMesaiTalep.ONAY_DURUM_RED:
						mesajlar.add(anaMesaj + " ait fazla mesai talep onaylanmadı!");
						break;
					default:
						break;
					}
					puantajMap.remove(fazlaMesaiTalep.getVardiyaGun().getId());
					devamMap.remove(fazlaMesaiTalep.getVardiyaGun().getId());
				}
			}
		}
		if (!puantajMap.isEmpty()) {
			fazlaMesaiTalep.setOlusturanUser(authenticatedUser);
			fazlaMesaiTalep.setOlusturmaTarihi(new Date());
			FazlaMesaiTalep asil = (FazlaMesaiTalep) fazlaMesaiTalep.clone();
			VardiyaGun vardiyaGunAsil = (VardiyaGun) seciliVardiyaGun.clone();
			for (Iterator iterator = aylikPuantajMesaiTalepList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				VardiyaGun vardiyaGunPer = aylikPuantaj.getVardiyaGun();
				if (puantajMap.containsKey(vardiyaGunPer.getId())) {
					fazlaMesaiTalep = (FazlaMesaiTalep) asil.clone();
					fazlaMesaiTalep.setGuncelleyenUser(userMap.get(aylikPuantaj.getYonetici2().getId()));
					fazlaMesaiTalep.setVardiyaGun(vardiyaGunPer);
					seciliVardiyaGun = vardiyaGunPer;
					mesaiKaydet(Boolean.TRUE);
					if (fazlaMesaiTalep == null)
						iterator.remove();
				}
			}
			fazlaMesaiTalep = null;
			seciliVardiyaGun = (VardiyaGun) vardiyaGunAsil.clone();
			if (!aylikPuantajMesaiTalepList.isEmpty())
				fazlaMesaiTalep = asil;
		}

		for (String mesaj : mesajlar)
			PdksUtil.addMessageAvailableWarn(mesaj);

		return "";
	}

	/**
	 * @param vgSec
	 * @return
	 */
	public String topluTalepOlusturmaKontrol(VardiyaGun vgSec) {
		seciliVardiyaGun = vgSec;
		if (aylikPuantajMesaiTalepList == null)
			aylikPuantajMesaiTalepList = new ArrayList<AylikPuantaj>();
		else
			aylikPuantajMesaiTalepList.clear();
		seciliVardiyaGun.setIslemVardiya(null);
		manuelHareketEkle = null;
		mesaiNedenId = null;
		hareketPdksList = null;
		boolean secili = false;
		List<String> mesajlar = new ArrayList<String>();
		int index = -1;
		List<Long> perIdList = new ArrayList<Long>();
		TreeMap<String, VardiyaGun> vm = new TreeMap<String, VardiyaGun>();
		List<Long> vardiyaIdList = new ArrayList<Long>();
		Personel girisYapan = authenticatedUser.getPdksPersonel(), personel = null;
		manuelHareketEkle = null;
		hareketPdksList = null;
		TreeMap<Long, Liste> seciliMap = new TreeMap<Long, Liste>();
		try {
			for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				aylikPuantaj.setVardiyaGun(null);
				if (aylikPuantaj.isOnayDurum()) {
					if (index < 0) {
						for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
							++index;
							if (vg.getVardiyaDate().getTime() == seciliVardiyaGun.getVardiyaDate().getTime()) {
								break;
							}
						}
					}
					VardiyaGun vardiyaGunPer = aylikPuantaj.getVardiyalar().get(index);
					Vardiya vardiya = vardiyaGunPer.getVardiya();
					Long id = vardiya != null ? vardiya.getId() : null;
					if (id != null) {
						Liste liste = seciliMap.containsKey(id) ? seciliMap.get(id) : new Liste(id, new ArrayList<AylikPuantaj>());
						List<AylikPuantaj> list = (List<AylikPuantaj>) liste.getValue();
						if (list.isEmpty())
							seciliMap.put(id, liste);
						list.add(aylikPuantaj);
						liste.setNumValue(list.size());
					} else
						aylikPuantaj.setOnayDurum(false);
				}
			}
			if (seciliMap.size() > 1) {
				List<Liste> list = PdksUtil.sortListByAlanAdi(new ArrayList<Liste>(seciliMap.values()), "numValue", true);
				Liste listeAna = list.get(0);
				List<AylikPuantaj> puantajList = (List<AylikPuantaj>) listeAna.getValue();
				VardiyaGun vardiyaGunPer = puantajList.get(0).getVardiyalar().get(index);
				Vardiya vardiya = vardiyaGunPer.getVardiya();
				PdksUtil.addMessageAvailableInfo("Birden fazla seçemezsiniz, " + vardiya.getKisaAdi() + (vardiya.isCalisma() ? " " + vardiyaGunPer.getVardiya().getAdi() : "") + " haricindeki vardiyalar işleme alınmadı!");
				for (int i = 1; i < list.size(); i++) {
					Liste liste = list.get(i);
					puantajList = (List<AylikPuantaj>) liste.getValue();
					for (AylikPuantaj aylikPuantaj : puantajList) {
						aylikPuantaj.setOnayDurum(false);
					}
					puantajList = null;
					liste = null;

				}
				list = null;
			}
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		seciliMap = null;
		for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
			aylikPuantaj.setVardiyaGun(null);
			if (aylikPuantaj.isOnayDurum()) {
				personel = aylikPuantaj.getPdksPersonel();
				secili = true;
				if (aylikPuantaj.getYonetici2() != null && aylikPuantaj.getPersonelDenklestirme().isOnaylandi()) {
					if (!perIdList.contains(aylikPuantaj.getYonetici2().getId()))
						perIdList.add(aylikPuantaj.getYonetici2().getId());
					if (index < 0) {
						for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
							++index;
							if (vg.getVardiyaDate().getTime() == seciliVardiyaGun.getVardiyaDate().getTime()) {
								break;
							}
						}
					}
					VardiyaGun vardiyaGunPer = aylikPuantaj.getVardiyalar().get(index);
					if (personel.getId().equals(girisYapan.getId())) {
						manuelHareketEkle = Boolean.FALSE;
						if (vardiyaGunPer != null) {
							Vardiya islemVardiya = vardiyaGunPer.getIslemVardiya();
							if (islemVardiya != null) {
								seciliVardiyaGun.setIslemVardiya(islemVardiya);
								Date basTarih = islemVardiya.getVardiyaFazlaMesaiBasZaman();
								Date bitTarih = islemVardiya.getVardiyaFazlaMesaiBitZaman();
								hareketPdksList = getPdksHareketler(girisYapan.getPersonelKGS().getId(), basTarih, bitTarih);
								manuelHareketEkle = hareketPdksList.isEmpty();
							}
						}
					}
					if (vardiyaGunPer.getId() != null && vardiyaGunPer.getVardiya() != null && vardiyaGunPer.getVardiya().getId() != null) {
						if (vardiyaGunPer.getIzin() == null) {
							aylikPuantaj.setVardiyaGun(vardiyaGunPer);
							aylikPuantajMesaiTalepList.add(aylikPuantaj);
							personel = aylikPuantaj.getPdksPersonel();
							for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
								setVardiyalarToMap(vg, vm, aylikPuantaj);
							}
							if (!vardiyaIdList.contains(vardiyaGunPer.getVardiya().getId())) {
								seciliVardiyaGun = (VardiyaGun) vardiyaGunPer.clone();
								seciliVardiyaGun.setId(null);
								seciliVardiyaGun.setPersonel(null);
								vardiyaIdList.add(vardiyaGunPer.getVardiya().getId());
							}
						} else {
							aylikPuantaj.setOnayDurum(Boolean.FALSE);
							PdksUtil.addMessageAvailableWarn(aylikPuantaj.getPdksPersonel().getPdksSicilNo() + " " + aylikPuantaj.getPdksPersonel().getAdSoyad() + " izinlidir!");
						}

					} else {
						aylikPuantaj.setOnayDurum(Boolean.FALSE);
						PdksUtil.addMessageAvailableWarn(aylikPuantaj.getPdksPersonel().getPdksSicilNo() + " " + aylikPuantaj.getPdksPersonel().getAdSoyad() + " çalışmamaktadır!");
					}
				} else {
					String anaMesaj = aylikPuantaj.getPdksPersonel().getPdksSicilNo() + " " + aylikPuantaj.getPdksPersonel().getAdSoyad(), mesaj = null;
					if (aylikPuantaj.getYonetici2() == null)
						mesaj = anaMesaj + " 2. yönetici tanımsız";
					if (aylikPuantaj.getPersonelDenklestirme().isOnaylandi() == false) {
						if (mesaj == null)
							mesaj = anaMesaj;
						else
							mesaj += " ve ";
						mesaj += " çalışma planı onaylanmamış!";
					} else
						mesaj += "!";
					mesajlar.add(mesaj);
				}
			}

		}
		if (!secili)
			mesajlar.add("Seçili kayıt yok!");
		else if (vardiyaIdList.size() > 1)
			mesajlar.add("Seçili günde tek tip vardiya olmalıdır!");
		else if (!perIdList.isEmpty()) {
			if (!vm.isEmpty())
				ortakIslemler.fazlaMesaiSaatiAyarla(vm);
			HashMap map = new HashMap();
			String fieldName = "v";
			StringBuffer sb = new StringBuffer();
			sb.append("select P.* from " + Personel.TABLE_NAME + " P " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" left join " + User.TABLE_NAME + " U " + PdksEntityController.getJoinLOCK() + " on U." + User.COLUMN_NAME_PERSONEL + " = P." + Personel.COLUMN_NAME_ID + " and U." + User.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" where P." + Personel.COLUMN_NAME_ID + " :" + fieldName + " and U." + User.COLUMN_NAME_ID + " is null ");
			map.put(fieldName, perIdList);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			// List<Personel> perList = pdksEntityController.getObjectBySQLList(sb, map, Personel.class);
			List<Personel> perList = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, map, Personel.class, session);
			for (Personel yonetici : perList) {
				mesajlar.add(yonetici.getPdksSicilNo() + " " + yonetici.getAdSoyad() + " aktif kullanıcısı bulunmamaktadır!");
			}
		}
		if (!mesajlar.isEmpty()) {
			aylikPuantajMesaiTalepList.clear();
			for (String mesaj : mesajlar) {
				PdksUtil.addMessageAvailableWarn(mesaj);
			}
		} else {
			fazlaMesaiTalep = new FazlaMesaiTalep(seciliVardiyaGun);
			fazlaMesaiTalep.setBaslangicZamani(seciliVardiyaGun.getVardiyaDate());
			fazlaMesaiTalep.setBitisZamani(seciliVardiyaGun.getVardiyaDate());
			Vardiya vardiya = seciliVardiyaGun.getIslemVardiya();
			List<Long> personelIdler = new ArrayList<Long>();
			if (aylikPuantajMesaiTalepList != null)
				for (AylikPuantaj aylikPuantaj : aylikPuantajMesaiTalepList) {
					personelIdler.add(aylikPuantaj.getPdksPersonel().getId());
				}
			if (!personelIdler.isEmpty()) {
				List<VardiyaGun> vardiyaGunler = ortakIslemler.getAllPersonelIdVardiyalar(personelIdler, seciliVardiyaGun.getVardiyaDate(), seciliVardiyaGun.getVardiyaDate(), false, session);
				for (VardiyaGun vardiyaGun : vardiyaGunler) {
					seciliVardiyaGun.setYarimYuvarla(vardiyaGun.getYarimYuvarla());
					break;
				}
				vardiyaGunler = null;
			}
			personelIdler = null;
			if (seciliVardiyaGun.getVardiya().isCalisma()) {
				Calendar cal = Calendar.getInstance();
				cal.setTime(vardiya.getVardiyaBitZaman());
				fazlaMesaiTalep.setBasSaat(cal.get(Calendar.HOUR_OF_DAY));
				fazlaMesaiTalep.setBasDakika(cal.get(Calendar.MINUTE));
				fazlaMesaiTalep.setBitSaat(cal.get(Calendar.HOUR_OF_DAY));
				fazlaMesaiTalep.setBitDakika(cal.get(Calendar.MINUTE));
			}

			mesaiNedenTanimList = ortakIslemler.getTanimSelectItem("mesaiNeden", ortakIslemler.getTanimList(Tanim.TIPI_FAZLA_MESAI_NEDEN, session));

		}

		return "";
	}

	/**
	 * @param seciliPer
	 * @return
	 */
	public String mesaiTalepListeSil(Personel seciliPer) {
		Long id = seciliPer != null && seciliPer.getId() != null ? seciliPer.getId() : 0L;
		Personel girisYapan = authenticatedUser.getPdksPersonel();
		if (girisYapan.getId().equals(id)) {
			manuelHareketEkle = null;
			hareketPdksList = null;
		}
		for (Iterator iterator = aylikPuantajMesaiTalepList.iterator(); iterator.hasNext();) {
			AylikPuantaj puantaj = (AylikPuantaj) iterator.next();
			if (puantaj.getPdksPersonel().getId().equals(id))
				iterator.remove();
		}
		return "";
	}

	/**
	 * 
	 */
	private void aylikHareketKaydiVardiyalariBul() {
		aylikHareketKaydiVardiyaBul = Boolean.FALSE;
		Date bugun = new Date();
		for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
			CalismaModeliAy calismaModeliAy = aylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy();
			if (calismaModeliAy != null && calismaModeliAy.isHareketKaydiVardiyaBulsunmu()) {
				for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
					if (!vg.getDurum() && vg.isAyinGunu() && vg.getId() != null) {
						if (vg.getIslemVardiya() == null || bugun.before(vg.getIslemVardiya().getVardiyaTelorans2BitZaman()))
							continue;

						if (vg.getVersion() == 0)
							aylikHareketKaydiVardiyaBul = Boolean.TRUE;
					}
				}
			}
			if (aylikHareketKaydiVardiyaBul)
				break;
		}
	}

	/**
	 * @return
	 */
	@Transactional
	public String aylikHareketKaydiVardiyaBulGuncelle() {
		try {
			Date bugun = new Date();
			for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
				CalismaModeliAy calismaModeliAy = aylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy();
				if (calismaModeliAy != null && calismaModeliAy.isHareketKaydiVardiyaBulsunmu()) {
					boolean flush = false;
					for (VardiyaGun vg : aylikPuantaj.getVardiyalar()) {
						if (vg.getIslemVardiya() == null || bugun.before(vg.getIslemVardiya().getVardiyaTelorans2BitZaman()))
							continue;
						if (vg.isAyinGunu() && vg.getId() != null) {
							if (vg.getDurum() && vg.getVersion() < 0) {
								vg.setVersion(0);
								saveOrUpdate(vg);
								flush = true;
							} else if (!vg.getDurum() && vg.getVersion() == 0 && vg.isIzinli() == false && vg.getVardiya().isHaftaTatil() == false) {
								vg.setVersion(-1);
								saveOrUpdate(vg);
								flush = true;
							}
						}
					}
					if (flush)
						sessionFlush();
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		aylikHareketKaydiVardiyalariBul();
		return "";
	}

	/**
	 * @param personel
	 * @return
	 */
	@Transactional
	public String fillBolumPersonelDenklestirmeList(Personel secPersonel) {
		if (secPersonel != null && secPersonel.getEkSaha3() != null) {
			sicilNo = secPersonel.getPdksSicilNo();
			// aramaSecenekleri.setSicilNo(secPersonel.getPdksSicilNo());
			aramaSecenekleri.setEkSaha3Id(secPersonel.getEkSaha3().getId());
			if (ekSaha4Tanim != null && secPersonel.getEkSaha4() != null) {
				aramaSecenekleri.setEkSaha4Id(secPersonel.getEkSaha4().getId());
				altBolumDoldur();
			}
			aylikPuantajOlusturuluyor();
		}
		return "";
	}

	/**
	 * 
	 */
	@Transactional
	public Boolean aylikPuantajOlusturuluyor() {

		if (loginUser == null)
			loginUser = authenticatedUser;
		Personel per = loginUser.getPdksPersonel();
		aylikPuantajListClear();
		// HashMap<Long, List<PersonelDonemselDurum>> pddMap = new HashMap<Long, List<PersonelDonemselDurum>>();
		Boolean mudurAltSeviye = ortakIslemler.getMudurAltSeviyeDurum(per, session);
		if (per != null)
			per.setMudurAltSeviye(mudurAltSeviye);
		componentState.setSeciliTab("tab1");
		fazlaMesaiOde = false;
		fazlaMesaiIzinKullan = false;
		seciliBolum = null;
		seciliAltBolum = null;
		bordroPuantajEkranindaGoster = ortakIslemler.getParameterKey("bordroPuantajEkranindaGoster").equals("1");
		yoneticiERP1Kontrol = !ortakIslemler.getParameterKeyHasStringValue("yoneticiERP1Kontrol") && ortakIslemler.yoneticiRolVar(session);
		bordroAlanKapat();
		Boolean kontrolDurum = false;
		String donem = String.valueOf(yil * 100 + ay);
		vardiyalarMap.clear();
		vardiyaBolumList = null;
		aylikHareketKaydiVardiyaBul = Boolean.FALSE;
		savePlanLastParameter();
		fazlaMesaiIzinRaporuDurum = userHome != null && loginUser.getLogin() && userHome.hasPermission("fazlaMesaiIzinRaporu", "view");
		offIzinGuncelle = ortakIslemler.getParameterKey("offIzinGuncelle").equals("1");
		try {
			User user = ortakIslemler.getSistemAdminUser(session);
			if (user == null)
				user = loginUser;
			boolean testDurum = PdksUtil.getTestDurum() && PdksUtil.getCanliSunucuDurum() == false;
			testDurum = false;
			if (testDurum)
				logger.info("aylikPuantajOlusturuluyor 0000 " + PdksUtil.getCurrentTimeStampStr());
			manuelVardiyaIzinGir = ortakIslemler.getVardiyaIzinGir(session, loginUser.getDepartman());
			gorevYeriGirisDurum = ortakIslemler.getParameterKey("uygulamaTipi").equals("H") && ortakIslemler.getParameterKey("gorevYeriGiris").equals("1");
			departmanBolumAyni = Boolean.FALSE;
			setFazlaMesaiTalepVar(planGirisi && aramaSecenekleri.getSirket() != null && aramaSecenekleri.getSirket().isFazlaMesaiTalepGirer() && aramaSecenekleri.getSirket().getDepartman().isFazlaMesaiTalepGirer());
			modelGoster = Boolean.FALSE;
			sanalPersonelAciklama = ortakIslemler.sanalPersonelAciklama();
			String aksamBordroBasZamani = ortakIslemler.getParameterKey("aksamBordroBasZamani"), aksamBordroBitZamani = ortakIslemler.getParameterKey("aksamBordroBitZamani");
			Integer[] basZaman = ortakIslemler.getSaatDakika(aksamBordroBasZamani), bitZaman = ortakIslemler.getSaatDakika(aksamBordroBitZamani);
			aksamVardiyaBasSaat = basZaman[0];
			aksamVardiyaBasDakika = basZaman[1];
			aksamVardiyaBitSaat = bitZaman[0];
			aksamVardiyaBitDakika = bitZaman[1];
			ozelIstek = Boolean.FALSE;
			onayDurum = Boolean.FALSE;
			aylikPuantajListClear();
			List<VardiyaGun> aylikSablonVardiyalar = new ArrayList<VardiyaGun>();
			gunSec = Boolean.FALSE;
			DepartmanDenklestirmeDonemi denklestirmeDonemiGecenAy = new DepartmanDenklestirmeDonemi();
			HashMap fields = new HashMap();

			if (aramaSecenekleri.getSirketId() != null) {
				fields.clear();

				Sirket sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

				if (sirket != null) {
					departmanBolumAyni = sirket.isTesisDurumu() == false && sirket.getSirketGrup() != null;
				}
				aramaSecenekleri.setSirket(sirket);

			}

			sutIzniGoster = Boolean.FALSE;
			gebeGoster = Boolean.FALSE;
			partTimeGoster = Boolean.FALSE;
			isAramaGoster = Boolean.FALSE;
			fields.clear();

			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			if (denklestirmeAy == null) {
				denklestirmeAy = new DenklestirmeAy();
				denklestirmeAy.setOlusturanUser(user);
				denklestirmeAy.setAy(ay);
				denklestirmeAy.setYil(yil);
				denklestirmeAy.setFazlaMesaiMaxSure(ortakIslemler.getFazlaMesaiMaxSure(null));
				saveOrUpdate(denklestirmeAy);
				sessionFlush();
			} else if (denklestirmeAy.getFazlaMesaiMaxSure() == null)
				fazlaMesaiOrtakIslemler.setFazlaMesaiMaxSure(denklestirmeAy, session);
			setAylikPuantajDonem(denklestirmeAy);
			setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy, loginUser));

			LinkedHashMap<Long, CalismaModeliAy> modelMap = new LinkedHashMap<Long, CalismaModeliAy>();

			fields.clear();
			Calendar cal = Calendar.getInstance();

			cal.set(Calendar.MONTH, ay - 1);
			cal.set(Calendar.YEAR, yil);
			cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DATE));
			Date bitGun = PdksUtil.getDate(cal.getTime());
			cal.set(Calendar.DATE, 1);
			Date basGun = PdksUtil.getDate(cal.getTime());
			boolean devam = true;
			List<VardiyaHafta> vardiyaHaftaList = null;
			tatilGunleriMap = null;
			if (testDurum)
				logger.info("aylikPuantajOlusturuluyor 1000 " + PdksUtil.getCurrentTimeStampStr());
			if (departmanDenklestirmeDonemi == null)
				departmanDenklestirmeDonemi = new DepartmanDenklestirmeDonemi();

			if (sicilNo != null)
				sicilNo = sicilNo.trim();

			AylikPuantaj gecenAylikPuantajSablon = null;

			devam = true;
			User islemYapan = (User) loginUser.clone();

			ArrayList<String> perList = null;
			fields.clear();
			bolumKatlari = null;

			gorevYerileri = new ArrayList<Long>();
			if (gorevliPersonelMap != null)
				gorevliPersonelMap.clear();
			else
				gorevliPersonelMap = new HashMap<String, Personel>();

			ArrayList<Long> perIdler = new ArrayList<Long>();
			List<Personel> personelFMList = new ArrayList<Personel>(fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(aramaSecenekleri.getSirket(), aramaSecenekleri.getTesisId() != null ? String.valueOf(aramaSecenekleri.getTesisId()) : null, aramaSecenekleri.getEkSaha3Id(),
					aramaSecenekleri.getEkSaha4Id(), denklestirmeAy != null ? aylikPuantajDonem : null, true, session));

			List<Personel> personelList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(aramaSecenekleri.getSirket(), aramaSecenekleri.getTesisId() != null ? String.valueOf(aramaSecenekleri.getTesisId()) : null, aramaSecenekleri.getEkSaha3Id(), aramaSecenekleri.getEkSaha4Id(),
					denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
			for (Personel personelFm : personelFMList) {
				boolean ekle = true;
				for (Personel personelPlan : personelList) {
					if (personelPlan.getId().equals(personelFm.getId())) {
						ekle = false;
						break;
					}
				}
				if (ekle)
					personelList.add(personelFm);
			}

			personelFMList = null;
			perList = new ArrayList<String>();
			for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
				Personel personel = (Personel) iterator.next();
				if (PdksUtil.hasStringValue(sicilNo) && !personel.getPdksSicilNo().equals(sicilNo)) {
					iterator.remove();

				} else {
					perIdler.add(personel.getId());
					perList.add(personel.getPdksSicilNo());
				}

			}

			if (testDurum)
				logger.info("aylikPuantajOlusturuluyor 2000 " + PdksUtil.getCurrentTimeStampStr());

			if (adminRole || islemYapan.getYetkiTumPersonelNoList().contains(sicilNo))
				if (PdksUtil.hasStringValue(sicilNo))
					if (fazlaMesaiMap == null)
						fazlaMesaiMap = new TreeMap<String, Tanim>();
					else
						fazlaMesaiMap.clear();
			int okumaAdet = 0;
			List<String> talepGunList = new ArrayList<String>();

			DenklestirmeAy denklestirmeGecenAy = null, denklestirmeGelecekAy = null;
			while (!personelList.isEmpty() && okumaAdet < 2) {
				boolean gunSaatGuncelle = false;
				Boolean tekrarOku = false;
				++okumaAdet;
				boolean flush = false;

				fazlaMesaiMap = ortakIslemler.getFazlaMesaiMap(session);
				fields.clear();
				List<String> superVisorList = null;
				if (loginUser.isSuperVisor()) {
					superVisorList = new ArrayList<String>();
					for (Personel personel : loginUser.getTumPersoneller()) {
						String sicil = personel.getSicilNo();
						if (!PdksUtil.hasStringValue(sicil))
							continue;
						superVisorList.add(sicil);
					}

				}

				if (!perList.isEmpty()) {

					defaultAylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, departmanDenklestirmeDonemi, session);
					fazlaMesaiOrtakIslemler.setDenklestirmeAySure(defaultAylikPuantajSablon.getVardiyalar(), aramaSecenekleri.getSirket(), denklestirmeAy, session);
					for (CalismaModeliAy cm : denklestirmeAy.getModeller()) {
						modelMap.put(cm.getCalismaModeli().getId(), cm);
					}
					defaultAylikPuantajSablon.setFazlaMesaiHesapla(Boolean.TRUE);

					basTarih = departmanDenklestirmeDonemi.getBaslangicTarih();
					bitTarih = departmanDenklestirmeDonemi.getBitisTarih();

					try {

						if (aramaSecenekleri.getEkSaha3Id() != null && !personelList.isEmpty() && !gorevliPersonelMap.isEmpty()) {
							for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
								Personel personel = (Personel) iterator.next();
								if (personel.getEkSaha3() == null || personel.getIseGirisTarihi() == null || personel.getIstenAyrilisTarihi() == null)
									iterator.remove();
								else if (!personel.getEkSaha3().getId().equals(aramaSecenekleri.getEkSaha3Id()) && !helpPersonel(personel))
									iterator.remove();
								else if (!(personel.getIseBaslamaTarihi().getTime() <= bitGun.getTime() && personel.getSskCikisTarihi().getTime() <= basGun.getTime()))
									iterator.remove();
							}
						}
					} catch (Exception e) {
						logger.error("Pdks hata in : \n");
						e.printStackTrace();
						logger.error("Pdks hata out : " + e.getMessage());

						personelList = null;
					}
				}
				if (personelList == null)
					personelList = new ArrayList<Personel>();

				if (!personelList.isEmpty()) {
					if (denklestirmeGecenAy == null || denklestirmeGelecekAy == null) {
						if (aramaSecenekleri.getEkSaha3Id() != null && PdksUtil.isSistemDestekVar()) {
							seciliBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, aramaSecenekleri.getEkSaha3Id(), Tanim.class, session);
						}
						if (aramaSecenekleri.getEkSaha4Id() != null && PdksUtil.isSistemDestekVar()) {
							seciliAltBolum = (Tanim) pdksEntityController.getSQLParamByFieldObject(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, aramaSecenekleri.getEkSaha4Id(), Tanim.class, session);
						}

						if (!loginUser.isDirektorSuperVisor() && !loginUser.isIK() && (loginUser.isYonetici() || loginUser.isYoneticiKontratli())) {
							ortakIslemler.sistemeGirisIslemleri(islemYapan, Boolean.TRUE, basTarih, bitTarih, session);

							boolean hastane = ortakIslemler.getParameterKey("uygulamaTipi").equals("H");

							if (aramaSecenekleri.getEkSaha3Id() != null && hastane) {
								if (aramaSecenekleri.getGorevYeriList() != null) {
									if (aramaSecenekleri.getEkSaha3Id() != null)
										gorevYerileri.add(aramaSecenekleri.getEkSaha3Id());
									else
										for (Iterator iterator = aramaSecenekleri.getGorevYeriList().iterator(); iterator.hasNext();) {
											SelectItem item = (SelectItem) iterator.next();
											gorevYerileri.add((Long) item.getValue());
										}
									List<VardiyaGorev> gorevliler = ortakIslemler.getVardiyaGorevYerleri(loginUser, defaultAylikPuantajSablon.getIlkGun(), defaultAylikPuantajSablon.getSonGun(), gorevYerileri, session);
									for (VardiyaGorev pdksVardiyaGorev : gorevliler) {
										Personel personel = pdksVardiyaGorev.getVardiyaGun().getPersonel();
										String perNo = personel.getPdksSicilNo();
										if (PdksUtil.hasStringValue(perNo) == false)
											continue;
										perNo = perNo.trim();
										if (!perList.contains(perNo) && (PdksUtil.hasStringValue(sicilNo) == false || sicilNo.equals(perNo)))
											gorevliPersonelMap.put(personel.getPdksSicilNo().trim(), personel);
									}

									if (!gorevliPersonelMap.isEmpty())
										perList.addAll(new ArrayList(gorevliPersonelMap.keySet()));
								}

							}

						}
						fields.clear();
						int gecenAy = ay - 1;
						int gecenYil = yil;
						if (gecenAy == 0) {
							gecenAy = 12;
							gecenYil = yil - 1;
						}

						denklestirmeGecenAy = ortakIslemler.getSQLDenklestirmeAy(gecenYil, gecenAy, session);
						if (denklestirmeGecenAy == null) {
							denklestirmeGecenAy = new DenklestirmeAy();
							denklestirmeGecenAy.setOlusturanUser(user);
							denklestirmeGecenAy.setAy(gecenAy);
							denklestirmeGecenAy.setYil(gecenYil);
							saveOrUpdate(denklestirmeGecenAy);
							sessionFlush();

						}
						fields.clear();
						int gelecekAy = ay + 1;
						int gelecekAyYil = yil;
						if (gelecekAy > 12) {
							gelecekAy = 1;
							gelecekAyYil = yil + 1;
						}

						denklestirmeGelecekAy = ortakIslemler.getSQLDenklestirmeAy(gelecekAyYil, gelecekAy, session);
						if (denklestirmeGelecekAy == null) {
							denklestirmeGelecekAy = new DenklestirmeAy();
							denklestirmeGelecekAy.setOlusturanUser(user);
							denklestirmeGelecekAy.setAy(gelecekAy);
							denklestirmeGelecekAy.setYil(gelecekAyYil);
							saveOrUpdate(denklestirmeGelecekAy);
							sessionFlush();
						}
						if (ay != 1)
							gecenAylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay - 1, yil, denklestirmeDonemiGecenAy, session);
						else
							gecenAylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(12, yil - 1, denklestirmeDonemiGecenAy, session);
					}
					boolean mevcutDonem = yil == cal.get(Calendar.YEAR) && ay == cal.get(Calendar.MONTH) + 1;
					TreeMap<Long, Personel> perKeyMap = new TreeMap<Long, Personel>();
					for (Personel personel : personelList) {
						perKeyMap.put(personel.getId(), personel);
					}
					perIdler.addAll(new ArrayList<Long>(perKeyMap.keySet()));
					if (testDurum)
						logger.info("aylikPuantajOlusturuluyor 3000 " + PdksUtil.getCurrentTimeStampStr());
					List<VardiyaGun> vardiyaGunList = null;
					HashMap<Long, List<PersonelIzin>> izinMap = ortakIslemler.getPersonelIzinMap(perIdler, ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemiGecenAy.getBaslangicTarih(), -2), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 1), session);

					TreeMap<String, VardiyaGun> yeniVardiyaMap = null;
					if (denklestirmeAyDurum) {
						try {
							vardiyaGunList = ortakIslemler.getAllPersonelIdVardiyalar(perIdler, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -7), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 7), Boolean.FALSE, session);
							List<Long> olmayanPerList = new ArrayList<Long>(perIdler);
							for (VardiyaGun vardiyaGun : vardiyaGunList) {
								vardiyaGun.setAyinGunu(vardiyaGun.getVardiyaDateStr().startsWith(donem));
								if (vardiyaGun.isAyinGunu()) {
									Long perId = vardiyaGun.getPersonel().getId();
									if (olmayanPerList.contains(perId))
										olmayanPerList.remove(perId);
								}
							}
							if (!olmayanPerList.isEmpty()) {
								List<Personel> perYeniList = new ArrayList<Personel>();
								for (Long key : olmayanPerList)
									perYeniList.add(perKeyMap.get(key));
								yeniVardiyaMap = ortakIslemler.getVardiyalar(perYeniList, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -7), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 7), izinMap, true, session, false);
								tekrarOku = true;
								if (!yeniVardiyaMap.isEmpty())
									vardiyaGunList = ortakIslemler.getAllPersonelIdVardiyalar(perIdler, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -7), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 7), Boolean.FALSE, session);
								perYeniList = null;

							}
						} catch (Exception e) {
							vardiyaGunList = ortakIslemler.getAllPersonelIdVardiyalar(perIdler, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -7), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 7), Boolean.FALSE, session);
						}
					} else
						vardiyaGunList = ortakIslemler.getPersonelIdVardiyalar(perIdler, ortakIslemler.tariheGunEkleCikar(cal, basTarih, -7), ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 7), null, session);
					if (yeniVardiyaMap == null)
						yeniVardiyaMap = new TreeMap<String, VardiyaGun>();
					if (testDurum)
						logger.info("aylikPuantajOlusturuluyor 4000 " + PdksUtil.getCurrentTimeStampStr());

					perKeyMap = null;
					fields.clear();

					fields.clear();

					Vardiya offVardiya = ortakIslemler.getVardiyaOFF(session);

					TreeMap<Long, PersonelDenklestirme> denklestirmeMap = getPersonelDenklestirme(denklestirmeAy, perIdler);
					List<Long> pdIdList = new ArrayList<Long>();
					for (Long key : denklestirmeMap.keySet()) {
						PersonelDenklestirme pd = denklestirmeMap.get(key);
						pdIdList.add(pd.getId());
					}
					HashMap<Long, List<PersonelDenklestirmeDinamikAlan>> hashMap = new HashMap<Long, List<PersonelDenklestirmeDinamikAlan>>();
					if (!pdIdList.isEmpty()) {
						TreeMap<String, PersonelDenklestirmeDinamikAlan> dinamikMap = new TreeMap<String, PersonelDenklestirmeDinamikAlan>();
						ortakIslemler.setDenklestirmeDinamikDurum(pdIdList, dinamikMap, session);
						if (!dinamikMap.isEmpty()) {
							for (String str : dinamikMap.keySet()) {
								PersonelDenklestirmeDinamikAlan pda = dinamikMap.get(str);
								Long key = pda.getPersonelDenklestirme().getId();
								List<PersonelDenklestirmeDinamikAlan> list = hashMap.containsKey(key) ? hashMap.get(key) : new ArrayList<PersonelDenklestirmeDinamikAlan>();
								if (list.isEmpty())
									hashMap.put(key, list);
								list.add(pda);

							}
						}
						dinamikMap = null;
					}
					HashMap<Long, PersonelDonemselDurum> sutIzniMap = new HashMap<Long, PersonelDonemselDurum>();

					flush = denklestirmeMap.isEmpty();
					TreeMap<Long, PersonelDenklestirme> denklestirmeGecenAyMap = getPersonelDenklestirme(denklestirmeGecenAy, perIdler);
					TreeMap<Long, PersonelDenklestirme> denklestirmeGelecekAyMap = getPersonelDenklestirme(denklestirmeGelecekAy, perIdler);

					TreeMap<String, VardiyaHafta> vardiyaHaftaMap = getVardiyaHaftaMap(perIdler);
					if (!flush)
						flush = vardiyaHaftaMap.isEmpty();
					boolean bolumGorevlendirmeVar = ortakIslemler.getParameterKey("bolumGorevlendirmeVar").equals("1");
					fields.clear();
					// fields.put(PdksEntityController.MAP_KEY_MAP, "getVardiyaGunId");
					String fieldName = "vardiyaGun.id";
					fields.put(fieldName, vardiyaGunList);
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					TreeMap<Long, VardiyaGorev> vardiyaGunGorevMap = bolumGorevlendirmeVar ? ortakIslemler.getParamTreeMap(Boolean.FALSE, "getVardiyaGunId", false, vardiyaGunList, fieldName, fields, VardiyaGorev.class, session) : new TreeMap<Long, VardiyaGorev>();

					List<Long> idList = null;
					if ((loginUser.isYonetici() || loginUser.isYoneticiKontratli()) && !loginUser.isDirektorSuperVisor()) {
						idList = new ArrayList<Long>();
						if (aramaSecenekleri.getGorevYeriList() != null) {
							for (SelectItem selectItem : aramaSecenekleri.getGorevYeriList())
								idList.add((Long) selectItem.getValue());

						}
						if (departmanBolumAyni == false && aramaSecenekleri.getTesisId() != null && aramaSecenekleri.getTesisId() > 0)
							idList.add(aramaSecenekleri.getTesisId());
						if (aramaSecenekleri.getEkSaha3Id() != null && !idList.contains(aramaSecenekleri.getEkSaha3Id()))
							idList.add(aramaSecenekleri.getEkSaha3Id());
					}

					for (VardiyaGun pdksVardiyaGun : vardiyaGunList) {
						pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(donem));
						VardiyaGorev pdksVardiyaGorev = vardiyaGunGorevMap.containsKey(pdksVardiyaGun.getId()) ? vardiyaGunGorevMap.get(pdksVardiyaGun.getId()) : null;
						if (pdksVardiyaGorev == null) {
							pdksVardiyaGorev = new VardiyaGorev();
							pdksVardiyaGorev.setVardiyaGun(pdksVardiyaGun);
						} else if (pdksVardiyaGorev.getYeniGorevYeri() != null && idList != null && !idList.isEmpty() && !helpPersonel(pdksVardiyaGun.getPersonel()) && idList.contains(pdksVardiyaGorev.getYeniGorevYeri().getId())) {
							if (pdksVardiyaGorev.isOzelDurumYok()) {
								pdksEntityController.deleteObject(session, entityManager, pdksVardiyaGorev);
								pdksVardiyaGorev = null;
								pdksVardiyaGorev = new VardiyaGorev();
								pdksVardiyaGorev.setVardiyaGun(pdksVardiyaGun);
								flush = true;
							} else if (gorevYeriGirisDurum) {
								pdksVardiyaGorev.setYeniGorevYeri(null);
								saveOrUpdate(pdksVardiyaGorev);
								flush = true;
							}
						}
						pdksVardiyaGun.setVardiyaGorev(pdksVardiyaGorev);
						setVardiyalarToMap(pdksVardiyaGun, vardiyalarMap, null);
					}
					TreeMap<Long, List> bagliIdMap = new TreeMap<Long, List>();
					if (!vardiyaGunList.isEmpty()) {
						List<YemekIzin> yemekGenelList = ortakIslemler.getYemekList(basTarih, bitTarih, session);
						ortakIslemler.setVardiyaYemekList(vardiyaGunList, yemekGenelList);
						setFazlaMesaiTalepVar(false);
						List<Long> vardiyaIdList = new ArrayList<Long>();
						for (VardiyaGun vardiyaGun : vardiyaGunList) {
							if (vardiyaGun.getId() != null) {
								if (vardiyaGun.isFazlaMesaiTalepDurum() && vardiyaGun.isAyinGunu()) {
									setFazlaMesaiTalepVar(true);
									if (!talepGunList.contains(vardiyaGun.getVardiyaDateStr()))
										talepGunList.add(vardiyaGun.getVardiyaDateStr());
									vardiyaIdList.add(vardiyaGun.getId());
								}

							}

						}
						if (testDurum)
							logger.info("aylikPuantajOlusturuluyor 5000 " + PdksUtil.getCurrentTimeStampStr());
						if (!vardiyaIdList.isEmpty()) {
							List<FazlaMesaiTalep> fazlaMesaiList = getVardiyaTable(vardiyaIdList, FazlaMesaiTalep.TABLE_NAME, FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN, FazlaMesaiTalep.class);
							if (fazlaMesaiList != null && !fazlaMesaiList.isEmpty()) {
								for (FazlaMesaiTalep fazlaMesaiTalep : fazlaMesaiList) {
									Long id = fazlaMesaiTalep.getVardiyaGun().getId();
									List list = bagliIdMap.containsKey(id) ? bagliIdMap.get(id) : new ArrayList();
									if (list.isEmpty())
										bagliIdMap.put(id, list);
									list.add(fazlaMesaiTalep);
								}
							}

							List<PersonelFazlaMesai> personelFazlaMesaiList = getVardiyaTable(vardiyaIdList, PersonelFazlaMesai.TABLE_NAME, PersonelFazlaMesai.COLUMN_NAME_VARDIYA_GUN, PersonelFazlaMesai.class);
							if (personelFazlaMesaiList != null && !personelFazlaMesaiList.isEmpty()) {
								for (PersonelFazlaMesai personelFazlaMesai : personelFazlaMesaiList) {
									Long id = personelFazlaMesai.getVardiyaGun().getId();
									List list = bagliIdMap.containsKey(id) ? bagliIdMap.get(id) : new ArrayList();
									if (list.isEmpty())
										bagliIdMap.put(id, list);
									list.add(personelFazlaMesai);
								}
							}
						}
						vardiyaIdList = null;
					}

					vardiyaGunList = null;
					vardiyaGunGorevMap = null;
					//
					TreeMap<String, Personel> perMap = new TreeMap<String, Personel>();

					List<Long> kontratliPerIdList = new ArrayList<Long>();
					for (Personel personel : personelList) {
						Tanim grup = personel.getPlanGrup2();
						String key = (personel.getPdksYonetici() != null ? personel.getPdksYonetici().getPdksSicilNo() + "_" : "");
						key += (personel.getEkSaha3() != null ? personel.getEkSaha3().getId().toString() : "");
						key += "_" + (grup != null ? grup.getId().toString() : "");
						key += "_" + personel.getPdksSicilNo() + "_" + personel.getId();
						perMap.put(key, personel);
					}

					personelList = null;
					personelList = new ArrayList<Personel>(perMap.values());

					if (!personelList.isEmpty()) {
						TreeMap<Long, Long> sirketIdMap = new TreeMap<Long, Long>(), sirketMap = new TreeMap<Long, Long>();
						kontratliPerIdList.clear();
						for (Personel personel : personelList) {
							kontratliPerIdList.add(personel.getId());
							if (personel.getSirket() != null)
								sirketMap.put(personel.getSirket().getId(), personel.getId());
							if (personel.getPdksYonetici() != null)
								sirketIdMap.put(personel.getPdksYonetici().getId(), personel.getId());
						}
						departmanBolumAyni = sirketMap.size() > 1;
						sirketMap = null;
						if (kontratliPerIdList.size() > 40)
							personelList = ortakIslemler.getKontratliSiraliPersonel(kontratliPerIdList, session);
						else
							personelList = (ArrayList<Personel>) PdksUtil.sortObjectStringAlanList(personelList, "getKontratliSortKey", null);

						List<Personel> perDigerList = new ArrayList<Personel>();
						if (ikRole == false && adminRole == false) {
							if (sirketIdMap.size() > 0 && sirketIdMap.size() < personelList.size()) {
								for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
									Personel personel = (Personel) iterator.next();
									if (!sirketIdMap.containsKey(personel.getId())) {
										perDigerList.add(personel);
										iterator.remove();
									}
								}
							}
							if (!perDigerList.isEmpty())
								personelList.addAll(perDigerList);
							perDigerList = null;
						}
						sirketIdMap = null;
					}
					kontratliPerIdList = null;
					perMap = null;
					if (testDurum)
						logger.info("aylikPuantajOlusturuluyor 6000 " + PdksUtil.getCurrentTimeStampStr());
					tatilGunleriMap = ortakIslemler.getTatilGunleri(personelList, denklestirmeDonemiGecenAy.getBaslangicTarih(), bitTarih, session);

					fazlaMesaiOrtakIslemler.haftalikVardiyaOlustur(vardiyaHaftaList, gecenAylikPuantajSablon, denklestirmeDonemiGecenAy, tatilGunleriMap, null);

					vardiyaHaftaList = new ArrayList<VardiyaHafta>();
					VardiyaPlan pdksVardiyaPlanMaster = fazlaMesaiOrtakIslemler.haftalikVardiyaOlustur(vardiyaHaftaList, defaultAylikPuantajSablon, departmanDenklestirmeDonemi, tatilGunleriMap, aylikSablonVardiyalar);
					if (testDurum)
						logger.info("aylikPuantajOlusturuluyor 7000 " + PdksUtil.getCurrentTimeStampStr());

					this.setVardiyaPlan(pdksVardiyaPlanMaster);
					if (gorevliPersonelMap != null)
						gorevliPersonelMap.clear();
					else
						gorevliPersonelMap = new HashMap<String, Personel>();

					for (Iterator iterator2 = aylikSablonVardiyalar.iterator(); iterator2.hasNext();) {
						VardiyaGun pdksVardiyaGunMaster = (VardiyaGun) iterator2.next();
						pdksVardiyaGunMaster.setDonemStr(donem);

						String tarih = PdksUtil.convertToDateString(pdksVardiyaGunMaster.getVardiyaDate(), "yyyyMMdd");
						pdksVardiyaGunMaster.setAyinGunu(tarih.startsWith(donem));
						if (tatilGunleriMap.containsKey(tarih))
							pdksVardiyaGunMaster.setTatil(tatilGunleriMap.get(tarih));
						else
							pdksVardiyaGunMaster.setTatil(null);
					}
					TreeMap<String, CalismaModeliAy> cmaMap = new TreeMap<String, CalismaModeliAy>();

					List<Long> plansizList = new ArrayList<Long>();
					for (Personel personel : personelList) {

						boolean pdks = false;
						try {
							pdks = personel.getMailTakip() && personel.getPdks().equals(Boolean.FALSE);
						} catch (Exception e) {
							pdks = false;
						}
						boolean vardiyaCalisiyor = Boolean.FALSE;
						long iseBasTarih = Long.parseLong(PdksUtil.convertToDateString(personel.getIseGirisTarihi(), "yyyyMMdd"));
						long istenAyrilmaTarih = Long.parseLong(PdksUtil.convertToDateString(personel.getSonCalismaTarihi(), "yyyyMMdd"));
						VardiyaSablonu sablonu = personel.getSablon();
						AylikPuantaj aylikPuantaj = new AylikPuantaj(), gecenAylikPuantaj = new AylikPuantaj();
						List<VardiyaGun> vardiyaGunler = new ArrayList<VardiyaGun>();

						aylikPuantaj.setGecenAylikPuantaj(gecenAylikPuantaj);
						gecenAylikPuantaj.setPdksPersonel(personel);
						PersonelDenklestirme personelDenklestirme = null;
						PersonelDonemselDurum sutIzniDurum = null;
						String cmaKey = CalismaModeliAy.getKey(denklestirmeAy, personel.getCalismaModeli());
						CalismaModeliAy cma = null;
						if (denklestirmeMap.containsKey(personel.getId()))
							personelDenklestirme = denklestirmeMap.get(personel.getId());
						else {
							if (cmaMap.containsKey(cmaKey))
								cma = cmaMap.get(cmaKey);
							else {
								cma = ortakIslemler.getCalismaModeliAy(denklestirmeAy, personel.getCalismaModeli(), session);
								if (cma == null) {
									cma = new CalismaModeliAy(denklestirmeAy, personel.getCalismaModeli());
									saveOrUpdate(cma);
									flush = true;
									gunSaatGuncelle = true;
								}
								cmaMap.put(cmaKey, cma);
							}

							personelDenklestirme = new PersonelDenklestirme(personel, denklestirmeAy, cma);

							saveOrUpdate(personelDenklestirme);
							if (plansiz)
								plansizList.add(personel.getId());
							flush = true;

						}
						if (personelDenklestirme.getCalismaModeliAy() == null) {
							if (cmaMap.containsKey(cmaKey))
								cma = cmaMap.get(cmaKey);
							else
								cma = ortakIslemler.getCalismaModeliAy(denklestirmeAy, personel.getCalismaModeli(), session);
							if (cma == null) {
								cma = new CalismaModeliAy(denklestirmeAy, personel.getCalismaModeli());
								saveOrUpdate(cma);
								flush = true;
								gunSaatGuncelle = true;
							}
							personelDenklestirme.setCalismaModeliAy(cma);
							saveOrUpdate(personelDenklestirme);
							flush = true;
							cmaMap.put(cmaKey, cma);

						}
						// if (pddMap.containsKey(personel.getId())) {
						// List<PersonelDonemselDurum> list = pddMap.get(personel.getId());
						// for (Iterator iterator = list.iterator(); iterator.hasNext();) {
						// PersonelDonemselDurum pdd = (PersonelDonemselDurum) iterator.next();
						// boolean donemIci = donemBas.getTime() <= pdd.getBitTarih().getTime() && donemBit.getTime() >= pdd.getBasTarih().getTime();
						// if (donemIci) {
						// if (pdd.isGebe())
						// personelDenklestirme.setGebePersonelDonemselDurum(pdd);
						// else if (pdd.isSutIzni()) {
						// personelDenklestirme.setSutIzniPersonelDonemselDurum(pdd);
						//
						// } else if (pdd.getIsAramaIzni())
						// personelDenklestirme.setIsAramaPersonelDonemselDurum(pdd);
						// }
						//
						// }
						// list = null;

						// }
						if (cma != null) {
							if (!gunSaatGuncelle)
								gunSaatGuncelle = cma.getSure() == 0.0d || cma.getToplamIzinSure() == 0.0d;
							if (cma.getDurum().booleanValue() == false) {
								cma.setDurum(Boolean.TRUE);
								saveOrUpdate(cma);
								flush = true;
								gunSaatGuncelle = true;
							}
						}

						if (personelDenklestirme.getId() != null && hashMap.containsKey(personelDenklestirme.getId())) {
							List<PersonelDenklestirmeDinamikAlan> list = hashMap.get(personelDenklestirme.getId());
							TreeMap<Long, PersonelDenklestirmeDinamikAlan> dinamikAlanMap = new TreeMap<Long, PersonelDenklestirmeDinamikAlan>();
							for (PersonelDenklestirmeDinamikAlan pda : list) {
								dinamikAlanMap.put(pda.getId(), pda);
							}
							aylikPuantaj.setDinamikAlanMap(dinamikAlanMap);
						}

						if (denklestirmeAyDurum) {

							boolean kaydet = personelDenklestirme.getId() == null;
							if (personelDenklestirme.getPersonelDenklestirmeGecenAy() == null && denklestirmeGecenAyMap.containsKey(personel.getId())) {
								kaydet = true;
								personelDenklestirme.setPersonelDenklestirmeGecenAy(denklestirmeGecenAyMap.get(personel.getId()));
								personelDenklestirme.setGuncellendi(Boolean.TRUE);
							}
							if (personelDenklestirme.getCalismaModeliAy() != null && personelDenklestirme.getPlanlanSure().doubleValue() == 0.0d && personelDenklestirme.getCalismaModeli().getToplamGunGuncelle()) {
								personelDenklestirme.setPlanlanSure(personelDenklestirme.getCalismaModeliAy().getSure());
								saveOrUpdate(personelDenklestirme);
								flush = true;
								kaydet = true;
							}
							boolean denklestirme = true;

							if (kaydet) {
								if (personelDenklestirme.getId() == null) {
									if (sutIzniMap.containsKey(personel.getId()))
										sutIzniDurum = sutIzniMap.get(personel.getId());

									if (personelDenklestirme.getCalismaModeliAy() == null || personelDenklestirme.getCalismaModeli().getToplamGunGuncelle().equals(Boolean.FALSE))
										personelDenklestirme.setPlanlanSure(-1d);
									personelDenklestirme.setOlusturanUser(loginUser);
									denklestirme = personelDenklestirme.isDenklestirme();
								}
							} else {
								denklestirme = personelDenklestirme.isDenklestirme();

								if (denklestirmeAyDurum) {
									kaydet = personelDenklestirme.isSuaDurumu() != personel.isSuaOlur();
									if (!kaydet)
										kaydet = denklestirme != personelDenklestirme.isDenklestirme();
									if (!kaydet)
										try {
											kaydet = personel.getFazlaMesaiOde() != null && personelDenklestirme.getFazlaMesaiOde() != null && !personelDenklestirme.getFazlaMesaiOde().equals(personel.getFazlaMesaiOde());
										} catch (Exception e1) {
											e1.printStackTrace();
										}

								}

							}
							if (mevcutDonem) {
								if (personelDenklestirme.getCalismaModeliAy() != null && personelDenklestirme.getCalismaModeli() != null) {
									CalismaModeli calismaModeli = personelDenklestirme.getCalismaModeli();
									if (calismaModeli.getBagliVardiyaSablonu() != null) {
										VardiyaSablonu bagliVardiyaSablonu = calismaModeli.getBagliVardiyaSablonu();
										Personel personelDenk = personelDenklestirme.getPersonel();
										if (!bagliVardiyaSablonu.getId().equals(personelDenk.getSablon().getId())) {
											// personelDenk.setSablon(bagliVardiyaSablonu);
											// saveOrUpdate( personelDenk);
											// flush = true;
											// kaydet = true;
										}
									}

								}

							}

							if (kaydet) {
								personelDenklestirme.setGuncellendi(personelDenklestirme.getId() == null);
								if (personelDenklestirme.getId() != null) {
									personelDenklestirme.setGuncellemeTarihi(new Date());
								}
								if (denklestirmeAyDurum) {
									if (personel.getEgitimDonemi() != null && personel.getEgitimDonemi() && personelDenklestirme.getEgitimSuresiAksamGunSayisi() == null)
										personelDenklestirme.setEgitimSuresiAksamGunSayisi(0);
									if (personelDenklestirme.getId() == null)
										personelDenklestirme.setFazlaMesaiOde(personel.getFazlaMesaiOde());

									personelDenklestirme.setDenklestirme(denklestirme);
									if (personel.isSuaOlur() && personelDenklestirme.getSuaDurum() == null)
										personelDenklestirme.setSuaDurum(personel.isSuaOlur());
								}

								if (!personelDenklestirme.isKapandi())
									personelDenklestirme.setDevredenSure(null);
								if (personelDenklestirme.isGuncellendi()) {
									saveOrUpdate(personelDenklestirme);
									flush = true;
								}

							}
							aylikPuantaj.setPersonelDenklestirme(personelDenklestirme);

						} else {
						}
						List<VardiyaGun> vardiyaGunHareketOnaysizList = new ArrayList<VardiyaGun>();

						aylikPuantaj.setPdksPersonel(personel);
						aylikPuantaj.setVardiyaPlan(new VardiyaPlan());
						aylikPuantaj.getVardiyaPlan().getVardiyaHaftaList().clear();
						aylikPuantaj.setTrClass(devam ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
						devam = !devam;
						double saatToplami = 0d;
						CalismaModeliAy calismaModeliAy = denklestirmeAyDurum && personelDenklestirme != null ? personelDenklestirme.getCalismaModeliAy() : null;
						Date sonCalismaTarihi = personel.getSonCalismaTarihi(), iseGirisTarihi = personel.getIseGirisTarihi();
						boolean personelCalmayaBasladi = false, calisiyor = false, hareketKaydiVardiyaBul = calismaModeliAy != null && calismaModeliAy.isHareketKaydiVardiyaBulsunmu();
						for (Iterator iterator2 = aylikSablonVardiyalar.iterator(); iterator2.hasNext();) {
							VardiyaGun pdksVardiyaGunMaster = (VardiyaGun) iterator2.next();
							VardiyaGun pdksVardiyaGun = new VardiyaGun(personel, null, pdksVardiyaGunMaster.getVardiyaDate());
							if (!personelCalmayaBasladi)
								personelCalmayaBasladi = pdksVardiyaGunMaster.getVardiyaDate().getTime() >= iseGirisTarihi.getTime();
							String vardiyaKey = pdksVardiyaGun.getVardiyaKey();
							if (vardiyalarMap.containsKey(vardiyaKey)) {
								pdksVardiyaGun = vardiyalarMap.get(pdksVardiyaGun.getVardiyaKey());
								pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(donem));
							}
							pdksVardiyaGun.setDonemStr(pdksVardiyaGunMaster.getDonemStr());

							pdksVardiyaGun.setTdClass(aylikPuantaj.getTrClass());
							boolean kayit = false;
							if (!pdksVardiyaGun.isCalismayaBaslamadi() && !pdksVardiyaGun.isCalismayiBirakti()) {
								boolean yeniKayit = false;
								if (pdksVardiyaGun.getId() == null) {
									yeniKayit = true;
									if (pdksVardiyaGun.isAyinGunu())
										tekrarOku = true;
									if (tatilGunleriMap.containsKey(pdksVardiyaGun.getVardiyaDateStr()))
										pdksVardiyaGun.setTatil(tatilGunleriMap.get(pdksVardiyaGun.getVardiyaDateStr()));
									int haftaGunu = PdksUtil.getDateField(pdksVardiyaGun.getVardiyaDate(), Calendar.DAY_OF_WEEK);
									switch (haftaGunu) {
									case Calendar.MONDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya1());
										break;
									case Calendar.TUESDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya2());
										break;
									case Calendar.WEDNESDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya3());
										break;
									case Calendar.THURSDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya4());
										break;
									case Calendar.FRIDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya5());
										break;
									case Calendar.SATURDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya6());
										break;
									case Calendar.SUNDAY:
										pdksVardiyaGun.setVardiya(sablonu.getVardiya7());
										break;
									default:
										break;
									}
									pdksVardiyaGun.setOlusturanUser(loginUser);
									if (yeniKayit && pdksVardiyaGun.getTatil() != null && pdksVardiyaGun.getVardiya().isCalisma()) {
										Tatil tatil = pdksVardiyaGun.getTatil();
										if (!tatil.isYarimGunMu())
											pdksVardiyaGun.setVardiya(offVardiya);
									}

									try {
										if (personelCalmayaBasladi && pdksVardiyaGunMaster.getVardiyaDate().getTime() <= sonCalismaTarihi.getTime()) {
											calisiyor = true;
											kayit = true;
										}

									} catch (Exception ee) {
										if (calisiyor)
											logger.error(pdksVardiyaGun.getVardiyaKeyStr() + "\n" + ee);
									}

									VardiyaGorev pdksVardiyaGorev = new VardiyaGorev();
									pdksVardiyaGorev.setVardiyaGun(pdksVardiyaGun);
									pdksVardiyaGun.setVardiyaGorev(pdksVardiyaGorev);
									flush = true;
								}
								if (pdksVardiyaGun.isAyinGunu()) {
									if (hareketKaydiVardiyaBul && !pdksVardiyaGun.getVardiya().isHaftaTatil() && pdksVardiyaGun.getVersion() >= 0) {
										if ((yeniKayit || yeniVardiyaMap.containsKey(vardiyaKey))) {
											kayit = true;
											if (pdksVardiyaGun.getVardiya().isCalisma())
												pdksVardiyaGun.setVersion(-1);
											vardiyaGunHareketOnaysizList.add(pdksVardiyaGun);

										}
									}
									vardiyalarMap.put(pdksVardiyaGun.getVardiyaKey(), pdksVardiyaGun);
									if (hareketKaydiVardiyaBul && !pdksVardiyaGun.getDurum() && pdksVardiyaGun.getVersion() == 0 && !pdksVardiyaGun.getVardiya().isHaftaTatil())
										aylikHareketKaydiVardiyaBul = Boolean.TRUE;
								}
								if (kayit) {
									if (pdksVardiyaGun.isAyinGunu() && denklestirmeAyDurum)
										pdksVardiyaGun.setDurum(false);
									saveOrUpdate(pdksVardiyaGun);
									flush = true;
								}
							} else {
								if (pdksVardiyaGun.getId() != null) {
									if (pdksVardiyaGun.getVardiyaGorev().getId() != null) {
										VardiyaGorev pdksVardiyaGorev = pdksVardiyaGun.getVardiyaGorev();
										pdksEntityController.deleteObject(session, entityManager, pdksVardiyaGorev);
										pdksVardiyaGun.getVardiyaGorev().setId(null);
										pdksVardiyaGun.getVardiyaGorev().setYeniGorevYeri(null);
									}
									if (bagliIdMap.containsKey(pdksVardiyaGun.getId())) {
										List list = bagliIdMap.get(pdksVardiyaGun.getId());
										for (Iterator iterator = list.iterator(); iterator.hasNext();) {
											Object object = (Object) iterator.next();
											pdksEntityController.deleteObject(session, entityManager, object);
										}
									}
									pdksEntityController.deleteObject(session, entityManager, pdksVardiyaGun);
									flush = true;
									pdksVardiyaGun.setId(null);
									pdksVardiyaGun.setVardiya(null);

								}

							}

							vardiyaGunler.add(pdksVardiyaGun);

						}
						TreeMap<String, VardiyaGun> vardiyaGunMap = new TreeMap<String, VardiyaGun>();
						for (VardiyaGun pdksVardiyaGun : vardiyaGunler) {
							pdksVardiyaGun.setIzin(null);
							if (pdksVardiyaGun.getVardiya() != null && !pdksVardiyaGun.isAyinGunu()) {
								if (pdksVardiyaGun.getVardiyaDate().before(defaultAylikPuantajSablon.getIlkGun())) {
									if (aylikPuantaj.getPersonelDenklestirmeGecenAy() == null && denklestirmeGecenAyMap.containsKey(pdksVardiyaGun.getPersonel().getId()))
										aylikPuantaj.setPersonelDenklestirmeGecenAy(denklestirmeGecenAyMap.get(pdksVardiyaGun.getPersonel().getId()));
								} else if (pdksVardiyaGun.getVardiyaDate().after(defaultAylikPuantajSablon.getSonGun())) {
									if (aylikPuantaj.getPersonelDenklestirmeGelecekAy() == null && denklestirmeGelecekAyMap.containsKey(pdksVardiyaGun.getPersonel().getId()))
										aylikPuantaj.setPersonelDenklestirmeGelecekAy(denklestirmeGelecekAyMap.get(pdksVardiyaGun.getPersonel().getId()));
								}

							}
							vardiyaGunMap.put(String.valueOf(pdksVardiyaGun.getVardiyaKeyStr()), pdksVardiyaGun);
						}

						boolean haftaRenk = true;

						for (Iterator iterator = defaultAylikPuantajSablon.getVardiyaHaftaList().iterator(); iterator.hasNext();) {
							VardiyaHafta pdksVardiyaHaftaMaster = (VardiyaHafta) iterator.next();
							long planTarih1 = Long.parseLong(PdksUtil.convertToDateString(pdksVardiyaHaftaMaster.getBasTarih(), "yyyyMMdd"));
							long planTarih2 = Long.parseLong(PdksUtil.convertToDateString(pdksVardiyaHaftaMaster.getBitTarih(), "yyyyMMdd"));
							VardiyaHafta pdksVardiyaHafta = (VardiyaHafta) pdksVardiyaHaftaMaster.clone();
							pdksVardiyaHafta.setPersonel(personel);
							if (vardiyaHaftaMap.containsKey(pdksVardiyaHafta.getKeyHafta()))
								pdksVardiyaHafta = vardiyaHaftaMap.get(pdksVardiyaHafta.getKeyHafta());
							pdksVardiyaHafta.setTrClass(haftaRenk ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
							haftaRenk = !haftaRenk;
							pdksVardiyaHafta.setHafta(pdksVardiyaHaftaMaster.getHafta());
							List<VardiyaGun> haftaVardiyaGunleri = new ArrayList<VardiyaGun>();
							List<VardiyaGun> vardiyaGunleri = new ArrayList<VardiyaGun>();
							vardiyaGunleri.addAll(pdksVardiyaHaftaMaster.getVardiyaGunler());
							for (VardiyaGun pdksVardiyaGunMaster : vardiyaGunleri) {
								VardiyaGun pdksVardiyaGun2 = new VardiyaGun(personel, null, pdksVardiyaGunMaster.getVardiyaDate());
								haftaVardiyaGunleri.add(pdksVardiyaGun2);
							}
							vardiyaGunleri = null;
							calisiyor = planTarih2 >= iseBasTarih && istenAyrilmaTarih >= planTarih1;
							if (pdks == false || !calisiyor) {
								if (pdksVardiyaHafta.getId() != null) {
									pdksEntityController.deleteObject(session, entityManager, pdksVardiyaHafta);
									pdksVardiyaHafta.setId(null);
									flush = true;
								}
								if (!calisiyor)
									pdksVardiyaHafta.setVardiyaGunler(haftaVardiyaGunleri);
							} else {
								if (pdks && pdksVardiyaHafta.getId() == null && sablonu != null) {
									pdksVardiyaHafta.setVardiyaSablonu(sablonu);
									pdksVardiyaHafta.setOlusturanUser(loginUser);
									saveOrUpdate(pdksVardiyaHafta);
									flush = true;
								}

							}
							if (calisiyor) {
								pdksVardiyaHafta.getVardiyaGunler().clear();
								for (VardiyaGun pdksVardiyaGun2 : haftaVardiyaGunleri) {
									String key = pdksVardiyaGun2.getVardiyaKeyStr();
									long ayinGunu = Long.parseLong(key.substring(key.indexOf("_") + 1));
									VardiyaGun pdksVardiyaGun = pdksVardiyaGun2;
									if (ayinGunu >= iseBasTarih && istenAyrilmaTarih >= ayinGunu)
										pdksVardiyaGun = vardiyaGunMap.containsKey(key) ? vardiyaGunMap.get(key) : pdksVardiyaGun2;
									pdksVardiyaHafta.getVardiyaGunler().add(pdksVardiyaGun);
								}
							}

							for (VardiyaGun pdksVardiyaGun : pdksVardiyaHafta.getVardiyaGunler()) {

								int haftaGunu = PdksUtil.getDateField(pdksVardiyaGun.getVardiyaDate(), Calendar.DAY_OF_WEEK);
								switch (haftaGunu) {
								case Calendar.MONDAY:
									pdksVardiyaHafta.setVardiyaGun1(pdksVardiyaGun);
									break;
								case Calendar.TUESDAY:

									pdksVardiyaHafta.setVardiyaGun2(pdksVardiyaGun);
									break;
								case Calendar.WEDNESDAY:

									pdksVardiyaHafta.setVardiyaGun3(pdksVardiyaGun);
									break;
								case Calendar.THURSDAY:
									pdksVardiyaHafta.setVardiyaGun4(pdksVardiyaGun);
									break;
								case Calendar.FRIDAY:
									pdksVardiyaHafta.setVardiyaGun5(pdksVardiyaGun);
									break;
								case Calendar.SATURDAY:
									pdksVardiyaHafta.setVardiyaGun6(pdksVardiyaGun);
									break;
								case Calendar.SUNDAY:
									pdksVardiyaHafta.setVardiyaGun7(pdksVardiyaGun);
									break;
								default:
									break;
								}
							}

							aylikPuantaj.getVardiyaPlan().getVardiyaHaftaList().add(pdksVardiyaHafta);

						}
						vardiyaGunMap.clear();
						aylikPuantaj.setGecenAyFazlaMesai(null);
						if (gecenAylikPuantajSablon.getIlkGun().getTime() <= personel.getSonCalismaTarihi().getTime() && gecenAylikPuantajSablon.getSonGun().getTime() >= personel.getIseGirisTarihi().getTime()) {
							gecenAylikPuantaj.setIlkGun(gecenAylikPuantajSablon.getIlkGun());
							gecenAylikPuantaj.setSonGun(gecenAylikPuantajSablon.getSonGun());
							gecenAylikPuantaj.setGunSayisi(gecenAylikPuantajSablon.getGunSayisi());
							gecenAylikPuantaj.setGecenAyFazlaMesai(null);
							if (personelDenklestirme != null)
								gecenAylikPuantaj.setPersonelDenklestirme(personelDenklestirme.getPersonelDenklestirmeGecenAy());
							ortakIslemler.puantajHaftalikPlanOlustur(Boolean.FALSE, vardiyaGunMap, vardiyalarMap, gecenAylikPuantajSablon, gecenAylikPuantaj);
						} else
							aylikPuantaj.setGecenAylikPuantaj(null);
						ortakIslemler.puantajHaftalikPlanOlustur(Boolean.FALSE, vardiyaGunMap, vardiyalarMap, defaultAylikPuantajSablon, aylikPuantaj);
						VardiyaPlan plan = new VardiyaPlan();
						plan.setPersonel(personel);
						plan.setVardiyaGunMap(vardiyaGunMap);

						List<VardiyaGun> list = PdksUtil.sortListByAlanAdi(new ArrayList(vardiyalarMap.values()), "vardiyaDate", Boolean.FALSE);

						setIzin(plan.getPersonel().getId(), izinMap.get(plan.getPersonel().getId()), list);
						try {
							if (gecenAylikPuantaj.getPersonelDenklestirme() != null && !helpPersonel(personel) && loginUser.getLogin()) {
								gecenAylikPuantaj.setCalismaModeliAy(gecenAylikPuantaj.getPersonelDenklestirme().getCalismaModeliAy());
								gecenAylikPuantaj.setLoginUser(loginUser);
								// ortakIslemler.aylikPlanSureHesapla(false, getNormalCalismaVardiya(), true, gecenAylikPuantaj, false, tatilGunleriMap, session);
							}

						} catch (Exception e) {
							logger.error("Pdks hata in : \n");
							e.printStackTrace();
							logger.error("Pdks hata out : " + e.getMessage());

						}
						if (!vardiyaGunHareketOnaysizList.isEmpty()) {
							for (Iterator iterator = vardiyaGunHareketOnaysizList.iterator(); iterator.hasNext();) {
								VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
								if (vardiyaGun.getId() != null && vardiyaGun.isIzinli() && vardiyaGun.isAyinGunu() && vardiyaGun.getVersion() < 0) {
									vardiyaGun.setVersion(0);
									saveOrUpdate(vardiyaGun);
									flush = true;
								}

							}

						}
						vardiyaGunHareketOnaysizList = null;
						if (personelDenklestirme != null && personelDenklestirme.getPlanlanSure() < 0d) {
							if (personelDenklestirme.getCalismaModeliAy() == null || personelDenklestirme.getCalismaModeli().getToplamGunGuncelle().equals(Boolean.FALSE))
								personelDenklestirme.setPlanlanSure(saatToplami);
							if (!personelDenklestirme.isKapandi())
								personelDenklestirme.setDevredenSure(null);
							saveOrUpdate(personelDenklestirme);
							flush = true;
						}
						vardiyaPlan = null;
						vardiyaGunMap = null;
						aylikPuantaj.setVardiyaHaftaList(aylikPuantaj.getVardiyaPlan().getVardiyaHaftaList());
						// if (personelDenklestirme == null) {
						// personelDenklestirme = denklestirmeMap.containsKey(personel.getId()) ? denklestirmeMap.get(personel.getId()) : new PersonelDenklestirme(personel, denklestirmeAy, ortakIslemler.getCalismaModeliAy(denklestirmeAy, personel.getCalismaModeli(), session));
						// aylikPuantaj.setPersonelDenklestirme(personelDenklestirme);
						// }

						if (personelDenklestirme != null) {
							try {
								if (sutIzniDurum != null) {
									Double sutIzniSaatSayisi = sutIzniGuncelle(aylikPuantaj, sutIzniDurum);
									personelDenklestirme.setSutIzniSaatSayisi(sutIzniSaatSayisi);
									personelDenklestirme.setSutIzniDurum(true);
									saveOrUpdate(personelDenklestirme);
									flush = true;
								}

							} catch (Exception es) {
								logger.error(es);
							}

							if (personelDenklestirme.getCalismaModeliAy() == null && personel.getCalismaModeli() != null && modelMap.containsKey(personel.getCalismaModeli().getId())) {
								personelDenklestirme.setCalismaModeliAy(modelMap.get(personel.getCalismaModeli().getId()));
								saveOrUpdate(personelDenklestirme);
								flush = true;
							}
							aylikPuantaj.setCalismaModeliAy(personelDenklestirme.getCalismaModeliAy());
							CalismaModeli calismaModeli = personelDenklestirme.getCalismaModeli();
							if (aylikPuantaj.getPersonelDenklestirme() == null)
								aylikPuantaj.setPersonelDenklestirme(personelDenklestirme);
							aylikPuantaj.setLoginUser(loginUser);
							// if (!helpPersonel(personel)) {
							// ortakIslemler.aylikPlanSureHesapla(false, getNormalCalismaVardiya(), true, aylikPuantaj, denklestirmeAyDurum, tatilGunleriMap, session);
							// }

							if (denklestirmeAy.getSure() == 0.0d) {
								Double genelSaatToplami = aylikPuantaj.getSaatToplami();
								aylikPuantaj.setPlanlananSure(genelSaatToplami);
								int gun = new Double(genelSaatToplami / AylikPuantaj.getGunlukCalismaSuresi()).intValue();
								Double toplamIzinSure = genelSaatToplami - (gun * (AylikPuantaj.getGunlukCalismaSuresi() - 7.5d));
								denklestirmeAy.setSure(genelSaatToplami);
								denklestirmeAy.setToplamIzinSure(toplamIzinSure);
								User sistemUser = ortakIslemler.getSistemAdminUser(session);
								denklestirmeAy.setGuncellemeTarihi(new Date());
								denklestirmeAy.setGuncelleyenUser(sistemUser);
								saveOrUpdate(denklestirmeAy);
								aylikPuantaj.setFazlaMesaiHesapla(false);
								calismaPlaniDenklestir(departmanDenklestirmeDonemi, null, aylikPuantaj);
								flush = true;
							}
							if (!denklestirmeAyDurum) {
								aylikPuantaj.setFazlaMesaiSure(personelDenklestirme.getOdenecekSure());
								aylikPuantaj.setDevredenSure(personelDenklestirme.getDevredenSure());
								aylikPuantaj.setEksikCalismaSure(personelDenklestirme.getEksikCalismaSure());
								aylikPuantaj.setResmiTatilToplami(personelDenklestirme.getResmiTatilSure());
								aylikPuantaj.setHaftaCalismaSuresi(personelDenklestirme.getHaftaCalismaSuresi());
								aylikPuantaj.setAksamVardiyaSayisi(personelDenklestirme.getAksamVardiyaSayisi().intValue());
							} else {

								if (personel.getPartTime() != null && personel.getPartTime().booleanValue()) {
									aylikPuantaj.setFazlaMesaiSure(0d);
									aylikPuantaj.setDevredenSure(0d);
									aylikPuantaj.setResmiTatilToplami(0d);
									aylikPuantaj.setHaftaCalismaSuresi(0d);
									aylikPuantaj.setAksamVardiyaSayisi(0);
									aylikPuantaj.setEksikCalismaSure(0d);
								} else {
									aylikPuantaj.setEksikCalismaSure(0.0d);
									double devredenSure = aylikPuantaj.getDevredenSure();
									if (calismaModeli.isSaatlikOdeme()) {
										if (devredenSure < 0.0d) {
											Double eksikCalismaSure = -aylikPuantaj.getDevredenSure();
											if (aylikPuantaj.getFazlaMesaiSure() > 0.0d) {
												double sure = aylikPuantaj.getFazlaMesaiSure() - eksikCalismaSure;
												eksikCalismaSure = 0.0d;
												aylikPuantaj.setFazlaMesaiSure(0.0d);
												if (sure > 0)
													aylikPuantaj.setFazlaMesaiSure(sure);
												else
													eksikCalismaSure = -sure;
											}
											aylikPuantaj.setEksikCalismaSure(eksikCalismaSure);

										} else if (devredenSure > 0.0d) {
											double sure = aylikPuantaj.getFazlaMesaiSure() + devredenSure;
											aylikPuantaj.setFazlaMesaiSure(sure);
										}
										aylikPuantaj.setDevredenSure(0.0d);
									}

								}

							}
							aylikPuantaj.setOnayDurum(!personelDenklestirme.isCheckBoxDurum());
						} else
							aylikPuantaj.setOnayDurum(Boolean.FALSE);

						if (!onayDurum)
							onayDurum = aylikPuantaj.isOnayDurum();
						int yuvarmaTipi = aylikPuantaj.getYarimYuvarla();
						if (aylikPuantaj.getResmiTatilToplami() > 0)
							aylikPuantaj.setResmiTatilToplami(PdksUtil.setSureDoubleTypeRounded(aylikPuantaj.getResmiTatilToplami(), yuvarmaTipi));
						if (aylikPuantaj.getHaftaCalismaSuresi() > 0)
							aylikPuantaj.setHaftaCalismaSuresi(PdksUtil.setSureDoubleTypeRounded(aylikPuantaj.getHaftaCalismaSuresi(), yuvarmaTipi));
						if (aylikPuantaj.getVardiyalar() != null) {
							for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
								if (pdksVardiyaGun.isAyinGunu() && pdksVardiyaGun.getVardiya() != null) {
									vardiyaCalisiyor = Boolean.TRUE;
									break;
								}

							}
						}
						aylikPuantaj.setVardiyalar(vardiyaGunler);

						if (vardiyaCalisiyor)
							aylikPuantajList.add(aylikPuantaj);
					}
					if (!plansizList.isEmpty()) {
						for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
							AylikPuantaj ap = (AylikPuantaj) iterator.next();
							if (!plansizList.contains(ap.getPdksPersonel().getId()))
								iterator.remove();

						}
						plansiz = false;
					}
					plansizList = null;
					if (gunSaatGuncelle)
						fazlaMesaiOrtakIslemler.setDenklestirmeAySure(defaultAylikPuantajSablon.getVardiyalar(), aramaSecenekleri.getSirket(), denklestirmeAy, session);

				} else {
					PdksUtil.addMessageWarn(denklestirmeAy.getAyAdi() + " " + yil + " dönemine ait çalışma planı bulunamadı!");
					return false;
				}

				if (testDurum)
					logger.info("aylikPuantajOlusturuluyor 8000 " + PdksUtil.getCurrentTimeStampStr());

				setTatilMap(tatilGunleriMap);
				if (flush) {
					sessionFlush();
					veriGuncellendi = Boolean.TRUE;
				}
				if (tekrarOku || gunSaatGuncelle) {
					tekrarOku = gunSaatGuncelle || (ikRole == false && adminRole == false) && denklestirmeAyDurum && aylikPuantajList.size() < personelList.size();
					if (tekrarOku) {
						aylikPuantajListClear();
						talepGunList.clear();

					}

				}
				if (!tekrarOku)
					++okumaAdet;

			}
			if (defaultAylikPuantajSablon == null)
				defaultAylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, departmanDenklestirmeDonemi, session);

			defaultAylikPuantajSablon.setVardiyalar(aylikSablonVardiyalar);
			setAylikPuantajDefault(defaultAylikPuantajSablon);
			bitTarih = null;
			resmiTatilVar = Boolean.FALSE;
			aksamGunVar = Boolean.FALSE;
			aksamSaatVar = Boolean.FALSE;
			haftaTatilVar = Boolean.FALSE;
			eksikMaasGoster = Boolean.FALSE;
			for (VardiyaGun pdksVardiyaGun : aylikPuantajDefault.getVardiyalar()) {
				if (pdksVardiyaGun.isAyinGunu()) {
					if (pdksVardiyaGun.getTatil() != null)
						resmiTatilVar = Boolean.TRUE;
				}

				else {
					if (pdksVardiyaGun.getVardiyaDate().before(aylikPuantajDefault.getIlkGun()))
						aylikPuantajDefault.setDenklestirmeGecenAy(denklestirmeGecenAy);
					else if (pdksVardiyaGun.getVardiyaDate().after(aylikPuantajDefault.getSonGun()))
						aylikPuantajDefault.setDenklestirmeGelecekAy(denklestirmeGelecekAy);
				}
			}
			if (gorevliPersonelMap != null && !gorevliPersonelMap.isEmpty()) {
				List<AylikPuantaj> helpAylikPuantajList = new ArrayList<AylikPuantaj>();

				for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
					AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
					if (!resmiTatilVar)
						resmiTatilVar = aylikPuantaj.getResmiTatilToplami() > 0d;
					if (!aksamGunVar)
						aksamGunVar = aylikPuantaj.getAksamVardiyaSayisi() > 0d;
					if (!aksamSaatVar)
						aksamSaatVar = aylikPuantaj.getAksamVardiyaSaatSayisi() > 0d;
					if (!haftaTatilVar)
						haftaTatilVar = aylikPuantaj.getHaftaCalismaSuresi() > 0d;
					if (!eksikMaasGoster)
						eksikMaasGoster = aylikPuantaj.getEksikCalismaSure() != 0d;

					if (helpPersonel(aylikPuantaj.getPdksPersonel())) {
						aylikPuantaj.setTrClass("help");
						helpAylikPuantajList.add(aylikPuantaj);
						iterator.remove();
					}

				}
				if (!helpAylikPuantajList.isEmpty())
					aylikPuantajList.addAll(helpAylikPuantajList);
				helpAylikPuantajList = null;
			}
			if (aylikPuantajList != null && loginUser.getLogin())
				aylikVardiyaOzetOlustur();
			List<AylikPuantaj> aylikPuantajAllList = new ArrayList<AylikPuantaj>();
			Long userId = loginUser.getPdksPersonel().getId();
			boolean kullaniciYonetici = loginUser.isYonetici() || loginUser.isSuperVisor() || loginUser.isProjeMuduru() || loginUser.isDirektorSuperVisor();
			calismaPlaniDenklestir(departmanDenklestirmeDonemi, aylikPuantajList, null);

			for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				if (!kullaniciYonetici || !aylikPuantaj.getPdksPersonel().getId().equals(userId)) {
					aylikPuantajAllList.add(aylikPuantaj);
					iterator.remove();
				}
				aylikPuantaj.setVardiyaOlustu(Boolean.FALSE);
			}
			if (!aylikPuantajAllList.isEmpty()) {
				aylikPuantajList.addAll(aylikPuantajAllList);
				List<String> list = new ArrayList<String>();
				for (Iterator iterator = aylikPuantajDefault.getAyinVardiyalari().iterator(); iterator.hasNext();) {
					VardiyaGun pdksVardiyaGun = (VardiyaGun) iterator.next();
					list.add(pdksVardiyaGun.getVardiyaDateStr());
				}
				devam = Boolean.TRUE;
				resmiTatilVar = Boolean.FALSE;
				aksamGunVar = Boolean.FALSE;
				aksamSaatVar = Boolean.FALSE;
				haftaTatilVar = Boolean.FALSE;
				eksikMaasGoster = Boolean.FALSE;
				cal = Calendar.getInstance();

				for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
					AylikPuantaj puantaj = (AylikPuantaj) iterator.next();

					try {
						if (!aksamGunVar)
							aksamGunVar = puantaj.getAksamVardiyaSayisi() > 0d;
						if (!aksamSaatVar)
							aksamSaatVar = puantaj.getAksamVardiyaSaatSayisi() > 0d;
						if (!haftaTatilVar)
							haftaTatilVar = puantaj.getHaftaCalismaSuresi() > 0d;
						if (!resmiTatilVar)
							resmiTatilVar = puantaj.getResmiTatilToplami() > 0d;
						if (!eksikMaasGoster)
							eksikMaasGoster = puantaj.getEksikCalismaSure() != 0d;
					} catch (Exception e) {
						logger.info(e);
					}

					if (puantaj.getVardiyalar() != null) {
						for (Iterator iterator2 = puantaj.getVardiyalar().iterator(); iterator2.hasNext();) {
							VardiyaGun pdksVardiyaGun = (VardiyaGun) iterator2.next();
							if (pdksVardiyaGun.isFazlaMesaiTalepDurum())
								setFazlaMesaiTalepVar(Boolean.TRUE);
							pdksVardiyaGun.setAyinGunu(list.contains(pdksVardiyaGun.getVardiyaDateStr()));
						}

					}
					if (!fazlaMesaiTalepVar && planGirisi)
						setFazlaMesaiTalepVar(puantaj.isFazlaMesaiTalepVar());
					if (!puantaj.getTrClass().equals("help")) {
						puantaj.setTrClass(devam ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
						devam = !devam;
					}

				}
				suaKontrol(aylikPuantajList);
				list = null;
			}
			ortakIslemler.yoneticiPuantajKontrol(loginUser, aylikPuantajList, Boolean.FALSE, session);
			if (denklestirmeAyDurum && !aylikPuantajList.isEmpty()) {
				try {
					setDenklestirmeAlanlari(aylikPuantajList);
				} catch (Exception e) {

				}

			}
			vardiyaFazlaMesaiMap.clear();
			if (fazlaMesaiTalepVar && aylikPuantajList != null && !aylikPuantajList.isEmpty())
				fazlaMesaiGunleriniBul(defaultAylikPuantajSablon.getIlkGun(), defaultAylikPuantajSablon.getSonGun(), aylikPuantajList);
			vardiyaFazlaMesaiTalepGoster = planGirisi && !vardiyaFazlaMesaiMap.isEmpty() && (loginUser.isAdmin() || PdksUtil.isSistemDestekVar());
			TreeMap<String, Object> ozetMap = fazlaMesaiOrtakIslemler.getIzinOzetMap(loginUser, null, aylikPuantajList, false);
			izinTipiVardiyaList = ozetMap.containsKey("izinTipiVardiyaList") ? (List<Vardiya>) ozetMap.get("izinTipiVardiyaList") : new ArrayList<Vardiya>();
			izinTipiPersonelVardiyaMap = ozetMap.containsKey("izinTipiPersonelVardiyaMap") ? (TreeMap<String, TreeMap<String, List<VardiyaGun>>>) ozetMap.get("izinTipiPersonelVardiyaMap") : new TreeMap<String, TreeMap<String, List<VardiyaGun>>>();
			if (izinTipiVardiyaList != null && !izinTipiVardiyaList.isEmpty()) {
				fazlaMesaiOrtakIslemler.personelIzinAdetleriOlustur(aylikPuantajList, izinTipiVardiyaList, izinTipiPersonelVardiyaMap);
			}
			if (fazlaMesaiTalepler == null)
				fazlaMesaiTalepler = new ArrayList<FazlaMesaiTalep>();
			else
				fazlaMesaiTalepler.clear();
			boolean vardiyaHareketKontrol = ortakIslemler.getParameterKey("vardiyaHareketKontrol").equals("1");
			if (vardiyaHareketKontrol && defaultAylikPuantajSablon != null && (loginUser.isAdmin()))
				vardiyaHareketKontrol(defaultAylikPuantajSablon);
			aylikPuantajAllList = null;
			if (fazlaMesaiTalepVar && denklestirmeAyDurum && aylikPuantajList.size() > 0)
				topluFazlaCalismaTalep = ortakIslemler.getParameterKey("topluFazlaCalismaTalep").equals("1") || (userHome != null && loginUser.getLogin() && userHome.hasPermission("vardiyaPlani", "topluFazlaCalismaTalep")) || loginUser.isAdmin();
			else
				topluFazlaCalismaTalep = false;

			fields.clear();

			HashMap<Long, AylikPuantaj> pdIdMap = new HashMap<Long, AylikPuantaj>();
			Sirket sirket = basliklariGuncelle(pdIdMap);
			List<Tanim> dinamikAlanList = ortakIslemler.dinamikAlanlariDoldur(pdIdMap, session);
			if (dinamikAlanlar == null)
				dinamikAlanlar = new ArrayList<Tanim>();

			for (Iterator iterator = dinamikAlanList.iterator(); iterator.hasNext();) {
				Tanim tanim = (Tanim) iterator.next();
				boolean ekle = true;
				for (Tanim tanim1 : dinamikAlanlar) {
					if (tanim1.getId().equals(tanim.getId()))
						ekle = false;
				}
				if (ekle)
					dinamikAlanlar.add(tanim);
			}
			if (denklestirmeAy.getBakiyeSifirlaDurum() == null || denklestirmeAy.getBakiyeSifirlaDurum() == false) {
				for (Iterator iterator = dinamikAlanlar.iterator(); iterator.hasNext();) {
					Tanim tanim = (Tanim) iterator.next();
					if (tanim.getKodu().equals(PersonelDenklestirmeDinamikAlan.TIPI_BAKIYE_SIFIRLA))
						iterator.remove();

				}
			}
			if (dinamikAlanlar.size() > 1)
				dinamikAlanlar = PdksUtil.sortTanimList(null, new ArrayList(dinamikAlanlar));
			int adet = 0;
			if (topluFazlaCalismaTalep) {
				topluFazlaCalismaTalep = false;
				// aylikPuantajDefault

				for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
					AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
					if (aylikPuantaj.getPersonelDenklestirme().isOnaylandi()) {
						boolean durum = aylikPuantaj.isFazlaMesaiDurum();
						if (durum) {
							++adet;
						}

					}

				}
			}
			topluFazlaCalismaTalep = adet > 1;
			if (topluFazlaCalismaTalep) {
				for (VardiyaGun vardiyaGun : aylikPuantajDefault.getAyinVardiyalari()) {
					vardiyaGun.setFazlaMesaiTalepDurum(talepGunList.contains(vardiyaGun.getVardiyaDateStr()));
				}
			}

			aylikHareketKaydiVardiyaBul = Boolean.FALSE;
			if (denklestirmeAyDurum)
				aylikHareketKaydiVardiyalariBul();
			if (adminRole || denklestirmeAyDurum) {

				if (sirket != null)
					bordroVeriOlusturBasla(sirket, aylikPuantajList);
			}
			if (!aylikPuantajList.isEmpty()) {
				ortakIslemler.sortAylikPuantajList(aylikPuantajList, true);

				modelGoster = ortakIslemler.getModelGoster(denklestirmeAy, session);
			}
		} catch (Exception es) {
			logger.error(es);
			ortakIslemler.setExceptionLog(null, es);
		}
		calismaPlanKilit = null;
		if (denklestirmeAy.getDurum() && ortakIslemler.getParameterKey("calismaPlanKilitKontrol").equals("1")) {
			if (!PdksUtil.hasStringValue(sicilNo)) {
				if (ikRole == false) {
					Long loginId = authenticatedUser.getPersonelId();
					for (AylikPuantaj ap : aylikPuantajList) {
						try {
							if (ap.getYonetici() != null && ap.getYonetici().getId() != null && ap.getYonetici().getId().equals(loginId)) {
								calismaPlanKilit = calismaPlanKilitGetir();
								break;
							}
						} catch (Exception e) {
							e.printStackTrace();
						}

					}
				}
				calismaPlanKilitKontrol();
			}
		}
		return kontrolDurum;
	}

	/**
	 * @param pdIdMap
	 * @return
	 */
	private Sirket basliklariGuncelle(HashMap<Long, AylikPuantaj> pdIdMap) {
		Sirket sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);
		topluFazlaCalismaTalep = false;
		fazlaMesaiIzinKullan = false;
		partTimeGoster = false;
		sutIzniGoster = false;
		isAramaGoster = false;
		fazlaMesaiTalepVar = false;

		boolean sirketFazlaMesaiOde = sirket != null && sirket.getFazlaMesaiOde() != null && sirket.getFazlaMesaiOde();
		for (Iterator iterator = aylikPuantajList.iterator(); iterator.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
			aylikPuantaj.setDinamikAlanMap(new TreeMap<Long, PersonelDenklestirmeDinamikAlan>());
			PersonelDenklestirme pd = aylikPuantaj.getPersonelDenklestirme();
			if (aylikPuantaj.isSutIzniDurumu() == false && pd.getSutIzniPersonelDonemselDurum() != null) {
				sutIzniGoster = true;
				aylikPuantaj.setSutIzniDurumu(true);
			}
			if (aylikPuantaj.isGebeDurum() == false && pd.getGebePersonelDonemselDurum() != null) {
				gebeGoster = true;
				aylikPuantaj.setGebeDurum(true);
			}
			if (!fazlaMesaiTalepVar && aylikPuantaj.getVardiyalar() != null) {
				for (VardiyaGun vardiyaGun : aylikPuantaj.getVardiyalar()) {
					if (vardiyaGun.getId() != null) {
						if (vardiyaGun.isFazlaMesaiTalepDurum() && vardiyaGun.isAyinGunu()) {
							setFazlaMesaiTalepVar(true);
							break;
						}

					}

				}
			}
			if (aylikPuantaj.getIsAramaDurum().booleanValue() == false && pd.getIsAramaPersonelDonemselDurum() != null) {
				isAramaGoster = true;
				aylikPuantaj.setIsAramaDurum(true);
			}

			if (!gebeGoster)
				gebeGoster = aylikPuantaj.isGebeDurum();
			if (pdIdMap != null)
				pdIdMap.put(pd.getId(), aylikPuantaj);
			if (topluFazlaCalismaTalep)
				topluFazlaCalismaTalep = pd.getPersonel().getSirket().isFazlaMesaiTalepGirer();
			if (fazlaMesaiTalepVar)
				setFazlaMesaiTalepVar(pd.getPersonel().getSirket().isFazlaMesaiTalepGirer());
			if (!sutIzniGoster)
				sutIzniGoster = (pd.getSutIzniDurum() != null && pd.getSutIzniDurum());
			if (!partTimeGoster)
				partTimeGoster = pd.getPartTime() != null && pd.getPartTime();

			if (!fazlaMesaiIzinKullan)
				fazlaMesaiIzinKullan = pd.getFazlaMesaiIzinKullan() != null && pd.getFazlaMesaiIzinKullan();
			if (!fazlaMesaiOde)
				fazlaMesaiOde = pd.getFazlaMesaiOde() != null && !pd.getFazlaMesaiOde().equals(sirketFazlaMesaiOde);
		}
		return sirket;
	}

	/**
	 * @param denklestirmeDonemi
	 * @param puantajList
	 * @param puantaj
	 */
	private void calismaPlaniDenklestir(DepartmanDenklestirmeDonemi donemi, List<AylikPuantaj> puantajList, AylikPuantaj puantaj) {
		if (puantaj != null) {
			if (puantajList == null)
				puantajList = new ArrayList<AylikPuantaj>();
			puantajList.add(puantaj);
		}

		if (puantajList != null && !puantajList.isEmpty()) {
			LinkedHashMap<String, Object> dataMap = new LinkedHashMap<String, Object>();
			dataMap.put("aylikPuantajList", puantajList);
			dataMap.put("manuelGirisKapi", manuelGiris);
			dataMap.put("manuelCikisKapi", manuelCikis);
			dataMap.put("basTarih", donemi.getBaslangicTarih());
			dataMap.put("bitTarih", donemi.getBitisTarih());
			dataMap.put("normalCalismaVardiya", getNormalCalismaVardiya());
			dataMap.put("denklestirmeAyDurum", denklestirmeAyDurum);
			dataMap.put("tatilGunleriMap", tatilGunleriMap);
			fazlaMesaiOrtakIslemler.calismaPlaniDenklestir(dataMap, session);
		}

	}

	/**
	 * @param kilit
	 * @return
	 */
	public String calismaPlanSorumluMailGonder(CalismaPlanKilit kilit) {

		if (toList == null)
			toList = new ArrayList<User>();
		else
			toList.clear();

		if (ccList == null)
			ccList = new ArrayList<User>();
		else
			ccList.clear();
		if (kilit.getTalepKontrol())
			ccList = ortakIslemler.IKKullanicilariBul(toList, null, session);
		String bolumAdi = PdksUtil.getSelectItemLabel(aramaSecenekleri.getEkSaha3Id(), aramaSecenekleri.getGorevYeriList());
		toList.add(kilit.getOlusturanUser());
		MailStatu mailSatu = null;
		try {
			MailObject mail = new MailObject();
			mail.setSubject(denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil() + " " + bolumAdi + " çalışma plan durumu");
			StringBuffer body = new StringBuffer();
			body.append("<p>");
			body.append(denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil() + " " + bolumAdi);
			CalismaPlanKilitTalep talep = kilit.getTalep();
			if (talep == null) {
				body.append("  çalışma planı kiliti açılmıştır.");
			} else {
				if (talep.getOnayDurum())
					body.append("  çalışma planı kiliti açılmıştır.");
				else
					body.append("  çalışma planı kiliti açılmayacaktır.");
			}
			body.append("</p>");
			mail.setBody(body.toString());
			body = null;
			if (!ccList.isEmpty())
				ortakIslemler.addMailPersonelUserList(ccList, mail.getCcList());
			ortakIslemler.addMailPersonelUserList(toList, mail.getToList());
			mailSatu = ortakIslemler.mailSoapServisGonder(true, mail, renderer, "/email/fazlaMesaiTalepMail.xhtml", session);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
			PdksUtil.addMessageError(e.getMessage());
		}
		if (mailSatu != null && mailSatu.getDurum()) {
			if (authenticatedUser != null)
				PdksUtil.addMessageAvailableInfo("Bilgilendirme maili gönderildi.");
		}

		return "";
	}

	/**
	 * 
	 */
	private void calismaPlanKilitKontrol() {
		boolean onayla = false;
		if (calismaPlanKilit != null) {
			if (calismaPlanKilit.getId() == null) {
				calismaPlanKilit.setOlusturanUser(authenticatedUser);
				calismaPlanKilit.setOlusturmaTarihi(new Date());
				saveOrUpdate(calismaPlanKilit);
				sessionFlush();
				kullaniciPersonel = false;
			} else {
				for (AylikPuantaj ap : aylikPuantajList) {
					ap.setOnayDurum(ap.getPersonelDenklestirme().isOnaylandi() == false);
					if (ap.getPersonelDenklestirme().isOnaylandi() == false) {
						onayla = true;
					}
				}

				if (ikRole == false) {
					if (onayla == false && calismaPlanKilit.getKilitDurum().booleanValue() == false && calismaPlanKilit.getGuncelleyenUser() == null) {
						calismaPlanKilit.setGuncelleyenUser(authenticatedUser);
						calismaPlanKilit.setGuncellemeTarihi(new Date());
						calismaPlanKilit.setKilitDurum(Boolean.TRUE);
						saveOrUpdate(calismaPlanKilit);
						sessionFlush();
					}
					kullaniciPersonel = calismaPlanKilit.getKilitDurum() || !onayla;
				} else
					kullaniciPersonel = !onayla;
			}
			if (ikRole) {
				calismaPlanKilit.setTalep(null);
				if (calismaPlanKilit.getId() == null || calismaPlanKilit.getKilitDurum().booleanValue() == false)
					calismaPlanKilit = null;
				else {
					kullaniciPersonel = onayla == false;
					if (calismaPlanKilit.getTalepKontrol()) {

						List<CalismaPlanKilitTalep> list = pdksEntityController.getSQLParamByFieldList(CalismaPlanKilitTalep.TABLE_NAME, CalismaPlanKilitTalep.COLUMN_NAME_CALISMA_PLAN_KILIT, calismaPlanKilit.getId(), CalismaPlanKilitTalep.class, session);

						for (Iterator iterator = list.iterator(); iterator.hasNext();) {
							CalismaPlanKilitTalep talep = (CalismaPlanKilitTalep) iterator.next();
							if (talep.getDurum() && talep.getGuncelleyenUser() == null)
								calismaPlanKilit.setTalep(talep);

						}

					}
				}
			}
		}
		if (kilitliPlanList == null)
			kilitliPlanList = new ArrayList<CalismaPlanKilit>();
		else
			kilitliPlanList.clear();
		if (ikRole) {
			HashMap fields = new HashMap();
			fields.put("denklestirmeAy.id", denklestirmeAy.getId());
			fields.put("sirketId", aramaSecenekleri.getSirketId());
			if (aramaSecenekleri.getTesisId() != null)
				fields.put("tesisId", aramaSecenekleri.getTesisId());
			fields.put("bolumId", aramaSecenekleri.getEkSaha3Id());
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<CalismaPlanKilit> list = pdksEntityController.getObjectByInnerObjectList(fields, CalismaPlanKilit.class);
			List<Long> kilitIdList = new ArrayList<Long>();
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				CalismaPlanKilit cpk = (CalismaPlanKilit) iterator.next();
				if (cpk.getKilitDurum().booleanValue() == false)
					iterator.remove();
				else if (cpk.getTalepKontrol()) {
					kilitIdList.add(cpk.getId());
					iterator.remove();
				} else
					kilitliPlanList.add(cpk);

			}
			list = null;
			if (!kilitIdList.isEmpty()) {

				List<CalismaPlanKilitTalep> talepList = pdksEntityController.getSQLParamByFieldList(CalismaPlanKilitTalep.TABLE_NAME, CalismaPlanKilitTalep.COLUMN_NAME_CALISMA_PLAN_KILIT, kilitIdList, CalismaPlanKilitTalep.class, session);

				for (CalismaPlanKilitTalep cpkt : talepList) {
					if (cpkt.getOnayDurum() == null) {
						CalismaPlanKilit cpk = cpkt.getCalismaPlanKilit();
						cpk.setTalep(cpkt);
						kilitliPlanList.add(cpk);
					}

				}
				talepList = null;

			}
			kilitIdList = null;
		}
	}

	/**
	 * @return
	 */
	private CalismaPlanKilit calismaPlanKilitGetir() {
		CalismaPlanKilit cpk = null;

		HashMap parametreMap = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select TOP 1 U.* from " + CalismaPlanKilit.TABLE_NAME + " U " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" where U." + CalismaPlanKilit.COLUMN_NAME_DONEM + " = :d and U." + CalismaPlanKilit.COLUMN_NAME_SIRKET + " = :s");
		if (aramaSecenekleri.getTesisId() != null) {
			sb.append(" and U." + CalismaPlanKilit.COLUMN_NAME_TESIS + " = :t");
			parametreMap.put("t", aramaSecenekleri.getTesisId());
		}
		sb.append(" and U." + CalismaPlanKilit.COLUMN_NAME_BOLUM + " = :b and U." + CalismaPlanKilit.COLUMN_NAME_OLUSTURMA_TARIHI + " = :o");
		parametreMap.put("d", denklestirmeAy.getId());
		parametreMap.put("s", aramaSecenekleri.getSirketId());
		parametreMap.put("b", aramaSecenekleri.getEkSaha3Id());
		parametreMap.put("o", authenticatedUser.getId());
		if (session != null)
			parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<CalismaPlanKilit> cpkList = pdksEntityController.getObjectBySQLList(sb, parametreMap, CalismaPlanKilit.class);

		cpk = cpkList != null && !cpkList.isEmpty() ? cpkList.get(0) : null;

		if (cpk == null) {
			cpk = new CalismaPlanKilit(new Sirket(aramaSecenekleri.getSirketId()), aramaSecenekleri.getTesisId(), aramaSecenekleri.getEkSaha3Id(), denklestirmeAy);
			cpk.setOlusturanUser(authenticatedUser);
		} else if (cpk.getTalepKontrol()) {
			CalismaPlanKilitTalep talepYeni = null;

			List<CalismaPlanKilitTalep> list = pdksEntityController.getSQLParamByFieldList(CalismaPlanKilitTalep.TABLE_NAME, CalismaPlanKilitTalep.COLUMN_NAME_CALISMA_PLAN_KILIT, cpk.getId(), CalismaPlanKilitTalep.class, session);
			for (CalismaPlanKilitTalep talep : list) {
				if (talep.getOnayDurum() == null)
					talepYeni = talep;
			}
			if (talepYeni == null) {
				talepYeni = new CalismaPlanKilitTalep();
				talepYeni.setCalismaPlanKilit(cpk);
				cpk.setTalep(talepYeni);
			}
			cpk.setTalep(talepYeni);
			list = null;
		}

		return cpk;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String seciliPuantajSutIzniGuncelle() throws Exception {
		try {
			Double sutIzniSaatSayisi = sutIzniGuncelle(personelAylikPuantaj, personelSutIzniDurum);
			personelDenklestirme.setGuncellendi(true);
			personelDenklestirme.setSutIzniSaatSayisi(sutIzniSaatSayisi);
		} catch (Exception e) {

		}
		return "";
	}

	/**
	 * @param puantaj
	 * @param pdd
	 * @return
	 */
	private Double sutIzniGuncelle(AylikPuantaj puantaj, PersonelDonemselDurum pdd) throws Exception {
		PersonelDenklestirme pd = puantaj.getPersonelDenklestirme();
		Double toplamIzinSure = null;
		CalismaModeliAy calismaModeliAy = pd.getCalismaModeliAy();
		if (calismaModeliAy != null) {
			String donemStr = String.valueOf(yil * 100 + ay);
			List<String> gunler = new ArrayList<String>();
			if (puantaj.getVardiyalar() != null) {
				for (VardiyaGun vg : puantaj.getVardiyalar())
					if (vg.getVardiyaDateStr().startsWith(donemStr) && vg.getVardiya() != null)
						gunler.add(vg.getVardiyaDateStr());
			}
			CalismaModeli cm = calismaModeliAy.getCalismaModeli();
			double sure = 0.0d, sutIzinSure = 0.0d;
			Calendar cal = Calendar.getInstance();
			for (VardiyaGun vg : defaultAylikPuantajSablon.getVardiyalar()) {
				if (vg.isAyinGunu() && gunler.contains(vg.getVardiyaDateStr())) {
					cal.setTime(vg.getVardiyaDate());
					int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
					if (dayOfWeek != Calendar.SUNDAY) {
						boolean sutIzniYok = vg.getVardiyaDate().before(pdd.getBasTarih()) || vg.getVardiyaDate().after(pdd.getBitTarih());
						if (vg.getTatil() == null) {
							double gunSure = cm.getSaat(dayOfWeek);
							if (sutIzniYok == false)
								sutIzinSure += gunSure > 7.5d ? 7.5d : gunSure;
							else
								sure += gunSure;
						} else if (vg.getTatil().isYarimGunMu()) {
							if (PdksUtil.tarihKarsilastirNumeric(vg.getVardiyaDate(), vg.getTatil().getBasTarih()) == 0) {
								if (sutIzniYok == false)
									sutIzinSure += cm.getArife();
								else if (cm.isHaftaTatilVar() || vg.isHaftaIci())
									sure += cm.getArife();
							}
						}
						logger.debug(vg.getVardiyaDateStr() + " " + sure + " " + sutIzinSure);
					}
				}
			}
			gunler = null;
			if (sure + sutIzinSure > 0)
				toplamIzinSure = sure + sutIzinSure;
		}
		return toplamIzinSure;
	}

	/**
	 * @param sirket
	 * @param puantajList
	 */
	private void bordroVeriOlusturBasla(Sirket sirket, List<AylikPuantaj> puantajList) {
		if (sirket != null && ortakIslemler.getParameterKeyHasStringValue("bordroVeriOlustur")) {
			try {
				String str = ortakIslemler.getParameterKey("bordroVeriOlustur");
				int donem = yil * 100 + ay;
				if (donem >= Integer.parseInt(str))
					baslikMap = fazlaMesaiOrtakIslemler.bordroVeriOlustur(false, puantajList, denklestirmeAyDurum, String.valueOf(donem), authenticatedUser, session);
			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		}
		boolean saatlikCalismaVar = ortakIslemler.getParameterKey("saatlikCalismaVar").equals("1");
		gerceklesenMesaiKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.gerceklesenMesaiKod());
		devredenBakiyeKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.devredenBakiyeKod());
		devredenMesaiKod = saatlikCalismaVar == false || bordroPuantajEkranindaGoster == false || baslikMap.containsKey(ortakIslemler.devredenMesaiKod());
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
	 * @param puantajList
	 * @throws Exception
	 */
	@Transactional
	private void setDenklestirmeAlanlari(List<AylikPuantaj> puantajList) throws Exception {

		TreeMap<Long, PersonelDenklestirme> map = new TreeMap<Long, PersonelDenklestirme>();
		StringBuffer pdIdSb = new StringBuffer();
		for (Iterator iterator = puantajList.iterator(); iterator.hasNext();) {
			AylikPuantaj ap = (AylikPuantaj) iterator.next();
			PersonelDenklestirme pd = ap.getPersonelDenklestirme();
			map.put(pd.getId(), pd);
			pdIdSb.append(pd.getId() + "");
			if (iterator.hasNext())
				pdIdSb.append(",");
		}

		StringBuffer sb = new StringBuffer();
		sb.append("SP_GET_PERS_DENK_DINAMIK_ALAN");
		LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
		fields.put("pdId", pdIdSb.toString());
		fields.put("durum", 1);
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		List list = pdksEntityController.execSPList(fields, sb, null);
		pdIdSb = null;
		sb = null;
		List<Long> tanimIdList = new ArrayList<Long>();
		HashMap<Long, Boolean> secimDurumMap = new HashMap<Long, Boolean>();
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			Object[] object = (Object[]) iterator.next();
			if (object != null) {
				if (object[2] != null) {
					boolean durum = false;
					if (object.length == 4 && object[3] != null) {
						Integer durumSecim = (Integer) object[3];
						durum = durumSecim == 1;
						secimDurumMap.put(((BigDecimal) object[2]).longValue(), durum);
					}
				}
				if (object[1] != null)
					tanimIdList.add(((BigDecimal) object[1]).longValue());

			}
		}
		if (dinamikAlanlar != null)
			dinamikAlanlar.clear();
		if (!tanimIdList.isEmpty()) {
			TreeMap<Long, Tanim> tanimMap = pdksEntityController.getSQLParamByFieldMap(Tanim.TABLE_NAME, Tanim.COLUMN_NAME_ID, tanimIdList, Tanim.class, "getId", false, session);
			dinamikAlanlar = new ArrayList(tanimMap.values());
			boolean flush = false;
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Object[] object = (Object[]) iterator.next();
				if (object != null) {
					Long pdId = ((BigDecimal) object[2]).longValue(), alanId = ((BigDecimal) object[1]).longValue();
					Tanim tanim = tanimMap.get(alanId);
					PersonelDenklestirme personelDenklestirme = map.get(pdId);
					PersonelDenklestirmeDinamikAlan pdda = new PersonelDenklestirmeDinamikAlan(personelDenklestirme, tanim);
					if (secimDurumMap.containsKey(personelDenklestirme.getId())) {
						pdda.setIslemDurum(secimDurumMap.get(personelDenklestirme.getId()));
						pdda.setDurum(Boolean.TRUE);
						saveOrUpdate(pdda);
						flush = true;
					}

				}
			}
			if (flush)
				sessionFlush();

		}
		secimDurumMap = null;
		list = null;

		tanimIdList = null;

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
	 * @param t1
	 * @param t2
	 * @param list
	 */
	private void fazlaMesaiGunleriniBul(Date t1, Date t2, List<AylikPuantaj> list) {
		List<Long> perIdList = new ArrayList<Long>();
		for (AylikPuantaj aylikPuantaj : list) {
			if (aylikPuantaj.getPdksPersonel() != null)
				perIdList.add(aylikPuantaj.getPdksPersonel().getId());
		}
		if (!perIdList.isEmpty()) {
			HashMap map = new HashMap();
			String fieldName = "p";
			StringBuffer sb = new StringBuffer();
			sb.append("select F.* from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " F " + PdksEntityController.getJoinLOCK() + " on F." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V." + VardiyaGun.COLUMN_NAME_ID);
			sb.append(" and F." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1 and F." + FazlaMesaiTalep.COLUMN_NAME_ONAY_DURUMU + " <> " + FazlaMesaiTalep.ONAY_DURUM_RED);
			sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :t1 and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= :t2  ");
			sb.append(" and V." + VardiyaGun.COLUMN_NAME_PERSONEL + " :" + fieldName);
			sb.append(" order by F." + FazlaMesaiTalep.COLUMN_NAME_BASLANGIC_ZAMANI);
			map.put(fieldName, perIdList);
			map.put("t1", t1);
			map.put("t2", t2);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			// fazlaMesaiTalepler = pdksEntityController.getObjectBySQLList(sb, map, FazlaMesaiTalep.class);
			fazlaMesaiTalepler = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, map, FazlaMesaiTalep.class, session);

			for (FazlaMesaiTalep fmt : fazlaMesaiTalepler) {
				Long key = fmt.getVardiyaGun().getId();
				List<FazlaMesaiTalep> ftmList = vardiyaFazlaMesaiMap.containsKey(key) ? vardiyaFazlaMesaiMap.get(key) : new ArrayList<FazlaMesaiTalep>();
				if (ftmList.isEmpty())
					vardiyaFazlaMesaiMap.put(key, ftmList);
				ftmList.add(fmt);
			}

		}
		perIdList = null;
	}

	/**
	 * @param aylikPuantajSablon
	 */
	private void vardiyaHareketKontrol(AylikPuantaj aylikPuantajSablon) {
		if (denklestirmeAy != null && denklestirmeAyDurum && aylikPuantajList != null && aylikPuantajList.size() > 0) {
			TreeMap<Long, AylikPuantaj> personelIdMap = new TreeMap<Long, AylikPuantaj>();
			for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
				Personel personel = aylikPuantaj.getPdksPersonel();
				personelIdMap.put(personel.getPersonelKGS().getId(), aylikPuantaj);
			}

			List<Long> kapiIdler = ortakIslemler.getPdksKapiIdler(session, Boolean.TRUE);
			TreeMap<Long, TreeMap<String, List<HareketKGS>>> map = new TreeMap<Long, TreeMap<String, List<HareketKGS>>>();
			try {
				List<HareketKGS> kgsList = ortakIslemler.getHareketBilgileri(kapiIdler, new ArrayList<Long>(personelIdMap.keySet()), aylikPuantajSablon.getIlkGun(), aylikPuantajSablon.getSonGun(), HareketKGS.class, session);
				if (!kgsList.isEmpty()) {
					for (HareketKGS hareket : kgsList) {
						if (hareket.getDurum() != 0)
							continue;

						Long perId = hareket.getPersonelId();
						TreeMap<String, List<HareketKGS>> map1 = map.containsKey(perId) ? map.get(perId) : new TreeMap<String, List<HareketKGS>>();
						if (map1.isEmpty())
							map.put(perId, map1);
						String tarihStr = PdksUtil.convertToDateString(hareket.getZaman(), "yyyyMMdd");
						List<HareketKGS> list = map1.containsKey(tarihStr) ? map1.get(tarihStr) : new ArrayList<HareketKGS>();
						if (list.isEmpty())
							map1.put(tarihStr, list);
						list.add(hareket);
					}
					if (!map.isEmpty()) {
						for (Long perId : map.keySet()) {
							AylikPuantaj aylikPuantaj = personelIdMap.get(perId);
							TreeMap<String, List<HareketKGS>> map1 = map.get(perId);
							// PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
							// CalismaModeli calismaModeli = personelDenklestirme.getCalismaModeli();
							for (VardiyaGun vardiyaGun : aylikPuantaj.getAyinVardiyalari()) {
								String tarihStr = vardiyaGun.getVardiyaDateStr();
								List<HareketKGS> orjinalList = new ArrayList<HareketKGS>(map1.get(tarihStr));
								vardiyaGun.setYeniVardiya(null);
								map1.remove(tarihStr);
								if (vardiyaGun.getVardiya().isHaftaTatil() && orjinalList.size() > 1)
									continue;
								if (vardiyaGun.isAyinGunu() && vardiyaGun.isCalisan() && vardiyaGun.getIzin() == null) {
									for (Vardiya vardiya : vardiyaGun.getVardiyalar()) {
										if (vardiya.isCalisma()) {
											VardiyaGun vardiyaYeniGun = new VardiyaGun(vardiyaGun.getPersonel(), vardiya, vardiyaGun.getVardiyaDate());
											List<HareketKGS> list = new ArrayList<HareketKGS>(orjinalList);
											vardiyaYeniGun.setVardiyaZamani();
											Vardiya islemVardiya = vardiyaYeniGun.getIslemVardiya();
											Date basZaman = islemVardiya.getVardiyaTelorans1BasZaman(), bitZaman = islemVardiya.getVardiyaTelorans2BitZaman();
											for (Iterator iterator = list.iterator(); iterator.hasNext();) {
												HareketKGS hareket = (HareketKGS) iterator.next();
												if (hareket.getZaman().after(bitZaman) || hareket.getZaman().before(basZaman))
													continue;
												if (vardiyaYeniGun.addHareket((HareketKGS) hareket.clone(), Boolean.TRUE))
													iterator.remove();
											}
											if (list.isEmpty()) {
												if (vardiyaYeniGun.getGirisHareketleri() != null && vardiyaYeniGun.getCikisHareketleri() != null && vardiyaYeniGun.getGirisHareketleri().size() == vardiyaYeniGun.getCikisHareketleri().size()) {
													if (!vardiya.getId().equals(vardiyaGun.getVardiya().getId())) {
														vardiyaGun.setYeniVardiya(vardiya);
														logger.info(vardiyaYeniGun.getVardiyaKeyStr() + " " + vardiya.getVardiyaAdi());
													}
													break;
												}

											}

										}

									}

								}

							}
							map1 = null;

						}
					}
				}
			} catch (Exception e) {

			}

		}
	}

	/**
	 * @param list
	 */
	private void suaKontrol(List<AylikPuantaj> list) {
		if (list != null && departman != null && departman.isAdminMi()) {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();
				if (aylikPuantaj.getPersonelDenklestirme().getSuaDurum() != null && aylikPuantaj.getPersonelDenklestirme().getSuaDurum()) {
					aylikPuantaj.setVardiyaSua(Boolean.TRUE);
					continue;
				}

				boolean vardiyaSua = Boolean.FALSE;
				if (aylikPuantaj.getVardiyalar() != null) {

					for (Iterator iterator2 = aylikPuantaj.getVardiyalar().iterator(); iterator2.hasNext();) {
						VardiyaGun pdksVardiyaGun = (VardiyaGun) iterator2.next();
						if (pdksVardiyaGun.isAyinGunu() && pdksVardiyaGun.getVardiya() != null && pdksVardiyaGun.getVardiya() != null) {
							if ((pdksVardiyaGun.getVardiya().getSua() != null && pdksVardiyaGun.getVardiya().getSua()) || (pdksVardiyaGun.getIzin() != null && pdksVardiyaGun.getIzin().getIzinTipi().isSuaIzin())) {
								vardiyaSua = Boolean.TRUE;
								break;
							}

						}

					}
					if (vardiyaSua)
						PdksUtil.addMessageAvailableWarn(aylikPuantaj.getPdksPersonel().getPdksSicilNo() + " " + aylikPuantaj.getPdksPersonel().getAdSoyad() + " şua izni veya vardiyası vardır!");

				}
				aylikPuantaj.setVardiyaSua(vardiyaSua);
			}
		}
	}

	/**
	 * @param denklestirmeAy
	 * @param idler
	 * @return
	 */
	private TreeMap<Long, PersonelDenklestirme> getPersonelDenklestirme(DenklestirmeAy denklestirmeAy, ArrayList<Long> idler) {
		String fieldName = "p";
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("select S.* from " + PersonelDenklestirme.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" where S." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId() + " and S." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
		fields.put(fieldName, idler);
		// fields.put(PdksEntityController.MAP_KEY_MAP, "getPersonelId");
		if (session != null)
			fields.put(PdksEntityController.MAP_KEY_SESSION, session);
		TreeMap<Long, PersonelDenklestirme> denklestirmeMap = new TreeMap<Long, PersonelDenklestirme>();
		List<PersonelDenklestirme> list = pdksEntityController.getSQLParamList(idler, sb, fieldName, fields, PersonelDenklestirme.class, session);
		for (PersonelDenklestirme pd : list) {
			pd.setGuncellendi(Boolean.FALSE);
			denklestirmeMap.put(pd.getPersonelId(), pd);
		}
		if (denklestirmeAy.getBakiyeSifirlaDurum())
			ortakIslemler.setBakiyeSifirlaDurum(list, session);

		list = null;
		sb = null;
		fields = null;
		return denklestirmeMap;
	}

	/**
	 * @return
	 */
	public String fillPersonelSicilAylikPlanRaporList() {
		if (!PdksUtil.hasStringValue(sicilNo))
			aylikPuantajListClear();
		else {
			sicilNo = ortakIslemler.getSicilNo(sicilNo);
			try {
				fillAylikPlanRaporList();
			} catch (Exception e) {
				logger.equals(e);
				e.printStackTrace();
			}

		}

		return "";
	}

	/**
	 * @return
	 */
	public String fillAylikPlanRaporList() {
		if (!islemYapiliyor) {
			try {
				islemYapiliyor = Boolean.TRUE;
				aylikPuantajOlusturuluyor();
			} catch (Exception ee) {
				logger.info(ee.getMessage());
				ee.printStackTrace();
			} finally {
				islemYapiliyor = Boolean.FALSE;
			}
		}
		return "";

	}

	/**
	 * @param gorevYeriAciklama
	 * @param puantajList
	 * @param sonucGoster
	 * @return
	 */
	public ByteArrayOutputStream excelAylikMesaiTalepDevam() {
		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, "Fazla Mesai Talep", Boolean.TRUE);
		CellStyle header = ExcelUtil.getStyleHeader(wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddTutar = ExcelUtil.setAlignment(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleOddNumber = ExcelUtil.setAlignment(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_NUMBER, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleOddDateTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenTutar = ExcelUtil.setAlignment(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleEvenNumber = ExcelUtil.setAlignment(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_NUMBER, wb), CellStyle.ALIGN_CENTER);
		CellStyle styleEvenDateTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);

		int row = 0;
		int col = 0;
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adı Soyadı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.sirketAciklama());
		boolean tesisDurum = ortakIslemler.getListTesisDurum(aylikFazlaMesaiTalepler);
		if (tesisDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.tesisAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(bolumAciklama);
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Başlangıç Zamanı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Bitiş Zamanı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Süresi (Saat)");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Nedeni");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay Durum");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay'a Gönderen");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay'a Gönderme Zamanı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onaylayan");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onaylama Zamanı");
		boolean renk = true;
		for (Iterator iter = aylikFazlaMesaiTalepler.iterator(); iter.hasNext();) {
			FazlaMesaiTalep fmt = (FazlaMesaiTalep) iter.next();
			Personel personel = fmt.getVardiyaGun().getPersonel();
			Sirket sirket = personel.getSirket();
			++row;
			col = 0;
			CellStyle style = null, styleCenter = null, styleTutar = null, styleNumber = null, styleDateTime = null;
			if (renk) {
				styleDateTime = styleOddDateTime;
				styleNumber = styleOddNumber;
				styleTutar = styleOddTutar;
				style = styleOdd;
				styleCenter = styleOddCenter;
			} else {
				styleDateTime = styleEvenDateTime;
				styleNumber = styleEvenNumber;
				styleTutar = styleEvenTutar;
				style = styleEven;
				styleCenter = styleEvenCenter;
			}
			renk = !renk;
			ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSicilNo());
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAdSoyad());
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket.getAd());
			if (tesisDurum)
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getTesis() != null ? personel.getTesis().getAciklama() : "");
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "");
			ExcelUtil.getCell(sheet, row, col++, styleDateTime).setCellValue(fmt.getBaslangicZamani());
			ExcelUtil.getCell(sheet, row, col++, styleDateTime).setCellValue(fmt.getBitisZamani());
			Double sure = fmt.getMesaiSuresi();
			ExcelUtil.getCell(sheet, row, col++, PdksUtil.isDoubleValueNotLong(sure) ? styleTutar : styleNumber).setCellValue(sure);
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(fmt.getMesaiNeden() != null ? fmt.getMesaiNeden().getAciklama() : "");
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(fmt.getOnayDurumAciklama());
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(fmt.getOlusturanUser() != null ? fmt.getOlusturanUser().getAdSoyad() : "");
			if (fmt.getOlusturmaTarihi() != null)
				ExcelUtil.getCell(sheet, row, col++, styleDateTime).setCellValue(fmt.getOlusturmaTarihi());
			else
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");

			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(fmt.getGuncelleyenUser() != null ? fmt.getGuncelleyenUser().getAdSoyad() : "");
			if (fmt.getGuncellemeTarihi() != null)
				ExcelUtil.getCell(sheet, row, col++, styleEvenDateTime).setCellValue(fmt.getGuncellemeTarihi());
			else
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");

		}

		try {
			// double katsayi = 3.43;
			// int[] dizi = new int[] { 1575, 1056, 2011, 2056, 1575, 3722,
			// 1575, 2078, 2600, 2056, 3722, 2078, 2600, 2056, 3722, 2078, 2600,
			// 2056, 3722, 2078, 2600, 3722, 2078, 2600, 3722, 2078, 2600 };
			for (int i = 0; i < col; i++)
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
	 * @return
	 * @throws Exception
	 */
	public String excelAylikMesaiTalepList() throws Exception {

		try {

			baosDosya = excelAylikMesaiTalepDevam();
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "FazlaMesaiTalepListesi.xlsx");

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}

		return "";

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String fillAylikMesaiTalepList() throws Exception {
		sessionClear();
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		veriMap.put("yil", yil);
		veriMap.put("ay", ay);
		Long perId = authenticatedUser.getPdksPersonel().getId();
		if (ikRole)
			veriMap.put("yoneticiId", 0L);
		else
			veriMap.put("yoneticiId", perId);
		veriMap.put("basTarih", basTarihStr != null ? PdksUtil.convertToDateString(PdksUtil.convertToJavaDate(basTarihStr, PdksUtil.getDateFormat()), "yyyy-MM-dd") : "");
		veriMap.put("bitTarih", bitTarihStr != null ? PdksUtil.convertToDateString(PdksUtil.convertToJavaDate(bitTarihStr, PdksUtil.getDateFormat()), "yyyy-MM-dd") : "");
		if (session != null)
			veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
		StringBuffer sb = new StringBuffer("SP_FAZLA_MESAI_TALEP_MAIL");
		aylikFazlaMesaiTalepler = pdksEntityController.execSPList(veriMap, sb, FazlaMesaiTalep.class);
		islemYapiliyor = null;
		if (!aylikFazlaMesaiTalepler.isEmpty()) {
			HashMap fields = new HashMap();
			fields.put("yil", yil);
			fields.put("ay", ay);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			setAylikPuantajDonem(denklestirmeAy);
			setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
			List<FazlaMesaiTalep> list = new ArrayList<FazlaMesaiTalep>();
			islemYapiliyor = false;
			for (Iterator iterator = aylikFazlaMesaiTalepler.iterator(); iterator.hasNext();) {
				FazlaMesaiTalep fmt = (FazlaMesaiTalep) iterator.next();
				fmt.setCheckBoxDurum(false);
				boolean islemYapildi = false;
				if (fmt.getVardiyaGun().getPersonel().getPdksYonetici() != null && fmt.getVardiyaGun().getPersonel().getPdksYonetici().getPdksYonetici() != null)
					islemYapildi = fmt.getVardiyaGun().getPersonel().getPdksYonetici().getPdksYonetici().getId().equals(perId);
				if (!islemYapiliyor && islemYapildi)
					islemYapiliyor = fmt.isHatirlatmaMail();
				fmt.setIslemYapildi(islemYapildi);
				if (!islemYapildi) {
					list.add(fmt);
					fmt.setIslemYapildi(false);
					iterator.remove();
				}

			}
			if (!islemYapiliyor || denklestirmeAyDurum == false)
				islemYapiliyor = null;
			else
				islemYapiliyor = false;
			if (!list.isEmpty())
				aylikFazlaMesaiTalepler.addAll(list);
			list = null;

		}
		return "";

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String fillStartAylikVardiyaPlanList() throws Exception {
		if (adminRole || ikRole)
			sessionClear();
		fillAylikVardiyaPlanList();
		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String fillAylikVardiyaPlanList() throws Exception {
		veriGuncellendi = Boolean.FALSE;
		haftaTatilMesaiDurum = Boolean.FALSE;
		if (!islemYapiliyor) {
			islemYapiliyor = Boolean.TRUE;
			try {
				if (aylikPuantajOlusturuluyor())
					aylikPuantajOlusturuluyor();
				islemYapiliyor = Boolean.FALSE;
			} catch (Exception ex) {
				ortakIslemler.loggerErrorYaz(sayfaURL, ex);
				throw new Exception(ex);
			}

		}
		if (!(ikRole))
			departmanBolumAyni = false;
		return "";
	}

	/**
	 * @param idler
	 * @return
	 */
	private TreeMap<String, VardiyaHafta> getVardiyaHaftaMap(ArrayList<Long> idler) {
		HashMap map = new HashMap();
		String fieldName = "pId";
		StringBuffer sb = new StringBuffer();
		sb.append("select distinct * from " + VardiyaHafta.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
		sb.append(" where " + VardiyaHafta.COLUMN_NAME_BAS_TARIH + " <= :bitTarih and " + VardiyaHafta.COLUMN_NAME_BIT_TARIH + " >= :basTarih and " + VardiyaHafta.COLUMN_NAME_PERSONEL + ":pId ");
		// map.put(PdksEntityController.MAP_KEY_MAP, "getKeyHafta");
		map.put(fieldName, idler);
		map.put("basTarih", basTarih);
		map.put("bitTarih", bitTarih);
		if (session != null)
			map.put(PdksEntityController.MAP_KEY_SESSION, session);
		// TreeMap<String, VardiyaHafta> vardiyaHaftaMap = pdksEntityController.getObjectBySQLMap(sb, map, VardiyaHafta.class, Boolean.FALSE);
		TreeMap<String, VardiyaHafta> vardiyaHaftaMap = pdksEntityController.getSQLParamTreeMap("getKeyHafta", false, idler, sb, fieldName, map, VardiyaHafta.class, session);

		idler = null;
		return vardiyaHaftaMap;
	}

	/**
	 * @param vardiyaGun
	 * @param hafta
	 * @return
	 */
	public boolean getVardiyaDurum(VardiyaGun vardiyaGun, int hafta) {
		double maxCalismaSure = 12;
		try {
			if (parameterMap.containsKey("maxCalismaSure"))
				maxCalismaSure = Double.parseDouble((String) parameterMap.get("maxCalismaSure"));
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			maxCalismaSure = 12;
		}
		boolean hata = vardiyaGun == null || vardiyaGun.getVardiya() == null || !vardiyaGun.getVardiya().isCalisma();

		if (!hata) {
			if (vardiyaGun.getVardiya() != null && vardiyaGun.getIslemVardiya() == null)
				vardiyaGun.setVardiyaZamani();
			double fark = vardiyaGun.getIslemVardiya().getNetCalismaSuresi();
			if (!authenticatedUser.isVardiyaDuzeltebilir() && fark > (maxCalismaSure + 0.5)) {
				hata = Boolean.TRUE;
				PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "d MMMMM EEEEE") + " günü çalışma süresi " + maxCalismaSure + " saati geçemez!");
			} else if (fark == 0) {
				hata = Boolean.TRUE;
				PdksUtil.addMessageAvailableWarn(PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "d MMMMM EEEEE") + " günü çalışma süresi 0 olamaz!");
			}
		} else
			hata = Boolean.FALSE;

		return hata;
	}

	/**
	 * @param sablon
	 * @param personel
	 * @return
	 */
	public ArrayList<Vardiya> fillVardiyaDoldurList(VardiyaSablonu sablon, Personel personel) {
		ArrayList<Vardiya> list = new ArrayList<Vardiya>();
		try {
			if (vardiyaMap.containsKey(sablon.getToplamSaat()))
				list.addAll(vardiyaMap.get(sablon.getToplamSaat()));
			if (list.isEmpty())
				list.addAll(calismaOlmayanVardiyaList);

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}

		return list;

	}

	/**
	 * @param aylikPuantaj
	 * @param pd
	 * @return
	 */
	public ArrayList<Vardiya> fillAylikVardiyaList(AylikPuantaj aylikPuantaj, PersonelDenklestirme pd) {
		vardiyaMap.clear();
		ArrayList<Vardiya> pdksList = new ArrayList<Vardiya>();
		String radyolojiIzinDurum = ortakIslemler.getParameterKey("radyolojiIzinDurum");
		TreeMap<Long, Vardiya> vardiyaMap = new TreeMap<Long, Vardiya>();
		try {
			if (pd == null && aylikPuantaj != null)
				pd = aylikPuantaj.getPersonelDenklestirme();
			boolean fmi = true;
			boolean calismaOlmayanVardiyalar = false;
			List<AylikPuantaj> aylikPuantajAllList = new ArrayList<AylikPuantaj>();
			Personel personel = null;
			if (aylikPuantaj != null) {
				personel = aylikPuantaj.getPdksPersonel();
				aylikPuantajAllList.add(aylikPuantaj);
			} else if (!aylikPuantajList.isEmpty())
				aylikPuantajAllList.addAll(aylikPuantajList);
			boolean gebeMi = false, sua = false, icap = false;
			for (Iterator iterator = aylikPuantajAllList.iterator(); iterator.hasNext();) {
				AylikPuantaj aylikPuantaj2 = (AylikPuantaj) iterator.next();
				personel = aylikPuantaj2.getPdksPersonel();
				if (!gebeMi)
					gebeMi = personel.isPersonelGebeMi();
				if (!sua)
					sua = personel.isSuaOlur();
				if (!icap)
					icap = personel.getIcapciOlabilir();
				if (personelGebeDurum != null)
					ortakIslemler.vardiyaCalismaModeliGuncelle(aylikPuantaj2.getVardiyalar(), session);
				String donem = String.valueOf(yil * 100 + ay);
				for (VardiyaGun pdksVardiyaGun : aylikPuantaj2.getVardiyalar()) {
					pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(donem));
					Vardiya pdksVardiya = pdksVardiyaGun.getVardiya();
					if (pdksVardiyaGun.isAyinGunu() && pdksVardiya != null) {
						if (pdksVardiya.isCalisma()) {
							if (!sua)
								sua = pdksVardiyaGun.getVardiya().getSua() != null && pdksVardiyaGun.getVardiya().getSua();
							if (!gebeMi)
								gebeMi = pdksVardiyaGun.isGebeMi() || (pdksVardiyaGun.getVardiya().getGebelik() != null && pdksVardiyaGun.getVardiya().getGebelik());
							if (!icap)
								icap = pdksVardiyaGun.getVardiya().getIcapVardiya() != null && pdksVardiyaGun.getVardiya().getIcapVardiya();
						}
						if (pdksVardiya.isFMI() && fmi == false)
							continue;
						if (pdksVardiya.getDurum())
							vardiyaMap.put(pdksVardiya.getId(), pdksVardiya);
					}
				}
			}

			HashMap map = new HashMap();
			StringBuffer sb = new StringBuffer();

			HashMap<String, String> ozelMap = new HashMap<String, String>();
			ozelMap.put(Vardiya.GEBE_KEY, "");
			ozelMap.put(Vardiya.SUA_KEY, "");
			ozelMap.put(Vardiya.ICAP_KEY, "");
			if (pd.getCalismaModeliAy() != null) {
				if (pd.getCalismaModeliAy() != null) {
					try {
						CalismaModeliAy calismaModeliAy = pd.getCalismaModeliAy();
						CalismaModeli cm = calismaModeliAy.getCalismaModeli();
						if (cm.getGenelVardiya().equals(Boolean.FALSE) && cm.isOrtakVardiyadir() == false) {
							sb.append("select V.* from " + Vardiya.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
							sb.append(" inner join " + CalismaModeliVardiya.TABLE_NAME + " CV " + PdksEntityController.getJoinLOCK() + " on CV." + CalismaModeliVardiya.COLUMN_NAME_CALISMA_MODELI + " = " + calismaModeliAy.getCalismaModeli().getId());
							sb.append(" and CV." + CalismaModeliVardiya.COLUMN_NAME_VARDIYA + " = V." + Vardiya.COLUMN_NAME_ID);
							sb.append(" where V." + Vardiya.COLUMN_NAME_DURUM + " = 1 and V." + Vardiya.COLUMN_NAME_GENEL + " <> 1");
							if (session != null)
								map.put(PdksEntityController.MAP_KEY_SESSION, session);
							List<Vardiya> list = pdksEntityController.getObjectBySQLList(sb, map, Vardiya.class);
							if (!list.isEmpty()) {
								HashMap<String, List<Vardiya>> varMap = new HashMap<String, List<Vardiya>>();
								for (Vardiya vardiya : list) {
									String key = "";
									if (vardiya.isGebelikMi())
										key = Vardiya.GEBE_KEY;
									else if (vardiya.isSuaMi())
										key = Vardiya.SUA_KEY;
									else if (vardiya.isIcapVardiyasi())
										key = Vardiya.ICAP_KEY;
									else
										continue;
									List<Vardiya> vList = varMap.containsKey(key) ? varMap.get(key) : new ArrayList<Vardiya>();
									if (vList.isEmpty())
										varMap.put(key, vList);
									vList.add(vardiya);
								}
								for (String key : varMap.keySet()) {
									String str = "";
									list = varMap.get(key);
									for (Iterator iterator = list.iterator(); iterator.hasNext();) {
										Vardiya vardiya = (Vardiya) iterator.next();

										str += vardiya.getId() + (iterator.hasNext() ? ", " : "");
									}
									str = " and " + Vardiya.COLUMN_NAME_ID + (list.size() == 1 ? " = " + str : " IN (" + str + ")");
									ozelMap.put(key, str);
								}

							}

						}
					} catch (Exception eee) {
						logger.error(eee);
					}

				}

			}

			sb = new StringBuffer();
			sb.append("with VARDIYA_DATA as ( ");
			sb.append("select V.* from " + Vardiya.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
			if (pd != null) {
				if (pd.getCalismaModeliAy() != null) {
					try {
						CalismaModeliAy calismaModeliAy = pd.getCalismaModeliAy();
						CalismaModeli cm = calismaModeliAy.getCalismaModeli();
						if (cm.getGenelVardiya().equals(Boolean.FALSE) && cm.isOrtakVardiyadir() == false) {
							sb.append(" inner join " + CalismaModeliVardiya.TABLE_NAME + " CV " + PdksEntityController.getJoinLOCK() + " on CV." + CalismaModeliVardiya.COLUMN_NAME_CALISMA_MODELI + " = " + calismaModeliAy.getCalismaModeli().getId());
							sb.append(" and CV." + CalismaModeliVardiya.COLUMN_NAME_VARDIYA + " = V." + Vardiya.COLUMN_NAME_ID);
							// map.put("cm", calismaModeliAy.getCalismaModeli().getId());
							calismaOlmayanVardiyalar = true;

						}
					} catch (Exception eee) {
						logger.error(eee);
					}

				}
			}
			Sirket sirket = personel.getSirket();
			VardiyaSablonu vardiyaSablonu = personel.getSablon();
			sb.append(" where V." + Vardiya.COLUMN_NAME_GENEL + " = 1 and V." + Vardiya.COLUMN_NAME_GEBELIK + " = 0   ");
			sb.append(" and V." + Vardiya.COLUMN_NAME_GEBELIK + " = 0 and V." + Vardiya.COLUMN_NAME_ICAP + " = 0 and V." + Vardiya.COLUMN_NAME_SUA + " = 0 ");
			sb.append(" and V." + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <>'I'  ");
			if (gebeMi) {
				sb.append(" union ");
				sb.append(" select * from " + Vardiya.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where " + Vardiya.COLUMN_NAME_GEBELIK + " = 1 " + ozelMap.get(Vardiya.GEBE_KEY));
			}
			if (sua) {
				sb.append(" union ");
				sb.append(" select * from " + Vardiya.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where " + Vardiya.COLUMN_NAME_SUA + " = 1 " + ozelMap.get(Vardiya.SUA_KEY));
			}
			if (vardiyaSablonu != null && vardiyaSablonu.getVardiya1() != null && vardiyaSablonu.getVardiya1().isIzinVardiya()) {
				sb.append(" union ");
				sb.append(" select * from " + Vardiya.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " = 'I' ");
			}
			if (icap) {
				sb.append(" union ");
				sb.append(" select * from " + Vardiya.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where " + Vardiya.COLUMN_NAME_ICAP + " = 1 " + ozelMap.get(Vardiya.ICAP_KEY));

			}
			if (calismaOlmayanVardiyalar) {
				sb.append(" union ");
				sb.append(" select * from " + Vardiya.TABLE_NAME + " " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <>'' and " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <>'I'  ");
			}
			if (manuelVardiyaIzinGir) {
				sb.append(" union ");
				sb.append(" select distinct V.* from " + Vardiya.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" left join " + IzinTipi.TABLE_NAME + " I " + PdksEntityController.getJoinLOCK() + " on I." + IzinTipi.COLUMN_NAME_DEPARTMAN + " = V." + Vardiya.COLUMN_NAME_DEPARTMAN + " and I." + IzinTipi.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" and I." + IzinTipi.COLUMN_NAME_GIRIS_TIPI + " = 0 and I." + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + " is null");
				sb.append(" where V." + Vardiya.COLUMN_NAME_DEPARTMAN + " = " + personel.getSirket().getDepartman().getId() + " and V." + Vardiya.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" and V." + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " IN ('" + Vardiya.TIPI_IZIN + "','" + Vardiya.TIPI_HASTALIK_RAPOR + "') and I." + IzinTipi.COLUMN_NAME_ID + " is null ");
			}
			sb.append(" )   ");

			sb.append(" select distinct D.* from VARDIYA_DATA D ");
			sb.append(" where " + Vardiya.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" and (" + Vardiya.COLUMN_NAME_DEPARTMAN + " is null or " + Vardiya.COLUMN_NAME_DEPARTMAN + " = " + sirket.getDepartman().getId() + ") ");
			sb.append(" and (" + Vardiya.COLUMN_NAME_SIRKET + " is null or " + Vardiya.COLUMN_NAME_SIRKET + " = " + sirket.getId() + ") ");
			if (fmi == false) {
				sb.append(" and " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <> :fm ");
				map.put("fm", Vardiya.TIPI_FMI);
			}

			// map.put("departmanId", personel.getSirket().getDepartman().getId());
			logger.debug(sb.toString());
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			vardiyaList = pdksEntityController.getObjectBySQLList(sb, map, Vardiya.class);
			if (!vardiyaList.isEmpty()) {
				for (Iterator<Vardiya> iterator = vardiyaList.iterator(); iterator.hasNext();) {
					Vardiya pdksVardiya = iterator.next();
					if (pdksVardiya.isGebelikMi() && !gebeMi)
						continue;
					if (vardiyaMap.containsKey(pdksVardiya.getId()))
						vardiyaMap.remove(pdksVardiya.getId());
					if (!pdksVardiya.getGenel()) {
						if (!pdksVardiya.isRadyasyonIzni() || (pdksVardiya.isRadyasyonIzni() && (sua || radyolojiIzinDurum.equals("1"))))
							pdksList.add(pdksVardiya);
					} else if (!pdksVardiya.isIsKurMu()) {
						pdksList.add(pdksVardiya);
					}
				}
			}

			if (!vardiyaMap.isEmpty()) {
				vardiyaList.clear();
				vardiyaList.addAll(new ArrayList<Vardiya>(vardiyaMap.values()));
				if (!pdksList.isEmpty())
					vardiyaList.addAll(pdksList);
				pdksList.clear();
				pdksList.addAll(vardiyaList);
				vardiyaMap.clear();
				map.clear();
				for (Vardiya pdksVardiya : vardiyaList)
					vardiyaMap.put(pdksVardiya.getId(), pdksVardiya);
				String fieldName = "id";
				List idList = new ArrayList(vardiyaMap.keySet());
				sb = new StringBuffer();

				sb.append("select V.* from " + Vardiya.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where V." + Vardiya.COLUMN_NAME_ID + " :" + fieldName);
				sb.append(" and V." + Vardiya.COLUMN_NAME_DURUM + " = 1   ");

				map.put(fieldName, idList);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// vardiyaList = pdksEntityController.getObjectBySQLList(sb, map, Vardiya.class);
				vardiyaList = pdksEntityController.getSQLParamList(idList, sb, fieldName, map, Vardiya.class, session);
			}
			vardiyaSablonu = personel.getSablon();
			vardiyaSablonu.setVardiyaList(null);
			List<Vardiya> list = new ArrayList<Vardiya>(vardiyaSablonu.getVardiyaList());
			List<Long> idList = new ArrayList<Long>();
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Vardiya vardiyaS = (Vardiya) iterator.next();
				if (vardiyaS == null || idList.contains(vardiyaS.getId())) {
					iterator.remove();
					continue;
				}
				idList.add(vardiyaS.getId());
				boolean ekle = true;
				for (Iterator iterator2 = vardiyaList.iterator(); iterator2.hasNext();) {
					Vardiya vardiya = (Vardiya) iterator2.next();
					if (vardiyaS.getId().equals(vardiya.getId())) {
						iterator.remove();
						ekle = false;
						break;
					}

				}
				if (ekle)
					vardiyaList.add(vardiyaS);
			}
			list = null;
			idList = null;
			// if (vardiyaList.size() > 1)
			// vardiyaList = PdksUtil.sortObjectStringAlanList(vardiyaList, "getKisaAdiSort", null);
			String donemVardiyalariSiralama = ortakIslemler.getParameterKey("donemVardiyalariSira");
			if (donemVardiyalariSiralama.equals("1"))
				donemVardiyalariSirala(vardiyaList, aylikPuantajAllList);
			else {
				vardiyaMap.clear();
				map.clear();
				for (Vardiya pdksVardiya : vardiyaList)
					vardiyaMap.put(pdksVardiya.getId(), pdksVardiya);
				String fieldName = "id";
				idList = new ArrayList(vardiyaMap.keySet());
				sb = new StringBuffer();
				sb.append("with VERI as ( ");
				sb.append("select V.* from " + Vardiya.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where V." + Vardiya.COLUMN_NAME_ID + " :" + fieldName);
				sb.append(" and V." + Vardiya.COLUMN_NAME_DURUM + " = 1   ");
				sb.append(" ), ");
				sb.append(" SIRA_VARDIYA as (");
				sb.append(" select CASE WHEN " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <>'' THEN 1  ");
				sb.append(" WHEN " + Vardiya.COLUMN_NAME_GENEL + " = 1 THEN 99 ");
				sb.append(" WHEN " + Vardiya.COLUMN_NAME_GEBELIK + " = 1 THEN 12 WHEN " + Vardiya.COLUMN_NAME_SUA + " = 1 THEN 13 WHEN " + Vardiya.COLUMN_NAME_ICAP + " = 1 THEN 14  ELSE 19 END * 10000 + ");
				sb.append(" CASE WHEN " + Vardiya.COLUMN_NAME_VARDIYA_TIPI + " <>'' THEN " + Vardiya.COLUMN_NAME_ID + " ELSE 0 END ONCELIK_SIRA,   " + Vardiya.COLUMN_NAME_ID + " from VERI ");
				sb.append(" ) ");
				sb.append(" select V.* from VERI V ");
				sb.append(" inner join SIRA_VARDIYA S on S." + Vardiya.COLUMN_NAME_ID + " = V." + Vardiya.COLUMN_NAME_ID + " ");
				sb.append(" order by S.ONCELIK_SIRA, V." + Vardiya.COLUMN_NAME_ADI);
				map.put(fieldName, idList);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// vardiyaList = pdksEntityController.getObjectBySQLList(sb, map, Vardiya.class);
				vardiyaList = pdksEntityController.getSQLParamList(idList, sb, fieldName, map, Vardiya.class, session);
			}
			aylikPuantajAllList = null;
			pdksList.clear();
			pdksList.addAll(vardiyaList);

			vardiyaMap = null;
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			logger.error("fillVardiyaList Hata : " + e.getMessage());
		}

		return pdksList;

	}

	/**
	 * @param list
	 * @param aylikPuantajAllList
	 */
	private void donemVardiyalariSirala(List<Vardiya> list, List<AylikPuantaj> aylikPuantajAllList) {
		if (denklestirmeAyDurum && aylikPuantajList != null && !aylikPuantajList.isEmpty()) {
			List<Long> idList = new ArrayList<Long>();
			if (aylikPuantajAllList != null)
				for (AylikPuantaj ap : aylikPuantajAllList)
					idList.add(ap.getPdksPersonel().getId());
			int yilBas = maxYil - 1;
			int maxDonem = yil * 100 + ay;
			try {
				int sistemBaslangicYili = Integer.parseInt(ortakIslemler.getParameterKey("sistemBaslangicYili"));
				if (yilBas < sistemBaslangicYili)
					yilBas = sistemBaslangicYili;
			} catch (Exception e) {

			}
			StringBuffer sb = new StringBuffer();
			sb.append("SP_GET_PERSONEL_VARDIYA");
			LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
			veriMap.put("basYil", yilBas);
			veriMap.put("bitDonem", maxDonem);
			veriMap.put("perId", ortakIslemler.getListIdStr(idList));
			if (session != null)
				veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Vardiya> vardiyaDonemList = null;
			try {
				vardiyaDonemList = pdksEntityController.execSPList(veriMap, sb, Vardiya.class);

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}

			if (vardiyaDonemList != null && !vardiyaDonemList.isEmpty()) {
				for (Iterator iterator = vardiyaDonemList.iterator(); iterator.hasNext();) {
					Vardiya vardiyaDonem = (Vardiya) iterator.next();
					boolean sil = true;
					for (Iterator iterator2 = list.iterator(); iterator2.hasNext();) {
						Vardiya vardiya = (Vardiya) iterator2.next();
						if (vardiya.getId().equals(vardiyaDonem.getId())) {
							sil = false;
							iterator2.remove();
							break;
						}
					}
					if (sil)
						iterator.remove();

				}
				if (!list.isEmpty()) {
					vardiyaDonemList.addAll(list);
					list.clear();
				}
				list.addAll(vardiyaDonemList);
				vardiyaDonemList.clear();
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Vardiya vardiya = (Vardiya) iterator.next();
					if (vardiya.isCalisma()) {
						vardiyaDonemList.add(vardiya);
						iterator.remove();
					}
				}
				if (!vardiyaDonemList.isEmpty())
					list.addAll(vardiyaDonemList);
			}
			vardiyaDonemList = null;
			idList = null;
		}
	}

	/**
	 * @return
	 */
	public List<Vardiya> fillAllVardiyaList() {
		List<Vardiya> pdksList = null;

		try {

			pdksList = pdksEntityController.getSQLParamByFieldList(Vardiya.TABLE_NAME, Vardiya.COLUMN_NAME_DURUM, Boolean.TRUE, Vardiya.class, session);
			for (Iterator iterator = pdksList.iterator(); iterator.hasNext();) {
				Vardiya pdksVardiya = (Vardiya) iterator.next();
				if (pdksVardiya.getDepartman() != null && !aramaSecenekleri.getDepartmanId().equals(pdksVardiya.getDepartman().getId()))
					iterator.remove();
				else if (pdksVardiya.isIsKurMu())
					iterator.remove();

			}

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			logger.error("fillVardiyaList Hata : " + e.getMessage());
		} finally {

		}

		return pdksList;

	}

	/**
	 * 
	 */
	public void fillVardiyaList() {
		vardiyaMap.clear();
		List<Vardiya> pdksList = fillAllVardiyaList();
		boolean durum = Boolean.FALSE;
		List<Vardiya> calismaolmayanVardiyaList = new ArrayList<Vardiya>();
		try {
			for (Iterator<Vardiya> iterator = pdksList.iterator(); iterator.hasNext();) {
				Vardiya pdksVardiya = iterator.next();
				if (pdksVardiya.isCalisma()) {
					ArrayList<Vardiya> list = vardiyaMap.containsKey(pdksVardiya.getCalismaSaati()) ? vardiyaMap.get(pdksVardiya.getCalismaSaati()) : new ArrayList<Vardiya>();
					list.add(pdksVardiya);
					vardiyaMap.put(pdksVardiya.getCalismaSaati(), list);
				} else
					calismaolmayanVardiyaList.add(pdksVardiya);
			}
			if (!calismaolmayanVardiyaList.isEmpty()) {
				for (Iterator<Double> iterator = vardiyaMap.keySet().iterator(); iterator.hasNext();) {
					Double gun = iterator.next();
					ArrayList<Vardiya> list = vardiyaMap.get(gun);
					for (Vardiya pdksVardiya : calismaolmayanVardiyaList)
						list.add(pdksVardiya);
					vardiyaMap.put(gun, list);

				}
			}

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
			logger.error("fillVardiyaList Hata : " + e.getMessage());
		}

		setVardiyaVar(durum);

	}

	/**
	 * @param vardiyaHafta
	 */
	public void haftaRefresh(VardiyaHafta vardiyaHafta) {
		session.refresh(vardiyaHafta);
		for (VardiyaGun pdksVardiyaGun : vardiyaHafta.getVardiyaGunler()) {
			if (pdksVardiyaGun.getId() != null)
				session.refresh(pdksVardiyaGun);

		}
	}

	/**
	 * 
	 */
	public void instanceRefresh() {
		if (degisti != null && degisti) {
			try {
				session.refresh(getVardiyaPlan().getPersonel().getSablon());
				if (getVardiyaPlan().getVardiyaHafta1().getStyle().equals(VardiyaGun.STYLE_CLASS_EVEN) || getVardiyaPlan().getVardiyaHafta2().getStyle().equals(VardiyaGun.STYLE_CLASS_EVEN)) {
					if (getVardiyaPlan().getVardiyaHafta1().isCheckBoxDurum())
						haftaRefresh(getVardiyaPlan().getVardiyaHafta1());
					if (getVardiyaPlan().getVardiyaHafta2().isCheckBoxDurum())
						haftaRefresh(getVardiyaPlan().getVardiyaHafta2());
				} else {
					fillAylikVardiyaPlanList();
				}
			} catch (Exception e) {
				logger.error("Pdks hata in : \n");
				e.printStackTrace();
				logger.error("Pdks hata out : " + e.getMessage());
				logger.error("instanceRefresh : " + e.getMessage());
			}

			degisti = Boolean.FALSE;
		}
		setKaydet(Boolean.TRUE);
	}

	/**
	 * @throws Exception
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	@Transactional
	public void sayfaMesaiTalepAction() throws Exception {
		if (session == null)
			session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		session.setFlushMode(FlushMode.MANUAL);
		sessionClear();
		if (authenticatedUser != null)
			adminRoleDurum(authenticatedUser);
		fazlaMesaiTalepDurum = Boolean.TRUE;
		HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String id = (String) req.getParameter("id");
		islemFazlaMesaiTalep = null;

		mesaiIptalNedenTanimList = null;
		if (id != null) {
			String decodeStr = OrtakIslemler.getDecodeStringByBase64(id);
			StringTokenizer st = new StringTokenizer(decodeStr, "&");
			HashMap<String, String> param = new HashMap<String, String>();
			String fmtIdStr = null, onayDurumuStr = null;
			while (st.hasMoreTokens()) {
				String tk = st.nextToken();
				String[] parStrings = tk.split("=");
				param.put(parStrings[0], parStrings[1]);
			}
			if (param.size() == 2) {
				if (param.containsKey("fmtId"))
					fmtIdStr = param.get("fmtId");
				else
					PdksUtil.addMessageAvailableWarn("Form bilgisi bulunamadı");
				if (param.containsKey("durum"))
					onayDurumuStr = param.get("durum");
				else
					PdksUtil.addMessageAvailableWarn("Form bilgisi onay bulunamadı");

			}
			boolean islemYapildi = fmtIdStr != null && onayDurumuStr != null;
			try {

				if (islemYapildi) {
					int onayDurumu = Integer.parseInt(onayDurumuStr);
					long fmtId = Long.parseLong(fmtIdStr);

					islemFazlaMesaiTalep = (FazlaMesaiTalep) pdksEntityController.getSQLParamByFieldObject(FazlaMesaiTalep.TABLE_NAME, FazlaMesaiTalep.COLUMN_NAME_ID, fmtId, FazlaMesaiTalep.class, session);

					if (islemFazlaMesaiTalep != null) {
						if (islemFazlaMesaiTalep.isIptalEdilebilir()) {
							if (islemFazlaMesaiTalep.getOnayDurumu() == FazlaMesaiTalep.ONAY_DURUM_ISLEM_YAPILMADI) {
								islemYapildi = false;
								if (onayDurumu == FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI) {
									islemFazlaMesaiTalep.setOnayDurumu(onayDurumu);
									islemFazlaMesaiTalep.setGuncellemeTarihi(new Date());
									saveOrUpdate(islemFazlaMesaiTalep);
									sessionFlush();
									mesaiMudurOnayCevabi(true);

								} else {

									redNedenleri = ortakIslemler.getSelectItemList("redNeden", authenticatedUser);

									mesaiIptalNedenTanimList = ortakIslemler.getTanimList(Tanim.TIPI_FAZLA_MESAI_IPTAL_NEDEN, session);
									for (Tanim redNedeni : mesaiIptalNedenTanimList) {
										redNedenleri.add(new SelectItem(redNedeni.getId(), redNedeni.getAciklama()));
									}

								}

							}

						}
						if (islemYapildi)
							PdksUtil.addMessageAvailableWarn("Form bilgisi önceden " + islemFazlaMesaiTalep.getOnayDurumAciklama());

					} else
						PdksUtil.addMessageAvailableWarn("Form bilgisi bulunamadı");
				} else
					fillEkSahaTanim();

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}

		}
	}

	/**
	 * @param sirket
	 * @param tip
	 * @param veriList
	 */
	protected void fazlaMesaiListeGuncelle(Sirket sirket, String tip, List<SelectItem> veriList) {
		if (fazlaMesaiTalepDurum && ikRole) {

			if (denklestirmeAy != null && veriList != null && !veriList.isEmpty()) {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.DATE, 1);
				cal.set(Calendar.MONTH, denklestirmeAy.getAy() - 1);
				cal.set(Calendar.YEAR, denklestirmeAy.getYil());
				Date tarih1 = PdksUtil.getDate(cal.getTime());
				cal.add(Calendar.MONTH, 1);
				cal.add(Calendar.DATE, -1);
				Date tarih2 = PdksUtil.getDate(cal.getTime());
				List list = new ArrayList();
				for (SelectItem selectItem : veriList)
					list.add(selectItem.getValue());
				String fieldName = "l";
				StringBuffer sb = new StringBuffer();
				HashMap fields = new HashMap();
				sb.append("select distinct " + tip + ".ID from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " FT " + PdksEntityController.getJoinLOCK() + " on FT." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V." + VardiyaGun.COLUMN_NAME_ID);
				sb.append(" and FT." + FazlaMesaiTalep.COLUMN_NAME_DURUM + " = 1 and FT." + FazlaMesaiTalep.COLUMN_NAME_ONAY_DURUMU + " not in (" + FazlaMesaiTalep.ONAY_DURUM_RED + ") ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = V." + VardiyaGun.COLUMN_NAME_PERSONEL);
				if (sirket == null) {
					sb.append(" inner join " + Sirket.TABLE_NAME + " S " + PdksEntityController.getJoinLOCK() + " on S." + Sirket.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_SIRKET);
					if (tip.equals("D"))
						sb.append(" inner join " + Departman.TABLE_NAME + " D " + PdksEntityController.getJoinLOCK() + " on D." + Departman.COLUMN_NAME_ID + " = S." + Sirket.COLUMN_NAME_DEPARTMAN);
				} else {
					if (sirket.getDepartman().isAdminMi() || sirket.isTesisDurumu()) {
						if (sirket.getDepartman().isAdminMi() == false || sirket.isTesisDurumu()) {
							sb.append(" and P." + Personel.COLUMN_NAME_SIRKET + " = :s");
							fields.put("s", sirket.getId());
						}
						if (tip.equals("T") && sirket.getDepartman().isAdminMi())
							sb.append(" inner join " + Tanim.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + Tanim.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_TESIS);
					}
				}
				if (tip.equals("B"))
					sb.append(" inner join " + Tanim.TABLE_NAME + " B " + PdksEntityController.getJoinLOCK() + " on B." + Tanim.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_EK_SAHA3);
				sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :ta1 and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= :ta2");
				sb.append(" and " + tip + ".ID " + fieldName);
				fields.put("ta1", tarih1);
				fields.put("ta2", tarih2);
				fields.put(fieldName, list);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				List idler = null;
				try {
					// idler = pdksEntityController.getObjectBySQLList(sb, fields, null);
					idler = pdksEntityController.getSQLParamList(list, sb, fieldName, fields, null, session);
				} catch (Exception e) {
					logger.equals(e);
					e.printStackTrace();
				}

				if (idler != null && veriList.size() > idler.size()) {
					list.clear();
					for (Iterator iterator = idler.iterator(); iterator.hasNext();) {
						Object id = (Object) iterator.next();
						if (id instanceof BigDecimal)
							list.add(((BigDecimal) id).longValue());
						else
							list.add(Long.parseLong(String.valueOf(id)));
					}
					for (Iterator iterator = veriList.iterator(); iterator.hasNext();) {
						SelectItem selectItem = (SelectItem) iterator.next();
						if (!list.contains(selectItem.getValue()))
							iterator.remove();
					}
				}
				veriList = null;
				list = null;
			}
		}

	}

	/**
	 * @throws Exception
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaMesaiTalepListAction() throws Exception {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		session.setFlushMode(FlushMode.MANUAL);
		adminRoleDurum(authenticatedUser);
		fazlaMesaiTalepDurum = Boolean.TRUE;
		sessionClear();
		seciliVardiyaGun = null;
		islemYapiliyor = null;
		aylar = ortakIslemler.getAyListesi(Boolean.TRUE);
		modelGoster = Boolean.FALSE;
		Calendar cal = Calendar.getInstance();
		ay = cal.get(Calendar.MONTH) + 1;
		yil = cal.get(Calendar.YEAR);
		cal.add(Calendar.MONTH, 1);
		maxYil = cal.get(Calendar.YEAR);
		if (aylikFazlaMesaiTalepler != null)
			aylikFazlaMesaiTalepler.clear();
		else
			aylikFazlaMesaiTalepler = new ArrayList<FazlaMesaiTalep>();
		sicilNo = "";
		kullaniciPersonel = ortakIslemler.getKullaniciPersonel(authenticatedUser);
		if (kullaniciPersonel)
			sicilNo = authenticatedUser.getPdksPersonel().getPdksSicilNo();
		mesaiDonemDegisti(true);
		fillEkSahaTanim();
	}

	/**
	 * @return
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public String sayfaFazlaMesaiTalepRaporAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		session.setFlushMode(FlushMode.MANUAL);
		adminRoleDurum(authenticatedUser);

		fazlaMesaiTalepDurum = Boolean.TRUE;
		sessionClear();
		mesaiOnayla = Boolean.FALSE;
		modelList = new ArrayList<CalismaModeliAy>();
		if (fazlaMesaiTalepler == null)
			fazlaMesaiTalepler = new ArrayList<FazlaMesaiTalep>();
		else
			fazlaMesaiTalepler.clear();
		try {
			departmanBolumAyni = Boolean.FALSE;
			aramaSecenekleri = new AramaSecenekleri(authenticatedUser);
			stajerSirket = Boolean.FALSE;
			boolean ayniSayfa = authenticatedUser.getCalistigiSayfa() != null && authenticatedUser.getCalistigiSayfa().equals("fazlaMesaiTalep");
			if (!ayniSayfa)
				authenticatedUser.setCalistigiSayfa("fazlaMesaiTalep");
			setPlanGirisi(Boolean.TRUE);
			islemYapiliyor = Boolean.FALSE;
			hastaneSuperVisor = authenticatedUser.isDirektorSuperVisor();

			aramaSecenekleri.setTesisList(null);
			aramaSecenekleri.setTesisId(null);
			aramaSecenekleri.setGorevYeriList(null);
			aramaSecenekleri.setEkSaha3Id(null);
			aramaSecenekleri.setSirketId(null);
			gorevYeri = null;

			Calendar cal = Calendar.getInstance();
			ay = cal.get(Calendar.MONTH) + 1;
			yil = cal.get(Calendar.YEAR);
			cal.add(Calendar.MONTH, 1);
			maxYil = cal.get(Calendar.YEAR);
			sonDonem = (maxYil * 100) + cal.get(Calendar.MONTH) + 1;
			if (basTarih == null && (ikRole)) {
				cal = Calendar.getInstance();
				setBasTarih(PdksUtil.getDate(cal.getTime()));
			}
			setBitTarih(null);
			LinkedHashMap<String, Object> veriLastMap = ortakIslemler.getLastParameter("fazlaMesaiTalep", session);

			aramaSecenekleri.setDepartman(authenticatedUser.getDepartman());
			aramaSecenekleri.setDepartmanId(authenticatedUser.getDepartman().getId());
			setDepartman(authenticatedUser.getDepartman());

			setVardiyaPlanList(new ArrayList<VardiyaPlan>());
			if (aramaSecenekleri.getGorevYeriList() == null && departman != null && !departman.isAdminMi())
				aramaSecenekleri.setGorevYeriList(fazlaMesaiOrtakIslemler.getBolumDepartmanSelectItems(departman.getId(), aramaSecenekleri.getSirketId(), yil, ay, Boolean.FALSE, session));

			setDonusAdres("");
			Long tesisId = null, ekSaha4Id = null;

			if (veriLastMap != null) {
				if (veriLastMap.containsKey("yil") && veriLastMap.containsKey("ay") && veriLastMap.containsKey("sirketId")) {
					yil = Integer.parseInt((String) veriLastMap.get("yil"));
					ay = Integer.parseInt((String) veriLastMap.get("ay"));
					basTarih = null;
					if (veriLastMap.containsKey("tarih") && (ikRole))
						basTarih = PdksUtil.convertToJavaDate((String) veriLastMap.get("tarih"), "yyyy-MM-dd");
					Long sId = Long.parseLong((String) veriLastMap.get("sirketId"));
					aramaSecenekleri.setSirketId(sId);
					if (veriLastMap.containsKey("tesisId")) {
						tesisId = Long.parseLong((String) veriLastMap.get("tesisId"));
						aramaSecenekleri.setTesisId(tesisId);
					}
					if (veriLastMap.containsKey("altBolumId")) {
						ekSaha4Id = Long.parseLong((String) veriLastMap.get("altBolumId"));
						aramaSecenekleri.setEkSaha4Id(ekSaha4Id);
					}
					if (veriLastMap.containsKey("bolumId"))
						aramaSecenekleri.setEkSaha3Id(Long.parseLong((String) veriLastMap.get("bolumId")));
					if ((ikRole) && veriLastMap.containsKey("sicilNo"))
						sicilNo = (String) veriLastMap.get("sicilNo");

				}

			}
			aylariDoldur();
			if (adminRole)
				fillDepartmanList();

			fillSirketDoldur(false);
			tesisId = aramaSecenekleri.getTesisId();
			if (aramaSecenekleri.getSirketId() != null) {
				fillTesisDoldur(false);
			}
			if (aramaSecenekleri.getEkSaha3Id() != null || tesisId != null) {
				fillBolumDoldur();
			}

			if (!ayniSayfa)
				authenticatedUser.setCalistigiSayfa("");
			if (donusAdres.equals("fazlaMesaiTalep"))
				setDonusAdres("");

			fazlaMesaiTalepDurumList = ortakIslemler.getSelectItemList("fazlaMesaiTalepDurum", authenticatedUser);

			talepOnayDurum = 0;
			fazlaMesaiTalepDurumList.add(new SelectItem(talepOnayDurum, "Tüm Fazla Mesai Talepler"));
			fazlaMesaiTalepDurumList.add(new SelectItem(FazlaMesaiTalep.ONAY_DURUM_ISLEM_YAPILMADI, "İşlem Yapılmayan"));
			fazlaMesaiTalepDurumList.add(new SelectItem(FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI, "Onaylanan"));
			fillEkSahaTanim();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	/**
	 * @return
	 */
	public String fillFazlaMesaiTalepList() {
		mailGonder = Boolean.FALSE;
		mesaiOnayla = Boolean.FALSE;
		linkAdres = null;
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		sessionClear();

		denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
		setAylikPuantajDonem(denklestirmeAy);
		setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
		if (denklestirmeAy != null) {
			try {
				departmanDenklestirmeDonemi = new DepartmanDenklestirmeDonemi();
				AylikPuantaj aylikPuantaj = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, departmanDenklestirmeDonemi, session);
				departmanDenklestirmeDonemi.setDenklestirmeAy(denklestirmeAy);
				fillFazlaMesaiTalepDevam(aylikPuantaj, departmanDenklestirmeDonemi);
			} catch (Exception ee) {
				logger.error(ee);
				ee.printStackTrace();
			}

		} else
			PdksUtil.addMessageWarn("İlgili döneme ait fazla mesai bulunamadı!");
		if (!(ikRole))
			departmanBolumAyni = false;
		return "";
	}

	/**
	 * @param aylikPuantajSablon
	 * @param denklestirmeDonemi
	 */
	@Transactional
	public void fillFazlaMesaiTalepDevam(AylikPuantaj aylikPuantajSablon, DepartmanDenklestirmeDonemi denklestirmeDonemi) {
		mailGonder = Boolean.FALSE;
		mesaiOnayla = Boolean.FALSE;
		seciliDurum = false;

		departmanBolumAyni = Boolean.FALSE;
		LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
		Long departmanId = aramaSecenekleri.getDepartmanId(), sirketId = aramaSecenekleri.getSirketId(), tesisId = aramaSecenekleri.getTesisId(), seciliEkSaha3Id = aramaSecenekleri.getEkSaha3Id();
		if (sirketId != null || seciliEkSaha3Id != null) {
			lastMap.put("yil", "" + yil);
			lastMap.put("ay", "" + ay);
			if (departmanId != null)
				lastMap.put("departmanId", "" + departmanId);
			if (sirketId != null)
				lastMap.put("sirketId", "" + sirketId);
			if (tesisId != null)
				lastMap.put("tesisId", "" + tesisId);
			if (seciliEkSaha3Id != null)
				lastMap.put("bolumId", "" + seciliEkSaha3Id);
			if (PdksUtil.hasStringValue(sicilNo))
				lastMap.put("sicilNo", sicilNo.trim());
		}

		if (basTarih != null)
			lastMap.put("tarih", PdksUtil.convertToDateString(basTarih, "yyyy-MM-dd"));
		try {
			ortakIslemler.saveLastParameter(lastMap, session);
		} catch (Exception e) {

		}
		Sirket sirket = null;
		if (sirketId != null && sirketId > 0) {

			sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);

		}
		departmanBolumAyni = sirket != null && sirket.isTesisDurumu() == false;

		if (sicilNo != null)
			sicilNo = ortakIslemler.getSicilNo(sicilNo.trim());
		else
			sicilNo = "";

		aylikPuantajSablon.getVardiyalar();
		setAylikPuantajDefault(aylikPuantajSablon);

		try {

			HashMap map = new HashMap();
			List<Personel> personelList = fazlaMesaiOrtakIslemler.getFazlaMesaiMudurPersonelList(aramaSecenekleri.getSirket(), tesisId != null ? "" + tesisId : null, seciliEkSaha3Id, (denklestirmeAy != null ? aylikPuantajDonem : null), getDenklestirmeDurum(), fazlaMesaiTalepDurum, session);
			List<String> perList = new ArrayList<String>();
			for (Personel personel : personelList) {
				if (PdksUtil.hasStringValue(sicilNo) == false || ortakIslemler.isStringEqual(sicilNo, personel.getPdksSicilNo()))
					perList.add(personel.getPdksSicilNo());

			}

			if (!perList.isEmpty()) {
				List<String> list1 = ortakIslemler.getYetkiTumPersonelNoList(), list2 = null;
				List<Personel> yetkiList = getFazlaMesaiSecimList("P");
				if (yetkiList != null && !yetkiList.isEmpty()) {
					list2 = new ArrayList<String>();
					for (Personel personel : yetkiList) {
						list2.add(personel.getPdksSicilNo());
					}
					list1.addAll(list2);
					if (ikRole) {
						list2.clear();
						if (authenticatedUser.getIkinciYoneticiPersonel() != null) {

							for (Personel personel : authenticatedUser.getIkinciYoneticiPersonel()) {
								list2.add(personel.getPdksSicilNo());
							}
						}
					}
				}
				map.clear();
				String fieldName = "p";
				StringBuffer sb = new StringBuffer();
				sb.append("select F.* from " + VardiyaGun.TABLE_NAME + " V " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P.ID=V." + VardiyaGun.COLUMN_NAME_PERSONEL);
				sb.append(" and P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);
				sb.append(" inner join " + FazlaMesaiTalep.TABLE_NAME + " F " + PdksEntityController.getJoinLOCK() + " on F." + FazlaMesaiTalep.COLUMN_NAME_VARDIYA_GUN + " = V.ID and F.DURUM=1  ");
				if (talepOnayDurum > 0) {
					sb.append(" and F." + FazlaMesaiTalep.COLUMN_NAME_ONAY_DURUMU + " = :d  ");
					map.put("d", talepOnayDurum);
				}
				if (basTarih != null && (ikRole)) {
					sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " = :t");
					map.put("t", basTarih);
				} else {
					sb.append(" where V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " >= :b1 and V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " <= :b2 ");
					map.put("b2", aylikPuantajSablon.getSonGun());
					map.put("b1", aylikPuantajSablon.getIlkGun());

				}
				sb.append(" order by V." + VardiyaGun.COLUMN_NAME_VARDIYA_TARIHI + " desc, F." + FazlaMesaiTalep.COLUMN_NAME_BASLANGIC_ZAMANI + " desc, ");
				sb.append(Personel.COLUMN_NAME_AD + "," + Personel.COLUMN_NAME_SOYAD);
				map.put(fieldName, perList);
				if (session != null)
					map.put(PdksEntityController.MAP_KEY_SESSION, session);
				// fazlaMesaiTalepler = pdksEntityController.getObjectBySQLList(sb, map, FazlaMesaiTalep.class);
				fazlaMesaiTalepler = pdksEntityController.getSQLParamList(perList, sb, fieldName, map, FazlaMesaiTalep.class, session);

				Personel loginPersonel = authenticatedUser.getPdksPersonel();
				for (FazlaMesaiTalep ft : fazlaMesaiTalepler) {
					ft.setCheckBoxDurum(false);
					Personel personel = ft.getVardiyaGun().getPersonel();
					String personelNo = personel.getPdksSicilNo();
					boolean onaylayan = list2 != null && list2.contains(personelNo);
					if (!onaylayan) {
						Personel yonetici2 = personel.getYonetici2();
						if (yonetici2 != null)
							onaylayan = loginPersonel.getId().equals(yonetici2.getId());
					}
					ft.setOnaylayan(onaylayan);
					if (ft.isOnaylayan())
						ft.setYoneticisi(false);
					else {
						ft.setYoneticisi(list1 != null && list1.contains(personelNo));
					}

					if (ft.isHatirlatmaMail()) {
						if (ft.isYoneticisi())
							mailGonder = Boolean.TRUE;
						if (ft.isOnaylayan())
							mesaiOnayla = Boolean.TRUE;
					}
				}
				list1 = null;
				list2 = null;
			}

		} catch (InvalidStateException e) {
			InvalidValue[] invalidValues = e.getInvalidValues();
			if (invalidValues != null) {
				for (InvalidValue invalidValue : invalidValues) {
					Object object = invalidValue.getBean();
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

			logger.debug("Pdks hata in : \n");
		}

	}

	protected List getFazlaMesaiSecimList(String tip) {
		List list = null;
		if (denklestirmeAy == null) {

			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			setAylikPuantajDonem(denklestirmeAy);
			setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
		}
		if (denklestirmeAy != null && authenticatedUser.getCalistigiSayfa() != null && authenticatedUser.getCalistigiSayfa().equals("fazlaMesaiTalep")) {
			String whereStr = " inner join FAZLA_MESAI_TALEP FT " + PdksEntityController.getJoinLOCK() + " on FT.VARDIYA_GUN_ID=V.ID and FT.DURUM=1 ";
			Class class1 = null;
			if (tip.equals("S"))
				class1 = Sirket.class;
			else {
				String baglac = " WHERE";
				if (tip.equals("T") || tip.equals("B")) {
					if (aramaSecenekleri.getSirketId() != null) {
						whereStr += baglac + " D." + Personel.COLUMN_NAME_SIRKET + " = " + aramaSecenekleri.getSirketId();
						baglac = " and ";
					}
					if (tip.equals("B") && aramaSecenekleri.getTesisId() != null) {
						whereStr += baglac + " D." + Personel.COLUMN_NAME_TESIS + " = " + aramaSecenekleri.getTesisId();
						baglac = " and ";
					}
					class1 = Tanim.class;
				}

				else if (tip.equals("P")) {
					class1 = Personel.class;
					if (aramaSecenekleri.getSirketId() != null) {
						whereStr += baglac + " D." + Personel.COLUMN_NAME_SIRKET + " = " + aramaSecenekleri.getSirketId();
						baglac = " and ";
					}
					if (tip.equals("B") && aramaSecenekleri.getTesisId() != null) {
						whereStr += baglac + " D." + Personel.COLUMN_NAME_TESIS + " = " + aramaSecenekleri.getTesisId();
						baglac = " and ";
					}
					if (tip.equals("B") && aramaSecenekleri.getEkSaha3Id() != null) {
						whereStr += baglac + " D." + Personel.COLUMN_NAME_EK_SAHA3 + " = " + aramaSecenekleri.getEkSaha3Id();
						baglac = " and ";
					}
				}
			}

			list = new ArrayList();
			StringBuffer sb = new StringBuffer();
			sb.append("SP_YONETICI_VARDIYA_BILGI_TIPI ");
			LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
			fields.put("yoneticiId", authenticatedUser.isIK() == false && authenticatedUser.isAdmin() == false ? authenticatedUser.getPdksPersonel().getId() : -1L);
			fields.put("donemId", denklestirmeAy.getId());
			fields.put("tip", tip);
			fields.put("kosul", whereStr);
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			try {
				List newList = pdksEntityController.execSPList(fields, sb, class1);
				if (list != null && !newList.isEmpty())
					list.addAll(newList);
				newList = null;

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
			sb = null;

		}
		return list;
	}

	/**
	 * @return
	 */
	public String mailSec() {
		for (FazlaMesaiTalep ft : fazlaMesaiTalepler) {
			if (ft.isHatirlatmaMail())
				ft.setCheckBoxDurum(seciliDurum);
		}
		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String topluMesaiOnayla() {
		List<FazlaMesaiTalep> list = new ArrayList<FazlaMesaiTalep>();
		for (FazlaMesaiTalep ft : fazlaMesaiTalepler) {
			if (ft.isOnaylayan() && ft.isHatirlatmaMail() && ft.isCheckBoxDurum()) {
				list.add(ft);
				if (list.size() >= 20)
					break;
			}

		}
		if (!list.isEmpty()) {
			boolean flush = false;
			List<String> mesajList = new ArrayList<String>();
			for (FazlaMesaiTalep ft : list) {
				islemFazlaMesaiTalep = ft;
				ft.setOnayDurumu(FazlaMesaiTalep.ONAY_DURUM_ONAYLANDI);
				ft.setGuncellemeTarihi(new Date());
				ft.setGuncelleyenUser(authenticatedUser);
				saveOrUpdate(ft);

				mesaiMudurOnayCevabi(true);
				String mesaj = ft.getOlusturanUser().getAdSoyad() + " fazla mesai talep cevabı gönderildi.";
				if (!mesajList.contains(mesaj)) {
					PdksUtil.addMessageAvailableInfo(mesaj);
					mesajList.add(mesaj);
				}

				flush = true;
			}
			if (flush)
				sessionFlush();
		} else
			PdksUtil.addMessageAvailableWarn("Seçili kayıt yok!");
		list = null;
		return "";
	}

	/**
	 * @return
	 */
	@Transactional
	public String topluMailGonder() {
		List<FazlaMesaiTalep> list = new ArrayList<FazlaMesaiTalep>();
		for (FazlaMesaiTalep ft : fazlaMesaiTalepler) {
			if (ft.isYoneticisi() && ft.isHatirlatmaMail() && ft.isCheckBoxDurum()) {
				list.add(ft);
				if (list.size() >= 20)
					break;
			}

		}
		if (!list.isEmpty()) {
			boolean flush = false;
			List<String> mesajList = new ArrayList<String>();
			for (FazlaMesaiTalep ft : list) {
				Personel yonetici2 = ft.getVardiyaGun().getPersonel().getYonetici2(), fmOnay = ft.getGuncelleyenUser().getPdksPersonel();
				String mesaj = null;
				if (fmOnay.isCalisiyor())
					yonetici2 = fmOnay;
				else if (yonetici2 != null) {
					if (yonetici2.getId().equals(fmOnay.getId())) {
						mesaj = yonetici2.getAdSoyad() + " çalışmıyor!";
						yonetici2 = null;
					} else {

						User guncelleyenUser = (User) pdksEntityController.getSQLParamByFieldObject(User.TABLE_NAME, User.COLUMN_NAME_PERSONEL, yonetici2.getId(), User.class, session);

						if ((guncelleyenUser != null && guncelleyenUser.isDurum())) {
							ft.setGuncelleyenUser(guncelleyenUser);
							saveOrUpdate(ft);
							flush = true;
						} else {
							if (guncelleyenUser != null)
								mesaj = guncelleyenUser.getUsername() + " " + yonetici2.getAdSoyad() + " aktif kullanıcı değildir!";
							else
								mesaj = ft.getVardiyaGun().getPersonel().getAdSoyad() + " aktif kullanıcısı yok!";
							yonetici2 = null;
						}
					}
				} else
					mesaj = ft.getVardiyaGun().getPersonel().getAdSoyad() + " aktif 2. yöneticisi yok!";
				if (mesaj != null) {
					if (!mesajList.contains(mesaj)) {
						PdksUtil.addMessageAvailableError(mesaj);
						mesajList.add(mesaj);
					}

				}
				if (yonetici2 != null) {
					mesaiMailHatirlatma(ft);
					String talepGirisCikisHareketEkleStr = ortakIslemler.getParameterKey("talepGirisCikisHareketEkle");
					if (talepGirisCikisHareketEkleStr.equals("1"))
						talepGirisCikisHareketEkle();
				}

				ft.setCheckBoxDurum(false);
			}
			if (flush)
				sessionFlush();
		} else
			PdksUtil.addMessageAvailableWarn("Seçili kayıt yok!");
		list = null;
		return "";
	}

	/**
	 * @return
	 */
	public String fazlaMesaiTalepExcel() {
		try {

			String gorevYeriAciklama = getExcelAciklama();
			ByteArrayOutputStream baosDosya = fazlaMesaiTalepExcelDevam();
			if (baosDosya != null) {
				String dosyaAdi = "AylıkFazlaMesaiTalep_" + gorevYeriAciklama + PdksUtil.convertToDateString(aylikPuantajDefault.getIlkGun(), "yyyyMM") + ".xlsx";
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
	 * @return
	 */
	private ByteArrayOutputStream fazlaMesaiTalepExcelDevam() {

		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		try {

			Sheet sheet = ExcelUtil.createSheet(wb, "Tüm Talepler", Boolean.TRUE);
			CellStyle header = ExcelUtil.getStyleHeader(wb);

			CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
			CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
			CellStyle styleOddTutar = ExcelUtil.setAlignment(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
			CellStyle styleOddNumber = ExcelUtil.setAlignment(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_NUMBER, wb), CellStyle.ALIGN_CENTER);
			CellStyle styleOddDateTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
			CellStyle styleOddDate = ExcelUtil.setBoldweight(ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);
			CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
			CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
			CellStyle styleEvenTutar = ExcelUtil.setAlignment(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_TUTAR, wb), CellStyle.ALIGN_CENTER);
			CellStyle styleEvenNumber = ExcelUtil.setAlignment(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_NUMBER, wb), CellStyle.ALIGN_CENTER);
			CellStyle styleEvenDateTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);
			CellStyle styleEvenDate = ExcelUtil.setBoldweight(ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb), HSSFFont.BOLDWEIGHT_BOLD);

			int row = 0;
			int col = 0;
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adı Soyadı");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.sirketAciklama());
			boolean tesisDurum = ortakIslemler.getListTesisDurum(fazlaMesaiTalepler);
			if (tesisDurum)
				ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.tesisAciklama());
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(bolumAciklama);
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Tarihi");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.vardiyaAciklama());
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Başlangıç Zamanı");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Bitiş Zamanı");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Süresi (Saat)");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay Durum");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay'a Gönderen");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onay'a Gönderme Zamanı");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onaylayan");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Onaylama Zamanı");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Mesai Nedeni");
			boolean renk = true;
			for (Iterator iterator = fazlaMesaiTalepler.iterator(); iterator.hasNext();) {
				FazlaMesaiTalep ft = (FazlaMesaiTalep) iterator.next();
				VardiyaGun vg = ft.getVardiyaGun();
				Personel personel = vg.getPersonel();
				String sirket = "";
				CellStyle style = null, styleCenter = null, cellStyleDateTime = null, cellStyleDate = null, cellTutar = null, cellNumber = null;
				if (renk) {
					cellStyleDateTime = styleOddDateTime;
					cellStyleDate = styleOddDate;
					cellTutar = styleOddTutar;
					cellNumber = styleOddNumber;
					style = styleOdd;
					styleCenter = styleOddCenter;
				} else {
					cellStyleDateTime = styleEvenDateTime;
					cellStyleDate = styleEvenDate;
					cellTutar = styleEvenTutar;
					cellNumber = styleEvenNumber;
					style = styleEven;
					styleCenter = styleEvenCenter;
				}
				renk = !renk;
				try {
					sirket = personel.getSirket().getAd();
				} catch (Exception e1) {
					sirket = "" + ortakIslemler.sirketAciklama() + " tanımsız";
				}

				row++;
				col = 0;
				try {
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAdSoyad());
					ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getSicilNo());
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket);
					if (tesisDurum)
						ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getTesis() != null ? personel.getTesis().getAciklama() : "");
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "");
					ExcelUtil.getCell(sheet, row, col++, cellStyleDate).setCellValue(vg.getVardiyaDate());
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(vg.getVardiyaAciklama());
					ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(ft.getBaslangicZamani());
					ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(ft.getBitisZamani());
					Double sure = ft.getMesaiSuresi();
					ExcelUtil.getCell(sheet, row, col++, PdksUtil.isDoubleValueNotLong(sure) ? cellTutar : cellNumber).setCellValue(sure);
					String neden = ft.getMesaiNeden().getAciklama() + (PdksUtil.hasStringValue(ft.getAciklama()) ? "\nAçıklama : " + ft.getAciklama().trim() : "");
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(ft.getOnayDurumAciklama());
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(ft.getOlusturanUser().getAdSoyad());
					ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(ft.getOlusturmaTarihi());
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(ft.getGuncelleyenUser() != null ? ft.getGuncelleyenUser().getAdSoyad() : "");
					if (ft.getGuncellemeTarihi() != null)
						ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(ft.getGuncellemeTarihi());
					else
						ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(neden);
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					logger.debug(row);

				}

			}
			for (int i = 0; i < col; i++)
				sheet.autoSizeColumn(i);

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
	 * 
	 */
	private void bordroAlanKapat() {
		gerceklesenMesaiKod = Boolean.TRUE;
		devredenBakiyeKod = Boolean.TRUE;
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
		devredenMesaiKod = Boolean.TRUE;
		ucretiOdenenKod = Boolean.TRUE;
		if (baslikMap == null)
			baslikMap = new TreeMap<String, Boolean>();
		else
			baslikMap.clear();
	}

	private void aylikPuantajListClear() {
		if (loginUser == null)
			loginUser = authenticatedUser;
		if (aylikPuantajList != null)
			aylikPuantajList.clear();
		else
			aylikPuantajList = ortakIslemler.getSelectItemList("aylikPuantaj", loginUser);
	}

	/**
	 * @throws Exception
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaGirisAction() throws Exception {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		aylikPuantajListClear();
		componentState.setSeciliTab("");
		tumBolumPersonelleri = null;
		bordroPuantajEkranindaGoster = false;
		linkBordroAdres = null;
		aylikVardiyaPlanGiris(sayfaURL, true);

	}

	/**
	 * @return
	 */
	private void setManuelKapi() {
		HashMap<String, KapiView> manuelKapiMap = ortakIslemler.getManuelKapiMap(null, session);
		manuelGiris = manuelKapiMap.get(Kapi.TIPI_KODU_GIRIS);
		manuelCikis = manuelKapiMap.get(Kapi.TIPI_KODU_CIKIS);
		manuelKapiMap = null;
	}

	/**
	 * @param bolumDoldurDurum
	 * @throws Exception
	 */
	public void tesisDoldur(boolean bolumDoldurDurum) throws Exception {
		Sirket sirket = null;
		listeleriTemizle();
		tumBolumPersonelleri = null;
		if (aramaSecenekleri.getSirketId() != null) {

			sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

			ekSaha4Tanim = ortakIslemler.getEkSaha4(sirket, null, session);
		}
		aramaSecenekleri.setSirket(sirket);

		List<SelectItem> list = fazlaMesaiOrtakIslemler.getFazlaMesaiTesisList(aramaSecenekleri.getSirket(), denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
		fazlaMesaiListeGuncelle(sirket, "T", list);
		aramaSecenekleri.setTesisList(list);
		Long onceki = aramaSecenekleri.getTesisId();
		if (!list.isEmpty()) {
			if (list.size() == 1 || onceki == null)
				aramaSecenekleri.setTesisId((Long) list.get(0).getValue());
			else if (onceki != null) {
				for (SelectItem st : list) {
					if (st.getValue().equals(onceki))
						aramaSecenekleri.setTesisId(onceki);
				}
			}
		} else
			aramaSecenekleri.setTesisId(null);
		if (sirket != null) {
			if (bolumDoldurDurum || sirket.getTesisDurum() == null || sirket.getTesisDurum() == false) {
				if (sirket != null) {
					if (sirket.isTesisDurumu() == false)
						aramaSecenekleri.setTesisId(null);
				}
				bolumDoldur();
			}
		}

		aylikPuantajList.clear();
		fazlaMesaiTalepler.clear();

	}

	/**
	 * @throws Exception
	 */
	public void bolumDoldur() throws Exception {
		stajerSirket = Boolean.FALSE;
		aramaSecenekleri.setGorevYeriList(null);
		tumBolumPersonelleri = null;
		if (!PdksUtil.hasStringValue(donusAdres)) {
			listeleriTemizle();
		}
		Sirket sirket = null;
		if (aramaSecenekleri.getSirket() != null || aramaSecenekleri.getSirketId() != null) {
			sirket = aramaSecenekleri.getSirket();
			if (aramaSecenekleri.getSirket() == null) {

				sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

				aramaSecenekleri.setSirket(sirket);
			}
			departman = sirket.getDepartman();

		}
		List<SelectItem> list = null;
		if (sirket != null) {
			String tesisId = null;
			if (departman.isAdminMi() && sirket.isTesisDurumu())
				tesisId = aramaSecenekleri.getTesisId() != null ? String.valueOf(aramaSecenekleri.getTesisId()) : null;

			list = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, tesisId, denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
			fazlaMesaiListeGuncelle(sirket, "B", list);
		} else
			list = ortakIslemler.getSelectItemList("bolum", authenticatedUser);

		aramaSecenekleri.setGorevYeriList(list);
		Long onceki = aramaSecenekleri.getEkSaha3Id();
		if (!list.isEmpty()) {
			if (list.size() == 1 || onceki == null)
				aramaSecenekleri.setEkSaha3Id((Long) list.get(0).getValue());
			else if (onceki != null) {
				aramaSecenekleri.setEkSaha3Id(null);
				for (SelectItem st : list) {
					if (st.getValue().equals(onceki))
						aramaSecenekleri.setEkSaha3Id(onceki);

				}
			}
			if (list.size() > 1) {
				if (ortakIslemler.getParameterKey("tumBolumPersonelGetir").equals("1") && !(ikRole || adminRole)) {
					tumBolumPersonelleri = fazlaMesaiOrtakIslemler.getTumBolumPersonelListesi(sirket, denklestirmeAy, aramaSecenekleri.getTesisId(), getDenklestirmeDurum(), session);
					if (tumBolumPersonelleri != null) {
						if (!tumBolumPersonelleri.isEmpty())
							componentState.setSeciliTab("tab2");
						else
							tumBolumPersonelleri = null;
					}
				}
			}
		} else
			aramaSecenekleri.setEkSaha3Id(null);

		fileImportKontrol();
		if (ekSaha4Tanim != null && aramaSecenekleri.getEkSaha3Id() != null)
			altBolumDoldur();
		else {
			aramaSecenekleri.setAltBolumIdList(null);
			aramaSecenekleri.setEkSaha4Id(null);
		}

	}

	/**
	 * @return
	 */
	public String altBolumDoldur() {
		aylikPuantajList.clear();
		List<SelectItem> altBolumIdList = null;
		seciliEkSaha4Id = aramaSecenekleri.getEkSaha4Id();
		if (ekSaha4Tanim != null) {
			Long tesisId = aramaSecenekleri.getTesisId();
			List<SelectItem> list = fazlaMesaiOrtakIslemler.getFazlaMesaiAltBolumList(aramaSecenekleri.getSirket(), tesisId != null ? String.valueOf(tesisId) : null, aramaSecenekleri.getEkSaha3Id(), denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
			altBolumIdList = ortakIslemler.getSelectItemList("altBolum", authenticatedUser);
			boolean hepsiEkle = true;
			if (list.size() > 1) {
				List<Personel> donemPerList = null;
				try {
					donemPerList = fazlaMesaiOrtakIslemler.getFazlaMesaiPersonelList(aramaSecenekleri.getSirket(), tesisId != null ? String.valueOf(tesisId) : null, aramaSecenekleri.getEkSaha3Id(), null, denklestirmeAy != null ? aylikPuantajDonem : null, getDenklestirmeDurum(), session);
				} catch (Exception e) {
					logger.error(e);
				}
				hepsiEkle = donemPerList == null || donemPerList.size() < 100;
				if (hepsiEkle == false)
					altBolumIdList.add(new SelectItem(null, "Seçiniz"));
				donemPerList = null;
			}
			if (hepsiEkle)
				altBolumIdList.add(new SelectItem(-1L, "Hepsi"));
			if (list != null && !list.isEmpty()) {
				altBolumIdList.addAll(list);
				boolean eski = list.size() == 1;
				if (eski)
					seciliEkSaha4Id = (Long) altBolumIdList.get(0).getValue();
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

		} else
			seciliEkSaha4Id = null;

		aramaSecenekleri.setEkSaha4Id(seciliEkSaha4Id);
		aramaSecenekleri.setAltBolumIdList(altBolumIdList);

		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaGirisRaporAction() throws Exception {
		linkBordroAdres = null;
		try {
			aylikVardiyaPlanGiris("aylikPlanRapor", false);

		} catch (Exception e) {

			logger.error(e);
			e.printStackTrace();
		}

	}

	/**
	 * @param calistigiSayfa
	 * @param planGirisiDurum
	 * @return
	 * @throws Exception
	 */
	private void aylikVardiyaPlanGiris(String calistigiSayfa, boolean planGirisiDurum) throws Exception {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		session.setFlushMode(FlushMode.MANUAL);
		setManuelKapi();
		bordroAlanKapat();
		seciliBolum = null;
		seciliAltBolum = null;
		fazlaMesaiOde = false;
		fazlaMesaiIzinKullan = false;
		adminRoleDurum(authenticatedUser);
		loginUser = authenticatedUser;
		loginUser.setLogin(authenticatedUser != null);
		aylikHareketKaydiVardiyaBul = Boolean.FALSE;
		fillEkSahaTanim();
		Calendar cal = Calendar.getInstance();
		yil = cal.get(Calendar.YEAR);
		ay = cal.get(Calendar.MONTH) + 1;
		cal.add(Calendar.MONTH, 1);
		maxYil = cal.get(Calendar.YEAR);

		denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
		if (denklestirmeAy == null) {
			try {
				ortakIslemler.yilAyKontrol(yil, null, session);
			} catch (Exception e) {
			}

		}
		setAylikPuantajDonem(denklestirmeAy);

		setDonusAdres("");
		denklestirmeAyDurum = Boolean.FALSE;
		modelList = new ArrayList<CalismaModeliAy>();
		if (fazlaMesaiTalepler == null)
			fazlaMesaiTalepler = new ArrayList<FazlaMesaiTalep>();
		else
			fazlaMesaiTalepler.clear();

		if (personelDenklestirmeDinamikAlanList == null)
			personelDenklestirmeDinamikAlanList = new ArrayList<PersonelDenklestirmeDinamikAlan>();
		else
			personelDenklestirmeDinamikAlanList.clear();

		if (vardiyaFazlaMesaiMap == null)
			vardiyaFazlaMesaiMap = new TreeMap<Long, List<FazlaMesaiTalep>>();
		else
			vardiyaFazlaMesaiMap.clear();
		try {
			departmanBolumAyni = Boolean.FALSE;
			aramaSecenekleri = new AramaSecenekleri(authenticatedUser);
			stajerSirket = Boolean.FALSE;
			boolean ayniSayfa = authenticatedUser.getCalistigiSayfa() != null && authenticatedUser.getCalistigiSayfa().equals(calistigiSayfa);
			if (!ayniSayfa)
				authenticatedUser.setCalistigiSayfa(calistigiSayfa);
			setPlanGirisi(planGirisiDurum);
			islemYapiliyor = Boolean.FALSE;
			hastaneSuperVisor = authenticatedUser.isDirektorSuperVisor();

			aramaSecenekleri.setTesisList(null);
			aramaSecenekleri.setTesisId(null);
			aramaSecenekleri.setGorevYeriList(null);
			aramaSecenekleri.setEkSaha3Id(null);
			aramaSecenekleri.setSirketId(null);
			gorevYeri = null;

			if (aylikVardiyaOzetList != null)
				aylikVardiyaOzetList.clear();
			else
				aylikVardiyaOzetList = new ArrayList<VardiyaGun>();

			if (!authenticatedUser.isAdmin() && !authenticatedUser.isIK())
				planDepartman = authenticatedUser.getPdksPersonel().getEkSaha1() != null ? authenticatedUser.getPdksPersonel().getEkSaha1() : null;
			else
				planDepartman = null;

			aylar = ortakIslemler.getAyListesi(Boolean.TRUE);
			aylikPuantajList.clear();

			sonDonem = (maxYil * 100) + cal.get(Calendar.MONTH) + 1;
			if (basTarih == null) {
				cal = Calendar.getInstance();
				cal.add(Calendar.DATE, -7);
				cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				setBasTarih(PdksUtil.getDate(cal.getTime()));
			}
			setBitTarih(null);

			if (!authenticatedUser.isAdmin())
				setDepartman(authenticatedUser.getDepartman());
			aramaSecenekleri.setDepartman(authenticatedUser.getDepartman());
			aramaSecenekleri.setDepartmanId(authenticatedUser.getDepartman().getId());
			setDepartman(authenticatedUser.getDepartman());
			setVardiyaPlanList(new ArrayList<VardiyaPlan>());
			HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
			boolean perIdVar = true;
			String perIdStr = (String) req.getParameter("perId");
			String dateStr = (String) req.getParameter("tarih");
			String planKey = (String) req.getParameter("planKey");

			if (planKey != null) {
				HashMap<String, String> veriMap = PdksUtil.getDecodeMapByBase64(planKey);
				if (veriMap.containsKey("tarih"))
					dateStr = veriMap.get("tarih");
				if (veriMap.containsKey("perId"))
					perIdStr = veriMap.get("perId");
				veriMap = null;
			}

			LinkedHashMap<String, Object> veriLastMap = null;
			AramaSecenekleri saveAramaSecenekleri = null;
			Long tesisId = null, ekSaha4Id = null;
			String doldurStr = null;
			plansiz = false;
			if (planKey == null) {
				veriLastMap = ortakIslemler.getLastParameter(calistigiSayfa, session);
				if (veriLastMap != null) {
					if (!veriLastMap.isEmpty())
						doldurStr = "V";
					if (veriLastMap.containsKey("yil") && veriLastMap.containsKey("ay") && veriLastMap.containsKey("bolumId") && veriLastMap.containsKey("sirketId")) {
						if (bordroAdres != null)
							linkBordroAdres = bordroAdres;
						yil = Integer.parseInt((String) veriLastMap.get("yil"));
						ay = Integer.parseInt((String) veriLastMap.get("ay"));
						aramaSecenekleri.setEkSaha3Id(Long.parseLong((String) veriLastMap.get("bolumId")));
						Long sId = Long.parseLong((String) veriLastMap.get("sirketId"));
						aramaSecenekleri.setSirketId(sId);
						if (veriLastMap.containsKey("departmanId") && adminRole) {
							Long departmanId = Long.parseLong((String) veriLastMap.get("departmanId"));
							aramaSecenekleri.setDepartmanId(departmanId);
						}
						if (veriLastMap.containsKey("altBolumId")) {
							ekSaha4Id = Long.parseLong((String) veriLastMap.get("altBolumId"));
							aramaSecenekleri.setEkSaha4Id(ekSaha4Id);
						}
						if (veriLastMap.containsKey("tesisId")) {
							tesisId = Long.parseLong((String) veriLastMap.get("tesisId"));
							aramaSecenekleri.setTesisId(tesisId);
						}
						if (veriLastMap.containsKey("veriDoldur")) {
							doldurStr = (String) veriLastMap.get("veriDoldur");

						}
						if (veriLastMap.containsKey("plansiz")) {
							plansiz = true;

						}

						if ((ikRole) && veriLastMap.containsKey("sicilNo"))
							sicilNo = (String) veriLastMap.get("sicilNo");
						if (yil > 0 && ay > 0 && aramaSecenekleri.getEkSaha3Id() != null) {
							dateStr = String.valueOf(yil * 100 + ay) + "01";
							if (doldurStr == null || !doldurStr.equalsIgnoreCase("F")) {
								perIdStr = "0";
								perIdVar = false;
							}

							else
								setDonusAdres(planGirisiDurum ? linkAdres : "");
							saveAramaSecenekleri = (AramaSecenekleri) aramaSecenekleri.clone();
						}

					}

				}
			}

			if (dateStr != null && perIdStr != null) {
				setDonusAdres(planGirisiDurum ? linkAdres : "");
				if (linkBordroAdres != null)
					doldurStr = "F";
				Date vardiyaDate = PdksUtil.convertToJavaDate(dateStr, "yyyyMMdd");
				cal.setTime(vardiyaDate);
				yil = cal.get(Calendar.YEAR);
				ay = cal.get(Calendar.MONTH) + 1;
				PersonelKGS personelKGS = perIdVar ? (PersonelKGS) pdksEntityController.getSQLParamByFieldObject(PersonelKGS.TABLE_NAME, PersonelKGS.COLUMN_NAME_PERSONEL_ID, new Long(perIdStr), PersonelKGS.class, session) : null;
				PersonelView personelView = personelKGS != null ? personelKGS.getPersonelView() : null;
				islemYapiliyor = false;
				if (personelView != null && personelView.getPdksPersonel() != null) {
					doldurStr = "F";
					Personel personel = personelView.getPdksPersonel();
					if (personel.getEkSaha4() != null) {
						ekSaha4Id = personel.getEkSaha4().getId();
						aramaSecenekleri.setEkSaha4Id(ekSaha4Id);
					}
					Sirket sirket = personel.getSirket();
					if (sirket != null) {
						aramaSecenekleri.setDepartmanId(sirket.getDepartman().getId());
						aramaSecenekleri.setSirketId(sirket.getId());

						if (personel.getTesis() != null)
							aramaSecenekleri.setTesisId(personel.getTesis().getId());

						if (personel.getEkSaha3() != null)
							aramaSecenekleri.setEkSaha3Id(personel.getEkSaha3().getId());
					}

					sicilNo = personel.getPdksSicilNo();
				}
				if (saveAramaSecenekleri != null) {
					aramaSecenekleri.setEkSaha3Id(saveAramaSecenekleri.getEkSaha3Id());
					if (saveAramaSecenekleri.getTesisId() != null)
						aramaSecenekleri.setTesisId(saveAramaSecenekleri.getTesisId());
				}

			}
			setSeciliDenklestirmeAy();
			aylariDoldur();
			if (doldurStr != null) {
				Long sirketId = aramaSecenekleri.getSirketId();
				fillSirketList();
				if (sirketId != null && (aramaSecenekleri.getSirketIdList() == null || aramaSecenekleri.getSirketIdList().size() != 1))
					aramaSecenekleri.setSirketId(sirketId);
				tesisDoldur(false);
				bolumDoldur();
				if (doldurStr.equals("F")) {
					planGirisiDurum = planGirisiDurum && aramaSecenekleri.getEkSaha3Id() != null;
					if (planGirisiDurum)
						fillStartAylikVardiyaPlanList();
				} else
					setDonusAdres("");
			} else {
				fillSirketList();
				if (aramaSecenekleri.getSirketId() == null) {
					if (aramaSecenekleri.getSirketIdList() != null && !aramaSecenekleri.getSirketIdList().isEmpty()) {
						aramaSecenekleri.setSirketId((Long) aramaSecenekleri.getSirketIdList().get(0).getValue());

						Sirket sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, aramaSecenekleri.getSirketId(), Sirket.class, session);

						ekSaha4Tanim = ortakIslemler.getEkSaha4(sirket, null, session);

					}

				}
				if (aramaSecenekleri.getSirketId() != null) {
					tesisDoldur(true);
				}

			}
			if (!ayniSayfa)
				authenticatedUser.setCalistigiSayfa("");
			fileImportKontrol();

			if (donusAdres != null && donusAdres.equals(calistigiSayfa))
				setDonusAdres("");

		} catch (Exception e) {
			e.printStackTrace();
		}

		kullaniciPersonel = ortakIslemler.getKullaniciPersonel(authenticatedUser);
		if (kullaniciPersonel)
			sicilNo = authenticatedUser.getPdksPersonel().getPdksSicilNo();
		authenticatedUser.setCalistigiSayfa(calistigiSayfa);

	}

	/**
	 * @param vardiyaGun
	 * @param vardiyalar
	 * @param tatilVardiyalar
	 * @param vardiya
	 * @return
	 */
	public VardiyaGun vardiyaListeDegistir(VardiyaGun vardiyaGun, ArrayList<Vardiya> vardiyalar, ArrayList<Vardiya> tatilVardiyalar, Vardiya vardiya) {
		TreeMap<String, Tatil> tatillerMap = getTatilMap();
		String key = PdksUtil.convertToDateString(vardiyaGun.getVardiyaDate(), "yyyyMMdd");
		vardiyaGun.setTatil(null);
		if (tatillerMap.containsKey(key))
			vardiyaGun.setTatil(tatillerMap.get(key));
		if (vardiyaGun.getVardiya() != null) {
			if (vardiya != null) {
				vardiyaGun.setVardiya(vardiya);
				if (vardiya.isCalisma()) {
					vardiyaGun.setBasSaat((int) vardiya.getBasSaat());
					vardiyaGun.setBasDakika((int) vardiya.getBasDakika());
					vardiyaGun.setBitSaat((int) vardiya.getBitSaat());
					vardiyaGun.setBitDakika((int) vardiya.getBitDakika());
				} else {
					vardiyaGun.setBasSaat(null);
					vardiyaGun.setBasDakika(null);
					vardiyaGun.setBitSaat(null);
					vardiyaGun.setBitDakika(null);
				}
			}
			if (vardiyaGun.getTatil() == null || vardiyaGun.getTatil().isYarimGunMu())
				vardiyaGun.setVardiyalar(vardiyalar);
			else
				vardiyaGun.setVardiyalar(tatilVardiyalar);
		}
		return vardiyaGun;
	}

	/**
	 * @param donemi
	 * @param perList
	 * @return
	 */
	public List<PersonelDenklestirme> getPersonelDenklestirme(DepartmanDenklestirmeDonemi donemi, List<Personel> perList) {
		List<PersonelDenklestirme> list = null;
		return list;
	}

	/**
	 * @param hafta
	 */
	public void personelSablonGuncelle(VardiyaHafta hafta) {
		setDegisti(Boolean.TRUE);
		VardiyaSablonu vardiyaSablonu = hafta.getVardiyaSablonu();
		boolean vardiyaDegistir = Boolean.TRUE;
		ArrayList<Vardiya> tatilVardiyaList = null;
		try {
			tatilVardiyaList = fillVardiyaDoldurList(vardiyaSablonu, vardiyaPlan.getPersonel());

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
		}
		ArrayList<Vardiya> vardiyaList = new ArrayList<Vardiya>();
		for (Vardiya vardiya : tatilVardiyaList)
			vardiyaList.add(vardiya);
		int calismaGunSayisi = vardiyaSablonu.getCalismaGunSayisi();
		double toplamSaat = vardiyaSablonu.getToplamSaat();
		for (Iterator<Vardiya> iterator = hafta.getVardiyalar().iterator(); iterator.hasNext();) {
			Vardiya vardiya = iterator.next();
			if (vardiya.isCalisma() && vardiya.getCalismaGun() == calismaGunSayisi && toplamSaat == vardiya.getCalismaSaati()) {
				vardiyaDegistir = Boolean.FALSE;
				break;
			}
		}
		hafta.setVardiyalar(vardiyaList);
		vardiyaListeDegistir(hafta.getVardiyaGun1(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya1() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun2(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya2() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun3(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya3() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun4(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya4() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun5(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya5() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun6(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya6() : null);
		vardiyaListeDegistir(hafta.getVardiyaGun7(), vardiyaList, tatilVardiyaList, vardiyaDegistir ? vardiyaSablonu.getVardiya7() : null);

	}

	/**
	 * @param personel
	 */
	public void fillVardiyaSablonList(Personel personel) {
		List<VardiyaSablonu> sablonList = null;
		try {

			Departman departman = personel.getSirket() != null ? personel.getSirket().getDepartman() : null;

			sablonList = pdksEntityController.getSQLParamByFieldList(VardiyaSablonu.TABLE_NAME, VardiyaSablonu.COLUMN_NAME_DURUM, Boolean.TRUE, VardiyaSablonu.class, session);

			if (departman != null) {
				for (Iterator iterator = sablonList.iterator(); iterator.hasNext();) {
					VardiyaSablonu pdksVardiyaSablonu = (VardiyaSablonu) iterator.next();
					if (pdksVardiyaSablonu.getDepartman() != null && !departman.getId().equals(pdksVardiyaSablonu.getDepartman().getId()))
						iterator.remove();
				}
			}
			if (sablonList != null)
				sablonList.clear();
			else
				sablonList = new ArrayList<VardiyaSablonu>();
			if (!sablonList.isEmpty())
				sablonList.addAll(sablonList);

		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}
		sablonList = null;

	}

	/**
	 * @param personel
	 * @return
	 */
	private boolean helpPersonel(Personel personel) {
		return personel != null && gorevliPersonelMap != null && gorevliPersonelMap.containsKey(personel.getPdksSicilNo());

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
	 * @param list
	 * @param vardiyaPlan
	 * @throws Exception
	 */
	protected void denklestirmeVerileriOlustur(List<VardiyaPlan> list, VardiyaPlan vardiyaPlan) throws Exception {
		if (list == null)
			list = new ArrayList<VardiyaPlan>();
		if (vardiyaPlan != null)
			list.add(vardiyaPlan);
		if (!list.isEmpty()) {
			HashMap<Long, VardiyaPlan> hashMap = new HashMap<Long, VardiyaPlan>();
			for (VardiyaPlan pdksVardiyaPlan : list)
				hashMap.put(pdksVardiyaPlan.getPersonel().getId(), pdksVardiyaPlan);
			DepartmanDenklestirmeDonemi denklestirmeDonemi = (DepartmanDenklestirmeDonemi) departmanDenklestirmeDonemi.clone();
			denklestirmeDonemi.setDenklestirmeAy(denklestirmeAy);
			denklestirmeDonemi.setDenklestirmeAyDurum(denklestirmeAyDurum);
			Calendar cal = Calendar.getInstance();
			for (int i = 0; i < 3; i++) {
				if (denklestirmeDonemi.getBaslangicTarih() != null && PdksUtil.tarihKarsilastirNumeric(denklestirmeDonemi.getBitisTarih(), denklestirmeDonemi.getBaslangicTarih()) == 1) {
					tatilGunleriMap = ortakIslemler.getTatilGunleri(new ArrayList<Personel>(), ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBaslangicTarih(), -1), ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBitisTarih(), 1), session);
					List<PersonelDenklestirmeTasiyici> personelDenklestirmeList = ortakIslemler.personelDenklestir(denklestirmeDonemi, tatilGunleriMap, "id", new ArrayList<Long>(hashMap.keySet()), Boolean.FALSE, Boolean.TRUE, Boolean.FALSE, session);
					for (PersonelDenklestirmeTasiyici personelDenklestirme : personelDenklestirmeList) {
						if (hashMap.containsKey(personelDenklestirme.getPersonel().getId())) {

							if (personelDenklestirme.getVardiyaGunleriMap() != null) {
								List<VardiyaGun> list2 = new ArrayList<VardiyaGun>(personelDenklestirme.getVardiyaGunleriMap().values());
								for (VardiyaGun pdksVardiyaGun : list2) {
									if (pdksVardiyaGun.getVardiya() == null)
										continue;

								}
							}

							hashMap.get(personelDenklestirme.getPersonel().getId()).setPersonelDenklestirme(personelDenklestirme, i);

						}
					}
				}
				Date tarih = ortakIslemler.tariheGunEkleCikar(cal, denklestirmeDonemi.getBitisTarih(), 7);
				denklestirmeDonemi.setBitisTarih(tarih);
				denklestirmeDonemi.setBaslangicTarih(ortakIslemler.tariheGunEkleCikar(cal, tarih, -6));

			}

		}

	}

	/**
	 * 
	 */
	private void fileImportKontrol() throws Exception {
		fileImport = Boolean.FALSE;
		if (aylikPuantajDosyaList == null)
			aylikPuantajDosyaList = new ArrayList<AylikPuantaj>();
		boolean fileImportDurum = userHome.hasPermission("vardiyaPlani", "fileImportPlan");
		String fileImportStr = ortakIslemler.getParameterKey("fileImport");
		if (ay > 0 && aramaSecenekleri.getSirketId() != null && (fileImportDurum || fileImportStr.equals("1") || (ikRole && fileImportStr.equalsIgnoreCase("ik")))) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, 1);
			int donem = (cal.get(Calendar.YEAR) * 100) + cal.get(Calendar.MONTH) + 1;
			if (yil * 100 + ay <= donem) {

				denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
				setAylikPuantajDonem(denklestirmeAy);
				setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
				fileImport = denklestirmeAy != null && denklestirmeAyDurum;
			}

		}
		if (fileImport) {

			fileImportDosyaSifirla();

		}

	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String fileImportDosyaSifirla() throws Exception {
		aylikPuantajDosyaList.clear();
		vardiyaPlanDosya.setDosyaIcerik(null);
		degisti = Boolean.FALSE;
		onayDurum = Boolean.FALSE;
		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public void listenerFileImportDosya(UploadEvent event) throws Exception {
		UploadItem item = event.getUploadItem();
		PdksUtil.getDosya(item, vardiyaPlanDosya);
		aylikPuantajDosyaList.clear();
	}

	/**
	 * @return
	 */
	public String durumDegistir() {
		for (AylikPuantaj aylikPuantaj : aylikPuantajDosyaList) {
			if (aylikPuantaj.isKaydet()) {
				aylikPuantaj.setSecili(onayDurum);
			}
		}
		return "";
	}

	/**
	 * @return
	 * @throws Exception
	 */
	@Transactional
	public String vardiyaPlanDosyaYaz() throws Exception {
		Calendar cal = Calendar.getInstance();
		boolean islemYapildi = Boolean.FALSE;
		Date sonGun = ortakIslemler.tariheAyEkleCikar(cal, aylikPuantajDefault.getIlkGun(), 1);
		List<VardiyaGun> vardiyaGunleri = new ArrayList<VardiyaGun>();

		List<String> perNoList = new ArrayList<String>();
		List<Long> perIdList = new ArrayList<Long>();
		HashMap<Long, HashMap<String, Boolean>> calismaModeliVardiyaOzelMap = new HashMap<Long, HashMap<String, Boolean>>();
		TreeMap<Long, TreeMap<Long, Vardiya>> calismaModeliMap = getCalismaModeliMap(aylikPuantajDosyaList, calismaModeliVardiyaOzelMap);
		boolean flush = false, hata = false;
		for (Iterator iterator = aylikPuantajDosyaList.iterator(); iterator.hasNext();) {
			AylikPuantaj aylikPuantaj = (AylikPuantaj) iterator.next();

			if (aylikPuantaj.isKaydet() && aylikPuantaj.isSecili()) {
				boolean devam = true;
				islemYapildi = Boolean.TRUE;
				TreeMap<Long, Vardiya> vardiyaMap = null;
				if (aylikPuantaj.getCalismaModeli() != null) {
					Long id = aylikPuantaj.getCalismaModeli().getId();
					if (calismaModeliMap.containsKey(id))
						vardiyaMap = calismaModeliMap.get(id);
				}
				setPersonelAylikPuantaj(aylikPuantaj);
				Personel personel = (Personel) aylikPuantaj.getPdksPersonel().clone();
				boolean calisiyor = personel.getIseGirisTarihi().before(aylikPuantajDefault.getIlkGun()) && personel.getIstenAyrilisTarihi().after(sonGun);
				personel.setDurum(Boolean.TRUE);
				VardiyaPlan pdksVardiyaPlan = aylikPuantaj.getVardiyaPlan();

				for (VardiyaGun pdksVardiyaGun : aylikPuantaj.getVardiyalar()) {
					if (pdksVardiyaGun.isAyinGunu()) {
						if (!calisiyor && !personel.isCalisiyorGun(pdksVardiyaGun.getVardiyaDate()))
							continue;
						long basVardiyaId = (pdksVardiyaGun.getVardiya() != null ? pdksVardiyaGun.getVardiya().getIdLong() : 0L), bitVardiyaId = (pdksVardiyaGun.getYeniVardiya() != null ? pdksVardiyaGun.getYeniVardiya().getIdLong() : 0L);
						if (basVardiyaId != bitVardiyaId) {
							Vardiya vardiyaYeni = pdksVardiyaGun.getYeniVardiya();
							if (vardiyaYeni != null) {
								Boolean kaydet = true;
								if (vardiyaYeni.isGebelikMi() && pdksVardiyaGun.isGebeMi() == false) {
									kaydet = false;
									devam = false;
									PdksUtil.addMessageAvailableWarn(personel.getAdSoyad() + " " + authenticatedUser.dateFormatla(pdksVardiyaGun.getVardiyaDate()) + " tarihinde " + vardiyaYeni.getKisaAdi() + " seçemezsiniz!");

								}
								if (kaydet) {
									pdksVardiyaGun.setVardiya(vardiyaYeni);
									pdksVardiyaGun.setIslendi(Boolean.FALSE);
									pdksVardiyaGun.setIslemVardiya(null);
									pdksVardiyaGun.setIslemVardiyaZamani();
									saveOrUpdate(pdksVardiyaGun);
									flush = true;
									vardiyaGunleri.add(pdksVardiyaGun);
									try {
										pdksVardiyaGun.setIslemVardiya(null);
										pdksVardiyaGun.setIslendi(Boolean.FALSE);
										pdksVardiyaGun.setIslemVardiyaZamani();
									} catch (Exception ex) {
										logger.error(ex);
										ex.printStackTrace();
									}
								}

							} else if (pdksVardiyaGun.getId() != null) {

								pdksEntityController.deleteObject(session, entityManager, pdksVardiyaGun);
								flush = true;
							}
						}

					}
				}
				boolean hataOlustu = Boolean.FALSE;
				try {
					PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
					if (vardiyaPlanKontrol(personelDenklestirme, calismaModeliVardiyaOzelMap, vardiyaMap, pdksVardiyaPlan, personel.getSicilNo() + " " + personel.getAdSoyad() + " ", true)) {
						// PersonelDenklestirme personelDenklestirme = aylikPuantaj.getPersonelDenklestirme();
						perIdList.add(personel.getId());
						aylikPuantaj.setSecili(Boolean.FALSE);
						perNoList.add(aylikPuantaj.getPdksPersonel().getPdksSicilNo());
					} else
						hataOlustu = Boolean.TRUE;

				} catch (Exception e) {
					logger.equals(e);
					e.printStackTrace();
					hataOlustu = Boolean.TRUE;

				}
				if (hataOlustu) {
					for (VardiyaGun pdksVardiyaGun : vardiyaGunleri) {
						Vardiya yeniVardiya = pdksVardiyaGun.getYeniVardiya();
						session.refresh(pdksVardiyaGun);
						pdksVardiyaGun.setYeniVardiya(yeniVardiya);
					}
				}
				if (flush) {
					sessionFlush();
					if (devam)
						vardiyaGunleri.clear();
				}
				if (devam)
					iterator.remove();
				else
					hata = true;
			}
		}

		if (!islemYapildi)
			PdksUtil.addMessageWarn("İşlem yapılacak kayıt seçiniz!");
		else if (hata == false) {
			if (!perIdList.isEmpty()) {
				String fieldName = "p";
				HashMap fields = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select S.* from " + PersonelDenklestirme.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
				sb.append(" where S." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId() + " and (S." + PersonelDenklestirme.COLUMN_NAME_DURUM + " = 1 or S." + PersonelDenklestirme.COLUMN_NAME_ONAYLANDI + " = 1 )");
				sb.append(" and S." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " :" + fieldName);
				fields.put(fieldName, perIdList);
				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
				// List<PersonelDenklestirme> list = pdksEntityController.getObjectBySQLList(sb, fields, PersonelDenklestirme.class);
				List<PersonelDenklestirme> list = pdksEntityController.getSQLParamList(perIdList, sb, fieldName, fields, PersonelDenklestirme.class, session);
				flush = false;
				for (PersonelDenklestirme personelDenklestirme : list) {
					savePersonelDenklestirme(personelDenklestirme);
					flush = true;

				}
				if (flush)
					try {
						sessionFlush();
					} catch (Exception e) {
					}

			}
			try {
				if (!perNoList.isEmpty()) {
					onayDurum = Boolean.FALSE;
					vardiyaPlanDosyaOku();
					if (aylikPuantajList != null && !aylikPuantajList.isEmpty()) {
						for (AylikPuantaj aylikPuantaj : aylikPuantajList) {
							if (perNoList.contains(aylikPuantaj.getPdksPersonel().getPdksSicilNo())) {
								fillAylikVardiyaPlanList();
								break;
							}
						}

					}
					aylikPuantajDosyaList.clear();
				}

			} catch (Exception e) {
			}

		}
		perNoList = null;
		return "";
	}

	@Transactional
	public String saveSablonGuncelle() {
		Personel personel = personelAylikPuantaj.getPdksPersonel();
		VardiyaSablonu vardiyaSablonu = personel.getSablon();
		List<Vardiya> vardiyaList = vardiyaSablonu.getVardiyaList();
		TreeMap<Integer, Vardiya> vardiyaMap = new TreeMap<Integer, Vardiya>();
		for (int i = 1; i <= vardiyaList.size(); i++) {
			Vardiya vardiya = vardiyaList.get(i - 1);
			int index = i == 7 ? Calendar.SUNDAY : i + 1;
			vardiyaMap.put(index, vardiya);
		}
		vardiyaList = null;
		for (VardiyaHafta vh : personelAylikPuantaj.getVardiyaHaftaList()) {
			for (VardiyaGun vg : vh.getVardiyaGunler()) {
				Vardiya vardiya = vg.getVardiya();
				if (vg.isAyinGunu() && vardiya != null) {
					int index = vg.getHaftaninGunu();
					if (vardiyaMap.containsKey(index)) {
						Vardiya sablonVardiya = vardiyaMap.get(index);
						if (!sablonVardiya.getId().equals(vardiya.getId())) {
							vg.setVardiya(sablonVardiya);
							vardiyalarMap.put(vg.getVardiyaKeyStr(), vg);
							kartBasmayanPersonel = false;
						}
					}
				}
			}
		}
		vardiyaMap = null;
		return "";
	}

	/**
	 * @param personelDenklestirme
	 */

	private void savePersonelDenklestirme(PersonelDenklestirme personelDenklestirme) {
		personelDenklestirme.setOnaylandi(Boolean.FALSE);
		personelDenklestirme.setGuncellemeTarihi(new Date());
		personelDenklestirme.setDevredenSure(null);
		saveOrUpdate(personelDenklestirme);
		if (personelDenklestirme.getPersonelDenklestirmeGecenAy() == null && personelDenklestirme.getCalismaModeliAy() != null) {
			try {
				CalismaModeli cm = personelDenklestirme.getCalismaModeliAy().getCalismaModeli();
				if (cm != null) {
					Personel personel = personelDenklestirme.getPdksPersonel();
					if (personel.getCalismaModeli() == null || !personel.getCalismaModeli().getId().equals(cm.getId())) {
						personel.setCalismaModeli(cm);
						if (authenticatedUser.isAdmin() == false) {
							personel.setGuncellemeTarihi(new Date());
							personel.setGuncelleyenUser(authenticatedUser);
						}
						saveOrUpdate(personel);
					}
				}

			} catch (Exception e) {

			}

		}
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String vardiyaPlanDosyaOku() throws Exception {
		aylikPuantajDosyaList.clear();
		onayDurum = Boolean.FALSE;
		degisti = Boolean.FALSE;
		Workbook wb = vardiyaPlanDosya.getDosyaIcerik() != null ? ortakIslemler.getWorkbook(vardiyaPlanDosya) : null;
		if (wb != null) {
			sessionClear();
			HashMap fields = new HashMap();
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			TreeMap<Long, TreeMap<String, IzinTipi>> calIzinMap = new TreeMap<Long, TreeMap<String, IzinTipi>>();
			List<IzinTipi> izinList = pdksEntityController.getSQLParamByFieldList(IzinTipi.TABLE_NAME, null, null, IzinTipi.class, session);
			for (IzinTipi izinTipi : izinList) {
				if (izinTipi.getKisaAciklama() != null) {
					Long key = izinTipi.getDepartman() != null ? izinTipi.getDepartman().getId() : 0L;
					TreeMap<String, IzinTipi> map1 = calIzinMap.containsKey(key) ? calIzinMap.get(key) : new TreeMap<String, IzinTipi>();
					if (map1.isEmpty())
						calIzinMap.put(key, map1);
					map1.put(izinTipi.getKisaAciklama(), izinTipi);
				}

			}
			fields.clear();
			StringBuffer sb = new StringBuffer();
			sb.append("select S.* from " + Vardiya.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
			sb.append(" where (S." + Vardiya.COLUMN_NAME_DEPARTMAN + " is null or S." + Vardiya.COLUMN_NAME_DEPARTMAN + " = :deptId )");
			sb.append(" and S." + Vardiya.COLUMN_NAME_KISA_ADI + " <> '' and S." + Vardiya.COLUMN_NAME_DURUM + " = 1 ");
			fields.put("deptId", departman.getId());
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<Vardiya> vardiyalar = pdksEntityController.getObjectBySQLList(sb, fields, Vardiya.class);
			TreeMap<String, Vardiya> vardiyaMap = new TreeMap<String, Vardiya>();
			for (Vardiya pdksVardiya : vardiyalar) {
				String key = PdksUtil.setTurkishStr(pdksVardiya.getKisaAdi()).toLowerCase(Locale.ENGLISH);
				vardiyaMap.put(key, pdksVardiya);
			}

			fields.clear();

			denklestirmeAy = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			setAylikPuantajDonem(denklestirmeAy);
			setDenklestirmeAyDurum(fazlaMesaiOrtakIslemler.getDurum(denklestirmeAy));
			fields.clear();
			Integer oncekiYil = null, oncekiAy = null;
			if (ay > 1) {
				oncekiYil = yil;
				oncekiAy = ay - 1;
				// fields.put("ay", ay - 1);
				// fields.put("yil", yil);
			} else {
				oncekiYil = yil - 1;
				oncekiAy = 12;
				// fields.put("ay", 12);
				// fields.put("yil", yil - 1);
			}

			denklestirmeOncekiAy = (DenklestirmeAy) ortakIslemler.getSQLDenklestirmeAy(oncekiYil, oncekiAy, session);
			if (denklestirmeOncekiAy != null && !denklestirmeOncekiAy.getDurum())
				denklestirmeOncekiAy = null;

			fields.clear();
			Integer sonrakiYil = null, sonrakiAy = null;
			if (ay < 12) {
				sonrakiYil = yil;
				sonrakiAy = ay + 1;
				// fields.put("ay", ay + 1);
				// fields.put("yil", yil);
			} else {
				sonrakiYil = yil + 1;
				sonrakiAy = 1;
				// fields.put("ay", 1);
				// fields.put("yil", yil + 1);
			}
			denklestirmeSonrakiAy = ortakIslemler.getSQLDenklestirmeAy(sonrakiYil, sonrakiAy, session);
			if (denklestirmeSonrakiAy != null && !denklestirmeSonrakiAy.getDurum())
				denklestirmeSonrakiAy = null;

			vardiyalar = null;
			if (!vardiyaMap.isEmpty() && denklestirmeAy != null) {
				departmanDenklestirmeDonemi = new DepartmanDenklestirmeDonemi();
				AylikPuantaj aylikPuantajSablon = fazlaMesaiOrtakIslemler.getAylikPuantaj(ay, yil, departmanDenklestirmeDonemi, session);
				List<VardiyaHafta> vardiyaHaftaList = new ArrayList<VardiyaHafta>();
				List<VardiyaGun> sablonVardiyalar = new ArrayList<VardiyaGun>();
				VardiyaPlan pdksVardiyaPlanMaster = fazlaMesaiOrtakIslemler.haftalikVardiyaOlustur(vardiyaHaftaList, aylikPuantajSablon, departmanDenklestirmeDonemi, tatilGunleriMap, sablonVardiyalar);
				aylikPuantajSablon.setVardiyaHaftaList(pdksVardiyaPlanMaster.getVardiyaHaftaList());
				setAylikPuantajDefault(aylikPuantajSablon);
				String donem = String.valueOf(yil * 100 + ay);
				for (VardiyaGun pdksVardiyaGun : sablonVardiyalar)
					pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(donem));
				TreeMap<String, String> perMap = new TreeMap<String, String>();
				LinkedHashMap<String, List<String>> vardiyaListMap = new LinkedHashMap<String, List<String>>();
				Sheet sheet = wb.getSheetAt(0);
				int COL_SICIL_NO = 0;
				int COL_ADI_SOYADI = 1;
				String perSicilNo = null, adiSoyadi = null;
				int gunSayisi = COL_ADI_SOYADI + 2, maxGun = aylikPuantajSablon.getGunSayisi();
				Boolean devam = Boolean.TRUE;
				String sicilNoUzunlukStr = ortakIslemler.getParameterKey("sicilNoUzunluk");
				int maxTextLength = 0;
				try {
					if (PdksUtil.hasStringValue(sicilNoUzunlukStr))
						maxTextLength = Integer.parseInt(sicilNoUzunlukStr);
				} catch (Exception e) {
					maxTextLength = 0;
				}
				int baslangic = 0;
				String str = null;
				Integer baslangicYer = null;

				while (devam) {
					try {
						str = ExcelUtil.getSheetStringValue(sheet, baslangic, gunSayisi++);
						String[] gunler = str.indexOf("\n") > 0 ? str.split("\n") : str.split(" ");
						if (gunler.length == 2)
							maxGun = Integer.parseInt(gunler[0]);
						else
							maxGun = Integer.parseInt(str);
						if (baslangicYer == null)
							baslangicYer = gunSayisi - 1;

					} catch (Exception e) {
						if (baslangic == 0) {
							if (gunSayisi < 28) {
								baslangic = 2;
								gunSayisi = COL_ADI_SOYADI + 2;
							} else
								devam = false;
						} else {
							if (str != null)
								logger.debug(str + " " + maxGun);
							if (baslangicYer != null)
								devam = false;
						}

					}
				}
				if (baslangicYer == null)
					baslangicYer = COL_ADI_SOYADI + 2;
				if (aylikPuantajSablon.getGunSayisi() == maxGun) {
					List<String> perList = new ArrayList<String>();
					String vardiyaAdi = "";
					for (int row = baslangic + 1; row <= sheet.getLastRowNum(); row++) {
						try {
							perSicilNo = ExcelUtil.getSheetStringValue(sheet, row, COL_SICIL_NO);
							if (!PdksUtil.hasStringValue(perSicilNo))
								break;

							adiSoyadi = ExcelUtil.getSheetStringValue(sheet, row, COL_ADI_SOYADI);
							if (!PdksUtil.hasStringValue(adiSoyadi))
								break;
							if (maxTextLength > 0 && perSicilNo != null && perSicilNo.trim().length() < maxTextLength)
								perSicilNo = PdksUtil.textBaslangicinaKarakterEkle(perSicilNo, '0', maxTextLength);
							perMap.put(perSicilNo, adiSoyadi);
							List<String> list = new ArrayList<String>();
							for (int i = 0; i < maxGun; i++) {
								vardiyaAdi = ExcelUtil.getSheetStringValueTry(sheet, row, baslangicYer + i);
								String key = null;
								if (PdksUtil.hasStringValue(vardiyaAdi)) {
									key = PdksUtil.setTurkishStr(vardiyaAdi).toLowerCase(Locale.ENGLISH);
									if (!vardiyaMap.containsKey(key))
										key = vardiyaAdi;
								}
								list.add(key);
							}
							vardiyaListMap.put(perSicilNo, list);
							perList.add(perSicilNo);
						} catch (Exception e1) {
							logger.error(perSicilNo + " " + e1);
							e1.printStackTrace();
						}
					}
					if (!vardiyaListMap.isEmpty() && vardiyaListMap.size() == perMap.size()) {
						Date sonGun = null;
						if (aylikPuantajSablon.getVardiyalar() != null && !aylikPuantajSablon.getVardiyalar().isEmpty())
							sonGun = aylikPuantajSablon.getVardiyalar().get(aylikPuantajSablon.getVardiyalar().size() - 1).getVardiyaDate();

						if (sonGun == null) {
							Calendar cal = Calendar.getInstance();
							cal.setTime(aylikPuantajSablon.getIlkGun());
							cal.set(Calendar.DATE, cal.getActualMaximum(Calendar.DAY_OF_MONTH));
							sonGun = cal.getTime();
						}
						List dataIdList = new ArrayList(perMap.keySet());
						String fieldName = "p";
						fields.clear();
						sb = new StringBuffer();
						sb.append("select S.* from " + Personel.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
						sb.append(" inner join " + Sirket.TABLE_NAME + " SI " + PdksEntityController.getJoinLOCK() + " on SI." + Sirket.COLUMN_NAME_ID + " = S." + Personel.COLUMN_NAME_SIRKET);
						sb.append(" and SI." + Sirket.COLUMN_NAME_DEPARTMAN + " = :deptId ");
						sb.append(" where S." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= :basTarih ");
						sb.append(" and S." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI + " <= :bitTarih ");
						sb.append(" and S." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);
						fields.put("basTarih", aylikPuantajSablon.getIlkGun());
						fields.put("deptId", departman.getId());
						fields.put("bitTarih", sonGun);
						fields.put(fieldName, dataIdList);
						fields.put(PdksEntityController.MAP_KEY_MAP, "getPdksSicilNo");
						if (session != null)
							fields.put(PdksEntityController.MAP_KEY_SESSION, session);
						// TreeMap<String, Personel> personelMap = pdksEntityController.getObjectBySQLMap(sb, fields, Personel.class, false);
						TreeMap<String, Personel> personelMap = pdksEntityController.getSQLParamTreeMap("getPdksSicilNo", false, dataIdList, sb, fieldName, fields, Personel.class, session);
						if (personelMap.size() == perMap.size()) {
							fieldName = "s";
							sb = new StringBuffer();
							sb.append("select D.* from " + PersonelDenklestirme.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK());
							sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = D." + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
							sb.append(" and P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);
							sb.append(" where D." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = :d");

							List idList = new ArrayList(perMap.keySet());
							fields.clear();
							fields.put("d", denklestirmeAy.getId());
							fields.put(fieldName, idList);
							if (session != null)
								fields.put(PdksEntityController.MAP_KEY_SESSION, session);
							List<PersonelDenklestirme> personelDenklestirmeList = pdksEntityController.getSQLParamList(idList, sb, fieldName, fields, PersonelDenklestirme.class, session);

							TreeMap<String, PersonelDenklestirme> personelDenklestirmeMap = new TreeMap<String, PersonelDenklestirme>();

							List<Long> modelIdList = new ArrayList<Long>();

							for (PersonelDenklestirme personelDenklestirme : personelDenklestirmeList) {
								if (personelDenklestirme.getCalismaModeliAy() != null && personelDenklestirme.getCalismaModeli() != null) {
									CalismaModeli calismaModeli = personelDenklestirme.getCalismaModeli();
									if (calismaModeli.getGenelVardiya().equals(Boolean.FALSE) && !modelIdList.contains(calismaModeli.getId()))
										modelIdList.add(calismaModeli.getId());
								}
								if (!personelDenklestirme.isErpAktarildi())
									personelDenklestirmeMap.put(personelDenklestirme.getPersonel().getPdksSicilNo(), personelDenklestirme);
							}
							HashMap<Long, List<Long>> calismaModeliVardiyaMap = new HashMap<Long, List<Long>>();
							HashMap<Long, Boolean> calismaModeliVardiyaOzelMap = new HashMap<Long, Boolean>();
							if (!modelIdList.isEmpty()) {

								List<CalismaModeliVardiya> calismaModeliVardiyaList = pdksEntityController.getSQLParamByFieldList(CalismaModeliVardiya.TABLE_NAME, CalismaModeliVardiya.COLUMN_NAME_CALISMA_MODELI, modelIdList, CalismaModeliVardiya.class, session);

								for (CalismaModeliVardiya calismaModeliVardiya : calismaModeliVardiyaList) {
									if (calismaModeliVardiya.getVardiya().getDurum()) {
										Vardiya vardiya = calismaModeliVardiya.getVardiya();
										Long key = calismaModeliVardiya.getCalismaModeli().getId();
										List<Long> list = calismaModeliVardiyaMap.containsKey(key) ? calismaModeliVardiyaMap.get(key) : new ArrayList<Long>();
										if (list.isEmpty()) {
											calismaModeliVardiyaMap.put(key, list);
											calismaModeliVardiyaOzelMap.put(key, !vardiya.getGenel());
										} else if (vardiya.getGenel().equals(Boolean.FALSE))
											calismaModeliVardiyaOzelMap.put(key, Boolean.TRUE);
										list.add(calismaModeliVardiya.getVardiya().getId());
									}
								}
							}
							modelIdList = null;
							personelDenklestirmeList = null;
							try {
								vardiyalarMap = ortakIslemler.getIslemVardiyalar(new ArrayList<Personel>(personelMap.values()), departmanDenklestirmeDonemi.getBaslangicTarih(), departmanDenklestirmeDonemi.getBitisTarih(), Boolean.TRUE, session, Boolean.FALSE);
							} catch (Exception ev) {
								logger.error(ev);
								ev.printStackTrace();
							}
							Date sonGunVardiya = (Date) sonGun.clone();
							for (String key : vardiyalarMap.keySet()) {
								VardiyaGun pdksVardiyaGun = vardiyalarMap.get(key);

								pdksVardiyaGun.setIslendi(Boolean.FALSE);
								pdksVardiyaGun.setGuncellendi(Boolean.FALSE);
								pdksVardiyaGun.saklaVardiya();
								if (pdksVardiyaGun.getIslemVardiya() == null)
									pdksVardiyaGun.setVardiyaZamani();
								if (pdksVardiyaGun.getIslemVardiya() != null)
									if (pdksVardiyaGun.getIslemVardiya().getVardiyaBitZaman().after(sonGunVardiya))
										sonGunVardiya = pdksVardiyaGun.getIslemVardiya().getVardiyaBitZaman();
							}
							fieldName = "s";
							idList = new ArrayList(perMap.keySet());
							fields.clear();
							sb = new StringBuffer();
							sb.append("select D.* from " + PersonelIzin.TABLE_NAME + " D " + PdksEntityController.getSelectLOCK());
							sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksEntityController.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = D." + PersonelIzin.COLUMN_NAME_PERSONEL);
							sb.append(" and P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);
							sb.append(" inner join " + IzinTipi.TABLE_NAME + " T " + PdksEntityController.getJoinLOCK() + " on T." + IzinTipi.COLUMN_NAME_ID + " = D." + PersonelIzin.COLUMN_NAME_IZIN_TIPI);
							sb.append(" and T." + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + " is null ");
							sb.append(" where D." + PersonelIzin.COLUMN_NAME_BITIS_ZAMANI + " >= :b1 and D." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI + " <= :b2");
							sb.append(" and D." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " not :d");
							fields.put("b1", aylikPuantajSablon.getIlkGun());
							fields.put("b2", sonGunVardiya);
							fields.put("d", Arrays.asList(new Integer[] { PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL, PersonelIzin.IZIN_DURUMU_REDEDILDI }));
							fields.put(fieldName, idList);
							if (session != null)
								fields.put(PdksEntityController.MAP_KEY_SESSION, session);
							List<PersonelIzin> izinler = pdksEntityController.getSQLParamList(idList, sb, fieldName, fields, PersonelIzin.class, session);

							HashMap<Long, List<PersonelIzin>> izinMap = new HashMap<Long, List<PersonelIzin>>();
							for (PersonelIzin izin : izinler) {
								Long id = izin.getIzinSahibi().getId();
								List<PersonelIzin> list = izinMap.containsKey(id) ? izinMap.get(id) : new ArrayList<PersonelIzin>();
								if (list.isEmpty()) {
									logger.debug(id);
									izinMap.put(id, list);
								}

								list.add(izin);
							}
							izinler = null;
							List<VardiyaGun> vardiyaList = new ArrayList<VardiyaGun>();

							for (String sicilNo : perList) {
								Personel personel = personelMap.get(sicilNo);
								Long depId = personel.getSirket().getDepartman().getId();
								TreeMap<String, IzinTipi> izinTipiMap = calIzinMap.containsKey(depId) ? calIzinMap.get(depId) : new TreeMap<String, IzinTipi>();
								boolean hataVar = Boolean.FALSE, renk = Boolean.TRUE;
								if (personel != null) {
									personel = (Personel) personelMap.get(sicilNo).clone();
									personel.setDurum(Boolean.TRUE);
									boolean calisiyor = personel.getIseGirisTarihi().before(aylikPuantajSablon.getIlkGun()) && personel.getIstenAyrilisTarihi().after(sonGun);
									AylikPuantaj aylikPuantajSablonNew = new AylikPuantaj();
									PersonelDenklestirme personelDenklestirmeAylik = personelDenklestirmeMap.containsKey(sicilNo) ? personelDenklestirmeMap.get(sicilNo) : null;
									aylikPuantajSablonNew.setPersonelDenklestirme(personelDenklestirmeAylik);
									aylikPuantajSablonNew.setKaydet(Boolean.FALSE);
									aylikPuantajSablonNew.setSecili(Boolean.FALSE);
									aylikPuantajSablonNew.setPdksPersonel(personel);
									aylikPuantajSablonNew.setVardiyalar(new ArrayList<VardiyaGun>());
									aylikPuantajSablonNew.setVardiyaPlan(new VardiyaPlan());
									aylikPuantajSablonNew.getVardiyaPlan().getVardiyaHaftaList().clear();
									List<VardiyaGun> puantajVardiyaGunler = new ArrayList<VardiyaGun>();
									devam = !devam;
									List<Long> vardiyaIdList = null;
									try {
										if (personelDenklestirmeAylik != null && personelDenklestirmeAylik.getCalismaModeliAy() != null && personelDenklestirmeAylik.getCalismaModeli() != null) {
											CalismaModeli calismaModeli = personelDenklestirmeAylik.getCalismaModeli();
											if (calismaModeliVardiyaMap.containsKey(calismaModeli.getId()))
												vardiyaIdList = calismaModeliVardiyaMap.get(calismaModeli.getId());

										}
									} catch (Exception ee2) {
										logger.error(ee2);
										ee2.printStackTrace();
									}

									for (Iterator iterator2 = sablonVardiyalar.iterator(); iterator2.hasNext();) {
										VardiyaGun pdksVardiyaGunMaster = (VardiyaGun) iterator2.next();
										VardiyaGun pdksVardiyaGun = new VardiyaGun(personel, null, pdksVardiyaGunMaster.getVardiyaDate());
										String vardiyaKey = pdksVardiyaGun.getVardiyaKey();
										if (vardiyalarMap.containsKey(vardiyaKey))
											pdksVardiyaGun = vardiyalarMap.get(vardiyaKey);
										else
											vardiyalarMap.put(vardiyaKey, pdksVardiyaGun);
										pdksVardiyaGun.setAyinGunu(pdksVardiyaGunMaster.isAyinGunu());
										String key = PdksUtil.convertToDateString(pdksVardiyaGun.getVardiyaDate(), "yyyyMMdd");
										if (tatilGunleriMap == null)
											tatilGunleriMap = ortakIslemler.getTatilGunleri(null, aylikPuantajSablon.getIlkGun(), sonGunVardiya, session);
										pdksVardiyaGun.setAyinGunu(pdksVardiyaGun.getVardiyaDateStr().startsWith(donem));
										if (tatilGunleriMap.containsKey(key))
											pdksVardiyaGun.setTatil(tatilGunleriMap.get(key));
										puantajVardiyaGunler.add(pdksVardiyaGun);

									}
									haftalikSablonOlustur(aylikPuantajSablon, false, false, null, personel, personel.getSablon(), aylikPuantajSablonNew, vardiyalarMap);
									aylikPuantajSablonNew.setVardiyaHaftaList(aylikPuantajSablonNew.getVardiyaPlan().getVardiyaHaftaList());
									aylikPuantajSablonNew.setVardiyalar(puantajVardiyaGunler);
									int gun = 0;
									List<String> list = vardiyaListMap.get(sicilNo);
									for (VardiyaGun pdksVardiyaGun : aylikPuantajSablonNew.getVardiyalar()) {
										Date vardiyaDate = pdksVardiyaGun.getVardiyaDate();
										if (!pdksVardiyaGun.isAyinGunu())
											continue;

										String vardiyaKey = list.get(gun++);
										Vardiya pdksVardiya = null;
										if (vardiyaKey != null) {
											if (PdksUtil.setTurkishStr(vardiyaKey).equalsIgnoreCase("ISTIFA"))
												vardiyaKey = "";

											if (PdksUtil.hasStringValue(vardiyaKey)) {
												if (izinler != null && !izinler.isEmpty()) {
													vardiyaList.add(pdksVardiyaGun);
													setIzin(personel.getId(), izinMap.get(personel.getId()), vardiyaList);
													vardiyaList.clear();
												}
												if (pdksVardiyaGun.getIzin() != null) {
													pdksVardiya = pdksVardiyaGun.getVardiya();
													if (aylikPuantajSablonNew.getCalismaModeli() != null) {
														if (vardiyaMap.containsKey(vardiyaKey)) {
															Vardiya pdksVardiyaNew = vardiyaMap.get(vardiyaKey);
															if (!pdksVardiyaNew.getId().equals(pdksVardiya.getId())) {
																pdksVardiya = pdksVardiyaNew;
															}
														}
													}
												} else if (vardiyaMap.containsKey(vardiyaKey)) {
													pdksVardiya = vardiyaMap.get(vardiyaKey);
												} else {
													if (!izinTipiMap.containsKey(vardiyaKey)) {
														hataVar = Boolean.TRUE;
														PdksUtil.addMessageAvailableWarn(sicilNo + " " + perMap.get(sicilNo) + " " + PdksUtil.convertToDateString(vardiyaDate, "d/M/yyyyy") + " günü " + vardiyaKey + " vardiya bilgisi okunamadı!");
													} else {
														if (pdksVardiya == null)
															pdksVardiya = pdksVardiyaGun.getVardiya();
													}
												}
											}
										}
										if (vardiyaIdList != null && pdksVardiya != null && pdksVardiya.getGenel() && pdksVardiya.isCalisma() && !vardiyaIdList.contains(pdksVardiya.getId()))
											pdksVardiya = pdksVardiyaGun.getVardiya();

										pdksVardiyaGun.setYeniVardiya(pdksVardiya);
										if (!calisiyor) {
											if (!personel.isCalisiyorGun(vardiyaDate)) {
												pdksVardiyaGun.setYeniVardiya(null);
												pdksVardiyaGun.setIzin(null);
												pdksVardiyaGun.setGuncellendi(Boolean.TRUE);

											}
										}
										long basVardiyaId = (pdksVardiyaGun.getVardiya() != null ? pdksVardiyaGun.getVardiya().getIdLong() : 0L), bitVardiyaId = (pdksVardiyaGun.getYeniVardiya() != null ? pdksVardiyaGun.getYeniVardiya().getIdLong() : 0L);
										pdksVardiyaGun.setGuncellendi(bitVardiyaId > 0 && basVardiyaId != bitVardiyaId);
										if (!aylikPuantajSablonNew.isKaydet())
											aylikPuantajSablonNew.setKaydet(basVardiyaId != bitVardiyaId);

									}

									aylikPuantajSablonNew.setVardiyaOlustu(!hataVar);
									if (hataVar || aylikPuantajSablonNew.getPersonelDenklestirme() == null) {
										aylikPuantajSablonNew.setKaydet(Boolean.FALSE);
										aylikPuantajSablonNew.setTrClass("hata");
									} else {
										aylikPuantajSablonNew.setTrClass(renk ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
										renk = !renk;
									}
									if (!degisti)
										degisti = aylikPuantajSablonNew.isKaydet();
									VardiyaPlan pdksVardiyaPlan = aylikPuantajSablonNew.getVardiyaPlan();
									for (VardiyaHafta vardiyaHafta : pdksVardiyaPlan.getVardiyaHaftaList())
										vardiyaHafta.setCheckBoxDurum(true);
									aylikPuantajDosyaList.add(aylikPuantajSablonNew);

								}
							}
						} else {
							for (String sicilNo : perMap.keySet()) {
								if (!personelMap.containsKey(sicilNo))
									PdksUtil.addMessageAvailableWarn(sicilNo + " " + perMap.get(sicilNo) + " personel bilgisi okunamadı!");
							}

						}

					}
				} else {
					if (baslangic == 0) {
						baslangic = 2;
					} else {
						devam = false;
						PdksUtil.addMessageAvailableWarn("Gün sayısı uyumsuz!");
					}
				}

			}
		}
		return "";
	}

	/**
	 * @param aylikPuantajSablon
	 * @param fiush
	 * @param pdksVardiyaHaftaSave
	 * @param vardiyaHaftaMap
	 * @param personelGelen
	 * @param sablonu
	 * @param aylikPuantaj
	 * @param vardiyaGunMap
	 * @return
	 */
	private boolean haftalikSablonOlustur(AylikPuantaj aylikPuantajSablon, boolean fiush, boolean pdksVardiyaHaftaSave, TreeMap<String, VardiyaHafta> vardiyaHaftaMap, Personel personelGelen, VardiyaSablonu sablonu, AylikPuantaj aylikPuantaj, TreeMap<String, VardiyaGun> vardiyaGunAllMap) {
		boolean haftaRenk = true;
		Personel personel = (Personel) personelGelen.clone();
		personel.setDurum(Boolean.TRUE);
		boolean calisiyorPersonel = aylikPuantajSablon.getSonGun().after(personel.getIseGirisTarihi()) && personel.getIstenAyrilisTarihi().after(aylikPuantajSablon.getIlkGun());
		if (vardiyaHaftaMap == null)
			vardiyaHaftaMap = new TreeMap<String, VardiyaHafta>();
		VardiyaPlan plan = aylikPuantaj.getVardiyaPlan();
		TreeMap<String, VardiyaGun> vardiyaGunMap = plan.getVardiyaGunMap();
		if (vardiyaGunMap == null) {
			vardiyaGunMap = new TreeMap<String, VardiyaGun>();
			plan.setVardiyaGunMap(vardiyaGunMap);
		} else
			vardiyaGunMap.clear();
		List<VardiyaHafta> vardiyaHaftaList = plan.getVardiyaHaftaList();
		if (vardiyaHaftaList == null) {
			vardiyaHaftaList = new ArrayList<VardiyaHafta>();
			plan.setVardiyaHaftaList(vardiyaHaftaList);
		} else
			vardiyaHaftaList.clear();
		aylikPuantaj.setVardiyaHaftaList(vardiyaHaftaList);
		long iseBasTarih = Long.parseLong(PdksUtil.convertToDateString(personel.getIseGirisTarihi(), "yyyyMMdd"));
		long istenAyrilmaTarih = Long.parseLong(PdksUtil.convertToDateString(personel.getSonCalismaTarihi(), "yyyyMMdd"));
		for (Iterator iterator = aylikPuantajSablon.getVardiyaHaftaList().iterator(); iterator.hasNext();) {
			VardiyaHafta pdksVardiyaHaftaMaster = (VardiyaHafta) iterator.next();
			long planTarih1 = Long.parseLong(PdksUtil.convertToDateString(pdksVardiyaHaftaMaster.getBasTarih(), "yyyyMMdd"));
			long planTarih2 = Long.parseLong(PdksUtil.convertToDateString(pdksVardiyaHaftaMaster.getBitTarih(), "yyyyMMdd"));
			VardiyaHafta pdksVardiyaHafta = (VardiyaHafta) pdksVardiyaHaftaMaster.clone();
			pdksVardiyaHafta.setPersonel(personel);
			pdksVardiyaHafta.setTrClass(haftaRenk ? VardiyaGun.STYLE_CLASS_ODD : VardiyaGun.STYLE_CLASS_EVEN);
			haftaRenk = !haftaRenk;
			pdksVardiyaHafta.setHafta(pdksVardiyaHaftaMaster.getHafta());
			pdksVardiyaHafta.setVardiyaGunler(new ArrayList<VardiyaGun>());
			vardiyaHaftaList.add(pdksVardiyaHafta);
			List<VardiyaGun> haftaVardiyaGunleri = pdksVardiyaHafta.getVardiyaGunler();
			for (VardiyaGun pdksVardiyaGunMaster : pdksVardiyaHaftaMaster.getVardiyaGunler()) {
				VardiyaGun pdksVardiyaGun2 = new VardiyaGun(personel, null, pdksVardiyaGunMaster.getVardiyaDate());
				if (vardiyaGunAllMap.containsKey(pdksVardiyaGun2.getVardiyaKeyStr()))
					pdksVardiyaGun2 = vardiyaGunAllMap.get(pdksVardiyaGun2.getVardiyaKeyStr());
				vardiyaGunMap.put(pdksVardiyaGun2.getVardiyaDateStr(), pdksVardiyaGun2);
				haftaVardiyaGunleri.add(pdksVardiyaGun2);
			}

			boolean calisiyor = planTarih2 >= iseBasTarih && istenAyrilmaTarih >= planTarih1;
			if (!calisiyor) {
				if (pdksVardiyaHafta.getId() != null) {
					pdksEntityController.deleteObject(session, entityManager, pdksVardiyaHafta);
					pdksVardiyaHafta.setId(null);

				}
				if (!calisiyor)
					pdksVardiyaHafta.setVardiyaGunler(haftaVardiyaGunleri);
			} else {
				if (pdksVardiyaHaftaSave && pdksVardiyaHafta.getId() == null && sablonu != null) {
					pdksVardiyaHafta.setVardiyaSablonu(sablonu);
					pdksVardiyaHafta.setOlusturanUser(authenticatedUser);
					saveOrUpdate(pdksVardiyaHafta);

				}

			}
			if (calisiyor) {
				pdksVardiyaHafta.getVardiyaGunler().clear();
				for (VardiyaGun pdksVardiyaGun2 : pdksVardiyaHaftaMaster.getVardiyaGunler()) {
					String key = PdksUtil.convertToDateString(pdksVardiyaGun2.getVardiyaDate(), "yyyyMMdd");
					long ayinGunu = Long.parseLong(key);
					VardiyaGun pdksVardiyaGun = pdksVardiyaGun2;
					if (ayinGunu >= iseBasTarih && istenAyrilmaTarih >= ayinGunu)
						pdksVardiyaGun = vardiyaGunMap.containsKey(key) ? vardiyaGunMap.get(key) : (VardiyaGun) pdksVardiyaGun2.clone();

					pdksVardiyaHafta.getVardiyaGunler().add(pdksVardiyaGun);
				}
			}

			for (int i = 0; i < haftaVardiyaGunleri.size(); i++) {
				VardiyaGun pdksVardiyaGun = haftaVardiyaGunleri.get(i);
				int haftaGunu = PdksUtil.getDateField(pdksVardiyaGun.getVardiyaDate(), Calendar.DAY_OF_WEEK);
				if (pdksVardiyaGun.getId() == null) {
					if (vardiyaGunAllMap.containsKey(pdksVardiyaGun.getVardiyaKeyStr())) {
						pdksVardiyaGun = vardiyaGunAllMap.get(pdksVardiyaGun.getVardiyaKeyStr());
					} else if (sablonu != null) {
						pdksVardiyaGun.setPersonel(personelGelen);
						switch (haftaGunu) {
						case Calendar.MONDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya1());
							break;
						case Calendar.TUESDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya2());
							break;
						case Calendar.WEDNESDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya3());
							break;
						case Calendar.THURSDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya4());
							break;
						case Calendar.FRIDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya5());
							break;
						case Calendar.SATURDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya6());
							break;
						case Calendar.SUNDAY:
							pdksVardiyaGun.setVardiya(sablonu.getVardiya7());
							break;
						default:
							break;
						}
					}
					if (pdksVardiyaGun.getPersonel() != null)
						vardiyaGunMap.put(pdksVardiyaGun.getVardiyaDateStr(), pdksVardiyaGun);
					haftaVardiyaGunleri.set(i, pdksVardiyaGun);
				}
				String key = PdksUtil.convertToDateString(pdksVardiyaGun.getVardiyaDate(), "yyyyMMdd");
				if (!calisiyorPersonel) {
					if (!personel.isCalisiyorGun(pdksVardiyaGun.getVardiyaDate()))
						pdksVardiyaGun.setVardiya(null);
				}
				vardiyaGunMap.put(key, pdksVardiyaGun);
				switch (haftaGunu) {
				case Calendar.MONDAY:
					pdksVardiyaHafta.setVardiyaGun1(pdksVardiyaGun);
					break;
				case Calendar.TUESDAY:
					pdksVardiyaHafta.setVardiyaGun2(pdksVardiyaGun);
					break;
				case Calendar.WEDNESDAY:
					pdksVardiyaHafta.setVardiyaGun3(pdksVardiyaGun);
					break;
				case Calendar.THURSDAY:
					pdksVardiyaHafta.setVardiyaGun4(pdksVardiyaGun);
					break;
				case Calendar.FRIDAY:
					pdksVardiyaHafta.setVardiyaGun5(pdksVardiyaGun);
					break;
				case Calendar.SATURDAY:
					pdksVardiyaHafta.setVardiyaGun6(pdksVardiyaGun);
					break;
				case Calendar.SUNDAY:
					pdksVardiyaHafta.setVardiyaGun7(pdksVardiyaGun);
					break;
				default:
					break;
				}
			}

		}
		aylikPuantaj.setVardiyalar(new ArrayList<VardiyaGun>(vardiyaGunMap.values()));
		return fiush;
	}

	public Date getHaftaTatili() {
		return haftaTatili;
	}

	public void setHaftaTatili(Date haftaTatili) {
		this.haftaTatili = haftaTatili;
	}

	public Boolean getKaydet() {
		return kaydet;
	}

	public void setKaydet(Boolean kaydet) {
		this.kaydet = kaydet;
	}

	public ArrayList<Date> getIslemGunleri() {
		return islemGunleri;
	}

	public void setIslemGunleri(ArrayList<Date> islemGunleri) {
		this.islemGunleri = islemGunleri;
	}

	public List<VardiyaPlan> getVardiyaPlanList() {
		return vardiyaPlanList;
	}

	public void setVardiyaPlanList(List<VardiyaPlan> vardiyaPlanList) {
		this.vardiyaPlanList = vardiyaPlanList;
	}

	public VardiyaPlan getVardiyaPlan() {
		return vardiyaPlan;
	}

	public void setVardiyaPlan(VardiyaPlan vardiyaPlan) {
		this.vardiyaPlan = vardiyaPlan;
	}

	public VardiyaHafta getVardiyaHafta1() {
		return vardiyaHafta1;
	}

	public void setVardiyaHafta1(VardiyaHafta vardiyaHafta1) {
		this.vardiyaHafta1 = vardiyaHafta1;
	}

	public VardiyaHafta getVardiyaHafta2() {
		return vardiyaHafta2;
	}

	public void setVardiyaHafta2(VardiyaHafta vardiyaHafta2) {
		this.vardiyaHafta2 = vardiyaHafta2;
	}

	public Departman getDepartman() {
		return departman;
	}

	public void setDepartman(Departman departman) {
		this.departman = departman;
	}

	public List<Vardiya> getCalismaOlmayanVardiyaList() {
		return calismaOlmayanVardiyaList;
	}

	public void setCalismaOlmayanVardiyaList(List<Vardiya> calismaOlmayanVardiyaList) {
		this.calismaOlmayanVardiyaList = calismaOlmayanVardiyaList;
	}

	public TreeMap<String, Tatil> getTatilMap() {
		return tatilMap;
	}

	public void setTatilMap(TreeMap<String, Tatil> tatilMap) {
		this.tatilMap = tatilMap;
	}

	public String getSicilNo() {
		return sicilNo;
	}

	public void setSicilNo(String sicilNo) {
		this.sicilNo = sicilNo;
	}

	public Boolean getDegisti() {
		return degisti;
	}

	public void setDegisti(Boolean degisti) {
		this.degisti = degisti;
	}

	public void setDenklestirmeHesapla(boolean denklestirmeHesapla) {
		this.denklestirmeHesapla = denklestirmeHesapla;
	}

	public TreeMap<String, Tanim> getEkSahaTanimMap() {
		return ekSahaTanimMap;
	}

	public void setEkSahaTanimMap(TreeMap<String, Tanim> ekSahaTanimMap) {
		this.ekSahaTanimMap = ekSahaTanimMap;
	}

	public void setVardiyaGuncelle(boolean vardiyaGuncelle) {
		this.vardiyaGuncelle = vardiyaGuncelle;
	}

	public int getAy() {
		return ay;
	}

	public void setAy(int ay) {
		this.ay = ay;
	}

	public int getYil() {
		return yil;
	}

	public void setYil(int yil) {
		this.yil = yil;
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

	public AylikPuantaj getAylikPuantajDefault() {
		return aylikPuantajDefault;
	}

	public void setAylikPuantajDefault(AylikPuantaj aylikPuantajDefault) {
		this.aylikPuantajDefault = aylikPuantajDefault;
	}

	public List<AylikPuantaj> getAylikPuantajList() {
		return aylikPuantajList;
	}

	public void setAylikPuantajList(List<AylikPuantaj> aylikPuantajList) {
		this.aylikPuantajList = aylikPuantajList;
	}

	public AylikPuantaj getPersonelAylikPuantaj() {
		return personelAylikPuantaj;
	}

	public void setPersonelAylikPuantaj(AylikPuantaj personelAylikPuantaj) {
		this.personelAylikPuantaj = personelAylikPuantaj;
	}

	public DenklestirmeAy getDenklestirmeAy() {
		return denklestirmeAy;
	}

	public void setDenklestirmeAy(DenklestirmeAy denklestirmeAy) {
		this.denklestirmeAy = denklestirmeAy;
	}

	public PersonelDenklestirme getPersonelDenklestirme() {
		return personelDenklestirme;
	}

	public void setPersonelDenklestirme(PersonelDenklestirme personelDenklestirme) {
		this.personelDenklestirme = personelDenklestirme;
	}

	public double getHaftaTatilSaat() {
		return haftaTatilSaat;
	}

	public void setHaftaTatilSaat(double haftaTatilSaat) {
		this.haftaTatilSaat = haftaTatilSaat;
	}

	public VardiyaGun getSeciliVardiyaGun() {
		return seciliVardiyaGun;
	}

	public void setSeciliVardiyaGun(VardiyaGun seciliVardiyaGun) {
		this.seciliVardiyaGun = seciliVardiyaGun;
	}

	public void setGunSec(boolean gunSec) {
		this.gunSec = gunSec;
	}

	public HashMap<String, Personel> getGorevliPersonelMap() {
		return gorevliPersonelMap;
	}

	public void setGorevliPersonelMap(HashMap<String, Personel> gorevliPersonelMap) {
		this.gorevliPersonelMap = gorevliPersonelMap;
	}

	public void setGorevli(boolean gorevli) {
		this.gorevli = gorevli;
	}

	public Tanim getGorevYeri() {
		return gorevYeri;
	}

	public void setGorevYeri(Tanim gorevYeri) {
		this.gorevYeri = gorevYeri;
	}

	public void setHastaneSuperVisor(boolean hastaneSuperVisor) {
		this.hastaneSuperVisor = hastaneSuperVisor;
	}

	public void setOzelIstek(boolean ozelIstek) {
		this.ozelIstek = ozelIstek;
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

	public File getEkliDosya() {
		return ekliDosya;
	}

	public void setEkliDosya(File ekliDosya) {
		this.ekliDosya = ekliDosya;
	}

	public Date getBasTarih() {
		return basTarih;
	}

	public void setBasTarih(Date basTarih) {
		this.basTarih = basTarih;
	}

	public Date getBitTarih() {
		return bitTarih;
	}

	public void setBitTarih(Date bitTarih) {
		this.bitTarih = bitTarih;
	}

	public List<VardiyaGun> getVardiyaGunList() {
		return vardiyaGunList;
	}

	public void setVardiyaGunList(List<VardiyaGun> vardiyaGunList) {
		this.vardiyaGunList = vardiyaGunList;
	}

	public String getDosyaAdi() {
		return dosyaAdi;
	}

	public void setDosyaAdi(String dosyaAdi) {
		this.dosyaAdi = dosyaAdi;
	}

	public ByteArrayOutputStream getBaosDosya() {
		return baosDosya;
	}

	public void setBaosDosya(ByteArrayOutputStream baosDosya) {
		this.baosDosya = baosDosya;
	}

	public void setOnayDurum(boolean onayDurum) {
		this.onayDurum = onayDurum;
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

	public byte[] getMailData() {
		return mailData;
	}

	public void setMailData(byte[] mailData) {
		this.mailData = mailData;
	}

	public Tanim getPlanDepartman() {
		return planDepartman;
	}

	public void setPlanDepartman(Tanim planDepartman) {
		this.planDepartman = planDepartman;
	}

	public List<SelectItem> getOzelDurumList() {
		return ozelDurumList;
	}

	public void setOzelDurumList(List<SelectItem> ozelDurumList) {
		this.ozelDurumList = ozelDurumList;
	}

	public List<BolumKat> getBolumKatlari() {
		return bolumKatlari;
	}

	public void setBolumKatlari(List<BolumKat> bolumKatlari) {
		this.bolumKatlari = bolumKatlari;
	}

	public List<Tanim> getGorevYeriTanimList() {
		return gorevYeriTanimList;
	}

	public void setGorevYeriTanimList(List<Tanim> gorevYeriTanimList) {
		this.gorevYeriTanimList = gorevYeriTanimList;
	}

	public List<Long> getGorevYerileri() {
		return gorevYerileri;
	}

	public void setGorevYerileri(List<Long> gorevYerileri) {
		this.gorevYerileri = gorevYerileri;
	}

	public void setResmiTatilVar(boolean resmiTatilVar) {
		this.resmiTatilVar = resmiTatilVar;
	}

	public List<VardiyaGun> getAylikVardiyaOzetList() {
		return aylikVardiyaOzetList;
	}

	public void setAylikVardiyaOzetList(List<VardiyaGun> aylikVardiyaOzetList) {
		this.aylikVardiyaOzetList = aylikVardiyaOzetList;
	}

	public void setSutIzniGoster(boolean sutIzniGoster) {
		this.sutIzniGoster = sutIzniGoster;
	}

	public TreeMap<String, VardiyaGun> getVardiyalarMap() {
		return vardiyalarMap;
	}

	public void setVardiyalarMap(TreeMap<String, VardiyaGun> vardiyalarMap) {
		this.vardiyalarMap = vardiyalarMap;
	}

	public void setIslemYapiliyor(boolean islemYapiliyor) {
		this.islemYapiliyor = islemYapiliyor;
	}

	public void setPlanGirisi(boolean planGirisi) {
		this.planGirisi = planGirisi;
	}

	public Long getGorevTipiId() {
		return gorevTipiId;
	}

	public void setGorevTipiId(Long gorevTipiId) {
		this.gorevTipiId = gorevTipiId;
	}

	public void setSablonGuncelle(boolean sablonGuncelle) {
		this.sablonGuncelle = sablonGuncelle;
	}

	public TreeMap<String, Vardiya> getKayitliVardiyalarMap() {
		return kayitliVardiyalarMap;
	}

	public void setKayitliVardiyalarMap(TreeMap<String, Vardiya> kayitliVardiyalarMap) {
		this.kayitliVardiyalarMap = kayitliVardiyalarMap;
	}

	public void setVeriGuncellendi(boolean veriGuncellendi) {
		this.veriGuncellendi = veriGuncellendi;
	}

	public String getDonusAdres() {
		return donusAdres;
	}

	public void setDonusAdres(String donusAdres) {
		this.donusAdres = donusAdres;
	}

	public void setPartTimeGoster(boolean partTimeGoster) {
		this.partTimeGoster = partTimeGoster;
	}

	public void setFileImport(boolean fileImport) {
		this.fileImport = fileImport;
	}

	public Dosya getVardiyaPlanDosya() {
		return vardiyaPlanDosya;
	}

	public void setVardiyaPlanDosya(Dosya vardiyaPlanDosya) {
		this.vardiyaPlanDosya = vardiyaPlanDosya;
	}

	public List<AylikPuantaj> getAylikPuantajDosyaList() {
		return aylikPuantajDosyaList;
	}

	public void setAylikPuantajDosyaList(List<AylikPuantaj> aylikPuantajDosyaList) {
		this.aylikPuantajDosyaList = aylikPuantajDosyaList;
	}

	public void setStajerSirket(boolean stajerSirket) {
		this.stajerSirket = stajerSirket;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public AramaSecenekleri getAramaSecenekleri() {
		return aramaSecenekleri;
	}

	public void setAramaSecenekleri(AramaSecenekleri aramaSecenekleri) {
		this.aramaSecenekleri = aramaSecenekleri;
	}

	public void setAksamGunVar(boolean aksamGunVar) {
		this.aksamGunVar = aksamGunVar;
	}

	public void setAksamSaatVar(boolean aksamSaatVar) {
		this.aksamSaatVar = aksamSaatVar;
	}

	public void setHaftaTatilVar(boolean haftaTatilVar) {
		this.haftaTatilVar = haftaTatilVar;
	}

	public Boolean getDenklestirmeHesapla() {
		return denklestirmeHesapla;
	}

	public void setDenklestirmeHesapla(Boolean denklestirmeHesapla) {
		this.denklestirmeHesapla = denklestirmeHesapla;
	}

	public Boolean getGunSec() {
		return gunSec;
	}

	public void setGunSec(Boolean gunSec) {
		this.gunSec = gunSec;
	}

	public Boolean getGorevli() {
		return gorevli;
	}

	public void setGorevli(Boolean gorevli) {
		this.gorevli = gorevli;
	}

	public Boolean getOzelIstek() {
		return ozelIstek;
	}

	public void setOzelIstek(Boolean ozelIstek) {
		this.ozelIstek = ozelIstek;
	}

	public Boolean getIslemYapiliyor() {
		return islemYapiliyor;
	}

	public void setIslemYapiliyor(Boolean islemYapiliyor) {
		this.islemYapiliyor = islemYapiliyor;
	}

	public Boolean getResmiTatilVar() {
		return resmiTatilVar;
	}

	public void setResmiTatilVar(Boolean resmiTatilVar) {
		this.resmiTatilVar = resmiTatilVar;
	}

	public Boolean getAksamGunVar() {
		return aksamGunVar;
	}

	public void setAksamGunVar(Boolean aksamGunVar) {
		this.aksamGunVar = aksamGunVar;
	}

	public Boolean getAksamSaatVar() {
		return aksamSaatVar;
	}

	public void setAksamSaatVar(Boolean aksamSaatVar) {
		this.aksamSaatVar = aksamSaatVar;
	}

	public Boolean getHaftaTatilVar() {
		return haftaTatilVar;
	}

	public void setHaftaTatilVar(Boolean haftaTatilVar) {
		this.haftaTatilVar = haftaTatilVar;
	}

	public boolean isFileImport() {
		return fileImport;
	}

	public boolean isVardiyaVar() {
		return vardiyaVar;
	}

	public boolean isVardiyaGuncelle() {
		return vardiyaGuncelle;
	}

	public boolean isHastaneSuperVisor() {
		return hastaneSuperVisor;
	}

	public boolean isOnayDurum() {
		return onayDurum;
	}

	public boolean isPartTimeGoster() {
		return partTimeGoster;
	}

	public boolean isSutIzniGoster() {
		return sutIzniGoster;
	}

	public boolean isPlanGirisi() {
		return planGirisi;
	}

	public boolean isSablonGuncelle() {
		return sablonGuncelle;
	}

	public boolean isVeriGuncellendi() {
		return veriGuncellendi;
	}

	public boolean isStajerSirket() {
		return stajerSirket;
	}

	public List<CalismaModeliAy> getModelList() {
		return modelList;
	}

	public void setModelList(List<CalismaModeliAy> modelList) {
		this.modelList = modelList;
	}

	public FazlaMesaiTalep getFazlaMesaiTalep() {
		return fazlaMesaiTalep;
	}

	public void setFazlaMesaiTalep(FazlaMesaiTalep fazlaMesaiTalep) {
		this.fazlaMesaiTalep = fazlaMesaiTalep;
	}

	public List<SelectItem> getMesaiNedenTanimList() {
		return mesaiNedenTanimList;
	}

	public void setMesaiNedenTanimList(List<SelectItem> mesaiNedenTanimList) {
		this.mesaiNedenTanimList = mesaiNedenTanimList;
	}

	public List<Tanim> getMesaiIptalNedenTanimList() {
		return mesaiIptalNedenTanimList;
	}

	public void setMesaiIptalNedenTanimList(List<Tanim> mesaiIptalNedenTanimList) {
		this.mesaiIptalNedenTanimList = mesaiIptalNedenTanimList;
	}

	public FazlaMesaiTalep getIslemFazlaMesaiTalep() {
		return islemFazlaMesaiTalep;
	}

	public void setIslemFazlaMesaiTalep(FazlaMesaiTalep islemFazlaMesaiTalep) {
		this.islemFazlaMesaiTalep = islemFazlaMesaiTalep;
	}

	public boolean isFazlaMesaiTalepVar() {
		return fazlaMesaiTalepVar;
	}

	public void setFazlaMesaiTalepVar(boolean fazlaMesaiTalepVar) {
		this.fazlaMesaiTalepVar = fazlaMesaiTalepVar;
	}

	public List<FazlaMesaiTalep> getAylikFazlaMesaiTalepler() {
		return aylikFazlaMesaiTalepler;
	}

	public void setAylikFazlaMesaiTalepler(List<FazlaMesaiTalep> aylikFazlaMesaiTalepler) {
		this.aylikFazlaMesaiTalepler = aylikFazlaMesaiTalepler;
	}

	public List<SelectItem> getGunler() {
		return gunler;
	}

	public void setGunler(List<SelectItem> gunler) {
		this.gunler = gunler;
	}

	public String getBasTarihStr() {
		return basTarihStr;
	}

	public void setBasTarihStr(String basTarihStr) {
		this.basTarihStr = basTarihStr;
	}

	public String getBitTarihStr() {
		return bitTarihStr;
	}

	public void setBitTarihStr(String bitTarihStr) {
		this.bitTarihStr = bitTarihStr;
	}

	public Boolean getDepartmanBolumAyni() {
		return departmanBolumAyni;
	}

	public void setDepartmanBolumAyni(Boolean departmanBolumAyni) {
		this.departmanBolumAyni = departmanBolumAyni;
	}

	public boolean isHaftaTatilMesaiDurum() {
		return haftaTatilMesaiDurum;
	}

	public void setHaftaTatilMesaiDurum(boolean haftaTatilMesaiDurum) {
		this.haftaTatilMesaiDurum = haftaTatilMesaiDurum;
	}

	public boolean isModelGoster() {
		return modelGoster;
	}

	public void setModelGoster(boolean modelGoster) {
		this.modelGoster = modelGoster;
	}

	public Integer getAksamVardiyaBasSaat() {
		return aksamVardiyaBasSaat;
	}

	public void setAksamVardiyaBasSaat(Integer aksamVardiyaBasSaat) {
		this.aksamVardiyaBasSaat = aksamVardiyaBasSaat;
	}

	public Integer getAksamVardiyaBitSaat() {
		return aksamVardiyaBitSaat;
	}

	public void setAksamVardiyaBitSaat(Integer aksamVardiyaBitSaat) {
		this.aksamVardiyaBitSaat = aksamVardiyaBitSaat;
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

	public TreeMap<String, Tanim> getFazlaMesaiMap() {
		return fazlaMesaiMap;
	}

	public void setFazlaMesaiMap(TreeMap<String, Tanim> fazlaMesaiMap) {
		this.fazlaMesaiMap = fazlaMesaiMap;
	}

	public boolean isMailGonder() {
		return mailGonder;
	}

	public void setMailGonder(boolean mailGonder) {
		this.mailGonder = mailGonder;
	}

	public List<FazlaMesaiTalep> getFazlaMesaiTalepler() {
		return fazlaMesaiTalepler;
	}

	public void setFazlaMesaiTalepler(List<FazlaMesaiTalep> fazlaMesaiTalepler) {
		this.fazlaMesaiTalepler = fazlaMesaiTalepler;
	}

	public boolean isSeciliDurum() {
		return seciliDurum;
	}

	public void setSeciliDurum(boolean seciliDurum) {
		this.seciliDurum = seciliDurum;
	}

	public List<SelectItem> getFazlaMesaiTalepDurumList() {
		return fazlaMesaiTalepDurumList;
	}

	public void setFazlaMesaiTalepDurumList(List<SelectItem> fazlaMesaiTalepDurumList) {
		this.fazlaMesaiTalepDurumList = fazlaMesaiTalepDurumList;
	}

	public int getTalepOnayDurum() {
		return talepOnayDurum;
	}

	public void setTalepOnayDurum(int talepOnayDurum) {
		this.talepOnayDurum = talepOnayDurum;
	}

	public boolean isMesaiOnayla() {
		return mesaiOnayla;
	}

	public void setMesaiOnayla(boolean mesaiOnayla) {
		this.mesaiOnayla = mesaiOnayla;
	}

	public String getSanalPersonelAciklama() {
		return sanalPersonelAciklama;
	}

	public void setSanalPersonelAciklama(String sanalPersonelAciklama) {
		this.sanalPersonelAciklama = sanalPersonelAciklama;
	}

	public Boolean getKullaniciPersonel() {
		return kullaniciPersonel;
	}

	public void setKullaniciPersonel(Boolean kullaniciPersonel) {
		this.kullaniciPersonel = kullaniciPersonel;
	}

	public Boolean getTopluFazlaCalismaTalep() {
		return topluFazlaCalismaTalep;
	}

	public void setTopluFazlaCalismaTalep(Boolean topluFazlaCalismaTalep) {
		this.topluFazlaCalismaTalep = topluFazlaCalismaTalep;
	}

	public List<AylikPuantaj> getAylikPuantajMesaiTalepList() {
		return aylikPuantajMesaiTalepList;
	}

	public void setAylikPuantajMesaiTalepList(List<AylikPuantaj> aylikPuantajMesaiTalepList) {
		this.aylikPuantajMesaiTalepList = aylikPuantajMesaiTalepList;
	}

	public TreeMap<Long, List<FazlaMesaiTalep>> getVardiyaFazlaMesaiMap() {
		return vardiyaFazlaMesaiMap;
	}

	public void setVardiyaFazlaMesaiMap(TreeMap<Long, List<FazlaMesaiTalep>> vardiyaFazlaMesaiMap) {
		this.vardiyaFazlaMesaiMap = vardiyaFazlaMesaiMap;
	}

	public List<Vardiya> getIzinTipiVardiyaList() {
		return izinTipiVardiyaList;
	}

	public void setIzinTipiVardiyaList(List<Vardiya> izinTipiVardiyaList) {
		this.izinTipiVardiyaList = izinTipiVardiyaList;
	}

	public TreeMap<String, TreeMap<String, List<VardiyaGun>>> getIzinTipiPersonelVardiyaMap() {
		return izinTipiPersonelVardiyaMap;
	}

	public void setIzinTipiPersonelVardiyaMap(TreeMap<String, TreeMap<String, List<VardiyaGun>>> izinTipiPersonelVardiyaMap) {
		this.izinTipiPersonelVardiyaMap = izinTipiPersonelVardiyaMap;
	}

	public List<PersonelDenklestirmeDinamikAlan> getPersonelDenklestirmeDinamikAlanList() {
		return personelDenklestirmeDinamikAlanList;
	}

	public void setPersonelDenklestirmeDinamikAlanList(List<PersonelDenklestirmeDinamikAlan> personelDenklestirmeDinamikAlanList) {
		this.personelDenklestirmeDinamikAlanList = personelDenklestirmeDinamikAlanList;
	}

	public Boolean getDenklestirmeAyDurum() {
		return denklestirmeAyDurum;
	}

	public void setDenklestirmeAyDurum(Boolean denklestirmeAyDurum) {
		this.denklestirmeAyDurum = denklestirmeAyDurum;
	}

	public DenklestirmeAy getDenklestirmeOncekiAy() {
		return denklestirmeOncekiAy;
	}

	public void setDenklestirmeOncekiAy(DenklestirmeAy denklestirmeOncekiAy) {
		this.denklestirmeOncekiAy = denklestirmeOncekiAy;
	}

	public DenklestirmeAy getDenklestirmeSonrakiAy() {
		return denklestirmeSonrakiAy;
	}

	public void setDenklestirmeSonrakiAy(DenklestirmeAy denklestirmeSonrakiAy) {
		this.denklestirmeSonrakiAy = denklestirmeSonrakiAy;
	}

	public boolean isManuelVardiyaIzinGir() {
		return manuelVardiyaIzinGir;
	}

	public void setManuelVardiyaIzinGir(boolean manuelVardiyaIzinGir) {
		this.manuelVardiyaIzinGir = manuelVardiyaIzinGir;
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

	public boolean isGorevYeriGirisDurum() {
		return gorevYeriGirisDurum;
	}

	public void setGorevYeriGirisDurum(boolean gorevYeriGirisDurum) {
		this.gorevYeriGirisDurum = gorevYeriGirisDurum;
	}

	public Boolean getFazlaMesaiTalepDurum() {
		return fazlaMesaiTalepDurum;
	}

	public void setFazlaMesaiTalepDurum(Boolean fazlaMesaiTalepDurum) {
		this.fazlaMesaiTalepDurum = fazlaMesaiTalepDurum;
	}

	public boolean isFazlaMesaiTarihGuncelle() {
		return fazlaMesaiTarihGuncelle;
	}

	public void setFazlaMesaiTarihGuncelle(boolean fazlaMesaiTarihGuncelle) {
		this.fazlaMesaiTarihGuncelle = fazlaMesaiTarihGuncelle;
	}

	public HashMap<Double, ArrayList<Vardiya>> getVardiyaMap() {
		return vardiyaMap;
	}

	public void setVardiyaMap(HashMap<Double, ArrayList<Vardiya>> vardiyaMap) {
		this.vardiyaMap = vardiyaMap;
	}

	public void setVardiyaVar(boolean vardiyaVar) {
		this.vardiyaVar = vardiyaVar;
	}

	public List<Vardiya> getVardiyaList() {
		return vardiyaList;
	}

	public void setVardiyaList(List<Vardiya> vardiyaList) {
		this.vardiyaList = vardiyaList;
	}

	public List<VardiyaSablonu> getSablonList() {
		return sablonList;
	}

	public void setSablonList(List<VardiyaSablonu> sablonList) {
		this.sablonList = sablonList;
	}

	public boolean isGebeSutIzniGuncelle() {
		return gebeSutIzniGuncelle;
	}

	public void setGebeSutIzniGuncelle(boolean gebeSutIzniGuncelle) {
		this.gebeSutIzniGuncelle = gebeSutIzniGuncelle;
	}

	public boolean isGebeGoster() {
		return gebeGoster;
	}

	public void setGebeGoster(boolean gebeGoster) {
		this.gebeGoster = gebeGoster;
	}

	public boolean isOffIzinGuncelle() {
		return offIzinGuncelle;
	}

	public void setOffIzinGuncelle(boolean offIzinGuncelle) {
		this.offIzinGuncelle = offIzinGuncelle;
	}

	public String getBolumAciklama() {
		if (bolumAciklama == null)
			fillEkSahaTanim();
		return bolumAciklama;
	}

	public void setBolumAciklama(String bolumAciklama) {
		this.bolumAciklama = bolumAciklama;
	}

	public HashMap<String, List<Tanim>> getEkSahaListMap() {
		return ekSahaListMap;
	}

	public void setEkSahaListMap(HashMap<String, List<Tanim>> ekSahaListMap) {
		this.ekSahaListMap = ekSahaListMap;
	}

	public Boolean getAylikHareketKaydiVardiyaBul() {
		return aylikHareketKaydiVardiyaBul;
	}

	public void setAylikHareketKaydiVardiyaBul(Boolean aylikHareketKaydiVardiyaBul) {
		this.aylikHareketKaydiVardiyaBul = aylikHareketKaydiVardiyaBul;
	}

	public List<SelectItem> getRedNedenleri() {
		return redNedenleri;
	}

	public void setRedNedenleri(List<SelectItem> redNedenleri) {
		this.redNedenleri = redNedenleri;
	}

	public Long getRedNedeniId() {
		return redNedeniId;
	}

	public void setRedNedeniId(Long redNedeniId) {
		this.redNedeniId = redNedeniId;
	}

	public List<HareketKGS> getHareketPdksList() {
		return hareketPdksList;
	}

	public void setHareketPdksList(List<HareketKGS> hareketPdksList) {
		this.hareketPdksList = hareketPdksList;
	}

	public Boolean getManuelHareketEkle() {
		return manuelHareketEkle;
	}

	public void setManuelHareketEkle(Boolean manuelHareketEkle) {
		this.manuelHareketEkle = manuelHareketEkle;
	}

	public Long getMesaiNedenId() {
		return mesaiNedenId;
	}

	public void setMesaiNedenId(Long mesaiNedenId) {
		this.mesaiNedenId = mesaiNedenId;
	}

	public Long getSeciliEkSaha4Id() {
		return seciliEkSaha4Id;
	}

	public void setSeciliEkSaha4Id(Long seciliEkSaha4Id) {
		this.seciliEkSaha4Id = seciliEkSaha4Id;
	}

	public Tanim getEkSaha4Tanim() {
		return ekSaha4Tanim;
	}

	public void setEkSaha4Tanim(Tanim ekSaha4Tanim) {
		this.ekSaha4Tanim = ekSaha4Tanim;
	}

	public String getAltBolumAciklama() {
		return altBolumAciklama;
	}

	public void setAltBolumAciklama(String altBolumAciklama) {
		this.altBolumAciklama = altBolumAciklama;
	}

	public String getTesisAciklama() {
		return tesisAciklama;
	}

	public void setTesisAciklama(String tesisAciklama) {
		this.tesisAciklama = tesisAciklama;
	}

	public Boolean getVardiyaFazlaMesaiTalepGoster() {
		return vardiyaFazlaMesaiTalepGoster;
	}

	public void setVardiyaFazlaMesaiTalepGoster(Boolean vardiyaFazlaMesaiTalepGoster) {
		this.vardiyaFazlaMesaiTalepGoster = vardiyaFazlaMesaiTalepGoster;
	}

	public Boolean getEksikMaasGoster() {
		return eksikMaasGoster;
	}

	public void setEksikMaasGoster(Boolean eksikMaasGoster) {
		this.eksikMaasGoster = eksikMaasGoster;
	}

	public Boolean getBordroPuantajEkranindaGoster() {
		return bordroPuantajEkranindaGoster;
	}

	public void setBordroPuantajEkranindaGoster(Boolean bordroPuantajEkranindaGoster) {
		this.bordroPuantajEkranindaGoster = bordroPuantajEkranindaGoster;
	}

	public TreeMap<String, Boolean> getBaslikMap() {
		return baslikMap;
	}

	public void setBaslikMap(TreeMap<String, Boolean> baslikMap) {
		this.baslikMap = baslikMap;
	}

	public boolean isFazlaMesaiIzinRaporuDurum() {
		return fazlaMesaiIzinRaporuDurum;
	}

	public void setFazlaMesaiIzinRaporuDurum(boolean fazlaMesaiIzinRaporuDurum) {
		this.fazlaMesaiIzinRaporuDurum = fazlaMesaiIzinRaporuDurum;
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

	public Boolean getBordroToplamGunKod() {
		return bordroToplamGunKod;
	}

	public void setBordroToplamGunKod(Boolean bordroToplamGunKod) {
		this.bordroToplamGunKod = bordroToplamGunKod;
	}

	public Boolean getArtikGunKod() {
		return artikGunKod;
	}

	public void setArtikGunKod(Boolean artikGunKod) {
		this.artikGunKod = artikGunKod;
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

	public void setAylikPuantajDonem(AylikPuantaj aylikPuantajDonem) {
		this.aylikPuantajDonem = aylikPuantajDonem;
	}

	public User getLoginUser() {
		return loginUser;
	}

	public void setLoginUser(User loginUser) {
		this.loginUser = loginUser;
	}

	public String getLinkBordroAdres() {
		return linkBordroAdres;
	}

	public void setLinkBordroAdres(String linkBordroAdres) {
		this.linkBordroAdres = linkBordroAdres;
	}

	public Tanim getSeciliBolum() {
		return seciliBolum;
	}

	public void setSeciliBolum(Tanim seciliBolum) {
		this.seciliBolum = seciliBolum;
	}

	public Tanim getSeciliAltBolum() {
		return seciliAltBolum;
	}

	public void setSeciliAltBolum(Tanim seciliAltBolum) {
		this.seciliAltBolum = seciliAltBolum;
	}

	public PersonelDonemselDurum getPersonelGebeDurum() {
		return personelGebeDurum;
	}

	public void setPersonelGebeDurum(PersonelDonemselDurum personelGebeDurum) {
		this.personelGebeDurum = personelGebeDurum;
	}

	public PersonelDonemselDurum getPersonelSutIzniDurum() {
		return personelSutIzniDurum;
	}

	public void setPersonelSutIzniDurum(PersonelDonemselDurum personelSutIzniDurum) {
		this.personelSutIzniDurum = personelSutIzniDurum;
	}

	public List<Personel> getTumBolumPersonelleri() {
		return tumBolumPersonelleri;
	}

	public void setTumBolumPersonelleri(List<Personel> tumBolumPersonelleri) {
		this.tumBolumPersonelleri = tumBolumPersonelleri;
	}

	public CalismaPlanKilit getCalismaPlanKilit() {
		return calismaPlanKilit;
	}

	public void setCalismaPlanKilit(CalismaPlanKilit calismaPlanKilit) {
		this.calismaPlanKilit = calismaPlanKilit;
	}

	public List<Tanim> getDinamikAlanlar() {
		return dinamikAlanlar;
	}

	public void setDinamikAlanlar(List<Tanim> dinamikAlanlar) {
		this.dinamikAlanlar = dinamikAlanlar;
	}

	public List<CalismaPlanKilit> getKilitliPlanList() {
		return kilitliPlanList;
	}

	public void setKilitliPlanList(List<CalismaPlanKilit> kilitliPlanList) {
		this.kilitliPlanList = kilitliPlanList;
	}

	public DepartmanDenklestirmeDonemi getDepartmanDenklestirmeDonemi() {
		return departmanDenklestirmeDonemi;
	}

	public void setDepartmanDenklestirmeDonemi(DepartmanDenklestirmeDonemi departmanDenklestirmeDonemi) {
		this.departmanDenklestirmeDonemi = departmanDenklestirmeDonemi;
	}

	public List<Vardiya> getCalismaModeliVardiyaList() {
		return calismaModeliVardiyaList;
	}

	public void setCalismaModeliVardiyaList(List<Vardiya> calismaModeliVardiyaList) {
		this.calismaModeliVardiyaList = calismaModeliVardiyaList;
	}

	public boolean isFazlaMesaiOde() {
		return fazlaMesaiOde;
	}

	public void setFazlaMesaiOde(boolean fazlaMesaiOde) {
		this.fazlaMesaiOde = fazlaMesaiOde;
	}

	public boolean isFazlaMesaiIzinKullan() {
		return fazlaMesaiIzinKullan;
	}

	public void setFazlaMesaiIzinKullan(boolean fazlaMesaiIzinKullan) {
		this.fazlaMesaiIzinKullan = fazlaMesaiIzinKullan;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		VardiyaGunHome.sayfaURL = sayfaURL;
	}

	public boolean isKartBasmayanPersonel() {
		return kartBasmayanPersonel;
	}

	public void setKartBasmayanPersonel(boolean kartBasmayanPersonel) {
		this.kartBasmayanPersonel = kartBasmayanPersonel;
	}

	public Boolean getIsAramaGoster() {
		return isAramaGoster;
	}

	public void setIsAramaGoster(Boolean isAramaGoster) {
		this.isAramaGoster = isAramaGoster;
	}

	public List<SelectItem> getFazlaMesaiDurumList() {
		return fazlaMesaiDurumList;
	}

	public void setFazlaMesaiDurumList(List<SelectItem> fazlaMesaiDurumList) {
		this.fazlaMesaiDurumList = fazlaMesaiDurumList;
	}

	public Integer getFazlaMesaiDurum() {
		return fazlaMesaiDurum;
	}

	public void setFazlaMesaiDurum(Integer fazlaMesaiDurum) {
		this.fazlaMesaiDurum = fazlaMesaiDurum;
	}

	public Boolean getBakiyeSifirlaDurum() {
		return bakiyeSifirlaDurum;
	}

	public void setBakiyeSifirlaDurum(Boolean bakiyeSifirlaDurum) {
		this.bakiyeSifirlaDurum = bakiyeSifirlaDurum;
	}

}
