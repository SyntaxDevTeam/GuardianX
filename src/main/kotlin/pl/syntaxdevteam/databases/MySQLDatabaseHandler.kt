package pl.syntaxdevteam.databases

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.ResultSet
import org.bukkit.configuration.file.FileConfiguration
import pl.syntaxdevteam.PunisherX
import java.io.File
import java.io.IOException

class MySQLDatabaseHandler(private val plugin: PunisherX, config: FileConfiguration) : DatabaseHandler {
    private var connection: Connection? = null
    private val url: String = "jdbc:mysql://${config.getString("database.sql.host")}:${config.getString("database.sql.port")}/${config.getString("database.sql.dbname")}"
    private val user: String = config.getString("database.sql.username") ?: ""
    private val password: String = config.getString("database.sql.password") ?: ""

    override fun openConnection() {
        try {
            connection = DriverManager.getConnection(url, user, password)
            plugin.logger.debug("Connection to the MySQL database established.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to establish connection to the MySQL database. ${e.message}")
        }
    }

    private fun isConnected(): Boolean {
        return connection != null && !connection!!.isClosed
    }

    override fun createTables() {
        if (isConnected()) {
            try {
                val statement = connection!!.createStatement()
                val createPunishmentsTable = """
                CREATE TABLE IF NOT EXISTS `punishments` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `name` varchar(32) DEFAULT NULL,
                  `uuid` varchar(36) DEFAULT NULL,
                  `reason` varchar(255) DEFAULT NULL,
                  `operator` varchar(16) DEFAULT NULL,
                  `punishmentType` varchar(16) DEFAULT NULL,
                  `start` bigint(20) DEFAULT NULL,
                  `end` varchar(32) DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
            """.trimIndent()

                val createPunishmentHistoryTable = """
                CREATE TABLE IF NOT EXISTS `punishmenthistory` (
                  `id` int(11) NOT NULL AUTO_INCREMENT,
                  `name` varchar(32) DEFAULT NULL,
                  `uuid` varchar(36) DEFAULT NULL,
                  `reason` varchar(255) DEFAULT NULL,
                  `operator` varchar(16) DEFAULT NULL,
                  `punishmentType` varchar(16) DEFAULT NULL,
                  `start` bigint(20) DEFAULT NULL,
                  `end` varchar(32) DEFAULT NULL,
                  PRIMARY KEY (`id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
            """.trimIndent()

                statement.executeUpdate(createPunishmentsTable)
                statement.executeUpdate(createPunishmentHistoryTable)
                plugin.logger.debug("Tables `punishments` and `punishmenthistory` created or already exist.")
            } catch (e: SQLException) {
                plugin.logger.err("Failed to create tables. ${e.message}")
            }
        } else {
            plugin.logger.warning("Not connected to the database.")
        }
    }

    override fun closeConnection() {
        try {
            connection?.close()
            plugin.logger.info("Connection to the database closed.")
        } catch (e: SQLException) {
            plugin.logger.err("Failed to close the connection to the database. ${e.message}")
        }
    }

