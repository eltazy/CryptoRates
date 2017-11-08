package com.alc.eltazy.cryptorates;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
class RateContainer {
    //Class for Objects to handle rate data
    private String icon = null;
    private String symbol = null;

    public RateContainer(String ic, String symb){
        setIcon(ic);
        setSymbol(symb);
    }
    public RateContainer(RateContainer rate){
        setIcon(rate.getIcon());
        setSymbol(rate.getSymbol());
    }
    public void setIcon(String t_icon) {
        this.icon = t_icon;
    }
    public String getIcon() {
        return this.icon;
    }
    public void setSymbol(String t_symbol) {
        this.symbol = t_symbol;
    }
    public String getSymbol() {
        return this.symbol;
    }
}
public class CardActivity extends AppCompatActivity implements View.OnClickListener{
    // From MainActivity
    private Bundle data = null;
    private String coin_code = null;
    private String coin_symbol = null;
    private String currency_code = null;
    private String currency_symbol = null;
    private Double rate = null;
    // UI
    private EditText edit_base_value = null;
    private TextView text_base_symbol = null;
    private TextView text_currency_value = null;
    private TextView text_currency_symbol = null;
    private ImageView coin_icon = null;
    private ImageView currency_icon = null;
    private Button button_switch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializing data from intent Bundle
        data = getIntent().getExtras();
        coin_code = data.getString(getString(R.string.INTENT_COIN_CODE));
        coin_symbol = data.getString(getString(R.string.INTENT_COIN_SYMBOL));
        currency_code = data.getString(getString(R.string.INTENT_CURRENCY_CODE));
        if (currency_code.equals("try")) currency_code = "trk";
        currency_symbol = data.getString(getString(R.string.INTENT_CURRENCY_SYMBOL));
        rate = data.getDouble(getString(R.string.INTENT_SELECTION_RATE));

        // Set UI
        edit_base_value = findViewById(R.id.base_value);
        text_base_symbol = findViewById(R.id.symbol_base);
        text_currency_value = findViewById(R.id.currency_value);
        text_currency_symbol = findViewById(R.id.currency_symbol);
        coin_icon = findViewById(R.id.base_icon);
        currency_icon = findViewById(R.id.currency_icon);
        button_switch = findViewById(R.id.button_switch);
        // Initializing UI
        text_currency_symbol.setText(currency_symbol);
        text_base_symbol.setText(coin_symbol);
        coin_icon.setImageResource(getResources().getIdentifier(coin_code, "drawable", getPackageName()));
        currency_icon.setImageResource(getResources().getIdentifier(currency_code, "drawable", getPackageName()));
        button_switch.setOnClickListener(this);

        edit_base_value.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculate();
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override
            public void afterTextChanged(Editable s) {
                Log.e("TextWatcherTest", "afterTextChanged:\t" +s.toString());
            }
        });
    }

    @Override
    public void onClick(View view) {
        RateContainer base = new RateContainer(coin_code, coin_symbol);
        RateContainer value = new RateContainer(currency_code, currency_symbol);
        // Switch values
        RateContainer temp = new RateContainer(base);
        base = value;
        value = temp;
        coin_code = base.getIcon();
        coin_symbol = base.getSymbol();
        currency_code = value.getIcon();
        currency_symbol = value.getSymbol();
        // Change UI appearance
        text_currency_symbol.setText(value.getSymbol());
        text_base_symbol.setText(base.getSymbol());
        coin_icon.setImageResource(getResources().getIdentifier(base.getIcon(), "drawable", getPackageName()));
        currency_icon.setImageResource(getResources().getIdentifier(value.getIcon(), "drawable", getPackageName()));
        calculate();

    }
    public boolean isGreaterCurrency(String code){
        if (!areBothCryptoCurrencies()) return code.equals("btc")  || code.equals("eth");
        else if (code.equals("btc")) return true;
        else return false;
    }
    public boolean isCryptoCurrency(String code){
        return code.equals("btc")  || code.equals("eth");
    }
    public boolean areBothCryptoCurrencies(){
        return isCryptoCurrency(coin_code) && isCryptoCurrency(currency_code);
    }
    public void calculate(){
        String str = edit_base_value.getText().toString();
        Double base_input;
        if (str.isEmpty() || str.equals(".")) base_input = .0;
        else base_input = Double.parseDouble(str);
        Double curr_output = isGreaterCurrency(coin_code) ? base_input * rate : base_input / rate;
        text_currency_value.setText(curr_output.toString());
    }
}
