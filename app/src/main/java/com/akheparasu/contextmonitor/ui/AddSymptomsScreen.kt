package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star as StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akheparasu.contextmonitor.R
import com.akheparasu.contextmonitor.storage.DataEntity
import com.akheparasu.contextmonitor.storage.Storagedb
import com.akheparasu.contextmonitor.ui.theme.ContextMonitorTheme
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSymptomsScreen(viewModel: SymptomAddModel, heartRate: Float, respiratoryRate: Float) {
    // Get the context from the Composable
    val context = LocalContext.current
    // Load the string array from resources
    val symptomsList = remember {
        context.resources.getStringArray(R.array.symptoms_array).toList()
    }
    val symptoms by remember { mutableStateOf(
        symptomsList.associateWith { 0 }.toMutableMap()
    ) }
    var selectedSymptom by remember { mutableStateOf(symptomsList[0]) }
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Record a symptom:")

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            // TextField showing the selected number
            TextField(
                value = selectedSymptom,
                onValueChange = {},
                readOnly = true,
                label = { Text("Select a symptom") },
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                },
                colors = TextFieldDefaults.textFieldColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )

            // Dropdown menu for selecting numbers
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                symptomsList.forEach { value ->
                    DropdownMenuItem(
                        onClick = {
                            selectedSymptom = value
                            expanded = false
                        },
                        text = {
                            Text(text = value)
                        }
                    )
                }
            }
        }

        Text(text = "Select symptom rating")
        symptoms[selectedSymptom]?.let {
            StarRatingBar(
                rating = it,
                onRatingChange = { newRating -> symptoms.apply { this[selectedSymptom] = newRating } }
            )
        }

        Button(onClick = {
            viewModel.insertSymptom(heartRate, respiratoryRate, symptoms)
        }) {
            Text("Upload Symptoms")
        }
    }
}

class SymptomAddModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = Storagedb.getDatabase(application).dataDao()

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

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row {
        for (i in 1..5) {
            val icon = if (i <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder
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

@Preview(showBackground = true)
@Composable
fun AddSymptomsScreenPreview() {
    ContextMonitorTheme {
        AddSymptomsScreen(SymptomAddModel(Application()),1.0f, 10.0f)
    }
}

