package svcs

import java.io.File
import java.security.MessageDigest

const val ROOT_DIR = "vcs"
const val COMMIT_DIR = "commits"
const val CONFIG = "config.txt"
const val INDEX = "index.txt"
const val LOG = "log.txt"

fun main(args: Array<String>) {
    initialize()
    val command = if (args.isNotEmpty()) args[0] else "--help"
    val param = if (args.size > 1) args[1] else ""

    val commands = mapOf(
        "config" to "Get and set a username.",
        "add" to "Add a file to the index.",
        "log" to "Show commit logs.",
        "commit" to "Save changes.",
        "checkout" to "Restore a file.",
    )

    when (command) {
        "config" -> processConfig(param)
        "add" -> processAdd(param)
        "log" -> printLog()
        "commit" -> processCommit(param)
        "checkout" -> processCheckout(param)
        "--help" -> printHelp(commands)
        else -> printCommand(command, commands)
    }
}

fun initialize() {
    val vcs = File(ROOT_DIR)
    if (!vcs.exists()) vcs.mkdir()
    val commits = File("$ROOT_DIR${File.separator}$COMMIT_DIR")
    if (!commits.exists()) commits.mkdir()
    val config = File("$ROOT_DIR${File.separator}$CONFIG")
    if (!config.exists()) config.writeText("")
    val index = File("$ROOT_DIR${File.separator}$INDEX")
    if (!index.exists()) index.writeText("")
    val log = File("$ROOT_DIR${File.separator}$LOG")
    if (!log.exists()) log.writeText("")
}

fun processConfig(param: String) {
    if (param.isNotEmpty()) writeConfig(param)
    readConfig()
}

fun processAdd(param: String) {
    if (param.isNotEmpty()) trackFile(param)
    else printTrackedFiles()
}

fun processCommit(commitMessage: String) {
    if (commitMessage.isEmpty()) {
        println("Message was not passed.")
    } else {
        val trackedFiles = getTrackedFiles()
        val lastCommit = getLastCommit()
        val trackedFilesContent = getTrackedFilesContent(trackedFiles)
        val hash = calculateHash(trackedFilesContent)
        if (hash == lastCommit) {
            println("Nothing to commit.")
        } else {
            commitFiles(hash, trackedFiles)
            writeLog(hash, commitMessage)
            println("Changes are committed.")
        }
    }
}

fun processCheckout(commitId: String) {
    if (commitId.isEmpty()) {
        println("Commit id was not passed.")
    } else {
        checkoutFiles(commitId)
    }
}

fun checkoutFiles(commitId: String) {
    val commitDir = File("$ROOT_DIR${File.separator}$COMMIT_DIR${File.separator}$commitId")
    if (!commitDir.exists()) {
        println("Commit does not exist.")
    } else {
        for (file in commitDir.listFiles()!!) {
            file.copyTo(File(file.name), true)
        }
        println("Switched to commit $commitId.")
    }
}

fun printLog() {
    val log = File("$ROOT_DIR${File.separator}$LOG").readLines()
    if (log.isEmpty()) {
        println("No commits yet.")
    } else {
        log.forEach { println(it) }
    }
}

fun writeLog(commit: String, message: String) {
    val username = File("$ROOT_DIR${File.separator}$CONFIG").readText()
    val log = File("$ROOT_DIR${File.separator}$LOG")
    val currentLog = log.readText()
    log.writeText("commit $commit\nAuthor: $username\n$message\n\n$currentLog")
}

fun getLastCommit(): String {
    val commits = File("$ROOT_DIR${File.separator}$LOG").readLines().filter { it.startsWith("commit") }
    return if (commits.isEmpty()) {
        ""
    } else {
        commits.first().substringAfter("commit ")
    }
}

fun commitFiles(hash: String, trackedFiles: List<String>) {
    val commitDir = "$ROOT_DIR${File.separator}$COMMIT_DIR${File.separator}$hash"
    File(commitDir).mkdir()
    for (filename in trackedFiles) {
        File(filename).copyTo(File("$commitDir${File.separator}$filename"))
    }
}

fun getTrackedFilesContent(trackedFiles: List<String>): String {
    var content = ""
    for (file in trackedFiles) {
        content += File(file).readText()
    }
    return content
}

fun calculateHash(param: String): String {
    val digest: MessageDigest = MessageDigest.getInstance("SHA1")
    val hash: ByteArray = digest.digest(param.toByteArray())
    return hash.joinToString("") { byte -> "%02x".format(byte) }
}

fun readConfig() {
    val config = File("$ROOT_DIR${File.separator}$CONFIG").readText()
    println(
        if (config.isEmpty()) {
            "Please, tell me who you are."
        } else {
            "The username is $config."
        }
    )
}

fun writeConfig(configItem: String) {
    File("$ROOT_DIR${File.separator}$CONFIG").writeText(configItem)
}

fun printTrackedFiles() {
    val index = File("$ROOT_DIR${File.separator}$INDEX").readLines()
    return if (index.isEmpty()) {
        println("Add a file to the index.")
    } else {
        println("Tracked files:")
        index.forEach { println(it) }
    }
}

fun getTrackedFiles(): List<String> {
    return File("$ROOT_DIR${File.separator}$INDEX").readLines()
}

fun trackFile(fileName: String) {
    val file = File(fileName)
    if (file.exists()) {
        File("$ROOT_DIR${File.separator}$INDEX").appendText("$fileName\n")
        println("The file '$fileName' is tracked.")
    } else {
        println("Can't find '$fileName'.")
    }
}

fun printHelp(commands: Map<String, String>) {
    println("These are SVCS commands:")
    for ((command, details) in commands) {
        println("${command.padEnd(10, ' ')} $details")
    }
}

fun printCommand(command: String, commands: Map<String, String>) {
    if (commands.containsKey(command)) {
        println("${commands[command]}")
    } else {
        println("'$command' is not a SVCS command.")
    }
}