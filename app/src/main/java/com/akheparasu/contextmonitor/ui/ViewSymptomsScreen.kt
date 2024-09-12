package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.akheparasu.contextmonitor.R
import com.akheparasu.contextmonitor.storage.DataEntity
import com.akheparasu.contextmonitor.storage.Storagedb
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun ViewSymptomsScreen(viewModel: SymptomViewModel) {
    // Get the context from the Composable
    val context = LocalContext.current
    // Load the string array from resources
    val symptomsList = remember {
        context.resources.getStringArray(R.array.symptoms_array).toList()
    }
    val records by viewModel.records.collectAsState()

    // Call getAllRecords to fetch data
    LaunchedEffect(Unit) {
        viewModel.getAllRecords()
    }

    // Display records
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records.size) { record ->
            Text(text = records[record].recordedOn.toString())
            Text(text = records[record].heartRate.toString())
            Text(text = records[record].respiratoryRate.toString())
            Column(modifier = Modifier.padding(16.dp)) {
                symptomsList
                    .filter { value -> records[record].symptoms[value] != 0 }
                    .forEach { value ->
                        Text(text = "$value : ${records[record].symptoms[value]}")
                    }
            }
        }
    }
}

class SymptomViewModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = Storagedb.getDatabase(application).dataDao()

    // StateFlow to hold the records
    private val _records = MutableStateFlow<List<DataEntity>>(emptyList())
    val records: StateFlow<List<DataEntity>> get() = _records

    // Function to fetch records
    fun getAllRecords() {
        viewModelScope.launch {
            dataDao.getAllRows().collect { data ->
                _records.value = data
            }
        }
    }
}
