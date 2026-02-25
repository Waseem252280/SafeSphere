package com.example.safesphere.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.safesphere.dto.TrustedContact;
import com.example.safesphere.dto.UserDto;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SharedPrefferanceUtil {

    private static final String PREF_NAME = "user_prefs";
    private static final String CONTACTS_PREF = "user_contacts";


    //save user data into shared prefferance
    public static void saveUserDetails(UserDto user, Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userId", user.getUserId());
        editor.putString("fullName", user.getFullName());
        editor.putString("email", user.getEmail());
        editor.putString("gender", user.getGender());
        editor.putString("photoUrl", user.getPhotoUrl());
        editor.apply();
    }

    //check if user is logged in or not
    public static UserDto isUserLoggedIn(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = sharedPreferences.getString("email", null);
        if (email == null || email.isEmpty()) {
            return null; // user not logged in
        }
        UserDto user = new UserDto();
        user.setUserId(sharedPreferences.getString("userId", ""));
        user.setFullName(sharedPreferences.getString("fullName", ""));
        user.setEmail(email);
        user.setGender(sharedPreferences.getString("gender", ""));
        user.setPhotoUrl(sharedPreferences.getString("photoUrl", ""));
        return user;
    }
    //after logged out clear the data from shared prefferance
    public static void clearUserData(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }


    // Ye methods SharedPrefferanceUtil class ke andar dalen
    // 1. CREATE & UPDATE (Save poori list)

        public static void saveTrustedContacts(Context context, List<TrustedContact> list) {
            SharedPreferences sp = context.getSharedPreferences(CONTACTS_PREF, Context.MODE_PRIVATE);
            String json = new Gson().toJson(list);
            sp.edit().putString("contacts_key", json).apply();
        }

        public static List<TrustedContact> getTrustedContacts(Context context) {
            SharedPreferences sp = context.getSharedPreferences(CONTACTS_PREF, Context.MODE_PRIVATE);
            String json = sp.getString("contacts_key", null);
            if (json == null) return new ArrayList<>();
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<TrustedContact>>() {}.getType();
            return gson.fromJson(json, type);
        }

        public static void deleteContact(Context context, String phone) {
            List<TrustedContact> list = getTrustedContacts(context);
            // Java 8 removeIf
            list.removeIf(c -> c.getPhone().equals(phone));
            saveTrustedContacts(context, list);
        }

    // Trusted contacts ki list ko poora clear karne ke liye (Logout ke waqt)
    public static void clearTrustedContactData(Context context) {
        SharedPreferences sp = context.getSharedPreferences(CONTACTS_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.clear();
        editor.apply();
    }
}
