
package com.meucofre.app




import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private val Fundo = Color(0xFF101713)
private val Cartao = Color(0xFF1B2720)
private val Verde = Color(0xFF5BE39B)
private val Vermelho = Color(0xFFFF7777)
private val Texto = Color(0xFFF0F5F1)
private val TextoSecundario = Color(0xFF9BAAA0)
private val Azul = Color(0xFF79B8FF)
private val categorias = listOf(
    "Alimentação", "Moradia", "Transporte", "Saúde",
    "Lazer", "Educação", "Contas", "Salário",
    "Freelance", "Compras", "Outros"
)

private data class Movimento(
    val id: String = UUID.randomUUID().toString(),
    val descricao: String,
    val valor: Double,
    val tipo: String,
    val categoria: String,
    val data: String
)

private data class Meta(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val objetivo: Double,
    val guardado: Double = 0.0
)

private fun dinheiro(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun valorDigitado(texto: String): Double? {
    val limpo = texto.trim()
        .replace("R$", "")
        .replace(" ", "")
        .replace(".", "")
        .replace(",", ".")
    return limpo.toDoubleOrNull()?.takeIf { it > 0 }
}

private fun hoje(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

private fun mesAtual(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

private fun nomeMes(mes: String): String {
    return try {
        val data = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("$mes-01")
        SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
            .format(data ?: Date())
            .replaceFirstChar { it.uppercase(Locale("pt", "BR")) }
    } catch (_: Exception) {
        mes
    }
}

private fun mudarMes(mes: String, quantidade: Int): String {
    val calendario = Calendar.getInstance()
    try {
        calendario.time = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse("$mes-01") ?: Date()
    } catch (_: Exception) {
        calendario.time = Date()
    }
    calendario.add(Calendar.MONTH, quantidade)
    return SimpleDateFormat("yyyy-MM", Locale.US).format(calendario.time)
}

private fun dataCurta(data: String): String {
    return try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(data)
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(parsed ?: Date())
    } catch (_: Exception) {
        data
    }
}

private fun carregarMovimentos(context: Context): List<Movimento> {
    return try {
        val json = JSONArray(
            context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
                .getString("movimentos", "[]")
        )
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Movimento(
                id = o.optString("id", UUID.randomUUID().toString()),
                descricao = o.optString("descricao", ""),
                valor = o.optDouble("valor", 0.0),
                tipo = o.optString("tipo", "Despesa"),
                categoria = o.optString("categoria", "Outros"),
                data = o.optString("data", hoje())
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun salvarMovimentos(context: Context, lista: List<Movimento>) {
    val json = JSONArray()
    lista.forEach {
        json.put(JSONObject().apply {
            put("id", it.id)
            put("descricao", it.descricao)
            put("valor", it.valor)
            put("tipo", it.tipo)
            put("categoria", it.categoria)
            put("data", it.data)
        })
    }
    context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
        .edit().putString("movimentos", json.toString()).apply()
}

private fun carregarMetas(context: Context): List<Meta> {
    return try {
        val json = JSONArray(
            context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
                .getString("metas", "[]")
        )
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Meta(
                id = o.optString("id", UUID.randomUUID().toString()),
                nome = o.optString("nome", "Minha meta"),
                objetivo = o.optDouble("objetivo", 0.0),
                guardado = o.optDouble("guardado", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun salvarMetas(context: Context, lista: List<Meta>) {
    val json = JSONArray()
    lista.forEach {
        json.put(JSONObject().apply {
            put("id", it.id)
            put("nome", it.nome)
            put("objetivo", it.objetivo)
            put("guardado", it.guardado)
        })
    }
    context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
        .edit().putString("metas", json.toString()).apply()
}

private fun carregarOrcamentos(context: Context): Map<String, Double> {
    return try {
        val obj = JSONObject(
            context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
                .getString("orcamentos", "{}")
        )
        obj.keys().asSequence().associateWith { obj.optDouble(it, 0.0) }
    } catch (_: Exception) {
        emptyMap()
    }
}

private fun salvarOrcamentos(context: Context, mapa: Map<String, Double>) {
    val obj = JSONObject()
    mapa.forEach { (mes, valor) -> obj.put(mes, valor) }
    context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)
        .edit().putString("orcamentos", obj.toString()).apply()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Verde,
                    background = Fundo,
                    surface = Cartao,
                    onBackground = Texto,
                    onSurface = Texto
                )
            ) {
                MeuCofreApp()
            }
        }
    }
}

@Composable
private fun MeuCofreApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var movimentos by remember { mutableStateOf(carregarMovimentos(context)) }
    var metas by remember { mutableStateOf(carregarMetas(context)) }
    var orcamentos by remember { mutableStateOf(carregarOrcamentos(context)) }

    var mes by remember { mutableStateOf(mesAtual()) }
    var aba by remember { mutableStateOf("Início") }

    var mostrarMovimento by remember { mutableStateOf(false) }
    var mostrarOrcamento by remember { mutableStateOf(false) }
    var mostrarMeta by remember { mutableStateOf(false) }
    var metaSelecionada by remember { mutableStateOf<String?>(null) }
    var excluirMovimento by remember { mutableStateOf<Movimento?>(null) }
    var excluirMeta by remember { mutableStateOf<Meta?>(null) }

    val doMes = movimentos.filter { it.data.startsWith(mes) }
    val receitas = doMes.filter { it.tipo == "Receita" }.sumOf { it.valor }
    val despesas = doMes.filter { it.tipo == "Despesa" }.sumOf { it.valor }
    val saldo = receitas - despesas
    val orcamento = orcamentos[mes] ?: 0.0
    val restante = orcamento - despesas
    val progressoOrcamento =
        if (orcamento > 0) (despesas / orcamento).toFloat().coerceIn(0f, 1f) else 0f

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Fundo
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cartao)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("MEU COFRE", color = Verde, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Seu dinheiro, sob controle.", color = Texto, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text("●", color = Verde, fontSize = 22.sp)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { mes = mudarMes(mes, -1) }) {
                    Text("‹ Anterior", color = Verde)
                }
                Text(nomeMes(mes), color = Texto, fontWeight = FontWeight.Bold)
                TextButton(
                    onClick = { mes = mudarMes(mes, 1) },
                    enabled = mes < mesAtual()
                ) {
                    Text("Próximo ›", color = if (mes < mesAtual()) Verde else TextoSecundario)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BotaoAcao("+ Receita", Modifier.weight(1f), Verde) {
                    mostrarMovimento = true
                }
                BotaoAcao("+ Despesa", Modifier.weight(1f), Vermelho) {
                    mostrarMovimento = true
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                when (aba) {
                    "Início" -> Inicio(
                        mes = mes,
                        receitas = receitas,
                        despesas = despesas,
                        saldo = saldo,
                        orcamento = orcamento,
                        restante = restante,
                        progresso = progressoOrcamento,
                        movimentos = doMes.sortedByDescending { it.data }.take(5),
                        aoAdicionarMovimento = { mostrarMovimento = true },
                        aoEditarOrcamento = { mostrarOrcamento = true },
                        aoVerExtrato = { aba = "Extrato" },
                        aoExcluirMovimento = { excluirMovimento = it }
                    )
                    "Extrato" -> Extrato(
                        movimentos = doMes.sortedByDescending { it.data },
                        aoAdicionar = { mostrarMovimento = true },
                        aoExcluir = { excluirMovimento = it }
                    )
                    "Relatórios" -> Relatorios(
                        receitas = receitas,
                        despesas = despesas,
                        movimentos = doMes,
                        mes = mes
                    )
                    "Metas" -> TelaMetas(
                        metas = metas,
                        aoAdicionar = { mostrarMeta = true },
                        aoContribuir = { metaSelecionada = it.id },
                        aoExcluir = { excluirMeta = it }
                    )
                    else -> Inicio(
                        mes, receitas, despesas, saldo, orcamento, restante,
                        progressoOrcamento, doMes.take(5),
                        { mostrarMovimento = true },
                        { mostrarOrcamento = true },
                        { aba = "Extrato" },
                        { excluirMovimento = it }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Cartao)
                    .navigationBarsPadding()
                    .padding(vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("Início", "Extrato", "Relatórios", "Metas").forEach { item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { aba = item }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            when (item) {
                                "Início" -> "⌂"
                                "Extrato" -> "☷"
                                "Relatórios" -> "▥"
                                else -> "☆"
                            },
                            color = if (aba == item) Verde else TextoSecundario,
                            fontSize = 21.sp
                        )
                        Text(
                            item,
                            color = if (aba == item) Verde else TextoSecundario,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }

    if (mostrarMovimento) {
        DialogoMovimento(
            aoFechar = { mostrarMovimento = false },
            aoSalvar = { descricao, valor, tipo, categoria ->
                val novo = Movimento(
                    descricao = descricao,
                    valor = valor,
                    tipo = tipo,
                    categoria = categoria,
                    data = hoje()
                )
                movimentos = movimentos + novo
                salvarMovimentos(context, movimentos)
                mes = novo.data.substring(0, 7)
                mostrarMovimento = false
            }
        )
    }

    if (mostrarOrcamento) {
        DialogoOrcamento(
            valorInicial = if (orcamento > 0) orcamento.toString() else "",
            mes = mes,
            aoFechar = { mostrarOrcamento = false },
            aoSalvar = { valor ->
                orcamentos = orcamentos + (mes to valor)
                salvarOrcamentos(context, orcamentos)
                mostrarOrcamento = false
            }
        )
    }

    if (mostrarMeta) {
        DialogoMeta(
            aoFechar = { mostrarMeta = false },
            aoSalvar = { nome, objetivo ->
                metas = metas + Meta(nome = nome, objetivo = objetivo)
                salvarMetas(context, metas)
                mostrarMeta = false
            }
        )
    }

    val metaParaContribuir = metas.firstOrNull { it.id == metaSelecionada }
    if (metaParaContribuir != null) {
        DialogoContribuicao(
            meta = metaParaContribuir,
            aoFechar = { metaSelecionada = null },
            aoSalvar = { valor ->
                metas = metas.map {
                    if (it.id == metaParaContribuir.id) {
                        it.copy(guardado = it.guardado + valor)
                    } else it
                }
                salvarMetas(context, metas)
                metaSelecionada = null
            }
        )
    }

    if (excluirMovimento != null) {
        AlertDialog(
            onDismissRequest = { excluirMovimento = null },
            title = { Text("Excluir lançamento?") },
            text = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = {
                    movimentos = movimentos.filterNot { it.id == excluirMovimento?.id }
                    salvarMovimentos(context, movimentos)
                    excluirMovimento = null
                }) { Text("Excluir", color = Vermelho) }
            },
            dismissButton = {
                TextButton(onClick = { excluirMovimento = null }) { Text("Cancelar") }
            },
            containerColor = Cartao
        )
    }

    if (excluirMeta != null) {
        AlertDialog(
            onDismissRequest = { excluirMeta = null },
            title = { Text("Excluir meta?") },
            text = { Text("O objetivo e o valor guardado serão removidos.") },
            confirmButton = {
                TextButton(onClick = {
                    metas = metas.filterNot { it.id == excluirMeta?.id }
                    salvarMetas(context, metas)
                    excluirMeta = null
                }) { Text("Excluir", color = Vermelho) }
            },
            dismissButton = {
                TextButton(onClick = { excluirMeta = null }) { Text("Cancelar") }
            },
            containerColor = Cartao
        )
    }
}

@Composable
private fun BotaoAcao(
    texto: String,
    modifier: Modifier = Modifier,
    cor: Color,
    aoClicar: () -> Unit
) {
    Button(
        onClick = aoClicar,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Cartao,
            contentColor = cor
        )
    ) {
        Text(texto, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Inicio(
    mes: String,
    receitas: Double,
    despesas: Double,
    saldo: Double,
    orcamento: Double,
    restante: Double,
    progresso: Float,
    movimentos: List<Movimento>,
    aoAdicionarMovimento: () -> Unit,
    aoEditarOrcamento: () -> Unit,
    aoVerExtrato: () -> Unit,
    aoExcluirMovimento: (Movimento) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Cartao),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("SALDO DO MÊS", color = TextoSecundario, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    dinheiro(saldo),
                    color = if (saldo >= 0) Verde else Vermelho,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(nomeMes(mes), color = TextoSecundario, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumoMini("Entradas", receitas, Verde, Modifier.weight(1f))
                    ResumoMini("Saídas", despesas, Vermelho, Modifier.weight(1f))
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Cartao),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Orçamento mensal", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (orcamento > 0) "Limite: ${dinheiro(orcamento)}"
                            else "Defina um limite para suas despesas",
                            color = TextoSecundario,
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = aoEditarOrcamento) {
                        Text(if (orcamento > 0) "Editar" else "Definir", color = Verde)
                    }
                }
                if (orcamento > 0) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { progresso },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (despesas > orcamento) Vermelho else Verde,
                        trackColor = Fundo
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Gasto: ${dinheiro(despesas)}", color = TextoSecundario, fontSize = 12.sp)
                        Text(
                            if (restante >= 0) "Restam ${dinheiro(restante)}"
                            else "Excedido ${dinheiro(-restante)}",
                            color = if (restante >= 0) Verde else Vermelho,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Últimos lançamentos", fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = aoVerExtrato) { Text("Ver todos", color = Verde) }
        }

        if (movimentos.isEmpty()) {
            CaixaVazia(
                titulo = "Nenhum lançamento neste mês",
                descricao = "Adicione uma receita ou despesa para começar."
            )
        } else {
            movimentos.forEach {
                ItemMovimento(it, aoExcluirMovimento)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun ResumoMini(
    titulo: String,
    valor: Double,
    cor: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier.background(Fundo, RoundedCornerShape(14.dp)).padding(12.dp)) {
        Text(titulo, color = TextoSecundario, fontSize = 12.sp)
        Spacer(Modifier.height(5.dp))
        Text(dinheiro(valor), color = cor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun Extrato(
    movimentos: List<Movimento>,
    aoAdicionar: () -> Unit,
    aoExcluir: (Movimento) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Extrato", fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(onClick = aoAdicionar, colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)) {
                Text("+ Adicionar")
            }
        }
        Spacer(Modifier.height(12.dp))
        if (movimentos.isEmpty()) {
            CaixaVazia("Seu extrato está vazio", "Os lançamentos do mês escolhido aparecerão aqui.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(movimentos, key = { it.id }) { movimento ->
                    ItemMovimento(movimento, aoExcluir)
                }
            }
        }
    }
}

@Composable
private fun ItemMovimento(
    movimento: Movimento,
    aoExcluir: (Movimento) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cartao),
        shape = RoundedCornerShape(15.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).background(Fundo, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (movimento.tipo == "Receita") "↗" else "↘",
                    color = if (movimento.tipo == "Receita") Verde else Vermelho,
                    fontSize = 22.sp
                )
            }
            Spacer(Modifier.width(11.dp))
            Column(Modifier.weight(1f)) {
                Text(movimento.descricao, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${movimento.categoria} • ${dataCurta(movimento.data)}",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (movimento.tipo == "Receita") "+ " else "- ") + dinheiro(movimento.valor),
                    color = if (movimento.tipo == "Receita") Verde else Vermelho,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                TextButton(
                    onClick = { aoExcluir(movimento) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) { Text("Excluir", color = TextoSecundario, fontSize = 10.sp) }
            }
        }
    }
}

@Composable
private fun Relatorios(
    receitas: Double,
    despesas: Double,
    movimentos: List<Movimento>,
    mes: String
) {
    val despesasCategoria = movimentos
        .filter { it.tipo == "Despesa" }
        .groupBy { it.categoria }
        .mapValues { entry -> entry.value.sumOf { it.valor } }
        .toList()
        .sortedByDescending { it.second }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Relatórios", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(nomeMes(mes), color = TextoSecundario)

        Card(colors = CardDefaults.cardColors(containerColor = Cartao)) {
            Column(Modifier.padding(18.dp)) {
                Text("Receitas x despesas", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(18.dp))
                GraficoBarras(receitas, despesas)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Legenda("Receitas", Verde, Modifier.weight(1f))
                    Legenda("Despesas", Vermelho, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                LinhaRelatorio("Total de receitas", dinheiro(receitas), Verde)
                LinhaRelatorio("Total de despesas", dinheiro(despesas), Vermelho)
                LinhaRelatorio("Resultado do mês", dinheiro(receitas - despesas), if (receitas >= despesas) Verde else Vermelho)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Cartao)) {
            Column(Modifier.padding(18.dp)) {
                Text("Despesas por categoria", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(14.dp))
                if (despesasCategoria.isEmpty()) {
                    Text("Registre despesas para ver a distribuição por categoria.", color = TextoSecundario, fontSize = 13.sp)
                } else {
                    val maior = max(1.0, despesasCategoria.maxOf { it.second })
                    despesasCategoria.forEach { (categoria, valor) ->
                        Column(Modifier.padding(vertical = 7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(categoria, color = Texto, modifier = Modifier.weight(1f), fontSize = 13.sp)
                                Text(dinheiro(valor), color = TextoSecundario, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { (valor / maior).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(7.dp),
                                color = Azul,
                                trackColor = Fundo
                            )
                            Text(
                                "${if (despesas > 0) (valor / despesas * 100).toInt() else 0}% das despesas",
                                color = TextoSecundario,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraficoBarras(receitas: Double, despesas: Double) {
    val maior = max(1.0, max(receitas, despesas))
    Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
        val largura = size.width
        val base = size.height - 8.dp.toPx()
        val maxAltura = size.height - 24.dp.toPx()
        val barWidth = 64.dp.toPx()
        val x1 = largura * 0.28f - barWidth / 2
        val x2 = largura * 0.72f - barWidth / 2
        val h1 = maxAltura * (receitas / maior).toFloat()
        val h2 = maxAltura * (despesas / maior).toFloat()
        drawLine(
            color = TextoSecundario.copy(alpha = 0.3f),
            start = Offset(0f, base),
            end = Offset(largura, base),
            strokeWidth = 1.dp.toPx()
        )
        drawRoundRect(
            color = Verde,
            topLeft = Offset(x1, base - h1),
            size = Size(barWidth, h1.coerceAtLeast(3.dp.toPx())),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
        )
        drawRoundRect(
            color = Vermelho,
            topLeft = Offset(x2, base - h2),
            size = Size(barWidth, h2.coerceAtLeast(3.dp.toPx())),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx())
        )
    }
}

@Composable
private fun Legenda(texto: String, cor: Color, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(9.dp).background(cor, RoundedCornerShape(3.dp)))
        Spacer(Modifier.width(7.dp))
        Text(texto, color = TextoSecundario, fontSize = 12.sp)
    }
}

@Composable
private fun LinhaRelatorio(nome: String, valor: String, cor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nome, color = TextoSecundario, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(valor, color = cor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TelaMetas(
    metas: List<Meta>,
    aoAdicionar: () -> Unit,
    aoContribuir: (Meta) -> Unit,
    aoExcluir: (Meta) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Minhas metas", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Planeje o que quer conquistar.", color = TextoSecundario, fontSize = 12.sp)
            }
            Button(
                onClick = aoAdicionar,
                colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
            ) { Text("+ Criar") }
        }
        Spacer(Modifier.height(14.dp))
        if (metas.isEmpty()) {
            CaixaVazia("Você ainda não tem metas", "Crie uma meta para acompanhar seu progresso financeiro.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(metas, key = { it.id }) { meta ->
                    val progresso = if (meta.objetivo > 0)
                        (meta.guardado / meta.objetivo).toFloat().coerceIn(0f, 1f) else 0f
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Cartao),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(meta.nome, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Spacer(Modifier.height(7.dp))
                            Text(
                                "${dinheiro(meta.guardado)} de ${dinheiro(meta.objetivo)}",
                                color = TextoSecundario,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { progresso },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = Verde,
                                trackColor = Fundo
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("${(progresso * 100).toInt()}% concluído", color = Verde, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { aoContribuir(meta) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo),
                                    modifier = Modifier.weight(1f)
                                ) { Text("Guardar dinheiro") }
                                OutlinedButton(
                                    onClick = { aoExcluir(meta) },
                                    modifier = Modifier.weight(1f)
                                ) { Text("Excluir", color = Vermelho) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CaixaVazia(titulo: String, descricao: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cartao),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("◇", color = Verde, fontSize = 34.sp)
            Spacer(Modifier.height(8.dp))
            Text(titulo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(descricao, color = TextoSecundario, fontSize = 12.sp)
        }
    }
}

@Composable
private fun DialogoMovimento(
    aoFechar: () -> Unit,
    aoSalvar: (String, Double, String, String) -> Unit
) {
    var descricao by remember { mutableStateOf("") }
    var valorTexto by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Despesa") }
    var categoria by remember { mutableStateOf("Alimentação") }
    var categoriaExpandida by remember { mutableStateOf(false) }
    val valor = valorDigitado(valorTexto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Novo lançamento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Despesa", "Receita").forEach { opcao ->
                        FilterChip(
                            selected = tipo == opcao,
                            onClick = {
                                tipo = opcao
                                categoria = if (opcao == "Receita") "Salário" else "Alimentação"
                            },
                            label = { Text(opcao) }
                        )
                    }
                }
                OutlinedTextField(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = { Text("Descrição") },
                    placeholder = { Text("Ex.: mercado, salário...") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = valorTexto,
                    onValueChange = { valorTexto = it },
                    label = { Text("Valor (R$)") },
                    placeholder = { Text("Ex.: 150,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Box {
                    OutlinedButton(
                        onClick = { categoriaExpandida = true },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Categoria: $categoria") }
                    DropdownMenu(
                        expanded = categoriaExpandida,
                        onDismissRequest = { categoriaExpandida = false }
                    ) {
                        categorias.filter {
                            tipo != "Receita" || it in listOf("Salário", "Freelance", "Outros")
                        }.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                onClick = {
                                    categoria = item
                                    categoriaExpandida = false
                                }
                            )
                        }
                    }
                }
                Text("A data será preenchida automaticamente com o dia de hoje.", color = TextoSecundario, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (descricao.isNotBlank() && valor != null) {
                        aoSalvar(descricao.trim(), valor, tipo, categoria)
                    }
                },
                enabled = descricao.isNotBlank() && valor != null
            ) { Text("Salvar", color = Verde) }
        },
        dismissButton = {
            TextButton(onClick = aoFechar) { Text("Cancelar") }
        },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoOrcamento(
    valorInicial: String,
    mes: String,
    aoFechar: () -> Unit,
    aoSalvar: (Double) -> Unit
) {
    var texto by remember { mutableStateOf(valorInicial) }
    val valor = valorDigitado(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Orçamento mensal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(nomeMes(mes), color = TextoSecundario)
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Limite de despesas (R$)") },
                    placeholder = { Text("Ex.: 2000,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                Text("O orçamento considera as despesas registradas neste mês.", color = TextoSecundario, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valor != null) aoSalvar(valor) }, enabled = valor != null) {
                Text("Salvar", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoMeta(
    aoFechar: () -> Unit,
    aoSalvar: (String, Double) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    val valor = valorDigitado(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Criar meta financeira") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome da meta") },
                    placeholder = { Text("Ex.: comprar uma scooter") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Valor desejado (R$)") },
                    placeholder = { Text("Ex.: 3000,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (nome.isNotBlank() && valor != null) aoSalvar(nome.trim(), valor) },
                enabled = nome.isNotBlank() && valor != null
            ) { Text("Criar meta", color = Verde) }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoContribuicao(
    meta: Meta,
    aoFechar: () -> Unit,
    aoSalvar: (Double) -> Unit
) {
    var texto by remember { mutableStateOf("") }
    val valor = valorDigitado(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Guardar para ${meta.nome}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Já guardado: ${dinheiro(meta.guardado)}", color = TextoSecundario)
                Text("Falta: ${dinheiro((meta.objetivo - meta.guardado).coerceAtLeast(0.0))}", color = Verde)
                OutlinedTextField(
                    value = texto,
                    onValueChange = { texto = it },
                    label = { Text("Quanto deseja adicionar? (R$)") },
                    placeholder = { Text("Ex.: 100,00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (valor != null) aoSalvar(valor) }, enabled = valor != null) {
                Text("Adicionar", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}


