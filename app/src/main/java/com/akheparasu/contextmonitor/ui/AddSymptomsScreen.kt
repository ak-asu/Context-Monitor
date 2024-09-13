package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.akheparasu.contextmonitor.R
import com.akheparasu.contextmonitor.storage.DataEntity
import com.akheparasu.contextmonitor.storage.StorageDB
import com.akheparasu.contextmonitor.ui.theme.ContextMonitorTheme
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSymptomsScreen(
    navController: NavHostController,
    viewModel: SymptomAddModel,
    padding: PaddingValues,
    heartRate: Float,
    respiratoryRate: Float
) {
    // Get the context from the Composable
    val context = LocalContext.current
    // Load the string array from resources
    val symptomsList = remember {
        context.resources.getStringArray(R.array.symptoms_array).toList()
    }
    var symptoms by rememberSaveable { mutableStateOf(symptomsList.associateWith { 0 }) }
    var selectedSymptom by remember { mutableStateOf(symptomsList[0]) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    var pressOffset by remember {
        mutableStateOf(DpOffset.Zero)
    }
    var itemHeight by remember {
        mutableStateOf(0.dp)
    }
    val interactionSource = remember {
        MutableInteractionSource()
    }
    val density = LocalDensity.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Record a symptom:")

        Card(
            // elevation = 4.dp,
            modifier = Modifier
                .onSizeChanged {
                    itemHeight = with(density) { it.height.toDp() }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .indication(interactionSource, LocalIndication.current)
                    .pointerInput(true) {
                        detectTapGestures(
                            onPress = {
                                expanded = !expanded
                                pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                                val press = PressInteraction.Press(it)
                                interactionSource.emit(press)
                                tryAwaitRelease()
                                interactionSource.emit(PressInteraction.Release(press))
                            }
                        )
                    }
                    .padding(16.dp)
            ) {
                Text(text = selectedSymptom)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = pressOffset.copy(
                    y = pressOffset.y - itemHeight
                )
            ) {
                symptomsList.forEach {
                    DropdownMenuItem(onClick = {
                        selectedSymptom = it
                        expanded = false
                    }, text = { Text(text = it) }
                    )
                }
            }
        }
        Text(text = "Select symptom ${symptoms[selectedSymptom]}")
        StarRatingBar(
            rating = symptoms.getOrDefault(selectedSymptom, 0),
            onRatingChange = { newRating -> symptoms = symptoms.toMutableMap().apply { this[selectedSymptom] = newRating }.toMap() }
        )

        Button(onClick = {
            viewModel.insertSymptom(heartRate, respiratoryRate, symptoms)
            navController.popBackStack()
        }) {
            Text("Upload Symptoms")
        }
    }
}

class SymptomAddModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = StorageDB.getDatabase(application).dataDao()

    fun insertSymptom(heartRate: Float, respiratoryRate: Float, symptoms: Map<String, Int>) {
        val dataEntity = DataEntity(
            heartRate = heartRate,
            respiratoryRate = respiratoryRate,
            symptoms = symptoms,
            recordedOn = Date())
        viewModelScope.launch {
            dataDao.insertRow(dataEntity)
        }
    }
}

class SymptomAddModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SymptomAddModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SymptomAddModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            val icon = if (i <= rating) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
            Icon(
                imageVector = icon,
                contentDescription = "Star $i",
                tint = Color.Yellow,
                modifier = Modifier
                    .clickable { onRatingChange(i) }
                    .size(24.dp)
            )
        }
    }
}
