// https://searchcode.com/api/result/13355043/

/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package net.l2emuproject.gameserver.world.spawn;

import java.lang.reflect.Constructor;

import net.l2emuproject.Config;
import net.l2emuproject.gameserver.datatables.NpcTable;
import net.l2emuproject.gameserver.system.idfactory.IdFactory;
import net.l2emuproject.gameserver.system.threadmanager.ThreadPoolManager;
import net.l2emuproject.gameserver.templates.chars.L2NpcTemplate;
import net.l2emuproject.gameserver.world.Location;
import net.l2emuproject.gameserver.world.geodata.GeoData;
import net.l2emuproject.gameserver.world.object.L2Attackable;
import net.l2emuproject.gameserver.world.object.L2Character;
import net.l2emuproject.gameserver.world.object.L2Npc;
import net.l2emuproject.gameserver.world.object.L2Object;
import net.l2emuproject.gameserver.world.object.instance.L2DecoyInstance;
import net.l2emuproject.gameserver.world.object.instance.L2EffectPointInstance;
import net.l2emuproject.gameserver.world.object.instance.L2MinionInstance;
import net.l2emuproject.gameserver.world.object.instance.L2MonsterInstance;
import net.l2emuproject.gameserver.world.object.instance.L2NpcInstance;
import net.l2emuproject.gameserver.world.object.instance.L2PetInstance;
import net.l2emuproject.gameserver.world.object.instance.L2TrapInstance;
import net.l2emuproject.gameserver.world.object.position.L2CharPosition;
import net.l2emuproject.tools.random.Rnd;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * This class manages the spawn and respawn of a group of L2Npc that are in the same are and have the same type.
 * 
 * <B><U> Concept</U> :</B><BR><BR>
 * L2Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
 * The heading of the L2Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
 * 
 * @author Nightmare
 * @version $Revision: 1.9.2.3.2.8 $ $Date: 2005/03/27 15:29:32 $
 */
public class L2Spawn
{
	protected static final Log _log = LogFactory.getLog(L2Spawn.class);

	/** The link on the L2NpcTemplate object containing generic and static properties of this spawn (ex : RewardExp, RewardSP, AggroRange...) */
	private final L2NpcTemplate	_template;

	/** The Identifier of this spawn in the spawn table */
	private int				_id;

	/** The Identifier of this spawn in the db table */
	private int				_dbid;

	// private String _location = DEFAULT_LOCATION;

	/** The identifier of the location area where L2Npc can be spwaned */
	private int				_location;

	/** The maximum number of L2Npc that can manage this L2Spawn */
	private int				_maximumCount;

	/** The current number of L2Npc managed by this L2Spawn */
	private int				_currentCount;

	/** The current number of SpawnTask in progress or stand by of this L2Spawn */
	protected int			_scheduledCount;

	/** The X position of the spwan point */
	private int				_locX;

	/** The Y position of the spwan point */
	private int				_locY;

	/** The Z position of the spwan point */
	private int				_locZ;

	/** The heading of L2Npc when they are spawned */
	private int				_heading;

	/** The delay between a L2Npc remove and its re-spawn */
	private int				_respawnDelay;

	/** Minimum delay RaidBoss */
	private int				_respawnMinDelay;

	/** Maximum delay RaidBoss */
	private int				_respawnMaxDelay;

	private int				_instanceId	= 0;

	/** The generic constructor of L2Npc managed by this L2Spawn */
	private Constructor<?>	_constructor;

	/** If True a L2Npc is respawned each time that another is killed */
	// [L2J_JP DELETE]private boolean _doRespawn;
	protected boolean		_doRespawn;

	public boolean isRespawnable()
	{
		return _doRespawn;
	}

	/** If True then spawn point is custom */
	private boolean			_customSpawn;

	private L2Npc			_lastSpawn;

	/** The task launching the function doSpawn() */
	private final class SpawnTask implements Runnable
	{
		private final L2Npc _oldNpc;
		
		private SpawnTask(L2Npc pOldNpc)
		{
			_oldNpc = pOldNpc;
		}
		