    override fun addPunishment(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long): Boolean {
        if (!isConnected()) {
            openConnection()
        }

        if (isConnected()) {
            val query = """
        INSERT INTO `punishments` (name, uuid, reason, operator, punishmentType, start, end)
        VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

            return try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                preparedStatement.setString(1, name)
                preparedStatement.setString(2, uuid)
                preparedStatement.setString(3, reason)
                preparedStatement.setString(4, operator)
                preparedStatement.setString(5, punishmentType)
                preparedStatement.setLong(6, start)
                preparedStatement.setLong(7, end)
                preparedStatement.executeUpdate()
                plugin.logger.debug("Punishment for player $name added to the database.")
                true
            } catch (e: SQLException) {
                plugin.logger.err("Failed to add punishment for player $name. ${e.message}")
                false
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
            return false
        }
    }

    override fun addPunishmentHistory(name: String, uuid: String, reason: String, operator: String, punishmentType: String, start: Long, end: Long) {
        if (!isConnected()) {
            openConnection()
        }

        if (isConnected()) {
            val query = """
            INSERT INTO `punishmenthistory` (name, uuid, reason, operator, punishmentType, start, end)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()

            try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                preparedStatement.setString(1, name)
                preparedStatement.setString(2, uuid)
                preparedStatement.setString(3, reason)
                preparedStatement.setString(4, operator)
                preparedStatement.setString(5, punishmentType)
                preparedStatement.setLong(6, start)
                preparedStatement.setLong(7, end)
                preparedStatement.executeUpdate()
                plugin.logger.debug("Punishment history for player $name added to the database.")
            } catch (e: SQLException) {
                plugin.logger.err("Failed to add punishment history for player $name. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
        }
    }

    override fun getPunishments(uuid: String): List<PunishmentData> {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ?"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, uuid)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(id, uuid, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                } else {
                    removePunishment(uuid, type)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    override fun getPunishmentsByIP(ip: String): List<PunishmentData> {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ?"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, ip)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(id, ip, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                } else {
                    removePunishment(ip, type)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishments for IP: $ip. ${e.message}")
        }
        return punishments
    }


    override fun removePunishment(uuidOrIp: String, punishmentType: String, removeAll: Boolean) {
        if (!isConnected()) {
            openConnection()
        }
        if (isConnected()) {
            val query = if (removeAll) {
                """
            DELETE FROM `punishments` 
            WHERE `uuid` = ? AND `punishmentType` = ?
            """.trimIndent()
            } else {
                """
            DELETE FROM `punishments` 
            WHERE `uuid` = ? AND `punishmentType` = ?
            ORDER BY `start` DESC
            LIMIT 1
            """.trimIndent()
            }
            try {
                val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
                preparedStatement.setString(1, uuidOrIp)
                preparedStatement.setString(2, punishmentType)
                val rowsAffected = preparedStatement.executeUpdate()
                if (rowsAffected > 0) {
                    plugin.logger.debug("Punishment of type $punishmentType for UUID/IP: $uuidOrIp removed from the database.")
                } else {
                    plugin.logger.warning("No punishment of type $punishmentType found for UUID/IP: $uuidOrIp.")
                }
            } catch (e: SQLException) {
                plugin.logger.err("Failed to remove punishment of type $punishmentType for UUID/IP: $uuidOrIp. ${e.message}")
            }
        } else {
            plugin.logger.warning("Failed to reconnect to the database.")
        }
    }

    override fun getActiveWarnCount(uuid: String): Int {
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishments WHERE uuid = ? AND punishmentType = 'WARN'"
        val punishments = mutableListOf<PunishmentData>()
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, uuid)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(id, uuid, type, reason, start, end)
                if (plugin.punishmentManager.isPunishmentActive(punishment)) {
                    punishments.add(punishment)
                }
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get active warn count for UUID: $uuid. ${e.message}")
        }
        return punishments.size
    }

    override fun getPunishmentHistory(uuid: String, limit: Int, offset: Int): List<PunishmentData> {
        val punishments = mutableListOf<PunishmentData>()
        if (!isConnected()) {
            openConnection()
        }

        val query = "SELECT * FROM punishmentHistory WHERE uuid = ? ORDER BY start DESC LIMIT ? OFFSET ?"
        try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, uuid)
            preparedStatement.setInt(2, limit)
            preparedStatement.setInt(3, offset)
            val resultSet: ResultSet = preparedStatement.executeQuery()
            while (resultSet.next()) {
                val id = resultSet.getInt("id")
                val type = resultSet.getString("punishmentType")
                val reason = resultSet.getString("reason")
                val start = resultSet.getLong("start")
                val end = resultSet.getLong("end")
                val punishment = PunishmentData(id, uuid, type, reason, start, end)
                punishments.add(punishment)
            }
            resultSet.close()
            preparedStatement.close()
        } catch (e: SQLException) {
            plugin.logger.err("Failed to get punishment history for UUID: $uuid. ${e.message}")
        }
        return punishments
    }

    override fun updatePunishmentReason(id: Int, newReason: String): Boolean {
        if (!isConnected()) {
            openConnection()
        }

        val query = "UPDATE punishmentHistory SET reason = ? WHERE id = ?"
        return try {
            val preparedStatement: PreparedStatement = connection!!.prepareStatement(query)
            preparedStatement.setString(1, newReason)
            preparedStatement.setInt(2, id)
            val rowsUpdated = preparedStatement.executeUpdate()
            preparedStatement.close()
            rowsUpdated > 0
        } catch (e: SQLException) {
            plugin.logger.err("Failed to update punishment reason for ID: $id. ${e.message}")
            false
        }
    }

    override fun exportDatabase() {
        val tables = listOf("punishments", "punishmenthistory")
        try {
            connection?.let { conn ->
                val dumpDir = File(plugin.dataFolder, "dump")
                if (!dumpDir.exists()) {
                    dumpDir.mkdirs()
                }
                val writer = File(dumpDir, "backup.sql").bufferedWriter()
                for (table in tables) {
                    val resultSet = conn.createStatement().executeQuery("SELECT * FROM $table")
                    val metaData = resultSet.metaData
                    val columnCount = metaData.columnCount

                    if (!resultSet.isBeforeFirst) {
                        continue
                    }

                    writer.write("INSERT INTO $table VALUES\n")
                    var first = true
                    while (resultSet.next()) {
                        if (!first) {
                            writer.write(",\n")
                        }
                        first = false
                        writer.write("(")
                        for (i in 1..columnCount) {
                            val value = resultSet.getObject(i)
                            if (value == null) {
                                writer.write("NULL")
                            } else {
                                writer.write("'${value.toString().replace("'", "''")}'")
                            }
                            if (i < columnCount) writer.write(", ")
                        }
                        writer.write(")")
                    }
                    writer.write(";\n")
                }
                writer.close()
                plugin.logger.success("Database exported to ${dumpDir}/backup.sql")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to export database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to write to file. ${e.message}")
        }
    }


    override fun importDatabase() {
        val filePath = File(plugin.dataFolder, "dump/backup.sql").absolutePath
        try {
            connection?.let { conn ->
                val lines = File(filePath).readLines()
                val statement = conn.createStatement()
                val sql = StringBuilder()
                for (line in lines) {
                    sql.append(line)
                    if (line.trim().endsWith(";")) {
                        statement.execute(sql.toString())
                        sql.setLength(0) // Clear the StringBuilder
                    }
                }
                plugin.logger.success("Database imported from $filePath")
            }
        } catch (e: SQLException) {
            plugin.logger.err("Failed to import database. ${e.message}")
        } catch (e: IOException) {
            plugin.logger.err("Failed to read from file. ${e.message}")
        }
    }
}
