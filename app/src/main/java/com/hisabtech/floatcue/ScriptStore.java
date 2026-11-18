package com.hisabtech.floatcue;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Private, local-only script library with explicit portable backup and safe restore. */
public final class ScriptStore {
    private static final String PREFS = "floatcue";
    private static final int MAX_SCRIPTS = 300;
    private static final int MAX_SCRIPT_LENGTH = 450000;
    private ScriptStore() { }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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

    private static Script sample() {
        return new Script(UUID.randomUUID().toString(), "My first script",
            "Welcome to FloatCue!\n\nYour voice deserves a great stage.\n\n"
                + "Write or paste your speech, choose a reading style, and launch your floating reader.",
            System.currentTimeMillis());
    }

    private static JSONArray serialize(List<Script> scripts) throws JSONException {
        JSONArray list = new JSONArray();
        for (Script s : scripts) {
            JSONObject o = new JSONObject();
            o.put("id", s.id); o.put("title", s.title);
            o.put("body", s.body); o.put("updatedAt", s.updatedAt);
            list.put(o);
        }
        return list;
    }

    private static List<Script> deserialize(JSONArray array, boolean strict) throws JSONException {
        if (array.length() > MAX_SCRIPTS) throw new JSONException("Too many scripts");
        List<Script> out = new ArrayList<>();
        Set<String> known = new HashSet<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            String id = item.optString("id", "");
            String title = item.optString("title", "");
            String body = item.optString("body", "");
            if (strict && (body.length() > MAX_SCRIPT_LENGTH || title.length() > 250))
                throw new JSONException("Script too large");
            if (id.isEmpty() || known.contains(id)) id = UUID.randomUUID().toString();
            known.add(id);
            if (title.trim().isEmpty()) title = "Untitled script";
            if (!strict) {
                title = title.length() > 250 ? title.substring(0, 250) : title;
                body = body.length() > MAX_SCRIPT_LENGTH ? body.substring(0, MAX_SCRIPT_LENGTH) : body;
            }
            out.add(new Script(id, title, body, item.optLong("updatedAt", System.currentTimeMillis())));
        }
        return out;
    }

    public static synchronized List<Script> all(Context context) {
        String raw = prefs(context).getString("scripts", "");
        if (!raw.isEmpty()) {
            try {
                List<Script> scripts = deserialize(new JSONArray(raw), false);
                if (!scripts.isEmpty()) return scripts;
            } catch (Exception malformed) {
                // Preserve the original for recovery before resetting the working library.
                prefs(context).edit().putString("recoverable_library_json", raw).apply();
            }
        }
        List<Script> initial = new ArrayList<>();
        initial.add(sample());
        persist(context, initial);
        select(context, initial.get(0).id);
        return initial;
    }

    private static void persist(Context context, List<Script> list) {
        try {
            prefs(context).edit().putString("scripts", serialize(list).toString()).apply();
        } catch (JSONException ignored) { }
    }

    public static synchronized void save(Context context, Script script) {
        if (script.body == null) script.body = "";
        if (script.title == null || script.title.trim().isEmpty()) script.title = "Untitled script";
        if (script.body.length() > MAX_SCRIPT_LENGTH)
            script.body = script.body.substring(0, MAX_SCRIPT_LENGTH);
        List<Script> list = all(context);
        script.updatedAt = System.currentTimeMillis();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(script.id)) {
                list.set(i, script); found = true; break;
            }
        }
        if (!found && list.size() < MAX_SCRIPTS) list.add(script);
        persist(context, list);
        select(context, script.id);
    }

    public static Script create(Context context) {
        Script s = new Script(UUID.randomUUID().toString(), "Untitled script", "", System.currentTimeMillis());
        save(context, s);
        return s;
    }

    public static synchronized void delete(Context context, String id) {
        List<Script> list = all(context);
        list.removeIf(s -> s.id.equals(id));
        if (list.isEmpty()) list.add(sample());
        persist(context, list);
        select(context, list.get(0).id);
    }

    public static void select(Context context, String id) {
        prefs(context).edit().putString("active_id", id).apply();
    }

    public static Script active(Context context) {
        List<Script> list = all(context);
        String id = prefs(context).getString("active_id", list.get(0).id);
        for (Script s : list) if (s.id.equals(id)) return s;
        return list.get(0);
    }

    public static String exportJson(Context context) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("format", "floatcue-library");
        json.put("version", 1);
        json.put("exportedAt", System.currentTimeMillis());
        json.put("activeId", active(context).id);
        json.put("scripts", serialize(all(context)));
        return json.toString(2);
    }

    /** Validates the entire backup before replacing any existing data. */
    public static synchronized int restoreJson(Context context, String contents) throws JSONException {
        if (contents == null || contents.length() > 2 * 1024 * 1024)
            throw new JSONException("Backup exceeds size limit");
        JSONObject json = new JSONObject(contents);
        if (!"floatcue-library".equals(json.optString("format")) || json.optInt("version") != 1)
            throw new JSONException("Not a FloatCue backup");
        List<Script> scripts = deserialize(json.getJSONArray("scripts"), true);
        if (scripts.isEmpty()) throw new JSONException("Backup contains no scripts");
        String activeId = json.optString("activeId", "");
        boolean present = false;
        for (Script s : scripts) if (s.id.equals(activeId)) present = true;
        if (!present) activeId = scripts.get(0).id;
        SharedPreferences prefs = prefs(context);
        String before = prefs.getString("scripts", "");
        // A restore is deliberate, but retain one previous snapshot in case of an accidental replacement.
        if (!prefs.edit().putString("pre_restore_backup", before)
            .putString("scripts", serialize(scripts).toString())
            .putString("active_id", activeId).commit())
            throw new JSONException("Storage write failed");
        return scripts.size();
    }
}
