package com.example.hmint.composables

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.hmint.MainViewModel
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

@Composable
fun WalletConnectButton(
    identityUri: Uri,
    iconUri: Uri,
    identityName: String,
    activityResultSender: ActivityResultSender,
    mainViewModel: MainViewModel = hiltViewModel()
) {
    val viewState = mainViewModel.viewState.collectAsState().value

    Column() {
        Button(
            onClick = {
                if (viewState.userAddress.isEmpty()) {
                    mainViewModel.connect(identityUri, iconUri, identityName, activityResultSender)
                } else {
                    mainViewModel.disconnect()
                }
            },
            colors = ButtonDefaults.buttonColors(
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.primary
            )
        ) {
            val pubKey = viewState.userAddress
            val buttonText = when {
                viewState.noWallet -> "Please install a wallet"
                pubKey.isEmpty() -> "Connect Wallet"
                viewState.userAddress.isNotEmpty() -> pubKey.take(4).plus("...").plus(pubKey.takeLast(4))
                else -> ""
            }

            Text(
                modifier = Modifier.padding(start = 8.dp),
                color = MaterialTheme.colorScheme.primary,
                text = buttonText,
                maxLines = 1,)
        }
    }
}
