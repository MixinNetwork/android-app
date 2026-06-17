package one.mixin.android.widget.countrypicker;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArrayMap;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.timehop.stickyheadersrecyclerview.StickyRecyclerHeadersDecoration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import in.myinnos.alphabetsindexfastscrollrecycler.IndexFastScrollRecyclerView;
import one.mixin.android.R;

@SuppressWarnings("unused")
public class CountryPicker extends Fragment implements View.OnClickListener {

  private View topSpaceView;
  private int topViewHeight = 0;
  private View closeView;
  private CountryAdapter adapter;
  private List<Country> countriesList;
  private List<Country> selectedCountriesList;
  private CountryPickerListener listener;
  private Country mSelectedCountry;
  private Country mLocationCountry;
  private Context context;
  private EditText mSearchEditText;
  private View header;

  private ArrayMap<String, Country> defaultCountryMap;

  public static CountryPicker newInstance() {
    return new CountryPicker();
  }

  public CountryPicker() {
  }

  private final Country anonymous = new Country();

  @Override
  public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    assert container != null;
    context = container.getContext();
    getAllCountries();
    anonymous.setName(getString(R.string.Mixin));
    anonymous.setCode(getString(R.string.Mixin));
    anonymous.setDialCode(getString(R.string.mixin_dial_code));
    anonymous.setFlag(R.drawable.flag_mixin);
    View view = inflater.inflate(R.layout.country_picker, container, false);
    topSpaceView = view.findViewById(R.id.country_picker_top_space);
    applyTopViewHeight();
    mSearchEditText = view.findViewById(R.id.country_code_picker_search);
    IndexFastScrollRecyclerView countryRv = view.findViewById(R.id.country_code_picker_rv);
    countryRv.setIndexBarTransparentValue(0);
    countryRv.setIndexBarStrokeVisibility(false);
    countryRv.setIndexBarTextColor(R.color.text_blue);
    closeView = view.findViewById(R.id.close);
    closeView.setOnClickListener(this);

    selectedCountriesList = new ArrayList<>(countriesList.size());
    selectedCountriesList.addAll(countriesList);

    if (mSelectedCountry == null) {
      mSelectedCountry = mLocationCountry;
    }

    adapter = new CountryAdapter(selectedCountriesList, country -> {
      if (listener != null) {
        mSelectedCountry = country;
        refreshCountryInfo(country);
      }
      return null;
    });
    countryRv.setLayoutManager(new LinearLayoutManager(requireContext()));
    countryRv.addItemDecoration(new StickyRecyclerHeadersDecoration(adapter));
    countryRv.setAdapter(adapter);
    header = inflater.inflate(R.layout.header, container, false);
    adapter.setHeaderView(header);
    View mixinView = header.findViewById(R.id.mixin_rl);
    mixinView.setOnClickListener(v -> {
      if (listener != null) {
        mSelectedCountry = anonymous;
        refreshCountryInfo(anonymous);
      }
    });

