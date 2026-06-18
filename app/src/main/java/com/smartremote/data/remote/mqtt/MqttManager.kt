package com.smartremote.data.remote.mqtt

import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

data class MqttMessage(
    val topic: String,
    val payload: String,
    val qos: Int = 0
)

sealed class MqttConnectionState {
    object Connected : MqttConnectionState()
    object Disconnected : MqttConnectionState()
    data class Error(val exception: Throwable) : MqttConnectionState()
}

@Singleton
class MqttManager @Inject constructor(
    private val context: Context
) {
    private var mqttClient: MqttAsyncClient? = null
    private val brokerUrl = "tcp://mqtt.smartremote.io:1883"
    private val clientId = "SmartRemote_${System.currentTimeMillis()}"

    val connectionState: Flow<MqttConnectionState> = callbackFlow {
        val client = MqttAsyncClient(brokerUrl, clientId, MemoryPersistence())
        mqttClient = client

        client.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                trySend(MqttConnectionState.Error(cause ?: Exception("Connection lost")))
            }

            override fun messageArrived(topic: String, message: org.eclipse.paho.client.mqttv3.MqttMessage) {
                // handled in subscribe()
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                Timber.d("MQTT delivery complete")
            }
        })

        val options = MqttConnectOptions().apply {
            isCleanSession = true
            connectionTimeout = 30
            keepAliveInterval = 60
            isAutomaticReconnect = true
        }

        try {
            client.connect(options, null, object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken?) {
                    trySend(MqttConnectionState.Connected)
                    Timber.d("MQTT connected to $brokerUrl")
                }

                override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                    trySend(MqttConnectionState.Error(exception ?: Exception("Connection failed")))
                    Timber.e(exception, "MQTT connection failed")
                }
            })
        } catch (e: MqttException) {
            trySend(MqttConnectionState.Error(e))
        }

        awaitClose { disconnect() }
    }

    fun subscribe(topic: String): Flow<MqttMessage> = callbackFlow {
        mqttClient?.subscribe(topic, 0) { receivedTopic, message ->
            trySend(
                MqttMessage(
                    topic = receivedTopic,
                    payload = String(message.payload),
                    qos = message.qos
                )
            )
        }
        awaitClose {
            try { mqttClient?.unsubscribe(topic) } catch (e: Exception) { Timber.e(e) }
        }
    }

    fun publish(topic: String, payload: String, qos: Int = 0) {
        try {
            val msg = org.eclipse.paho.client.mqttv3.MqttMessage(payload.toByteArray()).apply {
                this.qos = qos
                isRetained = false
            }
            mqttClient?.publish(topic, msg)
            Timber.d("MQTT published to $topic: $payload")
        } catch (e: MqttException) {
            Timber.e(e, "MQTT publish failed")
        }
    }

    fun disconnect() {
        try {
            mqttClient?.disconnect()
            mqttClient?.close()
            mqttClient = null
        } catch (e: MqttException) {
            Timber.e(e, "MQTT disconnect error")
        }
    }

    fun isConnected() = mqttClient?.isConnected == true
}
