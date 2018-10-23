// https://searchcode.com/api/result/13354700/

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
package net.l2emuproject.gameserver.entity.status;

import java.util.Set;

import net.l2emuproject.Config;
import net.l2emuproject.gameserver.entity.stat.CharStat;
import net.l2emuproject.gameserver.services.duel.Duel;
import net.l2emuproject.gameserver.skills.L2Skill;
import net.l2emuproject.gameserver.skills.formulas.Formulas;
import net.l2emuproject.gameserver.system.taskmanager.AbstractIterativePeriodicTaskManager;
import net.l2emuproject.gameserver.world.object.L2Character;
import net.l2emuproject.gameserver.world.object.L2Player;
import net.l2emuproject.tools.random.Rnd;
import net.l2emuproject.util.SingletonSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CharStatus
{
	protected static final Log _log = LogFactory.getLog(CharStatus.class);
	
	protected final L2Character _activeChar;
	private final int _period;
	
	private double _currentHp = 0;
	private double _currentMp = 0;
	
	public CharStatus(L2Character activeChar)
	{
		_activeChar = activeChar;
		_period = Formulas.getRegeneratePeriod(_activeChar);
	}
	
	// ========================================================================
	
	protected L2Character getActiveChar()
	{
		return _activeChar;
	}
	
	public final double getCurrentHp()
	{
		return _currentHp;
	}
	
	public final double getCurrentMp()
	{
		return _currentMp;
	}
	
	public double getCurrentCp()
	{
		return 0;
	}
	
	// ========================================================================
	
	public final void setCurrentHpMp(double newHp, double newMp)
	{
		setCurrentHp(newHp);
		setCurrentMp(newMp);
	}
	
	public final void setCurrentHp(double newHp)
	{
		if (getActiveChar().isDead())
			return;
		
		if (setCurrentHp0(newHp))
			startHpMpRegeneration();
	}
	
	protected boolean setCurrentHp0(double newHp)
	{
		double maxHp = getActiveChar().getStat().getMaxHp();
		if (newHp < 0)
			newHp = 0;
		
		boolean requireRegen;
		
		synchronized (this)
		{
			final double currentHp = _currentHp;
			
			if (newHp >= maxHp)
			{
				_currentHp = maxHp;
				requireRegen = false;
			}
			else
			{
				_currentHp = newHp;
				requireRegen = true;
			}
			
			if (currentHp != _currentHp)
				getActiveChar().broadcastStatusUpdate();
		}
		
		return requireRegen;
	}
	
	public final void setCurrentMp(double newMp)
	{
		if (getActiveChar().isDead())
			return;
		
		if (setCurrentMp0(newMp))
			startHpMpRegeneration();
	}
	
	private boolean setCurrentMp0(double newMp)
	{
		double maxMp = getActiveChar().getStat().getMaxMp();
		if (newMp < 0)
			newMp = 0;
		
		boolean requireRegen;
		
		synchronized (this)
		{
			final double currentMp = _currentMp;
			
			if (newMp >= maxMp)
			{
				_currentMp = maxMp;
				requireRegen = false;
			}
			else
			{
				_currentMp = newMp;
				requireRegen = true;
			}
			
			if (currentMp != _currentMp)
				getActiveChar().broadcastStatusUpdate();
		}
		
		return requireRegen;
	}
	
	public final void setCurrentCp(double newCp)
	{
		if (getActiveChar().isDead())
			return;
		
		if (setCurrentCp0(newCp))
			startHpMpRegeneration();
	}
	
	protected boolean setCurrentCp0(double newCp)
	{
		return false;
	}
	
	// ========================================================================
	
	boolean canReduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isConsume)
	{
		if (attacker == null || getActiveChar().isDead())
			return false;
		
		if (getActiveChar().isInvul())
		{
			if (attacker == getActiveChar())
			{
				if (!isDOT && !isConsume)
					return false;
			}
			else
				return false;
		}
		
		L2Player attackerPlayer = attacker.getActingPlayer();
		
		// Additional prevention
		// Check if player is GM and has sufficient rights to make damage
		if (attackerPlayer != null)
			if (attackerPlayer.isGM() && attackerPlayer.getAccessLevel() < Config.GM_CAN_GIVE_DAMAGE)
				return false;
		
		if (Duel.isInvul(attacker, getActiveChar()))
			return false;
		
		return true;
	}
	
	public final void increaseHp(double value)
	{
		setCurrentHp(getCurrentHp() + value);
	}
	
	public final void reduceHp(double value, L2Character attacker)
	{
		reduceHp(value, attacker, true);
	}
	
	/**
	 * @deprecated the last boolean parameter was used for awake/isConsume as well, so this is confusing
	 */
	@Deprecated
	public final void reduceHp(double value, L2Character attacker, boolean awake)
	{
		reduceHp(value, attacker, awake, false, false);
	}
	
	public final boolean reduceHp(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isConsume)
	{
		if (!canReduceHp(value, attacker, awake, isDOT, isConsume))
			return false;
		
		reduceHp0(value, attacker, awake, isDOT, isConsume);
		return true;
	}
	
	public final void reduceHpByDOT(double value, L2Character attacker, L2Skill skill)
	{
		reduceHp(value, attacker, !skill.isToggle(), true, false);
	}
	
	public final void reduceHpByConsume(double value)
	{
		reduceHp(value, getActiveChar(), false, false, true);
	}
	
	void reduceHp0(double value, L2Character attacker, boolean awake, boolean isDOT, boolean isConsume)
	{
		if (!isConsume)
		{
			if (!isDOT)
			{
				if (awake)
				{
					if (getActiveChar().isSleeping())
						getActiveChar().stopSleeping(true);
				}
				
				if (getActiveChar().isStunned() && Rnd.get(10) == 0)
					getActiveChar().stopStunning(true);
				if (getActiveChar().isImmobileUntilAttacked())
					getActiveChar().stopImmobileUntilAttacked(true);
			}
			
			getActiveChar().getEffects().dispelOnAction();
		}
		
		if (value > 0) // Reduce Hp if any, and Hp can't be negative
			setCurrentHp(Math.max(getCurrentHp() - value, 0));
		
		if (getCurrentHp() < 0.5 && getActiveChar().isMortal()) // Die
		{
			getActiveChar().abortAttack();
			getActiveChar().abortCast();
			
			// Start the doDie process
			getActiveChar().doDie(attacker);
		}
	}
	
	public final void increaseMp(double value)
	{
		setCurrentMp(getCurrentMp() + value);
	}
	
	public void reduceMp(double value)
	{
		setCurrentMp(getCurrentMp() - value);
	}
	
	public final void reduceCp(int value)
	{
		setCurrentCp(getCurrentCp() - value);
	}
	
	// ========================================================================
	
	private static final class RegenTaskManager extends AbstractIterativePeriodicTaskManager<CharStatus>
	{
		private static final RegenTaskManager _instance = new RegenTaskManager();
		
		private static RegenTaskManager getInstance()
		{
			return _instance;
		}
		
		private RegenTaskManager()
		{
			super(1000);
		}
		
		@Override
		protected void callTask(CharStatus task)
		{
			if (task.regenTask())
				startTask(task);
			else
				stopTask(task);
		}
		
		@Override
		protected String getCalledMethodName()
		{
			return "regenTask()";
		}
	}
	
	private long _lastRunTime = System.currentTimeMillis();
	
	public final void startHpMpRegeneration()
	{
		if (getActiveChar().isDead())
			return;
		
		RegenTaskManager.getInstance().startTask(this);
	}
	
	public final void stopHpMpRegeneration()
	{
		RegenTaskManager.getInstance().stopTask(this);
	}
	
	public final boolean regenTask()
	{
		if (getActiveChar().isDead())
			return false;
		
		if (System.currentTimeMillis() < _lastRunTime + _period)
			return true;
		
		_lastRunTime = System.currentTimeMillis();
		
		CharStat cs = getActiveChar().getStat();
		
		boolean requireRegen = false;
		
		if (getCurrentHp() < cs.getMaxHp())
			requireRegen |= setCurrentHp0(getCurrentHp() + Formulas.calcHpRegen(getActiveChar()));
		
		if (getCurrentMp() < cs.getMaxMp())
			requireRegen |= setCurrentMp0(getCurrentMp() + Formulas.calcMpRegen(getActiveChar()));
		
		if (getCurrentCp() < cs.getMaxCp())
			requireRegen |= setCurrentCp0(getCurrentCp() + Formulas.calcCpRegen(getActiveChar()));
		
		return requireRegen;
	}
	
	// ========================================================================
	// Status Listeners
	
	private Set<L2Player> _statusListeners;
	
	public final Set<L2Player> getStatusListeners()
	{
		if (_statusListeners == null)
			_statusListeners = new SingletonSet<L2Player>();
		
		return _statusListeners;
	}
	
	public final void addStatusListener(L2Player player)
	{
		synchronized (getStatusListeners())
		{
			if (getActiveChar() != player)
				getStatusListeners().add(player);
		}
	}
	
	public final void removeStatusListener(L2Player player)
	{
		synchronized (getStatusListeners())
		{
			getStatusListeners().remove(player);
		}
	}
}

