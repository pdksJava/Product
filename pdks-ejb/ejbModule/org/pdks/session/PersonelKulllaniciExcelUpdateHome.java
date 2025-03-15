package org.pdks.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import javax.faces.model.SelectItem;
import javax.mail.internet.InternetAddress;
import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hibernate.Session;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.Dosya;
import org.pdks.entity.FileUpload;
import org.pdks.entity.MailGrubu;
import org.pdks.entity.PdksPersonelView;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelView;
import org.pdks.security.entity.User;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

@Name("personelKulllaniciExcelUpdateHome")
public class PersonelKulllaniciExcelUpdateHome extends EntityHome<PersonelView> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3927468770176440280L;
	static Logger logger = Logger.getLogger(PersonelKulllaniciExcelUpdateHome.class);
	/**
	 * 
	 */

	@RequestParameter
	Long perId;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = false, create = true)
	User authenticatedUser;
	@In(create = true)
	Renderer renderer;
	@In(required = false, create = true)
	HashMap parameterMap;
	@In(required = false, create = true)
	OrtakIslemler ortakIslemler;

	public static String sayfaURL = "personelKullaniciExcelGuncelle";

	private List<PersonelView> personelList = new ArrayList<PersonelView>();
	private List<SelectItem> islemList;
	private String islemTipi, mailAdres, ekleSil;
	private FileUpload file;
	private Session session;

	@Override
	public Object getId() {
		if (perId == null) {
			return super.getId();
		} else {
			return perId;
		}
	}

	@Override
	@Begin(join = true)
	public void create() {
		super.create();
	}

	public String tipDegisti() {
		personelList.clear();
		return "";
	}

	public String setAdres() {
		List<String> dogruAdresler = new ArrayList<String>();
		List<String> adresler = ccAdresList(mailAdres);
		List<PersonelView> personelList = new ArrayList<PersonelView>();
		for (String adres : adresler) {
			String eMail = PdksUtil.setTurkishStr(adres.trim()).toLowerCase(Locale.ENGLISH);
			try {
				if (eMail.indexOf("@") < 1)
					throw new Exception(eMail);
				InternetAddress internetAddress = new InternetAddress(eMail);
				eMail = internetAddress.getAddress();
				dogruAdresler.add(eMail.trim());
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				break;
			}
		}
		if (dogruAdresler.size() == adresler.size()) {
			Dosya dosya = ortakIslemler.getDosyaFromFileUpload(file);
			try {

				Workbook wb = ortakIslemler.getWorkbook(dosya);

				LinkedHashMap<String, String> sicilMap = new LinkedHashMap<String, String>();
				if (wb != null) {
					try {
						Sheet sheet = wb.getSheetAt(0);
						for (int j = 1; j <= sheet.getLastRowNum(); j++) {
							String sicilNo = "";
							try {
								sicilNo = getSheetStringValue(sheet, j, 0);
								if (sicilNo != null)
									sicilMap.put(sicilNo, sicilNo);
							} catch (Exception e) {
								logger.error("PDKS hata in : \n");
								e.printStackTrace();
								logger.error("PDKS hata out : " + e.getMessage());
							}
						}
					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());
					}
				}
				if (!sicilMap.isEmpty()) {
					List idList = new ArrayList(sicilMap.keySet());
					String fieldName = "pdksSicilNo";
					HashMap parametreMap = new HashMap();
					parametreMap.put(fieldName, idList);
					if (session != null)
						parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
					// List<PersonelView> list = ortakIslemler.getPersonelViewList(pdksEntityController.getObjectByInnerObjectListInLogic(parametreMap, PdksPersonelView.class));
					List<PersonelView> list = ortakIslemler.getParamList(false, idList, fieldName, parametreMap, PdksPersonelView.class, session);
					TreeMap<String, PersonelView> map1 = new TreeMap<String, PersonelView>();
					for (PersonelView personelView : list) {
						if (personelView.getPdksPersonel() != null) {
							personelView.setCcAdres(null);
							personelView.setBccAdres(null);
							personelView.setHareketAdres(null);
							map1.put(personelView.getSicilNo(), personelView);
						}
					}
					for (String key : sicilMap.keySet()) {
						if (map1.containsKey(key))
							personelList.add(map1.get(key));

					}
					map1 = null;
					sicilMap = null;
					list = null;
					parametreMap = null;
					boolean bcc = islemTipi.indexOf(MailGrubu.TIPI_BCC) >= 0;
					boolean cc = !bcc && islemTipi.indexOf(MailGrubu.TIPI_CC) >= 0;
					boolean hareket = islemTipi.indexOf(MailGrubu.TIPI_HAREKET) >= 0;
					boolean ekle = ekleSil != null && ekleSil.equals("+");
					boolean cikar = ekleSil != null && ekleSil.equals("-");
					for (PersonelView personelView : personelList) {
						// User kullanici = personelView.getKullanici();
						Personel personel = personelView.getPdksPersonel();
						List<String> kullaniciAdresler = null;
						if (hareket)
							kullaniciAdresler = ccAdresList(personel.getHareketMail());
						else if (cc)
							kullaniciAdresler = ccAdresList(personel.getEmailCC());
						else if (bcc)
							kullaniciAdresler = ccAdresList(personel.getEmailBCC());
						else
							kullaniciAdresler = new ArrayList<String>();
						for (String eMail : dogruAdresler) {
							if (ekle) {
								if (!kullaniciAdresler.contains(eMail))
									kullaniciAdresler.add(eMail);
							} else if (cikar) {
								if (kullaniciAdresler.contains(eMail))
									kullaniciAdresler.remove(eMail);
							}
						}

						if (bcc)
							personelView.setBccAdres(adresGuncelle(kullaniciAdresler));
						else if (cc)
							personelView.setCcAdres(adresGuncelle(kullaniciAdresler));
						else if (hareket)
							personelView.setHareketAdres(adresGuncelle(kullaniciAdresler));

						kullaniciAdresler = null;
					}
				}

			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());

			}
			dosya = null;
		} else
			PdksUtil.addMessageWarn("Mail adreslerini kontrol ediniz!");
		adresler = null;
		dogruAdresler = null;
		setPersonelList(personelList);
		return "";
	}

	public String setYoneticiVeri() {
		try {
			Dosya dosya = ortakIslemler.getDosyaFromFileUpload(file);
			Workbook wb = ortakIslemler.getWorkbook(dosya);
			List<String> yoneticiList = new ArrayList<String>();
			LinkedHashMap<String, String> yoneticiSicilMap = new LinkedHashMap<String, String>();
			int hucre = -1;
			boolean yonetici1 = islemTipi.equals("Y1");
			boolean yonetici2 = islemTipi.equals("Y2");
			if (yonetici1)
				hucre = 2;
			else if (yonetici2)
				hucre = 4;
			if (hucre > 0 && wb != null) {
				Sheet sheet = wb.getSheetAt(0);
				for (int j = 1; j <= sheet.getLastRowNum(); j++) {
					String sicilNo = "";
					try {
						sicilNo = getSheetStringValue(sheet, j, 0);
						String yoneticiSicilNo = getSheetStringValue(sheet, j, hucre);
						if (sicilNo != null && PdksUtil.hasStringValue(yoneticiSicilNo)) {
							if (!yoneticiList.contains(yoneticiSicilNo))
								yoneticiList.add(yoneticiSicilNo);
							yoneticiSicilMap.put(sicilNo, yoneticiSicilNo);
						}
					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());
					}
				}
			}
			dosya = null;
			List<PersonelView> personelList = new ArrayList<PersonelView>();
			TreeMap<String, Personel> yoneticiMap = null;
			if (!yoneticiList.isEmpty()) {
				HashMap parametreMap = new HashMap();
				parametreMap.put(PdksEntityController.MAP_KEY_MAP, "getPdksSicilNo");
				parametreMap.put(PdksEntityController.MAP_KEY_SELECT, "pdksPersonel");
				parametreMap.put("pdksSicilNo", yoneticiList);
				if (session != null)
					parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
				yoneticiMap = pdksEntityController.getObjectByInnerObjectMapInLogic(parametreMap, PdksPersonelView.class, Boolean.FALSE);
				if (!yoneticiMap.isEmpty()) {
					List idList = new ArrayList(yoneticiSicilMap.keySet());
					String fieldName = "sicilNo";
					parametreMap.clear();
					parametreMap.put(fieldName, idList);
					if (session != null)
						parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
					List veriler = ortakIslemler.getParamList(false, idList, fieldName, parametreMap, PersonelKGS.class, session);
					List<PersonelView> list = ortakIslemler.getPersonelViewByPersonelKGSList(veriler);
					TreeMap<String, PersonelView> map1 = new TreeMap<String, PersonelView>();
					for (PersonelView personelView : list) {
						if (personelView.getPdksPersonel() != null) {
							personelView.setYonetici1(null);
							personelView.setYonetici2(null);
							map1.put(personelView.getSicilNo(), personelView);
						}
					}
					for (String key : yoneticiSicilMap.keySet()) {
						if (map1.containsKey(key))
							personelList.add(map1.get(key));

					}
					map1 = null;
					list = null;
					for (Iterator iterator = personelList.iterator(); iterator.hasNext();) {
						PersonelView personelView = (PersonelView) iterator.next();
						String yoneticiSicilNo = yoneticiSicilMap.containsKey(personelView.getPdksPersonel().getPdksSicilNo()) ? yoneticiSicilMap.get(personelView.getPdksPersonel().getPdksSicilNo()) : "";
						if (yoneticiMap.containsKey(yoneticiSicilNo)) {
							Personel yonetici = yoneticiMap.get(yoneticiSicilNo);
							if (yonetici.isCalisiyor()) {
								if (yonetici1)
									personelView.setYonetici1(yonetici);
								else if (yonetici2)
									personelView.setYonetici2(yonetici);
							}

						} else {

							PdksUtil.addMessageAvailableWarn(personelView.getKgsSicilNo() + " " + personelView.getAdSoyad() + " yönetici bilgisi tanımsız!");
						}
						if (personelView.getYonetici1() == null && personelView.getYonetici2() == null) {
							iterator.remove();

						}

					}
				}
				parametreMap = null;
			}

			setPersonelList(personelList);
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}
		// if (file1.exists())
		// file1.delete();
		return "";
	}

	/**
	 * @param sheet
	 * @param row
	 * @param col
	 * @return
	 * @throws Exception
	 */
	private String getSheetStringValue(Sheet sheet, int row, int col) throws Exception {
		String value = null;

		try {
			value = ExcelUtil.getSheetStringValue(sheet, row, col);
		} catch (Exception e) {
			value = String.valueOf(ExcelUtil.getSheetDoubleValue(sheet, row, col).longValue());

		}
		return value;
	}

	public void dosyaSifirla() {
		if (file == null)
			file.setData(null);
	}

	public void listener(UploadEvent event) throws Exception {
		UploadItem item = event.getUploadItem();
		if (file == null)
			file = new FileUpload();
		file.setName(item.getFileName());
		file.setLength(item.getData().length);
		file.setData(item.getData());
		personelList.clear();
	}

	@Transactional
	public String yoneticiGuncelle() {
		boolean yonetici1 = islemTipi.equals("Y1");
		boolean yonetici2 = islemTipi.equals("Y2");
		Date guncellemeTarihi = Calendar.getInstance().getTime();
		String personelERPGuncelleme = ortakIslemler.getParameterKey("personelERPOku");
		boolean personelERPGuncellemeDurum = personelERPGuncelleme != null && personelERPGuncelleme.equalsIgnoreCase("M");
		boolean flush = Boolean.FALSE;
		for (PersonelView personelView : personelList) {
			Personel pdksPersonel = personelView.getPdksPersonel();
			try {
				Boolean update = Boolean.FALSE;
				if (yonetici1) {
					if (pdksPersonel.getPdksYonetici() == null || !pdksPersonel.getPdksYonetici().getId().equals(personelView.getYonetici1().getId())) {
						pdksPersonel.setYoneticisi(personelView.getYonetici1());
						if (pdksPersonel.getAsilYonetici1() == null || personelERPGuncellemeDurum)
							pdksPersonel.setAsilYonetici1(personelView.getYonetici1());
						update = Boolean.TRUE;
					}
				} else if (yonetici2 && personelView.getYonetici2() != null) {
					Personel yoneticiPersonel2 = pdksPersonel.getYonetici2();
					Personel yoneticiAsilPersonel2 = pdksPersonel.getPdksYonetici();
					if (yoneticiAsilPersonel2 != null)
						yoneticiAsilPersonel2 = yoneticiAsilPersonel2.getPdksYonetici();
					if (yoneticiAsilPersonel2 != null && !yoneticiAsilPersonel2.isCalisiyor())
						yoneticiAsilPersonel2 = null;
					if (yoneticiAsilPersonel2 == null || !yoneticiPersonel2.getId().equals(personelView.getYonetici2().getId())) {
						if (yoneticiAsilPersonel2 != null && yoneticiAsilPersonel2.getId().equals(personelView.getYonetici2().getId())) {
							pdksPersonel.setAsilYonetici2(null);
						} else
							pdksPersonel.setAsilYonetici2(personelView.getYonetici2());
						update = Boolean.TRUE;
					}

				}

				if (update) {
					try {
						if (personelView.getSicilNo() != null && (pdksPersonel.getPdksSicilNo() == null || pdksPersonel.getPdksSicilNo().trim().equals(personelView.getSicilNo().trim())))
							pdksPersonel.setPdksSicilNo(personelView.getSicilNo().trim());
						pdksPersonel.setGuncelleyenUser(authenticatedUser);
						pdksPersonel.setGuncellemeTarihi(guncellemeTarihi);
						pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);
						flush = Boolean.TRUE;
					} catch (Exception e) {
						logger.error(e);
						flush = Boolean.FALSE;
						e.printStackTrace();
						break;
					}
				}
			} catch (Exception ee) {
				logger.error(ee);
				ee.printStackTrace();
				flush = Boolean.FALSE;
				break;
			}
		}
		personelList.clear();
		if (flush)
			session.flush();
		if (yonetici1)
			PdksUtil.addMessageInfo(ortakIslemler.yoneticiAciklama() + " güncellemesi yapılmıştır.");
		else if (yonetici2)
			PdksUtil.addMessageInfo(ortakIslemler.yonetici2Aciklama() + " güncellemesi yapılmıştır.");

		return "";
	}

	@Transactional
	public String adresGuncelle() {
		boolean bcc = islemTipi.indexOf(MailGrubu.TIPI_BCC) >= 0;
		boolean cc = !bcc && islemTipi.indexOf(MailGrubu.TIPI_CC) >= 0;
		boolean hareket = islemTipi.indexOf(MailGrubu.TIPI_HAREKET) >= 0;
		Date guncellemeTarihi = Calendar.getInstance().getTime();
		boolean flush = Boolean.FALSE;
		session.clear();
		for (PersonelView personelView : personelList) {
			// User kullanici = personelView.getKullanici();
			Personel pdksPersonel = personelView.getPdksPersonel();
			Boolean update = Boolean.FALSE;

			if (bcc) {
				if (pdksPersonel.getEmailBCC() == null || !pdksPersonel.getEmailBCC().equals(personelView.getBccAdres())) {
					pdksPersonel.setEmailBCC(ortakIslemler.getAktifMailAdress(personelView.getBccAdres(), session));
					update = Boolean.TRUE;
				}
			} else if (cc) {
				if (pdksPersonel.getEmailCC() == null || !pdksPersonel.getEmailCC().equals(personelView.getCcAdres())) {
					pdksPersonel.setEmailCC(ortakIslemler.getAktifMailAdress(personelView.getCcAdres(), session));
					update = Boolean.TRUE;
				}
			} else if (hareket) {
				if (pdksPersonel.getHareketMail() == null || !pdksPersonel.getHareketMail().equals(personelView.getHareketAdres())) {
					pdksPersonel.setHareketMail(ortakIslemler.getAktifMailAdress(personelView.getHareketAdres(), session));
					update = Boolean.TRUE;
				}
			}
			if (update) {
				try {
					pdksPersonel.setGuncelleyenUser(authenticatedUser);
					pdksPersonel.setGuncellemeTarihi(guncellemeTarihi);
					ortakIslemler.personelKaydet(pdksPersonel, session);
					// pdksEntityController.saveOrUpdate(session, entityManager, kullanici);
					pdksEntityController.saveOrUpdate(session, entityManager, pdksPersonel);
					flush = Boolean.TRUE;
				} catch (Exception e) {
					flush = Boolean.FALSE;
					e.printStackTrace();
					break;
				}

			}
		}
		if (flush)
			session.flush();
		if (bcc)
			PdksUtil.addMessageInfo("BCC mail adresleri güncellenmiştir.");
		else if (cc)
			PdksUtil.addMessageInfo("CC mail adresleri güncellenmiştir.");
		else if (hareket)
			PdksUtil.addMessageInfo("Hareket mail adresleri güncellenmiştir.");
		personelList.clear();
		return "";
	}

	public List<String> ccAdresList(String adres) {
		List<String> adresler = new ArrayList<String>();
		if (PdksUtil.hasStringValue(adres)) {
			if (adres.indexOf(",") > 0)
				adresler.addAll(Arrays.asList(adres.split(",")));
			else
				adresler.add(adres);
		}
		return adresler;

	}

	public String adresGuncelle(List<String> ccAdresList) {
		StringBuilder sb = new StringBuilder();
		if (ccAdresList != null) {
			if (ccAdresList.size() > 1) {
				TreeMap<String, String> map1 = new TreeMap<String, String>();
				for (String adres : ccAdresList) {
					try {
						String eMail = PdksUtil.getInternetAdres(adres);
						if (PdksUtil.hasStringValue(eMail))
							map1.put(eMail, eMail);
					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());

					}

				}
				ccAdresList.clear();
				if (!map1.isEmpty()) {
					List<String> adresler = new ArrayList<String>(map1.values());
					ccAdresList.addAll(adresler);
					adresler = null;
				}
				map1 = null;
			}

			for (Iterator iterator = ccAdresList.iterator(); iterator.hasNext();) {
				String adres = (String) iterator.next();
				if (PdksUtil.hasStringValue(adres))
					sb.append((sb.length() > 0 ? "," : "") + adres.trim());
			}
		}
		String ccAdres = sb.toString();
		sb = null;
		return ccAdres;

	}

	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public String sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		islemTipi = "";
		mailAdres = "";
		ekleSil = "";

		islemList = ortakIslemler.getSelectItemList("islem", authenticatedUser);

		islemList.add(new SelectItem("Y1", ortakIslemler.yoneticiAciklama() + " güncelle"));
		islemList.add(new SelectItem("Y2", ortakIslemler.yonetici2Aciklama() + " güncelle"));
		islemList.add(new SelectItem(MailGrubu.TIPI_CC, "CC Mail"));

		islemList.add(new SelectItem(MailGrubu.TIPI_BCC, "BCC Mail"));

		if (ortakIslemler.getParameterKey("hareketMailGrubu").equals("1")) {

			islemList.add(new SelectItem(MailGrubu.TIPI_HAREKET, "Hareket Mail"));
		}
		if (file != null)
			file.setData(null);
		personelList.clear();
		return ortakIslemler.yetkiIKAdmin(Boolean.FALSE);
	}

	public List<PersonelView> getPersonelList() {
		return personelList;
	}

	public void setPersonelList(List<PersonelView> personelList) {
		this.personelList = personelList;
	}

	public FileUpload getFile() {
		return file;
	}

	public void setFile(FileUpload file) {
		this.file = file;
	}

	public List<SelectItem> getIslemList() {
		return islemList;
	}

	public void setIslemList(List<SelectItem> islemList) {
		this.islemList = islemList;
	}

	public String getMailAdres() {
		return mailAdres;
	}

	public void setMailAdres(String mailAdres) {
		this.mailAdres = mailAdres;
	}

	public String getEkleSil() {
		return ekleSil;
	}

	public void setEkleSil(String ekleSil) {
		this.ekleSil = ekleSil;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getIslemTipi() {
		return islemTipi;
	}

	public void setIslemTipi(String islemTipi) {
		this.islemTipi = islemTipi;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		PersonelKulllaniciExcelUpdateHome.sayfaURL = sayfaURL;
	}
}
