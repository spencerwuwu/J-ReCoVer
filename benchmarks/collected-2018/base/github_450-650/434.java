// https://searchcode.com/api/result/105063490/

/* This code is part of Freenet. It is distributed under the GNU General
 * Public License, version 2 (or at your option any later version). See
 * http://www.gnu.org/ for further details of the GPL. */
package freenet.node;

import java.util.ArrayList;

import freenet.config.EnumerableOptionCallback;
import freenet.config.InvalidConfigValueException;
import freenet.config.NodeNeedRestartException;
import freenet.config.PersistentConfig;
import freenet.config.SubConfig;
import freenet.l10n.L10n;
import freenet.node.useralerts.AbstractUserAlert;
import freenet.node.useralerts.UserAlert;
import freenet.node.useralerts.UserAlertManager;
import freenet.support.HTMLNode;
import freenet.support.Logger;
import freenet.support.api.StringCallback;

/**
 * We have 3 basic security settings. The user chooses these in the first-time 
 * wizard, and can reconfigure them at any time. Each impacts on many other
 * config settings, changing their defaults and changing their values when the
 * security level changes, but the user can change those options independantly if
 * they do not change the security level. These options are important, and there
 * are explanations of every option for each setting. They have their own 
 * sub-page on the config toadlet. And the security levels are displayed on the
 * homepage as a useralert (instead of the opennet warning).
 * @author Matthew Toseland <toad@amphibian.dyndns.org> (0xE43DA450)
 */
public class SecurityLevels {
	
	private final Node node;
	
	public enum NETWORK_THREAT_LEVEL {
		LOW, // turn off every performance impacting security measure
		NORMAL, // normal setting, darknet/opennet hybrid
		HIGH, // darknet only, normal settings otherwise
		MAXIMUM, // paranoid - darknet only, turn off FOAF etc etc
	}
	
	public enum FRIENDS_THREAT_LEVEL {
		LOW, // Friends are ultimately trusted
		NORMAL, // Share some information
		HIGH, // Share no/minimal information and take measures to reduce harm if Friends are compromised
	}
	
	public enum PHYSICAL_THREAT_LEVEL {
		LOW, // Don't encrypt temp files etc etc
		NORMAL, // Encrypt temp files, centralise keys for client cache in master.keys, if that is deleted client cache is unreadable. Later on will include encrypting node.db4o as well, which contains tempfile keys.
		HIGH, // Password master.keys.
		MAXIMUM // Transient encryption for client cache, no persistent downloads support, etc.
	}
	
	NETWORK_THREAT_LEVEL networkThreatLevel;
	FRIENDS_THREAT_LEVEL friendsThreatLevel;
	PHYSICAL_THREAT_LEVEL physicalThreatLevel;
	
	private MyCallback<NETWORK_THREAT_LEVEL> networkThreatLevelCallback;
	private MyCallback<FRIENDS_THREAT_LEVEL> friendsThreatLevelCallback;
	private MyCallback<PHYSICAL_THREAT_LEVEL> physicalThreatLevelCallback;
	
