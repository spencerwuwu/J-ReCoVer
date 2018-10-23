// https://searchcode.com/api/result/13354592/

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
package net.l2emuproject.gameserver.skills.formulas;

import net.l2emuproject.Config;
import net.l2emuproject.gameserver.dataholders.ClassBalanceDataHolder;
import net.l2emuproject.gameserver.dataholders.ClassBalanceDataHolder.TypeBalance;
import net.l2emuproject.gameserver.entity.base.PlayerState;
import net.l2emuproject.gameserver.entity.itemcontainer.Inventory;
import net.l2emuproject.gameserver.events.global.clanhallsiege.ClanHall;
import net.l2emuproject.gameserver.events.global.clanhallsiege.ClanHallManager;
import net.l2emuproject.gameserver.events.global.fortsiege.Fort;
import net.l2emuproject.gameserver.events.global.fortsiege.FortManager;
import net.l2emuproject.gameserver.events.global.sevensigns.SevenSigns;
import net.l2emuproject.gameserver.events.global.sevensigns.SevenSignsFestival;
import net.l2emuproject.gameserver.events.global.siege.Castle;
import net.l2emuproject.gameserver.events.global.siege.CastleManager;
import net.l2emuproject.gameserver.events.global.siege.L2SiegeClan;
import net.l2emuproject.gameserver.events.global.siege.Siege;
import net.l2emuproject.gameserver.events.global.siege.SiegeManager;
import net.l2emuproject.gameserver.items.L2ItemInstance;
import net.l2emuproject.gameserver.network.SystemMessageId;
import net.l2emuproject.gameserver.skills.Calculator;
import net.l2emuproject.gameserver.skills.Env;
import net.l2emuproject.gameserver.skills.L2Effect;
import net.l2emuproject.gameserver.skills.L2Skill;
import net.l2emuproject.gameserver.skills.Stats;
import net.l2emuproject.gameserver.skills.conditions.ConditionPlayerState;
import net.l2emuproject.gameserver.skills.conditions.ConditionUsingItemType;
import net.l2emuproject.gameserver.skills.funcs.Func;
import net.l2emuproject.gameserver.system.time.GameTimeController;
import net.l2emuproject.gameserver.system.util.Util;
import net.l2emuproject.gameserver.system.util.Util.Direction;
import net.l2emuproject.gameserver.templates.chars.L2PcTemplate;
import net.l2emuproject.gameserver.templates.item.L2Armor;
import net.l2emuproject.gameserver.templates.item.L2Item;
import net.l2emuproject.gameserver.templates.item.L2Weapon;
import net.l2emuproject.gameserver.templates.item.L2WeaponType;
import net.l2emuproject.gameserver.templates.skills.L2SkillType;
import net.l2emuproject.gameserver.world.object.L2Attackable;
import net.l2emuproject.gameserver.world.object.L2Character;
import net.l2emuproject.gameserver.world.object.L2Npc;
import net.l2emuproject.gameserver.world.object.L2Playable;
import net.l2emuproject.gameserver.world.object.L2Player;
import net.l2emuproject.gameserver.world.object.L2Summon;
import net.l2emuproject.gameserver.world.object.instance.L2CubicInstance;
import net.l2emuproject.gameserver.world.object.instance.L2DoorInstance;
import net.l2emuproject.gameserver.world.object.instance.L2GrandBossInstance;
import net.l2emuproject.gameserver.world.object.instance.L2GuardInstance;
import net.l2emuproject.gameserver.world.object.instance.L2PetInstance;
import net.l2emuproject.gameserver.world.zone.L2Zone;
import net.l2emuproject.lang.L2Math;
import net.l2emuproject.tools.random.Rnd;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Global calculations, can be modified by server admins
 */
public final class Formulas
{
	/** Regen Task period */
	protected static final Log		_log							= LogFactory.getLog(L2Character.class);
	private static final int		HP_REGENERATE_PERIOD			= 3000;											// 3 secs

	public static final byte		SHIELD_DEFENSE_FAILED			= 0;												// no shield defense
	public static final byte		SHIELD_DEFENSE_SUCCEED			= 1;												// normal shield defense
	public static final byte		SHIELD_DEFENSE_PERFECT_BLOCK	= 2;												// perfect block

	public static final byte		SKILL_REFLECT_FAILED			= 0;												// no reflect
	public static final byte		SKILL_REFLECT_SUCCEED			= 1;												// normal reflect, some damage reflected some other not
	public static final byte		SKILL_REFLECT_VENGEANCE			= 2;												// 100% of the damage affect both

	private static final byte		MELEE_ATTACK_RANGE				= 40;

	public static int				MAX_STAT_VALUE					= 100;

	private static final double[]	STRCompute						= new double[] { 1.036, 34.845 };					//{1.016, 28.515}; for C1
	private static final double[]	INTCompute						= new double[] { 1.020, 31.375 };					//{1.020, 31.375}; for C1
	private static final double[]	DEXCompute						= new double[] { 1.009, 19.360 };					//{1.009, 19.360}; for C1
	private static final double[]	WITCompute						= new double[] { 1.050, 20.000 };					//{1.050, 20.000}; for C1
	private static final double[]	CONCompute						= new double[] { 1.030, 27.632 };					//{1.015, 12.488}; for C1
	private static final double[]	MENCompute						= new double[] { 1.010, -0.060 };					//{1.010, -0.060}; for C1

	protected static final double[]	WITbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	MENbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	INTbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	STRbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	DEXbonus						= new double[MAX_STAT_VALUE];
	protected static final double[]	CONbonus						= new double[MAX_STAT_VALUE];

	protected static final double[]	sqrtMENbonus					= new double[MAX_STAT_VALUE];
	protected static final double[]	sqrtCONbonus					= new double[MAX_STAT_VALUE];

