package com.georacing.georacing.ui.screens.login

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.navigation.NavController
import com.georacing.georacing.R
import com.georacing.georacing.data.firebase.FirebaseAuthService
import com.georacing.georacing.data.local.UserPreferencesDataStore
import com.georacing.georacing.data.repository.NetworkUserRepository
import com.georacing.georacing.ui.navigation.Screen
import com.georacing.georacing.ui.theme.CircuitStop
import com.georacing.georacing.ui.theme.GlassLevel
import com.georacing.georacing.ui.theme.LocalActiveEventConfig
import com.georacing.georacing.ui.theme.LocalEventVisualStyle
import com.georacing.georacing.ui.theme.StatusRed
import com.georacing.georacing.ui.theme.TextPrimary
import com.georacing.georacing.ui.theme.TextSecondary
import com.georacing.georacing.ui.theme.TextTertiary
import com.georacing.georacing.ui.theme.liquidGlass
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LoginScreen"
private const val FALLBACK_WEB_CLIENT_ID =
    "62243274149-iv3ra1epplkgsr3oeipgrej6i9r62qfs.apps.googleusercontent.com"

@Composable
fun LoginScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val backdrop = com.georacing.georacing.ui.glass.LocalBackdrop.current
    val scope = rememberCoroutineScope()
    val authService = remember { FirebaseAuthService() }
    val userRepository = remember { NetworkUserRepository() }
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val activeEvent = LocalActiveEventConfig.current
    val visuals = LocalEventVisualStyle.current

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val webClientId = remember(context) {
        runCatching { context.getString(R.string.default_web_client_id) }
            .getOrDefault(FALLBACK_WEB_CLIENT_ID)
            .ifBlank { FALLBACK_WEB_CLIENT_ID }
    }

    val googleSignInClient = remember(context, webClientId) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    suspend fun finishGoogleSignIn(idToken: String, provider: String) {
        val authResult = authService.signInWithGoogle(idToken)

        if (authResult.isSuccess) {
            val user = authResult.getOrNull()
            Log.d(TAG, "Google login OK ($provider): ${user?.email}")

            user?.let {
                val registerResult = userRepository.registerUser(
                    uid = it.uid,
                    name = it.displayName,
                    email = it.email,
                    photoUrl = it.photoUrl?.toString()
                )
                if (registerResult.isFailure) {
                    Log.w(
                        TAG,
                        "Firebase login succeeded but backend sync failed",
                        registerResult.exceptionOrNull()
                    )
                }
            }

            val onboardingCompleted = UserPreferencesDataStore(context)
                .onboardingCompleted
                .first()
            val nextRoute = if (onboardingCompleted) {
                Screen.Home.route
            } else {
                Screen.Onboarding.route
            }
            navController.navigate(nextRoute) {
                popUpTo(0) { inclusive = true }
            }
        } else {
            val exception = authResult.exceptionOrNull()
            Log.e(TAG, "Firebase auth error", exception)
            errorMessage = exception?.localizedMessage
                ?.takeIf { it.isNotBlank() }
                ?: "No se pudo autenticar con Firebase."
        }

        isLoading = false
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Log.d(TAG, "Google Sign-In canceled by user")
            isLoading = false
            return@rememberLauncherForActivityResult
        }

        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken.isNullOrBlank()) {
                errorMessage = "No se pudo obtener el token de Google."
                isLoading = false
                return@rememberLauncherForActivityResult
            }

            Log.d(TAG, "Google ID token received from legacy flow")
            scope.launch {
                isLoading = true
                finishGoogleSignIn(idToken, provider = "GoogleSignInClient")
            }
        } catch (e: ApiException) {
            Log.e(
                TAG,
                "Legacy Google Sign-In error: ${e.statusCode} (${CommonStatusCodes.getStatusCodeString(e.statusCode)})",
                e
            )
            errorMessage = googleSignInErrorMessage(e.statusCode)
            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected legacy Google Sign-In error", e)
            errorMessage = "Error inesperado al iniciar sesion con Google."
            isLoading = false
        }
    }

    fun launchLegacyGoogleSignIn() {
        Log.d(TAG, "Launching legacy Google Sign-In fallback")

        fun launchIntent() {
            try {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            } catch (launchError: Exception) {
                Log.e(TAG, "Could not launch Google account picker", launchError)
                errorMessage = "No se pudo abrir el selector de Google."
                isLoading = false
            }
        }

        googleSignInClient.signOut()
            .addOnCompleteListener { launchIntent() }
            .addOnFailureListener { signOutError ->
                Log.w(TAG, "Could not clear previous Google session", signOutError)
                launchIntent()
            }
    }

    fun handleGoogleSignIn() {
        Log.d(TAG, "Starting Google Sign-In")
        isLoading = true
        errorMessage = null

        val hostActivity = activity
        if (hostActivity == null) {
            Log.w(TAG, "No activity context available, using legacy flow")
            launchLegacyGoogleSignIn()
            return
        }

        scope.launch {
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(
                    GetGoogleIdOption.Builder()
                        .setServerClientId(webClientId)
                        .setFilterByAuthorizedAccounts(false)
                        .setAutoSelectEnabled(false)
                        .build()
                )
                .build()

            try {
                val result = credentialManager.getCredential(hostActivity, request)
                val credential = result.credential

                if (
                    credential is CustomCredential &&
                    (
                        credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL ||
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
                        )
                ) {
                    val googleIdCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdCredential.idToken

                    if (idToken.isBlank()) {
                        errorMessage = "No se pudo obtener el token de Google."
                        isLoading = false
                    } else {
                        Log.d(TAG, "Google ID token received from Credential Manager")
                        finishGoogleSignIn(idToken, provider = "CredentialManager")
                    }
                } else {
                    Log.w(TAG, "Credential Manager returned an unsupported credential type")
                    launchLegacyGoogleSignIn()
                }
            } catch (e: NoCredentialException) {
                Log.w(TAG, "No saved Google credential found, falling back to legacy flow", e)
                launchLegacyGoogleSignIn()
            } catch (e: GoogleIdTokenParsingException) {
                Log.e(TAG, "Could not parse Google credential", e)
                errorMessage = "No se pudo procesar la cuenta de Google."
                isLoading = false
            } catch (e: GetCredentialException) {
                Log.w(TAG, "Credential Manager failed, falling back to legacy flow", e)
                launchLegacyGoogleSignIn()
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error starting Google Sign-In", e)
                launchLegacyGoogleSignIn()
            }
        }
    }

    // Debug/tester only: enter the app without Google Sign-In. Gated by
    // BuildConfig.DEBUG so it never ships in a release build.
    fun handleGuestLogin() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val prefs = UserPreferencesDataStore(context)
                prefs.setGuestMode(true)
                val onboardingCompleted = prefs.onboardingCompleted.first()
                val nextRoute = if (onboardingCompleted) Screen.Home.route else Screen.Onboarding.route
                navController.navigate(nextRoute) { popUpTo(0) { inclusive = true } }
            } catch (e: Exception) {
                Log.e(TAG, "Guest login error", e)
                errorMessage = "No se pudo entrar como invitado."
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        Log.d(TAG, "LoginScreen loaded")
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = visuals.appBackgroundStops
                )
            )
    ) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        visuals.ambientPrimary,
                        Color.Transparent
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f),
                    radius = size.width * 0.6f
                ),
                radius = size.width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.5f, size.height * 0.25f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    visuals.navSelected.copy(alpha = 0.15f * pulseScale),
                                    Color.Transparent
                                )
                            ),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = "GeoRacing",
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            scaleX = pulseScale
                            scaleY = pulseScale
                        },
                    tint = visuals.navSelected
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(visuals.navSelected, shape = androidx.compose.foundation.shape.CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = activeEvent.shortName.uppercase(),
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.NIGHT) 0.4.sp else 3.sp
                    ),
                    color = TextPrimary
                )
            }

            Text(
                text = activeEvent.venueName.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = TextTertiary,
                modifier = Modifier.padding(top = 8.dp),
                letterSpacing = if (visuals.variant == com.georacing.georacing.data.event.EventThemeVariant.NIGHT) 0.5.sp else 2.sp
            )

            Spacer(modifier = Modifier.height(56.dp))

            Text(
                text = activeEvent.heroTitle,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = activeEvent.heroSubtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            com.georacing.georacing.ui.glass.LiquidButton(
                onClick = { handleGoogleSignIn() },
                backdrop = backdrop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                isInteractive = !isLoading,
                surfaceColor = visuals.loginButtonSurface
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = visuals.navSelected,
                        strokeWidth = 2.dp
                    )
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = "Google",
                            modifier = Modifier.size(24.dp),
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "CONTINUAR CON GOOGLE",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color.Black
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(
                            shape = RoundedCornerShape(12.dp),
                            level = GlassLevel.L1
                        )
                        .background(CircuitStop.copy(alpha = 0.1f))
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = StatusRed,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (com.georacing.georacing.BuildConfig.DEBUG) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(
                    onClick = { handleGuestLogin() },
                    enabled = !isLoading
                ) {
                    Text(
                        text = "ENTRAR COMO INVITADO (debug)",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = visuals.navSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Al continuar, aceptas nuestros terminos\ny condiciones de uso",
                style = MaterialTheme.typography.bodySmall,
                color = TextTertiary,
                textAlign = TextAlign.Center
            )
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private fun googleSignInErrorMessage(statusCode: Int): String {
    return when (statusCode) {
        CommonStatusCodes.NETWORK_ERROR -> "No se pudo conectar con Google. Revisa tu conexion."
        CommonStatusCodes.SIGN_IN_REQUIRED -> "Selecciona una cuenta de Google para continuar."
        CommonStatusCodes.CANCELED -> "Inicio de sesion cancelado."
        CommonStatusCodes.DEVELOPER_ERROR -> {
            "Esta build no esta autorizada para Google Sign-In. Anade la SHA de la app en Firebase y vuelve a descargar google-services.json."
        }
        else -> "Error al iniciar sesion con Google (${CommonStatusCodes.getStatusCodeString(statusCode)})."
    }
}