	public SecurityLevels(Node node, PersistentConfig config) {
		this.node = node;
		SubConfig myConfig = new SubConfig("security-levels", config);
		int sortOrder = 0;
		networkThreatLevelCallback = new MyCallback<NETWORK_THREAT_LEVEL>() {

			@Override
			public String get() {
				synchronized(SecurityLevels.this) {
					return networkThreatLevel.name();
				}
			}

			public String[] getPossibleValues() {
				NETWORK_THREAT_LEVEL[] values = NETWORK_THREAT_LEVEL.values();
				String[] names = new String[values.length];
				for(int i=0;i<names.length;i++)
					names[i] = values[i].name();
				return names;
			}

			@Override
			protected NETWORK_THREAT_LEVEL getValue() {
				return networkThreatLevel;
			}

			@Override
			protected void setValue(String val) throws InvalidConfigValueException {
				NETWORK_THREAT_LEVEL newValue = parseNetworkThreatLevel(val);
				if(newValue == null)
					throw new InvalidConfigValueException("Invalid value for network threat level: "+val);
				synchronized(SecurityLevels.this) {
					networkThreatLevel = newValue;
				}
			}

		};
		myConfig.register("networkThreatLevel", "HIGH", sortOrder++, false, true, "SecurityLevels.networkThreatLevelShort", "SecurityLevels.networkThreatLevel", networkThreatLevelCallback);
		NETWORK_THREAT_LEVEL netLevel = NETWORK_THREAT_LEVEL.valueOf(myConfig.getString("networkThreatLevel"));
		if(myConfig.getRawOption("networkThreatLevel") != null) {
			networkThreatLevel = netLevel;
		} else {
			// Call all the callbacks so that the config is consistent with the threat level.
			setThreatLevel(netLevel);
		}
		friendsThreatLevelCallback = new MyCallback<FRIENDS_THREAT_LEVEL>() {

			@Override
			public String get() {
				synchronized(SecurityLevels.this) {
					return friendsThreatLevel.name();
				}
			}

			public String[] getPossibleValues() {
				FRIENDS_THREAT_LEVEL[] values = FRIENDS_THREAT_LEVEL.values();
				String[] names = new String[values.length];
				for(int i=0;i<names.length;i++)
					names[i] = values[i].name();
				return names;
			}

			@Override
			protected FRIENDS_THREAT_LEVEL getValue() {
				return friendsThreatLevel;
			}

			@Override
			protected void setValue(String val) throws InvalidConfigValueException {
				FRIENDS_THREAT_LEVEL newValue = FRIENDS_THREAT_LEVEL.valueOf(val);
				if(newValue != null)
					throw new InvalidConfigValueException("Invalid value for friends threat level: "+val);
				synchronized(SecurityLevels.this) {
					friendsThreatLevel = newValue;
				}
			}

		};
		myConfig.register("friendsThreatLevel", "NORMAL", sortOrder++, false, true, "SecurityLevels.friendsThreatLevelShort", "SecurityLevels.friendsThreatLevel", friendsThreatLevelCallback);
		FRIENDS_THREAT_LEVEL friendsLevel = FRIENDS_THREAT_LEVEL.valueOf(myConfig.getString("friendsThreatLevel"));;
		if(myConfig.getRawOption("friendsThreatLevel") != null) {
			friendsThreatLevel = friendsLevel;
		} else {
			// Call all the callbacks so that the config is consistent with the threat level.
			setThreatLevel(friendsLevel);
		}
		physicalThreatLevelCallback = new MyCallback<PHYSICAL_THREAT_LEVEL>() {

			@Override
			public String get() {
				synchronized(SecurityLevels.this) {
					return physicalThreatLevel.name();
				}
			}

			public String[] getPossibleValues() {
				PHYSICAL_THREAT_LEVEL[] values = PHYSICAL_THREAT_LEVEL.values();
				String[] names = new String[values.length];
				for(int i=0;i<names.length;i++)
					names[i] = values[i].name();
				return names;
			}

			@Override
			protected PHYSICAL_THREAT_LEVEL getValue() {
				return physicalThreatLevel;
			}

			@Override
			protected void setValue(String val) throws InvalidConfigValueException {
				PHYSICAL_THREAT_LEVEL newValue = PHYSICAL_THREAT_LEVEL.valueOf(val);
				if(newValue != null)
					throw new InvalidConfigValueException("Invalid value for physical threat level: "+val);
				synchronized(SecurityLevels.this) {
					physicalThreatLevel = newValue;
				}
			}

		};
		myConfig.register("physicalThreatLevel", "NORMAL", sortOrder++, false, true, "SecurityLevels.physicalThreatLevelShort", "SecurityLevels.physicalThreatLevel", physicalThreatLevelCallback);
		PHYSICAL_THREAT_LEVEL physLevel = PHYSICAL_THREAT_LEVEL.valueOf(myConfig.getString("physicalThreatLevel"));
		if(myConfig.getRawOption("physicalThreatLevel") != null) {
			physicalThreatLevel = physLevel;
		} else {
			// Call all the callbacks so that the config is consistent with the threat level.
			setThreatLevel(physLevel);
		}
		
		myConfig.finishedInitialization();
	}
	