		@Override
		public void run()
		{
			try
			{
				// [L2J_JP DELETE SANDMAN]respawnNpc(oldNpc);
				if (_doRespawn)
					respawnNpc(_oldNpc);
			}
			catch (RuntimeException e)
			{
				_log.warn("", e);
			}
			
			_scheduledCount--;
		}
	}

	/**
	 * Constructor of L2Spawn.<BR><BR>
	 * 
	 * <B><U> Concept</U> :</B><BR><BR>
	 * Each L2Spawn owns generic and static properties (ex : RewardExp, RewardSP, AggroRange...).
	 * All of those properties are stored in a different L2NpcTemplate for each type of L2Spawn.
	 * Each template is loaded once in the server cache memory (reduce memory use).
	 * When a new instance of L2Spawn is created, server just create a link between the instance and the template.
	 * This link is stored in <B>_template</B><BR><BR>
	 * 
	 * Each L2Npc is linked to a L2Spawn that manages its spawn and respawn (delay, location...).
	 * This link is stored in <B>_spawn</B> of the L2Npc<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Set the _template of the L2Spawn </li>
	 * <li>Calculate the implementationName used to generate the generic constructor of L2Npc managed by this L2Spawn</li>
	 * <li>Create the generic constructor of L2Npc managed by this L2Spawn</li><BR><BR>
	 * 
	 * @param mobTemplate The L2NpcTemplate to link to this L2Spawn
	 * 
	 */
	public L2Spawn(L2NpcTemplate mobTemplate) throws SecurityException
	{
		// Set the _template of the L2Spawn
		_template = mobTemplate;
		
		try
		{
			// Create the generic constructor of L2Npc managed by this L2Spawn
			_constructor = _template.getDefaultConstructor();
		}
		catch (NoSuchMethodException e)
		{
			_log.fatal("", e);
		}
	}
	public L2Spawn(int npcId)
	{
		this(NpcTable.getInstance().getTemplate(npcId));
	}

	/**
	 * Return the maximum number of L2Npc that this L2Spawn can manage.<BR><BR>
	 */
	public int getAmount()
	{
		return _maximumCount;
	}

	/**
	 * Return the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public int getId()
	{
		return _id;
	}

	/**
	 * Return the Identifier of this L2Spwan (used as key in the Spawnlist).<BR><BR>
	 */
	public int getDbId()
	{
		return _dbid;
	}

	/**
	 * Return the Identifier of the location area where L2Npc can be spwaned.<BR><BR>
	 */
	public int getLocation()
	{
		return _location;
	}

	/**
	 * Return the X position of the spwan point.<BR><BR>
	 */
	public int getLocx()
	{
		return _locX;
	}

	/**
	 * Return the Y position of the spwan point.<BR><BR>
	 */
	public int getLocy()
	{
		return _locY;
	}

	/**
	 * Return the Z position of the spwan point.<BR><BR>
	 */
	public int getLocz()
	{
		return _locZ;
	}

	/**
	 * Return the Itdentifier of the L2Npc manage by this L2Spwan contained in the L2NpcTemplate.<BR><BR>
	 */
	public int getNpcId()
	{
		return _template.getNpcId();
	}

	// TODO : This is just a temp fix... check every quest
	@Deprecated
	public int getNpcid()
	{
		return _template.getNpcId();
	}

	/**
	 * Return the heading of L2Npc when they are spawned.<BR><BR>
	 */
	public int getHeading()
	{
		return _heading;
	}

	/**
	 * Return the delay between a L2Npc remove and its re-spawn.<BR><BR>
	 */
	public int getRespawnDelay()
	{
		return _respawnDelay;
	}

	/**
	 * Return Min <Boss Spawn delay.<BR><BR>
	*/
	public int getRespawnMinDelay()
	{
		return _respawnMinDelay;
	}

	/**
	 * Return Max RaidBoss Spawn delay.<BR><BR>
	*/
	public int getRespawnMaxDelay()
	{
		return _respawnMaxDelay;
	}

	/**
	 * Return type of spawn.<BR><BR>
	 */
	public boolean isCustom()
	{
		return _customSpawn;
	}

	/**
	 * Set the maximum number of L2Npc that this L2Spawn can manage.<BR><BR>
	 */
	public void setAmount(int amount)
	{
		// [L2J_JP EDIT SANDMAN]
		//_maximumCount = amount;
		if (amount < 1)
			amount = 1;
		_maximumCount = amount;
	}

