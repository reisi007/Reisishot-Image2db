package at.reisisoft.reisishot.image2db

import java.util.*

class DbCameraSet : TreeSet<DbCamera>({ a, b ->
    val manufacturer = a.manufacturer.compareTo(b.manufacturer)
    if (manufacturer != 0)
        manufacturer
    else a.model.compareTo(b.model)
}) {
    fun nextIndex(): Int = lastOrNull()?.let { it.id + 1 } ?: 0
}

class DbImageSet : TreeSet<DbImage>({ a, b ->
    val fileName = a.fileName.compareTo(b.fileName)
    if (fileName != 0)
        fileName
    else a.date.compareTo(b.date)
})