package at.reisisoft.reisishot.image2db

import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.sql.Connection
import java.util.*

class DbCameraSet : TreeSet<DbCamera>({ a, b ->
    val manufacturer = a.manufacturer.compareTo(b.manufacturer)
    if (manufacturer != 0)
        manufacturer
    else a.model.compareTo(b.model)
}) {
    private fun nextIndex(): Int = lastOrNull()?.let { it.id + 1 } ?: 0

    fun getOrCreateCamera(
        manufactrer: String,
        model: String,
        connection: Connection,
        cameraProperties: Properties
    ): DbCamera {
        val initialSize = size
        return find { Objects.equals(it.manufacturer, manufactrer) && Objects.equals(it.model, model) }?.let {
            it // Return the camera which is already known
        } ?: kotlin.run {
            synchronized(this) {
                if (size != initialSize)
                    return getOrCreateCamera(manufactrer, model, connection, cameraProperties)
                val newCamera = DbCamera(nextIndex(), manufactrer, model, getCropFactor(model, cameraProperties))
                val addSuccess = add(newCamera)
                if (!addSuccess) throw IllegalStateException("Unable to add $newCamera to all predefinedCameras!")
                persistCameras(connection, newCamera.toSet())
                newCamera // Return the newly generated camera
            }
        }
    }


    private fun getCropFactor(model: String, cameraProperties: Properties): BigDecimal {
        cameraProperties.getProperty(model)?.let {
            return it.toBigDecimal()
        }
        println("What is the crop factor of your $model?")
        BufferedReader(InputStreamReader(System.`in`)).let {
            var noError = false
            var cropFactor: BigDecimal = BigDecimal.ONE
            while (!noError)
                try {
                    cropFactor = it.readLine().replace(",", ".").toBigDecimal()
                    noError = cropFactor > BigDecimal.ZERO
                } catch (e: Exception) {
                    noError = false
                }
            return cropFactor
        }
    }


    fun persistCameras(connection: Connection, camerasToPersist: Set<DbCamera> = this) {
        connection.prepareStatement("INSERT OR IGNORE INTO Camera(id,manufacturer,model,cropfactor) VALUES (?,?,?,?)")
            .use { persistCameras ->
                camerasToPersist.forEach { camera ->
                    persistCameras.setInt(1, camera.id)
                    persistCameras.setString(2, camera.manufacturer)
                    persistCameras.setString(3, camera.model)
                    persistCameras.setBigDecimal(4, camera.cropFactor)
                    persistCameras.addBatch()
                }
                val persistedCameras = persistCameras.executeBatch().sum()
                println("$persistedCameras have been added!")
            }
    }
}


class DbImageSet : TreeSet<DbImage>({ a, b ->
    val fileName = a.fileName.compareTo(b.fileName)
    if (fileName != 0)
        fileName
    else a.date.compareTo(b.date)
})

private fun <T> T.toSet() = Collections.singleton(this)