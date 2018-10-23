// https://searchcode.com/api/result/13355083/

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
package net.l2emuproject.gameserver.services.cursedweapons;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ScheduledFuture;

import net.l2emuproject.Config;
import net.l2emuproject.gameserver.datatables.SkillTable;
import net.l2emuproject.gameserver.items.L2ItemInstance;
import net.l2emuproject.gameserver.network.SystemMessageId;
import net.l2emuproject.gameserver.network.serverpackets.Earthquake;
import net.l2emuproject.gameserver.network.serverpackets.ExRedSky;
import net.l2emuproject.gameserver.network.serverpackets.InventoryUpdate;
import net.l2emuproject.gameserver.network.serverpackets.ItemList;
import net.l2emuproject.gameserver.network.serverpackets.SystemMessage;
import net.l2emuproject.gameserver.network.serverpackets.UserInfo;
import net.l2emuproject.gameserver.services.transformation.TransformationService;
import net.l2emuproject.gameserver.skills.L2Skill;
import net.l2emuproject.gameserver.system.database.L2DatabaseFactory;
import net.l2emuproject.gameserver.system.restriction.global.CursedWeaponRestriction;
import net.l2emuproject.gameserver.system.restriction.global.GlobalRestrictions;
import net.l2emuproject.gameserver.system.threadmanager.ThreadPoolManager;
import net.l2emuproject.gameserver.templates.item.L2Item;
import net.l2emuproject.gameserver.world.L2World;
import net.l2emuproject.gameserver.world.Location;
import net.l2emuproject.gameserver.world.object.L2Attackable;
import net.l2emuproject.gameserver.world.object.L2Character;
import net.l2emuproject.gameserver.world.object.L2Player;
import net.l2emuproject.tools.random.Rnd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CursedWeapon
{
	private static final Log _log = LogFactory.getLog(CursedWeaponsService.class);

	// _name is the name of the cursed weapon associated with its ID.
	private final String		_name;
	// _itemId is the Item ID of the cursed weapon.
	private final int			_itemId;
	// _skillId is the skills ID.
	private final int			_skillId;
	private final int			_skillMaxLevel;
	private int					_dropRate;
	private int					_duration;
	private int					_durationLost;
	private int					_disapearChance;
	private int					_stageKills;
	private int					_transformId;

	// this should be false unless if the cursed weapon is dropped, in that case it would be true.
	private boolean				_isDropped		= false;
	// this sets the cursed weapon status to true only if a player has the cursed weapon, otherwise this should be false.
	private boolean 			_isActivated		= false;
	private ScheduledFuture<?>	_removeTask;

	private int					_nbKills		= 0;
	private long				_endTime		= 0;

	private int					_playerId		= 0;
	private L2Player		_player			= null;
	private L2ItemInstance		_item			= null;
	private int					_playerKarma	= 0;
	private int					_playerPkKills	= 0;

	public CursedWeapon(int itemId, int skillId, String name)
	{
		_name = name;
		_itemId = itemId;
		_skillId = skillId;
		_skillMaxLevel = SkillTable.getInstance().getMaxLevel(_skillId);
	}

	public void endOfLife()
	{
		if (_isActivated)
		{
			if (_player != null && _player.isOnline() == 1)
			{
				// Remove from player
				_log.info(_name + " being removed online." );

				_player.abortAttack();

				_player.setKarma(_playerKarma);
				_player.setPkKills(_playerPkKills);
				_player.setCursedWeaponEquippedId(0);
				removeSkillAndAppearance();

				// Remove
				_player.getInventory().unEquipItemInBodySlotAndRecord(L2Item.SLOT_LR_HAND);
				_player.store();

				// Destroy
				L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					if (removedItem.getCount() == 0)
						iu.addRemovedItem(removedItem);
					else
						iu.addModifiedItem(removedItem);

					_player.sendPacket(iu);
				}
				else
					_player.sendPacket(new ItemList(_player, true));

				_player.broadcastUserInfo();
			}
			else
			{
				// Remove from Db
				_log.info(_name + " being removed offline." );

				Connection con = null;
				try
				{
					con = L2DatabaseFactory.getInstance().getConnection(con);

					// Delete the item
					PreparedStatement statement = con.prepareStatement("DELETE FROM items WHERE owner_id=? AND item_id=?");
					statement.setInt(1, _playerId);
					statement.setInt(2, _itemId);
					if (statement.executeUpdate() != 1)
					{
						_log.warn("Error while deleting itemId "+ _itemId +" from userId "+ _playerId);
					}
					statement.close();
					// Restore the karma
					statement = con.prepareStatement("UPDATE characters SET karma=?, pkkills=? WHERE charId=?");
					statement.setInt(1, _playerKarma);
					statement.setInt(2, _playerPkKills);
					statement.setInt(3, _playerId);
					if (statement.executeUpdate() != 1)
					{
						_log.warn("Error while updating karma & pkkills for userId "+_playerId);
					}
					statement.close();
				}
				catch (Exception e)
				{
					_log.warn("Could not delete:", e);
				}
				finally
				{
					L2DatabaseFactory.close(con);
				}
			}
		}
		else
		{
			// either this cursed weapon is in the inventory of someone who has another cursed weapon equipped,
			// OR this cursed weapon is on the ground.
			if (_player != null && _player.getInventory().getItemByItemId(_itemId) != null)
			{
				// Destroy
				L2ItemInstance removedItem = _player.getInventory().destroyItemByItemId("", _itemId, 1, _player, null);
				if (!Config.FORCE_INVENTORY_UPDATE)
				{
					InventoryUpdate iu = new InventoryUpdate();
					if (removedItem.getCount() == 0)
						iu.addRemovedItem(removedItem);
					else
						iu.addModifiedItem(removedItem);

					_player.sendPacket(iu);
				}
				else
					_player.sendPacket(new ItemList(_player, true));

				_player.broadcastUserInfo();
			}
			//  is dropped on the ground
			else if (_item != null)
			{
				_item.decayMe();
				L2World.getInstance().removeObject(_item);
				_log.info(_name+" item has been removed from World.");
			}
		}

		// Delete infos from table if any
		CursedWeaponsService.removeFromDb(_itemId);

		SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_DISAPPEARED);
		sm.addString(_name);
		CursedWeaponsService.announce(sm);

		// Reset  state
		cancelTask();
		_isActivated = false;
		_isDropped = false;
		_endTime = 0;
		_player = null;
		_playerId = 0;
		_playerKarma = 0;
		_playerPkKills = 0;
		_item = null;
		_nbKills = 0;
	}

	private void cancelTask()
	{
		if (_removeTask != null)
		{
			_removeTask.cancel(true);
			_removeTask = null;
		}
	}

	private class RemoveTask implements Runnable
	{
		protected RemoveTask()
		{
		}

		@Override
		public void run()
		{
			if (System.currentTimeMillis() >= getEndTime())
				endOfLife();
		}
	}

	private void dropIt(L2Attackable attackable, L2Player player)
	{
		dropIt(attackable, player, null, true);
	}

	private void dropIt(L2Attackable attackable, L2Player player, L2Character killer, boolean fromMonster)
	{
		_isActivated = false;

		if (fromMonster)
		{
			_item = attackable.dropItem(player, _itemId, 1);
			_item.setDropTime(0); // Prevent item from being removed by ItemsAutoDestroy

			// RedSky and Earthquake
			ExRedSky packet = new ExRedSky(10);
			Earthquake eq = new Earthquake(player.getX(), player.getY(), player.getZ(), 14, 3);
			for (L2Player aPlayer : L2World.getInstance().getAllPlayers())
			{
				aPlayer.sendPacket(packet);
				aPlayer.sendPacket(eq);
			}
		}
		else
		{
			_player.dropItem("DieDrop", _item, killer, true);

			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkillAndAppearance();
			_player.abortAttack();
		}

		_isDropped = true;
		SystemMessage sm = new SystemMessage(SystemMessageId.S2_WAS_DROPPED_IN_THE_S1_REGION);
		sm.addZoneName(player.getX(), player.getY(), player.getZ()); // Region Name
		sm.addItemName(_item);
		CursedWeaponsService.announce(sm);
	}

	public void transform()
	{
		if (_transformId == 0)
			return;

		if (_player.getPlayerTransformation().getTransformation() != null)
		{
			_player.stopTransformation(true);

			ThreadPoolManager.getInstance().scheduleGeneral(new Runnable()
			{
				@Override
				public void run()
				{
					TransformationService.getInstance().transformPlayer(_transformId, _player);
				}
			}, 500);
		}
		else
			TransformationService.getInstance().transformPlayer(_transformId, _player);
	}

	public void cursedOnLogin()
	{
		transform();
		giveSkill();

		SystemMessage msg = new SystemMessage(SystemMessageId.S2_OWNER_HAS_LOGGED_INTO_THE_S1_REGION);
		msg.addZoneName(_player.getX(), _player.getY(), _player.getZ());
		msg.addItemName(_player.getCursedWeaponEquippedId());
		CursedWeaponsService.announce(msg);

		CursedWeapon cw = CursedWeaponsService.getInstance().getCursedWeapon(_player.getCursedWeaponEquippedId());
		SystemMessage msg2 = new SystemMessage(SystemMessageId.S2_MINUTE_OF_USAGE_TIME_ARE_LEFT_FOR_S1);
		int timeLeftInHours = (int) ((cw.getTimeLeft() / 60000) / 60);
		msg2.addItemName(_player.getCursedWeaponEquippedId());
		msg2.addNumber(timeLeftInHours * 60);
		_player.sendPacket(msg2);
	}

	/**
	* Yesod:<br>
	* Rebind the passive skill belonging to the CursedWeapon. Invoke this
	* method if the weapon owner switches to a subclass.
	*/
	public void giveSkill()
	{
		int level = 1 + (_nbKills / _stageKills);
		if (level > _skillMaxLevel)
			level = _skillMaxLevel;

		L2Skill skill = SkillTable.getInstance().getInfo(_skillId, level);
		// Yesod:
		// To properly support subclasses this skill can not be stored.
		_player.addSkill(skill, false);
		skill = SkillTable.getInstance().getInfo(3630, 1);
		_player.addSkill(skill, false);
		skill = SkillTable.getInstance().getInfo(3631, 1);
		_player.addSkill(skill, false);
		_player.getPlayerTransformation().setTransformAllowedSkills(new int[]{3630,3631});

		if (_log.isDebugEnabled())
			_log.debug("Player "+_player.getName() +" has been awarded with skill "+skill);
	}

	public void removeSkillAndAppearance()
	{
		_player.getPlayerTransformation().untransform();

		_player.removeSkill(SkillTable.getInstance().getInfo(_skillId, _player.getSkillLevel(_skillId)), false);

		if (_player.getPlayerTransformation().transformId() > 0)
		{
			TransformationService.getInstance().transformPlayer(_player.getPlayerTransformation().transformId(), _player);
		}
	}

	public void reActivate()
	{
		_isActivated = true;
		if (_endTime - System.currentTimeMillis() <= 0)
			endOfLife();
		else
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
					new RemoveTask(), _durationLost * 12000L, _durationLost * 12000L);
	}

	public boolean checkDrop(L2Attackable attackable, L2Player player)
	{
		if (Rnd.get(100000) < _dropRate)
		{
			// Drop the item
			dropIt(attackable, player);

			// Start the Life Task
			_endTime = System.currentTimeMillis() + _duration * 60000L;
			_removeTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(
					new RemoveTask(), _durationLost * 12000, _durationLost * 12000L);

			return true;
		}

		return false;
	}

	public boolean activate(L2Player player, L2ItemInstance item)
	{
		if (GlobalRestrictions.isRestricted(player, CursedWeaponRestriction.class))
		{
			// TODO: msg
			return false;
		}

		// if the player is mounted, attempt to unmount first.  Only allow picking up
		// the zariche if unmounting is successful.
		if (player.isMounted())
		{
			if (!player.dismount())
			{
				// TODO: correct this custom message.
				player.sendMessage("You may not pick up this item while riding in this territory");
				return false;
			}
		}

		_isActivated = true;

		// Player holding it data
		_player = player;
		_playerId = _player.getObjectId();
		_playerKarma = _player.getKarma();
		_playerPkKills = _player.getPkKills();
		saveData();

		// Change player stats
		_player.setKarma(9999999);
		_player.setPkKills(0);
		if (_player.isInParty())
			_player.getParty().removePartyMember(_player);

		// Add skill
		giveSkill();

		// Equip with the weapon
		_item = item;
		//L2ItemInstance[] items =
		_player.getInventory().equipItemAndRecord(_item);
		SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);

		// Fully heal player
		_player.getStatus().setCurrentHpMp(_player.getMaxHp(), _player.getMaxMp());
		_player.getStatus().setCurrentCp(_player.getMaxCp());

		// Refresh inventory
		if (!Config.FORCE_INVENTORY_UPDATE)
		{
			InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(_item);
			//iu.addItems(Arrays.asList(items));
			_player.sendPacket(iu);
		}
		else
			_player.sendPacket(new ItemList(_player, false));

		// Save previous transformation
		if (_player.getPlayerTransformation().transformId() > 0)
			_player.getPlayerTransformation().transformInsertInfo();
		// Refresh player stats
		transform();
		_player.broadcastUserInfo();
		_player.setCursedWeaponEquippedId(_itemId);
		//SocialAction atk = new SocialAction(_player.getObjectId(), 17);

		//_player.broadcastPacket(atk);

		sm = new SystemMessage(SystemMessageId.THE_OWNER_OF_S2_HAS_APPEARED_IN_THE_S1_REGION);
		sm.addZoneName(_player.getX(), _player.getY(), _player.getZ()); // Region Name
		sm.addItemName(_item);
		CursedWeaponsService.announce(sm);
		return true;
	}

	public void saveData()
	{
		if (_log.isDebugEnabled())
			_log.debug("CursedWeapon: Saving data to disk.");

		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection(con);

			// Delete previous datas
			PreparedStatement statement = con.prepareStatement("DELETE FROM cursed_weapons WHERE itemId = ?");
			statement.setInt(1, _itemId);
			statement.executeUpdate();

			if (_isActivated)
			{
				statement = con.prepareStatement("INSERT INTO cursed_weapons (itemId, charId, playerKarma, playerPkKills, nbKills, endTime) VALUES (?, ?, ?, ?, ?, ?)");
				statement.setInt(1, _itemId);
				statement.setInt(2, _playerId);
				statement.setInt(3, _playerKarma);
				statement.setInt(4, _playerPkKills);
				statement.setInt(5, _nbKills);
				statement.setLong(6, _endTime);
				statement.executeUpdate();
			}

			statement.close();
		}
		catch (SQLException e)
		{
			_log.error("CursedWeapon: Failed to save data: ", e);
		}
		finally
		{
			L2DatabaseFactory.close(con);
		}
	}

	public void dropIt(L2Character killer)
	{
		if (Rnd.get(100) <= _disapearChance)
		{
			// Remove it
			endOfLife();
		}
		else
		{
			//Unequip & Drop
			dropIt(null, _player, killer, false);
			// Reset player stats
			_player.setKarma(_playerKarma);
			_player.setPkKills(_playerPkKills);
			_player.setCursedWeaponEquippedId(0);
			removeSkillAndAppearance();

			_player.abortAttack();

			// Unequip weapon
			//_player.getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_LRHAND);

			_player.broadcastUserInfo();
		}
	}


	public void increaseKills()
	{
		_nbKills++;

		if (_player != null && _player.isOnline() > 0)
		{
			_player.setPkKills(_nbKills);
			_player.sendPacket(new UserInfo(_player));

			if (_nbKills % _stageKills == 0 && _nbKills <= _stageKills*(_skillMaxLevel-1))
			{
				giveSkill();
			}
		}

		// Reduce time-to-live
		_endTime -= _durationLost * 60000;
		saveData();
	}

	public void increaseKills(int kills)
	{
		_nbKills += kills;

		if (_player != null && _player.isOnline() > 0)
		{
			_player.setPkKills(_nbKills);
			_player.broadcastUserInfo();

			if (_nbKills % _stageKills == 0 && _nbKills <= _stageKills*(_skillMaxLevel-1))
			{
				giveSkill();
			}
		}

		// Reduce time-to-live
		_endTime -= _durationLost * 60000;
		saveData();
	}

	public void setDisapearChance(int disapearChance)
	{
		_disapearChance = disapearChance;
	}
	public void setDropRate(int dropRate)
	{
		_dropRate = dropRate;
	}
	public void setDuration(int duration)
	{
		_duration = duration;
	}
	public void setDurationLost(int durationLost)
	{
		_durationLost = durationLost;
	}
	public void setStageKills(int stageKills)
	{
		_stageKills = stageKills;
	}
	public void setNbKills(int nbKills)
	{
		_nbKills = nbKills;
	}
	public void setPlayerId(int playerId)
	{
		_playerId = playerId;
	}
	public void setPlayerKarma(int playerKarma)
	{
		_playerKarma = playerKarma;
	}
	public void setPlayerPkKills(int playerPkKills)
	{
		_playerPkKills = playerPkKills;
	}
	public void setActivated(boolean isActivated)
	{
		_isActivated = isActivated;
	}
	public void setDropped(boolean isDropped)
	{
		_isDropped = isDropped;
	}
	public void setEndTime(long endTime)
	{
		_endTime = endTime;
	}
	public void setPlayer(L2Player player)
	{
		_player = player;
	}
	public void setItem(L2ItemInstance item)
	{
		_item = item;
	}

	public void setTransformId(int id)
	{
		_transformId = id;
	}

	public boolean isActivated()
	{
		return _isActivated;
	}

	public boolean isDropped()
	{
		return _isDropped;
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public String getName()
	{
		return _name;
	}

	public int getItemId()
	{
		return _itemId;
	}

	public int getSkillId()
	{
		return _skillId;
	}

	public int getPlayerId()
	{
		return _playerId;
	}

	public L2Player getPlayer()
	{
		return _player;
	}

	public int getPlayerKarma()
	{
		return _playerKarma;
	}

	public int getPlayerPkKills()
	{
		return _playerPkKills;
	}

	public int getNbKills()
	{
		return _nbKills;
	}

	public int getStageKills()
	{
		return _stageKills;
	}

	public boolean isActive()
	{
		return _isActivated || _isDropped;
	}

	public int getLevel()
	{
		return Math.min(1 + (_nbKills / _stageKills), _skillMaxLevel);
	}

	public long getTimeLeft()
	{
		return _endTime - System.currentTimeMillis();
	}

	public int getTransformId()
	{
		return _transformId;
	}

	public void goTo(L2Player player)
	{
		if (player == null)
			return;

		if (_isActivated && _player != null)
		{
			// Go to player holding the weapon
			player.teleToLocation(_player.getX(), _player.getY(), _player.getZ() + 20);
		}
		else if (_isDropped && _item != null)
		{
			// Go to item on the groud
			player.teleToLocation(_item.getX(), _item.getY(), _item.getZ() + 20);
		}
		else
		{
			player.sendMessage(_name + " isn't in the World.");
		}
	}

	public Location getCurrentLocation()
	{
		if (_isActivated && _player != null)
			return _player.getPosition().getCurrentLocation();

		if (_isDropped && _item != null)
			return _item.getPosition().getCurrentLocation();

		return null;
	}
}

