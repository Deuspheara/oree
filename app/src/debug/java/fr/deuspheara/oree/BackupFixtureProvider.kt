package fr.deuspheara.oree

/** Same-UID document fixture, absent from optimized/release builds. No arbitrary paths. */
class BackupFixtureProvider : android.content.ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: android.net.Uri) = "application/json"

    override fun query(
        uri: android.net.Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): android.database.Cursor? = null

    override fun insert(uri: android.net.Uri, values: android.content.ContentValues?): android.net.Uri? =
        throw UnsupportedOperationException()

    override fun update(
        uri: android.net.Uri,
        values: android.content.ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ) = throw UnsupportedOperationException()

    override fun delete(uri: android.net.Uri, selection: String?, selectionArgs: Array<out String>?) = throw UnsupportedOperationException()

    override fun openFile(uri: android.net.Uri, mode: String): android.os.ParcelFileDescriptor {
        if (uri.path == "/denied") throw SecurityException("Fixture refusal")
        if (uri.path != "/document") throw java.io.FileNotFoundException()
        val flags =
            when (mode) {
                "r" -> android.os.ParcelFileDescriptor.MODE_READ_ONLY

                "wt" ->
                    android.os.ParcelFileDescriptor.MODE_WRITE_ONLY or
                        android.os.ParcelFileDescriptor.MODE_CREATE or
                        android.os.ParcelFileDescriptor.MODE_TRUNCATE

                else -> throw java.io.FileNotFoundException()
            }
        return android.os.ParcelFileDescriptor.open(
            java.io.File(context!!.cacheDir, "backup-fixture.json"),
            flags,
        )
    }
}
