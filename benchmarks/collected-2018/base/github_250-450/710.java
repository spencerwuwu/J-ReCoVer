// https://searchcode.com/api/result/97408701/

package ru.ekozoch.ksediffusionproject;

import android.app.Activity;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.ui.TableModel;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.PointLabelFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.androidplot.xy.XYSeries;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static java.lang.Math.*;

public class MainActivity extends Activity {
    Random randVal = new Random(System.currentTimeMillis());

    private XYPlot plot;
    LinearLayout layoutInteractive, layoutValues;
    EditText etTemp, etAmount;
    TextView tvOffset, tvSteps, tvDelay;
    SeekBar stepsBar, delayBar;
    Button btnDraw;

    private int array_size=150;
    private int T = 10000;
    private int steps=10;
    private int delay=100;

    int action;

    List<AsyncTask> asyncTaskList;

    Number[] seriesXNumbers;
    Number[] seriesYNumbers;
    Number[] seriesXOriginal;
    Number[] seriesYOriginal;

    LineAndPointFormatter series2Format;
    XYSeries series;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        action = 0;

        btnDraw = (Button) findViewById(R.id.btnDraw);
        tvOffset = (TextView) findViewById(R.id.tvOffset);
        plot = (XYPlot) findViewById(R.id.mySimpleXYPlot);
        stepsBar = (SeekBar) findViewById(R.id.sbSteps);
        delayBar = (SeekBar) findViewById(R.id.sbDelay);
        tvSteps = (TextView) findViewById(R.id.tvSteps);
        tvDelay = (TextView) findViewById(R.id.tvDelay);
        layoutInteractive = (LinearLayout) findViewById(R.id.layoutInteractive);
        layoutValues = (LinearLayout) findViewById(R.id.layoutValues);
        etAmount = (EditText) findViewById(R.id.etElectronAmount);
        etTemp = (EditText) findViewById(R.id.etTemperature);

        tvSteps.setText("10");
        tvDelay.setText("100");
        asyncTaskList = new ArrayList<AsyncTask>();

        layoutInteractive.setVisibility(View.GONE);
        layoutValues.setVisibility(View.VISIBLE);

        delayBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvDelay.setText(Integer.toString(progress * 20) + " ");
                delay = progress * 20;
            }
        });
        stepsBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(progress==100){
                    tvSteps.setText("");
                    steps = 10000;
                }
                else {
                    tvSteps.setText(Integer.toString(progress + 1));
                    steps = progress + 1;
                }
            }
        });
        btnDraw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(action==0){
                    if(etAmount.getText()!=null)
                        array_size = Integer.parseInt(etAmount.getText().toString());
                    else Toast.makeText(MainActivity.this, "  ", Toast.LENGTH_SHORT).show();

                    if(etTemp.getText()!=null)
                        T = Integer.parseInt(etTemp.getText().toString());
                    else Toast.makeText(MainActivity.this, "  ", Toast.LENGTH_SHORT).show();
                    initData();
                    layoutInteractive.setVisibility(View.VISIBLE);
                    layoutValues.setVisibility(View.GONE);
                    btnDraw.setText("");
                    action=1;
                }
                else if(action==1){
                    action=2;
                    btnDraw.setText("");
                    for (int i = 0; i < steps; ++i) {
                        ReloadTask task = new ReloadTask();
                        asyncTaskList.add(task);
                        task.execute();
                    }
                }
                else if(action==2){
                    action=1;
                    for(AsyncTask task: asyncTaskList) task.cancel(true);
                    btnDraw.setText("");
                }
            }
        });
    }

    void initData(){
        action=0;
        btnDraw.setText("");
        // initialize our XYPlot reference:
        for(AsyncTask task: asyncTaskList) task.cancel(true);

        layoutInteractive.setVisibility(View.GONE);
        layoutValues.setVisibility(View.VISIBLE);

        plot.clear();
        plot.setDomainBoundaries(0, BoundaryMode.FIXED, 100, BoundaryMode.FIXED);
        plot.setRangeBoundaries(0, BoundaryMode.FIXED, 100, BoundaryMode.FIXED);

        // Create a couple arrays of xy-values to plot:
        seriesXOriginal = generateNumberArray(49f, 51f, false);
        seriesYOriginal = generateNumberArray(49f, 51f,  false);
        seriesXNumbers = seriesXOriginal;
        seriesYNumbers = seriesYOriginal;

        // Turn the above arrays into XYSeries':
        series = new SimpleXYSeries(Arrays.asList(seriesXNumbers), Arrays.asList(seriesYNumbers), "electrons");

        // same as above:
        series2Format = new LineAndPointFormatter(Color.TRANSPARENT, Color.GREEN, Color.TRANSPARENT, null);

        plot.addSeries(series, series2Format);

        // reduce the number of range labels
        plot.setTicksPerRangeLabel(3);
        plot.getGraphWidget().setDomainLabelOrientation(-45);
        plot.redraw();

        tvOffset.setText("");
    }

    private int generateRandomDirection(){
        return (int) generateRandom(100, 0, false);
    }

    private void generateDiffusion(double offset){
        Number[] seriesNumbers_X = new Number[array_size];
        Number[] seriesNumbers_Y = new Number[array_size];
        for (int i=0; i<array_size; ++i){
            int angle = generateRandomDirection();
            seriesNumbers_Y[i] = seriesYNumbers[i].doubleValue() + manageYOffset(angle, offset);
            seriesNumbers_X[i] = seriesXNumbers[i].doubleValue() + manageXOffset(angle, offset);
        }
        seriesXNumbers = seriesNumbers_X;
        seriesYNumbers = seriesNumbers_Y;
    }

    private double manageYOffset(double angle, double offset){
        if(angle<=25){
            return offset*sin(2*Math.PI*(angle/100));
        }
        if(angle<=50){
            return offset*sin(2*Math.PI*((50-angle)/100));
        }
        if(angle<=75){
            return -offset*sin(2*Math.PI*((angle-50)/100));
        }
        if(angle<=100){
            return -offset*sin(2*Math.PI*((100-angle)/100));
        }
        return 0;
    }

    private double manageXOffset(double angle, double offset){
        if(angle<=25){
            return offset*cos(2 * Math.PI * (angle / 100));
        }
        if(angle<=50){
            return -offset*cos(2 * Math.PI * ((50 - angle) / 100));
        }
        if(angle<=75){
            return -offset*cos(2 * Math.PI * ((angle - 50) / 100));
        }
        if(angle<=100){
            return offset*cos(2 * Math.PI * ((100 - angle) / 100));
        }
        return 0;
    }


    private Number[] generateNumberArray(double maxValue, double minValue, boolean normal){
        Number[] seriesNumbers = new Number[array_size];
        for (int i=0; i<array_size; ++i) seriesNumbers[i] = generateRandom(maxValue, minValue, normal);
        return seriesNumbers;
    }

    private double generateRandom(double top, double bottom, boolean normal){
        if(normal==true) return generateBoxMuller(bottom);
        double delta = top - bottom;
        double tmp = randVal.nextDouble()*delta + bottom;
        return tmp;
    }

    double generateBoxMuller(double bottom){
        double s, x, y;
        do{
            x=generateRandom(1, 0, false);
            y=generateRandom(1, 0, false);
            s=x*x+y*y;
        }while (s==0 || s>1);
        return bottom + x*sqrt((-2*log10(s))/s);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            initData();
            plot.clear();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public class ReloadTask extends AsyncTask{
        double offset;
        double diffusion;
        double time;

        @Override
        protected Object doInBackground(Object[] params) {
            generateDiffusion(1);
            //seriesYNumbers = generateDiffusion(seriesXNumbers, 1, "Y");
            series = new SimpleXYSeries(Arrays.asList(seriesXNumbers), Arrays.asList(seriesYNumbers), "electrons");
            offset = countOffset();
            diffusion = countDiffusion();
            time = countTime();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Object o) {
            plot.clear();
            plot.addSeries(series, series2Format);
            plot.redraw();
            tvOffset.setText("D=" + diffusion +" ^2/\nt=" + time +" ");
            super.onPostExecute(o);
        }

        public double countDiffusion(){
            double k = 1.38 * pow(10, -23);
            double m = 9.1 * pow(10, -31);
            return sqrt((k*T*offset)/(2*m));
        }

        public double countOffset(){
            double res=0;
            for(int i=0; i<array_size; ++i){
                res+= pow(10, -9)*pow(10,-9)*(seriesXNumbers[i].doubleValue()-seriesXOriginal[i].doubleValue())*(seriesXNumbers[i].doubleValue()-seriesXOriginal[i].doubleValue());
                res+=pow(10, -9)*pow(10,-9)*(seriesYNumbers[i].doubleValue()-seriesYOriginal[i].doubleValue())*(seriesYNumbers[i].doubleValue()-seriesYOriginal[i].doubleValue());
            }
            res /= array_size;
            return res;
        }

        public double countTime(){
            return offset/(2*diffusion);
        }


    }

}

