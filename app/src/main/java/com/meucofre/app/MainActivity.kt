package com.meucofre.app

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.net.HttpURLConnection
import java.net.URL
import java.io.OutputStreamWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val Bg = Color(0xFFF5F6FB)
private val Panel = Color(0xFFFFFFFF)
private val Card = Color(0xFFF0F1FF)
private val Card2 = Color(0xFFE5E8F5)
private val Green = Color(0xFF6558E8)
private val GreenDeep = Color(0xFFE9E6FF)
private val Red = Color(0xFFD94F70)
private val Blue = Color(0xFF3478D4)
private val Orange = Color(0xFFC17A19)
private val Txt = Color(0xFF20243A)
private val Muted = Color(0xFF737B91)
private val Border = Color(0xFFE1E4EF)
private val localeBR = Locale("pt", "BR")

private fun money(v: Double): String = NumberFormat.getCurrencyInstance(localeBR).format(v)
private fun currentMonth(): String = todayIsoDate().take(7)
private fun monthLabel(key: String): String = try { SimpleDateFormat("MMMM 'de' yyyy", localeBR).format(SimpleDateFormat("yyyy-MM", Locale.US).parse(key)!!).replaceFirstChar { it.uppercase() } } catch (_: Exception) { key }
private fun parseMoney(s: String): Double? = s.trim().replace("R$", "").replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull()?.takeIf { it > 0 }
private fun sha(s: String): String = MessageDigest.getInstance("SHA-256").digest(s.toByteArray()).joinToString("") { "%02x".format(it) }


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(245, 246, 251)
        window.navigationBarColor = android.graphics.Color.rgb(255, 255, 255)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Bg, onBackground = Txt, surface = Panel, onSurface = Txt, secondary = Blue, error = Red)) {
                CofreApp(applicationContext)
            }
        }
    }
}

@Composable private fun CofreApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("meu_cofre_premium", Context.MODE_PRIVATE) }
    val moves = remember { mutableStateListOf<Move>().apply { addAll(loadMoves(prefs)) } }
    val accounts = remember { mutableStateListOf<Account>().apply { addAll(loadAccounts(prefs).ifEmpty { listOf(Account(name = "Conta principal", type = "Corrente")) }) } }
    val recurring = remember { mutableStateListOf<Recurring>().apply { addAll(loadRecurring(prefs)) } }
    val goals = remember { mutableStateListOf<Goal>().apply { addAll(loadGoals(prefs)) } }
    val budgets = remember { mutableStateMapOf<String, Double>().apply { putAll(loadBudgets(prefs)) } }
    var pendingWalletJson by remember { mutableStateOf(WalletNotificationStore.firstPending(context)) }
    var pinHash by remember { mutableStateOf(prefs.getString("pin_hash", "") ?: "") }
    var unlocked by remember { mutableStateOf(pinHash.isEmpty()) }
    var hideBalance by remember { mutableStateOf(prefs.getBoolean("hide_balance", false)) }
    var tab by remember { mutableStateOf("Início") }
    var month by remember { mutableStateOf(currentMonth()) }
    var showMove by remember { mutableStateOf(false) }
    var showAccount by remember { mutableStateOf(false) }
    var showRecurring by remember { mutableStateOf(false) }
    var showGoal by remember { mutableStateOf(false) }
    var showBudget by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var editingMove by remember { mutableStateOf<Move?>(null) }
    var editingGoal by remember { mutableStateOf<Goal?>(null) }
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Todas") }
    var toast by remember { mutableStateOf("") }
    fun persist() {
        prefs.edit().putString("movimentos", movesToJson(moves)).putString("contas", accountsToJson(accounts)).putString("recorrencias", recurringToJson(recurring)).putString("metas", goalsToJson(goals)).putString("orcamentos", budgetsToJson(budgets)).putBoolean("ocultar_saldo", hideBalance).putString("pin_hash", pinHash).apply()
    }
    LaunchedEffect(moves.toList(), accounts.toList(), recurring.toList(), goals.toList(), budgets.toMap(), hideBalance, pinHash) { persist() }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                pendingWalletJson = WalletNotificationStore.firstPending(context)
            }
        }
        val filterIntent = IntentFilter(WalletNotificationStore.ACTION)
        if (Build.VERSION.SDK_INT >= 33) context.registerReceiver(receiver, filterIntent, Context.RECEIVER_NOT_EXPORTED)
        else {
            @Suppress("DEPRECATION")
            context.registerReceiver(receiver, filterIntent)
        }
        onDispose { try { context.unregisterReceiver(receiver) } catch (_: Exception) {} }
    }
    BackHandler(enabled = tab != "Início") { tab = "Início" }
    if (!unlocked) {
        PinGate(onUnlock = { entered -> if (sha(entered) == pinHash) { unlocked = true; true } else false })
        return
    }
    val monthMoves = moves.filter { it.date.startsWith(month) }
    val monthSummary = calculateMonthlySummary(moves, month)
    val income = monthSummary.income
    val expense = monthSummary.expense
    val totalBalance = calculateTotalBalance(accounts, moves)
    Scaffold(containerColor = Bg, bottomBar = {
        NavigationBar(containerColor = Panel, contentColor = Muted, tonalElevation = 0.dp) {
            listOf(Triple("Início", Icons.Filled.Home, "Início"), Triple("Movimentos", Icons.Filled.SwapHoriz, "Movimentos"), Triple("Contas", Icons.Filled.AccountBalanceWallet, "Contas"), Triple("Relatórios", Icons.Filled.Insights, "Relatórios"), Triple("Mais", Icons.Filled.GridView, "Mais")).forEach { (label, icon, destination) ->
                NavigationBarItem(selected = tab == destination || (destination == "Mais" && tab in listOf("Fixas", "Metas", "Orçamentos", "Segurança", "Notificações", "Assistente")), onClick = { tab = if (destination == "Mais") "Mais" else destination }, icon = { Icon(icon, null) }, label = { Text(label, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Green, selectedTextColor = Green, indicatorColor = GreenDeep, unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
    }, floatingActionButton = { if (tab in listOf("Início", "Movimentos")) FloatingActionButton(onClick = { editingMove = null; showMove = true }, containerColor = Green, contentColor = Bg, shape = CircleShape) { Icon(Icons.Filled.Add, "Nova movimentação") } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).background(Bg)) {
            Header(onSecurity = { showSecurity = true }, hideBalance = hideBalance, onHide = { hideBalance = !hideBalance })
            AnimatedContent(targetState = tab, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "tab") { page ->
                when (page) {
                    "Início" -> Dashboard(month, { month = shiftMonth(month, it) }, totalBalance, income, expense, monthMoves, accounts, recurring, goals, budgets, hideBalance, onMove = { editingMove = it; showMove = true }, onTab = { tab = it }, onAddBudget = { showBudget = true })
                    "Movimentos" -> MovementsScreen(moves, month, { month = shiftMonth(month, it) }, search, { search = it }, filter, { filter = it }, hideBalance, onEdit = { editingMove = it; showMove = true }, onDelete = { moves.removeAll { m -> m.id == it.id }; toast = "Movimentação excluída" })
                    "Contas" -> AccountsScreen(accounts, moves, hideBalance, onAdd = { showAccount = true }, onTransfer = { showMove = true; editingMove = null }, onDelete = { a -> if (moves.any { it.account == a.id || it.destination == a.id } || recurring.any { it.account == a.id }) toast = "Esta conta tem movimentações vinculadas." else if (accounts.size > 1) accounts.remove(a) else toast = "Mantenha pelo menos uma conta." })
                    "Relatórios" -> ReportsScreen(month, { month = shiftMonth(month, it) }, moves, hideBalance)
                    "Mais" -> MoreScreen(onSelect = { tab = it }, recurringCount = recurring.size, goalsCount = goals.size, budgetsCount = budgets.size)
                    "Fixas" -> RecurringScreen(recurring, month, hideBalance, onAdd = { showRecurring = true }, onTogglePaid = { r -> val set = r.paidMonths.toMutableSet(); if (!set.add(month)) set.remove(month); val ix = recurring.indexOfFirst { it.id == r.id }; if (ix >= 0) recurring[ix] = r.copy(paidMonths = set) }, onDelete = { r -> recurring.removeAll { it.id == r.id } })
                    "Metas" -> GoalsScreen(goals, hideBalance, onAdd = { editingGoal = null; showGoal = true }, onEdit = { editingGoal = it; showGoal = true }, onDelete = { g -> goals.removeAll { it.id == g.id } })
                    "Orçamentos" -> BudgetsScreen(budgets, month, moves, hideBalance, onAdd = { showBudget = true }, onDelete = { budgets.remove(it) })
                    "Segurança" -> SecurityScreen(hideBalance, pinHash.isNotEmpty(), onHide = { hideBalance = !hideBalance }, onSetupPin = { showSecurity = true })
                    "Notificações" -> WalletImportScreen(
                        pendingJson = pendingWalletJson,
                        accounts = accounts,
                        onEnable = { try { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) { toast = "Não foi possível abrir as configurações de notificações." } },
                        onImport = { title, amount, account ->
                            val parsed = parseMoney(amount)
                            if (parsed != null && title.isNotBlank() && account.isNotBlank()) {
                                moves.add(0, Move(title = title, amount = parsed, type = "Despesa", category = "Compras", date = todayIsoDate(), account = account))
                                WalletNotificationStore.removeFirst(context)
                                pendingWalletJson = WalletNotificationStore.firstPending(context)
                                toast = "Compra adicionada ao Meu Cofre."
                            } else toast = "Confira a descrição e informe um valor válido."
                        },
                        onDismissPending = { WalletNotificationStore.removeFirst(context); pendingWalletJson = WalletNotificationStore.firstPending(context) }
                    )
                    "Assistente" -> FinancialAssistantScreen(context, month, moves, accounts, recurring, goals, budgets.toMap(), totalBalance, hideBalance)
                    else -> MoreScreen(onSelect = { tab = it }, recurringCount = recurring.size, goalsCount = goals.size, budgetsCount = budgets.size)
                }
            }
        }
    }
    if (showMove) MoveDialog(editingMove, accounts, onDismiss = { showMove = false }, onSave = { title, amount, type, category, date, account, destination ->
        if (editingMove != null) { val index = moves.indexOfFirst { it.id == editingMove!!.id }; if (index >= 0) moves[index] = editingMove!!.copy(title = title, amount = amount, type = type, category = category, date = date, account = account, destination = destination) }
        else moves.add(0, Move(title = title, amount = amount, type = type, category = category, date = date, account = account, destination = destination))
        showMove = false
    })
    if (showAccount) AccountDialog(onDismiss = { showAccount = false }, onSave = { name, type, initial -> accounts.add(Account(name = name, type = type, initial = initial)); showAccount = false })
    if (showRecurring) RecurringDialog(accounts, onDismiss = { showRecurring = false }, onSave = { title, amount, day, category, account -> recurring.add(Recurring(title = title, amount = amount, day = day, category = category, account = account)); showRecurring = false })
    if (showGoal) GoalDialog(editingGoal, onDismiss = { showGoal = false }, onSave = { title, target, saved -> if (editingGoal != null) { val i = goals.indexOfFirst { it.id == editingGoal!!.id }; if (i >= 0) goals[i] = editingGoal!!.copy(title = title, target = target, saved = saved) } else goals.add(Goal(title = title, target = target, saved = saved)); showGoal = false }, onAddSaved = { g, value -> val i = goals.indexOfFirst { it.id == g.id }; if (i >= 0) goals[i] = g.copy(saved = g.saved + value) })
    if (showBudget) BudgetDialog(budgets.keys.toList(), onDismiss = { showBudget = false }, onSave = { category, value -> budgets[category] = value; showBudget = false })
    if (showSecurity) SecurityDialog(pinHash.isNotEmpty(), onDismiss = { showSecurity = false }, onSavePin = { pin -> pinHash = if (pin.isBlank()) "" else sha(pin); showSecurity = false; toast = if (pin.isBlank()) "PIN removido" else "PIN configurado" })
    if (toast.isNotBlank()) AlertDialog(onDismissRequest = { toast = "" }, title = { Text("Meu Cofre") }, text = { Text(toast) }, confirmButton = { TextButton(onClick = { toast = "" }) { Text("OK") } }, containerColor = Card)
}

@Composable private fun Header(onSecurity: () -> Unit, hideBalance: Boolean, onHide: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AccountBalanceWallet, null, tint = Green, modifier = Modifier.size(23.dp)) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) { Text("MEU COFRE", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp, color = Txt); Text("Seu dinheiro, sob controle", fontSize = 11.sp, color = Muted) }
        IconButton(onClick = onHide) { Icon(if (hideBalance) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, "Ocultar saldos", tint = Muted) }
        IconButton(onClick = onSecurity) { Icon(Icons.Filled.Security, "Segurança", tint = Green) }
    }
}

