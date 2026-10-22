package fr.deuspheara.oree

/** Constant generated fixture, no filesystem/user data or production provider. */
class TestWallpaperProvider : android.content.ContentProvider() {
    override fun onCreate() = true

    override fun getType(uri: android.net.Uri) = "image/png"

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
        require(mode == "r")
        val pipe = android.os.ParcelFileDescriptor.createPipe()
        Thread {
            android.os.ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                if (uri.path == "/invalid") {
                    output.write(byteArrayOf(1, 2, 3))
                } else {
                    val image =
                        android.graphics.Bitmap.createBitmap(
                            192,
                            108,
                            android.graphics.Bitmap.Config.ARGB_8888,
                        )
                    image.eraseColor(android.graphics.Color.rgb(30, 80, 120))
                    image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, output)
                    image.recycle()
                }
            }
        }
            .start()
        return pipe[0]
    }
}
