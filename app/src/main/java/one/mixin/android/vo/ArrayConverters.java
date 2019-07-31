package one.mixin.android.vo;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import one.mixin.android.util.GsonHelper;

public class ArrayConverters {
    @TypeConverter
    public static ArrayList<String> fromString(String value) {
        Type listType = new TypeToken<ArrayList<String>>() {
        }.getType();
        return GsonHelper.INSTANCE.getCustomGson().fromJson(value, listType);
    }

    @TypeConverter
    public static String fromArrayList(ArrayList<String> list) {
        Gson gson = GsonHelper.INSTANCE.getCustomGson();
        return gson.toJson(list);
    }
}
