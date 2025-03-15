package org.pdks.session;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.engine.SessionImplementor;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.Transactional;
import org.pdks.entity.BaseObject;
import org.pdks.entity.BasePDKSObject;
import org.pdks.entity.HareketKGS;
import org.pdks.entity.KapiView;
import org.pdks.entity.Liste;
import org.pdks.entity.PersonelKGS;
import org.pdks.entity.PersonelView;
import org.pdks.security.entity.User;

import com.google.gson.Gson;

@Startup(depends = { "entityManager" })
@Scope(ScopeType.APPLICATION)
@Name("pdksEntityController")
public class PdksEntityController implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1084375085063213335L;

	public static final int LIST_MAX_SIZE = 1000;
	private static boolean showSQL = Boolean.FALSE;

	public static final String MAP_KEY_MAP = "Map";
	public static final String MAP_KEY_OR = "or";
	public static final String MAP_KEY_ORDER = "order";
	public static final String MAP_KEY_USER = "key_User";
	public static final String MAP_KEY_SHOW_SQL = "key_Show_Sql";
	public static final String MAP_KEY_SELECT = "select";
	public static final String MAP_KEY_SESSION = "session";
	public static final String MAP_KEY_SQLADD = "sql_add";
	public static final String MAP_KEY_SQLPARAMS = "sql_params";
	public static final String MAP_KEY_TRANSACTION = "transaction";
	public static final String TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED = "read uncommitted";
	public static final String TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED = "read committed";
	private static String selectLOCK = "with(nolock)", joinLOCK = "with(nolock)";
	private static boolean readUnCommitted = false;

	public static final String SELECT_KARAKTER = "c";

	static Logger logger = Logger.getLogger(PdksEntityController.class);

	@In(required = false, create = true)
	EntityManager entityManager;
	@In(required = false, create = true)
	User authenticatedUser;

	/**
	 * @param fieldValue
	 * @return
	 */
	private String getStringParse(Object fieldValue) {
		String stringParse = "";
		if (fieldValue != null) {
			StringBuilder str = new StringBuilder();
			if (fieldValue instanceof Collection) {
				for (Iterator iter = ((Collection) fieldValue).iterator(); iter.hasNext();) {
					String element = (String) iter.next();
					str.append(" " + SELECT_KARAKTER + "." + element + (iter.hasNext() ? "," : ""));
				}
			} else if (fieldValue instanceof String) {
				String fieldValueStr = (String) fieldValue;
				if (PdksUtil.hasStringValue(fieldValueStr)) {
					StringTokenizer st = new StringTokenizer(fieldValueStr, ",");
					while (st.hasMoreTokens())
						str.append(" " + SELECT_KARAKTER + "." + (String) st.nextToken() + (st.hasMoreTokens() ? "," : ""));
				}
			}
			if (str.length() > 0)
				stringParse = str.toString();
		}
		return stringParse;
	}

	/**
	 * @param indis
	 * @param listKey
	 * @param fieldsOrj
	 * @param toplamList
	 * @param class1
	 * @param esit
	 */
	private void getObjectByInnerObjectListParcala(int indis, List<Liste> listKey, HashMap fieldsOrj, List toplamList, Class class1, String esit) {
		String fieldName = (String) listKey.get(indis).getId();
		ArrayList kriterList = new ArrayList((Collection) fieldsOrj.get(fieldName));
		Double maxAdetR = (double) LIST_MAX_SIZE;
		for (int i = 0; i <= indis; i++)
			maxAdetR = maxAdetR / 2;
		int maxAdet = maxAdetR.intValue();
		HashMap map = new HashMap();
		map.putAll(fieldsOrj);

		Boolean sessionYok = !map.containsKey(MAP_KEY_SESSION);
		Session session = !sessionYok ? (Session) map.get(MAP_KEY_SESSION) : null;
		while (!kriterList.isEmpty()) {
			ArrayList yeniListe = new ArrayList();
			if (kriterList.size() > maxAdet) {
				for (int i = 0; i < maxAdet; i++) {
					yeniListe.add(kriterList.get(0));
					kriterList.remove(0);
				}
			} else {
				yeniListe.addAll(kriterList);
				kriterList.clear();
			}
			map.put(fieldName, yeniListe);
			// Session session=map.containsKey(MAP_KEY_SESSION)?(Session)map.get(MAP_KEY_SESSION):null;
			if (indis != listKey.size() - 1) {
				getObjectByInnerObjectListParcala(indis + 1, listKey, map, toplamList, class1, esit);
			}

			else {

				List list = getObjectByInnerObjectList(map, class1, esit, null);
				if (!list.isEmpty())
					toplamList.addAll(list);
				list = null;
			}
			yeniListe = null;
		}
		if (!sessionYok && session != null)
			fieldsOrj.put(MAP_KEY_SESSION, session);
		map = null;
	}

	/**
	 * @param fieldsOrj
	 * @param class1
	 * @param esit
	 * @return
	 */
	private List getObjectByInnerObjectList(HashMap fieldsOrj, Class class1, String esit) {
		List objectList = null;
		List<Liste> listKey = null;
		HashMap<String, String> ozelMap = new HashMap<String, String>();
		ozelMap.put(MAP_KEY_SQLADD, "");
		ozelMap.put(MAP_KEY_SESSION, "");
		ozelMap.put(MAP_KEY_SHOW_SQL, "");
		ozelMap.put(MAP_KEY_USER, "");
		ozelMap.put(MAP_KEY_SELECT, "");
		ozelMap.put(MAP_KEY_ORDER, "");
		ozelMap.put(MAP_KEY_OR, "");
		int parametreAdet = 0;
		int parametreSayisi = 0;
		int collectionCount = 0;
		String keyAlan = null;
		for (Iterator iterator = fieldsOrj.keySet().iterator(); iterator.hasNext();) {
			String fieldName = (String) iterator.next();
			Object fieldValue = fieldsOrj.get(fieldName);
			String mapFields = fieldName.toLowerCase();
			if (!fieldName.equals(MAP_KEY_SESSION) && !fieldName.equals(MAP_KEY_SELECT))
				++parametreSayisi;
			if (ozelMap.containsKey(mapFields))
				continue;
			if (fieldValue != null) {
				if (fieldValue instanceof Collection) {
					keyAlan = fieldName;
					++collectionCount;
					ArrayList liste = new ArrayList((Collection) fieldValue);
					if (!fieldName.equals(MAP_KEY_SQLPARAMS)) {
						if (listKey == null)
							listKey = new ArrayList<Liste>();
						Liste listeKey = new Liste(fieldName, liste.size());
						listKey.add(listeKey);
					}
					parametreAdet += liste.size();
				} else
					++parametreAdet;
			}
		}
		ozelMap = null;
		if (parametreSayisi == 1 && collectionCount == 1)
			objectList = tekListeVerisiGetir(fieldsOrj, class1, keyAlan);
		else {
			if (listKey != null) {
				if (parametreAdet > LIST_MAX_SIZE) {
					if (listKey.size() > 1)
						listKey = PdksUtil.sortListByAlanAdi(listKey, "value", true);
					objectList = new ArrayList();
					getObjectByInnerObjectListParcala(0, listKey, fieldsOrj, objectList, class1, esit);
				}
				listKey = null;
			}
			if (objectList == null)
				objectList = getObjectByInnerObjectList(fieldsOrj, class1, esit, null);

		}

		return objectList;

	}

	/**
	 * @param parametreMap
	 * @param class1
	 * @param keyAlan
	 * @return
	 */
	private List tekListeVerisiGetir(HashMap parametreMap, Class class1, String keyAlan) {
		List objectList = new ArrayList();
		Session session = null;
		boolean sessionYok = !parametreMap.containsKey(MAP_KEY_SESSION);
		if (!sessionYok)
			session = (Session) parametreMap.get(MAP_KEY_SESSION);
		else if (authenticatedUser != null) {
			session = authenticatedUser.getSessionSQL();
		}
		if (session == null)
			session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		List list = new ArrayList((Collection) parametreMap.get(keyAlan));
		ArrayList parametreList = new ArrayList();
		StringBuilder strArray = null;
		String select = parametreMap.containsKey(MAP_KEY_SELECT) ? (String) parametreMap.get(MAP_KEY_SELECT) : "";
		while (session != null && !list.isEmpty()) {
			strArray = new StringBuilder();
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				Object object = (Object) iterator.next();
				strArray.append((!parametreList.isEmpty() ? "," : "") + "?");
				parametreList.add(object);
				iterator.remove();
				if (parametreList.size() >= LIST_MAX_SIZE)
					break;
			}
			String query = " where " + SELECT_KARAKTER + "." + keyAlan + (parametreList.size() > 1 ? " in (" + strArray.toString() + ")" : "=" + strArray.toString());
			String sql = MAP_KEY_SELECT + " " + (PdksUtil.hasStringValue(select) == false ? " " + SELECT_KARAKTER + " " : select.trim()) + " from " + class1.getName() + " " + SELECT_KARAKTER + " " + query.toString();
			if (showSQL)
				logger.info(sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));
			else if (sessionYok) {
				sessionYok = Boolean.FALSE;
				String hata = (authenticatedUser != null && authenticatedUser.getCalistigiSayfa() != null ? authenticatedUser.getCalistigiSayfa() + " --> " : "") + sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss");
				logger.error(hata);
				try {
					PdksUtil.fileWriteEkle("<hata>" + hata + "</hata>", "error_" + PdksUtil.convertToDateString(new Date(), "yyyyMM"));
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
			SQLQuery queryReadUnCommitted = null;
			if (readUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			org.hibernate.Query qry1 = session.createQuery(sql);
			if (!parametreList.isEmpty()) {
				for (int i = 0; i < parametreList.size(); i++) {
					Object parametre = parametreList.get(i);
					qry1.setParameter(i, parametre);
				}
				parametreList.clear();
			}
			List liste = qry1.list();
			if (queryReadUnCommitted != null) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			if (showSQL)
				logger.info(sql + " out " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));

			if (!liste.isEmpty())
				objectList.addAll(liste);
			liste = null;
			strArray = null;

		}
		if (!sessionYok && session != null)
			parametreMap.put(MAP_KEY_SESSION, session);
		parametreList = null;
		list = null;
		return objectList;
	}

	/**
	 * @param fields
	 * @param class1
	 * @param esit
	 * @param pEntityManager
	 * @return
	 */
	private List getObjectByInnerObjectList(HashMap fields, Class class1, String esit, EntityManager pEntityManager) {
		if (esit == null)
			esit = "";
		else if (!PdksUtil.hasStringValue(esit))
			esit = "=";
		int parametreSayac = 1;
		Object fieldValue;
		List list = null;
		Boolean showSQLString = showSQL;
		Boolean sessionYok = !fields.containsKey(MAP_KEY_SESSION);
		Session session = null;
		if (class1 != null) {
			ArrayList parametreList = new ArrayList();
			StringBuilder query = new StringBuilder();
			String order = "";
			String select = "";
			String or = null;

			boolean yeniSession = fields.containsKey(MAP_KEY_SESSION);
			String userAdi = null;
			Object tr = null;
			if (fields.containsKey(MAP_KEY_SHOW_SQL)) {
				showSQLString = (Boolean) fields.get(MAP_KEY_SHOW_SQL);
				fields.remove(MAP_KEY_SHOW_SQL);
			}
			if (fields.containsKey(MAP_KEY_TRANSACTION)) {
				tr = fields.get(MAP_KEY_TRANSACTION);

				fields.remove(MAP_KEY_TRANSACTION);
			}

			if (fields.containsKey(MAP_KEY_USER)) {
				userAdi = (String) fields.get(MAP_KEY_USER);
				fields.remove(MAP_KEY_USER);
			}

			// Integer ti = null;
			// Connection cn = null;
			if (!fields.isEmpty()) {

				if (yeniSession) {
					session = (Session) fields.get(MAP_KEY_SESSION);
					fields.remove(MAP_KEY_SESSION);
				} else if (authenticatedUser != null) {
					session = authenticatedUser.getSessionSQL();
				}
				if (session == null)
					session = PdksUtil.getSession(pEntityManager == null ? entityManager : pEntityManager, Boolean.FALSE);
				StringBuilder strArray = null;

				for (Iterator iter = fields.keySet().iterator(); iter.hasNext();) {
					String fieldNameReal = (String) iter.next();
					fieldValue = fields.get(fieldNameReal);
					String fieldName = fieldNameReal.trim();
					if (fieldValue == null) {
						query.append((query.length() > 0 ? " and " : " where ") + SELECT_KARAKTER + "." + fieldName + " " + esit + " null");
						continue;
					}
					if (fieldName.toLowerCase().trim().equals(MAP_KEY_SQLPARAMS)) {
						continue;
					} else if (fieldName.toLowerCase().trim().equals(MAP_KEY_SQLADD)) {
						if (fields.containsKey(MAP_KEY_SQLPARAMS))
							parametreList.addAll((Collection) fields.get(MAP_KEY_SQLPARAMS));
						query.append((query.length() > 0 ? " and " : " where ") + "(" + fieldValue + ")");
						continue;
					} else

					if (fieldName.toLowerCase().trim().equals(MAP_KEY_OR))
						or = " " + MAP_KEY_OR + " ";
					else if (fieldName.toLowerCase().trim().equals(MAP_KEY_SELECT)) {
						fieldValue = fields.get(fieldName);
						select = getStringParse(fieldValue);
					} else if (fieldName.toLowerCase().trim().equals(MAP_KEY_ORDER)) {
						fieldValue = fields.get(fieldName);
						order = " " + MAP_KEY_ORDER + " by " + getStringParse(fieldValue);
					} else {
						boolean listeBos = Boolean.FALSE;
						StringBuilder str = new StringBuilder();
						if (fieldValue != null) {
							if (fieldValue instanceof Collection) {
								listeBos = ((Collection) fieldValue).isEmpty();
								if (listeBos) {
									query.append((query.length() > 0 ? " and " : " where ") + "1=2");
									continue;
								}
								strArray = new StringBuilder();
								for (Iterator iter1 = ((List) fieldValue).iterator(); iter1.hasNext();) {
									Object fieldListValue = iter1.next();
									strArray.append((session == null ? (parametreSayac++) : "?") + (iter1.hasNext() ? "," : ""));
									parametreList.add(fieldListValue);
								}
								str.append(strArray.toString().indexOf(",") > 0 ? "in (" + strArray.toString() + ")" : "= " + strArray.toString());
								strArray = null;
							} else {
								str.append(esit + "?" + (session == null ? (parametreSayac++) : ""));
								parametreList.add(fieldValue);
							}
						}
						query.append((query.length() > 0 ? " and " : " where ") + SELECT_KARAKTER + "." + fieldName + " " + str.toString());
						str = null;
					}
				}
			} else if (authenticatedUser != null) {
				session = authenticatedUser.getSessionSQL();
			}
			if (session == null)
				session = PdksUtil.getSession(pEntityManager == null ? entityManager : pEntityManager, Boolean.FALSE);
			String sql = MAP_KEY_SELECT + " " + (PdksUtil.hasStringValue(select) == false ? " " + SELECT_KARAKTER + " " : select.trim()) + " from " + class1.getName() + " " + SELECT_KARAKTER + " " + query.toString() + order;
			if (query.indexOf(" and ") > 0 && or != null)
				sql = replaceAll(sql, " and ", or);
			select = null;
			order = null;

			if (showSQLString)
				logger.info((userAdi != null ? userAdi + " --> " : "") + sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));
			if (sessionYok) {
				String hata = (authenticatedUser != null && authenticatedUser.getCalistigiSayfa() != null ? authenticatedUser.getCalistigiSayfa() + " --> " : "") + sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss");
				logger.error(hata);
				try {
					PdksUtil.fileWriteEkle("<hata>" + hata + "</hata>", "error_" + PdksUtil.convertToDateString(new Date(), "yyyyMM"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			if (session != null) {
				SQLQuery queryReadUnCommitted = null;
				if (readUnCommitted) {
					queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
					queryReadUnCommitted.executeUpdate();
				}
				org.hibernate.Query qry1 = session.createQuery(sql);
				if (!parametreList.isEmpty()) {
					if (parametreList.size() > 1500)
						logger.info(sql);
					for (int i = 0; i < parametreList.size(); i++) {
						Object parametre = parametreList.get(i);
						qry1.setParameter(i, parametre);
					}
				}
				list = qry1.list();
				if (queryReadUnCommitted != null) {
					queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
					queryReadUnCommitted.executeUpdate();
				}

				qry1 = null;
			} else {
				Query qry = null;
				if (pEntityManager == null)
					qry = entityManager.createQuery(sql);
				else
					qry = pEntityManager.createQuery(sql);
				if (!parametreList.isEmpty()) {
					parametreSayac = 0;
					for (Object parametre : parametreList) {
						qry.setParameter(++parametreSayac, parametre);

					}
				}
				list = qry.getResultList();
				qry = null;
			}
			query = null;

			parametreList = null;
			if (showSQLString)
				logger.info((userAdi != null ? userAdi + " --> " : "") + sql + " out " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));
			if (tr != null)
				fields.put(MAP_KEY_TRANSACTION, tr);
		}
		if (!sessionYok && session != null)
			fields.put(MAP_KEY_SESSION, session);
		return list;

	}

	public static String replaceAll(String str, String pattern, String replace) {
		StringBuilder lSb = new StringBuilder();
		if ((str != null) && (pattern != null) && (pattern.length() > 0) && (replace != null)) {
			int i = 0;
			int j = str.indexOf(pattern, i);
			int l = pattern.length();
			int m = str.length();
			if (j > -1) {
				while (j > -1) {
					if (i != j)
						lSb.append(str.substring(i, j));

					lSb.append(replace);
					i = j + l;
					j = (i > m) ? -1 : str.indexOf(pattern, i);
				}
				if (i < m)
					lSb.append(str.substring(i));

			} else
				lSb.append(str);

		}
		return lSb.toString();
	}

	public Object save(Object object, Session session) {
		if (session == null) {
			if (authenticatedUser != null)
				session = authenticatedUser.getSessionSQL();
			if (session == null)
				session = PdksUtil.getSession(entityManager, Boolean.FALSE);
		}

		session.saveOrUpdate(object);

		return object;
	}

	/**
	 * @param ses
	 * @param em
	 * @param saveObject
	 */
	public void saveOrUpdate(Session ses, EntityManager em, Object saveObject) {
		try {
			ses.saveOrUpdate(saveObject);
		} catch (Exception e) {
			if (em != null)
				ses.saveOrUpdate(getEntityManagerObject(em, saveObject));
		}
	}

	/**
	 * @param ses
	 * @param em
	 * @param del
	 */
	public void deleteObject(Session ses, EntityManager em, Object del) {
		try {
			ses.delete(del);
		} catch (Exception e) {
			if (em != null)
				ses.delete(getEntityManagerObject(em, del));

		}
	}

	/**
	 * @param em
	 * @param saveObject
	 * @return
	 */
	public Object getEntityManagerObject(EntityManager em, Object saveObject) {
		Object object = em == null || em.contains(saveObject) ? saveObject : em.merge(saveObject);
		return object;
	}

	/**
	 * @param fields
	 * @param class1
	 * @param esit
	 * @return
	 */
	public Object getObjectByInnerObject(HashMap fields, Class class1, String esit) {
		ArrayList<Object> tempObjectList = (ArrayList<Object>) getObjectByInnerObjectList(fields, class1, esit);
		return (tempObjectList != null && !tempObjectList.isEmpty() ? tempObjectList.get(0) : null);
	}

	public List getObjectByInnerObjectListInLogic(HashMap fields, Class class1) {
		return getObjectByInnerObjectList(fields, class1, null);
	}

	public Object getObjectByInnerObjectInLogic(HashMap fields, Class class1) {
		List list = getObjectByInnerObjectList(fields, class1, null);
		return !list.isEmpty() ? list.get(0) : null;
	}

	public List getObjectByInnerObjectList(HashMap fields, Class class1) {
		return getObjectByInnerObjectList(fields, class1, "");

	}

	public Object getObjectByInnerObject(HashMap fields, Class class1) {
		List list = getObjectByInnerObjectList(fields, class1);
		return (list != null && !list.isEmpty() ? list.get(0) : null);

	}

	public TreeMap getObjectByInnerObjectMap(HashMap map, Class class1, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		if (map.containsKey(MAP_KEY_MAP)) {
			String method = (String) map.get(MAP_KEY_MAP);
			if (PdksUtil.hasStringValue(method)) {
				map.remove(MAP_KEY_MAP);
				List list = getObjectByInnerObjectList(map, class1);
				if (!list.isEmpty())
					treeMap = getTreeMapByList(list, method, uzerineYaz);
			}

		}
		return treeMap;
	}

	public TreeMap getObjectByInnerObjectMapInLogic(HashMap map, Class class1, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		if (map.containsKey(MAP_KEY_MAP)) {
			String method = (String) map.get(MAP_KEY_MAP);
			if (PdksUtil.hasStringValue(method)) {
				map.remove(MAP_KEY_MAP);
				List list = getObjectByInnerObjectListInLogic(map, class1);
				if (!list.isEmpty())
					treeMap = getTreeMapByList(list, method, uzerineYaz);
			}

		}

		return treeMap;
	}

	/**
	 * @param list
	 * @param method
	 * @param uzerineYaz
	 * @return
	 */
	public TreeMap getTreeMapByList(List list, String method, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			Object object = (Object) iter.next();
			try {
				Object key = PdksUtil.getMethodObject(object, method, null);
				if (key != null)
					if (uzerineYaz || !treeMap.containsKey(key))
						treeMap.put(key, object);
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());

			}
		}
		return treeMap;
	}

	/**
	 * @param veriMap
	 * @param sp
	 * @return
	 * @throws Exception
	 */
	public List execFNList(LinkedHashMap<String, Object> veriMap, StringBuffer sp) throws Exception {
 		List sonucList = null;
		boolean manuelReadUnCommitted = false;
		try {
			if (veriMap.containsKey("readUnCommitted")) {
				manuelReadUnCommitted = true;
				veriMap.remove("readUnCommitted");
			}
			Session session = veriMap.containsKey(MAP_KEY_SESSION) ? (Session) veriMap.get(MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
			SQLQuery queryReadUnCommitted = null;
			if (readUnCommitted || manuelReadUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			SQLQuery query = prepareFunction(veriMap, sp);
 			sonucList = query.list();
			if (queryReadUnCommitted != null) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
		} catch (Exception e) {
			Gson gson = new Gson();
			logger.error(sp.toString() + (veriMap != null && !veriMap.isEmpty() ? "\n" + gson.toJson(veriMap) : "") + "\n" + e);
			gson = null;
			throw new Exception(e);
		}
		if (manuelReadUnCommitted)
			veriMap.put("readUnCommitted", true);

		return sonucList;

	}

	/**
	 * @param veriMap
	 * @param sp
	 * @param class1
	 * @return
	 * @throws Exception
	 */
	public List execSPList(LinkedHashMap<String, Object> veriMap, StringBuffer sp, Class class1) throws Exception {
		List sonucList = null;
		boolean manuelReadUnCommitted = false;
		try {
			if (veriMap.containsKey("readUnCommitted")) {
				manuelReadUnCommitted = true;
				veriMap.remove("readUnCommitted");
			}
			Session session = veriMap.containsKey(MAP_KEY_SESSION) ? (Session) veriMap.get(MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
			SQLQuery queryReadUnCommitted = null;
			if (readUnCommitted || manuelReadUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			SQLQuery query = prepareProcedure(veriMap, sp);
			if (class1 != null)
				query.addEntity(class1);
			sonucList = query.list();
			if (queryReadUnCommitted != null) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
		} catch (Exception e) {
			Gson gson = new Gson();
			logger.error(sp.toString() + (veriMap != null && !veriMap.isEmpty() ? "\n" + gson.toJson(veriMap) : "") + "\n" + e);
			gson = null;
			throw new Exception(e);
		}
		if (manuelReadUnCommitted)
			veriMap.put("readUnCommitted", true);

		return sonucList;

	}

	/**
	 * @param veriMap
	 * @param sp
	 */
	@Transactional
	public int execSP(LinkedHashMap<String, Object> veriMap, StringBuffer sp) throws Exception {
		Integer sonuc = null;
		boolean manuelReadUnCommitted = false;
		try {
			if (veriMap.containsKey("readUnCommitted")) {
				manuelReadUnCommitted = true;
				veriMap.remove("readUnCommitted");
			}
			Session session = veriMap.containsKey(MAP_KEY_SESSION) ? (Session) veriMap.get(MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
			SQLQuery queryReadUnCommitted = null;
			if (readUnCommitted || manuelReadUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			SQLQuery query = prepareProcedure(veriMap, sp);
			sonuc = query.executeUpdate();
			if (queryReadUnCommitted != null) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
		} catch (Exception e) {
			Gson gson = new Gson();
			logger.error(sp.toString() + (veriMap != null && !veriMap.isEmpty() ? "\n" + gson.toJson(veriMap) : "") + "\n" + e);
			gson = null;
		}
		if (manuelReadUnCommitted)
			veriMap.put("readUnCommitted", true);
		return sonuc;

	}

	/**
	 * @param level
	 * @return
	 */
	private String setTransactionIsolationLevel(String level) {
		String levelStr = "set transaction isolation level " + level;
		return levelStr;
	}

	/**
	 * @param veriMap
	 * @param sp
	 * @return
	 */
	private SQLQuery prepareProcedure(LinkedHashMap<String, Object> veriMap, StringBuffer sp) {
		String queryStr = "exec " + sp.toString();
		Session session = veriMap.containsKey(MAP_KEY_SESSION) ? (Session) veriMap.get(MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
		if (veriMap.containsKey(MAP_KEY_SESSION))
			veriMap.remove(MAP_KEY_SESSION);
		for (Iterator iterator = veriMap.keySet().iterator(); iterator.hasNext();) {
			String string = (String) iterator.next();
			if (string != null) {
				queryStr += " :" + string;
				// queryStr += " :" + string;
				if (iterator.hasNext())
					queryStr += ",";
			}
		}
		SQLQuery query = session.createSQLQuery(queryStr);
		logger.debug(queryStr);
		for (Iterator iterator = veriMap.keySet().iterator(); iterator.hasNext();) {
			String key1 = (String) iterator.next();
			if (key1 != null) {
				Object veri = veriMap.get(key1);
				query.setParameter(key1, veri);
			}
		}
		return query;
	}

	/**
	 * @param veriMap
	 * @param sp
	 * @return
	 */
	private SQLQuery prepareFunction(LinkedHashMap<String, Object> veriMap, StringBuffer sp) {
		String queryStr = "select dbo." + sp.toString() + " ( ";
		Session session = veriMap.containsKey(MAP_KEY_SESSION) ? (Session) veriMap.get(MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
		if (veriMap.containsKey(MAP_KEY_SESSION))
			veriMap.remove(MAP_KEY_SESSION);
		if (veriMap != null) {
			for (Iterator iterator = veriMap.keySet().iterator(); iterator.hasNext();) {
				String string = (String) iterator.next();
				if (string != null) {
					queryStr += " :" + string;
					// queryStr += " :" + string;
					if (iterator.hasNext())
						queryStr += ",";
				}
			}

		}
		queryStr += " )";

		SQLQuery query = session.createSQLQuery(queryStr);
		logger.debug(queryStr);
		for (Iterator iterator = veriMap.keySet().iterator(); iterator.hasNext();) {
			String key1 = (String) iterator.next();
			if (key1 != null) {
				Object veri = veriMap.get(key1);
				query.setParameter(key1, veri);
			}
		}
		return query;
	}

	/**
	 * @param kapi
	 * @param personel
	 * @param zaman
	 * @param guncelleyen
	 * @param nedenId
	 * @param aciklama
	 * @return
	 */
	public HareketKGS hareketSistemEkleReturn(KapiView kapi, PersonelKGS personel, Date zaman, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		HareketKGS id = null;
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		StringBuffer sp = new StringBuffer("SP_SISTEM_HAREKET_EKLE_RETURN");
		veriMap.put("kapi", kapi.getId());
		veriMap.put("personel", personel.getId());
		veriMap.put("zaman", zaman);

		StringBuffer sb = new StringBuffer();
		try {
			List<BigDecimal> list = execSPList(veriMap, sp, null);
			Long kgsId = null;
			if (!list.isEmpty()) {
				kgsId = list.get(0).longValue();
				HashMap fields = new HashMap();
				fields.put("hareketId", kgsId);
				fields.put(MAP_KEY_SESSION, session);
				sb.append("select 'K' + CAST(Z." + HareketKGS.COLUMN_NAME_ID + " as VARCHAR(12)) as ID, 'K' as SIRKET, Z." + HareketKGS.COLUMN_NAME_ID + " as TABLE_ID, Z.USERID as " + HareketKGS.COLUMN_NAME_PERSONEL + " ,");
				sb.append(" Z.KAPIID as " + HareketKGS.COLUMN_NAME_KAPI + ", Z.HAREKET_ZAMANI as ZAMAN, Z.DURUM, Z.ISLEM_ID, NULL as ORJ_ZAMAN,Z.OLUSTURMA_ZAMANI,Z.KGS_SIRKET_ID from " + HareketKGS.TABLE_NAME + " as Z " + selectLOCK);
				sb.append(" where " + HareketKGS.COLUMN_NAME_ID + " = :hareketId");
				List<HareketKGS> list1 = getObjectBySQLList(sb, fields, HareketKGS.class);
				if (!list1.isEmpty())
					id = list1.get(0);
			}

		} catch (Exception e) {
			logger.error(e + " " + sb.toString());
		}

		return id;
	}

	/**
	 * @param kapi
	 * @param personel
	 * @param zaman
	 * @param guncelleyen
	 * @param nedenId
	 * @param aciklama
	 * @return
	 */
	public Long hareketEkle(KapiView kapi, PersonelView personel, Date zaman, User guncelleyen, long nedenId, String aciklama, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		StringBuffer sp = new StringBuffer("SP_HAREKET_EKLE");
		veriMap.put("kapi", kapi.getId());
		veriMap.put("personel", personel.getId());
		veriMap.put("zaman", zaman);
		veriMap.put("guncelleyenId", guncelleyen.getId());
		veriMap.put("nedenId", nedenId);
		veriMap.put("aciklama", aciklama);
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		Long sonuc = null;
		try {
			List<BigDecimal> list = execSPList(veriMap, sp, null);
			if (!list.isEmpty())
				sonuc = list.get(0).longValue();
		} catch (Exception e) {
			sonuc = 0L;
		}
		return sonuc;
	}

	/**
	 * @param kgsId
	 * @param pdksId
	 * @param zaman
	 * @param guncelleyen
	 * @param nedenId
	 * @param aciklama
	 * @param sirketId
	 * @param session
	 * @return
	 */
	public Long hareketGuncelle(long kgsId, long pdksId, Date zaman, User guncelleyen, long nedenId, String aciklama, long sirketId, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		StringBuffer sp = new StringBuffer("SP_HAREKET_GUNCELLE_SIRKET");
		veriMap.put("kgsId", kgsId);
		veriMap.put("pdksId", pdksId);
		veriMap.put("zaman", zaman);
		veriMap.put("guncelleyen", guncelleyen.getId());
		veriMap.put("nedenId", nedenId);
		veriMap.put("aciklama", aciklama);

		veriMap.put("sirketId", sirketId);
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		Long sonuc = 0L;
		try {
			List<BigDecimal> list = execSPList(veriMap, sp, null);
			if (!list.isEmpty())
				sonuc = list.get(0).longValue();
		} catch (Exception e) {
			sonuc = 0L;
		}
		return sonuc;
	}

	/**
	 * @param kgsId
	 * @param pdksId
	 * @param guncelleyen
	 * @param nedenId
	 * @param aciklama
	 * @param sirketId
	 * 
	 * @param session
	 * @return
	 */
	public Long hareketSil(long kgsId, long pdksId, User guncelleyen, long nedenId, String aciklama, long sirketId, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		StringBuffer sp = new StringBuffer("SP_HAREKET_SIL_SIRKET");
		veriMap.put("kgsId", kgsId);
		veriMap.put("pdksId", pdksId);
		veriMap.put("guncelleyenId", guncelleyen.getId());
		veriMap.put("nedenId", nedenId);
		veriMap.put("aciklama", aciklama);
		veriMap.put("sirketId", sirketId);
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		Long sonuc = 0L;
		try {
			List<BigDecimal> list = execSPList(veriMap, sp, null);
			if (!list.isEmpty())
				sonuc = pdksId + kgsId;
		} catch (Exception e) {
			sonuc = 0L;
		}
		return sonuc;
	}

	/**
	 * @param class1
	 * @param em1
	 * @param session
	 * @return
	 * @throws Exception
	 */
	public Long savePrepareTableID(Class class1, EntityManager em1, Session session) throws Exception {
		HashMap fields = new HashMap();
		if (session != null)
			fields.put(MAP_KEY_SESSION, session);
		List list = getObjectByInnerObjectList(fields, class1);
		long kayitAdet = list.size();
		if (kayitAdet > 0) {
			if (kayitAdet > 1)
				list = PdksUtil.sortListByAlanAdi(list, "id", true);
			Long id = (Long) PdksUtil.getMethodObject(list.get(0), "getId", null);
			if (id.longValue() != kayitAdet) {
				kayitAdet = 0;
				list = PdksUtil.sortListByAlanAdi(list, "id", false);
				List saveList = new ArrayList(), removeList = new ArrayList();
				for (Iterator iterator = list.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					kayitAdet++;
					id = (Long) PdksUtil.getMethodObject(object, "getId", null);
					if (id.longValue() != kayitAdet) {
						removeList.add(object);
						BasePDKSObject basePDKSObject = (BasePDKSObject) PdksUtil.getMethodObject(object, "cloneEmpty", null);
						if (basePDKSObject != null)
							saveList.add(basePDKSObject);
					}
				}
				if (!saveList.isEmpty() && removeList.size() == saveList.size()) {
					kayitAdet = saveList.size();
					for (Object object : removeList) {
						if (object != null) {
							id = (Long) PdksUtil.getMethodObject(object, "getId", null);
							if (id != null) {
								deleteObject(session, em1, object);
							} else {
								saveList.clear();
							}
						}

					}
					session.flush();
					session.clear();
					LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
					if (session != null)
						veriMap.put(MAP_KEY_SESSION, session);
					StringBuffer sp = new StringBuffer("SP_CHECKIDENT_VIEW");
					execSP(veriMap, sp);
					session.flush();
					session.clear();
					for (Object object : saveList) {
						if (object != null) {
							id = (Long) PdksUtil.getMethodObject(object, "getId", null);
							if (id == null) {
								save(object, session);
							}
						}

					}
					session.flush();
				}
			} else
				kayitAdet = 0L;
		}
		list = null;
		if (kayitAdet > 0)
			logger.info(kayitAdet + " " + class1.getName() + " düzenlendi.");
		return kayitAdet;
	}

	/**
	 * @param islemId
	 * @param guncelleyen
	 * @return
	 */
	public int hareketOnayla(long islemId, User guncelleyen, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		StringBuffer sp = new StringBuffer("SP_HAREKET_ONAYLA");
		veriMap.put("islemId", islemId);
		veriMap.put("guncelleyen", guncelleyen.getId());
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		int sonuc = 0;
		try {
			sonuc = execSP(veriMap, sp);
		} catch (Exception e) {
			sonuc = 0;
		}
		return sonuc;

	}

	/**
	 * @param kgsId
	 * @param pdksId
	 * @param guncelleyen
	 * @return
	 */
	public int hareketOnaylama(long kgsId, long pdksId, User guncelleyen, Session session) {
		if (session == null)
			session = PdksUtil.getSessionUser(entityManager, authenticatedUser);
		LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
		StringBuffer sp = new StringBuffer("SP_HAREKET_ONAYLAMA");
		veriMap.put("kgsId", kgsId);
		veriMap.put("pdksId", pdksId);
		veriMap.put("guncelleyen", guncelleyen.getId());
		if (session != null)
			veriMap.put(MAP_KEY_SESSION, session);
		int sonuc = 0;
		try {
			sonuc = execSP(veriMap, sp);
		} catch (Exception e) {
			sonuc = 0;
		}
		return sonuc;

	}

	/**
	 * @param sb
	 * @param fields
	 * @param class1
	 * @return
	 */
	public Object getObjectBySQL(StringBuffer sb, HashMap fields, Class class1) {
		List list = getObjectBySQLList(sb, fields, class1);
		Object object = list != null && !list.isEmpty() ? list.get(0) : null;
		return object;
	}

	/**
	 * @param sb
	 * @param fields
	 * @param class1
	 * @param uzerineYaz
	 * @return
	 */
	public TreeMap getObjectBySQLMap(StringBuffer sb, HashMap fields, Class class1, Boolean uzerineYaz) {
		TreeMap treeMap = null;
		if (fields.containsKey(MAP_KEY_MAP)) {
			String method = (String) fields.get(MAP_KEY_MAP);
			fields.remove(MAP_KEY_MAP);
			List list = getObjectBySQLList(sb, fields, class1);
			treeMap = getTreeMapByList(list, method, uzerineYaz);
		} else
			treeMap = new TreeMap();
		return treeMap;
	}

	/**
	 * @param session
	 * @param maxSize
	 * @param sqlSTR
	 * @param key
	 * @param list
	 * @param class1
	 * @return
	 */
	public List getObjectBySQLList(Session session, Integer maxSize, String sqlSTR, String key, Collection list, Class class1) {
		List listAll = new ArrayList();
		List veriler = new ArrayList(list);
		if (maxSize == null || maxSize <= 0)
			maxSize = LIST_MAX_SIZE;
		String parametreKey = ":" + key;
		Connection cn = null;
		Integer ti = null;
		try {
			SessionImplementor si = (SessionImplementor) session;
			cn = si.connection();
			if (ti == null)
				ti = cn.getTransactionIsolation();
			// cn.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		while (!veriler.isEmpty()) {
			LinkedHashMap fieldsOther = new LinkedHashMap();
			StringBuffer strArray = new StringBuffer();
			for (Iterator iterator = veriler.iterator(); iterator.hasNext();) {
				Object data = (Object) iterator.next();
				if (!fieldsOther.isEmpty())
					strArray.append(",");
				String newKey = key + fieldsOther.size();
				strArray.append(":" + newKey);
				fieldsOther.put(newKey, data);
				iterator.remove();
				if (fieldsOther.size() >= maxSize)
					break;
			}
			String parametreler = strArray.toString();
			strArray = null;
			String str = sqlSTR.indexOf(" " + parametreKey) > 0 ? PdksUtil.replaceAll(sqlSTR, " " + parametreKey, parametreKey) : parametreKey;
			String sql = PdksUtil.replaceAll(str, parametreKey, parametreler.indexOf(",") > 0 ? " in ( " + parametreler + " ) " : " = " + parametreler);
			SQLQuery query = session.createSQLQuery(sql);
			if (class1 != null)
				query.addEntity(class1);
			for (Iterator iterator = fieldsOther.keySet().iterator(); iterator.hasNext();) {
				String key1 = (String) iterator.next();
				query.setParameter(key1, fieldsOther.get(key1));
			}

			if (showSQL)
				logger.info(sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));
			try {
				List listNew = query.list();
				if (!listNew.isEmpty())
					listAll.addAll(listNew);
			} catch (Exception e) {
				logger.error("PDKS hata in : \n");
				e.printStackTrace();
				logger.error("PDKS hata out : " + e.getMessage());
				logger.info(sql + " " + e.getMessage());

			}

			if (showSQL)
				logger.info(sql + " out " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));

			fieldsOther = null;
			query = null;

		}

		// try {
		// if (ti != null)
		// cn.setTransactionIsolation(ti);
		// } catch (Exception e) {
		// }

		veriler = null;
		return listAll;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param value
	 * @param class1
	 * @param method
	 * @param uzerineYaz
	 * @param session
	 * @return
	 */
	public TreeMap getSQLParamByFieldMap(String tableName, String fieldName, Object value, Class class1, String method, boolean uzerineYaz, Session session) {
		List list = getSQLParamByFieldList(tableName, fieldName, value, class1, session);
		TreeMap treeMap = getTreeMapByList(list, method, uzerineYaz);
		return treeMap;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param value
	 * @param class1
	 * @param session
	 * @return
	 */
	public List getSQLParamByFieldList(String tableName, String fieldName, Object value, Class class1, Session session) {
		// if (fieldName.equalsIgnoreCase("NAME"))
		// fieldName = "[" + fieldName + "]";
		StringBuffer sb = new StringBuffer();
		List list = null;
		HashMap<String, Object> fields = new HashMap<String, Object>();
		sb.append("select * from " + tableName + " " + selectLOCK);
		Collection collection = null;
		if (value instanceof Collection) {
			collection = (Collection) value;
			if (collection.size() == 1) {
				for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
					Object object = (Object) iterator.next();
					value = object;
				}
			}
		}
		if (value instanceof Collection) {
			String key = "v";
			sb.append(" where " + fieldName + " :" + key);
			fields.put(key, value);
			list = getSQLParamList((List) collection, sb, key, fields, class1, session);
		} else {
			if (PdksUtil.hasStringValue(fieldName)) {
				if (value != null && value instanceof Boolean)
					value = (Boolean) value ? 1 : 0;
				if (fieldName != null) {
					String key = "v";
					sb.append(" where " + fieldName + " = :" + key);
					fields.put(key, value);
				}

				if (session != null)
					fields.put(PdksEntityController.MAP_KEY_SESSION, session);
			}
			list = getObjectBySQLList(sb, fields, class1);

		}
		return list;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param value
	 * @param class1
	 * @param session
	 * @return
	 */
	public Object getSQLParamByAktifFieldObject(String tableName, String fieldName, Object value, Class class1, Session session) {
		List list = getSQLParamByAktifFieldList(tableName, fieldName, value, class1, session);
		Object object = list != null && !list.isEmpty() ? list.get(0) : null;
		list = null;
		return object;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param value
	 * @param class1
	 * @param session
	 * @return
	 */
	public List getSQLParamByAktifFieldList(String tableName, String fieldName, Object value, Class class1, Session session) {
		List list = null;
		try {
			list = getSQLParamByFieldList(tableName, fieldName, value, class1, session);
		} catch (Exception e) {
			logger.error(tableName + " --> " + fieldName + " = " + value + " : " + class1.getName() + "\n" + e.getMessage());
			e.printStackTrace();
		}
		if (list != null)
			PdksUtil.getAktifList(list);

		return list;
	}

	/**
	 * @param sb
	 * @param fields
	 * @param class1
	 * @return
	 */
	public List getObjectBySQLList(StringBuffer sb, HashMap fields, Class class1) {
		List list = null;
		// Integer ti = null;
		// Connection cn = null;
		if (sb != null && sb.length() > 0 && fields != null) {
			Session session = null;
			if (fields.containsKey(MAP_KEY_SESSION)) {
				session = (Session) fields.get(MAP_KEY_SESSION);
				fields.remove(MAP_KEY_SESSION);
			} else if (authenticatedUser != null) {
				session = authenticatedUser.getSessionSQL();
			}
			if (session == null)
				session = PdksUtil.getSession(entityManager, Boolean.FALSE);
			Object tr = null;

			if (fields.containsKey(MAP_KEY_TRANSACTION)) {
				tr = fields.get(MAP_KEY_TRANSACTION);

				fields.remove(MAP_KEY_TRANSACTION);
			}
			String sql = sb.toString();
			SQLQuery queryReadUnCommitted = null;
			if (readUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			TreeMap fieldsOther = new TreeMap();
			sb = null;
			Boolean devam = Boolean.TRUE;
			for (Iterator iterator = fields.keySet().iterator(); iterator.hasNext();) {
				String key = (String) iterator.next();
				String parametreKey = ":" + key;
				if (sql.indexOf(parametreKey) < 0)
					continue;
				Object object = fields.get(key);
				if (object instanceof Collection) {
					if (fields.size() > 1) {
						int i = 0;
						sb = new StringBuffer();
						for (Iterator iter = ((Collection) object).iterator(); iter.hasNext();) {
							Object data = (Object) iter.next();
							String newKey = key + (i++);
							sb.append(":" + newKey);
							if (iter.hasNext())
								sb.append(",");
							fieldsOther.put(newKey, data);
						}
						String parametreler = sb.toString();
						sb = null;
						sql = PdksUtil.replaceAll(sql, parametreKey, parametreler.indexOf(",") > 0 ? " in ( " + parametreler + " ) " : " = " + parametreler);
					} else {
						list = getObjectBySQLList(session, LIST_MAX_SIZE, sql, key, (Collection) object, class1);
						devam = Boolean.FALSE;
					}
				} else
					fieldsOther.put(key, object);
			}
			if (devam) {
				SQLQuery query = session.createSQLQuery(sql);
				if (class1 != null)
					query.addEntity(class1);
				for (Iterator iterator = fieldsOther.keySet().iterator(); iterator.hasNext();) {
					String key = (String) iterator.next();
					query.setParameter(key, fieldsOther.get(key));
				}
				fieldsOther = null;

				if (showSQL)
					logger.info(sql + " in " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));
				try {
					list = query.list();
				} catch (Exception e) {
					logger.error(sql);
					list = null;
				}
				if (queryReadUnCommitted != null) {
					queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
					queryReadUnCommitted.executeUpdate();
				}
				if (showSQL)
					logger.info(sql + " out " + PdksUtil.convertToDateString(new Date(), PdksUtil.getDateFormat() + " H:mm:ss"));

			}

			if (tr != null)
				fields.put(MAP_KEY_TRANSACTION, tr);

			sb = null;
		}
		return list;

	}

	/**
	 * @param sb
	 * @param map
	 * @param tableName
	 * @param class1
	 * @param idColumn
	 * @return
	 */
	public List getDataByIdList(StringBuffer sb, HashMap map, String tableName, Class class1, String idColumn) {
		List list = null;
		Session session = map.containsKey(PdksEntityController.MAP_KEY_SESSION) ? (Session) map.get(PdksEntityController.MAP_KEY_SESSION) : PdksUtil.getSessionUser(entityManager, authenticatedUser);
		List idler = getObjectBySQLList(sb, map, null);
		if (idler != null && !idler.isEmpty()) {
			String fieldName = "id";
			if (idColumn == null)
				idColumn = BaseObject.COLUMN_NAME_ID;
			map.clear();
			sb = null;
			sb = new StringBuffer();
			sb.append("select * from " + tableName + " " + selectLOCK);
			sb.append(" where " + idColumn + " :" + fieldName);
			map.put(fieldName, idler);
			if (session != null)
				map.put(PdksEntityController.MAP_KEY_SESSION, session);
			list = getSQLParamList(idler, sb, fieldName, map, class1, session);

		}
		if (list == null)
			list = new ArrayList();
		return list;
	}

	/**
	 * @param method
	 * @param uzerineYaz
	 * @param dataIdList
	 * @param sb
	 * @param fieldName
	 * @param fieldsOrj
	 * @param class1
	 * @param session
	 * @return
	 */
	public TreeMap getSQLParamTreeMap(String method, boolean uzerineYaz, List dataIdList, StringBuffer sb, String fieldName, HashMap<String, Object> fieldsOrj, Class class1, Session session) {
		TreeMap treeMap = null;
		if (fieldsOrj != null && fieldsOrj.containsKey(PdksEntityController.MAP_KEY_MAP)) {
			if (PdksUtil.hasStringValue(method) == false)
				method = (String) fieldsOrj.get(PdksEntityController.MAP_KEY_MAP);
			fieldsOrj.remove(PdksEntityController.MAP_KEY_MAP);
		}
		List veriList = getSQLParamList(dataIdList, sb, fieldName, fieldsOrj, class1, session);
		if (veriList != null)
			treeMap = getTreeMapByList(veriList, method, uzerineYaz);
		return treeMap;
	}

	/**
	 * @param tableName
	 * @param fieldName
	 * @param value
	 * @param class1
	 * @param session
	 * @return
	 */
	public Object getSQLParamByFieldObject(String tableName, String fieldName, Object value, Class class1, Session session) {
		Object object = null;
		try {
			HashMap parametreMap = new HashMap();
			StringBuffer sb = new StringBuffer();
			sb.append("select TOP 1 * from " + tableName + " " + selectLOCK);
			if (value != null) {
				sb.append(" where " + fieldName + " = :u");
				parametreMap.put("u", value);
			} else
				sb.append(" where " + fieldName + "  is null");

			if (session != null)
				parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
			List list = null;
			try {
				list = getObjectBySQLList(sb, parametreMap, class1);
			} catch (Exception e) {

			}
			object = list != null && !list.isEmpty() ? list.get(0) : null;
			if (object == null && class1 != null) {
				StringBuffer sb1 = new StringBuffer();
				if (authenticatedUser != null && PdksUtil.hasStringValue(authenticatedUser.getCalistigiSayfa())) {
					sb1.append(authenticatedUser.getCalistigiSayfa() + "\n");
				}
				sb1.append(sb.toString() + " --> " + value + " " + class1.getName());
				if (value != null)
					logger.debug(sb1.toString());
				sb1 = null;
			}
			sb = null;
			list = null;
			parametreMap = null;

		} catch (Exception e) {
			logger.error(tableName + " --> " + fieldName + " : " + value + " " + class1.getName() + "\n" + e.getMessage());
		}

		return object;
	}

	/**
	 * @param dataIdList
	 * @param sb
	 * @param fieldName
	 * @param fieldsOrj
	 * @param class1
	 * @param session
	 * @return
	 */
	public List getSQLParamList(List dataIdList, StringBuffer sb, String fieldName, HashMap<String, Object> fieldsOrj, Class class1, Session session) {
		List idList = new ArrayList();
		List veriList = new ArrayList();
		try {
			if (session == null && fieldsOrj != null && fieldsOrj.containsKey(PdksEntityController.MAP_KEY_SESSION))
				session = (Session) fieldsOrj.get(PdksEntityController.MAP_KEY_SESSION);
			int size = PdksEntityController.LIST_MAX_SIZE - fieldsOrj.size();
			List idInputList = new ArrayList(dataIdList);
			String str = ":" + fieldName, sqlStr = sb.toString();
			if (dataIdList.size() == 1) {
				fieldsOrj.put(fieldName, dataIdList);
				veriList = getObjectBySQLList(sb, fieldsOrj, class1);
			} else {
				int adet = 0;
				for (String key : fieldsOrj.keySet()) {
					if (!key.equals(PdksEntityController.MAP_KEY_SESSION) && !key.equals(fieldName)) {
						Object object = fieldsOrj.get(key);
						if (object instanceof Collection)
							adet += ((Collection) object).size();
						else
							++adet;
					}
				}
				while (!idInputList.isEmpty()) {
					for (Iterator iterator = idInputList.iterator(); iterator.hasNext();) {
						Object long1 = (Object) iterator.next();
						idList.add(long1);
						iterator.remove();
						if (idList.size() + adet >= size)
							break;
					}
					HashMap<String, Object> fields = new HashMap<String, Object>();
					fields.putAll(fieldsOrj);
					if (fields.containsKey(PdksEntityController.MAP_KEY_SESSION))
						fields.remove(PdksEntityController.MAP_KEY_SESSION);
					if (idList.size() > 1 || sqlStr.indexOf(str) < 1)
						fields.put(fieldName, idList);
					else {
						sb = new StringBuffer(PdksUtil.replaceAll(sqlStr, str, " = :" + fieldName));
						fields.put(fieldName, idList.get(0));
					}
					if (session != null)
						fields.put(PdksEntityController.MAP_KEY_SESSION, session);
					try {
						List list = getObjectBySQLList(sb, fields, class1);
						if (!list.isEmpty())
							veriList.addAll(list);
						list = null;
					} catch (Exception e) {
						logger.error(sb + "\n" + e);
						idInputList.clear();
					}
					idList.clear();
					fields = null;

				}
			}
			idInputList = null;
		} catch (Exception ex) {
			logger.error(sb + "\n" + ex);

		}

		return veriList;
	}

	public static boolean isShowSQL() {
		return showSQL;
	}

	public static void setShowSQL(boolean showSQL) {
		PdksEntityController.showSQL = showSQL;
	}

	public static String getSelectLOCK() {
		return selectLOCK;
	}

	public static void setSelectLOCK(String selectLOCK) {
		PdksEntityController.selectLOCK = selectLOCK;
	}

	public static String getJoinLOCK() {
		return joinLOCK;
	}

	public static void setJoinLOCK(String joinLOCK) {
		PdksEntityController.joinLOCK = joinLOCK;
	}

	public static boolean isReadUnCommitted() {
		return readUnCommitted;
	}

	public static void setReadUnCommitted(boolean readUnCommitted) {
		PdksEntityController.readUnCommitted = readUnCommitted;
	}

}
