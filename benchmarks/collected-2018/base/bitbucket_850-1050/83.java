// https://searchcode.com/api/result/118849368/

package com.cniska.dungeon.gameobject.character;

import com.cniska.dungeon.base.BaseMap;
import com.cniska.dungeon.base.BaseObject;
import com.cniska.dungeon.event.GameEvent;
import com.cniska.dungeon.fov.*;
import com.cniska.dungeon.fov.FieldOfView;
import com.cniska.dungeon.fov.GridFieldOfView;
import com.cniska.dungeon.gameobject.character.battle.Battle;
import com.cniska.dungeon.gameobject.character.battle.attack.Attack;
import com.cniska.dungeon.gameobject.character.battle.attack.SwordAttack;
import com.cniska.dungeon.gameobject.character.effect.Effect.EffectType;
import com.cniska.dungeon.gameobject.character.effect.event.IEffectApplyListener;
import com.cniska.dungeon.gameobject.character.effect.event.IEffectFadeListener;
import com.cniska.dungeon.gameobject.character.event.*;
import com.cniska.dungeon.gameobject.character.sct.ScrollingCombatText;
import com.cniska.dungeon.gameobject.grid.Grid;
import com.cniska.dungeon.gameobject.grid.GridGameObject;
import com.cniska.dungeon.gameobject.grid.GridPath;
import com.cniska.dungeon.gameobject.character.effect.Effect;
import com.cniska.dungeon.message.CombatLog;
import com.cniska.dungeon.path.IMover;

import java.awt.*;

/**
 * Character class.
 * All characters should be extended from this class.
 * @author Christoffer Niska <ChristofferNiska@gmail.com>
 */
