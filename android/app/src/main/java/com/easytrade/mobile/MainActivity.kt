package com.easytrade.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.gson.Gson
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val API_BASE_URL = "https://easytrade-backend-zrxm.onrender.com/"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EasyTradeTheme { EasyTradeApp() } }
    }
}

interface EasyTradeApi {
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @GET("api/stocks/search")
    suspend fun searchStock(@Header("Authorization") auth: String, @Query("query") query: String): StockResponse

    @GET("api/stocks/{code}")
    suspend fun stock(@Header("Authorization") auth: String, @Path("code") code: String): StockResponse

    @GET("api/stocks/{code}/chart")
    suspend fun chart(
        @Header("Authorization") auth: String,
        @Path("code") code: String,
        @Query("period") period: String,
    ): ChartResponse

    @GET("api/stocks/popular")
    suspend fun popularStocks(@Header("Authorization") auth: String): List<StockResponse>

    @GET("api/stocks/market")
    suspend fun marketStocks(
        @Header("Authorization") auth: String,
        @Query("query") query: String? = null,
        @Query("limit") limit: Int = 60,
    ): List<StockResponse>

    @GET("api/stocks/ranking")
    suspend fun rankingStocks(
        @Header("Authorization") auth: String,
        @Query("type") type: String,
        @Query("limit") limit: Int = 20,
    ): List<StockResponse>

    @POST("api/trades/buy")
    suspend fun buy(@Header("Authorization") auth: String, @Body request: TradeRequest): TradeResponse

    @POST("api/trades/sell")
    suspend fun sell(@Header("Authorization") auth: String, @Body request: TradeRequest): TradeResponse

    @GET("api/trades")
    suspend fun trades(@Header("Authorization") auth: String): List<TradeResponse>

    @GET("api/portfolio")
    suspend fun portfolio(@Header("Authorization") auth: String): PortfolioResponse

    @GET("api/watchlist")
    suspend fun watchlist(@Header("Authorization") auth: String): List<StockResponse>

    @POST("api/watchlist/{code}")
    suspend fun addWatchlist(@Header("Authorization") auth: String, @Path("code") code: String): StockResponse

    @DELETE("api/watchlist/{code}")
    suspend fun removeWatchlist(@Header("Authorization") auth: String, @Path("code") code: String)
}

object ApiClient {
    private val logger = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logger)
        .build()

    val api: EasyTradeApi = Retrofit.Builder()
        .baseUrl(API_BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(EasyTradeApi::class.java)
}

data class RegisterRequest(val name: String, val email: String, val password: String, val nickname: String)
data class LoginRequest(val email: String, val password: String)
data class LoginResponse(val token: String, val user: UserResponse)
data class UserResponse(val id: Long, val name: String, val email: String, val nickname: String, val balance: BigDecimal)
data class StockResponse(val code: String, val name: String, val price: BigDecimal, val changeRate: BigDecimal)
data class ChartResponse(val code: String, val name: String, val period: String, val points: List<ChartPoint>)
data class ChartPoint(val label: String, val price: BigDecimal)
data class TradeRequest(val stockCode: String, val quantity: Int)
data class TradeResponse(
    val id: Long,
    val type: String,
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val price: BigDecimal,
    val totalAmount: BigDecimal,
    val tradedAt: String,
)
data class PortfolioResponse(
    val cashBalance: BigDecimal,
    val stockValue: BigDecimal,
    val totalAsset: BigDecimal,
    val holdings: List<HoldingResponse>,
)
data class HoldingResponse(
    val stockCode: String,
    val stockName: String,
    val quantity: Int,
    val averagePrice: BigDecimal,
    val currentPrice: BigDecimal,
    val currentValue: BigDecimal,
    val profitLoss: BigDecimal,
    val profitRate: BigDecimal,
)
data class ErrorResponse(val message: String?)

class AppState(private val api: EasyTradeApi = ApiClient.api) {
    var token by mutableStateOf<String?>(null)
    var user by mutableStateOf<UserResponse?>(null)
    var selectedStock by mutableStateOf<StockResponse?>(null)
    var chart by mutableStateOf<ChartResponse?>(null)
    var portfolio by mutableStateOf<PortfolioResponse?>(null)
    var loading by mutableStateOf(false)
    var message by mutableStateOf("")
    val popularStocks = mutableStateListOf<StockResponse>()
    val marketStocks = mutableStateListOf<StockResponse>()
    val trades = mutableStateListOf<TradeResponse>()
    val watchlist = mutableStateListOf<StockResponse>()

