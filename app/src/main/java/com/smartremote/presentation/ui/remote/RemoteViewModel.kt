package com.smartremote.presentation.ui.remote

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartremote.data.local.dao.CommandHistoryDao
import com.smartremote.data.local.entities.CommandHistoryEntity
import com.smartremote.data.repository.DeviceRepository
import com.smartremote.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RemoteUiState(
    val device: Device? = null,
    val tvState: TvState? = null,
    val acState: AcState? = null,
    val fanState: FanState? = null,
    val lightState: LightState? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class RemoteViewModel @Inject constructor(
    private val deviceRepo: DeviceRepository,
    private val commandHistoryDao: CommandHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(RemoteUiState())
    val uiState: StateFlow<RemoteUiState> = _uiState.asStateFlow()

    fun loadDevice(deviceId: String) {
        viewModelScope.launch {
            val device = deviceRepo.getDeviceById(deviceId)
            _uiState.update {
                it.copy(
                    device = device,
                    tvState    = if (device?.type in listOf(DeviceType.TV, DeviceType.SET_TOP_BOX, DeviceType.PROJECTOR))
                        TvState(deviceId) else null,
                    acState    = if (device?.type == DeviceType.AIR_CONDITIONER) AcState(deviceId) else null,
                    fanState   = if (device?.type == DeviceType.FAN) FanState(deviceId) else null,
                    lightState = if (device?.type == DeviceType.SMART_LIGHT) LightState(deviceId) else null
                )
            }
        }
    }

    fun sendCommand(command: String, value: String?) {
        viewModelScope.launch {
            val device = _uiState.value.device ?: return@launch
            deviceRepo.sendCommand(device, command, value)
            commandHistoryDao.insertCommand(
                CommandHistoryEntity(deviceId = device.id, command = command, value = value)
            )
            updateLocalState(command, value)
        }
    }

    private fun updateLocalState(command: String, value: String?) {
        _uiState.update { state ->
            when {
                state.tvState != null -> state.copy(tvState = applyTvCommand(state.tvState, command, value))
                state.acState != null -> state.copy(acState = applyAcCommand(state.acState, command, value))
                state.fanState != null -> state.copy(fanState = applyFanCommand(state.fanState, command, value))
                state.lightState != null -> state.copy(lightState = applyLightCommand(state.lightState, command, value))
                else -> state
            }
        }
    }

    private fun applyTvCommand(s: TvState, cmd: String, v: String?) = when (cmd) {
        "POWER_TOGGLE" -> s.copy(isPowered = !s.isPowered)
        "MUTE_TOGGLE"  -> s.copy(isMuted = !s.isMuted)
        "VOLUME_UP"    -> s.copy(volume = (s.volume + 1).coerceAtMost(100))
        "VOLUME_DOWN"  -> s.copy(volume = (s.volume - 1).coerceAtLeast(0))
        "CHANNEL_UP"   -> s.copy(channel = s.channel + 1)
        "CHANNEL_DOWN" -> s.copy(channel = (s.channel - 1).coerceAtLeast(1))
        else -> s
    }

    private fun applyAcCommand(s: AcState, cmd: String, v: String?) = when (cmd) {
        "POWER_TOGGLE"  -> s.copy(isPowered = !s.isPowered)
        "TEMP_UP"       -> s.copy(temperature = (s.temperature + 1).coerceAtMost(30))
        "TEMP_DOWN"     -> s.copy(temperature = (s.temperature - 1).coerceAtLeast(16))
        "SET_MODE"      -> s.copy(mode = AcMode.valueOf(v ?: AcMode.COOL.name))
        "SET_FAN_SPEED" -> s.copy(fanSpeed = FanSpeed.valueOf(v ?: FanSpeed.AUTO.name))
        "TOGGLE_SWING"  -> s.copy(swingEnabled = !s.swingEnabled)
        else -> s
    }

    private fun applyFanCommand(s: FanState, cmd: String, v: String?) = when (cmd) {
        "POWER_TOGGLE"    -> s.copy(isPowered = !s.isPowered)
        "SET_SPEED"       -> s.copy(speed = v?.toIntOrNull() ?: s.speed)
        "TOGGLE_OSCILLATE"-> s.copy(oscillating = !s.oscillating)
        else -> s
    }

    private fun applyLightCommand(s: LightState, cmd: String, v: String?) = when (cmd) {
        "POWER_TOGGLE"   -> s.copy(isPowered = !s.isPowered)
        "SET_BRIGHTNESS" -> s.copy(brightness = v?.toIntOrNull() ?: s.brightness)
        "SET_COLOR_TEMP" -> s.copy(colorTemperature = v?.toIntOrNull() ?: s.colorTemperature)
        else -> s
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val device = _uiState.value.device ?: return@launch
            deviceRepo.toggleFavorite(device.id, !device.isFavorite)
            _uiState.update { it.copy(device = device.copy(isFavorite = !device.isFavorite)) }
        }
    }
}
