// https://searchcode.com/api/result/12821956/

/*
 *
 * This file is part of Aiphial.
 *
 * Copyright (c) 2010 Nicolay Mitropolsky <NicolayMitropolsky@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package me.uits.aiphial.general.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import me.uits.aiphial.general.dataStore.DataStore;
import me.uits.aiphial.general.dataStore.NDimPoint;
import me.uits.aiphial.general.dataStore.SimpleNDimPoint;

/**
 * Fast mean-shift clusterer assuming that nearest points
 * in source data would become members of same clusters.
 * Faster but less accurate.
 * @author Nicolay Mitropolsky <NicolayMitropolsky@gmail.com>
 */
public class FastMeanShiftClusterer<T extends NDimPoint> extends MeanShiftClusterer<T> implements IMeanShiftClusterer<T>
{

    protected DataStore<? extends T> tempStore;
    private int speedUpFactor;
    private SimpleNDimPoint window;

    public FastMeanShiftClusterer()
    {
        speedUpFactor = 10;
    }

    public void doClustering()
    {
        resultStore = dataStoreFactory.createDataStore(dataStore.getDim());

        //resultStore = new MultiDimMapDataStore(dataStore.getDim());
        //resultStore.setWindow(dataStore.getWindow());

        tempStore = dataStore.clone();


        Float[] sameWindow = new Float[tempStore.getDim()];

        Float[] originalWindow = window.getFloatData();

        for (int i = 0; i < originalWindow.length; i++)
        {
            sameWindow[i] = originalWindow[i] / speedUpFactor;

        }


        //tempStore.setWindow(sameWindow);

        SimpleNDimPoint samePoint = new SimpleNDimPoint(sameWindow);


 
        while (!tempStore.isEmpty())
        {

            

            NDimPoint nDimPoint = tempStore.getNearest(SimpleNDimPoint.getZeroPoint(tempStore.getDim()));

            Collection<? extends T> movingpoints = tempStore.removeWithinWindow(samePoint,nDimPoint);



            NDimPoint calkBofA = calkBofA(nDimPoint);

            //NDimPoint calkBofA = nDimPoint; // !!!!!!!!!!!!!!!!!!!!

            Collection<Bof<T>> withinWindow = resultStore.getWithinWindow(window,calkBofA);

            if (withinWindow.size() > 0)
            {
                Bof<T> first =  withinWindow.iterator().next();
                //first.points.add(nDimPoint);

                first.points.addAll(movingpoints);

            } else
            {
                Bof<T> newBof = new Bof<T>(calkBofA);
                //newBof.points.add(nDimPoint);
                newBof.points.addAll(movingpoints);
                resultStore.addOrGet(newBof);
            }

           

        //System.out.println(nDimPoint+" - "+calkBofA);
        }

        performClusters();

    }

    /**
     * @return speedup factor
     */
    public int getSpeedUpFactor()
    {
        return speedUpFactor;
    }

    /**
     * Set the speedup factors.
     * higher value will increase speed but reduce quality.
     * default value is 10
     * @param speedUpFactor
     */
    public void setSpeedUpFactor(int speedUpFactor)
    {
        this.speedUpFactor = speedUpFactor;
    }

    public void setWindow(Float... window) {
        this.window = new SimpleNDimPoint(window);
        super.setWindow(window);
    }

    
}

