package com.karcam

import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPSClient
import org.apache.commons.net.io.CopyStreamAdapter
import org.apache.commons.net.util.TrustManagerUtils
import java.io.File
import java.io.FileInputStream
import java.net.Socket
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket

data class UploadResult(val success: Boolean, val errorMessage: String = "")

object FtpUploader {

    fun upload(file: File, onProgress: (Int) -> Unit): UploadResult {
        val host = BuildConfig.FTP_HOST
        val port = BuildConfig.FTP_PORT.toIntOrNull() ?: 21
        val username = BuildConfig.FTP_USERNAME
        val password = BuildConfig.FTP_PASSWORD
        val destinationPath = BuildConfig.FTP_DESTINATION_PATH

        if (host.isBlank()) {
            return UploadResult(false, "FTP-Host ist nicht konfiguriert")
        }

        val ftpClient = object : FTPSClient("TLSv1.2", false) {
            override fun _prepareDataSocket_(socket: Socket) {
                if (socket is SSLSocket) {
                    val controlSocket = _socket_
                    if (controlSocket is SSLSocket) {
                        val session: SSLSession = controlSocket.session
                        val context = session.sessionContext
                        try {
                            val method = context.javaClass.getDeclaredMethod(
                                "setSession",
                                Long::class.javaPrimitiveType,
                                SSLSession::class.java
                            )
                            method.isAccessible = true
                            method.invoke(context, 100L, session)
                        } catch (e1: Exception) {
                            try {
                                val method = context.javaClass.getDeclaredMethod(
                                    "setSession",
                                    SSLSession::class.java
                                )
                                method.isAccessible = true
                                method.invoke(context, session)
                            } catch (e2: Exception) {}
                        }
                    }
                }
            }
        }
        
        ftpClient.trustManager = TrustManagerUtils.getAcceptAllTrustManager()
        ftpClient.connectTimeout = 20_000
        ftpClient.defaultTimeout = 20_000
        ftpClient.setDataTimeout(20_000)

        // Progress Listener
        val fileSize = file.length()
        ftpClient.copyStreamListener = object : CopyStreamAdapter() {
            override fun bytesTransferred(totalBytesTransferred: Long, bytesTransferred: Int, streamSize: Long) {
                val percentage = ((totalBytesTransferred.toDouble() / fileSize) * 100).toInt()
                onProgress(percentage.coerceIn(0, 99)) // 100% erst nach dem Rename
            }
        }

        return try {
            ftpClient.connect(host, port)
            
            if (!ftpClient.login(username, password)) {
                return UploadResult(false, "Login fehlgeschlagen für '$username' (${ftpClient.replyString})")
            }
            
            ftpClient.execPBSZ(0)
            ftpClient.execPROT("P")
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
            ftpClient.setRemoteVerificationEnabled(false)

            if (!destinationPath.isNullOrBlank() && destinationPath != "/") {
                if (!ftpClient.changeWorkingDirectory(destinationPath)) {
                    return UploadResult(false, "Ordnerwechsel fehlgeschlagen: $destinationPath")
                }
            }

            val tempFileName = "${file.name}.prog"
            
            FileInputStream(file).use { inputStream ->
                val uploaded = ftpClient.storeFile(tempFileName, inputStream)
                if (uploaded) {
                    // Nach erfolgreichem Upload umbenennen
                    if (ftpClient.rename(tempFileName, file.name)) {
                        onProgress(100)
                        UploadResult(true)
                    } else {
                        UploadResult(false, "Umbenennen fehlgeschlagen (${ftpClient.replyString})")
                    }
                } else {
                    UploadResult(false, "Upload fehlgeschlagen (Code: ${ftpClient.replyCode})")
                }
            }
        } catch (e: Exception) {
            UploadResult(false, "FTP-Fehler: ${e.message}")
        } finally {
            try {
                if (ftpClient.isConnected) {
                    ftpClient.logout()
                    ftpClient.disconnect()
                }
            } catch (_: Exception) {}
        }
    }
}
