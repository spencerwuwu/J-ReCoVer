// https://searchcode.com/api/result/11518074/

/*
 * Emulator game server Aion 2.7 from the command of developers 'Aion-Knight Dev. Team' is
 * free software; you can redistribute it and/or modify it under the terms of
 * GNU affero general Public License (GNU GPL)as published by the free software
 * security (FSF), or to License version 3 or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranties related to
 * CONSUMER PROPERTIES and SUITABILITY FOR CERTAIN PURPOSES. For details, see
 * General Public License is the GNU.
 *
 * You should have received a copy of the GNU affero general Public License along with this program.
 * If it is not, write to the Free Software Foundation, Inc., 675 Mass Ave,
 * Cambridge, MA 02139, USA
 *
 * Web developers : http://aion-knight.ru
 * Support of the game client : Aion 2.7- 'Arena of Death' (Innova)
 * The version of the server : Aion-Knight 2.7 (Beta version)
 */

package gameserver.model.gameobjects.stats;

import gameserver.configs.administration.AdminConfig;
import gameserver.model.alliance.PlayerAllianceEvent;
import gameserver.model.gameobjects.player.Player;
import gameserver.model.gameobjects.state.CreatureState;
import gameserver.model.group.GroupEvent;
import gameserver.network.aion.serverpackets.SM_ATTACK_STATUS;
import gameserver.network.aion.serverpackets.SM_ATTACK_STATUS.TYPE;
import gameserver.network.aion.serverpackets.SM_FLY_TIME;
import gameserver.network.aion.serverpackets.SM_STATUPDATE_HP;
import gameserver.network.aion.serverpackets.SM_STATUPDATE_MP;
import gameserver.services.AllianceService;
import gameserver.services.LifeStatsRestoreService;
import gameserver.task.impl.PacketBroadcaster.BroadcastMode;
import gameserver.utils.PacketSendUtility;
import gameserver.world.zone.ZoneName;

