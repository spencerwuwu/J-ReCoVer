// https://searchcode.com/api/result/125985043/

package com.moopek.SpaceAssassin.Missions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import org.anddev.andengine.audio.sound.Sound;
import org.anddev.andengine.audio.sound.SoundFactory;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.AnimatedSprite;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.sprite.AnimatedSprite.IAnimationListener;
import org.anddev.andengine.entity.sprite.BaseSprite;
import org.anddev.andengine.opengl.texture.Texture;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.opengl.texture.region.TiledTextureRegion;

import com.moopek.SpaceAssassin.BasicGameScene;
import com.moopek.SpaceAssassin.Enemies.BaseEnemy;
import com.moopek.SpaceAssassin.Hud.GameHud;
import com.moopek.SpaceAssassin.Ships.BaseShip;

import android.content.Context;

public class BaseMission implements IUpdateHandler
{
	protected Engine mGameEngine;
	protected Context mContext;
	protected GameHud mGameHud;
	protected BasicGameScene mGameScene;
	
	protected ArrayList<MissionObject> mMissionObjects = new ArrayList<MissionObject>();
	protected BaseShip mPlayerShip;
	
	protected Texture mMineralTexture;
	protected TiledTextureRegion mMineralTextureRegion;
	
	protected Texture mAsteroidTexture;
	protected TiledTextureRegion mAsteroidTextureRegion;
	
	protected TextureRegion mMarkerTextureRegion;
	
	protected Texture mExplosionTexture;
	
	protected final int MAX_EXPLOSIONS = 5;
	protected TiledTextureRegion mExplosionTextureRegion[] = new TiledTextureRegion[MAX_EXPLOSIONS];
	
	protected IMissionListener mListener;
	
	protected Random mRandom;
		
	ArrayList<BaseSprite> mToRemove = new ArrayList<BaseSprite>();
	ArrayList<MissionObject> mDeadMissionObjects = new ArrayList<MissionObject>();

	private Sound mExplosion1;
	private Sound mHealthCollected;
	
	protected TextureCache mTextureCache;
	protected SoundCache mSoundCache;
	
	protected int mScore = 0;
	protected int mMultiplier = 1;
	
	protected boolean mRequiresAsteroids = false;
		
	public static final float ASTEROID_DAMAGE_TO_SHIP = 5.0f;
	
	// Common mission objects
	public static final int MISSION_OBJECT_PLAYER_SHIP = 0;
	public static final int MISSION_OBJECT_SHIP_PRIMARY_FIRE = 1;
	public static final int MISSION_OBJECT_MINERAL = 2;
	public static final int MISSION_OBJECT_ASTEROID = 3;
	public static final int MISSION_OBJECT_ENEMY_SHIP = 4;
	public static final int MISSION_OBJECT_FRIENDLY_SHIP = 5;
	public static final int MISSION_OBJECT_MARKER = 6;
	
	public static final int MISSION_OBJECT_ANYTHING = Integer.MAX_VALUE;
		
	public static final float MINERAL_HEALTH_INCREASE = 10.0f;
	
	// A mission object
	public final class MissionObject
	{
		public BaseSprite mEntity;
		public BaseSprite mOwner;
		public int mType;
		public Object mUserData;
		public boolean mCheckCollisions;
		public float mExpiry;
		public boolean mExpires;
	}
	
	public final class DelayedEnemySpawn
	{
		BaseEnemy mEnemy;
		float mX;
		float mY;
		float mDistance;
		boolean mRegisterUpdates;
		float mTimeLeft;
	}
	
	protected ArrayList<DelayedEnemySpawn> mEnemiesToSpawn = new  ArrayList<DelayedEnemySpawn>();
	
	public enum Layer { Bottom, Top }
	
	public BaseMission(final Context context, final Engine gameEngine, final BasicGameScene gameScene, final BaseShip playerShip, final GameHud gameHud)
	{
		mContext = context;
		mGameEngine = gameEngine;
		mGameScene = gameScene;
		mGameHud = gameHud;
		mPlayerShip = playerShip;
		
		mTextureCache = new TextureCache(gameEngine, context);
		mSoundCache = new SoundCache(gameEngine, context);
		
		increaseScore(0);
		
		loadSounds();
		loadResources();
	}
	
	public Random random()
	{
		return mRandom;
	}
	
	void loadResources()
	{
		mMarkerTextureRegion = mTextureCache.loadTexture("x.png");
	}
	
