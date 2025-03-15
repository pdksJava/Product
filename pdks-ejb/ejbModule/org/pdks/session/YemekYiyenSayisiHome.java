package org.pdks.session;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
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
import org.pdks.entity.HareketKGS;
import org.pdks.entity.KapiView;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelView;
import org.pdks.entity.Sirket;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.YemekKartsiz;
import org.pdks.entity.YemekOgun;
import org.pdks.security.entity.User;

@Name("yemekYiyenSayisiHome")
public class YemekYiyenSayisiHome extends EntityHome<VardiyaGun> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2436201612751631719L;
	static Logger logger = Logger.getLogger(YemekYiyenSayisiHome.class);

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
	@In(required = false)
	FacesMessages facesMessages;

	public static String sayfaURL = "yemekYiyenSayisi";
	private List<HareketKGS> hareketList = new ArrayList<HareketKGS>();
	private List<HareketKGS> toplamYemekList = new ArrayList<HareketKGS>();
	private List<HareketKGS> ciftYemekList = new ArrayList<HareketKGS>();
	private List<YemekOgun> yemekList;
	private List<Long> yemekKapiList;

	private Date basTarih;
	private Date bitTarih;
	private HareketKGS hareketKGS;
	private boolean ogunVar = false;
	private Session session;

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

	public void hareketGoster(HareketKGS hareket) {
		setHareketKGS(hareket);

	}

	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public void sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
 		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		ogunVar = false;

		setHareketList(new ArrayList<HareketKGS>());
		setCiftYemekList(new ArrayList<HareketKGS>());
		setToplamYemekList(new ArrayList<HareketKGS>());
		HareketKGS hareket = new HareketKGS();
		hareket.setPersonel(new PersonelView());
		hareket.setKapiView(new KapiView());
		// setBasTarih(new Date());
		// setBitTarih(new Date());
		fillYemekKapiList();

	}

	public void fillYemekList() {
		HashMap parametreMapYemek = new HashMap();
		parametreMapYemek.put("bitTarih>=", basTarih);
		parametreMapYemek.put("basTarih<=", bitTarih);
		parametreMapYemek.put("durum=", Boolean.TRUE);
		if (session != null)
			parametreMapYemek.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<YemekOgun> list = pdksEntityController.getObjectByInnerObjectListInLogic(parametreMapYemek, YemekOgun.class);
		if (list.size() > 1)
			list = PdksUtil.sortListByAlanAdi(list, "baslangicSaat", Boolean.FALSE);
		setYemekList(list);

	}

	public void fillYemekKapiList() {
		setYemekKapiList(ortakIslemler.getYemekKapiIdList(session));
	}

	/**
	 * @param hareketler
	 * @param mükerrer
	 * @return
	 * @throws Exception
	 */
	public ByteArrayOutputStream excelDevam(List<HareketKGS> hareketler, boolean mükerrer) throws Exception {
		ByteArrayOutputStream baos = null;

		Workbook wb = new XSSFWorkbook();

		Sheet sheet = ExcelUtil.createSheet(wb, (mükerrer ? "Mükerrer " : "") + "Yemek yiyenler", Boolean.TRUE);

		CellStyle header = ExcelUtil.getStyleHeader(wb);

		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddDateTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenDateTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);

		int row = 0;
		ExcelUtil.getCell(sheet, row, 0, header).setCellValue("Yemek Zamanı");
		ExcelUtil.getCell(sheet, row, 1, header).setCellValue("Adı Soyadı");
		ExcelUtil.getCell(sheet, row, 2, header).setCellValue(ortakIslemler.personelNoAciklama());
		ExcelUtil.getCell(sheet, row, 3, header).setCellValue(ortakIslemler.sirketAciklama());
		int sayac = 4;

		if (ogunVar)
			ExcelUtil.getCell(sheet, row, sayac++, header).setCellValue("Öğün Tipi");
		ExcelUtil.getCell(sheet, row, sayac++, header).setCellValue("Yemek Yeri");
		ExcelUtil.getCell(sheet, row, sayac++, header).setCellValue("Yemek Durum");
		ExcelUtil.getCell(sheet, row, sayac++, header).setCellValue("Mükerrer Geçerli");
		ExcelUtil.getCell(sheet, row, sayac++, header).setCellValue("Öncek Giriş Zamanı");
		boolean renk = true;
		for (Iterator iter = hareketList.iterator(); iter.hasNext();) {
			HareketKGS yemek = (HareketKGS) iter.next();
			row++;
			String adSoyad = yemek.getAdSoyad();
			CellStyle style = null, styleCenter = null, stylDateTime = null;
			if (renk) {
				stylDateTime = styleOddDateTime;
				style = styleOdd;
				styleCenter = styleOddCenter;
			} else {
				stylDateTime = styleEvenDateTime;
				style = styleEven;
				styleCenter = styleEvenCenter;
			}
			renk = !renk;
			try {
				ExcelUtil.getCell(sheet, row, 0, stylDateTime).setCellValue(yemek.getZaman());
				ExcelUtil.getCell(sheet, row, 1, style).setCellValue(adSoyad);
				ExcelUtil.getCell(sheet, row, 2, styleCenter).setCellValue(yemek.getSicilNo());
				String sirket = "", ogun = "", durum = "";
				try {
					sirket = yemek.getPersonel().getPdksPersonel().getSirket().getAd();
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					sirket = "" + ortakIslemler.sirketAciklama() + " tanımsız";
				}
				ExcelUtil.getCell(sheet, row, 3, style).setCellValue(sirket);

				sayac = 4;
				if (ogunVar) {
					try {
						ogun = yemek.getYemekOgun().getYemekAciklama();
					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());
						sirket = "Öğün tanımsız";
					}

					ExcelUtil.getCell(sheet, row, sayac++, style).setCellValue(ogun);
				}
				ExcelUtil.getCell(sheet, row, sayac++, style).setCellValue(yemek.getKapiView().getAciklama());
				try {
					durum = yemek.getDurumu();
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					durum = "";
				}
				ExcelUtil.getCell(sheet, row, sayac++, style).setCellValue(durum);
				ExcelUtil.getCell(sheet, row, sayac++, style).setCellValue(yemek.getGecerliYemek() != null && yemek.getGecerliYemek() ? 1 : (yemek.isCokluOgun() ? 2 : 0));
				if (yemek.getOncekiYemekZamani() != null)
					ExcelUtil.getCell(sheet, row, sayac++, stylDateTime).setCellValue(yemek.getOncekiYemekZamani());
				else
					ExcelUtil.getCell(sheet, row, sayac++, style).setCellValue("");
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				logger.debug(row);

			}
		}
		double katsayi = 3.43;
		int[] dizi = new int[] { 1575, 1056, 2011, 2056, 2011, 2056, 3722, 2078, 2600, 2000, 2000 };
		for (int i = 0; i < dizi.length; i++)
			sheet.setColumnWidth(i, (short) (dizi[i] * katsayi));

		try {
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
	 * @param hareketler
	 * @param toplam
	 * @return
	 * @throws Exception
	 */
	public ByteArrayOutputStream excelOzetDevam(List<HareketKGS> hareketler, boolean toplam) throws Exception {
		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, "Yemek Rapor", Boolean.TRUE);
		CellStyle style = ExcelUtil.getStyleData(wb);
		CellStyle header = ExcelUtil.getStyleHeader(wb);
		CellStyle date = ExcelUtil.getCellStyleDate(wb);
		int row = 0, col = 0;
		if (!toplam)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Yemek Zamanı");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.sirketAciklama());
		if (ogunVar)
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Öğün Tipi");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Yemek Yeri");
		ExcelUtil.getCell(sheet, row, col++, header).setCellValue("Adet");
		for (Iterator iter = hareketler.iterator(); iter.hasNext();) {
			HareketKGS yemek = (HareketKGS) iter.next();
			row++;
			col = 0;
			try {
				if (!toplam)
					ExcelUtil.getCell(sheet, row, col++, date).setCellValue(yemek.getZaman());
				String sirket = "", ogun = "";
				try {
					sirket = yemek.getPersonel().getPdksPersonel().getSirket().getAd();
				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					sirket = "" + ortakIslemler.sirketAciklama() + " tanımsız";
				}
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(sirket);
				if (ogunVar) {
					try {
						ogun = yemek.getYemekOgun().getYemekAciklama();
					} catch (Exception e) {
						logger.error("PDKS hata in : \n");
						e.printStackTrace();
						logger.error("PDKS hata out : " + e.getMessage());
						sirket = "Öğün tanımsız";
					}

					ExcelUtil.getCell(sheet, row, col++, style).setCellValue(ogun);
				}

				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(yemek.getKapiView().getAciklama());
				ExcelUtil.getCell(sheet, row, col++, style).setCellValue(yemek.getYemekYiyenSayisi());
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				logger.debug(row);

			}
			double katsayi = 3.43;
			int[] dizi = toplam ? new int[] { 2011, 2056, 2011, 1000 } : new int[] { 1575, 2011, 2056, 2011, 1000 };
			for (int i = 0; i < dizi.length; i++)
				sheet.setColumnWidth(i, (short) (dizi[i] * katsayi));
		}
		try {
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

	public String excelToplamAktar(Boolean gunVar) {
		try {
			ByteArrayOutputStream baosDosya = excelOzetDevam(gunVar ? toplamYemekList : hareketList, gunVar);
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "toplamYemekYiyenler.xlsx");
		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
		}

		return "";
	}

	public String excelGunlukAktar() {
		try {
			ByteArrayOutputStream baosDosya = excelDevam(hareketList, Boolean.FALSE);
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "gunlukYemekYiyenler.xlsx");

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
		}

		return "";
	}

	public String excelMukerrerAktar() {
		try {
			ByteArrayOutputStream baosDosya = excelDevam(ciftYemekList, Boolean.TRUE);
			if (baosDosya != null)
				PdksUtil.setExcelHttpServletResponse(baosDosya, "mukerrerYemekYiyenler.xlsx");

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());
		}

		return "";
	}

	public void fillHareketList() {
		fillYemekList();
		List<HareketKGS> kgsList = new ArrayList<HareketKGS>();
		TreeMap<String, HareketKGS> yemekOzetZamanMap = new TreeMap<String, HareketKGS>();
		TreeMap<String, HareketKGS> yemekMap = new TreeMap<String, HareketKGS>();
		HashMap parametreMap = new HashMap();
		// Calendar cal = Calendar.getInstance();
		// ortakIslemler.showSQLQuery(parametreMap);
		// parametreMap.put("basTarih", PdksUtil.getDate(basTarih));
		// parametreMap.put("bitTarih", PdksUtil.getDate(ortakIslemler.tariheGunEkleCikar(cal, bitTarih, 1)));
		// StringBuffer qsb = new StringBuffer();
		// qsb.append("select S." + HareketKGS.COLUMN_NAME_ID + " S." + HareketKGS.COLUMN_NAME_TABLE_ID + " from " + HareketKGS.TABLE_NAME + " S " + PdksEntityController.getSelectLOCK() + " ");
		// qsb.append(" where S." + HareketKGS.COLUMN_NAME_ZAMAN + " >= :basTarih and S." + HareketKGS.COLUMN_NAME_ZAMAN + " <:bitTarih ");
		// if (!yemekKapiList.isEmpty()) {
		// qsb.append(" and S." + HareketKGS.COLUMN_NAME_KAPI + " :kapiId");
		// parametreMap.put("kapiId", yemekKapiList);
		// }
		// if (session != null)
		// parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
		// try {
		// List<Object[]> list = pdksEntityController.getObjectBySQLList(qsb, parametreMap, null);
		// kgsList = new ArrayList<HareketKGS>();
		// for (Object[] objects : list) {
		// HareketKGS hareketKGS = new HareketKGS();
		// hareketKGS.setId((String) objects[0]);
		// hareketKGS.setHareketTableId(new Long(objects[1].toString()));
		// kgsList.add(hareketKGS);
		// }
		//
		// kgsList = ortakIslemler.getHareketIdBilgileri(kgsList, null, basTarih, bitTarih, session);
		// list = null;
		// } catch (Exception e) {
		// kgsList = new ArrayList<HareketKGS>();
		// logger.error("PDKS hata in : \n");
		// e.printStackTrace();
		// logger.error("PDKS hata out : " + e.getMessage());
		// }
		kgsList = ortakIslemler.getYemekHareketleri(session, basTarih, bitTarih, false);
		int yemekMukerrerAraligi = ortakIslemler.getYemekMukerrerAraligi();
		if (!kgsList.isEmpty())
			kgsList = PdksUtil.sortListByAlanAdi(kgsList, "zaman", Boolean.FALSE);
		List<HareketKGS> kgsSonucList = new ArrayList<HareketKGS>();
		List<HareketKGS> kgsCifList = new ArrayList<HareketKGS>();
		try {

			Calendar calendar = Calendar.getInstance();
			Personel tanimsizSirketPersonel = new Personel();
			Sirket tanimsizSirket = new Sirket();
			tanimsizSirket.setAd("Sirket Tanimsiz");
			tanimsizSirket.setId(-1L);
			tanimsizSirketPersonel.setSirket(tanimsizSirket);
			tanimsizSirketPersonel.setId(-1L);
			Sirket sirket = null;
			TreeMap<String, HareketKGS> yemekZamanMap = new TreeMap<String, HareketKGS>();
			YemekOgun tanimYemek = new YemekOgun();
			tanimYemek.setId(0L);
			tanimYemek.setYemekAciklama("Tanimsiz");

			boolean toplamYaz = PdksUtil.tarihKarsilastirNumeric(basTarih, bitTarih) != 0;
			parametreMap.clear();
			parametreMap.put("tarih>=", basTarih);
			parametreMap.put("tarih<=", bitTarih);
			if (session != null)
				parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);

			List<YemekKartsiz> kartsizList = pdksEntityController.getObjectByInnerObjectListInLogic(parametreMap, YemekKartsiz.class);
			// kgsList.clear();
			if (!kartsizList.isEmpty()) {
				HashMap<Long, PersonelView> sirketPersonelMap = new HashMap<Long, PersonelView>();
				for (YemekKartsiz pdksYemekKartsiz : kartsizList) {
					if (pdksYemekKartsiz.getDurum()) {
						HareketKGS hareket = pdksYemekKartsiz.getKgsHareket();
						PersonelView kartsizPersonel = new PersonelView();
						kartsizPersonel.setId(-pdksYemekKartsiz.getSirket().getId());
						Personel kartsizSirketPersonel = new Personel();
						kartsizSirketPersonel.setAd("Kartsız");
						kartsizSirketPersonel.setSoyad("Giriş : " + pdksYemekKartsiz.getAdet());
						kartsizSirketPersonel.setId(-pdksYemekKartsiz.getSirket().getId());
						kartsizSirketPersonel.setSirket(pdksYemekKartsiz.getSirket());
						kartsizPersonel.setPdksPersonel(kartsizSirketPersonel);
						sirketPersonelMap.put(pdksYemekKartsiz.getSirket().getId(), kartsizPersonel);

						hareket.setPersonel(kartsizPersonel);
						kgsList.add(hareket);
					}
				}
			}
			ogunVar = false;
			for (Iterator iterator = kgsList.iterator(); iterator.hasNext();) {
				HareketKGS kgsHareket = (HareketKGS) iterator.next();
				PersonelView personelView = kgsHareket.getPersonel();
				kgsHareket.setGecerliYemek(null);
				kgsHareket.setGecerliYemekAdet(0);
				kgsHareket.addYemekTeloreansZamani(yemekMukerrerAraligi);
				if (!ogunVar)
					ogunVar = kgsHareket.getYemekOgun() != null && kgsHareket.getYemekOgun().getId() != null && kgsHareket.getYemekOgun().getId().longValue() > 0L;
				try {
					sirket = null;
					if (personelView.getPdksPersonel() == null || personelView.getPdksPersonel().getId() == null) {
						personelView.setPdksPersonel(tanimsizSirketPersonel);
						sirket = tanimsizSirketPersonel.getSirket();
					} else
						try {
							sirket = personelView.getPdksPersonel().getSirket();
						} catch (Exception e) {
							logger.error("PDKS hata in : \n");
							e.printStackTrace();
							logger.error("PDKS hata out : " + e.getMessage());
							sirket = null;
						}
					long yemekZamaniKgs = Long.parseLong((PdksUtil.convertToDateString(kgsHareket.getZaman(), "yyyyMMddHHmm")));
					YemekOgun pdksYemekOgun = kgsHareket.getYemekOgun();
					Date yemekGun = (Date) kgsHareket.getZaman().clone();
					if (pdksYemekOgun == null) {
						for (YemekOgun pdksYemekOgunOrj : yemekList) {
							if (PdksUtil.tarihKarsilastirNumeric(pdksYemekOgunOrj.getBasTarih(), kgsHareket.getZaman()) == 1 || PdksUtil.tarihKarsilastirNumeric(kgsHareket.getZaman(), pdksYemekOgunOrj.getBitTarih()) == 1) {
								continue;

							}
							YemekOgun pdksYemek = (YemekOgun) pdksYemekOgunOrj.clone();
							calendar.setTime(kgsHareket.getZaman());
							calendar.set(Calendar.HOUR_OF_DAY, pdksYemek.getBaslangicSaat());
							calendar.set(Calendar.MINUTE, pdksYemek.getBaslangicDakika());
							long yemekZamaniBas = Long.parseLong(PdksUtil.convertToDateString(calendar.getTime(), "yyyyMMddHHmm"));
							if (pdksYemek.getBitisSaat() < pdksYemek.getBaslangicSaat()) {
								int saat = PdksUtil.getDateField(kgsHareket.getZaman(), Calendar.HOUR_OF_DAY);
								if (saat >= pdksYemek.getBaslangicSaat())
									calendar.add(Calendar.DATE, 1);
								else {
									calendar.add(Calendar.DATE, -1);
									yemekGun = calendar.getTime();
									yemekZamaniBas = Long.parseLong(PdksUtil.convertToDateString(calendar.getTime(), "yyyyMMddHHmm"));
									calendar.setTime(kgsHareket.getZaman());
								}
							}
							calendar.set(Calendar.HOUR_OF_DAY, pdksYemek.getBitisSaat());
							calendar.set(Calendar.MINUTE, pdksYemek.getBitisDakika());
							long yemekZamaniBit = Long.parseLong(PdksUtil.convertToDateString(calendar.getTime(), "yyyyMMddHHmm"));

							if ((yemekZamaniBas <= yemekZamaniKgs && yemekZamaniKgs < yemekZamaniBit)) {
								pdksYemekOgun = (YemekOgun) pdksYemekOgunOrj.clone();
								break;
							}
						}
						if (pdksYemekOgun == null)
							pdksYemekOgun = tanimYemek;
					}

					kgsHareket.setYemekOgun(pdksYemekOgun);
					String tarih = kgsHareket.getKapiView().getId() + "_" + PdksUtil.convertToDateString(yemekGun, "yyyyMMdd") + sirket.getId() + "_" + pdksYemekOgun.getId();
					if (pdksYemekOgun.getId() != null && pdksYemekOgun.getId() > 0) {
						String key = kgsHareket.getKapiView().getId() + "_" + PdksUtil.convertToDateString(yemekGun, "yyyyMMdd") + "_" + pdksYemekOgun.getId() + "_" + personelView.getId();
						if (!yemekZamanMap.containsKey(key))
							yemekZamanMap.put(key, kgsHareket);
						else {
							HareketKGS kgsHareket2 = yemekZamanMap.get(key);
							kgsHareket.setGecerliYemek(kgsHareket2.getYemekTeloreansZamani().before(kgsHareket.getZaman()));
							if (kgsHareket.getGecerliYemek()) {
								kgsHareket2.setGecerliYemekAdet(kgsHareket2.getGecerliYemekAdet() + 1);
								String key1 = kgsHareket.getKapiView().getId() + "_" + sirket.getId() + "_" + pdksYemekOgun.getId();
								HareketKGS kgs = yemekOzetZamanMap.get(key1);
								if (kgs != null)
									kgs.setGecerliYemekAdet(kgs.getGecerliYemekAdet() + 1);
							}

							kgsCifList.add(kgsHareket);
							continue;
						}
					}
					int adet = kgsHareket.getYemekYiyenSayisi() > 0 ? kgsHareket.getYemekYiyenSayisi() : 1;
					if (yemekMap.containsKey(tarih)) {
						HareketKGS kgs = yemekMap.get(tarih);
						kgs.setYemekYiyenSayisi(kgs.getYemekYiyenSayisi() + adet);
						if (personelView.getId() != null)
							kgs.getYemekList().add(kgsHareket);
					} else {
						HareketKGS kgs = (HareketKGS) kgsHareket.clone();
						if (kgs.getYemekOgun().getId().longValue() == 0 || sirket.getId().longValue() == 0)
							kgs.setStyle(VardiyaGun.STYLE_CLASS_HATA);
						kgs.setYemekList(new ArrayList<HareketKGS>());
						if (personelView.getId() != null)
							kgs.getYemekList().add(kgsHareket);
						kgs.setYemekYiyenSayisi(adet);
						yemekMap.put(tarih, kgs);
					}
					if (toplamYaz && sirket != null && sirket.getId() != null) {
						String key = kgsHareket.getKapiView().getId() + "_" + sirket.getId() + "_" + pdksYemekOgun.getId();
						if (yemekOzetZamanMap.containsKey(key)) {
							HareketKGS kgs = yemekOzetZamanMap.get(key);
							kgs.setYemekYiyenSayisi(kgs.getYemekYiyenSayisi() + adet);
							if (personelView.getId() != null)
								kgs.getYemekList().add(kgsHareket);
						} else {
							HareketKGS kgs = (HareketKGS) kgsHareket.clone();
							kgs.setZaman(null);
							kgs.setStyle(VardiyaGun.STYLE_CLASS_EVEN);
							kgs.setYemekList(new ArrayList<HareketKGS>());
							if (personelView.getId() != null)
								kgs.getYemekList().add(kgsHareket);
							kgs.setYemekYiyenSayisi(adet);
							yemekOzetZamanMap.put(key, kgs);
						}
					}

				} catch (Exception e) {
					logger.error("PDKS hata in : \n");
					e.printStackTrace();
					logger.error("PDKS hata out : " + e.getMessage());
					logger.error(kgsHareket.getId() + " " + e.getMessage());
				}

			}
			if (!yemekMap.isEmpty()) {
				kgsSonucList.addAll(new ArrayList<HareketKGS>(yemekMap.values()));
				kgsSonucList = PdksUtil.sortListByAlanAdi(kgsSonucList, "zaman", Boolean.TRUE);

			}

		} catch (Exception e) {
			logger.error("PDKS hata in : \n");
			e.printStackTrace();
			logger.error("PDKS hata out : " + e.getMessage());

		}

		List<HareketKGS> toplamYemekListe = new ArrayList<HareketKGS>(yemekOzetZamanMap.values());
		setToplamYemekList(toplamYemekListe);
		setHareketList(kgsSonucList);
		setCiftYemekList(kgsCifList);
		if (!kgsCifList.isEmpty())
			PdksUtil.addMessageWarn("Mükerrer yemek yiyen personeller var!");

	}

	public List<HareketKGS> getHareketList() {
		return hareketList;
	}

	public void setHareketList(List<HareketKGS> hareketList) {
		this.hareketList = hareketList;
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

	public List<YemekOgun> getYemekList() {
		return yemekList;
	}

	public void setYemekList(List<YemekOgun> yemekList) {
		this.yemekList = yemekList;
	}

	public List<Long> getYemekKapiList() {
		return yemekKapiList;
	}

	public void setYemekKapiList(List<Long> yemekKapiList) {
		this.yemekKapiList = yemekKapiList;
	}

	public List<HareketKGS> getCiftYemekList() {
		return ciftYemekList;
	}

	public void setCiftYemekList(List<HareketKGS> ciftYemekList) {
		this.ciftYemekList = ciftYemekList;
	}

	public List<HareketKGS> getToplamYemekList() {
		return toplamYemekList;
	}

	public void setToplamYemekList(List<HareketKGS> toplamYemekList) {
		this.toplamYemekList = toplamYemekList;
	}

	public HareketKGS getHareketKGS() {
		return hareketKGS;
	}

	public void setHareketKGS(HareketKGS hareketKGS) {
		this.hareketKGS = hareketKGS;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public boolean isOgunVar() {
		return ogunVar;
	}

	public void setOgunVar(boolean ogunVar) {
		this.ogunVar = ogunVar;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		YemekYiyenSayisiHome.sayfaURL = sayfaURL;
	}
}