    private fun auth() = "Bearer ${token.orEmpty()}"

    suspend fun register(name: String, email: String, password: String, nickname: String) = call {
        api.register(RegisterRequest(name, email, password, nickname))
        true
    }

    suspend fun login(email: String, password: String) = call {
        val response = api.login(LoginRequest(email, password))
        token = response.token
        user = response.user
        loadHome()
        true
    }

    suspend fun loadHome() = call {
        popularStocks.replace(api.popularStocks(auth()))
        portfolio = api.portfolio(auth())
        true
    }

    suspend fun loadMarket(query: String? = null) = call {
        marketStocks.replace(api.marketStocks(auth(), query?.takeIf { it.isNotBlank() }, 10))
        true
    }

    suspend fun loadRanking(type: String) = call {
        marketStocks.replace(api.rankingStocks(auth(), type, 20))
        true
    }

    suspend fun search(query: String) = call {
        selectedStock = api.searchStock(auth(), query)
        true
    }

    suspend fun loadStock(code: String) = call {
        selectedStock = api.stock(auth(), code)
        true
    }

    suspend fun loadChart(code: String, period: String) = call {
        chart = api.chart(auth(), code, period)
        true
    }

    suspend fun buy(code: String, quantity: Int) = call {
        api.buy(auth(), TradeRequest(code, quantity))
        portfolio = api.portfolio(auth())
        loadTrades()
        true
    }

    suspend fun sell(code: String, quantity: Int) = call {
        api.sell(auth(), TradeRequest(code, quantity))
        portfolio = api.portfolio(auth())
        loadTrades()
        true
    }

    suspend fun loadPortfolio() = call {
        portfolio = api.portfolio(auth())
        true
    }

    suspend fun loadTrades() = call {
        trades.replace(api.trades(auth()))
        true
    }

    suspend fun loadWatchlist() = call {
        watchlist.replace(api.watchlist(auth()))
        true
    }

    suspend fun toggleWatch(stock: StockResponse) = call {
        if (watchlist.any { it.code == stock.code }) {
            api.removeWatchlist(auth(), stock.code)
            watchlist.removeAll { it.code == stock.code }
            message = "관심 종목에서 삭제했습니다."
        } else {
            val saved = api.addWatchlist(auth(), stock.code)
            watchlist.removeAll { it.code == saved.code }
            watchlist.add(saved)
            message = "관심 종목에 추가했습니다."
        }
        true
    }

    private suspend fun call(block: suspend () -> Boolean): Boolean {
        loading = true
        message = ""
        return try {
            block()
        } catch (e: Exception) {
            message = parseError(e)
            false
        } finally {
            loading = false
        }
    }

    private fun parseError(e: Exception): String {
        if (e is HttpException) {
            val body = e.response()?.errorBody()?.string()
            val parsed = runCatching { Gson().fromJson(body, ErrorResponse::class.java) }.getOrNull()
            return parsed?.message ?: "서버 오류가 발생했습니다."
        }
        if (e is SocketTimeoutException) {
            return "API 응답 시간이 초과되었습니다. 백엔드가 실행 중인지, KIS API fallback이 동작하는지 확인해주세요."
        }
        if (e is ConnectException) {
            return "백엔드 서버에 연결할 수 없습니다. 에뮬레이터는 10.0.2.2:8080, 실제 기기는 PC의 LAN IP를 사용해야 합니다."
        }
        if (e is UnknownHostException) {
            return "서버 주소를 찾을 수 없습니다. API_BASE_URL 설정을 확인해주세요."
        }
        return e.message ?: "요청 처리 중 오류가 발생했습니다."
    }
}

private fun <T> MutableList<T>.replace(items: List<T>) {
    clear()
    addAll(items)
}

@Composable
fun EasyTradeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF155EEF),
            secondary = Color(0xFF475467),
            background = Color(0xFFF3F6FA),
            surface = Color.White,
        ),
        content = content,
    )
}

