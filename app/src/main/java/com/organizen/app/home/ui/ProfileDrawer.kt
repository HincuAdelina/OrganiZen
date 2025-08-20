package com.organizen.app.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.organizen.app.auth.AuthViewModel
import com.organizen.app.home.data.HealthViewModel
import kotlin.math.roundToInt

@Composable
fun ProfileDrawerContent(vm: AuthViewModel, healthVm: HealthViewModel, onLogout: () -> Unit, onClose: () -> Unit){
var name by remember { mutableStateOf(vm.currentUser?.displayName ?: "") }
    val email = vm.currentUser?.email ?: ""
    var editingName by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Steps state
    var editingSteps by remember { mutableStateOf(false) }
    var stepsTargetText by remember { mutableStateOf(healthVm.stepsGoal.toInt().toString()) }

    // Sleep state (ore + minute)
    var editingSleep by remember { mutableStateOf(false) }
    val initSleepTotalMin = remember(healthVm.sleepGoal) { (healthVm.sleepGoal * 60).roundToInt() }
    var sleepHoursText by remember { mutableStateOf((initSleepTotalMin / 60).toString()) }
    var sleepMinutesText by remember { mutableStateOf((initSleepTotalMin % 60).toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Name
            EditableRow(
                value = name,
                editing = editingName,
                label = "Name",
                onValueChange = { name = it },
                onEdit = {
                    name = vm.currentUser?.displayName.orEmpty()
                    editingName = true
                },
                onSave = {
                    vm.updateName(name) { if (it) editingName = false }
                }
            )

            Spacer(Modifier.height(8.dp))

            ReadOnlyRow(label = "Email", value = email)

            Spacer(Modifier.height(24.dp))

            Button(onClick = { showPasswordDialog = true }) {
                Text("Change Password")
            }

            Spacer(Modifier.height(24.dp))
            Text("Set your targets", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            // Steps target (menține chenarul și când nu editezi)
            EditableRow(
                value = if (editingSteps) stepsTargetText else healthVm.stepsGoal.toInt().toString(),
                editing = editingSteps,
                label = "Steps",
                onValueChange = { stepsTargetText = it },
                onEdit = {
                    stepsTargetText = healthVm.stepsGoal.toInt().toString()
                    editingSteps = true
                },
                onSave = {
                    stepsTargetText.toFloatOrNull()?.let { healthVm.updateStepsGoal(it) }
                    editingSteps = false
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(8.dp))

            // Sleep target în ore + minute (menține chenarele și când nu editezi)
            EditableSleepRow(
                hoursText = sleepHoursText,
                minutesText = sleepMinutesText,
                editing = editingSleep,
                onHoursChange = { sleepHoursText = it.filter { ch -> ch.isDigit() } },
                onMinutesChange = { sleepMinutesText = it.filter { ch -> ch.isDigit() } },
                onEdit = {
                    val total = (healthVm.sleepGoal * 60).roundToInt()
                    sleepHoursText = (total / 60).toString()
                    sleepMinutesText = (total % 60).toString()
                    editingSleep = true
                },
                onSave = {
                    val h = sleepHoursText.toIntOrNull() ?: 0
                    val m = (sleepMinutesText.toIntOrNull() ?: 0).coerceIn(0, 59)
                    val goal = h + m / 60.0
                    healthVm.updateSleepGoal(goal)
                    editingSleep = false
                }
            )
        }

        Button(
            onClick = {
                vm.logout()
                onLogout()
                onClose()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Logout") }
    }


    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        var error by remember { mutableStateOf<String?>(null) }
        var passwordVisible by remember { mutableStateOf(false) }
        var confirmVisible by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                            IconButton(onClick = { confirmVisible = !confirmVisible }) {
                                Icon(image, contentDescription = if (confirmVisible) "Hide password" else "Show password")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = Color.Red) }
                }
            },

            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showPasswordDialog = false },
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }

                    Button(
                        onClick = {
                            if (password.isBlank() || confirm.isBlank()) {
                                error = "Something is incorrect"; return@Button
                            }
                            if (password != confirm) {
                                error = "Passwords do not match"; return@Button
                            }
                            vm.updatePassword(password) {
                                showPasswordDialog = false
                                showSuccessDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Save") }
                }
            },
            dismissButton = {}
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("Password changed successfully") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun EditableRow(
    value: String,
    editing: Boolean,
    label: String,
    onValueChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            label = { Text(label) },
            readOnly = !editing,
            keyboardOptions = keyboardOptions,
            singleLine = true
        )
        IconButton(onClick = if (editing) onSave else onEdit) {
            Icon(if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = if (editing) "Save" else "Edit")
        }
    }
}

@Composable
private fun EditableSleepRow(
    hoursText: String,
    minutesText: String,
    editing: Boolean,
    onHoursChange: (String) -> Unit,
    onMinutesChange: (String) -> Unit,
    onEdit: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = hoursText,
            onValueChange = onHoursChange,
            modifier = Modifier.weight(1f),
            label = { Text("Sleep hours") },
            placeholder = { Text("h") },
            readOnly = !editing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = minutesText,
            onValueChange = onMinutesChange,
            modifier = Modifier.weight(1f),
            label = { Text("Minutes") },
            placeholder = { Text("m") },
            readOnly = !editing,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        IconButton(onClick = if (editing) onSave else onEdit) {
            Icon(if (editing) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = if (editing) "Save" else "Edit")
        }
    }
}

@Composable
private fun ReadOnlyRow(
    label: String,
    value: String,
) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
