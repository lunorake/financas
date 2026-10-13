
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.max

private val Fundo = Color(0xFF0B120E)
private val Painel = Color(0xFF141F18)
private val Cartao = Color(0xFF1B2A20)
private val Verde = Color(0xFF70E5A0)
private val VerdeEscuro = Color(0xFF294D37)
private val Vermelho = Color(0xFFFF8585)
private val Azul = Color(0xFF83BDFF)
private val Texto = Color(0xFFF0F5F1)
private val TextoSecundario = Color(0xFF9EADA2)

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
    val data: String,
    val conta: String = "Principal",
    val destino: String = ""
)

private data class Conta(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val tipo: String,
    val inicial: Double = 0.0
)

private data class Recorrencia(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val valor: Double,
    val dia: Int,
    val categoria: String,
    val conta: String,
    val pagos: List<String> = emptyList()
)

private data class Meta(
    val id: String = UUID.randomUUID().toString(),
    val nome: String,
    val objetivo: Double,
    val guardado: Double = 0.0
)

private fun dinheiro(valor: Double): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(valor)

private fun lerValor(texto: String): Double? {
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

private fun nomeMes(mes: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse("$mes-01")
    SimpleDateFormat("MMMM 'de' yyyy", Locale("pt", "BR"))
        .format(d ?: Date()).replaceFirstChar { it.uppercase() }
} catch (_: Exception) {
    mes
}

private fun mudarMes(mes: String, quantidade: Int): String {
    val c = Calendar.getInstance()
    try {
        c.time = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            .parse("$mes-01") ?: Date()
    } catch (_: Exception) {
        c.time = Date()
    }
    c.add(Calendar.MONTH, quantidade)
    return SimpleDateFormat("yyyy-MM", Locale.US).format(c.time)
}

private fun dataCurta(data: String): String = try {
    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(data)
    SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(d ?: Date())
} catch (_: Exception) {
    data
}

private fun hashPin(pin: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(pin.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

private fun prefs(context: Context) =
    context.getSharedPreferences("meu_cofre", Context.MODE_PRIVATE)

private fun carregarMovimentos(context: Context): List<Movimento> = try {
    val json = JSONArray(prefs(context).getString("movimentos", "[]"))
    (0 until json.length()).map { i ->
        val o = json.getJSONObject(i)
        Movimento(
            id = o.optString("id", UUID.randomUUID().toString()),
            descricao = o.optString("descricao", ""),
            valor = o.optDouble("valor", 0.0),
            tipo = o.optString("tipo", "Despesa"),
            categoria = o.optString("categoria", "Outros"),
            data = o.optString("data", hoje()),
            conta = o.optString("conta", "Principal"),
            destino = o.optString("destino", "")
        )
    }
} catch (_: Exception) {
    emptyList()
}

private fun salvarMovimentos(context: Context, lista: List<Movimento>) {
    val json = JSONArray()
    lista.forEach { m ->
        json.put(JSONObject().apply {
            put("id", m.id)
            put("descricao", m.descricao)
            put("valor", m.valor)
            put("tipo", m.tipo)
            put("categoria", m.categoria)
            put("data", m.data)
            put("conta", m.conta)
            put("destino", m.destino)
        })
    }
    prefs(context).edit().putString("movimentos", json.toString()).apply()
}

private fun carregarContas(context: Context): List<Conta> {
    val lista = try {
        val json = JSONArray(prefs(context).getString("contas", "[]"))
        (0 until json.length()).map { i ->
            val o = json.getJSONObject(i)
            Conta(
                id = o.optString("id", UUID.randomUUID().toString()),
                nome = o.optString("nome", "Principal"),
                tipo = o.optString("tipo", "Conta"),
                inicial = o.optDouble("inicial", 0.0)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    return if (lista.isEmpty()) {
        listOf(Conta(nome = "Principal", tipo = "Conta"))
    } else {
        lista
    }
}

private fun salvarContas(context: Context, lista: List<Conta>) {
    val json = JSONArray()
    lista.forEach { c ->
        json.put(JSONObject().apply {
            put("id", c.id)
            put("nome", c.nome)
            put("tipo", c.tipo)
            put("inicial", c.inicial)
        })
    }
    prefs(context).edit().putString("contas", json.toString()).apply()
}

private fun carregarRecorrencias(context: Context): List<Recorrencia> = try {
    val json = JSONArray(prefs(context).getString("recorrencias", "[]"))
    (0 until json.length()).map { i ->
        val o = json.getJSONObject(i)
        val pagosJson = o.optJSONArray("pagos") ?: JSONArray()
        Recorrencia(
            id = o.optString("id", UUID.randomUUID().toString()),
            nome = o.optString("nome", ""),
            valor = o.optDouble("valor", 0.0),
            dia = o.optInt("dia", 1).coerceIn(1, 31),
            categoria = o.optString("categoria", "Contas"),
            conta = o.optString("conta", "Principal"),
            pagos = (0 until pagosJson.length()).map { pagosJson.optString(it) }
        )
    }
} catch (_: Exception) {
    emptyList()
}

private fun salvarRecorrencias(context: Context, lista: List<Recorrencia>) {
    val json = JSONArray()
    lista.forEach { r ->
        json.put(JSONObject().apply {
            put("id", r.id)
            put("nome", r.nome)
            put("valor", r.valor)
            put("dia", r.dia)
            put("categoria", r.categoria)
            put("conta", r.conta)
            put("pagos", JSONArray(r.pagos))
        })
    }
    prefs(context).edit().putString("recorrencias", json.toString()).apply()
}

private fun carregarMetas(context: Context): List<Meta> = try {
    val json = JSONArray(prefs(context).getString("metas", "[]"))
    (0 until json.length()).map { i ->
        val o = json.getJSONObject(i)
        Meta(
            id = o.optString("id", UUID.randomUUID().toString()),
            nome = o.optString("nome", "Meta"),
            objetivo = o.optDouble("objetivo", 0.0),
            guardado = o.optDouble("guardado", 0.0)
        )
    }
} catch (_: Exception) {
    emptyList()
}

private fun salvarMetas(context: Context, lista: List<Meta>) {
    val json = JSONArray()
    lista.forEach { m ->
        json.put(JSONObject().apply {
            put("id", m.id)
            put("nome", m.nome)
            put("objetivo", m.objetivo)
            put("guardado", m.guardado)
        })
    }
    prefs(context).edit().putString("metas", json.toString()).apply()
}

private fun carregarOrcamentos(context: Context): Map<String, Double> = try {
    val o = JSONObject(prefs(context).getString("orcamentos", "{}"))
    o.keys().asSequence().associateWith { o.optDouble(it, 0.0) }
} catch (_: Exception) {
    emptyMap()
}

private fun salvarOrcamentos(context: Context, mapa: Map<String, Double>) {
    val o = JSONObject()
    mapa.forEach { (k, v) -> o.put(k, v) }
    prefs(context).edit().putString("orcamentos", o.toString()).apply()
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
    var contas by remember { mutableStateOf(carregarContas(context)) }
    var recorrencias by remember { mutableStateOf(carregarRecorrencias(context)) }
    var metas by remember { mutableStateOf(carregarMetas(context)) }
    var orcamentos by remember { mutableStateOf(carregarOrcamentos(context)) }

    var mes by remember { mutableStateOf(mesAtual()) }
    var aba by remember { mutableStateOf("Início") }
    var ocultarSaldo by remember {
        mutableStateOf(prefs(context).getBoolean("ocultar_saldo", false))
    }
    var pinHash by remember {
        mutableStateOf(prefs(context).getString("pin_hash", null))
    }
    var desbloqueado by remember { mutableStateOf(pinHash == null) }

    var mostrarMovimento by remember { mutableStateOf(false) }
    var tipoInicialMovimento by remember { mutableStateOf("Despesa") }
    var mostrarConta by remember { mutableStateOf(false) }
    var mostrarTransferencia by remember { mutableStateOf(false) }
    var mostrarRecorrencia by remember { mutableStateOf(false) }
    var mostrarMeta by remember { mutableStateOf(false) }
    var mostrarOrcamento by remember { mutableStateOf(false) }
    var mostrarSeguranca by remember { mutableStateOf(false) }

    var metaContribuir by remember { mutableStateOf<String?>(null) }
    var movimentoExcluir by remember { mutableStateOf<Movimento?>(null) }
    var metaExcluir by remember { mutableStateOf<Meta?>(null) }
    var contaExcluir by remember { mutableStateOf<Conta?>(null) }
    var recorrenciaExcluir by remember { mutableStateOf<Recorrencia?>(null) }

    fun atualizarMovimentos(novos: List<Movimento>) {
        movimentos = novos
        salvarMovimentos(context, novos)
    }

    fun atualizarContas(novas: List<Conta>) {
        contas = novas
        salvarContas(context, novas)
    }

    fun atualizarRecorrencias(novas: List<Recorrencia>) {
        recorrencias = novas
        salvarRecorrencias(context, novas)
    }

    fun atualizarMetas(novas: List<Meta>) {
        metas = novas
        salvarMetas(context, novas)
    }

    val movimentosMes = movimentos.filter { it.data.startsWith(mes) }
    val receitas = movimentosMes.filter { it.tipo == "Receita" }.sumOf { it.valor }
    val despesas = movimentosMes.filter { it.tipo == "Despesa" }.sumOf { it.valor }
    val saldoMes = receitas - despesas
    val orcamento = orcamentos[mes] ?: 0.0
    val progressoOrcamento = if (orcamento > 0) {
        (despesas / orcamento).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }

    val saldosContas = contas.associate { conta ->
        val saldo = conta.inicial +
            movimentos.filter { it.conta == conta.nome }.sumOf { m ->
                when (m.tipo) {
                    "Receita" -> m.valor
                    "Despesa" -> -m.valor
                    "Transferência" -> -m.valor
                    else -> 0.0
                }
            } +
            movimentos.filter {
                it.tipo == "Transferência" && it.destino == conta.nome
            }.sumOf { it.valor }

        conta.nome to saldo
    }

    val saldoGeral = saldosContas.values.sum()

    Surface(Modifier.fillMaxSize(), color = Fundo) {
        if (!desbloqueado && pinHash != null) {
            TelaBloqueio { pin ->
                if (hashPin(pin) == pinHash) desbloqueado = true
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().background(Painel)
                        .padding(horizontal = 18.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(43.dp).background(VerdeEscuro, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("M", color = Verde, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    }

                    Spacer(Modifier.width(11.dp))

                    Column(Modifier.weight(1f)) {
                        Text(
                            "MEU COFRE",
                            color = Verde,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Sua vida financeira.",
                            color = Texto,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(onClick = {
                        ocultarSaldo = !ocultarSaldo
                        prefs(context).edit().putBoolean("ocultar_saldo", ocultarSaldo).apply()
                    }) {
                        Text(if (ocultarSaldo) "◉" else "◎", color = Verde, fontSize = 22.sp)
                    }

                    TextButton(onClick = { mostrarSeguranca = true }) {
                        Text("⚙", color = TextoSecundario, fontSize = 21.sp)
                    }
                }

                if (aba == "Início" || aba == "Movimentos" || aba == "Relatórios") {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = { mes = mudarMes(mes, -1) }) {
                            Text("‹", color = Verde, fontSize = 22.sp)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "VISÃO MENSAL",
                                color = TextoSecundario,
                                fontSize = 9.sp,
                                letterSpacing = 1.5.sp
                            )
                            Text(nomeMes(mes), color = Texto, fontWeight = FontWeight.Bold)
                        }

                        TextButton(
                            onClick = { if (mes < mesAtual()) mes = mudarMes(mes, 1) },
                            enabled = mes < mesAtual()
                        ) {
                            Text(
                                "›",
                                color = if (mes < mesAtual()) Verde else TextoSecundario,
                                fontSize = 22.sp
                            )
                        }
                    }
                }

                Box(Modifier.weight(1f)) {
                    when (aba) {
                        "Início" -> TelaInicio(
                            saldoGeral = saldoGeral,
                            saldoMes = saldoMes,
                            receitas = receitas,
                            despesas = despesas,
                            movimentos = movimentosMes,
                            orcamento = orcamento,
                            progressoOrcamento = progressoOrcamento,
                            ocultar = ocultarSaldo,
                            onReceita = {
                                tipoInicialMovimento = "Receita"
                                mostrarMovimento = true
                            },
                            onDespesa = {
                                tipoInicialMovimento = "Despesa"
                                mostrarMovimento = true
                            },
                            onTransferir = { mostrarTransferencia = true },
                            onOrcamento = { mostrarOrcamento = true },
                            onExtrato = { aba = "Movimentos" },
                            onExcluir = { movimentoExcluir = it }
                        )

                        "Movimentos" -> TelaMovimentos(
                            movimentosMes.sortedByDescending { it.data },
                            ocultarSaldo,
                            onAdicionar = {
                                tipoInicialMovimento = "Despesa"
                                mostrarMovimento = true
                            },
                            onExcluir = { movimentoExcluir = it }
                        )

                        "Relatórios" -> TelaRelatorios(
                            receitas, despesas, movimentosMes, ocultarSaldo
                        )

                        "Contas" -> TelaContas(
                            contas, saldosContas, ocultarSaldo,
                            onAdicionar = { mostrarConta = true },
                            onTransferir = { mostrarTransferencia = true },
                            onExcluir = { contaExcluir = it }
                        )

                        "Fixas" -> TelaRecorrencias(
                            recorrencias, mes, ocultarSaldo,
                            onAdicionar = { mostrarRecorrencia = true },
                            onPagar = { r ->
                                if (mes !in r.pagos) {
                                    val dataPagamento = if (mes == mesAtual()) hoje() else "$mes-01"
                                    val novo = Movimento(
                                        descricao = r.nome,
                                        valor = r.valor,
                                        tipo = "Despesa",
                                        categoria = r.categoria,
                                        data = dataPagamento,
                                        conta = r.conta
                                    )
                                    atualizarMovimentos(movimentos + novo)
                                    atualizarRecorrencias(
                                        recorrencias.map {
                                            if (it.id == r.id) it.copy(pagos = it.pagos + mes) else it
                                        }
                                    )
                                }
                            },
                            onExcluir = { recorrenciaExcluir = it }
                        )

                        "Metas" -> TelaMetas(
                            metas, ocultarSaldo,
                            onAdicionar = { mostrarMeta = true },
                            onContribuir = { metaContribuir = it.id },
                            onExcluir = { metaExcluir = it }
                        )
                    }
                }

                Row(
                    Modifier.fillMaxWidth().background(Painel)
                        .navigationBarsPadding().padding(vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Início", "Movimentos", "Contas", "Fixas", "Relatórios", "Metas")
                        .forEach { item ->
                            Column(
                                Modifier.weight(1f).clickable { aba = item }.padding(vertical = 7.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    when (item) {
                                        "Início" -> "⌂"
                                        "Movimentos" -> "↕"
                                        "Contas" -> "▣"
                                        "Fixas" -> "◷"
                                        "Relatórios" -> "▥"
                                        else -> "☆"
                                    },
                                    color = if (aba == item) Verde else TextoSecundario,
                                    fontSize = 19.sp
                                )
                                Text(
                                    when (item) {
                                        "Movimentos" -> "Extrato"
                                        "Fixas" -> "Fixas"
                                        else -> item
                                    },
                                    color = if (aba == item) Verde else TextoSecundario,
                                    fontSize = 9.sp,
                                    maxLines = 1
                                )
                            }
                        }
                }
            }
        }
    }

    if (mostrarMovimento) {
        DialogoLancamento(
            contas = contas.map { it.nome },
            tipoInicial = tipoInicialMovimento,
            aoFechar = { mostrarMovimento = false },
            aoSalvar = { descricao, valor, tipo, categoria, conta ->
                atualizarMovimentos(
                    movimentos + Movimento(
                        descricao = descricao,
                        valor = valor,
                        tipo = tipo,
                        categoria = categoria,
                        data = hoje(),
                        conta = conta
                    )
                )
                mes = mesAtual()
                mostrarMovimento = false
            }
        )
    }

    if (mostrarConta) {
        DialogoConta(
            aoFechar = { mostrarConta = false },
            aoSalvar = { nome, tipo, inicial ->
                if (contas.none { it.nome.equals(nome, ignoreCase = true) }) {
                    atualizarContas(contas + Conta(nome = nome, tipo = tipo, inicial = inicial))
                }
                mostrarConta = false
            }
        )
    }

    if (mostrarTransferencia) {
        DialogoTransferencia(
            contas = contas.map { it.nome },
            aoFechar = { mostrarTransferencia = false },
            aoSalvar = { origem, destino, valor ->
                if (origem != destino) {
                    atualizarMovimentos(
                        movimentos + Movimento(
                            descricao = "Transferência para $destino",
                            valor = valor,
                            tipo = "Transferência",
                            categoria = "Transferência",
                            data = hoje(),
                            conta = origem,
                            destino = destino
                        )
                    )
                }
                mostrarTransferencia = false
            }
        )
    }

    if (mostrarRecorrencia) {
        DialogoRecorrencia(
            contas = contas.map { it.nome },
            aoFechar = { mostrarRecorrencia = false },
            aoSalvar = { nome, valor, dia, categoria, conta ->
                atualizarRecorrencias(
                    recorrencias + Recorrencia(
                        nome = nome,
                        valor = valor,
                        dia = dia,
                        categoria = categoria,
                        conta = conta
                    )
                )
                mostrarRecorrencia = false
            }
        )
    }

    if (mostrarOrcamento) {
        DialogoValor(
            titulo = "Orçamento de ${nomeMes(mes)}",
            rotulo = "Limite mensal (R$)",
            inicial = if (orcamento > 0) orcamento.toString() else "",
            aoFechar = { mostrarOrcamento = false },
            aoSalvar = {
                orcamentos = orcamentos + (mes to it)
                salvarOrcamentos(context, orcamentos)
                mostrarOrcamento = false
            }
        )
    }

    if (mostrarMeta) {
        DialogoMeta(
            aoFechar = { mostrarMeta = false },
            aoSalvar = { nome, objetivo ->
                atualizarMetas(metas + Meta(nome = nome, objetivo = objetivo))
                mostrarMeta = false
            }
        )
    }

    val metaAtual = metas.firstOrNull { it.id == metaContribuir }
    if (metaAtual != null) {
        DialogoValor(
            titulo = "Guardar para ${metaAtual.nome}",
            rotulo = "Valor da contribuição (R$)",
            inicial = "",
            aoFechar = { metaContribuir = null },
            aoSalvar = { valorContribuicao ->
                atualizarMetas(
                    metas.map { meta ->
                        if (meta.id == metaAtual.id) {
                            meta.copy(guardado = meta.guardado + valorContribuicao)
                        } else {
                            meta
                        }
                    }
                )
                metaContribuir = null
            }
        )
    }

    if (mostrarSeguranca) {
        DialogoSeguranca(
            pinAtivo = pinHash != null,
            aoFechar = { mostrarSeguranca = false },
            aoDefinirPin = { pin ->
                pinHash = hashPin(pin)
                prefs(context).edit().putString("pin_hash", pinHash).apply()
                desbloqueado = true
                mostrarSeguranca = false
            },
            aoRemoverPin = {
                pinHash = null
                prefs(context).edit().remove("pin_hash").apply()
                desbloqueado = true
                mostrarSeguranca = false
            },
            aoBloquear = {
                desbloqueado = false
                mostrarSeguranca = false
            }
        )
    }

    if (movimentoExcluir != null) {
        Confirmacao(
            titulo = "Excluir lançamento?",
            mensagem = "O lançamento será removido do extrato.",
            aoCancelar = { movimentoExcluir = null },
            aoConfirmar = {
                atualizarMovimentos(
                    movimentos.filterNot { it.id == movimentoExcluir?.id }
                )
                movimentoExcluir = null
            }
        )
    }

    if (metaExcluir != null) {
        Confirmacao(
            titulo = "Excluir meta?",
            mensagem = "O objetivo e o progresso serão removidos.",
            aoCancelar = { metaExcluir = null },
            aoConfirmar = {
                atualizarMetas(metas.filterNot { it.id == metaExcluir?.id })
                metaExcluir = null
            }
        )
    }

    if (contaExcluir != null) {
        val contaSelecionada = contaExcluir
        Confirmacao(
            titulo = "Excluir conta?",
            mensagem = "A exclusão só é permitida se a conta não tiver lançamentos ou recorrências associados.",
            aoCancelar = { contaExcluir = null },
            aoConfirmar = {
                if (
                    contaSelecionada != null &&
                    contas.size > 1 &&
                    movimentos.none {
                        it.conta == contaSelecionada.nome ||
                            it.destino == contaSelecionada.nome
                    } &&
                    recorrencias.none { it.conta == contaSelecionada.nome }
                ) {
                    atualizarContas(
                        contas.filterNot { it.id == contaSelecionada.id }
                    )
                }
                contaExcluir = null
            }
        )
    }

    if (recorrenciaExcluir != null) {
        Confirmacao(
            titulo = "Excluir conta recorrente?",
            mensagem = "A recorrência deixará de aparecer na lista.",
            aoCancelar = { recorrenciaExcluir = null },
            aoConfirmar = {
                atualizarRecorrencias(
                    recorrencias.filterNot { it.id == recorrenciaExcluir?.id }
                )
                recorrenciaExcluir = null
            }
        )
    }
}

@Composable
private fun TelaInicio(
    saldoGeral: Double,
    saldoMes: Double,
    receitas: Double,
    despesas: Double,
    movimentos: List<Movimento>,
    orcamento: Double,
    progressoOrcamento: Float,
    ocultar: Boolean,
    onReceita: () -> Unit,
    onDespesa: () -> Unit,
    onTransferir: () -> Unit,
    onOrcamento: () -> Unit,
    onExtrato: () -> Unit,
    onExcluir: (Movimento) -> Unit
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Cartao),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(21.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "PATRIMÔNIO REGISTRADO",
                        color = TextoSecundario,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(8.dp).background(Verde, RoundedCornerShape(50)))
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    if (ocultar) "••••••••" else dinheiro(saldoGeral),
                    color = Texto,
                    fontSize = 31.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    if (ocultar) "Valores ocultos" else "Saldo de todas as contas",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(19.dp))
                HorizontalDivider(color = TextoSecundario.copy(alpha = 0.2f))
                Spacer(Modifier.height(14.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ResumoFinanceiro("Entradas", receitas, Verde, ocultar, Modifier.weight(1f))
                    ResumoFinanceiro("Saídas", despesas, Vermelho, ocultar, Modifier.weight(1f))
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    "Resultado do mês: " +
                        if (ocultar) "••••••" else dinheiro(saldoMes),
                    color = if (saldoMes >= 0) Verde else Vermelho,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            AcaoRapida("＋", "Receita", Modifier.weight(1f), Verde, onReceita)
            AcaoRapida("－", "Despesa", Modifier.weight(1f), Vermelho, onDespesa)
            AcaoRapida("⇄", "Transferir", Modifier.weight(1f), Azul, onTransferir)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Painel),
            shape = RoundedCornerShape(19.dp)
        ) {
            Column(Modifier.padding(17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Orçamento do mês", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            if (orcamento > 0) {
                                "Limite ${if (ocultar) "••••" else dinheiro(orcamento)}"
                            } else {
                                "Defina quanto pretende gastar"
                            },
                            color = TextoSecundario,
                            fontSize = 11.sp
                        )
                    }
                    TextButton(onClick = onOrcamento) {
                        Text(if (orcamento > 0) "Editar" else "Definir", color = Verde)
                    }
                }

                if (orcamento > 0) {
                    Spacer(Modifier.height(9.dp))
                    LinearProgressIndicator(
                        progress = { progressoOrcamento },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (progressoOrcamento >= 1f) Vermelho else Verde,
                        trackColor = Cartao
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (ocultar) "Gastos e limite ocultos"
                        else "${dinheiro(despesas)} usados de ${dinheiro(orcamento)}",
                        color = if (progressoOrcamento >= 1f) Vermelho else TextoSecundario,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Movimentações recentes",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onExtrato) {
                Text("Ver extrato →", color = Verde)
            }
        }

        if (movimentos.isEmpty()) {
            CaixaVazia("Tudo começa aqui", "Adicione sua primeira receita ou despesa.")
        } else {
            movimentos.sortedByDescending { it.data }.take(5).forEach {
                ItemMovimento(it, ocultar, onExcluir)
            }
        }
    }
}

@Composable
private fun ResumoFinanceiro(
    titulo: String,
    valor: Double,
    cor: Color,
    ocultar: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.background(Fundo, RoundedCornerShape(14.dp)).padding(12.dp)
    ) {
        Text(titulo, color = TextoSecundario, fontSize = 11.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            if (ocultar) "••••••" else dinheiro(valor),
            color = cor,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )
    }
}

@Composable
private fun AcaoRapida(
    simbolo: String,
    titulo: String,
    modifier: Modifier,
    cor: Color,
    aoClicar: () -> Unit
) {
    Column(
        modifier = modifier
            .background(Cartao, RoundedCornerShape(17.dp))
            .clickable { aoClicar() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(simbolo, color = cor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(5.dp))
        Text(titulo, color = Texto, fontSize = 11.sp)
    }
}

@Composable
private fun TelaMovimentos(
    movimentos: List<Movimento>,
    ocultar: Boolean,
    onAdicionar: () -> Unit,
    onExcluir: (Movimento) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Extrato", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${movimentos.size} lançamentos neste mês",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onAdicionar,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Verde,
                    contentColor = Fundo
                )
            ) {
                Text("+ Novo")
            }
        }

        Spacer(Modifier.height(13.dp))

        if (movimentos.isEmpty()) {
            CaixaVazia("Sem movimentações", "Os lançamentos aparecerão aqui.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(movimentos, key = { it.id }) {
                    ItemMovimento(it, ocultar, onExcluir)
                }
            }
        }
    }
}

