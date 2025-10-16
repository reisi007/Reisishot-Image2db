package at.reisisoft.reisishot.image2db

import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager

object DataBaseUtils {

    fun createDatabase(path: Path) {
        val sqlDdlText =
            BufferedReader(InputStreamReader(javaClass.getResourceAsStream("/createDb.ddl.sql"))).useLines {
                it.joinToString(" ")
            }
        val statements = sqlDdlText.split(";")

        Files.deleteIfExists(path)

        useDatabase(path) { con ->
            con.createStatement().use { statement ->
                val tableCount =
                    statements.stream().sequential().filter { !it.isBlank() }.peek { statement.addBatch(it) }.count()
                statement.executeBatch()
                println("Database created successfully! Number of tables $tableCount")
            }
        }
    }

    fun <T> useDatabase(path: Path, workerBlock: (Connection) -> T): T =
        DriverManager.getConnection("jdbc:sqlite:${path.toAbsolutePath()}").use { workerBlock.invoke(it) }

    fun getCameraModels(connection: Connection): DbCameraSet {
        val sql = "SELECT id, manufacturer,model,cropfactor FROM Camera"
        val set = DbCameraSet()

        connection.createStatement().use { statement ->
            statement.executeQuery(sql).use {
                while (it.next())
                    set += DbCamera(
                        it.getInt(1), it.getString(2),
                        it.getString(3), it.getBigDecimal(4)
                    )
            }
        }
        return set
    }
}