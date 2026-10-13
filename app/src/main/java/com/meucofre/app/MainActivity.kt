
package com.meucofre.app


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

private val Fundo = Color(0xFF101510)
private val Cartao = Color(0xFF1B241D)
private val Verde = Color(0xFF65E697)
private val Texto = Color(0xFFF2F5F2)
private val Secundario = Color(0xFF9BA89E)

private fun dinheiro(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private data class Movimento(
    val id: Long,
    val descricao: String,
    val valor: Double,
    val tipo: String,
    val categoria: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preferencias = getSharedPreferences("meu_cofre", MODE_PRIVATE)
        val lista = mutableListOf<Movimento>()

        try {
            val dados = JSONArray(preferencias.getString("movimentos", "[]"))
            for (i in 0 until dados.length()) {
                val item = dados.getJSONObject(i)
                lista.add(
                    Movimento(
                        item.getLong("id"),
                        item.getString("descricao"),
                        item.getDouble("valor"),
                        item.getString("tipo"),
                        item.getString("categoria")
                    )
                )
            }
        } catch (_: Exception) {
            // Mantém a lista vazia caso não existam dados salvos.
        }

        setContent {
            MeuCofreApp(
                movimentosIniciais = lista,
                salvar = { movimentos ->
                    val dados = JSONArray()
                    movimentos.forEach { m ->
                        dados.put(
                            JSONObject().apply {
                                put("id", m.id)
                                put("descricao", m.descricao)
                                put("valor", m.valor)
                                put("tipo", m.tipo)
                                put("categoria", m.categoria)
                            }
                        )
                    }
                    preferencias.edit()
                        .putString("movimentos", dados.toString())
                        .apply()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeuCofreApp(
    movimentosIniciais: List<Movimento>,
    salvar: (List<Movimento>) -> Unit
) {
    val movimentos = remember {
        mutableStateListOf<Movimento>().apply {
            addAll(movimentosIniciais)
        }
    }

    var aba by remember { mutableIntStateOf(0) }
    var mostrarCadastro by remember { mutableStateOf(false) }
    var descricao by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Despesa") }
    var categoria by remember { mutableStateOf("Outros") }

    val receitas = movimentos.filter { it.tipo == "Receita" }.sumOf { it.valor }
    val despesas = movimentos.filter { it.tipo == "Despesa" }.sumOf { it.valor }
    val saldo = receitas - despesas

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Verde,
            background = Fundo,
            surface = Cartao,
            onBackground = Texto,
            onSurface = Texto
        )
    ) {
        Scaffold(
            containerColor = Fundo,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "MEU COFRE",
                                fontWeight = FontWeight.Bold,
                                color = Verde
                            )
                            Text(
                                "Seu dinheiro sob controle",
                                style = MaterialTheme.typography.labelSmall,
                                color = Secundario
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Fundo
                    )
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Cartao) {
                    listOf("Início", "Extrato", "Orçamento", "Metas", "Ajustes")
                        .forEachIndexed { index, nome ->
                            NavigationBarItem(
                                selected = aba == index,
                                onClick = { aba = index },
                                icon = {
                                    Text(
                                        listOf("⌂", "≡", "▤", "☆", "⚙")[index]
                                    )
                                },
                                label = { Text(nome) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Verde,
                                    selectedTextColor = Verde,
                                    indicatorColor = Color(0xFF304735),
                                    unselectedTextColor = Secundario
                                )
                            )
                        }
                }
            },
            floatingActionButton = {
                if (aba == 0 || aba == 1) {
                    FloatingActionButton(
                        onClick = { mostrarCadastro = true },
                        containerColor = Verde,
                        contentColor = Fundo
                    ) {
                        Text("+", style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
        ) { espaco ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(espaco)
                    .padding(horizontal = 18.dp)
            ) {
                when (aba) {
                    0 -> {
                        Spacer(Modifier.height(12.dp))
                        Text("Visão geral", color = Secundario)
                        Spacer(Modifier.height(12.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Cartao),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(22.dp)) {
                                Text("Saldo registrado", color = Secundario)
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    dinheiro(saldo),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (saldo >= 0) Verde else Color(0xFFFF7777)
                                )
                                Spacer(Modifier.height(18.dp))
                                Row(Modifier.fillMaxWidth()) {
                                    Column(Modifier.weight(1f)) {
                                        Text("↑ Receitas", color = Verde)
                                        Text(
                                            dinheiro(receitas),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text("↓ Despesas", color = Color(0xFFFF8585))
                                        Text(
                                            dinheiro(despesas),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Últimas movimentações",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))

                        if (movimentos.isEmpty()) {
                            CartaoInfo(
                                "Seu cofre começa aqui",
                                "Toque no botão + para registrar sua primeira receita ou despesa."
                            )
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    movimentos.sortedByDescending { it.id }.take(5),
                                    key = { it.id }
                                ) { m ->
                                    LinhaMovimento(m)
                                }
                            }
                        }
                    }

                    1 -> {
                        Spacer(Modifier.height(16.dp))
                        Text("Extrato", style = MaterialTheme.typography.headlineMedium)
                        Text("Todas as suas movimentações", color = Secundario)
                        Spacer(Modifier.height(12.dp))

                        if (movimentos.isEmpty()) {
                            CartaoInfo("Nenhum lançamento", "Use o botão + para adicionar um.")
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(
                                    movimentos.sortedByDescending { it.id },
                                    key = { it.id }
                                ) { m ->
                                    LinhaMovimento(
                                        movimento = m,
                                        aoClicar = {
                                            movimentos.remove(m)
                                            salvar(movimentos.toList())
                                        }
                                    )
                                }
                            }
                            Text(
                                "Toque em um lançamento para excluí-lo.",
                                color = Secundario,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    2 -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Orçamento", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        CartaoInfo(
                            "Resumo financeiro",
                            "Receitas: ${dinheiro(receitas)}\nDespesas: ${dinheiro(despesas)}\nSaldo: ${dinheiro(saldo)}"
                        )
                        CartaoInfo(
                            "Próxima etapa",
                            "Vamos adicionar limites mensais por categoria e alertas de gastos."
                        )
                    }

                    3 -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Minhas metas", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        CartaoInfo(
                            "Reserva financeira",
                            "Meta inicial: R$ 5.000,00\nO acompanhamento de aportes será adicionado na próxima etapa."
                        )
                    }

                    else -> {
                        Spacer(Modifier.height(20.dp))
                        Text("Ajustes", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(12.dp))
                        CartaoInfo(
                            "Armazenamento local",
                            "Suas movimentações ficam salvas neste aparelho. Desinstalar o app ou limpar seus dados pode apagá-las. O backup será implementado depois."
                        )
                        CartaoInfo(
                            "Meu Cofre",
                            "Versão inicial • Controle financeiro pessoal"
                        )
                    }
                }
            }
        }

        if (mostrarCadastro) {
            AlertDialog(
                onDismissRequest = { mostrarCadastro = false },
                title = { Text("Nova movimentação") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = tipo == "Despesa",
                                onClick = { tipo = "Despesa" },
                                label = { Text("Despesa") }
                            )
                            FilterChip(
                                selected = tipo == "Receita",
                                onClick = { tipo = "Receita" },
                                label = { Text("Receita") }
                            )
                        }

                        OutlinedTextField(
                            value = descricao,
                            onValueChange = { descricao = it },
                            label = { Text("Descrição") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = valorTexto,
                            onValueChange = { valorTexto = it },
                            label = { Text("Valor em reais") },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Decimal
                            ),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = categoria,
                            onValueChange = { categoria = it },
                            label = { Text("Categoria") },
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val valor = valorTexto.trim()
                                .replace(".", "")
                                .replace(",", ".")
                                .toDoubleOrNull()

                            if (descricao.isNotBlank() &&
                                valor != null &&
                                valor > 0
                            ) {
                                movimentos.add(
                                    Movimento(
                                        id = System.currentTimeMillis(),
                                        descricao = descricao.trim(),
                                        valor = valor,
                                        tipo = tipo,
                                        categoria = categoria.ifBlank { "Outros" }
                                    )
                                )
                                salvar(movimentos.toList())
                                descricao = ""
                                valorTexto = ""
                                categoria = "Outros"
                                mostrarCadastro = false
                            }
                        }
                    ) {
                        Text("Salvar", color = Verde)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { mostrarCadastro = false }) {
                        Text("Cancelar")
                    }
                },
                containerColor = Cartao,
                titleContentColor = Texto,
                textContentColor = Texto
            )
        }
    }
}

@Composable
private fun LinhaMovimento(
    movimento: Movimento,
    aoClicar: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (aoClicar != null) Modifier.clickable { aoClicar() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Cartao)
    ) {
        Row(
            modifier = Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(movimento.descricao, fontWeight = FontWeight.SemiBold)
                Text(
                    "${movimento.categoria} • ${movimento.tipo}",
                    color = Secundario,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                (if (movimento.tipo == "Receita") "+ " else "− ") +
                    dinheiro(movimento.valor),
                color = if (movimento.tipo == "Receita") Verde
                    else Color(0xFFFF8585),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CartaoInfo(titulo: String, mensagem: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Cartao)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(titulo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(mensagem, color = Secundario)
        }
    }
}
