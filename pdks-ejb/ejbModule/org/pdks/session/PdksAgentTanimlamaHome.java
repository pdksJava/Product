package org.pdks.session;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.jboss.seam.annotations.Begin;
import org.jboss.seam.annotations.FlushModeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.web.RequestParameter;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.framework.EntityHome;
import org.pdks.entity.PdksAgent;
import org.pdks.quartz.ThreadAgent;
import org.pdks.security.entity.User;

@Name("pdksAgentTanimlamaHome")
public class PdksAgentTanimlamaHome extends EntityHome<PdksAgent> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2910343234370602034L;

	static Logger logger = Logger.getLogger(PdksAgentTanimlamaHome.class);

	@RequestParameter
	Long parameterId;
	@In(create = true)
	PdksEntityController pdksEntityController;
	@In(required = false)
	User authenticatedUser;
	@In(required = false, create = true)
	EntityManager entityManager;

	@In(required = true, create = true)
	Renderer renderer;
	@In(required = false, create = true)
	OrtakIslemler ortakIslemler;

	public static String sayfaURL = "pdksAgentTanimlama";
	private PdksAgent currentAgent;
	private List<PdksAgent> pdksAgentList;

	private Session session;
	private Boolean helpDesk, pasifGoster, admin;

	@Override
	public Object getId() {
		if (parameterId == null) {
			return super.getId();
		} else {
			return parameterId;
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
		admin = authenticatedUser.isAdmin();
		fillPdksAgentList();
	}

	@Transactional
	public String deleteAgent() {
		PdksAgent agent = getInstance();
		pdksEntityController.deleteObject(session, entityManager, agent);
		session.flush();
		try {
			pdksEntityController.savePrepareTableID(true, PdksAgent.class, entityManager, session);
		} catch (Exception e) {
		}
		session.flush();
		session.clear();

		fillPdksAgentList();

		return "";
	}

	public String agentRun(PdksAgent agent) {
		if (agent.getStart().booleanValue() == false) {
			ThreadAgent threadAgent = new ThreadAgent(agent, pdksEntityController, session);
			threadAgent.start();
			PdksUtil.addMessageAvailableInfo(agent.getAciklama() + " çalışmaya başladı.");
		} else
			PdksUtil.addMessageAvailableWarn(agent.getAciklama() + " çalışıyor!");

		return "";
	}

	public String guncelle(PdksAgent agent) {
		if (agent == null)
			agent = new PdksAgent();

		currentAgent = agent;
		setInstance(agent);
		return "";
	}

	@Transactional
	public String kaydet() {
		PdksAgent agent = getInstance();
		pdksEntityController.saveOrUpdate(session, entityManager, agent);
		session.flush();
		session.clear();
		fillPdksAgentList();
		return "persisted";

	}

	public void instanceRefresh() {
		if (currentAgent.getId() != null)
			session.refresh(currentAgent);
	}

	public String fillPdksAgentList() {

		HashMap parametreMap = new HashMap();
		if (session != null)
			parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);

		helpDesk = false;
		StringBuffer sb = new StringBuffer();
		sb.append("select T.* from " + PdksAgent.TABLE_NAME + " T " + PdksEntityController.getSelectLOCK() + " ");
		if (session != null)
			parametreMap.put(PdksEntityController.MAP_KEY_SESSION, session);
		List<PdksAgent> list = pdksEntityController.getObjectBySQLList(sb, parametreMap, PdksAgent.class);

		list = PdksUtil.sortListByAlanAdi(list, "id", admin);

		List<PdksAgent> pasifList = new ArrayList<PdksAgent>();
		try {
			for (Iterator iterator = list.iterator(); iterator.hasNext();) {
				PdksAgent parameter = (PdksAgent) iterator.next();
				if (parameter.getDurum().equals(Boolean.FALSE)) {
					pasifList.add(parameter);
					iterator.remove();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (!pasifList.isEmpty())
			list.addAll(pasifList);

		pasifList = null;

		setPdksAgentList(list);
		return "";
	}

	public void refreshInstance() {
		if (getInstance().getId() != null)
			session.refresh(getInstance());
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Boolean getHelpDesk() {
		return helpDesk;
	}

	public void setHelpDesk(Boolean helpDesk) {
		this.helpDesk = helpDesk;
	}

	public Boolean getPasifGoster() {
		return pasifGoster;
	}

	public void setPasifGoster(Boolean pasifGoster) {
		this.pasifGoster = pasifGoster;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
	}

	public static String getSayfaURL() {
		return sayfaURL;
	}

	public static void setSayfaURL(String sayfaURL) {
		PdksAgentTanimlamaHome.sayfaURL = sayfaURL;
	}

	public List<PdksAgent> getPdksAgentList() {
		return pdksAgentList;
	}

	public void setPdksAgentList(List<PdksAgent> pdksAgentList) {
		this.pdksAgentList = pdksAgentList;
	}

	public PdksAgent getCurrentAgent() {
		return currentAgent;
	}

	public void setCurrentAgent(PdksAgent currentAgent) {
		this.currentAgent = currentAgent;
	}

}
