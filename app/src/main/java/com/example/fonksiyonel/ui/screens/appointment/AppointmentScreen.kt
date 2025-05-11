package com.example.fonksiyonel.ui.screens.appointment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fonksiyonel.R
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import com.maxkeppeler.sheets.clock.ClockDialog
import com.maxkeppeler.sheets.clock.models.ClockConfig
import com.maxkeppeler.sheets.clock.models.ClockSelection
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentScreen(
    onNavigateBack: () -> Unit,
    onAppointmentBooked: () -> Unit
) {
    // State for appointment details
    var selectedDoctor by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    
    // Calendar dialog state
    var showCalendarDialog by remember { mutableStateOf(false) }
    
    // Time dialog state
    var showTimeDialog by remember { mutableStateOf(false) }
    
    // Available doctors (in a real app, this would come from a database)
    val availableDoctors = remember {
        listOf(
            "Dr. Ahmet Yılmaz - Dermatolog",
            "Dr. Ayşe Kaya - Dermatolog",
            "Dr. Mehmet Demir - Onkolog"
        )
    }
    
    // Calendar Dialog
    if (showCalendarDialog) {
        CalendarDialog(
            onCloseRequest = { showCalendarDialog = false },
            config = CalendarConfig(
                yearSelection = true,
                monthSelection = true
            ),
            selection = CalendarSelection.Date { date ->
                selectedDate = date
                showCalendarDialog = false
                showTimeDialog = true
            }
        )
    }
    
    // Time Dialog
    if (showTimeDialog) {
        ClockDialog(
            onCloseRequest = { showTimeDialog = false },
            config = ClockConfig(
                is24HourFormat = true
            ),
            selection = ClockSelection.HoursMinutes { hours, minutes ->
                selectedTime = LocalTime.of(hours, minutes)
                showTimeDialog = false
            }
        )
    }
    
    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {
                showSuccessDialog = false
                onAppointmentBooked()
            },
            title = { Text("Başarılı") },
            text = { Text("Randevunuz başarıyla oluşturuldu. Size bildirim gönderilecektir.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onAppointmentBooked()
                    }
                ) {
                    Text("Tamam")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Randevu Al") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = "Geri"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "Dermatoloji Randevusu",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            // Error Message
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Doctor Selection
            Text(
                text = "Doktor Seçimi",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Start
            )
            
            Box {
                var expanded by remember { mutableStateOf(false) }
                
                OutlinedTextField(
                    value = selectedDoctor,
                    onValueChange = { },
                    label = { Text("Doktor Seçin") },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_dropdown),
                                contentDescription = "Doktor Seç"
                            )
                        }
                    }
                )
                
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    availableDoctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text(doctor) },
                            onClick = {
                                selectedDoctor = doctor
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            // Date and Time Selection
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Date Selection
                OutlinedTextField(
                    value = if (selectedDate != null) 
                        selectedDate!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")) 
                    else 
                        "",
                    onValueChange = { },
                    label = { Text("Tarih") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { showCalendarDialog = true }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_calendar),
                                contentDescription = "Tarih Seç"
                            )
                        }
                    }
                )
                
                // Time Selection
                OutlinedTextField(
                    value = if (selectedTime != null) 
                        selectedTime!!.format(DateTimeFormatter.ofPattern("HH:mm")) 
                    else 
                        "",
                    onValueChange = { },
                    label = { Text("Saat") },
                    readOnly = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = { 
                            if (selectedDate != null) {
                                showTimeDialog = true
                            } else {
                                errorMessage = "Önce tarih seçmelisiniz"
                            }
                        }) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_time),
                                contentDescription = "Saat Seç"
                            )
                        }
                    }
                )
            }
            
            // Description
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Açıklama") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .padding(bottom = 24.dp),
                shape = RoundedCornerShape(12.dp),
                placeholder = { Text("Randevu sebebinizi kısaca açıklayın") }
            )
            
            // Book Appointment Button
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null
                    
                    // Validate inputs
                    when {
                        selectedDoctor.isBlank() -> {
                            errorMessage = "Lütfen bir doktor seçin"
                            isLoading = false
                        }
                        selectedDate == null -> {
                            errorMessage = "Lütfen bir tarih seçin"
                            isLoading = false
                        }
                        selectedTime == null -> {
                            errorMessage = "Lütfen bir saat seçin"
                            isLoading = false
                        }
                        else -> {
                            // Simulate booking (replace with actual booking logic)
                            // In a real app, this would be a call to your backend service
                            showSuccessDialog = true
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Randevu Al")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
                        text = "Randevunuz onaylandığında size bildirim gönderilecektir. Randevunuza gelmeden önce, lütfen teşhis raporlarınızı yanınızda bulundurun.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}