	void loadSounds()
	{
		try
		{
			mExplosion1 = SoundFactory.createSoundFromAsset(mGameEngine.getSoundManager(), mContext, "sounds/explosion01.ogg");
			mHealthCollected = SoundFactory.createSoundFromAsset(mGameEngine.getSoundManager(), mContext, "sounds/health_collected.ogg");			
		}
		catch (IllegalStateException e)
		{
		}
		catch (IOException e)
		{
		}
	}
	
	public Scene getGameScene()
	{
		return mGameScene;
	}
	
	public void loadMission()
	{
		mRandom = new Random();
		
		mMineralTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);		
		mMineralTextureRegion = TextureRegionFactory.createTiledFromAsset(mMineralTexture, mContext, "other/mineral.png", 0, 0, 8, 8);
		
		mGameEngine.getTextureManager().loadTexture(mMineralTexture);
		
		if(mRequiresAsteroids)
		{
			mAsteroidTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
			mAsteroidTextureRegion = TextureRegionFactory.createTiledFromAsset(mAsteroidTexture, mContext, "other/asteroid3.png", 0, 0, 8, 8);

			mGameEngine.getTextureManager().loadTexture(mAsteroidTexture);
		}

		mExplosionTexture = new Texture(512, 512, TextureOptions.BILINEAR_PREMULTIPLYALPHA);
		
		// For MAX_EXPLOSIONS...create texture regions
		for(int i = 0; i < MAX_EXPLOSIONS; i++)
		{
			mExplosionTextureRegion[i] = TextureRegionFactory.createTiledFromAsset(mExplosionTexture, mContext, "explosions/explosion1.png", 0, 0, 8, 8);
		}
		
