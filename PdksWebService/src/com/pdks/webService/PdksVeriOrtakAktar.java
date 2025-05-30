package com.pdks.webService;

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
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.pdks.dao.PdksDAO;
import org.pdks.dao.impl.BaseDAOHibernate;
import org.pdks.entity.BordroIzinGrubu;
import org.pdks.entity.CalismaModeli;
import org.pdks.entity.DenklestirmeAy;
import org.pdks.entity.Departman;
import org.pdks.entity.ERPPersonel;
import org.pdks.entity.IzinReferansERP;
import org.pdks.entity.IzinTipi;
import org.pdks.entity.KapiSirket;
import org.pdks.entity.Parameter;
import org.pdks.entity.Personel;
import org.pdks.entity.PersonelDenklestirme;
import org.pdks.entity.PersonelDinamikAlan;
import org.pdks.entity.PersonelIzin;
import org.pdks.entity.PersonelIzinDetay;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelMesai;
import org.pdks.entity.ServiceData;
import org.pdks.entity.Sirket;
import org.pdks.entity.Tanim;
import org.pdks.entity.Tatil;
import org.pdks.entity.TatilGunView;
import org.pdks.entity.Vardiya;
import org.pdks.entity.VardiyaGun;
import org.pdks.entity.VardiyaSablonu;
import org.pdks.erp.entity.PersonelERPDB;
import org.pdks.genel.model.Constants;
import org.pdks.genel.model.Liste;
import org.pdks.genel.model.MailManager;
import org.pdks.genel.model.PdksUtil;
import org.pdks.mail.model.MailFile;
import org.pdks.mail.model.MailObject;
import org.pdks.mail.model.MailPersonel;
import org.pdks.mail.model.MailStatu;
import org.pdks.security.entity.OrganizasyonTipi;
import org.pdks.security.entity.Role;
import org.pdks.security.entity.User;
import org.pdks.security.entity.UserDigerOrganizasyon;
import org.pdks.security.entity.UserRoles;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

