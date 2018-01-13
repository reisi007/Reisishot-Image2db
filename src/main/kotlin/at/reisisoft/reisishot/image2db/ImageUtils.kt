package at.reisisoft.reisishot.image2db

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

object ImageUtils {

    fun findImages(path: Path, cameraModels: DbCameraSet): NavigableSet<DbImage> {
        println("Looking for images!")
        val fileVisitor = object : SimpleFileVisitor<Path>() {

            val imageList = DbImageSet()

            override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (dir.fileName.startsWith("."))
                    return FileVisitResult.SKIP_SUBTREE
                println("Started processing ${path.relativize(dir)}")
                return FileVisitResult.CONTINUE
            }

            override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                if (file.fileName.toString().toLowerCase().let { it.endsWith(".jpeg") || it.endsWith(".jpg") })
                    processImageFile(file)?.let { imageList += it }

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

    private fun processImageFile(imageFile: Path): DbImage? {
        TODO("Needs implementation")
    }
}