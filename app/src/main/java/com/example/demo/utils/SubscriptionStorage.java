package com.example.demo.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.demo.models.Subscription;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * SharedPreferences-based CRUD storage for subscriptions.
 * Uses Gson for JSON serialization — no backend needed.
 */
public class SubscriptionStorage {

    private static final String PREFS_NAME = "subscription_prefs";
    private static final String KEY_SUBSCRIPTIONS = "subscriptions_list";
    private static final Gson gson = new Gson();

    // ── Load ─────────────────────────────────────────────────────────────────

    public static List<Subscription> load(Context ctx) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SUBSCRIPTIONS, null);
        if (json == null) return new ArrayList<>();

        Type type = new TypeToken<List<Subscription>>() {}.getType();
        List<Subscription> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    // ── Save ─────────────────────────────────────────────────────────────────

    public static void save(Context ctx, List<Subscription> list) {
        SharedPreferences prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_SUBSCRIPTIONS, gson.toJson(list)).apply();
    }

    // ── Add ──────────────────────────────────────────────────────────────────

    public static void add(Context ctx, Subscription sub) {
        List<Subscription> list = load(ctx);
        list.add(sub);
        save(ctx, list);
    }

    // ── Delete ───────────────────────────────────────────────────────────────

    public static void delete(Context ctx, String id) {
        List<Subscription> list = load(ctx);
        list.removeIf(s -> s.getId().equals(id));
        save(ctx, list);
    }

    // ── Update ───────────────────────────────────────────────────────────────

    public static void update(Context ctx, Subscription updated) {
        List<Subscription> list = load(ctx);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(updated.getId())) {
                list.set(i, updated);
                break;
            }
        }
        save(ctx, list);
    }

    // ── Computed helpers ─────────────────────────────────────────────────────

    /**
     * Total monthly cost of all active subscriptions.
     */
    public static double totalMonthly(Context ctx) {
        double total = 0;
        for (Subscription s : load(ctx)) {
            if (s.isActive()) total += s.monthlyPrice();
        }
        return total;
    }

    /**
     * Active subscriptions count.
     */
    public static int activeCount(Context ctx) {
        int count = 0;
        for (Subscription s : load(ctx)) {
            if (s.isActive()) count++;
        }
        return count;
    }

    /**
     * Subscriptions due within the given number of days.
     */
    public static List<Subscription> dueSoon(Context ctx, long withinDays) {
        List<Subscription> result = new ArrayList<>();
        for (Subscription s : load(ctx)) {
            if (s.isActive() && s.daysLeft() <= withinDays) {
                result.add(s);
            }
        }
        return result;
    }

    /**
     * Subscriptions due within 7 days (default).
     */
    public static List<Subscription> dueSoon(Context ctx) {
        return dueSoon(ctx, 7);
    }
}
