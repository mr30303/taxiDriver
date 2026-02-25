package com.lnk.app.navigation

sealed class Route(val route: String) {
    data object AppStart : Route("app_start")
    data object Login : Route("login")
    data object SignUp : Route("signup")
    data object Main : Route("main")
    data object SalarySetting : Route("salary_setting")
    data object DailySalesInput : Route("daily_sales_input")
    data object SalaryResult : Route("salary_result")
    data object ToiletMap : Route("toilet_map")
    data object ToiletDetail : Route("toilet_detail")
    data object AddToilet : Route("add_toilet")
    data object Comment : Route("comment")
}