public abstract class Character extends GridGameObject
		implements IMover, IViewer, IEffectApplyListener, IEffectFadeListener
{

	// ----------
	// Properties
	// ----------

	public static final int SCT_OFFSET_Y = 16;
	public static final int DEFAULT_HIT_CHANCE = 75;
	public static final int DEFAULT_CRITICAL_CHANCE = 25;
	public static final int DEFAULT_CRIT_MULTIPLIER = 2;
	public static final int DEFAULT_ATTACK_SPEED = 200;
	public static final int DEFAULT_MOVEMENT_SPEED = 100;
	public static final int DEFAULT_DAMAGE_MULTIPLIER = 1;
	public static final int DEFAULT_ATTACK_SPEED_MULTIPLIER = 1;
	public static final int DEFAULT_MOVEMENT_SPEED_MULTIPLIER = 1;
	public static final float DAMAGE_MULTIPLIER_LEVEL = 0.2f;

	private int level;
	private int minLevel;
	private int maxLevel;
	private int currentHealth;
	private int maximumHealth;
	private int minDamage;
	private int maxDamage;
	private int viewRange;
	private int attackSpeed; // milliseconds
	private int movementSpeed; // milliseconds
	private float attackSpeedMultiplier;
	private float damageMultiplier;
	private float movementSpeedMultiplier;
	private long nextAttackTime;
	private long nextMoveTime;
	
	private GridFieldOfView fov;
	private GridPath path;
	private Character killedBy;
	private BaseMap effects;
	private Attack attack;
	private ScrollingCombatText sct;

	private volatile boolean incapacitated;
	private volatile boolean dead;

	// -------
	// Methods
	// -------

	/**
	 * Creates the character.
	 */
	public Character()
	{
		super();

		level = 1; // characters are level 1 by default
		viewRange = 0; // characters are blind by default
		attackSpeed = DEFAULT_ATTACK_SPEED;
		movementSpeed = DEFAULT_MOVEMENT_SPEED;
		damageMultiplier = DEFAULT_DAMAGE_MULTIPLIER;
		attackSpeedMultiplier = DEFAULT_ATTACK_SPEED_MULTIPLIER;
		movementSpeedMultiplier = DEFAULT_MOVEMENT_SPEED_MULTIPLIER;

		effects = new BaseMap();
		sct = new ScrollingCombatText();

		incapacitated = false; // characters are not incapacitated by default
		dead = false; // characters are obviously not dead by default
	}

	/**
	 * Sets the character health.
	 * @param health the health.
	 */
	public void setHealth(int health)
	{
		currentHealth = maximumHealth = health;
	}

	/**
	 * Sets thi character's levle range.
	 * @param min The minimum level.
	 * @param max The maximum level.
	 */
	public void setLevelRange(int min, int max)
	{
		this.minLevel = min;
		this.maxLevel = max;
	}

	/**
	 * Sets the character's damage range.
	 * @param min The minimum damage.
	 * @param max The maximum damage.
	 */
	public void setDamageRange(int min, int max)
	{
		this.minDamage = min;
		this.maxDamage = max;
	}

	/**
	 * Attacks the target character.
	 * @param battle The battle the attack is happening in.
	 */
	public void attack(Battle battle)
	{
		// make sure the character may attack.
		if (isAttackAllowed())
		{
			attack = getAttack();
			battle.attack(attack);
			afterAttack();
		}
	}

	/**
	 * Actions to take after that this character has attacked.
	 */
	public void afterAttack()
	{
		// Calculate the next time when this character is allowed to attack.
		nextAttackTime = System.currentTimeMillis() + getAttackSpeed();
	}

	/**
	 * Returns the attack for this character.
	 * @return The attack.
	 */
	public Attack getAttack()
	{
		return new SwordAttack();
	}

	/**
	 * Returns whether this character is allowed to attack.
	 * @return Whether attacking is allowed.
	 */
	public boolean isAttackAllowed()
	{
		return !incapacitated && nextAttackTime <= System.currentTimeMillis();
	}

	/**
	 * Heals this character.
	 * @param amount The amount of hit points to heal.
	 */
	public void heal(int amount)
	{
		increaseHealth(amount);
	}

	/**
	 * Increases the character health by the given amount.
	 * @param amount The amount.
	 */
	private void increaseHealth(int amount)
	{
		// Increase this character's health making sure that it doesn't exceed the maximum health.
		int health = currentHealth + amount;
		currentHealth = health < maximumHealth ? health : maximumHealth;
		
		fireHealthGainEvent(new GameEvent(this));
	}

	/**
	 * Damages this character.
	 * @param amount The amount of damage to inflict.
	 */
	public void damage(int amount)
	{
		reduceHealth(amount);
	}

	/**
	 * Reduces the character health by the specified amount.
	 * @param amount The amount.
	 */
	private void reduceHealth(int amount)
	{
		// Reduce this character's health making sure that it doesn't go below zero.
		int newHealth = currentHealth - amount;
		currentHealth = newHealth > 0 ? newHealth : 0;
		fireHealthLossEvent(new GameEvent(this));

		if (currentHealth <= 0)
		{
			dead = true;
			afterDeath();
		}
	}

	/**
	 * Actions to take after that this character has died.
	 */
	public void afterDeath()
	{
		CombatLog.addMessage(getName() + " dies.");
		fireDeathEvent(new GameEvent(this));
		setRemoved(); // remove the dead character
	}

	/**
	 * Actions to be taken when the character moves.
	 * @param direction The direction to move.
	 */
	public void move(Directions direction)
	{
		Grid grid = getGrid();

		// Move left.
		if (direction == Directions.LEFT)
		{
			grid.moveCharacter(-1, 0, this);
		}
		// Move right.
		else if (direction == Directions.RIGHT)
		{
			grid.moveCharacter(1, 0, this);
		}
		// Move up.
		else if (direction == Directions.UP)
		{
			grid.moveCharacter(0, -1, this);
		}
		// Move down.
		else if (direction == Directions.DOWN)
		{
			grid.moveCharacter(0, 1, this);
		}

		afterMove();
	}

	/**
	 * Actions to take after that this character has moved.
	 */
	public void afterMove()
	{
		// Calculate the next time when this character is allowed to move.
		nextMoveTime = System.currentTimeMillis() + getMovementSpeed();
		
		super.afterMove();
	}

	/**
	 * Returns whether the character is allowed to move.
	 * @return Whether moving is allowed.
	 */
	public boolean isMovementAllowed()
	{
		return !incapacitated && nextMoveTime <= System.currentTimeMillis();
	}

	/**
	 * Returns the path to the given target if available.
	 * @param tgx the target grid x-coordinate.
	 * @param tgy the target grid y-coordinate.
	 * @param maxPathLength the maximum length of path.
	 * @return the path or null if unavailable.
	 */
	public GridPath createPath(int tgx, int tgy, int maxPathLength)
	{
		return getGrid().createPath(getGridX(), getGridY(), tgx, tgy, maxPathLength, this);
	}

	/**
	 * Adds an effect to this character.
	 * @param effect The effect to add.
	 */
	// TODO: Think about moving some logic to the effect class.
	public void addEffect(Effect effect)
	{
		EffectType type = (EffectType) effect.getType();
		Effect other = (Effect) effects.get(type);

		if (other != null)
		{
			removeEffect(other);
			effects.applyChanges();
		}

		effect.setSubject(this);
		effect.addListener(this);
		effects.add(effect);
		effect.apply();
	}

	/**
	 * Removes an effect from this character.
	 * @param effect The effect to remove.
	 */
	public void removeEffect(Effect effect)
	{
		effects.remove(effect.getType());
		effect.removeListener(this);
	}

	/**
	 * Returns whether this character is affected by specific effect.
	 * @param effect The effect.
	 * @return Whether the character is affected by the effect.
	 */
	public boolean hasEffect(Effect effect)
	{
		return effects.get(effect.getType()) != null;
	}

	/**
	 * Creates the field of view for this character.
	 */
	public void createFov()
	{
		fov = FieldOfViewFactory.getInstance().create(FieldOfView.FieldOfViewType.SHADOW_CASTING, viewRange, getGrid(), this);
		fov.refresh(getGridX(), getGridY()); // TODO: Think of a better place to call this.
		addListener(fov); // let the fov listen to this character.
	}

	/**
	 * Resolves the real damage done to this character.
	 * @param damage The full damage.
	 * @param attacker The attacker.
	 * @return The real damage.
	 */
	public int resolveDamage(int damage, Character attacker)
	{
		int level = getLevel();
		int attackerLevel = attacker.getLevel();
		int levelDelta = attackerLevel - level;

		if (levelDelta == 0)
		{
			return damage;
		}
		else
		{
			int n = 0;
			float multiplier = levelDelta > 0 ? 1 + DAMAGE_MULTIPLIER_LEVEL : 1 - DAMAGE_MULTIPLIER_LEVEL;
			final int levelDiff = Math.abs(levelDelta);

			while (n < levelDiff)
			{
				damage = Math.round(damage * multiplier);
				n++;
			}

			damage = damage > 0 ? damage : 1;

			return damage;
		}
	}

	/**
	 * Calculates the value for a specific level.
	 * @param value The starting value.
	 * @param increase The increase multiplier.
	 * @param level The level to calculate the value for.
	 * @return The value.
	 */
	public int calculateValue(int value, float increase, int level)
	{
		final int minLevel = getMinLevel();
		final int maxLevel = getMaxLevel();

		// Make sure that the level is valid.
		if (level >= minLevel && level <= maxLevel)
		{
			if (level == minLevel)
			{
				return value;
			}
			else
			{
				int n = 1; // levels start from 1.

				while (n < level)
				{
					value = Math.round(value * increase);
					n++;
				}

				return value;
			}
		}

		return 0; // invalid level
	}

	/**
	 * @return The current health in percent of maximum health for this character.
	 */
	public float getHealthPercent()
	{
		return (float) currentHealth / (float) maximumHealth;
	}

	/**
	 * Returns the scrolling combat text offset on the x-axis.
	 * @return The offset.
	 */
	public int getSctX()
	{
		return getX() + getWidth() / 2;
	}

	/**
	 * Returns the scrolling combat text offset on the y-axis.
	 * @return The offset.
	 */
	public int getSctY()
	{
		return getY() - SCT_OFFSET_Y;
	}

	/**
	 * Draws the GUI objects for this character.
	 * @param g The graphics context.
	 */
	public void drawGui(Graphics2D g)
	{
		if (isReady())
		{
			if (sct != null)
			{
				sct.draw(g);
			}

			if (attack != null)
			{
				attack.draw(g);
			}
		}
	}

	// --------------
	// Event triggers
	// --------------

	/**
	 * Lets all the death listeners know that this character has died.
	 * @param event The event.
	 */
	private void fireDeathEvent(GameEvent event)
	{
		for (BaseObject listener : getListeners())
		{
			if (listener instanceof ICharacterDeathListener)
			{
				((ICharacterDeathListener) listener).onCharacterDeath(event);
			}
		}
	}

	/**
	 * Lets all the health listeners know that this character has gained health.
	 * @param event The event.
	 */
	private void fireHealthGainEvent(GameEvent event)
	{
		for (BaseObject listener : getListeners())
		{
			if (listener instanceof ICharacterHealthListener)
			{
				((ICharacterHealthListener) listener).onCharacterHealthGain(event);
			}
		}
	}

	/**
	 * Lets all the health listeners know that this character has lost health.
	 * @param event The event.
	 */
	private void fireHealthLossEvent(GameEvent event)
	{
		for (BaseObject listener : getListeners())
		{
			if (listener instanceof ICharacterHealthListener)
			{
				((ICharacterHealthListener) listener).onCharacterHealthLoss(event);
			}
		}
	}

	// ------------------
	// Overridden methods
	// ------------------

	/**
	 * Actions to take after that this game object is ready.
	 */
	@Override
	public void afterReady()
	{
		long timeNow = System.currentTimeMillis();
		nextAttackTime = timeNow + getAttackSpeed();
		nextMoveTime = timeNow + getMovementSpeed();

		createFov();

		super.afterReady();
	}

	/**
	 * Updates this object.
	 * @param parent The parent object.
	 */
	@Override
	public void update(BaseObject parent)
	{
		if (isReady() && !isDead() && !isRemoved())
		{
			super.update(parent);
			
			effects.update(this);

			if (sct != null)
			{
				sct.update(this);
			}

			if (attack != null)
			{
				attack.update(this);
			}
		}
	}

	/**
	 * Draws this object.
	 * @param g The graphics context.
	 */
	@Override
	public void draw(Graphics2D g)
	{
		if (isReady() && !isDead() && !isRemoved())
		{
			super.draw(g);

			final int effectCount = effects.getSize();

			if (effectCount > 0)
			{
				for (BaseObject effect : effects.getObjects().values())
				{
					((Effect) effects.get(effect.getType())).draw(g);
				}
			}
		}
	}

	/**
	 * @return The name of this object.
	 */
	@Override
	public String getName()
	{
		return super.getName() + " [" + getLevel() + "]";
	}

	// --------------
	// Event handlers
	// --------------

	/**
	 * Actions to be taken when a character effect is applied.
	 * @param e the event.
	 */
	@Override
	public void onEffectApply(GameEvent e)
	{
		Effect effect = (Effect) e.getSource();
		addEffect(effect);
	}

	/**
	 * Actions to be taken when a character effect fades.
	 * @param e the event.
	 */
	@Override
	public void onEffectFade(GameEvent e)
	{
		Effect effect = (Effect) e.getSource();
		removeEffect(effect);
	}

	// -------------------
	// Getters and setters
	// -------------------

	/**
	 * @return The hit chance for this character in percent.
	 */
	public int getHitChance()
	{
		return DEFAULT_HIT_CHANCE;
	}

	/**
	 * @return The critical chance for this character in percent .
	 */
	public int getCriticalChance()
	{
		return DEFAULT_CRITICAL_CHANCE;
	}

	/**
	 * @return The critical multiplier for this character.
	 */
	public int getCritMultiplier()
	{
		return DEFAULT_CRIT_MULTIPLIER;
	}

	/**
	 * @return The current level of this character.
	 */
	public int getLevel()
	{
		return level;
	}

	/**
	 * @param level The new level for this character.
	 */
	public void setLevel(int level)
	{
		this.level = level;
	}

	/**
	 * @return The new minimum level for this character.
	 */
	public int getMinLevel()
	{
		return minLevel;
	}

	/**
	 * @param minLevel The minimum level for this character.
	 */
	public void setMinLevel(int minLevel)
	{
		this.minLevel = minLevel;
	}

	/**
	 * @return The new maximum level for this character.
	 */
	public int getMaxLevel()
	{
		return maxLevel;
	}

	/**
	 * @param maxLevel The maximum level for this character.
	 */
	public void setMaxLevel(int maxLevel)
	{
		this.maxLevel = maxLevel;
	}

	/**
	 * @return The current maximum health of this character.
	 */
	public int getMaximumHealth()
	{
		return maximumHealth;
	}

	/**
	 * @param health The new maximum health for this character.
	 */
	public void setMaximumHealth(int health)
	{
		this.maximumHealth = health;
	}

	/**
	 * @return The current health of this character.
	 */
	public int getCurrentHealth()
	{
		return currentHealth;
	}

	/**
	 * @return The current minimum damage of this character.
	 */
	public int getMinDamage()
	{
		return Math.round(minDamage * damageMultiplier);
	}

	/**
	 * @return The current maximum damage of this character.
	 */
	public int getMaxDamage()
	{
		return Math.round(maxDamage * damageMultiplier);
	}

	/**
	 * @param range The new view range for this character.
	 */
	public void setViewRange(int range)
	{
		this.viewRange = range;
	}

	/**
	 * @return The current view range of this character.
	 */
	public int getViewRange()
	{
		return viewRange;
	}

	/**
	 * @param attackSpeed The new attack cooldown for this character (in milliseconds).
	 */
	public void setAttackSpeed(int attackSpeed)
	{
		this.attackSpeed = attackSpeed;
	}

	/**
	 * @return The time this character must wait between attacks (in milliseconds).
	 */
	public int getAttackSpeed()
	{
		return Math.round(attackSpeed * attackSpeedMultiplier);
	}

	/**
	 * @param movementSpeed The new movement cooldwon for this character (in milliseconds).
	 */
	public void setMovementSpeed(int movementSpeed)
	{
		this.movementSpeed = movementSpeed;
	}

	/**
	 * @return The time this character must wait in between moves (in milliseconds).
	 */
	public int getMovementSpeed()
	{
		return Math.round(movementSpeed * movementSpeedMultiplier);
	}

	/**
	 * @param killedBy The characterthat killed this character.
	 */
	public void setKilledBy(Character killedBy)
	{
		this.killedBy = killedBy;
	}

	/**
	 * @return The character that killed this character.
	 */
	public Character getKilledBy()
	{
		return killedBy;
	}

	/**
	 * @return Whether this character is dead.
	 */
	public boolean isDead()
	{
		return dead;
	}

	/**
	 * @param dead Whether this character is dead.
	 */
	public void setDead(boolean dead)
	{
		this.dead = dead;
	}

	/**
	 * @return The current field of view of this character.
	 */
	public GridFieldOfView getFov()
	{
		return fov;
	}

	/**
	 * @param path The new path for this character.
	 */
	public void setPath(GridPath path)
	{
		this.path = path;
	}

	/**
	 * @return The current path for this character.
	 */
	public GridPath getPath()
	{
		return path;
	}

	/**
	 * @return The scrolling combat text for this character.
	 */
	public ScrollingCombatText getSct()
	{
		return sct;
	}

	/**
	 * @return The effects affecting this character.
	 */
	public BaseMap getEffects()
	{
		return effects;
	}

	/**
	 * @return The damage multiplier for this character.
	 */
	public float getDamageMultiplier()
	{
		return damageMultiplier;
	}

	/**
	 * @param multiplier The damage multiplier for this character.
	 */
	public void setDamageMultiplier(float multiplier)
	{
		this.damageMultiplier = multiplier;
	}

	/**
	 * @return The attack speed multiplier for this character.
	 */
	public float getAttackSpeedMultiplier()
	{
		return attackSpeedMultiplier;
	}

	/**
	 * @param multiplier The attack speed multiplier for this character.
	 */
	public void setAttackSpeedMultiplier(float multiplier)
	{
		this.attackSpeedMultiplier = multiplier;
	}

	/**
	 * @return The movement speed multiplier for this character.
	 */
	public float getMovementSpeedMultiplier()
	{
		return movementSpeedMultiplier;
	}

	/**
	 * @param multiplier The movement speed multiplier for this character.
	 */
	public void setMovementSpeedMultiplier(float multiplier)
	{
		this.movementSpeedMultiplier = multiplier;
	}

	/**
	 * @param incapacitated Whether this character is incapacitated.
	 */
	public void setIncapacitated(boolean incapacitated)
	{
		this.incapacitated = incapacitated;
	}

	/**
	 * Returns whether the target character is hostile.
	 * @param target the target character.
	 * @return whether the target is hostile.
	 */
	public abstract boolean isHostile(Character target);
}

