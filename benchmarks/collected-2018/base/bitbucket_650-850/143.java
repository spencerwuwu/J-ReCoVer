// https://searchcode.com/api/result/59791285/

package com.smscoin.android.payment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.smscoin.android.payment.adapters.CountriesAdapter;
import com.smscoin.android.payment.adapters.ProvidersAdapter;
import com.smscoin.android.payment.adapters.RateListAdapter;

/**
 * @author SmsCoin
 * 
 */
public class SmscoinActivity extends Activity {
    private static final String LOGTAG = SmscoinActivity.class.getSimpleName();



    private ConnectivityManager connectivityManager;
    private ProgressDialog processDialog;
    private Handler mHandler;
    private SharedPreferences appSettings;
    private JSONArray rates, countries;
    private JSONObject persisted;
    private String mcc, mnc;
    private PaymentRequest paymentRequest;
    private int country_selector_pos, provider_selector_pos;
    private Rate currentRate;
    private RateListAdapter adapter;

    /**
     * Save the date of the last rate update
     */
    private void changeLastUpdateDate() {
        Editor editor = appSettings.edit();
        editor.putLong(paymentRequest.getServiceId(), new Date().getTime());
        editor.commit();
    }

    /**
     * Fill rates from source and copy it to sendbox area
     * 
     * @param is The InputStream of the source rates
     * @return Return the list of rates in JSON format
     * @throws IOException
     * @throws JSONException
     */
    private JSONArray copyAndReadFile(InputStream is) throws IOException, JSONException {
        Log.d(SmscoinActivity.LOGTAG, "Start fill rates from local file and copy it to sendbox");

        FileOutputStream fOut = openFileOutput(paymentRequest.getServiceId()+".tmp", Context.MODE_PRIVATE);
        InputStreamReader inputreader = new InputStreamReader(is, "UTF-8");
        OutputStreamWriter osw = new OutputStreamWriter(fOut);
        BufferedReader buffreader = new BufferedReader(inputreader);

        String line;
        StringBuilder builder = new StringBuilder();
        // read every line of the file into the line-variable, on line at the
        // time
        while ((line = buffreader.readLine()) != null) {
            builder.append(line);
            osw.write(line);
        }

        // close the file and streams
        is.close();
        inputreader.close();
        buffreader.close();
        osw.close();
        fOut.close();

        // move data from temp file to original
        File temp = this.getFileStreamPath(paymentRequest.getServiceId()+".tmp");
        temp.renameTo(this.getFileStreamPath(paymentRequest.getServiceId()+".json"));

        Log.d(SmscoinActivity.LOGTAG, "Fill rates from local file with copy success");
        return new JSONArray(builder.toString().substring(15));
    }

    /**
     * Download rates from specific url
     * 
     * @return Return the list of rates in JSON format
     * @throws IOException
     * @throws JSONException
     */
    private JSONArray downloadRates() throws IOException, JSONException {
        JSONArray rates;
        URL url = new URL(PaymentActivity.RATES_URL+paymentRequest.getServiceId()+"/all");

        /* Open a connection to that URL. */
        URLConnection ucon = url.openConnection();
        InputStream is = ucon.getInputStream();
        rates = copyAndReadFile(is);

        return rates;
    }

    /**
     * Load country list organized by mcc mnc
     * 
     * @return Return object that contains countries organized by mcc/mnc in JSON format
     * @throws IOException
     * @throws JSONException
     */
    private JSONObject loadHardcodedList() throws IOException, JSONException {
        Log.d(SmscoinActivity.LOGTAG, "Start loading hardcoded list");
        InputStream is = this.getResources().openRawResource(R.raw.mcc_mnc_mini);
        InputStreamReader inputreader = new InputStreamReader(is, "UTF-8");
        BufferedReader buffreader = new BufferedReader(inputreader);

        String line;
        StringBuilder builder = new StringBuilder();
        // read every line of the file into the line-variable, on line at the
        // time
        while ((line = buffreader.readLine()) != null) {
            builder.append(line);
        }

        // close the file and streams
        is.close();
        inputreader.close();
        buffreader.close();
        Log.d(SmscoinActivity.LOGTAG, "Loading hardcoded list success");
        return new JSONObject(builder.toString());
    }

