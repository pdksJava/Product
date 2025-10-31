package org.pdks.session;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.AramaSecenekleri;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.HareketKGS;
import org.pdks.entity.KapiKGS;
import org.pdks.entity.KapiView;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelFazlaMesai;
import org.pdks.entity.PersonelHareketIslem;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelView;
import org.pdks.entity.Sirket;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.VardiyaSaat;
import org.pdks.entity.YemekIzin;
import org.pdks.security.entity.User;

@Name("calismaSaatleriHome")
public class CalismaSaatleriHome extends EntityHome<VardiyaGun> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5596157513342417469L;
	static Logger logger = Logger.getLogger(CalismaSaatleriHome.class);

	@RequestParameter
	Long kgsHareketId;
	@In(create = true)
	PdksEntityController pdksEntityController;
	@In(required = false)
	User authenticatedUser;
	@In(required = false, create = true)
	OrtakIslemler ortakIslemler;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	List<User> userList;

	@In(required = false, create = true)
	HashMap parameterMap;

	public static String sayfaURL = "calismaSaatleri";

	List<HareketKGS> hareketList = new ArrayList<HareketKGS>();
	List<VardiyaGun> vardiyaGunList = new ArrayList<VardiyaGun>();

	private String islemTipi, bolumAciklama, sicilNo;
	private Date date;
	private AramaSecenekleri aramaSecenekleri = null;
	private boolean tesisDurum = false, digerTesisDurum = false;
	private Session session;

	@In(required = false)
	FacesMessages facesMessages;

	@Override
	public Object getId() {
		if (kgsHareketId == null) {
			return super.getId();
		} else {
			return kgsHareketId;
		}
	}

	@Override
	@Begin(join = true)
	public void create() {
		super.create();
	}

	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		sicilNo = "";
		setHareketList(new ArrayList<HareketKGS>());
		setVardiyaGunList(new ArrayList<VardiyaGun>());
		HareketKGS hareket = new HareketKGS();
		hareket.setPersonel(new PersonelView());
		hareket.setKapiView(new KapiView());
		hareket.setIslem(new PersonelHareketIslem());
		setDate(new Date());

		if (aramaSecenekleri == null)
			aramaSecenekleri = new AramaSecenekleri(authenticatedUser);
		fillEkSahaTanim();
		if (aramaSecenekleri.getDepartmanId() == null) {
			aramaSecenekleri.setDepartman(authenticatedUser.getDepartman());
			aramaSecenekleri.setDepartmanId(authenticatedUser.getDepartman().getId());
		}
		tesisDurum = false;
		digerTesisDurum = false;
		fillSirketList();
	}

	public String fillSirketList() {
		Date bugun = PdksUtil.getDate(date);
		ortakIslemler.setAramaSecenekSirketVeTesisData(aramaSecenekleri, bugun, bugun, false, session);
		clearVardiyaList();
		return "";
	}

	public String fillTesisList() {
		clearVardiyaList();
		Date bugun = PdksUtil.getDate(date);
		ortakIslemler.setAramaSecenekTesisData(aramaSecenekleri, bugun, bugun, false, session);
		return "";
	}

	private void clearVardiyaList() {
		hareketList.clear();
		vardiyaGunList.clear();
	}

	private void fillEkSahaTanim() {
		ortakIslemler.fillEkSahaTanimAramaSecenekAta(session, Boolean.FALSE, Boolean.TRUE, aramaSecenekleri);
		HashMap sonucMap = ortakIslemler.fillEkSahaTanim(session, Boolean.FALSE, null);
		bolumAciklama = (String) sonucMap.get("bolumAciklama");
	}

	public void hareketGoster(VardiyaGun pdksVardiyaGun) {
		setInstance(pdksVardiyaGun);
		List<HareketKGS> kgsList = pdksVardiyaGun.getHareketler();
		setHareketList(kgsList);

	}

	public String fillHareketList() {
		try {
			clearVardiyaList();
			if (ortakIslemler.ileriTarihSeciliDegil(date))
				fillHareketListOlustur();
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		return "";

	}

	public void fillHareketListOlustur() {
		Calendar cal = Calendar.getInstance();
		List<VardiyaGun> vardiyaList = new ArrayList<VardiyaGun>();
		List<HareketKGS> kgsList = new ArrayList<HareketKGS>();
		List<Long> perIdList = new ArrayList<Long>();
		// List<Personel> tumPersoneller = new ArrayList<Personel>(authenticatedUser.getTumPersoneller());
		Date oncekiGun = ortakIslemler.tariheGunEkleCikar(cal, date, -1);
		HashMap map = new HashMap();
		map.put("pdks=", Boolean.TRUE);
		map.put("durum=", Boolean.TRUE);
		if (PdksUtil.hasStringValue(sicilNo))
			map.put("pdksSicilNo=", sicilNo);
		if (aramaSecenekleri.getSirketId() != null)
			map.put("sirket.id=", aramaSecenekleri.getSirketId());
		else {
			map.put("sirket.pdks=", true);
			if (aramaSecenekleri.getDepartmanId() != null)
				map.put("sirket.departman.id=", aramaSecenekleri.getDepartmanId());
		}
		if (aramaSecenekleri.getTesisId() != null)
			map.put("tesis.id=", aramaSecenekleri.getTesisId());
		map.put("sskCikisTarihi>=", oncekiGun);
		map.put("iseBaslamaTarihi<=", date);
		if (session != null)
			map.put(PdksEntityController.MAP_KEY_SESSION, session);
		ArrayList<Personel> tumPersoneller = (ArrayList<Personel>) pdksEntityController.getObjectByInnerObjectListInLogic(map, Personel.class);

		boolean sicilDolu = PdksUtil.hasStringValue(sicilNo);
		for (Iterator iterator = tumPersoneller.iterator(); iterator.hasNext();) {
			Personel per = (Personel) iterator.next();
			if (sicilDolu) {
				if (!per.getPdksSicilNo().equals(sicilNo))
					iterator.remove();
			} else if (per.getSirket().isPdksMi() && per.getPdks().equals(Boolean.TRUE))
				perIdList.add(per.getId());
			else
				iterator.remove();
		}

		TreeMap<String, VardiyaGun> vardiyaMap = null;
		try {
			if (!tumPersoneller.isEmpty())
				vardiyaMap = ortakIslemler.getIslemVardiyalar(tumPersoneller, ortakIslemler.tariheGunEkleCikar(cal, date, -1), date, Boolean.FALSE, session, Boolean.TRUE);

		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		if (vardiyaMap == null)
			vardiyaMap = new TreeMap<String, VardiyaGun>();
		vardiyaList = new ArrayList<VardiyaGun>(vardiyaMap.values());
		Collections.reverse(vardiyaList);
		Date bugun = new Date();
		int gunDurum = PdksUtil.tarihKarsilastirNumeric(date, bugun);
		Date tarih1 = null;
		Date tarih2 = null;
		Date tarih3 = null;
		Date tarih4 = null;
		perIdList.clear();
		List<Long> idList = new ArrayList<Long>();
		for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
			VardiyaGun pdksVardiyaGun = (VardiyaGun) iterator.next();
			Personel personel = pdksVardiyaGun.getPersonel();
			Long personelId = personel.getId();

			Vardiya islemVardiya = pdksVardiyaGun.getIslemVardiya(), vardiya = pdksVardiyaGun.getVardiya();

			boolean sil = false;
			if (islemVardiya == null || gunDurum == 1 || vardiya.getId() == null || perIdList.contains(personelId)) {
				sil = true;
			} else if (islemVardiya.isCalisma() == false) {
				sil = PdksUtil.tarihKarsilastirNumeric(pdksVardiyaGun.getVardiyaDate(), date) != 0;
			} else {
				if (pdksVardiyaGun.getVardiyaDate().before(date)) {
					if (pdksVardiyaGun.getIzin() != null || !(islemVardiya.getBitDonem() < islemVardiya.getBasDonem() && gunDurum == 0))
						sil = true;

				} else {
					if (islemVardiya.getBitDonem() < islemVardiya.getBasDonem() && gunDurum == 0 && bugun.before(islemVardiya.getVardiyaBasZaman()))
						sil = true;
				}
			}
			if (sil) {
				iterator.remove();
				continue;
			} else if (islemVardiya.isCalisma())
				logger.debug(pdksVardiyaGun.getVardiyaDateStr() + " " + islemVardiya.getAdi());
			perIdList.add(personelId);

			if ((tarih1 == null && tarih3 == null) || islemVardiya.getVardiyaBasZaman().getTime() < tarih3.getTime()) {
				tarih3 = islemVardiya.getVardiyaBasZaman();
				tarih1 = islemVardiya.getVardiyaFazlaMesaiBasZaman();

			}
			if (pdksVardiyaGun.getId() != null)
				idList.add(pdksVardiyaGun.getId());
			if (tarih2 == null || islemVardiya.getVardiyaBitZaman().getTime() > tarih4.getTime()) {
				tarih4 = islemVardiya.getVardiyaBitZaman();
				tarih2 = islemVardiya.getVardiyaFazlaMesaiBitZaman();

			}

		}
		List<Long> kapiIdler = !tumPersoneller.isEmpty() ? ortakIslemler.getPdksDonemselKapiIdler(tarih1, tarih2, session) : null;

		if (kapiIdler != null && !kapiIdler.isEmpty())
			try {
				kgsList = null;
				kgsList = ortakIslemler.getPdksHareketBilgileri(Boolean.TRUE, kapiIdler, tumPersoneller, tarih1, tarih2, HareketKGS.class, session);

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}
		if (kgsList == null)
			kgsList = new ArrayList<HareketKGS>();
		tumPersoneller = null;
		if (!kgsList.isEmpty())
			kgsList = PdksUtil.sortListByAlanAdi(kgsList, "zaman", Boolean.FALSE);
		TreeMap<Long, List<HareketKGS>> hareketMap = new TreeMap<Long, List<HareketKGS>>();
		for (HareketKGS hareketKGS : kgsList) {
			Personel personel = hareketKGS.getPersonel() != null ? hareketKGS.getPersonel().getPdksPersonel() : null;
			if (personel != null) {
				List<HareketKGS> list = hareketMap.containsKey(personel.getId()) ? hareketMap.get(personel.getId()) : new ArrayList<HareketKGS>();
				if (list.isEmpty())
					hareketMap.put(personel.getId(), list);
				list.add(hareketKGS);
			}
		}
		TreeMap<Long, List<PersonelFazlaMesai>> fmMap = new TreeMap<Long, List<PersonelFazlaMesai>>();
		if (!idList.isEmpty()) {
			HashMap parametreMap2 = new HashMap();
			parametreMap2.put("vardiyaGun.id", idList);
			if (session != null)
				parametreMap2.put(PdksEntityController.MAP_KEY_SESSION, session);
			List<PersonelFazlaMesai> fmList = pdksEntityController.getObjectByInnerObjectList(parametreMap2, PersonelFazlaMesai.class);
			for (PersonelFazlaMesai personelFazlaMesai : fmList) {
				if (personelFazlaMesai.getDurum() && personelFazlaMesai.isOnaylandi()) {
					Long key = personelFazlaMesai.getVardiyaGun().getId();
					List<PersonelFazlaMesai> list = fmMap.containsKey(key) ? fmMap.get(key) : new ArrayList<PersonelFazlaMesai>();
					if (list.isEmpty())
						fmMap.put(key, list);
					list.add(personelFazlaMesai);
				}

			}
			fmList = null;
		}

		try {

			cal.setTime(date);

			DenklestirmeAy da = (DenklestirmeAy) ortakIslemler.getSQLDenklestirmeAy(cal, session);
			Double yemekMolasiYuzdesi = ortakIslemler.getYemekMolasiYuzdesi(da, session);

			List<YemekIzin> yemekGenelList = ortakIslemler.getYemekList(tarih1, tarih2, session);
			ortakIslemler.setVardiyaYemekList(vardiyaList, yemekGenelList);
			// List<VardiyaGun> gunList = new ArrayList<VardiyaGun>();
			for (Iterator iterator = vardiyaList.iterator(); iterator.hasNext();) {
				VardiyaGun vardiyaGun = (VardiyaGun) iterator.next();
				// VardiyaGun vardiyaGun = vg.getDurum() == false ? (VardiyaGun) vg.clone() : vg;
				// gunList.add(vardiyaGun);
				List<YemekIzin> yemekList = vardiyaGun.getYemekList();
				Long personelId = vardiyaGun.getPersonel().getId();
				vardiyaGun.setHareketler(null);
				vardiyaGun.setGirisHareketleri(null);
				vardiyaGun.setCikisHareketleri(null);
				vardiyaGun.setGecersizHareketler(null);
				Vardiya vardiya = vardiyaGun.getVardiya();
				if (hareketMap.containsKey(personelId)) {
					List<HareketKGS> list = hareketMap.get(personelId);
					for (Iterator iterator1 = list.iterator(); iterator1.hasNext();) {
						HareketKGS kgsHareket = (HareketKGS) iterator1.next();
						if (vardiyaGun.addHareket(kgsHareket, Boolean.TRUE)) {
							iterator1.remove();
						}
					}
				}

				int giris = vardiyaGun.getGirisHareketleri() != null ? vardiyaGun.getGirisHareketleri().size() : 0;
				int cikis = vardiyaGun.getCikisHareketleri() != null ? vardiyaGun.getCikisHareketleri().size() : 0;
				double calismaSuresi = 0.0d, fazlaMesaiSure = 0.0d, resmiMesaiSure = 0.0d;
				boolean hesapla = true;
				if (vardiyaGun.getDurum() && vardiyaGun.getVardiyaSaat() != null) {
					VardiyaSaat vs = vardiyaGun.getVardiyaSaat();
					calismaSuresi = vs.getCalismaSuresi();
					resmiMesaiSure = vs.getResmiTatilToplamSure();
					hesapla = calismaSuresi == 0.0d;

				} else
					vardiyaGun.setVardiyaSaatDB(new VardiyaSaat());

				if (!vardiyaGun.isHareketHatali() && giris > 0 && cikis == giris) {
					List<String> hIdList = new ArrayList<String>();
					double toplamYemekSuresi = 0.0d;
					for (int i = 0; i < vardiyaGun.getGirisHareketleri().size(); i++) {
						HareketKGS girisHareket = vardiyaGun.getGirisHareketleri().get(i);
						HareketKGS cikisHareket = vardiyaGun.getCikisHareketleri().get(i);
						if (girisHareket.getId() != null)
							hIdList.add(girisHareket.getId());
						if (cikisHareket.getId() != null)
							hIdList.add(cikisHareket.getId());
						if (hesapla) {
							double yemeksizSure = ortakIslemler.getSaatSure(girisHareket.getZaman(), cikisHareket.getZaman(), yemekList, vardiyaGun, session);
							double sure = PdksUtil.getDakikaFarki(cikisHareket.getZaman(), girisHareket.getZaman()) / 60.0d;
							toplamYemekSuresi += sure - yemeksizSure;
							calismaSuresi += sure;

						}
					}
					if (vardiyaGun.getId() != null && fmMap.containsKey(vardiyaGun.getId())) {
						List<PersonelFazlaMesai> list = fmMap.get(vardiyaGun.getId());
						for (PersonelFazlaMesai personelFazlaMesai2 : list) {
							if (personelFazlaMesai2.getHareketId() != null && hIdList.contains(personelFazlaMesai2.getHareketId()))
								fazlaMesaiSure += personelFazlaMesai2.getFazlaMesaiSaati();
						}
					}
					hIdList = null;
					if (!hesapla)
						calismaSuresi -= fazlaMesaiSure;
					else {
						Double vardiyaYemekSuresi = vardiya != null ? vardiya.getYemekSuresi().doubleValue() / 60.0d : 0.0d;
						Double netSuresi = vardiya.getNetCalismaSuresi();
						if (vardiyaYemekSuresi > toplamYemekSuresi && netSuresi * yemekMolasiYuzdesi <= calismaSuresi) {
							calismaSuresi -= vardiyaYemekSuresi;
						} else
							calismaSuresi -= toplamYemekSuresi;
					}

				}

				int yarimYuvarla = vardiyaGun.getYarimYuvarla();
				vardiyaGun.setCalismaSuresi(PdksUtil.setSureDoubleTypeRounded(calismaSuresi, yarimYuvarla));
				vardiyaGun.setFazlaMesaiSure(fazlaMesaiSure);
				vardiyaGun.setResmiTatilSure(resmiMesaiSure);
			}

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}
		setVardiyaGunList(vardiyaList);

	}

	public String calismaSaatleriExcel() {
		try {

			ByteArrayOutputStream baosDosya = calismaSaatleriExcelDevam();
			if (baosDosya != null) {
				String dosyaAdi = "ÇalışmaSaatleri_" + PdksUtil.convertToDateString(date, "yyyyMMdd") + ".xlsx";
				PdksUtil.setExcelHttpServletResponse(baosDosya, dosyaAdi);
			}
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());

		}

		return "";
	}

	public ByteArrayOutputStream calismaSaatleriExcelDevam() {
		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, ortakIslemler.vardiyaAciklama() + " Listesi", false);
		// Sheet sheetHareket = ExcelUtil.createSheet(wb, "Hareket  Listesi", false);
		CellStyle header = ExcelUtil.getStyleHeader(wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddDateTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenDateTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);

		int row = 0;
		int col = 0;
		boolean tesisDurum = ortakIslemler.getListTesisDurum(vardiyaGunList), izinDurum = false, hareketDurum = false, fazlaMesaiDurum = false, eksikCalismaDurum = false;
		for (VardiyaGun calismaPlani : vardiyaGunList) {
			Vardiya vardiya = calismaPlani.getVardiya();
			if (vardiya == null || vardiya.getId() == null)
				continue;
			if (fazlaMesaiDurum == false)
				fazlaMesaiDurum = calismaPlani.getFazlaMesaiSure() > 0.0d || (calismaPlani.getDurum() == false && calismaPlani.getHareketler() != null);
			if (eksikCalismaDurum == false && vardiya.isCalisma() && calismaPlani.getCalismaSuresi() > 0.0d)
				eksikCalismaDurum = calismaPlani.getCalismaSuresi() - calismaPlani.getFazlaMesaiSure() < calismaPlani.getVardiya().getNetCalismaSuresi();
			if (izinDurum == false)
				izinDurum = calismaPlani.getVardiya().isCalisma() == false || calismaPlani.isIzinli();
			if (hareketDurum == false)
				hareketDurum = calismaPlani.getHareketler() != null && !calismaPlani.getHareketler().isEmpty();
			if (izinDurum && hareketDurum && fazlaMesaiDurum)
				break;

		}
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Personel");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.yoneticiAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.sirketAciklama());
		if (tesisDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.tesisAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(bolumAciklama);
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Çalışma Süresi");
		if (fazlaMesaiDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Fazla Çalışma");
		if (eksikCalismaDurum) {
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Eksik Çalışma");
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Eksik Çalışma Süre");
		}

		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.vardiyaAciklama());
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("İlk Giriş");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Son Çıkış");
		if (izinDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("İzin Durum");
		if (hareketDurum)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Hareketler");

		col = 0;

		boolean manuelGiris = false;
		for (VardiyaGun calismaPlani : vardiyaGunList) {
			if (calismaPlani.getHareketler() != null && !calismaPlani.getHareketler().isEmpty()) {
				for (HareketKGS hareketKGS : calismaPlani.getHareketler()) {
					if (hareketKGS.getIslem() != null) {
						manuelGiris = true;
						break;
					}
				}
				if (manuelGiris)
					break;
			}
		}

		boolean renk = true;
		for (VardiyaGun calismaPlani : vardiyaGunList) {
			row++;
			col = 0;
			CellStyle style = null, styleCenter = null, cellStyleDateTime = null;
			if (renk) {
				cellStyleDateTime = styleOddDateTime;
				style = styleOdd;
				styleCenter = styleOddCenter;
			} else {
				cellStyleDateTime = styleEvenDateTime;
				style = styleEven;
				styleCenter = styleEvenCenter;
			}
			renk = !renk;
			Personel personel = calismaPlani.getPersonel();
			List<HareketKGS> hareketler = calismaPlani.getHareketler();
			Sirket sirket = personel.getSirket();
			Vardiya vardiya = calismaPlani.getIslemVardiya();
			ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(personel.getPdksSicilNo());
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getAdSoyad());
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getYoneticisi() != null && personel.getYoneticisi().isCalisiyorGun(date) ? personel.getYoneticisi().getAdSoyad() : "");
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket.getAd());
			if (tesisDurum)
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getTesis() != null ? personel.getTesis().getAciklama() : "");
			ExcelUtil.getCell(sheet, row, col++, style).setCellValue(personel.getEkSaha3() != null ? personel.getEkSaha3().getAciklama() : "");
			ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(vardiya.isCalisma() && calismaPlani.getCalismaSuresi() > 0.0d ? authenticatedUser.sayiFormatliGoster(calismaPlani.getCalismaSuresi()) + " saat" : "");
			if (fazlaMesaiDurum) {
				String str = "";
				if (calismaPlani.getFazlaMesaiSure() > 0.0d)
					str = authenticatedUser.sayiFormatliGoster(calismaPlani.getFazlaMesaiSure()) + " saat";
				else if (calismaPlani.getDurum() == false && hareketler != null)
					str = calismaPlani.getCalismaSuresi() > 0.0d ? "Onaysız" : "Hatalı Kart";
				ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(str);

			}
			if (eksikCalismaDurum) {
				boolean eksikCalisma = false;
				if (vardiya.isCalisma() && calismaPlani.getCalismaSuresi() > 0.0d) {
					double sure = calismaPlani.getVardiya().getNetCalismaSuresi() - (calismaPlani.getCalismaSuresi() - calismaPlani.getFazlaMesaiSure());
					if (sure > 0) {
						List<HareketKGS> perHareketler = calismaPlani.getHareketler();
						String strGecGelme = "", strErkenCikma = "";
						Date girisZaman = perHareketler.get(0).getOrjinalZaman(), cikisZaman = perHareketler.get(perHareketler.size() - 1).getOrjinalZaman();
						if (vardiya.getVardiyaTelorans2BasZaman().before(girisZaman))
							strGecGelme = "Geç Giriş";
						else if (vardiya.getVardiyaTelorans1BasZaman().after(girisZaman))
							strGecGelme = "Erken Giriş";
						if (vardiya.getVardiyaTelorans1BitZaman().after(cikisZaman))
							strErkenCikma = (strGecGelme.equals("") ? "" : " + ") + "Erken Çıkış";
						else if (vardiya.getVardiyaTelorans2BitZaman().before(cikisZaman))
							strErkenCikma = (strGecGelme.equals("") ? "" : " + ") + "Geç Çıkış";
						ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(strGecGelme + strErkenCikma);
						ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(authenticatedUser.sayiFormatliGoster(sure) + " saat");
						eksikCalisma = true;
					}
				}
				if (eksikCalisma == false) {
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
					ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
				}
			}
			ExcelUtil.getCell(sheet, row, col++, styleCenter).setCellValue(vardiya.isCalisma() ? authenticatedUser.dateFormatla(calismaPlani.getVardiyaDate()) + " " + vardiya.getAciklama() : "");
			if (calismaPlani.getGirisHareket() != null)
				ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(calismaPlani.getGirisHareket().getOrjinalZaman());
			else
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");

			if (calismaPlani.getCikisHareket() != null)
				ExcelUtil.getCell(sheet, row, col++, cellStyleDateTime).setCellValue(calismaPlani.getCikisHareket().getOrjinalZaman());
			else
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue("");
			if (izinDurum) {
				StringBuffer sb = new StringBuffer();
				if (vardiya.isCalisma() == false || calismaPlani.isIzinli()) {
					PersonelIzin izin = calismaPlani.getIzin();
					if (izin != null) {
						sb.append(izin.getIzinTipi().getIzinTipiTanim().getAciklama() + "\n");
						sb.append(authenticatedUser.dateFormatla(izin.getBaslangicZamani()) + " - ");
						sb.append(authenticatedUser.dateFormatla(izin.getBitisZamani()));

					} else
						sb.append(vardiya.getAdi());
				}
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sb.toString());
			}
			if (hareketDurum) {
				StringBuffer sb = new StringBuffer();
				if (hareketler != null) {
					for (Iterator iterator = calismaPlani.getHareketler().iterator(); iterator.hasNext();) {
						HareketKGS hareketKGS = (HareketKGS) iterator.next();
						KapiKGS kapiKGS = hareketKGS.getKapiKGS();
						String kapiAciklama = kapiKGS.getKapi() != null ? kapiKGS.getKapi().getAciklama() : kapiKGS.getAciklamaKGS();
						sb.append(kapiAciklama + " --> " + authenticatedUser.dateTimeFormatla(hareketKGS.getOrjinalZaman()) + (iterator.hasNext() ? "\n" : ""));

					}
				}
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sb.toString());
			}

		}
		try {

			for (int i = 0; i < col; i++)
				sheet.autoSizeColumn(i);
			// for (int i = 0; i < colHareket; i++)
			// sheetHareket.autoSizeColumn(i);
			ortakIslemler.vardiyaHareketExcel(date, vardiyaGunList, bolumAciklama, wb);
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

	public List<HareketKGS> getHareketList() {
		return hareketList;
	}

	public void setHareketList(List<HareketKGS> hareketList) {
		this.hareketList = hareketList;
	}

	public String getIslemTipi() {
		return islemTipi;
	}

	public void setIslemTipi(String islemTipi) {
		this.islemTipi = islemTipi;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public List<VardiyaGun> getVardiyaGunList() {
		return vardiyaGunList;
	}

	public void setVardiyaGunList(List<VardiyaGun> vardiyaGunList) {
		this.vardiyaGunList = vardiyaGunList;
	}

	public String getBolumAciklama() {
		return bolumAciklama;
	}

	public void setBolumAciklama(String bolumAciklama) {
		this.bolumAciklama = bolumAciklama;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public String getSicilNo() {
		return sicilNo;
	}

	public void setSicilNo(String sicilNo) {
		this.sicilNo = sicilNo;
	}

	public AramaSecenekleri getAramaSecenekleri() {
		return aramaSecenekleri;
	}

	public void setAramaSecenekleri(AramaSecenekleri aramaSecenekleri) {
		this.aramaSecenekleri = aramaSecenekleri;
	}

	public boolean isTesisDurum() {
		return tesisDurum;
	}

	public void setTesisDurum(boolean tesisDurum) {
		this.tesisDurum = tesisDurum;
	}

	public boolean isDigerTesisDurum() {
		return digerTesisDurum;
	}

	public void setDigerTesisDurum(boolean digerTesisDurum) {
		this.digerTesisDurum = digerTesisDurum;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		CalismaSaatleriHome.sayfaURL = sayfaURL;
	}

}
