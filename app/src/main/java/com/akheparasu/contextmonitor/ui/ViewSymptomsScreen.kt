package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        items(records.size) { record ->
            Card {
                DisplayText("Recorded On: ", records[record].recordedOn.toString())
                DisplayText("Heart Rate: ", records[record].heartRate.toString())
                DisplayText("Respiratory Rate: ", records[record].respiratoryRate.toString())
                DisplayText(
                    if (records[record].symptoms.containsValue(1)) {
                        "Symptoms: "
                    } else {
                        "No Symptoms"
                    }
                )
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    symptomsList
                        .filter { value -> records[record].symptoms[value] != 0 }
                        .forEach { value ->
                            Text(text = " - $value : ${records[record].symptoms[value]}")
                        }
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

@Composable
fun DisplayText(
    key: String,
    value: String? = null
) {
    Text(
        text = buildAnnotatedString {
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(key)
            }
            if (value != null) {
                append(value)
            }
        },
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}
