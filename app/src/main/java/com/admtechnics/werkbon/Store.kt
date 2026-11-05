package com.admtechnics.werkbon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Worker(val name: String, val rate: Double)
data class Project(val code: String, val name: String)

data class AppState(
    val compName: String,
    val compAddress: String,
    val compEmail: String,
    val compGsm: String,
    val compBtw: String,
    val compLogoPath: String,        // YENİ — logo dosya yolu
    val workers: List<Worker>,
    val projects: List<Project>,
    val categories: List<String>
)

object Store {
    private const val FILE_NAME = "werkbon_data.json"

    fun defaults() = AppState(
        compName = "ADM TECHNICS",
        compAddress = "Scheepvaartstraat 5, Diepenbeek/België",
        compEmail = "admtechnics2018@gmail.com",
        compGsm = "0467752557",
        compBtw = "0707743276",
        compLogoPath = "",
        workers = listOf(Worker("Adem", 25.0), Worker("Melih", 20.0)),
        projects = listOf(
            Project("E260001", "Kruidvat Diepenbeek"),
            Project("E260002", "ICI PARIS XL Hasselt"),
            Project("E260246", "Kruidvat Brakel")
        ),
        categories = listOf(
            "Malzeme", "Su borusu", "Şalter", "Parking",
            "Kabel", "Verlichting", "Eten", "Hotel"
        )
    )

    fun load(ctx: Context): AppState {
        val f = File(ctx.filesDir, FILE_NAME)
        if (!f.exists()) return defaults()
        return try { parse(JSONObject(f.readText())) } catch (e: Exception) { defaults() }
    }

    fun save(ctx: Context, s: AppState) {
        File(ctx.filesDir, FILE_NAME).writeText(toJson(s).toString())
    }

    private fun toJson(s: AppState): JSONObject {
        val o = JSONObject()
        o.put("compName", s.compName)
        o.put("compAddress", s.compAddress)
        o.put("compEmail", s.compEmail)
        o.put("compGsm", s.compGsm)
        o.put("compBtw", s.compBtw)
        o.put("compLogoPath", s.compLogoPath)   // YENİ
        val wa = JSONArray()
        for (w in s.workers) wa.put(JSONObject().put("name", w.name).put("rate", w.rate))
        o.put("workers", wa)
        val pa = JSONArray()
        for (p in s.projects) pa.put(JSONObject().put("code", p.code).put("name", p.name))
        o.put("projects", pa)
        val ca = JSONArray()
        for (c in s.categories) ca.put(c)
        o.put("categories", ca)
        return o
    }

    private fun parse(o: JSONObject): AppState {
        val d = defaults()
        val workers = ArrayList<Worker>()
        val wa = o.optJSONArray("workers") ?: JSONArray()
        for (i in 0 until wa.length()) {
            val x = wa.getJSONObject(i)
            workers.add(Worker(x.getString("name"), x.optDouble("rate", 20.0)))
        }
        val projects = ArrayList<Project>()
        val pa = o.optJSONArray("projects") ?: JSONArray()
        for (i in 0 until pa.length()) {
            val x = pa.getJSONObject(i)
            projects.add(Project(x.getString("code"), x.getString("name")))
        }
        val cats = ArrayList<String>()
        val ca = o.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until ca.length()) cats.add(ca.getString(i))
        return AppState(
            compName = o.optString("compName", d.compName),
            compAddress = o.optString("compAddress", d.compAddress),
            compEmail = o.optString("compEmail", d.compEmail),
            compGsm = o.optString("compGsm", d.compGsm),
            compBtw = o.optString("compBtw", d.compBtw),
            compLogoPath = o.optString("compLogoPath", ""),   // YENİ
            workers = workers,
            projects = projects,
            categories = cats
        )
    }
}
