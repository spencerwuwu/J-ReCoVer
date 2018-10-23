// https://searchcode.com/api/result/67321618/

/***********************************************************************
 *Author: Chris Rees and Wilfredo Velasquez
 *Date: 10/27/08
 *File Name: Stats.java
 *Purpose: All of the methods that involve character stats.
***********************************************************************/

package com.serneum.rpg.character;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

import javax.swing.JOptionPane;

import com.serneum.rpg.battle.BattleData;
import com.serneum.rpg.core.Inventory;

public class Stats
{
    static public StringBuffer race = new StringBuffer("");
    static public StringBuffer classChoice = new StringBuffer("");
    static public StringBuffer charName = new StringBuffer("");
    public static int charNum = 0;
    private static String temp = "";

    //Set all stats as integers with base value 10. These will be incremented
    //based upon race and class choice
    public static int maxHP = 0;
    public static int hp = 0;
    public static int maxAP = 10;
    public static int AP = 0;
    public static int str = 10;
    public static int endur = 10;
    public static int dex = 10;
    public static int intel = 10;
    public static int wis = 10;

    private static int HpInc = 0;
    private static int HpBase = 0;

    public static int AC = 0;
    public static int wMin = 0;
    public static int wMax = 0;
    public static int wAcc = 0;
    public static int armMod = 0;
    public static int shlMod = 0;

    public static String wep = "";
    public static String arm = "";
    public static String shl = "";

    //All stats/variables for levels. Intial level is 1. All other values
    //intialized later in the program.
    public static int level = 1;
    public static int EXP = 0;
    public static int expGain = 0;
    public static int currentLvExp = 0;
    public static int nextLevel = 0;
    public static int statPoints = 0;

    //Stat modifiers kept together for easy management.
    public static int endMod = 0;
    public static int strMod = 0;
    public static int dexMod = 0;
    public static int intMod = 0;
    public static int wisMod = 0;

    public static int minStr = 0;
    public static int minEnd = 0;
    public static int minDex = 0;
    public static int minInt = 0;
    public static int minWis = 0;

    //Boolean newChar used to reduce lines of code wherever a test
    //for a new or continued character exists.
    private static boolean newChar = false;

    public Stats()
    {
        //Default null values if nothing is entered
        charName.append("");
        race.append("");
        classChoice.append("");
    }

    public static void statInitialGen()
    {
        try
        {
            newChar = true;
            //Used to generate stats the first time
            raceStatGen();
            classStatGen();

            printGear();
            printStats();
            modGen();
            getNextEXP();
            minStatsPrint();
            Inventory.New();
        }

        catch(FileNotFoundException FNFE)
        {
            System.out.println("Unable to initialize stats");
        }
    }

    public static void statGen() throws FileNotFoundException
    {
        InputStream charStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + "/CharSheet.dat");
        Scanner plData = new Scanner(charStream);

        while(plData.hasNext())
        {
            //Correct character information loading
            plData.next();
            charName.delete(0,40);
            charName.append(plData.next());
            //System.out.print("\nName: " + charName);

            plData.next();
            race.delete(0,40);
            race.append(plData.next());
            //System.out.print("\nRace: " + race);

            plData.next();
            classChoice.delete(0,40);
            classChoice.append(plData.next());
            //System.out.print("\nClass: " + classChoice);

            plData.next();
            level = plData.nextInt();
            //System.out.print("\nLevel: " + level);

            plData.next();
            EXP = plData.nextInt();
            //System.out.print("\nEXP: " + EXP);

            plData.next();
            nextLevel = plData.nextInt();

            plData.next();
            statPoints = plData.nextInt();
            //System.out.print("\nStat Points: " + statPoints);

            plData.next();
              maxHP = plData.nextInt();
            //System.out.print("\nMaxHp: " + maxHP);

            plData.next();
            hp = plData.nextInt();
            //System.out.print("\nCurrentHp: " + hp);

            plData.next();
            maxAP = plData.nextInt();
            //System.out.print("\nMaxAP: " + maxAP);

            plData.next();
            AP = plData.nextInt();
            //System.out.print("\nCurrentAP: " + AP);

            plData.next();
            str = plData.nextInt();
            //System.out.print("\nStrength: " + str);

            plData.next();
            endur = plData.nextInt();
            //System.out.print("\nEndurance: " + endur);

            plData.next();
            dex = plData.nextInt();
            //System.out.print("\nDexterity: " + dex);

            plData.next();
            intel = plData.nextInt();
            //System.out.print("\nIntelligence: " + intel);

            plData.next();
            wis = plData.nextInt();
            //System.out.print("\nWisdom: " + wis);
        }