	public synchronized void addNetworkThreatLevelListener(SecurityLevelListener<NETWORK_THREAT_LEVEL> listener) {
		networkThreatLevelCallback.addListener(listener);
	}
	
	public synchronized void addFriendsThreatLevelListener(SecurityLevelListener<FRIENDS_THREAT_LEVEL> listener) {
		friendsThreatLevelCallback.addListener(listener);
	}
	
	public synchronized void addPhysicalThreatLevelListener(SecurityLevelListener<PHYSICAL_THREAT_LEVEL> listener) {
		physicalThreatLevelCallback.addListener(listener);
	}
	
	private abstract class MyCallback<T> extends StringCallback implements EnumerableOptionCallback {

		private final ArrayList<SecurityLevelListener<T>> listeners;
		
		MyCallback() {
			listeners = new ArrayList<SecurityLevelListener<T>>();
		}
		
		public void addListener(SecurityLevelListener<T> listener) {
			if(listeners.contains(listener)) {
				Logger.error(this, "Already have listener "+listener+" in "+this);
				return;
			}
			listeners.add(listener);
		}
		
		@Override
		public void set(String val) throws InvalidConfigValueException, NodeNeedRestartException {
			T oldLevel = getValue();
			setValue(val);
			T newLevel = getValue();
			onSet(oldLevel, newLevel);
		}

		void onSet(T oldLevel, T newLevel) {
			for(SecurityLevelListener<T> listener : listeners) {
				listener.onChange(oldLevel, newLevel);
			}
		}

		protected abstract void setValue(String val) throws InvalidConfigValueException;

		protected abstract T getValue();
		
	}

	public NETWORK_THREAT_LEVEL getNetworkThreatLevel() {
		return networkThreatLevel;
	}
	
	public FRIENDS_THREAT_LEVEL getFriendsThreatLevel() {
		return friendsThreatLevel;
	}

	public PHYSICAL_THREAT_LEVEL getPhysicalThreatLevel() {
		return physicalThreatLevel;
	}
	
