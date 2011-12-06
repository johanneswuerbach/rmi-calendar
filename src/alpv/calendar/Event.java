package alpv.calendar;

import java.io.Serializable;
import java.util.Date;

public final class Event implements Serializable {
	
	private static final long serialVersionUID = 3084014581109727872L;

	public Event(String name, String[] user, Date begin) {
		super();
		this.name = name;
		this.user = user;
		this.begin = begin;
	}

	private long id;
	
	private String name;
	
	private String[] user;
	
	private Date begin;
	
	/**
	 * @return the id
	 */
	public long getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 */
	public void setId(long id) {
		this.id = id;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the user
	 */
	public String[] getUser() {
		return user;
	}

	/**
	 * @param user the user to set
	 */
	public void setUser(String[] user) {
		this.user = user;
	}

	/**
	 * @return the begin
	 */
	public Date getBegin() {
		return begin;
	}

	/**
	 * @param begin the begin to set
	 */
	public void setBegin(Date begin) {
		this.begin = begin;
	}
}
