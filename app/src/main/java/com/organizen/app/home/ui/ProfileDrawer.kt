package com.organizen.app.home.ui

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.organizen.app.auth.AuthViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.organizen.app.home.data.HealthViewModel

@Composable
fun ProfileDrawerContent(vm: AuthViewModel, onLogout: () -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(vm.currentUser?.displayName ?: "") }
    val email = vm.currentUser?.email ?: ""
    var editingName by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    val healthVm: HealthViewModel = viewModel()
    var editingSteps by remember { mutableStateOf(false) }
    var stepsTargetText by remember { mutableStateOf(healthVm.stepsGoal.toInt().toString()) }
    var editingSleep by remember { mutableStateOf(false) }
    var sleepTargetText by remember { mutableStateOf(healthVm.sleepGoal.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            // Name (editabil)
            EditableRow(
                value = name,
                editing = editingName,
                label = "Name",
                onValueChange = { name = it },
                onEdit = { editingName = true },
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
            EditableRow(
                value = if (editingSteps) stepsTargetText else healthVm.stepsGoal.toInt().toString(),
                editing = editingSteps,
                label = "Steps target",
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
            EditableRow(
                value = if (editingSleep) sleepTargetText else healthVm.sleepGoal.toString(),
                editing = editingSleep,
                label = "Sleep hours target",
                onValueChange = { sleepTargetText = it },
                onEdit = {
                    sleepTargetText = healthVm.sleepGoal.toString()
                    editingSleep = true
                },
                onSave = {
                    sleepTargetText.toDoubleOrNull()?.let { healthVm.updateSleepGoal(it) }
                    editingSleep = false
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
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
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editing) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                label = { Text(label) },
                keyboardOptions = keyboardOptions
            )
            IconButton(onClick = onSave) {
                Icon(Icons.Filled.Check, contentDescription = "Save")
            }
        } else {
            Text(text = value, modifier = Modifier.weight(1f))
            IconButton(onClick = onEdit) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit")
            }
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
