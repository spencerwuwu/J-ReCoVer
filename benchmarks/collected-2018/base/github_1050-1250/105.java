// https://searchcode.com/api/result/93978503/

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package advanced.virtual.drug.dealer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;
import javax.swing.Timer;

/**
 *
 * @author ScottyWired
 * This code(?) is horrendous. Hi.
 * I don't even know how to interact with other classes so I just dumped it all here.
 * 
 * TODO Stop allowing to buy negative amount of drugs
 */
public class Main extends javax.swing.JFrame {

    int Cash = 1000;
    int gameSpeed = 50;//milliseconds.  gameSpeed * RepriceCountReset * 1000 = seconds per cycle
    int Heat = 1;
    int HeatCount = 0;
    int RepriceCount = 20;//Initial cycle
    int RepriceCountReset = 20;//Cycle thereafter
    boolean OpenStatus = false;//'true' makes TickTock run
    Random Seed = new Random();//TODO reduce dependance on this, introduce other variables affected by player
    
    public void NL(){
        Report.append("\n");
    }
    public void Feed(){
         int c = Seed.nextInt(1000);
                /*
                Drug/Shares of 1000/Range
                Marijuna        150     0-149
                Purple Weed     50      150-199
                Hashish         100     200-299
                Cocaine         100     300-399
                Crack Cocaine   50      400-449
                LSD             100     450-549
                CrystalMeth     100     550-649
                Opium           150     650-799
                Shrooms         100     800-899
                Heroin          99      900-998
                BlueStuff       1       999
                */
                if(c<149){
                    int s = Integer.parseInt(MarijuanaS.getText());
                    String set = Integer.toString((s+1));
                    MarijuanaS.setText(set);
                }
                else if(c>=150&&c<=199){
                    int s = Integer.parseInt(PurpleWeedS.getText());
                    String set = Integer.toString((s+1));
                    PurpleWeedS.setText(set);
                }
                else if(c>=200&&c<=299){
                    int s = Integer.parseInt(HashishS.getText());
                    String set = Integer.toString((s+1));
                    HashishS.setText(set);
                }
                else if(c>=300&&c<=399){
                    int s = Integer.parseInt(CocaineS.getText());
                    String set = Integer.toString((s+1));
                    CocaineS.setText(set);
                }
                else if(c>=400&&c<=449){
                    int s = Integer.parseInt(CrackCocaineS.getText());
                    String set = Integer.toString((s+1));
                    CrackCocaineS.setText(set);
                }
                else if(c>=450&&c<=549){
                    int s = Integer.parseInt(LSDS.getText());
                    String set = Integer.toString((s+1));
                    LSDS.setText(set);                
                }
                else if(c>=550&&c<=649){
                    int s = Integer.parseInt(CrystalMethS.getText());
                    String set = Integer.toString((s+1));
                    CrystalMethS.setText(set);
                }
                else if(c>=650&&c<=799){
                    int s = Integer.parseInt(OpiumS.getText());
                    String set = Integer.toString((s+1));
                    OpiumS.setText(set);
                }
                else if(c>=800&&c<=899){
                    int s = Integer.parseInt(ShroomsS.getText());
                    String set = Integer.toString((s+1));
                    ShroomsS.setText(set);
                }
                else if(c>=900&&c<=998){
                    int s = Integer.parseInt(HeroinS.getText());
                    String set = Integer.toString((s+1));
                    HeroinS.setText(set);
                }
                else if(c==999){
                    int s = Integer.parseInt(BlueStuffS.getText());
                    String set = Integer.toString((s+1));
                    BlueStuffS.setText(set);
                }
                //Report.append(Integer.toString(c)+"\n");
    }
    /*public void Heating(){
        TODO affects the market prices and chances of raids
    }*/
   