	/**
	 * Set the Identifier of this L2Spwan (used as key in the SpawnTable).<BR><BR>
	 */
	public void setId(int id)
	{
		_id = id;
	}

	/**
	* Set the Identifier of this L2Spwan (used as key in the Spawnlist).<BR><BR>
	*/
	public void setDbId(int id)
	{
		_dbid = id;
	}

	/**
	 * Set the Identifier of the location area where L2Npc can be spwaned.<BR><BR>
	 */
	public void setLocation(int location)
	{
		_location = location;
	}

	/**
	 * Set Minimum Respawn Delay.<BR><BR>
	 */
	public void setRespawnMinDelay(int date)
	{
		_respawnMinDelay = date;
	}

	/**
	 * Set Maximum Respawn Delay.<BR><BR>
	 */
	public void setRespawnMaxDelay(int date)
	{
		_respawnMaxDelay = date;
	}
	
	public void setLoc(Location loc)
	{
		setLocx(loc.getX());
		setLocy(loc.getY());
		setLocz(loc.getZ());
		setHeading(loc.getHeading());
	}
	
	public void setLoc(L2Object obj)
	{
		setLocx(obj.getX());
		setLocy(obj.getY());
		setLocz(obj.getZ());
		setHeading(obj.getHeading());
	}
	
	public void setLoc(L2CharPosition pos)
	{
		setLocx(pos.x);
		setLocy(pos.y);
		setLocz(pos.z);
		setHeading(pos.heading);
	}
	
	/**
	 * Set the X position of the spwan point.<BR><BR>
	 */
	public void setLocx(int locx)
	{
		_locX = locx;
	}

	/**
	 * Set the Y position of the spwan point.<BR><BR>
	 */
	public void setLocy(int locy)
	{
		_locY = locy;
	}

	/**
	 * Set the Z position of the spwan point.<BR><BR>
	 */
	public void setLocz(int locz)
	{
		_locZ = locz;
	}

	/**
	 * Set the heading of L2Npc when they are spawned.<BR><BR>
	 */
	public void setHeading(int heading)
	{
		_heading = heading;
	}

	/**
	 * Set the type of spawn.<BR><BR>
	 */
	public void setCustom()
	{
		_customSpawn = true;
	}

	/**
	 * Decrease the current number of L2Npc of this L2Spawn and if necessary create a SpawnTask to launch after the respawn Delay.<BR><BR>
	 * 
	 * <B><U> Actions</U> :</B><BR><BR>
	 * <li>Decrease the current number of L2Npc of this L2Spawn </li>
	 * <li>Check if respawn is possible to prevent multiple respawning caused by lag </li>
	 * <li>Update the current number of SpawnTask in progress or stand by of this L2Spawn </li>
	 * <li>Create a new SpawnTask to launch after the respawn Delay </li><BR><BR>
	 * 
	 * <FONT COLOR=#FF0000><B> <U>Caution</U> : A respawn is possible ONLY if _doRespawn=True and _scheduledCount + _currentCount < _maximumCount</B></FONT><BR><BR>
	 * 
	 */
	public void decreaseCount(L2Npc oldNpc)
	{
		// sanity check
		if (_currentCount <= 0)
			return;

		// Decrease the current number of L2Npc of this L2Spawn
		_currentCount--;

		// Check if respawn is possible to prevent multiple respawning caused by lag
		if (isRespawnable() && (_doRespawn && _scheduledCount + _currentCount < _maximumCount))
		{
			// Update the current number of SpawnTask in progress or stand by of this L2Spawn
			_scheduledCount++;

			// Create a new SpawnTask to launch after the respawn Delay
			ThreadPoolManager.getInstance().scheduleGeneral(new SpawnTask(oldNpc), _respawnDelay);
		}
	}

	/**
	 * Create the initial spawning and set _doRespawn to True.<BR><BR>
	 *
	 * @return The number of L2Npc that were spawned
	 */
	public int init()
	{
		return init(false);
	}

	public int init(boolean firstspawn)
	{
		while (_currentCount < _maximumCount)
		{
			doSpawn(false, firstspawn);
		}
		_doRespawn = true;

		return _currentCount;
	}

