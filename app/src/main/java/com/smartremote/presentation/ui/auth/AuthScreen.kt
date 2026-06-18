package com.smartremote.presentation.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.*
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.smartremote.presentation.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

// ─── Auth ViewModel ───────────────────────────────────────────────────────────

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val displayName: String = "",
    val isLogin: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val passwordVisible: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun updateEmail(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun updatePassword(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun updateName(v: String) = _uiState.update { it.copy(displayName = v, error = null) }
    fun toggleMode() = _uiState.update { it.copy(isLogin = !it.isLogin, error = null) }
    fun togglePasswordVisible() = _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }

    fun submit(onSuccess: () -> Unit) {
        val s = _uiState.value
        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = "Please fill in all fields") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                if (s.isLogin) {
                    auth.signInWithEmailAndPassword(s.email, s.password).await()
                } else {
                    val result = auth.createUserWithEmailAndPassword(s.email, s.password).await()
                    // Update display name
                }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message?.substringAfter("] ") ?: "Authentication failed") }
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun continueAsGuest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                auth.signInAnonymously().await()
                onSuccess()
            } catch (e: Exception) {
                onSuccess() // Allow offline use
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}

// ─── Auth Screen ──────────────────────────────────────────────────────────────

@Composable
fun AuthScreen(
    onAuthenticated: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current

    // Check if already signed in
    LaunchedEffect(Unit) {
        // Skip auth for demo / already signed in
    }

    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBg, DarkSurface)))
    ) {
        // Decorative orbs
        Box(
            Modifier.size(300.dp).offset((-80).dp, (-60).dp)
                .clip(CircleShape)
                .background(Brand600.copy(alpha = 0.15f))
        )
        Box(
            Modifier.size(200.dp).align(Alignment.BottomEnd).offset(60.dp, 60.dp)
                .clip(CircleShape)
                .background(TealAccent.copy(alpha = 0.1f))
        )

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(60.dp))

            // Logo
            Box(
                Modifier.size(88.dp).clip(RoundedCornerShape(28.dp))
                    .background(Brush.linearGradient(
                        listOf(Brand500, Brand700),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Hub, null, Modifier.size(48.dp), tint = Color.White)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Smart Remote",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
            Text(
                "Your universal smart home controller",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(48.dp))

            // Mode toggle
            Row(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(4.dp)
            ) {
                listOf("Sign In", "Sign Up").forEachIndexed { i, label ->
                    val selected = (i == 0) == uiState.isLogin
                    Box(
                        Modifier.weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) Brand600 else Color.Transparent)
                            .clickable { if ((i == 0) != uiState.isLogin) viewModel.toggleMode() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Form card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {

                    AnimatedVisibility(!uiState.isLogin) {
                        AuthTextField(
                            value = uiState.displayName,
                            onValueChange = viewModel::updateName,
                            label = "Full Name",
                            icon = Icons.Default.Person,
                            imeAction = ImeAction.Next,
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        )
                    }

                    AuthTextField(
                        value = uiState.email,
                        onValueChange = viewModel::updateEmail,
                        label = "Email Address",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )

                    AuthTextField(
                        value = uiState.password,
                        onValueChange = viewModel::updatePassword,
                        label = "Password",
                        icon = Icons.Default.Lock,
                        isPassword = true,
                        passwordVisible = uiState.passwordVisible,
                        onTogglePassword = viewModel::togglePasswordVisible,
                        imeAction = ImeAction.Done,
                        onDone = { viewModel.submit(onAuthenticated) }
                    )

                    // Error message
                    AnimatedVisibility(uiState.error != null) {
                        Row(
                            Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(RedAlert.copy(alpha = 0.15f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, null, Modifier.size(16.dp), tint = RedAlert)
                            Text(uiState.error ?: "", style = MaterialTheme.typography.bodySmall, color = RedAlert)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Submit button
            Button(
                onClick = { viewModel.submit(onAuthenticated) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Brand600)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (uiState.isLogin) "Sign In" else "Create Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Divider
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
                Text("  or  ", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.4f))
                HorizontalDivider(Modifier.weight(1f), color = Color.White.copy(alpha = 0.15f))
            }

            Spacer(Modifier.height(16.dp))

            // Guest button
            OutlinedButton(
                onClick = { viewModel.continueAsGuest(onAuthenticated) },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Icon(Icons.Default.PersonOutline, null, tint = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.width(8.dp))
                Text("Continue as Guest", color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.titleSmall)
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Auth Text Field ──────────────────────────────────────────────────────────

@Composable
fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: () -> Unit = {},
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        leadingIcon = { Icon(icon, null, tint = Brand400) },
        trailingIcon = if (isPassword) {{
            IconButton(onClick = onTogglePassword) {
                Icon(
                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, tint = Color.White.copy(alpha = 0.5f)
                )
            }
        }} else null,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White.copy(alpha = 0.8f),
            focusedBorderColor = Brand400,
            unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Brand400
        ),
        singleLine = true
    )
}
