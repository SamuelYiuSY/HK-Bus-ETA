/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.loohp.hkbuseta.common.objects

import com.loohp.hkbuseta.common.appcontext.AppContext
import com.loohp.hkbuseta.common.appcontext.ReduceDataOmitted
import com.loohp.hkbuseta.common.utils.IOSerializable
import com.loohp.hkbuseta.common.utils.Immutable
import com.loohp.hkbuseta.common.utils.IntOrStringAsIntSerializer
import com.loohp.hkbuseta.common.utils.IntOrStringAsStringSerializer
import com.loohp.hkbuseta.common.utils.JSONSerializable
import com.loohp.hkbuseta.common.utils.NullAsEmptyStringSerializer
import com.loohp.hkbuseta.common.utils.mapToMutableList
import com.loohp.hkbuseta.common.utils.mapToMutableMap
import com.loohp.hkbuseta.common.utils.optBoolean
import com.loohp.hkbuseta.common.utils.optJsonArray
import com.loohp.hkbuseta.common.utils.optJsonObject
import com.loohp.hkbuseta.common.utils.optString
import com.loohp.hkbuseta.common.utils.readBoolean
import com.loohp.hkbuseta.common.utils.readCollection
import com.loohp.hkbuseta.common.utils.readMap
import com.loohp.hkbuseta.common.utils.readNullable
import com.loohp.hkbuseta.common.utils.readString
import com.loohp.hkbuseta.common.utils.toJsonArray
import com.loohp.hkbuseta.common.utils.toJsonObject
import com.loohp.hkbuseta.common.utils.writeBoolean
import com.loohp.hkbuseta.common.utils.writeCollection
import com.loohp.hkbuseta.common.utils.writeMap
import com.loohp.hkbuseta.common.utils.writeNullable
import com.loohp.hkbuseta.common.utils.writeString
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.readInt
import kotlinx.io.Sink
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
@Immutable
class Route(
    @SerialName("route") val routeNumber: String,
    val bound: Map<Operator, String>,
    val co: List<Operator>,
    @Serializable(with = IntOrStringAsStringSerializer::class) val serviceType: String,
    @Serializable(with = NullAsEmptyStringSerializer::class) val nlbId: String,
    @Serializable(with = NullAsEmptyStringSerializer::class) val gtfsId: String,
    @SerialName("ctbIsCircular") val isCtbIsCircular: Boolean = false,
    @SerialName("kmbCtbJoint") val isKmbCtbJoint: Boolean = false,
    val gmbRegion: GMBRegion? = null,
    val lrtCircular: BilingualText? = null,
    val dest: BilingualText,
    val orig: BilingualText,
    val fakeRoute: Boolean = false,
    val stops: Map<Operator, List<String>>,
    @ReduceDataOmitted val fares: List<Fare>? = null,
    @ReduceDataOmitted val faresHoliday: List<Fare>? = null,
    val freq: Map<String, Map<String, List<String>?>>? = null,
    @SerialName("jt") @Serializable(with = IntOrStringAsIntSerializer::class) val journeyTime: Int? = null
) : JSONSerializable, IOSerializable {

    companion object {

        fun deserialize(json: JsonObject): Route {
            val route = json.optString("route")
            val bound = json.optJsonObject("bound")!!.mapToMutableMap({ Operator.valueOf(it) }, { it.jsonPrimitive.content })
            val co = json.optJsonArray("co")!!.map { Operator.valueOf(it.jsonPrimitive.content) }
            val serviceType = json.optString("serviceType")
            val nlbId = json.optString("nlbId")
            val gtfsId = json.optString("gtfsId")
            val ctbIsCircular = json.optBoolean("ctbIsCircular")
            val kmbCtbJoint = json.optBoolean("kmbCtbJoint")
            val gmbRegion = if (json.contains("gmbRegion")) GMBRegion.valueOfOrNull(json.optString("gmbRegion")) else null
            val lrtCircular = if (json.contains("lrtCircular")) BilingualText.deserialize(json.optJsonObject("lrtCircular")!!) else null
            val dest = BilingualText.deserialize(json.optJsonObject("dest")!!)
            val orig = BilingualText.deserialize(json.optJsonObject("orig")!!)
            val fakeRoute = json.optBoolean("fakeRoute", false)
            val stops = json.optJsonObject("stops")!!.mapToMutableMap({ Operator.valueOf(it) }, { it.jsonArray.mapToMutableList { e -> e.jsonPrimitive.content } })
            val fares = json.optJsonArray("fares")?.mapToMutableList { Fare(it.jsonPrimitive.content) }
            val faresHoliday = json.optJsonArray("faresHoliday")?.mapToMutableList { Fare(it.jsonPrimitive.content) }
            val freq = json.optJsonObject("freq")?.mapToMutableMap { m -> m.jsonObject.mapToMutableMap { a -> (a as? JsonArray)?.mapToMutableList { e -> e.jsonPrimitive.content } } }
            val journeyTime = json.optString("jt").toIntOrNull()
            return Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, fakeRoute, stops, fares, faresHoliday, freq, journeyTime)
        }

        suspend fun deserialize(input: ByteReadChannel): Route {
            val route = input.readString(Charsets.UTF_8)
            val bound = input.readMap(linkedMapOf()) { Operator.valueOf(it.readString(Charsets.UTF_8)) to it.readString(Charsets.UTF_8) }
            val co = input.readCollection(mutableListOf()) { Operator.valueOf(it.readString(Charsets.UTF_8)) }
            val serviceType = input.readString(Charsets.UTF_8)
            val nlbId = input.readString(Charsets.UTF_8)
            val gtfsId = input.readString(Charsets.UTF_8)
            val ctbIsCircular = input.readBoolean()
            val kmbCtbJoint = input.readBoolean()
            val gmbRegion = input.readNullable { GMBRegion.valueOfOrNull(it.readString(Charsets.UTF_8)) }
            val lrtCircular = input.readNullable { BilingualText.deserialize(it) }
            val dest = BilingualText.deserialize(input)
            val orig = BilingualText.deserialize(input)
            val fakeRoute = input.readBoolean()
            val stops = input.readMap(linkedMapOf()) { Operator.valueOf(it.readString(Charsets.UTF_8)) to it.readCollection(mutableListOf()) { it1 -> it1.readString(Charsets.UTF_8) } }
            val fares = input.readNullable { i -> i.readCollection(mutableListOf()) { Fare(it.readString(Charsets.UTF_8)) } }
            val faresHoliday = input.readNullable { i -> i.readCollection(mutableListOf()) { Fare(it.readString(Charsets.UTF_8)) } }
            val freq = input.readNullable { n -> n.readMap(mutableMapOf()) { m -> m.readString(Charsets.UTF_8) to m.readMap(mutableMapOf()) { a -> a.readString(Charsets.UTF_8) to a.readNullable { an -> an.readCollection(mutableListOf()) { e -> e.readString(Charsets.UTF_8) } } } } }
            val journeyTime = input.readNullable { i -> i.readInt() }
            return Route(route, bound, co, serviceType, nlbId, gtfsId, ctbIsCircular, kmbCtbJoint, gmbRegion, lrtCircular, dest, orig, fakeRoute, stops, fares, faresHoliday, freq, journeyTime)
        }
    }

    override fun serialize(): JsonObject {
        return buildJsonObject {
            put("route", routeNumber)
            put("bound", bound.toJsonObject { it })
            put("co", co.asSequence().map { o -> o.name }.toJsonArray())
            put("serviceType", serviceType)
            put("nlbId", nlbId)
            put("gtfsId", gtfsId)
            put("ctbIsCircular", isCtbIsCircular)
            put("kmbCtbJoint", isKmbCtbJoint)
            if (gmbRegion != null) {
                put("gmbRegion", gmbRegion.name)
            }
            if (lrtCircular != null) {
                put("lrtCircular", lrtCircular.serialize())
            }
            put("dest", dest.serialize())
            put("orig", orig.serialize())
            put("fakeRoute", fakeRoute)
            put("stops", stops.toJsonObject { it.toJsonArray() })
            if (fares != null) {
                put("fares", fares.toJsonArray())
            }
            if (faresHoliday != null) {
                put("faresHoliday", faresHoliday.toJsonArray())
            }
            if (freq != null) {
                put("freq", freq.toJsonObject { m -> m.toJsonObject { a -> a?.toJsonArray()?: JsonNull } })
            }
            if (journeyTime != null) {
                put("jt", journeyTime.toString())
            }
        }
    }

    override fun serialize(out: Sink) {
        out.writeString(routeNumber, Charsets.UTF_8)
        out.writeMap(bound) { o, k, v -> o.writeString(k.name, Charsets.UTF_8) to o.writeString(v, Charsets.UTF_8) }
        out.writeCollection(co) { o, t -> o.writeString(t.name, Charsets.UTF_8) }
        out.writeString(serviceType, Charsets.UTF_8)
        out.writeString(nlbId, Charsets.UTF_8)
        out.writeString(gtfsId, Charsets.UTF_8)
        out.writeBoolean(isCtbIsCircular)
        out.writeBoolean(isKmbCtbJoint)
        out.writeNullable(gmbRegion) { o, v -> o.writeString(v.name, Charsets.UTF_8) }
        out.writeNullable(lrtCircular) { o, v -> v.serialize(o) }
        dest.serialize(out)
        orig.serialize(out)
        out.writeBoolean(fakeRoute)
        out.writeMap(stops) { o, k, v -> o.writeString(k.name, Charsets.UTF_8) to o.writeCollection(v) { o1, t1 -> o1.writeString(t1, Charsets.UTF_8) } }
        out.writeNullable(fares) { o, v -> o.writeCollection(v) { o1, v1 -> o1.writeString(v1.toString(), Charsets.UTF_8) } }
        out.writeNullable(faresHoliday) { o, v -> o.writeCollection(v) { o1, v1 -> o1.writeString(v1.toString(), Charsets.UTF_8) } }
        out.writeNullable(freq) { o, n -> o.writeMap(n) { o1, mk, mv ->
            o1.writeString(mk, Charsets.UTF_8)
            o1.writeMap(mv) { o2, ak, av ->
                o2.writeString(ak, Charsets.UTF_8)
                o2.writeNullable(av) { o3, avn ->
                    o3.writeCollection(avn) { o4, e ->
                        o4.writeString(e, Charsets.UTF_8)
                    }
                }
            }
        } }
        out.writeNullable(journeyTime) { o, t -> o.writeInt(t) }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Route) return false

        if (routeNumber != other.routeNumber) return false
        if (bound != other.bound) return false
        if (co != other.co) return false
        if (serviceType != other.serviceType) return false
        if (nlbId != other.nlbId) return false
        if (gtfsId != other.gtfsId) return false
        if (isCtbIsCircular != other.isCtbIsCircular) return false
        if (isKmbCtbJoint != other.isKmbCtbJoint) return false
        if (gmbRegion != other.gmbRegion) return false
        if (lrtCircular != other.lrtCircular) return false
        if (dest != other.dest) return false
        if (orig != other.orig) return false
        if (fakeRoute != other.fakeRoute) return false
        return stops == other.stops
    }

    override fun hashCode(): Int {
        var result = routeNumber.hashCode()
        result = 31 * result + bound.hashCode()
        result = 31 * result + co.hashCode()
        result = 31 * result + serviceType.hashCode()
        result = 31 * result + nlbId.hashCode()
        result = 31 * result + gtfsId.hashCode()
        result = 31 * result + isCtbIsCircular.hashCode()
        result = 31 * result + isKmbCtbJoint.hashCode()
        result = 31 * result + (gmbRegion?.hashCode() ?: 0)
        result = 31 * result + (lrtCircular?.hashCode() ?: 0)
        result = 31 * result + dest.hashCode()
        result = 31 * result + orig.hashCode()
        result = 31 * result + fakeRoute.hashCode()
        result = 31 * result + stops.hashCode()
        return result
    }

    override fun toString(): String {
        val co = co.firstCo()!!
        return "$co $routeNumber ${idBoundWithRegion(co)} ($serviceType): ${super.toString()}"
    }
}

fun Route.toRouteSearchResult(instance: AppContext): RouteSearchResultEntry {
    val routeKey = getRouteKey(instance)!!
    return RouteSearchResultEntry(routeKey, this, co.firstCo()!!)
}

inline infix fun Route?.strictEquals(other: Route?): Boolean {
    return this == other && this?.fares == other?.fares && this?.faresHoliday == other?.faresHoliday
}