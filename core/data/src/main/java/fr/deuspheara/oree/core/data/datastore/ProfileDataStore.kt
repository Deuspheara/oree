package fr.deuspheara.oree.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private val Context.profiles by preferencesDataStore("profiles")

/** The same singleton is used by profile and organization repository bindings. */
fun profileDataStore(context: Context): DataStore<Preferences> = context.applicationContext.profiles
