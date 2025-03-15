package org.pdks.security.entity;

import java.io.Serializable;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.pdks.entity.BasePDKSObject;

@Entity(name = UserRoles.TABLE_NAME)
public class UserRoles extends BasePDKSObject implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1425317555516469129L;
	public static final String TABLE_NAME = "USERROLES";

	public static final String COLUMN_NAME_USER = "USERID";
	public static final String COLUMN_NAME_ROLE = "ROLEID";

	private User user;

	private Role role;

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_USER, nullable = false)
	@Fetch(FetchMode.JOIN)
	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	@ManyToOne(cascade = CascadeType.REFRESH)
	@JoinColumn(name = COLUMN_NAME_ROLE, nullable = false)
	@Fetch(FetchMode.JOIN)
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}
}
