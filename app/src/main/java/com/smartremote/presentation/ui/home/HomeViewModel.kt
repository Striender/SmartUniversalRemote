package com.smartremote.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.data.repository.EnergyRepository
import com.smartremote.data.repository.RoomRepository
import com.smartremote.data.repository.SceneRepository
import com.smartremote.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val userName: String = "",
    val favoriteDevices: List<Device> = emptyList(),
    val roomsWithDevices: List<Pair<Room, List<Device>>> = emptyList(),
    val quickScenes: List<Scene> = emptyList(),
    val aiSuggestions: List<String> = emptyList(),
    val onlineDeviceCount: Int = 0,
    val totalDeviceCount: Int = 0,
    val dailyKWh: Double = 0.0,
    val monthlyCost: Double = 0.0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val roomRepo: RoomRepository,
    private val sceneRepo: SceneRepository,
    private val energyRepo: EnergyRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserName()
        observeData()
    }

    private fun loadUserName() {
        val name = auth.currentUser?.displayName
            ?: auth.currentUser?.email?.substringBefore("@")
            ?: "Guest"
        _uiState.update { it.copy(userName = name) }
    }

    private fun observeData() {
        viewModelScope.launch {
            // Combine devices and rooms
            combine(
                deviceRepo.getAllDevices(),
                roomRepo.getAllRooms(),
                deviceRepo.getFavoriteDevices()
            ) { allDevices, rooms, favorites ->
                val roomsWithDevices = rooms.map { room ->
                    room to allDevices.filter { it.roomId == room.id }
                }.filter { it.second.isNotEmpty() }

                Triple(allDevices, roomsWithDevices, favorites)
            }.collect { (allDevices, roomsWithDevices, favorites) ->
                _uiState.update {
                    it.copy(
                        totalDeviceCount = allDevices.size,
                        onlineDeviceCount = allDevices.count { d -> d.status == DeviceStatus.ONLINE },
                        roomsWithDevices = roomsWithDevices,
                        favoriteDevices = favorites
                    )
                }
            }
        }

        viewModelScope.launch {
            sceneRepo.getAllScenes().collect { scenes ->
                _uiState.update { it.copy(quickScenes = scenes.take(6)) }
            }
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    aiSuggestions = listOf(
                        "Turn on Movie Mode",
                        "Set AC to 24°C — it's hot outside",
                        "Dim lights for evening"
                    ),
                    dailyKWh = 4.2,
                    monthlyCost = 1260.0
                )
            }
        }
    }

    fun toggleDevicePower(device: Device) {
        viewModelScope.launch {
            val command = if (device.status == DeviceStatus.ONLINE) "POWER_OFF" else "POWER_ON"
            deviceRepo.sendCommand(device, command)
        }
    }

    fun activateScene(scene: Scene) {
        viewModelScope.launch {
            scene.actions.forEach { action ->
                val device = deviceRepo.getDeviceById(action.deviceId) ?: return@forEach
                if (action.delayMs > 0) kotlinx.coroutines.delay(action.delayMs)
                deviceRepo.sendCommand(device, action.command, action.value)
            }
            sceneRepo.activateScene(scene.id)
        }
    }

    fun executeAiSuggestion(suggestion: String) {
        viewModelScope.launch {
            // Parse and execute AI suggestion
        }
    }
}