    public void Reprice(){

              /*Marijuana       150     0-149
                Purple Weed     50      150-199
                Hashish         100     200-299
                Cocaine         100     300-399
                Crack Cocaine   50      400-449
                LSD             100     450-549
                CrystalMeth     100     550-649
                Opium           150     650-799
                Shrooms         100     800-899
                Heroin          99      900-998
                BlueStuff       1       999*/
        if(RepriceCount==0){
            //TODO Make these equations interesting, include Heat
            int MR = Seed.nextInt(5)-2;
            int PWR = MR+Seed.nextInt(15)-7;
            int HR = Seed.nextInt(10)-5;
            int CR = Seed.nextInt(30)-15;
            int CCR = Seed.nextInt(50)-25;
            int LR = Seed.nextInt(25)-14;
            int CMR = Seed.nextInt(10)-5;
            int OR = Seed.nextInt(15)-7;
            int SR = Seed.nextInt(10)-5;
            int HER = Seed.nextInt(30)-15;
            int BSR = Seed.nextInt(200)-5;
            
            //TODO Place images next to prices to represent rise or fall
            MarijuanaP.setText(Integer.toString(MR + Integer.parseInt(MarijuanaP.getText())));
            PurpleWeedP.setText(Integer.toString(PWR + Integer.parseInt(PurpleWeedP.getText())));
            HashishP.setText(Integer.toString(HR + Integer.parseInt(HashishP.getText())));
            CocaineP.setText(Integer.toString(CR + Integer.parseInt(CocaineP.getText())));
            CrackCocaineP.setText(Integer.toString(CCR + Integer.parseInt(CrackCocaineP.getText())));
            LSDP.setText(Integer.toString(LR + Integer.parseInt(LSDP.getText())));
            CrystalMethP.setText(Integer.toString(CMR + Integer.parseInt(CrystalMethP.getText())));
            OpiumP.setText(Integer.toString(OR + Integer.parseInt(OpiumP.getText())));
            ShroomsP.setText(Integer.toString(SR + Integer.parseInt(ShroomsP.getText())));
            HeroinP.setText(Integer.toString(HER + Integer.parseInt(HeroinP.getText())));
            BlueStuffP.setText(Integer.toString(BSR + Integer.parseInt(BlueStuffP.getText())));
            
            RepriceCount=RepriceCountReset;
            
        }
        else{
            RepriceCount--;
        }
    }
    
    
    //Tickety let's get this engine running
    public void TickTock(){ 
        ActionListener taskPerformer;
        taskPerformer = new ActionListener(){
            public void actionPerformed(ActionEvent evt){
                CashLbl.setText("$"+Integer.toString(Cash));
                if(OpenStatus==true){
                
                //Feeds supplies into economy
                Feed();
                //Heating();
                Reprice();
                
                }
               
            }
        };
        new Timer(gameSpeed, taskPerformer).start();
    }

