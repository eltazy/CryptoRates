package com.alc.eltazy.cryptorates;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.math.BigDecimal;

public class CardActivity extends AppCompatActivity implements View.OnClickListener{
    // From MainActivity
    private Bundle data = null;
    private String base_code = null;
    private String base_symbol = null;
    private String target_code = null;
    private String target_symbol = null;
    private Double rate = null;
    // UI
    private EditText edit_base_value = null;
    private TextView text_base_symbol = null;
    private TextView text_target_value = null;
    private TextView text_target_symbol = null;
    private ImageView base_icon = null;
    private ImageView target_icon = null;
    private ImageButton button_switch = null;

    private boolean invert = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Default
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_card);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Initializing data from intent Bundle
        data = getIntent().getExtras();
        rate = data.getDouble(getString(R.string.INTENT_SELECTION_RATE));
        base_code = data.getString(getString(R.string.INTENT_COIN_CODE));
        base_symbol = data.getString(getString(R.string.INTENT_COIN_SYMBOL));
        target_code = data.getString(getString(R.string.INTENT_CURRENCY_CODE));
        if (target_code.equals("try")) target_code = "trk";
        target_symbol = data.getString(getString(R.string.INTENT_CURRENCY_SYMBOL));
        invert = base_code.equals("eth");

        // Set UI
        base_icon = findViewById(R.id.base_icon);
        text_base_symbol = findViewById(R.id.symbol_base);
        edit_base_value = findViewById(R.id.base_value);
        target_icon = findViewById(R.id.currency_icon);
        text_target_symbol = findViewById(R.id.currency_symbol);
        text_target_value = findViewById(R.id.currency_value);
        button_switch = findViewById(R.id.button_switch);
        // Initializing UI
        base_icon.setImageResource(getResources().getIdentifier(base_code, "drawable", getPackageName()));
        text_base_symbol.setText(base_symbol);
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
        target_icon.setImageResource(getResources().getIdentifier(target_code, "drawable", getPackageName()));
        text_target_symbol.setText(target_symbol);
        text_target_value.setText("0.0");
        button_switch.setOnClickListener(this);
    }
    //Listeners
    @Override
    public void onClick(View view) {
        RateContainer base = new RateContainer(base_code, base_symbol);
        RateContainer value = new RateContainer(target_code, target_symbol);
        // Switch values
        RateContainer temp = new RateContainer(base);
        base = value;
        value = temp;
        base_code = base.getIcon();
        base_symbol = base.getSymbol();
        target_code = value.getIcon();
        target_symbol = value.getSymbol();
        // Change UI appearance
        text_target_symbol.setText(value.getSymbol());
        text_base_symbol.setText(base.getSymbol());
        base_icon.setImageResource(getResources().getIdentifier(base.getIcon(), "drawable", getPackageName()));
        target_icon.setImageResource(getResources().getIdentifier(value.getIcon(), "drawable", getPackageName()));
        calculate();
    }
// Other methods
    // Determines whether or not the base currency is of greater value
    public boolean baseCoinIsGreater(){
        return base_code.equals("btc");
    }
    public boolean baseHasGreaterValue(){
        if (areBothCryptoCurrencies()) return invert != baseCoinIsGreater();
        else return isCryptoCurrency(base_code);
    }
    public boolean isCryptoCurrency(String code){
        return code.equals("btc")  || code.equals("eth");
    }
    public boolean areBothCryptoCurrencies(){
        return isCryptoCurrency(base_code) && isCryptoCurrency(target_code);
    }
    // Function to compute output value based on user input
    public void calculate(){
        String str = edit_base_value.getText().toString();
        Double base_input;
        if (str.isEmpty() || str.equals(".")) base_input = .0;
        else base_input = Double.parseDouble(str);
        Double curr_output = baseHasGreaterValue() ? base_input * rate : base_input / rate;
        BigDecimal out = new BigDecimal(curr_output);
        text_target_value.setText(out.toPlainString());
    }
// Sub-class
    private class RateContainer {
        //Class for Objects to handle rate data
        private String icon = null;
        private String symbol = null;

        private RateContainer(String ic, String symb){
            setIcon(ic);
            setSymbol(symb);
        }
        private RateContainer(RateContainer rate){
            setIcon(rate.getIcon());
            setSymbol(rate.getSymbol());
        }
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
    }
}