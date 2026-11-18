package com.hisabtech.floatcue;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Local-only storage. No account, network permission or analytics. */
public final class ScriptStore {
    private static final String PREFS = "floatcue";
    private ScriptStore() { }

    public static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static class Script {
        public String id;
        public String title;
        public String body;
        public long updatedAt;
        Script(String id, String title, String body, long updatedAt) {
            this.id = id; this.title = title; this.body = body; this.updatedAt = updatedAt;
        }
    }

    public static List<Script> all(Context ctx) {
        List<Script> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(prefs(ctx).getString("scripts", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Script(o.optString("id"), o.optString("title"),
                    o.optString("body"), o.optLong("updatedAt")));
            }
        } catch (Exception ignored) { }
        if (result.isEmpty()) {
            Script sample = new Script(UUID.randomUUID().toString(), "My first script",
                "Welcome to FloatCue!\n\nPaste your speech, tap Save, then Start floating.\n\nOpen your camera or favorite social media app and read with confidence.",
                System.currentTimeMillis());
            result.add(sample);
            persist(ctx, result);
            select(ctx, sample.id);
        }
        return result;
    }

    private static void persist(Context ctx, List<Script> list) {
        JSONArray array = new JSONArray();
        try {
            for (Script s : list) {
                JSONObject o = new JSONObject();
                o.put("id", s.id); o.put("title", s.title);
                o.put("body", s.body); o.put("updatedAt", s.updatedAt);
                array.put(o);
            }
        } catch (Exception ignored) { }
        prefs(ctx).edit().putString("scripts", array.toString()).apply();
    }

    public static void save(Context ctx, Script script) {
        List<Script> list = all(ctx);
        script.updatedAt = System.currentTimeMillis();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(script.id)) {
                list.set(i, script); found = true; break;
            }
        }
        if (!found) list.add(script);
        persist(ctx, list);
        select(ctx, script.id);
    }

    public static Script create(Context ctx) {
        Script s = new Script(UUID.randomUUID().toString(), "Untitled script", "", System.currentTimeMillis());
        save(ctx, s);
        return s;
    }

    public static void delete(Context ctx, String id) {
        List<Script> list = all(ctx);
        list.removeIf(s -> s.id.equals(id));
        if (list.isEmpty()) {
            Script s = new Script(UUID.randomUUID().toString(), "Untitled script", "", System.currentTimeMillis());
            list.add(s);
        }
        persist(ctx, list);
        select(ctx, list.get(0).id);
    }

    public static void select(Context ctx, String id) {
        prefs(ctx).edit().putString("active_id", id).apply();
    }

    public static Script active(Context ctx) {
        List<Script> list = all(ctx);
        String id = prefs(ctx).getString("active_id", list.get(0).id);
        for (Script s : list) if (s.id.equals(id)) return s;
        return list.get(0);
    }
}