	/**
	 * Create a L2Npc in this L2Spawn.<BR><BR>
	 */
	public L2Npc spawnOne(boolean val)
	{
		return doSpawn(val);
	}

	public L2Npc doSpawn(boolean isSummonSpawn)
	{
		return doSpawn(isSummonSpawn, false);
	}

	public L2Npc doSpawn()
	{
		return doSpawn(false, false);
	}

	/**
	 * Set _doRespawn to False to stop respawn in thios L2Spawn.<BR><BR>
	 */
	public void stopRespawn()
	{
		_doRespawn = false;
	}

	/**
	 * Set _doRespawn to True to start or restart respawn in this L2Spawn.<BR><BR>
	 */
	public void startRespawn()
	{
		_doRespawn = true;
	}

	/**
	 * Create the L2Npc, add it to the world and lauch its onSpawn action.<BR><BR>
	 * 
	 * <B><U> Concept</U> :</B><BR><BR>
	 * L2Npc can be spawned either in a random position into a location area (if Lox=0 and Locy=0), either at an exact position.
	 * The heading of the L2Npc can be a random heading if not defined (value= -1) or an exact heading (ex : merchant...).<BR><BR>
	 * 
	 * <B><U> Actions for an random spawn into location area</U> : <I>(if Locx=0 and Locy=0)</I></B><BR><BR>
	 * <li>Get L2Npc Init parameters and its generate an Identifier </li>
	 * <li>Call the constructor of the L2Npc </li>
	 * <li>Calculate the random position in the location area (if Locx=0 and Locy=0) or get its exact position from the L2Spawn </li>
	 * <li>Set the position of the L2Npc </li>
	 * <li>Set the HP and MP of the L2Npc to the max </li>
	 * <li>Set the heading of the L2Npc (random heading if not defined : value=-1) </li>
	 * <li>Link the L2Npc to this L2Spawn </li>
	 * <li>Init other values of the L2Npc (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world </li>
	 * <li>Lauch the action onSpawn fo the L2Npc </li><BR><BR>
	 * <li>Increase the current number of L2Npc managed by this L2Spawn  </li><BR><BR>
	 * 
	 */
	public L2Npc doSpawn(boolean isSummonSpawn, boolean firstspawn)
	{
		L2Npc mob = null;
		try
		{
			// Check if the L2Spawn is not a L2Pet or L2Minion or L2Decoy spawn
			if (_template.isAssignableTo(L2PetInstance.class) || _template.isAssignableTo(L2MinionInstance.class)
					|| _template.isAssignableTo(L2DecoyInstance.class) || _template.isAssignableTo(L2TrapInstance.class)
					|| _template.isAssignableTo(L2EffectPointInstance.class))
			{
				_currentCount++;
				return mob;
			}

			// Get L2Npc Init parameters and its generate an Identifier
			Object[] parameters =
			{ IdFactory.getInstance().getNextId(), _template };

			// Call the constructor of the L2Npc
			// (can be a L2ArtefactInstance, L2FriendlyMobInstance, L2GuardInstance, L2MonsterInstance, L2SiegeGuardInstance or L2NpcInstance)
			L2Object tmp = (L2Object) _constructor.newInstance(parameters);
			// Must be done before object is spawned into visible world
			tmp.setInstanceId(_instanceId);

			if (isSummonSpawn && tmp instanceof L2Character)
				((L2Character) tmp).setShowSummonAnimation(isSummonSpawn);
			// Check if the Instance is a L2Npc
			if (!(tmp instanceof L2Npc))
				return mob;
			mob = (L2Npc) tmp;
			return intializeNpcInstance(mob, firstspawn);
		}
		catch (Exception e)
		{
			// Spawning failed
			_currentCount++;
			_log.warn("NPC " + _template.getNpcId() + " class not found: ", e);
		}
		return mob;
	}

