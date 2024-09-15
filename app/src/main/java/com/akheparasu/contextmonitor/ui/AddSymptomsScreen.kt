package com.akheparasu.contextmonitor.ui

import android.app.Application
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import kotlinx.coroutines.launch
import java.util.Date

@Composable
fun AddSymptomsScreen(
    navController: NavHostController,
    viewModel: AddSymptomsViewModel,
    padding: PaddingValues,
    heartRate: Int,
    respiratoryRate: Int
) {
    // Get the context from the Composable
    val context = LocalContext.current
    val density = LocalDensity.current
    val symptomsList = context.resources.getStringArray(R.array.symptoms_array).toList()
    val scrollState = rememberScrollState()

    var symptoms by rememberSaveable { mutableStateOf(symptomsList.associateWith { 0 }) }
    var selectedSymptom by rememberSaveable { mutableStateOf(symptomsList[0]) }
    var expanded by rememberSaveable { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemHeight by remember { mutableStateOf(0.dp) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .padding(4.dp)
                .onSizeChanged {
                    itemHeight = with(density) { it.height.toDp() }
                }
        ) {
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .width(256.dp)
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
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = selectedSymptom, modifier = Modifier.padding(8.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Star",
                        tint = Color.Yellow,
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    )
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        StarRatingBar(
            rating = symptoms.getOrDefault(selectedSymptom, 0),
            onRatingChange = { newRating ->
                symptoms = symptoms
                    .toMutableMap()
                    .apply { this[selectedSymptom] = newRating }.toMap()
            }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            modifier = Modifier.padding(vertical = 4.dp),
            onClick = {
                viewModel.insertSymptom(heartRate, respiratoryRate, symptoms)
                navController.popBackStack()
            }) {
            Text("UPLOAD SYMPTOMS")
        }
    }
}

class AddSymptomsViewModel(application: Application) : AndroidViewModel(application) {
    private val dataDao = StorageDB.getDatabase(application).dataDao()
    fun insertSymptom(heartRate: Int, respiratoryRate: Int, symptoms: Map<String, Int>) {
        val dataEntity = DataEntity(
            heartRate = heartRate,
            respiratoryRate = respiratoryRate,
            symptoms = symptoms,
            recordedOn = Date()
        )
        viewModelScope.launch {
            dataDao.insertRow(dataEntity)
        }
    }
}

class AddSymptomsViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddSymptomsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AddSymptomsViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Composable
fun StarRatingBar(
    rating: Int,
    onRatingChange: (Int) -> Unit
) {
    Row(modifier = Modifier.padding(4.dp)) {
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
