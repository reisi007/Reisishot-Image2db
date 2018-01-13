package at.reisisoft.reisishot.image2db;

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.MissingValueException
import com.xenomachina.argparser.ShowHelpException
import java.io.StringWriter
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

internal enum class Mode {
    CREATE, UPDATE;

    override fun toString(): String {
        return "--${super.toString().toLowerCase()}"
    }
}

internal fun String.asPath(): Path {
    return Paths.get(this)
}

internal fun <T> Array<T>.stream(): Stream<T> {
    return Arrays.stream(this)
}

fun <T> Stream<T>.checkpoint(continueParallel: Boolean = isParallel, checkpointAction: (() -> Unit)? = null): Stream<T> {
    val checkpoint = collect(Collectors.toList())
    checkpointAction?.invoke()
    return if (continueParallel) checkpoint.parallelStream() else checkpoint.stream()
}


object CLI {
    internal class ParsedArgs(parser: ArgParser) {
        val rootImagePaths by parser.adding("--path", "--imagePath", "--rootPath", "-p", help = "The path to the images") { asPath() }

        val mode by parser.mapping(Mode.values().asSequence().map { it.toString() to it }.toMap(), "Either update a specific database or create a new one")

        val databaseLocation by parser.storing("--db", "--database", "-d", help = "The location of the database on disk") { asPath() }

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

                    connection.prepareStatement("INSERT OR IGNORE INTO Image(fileName,camera,iso,av,tv,lens,date) VALUES (?,?,?,?,?,?,?)").use { persistImage ->
                        connection.prepareStatement("INSERT OR IGNORE INTO Camera(id,manufacturer,model,cropfactor) VALUES (?,?,?,?)").use { persistCameras ->
                            val images = rootImagePaths.parallelStream().flatMap { ImageUtils.findImages(it, cameraModels).stream() }.checkpoint(false) {
                                //Persist camera
                                cameraModels.forEach { camera ->
                                    persistCameras.setInt(1, camera.id)
                                    persistCameras.setString(2, camera.manufacturer)
                                    persistCameras.setString(3, camera.model)
                                    persistCameras.setBigDecimal(4, camera.cropFactor)
                                    persistCameras.addBatch()
                                }
                                val persistedCameras = persistCameras.executeBatch().sum()
                                println("${cameraModels.size} camera models proccessed, $persistedCameras are new!")

                            }.peek { image ->
                                persistImage.setString(1, image.fileName)
                                persistImage.setInt(2, image.camera.id)
                                persistImage.setInt(3, image.iso)
                                persistImage.setBigDecimal(4, image.av)
                                persistImage.setBigDecimal(5, image.tv)
                                persistImage.setString(6, image.lens)
                                persistImage.setDate(7, image.date)
                                persistImage.addBatch()
                            }.count()
                            val dbImages = persistImage.executeBatch().sum()
                            println("$images processed in total, $dbImages new images found!")
                        }
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