    /**
     * Fill rates from source file in sendbox area
     * 
     * @return
     * @throws JSONException
     * @throws IOException
     */
    private JSONArray loadRatesFromFile() throws JSONException, IOException {
        Log.d(SmscoinActivity.LOGTAG, "Start filling rates from local file");
        InputStream is = this.openFileInput(paymentRequest.getServiceId()+".json");
        InputStreamReader inputreader = new InputStreamReader(is, "UTF-8");
        BufferedReader buffreader = new BufferedReader(inputreader);

        String line;
        StringBuilder builder = new StringBuilder();
        // read every line of the file into the line-variable, on line at the
        // time
        while ((line = buffreader.readLine()) != null) {
            builder.append(line);
        }

        // close the file and streams
        is.close();
        inputreader.close();
        buffreader.close();
        Log.d(SmscoinActivity.LOGTAG, "Filling rates from local file success");
        return new JSONArray(builder.toString().substring(15));
    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(SmscoinActivity.LOGTAG, "Init Smscoin Activity");

        setContentView(R.layout.smscoin_view);

        // make dim behind dialog
        WindowManager.LayoutParams lp = this.getWindow().getAttributes();
        lp.dimAmount = 0.4f;
        this.getWindow().setAttributes(lp);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        mHandler = new Handler();

        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        appSettings = PreferenceManager.getDefaultSharedPreferences(this);

        country_selector_pos = 0;
        provider_selector_pos = 0;
        mcc = getIntent().getStringExtra(PaymentActivity.EXTRA_MCC);
        mnc = getIntent().getStringExtra(PaymentActivity.EXTRA_MNC);
        paymentRequest = (PaymentRequest) getIntent().getSerializableExtra(PaymentActivity.EXTRA_PAYMENT_REQUEST);

        processDialog = ProgressDialog.show(this, null, getString(R.string.please_wait), true, false);

        Runnable loader = new Runnable() {

            public void run() {
                long last_updated = appSettings.getLong(paymentRequest.getServiceId(), 0);
                try {
                    // check if rates need to be updated from network
                    if (last_updated == 0 || (new Date().getTime() - last_updated) > PaymentActivity.RATES_CACHE_PERIOD) {
                        // check for internet availability
                        if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected()) {
                            Log.d(SmscoinActivity.LOGTAG, "Try to download rates from network");
                            try {
                                rates = downloadRates();
                                changeLastUpdateDate();
                            } catch (IOException e) {
                                Log.d(SmscoinActivity.LOGTAG, "Load rates from network fail. Using local file");
                                if (last_updated == 0) {
                                    InputStream is = SmscoinActivity.this.getResources().openRawResource(
                                            getResources().getIdentifier("t_"+paymentRequest.getServiceId(), "raw", getPackageName()));
                                    rates = copyAndReadFile(is);
                                    changeLastUpdateDate();
                                } else {
                                    rates = loadRatesFromFile();
                                }
                            }
                            // call function in UI thread
                            mHandler.post(new Runnable() {
                                public void run() {
                                    onLoadRatesSuccess(rates);
                                }
                            });
                        } else {
                            Log.d(SmscoinActivity.LOGTAG, "Load local rates");
                            if (last_updated == 0) {
                                InputStream is = SmscoinActivity.this.getResources().openRawResource(
                                        getResources().getIdentifier("t_"+paymentRequest.getServiceId(), "raw", getPackageName()));
                                rates = copyAndReadFile(is);
                                // call function in UI thread
                                mHandler.post(new Runnable() {
                                    public void run() {
                                        onLoadRatesSuccess(rates);
                                    }
                                });
                            } else {
                                rates = loadRatesFromFile();
                                // call function in UI thread
                                mHandler.post(new Runnable() {
                                    public void run() {
                                        onLoadRatesSuccess(rates);
                                    }
                                });
                            }
                        }
                    } else {
                        rates = loadRatesFromFile();
                        // call function in UI thread
                        mHandler.post(new Runnable() {
                            public void run() {
                                onLoadRatesSuccess(rates);
                            }
                        });
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    onLoadRatesError();
                } catch (IOException e) {
                    e.printStackTrace();
                    onLoadRatesError();
                }
            }
        };
        // load rates in different thread
        new Thread(loader).start();

    }

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onKeyDown(int, android.view.KeyEvent)
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            View dialog_view = findViewById(R.id.dialog_body);
            if (dialog_view.getVisibility() == View.VISIBLE) {
                finish();
            } else {
                findViewById(R.id.dialog_selection).setVisibility(View.GONE);
                dialog_view.setVisibility(View.VISIBLE);
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    /**
     * Called if loading rates are fail
     */
    private void onLoadRatesError() {
        processDialog.dismiss();
        Intent data = new Intent();
        data.putExtra("error", "Error loading rates");
        this.setResult(Activity.RESULT_CANCELED, data);
        finish();
    }

    /**
     * Called when loading rates are success
     * 
     * @param rates List of loaded rates in JSON format
     */
    private void onLoadRatesSuccess(JSONArray rates) {
        processDialog.dismiss();

        try {

            String country = "unknown";
            String provider_code = "unknown";

            // check if country are saved in preferences
            if (appSettings.getString("selected_country", null) != null) {
                country = appSettings.getString("selected_country", null);
                provider_code = appSettings.getString("selected_provider", null);

            } else {

                // load hardcoded list of countries, organized by mcc mnc
                if (persisted == null) {
                    persisted = loadHardcodedList();
                }

                if (!persisted.isNull(mcc)) {
                    JSONObject countryData = persisted.getJSONObject(mcc);
                    country = countryData.getString("country");
                    if (!countryData.isNull(mnc)) {
                        JSONObject provider_data = countryData.getJSONObject(mnc);
                        provider_code = provider_data.getString("code");
                        // provider_name = provider_data.getString("name");
                    }
                }
            }

            populateDataByCode(country, provider_code);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Populate country and provider lists of payment dialog and select specific from parameters
     * 
     * @param selected_country Country code for selecting
     * @param selected_provider Provider code for selecting
     */
    private void populateCountrySelector(String selected_country, String selected_provider) {

        SmscoinActivity.this.findViewById(R.id.dialog_body).setVisibility(View.GONE);
        View cange_view = SmscoinActivity.this.findViewById(R.id.dialog_selection);
        cange_view.setVisibility(View.VISIBLE);

        if (countries == null) {
            try {
                countries = new JSONArray();

                // build unique element list
                // using a clever system of building a unique list in order to reduce the number of iterations. Build
                // list for O(n)
                int counter = 0;

                JSONObject temp_data = new JSONObject();
                for (int i = 0; i < rates.length(); i++) {
                    JSONObject rate = rates.getJSONObject(i);
                    if (temp_data.isNull(rate.getString("country"))) {
                        JSONObject c = new JSONObject();
                        c.put("country_name", rate.getString("country_name"));
                        c.put("country", rate.getString("country"));
                        c.put("providers_t", new JSONObject());
                        c.put("providers", new JSONArray());
                        countries.put(c);
                        if (selected_country.equals(rate.getString("country"))) {
                            country_selector_pos = counter;
                        }
                        temp_data.put(rate.getString("country"), c);
                        counter++;
                    }
                    JSONObject providers = temp_data.getJSONObject(rate.getString("country")).getJSONObject("providers_t");
                    if (rate.isNull("usd")) {
                        JSONArray original_providers = rate.getJSONArray("providers");
                        int counter2 = 0;
                        for (int j = 0; j < original_providers.length(); j++) {
                            JSONObject original_provider = original_providers.getJSONObject(j);
                            if (providers.isNull(original_provider.getString("code"))) {
                                JSONObject p = new JSONObject();
                                p.put("provider_name", original_provider.getString("name"));
                                p.put("provider_code", original_provider.getString("code"));
                                providers.put(original_provider.getString("code"), p);

                                JSONArray providers_2 = temp_data.getJSONObject(rate.getString("country")).getJSONArray("providers");
                                providers_2.put(p);
                                if (selected_provider.equals(original_provider.getString("code"))) {
                                    provider_selector_pos = counter2;
                                }
                                counter2++;
                            }
                        }
                    } else {
                        if (providers.isNull("_all_")) {
                            JSONObject p = new JSONObject();
                            p.put("provider_name", getString(R.string.all_providers));
                            p.put("provider_code", "_all_");
                            providers.put("_all_", p);

                            JSONArray providers_2 = temp_data.getJSONObject(rate.getString("country")).getJSONArray("providers");
                            providers_2.put(p);
                        }
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        final Spinner spn_countries = (Spinner) cange_view.findViewById(R.id.spinner_countries);
        final Spinner spn_providers = (Spinner) cange_view.findViewById(R.id.spinner_providers);

        CountriesAdapter country_adapter = new CountriesAdapter(SmscoinActivity.this, countries);
        spn_countries.setPromptId(R.string.prompt_countries);
        spn_countries.setAdapter(country_adapter);
        spn_countries.setSelection(country_selector_pos);
        final ProvidersAdapter provider_adapter = new ProvidersAdapter(SmscoinActivity.this);
        spn_providers.setPromptId(R.string.prompt_provider);
        spn_providers.setAdapter(provider_adapter);

        spn_countries.setOnItemSelectedListener(new OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> adapter, View arg1, int position, long arg3) {
                JSONObject country = (JSONObject) adapter.getItemAtPosition(position);
                if (position == country_selector_pos) {
                    spn_providers.setSelection(provider_selector_pos);
                }
                try {
                    provider_adapter.setData(country.getJSONArray("providers"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            public void onNothingSelected(AdapterView<?> arg0) {

            }
        });

        Button btn_select = (Button) cange_view.findViewById(R.id.btn_select);
        btn_select.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                JSONObject country_obj = (JSONObject) spn_countries.getSelectedItem();
                JSONObject provider_obj = (JSONObject) spn_providers.getSelectedItem();
                country_selector_pos = spn_countries.getSelectedItemPosition();
                provider_selector_pos = spn_providers.getSelectedItemPosition();
                try {
                    findViewById(R.id.dialog_selection).setVisibility(View.GONE);
                    View dialog_view = findViewById(R.id.dialog_body);
                    dialog_view.setVisibility(View.VISIBLE);
                    populateDataByCode(country_obj.getString("country"), provider_obj.getString("provider_code"));

                    // saving user choose
                    Editor editor = appSettings.edit();
                    editor.putString("selected_country", country_obj.getString("country"));

                    editor.putString("selected_provider", provider_obj.getString("provider_code"));
                    editor.commit();

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        });

    }

    /**
     * Setup payment dialog bay input data
     * 
     * @param country Country code
     * @param provider_code Provider code
     * @throws JSONException
     */
    private void populateDataByCode(String country, String provider_code) throws JSONException {
        final String selected_country = country;
        final String selected_provider = provider_code;
        String provider_name = "";
        ArrayList<Rate> rates_list = new ArrayList<Rate>();

        View dialog_view = findViewById(R.id.dialog_body);
        dialog_view.setVisibility(View.INVISIBLE);

        // load country flag if detected
        ImageView img_flag = (ImageView) dialog_view.findViewById(R.id.img_flag);
        String name = selected_country.toLowerCase().trim();

        if (name.equals("do")) {
            name = "do_flag";
        }

        int flagid = getResources().getIdentifier(name, "drawable", getPackageName());
        if (flagid == 0) {
            img_flag.setImageResource(R.drawable.flag_unknown);
        } else {
            img_flag.setImageResource(flagid);
        }

        // Search data by specified criteria
        boolean isProviderMissing = true;
        for (int i = 0; i < rates.length(); i++) {
            JSONObject rate = rates.getJSONObject(i);
            if (rate.getString("country").equals(selected_country)) {
                if (rate.isNull("usd")) {
                    JSONArray providers = rate.getJSONArray("providers");

                    for (int j = 0; j < providers.length(); j++) {
                        JSONObject provider_data = providers.getJSONObject(j);
                        if (provider_data.getString("code").equals(selected_provider)) {

                            isProviderMissing = false;
                            provider_name = provider_data.getString("name");
                            double expected_summ = provider_data.getDouble("usd");
                            if (paymentRequest.isMyProfit()) {
                                expected_summ = provider_data.getDouble("profit") * provider_data.getDouble("usd") / 100;
                            }
                            if (expected_summ >= paymentRequest.getPrice()) {
                                if (paymentRequest.isMultyRate()) {
                                    rates_list.add(new Rate(provider_data));
                                } else if (rates_list.size() > 0 && rates_list.get(0).usd > provider_data.getDouble("usd")) {
                                    rates_list.set(0, new Rate(provider_data));
                                } else if (rates_list.size() == 0) {
                                    rates_list.add(new Rate(provider_data));
                                }
                            }
                        }
                    }

                    break;
                } else {
                    isProviderMissing = false;
                    provider_name = getString(R.string.all_providers);
                    double expected_summ = rate.getDouble("usd");
                    if (paymentRequest.isMyProfit()) {
                        expected_summ = rate.getDouble("profit") * rate.getDouble("usd") / 100;
                    }
                    if (expected_summ >= paymentRequest.getPrice()) {
                        if (paymentRequest.isMultyRate()) {
                            rates_list.add(new Rate(rate));
                        } else if (rates_list.size() > 0 && rates_list.get(0).usd > rate.getDouble("usd")) {
                            rates_list.set(0, new Rate(rate));
                        } else if (rates_list.size() == 0) {
                            rates_list.add(new Rate(rate));
                        }
                    }
                }
            }
        }


        Button btn_buy = (Button) dialog_view.findViewById(R.id.btn_buy);
        final TextView special_message = (TextView) dialog_view.findViewById(R.id.lbl_specials);
        TextView lbl_provider_name = (TextView) dialog_view.findViewById(R.id.lbl_provider);
        Spinner price_selector = (Spinner) dialog_view.findViewById(R.id.price_selector);
        TextView user_message = (TextView) dialog_view.findViewById(R.id.lbl_user_text);
        TextView must_message = (TextView) dialog_view.findViewById(R.id.lbl_must_text);
        final TextView lbl_vat = (TextView) dialog_view.findViewById(R.id.lbl_vat);

        TextView lbl_terms = (TextView) dialog_view.findViewById(R.id.lbl_terms);

        lbl_terms.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                Intent browserIntent = new Intent("android.intent.action.VIEW", Uri.parse(PaymentActivity.AGREEMENT_URL));
                startActivity(browserIntent);
            }
        });

        // display texts

        if (TextUtils.isEmpty(paymentRequest.getDisplayText())) {
            user_message.setVisibility(View.GONE);
        } else {
            user_message.setText(paymentRequest.getDisplayText());
        }
        // check if any rates found
        if (!isProviderMissing && rates_list.size() == 0) {
            special_message.setText("");
            btn_buy.setEnabled(false);
            lbl_vat.setText("");
            must_message.setText(R.string.no_rate);
            price_selector.setVisibility(View.INVISIBLE);

        }else if (!isProviderMissing && paymentRequest.isMultyRate()) {
            must_message.setText(getString(R.string.prompt_price));
            adapter = new RateListAdapter(this, rates_list, paymentRequest);
            price_selector.setPromptId(R.string.prompt_price);
            price_selector.setAdapter(adapter);
            price_selector.setOnItemSelectedListener(new OnItemSelectedListener() {

                public void onItemSelected(AdapterView<?> adapter, View arg1, int position, long arg3) {
                    currentRate = (Rate) adapter.getItemAtPosition(position);
                    if (currentRate.vat) {
                        lbl_vat.setText(R.string.include_vat);
                    } else {
                        lbl_vat.setText(R.string.exclude_vat);
                    }
                    if (TextUtils.isEmpty(currentRate.special)) {
                        special_message.setVisibility(View.GONE);
                    } else {
                        special_message.setVisibility(View.VISIBLE);
                        special_message.setText(currentRate.special);
                    }
                }

                public void onNothingSelected(AdapterView<?> arg0) {
                    // TODO Auto-generated method stub

                }
            });

        } else if (!isProviderMissing){
            price_selector.setVisibility(View.GONE);
            currentRate = rates_list.get(0);
            must_message.setText(String.format(getString(R.string.prompt_message), 
                    new Object[] { currentRate.price, currentRate.currency, paymentRequest.getRatio(), paymentRequest.getProduct() }));

            if (TextUtils.isEmpty(rates_list.get(0).special)) {
                special_message.setVisibility(View.GONE);
            } else {
                special_message.setVisibility(View.VISIBLE);
                special_message.setText(rates_list.get(0).special);
            }

            if (rates_list.get(0).vat) {
                lbl_vat.setText(R.string.include_vat);
            } else {
                lbl_vat.setText(R.string.exclude_vat);
            }

        }

        // init cancel click listeners
        Button btn_cancel = (Button) findViewById(R.id.btn_cancel);
        btn_cancel.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                finish();
            }
        });

        // init change country click listener
        Button btn_change = (Button) findViewById(R.id.btn_change);
        if (btn_change.getTag() == null) {
            btn_change.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {
                    v.setTag("setted");
                    populateCountrySelector(selected_country, selected_provider);

                }
            });
        }

        // show dialog
        dialog_view.setVisibility(View.VISIBLE);

        // hiding controls if provider was not detected
        if (isProviderMissing) {
            lbl_provider_name.setText(R.string.provider_not_found);
            lbl_provider_name.setTextColor(Color.RED);
            btn_buy.setEnabled(false);
            special_message.setText("");
            price_selector.setVisibility(View.INVISIBLE);
            user_message.setVisibility(View.INVISIBLE);
            must_message.setVisibility(View.INVISIBLE);

            // show select country view
            populateCountrySelector(selected_country, selected_provider);
        } else {
            lbl_provider_name.setText(provider_name);
            lbl_provider_name.setTextColor(Color.BLACK);

            user_message.setVisibility(View.VISIBLE);
            must_message.setVisibility(View.VISIBLE);
            if(rates_list.size() > 0){
                btn_buy.setEnabled(true);
                if (paymentRequest.isMultyRate()) {
                    price_selector.setVisibility(View.VISIBLE);
                }
            }
            btn_buy.setOnClickListener(new OnClickListener() {

                public void onClick(View v) {

                    // build message for sending
                    String message = String.format("%s %s and01%s",
                            new Object[] { currentRate.prefix, paymentRequest.getServiceId(), Base64.encodeBytes(paymentRequest.getData().getBytes()) });
                    Intent result = new Intent();
                    result.putExtra("message", message);
                    result.putExtra("number", currentRate.number);
                    SmscoinActivity.this.setResult(Activity.RESULT_OK, result);
                    SmscoinActivity.this.finish();

                }
            });
        }


    }

}

