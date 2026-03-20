package one.mixin.android.widget.countrypicker;

import android.text.TextUtils;

import java.util.Locale;

/**
 * Created by mukesh on 25/04/16.
 */

public class Country {
  private String code;
  private String name;
  private String dialCode;
  // sorted by English name
  private String englishName;
  private int flag;

  public String getDialCode() {
    return dialCode;
  }

  public void setDialCode(String dialCode) {
    this.dialCode = dialCode;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
    if (TextUtils.isEmpty(name)) {
      if (code.equals("BQ")) {
        if (Locale.getDefault().getLanguage().contains("zh")) {
          name = "博内尔、圣尤斯特歇斯和萨巴";
        } else {
          name = "Bonaire, Sint Eustatius and Saba";
        }
        englishName = "Bonaire, Sint Eustatius and Saba";
      } else {
        name = new Locale("", code).getDisplayName(Locale.getDefault());
        englishName = new Locale("en", code).getDisplayName(Locale.ENGLISH);
        englishName = englishName.substring(englishName.indexOf('(') + 1);
      }
    }
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public int getFlag() {
    return flag;
  }

  public void setFlag(int flag) {
    this.flag = flag;
  }

  public String getEnglishName() {
    return englishName;
  }
}