package com.smartremote.data.remote.api

import com.smartremote.domain.model.Device
import com.smartremote.domain.model.EnergyReport
import com.smartremote.domain.model.ReportPeriod
import retrofit2.Response
import retrofit2.http.*

// ─── DTOs ─────────────────────────────────────────────────────────────────────

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val error: String? = null
)

data class DeviceDiscoveryResponse(
    val devices: List<DiscoveredDevice>
)

data class DiscoveredDevice(
    val name: String,
    val ipAddress: String,
    val macAddress: String,
    val type: String,
    val brand: String?,
    val model: String?
)

data class CommandRequest(
    val deviceId: String,
    val command: String,
    val value: String? = null,
    val parameters: Map<String, String> = emptyMap()
)

data class CommandResponse(
    val success: Boolean,
    val message: String,
    val newState: Map<String, String>? = null
)

data class SyncRequest(
    val devices: List<Device>,
    val userId: String,
    val timestamp: Long
)

data class BackupResponse(
    val backupId: String,
    val timestamp: Long,
    val deviceCount: Int
)

data class EnergyReportRequest(
    val deviceIds: List<String>,
    val period: ReportPeriod,
    val startDate: Long,
    val endDate: Long
)

data class AiChatRequest(
    val message: String,
    val deviceContext: List<String>,
    val conversationHistory: List<Map<String, String>>
)

data class AiChatResponse(
    val reply: String,
    val suggestedActions: List<String>,
    val parsedCommand: ParsedCommand?
)

data class ParsedCommand(
    val intent: String,
    val deviceTarget: String?,
    val value: String?
)

data class IrCodeSearchResponse(
    val codes: List<IrCodeResult>
)

data class IrCodeResult(
    val brand: String,
    val model: String,
    val deviceType: String,
    val irCode: String,
    val confidence: Float
)

// ─── API Interface ────────────────────────────────────────────────────────────

interface SmartRemoteApiService {

    // Device Discovery
    @GET("devices/discover")
    suspend fun discoverDevices(
        @Query("network") network: String
    ): Response<ApiResponse<DeviceDiscoveryResponse>>

    // Device Control
    @POST("devices/{deviceId}/command")
    suspend fun sendCommand(
        @Path("deviceId") deviceId: String,
        @Body request: CommandRequest
    ): Response<ApiResponse<CommandResponse>>

    @GET("devices/{deviceId}/state")
    suspend fun getDeviceState(
        @Path("deviceId") deviceId: String
    ): Response<ApiResponse<Map<String, String>>>

    // IR Codes
    @GET("ir/search")
    suspend fun searchIrCodes(
        @Query("brand") brand: String,
        @Query("model") model: String?,
        @Query("type") deviceType: String
    ): Response<ApiResponse<IrCodeSearchResponse>>

    @GET("ir/brands")
    suspend fun getIrBrands(
        @Query("deviceType") deviceType: String
    ): Response<ApiResponse<List<String>>>

    // Cloud Sync
    @POST("sync/backup")
    suspend fun backupDevices(@Body request: SyncRequest): Response<ApiResponse<BackupResponse>>

    @GET("sync/restore/{userId}")
    suspend fun restoreDevices(
        @Path("userId") userId: String
    ): Response<ApiResponse<List<Device>>>

    // Energy
    @POST("energy/report")
    suspend fun getEnergyReport(
        @Body request: EnergyReportRequest
    ): Response<ApiResponse<EnergyReport>>

    // AI Assistant
    @POST("ai/chat")
    suspend fun chatWithAi(
        @Body request: AiChatRequest
    ): Response<ApiResponse<AiChatResponse>>

    // User
    @POST("auth/register")
    suspend fun registerUser(@Body body: Map<String, String>): Response<ApiResponse<Map<String, String>>>

    @GET("user/profile")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<ApiResponse<Map<String, String>>>
}

// ─── Anthropic API ────────────────────────────────────────────────────────────

interface AnthropicApiService {

    @POST("messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body body: AnthropicRequest
    ): Response<AnthropicResponse>
}

data class AnthropicRequest(
    val model: String = "claude-sonnet-4-6",
    val max_tokens: Int = 1024,
    val system: String,
    val messages: List<AnthropicMessage>
)

data class AnthropicMessage(
    val role: String,
    val content: String
)

data class AnthropicResponse(
    val id: String,
    val content: List<AnthropicContent>,
    val model: String,
    val stop_reason: String?,
    val usage: AnthropicUsage
)

data class AnthropicContent(
    val type: String,
    val text: String
)

data class AnthropicUsage(
    val input_tokens: Int,
    val output_tokens: Int
)
