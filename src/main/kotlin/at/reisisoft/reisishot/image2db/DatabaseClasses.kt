package at.reisisoft.reisishot.image2db

import java.math.BigDecimal
import java.sql.Date

data class DbCamera(val id: Int, val manufacturer: String, val model: String, val cropFactor: BigDecimal)



data class DbImage(val fileName: String, val camera: DbCamera, val iso: Int, val av: BigDecimal, val tv: BigDecimal,
                   val lens: String, val date: Date)