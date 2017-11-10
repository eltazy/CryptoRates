package com.alc.eltazy.cryptorates;

// Import statements
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener, AdapterView.OnItemClickListener {
    // UI widgets
    private ImageButton settingsButton = null;
    private ImageButton refreshButton = null;
    private ListView list_view = null;
    private Spinner spinner_base_coin = null;
    private Spinner spinner_pref_currency = null;
    private TextView pref_exchange_rate = null;
    private TextView pref_symbol = null;
    private ProgressBar refresh_progress = null;
    private ImageView pref_icon = null;
    // Class attributes
    private String base_currency = null;
    private String preference_currency = null;
    private Double preference_rate = .0;
    // HTTP Request data containers
    private HashMap<String, Double> rates = null;
    private ArrayList<RateItem> view_rates = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Retrieving user settings if saved before or
        // Return default app settings
        SharedPreferences myPreferences = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        String coin_setting = myPreferences.getString(getString(R.string.pref_coin_setting), getString(R.string.default_coin));
        setBaseCurrency(coin_setting);
        String currency_setting = myPreferences.getString(getString(R.string.pref_currency_setting), getString(R.string.default_currency));
        setPreferenceCurrency(currency_setting);

        // Setting UI elements
        refresh_progress = findViewById(R.id.refresh_progress);
        list_view = findViewById(R.id.custom_list);
        spinner_base_coin = findViewById(R.id.base_currency_spinner);
        spinner_pref_currency = findViewById(R.id.preference_currency_spinner);
        refreshButton = findViewById(R.id.refresh_button);
        settingsButton = findViewById(R.id.settings_button);
        pref_symbol = findViewById(R.id.preference_symbol);
        pref_exchange_rate = findViewById(R.id.preference_rate);
        pref_icon = findViewById(R.id.highlight_icon);

        // Initializing UI with user settings if existing
        // or with default values

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.pref_list_coin_values, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_base_coin.setAdapter(adapter);
        spinner_base_coin.setSelection(adapter.getPosition(coin_setting));

        adapter = ArrayAdapter.createFromResource(this, R.array.pref_list_currency_values, android.R.layout.simple_spinner_item);
        spinner_pref_currency.setAdapter(adapter);
        spinner_pref_currency.setSelection(adapter.getPosition(currency_setting));

        pref_icon.setImageResource(getResources().getIdentifier(currency_setting.toLowerCase(), "drawable", getPackageName()));
        pref_symbol.setText(getCurrencySymbol(preferenceCurrency()));
        pref_exchange_rate.setText(preferenceRate().toString());

        //Setting Listeners
        list_view.setOnItemClickListener(this);
        spinner_base_coin.setOnItemSelectedListener(this);
        spinner_pref_currency.setOnItemSelectedListener(this);
        refreshButton.setOnClickListener(this);
        settingsButton.setOnClickListener(this);

        //Retrieving CRYPTOCOMPARE API Data based on default settings
        // or user settings
        retrievingData();
    }

    // Listeners
    @Override
    public void onClick(View view){
        switch (view.getId()){
            case R.id.refresh_button:
                // If user pressed the Refresh button
                retrievingData();
                break;
            case R.id.settings_button:
                // If user pressed the Settings button
                Intent settings_intent = new Intent(this, SettingsActivity.class);
                startActivity(settings_intent);
                break;
        }
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // Refresh Data if user changed options in spinners
        switch (parent.getId()){
            case R.id.base_currency_spinner:
                setBaseCurrency(parent.getSelectedItem().toString());
                retrievingData();
                break;
            case R.id.preference_currency_spinner:
                setPreferenceCurrency(parent.getSelectedItem().toString());
                pref_icon.setImageResource(getResources().getIdentifier(preferenceCurrency().toLowerCase(), "drawable", getPackageName()));
                pref_exchange_rate.setText(preferenceRate().toString());
                pref_symbol.setText(getCurrencySymbol(preferenceCurrency()));
                retrievingData();
                break;
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent){}
    @Override
    public void onItemClick(AdapterView<?> parent, View view,int pos, long id){
        // If user pressed the on a ListView item
        Intent convertor_intent = new Intent(this, CardActivity.class);
        RateItem selected_item = view_rates.get(pos);
        String coin = baseCurrency(), curr = selected_item.getIcon();
        convertor_intent.putExtra(getString(R.string.INTENT_SELECTION_RATE), selected_item.getValue());
        convertor_intent.putExtra(getString(R.string.INTENT_COIN_CODE), coin.toLowerCase());
        convertor_intent.putExtra(getString(R.string.INTENT_COIN_SYMBOL), getCurrencySymbol(coin));
        convertor_intent.putExtra(getString(R.string.INTENT_CURRENCY_CODE), curr.toLowerCase());
        convertor_intent.putExtra(getString(R.string.INTENT_CURRENCY_SYMBOL), getCurrencySymbol(curr));
        // Show card activity
        startActivity(convertor_intent);
    }
    //Setters and Getters
    public String baseCurrency(){
        return base_currency;
    }
    public String preferenceCurrency(){
        return preference_currency;
    }
    public Double preferenceRate(){
        return preference_rate;
    }
    public void setBaseCurrency(String b){
        base_currency = b;
    }
    public void setPreferenceCurrency(String b){
        preference_currency = b;
    }
    public void setPreferenceRate(Double b){
        preference_rate = b;
    }


    //other class methods
    private ArrayList getListData() {
        // Function to return CRYPTOCOMPARE API Data
        // formatted as an array of RateItem Objects
        ArrayList<RateItem> results = new ArrayList<>();
        for(HashMap.Entry<String, Double> entry: rates.entrySet()) {
            RateItem item = new RateItem();
            String code = entry.getKey();

            item.setIcon(code);
            item.setSymbol(getCurrencySymbol(code));
            item.setValue(entry.getValue());
            results.add(item);
        }
        return results;
    }
    protected  String getCurrencySymbol(String code){
        switch (code){
            case "BTC": return "BTC";
            case "ETH": return "ETH";
            case "JPY": return "¥";
            case "EUR": return "€";
            case "GBP": return "£";
            case "NGN": return "₦";
            case "CHF": return "Fr";
            case "KRW": return "₩";
            case "CNY": return "元";
            case "RUB": return "\u20BD";
            case "INR": return "₹";
            case "SEK": case "NOK": return "kr";
            case "TRK": case "TRY": return "₺";
            default: return "$";
        }
    }
    public void refresh() {
        // Show ListView and hide progressBar
        refresh_progress.setVisibility(View.GONE);
        list_view.setVisibility(View.VISIBLE);
        list_view.setAdapter(new CustomAdapter(this, view_rates));
        // Set rate and currency symbol to highlight
        pref_symbol.setText(getCurrencySymbol(preferenceCurrency()));
        pref_exchange_rate.setText(rates.get(preferenceCurrency()).toString());
    }
    public void retrievingData() {
        if(hasInternetConnectivity()){
            // Hides ListView and shows ProgressBar as the API request is sent
            new RetrieveHTTPSRequestData().execute();
            list_view.setVisibility(View.GONE);
            refresh_progress.setVisibility(View.VISIBLE);
        }
        //Notifies on a Toast "No Internet" message
        else Toast.makeText(MainActivity.this, "No Internet.\nConnect to the Internet then Refresh", Toast.LENGTH_LONG).show();
    }
    public boolean hasInternetConnectivity() {
        // Returns true or false whether or not there is Internet connexion
        ConnectivityManager connectivity = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
        if (activeNetwork != null) if (activeNetwork.isConnected()) return true;
        return false;
    }
//Sub-classes
    private class RateItem {
        //Class for Objects to handle rate data
        private String icon = null;
        private String symbol = null;
        private Double value = null;

        private void setIcon(String t_icon) {
                this.icon = t_icon;
            }
        private String getIcon() {
            return this.icon;
        }
        private void setSymbol(String t_symbol) {
            this.symbol = t_symbol;
        }
        private String getSymbol() {
            return this.symbol;
        }
        private void setValue(Double t_value) {
            this.value = t_value;
        }
        private Double getValue() {
            return this.value;
        }
    }
    // ListView Custom Adapter to help display
    // in list the icon, the symbol and exchange rate
    class CustomAdapter extends BaseAdapter{
        private ArrayList<RateItem> listData = null;
        private LayoutInflater layoutInflater = null;

        private CustomAdapter(Context context, ArrayList<RateItem> listData) {
            this.listData = listData;
            this.layoutInflater = LayoutInflater.from(context);
        }
        @Override
        public int getCount() {
            return listData.size();
        }
        @Override
        public Object getItem(int i) {
            return listData.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_item_style, null);
                holder = new ViewHolder();
                holder.icon = convertView.findViewById(R.id.icon);
                holder.symbol = convertView.findViewById(R.id.symbol);
                holder.value = convertView.findViewById(R.id.value);
                convertView.setTag(holder);
            }
            else holder = (ViewHolder) convertView.getTag();

            String temp_id = listData.get(position).getIcon().toLowerCase();
            holder.icon.setImageResource(getResources().getIdentifier(temp_id, "drawable", getPackageName()));
            holder.symbol.setText(listData.get(position).getSymbol());
            holder.value.setText(listData.get(position).getValue().toString());
            return convertView;
        }
        class ViewHolder {
            ImageView icon;
            TextView symbol;
            TextView value;
        }
    }
    //Asynchronous https request connexion thread
    class RetrieveHTTPSRequestData extends AsyncTask<Void, Void, String> {
        protected void onPreExecute() {}
        protected String doInBackground(Void... urls) {
            // Workaround as try is a java reserved word
            // Replacing it with trk for turkey
            String curr;
            if (baseCurrency().equals("TRK")) curr = "TRY";
            else curr = baseCurrency();
            // Based on Wikipedia's world top 20 most traded currencies
            // with South Africa's Rand replaced by Nigeria's Naira (NGN)
            String str_url = "https://min-api.cryptocompare.com/data/price?fsym=" + curr +
                    "&tsyms=BTC,ETH,USD,EUR,JPY,GBP,AUD,CAD,CHF,CNY,SEK,MXN,NZD,SGD,HKD,NOK,KRW,TRY,INR,RUB,BRL,NGN";
            try {
                // Sending Request
                URL url = new URL(str_url);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                try {
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(bufferedReader.readLine());
                    bufferedReader.close();
                    return stringBuilder.toString();
                }
                finally{
                    // Closing connexion
                    urlConnection.disconnect();
                }
            }
            catch(Exception e) {
                //Handling Exception Errors
                Log.e("ERROR", e.getMessage(), e);
                return null;
            }
        }

        protected void onPostExecute(String response) {
            //Statements to execute after request returned a response
            if(response == null) response = "THERE WAS AN ERROR";
            Log.i("INFO", response);
            try {
                //Saving response in a JSON Object
                JSONObject object = new JSONObject(response.replace("TRY", "TRK"));
                HashMap<String, Double> rate_list = new HashMap<>();

                // Saving values for each keys in JSON Object
                Iterator<String> keysItr = object.keys();
                while(keysItr.hasNext()) {
                    String key = keysItr.next();
                    Double value = object.getDouble(key);
                    rate_list.put(key, value);
                }
                // Initializing data arrays
                rates = rate_list;
                view_rates = getListData();
                // Updating preference data
                setPreferenceRate(rate_list.get(preferenceCurrency()));
                // Displaying Rates
                refresh();
            } catch (JSONException e) {
                // Error handling
                e.printStackTrace();
            }
        }
    }
}