@Composable private fun Dashboard(month: String, shift: (Int) -> Unit, balance: Double, income: Double, expense: Double, moves: List<Move>, accounts: List<Account>, recurring: List<Recurring>, goals: List<Goal>, budgets: Map<String, Double>, hidden: Boolean, onMove: (Move) -> Unit, onTab: (String) -> Unit, onAddBudget: () -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 100.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) { Column(Modifier.weight(1f)) { Text("Visão geral", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Acompanhe sua vida financeira", color = Muted, fontSize = 12.sp) }; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 8.dp, vertical = 4.dp)) { IconButton(onClick = { shift(-1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.ChevronLeft, null, tint = Muted) }; Text(month.take(7).replace("-", "/"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold); IconButton(onClick = { shift(1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Filled.ChevronRight, null, tint = Muted) } } } }
        item { BalanceCard(balance, hidden, income, expense) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricCard("Receitas", income, Green, Icons.Filled.TrendingUp, hidden, Modifier.weight(1f)); MetricCard("Despesas", expense, Red, Icons.Filled.TrendingDown, hidden, Modifier.weight(1f)) } }
        item { Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Fluxo do mês", "Receitas x despesas"); Spacer(Modifier.weight(1f)); Text("Ver relatórios", color = Green, fontSize = 11.sp, modifier = Modifier.clickable { onTab("Relatórios") }) } }
        item { FlowCard(income, expense) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Orçamentos", "Controle por categoria"); Spacer(Modifier.weight(1f)); IconButton(onClick = onAddBudget, modifier = Modifier.size(32.dp)) { Icon(Icons.Filled.Add, "Adicionar orçamento", tint = Green) } } }
        if (budgets.isEmpty()) item { EmptyHint("Defina limites para suas categorias e acompanhe seus gastos.", "Criar orçamento", onAddBudget) }
        else items(budgets.entries.take(3).toList()) { (category, limit) -> val spent = moves.filter { it.type == "Despesa" && it.category == category }.sumOf { it.amount }; BudgetRow(category, spent, limit, hidden) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Movimentações recentes", "Suas últimas atividades"); Spacer(Modifier.weight(1f)); Text("Ver todas", color = Green, fontSize = 11.sp, modifier = Modifier.clickable { onTab("Movimentos") }) } }
        if (moves.isEmpty()) item { EmptyHint("Sua jornada financeira começa com a primeira movimentação.", "Adicionar movimentação", { onTab("Movimentos") }) }
        else items(moves.sortedByDescending { it.date }.take(4), key = { it.id }) { MoveRow(it, hidden, onClick = { onMove(it) }) }
        item { Row(verticalAlignment = Alignment.CenterVertically) { SectionTitle("Minhas contas", "Saldo por carteira"); Spacer(Modifier.weight(1f)); Text("Ver contas", color = Green, fontSize = 11.sp, modifier = Modifier.clickable { onTab("Contas") }) } }
        items(accounts.take(3), key = { it.id }) { account -> val value = account.initial + moves.filter { it.account == account.id }.sumOf { if (it.type == "Receita") it.amount else if (it.type == "Despesa" || it.type == "Transferência") -it.amount else 0.0 } + moves.filter { it.type == "Transferência" && it.destination == account.id }.sumOf { it.amount }; AccountMini(account, value, hidden) }
        if (recurring.isNotEmpty() || goals.isNotEmpty()) item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { SmallSummary("Contas fixas", "${recurring.size} cadastradas", Icons.Filled.EventRepeat, Modifier.weight(1f)) { onTab("Fixas") }; SmallSummary("Metas", "${goals.size} objetivos", Icons.Filled.Flag, Modifier.weight(1f)) { onTab("Metas") } } }
    }
}

@Composable private fun BalanceCard(balance: Double, hidden: Boolean, income: Double, expense: Double) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(Card).border(1.dp, Border, RoundedCornerShape(26.dp))) {
        Canvas(Modifier.fillMaxWidth().height(175.dp)) { drawCircle(Green.copy(alpha = .10f), radius = 150f, center = Offset(size.width * .96f, size.height * .04f)); drawCircle(Green.copy(alpha = .06f), radius = 95f, center = Offset(size.width * .9f, size.height * .02f)) }
        Column(Modifier.padding(21.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Savings, null, tint = Green); Spacer(Modifier.width(8.dp)); Text("PATRIMÔNIO TOTAL", color = Muted, fontSize = 11.sp, letterSpacing = 1.4.sp); Spacer(Modifier.weight(1f)); Surface(color = GreenDeep, shape = CircleShape) { Text("  PREMIUM  ", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp)) } }
            Spacer(Modifier.height(10.dp)); Text(if (hidden) "••••••••" else money(balance), fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Txt, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Saldo calculado de todas as contas", color = Muted, fontSize = 11.sp)
            Spacer(Modifier.height(22.dp)); Divider(color = Border)
            Spacer(Modifier.height(13.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.ArrowDownward, null, tint = Green, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Column(Modifier.weight(1f)) { Text("Entradas", color = Muted, fontSize = 10.sp); Text(if (hidden) "••••" else money(income), color = Green, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }; Icon(Icons.Filled.ArrowUpward, null, tint = Red, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Column(Modifier.weight(1f)) { Text("Saídas", color = Muted, fontSize = 10.sp); Text(if (hidden) "••••" else money(expense), color = Red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) } }
        }
    }
}

@Composable private fun MetricCard(label: String, value: Double, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, hidden: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier, color = Panel, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(15.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = .13f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp)) }; Spacer(Modifier.weight(1f)); Icon(Icons.Filled.MoreHoriz, null, tint = Muted, modifier = Modifier.size(18.dp)) }; Spacer(Modifier.height(12.dp)); Text(label, color = Muted, fontSize = 11.sp); Text(if (hidden) "••••••" else money(value), color = tint, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
}

