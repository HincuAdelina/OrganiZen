package com.organizen.app.home.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.organizen.app.auth.AuthViewModel

@Composable
fun ProfileDrawerContent(vm: AuthViewModel, onLogout: () -> Unit, onClose: () -> Unit) {
    var name by remember { mutableStateOf(vm.currentUser?.displayName ?: "") }
    var email by remember { mutableStateOf(vm.currentUser?.email ?: "") }
    var editingName by remember { mutableStateOf(false) }
    var editingEmail by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Column {
            EditableRow(value = name, editing = editingName, label = "Name",
                onValueChange = { name = it },
                onEdit = { editingName = true },
                onSave = {
                    vm.updateName(name) { if (it) editingName = false }
                })
            Spacer(Modifier.height(8.dp))
            EditableRow(value = email, editing = editingEmail, label = "Email",
                onValueChange = { email = it },
                onEdit = { editingEmail = true },
                onSave = {
                    vm.updateEmail(email) { if (it) editingEmail = false }
                })
            Spacer(Modifier.height(24.dp))
            Button(onClick = { showPasswordDialog = true }) {
                Text("Change Password")
            }
        }
        Button(onClick = {
            vm.logout()
            onLogout()
            onClose()
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Logout")
        }
    }

    if (showPasswordDialog) {
        var password by remember { mutableStateOf("") }
        var confirm by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { showPasswordDialog = false },
            title = { Text("Change Password") },
            text = {
                Column {
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = confirm, onValueChange = { confirm = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (password.isNotBlank() && password == confirm) {
                        vm.updatePassword(password) {
                            showPasswordDialog = false
                            showSuccessDialog = true
                        }
                    }
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) { Text("Cancel") }
            })
    }

    if (showSuccessDialog) {
        AlertDialog(onDismissRequest = { showSuccessDialog = false },
            title = { Text("Success") },
            text = { Text("Password changed successfully") },
            confirmButton = {
                Button(onClick = { showSuccessDialog = false }) { Text("OK") }
            })
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
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editing) {
            OutlinedTextField(value = value, onValueChange = onValueChange,
                modifier = Modifier.weight(1f), label = { Text(label) })
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