@Composable
fun EasyTradeApp() {
    val nav = rememberNavController()
    val state = remember { AppState() }

    NavHost(navController = nav, startDestination = "login") {
        composable("login") { LoginScreen(state, nav) }
        composable("register") { RegisterScreen(state, nav) }
        composable("home") { HomeScreen(state, nav) }
        composable("search") { SearchScreen(state, nav) }
        composable("portfolio") { PortfolioScreen(state, nav) }
        composable("watchlist") { WatchlistScreen(state, nav) }
        composable("ranking") { RankingScreen(state, nav) }
        composable("history") { TradeHistoryScreen(state, nav) }
        composable("return") { ReturnScreen(state, nav) }
        composable("stock/{code}", listOf(navArgument("code") { type = NavType.StringType })) {
            StockDetailScreen(state, nav, it.arguments?.getString("code").orEmpty())
        }
        composable("buy/{code}", listOf(navArgument("code") { type = NavType.StringType })) {
            BuyScreen(state, nav, it.arguments?.getString("code").orEmpty())
        }
        composable("sell/{code}", listOf(navArgument("code") { type = NavType.StringType })) {
            SellScreen(state, nav, it.arguments?.getString("code").orEmpty())
        }
    }
}

@Composable
fun LoginScreen(state: AppState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    AuthScaffold("EasyTrade") {
        Input("Email", email) { email = it }
        Password("Password", password) { password = it }
        Status(state)
        Button(
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    when {
                        email.isBlank() || password.isBlank() -> state.message = "이메일과 비밀번호를 입력해주세요."
                        state.login(email, password) -> nav.navigate("home") { popUpTo("login") { inclusive = true } }
                    }
                }
            },
        ) { Text("Login") }
        OutlinedButton(onClick = { nav.navigate("register") }, modifier = Modifier.fillMaxWidth()) { Text("Register") }
    }
}

@Composable
fun RegisterScreen(state: AppState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf("") }

    AuthScaffold("Register") {
        Input("Name", name) { name = it }
        Input("Email", email) { email = it }
        Password("Password", password) { password = it }
        Input("Nickname", nickname) { nickname = it }
        Status(state)
        Button(
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    when {
                        name.isBlank() || email.isBlank() || password.isBlank() || nickname.isBlank() -> state.message = "모든 항목을 입력해주세요."
                        state.register(name, email, password, nickname) -> nav.navigate("login") { popUpTo("register") { inclusive = true } }
                    }
                }
            },
        ) { Text("Create Account") }
        TextButton(onClick = { nav.popBackStack() }, modifier = Modifier.fillMaxWidth()) { Text("Back to Login") }
    }
}