	/**
	 * @param mob
	 * @return
	 */
	private L2Npc intializeNpcInstance(L2Npc mob, boolean firstspawn)
	{
		int newlocx, newlocy, newlocz;

		boolean doCorrect = false;
		if (Config.GEODATA > 0)
		{
			switch (Config.GEO_CORRECT_Z)
			{
			case ALL:
				doCorrect = true;
				break;
			case TOWN:
				if (mob instanceof L2NpcInstance)
					doCorrect = true;
				break;
			case MONSTER:
				if (mob instanceof L2Attackable)
					doCorrect = true;
				break;
			}
		}

		// The L2Npc is spawned at the exact position (Lox, Locy, Locz)
		newlocx = getLocx();
		newlocy = getLocy();
		newlocz = doCorrect ? GeoData.getInstance().getSpawnHeight(newlocx, newlocy, getLocz(), getLocz(), _id) : getLocz();

		mob.stopAllEffects();

		// setting up champion mobs
		if (mob.canBeChampion())
		{
			if (Rnd.get(100000) <= Config.CHAMPION_FREQUENCY)
			{
				mob.setChampion(true);
			}
		}
		else
		{
			mob.setChampion(false);
		}

		if (mob.getNpcId() >= 22349 && mob.getNpcId() <= 22353)
			mob.setQuestDropable(false);

		mob.setIsDead(false);
		// Reset decay info
		mob.setDecayed(false);
		// Set the HP and MP of the L2Npc to the max
		mob.getStatus().setCurrentHpMp(mob.getMaxHp(), mob.getMaxMp());

		// Set the heading of the L2Npc (random heading if not defined)
		if (getHeading() == -1)
		{
			mob.setHeading(Rnd.nextInt(61794));
		}
		else
		{
			mob.setHeading(getHeading());
		}
		
		mob.setChampion(false);
		if (Config.CHAMPION_FREQUENCY > 0)
		{
			if (mob instanceof L2MonsterInstance && !getTemplate().isQuestMonster() &&
				(!mob.isRaid() || Config.CHAMPION_BOSS) &&
				(!mob.isRaidMinion() || Config.CHAMPION_MINIONS) &&
				mob.getLevel() <= Config.CHAMPION_MAX_LEVEL &&
				mob.getLevel() >= Config.CHAMPION_MIN_LEVEL)
			{
				mob.setChampion(Rnd.get(100) < Config.CHAMPION_FREQUENCY);
			}
		}

		// Link the L2Npc to this L2Spawn
		mob.setSpawn(this);

		// Init other values of the L2Npc (ex : from its L2CharTemplate for INT, STR, DEX...) and add it in the world as a visible object
		mob.spawnMe(newlocx, newlocy, newlocz, firstspawn);
		
		L2Spawn.notifyNpcSpawned(mob);

		_lastSpawn = mob;

		if (_log.isDebugEnabled())
			_log.debug("spawned Mob ID: " + _template.getNpcId() + " ,at: " + mob.getX() + " x, " + mob.getY() + " y, " + mob.getZ() + " z");

		// Increase the current number of L2Npc managed by this L2Spawn
		_currentCount++;
		return mob;
	}

	private static SpawnListener[] _spawnListeners = new SpawnListener[0];
	
	public static void addSpawnListener(SpawnListener listener)
	{
		synchronized (_spawnListeners)
		{
			_spawnListeners = (SpawnListener[])ArrayUtils.add(_spawnListeners, listener);
		}
	}
	
	public static void notifyNpcSpawned(L2Npc npc)
	{
		synchronized (_spawnListeners)
		{
			for (SpawnListener listener : _spawnListeners)
				listener.npcSpawned(npc);
		}
	}

	/**
	 * @param i delay in seconds
	 */
	public void setRespawnDelay(int i)
	{
		if (i < 0)
			_log.warn("respawn delay is negative for spawnId:" + _id);

		if (i < 10 && i != 0)
			i = 10;

		_respawnDelay = i * 1000;
	}

	public L2Npc getLastSpawn()
	{
		return _lastSpawn;
	}

	/**
	 * @param oldNpc
	 */
	public void respawnNpc(L2Npc oldNpc)
	{
		oldNpc.refreshID();
		intializeNpcInstance(oldNpc, false);
	}

	public L2NpcTemplate getTemplate()
	{
		return _template;
	}

	public int getInstanceId()
	{
		return _instanceId;
	}

	public void setInstanceId(int instanceId)
	{
		_instanceId = instanceId;
	}
}