	// These values are 100% matching retail tables, no need to change and no need add
	// calculation into the stat bonus when accessing (not efficient),
	// better to have everything precalculated and use values directly (saves CPU)
	static
	{
		for (int i = 0; i < STRbonus.length; i++)
			STRbonus[i] = Math.floor(Math.pow(STRCompute[0], i - STRCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < INTbonus.length; i++)
			INTbonus[i] = Math.floor(Math.pow(INTCompute[0], i - INTCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < DEXbonus.length; i++)
			DEXbonus[i] = Math.floor(Math.pow(DEXCompute[0], i - DEXCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < WITbonus.length; i++)
			WITbonus[i] = Math.floor(Math.pow(WITCompute[0], i - WITCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < CONbonus.length; i++)
			CONbonus[i] = Math.floor(Math.pow(CONCompute[0], i - CONCompute[1]) * 100 + .5d) / 100;
		for (int i = 0; i < MENbonus.length; i++)
			MENbonus[i] = Math.floor(Math.pow(MENCompute[0], i - MENCompute[1]) * 100 + .5d) / 100;

		// precompute  square root values
		for (int i = 0; i < sqrtCONbonus.length; i++)
			sqrtCONbonus[i] = Math.sqrt(CONbonus[i]);
		for (int i = 0; i < sqrtMENbonus.length; i++)
			sqrtMENbonus[i] = Math.sqrt(MENbonus[i]);
	}

	static class FuncAddLevel3 extends Func
	{
		static final FuncAddLevel3[]	_instancies	= new FuncAddLevel3[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncAddLevel3(stat);
			return _instancies[pos];
		}

		private FuncAddLevel3(Stats pStat)
		{
			super(pStat, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			env.setValue(env.getValue() + (env.getPlayer().getLevel() / 3.0));
		}
	}

	static class FuncMultLevelMod extends Func
	{
		static final FuncMultLevelMod[]	_instancies	= new FuncMultLevelMod[Stats.NUM_STATS];

		static Func getInstance(Stats stat)
		{

			int pos = stat.ordinal();
			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultLevelMod(stat);
			return _instancies[pos];
		}

		private FuncMultLevelMod(Stats pStat)
		{
			super(pStat, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			env.setValue(env.getValue() * env.getPlayer().getLevelMod());
		}
	}

	static class FuncMultRegenResting extends Func
	{
		static final FuncMultRegenResting[]	_instancies	= new FuncMultRegenResting[Stats.NUM_STATS];

		/**
		 * Return the Func object corresponding to the state concerned.<BR>
		 * <BR>
		 */
		static Func getInstance(Stats stat)
		{
			int pos = stat.ordinal();

			if (_instancies[pos] == null)
				_instancies[pos] = new FuncMultRegenResting(stat);

			return _instancies[pos];
		}

		/**
		 * Constructor of the FuncMultRegenResting.<BR>
		 * <BR>
		 */
		private FuncMultRegenResting(Stats pStat)
		{
			super(pStat, 0x20, null, new ConditionPlayerState(PlayerState.RESTING, true));
		}

		/**
		 * Calculate the modifier of the state concerned.<BR>
		 * <BR>
		 */
		@Override
		public void calc(Env env)
		{
			env.setValue(env.getValue() * 1.45);
		}
	}

	static class FuncPAtkMod extends Func
	{
		static final FuncPAtkMod	_fpa_instance	= new FuncPAtkMod();

		static Func getInstance()
		{
			return _fpa_instance;
		}

		private FuncPAtkMod()
		{
			super(Stats.POWER_ATTACK, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			env.setValue(env.getValue() * (STRbonus[env.getPlayer().getStat().getSTR()] * env.getPlayer().getLevelMod()));
		}
	}

	static class FuncMAtkMod extends Func
	{
		static final FuncMAtkMod	_fma_instance	= new FuncMAtkMod();

		static Func getInstance()
		{
			return _fma_instance;
		}

		private FuncMAtkMod()
		{
			super(Stats.MAGIC_ATTACK, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			double intb = INTbonus[env.getPlayer().getINT()];
			double lvlb = env.getPlayer().getLevelMod();
			env.setValue(env.getValue() * ((lvlb * lvlb) * (intb * intb)));
		}
	}

	static class FuncMDefMod extends Func
	{
		static final FuncMDefMod	_fmm_instance	= new FuncMDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.getPlayer() instanceof L2Player)
			{
				L2Player p = (L2Player) env.getPlayer();
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LFINGER) != null)
					env.setValue(env.getValue() - 5);
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RFINGER) != null)
					env.setValue(env.getValue() - 5);
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEAR) != null)
					env.setValue(env.getValue() - 9);
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_REAR) != null)
					env.setValue(env.getValue() - 9);
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_NECK) != null)
					env.setValue(env.getValue() - 13);
			}
			env.setValue(env.getValue() * (MENbonus[env.getPlayer().getStat().getMEN()] * env.getPlayer().getLevelMod()));
		}
	}

	static class FuncPDefMod extends Func
	{
		static final FuncPDefMod	_fmm_instance	= new FuncPDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.getPlayer() instanceof L2Player)
			{
				L2Player p = (L2Player) env.getPlayer();
				boolean hasMagePDef = p.getClassId().isMage();
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_HEAD) != null)
					env.setValue(env.getValue() - 12);
				L2ItemInstance chest = p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CHEST);
				if (chest != null)
					env.setValue(env.getValue() - (hasMagePDef ? 15 : 31));
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LEGS) != null ||
						(chest != null && chest.getItem().getBodyPart() == L2Item.SLOT_FULL_ARMOR))
					env.setValue(env.getValue() - (hasMagePDef ? 8 : 18));
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_GLOVES) != null)
					env.setValue(env.getValue() - 8);
				if (p.getInventory().getPaperdollItem(Inventory.PAPERDOLL_FEET) != null)
					env.setValue(env.getValue() - 7);
			}
			env.setValue(env.getValue() * env.getPlayer().getLevelMod());
		}
	}

	static class FuncGatesPDefMod extends Func
	{
		static final FuncGatesPDefMod	_fmm_instance	= new FuncGatesPDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncGatesPDefMod()
		{
			super(Stats.POWER_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.setValue(env.getValue() * Config.ALT_SIEGE_DAWN_GATES_PDEF_MULT);
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.setValue(env.getValue() * Config.ALT_SIEGE_DUSK_GATES_PDEF_MULT);
		}
	}

	static class FuncGatesMDefMod extends Func
	{
		static final FuncGatesMDefMod	_fmm_instance	= new FuncGatesMDefMod();

		static Func getInstance()
		{
			return _fmm_instance;
		}

		private FuncGatesMDefMod()
		{
			super(Stats.MAGIC_DEFENCE, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DAWN)
				env.setValue(env.getValue() * Config.ALT_SIEGE_DAWN_GATES_MDEF_MULT);
			else if (SevenSigns.getInstance().getSealOwner(SevenSigns.SEAL_STRIFE) == SevenSigns.CABAL_DUSK)
				env.setValue(env.getValue() * Config.ALT_SIEGE_DUSK_GATES_MDEF_MULT);
		}
	}

	static class FuncBowAtkRange extends Func
	{
		private static final FuncBowAtkRange	_fbarInstance	= new FuncBowAtkRange();

		static Func getInstance()
		{
			return _fbarInstance;
		}

		private FuncBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null, new ConditionUsingItemType(L2WeaponType.BOW.mask()));
		}

		@Override
		public void calc(Env env)
		{
			// default is 40 and with bow should be 500
			env.setValue(env.getValue() + 460);
		}
	}

	static class FuncCrossBowAtkRange extends Func
	{
		private static final FuncCrossBowAtkRange	_fcb_instance	= new FuncCrossBowAtkRange();

		static Func getInstance()
		{
			return _fcb_instance;
		}

		private FuncCrossBowAtkRange()
		{
			super(Stats.POWER_ATTACK_RANGE, 0x10, null, new ConditionUsingItemType(L2WeaponType.CROSSBOW.mask()));
		}

		@Override
		public void calc(Env env)
		{
			// default is 40 and with crossbow should be 400
			env.setValue(env.getValue() + 360);
		}
	}

	static class FuncAtkAccuracy extends Func
	{
		static final FuncAtkAccuracy	_faaInstance	= new FuncAtkAccuracy();

		static Func getInstance()
		{
			return _faaInstance;
		}

		private FuncAtkAccuracy()
		{
			super(Stats.ACCURACY_COMBAT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.getPlayer();
			//[Square(DEX)]*6 + lvl + weapon hitbonus;
			env.setValue(env.getValue() + (Math.sqrt(p.getStat().getDEX()) * 6));
			env.setValue(env.getValue() + p.getLevel());
			if (p instanceof L2Summon)
				env.setValue(env.getValue() + ((p.getLevel() < 60) ? 4 : 5));
			if (p.getLevel() > 77)
				env.setValue(env.getValue() + (p.getLevel() - 77));
			if (p.getLevel() > 69)
				env.setValue(env.getValue() + (p.getLevel() - 69));
		}
	}

	static class FuncAtkEvasion extends Func
	{
		static final FuncAtkEvasion	_faeInstance	= new FuncAtkEvasion();

		static Func getInstance()
		{
			return _faeInstance;
		}

		private FuncAtkEvasion()
		{
			super(Stats.EVASION_RATE, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.getPlayer();
			//[Square(DEX)]*6 + lvl;
			env.setValue(env.getValue() + (Math.sqrt(p.getStat().getDEX()) * 6));
			env.setValue(env.getValue() + p.getLevel());
			if (p.getLevel() > 77)
				env.setValue(env.getValue() + (p.getLevel() - 77));
			if (p.getLevel() > 69)
				env.setValue(env.getValue() + (p.getLevel() - 69));
		}
	}

	static class FuncAtkCritical extends Func
	{
		static final FuncAtkCritical	_facInstance	= new FuncAtkCritical();

		static Func getInstance()
		{
			return _facInstance;
		}

		private FuncAtkCritical()
		{
			super(Stats.CRITICAL_RATE, 0x09, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Character p = env.getPlayer();
			if (p instanceof L2Summon)
				env.setValue(40);
			else if (p instanceof L2Player && p.getActiveWeaponInstance() == null)
				env.setValue(40);
			else
			{
				env.setValue(env.getValue() * DEXbonus[p.getStat().getDEX()]);
				env.setValue(env.getValue() * 10);
			}
		}
	}

	static class FuncMAtkCritical extends Func
	{
		static final FuncMAtkCritical	_fac_instance	= new FuncMAtkCritical();

		static Func getInstance()
		{
			return _fac_instance;
		}

		private FuncMAtkCritical()
		{
			super(Stats.MCRITICAL_RATE, 0x29 /*guess, but must be before 0x30*/, null);
		}

		@Override
		public void calc(Env env)
		{
			if (env.getPlayer() instanceof L2Summon)
				env.setValue(8); // TODO: needs retail value
			else if (env.getPlayer() instanceof L2Player && env.getPlayer().getActiveWeaponInstance() != null)
				env.setValue(env.getValue() * WITbonus[env.getPlayer().getStat().getWIT()]);
		}
	}

	static class FuncMoveSpeed extends Func
	{
		static final FuncMoveSpeed	_fmsInstance	= new FuncMoveSpeed();

		static Func getInstance()
		{
			return _fmsInstance;
		}

		private FuncMoveSpeed()
		{
			super(Stats.RUN_SPEED, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * DEXbonus[p.getStat().getDEX()]);
		}
	}

	static class FuncPAtkSpeed extends Func
	{
		static final FuncPAtkSpeed	_fasInstance	= new FuncPAtkSpeed();

		static Func getInstance()
		{
			return _fasInstance;
		}

		private FuncPAtkSpeed()
		{
			super(Stats.POWER_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * DEXbonus[p.getStat().getDEX()]);
		}
	}

	static class FuncMAtkSpeed extends Func
	{
		static final FuncMAtkSpeed	_fasInstance	= new FuncMAtkSpeed();

		static Func getInstance()
		{
			return _fasInstance;
		}

		private FuncMAtkSpeed()
		{
			super(Stats.MAGIC_ATTACK_SPEED, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * WITbonus[p.getStat().getWIT()]);
		}
	}

	static class FuncMaxLoad extends Func
	{
		static final FuncMaxLoad	_fmsInstance	= new FuncMaxLoad();

		static Func getInstance()
		{
			return _fmsInstance;
		}

		private FuncMaxLoad()
		{
			super(Stats.MAX_LOAD, 0x30, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * CONbonus[p.getStat().getCON()]);
		}
	}

	static class FuncHennaSTR extends Func
	{
		static final FuncHennaSTR	_fhInstance	= new FuncHennaSTR();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaSTR()
		{
			super(Stats.STAT_STR, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatSTR());
		}
	}

	static class FuncHennaDEX extends Func
	{
		static final FuncHennaDEX	_fhInstance	= new FuncHennaDEX();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaDEX()
		{
			super(Stats.STAT_DEX, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatDEX());
		}
	}

	static class FuncHennaINT extends Func
	{
		static final FuncHennaINT	_fhInstance	= new FuncHennaINT();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaINT()
		{
			super(Stats.STAT_INT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatINT());
		}
	}

	static class FuncHennaMEN extends Func
	{
		static final FuncHennaMEN	_fhInstance	= new FuncHennaMEN();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaMEN()
		{
			super(Stats.STAT_MEN, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatMEN());
		}
	}

	static class FuncHennaCON extends Func
	{
		static final FuncHennaCON	_fhInstance	= new FuncHennaCON();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaCON()
		{
			super(Stats.STAT_CON, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatCON());
		}
	}

	static class FuncHennaWIT extends Func
	{
		static final FuncHennaWIT	_fhInstance	= new FuncHennaWIT();

		static Func getInstance()
		{
			return _fhInstance;
		}

		private FuncHennaWIT()
		{
			super(Stats.STAT_WIT, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			//          L2PcTemplate t = (L2PcTemplate)env._player.getTemplate();
			L2Player pc = (L2Player) env.getPlayer();
			if (pc != null)
				env.setValue(env.getValue() + pc.getPlayerHenna().getHennaStatWIT());
		}
	}

	static class FuncMaxHpAdd extends Func
	{
		static final FuncMaxHpAdd	_fmhaInstance	= new FuncMaxHpAdd();

		static Func getInstance()
		{
			return _fmhaInstance;
		}

		private FuncMaxHpAdd()
		{
			super(Stats.MAX_HP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.getPlayer().getTemplate();
			int lvl = env.getPlayer().getLevel() - t.getClassBaseLevel();
			double hpmod = t.getLvlHpMod() * lvl;
			double hpmax = (t.getLvlHpAdd() + hpmod) * lvl;
			double hpmin = (t.getLvlHpAdd() * lvl) + hpmod;
			env.setValue(env.getValue() + ((hpmax + hpmin) / 2));
		}
	}

	static class FuncMaxHpMul extends Func
	{
		static final FuncMaxHpMul	_fmhmInstance	= new FuncMaxHpMul();

		static Func getInstance()
		{
			return _fmhmInstance;
		}

		private FuncMaxHpMul()
		{
			super(Stats.MAX_HP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * CONbonus[p.getStat().getCON()]);
		}
	}

	static class FuncMaxCpAdd extends Func
	{
		static final FuncMaxCpAdd	_fmcaInstance	= new FuncMaxCpAdd();

		static Func getInstance()
		{
			return _fmcaInstance;
		}

		private FuncMaxCpAdd()
		{
			super(Stats.MAX_CP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.getPlayer().getTemplate();
			int lvl = env.getPlayer().getLevel() - t.getClassBaseLevel();
			double cpmod = t.getLvlCpMod() * lvl;
			double cpmax = (t.getLvlCpAdd() + cpmod) * lvl;
			double cpmin = (t.getLvlCpAdd() * lvl) + cpmod;
			env.setValue(env.getValue() + ((cpmax + cpmin) / 2));
		}
	}

	static class FuncMaxCpMul extends Func
	{
		static final FuncMaxCpMul	_fmcmInstance	= new FuncMaxCpMul();

		static Func getInstance()
		{
			return _fmcmInstance;
		}

		private FuncMaxCpMul()
		{
			super(Stats.MAX_CP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * CONbonus[p.getStat().getCON()]);
		}
	}

	static class FuncMaxMpAdd extends Func
	{
		static final FuncMaxMpAdd	_fmmaInstance	= new FuncMaxMpAdd();

		static Func getInstance()
		{
			return _fmmaInstance;
		}

		private FuncMaxMpAdd()
		{
			super(Stats.MAX_MP, 0x10, null);
		}

		@Override
		public void calc(Env env)
		{
			L2PcTemplate t = (L2PcTemplate) env.getPlayer().getTemplate();
			int lvl = env.getPlayer().getLevel() - t.getClassBaseLevel();
			double mpmod = t.getLvlMpMod() * lvl;
			double mpmax = (t.getLvlMpAdd() + mpmod) * lvl;
			double mpmin = (t.getLvlMpAdd() * lvl) + mpmod;
			env.setValue(env.getValue() + ((mpmax + mpmin) / 2));
		}
	}

	static class FuncMaxMpMul extends Func
	{
		static final FuncMaxMpMul	_fmmmInstance	= new FuncMaxMpMul();

		static Func getInstance()
		{
			return _fmmmInstance;
		}

		private FuncMaxMpMul()
		{
			super(Stats.MAX_MP, 0x20, null);
		}

		@Override
		public void calc(Env env)
		{
			L2Player p = (L2Player) env.getPlayer();
			env.setValue(env.getValue() * MENbonus[p.getStat().getMEN()]);
		}
	}

	/**
	 * Return the period between 2 regenerations task (3s for L2Character, 5 min
	 * for L2DoorInstance).<BR>
	 * <BR>
	 */
	public static int getRegeneratePeriod(L2Character cha)
	{
		if (cha instanceof L2DoorInstance)
			return HP_REGENERATE_PERIOD * 100; // 5 mins

		return HP_REGENERATE_PERIOD; // 3s
	}

	/**
	 * Return the standard NPC Calculator set containing ACCURACY_COMBAT and
	 * EVASION_RATE.<BR>
	 * <BR>
	 * 
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of
	 * a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each
	 * calculator is a table of Func object in which each Func represents a
	 * mathematic function : <BR>
	 * <BR>
	 * 
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * 
	 * To reduce cache memory use, L2Npcs who don't have skills share the same
	 * Calculator set called <B>NPC_STD_CALCULATOR</B>.<BR>
	 * <BR>
	 * 
	 */
	public static Calculator[] getStdNPCCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		return std;
	}

	public static Calculator[] getStdDoorCalculators()
	{
		Calculator[] std = new Calculator[Stats.NUM_STATS];

		// Add the FuncAtkAccuracy to the Standard Calculator of ACCURACY_COMBAT
		std[Stats.ACCURACY_COMBAT.ordinal()] = new Calculator();
		std[Stats.ACCURACY_COMBAT.ordinal()].addFunc(FuncAtkAccuracy.getInstance());

		// Add the FuncAtkEvasion to the Standard Calculator of EVASION_RATE
		std[Stats.EVASION_RATE.ordinal()] = new Calculator();
		std[Stats.EVASION_RATE.ordinal()].addFunc(FuncAtkEvasion.getInstance());

		//SevenSigns PDEF Modifier
		std[Stats.POWER_DEFENCE.ordinal()] = new Calculator();
		std[Stats.POWER_DEFENCE.ordinal()].addFunc(FuncGatesPDefMod.getInstance());

		//SevenSigns MDEF Modifier
		std[Stats.MAGIC_DEFENCE.ordinal()] = new Calculator();
		std[Stats.MAGIC_DEFENCE.ordinal()].addFunc(FuncGatesMDefMod.getInstance());

		return std;
	}

	/**
	 * Add basics Func objects to L2Player and L2Summon.<BR>
	 * <BR>
	 * 
	 * <B><U> Concept</U> :</B><BR>
	 * <BR>
	 * A calculator is created to manage and dynamically calculate the effect of
	 * a character property (ex : MAX_HP, REGENERATE_HP_RATE...). In fact, each
	 * calculator is a table of Func object in which each Func represents a
	 * mathematic function : <BR>
	 * <BR>
	 * 
	 * FuncAtkAccuracy -> Math.sqrt(_player.getDEX())*6+_player.getLevel()<BR>
	 * <BR>
	 * 
	 * @param cha L2Player or L2Summon that must obtain basic Func objects
	 */
	public static void addFuncsToNewCharacter(L2Character cha)
	{
		if (cha instanceof L2Player)
		{
			cha.addStatFunc(FuncMaxHpAdd.getInstance());
			cha.addStatFunc(FuncMaxHpMul.getInstance());
			cha.addStatFunc(FuncMaxCpAdd.getInstance());
			cha.addStatFunc(FuncMaxCpMul.getInstance());
			cha.addStatFunc(FuncMaxMpAdd.getInstance());
			cha.addStatFunc(FuncMaxMpMul.getInstance());
			//cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			//cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_CP_RATE));
			//cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncBowAtkRange.getInstance());
			cha.addStatFunc(FuncCrossBowAtkRange.getInstance());
			//cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_ATTACK));
			//cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.POWER_DEFENCE));
			//cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAGIC_DEFENCE));
			if (Config.LEVEL_ADD_LOAD)
				cha.addStatFunc(FuncMultLevelMod.getInstance(Stats.MAX_LOAD));
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
			cha.addStatFunc(FuncPAtkSpeed.getInstance());
			cha.addStatFunc(FuncMAtkSpeed.getInstance());
			cha.addStatFunc(FuncMoveSpeed.getInstance());
			cha.addStatFunc(FuncMaxLoad.getInstance());

			cha.addStatFunc(FuncHennaSTR.getInstance());
			cha.addStatFunc(FuncHennaDEX.getInstance());
			cha.addStatFunc(FuncHennaINT.getInstance());
			cha.addStatFunc(FuncHennaMEN.getInstance());
			cha.addStatFunc(FuncHennaCON.getInstance());
			cha.addStatFunc(FuncHennaWIT.getInstance());
		}
		else if (cha instanceof L2PetInstance)
		{
			cha.addStatFunc(FuncPAtkMod.getInstance());
			cha.addStatFunc(FuncMAtkMod.getInstance());
			cha.addStatFunc(FuncPDefMod.getInstance());
			cha.addStatFunc(FuncMDefMod.getInstance());
		}
		else if (cha instanceof L2Summon)
		{
			//cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_HP_RATE));
			//cha.addStatFunc(FuncMultRegenResting.getInstance(Stats.REGENERATE_MP_RATE));
			cha.addStatFunc(FuncAtkCritical.getInstance());
			cha.addStatFunc(FuncMAtkCritical.getInstance());
			cha.addStatFunc(FuncAtkAccuracy.getInstance());
			cha.addStatFunc(FuncAtkEvasion.getInstance());
		}

	}

	/**
	 * Calculate the HP regen rate (base + modifiers).<BR>
	 * <BR>
	 */
	public static final double calcHpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		double hpRegenMultiplier;
		double hpRegenBonus = 0;

		if (cha.isRaid())
			hpRegenMultiplier = Config.RAID_HP_REGEN_MULTIPLIER;
		else if (cha instanceof L2Player)
			hpRegenMultiplier = Config.PLAYER_HP_REGEN_MULTIPLIER;
		else
			hpRegenMultiplier = Config.NPC_HP_REGEN_MULTIPLIER;

		if (cha.isChampion())
			hpRegenMultiplier *= Config.CHAMPION_HP_REGEN;

		// The recovery power of Zaken decreases under sunlight.
		// The recovery power of Zaken increases during night.
		if (cha instanceof L2GrandBossInstance)
		{
			L2GrandBossInstance boss = (L2GrandBossInstance) cha;
			if (boss.getNpcId() == 29022)
			{
				if (boss.isInsideZone(L2Zone.FLAG_SUNLIGHTROOM))
					hpRegenMultiplier *= 0.75;
				else if (GameTimeController.getInstance().isNowNight())
					hpRegenMultiplier *= 1.75;
			}
		}

		if (cha instanceof L2Player)
		{
			L2Player player = (L2Player) cha;

			// Calculate correct baseHpReg value for certain level of PC
			if (player.getLevel() >= 71)
				init = 8.5;
			else if (player.getLevel() >= 61)
				init = 7.5;
			else if (player.getLevel() >= 51)
				init = 6.5;
			else if (player.getLevel() >= 41)
				init = 5.5;
			else if (player.getLevel() >= 31)
				init = 4.5;
			else if (player.getLevel() >= 21)
				init = 3.5;
			else if (player.getLevel() >= 11)
				init = 2.5;
			else
				init = 2.0;

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				init *= calcFestivalRegenModifier(player);
			else
			{
				double siegeModifier = calcSiegeRegenModifer(player);
				if (siegeModifier > 0)
					init *= siegeModifier;
			}

			if (player.isInsideZone(L2Zone.FLAG_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(L2Zone.FLAG_MOTHERTREE))
				hpRegenBonus += 2;

			if (player.isInsideZone(L2Zone.FLAG_CASTLE) && player.getClan() != null)
			{
				int castleIndex = player.getClan().getHasCastle();
				if (castleIndex > 0)
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
						if (castle.getFunction(Castle.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + (double) castle.getFunction(Castle.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			if (player.isInsideZone(L2Zone.FLAG_FORT) && player.getClan() != null)
			{
				int fortIndex = player.getClan().getHasFort();
				if (fortIndex > 0)
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
						if (fort.getFunction(Fort.FUNC_RESTORE_HP) != null)
							hpRegenMultiplier *= 1 + (double) fort.getFunction(Fort.FUNC_RESTORE_HP).getLvl() / 100;
				}
			}

			// Calculate Movement bonus
			if (player.isSitting() && player.getLevel() < 41) // Sitting below lvl 40
			{
				init *= 1.5;
				hpRegenBonus += (40 - player.getLevel()) * 0.7;
			}
			else if (player.isSitting())
				init *= 2.5; // Sitting
			else if (player.isRunning())
				init *= 0.7; // Running
			else if (player.isMoving())
				init *= 1.1; // Walking
			else
				init *= 1.5; // Staying

			// Add CON bonus
			init *= cha.getLevelMod() * CONbonus[cha.getStat().getCON()];
		}
		else if (cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getPetData().getPetRegenHP();

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_HP_RATE, init, null, null) * hpRegenMultiplier + hpRegenBonus;
	}

	/**
	 * Calculate the MP regen rate (base + modifiers).<BR>
	 * <BR>
	 */
	public static final double calcMpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseMpReg();
		double mpRegenMultiplier;
		double mpRegenBonus = 0;

		if (cha.isRaid())
			mpRegenMultiplier = Config.RAID_MP_REGEN_MULTIPLIER;
		else if (cha instanceof L2Player)
			mpRegenMultiplier = Config.PLAYER_MP_REGEN_MULTIPLIER;
		else
			mpRegenMultiplier = Config.NPC_MP_REGEN_MULTIPLIER;

		if (cha instanceof L2Player)
		{
			L2Player player = (L2Player) cha;

			// Calculate correct baseMpReg value for certain level of PC
			if (player.getLevel() >= 71)
				init = 3.0;
			else if (player.getLevel() >= 61)
				init = 2.7;
			else if (player.getLevel() >= 51)
				init = 2.4;
			else if (player.getLevel() >= 41)
				init = 2.1;
			else if (player.getLevel() >= 31)
				init = 1.8;
			else if (player.getLevel() >= 21)
				init = 1.5;
			else if (player.getLevel() >= 11)
				init = 1.2;
			else
				init = 0.9;

			// SevenSigns Festival modifier
			if (SevenSignsFestival.getInstance().isFestivalInProgress() && player.isFestivalParticipant())
				init *= calcFestivalRegenModifier(player);

			// Mother Tree effect is calculated at last
			if (player.isInsideZone(L2Zone.FLAG_MOTHERTREE))
				mpRegenBonus += 2;

			if (player.isInsideZone(L2Zone.FLAG_CASTLE) && player.getClan() != null)
			{
				int castleIndex = player.getClan().getHasCastle();
				if (castleIndex > 0)
				{
					Castle castle = CastleManager.getInstance().getCastleById(castleIndex);
					if (castle != null)
						if (castle.getFunction(Castle.FUNC_RESTORE_MP) != null)
							mpRegenMultiplier *= 1 + (double) castle.getFunction(Castle.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			if (player.isInsideZone(L2Zone.FLAG_FORT) && player.getClan() != null)
			{
				int fortIndex = player.getClan().getHasFort();
				if (fortIndex > 0)
				{
					Fort fort = FortManager.getInstance().getFortById(fortIndex);
					if (fort != null)
						if (fort.getFunction(Fort.FUNC_RESTORE_MP) != null)
							mpRegenMultiplier *= 1 + (double) fort.getFunction(Fort.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			if (player.isInsideZone(L2Zone.FLAG_CLANHALL) && player.getClan() != null)
			{
				int clanHallIndex = player.getClan().getHasHideout();
				if (clanHallIndex > 0)
				{
					ClanHall clansHall = ClanHallManager.getInstance().getClanHallById(clanHallIndex);
					if (clansHall != null)
						if (clansHall.getFunction(ClanHall.FUNC_RESTORE_MP) != null)
							mpRegenMultiplier *= 1 + (double) clansHall.getFunction(ClanHall.FUNC_RESTORE_MP).getLvl() / 100;
				}
			}

			// Calculate Movement bonus
			if (player.isSitting())
				init *= 2.5; // Sitting.
			else if (player.isRunning())
				init *= 0.7; // Running
			else if (player.isMoving())
				init *= 1.1; // Walking
			else
				init *= 1.5; // Staying

			// Add MEN bonus
			init *= cha.getLevelMod() * MENbonus[cha.getStat().getMEN()];
		}
		else if (cha instanceof L2PetInstance)
			init = ((L2PetInstance) cha).getPetData().getPetRegenMP();

		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_MP_RATE, init, null, null) * mpRegenMultiplier + mpRegenBonus;
	}

	/**
	 * Calculate the CP regen rate (base + modifiers).<BR>
	 * <BR>
	 */
	public static final double calcCpRegen(L2Character cha)
	{
		double init = cha.getTemplate().getBaseHpReg();
		double cpRegenMultiplier = Config.PLAYER_CP_REGEN_MULTIPLIER;
		double cpRegenBonus = 0;

		if (cha instanceof L2Player)
		{
			L2Player player = (L2Player) cha;

			// Calculate correct baseHpReg value for certain level of PC
			init += (player.getLevel() > 10) ? ((player.getLevel() - 1) / 10.0) : 0.5;

			// Calculate Movement bonus
			if (player.isSitting())
				init *= 1.5; // Sitting
			else if (!player.isMoving())
				init *= 1.1; // Staying
			else if (player.isRunning())
				init *= 0.7; // Running
		}
		else
		{
			// Calculate Movement bonus
			if (!cha.isMoving())
				init *= 1.1; // Staying
			else if (cha.isRunning())
				init *= 0.7; // Running
		}

		// Apply CON bonus
		init *= cha.getLevelMod() * CONbonus[cha.getStat().getCON()];
		if (init < 1)
			init = 1;

		return cha.calcStat(Stats.REGENERATE_CP_RATE, init, null, null) * cpRegenMultiplier + cpRegenBonus;
	}

	@SuppressWarnings("deprecation")
	public static final double calcFestivalRegenModifier(L2Player activeChar)
	{
		final int[] festivalInfo = SevenSignsFestival.getInstance().getFestivalForPlayer(activeChar);
		final int oracle = festivalInfo[0];
		final int festivalId = festivalInfo[1];
		int[] festivalCenter;

		// If the player isn't found in the festival, leave the regen rate as it is.
		if (festivalId < 0)
			return 0;

		// Retrieve the X and Y coords for the center of the festival arena the player is in.
		if (oracle == SevenSigns.CABAL_DAWN)
			festivalCenter = SevenSignsFestival.FESTIVAL_DAWN_PLAYER_SPAWNS[festivalId];
		else
			festivalCenter = SevenSignsFestival.FESTIVAL_DUSK_PLAYER_SPAWNS[festivalId];

		// Check the distance between the player and the player spawn point, in the center of the arena.
		double distToCenter = activeChar.getDistance(festivalCenter[0], festivalCenter[1]);

		if (_log.isDebugEnabled())
			_log.info("Distance: " + distToCenter + ", RegenMulti: " + (distToCenter * 2.5) / 50);

		return 1.0 - (distToCenter * 0.0005); // Maximum Decreased Regen of ~ -65%;
	}

	public static final double calcSiegeRegenModifer(L2Player activeChar)
	{
		if (activeChar == null || activeChar.getClan() == null)
			return 0;

		Siege siege = SiegeManager.getInstance().getSiege(activeChar);
		if (siege == null || !siege.getIsInProgress())
			return 0;

		L2SiegeClan siegeClan = siege.getAttackerClan(activeChar.getClan().getClanId());
		if (siegeClan == null || siegeClan.getFlag().isEmpty() || !Util.checkIfInRange(200, activeChar, siegeClan.getFlag().getFirst(), true))
			return 0;

		return 1.5; // If all is true, then modifer will be 50% more
	}
	
	/**
	 * Calculated damage caused by ATTACK of attacker on target, called
	 * separatly for each weapon, if dual-weapon is used.
	 * 
	 * @param attacker player or NPC that makes ATTACK
	 * @param target player or NPC, target of ATTACK
	 * @param skill
	 * @param shld one of ATTACK_XXX constants
	 * @param crit if the ATTACK have critical success
	 * @param dual if dual weapon is used
	 * @param ss if weapon item was charged by soulshot
	 * @return damage points
	 */
	public static final double calcPhysDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean crit, boolean ss)
	{
		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		final boolean isPvE = (attacker instanceof L2Playable) && (target instanceof L2Attackable);
		boolean transformed = false;
		if (attacker instanceof L2Player)
		{
			L2Player pcInst = (L2Player) attacker;
			if (pcInst.isGM() && pcInst.getAccessLevel() < Config.GM_CAN_GIVE_DAMAGE)
				return 0;
			transformed = pcInst.getPlayerTransformation().isTransformed();
		}
		
		double damage = attacker.getPAtk(target);
		damage += calcValakasAttribute(attacker, target, skill);
		// apply SS boost to pAtk
		if (ss)
			damage *= 2;
		
		if (skill != null)
		{
			double skillPower = skill.getPower(attacker, target, isPvP, isPvE);
			if (attacker instanceof L2Playable && target instanceof L2Playable)
				skillPower *= skill.getPvpPowerMultiplier();
			float ssBoost = skill.getSSBoost();
			// apply SS boost to skill
			if (ss && ssBoost > 0)
				skillPower *= ssBoost;
			
			damage += skillPower;
		}
		
		double defence = target.getPDef(attacker);
		
		// Defense bonuses in PvP fight
		if(isPvP)
		{
			if(skill == null)
				defence *= target.calcStat(Stats.PVP_PHYSICAL_DEF, 1, null, null);
			else
				defence *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}
		
		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				if (!Config.ALT_GAME_SHIELD_BLOCKS)
					defence += target.getShldDef();
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1.;
		}
		
		if (crit)
		{
			//Finally retail like formula
			damage *= 2 * attacker.calcStat(Stats.CRITICAL_DAMAGE, 1, target, skill) * target.calcStat(Stats.CRIT_VULN, target.getTemplate().getBaseCritVuln(), target, skill);
			//Crit dmg add is almost useless in normal hits...
			if (skill != null && skill.getSkillType() == L2SkillType.BLOW)
				damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill) * 6.5;
			else
				damage += attacker.calcStat(Stats.CRITICAL_DAMAGE_ADD, 0, target, skill);
		}
		
		damage *= 70. / defence;
		
		// In C5 summons make 10 % less dmg in PvP.
		if (attacker instanceof L2Summon && target instanceof L2Player)
			damage *= 0.9;
		
		// defence modifier depending of the attacker weapon
		L2Weapon weapon = attacker.getActiveWeaponItem();
		
		double randomDamageMulti = 0.1;
		
		if (weapon != null)
		{
			randomDamageMulti = weapon.getRandomDamage() / 100.0;
			
			// defence modifier depending of the attacker weapon
			Stats stat = !transformed ? weapon.getItemType().getStat() : null;
			if (stat != null)
			{
				// get the vulnerability due to skills (buffs, passives, toggles, etc)
				damage = target.calcStat(stat, damage, target, null);
				
				if (target instanceof L2Npc)
				{
					// get the natural vulnerability for the template
					damage *= ((L2Npc)target).getTemplate().getVulnerability(stat);
				}
			}
		}
		
		// +/- 5..20%
		damage *= 1 + (2 * Rnd.nextDouble() - 1) * randomDamageMulti;
		
		if (shld > 0 && Config.ALT_GAME_SHIELD_BLOCKS)
		{
			damage -= target.getShldDef();
			if (damage < 0)
				damage = 0;
		}
		
		if (target instanceof L2Npc)
		{
			Stats stat = ((L2Npc)target).getTemplate().getRace().getOffensiveStat();
			
			if (stat != null)
				damage *= attacker.getStat().getMul(stat, target);
		}
		
		if (attacker instanceof L2Npc)
		{
			Stats stat = ((L2Npc)attacker).getTemplate().getRace().getDefensiveStat();
			
			if (stat != null)
				damage /= target.getStat().getMul(stat, attacker);
		}
		
		if (damage > 0 && damage < 1)
		{
			damage = 1;
		}
		else if (damage < 0)
		{
			damage = 0;
		}
		
		// +20% damage from behind attacks, +5% from side attacks
		damage *= calcPositionRate(attacker, target);
		
		// Physical skill dmg boost
		if (skill != null)
			damage *= attacker.calcStat(Stats.PHYSICAL_SKILL_POWER, 1, null, null);
	
		return calcDamage(attacker, target, damage, skill, TypeDam.PHYS);
	}

	public static final double calcMagicDam(L2CubicInstance attacker, L2Character target, L2Skill skill, boolean mcrit, byte shld)
	{
		final boolean isPvP = (target instanceof L2Playable);
		final boolean isPvE = (target instanceof L2Attackable);
		// double mAtk = attacker.getMAtk();
		double mDef = target.getMDef(attacker.getOwner(), skill);

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		double damage = 91 /* * Math.sqrt(mAtk)*/ / mDef * skill.getPower(isPvP, isPvE);
		L2Player owner = attacker.getOwner();
		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(owner, target, skill))
		{
			if (calcMagicSuccess(owner, target, skill) && getMagicLevelDifference(attacker.getOwner(), target, skill) >= -9)
			{
				owner.sendResistedMyMagicSlightlyMessage(target);
				damage /= 2;
			}
			else
			{
				owner.sendResistedMyMagicMessage(target);
				if (mcrit)
					damage = 1;
				else
					damage = Rnd.nextBoolean() ? 1 : 0;
			}
		}

		if (mcrit)
		{
			double mCritModifier = 1.0000000;
			if (target instanceof L2Playable)
				mCritModifier = owner.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, Config.ALT_MCRIT_PVP_RATE, target, skill);
			else
				mCritModifier = owner.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, Config.ALT_MCRIT_RATE, target, skill);
			damage *= mCritModifier;
		}

		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		
		if (target instanceof L2Attackable)
		{
			damage *= owner.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
		}

		return calcDamage(owner, target, damage, skill);
	}

	public static final double calcMagicDam(L2Character attacker, L2Character target, L2Skill skill, byte shld, boolean ss, boolean bss, boolean mcrit)
	{
		final boolean isPvP = (attacker instanceof L2Playable) && (target instanceof L2Playable);
		final boolean isPvE = (attacker instanceof L2Playable) && (target instanceof L2Attackable);
		double mAtk = attacker.getMAtk(target, skill);
		double mDef = target.getMDef(attacker, skill);
		
		// PvP bonuses for defense
		if (isPvP)
		{
			if(skill.isMagic())
				mDef *= target.calcStat(Stats.PVP_MAGICAL_DEF, 1, null, null);
			else
				mDef *= target.calcStat(Stats.PVP_PHYS_SKILL_DEF, 1, null, null);
		}

		switch (shld)
		{
			case SHIELD_DEFENSE_SUCCEED:
				mDef += target.getShldDef(); // kamael
				break;
			case SHIELD_DEFENSE_PERFECT_BLOCK: // perfect block
				return 1;
		}

		if (bss)
			mAtk *= 4;
		else if (ss)
			mAtk *= 2;

		double damage = 91 * Math.sqrt(mAtk) / mDef * skill.getPower(attacker, target, isPvP, isPvE);

		// In C5 summons make 10 % less dmg in PvP.
		if (attacker instanceof L2Summon && target instanceof L2Player)
			damage *= 0.9;

		// Failure calculation
		if (Config.ALT_GAME_MAGICFAILURES && !calcMagicSuccess(attacker, target, skill))
		{
			if (attacker instanceof L2Playable)
			{
				L2Player attOwner = attacker.getActingPlayer();
				if (calcMagicSuccess(attacker, target, skill) && getMagicLevelDifference(attacker, target, skill) >= -9)
				{
					// ~1/10 - weak resist
					attOwner.sendResistedMyMagicSlightlyMessage(target);
					damage /= 2;
				}
				else // retail message & dmg, verified
				{
					attOwner.sendResistedMyMagicMessage(target);
					if (mcrit)
						damage = 1;
					else
						damage = Rnd.nextBoolean() ? 1 : 0;
				}
			}
		}

		// Critical can happen even when failing
		if (mcrit)
		{
			double mCritModifier = 1.0000000;
			if (attacker instanceof L2Playable && target instanceof L2Playable)
				mCritModifier = attacker.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, Config.ALT_MCRIT_PVP_RATE, target, skill);
			else
				mCritModifier = attacker.calcStat(Stats.MAGIC_CRITICAL_DAMAGE, Config.ALT_MCRIT_RATE, target, skill);
			damage *= mCritModifier;
		}

		//random magic damage
		damage *= 1 + (2 * Rnd.nextDouble() - 1) * 0.2;

		// CT2.3 general magic vuln
		damage *= target.calcStat(Stats.MAGIC_DAMAGE_VULN, 1, null, null);
		
		if (target instanceof L2Attackable)
		{
			damage *= attacker.calcStat(Stats.PVE_MAGICAL_DMG, 1, null, null);
		}
	
		return calcDamage(attacker, target, damage, skill, TypeDam.MAGIC);
	}
	
	public static final double calcSoulBonus(L2Character activeChar, L2Skill skill)
	{
		if (skill != null && skill.getMaxSoulConsumeCount() > 0 && activeChar instanceof L2Player)
		{
			switch (((L2Player) activeChar).getLastSoulConsume())
			{
				case 0:
					return 1.00;
				case 1:
					return 1.10;
				case 2:
					return 1.12;
				case 3:
					return 1.15;
				case 4:
					return 1.18;
				default:
					return 1.20;
			}
		}
		
		return 1.0;
	}

	/** Returns true in case of critical hit */
	public static boolean calcSkillCrit(L2Character attacker, L2Character target, L2Skill skill)
	{
		final double rate = skill.getBaseCritRate() * 10 * Formulas.getSTRBonus(attacker);

		return calcCrit(attacker, target, rate);
	}

	public static boolean calcCriticalHit(L2Character attacker, L2Character target)
	{
		final double rate = attacker.getStat().getCriticalHit(target);

		if (!calcCrit(attacker, target, rate))
			return false;

		// support for critical damage evasion
		return Rnd.calcChance(200 - target.getStat().calcStat(Stats.CRIT_DAMAGE_EVASION, 100, attacker, null), 100);

		// l2jserver's version:
		//With default value 1.0 for CRIT_DAMAGE_EVASION critical hits will never be evaded at all.
		//After got buff with CRIT_DAMAGE_EVASION increase (1.3 for exabple) Rnd.get(130) will generate 30% chance to evade crit hit.

		// little weird, but remember what CRIT_DAMAGE_EVASION > 1 increase chances to _evade_ crit hits
		// return Rnd.get((int)target.getStat().calcStat(Stats.CRIT_DAMAGE_EVASION, 100, null, null)) < 100;
	}

	private static boolean calcCrit(L2Character attacker, L2Character target, double rate)
	{
		switch (Direction.getDirection(attacker, target))
		{
			case SIDE:
				rate *= 1.2;
				break;
			case BACK:
				rate *= 1.35;
				break;
		}

		rate *= 1 + getHeightModifier(attacker, target, 0.15);

		return Rnd.calcChance(rate, 1000);
	}

	private static double getHeightModifier(L2Character attacker, L2Character target, double base)
	{
		return base * L2Math.limit(-1.0, ((double) attacker.getZ() - target.getZ()) / 50., 1.0);
	}

	/** Calculate value of blow success */
	public static final boolean calcBlow(L2Character attacker, L2Character target, L2Skill skill)
	{
		if ((skill.getCondition() & L2Skill.COND_BEHIND) != 0 && !attacker.isBehind(target))
			return false;

		double chance = attacker.calcStat(Stats.BLOW_RATE, 40 + 0.5 * attacker.getStat().getDEX(), target, skill);

		switch (Direction.getDirection(attacker, target))
		{
			case SIDE:
				chance += 5;
				break;
			case BACK:
				chance += 15;
				break;
		}

		chance += getHeightModifier(attacker, target, 5);

		chance += 2 * getMagicLevelDifference(attacker, target, skill);

		return Rnd.calcChance(chance, 100);
	}

	/**
	 * 
	 * @param attacker
	 * @param target
	 * @param skill
	 * @return magic level influenced, balanced (attacker level - target level)
	 */
	public static int getMagicLevelDifference(L2Character attacker, L2Character target, L2Skill skill)
	{
		int attackerLvlmod = attacker.getLevel();
		int targetLvlmod = target.getLevel();
		
		// this was definitely overdrawn too
		//if (attackerLvlmod > 75)
		//	attackerLvlmod = 75 + (attackerLvlmod - 75) / 2;
		//if (targetLvlmod > 75)
		//	targetLvlmod = 75 + (targetLvlmod - 75) / 2;
		
		if (skill.getMagicLevel() > 0)
			return (skill.getMagicLevel() + attackerLvlmod) / 2 - targetLvlmod;
		else
			return attackerLvlmod - targetLvlmod;
	}

	/** Calculate value of lethal chance */
	private static final double calcLethal(L2Character activeChar, L2Character target, int baseLethal, L2Skill skill)
	{
		if (baseLethal <= 0)
			return 0;
		
		final double chance;
		final int delta = getMagicLevelDifference(activeChar, target, skill);
		
		
		// delta [-3,infinite)
		if (delta >= -3)
		{
			chance = (baseLethal * ((double) activeChar.getLevel() / target.getLevel()));
		}
		// delta [-9, -3[
		else if (delta < -3 && delta >= -9)
		{
			//               baseLethal
			// chance = -1 * -----------
			//               (delta / 3)
			chance = (-3) * (baseLethal / (delta));
		}
		//delta [-infinite,-9[
		else
		{
			chance = baseLethal / 15;
		}
		
		return 10 * activeChar.calcStat(Stats.LETHAL_RATE, chance, target, null);
	}

	public static final boolean calcLethalHit(L2Character activeChar, L2Character target, L2Skill skill)
	{
		if (target.isRaid() || target instanceof L2DoorInstance)
			return false;
		
		if (target instanceof L2Npc && ((L2Npc)target).getNpcId() == 35062)
			return false;
		
		final int chance = Rnd.get(1000);
		
		// 2nd lethal effect activate (cp,hp to 1 or if target is npc then hp to 1)
		if (chance < calcLethal(activeChar, target, skill.getLethalChance2(), skill))
		{
			if (target instanceof L2Player) // If is a active player set his HP and CP to 1
			{
				target.getStatus().reduceHp(target.getCurrentCp() + target.getCurrentHp() - 1, activeChar);
				target.getStatus().setCurrentHp(1); // just to be sure (transfer damage, etc)
				target.getStatus().setCurrentCp(1); // just to be sure (transfer damage, etc)
				
				target.sendPacket(SystemMessageId.LETHAL_STRIKE_SUCCESSFUL);
			}
			else if (target instanceof L2Npc) // If is a npc set his HP to 1
				target.reduceCurrentHp(target.getCurrentHp() - 1, activeChar, skill);
			
			activeChar.sendPacket(SystemMessageId.LETHAL_STRIKE);
			return true;
		}
		else if (chance < calcLethal(activeChar, target, skill.getLethalChance1(), skill))
		{
			if (target instanceof L2Player) // Set CP to 1
			{
				target.getStatus().reduceHp(target.getCurrentCp() - 1, activeChar);
				target.getStatus().setCurrentCp(1); // just to be sure (transfer damage, etc)
				
				target.sendPacket(SystemMessageId.CP_DISAPPEARS_WHEN_HIT_WITH_A_HALF_KILL_SKILL);
			}
			else if (target instanceof L2Npc) // If is a monster remove first damage and after 50% of current hp
				target.reduceCurrentHp(target.getCurrentHp() / 2, activeChar, skill);
			
			activeChar.sendPacket(SystemMessageId.HALF_KILL);
			return true;
		}
		
		return false;
	}

	public static final boolean calcMCrit(double mRate)
	{
		return mRate > Rnd.get(1000);
	}

	/** Returns true in case when ATTACK is canceled due to hit */
	public static final boolean calcAtkBreak(L2Character target, double dmg)
	{
		if (target.getFusionSkill() != null)
			return true;

		if (target.isRaid() || target.isInvul())
			return false; // No attack break

		double init;

		if (Config.ALT_GAME_CANCEL_CAST && target.isCastingNow() && target.canAbortCast())
			init = 15;
		else if (Config.ALT_GAME_CANCEL_BOW && target.isAttackingNow() &&
				target.getActiveWeaponItem() != null &&
				target.getActiveWeaponItem().getItemType() == L2WeaponType.BOW)
		{
			init = 15;
		}
		else
			return false;

		// Chance of break is higher with higher dmg
		init += Math.sqrt(13 * dmg);

		// Chance is affected by target MEN
		init -= (MENbonus[target.getStat().getMEN()] * 100 - 100);

		// Calculate all modifiers for ATTACK_CANCEL
		double rate = target.calcStat(Stats.ATTACK_CANCEL, init, null, null);

		// Adjust the rate to be between 1 and 99
		rate = L2Math.limit(1, rate, 99);

		return Rnd.get(100) < rate;
	}

	/** Calculate delay (in milliseconds) before next ATTACK */
	public static final int calcPAtkSpd(L2Character attacker, L2Character target, double atkSpd, double base)
	{
		if (attacker instanceof L2Player)
			base *= Config.ALT_ATTACK_DELAY;

		if (atkSpd < 10)
			atkSpd = 10;

		return (int) (base / atkSpd);
	}

	public static double calcCastingRelatedTimeMulti(L2Character attacker, L2Skill skill)
	{
		if (skill.isMagic())
			return 333.3 / attacker.getMAtkSpd();
		else
			return 333.3 / attacker.getPAtkSpd();
	}

	/**
	 * Returns true if hit missed (target evaded) Formula based on
	 * http://l2p.l2wh.com/nonskillattacks.html
	 */
	public static boolean calcHitMiss(L2Character attacker, L2Character target)
	{
		if (attacker instanceof L2GuardInstance)
			return false;

		double chance = getBaseHitChance(attacker, target);

		switch (Direction.getDirection(attacker, target))
		{
			case SIDE:
				chance *= 1.1;
				break;
			case BACK:
				chance *= 1.2;
				break;
		}

		chance *= 1 + getHeightModifier(attacker, target, 0.05);

		return !Rnd.calcChance(chance, 1000);
	}

	private static int getBaseHitChance(L2Character attacker, L2Character target)
	{
		final int diff = attacker.getStat().getAccuracy() - target.getStat().getEvasionRate(attacker);

		if (diff >= 10)
			return 980;

		switch (diff)
		{
			case 9:
				return 975;
			case 8:
				return 970;
			case 7:
				return 965;
			case 6:
				return 960;
			case 5:
				return 955;
			case 4:
				return 945;
			case 3:
				return 935;
			case 2:
				return 925;
			case 1:
				return 915;
			case 0:
				return 905;
			case -1:
				return 890;
			case -2:
				return 875;
			case -3:
				return 860;
			case -4:
				return 845;
			case -5:
				return 830;
			case -6:
				return 815;
			case -7:
				return 800;
			case -8:
				return 785;
			case -9:
				return 770;
			case -10:
				return 755;
			case -11:
				return 735;
			case -12:
				return 715;
			case -13:
				return 695;
			case -14:
				return 675;
			case -15:
				return 655;
			case -16:
				return 625;
			case -17:
				return 595;
			case -18:
				return 565;
			case -19:
				return 535;
			case -20:
				return 505;
			case -21:
				return 455;
			case -22:
				return 405;
			case -23:
				return 355;
			case -24:
				return 305;
		}

		return 275;
	}

	/**
	 * @param attacker
	 * @param target
	 * @param sendSysMsg
	 * @return 0 = shield defense doesn't succeed<br>
	 *         1 = shield defense succeed<br>
	 *         2 = perfect block<br>
	 */
	public static byte calcShldUse(L2Character attacker, L2Character target)
	{
		return calcShldUse(attacker, target, null);
	}

	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill)
	{
		return calcShldUse(attacker, target, skill, true);
	}

	public static byte calcShldUse(L2Character attacker, L2Character target, L2Skill skill, boolean sendSysMsg)
	{
		if (skill != null && skill.ignoreShld())
			return SHIELD_DEFENSE_FAILED;

		// if shield not exists
		Inventory inv = target.getInventory();
		if (inv != null && inv.getPaperdollItem(Inventory.PAPERDOLL_LHAND) == null)
			return SHIELD_DEFENSE_FAILED;

		if (!attacker.isInFrontOf(target, target.calcStat(Stats.SHIELD_ANGLE, 120, target, skill) / 2))
			return SHIELD_DEFENSE_FAILED;

		double shldRate = target.calcStat(Stats.SHIELD_RATE, 0, attacker, skill) * DEXbonus[target.getStat().getDEX()];
		if (shldRate == 0.0)
			return SHIELD_DEFENSE_FAILED;

		// if attacker use bow and target wear shield, shield block rate is multiplied by 1.3 (30%)
		L2Weapon weapon = attacker.getActiveWeaponItem();
		if (weapon != null && weapon.getItemType().isBowType())
			shldRate *= 1.3;

		if (!Rnd.calcChance(shldRate, 100))
			return SHIELD_DEFENSE_FAILED;

		byte shldSuccess = Rnd.calcChance(Config.ALT_PERFECT_SHLD_BLOCK, 100) ? SHIELD_DEFENSE_PERFECT_BLOCK : SHIELD_DEFENSE_SUCCEED;

		if (sendSysMsg && target instanceof L2Player)
		{
			switch (shldSuccess)
			{
				case SHIELD_DEFENSE_SUCCEED:
					target.sendPacket(SystemMessageId.SHIELD_DEFENCE_SUCCESSFULL);
					break;
				case SHIELD_DEFENSE_PERFECT_BLOCK:
					target.sendPacket(SystemMessageId.YOUR_EXCELLENT_SHIELD_DEFENSE_WAS_A_SUCCESS);
					break;
			}
		}

		return shldSuccess;
	}

	public static boolean calcMagicAffected(L2Character actor, L2Character target, L2Skill skill)
	{
		// TODO: CHECK/FIX THIS FORMULA UP!!
		L2SkillType type = skill.getSkillType();
		double defence = 0;
		if (skill.isActive() && skill.isOffensive())
			defence = target.getMDef(actor, skill);

		double attack = 2 * actor.getMAtk(target, skill) * calcSkillVulnerability(actor, target, skill, type);
		double d = (attack - defence) / (attack + defence);
		
		if (target.isRaid() && !calcRaidAffected(type))
			return false;

		d += 0.5 * Rnd.nextGaussian();
		return d > 0;
	}
	
	private static double calcSkillVulnerability(L2Character attacker, L2Character target, L2Skill skill, L2SkillType type)
	{
		if (target.isPreventedFromReceivingBuffs())
			return 0;

		double multiplier = 1; // initialize...
		
		// Get the skill type to calculate its effect in function of base stats
		// of the L2Character target
		if (skill.getElement() > 0)
			multiplier *= Math.sqrt(calcElemental(attacker, target, skill));
		
		/* I would believe this is more logical, and comment BUFF & DEBUFF in switch
		if (skill.isOffensive())
			multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
		else
			multiplier = target.calcStat(Stats.BUFF_VULN, multiplier, target, null);
		*/
		
		switch (type)
		{
			case BLEED:
				multiplier = target.calcStat(Stats.BLEED_VULN, multiplier, target, null);
				break;
			case POISON:
				multiplier = target.calcStat(Stats.POISON_VULN, multiplier, target, null);
				break;
			case STUN:
				multiplier = target.calcStat(Stats.STUN_VULN, multiplier, target, null);
				break;
			case PARALYZE:
				multiplier = target.calcStat(Stats.PARALYZE_VULN, multiplier, target, null);
				break;
			case ROOT:
				multiplier = target.calcStat(Stats.ROOT_VULN, multiplier, target, null);
				break;
			case SLEEP:
				multiplier = target.calcStat(Stats.SLEEP_VULN, multiplier, target, null);
				break;
			case MUTE:
			case FEAR:
			case BETRAY:
			case AGGREDUCE_CHAR:
				multiplier = target.calcStat(Stats.DERANGEMENT_VULN, multiplier, target, null);
				break;
			case CONFUSION:
			case CONFUSE_MOB_ONLY:
				multiplier = target.calcStat(Stats.CONFUSION_VULN, multiplier, target, null);
				break;
			case DEBUFF:
			case WEAKNESS:
				multiplier = target.calcStat(Stats.DEBUFF_VULN, multiplier, target, null);
				break;
			case CANCEL:
			case NEGATE:
				multiplier = target.calcStat(Stats.CANCEL_VULN, multiplier, target, null);
				break;
			case BUFF:
				multiplier = target.calc
