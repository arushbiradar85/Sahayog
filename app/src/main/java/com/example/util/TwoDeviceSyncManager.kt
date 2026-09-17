package com.example.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import com.example.data.engine.WageEngine
import com.example.data.model.Job
import com.example.data.model.JobStatus
import com.example.data.model.Role
import com.example.data.model.ServiceRequirement
import com.example.data.repository.CoopRepository
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job as CoroutineJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.URL
import java.util.UUID

enum class ConnectionStatus {
    OFFLINE,
    CONNECTING,
    CONNECTED
}

object TwoDeviceSyncManager {

    const val SERVER_PORT = 8989
    private const val PREFS_NAME = "sahayog_sync_prefs"
    private const val KEY_IS_HUB = "is_hub_mode"
    private const val KEY_HUB_IP = "hub_ip"
    private const val STORAGE_FILE_NAME = "sahayog_hub_requests_v2.json"

    private var httpServer: HttpServer? = null
    private var clientPollJob: CoroutineJob? = null
    private var prefs: SharedPreferences? = null

    // Operating mode: true = Dedicated Sahayog Hub, false = Client (Customer or Provider)
    private val _isHubMode = MutableStateFlow(false)
    val isHubMode: StateFlow<Boolean> = _isHubMode.asStateFlow()

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _localIp = MutableStateFlow("127.0.0.1")
    val localIp: StateFlow<String> = _localIp.asStateFlow()

    // Hub IP to connect to when operating as a Client
    private val _hubIp = MutableStateFlow("192.168.43.1")
    val hubIp: StateFlow<String> = _hubIp.asStateFlow()

    // Backward-compatible alias for partnerIp
    val partnerIp: StateFlow<String> = _hubIp.asStateFlow()

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _syncLog = MutableStateFlow("Sahayog Network standby")
    val syncLog: StateFlow<String> = _syncLog.asStateFlow()

    private val _demoSimulationEnabled = MutableStateFlow(true)
    val demoSimulationEnabled: StateFlow<Boolean> = _demoSimulationEnabled.asStateFlow()

    // Authoritative list of requests managed by the Hub
    private val _hubRequests = MutableStateFlow<List<Job>>(emptyList())
    val hubRequests: StateFlow<List<Job>> = _hubRequests.asStateFlow()

    private val _requestsCount = MutableStateFlow(0)
    val requestsCount: StateFlow<Int> = _requestsCount.asStateFlow()

    private fun setHubRequests(list: List<Job>) {
        _hubRequests.value = list
        _requestsCount.value = list.size
    }

    // Set of request IDs that this client has already notified about
    private val notifiedRequestIds = mutableSetOf<String>()

    private var appContext: Context? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedIsHub = prefs?.getBoolean(KEY_IS_HUB, false) ?: false
        val savedHubIp = prefs?.getString(KEY_HUB_IP, "192.168.43.1") ?: "192.168.43.1"

        _isHubMode.value = savedIsHub
        _hubIp.value = savedHubIp

        refreshLocalIp()

