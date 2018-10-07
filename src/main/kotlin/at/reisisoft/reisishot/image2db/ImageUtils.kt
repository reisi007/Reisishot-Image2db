package at.reisisoft.reisishot.image2db

import com.drew.imaging.ImageMetadataReader
import com.drew.metadata.exif.ExifIFD0Directory
import com.drew.metadata.exif.ExifSubIFDDirectory
import com.drew.metadata.file.FileSystemDirectory
import com.drew.metadata.jpeg.JpegDirectory
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.sql.Connection
import java.util.*


object ImageUtils {

    fun findImages(
        path: Path,
        cameraModels: DbCameraSet,
        connection: Connection,
        cameraProperties: Properties
    ): NavigableSet<DbImage> {
        println("Looking for images!")
        val fileVisitor = object : SimpleFileVisitor<Path>() {

            val imageList = DbImageSet()

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (dir.fileName.toString().startsWith("."))
                    return FileVisitResult.SKIP_SUBTREE
                println("Started processing ${path.relativize(dir)}")
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.fileName.toString().toLowerCase().let { it.endsWith(".jpeg") || it.endsWith(".jpg") })
                    processImageFile(file, cameraModels, connection, cameraProperties)?.let {
                        println("Processed ${path.relativize(file)} successfully!")
                        imageList += it
                    } ?: kotlin.run { println("Error while processing ${path.relativize(file)}!") }

                return FileVisitResult.CONTINUE
            }

            override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
                if (exc == null) {
                    println("Finished processing ${path.relativize(dir)}")
                    return FileVisitResult.CONTINUE
                }
                println("Error while processing $dir, error message: ${exc.message} ")
                return FileVisitResult.SKIP_SUBTREE
            }
        }
        Files.walkFileTree(path, fileVisitor)

        return fileVisitor.imageList
    }

    private fun processImageFile(
        imageFile: Path,
        cameraModels: DbCameraSet,
        connection: Connection,
        cameraProperties: Properties
    ): DbImage? =
        ImageMetadataReader.readMetadata(imageFile.toFile()).let { metadata ->
            metadata.getFirstDirectoryOfType(JpegDirectory::class.java).let { jpegDirectory ->
                metadata.getFirstDirectoryOfType(ExifIFD0Directory::class.java).let { exifIfd0Directory ->
                    metadata.getFirstDirectoryOfType(ExifSubIFDDirectory::class.java).let { exifSubIfdDirectory ->
                        metadata.getFirstDirectoryOfType(FileSystemDirectory::class.java).let { fileSystemDirectory ->
                            try {
                                val camera = cameraModels.getOrCreateCamera(
                                    exifIfd0Directory.getMake(),
                                    exifIfd0Directory.getModel(),
                                    connection,
                                    cameraProperties

                                )
                                return DbImage(
                                    fileSystemDirectory.getFileName(),
                                    jpegDirectory.imageHeight,
                                    jpegDirectory.imageWidth,
                                    camera,
                                    exifSubIfdDirectory.getIso(),
                                    exifSubIfdDirectory.getAv(),
                                    exifSubIfdDirectory.getTv(),
                                    exifSubIfdDirectory.getTvAsBigDecimal(),
                                    exifSubIfdDirectory.getLensModel(),
                                    exifSubIfdDirectory.getFocalLength(),
                                    java.sql.Date(exifSubIfdDirectory.dateOriginal.time)
                                )
                            } catch (e: RuntimeException) {
                                e.printStackTrace()
                                return null
                            }
                        }
                    }
                }
            }
        }

    private fun FileSystemDirectory.getFileName() = getString(1)
    private fun ExifIFD0Directory.getMake() = getString(271)
    private fun ExifIFD0Directory.getModel() = getString(272)
    private fun ExifSubIFDDirectory.getIso() = getString(34866).toInt()
    private fun ExifSubIFDDirectory.getAv() =
        BigDecimal(getRational(33437).toDouble()).setScale(1, RoundingMode.HALF_UP)

    private fun ExifSubIFDDirectory.getTv() = getString(33434)
    private fun ExifSubIFDDirectory.getTvAsBigDecimal() =
        BigDecimal(getRational(33434).toDouble()).setScale(6, RoundingMode.HALF_UP)

    private fun ExifSubIFDDirectory.getFocalLength() = Math.round(getRational(37386).toFloat())
    private fun ExifSubIFDDirectory.getLensModel() = getString(42036)?.let {
        if (it.isBlank())
            return@let "${getFocalLength()} mm"
        return@let it
    }

}