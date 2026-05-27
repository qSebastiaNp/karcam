package com.karcam

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.File
import java.io.FileInputStream

data class UploadResult(val success: Boolean, val errorMessage: String = "")

object FtpUploader {

    fun upload(file: File): UploadResult {
        val host = BuildConfig.FTP_HOST
        val port = BuildConfig.FTP_PORT.toIntOrNull() ?: 21
        val username = BuildConfig.FTP_USERNAME
        val password = BuildConfig.FTP_PASSWORD
        val destinationPath = BuildConfig.FTP_DESTINATION_PATH

        if (host.isBlank()) {
            return UploadResult(false, "FTP host is not configured")
        }

        val ftpClient = FTPClient()
        ftpClient.connectTimeout = 10_000
        ftpClient.defaultTimeout = 10_000

        return try {
            ftpClient.connect(host, port)
            ftpClient.login(username, password)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            if (destinationPath.isNotBlank() && destinationPath != "/") {
                ftpClient.changeWorkingDirectory(destinationPath)
            }

            FileInputStream(file).use { inputStream ->
                val uploaded = ftpClient.storeFile(file.name, inputStream)
                if (uploaded) {
                    UploadResult(true)
                } else {
                    UploadResult(false, "FTP server rejected the file (reply: ${ftpClient.replyString})")
                }
            }
        } catch (e: Exception) {
            UploadResult(false, e.message ?: "Unknown error during FTP upload")
        } finally {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (_: Exception) {
                // ignore disconnect errors
            }
        }
    }
}