import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class PlayerLifeStats extends CreatureLifeStats<Player>
{
	protected int currentFp;
	private final ReentrantLock fpLock = new ReentrantLock();
	
	private Future<?> flyRestoreTask;
	private Future<?> flyReduceTask;
	
	public PlayerLifeStats(Player owner, int currentHp, int currentMp, int currentFp)
	{
		super(owner,currentHp,currentMp);
		this.currentFp = currentFp;
	}

	public PlayerLifeStats(Player owner)
	{
		super(owner, owner.getGameStats().getCurrentStat(StatEnum.MAXHP), owner.getGameStats().getCurrentStat(
			StatEnum.MAXMP));
		this.currentFp = owner.getGameStats().getCurrentStat(StatEnum.FLY_TIME);
	}
	
	@Override
	protected void onReduceHp()
	{
		sendHpPacketUpdate();
		triggerRestoreTask();
		sendGroupPacketUpdate();	
	}

	@Override
	protected void onReduceMp()
	{
		sendMpPacketUpdate();		
		triggerRestoreTask();
		sendGroupPacketUpdate();
	}
	
	@Override
	protected void onIncreaseMp(TYPE type, int value, int skillId, int logId)
	{
		sendMpPacketUpdate();
		sendAttackStatusPacketUpdate(type, value, skillId, logId);
		sendGroupPacketUpdate();
	}	
	
	@Override
	protected void onIncreaseHp(TYPE type, int value, int skillId, int logId)
	{
		if (this.isFullyRestoredHp())
		{
			// FIXME: Temp Fix: Reset aggro list when hp is full.
			this.owner.getAggroList().clear();
		}
		sendHpPacketUpdate();
		sendAttackStatusPacketUpdate(type, value, skillId, logId);
		sendGroupPacketUpdate();
	}
	
	private void sendGroupPacketUpdate()
	{
		Player owner = getOwner();
		if(owner.isInGroup())
			owner.getPlayerGroup().updateGroupUIToEvent(owner, GroupEvent.MOVEMENT);
		if(owner.isInAlliance())
			AllianceService.getInstance().updateAllianceUIToEvent(owner, PlayerAllianceEvent.MOVEMENT);
	}

	@Override
	public Player getOwner()
	{
		return (Player) super.getOwner();
	}

	@Override
	public void restoreHp()
	{
		int currentRegenHp = getOwner().getGameStats().getCurrentStat(StatEnum.REGEN_HP);
		if(getOwner().isInState(CreatureState.RESTING))
			currentRegenHp *= 8;
		increaseHp(TYPE.NATURAL_HP, currentRegenHp);
	}

	@Override
	public void restoreMp()
	{
		int currentRegenMp = getOwner().getGameStats().getCurrentStat(StatEnum.REGEN_MP);
		if(getOwner().isInState(CreatureState.RESTING))
			currentRegenMp *= 8;
		increaseMp(TYPE.NATURAL_MP, currentRegenMp);
	}	

	@Override
	public void synchronizeWithMaxStats()
	{
		if(isAlreadyDead())
			return;
		
		super.synchronizeWithMaxStats();
		int maxFp = getMaxFp();
		if(currentFp != maxFp)
			currentFp = maxFp;
	}
	
	@Override
	public void updateCurrentStats()
	{
		super.updateCurrentStats();
		
		if(getMaxFp() < currentFp)
			currentFp = getMaxFp();

		if(!owner.isInState(CreatureState.FLYING))
			triggerFpRestore();
	}
	
	public void sendHpPacketUpdate()
	{
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_HP_STAT);
	}
	
	public void sendHpPacketUpdateImpl()
	{
		if(owner == null)
			return;

		PacketSendUtility.sendPacket((Player) owner, new SM_STATUPDATE_HP(currentHp, getMaxHp()));
	}
	
	public void sendMpPacketUpdate()
	{
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_MP_STAT);
	}
	
	public void sendMpPacketUpdateImpl()
	{
		if(owner == null)
			return;

		PacketSendUtility.sendPacket((Player) owner, new SM_STATUPDATE_MP(currentMp, getMaxMp()));
	}
	
	/**
	 * 
	 * @return the currentFp
	 */
	@Override
	public int getCurrentFp()
	{
		return this.currentFp;
	}
	
	/**
	 * 
	 * @return maxFp of creature according to stats
	 */
	public int getMaxFp()
	{
		return owner.getGameStats().getCurrentStat(StatEnum.FLY_TIME);
	}
	
	/**	 
	 * @return FP percentage 0 - 100
	 */
	public int getFpPercentage()
	{
		return 100 * currentFp / getMaxFp();
	}
	
	/**
	 * This method is called whenever caller wants to restore creatures's FP
	 * @param value
	 * @return
	 */
	@Override
	public int increaseFp(TYPE type, int value)
	{
		return this.increaseFp(type, value, 0, 170);
	}
	public int increaseFp(TYPE type, int value, int skillId, int logId)
	{
		
		fpLock.lock();

		try
		{
			if(isAlreadyDead())
			{
				return 0;
			}
			int newFp = this.currentFp + value;
			if(newFp > getMaxFp())
			{
				newFp = getMaxFp();
			}
			if(currentFp != newFp)
			{
				this.currentFp = newFp;
			}
		}
		finally
		{
			fpLock.unlock();
		}
		
		onIncreaseFp(type, value, skillId, logId);

		return currentFp;

	}
	
	/**
	 * This method is called whenever caller wants to reduce creatures's MP
	 * 
	 * @param value
	 * @return
	 */
	public int reduceFp(int value)
	{
		fpLock.lock();
		try
		{
			int newFp = this.currentFp - value;

			if(newFp < 0)
				newFp = 0;

			this.currentFp = newFp;	
		}
		finally
		{
			fpLock.unlock();
		}
		
		onReduceFp();

		return currentFp;
	}
	
	public int setCurrentFp(int value)
	{
		fpLock.lock();
		try
		{
			int newFp = value;

			if(newFp < 0)
				newFp = 0;

			this.currentFp = newFp;	
		}
		finally
		{
			fpLock.unlock();
		}
		
		onReduceFp();

		return currentFp;
	}

	protected void onIncreaseFp(TYPE type, int value, int skillId, int logId)
	{
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_FLY_TIME);
		sendAttackStatusPacketUpdate(type, value, skillId, logId);
	}
	
	protected void onReduceFp()
	{
		owner.addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_FLY_TIME);
		if (owner.isInState(CreatureState.FLYING))
			((Player)owner).getFlyController().checkFlightZone();
	}	
	
	public void sendFpPacketUpdateImpl()
	{
		if(owner == null)
			return;
		
		PacketSendUtility.sendPacket((Player) owner, new SM_FLY_TIME(currentFp, getMaxFp()));
	}
	
	/**
	 * this method should be used only on FlyTimeRestoreService
	 */
	public void restoreFp()
	{
		//how much fly time restoring per 2 second.
		increaseFp(TYPE.NATURAL_FP, 1);
	}
	
	public void specialrestoreFp()
	{
			increaseFp(TYPE.NATURAL_FP, owner.getGameStats().getCurrentStat(StatEnum.REGEN_FP)/6);
	}
	
	public void triggerFpRestore()
	{
		cancelFpReduce();
		
		if(flyRestoreTask == null && !alreadyDead && !isFlyTimeFullyRestored())
		{
			this.flyRestoreTask = LifeStatsRestoreService.getInstance().scheduleFpRestoreTask(this);
		}
	}
	
	public void cancelFpRestore()
	{
		if(flyRestoreTask != null && !flyRestoreTask.isCancelled())
		{
			flyRestoreTask.cancel(false);
			this.flyRestoreTask = null;
		}
	}
	
	public void triggerFpReduce(ZoneName currentFlightZoneName)
	{
		cancelFpRestore();
		cancelFpReduce();
		
		if(flyReduceTask == null && !alreadyDead &&
			getOwner().getAccessLevel() < AdminConfig.GM_FLIGHT_UNLIMITED)
		{
			this.flyReduceTask = LifeStatsRestoreService.getInstance().scheduleFpReduceTask(this, currentFlightZoneName);
		}
	}
	
	public void cancelFpReduce()
	{
		if(flyReduceTask != null && !flyReduceTask.isCancelled())
		{
			flyReduceTask.cancel(false);
			this.flyReduceTask = null;
		}
	}
	
	public boolean isFlyTimeFullyRestored()
	{
		return getMaxFp() == currentFp;
	}

	@Override
	public void cancelAllTasks()
	{
		super.cancelAllTasks();
		cancelFpReduce();
		cancelFpRestore();
	}
	
	@Override
	public void triggerRestoreOnRevive()
	{
		super.triggerRestoreOnRevive();
		triggerFpRestore();
	}
	
	@Override
	protected void sendAttackStatusPacketUpdate(TYPE type, int value, int skillId, int logId)
	{
		if(owner == null)
		{
			return;
		}
		PacketSendUtility.broadcastPacket((Player)owner, new SM_ATTACK_STATUS(owner, type, value, skillId, logId), true);	
	}
}

