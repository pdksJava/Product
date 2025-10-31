package org.pdks.dinamikRapor.action;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Clob;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hibernate.Session;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.framework.EntityHome;
import org.pdks.dinamikRapor.entity.PdksDinamikRapor;
import org.pdks.dinamikRapor.entity.PdksDinamikRaporAlan;
import org.pdks.dinamikRapor.entity.PdksDinamikRaporParametre;
import org.pdks.dinamikRapor.enums.ENumBaslik;
import org.pdks.entity.AramaSecenekleri;
import org.pdks.entity.AylikPuantaj;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.Liste;
import org.pdks.entity.Sirket;
import org.pdks.security.entity.MenuItemConstant;
import org.pdks.security.entity.User;
import org.pdks.security.entity.UserMenuItemTime;
import org.pdks.session.ExcelUtil;
import org.pdks.session.FazlaMesaiOrtakIslemler;
import org.pdks.session.OrtakIslemler;
import org.pdks.session.PdksEntityController;
import org.pdks.session.PdksUtil;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

@Name("dinamikRaporHome")
public class DinamikRaporHome extends EntityHome<PdksDinamikRapor> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7573955904085170923L;
	static Logger logger = Logger.getLogger(DinamikRaporHome.class);
	@RequestParameter
	Long pdksDepartmanId;
	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	PdksEntityController pdksEntityController;
	@In(required = true, create = true)
	OrtakIslemler ortakIslemler;
	@In(required = false, create = true)
	FazlaMesaiOrtakIslemler fazlaMesaiOrtakIslemler;
	@In(required = false, create = true)
	User authenticatedUser;

	public static String sayfaURL = "dinamikRapor";
	private PdksDinamikRapor seciliPdksDinamikRapor;
	private List<PdksDinamikRapor> dinamikRaporList;
	private List<PdksDinamikRaporAlan> dinamikRaporAlanList, dinamikRaporBagliAlanList;
	private List<PdksDinamikRaporParametre> dinamikRaporParametreList;
	private List<SelectItem> tesisIdList;
	private List<Liste> dinamikRaporDataList;
	private PdksDinamikRaporParametre sirketParametre, tesisParametre, basTarihParametre, bitTarihParametre, bolumParametre, yilParametre, ayParametre;
	private PdksDinamikRaporAlan tesisAlan;
	private DenklestirmeAy dm;
	private boolean goster = true;
	private int maxYil;
	private Sirket sirket = null;
	private Session session;

	@Override
	public Object getId() {
		if (pdksDepartmanId == null) {
			return super.getId();
		} else {
			return pdksDepartmanId;
		}
	}

	@Override
	@Begin(join = true)
	public void create() {
		super.create();
	}

	@Begin(join = true, flushMode = FlushModeType.MANUAL)
	public String sayfaGirisAction() {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		ortakIslemler.setUserMenuItemTime(session, sayfaURL);
		tesisAlan = null;
		sirket = null;
		String sayfa = "";
		if (dinamikRaporDataList == null)
			dinamikRaporDataList = new ArrayList<Liste>();
		else
			dinamikRaporDataList.clear();
		try {
			fillPdksDinamikRaporList();
			seciliPdksDinamikRapor = null;
			if (dinamikRaporList.isEmpty()) {
				PdksUtil.addMessageAvailableWarn("Rapor alınacak tanımlanmış veri yoktur!");
				sayfa = MenuItemConstant.home;
			} else if (dinamikRaporList.size() == 1) {
				PdksDinamikRapor dinamikRapor = dinamikRaporList.get(0);
				dinamikRaporGuncelle(dinamikRapor);
			}
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		return sayfa;
	}

	@Transactional
	private void saveLastParameter() {
		LinkedHashMap<String, Object> dataMap = null;
		Long menuId = authenticatedUser.getMenuItemTime() != null ? authenticatedUser.getMenuItemTime().getId() : null;
		UserMenuItemTime menuItemTime = menuId != null ? (UserMenuItemTime) pdksEntityController.getSQLParamByFieldObject(UserMenuItemTime.TABLE_NAME, UserMenuItemTime.COLUMN_NAME_ID, menuId, UserMenuItemTime.class, session) : null;
		if (menuItemTime != null) {
			dataMap = new LinkedHashMap<String, Object>();
			if (menuItemTime.getParametreJSON() != null) {
				Gson gson = new Gson();
				LinkedHashMap<String, Object> map = gson.fromJson(menuItemTime.getParametreJSON(), LinkedHashMap.class);
				if (map != null && map.containsKey("sayfaURL"))
					dataMap = map;
			}
		}
		if (dataMap == null)
			dataMap = new LinkedHashMap<String, Object>();
		LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
		lastMap.put("raporAdi", seciliPdksDinamikRapor.getAciklama());
		for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
			PdksDinamikRaporParametre rp = (PdksDinamikRaporParametre) iterator.next();
			String adi = rp.getAciklama();
			if (rp.isObjectValue())
				lastMap.put(adi, rp.getValue());
			else if (rp.isKarakter()) {
				if (PdksUtil.hasStringValue(rp.getKarakterDeger()))
					lastMap.put(adi, rp.getKarakterDeger());
			} else if (rp.isSayisal())
				lastMap.put(adi, rp.getSayisalDeger());
			else if (rp.isTarih())
				lastMap.put(adi, PdksUtil.convertToDateString(rp.getTarihDeger(), "yyyyMMdd"));
			else if (rp.isMantiksal()) {
				if (rp.getMantiksalDurum() != null)
					lastMap.put(adi, String.valueOf(rp.getMantiksalDurum()));
			}

		}
		lastMap.put("goster", String.valueOf(goster));
		dataMap.put("data_" + seciliPdksDinamikRapor.getId(), lastMap);
		dataMap.put("sayfaURL", sayfaURL);
		try {
			ortakIslemler.saveLastParameter(dataMap, session);
		} catch (Exception e) {
		}
	}

	/**
	 * @param dinamikRapor
	 * @param tip
	 * @return
	 */
	public String dinamikRaporGuncelle(PdksDinamikRapor dinamikRapor) {
		seciliPdksDinamikRapor = dinamikRapor;
		try {
			filllDinamikRaporParametreList();
		} catch (Exception e) {
			e.printStackTrace();
		}
		fillDinamikRaporAlanList();

		dinamikRaporDataList.clear();
		return "";

	}

	/**
	 * @return
	 */
	public String fillDinamikRaporList() {
		StringBuffer sb = new StringBuffer();
		List<Object[]> list = null;
		if (dinamikRaporDataList == null)
			dinamikRaporDataList = new ArrayList<Liste>();
		else
			dinamikRaporDataList.clear();
		if (seciliPdksDinamikRapor.isStoreProcedure()) {
			sb.append(seciliPdksDinamikRapor.getDbTanim());
			LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
			for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
				PdksDinamikRaporParametre rp = (PdksDinamikRaporParametre) iterator.next();
				String adi = "p" + rp.getSira();
				if (rp.isObjectValue())
					veriMap.put(adi, rp.getValue());
				else if (rp.isKarakter())
					veriMap.put(adi, rp.getKarakterDeger());
				else if (rp.isSayisal())
					veriMap.put(adi, rp.getSayisalDeger());
				else if (rp.isTarih())
					veriMap.put(adi, rp.getTarihDeger());
				else if (rp.isMantiksal())
					veriMap.put(adi, rp.getMantiksalDurum() != null && rp.getMantiksalDurum() ? 1 : 0);
			}
			if (session != null)
				veriMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			try {
				list = pdksEntityController.execSPList(veriMap, sb, null);
			} catch (Exception e) {

			}
		} else {
			HashMap fields = new HashMap();
			sb.append("select ");
			for (Iterator iterator = dinamikRaporAlanList.iterator(); iterator.hasNext();) {
				PdksDinamikRaporAlan ra = (PdksDinamikRaporAlan) iterator.next();
				sb.append(ra.getDbTanim() + (iterator.hasNext() ? ", " : ""));
			}
			String str = " where ";
			if (seciliPdksDinamikRapor.isFunction()) {
				sb.append(" from " + seciliPdksDinamikRapor.getDbTanim() + "(");
				String strFunction = "";
				for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
					PdksDinamikRaporParametre rp = (PdksDinamikRaporParametre) iterator.next();
					if (rp.getParametreDurum()) {
						String adi = "p" + rp.getSira();
						if (strFunction.length() > 0)
							strFunction += ", ";
						strFunction += " :" + adi;
						if (rp.getSecimList() != null)
							fields.put(adi, rp.getValue());
						else if (rp.isKarakter())
							fields.put(adi, rp.getKarakterDeger());
						else if (rp.isSayisal())
							fields.put(adi, rp.getSayisalDeger());
						else if (rp.isTarih())
							fields.put(adi, rp.getTarihDeger());

					}
				}
				sb.append(strFunction + " )");
				for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
					PdksDinamikRaporParametre rp = (PdksDinamikRaporParametre) iterator.next();
					if (rp.getParametreDurum().booleanValue() == false) {
						String adi = "p" + rp.getSira();
						Object veri = null;
						if (rp.getSecimList() != null)
							veri = rp.getValue();
						else if (rp.isKarakter()) {
							veri = rp.getKarakterDeger();
							if (PdksUtil.hasStringValue(rp.getKarakterDeger()) == false && rp.getZorunlu().booleanValue() == false)
								veri = null;
						} else if (rp.isSayisal())
							veri = rp.getSayisalDeger();
						else if (rp.isTarih())
							veri = rp.getTarihDeger();
						else if (rp.isMantiksal()) {
							if (rp.getMantiksalDurum() != null)
								veri = rp.getMantiksalDurum() ? 1 : 0;
						}
						if (veri != null || rp.getZorunlu()) {
							String esitlik = PdksUtil.hasStringValue(rp.getEsitlik()) ? rp.getEsitlik().trim() : "=";
							if (esitlik.equalsIgnoreCase("like"))
								veri = "%" + veri + "%";
							sb.append(str + rp.getDbTanim() + " " + esitlik + " :" + adi);
							fields.put(adi, veri);
							str = " and ";
						}
					}
				}
			} else if (seciliPdksDinamikRapor.isView()) {
				sb.append(" from " + seciliPdksDinamikRapor.getDbTanim() + " " + PdksEntityController.getSelectLOCK());
				for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
					PdksDinamikRaporParametre rp = (PdksDinamikRaporParametre) iterator.next();
					String adi = "p" + rp.getSira();
					Object veri = null;
					if (rp.getSecimList() != null)
						veri = rp.getValue();
					else if (rp.isKarakter()) {
						veri = rp.getKarakterDeger();
						if (PdksUtil.hasStringValue(rp.getKarakterDeger()) == false && rp.getZorunlu().booleanValue() == false)
							veri = null;
					} else if (rp.isSayisal())
						veri = rp.getSayisalDeger();
					else if (rp.isTarih())
						veri = rp.getTarihDeger();
					if (veri != null || rp.getZorunlu()) {
						String esitlik = PdksUtil.hasStringValue(rp.getEsitlik()) ? rp.getEsitlik().trim() : "=";
						if (esitlik.equalsIgnoreCase("like"))
							veri = "%" + veri + "%";
						sb.append(str + rp.getDbTanim() + " " + esitlik + " :" + adi);
						fields.put(adi, veri);
						str = " and ";
					}
				}
			}
			if (PdksUtil.hasStringValue(seciliPdksDinamikRapor.getWhereSQL()))
				sb.append(str + " " + seciliPdksDinamikRapor.getWhereSQL());
			if (PdksUtil.hasStringValue(seciliPdksDinamikRapor.getOrderSQL()))
				sb.append(" order by " + seciliPdksDinamikRapor.getOrderSQL());
			if (session != null)
				fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			try {
				list = pdksEntityController.getObjectBySQLList(sb, fields, null);
			} catch (Exception e) {
				list = null;
				logger.error(e);
				e.printStackTrace();
			}

		}
		saveLastParameter();
		if (list != null && list.isEmpty() == false) {
			long sira = 0;
			for (Object[] objects : list)
				dinamikRaporDataList.add(new Liste(++sira, objects));
			fillDinamikRaporAlanList();

			if (goster == false)
				PdksUtil.addMessageAvailableInfo(list.size() + " adet bilgi okundu.");
		}
		list = null;
		return "";
	}

	/**
	 * @return
	 */
	public String excelDinamikRaporList() {
		StringBuffer baslikSb = new StringBuffer(), aciklamaSb = new StringBuffer();
		try {
			for (PdksDinamikRaporParametre pdrp : dinamikRaporParametreList) {
				String aciklama = null;
				Date tarih = null;
				if (pdrp.getSecimList() != null) {
					if (pdrp.getValue() != null) {
						aciklama = ortakIslemler.getSelectItemText(Long.parseLong((String) pdrp.getValue()), pdrp.getSecimList());
					}
				} else if (pdrp.isKarakter()) {
					if (PdksUtil.hasStringValue(pdrp.getKarakterDeger()))
						aciklama = pdrp.getKarakterDeger().trim();
				} else if (pdrp.isTarih())
					tarih = pdrp.getTarihDeger();

				if (pdrp.getBaslik() != null && (tarih != null || (aciklama != null && aciklama.trim().length() > 0))) {
					String baslik = (String) PdksUtil.getMethodObject(ortakIslemler, pdrp.getBaslik().value(), null);
					if (baslikSb.length() > 0)
						baslikSb.append("|");
					if (tarih != null) {
						baslikSb.append((baslik != null ? baslik + " : " : "") + authenticatedUser.dateFormatla(tarih) + " ");
						aciklama = PdksUtil.convertToDateString(tarih, "yyyyMMdd");
					} else
						baslikSb.append((baslik != null ? baslik + " : " : "") + aciklama.trim());

					if (pdrp.isKarakter() == false)
						aciklamaSb.append("_" + aciklama.trim());
				}
			}
			ByteArrayOutputStream baosDosya = excelDinamikRaporListDevam(baslikSb.toString());
			if (baosDosya != null) {
				String dosyaAdi = seciliPdksDinamikRapor.getAciklama() + aciklamaSb.toString() + ".xlsx";
				PdksUtil.setExcelHttpServletResponse(baosDosya, dosyaAdi);
			}
		} catch (Exception e) {
			logger.error("Pdks hata in : \n");
			e.printStackTrace();
			logger.error("Pdks hata out : " + e.getMessage());
		}
		baslikSb = null;
		aciklamaSb = null;
		return "";
	}

	/**
	 * @return
	 */
	private ByteArrayOutputStream excelDinamikRaporListDevam(String baslik) {
		ByteArrayOutputStream baos = null;
		Workbook wb = new XSSFWorkbook();
		Sheet sheet = ExcelUtil.createSheet(wb, "Rapor", Boolean.TRUE);
		Drawing drawing = sheet.createDrawingPatriarch();
		CreationHelper helper = wb.getCreationHelper();
		ClientAnchor anchor = helper.createClientAnchor();
		XSSFCellStyle header = (XSSFCellStyle) ExcelUtil.getStyleHeader(9, wb);
		CellStyle styleOdd = ExcelUtil.getStyleOdd(null, wb);
		CellStyle styleOddCenter = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleOddRight = ExcelUtil.getStyleOdd(ExcelUtil.ALIGN_RIGHT, wb);
		CellStyle styleOddNumber = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_NUMBER, wb);
		CellStyle styleOddDate = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATE, wb);
		CellStyle styleOddDateTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleOddTime = ExcelUtil.getStyleOdd(ExcelUtil.FORMAT_TIME, wb);
		CellStyle styleEven = ExcelUtil.getStyleEven(null, wb);
		CellStyle styleEvenCenter = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_CENTER, wb);
		CellStyle styleEvenRight = ExcelUtil.getStyleEven(ExcelUtil.ALIGN_RIGHT, wb);
		CellStyle styleEvenNumber = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_NUMBER, wb);
		CellStyle styleEvenDate = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATE, wb);
		CellStyle styleEvenDateTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_DATETIME, wb);
		CellStyle styleEvenTime = ExcelUtil.getStyleEven(ExcelUtil.FORMAT_TIME, wb);
		int col = 0, row = 0;
		List<PdksDinamikRaporAlan> list = new ArrayList<PdksDinamikRaporAlan>(dinamikRaporAlanList);
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			PdksDinamikRaporAlan ra = (PdksDinamikRaporAlan) iterator.next();
			if (ra.getDurum() && (ra.getGoster() || authenticatedUser.isAdmin()))
				continue;
			else
				iterator.remove();
		}

		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			PdksDinamikRaporAlan ra = (PdksDinamikRaporAlan) iterator.next();
			ExcelUtil.getCell(sheet, row, col++, header).setCellValue(ortakIslemler.getDinamikRaporAlanAciklama(ra.getAciklama()));

		}

		boolean renk = true;
		for (Liste liste : dinamikRaporDataList) {
			Object[] veri = (Object[]) liste.getValue();
			col = 0;
			++row;
			CellStyle style = null;
			CellStyle styleCenter = null;
			CellStyle styleRight = null;
			CellStyle styleGenel = null;
			CellStyle styleNumber = null;
			CellStyle styleDate = null;
			CellStyle styleDateTime = null;
			CellStyle styleTime = null;
			if (renk) {
				styleGenel = styleOdd;
				styleCenter = styleOddCenter;
				styleRight = styleOddRight;
				styleNumber = styleOddNumber;
				styleDate = styleOddDate;
				styleDateTime = styleOddDateTime;
				styleTime = styleOddTime;
			} else {
				styleGenel = styleEven;
				styleCenter = styleEvenCenter;
				styleRight = styleEvenRight;
				styleNumber = styleEvenNumber;
				styleDate = styleEvenDate;
				styleDateTime = styleEvenDateTime;
				styleTime = styleEvenTime;
			}

			for (PdksDinamikRaporAlan ra : list) {
				Object data = getDinamikRaporAlanVeri(veri, ra.getSira());
				if (data != null) {
					boolean clob = data instanceof Clob;
					if (clob == false) {
						if (ra.isKarakter()) {
							String str = (String) data;
							style = styleGenel;
							if (ra.isHizalaOrtala())
								style = styleCenter;
							else if (ra.isHizalaSaga())
								style = styleRight;
							ExcelUtil.getCell(sheet, row, col, style).setCellValue(str);
						} else if (ra.isSayisal() == false) {
							Date tarih = (Date) data;
							style = styleDate;
							if (ra.isTarihSaat())
								style = styleDateTime;
							else if (ra.isSaat())
								style = styleTime;
							ExcelUtil.getCell(sheet, row, col, style).setCellValue(tarih);
						} else {
							Double db = null;
							style = styleNumber;
							if (data instanceof BigDecimal)
								db = ((BigDecimal) data).doubleValue();
							else if (data instanceof BigInteger)
								db = ((BigInteger) data).doubleValue();
							else if (data instanceof Double)
								db = ((Double) data).doubleValue();
							else if (data instanceof Integer)
								db = ((Integer) data).doubleValue();
							else
								db = new Double(data.toString());
							ExcelUtil.getCell(sheet, row, col, style).setCellValue(db);
						}
					} else {
						Cell clobCell = ExcelUtil.getCell(sheet, row, col, styleCenter);
						String str = PdksUtil.StringToByClob((Clob) data);
						clobCell.setCellValue("");
						ExcelUtil.setCellComment(clobCell, anchor, helper, drawing, str);
					}

				} else {
					ExcelUtil.getCell(sheet, row, col, styleGenel).setCellValue("");
				}
				++col;
			}
			renk = !renk;
		}
		if (PdksUtil.hasStringValue(baslik)) {
			List<String> strList = PdksUtil.getListByString(baslik, null);
			for (String string : strList) {
				ExcelUtil.getCell(sheet, row, 0, header).setCellValue(string.trim());
				for (int i = 0; i < list.size() - 1; i++)
					ExcelUtil.getCell(sheet, row, i + 1, header).setCellValue("");

				try {
					sheet.addMergedRegion(ExcelUtil.getRegion((int) row, (int) 0, (int) row, (int) list.size() - 1));
				} catch (Exception e) {
					e.printStackTrace();
				}
				++row;
			}

		}
		list = null;
		try {
			for (int i = 0; i <= col; i++)
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
	 * @param alan
	 * @return
	 */
	public Boolean getFilterAlan(PdksDinamikRaporAlan alan) {
		boolean filterDurum = dinamikRaporDataList.isEmpty() == false && alan != null && alan.getFilter() && alan.getDurum();
		return filterDurum;
	}

	/**
	 * @param veri
	 * @param alan
	 * @return
	 */
	public Object getDinamikRaporAlan(Object[] veri, PdksDinamikRaporAlan alan) {
		Object value = getDinamikRaporAlanVeri(veri, alan.getSira());
		if (value != null) {
			try {
				if (alan.isSayisal())
					value = authenticatedUser.sayiFormatliGoster(value);
				else if (alan.isTarih())
					value = authenticatedUser.dateFormatla((Date) value);
				else if (alan.isTarihSaat())
					value = authenticatedUser.dateTimeFormatla((Date) value);
				else if (alan.isSaat())
					value = authenticatedUser.timeFormatla((Date) value);
				else {
					if (value instanceof Clob) {
						Clob c = (Clob) value;
						value = PdksUtil.StringToByClob(c);

					}
				}
			} catch (Exception e) {
			}
		}
		return value;
	}

	/**
	 * @param veri
	 * @param index
	 * @return
	 */
	public boolean isClob(Object[] veri, Integer index) {
		boolean durum = false;
		Object value = getDinamikRaporAlanVeri(veri, index);
		if (value != null) {
			if (value instanceof Clob) {
				durum = true;
			}
		}
		return durum;
	}

	/**
	 * @param veri
	 * @param index
	 * @return
	 */
	private Object getDinamikRaporAlanVeri(Object[] veri, Integer index) {
		Object object = null;
		try {
			if (veri != null && index != null && veri.length >= index)
				object = veri[index];
		} catch (Exception e) {
			logger.error(index);
		}
		return object;
	}

	/**
	 * 
	 */
	private void fillPdksDinamikRaporList() {
		HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
		String paramStr = (String) req.getParameter("id");
		Long id = null;
		if (paramStr != null) {
			String str = PdksUtil.getDecodeStringByBase64(paramStr);
			HashMap<String, String> map = new HashMap();
			List<String> list = PdksUtil.getListStringTokenizer(str, "&");
			for (String string : list) {
				if (string.indexOf("=") > 0) {
					String[] strings = string.split("=");
					if (strings.length == 2)
						map.put(strings[0], strings[1]);
				}
			}
			if (map.containsKey("id"))
				try {
					id = Long.parseLong(map.get("id"));
				} catch (Exception e) {
				}
			map = null;
			list = null;
		}
		session.clear();
		if (id != null)
			dinamikRaporList = pdksEntityController.getSQLParamByFieldList(PdksDinamikRapor.TABLE_NAME, PdksDinamikRapor.COLUMN_NAME_ID, id, PdksDinamikRapor.class, session);
		else
			dinamikRaporList = pdksEntityController.getSQLParamByFieldList(PdksDinamikRapor.TABLE_NAME, PdksDinamikRapor.COLUMN_NAME_DURUM, Boolean.TRUE, PdksDinamikRapor.class, session);
		if (authenticatedUser.isAdmin() == false && id == null)
			id = -1L;
		if (authenticatedUser.isAdmin() == false) {
			if (dinamikRaporList.size() > 1)
				dinamikRaporList.clear();
			for (Iterator iterator = dinamikRaporList.iterator(); iterator.hasNext();) {
				PdksDinamikRapor pr = (PdksDinamikRapor) iterator.next();
				if (pr.getGoruntulemeDurum().booleanValue() == false || ortakIslemler.isRaporYetkili(pr) == false)
					iterator.remove();
			}
		}
	}

	/**
	 * 
	 */
	private void fillDinamikRaporAlanList() {
		tesisAlan = null;
		if (dinamikRaporAlanList == null)
			dinamikRaporAlanList = new ArrayList<PdksDinamikRaporAlan>();
		dinamikRaporAlanList.clear();
		List<PdksDinamikRaporAlan> list = pdksEntityController.getSQLParamByAktifFieldList(PdksDinamikRaporAlan.TABLE_NAME, PdksDinamikRaporAlan.COLUMN_NAME_DINAMIK_RAPOR, seciliPdksDinamikRapor.getId(), PdksDinamikRaporAlan.class, session);
		for (PdksDinamikRaporAlan pdra : list) {
			PdksDinamikRaporAlan dinamikRaporAlan = (PdksDinamikRaporAlan) pdra.clone();
			dinamikRaporAlan.setDurum(Boolean.TRUE);
			ENumBaslik baslik = ENumBaslik.fromValue(dinamikRaporAlan.getAciklama());
			if (baslik != null && baslik.equals(ENumBaslik.TESIS))
				tesisAlan = dinamikRaporAlan;
			dinamikRaporAlanList.add(dinamikRaporAlan);
		}
		list = null;
		if (dinamikRaporParametreList != null && dinamikRaporAlanList.isEmpty() == false) {
			basliklariGuncelle();
			if (dinamikRaporAlanList.size() > 1)
				dinamikRaporAlanList = PdksUtil.sortListByAlanAdi(dinamikRaporAlanList, "sira", Boolean.FALSE);

		}

	}

	private void basliklariGuncelle() {
		for (PdksDinamikRaporAlan pdra : dinamikRaporAlanList) {
			pdra.setDurum(Boolean.TRUE);
			String alanAdi = pdra.getBaslik() == null ? pdra.getAciklama() : "";
			if (pdra.getBaslik() != null) {
				for (PdksDinamikRaporParametre pdrp : dinamikRaporParametreList) {
					if (pdrp.getBagliDinamikAlan() != null && alanAdi.equals(pdrp.getBagliDinamikAlan().getAciklama())) {
						if (PdksUtil.hasStringValue(pdrp.getKarakterDeger()) || pdrp.getMantiksalDurum() != null)
							pdra.setDurum(Boolean.FALSE);
						break;
					}
					if (pdrp.getBaslik() != null && pdrp.getBaslik().equals(pdra.getBaslik())) {
						if (pdrp.getSecimList() != null)
							pdra.setDurum(pdrp.getValue() == null);
						else if (pdrp.isKarakter())
							pdra.setDurum(PdksUtil.hasStringValue(pdrp.getKarakterDeger()) == false);

						break;
					}
				}
			} else {
				for (PdksDinamikRaporParametre pdrp : dinamikRaporParametreList) {
					if (pdrp.getBagliDinamikAlan() != null && alanAdi.equals(pdrp.getBagliDinamikAlan().getAciklama())) {
						if (PdksUtil.hasStringValue(pdrp.getKarakterDeger()) || pdrp.getMantiksalDurum() != null)
							pdra.setDurum(Boolean.FALSE);
						break;
					}
					if (pdrp.getAciklama().equals(pdra.getAciklama())) {
						if (pdrp.isMantiksal())
							pdra.setDurum(pdrp.getMantiksalDurum() == null);
						else if (pdrp.isKarakter())
							pdra.setDurum(PdksUtil.hasStringValue(pdrp.getKarakterDeger()) == false);

						break;
					}
				}

			}
		}
		if (tesisAlan != null && tesisAlan.getDurum())
			tesisAlan.setDurum(sirket == null || sirket.isTesisDurumu());
	}

	/**
	 * @param parametre
	 * @return
	 */
	public String yilAyDoldur(PdksDinamikRaporParametre parametre) {
		if (ayParametre != null && yilParametre != null && sirketParametre != null && parametre != null) {
			if (parametre.getId().equals(ayParametre.getId()) || parametre.getId().equals(ayParametre.getId())) {
				int yil = Integer.parseInt("" + yilParametre.getValue()), ay = Integer.parseInt("" + ayParametre.getValue());
				dm = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			}
		}
		boolean baslikGuncelle = true;
		if (bolumParametre == null || bolumParametre.getId().equals(parametre.getId()) == false) {
			if (sirketParametre != null || tesisParametre != null) {
				if (sirketParametre.getId().equals(parametre.getId()) || tesisParametre.getId().equals(parametre.getId()))
					tesisDoldur(sirketParametre);
				else
					tesisDoldur(parametre);
				baslikGuncelle = false;
			}
		}

		dinamikRaporDataList.clear();
		if (baslikGuncelle)
			basliklariGuncelle();
		return "";
	}

	/**
	 * @param parametre
	 * @return
	 */
	public String tesisDoldur(PdksDinamikRaporParametre parametre) {
		dinamikRaporDataList.clear();

		if ((sirketParametre != null || tesisParametre != null) && parametre != null) {
			Date basTarih = basTarihParametre != null ? basTarihParametre.getTarihDeger() : PdksUtil.convertToJavaDate(PdksUtil.getSistemBaslangicYili() + "0101", "yyyyMMdd");
			Date bitTarih = bitTarihParametre != null ? bitTarihParametre.getTarihDeger() : new Date();
			if (yilParametre != null && ayParametre != null && dm == null) {
				basTarih = PdksUtil.convertToJavaDate(String.valueOf(new BigDecimal("" + yilParametre.getValue()).intValue() * 100 + new BigDecimal("" + ayParametre.getValue()).intValue()) + "01", "yyyyMMdd");
				bitTarih = PdksUtil.tariheGunEkleCikar(PdksUtil.tariheAyEkleCikar(basTarih, 1), -1);
			}
			Long oncekiTesisId = tesisParametre != null && tesisParametre.getValue() != null ? new BigDecimal("" + tesisParametre.getValue()).longValue() : null;
			if (parametre.isSirketBilgisi()) {
				sirket = null;
				Long sirketId = parametre.getValue() != null ? new BigDecimal("" + parametre.getValue()).longValue() : null;
				if (tesisParametre != null) {
					if (tesisIdList == null)
						tesisIdList = new ArrayList<SelectItem>();
					else
						tesisIdList.clear();
					if (bolumParametre != null)
						bolumParametre.setValue(null);
					tesisParametre.setValue(null);
					List<SelectItem> list = null;

					if (sirketId != null) {
						sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
						if (sirket.isTesisDurumu()) {

							if (dm != null) {
								List<SelectItem> tesisList = fazlaMesaiOrtakIslemler.getFazlaMesaiTesisList(sirket, dm != null ? new AylikPuantaj(dm) : null, false, session);
								if (tesisList != null && !tesisList.isEmpty()) {
									list = tesisList;
								}
							}
							if (list == null) {
								AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
								aramaSecenekleri.setSirketId(sirketId);
								list = ortakIslemler.setAramaSecenekTesisData(aramaSecenekleri, basTarih, bitTarih, false, session);
							}
							if (list != null) {
								if (list.size() == 1)
									tesisParametre.setValue("" + list.get(0).getValue());
								else if (oncekiTesisId != null) {
									for (SelectItem selectItem : list) {
										if (oncekiTesisId.equals(selectItem.getValue()))
											tesisParametre.setValue("" + oncekiTesisId);
									}
								}
								tesisParametre.setSecimList(list);
							}
						} else if (tesisParametre != null) {
							tesisParametre.setValue(null);
							tesisParametre.setSecimList(new ArrayList<SelectItem>());
						}
						if (bolumParametre != null && (sirket.getTesisDurum().booleanValue() == false || tesisParametre.getValue() != null))
							bolumDoldur(basTarih, bitTarih, sirket);

					}
				}
			} else if (parametre.isTesisBilgisi() == false && sirketParametre != null) {
				List<SelectItem> secimList = null;
				if (dm != null) {
					List<SelectItem> sirketler = fazlaMesaiOrtakIslemler.getFazlaMesaiSirketList(null, dm != null ? new AylikPuantaj(dm) : null, false, session);
					if (sirketler != null && sirketler.isEmpty() == false)
						secimList = sirketler;
				}
				if (secimList == null) {
					AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
					secimList = ortakIslemler.setAramaSecenekSirketVeTesisData(aramaSecenekleri, basTarih, bitTarih, false, session);
				}
				if (secimList != null) {
					Long oncekiSirketId = sirketParametre.getValue() != null ? new BigDecimal("" + sirketParametre.getValue()).longValue() : null;
					sirketParametre.setSecimList(secimList);
					sirketParametre.setValue(null);
					if (secimList.size() == 1)
						sirketParametre.setValue("" + secimList.get(0).getValue());
					else if (oncekiSirketId != null) {
						for (SelectItem selectItem : secimList) {
							if (oncekiSirketId.equals(selectItem.getValue()))
								sirketParametre.setValue("" + oncekiSirketId);
						}
					}
				}
				if (tesisParametre != null)
					tesisDoldur(sirketParametre);
			}
		}
		basliklariGuncelle();
		return "";
	}

	/**
	 * @param basTarih
	 * @param bitTarih
	 * @param sirket
	 */
	private void bolumDoldur(Date basTarih, Date bitTarih, Sirket sirket) {
		Long oncekiBolumId = bolumParametre != null && bolumParametre.getValue() != null ? new BigDecimal("" + bolumParametre.getValue()).longValue() : null;
		Long sirketId = sirket != null ? sirket.getId() : null;
		List<SelectItem> bolumList = null;
		if (dm != null && sirketId != null) {
			String tesisIdStr = "";
			if (tesisParametre.getValue() != null)
				tesisIdStr = "" + new BigDecimal("" + tesisParametre.getValue()).longValue();
			bolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, tesisIdStr, dm != null ? new AylikPuantaj(dm) : null, false, session);

		}
		if (bolumList == null) {
			Long tesisId = null;
			if (tesisParametre.getValue() != null)
				tesisId = new BigDecimal("" + tesisParametre.getValue()).longValue();
			AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
			aramaSecenekleri.setSirketId(sirketId);
			aramaSecenekleri.setTesisId(tesisId);
			HashMap<String, List<SelectItem>> ekSahaSelectListMap = new HashMap<String, List<SelectItem>>();
			aramaSecenekleri.setEkSahaSelectListMap(ekSahaSelectListMap);
			ekSahaSelectListMap.put("ekSaha3", null);
			ortakIslemler.setAramaSecenekEkDataDoldur(aramaSecenekleri, basTarih, bitTarih, session);
			if (ekSahaSelectListMap.get("ekSaha3") != null)
				bolumList = ekSahaSelectListMap.get("ekSaha3");
		}

		bolumParametre.setValue(null);
		if (bolumList == null)
			bolumList = new ArrayList<SelectItem>();
		if (bolumList.isEmpty() == false) {
			if (bolumList.size() == 1)
				bolumParametre.setValue("" + bolumList.get(0).getValue());
			else if (oncekiBolumId != null) {
				for (SelectItem selectItem : bolumList) {
					if (selectItem.getValue().equals(oncekiBolumId))
						bolumParametre.setValue("" + oncekiBolumId);

				}
			}
		}

		bolumParametre.setSecimList(bolumList);
	}

	/**
	 * 
	 */
	private void filllDinamikRaporParametreList() {
		dinamikRaporParametreList = pdksEntityController.getSQLParamByAktifFieldList(PdksDinamikRaporParametre.TABLE_NAME, PdksDinamikRaporParametre.COLUMN_NAME_DINAMIK_RAPOR, seciliPdksDinamikRapor.getId(), PdksDinamikRaporParametre.class, session);
		if (dinamikRaporParametreList.size() > 1)
			dinamikRaporParametreList = PdksUtil.sortListByAlanAdi(dinamikRaporParametreList, "sira", Boolean.FALSE);
		Date tarihDeger = null, basTarih = PdksUtil.convertToJavaDate(PdksUtil.getSistemBaslangicYili() + "0101", "yyyyMMdd"), bitTarih = new Date();
		Long sirketId = null;
		int adet = 0;
		tesisParametre = null;
		Long menuId = authenticatedUser.getMenuItemTime() != null ? authenticatedUser.getMenuItemTime().getId() : null;
		UserMenuItemTime menuItemTime = menuId != null ? (UserMenuItemTime) pdksEntityController.getSQLParamByFieldObject(UserMenuItemTime.TABLE_NAME, UserMenuItemTime.COLUMN_NAME_ID, authenticatedUser.getMenuItemTime().getId(), UserMenuItemTime.class, session) : null;
		LinkedHashMap<String, Object> lastMap = new LinkedHashMap<String, Object>();
		if (menuItemTime != null) {
			Gson gson = new Gson();
			LinkedHashMap<String, Object> map = gson.fromJson(menuItemTime.getParametreJSON(), LinkedHashMap.class);
			if (map != null) {
				String key = "data_" + seciliPdksDinamikRapor.getId();
				if (map.containsKey(key)) {
					try {
						LinkedTreeMap data = (LinkedTreeMap) map.get(key);
						lastMap.putAll(data);
					} catch (Exception e) {
					}
				}
			}
		}
		Calendar cal = Calendar.getInstance();
		maxYil = cal.get(Calendar.YEAR) + 1;
		dm = null;
		if (lastMap.containsKey("goster")) {
			String str = (String) lastMap.get("goster");
			try {
				goster = new Boolean(str);
			} catch (Exception e) {
				goster = true;
			}
		} else
			goster = true;
		for (PdksDinamikRaporParametre pr : dinamikRaporParametreList) {
			ENumBaslik baslik = ENumBaslik.fromValue(pr.getAciklama());
			Object paramValue = lastMap.containsKey(pr.getAciklama()) ? lastMap.get(pr.getAciklama()) : null;
			if (baslik != null) {
				pr.setValue(paramValue);
				if (baslik.equals(ENumBaslik.BAS_TARIH)) {
					basTarihParametre = pr;
					if (paramValue != null) {
						basTarih = PdksUtil.convertToJavaDate((String) paramValue, "yyyyMMdd");
						pr.setTarihDeger(basTarih);
					}
				} else if (baslik.equals(ENumBaslik.BIT_TARIH)) {
					bitTarihParametre = pr;
					if (paramValue != null) {
						bitTarih = PdksUtil.convertToJavaDate((String) paramValue, "yyyyMMdd");
						pr.setTarihDeger(bitTarih);
					}
				} else if (baslik.equals(ENumBaslik.SIRKET)) {
					sirketParametre = pr;
				} else if (baslik.equals(ENumBaslik.TESIS)) {
					tesisParametre = pr;
				} else if (baslik.equals(ENumBaslik.BOLUM)) {
					bolumParametre = pr;
				} else if (baslik.equals(ENumBaslik.YIL)) {
					yilParametre = pr;
				} else if (baslik.equals(ENumBaslik.AY)) {
					ayParametre = pr;
				}
			}
		}

		if (yilParametre != null && ayParametre != null) {
			cal = Calendar.getInstance();
			if (yilParametre.getValue() == null)
				yilParametre.setValue(cal.get(Calendar.YEAR));
			if (ayParametre.getValue() == null)
				ayParametre.setValue(cal.get(Calendar.MONTH) + 1);
			int yil = Integer.parseInt("" + yilParametre.getValue()), ay = Integer.parseInt("" + ayParametre.getValue());
			dm = ortakIslemler.getSQLDenklestirmeAy(yil, ay, session);
			if (dm == null) {
				basTarih = PdksUtil.convertToJavaDate(String.valueOf(yil * 100 + ay) + "01", "yyyyMMdd");
				bitTarih = PdksUtil.tariheGunEkleCikar(PdksUtil.tariheAyEkleCikar(basTarih, 1), -1);
			}
		}
		sirket = null;
		for (Iterator iterator = dinamikRaporParametreList.iterator(); iterator.hasNext();) {
			PdksDinamikRaporParametre pr = (PdksDinamikRaporParametre) iterator.next();
			Object paramValue = lastMap.containsKey(pr.getAciklama()) ? lastMap.get(pr.getAciklama()) : null;
			if (pr.isMantiksal()) {
				if (paramValue != null)
					pr.setMantiksalDurum(new Boolean(paramValue.toString()));
			} else if (pr.isTarih()) {
				if (paramValue != null)
					tarihDeger = PdksUtil.convertToJavaDate((String) paramValue, "yyyyMMdd");
				if (tarihDeger == null)
					tarihDeger = ortakIslemler.getBugun();
				pr.setTarihDeger(tarihDeger);
			} else {
				ENumBaslik baslik = ENumBaslik.fromValue(pr.getAciklama());
				if (baslik != null) {
					List<SelectItem> list = null;
					if (baslik.equals(ENumBaslik.YIL)) {
						if (paramValue == null)
							paramValue = cal.get(Calendar.YEAR);
						else {
							BigDecimal bd = new BigDecimal("" + paramValue);
							paramValue = bd.intValue();
						}
						pr.setValue(paramValue);
					} else if (baslik.equals(ENumBaslik.AY)) {
						list = new ArrayList<SelectItem>();
						if (paramValue == null)
							paramValue = cal.get(Calendar.MONTH) + 1;
						cal.set(Calendar.DATE, 1);
						for (int i = 0; i < 12; i++) {
							cal.set(Calendar.MONTH, i);
							list.add(new SelectItem(i + 1, PdksUtil.convertToDateString(cal.getTime(), "MMMMM")));
						}
					} else if (pr.isSirketBilgisi()) {
						++adet;
						if (yilParametre != null && ayParametre != null && dm != null) {
							List<SelectItem> sirketler = fazlaMesaiOrtakIslemler.getFazlaMesaiSirketList(null, dm != null ? new AylikPuantaj(dm) : null, false, session);
							if (sirketler != null && sirketler.isEmpty() == false)
								list = sirketler;
						}
						if (paramValue != null)
							sirketId = new BigDecimal("" + paramValue).longValue();
						if (sirketId != null && sirket == null)
							sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);

						if (list == null) {
							AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
							aramaSecenekleri.setSirketId(sirketId);
							list = ortakIslemler.setAramaSecenekSirketVeTesisData(aramaSecenekleri, basTarih, bitTarih, false, session);
						}
						if (list == null && pr.getZorunlu())
							list = new ArrayList<SelectItem>();
					} else if (pr.isTesisBilgisi()) {
						++adet;
						if (sirketId == null && sirketParametre != null && sirketParametre.getValue() != null)
							sirketId = new BigDecimal("" + sirketParametre.getValue()).longValue();
						if (sirketId != null) {
							if (sirket == null)
								sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
							if (dm != null) {
								List<SelectItem> tesisList = fazlaMesaiOrtakIslemler.getFazlaMesaiTesisList(sirket, dm != null ? new AylikPuantaj(dm) : null, false, session);
								if (tesisList != null && !tesisList.isEmpty())
									list = tesisList;
							}

							if (list == null) {
								AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
								aramaSecenekleri.setSirketId(sirketId);
								list = ortakIslemler.setAramaSecenekTesisData(aramaSecenekleri, basTarih, bitTarih, false, session);
							}
						}
						if (list == null && pr.getZorunlu())
							list = new ArrayList<SelectItem>();
						tesisIdList = list;
					}
					if (baslik.equals(ENumBaslik.BOLUM) && (sirketParametre != null && tesisParametre != null)) {
						if (sirketId == null && sirketParametre != null && sirketParametre.getValue() != null)
							sirketId = new BigDecimal("" + sirketParametre.getValue()).longValue();
						if (sirketId != null) {
							if (dm != null) {
								if (sirket == null)
									sirket = (Sirket) pdksEntityController.getSQLParamByFieldObject(Sirket.TABLE_NAME, Sirket.COLUMN_NAME_ID, sirketId, Sirket.class, session);
								String tesisIdStr = "";
								if (tesisParametre.getValue() != null && sirket.isTesisDurumu())
									tesisIdStr = "" + new BigDecimal("" + tesisParametre.getValue()).longValue();
								List<SelectItem> bolumList = fazlaMesaiOrtakIslemler.getFazlaMesaiBolumList(sirket, tesisIdStr, dm != null ? new AylikPuantaj(dm) : null, false, session);
								if (bolumList != null && !bolumList.isEmpty())
									list = bolumList;
							}
							if (list == null) {
								Long tesisId = null;
								if (tesisParametre.getValue() != null)
									tesisId = new BigDecimal("" + tesisParametre.getValue()).longValue();
								AramaSecenekleri aramaSecenekleri = new AramaSecenekleri();
								aramaSecenekleri.setSirketId(sirketId);
								aramaSecenekleri.setTesisId(tesisId);
								ortakIslemler.setAramaSecenekTesisData(aramaSecenekleri, basTarih, bitTarih, true, session);
								if (aramaSecenekleri.getEkSahaSelectListMap() != null)
									list = aramaSecenekleri.getEkSahaSelectListMap().get("ekSaha3");
							}
						}
						if (list == null && pr.getZorunlu())
							list = new ArrayList<SelectItem>();

					}
					if (paramValue != null) {
						try {
							if (pr.isKarakter() == false)
								pr.setValue(new BigDecimal("" + paramValue).longValue());
							else

								pr.setKarakterDeger(paramValue.toString());
						} catch (Exception e) {
							logger.error(e);
							e.printStackTrace();
						}

					}

					if (list != null) {
						if (!list.isEmpty()) {
							if (list.size() == 1) {
								pr.setValue(list.get(0).getValue());
								if (pr.isSirketBilgisi())
									sirketId = (Long) pr.getValue();
							}
						}
						pr.setSecimList(list);
					}
				} else if (paramValue != null) {
					if (pr.isKarakter())
						pr.setKarakterDeger((String) paramValue);
					else if (pr.isSayisal())
						pr.setKarakterDeger("" + paramValue);
				}
			}
		}
		if (tesisAlan != null)
			tesisAlan.setDurum(sirket == null || sirket.isTesisDurumu());

		if (adet == 2) {
			if (tesisIdList != null && tesisIdList.size() > 1) {
				if (sirketId == null)
					tesisIdList.clear();
			}
		} else
			tesisParametre = null;
		if (sirketParametre != null) {
			if (tesisParametre != null && tesisParametre.getSecimList() == null)
				tesisParametre.setSecimList(new ArrayList<SelectItem>());
			if (bolumParametre != null && bolumParametre.getSecimList() == null)
				bolumParametre.setSecimList(new ArrayList<SelectItem>());

		}
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		DinamikRaporHome.sayfaURL = sayfaURL;
	}

	public List<PdksDinamikRapor> getDinamikRaporList() {
		return dinamikRaporList;
	}

	public void setDinamikRaporList(List<PdksDinamikRapor> dinamikRaporList) {
		this.dinamikRaporList = dinamikRaporList;
	}

	public List<PdksDinamikRaporAlan> getDinamikRaporAlanList() {
		return dinamikRaporAlanList;
	}

	public void setDinamikRaporAlanList(List<PdksDinamikRaporAlan> dinamikRaporAlanList) {
		this.dinamikRaporAlanList = dinamikRaporAlanList;
	}

	public List<PdksDinamikRaporParametre> getDinamikRaporParametreList() {
		return dinamikRaporParametreList;
	}

	public void setDinamikRaporParametreList(List<PdksDinamikRaporParametre> dinamikRaporParametreList) {
		this.dinamikRaporParametreList = dinamikRaporParametreList;
	}

	public PdksDinamikRapor getSeciliPdksDinamikRapor() {
		return seciliPdksDinamikRapor;
	}

	public void setSeciliPdksDinamikRapor(PdksDinamikRapor seciliPdksDinamikRapor) {
		this.seciliPdksDinamikRapor = seciliPdksDinamikRapor;
	}

	public List<Liste> getDinamikRaporDataList() {
		return dinamikRaporDataList;
	}

	public void setDinamikRaporDataList(List<Liste> dinamikRaporDataList) {
		this.dinamikRaporDataList = dinamikRaporDataList;
	}

	public List<SelectItem> getTesisIdList() {
		return tesisIdList;
	}

	public void setTesisIdList(List<SelectItem> tesisIdList) {
		this.tesisIdList = tesisIdList;
	}

	public int getMaxYil() {
		return maxYil;
	}

	public void setMaxYil(int maxYil) {
		this.maxYil = maxYil;
	}

	public DenklestirmeAy getDm() {
		return dm;
	}

	public void setDm(DenklestirmeAy dm) {
		this.dm = dm;
	}

	public boolean isGoster() {
		return goster;
	}

	public void setGoster(boolean goster) {
		this.goster = goster;
	}

	public PdksDinamikRaporAlan getTesisAlan() {
		return tesisAlan;
	}

	public void setTesisAlan(PdksDinamikRaporAlan tesisAlan) {
		this.tesisAlan = tesisAlan;
	}

	public List<PdksDinamikRaporAlan> getDinamikRaporBagliAlanList() {
		return dinamikRaporBagliAlanList;
	}

	public void setDinamikRaporBagliAlanList(List<PdksDinamikRaporAlan> dinamikRaporBagliAlanList) {
		this.dinamikRaporBagliAlanList = dinamikRaporBagliAlanList;
	}

}
