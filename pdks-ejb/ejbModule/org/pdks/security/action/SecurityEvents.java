package org.pdks.security.action;

import java.util.HashMap;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.international.StatusMessages;
import org.jboss.seam.security.FacesSecurityEvents;
import org.jboss.seam.security.Identity;
import org.pdks.entity.Liste;
import org.pdks.security.entity.User;
import org.pdks.session.PdksUtil;

@Name("securityEvents")
public class SecurityEvents extends FacesSecurityEvents {
 	@In(required = false)
	User authenticatedUser;

	@Override
	@Observer(Identity.EVENT_LOGIN_SUCCESSFUL)
	public void addLoginSuccessfulMessage() {
		String key = "org.jboss.seam.loginSuccessful";
		StatusMessages.instance().clearGlobalMessages();
		StatusMessages.instance().addFromResourceBundleOrDefault(Severity.INFO, key, PdksUtil.getMessageBundleMessage(key), authenticatedUser.getAdSoyad());
	}

	@Override
	@Observer(Identity.EVENT_NOT_LOGGED_IN)
	public void addNotLoggedInMessage() {
		String key = "org.jboss.seam.NotLoggedIn";
		StatusMessages.instance().clearGlobalMessages();
		StatusMessages.instance().addFromResourceBundleOrDefault(Severity.WARN, PdksUtil.getMessageBundleMessage(key), "");
	}

	@Override
	@Observer(Identity.EVENT_ALREADY_LOGGED_IN)
	public void addAlreadyLoggedInMessage() {
		String key = "org.jboss.seam.AlreadyLoggedIn";
		StatusMessages.instance().clearGlobalMessages();
		StatusMessages.instance().addFromResourceBundleOrDefault(Severity.WARN, key, PdksUtil.getMessageBundleMessage(key), "");
	}

	@Override
	@Observer(Identity.EVENT_LOGIN_FAILED)
	public void addLoginFailedMessage(LoginException ex) {
		boolean mesajYaz = true;
		StatusMessages.instance().clearGlobalMessages();
		if (authenticatedUser != null) {
			HashMap<String, List> selectItemMap = authenticatedUser.getSelectItemMap();
			List<Liste> mesajList = selectItemMap != null && selectItemMap.containsKey("hataMesajList") ? (List<Liste>) selectItemMap.get("hataMesajList") : null;
			if (mesajList != null && mesajList.size() == 1) {
				Liste liste = mesajList.get(0);
				mesajYaz = false;
				PdksUtil.addMessage((String) liste.getValue(), (Severity) liste.getId(), false);
			}
		}
		if (mesajYaz) {
			String key = "org.jboss.seam.loginFailed";
			StatusMessages.instance().addFromResourceBundleOrDefault(Severity.ERROR, key, PdksUtil.getMessageBundleMessage(key), "");
		}
	}
}
