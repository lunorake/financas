
package com.meucofre.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

data class Movimento(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val receita: Boolean
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF65D6A0),
                    background = Color(0xFF101713),
                    surface = Color(0xFF1A241E)
                )
            ) {
                CofreApp(this)
            }
        }
    }
}

private fun dinheiro(valor: Double): String =
    NumberFormat.getCurrencyInstance(
        Locale("pt", "BR")
    ).format(valor)

@Composable
fun CofreApp(context: Context) {
    val preferencias = remember {
        context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
    }

    val movimentos = remember {
        mutableStateListOf<Movimento>().apply {
            val dados = preferencias.getString("movimentos", "[]")
            val json = JSONArray(dados)

            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                add(
                    Movimento(
                        id = item.getLong("id"),
                        descricao = item.getString("descricao"),
                        valor = item.getDouble("valor"),
                        receita = item.getBoolean("receita")
                    )
                )
            }
        }
    }

    var mostrarFormulario by remember { mutableStateOf(false) }
    var descricao by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }
    var receita by remember { mutableStateOf(false) }

    val totalReceitas = movimentos
        .filter { it.receita }
        .sumOf { it.valor }

    val totalDespesas = movimentos
        .filter { !it.receita }
        .sumOf { it.valor }

    val saldo = totalReceitas - totalDespesas

    fun salvar() {
        val valor = valorTexto
            .trim()
            .replace(".", "")
            .replace(",", ".")
            .toDoubleOrNull()

        if (descricao.isBlank() || valor == null || valor <= 0) {
            return
        }

        movimentos.add(
            0,
            Movimento(
                id = System.currentTimeMillis(),
                descricao = descricao.trim(),
                valor = valor,
                receita = receita
            )
        )

        val json = JSONArray()
        movimentos.forEach {
            json.put(
                JSONObject().apply {
                    put("id", it.id)
                    put("descricao", it.descricao)
                    put("valor", it.valor)
                    put("receita", it.receita)
                }
            )
        }

        preferencias.edit()
            .putString("movimentos", json.toString())
            .apply()

        descricao = ""
        valorTexto = ""
        receita = false
        mostrarFormulario = false
    }

    Scaffold(
        containerColor = Color(0xFF101713),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { mostrarFormulario = true },
                containerColor = Color(0xFF65D6A0),
                contentColor = Color(0xFF102017)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = Color(0xFF65D6A0),
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(Modifier.width(10.dp))

                    Column {
                        Text(
                            "MEU COFRE",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "Seu dinheiro, sob controle",
                            color = Color.LightGray
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1D382A)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            "Saldo registrado",
                            color = Color.LightGray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            dinheiro(saldo),
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color(0xFF65D6A0)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Calculado pelas movimentações cadastradas",
                            color = Color.LightGray,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Resumo(
                        "Receitas",
                        totalReceitas,
                        Color(0xFF65D6A0),
                        Modifier.weight(1f)
                    )
                    Resumo(
                        "Despesas",
                        totalDespesas,
                        Color(0xFFFF8A80),
                        Modifier.weight(1f)
                    )
                }
            }

            item {
                Text(
                    "Últimas movimentações",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            if (movimentos.isEmpty()) {
                item {
                    Text(
                        "Nenhuma movimentação ainda. Toque em + para começar.",
                        color = Color.LightGray
                    )
                }
            }

            items(movimentos, key = { it.id }) { movimento ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A241E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(movimento.descricao)
                            Text(
                                if (movimento.receita) "Receita" else "Despesa",
                                color = Color.LightGray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            (if (movimento.receita) "+ " else "- ") +
                                dinheiro(movimento.valor),
                            color = if (movimento.receita)
                                Color(0xFF65D6A0)
                            else
                                Color(0xFFFF8A80)
                        )
                    }
                }
            }
        }
    }

    if (mostrarFormulario) {
        AlertDialog(
            onDismissRequest = { mostrarFormulario = false },
            title = { Text("Nova movimentação") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = descricao,
                        onValueChange = { descricao = it },
                        label = { Text("Descrição") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = valorTexto,
                        onValueChange = { valorTexto = it },
                        label = { Text("Valor (ex.: 125,50)") },
                        singleLine = true
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Receita")
                        Switch(
                            checked = receita,
                            onCheckedChange = { receita = it }
                        )
                    }

                    Text(
                        "Ativado = receita. Desativado = despesa.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { salvar() }) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { mostrarFormulario = false }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun Resumo(
    titulo: String,
    valor: Double,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A241E)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, color = Color.LightGray)
            Spacer(Modifier.height(8.dp))
            Text(
                dinheiro(valor),
                color = cor,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}