    mSearchEditText.addTextChangedListener(new TextWatcher() {

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        search(s.toString());
      }
    });

    return view;
  }

  private void refreshCountryInfo(Country country) {
    reset();
    listener.onSelectCountry(country.getName(), country.getCode(), country.getDialCode(),
            country.getFlag());
  }

  public void setListener(CountryPickerListener listener) {
    this.listener = listener;
  }

  public void setTopViewHeight(int height) {
    if (height < 0) return;
    topViewHeight = height;
    applyTopViewHeight();
  }

  private void applyTopViewHeight() {
    if (topSpaceView != null && topViewHeight > 0) {
      ViewGroup.LayoutParams params = topSpaceView.getLayoutParams();
      params.height = topViewHeight;
      topSpaceView.setLayoutParams(params);
    }
  }

  @SuppressLint({"DefaultLocale", "NotifyDataSetChanged"})
  private void search(String text) {
      selectedCountriesList.clear();
      if (text.isBlank()) {
          adapter.setHeaderView(header);
      } else {
          adapter.setHeaderView(null);
      }
      for (Country country : countriesList) {
          if (country.getName().toLowerCase(Locale.ENGLISH).contains(text.toLowerCase()) || country.getDialCode().contains(text)) {
              selectedCountriesList.add(country);
          }
      }
      adapter.notifyDataSetChanged();
  }

  public void getAllCountries() {
    if (countriesList == null) {
      try {
        countriesList = new ArrayList<>();
        defaultCountryMap = new ArrayMap<>();
        String allCountriesCode = readEncodedJsonString();
        JSONArray countryArray = new JSONArray(allCountriesCode);
        for (int i = 0; i < countryArray.length(); i++) {
          JSONObject jsonObject = countryArray.getJSONObject(i);
          String countryName = jsonObject.getString("name");
          String countryDialCode = jsonObject.getString("dial_code");
          String countryCode = jsonObject.getString("code");
          Country country = new Country();
          country.setCode(countryCode);
          country.setDialCode(countryDialCode);
          country.setFlag(getFlagResId(countryCode));
          countriesList.add(country);

          if (Constants.DEFAULT_TOP_LEVEL_COUNTRIES.contains(countryName)) {
            defaultCountryMap.put(countryDialCode, country);
          }
        }
        selectedCountriesList = new ArrayList<>();
        selectedCountriesList.addAll(countriesList);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private static String readEncodedJsonString() {
    byte[] data = Base64.decode(Constants.ENCODED_COUNTRY_CODE, Base64.DEFAULT);
    return new String(data, StandardCharsets.UTF_8);
  }

  public void setCountriesList(List<Country> newCountries) {
    this.countriesList.clear();
    this.countriesList.addAll(newCountries);
  }

  public void setLocationCountry(Country country) {
    mLocationCountry = country;
  }

  public Country getUserCountryInfo(Context context) {
    this.context = context;
    getAllCountries();
    TelephonyManager telephonyManager =
            (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    if (!(telephonyManager.getSimState() == TelephonyManager.SIM_STATE_ABSENT)) {
      return getCountry(telephonyManager.getSimCountryIso());
    }

    return getCountry(Locale.getDefault().getCountry());
  }

  public Country getCountryByLocale(Context context, Locale locale ) {
    this.context = context;
    String countryIsoCode = locale.getISO3Country().substring(0,2).toLowerCase();
    return getCountry(countryIsoCode);
  }

  public Country getCountryByName (Context context, String countryName ) {
    this.context = context;
    Map<String, String> countries = new HashMap<>();
    for (String iso : Locale.getISOCountries()) {
      Locale l = new Locale("", iso);
      countries.put(l.getDisplayCountry(), iso);
    }

    String countryIsoCode = countries.get(countryName);
    if (countryIsoCode != null) {
      return getCountry(countryIsoCode);
    }
    return afghanistan();
  }

  @Nullable
  public Country getCountryByDialCode(String dialCode) {
    getAllCountries();
    Country country = defaultCountryMap.get(dialCode);
    if (country != null) {
      return country;
    }

    for (int i = 0; i < countriesList.size(); i++) {
      country = countriesList.get(i);
      if (country.getDialCode().equals(dialCode)) {
        country.setFlag(getFlagResId(country.getCode()));
        return country;
      }
    }
    return null;
  }

  private Country getCountry(String countryIsoCode ) {
    getAllCountries();
    for (int i = 0; i < countriesList.size(); i++) {
      Country country = countriesList.get(i);
      if (country.getCode().equalsIgnoreCase(countryIsoCode)) {
        country.setFlag(getFlagResId(country.getCode()));
        return country;
      }
    }
    return afghanistan();
  }

  private Country afghanistan() {
    Country country = new Country();
    country.setCode("AF");
    country.setDialCode("+93");
    country.setFlag(R.drawable.flag_af);
    return country;
  }

  @SuppressLint("DiscouragedApi")
  private int getFlagResId(String drawable) {
    try {
      return context.getResources()
              .getIdentifier("flag_" + drawable.toLowerCase(Locale.ENGLISH), "drawable",
                      context.getPackageName());
    } catch (Exception e) {
      e.printStackTrace();
      return 0;
    }
  }

  @Override public void onClick(View v) {
    if (v == closeView) {
      reset();
      getParentFragmentManager().popBackStackImmediate();
    }
  }

  private void reset() {
    mSearchEditText.setText("");
    InputMethodManager imm = (InputMethodManager)requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
    if (imm != null) {
      imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
    }
  }
}