    public void Purchase(int su,int pr,int vo){
        if(su<vo){
            Report.append("Not enough in supply.");
            NL();
            return;
        }
        if(Cash<(vo*pr)){
            Report.append("Not enough cash.");
            NL();
            return;
        }
        if(su*pr==(Integer.parseInt(MarijuanaS.getText())*Integer.parseInt(MarijuanaP.getText()))){
            MarijuanaSt.setText(Integer.toString((Integer.parseInt(MarijuanaSt.getText())+vo)));
            MarijuanaS.setText(Integer.toString((Integer.parseInt(MarijuanaS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(PurpleWeedS.getText())*Integer.parseInt(PurpleWeedP.getText()))){
            PurpleWeedSt.setText(Integer.toString((Integer.parseInt(PurpleWeedSt.getText())+vo)));
            PurpleWeedS.setText(Integer.toString((Integer.parseInt(PurpleWeedS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(HashishS.getText())*Integer.parseInt(HashishP.getText()))){
            HashishSt.setText(Integer.toString((Integer.parseInt(HashishSt.getText())+vo)));
            HashishS.setText(Integer.toString((Integer.parseInt(HashishS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CocaineS.getText())*Integer.parseInt(CocaineP.getText()))){
            CocaineSt.setText(Integer.toString((Integer.parseInt(CocaineSt.getText())+vo)));
            CocaineS.setText(Integer.toString((Integer.parseInt(CocaineS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CrackCocaineS.getText())*Integer.parseInt(CrackCocaineP.getText()))){
            CrackCocaineSt.setText(Integer.toString((Integer.parseInt(CrackCocaineSt.getText())+vo)));
            CrackCocaineS.setText(Integer.toString((Integer.parseInt(CrackCocaineS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(LSDS.getText())*Integer.parseInt(LSDP.getText()))){
            LSDSt.setText(Integer.toString((Integer.parseInt(LSDSt.getText())+vo)));
            LSDS.setText(Integer.toString((Integer.parseInt(LSDS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CrystalMethS.getText())*Integer.parseInt(CrystalMethP.getText()))){
            CrystalMethSt.setText(Integer.toString((Integer.parseInt(CrystalMethSt.getText())+vo)));
            CrystalMethS.setText(Integer.toString((Integer.parseInt(CrystalMethS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(BlueStuffS.getText())*Integer.parseInt(BlueStuffP.getText()))){
            BlueStuffSt.setText(Integer.toString((Integer.parseInt(BlueStuffSt.getText())+vo)));
            BlueStuffS.setText(Integer.toString((Integer.parseInt(BlueStuffS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(OpiumS.getText())*Integer.parseInt(OpiumP.getText()))){
            OpiumSt.setText(Integer.toString((Integer.parseInt(OpiumSt.getText())+vo)));
            OpiumS.setText(Integer.toString((Integer.parseInt(OpiumS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(HeroinS.getText())*Integer.parseInt(HeroinP.getText()))){
            HeroinSt.setText(Integer.toString((Integer.parseInt(HeroinSt.getText())+vo)));
            HeroinS.setText(Integer.toString((Integer.parseInt(HeroinS.getText())-vo)));
            Cash-=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(ShroomsS.getText())*Integer.parseInt(ShroomsP.getText()))){
            ShroomsSt.setText(Integer.toString((Integer.parseInt(ShroomsSt.getText())+vo)));
            ShroomsS.setText(Integer.toString((Integer.parseInt(ShroomsS.getText())-vo)));
            Cash-=vo*pr;
            //return;
        }
        
    }
    
    public void Sell(int su, int pr,int vo, int st){
        if(vo>st){
            Report.append("Not enough stocked!");
            return;
        }
        if(su*pr==(Integer.parseInt(MarijuanaS.getText())*Integer.parseInt(MarijuanaP.getText()))){
            MarijuanaSt.setText(Integer.toString((Integer.parseInt(MarijuanaSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        }
        if(su*pr==(Integer.parseInt(PurpleWeedS.getText())*Integer.parseInt(PurpleWeedP.getText()))){
            PurpleWeedSt.setText(Integer.toString((Integer.parseInt(PurpleWeedSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(HashishS.getText())*Integer.parseInt(HashishP.getText()))){
            HashishSt.setText(Integer.toString((Integer.parseInt(HashishSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CocaineS.getText())*Integer.parseInt(CocaineP.getText()))){
            CocaineSt.setText(Integer.toString((Integer.parseInt(CocaineSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CrackCocaineS.getText())*Integer.parseInt(CrackCocaineP.getText()))){
            CrackCocaineSt.setText(Integer.toString((Integer.parseInt(CrackCocaineSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(LSDS.getText())*Integer.parseInt(LSDP.getText()))){
            LSDSt.setText(Integer.toString((Integer.parseInt(LSDSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(CrystalMethS.getText())*Integer.parseInt(CrystalMethP.getText()))){
            CrystalMethSt.setText(Integer.toString((Integer.parseInt(CrystalMethSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(BlueStuffS.getText())*Integer.parseInt(BlueStuffP.getText()))){
            BlueStuffSt.setText(Integer.toString((Integer.parseInt(BlueStuffSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(OpiumS.getText())*Integer.parseInt(OpiumP.getText()))){
            OpiumSt.setText(Integer.toString((Integer.parseInt(OpiumSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(HeroinS.getText())*Integer.parseInt(HeroinP.getText()))){
            HeroinSt.setText(Integer.toString((Integer.parseInt(HeroinSt.getText())-vo)));
            Cash+=vo*pr;
            return;
        } 
        if(su*pr==(Integer.parseInt(ShroomsS.getText())*Integer.parseInt(ShroomsP.getText()))){
            ShroomsSt.setText(Integer.toString((Integer.parseInt(ShroomsSt.getText())-vo)));
            Cash+=vo*pr;
            //return;
        } 
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        MarijuanaS = new javax.swing.JLabel();
        PurpleWeedS = new javax.swing.JLabel();
        HashishS = new javax.swing.JLabel();
        CocaineS = new javax.swing.JLabel();
        CrackCocaineS = new javax.swing.JLabel();
        LSDS = new javax.swing.JLabel();
        CrystalMethS = new javax.swing.JLabel();
        BlueStuffS = new javax.swing.JLabel();
        OpiumS = new javax.swing.JLabel();
        HeroinS = new javax.swing.JLabel();
        ShroomsS = new javax.swing.JLabel();
        MarijuanaP = new javax.swing.JLabel();
        PurpleWeedP = new javax.swing.JLabel();
        HashishP = new javax.swing.JLabel();
        CocaineP = new javax.swing.JLabel();
        CrackCocaineP = new javax.swing.JLabel();
        LSDP = new javax.swing.JLabel();
        CrystalMethP = new javax.swing.JLabel();
        BlueStuffP = new javax.swing.JLabel();
        OpiumP = new javax.swing.JLabel();
        HeroinP = new javax.swing.JLabel();
        ShroomsP = new javax.swing.JLabel();
        jLabel23 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        jLabel25 = new javax.swing.JLabel();
        MarijuanaV = new javax.swing.JSpinner();
        PurpleWeedV = new javax.swing.JSpinner();
        HashishV = new javax.swing.JSpinner();
        CrackCocaineV = new javax.swing.JSpinner();
        CocaineV = new javax.swing.JSpinner();
        LSDV = new javax.swing.JSpinner();
        CrystalMethV = new javax.swing.JSpinner();
        BlueStuffV = new javax.swing.JSpinner();
        OpiumV = new javax.swing.JSpinner();
        HeroinV = new javax.swing.JSpinner();
        ShroomsV = new javax.swing.JSpinner();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        MarijuanaSt = new javax.swing.JLabel();
        PurpleWeedSt = new javax.swing.JLabel();
        HashishSt = new javax.swing.JLabel();
        CocaineSt = new javax.swing.JLabel();
        CrackCocaineSt = new javax.swing.JLabel();
        LSDSt = new javax.swing.JLabel();
        CrystalMethSt = new javax.swing.JLabel();
        BlueStuffSt = new javax.swing.JLabel();
        OpiumSt = new javax.swing.JLabel();
        HeroinSt = new javax.swing.JLabel();
        ShroomsSt = new javax.swing.JLabel();
        MarijunaBuy = new javax.swing.JButton();
        PurpleWeedBuy = new javax.swing.JButton();
        HashishBuy = new javax.swing.JButton();
        CocaineBuy = new javax.swing.JButton();
        CrackCocaineBuy = new javax.swing.JButton();
        LSDBuy = new javax.swing.JButton();
        BlueStuffBuy = new javax.swing.JButton();
        CrystalMethBuy = new javax.swing.JButton();
        OpiumBuy = new javax.swing.JButton();
        ShroomsBuy = new javax.swing.JButton();
        HeroinBuy = new javax.swing.JButton();
        MarijuanaSell = new javax.swing.JButton();
        PurpleWeedSell = new javax.swing.JButton();
        HashishSell = new javax.swing.JButton();
        CocaineSell = new javax.swing.JButton();
        CrackCocaineSell = new javax.swing.JButton();
        LSDSell = new javax.swing.JButton();
        CrystalMethSell = new javax.swing.JButton();
        BlueStuffSell = new javax.swing.JButton();
        OpiumSell = new javax.swing.JButton();
        HeroinSell = new javax.swing.JButton();
        ShroomsSell = new javax.swing.JButton();
        CashLbl = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        HeatLvl = new javax.swing.JProgressBar();
        jScrollPane1 = new javax.swing.JScrollPane();
        Report = new javax.swing.JTextArea();
        btnOpenClosed = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(800, 600));
        setResizable(false);
        getContentPane().setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel1.setText("Marijuana");
        getContentPane().add(jLabel1, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 43, -1, -1));

        jLabel2.setText("Purple Weed");
        getContentPane().add(jLabel2, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 82, -1, -1));

        jLabel3.setText("Hashish");
        getContentPane().add(jLabel3, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 123, -1, -1));

        jLabel4.setText("Cocaine");
        getContentPane().add(jLabel4, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 164, -1, -1));

        jLabel5.setText("Crack Cocaine");
        getContentPane().add(jLabel5, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 205, -1, -1));

        jLabel6.setText("LSD");
        getContentPane().add(jLabel6, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 246, -1, -1));

        jLabel7.setText("Crystal Meth");
        getContentPane().add(jLabel7, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 287, -1, -1));

        jLabel8.setText("The Blue Stuff");
        getContentPane().add(jLabel8, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 328, -1, -1));

        jLabel9.setText("Opium");
        getContentPane().add(jLabel9, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 369, -1, -1));

        jLabel10.setText("Heroin");
        getContentPane().add(jLabel10, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 410, -1, -1));

        jLabel11.setText("Shrooms");
        getContentPane().add(jLabel11, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 451, -1, -1));

        MarijuanaS.setText("0");
        getContentPane().add(MarijuanaS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 43, -1, -1));

        PurpleWeedS.setText("0");
        getContentPane().add(PurpleWeedS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 82, -1, -1));

        HashishS.setText("0");
        getContentPane().add(HashishS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 123, -1, -1));

        CocaineS.setText("0");
        getContentPane().add(CocaineS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 164, -1, -1));

        CrackCocaineS.setText("0");
        getContentPane().add(CrackCocaineS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 205, -1, -1));

        LSDS.setText("0");
        getContentPane().add(LSDS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 246, -1, -1));

        CrystalMethS.setText("0");
        getContentPane().add(CrystalMethS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 287, -1, -1));

        BlueStuffS.setText("0");
        getContentPane().add(BlueStuffS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 328, -1, -1));

        OpiumS.setText("0");
        getContentPane().add(OpiumS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 369, -1, -1));

        HeroinS.setText("0");
        getContentPane().add(HeroinS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 410, -1, -1));

        ShroomsS.setText("0");
        getContentPane().add(ShroomsS, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 451, -1, -1));

        MarijuanaP.setText("50");
        getContentPane().add(MarijuanaP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 43, -1, -1));

        PurpleWeedP.setText("200");
        getContentPane().add(PurpleWeedP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 82, -1, -1));

        HashishP.setText("150");
        getContentPane().add(HashishP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 123, 40, -1));

        CocaineP.setText("300");
        getContentPane().add(CocaineP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 164, -1, -1));

        CrackCocaineP.setText("500");
        getContentPane().add(CrackCocaineP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 205, -1, -1));

        LSDP.setText("250");
        getContentPane().add(LSDP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 246, -1, -1));

        CrystalMethP.setText("300");
        getContentPane().add(CrystalMethP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 287, -1, -1));

        BlueStuffP.setText("2000");
        getContentPane().add(BlueStuffP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 328, -1, -1));

        OpiumP.setText("70");
        getContentPane().add(OpiumP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 369, -1, -1));

        HeroinP.setText("250");
        getContentPane().add(HeroinP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 410, -1, -1));

        ShroomsP.setText("80");
        getContentPane().add(ShroomsP, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 451, -1, -1));

        jLabel23.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel23.setText("Product");
        getContentPane().add(jLabel23, new org.netbeans.lib.awtextra.AbsoluteConstraints(10, 8, -1, -1));

        jLabel24.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel24.setText("Supply");
        getContentPane().add(jLabel24, new org.netbeans.lib.awtextra.AbsoluteConstraints(109, 8, -1, -1));

        jLabel25.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel25.setText("Price");
        getContentPane().add(jLabel25, new org.netbeans.lib.awtextra.AbsoluteConstraints(167, 8, -1, -1));
        getContentPane().add(MarijuanaV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 40, 60, -1));
        getContentPane().add(PurpleWeedV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 79, 60, -1));
        getContentPane().add(HashishV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 120, 60, -1));
        getContentPane().add(CrackCocaineV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 202, 60, -1));
        getContentPane().add(CocaineV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 161, 60, -1));
        getContentPane().add(LSDV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 243, 60, -1));
        getContentPane().add(CrystalMethV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 284, 60, -1));
        getContentPane().add(BlueStuffV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 325, 60, -1));
        getContentPane().add(OpiumV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 366, 60, -1));
        getContentPane().add(HeroinV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 407, 60, -1));
        getContentPane().add(ShroomsV, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 448, 60, -1));

        jLabel12.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel12.setText("Volume");
        getContentPane().add(jLabel12, new org.netbeans.lib.awtextra.AbsoluteConstraints(225, 8, -1, -1));

        jLabel13.setFont(new java.awt.Font("Tahoma", 1, 11)); // NOI18N
        jLabel13.setText("Stock");
        getContentPane().add(jLabel13, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 10, -1, -1));

        MarijuanaSt.setText("0");
        getContentPane().add(MarijuanaSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 40, -1, -1));

        PurpleWeedSt.setText("0");
        getContentPane().add(PurpleWeedSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 82, -1, -1));

        HashishSt.setText("0");
        getContentPane().add(HashishSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 123, -1, -1));

        CocaineSt.setText("0");
        getContentPane().add(CocaineSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 164, -1, -1));

