package at.reisisoft.reisishot.image2db

import java.io.BufferedReader
import java.io.InputStreamReader
import java.math.BigDecimal
import java.util.*

class DbCameraSet : TreeSet<DbCamera>({ a, b ->
    val manufacturer = a.manufacturer.compareTo(b.manufacturer)
    if (manufacturer != 0)
        manufacturer
    else a.model.compareTo(b.model)
}) {
    private val nextIndex: Int = lastOrNull()?.let { it.id + 1 } ?: 0

    fun getOrCreateCamera(manufactrer: String, model: String): DbCamera {
        val initialSize = size
        return find { Objects.equals(it.manufacturer, manufactrer) && Objects.equals(it.model, model) }?.let {
            it // Return the camera which is already known
        } ?: kotlin.run {
            synchronized(this) {
                if (size != initialSize)
                    return getOrCreateCamera(manufactrer, model)
                println("What is the crop factor of your $model?")
                BufferedReader(InputStreamReader(System.`in`)).let {
                    var noError = false
                    var cropFactor: BigDecimal = BigDecimal.ONE
                    while (!noError)
                        try {
                            cropFactor = it.readLine().replace(",", ".").toBigDecimal()
                            noError = cropFactor >= BigDecimal.ONE
                        } catch (e: Exception) {
                            noError = false
                        }
                    val newCamera = DbCamera(nextIndex, manufactrer, model, cropFactor)
                    val addSuccess = add(newCamera)
                    if (!addSuccess) throw IllegalStateException("Unable to add $newCamera to all cameras!")
                    newCamera // Return the newly generated camera
                }
            }
        }
    }
}


class DbImageSet : TreeSet<DbImage>({ a, b ->
    val fileName = a.fileName.compareTo(b.fileName)
    if (fileName != 0)
        fileName
    else a.date.compareTo(b.date)
})