        if (savedIsHub) {
            loadHubRequestsFromDisk()
            startServer()
        } else {
            // Start server anyway on port 8989 for peer-to-peer compatibility if port available,
            // and begin client polling to the Hub
            startServer()
            startClientPolling()
        }
    }

    fun setHubMode(isHub: Boolean) {
        _isHubMode.value = isHub
        prefs?.edit()?.putBoolean(KEY_IS_HUB, isHub)?.apply()

        if (isHub) {
            clientPollJob?.cancel()
            _connectionStatus.value = ConnectionStatus.CONNECTED
            loadHubRequestsFromDisk()
            startServer()
            _syncLog.value = "Device operating as Dedicated Sahayog Hub (Port $SERVER_PORT)"
        } else {
            startClientPolling()
            _syncLog.value = "Device operating as Client -> Hub at ${_hubIp.value}"
        }
    }

    fun setHubIp(ip: String) {
        val cleanIp = ip.trim()
        if (cleanIp.isNotBlank()) {
            _hubIp.value = cleanIp
            prefs?.edit()?.putString(KEY_HUB_IP, cleanIp)?.apply()
            if (!_isHubMode.value) {
                // Restart polling with new IP
                startClientPolling()
            }
        }
    }

    fun setPartnerIp(ip: String) {
        setHubIp(ip)
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

    // ==========================================
    // EMBEDDED HTTP SERVER (SAHAYOG HUB)
    // ==========================================

    fun startServer() {
        if (httpServer != null) return
        scope.launch {
            try {
                val server = HttpServer.create(InetSocketAddress(SERVER_PORT), 0)

                // GET /status
                server.createContext("/status", HttpHandler { exchange ->
                    val response = JSONObject().apply {
                        put("status", "online")
                        put("isHub", _isHubMode.value)
                        put("role", CoopRepository.currentRole.value.name)
                        put("activeRequestsCount", _hubRequests.value.size)
                        put("timestamp", System.currentTimeMillis())
                    }.toString()
                    sendJsonResponse(exchange, 200, response)
                })

                // GET /api/requests & POST /api/requests
                server.createContext("/api/requests", HttpHandler { exchange ->
                    when (exchange.requestMethod.uppercase()) {
                        "GET" -> {
                            val list = if (_isHubMode.value) _hubRequests.value else CoopRepository.jobs.value
                            val jsonArray = JSONArray()
                            list.forEach { job ->
                                jsonArray.put(jobToJson(job))
                            }
                            val response = JSONObject().apply {
                                put("status", "ok")
                                put("count", list.size)
                                put("requests", jsonArray)
                            }.toString()
                            sendJsonResponse(exchange, 200, response)
                        }
                        "POST" -> {
                            val body = readRequestBody(exchange)
                            val createdJob = handleIncomingRequest(body)
                            if (createdJob != null) {
                                val response = JSONObject().apply {
                                    put("status", "ok")
                                    put("requestId", createdJob.id)
                                    put("request", jobToJson(createdJob))
                                }.toString()
                                sendJsonResponse(exchange, 200, response)
                            } else {
                                sendJsonResponse(exchange, 400, JSONObject().apply {
                                    put("status", "error")
                                    put("message", "Invalid request payload")
                                }.toString())
                            }
                        }
                        else -> {
                            sendJsonResponse(exchange, 405, "Method Not Allowed")
                        }
                    }
                })

                // POST /api/requests/accept
                server.createContext("/api/requests/accept", HttpHandler { exchange ->
                    if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                        val body = readRequestBody(exchange)
                        val acceptResult = handleIncomingAcceptance(body)
                        if (acceptResult.success) {
                            val response = JSONObject().apply {
                                put("status", "ok")
                                put("accepted", true)
                                put("message", acceptResult.message)
                                acceptResult.job?.let { put("request", jobToJson(it)) }
                            }.toString()
                            sendJsonResponse(exchange, 200, response)
                        } else {
                            val response = JSONObject().apply {
                                put("status", "error")
                                put("message", acceptResult.message)
                            }.toString()
                            sendJsonResponse(exchange, 400, response)
                        }
                    } else {
                        sendJsonResponse(exchange, 405, "Method Not Allowed")
                    }
                })

                // Legacy endpoints for backward compatibility
                server.createContext("/api/post_job", HttpHandler { exchange ->
                    if (exchange.requestMethod.equals("POST", ignoreCase = true)) {
                        val body = readRequestBody(exchange)
                        handleIncomingRequest(body)
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
                _syncLog.value = "Sahayog HTTP Server listening on port $SERVER_PORT"
            } catch (e: Exception) {
                _isServerRunning.value = false
                _syncLog.value = "Sahayog Server: ${e.message ?: "fallback to local mode"}"
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

    // ==========================================
    // REQUEST HANDLING (HUB AUTHORITATIVE)
    // ==========================================

    private fun handleIncomingRequest(jsonStr: String): Job? {
        return try {
            val json = JSONObject(jsonStr)
            val id = json.optString("id", "req_" + UUID.randomUUID().toString().take(8))
            val customerId = json.optString("customerId", "cust_1")
            val location = json.optString("location", "Indiranagar, Bangalore")
            val dateTime = json.optString("dateTime", "Today, 2:00 PM")
            val durationMinutes = json.optInt("durationMinutes", 120)
            val priceInPaise = json.optLong("priceInPaise", 60000L)
            val instructions = json.optString("instructions", "")
            val title = json.optString("title", "Cooperative Service Request")
            val createdAt = json.optLong("createdAtTimestamp", System.currentTimeMillis())

            // Parse requirements list
            val requirements = mutableListOf<ServiceRequirement>()
            if (json.has("requirements")) {
                val reqsArray = json.getJSONArray("requirements")
                for (i in 0 until reqsArray.length()) {
                    val rJson = reqsArray.getJSONObject(i)
                    val skill = rJson.getString("skill")
                    val quantity = rJson.optInt("quantity", 1)
                    val assignedIds = mutableListOf<String>()
                    val assignedNames = mutableListOf<String>()
                    if (rJson.has("assignedProviderIds")) {
                        val idsArr = rJson.getJSONArray("assignedProviderIds")
                        for (j in 0 until idsArr.length()) assignedIds.add(idsArr.getString(j))
                    }
                    if (rJson.has("assignedProviderNames")) {
                        val namesArr = rJson.getJSONArray("assignedProviderNames")
                        for (j in 0 until namesArr.length()) assignedNames.add(namesArr.getString(j))
                    }
                    requirements.add(
                        ServiceRequirement(
                            skill = skill,
                            quantity = quantity,
                            assignedProviderIds = assignedIds,
                            assignedProviderNames = assignedNames
                        )
                    )
                }
            } else if (json.has("skill")) {
                val skill = json.getString("skill")
                requirements.add(ServiceRequirement(skill = skill, quantity = 1))
            }

            val primarySkill = if (requirements.isNotEmpty()) {
                requirements.joinToString(", ") { "${it.quantity}x ${it.skill}" }
            } else {
                json.optString("skill", "Service")
            }

            val job = Job(
                id = id,
                customerId = customerId,
                workerId = null,
                cooperativeId = "coop_blr",
                skill = primarySkill,
                location = location,
                dateTime = dateTime,
                durationMinutes = durationMinutes,
                priceInPaise = priceInPaise,
                escrowAmountInPaise = priceInPaise,
                status = JobStatus.PENDING,
                title = title,
                description = instructions,
                createdAtTimestamp = createdAt,
                preferredTime = dateTime,
                instructions = instructions,
                requirements = requirements
            )

            mainHandler.post {
                // Add to Hub's authoritative list
                setHubRequests(listOf(job) + _hubRequests.value.filter { it.id != job.id })
                saveHubRequestsToDisk()

                // Add to local repository
                CoopRepository.addExternalJob(job)
                _syncLog.value = "Hub: Stored request ${job.id} ($primarySkill)"
            }

            job
        } catch (e: Exception) {
            _syncLog.value = "Error parsing incoming request: ${e.message}"
            null
        }
    }

    data class AcceptResult(val success: Boolean, val message: String, val job: Job? = null)

    private fun handleIncomingAcceptance(jsonStr: String): AcceptResult {
        return try {
            val json = JSONObject(jsonStr)
            val requestId = json.optString("requestId", json.optString("jobId", ""))
            val skill = json.optString("skill", "")
            val providerId = json.optString("providerId", json.optString("workerId", "user_worker"))
            val providerName = json.optString("providerName", json.optString("workerName", "Assigned Worker"))

            if (requestId.isBlank()) {
                return AcceptResult(false, "Missing requestId")
            }

            var updatedJob: Job? = null
            var acceptSuccess = false
            var failReason = "Request or requirement not found"

            // Update in Hub Authoritative Requests
            val currentList = if (_isHubMode.value) _hubRequests.value else CoopRepository.jobs.value
            val targetJob = currentList.firstOrNull { it.id == requestId }

            if (targetJob != null) {
                val updatedRequirements = targetJob.requirements.map { req ->
                    val skillMatches = skill.isBlank() || req.skill.equals(skill, ignoreCase = true)
                    val alreadyAssigned = req.assignedProviderIds.contains(providerId)
                    val isFull = req.assignedProviderIds.size >= req.quantity

                    if (skillMatches) {
                        if (alreadyAssigned) {
                            failReason = "Already accepted by this provider"
                            req
                        } else if (isFull) {
                            failReason = "Requirement already fully assigned (${req.quantity}/${req.quantity})"
                            req
                        } else {
                            acceptSuccess = true
                            req.copy(
                                assignedProviderIds = req.assignedProviderIds + providerId,
                                assignedProviderNames = req.assignedProviderNames + providerName
                            )
                        }
                    } else {
                        req
                    }
                }

                if (acceptSuccess) {
                    val allRequirementsFilled = updatedRequirements.all { it.assignedProviderIds.size >= it.quantity }
                    updatedJob = targetJob.copy(
                        workerId = providerId,
                        requirements = updatedRequirements,
                        status = if (allRequirementsFilled) JobStatus.ACCEPTED else JobStatus.PENDING
                    )

                    mainHandler.post {
                        if (_isHubMode.value) {
                            setHubRequests(_hubRequests.value.map { if (it.id == requestId) updatedJob!! else it })
                            saveHubRequestsToDisk()
                        }
                        CoopRepository.acceptJobRequirement(requestId, skill, providerId, providerName)
                        _syncLog.value = "Accepted: $providerName for $skill"

                        appContext?.let { ctx ->
                            NotificationHelper.notifyWorkerAccepted(ctx, providerName, skill.ifEmpty { targetJob.skill })
                        }
                    }
                    AcceptResult(true, "Successfully assigned $providerName", updatedJob)
                } else if (targetJob.requirements.isEmpty() && targetJob.status == JobStatus.PENDING) {
                    // Legacy single-job acceptance
                    updatedJob = targetJob.copy(workerId = providerId, status = JobStatus.ACCEPTED)
                    mainHandler.post {
                        if (_isHubMode.value) {
                            setHubRequests(_hubRequests.value.map { if (it.id == requestId) updatedJob!! else it })
                            saveHubRequestsToDisk()
                        }
                        CoopRepository.acceptJob(requestId, providerId)
                        _syncLog.value = "Accepted: $providerName for ${targetJob.skill}"
                    }
                    AcceptResult(true, "Accepted legacy job", updatedJob)
                } else {
                    AcceptResult(false, failReason)
                }
            } else {
                AcceptResult(false, "Job #$requestId not found on Hub")
            }
        } catch (e: Exception) {
            AcceptResult(false, "Error parsing acceptance: ${e.message}")
        }
    }

    // ==========================================
    // CLIENT POLLING (CUSTOMER & PROVIDER DEVICES)
    // ==========================================

    fun startClientPolling() {
        clientPollJob?.cancel()
        clientPollJob = scope.launch {
            while (isActive) {
                if (!_isHubMode.value) {
                    pollHubRequests()
                }
                delay(1500)
            }
        }
    }

    private fun pollHubRequests() {
        val targetIp = _hubIp.value.trim()
        if (targetIp.isBlank()) {
            _connectionStatus.value = ConnectionStatus.OFFLINE
            return
        }

        try {
            val url = URL("http://$targetIp:$SERVER_PORT/api/requests")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 1200
            conn.readTimeout = 1200

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String? = reader.readLine()
                while (line != null) {
                    sb.append(line)
                    line = reader.readLine()
                }
                reader.close()

                val json = JSONObject(sb.toString())
                val reqsArray = json.getJSONArray("requests")
                val remoteJobs = mutableListOf<Job>()

                for (i in 0 until reqsArray.length()) {
                    val jobJson = reqsArray.getJSONObject(i)
                    jsonToJob(jobJson)?.let { remoteJobs.add(it) }
                }

                mainHandler.post {
                    _connectionStatus.value = ConnectionStatus.CONNECTED

                    // Check for new matching requests to notify provider
                    val activeWorker = CoopRepository.getActiveWorker()
                    val isWorker = CoopRepository.currentRole.value == Role.WORKER

                    remoteJobs.forEach { job ->
                        if (!notifiedRequestIds.contains(job.id)) {
                            notifiedRequestIds.add(job.id)

                            if (isWorker) {
                                // Match if any requirement skill matches any worker skill
                                val matchingReq = job.requirements.firstOrNull { req ->
                                    activeWorker.skills.any { ws -> ws.equals(req.skill, ignoreCase = true) } && !req.isFilled
                                }
                                if (matchingReq != null) {
                                    appContext?.let { ctx ->
                                        NotificationHelper.notifyNewRequest(
                                            ctx,
                                            matchingReq.skill,
                                            job.priceInPaise,
                                            job.location
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Synchronize with local repository
                    // Preserve any local non-hub jobs that might exist, while updating from Hub
                    val hubJobIds = remoteJobs.map { it.id }.toSet()
                    val existingNonHub = CoopRepository.jobs.value.filter { !hubJobIds.contains(it.id) }
                    CoopRepository.setAuthoritativeJobs(remoteJobs + existingNonHub)
                    _syncLog.value = "Connected to Hub ($targetIp) • ${remoteJobs.size} requests"
                }
            } else {
                mainHandler.post {
                    _connectionStatus.value = ConnectionStatus.OFFLINE
                }
            }
        } catch (e: Exception) {
            mainHandler.post {
                _connectionStatus.value = ConnectionStatus.OFFLINE
            }
        }
    }

    // ==========================================
    // CLIENT NETWORK ACTIONS
    // ==========================================

    /**
     * Customer Posts a Request:
     * If this device is Hub, stores directly.
     * Otherwise, POSTs to http://<hubIp>:8989/api/requests.
     * In case of network error, falls back to local save so customer is never blocked.
     */
    fun postRequestToHub(job: Job, onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (_isHubMode.value) {
            setHubRequests(listOf(job) + _hubRequests.value.filter { it.id != job.id })
            saveHubRequestsToDisk()
            CoopRepository.addExternalJob(job)
            onComplete?.invoke(true, null)
            return
        }

        scope.launch {
            val targetIp = _hubIp.value.trim()
            try {
                val url = URL("http://$targetIp:$SERVER_PORT/api/requests")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = jobToJson(job).toString()
                conn.outputStream.use { it.write(payload.toByteArray()) }

                val code = conn.responseCode
                if (code == 200) {
                    mainHandler.post {
                        CoopRepository.addExternalJob(job)
                        _syncLog.value = "Sent request to Hub ($targetIp): ${job.skill}"
                        onComplete?.invoke(true, null)
                    }
                } else {
                    mainHandler.post {
                        CoopRepository.addExternalJob(job)
                        _syncLog.value = "Hub returned HTTP $code; saved locally"
                        onComplete?.invoke(false, "Hub returned HTTP $code")
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    CoopRepository.addExternalJob(job)
                    _syncLog.value = "Hub offline (${e.message}); stored locally"
                    onComplete?.invoke(false, e.message)
                }
            }
        }
    }

    /**
     * Provider Accepts a Requirement:
     * If this device is Hub, updates directly.
     * Otherwise, POSTs to http://<hubIp>:8989/api/requests/accept.
     */
    fun acceptRequirementOnHub(
        jobId: String,
        skill: String,
        providerId: String,
        providerName: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        if (_isHubMode.value) {
            val updated = CoopRepository.acceptJobRequirement(jobId, skill, providerId, providerName)
            if (updated) {
                val job = CoopRepository.jobs.value.firstOrNull { it.id == jobId }
                if (job != null) {
                    setHubRequests(_hubRequests.value.map { if (it.id == jobId) job else it })
                    saveHubRequestsToDisk()
                }
            }
            onComplete?.invoke(updated, if (updated) null else "Could not accept requirement")
            return
        }

        scope.launch {
            val targetIp = _hubIp.value.trim()
            try {
                val url = URL("http://$targetIp:$SERVER_PORT/api/requests/accept")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json")

                val payload = JSONObject().apply {
                    put("requestId", jobId)
                    put("skill", skill)
                    put("providerId", providerId)
                    put("providerName", providerName)
                }.toString()

                conn.outputStream.use { it.write(payload.toByteArray()) }

                val code = conn.responseCode
                if (code == 200) {
                    mainHandler.post {
                        CoopRepository.acceptJobRequirement(jobId, skill, providerId, providerName)
                        _syncLog.value = "Hub accepted assignment for $providerName"
                        onComplete?.invoke(true, null)
                    }
                } else {
                    val reader = BufferedReader(InputStreamReader(conn.errorStream ?: conn.inputStream))
                    val errBody = reader.readText()
                    reader.close()
                    val errMsg = try { JSONObject(errBody).optString("message", "Error $code") } catch (e: Exception) { "Error $code" }
                    mainHandler.post {
                        _syncLog.value = "Accept failed: $errMsg"
                        onComplete?.invoke(false, errMsg)
                    }
                }
            } catch (e: Exception) {
                mainHandler.post {
                    // Fallback to local
                    CoopRepository.acceptJobRequirement(jobId, skill, providerId, providerName)
                    _syncLog.value = "Hub unreachable; accepted locally (${e.message})"
                    onComplete?.invoke(true, null)
                }
            }
        }
    }

    // Backward-compatible broadcast helper
    fun broadcastNewJob(job: Job) {
        postRequestToHub(job)
    }

    fun postAcceptanceToHub(
        job: Job,
        providerId: String,
        skill: String,
        providerName: String,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        acceptRequirementOnHub(job.id, skill, providerId, providerName, onComplete)
    }

    // Backward-compatible broadcast helper
    fun broadcastAcceptance(jobId: String, workerId: String, workerName: String) {
        val job = CoopRepository.jobs.value.firstOrNull { it.id == jobId }
        val skill = job?.skill ?: ""
        acceptRequirementOnHub(jobId, skill, workerId, workerName)
    }

    // ==========================================
    // LOCAL DISK PERSISTENCE (FOR HUB MODE)
    // ==========================================

    fun saveHubRequestsToDisk() {
        val ctx = appContext ?: return
        scope.launch {
            try {
                val file = File(ctx.filesDir, STORAGE_FILE_NAME)
                val jsonArr = JSONArray()
                _hubRequests.value.forEach { job ->
                    jsonArr.put(jobToJson(job))
                }
                file.writeText(jsonArr.toString())
            } catch (e: Exception) {
                // Logging silently
            }
        }
    }

    fun loadHubRequestsFromDisk() {
        val ctx = appContext ?: return
        scope.launch {
            try {
                val file = File(ctx.filesDir, STORAGE_FILE_NAME)
                if (file.exists()) {
                    val content = file.readText()
                    val jsonArr = JSONArray(content)
                    val loaded = mutableListOf<Job>()
                    for (i in 0 until jsonArr.length()) {
                        jsonToJob(jsonArr.getJSONObject(i))?.let { loaded.add(it) }
                    }
                    mainHandler.post {
                        setHubRequests(loaded)
                        CoopRepository.setAuthoritativeJobs(loaded)
                        _syncLog.value = "Restored ${loaded.size} authoritative requests from disk"
                    }
                }
            } catch (e: Exception) {
                // Logging silently
            }
        }
    }

    fun clearHubRequests() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, STORAGE_FILE_NAME)
            if (file.exists()) file.delete()
            setHubRequests(emptyList())
            CoopRepository.resetToSeedData()
            _syncLog.value = "Hub requests reset"
        } catch (e: Exception) {}
    }

    // ==========================================
    // JSON CONVERTERS
    // ==========================================

    fun jobToJson(job: Job): JSONObject {
        return JSONObject().apply {
            put("id", job.id)
            put("customerId", job.customerId)
            put("workerId", job.workerId ?: "")
            put("cooperativeId", job.cooperativeId)
            put("skill", job.skill)
            put("location", job.location)
            put("dateTime", job.dateTime)
            put("durationMinutes", job.durationMinutes)
            put("priceInPaise", job.priceInPaise)
            put("escrowAmountInPaise", job.escrowAmountInPaise)
            put("status", job.status.name)
            put("title", job.title)
            put("description", job.description)
            put("createdAtTimestamp", job.createdAtTimestamp)
            put("preferredTime", job.preferredTime)
            put("instructions", job.instructions)

            val reqsArr = JSONArray()
            job.requirements.forEach { req ->
                reqsArr.put(JSONObject().apply {
                    put("skill", req.skill)
                    put("quantity", req.quantity)
                    put("assignedProviderIds", JSONArray(req.assignedProviderIds))
                    put("assignedProviderNames", JSONArray(req.assignedProviderNames))
                })
            }
            put("requirements", reqsArr)
        }
    }

    fun jsonToJob(json: JSONObject): Job? {
        return try {
            val id = json.getString("id")
            val customerId = json.optString("customerId", "cust_1")
            val rawWorkerId = json.optString("workerId", "")
            val workerId = if (rawWorkerId.isNotBlank()) rawWorkerId else null
            val cooperativeId = json.optString("cooperativeId", "coop_blr")
            val skill = json.optString("skill", "Service")
            val location = json.optString("location", "Indiranagar, Bangalore")
            val dateTime = json.optString("dateTime", "Today, 2:00 PM")
            val durationMinutes = json.optInt("durationMinutes", 120)
            val priceInPaise = json.optLong("priceInPaise", 60000L)
            val escrowAmountInPaise = json.optLong("escrowAmountInPaise", priceInPaise)
            val statusStr = json.optString("status", JobStatus.PENDING.name)
            val status = try { JobStatus.valueOf(statusStr) } catch (e: Exception) { JobStatus.PENDING }
            val title = json.optString("title", "$skill Service")
            val description = json.optString("description", "")
            val createdAtTimestamp = json.optLong("createdAtTimestamp", System.currentTimeMillis())
            val preferredTime = json.optString("preferredTime", dateTime)
            val instructions = json.optString("instructions", "")

            val requirements = mutableListOf<ServiceRequirement>()
            if (json.has("requirements")) {
                val arr = json.getJSONArray("requirements")
                for (i in 0 until arr.length()) {
                    val r = arr.getJSONObject(i)
                    val rSkill = r.getString("skill")
                    val qty = r.optInt("quantity", 1)
                    val ids = mutableListOf<String>()
                    val names = mutableListOf<String>()
                    if (r.has("assignedProviderIds")) {
                        val idsArr = r.getJSONArray("assignedProviderIds")
                        for (j in 0 until idsArr.length()) ids.add(idsArr.getString(j))
                    }
                    if (r.has("assignedProviderNames")) {
                        val namesArr = r.getJSONArray("assignedProviderNames")
                        for (j in 0 until namesArr.length()) names.add(namesArr.getString(j))
                    }
                    requirements.add(
                        ServiceRequirement(
                            skill = rSkill,
                            quantity = qty,
                            assignedProviderIds = ids,
                            assignedProviderNames = names
                        )
                    )
                }
            } else {
                requirements.add(ServiceRequirement(skill = skill, quantity = 1))
            }

            Job(
                id = id,
                customerId = customerId,
                workerId = workerId,
                cooperativeId = cooperativeId,
                skill = skill,
                location = location,
                dateTime = dateTime,
                durationMinutes = durationMinutes,
                priceInPaise = priceInPaise,
                escrowAmountInPaise = escrowAmountInPaise,
                status = status,
                title = title,
                description = description,
                createdAtTimestamp = createdAtTimestamp,
                preferredTime = preferredTime,
                instructions = instructions,
                requirements = requirements
            )
        } catch (e: Exception) {
            null
        }
    }

    // ==========================================
    // DEMO SIMULATION MODE (FALLBACK)
    // ==========================================

    fun simulateWorkerAcceptanceForJob(jobId: String, delayMillis: Long = 1000) {
        mainHandler.postDelayed({
            val job = CoopRepository.jobs.value.firstOrNull { it.id == jobId }
            if (job != null && (job.status == JobStatus.PENDING || job.requirements.any { it.assignedProviderIds.size < it.quantity })) {
                val eligibleWorkers = CoopRepository.workers.value.filter { w ->
                    w.verified && (job.requirements.isEmpty() || job.requirements.any { req ->
                        w.skills.any { it.equals(req.skill, ignoreCase = true) }
                    })
                }
                val chosenWorker = eligibleWorkers.firstOrNull() ?: CoopRepository.workers.value.first()
                val targetReq = job.requirements.firstOrNull { req ->
                    chosenWorker.skills.any { it.equals(req.skill, ignoreCase = true) } && req.assignedProviderIds.size < req.quantity
                } ?: job.requirements.firstOrNull()

                val skillToAccept = targetReq?.skill ?: job.skill
                acceptRequirementOnHub(jobId, skillToAccept, chosenWorker.id, chosenWorker.name)
            }
        }, delayMillis)
    }

    fun simulateCustomerRequest(
        skill: String = "Cleaning",
        offerRupees: Long = 600,
        location: String = "Indiranagar, Bangalore",
        date: String = "Today",
        time: String = "2:00 PM",
        instructions: String = "Please bring necessary tools & equipment"
    ): Job {
        val priceInPaise = offerRupees * 100
        val createdTime = System.currentTimeMillis()
        val jobId = "req_" + UUID.randomUUID().toString().take(6)

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
            instructions = instructions,
            requirements = listOf(ServiceRequirement(skill = skill, quantity = 1))
        )

        postRequestToHub(newJob)
        return newJob
    }
}