        CrackCocaineSt.setText("0");
        getContentPane().add(CrackCocaineSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 205, -1, -1));

        LSDSt.setText("0");
        getContentPane().add(LSDSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 246, -1, -1));

        CrystalMethSt.setText("0");
        getContentPane().add(CrystalMethSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 287, -1, -1));

        BlueStuffSt.setText("0");
        getContentPane().add(BlueStuffSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 328, -1, -1));

        OpiumSt.setText("0");
        getContentPane().add(OpiumSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 369, -1, -1));

        HeroinSt.setText("0");
        getContentPane().add(HeroinSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 410, -1, -1));

        ShroomsSt.setText("0");
        getContentPane().add(ShroomsSt, new org.netbeans.lib.awtextra.AbsoluteConstraints(303, 451, -1, -1));

        MarijunaBuy.setText("Buy");
        MarijunaBuy.setFocusable(false);
        MarijunaBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MarijunaBuyActionPerformed(evt);
            }
        });
        getContentPane().add(MarijunaBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(360, 40, -1, -1));

        PurpleWeedBuy.setText("Buy");
        PurpleWeedBuy.setFocusable(false);
        PurpleWeedBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PurpleWeedBuyActionPerformed(evt);
            }
        });
        getContentPane().add(PurpleWeedBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 78, -1, -1));

        HashishBuy.setText("Buy");
        HashishBuy.setFocusable(false);
        HashishBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HashishBuyActionPerformed(evt);
            }
        });
        getContentPane().add(HashishBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 119, -1, -1));

        CocaineBuy.setText("Buy");
        CocaineBuy.setFocusable(false);
        CocaineBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CocaineBuyActionPerformed(evt);
            }
        });
        getContentPane().add(CocaineBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 160, -1, -1));

        CrackCocaineBuy.setText("Buy");
        CrackCocaineBuy.setFocusable(false);
        CrackCocaineBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CrackCocaineBuyActionPerformed(evt);
            }
        });
        getContentPane().add(CrackCocaineBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 201, -1, -1));

        LSDBuy.setText("Buy");
        LSDBuy.setFocusable(false);
        LSDBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LSDBuyActionPerformed(evt);
            }
        });
        getContentPane().add(LSDBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 242, -1, -1));

        BlueStuffBuy.setText("Buy");
        BlueStuffBuy.setFocusable(false);
        BlueStuffBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BlueStuffBuyActionPerformed(evt);
            }
        });
        getContentPane().add(BlueStuffBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 324, -1, -1));

        CrystalMethBuy.setText("Buy");
        CrystalMethBuy.setFocusable(false);
        CrystalMethBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CrystalMethBuyActionPerformed(evt);
            }
        });
        getContentPane().add(CrystalMethBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 283, -1, -1));

        OpiumBuy.setText("Buy");
        OpiumBuy.setFocusable(false);
        OpiumBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpiumBuyActionPerformed(evt);
            }
        });
        getContentPane().add(OpiumBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 365, -1, -1));

        ShroomsBuy.setText("Buy");
        ShroomsBuy.setFocusable(false);
        ShroomsBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShroomsBuyActionPerformed(evt);
            }
        });
        getContentPane().add(ShroomsBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 447, -1, -1));

        HeroinBuy.setText("Buy");
        HeroinBuy.setFocusable(false);
        HeroinBuy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HeroinBuyActionPerformed(evt);
            }
        });
        getContentPane().add(HeroinBuy, new org.netbeans.lib.awtextra.AbsoluteConstraints(361, 406, -1, -1));

        MarijuanaSell.setText("Sell");
        MarijuanaSell.setFocusable(false);
        MarijuanaSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                MarijuanaSellActionPerformed(evt);
            }
        });
        getContentPane().add(MarijuanaSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(420, 40, -1, -1));

        PurpleWeedSell.setText("Sell");
        PurpleWeedSell.setFocusable(false);
        PurpleWeedSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                PurpleWeedSellActionPerformed(evt);
            }
        });
        getContentPane().add(PurpleWeedSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 78, -1, -1));

        HashishSell.setText("Sell");
        HashishSell.setFocusable(false);
        HashishSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HashishSellActionPerformed(evt);
            }
        });
        getContentPane().add(HashishSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 119, -1, -1));

        CocaineSell.setText("Sell");
        CocaineSell.setFocusable(false);
        CocaineSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CocaineSellActionPerformed(evt);
            }
        });
        getContentPane().add(CocaineSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 160, -1, -1));

        CrackCocaineSell.setText("Sell");
        CrackCocaineSell.setFocusable(false);
        CrackCocaineSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CrackCocaineSellActionPerformed(evt);
            }
        });
        getContentPane().add(CrackCocaineSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 201, -1, -1));

        LSDSell.setText("Sell");
        LSDSell.setFocusable(false);
        LSDSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                LSDSellActionPerformed(evt);
            }
        });
        getContentPane().add(LSDSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 242, -1, -1));

        CrystalMethSell.setText("Sell");
        CrystalMethSell.setFocusable(false);
        CrystalMethSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CrystalMethSellActionPerformed(evt);
            }
        });
        getContentPane().add(CrystalMethSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 283, -1, -1));

        BlueStuffSell.setText("Sell");
        BlueStuffSell.setFocusable(false);
        BlueStuffSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BlueStuffSellActionPerformed(evt);
            }
        });
        getContentPane().add(BlueStuffSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 324, -1, -1));

        OpiumSell.setText("Sell");
        OpiumSell.setFocusable(false);
        OpiumSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                OpiumSellActionPerformed(evt);
            }
        });
        getContentPane().add(OpiumSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 365, -1, -1));

        HeroinSell.setText("Sell");
        HeroinSell.setFocusable(false);
        HeroinSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HeroinSellActionPerformed(evt);
            }
        });
        getContentPane().add(HeroinSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 406, -1, -1));

        ShroomsSell.setText("Sell");
        ShroomsSell.setFocusable(false);
        ShroomsSell.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ShroomsSellActionPerformed(evt);
            }
        });
        getContentPane().add(ShroomsSell, new org.netbeans.lib.awtextra.AbsoluteConstraints(418, 447, -1, -1));

        CashLbl.setFont(new java.awt.Font("Tahoma", 0, 18)); // NOI18N
        CashLbl.setText("ready!");
        getContentPane().add(CashLbl, new org.netbeans.lib.awtextra.AbsoluteConstraints(530, 50, -1, -1));

        jLabel15.setText("Heat");
        getContentPane().add(jLabel15, new org.netbeans.lib.awtextra.AbsoluteConstraints(580, 99, -1, -1));

        HeatLvl.setToolTipText("");
        HeatLvl.setName(""); // NOI18N
        getContentPane().add(HeatLvl, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 100, 230, -1));

        Report.setEditable(false);
        Report.setColumns(20);
        Report.setRows(5);
        Report.setFocusable(false);
        jScrollPane1.setViewportView(Report);

        getContentPane().add(jScrollPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(480, 120, 230, 340));

        btnOpenClosed.setFont(new java.awt.Font("Cordia New", 1, 24)); // NOI18N
        btnOpenClosed.setForeground(new java.awt.Color(204, 0, 0));
        btnOpenClosed.setText("PAUSED");
        btnOpenClosed.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        btnOpenClosed.setName(""); // NOI18N
        btnOpenClosed.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnOpenClosedActionPerformed(evt);
            }
        });
        getContentPane().add(btnOpenClosed, new org.netbeans.lib.awtextra.AbsoluteConstraints(610, 40, -1, -1));

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void MarijunaBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MarijunaBuyActionPerformed
        Purchase(Integer.parseInt(MarijuanaS.getText()), Integer.parseInt(MarijuanaP.getText()), (int) MarijuanaV.getValue());
    }//GEN-LAST:event_MarijunaBuyActionPerformed

    private void PurpleWeedBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PurpleWeedBuyActionPerformed
        Purchase(Integer.parseInt(PurpleWeedS.getText()), Integer.parseInt(PurpleWeedP.getText()), (int) PurpleWeedV.getValue());
    }//GEN-LAST:event_PurpleWeedBuyActionPerformed

    private void HashishBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HashishBuyActionPerformed
        Purchase(Integer.parseInt(HashishS.getText()), Integer.parseInt(HashishP.getText()), (int) HashishV.getValue());
    }//GEN-LAST:event_HashishBuyActionPerformed

    private void CocaineBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CocaineBuyActionPerformed
        Purchase(Integer.parseInt(CocaineS.getText()), Integer.parseInt(CocaineP.getText()), (int) CocaineV.getValue());
    }//GEN-LAST:event_CocaineBuyActionPerformed

    private void CrackCocaineBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CrackCocaineBuyActionPerformed
        Purchase(Integer.parseInt(CrackCocaineS.getText()), Integer.parseInt(CrackCocaineP.getText()), (int) CrackCocaineV.getValue());
    }//GEN-LAST:event_CrackCocaineBuyActionPerformed

    private void LSDBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LSDBuyActionPerformed
        Purchase(Integer.parseInt(LSDS.getText()), Integer.parseInt(LSDP.getText()), (int) LSDV.getValue());
    }//GEN-LAST:event_LSDBuyActionPerformed

    private void BlueStuffBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BlueStuffBuyActionPerformed
        Purchase(Integer.parseInt(BlueStuffS.getText()), Integer.parseInt(BlueStuffP.getText()), (int) BlueStuffV.getValue());
    }//GEN-LAST:event_BlueStuffBuyActionPerformed

    private void CrystalMethBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CrystalMethBuyActionPerformed
        Purchase(Integer.parseInt(CrystalMethS.getText()), Integer.parseInt(CrystalMethP.getText()), (int) CrystalMethV.getValue());
    }//GEN-LAST:event_CrystalMethBuyActionPerformed

    private void OpiumBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpiumBuyActionPerformed
        Purchase(Integer.parseInt(OpiumS.getText()), Integer.parseInt(OpiumP.getText()), (int) OpiumV.getValue());
    }//GEN-LAST:event_OpiumBuyActionPerformed

    private void ShroomsBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShroomsBuyActionPerformed
        Purchase(Integer.parseInt(ShroomsS.getText()), Integer.parseInt(ShroomsP.getText()), (int) ShroomsV.getValue());
    }//GEN-LAST:event_ShroomsBuyActionPerformed

    private void HeroinBuyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HeroinBuyActionPerformed
        Purchase(Integer.parseInt(HeroinS.getText()), Integer.parseInt(HeroinP.getText()), (int) HeroinV.getValue());
    }//GEN-LAST:event_HeroinBuyActionPerformed

    private void btnOpenClosedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnOpenClosedActionPerformed
        if(OpenStatus==false){
            btnOpenClosed.setText("  OPEN  ");
            OpenStatus=true;
            btnOpenClosed.setForeground(new java.awt.Color(0, 204, 0));
        }
        else if(OpenStatus==true){
            btnOpenClosed.setText("PAUSED");
            OpenStatus=false;
            btnOpenClosed.setForeground(new java.awt.Color(204, 0, 0));
        }
    }//GEN-LAST:event_btnOpenClosedActionPerformed

    private void MarijuanaSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_MarijuanaSellActionPerformed
        Sell(Integer.parseInt(MarijuanaS.getText()), Integer.parseInt(MarijuanaP.getText()), (int) MarijuanaV.getValue(), Integer.parseInt(MarijuanaSt.getText()));
    }//GEN-LAST:event_MarijuanaSellActionPerformed

    private void PurpleWeedSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_PurpleWeedSellActionPerformed
        Sell(Integer.parseInt(PurpleWeedS.getText()), Integer.parseInt(PurpleWeedP.getText()), (int) PurpleWeedV.getValue(), Integer.parseInt(PurpleWeedSt.getText()));
    }//GEN-LAST:event_PurpleWeedSellActionPerformed

    private void HashishSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HashishSellActionPerformed
        Sell(Integer.parseInt(HashishS.getText()), Integer.parseInt(HashishP.getText()), (int) HashishV.getValue(), Integer.parseInt(HashishSt.getText()));
    }//GEN-LAST:event_HashishSellActionPerformed

    private void CocaineSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CocaineSellActionPerformed
        Sell(Integer.parseInt(CocaineS.getText()), Integer.parseInt(CocaineP.getText()), (int) CocaineV.getValue(), Integer.parseInt(CocaineSt.getText()));
    }//GEN-LAST:event_CocaineSellActionPerformed

    private void CrackCocaineSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CrackCocaineSellActionPerformed
        Sell(Integer.parseInt(CrackCocaineS.getText()), Integer.parseInt(CrackCocaineP.getText()), (int) CrackCocaineV.getValue(), Integer.parseInt(CrackCocaineSt.getText()));
    }//GEN-LAST:event_CrackCocaineSellActionPerformed

    private void LSDSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_LSDSellActionPerformed
        Sell(Integer.parseInt(LSDS.getText()), Integer.parseInt(LSDP.getText()), (int) LSDV.getValue(), Integer.parseInt(LSDSt.getText()));
    }//GEN-LAST:event_LSDSellActionPerformed

    private void CrystalMethSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CrystalMethSellActionPerformed
        Sell(Integer.parseInt(CrystalMethS.getText()), Integer.parseInt(CrystalMethP.getText()), (int) CrystalMethV.getValue(), Integer.parseInt(CrystalMethSt.getText()));
    }//GEN-LAST:event_CrystalMethSellActionPerformed

    private void BlueStuffSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BlueStuffSellActionPerformed
        Sell(Integer.parseInt(BlueStuffS.getText()), Integer.parseInt(BlueStuffP.getText()), (int) BlueStuffV.getValue(), Integer.parseInt(BlueStuffSt.getText()));
    }//GEN-LAST:event_BlueStuffSellActionPerformed

    private void OpiumSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_OpiumSellActionPerformed
        Sell(Integer.parseInt(OpiumS.getText()), Integer.parseInt(OpiumP.getText()), (int) OpiumV.getValue(), Integer.parseInt(OpiumSt.getText()));
    }//GEN-LAST:event_OpiumSellActionPerformed

    private void HeroinSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_HeroinSellActionPerformed
        Sell(Integer.parseInt(HeroinS.getText()), Integer.parseInt(HeroinP.getText()), (int) HeroinV.getValue(), Integer.parseInt(HeroinSt.getText()));
    }//GEN-LAST:event_HeroinSellActionPerformed

    private void ShroomsSellActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ShroomsSellActionPerformed
        Sell(Integer.parseInt(ShroomsS.getText()), Integer.parseInt(ShroomsP.getText()), (int) ShroomsV.getValue(), Integer.parseInt(ShroomsSt.getText()));
    }//GEN-LAST:event_ShroomsSellActionPerformed

    /**
     * @param args the command line arguments
     */
    public Main() {
        initComponents();
    }
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Main.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        Main Active = new Main();
        Active.setVisible(true);
        Active.TickTock();
    }
