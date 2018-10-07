package at.reisisoft.reisishot.image2db;

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.MissingValueException
import com.xenomachina.argparser.ShowHelpException
import com.xenomachina.argparser.default
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.sql.Date
import java.sql.PreparedStatement
import java.text.SimpleDateFormat
import java.util.*

internal enum class Mode {
    CREATE, UPDATE;

    override fun toString(): String {
        return "--${super.toString().toLowerCase()}"
    }
}

internal fun String.asPath(): Path {
    return Paths.get(this)
}

object CLI {
    internal class ParsedArgs(parser: ArgParser) {
        val rootImagePaths by parser.adding(
            "--path",
            "--imagePath",
            "--rootPath",
            "-p",
            help = "The path to the images"
        ) { asPath() }

        val mode by parser.mapping(
            Mode.values().asSequence().map { it.toString() to it }.toMap(),
            "Either update a specific database or create a new one"
        )

        val databaseLocation by parser.storing(
            "--db",
            "--database",
            "-d",
            help = "The location of the database on disk"
        ) { asPath() }

        val predefinedCameras by parser.storing(
            "--predefinedCameras",
            "-c",
            help = "A property map with the crop factor associated to the camera model"
        ) {
            asPath().let { propertyPath ->
                Properties().apply {
                    load(Files.newInputStream(propertyPath))

                }
            }
        }.default(Properties())
    }

    private val dateToStringConverter by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")
    }

    private fun PreparedStatement.setSQLiteDate(parameterIndex: Int, date: Date) {
        setString(parameterIndex, dateToStringConverter.format(date))
    }


    @JvmStatic
    fun main(args: Array<String>) {
        try {
            ParsedArgs(ArgParser(args)).run {
                if (mode == Mode.CREATE) {
                    DataBaseUtils.createDatabase(databaseLocation)
                }
                //Find images & write into DB
                DataBaseUtils.useDatabase(databaseLocation) { connection ->
                    val cameraModels = DataBaseUtils.getCameraModels(connection)

                    connection.prepareStatement("INSERT OR IGNORE INTO Image(fileName,height,width,camera,iso,av,tv,tv_real,lens,captureDate,focalLength) VALUES (?,?,?,?,?,?,?,?,?,?,?)")
                        .use { persistImage ->
                            val images = rootImagePaths.asSequence().flatMap {
                                ImageUtils.findImages(
                                    it,
                                    cameraModels,
                                    connection,
                                    predefinedCameras
                                ).asSequence()
                            }.map { image ->
                                persistImage.setString(1, image.fileName)
                                persistImage.setInt(2, image.height)
                                persistImage.setInt(3, image.width)
                                persistImage.setInt(4, image.camera.id)
                                persistImage.setInt(5, image.iso)
                                persistImage.setBigDecimal(6, image.av)
                                persistImage.setString(7, image.tv)
                                persistImage.setBigDecimal(8, image.tvReal)
                                persistImage.setString(9, image.lens)
                                persistImage.setSQLiteDate(10, image.date)
                                persistImage.setInt(11, image.focalLength)
                                persistImage.addBatch()
                                image
                            }.count()
                            println("$images processed in total!")
                            println("Persisting images! This might take a while!")
                            val dbImages = persistImage.executeBatch().sum()
                            println("$dbImages new images found!")
                        }
                }
            }
        } catch (e: MissingValueException) {
            println("Command line parameter ${e.valueName} is missing!")
        } catch (e: ShowHelpException) {
            val help = StringWriter().apply { e.printUserMessage(this, "Reisishot Image 2 DB", 120) }.toString()
            println(help)
        }
    }
}