@Composable private fun FlowCard(income: Double, expense: Double) {
    Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Column(Modifier.fillMaxWidth().padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Entradas", color = Muted, fontSize = 11.sp); Text(money(income), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp) }; Column(Modifier.weight(1f)) { Text("Saídas", color = Muted, fontSize = 11.sp); Text(money(expense), color = Red, fontWeight = FontWeight.Bold, fontSize = 16.sp) } }
            Spacer(Modifier.height(12.dp)); Canvas(Modifier.fillMaxWidth().height(80.dp)) { val maxV = max(1.0, max(income, expense)); val points = 22; val p1 = Path(); val p2 = Path(); for (i in 0 until points) { val x = size.width * i / (points - 1); val wave = kotlin.math.sin(i * .42f) * 5f + kotlin.math.cos(i * .19f) * 4f; val y1 = size.height * .78f - ((income / maxV).toFloat() * size.height * .5f) + wave; val y2 = size.height * .8f - ((expense / maxV).toFloat() * size.height * .5f) - wave * .7f; if (i == 0) { p1.moveTo(x, y1); p2.moveTo(x, y2) } else { p1.lineTo(x, y1); p2.lineTo(x, y2) } }; drawPath(p1, Green, style = Stroke(3f, cap = StrokeCap.Round)); drawPath(p2, Red.copy(alpha = .9f), style = Stroke(3f, cap = StrokeCap.Round)); for (i in 0..3) drawLine(Border, Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3), 1f) }
            Row(verticalAlignment = Alignment.CenterVertically) { LegendDot(Green); Text("Receitas", color = Muted, fontSize = 10.sp); Spacer(Modifier.width(15.dp)); LegendDot(Red); Text("Despesas", color = Muted, fontSize = 10.sp) }
        }
    }
}

@Composable private fun MovementsScreen(moves: List<Move>, month: String, shift: (Int) -> Unit, search: String, onSearch: (String) -> Unit, filter: String, onFilter: (String) -> Unit, hidden: Boolean, onEdit: (Move) -> Unit, onDelete: (Move) -> Unit) {
    var selected by remember { mutableStateOf<Move?>(null) }
    val list = moves.filter { it.date.startsWith(month) && (filter == "Todas" || it.type == filter) && (it.title.contains(search, true) || it.category.contains(search, true)) }.sortedByDescending { it.date }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Movimentações", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Entradas, saídas e transferências", color = Muted, fontSize = 11.sp) }; MonthPicker(month, shift) }
        OutlinedTextField(search, onSearch, Modifier.fillMaxWidth(), placeholder = { Text("Buscar por descrição ou categoria", color = Muted, fontSize = 12.sp) }, leadingIcon = { Icon(Icons.Filled.Search, null, tint = Muted) }, singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors())
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Todas", "Receita", "Despesa", "Transferência").forEach { FilterChip(selected = filter == it, onClick = { onFilter(it) }, label = { Text(it) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = GreenDeep, selectedLabelColor = Green)) } }
        if (list.isEmpty()) EmptyHint("Nenhuma movimentação encontrada neste período.", "Limpar filtros", { onSearch(""); onFilter("Todas") })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp), contentPadding = PaddingValues(bottom = 100.dp)) { items(list, key = { it.id }) { MoveRow(it, hidden) { selected = it } } }
    }
    selected?.let { m -> AlertDialog(onDismissRequest = { selected = null }, containerColor = Card, title = { Text(m.title, fontWeight = FontWeight.Bold) }, text = { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Text(m.category, color = Muted); Text(m.date); Text(if (hidden) "••••••" else money(m.amount), color = if (m.type == "Receita") Green else if (m.type == "Despesa") Red else Blue, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Tipo: ${m.type}"); Text("Conta: ${m.account}") } }, confirmButton = { TextButton(onClick = { onEdit(m); selected = null }) { Text("Editar") } }, dismissButton = { Row { TextButton(onClick = { onDelete(m); selected = null }) { Text("Excluir", color = Red) }; TextButton(onClick = { selected = null }) { Text("Fechar") } } }) }
}

@Composable private fun MoveRow(m: Move, hidden: Boolean, onClick: () -> Unit) {
    val tint = when (m.type) { "Receita" -> Green; "Despesa" -> Red; else -> Blue }
    val icon = when (m.type) { "Receita" -> Icons.Filled.ArrowDownward; "Despesa" -> Icons.Filled.ArrowUpward; else -> Icons.Filled.SwapHoriz }
    Surface(Modifier.fillMaxWidth().clickable { onClick() }, color = Panel, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = tint) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(m.title, color = Txt, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("${m.category} · ${m.date}", color = Muted, fontSize = 10.sp) }; Text(if (hidden) "••••" else (if (m.type == "Despesa") "− " else if (m.type == "Receita") "+ " else "") + money(m.amount), color = tint, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) }
    }
}

@Composable private fun AccountsScreen(accounts: List<Account>, moves: List<Move>, hidden: Boolean, onAdd: () -> Unit, onTransfer: () -> Unit, onDelete: (Account) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text("Minhas contas", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Organize seu dinheiro por carteira", color = Muted, fontSize = 11.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Add, null); Text(" Conta") } }
        Row(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onTransfer, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.SwapHoriz, null); Text(" Transferir") } }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) { items(accounts, key = { it.id }) { a -> val balance = a.initial + moves.filter { it.account == a.id }.sumOf { if (it.type == "Receita") it.amount else if (it.type == "Despesa" || it.type == "Transferência") -it.amount else 0.0 } + moves.filter { it.type == "Transferência" && it.destination == a.id }.sumOf { it.amount }; Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(18.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(43.dp).clip(RoundedCornerShape(14.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(Icons.Filled.AccountBalance, null, tint = Green) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(a.name, fontWeight = FontWeight.Bold); Text(a.type, color = Muted, fontSize = 11.sp) }; IconButton(onClick = { onDelete(a) }) { Icon(Icons.Filled.DeleteOutline, "Excluir conta", tint = Muted) } }; Spacer(Modifier.height(16.dp)); Text("SALDO ATUAL", color = Muted, fontSize = 10.sp, letterSpacing = 1.sp); Text(if (hidden) "••••••••" else money(balance), color = Green, fontSize = 24.sp, fontWeight = FontWeight.Bold) } } } }
    }
}

@Composable private fun RecurringScreen(items: List<Recurring>, month: String, hidden: Boolean, onAdd: () -> Unit, onTogglePaid: (Recurring) -> Unit, onDelete: (Recurring) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text("Contas fixas", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Organize os pagamentos recorrentes", color = Muted, fontSize = 11.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Add, null); Text(" Nova") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) { if (items.isEmpty()) item { EmptyHint("Cadastre aluguel, internet, assinaturas e outras contas.", "Adicionar conta fixa", onAdd) }; items(items.sortedBy { it.day }, key = { it.id }) { r -> val paid = month in r.paidMonths; Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(if (paid) GreenDeep else Card2), contentAlignment = Alignment.Center) { Icon(if (paid) Icons.Filled.CheckCircle else Icons.Filled.Event, null, tint = if (paid) Green else Orange) }; Spacer(Modifier.width(11.dp)); Column(Modifier.weight(1f)) { Text(r.title, fontWeight = FontWeight.SemiBold); Text("Dia ${r.day} · ${r.category}", color = Muted, fontSize = 10.sp); Text(if (paid) "Pago neste mês" else "Pendente", color = if (paid) Green else Orange, fontSize = 10.sp) }; Text(if (hidden) "••••" else money(r.amount), fontWeight = FontWeight.Bold, fontSize = 12.sp); IconButton(onClick = { onTogglePaid(r) }) { Icon(if (paid) Icons.Filled.Undo else Icons.Filled.Check, if (paid) "Desmarcar pagamento" else "Marcar como pago", tint = Green) }; IconButton(onClick = { onDelete(r) }) { Icon(Icons.Filled.DeleteOutline, "Excluir", tint = Muted) } } } } } }
}

@Composable private fun GoalsScreen(goals: List<Goal>, hidden: Boolean, onAdd: () -> Unit, onEdit: (Goal) -> Unit, onDelete: (Goal) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text("Metas financeiras", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Pequenos passos, grandes conquistas", color = Muted, fontSize = 11.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Add, null); Text(" Meta") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 100.dp)) { if (goals.isEmpty()) item { EmptyHint("Crie uma meta para acompanhar sua reserva ou uma compra importante.", "Criar meta", onAdd) }; items(goals, key = { it.id }) { g -> val progress = (g.saved / g.target).toFloat().coerceIn(0f, 1f); Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(17.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Flag, null, tint = Green) }; Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(g.title, fontWeight = FontWeight.Bold); Text("${(progress * 100).toInt()}% concluído", color = Muted, fontSize = 10.sp) }; IconButton(onClick = { onEdit(g) }) { Icon(Icons.Filled.Edit, "Editar", tint = Muted) }; IconButton(onClick = { onDelete(g) }) { Icon(Icons.Filled.DeleteOutline, "Excluir", tint = Muted) } }; Spacer(Modifier.height(15.dp)); LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Green, trackColor = Card2); Spacer(Modifier.height(9.dp)); Row { Text(if (hidden) "••••" else money(g.saved), color = Green, fontWeight = FontWeight.Bold); Spacer(Modifier.weight(1f)); Text("de ${if (hidden) "••••" else money(g.target)}", color = Muted, fontSize = 11.sp) }; if (g.saved < g.target) TextButton(onClick = { onEdit(g) }) { Icon(Icons.Filled.Add, null); Text("Atualizar valor guardado") } } } } } }
}

@Composable private fun BudgetsScreen(budgets: Map<String, Double>, month: String, moves: List<Move>, hidden: Boolean, onAdd: () -> Unit, onDelete: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text("Orçamentos", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text(monthLabel(month), color = Muted, fontSize = 11.sp) }; Button(onClick = onAdd, colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg), shape = RoundedCornerShape(14.dp)) { Icon(Icons.Filled.Add, null); Text(" Limite") } }; LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 100.dp)) { if (budgets.isEmpty()) item { EmptyHint("Defina um limite mensal para alimentação, transporte, lazer e outras categorias.", "Criar orçamento", onAdd) }; items(budgets.keys.toList(), key = { it }) { category -> val spent = moves.filter { it.date.startsWith(month) && it.type == "Despesa" && it.category == category }.sumOf { it.amount }; Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.weight(1f)) { BudgetRow(category, spent, budgets[category] ?: 0.0, hidden) }; IconButton(onClick = { onDelete(category) }) { Icon(Icons.Filled.DeleteOutline, "Remover orçamento", tint = Muted) } } } } }
}