		mGameEngine.getTextureManager().loadTexture(mExplosionTexture);		
	}
	
	public void startMission()
	{
		mGameScene.registerUpdateHandler(this);
	}
	
	public int getScore()
	{
		return mScore;
	}
	
	public MissionObject spawnPlayer(float x, float y)
	{
		// Create mission object for player ship
		MissionObject mo = new MissionObject();
		
		mPlayerShip.getSprite().setPosition(x, y);
		mPlayerShip.setAngle(0);
		
		mPlayerShip.makeAlive();
		
		mo.mEntity = mPlayerShip.getSprite();
		mo.mCheckCollisions = true;
		mo.mType = MISSION_OBJECT_PLAYER_SHIP;
		mo.mUserData = mPlayerShip;
		
		addMissionObject(mo, Layer.Top);
		return mo;
	}
	
	public void spawnPlayer()
	{
		spawnPlayer(mPlayerShip.getSprite().getX(), mPlayerShip.getSprite().getY());
	}
	
	protected MissionObject spawnMarker(float x, float y)
	{
		Sprite marker = new Sprite(x - mMarkerTextureRegion.getWidth() / 2, y - mMarkerTextureRegion.getHeight() / 2, mMarkerTextureRegion);
	
		MissionObject mo = new MissionObject();

		mo.mCheckCollisions = true;
		mo.mExpires = false;
		mo.mUserData = null;
		mo.mType = MISSION_OBJECT_MARKER;
		mo.mEntity = marker;
				
		addMissionObject(mo, Layer.Bottom);
		
		return mo;		
	}
	
	protected MissionObject spawnFriendly(BaseEnemy enemy, float x, float y, float distance, boolean registerUpdates)
	{
		float dirX = -1.0f + (mRandom.nextFloat() * 2);
		float dirY = -1.0f + (mRandom.nextFloat() * 2);
				
		MissionObject mo = new MissionObject();		

		enemy.getShip().setMission(this);
		enemy.getShip().setupForMission(mGameScene, mTextureCache);

		mo.mCheckCollisions = true;
		mo.mExpires = false;
		mo.mUserData = enemy.getShip();
		mo.mType = MISSION_OBJECT_FRIENDLY_SHIP;
		mo.mEntity = enemy.getSprite();
		
		enemy.getSprite().setPosition(x + (Math.signum(dirX) * distance), y + (Math.signum(dirY) * distance));				
		
		if(registerUpdates)
		{
			mGameScene.registerUpdateHandler(enemy);
		}
		
		addMissionObject(mo, Layer.Bottom);
		
		enemy.resetState();
		enemy.getShip().makeAlive();
		
		return mo;		
	}
		
	protected void spawnEnemyIn(BaseEnemy enemy, float x, float y, float distance, boolean registerUpdates, float inSeconds)
	{
		DelayedEnemySpawn delayedSpawn = new DelayedEnemySpawn();
		
		delayedSpawn.mEnemy = enemy;
		delayedSpawn.mX = x;
		delayedSpawn.mY = y;
		delayedSpawn.mDistance = distance;
		delayedSpawn.mRegisterUpdates = registerUpdates;
		delayedSpawn.mTimeLeft = inSeconds;
		
		mEnemiesToSpawn.add(delayedSpawn);
	}
	
	protected MissionObject spawnEnemy(BaseEnemy enemy, float x, float y, float distance, boolean registerUpdates)
	{
		float dirX = -1.0f + (mRandom.nextFloat() * 2);
		float dirY = -1.0f + (mRandom.nextFloat() * 2);
				
		enemy.getShip().setMission(this);
		enemy.getShip().setupForMission(mGameScene, mTextureCache);

		MissionObject mo = new MissionObject();		

		mo.mCheckCollisions = true;
		mo.mExpires = false;
		mo.mUserData = enemy.getShip();
		mo.mType = MISSION_OBJECT_ENEMY_SHIP;
		mo.mEntity = enemy.getSprite();
				
		enemy.getSprite().setPosition(x + (Math.signum(dirX) * distance), y + (Math.signum(dirY) * distance));				

		if(registerUpdates)
		{
			mGameScene.registerUpdateHandler(enemy);
		}
		
		addMissionObject(mo, Layer.Bottom);
		
		enemy.resetState();
		enemy.getShip().makeAlive();
		
		return mo;
	}
	
	protected MissionObject spawnAsteroid(float x, float y, float distance, float velocity)	
	{
		float dirX = -1.0f + (mRandom.nextFloat() * 2);
		float dirY = -1.0f + (mRandom.nextFloat() * 2);
		
		// Create animated sprite for asteroid
		AnimatedSprite asteroid = new AnimatedSprite(x + (Math.signum(dirX) * distance), y + (Math.signum(dirY) * distance), mAsteroidTextureRegion);
		
		// Set velocity of the asteroid
		asteroid.setVelocity(dirX * velocity, dirY * velocity);

		// Random rotation from animated texture
		asteroid.setCurrentTileIndex(mRandom.nextInt(64));
		
		// Rotation velocity
		asteroid.setAngularVelocity(-360.0f + mRandom.nextFloat() * 360.0f);		

		asteroid.setWidth(asteroid.getWidth());
		asteroid.setHeight(asteroid.getHeight());
		
		MissionObject mo = new MissionObject();
		
		mo.mEntity = asteroid;
		mo.mType = MISSION_OBJECT_ASTEROID;
		mo.mCheckCollisions = false;
		
		addMissionObject(mo);
		
		return mo;		
	}
		
	protected void spawnExplosion(BaseSprite sprite)
	{
		spawnExplosion(sprite, 0, 0, 0);
	}
	
	protected void spawnExplosion(BaseSprite sprite, float rotX, float rotY, float angle)
	{
		spawnExplosion(sprite.getX() + (sprite.getWidth() / 2) - 32,
				       sprite.getY() + (sprite.getHeight() / 2) - 32, rotX, rotY, angle, 1);
	}

	protected void spawnExplosion(float x, float y, float rotX, float rotY, float angle, float scale)
	{
		// Remove explosion from our max explosion list
		TiledTextureRegion textureRegion = null;
		for(int i = 0; i < MAX_EXPLOSIONS; i++)
		{
			if(mExplosionTextureRegion[i] != null)
			{
				textureRegion = mExplosionTextureRegion[i];
				mExplosionTextureRegion[i] = null;
				break;
			}
		}
		
		if(textureRegion == null)
		{
			// No more explosions can be created
			return;
		}
		
		// Play explosion sound
		mExplosion1.play();
				
		AnimatedSprite explosion = new AnimatedSprite(x, y, textureRegion);
		
		explosion.setRotationCenter(rotX, rotY);
		explosion.setRotation(angle);
		
		mGameScene.getTopLayer().addEntity(explosion);
		
		explosion.setScale(scale);		
		explosion.animate(20, false, new IAnimationListener()
		{
			public void onAnimationEnd(AnimatedSprite explosion)
			{
				// Put back explosion texture region
				for(int i = 0; i < MAX_EXPLOSIONS; i++)
				{
					if(mExplosionTextureRegion[i] == null)
					{
						mExplosionTextureRegion[i] = explosion.getTextureRegion();
						break;
					}
				}
				
				mToRemove.add(explosion);
			}
		});
	}
	
	protected MissionObject spawnMineral(float x, float y, float distance)
	{
		return spawnMineral(x, y, 0, 0, 0, distance);
	}
	
	protected MissionObject spawnMineral(float x, float y, float rotX, float rotY, float angle, float distance)	
	{
		float dirX = (mRandom.nextFloat() * 2.0f) - 1.0f;
		float dirY = (mRandom.nextFloat() * 2.0f) - 1.0f;
		
		x -= mMineralTextureRegion.getTileWidth() / 2;
		y -= mMineralTextureRegion.getTileHeight() / 2;
		
		AnimatedSprite mineral = new AnimatedSprite(x + (Math.signum(dirX) * distance), y + (Math.signum(dirY) * distance), mMineralTextureRegion);
		
		mineral.setRotationCenter(rotX, rotY);
		mineral.setRotation(angle);
		
		final long[] frameDurations = new long[60];
		Arrays.fill(frameDurations, 50);
		
		mineral.animate(frameDurations, 0, 59, true);
		
		MissionObject mo = new MissionObject();
		
		mo.mEntity = mineral;
		mo.mType = MISSION_OBJECT_MINERAL;
		mo.mCheckCollisions = false;

		addMissionObject(mo);
		
		return mo;
	}	

	public TextureCache getTextureCache()
	{
		return mTextureCache;
	}
	
	public SoundCache getSoundCache()
	{
		return mSoundCache;
	}
	
	public void onUpdate(float secondsElapsed)
	{
		// Check collisions between all missions objects
		for(int i = 0; i < mMissionObjects.size(); i++)
		{
			// Check expiry
			MissionObject first = mMissionObjects.get(i);
			
			if(first.mExpires)
			{
				first.mExpiry -= secondsElapsed;
				if(first.mExpiry < 0)
				{
					mDeadMissionObjects.add(first);
					continue;
				}
			}			
			
			// Check collisions
			if(first.mCheckCollisions)
			{
				for(int j = 0; j < mMissionObjects.size(); j++)
				{
					MissionObject second = mMissionObjects.get(j);
					if(first != second && first.mOwner != second.mEntity)
					{
						// We'll do a dirty hack here to speed this up
						// don't check collisions between enemies
						if(first.mType == MISSION_OBJECT_ENEMY_SHIP &&
						   second.mType == MISSION_OBJECT_ENEMY_SHIP)
						{
							continue;
						}
						
						if(first.mEntity.collidesWith(second.mEntity))
						{
							onCollision(first, second);
						}						
					}
				}
			}
		}
		
		// Remove items no longer needed
		for(int i = 0; i < mToRemove.size(); i++)
		{
			mGameScene.getTopLayer().removeEntity(mToRemove.get(i));
		}		
		mToRemove.clear();

		for(int i = 0; i < mDeadMissionObjects.size(); i++)
		{
			removeMissionObject(mDeadMissionObjects.get(i));
		}		
		mDeadMissionObjects.clear();
		
		// Delayed spawn
		for(int i = 0; i < mEnemiesToSpawn.size(); i++)
		{
			DelayedEnemySpawn d = mEnemiesToSpawn.get(i);
			d.mTimeLeft -= secondsElapsed;
			
			if(d.mTimeLeft <= 0)
			{
				spawnEnemy(d.mEnemy, d.mX, d.mY, d.mDistance, d.mRegisterUpdates);
				mEnemiesToSpawn.remove(i);
				i--;
			}
		}
		
		// Fail mission when player dies
		if(mPlayerShip.getShipHealth() <= 0)
		{
			onPlayerDead();
		}
	}
	
	protected void onPlayerDead()
	{
		if(mListener != null)
		{
			mListener.onMissionFailed();
			mGameScene.unregisterUpdateHandler(this);
		}		
	}

	protected void increaseShipHealth(MissionObject mo, float amount)
	{
		BaseShip ship = (BaseShip) mo.mUserData;		
		ship.setShipHealth(ship.getShipHealth() + amount);		
	}
	
	protected void reduceShipHealth(MissionObject mo, float amount)
	{
		BaseShip ship = (BaseShip) mo.mUserData;		
		ship.setShipHealth(ship.getShipHealth() - amount);

		if(ship == mPlayerShip)
		{
			mMultiplier = 0;
		}

		if(ship.getShipHealth() <= 0)
		{			
			if(ship.leavesHealth())
			{
				spawnMineral(ship.getSprite().getX() + ship.getSprite().getWidth() / 2,
						     ship.getSprite().getY() + ship.getSprite().getHeight() / 2,
						     ship.getSprite().getRotationCenterX(),
						     ship.getSprite().getRotationCenterY(),
						     ship.getSprite().getRotation(),
						     0);
			}

			mDeadMissionObjects.add(mo);			
			spawnExplosion(mo.mEntity);
			
			if(ship != mPlayerShip)
			{
				if(mMultiplier < 20)
				{
					mMultiplier++;
				}
				
				increaseScore(ship.getScore());
			}
		}		
	}
	
	protected void onAsteriodDestroyed(MissionObject asteroid)
	{
		spawnExplosion(asteroid.mEntity);				
		mDeadMissionObjects.add(asteroid);		
	}
	
	protected void onCollision(MissionObject first, MissionObject second)
	{
		if(first.mType == MISSION_OBJECT_PLAYER_SHIP ||
		   first.mType == MISSION_OBJECT_ENEMY_SHIP)
		{
			switch(second.mType)
			{
			case MISSION_OBJECT_ASTEROID:
				reduceShipHealth(first, ASTEROID_DAMAGE_TO_SHIP);
				onAsteriodDestroyed(second);
				break;
				
			case MISSION_OBJECT_MINERAL:
				if(first.mType == MISSION_OBJECT_PLAYER_SHIP)
				{
					increaseShipHealth(first, MINERAL_HEALTH_INCREASE);
					mHealthCollected.play();
					
					mDeadMissionObjects.add(second);
				}
				break;
			
			case MISSION_OBJECT_ENEMY_SHIP:
				if(first.mType == MISSION_OBJECT_PLAYER_SHIP ||
				   first.mType == MISSION_OBJECT_FRIENDLY_SHIP)
				{
					BaseShip playerShip = (BaseShip) first.mUserData;
					BaseShip enemyShip = (BaseShip) second.mUserData;				
					if(enemyShip.collideWithOtherShips())
					{					
						float playerHealth = playerShip.getShipHealth();
						float enemyHealth = enemyShip.getShipHealth();
						
						reduceShipHealth(first, enemyHealth);
						reduceShipHealth(second, playerHealth);
						
						if(enemyShip.getShipHealth() <= 0)
						{
							spawnExplosion(enemyShip.getSprite());
						}
					}
				}
				break;
				
			case MISSION_OBJECT_SHIP_PRIMARY_FIRE:
				if(first.mUserData != second.mUserData)
				{					
					// Reduce hit ship health
					BaseShip hitShip = (BaseShip) first.mUserData;
					BaseShip firingShip = (BaseShip) second.mUserData;
					
					// Enemy friendly fire
					if(firingShip == mPlayerShip &&
					   first.mType == MISSION_OBJECT_ENEMY_SHIP ||
					   hitShip == mPlayerShip)
					{
						if(hitShip == mPlayerShip)
						{
							mMultiplier = 1;
						}

						spawnExplosion(second.mEntity);
						mDeadMissionObjects.add(second);
						reduceShipHealth(first, firingShip.getPrimaryFireDamage());						
					}
				}
				break;

			}
		}
		else if(first.mType == MISSION_OBJECT_SHIP_PRIMARY_FIRE)
		{
			switch(second.mType)
			{
			case MISSION_OBJECT_ASTEROID:
				onAsteriodDestroyed(second);
				mDeadMissionObjects.add(first);
				break;
				
			case MISSION_OBJECT_FRIENDLY_SHIP:
				if(first.mUserData != second.mUserData)
				{					
					BaseShip firingShip = (BaseShip) first.mUserData;
					spawnExplosion(first.mEntity);
					mDeadMissionObjects.add(first);

					reduceShipHealth(second, firingShip.getPrimaryFireDamage());						
				}				
			}			
		}
	}
	
	public void increaseScore(int score)
	{
		mScore += score * mMultiplier;
	}
	
	public void removeMissionObject(MissionObject mo)
	{
		mGameScene.getTopLayer().removeEntity(mo.mEntity);
		mMissionObjects.remove(mo);
		
		mo.mEntity.setVisible(false);
	}
	
	public void addMissionObject(MissionObject mo, Layer layer)
	{
		if(layer == Layer.Bottom)
		{
			mGameScene.getBottomLayer().addEntity(mo.mEntity);
		}
		else
		{
			mGameScene.getTopLayer().addEntity(mo.mEntity);
		}

		mo.mEntity.setCullingEnabled(true);		
		mMissionObjects.add(mo);
	}
	
	public void addMissionObject(MissionObject mo)
	{
		addMissionObject(mo, Layer.Bottom);
	}
		
	public void reset()
	{		
	}
	
	public MissionObject getNewMissionObject()
	{
		return new MissionObject();
	}
	
	public void setListener(IMissionListener listener)
	{
		mListener = listener;
	}
	
	public ArrayList<MissionObject> getAllMissionObjects()
	{
		return mMissionObjects;
	}
}

