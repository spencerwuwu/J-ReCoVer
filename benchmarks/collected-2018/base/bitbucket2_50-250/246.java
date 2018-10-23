// https://searchcode.com/api/result/61134240/

package com.moopek.SpaceAssassin.Missions;

import org.anddev.andengine.engine.Engine;
import android.content.Context;

import com.moopek.SpaceAssassin.GameScene;
import com.moopek.SpaceAssassin.Enemies.AggressiveEnemy;
import com.moopek.SpaceAssassin.Enemies.BaseEnemy;
import com.moopek.SpaceAssassin.Hud.GameHud;
import com.moopek.SpaceAssassin.Ships.BaseShip;
import com.moopek.SpaceAssassin.Ships.BasicShip;
import com.moopek.SpaceAssassin.Ships.IShipListener;


public class Mission10 extends BaseMission implements IShipListener
{
	/////////////////////////////////////////
	// Mission 10
	//
	// * Fight yourself
	//
	/////////////////////////////////////////
	private BaseEnemy mEnemy;	

	public Mission10(final Context context, final Engine gameEngine, final GameScene gameScene, final BaseShip playerShip, final GameHud gameHud)
	{
		super(context, gameEngine, gameScene, playerShip, gameHud);
	}
	
	@Override
	public void startMission()
	{
		super.startMission();
		
		mEnemy = new AggressiveEnemy(new BasicShip(mContext, mGameEngine), mPlayerShip, this);				
		mEnemy.getShip().setListener(this);
		
		// Reduce performance
		mEnemy.getShip().setShipPerformance(1.0f, 200.0f, 2.0f);
		mEnemy.getShip().setMaximumShipHealth(mEnemy.getShip().getMaxHealth() * 2);
		
		spawnEnemy(mEnemy, 1000.0f, -800.0f, 0, true);
		mGameHud.addPointOfInterest(mEnemy.getSprite());
	}
	
	@Override
	protected void onCollision(MissionObject first, MissionObject second)
	{
		super.onCollision(first, second);
		
		if(first.mType == MISSION_OBJECT_PLAYER_SHIP)
		{
			switch(second.mType)
			{
			case MISSION_OBJECT_ASTEROID:
				break;
			}			
		}
		else if(first.mType == MISSION_OBJECT_SHIP_PRIMARY_FIRE)
		{
			switch(second.mType)
			{
			case MISSION_OBJECT_ASTEROID:
				break;
			}			
		}
	}

	public void onShipDamaged(BaseShip ship, float shipHealth)
	{
		if(shipHealth <= 0)
		{
			mListener.onMissionSucceeded(600);
		}
	}
}