@Composable private fun ReportsScreen(month: String, shift: (Int) -> Unit, moves: List<Move>, hidden: Boolean) {
    val list = moves.filter { it.date.startsWith(month) }; val income = list.filter { it.type == "Receita" }.sumOf { it.amount }; val expense = list.filter { it.type == "Despesa" }.sumOf { it.amount }; val categories = list.filter { it.type == "Despesa" }.groupBy { it.category }.mapValues { e -> e.value.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    Column(Modifier.fillMaxSize().padding(horizontal = 18.dp)) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) { Column(Modifier.weight(1f)) { Text("Relatórios", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Entenda para onde seu dinheiro vai", color = Muted, fontSize = 11.sp) }; MonthPicker(month, shift) }; LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 100.dp)) { item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { MetricCard("Receitas", income, Green, Icons.Filled.TrendingUp, hidden, Modifier.weight(1f)); MetricCard("Despesas", expense, Red, Icons.Filled.TrendingDown, hidden, Modifier.weight(1f)) } }; item { Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(17.dp)) { Text("Evolução das movimentações", fontWeight = FontWeight.Bold); Text("Valores diários agregados por período", color = Muted, fontSize = 10.sp); Spacer(Modifier.height(12.dp)); val groups = (0..5).map { idx -> val start = idx * 5 + 1; val end = if (idx == 5) 31 else start + 4; val filtered = list.filter { val day = it.date.takeLast(2).toIntOrNull() ?: 0; day in start..end }; Pair(filtered.filter { it.type == "Receita" }.sumOf { it.amount }, filtered.filter { it.type == "Despesa" }.sumOf { it.amount }) }; ReportChart(groups); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("1–5", "6–10", "11–15", "16–20", "21–25", "26–31").forEach { Text(it, color = Muted, fontSize = 9.sp) } } } } }; item { Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(17.dp)) { Text("Despesas por categoria", fontWeight = FontWeight.Bold); Text("Distribuição dos gastos do mês", color = Muted, fontSize = 10.sp); Spacer(Modifier.height(12.dp)); if (categories.isEmpty()) EmptyHint("Ainda não há despesas para mostrar.", "", {}) else { Row(verticalAlignment = Alignment.CenterVertically) { Donut(categories.map { it.second }); Spacer(Modifier.width(17.dp)); Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) { categories.take(5).forEachIndexed { i, item -> Row(verticalAlignment = Alignment.CenterVertically) { LegendDot(chartColors[i % chartColors.size]); Column(Modifier.weight(1f)) { Text(item.first, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(if (hidden) "••••" else money(item.second), color = Muted, fontSize = 10.sp) } } } } } } } } }; item { Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(17.dp)) { Text("Resumo do período", fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp)); ReportLine("Resultado líquido", if (hidden) "••••" else money(income - expense), if (income >= expense) Green else Red); ReportLine("Movimentações", list.size.toString()); ReportLine("Categorias com gastos", categories.size.toString()); ReportLine("Maior categoria", categories.firstOrNull()?.first ?: "—") } } } } }
}

private val chartColors = listOf(Green, Blue, Orange, Color(0xFFC9A0FF), Red, Color(0xFF79D6D1))
@Composable private fun ReportLine(label: String, value: String, tint: Color = Muted) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable private fun ReportChart(groups: List<Pair<Double, Double>>) { Canvas(Modifier.fillMaxWidth().height(155.dp)) { for (i in 0..3) drawLine(Border, Offset(0f, size.height * i / 3), Offset(size.width, size.height * i / 3), 1f); val maxV = max(1.0, groups.maxOfOrNull { max(it.first, it.second) } ?: 1.0); fun pathFor(selector: (Pair<Double, Double>) -> Double): Path { val p = Path(); groups.forEachIndexed { i, g -> val x = size.width * i / (groups.size - 1).coerceAtLeast(1); val y = size.height - (selector(g) / maxV).toFloat() * (size.height * .83f) - 6f; if (i == 0) p.moveTo(x, y) else { val prevX = size.width * (i - 1) / (groups.size - 1).coerceAtLeast(1); val prevY = size.height - (selector(groups[i - 1]) / maxV).toFloat() * (size.height * .83f) - 6f; val mid = (prevX + x) / 2; p.cubicTo(mid, prevY, mid, y, x, y) } }; return p }; drawPath(pathFor { it.first }, Green, style = Stroke(3f, cap = StrokeCap.Round)); drawPath(pathFor { it.second }, Red, style = Stroke(3f, cap = StrokeCap.Round)) } }
@Composable private fun Donut(values: List<Double>) { Canvas(Modifier.size(122.dp)) { val total = values.sum().coerceAtLeast(1.0); var start = -90f; values.forEachIndexed { i, v -> val sweep = (v / total * 360).toFloat(); drawArc(chartColors[i % chartColors.size], start, sweep - 2f, false, topLeft = Offset(8f, 8f), size = Size(size.width - 16f, size.height - 16f), style = Stroke(width = 22f, cap = StrokeCap.Butt)); start += sweep } } }

@Composable private fun MoreScreen(onSelect: (String) -> Unit, recurringCount: Int, goalsCount: Int, budgetsCount: Int) { LazyColumn(contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { item { Text("Mais ferramentas", fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Tudo para manter suas finanças organizadas", color = Muted, fontSize = 11.sp) }; item { MoreItem("Contas fixas", "Pagamentos recorrentes e vencimentos", Icons.Filled.EventRepeat, "$recurringCount cadastradas") { onSelect("Fixas") } }; item { MoreItem("Metas financeiras", "Acompanhe suas reservas e objetivos", Icons.Filled.Flag, "$goalsCount metas") { onSelect("Metas") } }; item { MoreItem("Orçamentos", "Limites mensais por categoria", Icons.Filled.PieChart, "$budgetsCount categorias") { onSelect("Orçamentos") } }; item { MoreItem("Assistente financeiro", "Orientações com base nos seus lançamentos", Icons.Filled.Lightbulb, "Local e gratuito") { onSelect("Assistente") } }; item { MoreItem("Importar compras", "Ler notificações da Samsung Wallet", Icons.Filled.NotificationsActive, "Sem API paga") { onSelect("Notificações") } }; item { MoreItem("Segurança e privacidade", "Ocultar saldos e configurar PIN", Icons.Filled.Security, "Proteção local") { onSelect("Segurança") } }; item { Surface(color = Panel, shape = RoundedCornerShape(20.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(17.dp)) { Text("Sobre o Meu Cofre", fontWeight = FontWeight.Bold); Spacer(Modifier.height(5.dp)); Text("Cofre Premium · Controle financeiro pessoal", color = Muted, fontSize = 11.sp); Text("Seus dados ficam armazenados localmente neste aparelho.", color = Muted, fontSize = 11.sp) } } } } }
@Composable private fun WalletImportScreen(
    pendingJson: String?,
    accounts: List<Account>,
    onEnable: () -> Unit,
    onImport: (String, String, String) -> Unit,
    onDismissPending: () -> Unit
) {
    val pending = remember(pendingJson) { try { pendingJson?.let { JSONObject(it) } } catch (_: Exception) { null } }
    var title by remember(pendingJson) { mutableStateOf(pending?.optString("text")?.takeIf { it.isNotBlank() } ?: pending?.optString("title", "Compra Samsung Wallet")?.takeIf { it.isNotBlank() } ?: "Compra Samsung Wallet") }
    var amount by remember(pendingJson) { mutableStateOf(pending?.optString("amount", "") ?: "") }
    var account by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Importação por notificação", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("O Meu Cofre pode ler notificações autorizadas no Android. A leitura funciona somente quando a Samsung Wallet e o emissor disponibilizam o conteúdo da compra.", color = Muted, fontSize = 12.sp)
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("1. Ative o acesso às notificações", fontWeight = FontWeight.SemiBold)
                Text("Nas configurações do Android, habilite o acesso do Meu Cofre às notificações. Essa permissão permite que o app leia notificações do aparelho; os dados ficam armazenados localmente.", color = Muted, fontSize = 11.sp)
                Button(onClick = onEnable, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Abrir configurações") }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("2. Compra detectada", fontWeight = FontWeight.SemiBold)
                if (pending == null) {
                    Text("Ainda não há uma notificação de compra aguardando confirmação. Faça uma compra de teste ou aguarde uma notificação da Samsung Wallet.", color = Muted, fontSize = 12.sp)
                } else {
                    Text("Conteúdo recebido: ${pending.optString("text", "").ifBlank { "A notificação não trouxe uma descrição legível." }}", color = Muted, fontSize = 11.sp)
                    Field(title, { title = it }, "Estabelecimento / descrição")
                    Field(amount, { amount = it }, "Valor da compra (R$)", KeyboardType.Decimal)
                    ChoiceRow("Conta de destino", accounts.map { it.name }, accounts.firstOrNull { it.id == account }?.name ?: "") { selected -> account = accounts.firstOrNull { it.name == selected }?.id ?: account }
                    Button(onClick = { onImport(title, amount, account) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Confirmar e adicionar despesa") }
                    TextButton(onClick = onDismissPending) { Text("Ignorar esta notificação", color = Red) }
                }
            }
        }
        Text("Privacidade: o Meu Cofre não acessa os dados internos do cartão nem realiza pagamentos. Ele usa apenas o texto que o Android disponibiliza na notificação.", color = Muted, fontSize = 10.sp)
    }
}

private data class AdviceMessage(val question: String, val answer: String, val title: String = "Análise do seu dinheiro", val severity: String = "info")

private fun financialAnswer(question: String, month: String, moves: List<Move>, recurring: List<Recurring>, goals: List<Goal>, budgets: Map<String, Double>, balance: Double): String {
    val monthMoves = moves.filter { it.date.startsWith(month) }
    val income = monthMoves.filter { it.type == "Receita" }.sumOf { it.amount }
    val expense = monthMoves.filter { it.type == "Despesa" }.sumOf { it.amount }
    val fixedItems = recurring.filter { month !in it.paidMonths }
    val fixed = fixedItems.sumOf { it.amount }
    val categories = monthMoves.filter { it.type == "Despesa" }.groupBy { it.category }.mapValues { (_, list) -> list.sumOf { it.amount } }.toList().sortedByDescending { it.second }
    val normalized = java.text.Normalizer.normalize(question.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
    val alerts = budgets.mapNotNull { (category, limit) ->
        val spent = monthMoves.filter { it.type == "Despesa" && it.category.equals(category, true) }.sumOf { it.amount }
        if (limit > 0 && spent >= limit) "$category: ${money(spent)} de ${money(limit)} (${(spent / limit * 100).toInt()}%)." else if (limit > 0 && spent >= limit * .8) "$category já consumiu ${(spent / limit * 100).toInt()}% do limite." else null
    }
    val amountMatch = Regex("(?:r\\$\\s*)?([0-9]{1,7}(?:[.,][0-9]{1,2})?)").find(normalized)
    val rawAmount = amountMatch?.groupValues?.get(1)
    val requestedAmount = rawAmount?.let { if (it.contains(',')) it.replace(".", "").replace(',', '.').toDoubleOrNull() else it.toDoubleOrNull() }
    val monthDate = try { SimpleDateFormat("yyyy-MM", Locale.US).parse(month) } catch (_: Exception) { null }
    val cal = Calendar.getInstance().apply { if (monthDate != null) time = monthDate }
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentMonth = currentMonth()
    val elapsedDays = if (month == currentMonth) Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(1) else daysInMonth
    val projected = if (expense > 0) expense / elapsedDays * daysInMonth else 0.0
    val previousMonth = shiftMonth(month, -1)
    val previousExpense = moves.filter { it.date.startsWith(previousMonth) && it.type == "Despesa" }.sumOf { it.amount }
    val previousIncome = moves.filter { it.date.startsWith(previousMonth) && it.type == "Receita" }.sumOf { it.amount }
    val categoryMatch = categories.firstOrNull { (cat, _) -> val plain = java.text.Normalizer.normalize(cat.lowercase(Locale.ROOT), java.text.Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), ""); normalized.contains(plain) }
    val dueList = fixedItems.sortedBy { it.day }.take(5).joinToString { "${it.title} (${money(it.amount)}, dia ${it.day})" }
    return when {
        (normalized.contains("posso gastar") || normalized.contains("disponivel") || normalized.contains("livre") || normalized.contains("sobrou")) && requestedAmount != null -> {
            val spendable = balance - fixed
            val afterPurchase = spendable - requestedAmount
            if (afterPurchase >= 0) "Considerando o saldo registrado de ${money(balance)} e ${money(fixed)} em contas fixas ainda não marcadas como pagas, a estimativa livre é ${money(spendable)}. Uma compra de ${money(requestedAmount)} deixaria ${money(afterPurchase)}. Isso só é seguro se todos os gastos e compromissos já estiverem cadastrados."
            else "Eu teria cautela: o saldo registrado é ${money(balance)} e as contas fixas a reservar somam ${money(fixed)}, deixando uma estimativa de ${money(spendable)}. Uma compra de ${money(requestedAmount)} ultrapassaria essa margem em ${money(-afterPurchase)}."
        }
        normalized.contains("compar") || normalized.contains("mes passado") || normalized.contains("aument") || normalized.contains("diminui") -> {
            val delta = expense - previousExpense
            val pct = if (previousExpense > 0) kotlin.math.abs(delta / previousExpense * 100) else 0.0
            "Em ${monthLabel(month)}, você registrou ${money(expense)} em despesas, contra ${money(previousExpense)} em ${monthLabel(previousMonth)}. " + when { previousExpense <= 0 && expense > 0 -> "Não há despesas registradas no mês anterior para uma comparação percentual confiável."; delta > 0 -> "As despesas aumentaram ${money(delta)} (${pct.toInt()}%)."; delta < 0 -> "As despesas caíram ${money(-delta)} (${pct.toInt()}%). Bom sinal — confira se todos os lançamentos foram incluídos."; else -> "As despesas ficaram no mesmo valor registrado." } + " As receitas foram ${money(income)} neste mês e ${money(previousIncome)} no anterior."
        }
        categoryMatch != null -> "A categoria ${categoryMatch.first} soma ${money(categoryMatch.second)} em ${monthLabel(month)}. Ela representa ${(if (expense > 0) categoryMatch.second / expense * 100 else 0.0).toInt()}% das despesas registradas do mês. " + (budgets[categoryMatch.first]?.let { limit -> "Seu limite é ${money(limit)}; você usou ${(categoryMatch.second / limit * 100).toInt()}% dele." } ?: "Você ainda não definiu um orçamento específico para essa categoria.")
        normalized.contains("gasto") || normalized.contains("categoria") || normalized.contains("onde") || normalized.contains("maior despesa") -> {
            if (categories.isEmpty()) "Ainda não há despesas cadastradas em ${monthLabel(month)}. Quando registrar compras e contas, consigo identificar as categorias que mais pesam."
            else "Sua maior categoria é ${categories.first().first}, com ${money(categories.first().second)} (${(categories.first().second / expense * 100).toInt()}% das despesas). " + categories.take(3).joinToString("; ") { "${it.first}: ${money(it.second)}" } + ". Eu começaria revisando a maior categoria flexível, sem cortar despesas essenciais às cegas."
        }
        normalized.contains("conta") || normalized.contains("fixa") || normalized.contains("vencimento") || normalized.contains("reservar") -> "Você tem ${fixedItems.size} contas fixas ainda não marcadas como pagas, somando ${money(fixed)}. " + if (dueList.isBlank()) "Não há contas pendentes cadastradas." else "Próximas da lista: $dueList." + " Marque como paga depois de quitar para não distorcer a previsão."
        normalized.contains("orcamento") || normalized.contains("limite") -> if (alerts.isEmpty()) "Com os dados atuais, não identifiquei orçamento acima de 80% do limite. Isso não garante que todos os gastos estejam registrados. Defina limites para as categorias que quer controlar." else "Encontrei estes pontos de atenção: ${alerts.joinToString(" ")} Priorize o que ainda falta pagar e evite assumir novas despesas nessa categoria até revisar o mês."
        normalized.contains("econom") || normalized.contains("guardar") || normalized.contains("reserva") || normalized.contains("poupar") || normalized.contains("meta") -> {
            val result = income - expense
            val goal = goals.firstOrNull { it.target > it.saved }
            "Neste mês, receitas: ${money(income)}; despesas: ${money(expense)}; resultado: ${money(result)}. " + (if (result > 0) "O resultado registrado é positivo, mas primeiro reserve as contas pendentes (${money(fixed)}). Uma referência conservadora seria guardar apenas parte do que realmente sobrar após despesas essenciais." else "O resultado ainda não está positivo. Antes de fixar uma meta de poupança, revise despesas variáveis, vencimentos e possíveis lançamentos ausentes.") + (goal?.let { " Sua meta '${it.title}' precisa de ${money((it.target - it.saved).coerceAtLeast(0.0))} para ser concluída." } ?: " Você pode cadastrar uma meta para acompanhar o progresso.")
        }
        normalized.contains("projec") || normalized.contains("previs") || normalized.contains("fim do mes") -> "Até agora, as despesas registradas somam ${money(expense)}. Projetando o ritmo médio de ${money(if (elapsedDays > 0) expense / elapsedDays else 0.0)} por dia para ${daysInMonth} dias, o total estimado seria ${money(projected)}. É uma projeção simples, não uma garantia: contas que ainda vão vencer e gastos irregulares podem alterar bastante o resultado."
        normalized.contains("receita") || normalized.contains("ganh") || normalized.contains("entrada") -> "As receitas registradas em ${monthLabel(month)} somam ${money(income)} e as despesas ${money(expense)}. O resultado é ${money(income - expense)}. A análise considera apenas movimentações cadastradas no aplicativo."
        else -> "Resumo de ${monthLabel(month)}: ${money(income)} em receitas, ${money(expense)} em despesas e resultado de ${money(income - expense)}. As contas fixas não marcadas como pagas somam ${money(fixed)}. Você também pode perguntar, por exemplo: 'posso gastar R$ 100?', 'compare com o mês passado', 'quanto gastei com Alimentação?', 'como está meu orçamento?' ou 'projete meus gastos até o fim do mês'."
    }
}

@Composable private fun FinancialAssistantScreen(context: Context, month: String, moves: List<Move>, accounts: List<Account>, recurring: List<Recurring>, goals: List<Goal>, budgets: Map<String, Double>, balance: Double, hidden: Boolean) {
    var input by remember { mutableStateOf("") }
    val prefs = remember { context.getSharedPreferences("meu_cofre_premium", Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString("gemini_api_key", "") ?: "") }
    var showKeySetup by remember { mutableStateOf(apiKey.isBlank()) }
    var loading by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf<AdviceMessage>() }
    val monthly = moves.filter { it.date.startsWith(month) }
    val income = monthly.filter { it.type == "Receita" }.sumOf { it.amount }
    val expense = monthly.filter { it.type == "Despesa" }.sumOf { it.amount }
    val fixed = recurring.filter { month !in it.paidMonths }.sumOf { it.amount }
    val result = income - expense
    val insight = when {
        expense > income -> "As despesas superam as receitas registradas em ${money(expense - income)}."
        budgets.any { (category, limit) -> limit > 0 && monthly.filter { it.type == "Despesa" && it.category.equals(category, true) }.sumOf { it.amount } >= limit } -> "Pelo menos uma categoria já atingiu ou ultrapassou o orçamento."
        fixed > 0 -> "Reserve ${money(fixed)} para as contas fixas ainda não marcadas como pagas."
        result > 0 -> "O resultado do mês está positivo em ${money(result)}. Confira os vencimentos antes de separar uma reserva."
        else -> "Registre suas movimentações para receber análises mais específicas."
    }
    fun ask(question: String) {
        if (question.isBlank() || loading) return
        if (apiKey.isBlank()) { status = "Configure sua chave gratuita do Gemini para conversar com a IA generativa."; showKeySetup = true; return }
        val q = question.trim()
        input = ""
        loading = true
        status = "A IA está analisando seus dados..."
        val contextJson = JSONObject().apply {
            put("mes", monthLabel(month)); put("saldo_total", balance)
            put("movimentacoes_do_mes", JSONArray().apply { moves.filter { it.date.startsWith(month) }.takeLast(150).forEach { m -> put(JSONObject().put("descricao", m.title).put("valor", m.amount).put("tipo", m.type).put("categoria", m.category).put("data", m.date)) } })
            put("contas_fixas_pendentes", JSONArray().apply { recurring.filter { month !in it.paidMonths }.forEach { r -> put(JSONObject().put("nome", r.title).put("valor", r.amount).put("vencimento_dia", r.day).put("categoria", r.category)) } })
            put("metas", JSONArray().apply { goals.forEach { g -> put(JSONObject().put("nome", g.title).put("objetivo", g.target).put("guardado", g.saved)) } })
            put("orcamentos", JSONObject().apply { budgets.forEach { (k,v) -> put(k,v) } })
        }.toString()
        Thread {
            val answer = try { callGemini(apiKey.trim(), q, contextJson) } catch (e: Exception) { "Não consegui acessar a IA agora. Verifique sua conexão e a chave do Gemini. Detalhe: ${e.message ?: "erro de conexão"}" }
            Handler(Looper.getMainLooper()).post {
                messages.add(AdviceMessage(q, answer, "Resposta da IA generativa"))
                loading = false
                status = if (answer.startsWith("Não consegui acessar")) "Falha na conexão. Confira a chave e tente novamente." else "Resposta gerada pelo Gemini. Confira os números antes de tomar decisões."
            }
        }.start()
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(17.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lightbulb, null, tint = Green, modifier = Modifier.size(27.dp)) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) { Text("Cofre Inteligente", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Txt); Text("Seu dinheiro, analisado com contexto", color = Muted, fontSize = 11.sp) }
            Surface(color = GreenDeep, shape = CircleShape) { Text("IA", color = Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) }
        }
        Surface(color = Green, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Insights, null, tint = Color.White); Spacer(Modifier.width(8.dp)); Text("INSIGHT DO MÊS", color = Color.White.copy(alpha = .8f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
                Text(insight, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                Text("${monthLabel(month)} · análise dos lançamentos cadastrados", color = Color.White.copy(alpha = .8f), fontSize = 10.sp)
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Panorama financeiro", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AdviceMetric("Receitas", if (hidden) "••••" else money(income), Green, Modifier.weight(1f)); AdviceMetric("Despesas", if (hidden) "••••" else money(expense), Red, Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AdviceMetric("Resultado", if (hidden) "••••" else money(result), if (result >= 0) Green else Red, Modifier.weight(1f)); AdviceMetric("A reservar", if (hidden) "••••" else money(fixed), Orange, Modifier.weight(1f)) }
            }
        }
        Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AutoAwesome, null, tint = Green); Spacer(Modifier.width(8.dp)); Text("IA generativa Gemini", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); TextButton(onClick = { showKeySetup = !showKeySetup }) { Text(if (showKeySetup) "Fechar" else "Configurar", color = Green) } }
                Text("As respostas são geradas por IA usando os dados financeiros que o app enviar na pergunta.", color = Muted, fontSize = 11.sp, lineHeight = 15.sp)
                if (showKeySetup) {
                    OutlinedTextField(value = apiKey, onValueChange = { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Chave de API do Gemini") }, placeholder = { Text("Cole sua chave aqui") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(13.dp), colors = fieldColors())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { prefs.edit().putString("gemini_api_key", apiKey.trim()).apply(); showKeySetup = false; status = if (apiKey.isBlank()) "Chave removida." else "Chave salva neste aparelho." }, colors = ButtonDefaults.buttonColors(containerColor = Green)) { Text("Salvar chave") }
                        TextButton(onClick = { apiKey = ""; prefs.edit().remove("gemini_api_key").apply(); status = "Chave removida." }) { Text("Apagar", color = Red) }
                    }
                    Text("Obtenha uma chave em aistudio.google.com. Não coloque sua chave em código público. A chave fica salva localmente no aparelho; as perguntas e o resumo financeiro são enviados ao Google para gerar respostas.", color = Muted, fontSize = 10.sp, lineHeight = 14.sp)
                }
                if (status.isNotBlank()) Text(status, color = if (status.startsWith("Falha") || status.startsWith("Não")) Red else Muted, fontSize = 11.sp)
            }
        }
        Text("Perguntas rápidas", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        val suggestions = listOf("Posso gastar R$ 100 hoje?", "Compare minhas despesas com o mês passado", "Onde estou gastando mais?", "Projete meus gastos até o fim do mês", "Quais contas preciso reservar?", "Como posso economizar?")
        suggestions.chunked(2).forEach { pair -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { pair.forEach { suggestion -> Surface(Modifier.weight(1f).clickable { ask(suggestion) }, color = Panel, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.ChatBubbleOutline, null, tint = Green, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(8.dp)); Text(suggestion, modifier = Modifier.weight(1f), fontSize = 11.sp, lineHeight = 15.sp); Icon(Icons.Filled.ChevronRight, null, tint = Muted, modifier = Modifier.size(15.dp)) } } } } }
        Surface(color = Panel, shape = RoundedCornerShape(22.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) {
            Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text("Converse com o Cofre", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                OutlinedTextField(value = input, onValueChange = { input = it }, modifier = Modifier.fillMaxWidth(), enabled = !loading, placeholder = { Text("Ex.: como posso economizar este mês?", fontSize = 12.sp, color = Muted) }, trailingIcon = { IconButton(onClick = { ask(input) }, enabled = !loading) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Filled.Send, "Enviar pergunta", tint = Green) } }, shape = RoundedCornerShape(17.dp), colors = fieldColors(), minLines = 1, maxLines = 3)
                Text("A IA usa os lançamentos do mês, contas pendentes, metas e orçamentos. Ao perguntar, esse resumo é enviado ao Google Gemini.", color = Muted, fontSize = 10.sp, lineHeight = 15.sp)
            }
        }
        if (messages.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("Análises recentes", fontWeight = FontWeight.Bold, fontSize = 16.sp); Spacer(Modifier.weight(1f)); TextButton(onClick = { messages.clear() }) { Text("Limpar", color = Muted) } }
            messages.reversed().forEach { message -> Surface(color = Panel, shape = RoundedCornerShape(19.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.Lightbulb, null, tint = Green, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(7.dp)); Text(message.title, color = Txt, fontWeight = FontWeight.Bold, fontSize = 12.sp) }; Text(message.question, color = Green, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Divider(color = Border); Text(message.answer, color = Txt, fontSize = 12.sp, lineHeight = 18.sp) } } }
        }
        Spacer(Modifier.height(18.dp))
    }
}

