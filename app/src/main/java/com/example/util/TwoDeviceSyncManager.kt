package com.example.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.data.engine.WageEngine
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.repository.CoopRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.UUID

object TwoDeviceSyncManager {

    private const val SERVER_PORT = 8989
    private var httpServer: HttpServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    private val _partnerIp = MutableStateFlow("")
    val partnerIp: StateFlow<String> = _partnerIp.asStateFlow()

    private val _syncLog = MutableStateFlow("Local sync standby")
    val syncLog: StateFlow<String> = _syncLog.asStateFlow()

    private val _demoSimulationEnabled = MutableStateFlow(true)
    val demoSimulationEnabled: StateFlow<Boolean> = _demoSimulationEnabled.asStateFlow()

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        appContext = context.applicationContext
        refreshLocalIp()
        startServer()
    }

    fun setPartnerIp(ip: String) {
        _partnerIp.value = ip.trim()
    }

    fun setDemoSimulation(enabled: Boolean) {
        _demoSimulationEnabled.value = enabled
    }

    fun refreshLocalIp(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        val host = address.hostAddress ?: "127.0.0.1"
                        _localIp.value = host
                        return host
                    }
                }
            }
        } catch (e: Exception) {
            _localIp.value = "127.0.0.1"
        }
        return _localIp.value
    }

    fun startServer() {
        if (httpServer != null) return
        scope.launch {
            try {
                val server = HttpServer.create(InetSocketAddress(SERVER_PORT), 0)

                server.createContext("/status", HttpHandler { exchange ->
                    val response = JSONObject().apply {
                        put("status", "online")
                        put("role", CoopRepository.currentRole.value.name)
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    sendJsonResponse(exchange, 200, response)
                })

                server.createContext("/api/post_job", HttpHandler { exchange ->
                    if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                        val body = readRequestBody(exchange)
                        handleIncomingJob(body)
                        val response = JSONObject().apply { put("result", "received") }.toString()
                        sendJsonResponse(exchange, 200, response)
                    } else {
                        sendJsonResponse(exchange, 405, "Method Not Allowed")
                    }
                })

                server.createContext("/api/accept_job", HttpHandler { exchange ->
                    if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                        val body = readRequestBody(exchange)
                        handleIncomingAcceptance(body)
                        val response = JSONObject().apply { put("result", "accepted") }.toString()
                        sendJsonResponse(exchange, 200, response)
                    } else {
                        sendJsonResponse(exchange, 405, "Method Not Allowed")
                    }
                })

                server.executor = null
                server.start()
                httpServer = server
                _isServerRunning.value = true
                _syncLog.value = "LAN Sync Server listening on port $SERVER_PORT"
            } catch (e: Exception) {
                _isServerRunning.value = false
                _syncLog.value = "LAN Server: ${e.message ?: "fallback to local mode"}"
            }
        }
    }

    private fun readRequestBody(exchange: HttpExchange): String {
        val reader = BufferedReader(InputStreamReader(exchange.requestBody))
        val sb = StringBuilder()
        var line: String? = reader.readLine()
        while (line != null) {
            sb.append(line)
            line = reader.readLine()
        }
        return sb.toString()
    }

    private fun sendJsonResponse(exchange: HttpExchange, statusCode: Int, json: String) {
        val bytes = json.toByteArray()
        exchange.responseHeaders.set("Content-Type", "application/json")
        exchange.sendResponseHeaders(statusCode, bytes.size.toLong())
        val os: OutputStream = exchange.responseBody
        os.write(bytes)
        os.close()
    }

    private fun handleIncomingJob(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val skill = json.getString("skill")
            val location = json.getString("location")
            val dateTime = json.optString("dateTime", "Today, 2:00 PM")
            val durationMinutes = json.optInt("durationMinutes", 120)
            val priceInPaise = json.optLong("priceInPaise", 60000L)
            val instructions = json.optString("instructions", "")
            val customerId = json.optString("customerId", "cust_1")
            val jobId = json.optString("id", "job_" + UUID.randomUUID().toString().take(8))
            val postedTime = json.optLong("createdAtTimestamp", System.currentTimeMillis())

            mainHandler.post {
                val newJob = Job(
                    id = jobId,
                    customerId = customerId,
                    workerId = null,
                    cooperativeId = "coop_blr",
                    skill = skill,
                    location = location,
                    dateTime = dateTime,
                    durationMinutes = durationMinutes,
                    priceInPaise = priceInPaise,
                    escrowAmountInPaise = priceInPaise,
                    status = JobStatus.PENDING,
                    title = "$skill Service",
                    description = instructions,
                    createdAtTimestamp = postedTime,
                    preferredTime = dateTime,
                    instructions = instructions
                )
                // Add to repository jobs
                val current = CoopRepository.jobs.value
                if (current.none { it.id == jobId }) {
                    CoopRepository.addExternalJob(newJob)
                }

                _syncLog.value = "Received request from Peer: $skill (₹${priceInPaise / 100})"

                appContext?.let { ctx ->
                    NotificationHelper.notifyNewRequest(
                        ctx,
                        skill,
                        priceInPaise,
                        location
                    )
                }
            }
        } catch (e: Exception) {
            _syncLog.value = "Error parsing peer job: ${e.message}"
        }
    }

    private fun handleIncomingAcceptance(jsonStr: String) {
        try {
            val json = JSONObject(jsonStr)
            val jobId = json.getString("jobId")
            val workerId = json.getString("workerId")
            val workerName = json.optString("workerName", "Assigned Worker")

            mainHandler.post {
                CoopRepository.acceptJob(jobId, workerId)
                _syncLog.value = "Peer Worker accepted: $workerName"

                val job = CoopRepository.jobs.value.firstOrNull { it.id == jobId }
                val service = job?.skill ?: "Service"

                appContext?.let { ctx ->
                    NotificationHelper.notifyWorkerAccepted(ctx, workerName, service)
                }
            }
        } catch (e: Exception) {
            _syncLog.value = "Error parsing peer acceptance: ${e.message}"
        }
    }

    /**
     * Broadcasts newly created job to partner device if IP configured
     */
    fun broadcastNewJob(job: Job) {
        val targetIp = _partnerIp.value
        if (targetIp.isBlank()) return

        scope.launch {
            try {
                val url = URL("http://$targetIp:$SERVER_PORT/api/post_job")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject().apply {
                    put("id", job.id)
                    put("customerId", job.customerId)
                    put("skill", job.skill)
                    put("location", job.location)
                    put("dateTime", job.dateTime)
                    put("durationMinutes", job.durationMinutes)
                    put("priceInPaise", job.priceInPaise)
                    put("instructions", job.instructions)
                    put("createdAtTimestamp", job.createdAtTimestamp)
                }.toString()

                conn.outputStream.use { it.write(payload.toByteArray()) }
                val code = conn.responseCode
                _syncLog.value = "Sent request to $targetIp (HTTP $code)"
            } catch (e: Exception) {
                _syncLog.value = "Peer send failed: ${e.message}"
            }
        }
    }

    /**
     * Broadcasts worker acceptance to partner device if IP configured
     */
    fun broadcastAcceptance(jobId: String, workerId: String, workerName: String) {
        val targetIp = _partnerIp.value
        if (targetIp.isBlank()) return

        scope.launch {
            try {
                val url = URL("http://$targetIp:$SERVER_PORT/api/accept_job")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject().apply {
                    put("jobId", jobId)
                    put("workerId", workerId)
                    put("workerName", workerName)
                }.toString()

                conn.outputStream.use { it.write(payload.toByteArray()) }
                val code = conn.responseCode
                _syncLog.value = "Sent acceptance to $targetIp (HTTP $code)"
            } catch (e: Exception) {
                _syncLog.value = "Peer acceptance send failed: ${e.message}"
            }
        }
    }

    // ==========================================
    // DEMO SIMULATION MODE (Zero-network fallback)
    // ==========================================

    /**
     * Immediately or after 3 seconds, simulates a verified cooperative worker accepting
     * the customer's open request.
     */
    fun simulateWorkerAcceptanceForJob(jobId: String, delayMillis: Long = 1000) {
        mainHandler.postDelayed({
            val job = CoopRepository.jobs.value.firstOrNull { it.id == jobId }
            if (job != null && job.status == JobStatus.PENDING) {
                // Pick a verified worker matching skill or any active worker
                val eligibleWorkers = CoopRepository.workers.value.filter { w ->
                    w.verified && w.skills.any { it.equals(job.skill, ignoreCase = true) }
                }
                val chosenWorker = eligibleWorkers.firstOrNull() ?: CoopRepository.workers.value.first()

                CoopRepository.acceptJob(jobId, chosenWorker.id)
                _syncLog.value = "Demo: ${chosenWorker.name} accepted job #${jobId.takeLast(6)}"

                appContext?.let { ctx ->
                    NotificationHelper.notifyWorkerAccepted(
                        ctx,
                        chosenWorker.name,
                        job.skill
                    )
                }
            }
        }, delayMillis)
    }

    /**
     * Simulates an incoming customer request (e.g. Floor Cleaning ₹600 Today 2 PM Indiranagar)
     * so that the Worker role sees a new Available Request with exact timestamp.
     */
    fun simulateCustomerRequest(
        skill: String = "Cleaning",
        offerRupees: Long = 600,
        location: String = "Indiranagar, Bangalore",
        date: String = "Today",
        time: String = "2:00 PM",
        instructions: String = "Please bring floor mop & eco-friendly detergent"
    ): Job {
        val priceInPaise = offerRupees * 100
        val createdTime = System.currentTimeMillis()
        val jobId = "job_req_" + UUID.randomUUID().toString().take(6)

        val newJob = Job(
            id = jobId,
            customerId = "cust_1",
            workerId = null,
            cooperativeId = "coop_blr",
            skill = skill,
            location = location,
            dateTime = "$date, $time",
            durationMinutes = 120,
            priceInPaise = priceInPaise,
            escrowAmountInPaise = priceInPaise,
            status = JobStatus.PENDING,
            title = "$skill Service Request",
            description = instructions,
            createdAtTimestamp = createdTime,
            preferredTime = time,
            instructions = instructions
        )

        CoopRepository.addExternalJob(newJob)
        _syncLog.value = "Demo: Incoming request posted: $skill (₹$offerRupees)"

        appContext?.let { ctx ->
            NotificationHelper.notifyNewRequest(
                ctx,
                skill,
                priceInPaise,
                location
            )
        }

        return newJob
    }
}
