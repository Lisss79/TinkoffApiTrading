package ru.lisss79.tinkofftradingrobot.activities.fragments

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import ru.lisss79.tinkofftradingrobot.R
import ru.lisss79.tinkofftradingrobot.activities.FILE_IDENTIFIER
import ru.lisss79.tinkofftradingrobot.activities.MIME_ZIP
import ru.lisss79.tinkofftradingrobot.queries_and_responses.JsonKeys
import java.io.*
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.system.exitProcess

class MainSettingsFragment : PreferenceFragmentCompat() {
    private lateinit var c: Context
    private var logsDir: File? = null
    private lateinit var prefsDir: File
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.main_preferences, rootKey)

        var accounts: Array<String>? = null
        var accountsNames: Array<String>? = null
        c = context as Context
        logsDir = c.getExternalFilesDir(null)
        prefsDir = File(
            c.applicationInfo.dataDir +
                    "/" + getString(R.string.shared_prefs_dir)
        )

        activity?.intent?.apply {
            if (hasExtra(JsonKeys.ACCOUNTS) && hasExtra(JsonKeys.ACCOUNTS_NAMES)) {
                accounts = getStringArrayExtra(JsonKeys.ACCOUNTS)
                accountsNames = getStringArrayExtra(JsonKeys.ACCOUNTS_NAMES)
            }
        }

        val saveSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.CreateDocument(MIME_ZIP)) { uri ->
                if (uri != null) {
                    val result = createZip(uri)
                    if (result) Toast.makeText(
                        context,
                        "Настройки сохранены!", Toast.LENGTH_SHORT
                    ).show()
                    else Toast.makeText(
                        context,
                        "Не удалось сохранить настройки!", Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val loadSettingsLauncher =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                if (uri != null) {
                    val result = unpackZip(uri)
                    if (result) {
                        Toast.makeText(
                            context,
                            "Настройки загружены!", Toast.LENGTH_SHORT
                        ).show()
                        view?.postDelayed({ exitProcess(0) }, 1800)
                    } else Toast.makeText(
                        context,
                        "Не удалось загрузить настройки!", Toast.LENGTH_SHORT
                    ).show()
                }
            }

        // Настраиваем меню настроек
        // Меню ввода токена
        val tokenPref = findPreference<EditTextPreference>(getString(R.string.TOKEN))
        tokenPref?.setOnPreferenceChangeListener { _, _ ->
            val builder = AlertDialog.Builder(context)
            builder.setTitle("Внимание!")
                .setMessage("Из-за изменения токена необходимо перезапустить настройки вручную")
                .setPositiveButton("OK") { _, _ -> exitProcess(0) }
                .setCancelable(false)
                .show()
            true
        }
        tokenPref?.summaryProvider = Preference.SummaryProvider<EditTextPreference> {
            val text = it.text
            if (TextUtils.isEmpty(text)) "Не задан"
            else "Установлен"
        }

        // Меню выбора инструмента
        val instrumentPref = findPreference<ListPreference>(getString(R.string.TICKER))
        if (instrumentPref?.value == null) instrumentPref?.setValueIndex(0)

        // Меню выбора аккаунта
        val accountPref = findPreference<ListPreference>(getString(R.string.ACCOUNT))
        if (accounts != null) {
            accountPref?.entryValues = accounts
            accountPref?.entries = accountsNames
        } else {
            accountPref?.entryValues = Array(0) { "Нет данных" }
            accountPref?.entries = Array(0) { "Нет данных" }
        }
        accountPref?.setOnPreferenceChangeListener { _, newValue ->
            val text = String.format("Id выбранного счета: %s", newValue)
            Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            true
        }

        // Меню сохранения настроек
        val savePref = findPreference<Preference>(getString(R.string.save_settings))
        savePref?.setOnPreferenceClickListener {
            saveSettingsLauncher.launch("settings")
            true
        }

        // Меню загрузки настроек
        val loadPref = findPreference<Preference>(getString(R.string.load_settings))
        loadPref?.setOnPreferenceClickListener {
            loadSettingsLauncher.launch(MIME_ZIP)
            true
        }

    }

    private fun unpackZip(uri: Uri): Boolean {
        var error = false

        // Читаем данные из архива
        val packageName = c.packageName
        ZipInputStream(c.contentResolver.openInputStream(uri)).use { zis ->
            var entry = zis.nextEntry
            try {
                while (entry != null) {
                    val name = entry.name
                    val data = entry.extra?.toString(Charset.defaultCharset())
                    val file = if (name.contains(packageName)) File(prefsDir, name)
                    else File(logsDir, name)

                    try {
                        BufferedOutputStream(FileOutputStream(file, false)).use { bos ->
                            if (data != FILE_IDENTIFIER) {
                                throw Exception("Файл не содержит настроек программы")
                            }

                            zis.copyTo(bos, 1024)
                            println("Файл ${file.name} успешно извлечен из архива")
                        }
                    } catch (e: Exception) {
                        println("Не удалось извлечь файл ${file.name} из архива")
                        error = true
                        e.printStackTrace()
                    }

                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }
        return !error
    }

    private fun createZip(uri: Uri): Boolean {

        // Получаем список файлов для архивации
        val settingsPrefs = PreferenceManager.getDefaultSharedPreferences(c)
        val prefs = c.getSharedPreferences(c.packageName, AppCompatActivity.MODE_PRIVATE)
        val settingsPrefsFieldFile = settingsPrefs.javaClass.getDeclaredField("mFile")
        settingsPrefsFieldFile.isAccessible = true
        val prefsSettingsFile = settingsPrefsFieldFile.get(settingsPrefs) as File
        val prefsFieldFile = prefs.javaClass.getDeclaredField("mFile")
        prefsFieldFile.isAccessible = true
        val prefsFile = prefsFieldFile.get(prefs) as File
        val logsFile =
            if (logsDir != null) File(logsDir, getString(R.string.logfile_name)) else null
        val robotFile =
            if (logsDir != null) File(logsDir, getString(R.string.robotfile_name)) else null
        val filesList = listOf(prefsSettingsFile, prefsFile, logsFile, robotFile)

        // Создаем архив и добавляем туда файлы из списка
        ZipOutputStream(c.contentResolver.openOutputStream(uri, "wt")).use { zos ->
            try {
                filesList.forEach { file ->
                    file?.let {
                        try {
                            val bis = BufferedInputStream(FileInputStream(it))
                            val entry = ZipEntry(it.name)
                            entry.extra = FILE_IDENTIFIER.toByteArray()
                            zos.putNextEntry(entry)
                            bis.copyTo(zos, 1024)
                            println("Файл ${it.name} успешно добавлен в архив")
                        } catch (e: Exception) {
                            println("Не удалось добавить файл ${it.name} в архив")
                            e.printStackTrace()
                        }
                    }
                }
                return true
            } catch (e: Exception) {
                e.printStackTrace()
                return false
            }
        }

    }

}