	public static NETWORK_THREAT_LEVEL parseNetworkThreatLevel(String arg) {
		try {
			return NETWORK_THREAT_LEVEL.valueOf(arg);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static FRIENDS_THREAT_LEVEL parseFriendsThreatLevel(String arg) {
		try {
			return FRIENDS_THREAT_LEVEL.valueOf(arg);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
	
	public static PHYSICAL_THREAT_LEVEL parsePhysicalThreatLevel(String arg) {
		try {
			return PHYSICAL_THREAT_LEVEL.valueOf(arg);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * If changing to the new threat level is a potential problem, warn the user,
	 * and include a checkbox for confirmation.
	 * @param newThreatLevel
	 * @return
	 */
	public HTMLNode getConfirmWarning(NETWORK_THREAT_LEVEL newThreatLevel, String checkboxName) {
		if(newThreatLevel == networkThreatLevel)
			return null; // Not going to be changed.
		HTMLNode parent = new HTMLNode("div");
		if((newThreatLevel == NETWORK_THREAT_LEVEL.HIGH && networkThreatLevel != NETWORK_THREAT_LEVEL.MAXIMUM) || 
				newThreatLevel == NETWORK_THREAT_LEVEL.MAXIMUM) {
			if(node.peers.getDarknetPeers().length == 0) {
				parent.addChild("p", l10n("noFriendsWarning"));
				if(newThreatLevel == NETWORK_THREAT_LEVEL.MAXIMUM) {
					HTMLNode p = parent.addChild("p");
					L10n.addL10nSubstitution(p, "SecurityLevels.maximumNetworkThreatLevelWarning", new String[] { "bold", "/bold" }, new String[] { "<b>", "</b>" });
				}
				parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("noFriendsCheckbox"));
				return parent;
			} else if(node.peers.countConnectedDarknetPeers() == 0) {
				parent.addChild("p", l10n("noConnectedFriendsWarning", "added", Integer.toString(node.peers.getDarknetPeers().length)));
				if(newThreatLevel == NETWORK_THREAT_LEVEL.MAXIMUM) {
					HTMLNode p = parent.addChild("p");
					L10n.addL10nSubstitution(p, "SecurityLevels.maximumNetworkThreatLevelWarning", new String[] { "bold", "/bold" }, new String[] { "<b>", "</b>" });
				}
				parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("noConnectedFriendsCheckbox"));
				return parent;
			} else if(node.peers.countConnectedDarknetPeers() < 10) {
				parent.addChild("p", l10n("fewConnectedFriendsWarning", new String[] { "connected", "added" }, new String[] { Integer.toString(node.peers.countConnectedDarknetPeers()), Integer.toString(node.peers.getDarknetPeers().length)}));
				if(newThreatLevel == NETWORK_THREAT_LEVEL.MAXIMUM) {
					HTMLNode p = parent.addChild("p");
					L10n.addL10nSubstitution(p, "SecurityLevels.maximumNetworkThreatLevelWarning", new String[] { "bold", "/bold" }, new String[] { "<b>", "</b>" });
				}
				parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("fewConnectedFriendsCheckbox"));
				return parent;
			}
		} else if(newThreatLevel == NETWORK_THREAT_LEVEL.LOW) {
			parent.addChild("p", l10n("networkThreatLevelLowWarning"));
			parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("networkThreatLevelLowCheckbox"));
			return parent;
		} // Don't warn on switching to NORMAL.
		if(newThreatLevel == NETWORK_THREAT_LEVEL.MAXIMUM) {
			HTMLNode p = parent.addChild("p");
			L10n.addL10nSubstitution(p, "SecurityLevels.maximumNetworkThreatLevelWarning", new String[] { "bold", "/bold" }, new String[] { "<b>", "</b>" });
			p.addChild("#", " ");
			L10n.addL10nSubstitution(p, "SecurityLevels.maxSecurityYouNeedFriends", new String[] { "bold", "/bold" }, new String[] { "<b>", "</b>" });
			parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("maximumNetworkThreatLevelCheckbox"));
			return parent;
		}
		return null;
	}
	
	public HTMLNode getConfirmWarning(FRIENDS_THREAT_LEVEL newFriendsLevel, String checkboxName) {
		if(newFriendsLevel == friendsThreatLevel)
			return null; // Not going to be changed.
		if(newFriendsLevel == FRIENDS_THREAT_LEVEL.HIGH) {
			HTMLNode parent = new HTMLNode("div");
			parent.addChild("p", l10n("highFriendsThreatLevelWarning"));
			parent.addChild("input", new String[] { "type", "name", "value" }, new String[] { "checkbox", checkboxName, "off" }, l10n("highFriendsThreatLevelCheckbox"));
			return parent;
		}
		return null;
	}

	private String l10n(String string) {
		return L10n.getString("SecurityLevels."+string);
	}

	private String l10n(String string, String pattern, String value) {
		return L10n.getString("SecurityLevels."+string, pattern, value);
	}

	private String l10n(String string, String[] patterns, String[] values) {
		return L10n.getString("SecurityLevels."+string, patterns, values);
	}

	public void setThreatLevel(NETWORK_THREAT_LEVEL newThreatLevel) {
		if(newThreatLevel == null) throw new NullPointerException();
		NETWORK_THREAT_LEVEL oldLevel;
		synchronized(this) {
			oldLevel = networkThreatLevel;
			networkThreatLevel = newThreatLevel;
		}
		networkThreatLevelCallback.onSet(oldLevel, newThreatLevel);
	}

	public void setThreatLevel(FRIENDS_THREAT_LEVEL newThreatLevel) {
		if(newThreatLevel == null) throw new NullPointerException();
		FRIENDS_THREAT_LEVEL oldLevel;
		synchronized(this) {
			oldLevel = friendsThreatLevel;
			friendsThreatLevel = newThreatLevel;
		}
		friendsThreatLevelCallback.onSet(oldLevel, newThreatLevel);
	}
	
	public void setThreatLevel(PHYSICAL_THREAT_LEVEL newThreatLevel) {
		if(newThreatLevel == null) throw new NullPointerException();
		PHYSICAL_THREAT_LEVEL oldLevel;
		synchronized(this) {
			oldLevel = physicalThreatLevel;
			physicalThreatLevel = newThreatLevel;
		}
		physicalThreatLevelCallback.onSet(oldLevel, newThreatLevel);
	}
	
	public void resetPhysicalThreatLevel(PHYSICAL_THREAT_LEVEL level) {
		physicalThreatLevel = level;
	}

	public static String localisedName(NETWORK_THREAT_LEVEL newThreatLevel) {
		return L10n.getString("SecurityLevels.networkThreatLevel.name."+newThreatLevel.name());
	}
	
	public static String localisedName(FRIENDS_THREAT_LEVEL newFriendsLevel) {
		return L10n.getString("SecurityLevels.friendsThreatLevel.name."+newFriendsLevel.name());
	}
	
	public static String localisedName(PHYSICAL_THREAT_LEVEL newPhysicalLevel) {
		return L10n.getString("SecurityLevels.physicalThreatLevel.name."+newPhysicalLevel.name());
	}

	public void registerUserAlert(UserAlertManager alerts) {
		alerts.register(new AbstractUserAlert() {

			public String anchor() {
				return "seclevels";
			}

			public String dismissButtonText() {
				return L10n.getString("UserAlert.hide");
			}

			public HTMLNode getHTMLText() {
				HTMLNode div = new HTMLNode("div");
				HTMLNode ul = div.addChild("ul");
				ul.addChild("li", l10n("userAlertNetworkThreatLevel", "level", localisedName(networkThreatLevel)));
				ul.addChild("li", l10n("userAlertFriendsThreatLevel", "level", localisedName(friendsThreatLevel)));
				ul.addChild("li", l10n("userAlertPhysicalThreatLevel", "level", localisedName(physicalThreatLevel)));
				div.addChild("br");
				L10n.addL10nSubstitution(div, "SecurityLevels.userAlertExtro",
						new String[] { "link", "/link" },
						new String[] { "<a href=\"/seclevels/\">", "</a>" });
				return div;
			}

			public short getPriorityClass() {
				return UserAlert.WARNING;
			}

			public String getShortText() {
				return l10n("userAlertShortText", new String[] { "network", "friends", "physical" },
						new String[] {
							localisedName(networkThreatLevel),
							localisedName(friendsThreatLevel),
							localisedName(physicalThreatLevel)} );
			}

			public String getText() {
				return getHTMLText().getContent();
			}

			public String getTitle() {
				return l10n("title");
			}

			public Object getUserIdentifier() {
				return null;
			}

			public boolean isEventNotification() {
				return false;
			}

			public boolean isValid() {
				return true;
			}

			public void isValid(boolean validity) {
				// Ignore
			}

			public void onDismiss() {
				// Ignore
			}

			public boolean shouldUnregisterOnDismiss() {
				return true;
			}

			public boolean userCanDismiss() {
				return true;
			}
			
		});
	}

}