@Composable private fun AdviceMetric(label: String, value: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(modifier, color = Card, shape = RoundedCornerShape(15.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(12.dp)) { Text(label, color = Muted, fontSize = 10.sp); Spacer(Modifier.height(5.dp)); Text(value, color = tint, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) } }
}

@Composable private fun SecurityScreen(hidden: Boolean, pinEnabled: Boolean, onHide: () -> Unit, onSetupPin: () -> Unit) { Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Segurança", fontSize = 23.sp, fontWeight = FontWeight.Bold); Text("Privacidade dos seus dados", color = Muted, fontSize = 11.sp); MoreItem("Ocultar valores", "Esconda os saldos nas telas", Icons.Filled.Visibility, if (hidden) "Ativado" else "Desativado", onHide); MoreItem("PIN de acesso", "Bloqueio simples ao abrir o app", Icons.Filled.Lock, if (pinEnabled) "Configurado" else "Não configurado", onSetupPin); Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Text("A proteção por PIN é um bloqueio local básico. Ela não substitui criptografia de banco de dados nem autenticação bancária.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp)) } } }

@Composable private fun MoreItem(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, trailing: String, onClick: () -> Unit) { Surface(Modifier.fillMaxWidth().clickable { onClick() }, color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Green) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(subtitle, color = Muted, fontSize = 10.sp) }; Text(trailing, color = Green, fontSize = 10.sp); Icon(Icons.Filled.ChevronRight, null, tint = Muted) } } }
@Composable private fun AccountMini(account: Account, value: Double, hidden: Boolean) { Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Filled.AccountBalanceWallet, null, tint = Green); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(account.name, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(account.type, color = Muted, fontSize = 10.sp) }; Text(if (hidden) "••••" else money(value), color = Green, fontWeight = FontWeight.Bold, fontSize = 12.sp) } } }
@Composable private fun SmallSummary(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) { Surface(modifier.clickable { onClick() }, color = Panel, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.padding(13.dp)) { Icon(icon, null, tint = Green); Spacer(Modifier.height(7.dp)); Text(title, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(subtitle, color = Muted, fontSize = 10.sp) } } }
@Composable private fun BudgetRow(category: String, spent: Double, limit: Double, hidden: Boolean) { val p = if (limit > 0) (spent / limit).toFloat().coerceAtLeast(0f) else 0f; val color = if (p >= 1f) Red else if (p >= .8f) Orange else Green; Surface(color = Panel, shape = RoundedCornerShape(17.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxWidth().padding(14.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text(category, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Spacer(Modifier.weight(1f)); Text("${(p * 100).toInt()}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold) }; Spacer(Modifier.height(9.dp)); LinearProgressIndicator(progress = { p.coerceAtMost(1f) }, modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = color, trackColor = Card2); Spacer(Modifier.height(7.dp)); Row { Text(if (hidden) "Gasto: ••••" else "Gasto: ${money(spent)}", color = Muted, fontSize = 10.sp); Spacer(Modifier.weight(1f)); Text(if (hidden) "Limite: ••••" else "Limite: ${money(limit)}", color = Muted, fontSize = 10.sp) } } } }
@Composable private fun SectionTitle(title: String, subtitle: String) { Column { Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(subtitle, color = Muted, fontSize = 10.sp) } }
@Composable private fun EmptyHint(message: String, action: String, onAction: () -> Unit) { Surface(color = Panel, shape = RoundedCornerShape(18.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Column(Modifier.fillMaxWidth().padding(19.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Filled.Inbox, null, tint = Muted, modifier = Modifier.size(28.dp)); Spacer(Modifier.height(8.dp)); Text(message, color = Muted, fontSize = 12.sp); if (action.isNotBlank()) TextButton(onClick = onAction) { Text(action, color = Green) } } } }
@Composable private fun LegendDot(color: Color) { Box(Modifier.padding(end = 5.dp).size(7.dp).clip(CircleShape).background(color)) }
@Composable private fun MonthPicker(month: String, shift: (Int) -> Unit) { Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(CircleShape).background(Panel).padding(horizontal = 4.dp)) { IconButton(onClick = { shift(-1) }, modifier = Modifier.size(27.dp)) { Icon(Icons.Filled.ChevronLeft, null, tint = Muted) }; Text(month.replace("-", "/"), fontSize = 10.sp); IconButton(onClick = { shift(1) }, modifier = Modifier.size(27.dp)) { Icon(Icons.Filled.ChevronRight, null, tint = Muted) } } }
private fun shiftMonth(month: String, delta: Int): String { val cal = Calendar.getInstance(); val parsed = try { SimpleDateFormat("yyyy-MM", Locale.US).parse(month) } catch (_: Exception) { null }; if (parsed != null) cal.time = parsed; cal.add(Calendar.MONTH, delta); return SimpleDateFormat("yyyy-MM", Locale.US).format(cal.time) }

@Composable private fun MoveDialog(existing: Move?, accounts: List<Account>, onDismiss: () -> Unit, onSave: (String, Double, String, String, String, String, String) -> Unit) {
    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }; var amount by remember(existing) { mutableStateOf(existing?.amount?.toString()?.replace('.', ',') ?: "") }; var type by remember(existing) { mutableStateOf(existing?.type ?: "Despesa") }; var category by remember(existing) { mutableStateOf(existing?.category ?: "Alimentação") }; var date by remember(existing) { mutableStateOf(existing?.date ?: todayIsoDate()) }; var account by remember(existing) { mutableStateOf(existing?.account ?: accounts.firstOrNull()?.id ?: "") }; var destination by remember(existing) { mutableStateOf(existing?.destination ?: accounts.firstOrNull { it.id != account }?.id ?: "") }; var error by remember { mutableStateOf("") }
    FormDialog(if (existing == null) "Nova movimentação" else "Editar movimentação", onDismiss, { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { Field(title, { title = it }, "Descrição"); Field(amount, { amount = it }, "Valor (R$)", KeyboardType.Decimal); ChoiceRow("Tipo", listOf("Despesa", "Receita", "Transferência"), type) { type = it }; ChoiceRow("Categoria", listOf("Alimentação", "Moradia", "Transporte", "Saúde", "Lazer", "Salário", "Compras", "Educação", "Contas", "Outros"), category) { category = it }; Field(date, { date = it }, "Data (AAAA-MM-DD)"); ChoiceRow("Conta", accounts.map { it.name }, accounts.firstOrNull { it.id == account }?.name ?: "") { account = accounts.firstOrNull { a -> a.name == it }?.id ?: account }; if (type == "Transferência") ChoiceRow("Conta de destino", accounts.filter { it.id != account }.map { it.name }, accounts.firstOrNull { it.id == destination }?.name ?: "") { destination = accounts.firstOrNull { a -> a.name == it }?.id ?: destination }; if (error.isNotBlank()) Text(error, color = Red, fontSize = 11.sp); Button(onClick = { val value = parseMoney(amount); if (title.isBlank() || value == null) error = "Informe uma descrição e um valor válido." else if (date.length != 10) error = "Use a data no formato AAAA-MM-DD." else if (type == "Transferência" && (destination.isBlank() || destination == account)) error = "Escolha uma conta de destino diferente." else onSave(title.trim(), value, type, category, date, account, if (type == "Transferência") destination else "") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Salvar movimentação") } } })
}

@Composable private fun AccountDialog(onDismiss: () -> Unit, onSave: (String, String, Double) -> Unit) { var name by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Corrente") }; var initial by remember { mutableStateOf("0") }; FormDialog("Nova conta", onDismiss, { Field(name, { name = it }, "Nome da conta"); ChoiceRow("Tipo", listOf("Corrente", "Poupança", "Dinheiro", "Carteira digital", "Investimento", "Outro"), type) { type = it }; Field(initial, { initial = it }, "Saldo inicial (R$)", KeyboardType.Decimal); Button(onClick = { if (name.isNotBlank()) onSave(name.trim(), type, initial.toDoubleOrNull() ?: parseMoney(initial) ?: 0.0) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Criar conta") } }) }
@Composable private fun RecurringDialog(accounts: List<Account>, onDismiss: () -> Unit, onSave: (String, Double, Int, String, String) -> Unit) { var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var day by remember { mutableStateOf("10") }; var category by remember { mutableStateOf("Moradia") }; var account by remember { mutableStateOf(accounts.firstOrNull()?.id ?: "") }; FormDialog("Nova conta fixa", onDismiss, { Field(title, { title = it }, "Nome da conta"); Field(amount, { amount = it }, "Valor (R$)", KeyboardType.Decimal); Field(day, { day = it }, "Dia do vencimento (1 a 31)", KeyboardType.Number); ChoiceRow("Categoria", listOf("Moradia", "Contas", "Transporte", "Saúde", "Educação", "Assinaturas", "Outros"), category) { category = it }; ChoiceRow("Conta de pagamento", accounts.map { it.name }, accounts.firstOrNull { it.id == account }?.name ?: "") { account = accounts.firstOrNull { a -> a.name == it }?.id ?: account }; Button(onClick = { val value = parseMoney(amount); val d = day.toIntOrNull(); if (title.isNotBlank() && value != null && d != null && d in 1..31) onSave(title, value, d, category, account) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Salvar conta fixa") } }) }
@Composable private fun GoalDialog(existing: Goal?, onDismiss: () -> Unit, onSave: (String, Double, Double) -> Unit, onAddSaved: (Goal, Double) -> Unit) { var title by remember(existing) { mutableStateOf(existing?.title ?: "") }; var target by remember(existing) { mutableStateOf(existing?.target?.toString()?.replace('.', ',') ?: "") }; var saved by remember(existing) { mutableStateOf(existing?.saved?.toString()?.replace('.', ',') ?: "0") }; var deposit by remember { mutableStateOf("") }; FormDialog(if (existing == null) "Nova meta" else "Editar meta", onDismiss, { Field(title, { title = it }, "Nome da meta"); Field(target, { target = it }, "Valor objetivo (R$)", KeyboardType.Decimal); Field(saved, { saved = it }, "Valor já guardado (R$)", KeyboardType.Decimal); Button(onClick = { val t = parseMoney(target); val s = saved.replace(".", "").replace(',', '.').toDoubleOrNull() ?: 0.0; if (title.isNotBlank() && t != null && s >= 0) onSave(title, t, s) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Salvar meta") } }) }
@Composable private fun BudgetDialog(existing: List<String>, onDismiss: () -> Unit, onSave: (String, Double) -> Unit) { var category by remember { mutableStateOf(existing.firstOrNull() ?: "Alimentação") }; var amount by remember { mutableStateOf("") }; FormDialog("Definir orçamento", onDismiss, { ChoiceRow("Categoria", listOf("Alimentação", "Moradia", "Transporte", "Saúde", "Lazer", "Compras", "Educação", "Contas", "Assinaturas", "Outros"), category) { category = it }; Field(amount, { amount = it }, "Limite mensal (R$)", KeyboardType.Decimal); Button(onClick = { val v = parseMoney(amount); if (v != null) onSave(category, v) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Salvar orçamento") } }) }
@Composable private fun SecurityDialog(pinEnabled: Boolean, onDismiss: () -> Unit, onSavePin: (String) -> Unit) { var pin by remember { mutableStateOf("") }; var confirm by remember { mutableStateOf("") }; var remove by remember { mutableStateOf(false) }; FormDialog("Segurança do aplicativo", onDismiss, { Text("Configure um PIN de 4 a 8 números para bloquear a abertura do app.", color = Muted, fontSize = 12.sp); if (pinEnabled) TextButton(onClick = { remove = true; pin = ""; confirm = "" }) { Text("Remover PIN", color = Red) }; if (!remove) { Field(pin, { pin = it.filter(Char::isDigit).take(8) }, "Novo PIN", KeyboardType.Number, true); Field(confirm, { confirm = it.filter(Char::isDigit).take(8) }, "Confirmar PIN", KeyboardType.Number, true) }; Button(onClick = { if (remove) onSavePin("") else if (pin.length in 4..8 && pin == confirm) onSavePin(pin) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text(if (remove) "Confirmar remoção" else "Salvar PIN") } }) }

@Composable private fun PinGate(onUnlock: (String) -> Boolean) { var pin by remember { mutableStateOf("") }; var error by remember { mutableStateOf(false) }; Column(Modifier.fillMaxSize().background(Bg).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Box(Modifier.size(76.dp).clip(RoundedCornerShape(24.dp)).background(GreenDeep), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Lock, null, tint = Green, modifier = Modifier.size(34.dp)) }; Spacer(Modifier.height(20.dp)); Text("Meu Cofre", fontSize = 27.sp, fontWeight = FontWeight.Bold); Text("Digite seu PIN para continuar", color = Muted); Spacer(Modifier.height(22.dp)); OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("PIN") }, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true, shape = RoundedCornerShape(16.dp), colors = fieldColors()); if (error) Text("PIN incorreto. Tente novamente.", color = Red, fontSize = 12.sp); Spacer(Modifier.height(12.dp)); Button(onClick = { error = !onUnlock(pin) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Green, contentColor = Bg)) { Text("Desbloquear") } } }

@Composable private fun FormDialog(title: String, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) { AlertDialog(onDismissRequest = onDismiss, containerColor = Card, title = { Text(title, fontWeight = FontWeight.Bold, color = Txt) }, text = { Column(Modifier.fillMaxWidth().heightIn(max = 490.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp), content = content) }, confirmButton = {}, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Muted) } }) }
@Composable private fun Field(value: String, onValue: (String) -> Unit, label: String, keyboard: KeyboardType = KeyboardType.Text, password: Boolean = false) { OutlinedTextField(value, onValue, Modifier.fillMaxWidth(), label = { Text(label, fontSize = 12.sp) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboard), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None, shape = RoundedCornerShape(14.dp), colors = fieldColors()) }
@Composable private fun ChoiceRow(label: String, options: List<String>, selected: String, onSelect: (String) -> Unit) { var expanded by remember { mutableStateOf(false) }; Column { Text(label, color = Muted, fontSize = 11.sp); Box { Surface(Modifier.fillMaxWidth().clickable { expanded = true }, color = Panel, shape = RoundedCornerShape(14.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Border)) { Row(Modifier.padding(horizontal = 13.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) { Text(selected.ifBlank { "Selecione" }, Modifier.weight(1f), fontSize = 13.sp); Icon(Icons.Filled.ExpandMore, null, tint = Muted) } }; DropdownMenu(expanded, { expanded = false }, modifier = Modifier.background(Card)) { options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false }) } } } } }
@Composable private fun fieldColors() = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, unfocusedBorderColor = Border, focusedLabelColor = Green, unfocusedLabelColor = Muted, focusedTextColor = Txt, unfocusedTextColor = Txt, cursorColor = Green, focusedContainerColor = Panel, unfocusedContainerColor = Panel)


private fun callGemini(apiKey: String, question: String, financialContext: String): String {
    val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.8-flash:generateContent?key=${java.net.URLEncoder.encode(apiKey, "UTF-8")}")
    val prompt = """Você é o Cofre Inteligente, um assistente de educação e organização financeira pessoal que fala português brasileiro de forma acolhedora, clara e prática. Responda à pergunta usando os dados JSON abaixo como fonte principal. Nunca invente valores, movimentações ou contas; se faltarem dados, diga isso. Diferencie saldo de resultado mensal, não trate contas pendentes como já pagas, explique cálculos de forma simples e evite recomendar investimentos específicos ou garantir resultados. Se perguntarem se podem fazer uma compra, estime com cautela e ressalte que os lançamentos podem estar incompletos. Não solicite senhas bancárias, tokens ou dados sensíveis. Seja direto, com listas curtas quando ajudarem.

DADOS FINANCEIROS DO APLICATIVO (JSON):
$financialContext

PERGUNTA DO USUÁRIO:
$question"""
    val payload = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
        .put("generationConfig", JSONObject().put("temperature", 0.4).put("maxOutputTokens", 900))
    val connection = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 20000
        readTimeout = 45000
        doOutput = true
        setRequestProperty("Content-Type", "application/json; charset=UTF-8")
    }
    try {
        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(payload.toString()) }
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (code !in 200..299) {
            val detail = try { JSONObject(body).optJSONObject("error")?.optString("message") ?: "HTTP $code" } catch (_: Exception) { "HTTP $code" }
            throw IllegalStateException(detail.take(220))
        }
        val root = JSONObject(body)
        val candidates = root.optJSONArray("candidates") ?: throw IllegalStateException("A IA não retornou uma resposta. Tente novamente.")
        val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
            ?: throw IllegalStateException("A IA retornou uma resposta vazia.")
        return (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text")?.takeIf(String::isNotBlank) }.joinToString("\n\n").ifBlank { "A IA retornou uma resposta vazia. Tente reformular a pergunta." }
    } finally { connection.disconnect() }
}

private fun movesToJson(list: List<Move>) = JSONArray().apply { list.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("amount", it.amount).put("type", it.type).put("category", it.category).put("date", it.date).put("account", it.account).put("destination", it.destination)) } }.toString()
private fun accountsToJson(list: List<Account>) = JSONArray().apply { list.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("type", it.type).put("initial", it.initial)) } }.toString()
private fun recurringToJson(list: List<Recurring>) = JSONArray().apply { list.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("amount", it.amount).put("day", it.day).put("category", it.category).put("account", it.account).put("paid", JSONArray(it.paidMonths.toList()))) } }.toString()
private fun goalsToJson(list: List<Goal>) = JSONArray().apply { list.forEach { put(JSONObject().put("id", it.id).put("title", it.title).put("target", it.target).put("saved", it.saved)) } }.toString()
private fun budgetsToJson(map: Map<String, Double>) = JSONObject().apply { map.forEach { (k, v) -> put(k, v) } }.toString()
private fun loadMoves(p: android.content.SharedPreferences): List<Move> = try { val a = JSONArray(p.getString("movimentos", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); Move(o.optString("id", newFinanceId()), o.optString("title"), o.optDouble("amount"), o.optString("type", "Despesa"), o.optString("category", "Outros"), o.optString("date", todayIsoDate()), o.optString("account"), o.optString("destination")) } } catch (_: Exception) { emptyList() }
private fun loadAccounts(p: android.content.SharedPreferences): List<Account> = try { val a = JSONArray(p.getString("contas", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); Account(o.optString("id", newFinanceId()), o.optString("name"), o.optString("type"), o.optDouble("initial")) } } catch (_: Exception) { emptyList() }
private fun loadRecurring(p: android.content.SharedPreferences): List<Recurring> = try { val a = JSONArray(p.getString("recorrencias", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); val paid = o.optJSONArray("paid") ?: JSONArray(); Recurring(o.optString("id", newFinanceId()), o.optString("title"), o.optDouble("amount"), o.optInt("day", 10), o.optString("category"), o.optString("account"), (0 until paid.length()).map { i -> paid.optString(i) }.toSet()) } } catch (_: Exception) { emptyList() }
private fun loadGoals(p: android.content.SharedPreferences): List<Goal> = try { val a = JSONArray(p.getString("metas", "[]")); (0 until a.length()).map { val o = a.getJSONObject(it); Goal(o.optString("id", newFinanceId()), o.optString("title"), o.optDouble("target"), o.optDouble("saved")) } } catch (_: Exception) { emptyList() }
private fun loadBudgets(p: android.content.SharedPreferences): Map<String, Double> = try { val o = JSONObject(p.getString("orcamentos", "{}")); o.keys().asSequence().associateWith { o.optDouble(it) } } catch (_: Exception) { emptyMap() }
