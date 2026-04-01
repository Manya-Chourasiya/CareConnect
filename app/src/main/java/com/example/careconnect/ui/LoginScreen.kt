package com.example.careconnect.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.careconnect.CareConnectViewModel
import com.example.careconnect.UserRole

@Composable
fun LoginScreen(
    viewModel: CareConnectViewModel,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.PATIENT) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to CareConnect",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = selectedRole == UserRole.PATIENT,
                onClick = { selectedRole = UserRole.PATIENT }
            )
            Text("Patient")
            Spacer(modifier = Modifier.width(16.dp))
            RadioButton(
                selected = selectedRole == UserRole.DOCTOR,
                onClick = { selectedRole = UserRole.DOCTOR }
            )
            Text("Doctor")
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (email.isNotBlank()) {
                    viewModel.login(email, selectedRole)
                    onLoginSuccess()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Login")
        }
    }
}
