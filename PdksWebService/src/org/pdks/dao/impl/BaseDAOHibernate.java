/*
 * Created on 28.Ara.2006
 *
 * 
 * Window - Preferences - Java - Code Style - Code Templates
 */
package org.pdks.dao.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.pdks.dao.BaseDAO;
import org.pdks.entity.BasePDKSObject;
import org.pdks.genel.model.Liste;
import org.pdks.genel.model.PdksUtil;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * @author Hasan Sayar
 * 
 * 
 *         Window - Preferences - Java - Code Style - Code Templates
 */
public class BaseDAOHibernate extends HibernateDaoSupport implements BaseDAO {

	public static final int LIST_MAX_SIZE = 512;
	public static final String MAP_KEY_MAP = "Map";
	public static final String MAP_KEY_OR = "or";
	public static final String MAP_KEY_ORDER = "order";
	public static final String MAP_KEY_SELECT = "select";
	public static final String MAP_KEY_SQLADD = "sql_add";
	public static final String MAP_KEY_SQLPARAMS = "sql_params";
	public static final String MAP_KEY_SP_NAME = "SP_NAME";
	public static final String MAP_KEY_SESSION = "session";
	public static final String SELECT_KARAKTER = "t";
	public static final String where = " where ";
	public static final String TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED = "read uncommitted";
	public static final String TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED = "read committed";
	private static boolean readUnCommitted = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#getObjectNumericId(String, java.lang.Class)
	 */
	public Object getObjectNumericId(String id, Class class1) {
		Object ob = null;
		if (class1 != null) {
			String query = "from " + class1.getName() + " " + SELECT_KARAKTER + " where " + SELECT_KARAKTER + ".id=" + id;
			List list = getHibernateTemplate().find(query);
			ob = list.isEmpty() ? null : list.get(0);
		}
		return ob;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#getObjectByInnerObjectList(java.lang.String, java.lang.Object, java.lang.Class)
	 */
	public List getObjectByInnerObjectList(String fieldName, Object fieldValue, Class class1) {
		List list = null;
		if (class1 != null) {
			HashMap map = new HashMap();
			map.put(fieldName, fieldValue);
			list = getObjectByInnerObjectList(map, class1);
		}
		return list;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#getObjectByInnerObject(java.util.HashMap, java.lang.Class)
	 */
	public Object getObjectByInnerObject(HashMap fields, Class class1) {
		List list = getObjectByInnerObjectList(fields, class1);
		return (list != null && !list.isEmpty() ? list.get(0) : null);

	}

	public List getObjectByInnerObjectListInLogic(HashMap fields, Class class1) {
		return getObjectByInnerObjectList(fields, class1, "");
	}

	public List getObjectByInnerObjectList(HashMap fields, Class class1) {
		return getObjectByInnerObjectList(fields, class1, "=");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObject(String, String, String, java.lang.Class)
	 */
	public Object getObjectByInnerObject(String fieldName, Object fieldValue, Class class1) {
		List list = getObjectByInnerObjectList(fieldName, fieldValue, class1);
		return (list != null && !list.isEmpty() ? list.get(0) : null);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#saveObject(java.lang.Object)
	 */
	public void saveObject(Object object) {
		if (object != null) {
			getHibernateTemplate().saveOrUpdate(object);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectList(java.util.List, java.lang.Class)
	 */
	public List getObjectInnerNameList(String fieldName, List list, Class class1) {
		List liste = new ArrayList();
		String str = "";
		while (!list.isEmpty()) {
			int uz = list.size() > LIST_MAX_SIZE ? LIST_MAX_SIZE : list.size();
			for (int j = 0; j < uz; j++) {
				String id = list.get(0).toString();
				str += (PdksUtil.hasStringValue(str) ? "," : "") + id;
				list.remove(0);
			}
			String query = tesisKoduKontrol(class1, "from " + class1.getName() + " " + SELECT_KARAKTER + " where " + SELECT_KARAKTER + "." + fieldName + " " + (str.indexOf(",") > 0 ? "in (" + str + ")" : "=" + str), null, null);
			List list1 = getHibernateTemplate().find(query);
			if (!list1.isEmpty())
				liste.addAll(list1);
			str = "";
		}
		return liste;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectList(java.util.List, java.lang.Class)
	 */
	public List getObjectList(List list, Class class1) {
		List liste = getObjectInnerNameList("id", list, class1);
		return liste;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#deleteObject(java.lang.Object)
	 */
	public void deleteObject(Object object) {
		if (object != null) {
			getHibernateTemplate().delete(object);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectStringId(String, java.lang.Class)
	 */
	public Object getObjectStringId(String id, Class class1) {
		return getObjectByInnerObject("id", id, class1);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#deleteObjectList(java.util.List)
	 */
	public void deleteObjectList(List objectList) {
		if (objectList != null && !objectList.isEmpty()) {
			ArrayList liste = new ArrayList();
			while (!objectList.isEmpty()) {
				int uz = objectList.size() > LIST_MAX_SIZE ? LIST_MAX_SIZE : objectList.size();
				liste.clear();
				for (int j = 0; j < uz; j++) {
					liste.add(objectList.get(0));
					objectList.remove(0);
				}
				getHibernateTemplate().deleteAll(liste);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectListStringId(String, java.lang.Class)
	 */
	public List getObjectList(Class class1) {
		// String query = "from " + class1.getName() + " " + SELECT_KARAKTER;
		HashMap fields = new HashMap();
		List list = getObjectByInnerObjectList(fields, class1);
		fields = null;
		return list;
	}

	public void saveObjectParcaliList(Collection objectList) {
		if (objectList != null && !objectList.isEmpty()) {
			for (Iterator iterator = objectList.iterator(); iterator.hasNext();) {
				Object entity = (Object) iterator.next();
				if (entity instanceof Collection) {
					saveObjectParcaliList((Collection) entity);
				} else
					try {
						getHibernateTemplate().saveOrUpdate(entity);
					} catch (Exception e) {
						logger.error(e);
					}

			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#saveObjectList(java.util.List)
	 */
	public void saveObjectList(List objectList) {
		if (objectList != null && !objectList.isEmpty()) {
			ArrayList liste = new ArrayList();
			while (!objectList.isEmpty()) {
				int uz = objectList.size() > LIST_MAX_SIZE ? LIST_MAX_SIZE : objectList.size();
				liste.clear();
				for (int j = 0; j < uz; j++) {
					liste.add(objectList.get(0));
					objectList.remove(0);
				}
				getHibernateTemplate().saveOrUpdateAll(liste);
			}
		}

	}

	public TreeMap getTreeMapByList(List list, String method, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		for (Iterator iter = list.iterator(); iter.hasNext();) {
			Object object = iter.next();
			try {
				Object key = PdksUtil.getMethodObject(object, method, null);
				if (key != null)
					if (uzerineYaz || !treeMap.containsKey(key))
						treeMap.put(key, object);
			} catch (Exception e) {
				logger.error("Medula Hata in : ");
				e.printStackTrace();
				logger.error("Medula Hata out : " + e.getMessage());

			}
		}
		return treeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#getObjectByInnerObjectMapInLogic(java.util.HashMap, java.lang.Class)
	 */
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#getObjectByInnerObjectMap(java.util.HashMap, java.lang.Class)
	 */
	public TreeMap getObjectByInnerObjectMap(HashMap map, Class class1, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		if (map.containsKey(MAP_KEY_MAP)) {
			String method = (String) map.get(MAP_KEY_MAP);
			if (PdksUtil.hasStringValue(method)) {
				map.remove(MAP_KEY_MAP);
				List list = getObjectByInnerObjectList(map, class1);
				treeMap = getTreeMapByList(list, method, uzerineYaz);
			}

		}
		return treeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectMap(java.lang.Class, boolean)
	 */
	public TreeMap getObjectMap(String method, Class class1, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		List list = getObjectList(class1);
		treeMap = getTreeMapByList(list, method, uzerineYaz);

		return treeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#getObjectByInnerObjectMap(String, java.lang.Object, java.lang.Class)
	 */
	public TreeMap getObjectByInnerObjectMap(String method, String fieldName, Object fieldValue, Class class1, boolean uzerineYaz) {
		TreeMap treeMap = new TreeMap();
		List list = getObjectByInnerObjectList(fieldName, fieldValue, class1);
		treeMap = getTreeMapByList(list, method, uzerineYaz);
		return treeMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#saveObjectAndList(java.lang.Object, java.util.List, java.util.List)
	 */
	public void saveObjectAndList(Object object, List saveList, List deleteList) {
		if (deleteList != null)
			deleteObjectList(deleteList);
		if (object != null)
			saveObject(object);
		if (saveList != null)
			saveObjectList(saveList);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#saveObjectAndList(java.lang.Object, java.util.List)
	 */
	public void saveObjectAndList(Object object, List saveDetayObjectList) {
		saveObjectAndList(object, saveDetayObjectList, null);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.abh.eig.dao.BaseDAO#saveAndDeleteObjectList(java.util.List, java.util.List)
	 */
	public void saveAndDeleteObjectList(List saveList, List deleteList) {
		saveObjectAndList(null, saveList, deleteList);

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.pdks.dao.BaseDAO#deleteAndSaveObject(java.lang.Object[], java.lang.Object[])
	 */
	public void deleteAndSaveObject(Object[] saveObjectArray, Object[] deleteObjectArray) {
		if (deleteObjectArray != null) {
			for (int i = 0; i < deleteObjectArray.length; i++) {
				if (!(deleteObjectArray[i] instanceof Collection))
					deleteObject(deleteObjectArray[i]);
				else
					deleteObjectList((List) deleteObjectArray[i]);
			}
		}
		if (saveObjectArray != null) {
			for (int i = 0; i < saveObjectArray.length; i++) {
				if (!(saveObjectArray[i] instanceof Collection))
					saveObject(saveObjectArray[i]);
				else
					saveObjectList((List) saveObjectArray[i]);
			}

		}
	}

	public String getStringParse(Object fieldValue) {
		String str = "";
		if (fieldValue instanceof Collection) {
			for (Iterator iter = ((Collection) fieldValue).iterator(); iter.hasNext();) {
				String element = (String) iter.next();
				str += " " + SELECT_KARAKTER + "." + element + (iter.hasNext() ? "," : "");
			}
		} else if (fieldValue instanceof String) {
			StringTokenizer st = new StringTokenizer((String) fieldValue, ",");
			while (st.hasMoreTokens())
				str += " " + SELECT_KARAKTER + "." + st.nextToken() + (st.hasMoreTokens() ? "," : "");
		}
		return str;
	}

	private void getObjectByInnerObjectListParcala(int indis, List listKey, HashMap fieldsOrj, List toplamList, Class class1, String esit) {
		String fieldName = (String) ((Liste) listKey.get(indis)).getId();
		ArrayList kriterList = new ArrayList((Collection) fieldsOrj.get(fieldName));
		Double maxAdetR = new Double(LIST_MAX_SIZE);
		for (int i = 0; i <= indis; i++)
			maxAdetR = new Double(maxAdetR.doubleValue() / 2);
		int maxAdet = maxAdetR.intValue();
		HashMap map = new HashMap();
		map.putAll(fieldsOrj);
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
			if (indis != listKey.size() - 1)
				getObjectByInnerObjectListParcala(indis + 1, listKey, map, toplamList, class1, esit);
			else {
				List list = getObjectByInnerObjectListGetir(map, class1, esit);
				if (!list.isEmpty())
					toplamList.addAll(list);
				list = null;
			}
			yeniListe = null;
		}
		map = null;
	}

	protected List getObjectByInnerObjectList(HashMap fieldsOrj, Class class1, String esit) {
		List objectList = null;
		List listKey = null;
		HashMap ozelMap = new HashMap();
		ozelMap.put(MAP_KEY_SQLADD, "");
		ozelMap.put(MAP_KEY_SELECT, "");
		ozelMap.put(MAP_KEY_ORDER, "");
		ozelMap.put(MAP_KEY_OR, "");
		int parametreAdet = 0;
		for (Iterator iterator = fieldsOrj.keySet().iterator(); iterator.hasNext();) {
			String fieldName = (String) iterator.next();
			Object fieldValue = fieldsOrj.get(fieldName);
			String mapFields = fieldName.toLowerCase();
			if (ozelMap.containsKey(mapFields))
				continue;
			if (fieldValue != null) {
				if (fieldValue instanceof Collection) {
					ArrayList liste = new ArrayList((Collection) fieldValue);
					if (!fieldName.equals(MAP_KEY_SQLPARAMS)) {
						if (listKey == null)
							listKey = new ArrayList();
						Liste listeKey = new Liste(fieldName, new Integer(liste.size()));
						listKey.add(listeKey);
					}
					parametreAdet += liste.size();
				} else
					++parametreAdet;
			}
		}
		ozelMap = null;
		if (listKey != null) {
			if (parametreAdet > LIST_MAX_SIZE) {
				if (listKey.size() > 1)
					listKey = PdksUtil.sortListByAlanAdi((ArrayList) listKey, "value", true);
				objectList = new ArrayList();
				getObjectByInnerObjectListParcala(0, listKey, fieldsOrj, objectList, class1, esit);
			}
			listKey = null;
		}
		if (objectList == null)
			objectList = getObjectByInnerObjectListGetir(fieldsOrj, class1, esit);

		return objectList;

	}

	/**
	 * @param fields
	 * @param class1
	 * @param esit
	 * @return
	 */
	private List getObjectByInnerObjectListGetir(HashMap fields, Class class1, String esit) {
		if (esit == null)
			esit = "";
		Object fieldValue;
		List list = null;
		SQLQuery queryReadUnCommitted = null;
		Session session = null;
		if (readUnCommitted) {
			session = getHibernateCurrentSession();
			queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
			queryReadUnCommitted.executeUpdate();
		}
		if (class1 != null) {
			ArrayList parametreList = new ArrayList();
			String query = "";
			// String order = "";
			String select = "";
			String or = null;

			if (!fields.isEmpty()) {
				for (Iterator iter = fields.keySet().iterator(); iter.hasNext();) {
					String fieldName = (String) iter.next();
					fieldValue = fields.get(fieldName);
					// if (fieldValue==null) continue;
					String str = esit + " null";
					if (fieldName.toLowerCase().trim().equals(MAP_KEY_SQLPARAMS)) {
						continue;
					} else if (fieldName.toLowerCase().trim().equals(MAP_KEY_SQLADD)) {
						if (fields.containsKey(MAP_KEY_SQLPARAMS)) {
							Object object = fields.get(MAP_KEY_SQLPARAMS);
							if (object instanceof Collection)
								parametreList.addAll((Collection) object);
							else if (object instanceof Object[]) {
								Object[] dizi = (Object[]) object;
								for (int i = 0; i < dizi.length; i++)
									parametreList.add(dizi[i]);
							} else
								parametreList.add(object);
						}

						query += (PdksUtil.hasStringValue(query) ? " and " : " where ") + "(" + fieldValue + ")";
						continue;
					} else if (fieldName.toLowerCase().trim().equals(MAP_KEY_OR))
						or = " or ";
					else if (fieldName.toLowerCase().trim().equals(MAP_KEY_SELECT)) {
						fieldValue = fields.get(fieldName);
						select = "select " + getStringParse(fieldValue);
					} else if (fieldName.toLowerCase().trim().equals(MAP_KEY_ORDER)) {
						fieldValue = fields.get(fieldName);
						// order = " order by " + getStringParse(fieldValue);
					} else {
						if (fieldValue != null) {
							str = esit + "?";
							if (fieldValue instanceof Collection) {
								if (((Collection) fieldValue).isEmpty()) {
									query += (PdksUtil.hasStringValue(query) ? " and" : " where") + " 1=2";
									continue;
								}
								StringBuffer sb = new StringBuffer();
								for (Iterator iter1 = ((List) fieldValue).iterator(); iter1.hasNext();) {
									Object element = (Object) iter1.next();
									if (element != null) {
										if (element instanceof String) {
											sb.append((sb.length() > 0 ? "," : "") + "?");
											parametreList.add(element);
										} else
											sb.append((sb.length() > 0 ? "," : "") + element.toString());
									}

								}
								String str1 = sb.toString();
								str = str1.indexOf(",") > 0 ? "in (" + str1 + ")" : "= " + str1;
								sb = null;
								str1 = null;

							} else
								parametreList.add(fieldValue);
						} else
							str = esit + "null";
						query += (PdksUtil.hasStringValue(query) ? " and " : " where ") + SELECT_KARAKTER + "." + fieldName + " " + str;
						str = null;
					}
				}
			}
			query = tesisKoduKontrol(class1, select.trim() + " from " + class1.getName() + " " + SELECT_KARAKTER + " " + query.trim(), or, parametreList);
			if (query.indexOf(" and ") > 0 && or != null)
				query = PdksUtil.replaceAll(query, " and ", or);
			if (parametreList.isEmpty()) {
				list = getHibernateTemplate().find(query);
			} else {

				if (parametreList.size() == 1) {
					list = getHibernateTemplate().find(query, parametreList.get(0));
				} else {
					Object[] objectValue = new Object[parametreList.size()];
					for (int i = 0; i < objectValue.length; i++)
						objectValue[i] = parametreList.get(i);
					list = getHibernateTemplate().find(query, objectValue);
				}

			}
		}
		if (queryReadUnCommitted != null) {
			queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
			queryReadUnCommitted.executeUpdate();
		}
		return list;
	}

	/**
	 * @return
	 */
	public Session openSession() {
		Session session = getHibernateTemplate().getSessionFactory().openSession();
		return session;
	}

	/**
	 * @return
	 */
	public Session getHibernateCurrentSession() {
		Session session = getHibernateTemplate().getSessionFactory().getCurrentSession();
		return session;
	}

	/**
	 * @param fields
	 * @param sb
	 * @param class1
	 * @return
	 */
	public List getNativeSQLList(HashMap fields, StringBuffer sb, Class class1) {
		List list = null;
		Session session = getHibernateCurrentSession();
		SQLQuery queryReadUnCommitted = null;
		if (readUnCommitted) {
			queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
			queryReadUnCommitted.executeUpdate();
		}
		String sql = sb.toString();
		TreeMap fieldsOther = new TreeMap();
		sb = null;
		boolean devam = true;
		for (Iterator iterator = fields.keySet().iterator(); iterator.hasNext();) {
			String key = (String) iterator.next();
			String parametreKey = ":" + key;
			if (sql.indexOf(parametreKey) < 0)
				continue;
			Object object = fields.get(key);
			if (object instanceof Collection) {
				Collection collection = (Collection) object;
				if (fields.size() > 1) {
					int i = 0;
					sb = new StringBuffer();
					for (Iterator iter = collection.iterator(); iter.hasNext();) {
						Object data = (Object) iter.next();
						String newKey = key + (i++);
						sb.append(":" + newKey);
						if (iter.hasNext())
							sb.append(",");
						fieldsOther.put(newKey, data);
					}
					String parametreler = sb.toString();
					sb = null;
					sql = PdksUtil.replaceAll(sql, parametreKey, parametreler.indexOf(",") > 0 ? " IN ( " + parametreler + " ) " : " = " + parametreler);
				} else {
					list = getObjectBySQLList(session, LIST_MAX_SIZE, sql, key, (Collection) object, class1);
					devam = false;
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
			try {
				list = query.list();
			} catch (Exception e) {
				logger.error(sql + "\n" + e);
				list = null;
			}

			fieldsOther = null;
		}
		if (queryReadUnCommitted != null) {
			queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
			queryReadUnCommitted.executeUpdate();
		}
		// session.close();
		return list;
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
	private List getObjectBySQLList(Session session, int maxSize, String sqlSTR, String key, Collection list, Class class1) {
		List listAll = new ArrayList();
		List veriler = new ArrayList(list);
		if (maxSize <= 0)
			maxSize = 900;
		// maxSize = maxSize / 2;
		String parametreKey = ":" + key;
		SQLQuery queryReadUnCommitted = null;
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
			String sql = PdksUtil.replaceAll(sqlSTR, parametreKey, parametreler.indexOf(",") > 0 ? " IN ( " + parametreler + " ) " : " = " + parametreler);
			session.clear();
			if (readUnCommitted) {
				queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_UNCOMMITTED));
				queryReadUnCommitted.executeUpdate();
			}
			SQLQuery query = session.createSQLQuery(sql);
			if (class1 != null)
				query.addEntity(class1);
			for (Iterator iterator = fieldsOther.keySet().iterator(); iterator.hasNext();) {
				String key1 = (String) iterator.next();
				query.setParameter(key1, fieldsOther.get(key1));
			}

			try {
				List listNew = query.list();
				logger.debug(veriler.size() + " " + fieldsOther.size() + " : " + listNew.size());
				if (!listNew.isEmpty())
					listAll.addAll(listNew);
			} catch (Exception e) {

				e.printStackTrace();

				logger.info(sql + " " + e.getMessage());

			}

			fieldsOther = null;
			query = null;

		}
		if (queryReadUnCommitted != null) {
			queryReadUnCommitted = session.createSQLQuery(setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL_READ_COMMITTED));
			queryReadUnCommitted.executeUpdate();
		}
		veriler = null;
		return listAll;
	}

	public List execSPList(LinkedHashMap<String, Object> veriMap, Class class1) throws Exception {
		List liste = null;
		if (veriMap.containsKey(BaseDAOHibernate.MAP_KEY_SELECT)) {

			SQLQuery query = prepareProcedure(veriMap);
			if (query != null) {
				if (class1 != null)
					query.addEntity(class1);
				liste = query.list();
			}
		}

		return liste;
	}

	/**
	 * @param fields
	 * @return
	 */
	public void execSP(LinkedHashMap<String, Object> veriMap) {

		if (veriMap.containsKey(BaseDAOHibernate.MAP_KEY_SELECT)) {
			SQLQuery query = prepareProcedure(veriMap);
			if (query != null)
				query.executeUpdate();
		}

	}

	/**
	 * @param veriMap
	 * @param sp
	 * @return
	 */
	private SQLQuery prepareProcedure(LinkedHashMap<String, Object> veriMap) {
		String sp = (String) veriMap.get(BaseDAOHibernate.MAP_KEY_SELECT);
		veriMap.remove(BaseDAOHibernate.MAP_KEY_SELECT);
		String queryStr = "exec " + sp;
		Session session = getHibernateCurrentSession();
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
	 * @param single
	 * @param class1
	 * @return
	 * @throws Exception
	 */
	public Long savePrepareTableID(boolean single, Class class1) throws Exception {
		HashMap fields = new HashMap();
		Session session = getHibernateCurrentSession();
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
								deleteObject(object);
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

					veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_CHECKIDENT_VIEW");
					execSP(veriMap);
					session.flush();
					session.clear();
					for (Object object : saveList) {
						if (object != null) {
							id = (Long) PdksUtil.getMethodObject(object, "getId", null);
							if (id == null) {
								saveObject(object);
							}
						}

					}
					session.flush();
				}
			} else {
				if (single) {
					LinkedHashMap<String, Object> veriMap = new LinkedHashMap<String, Object>();
					if (session != null)
						veriMap.put(MAP_KEY_SESSION, session);
					veriMap.put(BaseDAOHibernate.MAP_KEY_SELECT, "SP_CHECKIDENT_VIEW");
					session.flush();
				}

				kayitAdet = 0L;
			}

		}
		list = null;
		if (kayitAdet > 0)
			logger.info(kayitAdet + " " + class1.getName() + " düzenlendi.");
		return kayitAdet;
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
	 * @param class1
	 * @param query
	 * @param or
	 * @return
	 */
	private String tesisKoduKontrol(Class class1, String query, String or, List list) {
		if (query.indexOf(" and ") > 0 && or != null)
			query = PdksUtil.replaceAll(query, " and ", or);
		return query;
	}

	public static boolean isReadUnCommitted() {
		return readUnCommitted;
	}

	public static void setReadUnCommitted(boolean readUnCommitted) {
		BaseDAOHibernate.readUnCommitted = readUnCommitted;
	}

}