public class PdksVeriOrtakAktar implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3593586032361511570L;

	public Logger logger = Logger.getLogger(PdksVeriOrtakAktar.class);

	public static final int LIST_MAX_SIZE = 1800;
	public static final String FORMAT_DATE = "yyyy-MM-dd";
	public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm";
	public static final String FORMAT_TIME = "HH:mm";
	public static final String KEY_IK_MAIL_IPTAL = "ikMailIptal";
	public static final String TIPI_IK_ADMIN = Role.TIPI_IK + "Admin";

	public static boolean erpVeriOkuSaveIzinler = false, erpVeriOkuSavePersoneller = false, erpVeriOkuSaveHakedisIzinler = false;
	private static final String HELP_DESK_STATUS = "helpDeskStatus";
	public static final String[] HAKEDIS_IZIN_PROP_ORDER = { "hakedisList", "kidemBaslangicTarihi", "personelNo" };
	public static final String[] IZIN_PROP_ORDER = { "aciklama", "basZaman", "bitZaman", "durum", "izinSuresi", "izinTipi", "izinTipiAciklama", "personelNo", "referansNoERP", "sureBirimi" };
	public static final String[] PERSONEL_PROP_ORDER = { "adi", "bolumAdi", "bolumKodu", "bordroAltAlanAdi", "bordroAltAlanKodu", "cinsiyetKodu", "cinsiyeti", "departmanAdi", "departmanKodu", "dogumTarihi", "gorevKodu", "gorevi", "iseGirisTarihi", "istenAyrilmaTarihi", "kidemTarihi",
			"masrafYeriAdi", "masrafYeriKodu", "personelNo", "personelTipi", "personelTipiKodu", "sanalPersonel", "sirketAdi", "sirketKodu", "soyadi", "tesisAdi", "tesisKodu", "yoneticiPerNo", "grubaGirisTarihi", "yonetici2PerNo" };

	private static final String LAST_DATE = "9999-12-31";

	private PdksDAO pdksDAO = null;

	private HashMap fields = null;

	private HashMap<String, Object> mailMap = null;

	private User islemYapan = null;

	private VardiyaSablonu vardiyaSablonu = null;

	private boolean erpVeriOku = false;

	private KapiSirket kapiSirket = null;

	private String mesaj = null, dosyaEkAdi, parentBordroTanimKoduStr = "", kapiGiris, uygulamaBordro, ekSahaAdi = "", servisAdi;

	private Date bugun = null, ayBasi = null, minDate = null;

	private Integer sicilNoUzunluk = null;

	private ServiceData serviceData = null;

	private boolean sistemDestekVar = false, izinGirisiVar = false, personelCalismaModeliVar = false, sablonCalismaModeliVar = false, departmanYoneticiRolVar = false, yoneticiRolVarmi = false, updateYonetici2, altBolumDurum = false;

	private Tanim bosDepartman, ikinciYoneticiOlmaz;

	private TreeMap<String, Tanim> genelTanimMap;

	private List<Long> yoneticiIdList = null;

	private static String selectLOCK = "WITH(NOLOCK)", joinLOCK = "WITH(NOLOCK)";

	private List<CalismaModeli> modeller;
	private List<VardiyaSablonu> sablonlar;
	private LinkedHashMap<String, HashMap<String, List<User>>> ikUserMap;
	private LinkedHashMap<String, HashMap<String, List>> hataIKMap;
	private Sirket personelSirket;

	public PdksVeriOrtakAktar() {
		super();
		pdksDAO = Constants.pdksDAO;
		yoneticiRolVarmi = false;
		sicilNoUzunluk = null;
		if (pdksDAO != null) {
			bugun = new Date();
			fields = new HashMap();
			serviceData = null;
		}
	}

	private boolean isTatil() {
		boolean tatil = false;
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		Date bugun = PdksUtil.getDate(new Date());
		veriMap.put("basTarih", bugun);
		veriMap.put("bitTarih", bugun);
		veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, TatilGunView.SP_NAME);
		try {
			List<TatilGunView> list = pdksDAO.execSPList(veriMap, TatilGunView.class);
			if (list != null && list.size() == 1) {
				TatilGunView gunView = list.get(0);
				tatil = gunView != null && gunView.getYarimGun().booleanValue() == false;
			}
		} catch (Exception e) {
		}

		return tatil;
	}

	/**
	 * @return
	 */
	public boolean getTestSunucuDurum() {
		String hostName = PdksUtil.getHostName(false);
		String testSunucu = mailMap.containsKey("testSunucu") ? (String) mailMap.get("testSunucu") : null;
		String sunucu = testSunucu != null ? testSunucu.toLowerCase(Locale.ENGLISH) : "srvglf";
		boolean test = hostName.toLowerCase(Locale.ENGLISH).startsWith(sunucu);
		return test;
	}

	/**
	 * @param personelIdList
	 * @param departmanId
	 * @param session
	 * @return
	 */
	public HashMap<String, HashMap<String, List<User>>> getIKRollerUser() {
		HashMap<String, HashMap<String, List<User>>> map = new HashMap<String, HashMap<String, List<User>>>();
		if (pdksDAO != null) {
			List<Long> personelIdList = new ArrayList<Long>();
			boolean ikMailGonder = false;
			List<String> roller = new ArrayList<String>();
			if (mailMap != null) {
				if (mailMap.containsKey(KEY_IK_MAIL_IPTAL) == false && mailMap.containsKey("ikEntegrasyonMailGonder"))
					ikMailGonder = ((String) mailMap.get("ikEntegrasyonMailGonder")).equals("1");
			}
			HashMap rolMap = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select UR.* from " + UserRoles.TABLE_NAME + " UR " + PdksVeriOrtakAktar.getSelectLOCK());
			if (ikMailGonder == false)
				sb.append(" where 1 = 2");
			else {
				List<String> userNameList = null;
				LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
				veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_IK_USERNAME_LIST");
				veriMap.put("alanAdi", User.COLUMN_NAME_USERNAME);
				try {
					userNameList = pdksDAO.execSPList(veriMap, null);
				} catch (Exception e) {
					userNameList = new ArrayList<String>();
				}
				String fieldName = "rn";
				sb.append(" inner join " + User.TABLE_NAME + " U " + PdksVeriOrtakAktar.getJoinLOCK() + " on U." + User.COLUMN_NAME_ID + " = UR." + UserRoles.COLUMN_NAME_USER + " and U." + User.COLUMN_NAME_DURUM + " = 1 ");
				if (!userNameList.isEmpty()) {
					sb.append(" and U." + User.COLUMN_NAME_USERNAME + " :" + fieldName);
					rolMap.put(fieldName, userNameList);
					roller.add(Role.TIPI_IK);
					roller.add(Role.TIPI_IK_SIRKET);
					roller.add(Role.TIPI_IK_Tesis);
				}
				sb.append(" inner join " + Role.TABLE_NAME + " R " + PdksVeriOrtakAktar.getJoinLOCK() + " on R." + Role.COLUMN_NAME_ID + " = UR." + UserRoles.COLUMN_NAME_ROLE);
				sb.append(" and R." + Role.COLUMN_NAME_STATUS + " = 1");
				if (userNameList.isEmpty())
					sb.append(" where 1 = 2");
			}
			List<UserRoles> pdksRoles = pdksDAO.getNativeSQLList(rolMap, sb, UserRoles.class);
			if (!pdksRoles.isEmpty()) {

				List<Long> userIdList = new ArrayList<Long>();
				for (UserRoles userRoles : pdksRoles) {
					User user = userRoles.getUser();
					if (roller.contains(userRoles.getRole().getRolename()) && !userIdList.contains(user.getId()))
						userIdList.add(user.getId());
				}
				rolMap.clear();
				sb = new StringBuffer();
				sb.append("select UR.* from " + UserDigerOrganizasyon.TABLE_NAME + " UR " + PdksVeriOrtakAktar.getSelectLOCK());
				sb.append(" where " + UserDigerOrganizasyon.COLUMN_NAME_USER + " :u and " + UserDigerOrganizasyon.COLUMN_NAME_TIPI + " = " + OrganizasyonTipi.TESIS.value());
				rolMap.put("u", userIdList);
				List<UserDigerOrganizasyon> digerOrganizasyonsList = pdksDAO.getNativeSQLList(rolMap, sb, UserDigerOrganizasyon.class);
				HashMap<Long, List<Tanim>> userTesisMap = new HashMap<Long, List<Tanim>>();
				for (UserDigerOrganizasyon userDigerOrganizasyon : digerOrganizasyonsList) {
					Long key = userDigerOrganizasyon.getUser().getId();
					List<Tanim> tesisList = userTesisMap.containsKey(key) ? userTesisMap.get(key) : new ArrayList<Tanim>();
					if (tesisList.isEmpty())
						userTesisMap.put(key, tesisList);
					tesisList.add(userDigerOrganizasyon.getOrganizasyon());
				}
				userIdList = null;
				digerOrganizasyonsList = null;
				HashMap<String, List<UserRoles>> araMap = new HashMap<String, List<UserRoles>>();
				for (UserRoles userRoles : pdksRoles) {
					User user = userRoles.getUser();
					if (user != null && user.isDurum() && user.getPdksPersonel().isCalisiyor()) {
						String roleAdi = userRoles.getRole().getRolename();
						if (userTesisMap.containsKey(user.getId()))
							roleAdi = Role.TIPI_IK_Tesis;
						else if (roleAdi.equals(Role.TIPI_IK) && user.getDepartman().isAdminMi())
							roleAdi = TIPI_IK_ADMIN;
						List<UserRoles> dataList = araMap.containsKey(roleAdi) ? araMap.get(roleAdi) : new ArrayList<UserRoles>();
						if (dataList.isEmpty())
							araMap.put(roleAdi, dataList);
						dataList.add(userRoles);
						user.setYetkiSet(true);
						user.setIK(false);
					}
				}
				if (araMap.containsKey(TIPI_IK_ADMIN)) {
					roller.clear();
					roller.add(TIPI_IK_ADMIN);
					roller.add(Role.TIPI_IK);
					roller.add(Role.TIPI_IK_SIRKET);
					roller.add(Role.TIPI_IK_Tesis);
				}
				pdksRoles.clear();
				for (String roleAdi : roller) {
					if (araMap.containsKey(roleAdi)) {
						List<UserRoles> dataList = araMap.get(roleAdi);
						for (Iterator iterator = dataList.iterator(); iterator.hasNext();) {
							UserRoles userRoles = (UserRoles) iterator.next();
							User user = userRoles.getUser();
							Personel personel = user.getPdksPersonel();
							if (!personelIdList.contains(personel.getId())) {
								personelIdList.add(personel.getId());
								if (!roleAdi.equals(TIPI_IK_ADMIN) && !roleAdi.equals(Role.TIPI_IK))
									pdksRoles.add(userRoles);
							} else
								iterator.remove();
						}
						if (dataList.isEmpty())
							dataList.remove(roleAdi);
						else {
							if (roleAdi.equals(TIPI_IK_ADMIN)) {
								List<User> list = new ArrayList<User>();
								for (UserRoles userRoles : dataList) {
									User user = userRoles.getUser();
									user.setIK(true);
									list.add(user);
								}
								HashMap<String, List<User>> map1 = new HashMap<String, List<User>>();
								map1.put(TIPI_IK_ADMIN, list);
								map.put(Role.TIPI_IK, map1);
								dataList = null;
							} else if (roleAdi.equals(Role.TIPI_IK)) {
								for (Iterator iterator = dataList.iterator(); iterator.hasNext();) {
									UserRoles userRoles = (UserRoles) iterator.next();
									User user = userRoles.getUser();
									user.setIK(true);
									String roleAdiDep = roleAdi + (user.getDepartman() != null ? "_" + user.getDepartman().getId() : "");
									HashMap<String, List<User>> map1 = map.containsKey(roleAdi) ? map.get(roleAdi) : new HashMap<String, List<User>>();
									if (map1.isEmpty())
										map.put(roleAdi, map1);
									List<User> list = map1.containsKey(roleAdiDep) ? map1.get(roleAdiDep) : new ArrayList<User>();
									if (list.isEmpty())
										map1.put(roleAdiDep, list);
									list.add(user);

								}

							}

						}
					}

				}

				if (araMap.containsKey(Role.TIPI_IK_Tesis) || araMap.containsKey(Role.TIPI_IK_SIRKET)) {
					pdksRoles.clear();
					if (araMap.containsKey(Role.TIPI_IK_SIRKET))
						pdksRoles.addAll(araMap.get(Role.TIPI_IK_SIRKET));
					if (araMap.containsKey(Role.TIPI_IK_Tesis))
						pdksRoles.addAll(araMap.get(Role.TIPI_IK_Tesis));
					for (UserRoles userRoles : pdksRoles) {
						User user = userRoles.getUser();
						String roleAdi = userRoles.getRole().getRolename();
						Personel personel = user.getPdksPersonel();
						if (roleAdi.equals(Role.TIPI_IK_Tesis) && personel.getTesis() == null)
							continue;
						if (userTesisMap.containsKey(user.getId())) {
							List<Tanim> tesisList = userTesisMap.get(user.getId());
							Tanim tesis = personel.getTesis();
							if (tesis != null)
								tesisList.add(tesis);
							List<Long> idList = new ArrayList<Long>();
							for (Tanim tanim : tesisList) {
								if (!idList.contains(tanim.getId())) {
									idList.add(tanim.getId());
									roleAdi = Role.TIPI_IK_Tesis;
									String key = tanim.getErpKodu();
									HashMap<String, List<User>> map1 = map.containsKey(roleAdi) ? map.get(roleAdi) : new HashMap<String, List<User>>();
									if (map1.isEmpty())
										map.put(roleAdi, map1);
									List<User> list = map1.containsKey(key) ? map1.get(key) : new ArrayList<User>();
									if (list.isEmpty())
										map1.put(key, list);
									list.add(user);
								}

							}
						} else {
							String key = "" + (roleAdi.equals(Role.TIPI_IK_Tesis) ? personel.getTesis().getErpKodu() : personel.getSirket().getErpKodu());
							HashMap<String, List<User>> map1 = map.containsKey(roleAdi) ? map.get(roleAdi) : new HashMap<String, List<User>>();
							if (map1.isEmpty())
								map.put(roleAdi, map1);
							List<User> list = map1.containsKey(key) ? map1.get(key) : new ArrayList<User>();
							if (list.isEmpty())
								map1.put(key, list);
							list.add(user);
						}

					}
				}
				araMap = null;

			}
			roller = null;
			pdksRoles = null;
		}
		return map;
	}

	/**
	 * @param basTarih
	 * @param bitTarih
	 * @return
	 */
	public TreeMap<String, Tatil> getTatilGunleri(Date basTarih, Date bitTarih) {
		if (bitTarih == null)
			bitTarih = PdksUtil.getDate(bugun != null ? bugun : new Date());
		if (basTarih == null)
			basTarih = PdksUtil.tariheGunEkleCikar(bitTarih, -7);
		TreeMap<String, Tatil> tatilMap = new TreeMap<String, Tatil>();
		String pattern = PdksUtil.getDateTimeFormat();
		Calendar cal = Calendar.getInstance();
		cal.setTime(basTarih);
		int basYil = cal.get(Calendar.YEAR);
		cal.setTime(bitTarih);
		int bitYil = cal.get(Calendar.YEAR);
		List<Tatil> pdksTatilList = new ArrayList<Tatil>(), tatilList = new ArrayList<Tatil>();
		String formatStr = "yyyy-MM-dd";
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_GET_TATIL");
		map.put("basTarih", basTarih != null ? PdksUtil.convertToDateString(basTarih, formatStr) : null);
		map.put("bitTarih", basTarih != null ? PdksUtil.convertToDateString(bitTarih, formatStr) : null);
		map.put("df", null);

		boolean ayir = false;
		try {
			List<Object[]> list = pdksDAO.execSPList(map, null);
			if (!list.isEmpty()) {
				List<Long> idList = new ArrayList<Long>();
				TreeMap<Long, Integer> tatilVersionMap = new TreeMap<Long, Integer>();
				for (Object[] objects : list) {
					Long id = ((BigDecimal) objects[0]).longValue();
					if (!idList.contains(id))
						idList.add(id);
					tatilVersionMap.put(id, 0);
					Tatil tatil = new Tatil();
					tatil.setId(id);
					tatil.setBasTarih((Date) objects[1]);
					tatil.setBitTarih((Date) objects[2]);
					tatilList.add(tatil);
				}
				map.clear();
				// map.put(PdksVeriOrtakAktar.MAP_KEY_MAP, "getId");
				String fieldName = "id";
				map.put(fieldName, idList);

				// TreeMap<Long, Tatil> tatilDataMap = PdksVeriOrtakAktar.getObjectByInnerObjectMap(map, Tatil.class, false);
				TreeMap<Long, Tatil> tatilDataMap = pdksDAO.getObjectByInnerObjectMap("getId", fieldName, idList, Tatil.class, Boolean.TRUE);

				for (Tatil tatil : tatilList) {
					Tatil orjTatil = (Tatil) tatilDataMap.get(tatil.getId()).clone();
					orjTatil.setVersion(tatilVersionMap.get(tatil.getId()));
					orjTatil.setBasTarih(tatil.getBasTarih());
					orjTatil.setBitTarih(tatil.getBitTarih());
					Integer ver = orjTatil.getVersion() + 1;
					tatilVersionMap.put(tatil.getId(), ver);
					pdksTatilList.add(orjTatil);
				}
				tatilDataMap = null;
				idList = null;
				tatilList.clear();
			}
			list = null;
		} catch (Exception e) {
			ayir = true;
			map.clear();
			map.put("basTarih<=", bitTarih);
			map.put("bitisTarih>=", basTarih);

			tatilList = pdksDAO.getObjectByInnerObjectListInLogic(map, Tatil.class);
			if (!tatilList.isEmpty())
				tatilList = PdksUtil.sortListByAlanAdi(tatilList, "basTarih", false);
		}

		if (ayir) {
			if (tatilList.size() > 1) {
				for (Iterator iterator = tatilList.iterator(); iterator.hasNext();) {
					Tatil pdksTatil = (Tatil) iterator.next();
					if (!pdksTatil.isTekSefer()) {
						pdksTatilList.add(pdksTatil);
						iterator.remove();
					}
				}
				if (!pdksTatilList.isEmpty()) {
					tatilList.addAll(pdksTatilList);
					pdksTatilList.clear();
				}
			}
			for (Iterator<Tatil> iterator = tatilList.iterator(); iterator.hasNext();) {
				Tatil pdksTatilOrj = iterator.next();
				Tatil pdksTatil = (Tatil) pdksTatilOrj.clone();
				if (pdksTatil.isTekSefer()) {
					if (getObjeTarihiAraliktaMi(basTarih, bitTarih, pdksTatil.getBasTarih(), pdksTatil.getBitTarih()))
						pdksTatilList.add(pdksTatil);
				} else
					for (int i = basYil; i <= bitYil; i++) {
						Tatil pdksTatilP = (Tatil) pdksTatil.clone();
						cal.setTime(pdksTatilP.getBasTarih());
						cal.set(Calendar.YEAR, i);
						pdksTatilP.setYarimGun(pdksTatil.isYarimGunMu());
						pdksTatilP.setBasTarih(cal.getTime());
						cal.setTime(pdksTatilP.getBitTarih());
						cal.set(Calendar.YEAR, i);
						Date bitisTarih = PdksUtil.convertToJavaDate(PdksUtil.convertToDateString(cal.getTime(), "yyyyMMdd") + " 23:59:59", "yyyyMMdd HH:mm:ss");
						pdksTatilP.setBitTarih(bitisTarih);
						if (getObjeTarihiAraliktaMi(basTarih, bitTarih, pdksTatilP.getBasTarih(), pdksTatilP.getBitTarih()))
							pdksTatilList.add(pdksTatilP);
					}

			}
		}
		String arifeTatilBasZaman = getParameterKey("arifeTatilBasZaman");
		if (!pdksTatilList.isEmpty()) {
			String yarimGunStr = (mailMap != null && mailMap.containsKey("yarimGunSaati") ? (String) mailMap.get("yarimGunSaati") : "");
			if (PdksUtil.hasStringValue(arifeTatilBasZaman))
				yarimGunStr = arifeTatilBasZaman;
			int saat = 13, dakika = 0;
			if (yarimGunStr.indexOf(":") > 0) {
				StringTokenizer st = new StringTokenizer(yarimGunStr, ":");
				if (st.countTokens() >= 2) {
					try {
						saat = Integer.parseInt(st.nextToken().trim());
					} catch (Exception e) {
						logger.error("Pdks hata in : \n");
						e.printStackTrace();
						logger.error("Pdks hata out : " + e.getMessage());
						saat = 13;
					}
					try {
						dakika = Integer.parseInt(st.nextToken().trim());
					} catch (Exception e) {
						logger.error("Pdks hata in : \n");
						e.printStackTrace();
						logger.error("Pdks hata out : " + e.getMessage());
						saat = 13;
						dakika = 0;
					}
				}
			}

			for (Tatil pdksTatil : pdksTatilList) {
				Date tarih = pdksTatil.getBasTarih();
				Boolean ilkGun = Boolean.TRUE;
				Tatil orjTatil = (Tatil) pdksTatil.clone();
				orjTatil.setBasTarih(PdksUtil.getDate(orjTatil.getBasTarih()));
				orjTatil.setBitGun(tariheGunEkleCikar(cal, PdksUtil.getDate(orjTatil.getBitTarih()), 1));
				if (pdksTatil.isYarimGunMu()) {
					orjTatil.setBasTarih(PdksUtil.setTarih(orjTatil.getBasTarih(), Calendar.HOUR_OF_DAY, saat));
					orjTatil.setBasTarih(PdksUtil.setTarih(orjTatil.getBasTarih(), Calendar.MINUTE, dakika));
				}
				while (PdksUtil.tarihKarsilastirNumeric(pdksTatil.getBitTarih(), tarih) != -1) {
					String tarihStr = PdksUtil.convertToDateString(tarih, "yyyyMMdd");
					boolean yarimGun = ilkGun && pdksTatil.isYarimGunMu();
					if (pdksTatil.isPeriyodik() || !ilkGun || !tatilMap.containsKey(tarihStr)) {
						if (tatilMap.containsKey(tarihStr)) {
							Tatil tatil = tatilMap.get(tarihStr);
							if (yarimGun && !tatil.isYarimGunMu()) {
								tarih = tariheGunEkleCikar(cal, tarih, 1);
								ilkGun = Boolean.FALSE;
								continue;
							}

						}
						Tatil tatil = new Tatil();
						tatil.setOrjTatil((Tatil) orjTatil.clone());
						tatil.setBasTarih(tarih);
						tatil.setAciklama(pdksTatil.getAciklama());
						tatil.setAd(pdksTatil.getAd());
						tatil.setYarimGun(yarimGun);
						if (yarimGun)
							tatil.setArifeVardiyaYarimHesapla(pdksTatil.getArifeVardiyaYarimHesapla());
						tatil.setBasTarih(PdksUtil.getDate(tatil.getBasTarih()));
						if (tatil.isYarimGunMu()) {
							tatil.setBasTarih(PdksUtil.setTarih(tatil.getBasTarih(), Calendar.HOUR_OF_DAY, saat));
							tatil.setBasTarih(PdksUtil.setTarih(tatil.getBasTarih(), Calendar.MINUTE, dakika));
						}
						tatil.setBitGun(PdksUtil.getDate(tariheGunEkleCikar(cal, tarih, 1)));
						tatil.setBitTarih((Date) orjTatil.getBitGun());
						tatil.setBasGun(orjTatil.getBasTarih());
						tatilMap.put(tarihStr, tatil);
					}
					tarih = tariheGunEkleCikar(cal, tarih, 1);
					ilkGun = Boolean.FALSE;
				}

			}
		}

		if (!tatilMap.isEmpty()) {
			pattern = "yyyyMMdd";
			for (String dateStr : tatilMap.keySet()) {
				String afterDateStr = PdksUtil.convertToDateString(tariheGunEkleCikar(cal, PdksUtil.convertToJavaDate(dateStr, pattern), 1), pattern);
				if (tatilMap.containsKey(afterDateStr)) {
					Tatil tatil = tatilMap.get(dateStr), sonrakiTatil = tatilMap.get(afterDateStr);
					if (!sonrakiTatil.isYarimGunMu() && !tatil.getAd().equals(sonrakiTatil.getAd())) {
						tatil.setBitTarih(sonrakiTatil.getBitTarih());
					}
				}
			}
		}
		return tatilMap;
	}

	/**
	 * @param basTarih
	 * @param bitTarih
	 * @param basTarihObje
	 * @param bitTarihObje
	 * @return
	 */
	public Boolean getObjeTarihiAraliktaMi(Date basTarih, Date bitTarih, Date basTarihObje, Date bitTarihObje) {
		String patern = "yyyyMMdd";
		long basTarihLong = Long.parseLong(PdksUtil.convertToDateString(basTarih, patern));
		long bitTarihLong = Long.parseLong(PdksUtil.convertToDateString(bitTarih, patern));
		long basTarihObjeLong = Long.parseLong(PdksUtil.convertToDateString(basTarihObje, patern));
		long bitTarihObjeLong = Long.parseLong(PdksUtil.convertToDateString(bitTarihObje, patern));
		boolean durum = (bitTarihLong >= basTarihObjeLong) && (basTarihLong <= bitTarihObjeLong);
		return durum;
	}

	/**
	 * @param cal
	 * @param date
	 * @param gunSayisi
	 * @return
	 */
	public Date tariheGunEkleCikar(Calendar cal, Date date, int gunSayisi) {
		Date tarih = null;
		if (date != null)
			tarih = addTarih(cal, date, Calendar.DATE, gunSayisi);

		return tarih;
	}

	/**
	 * @param cal
	 * @param tarih
	 * @param field
	 * @param value
	 * @return
	 */
	public Date addTarih(Calendar cal, Date tarih, int field, int value) {
		if (tarih != null) {
			if (cal == null)
				cal = Calendar.getInstance();
			cal.setTime((Date) tarih.clone());
			try {
				cal.add(field, value);
				tarih = cal.getTime();
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());

			}
		}
		return tarih;
	}

	/**
	 * @param fonksiyonAdi
	 * @param bilgi
	 * @param list
	 */
	protected void mailBosGonder(String fonksiyonAdi, String bilgi, List list) {
		if (PdksUtil.isSistemDestekVar() && PdksUtil.isPazar() == false) {
			if (list != null && list.size() > 1 && PdksUtil.hasStringValue(fonksiyonAdi) && PdksUtil.hasStringValue(bilgi)) {
				if (PdksUtil.hasStringValue(uygulamaBordro) == false)
					uygulamaBordro = mailMap.containsKey("uygulamaBordro") ? (String) mailMap.get("uygulamaBordro") : "Bordro Uygulaması ";
				MailObject mailObject = new MailObject();
				String subject = uygulamaBordro + " " + fonksiyonAdi + " fonksiyon " + bilgi + " güncellemesi";
				String body = subject + " " + list.size() + " adet kayıt için başarılı olarak tamamlandı.";
				logger.info(subject + "\n" + body);
				mailObject.setSubject(subject);
				mailObject.setBody(body);
				try {
					// mailAdresKontrol(mailObject, null);
					if (PdksUtil.isSistemDestekVar()) {
						MailManager.addMailAdresBCC(mailObject, "bccAdres", mailMap);
						MailManager.addMailAdresBCC(mailObject, "bccEntegrasyonAdres", mailMap);
					}

					mailMap.put("mailObject", mailObject);
					MailManager.ePostaGonder(mailMap);
				} catch (Exception e) {
					logger.error(e);
					e.printStackTrace();
				}

			}
		}

	}

	/**
	 * @param kodu
	 * @return
	 */
	private List<Personel> saveIkinciYoneticiOlmazList(String kodu) {
		List<Personel> list = null;
		LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
		map.put("yoneticiId", 0L);
		map.put("tipi", kodu);
		map.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_IKINCI_YONETICI_OLAMAZ");
		try {
			list = pdksDAO.execSPList(map, Personel.class);
			if (!list.isEmpty()) {
				Date guncellemeTarihi = new Date();
				for (Personel personel2 : list) {
					personel2.setAsilYonetici2(personel2.getYoneticisi());
					personel2.setGuncellemeTarihi(guncellemeTarihi);
					personel2.setGuncelleyenUser(islemYapan);
				}
				pdksDAO.saveObjectList(list);
			}

		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
		if (list == null)
			list = new ArrayList<Personel>();
		return list;
	}

	/**
	 * @param adi
	 * @return
	 */
	public String getParametreDeger(String adi) {
		PdksDAO pdksDAO = Constants.pdksDAO;
		String deger = null;
		HashMap fields = new HashMap();
		fields.put("name", adi);
		StringBuffer sb = new StringBuffer();
		sb.append("select dbo.FN_GET_PARAMETRE_DEGER(:name) ");
		List<Object> veriList = pdksDAO.getNativeSQLList(fields, sb, null);
		if (veriList != null && !veriList.isEmpty()) {
			Object object = veriList.get(0);
			if (object != null && object instanceof String)
				deger = (String) object;

		}
		return deger;
	}

	/**
	 * @return
	 */
	public static String getParametrePersonelERPTableView() {
		String str = "personelERPTableViewAdi";
		return str;
	}

	/**
	 * @return
	 */
	public static String getParametreHakEdisIzinERPTableView() {
		String str = "hakEdisIzinERPTableViewAdi";
		return str;
	}

	/**
	 * @return
	 */
	public static String getParametreIzinERPTableView() {
		String str = "izinERPTableViewAdi";
		return str;
	}

	/**
	 * @param dao
	 */
	private void personelKontrolVerileriAyarla(PdksDAO dao) {
		izinGirisiVar = !sistemDestekVar;
		departmanYoneticiRolVar = !sistemDestekVar;
		HashMap fields = new HashMap();
		StringBuffer sb = new StringBuffer();
		sb.append("with BUGUN as ( ");
		sb.append("		select 1 as ID ");
		sb.append("	),");
		sb.append("	DEP_YONETICI as (");
		sb.append("		select R.ROLENAME DEP_YONETICI_ROL_ADI from " + Role.TABLE_NAME + " R " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append(" where R." + Role.COLUMN_NAME_ROLE_NAME + " = '" + Role.TIPI_DIREKTOR_SUPER_VISOR + "' and R." + Role.COLUMN_NAME_STATUS + " = 1 ");
		sb.append("	),");
		sb.append("	IZIN_DURUM as (");
		sb.append("		select count(I.ID) as IZIN_TIPI_ADET from " + IzinTipi.TABLE_NAME + " I " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append("			inner join " + Departman.TABLE_NAME + " D " + PdksVeriOrtakAktar.getJoinLOCK() + " on D." + Departman.COLUMN_NAME_ID + " = I." + IzinTipi.COLUMN_NAME_DEPARTMAN + " and D." + Departman.COLUMN_NAME_ADMIN_DURUM + " = 1 and D." + Departman.COLUMN_NAME_DURUM + " = 1 ");
		sb.append(" where I." + IzinTipi.COLUMN_NAME_DEPARTMAN + " = 1 and I." + IzinTipi.COLUMN_NAME_DURUM + " = 1 and I." + IzinTipi.COLUMN_NAME_BAKIYE_IZIN_TIPI + " is null and I." + IzinTipi.COLUMN_NAME_GIRIS_TIPI + " <> '" + IzinTipi.GIRIS_TIPI_YOK + "'");
		sb.append("	)");
		sb.append("	select COALESCE(DY.DEP_YONETICI_ROL_ADI,'') DEP_YONETICI_ROL_ADI,");
		sb.append("		COALESCE(ID.IZIN_TIPI_ADET,0) IZIN_TIPI_ADET, GETDATE() as TARIH from BUGUN B ");
		sb.append("	left join DEP_YONETICI DY " + PdksVeriOrtakAktar.getJoinLOCK() + " on 1=1");
		sb.append("	left join IZIN_DURUM ID " + PdksVeriOrtakAktar.getJoinLOCK() + " on 1=1");
		List<Object[]> veriList = dao.getNativeSQLList(fields, sb, null);
		if (!veriList.isEmpty()) {
			Object[] veri = veriList.get(0);
			departmanYoneticiRolVar = ((String) veri[0]).length() > 0;
			izinGirisiVar = ((Integer) veri[1]).intValue() > 0;
		}
		veriList = null;
		List<String> roleList = null;
		if (mailMap.containsKey("yoneticiRolleri")) {
			String yoneticiRolleri = (String) mailMap.get("yoneticiRolleri");
			if (PdksUtil.hasStringValue(yoneticiRolleri))
				roleList = PdksUtil.getListByString(yoneticiRolleri, null);
		}
		if (roleList == null || roleList.isEmpty())
			roleList = Arrays.asList(new String[] { Role.TIPI_GENEL_MUDUR, Role.TIPI_YONETICI, Role.TIPI_YONETICI_KONTRATLI });
		String fieldName = "r";
		sb = new StringBuffer();
		sb.append("select R." + Role.COLUMN_NAME_ROLE_NAME + " from " + Role.TABLE_NAME + " R " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append("	where R." + Role.COLUMN_NAME_STATUS + " = 1 and R.ADMIN_ROLE<>1 and R." + Role.COLUMN_NAME_ROLE_NAME + " :" + fieldName);
		fields.put(fieldName, roleList);
		veriList = getNativeSQLParamList(roleList, sb, fieldName, fields, null);
		yoneticiRolVarmi = !veriList.isEmpty();
		veriList = null;
	}

	/**
	 * @param userIKList
	 * @param mailDataMap
	 * @param dao
	 * @return
	 */
	public MailObject kullaniciIKYukle(List<User> userIKList, HashMap<String, Object> mailDataMap, PdksDAO dao) {
		MailObject mailService = new MailObject();
		List<User> userList = null;
		StringBuffer sb = new StringBuffer();
		if (mailMap == null)
			mailMap = mailDataMap;
		boolean testDurum = getTestDurum();
		String testMailAdres = mailDataMap.containsKey("testMailAdres") ? (String) mailDataMap.get("testMailAdres") : "pdkssayar@gmail.com";
		if (!mailDataMap.containsKey(KEY_IK_MAIL_IPTAL)) {
			LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
			veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_IK_USERNAME_LIST");
			veriMap.put("alanAdi", "*");
			try {
				userList = dao.execSPList(veriMap, User.class);
			} catch (Exception e) {
				List<String> ikYetkiliRoller = Arrays.asList(new String[] { Role.TIPI_IK, Role.TIPI_IK_SIRKET, Role.TIPI_IK_Tesis });
				HashMap fields = new HashMap();
				sb.append("select U.* from " + Role.TABLE_NAME + " R " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
				sb.append(" inner join " + UserRoles.TABLE_NAME + " UR " + PdksVeriOrtakAktar.getJoinLOCK() + " on UR." + UserRoles.COLUMN_NAME_ROLE + " = R." + Role.COLUMN_NAME_ID);
				sb.append(" inner join " + User.TABLE_NAME + " U " + PdksVeriOrtakAktar.getJoinLOCK() + " on U." + User.COLUMN_NAME_ID + " = UR." + UserRoles.COLUMN_NAME_USER + " and U." + User.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" inner join " + Departman.TABLE_NAME + " D " + PdksVeriOrtakAktar.getJoinLOCK() + " on D." + Departman.COLUMN_NAME_ID + " = U." + User.COLUMN_NAME_DEPARTMAN + " and D." + Departman.COLUMN_NAME_ADMIN_DURUM + " = 1 and D." + Departman.COLUMN_NAME_DURUM + " = 1 ");
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = U." + User.COLUMN_NAME_PERSONEL + " and P." + Personel.COLUMN_NAME_DURUM + " = 1 and P." + Personel.COLUMN_NAME_ISTEN_AYRILIS_TARIHI + " > GETDATE() ");
				sb.append(" where R." + Role.COLUMN_NAME_ROLE_NAME + " :r ");
				fields.put("r", ikYetkiliRoller);
				userList = dao.getNativeSQLList(fields, sb, User.class);
			}

		}
		String mailIcerik = "";
		if (mailDataMap.containsKey("mailIcerik"))
			mailIcerik = (String) mailDataMap.get("mailIcerik");
		if (userIKList != null) {
			if (userList == null)
				userList = new ArrayList<User>();
			userList.addAll(userIKList);
		}

		if (userList != null) {
			userList = PdksUtil.sortObjectStringAlanList(userList, "getAdSoyad", null);
			List<String> list = new ArrayList<String>();
			for (Iterator iterator = userList.iterator(); iterator.hasNext();) {
				User user = (User) iterator.next();
				if (list.contains(user.getEmail()))
					continue;
				list.add(user.getEmail());
				MailPersonel mailPersonel = new MailPersonel();
				if (userList != null && userList.size() == 1 && mailIcerik.length() > 1)
					mailIcerik = "Sayın <b>" + user.getAdSoyad() + "</b><br></br><br></br>" + mailIcerik;
				mailPersonel.setAdiSoyadi(user.getAdSoyad());
				String eposta = user.getEmail();
				if (testDurum && eposta.indexOf("@") > 1)
					eposta = testMailAdres;
				// eposta = PdksUtil.replaceAll(eposta, "@", "xyz@");
				mailPersonel.setePosta(eposta);
				mailService.getToList().add(mailPersonel);
			}
		}
		mailService.setBody(mailIcerik);
		if (mailDataMap.containsKey("konu"))
			mailService.setSubject((String) mailDataMap.get("konu"));

		if (mailDataMap.containsKey("fileMap")) {
			LinkedHashMap<String, Object> fileMap = (LinkedHashMap<String, Object>) mailDataMap.get("fileMap");
			for (String fileName : fileMap.keySet()) {
				MailFile mailFile = new MailFile();
				mailFile.setDisplayName(fileName);
				String str = (String) fileMap.get(fileName);
				mailFile.setIcerik(PdksUtil.getBytesUTF8(str));
				mailService.getAttachmentFiles().add(mailFile);
			}
		}
		sb = new StringBuffer();
		// try {
		// mailAdresKontrol(mailObject, sb);
		// } catch (Exception e) {
		// logger.error(e);
		// }
		sb = null;

		mailDataMap.put("mailObject", mailService);
		return mailService;
	}

	private boolean getTestDurum() {
		return !PdksUtil.getCanliSunucuDurum() && !PdksUtil.getTestSunucuDurum();
	}

	/**
	 * @param sirket
	 * @return
	 */
	protected TreeMap<String, Boolean> getOnaysizIzinDurumMap(Sirket sirket) {
		TreeMap<String, Boolean> dataMap = new TreeMap<String, Boolean>();
		boolean onaysizIzinSec = false, onaysizIzinDurum = false, ikinciYoneticiIzinOnayla = false;
		try {
			long sirketId = sirket != null ? sirket.getIdLong() : 0L;
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			map.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_SIRKET_IZIN_ONAY_BILGI");
			map.put("sirketId", sirketId);
			List<Object[]> list = pdksDAO.execSPList(map, null);
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
	 * @param dao
	 * @param lockVar
	 */
	public HashMap<String, Object> sistemVerileriniYukle(PdksDAO dao, boolean lockVar) {
		if (dao == null)
			dao = Constants.pdksDAO;
		if (dao != null) {
			mesaj = null;
			List<String> helpDeskList = new ArrayList<String>();
			if (mailMap == null)
				mailMap = new HashMap<String, Object>();
			else
				mailMap.clear();
			HashMap<String, Parameter> pmMap = new HashMap<String, Parameter>();
			islemYapan = getSistemAdminUser(dao);
			try {
				fields.clear();
				HashMap fields = new HashMap();
				StringBuffer sb = new StringBuffer();
				sb.append("select * from " + Parameter.TABLE_NAME + (lockVar ? " " + selectLOCK : ""));
				// sb.append(" where " + Parameter.COLUMN_NAME_DURUM + " = 1 ");
				List<Parameter> list = dao.getNativeSQLList(fields, sb, Parameter.class);
				for (Parameter parameter : list) {
					String key = parameter.getName().trim(), deger = parameter.getValue().trim();
					pmMap.put(key, parameter);
					if (parameter.getActive() != null && parameter.getActive()) {
						mailMap.put(key, deger);
						if (parameter.isHelpDeskMi())
							helpDeskList.add(key);
					}

				}
				String readUnCommitted = "";
				if (mailMap.containsKey("readUnCommitted"))
					readUnCommitted = (String) mailMap.get("readUnCommitted");
				BaseDAOHibernate.setReadUnCommitted(readUnCommitted != null && readUnCommitted.equals("1"));
				String selectLOCK = "WITH(NOLOCK)", joinLOCK = "WITH(NOLOCK)";
				if (mailMap.containsKey("selectLOCK"))
					selectLOCK = (String) mailMap.get("selectLOCK");
				if (mailMap.containsKey("joinLOCK"))
					joinLOCK = (String) mailMap.get("joinLOCK");
				PdksVeriOrtakAktar.setSelectLOCK(" " + selectLOCK + " ");
				PdksVeriOrtakAktar.setJoinLOCK(" " + joinLOCK + " ");

			} catch (Exception e) {
				logger.error(e);
				e.printStackTrace();
			}

			sistemDestekVar = false;

			String testSunucu = "srvglftest";
			if (mailMap.containsKey("testSunucu"))
				testSunucu = (String) mailMap.get("testSunucu");
			PdksUtil.setTestSunucu(testSunucu);
			String canliSunucu = "srvglf";
			if (mailMap.containsKey("canliSunucu"))
				canliSunucu = (String) mailMap.get("canliSunucu");
			PdksUtil.setCanliSunucu(canliSunucu);
			String dateFormat = null;
			if (mailMap.containsKey("dateFormat")) {
				String str = null;
				try {
					str = PdksUtil.convertToDateString(new Date(), (String) mailMap.get("dateFormat"));
					if (str != null && str.length() > 1)
						dateFormat = (String) mailMap.get("dateFormat");
				} catch (Exception e) {
				}
			}
			kapiGiris = mailMap.containsKey("kapiGirisUygulama") ? (String) mailMap.get("kapiGirisUygulama") : "kapı giriş";
			kapiSirket = null;
			bugun = PdksUtil.getDate(new Date());
			String birdenFazlaKGSSirketSQL = mailMap.containsKey("birdenFazlaKGSSirketSQL") ? (String) mailMap.get("birdenFazlaKGSSirketSQL") : "";
			if (PdksUtil.hasStringValue(birdenFazlaKGSSirketSQL)) {
				HashMap map = new HashMap();
				map.put("id>", 0L);
				map.put("basTarih<=", bugun);
				map.put("bitTarih>=", bugun);
				List<KapiSirket> kapiSirketList = pdksDAO.getObjectByInnerObjectListInLogic(map, KapiSirket.class);
				if (kapiSirketList.size() == 1) {
					kapiSirket = kapiSirketList.get(0);
					kapiGiris = kapiSirket.getAciklama();
				}
				kapiSirketList = null;

			}
			uygulamaBordro = mailMap.containsKey("uygulamaBordro") ? (String) mailMap.get("uygulamaBordro") : "Bordro Uygulaması ";
			if (dateFormat == null)
				dateFormat = "dd/MM/yyyy";
			PdksUtil.setDateFormat(dateFormat);
			if (mailMap.containsKey("helpDeskLastDate")) {
				String helpDeskLastDateStr = (String) mailMap.get("helpDeskLastDate");
				Date helpDeskLastDate = PdksUtil.getDateFromString(helpDeskLastDateStr);
				if (helpDeskLastDate == null)
					helpDeskLastDate = PdksUtil.getDateFromString(PdksUtil.getDecodeStringByBase64(helpDeskLastDateStr));
				if (helpDeskLastDate != null)
					sistemDestekVar = PdksUtil.tarihKarsilastirNumeric(new Date(), helpDeskLastDate) != 1;
			}
			if (!sistemDestekVar) {
				for (Iterator iterator = helpDeskList.iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					mailMap.remove(key);
				}
			}
			helpDeskList = null;

			if (PdksUtil.getTestSunucuDurum() || PdksUtil.getCanliSunucuDurum()) {
				try {
					List<String> strList = PdksUtil.getListByString((String) mailMap.get("serverTimeUpdateFromDB"), "|");
					StringBuffer sb = new StringBuffer();
					sb.append(strList.get(0));
					fields.clear();
					List list = pdksDAO.getNativeSQLList(fields, sb, null);
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

			PdksUtil.setSistemBaslangicYili(mailMap.containsKey("sistemBaslangicYili") ? Integer.parseInt((String) mailMap.get("sistemBaslangicYili")) : 2010);
			PdksUtil.setSistemDestekVar(sistemDestekVar);

			setHelpDeskParametre(pmMap, dao);
			pmMap = null;

		}
		return mailMap;

	}

	/**
	 * @param dao
	 * @return
	 */
	public static User getSistemAdminUser(PdksDAO dao) {
		if (dao == null)
			dao = Constants.pdksDAO;
		// try {
		// Departman departman = (Departman) dao.getObjectByInnerObject("id", 1L, Departman.class);
		// if (departman != null) {
		// Sirket sirket = (Sirket) dao.getObjectByInnerObject("id", 2L, Sirket.class);
		// if (sirket != null)
		// System.out.println(sirket.getAd());
		// }
		//
		// } catch (Exception e) {
		// System.err.println(e);
		// e.printStackTrace();
		// }
		User user = null;
		try {
			LinkedHashMap<String, Object> fields = new LinkedHashMap<String, Object>();
			fields.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_GET_SISTEM_ADMIN_LIST");
			fields.put("TIP", "U");
			List<User> list = dao.execSPList(fields, User.class);
			if (list != null && !list.isEmpty())
				user = list.get(0);
			list = null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return user;
	}

	/**
	 * @param pmMap
	 * @param dao
	 */
	private void setHelpDeskParametre(HashMap<String, Parameter> pmMap, PdksDAO dao) {
		Parameter helpDeskStatus = pmMap != null && pmMap.containsKey(HELP_DESK_STATUS) ? pmMap.get(HELP_DESK_STATUS) : new Parameter();
		bugun = new Date();
		if (helpDeskStatus.getId() == null) {
			Date changeDate = null;
			try {
				changeDate = PdksUtil.convertToJavaDate(PdksUtil.getSistemBaslangicYili() + "0101", "yyyyMMdd");
			} catch (Exception e) {
			}
			if (islemYapan == null)
				islemYapan = getSistemAdminUser(dao);
			helpDeskStatus.setChangeDate(changeDate != null ? changeDate : bugun);
			helpDeskStatus.setChangeUser(islemYapan);
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
			dao.saveObject(helpDeskStatus);
		}
	}

	/**
	 * @param fonksiyonAdi
	 * @param object
	 * @throws Exception
	 */
	private void saveFonksiyonVeri(String fonksiyonAdi, Object object) throws Exception {
		logger.info(fonksiyonAdi + " " + erpVeriOku + " " + PdksUtil.getCurrentTimeStampStr());
		if (!erpVeriOku) {
			Gson gson = new Gson();
			String dataStr = object != null ? gson.toJson(object) : "";
			if (PdksUtil.hasStringValue(dataStr)) {
				if (serviceData == null) {
					if (fonksiyonAdi != null) {
						serviceData = new ServiceData(fonksiyonAdi);
						serviceData.setInputData(dataStr);
					}
				} else if (serviceData.getId() != null)
					serviceData.setOutputData(dataStr);
				try {
					// if (serviceData != null)
					// pdksDAO.saveObject(serviceData);
				} catch (Exception e) {
				}
			}
		}

	}

	/**
	 * @param list
	 * @param mailMap
	 * @throws Exception
	 */
	private void mailListKontrol(List<MailPersonel> list, HashMap<String, MailPersonel> dataMap) throws Exception {
		if (list != null && mailMap != null) {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				MailPersonel mailPersonel = (MailPersonel) iterator.next();
				if (dataMap.containsKey(mailPersonel.getePosta())) {
					iterator.remove();
				} else
					dataMap.put(mailPersonel.getePosta(), mailPersonel);
			}

		}

	}

	/**
	 * @param list
	 * @param userMap
	 * @throws Exception
	 */
	private void mailUserListKontrol(List<MailPersonel> list, TreeMap<String, User> userMap) throws Exception {
		for (Iterator iterator = list.iterator(); iterator.hasNext();) {
			MailPersonel mailPersonel = (MailPersonel) iterator.next();
			if (userMap.containsKey(mailPersonel.getePosta()))
				mailPersonel.setAdiSoyadi(userMap.get(mailPersonel.getePosta()).getAdSoyad());

		}
	}

	/**
	 * @param list
	 * @param pasifList
	 * @param sb
	 * @throws Exception
	 */
	private void pasifListKontrol(List<MailPersonel> list, List<String> pasifList, StringBuffer sb) throws Exception {
		if (sb != null && list != null && pasifList != null) {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				MailPersonel mailPersonel = (MailPersonel) iterator.next();
				if (pasifList.contains(mailPersonel.getePosta())) {
					if (sb.length() > 0)
						sb.append(", ");
					sb.append((PdksUtil.hasStringValue(mailPersonel.getAdiSoyadi()) ? "<" + mailPersonel.getAdiSoyadi().trim() + "> " : "") + mailPersonel.getePosta());
					iterator.remove();
				}
			}
		}
	}

	/**
	 * @param mailObject
	 * @return
	 * @throws Exception
	 */
	public MailStatu sendMail(MailObject mailObject) throws Exception {
		MailStatu mailStatu = new MailStatu();
		if (pdksDAO != null && mailObject != null) {
			String subject = mailObject.getSubject() != null ? PdksUtil.setTurkishStr(mailObject.getSubject()) : null;
			if (subject != null)
				logger.info(subject + " in " + PdksUtil.getCurrentTimeStampStr());
			StringBuffer sb = new StringBuffer();
			if (!PdksUtil.hasStringValue(mailObject.getSmtpUser()))
				sb.append("Mail user belirtiniz!");
			if (PdksUtil.hasStringValue(mailObject.getSmtpPassword()))
				sb.append("Mail şifre belirtiniz!");

			if (PdksUtil.hasStringValue(mailObject.getSubject()))
				sb.append("Konu belirtiniz!");
			if (sb.length() > 0)
				mailStatu.setHataMesai(sb.toString());
			else {
				sistemVerileriniYukle(pdksDAO, true);
				StringBuffer pasifPersonelSB = new StringBuffer();
				String smtpUserName = mailMap.containsKey("smtpUserName") ? (String) mailMap.get("smtpUserName") : "";
				String smtpPassword = mailMap.containsKey("smtpPassword") ? (String) mailMap.get("smtpPassword") : "";
				if (mailObject.getSmtpUser().equals(smtpUserName) && mailObject.getSmtpPassword().equals(smtpPassword)) {
					mailMap.put("mailObject", mailObject);

					// mailAdresKontrol(mailObject, pasifPersonelSB);
					String body = mailObject.getBody();
					if (mailObject.getToList().size() == 1) {
						MailPersonel mailPersonel = mailObject.getToList().get(0);
						if (!body.contains(mailPersonel.getAdiSoyadi())) {
							body = "<P>Sayın " + mailPersonel.getAdiSoyadi() + ",</P>" + body;
							mailObject.setBody(body);
						}
					}
					if (!body.contains("Saygılarımla")) {
						body = body + "<P>Saygılarımla</P>";
						mailObject.setBody(body);
					}

					if (mailObject.getBccList().size() + mailObject.getCcList().size() + mailObject.getToList().size() > 0) {
						MailManager.ePostaGonder(mailMap);
						mailStatu.setDurum(Boolean.TRUE);
						mailStatu.setHataMesai(pasifPersonelSB.toString());
					} else {
						mailStatu.setHataMesai("Adres giriniz!");
					}

				} else {
					mailStatu.setHataMesai("Smtp bilgileri hatalıdır!");
				}
				pasifPersonelSB = null;
			}
			sb = null;

			if (subject != null)
				logger.info(subject + " out " + PdksUtil.getCurrentTimeStampStr());
		} else
			mailStatu.setHataMesai("Boş veri geldi!");
		return mailStatu;

	}

	/**
	 * // * @param mailObject // * @param bccAdresName //
	 */
	// private void addMailAdresBCC(MailObject mailObject, String bccAdresName) {
	// if (mailObject != null && mailMap.containsKey(bccAdresName)) {
	// String bccAdres = (String) mailMap.get(bccAdresName);
	// if (bccAdres.indexOf("@") > 1) {
	// List<String> list = PdksUtil.getListByString(bccAdres, null);
	// for (String email : list) {
	// if (email.indexOf("@") > 1 && PdksUtil.isValidEmail(email)) {
	// MailPersonel mailPersonel = new MailPersonel();
	// mailPersonel.setePosta(email);
	// mailObject.getBccList().add(mailPersonel);
	// }
	//
	// }
	// }
	// }
	// }

	/**
	 * @param mailObject
	 * @param pasifPersonelSB
	 * @throws Exception
	 */
	protected void mailAdresKontrol(MailObject mailObject, StringBuffer pasifPersonelSB) throws Exception {
		if (PdksUtil.isSistemDestekVar()) {
			MailManager.addMailAdresBCC(mailObject, "bccAdres", mailMap);
			MailManager.addMailAdresBCC(mailObject, "bccEntegrasyonAdres", mailMap);
		}
		HashMap<String, MailPersonel> mailDataMap = new HashMap<String, MailPersonel>();
		mailListKontrol(mailObject.getToList(), mailDataMap);
		mailListKontrol(mailObject.getCcList(), mailDataMap);
		mailListKontrol(mailObject.getBccList(), mailDataMap);
		if (!mailDataMap.isEmpty()) {
			List<String> list = new ArrayList<String>();
			for (String string : mailDataMap.keySet()) {
				// if (mailDataMap.size() > 1)
				// list.add("'" + string + "'");
				// else
				list.add(string);
			}
			HashMap map = new HashMap();
			map.put("email", list.size() > 1 ? list : list.get(0));
			TreeMap<String, User> userMap = new TreeMap<String, User>();
			List<User> userList = pdksDAO.getObjectByInnerObjectList(map, User.class);
			List<String> pasifList = new ArrayList<String>();
			for (User user : userList) {
				if (user.isDurum() && user.getPdksPersonel().isCalisiyor())
					userMap.put(user.getEmail(), user);
				else
					pasifList.add(user.getEmail());
			}
			if (!userMap.isEmpty()) {
				mailUserListKontrol(mailObject.getToList(), userMap);
				mailUserListKontrol(mailObject.getCcList(), userMap);
				mailUserListKontrol(mailObject.getBccList(), userMap);
			}
			if (!pasifList.isEmpty()) {
				pasifListKontrol(mailObject.getToList(), pasifList, pasifPersonelSB);
				pasifListKontrol(mailObject.getCcList(), pasifList, pasifPersonelSB);
				pasifListKontrol(mailObject.getBccList(), pasifList, pasifPersonelSB);
			}
			userList = null;
			list = null;
			userMap = null;
		}
	}

	/**
	 * @param sirketKodu
	 * @param personelNo
	 * @param basTarih
	 * @param bitTarih
	 * @return
	 * @throws Exception
	 */
	public List<PersonelERP> getERPPersonelList(String sirketKodu, String personelNo, String basTarih, String bitTarih) throws Exception {
		return null;
	}

	/**
	 * @param mesaiIdList
	 * @return
	 * @throws Exception
	 */
	public HashMap<String, Object> getFazlaMesaiList(List<Long> mesaiIdList) throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		if (mesaiIdList != null && !mesaiIdList.isEmpty()) {
			map = new HashMap<String, Object>();
			List<PersonelDenklestirme> list = null;
			try {
				list = pdksDAO.getObjectByInnerObjectList("id", mesaiIdList, PersonelDenklestirme.class);

			} catch (Exception e) {
				list = null;
			}
			if (list != null && !list.isEmpty()) {
				int sira = 0;
				HashMap<String, String> mesaiKodMap = new HashMap<String, String>();
				// List<Tanim> tipList = pdksDAO.getObjectByInnerObjectList("tipi", Tanim.TIPI_ERP_FAZLA_MESAI, Tanim.class);
				List<Tanim> tipList = getSQLTanimList(Tanim.TIPI_ERP_FAZLA_MESAI, null, null);
				for (Tanim tanim : tipList)
					mesaiKodMap.put(tanim.getKodu(), tanim.getErpKodu());
				DenklestirmeAy denklestirmeAy = null;
				TreeMap<String, String> veriMap = new TreeMap<String, String>();
				for (PersonelDenklestirme personelDenklestirme : list) {
					Personel personel = personelDenklestirme.getPersonel();
					if (personel.getSanalPersonel())
						continue;
					denklestirmeAy = personelDenklestirme.getDenklestirmeAy();
					Sirket sirket = personel.getSirket();
					String basString = personel.getPdksSicilNo() + ";" + sirket.getErpKodu() + ";" + (personel.getBordroAltAlan() != null ? personel.getBordroAltAlan().getErpKodu() : "") + ";NORM;" + denklestirmeAy.getYil() + ";" + denklestirmeAy.getAy() + ";" + (++sira);
					String key = sirket.getErpKodu() + "_" + personel.getPdksSicilNo() + "_";
					satirEkle(veriMap, key, basString, "UO", personelDenklestirme.getOdenecekSure(), mesaiKodMap);
					satirEkle(veriMap, key, basString, "RT", personelDenklestirme.getResmiTatilSure(), mesaiKodMap);
					satirEkle(veriMap, key, basString, "A", personelDenklestirme.getAksamVardiyaSayisi(), mesaiKodMap);
					satirEkle(veriMap, key, basString, "AS", personelDenklestirme.getAksamVardiyaSaatSayisi(), mesaiKodMap);
					satirEkle(veriMap, key, basString, "HT", personelDenklestirme.getHaftaCalismaSuresi(), mesaiKodMap);
				}
				if (!veriMap.isEmpty()) {
					StringBuilder sb = new StringBuilder();
					for (Iterator iterator = veriMap.keySet().iterator(); iterator.hasNext();) {
						String key = (String) iterator.next();
						String str = veriMap.get(key);
						sb.append(str);
						if (iterator.hasNext())
							sb.append("\n");
					}
					map.put("dosyaAdi", "dosya_" + denklestirmeAy.getYil() + "_" + denklestirmeAy.getAyAdi() + ".csv");
					map.put("contentType", "application/csv");
					map.put("content", sb.toString());
				}
				veriMap = null;
			}
		}
		return map;
	}

	/**
	 * @param veriMap
	 * @param startKey
	 * @param basString
	 * @param mesaiKodu
	 * @param tutar
	 * @param mesaiKodMap
	 */
	private void satirEkle(TreeMap<String, String> veriMap, String startKey, String basString, String mesaiKodu, Double tutar, HashMap<String, String> mesaiKodMap) {
		if (tutar != null && tutar.doubleValue() > 0.0d && mesaiKodMap.containsKey(mesaiKodu)) {
			String mesaiERPKodu = mesaiKodMap.get(mesaiKodu);
			String key = startKey + mesaiERPKodu + "_" + PdksUtil.textBaslangicinaKarakterEkle("" + (veriMap.size() + 1), '0', 8);
			veriMap.put(key, basString + ";" + mesaiERPKodu + ";" + tutar);
		}
	}

	/**
	 * @param mesaiIdList
	 * @return
	 * @throws Exception
	 */
	public List<MesaiPDKS> sendFazlaMesaiList(List<Long> mesaiIdList) throws Exception {
		List<MesaiPDKS> mesaiERPList = null;
		if (mesaiIdList != null && !mesaiIdList.isEmpty()) {
			List<PersonelDenklestirme> list = null;
			try {
				list = pdksDAO.getObjectByInnerObjectList("id", mesaiIdList, PersonelDenklestirme.class);
			} catch (Exception e) {
				list = null;
			}
			if (list != null && !list.isEmpty()) {
				HashMap<String, String> mesaiKodMap = new HashMap<String, String>();
				// List<Tanim> tipList = pdksDAO.getObjectByInnerObjectList("tipi", Tanim.TIPI_ERP_FAZLA_MESAI, Tanim.class);
				List<Tanim> tipList = getSQLTanimList(Tanim.TIPI_ERP_FAZLA_MESAI, null, null);
				for (Tanim tanim : tipList) {
					mesaiKodMap.put(tanim.getKodu(), tanim.getErpKodu());
				}
				TreeMap<String, MesaiPDKS> mesaiMap = new TreeMap<String, MesaiPDKS>();
				for (PersonelDenklestirme personelDenklestirme : list) {
					Personel personel = personelDenklestirme.getPersonel();
					if (personel.getSanalPersonel())
						continue;
					DenklestirmeAy denklestirmeAy = personelDenklestirme.getDenklestirmeAy();
					MesaiPDKS mesaiPDKS = new MesaiPDKS();
					mesaiPDKS.setYil(denklestirmeAy.getYil());
					mesaiPDKS.setAy(denklestirmeAy.getAy());
					mesaiPDKS.setPersonelNo(personel.getPdksSicilNo());
					mesaiPDKS.setSirketKodu(personel.getSirket() != null ? personel.getSirket().getErpKodu() : null);
					mesaiPDKS.setMasrafYeriKodu(personel.getMasrafYeri() == null ? personel.getMasrafYeri().getErpKodu() : null);
					mesaiPDKS.setTesisKodu(personel.getTesis() != null ? personel.getTesis().getErpKodu() : null);
					mesaiEkle(mesaiMap, mesaiPDKS, "UO", personelDenklestirme.getOdenecekSure(), mesaiKodMap);
					mesaiEkle(mesaiMap, mesaiPDKS, "HT", personelDenklestirme.getHaftaCalismaSuresi(), mesaiKodMap);
					mesaiEkle(mesaiMap, mesaiPDKS, "A", personelDenklestirme.getAksamVardiyaSayisi(), mesaiKodMap);
					mesaiEkle(mesaiMap, mesaiPDKS, "AS", personelDenklestirme.getAksamVardiyaSaatSayisi(), mesaiKodMap);
					mesaiEkle(mesaiMap, mesaiPDKS, "RT", personelDenklestirme.getResmiTatilSure(), mesaiKodMap);
				}
				if (!mesaiMap.isEmpty())
					mesaiERPList = new ArrayList<MesaiPDKS>(mesaiMap.values());
			}

		}
		if (mesaiERPList == null)
			mesaiERPList = new ArrayList<MesaiPDKS>();
		return mesaiERPList;
	}

	/**
	 * @param mesaiMap
	 * @param mesaiPDKSBase
	 * @param mesaiKodu
	 * @param tutar
	 * @param mesaiKodMap
	 */
	private void mesaiEkle(TreeMap<String, MesaiPDKS> mesaiMap, MesaiPDKS mesaiPDKSBase, String mesaiKodu, Double tutar, HashMap<String, String> mesaiKodMap) {
		if (tutar != null && tutar.doubleValue() > 0.0d && mesaiKodMap.containsKey(mesaiKodu)) {
			MesaiPDKS mesaiPDKS = (MesaiPDKS) mesaiPDKSBase.clone();
			String mesaiERPKodu = mesaiKodMap.get(mesaiKodu);
			mesaiPDKS.setMesaiKodu(mesaiERPKodu);
			String key = mesaiPDKSBase.getSirketKodu() + "_" + mesaiPDKSBase.getPersonelNo() + "_" + mesaiERPKodu + "_" + PdksUtil.textBaslangicinaKarakterEkle("" + (mesaiMap.size() + 1), '0', 8);
			mesaiPDKS.setToplamSure(tutar);
			mesaiMap.put(key, mesaiPDKS);
		}
	}

	/**
	 * @param izinList
	 * @return
	 * @throws Exception
	 */
	public List<IzinERP> sendERPIzinler(List<IzinERP> izinList) throws Exception {
		return null;
	}

	/**
	 * @param msg
	 */
	private void mesajInfoYaz(String msg) {
		if (PdksUtil.hasStringValue(msg))
			logger.info(msg);
	}

	/**
	 * @param sirketKodu
	 * @param yil
	 * @param ay
	 * @param donemKapat
	 * @return
	 * @throws Exception
	 */
	public List<MesaiPDKS> getMesaiPDKS(String sirketKodu, Integer yil, Integer ay, Boolean donemKapat) throws Exception {
		sistemVerileriniYukle(pdksDAO, true);
		boolean servisDurum = !PdksUtil.getCanliSunucuDurum() || !(mailMap.containsKey("getMesaiPDKSDurum") && mailMap.get("getMesaiPDKSDurum").equals("0"));
		List<MesaiPDKS> list = null;
		if (servisDurum && pdksDAO != null) {
			LinkedHashMap<String, Object> inputMap = new LinkedHashMap<String, Object>();
			if (sirketKodu != null)
				inputMap.put("sirketKodu", sirketKodu);
			if (yil != null)
				inputMap.put("yil", yil);
			if (ay != null)
				inputMap.put("ay", ay);
			if (donemKapat != null)
				inputMap.put("donemKapat", donemKapat);
			saveFonksiyonVeri("getMesaiPDKS", inputMap);

			dosyaEkAdi = (PdksUtil.hasStringValue(sirketKodu) ? sirketKodu.trim() : "") + "-" + ((yil != null ? yil : 0) * 100 + (ay != null ? +ay : 0));

			fields.clear();
			fields.put("yil", yil);
			fields.put("ay", ay);
			DenklestirmeAy denklestirmeAy = (DenklestirmeAy) pdksDAO.getObjectByInnerObject(fields, DenklestirmeAy.class);
			if (denklestirmeAy != null) {
				Sirket sirket = null;
				if (PdksUtil.hasStringValue(sirketKodu)) {
					sirket = (Sirket) pdksDAO.getObjectByInnerObject("erpKodu", sirketKodu, Sirket.class);
					if (sirket == null)
						denklestirmeAy = null;
				}
				if (denklestirmeAy != null) {
					mesaj = yil + "-" + ay + (sirket != null ? " " + sirket.getAd() : "");
					mesajInfoYaz("getMesaiPDKS --> " + mesaj + " in " + PdksUtil.getCurrentTimeStampStr());
					LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
					veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_GET_FAZLA_MESAI");
					veriMap.put("sirketId", sirket != null ? sirket.getId() : 0L);
					veriMap.put("yil", denklestirmeAy.getYil());
					veriMap.put("ay", denklestirmeAy.getAy());
					try {
						List<String> perNoList = null;
						List<PersonelMesai> personelMesaiList = pdksDAO.execSPList(veriMap, PersonelMesai.class);
						if (donemKapat != null && donemKapat.booleanValue() && !personelMesaiList.isEmpty())
							perNoList = new ArrayList<String>();
						list = new ArrayList<MesaiPDKS>();
						for (Iterator iterator = personelMesaiList.iterator(); iterator.hasNext();) {
							PersonelMesai personelMesai = (PersonelMesai) iterator.next();
							Personel personel = personelMesai.getPersonel();
							MesaiPDKS mesaiPDKS = new MesaiPDKS();
							mesaiPDKS.setYil(yil);
							mesaiPDKS.setAy(ay);
							mesaiPDKS.setPersonelNo(personel.getPdksSicilNo());
							mesaiPDKS.setMesaiKodu(personelMesai.getErpKodu());
							mesaiPDKS.setToplamSure(personelMesai.getSure());
							mesaiPDKS.setSirketKodu(personel.getSirket() != null ? personel.getSirket().getErpKodu() : null);
							mesaiPDKS.setMasrafYeriKodu(personel.getMasrafYeri() != null ? personel.getMasrafYeri().getErpKodu() : null);
							mesaiPDKS.setTesisKodu(personel.getTesis() != null ? personel.getTesis().getErpKodu() : null);
							list.add(mesaiPDKS);
							if (perNoList != null && !perNoList.contains(mesaiPDKS.getPersonelNo()))
								perNoList.add(mesaiPDKS.getPersonelNo());
						}
						if (perNoList != null && !perNoList.isEmpty()) {
							String fieldName = "p";
							HashMap fields = new HashMap();
							StringBuffer sb = new StringBuffer();
							sb.append("select D.* from " + PersonelDenklestirme.TABLE_NAME + " D " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
							sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = D." + PersonelDenklestirme.COLUMN_NAME_PERSONEL);
							sb.append(" and P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);
							sb.append(" where D." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = " + denklestirmeAy.getId());
							sb.append(" and (D.ERP_AKTARILDI is null or D.ERP_AKTARILDI<>1)");
							fields.put(fieldName, perNoList);
							List<PersonelDenklestirme> personelDenklestirmeList = getNativeSQLParamList(perNoList, sb, fieldName, fields, PersonelDenklestirme.class);
							// pdksDAO.getNativeSQLList(fields, sb, PersonelDenklestirme.class);
							if (!personelDenklestirmeList.isEmpty()) {
								Date guncellemeTarihi = new Date();
								for (PersonelDenklestirme personelDenklestirme : personelDenklestirmeList) {
									personelDenklestirme.setErpAktarildi(Boolean.TRUE);
									personelDenklestirme.setGuncellemeTarihi(guncellemeTarihi);
								}
								pdksDAO.saveObjectList(personelDenklestirmeList);
							}
						}
					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}
					mesajInfoYaz("getMesaiPDKS --> " + mesaj + " out " + PdksUtil.getCurrentTimeStampStr());
					try {

						mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
						MailObject mailObject = kullaniciIKYukle(null, mailMap, pdksDAO);
						String dosyaAdi = PdksUtil.setTurkishStr("FazlaMesai_" + +denklestirmeAy.getYil() + " " + denklestirmeAy.getAyAdi() + (sirket != null ? "_" + sirket.getAd() : "")) + ".xml";
						String subject = uygulamaBordro + " " + denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil() + " " + (sirket != null ? sirket.getAd() + " " : "") + "fazla mesai yükleme";
						String body = denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil() + " dönemi " + (sirket != null ? sirket.getAd() + " " : "") + " fazla mesai dosyası " + dosyaAdi + " ektedir.";
						mailObject.setSubject(subject);
						mailObject.setBody(body);
						LinkedHashMap dataMap = new LinkedHashMap(), dataInputMap = new LinkedHashMap();
						dataInputMap.put("sirketKodu", sirketKodu);
						dataInputMap.put("yil", yil);
						dataInputMap.put("ay", ay);
						dataInputMap.put("donemKapat", donemKapat);
						dataMap.put("input", dataInputMap);
						if (!list.isEmpty())
							dataMap.put("getMesaiPDKSReturn", list);
						else
							dataMap.put("mesaj", denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil() + " dönemi " + (denklestirmeAy.getDurum() ? " ait kayıt bulunamadı!" : " kapatılmıştır!"));
						// mailAdresKontrol(mailObject, null);
						Gson gs = new Gson();
						String xml = PdksUtil.getJsonToXML(gs.toJson(dataMap), "getMesaiPDKS", null);
						MailFile mailFile = new MailFile();
						mailFile.setDisplayName(dosyaAdi);
						mailFile.setIcerik(PdksUtil.getBytesUTF8(xml));
						mailObject.getAttachmentFiles().add(mailFile);
						mailMap.put("mailObject", mailObject);
						MailManager.ePostaGonder(mailMap);
						gs = null;
						mailObject = null;

					} catch (Exception e) {
						logger.error(e);
						e.printStackTrace();
					}

				}
			}
			if (list == null)
				list = new ArrayList<MesaiPDKS>();

			saveFonksiyonVeri(null, list);
		} else {
			throw new Exception(servisAdi + " servisi  kapalıdır!");
		}
		return list;
	}

	/**
	 * @param oldName
	 * @param newName
	 */
	private void mailMapGuncelle(String oldName, String newName) {
		try {
			if (mailMap != null && mailMap.containsKey(oldName)) {
				mailMap.put(newName, mailMap.get(oldName));
				mailMap.remove(oldName);
			}
		} catch (Exception e) {
		}

	}

	/**
	 * @param izinHakedisList
	 * @throws Exception
	 */
	public void saveHakedisIzinler(List<IzinHakedis> izinHakedisList) throws Exception {
		mesaj = "";
		if (pdksDAO != null && izinHakedisList != null && !izinHakedisList.isEmpty()) {
			bugun = PdksUtil.getDate(new Date());
			if (izinHakedisList.size() == 1)
				mesaj = izinHakedisList.get(0).getPersonelNo();
			sistemVerileriniYukle(pdksDAO, true);
			setErpVeriOku(mailMap == null || mailMap.containsKey(getParametreHakEdisIzinERPTableView()));
			erpVeriOkuSaveHakedisIzinler = erpVeriOku;
			setSicilNoUzunluk();

			saveFonksiyonVeri("saveHakedisIzinler", izinHakedisList);
			String personelNoAciklama = personelNoAciklama();
			LinkedHashMap<String, Object> izinKeyAciklamaMap = new LinkedHashMap<String, Object>();
			LinkedHashMap<String, String> izinKeyPersonelMap = new LinkedHashMap<String, String>();
			List<IzinHakedis> izinHakedisHataList = new ArrayList<IzinHakedis>();
			List<IzinERP> izinList = new ArrayList<IzinERP>();
			for (Iterator iterator1 = izinHakedisList.iterator(); iterator1.hasNext();) {
				IzinHakedis izinHakedis = (IzinHakedis) iterator1.next();
				if (izinHakedis.getHakedisList() == null || izinHakedis.getHakedisList().isEmpty()) {
					addHatalist(izinHakedis.getHataList(), "Hakediş veri yoktur!");
					izinHakedisHataList.add(izinHakedis);
					iterator1.remove();
					continue;
				}
				String perHakedisNo = PdksUtil.textBaslangicinaKarakterEkle(izinHakedis.getPersonelNo(), '0', sicilNoUzunluk);
				// izinKeyPersonelMap.put(perHakedisNo, "'" + perHakedisNo + "'");
				izinKeyPersonelMap.put(perHakedisNo, perHakedisNo);
				for (Iterator iterator = izinHakedis.getHakedisList().iterator(); iterator.hasNext();) {
					IzinHakedisDetay detay = (IzinHakedisDetay) iterator.next();
					List<IzinERP> kullanilanIzinler = detay.getKullanilanIzinler();
					if (kullanilanIzinler == null || kullanilanIzinler.isEmpty())
						continue;
					izinList.addAll(kullanilanIzinler);
					for (IzinERP izin : kullanilanIzinler) {
						String perNo = PdksUtil.textBaslangicinaKarakterEkle(izin.getPersonelNo(), '0', sicilNoUzunluk);
						if (!izinKeyPersonelMap.containsKey(perNo)) {
							// izinKeyPersonelMap.put(perNo, "'" + perNo + "'");
							izinKeyPersonelMap.put(perNo, perNo);
						}

						izinKeyAciklamaMap.put(izin.getReferansNoERP(), izin);
					}
				}

			}
			saveIzinler(izinList);
			if (!izinKeyPersonelMap.isEmpty()) {
				fields.clear();
				fields.put("departman.admin=", Boolean.TRUE);
				fields.put("izinTipiTanim.kodu=", IzinTipi.YILLIK_UCRETLI_IZIN);
				fields.put("durum=", Boolean.TRUE);
				List<IzinTipi> izinTipleri = pdksDAO.getObjectByInnerObjectListInLogic(fields, IzinTipi.class);
				IzinTipi hakedisIzinTipi = null;
				for (IzinTipi izinTipi : izinTipleri) {
					if (izinTipi.getBakiyeIzinTipi() != null)
						hakedisIzinTipi = izinTipi;
				}
				String fieldName = "izinSahibi.pdksSicilNo";
				List<String> perNoList = new ArrayList(izinKeyPersonelMap.values());
				fields.clear();
				fields.put("izinTipi.id=", hakedisIzinTipi.getId());
				fields.put(fieldName, perNoList);
				TreeMap<String, PersonelIzin> hakedisIzinMap = getSQLParamMap("getDonemKey", true, perNoList, fieldName, fields, true, PersonelIzin.class);

				TreeMap<String, Personel> personelMap = getParamListMap("getPdksSicilNo", "pdksSicilNo", perNoList, Personel.class, false);
				List<String> list = new ArrayList<String>();
				for (String string : izinKeyAciklamaMap.keySet()) {
					// list.add("'" + string + "'");
					list.add(string);
				}

				TreeMap<String, IzinReferansERP> izinMap = getParamListMap("getId", "id", list, IzinReferansERP.class, false);
				List<Long> idList = new ArrayList<Long>();
				for (String key : izinMap.keySet())
					idList.add(izinMap.get(key).getIzin().getId());
				TreeMap<Long, PersonelIzinDetay> izinDetayMap = new TreeMap<Long, PersonelIzinDetay>();
				if (!idList.isEmpty())
					izinDetayMap = getParamListMap("getPersonelIzinId", "personelIzin.id", idList, PersonelIzinDetay.class, false);

				list = null;

				List saveList = new ArrayList();
				Calendar cal = Calendar.getInstance();
				idList.clear();

				if (hakedisIzinTipi != null) {
					for (IzinHakedis izinHakedis : izinHakedisList) {
						String perHakedisNo = PdksUtil.textBaslangicinaKarakterEkle(izinHakedis.getPersonelNo(), '0', sicilNoUzunluk);
						if (personelMap.containsKey(perHakedisNo)) {
							Long id = personelMap.get(perHakedisNo).getId();
							if (!idList.contains(id))
								idList.add(id);
						}
					}
				}
				String erpEk = "_ERP";
				for (IzinHakedis izinHakedis : izinHakedisList) {
					Boolean izinHakedisYazildi = true;
					if (hakedisIzinTipi == null) {
						addHatalist(izinHakedis.getHataList(), "Hakediş izin tipi tanımsız!");
						izinHakedisHataList.add(izinHakedis);
						continue;
					}
					String perHakedisNo = PdksUtil.textBaslangicinaKarakterEkle(izinHakedis.getPersonelNo(), '0', sicilNoUzunluk);
					if (!personelMap.containsKey(perHakedisNo)) {
						addHatalist(izinHakedis.getHataList(), perHakedisNo + " personel veri yoktur!");
						izinHakedisHataList.add(izinHakedis);
						continue;
					}
					saveList.clear();
					Personel izinHakedisPersonel = personelMap.get(perHakedisNo);
					izinHakedisPersonel.setDegisti(false);
					Date izinHakEdisTarihi = getTarih(izinHakedis.getKidemBaslangicTarihi(), FORMAT_DATE);
					izinHakedisPersonel.setIzinHakEdisTarihi(izinHakEdisTarihi);
					if (izinHakedisPersonel.isDegisti()) {
						izinHakedisPersonel.setGuncellemeTarihi(new Date());
						izinHakedisPersonel.setGuncelleyenUser(islemYapan);
						saveList.add(izinHakedisPersonel);
					}
					TreeMap<String, Object> izinHakedisERPMap = new TreeMap<String, Object>();
					TreeMap<String, PersonelIzin> izinHakedisMap = new TreeMap<String, PersonelIzin>();
					for (Iterator iterator = izinHakedis.getHakedisList().iterator(); iterator.hasNext();) {
						IzinHakedisDetay detay = (IzinHakedisDetay) iterator.next();
						String id = PdksUtil.textBaslangicinaKarakterEkle(izinHakedis.getPersonelNo(), '0', sicilNoUzunluk) + "_" + detay.getHakEdisTarihi().substring(0, 4);
						PersonelIzin hakedisPersonelIzin = null;
						if (hakedisIzinMap.containsKey(id)) {
							hakedisPersonelIzin = hakedisIzinMap.get(id);
							detay.setId(hakedisPersonelIzin.getId());
						} else {
							hakedisPersonelIzin = new PersonelIzin();
							izinHakedisERPMap.put(id, hakedisPersonelIzin);
							izinHakedisERPMap.put(id + erpEk, detay);
							hakedisPersonelIzin.setDegisti(true);
							hakedisPersonelIzin.setIzinTipi(hakedisIzinTipi);
							hakedisPersonelIzin.setIzinSahibi(izinHakedisPersonel);
							hakedisPersonelIzin.setHesapTipi(hakedisIzinTipi.getHesapTipi());
						}
						hakedisPersonelIzin.setDegisti(hakedisPersonelIzin.getId() == null);

						Date hakEdisBitisZamani = getTarih(detay.getHakEdisTarihi(), FORMAT_DATE);
						if (hakEdisBitisZamani == null) {
							izinHakedisYazildi = false;
							if (detay.getHakEdisTarihi() == null || detay.getHakEdisTarihi().trim().length() < FORMAT_DATE.length())
								addHatalist(izinHakedis.getHataList(), "Hakediş tarihi boş veya " + FORMAT_DATE + " formatından farklıdır!");
							else
								addHatalist(izinHakedis.getHataList(), detay.getHakEdisTarihi().trim() + " hakediş tarihi " + FORMAT_DATE + " formatından farklıdır!");
							continue;
						}

						cal.setTime(hakEdisBitisZamani);
						String aciklama = detay.kidemYil > 1900 ? String.valueOf(detay.kidemYil) : "Devir Bakiye";
						Date hakEdisBaslangicZamani = getTarih(cal.get(Calendar.YEAR) + "-01-01", FORMAT_DATE);

						hakedisPersonelIzin.setBaslangicZamani(hakEdisBaslangicZamani);
						hakedisPersonelIzin.setBitisZamani(hakEdisBitisZamani);
						hakedisPersonelIzin.setIzinSuresi(detay.getIzinSuresi());
						hakedisPersonelIzin.setAciklama(aciklama);
						hakedisPersonelIzin.setIzinDurumu(detay.getIzinSuresi() > 0.0d ? PersonelIzin.IZIN_DURUMU_ONAYLANDI : PersonelIzin.IZIN_DURUMU_REDEDILDI);
						if (hakedisPersonelIzin.getId() == null) {
							hakedisPersonelIzin.setOlusturanUser(islemYapan);
							hakedisPersonelIzin.setOlusturmaTarihi(new Date());

						} else {
							hakedisPersonelIzin.setGuncellemeTarihi(new Date());
							hakedisPersonelIzin.setGuncelleyenUser(islemYapan);
						}

						if (hakedisPersonelIzin.isDegisti())

							saveList.add(hakedisPersonelIzin);

						List<IzinERP> kullanilanIzinler = detay.getKullanilanIzinler();
						if (kullanilanIzinler != null) {
							for (IzinERP izinERP : kullanilanIzinler) {
								IzinReferansERP izinReferansERP = izinMap.get(izinERP.getReferansNoERP());
								if (izinReferansERP == null)
									continue;
								PersonelIzin personelIzin = izinReferansERP.getIzin();
								if (!izinDetayMap.containsKey(personelIzin.getId())) {
									PersonelIzinDetay personelIzinDetay = new PersonelIzinDetay();
									personelIzinDetay.setPersonelIzin(personelIzin);
									personelIzinDetay.setHakEdisIzin(hakedisPersonelIzin);
									// personelIzinDetay.setIzinMiktari(izinERP.getIzinSuresi());
									saveList.add(personelIzinDetay);
								} else {
									PersonelIzinDetay personelIzinDetay = izinDetayMap.get(personelIzin.getId());
									personelIzinDetay.setHakEdisIzin(hakedisPersonelIzin);
									if (personelIzinDetay.getIzinMiktari() != izinERP.getIzinSuresi().doubleValue()) {
										// personelIzinDetay.setIzinMiktari(izinERP.getIzinSuresi());
										saveList.add(personelIzinDetay);
									}

								}

							}
						}
						if (detay.getHataList().isEmpty()) {
							detay.setYazildi(true);
							detay.setHataList(null);
						}
					}
					if (!izinHakedisYazildi)
						izinHakedisHataList.add(izinHakedis);
					izinHakedis.setYazildi(izinHakedisYazildi);
					if (izinHakedisYazildi || izinHakedis.getHataList().isEmpty())
						izinHakedis.setHataList(null);
					if (!saveList.isEmpty()) {
						try {
							pdksDAO.saveObjectList(saveList);
							for (String key : izinHakedisERPMap.keySet()) {
								if (!key.endsWith(erpEk)) {
									PersonelIzin personelIzin = (PersonelIzin) izinHakedisERPMap.get(key);
									IzinHakedisDetay detay = (IzinHakedisDetay) izinHakedisERPMap.get(key + erpEk);
									detay.setId(personelIzin.getId());
								}
							}
						} catch (Exception e) {
							logger.error(e);
						}
					} else if (izinHakedis.getHataList().isEmpty()) {
						izinHakedis.setYazildi(true);
						izinHakedis.setHataList(null);
					}
					for (String key : izinHakedisMap.keySet()) {
						PersonelIzin izin = izinHakedisMap.get(key);
						if (izin.getId() != null) {
							Object object = izinHakedisERPMap.get(key);
							if (object instanceof IzinERP) {
								IzinERP izinERP = (IzinERP) object;
								izinERP.setId(izin.getId());
								izinERP.setYazildi(true);
								izinERP.setHataList(null);
							} else if (object instanceof IzinHakedisDetay) {
								IzinHakedisDetay detay = (IzinHakedisDetay) object;
								detay.setId(izin.getId());
								detay.setYazildi(true);
								detay.setHataList(null);
							}
						}
					}
				}

				if (!izinDetayMap.isEmpty() || !saveList.isEmpty()) {
					try {
						pdksDAO.saveAndDeleteObjectList(saveList, new ArrayList(izinDetayMap.values()));
					} catch (Exception e) {
						logger.error(e);
					}
				}
				saveList = null;
				izinDetayMap = null;

				personelMap = null;
			}
			saveFonksiyonVeri(null, izinHakedisList);
			mesajInfoYaz("saveHakedisIzinler --> " + mesaj + " out " + PdksUtil.getCurrentTimeStampStr());
			bugun = PdksUtil.getDate(new Date());

			if (!izinHakedisHataList.isEmpty() && izinHakedisList != null && izinHakedisList.size() > 1) {
				TreeMap<String, Tatil> tatilMap = getTatilGunleri(PdksUtil.tariheGunEkleCikar(bugun, -5), bugun);
				if (tatilMap.containsKey(PdksUtil.convertToDateString(bugun, "yyyyMMdd")))
					izinHakedisHataList.clear();
			}
			if (!izinHakedisHataList.isEmpty()) {
				Gson gson = new Gson();
				String jsonStr = PdksUtil.toPrettyFormat(gson.toJson(izinHakedisHataList));
				StringBuffer sb = new StringBuffer();
				sb.append("<DIV>");
				for (IzinHakedis izinHakedis : izinHakedisHataList) {
					sb.append("<LABEL><b>" + personelNoAciklama + " : </b>" + izinHakedis.getPersonelNo() + "</LABEL>");
					sb.append("<TABLE style=\"border: solid 1px\" cellpadding=\"5\" cellspacing=\"0\">");
					if (!izinHakedis.getHataList().isEmpty()) {
						sb.append("<TR><TD colspan=3 align=center><u>Hatalar</u></TD></TR>");
						for (String string : izinHakedis.getHataList())
							sb.append("<TR><TD></TD><TD colspan=2><LI>" + string + "</LI></TD></TR>");
					}
					for (IzinHakedisDetay detay : izinHakedis.getHakedisList()) {
						sb.append("<TR><TD></TD><TD nowrap><b>Hakediş Tarihi</b></TD><TD nowrap>" + detay.getHakEdisTarihi() + "</TD></TR>");
						sb.append("<TR><TD></TD><TD nowrap><b>Kıdem Yıl</b></TD><TD nowrap>" + detay.getKidemYil() + "</TD></TR>");
						sb.append("<TR><TD></TD><TD nowrap><b>İzin Süresi</b></TD><TD nowrap>" + PdksUtil.numericValueFormatStr(detay.getIzinSuresi(), Constants.TR_LOCALE) + "</TD></TR>");
						if (!detay.getHataList().isEmpty()) {
							sb.append("<TR><TD></TD><TD colspan=2 align=center><u>Hatalar</u></TD></TR>");
							for (String string : detay.getHataList())
								sb.append("<TR><TD colspan=2></TD><TD><LI>" + string + "</LI></TD></TR>");
						}

						if (!detay.getKullanilanIzinler().isEmpty()) {
							sb.append("<TR><TD></TD><TD  colspan=2 align=center><b><u>Kullanılan İzinler</u></b></TD></TR>");
							for (IzinERP izinERP : detay.getKullanilanIzinler()) {
								sb.append("<TR><TD></TD><TD valign=top nowrap><b>Referans No</b></TD><TD valign=top>" + izinERP.getReferansNoERP() + "<DIV><TABLE>");
								if (!izinERP.getPersonelNo().equals(izinHakedis.getPersonelNo()))
									sb.append("<TR><TD nowrap><b>" + personelNoAciklama + "</b></TD><TD>" + izinERP.getPersonelNo() + "</TD></TR>");
								sb.append("<TR><TD nowrap><b>Başlangıç Zamanı</b></TD><TD>" + izinERP.getBasZaman() + "</TD></TR>");
								sb.append("<TR><TD nowrap><b>Bitiş Zamanı</b></TD><TD>" + izinERP.getBitZaman() + "</TD></TR>");
								sb.append("<TR><TD nowrap><b>İzin Tipi</b></TD><TD>" + izinERP.getIzinTipiAciklama() + "</TD></TR>");
								sb.append("<TR><TD nowrap><b>İzin Süresi</b></TD><TD>" + PdksUtil.numericValueFormatStr(izinERP.getIzinSuresi(), Constants.TR_LOCALE) + "</TD></TR>");

								sb.append("</TABLE></DIV></TD></TR>");
								if (!izinERP.getHataList().isEmpty()) {
									sb.append("<TR><TD></TD><TD colspan=2 align=center><u>Hatalar</u></TD></TR>");
									for (String string : izinERP.getHataList())
										sb.append("<TR><TD colspan=2></TD><TD><LI>" + string + "</LI></TD></TR>");
								}
							}
						}
					}

					sb.append("</TABLE>");
				}
				sb.append("</DIV>");
				mailMap.put("konu", uygulamaBordro + " saveIzinHakedisler servis hatası");
				mailMap.put("mailIcerik", sb.toString());
				LinkedHashMap<String, Object> fileMap = new LinkedHashMap<String, Object>();
				fileMap.put("saveIzinHakedisler.xml", PdksUtil.getJsonToXML(jsonStr, "saveIzinHakedisler", "izinHakedis"));
				mailMap.put("fileMap", fileMap);
				mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
				kullaniciIKYukle(null, mailMap, pdksDAO);
				MailManager.ePostaGonder(mailMap);

			}
		}

	}

	/**
	 * @param personeller
	 * @return
	 * @throws Exception
	 */
	public List<IzinHakedis> getIzinHakedisler(List<String> personeller) throws Exception {
		return null;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public boolean helpDeskStatus() throws Exception {
		if (pdksDAO != null)
			sistemVerileriniYukle(pdksDAO, true);
		return sistemDestekVar;
	}

	/**
	 * @return
	 * @throws Exception
	 */
	public String helpDeskDate() throws Exception {
		String helpDeskLastDateStr = "";
		if (pdksDAO != null)
			sistemVerileriniYukle(pdksDAO, true);
		if (mailMap.containsKey("helpDeskLastDate"))
			helpDeskLastDateStr = (String) mailMap.get("helpDeskLastDate");
		return helpDeskLastDateStr;
	}

	/**
	 * @param izinList
	 * @throws Exception
	 */
	public void saveIzinler(List<IzinERP> izinList) throws Exception {
		servisAdi = "saveIzinler";
		if (pdksDAO != null && izinList != null && !izinList.isEmpty()) {
			sistemVerileriniYukle(pdksDAO, true);
			setErpVeriOku(mailMap == null || mailMap.containsKey(getParametreIzinERPTableView()));
			erpVeriOkuSaveIzinler = erpVeriOku;
			boolean servisDurum = !PdksUtil.getCanliSunucuDurum() || !(mailMap.containsKey(servisAdi + "Durum") && mailMap.get(servisAdi + "Durum").equals("0"));
			if (servisDurum) {
				izinBilgileriniGuncelle(izinList);
			} else {
				for (IzinERP izinERP : izinList) {
					izinERP.getHataList().add(servisAdi + " servisi  kapalıdır!");
				}
			}
		}
	}

	/**
	 * @param izinList
	 * @throws Exception
	 */
	private void izinBilgileriniGuncelle(List<IzinERP> izinList) throws Exception {
		IzinERP erp = null;
		bugun = PdksUtil.getDate(new Date());
		Calendar cal = Calendar.getInstance();
		cal.setTime(bugun);
		cal.add(Calendar.MONTH, -3);
		cal.set(Calendar.DATE, 1);
		Date sonGun = PdksUtil.getDate(cal.getTime());
		Boolean izinCok = izinList.size() > 1;
		MailStatu mailStatu = null;
		Boolean mailBosGonder = izinCok;
		HashMap<String, List<String>> izinPersonelERPMap = new HashMap<String, List<String>>();
		HashMap<String, Integer> referansNoMap = new HashMap<String, Integer>();
		TreeMap<Integer, IzinERP> referansSiraMap = new TreeMap<Integer, IzinERP>();
		HashMap<String, String> bakiyeMap = new HashMap<String, String>();
		Date olusturmaTarihi = null;
		for (Iterator iterator = izinList.iterator(); iterator.hasNext();) {
			IzinERP izinERP = (IzinERP) iterator.next();
			String referansNoERP = izinERP.getReferansNoERP();
			if (referansNoERP.endsWith(PersonelIzin.IZIN_MANUEL_EK)) {
				referansNoERP = referansNoERP.substring(0, referansNoERP.indexOf(PersonelIzin.IZIN_MANUEL_EK) - 1);
				bakiyeMap.put(izinERP.getReferansNoERP(), referansNoERP);
			}
			if (izinCok) {
				String personelNo = izinERP.getPersonelNo();
				if (PdksUtil.hasStringValue(personelNo)) {
					String key = personelNo.trim() + "_" + (izinERP.getBasZaman() != null ? izinERP.getBasZaman().trim() : "") + "_" + (izinERP.getBitZaman() != null ? izinERP.getBitZaman().trim() : "");
					int size = referansNoMap.size() + 1;
					if (!referansNoMap.containsKey(key)) {
						referansNoMap.put(key, size);
					} else {
						size = referansNoMap.get(key);
					}
					referansSiraMap.put(size, izinERP);

					if (PdksUtil.hasStringValue(referansNoERP)) {
						List<String> list = izinPersonelERPMap.containsKey(personelNo) ? izinPersonelERPMap.get(personelNo) : new ArrayList<String>();
						if (list.isEmpty())
							izinPersonelERPMap.put(personelNo, list);
						if (!list.contains(referansNoERP))
							list.add(referansNoERP);
					}
				}
			}

			izinERP.veriSifirla();
		}
		if (!referansSiraMap.isEmpty())
			izinList = new ArrayList<IzinERP>(referansSiraMap.values());
		referansNoMap = null;
		referansSiraMap = null;
		if (izinList.size() == 1) {

			erp = izinList.get(0);
			if (erp != null)
				dosyaEkAdi = erp.getPersonelNo();
		}
		setSicilNoUzunluk();

		saveFonksiyonVeri("saveIzinler", izinList);

		if (erp != null)
			mesaj = PdksUtil.setTurkishStr(erp.getPersonelNo() + " " + erp.getIzinTipiAciklama() + " bilgisi guncellenecektir.");
		else
			mesaj = izinList.size() + " adet izin bilgisi guncellenecektir.";
		mesajInfoYaz("saveIzinler --> " + mesaj + " in " + PdksUtil.getCurrentTimeStampStr());
		// String canliSunucu = "srvglf";
		// if (mailMap != null && mailMap.containsKey("canliSunucu"))
		// canliSunucu = (String) mailMap.get("canliSunucu");
		boolean testDurum = getTestDurum();
		Integer izinBitisTarihiAySayisi = null;
		if (mailMap.containsKey("izinBitisTarihiAySayisi"))
			try {
				izinBitisTarihiAySayisi = Integer.parseInt((String) mailMap.get("izinBitisTarihiAySayisi"));
			} catch (Exception e) {
				izinBitisTarihiAySayisi = null;
			}
		if (izinBitisTarihiAySayisi == null || izinBitisTarihiAySayisi < 0)
			izinBitisTarihiAySayisi = 3;
		TreeMap<String, List<String>> veriSorguMap = new TreeMap<String, List<String>>();
		cal = Calendar.getInstance();
		cal.set(Calendar.DATE, 1);
		cal.add(Calendar.MONTH, -izinBitisTarihiAySayisi);
		Date gecmisTarihi = PdksUtil.getDate(cal.getTime());

		Date tarih1 = null, tarih2 = null;
		for (IzinERP izinERP : izinList) {

			Date baslangicZamani = getTarih(izinERP.getBasZaman(), FORMAT_DATE_TIME);
			Date bitisZamani = getTarih(izinERP.getBitZaman(), FORMAT_DATE_TIME);
			if (baslangicZamani != null)
				if (tarih1 == null || baslangicZamani.before(tarih1))
					tarih1 = baslangicZamani;
			if (bitisZamani != null)
				if (tarih2 == null || bitisZamani.after(tarih2))
					tarih2 = bitisZamani;
			izinERP.setYazildi(false);
			if (izinERP.getIzinSuresi() == null || izinERP.getIzinSuresi().doubleValue() == 0.0d)
				izinERP.setDurum(false);
			if (sicilNoUzunluk != null) {
				String perNo = PdksUtil.textBaslangicinaKarakterEkle(izinERP.getPersonelNo(), '0', sicilNoUzunluk);
				izinERP.setPersonelNo(perNo);
			}
			String referansNoERP = bakiyeMap.containsKey(izinERP.getReferansNoERP()) ? bakiyeMap.get(izinERP.getReferansNoERP()) : izinERP.getReferansNoERP();
			veriIsle("personel", izinERP.getPersonelNo(), veriSorguMap);
			veriIsle("izinTipi", izinERP.getIzinTipi(), veriSorguMap);
			veriIsle("personelIzin", referansNoERP, veriSorguMap);
		}
		List<String> personelNoList = veriSorguMap.get("personel");
		List<Personel> personelList = getSQLObjectListFromDataList(Personel.TABLE_NAME, Personel.COLUMN_NAME_PDKS_SICIL_NO, personelNoList, Personel.class);
		TreeMap<String, ERPPersonel> personelERPHataliMap = veriSorguMap.containsKey("personel") ? getSQLParamListMap(ERPPersonel.TABLE_NAME, "getSicilNo", ERPPersonel.COLUMN_NAME_SICIL_NO, veriSorguMap.get("personel"), ERPPersonel.class, false) : new TreeMap<String, ERPPersonel>();
		TreeMap<String, IzinReferansERP> izinERPMap = veriSorguMap.containsKey("personelIzin") ? getSQLParamListMap(IzinReferansERP.TABLE_NAME, "getId", IzinReferansERP.COLUMN_NAME_ID, veriSorguMap.get("personelIzin"), IzinReferansERP.class, false) : new TreeMap<String, IzinReferansERP>();

		if (personelList.size() > 1)
			personelList = PdksUtil.sortListByAlanAdi(personelList, "iseBaslamaTarihi", Boolean.FALSE);
		TreeMap<String, Personel> personelMap = new TreeMap<String, Personel>();
		String izinVardiyaKontrolStr = mailMap.containsKey("izinVardiyaKontrol") ? (String) mailMap.get("izinVardiyaKontrol") : null;
		Integer izinBitisEksiGun = null;
		try {
			if (izinVardiyaKontrolStr != null)
				izinBitisEksiGun = Integer.parseInt(izinVardiyaKontrolStr);
		} catch (Exception e) {
			izinBitisEksiGun = null;
		}
		if (izinBitisEksiGun == null)
			izinBitisEksiGun = 0;

		for (Personel personel : personelList) {
			if ((tarih1 == null || personel.getSskCikisTarihi().getTime() >= PdksUtil.getDate(tarih1).getTime()) && (tarih2 == null || personel.getIseBaslamaTarihi().getTime() <= PdksUtil.getDate(tarih2).getTime())) {
				if (personelNoList.contains(personel.getPdksSicilNo()))
					personelNoList.remove(personel.getPdksSicilNo());
			}
			personelMap.put(personel.getPdksSicilNo(), personel);
		}
		HashMap<String, Personel> personelERPDBMap = new HashMap<String, Personel>();
		if (!personelNoList.isEmpty() && mailMap.containsKey(getParametrePersonelERPTableView())) {
			String tableName = (String) mailMap.get(getParametrePersonelERPTableView());
			List<PersonelERPDB> personelERPDBList = getSQLObjectListFromDataList(tableName, PersonelERPDB.COLUMN_NAME_PERSONEL_NO, personelNoList, PersonelERPDB.class);
			HashMap<String, Sirket> sirketMap = new HashMap<String, Sirket>();
			HashMap<String, Tanim> tesisMap = new HashMap<String, Tanim>();
			for (PersonelERPDB personelERPDB : personelERPDBList) {
				String erpKodu = personelERPDB.getSirketKodu(), tesisKodu = personelERPDB.getTesisKodu();
				if (PdksUtil.hasStringValue(erpKodu)) {
					Sirket sirket = sirketMap.containsKey(erpKodu) ? sirketMap.get(erpKodu) : null;
					if (sirket == null) {
						sirket = new Sirket();
						sirket.setErpKodu(erpKodu);
						sirket.setAd(personelERPDB.getSirketAdi());
						sirketMap.put(erpKodu, sirket);
					}
					Tanim tesis = null;
					if (PdksUtil.hasStringValue(tesisKodu)) {
						tesis = tesisMap.containsKey(tesisKodu) ? tesisMap.get(tesisKodu) : null;
						if (tesis == null) {
							tesis = new Tanim();
							tesis.setKodu(tesisKodu);
							tesis.setErpKodu(tesisKodu);
							tesis.setAciklamatr(personelERPDB.getTesisAdi());
							tesis.setAciklamaen(personelERPDB.getTesisAdi());
							tesisMap.put(tesisKodu, tesis);
						}
					}

					Personel personel = new Personel();
					personel.setSirket(sirket);
					personel.setTesis(tesis);
					personel.setPdksSicilNo(personelERPDB.getPersonelNo());
					personel.setAd(personelERPDB.getAdi());
					personel.setSoyad(personelERPDB.getSoyadi());
					personelERPDBMap.put(personelERPDB.getPersonelNo(), personel);
				}
			}
			tesisMap = null;
			sirketMap = null;
			personelERPDBList = null;
		}

		// TreeMap<String, Personel> personelMap = veriSorguMap.containsKey("personel") ? pdksDAO.getObjectByInnerObjectMap("getPdksSicilNo", "pdksSicilNo", veriSorguMap.get("personel"), Personel.class, false) : new TreeMap<String, Personel>();
		TreeMap<String, IzinTipi> izinTipiMap = null;
		String erpIzinTipiOlusturStr = sistemDestekVar && mailMap.containsKey("erpIzinTipiOlustur") ? (String) mailMap.get("erpIzinTipiOlustur") : "";
		boolean erpIzinTipiOlustur = erpIzinTipiOlusturStr.equals("1");
		Departman departman = null;
		personelNoList = null;
		personelList = null;
		if (veriSorguMap.containsKey("izinTipi")) {
			List list = veriSorguMap.get("izinTipi");
			String fieldName = "erpKodu";
			fields.clear();
			fields.put("Select", "id");
			fields.put("tipi=", Tanim.TIPI_IZIN_TIPI);
			if (!erpIzinTipiOlustur)
				fields.put("durum=", Boolean.TRUE);

			fields.put(fieldName, list);
			List<Long> idList = getParamList(true, list, fieldName, fields, Tanim.class);
			// pdksDAO.getObjectByInnerObjectListInLogic(fields, Tanim.class);
			if (!idList.isEmpty()) {
				fieldName = "izinTipiTanim.id";
				fields.clear();
				fields.put(BaseDAOHibernate.MAP_KEY_MAP, "getKodERP");
				fields.put("bakiyeIzinTipi=", null);
				fields.put("departman.id=", 1L);
				fields.put("izinTipiTanim.id", idList);
				izinTipiMap = getSQLParamMap("getKodERP", true, idList, fieldName, fields, false, IzinTipi.class);

				// pdksDAO.getObjectByInnerObjectMapInLogic(fields, IzinTipi.class, false);
			}
		}
		if (izinTipiMap == null)
			izinTipiMap = new TreeMap<String, IzinTipi>();

		TreeMap<String, Tanim> izinTipiTanimMap = null;
		if (erpIzinTipiOlustur) {
			fields.clear();
			fields.put("id", 1L);
			departman = (Departman) pdksDAO.getObjectByInnerObject(fields, Departman.class);

			izinTipiTanimMap = getSQLTanimMap(Tanim.TIPI_IZIN_TIPI, null, null, "getErpKodu");
		} else
			izinTipiTanimMap = new TreeMap<String, Tanim>();
		TreeMap<String, Tanim> izinGrupTanimMap = null;
		List saveList = new ArrayList(), deleteList = new ArrayList();
		if (!izinTipiTanimMap.isEmpty()) {
			String fieldName = "erpKodu";
			fields.clear();
			fields.put(BaseDAOHibernate.MAP_KEY_MAP, "getErpKodu");
			fields.put("tipi=", Tanim.TIPI_IZIN_KODU_GRUPLARI);
			for (String key : izinTipiTanimMap.keySet()) {
				// saveList.add("'" + key + "'");
				saveList.add(key);
			}
			fields.put(fieldName, saveList);
			// izinGrupTanimMap = pdksDAO.getObjectByInnerObjectMapInLogic(fields, Tanim.class, false);
			izinGrupTanimMap = getSQLParamMap("getErpKodu", true, saveList, fieldName, fields, false, Tanim.class);

			saveList.clear();
		} else
			izinGrupTanimMap = new TreeMap<String, Tanim>();
		if (!saveList.isEmpty())
			pdksDAO.saveObjectList(saveList);
		saveList.clear();
		List<IzinERP> hataList = new ArrayList<IzinERP>();
		HashMap<String, PersonelIzin> izinMap = new HashMap<String, PersonelIzin>();

		HashMap fields = new HashMap();
		fields.put("beyazYakaDefault", Boolean.TRUE);
		// fields.put("isKur", Boolean.FALSE);
		VardiyaSablonu beyazYakaDefaultVardiyaSablonu = (VardiyaSablonu) pdksDAO.getObjectByInnerObject(fields, VardiyaSablonu.class);
		Integer izinServisGun = null;
		if (mailMap.containsKey("izinServisGunDuzelt")) {
			try {
				izinServisGun = Integer.parseInt((String) mailMap.get("izinServisGunDuzelt"));
			} catch (Exception e) {
				izinServisGun = null;
			}
		}

		List<String> kidemHataList = new ArrayList<String>();

		String kapaliDonemOkuma = mailMap.containsKey("kapaliDonemOkuma") ? (String) mailMap.get("kapaliDonemOkuma") : "";
		boolean kapaliDonemOku = kapaliDonemOkuma.equals("1") == false;
		List<String> kayitIzinList = new ArrayList<String>();
		Date izinlerBasTarih = null, izinlerBitTarih = null;
		for (Iterator iterator = izinList.iterator(); iterator.hasNext();) {
			IzinERP izinERP = (IzinERP) iterator.next();
			boolean tamam = false;
			String referansNoERP = bakiyeMap.containsKey(izinERP.getReferansNoERP()) ? bakiyeMap.get(izinERP.getReferansNoERP()) : izinERP.getReferansNoERP();
			String personelNo = izinERP.getPersonelNo();
			Personel izinSahibi = personelMap.containsKey(personelNo) ? personelMap.get(personelNo) : null;
			Date islemZamani = new Date();
			kidemHataList.clear();
			Date baslangicZamani = getTarih(izinERP.getBasZaman(), FORMAT_DATE_TIME);
			Date bitisZamani = getTarih(izinERP.getBitZaman(), FORMAT_DATE_TIME);
			if (PdksUtil.hasStringValue(referansNoERP) == false)
				addHatalist(hataList, izinERP, izinSahibi, "Referans numarası boş!");
			if (personelNo == null)
				addHatalist(hataList, izinERP, izinSahibi, "Personel numarası boş!");
			else if (!personelMap.containsKey(personelNo)) {
				if (bitisZamani != null && bitisZamani.before(gecmisTarihi)) {
					izinERP.setYazildi(null);
					continue;
				}
				izinERP.setYazildi(Boolean.FALSE);
				ERPPersonel erpPersonel = personelERPHataliMap.containsKey(personelNo) ? personelERPHataliMap.get(personelNo) : null;
				if (erpPersonel == null || erpPersonel.getDurum()) {
					Personel personel = personelERPDBMap.containsKey(personelNo) ? personelERPDBMap.get(personelNo) : izinSahibi;
					String mesaj = personelNo + (personel != null ? " " + personel.getAdSoyad() : "") + " " + kapiGiris + " personel numarası bulunamadı!";
					addHatalist(hataList, izinERP, personel, mesaj);
				} else {
					continue;
				}

			}
			if (izinERP.getIzinTipi() == null)
				addHatalist(hataList, izinERP, izinSahibi, "İzin tipi boş!");
			else if (!izinTipiMap.containsKey(izinERP.getIzinTipi())) {
				if (erpIzinTipiOlustur) {
					Tanim izinTipiTanim = null;
					String aciklama = izinERP.getIzinTipiAciklama();
					if (!izinTipiTanimMap.containsKey(izinERP.getIzinTipi())) {
						izinTipiTanim = new Tanim();
						izinTipiTanim.setTipi(Tanim.TIPI_IZIN_TIPI);
						izinTipiTanim.setKodu("E" + izinERP.getIzinTipi());
						izinTipiTanim.setErpKodu(izinERP.getIzinTipi());
						izinTipiTanim.setAciklamatr(aciklama);
						izinTipiTanim.setAciklamaen(aciklama);
						izinTipiTanim.setDurum(Boolean.TRUE);
						izinTipiTanim.setGuncelle(Boolean.FALSE);
						izinTipiTanim.setIslemTarihi(islemZamani);
						izinTipiTanim.setIslemYapan(islemYapan);
						saveList.add(izinTipiTanim);
						izinTipiTanimMap.put(izinERP.getIzinTipi(), izinTipiTanim);
					} else
						izinTipiTanim = izinTipiTanimMap.get(izinERP.getIzinTipi());
					IzinTipi izinTipi = new IzinTipi();
					izinTipi.setIzinTipiTanim(izinTipiTanim);
					izinTipi.setErpAktarim(Boolean.TRUE);
					izinTipi.setDurum(Boolean.TRUE);
					izinTipi.setDenklestirmeDahil(Boolean.TRUE);
					izinTipi.setOffDahil(Boolean.TRUE);
					izinTipi.setArtikIzinGun(0.0d);
					izinTipi.setDepartman(departman);
					izinTipi.setBakiyeDevirTipi("0");
					izinTipi.setDokumAlmaDurum(Boolean.FALSE);
					izinTipi.setGunGosterilecek(Boolean.TRUE);
					izinTipi.setSaatGosterilecek(Boolean.FALSE);
					izinTipi.setIzinKagidiGeldi(Boolean.FALSE);
					izinTipi.setGunSigortaDahil(Boolean.TRUE);
					izinTipi.setMaxGun(0d);
					izinTipi.setMinGun(0d);
					izinTipi.setMaxSaat(0d);
					izinTipi.setMinSaat(0d);
					izinTipi.setMesaj(aciklama);
					String kisaAciklama = getIzinKisaAciklama(aciklama);
					izinTipi.setKisaAciklama(kisaAciklama);
					izinTipi.setOnaylayanTipi("0");
					izinTipi.setPersonelGirisTipi("0");
					izinTipi.setTakvimGunumu(Boolean.FALSE);
					izinTipi.setHesapTipi(1);
					izinTipi.setDurumCGS(1);
					izinTipi.setKotaBakiye(null);
					izinTipi.setOlusturanUser(islemYapan);
					if (!izinGrupTanimMap.containsKey(izinTipiTanim.getErpKodu())) {
						Tanim tanim2 = new Tanim();
						tanim2.setTipi(Tanim.TIPI_IZIN_KODU_GRUPLARI);
						tanim2.setKodu(BordroIzinGrubu.TANIMSIZ.value());
						tanim2.setErpKodu(izinTipiTanim.getErpKodu());
						tanim2.setIslemYapan(islemYapan);
						tanim2.setIslemTarihi(islemZamani);
						izinGrupTanimMap.put(izinTipiTanim.getErpKodu(), tanim2);
						saveList.add(tanim2);
					}

					saveList.add(izinTipi);
					izinTipiMap.put(izinERP.getIzinTipi(), izinTipi);
				} else
					addHatalist(hataList, izinERP, izinSahibi, izinERP.getIzinTipi() + (izinERP.getIzinTipiAciklama() != null ? " - " + izinERP.getIzinTipiAciklama() : "") + " izin tipi tanımsız!");

			} else {
				IzinTipi izinTipi = izinTipiMap.get(izinERP.getIzinTipi());
				Tanim izinTipiTanim = izinTipi.getIzinTipiTanim();
				String aciklama = izinERP.getIzinTipiAciklama();
				try {
					if (!PdksUtil.setTurkishStr(izinTipiTanim.getAciklamatr().toLowerCase(Constants.TR_LOCALE)).equals(PdksUtil.setTurkishStr(aciklama.toLowerCase(Constants.TR_LOCALE)))) {
						String kisaAciklama = getIzinKisaAciklama(aciklama);
						izinTipiTanim.setAciklamatr(aciklama);
						izinTipiTanim.setAciklamaen(aciklama);
						if (!(izinTipi.isSenelikIzin() || izinTipi.isSutIzin()))
							izinTipiTanim.setKodu(kisaAciklama);
						izinTipiTanim.setIslemYapan(islemYapan);
						izinTipiTanim.setIslemTarihi(islemZamani);
						izinTipi.setMesaj(aciklama);
						izinTipi.setGuncelleyenUser(islemYapan);
						izinTipi.setGuncellemeTarihi(islemZamani);
						saveList.add(izinTipiTanim);
						saveList.add(izinTipi);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			if (baslangicZamani == null) {
				if (notEmptyStr(izinERP.getBasZaman()) == false)
					addHatalist(hataList, izinERP, izinSahibi, "İzin başlangıç zamanı boş olamaz!");
				else
					addHatalist(hataList, izinERP, izinSahibi, "İzin başlangıç zamanı hatalıdır! (" + izinERP.getBasZaman() + " --> format : " + FORMAT_DATE_TIME + " )");
			}
			if (bitisZamani == null) {
				if (notEmptyStr(izinERP.getBitZaman()) == false)
					addHatalist(hataList, izinERP, izinSahibi, "İzin bitiş zamanı boş olamaz!");
				else
					addHatalist(hataList, izinERP, izinSahibi, "İzin bitiş zamanı hatalıdır! (" + izinERP.getBitZaman() + " --> format : " + FORMAT_DATE_TIME + " )");
			}
			if (izinERP.getHataList().isEmpty()) {
				IzinReferansERP izinReferansERP = izinERPMap.containsKey(referansNoERP.trim()) ? izinERPMap.get(referansNoERP.trim()) : new IzinReferansERP(referansNoERP.trim());
				PersonelIzin personelIzin = izinReferansERP.getIzin();
				if (personelIzin.getId() != null)
					personelIzin.setDegisti(false);
				boolean doktor = izinSahibi.isHekim();
				boolean mailEkle = doktor && personelIzin.getId() == null;
				Date sonCalismaTarihi = izinSahibi.getSskCikisTarihi();
				Date bitTarih = PdksUtil.getDate(bitisZamani);
				if (izinBitisEksiGun != 0)
					bitTarih = PdksUtil.tariheGunEkleCikar(bitTarih, -izinBitisEksiGun);
				if (bitisZamani.before(baslangicZamani))
					addHatalist(hataList, izinERP, izinSahibi, "İzin başlama zamanı bitiş tarihinden sonra olamaz!");
				if (baslangicZamani.before(izinSahibi.getIseBaslamaTarihi()) && baslangicZamani.before(izinSahibi.getIzinHakEdisTarihi()))
					addHatalist(hataList, izinERP, izinSahibi, "İzin başlangıç zamanı işe giriş tarihi " + PdksUtil.convertToDateString(izinSahibi.getIseBaslamaTarihi(), FORMAT_DATE) + " den önce olamaz! [ " + izinSahibi.getAdSoyad() + " ]");
				if (PdksUtil.tarihKarsilastirNumeric(bitTarih, sonCalismaTarihi) > 1)
					addHatalist(hataList, izinERP, izinSahibi, "İzin bitiş zamanı işten ayrılma tarihi " + PdksUtil.convertToDateString(sonCalismaTarihi, FORMAT_DATE) + " den sonra olamaz! [ " + izinSahibi.getAdSoyad() + " ]");
				IzinTipi izinTipi = izinTipiMap.get(izinERP.getIzinTipi());
				List<PersonelDenklestirme> kapaliDenklestirmeler = null;
				boolean donemKapali = false;
				Boolean izinDegisti = personelIzin.getId() == null;
				boolean izinDurum = personelIzin.getIzinDurumu() == PersonelIzin.IZIN_DURUMU_ONAYLANDI;
				if (izinERP.getHataList().isEmpty()) {
					if (!mailEkle && personelIzin.getId() != null) {
						try {
							izinDegisti = !izinSahibi.getId().equals(personelIzin.getIzinSahibi().getId()) || !izinTipi.getId().equals(personelIzin.getIzinTipi().getId()) || izinDurum != izinERP.getDurum().booleanValue() || baslangicZamani.getTime() != personelIzin.getBaslangicZamani().getTime()
									|| bitisZamani.getTime() != personelIzin.getBitisZamani().getTime();
							mailEkle = izinDegisti && doktor;
						} catch (Exception e) {
							logger.error(e);
							e.printStackTrace();
						}

					}
					if (izinDegisti) {
						long gecerliDonem = Long.parseLong(PdksUtil.convertToDateString(PdksUtil.tariheGunEkleCikar(new Date(), -10), "yyyyMM"));
						long basDonem = Long.parseLong(PdksUtil.convertToDateString(baslangicZamani, "yyyyMM"));
						if (personelIzin.getId() != null || izinDurum) {
							if (kapaliDonemOku && gecerliDonem > basDonem && izinERP.getReferansNoERP().indexOf(PersonelIzin.IZIN_MANUEL_EK) < 0)
								kapaliDenklestirmeler = getDenklestirmeList(izinSahibi != null ? izinSahibi.getPdksSicilNo() : null, baslangicZamani, bitisZamani, false);
						}

					}
				}

				if (kapaliDenklestirmeler != null && !kapaliDenklestirmeler.isEmpty()) {

					StringBuffer donemStr = new StringBuffer();
					// donemKapali = true;
					for (Iterator iterator2 = kapaliDenklestirmeler.iterator(); iterator2.hasNext();) {
						PersonelDenklestirme personelDenklestirme = (PersonelDenklestirme) iterator2.next();
						DenklestirmeAy denklestirmeAy = personelDenklestirme.getDenklestirmeAy();
						donemStr.append(denklestirmeAy.getAyAdi() + " " + denklestirmeAy.getYil());
						if (iterator2.hasNext())
							donemStr.append(", ");
						if (personelDenklestirme.getDurum().equals(Boolean.FALSE))
							iterator2.remove();
						else {
							personelDenklestirme.setDurum(Boolean.FALSE);
						}
					}
					if (!kapaliDenklestirmeler.isEmpty()) {
						if (bitisZamani == null || bitisZamani.after(sonGun))
							hataList.add(izinERP);
					} else
						izinERP.setDurum(null);
					String str = donemStr.toString();
					if (personelIzin.getId() == null && izinDurum == false) {
						iterator.remove();
						continue;
					}
					addHatalist(hataList, izinERP, izinSahibi, str + " " + (kapaliDenklestirmeler.size() > 1 ? " dönemleri" : " dönemi") + " kapalıdır");

				}

				if (izinTipi != null && (donemKapali || izinERP.getHataList().isEmpty())) {

					if ((izinERP.getSureBirimi() == null && izinTipi.getHesapTipi() != null && izinTipi.getHesapTipi().equals(PersonelIzin.HESAP_TIPI_GUN)) || izinERP.getSureBirimi().value().equals(SureBirimi.GUN.value())) {
						if (izinServisGun != null) {
							Date t1 = PdksUtil.getDate(baslangicZamani), t2 = PdksUtil.getDate(bitisZamani);
							VardiyaSablonu vardiyaSablonu = beyazYakaDefaultVardiyaSablonu != null ? beyazYakaDefaultVardiyaSablonu : izinSahibi.getSablon();
							Vardiya vardiya = vardiyaSablonu.getVardiya1();
							if (t1.getTime() == baslangicZamani.getTime()) {
								fields.clear();
								fields.put("personel.id", izinSahibi.getId());
								fields.put("vardiyaDate", t1);
								VardiyaGun vardiyaGun = (VardiyaGun) pdksDAO.getObjectByInnerObject(fields, VardiyaGun.class);
								if (vardiyaGun == null)
									vardiyaGun = new VardiyaGun(izinSahibi, vardiya, t1);
								vardiyaGun.setVardiyaZamani();
								Vardiya islemVardiya = vardiyaGun.getIslemVardiya(), vardiya2 = vardiyaGun.getVardiya();
								baslangicZamani = islemVardiya.getVardiyaBasZaman();
								if (vardiya2.isHaftaTatil() || (vardiyaGun.getId() == null && PdksUtil.getDateField(t2, Calendar.DAY_OF_WEEK) == Calendar.SUNDAY))
									baslangicZamani = PdksUtil.tariheGunEkleCikar(baslangicZamani, 1);
								vardiyaGun = null;
							}
							if (t2.getTime() == bitisZamani.getTime()) {
								Date vardiyaDate = PdksUtil.tariheGunEkleCikar(t2, -izinServisGun);
								fields.clear();
								fields.put("personel.id", izinSahibi.getId());
								fields.put("vardiyaDate", vardiyaDate);
								VardiyaGun vardiyaGun = (VardiyaGun) pdksDAO.getObjectByInnerObject(fields, VardiyaGun.class);
								if (vardiyaGun == null)
									vardiyaGun = new VardiyaGun(izinSahibi, vardiya, vardiyaDate);
								vardiyaGun.setVardiyaZamani();
								Vardiya islemVardiya = vardiyaGun.getIslemVardiya(), vardiya2 = vardiyaGun.getVardiya();
								bitisZamani = islemVardiya.getVardiyaBitZaman();
								if (vardiya2.isHaftaTatil() || (vardiyaGun.getId() == null && PdksUtil.getDateField(t2, Calendar.DAY_OF_WEEK) == Calendar.MONDAY))
									bitisZamani = PdksUtil.tariheGunEkleCikar(bitisZamani, -1);
								vardiyaGun = null;
							}
						}
					}
					if (izinlerBasTarih == null || baslangicZamani.before(izinlerBasTarih))
						izinlerBasTarih = baslangicZamani;
					if (izinlerBitTarih == null || bitisZamani.after(izinlerBitTarih))
						izinlerBitTarih = bitisZamani;
					if (personelIzin.getId() != null && (olusturmaTarihi == null || olusturmaTarihi.before(personelIzin.getOlusturmaTarihi())))
						olusturmaTarihi = personelIzin.getOlusturmaTarihi();
					personelIzin.setIzinSahibi(izinSahibi);
					personelIzin.setBaslangicZamani(baslangicZamani);
					personelIzin.setBitisZamani(bitisZamani);
					if (izinERP.getDurum().booleanValue() == false && (personelIzin.getId() == null || personelIzin.getIzinDurumu() == PersonelIzin.IZIN_DURUMU_REDEDILDI)) {
						tamam = true;
						izinERP.setId(personelIzin.getId());
						izinERP.setYazildi(true);
					}
					if (izinERP.getDurum().booleanValue() || personelIzin.getId() != null) {
						if (personelIzin.getId() == null) {
							if (!(PdksUtil.getDate(izinlerBasTarih).getTime() <= izinSahibi.getSskCikisTarihi().getTime() && (PdksUtil.getDate(izinlerBitTarih).getTime() >= izinSahibi.getIseBaslamaTarihi().getTime() || bitTarih.getTime() >= izinSahibi.getIzinHakEdisTarihi().getTime())))
								addHatalist(hataList, izinERP, izinSahibi, PdksUtil.convertToDateString(personelIzin.getBitisZamani(), FORMAT_DATE_TIME) + " tarihinde çalışmıyor!!");
						}

						if (izinERP.getDurum().booleanValue() || donemKapali) {
							fields.clear();
							StringBuffer sb = new StringBuffer();
							sb.append(" select R." + IzinReferansERP.COLUMN_NAME_ID + ",I." + PersonelIzin.COLUMN_NAME_ID + " as " + IzinReferansERP.COLUMN_NAME_IZIN_ID + " from " + PersonelIzin.TABLE_NAME + " I " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
							sb.append(" left join " + IzinReferansERP.TABLE_NAME + " R " + PdksVeriOrtakAktar.getJoinLOCK() + " on I." + PersonelIzin.COLUMN_NAME_ID + " = R." + IzinReferansERP.COLUMN_NAME_IZIN_ID);
							sb.append(" where I." + PersonelIzin.COLUMN_NAME_PERSONEL + " = " + izinSahibi.getId());
							sb.append(" and I." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " not in ( " + PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL + "," + PersonelIzin.IZIN_DURUMU_REDEDILDI + ")");
							sb.append(" and I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI + " < :b2 and I." + PersonelIzin.COLUMN_NAME_BITIS_ZAMANI + " > :b1");
							if (personelIzin.getId() != null)
								sb.append(" and I." + PersonelIzin.COLUMN_NAME_ID + " <> " + personelIzin.getId());
							fields.put("b1", personelIzin.getBaslangicZamani());
							fields.put("b2", personelIzin.getBitisZamani());
							List<IzinReferansERP> kayitList = null;
							try {
								kayitList = pdksDAO.getNativeSQLList(fields, sb, IzinReferansERP.class);
							} catch (Exception ek) {
								logger.error(ek);
								ek.printStackTrace();
							}

							if (kayitList != null && !kayitList.isEmpty()) {
								IzinReferansERP izinReferansErp = kayitList.get(0);
								PersonelIzin digerIzin = izinReferansErp != null ? izinReferansErp.getIzin() : null;
								if (digerIzin != null) {
									logger.debug(personelNo + " " + referansNoERP + " [ " + izinERP.getBasZaman() + " - " + izinERP.getBitZaman() + " ]");
									String basStr = PdksUtil.convertToDateString(digerIzin.getBaslangicZamani(), FORMAT_DATE_TIME), bitStr = PdksUtil.convertToDateString(digerIzin.getBitisZamani(), FORMAT_DATE_TIME);
									if (!basStr.equals(izinERP.getBitZaman()) && !bitStr.equals(izinERP.getBasZaman())) {
										boolean hataVar = !(izinERP.getBasZaman().compareTo(basStr) != 1 && izinERP.getBitZaman().compareTo(bitStr) != -1) || !izinPersonelERPMap.containsKey(personelNo);
										if (hataVar)
											addHatalist(hataList, izinERP, izinSahibi, basStr + " - " + bitStr + " kayıtlı izin vardır!!");
										else {
											personelIzin = digerIzin;
											izinDegisti = true;
											if (izinReferansErp.getId() == null || !izinReferansErp.getId().equals(referansNoERP)) {
												if (izinReferansErp.getId() != null)
													deleteList.add(izinReferansErp);
												izinReferansERP.setIzin(personelIzin);
												saveList.add(izinReferansERP);
											}

										}
									} else {
										IzinReferansERP izinReferansERP2 = (IzinReferansERP) pdksDAO.getObjectByInnerObject("izin.id", digerIzin.getId(), IzinReferansERP.class);
										if (izinReferansERP2 != null) {
											digerIzin.setIzinDurumu(PersonelIzin.IZIN_DURUMU_REDEDILDI);
											digerIzin.setGuncelleyenUser(islemYapan);
											digerIzin.setGuncellemeTarihi(islemZamani);
											saveList.add(digerIzin);
										}
									}
								} else {
									logger.debug("hata : " + izinERP.getBasZaman() + " - " + izinERP.getBitZaman() + "\n" + sb.toString());
								}
							}
						}
						if (izinERP.getHataList().isEmpty() || donemKapali) {

							personelIzin.setIzinTipi(izinTipi);
							Double izinSuresi = izinERP.getIzinSuresi();
							String aciklama = izinERP.getAciklama() != null ? izinERP.getAciklama() : izinTipi.getMesaj();
							String referansBaslik = PdksUtil.hasStringValue(referansNoERP) == false || referansNoERP.startsWith(PersonelIzin.IZIN_MANUEL_EK) == false ? uygulamaBordro : "IK aktarım ";
							personelIzin.setAciklama((aciklama != null ? aciklama.trim() : "") + " ( " + referansBaslik + " referans no : " + referansNoERP + " )");
							Integer hesapTipi = null;
							if (izinERP.getSureBirimi() != null) {
								hesapTipi = Integer.parseInt(izinERP.getSureBirimi().value());
								if (izinTipi.getHesapTipi() != null && hesapTipi.intValue() != izinTipi.getHesapTipi().intValue()) {
									switch (hesapTipi) {
									case 1:
										izinSuresi = izinSuresi * 24d;
										break;
									case 2:
										izinSuresi = izinSuresi / 24d;
										break;
									default:
										break;
									}
									hesapTipi = izinTipi.getHesapTipi();
								}
							} else
								hesapTipi = izinTipi.getHesapTipi();
							personelIzin.setIzinSuresi(izinSuresi);
							personelIzin.setHesapTipi(hesapTipi);
							personelIzin.setIzinDurumu(izinERP.getDurum() != null && izinERP.getDurum() ? PersonelIzin.IZIN_DURUMU_ONAYLANDI : PersonelIzin.IZIN_DURUMU_REDEDILDI);
							if (personelIzin.isDegisti()) {
								saveList.add(personelIzin);
								if (personelIzin.getId() == null) {
									personelIzin.setOlusturanUser(islemYapan);
									personelIzin.setOlusturmaTarihi(islemZamani);
									saveList.add(izinReferansERP);
									if (donemKapali && kapaliDenklestirmeler != null)
										saveList.addAll(kapaliDenklestirmeler);
								} else {
									personelIzin.setGuncellemeTarihi(islemZamani);
									personelIzin.setGuncelleyenUser(islemYapan);
								}
							} else {
								izinERP.setYazildi(Boolean.TRUE);
								izinERP.setId(personelIzin.getId());
							}

							try {

								if (listeKaydet(referansNoERP, saveList, deleteList)) {
									if (personelIzin.getIzinDurumu() != PersonelIzin.IZIN_DURUMU_REDEDILDI && personelIzin.getIzinDurumu() != PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL) {
										kayitIzinList.add(referansNoERP);
									}
									if (mailEkle)
										izinMap.put(izinReferansERP.getId(), izinReferansERP.getIzin());
									izinERP.setYazildi(Boolean.TRUE);
									izinERP.setId(personelIzin.getId());
									izinERP.setHataList(null);
								}
							} catch (Exception e) {
								if (e.getMessage() != null)
									addHatalist(hataList, izinERP, izinSahibi, e.getMessage());
								else
									addHatalist(hataList, izinERP, izinSahibi, "Hata oluştu!");
							}

						} else if (!izinERP.getHataList().isEmpty())
							hataList.add((IzinERP) izinERP.clone());
						kapaliDenklestirmeler = null;
					} else {
						if (tamam == false)
							addHatalist(hataList, izinERP, izinSahibi, "İptal yeni kayıt sisteme yazılmadı!");
					}

				}
			} else if (!izinERP.getHataList().isEmpty()) {
				hataList.add((IzinERP) izinERP.clone());
			}
			izinERP.setAciklama(null);
			if (izinERP.getHataList().isEmpty()) {
				izinERP.setDurum(null);
				izinERP.setIzinTipiAciklama(null);
				izinERP.setBasZaman(null);
				izinERP.setBitZaman(null);
			}
		}
		Integer gecmisIzinKontrolAdet = null;
		try {
			if (mailMap.containsKey("gecmisIzinKontrolAdet"))
				gecmisIzinKontrolAdet = Integer.parseInt((String) mailMap.get("gecmisIzinKontrolAdet"));
			else {
				kayitIzinList.clear();
				gecmisIzinKontrolAdet = kayitIzinList.size() + 1;
			}

		} catch (Exception e) {
			gecmisIzinKontrolAdet = null;
		}
		if (gecmisIzinKontrolAdet == null) {
			kayitIzinList.clear();
			gecmisIzinKontrolAdet = kayitIzinList.size() + 1;
		}
		if (mailMap.containsKey("gelmeyenIzinERPIptal"))
			izinCok = ((String) mailMap.get("gelmeyenIzinERPIptal")).equals("1") == false;
		if (izinCok && kayitIzinList.size() >= gecmisIzinKontrolAdet && izinlerBasTarih != null && izinlerBitTarih != null && izinlerBasTarih.before(izinlerBitTarih)) {
			Date tarih = PdksUtil.tariheAyEkleCikar(bugun, 2);
			Date bTarih = PdksUtil.convertToJavaDate(PdksUtil.convertToDateString(PdksUtil.tariheAyEkleCikar(bugun, -2), "yyyyMM") + "01", "yyyyMMdd");
			if (izinlerBasTarih.before(bTarih))
				izinlerBasTarih = bTarih;
			if (izinlerBitTarih.after(tarih))
				izinlerBitTarih = tarih;
			HashMap map = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select R." + IzinReferansERP.COLUMN_NAME_ID + ",I." + PersonelIzin.COLUMN_NAME_PERSONEL_NO + ",R." + IzinReferansERP.COLUMN_NAME_IZIN_ID + " from " + PersonelIzin.TABLE_NAME + " I " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append(" inner join " + IzinReferansERP.TABLE_NAME + " R " + PdksVeriOrtakAktar.getJoinLOCK() + " on R." + IzinReferansERP.COLUMN_NAME_IZIN_ID + " = I." + PersonelIzin.COLUMN_NAME_ID);
			sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = I." + PersonelIzin.COLUMN_NAME_PERSONEL);
			sb.append(" and P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= :b");
			sb.append(" and P." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI + "< I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI);
			sb.append(" where I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI + " <= :b2 and I." + PersonelIzin.COLUMN_NAME_BITIS_ZAMANI + " >= :b1 ");
			sb.append(" and I." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " <> :d1 and I." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " <> :d2  ");
			sb.append(" and I." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " <> :d1 and I." + PersonelIzin.COLUMN_NAME_IZIN_DURUMU + " <> :d2  ");
			if (olusturmaTarihi != null) {
				sb.append(" and I." + Personel.COLUMN_NAME_OLUSTURMA_TARIHI + " <= :o");
				map.put("o", PdksUtil.getDate(olusturmaTarihi));
			}

			sb.append(" order by I." + PersonelIzin.COLUMN_NAME_PERSONEL_NO + ",I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI);
			map.put("b", PdksUtil.getDate(bugun));
			map.put("b1", izinlerBasTarih);
			map.put("b2", izinlerBitTarih);
			map.put("d1", PersonelIzin.IZIN_DURUMU_REDEDILDI);
			map.put("d2", PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL);
			List<Object[]> list = PdksUtil.isPazar() == false ? pdksDAO.getNativeSQLList(map, sb, null) : null;
			map.clear();
			if (list != null) {
				for (Object[] objects : list) {
					String ref = (String) objects[0];
					if (!kayitIzinList.contains(ref))
						map.put(((BigDecimal) objects[2]).longValue(), objects[1]);
				}
			}
			if (!map.isEmpty()) {
				List<IzinReferansERP> personelIzinList = pdksDAO.getObjectByInnerObjectList("izin.id", new ArrayList<Long>(map.keySet()), IzinReferansERP.class);
				Date dateBugun = PdksUtil.getDate(new Date());
				for (Iterator iterator = personelIzinList.iterator(); iterator.hasNext();) {
					IzinReferansERP izinReferansERP = (IzinReferansERP) iterator.next();
					PersonelIzin personelIzin = izinReferansERP.getIzin();
					if (personelIzin.getOlusturmaTarihi() != null && personelIzin.getOlusturmaTarihi().after(dateBugun))
						iterator.remove();
				}
				if (!personelIzinList.isEmpty()) {
					if (mailMap.containsKey("acikAyGelmeyenIzinERPIptal")) {
						List<Long> idList = new ArrayList<Long>();
						for (IzinReferansERP izinReferansERP : personelIzinList) {
							PersonelIzin personelIzin = izinReferansERP.getIzin();
							if (PdksUtil.convertToDateString(personelIzin.getBaslangicZamani(), "yyyyMM").equals(PdksUtil.convertToDateString(personelIzin.getBitisZamani(), "yyyyMM")))
								idList.add(personelIzin.getId());
						}
						if (!idList.isEmpty()) {
							String fieldName = "p";
							fields.clear();
							sb = new StringBuffer();
							sb.append("select I." + PersonelIzin.COLUMN_NAME_ID + ",P." + PersonelDenklestirme.COLUMN_NAME_ID + " as PD_ID from " + PersonelIzin.TABLE_NAME + " I " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
							sb.append(" inner join " + DenklestirmeAy.TABLE_NAME + " D " + PdksVeriOrtakAktar.getJoinLOCK() + " on D." + DenklestirmeAy.COLUMN_NAME_YIL + " = YEAR(I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI + ")");
							sb.append(" and D." + DenklestirmeAy.COLUMN_NAME_AY + " = MONTH(I." + PersonelIzin.COLUMN_NAME_BASLANGIC_ZAMANI + ") and D." + DenklestirmeAy.COLUMN_NAME_DURUM + " = 1 ");
							sb.append(" inner join " + PersonelDenklestirme.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = D." + DenklestirmeAy.COLUMN_NAME_ID);
							sb.append(" and P." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " = I." + PersonelIzin.COLUMN_NAME_PERSONEL);
							sb.append(" where I." + PersonelIzin.COLUMN_NAME_ID + " :" + fieldName);
							sb.append(" order by I." + PersonelIzin.COLUMN_NAME_BITIS_ZAMANI + " desc");
							fields.put(fieldName, idList);
							List<Object[]> izinAcikList = getNativeSQLParamList(idList, sb, fieldName, fields, null);

							// pdksDAO.getNativeSQLList(fields, sb, null);
							if (!izinAcikList.isEmpty()) {
								List saveIzinList = new ArrayList();
								Date guncellemeTarihi = new Date();
								for (Object[] objects : izinAcikList) {
									Long izinId = ((BigDecimal) objects[0]).longValue(), pdId = ((BigDecimal) objects[1]).longValue();
									PersonelDenklestirme personelDenklestirme = (PersonelDenklestirme) pdksDAO.getObjectByInnerObject("id", pdId, PersonelDenklestirme.class);
									personelDenklestirme.setDurum(Boolean.FALSE);
									saveIzinList.add(personelDenklestirme);
									for (Iterator iterator = personelIzinList.iterator(); iterator.hasNext();) {
										IzinReferansERP izinErp = (IzinReferansERP) iterator.next();
										PersonelIzin izin = izinErp.getIzin();
										if (izin.getId().equals(izinId)) {
											izin.setIzinDurumu(PersonelIzin.IZIN_DURUMU_SISTEM_IPTAL);
											izin.setGuncellemeTarihi(guncellemeTarihi);
											izin.setGuncelleyenUser(islemYapan);
											saveIzinList.add(izin);
											iterator.remove();
											break;
										}
									}
								}
								if (!saveIzinList.isEmpty())
									pdksDAO.saveObjectList(saveIzinList);
								saveIzinList = null;
							}
							izinAcikList = null;
						}
						idList = null;
					}

				}
				if (!personelIzinList.isEmpty()) {
					personelIzinList = PdksUtil.sortListByAlanAdi(personelIzinList, "sortAlan", Boolean.TRUE);
					mailMap.put("konu", uygulamaBordro + " gelmeyen izinler");
					sb = new StringBuffer();
					sb.append("<p><b>" + mailMap.get("konu") + " var!</b></p>");
					sb.append("<TABLE class=\"mars\" style=\"width: 90%\">");
					boolean renkUyari = true;
					sb.append("<THEAD><TR><TH>" + personelNoAciklama() + "</TH>");
					sb.append("<TH>Adı Soyadı</TH>");
					sb.append("<TH>Tipi</TH><TH>Başlangıç Zamanı</TH>");
					sb.append("<TH>Bitiş Zamanı</TH>");
					sb.append("<TH>" + uygulamaBordro + " Referans No</TH></TR></THEAD><TBODY>");
					for (IzinReferansERP personelIzinTum : personelIzinList) {
						PersonelIzin personelIzin = personelIzinTum.getIzin();
						sb.append("<TR class=\"" + (renkUyari ? "odd" : "even") + "\">");
						sb.append("<TD align=\"center\">" + personelIzin.getPersonelNo() + "</TD>");
						sb.append("<TD>" + personelIzin.getIzinSahibi().getAdSoyad() + "</TD>");
						sb.append("<TD >" + (personelIzin.getIzinTipi() != null ? personelIzin.getIzinTipi().getIzinTipiTanim().getAciklamatr() : "") + "</TD>");
						sb.append("<TD align=\"center\">" + PdksUtil.convertToDateString(personelIzin.getBaslangicZamani(), FORMAT_DATE_TIME) + "</TD>");
						sb.append("<TD align=\"center\">" + PdksUtil.convertToDateString(personelIzin.getBitisZamani(), FORMAT_DATE_TIME) + "</TD>");
						sb.append("<TD >" + (personelIzinTum.getId() != null ? personelIzinTum.getId().trim() : "") + "</TD></TR>");
						renkUyari = !renkUyari;
						logger.debug(personelIzin.getPersonelNo() + " | " + PdksUtil.convertToDateString(personelIzin.getBaslangicZamani(), FORMAT_DATE_TIME) + " | " + PdksUtil.convertToDateString(personelIzin.getBitisZamani(), FORMAT_DATE_TIME) + " | " + personelIzin.getAciklama() + " | "
								+ PdksUtil.convertToDateString(personelIzin.getOlusturmaTarihi(), FORMAT_DATE_TIME) + " | " + personelIzin.getId());
					}
					sb.append("</TBODY></TABLE>");
					mailMap.put("mailIcerik", sb.toString());
					if (testDurum)
						mailMap.put(KEY_IK_MAIL_IPTAL, testDurum);
					mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
					kullaniciIKYukle(null, mailMap, pdksDAO);
					mailStatu = MailManager.ePostaGonder(mailMap);
				}
				personelIzinList = null;
			}
			map = null;
		}
		kayitIzinList = null;
		boolean hataVar = false;
		if (hataList != null && !hataList.isEmpty() || (hataIKMap != null && !hataIKMap.isEmpty())) {
			hataVar = isTatil() == false;

		}
		if (hataVar) {
			List<String> perNoList = new ArrayList<String>();
			for (IzinERP izinERP : hataList) {
				if (PdksUtil.hasStringValue(izinERP.getPersonelNo()) && !perNoList.contains(izinERP.getPersonelNo().trim()))
					perNoList.add(izinERP.getPersonelNo().trim());

			}

			if (personelERPDBMap != null && !personelERPDBMap.isEmpty())
				personelMap.putAll(personelERPDBMap);
			List<User> userIKList = null;
			boolean devam = true;
			if (hataIKMap != null) {
				int hataIKMapSize = hataIKMap.size();
				List<Long> userIdList = new ArrayList<Long>();
				if (!hataIKMap.containsKey(TIPI_IK_ADMIN) && ikUserMap.containsKey(Role.TIPI_IK)) {
					HashMap<String, List<User>> map1 = ikUserMap.get(Role.TIPI_IK);
					if (map1.containsKey(TIPI_IK_ADMIN)) {
						userIKList = map1.get(TIPI_IK_ADMIN);
						if (userIKList != null) {
							for (User user : userIKList)
								userIdList.add(user.getId());
						}
					}
				}
				for (String key : hataIKMap.keySet()) {
					mailMap.put(KEY_IK_MAIL_IPTAL, Boolean.TRUE);
					HashMap<String, List> dataHataMap = hataIKMap.get(key);
					List<User> userList = dataHataMap.get("userList");
					if (userIdList.isEmpty() == false) {
						for (Iterator iterator = userList.iterator(); iterator.hasNext();) {
							User user = (User) iterator.next();
							if (userIdList.contains(user.getId()))
								iterator.remove();
						}
					}
					if (!userList.isEmpty()) {
						if ((perNoList.size() == 1 || hataIKMapSize == 1) && userIdList.isEmpty() == false) {
							if (userIKList != null) {
								if (!userIKList.isEmpty())
									userList.addAll(userIKList);
								userIKList.clear();
								devam = false;
							}

						}
						List hataIKList = dataHataMap.get("hataList");
						izinHataliMailGonder(userList, hataIKList, personelMap, izinCok);

					}
				}
				perNoList = null;
				userIdList = null;

			} else {
				userIKList = new ArrayList<User>();
				if (mailMap.containsKey(KEY_IK_MAIL_IPTAL))
					mailMap.remove(KEY_IK_MAIL_IPTAL);

			}
			if (devam && (userIKList != null || !mailMap.containsKey(KEY_IK_MAIL_IPTAL))) {
				mailStatu = izinHataliMailGonder(userIKList, hataList, personelMap, izinCok);
				if (mailStatu != null && mailStatu.isDurum())
					logger.info("saveIzinler hata gonderildi. " + PdksUtil.getCurrentTimeStampStr());
			}
		}
		if (!izinMap.isEmpty()) {
			List<Long> personelIdList = new ArrayList<Long>();
			for (String key : izinMap.keySet()) {
				Long personelId = izinMap.get(key).getIzinSahibi().getId();
				if (!personelIdList.contains(personelId))
					personelIdList.add(personelId);
			}
			TreeMap<Long, Personel> doktorUserMap = pdksDAO.getObjectByInnerObjectMap("getId", "id", personelIdList, Personel.class, false);
			if (!doktorUserMap.isEmpty()) {
				if (mailMap.containsKey("toEntegrasyonAdres"))
					mailMap.remove("toEntegrasyonAdres");
				if (mailMap.containsKey("ccEntegrasyonAdres"))
					mailMap.remove("ccEntegrasyonAdres");
				if (mailMap.containsKey("bccEntegrasyonAdres"))
					mailMap.remove("bccEntegrasyonAdres");
				for (String key : izinMap.keySet()) {
					PersonelIzin izin = izinMap.get(key);
					Personel izinSahibi = izin.getIzinSahibi();
					Long personelId = izin.getIzinSahibi().getId();
					if (doktorUserMap.containsKey(personelId) && (izinSahibi.getEmailCC() != null || izinSahibi.getEmailBCC() != null)) {
						// User doktorUser = doktorUserMap.get(personelId);
						String cc = izinSahibi.getEmailCC(), bcc = izinSahibi.getEmailBCC();
						if ((cc != null && cc.indexOf("@") > 0) || (bcc != null && bcc.indexOf("@") > 0)) {
							try {
								boolean gonder = false;
								if (mailMap.containsKey("mailAdresleriCC"))
									mailMap.remove("mailAdresleriCC");
								if (mailMap.containsKey("mailAdresleriBCC"))
									mailMap.remove("mailAdresleriBCC");
								if (cc != null && cc.indexOf("@") > 0) {
									List<String> list = PdksUtil.getListByString(cc, null);
									for (Iterator iterator = list.iterator(); iterator.hasNext();) {
										String string = (String) iterator.next();
										if (string.indexOf("@") < 1)
											iterator.remove();
									}
									if (!list.isEmpty()) {
										gonder = true;
										mailMap.put("mailAdresleriCC", list);
									}

								}
								if (bcc != null && bcc.indexOf("@") > 0) {
									List<String> list = PdksUtil.getListByString(bcc, null);
									for (Iterator iterator = list.iterator(); iterator.hasNext();) {
										String string = (String) iterator.next();
										if (string.indexOf("@") < 1)
											iterator.remove();
									}
									if (!list.isEmpty()) {
										gonder = true;
										mailMap.put("mailAdresleriBCC", list);
									}
								}
								if (gonder) {
									mailMap.put("konu", uygulamaBordro + " " + izinSahibi.getAdSoyad() + " izin " + (!izin.isRedmi() ? " girişi" : "iptali"));
									StringBuffer sb = new StringBuffer();
									sb.append("<p><b>" + izinSahibi.getAdSoyad() + " izin " + (!izin.isRedmi() ? " girişi " : " iptali ") + " yapıldı.</b></p>");
									sb.append("<p>" + PdksUtil.convertToDateString(izin.getBaslangicZamani(), FORMAT_DATE_TIME) + " - " + PdksUtil.convertToDateString(izin.getBitisZamani(), FORMAT_DATE_TIME) + " arası izinlidir.</p>");
									sb.append("<p></p>");
									sb.append("<p>Saygılarımla</p>");
									mailMap.put("mailIcerik", sb.toString());
									mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
									kullaniciIKYukle(null, mailMap, pdksDAO);
									mailStatu = MailManager.ePostaGonder(mailMap);
									mailBosGonder = false;
								} else
									doktorUserMap.remove(personelId);

							} catch (Exception em) {
								logger.equals(em);
								em.printStackTrace();
							}

						} else
							doktorUserMap.remove(personelId);
					}

				}
			}
			doktorUserMap = null;
		}
		if (mailBosGonder && mailStatu == null && (getTestSunucuDurum() || erpVeriOku == false))
			mailBosGonder("saveIzinler", "izin", izinList);
		hataList = null;
		bakiyeMap = null;
		saveFonksiyonVeri(null, izinList);

		mesajInfoYaz("saveIzinler --> " + mesaj + " out " + PdksUtil.getCurrentTimeStampStr());
	}

	private String getIzinKisaAciklama(String aciklama) {
		String kisaAciklama = "";
		if (PdksUtil.hasStringValue(aciklama)) {
			StringTokenizer st = new StringTokenizer(aciklama, " ");
			while (st.hasMoreTokens()) {
				String tk = st.nextToken();
				kisaAciklama += tk.substring(0, 1).toUpperCase(Constants.TR_LOCALE);

			}
		}
		return kisaAciklama;
	}

	/**
	 * 
	 */
	protected void testMailObject(MailObject mailObject) {
		MailPersonel mailPersonel = new MailPersonel();
		mailPersonel.setAdiSoyadi("Hasan Sayar");
		mailPersonel.setePosta("hasansayar58@gmail.com");
		mailObject.getToList().add(mailPersonel);
	}

	/**
	 * @param perNo
	 * @param basTarih
	 * @param bitTarih
	 * @param donemDurum
	 * @return
	 */
	private List<PersonelDenklestirme> getDenklestirmeList(String perNo, Date basTarih, Date bitTarih, boolean donemDurum) {
		List<PersonelDenklestirme> list = null;
		if (basTarih != null && bitTarih != null && PdksUtil.hasStringValue(perNo)) {
			String d1 = PdksUtil.convertToDateString(bitTarih, "yyyyMM"), d2 = PdksUtil.convertToDateString(bitTarih, "yyyyMM");
			HashMap fields = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("with DENKAY as ( ");
			sb.append(" select " + DenklestirmeAy.COLUMN_NAME_YIL + "*100+" + DenklestirmeAy.COLUMN_NAME_AY + " as DONEM,* from " + DenklestirmeAy.TABLE_NAME + " " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append("	 where " + DenklestirmeAy.COLUMN_NAME_DURUM + " = " + (donemDurum ? "1" : "0"));
			sb.append(" ) ");
			sb.append(" select PD.* from DENKAY D " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " = :p ");
			sb.append(" inner join " + PersonelDenklestirme.TABLE_NAME + " PD " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = PD." + PersonelDenklestirme.COLUMN_NAME_PERSONEL + " and PD." + PersonelDenklestirme.COLUMN_NAME_DONEM + " = D."
					+ DenklestirmeAy.COLUMN_NAME_ID);
			sb.append(" and PD." + PersonelDenklestirme.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" where D.DONEM >= :d1 and D.DONEM <= :d2 ");
			sb.append(" order by D.DONEM desc");
			fields.put("d1", Long.parseLong(d1));
			fields.put("d2", Long.parseLong(d2));
			fields.put("p", perNo);
			list = pdksDAO.getNativeSQLList(fields, sb, PersonelDenklestirme.class);
		} else if (donemDurum)
			list = new ArrayList<PersonelDenklestirme>();

		return list;
	}

	/**
	 * @param key
	 * @param value
	 * @param veriSorguMap
	 */
	private void veriIsle(String key, Object value, TreeMap<String, List<String>> veriSorguMap) {
		if (value != null) {
			List<String> list = veriSorguMap.containsKey(key) ? veriSorguMap.get(key) : null;
			String str = null;
			if (value instanceof String) {
				String deger = ((String) value).trim();
				if (deger.length() > 0)
					str = deger; // str = "'" + deger + "'";

			} else
				str = value.toString();
			if (str != null) {
				if (list == null) {
					list = new ArrayList<String>();
					veriSorguMap.put(key, list);
				}
				if (!list.contains(str))
					list.add(str);
			}
		}

	}

	/**
	 * @param parentKey
	 * @param tanimKodu
	 * @param aciklama
	 * @param dataMap
	 * @param saveList
	 * @return
	 */
	private Tanim getDinamikTanim(String parentKey, String tanimKodu, String aciklama, TreeMap<String, TreeMap> dataMap, List saveList) {
		Tanim tanim = null;
		try {
			boolean ekle = false;
			if (PdksUtil.hasStringValue(tanimKodu) && PdksUtil.hasStringValue(aciklama)) {
				TreeMap<String, Tanim> personelDinamikTanimAlanMap = dataMap.get("personelDinamikTanimAlanMap");
				Tanim parentTanim = personelDinamikTanimAlanMap.containsKey(parentKey) ? personelDinamikTanimAlanMap.get(parentKey) : null;
				if (parentTanim == null) {
					parentTanim = new Tanim();
					parentTanim.setKodu(parentKey);
					parentTanim.setErpKodu(parentKey);
					parentTanim.setTipi(Tanim.TIPI_PERSONEL_DINAMIK_TANIM);
					parentTanim.setParentTanim(genelTanimMap.get(Tanim.TIPI_PERSONEL_DINAMIK_TANIM));
					parentTanim.setGuncelle(Boolean.FALSE);
					parentTanim.setIslemYapan(islemYapan);
					parentTanim.setIslemTarihi(new Date());
					parentTanim.setAciklamatr(parentKey + " Tanımsız");
					parentTanim.setAciklamaen(parentKey + " Tanımsız");
					personelDinamikTanimAlanMap.put(parentKey, parentTanim);

					saveList.add(parentTanim);
				}

				TreeMap<String, Tanim> personelDinamikListeAlanMap = dataMap.get("personelDinamikListeAlanMap");
				String key = parentKey + "_" + tanimKodu;
				tanim = personelDinamikListeAlanMap.containsKey(key) ? personelDinamikListeAlanMap.get(key) : new Tanim();

				if (!tanim.isGuncellendi()) {
					if (tanim.getId() == null) {
						tanim.setParentTanim(parentTanim);
						tanim.setTipi(Tanim.TIPI_PERSONEL_DINAMIK_LISTE_TANIM);
						tanim.setKodu(tanimKodu);
						tanim.setErpKodu(tanimKodu);
						tanim.setGuncelle(Boolean.FALSE);
						tanim.setIslemYapan(islemYapan);
						tanim.setIslemTarihi(new Date());
						personelDinamikListeAlanMap.put(key, tanim);

					}
					ekle = tanim.getId() == null || !tanim.getDurum().booleanValue();
					if (aciklama != null) {
						if (aciklama.indexOf("&amp;") > 0)
							aciklama = PdksUtil.replaceAllManuel(aciklama, "&amp;", "&");
						if (tanim.getAciklamatr() == null || !tanim.getAciklamatr().equalsIgnoreCase(aciklama))
							ekle = true;
						tanim.setAciklamaen(aciklama);
						tanim.setAciklamatr(aciklama);

					}
					tanim.setDurum(Boolean.TRUE);
					tanim.setGuncellendi(Boolean.TRUE);
					if (ekle)
						saveList.add(tanim);
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tanim;

	}

	/**
	 * @param ekKodu
	 * @param parentKey
	 * @param tanimKodu
	 * @param aciklama
	 * @param dataMap
	 * @param saveList
	 * @return
	 */
	private Tanim getTanim(String ekKodu, String parentKey, String tanimKodu, String aciklama, TreeMap<String, TreeMap> dataMap, List saveList) {
		Tanim tanim = null;
		try {
			boolean ekle = false;
			if (PdksUtil.hasStringValue(tanimKodu) && PdksUtil.hasStringValue(aciklama)) {
				TreeMap<String, Tanim> personelEKSahaMap = dataMap.get("personelEKSahaMap");
				Tanim parentTanim = personelEKSahaMap.containsKey(parentKey) ? personelEKSahaMap.get(parentKey) : null;
				String tipi = parentTanim != null ? Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA : parentKey;
				TreeMap<String, Tanim> personelEKSahaVeriMap = dataMap.get("personelEKSahaVeriMap");
				String key = parentKey + "_" + (ekKodu != null ? ekKodu : "") + tanimKodu;
				tanim = personelEKSahaVeriMap.containsKey(key) ? personelEKSahaVeriMap.get(key) : new Tanim();
				if (tanim.getId() == null && parentTanim == null) {
					if (genelTanimMap.containsKey(tipi))
						parentTanim = genelTanimMap.get(tipi);
				}
				if (!tanim.isGuncellendi()) {
					if (tanim.getId() == null) {
						tanim.setParentTanim(parentTanim);
						tanim.setTipi(tipi);
						tanim.setKodu((ekKodu != null ? ekKodu : "") + tanimKodu);
						tanim.setErpKodu(tanimKodu);
						tanim.setGuncelle(Boolean.FALSE);
						tanim.setIslemYapan(islemYapan);
						tanim.setIslemTarihi(new Date());
						personelEKSahaVeriMap.put(key, tanim);

					}
					ekle = tanim.getId() == null || !tanim.getDurum().booleanValue();
					if (aciklama != null) {
						if (aciklama.indexOf("&amp;") > 0)
							aciklama = PdksUtil.replaceAllManuel(aciklama, "&amp;", "&");
						if (tanim.getAciklamatr() == null || PdksUtil.isStrDegisti(tanim.getAciklamatr(), aciklama))
							ekle = true;
						tanim.setAciklamaen(aciklama);
						tanim.setAciklamatr(aciklama);

					}
					tanim.setDurum(Boolean.TRUE);
					tanim.setGuncellendi(Boolean.TRUE);
					if (ekle && saveList != null)
						saveList.add(tanim);
				}
				if (tanim.getParentTanim() == null && genelTanimMap != null) {
					if (genelTanimMap.containsKey(tanim.getTipi())) {
						parentTanim = genelTanimMap.get(tanim.getTipi());
						tanim.setParentTanim(parentTanim);
						tanim.setGuncelle(Boolean.FALSE);
						tanim.setIslemYapan(islemYapan);
						tanim.setIslemTarihi(new Date());
						if (!ekle && saveList != null)
							saveList.add(tanim);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return tanim;

	}

	/**
	 * @param tip
	 * @return
	 */
	private void tanimGetir(TreeMap<String, Tanim> map, String tip) {
		fields.clear();
		fields.put("tipi", tip);
		List<Tanim> list = pdksDAO.getObjectByInnerObjectList(fields, Tanim.class);
		for (Tanim tanim : list)
			if (!tip.equals(Tanim.TIPI_TESIS))
				map.put(tip + "_" + tanim.getErpKodu(), tanim);
			else
				map.put(tip + "_" + tanim.getKodu(), tanim);
	}

	/**
	 * @param str
	 * @return
	 */
	private boolean notEmptyStr(String str) {
		boolean empt = sistemDestekVar && PdksUtil.hasStringValue(str);
		return empt;

	}

	/**
	 * @param str
	 * @param pattern
	 * @return
	 */
	private Date getTarih(String str, String pattern) {
		Date tarih = null;
		if (str != null && str.length() == pattern.length()) {
			tarih = PdksUtil.convertToJavaDate(str, pattern);
			if (tarih != null) {
				if (minDate == null)
					minDate = PdksUtil.convertToJavaDate("1900-01-01", FORMAT_DATE);
				if (minDate != null && tarih.before(minDate))
					tarih = null;
			}
		}

		return tarih;
	}

	/**
	 * @param str1
	 * @param str2
	 * @return
	 */
	private boolean isBenzer(String str1, String str2) {
		boolean benzer = false;
		if (PdksUtil.hasStringValue(str1) && PdksUtil.hasStringValue(str2)) {
			benzer = str1.equals(str2);
			if (!benzer) {
				String strNew1 = PdksUtil.replaceAllManuel(PdksUtil.setTurkishStr(str1).toUpperCase(Locale.ENGLISH), " ", "");
				String strNew2 = PdksUtil.replaceAllManuel(PdksUtil.setTurkishStr(str2).toUpperCase(Locale.ENGLISH), " ", "");
				benzer = strNew1.equals(strNew2) || strNew1.indexOf(strNew2) >= 0 || strNew2.indexOf(strNew1) >= 0;
			}
		}
		if (!benzer)
			logger.debug("[ " + str1 + " <> " + str2 + " ]");
		return benzer;
	}

	/**
	 * @param personelKGS
	 * @return
	 */
	private Date getGrubaGirisTarihi(PersonelKGS personelKGS) {
		Date date = null;
		if (personelKGS != null && PdksUtil.hasStringValue(personelKGS.getKimlikNo())) {
			HashMap fields = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select P.* from " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append(" inner join " + PersonelKGS.TABLE_NAME + " K " + PdksVeriOrtakAktar.getJoinLOCK() + " on K." + PersonelKGS.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_KGS_PERSONEL);
			sb.append(" where K." + PersonelKGS.COLUMN_NAME_KIMLIK_NO + " = :k ");
			sb.append(" order by P." + Personel.COLUMN_NAME_ISE_BASLAMA_TARIHI);
			fields.put("k", personelKGS.getKimlikNo());
			List<Personel> list = pdksDAO.getNativeSQLList(fields, sb, Personel.class);
			if (list.size() > 1)
				date = list.get(0).getIseBaslamaTarihi();
		}
		return date;
	}

	public String personelNoAciklama() {
		String personelNoAciklama = getBaslikAciklama("personelNoAciklama", "Personel No");
		return personelNoAciklama;
	}

	private String sirketAciklama() {
		String sirketAciklama = getBaslikAciklama("sirketAciklama", "Şirket");
		return sirketAciklama;
	}

	private String yoneticiAciklama() {
		String yoneticiAciklama = getBaslikAciklama("yoneticiAciklama", "Yönetici");
		return yoneticiAciklama;
	}

	/**
	 * 
	 */
	private void setIkinciYoneticiSifirla() {
		Boolean yonetici2ERPKontrol = Boolean.FALSE;
		if (mailMap.containsKey("yonetici2ERPKontrol")) {
			String str = (String) mailMap.get("yonetici2ERPKontrol");
			yonetici2ERPKontrol = str.equals("1");
		}

		if (!yonetici2ERPKontrol) {
			LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
			map.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_GET_IKINCI_YONETICI_UPDATE");
			pdksDAO.execSP(map);
		}

	}

	/**
	 * @param sirket
	 * @param map
	 * @return
	 */
	private Object getCalismaModel_VardiyaSablonByMap(Sirket sirket, TreeMap map) {
		Object object = null;
		Departman departman = null;
		if (map != null && !map.isEmpty()) {
			String key = "";
			if (sirket != null && sirket.getDepartman().isAdminMi()) {
				key = "0_" + sirket.getId();
				object = map.get(key);
				if (object == null && departman == null)
					departman = sirket.getDepartman();
			}
			if (object == null) {
				if (departman != null) {
					key = departman.getId() + "_0";
					object = map.get(key);
				}
				if (object == null) {
					key = "0_0";
					object = map.get(key);
				}
			}
		}
		return object;
	}

	/**
	 * @param yoneticiList
	 * @param dataMap
	 * @param personelList
	 * @param tip
	 */
	private void personelVeriYaz(List<Personel> yoneticiList, TreeMap<String, TreeMap> dataMap, List<String> personelList, String tip) {
		List<PersonelERP> hataList = null;
		TreeMap<String, PersonelERP> personelERPMap = dataMap.get("personelERPMap"), personelYoneticiERPMap = new TreeMap<String, PersonelERP>();
		TreeMap<String, PersonelKGS> personelKGSMap = dataMap.get("personelKGSMap");
		TreeMap<String, Personel> personelPDKSMap = dataMap.get("personelPDKSMap");
		TreeMap<String, ERPPersonel> personelERPHataliMap = dataMap.get("personelERPHataliMap");
		TreeMap<String, Personel> personelDigerMap = dataMap.get("personelDigerMap");
		TreeMap<String, PersonelDinamikAlan> personelDinamikAlanMap = dataMap.get("personelDinamikAlanMap");

		TreeMap<String, Sirket> sirketMap = dataMap.get("sirketMap");

		TreeMap<String, VardiyaSablonu> sablonMap = dataMap.get("sablonMap");
		TreeMap<String, CalismaModeli> cmMap = dataMap.get("cmMap");
		String yoneticiAciklama = yoneticiAciklama();
		boolean yoneticiBul = tip.equals("Y"), kendiYonetici = false;
		List saveList = new ArrayList();
		HashMap<String, Boolean> map1 = new HashMap<String, Boolean>();
		String genelMudurERPKoduStr = mailMap.containsKey("genelMudurERPKodu") ? (String) mailMap.get("genelMudurERPKodu") : "";
		List<String> genelMudurERPKodlari = !PdksUtil.hasStringValue(genelMudurERPKoduStr) ? new ArrayList<String>() : PdksUtil.getListByString(genelMudurERPKoduStr, null);
		String calisanIstenAyrilmaTarihi = mailMap.containsKey("calisanIstenAyrilmaTarihi") ? (String) mailMap.get("calisanIstenAyrilmaTarihi") : "";
		String iskurManuelGirisStr = mailMap.containsKey("iskurManuelGiris") ? (String) mailMap.get("iskurManuelGiris") : "";
		String yoneticiMailKontrol = mailMap.containsKey("yoneticiMailKontrol") ? (String) mailMap.get("yoneticiMailKontrol") : "1";
		String yoneticiERPKontrol = mailMap.containsKey("yoneticiERPKontrol") ? (String) mailMap.get("yoneticiERPKontrol") : "1";
		String personelERPGecmisTarihKontrolStr = mailMap.containsKey("personelERPGecmisTarihKontrol") ? (String) mailMap.get("personelERPGecmisTarihKontrol") : "0";
		String yonetici2ERPKontrolStr = mailMap.containsKey("yonetici2ERPKontrol") ? (String) mailMap.get("yonetici2ERPKontrol") : "";
		String sistemBaslangicYiliStr = mailMap.containsKey("sistemBaslangicYili") ? (String) mailMap.get("sistemBaslangicYili") : "0";
		boolean bayanSoyadKontrol = mailMap.containsKey("bayanSoyadKontrol") && ((String) mailMap.get("bayanSoyadKontrol")).equals("1");
		boolean yonetici2ERPKontrol = yonetici2ERPKontrolStr.equals("1");
		Date calisanTarihi = PdksUtil.convertToJavaDate("99991231", "yyyyMMdd");
		boolean izinERPUpdate = false, iskurManuelGiris = iskurManuelGirisStr != null && iskurManuelGirisStr.equals("1");
		if (mailMap.containsKey("izinERPUpdate")) {
			String str = (String) mailMap.get("izinERPUpdate");
			izinERPUpdate = str.equals("1");
		}
		boolean yoneticiERP1Kontrol = true;
		if (mailMap.containsKey("yoneticiERP1Kontrol"))
			yoneticiERP1Kontrol = !PdksUtil.hasStringValue(((String) mailMap.get("yoneticiERP1Kontrol")));
		Date gecmisTarih = null;

		int personelERPGecmisTarihKontrol = -1;
		try {
			personelERPGecmisTarihKontrol = Integer.parseInt(personelERPGecmisTarihKontrolStr);
		} catch (Exception e) {
			personelERPGecmisTarihKontrol = -1;
		}
		if (personelERPGecmisTarihKontrol > 0 && !sistemBaslangicYiliStr.equals("0")) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MONTH, -personelERPGecmisTarihKontrol);
			int yil = cal.get(Calendar.YEAR);
			try {
				int sistemBaslangicYili = Integer.parseInt(sistemBaslangicYiliStr);
				if (sistemBaslangicYili < yil)
					gecmisTarih = PdksUtil.getDate(cal.getTime());
			} catch (Exception e) {
			}

		}
		List<String> kidemHataList = new ArrayList<String>();
		TreeMap<String, Tanim> personelEKSahaMap = dataMap.get("personelEKSahaMap");
		Tanim parentDepartman = personelEKSahaMap != null && personelEKSahaMap.containsKey("ekSaha1") ? personelEKSahaMap.get("ekSaha1") : null;

		fields.clear();

		Tanim parentBordroTanim = getSQLTanim(Tanim.TIPI_GENEL_TANIM, Tanim.TIPI_BORDRO_ALT_BIRIMI, null);
		String parentBordroTanimKodu = Tanim.TIPI_BORDRO_ALT_BIRIMI;
		parentBordroTanimKoduStr = "";
		String durumParentBordroTanimKoduStr = "";
		if (parentBordroTanim != null) {
			parentBordroTanimKoduStr = parentBordroTanim.getErpKodu();
			if (parentBordroTanimKoduStr == null)
				parentBordroTanimKoduStr = "";
			else {
				parentBordroTanimKodu = parentBordroTanimKoduStr.trim();
			}

		} else
			parentBordroTanimKoduStr = Tanim.TIPI_BORDRO_ALT_BIRIMI;
		parentBordroTanimKoduStr = parentBordroTanimKodu;
		// if (parentBordroTanimKoduStr.startsWith("eksaha"))
		// parentBordroTanimKodu = Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA;

		List<String> parentBordroTanimKoduList = PdksUtil.getListByString(parentBordroTanimKoduStr, null);
		if (parentBordroTanimKoduList.size() <= 2) {
			switch (parentBordroTanimKoduList.size()) {
			case 1:
				durumParentBordroTanimKoduStr = parentBordroTanimKoduList.get(0);
				break;
			case 2:
				parentBordroTanimKoduStr = parentBordroTanimKoduList.get(0);
				durumParentBordroTanimKoduStr = parentBordroTanimKoduList.get(1);
				break;
			default:
				break;
			}
		}
		ekSahaAdi = parentBordroTanimKoduStr != null ? parentBordroTanimKoduStr.toLowerCase(Constants.TR_LOCALE) : "";
		Tanim istenAyrilanEkSaha = null;
		if (PdksUtil.hasStringValue(ekSahaAdi)) {
			if (ekSahaAdi.startsWith("eksaha")) {
				Tanim parentEkSaha = personelEKSahaMap != null && personelEKSahaMap.containsKey(parentBordroTanimKoduStr) ? personelEKSahaMap.get(parentBordroTanimKoduStr) : null;
				if (parentEkSaha != null) {
					fields.clear();

					List<Tanim> list = getSQLTanimList(Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA, "END", null);
					for (Tanim tanim : list) {
						if (tanim.getParentTanim() != null && tanim.getParentTanim().equals(parentEkSaha.getId()))
							istenAyrilanEkSaha = tanim;
					}
					list = null;
				}
			}

		}
		altBolumDurum = ekSahaAdi.startsWith("eksaha4");
		boolean durumParentBordroTanimKodu = durumParentBordroTanimKoduStr.equalsIgnoreCase("true");
		Tanim parentBolum = personelEKSahaMap != null && personelEKSahaMap.containsKey("ekSaha3") ? personelEKSahaMap.get("ekSaha3") : null;
		HashMap map = new HashMap();
		if (bosDepartman == null && departmanYoneticiRolVar && parentDepartman != null && mailMap.containsKey("bosDepartmanKodu")) {
			String bosDepartmanKodu = (String) mailMap.get("bosDepartmanKodu");
			List<Tanim> list = getSQLTanimList(Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA, bosDepartmanKodu, Boolean.FALSE);
			for (Tanim tanim : list) {
				if (tanim.getParentTanim() != null && tanim.getParentTanim().getId().equals(parentDepartman.getId()))
					bosDepartman = tanim;
			}
			list = null;
		}
		Date lastDate = getTarih(LAST_DATE, FORMAT_DATE);
		Personel personelTest = new Personel();

		boolean sirketBirlestirme = mailMap.containsKey("sirketKoduBirlestirme");
		String personelERPGuncelleme = mailMap.containsKey("personelERPOku") ? (String) mailMap.get("personelERPOku") : "";
		String personelERPTableViewAdi = mailMap.containsKey("personelERPTableViewAdi") ? (String) mailMap.get("personelERPTableViewAdi") : "";
		boolean personelERPGuncellemeDurum = PdksUtil.hasStringValue(personelERPTableViewAdi) || (personelERPGuncelleme != null && personelERPGuncelleme.equalsIgnoreCase("M"));
		List<CalismaModeli> modelList = new ArrayList<CalismaModeli>();
		List<VardiyaSablonu> sablonList = new ArrayList<VardiyaSablonu>();
		String tumPersonelDenklestirme = mailMap.containsKey("tumPersonelDenklestirme") ? (String) mailMap.get("tumPersonelDenklestirme") : "";
		String uygulamaBordro = mailMap.containsKey("uygulamaBordro") ? (String) mailMap.get("uygulamaBordro") : "Bordro Uygulaması ";

		for (String personelNo : personelList) {
			kidemHataList.clear();
			boolean calisiyor = false;
			Personel personel = null;

			if (personelERPMap.containsKey(personelNo)) {
				PersonelERP personelERP = personelERPMap.get(personelNo);
				Tanim bolum = null;
				Sirket sirket = null;
				personelSirket = PdksUtil.hasStringValue(personelERP.getSirketKodu()) && sirketMap.containsKey(personelERP.getSirketKodu()) ? sirketMap.get(personelERP.getSirketKodu()) : null;
				LinkedHashMap<String, Liste> personelListeMap = new LinkedHashMap<String, Liste>();
				for (int i = 1; i <= 10; i++) {
					String key = "TanimAlan" + PdksUtil.textBaslangicinaKarakterEkle(String.valueOf(i), '0', 2);
					String aciklama = (String) PdksUtil.getMethodObject(personelERP, "getDiger" + key, null);
					String kodu = (String) PdksUtil.getMethodObject(personelERP, "getDiger" + key + "Kodu", null);
					if (PdksUtil.hasStringValue(kodu))
						personelListeMap.put("diger" + key, new Liste(kodu, PdksUtil.hasStringValue(aciklama) ? aciklama : "Tanımsız"));

				}
				String yoneticiNo = personelERP.getYoneticiPerNo() != null ? personelERP.getYoneticiPerNo().trim() : "";
				String yonetici2No = personelERP.getYonetici2PerNo() != null ? personelERP.getYonetici2PerNo().trim() : "";
				boolean yoneticiPersonel = yoneticiBul || yoneticiNo.equals(personelNo);

				Tanim cinsiyet = getTanim(null, Tanim.TIPI_CINSIYET, personelERP.getCinsiyetKodu(), personelERP.getCinsiyeti(), dataMap, saveList);
				Tanim personelTipi = getTanim(null, Tanim.TIPI_PERSONEL_TIPI, personelERP.getPersonelTipiKodu(), personelERP.getPersonelTipi(), dataMap, saveList);
				String soyadi = personelERP.getSoyadi() != null ? new String(personelERP.getSoyadi()) : null;
				personelTest.setCinsiyet(bayanSoyadKontrol || cinsiyet != null ? cinsiyet : null);
				boolean bayanSoyad = false;
				Personel personelSecili = null;
				Date iseBaslamaTarihi = getTarih(personelERP.getIseGirisTarihi(), FORMAT_DATE);
				boolean genelMudurDurum = personelERP.getGorevKodu() != null && genelMudurERPKodlari.contains(personelERP.getGorevKodu());
				if (PdksUtil.hasStringValue(personelERP.getIstenAyrilmaTarihi()) == false || personelERP.getIstenAyrilmaTarihi().equals(calisanIstenAyrilmaTarihi))
					personelERP.setIstenAyrilmaTarihi(LAST_DATE);

				Date grubaGirisTarihi = getTarih(personelERP.getGrubaGirisTarihi(), FORMAT_DATE);
				Date istenAyrilisTarihi = getTarih(personelERP.getIstenAyrilmaTarihi(), FORMAT_DATE);
				if (yoneticiPersonel) {
					if (!kendiYonetici)
						kendiYonetici = yoneticiNo.equals(personelNo);
					personelYoneticiERPMap.put(personelNo, personelERP);
					personelERPMap.remove(personelNo);
				}
				ERPPersonel erpPersonel = null;
				if (vardiyaSablonu == null)
					addHatalist(hataList, personelERP, null, "Beyaz yaka şablonu bulunamadı!");
				if (personelERP.getSirketKodu() == null || !sirketMap.containsKey(personelERP.getSirketKodu())) {
					if (PdksUtil.hasStringValue(personelERP.getSirketKodu()) == false)
						addHatalist(hataList, personelERP, null, sirketAciklama() + " kodu boş olamaz!");
					else {

						if (sistemDestekVar && mailMap.containsKey("erpSirketOlustur") && mailMap.get("erpSirketOlustur").equals("1")) {
							fields.clear();
							fields.put("durum", Boolean.TRUE);
							fields.put("admin", Boolean.TRUE);
							Departman departman = (Departman) pdksDAO.getObjectByInnerObject(fields, Departman.class);
							if (departman != null) {
								sirket = new Sirket();
								sirket.setDepartman(departman);
								if (departman != null)
									sirket.setIsAramaGunlukSaat(departman.getIsAramaGunlukSaat());
								sirket.setFazlaMesaiTalepGirilebilir(departman.getFazlaMesaiTalepGirilebilir());
								sirket.setErpKodu(personelERP.getSirketKodu());
								if (mailMap.containsKey("gebeSutIzniDurum")) {
									String gebeSutIzniDurum = (String) mailMap.get("gebeSutIzniDurum");
									sirket.setGebelikSutIzin(gebeSutIzniDurum.equals("1"));
								}
								if (mailMap.containsKey("uygulamaTipi")) {
									String uygulamaTipi = (String) mailMap.get("uygulamaTipi");
									sirket.setSuaOlabilir(uygulamaTipi.equalsIgnoreCase("H"));
								}
								sirket.setErpDurum(Boolean.TRUE);
								sirket.setAd(personelERP.getSirketAdi());
								sirket.setAciklama(personelERP.getSirketAdi());
								sirket.setPdks(Boolean.TRUE);
								sirket.setFazlaMesai(Boolean.TRUE);
								sirket.setDurum(Boolean.TRUE);
								sirket.setOlusturmaTarihi(bugun);
								sirket.setOlusturanUser(islemYapan);
								pdksDAO.saveObject(sirket);
							}
						}
						if (sirket != null) {
							sirketMap.put(personelERP.getSirketKodu(), sirket);
							sirket.setGuncellendi(Boolean.TRUE);
						} else
							addHatalist(hataList, personelERP, null, personelERP.getSirketKodu() + " hatalı " + sirketAciklama() + " kodu!");

					}
				}
				PersonelKGS personelKGSData = personelKGSMap.containsKey(personelNo) ? personelKGSMap.get(personelNo) : null, personelKGSBos = null;
				if (personelKGSData == null) {
					map.clear();
					if (kapiSirket != null)
						map.put("kapiSirket.id", kapiSirket.getId());
					map.put("ad", personelERP.getAdi());
					map.put("soyad", personelERP.getSoyadi());
					map.put("durum", Boolean.TRUE);
					List<PersonelKGS> list = pdksDAO.getObjectByInnerObjectList(map, PersonelKGS.class);
					if (list.size() == 1) {
						personelKGSBos = list.get(0);
						map.clear();
						map.put("pdksSicilNo", personelKGSBos.getSicilNo());
						List<Personel> personelPDKSList = pdksDAO.getObjectByInnerObjectList(map, Personel.class);
						if (!personelPDKSList.isEmpty())
							personelKGSBos = null;
					}
				}
				String adSoyadERP = (personelERP.getAdi() != null ? personelERP.getAdi().trim() : "Ad tanımsız") + " " + (personelERP.getSoyadi() != null ? personelERP.getSoyadi().trim() : "Soyad tanımsız");
				String ad = PdksUtil.getCutFirstSpaces(personelERP.getAdi());
				String soyad = PdksUtil.getCutFirstSpaces(personelERP.getSoyadi());
				if (personelKGSData == null || personelKGSData.getId() < 0L) {
					boolean kayitYok = true;
					if (personelKGSData == null && mailMap.containsKey("personelKGSDataOlustur") && personelERP.getAdi() != null && personelERP.getSoyadi() != null && personelERP.getPersonelNo() != null) {
						LinkedHashMap<String, Object> map2 = new LinkedHashMap<String, Object>();
						if (kapiSirket != null)
							map2.put("kapiSirket.id", kapiSirket.getId());
						map2.put("ad", personelERP.getAdi());
						map2.put("soyad", personelERP.getSoyadi());
						map2.put("perNo", personelERP.getPersonelNo());
						map2.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_ADD_PERSONEL_KGS");
						try {
							List<PersonelKGS> list2 = pdksDAO.execSPList(map2, PersonelKGS.class);
							if (!list2.isEmpty())
								personelKGSData = list2.get(0);
						} catch (Exception e) {
						}
					}
					if (personelKGSData != null && personelKGSData.getId() > 0L)
						personelKGSMap.put(personelNo, personelKGSData);
					else {

						if (gecmisTarih != null && iseBaslamaTarihi != null && iseBaslamaTarihi.before(gecmisTarih))
							kayitYok = false;

						if (kayitYok && (istenAyrilisTarihi == null || !istenAyrilisTarihi.before(ayBasi))) {
							String goreviStr = PdksUtil.hasStringValue(personelERP.getGorevi()) ? "Görevi : " + personelERP.getGorevi() : "";
							String mesaj = adSoyadERP + " personel'in " + kapiGiris + " " + personelNo + " personel no bilgisi bulunamadı!"
									+ (iseBaslamaTarihi != null ? " [ İşe giriş tarihi : " + PdksUtil.convertToDateString(iseBaslamaTarihi, PdksUtil.getDateFormat()) + (goreviStr.equals("") ? "" : " - " + goreviStr) + " ]" : (goreviStr.equals("") ? "" : " [ " + goreviStr + " ]"));
							String ek = "";

							if (istenAyrilisTarihi != null && istenAyrilisTarihi.before(bugun))
								ek = " (İşten ayrılma tarihi : " + personelERP.getIstenAyrilmaTarihi();

							if (personelKGSBos != null && PdksUtil.hasStringValue(personelKGSBos.getSicilNo())) {
								if (!PdksUtil.hasStringValue(ek))
									ek += "(";
								else
									ek += " - ";
								ek += kapiGiris + " personel no " + personelKGSBos.getSicilNo().trim();

							}
							if (PdksUtil.hasStringValue(ek))
								ek += ")";
							mesaj += ek;

							addHatalist(hataList, personelERP, null, mesaj);
							bolum = getTanim(null, "ekSaha3", personelERP.getBolumKodu(), personelERP.getBolumAdi(), dataMap, null);
							if (bolum == null && parentBolum != null)
								addHatalist(hataList, personelERP, null, uygulamaBordro + " " + parentBolum.getAciklamatr() + " bilgisi boş olamaz!");
							if (!personelERPHataliMap.containsKey(personelNo)) {
								erpPersonel = new ERPPersonel();
								erpPersonel.setSicilNo(personelNo);
								personelERPHataliMap.put(personelNo, erpPersonel);
							} else {
								erpPersonel = personelERPHataliMap.get(personelNo);
								if (erpPersonel.getDurum())
									erpPersonel.setIslemZamani(new Date());
							}
							if (erpPersonel.getDurum()) {
								erpPersonel.setAd(PdksUtil.getCutFirstSpaces(personelERP.getAdi()));
								erpPersonel.setSoyad(PdksUtil.getCutFirstSpaces(personelERP.getSoyadi()));
								saveList.add(erpPersonel);
							}

						}
					}

				} else {
					if (tip.equals("P")) {

						String erpAdSoyad = PdksUtil.setTurkishStr(PdksUtil.getAdSoyad(personelERP.getAdi(), personelERP.getSoyadi()));
						String kgsAdSoyad = PdksUtil.setTurkishStr(PdksUtil.getAdSoyad(personelKGSData.getAd(), personelKGSData.getSoyad()));

						if (!kgsAdSoyad.equalsIgnoreCase(erpAdSoyad)) {
							if (personelERP.getAdi() == null || personelERP.getSoyadi() == null) {
								String mesaj = "adı ve soyadı";
								if (personelERP.getAdi() != null) {
									mesaj = "soyadı";
									bayanSoyad = personelTest.getCinsiyetBayan();
									if (bayanSoyad)
										personelERP.setSoyadi(personelKGSData.getSoyad());

								}

								else if (personelERP.getSoyadi() != null)
									mesaj = "adı";

								mesaj = personelNo + " personel " + mesaj + " girilmemiş!  ";
								addHatalist(bayanSoyad == false ? personelERP.getHataList() : kidemHataList, PdksUtil.replaceAllManuel(mesaj, "  ", " "));

							} else {
								PersonelKGS personelKGS = personelKGSMap.get(personelNo);
								String kgsAd = personelKGS.getAd();
								if (sistemDestekVar && ad != null && kgsAd != null && !ad.equals(kgsAd)) {
									if (ad.indexOf(" ") > 0 || kgsAd.indexOf(" ") > 0) {
										if (ad.indexOf(" ") > 0)
											ad = PdksUtil.replaceAllManuel(ad, " ", "");
										if (kgsAd.indexOf(" ") > 0)
											kgsAd = PdksUtil.replaceAllManuel(kgsAd, " ", "");
									}
								}
								boolean adiUyumlu = isBenzer(ad, kgsAd);
								boolean soyadiUyumlu = isBenzer(personelKGS.getSoyad(), soyad);
								if (!adiUyumlu || !soyadiUyumlu) {
									String mesaj = "";
									if (!adiUyumlu && !soyadiUyumlu)
										mesaj = "adı ve soyadı";
									if (!adiUyumlu) {
										mesaj = "adı";
									} else if (!soyadiUyumlu) {
										mesaj = "soyadi";
										bayanSoyad = personelTest.getCinsiyetBayan();
										if (bayanSoyad)
											personelERP.setSoyadi(personelKGS.getSoyad());

									}
									mesaj = personelNo + " personel " + mesaj + " uyumsuz! ( " + adSoyadERP + " farklı " + personelKGS.getAdSoyad() + (personelKGS.getKapiSirket() != null ? " [ " + personelKGS.getKapiSirket().getAciklama() + " ] " : "") + " ) ";
									addHatalist(bayanSoyad == false ? personelERP.getHataList() : kidemHataList, PdksUtil.replaceAllManuel(mesaj, "  ", " "));

								}
							}
						} else {
							if (personelERP.getAdi() == null)
								personelERP.setAdi("");
							if (personelERP.getSoyadi() == null)
								personelERP.setSoyadi("");
						}

					}
					if (personelERPHataliMap.containsKey(personelNo)) {
						ERPPersonel erpPersonel2 = personelERPHataliMap.get(personelNo);
						try {
							pdksDAO.deleteObject(erpPersonel2);
							personelERPHataliMap.remove(personelNo);
						} catch (Exception e) {
						}
					}
				}

				if (!sirketMap.containsKey(personelERP.getSirketKodu()))
					addHatalist(hataList, personelERP, null, personelERP.getSirketKodu() + " " + personelERP.getSirketAdi() + " " + sirketAciklama() + " bilgisi bulunamadı!");

				if (personelERP.getHataList().isEmpty() && personelKGSMap.containsKey(personelNo)) {
					PersonelKGS personelKGS = personelKGSMap.get(personelNo);
					personel = personelPDKSMap.containsKey(personelNo) ? personelPDKSMap.get(personelNo) : null;
					if (personel != null) {
						personel.setPersonelTipi(personelTipi);
						sablonList.clear();
						modelList.clear();
						PersonelKGS personelKGS2 = personel.getPersonelKGS();
						if (kapiSirket != null && personelKGS2.getKapiSirket() != null && !personelKGS2.getId().equals(personelKGS.getId()) && !personelKGS2.getKapiSirket().getId().equals(kapiSirket.getId())) {
							boolean adBenzer = isBenzer(personelKGS.getAd(), personelKGS2.getAd());
							boolean soyadBenzer = isBenzer(personelKGS.getSoyad(), personelKGS2.getSoyad());
							boolean kimlikBenzer = (PdksUtil.hasStringValue(personelKGS.getKimlikNo()) && PdksUtil.hasStringValue(personelKGS2.getKimlikNo())) || (personelKGS.getKimlikNo() != null && personelKGS.getKimlikNo() != null && personelKGS.getKimlikNo().equals(personelKGS2.getKimlikNo()));
							if (soyadBenzer && adBenzer && kimlikBenzer)
								personel.setPersonelKGS(personelKGS);
							else {
								String uygulamaFark = kapiSirket.getAciklama() + " ile " + personelKGS2.getKapiSirket().getAciklama();
								if (adBenzer) {
									bayanSoyad = personelTest.getCinsiyetBayan();
									if (bayanSoyad)
										personelERP.setSoyadi(personelKGS2.getSoyad());
									addHatalist(bayanSoyad == false ? personelERP.getHataList() : kidemHataList, PdksUtil.replaceAllManuel(uygulamaFark + " soyad uyumsuz!", "  ", " "));
								}

								else if (soyadBenzer)
									personelERP.getHataList().add(uygulamaFark + " ad uyumsuz!");
								else if (kimlikBenzer)
									personelERP.getHataList().add(uygulamaFark + " ad soyad uyumsuz!");
								if (!kimlikBenzer)
									personelERP.getHataList().add(uygulamaFark + " kimlik numarası uyumsuz!");
							}
						}

					} else if (personelDigerMap.containsKey(personelNo)) {
						personel = personelDigerMap.get(personelNo);
						personel.setPersonelKGS(personelKGS);
						personel.setPdksSicilNo(personelNo);
					}

					if (personel == null) {
						if (personelKGS.getDurum().booleanValue() == false) {
							continue;
						}
						personel = new Personel();
						if (!tumPersonelDenklestirme.equals(""))
							personel.setPdks(tumPersonelDenklestirme.equals("1"));
						personel.setDegisti(Boolean.TRUE);
						personel.setDurum(Boolean.TRUE);
						if (!yoneticiPersonel || yoneticiMailKontrol.equals("1"))
							personel.setMailTakip(Boolean.TRUE);
						personel.setPdks(Boolean.TRUE);
						personel.setPersonelKGS(personelKGS);
						personelPDKSMap.put(personelNo, personel);

					} else {
						personel.setDegisti(Boolean.FALSE);
						PersonelKGS personelKGSKayitli = personel.getPersonelKGS();
						if (!personelKGSKayitli.getDurum()) {
							map.clear();
							if (kapiSirket != null)
								map.put("kapiSirket.id", kapiSirket.getId());
							map.put("sicilNo", personelNo);
							map.put("durum", Boolean.TRUE);
							List<PersonelKGS> list = pdksDAO.getObjectByInnerObjectList(map, PersonelKGS.class);
							if (!list.isEmpty()) {
								for (PersonelKGS personelKGSDiger : list) {
									boolean adiUyumlu = isBenzer(personelKGSDiger.getAd(), ad);
									boolean soyadiUyumlu = isBenzer(personelKGSDiger.getSoyad(), soyad);
									if (adiUyumlu && soyadiUyumlu) {
										personel.setPersonelKGS(personelKGSDiger);

										break;
									}
								}
							}
						}
						if (!personelNo.equals(personelKGSKayitli.getSicilNo())) {
							personel.setPersonelKGS(personelKGS);
						}
						if (yoneticiPersonel && !yoneticiMailKontrol.equals("1"))
							personel.setMailTakip(Boolean.FALSE);
					}

					personel.setCinsiyet(bayanSoyadKontrol || cinsiyet != null ? cinsiyet : null);

					personelERP.setSoyadi(soyadi);
					personelSecili = personel;
					sirket = sirketMap.get(personelERP.getSirketKodu());
					if (sirket != null) {
						if (!sirket.isGuncellendi().booleanValue()) {
							if (PdksUtil.isStrDegisti(sirket.getAd(), personelERP.getSirketAdi())) {
								sirket.setAd(personelERP.getSirketAdi());
								sirket.setGuncellemeTarihi(new Date());
								sirket.setGuncelleyenUser(islemYapan);
								saveList.add(sirket);
							}
							sirket.setGuncellendi(Boolean.TRUE);
						}
						if (personel.getId() == null) {
							TreeMap<String, Boolean> izinMap = getOnaysizIzinDurumMap(sirket);
							boolean onaysizIzinDurum = izinMap.containsKey("onaysizIzinDurum");
							boolean ikinciYoneticiIzinOnayla = izinMap.containsKey("ikinciYoneticiIzinOnaySec");
							personel.setIkinciYoneticiIzinOnayla(ikinciYoneticiIzinOnayla);
							personel.setOnaysizIzinKullanilir(onaysizIzinDurum);
						}
					}
					personelSirket = sirket;
					personel.setSirket(sirket);
					if (personel.getId() == null) {
						boolean fazlaMesaiIzinKullan = false;
						if (mailMap.containsKey("fazlaMesaiIzinKullan")) {
							String str = (String) mailMap.get("fazlaMesaiIzinKullan");
							if (PdksUtil.hasStringValue(str) && sirket != null && sirket.getFazlaMesaiIzinKullan() != null)
								fazlaMesaiIzinKullan = str.equals("1") && sirket.getFazlaMesaiIzinKullan();
						}
						personel.setFazlaMesaiIzinKullan(fazlaMesaiIzinKullan);
						if (personel.getCalismaModeli() == null) {
							CalismaModeli cm = null;
							if (sirket != null)
								cm = (CalismaModeli) getCalismaModel_VardiyaSablonByMap(sirket, cmMap);
							if (cm == null && modeller.size() == 1)
								cm = modeller.get(0);
							if (cm != null)
								personel.setCalismaModeli(cm);

						}
						if (personel.getSablon() == null) {
							VardiyaSablonu vs = null;
							if (sirket != null)
								vs = (VardiyaSablonu) getCalismaModel_VardiyaSablonByMap(sirket, sablonMap);
							if (vs != null)
								personel.setSablon(vs);
							else {
								sablonList.addAll(sablonlar);
								if (personelTipi != null && personelCalismaModeliVar) {
									long id = personelTipi.getId().longValue();
									for (Iterator iterator = sablonList.iterator(); iterator.hasNext();) {
										VardiyaSablonu vardiyaSablonu = (VardiyaSablonu) iterator.next();
										if (vardiyaSablonu.getCalismaModeli() != null && vardiyaSablonu.getCalismaModeli().getId().longValue() != id)
											iterator.remove();
									}
								}
								if (sablonList.size() == 1)
									personel.setSablon(sablonList.get(0));
							}

						}
						if (sirket != null) {
							personel.setFazlaMesaiOde(sirket.getFazlaMesaiOde() != null && sirket.getFazlaMesaiOde());
						}
					}

					setPersonel(personel, personelERP, FORMAT_DATE);
					if (bolum == null)
						bolum = getTanim(null, "ekSaha3", personelERP.getBolumKodu(), personelERP.getBolumAdi(), dataMap, saveList);

					boolean bolumYok = bolum != null && bolum.getKodu().equalsIgnoreCase("yok");
					Tanim bordroAltAlan = getTanim(null, parentBordroTanimKoduStr, personelERP.getBordroAltAlanKodu(), personelERP.getBordroAltAlanAdi(), dataMap, saveList);
					personel.setTesis(getTanim((sirketBirlestirme ? null : personelERP.getSirketKodu()), Tanim.TIPI_TESIS, personelERP.getTesisKodu(), personelERP.getTesisAdi(), dataMap, saveList));
					personel.setGorevTipi(getTanim(null, Tanim.TIPI_GOREV_TIPI, personelERP.getGorevKodu(), personelERP.getGorevi(), dataMap, saveList));
					personel.setMasrafYeri(getTanim(null, Tanim.TIPI_ERP_MASRAF_YERI, personelERP.getMasrafYeriKodu(), personelERP.getMasrafYeriAdi(), dataMap, saveList));
					Date dogumTarihi = getTarih(personelERP.getDogumTarihi(), FORMAT_DATE);
					Date izinHakEdisTarihi = getTarih(personelERP.getKidemTarihi(), FORMAT_DATE);
					if (iskurManuelGiris) {
						if (iseBaslamaTarihi == null && notEmptyStr(personelERP.getIseGirisTarihi()) == false)
							iseBaslamaTarihi = personel.getIseBaslamaTarihi();
						if (izinHakEdisTarihi == null && notEmptyStr(personelERP.getKidemTarihi()) == false)
							izinHakEdisTarihi = personel.getIzinHakEdisTarihi();
						if (istenAyrilisTarihi == null && notEmptyStr(personelERP.getIstenAyrilmaTarihi()) == false)
							istenAyrilisTarihi = personel.getIstenAyrilisTarihi();
					}

					boolean sanalPersonel = personelERP.getSanalPersonel() != null && personelERP.getSanalPersonel().booleanValue();
					if (iskurManuelGiris && PdksUtil.hasStringValue(personelERP.getIseGirisTarihi()) == false) {
						sanalPersonel = personel.getId() == null || personel.getSanalPersonel();
						if (personel.getSanalPersonel() != null && personel.getSanalPersonel()) {
							iseBaslamaTarihi = personel.getIseBaslamaTarihi();
							izinHakEdisTarihi = personel.getIzinHakEdisTarihi();
							istenAyrilisTarihi = personel.getIstenAyrilisTarihi();
						}
					}
					personel.setIstenAyrilisTarihi(istenAyrilisTarihi);
					personel.setIseBaslamaTarihi(iseBaslamaTarihi);
					if (istenAyrilisTarihi == null) {
						String str = null;
						if (notEmptyStr(personelERP.getIstenAyrilmaTarihi()) == false)
							str = "İşten ayrılma tarihi boş olamaz!";
						else
							str = "İşten ayrılma tarihi hatalıdır! (" + personelERP.getIstenAyrilmaTarihi() + " --> format : " + FORMAT_DATE + " )";
						addHatalist(hataList, personelERP, null, str);
					}
					calisiyor = istenAyrilisTarihi != null && iseBaslamaTarihi != null && personel.isCalisiyor();
					if (iseBaslamaTarihi == null && sanalPersonel == false) {
						if (notEmptyStr(personelERP.getIseGirisTarihi()) == false)
							addHatalist(hataList, personelERP, null, "İşe giriş tarihi boş olamaz!");
						else
							addHatalist(hataList, personelERP, null, "İşe giriş tarihi hatalıdır! (" + personelERP.getIseGirisTarihi() + " --> format : " + FORMAT_DATE + " )");
					}
					if (istenAyrilisTarihi == null && sanalPersonel == false) {
						if (notEmptyStr(personelERP.getIstenAyrilmaTarihi()) == false)
							istenAyrilisTarihi = calisanTarihi;
						else
							addHatalist(hataList, personelERP, null, "İşten ayrılma tarihi hatalıdır! (" + personelERP.getIstenAyrilmaTarihi() + " --> format : " + FORMAT_DATE + " )");
					}
					if (izinGirisiVar && bolumYok == false) {
						if (dogumTarihi == null && calisiyor) {
							String str = null;
							if (notEmptyStr(personelERP.getDogumTarihi()) == false)
								str = "Doğum tarihi boş olamaz!";
							else if (sistemDestekVar == false || personelERP.getDogumTarihi().length() > 5)
								str = "Doğum tarihi hatalıdır! (" + personelERP.getDogumTarihi() + " --> format : " + FORMAT_DATE + " )";

							if (str != null) {
								if (!izinERPUpdate)
									addHatalist(hataList, personelERP, null, str);
								else {
									kidemHataList.add(str);
								}
							}
						}
						if (izinHakEdisTarihi == null && sanalPersonel == false) {
							String str = null;
							if (notEmptyStr(personelERP.getKidemTarihi()) == false)
								str = "Kıdem tarihi boş olamaz!";
							else
								str = "Kıdem tarihi hatalıdır! (" + personelERP.getKidemTarihi() + " --> format : " + FORMAT_DATE + " )";
							if (!izinERPUpdate)
								addHatalist(hataList, personelERP, null, str);
							else {

								kidemHataList.add(str);
							}

						}

					}
					if (dogumTarihi != null && bolumYok == false) {
						String str = null;
						if (iseBaslamaTarihi != null && iseBaslamaTarihi.before(dogumTarihi))
							str = "İşe giriş tarihi doğum tarihinden önce olamaz!";
						else if (grubaGirisTarihi != null && grubaGirisTarihi.before(dogumTarihi))
							str = "Gruba giriş tarihi doğum tarihinden önce olamaz!";
						if (str != null) {
							if (izinGirisiVar == false)
								kidemHataList.add(str);
							else
								addHatalist(hataList, personelERP, null, str);
						}

					}
					Tanim departman = getTanim(null, "ekSaha1", personelERP.getDepartmanKodu(), personelERP.getDepartmanAdi(), dataMap, saveList);
					if (departman == null) {
						departman = bosDepartman;
						if (bosDepartman != null && calisiyor)
							kidemHataList.add(uygulamaBordro + " " + parentDepartman.getAciklamatr() + " bilgisi boş olamaz!");
					}
					if (istenAyrilisTarihi != null && istenAyrilisTarihi.after(bugun) && istenAyrilisTarihi.getTime() == lastDate.getTime()) {
						if (departmanYoneticiRolVar && departman == null && parentDepartman != null && departmanYoneticiRolVar)
							addHatalist(hataList, personelERP, null, uygulamaBordro + " " + parentDepartman.getAciklamatr() + " bilgisi boş olamaz!");
						if (bolum == null && parentBolum != null)
							addHatalist(hataList, personelERP, null, uygulamaBordro + " " + parentBolum.getAciklamatr() + " bilgisi boş olamaz!");
						if (bordroAltAlan == null && parentBordroTanim != null) {
							if (durumParentBordroTanimKodu)
								addHatalist(hataList, personelERP, null, uygulamaBordro + " " + parentBordroTanim.getAciklamatr() + " bilgisi boş olamaz!");

						}

					} else if (bordroAltAlan == null)
						bordroAltAlan = istenAyrilanEkSaha;
					personel.setEkSaha1(departman);
					personel.setEkSaha3(bolum);

					if (ekSahaAdi.startsWith("eksaha2")) {
						personel.setEkSaha2(bordroAltAlan);
						if (personel.getBordroAltAlan() != null)
							personel.setBordroAltAlan(null);
					} else if (ekSahaAdi.startsWith("eksaha4")) {
						personel.setEkSaha4(bordroAltAlan);
						if (personel.getBordroAltAlan() != null)
							personel.setBordroAltAlan(null);
					} else
						personel.setBordroAltAlan(bordroAltAlan);

					personel.setDogumTarihi(dogumTarihi);
					personel.setIzinHakEdisTarihi(izinHakEdisTarihi);
					personel.setSanalPersonel(sanalPersonel);

					if (grubaGirisTarihi == null) {
						grubaGirisTarihi = iseBaslamaTarihi;
						if (personel.getId() != null) {
							Date tarih = getGrubaGirisTarihi(personel.getPersonelKGS());
							if (tarih != null)
								grubaGirisTarihi = tarih;
						}

						if (grubaGirisTarihi != null)
							personel.setGrubaGirisTarihi(grubaGirisTarihi);

					} else {
						if (iseBaslamaTarihi != null && iseBaslamaTarihi.before(grubaGirisTarihi))
							addHatalist(hataList, personelERP, null, "İşe giriş tarihi grubu giriş tarihinden önce olamaz!");
						if (izinHakEdisTarihi != null && izinHakEdisTarihi.before(grubaGirisTarihi))
							addHatalist(hataList, personelERP, null, "Kıdem tarihi grubu giriş tarihinden önce olamaz!");
						if (istenAyrilisTarihi != null && istenAyrilisTarihi.before(grubaGirisTarihi))
							addHatalist(hataList, personelERP, null, "İşten ayrılma tarihi grubu giriş tarihinden önce olamaz!");
						personel.setGrubaGirisTarihi(grubaGirisTarihi);
					}

					boolean yoneticiKoduVar = PdksUtil.hasStringValue(yoneticiNo);

					Personel yoneticisi = yoneticiNo.equals(personelNo) && personel.getId() == null ? personel : null;
					if (personel != null && personel.isGenelMudur() && yoneticisi == null && personel.getId() != null) {
						yoneticisi = personel;
						personel.setYoneticisiAta(personel);
					}
					if (yoneticisi == null) {
						if (yoneticiKoduVar) {
							if (personelPDKSMap.containsKey(yoneticiNo)) {
								yoneticisi = personelPDKSMap.get(yoneticiNo);
								yoneticiKoduVar = yoneticisi != null && (personel.isCalisiyor() == false || yoneticisi.isCalisiyor());
							}
							if (yoneticisi != null) {
								if (yoneticisi.getId() == null) {
									personel.setTmpYonetici(yoneticisi);
									yoneticiList.add(personel);
									yoneticisi = null;
								}

								if ((yoneticiERPKontrol.equals("1") && yoneticiERP1Kontrol) || personelERPGuncellemeDurum || personel.getYoneticisi() == null || !personel.getYoneticisi().isCalisiyor())
									personel.setYoneticisi(yoneticisi);
								personel.setAsilYonetici1(yoneticisi);

							} else if (!(iskurManuelGiris && sanalPersonel))
								map1.put(personelNo, yoneticiPersonel);

						} else {
							if (genelMudurDurum == false) {
								if (sanalPersonel == false && calisiyor) {
									if (yoneticiKoduVar == false) {
										if (personel.getId() != null && personel.getYoneticisi() != null) {
											if (yoneticiERP1Kontrol == false) {
												personel.setYoneticisi(null);
												personel.setAsilYonetici1(null);
											}

											personel.setGuncellemeTarihi(new Date());
											personel.setGuncelleyenUser(islemYapan);
											try {

												saveList.add(personel);
												listeKaydet(personelNo, saveList, null);
											} catch (Exception e) {
												logger.error(personelNo + "\n" + e);
												e.printStackTrace();
											}

										}
										if (yoneticiRolVarmi && yoneticiERP1Kontrol && bolumYok == false) {
											if (yoneticisi == null)
												kidemHataList.add(uygulamaBordro + " " + yoneticiAciklama + " bilgisi boş olamaz!" + (personelERP.getGorevKodu() != null && personelERP.getGorevi() != null ? "[ " + personelERP.getGorevKodu() + " - " + personelERP.getGorevi() + " ]" : ""));
											// else if (yoneticisi != null)
											// kidemHataList.add(yoneticisi.getPdksSicilNo() + " " + yoneticisi.getAdSoyad() + " yönetici çalışmıyor!");
										}
									}
								}
							} else if (personel.isGenelMudur() == false) {
								personel.setAsilYonetici1(null);
								personel.setYoneticisi(null);
							}
						}
					}

					Personel yoneticisi2 = yonetici2ERPKontrol ? null : personel.getAsilYonetici2();
					if (personel.getId() == null && yonetici2No.equals(personelNo))
						yoneticisi2 = personel;
					else {
						if (yonetici2ERPKontrol) {
							boolean yoneticiKodu2Var = PdksUtil.hasStringValue(yonetici2No);
							if (yoneticiKodu2Var) {
								String yonetici2PerNo = PdksUtil.textBaslangicinaKarakterEkle(yonetici2No.trim(), '0', sicilNoUzunluk);
								yoneticisi2 = personelPDKSMap.get(yonetici2PerNo);
								if (yoneticisi2 != null) {
									if (yoneticisi != null && yoneticisi.getYoneticisi() != null && yoneticisi2.getId().equals(yoneticisi.getYoneticisi().getId())) {
										yoneticisi2 = null;
									} else if (yoneticiRolVarmi && !yoneticisi2.isCalisiyor() && bolumYok == false)
										kidemHataList.add("2. yönetici " + yonetici2No.trim() + " " + yoneticisi2.getAdSoyad() + " çalışmıyor!");
								} else if (yoneticiRolVarmi && sanalPersonel == false && calisiyor && bolumYok == false)
									kidemHataList.add(kapiGiris + " 2. yönetici " + yonetici2No.trim() + " personel no bilgisi bulunamadı!");
							}
							if (yoneticisi2 != null)
								logger.debug(yoneticisi2.getId());
							if (personelERP.getHataList().isEmpty())
								personel.setAsilYonetici2(yoneticisi2);
						} else {
							boolean yoneticiKodu2Var = PdksUtil.hasStringValue(yonetici2No);
							if (yoneticiKodu2Var) {
								String yonetici2PerNo = PdksUtil.textBaslangicinaKarakterEkle(yonetici2No.trim(), '0', sicilNoUzunluk);
								yoneticisi2 = personelPDKSMap.get(yonetici2PerNo);
								if (yoneticiRolVarmi && bolumYok == false) {
									if (yoneticisi2 != null)
										addHatalist(hataList, personelERP, null, "2. yönetici " + yonetici2No.trim() + " " + yoneticisi2.getAdSoyad() + " güncellemesi sistemde açık değildir!");
									else
										addHatalist(hataList, personelERP, null, "2. yönetici güncellemesi sistemde açık değildir!");
								}
							}
						}
					}

					if (personelERP.getHataList().isEmpty()) {
						if (personel.isDegisti()) {
							if (personel.getId() != null) {
								personel.setGuncellemeTarihi(new Date());
								personel.setGuncelleyenUser(islemYapan);
							} else {
								personel.setOlusturmaTarihi(new Date());
								personel.setOlusturanUser(islemYapan);
								personel.setPdksSicilNo(personelNo);
								if (bolum != null && bolum.getKodu() != null) {
									String kodu = bolum.getKodu();
									if (kodu.equalsIgnoreCase("YOK")) {
										personel.setMailTakip(Boolean.FALSE);
										personel.setFazlaMesaiOde(Boolean.FALSE);
										personel.setPdks(Boolean.FALSE);
										personel.setCalismaModeli(null);
									} else if (kodu.equalsIgnoreCase(IzinTipi.SUA_IZNI)) {
										if (sirket != null && sirket.getSuaOlabilir() != null && sirket.getSuaOlabilir())
											personel.setSuaOlabilir(Boolean.TRUE);
									}
								}
							}
							if (personel.getSablon() == null)
								personel.setSablon(vardiyaSablonu);
							personel.setIzinKartiVar(dogumTarihi != null && izinHakEdisTarihi != null);
							saveList.add(personel);
						} else {
							personelERP.setYazildi(Boolean.TRUE);
							personelERP.setId(personel.getId());
						}
						if (!personelListeMap.isEmpty()) {
							for (String digerTanimAlanKey : personelListeMap.keySet()) {
								Liste liste = personelListeMap.get(digerTanimAlanKey);
								Tanim tanimDeger = getDinamikTanim(digerTanimAlanKey, (String) liste.getKey(), (String) liste.getValue(), dataMap, saveList), alan = null;
								alan = tanimDeger.getParentTanim();
								String key = PersonelDinamikAlan.getKey(personel, alan);
								PersonelDinamikAlan personelDinamikAlan = personelDinamikAlanMap.containsKey(key) ? personelDinamikAlanMap.get(key) : new PersonelDinamikAlan(personel, alan);
								personelDinamikAlan.setDegisti(personelDinamikAlan.getId() == null);
								personelDinamikAlan.setTanimDeger(tanimDeger);
								if (personelDinamikAlan.isDegisti())
									saveList.add(personelDinamikAlan);
							}
						}
						if (!updateYonetici2)
							updateYonetici2 = true;

					} else {
						boolean sil = false;
						for (Iterator iterator = saveList.iterator(); iterator.hasNext();) {
							Object object = (Object) iterator.next();
							if (object instanceof Personel) {
								sil = true;
								break;
							}
						}
						if (sil)
							saveList.clear();
						if (personel.getId() == null && !calisiyor && !(iskurManuelGiris && sanalPersonel)) {
							personelERP.setYazildi(Boolean.FALSE);
							continue;
						}
					}

				}
				if (personel != null && personel.isGenelMudur() && personel.getYoneticisi() == null && personel.getId() != null) {
					personel.setYoneticisi(personel);
					saveList.add(personel);
				}

				if (!saveList.isEmpty()) {
					try {
						if (listeKaydet(personelNo, saveList, null)) {
							if (personel != null && personel.isGenelMudur() && personel.getYoneticisi() == null && personel.getId() != null) {
								personel.setYoneticisi(personel);
								saveList.clear();
								saveList.add(personel);
								listeKaydet(personelNo, saveList, null);
							}

							if (yoneticiIdList != null && personelSecili != null && personelSecili.getYoneticisi() != null && personelSecili.getId() != null) {
								if (!yoneticiIdList.contains(personelSecili.getId()))
									yoneticiIdList.add(personelSecili.getId());
							}
							if (personelERP.getHataList().isEmpty()) {
								if (kidemHataList.isEmpty()) {
									personelERP.setHataList(null);
									personelERP.setYazildi(Boolean.TRUE);
								} else {
									// personelERP.getHataList().addAll(kidemHataList);
									for (String string : kidemHataList)
										addHatalist(hataList, personelERP, null, string);

								}

							}
						}

					} catch (Exception e) {
						e.printStackTrace();
						addHatalist(hataList, personelERP, null, e.getMessage());
					}
				}

			}

		}
		sablonList = null;
		modelList = null;
		personelTest = null;
		if (yoneticiBul || kendiYonetici) {
			for (String personelNo : personelList) {
				if (personelPDKSMap.containsKey(personelNo) && personelYoneticiERPMap.containsKey(personelNo)) {
					Personel personel = personelPDKSMap.get(personelNo);
					PersonelERP personelERP = personelYoneticiERPMap.get(personelNo);
					boolean durum = map1.containsKey(personelNo) && map1.get(personelNo);
					if (durum && personelPDKSMap.containsKey(personelERP.getYoneticiPerNo())) {
						Personel yoneticisi = personelPDKSMap.get(personelERP.getYoneticiPerNo());
						if ((personelERPGuncellemeDurum || personel.getYoneticisi() == null) && yoneticisi.getId() != null)
							personel.setYoneticisi(yoneticisi);
						personel.setAsilYonetici1(yoneticisi);
						saveList.add(personel);
					}
				}
			}
			if (!saveList.isEmpty()) {
				try {
					listeKaydet("", saveList, null);
				} catch (Exception e) {

				}

			}
		}
		personelYoneticiERPMap = null;
	}

	/**
	 * 
	 */
	private void setSicilNoUzunluk() {
		sicilNoUzunluk = null;
		if (mailMap != null && mailMap.containsKey("sicilNoUzunluk"))
			try {
				sicilNoUzunluk = Integer.parseInt(mailMap.get("sicilNoUzunluk").toString());
				if (sicilNoUzunluk < 1)
					sicilNoUzunluk = null;
			} catch (Exception e) {
				sicilNoUzunluk = null;
			}
	}

	/**
	 * @param personel
	 * @param personelERP
	 * @param pattern
	 */
	private void setPersonel(Personel personel, PersonelERP personelERP, String pattern) {
		if (personel != null && personelERP != null) {
			try {
				personel.setAd(PdksUtil.getCutFirstSpaces(personelERP.getAdi()));
				personel.setSoyad(PdksUtil.getCutFirstSpaces(personelERP.getSoyadi()));
			} catch (Exception e) {
				logger.error(e);
			}
		}
	}

	/**
	 * @param key
	 * @param saveList
	 * @param deleteList
	 * @return
	 * @throws Exception
	 */
	private boolean listeKaydet(String key, List saveList, List deleteList) throws Exception {
		Boolean durum = Boolean.FALSE;
		if (saveList != null || deleteList != null) {
			Object[] saveObjectArray = null;
			if (saveList != null && !saveList.isEmpty()) {
				logger.debug(key + " " + saveList.size());
				saveObjectArray = new Object[saveList.size()];
				for (int i = 0; i < saveObjectArray.length; i++) {
					saveObjectArray[i] = saveList.get(i);
				}
			}
			Object[] deleteObjectArray = null;
			if (deleteList != null && !deleteList.isEmpty()) {
				deleteObjectArray = new Object[deleteList.size()];
				for (int i = 0; i < deleteObjectArray.length; i++) {
					deleteObjectArray[i] = deleteList.get(i);
				}
			}

			pdksDAO.deleteAndSaveObject(saveObjectArray, deleteObjectArray);
			if (saveList != null)
				saveList.clear();
			if (deleteList != null)
				deleteList.clear();
			durum = Boolean.TRUE;
		}
		return durum;

	}

	/**
	 * @param personelList
	 * @throws Exception
	 */
	public void savePersoneller(List<PersonelERP> personelList) throws Exception {
		servisAdi = "savePersoneller";
		if (pdksDAO != null && personelList != null && !personelList.isEmpty()) {
			sistemVerileriniYukle(pdksDAO, true);
			setErpVeriOku(mailMap == null || mailMap.containsKey(getParametrePersonelERPTableView()));
			erpVeriOkuSavePersoneller = erpVeriOku;
			Boolean servisDurum = !PdksUtil.getCanliSunucuDurum() || !(mailMap.containsKey(servisAdi + "Durum") && mailMap.get(servisAdi + "Durum").equals("0"));
			if (servisDurum) {
				personelBilgileriniGuncelle(personelList);
			} else {
				for (PersonelERP personelERP2 : personelList) {
					personelERP2.getHataList().add(servisAdi + " servisi  kapalıdır!");
				}
			}
		}

	}

	/**
	 * @param method
	 * @param fieldName
	 * @param dataIdList
	 * @param class1
	 * @param yaz
	 * @return
	 */
	private TreeMap getSQLParamListMap(String tableName, String method, String fieldName, List dataIdList, Class class1, boolean yaz) {
		TreeMap map = new TreeMap();
		if (dataIdList != null) {
			List veriList = getSQLObjectListFromDataList(tableName, fieldName, dataIdList, class1);
			if (!veriList.isEmpty())
				map = pdksDAO.getTreeMapByList(veriList, method, yaz);
		}
		return map;
	}

	/**
	 * @param method
	 * @param fieldName
	 * @param dataIdList
	 * @param class1
	 * @param yaz
	 * @return
	 */
	private TreeMap getParamListMap(String method, String fieldName, List dataIdList, Class class1, boolean yaz) {
		TreeMap map = new TreeMap();
		if (dataIdList != null) {
			List veriList = getObjectListFromDataList(fieldName, dataIdList, class1);
			if (!veriList.isEmpty())
				map = pdksDAO.getTreeMapByList(veriList, method, yaz);
		}
		return map;
	}

	/**
	 * @param fieldName
	 * @param dataIdList
	 * @param class1
	 * @return
	 */
	private List getObjectListFromDataList(String fieldName, List dataIdList, Class class1) {
		HashMap<String, Object> fieldsOrj = new HashMap<String, Object>();
		fieldsOrj.put(fieldName, dataIdList);
		List veriList = getParamList(false, dataIdList, fieldName, fieldsOrj, class1);
		return veriList;
	}

	/**
	 * @param tipi
	 * @param kodu
	 * @param durum
	 * @param methodAdi
	 * @return
	 */
	public TreeMap getSQLTanimMap(String tipi, String kodu, Boolean durum, String methodAdi) {
		TreeMap map = new TreeMap();
		List<Tanim> list = getSQLTanimList(tipi, kodu, durum);
		if (list != null)
			map = pdksDAO.getTreeMapByList(list, methodAdi, true);
		list = null;
		return map;
	}

	/**
	 * @param tipi
	 * @param kodu
	 * @param durum
	 * @return
	 */
	private List getSQLTanimList(String tipi, String kodu, Boolean durum) {
		HashMap<String, Object> fieldsOrj = new HashMap<String, Object>();
		StringBuffer sb = new StringBuffer();
		sb.append("select P.* from " + Tanim.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append(" where P." + Tanim.COLUMN_NAME_TIPI + " = :t ");
		fieldsOrj.put("t", tipi);
		if (kodu != null) {
			fieldsOrj.put("k", kodu);
			sb.append(" and P." + Tanim.COLUMN_NAME_KODU + " = :k ");
		}
		if (durum != null)
			sb.append(" and P." + Tanim.COLUMN_NAME_DURUM + "=" + (durum ? 1 : 0));
		List list = pdksDAO.getNativeSQLList(fieldsOrj, sb, Tanim.class);
		return list;
	}

	/**
	 * @param tipi
	 * @param kodu
	 * @param durum
	 * @return
	 */
	private Tanim getSQLTanim(String tipi, String kodu, Boolean durum) {
		List<Tanim> list = getSQLTanimList(tipi, kodu, durum);
		Tanim tanim = list != null && !list.isEmpty() ? list.get(0) : null;
		list = null;
		return tanim;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param dataIdList
	 * @param class1
	 * @return
	 */
	private List getSQLObjectListFromDataList(String tableName, String fieldName, List dataIdList, Class class1) {
		HashMap<String, Object> fieldsOrj = new HashMap<String, Object>();
		List veriList = null;
		StringBuffer sb = new StringBuffer();
		sb.append("select P.* from " + tableName + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		if (dataIdList.size() > 1) {
			sb.append(" where P." + fieldName + " :q ");
			fieldsOrj.put(fieldName, dataIdList);
			veriList = getNativeSQLParamList(dataIdList, sb, "q", fieldsOrj, class1);
		} else {
			sb.append(" where P." + fieldName + " = :q ");
			fieldsOrj.put("q", dataIdList.get(0));
			veriList = pdksDAO.getNativeSQLList(fieldsOrj, sb, class1);
		}

		return veriList;
	}

	/**
	 * @param methodAdi
	 * @param dataIdList
	 * @param sb
	 * @param fieldName
	 * @param fieldsOrj
	 * @param uzerineYaz
	 * @param class1
	 * @return
	 */
	public TreeMap getNativeSQLParamMap(String methodAdi, List dataIdList, StringBuffer sb, String fieldName, HashMap<String, Object> fieldsOrj, boolean uzerineYaz, Class class1) {
		TreeMap map = new TreeMap();
		if (class1 != null) {
			List list = getNativeSQLParamList(dataIdList, sb, fieldName, fieldsOrj, class1);
			map = pdksDAO.getTreeMapByList(list, methodAdi, uzerineYaz);

		}
		return map;
	}

	/**
	 * @param methodAdi
	 * @param logic
	 * @param dataIdList
	 * @param fieldName
	 * @param fieldsOrj
	 * @param uzerineYaz
	 * @param class1
	 * @return
	 */
	public TreeMap getSQLParamMap(String methodAdi, boolean logic, List dataIdList, String fieldName, HashMap<String, Object> fieldsOrj, boolean uzerineYaz, Class class1) {
		TreeMap map = new TreeMap();
		if (fieldsOrj.containsKey(BaseDAOHibernate.MAP_KEY_MAP)) {
			methodAdi = (String) fieldsOrj.get(BaseDAOHibernate.MAP_KEY_MAP);
			fieldsOrj.remove(BaseDAOHibernate.MAP_KEY_MAP);
		}

		if (class1 != null) {
			List list = getParamList(logic, dataIdList, fieldName, fieldsOrj, class1);
			map = pdksDAO.getTreeMapByList(list, methodAdi, uzerineYaz);

		}
		return map;
	}

	/**
	 * @param logic
	 * @param dataIdList
	 * @param fieldName
	 * @param fieldsOrj
	 * @param class1
	 * @return
	 */
	public List getParamList(boolean logic, List dataIdList, String fieldName, HashMap<String, Object> fieldsOrj, Class class1) {
		List idList = new ArrayList();
		List veriList = new ArrayList();
		if (pdksDAO == null)
			pdksDAO = Constants.pdksDAO;
		try {
			int size = LIST_MAX_SIZE - fieldsOrj.size();
			List idInputList = new ArrayList(dataIdList);
			while (!idInputList.isEmpty()) {
				HashMap map = new HashMap();
				for (Iterator iterator = idInputList.iterator(); iterator.hasNext();) {
					Object long1 = (Object) iterator.next();
					idList.add(long1);
					iterator.remove();
					if (idList.size() + map.size() >= size)
						break;
				}
				HashMap<String, Object> fields = new HashMap<String, Object>();
				fields.putAll(fieldsOrj);
				Object data = idList;
				String key = fieldName;
				if (idList.size() == 1 && fields.containsKey(fieldName)) {
					fields.remove(fieldName);
					data = idList.get(0);
					if (logic)
						key = fieldName + " = ";
				}
				fields.put(key, data);

				try {
					List list = logic ? pdksDAO.getObjectByInnerObjectListInLogic(fields, class1) : pdksDAO.getObjectByInnerObjectList(fields, class1);
					if (!list.isEmpty())
						veriList.addAll(list);
					list = null;
				} catch (Exception e) {
					logger.error(e);
					idInputList.clear();
				}

				fields = null;
				idList.clear();
			}
			idInputList = null;
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		return veriList;

	}

	/**
	 * @param dataIdList
	 * @param sb
	 * @param fieldName
	 * @param fieldsOrj
	 * @param class1
	 * @return
	 */
	public List getNativeSQLParamList(List dataIdList, StringBuffer sb, String fieldName, HashMap<String, Object> fieldsOrj, Class class1) {
		List idList = new ArrayList();
		List veriList = new ArrayList();
		if (pdksDAO == null)
			pdksDAO = Constants.pdksDAO;
		try {
			int size = LIST_MAX_SIZE - fieldsOrj.size();
			String str = ":" + fieldName, sqlStr = sb.toString();
			List idInputList = new ArrayList(dataIdList);
			while (!idInputList.isEmpty()) {
				HashMap map = new HashMap();
				for (Iterator iterator = idInputList.iterator(); iterator.hasNext();) {
					Object long1 = (Object) iterator.next();
					idList.add(long1);
					iterator.remove();
					if (idList.size() + map.size() >= size)
						break;
				}
				HashMap<String, Object> fields = new HashMap<String, Object>();
				fields.putAll(fieldsOrj);
				if (idList.size() > 1 || sqlStr.indexOf(str) < 1)
					fields.put(fieldName, idList);
				else {
					sb = new StringBuffer(PdksUtil.replaceAll(sqlStr, str, " = :" + fieldName));
					fields.put(fieldName, idList.get(0));
				}
				try {
					List list = pdksDAO.getNativeSQLList(fields, sb, class1);
					if (!list.isEmpty())
						veriList.addAll(list);
					list = null;
				} catch (Exception e) {
					logger.error(e);
					idInputList.clear();
				}

				fields = null;
				idList.clear();
			}
			idInputList = null;
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}

		return veriList;
	}

	/**
	 * @param personelList
	 * @throws Exception
	 */
	private void personelBilgileriniGuncelle(List<PersonelERP> personelList) throws Exception {
		kapiSirket = null;
		bugun = PdksUtil.getDate(new Date());
		String birdenFazlaKGSSirketSQL = mailMap.containsKey("birdenFazlaKGSSirketSQL") ? (String) mailMap.get("birdenFazlaKGSSirketSQL") : "";
		if (PdksUtil.hasStringValue(birdenFazlaKGSSirketSQL)) {
			HashMap map = new HashMap();
			map.put("id>", 0L);
			map.put("basTarih<=", bugun);
			map.put("bitTarih>=", bugun);
			List<KapiSirket> list = pdksDAO.getObjectByInnerObjectListInLogic(map, KapiSirket.class);
			if (list.size() == 1) {
				kapiSirket = list.get(0);
				kapiGiris = kapiSirket.getAciklama();
			}

		}
		Boolean mailBosGonder = personelList.size() > 1;
		MailStatu mailStatu = null;
		altBolumDurum = false;
		HashMap fields = new HashMap();
		// fields.put("tipi", Tanim.TIPI_PERSONEL_DINAMIK_DURUM);
		// fields.put("kodu", Tanim.IKINCI_YONETICI_ONAYLAMAZ);
		// ikinciYoneticiOlmaz = (Tanim) pdksDAO.getObjectByInnerObject(fields, Tanim.class);;
		ikinciYoneticiOlmaz = getSQLTanim(Tanim.TIPI_PERSONEL_DINAMIK_DURUM, Tanim.IKINCI_YONETICI_ONAYLAMAZ, null);

		fields.clear();

		genelTanimMap = getSQLTanimMap(Tanim.TIPI_GENEL_TANIM, null, null, "getKodu");
		if (ikinciYoneticiOlmaz != null && !ikinciYoneticiOlmaz.getDurum())
			ikinciYoneticiOlmaz = null;

		yoneticiIdList = new ArrayList<Long>();
		bosDepartman = null;
		setSicilNoUzunluk();
		if (personelList.size() == 1) {
			PersonelERP personelERP = personelList.get(0);
			if (personelERP != null)
				dosyaEkAdi = personelERP.getPersonelNo();
		}
		for (PersonelERP personelERP : personelList) {
			personelERP.veriSifirla();
			if (sicilNoUzunluk != null) {
				String perNo = PdksUtil.textBaslangicinaKarakterEkle(personelERP.getPersonelNo(), '0', sicilNoUzunluk);
				personelERP.setPersonelNo(perNo);
				if (PdksUtil.hasStringValue(personelERP.getYoneticiPerNo())) {
					String yoneticiPerNo = PdksUtil.textBaslangicinaKarakterEkle(personelERP.getYoneticiPerNo(), '0', sicilNoUzunluk);
					personelERP.setYoneticiPerNo(yoneticiPerNo);
				}
				if (PdksUtil.hasStringValue(personelERP.getYonetici2PerNo())) {
					String yonetici2PerNo = PdksUtil.textBaslangicinaKarakterEkle(personelERP.getYonetici2PerNo(), '0', sicilNoUzunluk);
					personelERP.setYonetici2PerNo(yonetici2PerNo);
				}

			}
		}

		saveFonksiyonVeri("savePersoneller", personelList);

		if (personelList.size() == 1) {
			PersonelERP erp = personelList.get(0);
			mesaj = (erp.getPersonelNo() != null ? erp.getPersonelNo() + " " : "") + (erp.getAdi() != null ? erp.getAdi() + " " : "") + (erp.getSoyadi() != null ? erp.getSoyadi() + " " : "");
		} else
			mesaj = personelList.size() + " adet personel bilgisi guncellenecektir.";
		mesajInfoYaz("savePersoneller --> " + mesaj + " in " + PdksUtil.getCurrentTimeStampStr());
		HashMap<String, Object> fieldsOrj = new HashMap<String, Object>();
		StringBuffer sbAna = new StringBuffer();
		sbAna.append("select P.* from " + VardiyaSablonu.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sbAna.append(" where P." + VardiyaSablonu.COLUMN_NAME_BEYAZ_YAKA + " = 1 and P." + VardiyaSablonu.COLUMN_NAME_DURUM + " = 1");
		List<VardiyaSablonu> sablonuList = pdksDAO.getNativeSQLList(fieldsOrj, sbAna, VardiyaSablonu.class);
		TreeMap<String, VardiyaSablonu> sablonMap = new TreeMap<String, VardiyaSablonu>();
		for (VardiyaSablonu vs : sablonuList) {
			String key = "0_0";
			if (vs.getSirket() != null) {
				if (vs.getSirket().getDepartman().isAdminMi()) {
					key = "0_" + vs.getSirket().getId();
					sablonMap.put(key, vs);
				}
			} else if (vs.getDepartman() != null) {
				if (vs.getDepartman().isAdminMi()) {
					key = vs.getDepartman().getId() + "_0";
					sablonMap.put(key, vs);
				}
			} else
				sablonMap.put(key, vs);

		}
		sablonuList = null;
		sbAna = new StringBuffer();
		sbAna.append("select P.* from " + CalismaModeli.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sbAna.append(" where P." + CalismaModeli.COLUMN_NAME_IDARI_MODEL + " = 1 and P." + VardiyaSablonu.COLUMN_NAME_DURUM + " = 1");
		List<CalismaModeli> modelList = pdksDAO.getNativeSQLList(fieldsOrj, sbAna, CalismaModeli.class);
		TreeMap<String, CalismaModeli> cmMap = new TreeMap<String, CalismaModeli>();
		for (CalismaModeli cm : modelList) {
			String key = "0_0";
			if (cm.getSirket() != null) {
				if (cm.getSirket().getDepartman().isAdminMi()) {
					key = "0_" + cm.getSirket().getId();
					cmMap.put(key, cm);
				}
			} else if (cm.getDepartman() != null) {
				if (cm.getDepartman().isAdminMi()) {
					key = cm.getDepartman().getId() + "_0";
					cmMap.put(key, cm);
				}
			} else
				cmMap.put(key, cm);

		}
		sablonuList = null;
		modelList = null;
		TreeMap<String, PersonelERP> perMap = new TreeMap<String, PersonelERP>();
		List<String> yoneticiPerNoList = new ArrayList<String>(), kendiYoneticiPerNoList = new ArrayList<String>();
		TreeMap<String, List<String>> veriSorguMap = new TreeMap<String, List<String>>();
		TreeMap<String, TreeMap> dataMap = new TreeMap<String, TreeMap>();
		dataMap.put("personelERPMap", perMap);
		if (personelList.size() > 1) {
			List<PersonelERP> list1 = new ArrayList<PersonelERP>(), list2 = new ArrayList<PersonelERP>();
			List<String> yoneticiNoList = new ArrayList<String>();
			for (PersonelERP personelERP : personelList) {
				String yoneticiNo = personelERP.getYoneticiPerNo() != null ? personelERP.getYoneticiPerNo().trim() : "";
				if (PdksUtil.hasStringValue(yoneticiNo) && !yoneticiNoList.contains(yoneticiNo))
					yoneticiNoList.add(yoneticiNo);
			}
			for (PersonelERP personelERP : personelList) {
				String perNo = personelERP.getPersonelNo() != null ? personelERP.getPersonelNo().trim() : "";
				String yoneticiNo = personelERP.getYoneticiPerNo() != null ? personelERP.getYoneticiPerNo().trim() : "";
				if (perNo.equals(yoneticiNo) || yoneticiNoList.contains(perNo))
					list1.add(personelERP);
				else
					list2.add(personelERP);

			}
			personelList.clear();
			if (!list1.isEmpty())
				personelList.addAll(list1);
			if (!list2.isEmpty())
				personelList.addAll(list2);
			list1 = null;
			list2 = null;
			yoneticiNoList = null;
		}

		for (PersonelERP personelERP : personelList) {
			try {
				personelERP.setYazildi(false);
				String yoneticiPerNo = personelERP.getYoneticiPerNo() != null ? personelERP.getYoneticiPerNo().trim() : "";
				String yonetici2PerNo = personelERP.getYonetici2PerNo() != null ? personelERP.getYonetici2PerNo().trim() : "";
				String perNo = personelERP.getPersonelNo() != null ? personelERP.getPersonelNo().trim() : "";
				veriIsle("sirket", personelERP.getSirketKodu(), veriSorguMap);
				veriIsle("personel", perNo, veriSorguMap);
				if (perNo.equals(yoneticiPerNo)) {
					if (PdksUtil.hasStringValue(yoneticiPerNo) && !kendiYoneticiPerNoList.contains(yoneticiPerNo)) {
						kendiYoneticiPerNoList.add(yoneticiPerNo);
						veriIsle("personel", yoneticiPerNo, veriSorguMap);
					}
					if (PdksUtil.hasStringValue(yonetici2PerNo) && !kendiYoneticiPerNoList.contains(yonetici2PerNo)) {
						kendiYoneticiPerNoList.add(yonetici2PerNo);
						veriIsle("personel", yonetici2PerNo, veriSorguMap);
					}
				} else {
					if (PdksUtil.hasStringValue(yoneticiPerNo) && !yoneticiPerNoList.contains(yoneticiPerNo)) {
						yoneticiPerNoList.add(yoneticiPerNo);
						veriIsle("personel", yoneticiPerNo, veriSorguMap);
					}
					if (PdksUtil.hasStringValue(yonetici2PerNo) && !yoneticiPerNoList.contains(yonetici2PerNo)) {
						yoneticiPerNoList.add(yonetici2PerNo);
						veriIsle("personel", yonetici2PerNo, veriSorguMap);
					}
				}
				perMap.put(personelERP.getPersonelNo(), personelERP);
			} catch (Exception e) {
			}

		}
		fields.clear();
		fields.put("beyazYakaDefault", Boolean.TRUE);
		vardiyaSablonu = (VardiyaSablonu) pdksDAO.getObjectByInnerObject(fields, VardiyaSablonu.class);
		fields.clear();

		sablonCalismaModeliVar = false;
		personelCalismaModeliVar = false;
		fields.clear();
		fields.put("durum", Boolean.TRUE);
		sablonlar = pdksDAO.getObjectByInnerObjectList(fields, VardiyaSablonu.class);
		for (Iterator iterator = sablonlar.iterator(); iterator.hasNext();) {
			VardiyaSablonu sablon = (VardiyaSablonu) iterator.next();
			if (sablon.getDepartman() != null && sablon.getDepartman().isAdminMi() == false)
				iterator.remove();
			else if (!sablonCalismaModeliVar)
				sablonCalismaModeliVar = sablon.getCalismaModeli() != null;

		}
		modeller = pdksDAO.getObjectByInnerObjectList("durum", Boolean.TRUE, CalismaModeli.class);
		for (Iterator iterator = modeller.iterator(); iterator.hasNext();) {
			CalismaModeli calismaModeli = (CalismaModeli) iterator.next();
			if (calismaModeli.getDepartman() != null && calismaModeli.getDepartman().isAdminMi() == false)
				iterator.remove();
			else if (!personelCalismaModeliVar)
				personelCalismaModeliVar = calismaModeli.getPersonelTipi() != null;
		}

		fields.clear();
		if (veriSorguMap.containsKey("personel")) {
			fields.put(BaseDAOHibernate.MAP_KEY_MAP, "getSicilNo");
			fields.put("sicilNo", veriSorguMap.get("personel"));
		}
		TreeMap<String, PersonelKGS> personelKGSMap = new TreeMap<String, PersonelKGS>();
		StringBuffer sb = new StringBuffer();
		sb.append("select P.* from " + PersonelKGS.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		String ek = " where ";
		List list = null;
		String fieldName = "p";
		if (kapiSirket != null) {
			sb.append(ek + " P." + PersonelKGS.COLUMN_NAME_KGS_SIRKET + " = " + kapiSirket.getId());
			ek = " and ";
		}
		if (veriSorguMap.containsKey("personel")) {
			list = veriSorguMap.get("personel");
			fields.put(fieldName, list);
			sb.append(ek + " P." + PersonelKGS.COLUMN_NAME_SICIL_NO + " :" + fieldName);
		}
		sb.append(" order by  P." + PersonelKGS.COLUMN_NAME_SICIL_NO + ", P." + PersonelKGS.COLUMN_NAME_DURUM);
		List<PersonelKGS> personelKGSList = null;
		if (list != null)
			personelKGSList = getNativeSQLParamList(list, sb, fieldName, fields, PersonelKGS.class);
		else
			personelKGSList = pdksDAO.getNativeSQLList(fields, sb, PersonelKGS.class);
		if (personelKGSList != null) {
			for (PersonelKGS personel : personelKGSList)
				personelKGSMap.put(personel.getSicilNo(), personel);
		}

		personelKGSList = null;
		fields.clear();
		sb = new StringBuffer();
		sb.append("select P.* from " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append(" inner join " + PersonelKGS.TABLE_NAME + " K " + PdksVeriOrtakAktar.getJoinLOCK() + " on K." + PersonelKGS.COLUMN_NAME_ID + " = P." + Personel.COLUMN_NAME_KGS_PERSONEL + " and K.PERSONEL_NO<>P." + Personel.COLUMN_NAME_PDKS_SICIL_NO);
		sb.append(" where K." + PersonelKGS.COLUMN_NAME_SICIL_NO + " :" + fieldName);
		List<String> personelNoList = new ArrayList<String>();
		for (String string : veriSorguMap.get("personel")) {
			personelNoList.add(PdksUtil.replaceAll(string, "'", ""));
		}
		fields.put(fieldName, personelNoList);
		List<Personel> personelDigerList = getNativeSQLParamList(personelNoList, sb, fieldName, fields, Personel.class);
		TreeMap<String, Personel> personelDigerMap = new TreeMap<String, Personel>();
		for (Personel personel : personelDigerList) {
			PersonelKGS personelKGS = personel.getPersonelKGS();
			personelDigerMap.put(personelKGS.getSicilNo(), personel);
		}
		personelDigerList = null;
		TreeMap<String, PersonelDinamikAlan> personelDinamikAlanMap = new TreeMap<String, PersonelDinamikAlan>();

		fields.clear();
		sb = new StringBuffer();
		sb.append("select K.* from " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
		sb.append(" inner join " + PersonelDinamikAlan.TABLE_NAME + " K " + PdksVeriOrtakAktar.getJoinLOCK() + " on K." + PersonelDinamikAlan.COLUMN_NAME_PERSONEL + " = P." + Personel.COLUMN_NAME_ID);
		sb.append(" where P." + Personel.COLUMN_NAME_PDKS_SICIL_NO + " :" + fieldName);

		fields.put(fieldName, personelNoList);
		List<PersonelDinamikAlan> personelDinamikAlanList = getNativeSQLParamList(personelNoList, sb, fieldName, fields, PersonelDinamikAlan.class);
		for (PersonelDinamikAlan personelDinamikAlan : personelDinamikAlanList)
			personelDinamikAlanMap.put(personelDinamikAlan.getKey(), personelDinamikAlan);
		personelDinamikAlanList = null;

		TreeMap<String, ERPPersonel> personelERPHataliMap = !personelNoList.isEmpty() ? getSQLParamListMap(ERPPersonel.TABLE_NAME, "getSicilNo", ERPPersonel.COLUMN_NAME_SICIL_NO, personelNoList, ERPPersonel.class, true) : new TreeMap<String, ERPPersonel>();
		TreeMap<String, Sirket> sirketMap = veriSorguMap.containsKey("sirket") ? getSQLParamListMap(Sirket.TABLE_NAME, "getErpKodu", Sirket.COLUMN_NAME_ERP_KODU, veriSorguMap.get("sirket"), Sirket.class, false) : new TreeMap<String, Sirket>();
		TreeMap<String, Personel> personelPDKSMap = veriSorguMap.containsKey("personel") ? getSQLParamListMap(Personel.TABLE_NAME, "getPdksSicilNo", Personel.COLUMN_NAME_PDKS_SICIL_NO, veriSorguMap.get("personel"), Personel.class, false) : new TreeMap<String, Personel>();

		TreeMap<String, Tanim> personelDinamikTanimAlanMap = getSQLTanimMap(Tanim.TIPI_PERSONEL_DINAMIK_TANIM, null, Boolean.TRUE, "getKodu");
		List<Tanim> personelDinamikListeAlanList = getSQLTanimList(Tanim.TIPI_PERSONEL_DINAMIK_LISTE_TANIM, null, null);
		TreeMap<String, Tanim> personelDinamikListeAlanMap = new TreeMap<String, Tanim>();
		if (!personelDinamikListeAlanList.isEmpty()) {
			for (String key : personelDinamikTanimAlanMap.keySet()) {
				Tanim parentTanim = personelDinamikTanimAlanMap.get(key);
				for (Iterator iterator = personelDinamikListeAlanList.iterator(); iterator.hasNext();) {
					Tanim tanim = (Tanim) iterator.next();
					if (tanim.getParentTanim() != null && tanim.getParentTanim().getId().equals(parentTanim.getId())) {
						tanim.setGuncellendi(Boolean.FALSE);
						personelDinamikListeAlanMap.put(key + "_" + tanim.getErpKodu(), tanim);
						iterator.remove();
					}

				}
			}
		}
		personelDinamikListeAlanList = null;
		TreeMap<String, Tanim> personelEKSahaMap = getSQLTanimMap(Tanim.TIPI_PERSONEL_EK_SAHA, null, Boolean.TRUE, "getKodu");
		List<Tanim> personelEKSahaAciklamaList = getSQLTanimList(Tanim.TIPI_PERSONEL_EK_SAHA_ACIKLAMA, null, null);
		TreeMap<String, Tanim> personelEKSahaVeriMap = new TreeMap<String, Tanim>();
		if (!personelEKSahaAciklamaList.isEmpty()) {
			for (String key : personelEKSahaMap.keySet()) {
				Tanim parentTanim = personelEKSahaMap.get(key);
				for (Iterator iterator = personelEKSahaAciklamaList.iterator(); iterator.hasNext();) {
					Tanim tanim = (Tanim) iterator.next();
					if (tanim.getParentTanim() != null && tanim.getParentTanim().getId().equals(parentTanim.getId())) {
						tanim.setGuncellendi(Boolean.FALSE);
						personelEKSahaVeriMap.put(key + "_" + tanim.getErpKodu(), tanim);
						iterator.remove();
					}

				}
			}
		}
		personelEKSahaAciklamaList = null;
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_GIRIS_TIPI);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_CINSIYET);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_GOREV_TIPI);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_BORDRO_ALT_BIRIMI);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_ERP_MASRAF_YERI);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_TESIS);
		tanimGetir(personelEKSahaVeriMap, Tanim.TIPI_PERSONEL_TIPI);
		dataMap.put("cmMap", cmMap);
		dataMap.put("sablonMap", sablonMap);
		dataMap.put("personelDinamikAlanMap", personelDinamikAlanMap);
		dataMap.put("personelDinamikTanimAlanMap", personelDinamikTanimAlanMap);
		dataMap.put("personelDinamikListeAlanMap", personelDinamikListeAlanMap);
		dataMap.put("personelEKSahaVeriMap", personelEKSahaVeriMap);
		dataMap.put("personelEKSahaMap", personelEKSahaMap);
		dataMap.put("personelPDKSMap", personelPDKSMap);
		dataMap.put("personelERPHataliMap", personelERPHataliMap);
		dataMap.put("personelKGSMap", personelKGSMap);
		dataMap.put("personelDigerMap", personelDigerMap);
		dataMap.put("sirketMap", sirketMap);
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(PdksUtil.getDate(bugun));
		calendar.set(Calendar.DATE, 1);
		List<Personel> yoneticiList = new ArrayList<Personel>();
		ayBasi = PdksUtil.tariheGunEkleCikar(calendar.getTime(), -6);
		personelKontrolVerileriAyarla(pdksDAO);
		updateYonetici2 = false;
		// TODO Kendi yoneticisi personeller güncelleniyor
		List<PersonelERP> hataList = new ArrayList<PersonelERP>();
		if (perMap != null && !perMap.isEmpty())
			personelVeriYaz(yoneticiList, dataMap, kendiYoneticiPerNoList, "P");
		// TODO Yoneticiler personeller güncelleniyor
		if (!yoneticiPerNoList.isEmpty())
			personelVeriYaz(yoneticiList, dataMap, yoneticiPerNoList, "Y");
		// TODO Yonetici olmayan personeller güncelleniyor
		if (perMap != null && !perMap.isEmpty())
			personelVeriYaz(yoneticiList, dataMap, new ArrayList<String>(perMap.keySet()), "P");
		if (!yoneticiList.isEmpty()) {
			for (Iterator iterator = yoneticiList.iterator(); iterator.hasNext();) {
				Personel personel = (Personel) iterator.next();
				if (personel.getTmpYonetici() == null || personel.getTmpYonetici().getId() == null) {
					iterator.remove();
				} else {
					personel.setYoneticisi(personel.getTmpYonetici());
				}

			}
			if (!yoneticiList.isEmpty())
				pdksDAO.saveObjectList(yoneticiList);
		}
		hataList.clear();
		HashMap<String, PersonelERP> orjPersonelERPMap = new HashMap<String, PersonelERP>();
		for (PersonelERP personelERP : personelList) {
			if (personelERP.getPersonelNo() != null && personelPDKSMap.containsKey(personelERP.getPersonelNo())) {
				Personel personel = personelPDKSMap.get(personelERP.getPersonelNo());
				if (personel != null)
					personelERP.setId(personel.getId());
			}
			orjPersonelERPMap.put(personelERP.getPersonelNo(), (PersonelERP) personelERP.clone());
			PersonelERP hataPersonelERP = null;
			if (personelERP.getHataList() != null && !personelERP.getHataList().isEmpty()) {
				personelERP.setId(null);
				hataPersonelERP = (PersonelERP) personelERP.clone();
				hataList.add(hataPersonelERP);
			}
			personelERP.setBolumAdi(null);
			personelERP.setBolumKodu(null);
			if (altBolumDurum) {
				personelERP.setBordroAltAlanKodu(null);
				personelERP.setBordroAltAlanAdi(null);

			}
			personelERP.setSanalPersonel(null);
			personelERP.setCinsiyeti(null);
			personelERP.setCinsiyetKodu(null);
			personelERP.setDepartmanAdi(null);
			personelERP.setDepartmanKodu(null);
			personelERP.setMasrafYeriAdi(null);
			personelERP.setMasrafYeriKodu(null);
			personelERP.setDogumTarihi(null);
			personelERP.setIseGirisTarihi(null);
			personelERP.setGrubaGirisTarihi(null);
			if (personelERP.getIstenAyrilmaTarihi() != null && personelERP.getYazildi().booleanValue())
				personelERP.setIstenAyrilmaTarihi(null);
			personelERP.setKidemTarihi(null);
			personelERP.setGorevi(null);
			personelERP.setGorevKodu(null);
			personelERP.setTesisKodu(null);
			personelERP.setTesisAdi(null);
			personelERP.setYoneticiPerNo(null);
			personelERP.setYonetici2PerNo(null);
			personelERP.setGrubaGirisTarihi(null);
			digerAlanlarBosalt(personelERP);

		}
		if (personelList.size() > 1)
			saveIkinciYoneticiOlmazList("ikinciYoneticiOlmaz");

		if (yoneticiIdList != null && !yoneticiIdList.isEmpty()) {
			fieldName = "p";
			sb = new StringBuffer();
			sb.append(" with VERI as ( ");
			if (ikinciYoneticiOlmaz != null) {
				sb.append(" select D." + PersonelDinamikAlan.COLUMN_NAME_PERSONEL + " as ID from " + PersonelDinamikAlan.TABLE_NAME + " D " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
				sb.append(" inner join " + Personel.TABLE_NAME + " Y " + PdksVeriOrtakAktar.getJoinLOCK() + " on Y. " + Personel.COLUMN_NAME_YONETICI + " = D." + PersonelDinamikAlan.COLUMN_NAME_PERSONEL);
				sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P. " + Personel.COLUMN_NAME_YONETICI + " = Y." + Personel.COLUMN_NAME_ID);
				sb.append(" where D." + PersonelDinamikAlan.COLUMN_NAME_ALAN + " = " + ikinciYoneticiOlmaz.getId());
				sb.append(" and " + PersonelDinamikAlan.COLUMN_NAME_DURUM_SECIM + " = 1 ");
				sb.append(" union ");
			}
			sb.append(" select U." + User.COLUMN_NAME_PERSONEL + " as ID from " + User.TABLE_NAME + " U " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append(" inner join " + Personel.TABLE_NAME + " P " + PdksVeriOrtakAktar.getJoinLOCK() + " on P." + Personel.COLUMN_NAME_ID + " = U." + User.COLUMN_NAME_PERSONEL);
			sb.append(" and P." + Personel.COLUMN_NAME_DURUM + " = 1 and P." + Personel.COLUMN_NAME_SSK_CIKIS_TARIHI + " >= convert(date,GETDATE()) ");
			sb.append(" and P.IKINCI_YONETICI_IZIN_ONAYLA = 0");
			sb.append(" where U." + User.COLUMN_NAME_DURUM + " = 1 ");
			sb.append(" ) ");
			sb.append(" select distinct P.* from VERI P " + PdksVeriOrtakAktar.getSelectLOCK() + " ");
			sb.append(" where P.ID :" + fieldName);

			fields.clear();
			fields.put(fieldName, yoneticiIdList);
			List<BigDecimal> list2 = getNativeSQLParamList(yoneticiIdList, sb, fieldName, fields, null);
			if (!list2.isEmpty()) {
				LinkedHashMap<String, Object> map = new LinkedHashMap<String, Object>();
				map.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_GET_IKINCI_BIRINCI_YONETICI_UPDATE");
				pdksDAO.execSP(map);
			}
		}
		boolean hataVar = false;
		if (hataList != null && !hataList.isEmpty() || (hataIKMap != null && !hataIKMap.isEmpty())) {
			hataVar = isTatil() == false;

		}
		if (hataVar) {
			List<User> userIKList = null;
			MailStatu statu = null;
			boolean devam = true;
			if (hataIKMap != null) {
				int hataIKMapSize = hataIKMap.size();
				List<Long> userIdList = new ArrayList<Long>();
				if (!hataIKMap.containsKey(TIPI_IK_ADMIN) && ikUserMap.containsKey(Role.TIPI_IK)) {
					HashMap<String, List<User>> map1 = ikUserMap.get(Role.TIPI_IK);
					if (map1.containsKey(TIPI_IK_ADMIN)) {
						userIKList = map1.get(TIPI_IK_ADMIN);
						if (userIKList != null) {
							for (User user : userIKList)
								userIdList.add(user.getId());
						}
					}
				}
				for (String key : hataIKMap.keySet()) {
					mailMap.put(KEY_IK_MAIL_IPTAL, Boolean.TRUE);
					HashMap<String, List> dataHataMap = hataIKMap.get(key);
					List<User> userList = dataHataMap.get("userList");
					if (userIdList.isEmpty() == false) {
						for (Iterator iterator = userList.iterator(); iterator.hasNext();) {
							User user = (User) iterator.next();
							if (userIdList.contains(user.getId()))
								iterator.remove();
							else
								logger.debug(key + " " + user.getUsername());
						}
					}
					if (!userList.isEmpty()) {
						if ((hataList.size() == 1 || hataIKMapSize == 1) && userIdList.isEmpty() == false) {
							if (userIKList != null) {
								if (!userIKList.isEmpty())
									userList.addAll(userIKList);
								userIKList.clear();
								devam = false;
							}
						}
						List hataIKList = dataHataMap.get("hataList");
						personelHataMailGonder(userList, personelList, orjPersonelERPMap, hataIKList, personelERPHataliMap, sirketMap, false);

					}
				}
				userIdList = null;
			} else {
				userIKList = new ArrayList<User>();
				if (mailMap.containsKey(KEY_IK_MAIL_IPTAL))
					mailMap.remove(KEY_IK_MAIL_IPTAL);
			}

			if (devam && (userIKList != null || !mailMap.containsKey(KEY_IK_MAIL_IPTAL))) {

				statu = personelHataMailGonder(userIKList, personelList, orjPersonelERPMap, hataList, personelERPHataliMap, sirketMap, mailBosGonder);
				if (statu != null && statu.isDurum())
					logger.info("savePersoneller hata gonderildi. " + PdksUtil.getCurrentTimeStampStr());
			}
		}
		if (mailBosGonder && mailStatu == null && (getTestSunucuDurum() || erpVeriOku == false))
			mailBosGonder("savePersoneller", "personel", personelList);
		if (updateYonetici2)
			setIkinciYoneticiSifirla();

		saveFonksiyonVeri(null, personelList);
		hataList = null;

		mesajInfoYaz("savePersoneller --> " + mesaj + " out " + PdksUtil.getCurrentTimeStampStr());
	}

	/**
	 * @param userList
	 * @param hataList
	 * @param personelMap
	 * @param izinCok
	 * @return
	 */
	private MailStatu izinHataliMailGonder(List<User> userList, List<IzinERP> hataList, TreeMap<String, Personel> personelMap, boolean izinCok) {
		MailStatu mailStatu = null;
		List<String> verilist = new ArrayList<String>();
		for (Iterator iterator = hataList.iterator(); iterator.hasNext();) {
			IzinERP izinERP = (IzinERP) iterator.next();
			if (izinERP.getHataList().isEmpty() || verilist.contains(izinERP.getReferansNoERP()))
				iterator.remove();
			else
				verilist.add(izinERP.getReferansNoERP());
		}
		verilist = null;
		for (Iterator iterator = hataList.iterator(); iterator.hasNext();) {
			IzinERP izinERP = (IzinERP) iterator.next();
			if (izinERP == null || izinERP.getDurum() == null)
				iterator.remove();
		}
		if (!hataList.isEmpty() && izinCok) {
			TreeMap<String, Tatil> tatilMap = getTatilGunleri(PdksUtil.tariheGunEkleCikar(bugun, -5), bugun);
			if (tatilMap.containsKey(PdksUtil.convertToDateString(bugun, "yyyyMMdd")))
				hataList.clear();

		}
		if (!hataList.isEmpty()) {
			try {
				Gson gson = new Gson();
				String jsonStr = null;
				try {
					jsonStr = PdksUtil.toPrettyFormat(gson.toJson(hataList));
				} catch (Exception e) {
					jsonStr = null;
				}
				String konu = uygulamaBordro + " saveIzinler problem", konuEk = "";
				if (hataList.size() == 1 && personelMap != null) {
					IzinERP izinERP = hataList.get(0);
					if (PdksUtil.hasStringValue(izinERP.getPersonelNo()) && personelMap.containsKey(izinERP.getPersonelNo())) {
						Personel izinSahibi = personelMap.get(izinERP.getPersonelNo());
						konuEk = " [ " + izinERP.getPersonelNo() + " - " + izinSahibi.getAdSoyad() + " ]";
					}
				}
				mailMap.put("konu", konu + konuEk);
				StringBuffer sb = new StringBuffer();
				sb.append("<p><b>" + uygulamaBordro + " pdks entegrasyon servisi saveIzinler fonksiyonunda hatalı veri var!</b></p>");
				sb.append("<TABLE class=\"mars\" style=\"width: 80%\">");
				boolean gonder = false, renkUyari = true;
				for (Iterator iterator = hataList.iterator(); iterator.hasNext();) {
					IzinERP izinERP = (IzinERP) iterator.next();
					gonder = true;
					String zaman = (izinERP.getBasZaman() != null ? izinERP.getBasZaman().trim() + " - " : "") + (izinERP.getBitZaman() != null ? izinERP.getBitZaman() + " " : " ") + (izinERP.getIzinTipiAciklama() != null ? izinERP.getIzinTipiAciklama() : " ");
					Personel izinSahibi = izinERP.getPersonelNo() != null && personelMap.containsKey(izinERP.getPersonelNo()) ? personelMap.get(izinERP.getPersonelNo()) : null;
					String sirketBilgi = "";
					if (izinSahibi != null) {
						Sirket sirket = izinSahibi.getSirket();
						sirketBilgi = sirket.getSirketGrup() == null ? sirket.getAd() : sirket.getSirketGrup().getAciklama();
						if (izinSahibi.getTesis() != null && sirket.isTesisDurumu()) {
							if (sirket.isTesisDurumu()) {
								if (PdksUtil.hasStringValue(sirketBilgi))
									sirketBilgi += " - " + izinSahibi.getTesis().getAciklama();
								else
									sirketBilgi = izinSahibi.getTesis().getAciklama();
							}
						}
						if (izinSahibi.getEkSaha3() != null) {
							if (izinSahibi.getEkSaha3() != null) {
								if (!PdksUtil.hasStringValue(sirketBilgi))
									sirketBilgi = izinSahibi.getEkSaha3().getAciklama();
								else
									sirketBilgi += " / " + izinSahibi.getEkSaha3().getAciklama();
							}
						}
						sirketBilgi = " ( " + sirketBilgi + " ) ";
					}
					String personelBilgisi = izinSahibi != null ? izinERP.getPersonelNo() + " - " + izinSahibi.getAdSoyad() + " " : null;
					if (personelBilgisi == null)
						personelBilgisi = izinERP.getPersonelNo() != null ? izinERP.getPersonelNo() + " " : "";
					else
						personelBilgisi += sirketBilgi;
					sb.append("<TR class=\"" + (renkUyari ? "odd" : "even") + "\"><TD colspan=2><b>" + personelBilgisi + zaman + "</b></TD></TR>");
					List<String> veriHataList = izinERP.getHataList();
					for (int i = 0; i < veriHataList.size(); i++) {
						sb.append("<TR class=\"" + (renkUyari ? "odd" : "even") + "\"><TD width='10%'></TD><TD width='90%'>" + (veriHataList.size() > 1 ? (i + 1) + ". " : "") + veriHataList.get(i) + "</TD></TR>");
					}
					renkUyari = !renkUyari;
					if (iterator.hasNext())
						sb.append("<TR><TD colspan=2> </TD></TR>");
					izinERP.setIzinTipiAciklama(null);
					izinERP.setBasZaman(null);
					izinERP.setBitZaman(null);
				}
				sb.append("</TABLE>");
				if (gonder) {
					mailMap.put("mailIcerik", sb.toString());
					if (jsonStr != null) {
						LinkedHashMap<String, Object> fileMap = new LinkedHashMap<String, Object>();
						String str = getJsonToXML(jsonStr, "izin", IZIN_PROP_ORDER, "saveIzinler");
						fileMap.put("saveIzinler.xml", str);
						mailMap.put("fileMap", fileMap);
					}
					mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
					MailObject mailObject = kullaniciIKYukle(userList, mailMap, pdksDAO);
					if (mailObject != null && !mailObject.getToList().isEmpty())
						mailStatu = MailManager.ePostaGonder(mailMap);

				}
			} catch (Exception em) {
				logger.error(em);
				em.printStackTrace();
			}
		}
		if (mailStatu != null && userList != null && userList.size() > 1 && mailMap.containsKey("invalidAddresses")) {
			List<String> invalidAddresses = (List<String>) mailMap.get("invalidAddresses");
			mailMap.remove("invalidAddresses");
			for (Iterator iterator2 = userList.iterator(); iterator2.hasNext();) {
				User user = (User) iterator2.next();
				if (invalidAddresses.contains(user.getEmail()))
					iterator2.remove();
			}
			if (!userList.isEmpty())
				izinHataliMailGonder(userList, hataList, personelMap, izinCok);
			invalidAddresses = null;
		}
		return mailStatu;

	}

	/**
	 * @param userList
	 * @param personelList
	 * @param orjPersonelERPMap
	 * @param hataList
	 * @param personelERPHataliMap
	 * @param sirketMap
	 * @param mailBosGonder
	 * @return
	 */
	private MailStatu personelHataMailGonder(List<User> userList, List<PersonelERP> personelList, HashMap<String, PersonelERP> orjPersonelERPMap, List<PersonelERP> hataList, TreeMap<String, ERPPersonel> personelERPHataliMap, TreeMap<String, Sirket> sirketMap, boolean mailBosGonder) {
		MailStatu mailStatu = null;
		// boolean testDurum = getTestDurum();
		// if (testDurum)
		// hataList.clear();
		if (!hataList.isEmpty()) {
			if (mailBosGonder)
				logger.info("Hata kontrol ediliyor!");
			List<String> list = new ArrayList<String>();
			for (Iterator iterator = hataList.iterator(); iterator.hasNext();) {
				PersonelERP personelERP = (PersonelERP) iterator.next();
				if (personelERP.getHataList().isEmpty() || list.contains(personelERP.getPersonelNo()))
					iterator.remove();
				else {
					list.add(personelERP.getPersonelNo());
					if (personelERPHataliMap.containsKey(personelERP.getPersonelNo())) {
						ERPPersonel erpPersonel = personelERPHataliMap.get(personelERP.getPersonelNo());
						if (!erpPersonel.getDurum()) {
							personelERP.setHataList(null);
							iterator.remove();

						}
					}
				}
			}
			list = null;
		}
		if (!hataList.isEmpty() && personelList != null && personelList.size() > 1) {
			TreeMap<String, Tatil> tatilMap = getTatilGunleri(PdksUtil.tariheGunEkleCikar(bugun, -5), bugun);
			if (tatilMap.containsKey(PdksUtil.convertToDateString(bugun, "yyyyMMdd")))
				hataList.clear();
		}
		if (!hataList.isEmpty() && (mailBosGonder == false || PdksUtil.isPazar() == false)) {
			try {
				Gson gson = new Gson();
				String jsonStr = PdksUtil.toPrettyFormat(gson.toJson(hataList));
				String konu = uygulamaBordro + " savePersoneller problem";
				if (hataList.size() == 1) {
					PersonelERP personelERPData = hataList.get(0);
					PersonelERP personelERP = orjPersonelERPMap.containsKey(personelERPData.getPersonelNo()) ? orjPersonelERPMap.get(personelERPData.getPersonelNo()) : personelERPData;
					if (personelERP != null) {
						String adSoyad = (PdksUtil.hasStringValue(personelERP.getAdi()) ? personelERP.getAdi().trim() + " " : "") + (PdksUtil.hasStringValue(personelERP.getSoyadi()) ? personelERP.getSoyadi() : " ");
						if (PdksUtil.hasStringValue(adSoyad))
							konu += " [ " + (PdksUtil.hasStringValue(personelERP.getPersonelNo()) ? personelERP.getPersonelNo().trim() + " - " : "") + adSoyad.trim() + " ]";
					}
				}
				mailMap.put("konu", konu);
				StringBuffer sb = new StringBuffer();
				sb.append("<p><b>" + uygulamaBordro + " pdks entegrasyon servisi savePersoneller fonksiyonunda hatalı veri var!</b></p>");
				sb.append("<TABLE class=\"mars\" style=\"width: 80%\">");
				boolean renkUyari = true;
				Sirket bosSirket = new Sirket();
				if (parentBordroTanimKoduStr == null)
					parentBordroTanimKoduStr = "";

				for (Iterator iterator = hataList.iterator(); iterator.hasNext();) {
					PersonelERP personelERPData = (PersonelERP) iterator.next();
					PersonelERP personelERP = orjPersonelERPMap.containsKey(personelERPData.getPersonelNo()) ? orjPersonelERPMap.get(personelERPData.getPersonelNo()) : personelERPData;

					String adSoyad = (personelERP.getAdi() != null ? personelERP.getAdi().trim() + " " : "") + (personelERP.getSoyadi() != null ? personelERP.getSoyadi() : " ");
					String sirketBilgi = "";
					boolean altBolumVar = altBolumDurum && personelERP.getBordroAltAlanAdi() != null;
					if (personelERP.getSirketAdi() != null || personelERP.getBolumAdi() != null || altBolumVar) {
						sirketBilgi += "";
						if (personelERP.getSirketAdi() != null)
							sirketBilgi = personelERP.getSirketAdi();
						if (personelERP.getTesisAdi() != null) {
							Sirket sirket = sirketMap != null && sirketMap.containsKey(personelERP.getSirketKodu()) ? sirketMap.get(personelERP.getSirketKodu()) : bosSirket;
							if (sirket.isTesisDurumu()) {
								if (PdksUtil.hasStringValue(sirketBilgi))
									sirketBilgi += " - " + personelERP.getTesisAdi();
								else
									sirketBilgi = personelERP.getTesisAdi();
							}
						}

						if (personelERP.getBolumAdi() != null || altBolumVar) {
							if (personelERP.getBolumAdi() != null) {
								if (!PdksUtil.hasStringValue(sirketBilgi))
									sirketBilgi = personelERP.getBolumAdi();
								else
									sirketBilgi += " / " + personelERP.getBolumAdi();
							}
							if (altBolumVar && sirketBilgi.indexOf(personelERP.getBordroAltAlanAdi()) < 0) {
								if (!PdksUtil.hasStringValue(sirketBilgi))
									sirketBilgi = personelERP.getBordroAltAlanAdi();
								else
									sirketBilgi += (personelERP.getBolumAdi() != null ? " - " : " / ") + personelERP.getBordroAltAlanAdi();
							}
						}
						sirketBilgi = "( " + sirketBilgi + " )";
					}
					sb.append("<TR class=\"" + (renkUyari ? "odd" : "even") + "\"><TD colspan=2 nowrap><b>" + (personelERP.getPersonelNo() != null ? personelERP.getPersonelNo() + " " : "") + adSoyad + " " + sirketBilgi + "</b></TD></TR>");
					List<String> veriHataList = personelERP.getHataList();
					for (int i = 0; i < veriHataList.size(); i++) {
						sb.append("<TR class=\"" + (renkUyari ? "odd" : "even") + "\"><TD width='10%'></TD><TD width='90%' nowrap>" + (veriHataList.size() > 1 ? (i + 1) + ". " : "") + veriHataList.get(i) + "</TD></TR>");
					}
					renkUyari = !renkUyari;
					if (iterator.hasNext())
						sb.append("<TR><TD colspan=2> </TD></TR>");
				}
				sb.append("</TABLE>");
				mailMap.put("mailIcerik", sb.toString());
				LinkedHashMap<String, Object> fileMap = new LinkedHashMap<String, Object>();
				String xml = getJsonToXML(jsonStr, "personel", PERSONEL_PROP_ORDER, "savePersoneller");
				fileMap.put("savePersoneller.xml", xml);
				mailMap.put("fileMap", fileMap);
				mailMapGuncelle("bccEntegrasyon", "bccEntegrasyonAdres");
				kullaniciIKYukle(userList, mailMap, pdksDAO);
				mailStatu = MailManager.ePostaGonder(mailMap);
				mailBosGonder = false;
			} catch (Exception ex) {
				logger.error(ex);
				ex.printStackTrace();
			}
		}
		if (mailStatu != null && userList != null && userList.size() > 1 && mailMap.containsKey("invalidAddresses")) {
			List<String> invalidAddresses = (List<String>) mailMap.get("invalidAddresses");
			mailMap.remove("invalidAddresses");
			for (Iterator iterator2 = userList.iterator(); iterator2.hasNext();) {
				User user = (User) iterator2.next();
				if (invalidAddresses.contains(user.getEmail()))
					iterator2.remove();
			}
			invalidAddresses = null;
			if (!userList.isEmpty())
				personelHataMailGonder(userList, personelList, orjPersonelERPMap, hataList, personelERPHataliMap, sirketMap, mailBosGonder);
		}
		return mailStatu;
	}

	/**
	 * @param personelERP
	 */
	private void digerAlanlarBosalt(PersonelERP personelERP) {
		String veri = null;
		Object[] deger = new Object[] { veri };
		Class[] bos = new Class[] { String.class };
		for (int i = 1; i <= 10; i++) {
			String kod = (i < 10 ? "0" : "") + i;
			try {
				PdksUtil.runMethodObjectNull(personelERP, "setDigerTanimAlan" + kod, deger, bos);
				PdksUtil.runMethodObjectNull(personelERP, "setDigerTanimAlan" + kod + "Kodu", deger, bos);
			} catch (Exception e) {
				// TODO: handle exception
			}

		}
	}

	/**
	 * @param jsonStr
	 * @param arrayTag
	 * @param dizi
	 * @param rootName
	 * @return
	 */
	private String getJsonToXML(String jsonStr, String arrayTag, String[] dizi, String rootName) {
		jsonStr = "{\"" + arrayTag + "\":" + jsonStr + "}";
		Gson gson = new Gson();
		LinkedHashMap<String, Object> headerMap = gson.fromJson(jsonStr, LinkedHashMap.class);
		jsonStr = "";
		if (headerMap != null) {
			List<LinkedTreeMap> tableArray = (List<LinkedTreeMap>) headerMap.get(arrayTag);
			if (tableArray != null) {
				LinkedHashMap<String, String> map = WSLoggingOutInterceptor.getChangeMap();
				String hataList = "hataList";
				StringBuffer sb = new StringBuffer();
				for (LinkedTreeMap linkedHashMap : tableArray) {
					sb.append("<" + arrayTag + ">");
					for (String key : dizi) {
						if (linkedHashMap.containsKey(key)) {
							String str = linkedHashMap.get(key).toString();
							for (String pattern : map.keySet()) {
								String replace = map.get(pattern);
								if (str.indexOf(replace) >= 0)
									str = PdksUtil.replaceAll(str, replace, pattern);
							}
							sb.append("<" + key + ">" + str);
							sb.append("</" + key + ">");
						}
					}
					if (linkedHashMap.containsKey(hataList)) {
						List list1 = (List) linkedHashMap.get(hataList);
						for (Object object : list1) {
							String str = object.toString();
							for (String pattern : map.keySet()) {
								String replace = map.get(pattern);
								if (str.indexOf(replace) >= 0)
									str = PdksUtil.replaceAll(str, replace, pattern);
							}
							sb.append("<" + hataList + ">" + str);
							sb.append("</" + hataList + ">");
						}
					}
					sb.append("</" + arrayTag + ">");
				}

				jsonStr = PdksUtil.getJsonToXML(sb.toString(), rootName, null);
				for (String pattern : map.keySet()) {
					String replace = map.get(pattern);
					if (jsonStr.indexOf(pattern) >= 0)
						jsonStr = PdksUtil.replaceAllManuel(jsonStr, pattern, replace);
				}

			}
		}
		return jsonStr;
	}

	/**
	 * @param hataList
	 * @param object
	 * @param personel
	 * @param hataStr
	 */
	private void addHatalist(List hataList, Object object, Personel personel, String hataStr) {
		if (object != null && PdksUtil.hasStringValue(hataStr)) {
			String sirketKodu = null, tesisKodu = null;
			if (object instanceof PersonelERP) {
				PersonelERP personelERP = (PersonelERP) object;
				sirketKodu = personelERP.getSirketKodu() != null ? personelERP.getSirketKodu().trim() : "";
				tesisKodu = personelERP.getTesisKodu();
				personelERP.getHataList().add(hataStr);
			} else if (object instanceof IzinERP) {
				IzinERP izinERP = (IzinERP) object;
				izinERP.getHataList().add(hataStr);
				sirketKodu = "";
				if (personel != null) {
					if (personelSirket == null)
						personelSirket = personel.getSirket();
					if (personelSirket != null) {
						sirketKodu = personelSirket.getErpKodu();
						if (personelSirket.getTesisDurum() && personel.getTesis() != null)
							tesisKodu = personel.getTesis().getErpKodu();
					}
				}
			}
			if (sirketKodu != null) {
				if (ikUserMap == null) {
					HashMap<String, HashMap<String, List<User>>> map1 = getIKRollerUser();
					String[] roller = new String[] { Role.TIPI_IK_Tesis, Role.TIPI_IK_SIRKET, Role.TIPI_IK };
					ikUserMap = new LinkedHashMap<String, HashMap<String, List<User>>>();
					for (int i = 0; i < roller.length; i++) {
						String rolAdi = roller[i];
						if (map1.containsKey(rolAdi))
							ikUserMap.put(rolAdi, map1.get(rolAdi));
					}
					map1 = null;

				}
				if (!ikUserMap.isEmpty())
					addUserHataList(object, sirketKodu, tesisKodu);
			}
			if (hataList != null)
				hataList.add(object);
		}

	}

	/**
	 * @param data
	 * @param sirketKodu
	 * @param tesisKodu
	 */
	private void addUserHataList(Object data, String sirketKodu, String tesisKodu) {
		Departman departman = personelSirket != null ? personelSirket.getDepartman() : null;
		for (String rolAdi : ikUserMap.keySet()) {
			HashMap<String, List<User>> map1 = ikUserMap.get(rolAdi);
			List<User> userList = null;
			String anaKey = "";
			if (rolAdi.equals(Role.TIPI_IK_Tesis)) {
				if (map1.containsKey(tesisKodu)) {
					anaKey = rolAdi + "_" + tesisKodu;
					userList = map1.get(tesisKodu);
				}

			} else if (rolAdi.equals(Role.TIPI_IK_SIRKET)) {
				if (map1.containsKey(sirketKodu)) {
					anaKey = rolAdi + "_" + sirketKodu;
					userList = map1.get(sirketKodu);
				}

			} else if (rolAdi.equals(Role.TIPI_IK)) {
				for (String key : map1.keySet()) {
					List<User> userList1 = map1.get(key);
					for (User user : userList1) {
						if (key.equals(TIPI_IK_ADMIN) || (departman != null && departman.getId().equals(user.getDepartman().getId()))) {
							anaKey = key.equals(TIPI_IK_ADMIN) ? key : rolAdi + "_" + key;
							if (userList == null)
								userList = new ArrayList<User>();
							userList.add(user);
						}

					}
				}

			}
			if (userList != null) {
				if (hataIKMap == null)
					hataIKMap = new LinkedHashMap<String, HashMap<String, List>>();
				HashMap<String, List> dataMap = hataIKMap.containsKey(anaKey) ? hataIKMap.get(anaKey) : new HashMap<String, List>();
				List<Object> hataList = null;
				if (dataMap.isEmpty()) {
					hataList = new ArrayList<Object>();
					dataMap.put("userList", userList);
					dataMap.put("hataList", hataList);
					hataIKMap.put(anaKey, dataMap);
				} else
					hataList = dataMap.get("hataList");
				hataList.add(data);

				break;
			}
		}

	}

	/**
	 * @param list
	 * @param str
	 */
	private void addHatalist(List<String> list, String str) {
		if (PdksUtil.hasStringValue(str)) {
			list.add(str);
		} else {
			logger.debug("aa");
		}

	}

	public String getParameterKey(String key) {
		String parameterKey = null;
		try {
			parameterKey = mailMap != null && mailMap.containsKey(key) ? ((String) mailMap.get(key)).trim() : "";
		} catch (Exception e) {
			parameterKey = "";
		}
		logger.debug(key + " = '" + parameterKey + "'");
		return parameterKey;

	}

	/**
	 * @param key
	 * @param defaultBaslik
	 * @return
	 */
	private String getBaslikAciklama(String key, String defaultBaslik) {
		String aciklama = getParameterKey(key);
		if (!PdksUtil.hasStringValue(aciklama))
			aciklama = defaultBaslik;
		return aciklama;
	}

	public HashMap<String, Object> getMailMap() {
		return mailMap;
	}

	public void setMailMap(HashMap<String, Object> mailMap) {
		this.mailMap = mailMap;
	}

	public User getIslemYapan() {
		return islemYapan;
	}

	public void setIslemYapan(User islemYapan) {
		this.islemYapan = islemYapan;
	}

	public VardiyaSablonu getVardiyaSablonu() {
		return vardiyaSablonu;
	}

	public void setVardiyaSablonu(VardiyaSablonu vardiyaSablonu) {
		this.vardiyaSablonu = vardiyaSablonu;
	}

	public String getMesaj() {
		return mesaj;
	}

	public void setMesaj(String mesaj) {
		this.mesaj = mesaj;
	}

	public String getDosyaEkAdi() {
		return dosyaEkAdi;
	}

	public void setDosyaEkAdi(String dosyaEkAdi) {
		this.dosyaEkAdi = dosyaEkAdi;
	}

	public Tanim getBosDepartman() {
		return bosDepartman;
	}

	public void setBosDepartman(Tanim bosDepartman) {
		this.bosDepartman = bosDepartman;
	}

	public static String getSelectLOCK() {
		return selectLOCK;
	}

	public static void setSelectLOCK(String selectLOCK) {
		PdksVeriOrtakAktar.selectLOCK = selectLOCK;
	}

	public static String getJoinLOCK() {
		return joinLOCK;
	}

	public static void setJoinLOCK(String joinLOCK) {
		PdksVeriOrtakAktar.joinLOCK = joinLOCK;
	}

	public boolean isErpVeriOku() {
		return erpVeriOku;
	}

	public void setErpVeriOku(boolean erpVeriOku) {
		this.erpVeriOku = erpVeriOku;
	}
}
