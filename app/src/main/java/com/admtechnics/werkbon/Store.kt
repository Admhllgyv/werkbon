package com.admtechnics.werkbon

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class Worker(val name: String, val rate: Double)

data class Customer(
    val name: String,
    val address: String,
    val btw: String
)

data class Project(
    val code: String, 
    val name: String,
    val customerName: String = "" 
)

data class ProformaItem(
    val description: String,
    val quantity: Double,
    val price: Double
)

data class Proforma(
    val id: String,
    val projectName: String,
    val date: String,
    val items: List<ProformaItem>,
    val note: String = ""
)

data class AppState(
    val compName: String,
    val compAddress: String,
    val compEmail: String,
    val compGsm: String,
    val compBtw: String,
    val compLogoPath: String,
    val workers: List<Worker>,
    val projects: List<Project>,
    val categories: List<String>,
    val proformas: List<Proforma> = emptyList(),
    val customers: List<Customer> = emptyList()
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
            Project("E260001", "Kruidvat Diepenbeek", "Kruidvat"),
            Project("E260002", "ICI PARIS XL Hasselt", "ICI Paris XL")
        ),
        categories = listOf("Malzeme", "Parking", "Eten", "Hotel"),
        proformas = emptyList(),
        customers = listOf(
            Customer("Kruidvat", "België", "BE 0123.456.789"),
            Customer("ICI Paris XL", "België", "BE 987.654.321")
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
        o.put("compLogoPath", s.compLogoPath)
        
        val wa = JSONArray()
        for (w in s.workers) wa.put(JSONObject().put("name", w.name).put("rate", w.rate))
        o.put("workers", wa)
        
        val pa = JSONArray()
        for (p in s.projects) pa.put(JSONObject().put("code", p.code).put("name", p.name).put("customerName", p.customerName))
        o.put("projects", pa)
        
        val ca = JSONArray()
        for (c in s.categories) ca.put(c)
        o.put("categories", ca)

        val cua = JSONArray()
        for (c in s.customers) cua.put(JSONObject().put("name", c.name).put("address", c.address).put("btw", c.btw))
        o.put("customers", cua)

        val pra = JSONArray()
        for (pr in s.proformas) {
            val prObj = JSONObject().put("id", pr.id).put("projectName", pr.projectName).put("date", pr.date).put("note", pr.note)
            val ia = JSONArray()
            for (item in pr.items) ia.put(JSONObject().put("description", item.description).put("quantity", item.quantity).put("price", item.price))
            prObj.put("items", ia)
            pra.put(prObj)
        }
        o.put("proformas", pra)
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
            projects.add(Project(x.getString("code"), x.getString("name"), x.optString("customerName", "")))
        }
        
        val cats = ArrayList<String>()
        val ca = o.optJSONArray("categories") ?: JSONArray()
        for (i in 0 until ca.length()) cats.add(ca.getString(i))

        val customers = ArrayList<Customer>()
        val cua = o.optJSONArray("customers") ?: JSONArray()
        for (i in 0 until cua.length()) {
            val x = cua.getJSONObject(i)
            customers.add(Customer(x.getString("name"), x.getString("address"), x.getString("btw")))
        }

        val proformas = ArrayList<Proforma>()
        val pra = o.optJSONArray("proformas") ?: JSONArray()
        for (i in 0 until pra.length()) {
            val pObj = pra.getJSONObject(i)
            val items = ArrayList<ProformaItem>()
            val ia = pObj.optJSONArray("items") ?: JSONArray()
            for (j in 0 until ia.length()) {
                val iObj = ia.getJSONObject(j)
                items.add(ProformaItem(iObj.getString("description"), iObj.optDouble("quantity", 1.0), iObj.optDouble("price", 0.0)))
            }
            proformas.add(Proforma(pObj.getString("id"), pObj.getString("projectName"), pObj.getString("date"), items, pObj.optString("note", "")))
        }

        return AppState(
            compName = o.optString("compName", d.compName),
            compAddress = o.optString("compAddress", d.compAddress),
            compEmail = o.optString("compEmail", d.compEmail),
            compGsm = o.optString("compGsm", d.compGsm),
            compBtw = o.optString("compBtw", d.compBtw),
            compLogoPath = o.optString("compLogoPath", ""),
            workers = workers,
            projects = projects,
            categories = cats,
            proformas = proformas,
            customers = customers
        )
    }
}
