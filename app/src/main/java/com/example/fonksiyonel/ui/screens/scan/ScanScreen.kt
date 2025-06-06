package com.example.fonksiyonel.ui.screens.scan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.rememberAsyncImagePainter
import com.example.fonksiyonel.R
import com.example.fonksiyonel.model.CancerType
import com.example.fonksiyonel.model.DiagnosisResult
import com.example.fonksiyonel.model.RiskLevel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onNavigateBack: () -> Unit,
    onScanComplete: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisResult by remember { mutableStateOf<DiagnosisResult?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashEnabled by remember { mutableStateOf(false) }
    
    // Check and request camera permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            cameraError = "Kamera izni reddedildi. Lütfen ayarlardan izin verin."
        }
    }
    
    // Gallery launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            imageUri = it
            showCamera = false
        }
    }
    
    // Camera setup
    val imageCapture = remember { 
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build() 
    }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val cameraExecutor = remember { ContextCompat.getMainExecutor(context) }
    
    // Function to take a photo
    val takePhoto = {
        val photoFile = File(
            context.getExternalFilesDir(null),
            "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.jpg"
        )
        
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        
        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    imageUri = savedUri
                    showCamera = false
                    // Trigger analysis after taking photo
                    analyzeImage(savedUri)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    cameraError = "Fotoğraf çekilirken hata oluştu: ${exception.message}"
                    Log.e("CameraX", "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }
    
    // Function to analyze the image
    fun analyzeImage(uri: Uri) {
        isAnalyzing = true
        // Simulate analysis - replace with actual ML model in production
        android.os.Handler().postDelayed({
            analysisResult = DiagnosisResult(
                cancerType = CancerType.MELANOMA,
                confidencePercentage = 0.78f,
                riskLevel = RiskLevel.MEDIUM
            )
            isAnalyzing = false
        }, 2000)
    }
    
    // Check camera permission when screen is shown
    LaunchedEffect(Unit) {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) -> {
                showCamera = true
            }
            else -> {
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }


    // Camera Preview Composable
    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    @Composable
    fun CameraPreview(
        imageCapture: ImageCapture,
        modifier: Modifier = Modifier,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK,
        onCameraError: (String) -> Unit = {}
    ) {
        val lifecycleOwner = LocalLifecycleOwner.current
        val context = LocalContext.current
        val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
        var preview by remember { mutableStateOf<Preview?>(null) }
        
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val executor = ContextCompat.getMainExecutor(ctx)
                
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        
                        // Set up the preview use case
                        preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // Set up the image capture use case
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()
                        
                        // Unbind all use cases before rebinding
                        cameraProvider.unbindAll()
                        
                        // Bind use cases to the camera
                        val camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            selector,
                            preview,
                            imageCapture
                        )
                        
                        // Set up flash
                        camera.cameraControl.enableTorch(flashEnabled)
                        
                    } catch (e: Exception) {
                        onCameraError("Kamera başlatılamadı: ${e.message}")
                        Log.e("CameraPreview", "Use case binding failed", e)
                    }
                }, executor)
                
                previewView
            },
            modifier = modifier
        )
        
        // Update flash state when it changes
        LaunchedEffect(flashEnabled) {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val camera = cameraProvider.unbindAll().firstOrNull()
                camera?.cameraControl?.enableTorch(flashEnabled)
            } catch (e: Exception) {
                Log.e("CameraPreview", "Error updating flash state", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tarama Yap") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    if (showCamera) {
                        IconButton(onClick = { 
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) 
                                CameraSelector.LENS_FACING_FRONT 
                            else 
                                CameraSelector.LENS_FACING_BACK
                        }) {
                            Icon(
                                imageVector = Icons.Default.Cameraswitch,
                                contentDescription = "Kamera Değiştir"
                            )
                        }
                        IconButton(onClick = { flashEnabled = !flashEnabled }) {
                            Icon(
                                imageVector = if (flashEnabled) Icons.Default.FlashOn 
                                           else Icons.Default.FlashOff,
                                contentDescription = if (flashEnabled) "Flaş Kapat" else "Flaş Aç"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showCamera) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    CameraPreview(
                        imageCapture = imageCapture,
                        modifier = Modifier.fillMaxSize(),
                        lensFacing = lensFacing,
                        onCameraError = { error ->
                            cameraError = error
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_gallery),
                                contentDescription = "Galeriden Seç",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Capture Button
                        IconButton(
                            onClick = takePhoto,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_camera),
                                contentDescription = "Fotoğraf Çek",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        
                        // Close Camera Button
                        IconButton(
                            onClick = { showCamera = false },
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_close),
                                contentDescription = "Kamerayı Kapat",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            } else if (imageUri != null) {
                // Image Preview and Analysis
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = rememberAsyncImagePainter(imageUri),
                            contentDescription = "Selected Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    if (isAnalyzing) {
                        // Loading Indicator
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Görsel analiz ediliyor...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (analysisResult != null) {
                        // Analysis Result
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Analiz Sonucu",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                
                                // Result Icon
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            when (analysisResult?.riskLevel) {
                                                RiskLevel.LOW -> MaterialTheme.colorScheme.tertiary
                                                RiskLevel.MEDIUM -> Color(0xFFFFA000)
                                                RiskLevel.HIGH -> Color(0xFFF57C00)
                                                RiskLevel.VERY_HIGH -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            id = when (analysisResult?.cancerType) {
                                                CancerType.BENIGN -> R.drawable.ic_check_circle
                                                else -> R.drawable.ic_warning
                                            }
                                        ),
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                // Cancer Type
                                Text(
                                    text = when (analysisResult?.cancerType) {
                                        CancerType.BENIGN -> "İyi Huylu (Benign)"
                                        CancerType.MELANOMA -> "Melanoma"
                                        CancerType.BASAL_CELL_CARCINOMA -> "Bazal Hücreli Karsinom"
                                        CancerType.SQUAMOUS_CELL_CARCINOMA -> "Skuamöz Hücreli Karsinom"
                                        else -> "Bilinmiyor"
                                    },
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Confidence
                                Text(
                                    text = "Güven Oranı: ${(analysisResult?.confidencePercentage?.times(100))?.toInt()}%",
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Risk Level
                                Box(
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .clip(RoundedCornerShape(24.dp))
                                        .background(
                                            when (analysisResult?.riskLevel) {
                                                RiskLevel.LOW -> MaterialTheme.colorScheme.tertiary
                                                RiskLevel.MEDIUM -> Color(0xFFFFA000)
                                                RiskLevel.HIGH -> Color(0xFFF57C00)
                                                RiskLevel.VERY_HIGH -> MaterialTheme.colorScheme.error
                                                else -> MaterialTheme.colorScheme.primary
                                            }
                                        )
                                        .padding(horizontal = 24.dp, vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = when (analysisResult?.riskLevel) {
                                            RiskLevel.LOW -> "Düşük Risk"
                                            RiskLevel.MEDIUM -> "Orta Risk"
                                            RiskLevel.HIGH -> "Yüksek Risk"
                                            RiskLevel.VERY_HIGH -> "Çok Yüksek Risk"
                                            else -> "Bilinmiyor"
                                        },
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                                
                                // Warning
                                Text(
                                    text = "Bu sonuç sadece ön teşhistir. Mutlaka doktorunuza danışın.",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    imageUri = null
                                    analysisResult = null
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(end = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Yeni Tarama")
                            }
                            
                            Button(
                                onClick = { onScanComplete("report123") },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(start = 8.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Raporu Kaydet")
                            }
                        }
                    } else {
                        // Analyze Button
                        Button(
                            onClick = analyzeImage,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Analiz Et")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Change Image Button
                        OutlinedButton(
                            onClick = { imageUri = null },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Görseli Değiştir")
                        }
                    }
                }
            } else {
                // Initial Screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // App Logo
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 24.dp)
                    )
                    
                    // Title
                    Text(
                        text = "Yapay Zeka ile Cilt Taraması",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Description
                    Text(
                        text = "Şüpheli bir lekenin fotoğrafını çekerek veya galeriden yükleyerek analiz edebilirsiniz.",
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    
                    // Camera Button
                    Button(
                        onClick = { showCamera = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_camera),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Fotoğraf Çek")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Gallery Button
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_gallery),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Galeriden Yükle")
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Information
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Bilgilendirme",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "Bu uygulama sadece ön teşhis amaçlıdır. Kesin teşhis için mutlaka bir dermatoloğa başvurunuz.",
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