//<editor-fold>
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton BlueStuffBuy;
    private javax.swing.JLabel BlueStuffP;
    private javax.swing.JLabel BlueStuffS;
    private javax.swing.JButton BlueStuffSell;
    private javax.swing.JLabel BlueStuffSt;
    private javax.swing.JSpinner BlueStuffV;
    private javax.swing.JLabel CashLbl;
    private javax.swing.JButton CocaineBuy;
    private javax.swing.JLabel CocaineP;
    private javax.swing.JLabel CocaineS;
    private javax.swing.JButton CocaineSell;
    private javax.swing.JLabel CocaineSt;
    private javax.swing.JSpinner CocaineV;
    private javax.swing.JButton CrackCocaineBuy;
    private javax.swing.JLabel CrackCocaineP;
    private javax.swing.JLabel CrackCocaineS;
    private javax.swing.JButton CrackCocaineSell;
    private javax.swing.JLabel CrackCocaineSt;
    private javax.swing.JSpinner CrackCocaineV;
    private javax.swing.JButton CrystalMethBuy;
    private javax.swing.JLabel CrystalMethP;
    private javax.swing.JLabel CrystalMethS;
    private javax.swing.JButton CrystalMethSell;
    private javax.swing.JLabel CrystalMethSt;
    private javax.swing.JSpinner CrystalMethV;
    private javax.swing.JButton HashishBuy;
    private javax.swing.JLabel HashishP;
    private javax.swing.JLabel HashishS;
    private javax.swing.JButton HashishSell;
    private javax.swing.JLabel HashishSt;
    private javax.swing.JSpinner HashishV;
    private javax.swing.JProgressBar HeatLvl;
    private javax.swing.JButton HeroinBuy;
    private javax.swing.JLabel HeroinP;
    private javax.swing.JLabel HeroinS;
    private javax.swing.JButton HeroinSell;
    private javax.swing.JLabel HeroinSt;
    private javax.swing.JSpinner HeroinV;
    private javax.swing.JButton LSDBuy;
    private javax.swing.JLabel LSDP;
    private javax.swing.JLabel LSDS;
    private javax.swing.JButton LSDSell;
    private javax.swing.JLabel LSDSt;
    private javax.swing.JSpinner LSDV;
    private javax.swing.JLabel MarijuanaP;
    private javax.swing.JLabel MarijuanaS;
    private javax.swing.JButton MarijuanaSell;
    private javax.swing.JLabel MarijuanaSt;
    private javax.swing.JSpinner MarijuanaV;
    private javax.swing.JButton MarijunaBuy;
    private javax.swing.JButton OpiumBuy;
    private javax.swing.JLabel OpiumP;
    private javax.swing.JLabel OpiumS;
    private javax.swing.JButton OpiumSell;
    private javax.swing.JLabel OpiumSt;
    private javax.swing.JSpinner OpiumV;
    private javax.swing.JButton PurpleWeedBuy;
    private javax.swing.JLabel PurpleWeedP;
    private javax.swing.JLabel PurpleWeedS;
    private javax.swing.JButton PurpleWeedSell;
    private javax.swing.JLabel PurpleWeedSt;
    private javax.swing.JSpinner PurpleWeedV;
    private javax.swing.JTextArea Report;
    private javax.swing.JButton ShroomsBuy;
    private javax.swing.JLabel ShroomsP;
    private javax.swing.JLabel ShroomsS;
    private javax.swing.JButton ShroomsSell;
    private javax.swing.JLabel ShroomsSt;
    private javax.swing.JSpinner ShroomsV;
    private javax.swing.JButton btnOpenClosed;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel23;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}//</editor-fold>

