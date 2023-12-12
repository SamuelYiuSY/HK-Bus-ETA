/*
 * This file is part of HKBusETA.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.hkbuseta

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.loohp.hkbuseta.objects.Operator
import com.loohp.hkbuseta.objects.Route
import com.loohp.hkbuseta.objects.gmbRegion
import com.loohp.hkbuseta.objects.operator
import com.loohp.hkbuseta.shared.Registry
import com.loohp.hkbuseta.shared.Shared
import com.loohp.hkbuseta.theme.HKBusETATheme
import com.loohp.hkbuseta.utils.clamp
import com.loohp.hkbuseta.utils.scaledSize
import com.loohp.hkbuseta.utils.toJsonArray
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlin.math.absoluteValue


@Stable
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        Shared.setDefaultExceptionHandler(this)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val stopId = intent.extras?.getString("stopId")
        val co = intent.extras?.getString("co")?.operator
        val index = intent.extras?.getInt("index")
        val stop = intent.extras?.get("stop")
        val route = intent.extras?.get("route")

        val listStopRoute = intent.extras?.getByteArray("stopRoute")
        val listStopScrollToStop = intent.extras?.getString("scrollToStop")
        val listStopShowEta = intent.extras?.getBoolean("showEta")
        val listStopIsAlightReminder = intent.extras?.getBoolean("isAlightReminder")

        val queryKey = intent.extras?.getString("k")
        var queryRouteNumber = intent.extras?.getString("r")
        var queryBound = intent.extras?.getString("b")
        var queryCo = intent.extras?.getString("c")?.operator
        val queryDest = intent.extras?.getString("d")
        var queryGMBRegion = intent.extras?.getString("g")?.gmbRegion
        val queryStop = intent.extras?.getString("s")
        val queryStopIndex = intent.extras?.getInt("si")?: 0
        val queryStopDirectLaunch = intent.extras?.getBoolean("sd", false)?: false

        setContent {
            val state by remember { Registry.getInstance(this@MainActivity).also {
                if (System.currentTimeMillis() - it.lastUpdateCheck > 30000) it.checkUpdate(this@MainActivity, false)
            }.state }.collectAsStateWithLifecycle()

            LaunchedEffect (state) {
                when (state) {
                    Registry.State.READY -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (stopId != null && co != null && (stop is String || stop is ByteArray) && (route is String || route is ByteArray)) {
                                val routeParsed = if (route is String) Route.deserialize(Json.decodeFromString<JsonObject>(route)) else runBlocking { Route.deserialize(ByteReadChannel(route as ByteArray)) }
                                Registry.getInstance(this@MainActivity).findRoutes(routeParsed.routeNumber, true) { it ->
                                    val bound = it.bound
                                    if (!bound.containsKey(co) || bound[co] != routeParsed.bound[co]) {
                                        return@findRoutes false
                                    }
                                    val stops = it.stops[co]?: return@findRoutes false
                                    return@findRoutes stops.contains(stopId)
                                }.firstOrNull()?.let {
                                    val intent = Intent(this@MainActivity, ListStopsActivity::class.java)
                                    intent.putExtra("shouldRelaunch", false)
                                    intent.putExtra("route", it.toByteArray())
                                    intent.putExtra("scrollToStop", stopId)
                                    startActivity(intent)
                                }

                                val intent = Intent(this@MainActivity, EtaActivity::class.java)
                                intent.putExtra("shouldRelaunch", false)
                                intent.putExtra("stopId", stopId)
                                intent.putExtra("co", co.name)
                                intent.putExtra("index", index)
                                if (stop is String) {
                                    intent.putExtra("stopStr", stop)
                                } else {
                                    intent.putExtra("stop", stop as ByteArray)
                                }
                                if (route is String) {
                                    intent.putExtra("routeStr", route)
                                } else {
                                    intent.putExtra("route", route as ByteArray)
                                }
                                startActivity(intent)
                                finish()
                            } else if (listStopRoute != null && listStopScrollToStop != null && listStopShowEta != null && listStopIsAlightReminder != null) {
                                val intent = Intent(this@MainActivity, ListStopsActivity::class.java)
                                intent.putExtra("route", listStopRoute)
                                intent.putExtra("scrollToStop", listStopScrollToStop)
                                intent.putExtra("showEta", listStopShowEta)
                                intent.putExtra("isAlightReminder", listStopIsAlightReminder)
                                startActivity(intent)
                                finish()
                            } else if (queryRouteNumber != null || queryKey != null) {
                                if (queryKey != null) {
                                    val routeNumber = Regex("^([0-9a-zA-Z]+)").find(queryKey)?.groupValues?.getOrNull(1)
                                    val nearestRoute = Registry.getInstance(this@MainActivity).findRouteByKey(queryKey, routeNumber)
                                    queryRouteNumber = nearestRoute!!.routeNumber
                                    queryCo = if (nearestRoute.isKmbCtbJoint) Operator.KMB else nearestRoute.co[0]
                                    queryBound = if (queryCo == Operator.NLB) nearestRoute.nlbId else nearestRoute.bound[queryCo]
                                    queryGMBRegion = nearestRoute.gmbRegion
                                }

                                startActivity(Intent(this@MainActivity, TitleActivity::class.java))

                                val result = Registry.getInstance(this@MainActivity).findRoutes(queryRouteNumber?: "", true)
                                if (result.isNotEmpty()) {
                                    var filteredResult = result.asSequence().filter {
                                        return@filter when (queryCo) {
                                            Operator.NLB -> (queryCo == null || it.co == queryCo) && (queryBound == null || it.route!!.nlbId == queryBound)
                                            Operator.GMB -> {
                                                val r = it.route!!
                                                (queryCo == null || it.co == queryCo) && (queryBound == null || r.bound[queryCo] == queryBound) && r.gmbRegion == queryGMBRegion
                                            }
                                            else -> (queryCo == null || it.co == queryCo) && (queryBound == null || it.route!!.bound[queryCo] == queryBound)
                                        }
                                    }.toList()
                                    if (queryDest != null) {
                                        val destFiltered = filteredResult.asSequence().filter {
                                            val dest = it.route!!.dest
                                            return@filter queryDest == dest.zh || queryDest == dest.en
                                        }.toList()
                                        if (destFiltered.isNotEmpty()) {
                                            filteredResult = destFiltered
                                        }
                                    }
                                    if (filteredResult.isEmpty()) {
                                        val intent = Intent(this@MainActivity, ListRoutesActivity::class.java)
                                        intent.putExtra("result", result.asSequence().map {
                                            val clone = it.deepClone()
                                            clone.strip()
                                            clone.serialize()
                                        }.toJsonArray().toString())
                                        startActivity(intent)
                                    } else {
                                        val intent = Intent(this@MainActivity, ListRoutesActivity::class.java)
                                        intent.putExtra("result", filteredResult.asSequence().map {
                                            val clone = it.deepClone()
                                            clone.strip()
                                            clone.serialize()
                                        }.toJsonArray().toString())
                                        startActivity(intent)

                                        val it = filteredResult[0]
                                        val meta = when (it.co) {
                                            Operator.GMB -> it.route!!.gmbRegion!!.name
                                            Operator.NLB -> it.route!!.nlbId
                                            else -> ""
                                        }
                                        Registry.getInstance(this@MainActivity).addLastLookupRoute(queryRouteNumber, it.co, meta, this@MainActivity)

                                        if (queryStop != null) {
                                            val intent2 = Intent(this@MainActivity, ListStopsActivity::class.java)
                                            intent2.putExtra("route", it.toByteArray())
                                            intent2.putExtra("scrollToStop", queryStop)
                                            startActivity(intent2)

                                            if (queryStopDirectLaunch) {
                                                val stops = Registry.getInstance(this@MainActivity).getAllStops(queryRouteNumber!!, queryBound!!, queryCo!!, queryGMBRegion)
                                                stops.withIndex().filter { it.value.stopId == queryStop }.minByOrNull { (queryStopIndex - it.index).absoluteValue }?.let { r ->
                                                    val (i, stopData) = r
                                                    val intent3 = Intent(this@MainActivity, EtaActivity::class.java)
                                                    intent3.putExtra("stopId", stopId)
                                                    intent3.putExtra("co", it.co.name)
                                                    intent3.putExtra("index", i + 1)
                                                    intent3.putExtra("stop", stopData.stop.toByteArray())
                                                    intent3.putExtra("route", it.route!!.toByteArray())
                                                    startActivity(intent3)
                                                }
                                            }
                                        } else if (filteredResult.size == 1) {
                                            val intent2 = Intent(this@MainActivity, ListStopsActivity::class.java)
                                            intent2.putExtra("route", it.toByteArray())
                                            startActivity(intent2)
                                        }
                                    }
                                }
                                finishAffinity()
                            } else {
                                val currentActivity = Shared.getCurrentActivity()
                                if (currentActivity == null || !currentActivity.shouldRelaunch) {
                                    startActivity(Intent(this@MainActivity, TitleActivity::class.java))
                                    finishAffinity()
                                } else {
                                    val intent2 = Intent(this@MainActivity, currentActivity.cls)
                                    if (currentActivity.extras != null) {
                                        intent2.putExtras(currentActivity.extras)
                                    }
                                    startActivity(intent2)
                                    finishAffinity()
                                }
                            }
                        }
                    }
                    Registry.State.ERROR -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            val intent = Intent(this@MainActivity, FatalErrorActivity::class.java)
                            intent.putExtra("zh", "發生錯誤\n請檢查您的網絡連接")
                            intent.putExtra("en", "Fatal Error\nPlease check your internet connection")
                            startActivity(intent)
                            finish()
                        }
                    }
                    else -> {}
                }
            }

            Loading(this)
        }
    }
}

@Composable
fun Loading(instance: MainActivity) {
    HKBusETATheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            verticalArrangement = Arrangement.Top
        ) {
            Shared.MainTime()
        }
        LoadingUpdatingElements(instance)
    }
}

@Composable
fun LoadingUpdatingElements(instance: MainActivity) {
    val state by remember { Registry.getInstance(instance).state }.collectAsStateWithLifecycle()
    var wasUpdating by remember { mutableStateOf(state == Registry.State.UPDATING) }
    val updating by remember { derivedStateOf { wasUpdating || state == Registry.State.UPDATING } }

    LaunchedEffect (updating, state) {
        if (updating) {
            wasUpdating = true
        }
    }

    if (updating) {
        UpdatingElements(instance)
    } else {
        LoadingElements(instance)
    }
}

@Composable
fun UpdatingElements(instance: MainActivity) {
    val currentProgress by remember { Registry.getInstanceNoUpdateCheck(instance).updatePercentageState }.collectAsStateWithLifecycle()
    val progressAnimation by animateFloatAsState(
        targetValue = currentProgress,
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
        label = "LoadingProgressAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp, 0.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 17F.scaledSize(instance).sp,
            text = "更新數據中..."
        )
        Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 14F.scaledSize(instance).sp,
            text = "更新需時 請稍等"
        )
        Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 17F.scaledSize(instance).sp,
            text = "Updating..."
        )
        Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colors.primary,
            fontSize = 14F.scaledSize(instance).sp,
            text = "Might take a moment"
        )
        Spacer(modifier = Modifier.size(10.scaledSize(instance).dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .padding(25.dp, 0.dp),
            color = Color(0xFFF9DE09),
            trackColor = Color(0xFF797979),
            progress = progressAnimation
        )
    }
}

@Composable
fun LoadingElements(instance: MainActivity) {
    val currentState by remember { Registry.getInstanceNoUpdateCheck(instance).state }.collectAsStateWithLifecycle()
    val checkingUpdate by remember { derivedStateOf { currentState == Registry.State.UPDATE_CHECKING } }

    Box (
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp, 0.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Image(
                    modifier = Modifier
                        .size(50.scaledSize(instance).dp)
                        .align(Alignment.Center),
                    painter = painterResource(R.mipmap.icon_full_smaller),
                    contentDescription = instance.resources.getString(R.string.app_name)
                )
            }
            Spacer(modifier = Modifier.size(10.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 17F.scaledSize(instance).sp,
                text = "載入中..."
            )
            Spacer(modifier = Modifier.size(2.scaledSize(instance).dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 17F.scaledSize(instance).sp,
                text = "Loading..."
            )
        }
        if (checkingUpdate) {
            Box (
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(0.dp, 10.dp)
            ) {
                SkipChecksumButton(instance)
            }
        }
    }
}

@Composable
fun SkipChecksumButton(instance: MainActivity) {
    var enableSkip by remember { mutableStateOf(false) }

    val alpha by remember { derivedStateOf { if (enableSkip) 1F else 0F } }
    val animatedAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = TweenSpec(durationMillis = 400, easing = LinearEasing),
        label = ""
    )

    LaunchedEffect (Unit) {
        delay(3000)
        enableSkip = true
    }

    Button(
        onClick = {
            Registry.getInstanceNoUpdateCheck(instance).cancelCurrentChecksumTask()
        },
        modifier = Modifier
            .padding(20.dp, 0.dp)
            .width(55.scaledSize(instance).dp)
            .height(35.scaledSize(instance).dp)
            .alpha(animatedAlpha),
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary,
            contentColor = Color(0xFFFFFFFF)
        ),
        enabled = enableSkip,
        content = {
            Text(
                modifier = Modifier.fillMaxWidth(0.9F),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colors.primary,
                fontSize = 14F.scaledSize(instance).sp.clamp(max = 14.dp),
                text = if (Shared.language == "en") "Skip" else "略過"
            )
        }
    )
}