@Composable
fun HomeScreen(state: AppState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { state.loadHome() }

    MainScaffold(nav, "home") {
        Header(state)
        Spacer(Modifier.height(24.dp))
        SearchRow(query, { query = it }) {
            scope.launch {
                if (query.isBlank()) state.message = "검색어를 입력해주세요."
                else if (state.search(query)) nav.navigate("stock/${state.selectedStock?.code}")
            }
        }
        Status(state)
        Spacer(Modifier.height(24.dp))
        Text("Popular Stocks", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        state.popularStocks.forEach { StockCard(it) { nav.navigate("stock/${it.code}") }; Spacer(Modifier.height(10.dp)) }
        Button(onClick = { nav.navigate("ranking") }, modifier = Modifier.fillMaxWidth()) { Text("View Korean Market") }
    }
}

@Composable
fun SearchScreen(state: AppState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { state.loadMarket() }

    MainScaffold(nav, "search") {
        Text("Stock Search", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        SearchRow(query, { query = it }) {
            scope.launch {
                if (query.isBlank()) state.message = "검색어를 입력해주세요." else state.search(query)
            }
        }
        Status(state)
        state.selectedStock?.let {
            Spacer(Modifier.height(20.dp))
            StockCard(it) { nav.navigate("stock/${it.code}") }
            Spacer(Modifier.height(12.dp))
            Button(onClick = { nav.navigate("stock/${it.code}") }, modifier = Modifier.fillMaxWidth()) { Text("View Detail") }
        }
        Spacer(Modifier.height(24.dp))
        Text("Korean Market", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("종목명 또는 6자리 종목코드로 검색할 수 있습니다.", color = Color(0xFF667085), fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        state.marketStocks
            .filter { query.isBlank() || it.code.contains(query) || it.name.contains(query, ignoreCase = true) }
            .forEach {
                StockCard(it) { nav.navigate("stock/${it.code}") }
                Spacer(Modifier.height(10.dp))
            }
    }
}

@Composable
fun StockDetailScreen(state: AppState, nav: NavHostController, code: String) {
    val scope = rememberCoroutineScope()
    var period by remember { mutableStateOf("1M") }
    LaunchedEffect(code) {
        state.loadStock(code)
        state.loadChart(code, period)
    }
    LaunchedEffect(period) { state.loadChart(code, period) }
    val stock = state.selectedStock

    MainScaffold(nav, "") {
        if (stock == null) {
            Loading(state)
            return@MainScaffold
        }
        Text(stock.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        Text("Code: ${stock.code}", color = Color(0xFF667085))
        Spacer(Modifier.height(18.dp))
        InfoCard {
            Text("Current Price", color = Color(0xFF667085))
            Text(formatWon(stock.price), fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(formatRate(stock.changeRate), color = priceColor(stock.changeRate), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        InfoCard {
            Text("Chart", color = Color(0xFF667085))
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChartPeriodButton("1D", period == "1D", Modifier.weight(1f)) { period = "1D" }
                ChartPeriodButton("1W", period == "1W", Modifier.weight(1f)) { period = "1W" }
                ChartPeriodButton("1M", period == "1M", Modifier.weight(1f)) { period = "1M" }
                ChartPeriodButton("3M", period == "3M", Modifier.weight(1f)) { period = "3M" }
            }
            Spacer(Modifier.height(10.dp))
            ChartSummary(state.chart)
        }
        Status(state)
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth()) {
            Button(
                onClick = { nav.navigate("buy/${stock.code}") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD92D20)),
            ) { Text("Buy") }
            Spacer(Modifier.width(10.dp))
            Button(
                onClick = { nav.navigate("sell/${stock.code}") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF155EEF)),
            ) { Text("Sell") }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = { scope.launch { state.toggleWatch(stock) } },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Toggle Watchlist") }
    }
}

@Composable
fun ChartPeriodButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
fun ChartSummary(chart: ChartResponse?) {
    if (chart == null || chart.points.isEmpty()) {
        Text("차트 데이터를 불러오는 중입니다.", color = Color(0xFF667085))
        return
    }

    val points = chart.points
    val min = points.minOf { it.price }
    val max = points.maxOf { it.price }
    val first = points.first()
    val last = points.last()
    val lineColor = priceColor(last.price.subtract(first.price))
    val minValue = min.toFloat()
    val range = max.subtract(min).toFloat().takeIf { it > 0f } ?: 1f

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(Color(0xFFF9FAFB), RoundedCornerShape(8.dp))
            .padding(12.dp),
    ) {
        val left = 8.dp.toPx()
        val right = 8.dp.toPx()
        val top = 12.dp.toPx()
        val bottom = 20.dp.toPx()
        val chartWidth = size.width - left - right
        val chartHeight = size.height - top - bottom
        val gridColor = Color(0xFFE4E7EC)
        val axisColor = Color(0xFFD0D5DD)

        repeat(5) { index ->
            val y = top + chartHeight * index / 4f
            drawLine(gridColor, Offset(left, y), Offset(size.width - right, y), strokeWidth = 1.dp.toPx())
        }
        drawLine(axisColor, Offset(left, top), Offset(left, top + chartHeight), strokeWidth = 1.dp.toPx())
        drawLine(axisColor, Offset(left, top + chartHeight), Offset(size.width - right, top + chartHeight), strokeWidth = 1.dp.toPx())

        val coordinates = points.mapIndexed { index, point ->
            val x = if (points.size == 1) {
                left + chartWidth / 2f
            } else {
                left + chartWidth * index / (points.size - 1).toFloat()
            }
            val normalized = (point.price.toFloat() - minValue) / range
            val y = top + chartHeight - normalized * chartHeight
            Offset(x, y)
        }

        coordinates.zipWithNext().forEach { (start, end) ->
            drawLine(lineColor, start, end, strokeWidth = 3.dp.toPx())
        }
        coordinates.forEach { point ->
            drawCircle(Color.White, radius = 4.dp.toPx(), center = point)
            drawCircle(lineColor, radius = 2.5.dp.toPx(), center = point)
        }
    }

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(first.label, color = Color(0xFF667085), fontSize = 12.sp)
        Text(last.label, color = Color(0xFF667085), fontSize = 12.sp)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("${chart.period} High ${formatWon(max)}", color = Color(0xFF667085), fontSize = 12.sp)
        Text("Low ${formatWon(min)}", color = Color(0xFF667085), fontSize = 12.sp)
    }
}

@Composable
fun BuyScreen(state: AppState, nav: NavHostController, code: String) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(code) { state.loadStock(code) }
    state.selectedStock?.let { stock ->
        TradeForm("Buy Stock", stock, "Confirm Buy", Color(0xFFD92D20), state) { quantity ->
            scope.launch { if (state.buy(stock.code, quantity)) nav.navigate("portfolio") }
        }
    } ?: LoadingScreen(state)
}

@Composable
fun SellScreen(state: AppState, nav: NavHostController, code: String) {
    val scope = rememberCoroutineScope()
    LaunchedEffect(code) { state.loadStock(code); state.loadPortfolio() }
    state.selectedStock?.let { stock ->
        val holding = state.portfolio?.holdings?.find { it.stockCode == stock.code }
        TradeForm("Sell Stock", stock, "Confirm Sell", Color(0xFF155EEF), state, "Holding Quantity: ${holding?.quantity ?: 0}주") { quantity ->
            scope.launch { if (state.sell(stock.code, quantity)) nav.navigate("portfolio") }
        }
    } ?: LoadingScreen(state)
}

@Composable
fun PortfolioScreen(state: AppState, nav: NavHostController) {
    LaunchedEffect(Unit) { state.loadPortfolio() }
    val portfolio = state.portfolio

    MainScaffold(nav, "portfolio") {
        Text("Portfolio", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(18.dp))
        if (portfolio == null) {
            Loading(state)
            return@MainScaffold
        }
        InfoCard { Text("Cash Balance", color = Color(0xFF667085)); Text(formatWon(portfolio.cashBalance), fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        InfoCard { Text("Total Asset", color = Color(0xFF667085)); Text(formatWon(portfolio.totalAsset), fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(24.dp))
        Text("Holdings", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (portfolio.holdings.isEmpty()) {
            InfoCard { Text("보유 종목이 없습니다.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        } else {
            portfolio.holdings.forEach { HoldingCard(it) { nav.navigate("stock/${it.stockCode}") }; Spacer(Modifier.height(10.dp)) }
        }
    }
}

@Composable
fun WatchlistScreen(state: AppState, nav: NavHostController) {
    LaunchedEffect(Unit) { state.loadWatchlist() }
    MainScaffold(nav, "watchlist") {
        Text("Watchlist", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Status(state)
        if (state.watchlist.isEmpty()) InfoCard { Text("관심 종목이 없습니다.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        else state.watchlist.forEach { StockCard(it) { nav.navigate("stock/${it.code}") }; Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
fun RankingScreen(state: AppState, nav: NavHostController) {
    val scope = rememberCoroutineScope()
    var rankingType by remember { mutableStateOf("rising") }
    LaunchedEffect(rankingType) { state.loadRanking(rankingType) }
    MainScaffold(nav, "ranking") {
        Text("Stock Ranking", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text("한국 시장 주요 종목을 기준별로 정렬합니다.", color = Color(0xFF667085))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RankingButton("Rising", rankingType == "rising", Modifier.weight(1f)) {
                rankingType = "rising"
                scope.launch { state.loadRanking(rankingType) }
            }
            RankingButton("Falling", rankingType == "falling", Modifier.weight(1f)) {
                rankingType = "falling"
                scope.launch { state.loadRanking(rankingType) }
            }
            RankingButton("Price", rankingType == "price", Modifier.weight(1f)) {
                rankingType = "price"
                scope.launch { state.loadRanking(rankingType) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Status(state)
        state.marketStocks.forEachIndexed { index, stock ->
            InfoCard(Modifier.clickable { nav.navigate("stock/${stock.code}") }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${index + 1}. ${stock.name}", fontWeight = FontWeight.Bold)
                    Text(formatRate(stock.changeRate), color = priceColor(stock.changeRate), fontWeight = FontWeight.Bold)
                }
                Text(stock.code, color = Color(0xFF667085))
                Text(formatWon(stock.price))
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
fun RankingButton(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
fun TradeHistoryScreen(state: AppState, nav: NavHostController) {
    LaunchedEffect(Unit) { state.loadTrades() }
    MainScaffold(nav, "history") {
        Text("Trade History", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Status(state)
        if (state.trades.isEmpty()) {
            InfoCard { Text("거래 내역이 없습니다.", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }
        } else {
            state.trades.forEach { trade ->
                InfoCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (trade.type == "BUY") "매수" else "매도", fontWeight = FontWeight.Bold)
                        Text(formatWon(trade.totalAmount), fontWeight = FontWeight.Bold)
                    }
                    Text("${trade.stockName} (${trade.stockCode})", color = Color(0xFF667085))
                    Text("${trade.quantity}주 / ${formatWon(trade.price)}", color = Color(0xFF667085))
                    Text(trade.tradedAt, color = Color(0xFF667085), fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
fun ReturnScreen(state: AppState, nav: NavHostController) {
    LaunchedEffect(Unit) { state.loadPortfolio() }
    val portfolio = state.portfolio

    MainScaffold(nav, "return") {
        Text("Return Analysis", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (portfolio == null) {
            Loading(state)
            return@MainScaffold
        }
        val profit = portfolio.holdings.fold(BigDecimal.ZERO) { sum, h -> sum.add(h.profitLoss) }
        val invested = portfolio.holdings.fold(BigDecimal.ZERO) { sum, h -> sum.add(h.averagePrice.multiply(BigDecimal.valueOf(h.quantity.toLong()))) }
        val rate = if (invested == BigDecimal.ZERO) BigDecimal.ZERO else profit.multiply(BigDecimal("100")).divide(invested, 2, RoundingMode.HALF_UP)
        InfoCard {
            Text("Total Profit/Loss", color = Color(0xFF667085))
            Text(formatWon(profit), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = priceColor(profit))
            Text("전체 수익률 ${formatRate(rate)}", color = priceColor(rate), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        portfolio.holdings.forEach { HoldingCard(it) { nav.navigate("stock/${it.stockCode}") }; Spacer(Modifier.height(10.dp)) }
    }
}

@Composable
fun TradeForm(
    title: String,
    stock: StockResponse,
    action: String,
    actionColor: Color,
    state: AppState,
    extra: String = "",
    onConfirm: (Int) -> Unit,
) {
    var quantityText by remember { mutableStateOf("") }
    val quantity = quantityText.toIntOrNull() ?: 0
    val total = stock.price.multiply(BigDecimal.valueOf(quantity.toLong()))

    Surface(color = Color(0xFFF3F6FA), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(stock.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Current Price: ${formatWon(stock.price)}", color = Color(0xFF667085))
            if (extra.isNotBlank()) Text(extra, color = Color(0xFF667085))
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it.filter(Char::isDigit) },
                label = { Text("Quantity") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            InfoCard { Text("Total Amount", color = Color(0xFF667085)); Text(formatWon(total), fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            Status(state)
            Button(
                onClick = {
                    if (quantity < 1) state.message = "수량은 1주 이상이어야 합니다." else onConfirm(quantity)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = actionColor),
                enabled = !state.loading,
            ) { Text(action) }
        }
    }
}

@Composable
fun AuthScaffold(title: String, content: @Composable () -> Unit) {
    Surface(color = Color(0xFFF3F6FA), modifier = Modifier.fillMaxSize()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 24.dp),
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    content()
                }
            }
        }
    }
}

@Composable
fun MainScaffold(nav: NavHostController, selected: String, content: @Composable () -> Unit) {
    Scaffold(bottomBar = { BottomNav(nav, selected) }) { padding ->
        Column(Modifier.fillMaxSize().background(Color(0xFFF3F6FA)).verticalScroll(rememberScrollState()).padding(padding).padding(20.dp)) {
            content()
        }
    }
}

@Composable
fun BottomNav(nav: NavHostController, selected: String) {
    NavigationBar {
        NavigationBarItem(selected == "home", { nav.navigate("home") }, icon = { Text("H") }, label = { Text("Home") })
        NavigationBarItem(selected == "search", { nav.navigate("search") }, icon = { Text("S") }, label = { Text("Search") })
        NavigationBarItem(selected == "portfolio", { nav.navigate("portfolio") }, icon = { Text("P") }, label = { Text("Portfolio") })
        NavigationBarItem(selected == "watchlist", { nav.navigate("watchlist") }, icon = { Text("W") }, label = { Text("Watch") })
        NavigationBarItem(selected == "history", { nav.navigate("history") }, icon = { Text("T") }, label = { Text("Trades") })
        NavigationBarItem(selected == "return", { nav.navigate("return") }, icon = { Text("R") }, label = { Text("Return") })
    }
}

@Composable
fun Header(state: AppState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("EasyTrade", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(state.user?.nickname ?: "User", color = Color(0xFF667085))
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("Balance", color = Color(0xFF667085), fontSize = 12.sp)
            Text(formatWon(state.portfolio?.cashBalance ?: state.user?.balance ?: BigDecimal.ZERO), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SearchRow(query: String, onChange: (String) -> Unit, onSearch: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(query, onChange, placeholder = { Text("종목명 또는 종목코드") }, modifier = Modifier.weight(1f), singleLine = true)
        Spacer(Modifier.width(8.dp))
        Button(onClick = onSearch) { Text("검색") }
    }
}

@Composable
fun Input(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
fun Password(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value, onChange, label = { Text(label) }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
fun Status(state: AppState) {
    if (state.loading) Text("처리 중입니다.", color = Color(0xFF667085), modifier = Modifier.fillMaxWidth())
    if (state.message.isNotBlank()) Text(state.message, color = Color(0xFFD92D20), modifier = Modifier.fillMaxWidth())
}

@Composable
fun LoadingScreen(state: AppState) {
    Surface(color = Color(0xFFF3F6FA), modifier = Modifier.fillMaxSize()) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Loading(state) }
    }
}

@Composable
fun Loading(state: AppState) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("데이터를 불러오는 중입니다.", color = Color(0xFF667085))
        Status(state)
    }
}

@Composable
fun StockCard(stock: StockResponse, onClick: () -> Unit) {
    InfoCard(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stock.name, fontWeight = FontWeight.Bold)
                Text(stock.code, color = Color(0xFF667085), fontSize = 13.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatWon(stock.price), fontWeight = FontWeight.Bold)
                Text(formatRate(stock.changeRate), color = priceColor(stock.changeRate))
            }
        }
    }
}

@Composable
fun HoldingCard(holding: HoldingResponse, onClick: () -> Unit) {
    InfoCard(Modifier.clickable(onClick = onClick)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(holding.stockName, fontWeight = FontWeight.Bold)
                Text("Qty: ${holding.quantity}", color = Color(0xFF667085))
            }
            Text(formatRate(holding.profitRate), color = priceColor(holding.profitRate), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(10.dp))
        Text("Avg: ${formatWon(holding.averagePrice)}", color = Color(0xFF667085))
        Text("Current: ${formatWon(holding.currentPrice)}", color = Color(0xFF667085))
        Text("Profit/Loss: ${formatWon(holding.profitLoss)}", color = priceColor(holding.profitLoss), fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

fun formatWon(value: BigDecimal): String {
    val formatter = NumberFormat.getNumberInstance(Locale.KOREA)
    return "${formatter.format(value.setScale(0, RoundingMode.HALF_UP))} KRW"
}

fun formatRate(value: BigDecimal): String = "${if (value >= BigDecimal.ZERO) "+" else ""}$value%"

fun priceColor(value: BigDecimal): Color = if (value >= BigDecimal.ZERO) Color(0xFFD92D20) else Color(0xFF155EEF)

