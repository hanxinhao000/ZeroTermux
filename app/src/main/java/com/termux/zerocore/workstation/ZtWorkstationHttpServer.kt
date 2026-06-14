package com.termux.zerocore.workstation

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.termux.zerocore.http_service.NanoFileUpload
import fi.iki.elonen.NanoHTTPD
import fi.iki.elonen.NanoWSD
import org.apache.commons.fileupload.FileItemIterator
import org.apache.commons.fileupload.disk.DiskFileItemFactory
import java.io.ByteArrayInputStream

class ZtWorkstationHttpServer(
    private val appContext: Context,
    port: Int
) : NanoWSD(port) {

    private val gson = Gson()
    private val fileUpload = NanoFileUpload(DiskFileItemFactory())

    override fun openWebSocket(handshake: IHTTPSession): WebSocket? {
        if (handshake.uri == "/ws/terminal") {
            if (!ZtWorkstationPermissionHelper.isTerminalAllowed()) return null
            return ZtWorkstationTerminalWebSocket(handshake)
        }
        return null
    }

    override fun serveHttp(session: IHTTPSession): Response {
        if (session.method == Method.OPTIONS) {
            return cors(newFixedLengthResponse(Response.Status.OK, MIME_PLAINTEXT, ""))
        }

        val uri = session.uri
        val method = session.method
        return try {
            when {
                uri == "/" || uri == "/index.html" -> serveAsset("workstation/index.html", "text/html; charset=utf-8")
                uri == "/static/app.css" -> serveAsset("workstation/app.css", "text/css; charset=utf-8")
                uri == "/static/app.js" -> serveAsset("workstation/app.js", "application/javascript; charset=utf-8")
                uri == "/static/files.js" -> serveAsset("workstation/files.js", "application/javascript; charset=utf-8")
                uri == "/static/terminal.js" -> serveAsset("workstation/terminal.js", "application/javascript; charset=utf-8")
                uri == "/api/device/info" && method == Method.GET -> textResponse(
                    ZtWorkstationDeviceHelper.getDeviceInfo(appContext),
                    MIME_JSON
                )
                uri == "/api/workstation/permissions" && method == Method.GET -> textResponse(
                    ZtWorkstationPermissionHelper.toJson(),
                    MIME_JSON
                )
                uri == "/api/files/list" && method == Method.GET -> guardFiles {
                    textResponse(ZtWorkstationFileHelper.listDirectory(session.parms["path"]), MIME_JSON)
                }
                uri == "/api/files/download" && method == Method.GET -> guardFiles {
                    handleFileDownload(session.parms["path"])
                }
                uri == "/api/files/raw" && method == Method.GET -> guardFiles {
                    handleFileRaw(session.parms["path"])
                }
                uri == "/api/files/read" && method == Method.GET -> guardFiles {
                    textResponse(
                        ZtWorkstationFileHelper.readText(session.parms["path"] ?: ""),
                        MIME_JSON
                    )
                }
                uri == "/api/files/stat" && method == Method.GET -> guardFiles {
                    textResponse(
                        ZtWorkstationFileHelper.stat(session.parms["path"] ?: ""),
                        MIME_JSON
                    )
                }
                uri == "/api/files/mkdir" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.mkdir(obj.get("path").asString)
                    }
                }
                uri == "/api/files/delete" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.delete(obj.get("path").asString)
                    }
                }
                uri == "/api/files/copy" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.copy(obj.get("from").asString, obj.get("to").asString)
                    }
                }
                uri == "/api/files/move" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.move(obj.get("from").asString, obj.get("to").asString)
                    }
                }
                uri == "/api/files/rename" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.rename(obj.get("path").asString, obj.get("name").asString)
                    }
                }
                uri == "/api/files/create" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.createFile(obj.get("path").asString)
                    }
                }
                uri == "/api/files/write" && method == Method.POST -> guardFiles {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationFileHelper.writeText(obj.get("path").asString, obj.get("content").asString)
                    }
                }
                uri == "/api/files/upload" && method == Method.POST -> guardFiles {
                    handleFileUpload(session)
                }
                uri == "/api/contacts" && method == Method.GET -> guardPhoneSms {
                    textResponse(
                        ZtWorkstationContactsHelper.listContacts(appContext),
                        MIME_JSON
                    )
                }
                uri == "/api/sms/threads" && method == Method.GET -> guardPhoneSms {
                    textResponse(
                        ZtWorkstationSmsHelper.listThreads(appContext),
                        MIME_JSON
                    )
                }
                uri == "/api/sms/messages" && method == Method.GET -> guardPhoneSms {
                    textResponse(
                        ZtWorkstationSmsHelper.listMessages(appContext, session.parms["address"] ?: ""),
                        MIME_JSON
                    )
                }
                uri == "/api/sms/send" && method == Method.POST -> guardPhoneSms {
                    handleJsonAction(session) { obj ->
                        ZtWorkstationSmsHelper.sendSms(
                            obj.get("address").asString,
                            obj.get("body").asString
                        )
                    }
                }
                uri == "/api/camera/frame" && method == Method.GET -> guardCamera {
                    handleCameraFrame(session.parms["facing"] ?: "front")
                }
                uri == "/api/camera/start" && method == Method.POST -> guardCamera {
                    ZtWorkstationManager.cameraHelper?.startCameras()
                    textResponse("{\"ok\":true}", MIME_JSON)
                }
                uri == "/api/camera/release" && method == Method.POST -> guardCamera {
                    ZtWorkstationManager.cameraHelper?.releaseCameras()
                    textResponse("{\"ok\":true}", MIME_JSON)
                }
                uri == "/api/settings/pages" && method == Method.GET -> textResponse(
                    ZtWorkstationSettingsHelper.listPages(),
                    MIME_JSON
                )
                uri == "/api/settings/zt" && method == Method.GET -> textResponse(
                    ZtWorkstationSettingsHelper.listSettings(),
                    MIME_JSON
                )
                uri == "/api/settings/zt" && method == Method.POST -> handleJsonAction(session) { obj ->
                    ZtWorkstationSettingsHelper.updateSetting(
                        obj.get("key").asString,
                        obj.get("value").asString
                    )
                }
                else -> cors(newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found"))
            }
        } catch (e: Exception) {
            cors(newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, e.message ?: "error"))
        }
    }

    private fun guardFiles(block: () -> Response): Response {
        if (!ZtWorkstationPermissionHelper.isFilesAllowed()) return forbiddenPermission()
        return block()
    }

    private fun guardCamera(block: () -> Response): Response {
        if (!ZtWorkstationPermissionHelper.isCameraAllowed()) return forbiddenPermission()
        return block()
    }

    private fun guardPhoneSms(block: () -> Response): Response {
        if (!ZtWorkstationPermissionHelper.isPhoneSmsAllowed()) return forbiddenPermission()
        return block()
    }

    private fun forbiddenPermission(): Response {
        return cors(
            newFixedLengthResponse(
                Response.Status.FORBIDDEN,
                MIME_JSON,
                gson.toJson(mapOf("ok" to false, "error" to "permission denied"))
            )
        )
    }

    private fun handleFileDownload(path: String?): Response {
        val pair = ZtWorkstationFileHelper.openDownloadStream(path ?: "") ?: return cors(
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "file not found")
        )
        val (file, stream) = pair
        val response = newFixedLengthResponse(
            Response.Status.OK,
            "application/octet-stream",
            stream,
            file.length()
        )
        response.addHeader("Content-Disposition", "attachment; filename=\"${file.name}\"")
        return cors(response)
    }

    private fun handleFileRaw(path: String?): Response {
        val triple = ZtWorkstationFileHelper.openRawStream(path ?: "") ?: return cors(
            newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "file not found")
        )
        val (file, stream, mime) = triple
        val response = newFixedLengthResponse(
            Response.Status.OK,
            mime,
            stream,
            file.length()
        )
        response.addHeader("Content-Disposition", "inline; filename=\"${file.name}\"")
        return cors(response)
    }

    private fun handleFileUpload(session: IHTTPSession): Response {
        if (!NanoFileUpload.isMultipartContent(session)) {
            return jsonError("multipart required")
        }
        var targetPath = session.parms["path"] ?: ""
        var result = "{\"ok\":false}"
        val iterator: FileItemIterator = fileUpload.getItemIterator(session)
        while (iterator.hasNext()) {
            val item = iterator.next()
            if (item.isFormField) {
                if (item.fieldName == "path") {
                    targetPath = item.openStream().bufferedReader().readText()
                }
            } else {
                result = ZtWorkstationFileHelper.saveUploadedFile(targetPath, item.openStream())
            }
        }
        return textResponse(result, MIME_JSON)
    }

    private fun handleCameraFrame(facing: String): Response {
        val helper = ZtWorkstationManager.cameraHelper
        val bytes = helper?.getFrame(facing)
        if (bytes == null || bytes.isEmpty()) {
            return cors(newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, ""))
        }
        return cors(newFixedLengthResponse(Response.Status.OK, "image/jpeg", ByteArrayInputStream(bytes), bytes.size.toLong()))
    }

    private fun handleJsonAction(session: IHTTPSession, block: (JsonObject) -> String): Response {
        val body = readBody(session)
        val obj = JsonParser.parseString(body).asJsonObject
        return textResponse(block(obj), MIME_JSON)
    }

    private fun readBody(session: IHTTPSession): String {
        val files = HashMap<String, String>()
        session.parseBody(files)
        return files["postData"] ?: ""
    }

    private fun serveAsset(assetPath: String, mime: String): Response {
        val stream = appContext.assets.open(assetPath)
        val bytes = stream.readBytes()
        stream.close()
        return cors(newFixedLengthResponse(Response.Status.OK, mime, ByteArrayInputStream(bytes), bytes.size.toLong()))
    }

    private fun jsonError(message: String): Response {
        return textResponse(gson.toJson(mapOf("ok" to false, "error" to message)), MIME_JSON)
    }

    private fun textResponse(body: String, mime: String): Response {
        return cors(newFixedLengthResponse(Response.Status.OK, mime, body))
    }

    private fun cors(response: Response): Response {
        response.addHeader("Access-Control-Allow-Origin", "*")
        response.addHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
        response.addHeader("Access-Control-Allow-Headers", "Content-Type")
        return response
    }

    companion object {
        private const val MIME_JSON = "application/json; charset=utf-8"
    }
}
