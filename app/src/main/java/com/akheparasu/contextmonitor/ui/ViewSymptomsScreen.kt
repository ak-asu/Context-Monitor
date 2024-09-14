package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.akheparasu.contextmonitor.R
import com.akheparasu.contextmonitor.storage.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun ViewSymptomsScreen(viewModel: ViewSymptomsModel, padding: PaddingValues) {
    val context = LocalContext.current
    val symptomsList = context.resources.getStringArray(R.array.symptoms_array).toList()
    val records by viewModel.records.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getAllRecords()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(records.size) { record ->
            Text(text = "Signs $record")
            Text(text = "Recorded On: ${records[record].recordedOn}")
            Text(text = "Heart Rate: ${records[record].heartRate}")
            Text(text = "Respiratory Rate: ${records[record].respiratoryRate}")
            Text(text = "Symptoms:")
            Column {
                symptomsList
                    .filter { value -> records[record].symptoms[value] != 0 }
                    .forEach { value ->
                        Text(text = " - $value : ${records[record].symptoms[value]}")
                    }
            }
        }
    }
}

class ViewSymptomsModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = StorageDB.getDatabase(application).dataDao()

    // StateFlow to hold the records
    private val _records = MutableStateFlow<List<DataEntity>>(emptyList())
    val records: StateFlow<List<DataEntity>> get() = _records
    fun getAllRecords() {
        viewModelScope.launch {
            dataDao.getAllRows().collect { data -> _records.value = data }
        }
    }
}

class ViewSymptomsModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ViewSymptomsModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ViewSymptomsModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