        plData.close();
    }

    public static void raceStatGen() throws FileNotFoundException
    {
        //Assign additional stat points based on race
        if(race.toString().equals("Elf"))
        {
            str += 2;
            endur += -2;
            dex += 4;
            intel += 4;
            wis += 2;
        }

        if(race.toString().equals("Human"))
        {
            str += 2;
            endur += 2;
            dex += 2;
            intel +=2;
            wis +=2;
        }

        if(race.toString().equals("Dwarf"))
        {
            str += 2;
            endur += 4;
            dex += 0;
            intel += 0;
            wis += 4;
        }

        if(race.toString().equals("Orc"))
        {
            str += 4;
            endur += 4;
            dex += 2;
            intel += 0;
            wis += 0;
        }
    }

    public static void classStatGen() throws FileNotFoundException
    {
        //Assign additional stat points based on class
        if(classChoice.toString().equals("Fighter"))
        {
            str += 2;
            endur += 2;
            HpBase = 15;
            HpInc = 6;
            wep = "Spear";
            arm = "Chainmail";
            shl = "Heavy";


        }

        if(classChoice.toString().equals("Ranger"))
        {
            dex += 2;
            wis += 2;
            HpBase = 12;
            HpInc = 6;
            wep = "Dagger";
            arm = "Hide";
            shl = "Light";

        }

        if(classChoice.toString().equals("Cleric"))
        {
            wis += 3;
            HpBase = 12;
            HpInc = 5;
            wep = "Mace";
            arm = "Hide";
            shl = "Heavy";
        }

        if(classChoice.toString().equals("Mage"))
        {
            intel += 3;
            HpBase = 10;
            HpInc = 4;
            wep = "Dagger";
            arm = "Cloth";
            shl = "Arm";
        }

        modGen();
        hp = maxHP;
        AP = maxAP;
    }


    //setCharName deletes another name if it was entered, and takes in the new name
    public static StringBuffer setCharName(String x)
    {
        charName.delete(0,20);
        charName.append(x);
        return charName;
    }

    //setRace is like CharName but is used in case a player can't decide on a race
    //and continues clicking races.
    public static StringBuffer setRace(String x)
    {
        race.delete(0,10);
        race.append(x);
        return race;
    }

    //setClass is like setRace but for classes. It serves the same purpose.
    public static StringBuffer setClass(String x)
    {
        classChoice.delete(0,10);
        classChoice.append(x);
        return classChoice;
    }

    public static StringBuffer getName() throws FileNotFoundException
    {
        InputStream charStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + ".dat");
        Scanner inFile = new Scanner(charStream);

        while(inFile.hasNext())
        {
            temp = inFile.next();
            if(temp.equals("Character:"))
                charName.append(inFile.next());
        }

        inFile.close();
        return charName;
    }

    public static StringBuffer getRace() throws FileNotFoundException
    {
        InputStream charStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + ".dat");
        Scanner inFile = new Scanner(charStream);

        while(inFile.hasNext())
        {
            temp = inFile.next();
            if(temp.equals("Race:"))
                race.append(inFile.next());
        }

        inFile.close();
        return race;
    }

    public static StringBuffer getClassChoice() throws FileNotFoundException
    {
        InputStream charStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + ".dat");
        Scanner inFile = new Scanner(charStream);

        while(inFile.hasNext())
        {
            temp = inFile.next();
            if(temp.equals("Class:"))
                classChoice.append(inFile.next());
        }

        inFile.close();
        return classChoice;
    }

    public static String nameToString(StringBuffer charName)
    {
        String name;
        name = charName.toString();

        return name;
    }

    public static int getLevel() throws FileNotFoundException
    {
        InputStream charStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + "/CharSheet.dat");
        Scanner inFile = new Scanner(charStream);

        while(inFile.hasNext())
        {
            temp = inFile.next();
            if(temp.equals("Level:"))
                level = inFile.nextInt();
        }

        inFile.close();
        return level;
    }

    //Generates stat mods based on current stats
    public static void modGen() throws FileNotFoundException
    {
        BattleData.collectEquipmentData("Weapon", wep);
        BattleData.collectEquipmentData("Armour", arm);
        BattleData.collectEquipmentData("Shield", shl);

        endMod = ((endur - 10) / 2);
        strMod = ((str - 10) / 2);
        dexMod = ((dex - 10) / 2);
        intMod = ((intel - 10) / 2);
        wisMod = ((wis - 10) / 2);

        AC = 10 + (level/2) + dexMod + shlMod + armMod;


        //These if statements verify the player class and assign the Base and IncreaseHp values
        if (classChoice.toString().equals("Fighter"))
        {
            HpBase = 15;
            HpInc = 6;
        }
        if (classChoice.toString().equals("Ranger"))
        {
            HpBase = 12;
            HpInc = 5;
        }
        if (classChoice.toString().equals("Cleric"))
        {
            HpBase = 12;
            HpInc = 5;
        }
        if (classChoice.toString().equals("Mage"))
        {
            HpBase = 10;
            HpInc = 4;
        }

        maxHP = HpBase + endur + (HpInc * level);
        maxAP = 20 + ((endMod/2 + intMod/2) * level);

        //nextLevel = ((level * 250) + 750 + currentLvExp);
    }

    public static void levelUp() throws FileNotFoundException
    {
        //Tests if user's EXP is greater than required EXP to level
        while(EXP >= nextLevel)
        {
            //Levels the character once
            level++;

            JOptionPane.showMessageDialog(null, "Congratulations! You are now level " + level + "!",
                "Level Up!", JOptionPane.INFORMATION_MESSAGE);

            //Sets the currentLvExp to what you had to get to levelUp, resets HP and AP to max,
            //and prints the new data to the character file.
            currentLvExp = nextLevel;

            //Insures that the exp is calculated properly for the next level
            getNextEXP();

            statPoints++;
            modGen();

            hp = maxHP;
            AP = maxAP;

            printStats();
        }
    }

    private static void getNextEXP()
    {
        nextLevel = ((level * 250) + 750 + currentLvExp);
    }

    public static void printStats() throws FileNotFoundException
    {
        PrintWriter outFile = null;
        File charFile;
        URL charUrl = Stats.class.getResource("/data/characters/character" + charNum + "/MinStats.dat");
        try {
            charFile = new File(charUrl.toURI().getPath());
            outFile = new PrintWriter(charFile);
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }

        modGen();
        //Print out character name, race, and class to a file with the
        //name of the character
        outFile.printf("Character: %s%n", charName.toString());
        outFile.printf("Race: %s%n", race.toString());
        outFile.printf("Class: %s%n%n", classChoice.toString());
        //Prints out the character level
        outFile.printf("Level: %d%n", level);
        outFile.printf("EXP: %d%n", EXP);
        outFile.printf("NextLevel: %d%n", nextLevel);
        outFile.printf("StatPoints: %d%n%n", statPoints);
        //Prints out the character stats
        outFile.printf("MaxHP: %d%n", maxHP);
        outFile.printf("CurrentHP: %d%n", hp);
        outFile.printf("MaxAP: %d%n", maxAP);
        outFile.printf("CurrentAP: %d%n", AP);
        outFile.printf("Strength: %d%n", str);
        outFile.printf("Endurance: %d%n", endur);
        outFile.printf("Dexterity: %d%n", dex);
        outFile.printf("Intelligence: %d%n", intel);
        outFile.printf("Wisdom: %d%n%n", wis);

        outFile.close();
    }

    public static void printBlank() throws FileNotFoundException
    {
        PrintWriter outFile = new PrintWriter("Data\\Characters\\Character" + charNum + "\\CharSheet.dat");

        //Print out blank character name, race, and class to a file with the
        //name of the character
        outFile.printf("Character: ------%n");
        outFile.printf("Race: ------%n");
        outFile.printf("Class: ------%n%n");
        //Prints out the blank character level
        outFile.printf("Level:%n");
        outFile.printf("EXP:%n");
        outFile.printf("NextLevel:%n");
        outFile.printf("StatPoints:%n%n");
        //Prints out the blank character stats
        outFile.printf("MaxHP:%n");
        outFile.printf("CurrentHP:%n");
        outFile.printf("MaxAP:%n");
        outFile.printf("CurrentAP:%n");
        outFile.printf("Strength:%n");
        outFile.printf("Endurance:%n");
        outFile.printf("Dexterity:%n");
        outFile.printf("Intelligence:%n");
        outFile.printf("Wisdom:%n%n");

        outFile.close();
    }

    public static void printGear() throws FileNotFoundException
    {
        PrintWriter outFile = new PrintWriter("Data\\Characters\\Character" + charNum + "\\Equipment.dat");

        outFile.printf("Weapon: %s%n", wep);
        outFile.printf("Armor: %s%n", arm);
        outFile.printf("Shield: %s%n%n", shl);

        outFile.close();
    }

    public static void getGear() throws FileNotFoundException
    {
        Scanner plData = new Scanner(new FileReader("Data\\Characters\\Character" + charNum + "\\Equipment.dat"));

        plData.next();
        wep = plData.next();
        //System.out.print("\nWeapon: " + wep);

        plData.next();
        arm = plData.next();
        //System.out.print("\nArmor: " + arm);

        plData.next();
        shl = plData.next();
        //System.out.print("\nShield: " + shl);

        plData.close();
    }

    //Sets the min stats according to what is in the MinStats file
    public static void getMin() throws FileNotFoundException
    {
        InputStream statStream = Stats.class.getResourceAsStream("/data/characters/character" + charNum + "/MinStats.dat");
        Scanner inFile = new Scanner(statStream);

        minStr = inFile.nextInt();
        minEnd = inFile.nextInt();
        minDex = inFile.nextInt();
        minInt = inFile.nextInt();
        minWis = inFile.nextInt();

        inFile.close();
    }

    //Prints out the starting stats as the minimum stats possible
    public static void minStatsPrint() throws FileNotFoundException
    {
        URL statUrl = Stats.class.getResource("/data/characters/character" + charNum + "/MinStats.dat");
        File file = null;
        try {
            file = new File(statUrl.toURI().getPath());
        }
        catch (URISyntaxException e) {
            e.printStackTrace();
        }
        PrintWriter outFile = new PrintWriter(file);

        outFile.printf("%d%n", str);
        outFile.printf("%d%n", endur);
        outFile.printf("%d%n", dex);
        outFile.printf("%d%n", intel);
        outFile.printf("%d%n", wis);

        outFile.close();

        getMin();
    }
}
