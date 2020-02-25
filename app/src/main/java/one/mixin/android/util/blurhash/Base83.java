package one.mixin.android.util.blurhash;

import androidx.annotation.Nullable;

public class Base83 {

  private static final int MAX_LENGTH = 90;

  private static final char[]ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#$%*+,-.:;=?@[]^_{|}~".toCharArray();

  private static int indexOf(char[] a, char key) {
    for (int i = 0; i < a.length; i++) {
      if (a[i] == key) {
        return i;
      }
    }
    return -1;
  }

  static void encode(long value, int length, char[] buffer, int offset) {
    int exp = 1;
    for (int i = 1; i <= length; i++, exp *= 83) {
      int digit = (int)(value / exp % 83);
      buffer[offset + length - i] = ALPHABET[digit];
    }
  }

  static int decode(String value, int fromInclusive, int toExclusive) {
    int result = 0;
    char[] chars = value.toCharArray();
    for (int i = fromInclusive; i < toExclusive; i++) {
      result = result * 83 + indexOf(ALPHABET, chars[i]);
    }
    return result;
  }

  public static boolean isValid(@Nullable String value) {
    if (value == null) return false;
    final int length = value.length();

    if (length == 0 || length > MAX_LENGTH) return false;

    for (int i = 0; i < length; i++) {
      if (indexOf(ALPHABET, value.charAt(i)) == -1) return false;
    }

    return true;
  }

  private Base83() {
  }
}