@Composable
private fun ItemMovimento(
    m: Movimento,
    ocultar: Boolean,
    aoExcluir: (Movimento) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Cartao),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(Painel, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    when (m.tipo) {
                        "Receita" -> "↗"
                        "Transferência" -> "⇄"
                        else -> "↘"
                    },
                    color = when (m.tipo) {
                        "Receita" -> Verde
                        "Transferência" -> Azul
                        else -> Vermelho
                    },
                    fontSize = 21.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            Column(Modifier.weight(1f)) {
                Text(m.descricao, color = Texto, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${m.categoria} • ${dataCurta(m.data)}",
                    color = TextoSecundario,
                    fontSize = 10.sp
                )
                Text(
                    if (m.tipo == "Transferência") "${m.conta} → ${m.destino}" else m.conta,
                    color = TextoSecundario,
                    fontSize = 10.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (ocultar) {
                        "••••••"
                    } else {
                        (if (m.tipo == "Receita") "+ " else if (m.tipo == "Despesa") "− " else "") +
                            dinheiro(m.valor)
                    },
                    color = when (m.tipo) {
                        "Receita" -> Verde
                        "Transferência" -> Azul
                        else -> Vermelho
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                TextButton(
                    onClick = { aoExcluir(m) },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Excluir", color = TextoSecundario, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun TelaContas(
    contas: List<Conta>,
    saldos: Map<String, Double>,
    ocultar: Boolean,
    onAdicionar: () -> Unit,
    onTransferir: () -> Unit,
    onExcluir: (Conta) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Contas e carteiras", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Organize seu dinheiro por origem.", color = TextoSecundario, fontSize = 11.sp)
            }
            Button(
                onClick = onAdicionar,
                colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
            ) {
                Text("+ Conta")
            }
        }

        Spacer(Modifier.height(10.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(17.dp)) {
                Text("SALDO TOTAL", color = TextoSecundario, fontSize = 10.sp, letterSpacing = 1.5.sp)
                Text(
                    if (ocultar) "••••••••" else dinheiro(saldos.values.sum()),
                    color = Verde,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = onTransferir, modifier = Modifier.fillMaxWidth()) {
                    Text("⇄  Transferir entre contas", color = Verde)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (contas.isEmpty()) {
            CaixaVazia("Nenhuma conta", "Adicione uma conta ou carteira.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(contas, key = { it.id }) { conta ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Cartao),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(43.dp).background(VerdeEscuro, RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (conta.tipo == "Dinheiro") "$" else "▣", color = Verde, fontSize = 20.sp)
                            }

                            Spacer(Modifier.width(11.dp))

                            Column(Modifier.weight(1f)) {
                                Text(conta.nome, fontWeight = FontWeight.Bold)
                                Text(conta.tipo, color = TextoSecundario, fontSize = 11.sp)
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (ocultar) "••••••" else dinheiro(saldos[conta.nome] ?: conta.inicial),
                                    color = Verde,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                TextButton(
                                    onClick = { onExcluir(conta) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Excluir", color = TextoSecundario, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelaRecorrencias(
    recorrencias: List<Recorrencia>,
    mes: String,
    ocultar: Boolean,
    onAdicionar: () -> Unit,
    onPagar: (Recorrencia) -> Unit,
    onExcluir: (Recorrencia) -> Unit
) {
    val pendentes = recorrencias.filter { mes !in it.pagos }.sumOf { it.valor }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Contas recorrentes", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(nomeMes(mes), color = TextoSecundario, fontSize = 12.sp)
            }
            Button(
                onClick = onAdicionar,
                colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
            ) {
                Text("+ Conta")
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("PENDENTE NESTE MÊS", color = TextoSecundario, fontSize = 10.sp, letterSpacing = 1.sp)
                Text(
                    if (ocultar) "••••••" else dinheiro(pendentes),
                    color = Verde,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Marque cada conta como paga quando realizar o pagamento.",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (recorrencias.isEmpty()) {
            CaixaVazia("Nenhuma conta recorrente", "Cadastre aluguel, internet, luz ou outras contas.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(recorrencias, key = { it.id }) { r ->
                    val paga = mes in r.pagos
                    Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier.size(42.dp).background(Painel, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("◷", color = Verde, fontSize = 22.sp)
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(Modifier.weight(1f)) {
                                    Text(r.nome, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${r.categoria} • vence dia ${r.dia}",
                                        color = TextoSecundario,
                                        fontSize = 11.sp
                                    )
                                    Text("Conta: ${r.conta}", color = TextoSecundario, fontSize = 10.sp)
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        if (ocultar) "••••••" else dinheiro(r.valor),
                                        color = Texto,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        if (paga) "Paga" else "Pendente",
                                        color = if (paga) Verde else Vermelho,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(Modifier.height(10.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onPagar(r) },
                                    enabled = !paga,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Verde,
                                        contentColor = Fundo
                                    )
                                ) {
                                    Text(if (paga) "Pagamento registrado" else "Marcar como paga", fontSize = 11.sp)
                                }

                                OutlinedButton(onClick = { onExcluir(r) }) {
                                    Text("Excluir", color = Vermelho, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TelaRelatorios(
    receitas: Double,
    despesas: Double,
    movimentos: List<Movimento>,
    ocultar: Boolean
) {
    val porCategoria = movimentos.filter { it.tipo == "Despesa" }
        .groupBy { it.categoria }
        .mapValues { it.value.sumOf { m -> m.valor } }
        .toList()
        .sortedByDescending { it.second }

    val maior = max(1.0, max(receitas, despesas))

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Relatórios", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Entenda para onde seu dinheiro está indo.", color = TextoSecundario, fontSize = 12.sp)

        Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Entradas x saídas", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(15.dp))

                Canvas(Modifier.fillMaxWidth().height(150.dp)) {
                    val base = size.height - 6.dp.toPx()
                    val maxH = size.height - 22.dp.toPx()
                    val w = 65.dp.toPx()
                    val h1 = maxH * (receitas / maior).toFloat()
                    val h2 = maxH * (despesas / maior).toFloat()
                    val x1 = size.width * 0.28f - w / 2
                    val x2 = size.width * 0.72f - w / 2

                    drawLine(
                        TextoSecundario.copy(alpha = .3f),
                        Offset(0f, base),
                        Offset(size.width, base),
                        1.dp.toPx()
                    )

                    drawRoundRect(
                        color = Verde,
                        topLeft = Offset(x1, base - h1),
                        size = Size(w, h1.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )

                    drawRoundRect(
                        color = Vermelho,
                        topLeft = Offset(x2, base - h2),
                        size = Size(w, h2.coerceAtLeast(3.dp.toPx())),
                        cornerRadius = CornerRadius(10.dp.toPx())
                    )
                }

                LinhaResumo("Receitas", if (ocultar) "••••••" else dinheiro(receitas), Verde)
                LinhaResumo("Despesas", if (ocultar) "••••••" else dinheiro(despesas), Vermelho)
                LinhaResumo(
                    "Resultado",
                    if (ocultar) "••••••" else dinheiro(receitas - despesas),
                    if (receitas >= despesas) Verde else Vermelho
                )
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Despesas por categoria", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Spacer(Modifier.height(12.dp))

                if (porCategoria.isEmpty()) {
                    Text("Adicione despesas para ver seu relatório.", color = TextoSecundario, fontSize = 12.sp)
                } else {
                    val maxCategoria = max(1.0, porCategoria.maxOf { it.second })

                    porCategoria.forEach { (categoria, valor) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(categoria, color = Texto, modifier = Modifier.weight(1f), fontSize = 12.sp)
                            Text(
                                if (ocultar) "••••" else dinheiro(valor),
                                color = TextoSecundario,
                                fontSize = 11.sp
                            )
                        }

                        Spacer(Modifier.height(5.dp))

                        LinearProgressIndicator(
                            progress = { (valor / maxCategoria).toFloat().coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp),
                            color = Azul,
                            trackColor = Fundo
                        )

                        Text(
                            if (ocultar) "Percentual oculto"
                            else "${if (despesas > 0) (valor / despesas * 100).toInt() else 0}% das despesas",
                            color = TextoSecundario,
                            fontSize = 10.sp
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LinhaResumo(nome: String, valor: String, cor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(nome, color = TextoSecundario, modifier = Modifier.weight(1f), fontSize = 13.sp)
        Text(valor, color = cor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun TelaMetas(
    metas: List<Meta>,
    ocultar: Boolean,
    onAdicionar: () -> Unit,
    onContribuir: (Meta) -> Unit,
    onExcluir: (Meta) -> Unit
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Minhas metas", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text("Pequenos passos, grandes conquistas.", color = TextoSecundario, fontSize = 11.sp)
            }
            Button(
                onClick = onAdicionar,
                colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
            ) {
                Text("+ Criar")
            }
        }

        Spacer(Modifier.height(14.dp))

        if (metas.isEmpty()) {
            CaixaVazia("Sua próxima conquista começa aqui", "Crie uma meta para acompanhar o quanto já guardou.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(11.dp)) {
                items(metas, key = { it.id }) { m ->
                    val progresso = if (m.objetivo > 0) {
                        (m.guardado / m.objetivo).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }

                    Card(colors = CardDefaults.cardColors(containerColor = Cartao), shape = RoundedCornerShape(18.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(m.nome, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(5.dp))

                            Text(
                                if (ocultar) "•••••• de ••••••"
                                else "${dinheiro(m.guardado)} de ${dinheiro(m.objetivo)}",
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

                            Spacer(Modifier.height(5.dp))
                            Text("${(progresso * 100).toInt()}% concluído", color = Verde, fontSize = 11.sp)

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onContribuir(m) },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
                                ) {
                                    Text("Guardar dinheiro", fontSize = 11.sp)
                                }

                                OutlinedButton(onClick = { onExcluir(m) }) {
                                    Text("Excluir", color = Vermelho)
                                }
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
        colors = CardDefaults.cardColors(containerColor = Cartao),
        shape = RoundedCornerShape(19.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.fillMaxWidth().padding(25.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(54.dp).background(VerdeEscuro, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("◇", color = Verde, fontSize = 31.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text(titulo, color = Texto, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(descricao, color = TextoSecundario, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CampoTexto(
    valor: String,
    aoMudar: (String) -> Unit,
    rotulo: String,
    teclado: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = valor,
        onValueChange = aoMudar,
        label = { Text(rotulo) },
        keyboardOptions = KeyboardOptions(keyboardType = teclado),
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Seletor(
    rotulo: String,
    opcoes: List<String>,
    selecionado: String,
    aoSelecionar: (String) -> Unit
) {
    var aberto by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(onClick = { aberto = true }, modifier = Modifier.fillMaxWidth()) {
            Text("$rotulo: $selecionado", maxLines = 1)
        }

        DropdownMenu(expanded = aberto, onDismissRequest = { aberto = false }) {
            opcoes.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        aoSelecionar(item)
                        aberto = false
                    }
                )
            }
        }
    }
}

@Composable
private fun DialogoLancamento(
    contas: List<String>,
    tipoInicial: String,
    aoFechar: () -> Unit,
    aoSalvar: (String, Double, String, String, String) -> Unit
) {
    var descricao by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var tipo by remember(tipoInicial) { mutableStateOf(tipoInicial) }
    var categoria by remember(tipoInicial) {
        mutableStateOf(if (tipoInicial == "Receita") "Salário" else "Alimentação")
    }
    var conta by remember(contas) {
        mutableStateOf(contas.firstOrNull() ?: "Principal")
    }

    val valor = lerValor(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Novo lançamento") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Despesa", "Receita").forEach {
                        FilterChip(
                            selected = tipo == it,
                            onClick = {
                                tipo = it
                                categoria = if (it == "Receita") "Salário" else "Alimentação"
                            },
                            label = { Text(it) }
                        )
                    }
                }

                CampoTexto(descricao, { descricao = it }, "Descrição")
                CampoTexto(texto, { texto = it }, "Valor (R$)", KeyboardType.Decimal)

                Seletor(
                    "Categoria",
                    if (tipo == "Receita") listOf("Salário", "Freelance", "Outros")
                    else categorias.filter { it != "Salário" && it != "Freelance" },
                    categoria,
                    { categoria = it }
                )

                Seletor("Conta", contas.ifEmpty { listOf("Principal") }, conta, { conta = it })
                Text("Data do lançamento: hoje", color = TextoSecundario, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = descricao.isNotBlank() && valor != null,
                onClick = {
                    if (valor != null) {
                        aoSalvar(descricao.trim(), valor, tipo, categoria, conta)
                    }
                }
            ) {
                Text("Salvar", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoConta(
    aoFechar: () -> Unit,
    aoSalvar: (String, String, Double) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var tipo by remember { mutableStateOf("Banco") }
    var inicialTexto by remember { mutableStateOf("0") }
    val inicial = if (inicialTexto == "0" || inicialTexto.isBlank()) 0.0 else lerValor(inicialTexto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Nova conta ou carteira") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(nome, { nome = it }, "Nome da conta")
                Seletor(
                    "Tipo",
                    listOf("Banco", "Carteira digital", "Dinheiro", "Poupança", "Investimento", "Outro"),
                    tipo,
                    { tipo = it }
                )
                CampoTexto(inicialTexto, { inicialTexto = it }, "Saldo inicial (R$)", KeyboardType.Decimal)
                Text("Informe o saldo que já existe nessa conta.", color = TextoSecundario, fontSize = 11.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank() && inicial != null,
                onClick = { if (inicial != null) aoSalvar(nome.trim(), tipo, inicial) }
            ) {
                Text("Criar conta", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoTransferencia(
    contas: List<String>,
    aoFechar: () -> Unit,
    aoSalvar: (String, String, Double) -> Unit
) {
    var origem by remember(contas) {
        mutableStateOf(contas.firstOrNull() ?: "Principal")
    }
    var destino by remember(contas) {
        mutableStateOf(contas.getOrNull(1) ?: contas.firstOrNull() ?: "Principal")
    }
    var texto by remember { mutableStateOf("") }
    val valor = lerValor(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Transferir entre contas") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (contas.size < 2) {
                    Text("Cadastre pelo menos duas contas para transferir.", color = TextoSecundario)
                }

                Seletor("Origem", contas, origem, { origem = it })
                Seletor("Destino", contas, destino, { destino = it })
                CampoTexto(texto, { texto = it }, "Valor (R$)", KeyboardType.Decimal)

                Text(
                    "A transferência não será contada como receita ou despesa.",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = contas.size >= 2 && origem != destino && valor != null,
                onClick = {
                    if (valor != null) aoSalvar(origem, destino, valor)
                }
            ) {
                Text("Transferir", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoRecorrencia(
    contas: List<String>,
    aoFechar: () -> Unit,
    aoSalvar: (String, Double, Int, String, String) -> Unit
) {
    var nome by remember { mutableStateOf("") }
    var texto by remember { mutableStateOf("") }
    var diaTexto by remember { mutableStateOf("10") }
    var categoria by remember { mutableStateOf("Contas") }
    var conta by remember(contas) {
        mutableStateOf(contas.firstOrNull() ?: "Principal")
    }

    val valor = lerValor(texto)
    val dia = diaTexto.toIntOrNull()?.takeIf { it in 1..31 }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Cadastrar conta recorrente") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CampoTexto(nome, { nome = it }, "Nome (ex.: aluguel)")
                CampoTexto(texto, { texto = it }, "Valor (R$)", KeyboardType.Decimal)
                CampoTexto(diaTexto, { diaTexto = it }, "Dia do vencimento", KeyboardType.Number)

                Seletor(
                    "Categoria",
                    categorias.filter { it != "Salário" && it != "Freelance" },
                    categoria,
                    { categoria = it }
                )

                Seletor(
                    "Conta de pagamento",
                    contas.ifEmpty { listOf("Principal") },
                    conta,
                    { conta = it }
                )

                Text(
                    "A recorrência aparece todo mês. O pagamento só é registrado quando você marcar como pago.",
                    color = TextoSecundario,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank() && valor != null && dia != null,
                onClick = {
                    if (valor != null && dia != null) {
                        aoSalvar(nome.trim(), valor, dia, categoria, conta)
                    }
                }
            ) {
                Text("Salvar conta", color = Verde)
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
    val valor = lerValor(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Criar meta financeira") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CampoTexto(nome, { nome = it }, "Nome da meta")
                CampoTexto(texto, { texto = it }, "Valor desejado (R$)", KeyboardType.Decimal)
            }
        },
        confirmButton = {
            TextButton(
                enabled = nome.isNotBlank() && valor != null,
                onClick = { if (valor != null) aoSalvar(nome.trim(), valor) }
            ) {
                Text("Criar meta", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoValor(
    titulo: String,
    rotulo: String,
    inicial: String,
    aoFechar: () -> Unit,
    aoSalvar: (Double) -> Unit
) {
    var texto by remember(titulo) { mutableStateOf(inicial) }
    val valor = lerValor(texto)

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text(titulo) },
        text = { CampoTexto(texto, { texto = it }, rotulo, KeyboardType.Decimal) },
        confirmButton = {
            TextButton(
                enabled = valor != null,
                onClick = { if (valor != null) aoSalvar(valor) }
            ) {
                Text("Salvar", color = Verde)
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Cancelar") } },
        containerColor = Cartao
    )
}

@Composable
private fun DialogoSeguranca(
    pinAtivo: Boolean,
    aoFechar: () -> Unit,
    aoDefinirPin: (String) -> Unit,
    aoRemoverPin: () -> Unit,
    aoBloquear: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var confirmar by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = aoFechar,
        title = { Text("Privacidade e segurança") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (pinAtivo) "O bloqueio por PIN está ativado."
                    else "Configure um PIN de 4 a 8 números para bloquear a abertura do app.",
                    color = TextoSecundario,
                    fontSize = 12.sp
                )

                if (!pinAtivo) {
                    CampoTexto(
                        pin,
                        { if (it.length <= 8 && it.all(Char::isDigit)) pin = it },
                        "Novo PIN (4 a 8 dígitos)",
                        KeyboardType.NumberPassword
                    )

                    CampoTexto(
                        confirmar,
                        { if (it.length <= 8 && it.all(Char::isDigit)) confirmar = it },
                        "Confirme o PIN",
                        KeyboardType.NumberPassword
                    )

                    if (erro.isNotEmpty()) {
                        Text(erro, color = Vermelho, fontSize = 12.sp)
                    }
                }

                if (pinAtivo) {
                    OutlinedButton(onClick = aoBloquear, modifier = Modifier.fillMaxWidth()) {
                        Text("Bloquear o app agora", color = Verde)
                    }

                    OutlinedButton(onClick = aoRemoverPin, modifier = Modifier.fillMaxWidth()) {
                        Text("Remover PIN", color = Vermelho)
                    }
                }

                Text(
                    "O PIN é uma proteção básica local. Ele não substitui a segurança do sistema Android.",
                    color = TextoSecundario,
                    fontSize = 10.sp
                )
            }
        },
        confirmButton = {
            if (!pinAtivo) {
                TextButton(onClick = {
                    if (pin.length in 4..8 && pin == confirmar) {
                        aoDefinirPin(pin)
                    } else {
                        erro = "Os PINs precisam ter de 4 a 8 números e ser iguais."
                    }
                }) {
                    Text("Ativar PIN", color = Verde)
                }
            } else {
                TextButton(onClick = aoFechar) {
                    Text("Concluir", color = Verde)
                }
            }
        },
        dismissButton = { TextButton(onClick = aoFechar) { Text("Fechar") } },
        containerColor = Cartao
    )
}

@Composable
private fun TelaBloqueio(aoDesbloquear: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().background(Fundo).padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(78.dp).background(VerdeEscuro, RoundedCornerShape(25.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("M", color = Verde, fontSize = 42.sp, fontWeight = FontWeight.Black)
        }

        Spacer(Modifier.height(20.dp))
        Text("Meu Cofre", color = Texto, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Digite seu PIN para continuar", color = TextoSecundario, fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = {
                if (it.length <= 8 && it.all(Char::isDigit)) {
                    pin = it
                    erro = false
                }
            },
            label = { Text("PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            singleLine = true
        )

        if (erro) {
            Text("PIN incorreto. Tente novamente.", color = Vermelho, fontSize = 12.sp)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                // A tela principal valida o PIN e só libera o app quando estiver correto.
                aoDesbloquear(pin)
                erro = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Verde, contentColor = Fundo)
        ) {
            Text("Desbloquear")
        }
    }
}

@Composable
private fun Confirmacao(
    titulo: String,
    mensagem: String,
    aoCancelar: () -> Unit,
    aoConfirmar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = aoCancelar,
        title = { Text(titulo) },
        text = { Text(mensagem, color = TextoSecundario) },
        confirmButton = {
            TextButton(onClick = aoConfirmar) {
                Text("Confirmar", color = Vermelho)
            }
        },
        dismissButton = {
            TextButton(onClick = aoCancelar) {
                Text("Cancelar")
            }
        },
        containerColor = Cartao
    )
}



